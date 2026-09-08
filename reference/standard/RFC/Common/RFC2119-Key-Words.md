Network Working Group                                         S. Bradner
Request for Comments: 2119                            Harvard University
BCP: 14                                                       March 1997
Category: Best Current Practice


        RFC에서 요구 수준을 나타내기 위해 사용하는 핵심 단어

본 메모의 상태

   - 인터넷 커뮤니티를 위한 인터넷 최선의 현행 관례(Internet Best Current Practices) 명시
   - 개선을 위한 논의와 제안 요청
   - 메모 배포 무제한

요약

많은 표준 트랙 문서는 요구 사항을 나타내는 단어를 대문자로 표기한다. 이 문서는 그 단어들을 IETF 문서에서 어떻게 해석해야 하는지 정의한다. 이 지침을 따르는 문서에는 서두에 다음 문구를 포함해야 한다.

      이 문서에서 "MUST", "MUST NOT", "REQUIRED", "SHALL", "SHALL
      NOT", "SHOULD", "SHOULD NOT", "RECOMMENDED", "MAY", 그리고
      "OPTIONAL"이라는 핵심 단어는 RFC 2119에 기술된 대로 해석되어야
      한다.

   - 이 단어들의 효력: 사용된 문서의 요구 수준에 따라 조정됨

1. MUST

MUST, "REQUIRED", "SHALL"은 해당 정의가 명세의 절대적인 요구 사항임을 뜻한다.

2. MUST NOT

MUST NOT과 "SHALL NOT"은 해당 정의가 명세의 절대적인 금지 사항임을 뜻한다.

3. SHOULD

SHOULD와 "RECOMMENDED"는 특정 상황에서 해당 항목을 따르지 않을 타당한 이유가 있을 수 있음을 뜻한다. 다만 다른 방법을 선택하기 전에 그에 따른 모든 영향을 충분히 이해하고 신중하게 검토해야 한다.

4. SHOULD NOT

SHOULD NOT과 "NOT RECOMMENDED"는 특정 상황에서 해당 행위를 허용하거나 유용하게 사용할 타당한 이유가 있을 수 있음을 뜻한다. 그 행위를 구현하기 전에는 모든 영향을 충분히 이해하고 해당 상황을 신중하게 검토해야 한다.

5. MAY

MAY와 "OPTIONAL"은 해당 항목이 선택 사항임을 뜻한다. 한 업체는 시장의 요구나 제품 개선을 위해 포함하고, 다른 업체는 같은 항목을 생략할 수 있다.

옵션을 생략한 구현도 그 옵션이 있는 구현과 상호 운용할 수 있어야 한다(MUST). 다만 기능은 줄어들 수 있다. 반대로 옵션을 포함한 구현도 해당 옵션이 제공하는 기능을 제외하고는 옵션 없는 구현과 상호 운용할 수 있어야 한다(MUST).

6. 이러한 명령어 사용에 대한 지침

이 문서에서 정의한 요구 수준 표현은 신중하게 사용해야 한다. 상호 운용에 실제로 필요하거나 재전송 제한처럼 피해를 일으킬 수 있는 행위를 제한할 때만 사용해야 한다(MUST). 상호 운용과 관계없이 구현자에게 특정 방법을 강제하는 용도로 사용해서는 안 된다.

7. 보안 고려 사항

이 용어들은 보안에 영향을 주는 행위를 명시할 때도 자주 쓰인다. MUST나 SHOULD를 구현하지 않거나, MUST NOT이나 SHOULD NOT으로 명시한 행위를 수행했을 때의 보안 영향은 알아차리기 어려울 수 있다. 구현자는 명세를 만드는 과정의 경험과 논의를 모두 알기 어렵다. 따라서 작성자는 권고나 요구 사항을 따르지 않을 때 어떤 보안 문제가 생기는지 자세히 설명해야 한다.

7-1. RFC 8174에 의한 갱신

RFC 8174는 이 문서를 갱신하면서 핵심 단어가 대문자일 때만 규범적 의미를 갖는다고 명확히 했다. "must", "should", "may"처럼 소문자로 쓰였다면 여기서 정의한 특별한 의미를 갖지 않는다.

8. 감사의 글

   - 이 용어들의 정의: 다수의 RFC에서 가져온 정의들의 합성물
   - Robert Ullmann, Thomas Narten, Neal McBurnett, Robert Elz를 포함한 여러 사람들의 제안 반영

9. 저자 주소

      Scott Bradner
      Harvard University
      1350 Mass. Ave.
      Cambridge, MA 02138

      phone - +1 617 495 3864

      email - sob@harvard.edu
</content>
