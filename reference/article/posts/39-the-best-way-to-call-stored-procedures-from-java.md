# Java에서 저장 프로시저를 호출하는 가장 좋은 방법: jOOQ 활용

> 원문: https://blog.jooq.org/the-best-way-to-call-stored-procedures-from-java-with-jooq/

jOOQ는 주로 코드 생성을 통해 제공되는 강력한 타입 안전(type safe), 임베디드, 동적 SQL 기능으로 잘 알려져 있습니다. 하지만 코드 생성의 또 다른 활용 사례는 저장 프로시저에 사용하는 것입니다(경우에 따라 저장 프로시저 전용으로만 사용하기도 합니다).

저장 프로시저는 복잡한 데이터 처리 로직을 서버로 옮기는 강력한 방법입니다. 성능상의 이유로 대부분의 애플리케이션이 실제로 하고 있는 것보다 더 자주 이렇게 해야 합니다. 예를 들어 서버 왕복(roundtrip)을 줄이는 것에 대한 이 글을 참고하세요. 하지만 이는 클라이언트에게 API를 제공하면서 SQL 기반 세부 사항(예: 스키마, 테이블 구조, 트랜잭션 스크립트 등)을 클라이언트로부터 숨기는 실용적인 방법이 될 수도 있습니다 — 애플리케이션/팀에서 그것이 유용한 경우라면 말입니다.

어떤 경우든, jOOQ는 모든 함수, 프로시저, 패키지, UDT 등에 대한 스텁을 생성하여 여러분을 크게 도와줄 것입니다.

## JDBC 프로시저 호출 예제

Oracle에서의 간단한 예제 프로시저는 다음과 같습니다:

```sql
CREATE OR REPLACE PROCEDURE my_proc (
  i1 NUMBER,
  io1 IN OUT NUMBER,
  o1 OUT NUMBER,
  o2 OUT NUMBER,
  io2 IN OUT NUMBER,
  i2 NUMBER
) IS
BEGIN
  o1 := io1;
  io1 := i1;

  o2 := io2;
  io2 := i2;
END my_proc;
```

이 프로시저는 `IN`, `OUT`, `IN OUT` 매개변수를 사용합니다. 이 프로시저를 JDBC로 호출하려면 다음과 같이 작성해야 합니다:

```java
try (CallableStatement s = c.prepareCall(
    "{ call my_proc(?, ?, ?, ?, ?, ?) }"
)) {

    // 모든 입력 값 설정
    s.setInt(1, 1); // i1
    s.setInt(2, 2); // io1
    s.setInt(5, 5); // io2
    s.setInt(6, 6); // i2

    // 모든 출력 값을 타입과 함께 등록
    s.registerOutParameter(2, Types.INTEGER); // io1
    s.registerOutParameter(3, Types.INTEGER); // o1
    s.registerOutParameter(4, Types.INTEGER); // o2
    s.registerOutParameter(5, Types.INTEGER); // io2

    s.executeUpdate();

    System.out.println("io1 = " + s.getInt(2));
    System.out.println("o1 = " + s.getInt(3));
    System.out.println("o2 = " + s.getInt(4));
    System.out.println("io2 = " + s.getInt(5));
}
```

이 접근 방식에는 여러 가지 문제가 있습니다:

- 일반적인 매개변수 인덱스 방식은 오류가 발생하기 쉽습니다. 매개변수를 하나 더 추가하면 인덱스가 밀리게 되고, 이를 관리하기가 어렵습니다. 이름 기반 매개변수를 사용할 수도 있지만, 그래도 오타가 발생할 수 있으며, 모든 JDBC 드라이버가 이를 지원하는 것은 아닙니다. 다만 인덱스 기반 매개변수는 모든 드라이버가 지원합니다.
- API에서 `IN`, `IN OUT`, `OUT` 매개변수 간의 명확한 구분이 없습니다. 어떤 매개변수가 어떤 모드인지 *알고 있어야* 합니다. JDBC API는 이 부분에서 도움을 주지 않습니다.
- 또한 각 매개변수의 타입이 무엇인지 알고 이를 정확히 맞춰야 합니다.

다른 주의 사항과 세부 사항도 많지만, 위의 것들이 가장 중요한 사항입니다.

## jOOQ 생성 코드 사용

jOOQ의 코드 생성기는 이 프로시저에 대한 스텁을 생성합니다. 정확히 말하면 2개의 스텁을 생성합니다. 매개변수가 포함된 호출을 모델링하는 클래스와, 프로시저를 단일 메서드 호출로 호출할 수 있게 해주는 편의 메서드입니다. 다음과 같은 모습입니다:

