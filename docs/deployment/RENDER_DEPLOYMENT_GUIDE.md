# Render 배포 순서

이 문서는 `feature/render-deployment`를 기준으로 Render 대시보드에서 수동 배포할 때 쓴다. 기존 OCI Compute·Nginx·`deploy.yml`은 건드리지 않는다.

학생 이메일 OTP와 학생 대시보드는 이번 검증 범위에서 제외한다. 교사용 학생 등록·관리·커리큘럼·모니터링·과제는 유지한다.

## 1. GitHub 브랜치

1. 로컬이 `feature/render-deployment`인지 확인한다.
2. 이 브랜치를 origin에 push한다. `main`과 `dev`에는 직접 푸시하지 않는다.
3. Render 서비스의 Branch를 `feature/render-deployment`로 둔다. 운영 DNS 전환 전에는 `main`에 합치지 않는다.

## 2. Render 계정에서 서비스 생성

권장 구성은 서비스 2개다.

| 서비스 | 유형 | 역할 |
|---|---|---|
| `lessonpt-api` | Web Service / Docker | Spring Boot |
| `lessonpt-web` | Static Site | React 빌드 산출물 |

저장소 루트의 `render.yaml`을 Blueprint로 가져오거나, 대시보드에서 동일 값으로 만든다. 유료 인스턴스는 고르지 않는다. Web Service plan은 `free`다.

교사 JWT는 `Authorization` 헤더라서 프론트와 API가 다른 호스트여도 CORS만 열면 된다. 학생 세션은 `HttpOnly` 쿠키(`LESSONPT_STUDENT_SESSION`, path `/api/v1/student`)라서 호스트가 갈라지면 이번 MVP에서는 동작하지 않는다고 본다.

## 3. Web Service 설정값

- Runtime: Docker
- Dockerfile path: `./backend/Dockerfile`
- Docker build context: `./backend`
- Health check: `/actuator/health`
- Instance: Free
- 환경: `LESSONPT_PROFILE=prod`

## 4. Static Site 설정값

- Root directory: `frontend`
- Build command: `npm ci && npm run build`
- Publish directory: `dist`
- Rewrite: `/*` → `/index.html`
- Node: 22

## 5. 환경변수 등록

`docs/deployment/RENDER_ENV_CHECKLIST.md`의 Backend 표를 그대로 입력한다. Blueprint의 `sync: false` 항목은 최초 생성 시에만 물어본다. 이후 추가는 대시보드에서 한다.

## 6. Oracle Wallet

1. OCI Console에서 Wallet zip을 받는다. Git에 넣지 않는다.
2. zip 안에 `tnsnames.ora`, `sqlnet.ora`, `cwallet.sso` 등이 있는지 로컬에서만 확인한다.
3. 택1:
   - zip을 Base64로 만들어 `LESSONPT_ORACLE_WALLET_B64`에 넣는다. Base64는 인코딩일 뿐 Secret으로 다룬다.
   - Render Secret File로 zip을 마운트하고 `LESSONPT_ORACLE_WALLET_ARCHIVE`에 경로를 넣는다.
4. `LESSONPT_DB_URL`은 TNS 별칭과 `TNS_ADMIN=/var/lessonpt/wallet`을 쓴다. entrypoint가 Base64 Wallet을 그 경로에 풀고, `sqlnet.ora`의 Windows `WALLET_LOCATION`만 같은 경로로 고친다. 인증서·키 파일은 바꾸지 않는다.

## 7. OCI Object Storage

Instance Principal은 쓰지 않는다. 기존 Java 코드와 같이 `ConfigFileAuthenticationDetailsProvider`다.

1. Object Storage용 IAM 사용자를 준비한다. 가능하면 OCI Compute SSH와 다른, 버킷 권한만 있는 키를 Render 전용으로 만든다.
2. `LESSONPT_OCI_USER_OCID`, `LESSONPT_OCI_TENANCY_OCID`, `LESSONPT_OCI_REGION`, `LESSONPT_OCI_FINGERPRINT`, `LESSONPT_OCI_API_PRIVATE_KEY`를 넣는다.
3. 또는 기존 config + pem을 Secret File로 마운트하고 `LESSONPT_OCI_CONFIG_FILE`만 지정한다.
4. `LESSONPT_OCI_BUCKET_NAME`은 현재 버킷을 유지한다.

config 파일이 없으면 prod는 기동을 유지하되 업로드는 `Object storage is not configured.`로 거절한다. 기존 OCI prod 프로파일의 동작과 같다.

## 8. Backend 최초 배포

환경변수와 Wallet을 넣은 뒤 Deploy한다. Free 인스턴스는 빌드 후 기동까지 수 분이 걸릴 수 있다.

## 9. Health Check

```text
GET https://<api-host>/actuator/health
```

prod는 상세를 숨긴다. `status`가 `UP`이면 Oracle 조회가 된 것이다. `DOWN`이면 Wallet ACL, URL, TNS 별칭, 사용자 비밀번호를 점검한다. 예외 문구에 접속 URL이 나가지 않게 되어 있다.

## 10. Oracle 연결

1. Autonomous DB ACL에 Render outbound IP를 허용한다. Free 인스턴스 IP는 바뀔 수 있다. 검증 단계에서는 임시로 필요한 CIDR만 열고, 운영 전환 전에 범위를 다시 좁힌다.
2. DB가 비공개 전용이면 Render에서 붙지 않는다. Public endpoint 또는 허용된 경로가 필요하다.
3. 스키마·데이터는 변경하지 않는다.

## 11. Object Storage

교사로 로그인한 뒤 Content Detail에 악보 또는 음원을 올린다. 기존 object key 구조를 그대로 쓴다. 실패 시 로그에 PEM이나 fingerprint가 없는지 확인한다.

## 12. Frontend 배포

`VITE_API_BASE_URL`을 `https://<api-host>/api`로 넣고 빌드한다. 값을 바꾸면 Static Site를 다시 빌드해야 한다.

## 13. API 통신

브라우저에서 `/login` → 로그인 또는 `/signup`. 개발자 도구에서 API 호스트로 POST가 나가는지, CORS 오류가 없는지 본다. `LESSONPT_CORS_ALLOWED_ORIGINS`는 프론트 Origin과 스킴·호스트가 같아야 한다. 끝에 `/`를 붙이지 않는다.

## 14. 교사용 Smoke Test

1. 회원가입 또는 기존 교사 로그인
2. 학생 등록
3. 출강처
4. 커리큘럼·내용
5. 배정·모니터링·과제
6. 파일 업로드/다운로드

학생 포털·OTP 메일은 이 단계에서 통과 조건이 아니다. SMTP 환경변수 없이 prod는 기동한다.

## 15. 기존 도메인

`lessonpt.cloud` DNS는 Render 검증이 끝나기 전에는 옮기지 않는다. 옮길 때는 `main` 반영과 레코드 전환을 별도 작업으로 한다. 기존 OCI Nginx 인증서 설정을 이 브랜치에서 바꾸지 않는다.

## 16. 롤백

1. Render 서비스를 Suspend 한다.
2. DNS가 아직 OCI를 가리키면 사용자는 기존 서버를 본다.
3. 이미 DNS를 옮겼다면 레코드를 다시 OCI Compute로 돌린다.
4. Git은 `main`을 되돌리지 않고 feature 브랜치만 고친다.
5. Wallet·키를 Git에 넣은 커밋이 있으면 즉시 키를 폐기하고 재발급한다.
