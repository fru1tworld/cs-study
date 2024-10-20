

Network Working Group                                           G. Klyne
Request for Comments: 3339                        Clearswift Corporation
Category: Standards Track                                      C. Newman
                                                        Sun Microsystems
                                                               July 2002


# 인터넷에서의 날짜와 시간: 타임스탬프

## 이 메모의 상태

이 문서는 인터넷 커뮤니티를 위한 인터넷 표준 트랙 프로토콜을 명시하며, 개선을 위한 논의와 제안을 요청합니다. 이 프로토콜의 표준화 상태 및 상태에 대해서는 "Internet Official Protocol Standards" (STD 1)의 현재판을 참조하십시오. 이 메모의 배포는 무제한입니다.

## 저작권 고지

Copyright (C) The Internet Society (2002). All Rights Reserved.

## 요약

이 문서는 그레고리력을 사용한 날짜와 시간 표현을 위한 ISO 8601 표준의 프로파일로서, 인터넷 프로토콜에서 사용하기 위한 날짜 및 시간 형식을 정의합니다.

## 목차

   1. 서론
   2. 정의
   3. 두 자리 연도
   4. 지역 시간
      4.1. 협정 세계시 (UTC)
      4.2. 지역 오프셋
      4.3. 알 수 없는 지역 오프셋 규약
      4.4. 한정되지 않은 지역 시간
   5. 날짜 및 시간 형식
      5.1. 순서
      5.2. 사람의 가독성
      5.3. 드물게 사용되는 옵션
      5.4. 중복 정보
      5.5. 단순성
      5.6. 인터넷 날짜/시간 형식
      5.7. 제한사항
      5.8. 예시
   6. 참고 문헌
   7. 보안 고려사항
   부록 A. ISO 8601 수집된 ABNF
   부록 B. 요일
   부록 C. 윤년
   부록 D. 윤초
   감사의 글
   저자 주소
   전체 저작권 선언

## 1. 서론

날짜 및 시간 형식은 인터넷에서 상당한 혼란과 상호운용성 문제를 야기합니다. 이 문서는 직면한 많은 문제를 다루고, 인터넷 프로토콜에서 날짜와 시간을 표현하고 사용할 때 일관성과 상호운용성을 개선하기 위한 권고사항을 제시합니다.

이 문서는 그레고리력을 사용한 날짜와 시간 표현을 위한 ISO 8601 표준의 인터넷 프로파일을 포함합니다.

날짜와 시간 값이 인터넷 프로토콜에 나타나는 방법은 다양합니다. 이 문서는 하나의 일반적인 용례에 초점을 맞춥니다: 인터넷 프로토콜 이벤트의 타임스탬프. 이 제한된 고려에는 몇 가지 결과가 따릅니다:

   o 모든 날짜와 시간은 "현재 시대"에 있는 것으로 가정하며, 0000AD와 9999AD 사이의 어딘가에 해당합니다.

   o 표현된 모든 시간은 협정 세계시(UTC)와의 명시된 관계(오프셋)를 가집니다. 이는 지역 시간과 위치가 알려져 있지만 실제 UTC와의 관계가 정치인이나 관리자의 알려지지 않거나 알 수 없는 행동에 의존할 수 있는 스케줄링 응용 프로그램과 다릅니다. 2005년 3월 23일 뉴욕에서 17:00에 대응하는 UTC 시간은 일광 절약 시간에 대한 행정적 결정에 따라 달라질 수 있습니다. 이 명세는 그러한 고려사항을 피합니다.

   o 타임스탬프는 UTC가 도입되기 전에 발생한 시간을 표현할 수 있습니다. 그러한 타임스탬프는 명시된 시점에 이용 가능한 최선의 관행을 사용하여 세계시를 기준으로 표현됩니다.

   o 날짜와 시간 표현은 시간의 한 순간을 나타냅니다. 기간이나 간격에 대한 설명은 여기서 다루지 않습니다.

## 2. 정의

