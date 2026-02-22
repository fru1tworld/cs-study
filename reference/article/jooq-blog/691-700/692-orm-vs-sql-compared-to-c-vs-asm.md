# ORM vs. SQL, C vs. ASM에 비유
> 원문: https://blog.jooq.org/orm-vs-sql-compared-to-c-vs-asm/

*Lukas Eder, 2012년 4월 22일*

## ORM vs. SQL 논쟁

ORM(Object-Relational Mapping)과 SQL 사이의 논쟁은 오랫동안 개발자 커뮤니티에서 지속되어 왔습니다. 이 논쟁을 이해하기 위해 과거의 유사한 기술 논쟁인 C 언어와 어셈블리 언어 간의 비교를 살펴보겠습니다.

## ORM의 한계

많은 ORM들이 SQL의 고급 기능들을 지원하지 못합니다:

- 윈도우 함수 (Window functions)
- MERGE 문
- 재귀 쿼리 (Recursive queries)
- 공통 테이블 표현식 (Common Table Expressions, CTE)
- 정렬된 집계 함수 (Ordered aggregate functions)
- 통계 함수 (Statistical functions)
- 저장 프로시저 (Stored procedures)

SQL은 단순히 "저수준"인 것이 아닙니다. SQL은 근본적으로 다른 패러다임을 나타냅니다. ORM은 SQL의 관계형 모델을 추상화하지만, 이는 단순히 더 높은 수준의 인터페이스를 제공하는 것이 아닙니다. 오히려 SQL이 가진 관계형 모델 자체를 감추는 방식으로 작동합니다.

## C vs. 어셈블리 비유

이와 유사하게, 초기의 C 컴파일러는 수작업으로 작성된 어셈블리 코드보다 더 나은 최적화를 수행하지 못했습니다. 숙련된 어셈블리 프로그래머들은 다음과 같은 것들을 활용할 수 있었습니다:

- 고급 주소 지정 모드 (Advanced addressing modes)
- 연산 코드 트릭 (Opcode tricks)
- 특수 명령어 (Special instructions)
- 아키텍처별 최적화

두 논쟁 모두 단순한 추상화 계층이 아닌 패러다임 전환을 포함합니다.

## 결론

역사적으로 C 컴파일러는 결국 수작업 어셈블리 작업을 능가할 만큼 충분히 발전했습니다. 이와 마찬가지로, ORM과 SQL 간의 논쟁도 도구와 최적화 기능의 개선을 통해 시간이 지나면서 어떤 접근 방식이 우월한지 밝혀질 것입니다.

핵심은 각 도구의 적절한 사용 맥락을 이해하는 것입니다. ORM은 간단한 CRUD 작업에 편리하지만, 복잡한 쿼리나 성능이 중요한 상황에서는 SQL의 강력한 기능을 직접 활용하는 것이 더 나은 선택일 수 있습니다.
