package com.chapchap.auth.domain.token.entity;

import com.chapchap.auth.domain.user.entity.User;
import com.chapchap.auth.domain.token.constant.SessionTypePolicy;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "auth_sessions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuthSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_id")
    private Long id;

    // User 엔티티와 연결하는 FK 컬럼
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 일반 사용자 세션(USER)과 관리자 세션(ADMIN)을 구분
    @Enumerated(EnumType.STRING)
    @Column(name = "session_type", nullable = false, length = 20)
    private SessionTypePolicy sessionType;

    // 마지막 정상 사용 이후 허용되는 유휴 만료 시각
    @Column(name = "idle_expires_at", nullable = false)
    private LocalDateTime idleExpiresAt;

    // 최초 로그인 시점을 기준으로 더 이상 연장할 수 없는 절대 만료 시각
    @Column(name = "absolute_expires_at", nullable = false)
    private LocalDateTime absoluteExpiresAt;

    // Refresh Token 재발급 등 마지막 정상 사용 시각
    @Column(name = "last_used_at", nullable = false)
    private LocalDateTime lastUsedAt;

    // null이면 활성 세션, 값이 있으면 로그아웃·탈퇴·Token 재사용 등으로 폐기된 세션
    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    // 로그인 세션이 최초 생성된 시각
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;


    // 로그인 성공 시 새로운 인증 세션 생성
    public static AuthSession create(
            User user,
            SessionTypePolicy sessionType,
            LocalDateTime idleExpiresAt,
            LocalDateTime absoluteExpiresAt,
            LocalDateTime lastUsedAt
    ) {
        AuthSession authSession = new AuthSession();

        authSession.user = user;
        authSession.sessionType = sessionType;
        authSession.idleExpiresAt = idleExpiresAt;
        authSession.absoluteExpiresAt = absoluteExpiresAt;
        authSession.lastUsedAt = lastUsedAt;

        return authSession;
    }

    // 로그아웃·탈퇴·역할 변경·Token 재사용 등의 경우 세션 폐기
    public void revoke() {
        this.revokedAt = LocalDateTime.now();
    }

    // 폐기되지 않았으며 유휴 만료와 절대 만료가 모두 지나지 않았는지 확인
    public boolean isUsable() {
        LocalDateTime now = LocalDateTime.now();

        return revokedAt == null
                && idleExpiresAt.isAfter(now)
                && absoluteExpiresAt.isAfter(now);
    }

    // 일반 사용자 Refresh Token 재발급 성공 시 마지막 사용 시각과 유휴 만료 시각 갱신
    public void extendIdleExpiration(LocalDateTime lastUsedAt, LocalDateTime idleExpiresAt) {
        this.lastUsedAt = lastUsedAt;
        this.idleExpiresAt = idleExpiresAt;
    }
}