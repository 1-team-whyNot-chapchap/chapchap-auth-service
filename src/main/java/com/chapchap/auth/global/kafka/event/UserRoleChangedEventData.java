package com.chapchap.auth.global.kafka.event;

public record UserRoleChangedEventData(String previousRole, String newRole) {
}
