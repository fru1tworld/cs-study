# 빅 볼 오브 머드 (Big Ball of Mud) — 2부

> 원문: [Big Ball of Mud](http://www.laputan.org/mud/) (Brian Foote & Joseph Yoder, PLoP '97/EuroPLoP '97, 1997; *Pattern Languages of Program Design 4*, Addison-Wesley, 2000년 수록)
> 저자: Brian Foote, Joseph Yoder | 정리일: 2026-07-10
>
> 이 문서는 원문 논문을 그대로 옮긴 완역이 아니라, 저작권을 고려하여 원문의 절 구성과 논지를 빠짐없이 따라가며 한국어로 재서술한 학습 노트다. 핵심 개념을 나타내는 짧은 문구만 인용부호로 소량 인용했다. 1부는 [051-big-ball-of-mud-part1.md](./051-big-ball-of-mud-part1.md)를 참고.

## 패턴: SHEARING LAYERS (전단층)

저자들은 생태학자 R. V. 오닐의 관찰을 인용하며 이 패턴을 연다. 벌새와 꽃은 빠르게, 삼나무는 느리게, 삼나무 숲 전체는 더 느리게 변화하며, 대부분의 상호작용은 같은 속도 층위 안에서만 일어난다는 것이다("Most interaction is within the same pace level").

SHEARING LAYERS 개념은 스튜어트 브랜드의 『건물은 어떻게 배우는가(How Buildings Learn)』의 핵심 아이디어 중 하나이며, 브랜드는 이를 영국 설계자 프랭크 더피와 생태학자 오닐 등 여러 출처에서 종합했다. 더피는 "건물이라는 단일한 것은 존재하지 않는다. 제대로 이해하면 건물은 여러 층위의 수명을 가진 구성요소들의 집합"이라고 말했다고 브랜드는 전한다.

브랜드는 더피의 layer 개념을 여섯 가지로 정리한다.
- **Site(대지)**: 지리적 입지. 영구적이다.
- **Structure(구조)**: 기초·골조 등 하중을 지탱하는 요소. 30~300년 지속.
- **Skin(표피)**: 외장재, 창호 등 외부 표면. 대략 20년 주기.
- **Services(설비)**: 난방, 배선, 배관 등 건물의 순환계·신경계. 7~15년 주기로 더 빨리 마모·노후화된다.
- **Space Plan(공간 계획)**: 벽, 바닥, 천장. 상업 공간은 3년마다 바뀌기도 한다.
- **Stuff(집기)**: 조명, 의자, 가전 등. 끊임없이 유동적이다.

**문제**: "서로 다른 산출물은 서로 다른 속도로 변화한다"는 사실 자체가 이 패턴이 다루는 상황이다.

이를 뒷받침하는 두 힘이 서로 긴장 관계에 있다. **적응성**(Adaptability)은 폭넓은 요구사항에 유연하게 대응할 수 있는 능력이고, **안정성**(Stability)은 시스템이 이미 잘하고 있는 일(비용, 품질, 기능, 성능 등에서 경쟁 우위를 확보한 부분)을 순간적인 유행이나 변덕으로부터 지켜내는 능력이다. 시스템은 자신이 잘하는 영역에서 이미 어렵게 얻은 승리를 낭비해서는 안 된다.

**해법**: "비슷한 속도로 변화하는 요소들을 함께 묶도록 시스템을 계층화하라"는 것이다.

시스템 안의 상호작용은 대부분 같은 층 안에서, 혹은 인접한 층 사이에서 일어난다. 서로 다른 속도로 변화하는 것들은 갈라져 나가며, 이런 변화율의 차이가 층의 출현을 촉진한다. 브랜드는 이런 층에 맞춰 직업적 전문성도 함께 생겨난다고 지적한다(인테리어 담당자와 건축가의 분업처럼). 이는 로버츠와 존슨이 말한 "변하는 것과 변하지 않는 것을 분리하라"는 원칙을 더 큰 스케일로 적용한 것이다.

소프트웨어에서도 이런 층을 찾을 수 있다고 저자들은 말한다. 가장 아래에는 데이터가 있다. 가장 빠르게 변하는 것들은 데이터로 흘러들어 가는데, 소프트웨어에서 가장 변경하기 쉬운 부분이 데이터이기 때문이다. 코드는 데이터보다 느리게 변하며 프로그래머·분석가·설계자의 영역이다. 객체지향 언어에서는 빨리 변할 요소는 블랙박스 다형성 컴포넌트로, 덜 자주 변할 요소는 화이트박스 상속으로 표현되는 경향이 있다. 프레임워크를 구성하는 추상 클래스와 컴포넌트는 그 위에서 지어지는 애플리케이션보다 더 느리게 변한다. 언어는 프레임워크보다 더 느리게 변하며, 학자와 표준위원회의 영역이다.

빠르게 진화하는 산출물은 시스템에 동적임과 유연함을 부여하고, 느리게 진화하는 산출물은 변화에 맞선 방벽 역할을 하며 환경과의 상호작용에서 시스템이 축적한 지혜를 체화한다는 비유가 이어진다. 널리 채택되고 배치된 것일수록 변경에 저항이 커지는데, 예컨대 대형 엔터프라이즈 데이터베이스의 스키마 재편이 비용이 크고 시간이 오래 걸리는 작업이라 데이터베이스 설계자·관리자들이 변경을 꺼리게 되는 현상이 그 예다.

저자들은 METADATA 패턴([Foote & Yoder 1998b])도 언급한다. 복잡성과 힘을 데이터 쪽으로 밀어 넣으면, 그 힘이 프로그래머의 영역에서 사용자 자신의 영역으로 옮겨간다는 발상이다. 이는 사무실 파티션을 건축가나 시공업체 없이도 손쉽게 옮길 수 있는 모듈식 사무 가구에 비유된다. 시간이 흐르며 프레임워크·추상 클래스·컴포넌트는 도메인 구조에 대해 학습한 바를 체화하고, 유동적인 것들은 사용자가 직접 다룰 수 있는 데이터로 빠져나간다. 저자들은 이 과정을 변화가 돌리는 원심분리기에 비유한다.

알렉산더의 말도 인용된다. 좋은 것들은 어떤 특정한 구조를 가지는데, 그런 구조는 오직 동적으로만 얻어질 수 있으며, 이는 자연의 지속적이고 아주 미세한 피드백 루프를 통한 적응이 있어야만 가능하다는 취지다("Things that are good have a certain kind of structure. You can't get that structure except dynamically").

이 패턴은 로버츠와 존슨의 HOT SPOTS 패턴과도 밀접하게 연관된다. 변하는 것과 변하지 않는 것을 분리하는 힘이 SHEARING LAYERS를 만들어내는데, 이 층들은 변화율 차이의 결과물인 반면 HOT SPOTS는 그 층 사이의 단층선을 따라 일어나는 파열대, 즉 어긋남이 실제로 일어나는 지점으로 볼 수 있다. 이 지각 변동적 비유는 저자들 자신의 SOFTWARE TECTONICS 패턴과도 이어지며, METADATA와 ACTIVE OBJECT-MODELS는 힘을 데이터 쪽, 즉 사용자 쪽으로 밀어냄으로써 시스템이 변화하는 요구사항에 더 빨리 적응하도록 돕는다.

## 패턴: SWEEPING IT UNDER THE RUG (양탄자 밑으로 쓸어 넣기)

**별칭**: POTEMKIN VILLAGE(포템킨 마을), HOUSECLEANING(집안 청소), PRETTY FACE(예쁜 얼굴), QUARANTINE(격리), HIDING IT UNDER THE BED(침대 밑에 숨기기), REHABILITATION(재활)

저자들은 이 전략을 상징하는 극적인 사례로, 소련이 체르노빌 4호 원자로를 만 년짜리 콘크리트 석관으로 봉인한 일을 든다. 문제를 완전히 없앨 수 없다면 적어도 숨길 수는 있다는 것이다. 도시재생 사업이 낙서 위에 벽화를 그리고 폐가 주변에 울타리를 두르는 것으로 시작하듯, 아이들도 방바닥에 어지럽게 널린 것보다는 옷장 안에 몰아넣은 무더기 하나가 낫다는 걸 일찍 배운다.

**문제**: "지나치게 자라 뒤엉킨 스파게티 코드는 이해·수리·확장이 어렵고, 손대지 않으면 더 나빠지는 경향이 있다"는 상황이다.

이를 뒷받침하는 힘으로 **이해 가능성**(Comprehensibility)과 **사기**(Morale)가 제시된다. 이해하기 쉽고 잘 만들어진 코드가 복잡하고 뒤엉킨 코드보다 유지보수·확장이 쉽다는 건 굳이 말할 필요도 없지만, 지저분한 코드를 정비하는 데는 시간과 비용이 든다. 그럼에도 그것을 방치해 계속 곪게 두는 비용을 과소평가해서는 안 된다. BIG BALL OF MUD와 함께 사는 대가는 손익계산서를 넘어선다. 사소한 수정조차 유지보수 마라톤으로 번지고, 프로그래머는 실 한 가닥을 잘못 당겼다가 예측 불가능한 결과가 생길까 봐 몸을 사리게 된다. 저자들은 이를 걸리버가 소인국 사람들에게 온몸이 묶인 상황에 비유한다.

**해법**: "어수선함을 쉽게 없앨 수 없다면, 적어도 한곳에 몰아 격리하라. 이렇게 하면 무질서가 고정된 영역으로 제한되고 눈에 띄지 않게 되며, 추가 리팩터링을 위한 발판이 마련된다"는 것이다.

카펫 밑 한곳에 먼지를 몰아넣으면, 최소한 그 위치를 알 수 있고 필요할 때 옮길 수도 있다. 여전히 손에 먼지 더미가 있지만 국지화되어 있고, 손님 눈에는 보이지 않는다. 체르노빌 4호기를 봉인한 엔지니어들이 보여줬듯, 때로는 문제를 뚜껑으로 덮은 뒤에야 비로소 본격적인 정리를 시작할 수 있다.

스파게티 코드를 다루기 시작하려면, 결합도가 상대적으로 느슨해 보이는 부분을 찾아 그곳에 아키텍처적 경계를 긋는 것이 첫걸음이다. 전역 정보를 별개의 데이터 구조로 분리하고, 이 구획들 사이의 통신을 잘 정의된 인터페이스를 통해서만 강제하는 식이다. 낡은 영역에 산뜻한 인터페이스를 씌우는 일은 아키텍처적 재활의 첫걸음이 될 수 있지만, 갈 길이 멀다. BIG BALL OF MUD에서 의미 있는 추상화를 뽑아내는 일은 어렵고 힘겨운 작업이며 기술·통찰·끈기를 요구한다. 때로는 RECONSTRUCTION이 덜 고통스러운 길처럼 보이기도 하지만, 이는 스크램블 에그를 원래대로 되돌리는 것과는 다르다.

이 패턴은 GoF의 FAÇADE 패턴, 그리고 켄트 벡의 INTENTION REVEALING SELECTORS와도 통한다. 이렇게 지저분한 덩어리들을 격리하고 나면, 그 기능을 좀 더 그럴듯한 이름의 인터페이스로 노출할 수 있다. 이는 CONSOLIDATION으로 가는 첫걸음이 될 수도 있는데, PROTOTYPING이나 EXPANSION 단계에서 발생했을 통제되지 않은 성장을 이 시점에서 억제하기 시작하는 것이기 때문이다.

## 패턴: RECONSTRUCTION (재구축)

**별칭**: TOTAL REWRITE(전면 재작성), DEMOLITION(철거), THROWAWAY THE FIRST ONE(첫 번째 것을 버려라), START OVER(다시 시작)

저자들은 1966년 지어진 애틀랜타 풀턴 카운티 스타디움이 1997년 철거된 사례를 든다. 두 가지 이유가 겹쳤다. 하나는 원래 구조가 1990년대 스포츠 경제가 요구하는 스카이박스 스위트를 도저히 개조로 수용할 수 없었다는 점, 다른 하나는 야구와 미식축구 두 종목 모두를 위한 값싼 범용 경기장을 만들려던 시도가 결국 양쪽 모두를 어중간하게 만족시키는 데 그쳤다는 점이다. 저자들은 이를 두고 예상치 못한 요구사항과 범용 컴포넌트 설계에 대해 우리가 얻을 교훈이 있지 않겠느냐고 묻는다.

익스트림 프로그래밍의 기원이 된 크라이슬러 종합보상 프로젝트(C3) 이야기도 짧게 언급된다. 표류하던 프로젝트가 1년 반 동안의 작업물을 버리고 처음부터 다시 시작하기로 한 결단에서 XP가 탄생했다는 것이다. 저자들은 다만 이 첫 시도에서 건져낸 경험의 가치가 충분히 조명받지 못했다는 점을 짚으며, 그 실패한 첫 초안이야말로 이 이야기의 숨은 공로자가 아니었겠냐고 반문한다.

**문제**: "코드가 손쓸 수 없거나 이해조차 불가능한 지경까지 퇴락했다"는 상황이다.

여러 힘이 이 패턴을 뒷받침한다. **노후화**(Obsolescence)는 시스템이 실제로 기술적·경제적으로 낡았다는 것이고, 기술적으로 뒤처졌지만 여전히 잘 팔리는 시스템과, 기술적으로는 우수하지만 비기술적 이유로 더 인기 있는 경쟁자에게 밀리는 시스템은 서로 다른 상황이다. 콘크리트와 철골의 세계에서는 쇠락이 증상이고 자본 철수가 원인이며, 이 과정이 시작되면 스스로를 부추기는 악순환이 될 수 있다. **변화**(Change)는 소프트웨어가 매우 유연한 매체임에도, 풀턴 카운티 스타디움처럼 새로운 요구가 시스템의 아키텍처적 가정을 정면으로 거스르는 방식으로 다가와 수용이 사실상 불가능해지는 경우가 있다는 것이다. **비용**(Cost)은 시스템을 손절하는 일이 그것을 만든 사람들과 비용을 댄 사람들 모두에게 트라우마가 될 수 있다는 것이며, 다시 짓는다고 해서 그 개념적 설계나 인력의 경험까지 버려지는 것은 아니라는 점도 함께 지적된다. **조직**(Organization)은 시스템을 처음부터 다시 짓는 일이 높은 가시성을 갖는 대규모 사업이라, 상당한 시간과 자원, 그리고 경영진의 지원이 필수적이라는 것이다.

**해법**: "버리고 처음부터 다시 시작하라"는 것이다.

때로는 시스템을 버리고 새로 시작하는 편이 그냥 더 쉽다. 새로 시작하는 일은 낡은 코드에게 진 패배로 볼 수도, 그것에 대한 승리로 볼 수도 있다. 다시 짓는 이유 중 하나는 기존 시스템을 만든 사람들이 이미 조직을 떠나고 없다는 데 있다. 재작성은 새 인력에게 아키텍처와 구현 사이의 접점을 다시 이어주는 방편이 된다. 때로는 시스템을 직접 짜보는 것만이 그것을 이해하는 유일한 방법이기도 하다. 새로운 시스템이 진공 상태에서 설계되는 것은 아니다. 낡은 시스템이라는 브룩스의 타르 구덩이를 파헤쳐, 그 화석들을 살펴보고 무엇이 살아있는 자들에게 교훈이 될지 검토하는 철저한 사후 검토가 반드시 필요하다.

시스템이 BIG BALL OF MUD가 되면, 그 상대적 몰이해성이 오히려 시스템의 종말을 앞당길 수 있다. 변화에 저항하기 때문에 존속은 하지만, 같은 이유로 진화하지 못한다는 것이다.

버리고 다시 시작하는 것 외의 대안도 함께 제시된다. 하나는 점진적 리팩터링을 통해 진창에서 아키텍처적 요소와 식별 가능한 추상화를 캐내는 것으로, SWEEPING IT UNDER THE RUG에서 제안했듯 결합이 느슨한 지점부터 찾아 나가는 방식이다. 다른 하나는 시스템의 전체 또는 일부를 대체할 만한 새 컴포넌트나 프레임워크가 등장했는지 재평가하는 것이다. 기존 컴포넌트를 재사용·개조할 수 있다면, 지금 가진 것을 재구축·수리·유지하는 데 드는 시간과 비용을 아낄 수 있다.

저자들은 소프트웨어 자산이 미국 상무부가 정의하는 "내구재(3년 이상 지속되도록 설계된 재화)" 범주에, 컴퓨터 하드웨어보다 소프트웨어 산출물이 오히려 더 걸맞은 역설적 상황이 되어가고 있다고 짚는다. 애플의 리사 툴킷과 그 후속인 매킨토시 툴박스가 개인용 컴퓨터 역사에서 RECONSTRUCTION의 흥미로운 사례로 소개되며, 프랭크 로이드 라이트의 말도 인용된다. 건축가에게 가장 유용한 도구는 제도판 위의 지우개와 현장의 쇠지렛대라는 것이다("An architect's most useful tools are an eraser at the drafting board, and a wrecking bar at the site").

저자들은 자신들의 SOFTWARE TECTONICS 패턴을 다시 언급하며, 점진적 변화가 무한정 미뤄지면 결국 큰 격변만이 유일한 대안이 될 수 있다고 지적한다. 또한 [Foote & Yoder 1998a]에서 다룬 "이기는 팀(WINNING TEAM)" 현상, 즉 기술적으로 더 우수한 해법이 비기술적 사정에 밀려나는 현상도 언급된다. 마지막으로 브룩스가 말한 "2차 시스템 증후군(second-system effect)" — 설계자가 만드는 가장 위험한 시스템은 바로 그의 두 번째 시스템이라는 유명한 관찰 — 이 소개되며, RECONSTRUCTION은 이런 지나친 자신감이 발휘될 기회를 제공하므로 경계해야 한다고 저자들은 당부한다. 그럼에도 시스템을 개선할 유일한 방법이 버리고 다시 시작하는 것뿐인 때도 있다며, 브룩스의 오래된 경구 "어차피 하나는 버리게 될 테니, 버릴 계획을 세워라(plan to throw one away, you will anyway)"를 새길 만하다고 마무리한다.

## 결론

저자들은 결국 소프트웨어 아키텍처란 우리가 경험을 지혜로 증류하고 퍼뜨리는 방법에 관한 것이라고 정리한다. 이 논문의 패턴들을 안티패턴으로 여기지 않는다는 입장도 재확인한다. 좋은 프로그래머들이 BIG BALL OF MUD를 만드는 데는 그럴 만한 이유가 있으며, 시장이 워낙 빠르게 움직이는 탓에 장기적 아키텍처 야심이 무모해 보이고, 편의적이고 속전속결식의 일회용 프로그래밍이 오히려 최신 전략일 수도 있다는 것이다. 이런 접근의 성공은 부인할 수 없으며, 그 자체로 이것이 하나의 패턴임을 증명한다. 사람들이 BIG BALL OF MUD를 만드는 이유는 그것이 통하기 때문이며, 많은 영역에서 그것이 유일하게 통한다고 입증된 방식이기도 하다.

그럼에도 저자들은 BIG BALL OF MUD를 단죄하려는 의도가 아니라고 밝히면서도, 독자라면 자신들이 더 나은 것을 지향하기를 바란다는 것을 짐작할 수 있을 거라고 말한다. 아키텍처적 쇠퇴를 부르는 힘과, 그 힘에 언제 어떻게 맞설 수 있는지를 인식함으로써, 오래 지속되는 산출물이 등장할 토대를 마련하고자 한다는 것이다. 핵심은 시스템과 프로그래머, 나아가 조직 전체가 시스템이 자라고 성숙하는 동안 도메인과 그 안에 숨은 아키텍처적 기회에 대해 배우는 것이다.

적당한 수준의 무질서는 소프트웨어 진화가 겪는 밀물과 썰물의 자연스러운 일부다. 노련한 요리사가 어질러진 주방을 참아내듯, 개발자도 새로운 영역을 처음 탐험할 때 신발에 진흙이 좀 묻는 것을 두려워해서는 안 된다. 아키텍처적 통찰은 종합계획의 산물이 아니라 힘겹게 얻어낸 경험의 산물이다. 예전 소프트웨어 아키텍트들에게는 성공적인 초안을 거듭 만드는 과정에서 얻은 교훈을 적용하는 것 외에 다른 선택지가 별로 없었는데, RECONSTRUCTION이 평범한 시스템을 더 나은 것으로 대체할 실질적인 유일한 수단이었기 때문이다. 객체, 프레임워크, 컴포넌트, 리팩터링 도구는 이제 또 다른 대안을 제공한다.

모스크바의 성 바실리 대성당(원래 이름은 모스크바 해자 옆 성모 중보 대성당) 일화도 소개된다. 전설에 따르면 이반 뇌제는 이 성당이 완공되자 건축가들의 눈을 멀게 해 다시는 이보다 아름다운 것을 짓지 못하게 했다고 한다. 저자들은 오늘날 소프트웨어 아키텍처의 현주소를 보면, 우리 대부분은 시력을 걱정할 필요가 없다고 씁쓸하게 덧붙인다.

## 감사의 말 (요약)

저자들은 일리노이 대학 소프트웨어 아키텍처 그룹 구성원들, PLoP '97 학회 라이터스 워크숍 참가자들, 시카고·보스턴·뉴욕·시드니의 여러 패턴 커뮤니티, 그리고 여러 초안을 검토해 준 존 블리사이즈, 닐 해리슨, 한스 로너트, 제임스 코플린, 랄프 존슨 등에게 감사를 표한다. 또한 이 논문이 다소 디스토피아적이고 딜버트풍 정서를 담고 있다는 지적에 공감하며, 딜버트 만화를 게재하도록 허락해 준 유나이티드 피처 신디케이트에도 감사를 전한다.

## 참고문헌 (요약)

원문에는 다음과 같은 주요 문헌이 인용되어 있다(전체 서지 정보는 원문 참고).

- Christopher Alexander, *Notes on the Synthesis of Form* (1964), *The Timeless Way of Building* (1979), *A Pattern Language* (공저, 1977), *The Oregon Experiment* (1988)
- James Bach, *Good Enough Software: Beyond the Buzzword* (1997)
- Kent Beck, *Smalltalk Best Practice Patterns* (1997), *Embracing Change: Extreme Programming Explained* (2000); Kent Beck & Ward Cunningham, *A Laboratory for Teaching Object-Oriented Thinking* (OOPSLA '89)
- Grady Booch, *Object-Oriented Analysis and Design with Applications* (1994)
- Stewart Brand, *How Buildings Learn: What Happens After They're Built* (1994)
- Frederick P. Brooks Jr., *The Mythical Man-Month* (기념판, 1995)
- William J. Brown 외, *AntiPatterns: Refactoring, Software Architectures, and Projects in Crisis* (1998)
- Frank Buschmann 외, *Pattern-Oriented Software Architecture: A System of Patterns* (1996)
- James O. Coplien, *A Generative Development-Process Pattern Language* (PLoP '94)
- Ward Cunningham, *Peter Principle of Programming*, *The Most Complicated Thing that Could Possibly Work* (Portland Pattern Repository, 1999)
- Michael A. Cusumano & Richard W. Shelby, *Microsoft Secrets* (1995)
- Brian Foote, *Designing to Facilitate Change with Object-Oriented Frameworks* (석사논문, 1988); Brian Foote & William F. Opdyke, *Lifecycle and Refactoring Patterns that Support Evolution and Reuse* (PLoP '94); Brian Foote & Joseph W. Yoder, *Evolution, Architecture, and Metamorphosis* (PLoP '95), *The Selfish Class* (PLoP '96), *Metadata* (PLoP '98); Brian Foote & Don Roberts, *Lingua Franca* (PLoP '98)
- Martin Fowler, *Refactoring: Improving the Design of Existing Code* (1999)
- Richard P. Gabriel, *Lisp: Good News, Bad News, and How to Win Big*, *Patterns of Software: Tales from the Software Community* (1996)
- Erich Gamma, Richard Helm, Ralph Johnson, John Vlissides, *Design Patterns: Elements of Reusable Object-Oriented Software* (1995)
- David Garlan & Mary Shaw, *An Introduction to Software Architecture* (1993)
- Daniel H. H. Ingalls, *The Evolution of the Smalltalk Virtual Machine* (1983)
- Ralph Johnson & Brian Foote, *Designing Reusable Classes* (1988)
- Brian Marick, *The Craft of Software Testing* (1995)
- Gerard Meszaros, *Archi-Patterns: A Process Pattern Language for Defining Architectures* (PLoP '97)
- Don Roberts & Ralph E. Johnson, *Evolve Frameworks into Domain-Specific Languages* (PLoP '96)
- Mary Shaw, *Some Patterns for Software Architectures* (PLoP '95)
- Herbert A. Simon, *The Sciences of the Artificial* (1969)
- Jonathan Swift, *Gulliver's Travels* (1726)
- Marcus Vitruvius Pollio, *De Architectura* (기원전 20년경)
