# Developer Guide (개발자 가이드)

> 원본: https://netty.io/wiki/developer-guide.html

---

> **튜토리얼을 찾고 계신가요?** [문서 홈](https://netty.io/wiki/index.html)을 방문하세요. **질문이 있으신가요?** [StackOverflow.com](https://stackoverflow.com/questions/tagged/netty)에 질문하세요.
>
> 이 가이드는 'user guide'가 **아닙니다**. Netty를 사용해 애플리케이션을 만드는 '사용자'가 아니라, Netty 자체를 개발하려는 'dev'(기여자)를 위한 문서입니다.

### 시작하기 전에

* [개발 환경을 설정하세요.](./05_setting_up_dev.md)
* 단일 라인 변경이나 오타 수정 같은 사소한 기여가 아니라면, [Individual Contributor License Agreement (icla)](http://netty.io/s/icla)를 읽고 서명하거나, 고용주가 [Corporate Contributor License Agreement (ccla)](http://netty.io/s/ccla)에 서명해야 합니다.

### 체크리스트

커밋을 푸시하거나 풀 리퀘스트를 보내기 전에 다음 체크리스트를 활용하세요.

* 셸에서 `mvn test`를 실행했을 때 빌드가 실패 없이 성공하는가?
* 작업으로 인해 새로운 inspector 경고가 생기지 않는가?
* 커밋 메시지나 PR 설명이 [커밋 메시지 규칙](https://netty.io/wiki/writing-a-commit-message.html)에 부합하는가?
* PR을 대상 브랜치의 HEAD에 [rebase](http://git-scm.com/book/en/Git-Branching-Rebasing) 하고 모든 충돌을 해결했는가?
* [Contributor License Agreement](https://docs.google.com/spreadsheet/viewform?formkey=dHBjc1YzdWhsZERUQnhlSklsbG1KT1E6MQ)에 서명했는가?

### 푸시 권한이 있는 기여자에게

* PR에는 여러 커밋이 포함되는 경우가 많습니다. 해당 커밋들은 설명 주석과 함께 적은 수의 커밋으로 squash해야 합니다.
* 특별한 이유가 없는 한 웹 UI를 통해 PR을 머지하지 마세요. [GitHub help 페이지](https://help.github.com/articles/using-pull-requests#merging-a-pull-request)의 'Patch and Apply' 섹션을 참고하세요.
* [공식 웹사이트를 갱신](https://github.com/netty/netty/wiki/Working-with-official-web-site)하거나 [새 버전을 릴리스](https://netty.io/wiki/releasing-new-version.html)할 때는 신중하세요.
