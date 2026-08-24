package com.chapchap.auth.domain.user.entity;

import com.chapchap.auth.global.security.constant.RolePolicy;
import com.chapchap.auth.global.security.constant.SubscriptionStatusPolicy;
import com.chapchap.auth.global.security.constant.UserStatusPolicy;
import com.chapchap.auth.global.error.custom.business.InvalidParameterException;
import com.chapchap.auth.global.error.custom.business.InvalidStateException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    // 본인인증 DI를 HMAC-SHA-256으로 변환한 동일인 식별키
    // 일반 사용자는 사용하지만 관리자 계정은 null일 수 있다.
    @Column(name = "identity_key", length = 64, unique = true)
    private String identityKey;

    // 대표 배송지는 Subscription-Service가 소유한다.
    // Auth-Service는 ID Projection만 저장한다.
    @Column(name = "default_address_id")
    private Long defaultAddressId;

    // 대표 배송지 Event의 최신성 판단용 Business Version
    @Column(name = "default_address_version", nullable = false)
    private Long defaultAddressVersion = 0L;

    // Subscription-Service에서 전달받은 구독 상태 Projection
    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_status", nullable = false, length = 20)
    private SubscriptionStatusPolicy subscriptionStatus =
            SubscriptionStatusPolicy.INACTIVE;

    // 구독 상태 Event의 최신성 판단용 Business Version
    @Column(name = "subscription_version", nullable = false)
    private Long subscriptionVersion = 0L;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "email", length = 255)
    private String email;

    // MinIO 파일 URL이 아니라 Object Key만 저장한다.
    @Column(name = "profile_image_key", length = 255)
    private String profileImageKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private RolePolicy role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UserStatusPolicy status;

    @Column(name = "identity_verified_at")
    private LocalDateTime identityVerifiedAt;

    @Column(name = "withdrawn_at")
    private LocalDateTime withdrawnAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    // 본인인증이 완료된 신규 일반 사용자 생성
    public static User createCustomer(
            String identityKey,
            String name,
            String phone,
            String email,
            LocalDateTime identityVerifiedAt
    ) {
        User user = new User();

        user.identityKey = identityKey;

        user.defaultAddressId = null;
        user.defaultAddressVersion = 0L;

        user.subscriptionStatus = SubscriptionStatusPolicy.INACTIVE;
        user.subscriptionVersion = 0L;

        user.name = name;
        user.phone = phone;
        user.email = email;

        user.profileImageKey = null;

        user.role = RolePolicy.CUSTOMER;
        user.status = UserStatusPolicy.ACTIVE;

        user.identityVerifiedAt = identityVerifiedAt;
        user.withdrawnAt = null;

        return user;
    }

    public static User createAdministrator(String name, RolePolicy role) {
        if (role != RolePolicy.ADMIN && role != RolePolicy.SUPER_ADMIN) {
            throw new InvalidParameterException("관리자 계정은 ADMIN 또는 SUPER_ADMIN 역할이어야 합니다.");
        }
        User user = new User();
        user.defaultAddressVersion = 0L;
        user.subscriptionStatus = SubscriptionStatusPolicy.INACTIVE;
        user.subscriptionVersion = 0L;
        user.name = name;
        user.profileImageKey = null;
        user.role = role;
        user.status = UserStatusPolicy.ACTIVE;
        return user;
    }

    public void changeProfileImageKey(String profileImageKey) {
        this.profileImageKey = profileImageKey;
    }

    public void withdraw() {
        this.identityKey = null;
        this.defaultAddressId = null;
        this.defaultAddressVersion = 0L;
        this.subscriptionStatus = SubscriptionStatusPolicy.UNKNOWN;
        this.subscriptionVersion = 0L;
        this.name = "탈퇴회원";
        this.phone = null;
        this.email = null;
        this.profileImageKey = null;
        this.role = RolePolicy.CUSTOMER;
        this.status = UserStatusPolicy.WITHDRAWN;
        this.identityVerifiedAt = null;
        this.withdrawnAt = LocalDateTime.now();
    }

    public void suspendAdministrator() {
        if (role != RolePolicy.ADMIN) {
            throw new InvalidStateException("ADMIN 계정만 비활성화할 수 있습니다.");
        }
        this.status = UserStatusPolicy.SUSPENDED;
    }

    public void changeGeneralRole(RolePolicy targetRole) {
        if ((role != RolePolicy.CUSTOMER && role != RolePolicy.RIDER)
                || (targetRole != RolePolicy.CUSTOMER && targetRole != RolePolicy.RIDER)
                || status != UserStatusPolicy.ACTIVE) {
            throw new InvalidStateException("활성 일반 사용자 역할만 변경할 수 있습니다.");
        }
        this.role = targetRole;
    }

    /**
     * Subscription-Service가 소유한 대표 주소의 최소 Projection만 갱신한다.
     * 같은 사용자에 대한 중복/역순 Kafka 전달은 업무 버전으로 무시한다.
     */
    public boolean updateDefaultAddressProjection(Long addressId, long addressVersion) {
        if (addressVersion <= defaultAddressVersion) {
            return false;
        }
        this.defaultAddressId = addressId;
        this.defaultAddressVersion = addressVersion;
        return true;
    }

    /**
     * Subscription-Service가 확정한 구독 상태의 최소 Projection만 갱신한다.
     */
    public boolean updateSubscriptionProjection(SubscriptionStatusPolicy subscriptionStatus, long subscriptionVersion) {
        if (subscriptionVersion <= this.subscriptionVersion) {
            return false;
        }
        this.subscriptionStatus = subscriptionStatus;
        this.subscriptionVersion = subscriptionVersion;
        return true;
    }
}
