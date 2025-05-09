여기에서는 HTTP 프로토콜을 소개한다.

다음 4개의 장에서 웹의 기초가 되는 HTTP 핵심 기술을 설명한다.

- 1장 HTTP 개관에서는 HTTP를 빠르게 훑어본다.
- 2장 URL과 리소스에서는 URL 포맷과 인터넷에 있는 URL들이 가리키는 다양한 리소스 형식에 대해 자세히 살펴본다. URN으로 발전하는 과정에 대해서 개략적으로 알아본다.
- 3장 HTTP 메시지에서는 웹 콘텐츠를 실어나르는 HTTP 메시지에 대해 자세히 알아본다.
- 4장 커넥션 관리에서는 HTTP에서 관리하는 TCP 커넥션에 대한 일반적인 오해들과 잘못 작성된 규칙 및 동작 방식에 대해서 알아본다.

# 1장 HTTP 개관

## 1.1 HTTP : 인터넷의 멀티미디어 배달부

HTTP는 신뢰성 있는 데이터 전송 프로토콜을 사용하기 때문에 데이터가 지구 반대편에서 오더라도 전송 중 손상되거나 꼬이지 않음을 보장

## 1.2 웹 클라이언트와 서버

웹 콘텐츠는 웹 서버에 존재한다. 웹 서버는 HTTP 프로토콜로 의사소통하기 때문에 보통 HTTP 서버로 불린다.

예를 들어 웹 브라우저는 HTTP 객체를 서버에 요청하고 사용자 화면에 보여준다.

## 1.3 리소스

웹 서버는 웹 리소스를 관리하고 제공한다.

웹 리소스는 텍스트, HTML 파일, JPEG, AVI 그 외 모든 종류의 파일을 포함한 정적 파일이다.

그러나 리소스는 반드시 정적 파일일 필요 없이 요청에 따라 콘텐츠를 생산하는 프로그램이 될 수 있다.

이러한 동적 콘텐츠 리소스는 사용자가 누구인지, 어떤 정보를 요청했는지, 몇 시인지에 따라 다른 콘텐츠를 생성한다.

### 1.3.1 미디어 타입

인터넷은 수천 가지 데이터 타입을 다루기 때문에 HTTP는 웹에서 전송되는 객체 각각에 MIME 타입이라는 데이터 포맷 라벨을 붙인다.

MIME(Multipurpose Internet Mail Extensions, 다목적 인터넷 메일 확장)은 원래 각기 다른 전자메일 시스템 사이에 메시지가 오갈 때 겪는 문제점을 해결하기 위해 설계되었다. MIME는 원래 이메일에서 워낙 잘 동작했기 때문에 HTTP 에서도 멀티미디어 콘텐츠를 기술하고 라벨을 붙이기 위해 채택되었다.

웹 서버는 모든 HTTP 객체 데이터에 MIME 타입을 붙인다. 웹브라우저는 서버로부터 객체를 돌려받을 때 다룰 수 있는 객체인지 MIME 타입을 통해 확인한다. 대부분의 웹 브라우저는 잘 알려진 객체 타입 수백 가지를 다룰 수 있다.

MIME 타입은 사선으로 구분된 주 타입(primary object)과 부타입(specific subtype)으로 이루어진 문자열 라벨이다.

- HTML로 작성된 텍스트 문서는 text/html
- plain ASCII 텍스트 문서는 text
- jpeg 이미지는 image/jpeg

### 1.3.2 URI

웹 서버 리소스는 각자 이름을 갖고 있기 때문에 클라이언트는 관심 있는 리소스를 지목할 수 있다.

서버 리소스 이름은 통합 자원 식별자 URI(Uniform resource Information) 로 불린다.

### 1.3.3 URL

