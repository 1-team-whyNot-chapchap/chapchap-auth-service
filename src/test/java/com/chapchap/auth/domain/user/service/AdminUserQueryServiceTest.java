package com.chapchap.auth.domain.user.service;

import com.chapchap.auth.domain.user.entity.User;
import com.chapchap.auth.domain.user.repository.UserRepository;
import com.chapchap.auth.domain.user.request.AdminUserSearchRequest;
import com.chapchap.auth.global.error.custom.business.InvalidParameterException;
import com.chapchap.auth.global.error.custom.business.NotFoundResourceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminUserQueryServiceTest {
    private final UserRepository repository = mock(UserRepository.class);
    private final AdminUserQueryService service = new AdminUserQueryService(repository);

    @Test
    void exactSearchRetainsDuplicatesAndUsesBoundedStablePages() {
        var page = PageRequest.of(0, 20, Sort.by("id").ascending());
        when(repository.findByExactPhone("01012345678", page))
                .thenReturn(new PageImpl<>(List.of(user(25L), user(9007199254740993L)), page, 21));
        var result = service.search(new AdminUserSearchRequest(" 010-1234-5678 ", 0));
        assertThat(result.users()).extracting("userId").containsExactly("25", "9007199254740993");
        assertThat(result.totalElements()).isEqualTo(21);
        assertThat(result.hasNext()).isTrue();
        verify(repository).findByExactPhone("01012345678", page);
    }

    @Test
    void emptySearchIsSuccessful() {
        when(repository.findByExactPhone(anyString(), any())).thenReturn(new PageImpl<>(List.of()));
        assertThat(service.search(new AdminUserSearchRequest("01012345678", 0)).users()).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"010", "%", "01012345678 OR 1=1", "+821012345678", "02012345678", "010123456789", "010\t12345678"})
    void rejectsInvalidPhoneBeforeRepository(String phone) {
        assertThatThrownBy(() -> service.search(new AdminUserSearchRequest(phone, 0)))
                .isInstanceOf(InvalidParameterException.class);
        verifyNoInteractions(repository);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 1000001})
    void rejectsInvalidPage(int page) {
        assertThatThrownBy(() -> service.search(new AdminUserSearchRequest("01012345678", page)))
                .isInstanceOf(InvalidParameterException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void detailReturnsCurrentStateAndNotFound() {
        User user = user(25L);
        user.withdraw();
        when(repository.findById(25L)).thenReturn(Optional.of(user));
        assertThat(service.getUser(25L).status().name()).isEqualTo("WITHDRAWN");
        when(repository.findById(26L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getUser(26L)).isInstanceOf(NotFoundResourceException.class);
        assertThatThrownBy(() -> service.getUser(0L)).isInstanceOf(InvalidParameterException.class);
    }

    private User user(Long id) {
        User user = User.createCustomer("test-" + id, "테스트", "01012345678", null, LocalDateTime.now());
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
