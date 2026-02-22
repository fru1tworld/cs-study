# jOOQ 3.12 릴리스 - 새로운 절차적 언어 API

> 원문: https://blog.jooq.org/jooq-3-12-released-with-a-new-procedural-language-api/

jOOQ 3.12가 새로운 절차적 언어 API, 새로운 데이터 타입, MemSQL 지원, 공식 Java 11+ 지원, 크게 개선된 파서, 그리고 리액티브 스트림 API 지원과 함께 릴리스되었습니다.

이번 릴리스에서는 전반적인 품질 향상을 위한 사소한 인프라 작업에 집중했습니다. 팀은 자동화된 통합 테스트를 재작업하여, 발견되지 않았던 수많은 이슈들을 수정하고 26개 지원 RDBMS 방언 전반에 걸쳐 커버리지를 개선했습니다.

## 절차적 언어

jOOQ 3.11의 익명 블록 지원에 이어, 3.12 Professional과 Enterprise 버전은 이제 절차적 언어 기능을 포함합니다:

* 변수 선언
* 변수 할당
* 루프 (WHILE, REPEAT, FOR, LOOP)
* If Then Else
* 레이블
* Exit, Continue, Goto
* Execute

이는 저장 프로시저, 트리거, 그리고 애드혹 절차적 로직의 정의와 변환을 포함한 고급 벤더별 기능을 지원합니다.

## 새로 지원되는 데이터베이스

Professional Edition은 이제 MemSQL 방언을 지원합니다. MemSQL은 MySQL에서 파생되었지만, 구문적 정확성을 위해 공식 지원이 필요한 수많은 차이점이 있습니다.

## 리액티브 스트림

리액티브 프로그래밍 모델은 공통 SPI인 리액티브 스트림을 사용하는 Reactor와 같은 API와 함께 주목받고 있으며, JDK 9부터는 java.util.concurrent.Flow SPI가 있습니다. jOOQ 3.12는 더 쉬운 통합을 위해 API 수준에서 이러한 패러다임을 구현하지만, 구현은 여전히 JDBC(블로킹)에 바인딩되어 있습니다. 향후 버전에서는 ADBA나 R2DBC 지원을 위해 JDBC를 추상화할 예정입니다.

## 새로운 데이터 타입

* JSON / JSONB: 텍스트 및 바이너리 JSON 데이터를 위한 네이티브 문자열 래퍼
* INSTANT: SQL 표준 TIMESTAMP WITH TIME ZONE을 처리합니다; PostgreSQL은 이를 java.time.Instant처럼 유닉스 타임스탬프로 해석합니다
* ROWID: 성능이 좋은 벤더별 쿼리를 위한 네이티브 행 식별 값

## 파서

파서 개선은 다음을 통한 사용자 피드백에서 계속됩니다:

* DDL 스크립트에서 코드 생성을 위한 DDLDatabase
* SQL 방언 변환을 위한 jOOQ.org translate 웹사이트

구체적인 개선 사항은 다음과 같습니다:

* 더 나은 SQL 기능 에뮬레이션을 위한 스키마 메타 정보 접근
* PostgreSQL의 search_path와 유사한 파스 검색 경로
* DDL 시뮬레이션이 코어 라이브러리로 이동
* jOOQ 파서에서만 조각을 무시하기 위한 특수 주석 구문
* ParserCLI의 대화형 모드
* 중첩 블록 주석 지원

## 공식 Java 11 지원

jOOQ 3.12는 전이적 JAXB 의존성을 완전히 제거하여 Java 11을 완벽하게 지원합니다. 상업용 에디션은 최적의 새 API를 사용하는 Java 11+ 배포판과 함께 제공됩니다. 모든 에디션에는 Java 8 이상을 지원하는 Java 8+ 배포판이 포함됩니다.

## 상업용 에디션

jOOQ 3.12부터, 고급 기능은 상업용 배포판에서만 제공됩니다:

* Professional 및 Enterprise Edition의 절차적 언어 API
* Professional Edition의 Java 11+ 배포판
* Enterprise Edition에서 계속되는 Java 6 및 7 지원
* 런타임 라이브러리에서 Professional 및 Enterprise Edition 전용으로 예약된 레거시 RDBMS 방언 버전 지원 (코드 생성기는 영향 없음)

이 전략은 무료 오픈 소스 에디션을 유지하면서 유료 고객에게 레거시 지원과 고급 기능을 제공합니다.

## H2 및 SQLite 통합

두 데이터베이스 모두 크게 개선되었습니다. H2는 jOOQ와의 긴밀한 협력으로 빠르게 발전하며 SQL 표준에 대한 통찰을 제공하고, H2는 jOOQ 구현에 도움을 줍니다.

## 기타 개선 사항

전체 변경 사항은 jooq.org/notes에서 확인할 수 있습니다. 주목할 만한 개선 사항은 다음과 같습니다:

* 새로운 SQL 술어: UNIQUE, SIMILAR TO, 합성 LIKE ANY
* JAXB를 간소화된 커스텀 구현으로 대체
* 히스토릭 log4j (1.x) 의존성 제거; 이제 선택적 slf4j 또는 java.util.logging 사용
* 셰이딩된 jOOR 의존성이 0.9.12로 업그레이드
* jOOQ-checker를 위한 개선된 @Support 어노테이션 사용
* jOOQ-checker가 ErrorProne 및 checker 프레임워크와 함께 실행
* 수많은 새로운 DDL 문 및 절 지원
* 합성 PRODUCT() 집계 및 윈도우 함수
* 윈도우 함수 GROUPS 모드 지원
* CSV, JSON, XML 포맷팅이 이제 중첩 포맷팅 지원
* UPDATE/DELETE 문이 ORDER BY 및 LIMIT 지원 (및 에뮬레이션)
* SQL 문을 통한 동적 코드 생성 구성
* Configuration UnwrapperProvider SPI
* MockFileDatabase가 정규 표현식 및 업데이트 처리
* 설정에서 이름 케이스와 인용 구성 분리
* MySQL DDL 문자 집합 지원
* 간단한 파생 테이블을 위한 Table.where() API
* 열 제거를 위한 Asterisk.except() 및 QualifiedAsterisk.except()
* WEEK를 포함한 벤더별 DateParts

전체 릴리스 노트: jooq.org/notes
