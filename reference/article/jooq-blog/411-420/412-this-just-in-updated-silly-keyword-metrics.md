# 속보!! 업데이트된 재미있는 키워드 메트릭

> 원문: https://blog.jooq.org/this-just-in-updated-silly-keyword-metrics/

작년에 이어 jOOQ 코드베이스의 Java 키워드 사용 빈도를 분석한 연간 리포트를 다시 발표합니다.

## 2013년 vs 2014년 키워드 비교

| 키워드 | 2013 | 2014 |
|---------|------|------|
| public | 8127 | 9379 |
| return | 6801 | 8079 |
| final | 6608 | 7561 |
| import | 5938 | 7232 |
| static | 3903 | 5154 |
| new | 3110 | 3915 |
| extends | 2111 | 2884 |
| int | 1822 | 2132 |
| throws | 1756 | 1898 |
| void | 1707 | 1834 |
| if | 1661 | 1985 |
| this | 1464 | 1803 |
| private | 1347 | 1605 |
| class | 1239 | 1437 |
| case | 841 | 1225 |
| else | 839 | 940 |
| package | 711 | 842 |
| boolean | 506 | 623 |
| throw | 495 | 553 |
| for | 421 | 469 |
| long | 404 | 456 |
| true | 384 | 439 |
| byte | 345 | 397 |
| interface | 337 | 407 |
| false | 332 | 396 |
| protected | 293 | 328 |
| super | 265 | 328 |
| break | 200 | 357 |
| switch | 146 | 197 |
| try | 149 | 193 |
| implements | 139 | 162 |
| catch | 127 | 167 |
| default | 112 | 156 |
| instanceof | 107 | 156 |
| char | 96 | 122 |
| short | 91 | 93 |
| abstract | 54 | 50 |
| double | 43 | 44 |
| transient | 42 | 45 |
| finally | 34 | 54 |
| float | 34 | 35 |
| enum | 25 | 31 |
| while | 23 | 35 |
| continue | 12 | 13 |
| synchronized | 8 | 10 |
| volatile | 6 | 5 |
| do | 1 | 6 |

## 주요 관찰 사항

- `public`은 여전히 가장 즐겨 사용하는 키워드입니다(맞아요, 우리는 API니까요). 하지만 `return`이 바짝 뒤쫓고 있고, `final`도 마찬가지입니다.
- `if`가 `throws`와 `void`를 추월했습니다. jOOQ가 점점 덜 객체지향적이고, 더 명령형이 되어가는 걸까요?
- `true`는 여전히 `false`보다 더 많이 사용됩니다. 네, 우리는 삶에 대해 긍정적으로 생각하고 있습니다.
- `do`는 600% 증가를 경험했습니다!
- 우리는 여전히 `catch`보다 `try`를 더 많이 합니다.
- `char`를 추가했습니다. 더 많은 SQL 파싱을 하고 있다는 뜻일까요?
- `volatile`을 하나 제거했습니다.
- 여전히 `strictfp`나 `native`는 사용하지 않습니다.

## silly-metrics 도구

이 분석은 "silly-metrics"라는 도구를 사용하여 수행되었습니다. 이 도구는 무료이며 ASL 2.0 라이선스로 GitHub에서 제공됩니다. 여러분의 코드베이스에서도 키워드 분포를 분석해 보세요!
