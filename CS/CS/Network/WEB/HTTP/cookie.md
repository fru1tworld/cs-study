# Cookie

- 클라이언트 저장소중 하나

### 쿠키 형식

key-value 형식

#### 다른 클라이언트 저장소

- Local Storage
- Indexed DB
- WebSQL
- Cache Storage

#### Cookie Options

- Same Site: 동일한 Site에 대해서만 Cookie가 발송될 수 있도록 활용
  - Strict: 동일 사이트에서만 접속됨
  - Lax: GET 요청의 경우 외부에서도 허용함
  - None: 외부에서 쿠키 사용 허용
- HttpOnly: Http Message 로만 쿠키가 발송될 수 있도록 설정
- Secure: Https Session이 연결된 경우에 대해서만 쿠키가 발송될 수 있도록 설정
- Path: 쿠키가 적용되는 URL 경로 지정
- Domain: 쿠키가 적용될 도메인을 지정

### SameSite

- 같은 프로토콜 및 등록 가능한 도메인을 가질 경우 동일 사이트라고 한다.
- 등록 가능 도메인은 TLD + 1
- TLD는 DNS에서 .com 과 같은 역할을 한다. (root가 .이라서 root 다음)
- TLD + 1은 (example).com 을 의미한다.
- 다시 말해서 TLD + 1이 같으면서 프로토콜이 같으면 동일 사이트라고 한다.

#### Session Cookie와 Persistence Cookie

- Max-Age와 같은 설정을 해두면 디스크에 저장되는 것을 영속 쿠키라고 한다.
- Session Cookie는 브라우저 메모리 성에서만 존재해서, 브라우저를 닫으면 사라짐

#### Cookie Version

- 쿠키는 버전이 존재하고 0, 1로 나뉨 1이 0의 확장 버전

#### 쿠키 저장 한도

- 보통 4kb임
