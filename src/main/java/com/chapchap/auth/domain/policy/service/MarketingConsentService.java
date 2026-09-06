package com.chapchap.auth.domain.policy.service;

import com.chapchap.auth.domain.policy.entity.Policy;
import com.chapchap.auth.domain.policy.entity.UserPolicyConsent;
import com.chapchap.auth.domain.policy.repository.UserPolicyConsentRepository;
import com.chapchap.auth.domain.policy.request.MarketingConsentRequest;
import com.chapchap.auth.domain.policy.response.MarketingConsentResponse;
import com.chapchap.auth.domain.user.entity.User;
import com.chapchap.auth.domain.user.repository.UserRepository;
import com.chapchap.auth.global.error.custom.business.NotFoundResourceException;
import com.chapchap.auth.global.error.custom.business.InvalidStateException;
import com.chapchap.auth.global.security.constant.UserStatusPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MarketingConsentService {
    private final UserRepository userRepository;
    private final PolicyService policyService;
    private final UserPolicyConsentRepository userPolicyConsentRepository;

    @Transactional
    public MarketingConsentResponse change(Long userId, MarketingConsentRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundResourceException("사용자를 찾을 수 없습니다."));
        if (user.getStatus() != UserStatusPolicy.ACTIVE) {
            throw new InvalidStateException("활성 사용자만 마케팅 동의를 변경할 수 있습니다.");
        }

        Policy policy = policyService.getCurrentMarketingPolicy(request.policyId());
        UserPolicyConsent consent = userPolicyConsentRepository.findByUserAndPolicy(user, policy)
                .orElseGet(() -> UserPolicyConsent.createInitial(user, policy, false));
        consent.changeMarketingConsent(request.agreed());
        UserPolicyConsent savedConsent = userPolicyConsentRepository.save(consent);
        return new MarketingConsentResponse(
                policy.getId(),
                savedConsent.getConsentStatus(),
                savedConsent.getDecidedAt()
        );
    }
}
