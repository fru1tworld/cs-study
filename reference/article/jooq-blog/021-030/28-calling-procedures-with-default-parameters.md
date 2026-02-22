# JDBC 또는 jOOQ를 사용하여 기본 매개변수가 있는 프로시저 호출하기

> 원문: https://blog.jooq.org/calling-procedures-with-default-parameters-using-jdbc-or-jooq/

jOOQ의 코드 생성기를 사용하여 저장 프로시저를 호출하는 것은 jOOQ를 사용하는 대표적인 이유 중 하나입니다. 예를 들어, 다음과 같은 Oracle PL/SQL 프로시저가 있다고 가정해 봅시다:

```sql
CREATE OR REPLACE PROCEDURE p (
  p_i1 IN number,
  p_o1 OUT number,
  p_i2 IN varchar2,
  p_o2 OUT varchar2
)
IS
BEGIN
  p_o1 := p_i1;
  p_o2 := p_i2;
END;
```

jOOQ는 이를 매우 간단하게 호출할 수 있도록 코드를 생성해 줍니다:

```java
// Configuration에는 JDBC Connection과 기타 설정이 포함되어 있습니다
P result = Routines.p(configuration, 1, "A");
System.out.println(p.getPO1());
System.out.println(p.getPO2());
```

이 코드는 모든 `IN` 및 `OUT` 매개변수의 바인딩을 자동으로 처리하면서 다음을 실행합니다:

```
{ call "TEST"."P" (?, ?, ?, ?) }
```

프로그램의 출력은 다음과 같습니다:

```
1
A
```

그렇다면, 프로시저(또는 함수) 시그니처에 `DEFAULT` 값을 추가하면 어떻게 될까요?

```sql
CREATE OR REPLACE PROCEDURE p (
  p_i1 IN number := 1,
  p_o1 OUT number,
  p_i2 IN varchar2 := 'A',
  p_o2 OUT varchar2
)
IS
BEGIN
  p_o1 := p_i1;
  p_o2 := p_i2;
END;
```

위의 Java 코드에서는 `Routines.p()` 호출의 매개변수를 생략할 방법이 없습니다. 하지만 `Routines.p()`의 생성된 구현을 살펴보면, 이것은 단지 위치 기반 매개변수 인덱스를 사용하는 편의 메서드에 불과하다는 것을 알 수 있습니다(Java에서 익숙한 방식이죠). 프로시저 호출을 직접 인스턴스화할 수도 있으며, 두 가지 호출 방식 사이에 기술적 차이는 없습니다:

```java
P p = new P();
p.setPI1(2);
p.setPI2("B");
p.execute(configuration);
System.out.println(p.getPO1());
System.out.println(p.getPO2());
```

위 구문을 사용하면, 기본값이 설정되어 있다고 알고 있는 매개변수를 생략할 수 있습니다. 예를 들어:

```java
P p = new P();
p.setPI1(2);
p.execute(configuration);
System.out.println(p.getPO1());
System.out.println(p.getPO2());
```

이제 JDBC 이스케이프 구문 대신, jOOQ는 다음과 같은 익명 블록을 렌더링합니다:

```sql
begin
  "TEST"."P" ("P_I1" => ?, "P_O1" => ?, "P_O2" => ?)
end;
```

`P_I2`가 프로시저 호출에 명시적으로 전달되지 않는다는 점에 주목하세요.

출력은 다음과 같습니다:

```
2
A
```

이 기능은 기본 매개변수를 지원하는 모든 RDBMS에서 동작하며, 각 데이터베이스는 이름으로 매개변수를 전달하는 고유한 구문을 사용합니다. 지원되는 데이터베이스는 최소한 다음을 포함합니다:

- Db2
- Informix
- Oracle
- PostgreSQL
- SQL Server
