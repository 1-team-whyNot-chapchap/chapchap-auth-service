package com.chapchap.auth.domain.user.service;

import com.chapchap.auth.domain.token.repository.AuthSessionRepository;
import com.chapchap.auth.domain.audit.service.AuditLogService;
import com.chapchap.auth.domain.policy.repository.UserPolicyConsentRepository;
import com.chapchap.auth.domain.user.entity.SocialAccount;
import com.chapchap.auth.domain.user.entity.User;
import com.chapchap.auth.domain.user.repository.SocialAccountRepository;
import com.chapchap.auth.domain.user.repository.UserRepository;
import com.chapchap.auth.domain.user.response.UserProfileResponse;
import com.chapchap.auth.global.error.custom.business.NotFoundResourceException;
import com.chapchap.auth.global.error.custom.business.InvalidStateException;
import com.chapchap.auth.global.minio.MinioManager;
import com.chapchap.auth.global.kafka.producer.AuthEventProducer;
import com.chapchap.auth.global.security.constant.ConsentStatusPolicy;
import com.chapchap.auth.global.security.constant.RolePolicy;
import com.chapchap.auth.global.security.constant.SubscriptionStatusPolicy;
import com.chapchap.auth.global.security.constant.UserStatusPolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileService {
    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final AuthSessionRepository authSessionRepository;
    private final UserPolicyConsentRepository userPolicyConsentRepository;
    private final MinioManager minioManager;
    private final AuthEventProducer authEventProducer;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public UserProfileResponse getMyProfile(Long userId) {
        User user = getEligibleUser(userId);
        List<SocialAccount> accounts = socialAccountRepository.findAllByUser(user);
        return toProfileResponse(user, accounts);
    }

    @Transactional
    public UserProfileResponse replaceProfileImage(Long userId, MultipartFile file) {
        User user = getEligibleUser(userId);
        String previousKey = user.getProfileImageKey();
        String newKey = minioManager.generateProfileImageObjectKey(file);
        minioManager.uploadFile(newKey, file);

        try {
            user.changeProfileImageKey(newKey);
            userRepository.saveAndFlush(user);
            deleteAfterCommit(previousKey);
            return toProfileResponse(user, socialAccountRepository.findAllByUser(user));
        } catch (RuntimeException exception) {
            user.changeProfileImageKey(previousKey);
            deleteImmediatelyOrLog(newKey, "새 프로필 이미지 정리에 실패했습니다.");
            throw exception;
        }
    }

    @Transactional
    public void deleteProfileImage(Long userId) {
        User user = getEligibleUser(userId);
        String previousKey = user.getProfileImageKey();
        user.changeProfileImageKey(null);
        userRepository.saveAndFlush(user);
        deleteAfterCommit(previousKey);
    }

    @Transactional
    public void withdraw(Long userId) {
        User user = getEligibleUser(userId);
        if (user.getSubscriptionStatus() != SubscriptionStatusPolicy.INACTIVE) {
            throw new InvalidStateException("비활성 구독 상태에서만 탈퇴할 수 있습니다.");
        }

        String previousImageKey = user.getProfileImageKey();
        authSessionRepository.findAllByUser(user).forEach(session -> session.revoke());
        userPolicyConsentRepository.findAllByUser(user).forEach(consent -> {
            if (consent.getConsentStatus() == ConsentStatusPolicy.AGREED) {
                consent.withdrawMarketingConsentIfAgreed();
            }
        });
        socialAccountRepository.deleteAllByUser(user);
        user.withdraw();
        userRepository.saveAndFlush(user);
        auditLogService.recordUserWithdrawal(userId);
        authEventProducer.publishUserWithdrawnAfterCommit(userId, user.getWithdrawnAt());
        deleteAfterCommit(previousImageKey);
    }

    private User getEligibleUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundResourceException("사용자를 찾을 수 없습니다."));
        if (user.getStatus() != UserStatusPolicy.ACTIVE
                || (user.getRole() != RolePolicy.CUSTOMER && user.getRole() != RolePolicy.RIDER)) {
            throw new InvalidStateException("일반 사용자 프로필에 접근할 수 없습니다.");
        }
        return user;
    }

    private UserProfileResponse toProfileResponse(User user, List<SocialAccount> accounts) {
        String profileImageUrl = user.getProfileImageKey() == null
                ? null
                : minioManager.createMinioObjectUri(user.getProfileImageKey());
        return new UserProfileResponse(
                user.getName(),
                user.getPhone(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                user.getDefaultAddressId(),
                accounts.stream().map(SocialAccount::getProvider).toList(),
                profileImageUrl
        );
    }

    private void deleteAfterCommit(String objectKey) {
        if (objectKey == null) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deleteImmediatelyOrLog(objectKey, "기존 프로필 이미지 삭제에 실패했습니다.");
            }
        });
    }

    private void deleteImmediatelyOrLog(String objectKey, String failureMessage) {
        try {
            minioManager.deleteFile(objectKey);
        } catch (RuntimeException exception) {
            log.warn(failureMessage);
        }
    }
}
