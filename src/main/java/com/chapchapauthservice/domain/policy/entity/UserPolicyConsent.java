package com.chapchapauthservice.domain.policy.entity;

import com.chapchapauthservice.domain.user.entity.User;
import com.chapchapauthservice.global.security.constant.ConsentStatusPolicy;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "user_policy_consents",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_policy_consents_user_policy",
                        columnNames = {"user_id", "policy_id"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserPolicyConsent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "consent_id")
    private Long id;

    // 정책을 선택한 사용자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false) // users.user_id를 참조하는 외래키 컬럼
    private User user;

    // 사용자가 선택한 정확한 정책 Version
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id", nullable = false) // policies.policy_id를 참조하는 왜래키 컬럼
    private Policy policy;

    // 동의 / 거절 / 철회 상태
    @Enumerated(EnumType.STRING)
    @Column(name = "consent_status", nullable = false, length = 20)
    private ConsentStatusPolicy consentStatus;

    // 사용자가 가장 최근에 의사를 결정한 시각
    @Column(name = "decided_at", nullable = false)
    private LocalDateTime decidedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 회원가입 시 사용자의 최초 정책 선택 기록 생성
    public static UserPolicyConsent createInitial(
            User user,
            Policy policy,
            boolean agreed
    ) {
        UserPolicyConsent consent = new UserPolicyConsent();

        consent.user = user;
        consent.policy = policy;
        // 삼항연산자 True면 AGREED False면 DECLINED를 뜻한다.
        consent.consentStatus = agreed ? ConsentStatusPolicy.AGREED : ConsentStatusPolicy.DECLINED;

        consent.decidedAt = LocalDateTime.now();

        return consent;
    }
    
    // 이메일 마케팅 동의 또는 철회 상태 변경
    public void changeMarketingConsent(boolean agreed) {
        // 삼항연산자를 안쓰는 이유는 DECLINED -> 철회 요청 -> WITHDRAWN이 될수도 있기때문에 내가 설계한 흐름에 맞지않아 if로 분개한다.
        if (agreed) {
            this.consentStatus = ConsentStatusPolicy.AGREED;
        } else {
            // 실제 동의한 상태에서만 철회할 수 있다.
            if (this.consentStatus != ConsentStatusPolicy.AGREED) {
                throw new IllegalStateException("동의된 마케팅 정책만 철회할 수 있습니다.");
            }
            this.consentStatus =ConsentStatusPolicy.WITHDRAWN;
        }
        this.decidedAt = LocalDateTime.now();
    }
}
