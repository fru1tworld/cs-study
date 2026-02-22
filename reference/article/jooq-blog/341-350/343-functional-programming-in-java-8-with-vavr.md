# Java 8에서 vavr을 사용한 함수형 프로그래밍

> 원문: https://blog.jooq.org/functional-programming-in-java-8-with-vavr/

이 글은 HSH Nordbank의 시니어 소프트웨어 엔지니어인 Daniel Dietrich가 작성한 게스트 포스트입니다. Dietrich는 금융 상품을 위한 가격 책정 프레임워크 개발을 전문으로 합니다. 그의 관심 분야는 프로그래밍 언어, 알고리즘, 데이터 구조입니다. 그는 "Play Framework Starter"를 저술했으며 Java 8을 위한 함수형 컴포넌트 라이브러리인 vavr을 만들었습니다.

---

## 람다 표현식과 동기

"Java가 람다를 갖게 된다는 소식을 들었을 때 정말 흥분되는 순간이었습니다. 추상화의 수단으로 함수를 사용하는 근본적인 아이디어는 80년 전 '람다 계산법'에서 유래했습니다. 이제 Java 개발자들은 함수를 사용하여 동작을 전달할 수 있게 되었습니다."

예제 1 - 컬렉션과 기본 람다:

```java
List<Integer> list = Arrays.asList(2, 3, 1);
Collections.sort(list, (i1, i2) -> i1 - i2);
```

## Stream API의 한계

람다 표현식은 코드의 장황함을 크게 줄여줍니다. Stream API는 람다와 컬렉션을 연결해줍니다. 하지만 병렬 Stream은 실제로 많이 사용되지 않으며, 컬렉션을 앞뒤로 변환하는 과정에서 마찰이 발생합니다. 라이브러리에서 정수를 정렬하는 여러 가지 접근 방식을 보여드리겠습니다:

Stream 기반 접근 방식들:

```java
Arrays.asList(2, 3, 1)
  .stream()
  .sorted()
  .collect(Collectors.toList());

Stream.of(2, 3, 1)
  .sorted()
  .collect(Collectors.toList());

IntStream.of(2, 3, 1)
  .sorted()
  .collect(ArrayList::new, List::add, List::addAll);

IntStream.of(2, 3, 1)
  .sorted()
  .boxed()
  .collect(Collectors.toList());
```

## Vavr 솔루션

"와! 정수 리스트를 정렬하는 데 꽤 많은 변형이 있네요. 일반적으로 우리는 _어떻게_에 대해 고민하기보다 _무엇을_에 집중하고 싶습니다."

Vavr 접근 방식:

```java
List.of(2, 3, 1).sort();
```

---

## 명령형 vs 함수형 프로그래밍

### 명령형 접근 방식

Java를 포함한 대부분의 객체 지향 언어는 조건문과 반복문을 통한 명령형 프로그래밍을 강조합니다:

```java
String getContent(String location) throws IOException {
    try {
        final URL url = new URL(location);
        if (!"http".equals(url.getProtocol())) {
            throw new UnsupportedOperationException(
                "Protocol is not http");
        }
        final URLConnection con = url.openConnection();
        final InputStream in = con.getInputStream();
        return readAndClose(in);
    } catch(Exception x) {
        throw new IOException(
            "Error loading location " + location, x);
    }
}
```

### 함수형 접근 방식

함수형 언어는 문장보다 표현식을 우선시하며, 값의 변환을 강조합니다. 람다 표현식은 이러한 변환을 가능하게 합니다. Vavr의 `Try` 타입은 이 패러다임을 잘 보여줍니다:

```java
Try<String> getContent(String location) {
    return Try
        .of(() -> new URL(location))
        .filter(url -> "http".equals(url.getProtocol()))
        .flatMap(url -> Try.of(url::openConnection))
        .flatMap(con -> Try.of(con::getInputStream))
        .map(this::readAndClose);
}
```

결과는 내용을 담고 있는 `Success` 또는 예외를 담고 있는 `Failure`를 포함합니다. "일반적으로 이 표기법은 명령형 스타일에 비해 더 간결하며, 우리가 추론할 수 있는 견고한 프로그램으로 이어집니다."

---

## 결론

"이 짧은 소개가 vavr에 대한 여러분의 관심을 불러일으켰기를 바랍니다! Java 8과 vavr을 사용한 함수형 프로그래밍에 대해 더 알아보려면 사이트를 방문해 주세요."
