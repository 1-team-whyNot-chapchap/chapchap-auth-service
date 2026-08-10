package com.chapchapauthservice.domain.token.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;


// 리프레시 토큰 원문 대신 해시값을 저장하는 테이블
// 한 세션에서 토큰을 재발급할 때마다 새로운 행이 생성된다
@Entity
@Table(name = "refresh_tokens")
@Getter
// JPA의 객체 생성에는 필요하지만, 외부에서 빈 엔티티를 직접 만들지는 못하게 하는 기본 생성자
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "refresh_token_id")
    private Long id;

    // 여러 리프레시 토큰은 하나의 로그인 세션에 연결되는 N:1 관계
    // 토큰 조회 시 세션 정보는 필요할 때만 가져오도록 LAZY 로딩 적용
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private AuthSession authSession;

    /*
     * 실제 토큰 문자열은 DB에 저장하지 않는다.
     * 토큰을 해시 처리한 값으로만 비교해 유출 피해를 줄인다.
     */
    @Column(name = "token_hash", nullable = false, unique = true, length = 255)
    private String tokenHash;

    // 토큰 자체의 만료 시각으로, 세션이 살아 있어도 이 시간이 지나면 사용할 수 없다.
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    // null이면 아직 사용 전, 값이 있으면 재발급에 사용된 토큰
    @Column(name = "consumed_at")
    private LocalDateTime consumedAt;

    // 리프레시 토큰이 최초 발급된 시각
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 로그인 또는 토큰 재발급 시 새 리프래시 토큰 생성
    public static RefreshToken create(
            AuthSession authSession,
            String tokenHash,
            LocalDateTime expiresAt
    ) {
        RefreshToken refreshToken = new RefreshToken();

        refreshToken.authSession = authSession;
        refreshToken.tokenHash = tokenHash;
        refreshToken.expiresAt = expiresAt;

        return refreshToken;
    }

    // 정상 재발급에 사용된 기존 토큰을 재사용 불가 상태로 변경
    public void consume() {
        this.consumedAt = LocalDateTime.now();
    }

    // 리프레시 토큰 자체의 만료 여부를 확인한다
    // 세션이 살아 있어도 토큰 만료 시간이 지나면 재발급할 수 없다
    public boolean isExpired() {
        return !expiresAt.isAfter(LocalDateTime.now());
    }
}
