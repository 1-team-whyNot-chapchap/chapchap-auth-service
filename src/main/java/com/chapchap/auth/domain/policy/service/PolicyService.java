package com.chapchap.auth.domain.policy.service;

import com.chapchap.auth.domain.policy.entity.Policy;
import com.chapchap.auth.domain.policy.repository.PolicyRepository;
import com.chapchap.auth.domain.policy.response.CurrentPolicyResponse;
import com.chapchap.auth.domain.policy.constant.PolicyTypePolicy;
import com.chapchap.auth.global.error.custom.business.InvalidParameterException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PolicyService {
    private final PolicyRepository policyRepository;

    @Transactional(readOnly = true)
    public List<CurrentPolicyResponse> getCurrentSignupPolicies() {
        return getCurrentPolicies().stream()
                .map(CurrentPolicyResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Policy getCurrentMarketingPolicy(Long policyId) {
        Policy policy = getCurrentPolicies().stream()
                .filter(current -> current.getPolicyType() == PolicyTypePolicy.MARKETING_EMAIL)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("현재 이메일 마케팅 정책이 없습니다."));
        if (!policy.getId().equals(policyId)) {
            throw new InvalidParameterException("현재 이메일 마케팅 정책만 변경할 수 있습니다.");
        }
        return policy;
    }

    public List<Policy> getCurrentPolicies() {
        List<Policy> policies = policyRepository.findAllByActiveTrueAndEffectiveAtLessThanEqual(LocalDateTime.now());
        Map<PolicyTypePolicy, Policy> policyByType = new EnumMap<>(PolicyTypePolicy.class);
        for (Policy policy : policies) {
            if (policyByType.put(policy.getPolicyType(), policy) != null) {
                throw new IllegalStateException("같은 종류의 현재 정책이 여러 개입니다.");
            }
        }
        for (PolicyTypePolicy policyType : PolicyTypePolicy.values()) {
            if (!policyByType.containsKey(policyType)) {
                throw new IllegalStateException("가입에 필요한 현재 정책이 없습니다: " + policyType);
            }
        }
        return policies;
    }
}
