# RFC 3986: Uniform Resource Identifier (URI) - 일반 구문

> URI(Uniform Resource Identifier)의 일반 구문을 정의하는 표준 문서

## 문서 정보

| 항목 | 내용 |
|------|------|
| RFC 번호 | 3986 |
| 분류 | STD 66 (인터넷 표준) |
| 작성자 | Tim Berners-Lee (W3C/MIT), Roy T. Fielding (Day Software), Larry M. Masinter (Adobe Systems) |
| 발행일 | 2005년 1월 |
| 상태 | 인터넷 표준 (Internet Standard) |
| 갱신 | RFC 1738 |
| 폐기 | RFC 2732, RFC 2396, RFC 1808 |

## 개요

이 명세는 URI(Uniform Resource Identifier)의 일반 구문을 정의합니다. URI는 "추상적이거나 물리적인 리소스를 식별하는 간결한 문자 시퀀스" 입니다. 이 문서는 스킴에 독립적인 파싱 메커니즘을 제공하면서, 개별 URI 스킴이 자체적인 제한사항과 의미를 정의할 수 있도록 합니다.

---

## 1. 서론

### 1.1 URI의 특성

URI는 세 가지 핵심 속성으로 특징지어집니다:

#### 균일성 (Uniformity)

균일성은 다양한 리소스 식별자 유형이 동일한 컨텍스트에서 공존할 수 있게 합니다. 이를 통해 기존 식별자를 방해하지 않고 새로운 식별자 유형을 도입할 수 있습니다.

#### 리소스 (Resource)

리소스는 식별될 수 있는 모든 엔티티를 포함합니다:
- 문서, 이미지
- 서비스
- 추상적 개념
- 심지어 책이나 조직과 같은 비인터넷 엔티티

#### 식별자 (Identifier)

식별자는 해당 범위 내에서 하나의 리소스를 다른 모든 리소스와 구별합니다. 정의보다는 구별에 초점을 맞춥니다.

### 1.2 일반 구문 원칙

> "URI 구문은 연합적이고 확장 가능한 명명 시스템으로, 각 스킴의 명세는 해당 스킴을 사용하는 식별자의 구문과 의미를 추가로 제한할 수 있습니다."

이 명세는 모든 URI 스킴에서 요구되는 공통 요소를 정의하여, 스킴 독립적인 파싱을 가능하게 하면서 스킴별 처리는 연기합니다.

### 1.3 URI 예시

다양한 스킴을 보여주는 예시들:

```
ftp://ftp.is.co.za/rfc/rfc1808.txt
http://www.ietf.org/rfc/rfc2396.txt
ldap://[2001:db8::7]/c=GB?objectClass?one
mailto:John.Doe@example.com
news:comp.infosystems.www.servers.unix
tel:+1-816-555-1212
telnet://192.0.2.16:80/
urn:oasis:names:specification:docbook:dtd:xml:4.1.2
```

### 1.4 URI vs URL vs URN

> "URI는 로케이터(locator), 이름(name), 또는 둘 다로 추가 분류될 수 있습니다."

| 용어 | 설명 |
|------|------|
| URL (Uniform Resource Locator) | 위치 정보를 제공 |
| URN (Uniform Resource Name) | 영속적인 이름 부여를 강조 |
| URI | URL과 URN을 포괄하는 상위 개념 |

개별 스킴이 반드시 어느 한 유형으로만 분류될 필요는 없습니다.

---

## 2. 설계 고려사항

### 2.1 전사(Transcription)

URI는 "매우 제한된 문자 집합: 기본 라틴 알파벳 문자, 숫자, 그리고 몇 가지 특수 문자" 를 사용하여 다양한 매체와 키보드에서 전역적인 전사를 가능하게 합니다.

핵심 설계 원칙:

> "한 매체에서 다른 매체로 리소스 식별자를 전사할 수 있는 능력이 URI가 가장 의미 있는 구성요소로 이루어지는 것보다 더 중요하게 여겨졌습니다."

### 2.2 식별과 상호작용의 분리

중요한 구분:

> "URI 자체는 오직 식별만 제공합니다; 리소스에 대한 접근은 URI의 존재로 보장되거나 암시되지 않습니다."

| 용어 | 설명 |
|------|------|
| URI 해석 (Resolution) | 접근 메커니즘을 결정 |
| 역참조 (Dereference) | 리소스에 대한 실제 작업 수행 |

이러한 프로세스는 URI 명세가 아닌, URI를 사용하는 프로토콜에 의해 정의됩니다.

### 2.3 계층적 식별자

> "URI 구문은 계층적으로 구성되어 있으며, 구성요소는 왼쪽에서 오른쪽으로 중요도가 감소하는 순서로 나열됩니다."

특수 구분자들:
- 슬래시 (`/`)
- 물음표 (`?`)
- 번호 기호 (`#`)

이들은 스킴 전체에서 일관된 계층 표현을 가능하게 하여, 문서 트리 내에서의 상대 참조를 용이하게 합니다.

---

## 3. 문자 인코딩

### 3.1 퍼센트 인코딩 (Percent-Encoding)

> "퍼센트 인코딩된 옥텟은 퍼센트 문자 '%' 다음에 해당 옥텟의 숫자 값을 나타내는 두 개의 16진수 숫자로 구성된 문자 삼중자로 인코딩됩니다."

```
ABNF: pct-encoded = "%" HEXDIG HEXDIG
```

