# 타르 웅덩이에서 벗어나기 (Out of the Tar Pit) — Part 4

> 원문: [Out of the Tar Pit](https://curtclifton.net/papers/MoseleyMarks06a.pdf)
> 저자: Ben Moseley, Peter Marks | 번역일: 2026-07-10

[← Part 3](012-out-of-the-tar-pit-part3.md) | [Part 1](012-out-of-the-tar-pit-part1.md)

이 파트: 10 FRP 시스템 예제 / 11 관련 연구 / 12 결론 / 참고 문헌

---

## 10 FRP 시스템 예제

이제 간단한 예제 FRP 시스템을 살펴본다. 이 시스템은 부동산 중개(estate agency) 사업을 지원하도록 설계되었다. 판매 중인 매물, 매물에 대한 제안(offer), 제안에 대해 소유자가 내린 결정, 그리고 성사된 판매로 개별 중개소 직원이 벌어들인 수수료를 추적한다. 이 예제는 FRP 시스템 컴포넌트들의 선언적 성격을 부각하는 역할을 할 것이다.

단순함을 위해 이 시스템은 몇 가지 제약 아래 운영된다:

1. 판매만 다룬다 — 임대는 없다
2. 사람은 집을 하나만 가지며, 소유자는 자신이 파는 매물에 거주한다
3. 방은 완벽한 직사각형이다
4. 제안 수락은 구속력이 있다(즉 수락된 제안은 판매를 구성한다)

이 예제는 가상의 FRP 인프라(관계 대수뿐 아니라 8.5절의 흔한 확장 일부도 지원하는)의 문법을 사용한다 — 이를 위해 타자기 글꼴을 쓴다.

### 10.1 사용자 정의 타입

예제 시스템은 소수의 커스텀 타입을 사용하며(9.3절 참조), 그중 일부는 인프라가 제공하는 타입의 별칭일 뿐이다:

```
def alias address : string
def alias agent : string
def alias name : string
def alias price : double
def enum roomType : KITCHEN | BATHROOM | LIVING_ROOM
def enum priceBand : LOW | MED | HIGH | PREMIUM
def enum areaCode : CITY | SUBURBAN | RURAL
def enum speedBand : VERY_FAST | FAST | MEDIUM | SLOW |
VERY_SLOW
```

### 10.2 본질적 상태

본질적 상태는(9.1.1절 참조) base relvar들의 *타입* 정의로 이루어진다(속성의 타입은 *이탤릭*으로 표시).

```
def relvar Property :: {address:address price:price
     photo:filename agent:agent dateRegistered:date}

def relvar Offer :: {address:address offerPrice:price
     offerDate:date bidderName:name bidderAddress:address}

def relvar Decision :: {address:address offerDate:date
     bidderName:name bidderAddress:address decisionDate:date
     accepted:bool}

def relvar Room :: {address:address roomName:string
     width:double breadth:double type:roomType}

def relvar Floor :: {address:address roomName:string
     floor:int}

def relvar Commission :: {priceBand:priceBand
     areaCode:areaCode saleSpeed:speedBand commission:double}
```

예제는 여섯 개의 기본 관계를 사용하며, 대부분은 이름만으로 뜻이 통한다.

Property 관계는 팔렸거나 판매 중인 모든 매물을 저장한다. 10.3.3절에서 보게 되듯 매물은 *address*로 고유하게 식별된다. *price*는 희망 판매 가격이고, *agent*는 그 매물의 판매를 담당하는 중개소 직원이며, *dateRegistered*는 매물이 중개소에 판매 등록된 날짜다.

Offer 관계는 지금까지 제출된 모든 제안의 이력을 기록한다. *address*는 제안의 대상이 되는 매물을 나타내고, 제안자는 *bidderAddress*에 거주하는 *bidderName*이다. *offerDate* 속성은 제안이 이루어진 날짜를, *offerPrice*는 제안된 가격을 기록한다. 제안은 (*address*, *offerDate*, *bidderName*, *bidderAddress*) 조합으로 고유하게 식별된다.

Decision 관계는 제출된 Offer에 대해 소유자가 내린 결정을 기록한다. 해당 Offer는 (*address*, *offerDate*, *bidderName*, *bidderAddress*) 속성으로 식별되고, 결정의 날짜와 결과는 (*decisionDate*와 *accepted*)로 기록된다.

Room 관계는 각 매물에 존재하는 방들에 관한 정보(*width*, *breadth*, *type*)를 기록한다. 매물은 당연히 *address*로 표현된다. 짚어둘 만한 점 하나는(약간 인위적이기 때문인데) 각 Property의 모든 Room이 (그 Property 범위 안에서) 고유한 *roomName*을 갖는다는 가정이다. 많은 매물이 같은 *type*(과 크기)의 방을 둘 이상 가질 수 있으므로 이 가정이 필요하다.

Floor 관계는 각 Room(*roomName*, *address*)이 어느 *floor*(층)에 있는지 기록한다.

마지막으로 Commission 관계는 중개소 직원이 벌 수 있는 *commission* 수수료를 저장한다. 수수료는 판매 *price*를 여러 *priceBand*로 나누고, 매물 *address*를 *areaCode*로 분류하고, *saleSpeed* 등급을 매긴 것을 근거로 배정된다. (수수료율을 — 함수가 아니라 — 기본 관계로 표현하기로 한 것은, 수수료를 조회하고 쉽게 조정할 수 있게 하기 위해서다.)

### 10.3 본질적 로직

이것은 시스템의 심장이며(9.1.2절 참조) "비즈니스 로직"에 해당한다.

#### 10.3.1 함수

여기서는 실제 함수 정의는 제시하지 않고 동작만 비형식적으로 기술한다. 실제로는 인프라가 제공하는 어떤 언어로 함수 정의를 공급할 것이다.

**priceBandForPrice** 가격을 *priceBand*로 변환한다 (수수료 계산에 쓰인다)

**areaCodeForAddress** 주소를 *areaCode*로 변환한다

**datesToSpeedBand** 날짜 쌍을 *speedBand*로 변환한다 (계절을 감안한 판매 속도를 반영한다)

#### 10.3.2 파생 관계

시스템에는 열세 개의 파생 관계가 있다. 이는 주된 목적이 다른 파생 관계(와 제약)의 정의를 돕는 것인지, 사용자에게 정보를 제공하는 것인지에 따라 아주 느슨하게 *내부(internal)*와 *외부(external)*로 분류할 수 있다. 각각의 정의와 목적을 차례로 살펴본다.

이해를 돕기 위해 파생 관계의 타입을 주석(`/*`와 `*/`로 구분)으로 표시했다. 실제로는 이 타입들이 인프라가 제공하는 타입 추론 메커니즘으로 도출(또는 검사)될 것이다.

**내부**

열 개의 *내부* 파생 관계는 주로 뒤에 나오는 세 *외부* 파생 관계의 정의를 돕기 위해 존재한다.

```
/* RoomInfo :: {address:address roomName:string width:double
               breadth:double type:roomType roomSize:double} */
RoomInfo = extend(Room, (roomSize = width*breadth))
```

RoomInfo 파생 관계는 Room 기본 관계에 각 방의 면적을 주는 추가 속성 *roomSize*를 덧붙인 것일 뿐이다.

```
/* Acceptance :: {address:address offerDate:date bidderName:name
                 bidderAddress:address decisionDate:date} */
Acceptance = project_away(restrict(Decision | accepted == true),
                          accepted)
```

Acceptance 파생 관계는 Decision 기본 관계에서 긍정적인 항목만 골라낸 뒤 *accepted* 속성을 벗겨낸다(`project_away` 연산은 `project` 연산의 쌍대다 — 나열된 속성을 유지하는 대신 제거한다).

```
/* Rejection :: {address:address offerDate:date bidderName:name
                bidderAddress:address decisionDate:date} */
Rejection = project_away(restrict(Decision | accepted == false),
                         accepted)
```

Rejection 파생 관계는 부정적인 결정만 골라내고 *accepted* 속성을 제거한다.

```
/* PropertyInfo :: {address:address price:price photo:filename
                   agent:agent dateRegistered:date
                   priceBand:priceBand areaCode:areaCode
                   numberOfRooms:int squareFeet:double} */
PropertyInfo =
extend(Property,
       (priceBand = priceBandForPrice(price)),
       (areaCode = areaCodeForAddress(address)),
       (numberOfRooms = count(restrict(RoomInfo |
                                       address == address))),
       (squareFeet = sum(roomSize, restrict(RoomInfo |
                                        address == address))))
```

PropertyInfo 파생 관계는 Property 기본 관계에 네 개의 새 속성을 덧붙인다. 첫째 — *priceBand* — 는 매물이 중개소의 어느 가격대에 속하는지 나타낸다. 최종 판매 가격의 가격대는 그 매물을 판매한 중개인이 얻을 수수료에 영향을 준다. *areaCode* 속성은 지역 코드를 나타내며, 이것 역시 중개인이 벌 수 있는 수수료에 영향을 준다. *numberOfRooms*는 방의 수를 세어(실제로는 해당 주소의 RoomInfo 파생 관계 항목 수를 세어) 계산하고, *squareFeet*는 관련된 *roomSize*들을 합산해 계산한다.

```
/* CurrentOffer :: {address:address offerPrice:price
                   offerDate:date bidderName:name
                   bidderAddress:address} */
CurrentOffer =
summarize(Offer,
          project(Offer, address bidderName bidderAddress),
          quota(offerDate,1))
```

CurrentOffer 파생 관계의 목적은 더 새로운 제안으로 대체된 오래된 제안을 걸러내는 것이다(예컨대 제안자가 수정된 — 더 높거나 낮은 — 제안을 제출했다면, 같은 매물에 대해 그가 예전에 했을 수 있는 더 오래된 제안에는 더 이상 관심이 없다).

이 정의는 Offer 기본 관계를 요약하여, 각 제안자가 각 매물에 대해 한 가장 최근의(즉 *offerDate*가 단 하나로 가장 큰) 제안을(즉 고유한 *address*, *bidderName*, *bidderAddress* 조합마다) 취한다. *bidderName*과 *bidderAddress*가 둘 다 포함되므로, 같은 곳(*bidderAddress*)에 사는 서로 다른 사람들이 같은 매물(*address*)에 서로 다른 제안을 제출하는 (분명 흔치 않은) 가능성도 시스템이 지원한다.

```
/* RawSales :: {address:address offerPrice:price
               decisionDate:date agent:agent
               dateRegistered:date} */
RawSales =
project_away(join(Acceptance,
                  join(CurrentOffer,
                       project(Property, address agent
                                         dateRegistered))),
             offerDate bidderName bidderAddress)
```

이 예제의 목적상 판매는 수락된 제안에 직접 대응하는 것으로 본다. 그 결과 RawSales 관계의 정의는 Acceptance 관계의 관점에서 이루어진다. 이 수락된 제안들에 (합의된 *offerPrice*를 포함하는) CurrentOffer 정보와 Property 관계의 정보(*agent*, *dateRegistered*)를 조인으로 보강한다.

```
/* SoldProperty :: {address:address} */
SoldProperty = project(RawSales, address)
```

SoldProperty 관계는 판매가 합의된 모든 Property의 *address*만 담는다(즉 RawSales 관계에 있는 매물들).

```
/* UnsoldProperty :: {address:address} */
UnsoldProperty = minus(project(Property, address), SoldProperty)
```

UnsoldProperty는 당연히 SoldProperty가 아닌 Property일 뿐이다(즉 모든 Property 주소에서 SoldProperty 주소를 뺀 것).

```
/* SalesInfo :: {address:address agent:agent areaCode:areaCode
                saleSpeed:speedBand priceBand:priceBand} */
SalesInfo =
project(extend(RawSales,
               (areaCode = areaCodeForAddress(address)),
               (saleSpeed = datesToSpeedBand(dateRegistered,
                                             decisionDate)),
               (priceBand = priceBandForPrice(offerPrice))),
        address agent areaCode saleSpeed priceBand)
```

SalesInfo 관계는 RawSales 관계에 기반하되, 관련된 세 함수를 호출해 *areaCode*, *saleSpeed*, *priceBand* 정보로 확장한 것이다.

```
/* SalesCommissions :: {address:address agent:agent
                        commission:double} */
SalesCommissions =
project(join(SalesInfo, Commission),
        address agent commission)
```

중개인들에게 지급해야 할 SalesCommissions는 SalesInfo를 Commission 기본 관계와 조인하는 것만으로 파생된다. 이것으로 각 Property(*address*로 표현)에 대해 각 *agent*에게 지급해야 할 *commission* 금액이 나온다.

**외부**

*내부* 파생 관계를 모두 정의했으니, 이제 *외부* 파생 관계를 정의할 수 있다 — 이것들이 시스템 사용자들에게 가장 직접적인 관심 대상이 될 것들이다.

```
/* OpenOffers :: {address:address offerPrice:price
                 offerDate:date bidderName:name
                 bidderAddress:address} */
OpenOffers =
join(CurrentOffer,
     minus(project_away(CurrentOffer, offerPrice),
           project_away(Decision, accepted decisionDate)))
```

OpenOffers 관계는 소유자가 아직 Decision을 내리지 않은 CurrentOffer들의 상세 정보를 준다. 이는 (*offerPrice*를 포함하는) CurrentOffer 정보를, 대응하는 Decision이 없는 (가격 정보를 제외한) CurrentOffer들과 조인하여 계산한다. 여기서 `project_away`를 쓴 것은 `minus`가 인자들의 타입이 같기를 요구하기 때문이다.

```
/* PropertyForWebSite :: {address:address price:price
                         photo:filename numberOfRooms:int
                         squareFeet:double} */
PropertyForWebSite = project( join(UnsoldProperty, PropertyInfo),
                              address price photo
                              numberOfRooms squareFeet )
```

이 사업체는 자사 외부 웹사이트에 PropertyInfo의 정보를 표시하고자 한다. 그러나 팔리지 않은 매물만 보여주고 싶고(이는 단순한 `join`으로 달성된다), 속성의 부분집합만 보여주고 싶다(이는 `project`로 달성된다).

```
/* CommissionDue :: {agent:agent totalCommission:double} */
CommissionDue =
project(summarize(SalesCommissions,
                  project(SalesCommissions, agent),
                  totalCommission = sum(commission)),
        agent totalCommission)
```

마지막으로, 각 중개인에게 지급해야 할 총수수료는 SalesCommissions 관계의 *commission* 속성을 *agent*별로 합산해 *totalCommission* 속성을 만드는 것만으로 계산된다.

#### 10.3.3 무결성

무결성 제약은 관계 대수 또는 관계 해석 식의 형태로 주어진다. 이미 언급했듯 우리의 가상 FRP 인프라는 흔한 관계 대수 확장(8.5절 참조)을 제공한다. 또한 *후보(candidate)* 키와 *외래(foreign)* 키 제약을 위한 특별 문법도 제공한다. (이 문법은 사실상 기저의 대수 또는 해석 식에 대한 축약 표기일 뿐이다.)

먼저 표준 (키) 제약을 본다:

```
candidate key Property = (address)
candidate key Offer = (address, offerDate,
                       bidderName, bidderAddress)
candidate key Decision = (address, offerDate,
                          bidderName, bidderAddress)
candidate key Room = (address, roomName)
candidate key Floor = (address, roomName)
candidate key Commision = (priceBand, areaCode, saleSpeed)

foreign key Offer (address) in Property
foreign key Decision (address, offerDate,
                      bidderName, bidderAddress) in Offer
foreign key Room (address) in Property
foreign key Floor (address) in Property
```

약간 더 흥미로운, 도메인 특화 제약들도 있다. 첫 번째는 모든 매물이 최소 한 개의 방을 가져야 한다는 것을 강제한다:

```
count(restrict(PropertyInfo | numberOfRooms < 1)) == 0
```

다음은 사람들이 자기 소유 매물에 입찰할 수 없음을 보장한다(소유자는 자신이 파는 매물에 거주한다고 가정한다):

```
count(restrict(Offer | bidderAddress == address)) == 0
```

이 제약은 판매가 일어난 뒤(즉 그 *address*에 대해 Acceptance가 발생한 뒤) 해당 매물(*address*)에 어떤 Offer도 제출되는 것을 금지한다:

```
count(restrict(join(Offer,
                    project(Acceptance, address decisionDate))
               | offerDate > decisionDate)) == 0
```

다음 제약은 웹사이트에 광고되는 `PREMIUM` 가격대 매물이 결코 50개를 넘지 않도록 보장한다:

```
count(restrict(extend(PropertyForWebSite,
                      (priceBand = priceBandForPrice(price)))
               | priceBand == PREMIUM)) < 50
```

이것은 흥미로운 제약인데, (마침 직접적으로) 사용자 정의 함수(`priceBandForPrice`)에 의존하기 때문이다. 이것의 한 함의는, 함수 정의의 변경이(*본질적 상태*의 변경과 마찬가지로) — 견제되지 않는다면 — 시스템이 제약을 위반하게 만들 수 있다는 것이다. 어떤 FRP 인프라도 이를 허용할 수 없다.

다행히 이를 해결하는 직접적인 접근이 두 가지 있다. 첫째는 인프라가 함수 정의를 데이터(본질적 상태)로 취급하고 같은 종류의 수정 검사를 적용하는 것이다. 대안은, 기존 데이터가 유효하지 않게 되는 새 함수 버전으로는 시스템 실행을 거부하는 것이다. 후자의 경우 무결성을 복원하고 시스템을 다시 운영 가능하게 만들려면 수동 상태 변경이 필요할 것이다.

마지막으로, 어떤 단일 제안자도 하나의 Property에 (기간 전체에 걸쳐) 10개를 넘는 제안을 제출할 수 없다. 이 제약은 각 제안자(*bidderName*, *bidderAddress*)가 각 Property(*address*)에 한 제안의 수를 먼저 계산한 뒤, 그것이 결코 10을 넘지 않음을 보장하는 방식으로 작동한다:

```
count(restrict(summarize(Offer,
                         project(Offer, address bidderName
                                        bidderAddress),
                         numberOfOffers = count())
               | numberOfOffers > 10)) == 0
```

시스템이 배포되면 FRP 인프라는 이 무결성 제약들 중 어느 하나라도 위반하게 될 상태 수정 시도를 전부 거부할 것이다.

### 10.4 우연적 상태와 제어

FRP 시스템의 *우연적 상태와 제어* 컴포넌트는 오로지 인프라를 위한 성능 힌트를 표현하는 선언들의 집합으로만 이루어진다(9.1.3절 참조). 이 예제에서 *우연적 상태와 제어*는 세 개의 힌트 선언 집합이다.

```
declare store PropertyInfo
```

이 선언은 PropertyInfo 파생 관계를 계속 재계산하는 대신 실제로 저장(즉 캐시)하라고 인프라에 요청하는 힌트일 뿐이다.

```
declare store shared Room Floor
```

이 힌트는 Room과 Floor 관계를 하나의 공유 저장 구조로 *비정규화*하라고 인프라에 지시한다. (이를 *우연적 상태와 제어*의 일부로 표현할 수 있는 덕분에, 여전히 Room과 Floor를 별개로 취급하는 시스템의 *본질적* 부분을 훼손하도록 강제되지 않았다는 점에 유의하라.)

```
declare store separate Property (photo)
```

이 힌트는 Property 관계의 *photo* 속성을 (자주 쓰이지 않으므로) 다른 속성들과 분리해 저장하라고 인프라에 지시한다.

이 세 힌트는 모두 *상태*에 집중했다(PropertyInfo는 *우연적 상태*이고, 나머지 두 선언은 상태의 *우연적 측면*에 관한 것이다). 더 큰 시스템이라면 성능상의 이유로 *우연적 제어* 명세도 아마 포함할 것이다.

### 10.5 기타

이 시스템의 *feeder*와 *observer*는 꽤 단순할 것이다 — 사용자 입력을 Decision, Offer 등으로 *feeding*하고, 다양한 파생 관계(예: OpenOffers, PropertyForWebSite, CommissionDue)를 출력으로 직접 *observing*하여 표시한다.

이 때문에 이 *feeder*와 *observer*는 커스텀 코딩이 전혀 필요 없고, 완전히 선언적인 방식으로 명세될 수 있으리라 기대하는 것이 합리적이다.

커스텀 *observer*가 필요할 수 있는 확장 하나는 CommissionDue를 외부 급여 시스템에 연결해야 한다는 요구사항일 것이다.

## 11 관련 연구

FRP는 [DD00]의 아이디어들로부터 어느 정도 영향을 받았다. 그러나 그 작업과 대조적으로 FRP는 범용의 대규모 *응용* 프로그래밍을 겨냥한다. 추가로 FRP는 *분리된*, *함수형* 하위 언어에 집중하며, 타입 사용에 관해 다른 생각을 갖고 있다. 마지막으로 FRP의 *우연적* 컴포넌트는 전통적 DBMS의 물리/논리 매핑보다 더 넓은 범위를 갖는다.

Backus의 Applicative State Transition 시스템 [Bac78], 그리고 관계 대수의 범용 응용을 연구한 McGill 대학의 Aldat 프로젝트 [Mer85]와도 어느 정도 유사성이 있다.

## 12 결론

우리는 *복잡성*이 대규모 소프트웨어 시스템에서 다른 무엇보다 많은 문제를 일으킨다고 논증했다. 또한 그것이 길들여질 *수 있다*고 논증했다 — 단, 가능한 곳에서는 그것을 *회피*하고 그렇지 않은 곳에서는 *분리*하려는 일치된 노력을 통해서만. 구체적으로, 시스템을 세 개의 주요 부분 — *본질적 상태*, *본질적 로직*, *우연적 상태와 제어* — 으로 유용하게 *분리*할 수 있다고 논증했다.

우리는 이 원리들을 취해 시스템 설계의 최상위 수준에 적용하는 것 — 사실상 서로 다른 컴포넌트에 서로 다른 특화된 *언어*들을 쓰는 것 — 이, 어떤 단일 범용 언어(명령형이든 논리형이든 함수형이든)의 비구조적 채택보다 단순함의 측면에서 더 많은 것을 줄 수 있다고 믿는다. 이 논증을 펴면서 우리는 흔한 프로그래밍 패러다임 각각을 짧게 개관했고, 명령형 접근의 특수한 예로서 객체지향의 약점에 어느 정도 주의를 기울였다.

이 *분리*를 직접 적용할 수 없는 경우(기존의 대규모 시스템 같은)에는 상태를 회피하고, 가능한 곳에서는 명시적 제어를 회피하며, 무슨 수를 써서라도 *코드를 없애려고* 애쓰는 데 집중해야 한다고 믿는다.

그렇다면, 타르 웅덩이에서 벗어나는 길은 무엇인가? 은탄환은 무엇인가? …그것은 FRP가 아닐지도 모른다. 그러나 그것이 *단순함*이라는 데에는 의심의 여지가 없다고 우리는 믿는다.

## 참고 문헌

- [Bac78] John W. Backus. Can programming be liberated from the von Neumann style? a functional style and its algebra of programs. *Commun. ACM*, 21(8):613–641, 1978.
- [Bak93] Henry G. Baker. Equal rights for functional objects or, the more things change, the more they are the same. *Journal of Object-Oriented Programming*, 4(4):2–27, October 1993.
- [Boo91] G. Booch. *Object Oriented Design with Applications*. Benjamin/Cummings, 1991.
- [Bro86] Frederick P. Brooks, Jr. No silver bullet: Essence and accidents of software engineering. *Information Processing 1986, Proceedings of the Tenth World Computing Conference*, H.-J. Kugler, ed.: 1069–76. Reprinted in IEEE Computer, 20(4):10-19, April 1987, and in Brooks, The Mythical Man-Month: Essays on Software Engineering, Anniversary Edition, Chapter 16, Addison-Wesley, 1995.
- [Che76] P. P. Chen. "The Entity-Relationship Model". *ACM Trans. on Database Systems (TODS)*, 1:9–36, 1976.
- [Cod70] E. F. Codd. A relational model of data for large shared data banks. *Comm. ACM*, 13(6):377–387, June 1970.
- [Cod79] E. F. Codd. Extending the database relational model to capture more meaning. *ACM Trans. on Database Sys.*, 4(4):397, December 1979.
- [Cod90] E. F. Codd. *The Relational Model for Database Management, Version 2*. Addison-Wesley, 1990.
- [Cor91] Fernando J. Corbató. On building systems that will fail. *Commun. ACM*, 34(9):72–81, 1991.
- [Dat04] C. J. Date. *An Introduction to Database Systems*. Addison Wesley, 8th edition, 2004.
- [DD00] Hugh Darwen and C. J. Date. *Foundation for Future Database Systems: The Third Manifesto*. Addison-Wesley, 2nd edition, 2000.
- [Dij71] Edsger W. Dijkstra. On the reliability of programs. circulated privately, 1971.
- [Dij72] Edsger W. Dijkstra. The humble programmer. *Commun. ACM*, 15(10):859–866, 1972.
- [Dij97] Dijkstra. The tide, not the waves. In Peter J. Denning and Robert M. Metcalfe, editors, *Beyond Calculation: The Next Fifty Years of Computing, Copernicus, 1997*. 1997.
- [Eco04] Managing complexity. *The Economist*, 373(8403):89–91, 2004.
- [EH97] Conal Elliott and Paul Hudak. Functional reactive animation. In *Proceedings of the ACM SIGPLAN International Conference on Functional Programming (ICFP-97)*, volume 32,8 of *ACM SIGPLAN Notices*, pages 263–273, New York, June 9–11 1997. ACM Press.
- [HJ89] I. Hayes and C. Jones. Specifications are not (necessarily) executable. *IEE Software Engineering Journal*, 4(6):330–338, November 1989.
- [Hoa81] C. A. R. Hoare. The emperor's old clothes. *Commun. ACM*, 24(2):75–83, 1981.
- [Kow79] Robert A. Kowalski. Algorithm = logic + control. *Commun. ACM*, 22(7):424–436, 1979.
- [Mer85] T. H. Merrett. Persistence and Aldat. In *Data Types and Persistence (Appin)*, pages 173–188, 1985.
- [NR69] P. Naur and B. Randell. Software engineering report of a conference sponsored by the NATO science committee Garmisch Germany 7th-11th October 1968, January 01 1969.
- [OB88] A. Ohori and P. Buneman. Type inference in a database programming language. In *Proceedings of the 1988 ACM Conference on LISP and Functional Programming, Snowbird, UT*, pages 174–183, New York, NY, 1988. ACM.
- [O'K90] Richard A. O'Keefe. *The Craft of Prolog*. The MIT Press, Cambridge, 1990.
- [PJ+03] Simon Peyton Jones et al., editors. *Haskell 98 Language and Libraries, the Revised Report*. CUP, April 2003.
- [SS94] Leon Sterling and Ehud Y. Shapiro. *The Art of Prolog - Advanced Programming Techniques, 2nd Ed*. MIT Press, 1994.
- [SU96] Randall B. Smith and David Ungar. A simple and unifying approach to subjective objects. *TAPOS*, 2(3):161–178, 1996.
- [vRH04] Peter van Roy and Seif Haridi. *Concepts, Techniques, and Models of Computer Programming*. MIT Press, 2004.
- [Wad95] Philip Wadler. Monads for functional programming. In *Advanced Functional Programming*, pages 24–52, 1995.
- [Won00] Limsoon Wong. Kleisli, a functional query system. *J. Funct. Program*, 10(1):19–56, 2000.
