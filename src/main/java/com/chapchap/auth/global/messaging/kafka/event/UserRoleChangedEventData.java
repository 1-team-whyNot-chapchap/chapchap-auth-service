package com.chapchap.auth.global.messaging.kafka.event;

public record UserRoleChangedEventData(String previousRole, String newRole) {
}
