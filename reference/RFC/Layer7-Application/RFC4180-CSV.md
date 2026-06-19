Network Working Group                                    Y. Shafranovich
Request for Comments: 4180                SolidMatrix Technologies, Inc.
Category: Informational                                     October 2005


            쉼표로 구분된 값(CSV) 파일의 공통 형식 및 MIME 타입

이 메모의 상태

   이 메모는 인터넷 커뮤니티를 위한 정보를 제공한다. 이 메모는 어떠한
   종류의 인터넷 표준도 명시하지 않는다. 이 메모의 배포에는 제한이 없다.

저작권 고지

   Copyright (C) The Internet Society (2005).

초록

   이 RFC는 쉼표로 구분된 값(Comma-Separated Values, CSV) 파일에 사용되는
   형식을 문서화하고, 이와 관련된 MIME 타입 "text/csv"를 등록한다.

목차

   1. 소개 ...........................................................2
   2. CSV 형식의 정의 ................................................2
   3. text/csv의 MIME 타입 등록 .....................................4
   4. IANA 고려 사항 .................................................5
   5. 보안 고려 사항 .................................................5
   6. 감사의 말 ......................................................6
   7. 참고 문헌 ......................................................6
      7.1. 규범적 참고 문헌 ..........................................6
      7.2. 정보성 참고 문헌 ..........................................6

















Shafranovich                 Informational                      [Page 1]

RFC 4180       Common Format and MIME Type for CSV Files    October 2005


1.  소개

   쉼표로 구분된 값 형식(CSV)은 다양한 스프레드시트 프로그램들 사이에서
   데이터를 교환하고 변환하기 위해 꽤 오랜 기간 동안 사용되어 왔다. 놀랍게도,
   이 형식이 매우 흔하게 사용됨에도 불구하고 단 한 번도 공식적으로 문서화된
   적이 없었다. 또한, IANA MIME 등록 트리에는
   "text/tab-separated-values" 타입에 대한 등록이 포함되어 있지만, CSV에
   대해서는 어떠한 MIME 타입도 IANA에 등록된 적이 없다. 동시에, 다양한
   프로그램과 운영 체제들은 이 형식에 대해 서로 다른 MIME 타입을 사용하기
   시작했다. 이 RFC는 쉼표로 구분된 값(CSV) 파일의 형식을 문서화하고, RFC
   2048 [1]에 따라 CSV를 위한 "text/csv" MIME 타입을 공식적으로 등록한다.

2.  CSV 형식의 정의

   CSV 형식에 대한 다양한 사양과 구현(예를 들어 [4], [5], [6], [7])이
   존재하지만, 공식적인 사양은 존재하지 않으며, 이로 인해 CSV 파일에 대한
   매우 다양한 해석이 가능해진다. 이 절에서는 대부분의 구현이 따르는 것으로
   보이는 형식을 문서화한다:

   1.  각 레코드는 별도의 줄에 위치하며, 줄 바꿈(CRLF)으로 구분된다.
       예를 들어:

       aaa,bbb,ccc CRLF
       zzz,yyy,xxx CRLF

   2.  파일의 마지막 레코드는 끝맺는 줄 바꿈이 있을 수도 있고 없을 수도
       있다. 예를 들어:

       aaa,bbb,ccc CRLF
       zzz,yyy,xxx

   3.  파일의 첫 번째 줄로 선택적인 헤더 줄이 나타날 수 있으며, 이는 일반
       레코드 줄과 동일한 형식을 가진다. 이 헤더는 파일의 필드에 대응하는
       이름들을 포함하며, 파일의 나머지 레코드들과 동일한 개수의 필드를
       포함해야 한다(헤더 줄의 존재 여부는 이 MIME 타입의 선택적 "header"
       매개변수를 통해 표시되어야 한다). 예를 들어:

       field_name,field_name,field_name CRLF
       aaa,bbb,ccc CRLF
       zzz,yyy,xxx CRLF






Shafranovich                 Informational                      [Page 2]

RFC 4180       Common Format and MIME Type for CSV Files    October 2005


   4.  헤더와 각 레코드 안에는 쉼표로 구분된 하나 이상의 필드가 있을 수
       있다. 각 줄은 파일 전체에 걸쳐 동일한 개수의 필드를 포함해야 한다.
       공백은 필드의 일부로 간주되며 무시되어서는 안 된다. 레코드의 마지막
       필드 뒤에는 쉼표가 와서는 안 된다. 예를 들어:

       aaa,bbb,ccc

   5.  각 필드는 큰따옴표로 둘러싸일 수도 있고 그렇지 않을 수도 있다(다만
       Microsoft Excel과 같은 일부 프로그램은 큰따옴표를 전혀 사용하지
       않는다). 필드가 큰따옴표로 둘러싸이지 않은 경우에는, 필드 안에
       큰따옴표가 나타나서는 안 된다. 예를 들어:

       "aaa","bbb","ccc" CRLF
       zzz,yyy,xxx

   6.  줄 바꿈(CRLF), 큰따옴표, 쉼표를 포함하는 필드는 큰따옴표로
       둘러싸여야 한다. 예를 들어:

       "aaa","b CRLF
       bb","ccc" CRLF
       zzz,yyy,xxx

   7.  필드를 둘러싸기 위해 큰따옴표를 사용하는 경우, 필드 안에 나타나는
       큰따옴표는 그 앞에 또 다른 큰따옴표를 붙여 이스케이프해야 한다.
       예를 들어:

       "aaa","b""bb","ccc"

   ABNF 문법 [2]은 다음과 같다:

   file = [header CRLF] record *(CRLF record) [CRLF]

   header = name *(COMMA name)

   record = field *(COMMA field)

   name = field

   field = (escaped / non-escaped)

   escaped = DQUOTE *(TEXTDATA / COMMA / CR / LF / 2DQUOTE) DQUOTE

   non-escaped = *TEXTDATA

   COMMA = %x2C

   CR = %x0D ;as per section 6.1 of RFC 2234 [2]



