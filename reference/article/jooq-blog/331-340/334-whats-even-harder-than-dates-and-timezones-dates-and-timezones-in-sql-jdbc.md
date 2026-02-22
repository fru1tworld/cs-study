# 날짜와 시간대보다 더 어려운 것? SQL/JDBC에서의 날짜와 시간대!

> 원문: https://blog.jooq.org/whats-even-harder-than-dates-and-timezones-dates-and-timezones-in-sql-jdbc/

(알림: 이 글은 꽤 오래전에 작성되었습니다. jOOQ는 현재 JSR 310 데이터 타입을 지원합니다)

최근 jOOQ 메일링 리스트에서 jOOQ가 `TIMESTAMP WITH TIME ZONE` 데이터 타입을 기본적으로 지원하지 않는 것에 대한 흥미로운 논의가 있었습니다. 날짜, 시간, 그리고 시간대가 쉽다고 말한 사람은 아무도 없었습니다!

여기 재미있는 글이 있는데, 읽어보시길 권합니다: "[프로그래머가 시간에 대해 믿는 거짓말들](https://infiniteundo.com/post/25326999628/falsehoods-programmers-believe-about-time)"

그리고 그것만으로 부족하다면, 이것도 읽어보세요: "[프로그래머가 시간에 대해 믿는 더 많은 거짓말들](https://infiniteundo.com/post/25509354022/more-falsehoods-programmers-believe-about-time)"

개인적으로 프로그래머들이 "Unix 시간은 1970년 1월 1일 이후의 초 수이다"라고 잘못 믿는다는 부분이 마음에 듭니다... unix 시간은 윤초를 표현할 방법이 없거든요 ;)

## JDBC로 돌아가서

Jaybird 개발자(Firebird JDBC 드라이버)인 Mark Rotteveel의 흥미로운 Stack Overflow 답변이 있습니다: "[java.sql.Timestamp는 시간대에 특정한가요?](https://stackoverflow.com/questions/14070572/is-java-sql-timestamp-timezone-specific)"

Mark의 설명은 다음과 같이 관찰할 수 있습니다(여기서는 PostgreSQL을 사용합니다):

```java
Connection c = getConnection();
// UTC 캘린더 인스턴스 생성
Calendar utc = Calendar.getInstance(
    TimeZone.getTimeZone("UTC"));

try (PreparedStatement ps = c.prepareStatement(
    "select"
  + "  ?::timestamp,"
  + "  ?::timestamp,"
  + "  ?::timestamp with time zone,"
  + "  ?::timestamp with time zone"
)) {

    ps.setTimestamp(1, new Timestamp(0));
    ps.setTimestamp(2, new Timestamp(0), utc);
    ps.setTimestamp(3, new Timestamp(0));
    ps.setTimestamp(4, new Timestamp(0), utc);

    try (ResultSet rs = ps.executeQuery()) {
        rs.next();

        System.out.println(rs.getTimestamp(1)
                 + " / " + rs.getTimestamp(1).getTime());
        System.out.println(rs.getTimestamp(2, utc)
                 + " / " + rs.getTimestamp(2, utc).getTime());
        System.out.println(rs.getTimestamp(3)
                 + " / " + rs.getTimestamp(3).getTime());
        System.out.println(rs.getTimestamp(4, utc)
                 + " / " + rs.getTimestamp(4, utc).getTime());
    }
}
```

위 프로그램은 Java와 DB에서 시간대를 사용하거나 사용하지 않는 모든 조합을 사용하며, 출력은 항상 동일합니다:

```
1970-01-01 01:00:00.0 / 0
1970-01-01 01:00:00.0 / 0
1970-01-01 01:00:00.0 / 0
1970-01-01 01:00:00.0 / 0
```

보시다시피, 각 경우에 UTC 타임스탬프 0이 데이터베이스에 올바르게 저장되고 검색되었습니다. 제 로케일은 스위스이므로 CET / CEST이며, Epoch 시점에 UTC+1이었고, 이것이 `Timestamp.toString()`에서 출력되는 내용입니다.

타임스탬프 리터럴을 SQL과/또는 Java에서 사용할 때 흥미로운 일이 발생합니다. 바인드 변수를 다음과 같이 교체하면:

```java
// 거의 Epoch에 가까운 타임스탬프 생성
Timestamp almostEpoch = Timestamp.valueOf("1970-01-01 00:00:00");

ps.setTimestamp(1, almostEpoch);
ps.setTimestamp(2, almostEpoch, utc);
ps.setTimestamp(3, almostEpoch);
ps.setTimestamp(4, almostEpoch, utc);
```

제 머신에서 CET / CEST로 실행하면 이런 결과를 얻습니다:

