package com.chapchap.auth.domain.auth.entity;

import com.chapchap.auth.global.security.constant.ProviderPolicy;
import com.chapchap.auth.global.security.constant.SignupSessionStatusPolicy;
import com.chapchap.auth.global.error.custom.business.InvalidStateException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "signup_sessions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_signup_sessions_identity_verification_id",
                        columnNames = "identity_verification_id"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SignupSession {

    // 클라이언트에게 전달하는 UUID 형식의 가입 세션 ID
    @Id
    @Column(
            name = "signup_session_id",
            nullable = false,
            length = 36,
            updatable = false
    )
    private String id;

    // 가입을 시작한 소셜 로그인 Provider
    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 20)
    private ProviderPolicy provider;

    // 카카오/구글에서 전달받은 사용자 고유 ID
    @Column(name = "provider_user_id", nullable = false, length = 191)
    private String providerUserId;

    // PortOne 본인인증 요청의 고유 ID
    // 본인인증 전에는 null이다.
    @Column(name = "identity_verification_id", length = 191)
    private String identityVerificationId;

    // DI 원문을 HMAC-SHA-256으로 변환한 임시 동일인 식별키
    // DI 원문 자체는 저장하지 않는다.
    @Column(name = "identity_key", length = 64)
    private String identityKey;

    // 가입 진행 상태
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private SignupSessionStatusPolicy status;

    // 가입 세션 사용 가능 기한
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    // 서버가 PortOne 본인인증 결과를 정상 확인한 시각
    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    // 실제 가입 또는 기존 계정 연결에 사용된 시각
    @Column(name = "consumed_at")
    private LocalDateTime consumedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static SignupSession createPending(
            ProviderPolicy provider,
            String providerUserId
    ) {
        SignupSession signupSession = new SignupSession();

        signupSession.id = UUID.randomUUID().toString();
        signupSession.provider = provider;
        signupSession.providerUserId = providerUserId;
        signupSession.status = SignupSessionStatusPolicy.PENDING;
        signupSession.expiresAt = LocalDateTime.now().plusMinutes(15);

        return signupSession;
    }

    public boolean isExpired() {
        return !expiresAt.isAfter(LocalDateTime.now());
    }
    
    // 유효시간이 지난 진행 중 가입 세션을 EXPIRED 상태로 변경
    public boolean markExpiredIfNeeded() {

        if (!isExpired()) {
            return false;
        }

        if (status != SignupSessionStatusPolicy.PENDING
            &&
            status != SignupSessionStatusPolicy.IDENTITY_VERIFIED
        ) {
            return false;
        }
        this.status = SignupSessionStatusPolicy.EXPIRED;

        return true;
    }
    
    // PortOne/KCP 본인인증 검증 성공 후 가입 세션 상태 변경
    public void markIdentityVerified(
            String identityVerificationId,
            String identityKey
    ) {
        // PENDING 상태에서만 본인인증 완료 상태로 변경할 수 있다.
        if (this.status != SignupSessionStatusPolicy.PENDING) {
            throw new InvalidStateException("PENDING 상태의 가입 세션만 본인인증 완료 처리할 수 있습니다.");
        }

        this.identityVerificationId = identityVerificationId;
        this.identityKey = identityKey;
        this.status = SignupSessionStatusPolicy.IDENTITY_VERIFIED;
        this.verifiedAt = LocalDateTime.now();
    }
    
    // 가입 또는 기존 계정 연결이 모두 성공한 가입 세션을 완료/소비 처리
    public void complete() {

        if (this.status != SignupSessionStatusPolicy.IDENTITY_VERIFIED) {
            throw new InvalidStateException("본인인증이 완료된 가입 세션만 완료 처리할 수 있습니다.");
        }

        if (this.consumedAt != null) {
            throw new InvalidStateException("이미 사용된 가입 세션입니다.");
        }

        this.status = SignupSessionStatusPolicy.COMPLETED;
        this.consumedAt = LocalDateTime.now();
    }

    // 가입 진행 중 복구할 수 없는 실패가 발생한 세션을 FAILED 상태로 변경
    public void markFailed() {
        if (this.status != SignupSessionStatusPolicy.PENDING
                && this.status != SignupSessionStatusPolicy.IDENTITY_VERIFIED
        ) {
            throw new InvalidStateException("진행 중인 가입 세션만 실패 처리할 수 있습니다.");
        }
        this.status = SignupSessionStatusPolicy.FAILED;
    }

}
