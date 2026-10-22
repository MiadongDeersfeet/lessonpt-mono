# Render 시험 배포

기준: main의 92c359d9d53bce187eb373cd283d4bd8d6468b30. 현재 파일들은 별도 준비 브랜치에 있으며 배포 완료를 의미하지 않는다.

## 구성

- Web Service 하나: nginx가 React를 제공하고 /api를 같은 컨테이너의 Spring Boot로 전달한다.
- Free, Singapore, main, Dockerfile: deploy/render/Dockerfile, build context: 저장소 루트(.).
- Auto-Deploy: Off. HTTP health check: /healthz. 신규 DB·디스크·워커는 만들지 않는다.
- Docker 이미지가 prod,render 프로파일을 활성화한다. 기존 OCI 서비스의 prod 단독 동작은 유지한다.
- 학생 이메일 인증 폐지 예정에 따라 Render에서는 OTP 발급/검증을 503으로 막는다. 서버 메일 구현도 실패하도록 해 OTP나 인증 없는 접근으로 대체하지 않는다. 기존 인증된 학생 세션을 강제 삭제하지는 않는다.
- 교사 로그인, Oracle, OCI 파일 저장소는 유지한다.

## 기본 방어

- API 전체 초당 10건, 순간 추가 20건. 교사 가입/로그인/갱신은 전체 분당 30건, 순간 추가 10건. 초과 시 429.
- API 동시 처리 20건, POST/PUT 동시 처리 2건. 모두 단일 인스턴스 전체 기준이며 사용자별 한도가 아니다.
- 임의의 X-Forwarded-For 값을 믿고 IP별 제한을 적용하지 않는다. 공격자가 전체 한도를 소진하면 정상 사용자도 거절될 수 있다.
- 요청 본문 55MiB, 인증 요청 16KiB. 기존 PDF 20MiB·음원 50MiB 및 활성 저장량 7GiB 검사는 유지한다.
- Spring은 loopback:8080에만 바인딩한다. 외부 PORT는 nginx가 받는다. /actuator 직접 접근은 404, /healthz만 상태 확인을 허용한다.
- DB 풀 최대 3개, 서버 작업 스레드 최대 30개. JVM 최대 힙 비율 60%는 초기값이며 512MB에서 실제 최대 사용량은 아직 검증하지 않았다.
- 비루트 사용자, 이미지에 비밀 파일 미포함, access log 비활성. 요청 제한은 전송량 과금이나 플랫폼 수준 DDoS의 완전 차단을 보장하지 않는다.
- /healthz는 Oracle 의존 health를 사용한다. DB 장애가 길어지면 Render가 재시작할 수 있으며, 실제 연결 지연은 5초 health 제한 내인지 확인해야 한다.

## Render에 직접 입력할 값

필수 환경변수:

- LESSONPT_DB_URL
- LESSONPT_DB_USERNAME
- LESSONPT_DB_PASSWORD
- LESSONPT_JWT_SECRET (기존 운영 비밀과 분리된 최소 32바이트 비밀 권장)

OCI secret files:

- oci-config: OCI config 파일. key_file은 /etc/secrets/oci-key.pem 등 실제 마운트 경로를 사용한다.
- oci-key.pem: 해당 config의 개인키. Git이나 채팅에 붙여 넣지 않는다.

선택 환경변수:

- LESSONPT_OCI_CONFIG_FILE (기본 /etc/secrets/oci-config)
- LESSONPT_OCI_PROFILE (기본 DEFAULT)
- LESSONPT_OCI_BUCKET_NAME (기본 lessonpt-resources)
- LESSONPT_DB_EXPECTED_SCHEMA (기본 DRUMMER_LMS)

SMTP 환경변수는 Render 프로파일에서 필요하지 않다. Oracle이 mTLS Wallet을 요구하면 Wallet 전달 방식과 JDBC/TNS 경로를 별도로 확정해야 한다. 아직 실제 DB 주소, 허용 IP, Wallet 필요 여부를 확인하지 않았다.

## 배포 전 확인

1. main에 이 준비 변경이 반영돼야 Dockerfile을 찾을 수 있다. main 변경은 기존 GitHub Actions OCI 배포도 자동 실행하므로 검토 후 진행한다.
2. 새 Render 서비스가 기존 Oracle/버킷을 사용하면 실제 운영 데이터에 접근한다. 시험 계정·범위와 백업 상태를 확인한다.
3. 필요한 비밀은 Render에서 직접 입력하고 화면 공유·로그·Git에 노출하지 않는다.
4. 컨테이너 빌드 및 nginx -t, 정적 파일 새로고침, /healthz, 교사 인증, 요청 초과 429, 학생 OTP 503, 파일 업로드/다운로드를 시험한다.
5. 실제 외부 서비스 쓰기 시험은 별도 테스트 계정·파일로 수행한다. 이번 준비 중에는 실행하지 않았다.

## 현재 계정에서 확인한 사항 (2026-10-22)

Hobby, 카드 없음, 빌드 추가 지출 한도 월 $0. 포함량은 워크스페이스 합산 750시간·5GB 전송·500분 빌드. 기존 다른 서비스 1개가 있으므로 무료 시간·전송량을 함께 소비한다. 한도와 가격은 배포 시 다시 확인한다.

운영 주소/DNS는 전환하지 않는다. 무료 Render의 대기 후 기동 지연과 메모리 한도를 확인한 뒤 전환을 별도 판단한다.
