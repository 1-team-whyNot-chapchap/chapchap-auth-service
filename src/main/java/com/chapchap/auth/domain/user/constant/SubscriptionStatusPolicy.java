package com.chapchap.auth.domain.user.constant;

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
