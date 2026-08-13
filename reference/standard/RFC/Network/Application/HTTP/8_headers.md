# HTTP 헤더: 구조화 필드와 Forwarded

## RFC 8941 - HTTP를 위한 구조화된 필드 값(Structured Field Values)

> 발행: 2021년 2월 (M. Nottingham, P-H. Kamp)
> 폐기: RFC 9651로 대체 (하위 호환, Date/DisplayString 타입 추가)

### 개요

- "구조화된 필드(Structured Fields)"·"구조화된 헤더"·"구조화된 트레일러"로 불리는 HTTP 헤더/트레일러 필드를 쉽고 안전하게 정의·처리하기 위한 데이터 타입·알고리즘 정의
- 전통적인 HTTP 필드 값보다 제한적인 공통 구문을 쓰려는 새 HTTP 필드 명세용

### 1. 소개

- 새 HTTP 헤더(트레일러) 필드 구문 명세는 부담스러운 작업 → RFC7231 8.3.1절 지침이 있어도 결정 사항·함정 다수
- 필드 정의 후에는 값마다 공통 구문처럼 보이는 것을 조금씩 다르게 처리 → 맞춤형 파서/직렬화기를 매번 작성해야 함
- 이 문서: 새 HTTP 필드 값 정의에 쓸 공통 데이터 구조 도입 → 일반적·추상적 모델 + HTTP([RFC7230]) 헤더/트레일러 필드에서의 구체적 직렬화 방식 정의
- "구조화된 헤더"/"구조화된 트레일러"(둘 다 가능하면 "구조화된 필드")로 정의된 HTTP 필드는 이 명세의 타입으로 구문·기본 처리 규칙 정의 → 명세 작성과 구현 처리 모두 단순화
- 향후 HTTP 버전은 추상 모델의 대안 직렬화 정의 가능 → 그 모델을 쓰는 필드는 재정의 없이 더 효율적으로 전송 가능
- 이 문서의 목표는 기존 HTTP 필드 구문 재정의가 아님 → 명시적으로 채택(opt into)한 필드에만 적용되도록 의도
- 2절: 구조화된 필드 명세 방법
- 3절: 구조화된 필드에 쓸 수 있는 추상 데이터 타입
- 4절: 위 타입을 HTTP 필드 값으로 직렬화·파싱하는 알고리즘

#### 1.1 의도적으로 엄격한 처리

- 단계별 알고리즘으로 엄격한 파싱/직렬화 동작 정의 → 정의된 유일한 오류 처리는 연산 전체 실패
- 목적: 충실한 구현과 상호운용성 장려 → 입력에 관대하게 대응하려는 시도는 오히려 상호운용성 악화 (다른 구현체에도 유사하지만 미묘하게 다른 우회책 구현 압력 유발)
- 엄격한 처리 = 의도적 기능 → 비순응(non-conformant) 입력을 생산자가 조기 발견·수정 가능 → 상호운용성·보안 문제 예방
- 한 필드에 여러 당사자(중개자, 송신자 내부 컴포넌트 등)가 값을 덧붙이는 경우 → 한 당사자의 오류가 전체 필드 값 파싱 실패로 이어질 가능성 높음

#### 1.2 표기 규약

- MUST/MUST NOT/REQUIRED/SHALL/SHALL NOT/SHOULD/SHOULD NOT/RECOMMENDED/NOT RECOMMENDED/MAY/OPTIONAL: 대문자 표기 시 BCP14([RFC2119], [RFC8174]) 정의 따름
- ABNF: [RFC5234] 표기 사용, VCHAR·SP·DIGIT·ALPHA·DQUOTE 규칙 포함, [RFC7230]의 tchar·OWS 규칙도 포함
- HTTP 필드 파싱: 구현체는 알고리즘과 구별 불가능한 동작 필요(MUST) → 알고리즘과 ABNF 불일치 시 알고리즘이 우선
- HTTP 필드 직렬화: ABNF는 기대되는 와이어 표현, 알고리즘은 권장 생성 방식 → 출력이 4.2절 파싱 알고리즘에 의해 올바르게 처리되는 한 명세된 동작에서 벗어날 수 있음(MAY)

### 2. 새로운 구조화된 필드 정의하기

- HTTP 필드를 구조화된 필드로 명세하려면 저자가 다음을 수행:
  - 이 명세를 규범적으로 참조 → 수신자·생성자가 요구사항 적용 사실을 알아야 함
  - 필드 종류 식별: 구조화된 헤더(헤더 섹션 전용, 일반적) / 구조화된 트레일러(트레일러 섹션 전용) / 구조화된 필드(둘 다)
  - 필드 값 타입 명세: 리스트(3.1) · 딕셔너리(3.2) · 아이템(3.3) 중 하나
  - 필드 값의 의미론(semantics) 정의
  - 필드 값에 대한 추가 제약 조건과, 위반 시 결과 명세
- 일반적으로 필드 정의는 최상위 타입(리스트/딕셔너리/아이템)을 명세한 후 허용 타입·제약 정의
  - 예: 리스트로 정의된 헤더는 모든 멤버가 정수일 수도, 타입 혼합일 수도 있음
  - 예: 아이템으로 정의된 헤더는 문자열만 허용하거나, 추가로 "Q"로 시작하는 문자열·소문자 문자열만 허용 가능
  - 내부 리스트(3.1.1)는 필드 정의가 명시적으로 허용할 때만 유효
- 파싱 실패 시 전체 필드 무시(4.2절 참고) → 대부분 상황에서 필드별 제약 위반도 동일 효과여야 함
  - 예: 아이템이며 정수여야 하는 헤더가 문자열을 받으면 필드는 기본적으로 무시 → 다른 오류 처리가 필요하면 명시적으로 명세 필요
- 아이템·내부 리스트는 파라미터를 확장성 메커니즘으로 허용 → 필요 시 값이 더 많은 정보를 수용하도록 확장 가능
  - 전방 호환성(forward compatibility) 보존 위해 인식되지 않은 파라미터 존재를 오류 조건으로 정의하는 것은 비권장
- 확장성을 향후에도 이용 가능하게 하고 소비자가 완전한 파서 구현을 쓰도록 장려하기 위해, 필드 정의는 송신자가 "그리스(grease)" 파라미터를 추가하도록 명세 가능
  - 정의된 패턴에 맞는 모든 파라미터를 이 용도로 예약 → 일부 비율의 요청에 전송 장려 → 파라미터를 고려하지 않는 파서 작성을 억제
- 딕셔너리 사용 명세도 알려지지 않은 멤버(및 그와 연관된 값·타입)를 무시하도록 요구함으로써 전방 호환성 확보 가능 → 후속 명세가 멤버를 추가하고 제약을 명세 가능
- 구조화된 필드 확장: 확장을 이해하는 수신자가 그 확장이 정의하는 값 제약이 충족되지 않으면 전체 필드 값을 무시하도록 요구 가능
- 필드 정의는 이 명세의 요구사항을 완화할 수 없음 → 완화 시 범용 소프트웨어에 의한 처리가 불가능해지기 때문
  - 필드 정의는 추가 제약만 가능 (예: 정수/소수의 수치 범위, 문자열/토큰 형식, 딕셔너리 값 허용 타입, 리스트 아이템 수 제약)
  - 이 명세는 전체 필드 값에만 사용 가능, 일부에는 사용 불가
- 이 명세는 구현체가 지원할 구조의 길이·개수 최소값 정의 → 대부분 최대 크기는 미명세
  - 저자는 HTTP 구현체가 개별 필드 크기·필드 총 개수·전체 헤더 또는 트레일러 섹션 크기에 제한을 부과한다는 점 인지 필요
- 필드 이름은 "구조화된 헤더/트레일러/필드 이름"으로, 값은 "구조화된 헤더/트레일러/필드 값"으로 지칭 가능
  - 필드 정의는 이 명세의 "sf-" 접두 ABNF 규칙 사용 권장, 그 외 규칙은 필드 정의용으로 의도되지 않음

예시: 가상의 Foo-Example 헤더 필드 명세

```
42.  Foo-Example 헤더

Foo-Example HTTP 헤더 필드는 메시지가 얼마나 많은 Foo를 가지고 있는지에
관한 정보를 전달한다.

Foo-Example은 아이템 구조화된 헤더 [RFC8941]다. 값은 정수(Integer,
[RFC8941]의 3.3.1절)여야 함(MUST). ABNF:

      Foo-Example = sf-integer

값은 메시지 내 Foo 양을 나타내며, 0과 10 사이(양 끝값 포함)여야 함
(MUST). 다른 값은 전체 헤더 필드가 무시되도록 해야 함(MUST).

다음 파라미터가 정의됨:
*  키가 "foourl"이고 값이 문자열(String, [RFC8941]의 3.3.3절)인
   파라미터로, 메시지에 대한 Foo URL 전달. 처리 요구사항은 아래 참조.

"foourl"은 URI 참조(URI-reference, [RFC3986]의 4.1절) 포함. 값이
유효한 URI 참조가 아니면 전체 헤더 필드가 무시되어야 함(MUST). 값이
상대 참조(relative reference, [RFC3986]의 4.2절)이면 사용 전 해석
([RFC3986]의 5절)되어야 함(MUST).

예:

      Foo-Example: 2; foourl="https://foo.example.com/"
```

