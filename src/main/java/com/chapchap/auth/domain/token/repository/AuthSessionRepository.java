package com.chapchap.auth.domain.token.repository;

import com.chapchap.auth.domain.token.entity.AuthSession;
import com.chapchap.auth.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// 로그인 세션의 저장/조회 창구
// save(), findById() 같은 기본 기능은 JpaRepository가 제공한다
public interface AuthSessionRepository extends JpaRepository<AuthSession, Long> {
    List<AuthSession> findAllByUser(User user);
}
