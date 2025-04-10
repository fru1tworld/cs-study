# 3장 HTTP 메시지

## 3.1 메시지의 흐름

인바운드, 아웃바운드, 업스트림, 다운스트림은 메시지의 방향을 의미하는 용어다.

### 3.1.1 메시지는 원 서버 방향을 인바운드로 하여 송신된다.

HTTP는 인바운드와 아웃바운드라는 용어를 트랜잭션 방향을 표현하기 위해 사용한다.

메시지가 원 서버로 가는 것을 인바운드라고 한다.

### 3.1.2 다운 스트림으로 흐르는 메시지

HTTP 메시지는 다운 스트림으로 흐른다.

(업스트림과 같은 용어는 발송지와 수신자에 대한 것으로 어느 방향이든 다운 스트림이다.)

## 3.2 메시지의 각 부분

### 3.2.1 메시지 문법

Request

```
<메서드><요청 URL><버전>
<헤더>

<엔터티 본문>
```

```
<버전><상태 코드><사유 구절>
<헤더>

<엔터티 본문>
```

#### 사유 구절(reason-pharase)

- 200 OK 혹은 200 NOT OK와 상관없이 모두 똑같이 성공을 의미해야한다.

## 3.3 메서드

### 3.3.1 안전한 메서드

### 3.3.2 GET

### 3.3.3 HEAD

HEAD는 GET처럼 동작하는데 응답으로 헤더만을 돌려준다.

### 3.3.6 TRACE

클라이언트가 어떤 요청을 할 때, 그 요청은 방화벽, 프락시, 게이트웨이 등의 애플리케이션을 통과할 수 있다.

이들은 원래 HTTP 요청을 수정할 수 있는 기회가 있는다.

TRACE 요청은 목적지 서버에서 루프백 진단을 한다.

주로 진단을 위해 사용한다.

### 3.3.7 OPTIOS

OPTIONS 메서드는 웹 서버에게 여러 가지 종류의 지원 범위에 대해 물ㅇ본다.

서버에게 특정 리소스에 대해 어떤 메서드가 지원되는지 물어볼 수 있다.

### 3.3.9 확장 메서드

HTTP는 필요에 따라 확장해도 문제가 없도록 설계되어있으므로, 새로 기능을 추가해도 과거에 구현된 소프트웨어들의 오동작을 유발하지 않는다.

## 3.4 상태 코드

### 3.4.1 100-199: 정보성 상태 코드

| 상태 코드 | 사유 구절          | 의미                                                                                                                                                    |
| --------- | ------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 100       | Continue           | 요청의 시작 부분 일부가 받아들여졌으며, 클라이언트는 나머지를 계속이어서 보내야 함을 의미한다. 이것을 보낸 후, 서버는 반드시 요청을 받아 응답해야 한다. |
| 101       | Switching Protocol | 클라이언트가 Upgrade 헤더에 나열한 것 중 하나로 서버가 프로토콜을 바꾸었음을 의미한다.                                                                  |

### 3.4.2 200-299: 성공 상태 코드