이 문서에서 "MUST", "MUST NOT", "REQUIRED", "SHALL", "SHALL NOT", "SHOULD", "SHOULD NOT", "RECOMMENDED", "MAY", "OPTIONAL"이라는 핵심 단어는 RFC 2119에 기술된 대로 해석되어야 합니다.

   UTC         국제 도량형국(BIPM)이 유지하는 협정 세계시.

   second      국제 단위계에서 시간 측정의 기본 단위. 외부 필드에 의해 방해받지 않는 바닥 상태의 세슘-133 원자의 초미세 전이에 의해 흡수되거나 방출되는 마이크로파 빛의 9,192,631,770 주기의 지속 시간으로 정의됩니다.

   minute      60초의 기간. 그러나 윤초가 분 안에서 어떻게 표시되는지에 대해서는 5.7절의 제한사항과 부록 D도 참조하십시오.

   hour        60분의 기간.

   day         24시간의 기간.

   leap year   그레고리력에서 366일이 있는 해. 윤년은 4로 정수배 나누어 떨어지는 해이며, 단 세기년(즉 100으로 나누어 떨어지는 해)인 경우 400으로도 정수배 나누어 떨어져야 합니다.

   ABNF        확장 배커스-나우르 형식, RFC 2234에 정의된 바와 같이 프로토콜이나 언어에서 허용 가능한 문자열을 표현하는 데 사용되는 형식.

   Email Date/Time Format
               RFC 2822에 정의된 바와 같이 인터넷 메일에서 사용하는 날짜/시간 형식.

   Internet Date/Time Format
               이 문서의 5절에 정의된 날짜 형식.

   Timestamp   이 문서에서 이 용어는 시간의 어떤 순간에 대한 모호하지 않은 표현을 지칭하는 데 사용됩니다.

   Z           시간에 적용될 때 00:00의 UTC 오프셋을 나타내는 접미사; "Z" 문자의 ICAO 음성 알파벳 표현에서 "Zulu"라고 종종 발음됩니다.

시간 척도에 대한 자세한 정보는 RFC 1305 (NTP)의 부록 E, ISO 8601의 3절, 그리고 적절한 ITU 문서를 참조하십시오.

## 3. 두 자리 연도

다음 요구사항은 두 자리 연도의 모호성 문제를 다룹니다:

   o 인터넷 프로토콜은 날짜에 네 자리 연도를 생성해야 합니다(MUST).

   o 두 자리 연도의 사용은 더 이상 사용되지 않습니다(deprecated). 두 자리 연도가 수신된 경우, 잘못된 해석이 프로토콜 또는 처리 실패를 야기하지 않을 때에만(예: 로깅이나 추적 목적으로만 사용될 경우) 수락되어야 합니다(SHOULD).

   o 두 자리 연도를 사용하는 프로그램이 1999년 이후의 연도를 세 자리로 표현할 수 있습니다. 이는 프로그램이 단순히 연도에서 1900을 빼고 자릿수를 확인하지 않을 때 발생합니다. 그러한 결함 있는 소프트웨어가 생성한 날짜를 견고하게 처리하고자 하는 프로그램은 세 자리 연도에 1900을 더할 수 있습니다.

   o 두 자리 연도를 사용하는 프로그램이 1999년 이후의 연도를 ":0", ":1", ... ":9", ";0", ...으로 표현할 수 있습니다. 이는 프로그램이 단순히 연도에서 1900을 빼고 10년 단위를 US-ASCII 문자 0에 더할 때 발생합니다. 그러한 결함 있는 소프트웨어가 생성한 날짜를 견고하게 처리하고자 하는 프로그램은 비숫자 10년 단위를 감지하고 적절히 해석해야 합니다.

두 자리 연도의 문제는 인터넷 프로토콜에서 사용되는 모든 날짜와 시간이 완전히 한정되어야 하는(MUST) 이유를 충분히 보여줍니다.

## 4. 지역 시간

### 4.1. 협정 세계시 (UTC)

지역 시간대의 일광 절약 규칙은 복잡하고 지역 법률에 따라 예측 불가능한 시점에 변경될 수 있으므로, 진정한 상호운용성은 협정 세계시(UTC)를 사용함으로써 가장 잘 달성됩니다. 이 명세는 지역 시간대 규칙에 대응하지 않습니다.

### 4.2. 지역 오프셋

지역 시간과 UTC 사이의 오프셋은 종종 유용한 정보입니다. 예를 들어, 전자 메일(RFC 2822)에서 지역 오프셋은 신속한 응답의 확률을 결정하기 위한 유용한 휴리스틱을 제공합니다. 알파벳 문자열로 지역 오프셋에 레이블을 붙이려는 시도는 과거에 좋지 않은 상호운용성을 초래했습니다. 그 결과, RFC 2822는 숫자 오프셋을 필수로 만들었습니다.

