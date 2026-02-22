Network Working Group                                           P. Leach
Request for Comments: 4122                                     Microsoft
Category: Standards Track                                    M. Mealling
                                                Refactored Networks, LLC
                                                                 R. Salz
                                              DataPower Technology, Inc.
                                                               July 2005


        A Universally Unique IDentifier (UUID) URN Namespace
        범용 고유 식별자 (UUID) URN 네임스페이스

본 메모의 상태

   본 문서는 인터넷 커뮤니티를 위한 인터넷 표준 추적 프로토콜을 규정하며,
   개선을 위한 논의와 제안을 요청한다. 본 프로토콜의 표준화 상태 및 현황은
   "Internet Official Protocol Standards" (STD 1) 최신판을 참조하기 바란다.
   본 메모의 배포는 무제한이다.

저작권 고지

   Copyright (C) The Internet Society (2005).

초록

   본 명세는 UUID(Universally Unique IDentifier)를 위한 URN(Uniform
   Resource Name) 네임스페이스를 정의한다. UUID는 GUID(Globally Unique
   IDentifier)라고도 알려져 있다. UUID는 128비트 길이이며, 공간과 시간에
   걸쳐 고유성을 보장할 수 있다. UUID는 원래 Apollo Network Computing
   System에서 사용되었고, 이후 Open Software Foundation (OSF)의 Distributed
   Computing Environment (DCE)에서 사용되었으며, 그 후 Microsoft Windows
   플랫폼에서 사용되었다.

   본 명세는 OSF의 허가 하에 DCE 명세에서 파생된 것이다.

목차

   1.  소개 ............................................................2
   2.  동기 ............................................................3
   3.  네임스페이스 등록 템플릿 ........................................3
   4.  명세 ............................................................6
       4.1.  형식 ......................................................6
             4.1.1.  Variant ........................................... 7
             4.1.2.  레이아웃과 바이트 순서 ............................8
             4.1.3.  버전 ..............................................9
             4.1.4.  타임스탬프 ........................................9
             4.1.5.  클럭 시퀀스 ......................................10
             4.1.6.  노드 .............................................10
             4.1.7.  Nil UUID .........................................10
       4.2.  시간 기반 UUID 생성 알고리즘 .............................10
             4.2.1.  기본 알고리즘 ....................................11
             4.2.2.  생성 세부사항 ....................................13
       4.3.  이름 기반 UUID 생성 알고리즘 .............................13
       4.4.  진정한 난수 또는 의사 난수로부터 UUID를 생성하는 알고리즘..14
       4.5.  호스트를 식별하지 않는 노드 ID ............................15
   5.  커뮤니티 고려사항 ..............................................15
   6.  보안 고려사항 ..................................................16
   7.  감사의 글 ......................................................16
   8.  참고 문헌 ......................................................16
       8.1.  규범적 참고 문헌 .........................................16
       8.2.  정보성 참고 문헌 .........................................17
   부록 A - 구현 예시 .................................................18
   부록 B - 출력 예시 .................................................29
   부록 C - 미리 정의된 몇 가지 네임스페이스 ID .......................30

1.  소개

   본 명세는 UUID(Universally Unique IDentifier)를 위한 URN(Uniform
   Resource Name) 네임스페이스를 정의한다. UUID는 GUID(Globally Unique
   IDentifier)라고도 알려져 있다.

   UUID는 128비트 길이이며, 중앙 등록 절차를 필요로 하지 않는다.

   본 명세의 정보는 간결한 구현 가이드로 작성되었다. 다른 무엇보다도
   원래 UUID를 정의한 DCE 표준 [2]를 대체하거나 무시해서는 안 된다.

   본 문서에서 "반드시(MUST)", "절대 안 된다(MUST NOT)",
   "요구된다(REQUIRED)", "해야 한다(SHALL)", "해서는 안 된다(SHALL NOT)",
   "권장된다(SHOULD)", "권장되지 않는다(SHOULD NOT)",
   "추천된다(RECOMMENDED)", "할 수 있다(MAY)", "선택적이다(OPTIONAL)"라는
   핵심 용어는 [RFC 2119]에 기술된 대로 해석되어야 한다.

   UUID는 128비트 숫자에 대한 특정 표현과 시맨틱의 조합이다. 본 문서는
   이 안에 정의된 variant를 위한 바이트 레벨의 정확한 표현 외에는
   프로그래밍 언어에서 UUID를 사용하는 방식에 대한 요구사항을 부과하지
   않는다.

   UUID는 ITU-T Rec. X.667 | ISO/IEC 9834-8:2004 [3]에 의해서도
   규정된다.

2.  동기

   분산 컴퓨팅 환경 내부뿐만 아니라 그 너머에서도 고유 식별자가 필요한
   경우가 많다. UUID를 사용하는 가장 설득력 있는 이유 중 하나는 중앙
   관리 기관이 필요하지 않다는 것이다 (한 형식은 IEEE 802 노드 식별자를
   사용하지만, 다른 형식은 사용하지 않는다). 이 외에, UUID의 고정된
   크기(128비트)는 정렬, 순서 지정, 해싱, 배열로의 저장, 데이터베이스,
   일반적인 프로그래밍 중 간편한 사용에 합리적으로 작다.

   UUID는 고유하고 영속적이므로 훌륭한 URN(Uniform Resource Name)이 된다.
   등록 과정에서 어느 곳에도 연락하지 않고 새 UUID를 생성하는 고유한
   특성은 여러 URN 중에서도 가장 낮은 발행 비용 중 하나이다.

