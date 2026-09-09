package com.chapchap.auth.domain.user.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminUserSearchRequest(
        @NotBlank @Size(max = 30)
        @Schema(description = "국내 전체 휴대폰 번호. 공백과 하이픈 허용", example = "010-1234-5678")
        String phone,
        @Min(0) @Max(1000000)
        @Schema(description = "0부터 시작하는 페이지. 페이지당 20명", defaultValue = "0")
        int page
) {
}
