# JDBC 서버 왕복 비용

> 원문: https://blog.jooq.org/the-cost-of-jdbc-server-roundtrips/

"JDBC에서 Oracle의 DBMS_OUTPUT.GET_LINES를 어떻게 호출하나요?"라는 Stack Overflow 질문에 영감을 받아 이 글을 작성했습니다.

DBMS_OUTPUT.GET_LINES(복수형)는 서버 출력의 대량 데이터를 배열로 가져올 수 있게 해주는 반면, DBMS_OUTPUT.GET_LINE(단수형)은 서버 출력의 한 줄만 문자열로 가져옵니다.

위 질문에서 제 관심을 끈 것은 성능 측면이었습니다. "대량으로 데이터를 가져오라"는 것은 데이터베이스 작업을 할 때 당연히 따라야 할 기본 원칙입니다. 하지만 숫자를 확인해 본 적이 있으신가요?

## 간단한 벤치마크

Stack Overflow 답변에 게시한 다음 벤치마크를 확인해 보세요.

```java
int max = 50;
long[] getLines = new long[max];
long[] getLine = new long[max];

try (Connection c = DriverManager.getConnection(url, properties);
    Statement s = c.createStatement()) {

    for (int warmup = 0; warmup < 2; warmup++) {
        for (int i = 0; i < max; i++) {
            s.executeUpdate("begin dbms_output.enable(); end;");
            String sql =
                "begin "
              + "for i in 1 .. 100 loop "
              + "dbms_output.put_line('Message ' || i); "
              + "end loop; "
              + "end;";

            // GET_LINES 테스트
            long t1 = System.nanoTime();
            logGetLines(c, 100, () -> s.executeUpdate(sql));
            long t2 = System.nanoTime();

            // GET_LINE 테스트
            logGetLine(c, 100, () -> s.executeUpdate(sql));
            long t3 = System.nanoTime();
            s.executeUpdate("begin dbms_output.disable(); end;");

            if (warmup > 0) {
                getLines[i] = t2 - t1;
                getLine[i] = t3 - t2;
            }
        }
    }
}

System.out.println(LongStream.of(getLines).summaryStatistics());
System.out.println(LongStream.of(getLine).summaryStatistics());
```

이 벤치마크는 다음과 같이 작동합니다:

- 첫 번째 반복은 워밍업이므로 결과에서 제외합니다
- 50회의 테스트 반복을 수행합니다
- 반복당 100개의 메시지를 생성합니다 (총 5,000개)
- 단일 GET_LINES 호출과 100번의 GET_LINE 호출을 비교합니다

제 데스크톱에서 실행한 결과(도커 컨테이너에서 Oracle 18c XE 실행, 네트워크 없음):

```
GET_LINES (대량):
{count=50, sum=69120455, min=1067521, average=1382409.100000, max=2454614}

GET_LINE (행 단위):
{count=50, sum=2088201423, min=33737827, average=41764028.460000, max=64498375}
```

제 데스크톱에서 5,000개의 메시지를 가져올 때 GET_LINES로 한 번 가져오는 것이 약 30배 더 빠릅니다!

네트워크가 있는 프로덕션 환경이라면 상황이 얼마나 더 나빠지는지 상상해 보세요.

## PL/SQL만으로 벤치마크

저는 데이터베이스 내부에서 순수 PL/SQL 벤치마크도 실행했습니다:

```sql
FOR r IN 1..5 LOOP
  v_ts := SYSTIMESTAMP;

  FOR i IN 1..v_repeat LOOP
    m();

    v_i := v_max;
    dbms_output.get_lines(v_array, v_i);
  END LOOP;

  INSERT INTO results VALUES (1, (SYSTIMESTAMP - v_ts));
  v_ts := SYSTIMESTAMP;

  FOR i IN 1..v_repeat LOOP
    m();

    FOR j IN 1 .. v_max LOOP
      dbms_output.get_line(v_string, v_i);
    END LOOP;
  END LOOP;

  INSERT INTO results VALUES (2, (SYSTIMESTAMP - v_ts));
END LOOP;
```