Shafranovich                 Informational                      [Page 3]

RFC 4180       Common Format and MIME Type for CSV Files    October 2005


   DQUOTE =  %x22 ;as per section 6.1 of RFC 2234 [2]

   LF = %x0A ;as per section 6.1 of RFC 2234 [2]

   CRLF = CR LF ;as per section 6.1 of RFC 2234 [2]

   TEXTDATA =  %x20-21 / %x23-2B / %x2D-7E

3.  text/csv의 MIME 타입 등록

   이 절에서는 (RFC 2048 [1]에 따른) 미디어 타입 등록 신청서를 제공한다.

   To: ietf-types@iana.org

   Subject: Registration of MIME media type text/csv

   MIME 미디어 타입 이름: text

   MIME 서브타입 이름: csv

   필수 매개변수: 없음

   선택적 매개변수: charset, header

      CSV의 일반적인 용도는 US-ASCII이지만, "text" 트리에 대해 IANA가
      정의한 다른 문자 집합도 "charset" 매개변수와 함께 사용될 수 있다.

      "header" 매개변수는 헤더 줄의 존재 여부를 나타낸다. 유효한 값은
      "present" 또는 "absent"이다. 이 매개변수를 사용하지 않기로 선택한
      구현자는 헤더 줄의 존재 여부에 대해 스스로 결정을 내려야 한다.

   인코딩 고려 사항:

      RFC 2046 [3]의 4.1.1절에 따라, 이 미디어 타입은 줄 바꿈을 나타내기
      위해 CRLF를 사용한다. 다만 구현자는 일부 구현이 다른 값을 사용할 수
      있다는 점을 인지해야 한다.

   보안 고려 사항:

      CSV 파일은 어떠한 위험도 초래하지 않아야 하는 수동적 텍스트 데이터를
      포함한다. 그러나 이론적으로는, CSV 데이터를 처리하는 프로그램의 잠재적
      버퍼 오버런을 악용하기 위해 악의적인 바이너리 데이터가 포함될 수도
      있다. 또한, 이 형식을 통해 비공개 데이터가 공유될 수 있다(이는 물론
      모든 텍스트 데이터에 적용된다).



Shafranovich                 Informational                      [Page 4]

RFC 4180       Common Format and MIME Type for CSV Files    October 2005


   상호운용성 고려 사항:

      단일 사양의 부재로 인해, 구현들 사이에는 상당한 차이가 존재한다.
      구현자는 CSV 파일을 처리할 때 "자신이 하는 일에는 보수적으로,
      다른 곳으로부터 받아들이는 것에는 관대하게"(RFC 793 [8])의 원칙을
      따라야 한다. 공통 정의에 대한 시도는 2절에서 찾을 수 있다.

      선택적 "header" 매개변수를 사용하지 않기로 결정한 구현은 헤더의
      존재 여부에 대해 스스로 결정을 내려야 한다.

   발행된 사양:

      다양한 프로그램과 시스템에 대한 수많은 비공개 사양이 존재하지만, 이
      형식에 대한 단일한 "마스터" 사양은 존재하지 않는다. 공통 정의에 대한
      시도는 2절에서 찾을 수 있다.

   이 미디어 타입을 사용하는 애플리케이션:

      스프레드시트 프로그램 및 다양한 데이터 변환 유틸리티

   추가 정보:

      매직 넘버: 없음

      파일 확장자: CSV

      매킨토시 파일 타입 코드: TEXT

   추가 정보를 위해 연락할 사람 및 이메일 주소:

      Yakov Shafranovich <ietf@shaftek.org>

   의도된 용도: COMMON

   저자/변경 관리자: IESG

4.  IANA 고려 사항

   IANA는 이 문서의 3절에 제공된 신청서를 사용하여 MIME 타입 "text/csv"를
   등록하였다.

5.  보안 고려 사항

   위 3절의 논의를 참조하라.




Shafranovich                 Informational                      [Page 5]

RFC 4180       Common Format and MIME Type for CSV Files    October 2005


