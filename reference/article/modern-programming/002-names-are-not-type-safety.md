# 이름은 타입 안전성이 아니다

> 원문: [Names are not type safety](https://lexi-lambda.github.io/blog/2020/11/01/names-are-not-type-safety/)
> 저자: Alexis King | 번역일: 2026-07-10

Haskell 프로그래머는 _타입 안전성_ 이야기에 많은 시간을 쓴다. Haskell식 프로그램 구성법은 "불변식을 타입 시스템에 담아라", "잘못된 상태를 표현 불가능하게 만들어라"를 주창한다. 둘 다 매력적인 목표로 들리지만, 그걸 달성하는 기법에 대해서는 꽤 모호하다. 거의 정확히 1년 전, 나는 그 간극을 잇는 첫 시도로 [Parse, Don't Validate](https://lexi-lambda.github.io/blog/2019/11/05/parse-don-t-validate/)를 발표했다.

이어진 논의는 대체로 생산적이고 올바른 방향이었지만, 혼란의 원천 하나가 금세 분명해졌다. Haskell의 `newtype` 구문이다. 아이디어 자체는 단순하다. `newtype` 키워드는 감싸는 타입과 이름으로는(nominally) 구별되지만 표현으로는(representationally) 동등한 래퍼 타입을 선언한다. 겉보기에는 타입 안전성으로 가는 단순하고 곧은 길처럼 _들린다_. 예를 들어 이메일 주소 타입을 `newtype` 선언으로 정의할 수 있겠다.

```haskell
newtype EmailAddress = EmailAddress Text
```

이 기법은 _어느 정도_ 가치를 줄 수 있고, 스마트 생성자와 캡슐화 경계를 곁들이면 어느 정도 안전성도 줄 수 있다. 하지만 이는 내가 1년 전에 강조한 것과는 의미 있게 다른 _종류_ 의 타입 안전성이며, 훨씬 약하다. newtype은 그 자체로는 그냥 이름일 뿐이다.

그리고 이름은 타입 안전성이 아니다.

## 내재적 안전성과 외재적 안전성

구성적 데이터 모델링([이전 글](https://lexi-lambda.github.io/blog/2020/08/13/types-as-axioms-or-playing-god-with-static-types/)에서 길게 다뤘다)과 newtype 래퍼의 차이를 보여 주는 예를 하나 생각해 보자. "1 이상 5 이하의 정수"를 위한 타입을 원한다고 하자. 자연스러운 구성적 모델링은 다섯 케이스를 가진 열거형이다.

```haskell
data OneToFive
  = One
  | Two
  | Three
  | Four
  | Five
```

그리고 `Int`와 `OneToFive` 타입 사이를 변환하는 함수를 몇 개 작성할 수 있다.

```haskell
toOneToFive :: Int -> Maybe OneToFive
toOneToFive 1 = Just One
toOneToFive 2 = Just Two
toOneToFive 3 = Just Three
toOneToFive 4 = Just Four
toOneToFive 5 = Just Five
toOneToFive _ = Nothing

fromOneToFive :: OneToFive -> Int
fromOneToFive One   = 1
fromOneToFive Two   = 2
fromOneToFive Three = 3
fromOneToFive Four  = 4
fromOneToFive Five  = 5
```

우리가 밝힌 목표를 달성하기에는 이걸로 충분하지만, 이상하다고 느껴도 무리는 아니다. 실제로 다루기에는 꽤 불편할 것이다. 완전히 새로운 타입을 발명했으니 Haskell이 제공하는 일반적인 수치 함수를 하나도 재사용할 수 없다. 그래서 많은 프로그래머가 대신 newtype 래퍼로 기울 것이다.

```haskell
newtype OneToFive = OneToFive Int
```

이전과 마찬가지로, 동일한 타입의 `toOneToFive`와 `fromOneToFive` 함수를 제공할 수 있다.

```haskell
toOneToFive :: Int -> Maybe OneToFive
toOneToFive n
  | n >= 1 && n <= 5 = Just $ OneToFive n
  | otherwise        = Nothing

fromOneToFive :: OneToFive -> Int
fromOneToFive (OneToFive n) = n
```

이 선언들을 별도 모듈에 넣고 `OneToFive` 생성자를 내보내지 않기로 하면, 두 API는 완전히 맞바꿀 수 있어 보인다. 순진하게 보면 newtype 버전이 더 단순하면서도 똑같이 타입 안전한 것 같다. 하지만 어쩌면 놀랍게도, 실제로는 그렇지 않다.

왜 그런지 보기 위해 `OneToFive` 값을 인자로 소비하는 함수를 작성한다고 하자. 구성적 모델링에서는 다섯 생성자 각각에 패턴 매칭하기만 하면 되고, GHC는 이 정의를 완전한(exhaustive) 것으로 받아들인다.

```haskell
ordinal :: OneToFive -> Text
ordinal One   = "first"
ordinal Two   = "second"
ordinal Three = "third"
ordinal Four  = "fourth"
ordinal Five  = "fifth"
```

newtype 인코딩에서는 사정이 다르다. newtype은 불투명하므로 그 값을 들여다보는 유일한 방법은 다시 `Int`로 변환하는 것이다. 어쨌든 실체가 `Int`이니까. `Int`는 당연히 `1`부터 `5` 외에도 많은 값을 담을 수 있으므로, 완전성 검사기를 만족시키려면 오류 케이스를 추가할 수밖에 없다.

```haskell
ordinal :: OneToFive -> Text
ordinal n = case fromOneToFive n of
  1 -> "first"
  2 -> "second"
  3 -> "third"
  4 -> "fourth"
  5 -> "fifth"
  _ -> error "impossible: bad OneToFive value"
```

이 지극히 작위적인 예제에서는 별 문제가 아닌 것처럼 보일 수 있다. 그래도 두 접근이 제공하는 보장의 핵심 차이를 보여 준다.

- 구성적 데이터 타입은 불변식을 하류 소비자가 _접근할 수 있는_ 방식으로 담아낸다. 잘못된 값은 입 밖에 낼 수조차 없게 됐으므로, `ordinal` 함수는 그런 값을 처리할 걱정에서 해방된다.

- newtype 래퍼는 값을 _검증하는_ 스마트 생성자를 제공하지만, 그 검사의 불리언 결과는 제어 흐름에만 쓰일 뿐 함수의 결과에 보존되지 않는다. 따라서 하류 소비자는 제한된 정의역의 이점을 누릴 수 없다. 사실상 `Int`를 받는 것이다.

완전성 검사를 잃는 게 사소해 보일 수 있지만 절대 그렇지 않다. `error`를 사용하는 순간 타입 시스템에 구멍을 뚫어 버린 것이다. `OneToFive` 데이터 타입에 생성자를 하나 더 추가하면,[^1] 구성적 데이터 타입을 소비하는 `ordinal` 버전은 컴파일 타임에 즉시 불완전한 것으로 검출되지만, newtype 래퍼를 소비하는 버전은 계속 컴파일되면서도 런타임에 "불가능한" 케이스로 떨어져 실패한다.

이 모든 것은 구성적 모델링이 _내재적으로(intrinsically)_ 타입 안전하다는 사실의 귀결이다. 즉 안전성이 타입 선언 자체에 의해 강제된다. 잘못된 값은 정말로 표현 불가능하다. 다섯 생성자 중 어떤 것으로도 `6`을 표현할 방법이 아예 없다. newtype 선언은 그렇지 않다. `Int`와 내재적인 의미 차이가 전혀 없고, 그 의미는 `toOneToFive` 스마트 생성자를 통해 외재적으로(extrinsically) 명시된다. newtype이 의도하는 의미적 구별은 타입 시스템에 전혀 보이지 않는다. 프로그래머의 머릿속에만 존재한다.

### 비어 있지 않은 리스트 다시 보기

`OneToFive` 데이터 타입은 꽤 인위적이지만, 훨씬 실용적인 다른 데이터 타입에도 동일한 추론이 적용된다. 최근 글들에서 거듭 강조한 `NonEmpty` 데이터 타입을 보자.

```haskell
data NonEmpty a = a :| [a]
```

`NonEmpty`를 평범한 리스트에 대한 newtype으로 표현한 버전을 상상해 보면 도움이 된다. 원하는 비어 있지 않음(non-emptiness) 속성은 익숙한 스마트 생성자 전략으로 강제할 수 있다.

```haskell
newtype NonEmpty a = NonEmpty [a]

nonEmpty :: [a] -> Maybe (NonEmpty a)
nonEmpty [] = Nothing
nonEmpty xs = Just $ NonEmpty xs

instance Foldable NonEmpty where
  toList (NonEmpty xs) = xs
```

`OneToFive`에서와 마찬가지로, 이 정보를 타입 시스템에 보존하지 못한 대가를 금세 발견하게 된다. `NonEmpty`를 도입한 동기는 안전한 `head`를 작성하는 것이었는데, newtype 버전은 또 하나의 단언을 요구한다.

```haskell
head :: NonEmpty a -> a
head xs = case toList xs of
  x:_ -> x
  []  -> error "impossible: empty NonEmpty value"
```

그런 케이스가 실제로 일어날 것 같지 않으니 대수롭지 않아 보일 수 있다. 하지만 그 추론은 전적으로 `NonEmpty`를 정의하는 모듈의 정확성을 신뢰하는 데 달려 있고, 구성적 정의는 GHC 타입 검사기만 신뢰하면 된다. 우리는 대체로 타입 검사기가 올바르게 동작한다고 믿으므로, 후자가 훨씬 설득력 있는 증명이다.

## 토큰으로서의 newtype

newtype을 좋아하는 사람이라면 이 논증 전체가 좀 거슬릴 수 있다. newtype이 주석보다 별로 나을 게 없다고, 다만 타입 검사기에게도 의미가 있는 주석일 뿐이라고 말하는 것처럼 보일 수 있다. 다행히 상황이 그렇게까지 암울하지는 않다. newtype도 일종의 안전성을 _제공할 수 있다_. 다만 더 약한 종류다.

newtype의 주된 안전성 이점은 추상화 경계에서 나온다. newtype의 생성자를 내보내지 않으면 다른 모듈에게 불투명해진다. newtype을 정의하는 모듈, 즉 "홈 모듈"은 이를 활용해 클라이언트를 안전한 API로 제한함으로써 내부 불변식이 강제되는 _신뢰 경계(trust boundary)_ 를 만들 수 있다.

위의 `NonEmpty` 예제로 이게 어떻게 동작하는지 보자. `NonEmpty` 생성자를 내보내지 않고, 결코 실제로는 실패하지 않으리라 믿는 `head`와 `tail` 연산을 제공한다.

```haskell
module Data.List.NonEmpty.Newtype
  ( NonEmpty
  , cons
  , nonEmpty
  , head
  , tail
  ) where

newtype NonEmpty a = NonEmpty [a]

cons :: a -> [a] -> NonEmpty a
cons x xs = NonEmpty (x:xs)

nonEmpty :: [a] -> Maybe (NonEmpty a)
nonEmpty [] = Nothing
nonEmpty xs = Just $ NonEmpty xs

head :: NonEmpty a -> a
head (NonEmpty (x:_)) = x
head (NonEmpty [])    = error "impossible: empty NonEmpty value"

tail :: NonEmpty a -> [a]
tail (NonEmpty (_:xs)) = xs
tail (NonEmpty [])     = error "impossible: empty NonEmpty value"
```

`NonEmpty` 값을 만들거나 소비하는 유일한 방법이 `Data.List.NonEmpty.Newtype`이 내보낸 API의 함수들을 쓰는 것이므로, 위 구현에서 클라이언트가 비어 있지 않음 불변식을 위반하는 것은 불가능하다. 어떤 의미에서 불투명한 newtype의 값은 _토큰_ 과 같다. 구현 모듈이 생성자 함수를 통해 토큰을 발행하고, 그 토큰 자체에는 아무 내재적 가치가 없다. 토큰으로 뭔가 유용한 일을 하는 유일한 방법은 발행 모듈의 접근자 함수, 여기서는 `head`와 `tail`에 토큰을 "상환"해서 그 안에 담긴 값을 얻는 것이다.

이 접근은 구성적 데이터 타입보다 훨씬 약하다. 실수로 잘못된 `NonEmpty []` 값을 만들 수단을 제공해 버리는 게 이론적으로 가능하기 때문이다. 그래서 newtype식 타입 안전성은 그 자체로는 원하는 불변식이 성립한다는 _증명_ 이 되지 못한다. 다만 불변식 위반이 일어날 수 있는 "표면적"을 정의 모듈로 제한해 주므로, 퍼징이나 속성 기반 테스트 기법으로 모듈의 API를 철저히 테스트하면 불변식이 정말 성립한다는 합리적인 확신을 얻을 수 있다.[^2]

이 트레이드오프가 그렇게 나빠 보이지 않을 수 있고, 실제로 아주 좋은 거래인 경우가 많다! 구성적 데이터 모델링으로 불변식을 보장하는 일은 일반적으로 꽤 어려울 수 있어서 비현실적일 때가 많다. 하지만 불변식을 위반할 수 있는 메커니즘을 실수로 제공하지 않기 위해 필요한 주의를 극적으로 과소평가하기 쉽다. 예를 들어 프로그래머가 GHC의 편리한 타입클래스 파생을 활용해 `NonEmpty`에 `Generic` 인스턴스를 파생할 수 있다.

```haskell
{-# LANGUAGE DeriveGeneric #-}

import GHC.Generics (Generic)

newtype NonEmpty a = NonEmpty [a]
  deriving (Generic)
```

그런데 이 무해해 보이는 한 줄이 추상화 경계를 우회하는 손쉬운 메커니즘을 제공한다.

```
ghci> GHC.Generics.to @(NonEmpty ()) (M1 $ M1 $ M1 $ K1 [])
NonEmpty []
```

파생된 `Generic` 인스턴스는 근본적으로 추상화를 깨뜨리므로 이건 특히 극단적인 예지만, 이 문제는 덜 뻔한 방식으로도 튀어나올 수 있다. 파생된 `Read` 인스턴스에서도 같은 문제가 생긴다.

```
ghci> read @(NonEmpty ()) "NonEmpty []"
NonEmpty []
```

어떤 독자에게는 이런 함정이 뻔해 보일지 모르지만, 실무에서 이런 종류의 안전성 구멍은 놀랄 만큼 흔하다. 더 정교한 불변식을 가진 데이터 타입이라면 특히 그렇다. 모듈 구현이 불변식을 실제로 지키는지 판별하기가 쉽지 않을 수 있기 때문이다. 이 기법을 제대로 쓰려면 신중함과 주의가 필요하다.

- 모든 불변식을 신뢰 모듈의 유지보수자에게 분명히 알려야 한다. `NonEmpty` 같은 단순한 타입은 불변식이 자명하지만, 더 정교한 타입에서 주석은 선택이 아니다.

- 신뢰 모듈의 모든 변경은 원하는 불변식을 어떤 식으로든 약화하지 않는지 신중히 감사해야 한다.

- 잘못 쓰면 불변식을 훼손할 수 있는 안전하지 않은 뒷문을 추가하고 싶은 유혹에 저항하는 규율이 필요하다.

- 신뢰 표면적이 작게 유지되도록 주기적인 리팩터링이 필요할 수 있다. 신뢰 모듈의 책임은 시간이 지나며 쌓이기 아주 쉽고, 그러면 어떤 미묘한 상호작용이 불변식 위반을 일으킬 가능성이 극적으로 커진다.

반면 구성적으로 올바른(correct by construction) 데이터 타입은 이런 문제를 하나도 겪지 않는다. 데이터 타입 정의 자체를 바꾸지 않고는 불변식을 위반할 수 없고, 정의를 바꾸면 프로그램 전체에 파급 효과가 일어나 그 결과가 즉시 드러난다. 타입 검사기가 불변식을 자동으로 강제하므로 프로그래머의 규율은 불필요하다. 이런 데이터 타입에는 "신뢰 코드"가 없다. 프로그램의 모든 부분이 데이터 타입이 부과하는 제약에 똑같이 묶여 있기 때문이다.

라이브러리에서는 newtype이 주는 캡슐화를 통한 안전성 개념이 유용하다. 라이브러리는 더 복잡한 데이터 구조를 만드는 데 쓰이는 빌딩 블록을 제공하는 경우가 많기 때문이다. 그런 라이브러리는 대체로 애플리케이션 코드보다 더 많은 정밀 검토와 주의를 받으며, 변경 빈도도 훨씬 낮다. 애플리케이션 코드에서도 이 기법은 여전히 유용하지만, 프로덕션 코드베이스의 잦은 변경(churn)은 시간이 지나며 캡슐화 경계를 약화하는 경향이 있으므로, 실용적인 한 구성에 의한 정확성을 우선해야 한다.

## 그 밖의 newtype 사용, 남용, 오용

앞 절은 newtype이 유용한 주된 방식을 다뤘다. 하지만 실무에서 newtype은 위 패턴에 맞지 않는 방식으로도 흔히 쓰인다. 그중 일부는 합리적이다.

- Haskell의 타입클래스 일관성(coherency) 개념은 각 타입이 어떤 클래스든 인스턴스를 하나만 갖도록 제한한다. 유용한 인스턴스가 둘 이상 가능한 타입에는 newtype이 전통적인 해법이고, 이는 좋은 효과를 낼 수 있다. 예를 들어 `Data.Monoid`의 `Sum`과 `Product` newtype은 수치 타입에 유용한 `Monoid` 인스턴스를 제공한다.

- 비슷한 맥락에서, newtype은 타입 매개변수를 도입하거나 재배열하는 데 유용할 수 있다. `Data.Bifunctor.Flip`의 `Flip` newtype이 단순한 예다. `Bifunctor`의 인자를 뒤집어 `Functor` 인스턴스가 반대쪽에서 동작하게 한다.

  ```haskell
  newtype Flip p a b = Flip { runFlip :: p b a }
  ```

  Haskell은 (아직) 타입 수준 람다를 지원하지 않기 때문에 이런 저글링에는 newtype이 필요하다.

- 더 단순하게는, 값이 프로그램의 서로 먼 부분 사이를 오가야 하고 중간 코드가 그 값을 들여다볼 이유가 없을 때, 투명한 newtype이 오용을 억제하는 데 유용할 수 있다. 예를 들어 비밀 키를 담은 `ByteString`을 (`Show` 인스턴스를 뺀) newtype으로 감싸면 코드가 실수로 로그에 남기거나 노출하는 일을 억제할 수 있다.

이 모든 활용은 좋은 것이지만 _타입 안전성_ 과는 거의 무관하다. 특히 마지막 항목은 안전성으로 자주 혼동되는데, 공정하게 말하면 실제로 타입 시스템을 활용해 논리적 실수를 피하도록 돕기는 한다. 하지만 그런 사용이 오용을 실제로 _막는다_ 고 주장하면 왜곡이다. 프로그램의 어느 부분이든 언제든 그 값을 들여다볼 수 있으니까.

너무 자주, 이 안전성의 환상은 노골적인 newtype 남용으로 이어진다. 예를 들어 내가 생업으로 다루는 바로 그 코드베이스에 이런 정의가 있다.

```haskell
newtype ArgumentName = ArgumentName { unArgumentName :: GraphQL.Name }
  deriving ( Show, Eq, FromJSON, ToJSON, FromJSONKey, ToJSONKey
           , Hashable, ToTxt, Lift, Generic, NFData, Cacheable )
```

이 newtype은 쓸모없는 소음이다. 기능적으로 그 바탕 타입인 `Name`과 완전히 맞바꿀 수 있다. 오죽하면 타입클래스를 열두 개나 파생한다! 쓰이는 모든 자리에서, 이 값은 자신을 감싼 레코드에서 꺼내지는 순간 즉시 벗겨지므로 타입 안전성의 이점이 전혀 없다. 심지어 `ArgumentName`이라고 이름 붙여서 더해지는 명료함도 없다. 이미 그 값이 담긴 필드 이름이 역할을 분명히 하고 있으니까.

이런 newtype은 타입 시스템을 외부 세계의 분류 체계(taxonomy)로 쓰려는 욕망에서 생겨나는 것 같다. "인자 이름"은 일반적인 "이름"보다 더 구체적인 개념이니 당연히 자기 타입이 있어야 한다는 것이다. 직관적으로는 그럴듯하지만 상당히 잘못된 방향이다. 분류 체계는 관심 도메인을 문서화하는 데는 유용하지만, 도메인을 모델링하는 데 꼭 도움이 되는 건 아니다. 프로그래밍할 때 우리는 타입을 다른 목적으로 쓴다.

- 일차적으로, 타입은 값들 사이의 _기능적_ 차이를 구별한다. `NonEmpty a` 타입의 값은 `[a]` 타입의 값과 _기능적으로_ 구별된다. 근본적으로 구조가 다르고 추가 연산을 허용하기 때문이다. 이런 의미에서 타입은 _구조적_ 이다. 프로그래밍 언어의 내부 세계에서 값이 _무엇인지_ 를 기술한다.

- 이차적으로, 우리는 때때로 논리적 실수를 피하는 데 타입을 쓴다. 표현상으로는 둘 다 실수(real number)일지라도, 실수로 더해 버리는 것 같은 무의미한 짓을 피하려고 `Distance`와 `Duration`을 별개 타입으로 둘 수 있다.

두 용도 모두 _실용적_ 이라는 점에 주목하자. 타입 시스템을 도구로 본다. 정적 타입 시스템은 문자 그대로의 의미에서 도구 _이므로_ 이는 꽤 자연스러운 관점이다. 그런데도 이 관점은 의외로 드문 듯하다. 세계를 분류하는 용도로 타입을 쓰는 관행이 `ArgumentName` 같은 도움 안 되는 소음을 일상적으로 만들어 내는데도 말이다.

newtype이 완전히 투명하고 아무 때나 감쌌다 벗겼다 한다면, 그다지 도움이 되지 않을 가능성이 높다. 이 사례라면 나는 구별을 아예 없애고 `Name`을 쓰겠지만, 다른 라벨이 진짜 명료함을 더하는 상황이라면 언제든 타입 별칭을 쓸 수 있다.[^3]

```haskell
type ArgumentName = GraphQL.Name
```

이런 newtype은 안심 담요(security blanket)다. 프로그래머에게 몇 개의 관문을 통과하게 강제하는 것은 타입 안전성이 아니다. 그들은 아무 생각 없이 기꺼이 그 관문을 뛰어넘을 것이라고 장담한다.

## 마무리 생각과 읽을거리

이 글은 오래전부터 쓰고 싶었다. 표면적으로는 Haskell newtype에 대한 매우 구체적인 비판이고, 내가 생업으로 Haskell을 쓰면서 실제로 이 문제를 이런 모습으로 마주치기 때문에 이렇게 틀을 잡았다. 하지만 사실 핵심 아이디어는 그보다 훨씬 크다.

newtype은 _래퍼 타입_ 을 정의하는 하나의 특정 메커니즘일 뿐이고, 래퍼 타입이라는 개념은 거의 모든 언어에, 심지어 동적 타입 언어에도 존재한다. Haskell을 쓰지 않더라도 이 글의 추론 대부분은 당신이 쓰는 언어에서도 유효할 가능성이 높다. 더 넓게 보면, 이 글은 내가 지난 1년간 여러 각도에서 전하려 해 온 주제의 연장이다. 타입 시스템은 도구이며, 그것이 실제로 무엇을 하는지 그리고 어떻게 효과적으로 쓸지에 대해 더 의식적이고 의도적이어야 한다는 것이다.

마침내 자리에 앉아 이 글을 쓰게 만든 촉매는 최근 발표된 [Tagged is not a Newtype](https://tech.freckle.com/2020/10/26/tagged-is-not-a-newtype/)이다. 좋은 글이고 전반적인 취지에는 전적으로 동의하지만, 더 큰 논점을 짚을 기회를 놓쳤다고 생각했다. 실제로 `Tagged`는 정의상 newtype _이 맞으므로_, 그 글의 제목은 다소 초점을 흐린다. 진짜 문제는 조금 더 깊은 곳에 있다.

newtype은 신중하게 적용하면 유용하지만, 그 안전성은 내재적이지 않다. 교통 콘의 안전성이 그것을 만든 플라스틱 안에 들어 있지 않은 것과 마찬가지다. 중요한 것은 올바른 맥락에 놓이는 것이다. 그게 없으면 newtype은 그저 라벨링 체계, 무언가에 이름을 붙이는 방법일 뿐이다.

그리고 이름은 타입 안전성이 아니다.

[^1]: 이름을 생각하면 꽤 있을 법하지 않은 일이지만, 작위적인 예제이니 넘어가 주기 바란다.

[^2]: 이론적으로는 손으로 쓴 증명이나, 증명 보조기(proof assistant)/정리 증명기와 결합한 프로그램 추출 같은 외부 검증 기법으로 불변식이 성립함을 철저히 증명하는 것도 여전히 가능하다. 하지만 이런 기법은 일반적인 프로그래밍 실무에서 극히 드물다.

[^3]: 사실 나는 타입 별칭도 득보다 실이 많은 경우가 흔하다고 생각해서 남용은 역시 경계하라고 말하고 싶지만, 그건 이 글의 범위를 벗어난다.
