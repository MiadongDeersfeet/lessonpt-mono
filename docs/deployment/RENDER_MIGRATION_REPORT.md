# Render 이전 준비 보고서

기준 커밋: `92c359d` (`main`). 작업 브랜치: `feature/render-deployment`. 자동 commit/push는 하지 않았다.

## 1. 현재 프로젝트 아키텍처

- 교사 UI: React 19 + Vite. 교사 토큰은 `sessionStorage` + `Authorization`.
- 학생 UI: 같은 SPA. 세션은 쿠키 `LESSONPT_STUDENT_SESSION`, `credentials: 'include'`.
- API: Spring Boot 4.1.1, 기본 포트 8080, `/api/v1/**`, health `/actuator/health`.
- DB: OCI Autonomous Database. JDBC URL + Oracle Wallet(`oraclepki`). Java는 Wallet을 풀지 않고 `TNS_ADMIN`/URL에 의존한다.
- 파일: OCI Object Storage. 인증은 **config 파일**(`ConfigFileAuthenticationDetailsProvider`). local 프로파일만 실제 클라이언트를 연다. prod는 기존에 `UnavailableObjectStorageGateway`였다.
- 기존 운영: OCI Compute + systemd `LESSONPT_PROFILE=prod` + Nginx가 `/` 정적 파일과 `/api` 프록시. 동일 출처.
- CD: `.github/workflows/deploy.yml`이 `main` push에서 OCI로 배포. **이 파일은 수정하지 않았다.**

## 2. Render용으로 변경한 사항

선택한 배포 형태는 **Web Service(API) + Static Site(프론트)** 이다.

이유:

- 교사 핵심 기능은 Bearer JWT라서 교차 출처가 가능하다.
- Static Site는 무료이고 JVM 메모리(Free 512MB)를 Java에만 쓸 수 있다.
- 단일 컨테이너에 Nginx+JAR를 넣으면 Free 메모리와 이미지 복잡도가 커진다.

제약:

- 학생 쿠키는 동일 출처를 전제로 한다. 분리 배포에서는 학생 OTP/대시보드를 MVP에서 제외한다.
- CORS Origin을 프론트 URL로 열어야 교사 로그인이 된다. 값이 비면 기존 OCI 동일 출처와 같다.

기타 변경:

- `server.address=0.0.0.0`, `server.port=${PORT:8080}` — Render `PORT`와 로컬 8080을 같이 쓴다.
- prod에서 OCI config 파일이 있으면 local과 같은 파일 인증으로 Object Storage를 연다. 없으면 기존처럼 비가용.
- 컨테이너 시작 시 Wallet/OCI config를 복원하는 스크립트. Git·이미지에 Secret 없음.
- Wallet `sqlnet.ora`의 Windows `DIRECTORY`는 런타임 경로(`/var/lessonpt/wallet`)로만 고친다. `cwallet.sso` 등 키 파일은 수정하지 않는다.
- SMTP는 `application-mail.yml`로 분리. `SPRING_MAIL_HOST`가 있을 때만 붙고, Render MVP 기동에는 필요 없다.

## 3. 새로 만든 파일

- `backend/Dockerfile`
- `backend/.dockerignore`
- `backend/scripts/render-entrypoint.sh`
- `render.yaml`
- `.gitignore` (저장소 루트)
- `backend/src/main/java/com/yunki/lessonpt/storage/OciConfigFiles.java`
- `backend/src/main/java/com/yunki/lessonpt/storage/ProdOciObjectStorageConfig.java`
- `backend/src/main/java/com/yunki/lessonpt/common/config/CorsProperties.java`
- `backend/src/main/java/com/yunki/lessonpt/common/config/CorsConfig.java`
- `backend/src/test/java/com/yunki/lessonpt/storage/OciConfigFilesTest.java`
- `backend/src/test/java/com/yunki/lessonpt/common/config/CorsPropertiesTest.java`
- `docs/deployment/RENDER_DEPLOYMENT_GUIDE.md`
- `docs/deployment/RENDER_ENV_CHECKLIST.md`
- `docs/deployment/RENDER_MIGRATION_REPORT.md`
- `backend/src/main/resources/application-mail.yml`
- `backend/src/main/java/com/yunki/lessonpt/relationship/mail/DisabledEmailSender.java`
- `backend/src/test/java/com/yunki/lessonpt/relationship/mail/DisabledEmailSenderTest.java`

## 4. 수정한 파일

