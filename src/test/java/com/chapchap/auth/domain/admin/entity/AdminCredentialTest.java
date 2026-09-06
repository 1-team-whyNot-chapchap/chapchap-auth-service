package com.chapchap.auth.domain.admin.entity;

import com.chapchap.auth.domain.user.entity.User;
import com.chapchap.auth.global.security.constant.RolePolicy;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class AdminCredentialTest {

    @Test
    void locksForFiveMinutesAfterFiveConsecutiveFailuresAndResetsAfterSuccess() {
        AdminCredential credential = AdminCredential.create(
                User.createAdministrator("admin", RolePolicy.ADMIN), "admin-id", "hash", false
        );
        LocalDateTime now = LocalDateTime.of(2026, 8, 24, 12, 0);

        for (int index = 0; index < 4; index++) {
            assertThat(credential.recordFailedLogin(now)).isFalse();
        }
        assertThat(credential.recordFailedLogin(now)).isTrue();
        assertThat(credential.isLockedAt(now.plusMinutes(4))).isTrue();

        assertThat(credential.unlockIfExpired(now.plusMinutes(5))).isTrue();
        credential.recordSuccessfulLogin(now.plusMinutes(5));
        assertThat(credential.getFailedLoginCount()).isZero();
        assertThat(credential.getLockedUntil()).isNull();
    }
}
