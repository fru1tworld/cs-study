# Java에 더 나은 try-catch 구문이 있었다면 검사 예외를 여전히 비판했을까?

> 원문: https://blog.jooq.org/would-we-still-criticise-checked-exceptions-if-java-had-a-better-try-catch-syntax/

JUnit 5에 관한 이전 블로그 게시물의 맥락에서, 우리 독자 중 한 명인 Maaartinus가 매우 흥미로운 아이디어를 제시했습니다:

> try-catch의 유일한 문제는 장황함인데, 이것은 제가 감수할 수 있는 부분입니다 (제 생각에는 catch만 단독으로 쓰는 것이 더 나을 것 같습니다. 암묵적인 try가 블록 내의 모든 이전 코드에 적용되는 것이죠; 단순한 구문적 설탕입니다)

허! 다음이 유효한 Java 코드인 세상을 상상해 보세요:

```java
{
    something();
}
catch (Exception e) {
    /* 위 블록에서 발생한 모든 예외 */
}
```

마찬가지로:

```java
{
    something();
}
finally {
    /* 이전 블록 후 정리 */
}
```

다른 언어들에서는 이것이 정확히 이렇게 구현되어 있습니다. 예를 들어 PL/SQL을 봅시다. 일반적인 블록은 다음과 같습니다:

```sql
BEGIN
  SOMETHING();
END;
```

중괄호를 `BEGIN`과 `END` 키워드로 대체하면 정확히 같은 것이 됩니다. 이제 `SOMETHING`이 예외를 발생시키면, PL/SQL에서는 Java의 `catch`와 정확히 같은 역할을 하는 `EXCEPTION` 블록을 추가할 수 있습니다:

```sql
BEGIN
  SOMETHING();
EXCEPTION
  WHEN OTHERS THEN NULL;
END;
```

실제로, 이러한 매우 간단한 경우에서 `try` 키워드는 PL/SQL에 그런 키워드가 없는 것처럼 선택 사항인 것 같으며, `catch`와 `finally` 블록의 범위가 매우 잘 정의되어 있기 때문에 실제로 필요하지 않습니다 (물론 언뜻 보기에 주의사항이 있을 수 있습니다).

## 그래서 어떻다고? 3글자를 절약했을 뿐인데...

이러한 간단한 경우에서는 "개선된" 구문으로 많은 것을 얻지 못합니다. 하지만 악명 높게 장황한 `try { ... } catch { ... }` 구문이 우리를 짜증나게 하는 다른 많은 경우들은 어떨까요...? 다시 말하지만, PL/SQL에서는 `BEGIN .. END`를 사용하는 블록을 사용할 때마다 선택적으로 `EXCEPTION` 블록을 추가하는 것의 이점을 자동으로 누릴 수 있습니다. 이것을 철저하게 검토하지는 않았지만, 이것은 Java에 엄청난 구문적 가치를 더할 수 있습니다. 예를 들어: 람다

```java
// 더 나은 방식:
Consumer<String> consumer = string -> {
    something();
}
catch (Exception e) {
    /* 여전히 consumer의 일부 */
}

// 기존 방식:
Consumer<String> consumer = string -> {
    try {
        something();
    }
    catch (Exception e) {
        /* 여전히 consumer의 일부 */
    }
}
```

이것이 람다와 Stream API에서의 검사 예외에 대한 긴 논의를 방지했을까요?

반복문

```java
// 더 나은 방식:
for (String string : strings) {
    something();
}
catch (Exception e) {
    /* 여전히 반복문 순회의 일부 */
}

// 기존 방식:
for (String string : strings) {
    try {
        something();
    }
    catch (Exception e) {
        /* 여전히 반복문 순회의 일부 */
    }
}
```

다시 말하지만, 여기서 엄청난 구문적 가치가 있습니다! if / else 일관성을 위해서인데, Java 코드에 익숙한 사람들에게는 다소 이상하게 보일 수 있습니다. 하지만 틀에서 벗어나 생각하고 다음을 인정해 봅시다!

```java
// 더 나은 방식:
if (check) {
    something();
}
catch (Exception e) {
    /* 여전히 if 분기의 일부 */
}
else {
    somethingElse();
}
catch (Exception e) {
    /* 여전히 else 분기의 일부 */
}

// 기존 방식:
if (check) {
    try {
        something();
    }
    catch (Exception e) {
        /* 여전히 if 분기의 일부 */
    }
}
else {
    try {
        somethingElse();
    }
    catch (Exception e) {
        /* 여전히 else 분기의 일부 */
    }
}
```

허! 메서드 본문 마지막으로 중요한 것은 메서드 본문이 이 추가적인 구문 설탕으로부터 가장 큰 이점을 얻는 궁극적인 엔티티가 될 것이라는 점입니다. 메서드의 중괄호가 필수 블록(또는 필수 `BEGIN .. END` 구조)일 뿐이라고 인정한다면, 다음과 같이 할 수 있습니다:

```java
// 더 나은 방식:
public void method() {
    something();
}
catch (Exception e) {
    /* 여전히 메서드 본문의 일부 */
}

// 기존 방식:
public void method() {
    try {
        something();
    }
    catch (Exception e) {
        /* 여전히 메서드 본문의 일부 */
    }
}
```

이것은 (정적) 초기화 블록에서 특히 유용한데, (정적) 초기화 블록에서는 throws 절을 지정할 방법이 없기 때문에 예외 처리가 항상 고통스럽습니다 (이것을 수정할 좋은 기회가 될 수 있습니다!)

```java
class Something {

    // 더 나은 방식:
    static {
        something();
    }
    catch (Exception e) {
        /* 여전히 초기화 블록 본문의 일부 */
    }

    // 기존 방식:
    static {
        try {
            something();
        }
        catch (Exception e) {
            /* 여전히 초기화 블록 본문의 일부 */
        }
    }
}
```

## 한 단계 더 나아가기

물론, 우리는 여기서 멈추지 않을 것입니다. `catch` (또는 `finally`) 뒤에 중괄호를 넣어야 하는 매우 독특한 요구사항도 없앨 것입니다. 위의 것을 확립한 후에는 다음을 허용하는 것은 어떨까요:

```java
// 더 나은 방식:
something();
    catch (SQLException e)
        log.info(e);
    catch (IOException e)
        log.warn(e);
    finally
        close();

// 기존 방식:
try {
    something();
}
catch (SQLException e) {
    log.info(e);
}
catch (IOException e) {
    log.warn(e);
}
finally {
    close();
}
```

이제 예외 블록을 문장이 아닌 표현식으로 만들면, 갑자기 Java가 그 멋진 언어들과 매우 비슷해 보이기 시작합니다. Scala처럼. 또는 Kotlin처럼.

## 결론

물론, "이전" 구문도 여전히 가능할 것입니다. 예를 들어, try-with-resources 문을 사용할 때는 불가피합니다. 하지만 이러한 구문 설탕의 큰 장점은 예외를 _반드시_ 처리해야 하는 경우(즉, 검사 예외)에 블록을 여러 레벨 깊이로 중첩하지 않고도 처리할 수 있기 때문에 고통이 조금 줄어들 것이라는 점입니다. 아마도 이 구문이 있었다면 검사 예외를 더 이상 비판하지 않았을까요? 매우 흥미로운 아이디어입니다. 다시 한번 공유해 주셔서 감사합니다, Maaartinus. 여러분의 생각은 어떠신가요?
