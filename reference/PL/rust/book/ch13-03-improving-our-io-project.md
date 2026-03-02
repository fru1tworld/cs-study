# 반복자를 사용하여 I/O 프로젝트 개선하기

반복자에 대한 새로운 지식을 바탕으로 12장의 I/O 프로젝트를 반복자를 사용하여 코드의 여러 부분을 더 명확하고 간결하게 만들어 개선할 수 있습니다. `Config::build` 함수와 `search` 함수의 구현을 반복자가 어떻게 개선할 수 있는지 살펴보겠습니다.

## 반복자를 사용하여 `clone` 제거하기

Listing 12-6에서 `String` 값의 슬라이스를 받아 슬라이스에 인덱싱하고 값을 복제하여 `Config` 구조체가 해당 값을 소유하도록 `Config` 구조체의 인스턴스를 만드는 코드를 추가했습니다. Listing 13-17에서 Listing 12-23에서와 같이 `Config::build` 함수의 구현을 다시 보여줍니다:

```rust
impl Config {
    pub fn build(args: &[String]) -> Result<Config, &'static str> {
        if args.len() < 3 {
            return Err("인수가 충분하지 않습니다");
        }

        let query = args[1].clone();
        let file_path = args[2].clone();

        let ignore_case = env::var("IGNORE_CASE").is_ok();

        Ok(Config {
            query,
            file_path,
            ignore_case,
        })
    }
}
```

*Listing 13-17: Listing 12-23의 `Config::build` 함수 재현*

당시에 우리는 비효율적인 `clone` 호출에 대해 걱정하지 말고 나중에 제거하겠다고 말했습니다. 이제 그때가 되었습니다!

여기서 `clone`이 필요했던 이유는 매개변수 `args`에 `String` 요소가 있는 슬라이스가 있지만 `build` 함수는 `args`를 소유하지 않기 때문입니다. `Config` 인스턴스의 소유권을 반환하려면 `Config`의 `query` 및 `file_path` 필드에서 값을 복제해야 `Config` 인스턴스가 해당 값을 소유할 수 있습니다.

반복자에 대한 새로운 지식을 통해 슬라이스를 빌리는 대신 반복자의 소유권을 인수로 받도록 `build` 함수를 변경할 수 있습니다. 슬라이스의 길이를 확인하고 특정 위치에 인덱싱하는 코드 대신 반복자 기능을 사용합니다. 이렇게 하면 반복자가 값에 액세스하기 때문에 `Config::build` 함수가 수행하는 작업이 명확해집니다.

`Config::build`가 반복자의 소유권을 가져가고 빌린 인덱싱 작업을 사용하지 않으면 `clone`을 호출하고 새 할당을 만드는 대신 반복자의 `String` 값을 `Config`로 이동할 수 있습니다.

### 반환된 반복자 직접 사용하기

I/O 프로젝트의 *src/main.rs* 파일을 열면 다음과 같이 보일 것입니다:

```rust
fn main() {
    let args: Vec<String> = env::args().collect();

    let config = Config::build(&args).unwrap_or_else(|err| {
        eprintln!("인수 파싱 문제: {err}");
        process::exit(1);
    });

    // --snip--
}
```

먼저 Listing 12-24에 있던 `main` 함수의 시작 부분을 Listing 13-18의 코드로 변경합니다. 이번에는 반복자를 사용합니다. `Config::build`도 업데이트할 때까지 컴파일되지 않습니다.

```rust
fn main() {
    let config = Config::build(env::args()).unwrap_or_else(|err| {
        eprintln!("인수 파싱 문제: {err}");
        process::exit(1);
    });

    // --snip--
}
```

*Listing 13-18: `Config::build`에 `env::args`의 반환 값 전달하기*

`env::args` 함수는 반복자를 반환합니다! 반복자 값을 벡터로 수집한 다음 슬라이스를 `Config::build`에 전달하는 대신, 이제 `env::args`에서 반환된 반복자의 소유권을 `Config::build`에 직접 전달합니다.

다음으로 `Config::build`의 정의를 업데이트해야 합니다. I/O 프로젝트의 *src/lib.rs* 파일에서 `Config::build`의 시그니처를 Listing 13-19처럼 변경해 보겠습니다. 함수 본문을 업데이트해야 하므로 아직 컴파일되지 않습니다.

```rust
impl Config {
    pub fn build(
        mut args: impl Iterator<Item = String>,
    ) -> Result<Config, &'static str> {
        // --snip--
    }
}
```

*Listing 13-19: 반복자를 기대하도록 `Config::build`의 시그니처 업데이트하기*

`env::args` 함수의 표준 라이브러리 문서는 반환하는 반복자의 타입이 `std::env::Args`이고, 해당 타입이 `Iterator` 트레이트를 구현하고 `String` 값을 반환함을 보여줍니다.

`Config::build` 함수의 시그니처를 업데이트하여 `args` 매개변수가 `&[String]` 대신 트레이트 바운드 `impl Iterator<Item = String>`이 있는 제네릭 타입을 갖도록 했습니다. 11장의 "매개변수로서의 트레이트" 섹션에서 논의한 `impl Trait` 구문의 사용은 `args`가 `Iterator` 타입을 구현하고 `String` 항목을 반환하는 모든 타입이 될 수 있음을 의미합니다.

