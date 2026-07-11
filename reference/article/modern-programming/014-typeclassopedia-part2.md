# Typeclassopedia (2/3): MonadFail부터 Traversable까지

> 원문: [Typeclassopedia](https://wiki.haskell.org/Typeclassopedia)
> 저자: Brent Yorgey | 번역일: 2026-07-10

전체 목차:

- [Part 1](014-typeclassopedia-part1.md): 초록, 서론, Functor, Applicative, Monad
- **Part 2 (이 문서)**: MonadFail, 모나드 트랜스포머, MonadFix, Semigroup, Monoid, 실패와 선택(Alternative, MonadPlus, ArrowPlus), Foldable, Traversable
- [Part 3](014-typeclassopedia-part3.md): Bifunctor, Category, Arrow, Comonad, 감사의 말, 저자 소개, 콜로폰

## MonadFail

어떤 모나드들은 `MonadPlus`가 시사하는 *복구(recovery)* 개념까지는 아니더라도 *실패(failure)* 개념을 지원하며, 원시적인 에러 보고 메커니즘을 포함하기도 한다. 이 개념은 비교적 원칙 없는 클래스인 `MonadFail`로 표현된다. GHC 8.8부터 `do` 바인딩의 패턴 매치 실패에는 `MonadFail`의 `fail` 메서드가 사용된다. 그러나 `Reader`, `State`, `Writer`, `RWST`, `Cont`처럼 정당한 `fail` 메서드를 지원할 수 없는 모나드도 많으며, 이들에게는 `MonadFail` 인스턴스가 없다.

`fail` 메서드의 역사는 [MonadFail 제안](https://gitlab.haskell.org/haskell/prime/-/wikis/libraries/proposals/monad-fail)을 보라.

### 정의

```haskell
class Monad m => MonadFail m where
  fail :: String -> m a
```

### 법칙

```haskell
fail s >>= m = fail s
```

## 모나드 트랜스포머

두 모나드를 하나로 결합하고 싶을 때가 종종 있다. 예를 들어 상태가 있으면서 비결정적인 계산(`State` + `[]`)이라든가, 실패할 수 있으면서 읽기 전용 환경을 참조할 수 있는 계산(`Maybe` + `Reader`) 같은 것 말이다. 아쉽게도 모나드는 applicative functor만큼 곱게 합성되지 않는다(`Monad`의 온전한 힘이 필요 없다면 `Applicative`를 쓸 또 하나의 이유다). 그래도 어떤 모나드들은 특정한 방식으로 결합할 수 있다.

### 표준 모나드 트랜스포머

[transformers](http://hackage.haskell.org/package/transformers) 라이브러리는 여러 표준 *모나드 트랜스포머(monad transformer)*를 제공한다. 각 모나드 트랜스포머는 기존의 어떤 모나드에든 특정한 능력/기능/효과를 추가한다.

* [`IdentityT`](http://hackage.haskell.org/packages/archive/transformers/latest/doc/html/Control-Monad-Trans-Identity.html)는 항등 트랜스포머로, 모나드를 (그것과 동형인) 자기 자신으로 사상한다. 얼핏 쓸모없어 보이지만, `id` 함수가 유용한 것과 같은 이유로 유용하다—임의의 모나드 트랜스포머에 대해 매개변수화된 것에, 실제로는 아무 추가 능력도 원하지 않을 때 인자로 넘길 수 있다.
* [`StateT`](http://hackage.haskell.org/packages/archive/transformers/latest/doc/html/Control-Monad-Trans-State.html)는 읽고 쓸 수 있는 상태를 추가한다.
* [`ReaderT`](http://hackage.haskell.org/packages/archive/transformers/latest/doc/html/Control-Monad-Trans-Reader.html)는 읽기 전용 환경을 추가한다.
* [`WriterT`](http://hackage.haskell.org/packages/archive/transformers/latest/doc/html/Control-Monad-Trans-Writer.html)는 쓰기 전용 로그를 추가한다.
* [`RWST`](http://hackage.haskell.org/packages/archive/transformers/0.2.2.0/doc/html/Control-Monad-Trans-RWS.html)는 `ReaderT`, `WriterT`, `StateT`를 하나로 편리하게 결합한다.
* [`MaybeT`](http://hackage.haskell.org/packages/archive/transformers/latest/doc/html/Control-Monad-Trans-Maybe.html)는 실패 가능성을 추가한다.
* [`ErrorT`](http://hackage.haskell.org/packages/archive/transformers/latest/doc/html/Control-Monad-Trans-Error.html)는 에러를 임의의 타입으로 표현하는 실패 가능성을 추가한다.
* [`ListT`](http://hackage.haskell.org/packages/archive/transformers/latest/doc/html/Control-Monad-Trans-List.html)는 비결정성을 추가한다(다만 아래의 `ListT` 논의를 보라).
* [`ContT`](http://hackage.haskell.org/packages/archive/transformers/latest/doc/html/Control-Monad-Trans-Cont.html)는 컨티뉴에이션 처리를 추가한다.

예를 들어 `StateT s Maybe`는 `Monad`의 인스턴스다. 타입 `StateT s Maybe a`의 계산은 실패할 수 있으며, 타입 `s`의 변경 가능한 상태에 접근할 수 있다. 모나드 트랜스포머는 여러 겹으로 쌓을 수 있다. 모나드 트랜스포머를 쓸 때 기억해 둘 한 가지는 합성 순서가 중요하다는 것이다. 예를 들어 `StateT s Maybe a` 계산이 실패하면 상태 갱신이 멈춘다(실은 상태가 그냥 사라진다). 반면 `MaybeT (State s) a` 계산의 상태는 계산이 "실패"한 뒤에도 계속 수정될 수 있다. 거꾸로인 것 같지만 이게 맞다. 모나드 트랜스포머는 합성 모나드를 "안팎이 뒤집힌" 방식으로 쌓는다. `MaybeT (State s) a`는 `s -> (Maybe a, s)`와 동형이다. (Lambdabot에는 모나드 트랜스포머 스택을 이런 식으로 "풀어" 볼 수 있는 없어서는 안 될 `@unmtl` 명령이 있다.) 직관적으로, 스택 안쪽으로 들어갈수록 모나드는 "더 근본적"이 되고, 안쪽 모나드의 효과가 바깥쪽 모나드의 효과보다 "우선한다". 물론 이것은 두루뭉술한 이야기일 뿐이고, 결합하려는 모나드들의 올바른 순서가 확실치 않다면 `@unmtl`을 쓰거나 여러 선택지를 그냥 직접 시험해 보는 것이 최선이다.

### 정의와 법칙

모든 모나드 트랜스포머는 `Control.Monad.Trans.Class`에 정의된 `MonadTrans` 타입 클래스를 구현해야 한다.

```haskell
class MonadTrans t where
  lift :: Monad m => m a -> t m a
```

이는 기반 모나드 `m`의 임의의 계산을 변환된 모나드 `t m`의 계산으로 "들어 올릴" 수 있게 해 준다. (타입 적용은 함수 적용처럼 왼쪽 결합이므로 `t m a = (t m) a`라는 점에 유의하라.)

`lift`는 다음 법칙을 만족해야 한다.

```haskell
lift . return   =  return
lift (m >>= f)  =  lift m >>= (lift . f)
```

직관적으로 이 법칙들은 `lift`가 `m a` 계산을 `t m a` 계산으로 "온당한" 방식으로 변환한다는 것, 즉 `m`의 `return`과 `(>>=)`를 `t m`의 `return`과 `(>>=)`로 보낸다는 것을 말한다.

**연습문제**

1. `MonadTrans` 선언에서 `t`의 카인드는 무엇인가?

### 트랜스포머 타입 클래스와 "능력(capability)" 스타일

> **참고**: 이 방식의 유일한 문제는 표준 모나드 트랜스포머의 수가 늘어남에 따라 필요한 인스턴스 수가 제곱으로 늘어난다는 것이다—하지만 현재의 표준 모나드 트랜스포머 집합이 대부분의 흔한 사용 사례에 충분해 보이므로 그리 큰 문제는 아닐 수 있다.

각 트랜스포머의 연산에 대한 타입 클래스도 ([`mtl` 패키지](http://hackage.haskell.org/package/mtl)가) 제공한다. 예를 들어 `MonadState` 타입 클래스는 상태 전용 메서드 `get`과 `put`을 제공하는데, 이 메서드들을 `State`뿐 아니라 `MonadState`의 인스턴스인 어떤 모나드와도—`MaybeT (State s)`, `StateT s (ReaderT r IO)` 등—편리하게 쓸 수 있다. `Reader`, `Writer`, `Cont`, `IO` 등에도 비슷한 타입 클래스가 있다.

이 타입 클래스들은 두 가지 목적에 봉사한다. 첫째, 명시적인 `lift` 사용의 필요를 (대부분) 없애 주며, 필요한 `lift` 호출 횟수를 타입이 이끄는 방식으로 자동으로 결정해 준다. 그냥 `put`이라고 쓰면, 사용 중인 구체적인 모나드 스택에 따라 `lift . put`, `lift . lift . put` 등으로 자동으로 번역된다.

둘째, 서로 다른 구체적 모나드 스택 사이를 오갈 유연성을 더 준다. 예를 들어 상태 기반 알고리즘을 쓴다면

```haskell
foo :: State Int Char
foo = modify (*2) >> return 'x'
```

라고 쓰지 말고 이렇게 쓰라.

```haskell
foo :: MonadState Int m => m Char
foo = modify (*2) >> return 'x'
```

이제 나중에 실패 가능성을 도입해야 한다는 것을 깨달으면 `State Int`에서 `MaybeT (State Int)`로 바꿀 수 있다. 첫 번째 버전의 `foo`는 이 변경을 반영해 타입을 수정해야 하지만, 두 번째 버전의 `foo`는 그대로 쓸 수 있다.

그러나 이런 "능력 기반" 스타일(예컨대 `foo`가 "상태 능력"이 있는 어떤 모나드에서든 동작한다고 명시하는 것)은 순진하게 확장하려 하면 금방 문제에 부딪힌다. 예를 들어 서로 독립적인 상태 두 개를 유지해야 한다면? 이 문제와 관련 문제들을 푸는 프레임워크가 Schrijvers와 Olivera의 논문([Monads, zippers and views: virtualizing the monad stack, ICFP 2011](http://users.ugent.be/~tschrijv/Research/papers/icfp2011.pdf))에 기술되어 있으며 [`Monatron` 패키지](http://hackage.haskell.org/package/Monatron)에 구현되어 있다.

### 모나드 합성하기

두 모나드의 합성은 항상 모나드인가? 앞서 암시했듯 답은 아니오다.

`Applicative` functor는 합성에 닫혀 있으므로 문제는 `join`에 있을 수밖에 없다. 실제로 `m`과 `n`이 임의의 모나드라고 하자. 이들의 합성으로 모나드를 만들려면

```haskell
join :: m (n (m (n a))) -> m (n a)
```

를 구현할 수 있어야 하는데, 일반적으로 이를 어떻게 할 수 있을지 분명치 않다. `m`의 `join` 메서드는 도움이 안 된다. 두 개의 `m`이 서로 붙어 있지 않기 때문이다(`n`도 마찬가지다).

그러나 가능한 상황이 하나 있는데, `n`이 `m` 위로 *분배(distribute)*되는 경우, 즉 특정 법칙들을 만족하는 함수

```haskell
distrib :: n (m a) -> m (n a)
```

가 있는 경우다. Jones와 Duponcheel의 [Composing Monads](https://web.cecs.pdx.edu/~mpj/pubs/RR-1004.pdf)를 보라. [Traversable 절](#traversable)도 참고하라.

모나드가 합성에 닫혀 있지 않다는 것에 대한 훨씬 깊은 논의와 분석은 [StackOverflow의 이 질문](http://stackoverflow.com/questions/13034229/concrete-example-showing-that-monads-are-not-closed-under-composition-with-proo?lq=1)을 보라.

**연습문제**

* `distrib :: N (M a) -> M (N a)`가 주어지고 `M`과 `N`이 `Monad`의 인스턴스라고 가정할 때, `join :: M (N (M (N a))) -> M (N a)`를 구현하라.

### 더 읽을거리

모나드 트랜스포머 라이브러리(원래는 [`mtl`](http://hackage.haskell.org/package/mtl), 지금은 `mtl`과 [`transformers`](http://hackage.haskell.org/package/transformers)로 나뉘어 있다)의 상당 부분—`Reader`, `Writer`, `State` 등의 모나드들과 모나드 트랜스포머 프레임워크 자체—은 Mark Jones의 고전적 논문 [Functional Programming with Overloading and Higher-Order Polymorphism](http://web.cecs.pdx.edu/~mpj/pubs/springschool.html)에서 영감을 얻었다. 거의 15년이 지난 지금도 매우 읽을 만하고, 읽기도 쉽다.

모나드 트랜스포머 패키지들(`mtl`, `transformers`, `monads-fd`, `monads-tf`)의 역사와 상호 관계에 대한 설명은 [Edward Kmett의 메일링 리스트 메시지](http://archive.fo/wxSkj)를 보라.

모나드 트랜스포머에 관한 훌륭한 자료가 둘 있다. Martin Grabmüller의 [Monad Transformers Step by Step](https://github.com/mgrabmueller/TransformersStepByStep/blob/master/Transformers.lhs)은 모나드 트랜스포머로 다양한 효과를 갖는 계산을 우아하게 조립하는 방법을 실행 가능한 예제와 함께 철저히 설명한다. [Cale Gibbard의 글](http://cale.yi.org/index.php/How_To_Use_Monad_Transformers)은 더 실용적으로, 모나드 트랜스포머를 사용하는 코드를 최대한 고통 없이 작성할 수 있도록 구조화하는 방법을 다룬다. 모나드 트랜스포머를 배우기 시작하기 좋은 또 다른 자료는 [Dan Piponi의 블로그 글](http://blog.sigfpe.com/2006/05/grok-haskell-monad-transformers.html)이다.

`transformers` 패키지의 `ListT` 트랜스포머에는 주의 사항이 있다. `ListT m`은 `m`이 *가환(commutative)*일 때만, 즉 `ma >>= \a -> mb >>= \b -> foo`가 `mb >>= \b -> ma >>= \a -> foo`와 동등할 때만(`m`의 효과 순서가 무관할 때만) 모나드다. 왜 그런지에 대한 한 가지 설명은 Dan Piponi의 블로그 글 ["Why isn't `ListT []` a monad"](http://blog.sigfpe.com/2006/11/why-isnt-listt-monad.html)를 보라. 더 많은 예제와, 이 문제가 없는 `ListT` 버전의 설계는 [`ListT` done right](http://www.haskell.org/haskellwiki/ListT_done_right)를 보라.

[Lüth와 Ghani](https://web.archive.org/web/20140721174732/http://citeseerx.ist.psu.edu/viewdoc/download?doi=10.1.1.8.3581&rep=rep1&type=pdf)가 기술한, 쌍대곱(coproduct)을 사용해 모나드를 합성하는 다른 방법도 있다. 흥미롭지만 (아직?) 널리 쓰이지는 않는다. 더 최근의 대안으로는 Kiselyov 등의 [Extensible Effects: An Alternative to Monad Transformers](http://okmij.org/ftp/Haskell/extensible/exteff.pdf)를 보라.

## MonadFix

*참고: `MonadFix`는 완전성을 위해 (그리고 흥미롭기 때문에) 실었지만, 그리 많이 쓰이지는 않는 것 같다. 처음 읽을 때 이 절을 건너뛰어도 전혀 문제없다(오히려 권장할 만하다).*

### `do rec` 표기법

`MonadFix` 클래스는 특별한 고정점 연산 `mfix :: (a -> m a) -> m a`를 지원하는 모나드를 기술한다. 이 연산은 모나드 계산의 출력을 (효과 있는) 재귀로 정의할 수 있게 해 준다. GHC에서는 `-XRecursiveDo` 플래그로 켜는 특별한 "recursive do" 표기법으로 [지원된다](https://downloads.haskell.org/~ghc/9.0.1/docs/html/users_guide/exts/recursive_do.html). `do` 블록 안에 다음처럼 중첩된 `rec` 블록을 둘 수 있다.

```haskell
do { x <- foo
   ; rec { y <- baz
         ; z <- bar
         ;      bob
         }
   ; w <- frob
   }
```

보통이라면(위 예제에서 `rec` 자리에 `do`가 있었다면) `y`는 `bar`와 `bob`의 스코프에는 있지만 `baz`의 스코프에는 없고, `z`는 `bob`의 스코프에만 있다. 그러나 `rec`을 쓰면 `y`와 `z` 모두 `baz`, `bar`, `bob` 셋 모두의 스코프에 있다. `rec` 블록은 다음과 같은 `let` 블록과 유사하다.

```haskell
let { y = baz
    ; z = bar
    }
in bob
```

Haskell에서 `let` 블록에 바인딩된 모든 변수는 블록 전체에서 스코프에 있기 때문이다. (이 관점에서 보면 Haskell의 보통 `do` 블록은 Scheme의 `let*` 구문과 유사하다.)

이런 기능은 어디에 쓸 수 있을까? `MonadFix`를 기술한 원 논문(아래 참고)의 동기 부여 예제 중 하나가 회로 기술의 인코딩이다. `do` 블록의 한 줄

```haskell
  x <- gate y z
```

은 입력 배선이 `y`와 `z`이고 출력 배선이 `x`인 게이트를 기술한다. 그런데 유용한 회로의 다수(대부분?)는 어떤 형태로든 피드백 루프를 포함하므로, 보통의 `do` 블록으로는 쓸 수 없다(어떤 배선은 출력으로 나열되기 *전에* 입력으로 언급되어야 하므로). `rec` 블록을 쓰면 이 문제가 해결된다.

### 예제와 직관

물론 모든 모나드가 이런 재귀적 바인딩을 지원하는 것은 아니다. 그러나 위에서 말했듯, 몇 가지 법칙을 만족하는 `mfix :: (a -> m a) -> m a`의 구현만 있으면 충분하다. `Maybe` 모나드에 대해 `mfix`를 구현해 보자. 즉 다음 함수를 구현하고 싶다.

```haskell
maybeFix :: (a -> Maybe a) -> Maybe a
```

> **참고**: 사실 `fix`는 효율상의 이유로 약간 다르게 구현되어 있지만, 여기 보인 정의는 그것과 동등하며 지금 목적에는 더 단순하다.

비모나드 함수 `fix :: (a -> a) -> a`의 구현을 잠시 생각해 보자.

```haskell
fix f = f (fix f)
```

`fix`에서 영감을 얻은 `maybeFix` 구현의 첫 시도는 이런 모습일 것이다.

```haskell
maybeFix :: (a -> Maybe a) -> Maybe a
maybeFix f = maybeFix f >>= f
```

타입은 맞다. 그런데 뭔가 이상하다. 여기에는 `Maybe`에 특유한 것이 전혀 없다. `maybeFix`는 실제로 더 일반적인 타입 `Monad m => (a -> m a) -> m a`를 갖는다. 그런데 방금 모든 모나드가 `mfix`를 지원하는 것은 아니라고 하지 않았나?

답은 이 `maybeFix` 구현이 타입은 맞지만 의도한 의미를 갖지 *않는다*는 것이다. `Maybe` 모나드에서 `(>>=)`가 어떻게 동작하는지(첫 인자를 패턴 매칭해 `Nothing`인지 `Just`인지 본다) 생각해 보면, 이 `maybeFix` 정의는 완전히 쓸모없다는 것을 알 수 있다. `Nothing`을 반환할지 `Just`를 반환할지 결정하려고 무한히 재귀할 뿐, `f` 쪽은 쳐다보지도 않는다.

요령은 `maybeFix`가 `Just`를 반환할 것이라고 그냥 *가정*하고 넘어가는 것이다!

```haskell
maybeFix :: (a -> Maybe a) -> Maybe a
maybeFix f = ma
  where ma = f (fromJust ma)
```

이는 `maybeFix`의 결과가 `ma`이고, `ma = Just x`라고 가정하면 `ma`가 (재귀적으로) `f x`와 같도록 정의된다는 말이다.

이게 왜 괜찮을까? `fromJust`는 `unsafePerformIO` 못지않게 나쁜 것 아닌가? 뭐, 보통은 그렇다. 이것이 거의 유일하게 정당화되는 상황이다! 주목할 흥미로운 점은 `maybeFix`가 *결코 크래시하지 않는다*는 것이다—물론 종료하지 않을 수는 있다. 크래시가 나려면 `ma = Nothing`임을 알면서 `fromJust ma`를 평가해야 한다. 그런데 어떻게 `ma = Nothing`임을 알 수 있을까? `ma`는 `f (fromJust ma)`로 정의되어 있으므로, 그렇다면 이 표현식이 이미 `Nothing`으로 평가되었어야 한다—그런데 그 경우라면 애초에 `fromJust ma`를 평가할 이유가 없다!

다른 관점에서 보기 위해 세 가지 가능성을 생각해 보자. 첫째, `f`가 인자를 보지 않고 `Nothing`을 내놓는다면 `maybeFix f`는 분명히 `Nothing`을 반환한다. 둘째, `f`가 항상 `Just x`를 내놓고 `x`가 인자에 의존한다면, 재귀는 유용하게 진행될 수 있다. `fromJust ma`가 `x`로 평가될 수 있으므로 `f`의 출력이 다시 입력으로 공급된다. 셋째, `f`가 인자를 사용해 `Just`를 내놓을지 `Nothing`을 내놓을지 결정하려 한다면 `maybeFix f`는 종료하지 않는다. `f`의 인자를 평가하려면 `ma`가 `Just`인지 보기 위해 `ma`를 평가해야 하고, 그러려면 `f (fromJust ma)`를 평가해야 하고, 그러려면 `ma`를 평가해야 하고, ... 하는 식이다.

리스트(`Maybe` 인스턴스와 유사하게 동작한다), `ST`, `IO`의 `MonadFix` 인스턴스도 있다. [`IO` 인스턴스](http://hackage.haskell.org/packages/archive/base/latest/doc/html/System-IO.html#fixIO)는 특히 재미있다. 새 (빈) `MVar`를 만들고, `unsafeInterleaveIO`(실제 읽기를 값이 필요할 때까지 게으르게 지연시킨다)로 그 내용을 즉시 읽은 다음, `MVar`의 내용을 사용해 새 값을 계산하고, 그것을 다시 `MVar`에 써넣는다. 마치 `mfix`가 `MVar`를 통해 값을 과거의 자신에게 보내는 것처럼 으스스해 보이지만, 물론 실제로 일어나는 일은 (`unsafeInterleaveIO`를 통해) 읽기가 프로세스를 부트스트랩하기에 딱 충분할 만큼 지연되는 것이다.

**연습문제**

* `[]`의 `MonadFix` 인스턴스를 구현하라.

### `mdo` 문법

이 절 시작의 예제는 이렇게도 쓸 수 있다.

```haskell
mdo { x <- foo
    ; y <- baz
    ; z <- bar
    ;      bob
    ; w <- frob
    }
```

이는 (예컨대 `bar`와 `bob`이 `y`를 참조한다고 가정하면) 원래 예제로 번역된다. 차이는 `mdo`가 코드를 분석해 최소 재귀 블록들을 찾아 `rec` 블록에 넣는 반면, `rec` 블록은 추가 분석 없이 곧바로 `mfix` 호출로 탈설탕된다는 것이다.

### 더 읽을거리

더 많은 정보(`rec` 블록의 정확한 탈설탕 규칙 등)는 Levent Erkök과 John Launchbury의 2002년 Haskell 워크숍 논문 [A Recursive do for Haskell](http://sites.google.com/site/leventerkok/recdo.pdf?attredirects=0)을, 전체 세부 사항은 Levent Erkök의 학위 논문 [Value Recursion in Monadic Computations](https://web.archive.org/web/20130429231549/http://citeseerx.ist.psu.edu/viewdoc/download?doi=10.1.1.15.1543&rep=rep1&type=pdf)을 보라. (읽을 때 `MonadFix`가 예전에는 `MonadRec`이라고 불렸다는 점에 유의하라.) [GHC 사용자 매뉴얼의 recursive do 표기법 절](https://downloads.haskell.org/ghc/latest/docs/html/users_guide/glasgow_exts.html#extension-RecursiveDo)도 읽어 볼 수 있다.

## Semigroup

반군(semigroup)은 집합 S와, S의 원소들을 결합하는 이항 연산 ⊕의 쌍이다. 연산자 ⊕는 결합적이어야 한다(즉 S의 임의의 원소 a, b, c에 대해 (a ⊕ b) ⊕ c = a ⊕ (b ⊕ c)).

예를 들어 자연수는 덧셈에 대해 반군을 이룬다. 두 자연수의 합은 자연수이고, 임의의 자연수 a, b, c에 대해 (a+b)+c = a+(b+c)이기 때문이다. 정수는 곱셈에 대해서도 반군을 이루고, 정수(또는 유리수, 실수)는 max나 min에 대해서도, 불리언 값은 논리곱과 논리합에 대해서도, 리스트는 이어붙이기에 대해서도, 한 집합에서 자기 자신으로 가는 함수들은 합성에 대해서도 반군을 이룬다... 찾아볼 줄 알게 되면 반군은 어디에나 나타난다.

### 정의

`base` 패키지 버전 4.9(GHC 8.0에 딸려 온다)부터 반군은 `Data.Semigroup` 모듈에 정의되어 있다. (이전 버전의 base로 작업하거나 이전 버전의 base를 지원하는 라이브러리를 쓰고 싶다면 `semigroups` 패키지를 쓸 수 있다.)

`Semigroup` 타입 클래스의 정의([haddock](https://hackage.haskell.org/package/base-4.9.0.0/docs/Data-Semigroup.html))는 다음과 같다.

```haskell
class Semigroup a where
  (<>) :: a -> a -> a

  sconcat :: NonEmpty a -> a
  sconcat (a :| as) = go a as where
    go b (c:cs) = b <> go c cs
    go b []     = b

  stimes :: Integral b => b -> a -> a
  stimes = ...
```

정말 중요한 메서드는 결합적 이항 연산을 나타내는 `(<>)`다. 나머지 두 메서드는 `(<>)`를 이용한 기본 구현이 있으며, 어떤 인스턴스가 기본 구현보다 효율적인 구현을 제공할 수 있는 경우를 위해 클래스에 포함되어 있다.

`sconcat`은 비어 있지 않은 리스트를 `(<>)`로 축약한다. 대부분의 인스턴스에서 이는 `foldr1 (<>)`과 같지만, 멱등(idempotent) 반군에서는 상수 시간이 될 수 있다.

`stimes n`은 `sconcat . replicate n`과 동등하다(하지만 때때로 훨씬 효율적이다). 기본 정의는 배가에 의한 곱셈(제곱에 의한 거듭제곱이라고도 알려져 있다)을 사용한다. 많은 반군에서 이는 중요한 최적화지만, 리스트 같은 일부 반군에서는 끔찍하므로 반드시 재정의해야 한다.

`sconcat`과 `stimes`에 대한 자세한 내용은 [haddock 문서](https://hackage.haskell.org/package/base-4.9.0.0/docs/Data-Semigroup.html)를 보라.

### 법칙

유일한 법칙은 `(<>)`가 결합적이어야 한다는 것이다.

```haskell
(x <> y) <> z = x <> (y <> z)
```

## Monoid

많은 반군에는 이항 연산 ⊕의 항등원이 되는 특별한 원소 e가 있다. 즉 모든 원소 x에 대해 e ⊕ x = x ⊕ e = x다. 이렇게 항등원이 있는 반군을 *모노이드(monoid)*라고 부른다.

### 정의

`Monoid` 타입 클래스의 정의(`Data.Monoid`에 정의됨; [haddock](https://hackage.haskell.org/package/base/docs/Data-Monoid.html))는 다음과 같다.

```haskell
class Monoid a where
  mempty  :: a
  mappend :: a -> a -> a

  mconcat :: [a] -> a
  mconcat = foldr mappend mempty
```

`mempty` 값은 모노이드의 항등원을 지정하고, `mappend`는 이항 연산이다. `mconcat`의 기본 정의는 오른쪽 접기(right fold)를 사용해 원소들의 리스트를 `mappend`로 모두 결합하여 "축약"한다. `mconcat`이 `Monoid` 클래스에 있는 이유는 특정 인스턴스가 더 효율적인 대체 구현을 제공할 수 있게 하기 위해서일 뿐이다. 보통은 `Monoid` 인스턴스를 만들 때 `mconcat`을 무시해도 안전하다. 기본 정의가 잘 동작하기 때문이다.

`Monoid`의 메서드들은 이름이 상당히 불행하다. 이름들은 `Monoid`의 리스트 인스턴스에서 영감을 얻었는데, 거기서는 실제로 `mempty = []`이고 `mappend = (++)`이다. 하지만 많은 모노이드가 이어붙이기(append)와 별 상관이 없으므로 오해를 부른다(Haskell-cafe 메일링 리스트의 [OCaml 해커 Brian Hurt의 논평](http://archive.fo/hkTOb) 참고). `mappend`의 별칭으로 제공되는 `(<>)` 덕에 상황이 조금 나아졌다.

`mappend`의 별칭 `(<>)`는 같은 이름의 `Semigroup` 메서드와 충돌한다는 점에 유의하라. 이 때문에 `Data.Semigroup`은 `Data.Monoid`의 상당 부분을 다시 익스포트한다. 반군과 모노이드를 함께 쓰려면 그냥 `Data.Semigroup`을 임포트하고, 모든 타입에 `Semigroup`과 `Monoid` 인스턴스가 모두 있는지(그리고 `(<>) = mappend`인지) 확인하라.

### 법칙

물론 모든 `Monoid` 인스턴스는 실제로 수학적 의미의 모노이드여야 하며, 이는 다음 법칙들을 함의한다.

```haskell
mempty `mappend` x = x
x `mappend` mempty = x
(x `mappend` y) `mappend` z = x `mappend` (y `mappend` z)
```

### 인스턴스

`Data.Monoid`에는 꽤 흥미로운 `Monoid` 인스턴스가 여럿 정의되어 있다.

* `[a]`는 `mempty = []`, `mappend = (++)`인 `Monoid`다. 임의의 리스트 `x`, `y`, `z`에 대해 `(x ++ y) ++ z = x ++ (y ++ z)`이고 빈 리스트가 항등원(`[] ++ x = x ++ [] = x`)임은 어렵지 않게 확인할 수 있다.

* 앞서 말했듯 임의의 숫자 타입은 덧셈이나 곱셈에 대해 모노이드가 될 수 있다. 그러나 같은 타입에 두 인스턴스를 둘 수는 없으므로, `Data.Monoid`는 적절한 `Monoid` 인스턴스를 갖춘 두 `newtype` 래퍼 `Sum`과 `Product`를 제공한다.

  ```haskell
  > getSum (mconcat . map Sum $ [1..5])
  15
  > getProduct (mconcat . map Product $ [1..5])
  120
  ```

  이 예제 코드는 물론 우스꽝스럽다. 그냥 `sum [1..5]`, `product [1..5]`라고 쓰면 된다. 그래도 이 인스턴스들은 더 일반화된 상황에서 유용하다. [`Foldable` 절](#foldable)에서 보게 될 것이다.

* `Any`와 `All`은 `Bool`에 (각각 논리합과 논리곱에 대한) `Monoid` 인스턴스를 제공하는 `newtype` 래퍼다.

* `Maybe`에는 인스턴스가 세 개 있다. `a`의 `Monoid` 인스턴스를 `Maybe a`의 인스턴스로 들어 올리는 기본 인스턴스와, `mappend`가 첫 번째(각각 마지막) 비-`Nothing` 항목을 선택하는 두 `newtype` 래퍼 `First`와 `Last`다.

* `Endo a`는 함수 `a -> a`의 newtype 래퍼로, 이 함수들은 합성에 대해 모노이드를 이룬다.

* `Monoid` 인스턴스를 추가 구조를 갖는 인스턴스로 "들어 올리는" 방법이 여럿 있다. `a`의 인스턴스를 `Maybe a`의 인스턴스로 들어 올릴 수 있다는 것은 이미 보았다. 튜플 인스턴스도 있다. `a`와 `b`가 `Monoid`의 인스턴스면 `(a,b)`도 인스턴스이며, `a`와 `b`의 모노이드 연산을 자명하게 성분별로 사용한다. 마지막으로 `a`가 `Monoid`면 임의의 `e`에 대해 함수 타입 `e -> a`도 `Monoid`다. 특히 ``g `mappend` h``는 인자에 `g`와 `h`를 모두 적용한 뒤 결과를 `a`의 기저 `Monoid` 인스턴스로 결합하는 함수다. 이는 꽤 유용하고 우아할 수 있다([예제](http://archive.fo/dUbHK) 참고).

* 타입 `Ordering = LT | EQ | GT`는 `Monoid`인데, (`xs`와 `ys`의 길이가 같다면) `mconcat (zipWith compare xs ys)`가 `xs`와 `ys`의 사전식 순서를 계산하도록 정의되어 있다. 구체적으로 `mempty = EQ`이고, `mappend`는 가장 왼쪽의 비-`EQ` 인자로 평가된다(둘 다 `EQ`면 `EQ`). 이를 함수의 `Monoid` 인스턴스와 함께 쓰면 꽤 영리한 일들을 할 수 있다([예제](http://www.reddit.com/r/programming/comments/7cf4r/monoids_in_my_programming_language/c06adnx)).

* containers 라이브러리의 여러 표준 데이터 구조([haddock](http://hackage.haskell.org/packages/archive/containers/0.2.0.0/doc/html/index.html))—`Map`, `Set`, `Sequence` 등—에도 `Monoid` 인스턴스가 있다.

`Monoid`는 다른 여러 타입 클래스 인스턴스를 가능하게 하는 데도 쓰인다. 앞서 보았듯 `Monoid`를 사용해 `((,) e)`를 `Applicative`의 인스턴스로 만들 수 있다.

```haskell
instance Monoid e => Applicative ((,) e) where
  pure :: Monoid e => a -> (e,a)
  pure x = (mempty, x)

  (<*>) :: Monoid e => (e,a -> b) -> (e,a) -> (e,b)
  (u, f) <*> (v, x) = (u `mappend` v, f x)
```

비슷하게 `Monoid`를 사용해 `((,) e)`를 `Monad`의 인스턴스로도 만들 수 있다. 이것이 *라이터 모나드(writer monad)*로 알려져 있다. 이미 보았듯 `Writer`와 `WriterT`는 각각 이 모나드의 newtype 래퍼와 트랜스포머다.

`Monoid`는 `Foldable` 타입 클래스에서도 핵심적인 역할을 한다([Foldable 절](#foldable) 참고).

### 더 읽을거리

모노이드는 2009년에 꽤 주목을 받았다. [Brian Hurt의 블로그 글](http://blog.enfranchisedmind.com/2009/01/random-thoughts-on-haskell/)이 많은 Haskell 타입 클래스의 이름(특히 `Monoid`)이 추상 수학에서 왔다는 사실에 불평하면서였다. 그 결과 이 논점을 두고 논쟁하며 모노이드 일반을 논의하는 [긴 Haskell-cafe 스레드](http://archive.fo/hkTOb)가 이어졌다.

> **참고**: 그 이름이 영원하기를.

그러나 곧이어 `Monoid`에 관한 블로그 글이 여럿 나왔다. 먼저 Dan Piponi가 훌륭한 입문 글 [Haskell Monoids and their Uses](http://blog.sigfpe.com/2009/01/haskell-monoids-and-their-uses.html)를 썼다. 곧바로 Heinrich Apfelmus의 [Monoids and Finger Trees](http://apfelmus.nfshost.com/monoid-fingertree.html)가 뒤따랐는데, 이는 `Monoid`를 매우 영리하게 사용해 우아하고 제네릭한 데이터 구조를 구현하는 Hinze와 Paterson의 [2-3 핑거 트리에 관한 고전 논문](http://www.soi.city.ac.uk/%7Eross/papers/FingerTree.html)을 접근하기 쉽게 풀어낸 글이다. 그다음 Dan Piponi가 `Monoid`(와 핑거 트리)를 사용하는 매혹적인 글 두 편을 썼다. [Fast Incremental Regular Expressions](http://blog.sigfpe.com/2009/01/fast-incremental-regular-expression.html)와 [Beyond Regular Expressions](http://blog.sigfpe.com/2009/01/beyond-regular-expressions-more.html)다.

비슷한 맥락에서, 증분 접기(incremental fold)를 계산하기 위해 `Data.Map`을 개선하는 David Place의 글([the Monad Reader issue 11](http://www.haskell.org/wikiupload/6/6a/TMR-Issue11.pdf) 참고)도 `Monoid`로 데이터 구조를 일반화하는 좋은 예다.

`Monoid` 사용의 다른 흥미로운 예로는 [우아한 리스트 정렬 콤비네이터 만들기](http://www.reddit.com/r/programming/comments/7cf4r/monoids_in_my_programming_language/c06adnx), [비구조화된 정보 수집하기](http://byorgey.wordpress.com/2008/04/17/collecting-unstructured-information-with-the-monoid-of-partial-knowledge/), [확률 분포 결합하기](http://izbicki.me/blog/gausian-distributions-are-monoids), 그리고 Chung-Chieh Shan과 Dylan Thurston이 `Monoid`로 [어려운 조합론 퍼즐을 우아하게 푸는](http://conway.rutgers.edu/~ccshan/wiki/blog/posts/WordNumbers1/) 빼어난 연작([2부](http://conway.rutgers.edu/~ccshan/wiki/blog/posts/WordNumbers2/), [3부](http://conway.rutgers.edu/~ccshan/wiki/blog/posts/WordNumbers3/), [4부](http://conway.rutgers.edu/~ccshan/wiki/blog/posts/WordNumbers4/))가 있다.

믿기 어렵겠지만, 모나드는 실제로 일종의 모노이드로 볼 수 있다. `join`이 이항 연산의 역할을, `return`이 항등원의 역할을 한다. [Dan Piponi의 블로그 글](http://blog.sigfpe.com/2008/11/from-monoids-to-monads.html)을 보라.

## 실패와 선택: Alternative, MonadPlus, ArrowPlus

여러 클래스(`Applicative`, `Monad`, `Arrow`)에는 "모노이드적" 서브클래스가 있다. (적절한 의미에서) "실패"와 "선택"을 지원하는 계산을 모델링하기 위한 것이다.

### 정의

`Alternative` 타입 클래스([haddock](https://hackage.haskell.org/package/base/docs/Control-Applicative.html#g:2))는 모노이드 구조도 갖는 `Applicative` functor를 위한 것이다.

```haskell
class Applicative f => Alternative f where
  empty :: f a
  (<|>) :: f a -> f a -> f a

  some :: f a -> f [a]
  many :: f a -> f [a]
```

기본 직관은 `empty`가 어떤 종류의 "실패"를, `(<|>)`가 대안들 사이의 선택을 나타낸다는 것이다. (다만 이 직관이 가능한 뉘앙스를 온전히 담아내지는 못한다. 아래 법칙 절을 보라.) 물론 `(<|>)`는 결합적이어야 하고 `empty`는 그 항등원이어야 한다. `Alternative`의 인스턴스는 `empty`와 `(<|>)`를 구현해야 한다. `some`과 `many`는 기본 구현이 있지만, 특수화된 구현이 기본보다 효율적일 수 있어 클래스에 포함되어 있다.

`some`과 `many`의 기본 정의는 본질적으로 다음과 같다.

```haskell
some v = (:) <$> v <*> many v
many v = some v <|> pure []
```

(무슨 이유에서인지 실제로는 상호 재귀로 정의되어 있지 않지만.) 직관은 둘 다 `v`를 계속 실행해 결과를 리스트에 모으다가 실패하면 멈춘다는 것이다. `some v`는 `v`가 적어도 한 번 성공하기를 요구하고, `many v`는 아예 성공하지 않아도 된다. 즉 `many`는 `v`의 0회 이상 반복을, `some`은 1회 이상 반복을 나타낸다. `some`과 `many`가 모든 `Alternative` 인스턴스에서 말이 되는 것은 아니라는 점에 유의하라. 아래에서 더 논의한다.

마찬가지로 `MonadPlus`([haddock](https://hackage.haskell.org/package/base/docs/Control-Monad.html#t:MonadPlus))는 모노이드 구조를 갖는 `Monad`를 위한 것이다.

```haskell
class Monad m => MonadPlus m where
  mzero :: m a
  mplus :: m a -> m a -> m a
```

마지막으로 `ArrowZero`와 `ArrowPlus`([haddock](https://hackage.haskell.org/package/base/docs/Control-Arrow.html#t:ArrowZero))는 모노이드 구조를 갖는 `Arrow`([Part 3 참고](014-typeclassopedia-part3.md#arrow))를 나타낸다.

```haskell
class Arrow arr => ArrowZero arr where
  zeroArrow :: b `arr` c

class ArrowZero arr => ArrowPlus arr where
  (<+>) :: (b `arr` c) -> (b `arr` c) -> (b `arr` c)
```

### 인스턴스

이 문서는 보통 예제 인스턴스보다 법칙을 먼저 논의하지만, `Alternative`와 그 친구들에 대해서는 순서를 바꾸는 편이 낫다. 법칙에 대해 논란이 좀 있어서, 논의할 때 구체적인 예제를 염두에 두는 것이 도움이 되기 때문이다. 이 절과 다음 절에서는 주로 `Alternative`에 집중한다. `Applicative`가 `Monad`의 슈퍼클래스가 된 지금은 `MonadPlus`를 더 쓸 이유가 거의 없고, `ArrowPlus`는 상당히 마이너하다.

* `Maybe`는 `Alternative`의 인스턴스다. `empty`는 `Nothing`이고, 선택 연산자 `(<|>)`는 첫 인자가 `Just`면 첫 인자를, 아니면 두 번째 인자를 결과로 낸다. 따라서 `Maybe` 리스트를 `(<|>)`로 접으면(`Data.Foldable`의 `asum`으로 할 수 있다) 리스트에서 처음 나오는 비-`Nothing` 값(없으면 `Nothing`)을 얻는다.

* `[]`도 인스턴스다. `empty`는 빈 리스트이고 `(<|>)`는 `(++)`와 같다. 이것이 `[a]`의 `Monoid` 인스턴스와 동일하다는 점을 짚어 둘 만하다. 반면 `Maybe`의 `Alternative` 인스턴스와 `Monoid` 인스턴스는 다르다. `Maybe a`의 `Monoid` 인스턴스는 `a`의 `Monoid` 인스턴스를 요구하며, 두 `Just`가 주어지면 담긴 값들을 모노이드적으로 결합한다.

`Maybe`와 `[]`에 대한 `some`과 `many`의 동작을 생각해 보자. `Maybe`에 대해 `some Nothing = (:) <$> Nothing <*> many Nothing = Nothing <*> many Nothing = Nothing`이다. 따라서 `many Nothing = some Nothing <|> pure [] = Nothing <|> pure [] = pure [] = Just []`이기도 하다. 시시하다. 그런데 `some`과 `many`를 `Just`에 적용하면? 사실 `some (Just a)`와 `many (Just a)`는 둘 다 바텀(bottom)이다! 문제는 `Just a`가 항상 "성공"하므로 재귀가 결코 끝나지 않는다는 것이다. 이론상 결과는 무한 리스트 `[a,a,a,...]`"여야" 하지만, 이 리스트의 원소를 하나도 만들어 내지 못한다. `many` 호출의 결과가 `Just`임을 알기 전까지 `(<*>)` 연산자가 어떤 출력도 내놓을 수 없기 때문이다.

`[]`의 경우도 직접 따져 볼 수 있는데, 결과는 꽤 비슷하다. `some`과 `many`는 빈 리스트에 적용하면 시시한 결과를, 비어 있지 않은 리스트에 적용하면 바텀을 낸다.

결국 `some`과 `many`는 어떤 행동 `v`를 여러 번 실행했을 때 유한 번 성공하다가 실패할 수 있는, 일종의 "상태 있는" `Applicative` 인스턴스와 함께 써야만 진짜 의미가 있다. 예를 들어 파서가 그런 동작을 보이며, 실제로 파서가 `some`과 `many` 메서드의 원래 동기가 된 예다. 아래에서 더 다룬다.

* GHC 8.0(즉 `base-4.9`)부터 `IO`에 `Alternative` 인스턴스가 있다. `empty`는 I/O 예외를 던지고, `(<|>)`는 먼저 왼쪽 인자를 실행하되 왼쪽 인자가 I/O 예외를 던지면 그 예외를 잡고 두 번째 인자를 호출한다. (다른 종류의 예외는 잡지 않는다는 점에 유의하라.) I/O 에러를 다루는 훨씬 좋은 방법들이 있지만, GHCi 프롬프트에 입력하는 표현식 같은 단순한 일회성 프로그램에는 통할 수 있는 임시방편이다. 예를 들어 파일 내용을 읽되 파일이 없으면 기본 내용을 쓰고 싶다면 그냥 `readFile "somefile.txt" <|> return "default file contents"`라고 쓰면 된다.

* `async` 패키지의 `Concurrently`에는 `Alternative` 인스턴스가 있는데, `c1 <|> c2`는 `c1`과 `c2`를 병렬로 경주시켜 먼저 끝나는 쪽의 결과를 반환한다. `empty`는 값을 반환하지 않고 영원히 실행되는 행동에 해당한다.

* 사실상 모든 파서 타입(`parsec`, `megaparsec`, `trifecta` 등)에 `Alternative` 인스턴스가 있다. `empty`는 무조건적인 파싱 실패이고, `(<|>)`는 왼쪽 우선 선택이다. 즉 `p1 <|> p2`는 먼저 `p1`으로 파싱을 시도하고, `p1`이 실패하면 대신 `p2`를 시도한다.

`some`과 `many`는 `Applicative` 인스턴스가 있는 파서 타입과 특히 잘 맞는다. `p`가 파서라면 `some p`는 연속된 `p`의 1회 이상 출현을 파싱하고(즉 가능한 한 많은 `p`를 파싱한 뒤 멈춘다), `many p`는 0회 이상 출현을 파싱한다.

### 법칙

물론 `Alternative`의 인스턴스는 모노이드 법칙을 만족해야 한다.

```haskell
empty <|> x = x
x <|> empty = x
(x <|> y) <|> z = x <|> (y <|> z)
```

`some`과 `many`의 문서는 이들이 자신을 특징짓는 상호 재귀적 기본 정의의 "최소 해"(정의됨(definedness) 순서에서 최소)여야 한다고 말한다. 그러나 [이는 논란이 있으며](https://www.reddit.com/r/haskell/comments/2j8bvl/laws_of_some_and_many/), 아마 그리 깊이 고민된 것이 아닐 것이다.

`Alternative`는 `Applicative`의 서브클래스이므로, "`empty`와 `(<|>)`가 `(<*>)`, `pure`와 어떻게 상호작용해야 하는가?"라는 물음이 자연스럽다.

*left zero* 법칙에는 거의 모두가 동의한다(다만 아래 *right zero* 법칙 논의를 보라).

```haskell
empty <*> f = empty
```

여기서부터 좀 골치 아파지기 시작한다. 추가로 상상해 볼 수 있는 법칙이 여럿 있는데, 인스턴스마다 만족하는 법칙이 다르다.

* *Right Zero*:

  ```haskell
  f <*> empty = empty
  ```

  또 하나의 자명해 보이는 법칙이다. 대부분의 인스턴스가 만족하지만 `IO`는 만족하지 않는다. `f`의 효과가 일단 실행되고 나면, 나중에 예외를 만나도 되돌릴 방법이 없기 때문이다. 이제 `transformers` 패키지의 applicative 트랜스포머 `Backwards`를 생각해 보자. `f`가 `Applicative`면 `Backwards f`도 그렇다. 같은 방식으로 동작하되 `(<*>)` 인자들의 행동을 반대 순서로 수행한다. 인스턴스 `Alternative f => Alternative (Backwards f)`도 있다. 어떤 `f`(예컨대 `IO`)가 *left zero*는 만족하고 *right zero*는 만족하지 않는다면, `Backwards f`는 *right zero*는 만족하고 *left zero*는 만족하지 않는다! 그러니 *left zero* 법칙조차 의심스럽다. 요점은 `Backwards`가 존재하는 이상 어느 한 방향에만 특권을 줄 수 없다는 것이다.

* *Left Distribution*:

  ```haskell
  (a <|> b) <*> c = (a <*> c) <|> (b <*> c)
  ```

  이 분배 법칙은 `[]`와 `Maybe`가 만족한다(직접 확인해 보라). 그러나 `IO`와 대부분의 파서는 만족하지 *않는다*. `a`와 `b`가 `c`의 실행에 영향을 주는 효과를 가질 수 있어서, 좌변은 실패하는데 우변은 성공할 수 있기 때문이다.

  예를 들어 `IO`에서, `a`는 항상 성공하지만 `c`는 `a`가 실행된 뒤 I/O 예외를 던진다고 하자. 구체적으로 `a`는 어떤 파일이 존재하지 않도록 만들고(존재하면 삭제, 없으면 아무것도 안 함), `c`는 그 파일을 읽으려 한다고 하자. 이 경우 `(a <|> b) <*> c`는 먼저 파일을 삭제하고(`a`가 성공하므로 `b`는 무시), `c`가 파일을 읽으려 할 때 예외를 던진다. 한편 `b`는 문제의 그 파일이 *존재하도록* 만든다고 하자. 그러면 `(a <*> c) <|> (b <*> c)`는 성공한다. `(a <*> c)`가 예외를 던지면 `(<|>)`가 잡고 `(b <*> c)`를 시도하기 때문이다.

  파서에서 이 법칙이 성립하지 않는 이유도 비슷하다. `(a <|> b) <*> c`는 `c`를 실행하기 전에 `a`로 파싱할지 `b`로 파싱할지 "확정"해야 하는 반면, `(a <*> c) <|> (b <*> c)`는 `a <*> c`가 실패하면 백트래킹을 허용한다. 특히 `a`는 성공하지만 `c`가 `a` 뒤에서는 실패하고 `b` 뒤에서는 실패하지 않는 경우 결과가 다를 수 있다. 예를 들어 `a`와 `c`는 둘 다 별표 두 개를 기대하고 `b`는 하나만 기대한다고 하자. 입력에 별표가 세 개뿐이면 `b <*> c`는 성공하지만 `a <*> c`는 그렇지 않다.

* *Right Distribution*:

  ```haskell
  a <*> (b <|> c) = (a <*> b) <|> (a <*> c)
  ```

  이 법칙을 만족하는 인스턴스는 많지 않지만 그래도 논의할 가치가 있다. 특히 `Maybe`는 이 법칙을 만족한다. 그러나 예컨대 리스트는 만족하지 *않는다*. 결과가 다른 순서로 나오는 것이 문제다. 예를 들어 `a = [(+1), (*10)]`, `b = [2]`, `c = [3]`이라 하자. 좌변은 `[3,4,20,30]`이지만 우변은 `[3,20,4,30]`이다.

  `IO`도 만족하지 않는다. 예컨대 `a`가 *두 번째* 실행에서만 성공할 수도 있기 때문이다. 반면 파서는 백트래킹을 어떻게 처리하느냐에 따라 만족할 수도 안 할 수도 있다. `(<|>)` 자체가 완전한 백트래킹을 수행하는 파서는 이 법칙을 만족하지만, 많은 파서 콤비네이터 라이브러리는 효율상의 이유로 그렇지 않다. 예를 들어 parsec은 이 법칙에 어긋난다. `a`가 입력을 소비하며 성공한 뒤 `b`가 입력을 소비하지 않고 실패하면, 좌변은 성공하는데 우변은 실패할 수 있다. `(a <*> b)`가 실패한 뒤 우변은 원래의 `a`가 소비한 입력을 되돌리지 않은 채 `a`를 다시 실행하려 하기 때문이다.

* *Left Catch*:

  ```haskell
  (pure a) <|> x = pure a
  ```

  직관적으로 이 법칙은 `pure`가 항상 "성공한" 계산을 나타내야 한다고 말한다. `Maybe`, `IO`, 파서가 만족한다. 그러나 리스트는 만족하지 않는다. 리스트는 가능한 결과를 *모두* 모으기 때문이다. 이는 `[a] ++ x == [a]`에 해당하는데 명백히 거짓이다.

정리하면 상황은 이렇다. `Alternative`(와 `MonadPlus`)의 인스턴스는 많고, 각 인스턴스는 이 법칙들 중 어떤 *부분집합*을 만족한다. 게다가 항상 *같은* 부분집합도 아니어서, 자명한 "기본" 법칙 집합을 고를 수도 없다. 적어도 당분간은 이 상황을 그냥 안고 살아야 한다. 특정 `Alternative`나 `MonadPlus` 인스턴스를 쓸 때는 그것이 어떤 법칙을 만족하는지 신중히 생각해 볼 가치가 있다.

### 유틸리티 함수

언급할 만한 `Alternative` 전용 유틸리티 함수가 몇 개 있다.

* ```haskell
  guard :: Alternative f => Bool -> f ()
  ```

  주어진 조건을 검사해 조건이 성립하면 `pure ()`로, 아니면 `empty`로 평가된다. 계산 중간에 조건부 실패 지점을 만드는 데 쓸 수 있다. 특정 조건이 성립할 때만 계산이 계속된다.

* ```haskell
  optional :: Alternative f => f a -> f (Maybe a)
  ```

  잠재적 실패를 `Maybe` 타입으로 구체화한다. 즉 `optional x`는 항상 성공하는 계산으로, `x`가 실패하면 `Nothing`을, `x`가 성공해 `a`를 내면 `Just a`를 반환한다. 예컨대 파서 맥락에서 0회 또는 1회 나타날 수 있는 생성 규칙에 해당하여 유용하다.

### 더 읽을거리

예전에는 `mzero`만을 담은 `MonadZero`라는 타입 클래스가 있었다. 실패가 있는 모나드를 나타냈다. `do` 표기법은 실패하는 패턴 매치를 다루기 위해 어떤 실패 개념을 필요로 한다. 불행히도 `MonadZero`는 폐기되고 `Monad` 클래스에 `fail` 메서드가 추가되었다. 운이 좋다면 언젠가 `MonadZero`가 복원되고, `fail`은 제 있을 곳인 쓰레기통으로 추방될 것이다([MonadPlus reform proposal](https://wiki.haskell.org/MonadPlus_reform_proposal) 참고). 아이디어는 패턴 매칭을 사용해서 실패할 수 있는 `do` 블록에는 `MonadZero` 제약을 요구하고, 그렇지 않으면 `Monad` 제약만 요구하자는 것이다.

`MonadPlus` 타입 클래스에 대한 훌륭한 입문으로, 흥미로운 사용 예와 함께, Doug Auclair의 *MonadPlus: What a Super Monad!*가 [Monad.Reader 11호](http://www.haskell.org/wikiupload/6/6a/TMR-Issue11.pdf)에 실려 있다.

`MonadPlus`의 또 다른 흥미로운 사용은 ICFP 2016의 Christiansen 등, [All Sorts of Permutations](http://www-ps.informatik.uni-kiel.de/~sad/icfp2016-preprint.pdf)에서 볼 수 있다.

[logict 패키지](https://hackage.haskell.org/package/logict)는 제약 조건하에서 가능성들을 효율적으로 열거하는 데 쓸 수 있는, 즉 논리 프로그래밍을 위한, 두드러진 `Alternative`와 `MonadPlus` 인스턴스를 가진 타입을 정의한다. 스테로이드를 맞은 리스트 모나드 같은 것이다.

## Foldable

`Data.Foldable` 모듈에 정의된 `Foldable` 클래스([haddock](https://hackage.haskell.org/package/base/docs/Data-Foldable.html))는 요약 값으로 "접을(fold)" 수 있는 컨테이너를 추상화한다. 이 덕분에 그런 접기 연산을 컨테이너에 구애받지 않는 방식으로 작성할 수 있다.

### 정의

`Foldable` 타입 클래스의 정의는 다음과 같다.

```haskell
class Foldable t where
  fold    :: Monoid m => t m -> m
  foldMap :: Monoid m => (a -> m) -> t a -> m
  foldr   :: (a -> b -> b) -> b -> t a -> b
  foldr'  :: (a -> b -> b) -> b -> t a -> b
  foldl   :: (b -> a -> b) -> b -> t a -> b
  foldl'  :: (b -> a -> b) -> b -> t a -> b
  foldr1  :: (a -> a -> a) -> t a -> a
  foldl1  :: (a -> a -> a) -> t a -> a
  toList  :: t a -> [a]
  null    :: t a -> Bool
  length  :: t a -> Int
  elem    :: Eq a => a -> t a -> Bool
  maximum :: Ord a => t a -> a
  minimum :: Ord a => t a -> a
  sum     :: Num a => t a -> a
  product :: Num a => t a -> a
```

복잡해 보이지만, 사실 `Foldable` 인스턴스를 만들려면 메서드 하나만 구현하면 된다. `foldMap`이나 `foldr` 중 하나를 고르면 된다. 다른 모든 메서드에는 이들을 이용한 기본 구현이 있으며, 더 효율적인 구현을 제공할 수 있는 경우를 위해 클래스에 포함되어 있다.

### 인스턴스와 예제

`foldMap`의 타입은 그것이 무엇을 하려는 것인지 분명히 보여 준다. 컨테이너 속 데이터를 `Monoid`로 변환하는 방법(함수 `a -> m`)과 `a`들의 컨테이너(`t a`)가 주어지면, `foldMap`은 컨테이너의 전체 내용물을 순회하면서 모든 `a`를 `m`으로 변환하고 그 `m`들을 전부 `mappend`로 결합하는 방법을 제공한다. 다음 코드는 두 예제를 보여 준다. 리스트에 대한 `foldMap`의 단순한 구현과, `Foldable` 문서에 나오는 이진 트리 예제다.

```haskell
instance Foldable [] where
  foldMap :: Monoid m => (a -> m) -> [a] -> m
  foldMap g = mconcat . map g

data Tree a = Empty | Leaf a | Node (Tree a) a (Tree a)

instance Foldable Tree where
  foldMap :: Monoid m => (a -> m) -> Tree a -> m
  foldMap f Empty        = mempty
  foldMap f (Leaf x)     = f x
  foldMap f (Node l k r) = foldMap f l `mappend` f k `mappend` foldMap f r
```

`Foldable` 모듈은 `Maybe`와 `Array`의 인스턴스도 제공한다. 또 표준 [containers 라이브러리](http://hackage.haskell.org/package/containers)의 많은 데이터 구조(예: `Map`, `Set`, `Tree`, `Sequence`)가 자체 `Foldable` 인스턴스를 제공한다.

**연습문제**

1. `fold`를 `foldMap`으로 구현하라.
2. `foldMap`을 `fold`로 구현하려면 무엇이 필요한가?
3. `foldMap`을 `foldr`로 구현하라.
4. `foldr`를 `foldMap`으로 구현하라(힌트: `Endo` 모노이드를 사용하라).
5. `foldMap . foldMap`의 타입은 무엇인가? `foldMap . foldMap . foldMap` 등은? 이들은 무엇을 하는가?

### 유도된 접기

`Foldable` 인스턴스가 주어지면, 다음처럼 제네릭하고 컨테이너에 구애받지 않는 함수를 쓸 수 있다.

```haskell
-- 임의의 컨테이너의 크기를 계산한다.
containerSize :: Foldable f => f a -> Int
containerSize = getSum . foldMap (const (Sum 1))

-- 술어를 만족하는 컨테이너 요소들의 리스트를 계산한다.
filterF :: Foldable f => (a -> Bool) -> f a -> [a]
filterF p = foldMap (\a -> if p a then [a] else [])

-- 컨테이너 안의 String 중 문자 a를 포함하는 것들의
-- 리스트를 얻는다.
aStrings :: Foldable f => f String -> [String]
aStrings = filterF (elem 'a')
```

`Foldable` 모듈은 미리 정의된 접기도 다수 제공한다. 예전에는 리스트에만 동작하던 동명의 `Prelude` 함수들을 일반화한 버전이었지만, [GHC 7.10부터는](https://wiki.haskell.org/Foldable_Traversable_In_Prelude) 일반화된 버전 자체가 Prelude에서 익스포트된다. 예를 들면 `concat`, `concatMap`, `and`, `or`, `any`, `all`, `sum`, `product`, `maximum`(`By`), `minimum`(`By`), `elem`, `notElem`, `find`가 그렇다. 예컨대 GHC 7.10 이전에 `length`의 타입은 `length :: [a] -> Int`였지만, 지금은 `Foldable t => t a -> Int`다(실제로 위에서 보인 `containerSize` 함수와 같다).

중요한 함수 `toList`도 제공된다. 임의의 `Foldable` 구조를 왼쪽에서 오른쪽 순서의 요소 리스트로 바꾸며, 리스트 모노이드로 접어서 동작한다.

컨테이너의 각 요소로부터 어떤 계산을 생성한 다음, 그 계산들의 부수 효과를 모두 수행하되 결과는 버리는, `Applicative`나 `Monad` 인스턴스와 함께 동작하는 제네릭 함수도 있다. `traverse_`, `sequenceA_` 등이다. 결과를 버려야 하는 이유는 `Foldable` 클래스가 결과로 무엇을 할지 지정하기에는 너무 약하기 때문이다. 임의의 `Applicative`나 `Monad` 인스턴스를 일반적으로 `Monoid`로 만들 수는 없지만, 그런 `m`에 대해 `m ()`은 `Monoid`로 만들 수 있다. 모노이드 구조를 갖는 `Applicative`나 `Monad`—즉 `Alternative`나 `MonadPlus`—가 있다면, 결과까지 결합할 수 있는 `asum`이나 `msum` 함수를 쓸 수 있다. 이 함수들에 대한 자세한 내용은 [`Foldable` 문서](https://hackage.haskell.org/package/base/docs/Data-Foldable.html)를 참고하라.

`Foldable` 연산은 접히는 컨테이너의 구조를 항상 잊어버린다는 점에 유의하라. 어떤 `Foldable t`에 대해 타입 `t a`의 컨테이너로 시작하면, `Foldable` 모듈에 정의된 어떤 연산의 출력 타입에도 `t`는 결코 나타나지 않는다. 많은 경우 이것이 정확히 우리가 원하는 바이지만, 때로는 구조를 보존하면서 컨테이너를 제네릭하게 순회하고 싶다—그것이 바로 다음 절에서 논의할 `Traversable` 클래스가 제공하는 것이다.

**연습문제**

1. `toList :: Foldable f => f a -> [a]`를 `foldr`나 `foldMap` 중 하나로 구현하라.
2. 리스트 전용 `foldr :: (a -> b -> b) -> b -> [a] -> b`만 있다고 가정하고, 제네릭 버전의 `foldr`를 `toList`로 구현하는 방법을 보여라.
3. 다음 함수들 중 몇 개를 골라 구현해 보라: `concat`, `concatMap`, `and`, `or`, `any`, `all`, `sum`, `product`, `maximum`(`By`), `minimum`(`By`), `elem`, `notElem`, `find`. 이들이 `Foldable`로 어떻게 일반화되는지 알아내고, 적절한 `Monoid` 인스턴스와 함께 `fold`나 `foldMap`을 사용하는 우아한 구현을 생각해 내라.

### 유틸리티 함수

* `asum :: (Alternative f, Foldable t) => t (f a) -> f a`는 계산들로 가득 찬 컨테이너를 받아 `(<|>)`로 결합한다.

* `sequenceA_ :: (Applicative f, Foldable t) => t (f a) -> f ()`는 계산들로 가득 찬 컨테이너를 받아 순서대로 실행하되 결과를 버린다(즉 효과만을 위해 사용한다). 결과를 버리므로 컨테이너는 `Foldable`이기만 하면 된다. (원래 컨테이너와 같은 모양의 결과 컨테이너를 재구성하기 위해 더 강한 `Traversable` 제약을 요구하는 `sequenceA :: (Applicative f, Traversable t) => t (f a) -> f (t a)`와 비교해 보라.)

* `traverse_ :: (Applicative f, Foldable t) => (a -> f b) -> t a -> f ()`는 foldable 컨테이너의 각 요소에 주어진 함수를 적용하고 효과를 순서대로 수행한다(결과는 버린다).

* `for_`는 인자 순서가 뒤집힌 `traverse_`다. 명령형 언어의 "foreach" 루프에 도의적으로 해당한다.

* 역사적인 이유로, 위 함수들 모두에 지나치게 제한적인 `Monad`(류) 제약이 붙은 변형도 있다. `msum`은 `MonadPlus`에 특수화된 `asum`이고, `sequence_`, `mapM_`, `forM_`은 각각 `sequenceA_`, `traverse_`, `for_`의 `Monad` 특수화다.

**연습문제**

1. `traverse_`를 `sequenceA_`로, 또 그 반대로 구현하라. 둘 중 하나에는 추가 제약이 필요하다. 무엇인가?

### 사실 Foldable은 fold가 아니다

일반 용어 "fold"는 흔히 [catamorphism](https://wiki.haskell.org/Catamorphisms)이라는 더 기술적인 개념을 가리키는 데 쓰인다. 직관적으로, (재귀적 하위 항이 이미 요약으로 대체되었다고 할 때) "한 수준의 구조"를 요약하는 방법이 주어지면, catamorphism은 재귀 구조 전체를 요약할 수 있다. `Foldable`은 catamorphism에 대응하지 *않고* 그보다 약한 것에 대응한다는 점을 깨닫는 것이 중요하다. 구체적으로, `Foldable`은 구조 안에서 요소들의 왼쪽-오른쪽 순회 순서만 관찰할 수 있게 해 줄 뿐, 실제 구조 자체는 볼 수 없다. 달리 말하면, `Foldable`의 모든 사용은 `toList`로 표현할 수 있다. 예를 들어 `fold` 자체는 `mconcat . toList`와 동등하다.

이는 많은 작업에 충분하지만 전부는 아니다. 예를 들어 `Tree`의 깊이를 계산해 보려 하자. 아무리 애써도 `Foldable`로는 구현할 방법이 없다. 하지만 catamorphism으로는 구현할 *수 있다*.

### 더 읽을거리

`Foldable` 클래스는 `Applicative`를 도입한 [McBride와 Paterson의 논문](http://www.soi.city.ac.uk/~ross/papers/Applicative.html)에서 기원했지만, 논문에 실린 형태에서 상당히 살이 붙었다.

`Foldable`(그리고 `Traversable`)의 흥미로운 사용은 Janis Voigtländer의 논문 [Bidirectionalization for free!](http://doi.acm.org/10.1145/1480881.1480904)에서 볼 수 있다.

`fold`, `foldMap`, `foldr`의 관계에 대해 더 알고 싶다면 [foldr is made of monoids](https://byorgey.wordpress.com/2012/11/05/foldr-is-made-of-monoids/)를 보라.

[`Foldable`(과 `Traversable`)을 Prelude에 더 깊이 통합하자는 제안](https://wiki.haskell.org/Foldable_Traversable_In_Prelude)(FTP라고 알려져 있다)을 두고 Haskell 커뮤니티에서 [꽤 큰 논란](http://tojans.me/blog/2015/10/13/foldable-for-non-haskellers-haskells-controversial-ftp-proposal/)이 있었다. 논란의 일부는 `((,) a)`의 `Foldable` 인스턴스 같은 것에 집중되었다. 이 인스턴스는 `length :: Foldable t => t a -> Int` 같은 일반화된 함수 타입과 결합하면 `length (2,3) = 1`처럼 언뜻 터무니없어 보이는 결과를 유도할 수 있다. 이 상황을 풍자하는 [유머러스한 발표](https://www.youtube.com/watch?v=87re_yIQMDw)도 있다.

## Traversable

### 정의

`Data.Traversable` 모듈에 정의된 `Traversable` 타입 클래스([haddock](https://hackage.haskell.org/package/base/docs/Data-Traversable.html))는 다음과 같다.

```haskell
class (Functor t, Foldable t) => Traversable t where
  traverse  :: Applicative f => (a -> f b) -> t a -> f (t b)
  sequenceA :: Applicative f => t (f a) -> f (t a)
  mapM      ::       Monad m => (a -> m b) -> t a -> m (t b)
  sequence  ::       Monad m => t (m a) -> m (t a)
```

보다시피 모든 `Traversable`은 `Foldable`이자 `Functor`다. `Traversable` 인스턴스를 만들려면 `traverse`나 `sequenceA` 중 하나를 구현하면 충분하다. 다른 메서드들은 모두 이들을 이용한 기본 구현이 있다. `mapM`과 `sequence`는 역사적인 이유로만 존재한다. 특히 `Applicative`가 `Monad`의 슈퍼클래스가 된 지금, 이들은 각각 `traverse`와 `sequenceA`의 복사본에 타입만 더 제한적인 것에 지나지 않는다.

### 직관

`Traversable` 클래스의 핵심 메서드는 `traverse`이며, 타입은 다음과 같다.

```haskell
  traverse :: Applicative f => (a -> f b) -> t a -> f (t b)
```

이 타입은 `Traversable`을 `Functor`의 일반화로 보게 해 준다. `traverse`는 "효과 있는 `fmap`"이다. 타입 `t a`의 구조 위를 매핑하면서 타입 `a`의 모든 요소에 함수를 적용해 타입 `t b`의 새 구조를 만들되, 그 과정에서 함수가 (applicative functor `f`로 포착되는) 어떤 효과를 가질 수 있다.

다른 방법으로 `sequenceA` 함수를 살펴볼 수도 있다. 타입을 보자.

```haskell
  sequenceA :: Applicative f => t (f a) -> f (t a)
```

이는 근본적인 물음에 답한다. 언제 두 펑터를 교환(commute)할 수 있는가? 예를 들어 리스트들의 트리를 트리들의 리스트로 바꿀 수 있는가?

두 모나드를 합성하는 능력은 바로 이 펑터 교환 능력에 결정적으로 의존한다. 직관적으로, 모나드 `m`과 `n`으로 합성 모나드 `M a = m (n a)`를 만들고 싶다면, `join :: M (M a) -> M a`, 즉 `join :: m (n (m (n a))) -> m (n a)`를 구현하려면 `n`을 `m` 너머로 교환해 `m (m (n (n a)))`를 얻을 수 있어야 하고, 그러면 `m`과 `n`의 `join`을 사용해 타입 `m (n a)`의 무언가를 만들 수 있다. 자세한 내용은 [Mark Jones의 논문](http://web.cecs.pdx.edu/~mpj/pubs/springschool.html)을 보라.

타입 `t`에 `Functor` 제약이 있으면 `traverse`와 `sequenceA`는 표현력이 동등한 것으로 드러난다. 서로가 서로로 구현될 수 있다.

**연습문제**

1. 리스트들의 트리를 트리들의 리스트로 바꾸는 자연스러운 방법이 적어도 둘 있다. 무엇이며, 왜 그런가?
2. 트리들의 리스트를 리스트들의 트리로 바꾸는 자연스러운 방법을 제시하라.
3. `traverse . traverse`의 타입은 무엇인가? 무엇을 하는가?
4. `traverse`를 `sequenceA`로, 또 그 반대로 구현하라.

### 인스턴스와 예제

`Traversable` 인스턴스의 예는 무엇일까? 다음 코드는 앞의 `Foldable` 절에서 예로 든 것과 같은 `Tree` 타입에 대한 예제 인스턴스를 보여 준다. `Tree`의 `Functor` 인스턴스와 비교해 보면 배울 점이 많은데, 그것도 함께 보였다.

```haskell
data Tree a = Empty | Leaf a | Node (Tree a) a (Tree a)

instance Traversable Tree where
  traverse :: Applicative f => (a -> f b) -> Tree a -> f (Tree b) 
  traverse g Empty        = pure Empty
  traverse g (Leaf x)     = Leaf <$> g x
  traverse g (Node l x r) = Node <$> traverse g l
                                 <*> g x
                                 <*> traverse g r

instance Functor Tree where
  fmap :: (a -> b) -> Tree a -> Tree b
  fmap     g Empty        = Empty
  fmap     g (Leaf x)     = Leaf $ g x
  fmap     g (Node l x r) = Node (fmap g l)
                                 (g x)
                                 (fmap g r)
```

`Tree`의 `Traversable`과 `Functor` 인스턴스가 구조적으로 동일하다는 것이 분명히 보일 것이다. 유일한 차이는 `Functor` 인스턴스는 보통의 함수 적용을 쓰는 반면, `Traversable` 인스턴스의 적용은 `(<$>)`와 `(<*>)`를 사용해 `Applicative` 문맥 안에서 이루어진다는 것이다. 이 패턴은 어떤 타입에서든 똑같이 성립한다.

모든 `Traversable` 펑터는 `Foldable`이자 `Functor`이기도 하다. 이는 클래스 선언에서뿐 아니라, `Traversable` 메서드만으로 두 클래스의 메서드를 구현할 수 있다는 사실에서도 알 수 있다.

표준 라이브러리는 `[]`, `ZipList`, `Maybe`, `((,) e)`, `Sum`, `Product`, `Either e`, `Map`, `Tree`, `Sequence` 등 많은 `Traversable` 인스턴스를 제공한다. 주목할 점은 `Set`이 `Foldable`이긴 하지만 `Traversable`은 아니라는 것이다.

**연습문제**

1. `Traversable` 메서드만 사용해 `fmap`과 `foldMap`을 구현하라. (`Traversable` 모듈은 이 구현들을 `fmapDefault`와 `foldMapDefault`로 제공한다.)
2. `[]`, `Maybe`, `((,) e)`, `Either e`의 `Traversable` 인스턴스를 구현하라.
3. `Set`이 `Foldable`이지만 `Traversable`은 아닌 이유를 설명하라.
4. `Traversable` 펑터들이 합성됨을 보여라. 즉 `f`와 `g`의 `Traversable` 인스턴스가 주어졌을 때 `Traversable (Compose f g)` 인스턴스를 구현하라.

### 법칙

`Traversable`의 인스턴스는 다음 두 법칙을 만족해야 한다. 여기서 `Identity`는 항등 펑터(`transformers` 패키지의 [`Data.Functor.Identity` 모듈](http://hackage.haskell.org/packages/archive/transformers/latest/doc/html/Data-Functor-Identity.html)에 정의된 대로)이고, `Compose`는 두 펑터의 합성을 감싼다([`Data.Functor.Compose`](http://hackage.haskell.org/packages/archive/transformers/0.3.0.0/doc/html/Data-Functor-Compose.html)에 정의된 대로).

1. `traverse Identity = Identity`
2. `traverse (Compose . fmap g . f) = Compose . fmap (traverse g) . traverse f`

첫 번째 법칙은 본질적으로 순회가 임의의 효과를 지어내서는 안 된다고 말한다. 두 번째 법칙은 두 순회를 연달아 하는 것이 어떻게 하나의 순회로 접힐 수 있는지 설명한다.

추가로, `eta`가 "`Applicative` 사상(morphism)"이라고 하자. 즉

```haskell
  eta :: forall a f g. (Applicative f, Applicative g) => f a -> g a
```

이고 `eta`가 `Applicative` 연산을 보존한다고 하자: `eta (pure x) = pure x`이고 `eta (x <*> y) = eta x <*> eta y`. 그러면 매개변수성(parametricity)에 의해, 위 두 법칙을 만족하는 `Traversable` 인스턴스는 `eta . traverse f = traverse (eta . f)`도 만족한다.

### 더 읽을거리

`Traversable` 클래스 역시 [McBride와 Paterson의 `Applicative` 논문](http://www.soi.city.ac.uk/~ross/papers/Applicative.html)에서 기원했고, Gibbons와 Oliveira의 [The Essence of the Iterator Pattern](http://www.comlab.ox.ac.uk/jeremy.gibbons/publications/iterator.pdf)에 더 자세히 기술되어 있다. 이 논문에는 관련 연구에 대한 풍부한 참고 문헌도 담겨 있다.

`Traversable`은 Edward Kmett의 [lens 라이브러리](http://hackage.haskell.org/package/lens)의 핵심 구성 요소다. [이 주제에 대한 Edward의 발표](https://vimeo.com/56063074)를 보는 것은 `Traversable`, `Foldable`, `Applicative`, 그 밖의 많은 것에 대한 더 나은 통찰을 얻는 강력 추천 방법이다.

`Traversable` 법칙에 대한 참고 자료로는 Russell O'Connor의 [메일링 리스트 글](http://archive.fo/7XcVE)(과 이어지는 스레드), 그리고 더 깊은 논의를 담은 [Jaskelioff와 Rypacek의 논문](https://arxiv.org/abs/1202.2919)을 보라. Daniel Mlot의 [아주 좋은 블로그 글](http://duplode.github.io/posts/traversable-a-remix.html)은 모나드의 통상적인 클라이슬리 범주의 변형을 고려하면 `Traversable`이 어떻게 등장하는지 설명하며, `Traversable` 법칙이 어디서 오는지도 밝혀 준다.

[Will Fancher의 이 블로그 글](http://elvishjerricco.github.io/2017/03/23/applicative-sorting.html)은 `Traversable`을 영리하게 고른 `Applicative`와 함께 사용해 임의의 `Traversable` 컨테이너를 효율적으로 정렬하는 방법을 보여 준다.

---

[Part 3에서 계속: Bifunctor, Category, Arrow, Comonad](014-typeclassopedia-part3.md)
