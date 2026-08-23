package com.chapchapauthservice.domain.policy.entity;

import com.chapchapauthservice.global.security.constant.PolicyTypePolicy;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "policies",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_policies_policy_type_version",
                        columnNames = {"policy_type", "version"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Policy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "policy_id")
    private Long id;

    // 정책 종류
    @Enumerated(EnumType.STRING)
    @Column(name = "policy_type", nullable = false, length = 30)
    private PolicyTypePolicy policyType;

    // 사용자에게 표시되는 정책 Version
    @Column(name = "version", nullable = false, length = 20)
    private String version;

    // 가입 화면 등에 표시되는 정책 제목
    @Column(name = "title", nullable = false, length = 100)
    private String title;

    // 해당 Version의 정책 원문
    @Lob // Large Object의 줄임말 큰 데이터를 저장하는 용도라고 알려주는 어노테이션
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    // 가입 시 반드시 동의해야 하는 정책인지 여부
    @Column(name = "is_required", nullable = false)
    private boolean required;

    // 현재 신규 가입에 사용되는 정책인지 여부
    @Column(name = "is_active", nullable = false)
    private boolean active;

    // 정책 효력이 실제로 시작되는 시각
    @Column(name = "effective_at", nullable = false)
    private LocalDateTime effectiveAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
