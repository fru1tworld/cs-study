# Lean 4 IO와 iterator

## 문서 범위

> 원문: https://lean-lang.org/doc/reference/latest/IO/

>

> 원문: https://lean-lang.org/doc/reference/latest/Iterators/

- 외부 세계와 상호 작용하는 Lean 프로그램의 구조
- collection을 일관된 방식으로 순회하는 iterator model

## `IO`의 역할

`IO`는 pure expression과 외부 effect를 구분하는 type이다. `IO α`는 실행될 때 effect를 수행하고 `α` 값을 반환할 수 있는 computation을 나타내며, 다음과 같은 외부 동작을 다룬다.

- 대표 effect
  - terminal input, output
  - file system
  - process와 environment
  - clock과 random source
  - task와 concurrency
- `IO` value를 정의하는 것과 실제로 실행하는 것을 구분

## entry point

- executable은 보통 `main` definition에서 시작
- 일반적인 signature
  - `main : IO Unit`
  - command-line argument를 받는 지원 형태
- 종료 성공은 정상 반환, 실패는 exception 또는 명시적 exit code로 표현

```lean
def main : IO Unit := do
  IO.println "Hello, Lean!"
```

## 순차 실행

`do` notation은 `IO` action을 실행할 순서를 나타낸다. `let x ← action`으로 action을 실행하고 결과를 binding하며, effect가 없는 pure 계산은 `let x := value`로 둔다. 마지막에는 `return value`로 result를 만든다.

Effect의 실행 순서도 program 의미의 일부다. 아래 예제에서는 이름을 입력받은 뒤 그 결과를 인사말에 사용한다.

```lean
def greet : IO Unit := do
  IO.print "name: "
  let name ← IO.getLine
  IO.println s!"hello, {name.trim}"
```

## 표준 입출력

- convenience function
  - `IO.print`
  - `IO.println`
  - `IO.eprint`
  - `IO.eprintln`
  - `IO.getLine`
- handle 기반 API로 stdin, stdout, stderr를 직접 다룰 수 있음
- buffer flush 시점이 interactive program의 동작에 영향
- text encoding과 newline 처리의 platform 차이 고려

## 오류 처리

`IO`는 runtime IO error를 exception으로 전달하므로 `try ... catch`로 처리할 수 있다. 복구 가능한 domain error는 `Except`나 `ExceptT`로 분리하면 specification이 명확해진다. 다만 catch 범위를 지나치게 넓히면 원인과 stack context를 잃을 수 있다.

```lean
def readText (path : System.FilePath) : IO String := do
  try
    IO.FS.readFile path
  catch e =>
    throw <| IO.userError s!"cannot read {path}: {e}"
```

## 파일 시스템

- `System.FilePath`로 path 표현
- 주요 operation
  - file read, write
  - directory 생성, 순회
  - metadata 조회
  - path 결합과 normalization
- 작은 text file은 whole-file API가 간단
- 큰 file은 handle과 streaming 방식이 메모리 사용에 유리
- 존재 확인 뒤 사용하면 TOCTOU race가 생길 수 있으므로 실제 operation의 error 처리 필요

## handle과 resource lifetime

File handle은 반드시 닫아야 하므로 정상 반환뿐 아니라 exception 경로에서도 release를 보장해야 한다. Bracket-style resource management나 제공되는 scoped API로 lifetime을 묶고, 필요한 경우 flush와 close의 실패도 별도로 처리한다. 검증할 때는 theorem 수준의 pure model과 실제 handle interpreter를 분리하면 다루기 쉽다.

## process와 environment

- command-line arguments, environment variable, current directory 조회 가능
- child process 실행 시 구분할 항목
  - executable과 argument
  - stdin 전달 방식
  - stdout, stderr capture 또는 inherit
  - exit code
- shell 문자열 하나로 합치기보다 argument list API가 quoting과 injection 위험을 줄임
- environment 값은 외부 입력이므로 parsing과 validation 필요

## 시간과 난수

- wall-clock time과 monotonic time은 용도가 다름
  - wall clock: 날짜와 시각
  - monotonic clock: duration 측정과 timeout
- 시스템 시계는 조정될 수 있으므로 elapsed-time 계산에 부적합할 수 있음
- random IO는 재현 가능한 test를 위해 seed가 명시된 pure generator와 분리 권장
- 보안용 난수는 일반 pseudo-random generator와 요구사항이 다름