| 상태 코드 | 사유 구절                     | 의미                                                                                                                                                                                                |
| --------- | ----------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 200       | OK                            | 요청은 정상이고, 엔터티 본문은 요청된 리소스를 퐇마하고 있다.                                                                                                                                       |
| 201       | Created                       | 서버 개체를 생성하라는 요청을 위한 것. 응답은, 생성된 리소스에 대한 최대한 구체적이고 참조가 담긴 Location 헤더와 함께 그 리소스를 참조할 수 있는 여러 URL을 엔터티 본문에 포함해야 한다.           |
| 202       | Accepted                      | 요청은 받아들여졌으나 서버는 아직 그에 대한 어떤 동작도 수행하지 않았다. 서버는 요청의 처리를 완료할 것인지에 대한 어떤 보장도 없다. 이것은 단지 요청이 받아들이기에 적법해 보인다는 의미일 뿐이다. |
| 203       | Non-Authoritative Information | 엔터티 헤더에 들어있는 정보가 원래 서버가 아닌 리소스의 사본에서 왔다. 중개자가 리소스의 사뵨을 갖고 있었지만 리소스에 대한 메타 정보를 검증하지 못한 경우 이런 일이 발생할 수 있다.                |
| 204       | No Content                    | 헤더와 상태줄을 포함하지만 본문은 없다. 주로 웹브라우저를 새 문서로 이동시키지 않고 갱신하고자 할 때 사용한다.                                                                                      |
| 205       | Reset Content                 | 주로 브라우저를 위해 사용되는 또 하나의 코드 브라우저에게 현재 페이지에 있는 HTML 폼에 채워진 모든 값을 배우라고 말한다.                                                                            |
| 206       | Partial Content               | 부분 혹은 범위 요청이 성공했다. 나중에 클라이언트가 특별한 헤더를 사용해서 문서의 부분 혹은 특정 범위를 요청할 수 있다는 것을 보게 될 것이다.                                                       |

### 3.4.3 300-399: 리다이렉션 코드

리다이렉션 상태 코드는 클라이언트가 관심 있어하는 리소스에 대해 다른 위치를 사용하라고 말해주거나 그 리소스의 내용 대신 다른 대안 응답을 제공한다.

만약 리소스가 옮겨졌다면, 클라이언트에게 리소스가 옮겨졌으며 어디서 찾을 수있는지 알려주기 위해 리다이렉션 상태 코드와 Location 헤더를 보낼 수 있다.

이는 브라우저가 사용자를 귀찮게 하지 않고 알아서 새 위치로 이동할 수 있게 해준다.

| 상태 코드 | 사유 구절          | 의미                                                                                                                                                                                                                                                                                                                                                                                                                    |
| --------- | ------------------ | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 300       | Multiple Choices   | 클라이언트가 동시에 여러 리소스를 가리키는 URL을 요청하는 경우 그 리소스의 목록과 함께 반환한다. 사용자는 목록에서 원하는 하나를 선택할 수 있다.                                                                                                                                                                                                                                                                        |
| 301       | Moved Permanently  | 요청한 URL이 이동했을 때 사용하며, 응답은 Location 헤더에 현재 리소스가 존재하고 있는 URL을 포함해야한다.                                                                                                                                                                                                                                                                                                               |
| 302       | Found              | 301 상태 코드와 같다. 그러나 클라이언트는 Location 헤더로 주어진 URL을 리소스를 임시로 가리키기 위한 목적으로 사용해야 한다. 이후의 요청에서는 원래 URL을 사용해야 한다.                                                                                                                                                                                                                                                |
| 303       | See Other          | 클라이언트에게 리소스를 다른 URL에서 가져와야 한다고 말해주고자 할 때 쓰인다. 새 응답은 응답 메시지의 Location 헤더에 들어있다. 이 상태 코드의 주 목적은 POST 요청에 대한 응답으로 클라이언트에게 리소스의 위치를 알려주는 것이다.                                                                                                                                                                                      |
| 304       | Not Modified       | 클라이언트는 헤더를 이용해 조건부 요청을 만들 수 있다. 이 코드는 리소스가 수정되지 않았음을 의미한다.                                                                                                                                                                                                                                                                                                                   |
| 305       | Use Proxy          | 리소스가 반드시 프락시를 통해서 접근되어야 함을 나타내기 위해 사용한다. 프락시의 위치는 Location을 통해 주어진다. 클라이언트는 이 응답을 특정 리소스에 대한 것이라고만 해석한다. 모든 요청에 대해 이 프락시를 통해 한다고 상정하지 않으며, 그 리소스를 가지고 있는 서버에 대한 요청이라 할지라도 마찬가지다. 이점은 중요하며, 프락시가 요청에 잘못 간섭한다면 이는 오작동을 유발할 수 있고, 보안 문제를 일으킬 수 있다. |
| 306       | (사용되지 않음)    | 현재는 사용되지 않는다.                                                                                                                                                                                                                                                                                                                                                                                                 |
| 307       | Temporary Redirect | 301 상태 코드와 비슷하다. 그러나 클라이언트는 Location 헤더로 주어진 URL을 리소스를 임시로 가리키기 위한 목적으로 사용해야 한다. 이후의 요청에서 원래 URL을 사용해야 한다.                                                                                                                                                                                                                                              |

