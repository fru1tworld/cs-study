# Oracle의 객체 지향 PL/SQL 확장

> 원문: https://blog.jooq.org/oracles-object-oriented-plsql-extensions/

2011년 8월 21일 (2014년 3월 26일 수정), lukaseder

최근 Oracle PL/SQL 언어의 흥미로운 기능을 재발견했습니다. 자신만의 타입을 매우 쉽게 정의할 수 있을 뿐만 아니라, 다른 객체 지향 언어처럼 "메서드"를 연결할 수도 있습니다. Oracle은 이러한 "메서드"를 멤버 함수(member functions)와 멤버 프로시저(member procedures)라고 부릅니다. 이것은 예를 들어 여기에 문서화되어 있습니다: https://download.oracle.com/docs/cd/B28359_01/appdev.111/b28371/adobjbas.htm#i477669 따라서 다음과 같이 자신만의 타입을 정의할 수 있습니다

```sql
create type circle as object (
   radius number,
   member procedure draw,
   member function  circumference return number
);

create type body circle as
   member procedure draw is
   begin
     null; -- 원을 그린다
   end draw;

   member function circumference return number is
   begin
     return 2 * 3.141 * radius;
   end circumference;
end;
```

PL/SQL에서는 해당 타입을 인스턴스화하고 멤버 프로시저와 함수를 쉽게 호출할 수 있습니다:

```sql
declare
   c circle;
begin
   c := circle(5);
   dbms_output.put_line(to_char(c.circumference));
end;
```

동일한 함수를 JDBC에서 다음 구문으로 호출할 수 있습니다

```java
CallableStatement call = c.prepareCall(
  "{ ? = call circle(5).circumference() }");
```

jOOQ가 생성하는 UDT 레코드가 기본 멤버 프로시저와 함수에 대한 접근을 제공하는 것은 자연스러운 일입니다. jOOQ의 UDTRecord는 연결 가능한(attachable) 객체입니다. 즉, 데이터베이스에서 가져온 경우 jOOQ Configuration에 대한 참조를 보유하고, 따라서 JDBC Connection에 대한 참조도 보유합니다. 따라서 circle을 반환하는 프로시저를 만들면, 해당 circle에서 직접 프로시저와 함수를 호출할 수 있습니다. Java 코드는 다음과 같을 수 있습니다:

```java
// 새로운 circle 타입을 반환하는 저장 함수를 호출한다
CircleRecord circle = Functions.getNewCircle(configuration);

// 연결된 CircleRecord를 사용하여 원주를 계산한다
BigDecimal circumference = circle.circumference();

// 데이터베이스에서 원을 그린다
circle.draw();
```

jOOQ의 향후 버전에서 이 흥미로운 새 기능을 기대해 주세요! 이 멋진 기능은 이미 오래 전부터 jOOQ에 포함되어 있습니다!