숫자 오프셋은 "지역 시간에서 UTC를 뺀 값"으로 계산됩니다. 따라서 UTC에서의 동등한 시간은 지역 시간에서 오프셋을 빼서 결정할 수 있습니다. 예를 들어, 18:50:00-04:00은 22:50:00Z와 같은 시간입니다. (이 예시는 오프셋의 절대값을 더하여 처리되는 음수 오프셋을 보여줍니다.)

   참고: ISO 8601을 따르면, 숫자 오프셋은 UTC와 정수 분 단위로 차이나는 시간대만 표현합니다. 그러나 많은 역사적 시간대는 UTC와 비정수 분 단위로 차이납니다. 그러한 역사적 타임스탬프를 정확히 표현하려면, 응용 프로그램은 이를 표현 가능한 시간대로 변환해야 합니다.

### 4.3. 알 수 없는 지역 오프셋 규약

UTC 시간은 알려져 있지만 지역 시간에 대한 오프셋을 알 수 없는 경우, 이를 "-00:00" 오프셋으로 표현할 수 있습니다. 이는 "Z" 또는 "+00:00" 오프셋과 의미적으로 다르며, 후자는 UTC가 지정된 시간의 선호 참조점임을 암시합니다. RFC 2822는 이메일에 대한 유사한 규약을 설명합니다.

### 4.4. 한정되지 않은 지역 시간

현재 인터넷에 연결된 많은 장치들이 내부 시계를 지역 시간으로 실행하며 UTC를 인식하지 못합니다. 인터넷은 명세를 작성할 때 현실을 받아들이는 전통이 있지만, 이것이 상호운용성을 희생하면서까지 이루어져서는 안 됩니다. 한정되지 않은 지역 시간대의 해석은 지구의 약 23/24에서 실패할 것이므로, 한정되지 않은 지역 시간의 상호운용성 문제는 인터넷에 허용할 수 없는 것으로 간주됩니다. 지역 시간으로 구성되어 있고, 대응하는 UTC 오프셋을 인식하지 못하며, 다른 인터넷 시스템과의 시간 동기화에 의존하는 시스템은 UTC와의 올바른 동기화를 보장하는 메커니즘을 사용해야 합니다(MUST). 적합한 메커니즘에는 다음이 포함됩니다:

   o 네트워크 시간 프로토콜(RFC 1305)을 사용하여 UTC 시간을 획득합니다.

   o 같은 지역 시간대에 있는 다른 호스트를 인터넷 게이트웨이로 사용합니다. 이 호스트는 다른 호스트로 전송되는 한정되지 않은 지역 시간을 반드시 보정해야 합니다(MUST).

   o 사용자에게 지역 시간대 및 일광 절약 규칙 설정을 요청합니다.

## 5. 날짜 및 시간 형식

이 절에서는 날짜 및 시간 형식의 바람직한 특성을 논의하고 인터넷 프로토콜에서 사용하기 위한 ISO 8601의 프로파일을 정의합니다.

### 5.1. 순서

날짜와 시간 구성요소가 가장 덜 정밀한 것에서 가장 정밀한 것으로 순서가 지정되면, 유용한 속성이 달성됩니다. 날짜와 시간의 시간대가 같고(예: 모두 UTC), 같은 문자열로 표현되며(예: 모두 "Z" 또는 모두 "+00:00"), 모든 시간이 같은 수의 소수 초 자릿수를 가진다고 가정하면, 날짜 및 시간 문자열은 문자열로 정렬될 수 있고(예: C의 strcmp() 함수를 사용하여) 시간순 시퀀스가 결과로 나타납니다. 선택적 구두점의 존재는 이 특성을 위반할 것입니다.

### 5.2. 사람의 가독성

사람의 가독성은 인터넷 프로토콜의 귀중한 기능으로 입증되었습니다. 사람이 읽을 수 있는 프로토콜은 telnet이 종종 테스트 클라이언트로 충분하고 네트워크 분석기가 프로토콜에 대한 지식으로 수정될 필요가 없으므로 디버깅 비용을 크게 줄입니다. 반면에, 사람의 가독성은 때때로 상호운용성 문제를 초래합니다. 예를 들어, 날짜 형식 "10/11/1996"은 나라마다 다르게 해석되기 때문에 전 세계적 교환에 완전히 부적합합니다. 또한, RFC 822의 날짜 형식은 사람들이 어떤 텍스트 문자열이든 허용된다고 가정하고 세 글자 약어를 다른 언어로 번역하거나 생성하기 더 쉬운 날짜 형식(예: C 함수 ctime이 사용하는 형식)으로 대체했을 때 상호운용성 문제를 초래했습니다. 이러한 이유로, 사람의 가독성과 상호운용성 사이에 균형이 이루어져야 합니다.

