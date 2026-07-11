# 거북이를 바라보는 열세 가지 방법

> 원문: [Thirteen ways of looking at a turtle](https://fsharpforfunandprofit.com/posts/13-ways-of-looking-at-a-turtle/) · [Part 2](https://fsharpforfunandprofit.com/posts/13-ways-of-looking-at-a-turtle-2/)
> 저자: Scott Wlaschin | 정리일: 2026-07-10

같은 거북이 그래픽 API를 13가지 서로 다른 설계 스타일로 구현하며 각 접근의 장단점을 비교하는 글의 학습 노트다. 부분 적용, 검증, 리프팅, 에이전트, 의존성 주입, 상태 모나드, 이벤트 소싱, 스트림 처리, 인터프리터까지 폭넓은 기법이 등장한다. 원문은 1부(방법 1~9)와 2부(방법 10~13)로 나뉘어 있고, 이 노트는 두 편을 모두 다룬다. 전체 소스는 저자의 GitHub 저장소에 있다.

## 요구사항

거북이는 네 가지 명령을 받는다.

1. 현재 방향으로 일정 거리 이동
2. 시계/반시계 방향으로 일정 각도 회전
3. 펜 올리기/내리기 (내린 상태로 이동하면 선이 그려짐)
4. 펜 색상 설정 (검정, 파랑, 빨강)

인터페이스 형태로 쓰면 이렇다.

```
- Move aDistance
- Turn anAngle
- PenUp
- PenDown
- SetColor aColor
```

## 공통 코드

모든 구현이 공유하는 타입과 헬퍼. 각도에 측정 단위(unit of measure)를 붙여 라디안이 아닌 도(degree)임을 타입으로 명시한 점이 눈에 띈다.

```fsharp
/// float의 별칭
type Distance = float

/// 각도가 라디안이 아니라 도(degree)임을 명확히 하기 위해 측정 단위 사용
type [<Measure>] Degrees

/// Degrees 단위 float의 별칭
type Angle  = float<Degrees>

/// 사용 가능한 펜 상태 열거
type PenState = Up | Down

/// 사용 가능한 펜 색상 열거
type PenColor = Black | Red | Blue
```

```fsharp
/// (x,y) 좌표를 담는 구조체
type Position = {x:float; y:float}
```

```fsharp
// 읽기 쉽도록 float를 소수 둘째 자리로 반올림
let round2 (flt:float) = Math.Round(flt,2)

/// 현재 위치에서 각도와 거리로 새 위치를 계산
let calcNewPosition (distance:Distance) (angle:Angle) currentPos =
    // 180.0도 = 1 파이 라디안으로 도를 라디안으로 변환
    let angleInRads = angle * (Math.PI/180.0) * 1.0<1/Degrees>
    // 현재 위치
    let x0 = currentPos.x
    let y0 = currentPos.y
    // 새 위치
    let x1 = x0 + (distance * cos angleInRads)
    let y1 = y0 + (distance * sin angleInRads)
    // 새 Position 반환
    {x=round2 x1; y=round2 y1}
```

```fsharp
/// 기본 초기 상태
let initialPosition,initialColor,initialPenState =
    {x=0.0; y=0.0}, Black, Down
```

```fsharp
let dummyDrawLine log oldPos newPos color =
    // 지금은 로그만 남긴다
    log (sprintf "...Draw line from (%0.1f,%0.1f) to (%0.1f,%0.1f) using %A" oldPos.x oldPos.y newPos.x newPos.y color)
```

## 방법 1. 기본 OO — 가변 상태를 가진 클래스

가장 익숙한 방식. 위치·각도·색상·펜 상태를 가변 필드로 들고, 메서드가 그 필드를 갱신한다.

```fsharp
type Turtle(log) =

    let mutable currentPosition = initialPosition
    let mutable currentAngle = 0.0<Degrees>
    let mutable currentColor = initialColor
    let mutable currentPenState = initialPenState

    member this.Move(distance) =
        log (sprintf "Move %0.1f" distance)
        // 새 위치 계산
        let newPosition = calcNewPosition distance currentAngle currentPosition
        // 필요하면 선 그리기
        if currentPenState = Down then
            dummyDrawLine log currentPosition newPosition currentColor
        // 상태 갱신
        currentPosition <- newPosition

    member this.Turn(angle) =
        log (sprintf "Turn %0.1f" angle)
        // 새 각도 계산
        let newAngle = (currentAngle + angle) % 360.0<Degrees>
        // 상태 갱신
        currentAngle <- newAngle

    member this.PenUp() =
        log "Pen up"
        currentPenState <- Up

    member this.PenDown() =
        log "Pen down"
        currentPenState <- Down

    member this.SetColor(color) =
        log (sprintf "SetColor %A" color)
        currentColor <- color
```

로거를 생성자로 주입받는 점에 주목할 것 — OO 스타일에서도 함수를 파라미터로 넘기는 기법은 유용하다.

```fsharp
/// 메시지를 로그로 남기는 함수
let log message =
    printfn "%s" message

let drawTriangle() =
    let turtle = Turtle(log)
    turtle.Move 100.0
    turtle.Turn 120.0<Degrees>
    turtle.Move 100.0
    turtle.Turn 120.0<Degrees>
    turtle.Move 100.0
    turtle.Turn 120.0<Degrees>
    // (0,0)으로 돌아오고 각도는 0
```

실행 결과는 이렇다.

```
Move 100.0
...Draw line from (0.0,0.0) to (100.0,0.0) using Black
Turn 120.0
Move 100.0
...Draw line from (100.0,0.0) to (50.0,86.6) using Black
Turn 120.0
Move 100.0
...Draw line from (50.0,86.6) to (0.0,0.0) using Black
Turn 120.0
```

```fsharp
let drawPolygon n =
    let angle = 180.0 - (360.0/float n)
    let angleDegrees = angle * 1.0<Degrees>
    let turtle = Turtle(log)

    // 한 변을 그리는 함수 정의
    let drawOneSide() =
        turtle.Move 100.0
        turtle.Turn angleDegrees

    // 모든 변에 대해 반복
    for i in [1..n] do
        drawOneSide()
```

- 장점: 구현과 이해가 매우 쉽다.
- 단점: 상태가 있는 코드는 테스트 전에 객체를 특정 상태로 만들어야 해서 번거롭다. 클라이언트가 특정 구현에 결합된다(인터페이스 없음).

## 방법 2. 기본 FP — 불변 상태를 다루는 함수 모듈

상태를 불변 레코드로 정의하고, 각 함수가 상태를 받아 새 상태를 반환한다.

```fsharp
module Turtle =

    type TurtleState = {
        position : Position
        angle : float<Degrees>
        color : PenColor
        penState : PenState
    }

    let initialTurtleState = {
        position = initialPosition
        angle = 0.0<Degrees>
        color = initialColor
        penState = initialPenState
    }
```

```fsharp
module Turtle =

    // [상태 타입 생략]

    let move log distance state =
        log (sprintf "Move %0.1f" distance)
        // 새 위치 계산
        let newPosition = calcNewPosition distance state.angle state.position
        // 필요하면 선 그리기
        if state.penState = Down then
            dummyDrawLine log state.position newPosition state.color
        // 상태 갱신
        {state with position = newPosition}

    let turn log angle state =
        log (sprintf "Turn %0.1f" angle)
        // 새 각도 계산
        let newAngle = (state.angle + angle) % 360.0<Degrees>
        // 상태 갱신
        {state with angle = newAngle}

    let penUp log state =
        log "Pen up"
        {state with penState = Up}

    let penDown log state =
        log "Pen down"
        {state with penState = Down}

    let setColor log color state =
        log (sprintf "SetColor %A" color)
        {state with color = color}
```

클라이언트는 부분 적용으로 로거를 미리 구워 넣은 버전을 만들어 쓴다.

```fsharp
/// 메시지를 로그로 남기는 함수
let log message =
    printfn "%s" message

// 부분 적용으로 log를 구워 넣은 버전들
let move = Turtle.move log
let turn = Turtle.turn log
let penDown = Turtle.penDown log
let penUp = Turtle.penUp log
let setColor = Turtle.setColor log
```

상태 파라미터가 마지막에 있으므로 파이프 연산자로 자연스럽게 이어진다.

```fsharp
let drawTriangle() =
    Turtle.initialTurtleState
    |> move 100.0
    |> turn 120.0<Degrees>
    |> move 100.0
    |> turn 120.0<Degrees>
    |> move 100.0
    |> turn 120.0<Degrees>
    // (0,0)으로 돌아오고 각도는 0
```

```fsharp
let drawPolygon n =
    let angle = 180.0 - (360.0/float n)
    let angleDegrees = angle * 1.0<Degrees>

    // 한 변을 그리는 함수 정의
    let oneSide state sideNumber =
        state
        |> move 100.0
        |> turn angleDegrees

    // 모든 변에 대해 반복
    [1..n]
    |> List.fold oneSide Turtle.initialTurtleState
```

- 장점: 역시 구현이 쉽다. 상태 없는 함수라 테스트 준비가 필요 없고, 함수가 모듈화되어 다른 맥락에서 재사용할 수 있다.
- 단점: 여전히 특정 구현에 결합된다. 클라이언트가 상태를 직접 추적하며 넘겨야 한다.

## 방법 3. OO 코어를 감싼 API

문자열 명령("Move 100" 등)을 받는 API 계층을 방법 1의 클래스 앞에 세운다. 서비스 경계에서 흔히 보는 구조다. 잘못된 입력은 예외로 처리한다.

```fsharp
exception TurtleApiException of string
```

```fsharp
// distance 파라미터를 float로 변환, 실패 시 예외
let validateDistance distanceStr =
    try
        float distanceStr
    with
    | ex ->
        let msg = sprintf "Invalid distance '%s' [%s]" distanceStr  ex.Message
        raise (TurtleApiException msg)

// angle 파라미터를 float<Degrees>로 변환, 실패 시 예외
let validateAngle angleStr =
    try
        (float angleStr) * 1.0<Degrees>
    with
    | ex ->
        let msg = sprintf "Invalid angle '%s' [%s]" angleStr ex.Message
        raise (TurtleApiException msg)

// color 파라미터를 PenColor로 변환, 실패 시 예외
let validateColor colorStr =
    match colorStr with
    | "Black" -> Black
    | "Blue" -> Blue
    | "Red" -> Red
    | _ ->
        let msg = sprintf "Color '%s' is not recognized" colorStr
        raise (TurtleApiException msg)
```

```fsharp
type TurtleApi() =

    let turtle = Turtle(log)

    member this.Exec (commandStr:string) =
        let tokens = commandStr.Split(' ') |> List.ofArray |> List.map trimString
        match tokens with
        | [ "Move"; distanceStr ] ->
            let distance = validateDistance distanceStr
            turtle.Move distance
        | [ "Turn"; angleStr ] ->
            let angle = validateAngle angleStr
            turtle.Turn angle
        | [ "Pen"; "Up" ] ->
            turtle.PenUp()
        | [ "Pen"; "Down" ] ->
            turtle.PenDown()
        | [ "SetColor"; colorStr ] ->
            let color = validateColor colorStr
            turtle.SetColor color
        | _ ->
            let msg = sprintf "Instruction '%s' is not recognized" commandStr
            raise (TurtleApiException msg)
```

```fsharp
let drawPolygon n =
    let angle = 180.0 - (360.0/float n)
    let api = TurtleApi()

    // 한 변을 그리는 함수 정의
    let drawOneSide() =
        api.Exec "Move 100.0"
        api.Exec (sprintf "Turn %f" angle)

    // 모든 변에 대해 반복
    for i in [1..n] do
        drawOneSide()
```

```fsharp
let triggerError() =
    let api = TurtleApi()
    api.Exec "Move bad"
```

- 장점: 거북이 구현이 클라이언트에게 숨겨진다. 서비스 경계의 API라 검증과 모니터링을 넣기 좋다.
- 단점: API가 특정 구현에 결합된다. 시스템 전체가 상태를 가져 테스트가 어렵다.

## 방법 4. 함수형 코어를 감싼 API

"Functional Core / Imperative Shell" 패턴. 검증 실패를 예외 대신 Result로 표현하고, 상태 변경은 경계(API 클래스)의 한 곳에만 몰아넣는다.

```fsharp
type ErrorMessage =
    | InvalidDistance of string
    | InvalidAngle of string
    | InvalidColor of string
    | InvalidCommand of string
```

```fsharp
let validateDistance distanceStr =
    try
        Success (float distanceStr)
    with
    | ex ->
        Failure (InvalidDistance distanceStr)
```

```fsharp
type TurtleApi() =

    let mutable state = initialTurtleState

    /// 가변 상태 값 갱신
    let updateState newState =
        state <- newState
```

핵심 기법은 리프팅이다. 일반 함수(move 등)를 Result 세계로 끌어올려, 검증 결과(Result)와 현재 상태를 함께 넘긴다.

```fsharp
// 현재 상태를 Result로 리프팅
let stateR = returnR state

// 거리를 Result로 얻기
let distanceR = validateDistance distanceStr

// Result 세계로 리프팅한 "move" 호출
lift2R move distanceR stateR
```

전체 Exec 구현.

```fsharp
/// 명령 문자열을 실행하고 Result를 반환
/// Exec : commandStr:string -> Result<unit,ErrorMessage>
member this.Exec (commandStr:string) =
    let tokens = commandStr.Split(' ') |> List.ofArray |> List.map trimString

    // 현재 상태를 Result로 리프팅
    let stateR = returnR state

    // 새 상태 계산
    let newStateR =
        match tokens with
        | [ "Move"; distanceStr ] ->
            // 거리를 Result로 얻기
            let distanceR = validateDistance distanceStr

            // Result 세계로 리프팅한 "move" 호출
            lift2R move distanceR stateR

        | [ "Turn"; angleStr ] ->
            let angleR = validateAngle angleStr
            lift2R turn angleR stateR

        | [ "Pen"; "Up" ] ->
            returnR (penUp state)

        | [ "Pen"; "Down" ] ->
            returnR (penDown state)

        | [ "SetColor"; colorStr ] ->
            let colorR = validateColor colorStr
            lift2R setColor colorR stateR

        | _ ->
            Failure (InvalidCommand commandStr)

    // `updateState`를 Result 세계로 리프팅해 새 상태로 호출
    mapR updateState newStateR

    // 최종 결과(updateState의 출력) 반환
```

클라이언트는 `result` 컴퓨테이션 표현식으로 에러 전파를 처리한다.

```fsharp
let drawTriangle() =
    let api = TurtleApi()
    result {
        do! api.Exec "Move 100"
        do! api.Exec "Turn 120"
        do! api.Exec "Move 100"
        do! api.Exec "Turn 120"
        do! api.Exec "Move 100"
        do! api.Exec "Turn 120"
        }
```

```fsharp
let drawPolygon n =
    let angle = 180.0 - (360.0/float n)
    let api = TurtleApi()

    // 한 변을 그리는 함수 정의
    let drawOneSide() = result {
        do! api.Exec "Move 100.0"
        do! api.Exec (sprintf "Turn %f" angle)
        }

    // 모든 변에 대해 반복
    result {
        for i in [1..n] do
            do! drawOneSide()
    }
```

- 장점: 구현 은닉과 검증은 그대로 얻으면서, 상태를 가진 부분이 경계 하나뿐이고 코어는 무상태라 테스트하기 좋다.
- 단점: API가 여전히 특정 구현에 결합된다.

## 방법 5. 에이전트 앞의 API

가변 상태를 메시지 큐 뒤로 숨기는 액터/에이전트 방식. API는 명령을 메시지로 만들어 에이전트에게 보내기만 한다.

```fsharp
type TurtleCommand =
    | Move of Distance
    | Turn of Angle
    | PenUp
    | PenDown
    | SetColor of PenColor
```

에이전트 내부에서는 재귀 루프의 파라미터로 상태를 넘기므로 가변 필드조차 필요 없다.

```fsharp
type TurtleAgent() =

    /// 메시지를 로그로 남기는 함수
    let log message =
        printfn "%s" message

    // 로그가 적용된 버전들
    let move = Turtle.move log
    let turn = Turtle.turn log
    let penDown = Turtle.penDown log
    let penUp = Turtle.penUp log
    let setColor = Turtle.setColor log

    let mailboxProc = MailboxProcessor.Start(fun inbox ->
        let rec loop turtleState = async {
            // 큐에서 명령 메시지 읽기
            let! command = inbox.Receive()
            // 메시지를 처리해 새 상태 생성
            let newState =
                match command with
                | Move distance ->
                    move distance turtleState
                | Turn angle ->
                    turn angle turtleState
                | PenUp ->
                    penUp turtleState
                | PenDown ->
                    penDown turtleState
                | SetColor color ->
                    setColor color turtleState
            return! loop newState
            }
        loop Turtle.initialTurtleState )

    // 큐를 외부에 노출
    member this.Post(command) =
        mailboxProc.Post command
```

```fsharp
member this.Exec (commandStr:string) =
    let tokens = commandStr.Split(' ') |> List.ofArray |> List.map trimString

    // 새 상태 계산
    let result =
        match tokens with
        | [ "Move"; distanceStr ] -> result {
            let! distance = validateDistance distanceStr
            let command = Move distance
            turtleAgent.Post command
            }

        | [ "Turn"; angleStr ] -> result {
            let! angle = validateAngle angleStr
            let command = Turn angle
            turtleAgent.Post command
            }

        | [ "Pen"; "Up" ] -> result {
            let command = PenUp
            turtleAgent.Post command
            }

        | [ "Pen"; "Down" ] -> result {
            let command = PenDown
            turtleAgent.Post command
            }

        | [ "SetColor"; colorStr ] -> result {
            let! color = validateColor colorStr
            let command = SetColor color
            turtleAgent.Post command
            }

        | _ ->
            Failure (InvalidCommand commandStr)

    // 에러가 있으면 반환
    result
```

- 장점: 락 없이 가변 상태를 보호한다. 메시지 큐가 API와 구현을 분리한다. 태생적으로 비동기이고 수평 확장이 쉽다.
- 단점: 에이전트도 결국 상태를 가지므로 상태 객체와 같은 테스트·추론 문제가 있다. 견고하게 만들려면 슈퍼바이저, 하트비트, 백프레셔 등이 필요해 복잡해진다.

## 방법 6. 인터페이스를 이용한 의존성 주입

API와 구현의 결합을 끊는 고전적인 방법. OO식 인터페이스와 FP식 함수 레코드 두 가지를 비교한다.

OO 인터페이스.

```fsharp
type ITurtle =
    abstract Move : Distance -> unit
    abstract Turn : Angle -> unit
    abstract PenUp : unit -> unit
    abstract PenDown : unit -> unit
    abstract SetColor : PenColor -> unit
```

```fsharp
type TurtleApi(turtle: ITurtle) =

    // 기타 코드

    member this.Exec (commandStr:string) =
        let tokens = commandStr.Split(' ') |> List.ofArray |> List.map trimString
        match tokens with
        | [ "Move"; distanceStr ] ->
            let distance = validateDistance distanceStr
            turtle.Move distance
        | [ "Turn"; angleStr ] ->
            let angle = validateAngle angleStr
            turtle.Turn angle
        // 기타
```

F#의 객체 표현식(object expression)으로 클래스 없이 인터페이스 구현체를 만든다.

```fsharp
let normalSize() =
    let log = printfn "%s"
    let turtle = Turtle(log)

    // Turtle을 감싼 인터페이스 반환
    {new ITurtle with
        member this.Move dist = turtle.Move dist
        member this.Turn angle = turtle.Turn angle
        member this.PenUp() = turtle.PenUp()
        member this.PenDown() = turtle.PenDown()
        member this.SetColor color = turtle.SetColor color
    }
```

데코레이터 패턴 — 이동 거리를 절반으로 줄이는 구현.

```fsharp
let halfSize() =
    let normalSize = normalSize()

    // 데코레이트한 인터페이스 반환
    {new ITurtle with
        member this.Move dist = normalSize.Move (dist/2.0)   // 절반!!
        member this.Turn angle = normalSize.Turn angle
        member this.PenUp() = normalSize.PenUp()
        member this.PenDown() = normalSize.PenDown()
        member this.SetColor color = normalSize.SetColor color
    }
```

```fsharp
let drawTriangle(api:TurtleApi) =
    api.Exec "Move 100"
    api.Exec "Turn 120"
    api.Exec "Move 100"
    api.Exec "Turn 120"
    api.Exec "Move 100"
    api.Exec "Turn 120"

let iTurtle = normalSize()   // ITurtle 타입
let api = TurtleApi(iTurtle)
drawTriangle(api)
```

함수형 대안은 인터페이스 대신 함수들의 레코드를 넘기는 것이다.

```fsharp
type TurtleFunctions = {
    move : Distance -> TurtleState -> TurtleState
    turn : Angle -> TurtleState -> TurtleState
    penUp : TurtleState -> TurtleState
    penDown : TurtleState -> TurtleState
    setColor : PenColor -> TurtleState -> TurtleState
    }
```

```fsharp
type TurtleApi(turtleFunctions: TurtleFunctions) =

    let mutable state = initialTurtleState
```

레코드 방식에서는 구현체 생성과 데코레이션이 훨씬 간결하다. 레코드 복사 문법(`with`)으로 한 필드만 바꾸면 된다.

```fsharp
let normalSize() =
    let log = printfn "%s"
    // 함수 레코드 반환
    {
        move = Turtle.move log
        turn = Turtle.turn log
        penUp = Turtle.penUp log
        penDown = Turtle.penDown log
        setColor = Turtle.setColor log
    }

let halfSize() =
    let normalSize = normalSize()
    // 축소된 거북이 반환
    { normalSize with
        move = fun dist -> normalSize.move (dist/2.0)
    }
```

- 장점: 인터페이스 덕에 API가 구현과 분리된다. FP 레코드 방식은 복제가 쉽고 함수가 무상태다.
- 단점: 인터페이스는 덩어리째 움직여서 인터페이스 분리 원칙(ISP)을 깨기 쉽다. 개별 함수와 달리 인터페이스는 합성이 안 된다. OO 방식은 기존 클래스 수정이 필요할 수 있다.

## 방법 7. 함수를 이용한 의존성 주입

인터페이스조차 없애고 함수 자체를 파라미터로 넘긴다. 두 가지 변형이 있다.

### 변형 1: 의존성을 함수 하나씩 따로 넘기기

```fsharp
member this.Exec move turn penUp penDown setColor (commandStr:string) =
    ...

    // Success of unit 또는 Failure 반환
    match tokens with
    | [ "Move"; distanceStr ] -> result {
        let! distance = validateDistance distanceStr
        let newState = move distance state   // 파라미터로 받은 `move` 함수 사용
        updateState newState
        }
    | [ "Turn"; angleStr ] -> result {
        let! angle = validateAngle angleStr
        let newState = turn angle state   // 파라미터로 받은 `turn` 함수 사용
        updateState newState
        }
    ...
```

구현체는 부분 적용으로 만든다. DI 컨테이너 없이 언어 기능만으로 의존성 주입이 된다.

```fsharp
let log = printfn "%s"
let move = Turtle.move log
let turn = Turtle.turn log
let penUp = Turtle.penUp log
let penDown = Turtle.penDown log
let setColor = Turtle.setColor log

let normalSize() =
    let api = TurtleApi()
    // 함수들을 부분 적용
    api.Exec move turn penUp penDown setColor
    // 반환값은 함수:
    //     string -> Result<unit,ErrorMessage>

let halfSize() =
    let moveHalf dist = move (dist/2.0)
    let api = TurtleApi()
    // 함수들을 부분 적용
    api.Exec moveHalf turn penUp penDown setColor
    // 반환값은 함수:
    //     string -> Result<unit,ErrorMessage>
```

API 자체가 그냥 함수 하나가 된다.

```fsharp
// API 타입은 그냥 함수다
type ApiFunction = string -> Result<unit,ErrorMessage>

let drawTriangle(api:ApiFunction) =
    result {
        do! api "Move 100"
        do! api "Turn 120"
        do! api "Move 100"
        do! api "Turn 120"
        do! api "Move 100"
        do! api "Turn 120"
        }

let apiFn = normalSize()  // string -> Result<unit,ErrorMessage>
drawTriangle(apiFn)

let apiFn = halfSize()
drawTriangle(apiFn)
```

테스트용 목(mock)도 함수 하나면 된다.

```fsharp
let mockApi s =
    printfn "[MockAPI] %s" s
    Success ()

drawTriangle(mockApi)
```

### 변형 2: 판별 유니언으로 함수 하나만 넘기기

함수 다섯 개를 따로 넘기는 대신 명령을 데이터로 표현하고, 그 명령을 처리하는 함수 하나만 넘긴다.

```fsharp
type TurtleCommand =
    | Move of Distance
    | Turn of Angle
    | PenUp
    | PenDown
    | SetColor of PenColor
```

```fsharp
member this.Exec turtleFn (commandStr:string) =
    ...

    // Success of unit 또는 Failure 반환
    match tokens with
    | [ "Move"; distanceStr ] -> result {
        let! distance = validateDistance distanceStr
        let command =  Move distance      // Command 객체 생성
        let newState = turtleFn command state
        updateState newState
        }
    | [ "Turn"; angleStr ] -> result {
        let! angle = validateAngle angleStr
        let command =  Turn angle      // Command 객체 생성
        let newState = turtleFn command state
        updateState newState
        }
    ...
```

```fsharp
let log = printfn "%s"
let move = Turtle.move log
let turn = Turtle.turn log
let penUp = Turtle.penUp log
let penDown = Turtle.penDown log
let setColor = Turtle.setColor log

let normalSize() =
    let turtleFn = function
        | Move dist -> move dist
        | Turn angle -> turn angle
        | PenUp -> penUp
        | PenDown -> penDown
        | SetColor color -> setColor color

    // 함수를 API에 부분 적용
    let api = TurtleApi()
    api.Exec turtleFn
    // 반환값은 함수:
    //     string -> Result<unit,ErrorMessage>

let halfSize() =
    let turtleFn = function
        | Move dist -> move (dist/2.0)
        | Turn angle -> turn angle
        | PenUp -> penUp
        | PenDown -> penDown
        | SetColor color -> setColor color

    // 함수를 API에 부분 적용
    let api = TurtleApi()
    api.Exec turtleFn
    // 반환값은 함수:
    //     string -> Result<unit,ErrorMessage>
```

- 장점: 파라미터화만으로 API와 구현이 분리된다. 의존성이 시그니처에 그대로 드러나 숨은 의존성 증식을 막는다. 함수 파라미터 하나하나가 자동으로 "메서드 하나짜리 인터페이스"다. 부분 적용이 특별한 패턴 없이 DI를 대신한다.
- 단점: 함수가 4개를 넘어가면 따로 넘기는 방식이 거추장스럽다. 판별 유니언 방식은 인터페이스보다 다루기 까다로울 수 있다.

## 방법 8. 상태 모나드를 이용한 배치 처리

클라이언트가 상태를 직접 나르지 않도록, 상태 전달을 자동화하는 래퍼 타입을 만든다. 이것이 상태 모나드이며, 여기서는 거북이 전용으로 `TurtleStateComputation`이라 이름 붙였다.

```fsharp
type TurtleStateComputation<'a> =
    TurtleStateComputation of (Turtle.TurtleState -> 'a * Turtle.TurtleState)
```

상태를 받아 "결과와 새 상태의 쌍"을 반환하는 함수를 감싼 타입이다. 실행은 안의 함수를 꺼내 초기 상태를 먹이는 것이다.

```fsharp
let runT turtle state =
    // 패턴 매칭으로 내부 함수 추출
    let (TurtleStateComputation innerFn) = turtle
    // 넘겨받은 상태로 내부 함수 실행
    innerFn state
```

컴퓨테이션 표현식 빌더를 정의하면 `turtle { ... }` 문법을 쓸 수 있다.

```fsharp
// 컴퓨테이션 표현식 빌더 정의
type TurtleBuilder() =
    member this.Return(x) = returnT x
    member this.Bind(x,f) = bindT f x

// 빌더 인스턴스 생성
let turtle = TurtleBuilder()
```

거북이 함수들을 이 세계로 리프팅한다.

```fsharp
let move dist =
    toUnitComputation (Turtle.move log dist)
// val move : Distance -> TurtleStateComputation<unit>

let turn angle =
    toUnitComputation (Turtle.turn log angle)
// val turn : Angle -> TurtleStateComputation<unit>

let penDown =
    toUnitComputation (Turtle.penDown log)
// val penDown : TurtleStateComputation<unit>

let penUp =
    toUnitComputation (Turtle.penUp log)
// val penUp : TurtleStateComputation<unit>

let setColor color =
    toUnitComputation (Turtle.setColor log color)
// val setColor : PenColor -> TurtleStateComputation<unit>
```

클라이언트 코드는 명령형처럼 읽히지만 상태는 뒤에서 불변으로 흐른다.

```fsharp
let drawTriangle() =
    // 일련의 명령 정의
    let t = turtle {
        do! move 100.0
        do! turn 120.0<Degrees>
        do! move 100.0
        do! turn 120.0<Degrees>
        do! move 100.0
        do! turn 120.0<Degrees>
        }

    // 마지막으로 초기 상태를 입력으로 실행
    runT t initialTurtleState
```

워크플로는 합성이 가능하다. 워크플로 둘을 이어 새 워크플로를 만들 수 있다.

```fsharp
let drawPolygon n =
    let angle = 180.0 - (360.0/float n)
    let angleDegrees = angle * 1.0<Degrees>

    // 한 변을 그리는 함수 정의
    let oneSide = turtle {
        do! move 100.0
        do! turn angleDegrees
        }

    // 거북이 연산 두 개를 순서대로 연결
    let chain f g  = turtle {
        do! f
        do! g
        }

    // 변마다 하나씩 연산 리스트 생성
    let sides = List.replicate n oneSide

    // 모든 변을 하나의 연산으로 연결
    let all = sides |> List.reduce chain

    // 마지막으로 초기 상태로 실행
    runT all initialTurtleState
```

- 장점: 명령형처럼 보이는 클라이언트 코드에 불변성이 보존된다. 워크플로가 합성 가능하다.
- 단점: 특정 구현에 결합된다. 상태를 명시적으로 넘기는 것보다 복잡하다. 모나드/워크플로가 중첩되면 다루기 어렵다.

## 방법 9. 커맨드 객체를 이용한 배치 처리

명령 자체를 데이터(리스트)로 만들어 모았다가 fold로 한꺼번에 실행한다.

```fsharp
type TurtleCommand =
    | Move of Distance
    | Turn of Angle
    | PenUp
    | PenDown
    | SetColor of PenColor

/// 명령을 거북이 상태에 적용해 새 상태 반환
let applyCommand state command =
    match command with
    | Move distance ->
        move distance state
    | Turn angle ->
        turn angle state
    | PenUp ->
        penUp state
    | PenDown ->
        penDown state
    | SetColor color ->
        setColor color state

/// 명령 리스트를 한 번에 실행
let run aListOfCommands =
    aListOfCommands
    |> List.fold applyCommand Turtle.initialTurtleState
```

```fsharp
let drawTriangle() =
    // 명령 리스트 생성
    let commands = [
        Move 100.0
        Turn 120.0<Degrees>
        Move 100.0
        Turn 120.0<Degrees>
        Move 100.0
        Turn 120.0<Degrees>
        ]
    // 실행
    run commands
```

```fsharp
let drawPolygon n =
    let angle = 180.0 - (360.0/float n)
    let angleDegrees = angle * 1.0<Degrees>

    // 한 변을 그리는 함수 정의
    let drawOneSide sideNumber = [
        Move 100.0
        Turn angleDegrees
        ]

    // 모든 변에 대해 반복
    let commands =
        [1..n] |> List.collect drawOneSide

    // 명령 실행
    run commands
```

- 장점: 워크플로/모나드보다 만들고 쓰기가 단순하다. 구현에 결합된 함수가 `applyCommand` 하나뿐이고 나머지는 전부 분리된다.
- 단점: 배치 전용이다. 앞선 명령의 응답에 따라 흐름을 바꿔야 하는 경우에는 못 쓴다(그 경우는 방법 12 참고).

## 막간: 데이터 타입에 의한 의식적 분리

방법 5(에이전트), 7(함수형 DI), 9(배치), 그리고 뒤에 나올 10(이벤트 소싱), 13(인터프리터)의 공통점은 전부 데이터 타입으로 결합을 끊는다는 것이다.

- OO 설계는 캡슐화된 행위 묶음(인터페이스)을 공유해서 분리한다.
- 함수형 설계는 공통 데이터 타입(일종의 프로토콜)에 합의해서 분리한다.

데이터 기반 분리의 이점은 다음과 같다.

- 합성이 쉽다. 행위 기반 인터페이스는 합성이 어렵다.
- 모든 함수가 이미 분리되어 있어 리팩터링 시 인터페이스를 소급해서 뽑아낼 필요가 없다.
- 데이터 구조는 직렬화가 쉬워 원격 서비스 경계로 자연스럽게 확장된다.
- 안전하게 진화시키기 쉽다. 예컨대 판별 유니언에 케이스를 추가하면 컴파일러가 모든 클라이언트의 미처리 지점을 잡아준다.

웹 서비스에서 RPC보다 메시지 지향 API가 유연하고 합성하기 좋다고 평가받는 것과 같은 원리다.

## 방법 10. 이벤트 소싱

에이전트(방법 5)와 배치(방법 9)의 "명령" 개념을 잇되, 상태를 갱신하는 수단을 명령이 아닌 이벤트로 바꾼다. 흐름은 이렇다.

- 클라이언트가 `CommandHandler`에 `Command`를 보낸다.
- 핸들러는 해당 거북이의 과거 이벤트를 전부 재생해 현재 상태를 재구성한다.
- 명령을 검증하고, 그 결과로 발생한 이벤트들을 만든다.
- 이벤트를 `EventStore`에 저장한다.

상태는 어디에도 저장되지 않는다. 이벤트 이력이 곧 진실의 원천이다.

```fsharp
type TurtleId = System.Guid

/// 거북이에 대해 원하는 동작
type TurtleCommandAction =
    | Move of Distance
    | Turn of Angle
    | PenUp
    | PenDown
    | SetColor of PenColor

/// 특정 거북이에게 보내는, 원하는 동작을 나타내는 명령
type TurtleCommand = {
    turtleId : TurtleId
    action : TurtleCommandAction
    }

/// 실제로 일어난 상태 변화를 나타내는 이벤트
type StateChangedEvent =
    | Moved of Distance
    | Turned of Angle
    | PenWentUp
    | PenWentDown
    | ColorChanged of PenColor

/// 실제로 일어난 이동을 나타내는 이벤트
type MovedEvent = {
    startPos : Position
    endPos : Position
    penColor : PenColor option
    }

/// 가능한 모든 이벤트의 합집합
type TurtleEvent =
    | StateChangedEvent of StateChangedEvent
    | MovedEvent of MovedEvent
```

명령은 요청(미래형), 이벤트는 일어난 사실(과거형)이라는 구분에 주목할 것.

```fsharp
/// 이벤트를 현재 상태에 적용해 거북이의 새 상태 반환
let applyEvent log oldState event =
    match event with
    | Moved distance ->
        Turtle.move log distance oldState
    | Turned angle ->
        Turtle.turn log angle oldState
    | PenWentUp ->
        Turtle.penUp log oldState
    | PenWentDown ->
        Turtle.penDown log oldState
    | ColorChanged color ->
        Turtle.setColor log color oldState

// 명령과 상태를 바탕으로 어떤 이벤트를 생성할지 결정
let eventsFromCommand log command stateBeforeCommand =

    // --------------------------
    // TurtleCommand로부터 StateChangedEvent 생성
    let stateChangedEvent =
        match command.action with
        | Move dist -> Moved dist
        | Turn angle -> Turned angle
        | PenUp -> PenWentUp
        | PenDown -> PenWentDown
        | SetColor color -> ColorChanged color

    // --------------------------
    // 새 이벤트로부터 현재 상태 계산
    let stateAfterCommand =
        applyEvent log stateBeforeCommand stateChangedEvent

    // --------------------------
    // MovedEvent 생성
    let startPos = stateBeforeCommand.position
    let endPos = stateAfterCommand.position
    let penColor =
        if stateBeforeCommand.penState=Down then
            Some stateBeforeCommand.color
        else
            None

    let movedEvent = {
        startPos = startPos
        endPos = endPos
        penColor = penColor
        }

    // --------------------------
    // 이벤트 리스트 반환
    if startPos <> endPos then
        [ StateChangedEvent stateChangedEvent; MovedEvent movedEvent]
    else
        [ StateChangedEvent stateChangedEvent]

/// 거북이 id로 StateChangedEvent들을 가져오는 함수 타입
type GetStateChangedEventsForId =
     TurtleId -> StateChangedEvent list

/// TurtleEvent를 저장하는 함수 타입
type SaveTurtleEvent =
    TurtleId -> TurtleEvent -> unit

/// 메인 함수: 명령 처리
let commandHandler
    (log:string -> unit)
    (getEvents:GetStateChangedEventsForId)
    (saveEvent:SaveTurtleEvent)
    (command:TurtleCommand) =

    /// 먼저 이벤트 저장소에서 모든 이벤트 로드
    let eventHistory =
        getEvents command.turtleId

    /// 그 다음, 명령 이전의 상태를 재구성
    let stateBeforeCommand =
        let nolog = ignore
        eventHistory
        |> List.fold (applyEvent nolog) Turtle.initialTurtleState

    /// 명령과 stateBeforeCommand로부터 이벤트 생성
    let events = eventsFromCommand log command stateBeforeCommand

    // 이벤트를 이벤트 저장소에 저장
    events |> List.iter (saveEvent command.turtleId)
```

```fsharp
let turtleId = System.Guid.NewGuid()
let move dist = {turtleId=turtleId; action=Move dist}
let turn angle = {turtleId=turtleId; action=Turn angle}
let penDown = {turtleId=turtleId; action=PenDown}
let penUp = {turtleId=turtleId; action=PenUp}
let setColor color = {turtleId=turtleId; action=SetColor color}

let drawTriangle() =
    let handler = makeCommandHandler()
    handler (move 100.0)
    handler (turn 120.0<Degrees>)
    handler (move 100.0)
    handler (turn 120.0<Degrees>)
    handler (move 100.0)
    handler (turn 120.0<Degrees>)
```

- 장점: 코드 전체가 무상태라 테스트가 쉽다. 이벤트 재생이 자연스럽게 지원된다(감사, 시점 복원, 리플레이 디버깅).
- 단점: CRUD보다 구현이 어렵다. 관리에 신경 쓰지 않으면 커맨드 핸들러가 비대해진다.

## 방법 11. 함수형 회고적 프로그래밍 (스트림 처리)

이벤트 소싱에 함수형 반응형 프로그래밍(FRP)을 결합한 형태. 저자는 "이미 일어난 이벤트에 반응한다"는 뜻에서 Functional Retroactive Programming이라 농담 삼아 부른다. 쓰기 쪽은 이벤트 소싱과 같지만, 커맨드 핸들러는 상태 갱신 같은 최소한의 일만 하고 복잡한 도메인 로직은 이벤트 스트림을 구독하는 다운스트림 프로세서들이 맡는다. CQRS의 읽기/쓰기 분리와 같은 발상이다.

이벤트 스트림에서 원하는 이벤트만 골라내는 필터들.

```fsharp
// TurtleEvent만 고르는 필터
let turtleFilter ev =
    match box ev with
    | :? TurtleEvent as tev -> Some tev
    | _ -> None

// TurtleEvent 중 MovedEvent만 고르는 필터
let moveFilter = function
    | MovedEvent ev -> Some ev
    | _ -> None

// TurtleEvent 중 StateChangedEvent만 고르는 필터
let stateChangedEventFilter = function
    | StateChangedEvent ev -> Some ev
    | _ -> None
```

프로세서 세 개 — 실제 거북이 구동, 그래픽 출력, 잉크 사용량 집계. 각각 독립적으로 스트림을 구독한다.

```fsharp
/// 거북이를 물리적으로 이동
let physicalTurtleProcessor (eventStream:IObservable<Guid*obj>) =

    // 옵저버블 입력을 처리하는 함수
    let subscriberFn (ev:MovedEvent) =
        let colorText =
            match ev.penColor with
            | Some color -> sprintf "line of color %A" color
            | None -> "no line"
        printfn "[turtle  ]: Moved from (%0.2f,%0.2f) to (%0.2f,%0.2f) with %s"
            ev.startPos.x ev.startPos.y ev.endPos.x ev.endPos.y colorText

    // 전체 이벤트에서 시작
    eventStream
    // TurtleEvent만 필터링
    |> Observable.choose (function (id,ev) -> turtleFilter ev)
    // MovedEvent만 필터링
    |> Observable.choose moveFilter
    // 처리
    |> Observable.subscribe subscriberFn

/// 그래픽 장치에 선 그리기
let graphicsProcessor (eventStream:IObservable<Guid*obj>) =

    // 옵저버블 입력을 처리하는 함수
    let subscriberFn (ev:MovedEvent) =
        match ev.penColor with
        | Some color ->
            printfn "[graphics]: Draw line from (%0.2f,%0.2f) to (%0.2f,%0.2f) with color %A"
                ev.startPos.x ev.startPos.y ev.endPos.x ev.endPos.y color
        | None ->
            ()

    // 전체 이벤트에서 시작
    eventStream
    // TurtleEvent만 필터링
    |> Observable.choose (function (id,ev) -> turtleFilter ev)
    // MovedEvent만 필터링
    |> Observable.choose moveFilter
    // 처리
    |> Observable.subscribe subscriberFn

/// "moved" 이벤트를 듣고 집계해 총 잉크 사용량 추적
let inkUsedProcessor (eventStream:IObservable<Guid*obj>) =

    // 새 이벤트가 발생하면 지금까지의 총 이동 거리를 누적
    let accumulate distanceSoFar (ev:StateChangedEvent) =
        match ev with
        | Moved dist ->
            distanceSoFar + dist
        | _ ->
            distanceSoFar

    // 옵저버블 입력을 처리하는 함수
    let subscriberFn distanceSoFar  =
        printfn "[ink used]: %0.2f" distanceSoFar

    // 전체 이벤트에서 시작
    eventStream
    // TurtleEvent만 필터링
    |> Observable.choose (function (id,ev) -> turtleFilter ev)
    // StateChangedEvent만 필터링
    |> Observable.choose stateChangedEventFilter
    // 총 거리 누적
    |> Observable.scan accumulate 0.0
    // 처리
    |> Observable.subscribe subscriberFn
```

```fsharp
let drawTriangle() =
    // 이전 이벤트 정리
    eventStore.Clear turtleId

    // IEvent에서 이벤트 스트림 생성
    let eventStream = eventStore.SaveEvent :> IObservable<Guid*obj>

    // 프로세서 등록
    use physicalTurtleProcessor = EventProcessors.physicalTurtleProcessor eventStream
    use graphicsProcessor = EventProcessors.graphicsProcessor eventStream
    use inkUsedProcessor = EventProcessors.inkUsedProcessor eventStream

    let handler = makeCommandHandler
    handler (move 100.0)
    handler (turn 120.0<Degrees>)
    handler (move 100.0)
    handler (turn 120.0<Degrees>)
    handler (move 100.0)
    handler (turn 120.0<Degrees>)
```

잉크 프로세서는 이동이 아닌 이벤트(Turn 등)에서도 같은 값을 중복 출력하는 문제가 있어, 이전/현재 쌍을 누적하고 값이 바뀔 때만 흘려보내도록 개선한다.

```fsharp
/// "moved" 이벤트를 듣고 집계해 총 이동 거리 추적
/// NEW! 중복 이벤트 없음!
let inkUsedProcessor (eventStream:IObservable<Guid*obj>) =

    // 새 이벤트가 발생하면 지금까지의 총 이동 거리를 누적
    let accumulate (prevDist,currDist) (ev:StateChangedEvent) =
        let newDist =
            match ev with
            | Moved dist ->
                currDist + dist
            | _ ->
                currDist
        (currDist, newDist)

    // 변화가 없는 이벤트는 None으로 바꿔 "choose"로 걸러낸다
    let changedDistanceOnly (currDist, newDist) =
        if currDist <> newDist then
            Some newDist
        else
            None

    // 옵저버블 입력을 처리하는 함수
    let subscriberFn distanceSoFar  =
        printfn "[ink used]: %0.2f" distanceSoFar

    // 전체 이벤트에서 시작
    eventStream
    // TurtleEvent만 필터링
    |> Observable.choose (function (id,ev) -> turtleFilter ev)
    // StateChangedEvent만 필터링
    |> Observable.choose stateChangedEventFilter
    // NEW! 총 거리를 쌍으로 누적
    |> Observable.scan accumulate (0.0,0.0)
    // NEW! 거리가 변하지 않았으면 걸러내기
    |> Observable.choose changedDistanceOnly
    // 처리
    |> Observable.subscribe subscriberFn
```

- 장점: 이벤트 소싱의 이점을 그대로 가진다. 상태가 필요한 핵심 로직과 부가 로직이 분리된다. 핵심 핸들러를 건드리지 않고 도메인 로직을 붙였다 뗐다 할 수 있다.
- 단점: 구현이 더 어렵다.

## 막간 2: 거북이의 역습 — 실패할 수 있는 API

마지막 두 방법을 위해 API를 바꾼다. 이동은 장벽에 부딪힐 수 있고, 색상 변경은 잉크 부족으로 실패할 수 있다. 이제 명령이 응답을 돌려준다.

```fsharp
type MoveResponse =
    | MoveOk
    | HitABarrier

type SetColorResponse =
    | ColorOk
    | OutOfInk
```

```fsharp
// 위치가 사각형 (0,0,100,100)을 벗어나면
// 위치를 안쪽으로 제한하고 HitABarrier 반환
let checkPosition position =
    let isOutOfBounds p =
        p > 100.0 || p < 0.0
    let bringInsideBounds p =
        max (min p 100.0) 0.0

    if isOutOfBounds position.x || isOutOfBounds position.y then
        let newPos = {
            x = bringInsideBounds position.x
            y = bringInsideBounds position.y }
        HitABarrier,newPos
    else
        MoveOk,position
```

```fsharp
let move log distance state =
    log (sprintf "Move %0.1f" distance)
    // 새 위치 계산
    let newPosition = calcNewPosition distance state.angle state.position
    // 범위를 벗어나면 새 위치 조정
    let moveResult, newPosition = checkPosition newPosition
    // 필요하면 선 그리기
    if state.penState = Down then
        dummyDrawLine log state.position newPosition state.color
    // 새 상태와 Move 결과 반환
    let newState = {state with position = newPosition}
    (moveResult,newState)

let setColor log color state =
    let colorResult =
        if color = Red then OutOfInk else ColorOk
    log (sprintf "SetColor %A" color)
    // 새 상태와 SetColor 결과 반환
    let newState = {state with color = color}
    (colorResult,newState)
```

## 방법 12. 모나딕 제어 흐름

방법 8의 `turtle` 워크플로를 재사용하되, 이제는 앞선 명령의 응답에 따라 다음 행동을 결정한다. 배치(방법 9)로는 불가능했던 일이다.

```fsharp
let handleMoveResponse moveResponse = turtle {
    match moveResponse with
    | Turtle.MoveOk ->
        () // 아무것도 안 함
    | Turtle.HitABarrier ->
        // 다시 시도하기 전에 90도 회전
        printfn "Oops -- hit a barrier -- turning"
        do! turn 90.0<Degrees>
    }
```

응답을 무시하는 버전과 응답에 반응하는 버전을 비교해 보자. `let!`로 응답을 받아 `do!`로 처리 로직을 잇는 것만으로 제어 흐름이 생긴다.

```fsharp
let drawShapeWithoutResponding() =
    // 일련의 명령 정의
    let t = turtle {
        let! response = move 60.0
        let! response = move 60.0
        let! response = move 60.0
        return ()
        }

    // 마지막으로 초기 상태로 모나드 실행
    runT t initialTurtleState

let drawShape() =
    // 일련의 명령 정의
    let t = turtle {
        let! response = move 60.0
        do! handleMoveResponse response

        let! response = move 60.0
        do! handleMoveResponse response

        let! response = move 60.0
        do! handleMoveResponse response
        }

    // 마지막으로 초기 상태로 모나드 실행
    runT t initialTurtleState
```

- 장점: 컴퓨테이션 표현식 덕에 배관은 감춰지고 코드가 로직에 집중한다.
- 단점: 특정 거북이 구현에 결합된다. 컴퓨테이션 표현식 구현 자체가 까다로울 수 있다.

## 방법 13. 거북이 인터프리터

거북이 프로그램의 작성과 해석을 완전히 분리하는 방식. 프로그램을 추상 구문 트리(AST) 같은 데이터 구조로 표현하고, 같은 프로그램을 서로 다른 인터프리터가 서로 다른 방식으로 실행한다.

핵심 아이디어는 "클라이언트 → 명령 → 응답 → 다음 명령"의 사슬을 타입으로 표현하는 것이다. 각 케이스가 입력 파라미터와, 응답을 받아 다음 프로그램을 돌려주는 연속(continuation) 함수를 담는다.

```fsharp
type TurtleProgram =
    //         (입력 파라미터)  (응답)
    | Stop
    | Move     of Distance   * (MoveResponse -> TurtleProgram)
    | Turn     of Angle      * (unit -> TurtleProgram)
    | PenUp    of (* 없음 *)   (unit -> TurtleProgram)
    | PenDown  of (* 없음 *)   (unit -> TurtleProgram)
    | SetColor of PenColor   * (SetColorResponse -> TurtleProgram)
```

삼각형 프로그램을 손으로 쓰면 중첩된 람다 사슬이 된다.

```fsharp
let drawTriangle =
    Move (100.0, fun response ->
    Turn (120.0<Degrees>, fun () ->
    Move (100.0, fun response ->
    Turn (120.0<Degrees>, fun () ->
    Move (100.0, fun response ->
    Turn (120.0<Degrees>, fun () ->
    Stop))))))
```

이 프로그램은 아무것도 실행하지 않는 순수한 데이터다. 실행은 인터프리터의 몫이다.

```fsharp
let rec interpretAsTurtle state program =
    let log = printfn "%s"

    match program  with
    | Stop ->
        state
    | Move (dist,next) ->
        let result,newState = Turtle.move log dist state
        let nextProgram = next result  // 다음 단계 계산
        interpretAsTurtle newState nextProgram
    | Turn (angle,next) ->
        let newState = Turtle.turn log angle state
        let nextProgram = next()       // 다음 단계 계산
        interpretAsTurtle newState nextProgram
    | PenUp next ->
        let newState = Turtle.penUp log state
        let nextProgram = next()
        interpretAsTurtle newState nextProgram
    | PenDown next ->
        let newState = Turtle.penDown log state
        let nextProgram = next()
        interpretAsTurtle newState nextProgram
    | SetColor (color,next) ->
        let result,newState = Turtle.setColor log color state
        let nextProgram = next result
        interpretAsTurtle newState nextProgram
```

```fsharp
let program = drawTriangle
let interpret = interpretAsTurtle
let initialState = Turtle.initialTurtleState
interpret initialState program |> ignore
```

같은 프로그램을 전혀 다른 방식으로 해석하는 두 번째 인터프리터 — 그림은 안 그리고 총 이동 거리만 집계한다.

```fsharp
let rec interpretAsDistance distanceSoFar program =
    let recurse = interpretAsDistance
    let log = printfn "%s"

    match program with
    | Stop ->
        distanceSoFar
    | Move (dist,next) ->
        let newDistanceSoFar = distanceSoFar + dist
        let result = Turtle.MoveOk
        let nextProgram = next result
        recurse newDistanceSoFar nextProgram
    | Turn (angle,next) ->
        let nextProgram = next()
        recurse distanceSoFar nextProgram
    | PenUp next ->
        let nextProgram = next()
        recurse distanceSoFar nextProgram
    | PenDown next ->
        let nextProgram = next()
        recurse distanceSoFar nextProgram
    | SetColor (color,next) ->
        let result = Turtle.ColorOk
        let nextProgram = next result
        recurse distanceSoFar nextProgram

let program = drawTriangle
let interpret = interpretAsDistance
let initialState = 0.0
interpret initialState program |> printfn "Total distance moved is %0.1f"
```

### 컴퓨테이션 표현식으로 프로그램 작성하기

중첩 람다를 손으로 쓰는 건 고역이므로, 타입을 제네릭으로 만들고 빌더를 정의해 `turtleProgram { ... }` 문법으로 프로그램을 조립한다.

```fsharp
type TurtleProgram<'a> =
    | Stop     of 'a
    | Move     of Distance * (MoveResponse -> TurtleProgram<'a>)
    | Turn     of Angle    * (unit -> TurtleProgram<'a>)
    | PenUp    of             (unit -> TurtleProgram<'a>)
    | PenDown  of             (unit -> TurtleProgram<'a>)
    | SetColor of PenColor * (SetColorResponse -> TurtleProgram<'a>)

let returnT x =
    Stop x

let rec bindT f inst  =
    match inst with
    | Stop x ->
        f x
    | Move(dist,next) ->
        Move(dist,next >> bindT f)
    | Turn(angle,next) ->
        Turn(angle,next >> bindT f)
    | PenUp(next) ->
        PenUp(next >> bindT f)
    | PenDown(next) ->
        PenDown(next >> bindT f)
    | SetColor(color,next) ->
        SetColor(color,next >> bindT f)

type TurtleProgramBuilder() =
    member this.Return(x) = returnT x
    member this.Bind(x,f) = bindT f x
    member this.Zero(x) = returnT ()

let turtleProgram = TurtleProgramBuilder()

let stop = fun x -> Stop x
let move dist  = Move (dist, stop)
let turn angle  = Turn (angle, stop)
let penUp  = PenUp stop
let penDown  = PenDown stop
let setColor color = SetColor (color,stop)

let handleMoveResponse log moveResponse = turtleProgram {
    match moveResponse with
    | Turtle.MoveOk ->
        ()
    | Turtle.HitABarrier ->
        log "Oops -- hit a barrier -- turning"
        let! x = turn 90.0<Degrees>
        ()
    }

let drawTwoLines log = turtleProgram {
    let! response = move 60.0
    do! handleMoveResponse log response
    let! response = move 60.0
    do! handleMoveResponse log response
    }
```

### 프리 모나드로 리팩터링

위 설계는 명령마다 bindT 케이스를 하나씩 손으로 써야 한다. "명령 집합"과 "계속/정지"라는 두 관심사를 타입 수준에서 분리하면 이것이 프리 모나드(free monad) 패턴이 된다. 재귀 부분을 `Stop | KeepGoing`이라는 범용 골격으로 뽑아내고, 명령별 로직은 `mapInstr` 하나로 국한시킨다.

```fsharp
/// 각 명령을 표현하는 타입
type TurtleInstruction<'next> =
    | Move     of Distance * (MoveResponse -> 'next)
    | Turn     of Angle    * 'next
    | PenUp    of            'next
    | PenDown  of            'next
    | SetColor of PenColor * (SetColorResponse -> 'next)

/// 거북이 프로그램을 표현하는 타입
type TurtleProgram<'a> =
    | Stop of 'a
    | KeepGoing of TurtleInstruction<TurtleProgram<'a>>

let mapInstr f inst  =
    match inst with
    | Move(dist,next) ->      Move(dist,next >> f)
    | Turn(angle,next) ->     Turn(angle,f next)
    | PenUp(next) ->          PenUp(f next)
    | PenDown(next) ->        PenDown(f next)
    | SetColor(color,next) -> SetColor(color,next >> f)

let rec interpretAsTurtle log state program =
    let recurse = interpretAsTurtle log

    match program with
    | Stop a ->
        state
    | KeepGoing (Move (dist,next)) ->
        let result,newState = Turtle.move log dist state
        let nextProgram = next result
        recurse newState nextProgram
    | KeepGoing (Turn (angle,next)) ->
        let newState = Turtle.turn log angle state
        let nextProgram = next
        recurse newState nextProgram
```

- 장점: 프로그램 흐름과 구현이 완전히 분리된다. 실행 전에 AST를 최적화할 수 있다. 프리 모나드 방식이면 골격 코드가 최소화된다.
- 단점: 이해하기 어렵다. 연산 집합이 작을 때 가장 잘 맞는다. AST가 커지면 비효율적일 수 있다.

## 총정리: 사용된 기법들

13가지 구현을 관통한 기법을 모으면 함수형 설계의 도구 상자가 된다.

- 테스트하기 쉬운 순수 무상태 함수
- 유연한 합성을 위한 부분 적용
- 인터페이스를 구현하는 객체 표현식
- 에러 처리를 위한 Result 타입 (철도 지향 프로그래밍)
- 어플리커티브 리프팅
- 다양한 상태 관리 전략: 가변 필드, 명시적 상태 전달, 상태 모나드, 배치 커맨드, 이벤트 소싱, 인터프리터
- 함수를 타입으로 감싸기
- 컴퓨테이션 표현식
- 모나딕 함수 연결
- 행위를 데이터 구조로 표현하기
- 데이터 중심의 분리 프로토콜
- 락 없는 에이전트 처리
- 계산의 구성과 실행 분리
- 상태 재구성을 위한 이벤트 소싱
- 이벤트 스트림과 함수형 반응형 프로그래밍

같은 요구사항이라도 관점(상태를 어디에 둘 것인가, 무엇을 데이터로 표현할 것인가, 구성과 실행을 언제 분리할 것인가)에 따라 전혀 다른 설계가 나온다는 것이 이 시리즈의 핵심 교훈이다. 원문에는 보너스로 방법 14(추상 데이터 타입)와 방법 15(케이퍼빌리티 기반 설계)를 다루는 확장편도 있다.
