# PostgreSQL 보안

> 공식 문서: https://www.postgresql.org/docs/current/client-authentication.html

---

## 역할과 권한 관리

### 역할 (Roles)

PostgreSQL은 역할(role) 개념으로 데이터베이스 접근을 관리합니다. 역할은 사용자 또는 그룹으로 동작할 수 있습니다.

### 역할 생성

```sql
-- 기본 역할 생성
CREATE ROLE myuser;

-- 로그인 가능한 역할 (사용자)
CREATE ROLE myuser WITH LOGIN PASSWORD 'password';

-- 또는 CREATE USER 사용 (LOGIN 기본 포함)
CREATE USER myuser WITH PASSWORD 'password';

-- 다양한 속성 지정
CREATE ROLE admin WITH
    LOGIN
    SUPERUSER
    CREATEDB
    CREATEROLE
    INHERIT
    REPLICATION
    BYPASSRLS
    CONNECTION LIMIT 10
    VALID UNTIL '2025-12-31'
    PASSWORD 'securepassword';
```

### 역할 속성

| 속성 | 설명 |
|------|------|
| `LOGIN` | 로그인 가능 |
| `SUPERUSER` | 슈퍼유저 권한 |
| `CREATEDB` | 데이터베이스 생성 권한 |
| `CREATEROLE` | 역할 생성 권한 |
| `REPLICATION` | 복제 연결 권한 |
| `BYPASSRLS` | RLS 정책 우회 |
| `INHERIT` | 멤버 역할 권한 상속 |
| `CONNECTION LIMIT` | 최대 연결 수 |
| `VALID UNTIL` | 패스워드 만료 시간 |

### 역할 수정

```sql
-- 속성 변경
ALTER ROLE myuser WITH CREATEDB;
ALTER ROLE myuser WITH PASSWORD 'newpassword';
ALTER ROLE myuser VALID UNTIL '2026-01-01';

-- 이름 변경
ALTER ROLE myuser RENAME TO newuser;

-- 역할 삭제
DROP ROLE myuser;
```

---

## 역할 멤버십 (그룹)

### 그룹 역할 생성

```sql
-- 그룹 역할 생성
CREATE ROLE developers;
CREATE ROLE readonly;

-- 사용자를 그룹에 추가
GRANT developers TO myuser;
GRANT readonly TO reportuser;

-- 그룹에서 제거
REVOKE developers FROM myuser;
```

### 멤버십 확인

```sql
-- 역할 목록
\du

-- 상세 멤버십
SELECT
    r.rolname AS role,
    ARRAY_AGG(m.rolname) AS members
FROM pg_roles r
LEFT JOIN pg_auth_members am ON r.oid = am.roleid
LEFT JOIN pg_roles m ON am.member = m.oid
GROUP BY r.rolname;
```

---

## 객체 권한 (GRANT/REVOKE)

### 데이터베이스 권한

```sql
-- 데이터베이스 연결 권한
GRANT CONNECT ON DATABASE mydb TO myuser;
REVOKE CONNECT ON DATABASE mydb FROM myuser;

-- 데이터베이스 생성 권한
GRANT CREATE ON DATABASE mydb TO myuser;
```

### 스키마 권한

```sql
-- 스키마 사용 권한
GRANT USAGE ON SCHEMA myschema TO myuser;

-- 스키마 내 객체 생성 권한
GRANT CREATE ON SCHEMA myschema TO myuser;
```

### 테이블 권한

```sql
-- 특정 권한
GRANT SELECT ON users TO readonly;
GRANT SELECT, INSERT, UPDATE ON users TO app_user;
GRANT ALL PRIVILEGES ON users TO admin;

-- 특정 컬럼만
GRANT SELECT (id, name) ON users TO limited_user;
GRANT UPDATE (status) ON orders TO operator;

-- 스키마 내 모든 테이블
GRANT SELECT ON ALL TABLES IN SCHEMA public TO readonly;

-- 향후 생성될 테이블에도 적용
ALTER DEFAULT PRIVILEGES IN SCHEMA public
GRANT SELECT ON TABLES TO readonly;
```

### 시퀀스 권한

```sql
GRANT USAGE, SELECT ON SEQUENCE users_id_seq TO app_user;
GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO admin;
```

### 함수 권한

```sql
GRANT EXECUTE ON FUNCTION my_function(integer) TO myuser;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA public TO app_user;
```

### 권한 확인

```sql
-- 테이블 권한 확인
\dp users

-- SQL로 확인
SELECT grantee, privilege_type
FROM information_schema.table_privileges
WHERE table_name = 'users';
```

---

## 클라이언트 인증 (pg_hba.conf)

