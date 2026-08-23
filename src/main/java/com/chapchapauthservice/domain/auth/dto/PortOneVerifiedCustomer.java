package com.chapchapauthservice.domain.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = true) // record에 없는 JSON 필드가 있어도 오류내지 말고 무시하도록 설정
public record PortOneVerifiedCustomer(

        String di,          // 동일인 식별에 사용할 DI 원문

        String name,        // 본인인증으로 확인된 이름

        String phoneNumber, // 본인인증으로 확인된 전화번호

        LocalDate birthDate // 만 14세 이상 검증에 사용할 생년월일
) {
}
