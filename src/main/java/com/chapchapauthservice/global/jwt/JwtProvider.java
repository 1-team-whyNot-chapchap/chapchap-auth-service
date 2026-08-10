package com.chapchapauthservice.global.jwt;

import com.chapchapauthservice.domain.user.entity.User;
import com.chapchapauthservice.global.error.custom.business.InvalidTokenException;
import com.chapchapauthservice.global.security.constant.SessionTypePolicy;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Date;

@Component
public class JwtProvider {
    private final JwtConfig jwtConfig;
    private final SecretKey secretKey;

    public JwtProvider(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtConfig.secret()));

    }

    public String generateAccessToken(User user) {
        return this.generateToken(user, jwtConfig.accessTokenExpiry());
    }

    // 로그인한 서비스 영역에 맞는 만료 시간으로 리프레시 토큰을 생성한다.
    // USER는 14일, ADMIN은 30분 정책을 적용한다.
    public String generateRefreshToken(User user, SessionTypePolicy sessionType) {
        return this.generateToken(user, getRefreshTokenExpiry(sessionType));
    }

    // 세션 종류에 따라 JwtConfig에 설정한 리프레시 토큰 만료 시간을 선택한다
    // 설정값은 미리초 단위이므로 그대로 JWT 생성 시간에 전달된다
    public int getRefreshTokenExpiry(SessionTypePolicy sessionType) {
        return switch (sessionType) {
            case USER -> jwtConfig.userRefreshTokenExpiry();
            case ADMIN -> jwtConfig.adminRefreshTokenExpiry();
        };
    }

    private String generateToken(User user, int ttl) {
        Date now = new Date();
        return Jwts.builder()
                   .header() // 헤더를 셋팅하겠다
                   .type(jwtConfig.type()) // 토큰의 유형 셋팅
                   .and()
                   .subject(String.valueOf(user.getId())) // sub 셋팅
                   .issuer(jwtConfig.issuer()) // 토큰 발급자 셋팅
                   .issuedAt(now) // 토큰 발급시간
                   .expiration(new Date(now.getTime() + ttl)) // 토큰 만료 시간 설정
                   .claim("role", user.getRole().name()) // Private Claim 설정
                   .signWith(secretKey) // 시그니쳐 작성
                   .compact();
    }
    public Claims extractClaims(String token) {
        try{
            return Jwts.parser()
                       .verifyWith(this.secretKey)
                       .build()
                       .parseSignedClaims(token)
                       .getPayload();
        } catch (ExpiredJwtException e) {
            throw new InvalidTokenException("토큰이 만료됐습니다.");
        } catch (UnsupportedJwtException e) {
            throw new InvalidTokenException("서명이 위조된 토큰입니다.");
        } catch (MalformedJwtException e) {
            throw new InvalidTokenException("토큰 형식이 올바르지 않습니다.");
        } catch (JwtException | IllegalArgumentException e) {
            throw new InvalidTokenException("토큰 검증에 실패했습니다.");
        }
    }
    
    // 리프레시 토큰 원문을 DB 저장용 SHA-256 해시값으로 변환한다.
    // DB 에는 해시값만 저장해 토큰 원문 유출 위험을 줄인다
    public String hashRefreshToken(String refreshToken) {
        try {

            // SHA-256 해시 알고리즘으로 토큰 원문을 되돌릴 수 없는 값으로 변환
            // 변환된 byte[] 결과는 이후 Base64 문자열로 바꿔 DB에 저장한다.
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");

            byte[] hashBytes = messageDigest.digest(
                    refreshToken.getBytes(StandardCharsets.UTF_8)
            );

            // byte[] 형태의 해시값을 DB에 저장 가능한 문자열로 변환
            return Base64.getEncoder().encodeToString(hashBytes);
        }  catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", e);
        }
    }
}
