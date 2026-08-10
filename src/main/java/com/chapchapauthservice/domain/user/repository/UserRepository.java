package com.chapchapauthservice.domain.user.repository;

import com.chapchapauthservice.domain.user.entity.User;
import com.chapchapauthservice.global.security.constant.ProviderPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // 같은 소셜 제공자와 소셜 계정 ID를 가진 회원 조회
    Optional<User> findByProviderAndProviderUserId(
            ProviderPolicy provider, // 카카오, 구글
            String providerUserId // 해당 소셜 서비스가 전달한 계정 고유 Id
    );
}
