package com.chapchap.auth.global.security.constant;

import lombok.Getter;

@Getter
public enum SubscriptionStatusPolicy {

    INACTIVE("INACTIVE"),
    ACTIVE("ACTIVE"),
    UNKNOWN("UNKNOWN");

    private final String status;

    SubscriptionStatusPolicy(String status) {
        this.status = status;
    }
}
