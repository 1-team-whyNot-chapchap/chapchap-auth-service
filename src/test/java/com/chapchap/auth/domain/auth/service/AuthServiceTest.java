package com.chapchap.auth.domain.auth.service;

import com.chapchap.auth.domain.auth.dto.IssuedRefreshToken;
import com.chapchap.auth.domain.auth.dto.IssuedToken;
import com.chapchap.auth.domain.token.entity.AuthSession;
import com.chapchap.auth.domain.token.entity.RefreshToken;
import com.chapchap.auth.domain.token.repository.AuthSessionRepository;
import com.chapchap.auth.domain.token.repository.RefreshTokenRepository;
import com.chapchap.auth.domain.token.service.AuthSessionService;
import com.chapchap.auth.domain.user.entity.User;
import com.chapchap.auth.domain.audit.service.AuditLogService;
import com.chapchap.auth.global.security.jwt.JwtProvider;
import com.chapchap.auth.domain.user.constant.RolePolicy;
import com.chapchap.auth.domain.token.constant.SessionTypePolicy;
import com.chapchap.auth.domain.token.service.RefreshTokenGenerator;
import com.chapchap.auth.domain.token.service.RefreshTokenHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private AuthSessionRepository authSessionRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private RefreshTokenHasher refreshTokenHasher;

    @Mock
    private AuthSessionService authSessionService;

    @Mock
    private RefreshTokenGenerator refreshTokenGenerator;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private com.chapchap.auth.domain.user.repository.UserRepository userRepository;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
            jwtProvider,
            authSessionRepository,
            refreshTokenRepository,
            refreshTokenHasher,
            authSessionService,
            refreshTokenGenerator,
            auditLogService,
            userRepository
        );
    }

    @Test
    void issueInitialRefreshTokenCreatesSessionWithoutGeneratingAccessToken() {
        User user = mock(User.class);
        AuthSession authSession = mock(AuthSession.class);
        LocalDateTime idleExpiresAt = LocalDateTime.of(2026, 8, 31, 12, 0);

        when(user.getRole()).thenReturn(RolePolicy.CUSTOMER);
        when(authSessionService.createSession(user, SessionTypePolicy.USER)).thenReturn(authSession);
        when(authSession.getIdleExpiresAt()).thenReturn(idleExpiresAt);
        when(authSession.getSessionType()).thenReturn(SessionTypePolicy.USER);
        when(refreshTokenGenerator.generate()).thenReturn("refresh-token");
        when(refreshTokenHasher.hash("refresh-token")).thenReturn("refresh-token-hash");

        IssuedRefreshToken result = authService.issueInitialRefreshToken(user);

        assertEquals("refresh-token", result.refreshToken());
        assertEquals(SessionTypePolicy.USER, result.sessionType());
        verify(authSessionService).createSession(user, SessionTypePolicy.USER);
        verify(refreshTokenRepository).save(org.mockito.ArgumentMatchers.any());
        verify(jwtProvider, never()).generateAccessToken(user, SessionTypePolicy.USER);
    }

    @Test
    void issueTokenKeepsAccessAndRefreshTokenIssuanceForSignup() {
        User user = mock(User.class);
        AuthSession authSession = mock(AuthSession.class);
        LocalDateTime idleExpiresAt = LocalDateTime.of(2026, 8, 31, 12, 0);

        when(user.getRole()).thenReturn(RolePolicy.CUSTOMER);
        when(authSessionService.createSession(user, SessionTypePolicy.USER)).thenReturn(authSession);
        when(authSession.getIdleExpiresAt()).thenReturn(idleExpiresAt);
        when(authSession.getSessionType()).thenReturn(SessionTypePolicy.USER);
        when(refreshTokenGenerator.generate()).thenReturn("refresh-token");
        when(refreshTokenHasher.hash("refresh-token")).thenReturn("refresh-token-hash");
        when(jwtProvider.generateAccessToken(user, SessionTypePolicy.USER)).thenReturn("access-token");

        IssuedToken result = authService.issueToken(user);

        assertEquals("access-token", result.accessToken());
        assertEquals("refresh-token", result.refreshToken());
        assertEquals(SessionTypePolicy.USER, result.sessionType());
        verify(jwtProvider).generateAccessToken(user, SessionTypePolicy.USER);
    }

    @Test
    void refreshTokenReuseRevokesOnlyItsSessionAndRecordsAudit() {
        RefreshToken refreshToken = mock(RefreshToken.class);
        AuthSession session = mock(AuthSession.class);
        User user = mock(User.class);
        when(refreshTokenHasher.hash("reused-token")).thenReturn("hash");
        when(refreshTokenRepository.findOwnerIdByTokenHash("hash")).thenReturn(Optional.of(25L));
        when(userRepository.findByIdForUpdate(25L)).thenReturn(Optional.of(user));
        when(user.getStatus()).thenReturn(com.chapchap.auth.domain.user.constant.UserStatusPolicy.ACTIVE);
        when(refreshTokenRepository.findByTokenHashForUpdate("hash")).thenReturn(Optional.of(refreshToken));
        when(refreshToken.getAuthSession()).thenReturn(session);
        when(refreshToken.getConsumedAt()).thenReturn(LocalDateTime.now());
        when(session.getUser()).thenReturn(user);
        when(session.getId()).thenReturn(7L);
        when(user.getId()).thenReturn(25L);

        assertThrows(com.chapchap.auth.global.error.custom.business.InvalidTokenException.class,
                () -> authService.reissueRefreshToken("reused-token"));

        verify(session).revoke();
        verify(authSessionRepository).save(session);
        verify(auditLogService).recordRefreshTokenReuse(25L, 7L);
        verify(refreshTokenGenerator, never()).generate();
    }
}