3.  네임스페이스 등록 템플릿

   Namespace ID:

      UUID

   Registration Information:

      등록일: 2003-10-01

   Declared registrant of the namespace:

      JTC 1/SC6 (ASN.1 Rapporteur Group)

   Declaration of syntactic structure:

      UUID는 공간과 시간에 걸쳐, 모든 UUID의 공간에 대해 고유한
      식별자이다. UUID는 고정 크기이고 시간 필드를 포함하므로,
      값이 순환하는 것이 가능하다 (사용되는 특정 알고리즘에 따라
      서기 3400년경). UUID는 매우 짧은 수명의 객체에 태깅하는 것부터
      네트워크를 통해 매우 영속적인 객체를 신뢰성 있게 식별하는 것까지
      다양한 목적에 사용될 수 있다.

      UUID의 내부 표현은 섹션 4에 기술된 대로 메모리 내의 특정 비트
      시퀀스이다. UUID를 URN으로 정확하게 표현하려면, 비트 시퀀스를
      문자열 표현으로 변환하는 것이 필요하다.

      각 필드는 정수로 취급되며, 가장 유효한 숫자를 먼저 하여 0으로
      채워진 16진수 문자열로 값이 출력된다. 16진수 값 "a"에서 "f"는
      소문자로 출력되며, 입력 시에는 대소문자를 구분하지 않는다.

      UUID 문자열 표현의 형식적 정의는 다음의 ABNF [7]로 제공된다:

      UUID                   = time-low "-" time-mid "-"
                               time-high-and-version "-"
                               clock-seq-and-reserved
                               clock-seq-low "-" node
      time-low               = 4hexOctet
      time-mid               = 2hexOctet
      time-high-and-version  = 2hexOctet
      clock-seq-and-reserved = hexOctet
      clock-seq-low          = hexOctet
      node                   = 6hexOctet
      hexOctet               = hexDigit hexDigit
      hexDigit =
            "0" / "1" / "2" / "3" / "4" / "5" / "6" / "7" / "8" / "9" /
            "a" / "b" / "c" / "d" / "e" / "f" /
            "A" / "B" / "C" / "D" / "E" / "F"

      다음 예시는 UUID를 URN으로 표현한 것이다:

      urn:uuid:f81d4fae-7dec-11d0-a765-00a0c91e6bf6

   Relevant ancillary documentation:

      [1] [2]

   Identifier uniqueness considerations:

      본 문서는 세 가지 알고리즘을 규정하여 UUID를 생성한다. 하나는
      시간과 함께 고유한 IEEE 802 MAC 주소를 활용하여 고유한 UUID를
      생성한다. 하나는 의사 난수 생성기를 사용하며, 세 번째는
      응용 프로그램에서 제공한 텍스트 문자열과 함께 암호화 해싱을
      사용한다. 그 결과, 각 메커니즘이 다소 다른 특성을 가지더라도,
      세 가지 모두에 대해 고유성이 보장된다.

   Identifier persistence considerations:

      UUID는 본질적으로 전역 해석에 의존하지 않으며, 이는 공간과
      시간에 걸쳐 주어진 UUID의 고유성에 의해 UUID를 만들어 배포한
      조직이 존재하지 않게 된 후에도 지속될 수 있음을 의미한다.

   Process of identifier assignment:

      UUID를 생성하는 데 등록 기관에 연락할 필요는 없다. 한 알고리즘은
      IEEE 802 MAC 주소(일반적으로 이미 사용 가능)를 요구하며, 하나는
      의사 난수 시드를 사용하고, 세 번째는 응용 프로그램에서 제공한
      텍스트 문자열과 함께 암호화 해싱을 사용한다. IEEE 802 주소는
      해당 IEEE 등록 기관에 문의하여 얻을 수 있다.

   Process for identifier resolution:

      UUID는 전역적으로 해석 가능하지 않으므로, 이는 해당되지 않는다.

   Rules for Lexical Equivalence:

      두 UUID 각각의 필드를 부호 없는 정수로 비교하여 동일한지
      판단한다. 둘은 모든 해당 필드가 같을 때만 같다. 첫 번째
      규칙의 결과로, 문자열 표현에서 대소문자를 무시한
      비교는 동등성을 결정하기 위한 어떤 비교와도 동등하다.

      UUID의 사전식 순서를 설정하기 위해 각 필드를 부호 없는
      정수로 비교하고, 유효한 필드를 먼저 비교한다.

   Conformance with URN Syntax:

      UUID의 문자열 표현은 URN 구문과 완전히 호환된다. UUID를
      URN에서 변환할 때 선행 "urn:uuid:"의 제거가 필요하며,
      역방향 변환 시에는 추가가 필요하다.

   Validation mechanism:

      UUID의 타임스탬프 부분이 미래인지(따라서 아직 할당 가능하지
      않은지) 판단하는 것 외에, UUID가 '유효'한지 판단하는 메커니즘은
      없다.

   Scope:

      UUID는 범위가 전역적이다.

4.  명세

4.1.  형식

   UUID의 형식은 16옥텟이다; 아래에 규정된 8옥텟 variant 필드의 일부
   비트가 더 세밀한 구조를 결정한다.

4.1.1.  Variant

   variant 필드는 UUID의 레이아웃을 결정한다. 즉, 해석의 다른 모든
   비트의 의미는 variant 필드의 비트 설정에 따라 달라진다. 따라서,
   정확히 말하면, variant 필드의 크기 자체가 variant에 의해 결정된다.
   variant 필드는 UUID의 옥텟 8에서 최상위 비트로 구성된다.

   다음 표는 variant 필드의 내용을 나열한다. 여기서 문자 "x"는
   "상관없음" 값을 나타낸다.

   Msb0  Msb1  Msb2  설명

    0     x     x    NCS 역호환용으로 예약됨.

    1     0     x    본 문서에 규정된 variant.

    1     1     0    Microsoft Corporation 역호환용으로 예약됨.

    1     1     1    향후 정의를 위해 예약됨.

   해당하는 곳에서, UUID의 "variant"를 참조하는 것은 옥텟 8의 비트를
   참조하는 것이다.

4.1.2.  레이아웃과 바이트 순서

   UUID 레이아웃의 최상위 비트가 먼저 표시되도록 (왼쪽에서 오른쪽으로
   읽음) 제시된다. 아래 제시된 비트 번호 매기기 체계는 해당 섹션에서
   달리 언급되지 않는 한 적용된다. UUID의 필드는 16옥텟으로 인코딩되며,
   각 필드는 가장 유효한 바이트(Most Significant Byte)를 먼저 하여
   인코딩된다 (네트워크 바이트 순서로 알려져 있음). 필드 이름, 길이,
   값, 의미에 대해서는 관련 섹션을 참조한다.

   필드                          데이터 타입     옥텟  참고
                                                  #

   time_low                      unsigned 32     0-3   타임스탬프의
                                 bit integer           하위 필드

   time_mid                      unsigned 16     4-5   타임스탬프의
                                 bit integer           중간 필드

   time_hi_and_version           unsigned 16     6-7   타임스탬프의
                                 bit integer           상위 필드에
                                                       4비트
                                                       "version"이
                                                       곱해진 것

   clock_seq_hi_and_reserved     unsigned 8       8    클럭 시퀀스의
                                 bit integer           상위 필드에
                                                       2비트
                                                       variant가
                                                       곱해진 것

   clock_seq_low                 unsigned 8       9    클럭 시퀀스의
                                 bit integer           하위 필드

   node                          unsigned 48    10-15  공간적으로
                                 bit integer           고유한 노드
                                                       식별자

   해석상, UUID는 다음 다이어그램과 같이 바이트 0-3의 time_low 필드가
   가장 유효한 것으로 표시된다.

       0                   1                   2                   3
        0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |                          time_low                             |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |       time_mid                |         time_hi_and_version   |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |clk_seq_hi_res |  clk_seq_low  |         node (0-1)            |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |                         node (2-5)                            |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

