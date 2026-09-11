package com.chapchap.auth.domain.token.service;

import com.chapchap.auth.domain.token.entity.AuthSession;
import com.chapchap.auth.domain.token.repository.AuthSessionRepository;
import com.chapchap.auth.domain.user.entity.User;
import com.chapchap.auth.domain.token.constant.SessionTypePolicy;
import com.chapchap.auth.domain.token.service.AuthSessionPolicy;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AuthSessionServiceTest {
    @Test
    void revokesEverySessionBelongingToPromotedUser() {
        var repository = mock(AuthSessionRepository.class);
        var service = new AuthSessionService(mock(AuthSessionPolicy.class), repository);
        var now = LocalDateTime.now();
        User user = User.createCustomer("identity", "테스트", "01012345678", null, now);
        AuthSession first = AuthSession.create(user, SessionTypePolicy.USER, now.plusDays(1), now.plusDays(7), now);
        AuthSession second = AuthSession.create(user, SessionTypePolicy.USER, now.plusDays(1), now.plusDays(7), now);
        assertThat(first.isUsable()).isTrue();
        when(repository.findAllByUser(user)).thenReturn(List.of(first, second));
        service.revokeAllSessions(user);
        assertThat(first.isUsable()).isFalse();
        assertThat(second.isUsable()).isFalse();
    }
}
