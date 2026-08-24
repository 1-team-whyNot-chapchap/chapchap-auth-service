package com.chapchapauthservice.domain.auth.service;

import com.chapchapauthservice.domain.auth.dto.IssuedRefreshToken;
import com.chapchapauthservice.domain.auth.dto.ReissuedToken;
import com.chapchapauthservice.domain.token.entity.AuthSession;
import com.chapchapauthservice.domain.token.entity.RefreshToken;
import com.chapchapauthservice.domain.token.repository.AuthSessionRepository;
import com.chapchapauthservice.domain.token.repository.RefreshTokenRepository;
import com.chapchapauthservice.domain.user.entity.User;
import com.chapchapauthservice.global.error.custom.business.InvalidTokenException;
import com.chapchapauthservice.global.jwt.JwtProvider;
import com.chapchapauthservice.global.security.constant.RolePolicy;
import com.chapchapauthservice.global.security.constant.SessionTypePolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtProvider jwtProvider;
    private final AuthSessionRepository authSessionRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    // 쿠키로 전달받은 리프레시 토큰으로 새 토큰을 발급한다.
    // 이미 사용된 토큰이 다시 들어오면 해당 로그인 세션 전체를 폐기한다.
    @Transactional(noRollbackFor = InvalidTokenException.class)
    public ReissuedToken reissueRefreshToken(String refreshToken) {
        // 쿠키의 토큰 원문을 DB 조회용 해시값으로 변환
        String tokenHash = jwtProvider.hashRefreshToken(refreshToken);
        
        // 잠금 조회로 동시에 들어온 재발급 요청을 순서대로 처리
        RefreshToken savedRefreshToken = refreshTokenRepository
                .findByTokenHashForUpdate(tokenHash)
                .orElseThrow(() -> new InvalidTokenException("존재하지 않는 리프레시 토큰입니다."));

        AuthSession authSession = savedRefreshToken.getAuthSession();

        // consumed_at이 있으면 이미 재발급에 사용된 토큰이다.
        // 토큰 탈취 후 재사용했을 가능성이 있으므로 세션 전체를 폐기한다.
        if (savedRefreshToken.getConsumedAt() != null) {
            authSession.revoke();
            authSessionRepository.save(authSession);

            throw new InvalidTokenException("이미 사용된 리프레시 토큰입니다.");
        }

        // 토큰 또는 세션이 만료/폐기 되었다면 재발급을 거절
        if (savedRefreshToken.isExpired() || !authSession.isUsable()) {
            throw new InvalidTokenException("만료되었거나 사용할 수 없는 리프레시 토큰입니다.");
        }
        
        // 기존 토큰을 사용 완료 처리하고, 같은 세션에 새 리프레시 토큰을 연결
        savedRefreshToken.consume();
        IssuedRefreshToken issuedRefreshToken = createRefreshToken(authSession);
        
        // 새 액세스 토큰은 응답 본문으로 전달할 값
        String accessToken = jwtProvider.generateAccessToken(authSession.getUser());

        return new ReissuedToken(
                accessToken,
                issuedRefreshToken.refreshToken(),
                issuedRefreshToken.sessionType()
        );
    }












    // OAuth2 로그인 성공 후 리프레시 토큰을 발급하고 DB에 세션 정보를 저장한다
    // 세션/토큰 저장이 한 묶음으로 성공하거나 실패하도록 Transactional를 적용한다.
    @Transactional
    public IssuedRefreshToken issueRefreshToken(User user) {
        // 사용자 권한에 따라 일반 서비스 또는 관리자 사이트 세션으로 구분
        SessionTypePolicy sessionType = getSessionType(user);
        
        // JWT 만료 시간과 DB 세션 만료 시간을 동일한 기준으로 설정
        LocalDateTime expiresAt = getSessionExpiresAt(sessionType);
        
        // 로그인 세션을 먼저 생성한 뒤, 리프레시 토큰을 해당 세션에 연결
        AuthSession authSession = authSessionRepository.save(
                AuthSession.create(user, sessionType.getSessionType(), expiresAt,)
        );

        return createRefreshToken(authSession);
    }

    // 전달받은 세션에 리프레시 토큰을 하나 발급해 연결한다
    // DB 에는 토큰 원문 대신 해시값만 저장한다
    private IssuedRefreshToken createRefreshToken(AuthSession authSession) {
        String refreshToken = jwtProvider.generateRefreshToken(
                authSession.getUser(),
                authSession.getSessionType()
        );

        String tokenHash = jwtProvider.hashRefreshToken(refreshToken);

        refreshTokenRepository.save(
                RefreshToken.create(
                        authSession,
                        tokenHash,
                        authSession.getExpiresAt()
                )
        );

        return new IssuedRefreshToken(refreshToken, authSession.getSessionType());
    }
    
    // ADMIN, SUPER_ADMIN은 관리자 세션으로 분류하며
    // 그 외에 CUSTOMER, RIDER는 일반 사용자 세션으로 처리한다
    private SessionTypePolicy getSessionType(User user) {
        RolePolicy role = user.getRole();

        return switch (role) {
            case ADMIN, SUPER_ADMIN -> SessionTypePolicy.ADMIN;
            default -> SessionTypePolicy.USER;
        };
    }
    
    // 세션 종류에 따라 DB 세션의 만료 시작을 계산한다
    // User는 14일, ADMIN은 30분 동안 로그인 상태를 유지한다
    private LocalDateTime getSessionExpiresAt(SessionTypePolicy sessionType) {
        return switch (sessionType) {
            case USER -> LocalDateTime.now().plusDays(14);
            case ADMIN -> LocalDateTime.now().plusMinutes(30);
        };
    }
    
}