4.1.3.  버전

   버전 번호는 time_hi_and_version 필드의 최상위 4비트에 위치한다.
   다음 표는 본 UUID variant에 대해 현재 정의된 버전을 나열한다.

   Msb0  Msb1  Msb2  Msb3   버전  설명

    0     0     0     1        1   본 문서에 규정된 시간 기반 버전.

    0     0     1     0        2   임베디드 POSIX UID를 가진 DCE
                                   Security 버전.

    0     0     1     1        3   MD5 해싱을 사용하는, 본 문서에
                                   규정된 이름 기반 버전.

    0     1     0     0        4   본 문서에 규정된 랜덤 또는 의사
                                   랜덤 생성 버전.

    0     1     0     1        5   SHA-1 해싱을 사용하는, 본 문서에
                                   규정된 이름 기반 버전.

   버전은 UUID가 어떻게 생성되었는지를 더 정확히 결정하는 데
   사용된다.

   버전의 문자열 표현은 위 표의 버전 번호에 해당하는 16진수에
   다음 표에 따른 variant 비트를 갖는 옥텟의 16진수 값이
   뒤따른다.

4.1.4.  타임스탬프

   타임스탬프는 60비트 값이다. 버전 1 UUID에서 이것은 UUID가 생성된
   시스템의 시간 값에 해당하는, 그레고리력 개혁에 의해 채택된 날짜인
   1582년 10월 15일 00:00:00.00 이후의 100나노초 간격의 수로
   표현된다.

   버전 3 또는 5 UUID에서 타임스탬프는 이름에서 구성된 60비트 값이다.
   버전 4 UUID에서 타임스탬프는 랜덤하게 또는 의사 랜덤하게 생성된
   60비트 값이다. 알고리즘에 대한 설명은 섹션 4.4를 참조한다.

4.1.5.  클럭 시퀀스

   버전 1 UUID에서 클럭 시퀀스는 UUID가 시간 순서로 생성된 것처럼
   보이지만 실제로는 시스템 클럭이 뒤로 설정된 경우를 감지하는 데
   사용된다. UUID 클럭 시퀀스의 값에 대한 규칙은 다음과 같다:

   o  클럭 시퀀스는 원래 (즉, 시스템의 수명 중 한 번)
      랜덤 숫자로 초기화되어야 한다(MUST).

   o  시스템에서 마지막으로 UUID를 생성했을 때의 클럭 값이
      알려져 있고 현재 클럭 값보다 나중이면, 클럭 시퀀스가
      변경되어야 한다(MUST).

   o  시스템에서 마지막으로 UUID를 생성했을 때의 클럭 값이
      알려져 있지 않으면, 클럭 시퀀스를 랜덤 또는 고품질의
      의사 랜덤 값으로 설정해야 한다(SHOULD).

   버전 3 또는 5 UUID에서 클럭 시퀀스는 이름에서 구성된 14비트 값이다.
   버전 4 UUID에서 클럭 시퀀스는 랜덤하게 또는 의사 랜덤하게 생성된
   14비트 값이다.

4.1.6.  노드

   버전 1 UUID에서 노드 필드는 UUID를 생성하는 시스템의 IEEE 802 MAC
   주소로 구성된다. 시스템에 여러 IEEE 802 주소가 있는 경우, 사용
   가능한 어떤 것이든 사용할 수 있다. 시스템에 IEEE 802 주소가 없는
   경우에 대한 가능한 알고리즘은 섹션 4.5를 참조한다.

   버전 3 또는 5 UUID에서 노드 필드는 이름에서 구성된 48비트 값이다.
   버전 4 UUID에서 노드 필드는 랜덤하게 또는 의사 랜덤하게 생성된
   48비트 값이다.

4.1.7.  Nil UUID

   nil UUID는 128비트 모두 0으로 설정되도록 규정된 UUID의 특수한
   형태이다.

4.2.  시간 기반 UUID 생성 알고리즘

   다양한 측면에서 중요한 것은 시간 기반 UUID를 생성하는 데 사용되는
   알고리즘이다. UUID 생성기는 다음을 보장해야 한다(MUST):

   o  UUID를 이전에 저장된 어떤 것의 복제본으로 생성하지 않는다.

   o  UUID를 다른 UUID 생성기가 과거에 생성했거나 미래에 생성할
      어떤 것의 복제본으로 생성하지 않는다.

   o  가능하면 UUID를 초당 1000만 개 이상 생성할 수 있다.

4.2.1.  기본 알고리즘

   위의 조건을 충족하기 위한 가장 단순하고 합리적인 접근법은 하나의
   UUID를 생성하는 각 시간에 다음 단계를 따르는 것이다:

   o  시스템 전체의 전역 잠금을 획득한다.

   o  시스템 전체의 공유 안정 저장소 (예: 파일)에서 UUID 생성기
      상태를 읽는다: 마지막 UUID 생성에 사용된 타임스탬프, 클럭
      시퀀스, 노드 ID의 값.

   o  현재 시간을 1582년 10월 15일 00:00:00.00 이후의 100나노초
      간격의 60비트 수로 가져온다.

   o  현재 노드 ID를 가져온다.

   o  상태를 사용할 수 없는 경우 (예: 존재하지 않거나 손상됨),
      또는 저장된 노드 ID가 현재 노드 ID와 다른 경우, 랜덤
      클럭 시퀀스 값을 생성한다.

   o  상태를 사용할 수 있지만 저장된 타임스탬프가 현재 타임스탬프보다
      나중인 경우, 클럭 시퀀스 값을 증가시킨다.

   o  상태 (현재 타임스탬프, 클럭 시퀀스, 노드 ID)를 안정 저장소에
      다시 저장한다.

   o  전역 잠금을 해제한다.

   o  현재 타임스탬프, 클럭 시퀀스, 노드 ID 값으로부터 섹션 4.2.2의
      단계에 따라 UUID를 형식화한다.

   위의 단순한 알고리즘은 성능에 관심이 있는 어떤 시스템이든
   허용하기 어려울 수 있다. 왜냐하면 안정 저장소에 대한 읽기/쓰기에
   해당하는 I/O가 UUID 하나당 필요하기 때문이다.

   위의 알고리즘은 또한 시스템 클럭의 해상도보다 더 빠르게 UUID를
   발행해야 하는 상황을 처리하지 않는다; 부록 A에 제공된 예시 구현은
   이에 대한 가능한 해결책을 보여준다.

   아래에 제시된 알고리즘의 변형은 위의 측면에서 더 효율적이다:

   o  더 효율적인 I/O를 제공하기 위해 절약 조치를 채택한다.

   o  더 나은 세분성을 위해 프로세서/잠금을 타임스탬프
      틱에 맞춤으로써 시스템 전체의 잠금이 필요하지 않다.

   이러한 변형은 공유 상태를 기본 알고리즘과는 다른 방식으로 사용할
   수 있다. 그러한 변형의 세부사항은 완전한 UUID 생성기의 구현에
   필요하지만, 본 문서의 범위를 벗어난다.

   다중 프로세서 다중 처리 환경에서 프로세스 간에 상태를 공유하는 것이
   어려울 수 있음을 인식해야 한다; 아래 제시된 변형은 이를 해결하는
   알고리즘에 모두 의존한다.

   o  전역 잠금에 대한 대안:

      안정 저장소에 대한 I/O 비용은 각 UUID를 생성하는 대신,
      이전 시간 소인 값을 휘발성 저장소 (예: 시스템 변수)에
      저장함으로써 절약할 수 있다. 이것은 UUID 생성기가
      다시 실행될 때 타임스탬프와 클럭 시퀀스의 이전 값이
      사용 불가능할 수 있게 되어, 클럭 시퀀스가 랜덤 값으로
      설정되어야 한다는 효과가 있다.

      각 UUID 간에 상태를 저장하는 간격을 넓혀 I/O를 줄이는
      보다 세밀한 접근이 가능하다 (부록 A의 구현 예시 참조).

   o  클럭 시퀀스 유지를 원하지 않는 시스템의 경우:

      클럭 시퀀스는 UUID 생성기가 초기화될 때마다 랜덤으로
      설정될 수 있다. 이 경우 클럭 시퀀스 값이 14비트에
      불과하므로, 클럭 시퀀스가 초기화될 때마다 중복 UUID
      생성 위험이 있다. 이 위험을 최소화하는 방법은 세 가지이다:

      a) 클럭 시퀀스의 초기화 간격을 합리적으로 늘린다.

      b) 생성기가 초기화된 후 시스템 클럭의 다음 틱까지
         기다린다.

      c) 좋은 랜덤 소스를 사용하여 클럭 시퀀스를 초기화한다.
         난수 생성에 대한 가이드는 [4]를 참조한다.

