# jOOR을 사용하여 어노테이션 프로세서를 단위 테스트하는 방법

> 원문: https://blog.jooq.org/how-to-unit-test-your-annotation-processor-using-joor/

게시일: 2018년 12월 7일 / 2020년 12월 21일
작성자: lukaseder

---

어노테이션 프로세서는 Java 언어에 특정 언어 기능을 도입하기 위한 해키한 우회 방법으로 유용할 수 있습니다. jOOQ도 다음과 같은 SQL 구문 검증을 돕는 어노테이션 프로세서를 가지고 있습니다:

- Plain SQL 사용 (SQL 인젝션 위험)
- SQL 방언 지원 (MySQL에서 Oracle 전용 기능 사용 방지)

이에 대한 자세한 내용은 여기서 읽을 수 있습니다.

## 어노테이션 프로세서 단위 테스트하기

어노테이션 프로세서를 단위 테스트하는 것은 사용하는 것보다 조금 더 까다롭습니다. 프로세서는 Java 컴파일러에 연결되어 컴파일된 AST를 조작합니다(또는 다른 작업을 수행합니다). 자신만의 프로세서를 테스트하려면 테스트에서 Java 컴파일러를 실행해야 하지만, 일반적인 프로젝트 설정에서는 이것이 어렵습니다. 특히 주어진 테스트의 예상 동작이 컴파일 오류인 경우에는 더욱 그렇습니다. 다음과 같은 두 개의 어노테이션이 있다고 가정해 봅시다:

```java
@interface A {}
@interface B {}
```

이제 `@A`는 항상 `@B`와 함께 사용되어야 한다는 규칙을 만들고 싶습니다. 예를 들면:

```java
// 이것은 컴파일되면 안 됨
@A
class Bad {}

// 이것은 괜찮음
@A @B
class Good {}
```

이것을 어노테이션 프로세서로 강제할 것입니다:

```java
class AProcessor implements Processor {
    boolean processed;
    private ProcessingEnvironment processingEnv;

    @Override
    public Set<String> getSupportedOptions() {
        return Collections.emptySet();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton("*");
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.RELEASE_8;
    }

    @Override
    public void init(ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (TypeElement e1 : annotations)
            if (e1.getQualifiedName().contentEquals(A.class.getName()))
                for (Element e2 : roundEnv.getElementsAnnotatedWith(e1))
                    if (e2.getAnnotation(B.class) == null)
                        processingEnv.getMessager().printMessage(ERROR, "Annotation A must be accompanied by annotation B");

        this.processed = true;
        return false;
    }

    @Override
    public Iterable<? extends Completion> getCompletions(Element element, AnnotationMirror annotation, ExecutableElement member, String userText) {
        return Collections.emptyList();
    }
}
```

자, 이것은 작동합니다. Maven 컴파일러 설정에 어노테이션 프로세서를 추가하고 몇몇 클래스에 A와 B 어노테이션을 붙여서 수동으로 쉽게 검증할 수 있습니다. 하지만 누군가 코드를 변경하면 회귀를 알아차리지 못합니다. 수동으로 하는 대신 어떻게 이것을 단위 테스트할 수 있을까요?

## jOOR 0.9.10의 어노테이션 프로세서 지원

jOOR은 jOOQ 내부에서 사용하는 작은 오픈소스 리플렉션 라이브러리입니다. jOOR은 `Reflect.compile()`을 통해 `javax.tools.JavaCompiler` API를 호출하는 편리한 API를 제공합니다. 가장 최근 릴리스인 0.9.10은 이제 어노테이션 프로세서를 등록할 수 있는 선택적 `CompileOptions` 인수를 받습니다. 이것은 이제 다음과 같이 매우 간단한 단위 테스트를 작성할 수 있다는 것을 의미합니다 (그리고 Java 15+를 사용한다면 텍스트 블록의 이점을 누릴 수 있습니다! 텍스트 블록이 없는 Java 11 호환 버전은 github의 단위 테스트를 참조하세요):

```java
@Test
public void testCompileWithAnnotationProcessors() {
    AProcessor p = new AProcessor();

    try {
        Reflect.compile(
            "org.joor.test.FailAnnotationProcessing",
            """
             package org.joor.test;
             @A
             public class FailAnnotationProcessing {
             }
            """,
            new CompileOptions().processors(p)
        ).create().get();
        Assert.fail();
    }
    catch (ReflectException expected) {
        assertTrue(p.processed);
    }

    Reflect.compile(
        "org.joor.test.SucceedAnnotationProcessing",
        """
         package org.joor.test;
         @A @B
         public class SucceedAnnotationProcessing {
         }
        """,
        new CompileOptions().processors(p)
    ).create().get();
    assertTrue(p.processed);
}
```

정말 쉽습니다! 이제 어노테이션 프로세서에서 더 이상 회귀가 발생하지 않습니다!

---

## 댓글 섹션

xyz (2019년 1월 10일):
"또는 google/compile-testing을 사용할 수도 있습니다 (꽤 오래전부터 있었습니다): https://github.com/google/compile-testing"

