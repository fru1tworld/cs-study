# No OFFSET 운동에 참여하라!

> 원문: https://blog.jooq.org/join-the-no-offset-movement/

Use The Index, Luke!의 Markus Winand가 또 해냈다. 그는 SQL 언어의 가장 큰 결함 중 하나에 맞서는 흥미진진한 전투를 시작했다.

우리는 이전에도 이에 대해 블로그에 글을 쓴 적이 있다. OFFSET 페이지네이션은 높은 페이지 번호에 도달하면 엄청나게 느려진다. 게다가 여러분의 데이터베이스가 아직 이것을 제대로 구현하지 못하고 있을 가능성이 높다(그리고 여러분이 직접 만든 에뮬레이션도 아마 잘못되었을 것이다).

KEYSET 페이지네이션을 위한 Markus의 운동에 동참하라. 이 방식은 훨씬 더 빠를 뿐만 아니라 더 직관적이기도 하다. Reddit, Twitter, Facebook 등 많은 인기 웹사이트들이 이미 keyset 페이지네이션을 구현하고 있다. 여러분은 왜 안 하는가?

jOOQ는 합성 SEEK 절을 사용하여 네이티브로 KEYSET 페이지네이션을 구현한 유일한 Java SQL API이다. 다음은 사용 방법이다:

```java
DSL.using(configuration)
   .select(PLAYERS.PLAYER_ID,
           PLAYERS.FIRST_NAME,
           PLAYERS.LAST_NAME,
           PLAYERS.SCORE)
   .from(PLAYERS)
   .where(PLAYERS.GAME_ID.eq(42))
   .orderBy(PLAYERS.SCORE.desc(),
            PLAYERS.PLAYER_ID.asc())
   .seek(949, 15) // 튜플 (949, 15)로 점프한다
   .limit(10)
   .fetch();
```