### 3. 구조화된 데이터 타입

- 구조화된 필드용 추상 타입 정의, ABNF는 HTTP 필드 값 와이어 형식 표현
- 요약
  - HTTP 필드가 정의될 수 있는 최상위 타입 3가지: 리스트 · 딕셔너리 · 아이템
  - 리스트·딕셔너리는 컨테이너 → 멤버는 아이템 또는 내부 리스트(그 자체로 아이템 배열)
  - 아이템·내부 리스트는 키/값 쌍으로 파라미터화 가능

#### 3.1 리스트(Lists)

- 0개 이상 멤버로 이루어진 배열, 각 멤버는 아이템(3.3) 또는 내부 리스트(3.1.1) → 둘 다 파라미터화(3.1.2) 가능

```
sf-list       = list-member *( OWS "," OWS list-member )
list-member   = sf-item / inner-list
```

- 각 멤버는 쉼표와 선택적 공백으로 구분. 예: `Example-List: sugar, tea, rum`
- 빈 리스트는 필드를 전혀 직렬화하지 않음으로 표시 → 리스트로 정의된 필드는 기본 빈 값을 가짐을 함의
- [RFC7230] 3.2.2절에 따라 동일 헤더/트레일러 섹션의 여러 줄에 멤버 분할 가능
  - 다음은 동등: `Example-List: sugar, tea, rum` / `Example-List: sugar, tea` + `Example-List: rum`
  - 단, 개별 멤버는 여러 줄에 걸쳐 분할 불가 (4.2절 참고)
- 파서는 최소 1024개 멤버를 포함하는 리스트를 지원해야 함(MUST) → 필드 명세는 개별 리스트 값의 타입·개수 제약 가능

##### 3.1.1 내부 리스트(Inner Lists)

- 0개 이상의 아이템(3.3)으로 이루어진 배열 → 개별 아이템·내부 리스트 자체 모두 파라미터화(3.1.2) 가능

```
inner-list    = "(" *SP [ sf-item *( 1*SP sf-item ) *SP ] ")"
                parameters
```

- 둘러싸는 괄호로 표시, 값들은 하나 이상의 공백으로 구분
  - 예: `Example-List: ("foo" "bar"), ("baz"), ("bat" "one"), ()` — 마지막 멤버는 빈 내부 리스트
  - 두 수준 모두 파라미터를 갖는 예: `Example-List: ("foo"; a=1;b=2);lvl=5, ("bar" "baz");lvl=1`
- 파서는 최소 256개 멤버를 포함하는 내부 리스트를 지원해야 함(MUST) → 필드 명세는 개별 내부 리스트 멤버의 타입·개수 제약 가능

##### 3.1.2 파라미터(Parameters)

- 아이템(3.3) 또는 내부 리스트(3.1.1)와 연관된 키-값 쌍의 순서 있는 맵(ordered map)
  - 키는 그것이 나타나는 파라미터 범위 내에서 고유
  - 값은 베어 아이템(bare item) — 그 자체는 파라미터화 불가 (3.3 참고)
- 구현체는 인덱스·키 양쪽 모두로 파라미터 접근을 제공해야 함(MUST) → 명세는 둘 중 어느 접근 방식이든 사용 가능(MAY)

```
parameters    = *( ";" *SP parameter )
parameter     = param-key [ "=" param-value ]
param-key     = key
key           = ( lcalpha / "*" )
                *( lcalpha / DIGIT / "_" / "-" / "." / "*" )
lcalpha       = %x61-7A ; a-z
param-value   = bare-item
```

- 파라미터는 직렬화된 순서대로 정렬, 파라미터 키는 대문자 포함 불가
- 파라미터는 아이템/내부 리스트, 그리고 다른 파라미터들과 세미콜론으로 구분
  - 예: `Example-List: abc;a=1;b=2; cde_456, (ghi;jk=4 l);q="9";r=w`
- 값이 불리언(3.3.6) true인 파라미터는 직렬화 시 값을 생략해야 함(MUST)
  - 예: `Example-Integer: 1; a; b=?0` — "a" 파라미터는 true, "b" 파라미터는 false
  - 이 요구사항은 직렬화에만 적용 → 파서는 파라미터에서 true 값이 나타날 때 여전히 올바르게 처리해야 함
- 파서는 아이템/내부 리스트 하나당 최소 256개 파라미터, 최소 64개 문자의 파라미터 키를 지원해야 함(MUST) → 필드 명세는 순서·값 타입 제약 가능

#### 3.2 딕셔너리(Dictionaries)

- 키-값 쌍의 순서 있는 맵, 키는 짧은 텍스트 문자열, 값은 아이템(3.3) 또는 아이템 배열 → 둘 다 파라미터화(3.1.2) 가능
  - 0개 이상 멤버 가능, 키는 딕셔너리 범위 내에서 고유
- 구현체는 인덱스·키 양쪽 모두로 딕셔너리 접근을 제공해야 함(MUST) → 명세는 둘 중 어느 접근 방식이든 사용 가능(MAY)

```
sf-dictionary  = dict-member *( OWS "," OWS dict-member )
dict-member    = member-key ( parameters / ( "=" member-value ))
member-key     = key
member-value   = sf-item / inner-list
```

- 멤버는 직렬화된 순서대로 정렬, 쉼표와 선택적 공백으로 구분
  - 멤버 키는 대문자 포함 불가, 키와 값은 공백 없이 "="로 구분
  - 예: `Example-Dict: en="Applepie", da=:w4ZibGV0w6ZydGU=:` — 마지막 "="는 바이트 시퀀스 포함으로 인한 것 (3.3.5 참고)
- 값이 불리언(3.3.6) true인 멤버는 직렬화 시 값을 생략해야 함(MUST)
  - 예: `Example-Dict: a=?0, b, c; foo=bar` — "b"와 "c"는 모두 true
  - 이 요구사항은 직렬화에만 적용 → 파서는 딕셔너리 값에서 true 불리언이 나타날 때 여전히 올바르게 처리해야 함
- 예: 값이 토큰의 내부 리스트인 멤버 — `Example-Dict: rating=1.5, feelings=(joy sadness)`
- 예: 아이템과 내부 리스트가 혼합, 일부는 파라미터를 가짐 — `Example-Dict: a=(1 2), b=3, c=4;aa=bb, d=(5 6);valid`
- 리스트와 마찬가지로 빈 딕셔너리는 필드 전체 생략으로 표현 → 딕셔너리로 정의된 필드는 기본 빈 값을 가짐을 함의
- 필드 명세는 키별로 개별 멤버의 허용 타입과 필수/선택 여부를 명세함으로써 딕셔너리 의미론 정의
  - 수신자는 필드 명세가 명시적으로 금지하지 않는 한, 정의되지 않았거나 알려지지 않은 키를 갖는 멤버를 무시해야 함(MUST)
- 딕셔너리도 동일 헤더/트레일러 섹션의 여러 줄에 멤버 분할 가능
  - 다음은 동등: `Example-Dict: foo=1, bar=2` / `Example-Dict: foo=1` + `Example-Dict: bar=2`
  - 단, 개별 멤버는 여러 줄에 걸쳐 분할 불가 (4.2절 참고)
- 파서는 최소 1024개 키/값 쌍, 최소 64개 문자의 키를 지원해야 함(MUST) → 필드 명세는 순서·값 타입 제약 가능

#### 3.3 아이템(Items)

- 정수(3.3.1) · 소수(3.3.2) · 문자열(3.3.3) · 토큰(3.3.4) · 바이트 시퀀스(3.3.5) · 불리언(3.3.6) 중 하나 → 연관된 파라미터(3.1.2) 가능

```
sf-item   = bare-item parameters
bare-item = sf-integer / sf-decimal / sf-string / sf-token
            / sf-binary / sf-boolean
```

- 예: 정수인 아이템 — `Example-Integer: 5`, 파라미터를 가지면 `Example-Integer: 5; foo=bar`

##### 3.3.1 정수(Integers)

- IEEE 754 호환성([IEEE754])을 위해 -999,999,999,999,999 ~ 999,999,999,999,999(양 끝값 포함, 부호 있는 최대 15자리) 범위

```
sf-integer = ["-"] 1*15DIGIT
```

- 예: `Example-Integer: 42`
- 15자리보다 큰 정수는 문자열(3.3.3)·바이트 시퀀스(3.3.5) 사용, 또는 스케일링 인자로 작동하는 정수 파라미터로 지원 가능
- 선행 0(예: "0002", "-01")·부호 있는 0("-0")으로 직렬화 가능하나, 구현체가 이 구별을 보존하지 않을 수 있음
- 정수 프로즈에 쓰인 쉼표는 가독성용 → 와이어 형식에서는 무효

##### 3.3.2 소수(Decimals)

- 정수 부분(최대 12자리)과 소수 부분(최대 3자리)을 가진 숫자

```
sf-decimal  = ["-"] 1*12DIGIT "." 1*3DIGIT
```

