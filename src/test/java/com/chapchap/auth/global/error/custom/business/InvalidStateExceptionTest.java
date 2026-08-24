package com.chapchap.auth.global.error.custom.business;

import com.chapchap.auth.global.response.constant.CustomResponseCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InvalidStateExceptionTest {
    @Test
    void mapsBusinessStateConflictToOneStableErrorCode() {
        InvalidStateException exception = new InvalidStateException("가입 세션이 만료되었습니다.");

        assertThat(exception.getCustomResponseCode()).isEqualTo(CustomResponseCode.INVALID_STATE_ERROR);
        assertThat(exception.getCustomResponseCode().getHttpStatus().value()).isEqualTo(409);
        assertThat(exception.getCustomResponseCode().getCode()).isEqualTo("E20");
    }
}
