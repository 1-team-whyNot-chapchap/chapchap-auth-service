package com.chapchapauthservice.global.security.constant;

import lombok.Getter;

@Getter
public enum RolePolicy {
    CUSTOMER("CUSTOMER"),
    STORE_OWNER("STORE_OWNER"),
    RIDER("RIDER"),
    ADMIN("ADMIN"),
    SUPER_ADMIN("SUPER_ADMIN")
    ;

    private final String role;

    RolePolicy(String role) {
        this.role = role;
    }
}
