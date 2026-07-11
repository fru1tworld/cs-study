# Typeclassopedia (1/3): Functor, Applicative, Monad

> 원문: [Typeclassopedia](https://wiki.haskell.org/Typeclassopedia)
> 저자: Brent Yorgey | 번역일: 2026-07-10

전체 목차:

- **Part 1 (이 문서)**: 초록, 서론, Functor, Applicative, Monad
- [Part 2](014-typeclassopedia-part2.md): MonadFail, 모나드 트랜스포머, MonadFix, Semigroup, Monoid, 실패와 선택(Alternative, MonadPlus, ArrowPlus), Foldable, Traversable
- [Part 3](014-typeclassopedia-part3.md): Bifunctor, Category, Arrow, Comonad, 감사의 말, 저자 소개, 콜로폰

*저자: [Brent Yorgey](https://byorgey.wordpress.com/), byorgey@gmail.com*

*2009년 3월 12일 [the Monad.Reader](http://themonadreader.wordpress.com/) [13호](http://www.haskell.org/wikiupload/8/85/TMR-Issue13.pdf)에 처음 게재되었다. 2011년 11월 Geheimdienst가 Haskell 위키로 옮겼다.*

*이 위키 버전이 이제 Typeclassopedia의 공식 버전이며, Monad.Reader에 실린 버전을 대체한다. 직접 편집하거나 토론 페이지에 의견, 제안, 질문을 남겨 문서를 갱신하고 확장하는 데 힘을 보태 달라.*

## 초록

Haskell 표준 라이브러리에는 대수학이나 범주론에 뿌리를 둔 타입 클래스가 여럿 있다. 능숙한 Haskell 해커가 되려면 이들 모두에 익숙해져야 하는데, 그 과정에서 보통 산더미 같은 튜토리얼, 블로그 글, 메일링 리스트 아카이브, IRC 로그를 뒤지게 된다.

이 문서의 목표는 Haskell 표준 타입 클래스를 탄탄하게 이해하고 싶은 학습자를 위한 출발점이 되는 것이다. 각 타입 클래스의 핵심을 예제와 해설, 그리고 더 읽을거리에 대한 폭넓은 참고 문헌과 함께 소개한다.

## 서론

다음과 같은 생각을 해 본 적이 있는가?

* 도대체 모노이드(monoid)가 뭐고, 모나드(mon<u>a</u>d)와는 뭐가 다른 거지?

* [Parsec](https://wiki.haskell.org/Parsec)을 do 표기법으로 쓰는 법을 겨우 익혔더니, 누가 `Applicative`라는 걸 대신 쓰라고 한다. 음, 뭐라고?

* #haskell IRC 채널에서 누군가 `(***)`를 썼길래 Lambdabot에게 타입을 물어봤더니, 한 줄에 다 들어가지도 않는 무시무시한 헛소리를 출력했다! 그러더니 누가 `fmap fmap fmap`을 썼고 내 머리는 폭발했다.

* 정말 복잡하다고 생각한 일을 어떻게 하느냐고 물었더니, 사람들이 `zip.ap fmap.(id &&& wtf)` 같은 걸 타이핑하기 시작했고, 무서운 건 그게 실제로 동작했다는 거다! 아무튼 그 사람들은 분명 로봇일 거다. 저런 걸 2초 만에 머릿속에서 떠올릴 수 있는 사람은 없을 테니까.

그렇다면 더 볼 것 없다! 당신도 최고수들처럼 간결하고 우아하며 관용적인 Haskell 코드를 읽고 쓸 수 있다.

전문가 Haskell 해커의 지혜에는 두 가지 열쇠가 있다.

1. 타입을 이해하라.
2. 각 타입 클래스에 대한 깊은 직관과, 다른 타입 클래스와의 관계에 대한 이해를, 많은 예제에 대한 익숙함으로 뒷받침해 얻어라.

첫 번째의 중요성은 아무리 강조해도 지나치지 않다. 타입 시그니처를 끈기 있게 들여다보는 학생은 많은 심오한 비밀을 발견하게 된다. 반대로 자기 코드의 타입을 모르는 사람은 영원한 불확실성에 시달릴 운명이다. "흠, 컴파일이 안 되네... `fmap`을 여기 하나 넣어 볼까... 아니네, 어디 보자... `(.)`을 어딘가 하나 더 넣어야 하나? ... 음..."

두 번째 열쇠—예제로 뒷받침되는 깊은 직관—도 중요하지만 얻기가 훨씬 어렵다. 이 문서의 주된 목표는 그런 직관을 얻는 길에 당신을 올려놓는 것이다. 다만—

> *Haskell로 가는 왕도는 없다. —유클리드 (Haskell을 알았다면 아마 이렇게 말했을 것이다.)*

좋은 직관은 [올바른 비유를 배우는 데서가 아니라](http://byorgey.wordpress.com/2009/01/12/abstraction-intuition-and-the-monad-tutorial-fallacy/) 고된 노력에서 나오므로, 이 문서는 출발점일 수밖에 없다. 이 문서를 전부 읽고 이해한 사람에게도 여전히 험난한 여정이 남아 있다—하지만 때로는 좋은 출발점이 큰 차이를 만든다.

이 문서는 Haskell 튜토리얼이 아니라는 점을 밝혀 둔다. 독자가 표준 [`Prelude`](https://hackage.haskell.org/package/base/docs/Prelude.html), 타입 시스템, 데이터 타입, 타입 클래스 등 Haskell의 기초에 이미 익숙하다고 가정한다.

우리가 다룰 타입 클래스들과 그 상호 관계는 다음과 같다([이 그래프의 소스 코드는 여기서 찾을 수 있다](https://wiki.haskell.org/index.php?title=File:Dependencies.txt)):

![Typeclassopedia 다이어그램](https://wiki.haskell.org/wikiupload/d/df/Typeclassopedia-diagram.png)

> **참고**: `Apply`는 [`semigroupoids` 패키지](http://hackage.haskell.org/package/semigroupoids)에, `Comonad`는 [`comonad` 패키지](http://hackage.haskell.org/package/comonad)에 있다.

* 실선 화살표는 일반적인 것에서 구체적인 것을 가리킨다. 즉 `Foo`에서 `Bar`로 화살표가 있으면, 모든 `Bar`는 `Foo`이다(또는 `Foo`여야 하거나, `Foo`로 만들 수 있다)는 뜻이다.
* 점선은 그 밖의 다른 종류의 관계를 나타낸다.
* `Monad`와 `ArrowApply`는 동등하다.
* `Apply`와 `Comonad`는 (아직?) 표준 Haskell 라이브러리에 실제로 들어 있지 않아 회색으로 표시했다.

시작하기 전에 한 가지만 더 짚어 두자. "type class"의 원래 표기는 두 단어였다. 예를 들어 [Haskell 2010 언어 보고서](http://www.haskell.org/onlinereport/haskell2010/), [Type classes in Haskell](https://dl.acm.org/doi/pdf/10.1145/227699.227700)이나 [Type classes: an exploration of the design space](https://web.archive.org/web/20220920022231/https://citeseer.ist.psu.edu/viewdoc/download?doi=10.1.1.1085.8703&rep=rep1&type=pdf) 같은 타입 클래스에 관한 초기 논문들, 그리고 [Hudak 등의 Haskell 역사 논문](https://web.archive.org/web/20150424040536/https://haskell.cs.yale.edu/wp-content/uploads/2011/02/history.pdf)이 그 증거다. 그런데 많이 쓰이는 두 단어짜리 표현이 흔히 그렇듯, 한 단어("typeclass")로 붙여 쓰거나 드물게 하이픈으로 연결해("type-class") 쓰는 표기도 나타나기 시작했다. 규범주의자의 모자를 쓰면 나는 "type class"를 선호하지만, 기술주의자의 모자로 바꿔 쓰고 나면 내가 어찌할 수 있는 일이 별로 없다는 것도 안다.

[Instances of List and Maybe](https://wiki.haskell.org/Instances_of_List_and_Maybe) 문서는 List와 Maybe를 사용한 간단한 예제로 이 타입 클래스들을 설명한다. 이제 모든 타입 클래스 가운데 가장 단순한 `Functor`부터 시작하자.

## Functor

`Functor` 클래스([haddock](https://hackage.haskell.org/package/base/docs/Prelude.html#t:Functor))는 Haskell 라이브러리에서 가장 기본적이고 어디에나 있는 타입 클래스다. 단순한 직관은, `Functor`가 어떤 종류의 "컨테이너"와 함께, 컨테이너 안의 모든 요소에 함수를 균일하게 적용하는 능력을 나타낸다는 것이다. 예를 들어 리스트는 요소들의 컨테이너이고, `map`을 사용해 리스트의 모든 요소에 함수를 적용할 수 있다. 또 다른 예로 이진 트리도 요소들의 컨테이너이며, 트리의 모든 요소에 함수를 재귀적으로 적용하는 방법을 떠올리는 것은 어렵지 않다.

또 다른 직관은 `Functor`가 어떤 종류의 "계산 문맥(computational context)"을 나타낸다는 것이다. 이 직관이 대체로 더 유용하지만, 바로 그 일반성 때문에 설명하기가 더 어렵다. 뒤에 나올 몇 가지 예제가 문맥으로서의 `Functor`라는 관점을 명확히 하는 데 도움이 될 것이다.

그러나 결국 `Functor`는 그저 정의된 대로의 것일 뿐이다. 위의 두 직관 어느 쪽에도 딱 들어맞지 않는 `Functor` 인스턴스 예도 분명 많다. 현명한 학생이라면 특정 비유에 지나치게 기대지 말고 정의와 예제에 집중할 것이다. 직관은 시간이 지나면 저절로 찾아온다.

### 정의

`Functor`의 타입 클래스 선언은 다음과 같다.

```haskell
class Functor f where
  fmap :: (a -> b) -> f a -> f b

  (<$) :: a        -> f b -> f a
  (<$) = fmap . const
```

`Functor`는 `Prelude`에서 익스포트되므로, 사용하는 데 특별한 임포트가 필요 없다. `(<$)` 연산자는 편의를 위해 제공되며 `fmap`을 이용한 기본 구현이 있다. 클래스에 포함된 이유는 `Functor` 인스턴스가 기본 구현보다 효율적인 구현을 제공할 기회를 주기 위해서일 뿐이다. 따라서 `Functor`를 이해하려면 사실 `fmap`을 이해해야 한다.

먼저, `fmap`의 타입 시그니처에 있는 `f a`와 `f b`는 `f`가 `Int` 같은 구체적인 타입이 아니라는 것을 알려 준다. `f`는 다른 타입을 매개변수로 받는 일종의 *타입 함수*다. 더 정확히 말하면, `f`의 *카인드(kind)*는 `* -> *`여야 한다. 예를 들어 `Maybe`가 카인드 `* -> *`인 타입이다. `Maybe`는 그 자체로는 구체적인 타입이 아니고(즉 타입이 `Maybe`인 값은 없다), `Maybe Integer`처럼 다른 타입을 매개변수로 요구한다. 그래서 `instance Functor Integer`는 말이 안 되지만, `instance Functor Maybe`는 말이 된다.

이제 `fmap`의 타입을 보자. `a`에서 `b`로 가는 임의의 함수와 타입 `f a`의 값을 받아 타입 `f b`의 값을 내놓는다. 컨테이너 관점에서 보면, `fmap`은 컨테이너의 구조를 바꾸지 않으면서 각 요소에 함수를 적용하겠다는 의도다. 문맥 관점에서 보면, `fmap`은 값의 문맥을 바꾸지 않으면서 값에 함수를 적용하겠다는 의도다. 구체적인 예 몇 가지를 살펴보자.

마지막으로 `(<$)`도 이해할 수 있다. 컨테이너/문맥 안의 값들에 함수를 적용하는 대신, 그 값들을 주어진 값으로 그냥 교체한다. 이는 상수 함수를 적용하는 것과 같으므로, `(<$)`는 `fmap`으로 구현할 수 있다.

### 인스턴스

> **참고**: Haskell에서 `[]`는 두 가지 의미가 있다는 점을 기억하자. 빈 리스트를 나타낼 수도 있고, 여기서처럼 리스트 타입 생성자("list-of"라고 읽는다)를 나타낼 수도 있다. 즉 타입 `[a]`(list-of-`a`)는 `[] a`로도 쓸 수 있다.

> **참고**: 왜 별도의 `map` 함수가 필요한지 궁금할 수 있다. 리스트 전용인 지금의 `map`을 없애고 `fmap`을 `map`으로 이름을 바꾸면 안 될까? 좋은 질문이다. 흔한 논거는, Haskell을 이제 막 배우는 사람이 `map`을 잘못 사용했을 때 `Functor`에 관한 에러보다는 리스트에 관한 에러를 보는 편이 훨씬 낫다는 것이다.

앞서 말했듯 리스트 생성자 `[]`는 펑터(functor)다. 표준 리스트 함수 `map`으로 리스트의 각 요소에 함수를 적용할 수 있다. `Maybe` 타입 생성자도 펑터이며, 요소를 하나 담을 수도 있는 컨테이너를 나타낸다. 함수 `fmap g`는 `Nothing`에는 아무 효과가 없고(`g`를 적용할 요소가 없으므로), `Just` 안의 단일 요소에는 단순히 `g`를 적용한다. 한편 문맥 해석에서 보면, 리스트 펑터는 비결정적 선택의 문맥을 나타낸다. 즉 리스트는 여러 가능성(리스트의 요소들) 가운데 비결정적으로 선택된 하나의 값을 나타낸다고 생각할 수 있다. 마찬가지로 `Maybe` 펑터는 실패할 수 있는 문맥을 나타낸다. 이 인스턴스들은 다음과 같다.

```haskell
instance Functor [] where
  fmap :: (a -> b) -> [a] -> [b]
  fmap _ []     = []
  fmap g (x:xs) = g x : fmap g xs
  -- 또는 그냥 fmap = map 이라고 해도 된다

instance Functor Maybe where
  fmap :: (a -> b) -> Maybe a -> Maybe b
  fmap _ Nothing  = Nothing
  fmap g (Just a) = Just (g a)
```

여담이지만, 관용적인 Haskell 코드에서는 문자 `f`가 임의의 `Functor`와 임의의 함수를 둘 다 나타내는 데 자주 쓰인다. 이 문서에서 `f`는 오직 `Functor`만을 나타내고, `g`나 `h`는 항상 함수를 나타내지만, 혼동의 여지가 있다는 점은 알아 두어야 한다. 실전에서는 `f`가 타입의 일부인지 코드의 일부인지를 보면 무엇을 가리키는지 문맥으로 항상 분명히 알 수 있다.

표준 라이브러리에는 다른 `Functor` 인스턴스들도 있다.

* `Either e`는 `Functor`의 인스턴스다. `Either e a`는 타입 `a`의 값이나 타입 `e`의 값(흔히 어떤 에러 상황을 나타낸다) 중 하나를 담을 수 있는 컨테이너를 나타낸다. 실패 가능성을 나타낸다는 점에서 `Maybe`와 비슷하지만, 실패에 관한 추가 정보를 담을 수 있다.

* `((,) e)`는 실제 담고 있는 값과 함께 타입 `e`의 "주석(annotation)"을 담는 컨테이너를 나타낸다. `(1+)` 같은 연산자 섹션에 빗대어 `(e,)`로 쓰면 더 명확하겠지만, 그 문법은 타입에서는 허용되지 않는다(`TupleSections` 확장을 켜면 표현식에서는 허용된다). 그래도 머릿속으로는 얼마든지 `(e,)`라고 *생각*해도 좋다.

* `((->) e)`(위와 같은 요령으로 `(e ->)`라고 생각할 수 있다), 즉 타입 `e`의 값을 매개변수로 받는 함수들의 타입도 `Functor`다. 컨테이너로 보면 `(e -> a)`는 `e`의 값으로 색인되는 (무한할 수도 있는) `a` 값들의 집합을 나타낸다. 다른 관점에서, 그리고 더 유용하게는, `((->) e)`를 타입 `e`의 값을 읽기 전용으로 참조할 수 있는 문맥으로 생각할 수 있다. `((->) e)`가 종종 *리더 모나드(reader monad)*라고 불리는 이유이기도 하다. 이에 대해서는 나중에 더 다룬다.

* `IO`는 `Functor`다. 타입 `IO a`의 값은 I/O 효과를 동반할 수 있는, 타입 `a`의 값을 만들어 내는 계산을 나타낸다. `m`이 어떤 I/O 효과를 일으키며 값 `x`를 계산한다면, `fmap g m`은 같은 I/O 효과를 일으키며 값 `g x`를 계산한다.

* [containers 라이브러리](http://hackage.haskell.org/package/containers/)의 많은 표준 타입(`Tree`, `Map`, `Sequence` 등)이 `Functor`의 인스턴스다. 주목할 예외는 `Set`인데, 요소에 `Ord` 제약이 필요하기 때문에 Haskell에서는 `Functor`로 만들 수 없다(수학적으로는 분명 펑터지만). `fmap`은 *임의의* 타입 `a`, `b`에 적용 가능해야 하기 때문이다. 다만 `Set`(그리고 비슷하게 제약이 있는 다른 데이터 타입들)은 `Functor`를 적절히 일반화한 클래스의 인스턴스로는 만들 수 있다. [`a`와 `b`를 `Functor` 타입 클래스 자체의 인자로 만들거나](http://archive.fo/9sQhq), [연관 제약(associated constraint)을 추가하는](http://blog.omega-prime.co.uk/?p=127) 방법이 있다.

**연습문제**

1. `Either e`와 `((->) e)`의 `Functor` 인스턴스를 구현하라.
2. `((,) e)`와, 다음과 같이 정의된 `Pair`의 `Functor` 인스턴스를 구현하라.

   ```haskell
   data Pair a = Pair a a
   ```

   둘의 유사점과 차이점을 설명하라.
3. 다음과 같이 정의된 타입 `ITree`의 `Functor` 인스턴스를 구현하라.

   ```haskell
   data ITree a = Leaf (Int -> a) 
                | Node [ITree a]
   ```

4. 카인드가 `* -> *`이면서 (`undefined`를 쓰지 않고는) `Functor`의 인스턴스로 만들 수 없는 타입의 예를 들라.
5. 다음 진술은 참인가 거짓인가?

   > *두 `Functor`의 합성은 역시 `Functor`다.*

   거짓이면 반례를 들고, 참이면 적절한 Haskell 코드를 제시해 증명하라.

### 법칙

Haskell 언어 자체만 놓고 보면, `Functor`가 되기 위한 요구 사항은 올바른 타입의 `fmap` 구현뿐이다. 그러나 제대로 된 `Functor` 인스턴스라면 수학적 펑터 정의의 일부인 *펑터 법칙(functor laws)*도 만족해야 한다. 법칙은 두 가지다.

```haskell
fmap id = id
fmap (g . h) = (fmap g) . (fmap h)
```

> **참고**: 엄밀히 말하면 이 법칙들은 `f`와 `fmap`을 합쳐 Haskell 타입들의 범주 *Hask* 위의 자기 펑터(endofunctor)로 만든다(분위기 깨는 [⊥](https://wiki.haskell.org/Bottom)는 무시하고). [Wikibook: Category theory](http://en.wikibooks.org/wiki/Haskell/Category_theory)를 보라.

이 두 법칙은 `fmap g`가 컨테이너의 *구조*는 바꾸지 않고 요소만 바꾼다는 것을 보장한다. 동등하게, 그리고 더 간단하게 말하면, `fmap g`가 문맥을 바꾸지 않고 값만 바꾼다는 것을 보장한다.

첫 번째 법칙은 컨테이너의 모든 항목에 항등 함수를 매핑해도 아무 효과가 없다는 것이다. 두 번째 법칙은 두 함수의 합성을 모든 항목에 매핑하는 것이, 한 함수를 먼저 매핑하고 다른 함수를 그다음 매핑하는 것과 같다는 것이다.

예를 들어 다음 코드는 `Functor`의 "유효한"(타입 체크를 통과하는) 인스턴스이지만 펑터 법칙을 위반한다. 왜 그런지 보이는가?

```haskell
-- 사악한 Functor 인스턴스
instance Functor [] where
  fmap :: (a -> b) -> [a] -> [b]
  fmap _ [] = []
  fmap g (x:xs) = g x : g x : fmap g xs
```

밥값을 하는 Haskell 사용자라면 누구나 이 코드를 소름 끼치는 흉물로 여겨 거부할 것이다.

앞으로 만나게 될 다른 몇몇 타입 클래스와 달리, 주어진 타입의 유효한 `Functor` 인스턴스는 많아야 하나다. 이는 `fmap`의 타입에 대한 [*자유 정리(free theorem)*](http://homepages.inf.ed.ac.uk/wadler/topics/parametricity.html#free)로 [증명할 수 있다](http://archive.fo/U8xIY). 실제로 [GHC는 많은 데이터 타입의 `Functor` 인스턴스를 자동으로 유도할 수 있다](http://byorgey.wordpress.com/2010/03/03/deriving-pleasure-from-ghc-6-12-1/).

> **참고**: 사실 `seq`/`undefined`까지 고려하면, 첫 번째 법칙은 만족하면서 두 번째 법칙은 만족하지 않는 구현이 [가능하다](http://stackoverflow.com/a/8323243/305559). 이 절의 나머지 서술은 `seq`와 `undefined`를 배제한 맥락에서 읽어야 한다.

[비슷한 논증](https://github.com/quchen/articles/blob/master/second_functor_law.md)으로, 첫 번째 법칙(`fmap id = id`)을 만족하는 `Functor` 인스턴스는 자동으로 두 번째 법칙도 만족한다는 것을 보일 수 있다. 실용적으로 이는 `Functor` 인스턴스가 유효한지 확인하려면 첫 번째 법칙만 (보통 아주 단순한 귀납법으로) 검사하면 된다는 뜻이다.

**연습문제**

1. (`undefined`를 배제하면) 첫 번째 `Functor` 법칙은 만족하면서 두 번째는 만족하지 않는 `Functor` 인스턴스는 불가능하지만, 그 역은 가능하다. 두 번째 법칙은 만족하지만 첫 번째 법칙은 만족하지 않는 (엉터리) `Functor` 인스턴스의 예를 들라.
2. 위에서 보인 리스트의 사악한 `Functor` 인스턴스는 어느 법칙을 위반하는가? 두 법칙 모두인가, 첫 번째 법칙만인가? 구체적인 반례를 들라.

### 직관

`fmap`을 생각하는 근본적인 방식은 두 가지다. 첫 번째는 이미 언급했다. 함수와 컨테이너라는 두 매개변수를 받아, 함수를 컨테이너 "안쪽"에 적용해 새 컨테이너를 만든다는 것이다. 다른 방식으로는, `fmap`이 문맥 속의 값에 (문맥은 바꾸지 않고) 함수를 적용한다고 생각할 수 있다.

그런데 "매개변수가 둘 이상인" 다른 모든 Haskell 함수와 마찬가지로, `fmap`은 사실 *커링(curry)*되어 있다. 실제로는 매개변수 두 개를 받는 것이 아니라, 매개변수 하나를 받아 함수를 반환한다. 강조를 위해 괄호를 추가해 `fmap`의 타입을 쓰면 `fmap :: (a -> b) -> (f a -> f b)`다. 이렇게 써 놓고 보면, `fmap`이 "보통" 함수(`g :: a -> b`)를 컨테이너/문맥 위에서 동작하는 함수(`fmap g :: f a -> f b`)로 변환한다는 것이 분명해진다. 이 변환을 흔히 *리프트(lift)*라고 부른다. `fmap`은 함수를 "보통 세계"에서 "`f` 세계"로 "들어 올린다".

### 유틸리티 함수

`Data.Functor` 모듈에서 임포트할 수 있는 `Functor` 관련 함수가 몇 가지 더 있다.

* `(<$>)`는 `fmap`의 동의어로 정의된다. 이 덕분에 함수 적용 연산자 `($)`를 닮은 깔끔한 중위(infix) 스타일이 가능하다. 예를 들어 `f $ 3`은 함수 `f`를 3에 적용하고, `f <$> [1,2,3]`은 `f`를 리스트의 각 원소에 적용한다.
* `($>) :: Functor f => f a -> b -> f b`는 그냥 `flip (<$)`이며 가끔 유용하다. 헷갈리지 않으려면 `(<$)`와 `($>)`가 남게 될 값을 가리킨다고 기억하면 된다.
* `void :: Functor f => f a -> f ()`는 `(<$)`의 특수화다. 즉 `void x = () <$ x`다. 계산이 어떤 값을 만들어 내지만 그 값을 무시해야 하는 경우에 쓸 수 있다.

### 더 읽을거리

펑터라는 개념 뒤에 있는 범주론에 대해 읽기 시작하기 좋은 곳은 훌륭한 [Haskell wikibook의 범주론 페이지](http://en.wikibooks.org/wiki/Haskell/Category_theory)다.

## Applicative

표준 Haskell 타입 클래스의 전당에 비교적 새로 추가된 *applicative functor*는 표현력에서 `Functor`와 `Monad` 사이에 놓이는 추상화로, McBride와 Paterson이 처음 기술했다. 그들의 고전적 논문 제목 [Applicative Programming with Effects](http://www.soi.city.ac.uk/~ross/papers/Applicative.html)는 [`Applicative`](https://hackage.haskell.org/package/base/docs/Control-Applicative.html) 타입 클래스에 담긴 직관을 암시한다. 이 클래스는 특정 종류의 "효과 있는(effectful)" 계산을 함수형답게 순수한 방식으로 캡슐화하고, "applicative" 프로그래밍 스타일을 장려한다. 이것들이 정확히 무엇을 의미하는지는 뒤에서 보게 될 것이다.

### 정의

`Functor`는 "보통" 함수를 계산 문맥 위의 함수로 들어 올릴 수 있게 해 준다는 것을 기억하자. 하지만 `fmap`으로는, 그 자체가 문맥 안에 들어 있는 함수를 문맥 안의 값에 적용할 수 없다. `Applicative`는 바로 그런 도구인 `(<*>)`("apply", "app", "splat" 등으로 다양하게 읽는다)를 제공한다. 또한 값을 기본적인 "효과 없는" 문맥에 집어넣는 메서드 `pure`도 제공한다. `Control.Applicative`에 정의된 `Applicative`의 타입 클래스 선언은 다음과 같다.

```haskell
class Functor f => Applicative f where
  pure  :: a -> f a
  infixl 4 <*>, *>, <*
  (<*>) :: f (a -> b) -> f a -> f b

  (*>) :: f a -> f b -> f b
  a1 *> a2 = (id <$ a1) <*> a2

  (<*) :: f a -> f b -> f a
  (<*) = liftA2 const
```

모든 `Applicative`는 `Functor`이기도 해야 한다는 점에 주목하자. 실제로, 뒤에서 보겠지만 `fmap`은 `Applicative` 메서드들로 구현할 수 있으므로, 모든 `Applicative`는 우리가 원하든 원치 않든 펑터다. `Functor` 제약은 우리를 정직하게 만들 뿐이다.

`(*>)`와 `(<*)`는 특정 `Applicative` 인스턴스가 더 효율적인 구현을 제공할 수 있도록 편의상 포함되어 있으며, 기본 구현이 제공된다. 이 연산자들에 대해서는 아래 [유틸리티 함수](#유틸리티-함수-1) 절을 보라.

> **참고**: `($)`는 그냥 함수 적용이다: `f $ x = f x`.

언제나 그렇듯 타입 시그니처를 이해하는 것이 결정적이다. 먼저 `(<*>)`를 보자. 이를 이해하는 가장 좋은 방법은 `(<*>)`의 타입이 `($)`의 타입과 비슷하되 모든 것이 `f`로 감싸여 있다는 점에 주목하는 것이다. 다시 말해 `(<*>)`는 계산 문맥 안에서의 함수 적용일 뿐이다. `(<*>)`의 타입은 `fmap`의 타입과도 매우 비슷하다. 유일한 차이는 첫 번째 매개변수가 "보통" 함수 `(a -> b)`가 아니라 문맥 안의 함수 `f (a -> b)`라는 점이다.

`pure`는 임의의 타입 `a`의 값을 받아 타입 `f a`의 문맥/컨테이너를 반환한다. `pure`가 어떤 "기본" 컨테이너 또는 "효과 없는" 문맥을 만든다는 의도다. 실제로 `pure`의 동작은 `(<*>)`와 함께 만족해야 하는 법칙들에 의해 상당히 제약된다. 보통 주어진 `(<*>)` 구현에 대해 가능한 `pure` 구현은 하나뿐이다.

(참고로 예전 버전의 Typeclassopedia는 `pure`를 `Pointed`라는 타입 클래스로 설명했다. 이 클래스는 여전히 [`pointed` 패키지](http://hackage.haskell.org/package/pointed)에서 찾을 수 있다. 그러나 현재의 중론은 `Pointed`가 결국 그리 유용하지 않다는 것이다. 더 자세한 설명은 [Why not Pointed?](https://wiki.haskell.org/Why_not_Pointed%3F)를 보라.)

### 법칙

> **참고**: [Applicative의 haddock](https://hackage.haskell.org/package/base/docs/Control-Applicative.html)과 [Applicative programming with effects](http://www.soi.city.ac.uk/~ross/papers/Applicative.html)를 보라.

전통적으로 `Applicative` 인스턴스가 만족해야 하는 법칙은 네 가지다. 어떤 의미에서 이 법칙들은 모두 `pure`가 그 이름값을 하도록 보장하는 데 관련되어 있다.

* 항등(identity) 법칙:

  ```haskell
  pure id <*> v = v
  ```

* 준동형(homomorphism):

  ```haskell
  pure f <*> pure x = pure (f x)
  ```

  직관적으로, 효과 없는 함수를 효과 없는 인자에 효과 있는 문맥 안에서 적용하는 것은, 그냥 함수를 인자에 적용한 뒤 결과를 `pure`로 문맥에 주입하는 것과 같다.
* 교환(interchange):

  ```haskell
  u <*> pure y = pure (\f -> f y) <*> u
  ```

  직관적으로, 효과 있는 함수를 순수한 인자에 적용하는 것을 평가할 때 함수와 인자를 평가하는 순서는 중요하지 않다는 뜻이다.
* 합성(composition):

  ```haskell
  u <*> (v <*> w) = pure (.) <*> u <*> v <*> w
  ```

  직관을 얻기가 가장 까다로운 법칙이다. 어떤 의미에서 `(<*>)`의 일종의 결합성을 표현한다. 독자는 이 법칙이 타입상 올바르다는 것을 스스로 납득해 보는 것으로 충분할 수 있다.

준동형, 교환, 합성 법칙을 왼쪽에서 오른쪽으로 가는 재작성 규칙으로 보면, 이들은 `pure`와 `(<*>)`를 사용하는 임의의 표현식을 맨 앞에 `pure`가 딱 한 번 나오고 `(<*>)`가 왼쪽으로만 중첩되는 정규형으로 변환하는 알고리즘을 이룬다. 합성 법칙은 `(<*>)`의 재결합을, 교환 법칙은 `pure`를 왼쪽으로 옮기는 것을, 준동형 법칙은 인접한 여러 `pure`를 하나로 합치는 것을 가능하게 한다.

`Applicative`가 `Functor`와 어떻게 관계 맺어야 하는지를 명시하는 법칙도 있다.

```haskell
fmap g x = pure g <*> x
```

순수 함수 `g`를 문맥 `x` 위에 매핑하는 것은, 먼저 `g`를 `pure`로 문맥에 주입한 다음 `(<*>)`로 `x`에 적용하는 것과 같다는 뜻이다. 다시 말해 `fmap`을 더 원자적인 두 연산—문맥으로의 주입과 문맥 안에서의 적용—으로 분해할 수 있다. `(<$>)`는 `fmap`의 동의어이므로 위 법칙은 이렇게도 쓸 수 있다.

`g <$> x = pure g <*> x`

**연습문제**

1. (까다로움) 순수 함수를 효과 있는 인자에 적용하는 것에 관한, 교환 법칙의 변형을 상상해 볼 수 있다. 위 법칙들을 사용해 다음을 증명하라.

   ```haskell
   pure f <*> x = pure (flip ($)) <*> x <*> pure f
   ```

### 인스턴스

`Functor`의 인스턴스인 표준 타입 대부분은 `Applicative`의 인스턴스이기도 하다.

`Maybe`는 쉽게 `Applicative` 인스턴스로 만들 수 있다. 그런 인스턴스를 작성하는 일은 독자를 위한 연습문제로 남겨 둔다.

리스트 타입 생성자 `[]`는 사실 두 가지 방식으로 `Applicative` 인스턴스가 될 수 있다. 본질적으로, 리스트를 요소들의 순서 있는 모음으로 볼 것이냐, 아니면 비결정적 계산의 여러 결과를 나타내는 문맥으로 볼 것이냐의 문제다(Wadler의 [How to replace failure by a list of successes](https://rkrishnan.org/files/wadler-1985.pdf)를 보라).

먼저 모음(collection) 관점을 생각해 보자. 특정 타입에 대해 주어진 타입 클래스의 인스턴스는 하나만 있을 수 있으므로, 리스트의 두 `Applicative` 인스턴스 중 하나 또는 둘 다는 `newtype` 래퍼에 대해 정의해야 한다. 실제로는 비결정적 계산 인스턴스가 기본이고, 모음 인스턴스는 `ZipList`라는 `newtype`에 대해 정의되어 있다. 이 인스턴스는 다음과 같다.

```haskell
newtype ZipList a = ZipList { getZipList :: [a] }

instance Applicative ZipList where
  pure :: a -> ZipList a
  pure = undefined   -- 연습문제

  (<*>) :: ZipList (a -> b) -> ZipList a -> ZipList b
  (ZipList gs) <*> (ZipList xs) = ZipList (zipWith ($) gs xs)
```

`(<*>)`로 함수 리스트를 입력 리스트에 적용하려면, 함수와 입력을 요소별로 짝지어 결과 출력들의 리스트를 만들면 된다. 다시 말해 두 리스트를 함수 적용 `($)`로 "zip"하는 것이다. `ZipList`라는 이름이 여기서 나왔다.

비결정적 계산 관점에 기반한 리스트의 다른 `Applicative` 인스턴스는 다음과 같다.

```haskell
instance Applicative [] where
  pure :: a -> [a]
  pure x = [x]

  (<*>) :: [a -> b] -> [a] -> [b]
  gs <*> xs = [ g x | g <- gs, x <- xs ]
```

함수를 입력에 짝지어 적용하는 대신, 각 함수를 모든 입력에 차례로 적용하고 모든 결과를 리스트에 모은다.

이제 비결정적 계산을 자연스러운 스타일로 쓸 수 있다. 숫자 `3`과 `4`를 결정적으로 더하려면 물론 `(+) 3 4`라고 쓰면 된다. 그런데 `3` 대신 결과가 `2`, `3`, `4`일 수 있는 비결정적 계산이 있다고 하자. 그러면 이렇게 쓸 수 있다.

```haskell
  pure (+) <*> [2,3,4] <*> pure 4
```

더 관용적으로는 이렇게 쓴다.

```haskell
  (+) <$> [2,3,4] <*> pure 4
```

그 밖에도 여러 `Applicative` 인스턴스가 있다.

* `IO`는 `Applicative`의 인스턴스이며, 예상대로 동작한다. `m1 <*> m2`를 실행하려면 먼저 `m1`을 실행해 함수 `f`를 얻고, 다음으로 `m2`를 실행해 값 `x`를 얻은 뒤, 마지막으로 값 `f x`를 `m1 <*> m2` 실행의 결과로 반환한다.

* `((,) a)`는 `a`가 `Monoid`([Monoid 절](014-typeclassopedia-part2.md#monoid) 참고)의 인스턴스인 한 `Applicative`다. `a` 값들은 계산과 나란히 누적된다.

* `Applicative` 모듈은 `Const` 타입 생성자를 정의한다. 타입 `Const a b`의 값은 단순히 `a` 하나를 담는다. 이는 임의의 `Monoid a`에 대해 `Applicative`의 인스턴스이며, `Foldable`([Foldable 절](014-typeclassopedia-part2.md#foldable) 참고) 같은 것들과 결합할 때 특히 유용해진다.

* `WrappedMonad`와 `WrappedArrow`라는 newtype은 각각 `Monad`([Monad 절](#monad) 참고)나 `Arrow`([Arrow 절](014-typeclassopedia-part3.md#arrow) 참고)의 인스턴스를 `Applicative`의 인스턴스로 만들어 준다. 두 클래스를 공부할 때 보겠지만, 둘 다 `Applicative`보다 엄격하게 더 표현력이 크다. `Applicative` 메서드들을 그들의 메서드로 구현할 수 있다는 의미에서 그렇다.

**연습문제**

1. `Maybe`의 `Applicative` 인스턴스를 구현하라.
2. `ZipList`의 `Applicative` 인스턴스에서 올바른 `pure` 정의를 알아내라. `pure`와 `(<*>)`를 잇는 법칙을 만족하는 구현은 하나뿐이다.

### 직관

McBride와 Paterson의 논문은 계산 문맥 안에서의 함수 적용을 나타내기 위해 표기법 `[[g x1 x2 ... xn]]`을 도입한다. 각 `xi`가 어떤 applicative functor `f`에 대해 타입 `f ti`를 갖고 `g`가 타입 `t1 -> t2 -> ... -> tn -> t`를 갖는다면, 전체 표현식 `[[g x1 ... xn]]`은 타입 `f t`를 갖는다. 이는 함수를 여러 개의 "효과 있는" 인자에 적용하는 것으로 생각할 수 있다. 이런 의미에서 이 이중 대괄호 표기법은 함수를 문맥 안의 단일 인자에 적용하게 해 주는 `fmap`의 일반화다.

이 `fmap`의 일반화를 구현하는 데 왜 `Applicative`가 필요할까? `fmap`으로 `g`를 첫 매개변수 `x1`에 적용한다고 하자. 그러면 타입 `f (t2 -> ... t)`의 무언가를 얻는데, 여기서 막힌다. 이 문맥-속-함수를 `fmap`으로는 다음 인자에 적용할 수 없다. 그런데 `(<*>)`가 정확히 이것을 가능하게 해 준다.

여기서 이상화된 표기법 `[[g x1 x2 ... xn]]`을 Haskell로 옮기는 올바른 번역이 나온다. 바로

```haskell
  g <$> x1 <*> x2 <*> ... <*> xn,
```

이다. `Control.Applicative`가 `(<$>)`를 `fmap`의 편리한 중위 표기로 정의한다는 것을 기억하자. 이것이 "applicative 스타일"의 의미다—효과 있는 계산도 여전히 함수 적용의 형태로 기술할 수 있으며, 유일한 차이는 단순한 나란히 쓰기 대신 특별한 적용 연산자 `(<*>)`를 써야 한다는 것뿐이다.

`pure`를 사용하면 관용적 적용(idiomatic application)의 중간에 "효과 없는" 인자를 끼워 넣을 수 있다. 예를 들어

```haskell
  g <$> x1 <*> pure x2 <*> x3
```

은 다음이 주어졌을 때 타입 `f d`를 갖는다.

```haskell
g  :: a -> b -> c -> d
x1 :: f a
x2 :: b
x3 :: f c
```

이중 대괄호는 흔히 "idiom bracket"이라고 불린다. "관용적(idiomatic)" 함수 적용, 즉 겉보기에는 평범해 보이지만 (사용 중인 특정 `Applicative` 인스턴스에 따라 결정되는) 특별하고 비표준적인 의미를 갖는 함수 적용을 쓸 수 있게 해 주기 때문이다. Idiom bracket은 GHC에서는 지원되지 않지만, [Strathclyde Haskell Enhancement](http://personal.cis.strath.ac.uk/~conor/pub/she/)라는 전처리기가 지원한다. 이 전처리기는 (다른 많은 기능과 함께) idiom bracket을 표준적인 `(<$>)`와 `(<*>)` 사용으로 번역한다. `Applicative`를 많이 쓸 때 훨씬 읽기 좋은 코드가 될 수 있다.

또한 GHC 8부터는 `ApplicativeDo` 확장으로 `g <$> x1 <*> x2 <*> ... <*> xn`을 다른 스타일로 쓸 수 있다.

```haskell
do v1 <- x1
   v2 <- x2
   ...
   vn <- xn
   pure (g v1 v2 ... vn)
```

자세한 내용은 아래 더 읽을거리 절과 Monad 절의 do 표기법 논의를 보라.

### 유틸리티 함수

`Control.Applicative`는 임의의 `Applicative` 인스턴스와 함께 제네릭하게 동작하는 유틸리티 함수를 여럿 제공한다.

* `liftA :: Applicative f => (a -> b) -> f a -> f b`. 익숙할 것이다. 물론 `fmap`과 같고(따라서 `(<$>)`와도 같다), 다만 타입이 더 제한적이다. `liftA2`, `liftA3`과 짝을 맞추기 위해 존재하는 것으로 보이며, 실제로 이 함수를 써야 할 이유는 없다.

* `liftA2 :: Applicative f => (a -> b -> c) -> f a -> f b -> f c`는 2인자 함수를 어떤 `Applicative`의 문맥에서 동작하도록 들어 올린다. `liftA2 f arg1 arg2`처럼 완전히 적용된 경우에는 대신 `f <$> arg1 <*> arg2`를 쓰는 것이 대체로 더 좋은 스타일이다. 그러나 `liftA2`는 부분 적용되는 상황에서 유용할 수 있다. 예를 들어 `(+) = liftA2 (+)` 등으로 정의해 `Maybe Integer`의 `Num` 인스턴스를 만들 수 있다.

* `liftA3`은 있지만 더 큰 `n`에 대한 `liftAn`은 없다.

* `(*>) :: Applicative f => f a -> f b -> f b`는 두 `Applicative` 계산의 효과를 순서대로 수행하되 첫 번째의 결과는 버린다. 예를 들어 `m1, m2 :: Maybe Int`라면, `m1 *> m2`는 `m1`이나 `m2` 중 하나라도 `Nothing`이면 `Nothing`이고, 그렇지 않으면 `m2`와 같은 값을 갖는다.

* 마찬가지로 `(<*) :: Applicative f => f a -> f b -> f a`는 두 계산의 효과를 순서대로 수행하되 첫 번째의 결과만 남기고 두 번째의 결과는 버린다. `(<$)`와 `($>)`에서처럼, `(<*)`와 `(*>)`를 헷갈리지 않으려면 남게 될 값을 가리키는 방향이라고 기억하면 된다.

* `(<**>) :: Applicative f => f a -> f (a -> b) -> f b`는 `(<*>)`와 비슷하지만, 첫 번째 계산이 만드는 값(들)이 두 번째 계산이 만드는 함수(들)의 입력으로 제공된다. 이는 `flip (<*>)`과 같지 않다는 점에 유의하라. 효과가 반대 순서로 수행되기 때문이다. 리스트 인스턴스처럼 비가환적 효과를 갖는 `Applicative` 인스턴스라면 이 차이를 관찰할 수 있다. `(<**>) [1,2] [(+5),(*10)]`은 같은 인자에 대한 `(flip (<*>))`과 다른 결과를 낸다.

* `when :: Applicative f => Bool -> f () -> f ()`는 계산을 조건부로 실행한다. 조건이 `True`이면 두 번째 인자로, `False`이면 `pure ()`로 평가된다.

* `unless :: Applicative f => Bool -> f () -> f ()`는 `when`과 같지만 조건이 부정되어 있다.

* `guard` 함수는 `Alternative`(실패와 선택 개념을 통합한 `Applicative`의 확장)의 인스턴스와 함께 쓰기 위한 것으로, [Alternative와 그 친구들에 관한 절](014-typeclassopedia-part2.md#실패와-선택-alternative-monadplus-arrowplus)에서 논의한다.

**연습문제**

1. 함수

   ```haskell
   sequenceAL :: Applicative f => [f a] -> f [a]
   ```

   를 구현하라. 이것의 일반화 버전인 `sequenceA`는 임의의 `Traversable`에 대해 동작하지만(뒤의 Traversable 절 참고), 리스트에 특수화된 이 버전을 구현하는 것은 좋은 연습이다.

### 다른 형태의 정식화

`Applicative`와 동등한 다른 정식화는 다음과 같다.

```haskell
class Functor f => Monoidal f where
  unit :: f ()
  (**) :: f a -> f b -> f (a,b)
```

> **참고**: 범주론 용어로는, `f (a, b) -> (f a, f b)` 같은 반대 방향 함수가 꼭 있는 것은 아니기 때문에 `f`를 *lax* monoidal functor라고 부른다.

직관적으로 이는 *monoidal* functor가 일종의 "기본 형태"를 갖고 일종의 "결합" 연산을 지원하는 펑터라는 뜻이다. `pure`와 `(<*>)`는 `unit`과 `(**)`와 표현력이 동등하다(아래 연습문제 참고). 기술적으로 말하면, `f`가 짝 생성자 `(,)`와 단위 타입 `()`이 이루는 "모노이드 구조"를 보존한다는 아이디어다. `unit`과 `(**)`의 타입을 다음처럼 다시 쓰면 더 분명하게 보인다.

```haskell
  unit' :: () -> f ()
  (**') :: (f a, f b) -> f (a, b)
```

나아가 "monoidal"이라는 이름값을 하려면([Monoid 절](014-typeclassopedia-part2.md#monoid) 참고), `Monoidal`의 인스턴스는 다음 법칙들을 만족해야 한다. 전통적인 `Applicative` 법칙보다 훨씬 알기 쉬워 보인다.

> **참고**: 여기와 아래 법칙들에서 `≅`는 등식이 아니라 *동형(isomorphism)*을 뜻한다. 특히 `(x,()) ≅ x ≅ ((),x)`와 `((x,y),z) ≅ (x,(y,z))`로 간주한다.

* 왼쪽 항등:

  ```haskell
  unit ** v ≅ v
  ```

* 오른쪽 항등:

  ```haskell
  u ** unit ≅ u
  ```

* 결합성:

  ```haskell
  u ** (v ** w) ≅ (u ** v) ** w
  ```

이 법칙들은 통상적인 `Applicative` 법칙들과 동등한 것으로 밝혀져 있다. 범주론적 설정에서는 자연성(naturality) 법칙도 요구한다.

> **참고**: 여기서 `g *** h = \(x,y) -> (g x, h y)`다. [Arrow](014-typeclassopedia-part3.md#arrow)를 보라.

* 자연성:

  ```haskell
  fmap (g *** h) (u ** v) = fmap g u ** fmap h v
  ```

하지만 Haskell의 맥락에서 이는 자유 정리다.

이 절의 상당 부분은 [Edward Z. Yang의 블로그 글](http://blog.ezyang.com/2012/08/applicative-functors/)에서 가져왔다. 조금 더 자세한 내용은 원문을 보라.

**연습문제**

1. `pure`와 `(<*>)`를 `unit`과 `(**)`로 구현하고, 그 반대도 해 보라.
2. `f () -> ()`와 `f (a,b) -> (f a, f b)` 같은 함수들도 존재하며 어떤 "합리적인" 법칙을 만족하는 `Applicative` 인스턴스가 있는가?
3. (까다로움) 첫 번째 연습문제의 구현이 주어졌을 때, 통상적인 `Applicative` 법칙과 위의 `Monoidal` 법칙이 동등함을 증명하라.

### 더 읽을거리

[McBride와 Paterson의 원 논문](http://www.soi.city.ac.uk/~ross/papers/Applicative.html)은 정보와 예제의 보고이며, `Applicative`와 범주론의 연결에 대한 관점도 담고 있다. 초보자가 논문 전체를 읽어 내기는 어렵겠지만, 동기 부여가 매우 잘 되어 있어 초보자라도 읽을 수 있는 데까지 읽으면 얻는 것이 있을 것이다.

> **참고**: [이전 논문](http://conal.net/papers/simply-reactive/)에서 도입되었고, 이후 [Push-pull functional reactive programming](http://conal.net/papers/push-pull-frp/)으로 대체되었다.

Conal Elliott은 `Applicative`의 가장 큰 지지자 중 한 명이다. 예를 들어 [함수형 이미지를 위한 Pan 라이브러리](http://conal.net/papers/functional-images/)와 함수형 반응형 프로그래밍(FRP)을 위한 reactive 라이브러리가 이를 핵심적으로 사용하며, 그의 블로그에도 [`Applicative`가 활약하는 많은 예](http://conal.net/blog/tag/applicative-functor)가 있다. McBride와 Paterson의 작업 위에서 Elliott은 [TypeCompose](https://wiki.haskell.org/TypeCompose) 라이브러리도 만들었는데, 이는 (다른 것들과 함께) `Applicative` 타입이 합성에 닫혀 있다는 관찰을 구현한 것이다. 따라서 단순한 타입들로 조립된 복잡한 타입의 `Applicative` 인스턴스를 종종 자동으로 유도할 수 있다.

[Parsec 파싱 라이브러리](http://hackage.haskell.org/package/parsec)([논문](http://legacy.cs.uu.nl/daan/download/papers/parsec-paper.pdf))는 원래 모나드로 쓰도록 설계되었지만, 가장 흔한 사용 사례에서는 `Applicative` 인스턴스만으로 큰 효과를 볼 수 있다. [Bryan O'Sullivan의 블로그 글](http://www.serpentine.com/blog/2008/02/06/the-basics-of-applicative-functors-put-to-practical-work/)이 좋은 출발점이다. `Monad`가 제공하는 추가적인 힘이 필요 없다면 보통 `Applicative`를 대신 쓰는 것이 좋다.

`Applicative`가 활약하는 다른 좋은 예로는 [ConfigFile과 HSQL 라이브러리](http://web.archive.org/web/20090416111947/chrisdone.com/blog/html/2009-02-10-applicative-configfile-hsql.html), 그리고 [formlets 라이브러리](http://groups.inf.ed.ac.uk/links/formlets/)가 있다.

Gershom Bazerman의 [글](http://comonad.com/reader/2012/abstracting-with-applicatives/)에는 applicative에 대한 통찰이 많이 담겨 있다.

`ApplicativeDo` 확장은 [이 위키 페이지](https://ghc.haskell.org/trac/ghc/wiki/ApplicativeDo)에, 더 자세하게는 [이 Haskell Symposium 논문](http://doi.org/10.1145/2976002.2976007)에 기술되어 있다.

## Monad

이 글을 읽고 있다면 모나드에 대해 들어 봤을 것이라고 장담해도 좋다—`Applicative`나 `Arrow`, 심지어 `Monoid`는 들어 본 적이 없을지언정. 왜 Haskell에서는 모나드가 이렇게 큰 화제일까? 몇 가지 이유가 있다.

* Haskell은 실제로 모나드를 특별 취급한다. I/O 연산을 구성하는 프레임워크로 삼았기 때문이다.
* Haskell은 또 모나드 표현식을 위한 특별한 문법 설탕인 `do` 표기법을 제공함으로써 모나드를 특별 취급한다. (GHC 8부터는 `do` 표기법을 `Applicative`와도 쓸 수 있지만, 이 표기법은 여전히 근본적으로 모나드와 관련되어 있다.)
* `Monad`는 `Applicative`나 `Arrow` 같은 다른 계산 추상화 모델보다 더 오래되었다.
* 모나드 튜토리얼이 많아질수록 사람들은 모나드가 그만큼 어려운 것이라고 생각하게 되고, 마침내 모나드를 "이해했다"고 생각하는 사람들이 새 모나드 튜토리얼을 더 많이 쓰게 된다([모나드 튜토리얼의 오류](http://byorgey.wordpress.com/2009/01/12/abstraction-intuition-and-the-monad-tutorial-fallacy/)).

이것들이 좋은 이유인지는 여러분의 판단에 맡긴다.

결국 그 모든 소란에도 불구하고 `Monad`는 그저 또 하나의 타입 클래스일 뿐이다. 정의를 살펴보자.

### 정의

GHC 8.8 기준으로 [`Monad`](https://hackage.haskell.org/package/base/docs/Prelude.html#t:Monad)는 다음과 같이 정의된다.

```haskell
class Applicative m => Monad m where
  return :: a -> m a
  (>>=)  :: m a -> (a -> m b) -> m b
  (>>)   :: m a -> m b -> m b
  m >> n = m >>= \_ -> n
```

(GHC 7.10 이전에는 역사적인 이유로 `Applicative`가 `Monad`의 슈퍼클래스가 아니었고, GHC 8.8 이전에는 `Monad`에 `fail` 메서드가 하나 더 있었다.)

`Monad` 타입 클래스는 몇 가지 표준 인스턴스와 함께 `Prelude`에서 익스포트된다. 다만 많은 유틸리티 함수는 [`Control.Monad`](https://hackage.haskell.org/package/base/docs/Control-Monad.html)에 있다.

`Monad` 클래스의 메서드를 하나씩 살펴보자. `return`의 타입은 익숙할 것이다. `pure`와 같다. 실제로 `return`은 곧 `pure`이며, 이름이 불행할 뿐이다. (명령형 프로그래밍 배경에서 온 사람은 `return`이 C나 Java의 동명 키워드 같은 것이라고 생각할 수 있지만, 실제 유사점은 거의 없다는 점에서 불행하다.) 역사적인 이유로 두 이름이 모두 남아 있지만, 둘은 항상 같은 값을 나타내야 한다(강제할 수는 없지만). 마찬가지로 `(>>)`는 `Applicative`의 `(*>)`와 같아야 한다. `return`과 `(>>)`는 언젠가 `Monad` 클래스에서 제거될 수도 있다. [Monad of No Return 제안](https://ghc.haskell.org/trac/ghc/wiki/Proposal/MonadOfNoReturn)을 보라.

`(>>)`는 `(>>=)`의 특수화 버전이며 기본 구현이 주어져 있다. 타입 클래스 선언에 포함된 이유는 특정 `Monad` 인스턴스가 원한다면 더 효율적인 구현으로 `(>>)`의 기본 구현을 재정의할 수 있게 하기 위해서일 뿐이다. 또한 `_ >> n = n`도 타입은 맞는 `(>>)` 구현이지만 의도된 의미와 다르다는 점에 유의하라. `m >> n`은 `m`의 *결과*는 무시하되 *효과*는 무시하지 않는 것이 의도다.

정말로 흥미로운 것—그리고 `Monad`를 `Applicative`보다 엄격히 더 강력하게 만드는 것—은 `(>>=)`이며, 흔히 *bind*라고 부른다.

`(>>=)` 뒤의 직관에 대해서는 한참 이야기할 수 있고, 실제로 그럴 것이다. 하지만 먼저 예제를 몇 개 보자.

### 인스턴스

`Monad` 클래스 뒤의 직관을 이해하지 못하더라도, 그냥 타입이 이끄는 대로 따라가면서 인스턴스를 만들 수는 있다. 놀랍게도 이것만으로도 직관을 이해하는 데 꽤 멀리까지 갈 수 있다. 적어도, `Monad` 클래스 일반에 대해 더 읽어 나가는 동안 가지고 놀 구체적인 예제들이 생긴다. 처음 몇 예제는 표준 `Prelude`에서, 나머지 예제는 [`transformers` 패키지](http://hackage.haskell.org/package/transformers)에서 가져왔다.

* 가장 단순한 `Monad` 인스턴스는 [`Identity`](http://hackage.haskell.org/packages/archive/mtl/1.1.0.2/doc/html/Control-Monad-Identity.html)로, Dan Piponi의 강력 추천 블로그 글 [The Trivial Monad](http://blog.sigfpe.com/2007/04/trivial-monad.html)에 설명되어 있다. "자명"함에도 불구하고 `Monad` 타입 클래스의 훌륭한 입문이며, 머리를 굴리게 하는 좋은 연습문제도 담고 있다.

* 그다음으로 단순한 `Monad` 인스턴스는 `Maybe`다. `Maybe`의 `return`/`pure`는 이미 쓸 줄 안다. 그럼 `(>>=)`는 어떻게 쓸까? 타입을 생각해 보자. `Maybe`로 특수화하면 다음과 같다.

  ```haskell
  (>>=) :: Maybe a -> (a -> Maybe b) -> Maybe b
  ```

  `(>>=)`의 첫 인자가 `Just x`라면 타입 `a`의 무언가(바로 `x`)가 있으니 두 번째 인자를 적용할 수 있고, 그 결과는 우리가 원하던 `Maybe b`다. 첫 인자가 `Nothing`이라면? `a -> Maybe b` 함수를 적용할 대상이 없으니 할 수 있는 일은 하나뿐이다. `Nothing`을 내놓는 것이다. 이 인스턴스는 다음과 같다.

  ```haskell
  instance Monad Maybe where
    return :: a -> Maybe a
    return = Just

    (>>=) :: Maybe a -> (a -> Maybe b) -> Maybe b
    (Just x) >>= g = g x
    Nothing  >>= _ = Nothing
  ```

  여기서 무슨 일이 일어나는지 벌써 약간의 직관을 얻을 수 있다. `(>>=)`로 여러 함수를 사슬처럼 엮어 계산을 만들면, 그중 하나라도 실패하는 순간 전체 계산이 실패한다(`f`가 무엇이든 `Nothing >>= f`는 `Nothing`이므로). 구성 함수들이 개별적으로 모두 성공해야만 전체 계산이 성공한다. 즉 `Maybe` 모나드는 실패할 수 있는 계산을 모델링한다.

* 리스트 생성자 `[]`의 `Monad` 인스턴스는 그 `Applicative` 인스턴스와 비슷하다. 아래 연습문제를 보라.

* 물론 `IO` 생성자는 유명한 `Monad`이지만, 그 구현은 다소 마법 같고 컴파일러마다 다를 수도 있다. 강조할 점은 `IO` 모나드가 마법인 *유일한* 모나드라는 것이다. `IO`는 효과를 일으킬 수 있는 계산을 나타내는 값을 완전히 순수한 방식으로 조립할 수 있게 해 준다. 타입 `IO ()`의 특별한 값 `main`을 런타임이 받아 실제로 실행하고, 실제 효과를 일으킨다. 그 밖의 모든 모나드는 함수형답게 순수하며 특별한 컴파일러 지원이 전혀 필요 없다. 우리는 종종 모나드 값을 "효과 있는 계산"이라고 부르지만, 이는 어떤 모나드들이 마치 부수 효과가 있는 것처럼 코드를 쓰게 해 주기 때문이다. 실제로는 모나드가 그 겉보기 부수 효과를 함수형답게 순수한 방식으로 구현하는 배관을 숨기고 있는 것이다.

* 앞서 언급했듯 `((->) e)`는 *리더 모나드*라고 알려져 있다. 타입 `e`의 값이 읽기 전용 환경으로 주어지는 계산을 기술하기 때문이다.

  [`Control.Monad.Reader`](http://hackage.haskell.org/packages/archive/mtl/latest/doc/html/Control-Monad-Reader.html) 모듈은 `Reader e a` 타입을 제공한다. 이는 `(e -> a)`의 편의용 `newtype` 래퍼일 뿐이며, 적절한 `Monad` 인스턴스와 함께 `ask`(환경 가져오기), `asks`(환경에 함수를 적용한 값 가져오기), `local`(다른 환경에서 하위 계산 실행하기) 같은 `Reader` 전용 유틸리티 함수를 제공한다.

* [`Control.Monad.Writer`](http://hackage.haskell.org/packages/archive/mtl/latest/doc/html/Control-Monad-Writer-Lazy.html) 모듈은 `Writer` 모나드를 제공하는데, 계산이 진행되는 동안 정보를 수집할 수 있게 해 준다. `Writer w a`는 `(a,w)`와 동형이며, 출력 값 `a`가 타입 `w`의 주석 내지 "로그"와 함께 운반된다. `w`는 `Monoid`의 인스턴스여야 한다([Monoid 절](014-typeclassopedia-part2.md#monoid) 참고). 특별한 함수 `tell`이 로깅을 수행한다.

* [`Control.Monad.State`](http://hackage.haskell.org/packages/archive/mtl/latest/doc/html/Control-Monad-State-Lazy.html) 모듈은 `s -> (a,s)`의 `newtype` 래퍼인 `State s a` 타입을 제공한다. 타입 `State s a`의 무언가는 `a`를 만들어 내되 도중에 타입 `s`의 상태에 접근하고 수정할 수 있는, 상태 있는 계산을 나타낸다. 이 모듈은 `get`(현재 상태 읽기), `gets`(현재 상태에 함수를 적용한 값 읽기), `put`(상태 덮어쓰기), `modify`(상태에 함수 적용하기) 같은 `State` 전용 유틸리티 함수도 제공한다.

* [`Control.Monad.Cont`](http://hackage.haskell.org/packages/archive/mtl/latest/doc/html/Control-Monad-Cont.html) 모듈은 continuation-passing 스타일의 계산을 나타내는 `Cont` 모나드를 제공한다. 계산의 일시 중지와 재개, 비지역적 제어 이동, 코루틴, 그 밖의 복잡한 제어 구조를—모두 함수형답게 순수한 방식으로—구현하는 데 쓸 수 있다. `Cont`는 그 보편적 성질 때문에 ["모든 모나드의 어머니"](http://blog.sigfpe.com/2008/12/mother-of-all-monads.html)라고 불려 왔다.

**연습문제**

1. 리스트 생성자 `[]`의 `Monad` 인스턴스를 구현하라. 타입을 따라가라!
2. `((->) e)`의 `Monad` 인스턴스를 구현하라.
3. 다음과 같이 정의된 `Free f`의 `Functor`와 `Monad` 인스턴스를 구현하라.

   ```haskell
   data Free f a = Var a
                 | Node (f (Free f a))
   ```

   `f`에 `Functor` 인스턴스가 있다고 가정해도 된다. 이것은 펑터 `f`로부터 만들어지는 *자유 모나드(free monad)*로 알려져 있다.

### 직관

`(>>=)`의 타입을 더 자세히 보자. 기본 직관은 두 계산을 하나의 더 큰 계산으로 결합한다는 것이다. 첫 번째 인자 `m a`는 첫 번째 계산이다. 그런데 두 번째 인자가 그냥 `m b`였다면 재미없었을 것이다. 그러면 두 계산이 서로 상호작용할 방법이 전혀 없다(사실 이것이 정확히 `Applicative`의 상황이다). 그래서 `(>>=)`의 두 번째 인자는 타입 `a -> m b`를 갖는다. 이 타입의 함수는 첫 번째 계산의 *결과*가 주어지면 실행할 두 번째 계산을 만들어 낼 수 있다. 다시 말해 `x >>= k`는 `x`를 실행한 다음 `x`의 결과(들)를 사용해 무슨 계산을 두 번째로 실행할지 *결정*하고, 그 두 번째 계산의 출력을 전체 계산의 결과로 삼는 계산이다.

> **참고**: 사실 Haskell은 일반 재귀를 허용하므로 *무한한* 문법을 재귀적으로 구성할 수 있고, 따라서 `Applicative`(와 `Alternative`)만으로도 유한 알파벳 위의 어떤 문맥 의존 언어든 파싱할 수 있다. [Parsing context-sensitive languages with Applicative](http://byorgey.wordpress.com/2012/01/05/parsing-context-sensitive-languages-with-applicative/)를 보라.

직관적으로, 앞선 계산의 출력을 사용해 다음에 실행할 계산을 결정하는 바로 이 능력이 `Monad`를 `Applicative`보다 강력하게 만든다. `Applicative` 계산의 구조는 고정되어 있지만, `Monad` 계산의 구조는 중간 결과에 따라 바뀔 수 있다. 이는 또한 `Applicative` 인터페이스로 만든 파서는 문맥 자유 언어만 파싱할 수 있고, 문맥 의존 언어를 파싱하려면 `Monad` 인터페이스가 필요하다는 뜻이기도 하다.

`Monad`의 커진 힘을 다른 관점에서 보기 위해, `(>>=)`를 `fmap`, `pure`, `(<*>)`로 구현하려 하면 무슨 일이 벌어지는지 보자. 타입 `m a`의 값 `x`와 타입 `a -> m b`의 함수 `k`가 주어져 있으니, 할 수 있는 유일한 일은 `k`를 `x`에 적용하는 것이다. 물론 직접 적용할 수는 없고, `fmap`으로 `m` 위로 들어 올려야 한다. 그런데 `fmap k`의 타입은 무엇인가? `m a -> m (m b)`다. `x`에 적용하고 나면 타입 `m (m b)`의 무언가가 남는데—여기서 막힌다. 정말 원하는 것은 `m b`이지만 여기서 거기로 갈 방법이 없다. `pure`로 `m`을 *추가*할 수는 있지만, 여러 `m`을 하나로 *접을(collapse)* 방법이 없다.

> **참고**: `return`, `fmap`, `join`을 사용한 정의가 "수학의 정의"이고 `return`과 `(>>=)`를 사용한 정의는 Haskell 특유의 것이라고 주장하는 사람들이 있다. 사실 두 정의 모두 Haskell이 모나드를 받아들이기 한참 전부터 수학계에 알려져 있었다.

여러 `m`을 접는 이 능력이 정확히 함수 `join :: m (m a) -> m a`가 제공하는 능력이며, `Monad`의 대안적 정의를 `join`으로 줄 수 있다는 것은 놀랍지 않다.

```haskell
class Applicative m => Monad'' m where
  join :: m (m a) -> m a
```

실제로 범주론에서 모나드의 표준적 정의는 `return`, `fmap`, `join`(수학 문헌에서는 흔히 η, T, μ라고 부른다)을 사용한다. Haskell은 사용하기에 더 편리하다는 이유로 `join` 대신 `(>>=)`를 사용하는 다른 정식화를 채택했다. 그러나 `join`이 더 "원자적인" 연산이기 때문에, 때로는 `Monad` 인스턴스를 `join`으로 생각하는 편이 더 쉬울 수 있다. (예를 들어 리스트 모나드의 `join`은 그냥 `concat`이다.)

**연습문제**

1. `(>>=)`를 `fmap`(또는 `liftM`)과 `join`으로 구현하라.
2. 이번에는 `join`과 `fmap`(`liftM`)을 `(>>=)`와 `return`으로 구현하라.

### 유틸리티 함수

[`Control.Monad`](https://hackage.haskell.org/package/base/docs/Control-Monad.html) 모듈은 편리한 유틸리티 함수를 아주 많이 제공하는데, 전부 기본 `Monad` 연산(특히 `return`과 `(>>=)`)으로 구현할 수 있다. 그중 하나인 `join`은 이미 보았다. 여기서 주목할 만한 것 몇 개를 더 언급한다. 이 유틸리티 함수들을 직접 구현해 보는 것은 좋은 연습이다. 해설과 예제 코드가 곁들여진 더 자세한 안내는 Henk-Jan van Tuyl의 [투어](https://web.archive.org/web/20201109033750/members.chello.nl/hjgtuyl/tourdemonad.html)를 보라.

* `liftM :: Monad m => (a -> b) -> m a -> m b`. 익숙할 것이다. 물론 그냥 `fmap`이다. `fmap`과 `liftM`이 둘 다 있는 것은 `Monad` 타입 클래스가 최근까지 `Functor` 인스턴스를 요구하지 않았던 결과다. 수학적으로 모든 모나드는 펑터인데도 말이다. GHC 7.10 이상을 쓴다면 `liftM`은 피하고 그냥 `fmap`을 써야 한다.

* `ap :: Monad m => m (a -> b) -> m a -> m b`도 익숙할 것이다. `(<*>)`와 동등하며, `Monad` 인터페이스가 `Applicative`보다 엄격히 더 강력하다는 주장을 정당화한다. `pure = return`, `(<*>) = ap`으로 두면 어떤 `Monad`든 `Applicative`의 인스턴스로 만들 수 있다.

* `sequence :: Monad m => [m a] -> m [a]`는 계산들의 리스트를 받아, 그 결과들의 리스트를 수집하는 하나의 계산으로 결합한다. `sequence`에 `Monad` 제약이 있는 것도 일종의 역사적 사고다. 실제로는 `Applicative`만으로 구현할 수 있다(Applicative의 유틸리티 함수 절 끝의 연습문제 참고). 실제 `sequence`의 타입은 더 일반적이어서 리스트뿐 아니라 임의의 `Traversable`에 대해 동작한다. [`Traversable` 절](014-typeclassopedia-part2.md#traversable)을 보라.

* `replicateM :: Monad m => Int -> m a -> m [a]`는 단순히 [`replicate`](https://hackage.haskell.org/package/base/docs/Prelude.html#v:replicate)와 `sequence`의 조합이다.

* `mapM :: Monad m => (a -> m b) -> [a] -> m [b]`는 첫 인자를 두 번째 인자 위에 매핑한 뒤 결과를 `sequence`한다. `forM` 함수는 인자 순서를 뒤집은 `mapM`일 뿐이다. `forM`이라고 불리는 이유는 일반화된 `for` 루프를 모델링하기 때문이다. 리스트 `[a]`가 루프 인덱스를 제공하고, 함수 `a -> m b`가 각 인덱스에 대한 루프 "본문"을 지정한다. 이 함수들 역시 리스트뿐 아니라 임의의 `Traversable`에 대해 동작하며, `Monad`가 아닌 `Applicative`로도 정의할 수 있다. `Applicative`용 `mapM`의 대응물은 `traverse`라고 부른다.

* `(=<<) :: Monad m => (a -> m b) -> m a -> m b`는 인자 순서를 뒤집은 `(>>=)`일 뿐이다. 함수 적용과 더 닮아 있어서 이 방향이 더 편리할 때가 있다.

* `(>=>) :: Monad m => (a -> m b) -> (b -> m c) -> a -> m c`는 함수 합성과 비슷하지만, 각 함수의 결과 타입에 `m`이 하나씩 더 붙어 있고 인자 순서가 바뀌어 있다. 이 연산에 대해서는 나중에 더 이야기한다. 뒤집힌 변형인 `(<=<)`도 있다.

이 함수들 중 다수에는 `sequence_`, `mapM_`처럼 "밑줄" 변형이 있다. 이 변형들은 인자로 전달된 계산의 결과를 버리고 효과만을 위해 사용한다.

그 밖에 가끔 유용한 모나드 함수로 `filterM`, `zipWithM`, `foldM`, `forever`가 있다.

### 법칙

`Monad`의 인스턴스가 만족해야 하는 법칙이 몇 가지 있다([Monad laws](https://wiki.haskell.org/Monad_laws) 위키 페이지도 보라). 표준적인 제시는 다음과 같다.

```haskell
return a >>= k  =  k a
m >>= return    =  m
m >>= (\x -> k x >>= h)  =  (m >>= k) >>= h
```

첫 번째와 두 번째 법칙은 `return`이 얌전히 동작한다는 사실을 표현한다. 값 `a`를 `return`으로 모나드 문맥에 주입한 뒤 `k`에 바인딩하는 것은 처음부터 `k`를 `a`에 적용하는 것과 같고, 계산 `m`을 `return`에 바인딩해도 아무것도 바뀌지 않는다. 세 번째 법칙은 본질적으로 `(>>=)`가, 말하자면, 결합적이라는 것이다.

> **참고**: 나는 이 연산자를 "fish(물고기)"라고 읽는 것을 좋아한다.

그런데 위 법칙들의 제시는, 특히 세 번째는, `(>>=)`의 비대칭성 때문에 볼품이 없다. 법칙을 들여다봐도 정말 무슨 말인지 알기 어렵다. 나는 `(>=>)`로 정식화된 훨씬 우아한 버전의 법칙을 선호한다. `(>=>)`는 타입 `a -> m b`와 `b -> m c`의 두 함수를 "합성"한다는 것을 기억하자. 타입 `a -> m b`의 무언가는 (대략) `a`에서 `b`로 가되 `m`에 해당하는 문맥에서 어떤 효과를 일으킬 수도 있는 함수라고 생각할 수 있다. `(>=>)`는 이런 "효과 있는 함수들"을 합성하게 해 주며, 우리는 `(>=>)`가 어떤 성질을 갖는지 알고 싶다. `(>=>)`로 다시 쓴 모나드 법칙은 다음과 같다.

```haskell
return >=> g  =  g
g >=> return  =  g
(g >=> h) >=> k  =  g >=> (h >=> k)
```

> **참고**: 범주론 팬이라면 눈치챘겠지만, 이 법칙들은 정확히 타입 `a -> m b`의 함수들이 `(>=>)`를 합성으로 갖는 범주의 사상(arrow)이라는 말이다! 실제로 이것은 모나드 `m`의 *클라이슬리(Kleisli) 범주*로 알려져 있다. `Arrow`를 논의할 때 다시 등장한다.

아, 훨씬 낫다! 법칙들은 그저 `return`이 `(>=>)`의 항등원이고 `(>=>)`가 결합적이라고 말한다.

`fmap`, `return`, `join`으로 정식화한 모나드 법칙도 있다. 이 정식화에 대한 논의는 Haskell [wikibook의 범주론 페이지](http://en.wikibooks.org/wiki/Haskell/Category_theory)를 보라.

**연습문제**

1. 정의 `g >=> h = \x -> g x >>= h`가 주어졌을 때, 위 법칙들과 통상적인 모나드 법칙의 동등성을 증명하라.

### `do` 표기법

Haskell의 특별한 `do` 표기법은 모나드 표현식 사슬에 대한 문법 설탕을 제공함으로써 "명령형 스타일" 프로그래밍을 지원한다. 이 표기법의 기원은 `a >>= \x -> b >> c >>= \y -> d` 같은 것을 잇따르는 계산을 별도의 줄에 놓아 더 읽기 좋게 쓸 수 있다는 깨달음에 있다.

```haskell
a >>= \x ->
b >>
c >>= \y ->
d
```

이렇게 쓰면 전체 계산이 네 계산 `a`, `b`, `c`, `d`로 이루어져 있고, `x`는 `a`의 결과에, `y`는 `c`의 결과에 바인딩된다는 점이 강조된다(`b`, `c`, `d`는 `x`를 참조할 수 있고 `d`는 `y`도 참조할 수 있다). 여기서 더 좋은 표기법을 상상하는 것은 어렵지 않다.

```haskell
do { x <- a
   ;      b
   ; y <- c
   ;      d
   }
```

(중괄호와 세미콜론은 생략해도 된다. Haskell 파서가 레이아웃을 사용해 어디에 삽입해야 할지 결정한다.) 이 논의로 `do` 표기법이 그저 문법 설탕이라는 것이 분명해졌을 것이다. 실제로 `do` 블록은 (거의) 다음과 같이 재귀적으로 모나드 연산으로 번역된다.

```
                  do e → e
       do { e; stmts } → e >> do { stmts }
  do { v <- e; stmts } → e >>= \v -> do { stmts }
do { let decls; stmts} → let decls in do { stmts }
```

`v`가 변수가 아니라 패턴일 수도 있으므로 이것이 이야기의 전부는 아니다. 예를 들어 이렇게 쓸 수 있다.

```haskell
do (x:xs) <- foo
   bar x
```

그런데 `foo`가 빈 리스트라면 무슨 일이 일어날까? `[]`에는 `MonadFail`의 [인스턴스](https://hackage.haskell.org/package/base-4.14.0.0/docs/src/Control.Monad.Fail.html#fail)가 있고, `MonadFail` 인스턴스의 `fail` 함수가 그 `do` 표현식의 값으로 사용된다. [다른 모노이드적 클래스에 관한 절](014-typeclassopedia-part2.md#실패와-선택-alternative-monadplus-arrowplus)의 `MonadPlus`와 `MonadZero` 논의도 보라.

직관에 대한 마지막 참고: `do` 표기법은 "컨테이너" 관점보다 "계산 문맥" 관점에 강하게 기대고 있다. 바인딩 표기 `x <- m`이 `m`에서 하나의 `x`를 "추출"해 뭔가를 하는 것처럼 보이기 때문이다. 하지만 `m`은 리스트나 트리 같은 컨테이너를 나타낼 수도 있으며, `x <- m`의 의미는 전적으로 `(>>=)`의 구현에 달려 있다. 예를 들어 `m`이 리스트라면 `x <- m`은 실제로 `x`가 리스트의 각 값을 차례로 갖게 된다는 뜻이다.

#### ApplicativeDo

`do` 표기법을 탈설탕(desugar)하는 데 `Monad`의 온전한 힘이 필요하지 않을 때도 있다. 예를 들어

```haskell
do x <- foo1
   y <- foo2
   z <- foo3
   return (g x y z)
```

는 보통 `foo1 >>= \x -> foo2 >>= \y -> foo3 >>= \z -> return (g x y z)`로 탈설탕되지만, 이는 `g <$> foo1 <*> foo2 <*> foo3`과 동등하다. `ApplicativeDo` 확장을 켜면(GHC 8.0부터) GHC는 가능한 곳마다 `Applicative` 연산을 사용해 `do` 블록을 탈설탕하려고 최선을 다한다. 이는 `Monad` 인스턴스가 있는 타입에서도 효율 향상으로 이어질 수 있다. 일반적으로 `Applicative` 계산은 병렬로 실행될 수 있지만 모나드 계산은 그럴 수 없기 때문이다. 예를 들어 다음을 보자.

```haskell
g :: Int -> Int -> M Int

-- 비쌀 수 있는 계산들
bar, baz :: M Int

foo :: M Int
foo = do
  x <- bar
  y <- baz
  g x y
```

`foo`는 분명히 `M`의 `Monad` 인스턴스에 의존한다. 전체 계산이 일으키는 효과가 (`g`를 거쳐) `bar`와 `baz`의 `Int` 출력에 의존할 수 있기 때문이다. 그럼에도 `ApplicativeDo`를 켜면 `foo`는 다음과 같이 탈설탕될 수 있다.

```haskell
join (g <$> bar <*> baz)
```

이렇게 하면 `bar`와 `baz`는 적어도 서로에게는 의존하지 않으므로 병렬로 계산될 수 있다.

`ApplicativeDo` 확장은 [이 위키 페이지](https://ghc.haskell.org/trac/ghc/wiki/ApplicativeDo)에, 더 자세하게는 [이 Haskell Symposium 논문](http://doi.org/10.1145/2976002.2976007)에 기술되어 있다.

#### QualifiedDo

[GHC Proposal #216](https://github.com/ghc-proposals/ghc-proposals/blob/master/proposals/0216-qualified-do.rst)은 QualifiedDo 확장을 추가하며, GHC 9.0.1에서 사용할 수 있다.

`-XQualifiedDo`를 켜면 `[modid.]do` 문법을 쓸 수 있다. 여기서 `modid`는 모듈 이름이다.

이제 `x <- u` 문은 `(modid.>>=)`를 사용한다. 예를 들어

```
M.do { x <- u; stmts }
```

는 (새 확장 없이) 다음과 같이 쓰는 것과 같다.

```
u M.>>= \x -> M.do { stmts }
```

다른 모나드 메서드들도 다뤄진다. 자세한 내용은 제안서를 보라.

### 더 읽을거리

Philip Wadler는 함수형 프로그램을 구조화하는 데 모나드를 사용하자고 처음 제안한 사람이다. [그의 논문](http://homepages.inf.ed.ac.uk/wadler/topics/monads.html)은 지금도 읽을 만한 입문서다.

> **참고**: [All About Monads](https://wiki.haskell.org/All_About_Monads), [Monads as containers](http://www.haskell.org/haskellwiki/Monads_as_Containers), [Understanding monads](http://en.wikibooks.org/w/index.php?title=Haskell/Understanding_monads), [The Monadic Way](https://wiki.haskell.org/The_Monadic_Way), [You Could Have Invented Monads! (And Maybe You Already Have.)](http://blog.sigfpe.com/2006/08/you-could-have-invented-monads-and.html), [there's a monster in my Haskell!](http://www.haskell.org/pipermail/haskell-cafe/2006-November/019190.html), [Understanding Monads. For real.](http://kawagner.blogspot.com/2007/02/understanding-monads-for-real.html), [Monads in 15 minutes: Backtracking and Maybe](http://www.randomhacks.net/articles/2007/03/12/monads-in-15-minutes), [Monads as computation](http://www.haskell.org/haskellwiki/Monads_as_computation), [Practical Monads](http://metafoo.co.uk/practical-monads.txt)

물론 품질이 제각각인 수많은 모나드 튜토리얼이 있다.

가장 좋은 것 몇 가지로는 Cale Gibbard의 [Monads as containers](http://www.haskell.org/haskellwiki/Monads_as_Containers)와 [Monads as computation](http://www.haskell.org/haskellwiki/Monads_as_computation), 풍부한 예제를 갖춘 종합 안내서인 Jeff Newbern의 [All About Monads](https://wiki.haskell.org/All_About_Monads), 그리고 훌륭한 연습문제가 있는 Dan Piponi의 [You Could Have Invented Monads!](http://blog.sigfpe.com/2006/08/you-could-have-invented-monads-and.html)가 있다. `IO` 사용법만 알고 싶다면 [Introduction to IO](https://wiki.haskell.org/Introduction_to_IO)를 참고하면 된다. 이것도 일부일 뿐이다. [monad tutorials timeline](https://wiki.haskell.org/Monad_tutorials_timeline)이 더 완전한 목록이다. (이 모든 모나드 튜토리얼은 [think of a monad ...](http://koweycode.blogspot.com/2007/01/think-of-monad.html) 같은 패러디와, [Monads! (and Why Monad Tutorials Are All Awful)](http://ahamsandwich.wordpress.com/2007/07/26/monads-and-why-monad-tutorials-are-all-awful/)이나 [Abstraction, intuition, and the "monad tutorial fallacy"](http://byorgey.wordpress.com/2009/01/12/abstraction-intuition-and-the-monad-tutorial-fallacy/) 같은 다른 종류의 반발을 낳았다.)

꼭 튜토리얼은 아니지만 좋은 모나드 자료로는 `Control.Monad`의 함수들을 다룬 [Henk-Jan van Tuyl의 투어](https://web.archive.org/web/20201109033750/members.chello.nl/hjgtuyl/tourdemonad.html), Dan Piponi의 [field guide](http://blog.sigfpe.com/2006/10/monads-field-guide.html), Tim Newsham의 [What's a Monad?](http://www.thenewsh.com/~newsham/haskell/monad.html), 그리고 Chris Smith의 훌륭한 글 [Why Do Monads Matter?](http://cdsmith.wordpress.com/2012/04/18/why-do-monads-matter/)가 있다. 모나드의 다양한 측면에 대해 쓰인 블로그 글도 많다. 링크 모음은 [Blog articles/Monads](https://wiki.haskell.org/Blog_articles/Monads)에서 찾을 수 있다.

모나드를 밑바닥부터 구성하는 데 도움이 필요하거나, 예컨대 도메인 특화 언어를 컴파일하는 데 적합한 모나드 연산의 "deep embedding"을 얻고 싶다면 [Apfelmus의 operational 패키지](http://projects.haskell.org/operational)를 보라.

`Monad` 클래스와 Haskell 타입 시스템의 기벽 하나는, 수학적 관점에서는 모나드인데도 데이터에 클래스 제약이 필요한 타입에 대해서는 `Monad` 인스턴스를 곧바로 선언할 수 없다는 것이다. 예를 들어 `Data.Set`은 데이터에 `Ord` 제약을 요구하므로 쉽게 `Monad` 인스턴스로 만들 수 없다. 이 문제의 해법은 [Eric Kidd가 처음 기술](http://www.randomhacks.net/articles/2007/03/15/data-set-monad-haskell-macros)했고, 이후 Ganesh Sittampalam과 Peter Gavin이 [rmonad라는 라이브러리](http://hackage.haskell.org/cgi-bin/hackage-scripts/package/rmonad)로 만들었다.

`do` 표기법을 피할 좋은 이유도 많다. 어떤 이들은 [해롭다고 여기는](https://wiki.haskell.org/Do_notation_considered_harmful) 데까지 나아갔다.

모나드는 다양한 방식으로 일반화할 수 있다. 한 가지 가능성에 대한 설명으로는 Robert Atkey의 [parameterized monads 논문](https://bentnib.org/paramnotions-jfp.pdf)이나 Dan Piponi의 [Beyond Monads](http://blog.sigfpe.com/2009/02/beyond-monads.html)를 보라.

범주론에 기울어 있는 독자를 위해: 모나드는 모노이드로 볼 수도 있고([From Monoids to Monads](http://blog.sigfpe.com/2008/11/from-monoids-to-monads.html)), 폐포 연산자(closure operator)로 볼 수도 있다([Triples and Closure](http://blog.plover.com/math/monad-closure.html)). [Monad.Reader 13호](http://www.haskell.org/wikiupload/8/85/TMR-Issue13.pdf)에 실린 Derek Elkins의 글은 `State`와 `Cont` 같은 표준 `Monad` 인스턴스들의 범주론적 토대를 설명한다. Jonathan Hill과 Keith Clarke의 [초기 논문](https://web.archive.org/web/20140728233300/http://citeseerx.ist.psu.edu/viewdoc/download?doi=10.1.1.53.6497&rep=rep1&type=pdf)은 범주론에서 등장하는 모나드와 함수형 프로그래밍에서 쓰이는 모나드 사이의 연결을 설명한다. IO 모나드의 역사를 설명하는 [Oleg Kiselyov의 웹 페이지](http://okmij.org/ftp/Computation/IO-monad-history.html)도 있다.

모나드에 관련된 훨씬 많은 연구 논문 링크는 [Research papers/Monads and arrows](https://wiki.haskell.org/Research_papers/Monads_and_arrows)에서 찾을 수 있다.

---

[Part 2에서 계속: MonadFail, 모나드 트랜스포머, MonadFix, Semigroup, Monoid, Alternative, Foldable, Traversable](014-typeclassopedia-part2.md)
