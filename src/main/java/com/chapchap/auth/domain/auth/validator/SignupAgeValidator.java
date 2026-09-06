package com.chapchap.auth.domain.auth.validator;

import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class SignupAgeValidator {

    private static final int MINIMUM_SIGNUP_AGE = 14;
    
    // 생년월일을 기준으로 현재 만 14세 이상인지 확인
    public boolean isEligible(LocalDate birthDate) {

        if (birthDate == null) {
            return false;
        }

        LocalDate minimumBirthDate = birthDate.plusYears(MINIMUM_SIGNUP_AGE);

        return !minimumBirthDate.isAfter(LocalDate.now());
    }
}
