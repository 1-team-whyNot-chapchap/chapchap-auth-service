package com.chapchap.auth.domain.user.controller;

import com.chapchap.auth.domain.user.request.UserRoleChangeRequest;
import com.chapchap.auth.domain.user.response.UserRoleChangeResponse;
import com.chapchap.auth.domain.user.service.UserRoleService;
import com.chapchap.auth.global.security.constant.RolePolicy;
import com.chapchap.auth.domain.user.service.ActiveAdministratorAccess;
import com.chapchap.auth.domain.user.repository.UserRepository;
import com.chapchap.auth.domain.user.entity.User;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class UserRoleAuthorizationTest {
    private AnnotationConfigApplicationContext context;
    private UserRepository users;
    private UserRoleService service;
    private UserRoleController controller;

    @BeforeEach
    void setUp() {
        service = mock(UserRoleService.class);
        context = new AnnotationConfigApplicationContext();
        context.register(MethodSecurity.class);
        users = mock(UserRepository.class);
        context.registerBean(UserRepository.class, () -> users);
        context.registerBean("activeAdministratorAccess", ActiveAdministratorAccess.class);
        context.registerBean(UserRoleService.class, () -> service);
        context.registerBean(UserRoleController.class);
        context.refresh();
        controller = context.getBean(UserRoleController.class);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        context.close();
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "SUPER_ADMIN"})
    void administratorsCanPromoteCustomers(String role) {
        Authentication actor = authenticated(role);
        when(service.changeGeneralUserRole(1L, 25L, RolePolicy.RIDER))
                .thenReturn(new UserRoleChangeResponse(25L, RolePolicy.CUSTOMER, RolePolicy.RIDER));
        var response = controller.changeRole(actor, 25L, new UserRoleChangeRequest(RolePolicy.RIDER));
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().data().newRole()).isEqualTo(RolePolicy.RIDER);
        verify(service).changeGeneralUserRole(1L, 25L, RolePolicy.RIDER);
    }

    @ParameterizedTest
    @ValueSource(strings = {"CUSTOMER", "RIDER"})
    void generalUsersCannotPromoteAnyone(String role) {
        assertDenied(authenticated(role));
    }

    @Test
    void anonymousUsersCannotPromoteAnyone() {
        Authentication actor = new AnonymousAuthenticationToken("test", "anonymousUser",
                AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));
        SecurityContextHolder.getContext().setAuthentication(actor);
        assertDenied(actor);
    }

    private Authentication authenticated(String role) {
        if (role.equals("ADMIN") || role.equals("SUPER_ADMIN"))
            when(users.findById(1L)).thenReturn(Optional.of(User.createAdministrator("관리자", RolePolicy.valueOf(role))));
        Authentication actor = new UsernamePasswordAuthenticationToken("1", null,
                AuthorityUtils.createAuthorityList("ROLE_" + role));
        SecurityContextHolder.getContext().setAuthentication(actor);
        return actor;
    }

    private void assertDenied(Authentication actor) {
        assertThatThrownBy(() -> controller.changeRole(actor, 25L, new UserRoleChangeRequest(RolePolicy.RIDER)))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(service);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableMethodSecurity
    static class MethodSecurity {
    }
}
