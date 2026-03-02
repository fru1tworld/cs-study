# 사용자 정의 명령으로 Cargo 확장하기

Cargo는 수정 없이 새 하위 명령으로 확장할 수 있도록 설계되었습니다. `$PATH`에 있는 바이너리 이름이 `cargo-something`이면 `cargo something`을 실행하여 마치 Cargo 하위 명령인 것처럼 실행할 수 있습니다. 이와 같은 사용자 정의 명령은 `cargo --list`를 실행할 때도 나열됩니다. `cargo install`을 사용하여 확장을 설치한 다음 내장 Cargo 도구처럼 실행할 수 있는 것은 Cargo 디자인의 매우 편리한 이점입니다!

## 요약

Cargo와 [crates.io](https://crates.io/)로 코드를 공유하는 것은 Rust 생태계를 다양한 작업에 유용하게 만드는 부분입니다. Rust의 표준 라이브러리는 작고 안정적이지만, 크레이트는 언어와 다른 타임라인으로 공유, 사용 및 개선하기 쉽습니다. [crates.io](https://crates.io/)에서 유용한 코드를 공유하는 것을 부끄러워하지 마세요; 다른 누군가에게도 유용할 가능성이 높습니다!
