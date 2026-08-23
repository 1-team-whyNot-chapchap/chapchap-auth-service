package com.chapchapauthservice.global.security.policy;

import com.chapchapauthservice.global.security.constant.SessionTypePolicy;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class AuthSessionPolicy {

    // 세션 종류에 따라 최초 생성되는 유휴 만료 시각 계산
    public LocalDateTime calculateIdleExpiresAt(
            SessionTypePolicy sessionType,
            LocalDateTime now
    ) {
        if (sessionType == SessionTypePolicy.ADMIN) {
            return now.plusMinutes(30);
        }

        return now.plusDays(14);
    }

    // 세션 종류에 따라 최초 로그인 기준 절대 만료 시각 계산
    public LocalDateTime calculateAbsoluteExpiresAt(
            SessionTypePolicy sessionType,
            LocalDateTime now
    ) {
        if (sessionType == SessionTypePolicy.ADMIN) {
            return now.plusHours(8);
        }

        return now.plusDays(30);
    }
}