4.2.2.  생성 세부사항

   버전 1 UUID는 다음에 따라 생성된다:

   o  섹션 4.2.1에 따라 UTC 기반 타임스탬프와 클럭 시퀀스를 결정한다.

   o  타임스탬프의 경우, 이 목적상 비트 번호 매기기는 가장 유효한
      비트에 번호 0을 매기는 것과 반대로, 최하위 비트부터 0으로
      시작하여 순차적으로 번호를 매긴다.

   o  타임스탬프의 비트 0부터 31까지를 time_low 필드에 설정한다.

   o  타임스탬프의 비트 32부터 47까지를 time_mid 필드에 설정한다.

   o  타임스탬프의 비트 48부터 59까지를 time_hi_and_version의
      12 최하위 비트에 설정한다.

   o  time_hi_and_version 필드의 4 최상위 비트를 섹션 4.1.3의
      표에 있는 4비트 버전 번호로 설정한다.

   o  클럭 시퀀스의 비트 0부터 7까지를 clock_seq_low 필드에 설정한다.

   o  클럭 시퀀스의 비트 8부터 13까지를 clock_seq_hi_and_reserved
      필드의 6 최하위 비트에 설정한다.

   o  clock_seq_hi_and_reserved의 2 최상위 비트를 각각 0과 1로
      설정한다.

   o  노드 필드를 48비트 IEEE 주소로 설정한다. 버전 1 UUID의 경우
      이것은 섹션 4.1.6에 논의된 대로이다.

4.3.  이름 기반 UUID 생성 알고리즘

   버전 3 또는 5 UUID는 특정 "네임스페이스"에서 "이름"으로부터
   생성되도록 의도된다. 이에 대한 개념은 같은 네임스페이스의 같은
   이름에서 항상 같은 UUID가 생성되어야 한다는 것이다. 같은
   네임스페이스의 다른 이름에서 생성된 UUID는 (높은 확률로) 다르며,
   두 다른 네임스페이스의 같은 이름에서 생성된 UUID는 (높은 확률로)
   다르다.

   이름 기반 UUID의 요구사항은 다음과 같다:

   o  같은 이름, 같은 네임스페이스는 같은 UUID를 생성해야 한다.

   o  같은 네임스페이스의 두 다른 이름은 다른 UUID를 생성해야 한다.

   o  두 다른 네임스페이스의 같은 이름은 다른 UUID를 생성해야 한다.

   o  이름에서 생성된 UUID가 주어졌을 때, 이름을 역으로 결정하는
      것이 (일반적으로) 가능해서는 안 된다.

   이름에서 UUID를 생성하는 알고리즘은 다음과 같다:

   o  네임스페이스의 UUID를 할당한다. (예: 부록 C 참조)

   o  네임스페이스 내의 모든 이름에 사용될 해싱 알고리즘을
      선택한다; MD5 [4] 또는 SHA-1 [8].

   o  네임스페이스 ID를 네트워크 바이트 순서로 변환한다.

   o  네임스페이스의 이름에서 이름의 표준 옥텟 시퀀스를 계산한다;
      이름 시스템의 표준에 대한 표준을 사용하여 이름을 정규 형태로
      변환해야 한다 (예: DNS에서는 소문자화, X.500에서는 공백
      압축 등).

   o  네임스페이스 ID와 이름을 연결하여 해시를 계산한다.

   o  해시의 옥텟 0부터 3까지를 네트워크 바이트 순서로 time_low에
      설정한다.

   o  해시의 옥텟 4와 5를 네트워크 바이트 순서로 time_mid에 설정한다.

   o  해시의 옥텟 6과 7을 네트워크 바이트 순서로
      time_hi_and_version에 설정하고, 4 최상위 비트 (비트 12부터
      15)를 적절한 4비트 버전 번호 (섹션 4.1.3 참조)로 덮어쓴다.

   o  해시의 옥텟 8을 clock_seq_hi_and_reserved에 설정하고, 2
      최상위 비트 (비트 6과 7)를 각각 0과 1로 덮어쓴다.

   o  해시의 옥텟 9를 clock_seq_low에 설정한다.

   o  해시의 옥텟 10부터 15까지를 node에 설정한다.

   o  UUID를 로컬 바이트 순서로 변환한다.

4.4.  진정한 난수 또는 의사 난수로부터 UUID를 생성하는 알고리즘

   버전 4 UUID는 진정한 난수 또는 의사 난수로부터 생성되도록 의도된다.

   알고리즘은 다음과 같다:

   o  clock_seq_hi_and_reserved의 2 최상위 비트 (비트 6과 7)를
      각각 0과 1로 설정한다.

   o  time_hi_and_version 필드의 4 최상위 비트 (비트 12부터 15)를
      섹션 4.1.3의 표에 있는 4비트 버전 번호로 설정한다.

   o  나머지 모든 비트를 랜덤하게 (또는 의사 랜덤하게) 선택된
      값으로 설정한다.

   "Randomness Requirements for Security" [5]에 기술된 것처럼 가능한
   최선의 품질을 가진 랜덤 소스를 사용하는 것이 권장된다(RECOMMENDED).