모든 나라의 관습에 따라 읽을 수 있는 날짜 및 시간 형식은 없으므로, 인터넷 클라이언트는 날짜를 해당 지역에 적합한 표시 형식으로 변환할 준비가 되어 있어야 합니다(SHOULD). 이에는 UTC를 지역 시간으로 변환하는 것이 포함될 수 있습니다.

### 5.3. 드물게 사용되는 옵션

드물게 사용되는 옵션을 포함하는 형식은 상호운용성 문제를 야기할 가능성이 높습니다. 이는 드물게 사용되는 옵션이 알파 또는 베타 테스트에서 사용될 가능성이 낮아 파싱의 버그가 발견될 가능성이 낮기 때문입니다. 드물게 사용되는 옵션은 상호운용성을 위해 가능한 한 필수로 만들거나 생략되어야 합니다.

아래에 정의된 형식은 드물게 사용되는 옵션을 하나만 포함합니다: 초의 소수부. 이는 날짜/시간 스탬프의 엄격한 순서를 요구하거나 비정상적인 정밀도 요구사항이 있는 응용 프로그램에서만 사용될 것으로 예상됩니다.

### 5.4. 중복 정보

날짜/시간 형식에 중복 정보가 포함되면, 중복 정보가 일치하지 않을 가능성이 생깁니다. 예를 들어, 날짜/시간 형식에 요일을 포함하면 요일이 올바르지 않지만 날짜는 올바르거나, 그 반대일 가능성이 생깁니다. 날짜에서 요일을 계산하는 것이 어렵지 않으므로(부록 B 참조), 날짜/시간 형식에 요일을 포함하지 않아야 합니다.

### 5.5. 단순성

ISO 8601에 명시된 날짜 및 시간 형식의 전체 집합은 다중 표현과 부분 표현을 제공하려는 시도에서 상당히 복잡합니다. 부록 A는 ISO 8601의 전체 구문을 ABNF로 번역하려는 시도를 포함합니다. 인터넷 프로토콜은 다소 다른 요구사항을 가지며 단순성은 중요한 특성으로 입증되었습니다. 또한, 인터넷 프로토콜은 진정한 상호운용성을 달성하기 위해 일반적으로 데이터의 완전한 명세가 필요합니다. 따라서 ISO 8601의 전체 문법은 대부분의 인터넷 프로토콜에 너무 복잡한 것으로 간주됩니다.

다음 절에서는 인터넷에서 사용하기 위한 ISO 8601의 프로파일을 정의합니다. 이는 ISO 8601 확장 형식의 적합한 부분집합입니다. 대부분의 필드와 구두점을 필수로 만들어 단순성을 달성합니다.

### 5.6. 인터넷 날짜/시간 형식

다음 ISO 8601 날짜 프로파일은 인터넷의 새로운 프로토콜에서 사용되어야 합니다(SHOULD). 이는 RFC 2234에 정의된 구문 설명 표기법을 사용하여 명시됩니다.

```
   date-fullyear   = 4DIGIT
   date-month      = 2DIGIT  ; 01-12
   date-mday       = 2DIGIT  ; 01-28, 01-29, 01-30, 01-31
                              ; 월/연도에 따라
   time-hour       = 2DIGIT  ; 00-23
   time-minute     = 2DIGIT  ; 00-59
   time-second     = 2DIGIT  ; 00-58, 00-59, 00-60
                              ; 윤초 규칙에 따라
   time-secfrac    = "." 1*DIGIT
   time-numoffset  = ("+" / "-") time-hour ":" time-minute
   time-offset     = "Z" / time-numoffset

   partial-time    = time-hour ":" time-minute ":" time-second
                     [time-secfrac]
   full-date       = date-fullyear "-" date-month "-" date-mday
   full-time       = partial-time time-offset

   date-time       = full-date "T" full-time
```

   참고: RFC 2234와 ISO 8601에 따라, 이 구문에서 "T"와 "Z" 문자는 각각 소문자 "t" 또는 "z"로 대체될 수 있습니다.

   이 날짜/시간 형식은 대문자 'A'-'Z'와 소문자 'a'-'z'를 구별하는 일부 환경이나 컨텍스트(예: XML)에서 사용될 수 있습니다. 그러한 환경에서 이 형식을 사용하는 명세는 날짜/시간 구문에 사용되는 문자 'T'와 'Z'가 항상 대문자여야 하도록 날짜/시간 구문을 추가로 제한할 수 있습니다(MAY). 이 형식을 생성하는 응용 프로그램은 대문자를 사용해야 합니다(SHOULD).

   참고: ISO 8601은 날짜와 시간을 "T"로 구분하여 정의합니다. 이 구문을 사용하는 응용 프로그램은 가독성을 위해 full-date와 full-time을 (예를 들어) 공백 문자로 구분하도록 선택할 수 있습니다.

