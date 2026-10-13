# Newslit Backend

## Local Oracle Database

로컬과 테스트는 Oracle Database Free를 사용하고, 운영은 Oracle Autonomous Database를 사용한다.

로컬 DB를 실행한다.

```bash
docker compose up -d oracle
docker compose ps
```

기본 접속 정보는 다음과 같다.

- JDBC URL: `jdbc:oracle:thin:@//localhost:1521/FREEPDB1`
- 사용자: `newslit`
- 비밀번호: `newslit-local`

기본값을 바꾸려면 `backend/.env`에 아래 값을 설정한다. `.env`는 Git에서 제외된다.

```dotenv
ORACLE_ADMIN_PASSWORD=change-me
ORACLE_LOCAL_USERNAME=newslit
ORACLE_LOCAL_PASSWORD=change-me
```

애플리케이션은 같은 이름의 환경 변수를 읽으므로 Compose와 Spring Boot의 인증 정보가 함께 변경된다.

```bash
./gradlew bootRun
```

DB를 중지하되 데이터를 보존하려면 `docker compose down`을 사용한다. 볼륨까지 초기화하려면 로컬 데이터가 삭제되는 것을 확인한 뒤 `docker compose down -v`를 실행한다.

## Test

Docker가 실행 중인 상태에서 테스트한다. 테스트 DB는 별도로 띄울 필요가 없으며 Testcontainers가 Oracle Database Free 컨테이너를 자동으로 생성하고 제거한다.

```bash
./gradlew test
```
