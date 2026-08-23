package com.chapchapauthservice.global.security.token;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Component
public class RefreshTokenHasher {
    
    // 리프레시 토큰 원문을 SHA-256으로 해시하여 DB 저장/조회용 값으로 변환
    public String hash(String refreshToken) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            
            // 문자열 토큰을 UTF-8 바이트로 변환한 뒤 SHA-256 해시 처리
            byte[] hashBytes = messageDigest.digest(
                    refreshToken.getBytes(StandardCharsets.UTF_8)
            );
            
            // SHA-256 결과 바이트를 64자리 16진수 문자열로 변환
            return toHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256은 Java 기본 제공 알고리즘이므로 발생하면 서버 설정 문제로 처리
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", e);
        }
    }

    // 해시 바이트 배열을 DB에 저장할 수 있는 16진수 문자열로 변환
    private String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder();

        for (byte value : bytes) {
            builder.append(String.format("%02x", value));
        }

        return builder.toString();
    }
}
