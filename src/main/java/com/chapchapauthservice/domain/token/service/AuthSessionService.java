package com.chapchapauthservice.domain.token.service;

import com.chapchapauthservice.domain.token.entity.AuthSession;
import com.chapchapauthservice.domain.token.repository.AuthSessionRepository;
import com.chapchapauthservice.domain.user.entity.User;
import com.chapchapauthservice.global.security.constant.SessionTypePolicy;
import com.chapchapauthservice.global.security.policy.AuthSessionPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthSessionService {

    private final AuthSessionPolicy authSessionPolicy;
    private final AuthSessionRepository authSessionRepository;

    // 로그인 성공 시 USER 또는 ADMIN 정책에 맞는 새로운 인증 세션 생성
    public AuthSession createSession(
            User user,
            SessionTypePolicy sessionType
    ) {
        LocalDateTime now = LocalDateTime.now();
        
        // 세션 종류에 따라 유휴 만료 시각 계산
        LocalDateTime idleExpiresAt = authSessionPolicy.calculateIdleExpiresAt(sessionType,now);
        
        // 세션 종류에 따라 최초 로그인 기준 절대 만료 시작 계산
        LocalDateTime absoluteExpiresAt = authSessionPolicy.calculateAbsoluteExpiresAt(sessionType,now);
        
        // 계산된 만료시간을 기준으로 새로운 로그인 세션 생성
        AuthSession authSession = AuthSession.create(
                user,
                sessionType,
                idleExpiresAt,
                absoluteExpiresAt,
                now
        );
        // 생성한 세션을 auth_sessions 테이블에 저장
        return authSessionRepository.save(authSession);
    }
}