302, 303, 307 상태 코드가 중복되는 부분이 있다.

그러나 미묘한 차이가 있는데 이는 주로 HTTP/1.0 과 1.1 애플리케이션이 이 상태 코드를 다루는 방식의 차이점에 기인한다.

HTTP/1.0 클라이언트가 POST 요청을 보내고 302 리다이렉트 상태 코드가 담긴 응답을 받으면, 클라이언트는 Location 헤더에 들어있는 리다이렉트 URL을 GET 요청으로 따라갈(원래는 POST) 것이다.

HTTP/1.0 서버가 HTTP/1.0 클라이언트로부터 POST 요청을 받은 뒤 302 상태를 보내는 상황이라면, 서버는 클라이언트로부터 POST 요청을 받은 뒤 302 상태 코드를 보내는 상황이라면 GET 요청으로 리다이렉트되길 원한다

그러나 HTTP/1.1이 문제를 일으킨데, 명세에 따르면 303 상태 코드를 사용한다. 이 혼란을 막기 위해 1.1 명세는 일시적 리다이렉트를 위해 307 상태 코드를 사용하라고 한다.

결국 서버는 리다이렉트 응답에 들어갈 가장 적절한 리다이렉트 상태 코드를 선택하기 위해 HTTP 버전을 검사할 필요가 있다.

### 3.4.4 400-499: 클라이언트 에러 코드

많은 클라이언트 에러가 귀찮게 하지 않고 브라우저에 의해 처리된다.

| 상태 코드 | 사유 구절                       | 의미                                                                                                                                                                                 |
| --------- | ------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| 400       | Bad Request                     | 잘못된 요청을 보냈다고 말해준다.                                                                                                                                                     |
| 401       | Unauthorized                    | 리소스를 얻기 전에 클라이언트에게 스스로 인증하라고 요구하는 헤더와 함께 반환한다.                                                                                                   |
| 402       | Payment Required                | 현재는 쓰이지 않지만 미래에 사용될 가능성을 위해 존재                                                                                                                                |
| 403       | Forbidden                       | 요청이 서버에 의해 거부되었음을 알려주기 위해 사용한다.                                                                                                                              |
| 404       | Not Found                       | 서버가 요청한 URL을 찾을 수 없음을 알려주기 위함이다.                                                                                                                                |
| 405       | Method Not Allowed              | 요청한 URL에 대해 지원하지 않는 메서드로 요청받았을 때 사용한다.                                                                                                                     |
| 406       | Not Acceptable                  | 클라이언트는 자신이 어떤 종류의 엔터티를 받아들이고자 하는지에 대해 매개변수로 명시할 수 있는데 종종 서버는 왜 요청이 만족될 수 없는지 알려주는 헤더를 포함시킨다.                   |
| 407       | PRoxy Authentication Required   | 401 상태코드와 같으나 리소스에 대해 인증을 요구하는 프락시 서버를 위해 존재한다.                                                                                                     |
| 408       | Request Timeout                 | 클라이언트가 요청을 완수하기에 시간이 너무 많이 걸리는 경우 서버는 이 상태 코드로 요청을 끊을 수 있다.                                                                               |
| 409       | Conflict                        | 요청이 리소스에 대해 일으킬 수 있는 몇몇 충돌을 지칭하기 위해 사용한다.                                                                                                              |
| 410       | Gone                            | 404와 비슷하나, 서버가 한때 그 리소스를 가지고 있었다는 점이 다르다. 주로 웹 사이트를 유지보수하면서, 서버 관리자가 클라이언트에게 리소스가 제거된 경우 이를 알려주기 위해 사용한다. |
| 411       | Length Required                 | 서버가 요청 메시지에 Content-Length 헤더가 있을 것을 요구할 때 사용한다.                                                                                                             |
| 412       | Precondition Failed             | 클라이언트가 조건부 요청을 했는데 그 중 하나가 실패했을 때 사용한다.                                                                                                                 |
| 413       | Request Entity Too Large        | 서버가 처리할 수 있는 혹은 처리하고자 하는 한계를 넘은 크기의 요청을 클라이언트가 보냈을 때 사용한다.                                                                                |
| 414       | Request URI TOO Long            | 한계를 초과한 길이의 요청 URL이 포함된 요청을 클라이언트에 보냈을 때 사용한다.                                                                                                       |
| 415       | Unsupported Media Type          | 서버가 이해하거나 지원하지 못하는 내용 유형의 엔터티를 클라이언트가 보넀을 때 사용한다.                                                                                              |
| 416       | Requested Range Not Satisfiable | 요청 메시지가 리소스의 특정 범위를 요청했는데 그 범위가 잘못되었거나 맞지 않을 때 사용한다.                                                                                          |
| 417       | Expectation Failed              | 요청에 포함된 Expect 요청 헤더에 서버가 만족시킬 수 없는 기대가 담겨있는 경우 사용한다.                                                                                              |

