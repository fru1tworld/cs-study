# chapter03 - 자바스크립트 개발 환경과 실행 방법

## 3.1 자바스크립트 실행 환경

- 모든 브라우저또는 Node.js는 자바스크립트를 해석하고 실행할 수 있는 자바스크립트 엔진을 내장하고 있다.
- 주의해야하는 부분은 Node.js와 브라우저는 용도가 다르다.
- 예를 들어 DOM API 지원 여부가 다르다. 브라우저는 지원하며, Node.js는 지원하지 않음
- 웹 크롤링의 경우 서버 환경은 DOM API를 지원하지 않으므로 별도의 DOM 라이브러리를 사용해 HTML 문서를 가공하기도 한다.
- 반대로 Node.js에서는 파일을 생성하고 수정할 수 있는 파일 시스템을 제공하지만 브라우저는 이를 제공하지 않는다. (Web API인 FileReader 객체를 사용할 수 있긴함)
- 브라우저는 클라이언트 사이드 Web API
- Node.js는 Host APIs를 지원한다.

## 3.2 웹 브라우저

- 이 책은 크롬 기준으로 작성됨

### 3.2.1 개발자 도구

Elements: 로딩된 웹페이지의 DOM과 CSS를 편집해서 렌더링된 뷰를 확인해 볼 수 있다.
Console: 로딩된 웹페이지의 에러를 확인하거나 자바스크립트 소스코드에 작성한 consol.log 메서드의 실행 결과를 확인
Sources: 로딩된 웹페이지의 자바스크립트 코드를 디버깅
Network: 로딩된 웹페이지에 관련된 request 정보와 성능 확인
Application :웹 스토리지, 세션, 쿠키를 확인 및 관리 가능

### 3.2.2 콘솔

- 개발자 도구의 Console 패널은 디버깅 용도로 매우 유용함
- REPL(Read Eval Print Loop: 입력 수행 출력 반복) 환경으로 사용할 수도 있다.
- 콘솔과 디버깅 관련한 자세한 내용은 구글의 Tools for Web Developers, Tools for Web Developers: Chrome DevTools을 참고하자 [링크](https://developer.chrome.com/docs/devtools?hl=ko)

## 3.3 Node.js
### 3.3.1 Node.js와 npm
- Node.js는 V8 자바스크립트 엔진으로 빌드된 런타임 환경 
- npm은 자바스크립트 패키지 매니저다. Node.js에서 사용할 수 있는 모듈들을 패키지화해서 모아둔 저장소 역할과 패키지 설치 및 관리를 위한 CLI를 제공 

