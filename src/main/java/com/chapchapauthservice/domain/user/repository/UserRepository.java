package com.chapchapauthservice.domain.user.repository;

import com.chapchapauthservice.domain.user.entity.User;
import com.chapchapauthservice.global.security.constant.ProviderPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByIdentityKey(String identityKey);

}