`args`의 소유권을 가져가고 반복하여 `args`를 변형할 것이기 때문에 `args` 매개변수의 지정에 `mut` 키워드를 추가하여 가변으로 만들 수 있습니다.

### 인덱싱 대신 `Iterator` 트레이트 메서드 사용하기

다음으로 `Config::build`의 본문을 수정합니다. `args`가 `Iterator` 트레이트를 구현하기 때문에 `next` 메서드를 호출할 수 있다는 것을 알고 있습니다! Listing 13-20은 Listing 12-23의 코드를 `next` 메서드를 사용하도록 업데이트합니다:

```rust
impl Config {
    pub fn build(
        mut args: impl Iterator<Item = String>,
    ) -> Result<Config, &'static str> {
        args.next();

        let query = match args.next() {
            Some(arg) => arg,
            None => return Err("쿼리 문자열을 받지 못했습니다"),
        };

        let file_path = match args.next() {
            Some(arg) => arg,
            None => return Err("파일 경로를 받지 못했습니다"),
        };

        let ignore_case = env::var("IGNORE_CASE").is_ok();

        Ok(Config {
            query,
            file_path,
            ignore_case,
        })
    }
}
```

*Listing 13-20: 반복자 메서드를 사용하도록 `Config::build`의 본문 변경하기*

`env::args` 반환 값의 첫 번째 값은 프로그램 이름입니다. 그것을 무시하고 다음 값을 얻고 싶으므로 먼저 `next`를 호출하고 반환 값으로 아무것도 하지 않습니다. 그런 다음 `next`를 호출하여 `Config`의 `query` 필드에 넣을 값을 얻습니다. `next`가 `Some`을 반환하면 `match`를 사용하여 값을 추출합니다. `None`을 반환하면 인수가 충분하지 않다는 의미이므로 `Err` 값으로 일찍 반환합니다. `file_path` 값에 대해서도 동일하게 수행합니다.

## 반복자 어댑터로 코드 더 명확하게 만들기

I/O 프로젝트의 `search` 함수에서도 반복자를 활용할 수 있습니다. Listing 12-19에 있는 그대로 Listing 13-21에 재현되어 있습니다:

```rust
pub fn search<'a>(query: &str, contents: &'a str) -> Vec<&'a str> {
    let mut results = Vec::new();

    for line in contents.lines() {
        if line.contains(query) {
            results.push(line);
        }
    }

    results
}
```

*Listing 13-21: Listing 12-19의 `search` 함수 구현*

반복자 어댑터 메서드를 사용하여 이 코드를 더 간결하게 작성할 수 있습니다. 이렇게 하면 가변 중간 `results` 벡터도 피할 수 있습니다. 함수형 프로그래밍 스타일은 코드를 더 명확하게 하기 위해 가변 상태의 양을 최소화하는 것을 선호합니다. 가변 상태를 제거하면 검색이 병렬로 수행되도록 하는 향후 개선이 가능할 수 있습니다. `results` 벡터에 대한 동시 액세스를 관리할 필요가 없기 때문입니다. Listing 13-22는 이 변경 사항을 보여줍니다:

```rust
pub fn search<'a>(query: &str, contents: &'a str) -> Vec<&'a str> {
    contents
        .lines()
        .filter(|line| line.contains(query))
        .collect()
}
```

*Listing 13-22: `search` 함수 구현에서 반복자 어댑터 메서드 사용하기*

`search` 함수의 목적은 `query`를 포함하는 `contents`의 모든 줄을 반환하는 것임을 기억하세요. Listing 13-16의 `filter` 예제와 유사하게, 이 코드는 `filter` 어댑터를 사용하여 `line.contains(query)`가 `true`를 반환하는 줄만 유지합니다. 그런 다음 일치하는 줄을 `collect`로 다른 벡터에 수집합니다. 훨씬 더 간단합니다! `search_case_insensitive` 함수에서도 반복자 메서드를 사용하도록 동일한 변경을 자유롭게 해보세요.

## 루프와 반복자 중 선택하기

다음 논리적 질문은 자신의 코드에서 어떤 스타일을 선택해야 하고 그 이유가 무엇인지입니다: Listing 13-21의 원래 구현 또는 Listing 13-22의 반복자를 사용하는 버전. 대부분의 Rust 프로그래머는 반복자 스타일을 선호합니다. 처음에는 익숙해지기가 좀 더 어렵지만, 다양한 반복자 어댑터와 그들이 수행하는 작업에 대한 감각을 얻으면 반복자가 이해하기 더 쉬울 수 있습니다. 루핑과 새 벡터 생성의 다양한 부분을 만지작거리는 대신, 코드는 루프의 고수준 목적에 집중합니다. 이것은 일부 일반적인 코드를 추상화하여 이 코드에 고유한 개념, 즉 반복자의 각 요소가 통과해야 하는 필터링 조건을 더 쉽게 볼 수 있게 합니다.

그러나 두 구현이 정말 동등할까요? 직관적인 가정은 더 저수준 루프가 더 빠를 것이라는 것입니다. 성능에 대해 이야기해 봅시다.
