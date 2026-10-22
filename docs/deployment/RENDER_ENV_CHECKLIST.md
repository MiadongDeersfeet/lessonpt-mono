# Render 환경변수 점검표

실제 인증값은 적지 않는다. 변수 이름은 현재 코드가 읽는 이름을 그대로 쓴다.

## Backend (`lessonpt-api`)

| 변수명 | 용도 | 필수 | Secret | 입력 위치 | 확인 방법 | 예시 형식 |
|---|---|---|---|---|---|---|
| `LESSONPT_PROFILE` | Spring 프로파일 | 예 | 아니오 | Render Env / Blueprint | 값이 `prod`인지 | `prod` |
| `PORT` | HTTP 포트. Render가 주입 | 예(자동) | 아니오 | Render 런타임 | `/actuator/health` 응답 | `10000` |
| `LESSONPT_DB_URL` | Autonomous DB JDBC URL | 예 | 예 | Render Env | health `oracle`가 UP | `jdbc:oracle:thin:@dbname_high?TNS_ADMIN=/var/lessonpt/wallet` |
| `LESSONPT_DB_USERNAME` | DB 계정 | 예 | 예 | Render Env | health `currentSchema` | `DRUMMER_LMS` |
| `LESSONPT_DB_PASSWORD` | DB 비밀번호 | 예 | 예 | Render Env | 로그인 없이 health만 | (저장소에 적지 않음) |
| `LESSONPT_DB_EXPECTED_SCHEMA` | health/진단용 기대 스키마 | 권장 | 아니오 | Render Env | health 상세(로컬) | `DRUMMER_LMS` |
| `LESSONPT_JWT_SECRET` | 교사 Access/Refresh 서명. 32바이트 이상 | 예 | 예 | Render Env | 강사 로그인 성공 | 32자 이상 임의 문자열 |
| `LESSONPT_ORACLE_WALLET_B64` | Wallet zip의 Base64. 런타임 복원 | 택1 | 예 | Render Env | `tnsnames.ora` 복원 후 DB UP | Base64 한 줄. **암호화가 아님** |
| `LESSONPT_ORACLE_WALLET_ARCHIVE` | 마운트된 Wallet zip 경로 | 택1 | 예 | Render Secret File | 파일 존재 | `/etc/secrets/wallet.zip` |
| `LESSONPT_ORACLE_WALLET_DIR` | 이미 풀린 Wallet 디렉터리 | 택1 | 예 | Render Secret File | `tnsnames.ora` 존재 | `/etc/secrets/wallet` |
| `TNS_ADMIN` | JDBC가 Wallet을 찾는 경로 | 자동/수동 | 예 | entrypoint가 설정 | DB 연결 | `/var/lessonpt/wallet` |
| `LESSONPT_OCI_USER_OCID` | Object Storage API User | 파일 업로드 시 | 예 | Render Env | 악보/음원 업로드 | `ocid1.user.oc1..example` |
| `LESSONPT_OCI_TENANCY_OCID` | Tenancy | 파일 업로드 시 | 예 | Render Env | 업로드 성공 | `ocid1.tenancy.oc1..example` |
| `LESSONPT_OCI_REGION` | Object Storage 리전 | 파일 업로드 시 | 아니오 | Render Env | 기존 config의 region | `ap-seoul-1` |
| `LESSONPT_OCI_FINGERPRINT` | API key fingerprint | 파일 업로드 시 | 예 | Render Env | 기존 config와 동일 | `aa:bb:...` |
| `LESSONPT_OCI_API_PRIVATE_KEY` | PEM 개인키 본문 | 파일 업로드 시 | 예 | Render Env 또는 Secret File | 업로드 성공. 로그에 키가 없는지 | `-----BEGIN PRIVATE KEY-----` |
| `LESSONPT_OCI_CONFIG_FILE` | 기존 OCI config 경로 | 대체 가능 | 예 | Secret File 사용 시 | 파일이 읽히는지 | `/etc/secrets/oci/config` |
| `LESSONPT_OCI_PROFILE` | config 프로파일 이름 | 아니오 | 아니오 | Render Env | 기본 `DEFAULT` | `DEFAULT` |
| `LESSONPT_OCI_BUCKET_NAME` | 수업 자료 버킷 | 파일 업로드 시 | 아니오 | Render Env | 기존 버킷 이름 | `lessonpt-resources` |
| `LESSONPT_STORAGE_PROVIDER` | 저장소 구현 | 아니오 | 아니오 | 생략 가능 | 기본 `oci` | `oci` |
| `LESSONPT_CORS_ALLOWED_ORIGINS` | 프론트 Static Site Origin | 분리 배포 시 예 | 아니오 | Render Env | 브라우저에서 로그인 | `https://lessonpt-web.onrender.com` |
| `JAVA_OPTS` | JVM 메모리 | 아니오 | 아니오 | Render Env | OOM 없이 기동 | Dockerfile 기본값 사용 |

SMTP(`SPRING_MAIL_HOST` 등)는 **필수가 아니다.** 호스트가 없으면 prod는 기동하고 학생 OTP 메일은 보내지 않는다. OCI처럼 메일을 쓰려면 기존 변수 이름을 그대로 넣으면 된다. 교사 가입·로그인은 SMTP를 쓰지 않는다.

Wallet은 `LESSONPT_ORACLE_WALLET_B64`, `LESSONPT_ORACLE_WALLET_ARCHIVE`, `LESSONPT_ORACLE_WALLET_DIR`/`TNS_ADMIN` 중 하나로 준비하면 된다.

Object Storage는 기존처럼 **config 파일 + API Signing Key**다. Render가 OCI Compute가 아니므로 Instance Principal은 쓰지 않는다. 런타임 스크립트가 환경변수로 config와 `key_file`을 만들거나, Secret File로 기존 파일을 마운트한다.

## Frontend (`lessonpt-web`)

| 변수명 | 용도 | 필수 | Secret | 입력 위치 | 확인 방법 | 예시 형식 |
|---|---|---|---|---|---|---|
| `VITE_API_BASE_URL` | API origin + `/api` 접두사. **빌드 시점**에 박힌다 | 예 | 아니오 | Render Static Site Env | 브라우저 네트워크에 `/v1/auth/login` | `https://lessonpt-api.onrender.com/api` |

비밀번호, JWT, Wallet, OCI 개인키는 `VITE_` 변수에 넣지 않는다.

## 코드가 읽지 않는 이름

임의로 바꾸지 말 것: `ORACLE_USER`, `DATABASE_URL`, `JWT_SECRET`(접두사 없음), `OCI_CLI_*`.