4.5.  호스트를 식별하지 않는 노드 ID

   UUID가 프라이버시 문제나 IEEE 802 주소가 사용 불가능한 이유 등으로
   인해 호스트를 식별하지 않는 노드 ID를 포함하는 것이 바람직할 수
   있다. 이는 시스템 전체의 공유 안정 저장소가 필요한 위의 알고리즘에
   대한 대안이 아니다; 해당 알고리즘은 IEEE 802 주소를 사용하든
   아니든 필요하다.

   47비트 암호화 품질의 랜덤 또는 의사 랜덤 숫자를 얻고, 이 값의
   최하위 옥텟의 최상위 비트 (유니캐스트/멀티캐스트 비트)를 1로
   설정한다. 이 값은 이 시스템에 있는 어떤 카드의 IEEE 802 주소와도
   충돌하지 않는 것으로, 어떤 실제 IEEE 802 주소의 해당 비트도
   항상 0이기 때문이다. (참고: 일부 소프트웨어에서 멀티캐스트
   주소를 이미 사용하고 있기 때문에 RFC 895의 가이드라인을
   엄격히 따르지는 않는다.)

   대안으로, 다양한 시스템 정보 (예: 컴퓨터 이름, 운영 체제,
   프로세서 유형 등)를 수집하여 해시를 계산하고 이를 노드 ID로
   사용한다. 여러 소스에서 정보를 결합하여 가능한 최선의 노드 ID를
   생성하기 위해 MD5 또는 SHA-1을 사용하는 것이 권장된다.

5.  커뮤니티 고려사항

   UUID의 사용은 컴퓨팅에서 매우 광범위하다. 이는 많은 운영 체제
   (Microsoft Windows)와 응용 프로그램 (Mozilla 브라우저)의 핵심
   식별자 인프라를 구성하며, 많은 경우 비표준적인 방식으로 웹에
   노출된다.

   본 명세는 가능한 한 개방적으로, 인터넷 전체에 이익이 되는 방식으로
   그러한 관행을 표준화하려고 시도한다.

6.  보안 고려사항

   UUID가 추측하기 어렵다고 가정하지 말라; UUID를 보안 능력
   (단순한 소유가 접근 권한을 부여하는 식별자)으로 사용해서는
   안 된다. 예측 가능한 난수 소스는 상황을 악화시킬 것이다.

   UUID가 다른 객체에 대한 참조를 리디렉션하기 위해 약간
   변경되었는지 여부를 판단하기 쉽다고 가정하지 말라. 인간은
   UUID의 무결성을 단순히 한눈에 보아서 쉽게 확인할 수 있는 능력이
   없다.

   다양한 호스트에서 UUID를 생성하는 분산 응용 프로그램은 모든
   호스트의 난수 소스에 의존할 준비가 되어 있어야 한다. 이것이
   실현 가능하지 않으면, 네임스페이스 variant를 사용해야 한다.

7.  감사의 글

   본 문서는 UUID에 대한 OSF DCE 명세에 크게 의존한다. Ted Ts'o는
   특히 바이트 순서 섹션에 대해 유용한 의견을 제공했으며, 그가
   제공한 제안된 표현을 대부분 차용했다 (그러나 해당 섹션의 모든
   오류는 우리의 책임이다).

   우리는 또한 Ralf S. Engelschall, John Larmouth, Paul Thorpe의
   세심한 검토와 비트 조작에 감사한다. Larmouth 교수는 ISO/IEC와의
   조율을 달성하는 데에도 매우 소중했다.

8.  참고 문헌

8.1.  규범적 참고 문헌

   [1]  Zahn, L., Dineen, T., and P. Leach, "Network Computing
        Architecture", ISBN 0-13-611674-4, January 1990.

   [2]  "DCE: Remote Procedure Call", Open Group CAE Specification C309
        ISBN 1-85912-041-5, August 1994.

   [3]  ISO/IEC 9834-8:2004 Information Technology, "Procedures for the
        operation of OSI Registration Authorities: Generation and
        registration of Universally Unique Identifiers (UUIDs) and their
        use as ASN.1 Object Identifier components" ITU-T Rec. X.667,
        2004.

   [4]  Rivest, R., "The MD5 Message-Digest Algorithm ", RFC 1321,
        April 1992.

   [5]  Eastlake, D., 3rd, Schiller, J., and S. Crocker, "Randomness
        Requirements for Security", BCP 106, RFC 4086, June 2005.

8.2.  정보성 참고 문헌

   [6]  Moats, R., "URN Syntax", RFC 2141, May 1997.

   [7]  Crocker, D. and P. Overell, "Augmented BNF for Syntax
        Specifications: ABNF", RFC 2234, November 1997.

   [8]  National Institute of Standards and Technology, "Secure Hash
        Standard", FIPS PUB 180-1, April 1995,
        <http://www.itl.nist.gov/fipspubs/fip180-1.htm>.

부록 A - 구현 예시

   이 구현은 다음 파일로 구성된다:

      copyrt.h - 저작권 고지

      uuid.h - UUID 구조체와 함수에 대한 헤더 파일

      uuid.c - UUID 생성 함수의 구현

      sysdep.h - 시스템 종속 타입과 상수에 대한 헤더 파일

      sysdep.c - 시스템 종속 함수의 구현

      utest.c - UUID 생성과 비교 연산을 시연하는 테스트 드라이버

   copyrt.h

```c
/*
 Copyright (c) 1990- 1993, 1996 Open Software Foundation, Inc.
 Copyright (c) 1989 by Hewlett-Packard Company, Palo Alto, Ca. &
 Digital Equipment Corporation, Maynard, Mass.
 Copyright (c) 1998 Microsoft.
 To anyone who acknowledges that this file is provided "AS IS"
 without any express or implied warranty: permission to use, copy,
 modify, and distribute this file for any purpose is hereby
 granted without fee, provided that the above copyright notices and
 this notice appears in all source code copies, and that none of
 the names of Open Software Foundation, Inc., Hewlett-Packard
 Company, Microsoft, or Digital Equipment Corporation be used in
 advertising or publicity pertaining to distribution of the software
 without specific, written prior permission. Neither Open Software
 Foundation, Inc., Hewlett-Packard Company, Microsoft, nor Digital
 Equipment Corporation makes any representations about the
 suitability of this software for any purpose.
*/
```

   uuid.h

```c
#include "copyrt.h"
#undef uuid_t
typedef struct {
    unsigned32  time_low;
    unsigned16  time_mid;
    unsigned16  time_hi_and_version;
    unsigned8   clock_seq_hi_and_reserved;
    unsigned8   clock_seq_low;
    byte        node[6];
} uuid_t;

/* uuid_create -- generate a UUID */
int uuid_create(uuid_t * uuid);

/* uuid_create_md5_from_name -- create a version 3 (MD5) UUID using a
   "name" from a "name space" */
void uuid_create_md5_from_name(
    uuid_t *uuid,         /* resulting UUID */
    uuid_t nsid,          /* UUID of the namespace */
    void *name,           /* the name from which to generate a UUID */
    int namelen           /* the length of the name */
);

/* uuid_create_sha1_from_name -- create a version 5 (SHA-1) UUID
   using a "name" from a "name space" */
void uuid_create_sha1_from_name(
    uuid_t *uuid,         /* resulting UUID */
    uuid_t nsid,          /* UUID of the namespace */
    void *name,           /* the name from which to generate a UUID */
    int namelen           /* the length of the name */
);

/* uuid_compare --  Compare two UUID's "lexically" and return
        -1   u1 is lexically before u2
         0   u1 is equal to u2
         1   u1 is lexically after u2
   Note that lexical ordering is not temporal ordering!
*/
int uuid_compare(uuid_t *u1, uuid_t *u2);
```

   uuid.c

