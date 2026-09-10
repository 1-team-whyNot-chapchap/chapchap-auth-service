package com.chapchap.auth.domain.user.entity;

import com.chapchap.auth.domain.auth.constant.ProviderPolicy;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "social_accounts",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_social_accounts_provider_user_id",
                        columnNames = {"provider", "provider_user_id"}
                ),
                @UniqueConstraint(
                        name = "uk_social_accounts_user_provider",
                        columnNames = {"user_id", "provider"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SocialAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "social_account_id")
    private Long id;

    // social_accounts와 users는 같은 Auth-Service DB 내부 관계이므로
    // JPA 연관관계를 사용해도 된다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 20)
    private ProviderPolicy provider;

    // 카카오/구글이 제공하는 소셜 계정 고유 식별자
    @Column(name = "provider_user_id", nullable = false, length = 191)
    private String providerUserId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    // 사용자에게 신규 소셜 로그인 수단 연결
    public static SocialAccount create(
            User user,
            ProviderPolicy provider,
            String providerUserId
    ) {
        SocialAccount socialAccount = new SocialAccount();

        socialAccount.user = user;
        socialAccount.provider = provider;
        socialAccount.providerUserId = providerUserId;

        return socialAccount;
    }
}
