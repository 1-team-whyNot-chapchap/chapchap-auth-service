package com.chapchap.auth.domain.auth.dto;

public record VerifiedIdentity(

        String identityKey, // DI를 HMAC-SHA-256으로 변환한 동일인 식별키

        String name,        // PortOne 재조회로 검증된 이름

        String phone        // PortOne 재조회로 검증된 전화번호
) {
}