헬퍼 프로시저:

```sql
PROCEDURE m IS BEGIN
  FOR i IN 1 .. v_max LOOP
    dbms_output.put_line('Message ' || i);
  END LOOP;
END m;
```

결과는 다음과 같습니다:

```
stmt    sum     avg      min     max
1       0.0609  0.01218  0.0073  0.0303
2       0.0333  0.00666  0.0063  0.007
```

흥미롭게도 PL/SQL 내에서 실행할 때는 GET_LINE이 실제로 GET_LINES보다 2배 더 빠릅니다! 하지만 여기서 잘못된 결론을 내리지 마세요. 이는 다음 중 하나일 수 있습니다:

- GET_LINES가 PGA에 있는 원본 라인의 추가 배열 복사본을 할당하는데, 이 비용이 높습니다
- GET_LINE이 벤치마크에서 결과를 실제로 소비하지 않았기 때문에 어떤 최적화의 혜택을 받았습니다

중요한 것은 JDBC 벤치마크의 결과입니다. JDBC 벤치마크에서 GET_LINE이 GET_LINES보다 30배 더 느렸다는 것입니다. 왜 그럴까요?

## 오버헤드의 근본 원인

이는 분명히 GET_LINE 프로시저 자체 때문이 아니라(위에서 보듯이 실제로 더 빠릅니다), 외부 호출에서 발생하는 오버헤드 때문입니다. GET_LINE을 100번 호출하면 100번의 JDBC 오버헤드가 발생하지만, GET_LINES를 한 번 호출하면 한 번의 오버헤드만 발생합니다.

Toon Koppelaars(Oracle)가 "NoPLSQL and Thick Database Approaches"라는 발표에서 이 내용을 설명했습니다. 그는 플레임 그래프를 통해 오버헤드를 보여주었는데, Oracle 데이터베이스 외부에서 PL/SQL을 호출할 때 발생하는 오버헤드에 대한 훌륭한 시각화를 제공합니다.

이 이미지에서 실제 로직은 데이터베이스 내부에서 상대적으로 저렴하지만, 데이터베이스 외부에서 데이터베이스 로직을 호출할 때 오버헤드가 상당하다는 것을 알 수 있습니다.

다음을 거쳐야 합니다:

- JVM 오버헤드
- JDBC 로직
- 네트워크 오버헤드
- Oracle 데이터베이스의 다양한 '외부' 레이어
- SQL 및/또는 PL/SQL 엔진에 접근하는 API 레이어
- 마지막으로 실행(가장 작은 부분)

당연히, 단일 JDBC 호출의 비용은 실제 로직의 1000배 이상일 수 있습니다. 그리고 이것은 JDBC에만 해당되는 것이 아닙니다. 네트워크를 통해 객체에 접근하는 것(모든 네트워크)은 비용이 많이 듭니다. 이 경우 로컬에서 실행해도 마찬가지입니다. 따라서 가능한 한 접근 호출을 최소화해야 합니다.

이것이 바로 "코드로서의 SQL" 이면의 아이디어입니다.

## 이 문제를 어떻게 해결할까요?

쉬운 방법이 있고 어려운 방법이 있습니다:

### 어려운 방법

가능하다면, 즉 JDBC 대신 C/OCI를 사용해 데이터베이스에 접근하는 경우, 호출 오버헤드를 줄이기 위해 API를 변경할 수 있습니다. 물론 자신만의 트레이드오프가 있으며, 이를 위해 전체 기술 스택을 다시 작성해야 할 수도 있습니다.

### 쉬운 방법

데이터 수집 로직 일부를 데이터베이스로 옮기고 데이터를 대량으로 가져오세요. 이것이 항상 가능한 것은 아니지만, 제 경험상 상당히 많은 경우에 가능하며, 사람들은 이 옵션을 너무 자주 무시합니다.

