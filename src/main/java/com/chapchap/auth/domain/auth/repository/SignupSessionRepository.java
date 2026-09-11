package com.chapchap.auth.domain.auth.repository;

import com.chapchap.auth.domain.auth.entity.SignupSession;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SignupSessionRepository extends JpaRepository<SignupSession, String> {
    
    // 가입 완료 요청이 동시에 들어와도 하나의 요청만 처리하도록 행 잠금 조회
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT s
            FROM SignupSession s
            WHERE s.id = :signupSessionId
            """) // WHERE s.id = :signupSessionId 의 :signupSessionId은 변수명을 뜻한다
    Optional<SignupSession> findByIdForUpdate(
            @Param("signupSessionId") String signupSessionId
    );
    
    // 동일한 PortOne 본인인증 ID가 이미 사용된 가입 세션에 연결됐는지 확인
    boolean existsByIdentityVerificationId(String identityVerificationId);

}