예시: `%20`은 공백 문자(U+0020)를 나타냅니다.

핵심 원칙: 대문자 16진수 숫자('A'-'F')는 소문자와 동등하지만, 일관성을 위해 생산자는 대문자를 사용해야 합니다(SHOULD).

### 3.2 예약 문자 (Reserved Characters)

예약 문자는 URI 구성요소와 하위 구성요소를 구분합니다:

| 분류 | 문자 |
|------|------|
| 일반 구분자 (gen-delims) | `:` `/` `?` `#` `[` `]` `@` |
| 하위 구분자 (sub-delims) | `!` `$` `&` `'` `(` `)` `*` `+` `,` `;` `=` |

```
ABNF:
reserved    = gen-delims / sub-delims
gen-delims  = ":" / "/" / "?" / "#" / "[" / "]" / "@"
sub-delims  = "!" / "$" / "&" / "'" / "(" / ")"
            / "*" / "+" / "," / ";" / "="
```

> "URI 구성요소의 데이터가 예약 문자의 구분자 목적과 충돌하는 경우, 충돌하는 데이터는 URI가 형성되기 전에 퍼센트 인코딩되어야 합니다(MUST)."

### 3.3 비예약 문자 (Unreserved Characters)

이 문자들은 인코딩 없이 나타날 수 있습니다:

| 분류 | 문자 |
|------|------|
| 알파벳 | A-Z, a-z |
| 숫자 | 0-9 |
| 특수 문자 | 하이픈(`-`), 마침표(`.`), 밑줄(`_`), 틸데(`~`) |

```
ABNF: unreserved = ALPHA / DIGIT / "-" / "." / "_" / "~"
```

> "비예약 문자를 해당하는 퍼센트 인코딩된 US-ASCII 옥텟으로 대체한 것이 다른 URI는 동등합니다: 동일한 리소스를 식별합니다."

예시: `%41`은 `A`와 동등합니다.

### 3.4 인코딩/디코딩 원칙

> "정상적인 상황에서 URI 내의 옥텟이 퍼센트 인코딩되는 유일한 시점은 구성 부분에서 URI를 생성하는 과정입니다."

중요한 규칙:

> "구현체는 동일한 문자열을 두 번 이상 퍼센트 인코딩하거나 디코딩해서는 안 됩니다(MUST NOT). 이미 디코딩된 문자열을 디코딩하면 퍼센트 데이터 옥텟을 퍼센트 인코딩의 시작으로 잘못 해석할 수 있습니다."

### 3.5 국제 문자

> "새로운 URI 스킴이 유니버설 문자 집합의 문자로 구성된 텍스트 데이터를 나타내는 구성요소를 정의할 때, 데이터는 먼저 UTF-8 문자 인코딩에 따라 옥텟으로 인코딩되어야 합니다; 그런 다음 비예약 집합의 문자에 해당하지 않는 옥텟만 퍼센트 인코딩되어야 합니다."

예시: `LATIN CAPITAL LETTER A WITH GRAVE` (À) = `%C3%80`

---

## 4. 구문 구성요소

### 4.1 전체 구조

```
URI = scheme ":" hier-part [ "?" query ] [ "#" fragment ]

hier-part = "//" authority path-abempty
          / path-absolute
          / path-rootless
          / path-empty
```

시각적 표현:

```
         foo://example.com:8042/over/there?name=ferret#nose
         \_/   \______________/\_________/ \_________/ \__/
          |           |            |            |        |
       scheme     authority       path        query   fragment
          |   _____________________|__
         / \ /                        \
         urn:example:animal:ferret:nose
```

요구사항:
- 스킴과 경로는 필수 (경로는 비어 있을 수 있음)
- 권한(authority)이 존재할 때, 경로는 비어 있거나 `/`로 시작해야 함(MUST)
- 권한이 없을 때, 경로는 `//`로 시작할 수 없음(MUST NOT)

### 4.2 스킴 (Scheme)

> "스킴 이름은 문자로 시작하고 문자, 숫자, 플러스('+'), 마침표('.'), 또는 하이픈('-')의 조합이 뒤따르는 문자 시퀀스로 구성됩니다."

```
ABNF: scheme = ALPHA *( ALPHA / DIGIT / "+" / "-" / "." )
```

> "스킴은 대소문자를 구분하지 않지만(case-insensitive), 정규 형식은 소문자이며 스킴을 지정하는 문서는 소문자로 지정해야 합니다(MUST)."

예시:
- `http` (올바름)
- `HTTP` (유효하지만 정규화되지 않음)
- `Http` (유효하지만 정규화되지 않음)

### 4.3 권한 (Authority)

```
authority = [ userinfo "@" ] host [ ":" port ]
```

> "권한 구성요소는 이중 슬래시('//')로 시작하며, 다음 슬래시('/'), 물음표('?'), 번호 기호('#') 문자, 또는 URI의 끝으로 종료됩니다."

구조 분해:

```
         userinfo       host      port
         ┌──┴───┐ ┌──────┴──────┐ ┌┴┐
         user:pass@www.example.com:8080
```

#### 4.3.1 사용자 정보 (User Information)

선택적 userinfo 구성요소는 `@` 기호 앞에 위치합니다:

```
ABNF: userinfo = *( unreserved / pct-encoded / sub-delims / ":" )
```

보안 참고:

> "userinfo 필드에서 'user:password' 형식의 사용은 폐기되었습니다(deprecated)... 평문으로 인증 정보를 전달하는 것은 거의 모든 경우에 보안 위험으로 입증되었습니다."

