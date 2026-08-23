package com.chapchapauthservice.domain.auth.service;

import com.chapchapauthservice.domain.auth.client.IdentityVerificationClient;
import com.chapchapauthservice.domain.auth.dto.IdentityVerification;
import com.chapchapauthservice.domain.auth.dto.VerifiedIdentity;
import com.chapchapauthservice.domain.auth.validator.SignupAgeValidator;
import com.chapchapauthservice.global.security.identity.IdentityKeyGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class IdentityVerificationService {


    private final IdentityVerificationClient identityVerificationClient;
    private final SignupAgeValidator signupAgeValidator;
    private final IdentityKeyGenerator identityKeyGenerator;

    // PortOne 본인인증 결과를 서버에서 다시 검증하고, 회원가입 로직에서 사용할 안전한 본인정보로 변환한다.
    public VerifiedIdentity verify(String identityVerificationId) {

        // 클라이언트가 전달한 인중 ID로 PortOne에 실제 인증 결과를 재조회 한다.
        IdentityVerification verification = identityVerificationClient.getVerification(identityVerificationId);

        // PortOne 에서 정상적으로 완료된 본인인증이 아니면 가입에 사용할 수 없다.
        if (!verification.verified()) {
            throw new IllegalStateException("본인인증이 완료되지 않았습니다.");
        }

        // PortOne 에서 받은 생년월일을 이용해 만 14세 이상인지 확인한다.
        if (!signupAgeValidator.isEligible(verification.birthDate())) {
            throw new IllegalStateException("만 14세 미만은 가입할 수 없습니다.");
        }

        // DI 원문은 저장하지 않고 서버 Secret과 HMAC-SHA-256으로 동일인 식별용 identityKey를 생성한다.
        String identityKey = identityKeyGenerator.generate(verification.di());

        // DI와 생년월일은 여기서 버리고 이후 회원가입 로직에는 필요한 정보만 전달한다.
        return new VerifiedIdentity(
                identityKey,
                verification.name(),
                verification.phone()
        );
    }
}
