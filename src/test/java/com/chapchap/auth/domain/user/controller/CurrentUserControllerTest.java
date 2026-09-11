package com.chapchap.auth.domain.user.controller;

import com.chapchap.auth.domain.user.entity.User;
import com.chapchap.auth.domain.user.repository.UserRepository;
import com.chapchap.auth.global.error.custom.business.InvalidTokenException;
import com.chapchap.auth.domain.user.constant.RolePolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class CurrentUserControllerTest {
    private final UserRepository repository = mock(UserRepository.class);
    private final CurrentUserController controller = new CurrentUserController(
            new com.chapchap.auth.domain.user.service.CurrentUserService(repository));

    @ParameterizedTest
    @EnumSource(RolePolicy.class)
    void allRolesReadOnlyTheirOwnCurrentIdentity(RolePolicy role) {
        User user = customer();
        ReflectionTestUtils.setField(user, "role", role);
        when(repository.findById(25L)).thenReturn(Optional.of(user));
        var response = controller.getCurrentUser(actor(role));
        assertThat(response.getBody().data().role()).isEqualTo(role);
        assertThat(response.getBody().data().userId()).isEqualTo("25");
        assertThat(response.getHeaders().getCacheControl()).isEqualTo("no-store");
        verify(repository).findById(25L);
    }

    @Test
    void oldCustomerTokenCannotBeUsedAsRiderIdentity() {
        User user = customer();
        user.changeGeneralRole(RolePolicy.RIDER);
        when(repository.findById(25L)).thenReturn(Optional.of(user));
        assertThatThrownBy(() -> controller.getCurrentUser(actor(RolePolicy.CUSTOMER)))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void withdrawnAccountRequiresLogin() {
        User user = customer();
        user.withdraw();
        when(repository.findById(25L)).thenReturn(Optional.of(user));
        assertThatThrownBy(() -> controller.getCurrentUser(actor(RolePolicy.CUSTOMER)))
                .isInstanceOf(InvalidTokenException.class);
    }

    private UsernamePasswordAuthenticationToken actor(RolePolicy role) {
        return new UsernamePasswordAuthenticationToken("25", null, AuthorityUtils.createAuthorityList("ROLE_" + role));
    }

    private User customer() {
        User user = User.createCustomer("identity", "테스트", "01012345678", null, LocalDateTime.now());
        ReflectionTestUtils.setField(user, "id", 25L);
        return user;
    }
}
