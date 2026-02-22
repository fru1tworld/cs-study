> 원문: https://blog.jooq.org/how-to-order-file-names-semantically-in-java/

# Java에서 파일 이름을 의미론적으로 정렬하는 방법

## 개요

이 글에서는 Java에서 버전이 포함된 파일 이름을 의미론적(자연적)으로 정렬하는 방법을 설명합니다. 버전 번호를 사전순이 아닌 숫자 기준으로 비교하는 방식입니다.

## 문제 상황

표준 Java 정렬은 버전이 포함된 파일에서 직관적이지 않은 결과를 생성합니다:

사전순(Lexicographic) 정렬:
- version-1
- version-10
- version-10.1
- version-2
- version-21

의미론적(Semantic) 정렬 (원하는 결과):
- version-1
- version-2
- version-10
- version-10.1
- version-21

## 핵심 솔루션: FilenameComparator 클래스

```java
public final class FilenameComparator
implements Comparator<String> {

    private static final Pattern NUMBERS =
        Pattern.compile("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)");

    @Override
    public final int compare(String o1, String o2) {

        // 선택적 "NULLS LAST" 의미론:
        if (o1 == null || o2 == null)
            return o1 == null ? o2 == null ? 0 : -1 : 1;

        // 위 패턴으로 두 입력 문자열을 분할
        String[] split1 = NUMBERS.split(o1);
        String[] split2 = NUMBERS.split(o2);
        int length = Math.min(split1.length, split2.length);

        // 개별 세그먼트를 순회
        for (int i = 0; i < length; i++) {
            char c1 = split1[i].charAt(0);
            char c2 = split2[i].charAt(0);
            int cmp = 0;

            // 두 세그먼트가 모두 숫자로 시작하면
            // BigInteger를 사용하여 숫자로 안전하게 정렬
            if (c1 >= '0' && c1 <= '9' && c2 >= '0' && c2 <= '9')
                cmp = new BigInteger(split1[i]).compareTo(
                      new BigInteger(split2[i]));

            // 이전에 숫자로 정렬하지 않았거나
            // 숫자 정렬 결과가 동일한 경우 (예: 007과 7)
            // 사전순으로 정렬
            if (cmp == 0)
                cmp = split1[i].compareTo(split2[i]);

            // 접두사의 순서가 다르면 중단
            if (cmp != 0)
                return cmp;
        }

        // 여기에 도달하면 두 문자열의 접두사 순서가 동일하지만
        // 한 문자열이 다른 문자열보다 길 수 있음 (세그먼트가 더 많음)
        return split1.length - split2.length;
    }
}
```

## 사용 예제

```java
// 무작위 순서
List<String> list = asList(
    "version-10",
    "version-2",
    "version-21",
    "version-1",
    "version-10.1"
);

// 버전을 파일로 변환
List<File> l2 = list
    .stream()
    .map(s -> "C:\\temp\\" + s + ".sql")
    .map(File::new)
    .collect(Collectors.toList());

System.out.println("Natural sorting");
l2.stream()
  .sorted()
  .forEach(System.out::println);

System.out.println();
System.out.println("Semantic sorting");
l2.stream()
  .sorted(Comparator.comparing(
      File::getName,
      new FilenameComparator()))
  .forEach(System.out::println);
```

## 출력 결과

사전순 정렬:
```
C:\temp\version-1.sql
C:\temp\version-10.1.sql
C:\temp\version-10.sql
C:\temp\version-2.sql
C:\temp\version-21.sql
```

의미론적 정렬:
```
C:\temp\version-1.sql
C:\temp\version-2.sql
C:\temp\version-10.1.sql
C:\temp\version-10.sql
C:\temp\version-21.sql
```

## 개선된 버전: 파일 확장자 처리

파일 이름과 확장자를 분리하는 Windows 스타일의 적절한 정렬을 위한 버전:

```java
public final class FilenameComparator
implements Comparator<String> {

    private static final Pattern NUMBERS =
        Pattern.compile("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)");
    private static final Pattern FILE_ENDING =
        Pattern.compile("(?<=.*)(?=\\.*)");

    @Override
    public final int compare(String o1, String o2) {
        if (o1 == null || o2 == null)
            return o1 == null ? o2 == null ? 0 : -1 : 1;

        String[] name1 = FILE_ENDING.split(o1);
        String[] name2 = FILE_ENDING.split(o2);

        String[] split1 = NUMBERS.split(name1[0]);
        String[] split2 = NUMBERS.split(name2[0]);
        int length = Math.min(split1.length, split2.length);

        // 개별 세그먼트를 순회
        for (int i = 0; i < length; i++) {
            char c1 = split1[i].charAt(0);
            char c2 = split2[i].charAt(0);
            int cmp = 0;

            if (c1 >= '0' && c1 <= '9' && c2 >= 0 && c2 <= '9')
                cmp = new BigInteger(split1[i]).compareTo(
                      new BigInteger(split2[i]));

            if (cmp == 0)
                cmp = split1[i].compareTo(split2[i]);

            if (cmp != 0)
                return cmp;
        }

        int cmp = split1.length - split2.length;
        if (cmp != 0)
            return cmp;

        cmp = name1.length - name2.length;
        if (cmp != 0)
            return cmp;

        return name1[1].compareTo(name2[1]);
    }
}
```

## 핵심 기술 세부사항

정규식 패턴 `"(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)"`는 숫자와 비숫자 사이의 경계를 내용을 캡처하지 않고 매칭하여 깔끔한 문자열 세분화를 가능하게 합니다. 이 알고리즘은 두 세그먼트가 모두 숫자로 시작하면 숫자로 비교하고, 그렇지 않으면 사전순으로 비교합니다.

## 제한 사항 및 대안

이 구현은 유니코드 처리에 잠재적인 문제가 있을 수 있습니다. JDK의 제안된 구현을 참조하면 정규식 분할 대신 문자별 처리를 통해 이러한 문제를 더 견고하게 처리할 수 있습니다.