6.  감사의 말

   저자는 유익한 제안을 해 준 Dave Crocker, Martin Duerst, Joel M.
   Halpern, Clyde Ingram, Graham Klyne, Bruce Lilly, Chris Lilley, 그리고
   IESG의 구성원들에게 감사를 전하고자 한다. ABNF 문법을 도와준 Dave에게
   특별한 감사의 말을 전한다.

   저자는 또한 RFC와 인터넷 드래프트를 준비하는 데 사용된 많은 도구를
   제공해 준 Henrik Lefkowetz, Marshall Rose, 그리고 xml.resource.org의
   사람들에게도 감사를 전하고자 한다.

   L.T.S.에게 특별한 감사를 전한다.

7.  참고 문헌

7.1.  규범적 참고 문헌

   [1]  Freed, N., Klensin, J., and J. Postel, "Multipurpose Internet
        Mail Extensions (MIME) Part Four: Registration Procedures", BCP
        13, RFC 2048, November 1996.

   [2]  Crocker, D. and P. Overell, "Augmented BNF for Syntax
        Specifications: ABNF", RFC 2234, November 1997.

   [3]  Freed, N. and N. Borenstein, "Multipurpose Internet Mail
        Extensions (MIME) Part Two: Media Types", RFC 2046, November
        1996.

7.2.  정보성 참고 문헌

   [4]  Repici, J., "HOW-TO: The Comma Separated Value (CSV) File
        Format", 2004,
        <http://www.creativyst.com/Doc/Articles/CSV/CSV01.htm>.

   [5]  Edoceo, Inc., "CSV Standard File Format", 2004,
        <http://www.edoceo.com/utilis/csv-file-format.php>.

   [6]  Rodger, R. and O. Shanaghy, "Documentation for Ricebridge CSV
        Manager", February 2005,
        <http://www.ricebridge.com/products/csvman/reference.htm>.

   [7]  Raymond, E., "The Art of Unix Programming, Chapter 5", September
        2003,
        <http://www.catb.org/~esr/writings/taoup/html/ch05s02.html>.

   [8]  Postel, J., "Transmission Control Protocol", STD 7, RFC 793,
        September 1981.




Shafranovich                 Informational                      [Page 6]

RFC 4180       Common Format and MIME Type for CSV Files    October 2005


저자 주소

   Yakov Shafranovich
   SolidMatrix Technologies, Inc.

   EMail: ietf@shaftek.org
   URI:   http://www.shaftek.org





















































Shafranovich                 Informational                      [Page 7]

RFC 4180       Common Format and MIME Type for CSV Files    October 2005


전체 저작권 고지문

   Copyright (C) The Internet Society (2005).

   이 문서는 BCP 78에 포함된 권리, 라이선스 및 제한 사항의 적용을 받으며,
   그 안에 명시된 경우를 제외하고 저자들은 그들의 모든 권리를 보유한다.

   이 문서와 여기에 담긴 정보는 "있는 그대로(AS IS)"의 기준으로 제공되며,
   기여자, 그가/그녀가 대표하거나 후원받는 조직(있는 경우), 인터넷 소사이어티
   및 인터넷 엔지니어링 태스크 포스는 명시적이든 묵시적이든 모든 보증을
   부인한다. 여기에는 여기에 담긴 정보의 사용이 어떠한 권리도 침해하지 않을
   것이라는 보증이나 상품성 또는 특정 목적에의 적합성에 대한 묵시적 보증이
   포함되나 이에 국한되지 않는다.

지적 재산권

   IETF는 이 문서에 기술된 기술의 구현이나 사용과 관련된다고 주장될 수 있는
   지적 재산권 또는 기타 권리의 유효성이나 범위에 관하여, 또한 그러한 권리
   하의 라이선스가 이용 가능한지 여부의 범위에 관하여 어떠한 입장도 취하지
   않는다. 또한 IETF가 그러한 권리를 식별하기 위한 어떠한 독립적인 노력도
   기울였다고 표명하지 않는다. RFC 문서의 권리에 관한 절차에 대한 정보는
   BCP 78과 BCP 79에서 찾을 수 있다.

   IETF 사무국에 제출된 IPR 공개와 이용 가능하게 될 라이선스에 대한 보증,
   또는 이 사양의 구현자나 사용자가 그러한 독점적 권리를 사용하기 위한
   일반 라이선스나 허가를 얻으려는 시도의 결과는 http://www.ietf.org/ipr에
   있는 IETF 온라인 IPR 저장소에서 얻을 수 있다.

   IETF는 이 표준을 구현하는 데 필요할 수 있는 기술을 다룰 수 있는 저작권,
   특허 또는 특허 출원, 또는 기타 독점적 권리에 대해 주의를 환기시켜 줄 것을
   모든 관심 있는 당사자에게 요청한다. 정보는 IETF의 ietf-ipr@ietf.org로
   보내주기 바란다.

감사의 글

   RFC 편집자 기능에 대한 자금은 현재 인터넷 소사이어티가 제공한다.





Shafranovich                 Informational                      [Page 8]