### 5.7. 제한사항

문법 요소 date-mday는 현재 월 내의 일자 번호를 나타냅니다. 최대값은 다음과 같이 월과 연도에 따라 달라집니다:

```
   월 번호     월/연도             date-mday 최대값
   --------    ---------           ----------------
   01          1월                 31
   02          2월, 평년           28
   02          2월, 윤년           29
   03          3월                 31
   04          4월                 30
   05          5월                 31
   06          6월                 30
   07          7월                 31
   08          8월                 31
   09          9월                 30
   10          10월                31
   11          11월                30
   12          12월                31
```

부록 C는 연도가 윤년인지 결정하기 위한 샘플 C 코드를 포함합니다.

문법 요소 time-second는 윤초가 발생하는 월의 끝에 "60" 값을 가질 수 있습니다 -- 현재까지: 6월 (XXXX-06-30T23:59:60Z) 또는 12월 (XXXX-12-31T23:59:60Z); 윤초 표는 부록 D를 참조하십시오. 윤초가 차감되는 것도 가능하며, 그때 time-second의 최대값은 "58"입니다. 그 외 모든 시점에서 time-second의 최대값은 "59"입니다. 또한, "Z" 이외의 시간대에서는 윤초 지점이 시간대 오프셋만큼 이동합니다(따라서 전 세계적으로 같은 순간에 발생합니다).

윤초는 먼 미래까지 예측할 수 없습니다. 국제 지구 자전 서비스는 윤초를 몇 주 전 경고와 함께 공고하는 공보를 발행합니다. 응용 프로그램은 윤초가 공고될 때까지 삽입된 윤초를 포함하는 타임스탬프를 생성하지 않아야 합니다.

ISO 8601은 시간이 "24"인 것을 허용하지만, 이 ISO 8601 프로파일은 혼란을 줄이기 위해 시간에 대해 "00"과 "23" 사이의 값만 허용합니다.

### 5.8. 예시

다음은 인터넷 날짜/시간 형식의 몇 가지 예시입니다.

```
   1985-04-12T23:20:50.52Z
```

이것은 UTC로 1985년 4월 12일 23시 이후 20분 50.52초를 나타냅니다.

```
   1996-12-19T16:39:57-08:00
```

이것은 UTC에서 -08:00 오프셋(미국 태평양 표준시)으로 1996년 12월 19일 16시 이후 39분 57초를 나타냅니다. 이것은 UTC로 1996-12-20T00:39:57Z와 동일하다는 점에 유의하십시오.

```
   1990-12-31T23:59:60Z
```

이것은 1990년 말에 삽입된 윤초를 나타냅니다.

```
   1990-12-31T15:59:60-08:00
```

이것은 UTC보다 8시간 뒤인 태평양 표준시로 같은 윤초를 나타냅니다.

```
   1937-01-01T12:00:27.87+00:20
```

이것은 네덜란드 시간으로 1937년 1월 1일 정오와 같은 순간을 나타냅니다. 네덜란드의 표준시는 1909-05-01부터 1937-06-30까지 법률에 의해 UTC보다 정확히 19분 32.13초 앞서 있었습니다. 이 시간대는 HH:MM 형식을 사용하여 정확하게 표현할 수 없으며, 이 타임스탬프는 가장 가까운 표현 가능한 UTC 오프셋을 사용합니다.