#### 4.3.2 호스트 (Host)

세 가지 호스트 형식이 허용됩니다:

| 형식 | 설명 | 예시 |
|------|------|------|
| IP-literal | IPv6 주소 (대괄호로 묶음) | `[2001:db8::7]` |
| IPv4address | 점-십진 표기법 | `192.0.2.16` |
| reg-name | 도메인 이름 또는 기타 등록된 식별자 | `www.example.com` |

```
ABNF:
host        = IP-literal / IPv4address / reg-name
IP-literal  = "[" ( IPv6address / IPvFuture ) "]"
IPv4address = dec-octet "." dec-octet "." dec-octet "." dec-octet
reg-name    = *( unreserved / pct-encoded / sub-delims )
```

> "호스트 하위 구성요소는 대소문자를 구분하지 않습니다(case-insensitive)."

IPv6 표현:

> "128비트 IPv6 주소는 8개의 16비트 조각으로 나뉩니다... 주소 내에서 하나 이상의 연속된 0 값 16비트 조각의 시퀀스는 생략될 수 있으며, 모든 숫자를 생략하고 정확히 두 개의 연속 콜론만 남깁니다."

예시:
```
[2001:db8:85a3::8a2e:370:7334]  ← :: 는 연속된 0을 대체
[::1]                           ← 루프백 주소
[fe80::1%eth0]                  ← 존 ID 포함
```

등록된 이름:

> "DNS에서 조회하기 위한 등록된 이름은 [RFC1034]의 섹션 3.5에 정의된 구문을 사용합니다."

> "reg-name 구문은 기본 이름 확인 기술과 독립적인 균일한 방식으로 비ASCII 등록 이름을 나타내기 위해 퍼센트 인코딩된 옥텟을 허용합니다."

#### 4.3.3 포트 (Port)

```
ABNF: port = *DIGIT
```

> "스킴은 기본 포트를 정의할 수 있습니다... 포트 번호로 지정된 포트 유형(예: TCP, UDP, SCTP)은 URI 스킴에 의해 정의됩니다."

예시:
| 스킴 | 기본 포트 |
|------|----------|
| http | 80 |
| https | 443 |
| ftp | 21 |

### 4.4 경로 (Path)

경로는 리소스를 식별하는 계층적 데이터를 포함합니다. 다섯 가지 ABNF 규칙이 서로 다른 경로 유형을 구분합니다:

| 규칙 | 설명 | 예시 |
|------|------|------|
| `path-abempty` | `/`로 시작하거나 비어 있음 | `/a/b/c` 또는 `` |
| `path-absolute` | `/`로 시작하지만 `//`로는 아님 | `/a/b` |
| `path-noscheme` | 콜론이 없는 세그먼트로 시작 | `a/b/c` |
| `path-rootless` | 세그먼트로 시작 | `a:b/c` |
| `path-empty` | 문자 없음 | `` |

```
ABNF:
path          = path-abempty    ; "/"로 시작하거나 비어 있음
              / path-absolute   ; "/"로 시작하지만 "//"는 아님
              / path-noscheme   ; 콜론 없는 세그먼트로 시작
              / path-rootless   ; 세그먼트로 시작
              / path-empty      ; 문자 없음

path-abempty  = *( "/" segment )
path-absolute = "/" [ segment-nz *( "/" segment ) ]
path-noscheme = segment-nz-nc *( "/" segment )
path-rootless = segment-nz *( "/" segment )
path-empty    = 0<pchar>

segment       = *pchar
segment-nz    = 1*pchar
segment-nz-nc = 1*( unreserved / pct-encoded / sub-delims / "@" )
                ; 콜론 ":"이 없는 비어 있지 않은 세그먼트

pchar         = unreserved / pct-encoded / sub-delims / ":" / "@"
```

점-세그먼트 (Dot-Segments):

`.`과 `..`는 계층 내에서 상대 참조를 가능하게 하는 특수 경로 세그먼트입니다. 파일 시스템 디렉토리 표기법과 유사하지만 URI 계층 내에서만 해석됩니다.

> "경로 세그먼트는 일반 구문에 의해 불투명(opaque)으로 간주됩니다."

### 4.5 쿼리 (Query)

```
ABNF: query = *( pchar / "/" / "?" )
```

> "쿼리 구성요소는 경로 구성요소의 데이터와 함께 리소스를 식별하는 데 사용되는 비계층적 데이터를 포함합니다."

쿼리 구성요소는 종종 `key=value` 쌍을 전달하지만 다른 URI에 대한 참조를 나타낼 수도 있습니다.

예시:
```
?name=ferret&color=purple
?search=uri%20encoding
?redirect=http://example.com/
```

### 4.6 프래그먼트 (Fragment)

```
ABNF: fragment = *( pchar / "/" / "?" )
```

> "URI의 프래그먼트 식별자 구성요소는 기본 리소스에 대한 참조와 추가 식별 정보를 통해 보조 리소스의 간접 식별을 허용합니다."

핵심 원칙:

> "프래그먼트 식별자 의미는 URI 스킴과 독립적이므로 스킴 명세에 의해 재정의될 수 없습니다."

> "프래그먼트 식별자는 URI의 스킴별 처리에 사용되지 않습니다; 대신 프래그먼트 식별자는 역참조 전에 URI의 나머지 부분에서 분리되며, 따라서 프래그먼트 자체 내의 식별 정보는 사용자 에이전트에 의해서만 역참조됩니다."

