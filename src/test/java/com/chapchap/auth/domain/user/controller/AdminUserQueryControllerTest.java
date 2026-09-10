package com.chapchap.auth.domain.user.controller;

import com.chapchap.auth.domain.user.response.AdminUserResponse;
import com.chapchap.auth.domain.user.response.AdminUserSearchResponse;
import com.chapchap.auth.domain.user.service.AdminUserQueryService;
import com.chapchap.auth.global.error.GlobalExceptionHandler;
import com.chapchap.auth.domain.user.constant.RolePolicy;
import com.chapchap.auth.domain.user.constant.UserStatusPolicy;
import com.chapchap.auth.domain.user.service.ActiveAdministratorAccess;
import com.chapchap.auth.domain.user.repository.UserRepository;
import com.chapchap.auth.domain.user.entity.User;
import java.util.Optional;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AdminUserQueryControllerTest {
    private AnnotationConfigApplicationContext context;
    private UserRepository users;
    private AdminUserQueryService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(AdminUserQueryService.class);
        context = new AnnotationConfigApplicationContext();
        context.register(MethodSecurity.class);
        users = mock(UserRepository.class);
        context.registerBean(UserRepository.class, () -> users);
        context.registerBean("activeAdministratorAccess", ActiveAdministratorAccess.class);
        context.registerBean(AdminUserQueryService.class, () -> service);
        context.registerBean(AdminUserQueryController.class);
        context.refresh();
        mvc = MockMvcBuilders.standaloneSetup(context.getBean(AdminUserQueryController.class))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @AfterEach
    void close() {
        SecurityContextHolder.clearContext();
        context.close();
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "SUPER_ADMIN"})
    void administratorCanSearchAndReadWithoutCaching(String role) throws Exception {
        authenticate(role);
        var user = new AdminUserResponse("9007199254740993", "테스트", RolePolicy.CUSTOMER, UserStatusPolicy.ACTIVE);
        when(service.search(any())).thenReturn(new AdminUserSearchResponse(List.of(user), 0, 1, false));
        when(service.getUser(25L)).thenReturn(user);
        mvc.perform(post("/api/auth/admin/users/search").contentType("application/json")
                        .content("{\"phone\":\"01012345678\"}"))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.data.users[0].userId").value("9007199254740993"))
                .andExpect(jsonPath("$.data.users[0].phone").doesNotExist());
        mvc.perform(get("/api/auth/admin/users/25"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"CUSTOMER", "RIDER"})
    void nonAdministratorsAreDenied(String role) throws Exception {
        authenticate(role);
        mvc.perform(post("/api/auth/admin/users/search").contentType("application/json")
                        .content("{\"phone\":\"01012345678\"}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("E04"));
        mvc.perform(get("/api/auth/admin/users/25")).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    void anonymousCannotSearch() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken("test", "anonymousUser",
                AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
        mvc.perform(get("/api/auth/admin/users/25")).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{", "{\"phone\":\"\"}", "{\"phone\":\"01012345678\",\"page\":-1}"})
    void validatesBodyBeforeService(String body) throws Exception {
        authenticate("ADMIN");
        mvc.perform(post("/api/auth/admin/users/search").contentType("application/json").content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("E21"));
        verifyNoInteractions(service);
    }

    private void authenticate(String role) {
        if (role.equals("ADMIN") || role.equals("SUPER_ADMIN"))
            when(users.findById(1L)).thenReturn(Optional.of(User.createAdministrator("관리자", RolePolicy.valueOf(role))));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("1", null,
                AuthorityUtils.createAuthorityList("ROLE_" + role)));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableMethodSecurity(proxyTargetClass = true)
    static class MethodSecurity { }
}
