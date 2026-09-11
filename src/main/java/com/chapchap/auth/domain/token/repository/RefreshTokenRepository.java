package com.chapchap.auth.domain.token.repository;

import com.chapchap.auth.domain.token.entity.RefreshToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

// 리프레시 토큰의 저장/조회 창구
// 재발급 요청에서는 같은 토큰이 동시에 사용되지 않도록 잠금 조회를 사용한다
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    // Entity를 먼저 로드하지 않고 소유자 ID만 읽어 사용자 → 토큰 순서로 잠근다.
    @Query("select token.authSession.user.id from RefreshToken token where token.tokenHash = :tokenHash")
    Optional<Long> findOwnerIdByTokenHash(@Param("tokenHash") String tokenHash);
    
    // tokenHash가 일치하는 토큰과 연결된 세션을 함께 조회한다
    // PESSIMISTIC_WRITE: 조회한 DB 행에 쓰기 잠금을 걸어 현재 요청이 끝날 때 까지 다른 요청이 같은 토큰을 수정하지 못하게 한다
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT refreshToken
        FROM RefreshToken refreshToken
        JOIN FETCH refreshToken.authSession
        WHERE refreshToken.tokenHash = :tokenHash
        """)
    Optional<RefreshToken> findByTokenHashForUpdate(
            @Param("tokenHash") String tokenHash
    );
}
