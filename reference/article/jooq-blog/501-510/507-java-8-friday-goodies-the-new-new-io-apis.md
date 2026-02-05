# Java 8 금요일 선물: 새로운 새 I/O API

> 원문: https://blog.jooq.org/java-8-friday-goodies-the-new-new-io-apis/

Data Geekery에서 우리는 Java를 사랑합니다. 그리고 우리는 jOOQ의 유창한 API와 쿼리 DSL이 Java에서 가장 잘 작동하기 때문에 정말로 Java 8에 대해 열광하고 있습니다. 우리는 Java 8의 멋진 기능들에 대해 몇 차례 블로그 글을 작성해 왔으며, 이제 새로운 블로그 시리즈를 시작할 때가 되었습니다.

## Java 8 금요일

매주 금요일마다, 우리는 람다 표현식, 확장 메서드, 그리고 다른 멋진 것들을 활용하는 튜토리얼 스타일의 새로운 Java 8 기능 몇 가지를 보여드립니다. 소스 코드는 GitHub에서 찾으실 수 있습니다.

## 새로운 새 I/O API

이전에 우리는 Java 8이 람다 표현식을 사용하여 오래된 JDK 1.2 I/O API를 어떻게 개선하는지에 대해 블로그 글을 작성했습니다. 하지만 java.io의 대부분은 Java 7의 java.nio API로 대체되었습니다(여기서 "N"은 "New I/O"를 의미합니다). Java 8은 이를 더욱 개선합니다. 저는 이것을 "새로운 새 I/O API" 또는 "엔터프라이즈 IO"라고 부르고 싶습니다.

## Files.list()

새롭게 도입된 `Files.list()` 메서드를 살펴봅시다. 이 메서드는 파일들의 지연(lazy) Stream을 반환합니다:

```java
Files.list(new File(".").toPath())
     .forEach(System.out::println);
```

출력 결과:

```
.\java8-goodies.iml
.\LICENSE.txt
.\pom.xml
.\README.txt
.\src
.\.git
.\.gitignore
.\.idea
```

보시다시피 숨김 파일들(`.git`, `.gitignore`, `.idea`)도 포함되어 있습니다. 이제 숨김 파일을 필터링하고 결과를 제한하는 방법을 살펴보겠습니다:

```java
Files.list(new File(".").toPath())
     .filter(p -> !p.getFileName()
                    .toString().startsWith("."))
     .limit(3)
     .forEach(System.out::println);
```

이 코드는 숨김 파일이 아닌 파일들만 출력합니다:

```
.\java8-goodies.iml
.\LICENSE.txt
.\pom.xml
```

## Files.walk()

디렉토리 계층 구조를 하위로 탐색하고 싶다면 새로운 `Files.walk()` 메서드를 사용할 수 있습니다:

```java
Files.walk(new File(".").toPath())
     .filter(p -> !p.getFileName()
                    .toString().startsWith("."))
     .forEach(System.out::println);
```

하지만 여기에는 한계가 있습니다. 필터는 스트림에서 숨김 파일을 제거하지만, 알고리즘이 해당 디렉토리로 내려가는 것을 막지는 못합니다. 결과적으로 숨김 디렉토리의 내용이 여전히 결과에 나타납니다.

Java 7의 `Files.walkFileTree()` 메서드가 이론적으로 이 문제를 해결할 수 있지만, 그 `FileVisitor` 매개변수는 `@FunctionalInterface`가 아니므로 람다 표현식을 사용할 수 없습니다.

### 우회 해결책

비효율적이지만 문자열 경로 검사를 사용하는 우회 방법이 있습니다:

```java
Files.walk(new File(".").toPath())
     .filter(p -> !p.toString()
                    .contains(File.separator + "."))
     .forEach(System.out::println);
```

## Files.lines()

파일을 줄 단위로 읽으려면 새로운 `Files.lines()` 메서드를 사용할 수 있습니다:

```java
Files.lines(new File("pom.xml").toPath())
     .map(s -> s.trim())
     .filter(s -> !s.isEmpty())
     .forEach(System.out::println);
```

이 예제는 각 줄을 trim하고, 빈 줄을 필터링한 다음, Maven pom.xml 파일의 XML 내용을 표시합니다.

## 결론

지연 평가(lazy evaluation)는 분명히 커뮤니티에서 혼란을 야기할 것입니다. 스트림은 단 한 번만 소비될 수 있다는 제한과 마찬가지로요. 우리는 "Streams API가 새로운 Stack Overflow 질문의 가장 큰 단일 원천이 될 것"이라고 예측합니다. 그럼에도 불구하고 우리는 그 잠재력에 대해 여전히 열광하고 있습니다.

다음 금요일에는 람다 표현식을 사용한 정렬과 데이터베이스 상호작용 개선에 대해 다룰 예정입니다.

## 추가 자료

더 많은 Java 8 자료는 Eugen Paraschiv의 baeldung.com에서 찾아보실 수 있습니다.
