package com.chapchap.auth.domain.auth.service;

import com.chapchap.auth.domain.auth.dto.IssuedToken;
import com.chapchap.auth.domain.auth.dto.VerifiedIdentity;
import com.chapchap.auth.domain.auth.entity.SignupSession;
import com.chapchap.auth.domain.auth.repository.SignupSessionRepository;
import com.chapchap.auth.domain.auth.request.PolicyConsentRequest;
import com.chapchap.auth.domain.auth.request.SignupCompleteRequest;
import com.chapchap.auth.domain.auth.service.validator.SignupPolicyValidator;
import com.chapchap.auth.domain.policy.entity.Policy;
import com.chapchap.auth.domain.policy.entity.UserPolicyConsent;
import com.chapchap.auth.domain.policy.repository.PolicyRepository;
import com.chapchap.auth.domain.policy.repository.UserPolicyConsentRepository;
import com.chapchap.auth.domain.user.entity.SocialAccount;
import com.chapchap.auth.domain.user.entity.User;
import com.chapchap.auth.domain.user.repository.SocialAccountRepository;
import com.chapchap.auth.domain.user.repository.UserRepository;
import com.chapchap.auth.global.messaging.kafka.producer.AuthEventProducer;
import com.chapchap.auth.global.error.custom.business.DuplicatedResourceException;
import com.chapchap.auth.global.error.custom.business.InvalidStateException;
import com.chapchap.auth.global.error.custom.business.NotFoundResourceException;
import com.chapchap.auth.domain.policy.constant.ConsentStatusPolicy;
import com.chapchap.auth.domain.auth.constant.SignupSessionStatusPolicy;
import com.chapchap.auth.domain.user.constant.UserStatusPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SignupService {

    private final SignupSessionRepository signupSessionRepository;
    private final IdentityVerificationService identityVerificationService;
    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final PolicyRepository policyRepository;
    private final SignupPolicyValidator signupPolicyValidator;
    private final UserPolicyConsentRepository userPolicyConsentRepository;
    private final AuthService authService;
    private final AuthEventProducer authEventProducer;

    @Transactional
    public IssuedToken completeSignup(SignupCompleteRequest request) {
        
        // 가입 완료 중복 요청을 방지하기 위해 가입 세션을 행 잠금으로 조회
        SignupSession signupSession = signupSessionRepository.findByIdForUpdate(
                request.signupSessionId()
        ).orElseThrow(() -> new NotFoundResourceException("가입 세션을 찾을 수 없습니다."));
        
        // 가입 세션의 15분 유효시간이 지났는지 확인
        if (signupSession.markExpiredIfNeeded()) {
            throw new InvalidStateException("가입 세션이 만료되었습니다.");
        }

        // 가입을 시작할 수 있는 PENDING 상태인지 확인
        if (signupSession.getStatus() != SignupSessionStatusPolicy.PENDING) {
            throw new InvalidStateException("사용할 수 없는 가입 세션입니다.");
        }
        
        // 이미 가입 또는 계정 연결에 사용된 세션인지 확인
        if (signupSession.getConsumedAt() != null) {
            throw new InvalidStateException("이미 사용된 가입 세션입니다.");
        }

        // 동일한 본인인증 ID가 다른 가입 과정에서 이미 사용됐는지 확인
        if (signupSessionRepository.existsByIdentityVerificationId(request.identityVerificationId())) {
            throw new DuplicatedResourceException("이미 사용된 본인인증 정보입니다.");
        }

        // PortOne 본인인증 결과를 서버에서 재조회하고 연령 검증 및 identityKey 생성을 수행
        VerifiedIdentity verifiedIdentity = identityVerificationService.verify(request.identityVerificationId());
        
        // 검증이 완료된 본인정보를 가입 세션에 반영하고 PENDING -> IDENTITY_VERIFIED 상태로 변경
        signupSession.markIdentityVerified(
                request.identityVerificationId(),
                verifiedIdentity.identityKey()
        );
        
        // 현재 시점에 실제 가입에 적용되는 활성 정책 조회
        List<Policy> activePolicies = policyRepository.findAllByActiveTrueAndEffectiveAtLessThanEqual(
                LocalDateTime.now()
        );
        
        // 클라이언트가 제출한 정책 선택이 현재 정책과 일치하는지 검증
        // 필수 정책 누락, 미동의, 과거Version 제출 등을 차단
        signupPolicyValidator.validate(
                activePolicies,
                request.policies()
        );

        // identityKey를 기준으로 기존 동일인이 있는지 확인
        Optional<User> existingUser =
                userRepository.findByIdentityKey(
                        verifiedIdentity.identityKey()
                );

        User user;
        boolean isNewUser;

        if (existingUser.isPresent()) {

            // 동일인이 이미 존재하면 기존 User를 그대로 사용
            user = existingUser.get();
            isNewUser = false;

            // 정지 또는 탈퇴 사용자는 새 로그인 수단 연결과 Token 발급 차단
            if (user.getStatus() != UserStatusPolicy.ACTIVE || user.getWithdrawnAt() != null) {
                throw new InvalidStateException("현재 사용할 수 없는 사용자 계정입니다.");
            }

        } else {

            // 동일인이 없으면 신규 일반 사용자 생성
            user = User.createCustomer(
                    verifiedIdentity.identityKey(),
                    verifiedIdentity.name(),
                    verifiedIdentity.phone(),
                    null, // 이메일은 소셜 Provider 정보에서 별도로 처리
                    LocalDateTime.now()
            );

            user = userRepository.save(user);
            isNewUser = true;
        }
        
        // 해당 소셜 계정 자체가 이미 다른 가입에 사용되고 있는지 확인
        if (socialAccountRepository.findByProviderAndProviderUserId(
                signupSession.getProvider(),
                signupSession.getProviderUserId()
        ).isPresent()) {
            throw new DuplicatedResourceException("이미 연결된 소셜 계정입니다.");
        }
        
        // 기존 사용자가 같은 Provider의 소셜 계정을 이미 가지고 있는지 확인
        if (socialAccountRepository.existsByUserAndProvider(
                user,
                signupSession.getProvider()
        )) {
            throw new DuplicatedResourceException("이미 동일한 소셜 로그인 수단이 연결되어 있습니다.");
        }
        
        // 검증이 끝난 사용자에게 현재 가입 세션의 소셜 계정을 연결
        SocialAccount socialAccount = SocialAccount.create(
                user,
                signupSession.getProvider(),
                signupSession.getProviderUserId()
        );

        socialAccountRepository.save(socialAccount);
        
        // 현재 활성 정책 Version에 대한 사용자의 선택을 저장
        savePolicyConsents(
                user,
                activePolicies,
                request.policies()
        );

        // 모든 가입 데이터 처리가 끝난 가입 세션을 완료/소비 상태로 변경
        signupSession.complete();

        // 가입 완료 사용자에게 최초 인증 세션과 Token 발급
        IssuedToken issuedToken = authService.issueToken(user);

        if (isNewUser) {
            authEventProducer.publishUserRegisteredAfterCommit(user.getId(), user.getRole());
        }
        return issuedToken;
    }

    // 회원가입 시 검증이 끝난 현재 정책에 대한 사용자 선택을 저장
    private void savePolicyConsents(
            User user,
            List<Policy> activePolicies,
            List<PolicyConsentRequest> requests
    ) {

        for (Policy policy : activePolicies) {

            // 현재 Policy에 대해 사용자가 제출한 선택 찾기
            PolicyConsentRequest request = requests.stream()
                    .filter(policyRequest ->
                            policyRequest.policyId().equals(policy.getId())
                    )
                    .findFirst()
                    .orElse(null);

            // 현재 Policy Version에 대한 기존 동의 기록 조회
            Optional<UserPolicyConsent> existingConsent =
                    userPolicyConsentRepository.findByUserAndPolicy(
                            user,
                            policy
                    );

            // 현재 Version에 대한 동의 기록이 없는 경우
            if (existingConsent.isEmpty()) {

                // 선택 정책을 제출하지 않았다면 false → DECLINED
                boolean agreed =
                        request != null
                                && Boolean.TRUE.equals(request.agreed());

                UserPolicyConsent consent =
                        UserPolicyConsent.createInitial(
                                user,
                                policy,
                                agreed
                        );

                userPolicyConsentRepository.save(consent);
                continue;
            }

            UserPolicyConsent consent = existingConsent.get();

            // 선택 정책을 아예 제출하지 않았다면 기존 상태 유지
            if (request == null) {
                continue;
            }

            // 필수 정책은 Validator에서 이미 true인지 검증했으므로
            // 기존 AGREED 상태라면 별도 변경하지 않는다.
            if (policy.isRequired()) {

                if (consent.getConsentStatus()
                        != ConsentStatusPolicy.AGREED) {

                    throw new InvalidStateException(
                            "필수 정책 동의 상태가 올바르지 않습니다."
                    );
                }

                continue;
            }

            // 선택 정책에 동의한 경우
            if (Boolean.TRUE.equals(request.agreed())) {

                if (consent.getConsentStatus()
                        != ConsentStatusPolicy.AGREED) {

                    consent.changeMarketingConsent(true);
                }

                continue;
            }

            // 기존에 동의했던 선택 정책을 철회한 경우
            if (consent.getConsentStatus()
                    == ConsentStatusPolicy.AGREED) {

                consent.changeMarketingConsent(false);
            }
        }
    }
}