```c
#include "copyrt.h"
#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include "sysdep.h"
#include "uuid.h"

/* various forward declarations */
static int read_state(unsigned16 *clockseq, uuid_time_t *timestamp,
    uuid_node_t *node);
static void write_state(unsigned16 clockseq, uuid_time_t timestamp,
    uuid_node_t node);
static void format_uuid_v1(uuid_t *uuid, unsigned16 clockseq,
    uuid_time_t timestamp, uuid_node_t node);
static void format_uuid_v3or5(uuid_t *uuid, unsigned char hash[16],
    int v);
static void get_current_time(uuid_time_t *timestamp);
static unsigned16 true_random(void);

/* uuid_create -- generator a UUID */
int uuid_create(uuid_t *uuid)
{
     uuid_time_t timestamp, last_time;
     unsigned16 clockseq;
     uuid_node_t node;
     uuid_node_t last_node;
     int f;

     /* acquire system-wide lock so we're alone */
     LOCK;
     /* get time, node ID, saved state from non-volatile storage */
     get_current_time(&timestamp);
     get_ieee_node_identifier(&node);
     f = read_state(&clockseq, &last_time, &last_node);

     /* if no NV state, or if clock went backwards, or node ID
        changed (e.g., new network card) change clockseq */
     if (!f || memcmp(&node, &last_node, sizeof node))
         clockseq = true_random();
     else if (timestamp < last_time)
         clockseq++;

     /* save the state for next time */
     write_state(clockseq, timestamp, node);

     UNLOCK;

     /* stuff fields into the UUID */
     format_uuid_v1(uuid, clockseq, timestamp, node);
     return 1;
}

/* format_uuid_v1 -- make a UUID from the timestamp, clockseq,
                     and node ID */
void format_uuid_v1(uuid_t* uuid, unsigned16 clock_seq,
                    uuid_time_t timestamp, uuid_node_t node)
{
    /* Construct a version 1 uuid with the information we've gathered
       plus a few constants. */
    uuid->time_low = (unsigned long)(timestamp & 0xFFFFFFFF);
    uuid->time_mid = (unsigned short)((timestamp >> 32) & 0xFFFF);
    uuid->time_hi_and_version =
        (unsigned short)((timestamp >> 48) & 0x0FFF);
    uuid->time_hi_and_version |= (1 << 12);
    uuid->clock_seq_low = clock_seq & 0xFF;
    uuid->clock_seq_hi_and_reserved = (clock_seq & 0x3F00) >> 8;
    uuid->clock_seq_hi_and_reserved |= 0x80;
    memcpy(&uuid->node, &node, sizeof uuid->node);
}

/* data type for UUID generator persistent state */
typedef struct {
    uuid_time_t  ts;       /* saved timestamp */
    uuid_node_t  node;     /* saved node ID */
    unsigned16   cs;       /* saved clock sequence */
} uuid_state;

static uuid_state st;

/* read_state -- read UUID generator state from non-volatile store */
int read_state(unsigned16 *clockseq, uuid_time_t *timestamp,
               uuid_node_t *node)
{
    static int inited = 0;
    FILE *fp;

    /* only need to read state once per boot */
    if (!inited) {
        fp = fopen("state", "rb");
        if (fp == NULL)
            return 0;
        fread(&st, sizeof st, 1, fp);
        fclose(fp);
        inited = 1;
    }
    *clockseq = st.cs;
    *timestamp = st.ts;
    *node = st.node;
    return 1;
}

/* write_state -- save UUID generator state back to non-volatile
   storage */
void write_state(unsigned16 clockseq, uuid_time_t timestamp,
                 uuid_node_t node)
{
    static int inited = 0;
    static uuid_time_t next_save;
    FILE* fp;

    if (!inited) {
        next_save = timestamp;
        inited = 1;
    }

    /* always save state to volatile shared state */
    st.cs = clockseq;
    st.ts = timestamp;
    st.node = node;
    if (timestamp >= next_save) {
        fp = fopen("state", "wb");
        fwrite(&st, sizeof st, 1, fp);
        fclose(fp);
        /* schedule next save for 10 seconds from now */
        next_save = timestamp + (10 * 10 * 1000 * 1000);
    }
}

/* get-current_time -- get time as 60-bit 100ns ticks since UUID epoch.
   Compensate for the fact that real clock resolution is
   less than 100ns. */
void get_current_time(uuid_time_t *timestamp)
{
    static int inited = 0;
    static uuid_time_t time_last;
    static unsigned16 uuids_this_tick;
    uuid_time_t time_now;

    if (!inited) {
        get_system_time(&time_now);
        uuids_this_tick = UUIDS_PER_TICK;
        inited = 1;
    }

    for ( ; ; ) {
        get_system_time(&time_now);

        /* if clock reading changed since last UUID generated, */
        if (time_last != time_now) {
            /* reset count of uuids gen'd with this clock reading */
            uuids_this_tick = 0;
            time_last = time_now;
            break;
        }
        if (uuids_this_tick < UUIDS_PER_TICK) {
            uuids_this_tick++;
            break;
        }

        /* going too fast for our clock; spin */
    }
    /* add the count of uuids to low order bits of the clock reading */
    *timestamp = time_now + uuids_this_tick;
}

/* true_random -- generate a crypto-quality random number.
   This sample doesn't do that. */
static unsigned16 true_random(void)
{
    static int inited = 0;
    uuid_time_t time_now;

    if (!inited) {
        get_system_time(&time_now);
        time_now = time_now / UUIDS_PER_TICK;
        srand((unsigned int)
               (((time_now >> 32) ^ time_now) & 0xffffffff));
        inited = 1;
    }

    return rand();
}

/* uuid_create_md5_from_name -- create a version 3 (MD5) UUID using a
   "name" from a "name space" */
void uuid_create_md5_from_name(uuid_t *uuid, uuid_t nsid, void *name,
                               int namelen)
{
    MD5_CTX c;
    unsigned char hash[16];
    uuid_t net_nsid;

    /* put name space ID in network byte order so it hashes the same
       no matter what endian machine we're on */
    net_nsid = nsid;
    net_nsid.time_low = htonl(net_nsid.time_low);
    net_nsid.time_mid = htons(net_nsid.time_mid);
    net_nsid.time_hi_and_version = htons(net_nsid.time_hi_and_version);

    MD5Init(&c);
    MD5Update(&c, &net_nsid, sizeof net_nsid);
    MD5Update(&c, name, namelen);
    MD5Final(hash, &c);

    /* the hash is in network byte order at this point */
    format_uuid_v3or5(uuid, hash, 3);
}

void uuid_create_sha1_from_name(uuid_t *uuid, uuid_t nsid, void *name,
                                int namelen)
{
    SHA_CTX c;
    unsigned char hash[20];
    uuid_t net_nsid;

    net_nsid = nsid;
    net_nsid.time_low = htonl(net_nsid.time_low);
    net_nsid.time_mid = htons(net_nsid.time_mid);
    net_nsid.time_hi_and_version = htons(net_nsid.time_hi_and_version);

    SHA1_Init(&c);
    SHA1_Update(&c, &net_nsid, sizeof net_nsid);
    SHA1_Update(&c, name, namelen);
    SHA1_Final(hash, &c);

    /* the hash is in network byte order at this point */
    format_uuid_v3or5(uuid, hash, 5);
}

/* format_uuid_v3or5 -- make a UUID from a (pseudo)random 128-bit
   number */
void format_uuid_v3or5(uuid_t *uuid, unsigned char hash[16], int v)
{
    /* convert UUID to local byte order */
    memcpy(uuid, hash, sizeof *uuid);
    uuid->time_low = ntohl(uuid->time_low);
    uuid->time_mid = ntohs(uuid->time_mid);
    uuid->time_hi_and_version = ntohs(uuid->time_hi_and_version);

    /* put in the variant and version bits */
    uuid->time_hi_and_version &= 0x0FFF;
    uuid->time_hi_and_version |= (v << 12);
    uuid->clock_seq_hi_and_reserved &= 0x3F;
    uuid->clock_seq_hi_and_reserved |= 0x80;
}

/* uuid_compare --  Compare two UUID's "lexically" and return */
#define CHECK(f1, f2) if (f1 != f2) return f1 < f2 ? -1 : 1;
int uuid_compare(uuid_t *u1, uuid_t *u2)
{
    int i;

    CHECK(u1->time_low, u2->time_low);
    CHECK(u1->time_mid, u2->time_mid);
    CHECK(u1->time_hi_and_version, u2->time_hi_and_version);
    CHECK(u1->clock_seq_hi_and_reserved, u2->clock_seq_hi_and_reserved);
    CHECK(u1->clock_seq_low, u2->clock_seq_low)
    for (i = 0; i < 6; i++) {
        if (u1->node[i] < u2->node[i])
            return -1;
        if (u1->node[i] > u2->node[i])
            return 1;
    }
    return 0;
}
#undef CHECK
```

   sysdep.h

