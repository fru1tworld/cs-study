# J2eeBasedPreAuthenticatedWebAuthenticationDetailsSource... 뭐라고??

> 원문: https://blog.jooq.org/j2eebasedpreauthenticatedwebauthenticationdetailssource-wat/

Gary Bernhardt의 유명한 "WAT" 강연을 기억하시나요?

<iframe width="560" height="315" src="https://www.youtube.com/embed/20BySC_6HyY" frameborder="0" allowfullscreen></iframe>

JavaScript와 Ruby의 어이없는 기능들에 대해 다룬 그 강연 말입니다.

## Spring Security는 정말 "WAT??" 수준입니다

Spring Security를 살펴보면서 이 동영상이 떠올랐습니다. 다음과 같은 클래스를 만났기 때문입니다:

```
J2eeBasedPreAuthenticatedWebAuthenticationDetailsSource
```

뭐라고??

이게 끝이 아닙니다. `eraseCredentialsAfterAuthentication`이라는 기능도 있습니다. 개발자들이 실수로 자격 증명을 노출하지 않도록 돕기 위한 기능이라고 합니다.

뭐라고??

## 중복의 향연

패키지 이름까지 포함하면 더 심각해집니다. 전체 경로를 보면:

- "web" 2번
- "authentication" 4번
- "websphere" 2번

이런 식으로 같은 개념이 패키지와 클래스 이름에 걸쳐 반복됩니다.

## 다른 문제가 되는 클래스 이름들

Spring Security에서 발견한 다른 클래스 이름들도 있습니다:

```
PreAuthenticatedGrantedAuthoritiesWebAuthenticationDetails
PreAuthenticatedGrantedAuthoritiesAuthenticationDetails
GrantedAuthorityFromAssertionAttributesUserDetailsService
MutableGrantedAuthoritiesContainer
MethodSecurityMetadataSourceBeanDefinitionParser
AbstractUserDetailsServiceBeanDefinitionParser
```

## 더 넓은 맥락

이것은 PHP의 잘못된 설계 철학을 떠올리게 합니다. 프레임워크가 개발자들이 자격 증명 노출 같은 기본적인 실수를 피하도록 돕는 기능을 제공해야 한다면, 이는 사려 깊은 API 설계보다는 근본적인 아키텍처 문제가 있다는 것을 의미합니다.

이런 클래스 이름들은 API 문서를 탐색하기 어렵게 만들고, 프레임워크의 복잡성을 보여주는 증거입니다.

## 재미있는 논평

한 댓글 작성자는 Spring Security 클래스 이름들이 마르코프 체인으로 생성될 수 있으며, 실제 구현체와 구별할 수 없을 것이라고 유머러스하게 제안했습니다.

이런 과도하게 긴 이름들은 프레임워크가 얼마나 복잡해졌는지, 그리고 더 나은 추상화가 필요할 수 있다는 것을 보여줍니다.
