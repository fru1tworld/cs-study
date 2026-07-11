# Effective ML 다시 보기

> 원문: [Effective ML Revisited](https://blog.janestreet.com/effective-ml-revisited/)
> 저자: Yaron Minsky | 번역일: 2026-07-10

하버드가 올해도 1학년 학생들에게 OCaml을 가르치고 있고, Greg Morrissett이 올해도 나를 초청 강연자로 불러 주었다. [작년](https://blog.janestreet.com/effective-ml-video/)에 같은 수업에서 했던 Effective ML 강연을 조금 다듬어서 다시 했다.

OCaml은 미국에서 교육용 언어로 실제로 자리를 잡아가는 듯하다. 하버드, 펜실베이니아 대학, 코넬 모두 정규 커리큘럼의 일부로 학부생에게 OCaml을 가르친다. SML은 물론 CMU에서 오래전부터 가르쳐 왔고, 브라운과 노스이스턴은 Scheme/Racket을 가르친다.

여담이지만 수업 규모가 상당히 커져서 cs51에 200명 넘는 학생이 등록했다. 듣자 하니 성장분의 상당 부분은 영화 "소셜 네트워크"를 보고 컴퓨터 과학을 공부하겠다고 마음먹은 사람들 덕분이라고 한다….

지난번 강연 때 코드 스니펫을 달라는 요청이 있어서, (업데이트한) 스니펫을 아래에 실었다. 이 코드의 맥락을 이해하려면 작년 [강연](https://blog.janestreet.com/effective-ml-video/)을 보시라!

### 균일한 인터페이스를 사용하라

```ocaml
module type Comparable = sig
  type t

  val compare : t -> t -> [ `Lt | `Eq | `Gt ]

  val ( >= ) : t -> t -> bool
  val ( <= ) : t -> t -> bool
  val ( = ) : t -> t -> bool
  val ( > ) : t -> t -> bool
  val ( < ) : t -> t -> bool
  val ( <> ) : t -> t -> bool
  val min : t -> t -> t
  val max : t -> t -> t

  module Map : Core_map.S with type key = t
  module Set : Core_set.S with type elt = t
end
```

```ocaml
module Char : sig
  type t
  include Comparable with type t := t
  include Stringable with type t := t
  include Hashable with type t := t
end
```

### 잘못된 상태를 표현 불가능하게 만들어라

전:

```ocaml
type connection_state =
| Connecting
| Connected
| Disconnected

type connection_info = {
  state: connection_state;
  server: Inet_addr.t;
  last_ping_time: Time.t option;
  last_ping_id: int option;
  session_id: string option;
  when_initiated: Time.t option;
  when_disconnected: Time.t option;
}
```

후:

```ocaml
type connecting = { when_initiated: Time.t; }
type connected = { last_ping : (Time.t * int) option;
          session_id: string; }
type disconnected = { when_disconnected: Time.t; }

type connection_state =
| Connecting of connecting
| Connected of connected
| Disconnected of disconnected

type connection_info = {
  state : connection_state;
  server: Inet_addr.t;
}
```

### 완전성 검사를 살려서 코딩하라

```ocaml
type message = | Order of Order.t
               | Cancel of Order_id.t
               | Exec of Execution.t

let position_change m =
  match m with
  | Exec e ->
    let dir = Execution.dir e in
    Dir.sign dir * Execution.quantity e
  | _ -> 0
```

### 모듈은 적게 열어라

전:

```ocaml
open Core.Std
open Command
open Flag

type config = { exit_code: int;
        message: string option; }

let command =
  let default_config = { exit_code = 0; message = None } in
  let flags =
    [ int "-r" (fun cfg v -> { cfg with exit_code = v });
      string "-m" (fun cfg v -> { cfg with message = v });
    ]
  in
  let main cfg =
    Option.iter cfg.message (fun x -> eprintf "%s\n" x);
    cfg.exit_code
  in
  create ~summary:"does nothing, successfully"
    ~default_config ~flags ~main

let () = run command
```

후:

```ocaml
open Core.Std

type config = { exit_code: int;
        message: string option; }

let command =
  let default_config = { exit_code = 0; message = None } in
  let flags =
    let module F = Command.Flag in
    [ F.int "-r" (fun cfg v -> { cfg with exit_code = v });
    F.string "-m" (fun cfg v -> { cfg with message = v });
    ]
    in
    let main cfg =
      Option.iter cfg.message (fun x -> eprintf "%s\n" x);
      cfg.exit_code
    in
    Command.create ~summary:"does nothing, successfully"
    ~default_config ~flags ~main

let () = Command.run command
```

### 흔한 오류를 눈에 띄게 만들어라

```ocaml
module List : sig
  type 'a t

  ....

  val find : 'a t -> ('a -> bool) -> 'a option
  val find_exn : 'a t -> ('a -> bool) -> 'a

  val hd : 'a t -> 'a option
  val hd_exn : 'a t -> 'a

  val reduce : 'a t -> ('a -> 'a -> 'a) -> 'a option
  val reduce_exn : 'a t -> ('a -> 'a -> 'a) -> 'a

  val fold : 'a t -> init : 'b -> ('a -> 'b -> 'b) -> 'b

  .....

end
```
