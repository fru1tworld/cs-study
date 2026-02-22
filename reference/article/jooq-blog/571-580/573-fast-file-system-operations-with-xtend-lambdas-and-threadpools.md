# Xtend, 람다, ThreadPool로 빠른 파일 시스템 작업

> 원문: https://blog.jooq.org/fast-file-system-operations-with-xtend-lambdas-and-threadpools/

저는 최근에 Java 8 준비에 대해 블로그에 글을 썼고, 기존 코드에서 SAM(Single Abstract Method) 패턴을 활용하는 좋은 예시로 java.io.FileFilter를 사용했습니다. 또 다른 글에서는 Scala에 대해 이야기했지만, 가끔 Scala가 너무 멀리 나아가는 경우도 있습니다.

Eclipse Xtend는 Java 소스 코드로 컴파일되는 Java의 "방언(dialect)"이며, 그 다음에 바이트 코드로 컴파일됩니다. 이것은 매우 흥미로운 접근 방식입니다! 저는 최근 파일 시스템에서 재귀적 작업을 수행하기 위해 Xtend를 사용했습니다. 작은 작업들을 수많은 파일에 적용하고 싶었고, 작은 스레드 풀을 사용하여 스레드들 사이에 작업을 분산시키고 싶었습니다. 아래의 간결한 Xtend 코드를 확인해 보세요:

```java
class Transform {

  // 모든 "힘든" 작업을 수행하는
  // 스레드 풀입니다
  static ExecutorService ex;

  def static void main(String[] args) {
    // 의미 있는 값으로 스레드 풀을
    // 초기화합니다
    ex = Executors::newFixedThreadPool(4);

    // 변환 메서드에 루트 디렉토리를
    // 전달합니다
    val in = new File(...);

    // 파일 변환으로 재귀합니다
    transform(in);
  }

  def static transform(File in) {

    // 대상 파일 이름을 계산합니다
    val out = new File(...);

    // 디렉토리로 재귀합니다
    if (in.directory) {

      // Xtend 람다 표현식 형태로
      // FileFilter를 전달합니다
      for (file : in.listFiles[path |
             !path.name.endsWith(".class")
          && !path.name.endsWith(".zip")
          && !path.name.endsWith(".jar")
        ]) {
        transform(file);
      }
    }
    else {
      // ExecutorService에 Xtend 람다
      // 표현식을 전달합니다
      ex.submit[ |
        // read와 write는 Apache Commons IO로
        // 구현될 수 있습니다
        write(out, transform(read(in)));
      ];
    }
  }

  def static transform(String content) {
    // 실제 문자열 변환을 수행합니다
  }
}
```

위 코드에서 저는 몇 가지 좋은 Xtend 기능을 활용하고 있습니다:

- File.listFiles() 또는 ExecutorService.submit() 같은 몇몇 JDK 메서드에 람다를 전달합니다. 이것은 정말 멋진 기능입니다. SAM이 어디에든 있기 때문입니다.
- 지역 변수에 대해 val, var 또는 for를 사용할 때, 또는 def를 사용하여 메서드 반환 타입에 대해 타입 추론을 사용합니다. 물론 명시적으로 타입을 지정할 수도 있습니다.
- 메서드에 람다를 전달할 때 괄호를 생략합니다.
- getter/setter를 프로퍼티 이름으로 호출합니다. 예: path.getName() 대신 path.name
- 세미콜론을 사용하지 않습니다. 이것들은 Xtend에서 선택 사항입니다.

Xtend는 이미 지금 Java 8입니다. 이것은 위와 같은 스크립트에 매우 유용할 수 있습니다. 그것은 Scala만큼 강력하지는 않습니다(하지만 거의 비슷하게 간결합니다). 반면에 그것은 Scala보다 Java와 훨씬 더 잘 통합됩니다. Eclipse IDE에서 자동화 작업을 수행할 때 Xtend를 시도해 보세요!
