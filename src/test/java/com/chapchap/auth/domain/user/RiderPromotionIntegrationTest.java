package com.chapchap.auth.domain.user;

import com.chapchap.auth.domain.token.repository.AuthSessionRepository;
import com.chapchap.auth.domain.token.repository.RefreshTokenRepository;
import com.chapchap.auth.domain.token.service.RefreshTokenGenerator;
import com.chapchap.auth.domain.token.service.RefreshTokenHasher;
import com.chapchap.auth.domain.user.constant.RolePolicy;
import com.chapchap.auth.domain.user.constant.UserStatusPolicy;
import com.chapchap.auth.domain.user.controller.AdminUserQueryController;
import com.chapchap.auth.domain.user.controller.CurrentUserController;
import com.chapchap.auth.domain.user.controller.UserRoleController;
import com.chapchap.auth.domain.user.service.ActiveAdministratorAccess;
import com.chapchap.auth.domain.user.service.AdminUserQueryService;
import com.chapchap.auth.domain.user.service.CurrentUserService;
import com.chapchap.auth.domain.user.service.UserRoleService;
import com.chapchap.auth.global.config.security.JwtConfig;
import com.chapchap.auth.global.security.jwt.JwtProvider;

import com.chapchap.auth.domain.audit.repository.AuditLogRepository;
import com.chapchap.auth.domain.audit.service.AuditLogService;
import com.chapchap.auth.domain.auth.controller.AuthController;
import com.chapchap.auth.domain.auth.service.AuthService;
import com.chapchap.auth.domain.auth.service.SignupService;
import com.chapchap.auth.domain.token.repository.*;
import com.chapchap.auth.domain.token.service.AuthSessionService;
import com.chapchap.auth.domain.user.controller.*;
import com.chapchap.auth.domain.user.entity.User;
import com.chapchap.auth.domain.user.repository.UserRepository;
import com.chapchap.auth.domain.user.service.*;
import com.chapchap.auth.global.service.cookie.CookieManager;
import com.chapchap.auth.global.error.GlobalExceptionHandler;
import com.chapchap.auth.global.error.custom.business.InvalidStateException;
import com.chapchap.auth.global.config.kafka.KafkaTopicProperties;
import com.chapchap.auth.global.messaging.kafka.producer.AuthEventProducer;
import com.chapchap.auth.domain.token.service.AuthSessionPolicy;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 실제 JPA/트랜잭션/서명/MVC를 사용하며 Kafka 전송과 외부 OAuth만 경계로 둔다. */
@SpringJUnitConfig(RiderPromotionIntegrationTest.Config.class)
class RiderPromotionIntegrationTest {
    @Autowired UserRepository users;
    @Autowired RefreshTokenRepository tokens;
    @Autowired AuthSessionRepository sessions;
    @Autowired AuditLogRepository audit;
    @Autowired AuthService auth;
    @Autowired UserRoleService roles;
    @Autowired JwtProvider jwt;
    @Autowired JwtConfig jwtConfig;
    @Autowired KafkaTemplate<String, Object> kafka;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired AdminUserQueryController queryController;
    @Autowired UserRoleController roleController;
    @Autowired CurrentUserController currentController;
    @Autowired AuthController authController;
    MockMvc mvc;
    User customer;
    User administrator;

    @BeforeEach
    void setUp() {
        tokens.deleteAll();
        sessions.deleteAll();
        audit.deleteAll();
        users.deleteAll();
        reset(kafka);
        mvc = MockMvcBuilders.standaloneSetup(queryController, roleController, currentController, authController)
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        customer = users.save(User.createCustomer("identity-1", "테스트 고객", "010-1234-5678", null, LocalDateTime.now()));
        administrator = users.save(User.createAdministrator("테스트 관리자", RolePolicy.ADMIN));
        authenticate(administrator.getId(), "ADMIN");
    }

    @AfterEach
    void clear() { SecurityContextHolder.clearContext(); }

