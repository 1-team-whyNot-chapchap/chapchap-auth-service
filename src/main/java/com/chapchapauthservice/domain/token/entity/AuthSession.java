package com.chapchapauthservice.domain.token.entity;

import com.chapchapauthservice.domain.user.entity.User;
import com.chapchapauthservice.global.security.constant.SessionTypePolicy;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;


@Entity
@Table(name = "auth_sessions")
@Getter
// JPA의 객체 생성에는 필요하지만, 외부에서 빈 엔티티를 직접 만들지는 못하게 하는 기본 생성자
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuthSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_id")
    private Long id;

    // 여러 로그인 세션이 한 명의 사용자를 공유하는 N:1 관계
    // 세션 조회 시 사용자 정보는 필요할 때만 가져오도록 LAZY 로딩 적용
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 일반 서비스 로그인인지, 관리자 사이트 로그인인지 구분
    @Enumerated(EnumType.STRING)
    @Column(name = "session_type", nullable = false, length = 20)
    private SessionTypePolicy sessionType;

    // 이 시간이 지나면 리프레시 토큰이 남아 있어도 세션은 사용할 수 없다.
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    // null이면 정상 세션, 값이 있으면 로그아웃·토큰 재사용 등으로 폐기된 세션
    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    // 세션이 최초 생성된 시각
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 로그인 성공 시 새 세션 생성
    public static AuthSession create(
            User user,
            SessionTypePolicy sessionType,
            LocalDateTime expiresAt
    ) {
        AuthSession authSession = new AuthSession();

        authSession.user = user;
        authSession.sessionType = sessionType;
        authSession.expiresAt = expiresAt;

        return authSession;
    }
    
    // 로그아웃 또는 토큰 재사용이 감지되면 세션 전체를 폐기
    public void revoke() {
        this.revokedAt = LocalDateTime.now();
    }

    // 세션이 폐기되지 않았고 만료 시간도 지나지 않았는지 확인한다
    // 리프레시 토큰 재발급 전에 로그인 세션의 유효성을 검사할 때 사용한다
    public boolean isUsable() {
        return revokedAt == null && expiresAt.isAfter(LocalDateTime.now());
    }
}
