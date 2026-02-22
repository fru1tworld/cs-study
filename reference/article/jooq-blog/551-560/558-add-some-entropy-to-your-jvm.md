# JVM에 약간의 엔트로피를 추가하라

> 원문: https://blog.jooq.org/add-some-entropy-to-your-jvm/

진정한 난수를 생성하는 것은 어렵습니다. 유명한 XKCD 만화처럼 "공정한 주사위 굴림으로 선택됨. 무작위성 보장됨"이라고 말하며 항상 4를 반환하는 것은 분명히 좋은 해결책이 아닙니다. `java.math.Random.nextInt()`가 상수 값을 반환하도록 수정하는 것도 마찬가지로 터무니없는 방법입니다.

그래서 여기 더 나은 아이디어가 있습니다: JVM이 시작될 때 `java.lang.Integer.IntegerCache`를 다시 작성하는 것입니다!

```java
import java.lang.reflect.Field;
import java.util.Random;

public class Entropy {
  public static void main(String[] args)
  throws Exception {
    // 리플렉션을 통해 IntegerCache를 추출
    Class<?> clazz = Class.forName(
      "java.lang.Integer$IntegerCache");
    Field field = clazz.getDeclaredField("cache");
    field.setAccessible(true);
    Integer[] cache = (Integer[]) field.get(clazz);

    // Integer 캐시를 다시 작성
    for (int i = 0; i < cache.length; i++) {
      cache[i] = new Integer(
        new Random().nextInt(cache.length));
    }

    // 무작위성 증명
    for (int i = 0; i < 10; i++) {
      System.out.println((Integer) i);
    }
  }
}
```

위 프로그램의 출력은 다음과 같습니다:

```
92
221
45
48
236
183
39
193
33
84
```

물론, JVM 파라미터 `-XX:AutoBoxCacheMax=<size>`를 사용하여 IntegerCache를 확장하면 더 많은 엔트로피를 얻을 수 있습니다.

면책 조항: 이 코드는 Apache 2.0 라이선스에 따라 제공됩니다. 사용에 따른 모든 책임은 사용자에게 있습니다.