### 인증 파일 구조

```
# TYPE  DATABASE    USER        ADDRESS         METHOD
local   all         postgres                    peer
host    all         all         127.0.0.1/32    scram-sha-256
host    all         all         ::1/128         scram-sha-256
host    mydb        myuser      192.168.1.0/24  scram-sha-256
```

### 연결 타입 (TYPE)

| 타입 | 설명 |
|------|------|
| `local` | Unix 소켓 연결 |
| `host` | TCP/IP (SSL 선택) |
| `hostssl` | SSL 필수 |
| `hostnossl` | SSL 불가 |
| `hostgssenc` | GSSAPI 암호화 |

### 인증 방법 (METHOD)

| 방법 | 설명 |
|------|------|
| `trust` | 무조건 허용 (위험!) |
| `reject` | 무조건 거부 |
| `scram-sha-256` | 암호화된 비밀번호 (권장) |
| `md5` | MD5 해시 비밀번호 |
| `password` | 평문 비밀번호 (위험!) |
| `peer` | OS 사용자 이름 확인 |
| `ident` | 원격 ident 서버 |
| `ldap` | LDAP 인증 |
| `cert` | SSL 인증서 |
| `gss` | GSSAPI/Kerberos |

### 예제 설정

```
# 로컬 슈퍼유저
local   all         postgres                    peer

# 로컬 모든 사용자
local   all         all                         scram-sha-256

# 로컬호스트
host    all         all         127.0.0.1/32    scram-sha-256
host    all         all         ::1/128         scram-sha-256

# 특정 네트워크
host    production  app_user    10.0.0.0/8      scram-sha-256

# SSL 필수
hostssl all         all         0.0.0.0/0       scram-sha-256

# 복제 연결
host    replication repl_user   192.168.1.10/32 scram-sha-256
```

### 설정 적용

```bash
# 설정 리로드
pg_ctl reload -D /var/lib/postgresql/data

# 또는 SQL
SELECT pg_reload_conf();
```

---

## SSL/TLS 설정

### 서버 SSL 설정

**postgresql.conf:**

```ini
ssl = on
ssl_cert_file = 'server.crt'
ssl_key_file = 'server.key'
ssl_ca_file = 'root.crt'  # 클라이언트 인증서 검증용
```

### 인증서 생성 (자체 서명)

```bash
# 개인키 생성
openssl genrsa -out server.key 2048
chmod 600 server.key

# 인증서 생성
openssl req -new -x509 -days 365 -key server.key -out server.crt

# CA 인증서 (클라이언트 검증용)
cp server.crt root.crt
```

### 클라이언트 SSL 연결

```bash
# SSL 연결
psql "host=server dbname=mydb sslmode=require"

# SSL 모드 옵션
# disable: SSL 사용 안 함
# allow: 서버가 요구하면 사용
# prefer: SSL 우선 시도
# require: SSL 필수
# verify-ca: CA 인증서 검증
# verify-full: CA + 호스트명 검증

# 인증서로 연결
psql "host=server dbname=mydb sslmode=verify-full sslcert=client.crt sslkey=client.key sslrootcert=root.crt"
```

---

## 행 수준 보안 (Row-Level Security)

### RLS 활성화

```sql
-- RLS 활성화
ALTER TABLE orders ENABLE ROW LEVEL SECURITY;

-- 테이블 소유자에게도 적용
ALTER TABLE orders FORCE ROW LEVEL SECURITY;
```

### 정책 생성

```sql
-- 자신의 데이터만 볼 수 있음
CREATE POLICY user_orders ON orders
    FOR SELECT
    USING (user_id = current_user_id());

-- 특정 역할은 모든 데이터 접근
CREATE POLICY admin_all ON orders
    FOR ALL
    TO admin
    USING (true);

-- INSERT 정책
CREATE POLICY insert_own ON orders
    FOR INSERT
    WITH CHECK (user_id = current_user_id());

-- UPDATE 정책
CREATE POLICY update_own ON orders
    FOR UPDATE
    USING (user_id = current_user_id())
    WITH CHECK (user_id = current_user_id());
```

### 현재 사용자 ID 함수

```sql
-- 세션 변수 사용
CREATE OR REPLACE FUNCTION current_user_id()
RETURNS INTEGER AS $$
BEGIN
    RETURN current_setting('app.user_id')::INTEGER;
EXCEPTION
    WHEN OTHERS THEN
        RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- 애플리케이션에서 설정
SET app.user_id = '123';
```

### 정책 확인

```sql
-- 정책 목록
\d+ orders

-- 상세 확인
SELECT * FROM pg_policies WHERE tablename = 'orders';
```

---