예시:
```
http://example.com/page.html#section1
http://example.com/doc.pdf#page=10
http://example.com/video.mp4#t=30,60
```

---

## 5. 사용법 고려사항

### 5.1 URI 참조

URI-참조는 전체 URI이거나 상대 참조일 수 있습니다.

```
ABNF: URI-reference = URI / relative-ref
```

### 5.2 상대 참조 (Relative Reference)

```
ABNF:
relative-ref  = relative-part [ "?" query ] [ "#" fragment ]
relative-part = "//" authority path-abempty
              / path-absolute
              / path-noscheme
              / path-empty
```

세 가지 범주:

| 범주 | 설명 | 예시 |
|------|------|------|
| 네트워크 경로 참조 | `//`로 시작 (드물게 사용) | `//example.com/path` |
| 절대 경로 참조 | 단일 `/`로 시작 | `/path/to/resource` |
| 상대 경로 참조 | `/`로 시작하지 않음 | `path/to/resource`, `../parent` |

콜론 제약:

> "콜론 문자를 포함하는 경로 세그먼트(예: 'this:that')는 상대 경로 참조의 첫 번째 세그먼트로 사용될 수 없습니다. 스킴 이름으로 오인될 수 있기 때문입니다."

해결책:
```
❌ this:that         (스킴으로 오인됨)
✅ ./this:that       (명확한 상대 경로)
```

### 5.3 절대 URI

```
ABNF: absolute-URI = scheme ":" hier-part [ "?" query ]
```

프래그먼트 식별자가 허용되지 않을 때 사용됩니다.

### 5.4 동일 문서 참조 (Same-Document Reference)

> "URI 참조가 프래그먼트 구성요소(있는 경우)를 제외하고 기본 URI와 동일한 URI를 참조할 때, 그 참조를 '동일 문서' 참조라고 합니다."

주요 예시:
- 빈 참조: `""`
- 프래그먼트만 포함하는 참조: `#section1`

### 5.5 접미사 참조 (Suffix Reference)

전통적인 매체에서는 때때로 `www.w3.org/Addressing/`과 같은 부분 URI를 사용하여 휴리스틱 완성을 기대합니다.

주의:

> "이러한 접미사 참조 사용 관행이 일반적이지만, 가능할 때마다 피해야 하며 장기 참조가 예상되는 상황에서는 절대 사용해서는 안 됩니다."

이유:
- 휴리스틱은 시간이 지남에 따라 변경됨
- 컨텍스트 밖에서 종종 실패함
- 보안 취약점을 생성함

---

## 6. 참조 해석 (Reference Resolution)

### 6.1 기본 URI 설정

상대 참조는 기본 URI가 필요하며, 우선순위 계층을 통해 설정됩니다:

| 우선순위 | 방법 | 설명 |
|---------|------|------|
| 1 (최고) | 콘텐츠에 내장된 기본 URI | HTML의 `<base>` 태그 등 |
| 2 | 캡슐화 엔티티의 기본 URI | MIME 메시지의 Content-Location 등 |
| 3 | 검색 URI의 기본 URI | 리소스를 가져온 URI |
| 4 (최저) | 기본 기본 URI | 애플리케이션별 기본값 |

> "기본 URI는 `<absolute-URI>` 구문 규칙을 따라야 합니다(MUST). 기본 URI가 URI 참조에서 얻어진 경우, 해당 참조는 기본 URI로 사용되기 전에 절대 형식으로 변환되고 프래그먼트 구성요소가 제거되어야 합니다(MUST)."

### 6.2 상대 해석 알고리즘

상대 참조를 대상 URI로 변환하는 알고리즘:

```
1. 참조를 다섯 구성요소로 파싱
2. 스킴 정의 확인
3. 스킴이 없으면 권한 확인
4. 권한이 없으면 경로 확인
5. 병합 및 점-세그먼트 제거 작업 적용
```

의사 코드:

```
if defined(R.scheme) then
   T.scheme    = R.scheme;
   T.authority = R.authority;
   T.path      = remove_dot_segments(R.path);
   T.query     = R.query;
else
   if defined(R.authority) then
      T.authority = R.authority;
      T.path      = remove_dot_segments(R.path);
      T.query     = R.query;
   else
      if (R.path == "") then
         T.path = Base.path;
         if defined(R.query) then
            T.query = R.query;
         else
            T.query = Base.query;
         endif;
      else
         if (R.path starts-with "/") then
            T.path = remove_dot_segments(R.path);
         else
            T.path = merge(Base, R.path);
            T.path = remove_dot_segments(T.path);
         endif;
         T.query = R.query;
      endif;
      T.authority = Base.authority;
   endif;
   T.scheme = Base.scheme;
endif;

T.fragment = R.fragment;
```

#### 6.2.1 경로 병합 (Merge)

상대 경로 참조를 병합할 때:

```
if defined(Base.authority) and empty(Base.path) then
   return "/" + R.path;
else
   return remove_last_segment(Base.path) + R.path;
endif;
```

- 기본 URI에 권한이 있고 경로가 비어 있으면 `/`를 앞에 추가
- 그렇지 않으면 기본 경로의 마지막 세그먼트를 제외한 모든 세그먼트에 참조 경로 추가

#### 6.2.2 점-세그먼트 제거 알고리즘