통합 자원 지시자 ((Uniform resource locator)는 리소스 식별자의 가장 흔한 형태이다.

URL은 특정 서버의 한 리소스에 대한 구체적인 위치를 서술한다.

대부분의 URL은 세 부분으로 이루어진 표준 포맷을 따른다.

- 스키(scheme) : 리소스에 접근하기 위해 사용되는 첫 번째 프로토콜을 서술한다.
- 서버의 인터넷 주소를 제공한다.
- 웹 서버의 리소스를 가리킨다.

대부분의 URI는 URL 이다.

### 1.3.4 URN

URL의 두번 째 종류는 URN(Uniform resource name) 이다.

URN은 콘텐츠를 이루는 한 리소스에 대해 그 리소스의 위치에 영향 받지 않는 유일무이한 이름 역할을 한다.

이 위치 독립적인 URN은 리소스를 여기저기로 옮기더라도 문제없이 동작한다.

리소스가 그 이름이 변하지 않게 윶;하는 한, 여러 종류의 네트워크 접속 프로토콜로 접근해도 문제없다.

URN은 여전히 실험 중인 상태 널리 채택되지 않았다. 효율적인 동작을 위해 URN은 리소스 위치를 분석하기 위한 인프라 지원이 필요한데, 그러한 인프라가 부재하기에 URN 채택이 더 늦춰지고 있다.

특별한 언급이 없으면 통상적인 관례에 따라 URL과 URI을 같은 의미로 사용할 것이다.

## 1.4 트랜잭션

클라이언트가 웹 서버와 리소스를 주고받기 위해 HTTP를 어떻게 사용하는지 좀 더 자세히 알아보자

HTTP 트랜잭션은 요청 명령(클라이언트에서 보내는)과 응답결과로 구성되어 있다.

이 상호작용은 HTTP 메서드라고 불리는 정형화된 데이터 덩어리를 이용해 이루어진다.

### 1.4.1 메서드

HTTP는 HTTP 메서드라고 불리는 여러 가지 종류의 요청 명령을 지원한다.

모든 HTTP 요청 메시지는 한 개의 메서드를 갖는다.

흔히 쓰이는 메시지 5가지

- GET : 서버에서 클라이언트로 지정한 리소스를 보내라
- PUT : 클라이언트에서 서버로 보낸 데이터를 지정한 이름의 리소스로 저장하라,.
- DELETE : 지정한 리소스를 서버에서 삭제하라
- POST : 클라이언트 데이터를 서버 게이트웨이 애플리케이션으로 보내라
- HEAD 지정한 리소스에 대한 응답에, HTTP 헤더 부분만 보내라

### 1.4.2 상태코드

모든 HTTP 응답 메시지는 상태 코드와 함께 반환된다. 상태 코드는 클라이언트에게 요청이 성공했는지 아니면 추가 조치가 필요한지 알려주는 세 자리 숫자다.

흔히 쓰이는 상태 코드와 몇 가지에 대해 알아보자

- 200 문서가 바르게 반환되었다.
- 302 다른 곳에 가서 리소스를 가져가라
- 404 없음 리소스를 찾을 수 없다

HTTP는 각 숫자 상태 코드에 텍스트로 된 사유 구절도 함께 보낸다.

실제 응답처리에는 숫자로 된 코드가 사용된다.

### 1.4.3 웹페이지는 여러 객체로 이루어질 수 있다

애플리케이션은 보통 하나의 작업을 수행하기 위해 HTTP 트랜잭션을 수행한다.

웹브라우저는 시각적으로 풍부한 웹페이지를 가져올 때 대량의 HTTP 트랜잭션을 수행한다.

페이지 레이아웃을 서술하는 HTML를 한번 가져온 뒤 첨부된 이미지, 그래픽 조각, 자바 애플릿 등을 가져오기 위해 추가로 HTTP 트랜잭션을 수행한다.

## 1.5 메시지

이제 HTTP 요청과 응답 메시지의 구조를 살짝 들여다보자

HTTP 메시지는 다음의 세 부분으로 구성되어있다.

### 시작줄

요청이라면 무엇을 해야 하는지 응답이라면 무슨 일이 일어났는지 나타낸다.

### 헤더

시작줄 다음에는 0개 이상의 헤더 필드가 이어진다. 각 헤더 필드는 쉬운 구문분석을 위해 쌍점으로 구분되어 있는 하나의 이름과 하나의 값으로 구성된다. 헤더 필드를 추가혀려면 그저 한 줄을 더하기만 하면 된다. 헤더는 빈 줄로 끝난다.

### 본문

빈 줄 다음에는 어떤 종류의 데이터든 들어갈 수 있는 메시지 본문이 필요에 따라 올 수 있다.

## 1.6 TCP 커넥션

어떻게 메시지가 TCP 커넥션을 통해 한 곳에서 다른 곳으로 옮겨가는지 잠깐 이야기 해보도록 하자

### 1.6.1 TCP/IP

HTTP 는 애플리케이션 계층 프로토콜이다. HTTP는 네트워크 통신의 핵심적인 세부사항에 대해서 신경 쓰지 않는다.

대신 TCP/IP 에게 맡긴다.

TCP는 다음을 제공한다

- 오류없는 데이터 전송
- 순서에 맞는 전달
- 조각나지 않은 데이터 스트림

인터넷 자체가 전 세계의 컴퓨터와 네트워크 장치들 사이에서 대중적으로 사용되는 TCP/IP 를 기초하고 있다.

TCP/IP 는 패킷 교환 네트워크 프로토콜의 집합이다. 각 네트워크와 하드웨어의 특성을 숨기고 어떤 종류의 컴퓨터나 네트워크든 신뢰있는 의사소통을 하게 해준다.

TCP 커넥션이 맺어지면 클라이언트와 서버 컴퓨터 간에 교환되는 메시지가 손상되거나 순서가 바뀌는 일은 결코 없다.

네트워크 개념상 HTTP 프로토콜은 TCP 위의 계층이다.

### 1.6.2 접속 IP 주소 그리고 포트번호

HTTP 클라이언트가 서버에 메시지를 전송할 수 있게 되기 전에 인터넷 프로토콜 주소와 포트번호를 사용해 클라이언트와 서버 사이에 TCP/IP 커넥션을 맺어야한다.

### 1.8 웹의 구성요소

이 장에서는 우리는 웹 애플리에키션이 기본적인 트랜잭션을 구현하기 위해 어떻게 메시지를 주고받는지에 중점을 두었다.

- 프록시 :클라이언트와 서버 사이에 위치한 HTTP 중개자
- 캐시 : 많이 찾는 웹페이지를 클라이언트 가까이에 보관하는 HTTP 창고
- 게이트웨이 : 다른 애플리케이션과 연결된 특별한 웹서버
- 터널 : 단순히 HTTP 통신을 전달하기만 하는 특별한 프락시
- 에이전트 : 자동화된 HTTP 요청을 만드는 준지능적 웹클라이언트

### 1.8.1 프록시

웹 보안, 애플리케이션 통합, 성능 최적화를 위한 중요한 구성요소인 HTTP 프록시 서버에 대해 살펴보자

프록시는 클라이언트와 서버 사이에 위치하여 클라이언트의 모든 HTTP 요청을 받아 서버에 전달한다.

이 애플리케이션은 사용자를 위한 프락시로 동작하며 사용자를 대신해서 서버에 접근한다.

모든 웹 트래픽 흐름 속에서 신뢰할만한 중개자 역할을 한다.

또한 프락시는 요청과 응답을 필터링한다.

예를들어 회사에서 무엇인가를 다운 받을 때 애플리케이션 바이러스를 검출하거나 초등학교 학생들에게 성인 콘텐츠를 차단한다.

### 1.8.2 캐시

웹 캐시와 캐시 프락시는 자신을 거쳐 가는 문서들 중 자주 찾는 것의 사본을 저장해두는 특별한 종류의 HTTP 프락시 서버다. 다음번에 클라이언트가 같은 문서를 요청하면 그 캐시가 갖고 있는 사본을 받을 수 있다.

HTTP는 캐시를 효율적으로 동작하게 하고 캐시된 콘텐츠를 최신 버전으로 유지하면서 동시에 프라이버시도 보호하기 위한 많은 기능을 정의한다.

### 1.8.3 게이트웨이

게이트웨이는 다른 서버들의 중개자로 동작하는 특별한 서버다.

게이트웨이는 주로 HTTP 트래픽을 다른 프로토콜로 변환하기 위해 사용된다.

언제나 스스로가 리소스를 갖고 있는 진짜 서버인 것처럼 요청을 다룬다.

클라이언트는 자신이 게이트웨이와 통신하고 있음을 알아채지 못할 것이다.

### 1.8.4 터널

터널은 두 커넥션 사이에 날(raw) 데이터를 열어보지 않고 그대로 전달해주는 HTTP 애플리케이션이다.

HTTP 터널은 주로 비 HTTP 데이터를 하나 이상의 HTTP 연결을 통해 그대로 전송해주기 위해 사용된다.

### 1.8.5 에이전트

사용자 에이전트는 사용자를 위해 HTTP 요청을 만들어주는 클라이언트 프로그램이다.

웹 요청을 만드는 애플리케이션은 뭐든 HTTP 에이전트다.

다양한 종류가 있다.
