# 동적 타이핑 vs 정적 타이핑에 대한 불편한 진실

> 원문: https://blog.jooq.org/the-inconvenient-truth-about-dynamic-vs-static-typing/

게시일: 2014년 12월 11일
작성자: Lukas Eder

---

때때로 진실의 순간이 있습니다. 그것은 완전히 예상치 못하게 일어나는데, 예를 들어 이 트윗을 읽었을 때처럼요:

> Facebook Flow에 대한 좋은 토론 - http://t.co/5KTKakDB0w
>
> — David J. Pearce (@whileydave) 2014년 11월 23일

David는 덜 알려졌지만 전혀 덜 흥미롭지 않은 Whiley 프로그래밍 언어의 저자입니다. 이 언어는 많은 정적 타입 검사가 내장되어 있습니다. Whiley 언어의 가장 흥미로운 기능 중 하나는 흐름 민감 타이핑(flow sensitive typing, 때로는 단순히 flow typing이라고도 함)인데, 이는 주로 유니온 타입과 함께 사용할 때 유용합니다. 시작 가이드의 예제를 보겠습니다:

```
function indexOf(string str, char c) => null|int:

function split(string str, char c) => [string]:
  var idx = indexOf(str,c)

  // idx는 null|int 타입
  if idx is int:

    // idx는 이제 int 타입
    string below = str[0..idx]
    string above = str[idx..]
    return [below,above]

  else:
    // idx는 이제 null 타입
    return [str] // 발견되지 않음
```

기억하세요, Ceylon과 같은 다른 언어들도 흐름 민감 타이핑을 알고 있으며, Java도 어느 정도는 알고 있습니다. Java도 유니온 타입을 가지고 있기 때문입니다!

```java
try {
    ...
}
catch (SQLException | IOException e) {
    if (e instanceof SQLException)
        doSomething((SQLException) e);
    else
        doSomethingElse((IOException) e);
}
```

물론, Java의 흐름 민감 타이핑은 명시적이고 장황합니다. 우리는 Java 컴파일러가 모든 타입을 추론하기를 기대할 수 있습니다. 다음 코드도 타입 체크되고 컴파일되어야 합니다:

```java
try {
    ...
}
catch (SQLException | IOException e) {
    if (e instanceof SQLException)
        // e는 SQLException 타입임이 보장됨
        doSomething(e);
    else
        // e는 IOException 타입임이 보장됨
        doSomethingElse(e);
}
```

흐름 타이핑 또는 흐름 민감 타이핑은 컴파일러가 주변 프로그램의 제어 흐름에서 유일하게 가능한 타입을 추론할 수 있다는 것을 의미합니다. 이것은 Ceylon과 같은 현대 언어에서 비교적 새로운 개념이며, 특히 언어가 `var` 또는 `val` 키워드를 통한 정교한 타입 추론도 지원할 때 정적 타이핑을 매우 강력하게 만듭니다!

## Flow를 사용한 JavaScript 정적 타이핑

David의 트윗으로 돌아가서 그 기사가 Flow에 대해 무엇을 말했는지 살펴봅시다: http://sitr.us/2014/11/21/flow-is-the-javascript-type-checker-i-have-been-waiting-for.html

> `null` 인자와 함께 `length`를 사용하는 것이 존재한다는 것은 Flow에게 해당 함수에 `null` 체크가 있어야 한다고 알려줍니다. 이 버전은 타입 체크를 통과합니다:
>
> ```javascript
> function length(x) {
>   if (x) {
>     return x.length;
>   } else {
>     return 0;
>   }
> }
>
> var total = length('Hello') + length(null);
> ```
>
> Flow는 `if` 본문 내에서 `x`가 `null`이 될 수 없다는 것을 추론할 수 있습니다.

꽤 영리합니다. Microsoft의 TypeScript에서도 비슷한 기능이 곧 나올 것으로 보입니다. 하지만 Flow는 TypeScript와 다릅니다(또는 다르다고 주장합니다). Facebook Flow의 본질은 공식 Flow 발표의 이 문단에서 볼 수 있습니다:

> Flow의 타입 검사는 선택적입니다 - 모든 코드를 한 번에 타입 체크할 필요가 없습니다. 그러나 Flow 설계의 기반이 되는 가정은 대부분의 JavaScript 코드가 암묵적으로 정적 타이핑되어 있다는 것입니다; 코드 어디에도 타입이 나타나지 않더라도, 코드의 정확성을 추론하는 방법으로 개발자의 마음속에 있습니다. Flow는 가능한 한 자동으로 그러한 타입을 추론하는데, 이는 코드를 전혀 변경하지 않고도 타입 오류를 찾을 수 있다는 것을 의미합니다. 반면에, 일부 JavaScript 코드, 특히 프레임워크는 정적으로 추론하기 어려운 리플렉션을 많이 사용합니다. 이러한 본질적으로 동적인 코드의 경우, 타입 검사가 너무 부정확할 것이므로 Flow는 그러한 코드를 명시적으로 신뢰하고 넘어가는 간단한 방법을 제공합니다. 이 설계는 Facebook의 거대한 JavaScript 코드베이스에 의해 검증되었습니다: 대부분의 코드는 암묵적으로 정적 타이핑된 범주에 속하며, 개발자가 코드에 명시적으로 타입을 주석 달지 않고도 타입 오류를 확인할 수 있습니다.

## 이것을 깊이 생각해 봅시다

> 대부분의 JavaScript 코드는 암묵적으로 정적 타이핑되어 있습니다

다시 한번

> JavaScript 코드는 암묵적으로 정적 타이핑되어 있습니다

그렇습니다! 프로그래머는 타입 시스템을 좋아합니다. 프로그래머는 데이터 타입에 대해 형식적으로 추론하고 프로그램이 올바른지 확인하기 위해 좁은 제약 조건에 넣는 것을 좋아합니다. 그것이 정적 타이핑의 전체 본질입니다: 잘 설계된 데이터 구조 덕분에 실수를 줄이는 것. 사람들은 또한 데이터베이스에서 데이터 구조를 잘 설계된 형태로 넣는 것을 좋아합니다. 그래서 SQL이 그렇게 인기 있고 "스키마 없는" 데이터베이스가 더 많은 시장 점유율을 얻지 못하는 것입니다. 왜냐하면 사실, 같은 이야기이기 때문입니다. "스키마 없는" 데이터베이스에도 여전히 스키마가 있습니다, 단지 타입 체크되지 않고 따라서 정확성을 보장하는 모든 부담을 여러분에게 남깁니다.

여담으로: 분명히, 일부 NoSQL 벤더들은 자사 제품을 필사적으로 포지셔닝하기 위해 이런 터무니없는 블로그 포스트를 계속 작성하며, 스키마가 전혀 필요 없다고 주장합니다. 하지만 그 마케팅 속임수를 쉽게 알아볼 수 있습니다. 스키마 없음에 대한 진정한 필요성은 동적 타이핑에 대한 진정한 필요성만큼이나 드뭅니다. 다시 말해서, 마지막으로 Java 프로그램을 작성하면서 모든 메서드를 리플렉션으로 호출한 것이 언제입니까? 바로... 하지만 정적 타이핑 언어가 과거에 가지지 못했고 동적 타이핑 언어가 가졌던 한 가지가 있습니다: 장황함을 우회하는 수단. 왜냐하면 프로그래머는 타입 시스템과 타입 검사를 좋아하지만, 프로그래머는 타이핑(키보드로 치는 것)을 좋아하지 _않기_ 때문입니다.

## 장황함이 문제입니다. 정적 타이핑이 아닙니다

Java의 진화를 살펴봅시다:

Java 4

```java
List list = new ArrayList();
list.add("abc");
list.add("xyz");

// 으. 왜 이 Iterator가 필요하지?
Iterator iterator = list.iterator();
while (iterator.hasNext()) {
    // 에휴, 문자열만 있다는 걸 *알고* 있잖아. 왜 캐스팅?
    String value = (String) iterator.next();

    // [...]
}
```

Java 5

```java
// 아, 제네릭 타입을 두 번 선언해야 하다니!
List<String> list = new ArrayList<String>();
list.add("abc");
list.add("xyz");

// 훨씬 낫지만, String을 또 써야 해?
for (String value : list) {
    // [...]
}
```

Java 7

```java
// 낫지만, 여전히 "같은" List 타입을
// 두 번 써야 합니다
List<String> list = new ArrayList<>();
list.add("abc");
list.add("xyz");

for (String value : list) {
    // [...]
}
```

Java 8

```java
// 이제 서서히 나아지고 있습니다
Stream.of("abc", "xyz").forEach(value -> {
    // [...]
});
```