> "점-세그먼트는 기본 URI의 이름 계층에 상대적인 식별자를 표현하기 위해 URI 참조에서 사용됩니다."

두 버퍼 알고리즘:

```
1. 입력을 경로로 초기화, 출력을 빈 문자열로 초기화
2. 입력이 비어 있지 않은 동안:
   A. "../" 또는 "./" 접두사 제거
   B. "/./" 또는 "/."(끝)을 "/"로 대체
   C. "/../" 또는 "/.."(끝)에 대해 마지막 출력 세그먼트 제거
   D. 단독 "." 또는 ".." 제거
   E. 첫 번째 세그먼트(다음 "/" 포함 또는 끝까지)를 출력으로 이동
```

상세 단계:

```
A. 입력 버퍼가 "../" 또는 "./" 접두사로 시작하면
   해당 접두사를 입력 버퍼에서 제거

B. 입력 버퍼가 "/./" 접두사로 시작하면
   해당 접두사를 "/"로 대체
   또는 입력 버퍼가 "/."이고 "."가 완전한 경로 세그먼트이면
   "."을 제거

C. 입력 버퍼가 "/../" 접두사로 시작하면
   해당 접두사를 "/"로 대체하고 출력 버퍼의 마지막 세그먼트 제거
   또는 입력 버퍼가 "/.."이고 ".."가 완전한 경로 세그먼트이면
   ".."를 제거하고 출력 버퍼의 마지막 세그먼트 제거

D. 입력 버퍼가 "." 또는 ".."만으로 구성되어 있으면
   입력 버퍼에서 제거

E. 첫 번째 경로 세그먼트를 입력 버퍼에서 출력 버퍼로 이동
   (초기 "/" 문자(있는 경우)와 다음 "/" 문자까지 또는 입력 버퍼 끝까지)
```

예시:

| 입력 경로 | 결과 |
|----------|------|
| `/a/b/c/./../../g` | `/a/g` |
| `mid/content=5/../6` | `mid/6` |
| `/../../../g` | `/g` |
| `./g` | `g` |

### 6.3 구성요소 재조합 (Recomposition)

해석 후 구성요소를 재조합하여 대상 URI를 형성합니다:

```
result = ""

if defined(scheme) then
   append scheme + ":" to result

if defined(authority) then
   append "//" + authority to result

append path to result

if defined(query) then
   append "?" + query to result

if defined(fragment) then
   append "#" + fragment to result

return result
```

### 6.4 해석 예시

기본 URI: `http://a/b/c/d;p?q`

#### 정상 예시

| 참조 | 대상 URI |
|------|----------|
| `g` | `http://a/b/c/g` |
| `./g` | `http://a/b/c/g` |
| `g/` | `http://a/b/c/g/` |
| `/g` | `http://a/g` |
| `//g` | `http://g` |
| `?y` | `http://a/b/c/d;p?y` |
| `g?y` | `http://a/b/c/g?y` |
| `#s` | `http://a/b/c/d;p?q#s` |
| `g#s` | `http://a/b/c/g#s` |
| `g?y#s` | `http://a/b/c/g?y#s` |
| `;x` | `http://a/b/c/;x` |
| `g;x` | `http://a/b/c/g;x` |
| `g;x?y#s` | `http://a/b/c/g;x?y#s` |
| `""` (빈 문자열) | `http://a/b/c/d;p?q` |
| `.` | `http://a/b/c/` |
| `./` | `http://a/b/c/` |
| `..` | `http://a/b/` |
| `../` | `http://a/b/` |
| `../g` | `http://a/b/g` |
| `../..` | `http://a/` |
| `../../` | `http://a/` |
| `../../g` | `http://a/g` |

#### 비정상 예시

| 참조 | 대상 URI | 설명 |
|------|----------|------|
| `/../../../g` | `http://a/g` | 초과 `..`는 루트에서 멈춤 |
| `./g.` | `http://a/b/c/g.` | 점이 완전한 세그먼트가 아님 |
| `./../g` | `http://a/b/g` | |
| `./g/.` | `http://a/b/c/g/` | |
| `g/./h` | `http://a/b/c/g/h` | |
| `g/../h` | `http://a/b/c/h` | |
| `g;x=1/./y` | `http://a/b/c/g;x=1/y` | |
| `g;x=1/../y` | `http://a/b/c/y` | |
| `g?y/./x` | `http://a/b/c/g?y/./x` | 쿼리 내 점은 처리 안 됨 |
| `g?y/../x` | `http://a/b/c/g?y/../x` | 쿼리 내 점은 처리 안 됨 |
| `g#s/./x` | `http://a/b/c/g#s/./x` | 프래그먼트 내 점은 처리 안 됨 |
| `g#s/../x` | `http://a/b/c/g#s/../x` | 프래그먼트 내 점은 처리 안 됨 |

---

## 7. 정규화 및 비교 (Normalization and Comparison)

### 7.1 동등성 정의

> "URI는 리소스를 식별하기 위해 존재하므로, 동일한 리소스를 식별할 때 동등한 것으로 간주되어야 합니다."

실제 구현에서는 정확성과 계산 비용 사이의 다양한 절충을 가진 여러 비교 접근 방식이 필요합니다.

### 7.2 비교 방법

#### 7.2.1 단순 문자열 비교

가장 빠른 방법: 정확한 문자별 매칭.

```
http://example.com/data  =  http://example.com/data   ✅
http://example.com/data  ≠  http://EXAMPLE.COM/data   ❌ (같은 리소스임에도)
```