- 예: `Example-Decimal: 4.5`
- 선행 0(예: "0002.5", "-01.334")·후행 0(예: "5.230", "-0.40")·부호 있는 0(예: "-0.0")으로 직렬화 가능하나, 구현체가 이 구별을 보존하지 않을 수 있음
- 직렬화 알고리즘(4.1.5)은 소수 부분이 3자리보다 많은 정밀도를 가진 입력을 반올림 → 대안적 반올림 전략이 필요하면 직렬화 전에 헤더 정의로 명세 필요

##### 3.3.3 문자열(Strings)

- 0개 이상의 출력 가능한 ASCII([RFC0020]) 문자(%x20~%x7E 범위) → 탭·개행·캐리지 리턴 제외

```
sf-string = DQUOTE *chr DQUOTE
chr       = unescaped / escaped
unescaped = %x20-21 / %x23-5B / %x5D-7E
escaped   = "\" ( DQUOTE / "\" )
```

- 큰따옴표로 구분, 큰따옴표·백슬래시 이스케이프에 백슬래시("\") 사용. 예: `Example-String: "hello world"`
- 구분자로 DQUOTE만 사용 → 작은따옴표는 문자열을 구분하지 않음
  - 이스케이프 가능한 문자는 DQUOTE와 "\"뿐 → 그 외 문자가 뒤따르면 파싱이 실패해야 함(MUST)
- 유니코드는 문자열에서 직접 지원 안 됨 (다수의 상호운용성 문제 + 대부분 필드가 필요로 하지 않음)
  - 비-ASCII 콘텐츠 전달이 필요하면 문자 인코딩(가급적 UTF-8, [STD63])과 함께 바이트 시퀀스(3.3.5) 명세 가능
- 파서는 (디코딩 후) 최소 1024개 문자의 문자열을 지원해야 함(MUST)

##### 3.3.4 토큰(Tokens)

- 짧은 텍스트 단어, 추상 모델은 HTTP 필드 값 직렬화에서의 표현과 동일

```
sf-token = ( ALPHA / "*" ) *( tchar / ":" / "/" )
```

- 예: `Example-Token: foo123/456`
- 파서는 최소 512개 문자의 토큰을 지원해야 함(MUST)
- [RFC7230]의 "token" ABNF 규칙과 동일 문자 허용, 단 첫 문자는 ALPHA 또는 "*"여야 하고 이후 문자에 ":"·"/"도 허용되는 예외

##### 3.3.5 바이트 시퀀스(Byte Sequences)

```
sf-binary = ":" *(base64) ":"
base64    = ALPHA / DIGIT / "+" / "/" / "="
```

- 콜론으로 구분, base64([RFC4648] 4절)로 인코딩. 예: `Example-ByteSequence: :cHJldGVuZCB0aGlzIGlzIGJpbmFyeSBjb250ZW50Lg==:`
- 파서는 디코딩 후 최소 16384 옥텟의 바이트 시퀀스를 지원해야 함(MUST)

##### 3.3.6 불리언(Booleans)

```
sf-boolean = "?" boolean
boolean    = "0" / "1"
```

- 선행 "?" 뒤에 true는 "1", false는 "0". 예: `Example-Boolean: ?1`
- 딕셔너리(3.2)·파라미터(3.1.2) 값에서는 불리언 true를 값 생략으로 표현

### 4. HTTP에서 구조화된 필드 다루기

- 텍스트 형식 HTTP 필드 값 및 호환 인코딩(예: HPACK([RFC7541]) 압축 이전의 HTTP/2([RFC7540]))에서 구조화된 필드를 직렬화·파싱하는 방법 정의

#### 4.1 구조화된 필드 직렬화

구조가 주어졌을 때 HTTP 필드 값에 적합한 ASCII 문자열을 반환:

- 구조가 딕셔너리/리스트이고 값이 비어 있으면(멤버 없음) → 필드를 전혀 직렬화하지 않음(field-name·field-value 모두 생략)
- 구조가 리스트이면 → output_string = 리스트 직렬화(4.1.1) 결과
- 그 외 구조가 딕셔너리이면 → output_string = 딕셔너리 직렬화(4.1.2) 결과
- 그 외 구조가 아이템이면 → output_string = 아이템 직렬화(4.1.3) 결과
- 그 외 → 직렬화 실패
- ASCII 인코딩([RFC0020])으로 output_string을 바이트 배열로 변환해 반환

##### 4.1.1 리스트 직렬화

(member_value, parameters) 튜플 배열 input_list가 주어졌을 때:

- output = 빈 문자열
- input_list의 각 (member_value, parameters)에 대해
  - member_value가 배열이면 → 내부 리스트 직렬화(4.1.1.1) 결과를 output에 덧붙임
  - 그 외 → 아이템 직렬화(4.1.3) 결과를 output에 덧붙임
  - input_list에 남은 member_value가 있으면 → output에 "," 덧붙이고 단일 SP 덧붙임
- output 반환

###### 4.1.1.1 내부 리스트 직렬화

(member_value, parameters) 튜플 배열 inner_list, 파라미터 list_parameters가 주어졌을 때:

- output = 문자열 "("
- inner_list의 각 (member_value, parameters)에 대해
  - 아이템 직렬화(4.1.3) 결과를 output에 덧붙임
  - inner_list에 남은 값이 있으면 → output에 단일 SP 덧붙임
- output에 ")" 덧붙임
- list_parameters에 대해 파라미터 직렬화(4.1.1.2) 결과를 output에 덧붙임
- output 반환

###### 4.1.1.2 파라미터 직렬화

순서 있는 딕셔너리 input_parameters(각 멤버는 param_key·param_value)가 주어졌을 때:

- output = 빈 문자열
- input_parameters의 각 (param_key, param_value)에 대해
  - output에 ";" 덧붙임
  - param_key에 대해 키 직렬화(4.1.1.3) 결과를 output에 덧붙임
  - param_value가 불리언 true가 아니면
    - output에 "=" 덧붙임
    - param_value에 대해 베어 아이템 직렬화(4.1.3.1) 결과를 output에 덧붙임
- output 반환

###### 4.1.1.3 키 직렬화

키 input_key가 주어졌을 때:

- input_key를 ASCII 문자 시퀀스로 변환 → 실패 시 직렬화 실패
- input_key가 lcalpha·DIGIT·"_"·"-"·"."·"*"에 없는 문자를 포함하면 → 직렬화 실패
- input_key의 첫 문자가 lcalpha 또는 "*"가 아니면 → 직렬화 실패
- output = 빈 문자열, output에 input_key 덧붙임
- output 반환

##### 4.1.2 딕셔너리 직렬화

순서 있는 딕셔너리 input_dictionary(각 멤버는 member_key와 튜플 값 (member_value, parameters))가 주어졌을 때:

- output = 빈 문자열
- input_dictionary의 각 member_key에 대해
  - member_key에 대해 키 직렬화(4.1.1.3) 결과를 output에 덧붙임
  - member_value가 불리언 true이면 → parameters에 대해 파라미터 직렬화(4.1.1.2) 결과를 output에 덧붙임
  - 그 외
    - output에 "=" 덧붙임
    - member_value가 배열이면 → 내부 리스트 직렬화(4.1.1.1) 결과를 output에 덧붙임
    - 그 외 → 아이템 직렬화(4.1.3) 결과를 output에 덧붙임
  - input_dictionary에 남은 멤버가 있으면 → output에 "," 덧붙이고 단일 SP 덧붙임
- output 반환

##### 4.1.3 아이템 직렬화

아이템 bare_item, 파라미터 item_parameters가 주어졌을 때:

- output = 빈 문자열
- bare_item에 대해 베어 아이템 직렬화(4.1.3.1) 결과를 output에 덧붙임
- item_parameters에 대해 파라미터 직렬화(4.1.1.2) 결과를 output에 덧붙임
- output 반환

###### 4.1.3.1 베어 아이템 직렬화

아이템 input_item이 주어졌을 때:

- input_item이 정수이면 → 정수 직렬화(4.1.4) 결과 반환
- input_item이 소수이면 → 소수 직렬화(4.1.5) 결과 반환
- input_item이 문자열이면 → 문자열 직렬화(4.1.6) 결과 반환
- input_item이 토큰이면 → 토큰 직렬화(4.1.7) 결과 반환
- input_item이 바이트 시퀀스이면 → 바이트 시퀀스 직렬화(4.1.8) 결과 반환
- input_item이 불리언이면 → 불리언 직렬화(4.1.9) 결과 반환
- 그 외 → 직렬화 실패

##### 4.1.4 정수 직렬화

정수 input_integer가 주어졌을 때:

- input_integer가 -999,999,999,999,999 ~ 999,999,999,999,999(양 끝값 포함) 범위의 정수가 아니면 → 직렬화 실패
- output = 빈 문자열
- input_integer < 0이면 → output에 "-" 덧붙임
- input_integer의 수치 값을 십진수 숫자만 사용해 10진법으로 표현한 것을 output에 덧붙임
- output 반환

##### 4.1.5 소수 직렬화

소수 input_decimal이 주어졌을 때:

- input_decimal이 소수가 아니면 → 직렬화 실패
- input_decimal의 소수점 오른쪽 유효 숫자가 3자리보다 많으면 → 세 자리로 반올림(마지막 자릿수는 가장 가까운 값, 등거리면 짝수로)
- 반올림 후 소수점 왼쪽 유효 숫자가 12자리보다 많으면 → 직렬화 실패
- output = 빈 문자열
- input_decimal < 0이면 → output에 "-" 덧붙임
- input_decimal 정수 부분을 10진법(십진수 숫자만)으로 표현해 output에 덧붙임 (0이면 "0")
- output에 "." 덧붙임
- 소수 부분이 0이면 → output에 "0" 덧붙임, 그 외 → 소수 부분의 유효 숫자를 10진법으로 표현해 덧붙임
- output 반환

##### 4.1.6 문자열 직렬화

문자열 input_string이 주어졌을 때:

- input_string을 ASCII 문자 시퀀스로 변환 → 실패 시 직렬화 실패
- input_string이 %x00-1f 또는 %x7f-ff 범위(VCHAR·SP 외) 문자를 포함하면 → 직렬화 실패
- output = 문자열 DQUOTE
- input_string의 각 문자 char에 대해
  - char가 "\" 또는 DQUOTE이면 → output에 "\" 덧붙임
  - output에 char 덧붙임
- output에 DQUOTE 덧붙임
- output 반환

##### 4.1.7 토큰 직렬화

토큰 input_token이 주어졌을 때:

- input_token을 ASCII 문자 시퀀스로 변환 → 실패 시 직렬화 실패
- input_token의 첫 문자가 ALPHA·"*"가 아니거나, 나머지가 tchar·":"·"/"에 없는 문자를 포함하면 → 직렬화 실패
- output = 빈 문자열, output에 input_token 덧붙임
- output 반환

##### 4.1.8 바이트 시퀀스 직렬화

바이트 시퀀스 input_bytes가 주어졌을 때:

- input_bytes가 바이트 시퀀스가 아니면 → 직렬화 실패
- output = 빈 문자열, output에 ":" 덧붙임
- [RFC4648] 4절에 따라 input_bytes를 base64 인코딩한 결과를 덧붙임
- output에 ":" 덧붙임
- output 반환
- 인코딩된 데이터는 [RFC4648] 3.2절에 따라 "="로 패딩되어야 함(required)
- 인코딩된 데이터는 구현상 제약이 없는 한 [RFC4648] 3.5절에 따라 패딩 비트가 0으로 설정되어야 함(SHOULD)

##### 4.1.9 불리언 직렬화

불리언 input_boolean이 주어졌을 때:

- input_boolean이 불리언이 아니면 → 직렬화 실패
- output = 빈 문자열, output에 "?" 덧붙임
- input_boolean이 true이면 → output에 "1" 덧붙임, false이면 → "0" 덧붙임
- output 반환

#### 4.2 구조화된 필드 파싱

- 구조화된 필드로 알려진 HTTP 필드를 파싱할 때 상호운용성·보안 문제를 야기할 수 있는 경계 사례(edge cases)가 다수 존재 → 주의 필요

선택된 필드의 field-value를 나타내는 바이트 배열 input_bytes(필드가 없으면 빈 값), field_type("dictionary"/"list"/"item" 중 하나)이 주어졌을 때:

- input_bytes를 ASCII 문자열 input_string으로 변환 → 실패 시 파싱 실패
- input_string에서 선행 SP 문자들을 버림
- field_type이 "list"이면 → output = 리스트 파싱(4.2.1) 결과
- field_type이 "dictionary"이면 → output = 딕셔너리 파싱(4.2.2) 결과
- field_type이 "item"이면 → output = 아이템 파싱(4.2.3) 결과
- input_string에서 선행 SP 문자들을 버림
- input_string이 비어 있지 않으면 → 파싱 실패
- 그 외 → output 반환

- input_bytes 생성 시 파서는 [RFC7230] 3.2.2절에 따라 필드 이름이 대소문자 구분 없이 일치하는 동일 섹션(헤더/트레일러)의 모든 필드 라인을 하나의 쉼표로 구분된 field-value로 결합해야 함(MUST) → 전체 필드 값이 올바르게 처리되도록 보장
  - 리스트·딕셔너리는 최상위 구조의 개별 멤버가 여러 헤더 인스턴스에 걸쳐 분할되지 않는 한 이 결합이 모든 라인을 올바르게 연결 → 두 타입의 파싱 알고리즘은 탭 문자도 허용(일부 구현체가 필드 라인 결합에 사용)
  - 여러 필드 라인에 걸쳐 분할된 문자열은 예측 불가능한 결과 (쉼표가 파서 출력 문자열의 일부가 됨) → 연결이 상류(upstream) 중개자에 의해 수행될 수 있어 직렬화기·파서의 통제 밖일 수 있음
  - 토큰·정수·소수·바이트 시퀀스는 여러 필드 라인에 걸쳐 분할 불가 (삽입된 쉼표가 파싱 실패를 유발)
  - 파서는 여러 필드 라인에 걸친 값을 처리할 때 그중 하나가 해당 필드로 파싱되지 않으면 실패해도 됨(MAY)
    - 예: sf-string으로 정의된 Example-String 필드 처리 시 다음 섹션에서 실패 허용 — `Example-String: "foo` / `Example-String: bar"`
- 파싱 실패 시(다른 알고리즘 호출 실패 포함) 전체 필드 값이 무시되어야 함(MUST, 필드가 섹션에 존재하지 않는 것처럼 취급) → 상호운용성·안전성을 위해 의도적으로 엄격, 이 문서를 참조하는 명세는 이 요구사항을 완화할 수 없음
  - 이 요구사항은 필드를 파싱하지 않는 구현체에는 적용되지 않음 (예: 중개자가 전달 전 실패 필드 제거를 강제받지 않음)

##### 4.2.1 리스트 파싱

ASCII 문자열 input_string이 주어졌을 때 (item_or_inner_list, parameters) 튜플 배열 반환, input_string은 파싱된 값을 제거하도록 수정됨:

- members = 빈 배열
- input_string이 비어 있지 않은 동안
  - 아이템 또는 내부 리스트 파싱(4.2.1.1) 결과를 members에 덧붙임
  - input_string에서 선행 OWS 문자들을 버림
  - input_string이 비어 있으면 → members 반환
  - input_string의 첫 문자를 소비, ","가 아니면 → 파싱 실패
  - input_string에서 선행 OWS 문자들을 버림
  - input_string이 비어 있으면 → 후행 쉼표, 파싱 실패
- 구조화된 데이터 없음 → 빈 members 반환

###### 4.2.1.1 아이템 또는 내부 리스트 파싱

ASCII 문자열 input_string이 주어졌을 때 (item_or_inner_list, parameters) 튜플 반환(item_or_inner_list는 단일 베어 아이템 또는 (bare_item, parameters) 튜플 배열):

- input_string의 첫 문자가 "("이면 → 내부 리스트 파싱(4.2.1.2) 결과 반환
- 그 외 → 아이템 파싱(4.2.3) 결과 반환

###### 4.2.1.2 내부 리스트 파싱

ASCII 문자열 input_string이 주어졌을 때 (inner_list, parameters) 튜플 반환(inner_list는 (bare_item, parameters) 튜플 배열):

- input_string의 첫 문자를 소비, "("가 아니면 → 파싱 실패
- inner_list = 빈 배열
- input_string이 비어 있지 않은 동안
  - input_string에서 선행 SP 문자들을 버림
  - input_string의 첫 문자가 ")"이면
    - 첫 문자 소비
    - parameters = 파라미터 파싱(4.2.3.2) 결과
    - (inner_list, parameters) 튜플 반환
  - item = 아이템 파싱(4.2.3) 결과, item을 inner_list에 덧붙임
  - input_string의 첫 문자가 SP 또는 ")"가 아니면 → 파싱 실패
- 내부 리스트 끝을 못 찾음 → 파싱 실패

##### 4.2.2 딕셔너리 파싱

ASCII 문자열 input_string이 주어졌을 때 값이 (item_or_inner_list, parameters) 튜플인 순서 있는 맵 반환:

- dictionary = 빈 순서 있는 맵
- input_string이 비어 있지 않은 동안
  - this_key = 키 파싱(4.2.3.3) 결과
  - input_string의 첫 문자가 "="이면
    - 첫 문자 소비
    - member = 아이템 또는 내부 리스트 파싱(4.2.1.1) 결과
  - 그 외
    - value = 불리언 true
    - parameters = 파라미터 파싱(4.2.3.2) 결과
    - member = (value, parameters) 튜플
  - dictionary가 이미 this_key를 포함하면(문자 대 문자 비교) → 값을 member로 덮어씀
  - 그 외 → this_key·member를 dictionary에 덧붙임
  - input_string에서 선행 OWS 문자들을 버림
  - input_string이 비어 있으면 → dictionary 반환
  - input_string의 첫 문자를 소비, ","가 아니면 → 파싱 실패
  - input_string에서 선행 OWS 문자들을 버림
  - input_string이 비어 있으면 → 후행 쉼표, 파싱 실패
- 구조화된 데이터 없음 → 빈 dictionary 반환
- 중복된 딕셔너리 키가 발견되면 마지막 인스턴스를 제외한 모두가 무시됨

##### 4.2.3 아이템 파싱

ASCII 문자열 input_string이 주어졌을 때 (bare_item, parameters) 튜플 반환:

- bare_item = 베어 아이템 파싱(4.2.3.1) 결과
- parameters = 파라미터 파싱(4.2.3.2) 결과
- (bare_item, parameters) 튜플 반환

###### 4.2.3.1 베어 아이템 파싱

ASCII 문자열 input_string이 주어졌을 때 베어 아이템 반환:

- 첫 문자가 "-" 또는 DIGIT이면 → 정수 또는 소수 파싱(4.2.4) 결과 반환
- 첫 문자가 DQUOTE이면 → 문자열 파싱(4.2.5) 결과 반환
- 첫 문자가 ALPHA 또는 "*"이면 → 토큰 파싱(4.2.6) 결과 반환
- 첫 문자가 ":"이면 → 바이트 시퀀스 파싱(4.2.7) 결과 반환
- 첫 문자가 "?"이면 → 불리언 파싱(4.2.8) 결과 반환
- 그 외 → 아이템 타입 인식 불가, 파싱 실패

###### 4.2.3.2 파라미터 파싱

ASCII 문자열 input_string이 주어졌을 때 값이 베어 아이템인 순서 있는 맵 반환:

- parameters = 빈 순서 있는 맵
- input_string이 비어 있지 않은 동안
  - 첫 문자가 ";"가 아니면 → 루프 종료
  - ";" 소비
  - input_string에서 선행 SP 문자들을 버림
  - param_key = 키 파싱(4.2.3.3) 결과
  - param_value = 불리언 true
  - 첫 문자가 "="이면 → "=" 소비, param_value = 베어 아이템 파싱(4.2.3.1) 결과
  - parameters가 이미 param_key를 포함하면(문자 대 문자 비교) → 값을 param_value로 덮어씀
  - 그 외 → param_key·param_value를 parameters에 덧붙임
- parameters 반환
- 중복된 파라미터 키가 발견되면 마지막 인스턴스를 제외한 모두가 무시됨

###### 4.2.3.3 키 파싱

ASCII 문자열 input_string이 주어졌을 때 키 반환:

- 첫 문자가 lcalpha 또는 "*"가 아니면 → 파싱 실패
- output_string = 빈 문자열
- input_string이 비어 있지 않은 동안
  - 첫 문자가 lcalpha·DIGIT·"_"·"-"·"."·"*" 중 하나가 아니면 → output_string 반환
  - char = 첫 문자 소비 결과, output_string에 char 덧붙임
- output_string 반환

##### 4.2.4 정수 또는 소수 파싱

ASCII 문자열 input_string이 주어졌을 때 정수 또는 소수 반환. 이 알고리즘은 정수(3.3.1)와 소수(3.3.2)를 모두 파싱하며 해당 구조를 반환:

- type = "integer", sign = 1, input_number = 빈 문자열
- 첫 문자가 "-"이면 → 소비하고 sign = -1
- input_string이 비어 있으면 → 빈 정수, 파싱 실패
- 첫 문자가 DIGIT가 아니면 → 파싱 실패
- input_string이 비어 있지 않은 동안
  - char = 첫 문자 소비 결과
  - char가 DIGIT이면 → input_number에 덧붙임
  - 그 외 type이 "integer"이고 char가 "."이면
    - input_number가 12자보다 많으면 → 파싱 실패
    - 그 외 → char를 input_number에 덧붙이고 type = "decimal"
  - 그 외 → char를 input_string 앞에 되돌리고 루프 종료
  - type이 "integer"이고 input_number가 15자보다 많으면 → 파싱 실패
  - type이 "decimal"이고 input_number가 16자보다 많으면 → 파싱 실패
- type이 "integer"이면 → input_number를 정수로 파싱, output_number = 결과 × sign
- 그 외
  - input_number의 마지막 문자가 "."이면 → 파싱 실패
  - "." 이후 문자 수가 3자보다 많으면 → 파싱 실패
  - input_number를 소수로 파싱, output_number = 결과 × sign
- output_number 반환

##### 4.2.5 문자열 파싱

ASCII 문자열 input_string이 주어졌을 때 따옴표가 제거된(unquoted) 문자열 반환:

- output_string = 빈 문자열
- 첫 문자가 DQUOTE가 아니면 → 파싱 실패
- 첫 문자를 버림
- input_string이 비어 있지 않은 동안
  - char = 첫 문자 소비 결과
  - char가 "\"이면
    - input_string이 비어 있으면 → 파싱 실패
    - next_char = 첫 문자 소비 결과
    - next_char가 DQUOTE 또는 "\"가 아니면 → 파싱 실패
    - output_string에 next_char 덧붙임
  - 그 외 char가 DQUOTE이면 → output_string 반환
  - 그 외 char가 %x00-1f 또는 %x7f-ff 범위(VCHAR·SP 외)이면 → 파싱 실패
  - 그 외 → output_string에 char 덧붙임
- 닫는 DQUOTE를 못 찾고 끝에 도달 → 파싱 실패

##### 4.2.6 토큰 파싱

ASCII 문자열 input_string이 주어졌을 때 토큰 반환:

- 첫 문자가 ALPHA 또는 "*"가 아니면 → 파싱 실패
- output_string = 빈 문자열
- input_string이 비어 있지 않은 동안
  - 첫 문자가 tchar·":"·"/"에 없으면 → output_string 반환
  - char = 첫 문자 소비 결과, output_string에 char 덧붙임
- output_string 반환

##### 4.2.7 바이트 시퀀스 파싱

ASCII 문자열 input_string이 주어졌을 때 바이트 시퀀스 반환:

- 첫 문자가 ":"가 아니면 → 파싱 실패
- 첫 문자를 버림
- 끝 이전에 ":" 문자가 없으면 → 파싱 실패
- b64_content = 첫 ":" 인스턴스 직전까지(미포함) 소비한 내용
- 시작에서 ":" 소비
- b64_content가 ALPHA·DIGIT·"+"·"/"·"="에 없는 문자를 포함하면 → 파싱 실패
- binary_content = b64_content를 base64 디코딩([RFC4648])한 결과(필요 시 패딩 합성) → 디코딩 실패 시 파싱 실패
- binary_content 반환
- 일부 base64 구현은 "=" 패딩 미준수 데이터의 거부를 허용하지 않으므로([RFC4648] 3.2절) 파서는 그렇게 구성될 수 없는 경우가 아니면 "=" 패딩 부재로 실패해서는 안 됨(SHOULD NOT)
- 일부 base64 구현은 0이 아닌 패딩 비트를 가진 데이터의 거부를 허용하지 않으므로([RFC4648] 3.5절) 파서는 그렇게 구성될 수 없는 경우가 아니면 실패해서는 안 됨(SHOULD NOT)
- 이 명세는 [RFC4648] 3.1절·3.3절 요구사항을 완화하지 않음 → 파서는 base64 알파벳 밖 문자와 인코딩된 데이터 내 개행에 대해 실패해야 함(MUST)

##### 4.2.8 불리언 파싱

ASCII 문자열 input_string이 주어졌을 때 불리언 반환:

- 첫 문자가 "?"가 아니면 → 파싱 실패
- 첫 문자를 버림
- 첫 문자가 "1"이면 → 버리고 true 반환
- 첫 문자가 "0"이면 → 버리고 false 반환
- 일치 없음 → 파싱 실패

### 5. IANA 고려사항

- 이 문서는 IANA 조치 없음

### 6. 보안 고려사항

- 구조화된 필드가 정의하는 대부분 타입의 크기는 무제한 → 극도로 큰 필드는 자원 소모 등 공격 벡터가 될 수 있음
  - 대부분의 HTTP 구현체는 개별 필드 크기와 전체 헤더/트레일러 섹션 크기를 제한해 완화
- 새 HTTP 필드를 주입할 수 있는 당사자는 구조화된 필드의 의미를 변경 가능 → 일부 상황에서는 파싱 실패로 이어지지만, 모든 상황에서 신뢰성 있게 실패하는 것은 불가능

### 7. 참고문헌

#### 7.1 규범적 참고문헌

- [RFC0020] ASCII format for network interchange, STD 80, RFC 20, 1969
- [RFC2119] Key words for use in RFCs to Indicate Requirement Levels, BCP 14, RFC 2119, 1997
- [RFC4648] The Base16, Base32, and Base64 Data Encodings, RFC 4648, 2006
- [RFC5234] Augmented BNF for Syntax Specifications: ABNF, STD 68, RFC 5234, 2008
- [RFC7230] Hypertext Transfer Protocol (HTTP/1.1): Message Syntax and Routing, RFC 7230, 2014
- [RFC8174] Ambiguity of Uppercase vs Lowercase in RFC 2119 Key Words, BCP 14, RFC 8174, 2017

#### 7.2 정보성 참고문헌

- [IEEE754] IEEE Standard for Floating-Point Arithmetic, IEEE 754-2019, 2019
- [RFC7231] Hypertext Transfer Protocol (HTTP/1.1): Semantics and Content, RFC 7231, 2014
- [RFC7493] The I-JSON Message Format, RFC 7493, 2015
- [RFC7540] Hypertext Transfer Protocol Version 2 (HTTP/2), RFC 7540, 2015
- [RFC7541] HPACK: Header Compression for HTTP/2, RFC 7541, 2015
- [RFC8259] The JavaScript Object Notation (JSON) Data Interchange Format, STD 90, RFC 8259, 2017
- [STD63] UTF-8, a transformation format of ISO 10646, STD 63 / RFC 3629, 2003

### 부록 A. 자주 묻는 질문

#### A.1 왜 JSON이 아닌가?

- 구조화된 필드의 초기 제안은 JSON([RFC8259]) 기반 → HTTP 헤더 필드에 맞게 사용을 제약하려면 송신자·수신자가 추가 처리를 구현해야 했음
- JSON은 큰 숫자·중복 멤버를 가진 객체에 관해 명세상 문제 있음 → 회피 조언([RFC7493] 등)이 있으나 의존 불가
- JSON 문자열은 기본적으로 유니코드 문자열 → 비교 등에서 다수의 상호운용성 문제 → 비-ASCII 콘텐츠 회피를 권고할 수는 있으나 강제는 어려움
- JSON은 임의 깊이로 콘텐츠 중첩 가능 → 임베디드 등 제한적 서버 배포에서 메모리 부담 문제 → 제한이 필요하나 기존 JSON 구현체는 그런 제한이 없고, 제한을 명세해도 일부 필드 정의가 위반할 필요를 찾을 가능성 높음
- JSON의 광범위한 채택·구현으로 모든 구현체에 추가 제약 부과가 어려움 → 일부 배포가 강제 실패 시 상호운용성 훼손, "JSON처럼 보이면" 필드 값에 JSON 파서/직렬화기를 쓰고 싶은 유혹 발생
- 구조화된 필드의 주요 목표(상호운용성 개선, 구현 단순화)가 전용 파서·직렬화기를 요구하는 형식으로 이어짐
- 추가로 JSON이 HTTP 필드에서 "제대로 보이지 않는다"는 느낌이 광범위하게 공유됨

### 부록 B. 구현 노트

- 일반적 구현은 최상위 직렬화(4.1)·파싱(4.2) 함수를 노출해야 함 → 반드시 함수 형태일 필요는 없음(예: 최상위 타입별 메서드를 가진 객체로 구현 가능)
- 상호운용성을 위해 일반 구현이 완전하고 알고리즘을 면밀히 따르는 것이 중요 (1.1 참고) → 공통 테스트 스위트가 <https://github.com/httpwg/structured-field-tests>에서 커뮤니티 유지
- 딕셔너리·파라미터는 순서 보존 맵(order-preserving maps) → 일부 필드는 순서에 의미를 두지 않아도 애플리케이션이 이용할 수 있도록 순서를 노출해야 함
- 토큰과 문자열의 구별 보존이 중요 → 대부분 프로그래밍 언어는 다른 타입에 잘 매핑되는 네이티브 타입을 가지므로, 구별 유지를 위해 래퍼 "token" 객체나 함수 파라미터가 필요할 수 있음
- 직렬화 알고리즘은 3절 데이터 타입에 엄격히 한정되지 않는 방식으로 정의됨 (예: 소수는 더 넓은 입력을 받아 허용된 값으로 반올림)
- 구현체는 각 타입의 최소값을 따르는 한도 내에서 구조 크기 제한 가능 → 구조가 구현 제한을 초과하면 파싱/직렬화 실패

---

## RFC 7239 - Forwarded HTTP 확장

> 발행: 2014년 6월 (A. Petersson, M. Nilsson)

### 개요

- 프록시 구성 요소가 프록싱 과정에서 변경·손실되는 정보를 공개할 수 있게 하는 HTTP 확장 헤더 필드 정의
  - 예: 프록시의 사용자 에이전트 측 인터페이스 IP 주소, HTTP 요청의 원본 클라이언트 IP 주소
- 모든 프록시 체인 구성 요소가 완전한 IP 주소 정보에 접근 가능하도록 확장
- 요청 출처를 익명화하기 위한 지침도 명시

### 1. 서론

- 매우 다양한 애플리케이션이 프록시로 동작 → 상당수는 최종 사용자에 투명 → 예: 요청 로드 밸런싱, 암호화 오프로드, 자주 접근하는 리소스 캐싱
- 프록시 환경의 큰 한계: 요청을 전달할 때 프록시 IP에서 시작된 것처럼 보임 → 원본 클라이언트 정보 손실
- 이 정보 손실은 클라이언트 IP를 필요로 하는 서버 기능에 부정적 영향
  - 정보 필요 기능 범주: 진단(diagnostics) · 접근 제어(access control) · 남용 관리(abuse management)
  - 진단 예: 이벤트 로깅, 문제 해결, 통계 수집
  - 접근 제어 예: 클라이언트 주소 화이트리스팅 (신뢰할 수 있는 프록시 설정 없이는 작동 불가)
  - 남용 관리 예: 대부분 진단 기능에 의존
- 대부분의 경우 이 정보 손실은 프록시의 목적이 아니라 의도치 않은 부작용
  - 클라이언트 정보를 숨기는 것이 명시적 목적인 예: 익명화 프록시(anonymizing proxy) → 이 확장을 구현하지 않음
  - 리버스 프록시(reverse proxy)도 사용자·원본 서버 사이 정보를 숨기는 유형 → 정보 숨김이 의도치 않은 부작용일 때, HTTP 메시지 자체에 정보를 인코딩하면 다운스트림 소비자에 도움
- X-Forwarded-For, X-Forwarded-By, X-Forwarded-Proto 등 비표준 헤더가 이 문제를 부분적으로 해결 → 표준화는 상호운용성 이점
  - 이 문서의 "Forwarded" 헤더는 모든 정보를 단일 헤더 필드에 통합 → 서로 다른 X-Forwarded-* 변형으로는 불가능했던 정보 간 상관관계 가능
  - 이 헤더는 선택적 → 프록시가 프라이버시 보호를 위해 구현하지 않아도 영향 없음
- NAT(네트워크 주소 변환) 시스템에서도 유사한 문제 발생 ([RFC6269]에서 추가 논의)

### 2. 표기법 규칙

- MUST/MUST NOT/REQUIRED/SHALL/SHALL NOT/SHOULD/SHOULD NOT/RECOMMENDED/MAY/OPTIONAL은 [RFC2119] 정의 따름

### 3. 구문 표기법

- [RFC5234]의 ABNF(Augmented Backus-Naur Form) 사용, [RFC7230] 7절의 리스트 규칙 확장 포함

### 4. Forwarded HTTP 헤더 필드

- "Forwarded"는 프록시가 프록싱 과정에서 변경·손실될 수 있는 정보를 공개할 수 있는 선택적 헤더 필드
  - 공개 정보는 파라미터-식별자 쌍 형태, 5.5절의 확장으로 추가 가능
  - 헤더 정보는 민감할 수 있음(8절 참고) → 배포 시 기본값은 비활성화되어야 함(SHOULD), 개별 파라미터는 개별 활성화 가능
  - HTTP 요청에서만 사용해야 함(MUST) — 응답에는 사용 불가
  - 포워딩 프록시·리버스 프록시 모두에 적용 가능
- 전달 정보 예: 요청 클라이언트의 소스 IP, 프록시 수신 인터페이스 IP, 클라이언트가 사용한 프로토콜
  - 체인의 여러 프록시가 각각 파라미터 세트 추가 가능, 프록시는 이전에 추가된 "Forwarded" 헤더를 삭제할 수도 있음
- 최상위 리스트는 [RFC7230] 3.2절 정의의 HTTP 헤더 필드-값 목록
  - 첫 요소 = 이 확장을 구현하는 첫 프록시가 추가한 정보, 이후 요소 = 각 후속 프록시가 추가한 정보
  - 선택적 확장이므로 지원 프록시도 선택에 따라 업데이트하지 않을 수 있음
- 리스트의 각 필드-값은 세미콜론으로 구분된 파라미터-식별자 쌍 목록
  - 파라미터-식별자 쌍은 파라미터 다음에 등호와 값
  - 각 파라미터는 필드-값당 최대 한 번만 나타나야 함(MUST)
  - 파라미터 이름은 대소문자 구분 없음
  - 파라미터 값은 [RFC7230] 3.2.6절의 토큰 또는 따옴표로 묶인 문자열
  - 토큰 파라미터 값은 대소문자를 구분하지 않을 수 있음(MAY)

```
Forwarded   = 1#forwarded-element

forwarded-element =
    [ forwarded-pair ] *( ";" [ forwarded-pair ] )

forwarded-pair = token "=" value
value          = token / quoted-string

token = <[RFC7230] 3.2.6절에 정의됨>
quoted-string = <[RFC7230] 3.2.6절에 정의됨>
```

예시:

```
Forwarded: for="_gazonk"
Forwarded: For="[2001:db8:cafe::17]:4711"
Forwarded: for=192.0.2.60;proto=http;by=203.0.113.43
Forwarded: for=192.0.2.43, for=198.51.100.17
```

- IPv6 주소는 콜론·대괄호가 유효한 토큰 문자가 아니므로 따옴표+대괄호로 감쌈
- 프록시가 이미 "Forwarded" 헤더를 포함하는 요청에 새 값을 추가할 때
  - 기존 마지막 "Forwarded" 헤더 끝에 쉼표 뒤로 추가하거나, 헤더 블록 끝에 새 필드를 추가 가능
  - 프록시는 기존의 모든 "Forwarded" 헤더를 제거할 수도 있음(MAY)
  - 여러 "Forwarded" 헤더가 존재할 때 올바른 필드가 업데이트되도록 해야 함(MUST)

### 5. 파라미터

- 이 문서가 명시하는 파라미터: "by"·"for"·"host"·"proto"
  - by: 요청이 들어온 프록시의 사용자 에이전트 측 인터페이스
  - for: 프록시에 요청하는 노드
  - host: 프록시가 받은 host 요청 헤더 필드
  - proto: 수신 요청에 사용된 프로토콜

#### 5.1 Forwarded By

- 프록시 서버로의 수신 요청의 사용자 에이전트 측 인터페이스 공개용 → 일반적으로 IP 주소 + 선택적 포트 번호
- 기본 설정은 6.3절의 난독화된 식별자를 포함해야 함(SHOULD)
  - 서버가 주소 기반 기능을 필요로 하면 IP 주소 + 선택적 포트 번호로 설정 변경 가능
  - 세 번째 옵션: 6.2절의 "unknown" 식별자
- "by" 값 구문은 따옴표 문자열 이스케이프 해제 후 6절의 "node" ABNF를 따라야 함(MUST)
- 주된 사용 사례: 리버스 프록시가 백엔드 서버에 정보 전달 → 멀티홈 환경에서 요청 유입 경로 신호로도 유용

#### 5.2 Forwarded For

- 요청을 시작한 클라이언트와 프록시 체인의 후속 프록시 공개용 → 일반적으로 IP 주소 + 선택적 포트 번호
- 기본 설정은 6.3절의 난독화된 식별자를 포함해야 함(SHOULD)
  - 서버가 주소 기반 기능을 필요로 하면 IP 주소 + 선택적 포트 번호로 설정 변경 가능
  - 세 번째 옵션: 6.2절의 "unknown" 식별자
- "for" 값 구문은 따옴표 문자열 이스케이프 해제 후 6절의 "node" ABNF를 따라야 함(MUST)
- 이 확장을 완전히 활용하는 체인에서는 첫 "for" 값 = 최초 요청 클라이언트, 이후 각 값 = 이전 프록시의 식별자
  - 체인의 마지막 프록시는 "for" 목록에 나타나지 않음 → 마지막 프록시의 IP·포트는 네트워크 전송 계층의 원격 주소로 확인 가능
  - 단, 이전 "Forwarded" 헤더의 "by" 파라미터가 마지막 프록시 식별에 더 적절한 정보를 제공할 수 있음

#### 5.3 Forwarded Host

- 원본 "Host" 요청 헤더 필드 전달용 → 리버스 프록시가 들어오는 "Host" 헤더를 내부 호스트 이름으로 재작성하는 경우에 유용
- "host" 값 구문은 따옴표 문자열 이스케이프 해제 후 [RFC7230] 5.4절의 Host ABNF를 따라야 함(MUST)

#### 5.4 Forwarded Proto

- 사용된 프로토콜 유형 값 포함 → 값 구문은 이스케이프 해제 후 [RFC3986] 3.1절 정의·[RFC4395] 등록의 URI 스킴 이름을 따라야 함(MUST)
  - 일반적 값: "http" 또는 "https"
- 예: 리버스 프록시가 암호화 오프로드를 제공하는 환경에서 원본 서버 연결은 평문 HTTP일 수 있으나, 이 파라미터로 원본 서버가 사용자 에이전트가 요청한 연결 유형에 맞게 URL을 재작성 가능

#### 5.5 확장

- 확장은 추가 파라미터·값을 허용 → 리버스 프록시 환경에서 특히 유용
- 모든 확장 파라미터는 "HTTP Forwarded Parameter" 레지스트리에 등록되어야 함(SHOULD)
- 광범위한 배포가 예상되는 확장은 표준화되어야 함(SHOULD, 9절 참고)

### 6. 노드 식별자

- "by"·"for" 파라미터의 각 값은 다음 중 하나로 구성된 식별자 포함
  - 선택적 포트 번호를 포함하는 클라이언트 IP 주소
  - 요청 클라이언트 IP를 알 수 없을 때 이를 나타내는 토큰
  - 내부 구조·민감 정보를 공개하지 않고 추적·디버깅을 가능하게 하는 생성된 토큰

```
node     = nodename [ ":" node-port ]
nodename = IPv4address / "[" IPv6address "]" /
            "unknown" / obfnode

IPv4address = <[RFC3986] 3.2.2절에 정의됨>
IPv6address = <[RFC3986] 3.2.2절에 정의됨>
obfnode = "_" 1*( ALPHA / DIGIT / "." / "_" / "-")

node-port     = port / obfport
port          = 1*5DIGIT
obfport       = "_" 1*(ALPHA / DIGIT / "." / "_" / "-")

DIGIT = <[RFC5234] 3.4절에 정의됨>
ALPHA = <[RFC5234] B.1절에 정의됨>
```

- 각 식별자는 선택적으로 포트 식별과 함께 사용 가능 → NAT 환경에서 특히 유용
  - "node-port"는 실제 포트 번호이거나 이를 숨기는 난독화된 토큰 → 내부 정보를 드러내지 않으면서 디버깅 가능
- ABNF는 "unknown" 식별자에도 포트 번호 추가를 허용하나, 해석은 이를 소유한 프록시에 달려 있음
- "obfport"를 실제 포트 번호와 구분하기 위해 선행 밑줄이 필요(MUST), "ALPHA"·"DIGIT"·"."·"_"·"-"로만 구성되어야 함
- IPv6 주소와 node-port가 있는 모든 nodename은 콜론이 토큰에서 허용되지 않으므로 따옴표로 묶어야 함(MUST)

예시: `"192.0.2.43:47011"`, `"[2001:db8:cafe::17]:47011"`

#### 6.1 IPv4 및 IPv6 식별자

- "IPv6address"·"IPv4address" ABNF 규칙은 [RFC3986]에 명시
- "IPv6address"는 [RFC5952]의 텍스트 표현 권장사항(소문자, 제로 압축)을 따라야 함(SHOULD)
- IP 주소는 [RFC1918]·[RFC4193]에 따라 내부 네트워크에서 올 수 있음, IPv6 주소는 항상 대괄호로 감싸야 함

#### 6.2 "unknown" 식별자

- 이전 엔티티의 신원을 알 수 없지만 프록시가 요청 전달이 이루어졌음을 알리고자 할 때 사용
- 예: 수신 요청의 TCP 소켓에 직접 접근하지 않고 발신 요청을 생성하는 프록시 서버 프로세스

#### 6.3 난독화된 식별자

- 생성된 식별자는 내부 IP를 숨기면서도 "Forwarded" 헤더가 추적·디버깅에 쓰일 수 있게 함
  - IP 주소가 아닌 전송이 필요한 인터페이스 레이블을 쓰는 프록시에도 유용
- 정적 식별자 할당이 필요한 경우가 아니면 요청마다 무작위로 생성되어야 함(SHOULD)
  - 요청 간 지속되는 식별자가 필요하면 수명은 해당 구현의 클라이언트 IP 유지 기간과 같거나 짧아야 함(SHOULD)
- 선행 밑줄 "_"을 포함해야 하며(MUST), "ALPHA"·"DIGIT"·"."·"_"·"-"로만 구성되어야 함(MUST)

예시: `Forwarded: for=_hidden, for=_SEVKISEK`

### 7. 구현 고려사항

#### 7.1 HTTP 리스트

- HTTP 리스트는 식별자 사이 공백을 허용, 여러 헤더 필드에 걸칠 수 있음
- 다음은 모두 동일:

```
Forwarded: for=192.0.2.43,for="[2001:db8:cafe::17]",for=unknown
Forwarded: for=192.0.2.43, for="[2001:db8:cafe::17]", for=unknown
```

또는:

```
Forwarded: for=192.0.2.43
Forwarded: for="[2001:db8:cafe::17]", for=unknown
```

#### 7.2 헤더 필드 보존

- 일부 경우 "Forwarded" 헤더는 보존되어야 하고, 다른 경우는 그렇지 않음
  - 직접 전달되는 요청 → 헤더를 보존하고 잠재적으로 확장해야 함
  - 하나의 수신 요청이 여러 발신 요청을 생성하는 경우 → 헤더 보존 필요 여부를 특별히 판단해야 함
  - 여러 발신 요청이 있는 경우 일반적으로 보존해야 하지만, 수신 요청의 직접 결과가 아닌 발신 요청에는 보존하지 않아야 함
- [RFC7232] 4.1절 참고: 프록시가 304 응답 콘텐츠가 캐시된 엔티티와 다르다고 감지하면 조건 없이 요청을 반복해야 하는 경우 있음 → 이런 반복 요청은 수신 요청의 직접 결과로 간주되므로 헤더 보존이 적절

#### 7.3 Via와의 관계

- "Via" 헤더([RFC7230] 5.7.1절)는 "Forwarded"와 유사한 목적 → Via는 클라이언트 측 정보를 제공하지 않음
  - Via는 프록시 자체 정보, "Forwarded"는 프록시가 릴레이하는 클라이언트 측 정보에 초점
  - "Via"는 이미 널리 배포됨 → "Forwarded"가 해결하는 문제를 다루기 위해 Via 형식을 변경하는 것은 불가능
- 이 헤더 정보를 "Via" 정보와 결합해 해석하는 것이 불가능할 수 있음 → 일부 프록시는 "Forwarded"만, 다른 프록시는 Via만, 또 다른 프록시는 둘 다 업데이트

#### 7.4 전환

- X-Forwarded-For 등 X-Forwarded-* 헤더를 수신하는 프록시가 이를 "Forwarded" 형식으로 변환하는 것이 바람직할 수 있음
  - 단일 유형만 있는 경우(예: X-Forwarded-For) → 각 요소 앞에 "for=" 추가로 변환 가능
  - X-Forwarded-For의 IPv6 주소는 따옴표·대괄호 없이 쓰일 수 있으나, "Forwarded"에서는 둘 다 필요

예시:

```
X-Forwarded-For: 192.0.2.43, 2001:db8:cafe::17
```

변환 결과:

```
Forwarded: for=192.0.2.43, for="[2001:db8:cafe::17]"
```

- 여러 유형의 X-Forwarded-* 헤더가 있는 경우 특별 주의 필요 → 추가 순서를 모르면 변환 불가능할 수 있음
- X-Forwarded-For 헤더 제거는 아직 "Forwarded"를 지원하지 않는 당사자에게 문제를 일으킬 수 있음

#### 7.5 사용 예시

- 가정: IP 192.0.2.43의 클라이언트가 198.51.100.17 프록시를 거쳐 203.0.113.60의 또 다른 프록시를 지나 원본 서버에 도달 (예: 악성코드 필터링 프록시 뒤의 사무실 클라이언트가 리버스 프록시가 있는 서버에 접근)
  - 클라이언트 → 첫 프록시: 직접 전달이므로 "Forwarded" 헤더 없음
  - 첫 프록시 → 두 번째 프록시: `Forwarded: for=192.0.2.43`
  - 두 번째 프록시 → 원본 서버:

```
Forwarded: for=192.0.2.43,
           for=198.51.100.17;by=203.0.113.60;proto=http;
           host=example.com
```

- 연결 체인의 특정 지점에서 확장 지원이 없거나, 네트워크 구성 요소 정보를 공개하지 않기로 한 정책 결정으로 정보가 업데이트되지 않을 수 있음

### 8. 보안 고려사항

#### 8.1 헤더 유효성 및 무결성

- "Forwarded" 헤더는 올바르다고 신뢰할 수 없음 → 요청 클라이언트를 포함해 서버까지 경로의 모든 노드가 실수 또는 악의적으로 수정 가능
- 검증 방법 하나: 신뢰할 수 있는 프록시를 화이트리스트에 추가하고 해당 프록시가 제공하는 헤더로 판단
  - 약점 1: 프록시 요청 이전에 나열된 IP 주소 체인은 신뢰 불가
  - 약점 2: 프록시-엔드포인트 간 네트워크 통신이 보호되지 않으면 네트워크 접근 가능한 공격자가 데이터를 수정 가능

#### 8.2 정보 유출

- "Forwarded" 헤더는 NAT·프록시 설정 뒤의 내부 네트워크 구조를 원치 않게 노출할 수 있음
  - 대응: 난독화된 요소 사용 · 내부 노드가 헤더를 업데이트하지 않음 · 출구 프록시에서 항목 제거
- 이 헤더는 원본 서버나 중간자의 응답 메시지에 절대 나타나서는 안 됨 → 나타나면 전체 프록시 체인이 클라이언트에 노출될 수 있음
  - "Forwarded" 사용 환경에서는 이 필드가 응답 본문에 나타나는 TRACE 요청에 주의 필요

#### 8.3 프라이버시 고려사항

- 사용자 프라이버시와 디버깅/통계/위치 기반 콘텐츠를 위한 정보 공개 사이에는 트레이드오프 존재 → "Forwarded"는 설계상 프라이버시에 민감하다고 여겨지는 정보를 노출
- HTTP 요청이 프라이버시 의미를 요청하는 헤더를 포함하는 경우, 모든 프록시는 "Forwarded" 헤더를 사용해서는 안 되며(SHOULD NOT), IP 주소 등 개인 정보를 다른 방식으로도 다음 홉에 전달해서는 안 됨(SHOULD NOT)
- "for" 파라미터가 전달하는 클라이언트 IP는 프라이버시 민감 정보로 여겨짐 → 개별 클라이언트 식별, 인터넷 서비스 사업자 식별, 대략적 지리 위치 추정이 가능하기 때문
- 직접 연결에서 이용 가능한 정보를 보존하는 프록시는 사용자·배포자의 인지·기대 여부와 무관하게 최종 사용자 프라이버시에 영향을 줌
- 구현자·배포자는 이 확장 배포가 사용자 프라이버시에 미치는 영향을 고려해야 함
- "by"·"for" 파라미터의 기본 설정은 요청마다 무작위로 생성되는 난독화된 식별자를 포함해야 함(SHOULD)
  - 요청 간 지속 식별자가 필요하면 수명을 제한, 클라이언트 IP 유지 기간을 초과해서는 안 됨(SHOULD)
  - 난독화된 식별자 생성 시 잠재적으로 민감한 정보를 포함하지 않도록 주의 필요
- 사용자 IP는 이미 X-Forwarded-For를 통해 전달하는 프록시에 의해 전달될 수 있으며 널리 배포됨
  - 프록시 중개 없이 직접 연결하는 사용자는 클라이언트 IP가 어차피 웹 서버로 전송됨
  - 비익명화 프록시를 선택한 사용자는 IP 주소 보호를 기대할 수 없음
  - 추적 위험 최소화를 위해 브라우저 헤더 필드 핑거프린팅 등 다른 정보 유출 방법에도 유의 필요
  - 고유 클라이언트 식별자가 없어도 "Forwarded" 헤더 자체가 통과한 프록시 체인을 드러내 핑거프린팅을 용이하게 할 수 있음

### 9. IANA 고려사항

- 이 문서는 아래 HTTP 헤더 필드를 명시, [RFC3864]의 "영구 메시지 헤더 필드 이름" 레지스트리에 추가됨
  - 헤더 필드: Forwarded / 적용 프로토콜: http / 상태: standard / 관리자: IETF (iesg@ietf.org) / 명세 문서: 이 명세(4절)
- "Forwarded" 헤더는 IANA가 관리하는 "HTTP Forwarded Parameters" 레지스트리의 파라미터를 포함
  - 초기 등록은 아래, 추가 할당은 [RFC5226]의 IETF Review 절차
  - 새 파라미터의 보안·프라이버시 영향은 철저히 문서화되어야 함
  - 새 파라미터·값은 4절의 forwarded-pair ABNF 정의를 따라야 함(MUST), 등록 시 간단한 설명 포함 필요
- 초기 등록 파라미터
  - by: 프록시의 수신 인터페이스 IP 주소 (5.1절)
  - for: 프록시를 통해 요청하는 클라이언트의 IP 주소 (5.2절)
  - host: 수신 요청의 Host 헤더 필드 (5.3절)
  - proto: 수신 요청에 사용된 애플리케이션 프로토콜 (5.4절)

### 10. 참조

#### 10.1 규범적 참조

- [RFC1918] Address Allocation for Private Internets, BCP 5, RFC 1918, 1996
- [RFC2119] Key words for use in RFCs to Indicate Requirement Levels, BCP 14, RFC 2119, 1997
- [RFC3864] Registration Procedures for Message Header Fields, BCP 90, RFC 3864, 2004
- [RFC3986] Uniform Resource Identifier (URI): Generic Syntax, STD 66, RFC 3986, 2005
- [RFC4193] Unique Local IPv6 Unicast Addresses, RFC 4193, 2005
- [RFC4395] Guidelines and Registration Procedures for New URI Schemes, BCP 35, RFC 4395, 2006
- [RFC5226] Guidelines for Writing an IANA Considerations Section in RFCs, BCP 26, RFC 5226, 2008
- [RFC5234] Augmented BNF for Syntax Specifications: ABNF, STD 68, RFC 5234, 2008
- [RFC5952] A Recommendation for IPv6 Address Text Representation, RFC 5952, 2010
- [RFC7230] Hypertext Transfer Protocol (HTTP/1.1): Message Syntax and Routing, RFC 7230, 2014
- [RFC7232] Hypertext Transfer Protocol (HTTP/1.1): Conditional Requests, RFC 7232, 2014

#### 10.2 참고 참조

- [RFC6269] Issues with IP Address Sharing, RFC 6269, 2011

### 부록 A. 감사의 글

- 기여자: Per Cederqvist, Alissa Cooper, Adrian Farrel, Stephen Farrell, Ned Freed, Per Hedbor, Amos Jeffries, Poul-Henning Kamp, Murray S. Kucherawy, Barry Leiba, Salvatore Loreto, Alexey Melnikov, S. Moonesamy, Susan Nichols, Mark Nottingham, Julian Reschke, John Sullivan, Willy Tarreau, Dan Wing
