# Java의 누락된 부호 없는 정수 타입

> 원문: https://blog.jooq.org/javas-missing-unsigned-integer-types/

이 주제는 이전에도 여러 차례 논의된 바 있다. Java에는 부호 없는(unsigned) byte/short/int/long 타입이 없다는 것이다. JLS(Java Language Specification) 설계자들이 이 타입들을 생략한 주된 이유는 다음과 같다:

1. 실질적으로 거의 유용하지 않다
2. 구현이 다소 더 어렵다
3. 이해하기가 다소 더 어렵다
4. 기존 타입들과 별도로 처리해야 하는 원시 타입이 더 늘어난다
5. ... 그리고 아마도, 더 많은 이유가 있을 것이다

그럼에도 불구하고, 이 타입들은 암호화, 이미지 처리, 바이너리 프로토콜, 바이너리 데이터와 관련된 모든 작업(그런데 왜 byte는 부호가 있는 것인가??)에서 때때로 유용하며, Sun/Oracle의 다음 티켓에 달린 불만의 목록은 길다:

http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4504839

jOOQ의 경우, 일부 데이터베이스가 부호 없는 숫자 타입을 지원하기 때문에(예: MySQL, Postgres), 부호 없는 숫자 타입이 유용할 수 있다. 그리고 이들을 Java로 매핑하는 것은 반드시 간단한 일이 아니다. 그래서 나는 좋은 해결책을 찾고 있었다. 가장 좋은 방법은 `java.lang.Number`를 확장하는 래퍼 클래스를 사용하는 것이었다. 그래서 나는 이러한 라이브러리를 찾기 위해 Stack Overflow에 질문을 올렸다:

https://stackoverflow.com/questions/8193031/is-there-a-java-library-for-unsigned-number-type-wrappers

놀랍게도, 일부 대형 라이브러리에 부분적인 구현이 있는 것을 제외하면, 아무도 이것을 만들지 않은 것 같았다. 그래서 나는 jOOU라는 새로운 OSS 프로젝트를 시작한다 - U는 Unsigned(부호 없음)를 의미한다. Java 부호 없는 숫자 래퍼를 위한 소규모 라이브러리를 확인해 보라:

https://code.google.com/p/joou/