### 3.4.5 500-599: 서버에러 상태 코드

| 상태 코드 | 사유 구절                  | 의미                                                                                                                                          |
| --------- | -------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------- |
| 500       | Intenal Server Error       | 서버가 요청을 처리할 수 없게 만드는 에러를 만났을 때 사용한다.                                                                                |
| 501       | Not Implemented            | 클라이언트가 서버의 능력을 넘은 요청을 했을 때 사용. (서버가 지원하지 않는 메서드 사용)                                                       |
| 502       | Bad Gateway                | 프락시나 게이트웨이처럼 행동하는 서버가 그 요청 응답 연쇄에 있는 다음 링크로부터 가짜 응답에 맞딱뜨렸을 때 사용한다.                          |
| 503       | Service Unavailable        | 현재는 서버가 요청을 처리해줄 수 없지만 나중에는 가능함을 의미하고자 할 때 사용한다.                                                          |
| 504       | Gateway Timeout            | 상태 코드 408과 비슷하지만 다른 서버에게 요청을 보내고 응답을 기다리다 타임아웃이 발생한 게이트웨이나 프락시에서 온 응답이라는 점에서 다르다. |
| 505       | HTTP Version Not Supported | 서버가 지원할 수 없거나 지원하지 않으려고 하는 버전의 프로토콜로 된 요청을 받았을 때 사용한다.                                                |

## 3.5 헤더

- **일반 헤더(General Headers)**: 서버/클라이언트 양쪽 다 사용한다. 이들은 클라이언트, 서버, 그리고 어딘가에 메시지를 보내는 다른 애플리케이션들을 위해 다양한 목적으로 사용된다.
- **요청 헤더(Request Headers)**: 요청 메시지들을 위한 헤더이다. 서버에게 클라이언트가 받고자 하는 데이터의 탕비이 무엇인지 같은 부가 정보를 제공한다.
- **응답 헤더(Response Headers)**: 응답 메시지는 클라이언트에게 정보를 제공하기 위한 자신만의 헤더를 갖고 있다.
- **엔터티 헤더(Entity Headers)**: 엔터티 헤더란 엔터티 본문에 관한 헤더를 의미한다.
- **확장 헤더(Extension Headers)**: 애플리케이션 개발자에 의해 확장된 헤더인데 HTTP 표준에는 추가되지 않음

