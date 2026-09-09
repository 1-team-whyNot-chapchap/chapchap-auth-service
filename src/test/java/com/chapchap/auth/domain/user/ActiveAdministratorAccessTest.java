package com.chapchap.auth.domain.user;

import com.chapchap.auth.domain.user.entity.User;
import com.chapchap.auth.domain.user.repository.UserRepository;
import com.chapchap.auth.domain.user.service.ActiveAdministratorAccess;
import com.chapchap.auth.global.security.constant.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ActiveAdministratorAccessTest {
    private final UserRepository users = mock(UserRepository.class);
    private final ActiveAdministratorAccess access = new ActiveAdministratorAccess(users);

    @Test
    void requiresCurrentActiveAdministratorAndMatchingAuthority() {
        for (var role : RolePolicy.values()) {
            for (var status : UserStatusPolicy.values()) {
                User user = mock(User.class);
                when(user.getRole()).thenReturn(role);
                when(user.getStatus()).thenReturn(status);
                when(users.findById(1L)).thenReturn(Optional.of(user));
                for (String tokenRole : new String[]{"ADMIN", "SUPER_ADMIN", "CUSTOMER", "RIDER"}) {
                    assertThat(access.isAllowed(new UsernamePasswordAuthenticationToken("1", null,
                            AuthorityUtils.createAuthorityList("ROLE_" + tokenRole))))
                            .isEqualTo(status == UserStatusPolicy.ACTIVE
                                    && (role == RolePolicy.ADMIN || role == RolePolicy.SUPER_ADMIN)
                                    && role.name().equals(tokenRole));
                }
            }
        }
    }

    @Test
    void missingMalformedAndUnauthenticatedActorsAreDenied() {
        assertThat(access.isAllowed(null)).isFalse();
        assertThat(access.isAllowed(new UsernamePasswordAuthenticationToken("1", null))).isFalse();
        for (String id : new String[]{"anonymousUser", "0", "-1", "999999999999999999999"})
            assertThat(access.isAllowed(new UsernamePasswordAuthenticationToken(id, null,
                    AuthorityUtils.createAuthorityList("ROLE_ADMIN")))).isFalse();
        verifyNoInteractions(users);
        when(users.findById(1L)).thenReturn(Optional.empty());
        assertThat(access.isAllowed(new UsernamePasswordAuthenticationToken("1", null,
                AuthorityUtils.createAuthorityList("ROLE_ADMIN")))).isFalse();
    }
}
