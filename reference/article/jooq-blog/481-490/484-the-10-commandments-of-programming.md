# 프로그래밍의 10계명

> 원문: https://blog.jooq.org/the-10-commandments-of-programming/

튜링이 컴퓨트 산에서 내려왔을 때, 그의 손에는 두 개의 증거의 아이패드가 들려 있었다. 그것은 컴파일러의 손가락으로 작성된 아이패드였다. 컴파일러가 그 위에 쓴 내용은 다음과 같다:

1. GOTO를 사용하지 말지어다 - [XKCD 292](https://xkcd.com/292/) 참조

2. TODO를 남기지 말지어다 - [TODO 주석은 해롭다고 여겨진다](https://wiki.c2.com/?TodoCommentsConsideredHarmful) 참조

3. 모든 예외를 한꺼번에 catch 하지 말지어다 - 광범위한 예외 캐치를 피하라

4. `<br>`을 남용하지 말지어다 - HTML 줄바꿈 태그를 코드에서 남용하지 말라

5. `break`나 `continue`를 위해 코드에 레이블을 붙이지 말지어다

6. (6번은 의도적으로 생략됨)

7. getter에서 거짓 증거나 부작용(side effect)을 만들지 말지어다 - getter는 순수 함수여야 한다

8. 중괄호를 소홀히 하지 말지어다 - 항상 중괄호를 명시적으로 사용하라

9. Unsafe를 탐하지 말지어다 - Java의 sun.misc.Unsafe 클래스 사용을 피하라

10. 이웃의 private 필드나 메서드를 탐하지 말지어다 - 리플렉션으로 private 멤버에 접근하지 말라

11. 이름에 교묘한 속임수를 쓰지 말지어다 - 변수명과 메서드명은 명확하고 정직해야 한다

---

![He was not amused](https://geekandpoke.typepad.com/.a/6a00d8341d3df553ef01901ede3d0e970b-pi)

*"그는 기뻐하지 않았다" - Geek and Poke*

---

면책 조항: 튜링도 컴파일러도 실제로 이런 말을 하지 않았을 수도 있습니다.