### 3.5.1 일반 헤더

기본적인 정보를 제공

| 헤더                     | 설명                                                                        |
| ------------------------ | --------------------------------------------------------------------------- |
| Connection               | 클라이언트와 서버가 요청/응답 연결에 대한 옵션을 정할 수 있게 해준다.       |
| Date                     | 메시지가 언제 만들어졌는지에 대한 날짜와 시간을 제공한다.                   |
| MIME-Version             | 발송자가 사용한 MIME의 버전을 알려준다.                                     |
| Tralier chunked transfer | 인코딩으로 인코딩된 메시지의 끝 부분에 위치한 헤더들의 목록을 나열한다.     |
| Transfer-Encoding        | 수신자에게 안전한 전송을 위해 메시지에 어떤 인코딩이 적용되었는지 말해준다. |
| Upgrade                  | 발송자가 업그레이드하기 원하는 새 버전이나 프로토콜을 말해준다.             |
| Via                      | 이 메시지가 어떤 중개를 거쳐 왔는지 보여준다.                               |

#### 일반 캐시 헤더

HTTP/1.0은 매번 원 서버로부터 객체를 가져오는 대신 복사본으로 캐시할 수 있도록 해주는 최초의 헤더를 도입했다.

| 헤더          | 설명                                                               |
| ------------- | ------------------------------------------------------------------ |
| Cache-Control | 메시지와 함께 캐시 지시자를 전달하기 위해 사용한다.                |
| Pragma        | 메시지와 함께 지시자를 전달하는 또 다른 방법, 캐시에 국한되지 않음 |

### 3.5.2 요청 헤더

| 헤더       | 설명                                                             |
| ---------- | ---------------------------------------------------------------- |
| Client-IP  | 클라이언트의 실행된 컴퓨터의 IP를 제공한다.                      |
| From       | 클라이언트 사요자의 메일 주소를 제고                             |
| Host       | 요청이 대상이 되는 서버의 호스트명과 포트를 준다.                |
| Referer    | 현재의 요청 URI가 들어있었던 문서의 URL을 제공한다.              |
| UA-Color   | 클라이언트 기기 디스플레이의 색상 능력에 대한 정보 제공          |
| UA-CPU     | 클라이언트 CPU의 종류나 제조사를 알려준다.                       |
| UA-Disp    | 클라이언트의 디스플레이 능력에 대한 정보를 제공한다.             |
| UA-OS      | 클라이언트 기기에서 동작 중인 운영체제의 이름과 버전을 알려준다. |
| UA-Pixels  | 클라이언트 기기 디스 플레이에 대한 픽셀 정보를 제공한다.         |
| User-Agent | 요청을 보낸 애플리케이션의 이름을 서버에게 말해준다.             |

### 조건부 요청 헤더

| 헤더                | 설명                                                                              |
| ------------------- | --------------------------------------------------------------------------------- |
| Expect              | 클라이언트가 요청에 필요한 서버의 행동을 열거할 수 있게 해준다.                   |
| If-Match            | 문서의 엔터티 태그가 주어진 엔터티 태그와 일치하는 경우에만 문서를 가져온다.      |
| If-Modified-Since   | 주어진 날짜 이후에 리소스가 변경되지 않았다면 요청을 제한한다.                    |
| If-Unmodified-Since | 주어진 날짜 이후에 리소스가 변경되었다면 요청을 제한한다.                         |
| If-None-Match       | 문서의 엔터티 태그가 주어진 엔터티 태그와 일치하지 않는 경우에만 문서를 가져온다. |
| If-Range            | 문서의 특정 범위에 대한 요청을 할 수 있게 해준다.                                 |
| Range               | 서버가 범위 요청을 지원한다면, 리소스에 대한 특정 범위를 요청한다.                |

### 요청 보안 헤더

