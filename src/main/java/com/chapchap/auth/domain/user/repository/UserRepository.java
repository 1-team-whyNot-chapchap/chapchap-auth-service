package com.chapchap.auth.domain.user.repository;

import com.chapchap.auth.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByIdentityKey(String identityKey);

    @Query("select u from User u where replace(replace(u.phone, '-', ''), ' ', '') = :phone")
    Page<User> findByExactPhone(@Param("phone") String phone, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select user from User user where user.id = :userId")
    Optional<User> findByIdForUpdate(@Param("userId") Long userId);

}
