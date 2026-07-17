# Typeclassopedia (3/3): Bifunctor, Category, Arrow, Comonad

> 원문: [Typeclassopedia](https://wiki.haskell.org/Typeclassopedia)
> 저자: Brent Yorgey | 번역일: 2026-07-10

전체 목차:

- [Part 1](014-typeclassopedia-part1.md): 초록, 서론, Functor, Applicative, Monad
- [Part 2](014-typeclassopedia-part2.md): MonadFail, 모나드 트랜스포머, MonadFix, Semigroup, Monoid, 실패와 선택(Alternative, MonadPlus, ArrowPlus), Foldable, Traversable
- **Part 3 (이 문서)**: Bifunctor, Category, Arrow, Comonad, 감사의 말, 저자 소개, 콜로폰

## Bifunctor

`Functor`는 카인드가 `* -> *`인 타입으로, 타입 매개변수 위에 함수를 "매핑"할 수 있는 것임을 기억하자. `(Either e)`는 `Functor`이고(`fmap :: (a -> b) -> Either e a -> Either e b`), `((,) e)`도 그렇다. 그런데 이 두 예에는 묘하게 비대칭인 구석이 있다. 원리상 `a` 대신 `e` 위에 매핑하지 못할 이유가 없다. 예컨대 이렇게 말이다: `lmap :: (e -> e') -> Either e a -> Either e' a`. 이 관찰은 곧바로 `Bifunctor`의 정의로 이어진다. 카인드가 `* -> * -> *`인 타입에서 *두* 타입 매개변수 모두에 펑터처럼 매핑할 수 있는 클래스다.

### 정의

`Data.Bifunctor`(GHC 7.10에 딸려 온 `base-4.8`부터)에 정의된 `Bifunctor`의 타입 클래스 선언은 다음과 같다.

```haskell
class Bifunctor p where
  bimap  :: (a -> b) -> (c -> d) -> p a c -> p b d

  first  :: (a -> b) -> p a c -> p b c
  second :: (b -> c) -> p a b -> p a c
```

`p`가 두 개의 타입 인자에 적용된다는 사실로부터 그 카인드가 `* -> * -> *`임을 추론할 수 있다. `Bifunctor` 클래스의 가장 근본적인 메서드는 `bimap`으로, 두 타입 인자 위를 한 번에 매핑할 수 있게 해 준다. 예를 들면

```haskell
bimap (+1) length (4, [1,2,3]) = (5,3)
```

한 번에 한 타입 인자 위만 매핑하기 위한 `first`와 `second`도 제공된다. `bimap`을 정의하거나, `first`와 `second`를 둘 다 정의해야 한다. 각각은 서로를 이용한 기본 정의가 제공되기 때문이다. 즉:

```haskell
bimap f g = first f . second g

first  f = bimap f id
second g = bimap id g
```

### 법칙

`Bifunctor`의 법칙은 `Functor`의 법칙과 완전히 유사하다. 첫째, 항등 함수로 매핑하면 아무 효과가 없어야 한다.

```haskell
bimap id id = id
first id    = id
second   id = id
```

둘째, 합성으로 매핑하는 것은 매핑들의 합성과 같아야 한다.

```haskell
bimap (f . g) (h . i) = bimap f h . bimap g i

first  (f . g) = first f  . first g
second (f . g) = second f . second g
```

이 합성 법칙들은 사실 항등 법칙이 만족되면 "공짜로"(즉 매개변수성에 의해) 따라 나온다. 또한 `first`와 `second`의 기본 구현이 필요한 법칙을 만족하는 것과 `bimap`이 그런 것이 서로 필요충분 조건임을 확인할 수 있다.

`bimap`, `first`, `second`를 잇는 법칙이 하나 더 있다. 바로

```haskell
bimap f g = first f . second g
```

그러나 `bimap`만 정의하거나 `first`와 `second`만 정의하고 나머지에 기본 구현을 쓴다면, 이 법칙은 자동으로 성립한다. 따라서 (예컨대 효율 같은) 어떤 이유로 세 메서드를 모두 손으로 정의하는 경우에만 이 법칙을 신경 쓰면 된다.

대칭 법칙 `bimap f g = second g . first f`가 궁금할 수 있다. `bimap f g = first f . second g`가 성립하면 대칭 버전도 [매개변수성으로부터 따라 나오는](https://byorgey.wordpress.com/2018/03/30/parametricity-for-bifunctor/) 것으로 밝혀져 있다.

요약하면 진술할 수 있는 법칙은 많지만, 대부분은 기본 정의나 매개변수성으로부터 자동으로 따라 나온다. 예를 들어 `bimap`만 정의한다면 실제로 검사해야 할 법칙은 `bimap id id = id`뿐이다. 나머지 법칙은 모두 공짜로 온다. 마찬가지로 `first`와 `second`만 정의한다면 `first id = id`와 `second id = id`만 검사하면 된다.

### 인스턴스

* `(,)`와 `Either`는 자명한 방식으로 인스턴스다.

* 더 큰 튜플 생성자 중 일부도 인스턴스다. 예를 들어 `(,,)`의 인스턴스는 마지막 두 성분 위를 매핑하고 첫 성분은 그대로 둔다. 누가 왜 이걸 쓰고 싶어 할지는 불분명하다.

* 타입 `Const a b`의 값(뒤의 절에서 더 논의한다)은 단순히 타입 `a`의 값 하나로 이루어진다. `bimap f g`는 `f`를 그 `a` 위에 매핑하고 `g`는 무시한다.

## Category

`Category`는 Haskell 표준 라이브러리에 비교적 최근에 추가된 클래스다. 함수 합성의 개념을 일반적인 "사상(morphism)"으로 일반화한다.

> **참고**: GHC 7.6.1은 타입과 타입 변수에 관한 규칙을 바꿨다. 이제 타입 수준의 연산자는 타입 *변수*가 아니라 타입 *생성자*로 취급된다. GHC 7.6.1 이전에는 `` `arr` `` 대신 `(~>)`를 쓸 수 있었다. 자세한 내용은 [GHC-users 메일링 리스트의 논의](http://archive.fo/weS2f)를 보라. GHC 7.6.1에서 동작하는 멋진 화살표 표기의 새로운 접근법은 Edward Kmett의 [이 메시지](http://archive.fo/HhdvB)와 [이 메시지](http://archive.fo/iGx6W)를 보라. 다만 단순함을 위해 여기서는 채택하지 않았다.

`Category` 타입 클래스의 정의(`Control.Category`에 있음; [haddock](https://hackage.haskell.org/package/base/docs/Control-Category.html))는 아래와 같다. 읽기 쉽도록 중위 함수 타입 생성자 `(->)`에 맞춰 중위 타입 변수 `` `arr` ``를 사용했다. 이 문법은 Haskell 2010의 일부가 아니다. 두 번째로 보인 정의가 표준 라이브러리에서 실제로 쓰이는 것이다. 이 문서의 나머지 부분에서는 `Category`뿐 아니라 `Arrow`에 대해서도 중위 타입 생성자 `` `arr` ``를 사용한다.

```haskell
class Category arr where
  id  :: a `arr` a
  (.) :: (b `arr` c) -> (a `arr` b) -> (a `arr` c)

-- 같은 것을 보통의 (전위) 타입 생성자로 쓴 것
class Category cat where
  id  :: cat a a
  (.) :: cat b c -> cat a b -> cat a c
```

`Category`의 인스턴스는 타입 인자를 두 개 받는 타입, 즉 카인드가 `* -> * -> *`인 것이어야 한다는 점에 유의하라. 타입 변수 `cat`을 함수 생성자 `(->)`로 바꿔 상상해 보면 배울 점이 있다. 실제로 이 경우 표준 `Prelude`에 정의된 익숙한 항등 함수 `id`와 함수 합성 연산자 `(.)`를 정확히 되찾는다.

물론 `Category` 모듈은 정확히 그런 `(->)`의 `Category` 인스턴스를 제공한다. 그런데 인스턴스를 하나 더 제공하는데, 아래에 보인 것으로, 앞서 `Monad` 법칙 논의에서 익숙할 것이다. `Control.Arrow` 모듈에 정의된 `Kleisli m a b`는 `a -> m b`의 `newtype` 래퍼일 뿐이다.

```haskell
newtype Kleisli m a b = Kleisli { runKleisli :: a -> m b }

instance Monad m => Category (Kleisli m) where
  id :: Kleisli m a a
  id = Kleisli return

  (.) :: Kleisli m b c -> Kleisli m a b -> Kleisli m a c
  Kleisli g . Kleisli h = Kleisli (h >=> g)
```

`Category` 인스턴스가 만족해야 하는 법칙은 `id`가 `(.)`의 항등원이어야 하고 `(.)`가 결합적이어야 한다는 것뿐이다. 모노이드가 되는 것과 비슷하지만, 모노이드와 달리 아무 두 값이나 `(.)`로 합성할 수 있는 것은 아니다—타입이 맞아떨어져야 한다.

마지막으로 `Category` 모듈은 두 연산자를 추가로 익스포트한다. `(<<<)`는 그냥 `(.)`의 동의어이고, `(>>>)`는 인자 순서를 뒤집은 `(.)`다. (이전 버전의 라이브러리에서는 이 연산자들이 `Arrow` 클래스의 일부로 정의되어 있었다.)

### 더 읽을거리

`Category`라는 이름은 다소 오해를 부른다. `Category` 클래스는 임의의 범주를 표현할 수 없고, 대상(object)이 Haskell 타입들의 범주 `Hask`의 대상인 범주만 표현할 수 있기 때문이다. Haskell 안에서 범주를 더 일반적으로 다루는 것에 대해서는 [category-extras 패키지](http://hackage.haskell.org/package/category-extras)를 보라. 범주론 일반에 대해서는 훌륭한 [Haskell wikibook 페이지](http://en.wikibooks.org/wiki/Haskell/Category_theory), [Steve Awodey의 책](http://books.google.com/books/about/Category_theory.html?id=-MCJ6x2lC7oC), Benjamin Pierce의 [Basic category theory for computer scientists](http://books.google.com/books/about/Basic_category_theory_for_computer_scien.html?id=ezdeaHfpYPwC), 또는 [Barr와 Wells의 범주론 강의 노트](http://folli.loria.fr/cds/1999/esslli99/courses/barr-wells.html)를 보라. [Benjamin Russell의 블로그 글](http://dekudekuplex.wordpress.com/2009/01/19/motivating-learning-category-theory-for-non-mathematicians/)도 동기 부여와 범주론 링크의 좋은 원천이다. 성공적이고 생산적인 Haskell 프로그래머가 되기 위해 범주론을 알 필요는 전혀 없지만, 범주론은 Haskell의 기저 이론을 훨씬 깊이 음미할 수 있게 해 준다.

## Arrow

`Arrow` 클래스는 `Monad`, `Applicative`와 비슷한 계열의, 계산에 대한 또 하나의 추상화를 나타낸다. 그러나 타입이 출력만 반영하는 `Monad`나 `Applicative`와 달리, `Arrow` 계산의 타입은 입력과 출력을 모두 반영한다. 화살표(arrow)는 함수를 일반화한다. `arr`가 `Arrow`의 인스턴스라면, 타입 `` b `arr` c ``의 값은 타입 `b`의 값을 입력으로 받아 타입 `c`의 값을 출력으로 내는 계산으로 생각할 수 있다. `(->)`의 `Arrow` 인스턴스에서 이는 그냥 순수 함수지만, 일반적으로 화살표는 어떤 종류의 "효과 있는" 계산을 나타낼 수 있다.

### 정의

`Control.Arrow`에 있는 `Arrow` 타입 클래스의 정의([haddock](https://hackage.haskell.org/package/base/docs/Control-Arrow.html))는 다음과 같다.

```haskell
class Category arr => Arrow arr where
  arr :: (b -> c) -> (b `arr` c)
  first :: (b `arr` c) -> ((b, d) `arr` (c, d))
  second :: (b `arr` c) -> ((d, b) `arr` (d, c))
  (***) :: (b `arr` c) -> (b' `arr` c') -> ((b, b') `arr` (c, c'))
  (&&&) :: (b `arr` c) -> (b `arr` c') -> (b `arr` (c, c'))
```

> **참고**: 버전 4 이전의 `base` 패키지에는 `Category` 클래스가 없고, `Arrow` 클래스가 화살표 합성 연산자 `(>>>)`를 포함한다. `arr`의 동의어로 `pure`도 포함했지만, `Applicative`의 `pure`와 충돌해 제거되었다.

먼저 주목할 것은 `Category` 클래스 제약이다. 덕분에 항등 화살표와 화살표 합성이 공짜로 따라온다. 두 화살표 `` g :: b `arr` c ``와 `` h :: c `arr` d ``가 주어지면 그 합성 `` g >>> h :: b `arr` d ``를 만들 수 있다.

이제는 익숙한 패턴이겠지만, 새 `Arrow` 인스턴스를 작성할 때 반드시 정의해야 하는 메서드는 `arr`와 `first`뿐이다. 다른 메서드들은 이들을 이용한 기본 정의가 있지만, 원한다면 더 효율적인 구현으로 재정의할 수 있도록 `Arrow` 클래스에 포함되어 있다.

`first`와 `second`는 `Data.Bifunctor`의 동명 메서드와 충돌한다는 점에 유의하라. 어떤 이유로 둘 다 쓰고 싶다면 하나 또는 둘 다를 한정 임포트(qualified import)해야 한다. 예전에는 `first`나 `second`로 순서쌍을 편집하기 위한 `(->)` 인스턴스를 얻으려고 `Control.Arrow`를 임포트하는 일이 흔했다. 지금은 그 목적으로는 `Data.Bifunctor`를 임포트하는 것이 권장된다. (`Arrow`의 `(->)` 인스턴스와 `Bifunctor`의 `(,)` 인스턴스에서 `first`와 `second`는 같은 타입으로 특수화된다는 점에 주목하라.)

### 직관

화살표 메서드를 하나씩 살펴보자. [Ross Paterson의 화살표 웹 페이지](http://www.haskell.org/arrows/)에 직관을 쌓는 데 도움이 되는 좋은 다이어그램들이 있다.

* `arr` 함수는 임의의 함수 `b -> c`를 받아 일반화된 화살표 `` b `arr` c ``로 바꾼다. `arr` 메서드는 화살표가 함수를 일반화한다는 주장을 정당화한다. 어떤 함수든 화살표로 취급할 수 있다는 말이기 때문이다. 화살표 `arr g`는 `g`만 계산하고 (특정 화살표 타입에서 그것이 무엇을 뜻하든) 어떤 "효과"도 없다는 의미에서 "순수"하도록 의도되어 있다.

* `first` 메서드는 `b`에서 `c`로 가는 임의의 화살표를 `(b,d)`에서 `(c,d)`로 가는 화살표로 바꾼다. `first g`는 튜플의 첫 요소를 `g`로 처리하고 두 번째 요소는 그대로 통과시킨다는 아이디어다. `Arrow`의 함수 인스턴스에서는 물론 `first g (x,y) = (g x, y)`다.

* `second` 함수는 `first`와 비슷하되 튜플의 요소가 뒤바뀐 것이다. 실제로 `swap (x,y) = (y,x)`로 정의되는 보조 함수 `swap`을 사용해 `first`로 정의할 수 있다.

* `(***)` 연산자는 화살표의 "병렬 합성"이다. 화살표 두 개를 받아 튜플 위의 화살표 하나로 만드는데, 튜플의 첫 요소에는 첫 화살표의 동작을, 두 번째 요소에는 두 번째 화살표의 동작을 한다. 기억하는 요령은 `g *** h`가 `g`와 `h`의 *곱(product)*(그래서 `*`)이라는 것이다. `Arrow`의 함수 인스턴스에서는 `(g *** h) (x,y) = (g x, h y)`로 정의한다. `(***)`의 기본 구현은 `first`, `second`, 그리고 순차 화살표 합성 `(>>>)`으로 되어 있다. 독자는 `first`와 `second`를 `(***)`로 구현하는 방법도 생각해 보면 좋다.

* `(&&&)` 연산자는 화살표의 "팬아웃(fanout) 합성"이다. 두 화살표 `g`와 `h`를 받아, 입력을 `g`와 `h` 모두의 입력으로 공급하고 그 결과들을 튜플로 반환하는 새 화살표 `g &&& h`를 만든다. 기억하는 요령은 `g &&& h`가 입력에 대해 `g` *그리고(and)* `h`를 (그래서 `&`) 모두 수행한다는 것이다. 함수에 대해서는 `(g &&& h) x = (g x, h x)`로 정의한다.

### 인스턴스

`Arrow` 라이브러리 자체는 `Arrow` 인스턴스를 두 개만 제공하며, 둘 다 이미 본 것이다. 보통의 함수 생성자 `(->)`와, 임의의 `Monad m`에 대해 타입 `a -> m b`의 함수를 `Arrow`로 만드는 `Kleisli m`이다. 이 인스턴스들은 다음과 같다.

```haskell
instance Arrow (->) where
  arr :: (b -> c) -> (b -> c)
  arr g = g

  first :: (b -> c) -> ((b,d) -> (c,d))
  first g (x,y) = (g x, y)

newtype Kleisli m a b = Kleisli { runKleisli :: a -> m b }

instance Monad m => Arrow (Kleisli m) where
  arr :: (b -> c) -> Kleisli m b c
  arr f = Kleisli (return . f)

  first :: Kleisli m b c -> Kleisli m (b,d) (c,d)
  first (Kleisli f) = Kleisli (\ ~(b,d) -> do c <- f b
                                              return (c,d) )
```

### 법칙

> **참고**: [John Hughes: Generalising monads to arrows](http://dx.doi.org/10.1016/S0167-6423(99)00023-4); [Sam Lindley, Philip Wadler, Jeremy Yallop: The arrow calculus](http://homepages.inf.ed.ac.uk/wadler/papers/arrows/arrows.pdf); [Ross Paterson: Programming with Arrows](http://www.soi.city.ac.uk/~ross/papers/fop.html)를 보라.

`Arrow`의 인스턴스가 만족해야 하는 법칙은 꽤 많다.

```haskell
                       arr id  =  id
                  arr (h . g)  =  arr g >>> arr h
                first (arr g)  =  arr (g *** id)
              first (g >>> h)  =  first g >>> first h
   first g >>> arr (id *** h)  =  arr (id *** h) >>> first g
          first g >>> arr fst  =  arr fst >>> g
first (first g) >>> arr assoc  =  arr assoc >>> first g

assoc ((x,y),z) = (x,(y,z))
```

이 버전의 법칙들은 처음 두 참고 문헌에 나온 법칙들과 약간 다르다는 점에 유의하라. 몇몇 법칙이 이제 `Category` 법칙(특히 `id`가 항등 화살표라는 것과 `(>>>)`가 결합적이라는 요구)에 흡수되었기 때문이다. 여기 보인 법칙들은 `Category` 클래스를 사용하는 Paterson의 Programming with Arrows를 따른다.

> **참고**: 범주론이 유발하는 불면증이 취향이라면 이야기가 다르지만.

`Arrow` 법칙 때문에 잠을 너무 설치지는 말라고 권한다. 화살표로 프로그래밍하는 데 법칙을 이해하는 것이 필수는 아니다. `ArrowChoice`, `ArrowApply`, `ArrowLoop` 인스턴스가 만족해야 하는 법칙도 있다. 관심 있는 독자는 [Paterson: Programming with Arrows](http://www.soi.city.ac.uk/~ross/papers/fop.html)를 참고하라.

### ArrowChoice

`Applicative` 클래스로 만든 계산과 마찬가지로, `Arrow` 클래스로 만든 계산도 상당히 융통성이 없다. 계산의 구조가 처음부터 고정되어 있고, 중간 결과에 따라 다른 실행 경로를 선택할 능력이 없다. `ArrowChoice` 클래스가 정확히 그런 능력을 제공한다.

```haskell
class Arrow arr => ArrowChoice arr where
  left  :: (b `arr` c) -> (Either b d `arr` Either c d)
  right :: (b `arr` c) -> (Either d b `arr` Either d c)
  (+++) :: (b `arr` c) -> (b' `arr` c') -> (Either b b' `arr` Either c c')
  (|||) :: (b `arr` d) -> (c `arr` d) -> (Either b c `arr` d)
```

`ArrowChoice`를 `Arrow`와 비교해 보면 `left`, `right`, `(+++)`, `(|||)`가 각각 `first`, `second`, `(***)`, `(&&&)`와 놀랍도록 평행하다는 것이 드러난다. 실제로 이들은 쌍대(dual)다. `first`, `second`, `(***)`, `(&&&)`는 모두 곱 타입(튜플) 위에서 동작하고, `left`, `right`, `(+++)`, `(|||)`는 합 타입 위의 대응 연산이다. 일반적으로 이 연산들은 입력에 `Left`나 `Right` 태그가 붙어 있고 그 태그에 따라 어떻게 행동할지 선택할 수 있는 화살표를 만든다.

* `g`가 `b`에서 `c`로 가는 화살표라면, `left g`는 `Either b d`에서 `Either c d`로 가는 화살표다. `Left` 태그가 붙은 입력에는 `g`의 동작을 하고, `Right` 태그가 붙은 입력에는 항등처럼 행동한다.

* `right` 함수는 물론 `left`의 거울상이다. 화살표 `right g`는 `Right` 태그가 붙은 입력에 `g`의 동작을 한다.

* `(+++)` 연산자는 "멀티플렉싱"을 수행한다. `g +++ h`는 `Left` 태그가 붙은 입력에는 `g`처럼, `Right` 태그가 붙은 입력에는 `h`처럼 행동한다. 태그는 보존된다. `(+++)` 연산자는 두 화살표의 *합(sum)*(그래서 `+`)이며, `(***)`가 곱인 것과 짝을 이룬다.

* `(|||)` 연산자는 "병합" 내지 "팬인(fanin)"이다. 화살표 `g ||| h`는 `Left` 태그가 붙은 입력에는 `g`처럼, `Right` 태그가 붙은 입력에는 `h`처럼 행동하되 태그는 버린다(따라서 `g`와 `h`의 출력 타입이 같아야 한다). 기억하는 요령은 `g ||| h`가 입력에 대해 `g` *또는(or)* `h`를 수행한다는 것이다.

`ArrowChoice` 클래스는 계산이 중간 결과에 따라 유한개의 실행 경로 중에서 선택할 수 있게 해 준다. 가능한 실행 경로는 미리 알려져 있어야 하고, `(+++)`나 `(|||)`로 명시적으로 조립되어야 한다. 그러나 때로는 더 큰 유연성이 필요하다. 중간 결과로부터 화살표를 *계산*하고, 그 계산된 화살표로 계산을 이어 가고 싶은 것이다. 이것이 `ArrowApply`가 우리에게 주는 힘이다.

### ArrowApply

`ArrowApply` 타입 클래스는 다음과 같다.

```haskell
class Arrow arr => ArrowApply arr where
  app :: (b `arr` c, b) `arr` c
```

앞선 계산의 출력으로 화살표를 계산했다면, `app`은 그 화살표를 입력에 적용해 그 출력을 `app`의 출력으로 내놓을 수 있게 해 준다. 연습 삼아 독자는 `app`을 사용해 대안적인 "커링된" 버전 `` app2 :: b `arr` ((b `arr` c) `arr` c) ``를 구현해 보면 좋다.

새로운 계산을 *계산*할 수 있다는 이 개념은 익숙하게 들릴 것이다. 이는 정확히 모나드의 bind 연산자 `(>>=)`가 하는 일이다. `ArrowApply`와 `Monad`가 표현력에서 정확히 동등하다는 것은 그리 놀랍지 않을 것이다. 특히 `Kleisli m`은 `ArrowApply`의 인스턴스로 만들 수 있고, `ArrowApply`의 어떤 인스턴스든 (`newtype` 래퍼 `ArrowMonad`를 통해) `Monad`로 만들 수 있다. 연습 삼아 독자는 이 인스턴스들을 구현해 보면 좋다.

```haskell
class Arrow arr => ArrowApply arr where
  app :: (b `arr` c, b) `arr` c

instance Monad m => ArrowApply (Kleisli m) where
  app :: Kleisli m (Kleisli m b c, b) c
  app =    -- 연습문제

newtype ArrowApply a => ArrowMonad a b = ArrowMonad (a () b)

instance ArrowApply a => Monad (ArrowMonad a) where
  return :: b -> ArrowMonad a b
  return               =    -- 연습문제

  (>>=) :: ArrowMonad a a -> (a -> ArrowMonad a b) -> ArrowMonad a b
  (ArrowMonad a) >>= k =    -- 연습문제
```

### ArrowLoop

`ArrowLoop` 타입 클래스는 다음과 같다.

```haskell
class Arrow a => ArrowLoop a where
  loop :: a (b, d) (c, d) -> a b c

trace :: ((b,d) -> (c,d)) -> b -> c
trace f b = let (c,d) = f (b,d) in c
```

이 클래스는 재귀를 사용해 결과를 계산할 수 있는 화살표를 기술하며, (아래에서 설명할) 화살표 표기법의 `rec` 구문을 탈설탕하는 데 쓰인다.

그 자체만 보면 `loop` 메서드의 타입은 별로 말해 주는 것이 없어 보인다. 그러나 그 의도는 함께 보인 `trace` 함수의 일반화다. 첫 번째 화살표 출력의 `d` 성분이 자신의 입력으로 되먹임된다. 다시 말해 화살표 `loop g`는 `g` 입력의 두 번째 성분을 재귀적으로 "고정"해서 얻는다.

`trace` 함수가 무엇을 하는지 파악하기는 좀 어려울 수 있다. `d`가 어떻게 `let`의 좌변과 우변에 동시에 나타날 수 있는가? 이것이 바로 Haskell의 게으름(laziness)이 작동하는 방식이다. 여기서 온전히 설명할 지면은 없으니, 관심 있는 독자는 표준 `fix` 함수를 공부하고 [Paterson의 화살표 튜토리얼](http://www.soi.city.ac.uk/~ross/papers/fop.html)을 읽어 보길 권한다.

### 화살표 표기법

화살표 콤비네이터로 직접 프로그래밍하는 것은 고통스러울 수 있다. 특히 여러 중간 결과를 동시에 참조해야 하는 복잡한 계산을 쓸 때 그렇다. 화살표 콤비네이터만으로는 그런 중간 결과들을 중첩 튜플에 보관해야 하며, 어느 중간 결과가 어느 성분에 있는지 기억하고 필요할 때마다 튜플을 뒤바꾸고 재결합하고 온갖 방식으로 주무르는 일은 전적으로 프로그래머의 몫이다. 이 문제는 GHC가 지원하는, 모나드의 `do` 표기법과 비슷한 특별한 화살표 표기법으로 해결된다. 화살표 계산을 조립하면서 중간 결과에 이름을 붙일 수 있다. Paterson에게서 가져온, 화살표 표기법으로 구현한 화살표 예제는 다음과 같다.

```haskell
class ArrowLoop arr => ArrowCircuit arr where
  delay :: b -> (b `arr` b)

counter :: ArrowCircuit arr => Bool `arr` Int
counter = proc reset -> do
            rec output <- idA     -< if reset then 0 else next
                next   <- delay 0 -< output + 1
            idA -< output
```

이 화살표는 리셋 선이 있는, 재귀적으로 정의된 카운터 회로를 나타내려는 것이다.

화살표 표기법을 온전히 설명할 지면은 없다. 관심 있는 독자는 [표기법을 도입한 Paterson의 논문](http://www.soi.city.ac.uk/~ross/papers/notation.html)이나, 단순화된 버전을 제시하는 그의 이후 [튜토리얼](http://www.soi.city.ac.uk/~ross/papers/fop.html)을 참고하라.

### 더 읽을거리

화살표를 공부하는 학생에게 훌륭한 출발점은 [arrows 웹 페이지](http://www.haskell.org/arrows/)다. 입문과 많은 참고 문헌이 있다. 화살표에 관한 핵심 논문으로는 화살표를 도입한 Hughes의 원 논문 [Generalising monads to arrows](http://dx.doi.org/10.1016/S0167-6423(99)00023-4)와 [화살표 표기법에 관한 Paterson의 논문](http://www.soi.city.ac.uk/~ross/papers/notation.html)이 있다.

Hughes와 Paterson은 이후 더 넓은 독자층을 위한 접근하기 쉬운 튜토리얼도 각각 썼다. [Paterson: Programming with Arrows](http://www.soi.city.ac.uk/~ross/papers/fop.html)와 [Hughes: Programming with Arrows](http://www.cse.chalmers.se/~rjmh/afp-arrows.pdf)다.

`Arrow` 클래스를 정의한 Hughes의 목표는 `Monad`를 일반화하는 것이었고, `Arrow`가 힘에서 "`Applicative`와 `Monad` 사이"에 있다고들 말하지만, 둘은 직접 비교할 수 있는 대상이 아니다. 정확한 관계는 [Lindley, Wadler, Yallop이 분석](http://homepages.inf.ed.ac.uk/wadler/papers/arrows-and-idioms/arrows-and-idioms.pdf)하기 전까지 다소 혼란 속에 있었다. 이들은 람다 대수에 기반한 새로운 화살표 계산법(calculus)도 발명했는데, 이는 화살표 법칙의 제시를 상당히 단순화한다([The arrow calculus](http://homepages.inf.ed.ac.uk/wadler/papers/arrows/arrows.pdf) 참고). [`Arrow`를 `Applicative`와 `Category`의 교집합으로 볼 수 있다](http://just-bottom.blogspot.de/2010/04/programming-with-effects-story-so-far.html)는 정확한 기술적 의미도 있다.

`Arrow`의 사용 예로는 [Yampa](https://wiki.haskell.org/Yampa), [Haskell XML Toolkit](http://www.fh-wedel.de/~si/HXmlToolbox/), 함수형 GUI 라이브러리 [Grapefruit](https://wiki.haskell.org/Grapefruit)이 있다.

화살표의 확장도 몇 가지 탐구되었다. 예를 들어 단방향이 아닌 양방향 계산을 위한 Alimarine 등의 `BiArrow`가 있다([There and Back Again: Arrows for Invertible Programming](http://wiki.clean.cs.ru.nl/download/papers/2005/alia2005-biarrowsHaskellWorkshop.pdf)).

Haskell 위키에는 [`Arrow`에 관련된 추가 연구 논문 링크](https://wiki.haskell.org/Research_papers/Monads_and_arrows)가 많이 있다.

## Comonad

우리가 살펴볼 마지막 타입 클래스는 `Comonad`다. `Comonad` 클래스는 `Monad`의 범주론적 쌍대다. 즉 `Comonad`는 `Monad`에서 모든 함수 화살표를 뒤집은 것과 같다. 실제로 표준 Haskell 라이브러리에는 없지만, 최근 흥미로운 용례가 좀 있어서 완전성을 위해 여기 싣는다.

### 정의

[comonad 라이브러리](http://hackage.haskell.org/package/comonad)의 `Control.Comonad` 모듈에 정의된 `Comonad` 타입 클래스는 다음과 같다.

```haskell
class Functor w => Comonad w where
  extract :: w a -> a

  duplicate :: w a -> w (w a)
  duplicate = extend id

  extend :: (w a -> b) -> w a -> w b
  extend f = fmap f . duplicate
```

보다시피 `extract`는 `return`의 쌍대이고, `duplicate`는 `join`의 쌍대이며, `extend`는 `(=<<)`의 쌍대다. `Comonad`의 정의는 약간 중복이 있어서, 프로그래머가 `extend`와 `duplicate` 중 무엇을 구현할지 선택할 수 있다. 나머지 연산에는 기본 구현이 있다.

`Comonad` 인스턴스의 원형적인 예는 다음과 같다.

```haskell
-- 무한 게으른 스트림
data Stream a = Cons a (Stream a)

-- 'duplicate'는 리스트 함수 'tails'와 비슷하다
-- 'extend'는 옛 Stream으로부터 새 Stream을 계산하는데,
--   위치 n의 요소는 옛 Stream의 위치 n 이후 전체에 대한
--   함수로 계산된다
instance Comonad Stream where
  extract :: Stream a -> a
  extract (Cons x _) = x

  duplicate :: Stream a -> Stream (Stream a)
  duplicate s@(Cons x xs) = Cons s (duplicate xs)

  extend :: (Stream a -> b) -> Stream a -> Stream b
  extend g s@(Cons x xs)  = Cons (g s) (extend g xs)
                       -- = fmap g (duplicate s)
```

### 더 읽을거리

Dan Piponi는 블로그 글에서 [셀룰러 오토마타가 코모나드와 무슨 관계인지](http://blog.sigfpe.com/2006/12/evaluating-cellular-automata-is.html) 설명한다. 또 다른 블로그 글에서 Conal Elliott은 [함수형 반응형 프로그래밍의 코모나드적 정식화](http://conal.net/blog/posts/functional-interactive-behavior/)를 검토했다. Sterling Clover의 블로그 글 [Comonads in everyday life](http://fmapfixreturn.wordpress.com/2008/07/09/comonads-in-everyday-life/)는 코모나드와 지퍼(zipper)의 관계, 그리고 코모나드로 웹사이트 메뉴 시스템을 설계하는 방법을 설명한다.

Uustalu와 Vene는 코모나드와 함수형 프로그래밍에 관련된 아이디어를 탐구하는 논문을 여럿 썼다.

* [Comonadic Notions of Computation](http://dx.doi.org/10.1016/j.entcs.2008.05.029)
* [The dual of substitution is redecoration](http://www.ioc.ee/~tarmo/papers/sfp01-book.pdf) ([ps.gz](http://www.cs.ut.ee/~varmo/papers/sfp01-book.ps.gz)로도 제공)
* [Recursive coalgebras from comonads](http://dx.doi.org/10.1016/j.ic.2005.08.005)
* [Recursion schemes from comonads](http://www.fing.edu.uy/~pardo/papers/njc01.ps.gz)
* [The Essence of Dataflow Programming](http://cs.ioc.ee/~tarmo/papers/essence.pdf)

Gabriel Gonzalez의 [Comonads are objects](http://www.haskellforall.com/2013/02/you-could-have-invented-comonads.html)는 코모나드와 객체지향 프로그래밍의 유사점을 짚는다.

[comonad-transformers](http://hackage.haskell.org/package/comonad-transformers) 패키지에는 코모나드 트랜스포머가 들어 있다.

## 감사의 말

표준 Haskell 타입 클래스에 대해 가르쳐 주고 좋은 직관을 기르도록 도와준 모든 분들, 특히 Jules Bean(quicksilver), Derek Elkins(ddarius), Conal Elliott(conal), Cale Gibbard(Cale), David House, Dan Piponi(sigfpe), Kevin Reid(kpreid)에게 특별히 감사한다.

Typeclassopedia 초고에 산더미 같은 유익한 피드백과 제안을 준 많은 분들에게도 감사한다. David Amos, Kevin Ballard, Reid Barton, Doug Beardsley, Joachim Breitner, Andrew Cave, David Christiansen, Gregory Collins, Mark Jason Dominus, Conal Elliott, Yitz Gale, George Giorgidze, Steven Grady, Travis Hartwell, Steve Hicks, Philip Hölzenspies, Edward Kmett, Eric Kow, Serge Le Huitouze, Felipe Lessa, Stefan Ljungstrand, Eric Macaulay, Rob MacAulay, Simon Meier, Eric Mertens, Tim Newsham, Russell O'Connor, Conrad Parker, Walt Rorie-Baety, Colin Ross, Tom Schrijvers, Aditya Siram, C. Smith, Martijn van Steenbergen, Joe Thornber, Jared Updike, Rob Vollmert, Andrew Wagner, Louis Wasserman, Ashley Yakeley, 그리고 IRC 닉으로만 아는 몇 분들: b_jonas, maltem, tehgeekmeister, ziman. 분명 몇 분을 무심코 빠뜨렸겠지만, 그렇다고 내 감사가 줄어드는 것은 결코 아니다.

마지막으로 Monad.Reader를 편집하는 환상적인 작업을 해 준 Wouter Swierstra와, Typeclassopedia를 쓰는 동안 인내해 준 아내 Joyia에게 감사한다.

## 저자 소개

Brent Yorgey([블로그](http://byorgey.wordpress.com/), [홈페이지](http://www.cis.upenn.edu/~byorgey/))는 (2011년 11월 기준) [펜실베이니아 대학교](http://www.upenn.edu) [프로그래밍 언어 그룹](http://www.cis.upenn.edu/~plclub/)의 박사 과정 4년 차 학생이다. 가르치는 일, EDSL 만들기, 바흐 푸가 연주, 범주론에 대한 사색, 그리고 #haskell 주민들을 위해 맛있는 람다 간식을 요리하는 것을 즐긴다.

## 콜로폰

Typeclassopedia는 Brent Yorgey가 썼고 2009년 3월에 처음 게재되었다. 2011년 11월 Geheimdienst가 Brent의 허락을 구한 뒤 공들여 위키 문법으로 변환했다.

이런 TeX-위키 문법 변환을 다시 해야 할 일이 생긴다면, 도움이 되었던 vim 명령들은 다음과 같다.

* `%s/\\section{\([^}]*\)}/=\1=/gc`
* `%s/\\subsection{\([^}]*\)}/==\1==/gc`
* `%s/^ *\\item /\r* /gc`
* `%s/---/—/gc`
* `%s/\$\([^$]*\)\$/<math>\1\\ <\/math>/gc` — "\ "를 덧붙이면 이미지 렌더링이 강제된다. 그렇지 않으면 Mediawiki가 짧은 `<math>` 태그에는 이 글꼴, (몇 글자 이상을 담은) 긴 태그에는 더 TeX스러운 다른 글꼴을 오락가락하며 쓴다.
* `%s/|\([^|]*\)|/<code>\1<\/code>/gc`
* `%s/\\dots/.../gc`
* `%s/^\\label{.*$//gc`
* `%s/\\emph{\([^}]*\)}/''\1''/gc`
* `%s/\\term{\([^}]*\)}/''\1''/gc`

가장 큰 문제는 학술 논문 스타일의 인용을 적절한 제목과 적절한 대상을 가진 하이퍼링크로 바꾸는 일이었다. 대부분은 자명하게 할 일이 있었다(예컨대 인용된 논문의 온라인 PDF나 CiteSeer 항목). 그러나 때로는 덜 분명해서, [원본 Typeclassopedia PDF](https://wiki.haskell.org/wikiupload/e/e9/Typeclassopedia.pdf)를 [원본 참고 문헌 파일](http://code.haskell.org/~byorgey/TMR/Issue13/typeclassopedia.bib)과 대조하고 싶을 수 있다.

모든 인용을 본문에 넣기 위해 처음에는 소스를 TeX이나 Lyx로 처리해 보려 했다. 찾을 수 없는 누락 패키지, 문법 오류, 그리고 TeX에 대한 나의 전반적인 무능 때문에 실패했다.

그다음에는 차선책으로 보이는 방법을 택했다. 소스에서 "\cite{something}"의 모든 출현을 추출하고, *그 순서대로* .bib 파일에서 참조된 항목들을 뽑아내는 것이다. 이렇게 하면 소스 파일과 정렬된 참조 파일을 나란히 훑으면서 필요한 것을 옮겨 적을 수 있고, .bib 파일을 앞뒤로 뒤질 필요가 없다. 다음을 사용했다.

* `egrep -o "\cite\{[^\}]*\}" ~/typeclassopedia.lhs | cut -c 6- | tr "," "\n" | tr -d "}" > /tmp/citations`
* `for i in $(cat /tmp/citations); do  grep -A99 "$i" ~/typeclassopedia.bib|egrep -B99 '^\}$' -m1 ; done > ~/typeclasso-refs-sorted`
