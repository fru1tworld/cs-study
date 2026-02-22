# PL/SQL과 Formspider로 놀라운 웹 애플리케이션

> 원문: https://blog.jooq.org/amazing-web-applications-with-plsql-and-formspider/

옛날 옛적에, 동적 웹 애플리케이션은 cgi-bin과 C를 사용하여 만들어졌습니다. 그렇다면 왜 PL/SQL로 웹사이트를 만들면 안 될까요?

Formspider는 AJAX 요청을 PL/SQL 저장 프로시저 호출에 직접 연결하는 웹 프레임워크입니다. 예를 들어, 다음은 그들의 차트 API입니다:

(Formspider 차트 API 데모 비디오)

흥미롭지 않나요? 저도 그렇습니다! ;-)

---

## 댓글 토론

Samuel Rutishauser:
"공포다, 공포..."

Yalim K. Gerger (Formspider 창시자):

Formspider는 미들웨어를 사용하며 3계층 MVC 아키텍처를 따릅니다. PL/SQL은 활발하게 개발되고 있는 가장 활기찬 언어 중 하나이며, Oracle 고객사와 파트너들 사이에서 엔터프라이즈 애플리케이션을 구동하고 있습니다.

Formspider로 만든 애플리케이션의 소스 코드에는 JavaScript가 단 한 줄도 없습니다. Formspider IDE 자체도 Formspider를 사용하여 구축되었으며, 이는 프레임워크의 역량을 보여줍니다.

PL/SQL이 레거시 기술로 인식될 수 있지만, Formspider는 현대적인 개발 요구 사항을 충족하는 고품질 앱 개발 도구입니다. 이 프레임워크는 어떤 기술 스택에도 적용 가능한 원칙들을 구현하고 있습니다.
