package com.chapchapauthservice.domain.auth.service;

import com.chapchapauthservice.domain.auth.dto.IssuedRefreshToken;
import com.chapchapauthservice.domain.auth.dto.ReissuedToken;
import com.chapchapauthservice.domain.token.entity.AuthSession;
import com.chapchapauthservice.domain.token.entity.RefreshToken;
import com.chapchapauthservice.domain.token.repository.AuthSessionRepository;
import com.chapchapauthservice.domain.token.repository.RefreshTokenRepository;
import com.chapchapauthservice.domain.token.service.AuthSessionService;
import com.chapchapauthservice.domain.user.entity.User;
import com.chapchapauthservice.global.error.custom.business.InvalidTokenException;
import com.chapchapauthservice.global.jwt.JwtProvider;
import com.chapchapauthservice.global.security.constant.RolePolicy;
import com.chapchapauthservice.global.security.constant.SessionTypePolicy;
import com.chapchapauthservice.global.security.token.RefreshTokenGenerator;
import com.chapchapauthservice.global.security.token.RefreshTokenHasher;
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
    private final RefreshTokenHasher refreshTokenHasher;
    private final AuthSessionService authSessionService;
    private final RefreshTokenGenerator refreshTokenGenerator;

    // 쿠키로 전달받은 리프레시 토큰으로 새 토큰을 발급한다.
    // 이미 사용된 토큰이 다시 들어오면 해당 로그인 세션 전체를 폐기한다.
    @Transactional(noRollbackFor = InvalidTokenException.class)
    public ReissuedToken reissueRefreshToken(String refreshToken) {
        // 쿠키의 토큰 원문을 DB 조회용 해시값으로 변환
        String tokenHash = refreshTokenHasher.hash(refreshToken);
        
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
        
        // 기존 리프레시 토큰을 재사용할 수 없도록 소비 처리
        savedRefreshToken.consume();

        // 일반 사용자 세션이면 유휴 만료 시작을 연장
        // 관리자 세션은 AuthSessionService 내부에서 연장하지 않는다.
        authSessionService.extendUserIdleExpiration(
            authSession
        );

        // 갱신된 세션 만료 시작을 기준으로 새로운 리프레시 토큰 발급
        IssuedRefreshToken issuedRefreshToken = createRefreshToken(authSession);
        
        // 새 액세스 토큰은 응답 본문으로 전달할 값
        String accessToken = jwtProvider.generateAccessToken(
            authSession.getUser(),
            authSession.getSessionType()
            );

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

        // 세션 만료 정책 계산과 저장은 AuthSessionService가 담당
        AuthSession authSession = authSessionService.createSession(
            user,
            sessionType
        );

        // 생성한 세션에 최초 리프레시 토큰 연결
        return createRefreshToken(authSession);
    }

    // 전달받은 세션에 리프레시 토큰을 하나 발급해 연결한다
    // DB 에는 토큰 원문 대신 해시값만 저장한다
    private IssuedRefreshToken createRefreshToken(AuthSession authSession) {
        // 사용자에게 전달할 예측 불가능한 리프레시 토큰 원문 생성
        String refreshToken = refreshTokenGenerator.generate();

        // DB 저장 및 조회를 위한 SHA-256 + Base64 해시 생성
        String tokenHash = refreshTokenHasher.hash(refreshToken);

        // 리프레시 토큰은 현재 세션의 유휴 만료 시각까지만 사용 가능
        RefreshToken tokenEntity = RefreshToken.create(
            authSession,
            tokenHash,
            authSession.getIdleExpiresAt()
        );

        refreshTokenRepository.save(tokenEntity);

        return new IssuedRefreshToken(
            refreshToken,
            authSession.getSessionType()
        );
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
}
