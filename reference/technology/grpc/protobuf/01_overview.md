# Protocol Buffers 개요

> 원본: https://protobuf.dev/overview/

---

## 목차

1. [Protocol Buffers란](#protocol-buffers란)
2. [동작 원리](#동작-원리)
3. [.proto 정의 예제](#proto-정의-예제)
4. [장점](#장점)
5. [JSON / XML 대비](#json--xml-대비)
6. [지원 언어](#지원-언어)
7. [문법 버전 (proto2 / proto3 / Editions)](#문법-버전-proto2--proto3--editions)
8. [사용하기 적합한 경우와 그렇지 않은 경우](#사용하기-적합한-경우와-그렇지-않은-경우)
9. [전형적인 사용 흐름](#전형적인-사용-흐름)

---

## Protocol Buffers란

- Protocol Buffers(흔히 protobuf로 줄여 부름): 언어 중립적(language-neutral)·플랫폼 중립적(platform-neutral)이며 확장 가능한 구조화 데이터 직렬화(serialization) 메커니즘
- JSON과 비슷한 역할 → 더 작고 빠름, 각 언어에 맞는 네이티브(native) 바인딩 코드를 자동 생성
- 데이터 구조를 `.proto` 파일로 정의 → 그 정의로부터 여러 언어용 코드 생성 → 구조화된 데이터를 바이너리(binary) 형태로 읽고 쓰기 가능
- Google에서는 서버 간 통신과 데이터 저장에 가장 널리 쓰이는 데이터 형식

---

## 동작 원리

Protocol Buffers는 크게 세 단계로 동작.

1. 스키마 정의(Schema Definition): 개발자가 메시지(message) 구조를 담은 `.proto` 파일 작성
2. 코드 생성(Code Generation): 프로토콜 컴파일러(`protoc`)가 빌드 시점에 각 언어용 데이터 접근 클래스/구조체 생성
3. 직렬화 / 역직렬화(Serialization / Deserialization): 생성된 코드의 메서드로 데이터를 바이너리 형식으로 인코딩(encoding)하거나 다시 디코딩(decoding)

`스키마 → 코드 생성 → (역)직렬화` 흐름 → 데이터 구조와 인코딩 규칙을 한 곳에서 정의하고 여러 언어·시스템이 공유

---

## .proto 정의 예제

가장 기본적인 메시지 정의 예시.

```proto
edition = "2023";

message Person {
  string name = 1;
  int32 id = 2;
  string email = 3;
}
```

- `message`는 하나의 구조화된 레코드(record)를 정의
- 각 필드(field)에는 이름, 타입과 함께 고유한 필드 번호(`= 1`, `= 2` ...) 부여 → 이 번호가 바이너리 인코딩에서 필드를 식별

이 정의로부터 컴파일러가 각 언어의 접근자(accessor)와 직렬화 메서드 생성.

---

## 장점

- 작은 크기와 빠른 파싱(parsing): 바이너리 형식 → 텍스트 기반 형식보다 데이터가 작고 처리 속도 빠름
- 언어 간 호환성(Cross-language Compatibility): 예를 들어 Java 프로그램이 직렬화한 데이터를 Python 애플리케이션이 그대로 읽기 가능
- 하위/상위 호환성(Backward / Forward Compatibility): 정해진 갱신 규칙을 따르면 구버전 코드로 신버전 데이터를 읽거나 그 반대도 안전하게 처리 가능
- 자동 코드 생성: 직렬화 로직을 직접 작성하지 않아도 됨 → 개발 부담 감소, 실수 감소

---

## JSON / XML 대비

- 형식
  - Protocol Buffers: 바이너리
  - JSON / XML: 텍스트
- 크기
  - Protocol Buffers: 작음
  - JSON / XML: 상대적으로 큼
- 파싱 속도
  - Protocol Buffers: 빠름
  - JSON / XML: 느림
- 스키마
  - Protocol Buffers: 필수(.proto)
  - JSON / XML: 선택적
- 사람이 읽기
  - Protocol Buffers: 어려움(별도 도구 필요)
  - JSON / XML: 쉬움
- 코드 생성
  - Protocol Buffers: 네이티브 바인딩 자동 생성
  - JSON / XML: 일반적으로 수동 매핑

JSON과 달리 protobuf는 데이터를 해석하려면 대응되는 `.proto` 파일 필요. 대신 페이로드(payload)가 작고 파싱이 빠름.

---

## 지원 언어

- 컴파일러가 직접 지원: C++, C#, Java, Kotlin, Objective-C, PHP, Python, Ruby
- 플러그인(plugin)으로 지원: Dart, Go
- 서드파티(third-party): 그 외 다양한 언어가 GitHub 프로젝트로 지원

> Go의 경우 `protoc-gen-go` 플러그인을 통해 코드 생성(`google.golang.org/protobuf`).

---

## 문법 버전 (proto2 / proto3 / Editions)

- proto2: 원본 문법, `optional` 등으로 필드 존재 여부(presence) 명시적으로 다룸
- proto3: 단순화된 문법, 기본적으로 암시적(implicit) presence 사용
- Editions (2023, 2024): proto2/proto3를 대체하는 현대적 방식, `edition = "..."` 선언 → features로 동작을 세밀하게 설정

새 프로젝트는 proto3 또는 Editions 사용 권장.

---

## 사용하기 적합한 경우와 그렇지 않은 경우

적합한 경우

- 타입이 있는 구조화 데이터를 직렬화해야 할 때
- 분산 시스템(distributed system)에서 서버 간 통신할 때(특히 gRPC)
- 장기 저장 및 언어 간 데이터 교환이 필요할 때

적합하지 않은 경우

- 메시지 크기가 수 메가바이트(MB)를 크게 넘어가면 메모리 비효율 발생 가능
- 비압축 형식이므로 대규모 숫자 배열에는 부적합 가능
- 데이터를 해석하려면 항상 대응되는 `.proto` 파일 필요
- 동일한 데이터에 대해 여러 가지 바이너리 표현이 가능 → 바이트 단위 동일성 비교 용도에는 부적합

---

## 전형적인 사용 흐름

```bash
# 1. .proto 파일 작성 (스키마 정의)
# 2. protoc로 코드 생성
protoc --go_out=. --go_opt=paths=source_relative addressbook.proto

# 3. 애플리케이션 코드에서 생성된 타입 사용 (직렬화/역직렬화)
```

```go
// 직렬화
data, err := proto.Marshal(person)

// 역직렬화
p := &pb.Person{}
err = proto.Unmarshal(data, p)
```
