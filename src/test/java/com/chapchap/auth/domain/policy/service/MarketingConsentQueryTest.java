package com.chapchap.auth.domain.policy.service;

import com.chapchap.auth.domain.policy.entity.Policy;
import com.chapchap.auth.domain.policy.entity.UserPolicyConsent;
import com.chapchap.auth.domain.policy.repository.UserPolicyConsentRepository;
import com.chapchap.auth.domain.policy.constant.ConsentStatusPolicy;
import com.chapchap.auth.domain.user.entity.User;
import com.chapchap.auth.domain.user.repository.UserRepository;
import com.chapchap.auth.global.error.custom.business.InvalidStateException;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class MarketingConsentQueryTest {
    private final UserRepository users = mock(UserRepository.class);
    private final PolicyService policies = mock(PolicyService.class);
    private final UserPolicyConsentRepository consents = mock(UserPolicyConsentRepository.class);
    private final MarketingConsentService service = new MarketingConsentService(users, policies, consents);

    @Test
    void absentConsentIsUndecidedWithoutCreatingData() {
        User user = User.createCustomer("test", "test", "010", "test@example.test", LocalDateTime.now());
        Policy policy = mock(Policy.class);
        when(users.findById(1L)).thenReturn(Optional.of(user));
        when(policies.getCurrentMarketingPolicy(3L)).thenReturn(policy);
        when(policy.getId()).thenReturn(3L);
        when(consents.findByUserAndPolicy(user, policy)).thenReturn(Optional.empty());
        var result = service.getCurrent(1L, 3L);
        assertThat(result.policyId()).isEqualTo(3L);
        assertThat(result.consentStatus()).isNull();
        assertThat(result.decidedAt()).isNull();
        verify(consents, never()).save(any());
    }

    @Test
    void returnsStoredAgreementAndWithdrawalForCurrentPolicy() {
        User user = User.createCustomer("test", "test", "010", "test@example.test", LocalDateTime.now());
        Policy policy = mock(Policy.class);
        when(users.findById(1L)).thenReturn(Optional.of(user));
        when(policies.getCurrentMarketingPolicy(3L)).thenReturn(policy);
        var consent = UserPolicyConsent.createInitial(user, policy, true);
        when(consents.findByUserAndPolicy(user, policy)).thenReturn(Optional.of(consent));
        assertThat(service.getCurrent(1L, 3L).consentStatus()).isEqualTo(ConsentStatusPolicy.AGREED);
        consent.changeMarketingConsent(false);
        assertThat(service.getCurrent(1L, 3L).consentStatus()).isEqualTo(ConsentStatusPolicy.WITHDRAWN);
    }

    @Test
    void withdrawnUserCannotReadConsent() {
        User user = User.createCustomer("test", "test", "010", "test@example.test", LocalDateTime.now());
        user.withdraw();
        when(users.findById(1L)).thenReturn(Optional.of(user));
        assertThatThrownBy(() -> service.getCurrent(1L, 3L)).isInstanceOf(InvalidStateException.class);
        verifyNoInteractions(policies, consents);
    }
}
