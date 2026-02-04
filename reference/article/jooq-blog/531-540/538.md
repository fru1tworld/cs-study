# 깊은 스택 트레이스는 좋은 코드 품질의 신호일 수 있다

> 원문: https://blog.jooq.org/deep-stack-traces-can-be-a-sign-for-good-code-quality/

"새는 추상화(leaky abstractions)"라는 용어는 꽤 오래전부터 존재해왔습니다. 이 용어의 창시자는 대부분 Joel Spolsky로 알려져 있으며, 그는 이에 대해 자주 인용되는 글을 작성했습니다. 저는 최근 스택 트레이스의 깊이로 측정되는 새는 추상화에 대한 또 다른 해석을 발견했습니다:

[이미지 참조: Geek and Poke가 이해한 새는 추상화 (CC-BY 라이선스)]

그래서, Geek & Poke에 따르면 긴 스택 트레이스는 나쁜 것입니다. 저는 이 주장을 Igor Polevoy의 블로그에서 본 적이 있습니다(그는 인기 있는 Ruby ActiveRecord 쿼리 인터페이스의 Java 구현체인 ActiveJDBC의 창시자입니다). Joel Spolsky의 논증이 ORM을 비판하는 데 자주 사용되었던 것처럼, Igor의 주장도 ActiveJDBC를 Hibernate와 비교하는 데 사용되었습니다. 인용하자면:

"누군가는 '그래서 뭐, 의존성의 크기나 스택 트레이스의 깊이 같은 것을 왜 신경 써야 하지?'라고 말할 수 있습니다. 저는 좋은 개발자라면 이러한 것들을 신경 써야 한다고 생각합니다. 프레임워크가 두꺼울수록, 더 복잡하고, 더 많은 메모리를 할당하며, 더 많은 것들이 잘못될 수 있습니다."

저는 일정 수준의 복잡성을 가진 프레임워크가 더 긴 스택 트레이스를 가지는 경향이 있다는 것에 전적으로 동의합니다. 그래서 이 공리들을 우리의 정신적 Prolog 프로세서에 통과시키면:

- Hibernate가 새는 추상화라면, 그리고
- Hibernate가 복잡하다면, 그리고
- 복잡성이 긴 스택 트레이스로 이어진다면,
- 새는 추상화와 긴 스택 트레이스는 상관관계가 있다

저는 공식적인 인과 관계가 있다고 주장할 정도까지는 가지 않겠습니다. 하지만 상관관계는 논리적으로 보입니다.

