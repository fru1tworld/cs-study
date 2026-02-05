# Scala에서의 덕 타이핑: 구조적 타이핑

> 원문: https://blog.jooq.org/duck-typing-in-scala-structural-typing/

Scala는 JVM 생태계에 많은 멋진 기능들을 가져다 줍니다. 물론 일부는 필요 이상으로 나아가기도 하지만요. 하지만 타입 안전한 덕 타이핑은 정말 굉장합니다!

```scala
def quacker(duck: {def quack(value: String): String}) {
  println (duck.quack("Quack"))
}
```

위의 `quacker` 메서드는 "꽥꽥거릴(quack)" 수 있는 어떤 "오리(duck)"든 받아들입니다.

더 긴 논의는 여기에서 확인하세요: http://java.dzone.com/articles/duck-typing-scala-structural
