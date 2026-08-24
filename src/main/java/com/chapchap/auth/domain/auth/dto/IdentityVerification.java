package com.chapchap.auth.domain.auth.dto;

import java.time.LocalDate;

public record IdentityVerification(
        boolean verified,  // PortOne 본인인증 완료 여부

        String di,         // 동일인 식별용 DI 원문

        String name,       // 본인인증으로 확인된 이름

        String phone,      // 본인인증으로 확인된 전화번호

        LocalDate birthDate // 만 14세 이상 검증용 생년월일
) {
}
