package com.chapchap.auth.domain.auth.service;

import com.chapchap.auth.domain.auth.constant.ProviderPolicy;
import com.chapchap.auth.domain.auth.dto.IdentityVerification;
import com.chapchap.auth.domain.auth.dto.VerifiedIdentity;
import com.chapchap.auth.domain.auth.service.client.IdentityVerificationClient;
import com.chapchap.auth.domain.auth.service.identity.IdentityKeyGenerator;
import com.chapchap.auth.domain.auth.service.validator.SignupAgeValidator;
import com.chapchap.auth.global.error.custom.business.InvalidStateException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class IdentityVerificationService {

    // 이번에 확인된 PortOne 시뮬레이터의 공통 DI
    private static final String SIMULATOR_DI = "simulator__di_value";

    // 실제 DI를 사용하는 키와 테스트용 키의 재료를 구분한다.
    private static final String TEST_KEY_PREFIX = "TEST_SOCIAL_V1|";

    private final IdentityVerificationClient identityVerificationClient;
    private final SignupAgeValidator signupAgeValidator;
    private final IdentityKeyGenerator identityKeyGenerator;

    // 기본값은 false.
    // 별도로 분리된 테스트 서버·DB에서만 true로 설정한다.
    @Value("${AUTH_IDENTITY_TEST_MODE:false}")
    private boolean identityTestMode;

    // 기존 verify(인증ID) 호출과의 호환을 위해 유지한다.
    // 소셜 정보가 없으므로 시뮬레이터 가입은 이 메서드로 처리할 수 없다.
    public VerifiedIdentity verify(String identityVerificationId) {
        return verify(identityVerificationId, null, null);
    }

    // PortOne 인증 결과를 조회한 뒤 사용할 identityKey를 생성한다.
    // provider와 providerUserId는 서버가 저장한 가입 세션에서 받아야 한다.
    public VerifiedIdentity verify(
            String identityVerificationId,
            ProviderPolicy provider,
            String providerUserId
    ) {
        IdentityVerification verification =
                identityVerificationClient.getVerification(
                        identityVerificationId
                );

        // 테스트 모드여도 PortOne 조회와 인증 완료 상태 검사는 생략하지 않는다.
        if (verification == null || !verification.verified()) {
            throw new InvalidStateException(
                    "본인인증이 완료되지 않았습니다."
            );
        }

        // 기존 생년월일 조건 검사를 유지한다.
        if (!signupAgeValidator.isEligible(verification.birthDate())) {
            throw new InvalidStateException(
                    "만 14세 미만은 가입할 수 없습니다."
            );
        }

        String identityKey = createIdentityKey(
                verification.di(),
                provider,
                providerUserId
        );

        return new VerifiedIdentity(
                identityKey,
                verification.name(),
                verification.phone()
        );
    }

    private String createIdentityKey(
            String di,
            ProviderPolicy provider,
            String providerUserId
    ) {
        // DI 누락을 테스트용 키로 대체하지 않는다.
        if (di == null || di.isBlank()) {
            throw new InvalidStateException(
                    "본인인증 식별값이 없습니다."
            );
        }

        // 확인된 시뮬레이터 DI인 경우에만 테스트용 처리를 수행한다.
        if (SIMULATOR_DI.equals(di)) {

            // 설정이 없거나 false라면 시뮬레이터 가입을 차단한다.
            if (!identityTestMode) {
                throw new InvalidStateException(
                        "현재 환경에서는 시뮬레이터 본인인증을 사용할 수 없습니다."
                );
            }

            if (provider != ProviderPolicy.KAKAO
                    && provider != ProviderPolicy.GOOGLE) {
                throw new InvalidStateException(
                        "가입 세션의 소셜 제공자 정보가 올바르지 않습니다."
                );
            }

            if (providerUserId == null || providerUserId.isBlank()) {
                throw new InvalidStateException(
                        "가입 세션의 소셜 계정 정보가 없습니다."
                );
            }

            // 예: TEST_SOCIAL_V1|KAKAO|123456789
            // 같은 소셜 계정이면 항상 같은 재료로 키를 만든다.
            String source =
                    TEST_KEY_PREFIX
                            + provider.name()
                            + "|"
                            + providerUserId;

            // 기존 HMAC 계산 함수를 그대로 사용한다.
            return identityKeyGenerator.generate(source);
        }

        // 다른 시뮬레이터 값이 들어오면 실제 DI로 처리하지 않는다.
        if (di.startsWith("simulator__")) {
            throw new InvalidStateException(
                    "지원하지 않는 시뮬레이터 본인인증 식별값입니다."
            );
        }

        // 시뮬레이터 값이 아니면 기존 DI 기반 계산을 유지한다.
        return identityKeyGenerator.generate(di);
    }
}