여러 표현으로 인해 유용성이 제한됩니다.

#### 7.2.2 구문 기반 정규화

대소문자 정규화:
- 스킴과 호스트: 소문자로 변환
- 퍼센트 인코딩: 대문자 16진수 사용

```
HTTP://Example.COM/  →  http://example.com/
http://example.com/%7e  →  http://example.com/%7E
```

퍼센트 인코딩 정규화:
- 비예약 문자 디코딩: `%41`-`%5A`, `%61`-`%7A`, `%30`-`%39`, `%2D`, `%2E`, `%5F`, `%7E`
- 데이터로 나타나는 예약 문자 인코딩

```
http://example.com/%41%42%43  →  http://example.com/ABC
http://example.com/~user      =  http://example.com/%7Euser
```

경로 세그먼트 정규화:
- 불필요한 점-세그먼트 제거
- 슬래시 병합 (구현별)

```
http://example.com/a/./b/../c  →  http://example.com/a/c
http://example.com//a///b      →  http://example.com/a/b (스킴별)
```

#### 7.2.3 스킴 기반 정규화

개별 스킴은 추가 정규화 규칙을 정의할 수 있습니다:
- 기본 포트 추가 또는 생략
- 스킴별 데이터의 대소문자 변환
- 스킴별 구성요소의 퍼센트 인코딩 정규화

예시:
```
http://example.com:80/      →  http://example.com/      (기본 포트 생략)
http://example.com          →  http://example.com/      (빈 경로를 "/"로)
https://example.com:443/    →  https://example.com/     (기본 포트 생략)
```

#### 7.2.4 프로토콜 기반 정규화

실제 리소스 접근이 필요할 수 있습니다:
- 호스트 동등성을 위한 DNS 조회
- HTTP 콘텐츠 협상 해석
- 리다이렉트 따라가기

예시:
```
http://www.example.com/  =?=  http://example.com/  (DNS 확인 필요)
http://example.com/page  =?=  http://example.com/page/  (서버 응답에 따라)
```

### 7.3 정규화 비교 표

| 방법 | 성능 | 정확도 | 리소스 접근 |
|------|------|--------|-------------|
| 단순 문자열 비교 | 최고 | 최저 | 불필요 |
| 구문 기반 정규화 | 높음 | 중간 | 불필요 |
| 스킴 기반 정규화 | 중간 | 높음 | 불필요 |
| 프로토콜 기반 정규화 | 낮음 | 최고 | 필요 |

---

## 8. 보안 고려사항

### 8.1 신뢰성과 일관성

> "구현체는 스킴별 구문에 대한 가정 없이 일반 구문에 따라 URI를 파싱해야 합니다(MUST)."

### 8.2 악의적 구성

> "파서는 URI가 사용자를 오도하도록 구성될 수 있음을 인식해야 합니다... 스킴별 제한을 위반하는 URI 참조는 사용되지 않는 부분을 무시하는 대신 오류로 표시해야 합니다."

예시:
```
http://trusted.com@evil.com/     ← userinfo를 통한 오도
http://trusted.com%2F@evil.com/  ← 인코딩을 통한 오도
```

### 8.3 백엔드 트랜스코딩

> "트랜스코딩 서버는 리소스의 미디어 타입을 변경하는 데 사용될 수 있으며, 잠재적으로 리소스 콘텐츠의 잘못된 해석을 초래할 수 있습니다."

### 8.4 희귀 IP 주소 형식

> "일부 플랫폼은 이 명세에 정의된 점-십진 표기법보다 더 관대한 IPv4 주소 해석을 허용한다는 점에 주의하십시오."

예시:
```
http://0x7f.0x0.0x0.0x1/    ← 16진수 형식 (127.0.0.1)
http://2130706433/          ← 32비트 정수 형식 (127.0.0.1)
http://0177.0.0.1/          ← 8진수 형식 (127.0.0.1)
```

### 8.5 민감한 정보

> "URI는 종종 암호화되지 않은 형태로 전송, 저장, 로깅됩니다. 민감한 정보는 URI에 나타나서는 안 됩니다(MUST NOT)."

피해야 할 것들:
- 비밀번호
- 세션 토큰
- 개인 식별 정보
- API 키

### 8.6 의미적 공격 (Semantic Attacks)

시각적 유사성 공격은 URI가 동일하게 보이지만 다른 리소스를 참조할 때 발생합니다.

> "사용자 피드백을 위해 URI를 렌더링하는 애플리케이션(예: 그래픽 하이퍼텍스트 브라우징)은 가능한 경우 userinfo를 URI의 나머지 부분과 구별되는 방식으로 렌더링해야 합니다."

예시:
```
http://www.paypal.com@evil.com/           ← 권한 혼동
http://www.pаypal.com/                    ← 키릴 문자 'а' 사용 (U+0430)
http://www.paypa1.com/                    ← 숫자 '1' 대신 문자 'l'
```

대응책:
- Punycode 변환 표시
- 의심스러운 userinfo 강조
- IDN 호모그래프 검사

---

## 9. IANA 고려사항

URI 스킴 등록은 BCP 35에 따라 별도로 관리됩니다. 레지스트리는 스킴 이름을 명세에 매핑합니다.

