package com.chapchapauthservice.domain.policy.repository;

import com.chapchapauthservice.domain.policy.entity.Policy;
import com.chapchapauthservice.domain.policy.entity.UserPolicyConsent;
import com.chapchapauthservice.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserPolicyConsentRepository extends JpaRepository<UserPolicyConsent, Long> {
    // 특정 사용자의 특정 정책 Version에 대한 선택 기록 조회
    Optional<UserPolicyConsent> findByUserAndPolicy(User user, Policy policy);
}
