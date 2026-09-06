package com.chapchap.auth.domain.admin.repository;

import com.chapchap.auth.domain.admin.entity.AdminCredential;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AdminCredentialRepository extends JpaRepository<AdminCredential, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select credential
            from AdminCredential credential
            join fetch credential.user
            where credential.username = :username
            """)
    Optional<AdminCredential> findByUsernameForUpdate(@Param("username") String username);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select credential
            from AdminCredential credential
            join fetch credential.user
            where credential.user.id = :userId
            """)
    Optional<AdminCredential> findByUserIdForUpdate(@Param("userId") Long userId);

    Optional<AdminCredential> findByUserId(Long userId);

    boolean existsByUsername(String username);
}
