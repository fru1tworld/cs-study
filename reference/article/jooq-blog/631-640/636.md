# Java Streams 프리뷰 vs .Net LINQ

> 원문: https://blog.jooq.org/java-streams-preview-vs-net-linq/

저는 "Geeks From Paradise"라는 매우 유망한 블로그를 팔로우하기 시작했습니다. 코스타리카에 사는 개발자들이 약간 부럽다는 점을 차치하고, 다가오는 Java 8 Streams API와 .NET의 다양한 LINQ API 기능을 비교한 이 글은 매우 흥미로운 읽을거리입니다. 그곳에서 찾을 수 있는 내용의 미리보기입니다 (19개 예제 중 하나):

### LINQ

```csharp
List<string> nameList1 = new List(){
  "Anders", "David", "James",
  "Jeff", "Joe", "Erik" };
nameList1.Select(c => "Hello! " + c).ToList()
         .ForEach(c => Console.WriteLine(c));
```

### Java Streams

```java
List<String> nameList1 = asList(
  "Anders", "David", "James",
  "Jeff", "Joe", "Erik");
nameList1.stream()
     .map(c -> "Hello! " + c)
     .forEach(System.out::println);
```

전체 블로그 포스트는 여기서 읽으세요: [http://blog.informatech.cr/2013/03/24/java-streams-preview-vs-net-linq/](http://blog.informatech.cr/2013/03/24/java-streams-preview-vs-net-linq/)
