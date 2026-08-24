package com.chapchap.auth.domain.admin.service;

import com.chapchap.auth.global.error.custom.business.InvalidParameterException;
import org.springframework.stereotype.Component;

@Component
public class AdminPasswordPolicy {
    public void validate(String password, String confirmation) {
        if (!password.equals(confirmation)
                || password.length() < 10
                || password.length() > 64
                || password.chars().noneMatch(Character::isAlphabetic)
                || password.chars().noneMatch(Character::isDigit)
                || password.chars().anyMatch(Character::isWhitespace)) {
            throw new InvalidParameterException("관리자 비밀번호 정책을 만족하지 않습니다.");
        }
    }
}
