package com.chapchapauthservice.domain.auth.client;

import com.chapchapauthservice.domain.auth.dto.IdentityVerification;

public interface IdentityVerificationClient {

    // 본인인증 ID를 이용해 외부 인증기관의 실제 인증 결과를 조회
    IdentityVerification getVerification(String identityVerificationId);
}