## 데이터 암호화

### 저장 데이터 암호화 (pgcrypto)

```sql
-- 확장 설치
CREATE EXTENSION pgcrypto;

-- 대칭키 암호화
INSERT INTO secrets (data) VALUES
(pgp_sym_encrypt('sensitive data', 'encryption_key'));

SELECT pgp_sym_decrypt(data, 'encryption_key') FROM secrets;

-- 해시
SELECT crypt('password', gen_salt('bf'));

-- 해시 검증
SELECT crypt('password', stored_hash) = stored_hash;
```

### 비밀번호 저장

```sql
-- 안전한 비밀번호 저장
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash TEXT NOT NULL
);

-- 비밀번호 저장
INSERT INTO users (username, password_hash)
VALUES ('john', crypt('mypassword', gen_salt('bf', 12)));

-- 비밀번호 검증
SELECT id FROM users
WHERE username = 'john'
AND password_hash = crypt('mypassword', password_hash);
```

---

## 감사 (Audit)

### 기본 로깅

**postgresql.conf:**

```ini
# 로그 설정
log_statement = 'all'  # none, ddl, mod, all
log_connections = on
log_disconnections = on
log_duration = on
log_line_prefix = '%t [%p]: user=%u,db=%d,app=%a '
```

### 트리거 기반 감사

```sql
-- 감사 테이블
CREATE TABLE audit_log (
    id SERIAL PRIMARY KEY,
    table_name TEXT,
    operation TEXT,
    old_data JSONB,
    new_data JSONB,
    changed_by TEXT DEFAULT current_user,
    changed_at TIMESTAMPTZ DEFAULT NOW()
);

-- 감사 트리거 함수
CREATE OR REPLACE FUNCTION audit_trigger()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        INSERT INTO audit_log (table_name, operation, old_data)
        VALUES (TG_TABLE_NAME, 'DELETE', row_to_json(OLD)::jsonb);
        RETURN OLD;
    ELSIF TG_OP = 'UPDATE' THEN
        INSERT INTO audit_log (table_name, operation, old_data, new_data)
        VALUES (TG_TABLE_NAME, 'UPDATE', row_to_json(OLD)::jsonb, row_to_json(NEW)::jsonb);
        RETURN NEW;
    ELSIF TG_OP = 'INSERT' THEN
        INSERT INTO audit_log (table_name, operation, new_data)
        VALUES (TG_TABLE_NAME, 'INSERT', row_to_json(NEW)::jsonb);
        RETURN NEW;
    END IF;
END;
$$ LANGUAGE plpgsql;

-- 트리거 적용
CREATE TRIGGER users_audit
AFTER INSERT OR UPDATE OR DELETE ON users
FOR EACH ROW EXECUTE FUNCTION audit_trigger();
```

---

## 보안 모범 사례

### 체크리스트

1. **인증**
   - [ ] scram-sha-256 사용
   - [ ] SSL/TLS 활성화
   - [ ] 강력한 비밀번호 정책

2. **권한**
   - [ ] 최소 권한 원칙 적용
   - [ ] PUBLIC 권한 제한
   - [ ] 기본 권한 검토

3. **네트워크**
   - [ ] pg_hba.conf 제한적 설정
   - [ ] 방화벽 구성
   - [ ] SSL 필수화

4. **감사**
   - [ ] 로깅 활성화
   - [ ] 감사 트레일 구현
   - [ ] 정기적인 로그 검토

5. **업데이트**
   - [ ] 정기적인 패치 적용
   - [ ] 보안 공지 모니터링

### 초기 보안 설정

```sql
-- public 스키마 기본 권한 제거
REVOKE CREATE ON SCHEMA public FROM PUBLIC;
REVOKE ALL ON DATABASE mydb FROM PUBLIC;

-- 읽기 전용 역할
CREATE ROLE readonly;
GRANT CONNECT ON DATABASE mydb TO readonly;
GRANT USAGE ON SCHEMA public TO readonly;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO readonly;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT SELECT ON TABLES TO readonly;

-- 애플리케이션 역할
CREATE ROLE app_user WITH LOGIN PASSWORD 'strongpassword';
GRANT CONNECT ON DATABASE mydb TO app_user;
GRANT USAGE ON SCHEMA public TO app_user;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO app_user;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO app_user;
```

---

## 참고 문서

- [역할](https://www.postgresql.org/docs/current/user-manag.html)
- [권한](https://www.postgresql.org/docs/current/ddl-priv.html)
- [클라이언트 인증](https://www.postgresql.org/docs/current/client-authentication.html)
- [행 보안 정책](https://www.postgresql.org/docs/current/ddl-rowsecurity.html)