## 6. 참고 문헌

   [ZELLER]    Zeller, C., "Kalender-Formeln", Acta Mathematica, Vol.
               9, Nov 1886.

   [IMAIL]     Crocker, D., "Standard for the Format of Arpa Internet
               Text Messages", STD 11, RFC 822, August 1982.

   [IMAIL-UPDATE]
               Resnick, P., "Internet Message Format", RFC 2822, April
               2001.

   [ABNF]      Crocker, D. and P. Overell, "Augmented BNF for Syntax
               Specifications: ABNF", RFC 2234, November 1997.

   [ISO8601]   "Data elements and interchange formats -- Information
               interchange -- Representation of dates and times", ISO
               8601:1988(E), International Organization for
               Standardization, June, 1988.

   [ISO8601:2000]
               "Data elements and interchange formats -- Information
               interchange -- Representation of dates and times", ISO
               8601:2000, International Organization for
               Standardization, December, 2000.

   [HOST-REQ]  Braden, R., "Requirements for Internet Hosts --
               Application and Support", STD 3, RFC 1123, October 1989.

   [IERS]      International Earth Rotation Service Bulletins,
               <http://hpiers.obspm.fr/eop-pc/products/bulletins.html>.

   [NTP]       Mills, D, "Network Time Protocol (Version 3)
               Specification, Implementation and Analysis", RFC 1305,
               March 1992.

   [ITU-R-TF]  International Telecommunication Union Recommendations
               for Time Signals and Frequency Standards Emissions.
               <http://www.itu.ch/publications/itu-r/iturtf.htm>

   [RFC2119]   Bradner, S, "Key words for use in RFCs to Indicate
               Requirement Levels", BCP 14, RFC 2119, March 1997.

## 7. 보안 고려사항

사이트의 지역 시간대는 시스템이 덜 모니터링되고 보안 탐색에 더 취약할 수 있는 시간을 결정하는 데 유용할 수 있으므로, 일부 사이트는 UTC로만 시간을 내보내기를 원할 수 있습니다. 다른 사이트들은 이를 편집증에 의한 유용한 기능의 상실로 간주할 수 있습니다.

## 부록 A. ISO 8601 수집된 ABNF

이 정보는 ISO 8601의 1988년 버전에 기반합니다. 2000년 개정판에서 일부 변경이 있을 수 있습니다.

ISO 8601은 자신이 정의하는 날짜 및 시간 형식에 대한 공식 문법을 명시하지 않습니다. 다음은 ISO 8601에서 공식 문법을 만들려는 시도입니다. 이는 정보 제공 목적으로만 사용되며 오류를 포함할 수 있습니다. ISO 8601이 권위 있는 참조로 남습니다.

ISO 8601의 모호성으로 인해 일부 해석이 이루어져야 했습니다. 첫째, ISO 8601은 기본 형식과 확장 형식의 혼합이 허용되는지 명확하지 않습니다. 이 문법은 혼합을 허용합니다. ISO 8601은 24시가 분과 초가 0인 경우에만 허용되는지 명확하지 않습니다. 이것은 24시가 어떤 컨텍스트에서든 허용되는 것으로 가정합니다. 5.7절의 date-mday에 대한 제한이 적용됩니다. ISO 8601은 "T"가 특정 상황에서 생략될 수 있다고 명시합니다. 이 문법은 모호성을 피하기 위해 "T"를 필수로 합니다. ISO 8601은 또한 (5.3.1.3절에서) 소수부가 1 미만인 경우 "0"이 앞에 와야 한다고 요구합니다. ISO 8601의 부록 B.2는 소수부 앞에 "0"이 오지 않는 예시를 제공합니다. 이 문법은 5.3.1.3절이 올바르고 부록 B.2가 오류라고 가정합니다.

```
   날짜:

   date-century    = 2DIGIT  ; 00-99
   date-decade     =  DIGIT  ; 0-9
   date-subdecade  =  DIGIT  ; 0-9
   date-year       = date-decade date-subdecade
   date-fullyear   = date-century date-year
   date-month      = 2DIGIT  ; 01-12
   date-wday       =  DIGIT  ; 1-7  ; 1은 월요일, 7은 일요일
   date-mday       = 2DIGIT  ; 01-28, 01-29, 01-30, 01-31
                              ; 월/연도에 따라
   date-yday       = 3DIGIT  ; 001-365, 001-366 연도에 따라
   date-week       = 2DIGIT  ; 01-52, 01-53 연도에 따라

   datepart-fullyear = [date-century] date-year ["-"]
   datepart-ptyear   = "-" [date-subdecade ["-"]]
   datepart-wkyear   = datepart-ptyear / datepart-fullyear

   dateopt-century   = "-" / date-century
   dateopt-fullyear  = "-" / datepart-fullyear
   dateopt-year      = "-" / (date-year ["-"])
   dateopt-month     = "-" / (date-month ["-"])
   dateopt-week      = "-" / (date-week ["-"])
   datespec-full     = datepart-fullyear date-month ["-"] date-mday
   datespec-year     = date-century / dateopt-century date-year
   datespec-month    = "-" dateopt-year date-month [["-"] date-mday]
   datespec-mday     = "--" dateopt-month date-mday
   datespec-week     = datepart-wkyear "W"
                        (date-week / dateopt-week date-wday)
   datespec-wday     = "---" date-wday
   datespec-yday     = dateopt-fullyear date-yday

   date              = datespec-full / datespec-year
                        / datespec-month /
   datespec-mday / datespec-week / datespec-wday / datespec-yday
```