```java
// 생성된 코드
public class MyProc extends AbstractRoutine<java.lang.Void> {

    // [...]
    private static final long serialVersionUID = 1L;

    public void setI1(Number value) {
        setNumber(I1, value);
    }

    public void setIo1(Number value) {
        setNumber(IO1, value);
    }

    public void setIo2(Number value) {
        setNumber(IO2, value);
    }

    public void setI2(Number value) {
        setNumber(I2, value);
    }

    public BigDecimal getIo1() {
        return get(IO1);
    }

    public BigDecimal getO1() {
        return get(O1);
    }

    public BigDecimal getO2() {
        return get(O2);
    }

    public BigDecimal getIo2() {
        return get(IO2);
    }
}
```

Oracle용으로 생성된 코드는 `NUMBER` 타입에 바인딩하기 위해 입력 값에는 `Number`를, 출력 값에는 `BigDecimal`을 사용합니다. 다른 RDBMS는 `INTEGER` 타입을 지원하므로, 여러분의 코드에서 더 자주 사용하는 타입일 수 있습니다. 테이블과 마찬가지로 강제 타입(forced types)을 사용하여 jOOQ 코드 생성기에서 데이터 타입 정의를 재작성할 수 있습니다.

따라서 프로시저를 호출하는 한 가지 방법은 다음과 같습니다:

```java
MyProc call = new MyProc();
call.setI1(1);
call.setIo1(2);
call.setIo2(5);
call.setI2(6);

// 일반적인 jOOQ 설정을 사용합니다. 예를 들어
// Spring Boot 등에서 구성된 설정 등
call.execute(configuration);

System.out.println("io1 = " + call.getIo1());
System.out.println("o1 = " + call.getO1());
System.out.println("o2 = " + call.getO2());
System.out.println("io2 = " + call.getIo2());
```

이것만으로도 꽤 간단하며, 프로시저를 동적으로 호출할 수 있게 해줍니다. 대부분의 경우 jOOQ는 이 프로시저를 한 줄로 호출할 수 있는 편의 메서드도 함께 생성합니다. 생성된 편의 메서드는 다음과 같습니다:

```java
public class Routines {
    // [...]

    public static MyProc myProc(
          Configuration configuration
        , Number i1
        , Number io1
        , Number io2
        , Number i2
    ) {
        MyProc p = new MyProc();
        p.setI1(i1);
        p.setIo1(io1);
        p.setIo2(io2);
        p.setI2(i2);

        p.execute(configuration);
        return p;
    }
}
```

이 메서드가 입력 매개변수의 연결 작업을 대신 해주므로, 다음과 같이 호출할 수 있습니다:

```java
MyProc result = Routines.myProc(configuration, 1, 2, 5, 6);

System.out.println("io1 = " + result.getIo1());
System.out.println("o1 = " + result.getO1());
System.out.println("o2 = " + result.getO2());
System.out.println("io2 = " + result.getIo2());
```

프로시저를 호출하는 이 두 가지 방법은 동등하지만, 첫 번째 접근 방식은 프로시저 정의에서 기본값 매개변수(defaulted parameters)를 사용하는 경우에도 이를 지원합니다.

## 기타 기능

앞선 예제는 저장 프로시저와 함께 이 jOOQ 기능을 사용하는 가장 일반적인 방법을 보여주었습니다. 이 외에도 더 많은 기능이 있으며, 곧 후속 블로그 게시물에서 다음 내용을 포함하여 논의할 예정입니다:

- 기본값 매개변수(Defaulted parameters)
- jOOQ SQL 문에 임베디드하여 사용하는 스칼라 함수
- jOOQ SQL 문에 임베디드하여 사용하는 테이블 값 함수(`PIPELINED` 함수 포함)
- 저장 프로시저에서 반환되는 커서(`REF CURSOR`로 선언된 것과 선언되지 않은 것 모두)
- Oracle PL/SQL 패키지
- Oracle PL/SQL UDT 및 멤버 프로시저
- Oracle PL/SQL `TABLE`, `RECORD` 및 연관 배열 타입
- Oracle AQ 호출 및 메시지 UDT
- Microsoft T-SQL 테이블 값 매개변수

이 모든 것과 그 이상이 jOOQ에서 지원되므로, 더 많은 내용을 기대해 주세요.

## JPublisher에서 마이그레이션하는 경우

예전에 Oracle 사용자들은 저장 프로시저에 바인딩하기 위해 JPublisher를 사용했을 수 있습니다. jOOQ로 마이그레이션할 때 놓치는 것이 거의 없다는 사실을 알게 되면 기쁘실 것입니다! 한번 사용해 보세요.
