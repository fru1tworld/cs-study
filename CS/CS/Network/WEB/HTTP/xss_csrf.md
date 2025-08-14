# XSS(Cross-site scripting)

- 공격자가 스크립트를 실행해서 공격하는 방식

- 일반적으로 쿠키를 활용하면 쿠키에 HttpOnly, Secure 옵션을 통해서 방어할 수 있음

# CSRF(Cross-Site Request Forgery)

- 사용자가 세션을 가로채서 서버에 요청을 보내는 방식
- SameSite와 같은 방식을 통해서 해결할 수 있음
- 이때 Site는 Origin과 구분되는 개념
- 요청의 Origin이나 Referer 헤더를 확인해서 출처를 검증
- 출처 = Origin