## reference와 mutable state

- `IO.Ref α`는 IO 안에서 mutable reference 제공
- 읽기, 쓰기, 수정 operation 사용
- mutation을 local scope에 가두고 외부에는 pure result를 반환하는 방식 권장
- concurrent access에서는 atomicity와 synchronization 보장을 API별로 확인

## task와 concurrency

- `Task α`는 비동기 computation의 handle
- task 생성과 결과 대기 가능
- 여러 task가 공유 state나 IO handle에 접근하면 실행 순서가 비결정적일 수 있음
- cancellation, exception propagation, resource cleanup 정책을 명시
- parallel evaluation은 pure computation에서 추론이 가장 단순

## iterator가 필요한 이유

Iterator는 collection마다 별도 loop를 작성하지 않도록 순회 protocol을 추상화한다. 다음 element를 제공하는 source와, 그 element를 처리하고 계속할지 결정하는 consumer를 분리하는 방식이다. 이 사이에 lazy pipeline을 구성하면 intermediate collection 생성을 피할 수 있다.

## `ForIn`과 `for`

- `ForIn m ρ α` instance가 `ρ`를 순회하며 `α`를 내놓는 방법 정의
- `for x in xs do ...`가 typeclass를 통해 elaboration
- loop body는 monadic computation
- `break`, `continue` 같은 control flow 지원

```lean
def printAll (xs : List String) : IO Unit := do
  for x in xs do
    IO.println x
```

## loop state

Accumulator는 `let mut total := 0`과 assignment notation으로 mutable variable처럼 표기할 수 있다. 이 표기는 elaboration을 거치면 monadic state 전달로 바뀐다. 따라서 loop invariant를 세울 때는 각 iteration 전후에 accumulator가 만족하는 성질을 기술한다.

```lean
def sumArray (xs : Array Nat) : Nat := Id.run do
  let mut total := 0
  for x in xs do
    total := total + x
  return total
```

## iterator state machine

- iterator는 현재 위치를 나타내는 state를 가짐
- 다음 step은 대략 다음 결과를 구분
  - element와 다음 iterator state
  - iteration 종료
- monadic iterator는 step을 얻는 과정 자체가 effectful일 수 있음
- iterator의 내부 state type은 consumer에게 숨겨질 수 있음

## iterator adapter

- source를 다른 source로 변환
- 대표 pattern
  - `map`: 각 element 변환
  - `filter`: 조건을 만족하는 element만 통과
  - `take`: 앞의 유한 개수만 사용
  - `drop`: 앞부분 건너뜀
  - `zip`: 두 source를 함께 순회
  - `enumerate`: index 결합
- 정확한 이름과 available instance는 사용 중인 Lean version의 API 확인

## consumer

- iterator를 최종 result로 축약
- 대표 pattern
  - fold
  - collection으로 수집
  - 첫 matching element 검색
  - 모든 element, 일부 element 조건 검사
  - side-effect 실행
- early termination 가능한 consumer는 무한 iterator에도 종료할 수 있음

## 유한성과 생산성

Iterator는 무한할 수도 있으므로 `collect`나 전체 fold가 항상 종료한다고 가정하면 안 된다. Infinite source에는 `take`나 short-circuit search처럼 순회를 제한하는 방법이 필요하다. 다만 filter를 통과하는 element가 충분하지 않으면 `take n`도 끝나지 않을 수 있다. 따라서 종료성 theorem에는 source의 유한성이나 progress property가 필요하다.

## iterator와 collection의 선택

- 즉시 전체 데이터가 필요: collection
- 한 번 순회하고 intermediate allocation을 줄임: iterator
- 여러 번 순회 또는 random access: materialized collection
- effect가 element마다 발생: monadic iterator 또는 `for`
- theorem에서는 iterator state보다 list semantics로 specification하고 refinement를 증명하는 방식이 실용적

## IO program 설계 지침

- pure core와 effectful shell 분리
- parsing, validation, transformation은 pure function으로 작성
- `IO` layer는 input 획득, pure core 호출, output 기록 담당
- error type을 domain error와 runtime IO error로 구분
- resource lifetime을 lexical scope에 묶음
- test에서는 filesystem, clock, random source를 작은 interface로 대체