여담으로, 네, `Arrays.asList()`를 쭉 사용할 수 있었습니다. Java 8은 여전히 완벽과는 거리가 멀지만, 상황은 점점 나아지고 있습니다. 컴파일러가 추론할 수 있기 때문에 더 이상 람다 인자 목록에서 타입을 선언하지 않아도 된다는 사실은 생산성과 채택에 있어 정말 중요합니다. Java 8 이전의 람다에 해당하는 것을 생각해 봅시다(만약 이전에 Stream이 있었다면):

```java
// 네, Consumer입니다, 좋아요. 그리고 네 String을 받습니다
Stream.of("abc", "xyz").forEach(new Consumer<String>(){
    // 그리고 네, 메서드는 accept라고 합니다 (누가 신경 쓰나요)
    // 그리고 네, String을 받습니다 (이미 말했잖아요!?)
    @Override
    public void accept(String value) {
        // [...]
    }
});
```

이제, Java 8 버전을 JavaScript 버전과 비교해 보면:

```javascript
["abc", "xyz"].forEach(function(value) {
    // [...]
});
```

우리는 함수형이고 동적 타이핑인 JavaScript와 거의 같은 수준의 낮은 장황함에 도달했습니다(Java에서 리스트와 맵 리터럴이 없는 것이 정말 아쉽지 않습니다). 유일한 차이점은 우리(그리고 컴파일러)가 `value`가 `String` 타입이라는 것을 _안다_는 것입니다. 그리고 우리는 `forEach()` 메서드가 존재한다는 것을 _압니다_. 그리고 우리는 `forEach()`가 하나의 인자를 가진 함수를 받는다는 것을 _압니다_.

## 결국, 모든 것은 이것으로 귀결되는 것 같습니다:

JavaScript와 PHP 같은 동적 타이핑 언어는 주로 "그냥 실행되었기" 때문에 인기를 얻었습니다. 고전적인 정적 타이핑 언어가 요구하는 모든 "무거운" 구문을 배울 필요가 없었습니다(Ada와 PL/SQL을 생각해 보세요!). 그냥 프로그램을 작성하기 시작할 수 있었습니다. 프로그래머들은 변수에 문자열이 포함될 것이라는 것을 "_알고_" 있었고, 그것을 적어 둘 필요가 없었습니다. 그리고 그것은 사실입니다, 모든 것을 적어 둘 필요가 없습니다! Scala(또는 C#, Ceylon, 거의 모든 현대 언어)를 생각해 보세요:

```scala
val value = "abc"
```

`String` 외에 무엇이 될 수 있겠습니까?

```scala
val list = List("abc", "xyz")
```

`List[String]` 외에 무엇이 될 수 있겠습니까? 필요하다면 여전히 변수에 명시적으로 타입을 지정할 수 있습니다 - 항상 그런 엣지 케이스가 있습니다:

```scala
val list : List[String] = List[String]("abc", "xyz")
```

하지만 대부분의 구문은 "선택적"이며 컴파일러에 의해 추론될 수 있습니다.

## 동적 타이핑 언어는 죽었습니다

이 모든 것의 결론은 정적 타이핑 언어에서 구문적 장황함과 마찰이 제거되면, 동적 타이핑 언어를 사용하는 것에는 절대적으로 아무런 이점이 없다는 것입니다. 컴파일러는 매우 빠르고, 올바른 도구를 사용하면 배포도 빠를 수 있으며, 정적 타입 검사의 이점은 막대합니다. 예를 들어, SQL도 마찰의 많은 부분이 여전히 구문에 의해 생성되는 정적 타이핑 언어입니다. 그러나 많은 사람들이 SQL이 동적 타이핑 언어라고 생각하는데, 그들이 JDBC를 통해, 즉 타입이 없는 연결된 SQL 문자열을 통해 SQL에 접근하기 때문입니다. PL/SQL, Transact-SQL, 또는 jOOQ로 Java에 임베디드된 SQL을 작성하고 있다면, SQL을 이렇게 생각하지 않을 것이고 PL/SQL, Transact-SQL, 또는 Java 컴파일러가 모든 SQL 문을 타입 체크한다는 사실을 즉시 고마워할 것입니다.

그러니, 모든 타입을 입력하기 귀찮아서 만든 이 혼란을 버립시다(말장난입니다). 즐거운 타이핑(typing)을! 그리고 만약 이것을 읽고 있는 Java 언어 전문가 그룹 멤버가 있다면, 제발 Java 언어에 `var`와 `val`, 그리고 흐름 민감 타이핑을 추가해 주세요. 우리는 이것 때문에 여러분을 영원히 사랑할 것입니다, 약속합니다!