- `backend/src/main/resources/application.yml` — listen 주소/포트, CORS 설정 키
- `backend/src/main/java/com/yunki/lessonpt/common/config/SecurityConfig.java` — `cors(Customizer.withDefaults())`
- `backend/src/main/java/com/yunki/lessonpt/storage/OciObjectStorageConfig.java` — 비가용 빈을 `!local & !prod`로 한정
- `frontend/.env.example` — Render용 `VITE_API_BASE_URL` 설명
- `frontend/package.json` — `engines.node = 22.x`
- `backend/src/main/resources/application-prod.yml` — SMTP 필수 플레이스홀더 제거
- `backend/src/main/java/com/yunki/lessonpt/relationship/mail/SmtpEmailSender.java` — JavaMailSender가 있을 때만 활성
- `render.yaml` — SMTP 필수 환경변수 제거

## 5. 변경하지 않은 핵심 기능

- 교사 가입/로그인/JWT, 학생 CRUD, 출강처, 커리큘럼, 배정, 모니터링, 과제, 진도 계산
- Oracle 스키마, MyBatis SQL, 도메인 서비스
- `deploy.yml`, `ci.yml`, `deploy-smoke.yml`
- `application-prod.yml`의 DB/JWT 바인딩 이름
- Object Storage SDK 호출부(`OciObjectStorageGateway` put/get/delete, object key)
- 학생 OTP 구현과 `SmtpEmailSender`는 보존. SMTP 호스트가 없으면 메일을 보내지 않고 기동한다.

## 6. 성공한 테스트

- Backend `./mvnw -B test`: Tests run 453, Failures 0, Errors 0, Skipped 30 (Oracle 통합 테스트는 `LESSONPT_DB_URL` 없을 때 skip. 기존과 동일)
- SMTP 분리 후 `EmailSenderProfileTest`(5), `DisabledEmailSenderTest`, `TeacherAuthSecurityTest`(4) 통과
- 신규 `OciConfigFilesTest`, `CorsPropertiesTest` 통과
- `LessonPtApplicationTests` health(Oracle 미연결 DOWN) 통과
- Frontend `npm run lint` (기존 warning만), `npm test` 141 passed, `npm run build` 성공
- `./mvnw -B -DskipTests package`로 JAR 생성

## 7. 실패하거나 실행하지 못한 테스트

- Docker 이미지 빌드: 이 호스트에 `docker` 명령이 없어 **검증하지 못함**. Render가 Linux에서 Dockerfile을 빌드한다.
- Autonomous DB / Wallet / Object Storage 실접속: 인증값을 넣지 않음.
- Render Health·CORS·업로드: 서비스를 생성하지 않음.
- 로컬 Node는 v24라 `engines.node = 22.x` 경고가 난다. Render Static Site는 `NODE_VERSION=22`와 `.nvmrc`를 쓴다.

## 8. 남은 차단 요인

1. Wallet, DB 비밀번호, JWT secret, OCI API 키를 사용자가 Render에 넣어야 한다. SMTP는 학생 OTP를 켤 때만 넣는다.
2. Autonomous DB ACL이 Render outbound를 허용해야 한다.
3. `VITE_API_BASE_URL`은 빌드 시점 값이다. API URL이 바뀌면 프론트를 다시 빌드한다.
4. Render Free는 유휴 시 일시 중지, 휘발성 디스크, 대역폭 제한이 있다. Wallet은 매 기동마다 복원한다.
5. 학생 포털은 분리 배포에서 쿠키 SameSite 제약이 있다.
6. 현재 OCI 운영의 Object Storage는 prod 프로파일에서 Java 클라이언트가 꺼져 있다. Render에서 파일을 쓰려면 config 파일을 반드시 주입해야 한다.

## 9. 수동 입력이 필요한 정보

체크리스트 문서의 Secret 항목. 운영 OCI Compute의 키를 그대로 복사하는 것보다, Object Storage 전용 IAM 사용자를 새로 만드는 쪽을 권장한다. DB Wallet은 같은 Autonomous DB를 쓰므로 기존 Wallet을 Render Secret으로만 옮긴다. Git에 두지 않는다.

## 10. 실제 Render 배포 전 확인사항

- [ ] feature 브랜치에만 반영되어 있고 `main`은 그대로다
- [ ] Git status에 Wallet/pem이 없다
- [ ] `render.yaml`에 실제 비밀번호가 없다
- [ ] CORS Origin과 Static Site URL이 일치한다
- [ ] JDBC URL의 TNS 별칭이 Wallet `tnsnames.ora`와 같다
- [ ] DB ACL
- [ ] SMTP 없이도 prod가 기동한다
- [ ] 기존 `lessonpt.cloud` 레코드를 아직 바꾸지 않는다

## 11. OCI 보호 및 롤백

- 기존 서버, Nginx, systemd, DNS, `deploy.yml` 미변경
- Render는 병렬 환경이다. 검증 전에는 트래픽이 OCI에 남는다
- 문제 시 Render Suspend + DNS 유지/원복
- 이 브랜치를 `main`에 넣기 전에는 GitHub Actions OCI 배포가 이 변경을 나르지 않는다
