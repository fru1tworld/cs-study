# 컬렉션의 동등성과 성능 특성

> **원문:** https://docs.scala-lang.org/overviews/collections-2.13/equality.html
> **원문:** https://docs.scala-lang.org/overviews/collections-2.13/performance-characteristics.html

---

## 목차

1. [개요](#1-개요)
2. [컬렉션 동등성(`==`)의 규칙](#2-컬렉션-동등성-의-규칙)
   - 2.1 [규칙 1 — 카테고리가 다르면 무조건 다르다](#21-규칙-1--카테고리가-다르면-무조건-다르다)
   - 2.2 [규칙 2 — 같은 카테고리면 구현이 아니라 내용물로 비교한다](#22-규칙-2--같은-카테고리면-구현이-아니라-내용물로-비교한다)
   - 2.3 [가변 컬렉션과 동등성 — 해시 키로 쓸 때의 함정](#23-가변-컬렉션과-동등성--해시-키로-쓸-때의-함정)
3. [성능 특성 표기법](#3-성능-특성-표기법)
4. [순차 컬렉션(Sequence)의 성능](#4-순차-컬렉션sequence의-성능)
   - 4.1 [불변 시퀀스](#41-불변-시퀀스)
   - 4.2 [가변 시퀀스](#42-가변-시퀀스)
5. [집합/맵(Set/Map)의 성능](#5-집합맵setmap의-성능)
6. [실전 선택 가이드](#6-실전-선택-가이드)

---

## 1. 개요

Scala 컬렉션 라이브러리는 두 가지를 문서 두 페이지로 나누어 설명합니다.

- **동등성(equality)** — `==`로 두 컬렉션을 비교했을 때 언제 같다고 판단되는가.
- **성능 특성(performance characteristics)** — `head`, `apply`, `add` 같은 연산이 각 컬렉션 타입에서 상수 시간인지 선형 시간인지.

둘 다 "컬렉션 타입을 무엇으로 고를지"에 직접 영향을 주는 실전 지식입니다. 동등성 규칙을 모르면 해시맵 키로 가변 컬렉션을 썼다가 원인 모를 버그를 만나고, 성능 특성을 모르면 `List`에 인덱스로 접근하는 코드를 반복문 안에 넣어 놓고 왜 느린지 못 찾게 됩니다.

---

## 2. 컬렉션 동등성(`==`)의 규칙

> 💡 **왜 필요한가**
>
> Java에서는 컬렉션 종류(`ArrayList` vs `LinkedList`)가 다르면 `equals`가 대체로 내용까지 비교해 주지만, 인터페이스가 다르면(`List` vs `Set`) 아예 비교 자체가 어색합니다. Scala는 "컬렉션이 속한 큰 분류(세트/맵/시퀀스)만 같으면, 구체적인 구현 타입은 신경 쓰지 말고 내용물로 비교한다"는 일관된 규칙을 세워 이 혼란을 없앴습니다.

### 2.1 규칙 1 — 카테고리가 다르면 무조건 다르다

Scala 컬렉션은 크게 세 카테고리로 나뉩니다.

- **Seq** (순서가 있는 시퀀스: `List`, `Vector`, `ArraySeq` ...)
- **Set** (집합: `HashSet`, `TreeSet` ...)
- **Map** (맵: `HashMap`, `TreeMap` ...)

**카테고리가 다르면 원소가 똑같아도 항상** `false`입니다.

```scala
Set(1, 2, 3) == List(1, 2, 3)   // false — Set과 Seq는 카테고리가 다름
List(1, 2, 3) == Seq(1, 2, 3)   // true  — 둘 다 Seq 카테고리
```

### 2.2 규칙 2 — 같은 카테고리면 구현이 아니라 내용물로 비교한다

같은 카테고리 안에서는 **구체적인 클래스가 달라도 원소만 같으면 동등**합니다. Seq는 순서까지 같아야 하고, Set/Map은 순서와 무관하게 원소 집합만 같으면 됩니다.

```scala
List(1, 2, 3) == Vector(1, 2, 3)       // true  — 둘 다 Seq, 순서도 같음
List(1, 2, 3) == List(3, 2, 1)         // false — Seq는 순서까지 비교
HashSet(1, 2) == TreeSet(2, 1)         // true  — Set은 순서 무관
```

> ⚠️ **짚고 넘어가기**
>
> "구현이 다르면 안 같다"고 생각하기 쉽지만 정반대입니다. `List`와 `Vector`처럼 내부 구조가 완전히 다른 두 타입도 카테고리(Seq)와 내용(원소, 순서)만 맞으면 서로 `==`로 비교했을 때 `true`가 나옵니다. 배열(`Array`)만은 예외로, Scala 2.13 기준 `Array`의 `==`는 참조 동등성(reference equality)을 그대로 물려받으므로 원소가 같아도 다른 배열 인스턴스면 `false`입니다. 배열 내용 비교에는 `sameElements`를 쓰거나 `.toSeq`로 감싸 비교해야 합니다.

### 2.3 가변 컬렉션과 동등성 — 해시 키로 쓸 때의 함정

가변(mutable) 컬렉션은 **"지금 이 순간 담고 있는 원소"** 기준으로 동등성이 결정됩니다. 즉 컬렉션 내용이 바뀌면 `==` 결과와 해시코드(`hashCode`)도 함께 바뀝니다. 이 때문에 가변 컬렉션을 `HashMap`의 키로 쓰면 다음과 같은 사고가 납니다.

```scala
import scala.collection.mutable.{ArrayBuffer, HashMap}

val buf = ArrayBuffer(1, 2, 3)
val map = HashMap(buf -> "value")

map(buf)          // "value" — 아직은 정상 조회

buf(0) += 1       // buf의 내용이 바뀜 → hashCode도 바뀜
map(buf)          // NoSuchElementException! 버킷 위치가 어긋나 못 찾음
```

`HashMap`은 키를 넣을 때의 `hashCode`로 버킷 위치를 정해 두는데, 이후 그 키(가변 컬렉션)의 내용이 바뀌면 `hashCode`도 달라져 **원래 저장했던 버킷과 지금 계산되는 버킷이 어긋납니다.** 데이터는 그대로 맵 안에 있지만 찾을 수 없게 되는 것입니다.

> **실전 원칙:** 해시 기반 컬렉션(`HashMap`, `HashSet`)의 키·원소로는 **불변 컬렉션**만 쓰세요. 가변 컬렉션을 키로 써야 한다면 넣은 뒤 다시는 그 내용을 바꾸지 않는다는 확신이 있을 때만 허용합니다.

---

## 3. 성능 특성 표기법

공식 문서는 각 연산의 시간 복잡도를 다음 다섯 기호로 요약합니다.

| 기호 | 의미 | 설명 |
|---|---|---|
| **C** | 상수 시간(Constant) | 컬렉션 크기와 무관하게 항상 빠름 |
| **eC** | 사실상 상수 시간(effectively Constant) | 이론적으로는 조건이 있지만(예: 벡터 길이가 현실적 한계 안일 때) 실질적으로 상수로 취급 가능 |
| **aC** | 상환 상수 시간(amortized Constant) | 개별 호출은 느릴 수 있어도, 여러 번 호출한 평균은 상수 시간 (예: 배열이 꽉 차면 가끔 통째로 재할당하지만 자주 있는 일은 아님) |
| **Log** | 로그 시간 | 컬렉션 크기의 로그에 비례 (균형 트리 등) |
| **L** | 선형 시간(Linear) | 컬렉션 크기에 비례 — 커질수록 느려짐 |
| **–** | 미지원 | 해당 연산 자체를 제공하지 않음 |

> 📘 **처음 배우는 분께**
>
> "eC"와 "aC"를 헷갈리기 쉽습니다. **eC**(사실상 상수)는 "이론적 최악의 경우엔 로그 시간이지만 실제 크기 범위에서는 상수와 다름없다"는 뜻(`Vector`가 대표적)이고, **aC**(상환 상수)는 "가끔 한 번 비싼 연산(배열 재할당 등)이 끼어도, 여러 번 호출을 평균 내면 상수"라는 뜻(`ArrayBuffer.append`가 대표적)입니다. 전자는 "구조가 원래 빠르다", 후자는 "가끔 비싸지만 평균은 싸다"는 차이입니다.

---

## 4. 순차 컬렉션(Sequence)의 성능

시퀀스는 `head`(첫 원소), `tail`(첫 원소 제외 나머지), `apply`(인덱스 접근), `update`(특정 위치 변경), `prepend`(앞에 추가), `append`(뒤에 추가) 여섯 연산 기준으로 비교합니다.

### 4.1 불변 시퀀스

| 타입 | head | tail | apply | update | prepend | append |
|---|---|---|---|---|---|---|
| `List` | C | C | L | L | C | L |
| `LazyList` | C | C | L | L | C | L |
| `Vector` | eC | eC | eC | eC | eC | eC |
| `ArraySeq` | C | L | C | L | L | L |
| `Range` | C | C | C | – | – | – |
| `Queue` | aC | aC | L | L | C | C |
| `String` | C | L | C | L | L | L |

- **`List`**: "머리와 꼬리로 이루어진 연결 리스트"이므로 앞쪽 연산(`head`, `tail`, `prepend`)은 전부 상수 시간이지만, 인덱스로 임의 위치에 접근(`apply`)하거나 끝에 추가(`append`)하려면 리스트를 처음부터 끝까지 훑어야 해 선형 시간입니다.
- **`Vector`**: 내부적으로 갈래가 32개인 트리 구조라서 이론적으로는 `Log`지만, 트리 깊이가 실전 데이터 크기에서 사실상 상수(트리 깊이 ≤ 6~7 정도)라 모든 주요 연산이 `eC`로 균형 잡혀 있습니다. "이것도 저것도 적당히 빠른" 범용 시퀀스가 필요할 때의 기본 선택지입니다.
- **`ArraySeq`(불변)**: 배열 하나를 그대로 감싸므로 인덱스 접근(`apply`)은 `C`지만, 불변이라 원소 하나를 바꾸려면(`update`) 배열 전체를 복사해야 해 `L`입니다.
- **`Range`**: `start`, `end`, `step` 세 숫자만 들고 있는 컬렉션이라 `head`/`apply`가 계산만으로 끝나 `C`입니다. 대신 원소를 하나씩 끼워 넣거나 값을 바꾼다는 개념 자체가 없어 `update`/`prepend`/`append`는 아예 지원하지 않습니다(`–`).
- **`Queue`**: 앞쪽 리스트와 뒤집힌 뒤쪽 리스트 두 개로 구현되어 `prepend`/`append` 모두 어느 한쪽 리스트에 얹기만 하면 되어 `C`지만, `head`/`tail`은 뒤쪽 리스트를 뒤집어야 할 때가 있어 `aC`(상환 상수)입니다.

```scala
// List: 앞에서부터 처리하는 재귀 알고리즘에 적합
def sumList(xs: List[Int]): Int =
  if xs.isEmpty then 0 else xs.head + sumList(xs.tail)   // head/tail 모두 C

// Vector: 인덱스 접근이 잦은 코드에 적합
val v = Vector(1, 2, 3, 4, 5)
v(2)          // eC — List였다면 L
v.updated(2, 99)   // eC — List.updated는 L
```

### 4.2 가변 시퀀스

| 타입 | head | tail | apply | update | prepend | append |
|---|---|---|---|---|---|---|
| `ArrayBuffer` | C | L | C | C | L | aC |
| `ListBuffer` | C | L | L | L | C | C |
| `Array` | C | L | C | C | – | – |
| `StringBuilder` | C | L | C | C | L | aC |

- **`ArrayBuffer`**: 내부가 배열이라 인덱스 접근·변경(`apply`, `update`)이 `C`이고, 뒤에 추가(`append`)는 배열이 꽉 찼을 때만 가끔 재할당하므로 `aC`입니다. 반면 앞에 추가(`prepend`)는 기존 원소를 전부 한 칸씩 밀어야 해 `L`입니다. **뒤에서만 추가/삭제하는 스택·버퍼 용도**에 최적입니다.
- **`ListBuffer`**: `List`를 뒤에서부터 빠르게 만들기 위한 가변 버퍼로, 양 끝(`prepend`, `append`)이 모두 `C`입니다. 대신 인덱스 접근(`apply`)은 여전히 리스트 순회가 필요해 `L`입니다.

```scala
// ArrayBuffer: 뒤쪽 추가 + 인덱스 접근이 잦을 때
val buf = scala.collection.mutable.ArrayBuffer.empty[Int]
buf += 1; buf += 2; buf += 3   // append aC
buf(1)                          // apply C
```

---

## 5. 집합/맵(Set/Map)의 성능

Set/Map은 `lookup`(조회), `add`(추가), `remove`(삭제), `min`(최솟값) 네 연산으로 비교합니다.

| 타입 | lookup | add | remove | min |
|---|---|---|---|---|
| 불변 `HashSet` / `HashMap` | eC | eC | eC | L |
| 불변 `TreeSet` / `TreeMap` | Log | Log | Log | Log |
| 불변 `BitSet` | C | L | L | eC |
| 불변 `VectorMap` | eC | eC | aC | L |
| 불변 `ListMap` | L | L | L | L |
| 가변 `HashSet` / `HashMap` | eC | eC | eC | L |
| 가변 `TreeSet` | Log | Log | Log | Log |
| 가변 `BitSet` | C | aC | C | eC |

- **해시 기반(`HashSet`/`HashMap`)**: 해시코드로 버킷을 바로 찾아가므로 조회·추가·삭제가 (가변·불변 모두) 전부 사실상 상수 시간(`eC`)입니다. 단, 최솟값(`min`)은 정렬 정보를 따로 유지하지 않으므로 전체를 훑어야 해 `L`입니다. **순서가 중요하지 않은 대다수 상황의 기본 선택지**입니다.
- **트리 기반(`TreeSet`/`TreeMap`)**: 균형 이진 트리(레드-블랙 트리)로 구현되어 모든 연산이 `Log`로 고르지만, 그 대가로 **원소가 항상 정렬된 순서를 유지**합니다. 정렬된 순회나 범위 조회(`range`, `from`, `until`)가 필요할 때 선택합니다.
- **`BitSet`**: 정수 집합을 비트 배열로 표현하므로, 값의 범위가 촘촘하게 몰려 있을 때(dense) 조회(`C`)와 최솟값 조회(`eC`)가 빠릅니다. 추가(`add`)는 불변은 새 비트 배열을 만들어야 해 `L`, 가변은 배열을 가끔만 늘리면 되어 `aC`입니다. 희소(sparse)하고 값의 범위가 아주 넓으면 이 장점이 사라지므로 주의가 필요합니다.
- **`VectorMap`/`ListMap`**: `VectorMap`은 삽입 순서를 기억하면서도 조회·추가는 해시 기반이라 `eC`를 유지합니다. `ListMap`은 내부가 연결 리스트라 대부분의 연산이 `L`이므로, 삽입 순서 보존이 꼭 필요한 게 아니라면 `VectorMap`이 더 나은 선택입니다.

```scala
import scala.collection.immutable.{HashSet, TreeSet}

val hs = HashSet(3, 1, 2)
hs.contains(2)     // eC — 순서 없는 빠른 조회

val ts = TreeSet(3, 1, 2)
ts.min             // Log — 정렬 구조 덕분에 순서 관련 연산이 안정적
ts.range(1, 3)     // 정렬 순회가 필요하면 TreeSet
```

---

## 6. 실전 선택 가이드

| 상황 | 추천 컬렉션 | 이유 |
|---|---|---|
| 앞에서부터 재귀적으로 처리(패턴 매칭, `head`/`tail`) | `List` | 앞쪽 연산이 전부 `C` |
| 인덱스 접근·갱신이 잦은 불변 컬렉션 | `Vector` | 모든 연산이 균형 있게 `eC` |
| 뒤에서만 추가/삭제하는 누적 버퍼(가변) | `ArrayBuffer` | append `aC`, 인덱스 `C` |
| 해시맵/해시셋 키로 쓸 값 | **불변** 컬렉션 | 가변 컬렉션은 내용 변경 시 해시코드가 바뀌어 조회 실패 위험 |
| 순서 없이 빠른 존재 확인만 필요 | `HashSet`/`HashMap` | lookup `eC` |
| 정렬된 순서 유지·범위 조회 필요 | `TreeSet`/`TreeMap` | 모든 연산 `Log`이지만 정렬 보장 |
| 촘촘한 정수 집합 | `BitSet` | lookup `C`, min `eC` |

핵심은 두 가지로 요약됩니다.

1. **동등성**: 카테고리(Seq/Set/Map)가 같고 내용이 같으면 구현 타입과 무관하게 `==`이다. 단, 가변 컬렉션은 내용이 바뀌면 동등성과 해시코드도 함께 바뀌므로 해시 키로 쓰지 않는다.
2. **성능**: "어떤 연산을 자주 하는가"가 컬렉션 선택을 결정한다. 앞쪽 연산 위주면 `List`, 인덱스 접근 위주면 `Vector`, 뒤쪽 누적이면 `ArrayBuffer`, 정렬 유지가 필요하면 `TreeMap`/`TreeSet`.

---

## 참고 자료

- [Equality (Scala 2.13 Collections)](https://docs.scala-lang.org/overviews/collections-2.13/equality.html)
- [Performance Characteristics (Scala 2.13 Collections)](https://docs.scala-lang.org/overviews/collections-2.13/performance-characteristics.html)
