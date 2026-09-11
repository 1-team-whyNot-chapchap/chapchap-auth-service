package com.chapchap.auth.global.security.jwt;

import com.chapchap.auth.global.config.security.JwtConfig;

import com.chapchap.auth.domain.user.entity.User;
import com.chapchap.auth.global.error.custom.business.InvalidTokenException;
import com.chapchap.auth.domain.token.constant.SessionTypePolicy;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtProvider {
    private final JwtConfig jwtConfig;
    private final SecretKey secretKey;

    public JwtProvider(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtConfig.secret()));

    }

    public String generateAccessToken(User user, SessionTypePolicy sessionType) {
        return this.generateToken(user, getAccessTokenExpiry(sessionType));
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
            throw new InvalidTokenException("지원하지 않는 토큰입니다.");
        } catch (MalformedJwtException e) {
            throw new InvalidTokenException("토큰 형식이 올바르지 않습니다.");
        } catch (JwtException | IllegalArgumentException e) {
            throw new InvalidTokenException("토큰 검증에 실패했습니다.");
        }
    }

    private int getAccessTokenExpiry(
        SessionTypePolicy sessionType
    ) {
        return switch (sessionType) {
            case USER -> jwtConfig.userAccessTokenExpiry();
            case ADMIN -> jwtConfig.adminAccessTokenExpiry();
        };
    }
}