많은 사람들이 저장 프로시저에 대해 "종교적"이 되어 결코 사용하지 않습니다. 하지만 때로는 SQL만 필요할 때도 있습니다!

## 핵심 원칙

> "클라이언트에서 루프를 돌면서 같은 서버에서 개별 항목을 가져오고 있다면, 잘못하고 있는 것입니다. 루프를 서버로 옮기세요."

예를 들어, 다음과 같이 하는 대신:

```java
for (int id : ids)
    fetch(id);
```

다음과 같이 하세요:

```java
fetchAll(ids);
```

이것은 대부분의 사람들에게 명백해 보일 수 있지만, 실제로는 이런 실수가 놀라울 정도로 자주 발생합니다.

## ORM과 N+1 문제

많은 ORM 관련 성능 문제가 지나치게 많은 데이터베이스 호출에서 비롯됩니다. 특히 루프 내에서 지연 로딩이 쿼리를 트리거하는 "N+1 select" 문제가 대표적입니다. 생성되는 SQL을 인식하지 못하는 개발자들은 성능 저하를 겪게 됩니다. 해결책은 클라이언트 측 반복을 제거하도록 쿼리를 재구성하는 것입니다.

제가 이전에 트윗한 내용입니다:

> "데이터가 적고 로직이 매우 복잡할 때는 데이터를 로직으로 옮기는 것이 더 나을 때가 많습니다. 데이터가 많을 때는 (단순하든 복잡하든) 로직을 데이터로 옮기는 것이 더 나을 때가 많습니다."

그리고 이것은 SQL 및/또는 저장 프로시저로 수행됩니다.

## HTTP API에도 적용됨

이 원칙은 RDBMS를 넘어 모든 분산 시스템으로 확장됩니다. HTTP API에서도 배치 작업은 여러 논리적 요청을 단일 물리적 호출로 집계해야 합니다. 이 기본 규칙은 어디에나 적용됩니다.

## 업데이트 및 주의사항

Reddit의 `/u/cogman10`이 중요한 비판을 제기했습니다:

### 대량 배치의 위험:
- 데이터베이스 경합 증가
- Oracle과 같은 MVCC 시스템에서 UNDO/REDO 압력
- 영향받는 데이터를 읽는 다른 세션에서 지연 발생

### HTTP 특정 고려사항:
- 대량 배치는 세분화된 요청보다 캐시하기 어려움
- 인증 컨텍스트가 유지된다고 가정
- 유사한 리소스 작업에 최적 (개별 조회보다 대량 ID 조회 선호)

### 저자의 응답:
컨텍스트에 따라 최적화가 달라집니다. 이것은 보편적인 처방이 아닙니다. 대부분의 경우 대량 가져오기가 더 낫지만, 쓰기 작업이 많은 워크로드와 특정 시나리오에서는 추가 고려가 필요합니다.

## 결론

요점은 간단합니다. 네트워크를 통해 또는 프로세스 경계를 넘어 통신할 때, 왕복 횟수를 최소화하세요. 가능한 한 대량으로 데이터를 가져오거나 보내세요. 이 간단한 원칙은 종종 간과되지만, 일관되게 극적인 성능 향상을 제공합니다. 개발자들은 로직이 데이터베이스 커널에 도달하면 실행이 놀라울 정도로 효율적이라는 것을 자주 간과합니다. 단일 쿼리에 복잡성을 추가하는 것이 애플리케이션 계층에서 여러 쿼리를 실행하는 것보다 훨씬 적은 성능 저하를 야기합니다.

마지막으로 다시 한번 핵심 메시지를 강조합니다:

> "클라이언트에서 루프를 돌면서 같은 서버에서 개별 항목을 가져오고 있다면, 잘못하고 있는 것입니다. 루프를 서버로 옮기세요."
