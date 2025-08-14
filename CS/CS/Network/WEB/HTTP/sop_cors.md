# SOP

Single Origin Policy

동일한 Origin에 대해서만 `리소스 접근 허용`

- 리소스 접근 허용의 엄밀한 의미: 리소스 요청 및 응답 = 서버 req, res = Javascript의 XMLHttpRequest, fetch, AJAX 등

### Origin

Protocol, Domain , Port로 구성됨

- 1개라도 다르면 다른 Origin임

# CORS

- SOP 정책에 의해서 다른 Origin에 대해서 접근을 할 수 없는 이슈
- CORS 설정을 수정해서 접근할 수 있게 설정할 수 있다.
- 이때 설정할 수 있는 것: Origin의 구성 요소들 Protocol, Domain, Port 등
