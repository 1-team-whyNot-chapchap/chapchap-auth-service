package com.chapchap.auth.domain.policy.controller;

import com.chapchap.auth.domain.policy.response.CurrentPolicyResponse;
import com.chapchap.auth.domain.policy.service.PolicyService;
import com.chapchap.auth.global.response.GlobalResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "가입 정책 API", description = "현재 가입 정책 조회")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth/policies")
public class PolicyController {
    private final PolicyService policyService;

    @Operation(summary = "현재 가입 정책 조회", description = "효력이 시작된 현재 정책 Version만 반환합니다.")
    @GetMapping("/current")
    public ResponseEntity<GlobalResponse<List<CurrentPolicyResponse>>> getCurrentPolicies() {
        return GlobalResponse.success(policyService.getCurrentSignupPolicies());
    }
}