```c
#include "copyrt.h"
/* remove the following define if you aren't running WIN32 */
#define WININC 0

#ifdef WININC
#include <windows.h>
#else
#include <sys/types.h>
#include <sys/time.h>
#include <sys/sysinfo.h>
#endif

#include "global.h"
/* change to point to where MD5 .h's live; RFC 1321 has sample
   implementation */
#include "md5.h"

/* set the following to the number of 100ns ticks of the actual
   resolution of your system's clock */
#define UUIDS_PER_TICK 1024

/* Set the following to a calls to get and release a global lock */
#define LOCK
#define UNLOCK

typedef unsigned long   unsigned32;
typedef unsigned short  unsigned16;
typedef unsigned char   unsigned8;
typedef unsigned char   byte;

/* Set this to what your compiler uses for 64-bit data type */
#ifdef WININC
#define unsigned64_t unsigned __int64
#define I64(C) C
#else
#define unsigned64_t unsigned long long
#define I64(C) C##LL
#endif

typedef unsigned64_t uuid_time_t;
typedef struct {
    char nodeID[6];
} uuid_node_t;

void get_ieee_node_identifier(uuid_node_t *node);
void get_system_time(uuid_time_t *uuid_time);
void get_random_info(char seed[16]);
```

   sysdep.c

```c
#include "copyrt.h"
#include <stdio.h>
#include "sysdep.h"

/* system dependent call to get IEEE node ID.
   This sample implementation generates a random node ID. */
void get_ieee_node_identifier(uuid_node_t *node)
{
    static inited = 0;
    static uuid_node_t saved_node;
    char seed[16];
    FILE *fp;

    if (!inited) {
        fp = fopen("nodeid", "rb");
        if (fp) {
            fread(&saved_node, sizeof saved_node, 1, fp);
            fclose(fp);
        }
        else {
            get_random_info(seed);
            seed[0] |= 0x01;
            memcpy(&saved_node, seed, sizeof saved_node);
            fp = fopen("nodeid", "wb");
            if (fp) {
                fwrite(&saved_node, sizeof saved_node, 1, fp);
                fclose(fp);
            }
        }

        inited = 1;
    }

    *node = saved_node;
}

/* system dependent call to get the current system time. Returned as
   100ns ticks since UUID epoch, but resolution may be less than
   100ns. */
#ifdef _WINDOWS_

void get_system_time(uuid_time_t *uuid_time)
{
    ULARGE_INTEGER time;

    /* NT keeps time in FILETIME format which is 100ns ticks since
       Jan 1, 1601. UUIDs use time in 100ns ticks since Oct 15, 1582.
       The difference is 17 Days in Oct + 30 (Nov) + 31 (Dec)
       + 18 years and 5 leap days. */
    GetSystemTimeAsFileTime((FILETIME *)&time);
    time.QuadPart +=
          (unsigned __int64) (1000*1000*10)       // seconds
        * (unsigned __int64) (60 * 60 * 24)       // days
        * (unsigned __int64) (17+30+31+365*18+5); // # of days
    *uuid_time = time.QuadPart;
}

/* Sample code, not for use in production; see RFC 1750 */
void get_random_info(char seed[16])
{
    MD5_CTX c;
    struct {
        MEMORYSTATUS m;
        SYSTEM_INFO s;
        FILETIME t;
        LARGE_INTEGER pc;
        DWORD tc;
        DWORD l;
        char hostname[MAX_COMPUTERNAME_LENGTH + 1];
    } r;

    MD5Init(&c);
    GlobalMemoryStatus(&r.m);
    GetSystemInfo(&r.s);
    GetSystemTimeAsFileTime(&r.t);
    QueryPerformanceCounter(&r.pc);
    r.tc = GetTickCount();
    r.l = MAX_COMPUTERNAME_LENGTH + 1;
    GetComputerName(r.hostname, &r.l);
    MD5Update(&c, &r, sizeof r);
    MD5Final(seed, &c);
}

#else

void get_system_time(uuid_time_t *uuid_time)
{
    struct timeval tp;

    gettimeofday(&tp, (struct timezone *)0);

    /* Offset between UUID formatted times and Unix formatted times.
       UUID UTC base time is October 15, 1582.
       Unix base time is January 1, 1970.*/
    *uuid_time = ((unsigned64)tp.tv_sec * 10000000)
        + ((unsigned64)tp.tv_usec * 10)
        + I64(0x01B21DD213814000);
}

/* Sample code, not for use in production; see RFC 1750 */
void get_random_info(char seed[16])
{
    MD5_CTX c;
    struct {
        struct sysinfo s;
        struct timeval t;
        char hostname[257];
    } r;

    MD5Init(&c);
    sysinfo(&r.s);
    gettimeofday(&r.t, (struct timezone *)0);
    gethostname(r.hostname, 256);
    MD5Update(&c, &r, sizeof r);
    MD5Final(seed, &c);
}

#endif
```

   utest.c

