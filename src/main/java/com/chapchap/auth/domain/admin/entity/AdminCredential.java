package com.chapchap.auth.domain.admin.entity;

import com.chapchap.auth.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "admin_credentials", uniqueConstraints = {
        @UniqueConstraint(name = "uk_admin_credentials_user", columnNames = "user_id"),
        @UniqueConstraint(name = "uk_admin_credentials_username", columnNames = "username")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminCredential {
    private static final int MAX_FAILED_LOGIN_COUNT = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "admin_credential_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "username", nullable = false, length = 50, unique = true)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword;

    @Column(name = "failed_login_count", nullable = false)
    private int failedLoginCount;

    @Column(name = "last_failed_at")
    private LocalDateTime lastFailedAt;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "disabled_at")
    private LocalDateTime disabledAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static AdminCredential create(User user, String username, String passwordHash, boolean mustChangePassword) {
        AdminCredential credential = new AdminCredential();
        credential.user = user;
        credential.username = username;
        credential.passwordHash = passwordHash;
        credential.mustChangePassword = mustChangePassword;
        credential.failedLoginCount = 0;
        return credential;
    }

    public boolean isLockedAt(LocalDateTime now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }

    public boolean unlockIfExpired(LocalDateTime now) {
        if (lockedUntil == null || lockedUntil.isAfter(now)) {
            return false;
        }
        resetFailedLoginState();
        return true;
    }

    public boolean recordFailedLogin(LocalDateTime now) {
        failedLoginCount++;
        lastFailedAt = now;
        if (failedLoginCount >= MAX_FAILED_LOGIN_COUNT) {
            lockedUntil = now.plusMinutes(5);
            return true;
        }
        return false;
    }

    public void recordSuccessfulLogin(LocalDateTime now) {
        resetFailedLoginState();
        lastLoginAt = now;
    }

    public void changePassword(String passwordHash, boolean mustChangePassword) {
        this.passwordHash = passwordHash;
        this.mustChangePassword = mustChangePassword;
    }

    public void disable() {
        this.disabledAt = LocalDateTime.now();
    }

    public void unlockManually() {
        resetFailedLoginState();
    }

    private void resetFailedLoginState() {
        failedLoginCount = 0;
        lastFailedAt = null;
        lockedUntil = null;
    }
}