| 헤더          | 설명                                                                                        |
| ------------- | ------------------------------------------------------------------------------------------- |
| Authorization | 클라이언트가 서버에 제공하는 인증 그 자체에 대한 정보를 담고 있다.                          |
| Cookie        | 클라이언트가 서버에게 토큰을 전달할 때 사용한다. 보안헤더는 아닌데 보안에 영향을 줄 수 있음 |
| Cookie2       | 요청자가 지원하는 쿠키의 버전을 알려줄 때 사용한다.                                         |

### 프락시 요청 헤더

| 헤더                | 설명                                                                                 |
| ------------------- | ------------------------------------------------------------------------------------ |
| Max-Forward         | 요청이 원 서버로 향하는 과정에서 다른 프락시나 게이트웨이로 전달될 수 있는 최대 횟수 |
| Proxy-Authorization | Authorization과 같으나 프락시에서 인증을 할때 쓰인다.                                |
| Proxy-Connection    | Connection과 같으나 프락시에서 연결을 맺을 때 쓰인다.                                |

### 3.5.3 응답 헤더

| 헤더        | 설명                                                                    |
| ----------- | ----------------------------------------------------------------------- |
| Age         | 응답이 얼마나 오래되었는지 (중개자를 통해 왔음을 암시한다.)             |
| Public      | 서버가 특정 리소스에 대해 지원하는 요청 메서드의 목록                   |
| Retry-After | 현재 리소스가 사용 불가능한 상태일 때, 언제 가능해지는지 날짜 혹은 시각 |
| Server      | 서버 애플리케이션의 이름과 버전                                         |
| Title       | HTML 문서에서 주어진 것과 같은 제목                                     |
| Warning     | 사유 구절에 있는 것보다 더 자세한 경고 메시지                           |

#### 응답 보안 헤더

| 헤더               | 설명                                                                                                                                     |
| ------------------ | ---------------------------------------------------------------------------------------------------------------------------------------- |
| Proxy-Authenticate | 프락시에서 클라이언트로 보낸 인증요구의 목록                                                                                             |
| Set-Cookie         | 진짜 보안 헤더는 아니지만, 보안에 영향은 줄 수 있다. 서버가 클라이언트를 인증할 수 있도록 클라이언트 측에 토큰을 설정하기 위해 사용한다. |
| Set-Cookie2        | Set-Cookie와 비슷하게 Version 1의 쿠키                                                                                                   |
| WWWW_Authenticate  | 서버에서 클라이언트로 보낸 인증요구의 목록                                                                                               |

### 3.5.4 엔터티 헤더

HTTP 메시지의 엔티티에 대해 설명하는 헤더들은 많다.

| 헤더     | 설명                                                                                                                |
| -------- | ------------------------------------------------------------------------------------------------------------------- |
| Allow    | 이 엔터티에 대해 수행될 수 있는 요청 메서드들을 나열한다.                                                           |
| Location | 클라이언트에게 엔터티가 실제로 어디에 위치하고 있는지 말해준다. 수신자에게 리소스에 대한 위치를 알려줄 때 사용한다. |

##### 콘텐츠 헤더

| 헤더             | 설명                                                           |
| ---------------- | -------------------------------------------------------------- |
| Content-Base     | 본문에서 사용된 상대 URL을 계산하기 위핸 기저 URL              |
| Content-Encoding | 본문에 적용된 어떤 인코딩                                      |
| Content-Language | 본문을 이해하는데 가장 적절한 자연어                           |
| Content-Length   | 본문의 길이나 크기                                             |
| Content-Location | 리소스가 실제로 어디에 위치하는지                              |
| Content-MD5      | 본문의 MD5 체크섬                                              |
| Content-Range    | 전체 리소스에서 이 엔터티가 해당하는 범위를 바이트 단위로 표현 |
| Content-Type     | 이 본문이 어떤 종류의 객체인지                                 |
