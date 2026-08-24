package com.chapchap.auth.domain.user.entity;

import com.chapchap.auth.global.security.constant.RolePolicy;
import com.chapchap.auth.global.security.constant.SubscriptionStatusPolicy;
import com.chapchap.auth.global.security.constant.UserStatusPolicy;
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
}