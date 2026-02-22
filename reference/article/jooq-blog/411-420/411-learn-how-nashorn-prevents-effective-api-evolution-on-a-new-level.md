# Nashorn이 새로운 차원에서 API 진화를 방해하는 방법을 알아보세요

> 원문: https://blog.jooq.org/learn-how-nashorn-prevents-effective-api-evolution-on-a-new-level/

게시일: 2014년 9월 19일
작성자: lukaseder

jOOQ를 Java 8 및 Nashorn과 함께 사용하는 방법에 대한 이전 글에 이어, 한 사용자가 치명적인 결함을 발견했습니다. 이 문제는 Nashorn이 오버로드된 메서드를 처리하는 방식이 전통적인 Java의 기대와 어떻게 다른지를 보여줍니다.

## 핵심 문제

Nashorn을 통해 JavaScript에서 오버로드된 메서드를 호출할 때, 런타임은 Java 컴파일러와 다른 해석 규칙을 사용합니다. 다음은 문제가 되는 코드입니다:

Java API:
```java
public class API {
    public static void test(String string) {
        throw new RuntimeException("Don't call this");
    }

    public static void test(Integer... args) {
        System.out.println("OK");
    }
}
```

JavaScript 호출:
```javascript
var API = Java.type("org.jooq.nashorn.test.API");
API.test(1); // RuntimeException으로 실패합니다
```

예상치 못한 동작이 발생하는 이유는 Nashorn이 가변 인자(varargs)와의 정확한 일치보다 타입 변환(toString, toNumber, toBoolean)을 우선시하기 때문입니다.

## 기술적 설명

Nashorn 개발자인 Attila Szegedi에 따르면: "Nashorn의 오버로드 메서드 해석은 Java Language Specification을 가능한 한 따르지만, JavaScript 특유의 변환도 허용합니다. JLS에 따르면 오버로드된 이름에 대해 호출할 메서드를 선택할 때, 가변 인자 메서드는 적용 가능한 고정 인자 메서드가 없을 때만 호출 대상으로 고려될 수 있습니다."

문제는 "적용 가능"이 어떻게 해석되느냐에 있습니다. JavaScript 타입 강제 변환이 개발자들이 직관적으로 가변 인자와의 "정확한" 일치라고 기대하는 것보다 우선시됩니다.

## 사용 가능한 해결 방법

옵션 1: 명시적으로 배열 생성
```javascript
var API = Java.type("org.jooq.nashorn.test.API");
API.test([1]);
```

옵션 2: 메서드 시그니처를 명시적으로 지정
```javascript
var API = Java.type("org.jooq.nashorn.test.API");
API["test(Integer[])"] (1);
```

옵션 3: 오버로드 제거
```java
public class AlternativeAPI1 {
    public static void test(Integer... args) {
        System.out.println("OK");
    }
}
```

옵션 4: 가변 인자 제거
```java
public class AlternativeAPI3 {
    public static void test(String string) {
        throw new RuntimeException("Don't call this");
    }

    public static void test(Integer args) {
        System.out.println("OK");
    }
}
```

옵션 5: 정확한 단일 매개변수 옵션 제공
```java
public class AlternativeAPI4 {
    public static void test(String string) {
        throw new RuntimeException("Don't call this");
    }

    public static void test(Integer args) {
        test(new Integer[] { args });
    }

    public static void test(Integer... args) {
        System.out.println("OK");
    }
}
```

옵션 6: String을 CharSequence로 대체
```java
public class AlternativeAPI5 {
    public static void test(CharSequence string) {
        throw new RuntimeException("Don't call this");
    }

    public static void test(Integer args) {
        System.out.println("OK");
    }
}
```

## API 진화에 대한 영향

Szegedi가 언급했듯이: "우리가 무엇을 하든, 다른 무언가가 피해를 입습니다. 오버로드된 메서드 선택은 Java와 JS 타입 시스템 사이의 좁은 틈에 있으며, 로직의 작은 변경에도 매우 민감합니다."

이것은 API 설계자들에게 전례 없는 도전을 만들어냅니다. 전통적인 시맨틱 버저닝과 바이너리 호환성 개념은 Nashorn 컨텍스트에서 깔끔하게 적용되지 않습니다. 새로운 오버로드를 도입하면 경고 없이 예기치 않게 기존 JavaScript 클라이언트가 깨질 수 있습니다.

## 더 넓은 맥락

이 글은 답답하기는 하지만, 이것이 Java와 JavaScript 사이의 근본적인 차이를 반영한다는 점을 강조합니다. 전통적인 ORM 도구들도 객체와 관계형 데이터베이스 사이에서 유사한 임피던스 불일치에 직면합니다.

저자인 Lukas Eder는 최선의 해결책은 JavaScript 호출자에게 더 직관적으로 느껴지도록 더 지능적으로 설계된 오버로드를 추가하는 것이라고 결론짓습니다.

---

## 댓글 섹션

Chase는 비판적인 댓글을 남기며, 기사의 제목이 오해의 소지가 있고 JavaScript에 본래 함수 오버로딩이 없는 것에 대해 Nashorn을 비난해서는 안 된다고 제안했습니다.

Lukaseder는 외교적으로 응답하며, 이 글을 비판이 아닌 경고로 특징지었습니다.

Attila Szegedi는 기술적인 설명을 제공하며, 동적 언어에서 전통적인 바이너리 호환성이 근본적으로 불가능하다는 점을 설명하고, 대괄호 표기법을 통한 명시적 오버로드 선택이 JavaScript 컨텍스트에서 적절한 해결 방법임을 강조했습니다.