```
   시간:

   time-hour         = 2DIGIT ; 00-24
   time-minute       = 2DIGIT ; 00-59
   time-second       = 2DIGIT ; 00-58, 00-59, 00-60
                               ; 윤초 규칙에 따라
   time-fraction     = ("," / ".") 1*DIGIT
   time-numoffset    = ("+" / "-") time-hour [[":"] time-minute]
   time-zone         = "Z" / time-numoffset

   timeopt-hour      = "-" / (time-hour [":"])
   timeopt-minute    = "-" / (time-minute [":"])

   timespec-hour     = time-hour [[":" ] time-minute [[":" ] time-second]]
   timespec-minute   = timeopt-hour time-minute [[":" ] time-second]
   timespec-second   = "-" timeopt-minute time-second
   timespec-base     = timespec-hour / timespec-minute / timespec-second

   time              = timespec-base [time-fraction] [time-zone]

   iso-date-time     = date "T" time
```

```
   기간:

   dur-second        = 1*DIGIT "S"
   dur-minute        = 1*DIGIT "M" [dur-second]
   dur-hour          = 1*DIGIT "H" [dur-minute]
   dur-time          = "T" (dur-hour / dur-minute / dur-second)
   dur-day           = 1*DIGIT "D"
   dur-week          = 1*DIGIT "W"
   dur-month         = 1*DIGIT "M" [dur-day]
   dur-year          = 1*DIGIT "Y" [dur-month]
   dur-date          = (dur-day / dur-month / dur-year) [dur-time]

   duration          = "P" (dur-date / dur-time / dur-week)
```

```
   기간(Periods):

   period-explicit   = iso-date-time "/" iso-date-time
   period-start      = iso-date-time "/" duration
   period-end        = duration "/" iso-date-time

   period            = period-explicit / period-start / period-end
```

## 부록 B. 요일

다음은 0000-03-01 이후의 날짜에 대해 요일을 구하는 데 사용할 수 있는 Zeller의 합동공식에 느슨하게 기반한 샘플 C 서브루틴입니다:

```c
   char *day_of_week(int day, int month, int year)
   {
      int cent;
      char *dayofweek[] = {
         "Sunday", "Monday", "Tuesday", "Wednesday",
         "Thursday", "Friday", "Saturday"
      };

      /* 2월이 마지막이 되도록 월을 조정 */
      month -= 2;
      if (month < 1) {
         month += 12;
         --year;
      }
      /* 세기로 분리 */
      cent = year / 100;
      year %= 100;
      return (dayofweek[((26 * month - 2) / 10 + day + year
                        + year / 4 + cent / 4 + 5 * cent) % 7]);
   }
```

## 부록 C. 윤년

다음은 연도가 윤년인지 계산하는 샘플 C 서브루틴입니다:

```c
   /* 연도가 윤년이면 0이 아닌 값을 반환합니다. 4자리 연도를
      사용해야 합니다.
    */
   int leap_year(int year)
   {
       return (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0));
   }
```

## 부록 D. 윤초

윤초에 대한 정보는 다음에서 찾을 수 있습니다: <http://tycho.usno.navy.mil/leapsec.html>. 특히, 다음과 같이 언급합니다:

"UTC에 윤초를 도입하는 결정은 국제 지구 자전 서비스(IERS)의 책임입니다. CCIR 권고에 따르면, 12월과 6월의 끝에 있는 기회가 첫 번째 우선순위를 가지며, 3월과 9월의 끝에 있는 기회가 두 번째 우선순위를 가집니다."