lukaseder (2019년 1월 10일):
"네, 하지만 NIH(Not Invented Here, 여기서 발명하지 않았다)"

Ahmad Bawaneh (@AhmadBawaneh1) (2019년 5월 22일):
"안녕하세요, 좋은 글입니다. 여기에 2가지 의견을 추가하고 싶습니다:

1- Lombok이 정말 좋은 어노테이션 프로세싱 예제라고 생각하시나요? 그들은 실제로 APT 명세를 사용하고 있지 않습니다.

2- 위의 프로세서 예제에서 두 어노테이션이 모두 클래스에 존재하지 않을 때 런타임 예외를 던지고 있는데, 어노테이션 프로세서에서 오류를 강조하는 권장 방법은 ProcessingEnvironment에서 얻은 messager를 사용하고 메시지 로그에 오류를 발생시키는 요소를 전달하는 것입니다. 이는 IDE가 해당 요소를 표시하는 데 도움이 됩니다.

감사합니다"

lukaseder (2019년 5월 22일):
"댓글 감사합니다, Ahmad.

1과 2 모두 맞습니다. 해당 문장을 삭제하고 주석을 추가했습니다."

skapral (2019년 9월 28일):
"모든 것이 훌륭하고 멋지며, 아이디어가 정말 마음에 듭니다. 하지만 어노테이션 프로세서가 일부 소스를 생성하는 경우, 생성된 소스 내용을 어떻게 검증할 수 있을까요?"

lukaseder (2019년 9월 30일):
"로직을 호출할 수 있습니다"

CKosmowski (2021년 11월 11일):
"저도 skapral과 같은 질문이 있습니다. compile()을 사용하고 프로세서가 일부 소스 파일을 생성했을 때, 생성된 클래스의 인스턴스를 어떻게 생성하여 테스트할 수 있나요? 여기서 당신의 답변이 정말 도움이 되지 않습니다. 예제를 보여주실 수 있나요?"

lukaseder (2021년 11월 17일):
"여러분께 뭐라고 말씀드려야 할지 모르겠습니다. 저도 답을 모르고, 어노테이션 프로세서에서 새로운 소스 코드를 생성하는 사용 사례에 대해 아직 깊이 생각해보지 않았습니다... org.joor.Compile의 구현을 살펴보세요. 280줄의 코드에 불과합니다.

이것은 기존 JDK 클래스 집합에 대한 매우 간단한 래퍼입니다. 이 래퍼는 모든 사용 사례의 80%를 단순화하지만, 나머지 20%에는 확실히 충분하지 않습니다. 만약 당신에게 작동하지 않는다면, JavaCompiler를 직접 호출하는 것으로 대체하는 것은 어떨까요?

도움이 되길 바랍니다"

greg higgins (2022년 1월 15일):
"텍스트 블록과 lombok을 사용하여 코드를 컴파일하는 유틸리티 메서드를 다음과 같이 만들 수 있을 것 같습니다:

```java
public static @NotNull T compileInstance(String fqn, String content){
    Objects.requireNonNull(fqn);
    Objects.requireNonNull(content);
    Class classT = Reflect.compile(fqn, content).get();
    return classT.getDeclaredConstructor().newInstance();
}
```

그리고 다음과 같이 사용합니다:

```java
@Test
@SneakyThrows
public void simpleTest(){
    Runnable runner = compileInstance("com.whatever.MyRunner",
            """
                        package  com.whatever;


                        public class MyRunner implements Runnable{
                            @Override
                            public void run() {
                                System.out.println("hello world");
                            }
                        }
                    """);
    runner.run();
}
```"

lukaseder (2022년 1월 17일):
"이것이 이전 논의에 대한 답변인지 아니면 이 블로그 글에 대한 답변인지 잘 모르겠습니다. 어노테이션 프로세싱과 관련이 없는데요... 그리고 새로운 메서드가 필요 없습니다. 생성자를 호출하는 Reflect.create()를 사용하면 됩니다."

FRANCISCO DE AZEVEDO GAMARRA (2020년 8월 12일):
"불행히도 작동하지 않습니다. 열심히 시도했지만 안 됩니다. 이 코드를 어노테이션 프로세스와 같은 프로젝트에 넣어야 하나요? 첫 번째 검증에서 실패합니다. 감사합니다!"

lukaseder (2020년 8월 12일):
"메시지 감사합니다. 설명만으로는 왜 실패하는지 말하기 어렵습니다.

> 이 코드를 어노테이션 프로세스와 같은 프로젝트에 넣어야 하나요?

예제들은 프로세서가 src/main/java에 있고, 테스트가 src/test/java에 있다고 가정합니다. 테스트와 프로세서 모두 src/test/java에 있는 몇 가지 예제를 여기서 찾을 수 있습니다: https://github.com/jOOQ/jOOR/blob/main/jOOR/src/test/java/org/joor/test/CompileOptionsTest.java

아마도, 문제를 재현하는 방법에 대한 세부 정보와 함께 https://stackoverflow.com 에서 구체적인 질문을 해보시는 것은 어떨까요?

행운을 빕니다!"