    @Test
    void suspendedAdministratorCannotSearchReadOrPromoteWithExistingAuthority() throws Exception {
        auth.issueInitialRefreshToken(customer);
        administrator.suspendAdministrator();
        users.save(administrator);
        mvc.perform(post("/api/auth/admin/users/search").contentType("application/json")
                        .content("{\"phone\":\"01012345678\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/auth/admin/users/{id}", customer.getId()))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/auth/admin/users/{id}/role", customer.getId())
                        .principal(SecurityContextHolder.getContext().getAuthentication())
                        .contentType("application/json").content("{\"targetRole\":\"RIDER\"}"))
                .andExpect(status().isForbidden());
        assertThat(users.findById(customer.getId()).orElseThrow().getRole()).isEqualTo(RolePolicy.CUSTOMER);
        assertThat(sessions.findAll()).hasSize(1).allMatch(session -> session.isUsable());
        assertThat(audit.count()).isZero();
        verifyNoInteractions(kafka);
    }

    @Test
    void searchPromoteRevokeAndLoginWithNewRole() throws Exception {
        users.save(User.createCustomer("identity-2", "동일 번호 고객", "01012345678", null, LocalDateTime.now()));
        var first = auth.issueInitialRefreshToken(customer);
        var second = auth.issueInitialRefreshToken(customer);
        mvc.perform(post("/api/auth/admin/users/search").contentType("application/json")
                        .content("{\"phone\":\"010 1234 5678\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.users[0].userId").value(customer.getId().toString()));
        mvc.perform(patch("/api/auth/admin/users/{id}/role", customer.getId()).principal(SecurityContextHolder.getContext().getAuthentication())
                        .contentType("application/json").content("{\"targetRole\":\"RIDER\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.newRole").value("RIDER"));
        assertThat(users.findById(customer.getId()).orElseThrow().getRole()).isEqualTo(RolePolicy.RIDER);
        assertThat(sessions.findAll()).allMatch(session -> !session.isUsable());
        assertThat(audit.count()).isEqualTo(1);
        verify(kafka, times(1)).send(eq("msa4-team1.auth.user-events.v1"), eq(customer.getId().toString()), any());
        for (var old : List.of(first, second)) {
            mvc.perform(post("/api/auth/reissue-token").cookie(new Cookie("refreshToken", old.refreshToken())))
                    .andExpect(status().isUnauthorized()).andExpect(cookie().maxAge("refreshToken", 0));
        }
        // 외부 OAuth에서 동일 사용자 확인이 끝난 다음의 기존 서버 로그인 진입점.
        var login = auth.issueInitialRefreshToken(users.findById(customer.getId()).orElseThrow());
        var response = mvc.perform(post("/api/auth/reissue-token").cookie(new Cookie("refreshToken", login.refreshToken())))
                .andExpect(status().isOk()).andExpect(cookie().httpOnly("refreshToken", true)).andReturn().getResponse();
        String access = new JsonMapper().readTree(response.getContentAsString()).path("data").path("accessToken").asString();
        assertThat(jwt.extractClaims(access).get("role", String.class)).isEqualTo("RIDER");
        authenticate(customer.getId(), "RIDER");
        mvc.perform(get("/api/auth/me").principal(SecurityContextHolder.getContext().getAuthentication()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.role").value("RIDER"));
    }

    @Test
    void deniedAndInactiveAndDuplicatePromotionHaveNoExtraEffects() throws Exception {
        authenticate(customer.getId(), "CUSTOMER");
        mvc.perform(post("/api/auth/admin/users/search").contentType("application/json").content("{\"phone\":\"01012345678\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/auth/admin/users/{id}/role", customer.getId()).principal(SecurityContextHolder.getContext().getAuthentication())
                        .contentType("application/json").content("{\"targetRole\":\"RIDER\"}"))
                .andExpect(status().isForbidden());
        authenticate(administrator.getId(), "ADMIN");
        ReflectionTestUtils.setField(customer, "status", UserStatusPolicy.SUSPENDED);
        users.save(customer);
        assertThatThrownBy(() -> roles.changeGeneralUserRole(1L, customer.getId(), RolePolicy.RIDER)).isInstanceOf(InvalidStateException.class);
        assertThat(audit.count()).isZero();
        verifyNoInteractions(kafka);
        ReflectionTestUtils.setField(customer, "status", UserStatusPolicy.ACTIVE);
        users.save(customer);
        roles.changeGeneralUserRole(1L, customer.getId(), RolePolicy.RIDER);
        assertThatThrownBy(() -> roles.changeGeneralUserRole(1L, customer.getId(), RolePolicy.RIDER)).isInstanceOf(InvalidStateException.class);
        assertThat(audit.count()).isEqualTo(1);
        verify(kafka, times(1)).send(anyString(), anyString(), any());
    }

    @Test
    void rollbackRestoresRoleSessionAndAuditAndPublishesNothing() {
        auth.issueInitialRefreshToken(customer);
        new TransactionTemplate(transactionManager).executeWithoutResult(transaction -> {
            roles.changeGeneralUserRole(1L, customer.getId(), RolePolicy.RIDER);
            verifyNoInteractions(kafka);
            transaction.setRollbackOnly();
        });
        assertThat(users.findById(customer.getId()).orElseThrow().getRole()).isEqualTo(RolePolicy.CUSTOMER);
        assertThat(sessions.findAll()).allMatch(session -> session.isUsable());
        assertThat(audit.count()).isZero();
        verifyNoInteractions(kafka);
    }

    @Test
    void simultaneousPromotionsCommitOnlyOnce() throws Exception {
        try (var executor = Executors.newFixedThreadPool(2)) {
            var start = new CountDownLatch(1);
            Callable<Boolean> change = () -> {
                start.await();
                try { roles.changeGeneralUserRole(1L, customer.getId(), RolePolicy.RIDER); return true; }
                catch (InvalidStateException expected) { return false; }
            };
            var first = executor.submit(change);
            var second = executor.submit(change);
            start.countDown();
            assertThat(List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true, false);
            assertThat(audit.count()).isEqualTo(1);
            verify(kafka, times(1)).send(anyString(), anyString(), any());
        }
    }

    private void authenticate(Long userId, String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(userId.toString(), null,
                AuthorityUtils.createAuthorityList("ROLE_" + role)));
    }

    @Test
    void concurrentRefreshCannotRestoreASessionRevokedByPromotion() throws Exception {
        var login = auth.issueInitialRefreshToken(customer);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var start = new CountDownLatch(1);
            var refresh = executor.submit(() -> {
                start.await();
                try { auth.reissueRefreshToken(login.refreshToken()); }
                catch (com.chapchap.auth.global.error.custom.business.InvalidTokenException expected) { }
                return true;
            });
            var promotion = executor.submit(() -> {
                start.await();
                roles.changeGeneralUserRole(administrator.getId(), customer.getId(), RolePolicy.RIDER);
                return true;
            });
            start.countDown();
            assertThat(refresh.get(10, TimeUnit.SECONDS)).isTrue();
            assertThat(promotion.get(10, TimeUnit.SECONDS)).isTrue();
        }
        assertThat(sessions.findAll()).allMatch(session -> !session.isUsable());
        assertThat(users.findById(customer.getId()).orElseThrow().getRole()).isEqualTo(RolePolicy.RIDER);
    }

    @Test
    @org.junit.jupiter.api.condition.EnabledIfSystemProperty(named = "rider.client.path", matches = ".+")
    void actualFrontendClientSearchesPromotesAndRelogsAgainstAuth() throws Exception {
        users.save(User.createCustomer("identity-2", "중복 번호 고객", "01012345678", null, LocalDateTime.now()));
        var adminLogin = auth.issueInitialRefreshToken(administrator);
        // 테스트 서버는 loopback에만 바인딩한다. 테스트 JWT 검증으로 Gateway 경계를 대신한다.
        var server = com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1", 0), 0);
        var executor = Executors.newCachedThreadPool();
        server.setExecutor(executor);
        server.createContext("/", exchange -> {
            try {
                String path = exchange.getRequestURI().getPath();
                if (path.equals("/__test/social-login") && exchange.getRequestMethod().equals("POST")) {
                    var login = auth.issueInitialRefreshToken(users.findById(customer.getId()).orElseThrow());
                    exchange.getResponseHeaders().add("Set-Cookie", "refreshToken=" + login.refreshToken() + "; Path=/api/auth; HttpOnly; SameSite=Lax");
                    byte[] body = "{\"code\":\"00\",\"data\":{}}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
                    exchange.sendResponseHeaders(200, body.length);
                    exchange.getResponseBody().write(body);
                    return;
                }
                var authentication = new org.springframework.security.authentication.AnonymousAuthenticationToken("test", "anonymousUser",
                        AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));
                SecurityContextHolder.getContext().setAuthentication(authentication);
                String authorization = exchange.getRequestHeaders().getFirst("Authorization");
                if (authorization != null && authorization.startsWith("Bearer ")) {
                    var claims = jwt.extractClaims(authorization.substring(7));
                    authenticate(Long.valueOf(claims.getSubject()), claims.get("role", String.class));
                }
                var request = org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request(
                                org.springframework.http.HttpMethod.valueOf(exchange.getRequestMethod()), path)
                        .principal(SecurityContextHolder.getContext().getAuthentication())
                        .contentType("application/json").content(exchange.getRequestBody().readAllBytes());
                String cookie = exchange.getRequestHeaders().getFirst("Cookie");
                if (cookie != null) {
                    for (String item : cookie.split(";")) {
                        String[] pair = item.trim().split("=", 2);
                        if (pair.length == 2) request.cookie(new Cookie(pair[0], pair[1]));
                    }
                }
                var response = mvc.perform(request).andReturn().getResponse();
                for (String header : response.getHeaderNames()) {
                    for (String value : response.getHeaders(header)) exchange.getResponseHeaders().add(header, value);
                }
                byte[] body = response.getContentAsByteArray();
                exchange.sendResponseHeaders(response.getStatus(), body.length);
                exchange.getResponseBody().write(body);
            } catch (com.chapchap.auth.global.error.custom.business.InvalidTokenException expired) {
                exchange.sendResponseHeaders(401, -1);
            } catch (Exception failure) {
                exchange.sendResponseHeaders(500, -1);
            } finally {
                SecurityContextHolder.clearContext();
                exchange.close();
            }
        });
        server.start();
        try {
            var script = java.nio.file.Path.of(System.getProperty("rider.client.path"), "tests", "rider-auth.integration.mjs");
            assertThat(script).exists();
            var process = new ProcessBuilder("node", script.toString()).redirectErrorStream(true);
            process.environment().put("RIDER_AUTH_TEST_URL", "http://127.0.0.1:" + server.getAddress().getPort());
            process.environment().put("RIDER_TEST_ADMIN_COOKIE", "refreshToken=" + adminLogin.refreshToken());
            process.environment().put("RIDER_TEST_CUSTOMER_ID", customer.getId().toString());
            String expiredAccess = io.jsonwebtoken.Jwts.builder().subject(customer.getId().toString())
                    .claim("role", "RIDER").expiration(new Date(0))
                    .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(
                            io.jsonwebtoken.io.Decoders.BASE64.decode(jwtConfig.secret()))).compact();
            process.environment().put("RIDER_TEST_EXPIRED_ACCESS_TOKEN", expiredAccess);
            var running = process.start();
            try {
                assertThat(running.waitFor(30, TimeUnit.SECONDS)).isTrue();
                String output = new String(running.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                assertThat(running.exitValue()).withFailMessage(output).isZero();
                assertThat(output).contains("Auth + frontend integration passed");
            } finally { if (running.isAlive()) running.destroyForcibly(); }
        } finally {
            server.stop(0);
            executor.close();
        }
        assertThat(users.findById(customer.getId()).orElseThrow().getRole()).isEqualTo(RolePolicy.RIDER);
        assertThat(audit.findAll()).extracting(entry -> entry.getActionType().name())
                .containsExactlyInAnyOrder("RIDER_ROLE_GRANTED", "TOKEN_REUSE_DETECTED");
        assertThat(sessions.findAllByUser(customer)).allMatch(session -> !session.isUsable());
        verify(kafka, times(1)).send(anyString(), anyString(), any());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableTransactionManagement(proxyTargetClass = true)
    @EnableMethodSecurity(proxyTargetClass = true)
    @EnableJpaRepositories(basePackages = {"com.chapchap.auth.domain.user.repository", "com.chapchap.auth.domain.token.repository", "com.chapchap.auth.domain.audit.repository"})
    @Import({AdminUserQueryController.class, UserRoleController.class, CurrentUserController.class, AuthController.class,
            ActiveAdministratorAccess.class, AdminUserQueryService.class, CurrentUserService.class, UserRoleService.class, AuthService.class,
            AuthSessionService.class, AuthSessionPolicy.class, AuditLogService.class, RefreshTokenGenerator.class,
            RefreshTokenHasher.class, JwtProvider.class, CookieManager.class, AuthEventProducer.class})
    static class Config {
        @Bean DataSource dataSource() {
            return new DriverManagerDataSource("jdbc:h2:mem:rider-promotion;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        }
        @Bean LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
            var factory = new LocalContainerEntityManagerFactoryBean();
            factory.setDataSource(dataSource);
            factory.setPackagesToScan("com.chapchap.auth.domain");
            factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "create-drop"));
            return factory;
        }
        @Bean PlatformTransactionManager transactionManager(EntityManagerFactory factory) { return new JpaTransactionManager(factory); }
        @Bean SignupService signupService() { return mock(SignupService.class); }
        @Bean JwtConfig jwtConfig() {
            byte[] key = new byte[32];
            new java.security.SecureRandom().nextBytes(key);
            return new JwtConfig(false, "integration-test", "JWT", 60000, 60000, "refreshToken", 3600, 3600,
                    Base64.getEncoder().encodeToString(key), "Authorization", "Bearer", "/api/auth", "Lax");
        }
        @Bean @SuppressWarnings("unchecked") KafkaTemplate<String, Object> kafkaTemplate() { return mock(KafkaTemplate.class); }
        @Bean KafkaTopicProperties topics() {
            return new KafkaTopicProperties("msa4-team1.auth.user-events.v1", "test-dlt", "test-address", "test-address-dlt", "test-subscription", "test-subscription-dlt");
        }
    }
}
