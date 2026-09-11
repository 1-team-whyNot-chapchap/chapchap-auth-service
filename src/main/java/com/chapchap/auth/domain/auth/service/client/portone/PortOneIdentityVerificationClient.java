package com.chapchap.auth.domain.auth.service.client.portone;

import com.chapchap.auth.domain.auth.service.client.IdentityVerificationClient;
import com.chapchap.auth.domain.auth.dto.IdentityVerification;
import com.chapchap.auth.domain.auth.dto.PortOneIdentityVerification;
import com.chapchap.auth.domain.auth.dto.PortOneVerifiedCustomer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class PortOneIdentityVerificationClient implements IdentityVerificationClient {
    
    // PortOne V2 REST API 기본 주소
    private static final String PORTONE_BASE_URL = "https://api.portone.io";

    private final RestClient restClient;

    public PortOneIdentityVerificationClient(
            RestClient.Builder restClientBuilder,
            @Value("${PORTONE_API_SECRET}") String apiSecret
    ) {
        
        // API Secret 이 없는 상태로 PortOne 연동이 실행되는 것을 방지
        if (apiSecret == null || apiSecret.isBlank()) {
            throw new IllegalStateException("PortOne API Secret이 설정되지 않았습니다.");
        }
        
        // PortOne 전용 HTTP Client 생성
        // 이후 요청마다 주소와 Authorization Header를 반복해서 작성하지 않도록 기본값으로 등록한다.
        this.restClient = restClientBuilder
                .baseUrl(PORTONE_BASE_URL)
                .defaultHeader(
                        HttpHeaders.AUTHORIZATION,
                        "PortOne " + apiSecret
                )
                .build();
    }

    @Override
    public IdentityVerification getVerification(
            String identityVerificationId
    ) {

        // PortOne V2 본인인증 단건 조회
        // GET//identity-verifications/{identityVerificationId}
        PortOneIdentityVerification response = restClient.get()
                .uri(
                        "/identity-verifications/{identityVerificationId}",
                        identityVerificationId
                )
                .retrieve()
                .body(PortOneIdentityVerification.class);

        // 정상적인 응답 Body 자체가 없는 경우
        if (response == null) {
            throw new IllegalStateException("PortOne 본인인증 조회 결과가 없습니다.");
        }
        // VERIFIED 상태가 아니라면 아직 정상적인 본인인증 완료 결과로 인정하지 않는다.
        if (!"VERIFIED".equals(response.status())) {
            return new IdentityVerification(false,null,null,null,null);
        }

        PortOneVerifiedCustomer customer = response.verifiedCustomer();
        
        // VERIFIED 인데 고객 인증정보가 없는 비정상 응답 방어
        if (customer == null) {
            throw new IllegalStateException("PortOne 본인인증 고객 정보가 없습니다.");
        }
        // PortOne 전용 응답 객체를 Auth-Service 내부에서 사용할 DTO로 변환한다.
        return new IdentityVerification(true, customer.di(), customer.name(), customer.phoneNumber(), customer.birthDate());
    }
}