등록된 스킴 예시:
| 스킴 | 참조 | 설명 |
|------|------|------|
| http | RFC 7230 | 하이퍼텍스트 전송 프로토콜 |
| https | RFC 7230 | 보안 HTTP |
| ftp | RFC 1738 | 파일 전송 프로토콜 |
| mailto | RFC 6068 | 이메일 주소 |
| file | RFC 8089 | 로컬 파일 |
| data | RFC 2397 | 인라인 데이터 |
| urn | RFC 8141 | Uniform Resource Name |

---

## 부록 A: 수집된 ABNF 문법

### 핵심 규칙

```abnf
URI           = scheme ":" hier-part [ "?" query ] [ "#" fragment ]

hier-part     = "//" authority path-abempty
              / path-absolute
              / path-rootless
              / path-empty

URI-reference = URI / relative-ref

absolute-URI  = scheme ":" hier-part [ "?" query ]

relative-ref  = relative-part [ "?" query ] [ "#" fragment ]

relative-part = "//" authority path-abempty
              / path-absolute
              / path-noscheme
              / path-empty

scheme        = ALPHA *( ALPHA / DIGIT / "+" / "-" / "." )

authority     = [ userinfo "@" ] host [ ":" port ]
userinfo      = *( unreserved / pct-encoded / sub-delims / ":" )
host          = IP-literal / IPv4address / reg-name
port          = *DIGIT

IP-literal    = "[" ( IPv6address / IPvFuture ) "]"

IPvFuture     = "v" 1*HEXDIG "." 1*( unreserved / sub-delims / ":" )

IPv6address   =                            6( h16 ":" ) ls32
              /                       "::" 5( h16 ":" ) ls32
              / [               h16 ] "::" 4( h16 ":" ) ls32
              / [ *1( h16 ":" ) h16 ] "::" 3( h16 ":" ) ls32
              / [ *2( h16 ":" ) h16 ] "::" 2( h16 ":" ) ls32
              / [ *3( h16 ":" ) h16 ] "::"    h16 ":"   ls32
              / [ *4( h16 ":" ) h16 ] "::"              ls32
              / [ *5( h16 ":" ) h16 ] "::"              h16
              / [ *6( h16 ":" ) h16 ] "::"

h16           = 1*4HEXDIG
ls32          = ( h16 ":" h16 ) / IPv4address

IPv4address   = dec-octet "." dec-octet "." dec-octet "." dec-octet

dec-octet     = DIGIT                 ; 0-9
              / %x31-39 DIGIT         ; 10-99
              / "1" 2DIGIT            ; 100-199
              / "2" %x30-34 DIGIT     ; 200-249
              / "25" %x30-35          ; 250-255

reg-name      = *( unreserved / pct-encoded / sub-delims )

path          = path-abempty    ; "/"로 시작하거나 비어 있음
              / path-absolute   ; "/"로 시작하지만 "//"는 아님
              / path-noscheme   ; 콜론 없는 세그먼트로 시작
              / path-rootless   ; 세그먼트로 시작
              / path-empty      ; 문자 없음

path-abempty  = *( "/" segment )
path-absolute = "/" [ segment-nz *( "/" segment ) ]
path-noscheme = segment-nz-nc *( "/" segment )
path-rootless = segment-nz *( "/" segment )
path-empty    = 0<pchar>

segment       = *pchar
segment-nz    = 1*pchar
segment-nz-nc = 1*( unreserved / pct-encoded / sub-delims / "@" )
              ; 콜론 ":"이 없는 비어 있지 않은 세그먼트

pchar         = unreserved / pct-encoded / sub-delims / ":" / "@"

query         = *( pchar / "/" / "?" )

fragment      = *( pchar / "/" / "?" )

pct-encoded   = "%" HEXDIG HEXDIG

unreserved    = ALPHA / DIGIT / "-" / "." / "_" / "~"

reserved      = gen-delims / sub-delims

gen-delims    = ":" / "/" / "?" / "#" / "[" / "]" / "@"

sub-delims    = "!" / "$" / "&" / "'" / "(" / ")"
              / "*" / "+" / "," / ";" / "="
```

---

## 부록 B: URI 파싱 정규 표현식

다음 정규 표현식을 사용하여 URI 참조를 다섯 가지 주요 구성요소로 분해할 수 있습니다:

```
^(([^:/?#]+):)?(//([^/?#]*))?([^?#]*)(\?([^#]*))?(#(.*))?
 12            3  4          5. path   6  7      8 9
```

그룹 매핑:
| 그룹 | 구성요소 |
|------|----------|
| $2 | scheme |
| $4 | authority |
| $5 | path |
| $7 | query |
| $9 | fragment |

예시: `http://www.ics.uci.edu/pub/ietf/uri/#Related`

| 구성요소 | 값 |
|----------|-----|
| scheme | http |
| authority | www.ics.uci.edu |
| path | /pub/ietf/uri/ |
| query | (undefined) |
| fragment | Related |

---

## 부록 C: URI와 비ASCII 문자 구분

### C.1 구분자로서의 공백

공백 문자(SP, 탭, 줄바꿈 등)는 종종 주변 텍스트에서 URI를 구분하는 데 사용됩니다. URI 내에서 공백은 `%20`으로 퍼센트 인코딩되어야 합니다.

### C.2 꺾쇠 괄호

텍스트 내에서 URI를 명확하게 구분하기 위해 꺾쇠 괄호로 URI를 묶는 것이 권장됩니다:

```
자세한 내용은 <http://example.com/info>를 참조하세요.
```

### C.3 이중 따옴표