```c
#include "copyrt.h"
#include "sysdep.h"
#include <stdio.h>
#include "uuid.h"

uuid_t NameSpace_DNS = { /* 6ba7b810-9dad-11d1-80b4-00c04fd430c8 */
    0x6ba7b810,
    0x9dad,
    0x11d1,
    0x80, 0xb4, {0x00, 0xc0, 0x4f, 0xd4, 0x30, 0xc8}
};

/* puid -- print a UUID */
void puid(uuid_t u)
{
    int i;

    printf("%8.8x-%4.4x-%4.4x-%2.2x%2.2x-", u.time_low, u.time_mid,
    u.time_hi_and_version, u.clock_seq_hi_and_reserved,
    u.clock_seq_low);
    for (i = 0; i < 6; i++)
        printf("%2.2x", u.node[i]);
    printf("\n");
}

/* Simple driver for UUID generator */
void main(int argc, char argv)
{
    uuid_t u;
    int f;

    uuid_create(&u);
    printf("uuid_create(): "); puid(u);

    f = uuid_compare(&u, &u);
    printf("uuid_compare(u,u): %d\n", f);     /* should be 0 */
    f = uuid_compare(&u, &NameSpace_DNS);
    printf("uuid_compare(u, NameSpace_DNS): %d\n", f); /* s.b. 1 */
    f = uuid_compare(&NameSpace_DNS, &u);
    printf("uuid_compare(NameSpace_DNS, u): %d\n", f); /* s.b. -1 */
    uuid_create_md5_from_name(&u, NameSpace_DNS, "www.widgets.com", 15);
    printf("uuid_create_md5_from_name(): "); puid(u);
}
```

부록 B - 출력 예시

   이것은 위의 코드가 실행되었을 때의 출력이다. 물론 시간 기반 UUID와
   비교 결과 중 하나는 실행마다 다를 것이다.

```
uuid_create(): 7d444840-9dc0-11d1-b245-5ffdce74fad2
uuid_compare(u,u): 0
uuid_compare(u, NameSpace_DNS): 1
uuid_compare(NameSpace_DNS, u): -1
uuid_create_md5_from_name(): e902893a-9d22-3c7e-a7b8-d6e313b71d9f
```

부록 C - 미리 정의된 몇 가지 네임스페이스 ID

   본 부록은 미리 정의된 네임스페이스로 사용 가능한 UUID 값을 나열한다:

   /* 이름 문자열이 정규화된 도메인 이름인 경우 */
   uuid_t NameSpace_DNS = { /* 6ba7b810-9dad-11d1-80b4-00c04fd430c8 */
       0x6ba7b810,
       0x9dad,
       0x11d1,
       0x80, 0xb4, {0x00, 0xc0, 0x4f, 0xd4, 0x30, 0xc8}
   };

   /* 이름 문자열이 URL인 경우 */
   uuid_t NameSpace_URL = { /* 6ba7b811-9dad-11d1-80b4-00c04fd430c8 */
       0x6ba7b811,
       0x9dad,
       0x11d1,
       0x80, 0xb4, {0x00, 0xc0, 0x4f, 0xd4, 0x30, 0xc8}
   };

   /* 이름 문자열이 ISO OID인 경우 */
   uuid_t NameSpace_OID = { /* 6ba7b812-9dad-11d1-80b4-00c04fd430c8 */
       0x6ba7b812,
       0x9dad,
       0x11d1,
       0x80, 0xb4, {0x00, 0xc0, 0x4f, 0xd4, 0x30, 0xc8}
   };

   /* 이름 문자열이 X.500 DN (DER 또는 텍스트 출력 형식)인 경우 */
   uuid_t NameSpace_X500 = { /* 6ba7b814-9dad-11d1-80b4-00c04fd430c8 */
       0x6ba7b814,
       0x9dad,
       0x11d1,
       0x80, 0xb4, {0x00, 0xc0, 0x4f, 0xd4, 0x30, 0xc8}
   };

저자 주소

   Paul J. Leach
   Microsoft
   1 Microsoft Way
   Redmond, WA 98052
   US

   Phone: +1 425-882-8080
   EMail: paulle@microsoft.com

   Michael Mealling
   Refactored Networks, LLC
   1635 Old Hwy 41
   Suite 112, Box 138
   Kennesaw, GA 30152
   US

   Phone: +1-678-581-9656
   EMail: michael@refactored-networks.com
   URI:   http://www.refactored-networks.com

   Rich Salz
   DataPower Technology, Inc.
   1 Alewife Center
   Cambridge, MA 02142
   US

   Phone: +1 617-864-0455
   EMail: rsalz@datapower.com
   URI:   http://www.datapower.com

전체 저작권 성명

   Copyright (C) The Internet Society (2005).

   본 문서는 BCP 78에 포함된 권리, 라이선스 및 제한 사항의 적용을
   받으며, 거기에 명시된 것을 제외하고 저자는 모든 권리를 보유한다.

   본 문서와 여기에 포함된 정보는 "있는 그대로(AS IS)" 기반으로
   제공되며, 기여자, 그/그녀가 대표하거나 후원하는 조직(있는 경우),
   인터넷 소사이어티 및 인터넷 엔지니어링 태스크 포스는 여기 정보의
   사용이 어떠한 권리도 침해하지 않을 것이라는 보증 또는 상품성이나
   특정 목적에의 적합성에 대한 묵시적 보증을 포함하되 이에 국한되지
   않는 모든 명시적 또는 묵시적 보증을 부인한다.

지적 재산권

   IETF는 본 문서에 기술된 기술의 구현 또는 사용과 관련하여 주장될 수
   있는 지적 재산권이나 기타 권리의 유효성 또는 범위, 또는 그러한
   권리에 따른 라이선스가 이용 가능하거나 이용 가능하지 않을 수 있는
   범위에 대해 어떠한 입장도 취하지 않으며, 그러한 권리를 식별하기
   위한 독립적인 노력을 했다고 표명하지 않는다.

   RFC 문서의 권리에 관한 절차에 대한 정보는 BCP 78과 BCP 79에서
   찾을 수 있다.

   IETF 사무국에 제출된 IPR 공개 사본과 이용 가능하게 될 라이선스에
   대한 보증, 또는 본 명세의 구현자나 사용자에 의한 그러한 독점적
   권리의 사용을 위한 일반 라이선스 또는 허가를 얻으려는 시도의
   결과는 http://www.ietf.org/ipr 의 IETF 온라인 IPR 저장소에서
   얻을 수 있다.

   IETF는 본 표준을 구현하는 데 필요할 수 있는 기술을 포함할 수 있는
   저작권, 특허 또는 특허 출원, 또는 기타 독점적 권리에 대해 관심 있는
   당사자가 IETF에 알려주기를 요청한다. 정보는 ietf-ipr@ietf.org 로
   IETF에 보내 주기 바란다.

감사의 글

   RFC 편집기 기능에 대한 자금은 현재 인터넷 소사이어티에서 제공하고 있다.
