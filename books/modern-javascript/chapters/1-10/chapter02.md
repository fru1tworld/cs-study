# chapter02 - 자바스크립트란?

## 2.1 자바스크립트의 탄생

- 1995, 약 90%의 시장 점유율로 웹 브라우저 시장을 지배하고 있던 넷스케이프 커뮤니케이션즈는 웹페이지의 보조적인 기능을 수행하기 위해 브라우저에서 동작하는 경량 프로그래밍 언어를 도입하기로 결정
- 그래서 등장한게 브렌던 아이크가 개발한 자바스크립트
- 자바스크립트는 넷스케이프 내비게이터에 탑재되었고 모카로 명명, 그러다 라이브스크립트로 바뀌었다가 자바스크립트로 최종 명명되었다.
- 이후 JScript가 출시되어 자바스크립트는 위기를 맞는다.

## 2.2 자바스크립트의 표준화

- 1996년 8월 마이크로소프트는 자바스크립트의 파생 버전인 JScript를 인터넷 익스플로러 3.0에 탑재 문제는 JScript와 자바스크립트가 표준화되지 못하고 적당히 호환되었음
- 이로 인해 브라우저에 따라 웹 페이지가 정상적으로 동작하지 않는 **크로스 브라우징 이슈**가 발생
- 이후 ECMA 인터내셔널에 자바스크립트의 표준화를 요청하였다.
- ECMA-262라 불리는 표준화된 자바스크립트 초판 사양이 완성되었고 이후 상표권 문제로 ECMAScript로 명명되었다.
- 이후 ECMAScript이 공개되고, 10년 만인 2009년에 출사된 ECMAScript(ES5)는 HTML5와 함께 출현한 표준 사양이다.
- 2015년에 공개된 ECMAScript 6(ES6)는 let/const 키워드, 화살표 함수, 클래스, 모듈 등과 같이 범용 프로그래밍 언어로서 갖춰야 할 기능들을 대거 도입하는 큰 변화가 있었다.
- ES6 이후의 버전업은 비교적 작은 기능을 추가하는 수준으로 매년 공개할 것으로 예고

- ES6(2015)
  - let/const, 클래스, 화살표 함수, 템플릿 리터럴, 디스트럭처링 할당, 스프레드 문법, rest 파라미터, 심벌 프로미스, Map/Set, 이터러블, for..of, 제너레이터, Proxy, 모듈 import/export
- ES7(2016)
  - 지수(\*\*)연산자, Array.prototype.includes, Stringprototype.includes
- ES8(2017)
  - async/await, Object 정적 메서드(Object.values, Object.entries, Object.getOwnPropertyDescriptors)
- ES9(2018)
  - Object rest/spread 프로퍼티, Promise,prototype.finally, async generator, for await...of
- ES10(2019)
  - Object.fromEntries, Array.prototype.flat, Array.prototype.flatMap, optional catch binding
- ES11(2020)
  - String.prototype.matchAll, BigInt, globalThis, Promise,allSettled, null 병합 연산자, 옵셔널 체이닝 연산자, for .. in enumeration order

## 2.3 자바스크립트 성장의 역사
- 초창기 자바스크립트는 웹페이지의 보조적인 기능을 수행하기 위해 한정적인 용도로 사용
- 브라우저는 서버로부터 전달받은 HTML과 CSS를 단순히 렌더링하는 수준
### 2.3.1 Ajax
- 자바스크립트를 이용해 서버와 브라우저가 비동기 방식으로 데이터를 교환할 수 있는 통신 기능인 Ajax가 XMLHttpREquest라는 이름으로 등장 
- Ajax는 웹페이지에서 변경할 필요가 없는 부분은 다시 렌더링하지 않고 필요한 부분만 렌더링하는 방식이 가능해짐
### 2.3.2 jQuery
- jQuery의 등장으로 DOM을 더욱 쉽게 제어할 수 있게 되었고 크로스 브라우징 이슈도 어느정도 해결되었다.
### 2.3.3 V8 자바스크립트 엔진
- 구글의 V8 엔진으로 자바스크립트는 빠른 성능을 확보함 
- 이러한 발전으로 웹 서버에서 수행되던 로직들이 대거 클라이언트로 이동하였음
- 이는 FE 영역이 주목받는 계기로 작용 
### 2.3.4 Node.js
- Node.js는 V8으로 빌드된 자바스크립트 런타임 환경
- 다양한 플랫폼에서 사용될 수 있지만 SSR 애플리케이션 개발에 주로 사용된다.
- 자바스크립트 엔진을 기반으로 하므로 Node.js 환경에서 동작하는 애플리케이션은 자바스크립트를 사용해 개발한다.
- FE, BE 영역에서 사용할 수 있다는 동형성의 이점이 존재
- 비동기 I/O를 지원하며 단일 스레드 이벤트 루프 기반으로 동작하여 request 처리 성능이 좋다.
- 따라서 실시간으로 처리하기 위해 I/O가 빈번하게 발생하는 SPA에 적합하다. 
- 하지만 CPU 사용률이 높은 애플리케이션에는 권장하지 않는다. 
- 이제 자바스크립트는 크로스 플랫폼을 위한 가장 중요한 언어로 주목받는다. 
### 2.3.5 SPA 프레임워크 
- 모던 웹 애플리케이션은 데스크톱 애플리케이션과 비교해도 손색없는 성능과 사용자 경험을 제공하는 것이 필수가 되었고, 더불어 개발 규모와 복잡도도 상승
- CBD 방법론을 기반으로 하는 SPA가 대중화되면서 다양한 프레임워크가 등장 

## 2.4 자바스크립트와 ECMAScript
- ECMAScript는 자바스크립트 표준 사양인 ECMA-262를 말하며 다양한 핵심 문법을 규정한다.
- 자바스크립트는 프로그래밍 언어로서 뼈대를 이루는 ECMAScript를 포함하여 CSR WEB API를 포함한다. 

## 2.5 자바스크립트의 특징
- 웹 브라우저에서 동작하는 유일한 프로그래밍 언어
- 인터 프리터
- 대부분의 모던 자바스크립트 엔진은 인터프리터와 컴파일러의 장점을 결합해 단점을 해소
- 명령형, 함수형, 프로토기반 객체지향 프로그래밍을 지원하는 멀티 패러다임 프로그래밍 언어

## 2.6 ES6 브라우저 지원 현황 
- 인터넷 익스플로러를 제외한 대부분 모던 브라우저는 ES6를 지원하지만 100%는 아님
- 만약 구형 브라우저를 고려해야 하는 상황이라면 바벨과 같은 트랜스파일러를 사용해 다운 그레이드할 필요가 있다. 