package com.chapchapauthservice.domain.user.repository;

import com.chapchapauthservice.domain.user.entity.SocialAccount;
import com.chapchapauthservice.domain.user.entity.User;
import com.chapchapauthservice.global.security.constant.ProviderPolicy;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {

    // 같은 소셜 제공자와 소셜 계정 ID를 가진 회원 조회
    @EntityGraph(attributePaths = "user")
    Optional<SocialAccount> findByProviderAndProviderUserId(
            ProviderPolicy provider, // 카카오, 구글
            String providerUserId // 해당 소셜 서비스가 전달한 계정 고유 Id
    );


    // 사용자가 해당 Provider의 소셜 로그인 수단을 이미 가지고 있는지 확인
    boolean existsByUserAndProvider(
            User user,
            ProviderPolicy provider
    );

    // 회원 탈퇴 시 해당 사용자의 모든 소셜 로그인 연결을 삭제
    // 사용자는 Kakao와 Google 계정을 동시에 연결할 수 있으므로 전체 삭제가 필요
    void deleteAllByUser(User user);
}
