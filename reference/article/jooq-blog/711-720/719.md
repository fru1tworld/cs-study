# if-else 코딩 스타일 모범 사례

> 원문: https://blog.jooq.org/if-else-coding-style-best-practices/

2012년 1월 18일 lukaseder 게시

다음 글은 정답이나 오답이 없는, 그저 "취향의 문제"에 가까운 고급 중괄호 토론이 될 것이다.

이것은 "else"(그리고 "catch", "finally"와 같은 다른 키워드들)를 새 줄에 놓을 것인지 아닌지에 대한 것이다. 어떤 사람들은 이렇게 작성할 수 있다:

```java
if (something) {
  doIt();
} else {
  dontDoIt();
}
```

그러나 나는 다음을 선호한다:

```java
if (something) {
  doIt();
}
else {
  dontDoIt();
}
```

바보같아 보일 수도 있다. 하지만 주석은 어떤가? 주석은 어디에 들어가야 하는가? 다음은 내게 뭔가 잘못되어 보인다:

```java
// 뭔가가 발생하는 경우이고 어쩌고
// 저쩌고 등등...
if (something) {
  doIt();
} else {
  // 이것은 10%의 확률로만 발생하며, 그때는
  // 하지 않는 것에 대해 두 번 생각해 보는 것이 좋다
  dontDoIt();
}
```

다음이 훨씬 낫지 않은가?

```java
// 뭔가가 발생하는 경우이고 어쩌고
// 저쩌고 등등...
if (something) {
  doIt();
}

// 이것은 10%의 확률로만 발생하며, 그때는
// 하지 않는 것에 대해 두 번 생각해 보는 것이 좋다
else {
  dontDoIt();
}
```

두 번째 경우에서, 나는 정말로 "if" 케이스와 "else" 케이스를 별도로 문서화하고 있다. "dontDoIt()" 호출을 문서화하는 것이 아니다. 이것은 더 확장될 수 있다:

```java
// 뭔가가 발생하는 경우이고 어쩌고
// 저쩌고 등등...
if (something) {
  doIt();
}

// 만약의 경우를 위해
else if (somethingElse) {
  doSomethingElse();
}

// 이것은 10%의 확률로만 발생하며, 그때는
// 하지 않는 것에 대해 두 번 생각해 보는 것이 좋다
else {
  dontDoIt();
}
```

또는 try-catch-finally에서도:

```java
// 비즈니스 로직을 시도해 보자
try {
  doIt();
}

// IOException은 실제로 발생하지 않는다
catch (IOException ignore) {}

// SQLException은 전파되어야 한다
catch (SQLException e) {
  throw new RuntimeException(e);
}

// 리소스를 정리한다
finally {
  cleanup();
}
```

깔끔해 보이지 않는가? 다음과 비교해 보라:

```java
// 비즈니스 로직을 시도해 보자
try {
  doIt();
} catch (IOException ignore) {
  // IOException은 실제로 발생하지 않는다
} catch (SQLException e) {
  // SQLException은 전파되어야 한다
  throw new RuntimeException(e);
} finally {
  // 리소스를 정리한다
  cleanup();
}
```

여러분의 생각이 궁금하다...
