package com.chapchap.auth.domain.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


@JsonIgnoreProperties(ignoreUnknown = true) // record에 없는 JSON 필드가 있어도 오류내지 말고 무시하도록 설정
public record PortOneIdentityVerification(

        String status, // READY / VERIFIED / FAILED

        PortOneVerifiedCustomer verifiedCustomer
) {
}
