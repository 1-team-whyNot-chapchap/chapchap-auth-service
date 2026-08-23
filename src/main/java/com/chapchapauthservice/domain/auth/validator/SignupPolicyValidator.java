package com.chapchapauthservice.domain.auth.validator;

import com.chapchapauthservice.domain.auth.request.PolicyConsentRequest;
import com.chapchapauthservice.domain.policy.entity.Policy;
import com.chapchapauthservice.global.security.constant.PolicyTypePolicy;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class SignupPolicyValidator {

    // 현재 활성 정책과 사용자가 제출한 정책 선택이 회원가입 조건을 만족하는지 검증한다.
    public void validate(
            List<Policy> activePolicies,
            List<PolicyConsentRequest> requests
    ) {
        if (activePolicies == null || activePolicies.isEmpty()) {
            throw new IllegalStateException("현재 적용 가능한 정책이 없습니다.");
        }

        // policyId를 기준으로 사용자의 선택을 빠르게 찾기 위한 Map
        // HashMap: Key와 Value를 한 쌍으로 저장하며,
        // Key를 이용해 원하는 값을 빠르게 조회할 수 있는 자료구조
        Map<Long, PolicyConsentRequest> requestByPolicyId = new HashMap<>();

        for (PolicyConsentRequest request : requests) {

            // 같은 정책 ID가 요청에 두번 들어오는 것을 방지
            if (requestByPolicyId.put(request.policyId(), request) != null) {
                throw new IllegalStateException("동일한 정책이 중복 제출되었습니다.");
            }
        }

        // 같은 policyType의 현재 활성 Version이 여러 개 존재하는지 확인
        // HashSet: 중복을 허용하지 않으며,
        // 값이 이미 존재하는지 빠르게 확인할 수 있는 자료구조
        Set<PolicyTypePolicy> activePolicyTypes = new HashSet<>();
        
        // 현재 활성 정책 ID 목록
        Set<Long> activePolicyIds = new HashSet<>();

        for (Policy policy : activePolicies) {
            if (!activePolicyTypes.add(policy.getPolicyType())) {
                throw new IllegalStateException("동일한 종류의 활성 정책이 여러 개 존재합니다.");
            }
            activePolicyIds.add(policy.getId());

            PolicyConsentRequest request = requestByPolicyId.get(policy.getId());

            // 필수 정책은 반드시 제출되어야 하고 AGREED 여야 한다.
            if (policy.isRequired() && (request == null || !Boolean.TRUE.equals(request.agreed()))) {
                throw new IllegalStateException("필수 정책에 모두 동의해야 합니다.");
            }
        }
        
        // 사용자가 과거 Version 또는 비활성 정책 ID를 제출했는지 확인
        for (PolicyConsentRequest request : requests) {
            if (!activePolicyIds.contains(request.policyId())) {
            throw new IllegalStateException("현재 적용되지 않는 정책이 포함되어 있습니다.");
            }
        }
    }
}
