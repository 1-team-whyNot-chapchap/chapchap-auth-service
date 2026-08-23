package com.chapchapauthservice.domain.policy.repository;

import com.chapchapauthservice.domain.policy.entity.Policy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface PolicyRepository extends JpaRepository<Policy, Long> {
    
    // 현재 활성화되어 있고 효력 시작 시작이 지난 가입 정책 조회
    // optional이 아닌 List를 쓰는 이유는 종류별 정책을 보여줘야 하기에 List를 사용하는게 맞다.
    List<Policy> findAllByActiveTrueAndEffectiveAtLessThanEqual(
            LocalDateTime currentTime
    );
}
