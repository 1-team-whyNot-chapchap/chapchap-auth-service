# Internal Service JWT와 JWKS 설계

## 1. 목표

Auth-Service가 등록된 내부 서비스에만 짧은 수명의 RS256 Service JWT를 발급하고, 검증 서비스가 공개키를 조회할 수 있는 JWKS를 제공한다. 기존 사용자·관리자 HS256 JWT 발급과 검증은 변경하지 않는다.

## 2. 활성화와 경계

- `internal-service-jwt.enabled=false`를 기본값으로 두며, 비활성 상태에서는 토큰·JWKS endpoint와 관련 Bean을 등록하지 않는다.
- 토큰과 JWKS는 Gateway 공개 경로에 추가하지 않고 서비스 네트워크에서 직접 호출한다.
- 서비스 클라이언트, audience, scope는 설정 allowlist에 등록된 조합만 허용한다.
- 개인키와 client secret은 환경변수로만 주입하며 응답·예외·로그에 포함하지 않는다.
- DB 스키마, 사용자 JWT, OAuth 로그인, Refresh Token 흐름은 수정하지 않는다.

## 3. HTTP 계약

### Service Token

- `POST /internal/v1/service-tokens`
- Content-Type: `application/x-www-form-urlencoded`
- 요청: `grant_type=client_credentials`, `client_id`, `client_secret`, `audience`, `scope`
- 성공: `200 OK`

```json
{
  "access_token": "<signed JWT>",
  "token_type": "Bearer",
  "expires_in": 300,
  "scope": "customer-ai.invoke"
}
```

- 잘못된 요청은 `400`, client 인증 실패는 `401`, 허용되지 않은 audience/scope는 `403`으로 응답한다.

### JWKS

- `GET /.well-known/jwks.json`
- 성공: `200 OK`
- 현재 활성 공개키를 `kty=RSA`, `use=sig`, `alg=RS256`, `kid`, `n`, `e`로 반환한다.

## 4. JWT 계약

| 항목 | 값 |
|---|---|
| `alg` | `RS256` |
| header `kid` | 설정된 활성 key ID |
| `iss` | `chapchap-auth-service` |
| `sub` | 등록된 서비스 identity |
| `aud` | 요청한 allowlisted audience |
| `scope` | 정규화한 공백 구분 scope |
| `iat`, `exp` | KST Clock에서 계산한 NumericDate, 최대 300초 |
| `jti` | 요청마다 생성하는 UUID |

초기 허용 계약은 Customer-Service의 `customer-service -> chapchap-customer-ai / customer-ai.invoke`이며, 역방향 callback은 `customer-ai -> chapchap-customer-service / customer-ai.callback` client 설정으로 같은 발급기를 재사용한다.

## 5. 키와 설정

- PKCS#8 RSA private key와 X.509 RSA public key PEM을 각각 환경변수로 받는다.
- 키는 최소 2048 bit인지 확인하고 시작 시 서명·검증 round-trip으로 한 쌍인지 검증한다.
- 만료 시간은 1~300초만 허용한다.
- client secret은 최소 32자이며 상수 시간 비교를 사용한다.
- 시간대는 `Asia/Seoul`로 고정하되 JWT NumericDate는 표준 epoch seconds로 기록한다.

## 6. 검증

- 정상 발급 토큰의 header와 claim, RS256 서명을 공개키로 검증한다.
- client, secret, grant type, audience, scope 오류를 각각 차단한다.
- 요청 scope 중복 제거와 안정적인 정렬을 확인한다.
- JWKS modulus/exponent가 실제 공개키와 일치하는지 확인한다.
- 비활성 상태에서 endpoint가 등록되지 않는지 확인한다.
- 전체 기존 테스트로 사용자 인증 회귀를 확인한다.

## 7. 구현 결과

- `feat/internal-service-jwt-jwks` 브랜치에서 구현했다.
- Service Token endpoint, RS256 issuer, 설정 client allowlist, JWKS endpoint를 추가했다.
- Customer-Service 호출과 Customer-AI callback의 양방향 audience/scope를 설정으로 제한했다.
- 잘못된 grant, client, secret, audience, scope를 안정적인 `400/401/403` 오류로 축소했다.
- 기존 사용자 HS256 JWT 코드는 변경하지 않았다.
- `./gradlew.bat compileJava --no-daemon` 통과.
- `./gradlew.bat test --rerun-tasks --no-daemon` 통과: 37 tests, failures 0, errors 0, skipped 0.
- `git diff --check` 통과.

운영 키와 client secret은 저장소에 포함하지 않았다. 실제 값 배포와 HTTPS 내부 주소 확정 전에는 `INTERNAL_SERVICE_JWT_ENABLED=false`를 유지한다.