필요한 경우, 윤초의 삽입은 UTC에서 하루의 끝에 추가 초로 발생하며, YYYY-MM-DDT23:59:60Z 형태의 타임스탬프로 표현됩니다. 윤초는 모든 시간대에서 동시에 발생하므로, 시간대 관계에 영향을 미치지 않습니다. 윤초 시간의 일부 예시는 5.8절을 참조하십시오.

다음 표는 미국 해군 천문대에서 유지하는 표의 발췌입니다. 원본 데이터의 위치:

ftp://maia.usno.navy.mil/ser7/tai-utc.dat

이 표는 윤초의 날짜와 해당 윤초 이후의 시간 표준 TAI(윤초로 조정되지 않는)와 UTC 사이의 차이를 보여줍니다.

```
   UTC 날짜          윤초 이후 TAI - UTC
   --------          -------------------
   1972-06-30          11
   1972-12-31          12
   1973-12-31          13
   1974-12-31          14
   1975-12-31          15
   1976-12-31          16
   1977-12-31          17
   1978-12-31          18
   1979-12-31          19
   1981-06-30          20
   1982-06-30          21
   1983-06-30          22
   1985-06-30          23
   1987-12-31          24
   1989-12-31          25
   1990-12-31          26
   1992-06-30          27
   1993-06-30          28
   1994-06-30          29
   1995-12-31          30
   1997-06-30          31
   1998-12-31          32
```

## 감사의 글

다음 분들이 이 문서의 이전 판에 대해 유용한 조언을 제공했습니다: Ned Freed, Neal McBurnett, David Keegel, Markus Kuhn, Paul Eggert, Robert Elz. IETF 캘린더링/스케줄링 작업 그룹 메일링 리스트의 참가자들과 시간대 메일링 리스트의 참가자들에게도 감사드립니다.

다음 검토자들이 현재 개정판에 대해 유용한 제안을 했습니다: Tom Harsch, Markus Kuhn, Pete Resnick, Dan Kohn. Paul Eggert는 윤초와 시간대 오프셋의 미묘한 점에 관한 많은 세심한 관찰을 제공했습니다. 다음 분들이 이전 초안에 대한 수정과 개선 사항을 지적했습니다: Dr John Stockton, Jutta Degener, Joe Abley, Dan Wing.

## 저자 주소

   Chris Newman
   Sun Microsystems
   1050 Lakes Drive, Suite 250
   West Covina, CA 91790 USA

   EMail: chris.newman@sun.com


   Graham Klyne (편집자, 이번 개정)
   Clearswift Corporation
   1310 Waterside
   Arlington Business Park
   Theale, Reading  RG7 4SA
   UK

   Phone: +44 11 8903 8903
   Fax:   +44 11 8903 9000
   EMail: GK@ACM.ORG

## 전체 저작권 선언

Copyright (C) The Internet Society (2002). All Rights Reserved.

이 문서와 이 문서의 번역본은 복사되어 타인에게 제공될 수 있으며, 이 문서에 대해 논평하거나 달리 설명하거나 구현을 지원하는 파생 저작물은 어떤 종류의 제한 없이 전체 또는 일부로 작성, 복사, 출판 및 배포될 수 있습니다. 단, 위의 저작권 고지와 이 단락이 그러한 모든 복사본과 파생 저작물에 포함되어야 합니다. 그러나 이 문서 자체는 인터넷 표준 개발 목적으로 저작권에 대해 정의된 절차를 따라야 하는 경우를 제외하고는, 또는 영어 이외의 언어로 번역하기 위해 필요한 경우를 제외하고는, 저작권 고지를 제거하거나 Internet Society 또는 기타 인터넷 조직에 대한 참조를 제거하는 등의 어떤 방식으로도 수정될 수 없습니다.

위에서 부여된 제한된 권한은 영구적이며 Internet Society 또는 그 후계자나 양수인에 의해 철회되지 않습니다.

이 문서와 여기에 포함된 정보는 "있는 그대로" 제공되며, THE INTERNET SOCIETY AND THE INTERNET ENGINEERING TASK FORCE는 여기의 정보 사용이 어떤 권리도 침해하지 않을 것이라는 보증 또는 상품성이나 특정 목적에의 적합성에 대한 묵시적 보증을 포함하되 이에 국한되지 않는 모든 명시적 또는 묵시적 보증을 부인합니다.

## 감사의 글

RFC 편집자 기능에 대한 자금은 현재 Internet Society에 의해 제공됩니다.