꺾쇠 괄호를 사용할 수 없는 경우 이중 따옴표로 URI를 묶을 수 있습니다:

```
"http://example.com/path with spaces"
```

---

## 부록 D: 변경 사항 (RFC 2396 대비)

RFC 2396에서 RFC 3986으로의 주요 변경사항:

| 변경 항목 | 설명 |
|----------|------|
| IPv6 지원 | IP-literal을 통한 IPv6 주소 지원 추가 |
| 정규화 | 정규화 및 비교 방법 상세화 |
| 참조 해석 | 상대 참조 해석 알고리즘 개선 |
| 보안 | 보안 고려사항 확장 |
| ABNF | 명확하고 완전한 ABNF 문법 제공 |
| 문자 집합 | 비예약 문자 집합 조정 (~, -, ., _) |

---

## 핵심 요약

### 1. 범용 설계

URI는 ASCII 문자를 사용하여 전역적인 전사를 가능하게 하면서, UTF-8 + 퍼센트 인코딩을 통해 국제 데이터를 지원합니다.

### 2. 구성요소 계층

```
┌─────────────────────────────────────────────────────────┐
│ scheme : // authority / path ? query # fragment         │
├─────────────────────────────────────────────────────────┤
│ 스킴이 해석을 결정                                       │
│ 권한이 책임 있는 엔티티를 식별                            │
│ 경로가 계층적으로 리소스를 식별                           │
│ 쿼리가 비계층적 데이터를 제공                            │
│ 프래그먼트가 보조 리소스를 식별                          │
└─────────────────────────────────────────────────────────┘
```

### 3. 유연한 참조

상대 참조는 이식 가능한 문서 컬렉션을 가능하게 하며, 참조 해석 알고리즘은 일관된 해석을 제공합니다.

### 4. 보안 초점

- 퍼센트 인코딩이 예약 문자를 보호
- userinfo 폐기로 평문 자격 증명 억제
- 악의적 구성 경고로 구현자에게 정보 제공

### 5. 확장성

일반 구문은 수정 없이 미래 스킴을 수용하며, 스킴별 명세가 제약을 추가할 수 있습니다.

---

## 실제 활용 예시

### URL 파싱 예시 (JavaScript)

```javascript
function parseURI(uri) {
  const regex = /^(([^:/?#]+):)?(\/\/([^/?#]*))?([^?#]*)(\?([^#]*))?(#(.*))?/;
  const matches = uri.match(regex);

  return {
    scheme: matches[2],
    authority: matches[4],
    path: matches[5],
    query: matches[7],
    fragment: matches[9]
  };
}

// 예시
console.log(parseURI('https://example.com:8080/path/to/resource?key=value#section'));
// {
//   scheme: 'https',
//   authority: 'example.com:8080',
//   path: '/path/to/resource',
//   query: 'key=value',
//   fragment: 'section'
// }
```

### 상대 참조 해석 예시

```javascript
function resolveReference(base, reference) {
  // 단순화된 예시 - 실제 구현은 RFC 3986 알고리즘 전체를 따라야 함
  const baseURI = new URL(base);
  return new URL(reference, baseURI).href;
}

// 예시
const base = 'http://example.com/a/b/c';
console.log(resolveReference(base, '../d'));    // http://example.com/a/d
console.log(resolveReference(base, '/e'));      // http://example.com/e
console.log(resolveReference(base, '?q=1'));    // http://example.com/a/b/c?q=1
console.log(resolveReference(base, '#frag'));   // http://example.com/a/b/c#frag
```

### 퍼센트 인코딩 예시

```javascript
// 인코딩
function percentEncode(str) {
  return encodeURIComponent(str);
}

// 디코딩
function percentDecode(str) {
  return decodeURIComponent(str);
}

// 예시
console.log(percentEncode('안녕하세요'));  // %EC%95%88%EB%85%95%ED%95%98%EC%84%B8%EC%9A%94
console.log(percentEncode('Hello World')); // Hello%20World
console.log(percentDecode('%48%65%6C%6C%6F')); // Hello
```

---

## 참고 자료

- [RFC 3986 원문 (IETF)](https://datatracker.ietf.org/doc/html/rfc3986)
- [RFC 3986 (RFC Editor)](https://www.rfc-editor.org/rfc/rfc3986)
- [RFC 1738 - URL](https://datatracker.ietf.org/doc/html/rfc1738)
- [RFC 2396 - URI 일반 구문 (폐기됨)](https://datatracker.ietf.org/doc/html/rfc2396)
- [BCP 35 - URI 스킴 등록 지침](https://www.rfc-editor.org/info/bcp35)

---

## 관련 RFC

| RFC | 제목 | 관계 |
|-----|------|------|
| RFC 1738 | Uniform Resource Locators (URL) | 갱신됨 |
| RFC 2396 | URI 일반 구문 | 폐기됨 |
| RFC 2732 | URL에서 IPv6 주소의 형식 | 폐기됨 |
| RFC 1808 | 상대 URL | 폐기됨 |
| RFC 7230 | HTTP/1.1: 메시지 구문 및 라우팅 | URI 사용 |
| RFC 8089 | "file" URI 스킴 | 스킴 정의 |
| RFC 6068 | "mailto" URI 스킴 | 스킴 정의 |
| RFC 8141 | URN 구문 | 스킴 정의 |

---

*이 문서는 RFC 3986의 한국어 번역 및 정리본입니다.*
