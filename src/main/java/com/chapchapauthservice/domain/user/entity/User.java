package com.chapchapauthservice.domain.user.entity;

import com.chapchapauthservice.global.security.constant.ProviderPolicy;
import com.chapchapauthservice.global.security.constant.RolePolicy;
import com.chapchapauthservice.global.security.constant.UserStatusPolicy;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "users",
        // 소셜 로그인 계정을 중복 가입하지 못하도록 하는 복합 유니크 제약조건
        // @UniqueConstraint: 여러 컬럼 값을 묶어 중복 여부를 검사
        // provider와 provider_user_id가 모두 같을 때만 중복으로 판단
        // 서로 다른 소셜 제공자의 같은 ID 값은 별도 계정으로 허용
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_users_provider_provider_user_id",
                        columnNames = {"provider", "provider_user_id"}
                )
        }
)
@Getter
@SQLDelete(sql = "UPDATE users SET deleted_at = NOW() WHERE user_id = ?")
@FilterDef(name = "softDelete")
@Filter(name = "softDelete", condition = "deleted_at IS NULL")
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProviderPolicy provider;

    @Column(name = "provider_user_id", nullable = false, length = 100)
    private String providerUserId;

    @Column(length = 255)
    private String email;

    @Column(nullable = false, length = 50)
    private String nickname;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RolePolicy role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatusPolicy status;

    @Column(name = "default_address_id")
    private Long defaultAddressId;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    // 소셜 로그인으로 처음 접속한 사용자를 생성
    public static User createSocialUser(
            ProviderPolicy provider,
            String providerUserId,
            String email,
            String nickname,
            String profileImageUrl
    ) {
        User user = new User();

        user.provider = provider;
        user.providerUserId = providerUserId;
        user.email = email;
        user.nickname = nickname;
        user.profileImageUrl = profileImageUrl;
        
        // 최초 가입한 사용자는 일반 고객/정상 상태로 시작
        user.role = RolePolicy.CUSTOMER;
        user.status = UserStatusPolicy.ACTIVE;

        return user;
    }

    // 소셜 로그인으로 다시 접속한 탈퇴 계정을 복구
    public void restore() {
        this.deletedAt = null;
    }

}