하지만 이러한 것들이 반드시 나쁜 것은 아닙니다. 사실, 긴 스택 트레이스는 소프트웨어 품질 측면에서 좋은 신호일 수 있습니다. 이는 소프트웨어 내부가 높은 응집도(cohesion)와 높은 DRY(Don't Repeat Yourself) 수준을 보여준다는 것을 의미할 수 있으며, 이는 다시 프레임워크 깊은 곳에 미묘한 버그가 있을 위험이 적다는 것을 의미합니다. 높은 응집도와 높은 DRY 수준은 코드의 많은 부분이 전체 프레임워크 내에서 매우 관련성이 높다는 것을 의미하며, 이는 다시 어떤 저수준 버그든 _모든 것_이 잘못되게 만들면서 전체 프레임워크를 폭발시킬 것임을 의미합니다. 테스트 주도 개발을 한다면, 여러분의 사소한 실수가 테스트 케이스의 90%를 실패하게 만드는 것을 즉시 알아차리는 보상을 받게 될 것입니다.

## 실제 예시

Hibernate와 ActiveJDBC를 이미 비교하고 있으니, jOOQ를 예시로 사용하여 이를 설명해 보겠습니다. 데이터베이스 접근 추상화에서 가장 긴 스택 트레이스 중 일부는 해당 추상화와 JDBC의 인터페이스에 브레이크포인트를 걸어서 얻을 수 있습니다. 예를 들어, JDBC ResultSet에서 데이터를 가져올 때입니다.

```
Utils.getFromResultSet(ExecuteContext, Class<T>, int) line: 1945
Utils.getFromResultSet(ExecuteContext, Field<U>, int) line: 1938
CursorImpl$CursorIterator$CursorRecordInitialiser.setValue(AbstractRecord, Field<T>, int) line: 1464
CursorImpl$CursorIterator$CursorRecordInitialiser.operate(AbstractRecord) line: 1447
CursorImpl$CursorIterator$CursorRecordInitialiser.operate(Record) line: 1
RecordDelegate<R>.operate(RecordOperation<R,E>) line: 119
CursorImpl$CursorIterator.fetchOne() line: 1413
CursorImpl$CursorIterator.next() line: 1389
CursorImpl$CursorIterator.next() line: 1
CursorImpl<R>.fetch(int) line: 202
CursorImpl<R>.fetch() line: 176
SelectQueryImpl<R>(AbstractResultQuery<R>).execute(ExecuteContext, ExecuteListener) line: 274
SelectQueryImpl<R>(AbstractQuery).execute() line: 322
T_2698Record(UpdatableRecordImpl<R>).refresh(Field<?>...) line: 438
T_2698Record(UpdatableRecordImpl<R>).refresh() line: 428
H2Test.testH2T2698InsertRecordWithDefault() line: 931
```

ActiveJDBC의 스택 트레이스와 비교하면 꽤 많지만, Hibernate(많은 리플렉션과 인스트루멘테이션을 사용하는)에 비하면 적습니다. 그리고 여기에는 상당히 암호 같은 내부 클래스들과 꽤 많은 메소드 오버로딩이 포함되어 있습니다. 이것을 어떻게 해석할까요? 바닥에서 위로(또는 스택 트레이스에서는 위에서 아래로) 살펴보겠습니다.

### CursorRecordInitialiser

CursorRecordInitialiser는 Cursor에 의한 Record의 초기화를 캡슐화하는 내부 클래스이며, ExecuteListener SPI의 관련 부분이 한 곳에서 처리되도록 보장합니다. 이것은 JDBC의 다양한 `ResultSet` 메소드로 가는 게이트웨이입니다. 이것은 다음에 의해 호출되는 일반적인 내부 `RecordOperation` 구현입니다...

### RecordDelegate

... `RecordDelegate`. 클래스 이름은 꽤 의미 없지만, 그 목적은 RecordListener SPI의 중앙 집중식 구현이 달성될 수 있도록 모든 직접적인 레코드 작업을 보호하고 래핑하는 것입니다. 이 SPI는 클라이언트 코드에서 액티브 레코드 라이프사이클 이벤트를 리스닝하기 위해 구현될 수 있습니다. 이 SPI의 구현을 DRY하게 유지하는 대가는 스택 트레이스에 몇 개의 요소가 추가되는 것인데, 이러한 콜백이 Java 언어에서 클로저를 구현하는 표준 방법이기 때문입니다. 하지만 이 로직을 DRY하게 유지하면 Record가 어떻게 초기화되든 상관없이 SPI가 항상 호출된다는 것을 보장합니다. 잊혀진 코너 케이스가 (거의) 없습니다.

하지만 우리는 다음에서 Record를 초기화하고 있었습니다...

### CursorImpl

... CursorImpl, Cursor의 구현체. jOOQ Cursor는 "지연 페칭(lazy fetching)"에 사용되기 때문에, 즉 JDBC에서 Record를 하나씩 가져오는 데 사용되기 때문에 이상해 보일 수 있습니다.

반면에, 이 스택 트레이스의 `SELECT` 쿼리는 단순히 단일 UpdatableRecord(jOOQ의 액티브 레코드에 해당하는 것)를 리프레시합니다. 하지만 여전히, 마치 우리가 크고 복잡한 데이터 세트를 가져오는 것처럼 모든 지연 페칭 로직이 실행됩니다. 이것 역시 데이터를 가져올 때 DRY를 유지하기 위함입니다. 물론, 단일 레코드만 있을 수 있다는 것을 알기 때문에 단순히 단일 레코드를 읽음으로써 약 6단계의 스택 트레이스를 절약할 수 있었을 것입니다. 하지만 다시 말하지만, 커서의 어떤 미묘한 버그든 레코드 리프레시를 위한 테스트 케이스처럼 먼 것에서도 _어떤_ 테스트 케이스에서든 나타날 가능성이 높습니다.

어떤 사람들은 이 모든 것이 메모리와 CPU 사이클을 낭비한다고 주장할 수 있습니다. 반대가 사실일 가능성이 더 높습니다. 현대 JVM 구현은 수명이 짧은 객체와 메소드 호출을 관리하고 가비지 컬렉션하는 데 너무 뛰어나서, 약간의 추가적인 복잡성은 런타임 환경에 거의 추가 작업을 부과하지 않습니다.

## TL;DR: 긴 스택 트레이스는 높은 응집도와 DRY를 나타낼 _수 있다_

긴 스택 트레이스가 나쁜 것이라는 주장은 반드시 맞지 않습니다. 긴 스택 트레이스는 복잡한 프레임워크가 잘 구현되었을 때 발생하는 것입니다. 복잡성은 필연적으로 _"새는 추상화"_로 이어질 것입니다. 하지만 잘 설계된 복잡성만이 긴 스택 트레이스로 이어질 것입니다.

반대로, 짧은 스택 트레이스는 두 가지를 의미할 수 있습니다:

- 복잡성의 부재: 프레임워크가 기능이 적고 단순합니다. 이것은 ActiveJDBC를 "단순한 프레임워크"로 광고하고 있기 때문에 Igor의 주장과 일치합니다.
- 응집도와 DRY의 부재: 프레임워크가 형편없이 작성되었고, 아마도 테스트 커버리지가 낮고 버그가 많습니다.

## 트리 데이터 구조

마지막으로, 긴 스택 트레이스가 불가피한 또 다른 경우는 트리 구조 / 복합체 패턴 구조가 방문자(visitor)를 사용하여 순회될 때라는 것을 언급할 가치가 있습니다. XPath나 XSLT를 디버깅해 본 사람이라면 이러한 트레이스가 얼마나 깊은지 알 것입니다.
