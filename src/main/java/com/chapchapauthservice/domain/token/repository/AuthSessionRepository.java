package com.chapchapauthservice.domain.token.repository;

import com.chapchapauthservice.domain.token.entity.AuthSession;
import org.springframework.data.jpa.repository.JpaRepository;

// 로그인 세션의 저장/조회 창구
// save(), findById() 같은 기본 기능은 JpaRepository가 제공한다
public interface AuthSessionRepository extends JpaRepository<AuthSession, Long> {
}
