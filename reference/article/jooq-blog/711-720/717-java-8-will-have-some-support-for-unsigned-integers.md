# Java 8이 부호 없는 정수에 대한 약간의 지원을 가질 것이다

> 원문: https://blog.jooq.org/java-8-will-have-some-support-for-unsigned-integers/

2012년 1월 21일 lukaseder 게시

처음에는 좋은 소식처럼 보였다. Oracle의 Joe Darcy가 Java가 마침내 부호 없는 정수에 대한 *약간의* 지원을 할 것이라고 발표했다:

http://blogs.oracle.com/darcy/entry/unsigned_api

하지만 이것은 API 수준에서만 추가될 것이다. 다음과 같은 기대되는 모든 기능을 포함한 언어 수준에서는 아니다:

- 원시 타입(Primitive types)
- 래퍼 타입(Wrapper types)
- 산술 연산(Arithmetics)
- 캐스팅 규칙(Casting rules)
- 박싱 / 언박싱(Boxing / Unboxing)

기대할 수 있었던 것에 비하면 매우 "가벼운" 구현이다... 그동안 래퍼 타입이 필요하다면, jOOU를 자유롭게 다운로드(하고 기여)하기 바란다:

https://code.google.com/p/joou/

jOOU에 대한 이전 블로그 게시물도 참고하라:

[Java에 없는 부호 없는 정수 타입](https://blog.jooq.org/javas-missing-unsigned-integer-types/)
