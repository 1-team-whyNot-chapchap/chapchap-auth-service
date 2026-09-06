package com.chapchap.auth.global.error;

import com.chapchap.auth.global.error.custom.business.InvalidStateException;
import com.chapchap.auth.global.response.GlobalResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {
    @Test
    void returnsConflictForExplicitBusinessStateException() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        ResponseEntity<GlobalResponse<Void>> response = handler.handle(
                new InvalidStateException("가입 세션이 만료되었습니다.")
        );

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody().code()).isEqualTo("E20");
        assertThat(response.getBody().message()).isEqualTo("INVALID_STATE_ERROR");
    }
}
