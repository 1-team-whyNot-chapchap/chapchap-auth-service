package com.chapchap.auth.domain.user.request;

import jakarta.validation.constraints.AssertTrue;

/** 회원 탈퇴는 의도 재확인을 받은 경우에만 처리한다. */
public record WithdrawalRequest(
        @AssertTrue(message = "회원 탈퇴 확인이 필요합니다.")
        Boolean confirmed
) {
}