```
1970-01-01 00:00:00.0 / -3600000
1970-01-01 00:00:00.0 / -3600000
1970-01-01 00:00:00.0 / -3600000
1970-01-01 00:00:00.0 / -3600000
```

즉, Epoch가 아니라 처음에 서버로 보낸 타임스탬프 리터럴입니다. 바인딩/페칭의 네 가지 조합이 여전히 항상 동일한 타임스탬프를 생성한다는 점에 주목하세요.

데이터베이스에 쓰는 세션이 데이터베이스에서 가져오는 세션과 다른 시간대를 사용하면 어떻게 되는지 살펴보겠습니다(PST에 있다고 가정하고, 저는 다시 CET 또는 UTC를 사용합니다). 이 프로그램을 실행합니다:

```java
// UTC 캘린더 인스턴스 생성
Calendar utc = Calendar.getInstance(
    TimeZone.getTimeZone("UTC"));

// PST 캘린더 인스턴스 생성
Calendar pst = Calendar.getInstance(
    TimeZone.getTimeZone("PST"));

try (PreparedStatement ps = c.prepareStatement(
    "select"
  + "  ?::timestamp,"
  + "  ?::timestamp,"
  + "  ?::timestamp with time zone,"
  + "  ?::timestamp with time zone"
)) {

    ps.setTimestamp(1, new Timestamp(0), pst);
    ps.setTimestamp(2, new Timestamp(0), pst);
    ps.setTimestamp(3, new Timestamp(0), pst);
    ps.setTimestamp(4, new Timestamp(0), pst);

    try (ResultSet rs = ps.executeQuery()) {
        rs.next();

        System.out.println(rs.getTimestamp(1)
                 + " / " + rs.getTimestamp(1).getTime());
        System.out.println(rs.getTimestamp(2, utc)
                 + " / " + rs.getTimestamp(2, utc).getTime());
        System.out.println(rs.getTimestamp(3)
                 + " / " + rs.getTimestamp(3).getTime());
        System.out.println(rs.getTimestamp(4, utc)
                 + " / " + rs.getTimestamp(4, utc).getTime());
    }
}
```

이 프로그램은 다음 출력을 생성합니다:

```
1969-12-31 16:00:00.0 / -32400000
1969-12-31 17:00:00.0 / -28800000
1970-01-01 01:00:00.0 / 0
1970-01-01 01:00:00.0 / 0
```

첫 번째 타임스탬프는 PST로 저장된 Epoch(16:00)였고, 그 다음 시간대 정보가 데이터베이스에 의해 제거되어 Epoch가 Epoch 시점의 현지 시간(-28800초 / -8시간)으로 변환되었으며, 이것이 실제로 저장된 정보입니다. 이제 제 시간대 CET에서 이 시간을 가져올 때, 여전히 현지 시간(16:00)을 얻고 싶습니다. 하지만 제 시간대에서 이것은 더 이상 -28800초가 아니라 -32400초(-9시간)입니다.

충분히 이상하지 않나요? 저장된 현지 시간(16:00)을 가져오지만 UTC로 강제로 가져오면 반대 방향으로 진행됩니다. 이렇게 하면 원래 PST로 저장한 타임스탬프(-28800초)가 생성됩니다. 하지만 이 타임스탬프(-28800초)를 제 시간대 CET로 출력하면 이제 17:00이 됩니다.

데이터베이스에서 TIMESTAMP WITH TIME ZONE 데이터 타입을 사용하면 시간대가 유지되며(PST), Timestamp 값을 가져올 때 CET를 사용하든 UTC를 사용하든 관계없이 여전히 Epoch를 얻게 되며, 이는 안전하게 데이터베이스에 저장되어 CET로 01:00으로 출력됩니다.

휴.

## 요약 (TL;DR):

(알림: 이 글은 꽤 오래전에 작성되었습니다. jOOQ는 현재 JSR 310 데이터 타입을 지원합니다)

jOOQ를 사용할 때 정확한 UTC 타임스탬프가 중요하다면 TIMESTAMP WITH TIMEZONE을 사용하세요. 하지만 jOOQ가 현재 해당 데이터 타입을 지원하지 않기 때문에 자체 데이터 타입 바인딩을 구현해야 합니다. 자체 데이터 타입 바인딩을 사용하면 Java 8의 time API도 사용할 수 있으며, 이는 java.sql.Timestamp + 지저분한 Calendar보다 이러한 다양한 타입을 더 잘 표현합니다.

현지 시간이 중요하거나 시간대를 넘나들며 운영하지 않는다면 TIMESTAMP와 jOOQ의 `Field<Timestamp>`를 사용해도 괜찮습니다.

저처럼 단일 시간대를 가진 매우 작은 나라에서 운영하여 대부분의 로컬 소프트웨어가 이 문제에 부딪히지 않는다면 행운이네요.
