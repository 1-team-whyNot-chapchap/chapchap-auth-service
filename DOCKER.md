# Auth Docker 실행

## 설정

기존 .env가 있으면 보존하고 .env.example과 비교해 누락 항목만 채웁니다. 새 환경은 .env.example을 .env로 복사하고 모든 replace/change-me 예시값을 교체합니다.

- JWT_SECRET: 최소 32바이트 난수를 Base64 인코딩한 값. Gateway와 동일해야 합니다. 예시 키는 공개값입니다.
- IDENTITY_HMAC_SECRET는 JWT와 다른 비밀값입니다. 기존 사용자 identityKey가 있으면 임의 교체하지 않습니다.
- PORTONE_API_SECRET, OAuth client ID/secret, MinIO 계정은 실제 연동값이 필요합니다. OAuth redirect는 GATEWAY_URI + /api/auth/oauth2/callback/{registrationId}와 공급자 등록 URL이 같아야 합니다.
- FRONTEND_CALLBACK_URI는 실제 프론트 경로로 수정합니다. 예시 프론트는 제공되지 않습니다.
- COMPOSE_DB_HOST는 컨테이너에서 접근할 MySQL 주소, DB_PORT/DB_NAME/DB_USER/DB_PASSWORD는 실제 DB 정보입니다. Auth DB는 이 Compose가 생성하지 않습니다.
- JPA_DDL_AUTO 기본 validate: 스키마를 먼저 준비합니다. 기존 update 기본값을 제거했습니다. 임시 로컬 DB에서만 명시적으로 update를 선택하세요. prod는 validate 고정입니다.
- SQL_INIT_MODE 기본 never: 저장소에 dummy SQL이 없으며 임의 데이터 초기화를 수행하지 않습니다. prod도 never 고정입니다.
- KAFKA_LISTENER_AUTO_STARTUP=false는 REST 단독 확인용입니다. Kafka 연동 시 true와 COMPOSE_KAFKA_BOOTSTRAP_SERVERS를 설정합니다. broker의 advertised.listeners도 컨테이너에서 도달 가능해야 합니다.
- COMPOSE_FILE_ENDPOINT_URI는 실제 MinIO 주소입니다. 발급 파일 URL을 사용하는 클라이언트에서도 주소가 해석돼야 합니다. 버킷·권한은 사전 준비합니다.
- GATEWAY_URI는 브라우저가 사용하는 외부 주소로 유지합니다. Docker DNS 이름으로 바꾸지 않습니다.

## 실행

세 서비스가 공유하는 네트워크를 최초 한 번 만듭니다. 이미 있으면 재생성하지 않습니다.

```powershell
docker network create chapchap-network
# 이 저장소 루트에서 실행
docker compose config --quiet
docker compose up -d --build
```

앱은 비root Java 21 컨테이너로 실행되며 호스트 기본 포트는 127.0.0.1:8081, 컨테이너 포트는 8081입니다. 로컬 실행 검증용이며 운영 TLS/Secret 배포 구성은 별도로 필요합니다. Dockerfile은 실제 .env·로컬 키·로그를 빌드 컨텍스트에서 제외합니다.

Gateway와 함께 실행하면 http://localhost:8080/docs 에서 Auth 문서를 확인합니다. 실제 OAuth·본인인증 검증은 유효한 공급자 설정이 있어야 합니다.

내부 Service JWT는 기본 disabled입니다. AI 연동은 RSA 키·client secret과 HTTPS 인증 경로를 준비한 뒤 별도로 활성화합니다. 이 Compose는 TLS 프록시나 AI 활성화 증거를 만들지 않습니다.

## 환경변수와 종료

Compose는 .env를 치환에 사용하고 env_file로 앱에도 전달합니다. 별도 파일은 ENV_FILE과 --env-file을 같은 파일로 지정합니다. Windows PowerShell 예:

```powershell
$env:ENV_FILE='.env.local'
docker compose --env-file .env.local config --quiet
docker compose --env-file .env.local up -d --build
```

Compose 환경 파일은 Docker 문법입니다. PEM을 쓰면 큰따옴표 안의 \\n 줄바꿈을 사용하고, Java properties로 직접 읽는 로컬 .env 형식과 혼용하지 마세요. 컨테이너에는 환경변수로 전달되므로 .env 자체를 복사·마운트하지 않습니다. 비밀값을 출력하는 docker compose config 대신 --quiet를 사용하세요.

중지는 docker compose stop, 컨테이너 제거는 docker compose down입니다. 코드 변경만 반영할 때 up -d --build를 사용합니다.
