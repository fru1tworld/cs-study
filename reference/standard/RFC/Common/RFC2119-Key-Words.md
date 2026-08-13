Network Working Group                                         S. Bradner
Request for Comments: 2119                            Harvard University
BCP: 14                                                       March 1997
Category: Best Current Practice


        RFC에서 요구 수준을 나타내기 위해 사용하는 핵심 단어

본 메모의 상태

   - 인터넷 커뮤니티를 위한 인터넷 최선의 현행 관례(Internet Best Current Practices) 명시
   - 개선을 위한 논의·제안 요청
   - 메모 배포 무제한

요약

   - 많은 표준 트랙 문서: 명세의 요구 사항 표현에 여러 단어 사용, 종종 대문자로 표기
   - 본 문서: 이 단어들이 IETF 문서에서 어떻게 해석되어야 하는지 정의
   - 지침을 따르는 저자: 문서 서두 근처에 다음 문구 포함 필요

      이 문서에서 "MUST", "MUST NOT", "REQUIRED", "SHALL", "SHALL
      NOT", "SHOULD", "SHOULD NOT", "RECOMMENDED", "MAY", 그리고
      "OPTIONAL"이라는 핵심 단어는 RFC 2119에 기술된 대로 해석되어야
      한다.

   - 이 단어들의 효력: 사용된 문서의 요구 수준에 따라 조정됨

1. MUST

   - MUST, 또는 "REQUIRED"·"SHALL": 해당 정의가 명세의 절대적인 요구 사항임을 의미

2. MUST NOT

   - MUST NOT, 또는 "SHALL NOT": 해당 정의가 명세의 절대적인 금지 사항임을 의미

3. SHOULD

   - SHOULD, 또는 "RECOMMENDED": 특정 상황에서 특정 항목을 무시할 타당한 이유 존재 가능
   - 다만 다른 방향 선택 전 → 그에 따른 모든 영향을 충분히 이해하고 신중하게 검토 필요

4. SHOULD NOT

   - SHOULD NOT, 또는 "NOT RECOMMENDED": 특정 상황에서 특정 행위가 허용되거나 유용할 타당한 이유 존재 가능
   - 다만 해당 행위 구현 전 → 그에 따른 모든 영향을 충분히 이해하고 해당 경우를 신중하게 검토 필요

5. MAY

   - MAY, 또는 "OPTIONAL": 해당 항목이 진정으로 선택적임을 의미
   - 한 벤더: 특정 시장 요구 또는 제품 향상 판단에 따라 포함 가능, 다른 벤더는 동일 항목 생략 가능
   - 특정 옵션 미포함 구현 → 해당 옵션 포함 구현과 상호 운용 가능하도록 준비 필요(MUST), 단 기능 축소 가능
   - 특정 옵션 포함 구현 → 해당 옵션 미포함 구현과도 상호 운용 가능하도록 준비 필요(MUST), 단 해당 옵션 제공 기능은 제외

6. 이러한 명령어 사용에 대한 지침

   - 이 메모에서 정의한 유형의 명령어: 주의를 기울여 아껴서 사용 필요
   - 상호 운용을 위해 실제로 필요하거나 해를 끼칠 가능성이 있는 행위를 제한하기 위한 목적(예: 재전송 제한)에만 사용(MUST)
   - 상호 운용성에 필요하지 않은 경우 → 구현자에게 특정 방법을 강제하기 위한 용도로 사용 금지

7. 보안 고려 사항

   - 이 용어들: 보안에 영향을 미치는 행위를 명시하기 위해 자주 사용
   - MUST/SHOULD 미구현, 또는 명세에서 MUST NOT·SHOULD NOT이라고 명시한 행위 수행 → 보안에 미치는 영향이 매우 미묘할 가능
   - 대부분의 구현자: 해당 명세를 만들어 낸 경험과 논의의 혜택을 누리지 못함 → 문서 저자는 권고 사항이나 요구 사항을 따르지 않았을 때의 보안 영향을 상세히 설명하는 데 시간을 들일 필요

7-1. RFC 8174에 의한 갱신

   - RFC 8174: 이 문서를 갱신, 핵심 단어가 규범적 의미를 가지려면 반드시 대문자로 표기되어야 함을 명확화
   - "must", "should", "may"처럼 소문자로 쓰인 경우 → 여기서 정의한 특별한 의미 없음

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
