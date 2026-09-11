package com.chapchap.auth.domain.policy.entity;

import com.chapchap.auth.domain.user.entity.User;
import com.chapchap.auth.domain.policy.constant.ConsentStatusPolicy;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class UserPolicyConsentTest {

    @Test
    void marketingConsentCanMoveFromDeclinedToWithdrawnAndBackToAgreed() {
        User user = User.createCustomer("identity-key", "name", "010", "user@example.test", LocalDateTime.now());
        UserPolicyConsent consent = UserPolicyConsent.createInitial(user, new Policy(), false);

        consent.changeMarketingConsent(false);
        assertThat(consent.getConsentStatus()).isEqualTo(ConsentStatusPolicy.WITHDRAWN);

        consent.changeMarketingConsent(true);
        assertThat(consent.getConsentStatus()).isEqualTo(ConsentStatusPolicy.AGREED);
    }
}
