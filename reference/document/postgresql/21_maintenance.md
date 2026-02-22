# 제24장. 정기적인 데이터베이스 유지보수 작업 (Routine Database Maintenance Tasks)

> PostgreSQL 18 공식 문서 번역
>
> 원문: https://www.postgresql.org/docs/current/maintenance.html

PostgreSQL 데이터베이스는 최적의 성능을 유지하기 위해 정기적으로 특정 유지보수 작업을 수행해야 합니다. 여기서 논의되는 작업들은 필수적 이지만 반복적인 성격을 가지므로 cron 스크립트나 Windows의 작업 스케줄러와 같은 표준 도구를 사용하여 쉽게 자동화할 수 있습니다. 적절한 스크립트를 설정하고 성공적으로 실행되는지 확인하는 것은 데이터베이스 관리자의 책임입니다.

명백한 유지보수 작업 중 하나는 정기적인 백업 복사본을 만드는 것입니다. 최신 백업이 없으면 재해(디스크 장애, 화재, 실수로 인한 테이블 삭제 등) 후 복구할 수 없습니다. PostgreSQL에서 사용 가능한 백업 및 복구 메커니즘은 Chapter 25에서 자세히 논의됩니다.

다른 주요 유지보수 작업 범주는 정기적인 데이터베이스 "vacuuming"입니다. 이 활동은 Section 24.1에서 논의됩니다. 밀접하게 관련된 것으로 쿼리 플래너가 사용하는 통계를 업데이트하는 것이 있으며, Section 24.1.3에서 다룹니다.

정기적인 관리가 필요할 수 있는 또 다른 작업은 로그 파일 관리입니다. 이는 Section 24.3에서 논의됩니다.

[check_postgres](https://bucardo.org/check_postgres/)는 데이터베이스 상태를 모니터링하고 비정상적인 상황을 보고하는 데 사용할 수 있습니다. check_postgres는 Nagios 및 MRTG와 통합되지만 독립적으로도 실행할 수 있습니다.

PostgreSQL은 다른 데이터베이스 관리 시스템에 비해 유지보수가 적게 필요합니다. 그럼에도 불구하고 이러한 작업들에 적절한 관심을 기울이면 시스템과 함께 즐겁고 생산적인 경험을 보장하는 데 큰 도움이 됩니다.

목차
- [24.1. 정기적인 Vacuuming](#241-정기적인-vacuuming)
  - [24.1.1. Vacuuming 기초](#2411-vacuuming-기초)
  - [24.1.2. 디스크 공간 회수](#2412-디스크-공간-회수)
  - [24.1.3. 플래너 통계 업데이트](#2413-플래너-통계-업데이트)
  - [24.1.4. 가시성 맵 업데이트](#2414-가시성-맵-업데이트)
  - [24.1.5. 트랜잭션 ID Wraparound 장애 방지](#2415-트랜잭션-id-wraparound-장애-방지)
  - [24.1.6. Autovacuum 데몬](#2416-autovacuum-데몬)
- [24.2. 정기적인 재인덱싱](#242-정기적인-재인덱싱)
- [24.3. 로그 파일 유지보수](#243-로그-파일-유지보수)

---

## 24.1. 정기적인 Vacuuming

PostgreSQL 데이터베이스는 vacuuming 이라고 알려진 정기적인 유지보수가 필요합니다. 대부분의 설치에서는 autovacuum 데몬 (Section 24.1.6에서 설명)이 vacuuming을 수행하도록 하는 것으로 충분합니다. 상황에 따라 최적의 결과를 얻기 위해 autovacuum 매개변수를 조정해야 할 수도 있습니다. 일부 데이터베이스 관리자는 cron이나 작업 스케줄러 스크립트를 통해 일정에 따라 실행되는 수동으로 관리되는 `VACUUM` 명령으로 데몬의 활동을 보완하거나 대체하기를 원할 것입니다.

---

### 24.1.1. Vacuuming 기초

PostgreSQL의 `VACUUM` 명령은 여러 가지 이유로 각 테이블을 정기적으로 처리해야 합니다:

1. 업데이트되거나 삭제된 행이 차지하는 디스크 공간을 회수하거나 재사용하기 위해
2. PostgreSQL 쿼리 플래너가 사용하는 데이터 통계를 업데이트하기 위해
3. 인덱스 전용 스캔(index-only scan)의 속도를 높이는 가시성 맵을 업데이트하기 위해
4. 트랜잭션 ID wraparound 또는 multixact ID wraparound 로 인한 매우 오래된 데이터의 손실을 방지하기 위해

VACUUM 변형:

`VACUUM`에는 표준 `VACUUM`과 `VACUUM FULL`의 두 가지 변형이 있습니다.

- `VACUUM FULL`: 더 많은 디스크 공간을 회수할 수 있지만 훨씬 더 느리게 실행됩니다. 테이블에 대한 `ACCESS EXCLUSIVE` 잠금을 요구하여 병렬 작업을 방지합니다.
- 표준 `VACUUM`: 프로덕션 데이터베이스 작업(SELECT, INSERT, UPDATE, DELETE가 정상적으로 계속됨)과 병렬로 실행할 수 있습니다. 표준 vacuuming 중에는 `ALTER TABLE`로 테이블 정의를 수정할 수 없습니다.

성능 고려사항:

`VACUUM`은 상당한 I/O 트래픽을 생성하며, 이는 다른 활성 세션의 성능 저하를 유발할 수 있습니다. 백그라운드 vacuuming의 성능 영향을 줄이기 위해 구성 매개변수를 조정할 수 있습니다(Section 19.10.2 참조).

---

### 24.1.2. 디스크 공간 회수

PostgreSQL에서 행의 `UPDATE` 또는 `DELETE`는 이전 버전의 행을 즉시 제거하지 않습니다. 이 접근 방식은 다중 버전 동시성 제어(MVCC) 의 이점을 얻기 위해 필요합니다: 행 버전은 다른 트랜잭션에 여전히 잠재적으로 보일 수 있는 동안에는 삭제되어서는 안 됩니다. 결국 오래되거나 삭제된 행 버전은 더 이상 어떤 트랜잭션에도 관심이 없게 되며, 공간을 회수해야 합니다.

표준 VACUUM vs. VACUUM FULL:

- 표준 `VACUUM`: 테이블과 인덱스에서 죽은 행 버전을 제거하고 향후 재사용을 위해 공간을 사용 가능으로 표시합니다. 테이블 끝의 하나 이상의 페이지가 완전히 비어 있고 독점적인 테이블 잠금을 쉽게 얻을 수 있는 경우를 제외하고는 운영 체제에 공간을 반환하지 않습니다.
- `VACUUM FULL`: 죽은 공간 없이 완전히 새 버전의 테이블 파일을 작성하여 테이블을 적극적으로 압축합니다. 테이블 크기를 최소화하지만 오랜 시간이 걸리고 작업이 완료될 때까지 새 복사본을 위한 추가 디스크 공간이 필요합니다.

모범 사례:

정기적인 vacuuming의 일반적인 목표는 `VACUUM FULL`이 필요하지 않도록 표준 `VACUUM`을 충분히 자주 수행하는 것입니다. autovacuum 데몬은 이런 방식으로 작동하려고 하며 `VACUUM FULL`을 발행하지 않습니다. 테이블을 최소 크기로 유지하는 것이 아니라 디스크 공간의 안정적인 상태 사용을 유지하는 것이 아이디어입니다: 각 테이블은 최소 크기에 vacuum 실행 사이에 사용되는 공간을 더한 만큼의 공간을 차지합니다.

`VACUUM FULL`이 테이블을 최소 크기로 줄이고 디스크 공간을 운영 체제에 반환할 수 있지만, 테이블이 미래에 다시 커질 경우 별 의미가 없습니다. 적당히 자주 수행되는 표준 `VACUUM`이 많이 업데이트되는 테이블을 유지하는 데 더 좋은 접근 방식입니다.

스케줄링 접근 방식:

일부 관리자는 자체적으로 vacuuming을 스케줄링하는 것을 선호합니다(예: 부하가 낮은 야간에). 문제는 테이블이 업데이트 활동의 예상치 못한 급증을 경험하면 `VACUUM FULL`이 필요한 블로팅이 발생할 수 있다는 것입니다. autovacuum 데몬을 사용하면 업데이트 활동에 따라 동적으로 vacuuming을 스케줄링하여 이 문제를 완화합니다. 워크로드가 극도로 예측 가능하지 않는 한 데몬을 완전히 비활성화하는 것은 현명하지 않습니다.

autovacuum을 사용하지 않는 설치의 경우 일반적인 접근 방식은 다음과 같습니다:
- 사용량이 낮은 시간에 하루에 한 번 데이터베이스 전체 `VACUUM` 스케줄링
- 필요에 따라 많이 업데이트되는 테이블에 대해 더 자주 vacuuming으로 보완
- 업데이트 빈도가 매우 높은 일부 설치에서는 바쁜 테이블을 몇 분마다 vacuum
- 클러스터의 여러 데이터베이스에 `vacuumdb` 프로그램 사용

> 팁: 대안 명령
>
> 테이블에 대규모 업데이트 또는 삭제 활동으로 인한 많은 수의 죽은 행 버전이 포함된 경우 `VACUUM FULL`, `CLUSTER` 또는 `ALTER TABLE`의 테이블 재작성 변형이 필요할 수 있습니다. 이러한 명령은 테이블의 완전히 새로운 복사본을 다시 작성하고 새 인덱스를 빌드합니다. 모두 `ACCESS EXCLUSIVE` 잠금이 필요하고 테이블 크기와 거의 동일한 추가 디스크 공간을 일시적으로 사용합니다.

> 팁: TRUNCATE 대안
>
> 테이블의 전체 내용이 주기적으로 삭제되는 경우 `DELETE` 다음에 `VACUUM`을 사용하는 것보다 `TRUNCATE` 사용을 고려하세요. `TRUNCATE`는 전체 테이블 내용을 즉시 제거하며, 후속 `VACUUM` 또는 `VACUUM FULL`이 필요하지 않습니다. 단점은 엄격한 MVCC 의미론이 위반된다는 것입니다.

---

### 24.1.3. 플래너 통계 업데이트

PostgreSQL 쿼리 플래너는 좋은 쿼리 계획을 생성하기 위해 테이블 내용에 대한 통계 정보에 의존합니다. 이러한 통계는 `ANALYZE` 명령으로 수집되며, 이 명령은 단독으로 또는 `VACUUM`의 선택적 단계로 호출될 수 있습니다. 합리적으로 정확한 통계를 갖는 것이 중요합니다. 그렇지 않으면 잘못된 계획 선택이 데이터베이스 성능을 저하시킬 수 있습니다.

Autovacuum 동작:

autovacuum 데몬이 활성화된 경우 테이블 내용이 충분히 변경될 때마다 `ANALYZE` 명령을 자동으로 발행합니다. 그러나 관리자는 수동으로 스케줄된 `ANALYZE` 작업을 선호할 수 있습니다. 특히 테이블의 업데이트 활동이 "관심 있는" 열의 통계에 영향을 미치지 않는 경우에 그렇습니다. 데몬은 삽입되거나 업데이트된 행 수의 함수로 `ANALYZE`를 엄격하게 스케줄링하며, 그것이 의미 있는 통계적 변화로 이어질지에 대한 지식은 없습니다.

특수 케이스:

파티션과 상속 자식에서 변경된 튜플은 부모 테이블에 대한 분석을 트리거하지 않습니다. 부모 테이블이 비어 있거나 거의 변경되지 않는 경우 autovacuum에 의해 처리되지 않을 수 있으며, 전체 상속 트리에 대한 통계가 수집되지 않습니다. 통계를 최신 상태로 유지하려면 부모 테이블에서 `ANALYZE`를 수동으로 실행해야 합니다.

빈도 고려사항:

자주 통계 업데이트는 거의 업데이트되지 않는 테이블보다 많이 업데이트되는 테이블에 더 유용합니다. 많이 업데이트되는 테이블의 경우에도 데이터의 통계적 분포가 많이 변경되지 않는 경우 통계 업데이트가 필요하지 않을 수 있습니다.

경험 법칙: 테이블 열의 최소값과 최대값이 얼마나 변경되는지 생각해 보세요. 예를 들어:
- 행 업데이트 시간을 포함하는 `timestamp` 열은 행이 추가되고 업데이트됨에 따라 지속적으로 증가하는 최대값을 가질 것입니다. 이러한 열은 아마도 더 자주 통계 업데이트가 필요할 것입니다
- 웹사이트 페이지의 URL을 포함하는 열은 마찬가지로 자주 변경될 수 있지만, 값의 통계적 분포는 상대적으로 천천히 변할 것입니다

유연성:

특정 테이블과 테이블의 특정 열에서만 `ANALYZE`를 실행할 수 있으므로, 일부 통계를 다른 것보다 더 자주 업데이트하는 유연성이 있습니다. 실제로는 일반적으로 전체 데이터베이스를 분석하는 것이 가장 좋습니다. 빠른 작업이기 때문입니다. `ANALYZE`는 모든 행을 읽는 대신 통계적으로 무작위한 테이블 행 샘플링을 사용합니다.

> 팁: 열별 조정
>
> 열별로 `ANALYZE` 빈도를 조정하는 것은 크게 생산적이지 않을 수 있지만, `ANALYZE`가 수집하는 통계의 상세 수준을 열별로 조정하는 것은 가치가 있을 수 있습니다. `WHERE` 절에서 많이 사용되고 매우 불규칙한 데이터 분포를 가진 열은 다른 열보다 더 세밀한 데이터 히스토그램이 필요할 수 있습니다. `ALTER TABLE SET STATISTICS`를 참조하거나 `default_statistics_target` 구성 매개변수를 사용하여 데이터베이스 전체 기본값을 변경하세요.

추가 통계:

기본적으로 함수 선택도에 대한 정보는 제한적입니다. 그러나 함수 호출을 사용하여 통계 객체나 표현식 인덱스를 생성하면 함수에 대한 유용한 통계가 수집되며, 이는 표현식 인덱스를 사용하는 쿼리 계획을 크게 개선할 수 있습니다.

> 팁: 외부 테이블
>
> autovacuum 데몬은 외부 테이블에 대해 `ANALYZE` 명령을 발행하지 않습니다. 적절한 계획을 위해 쿼리가 외부 테이블의 통계를 필요로 하는 경우 적절한 일정에 따라 해당 테이블에서 수동으로 관리되는 `ANALYZE` 명령을 실행하세요.

> 팁: 파티션된 테이블
>
> autovacuum 데몬은 파티션된 테이블에 대해 `ANALYZE` 명령을 발행하지 않습니다. 상속 부모는 부모 자체가 변경된 경우에만 분석됩니다 - 자식 테이블의 변경은 부모 테이블에 대한 자동 분석을 트리거하지 않습니다. 적절한 계획을 위해 쿼리가 부모 테이블의 통계를 필요로 하는 경우 주기적으로 해당 테이블에서 수동 `ANALYZE`를 실행하여 통계를 최신 상태로 유지하세요.

---

### 24.1.4. 가시성 맵 업데이트

Vacuum은 각 테이블에 대해 가시성 맵(visibility map)을 유지하여 모든 활성 트랜잭션(및 페이지가 다시 수정될 때까지 모든 미래 트랜잭션)에게 보이는 것으로 알려진 튜플만 포함하는 페이지를 추적합니다.

두 가지 목적:

1. Vacuum 최적화: Vacuum 자체가 다음 실행에서 정리할 것이 없으므로 그러한 페이지를 건너뛸 수 있습니다.
2. 인덱스 전용 스캔: PostgreSQL이 기본 테이블을 참조하지 않고 인덱스만 사용하여 일부 쿼리에 응답할 수 있게 합니다.

인덱스 전용 스캔:

PostgreSQL 인덱스에는 튜플 가시성 정보가 포함되어 있지 않으므로 일반 인덱스 스캔은 일치하는 각 인덱스 항목에 대해 힙 튜플을 가져와 현재 트랜잭션에서 볼 수 있는지 확인합니다. 반면에 인덱스 전용 스캔 은 먼저 가시성 맵을 확인합니다. 페이지의 모든 튜플이 보이는 것으로 알려진 경우 힙 페치를 건너뛸 수 있습니다. 이것은 가시성 맵이 디스크 접근을 방지할 수 있는 대규모 데이터 세트에서 가장 유용합니다. 가시성 맵은 힙보다 훨씬 작으므로 힙이 매우 큰 경우에도 쉽게 캐시될 수 있습니다.

---

### 24.1.5. 트랜잭션 ID Wraparound 장애 방지

PostgreSQL의 MVCC 트랜잭션 의미론은 트랜잭션 ID(XID) 번호를 비교할 수 있는 것에 의존합니다: 삽입 XID가 현재 트랜잭션의 XID보다 큰 행 버전은 "미래"에 있으며 현재 트랜잭션에 보이지 않아야 합니다. 트랜잭션 ID는 제한된 크기(32비트)를 가지므로 오랫동안 실행되는 클러스터(40억 개 이상의 트랜잭션)는 트랜잭션 ID wraparound 를 겪게 됩니다: XID 카운터가 0으로 래핑되고, 과거에 있던 트랜잭션이 갑자기 미래에 있는 것처럼 보입니다 - 이는 그들의 출력이 보이지 않게 됨을 의미합니다. 이것은 치명적인 데이터 손실을 초래합니다(데이터는 여전히 거기에 있지만 접근할 수 없습니다).

예방:

이를 방지하려면 최소한 20억 트랜잭션마다 모든 데이터베이스의 모든 테이블을 vacuum 해야 합니다.

VACUUM이 문제를 해결하는 방법:

정기적인 vacuuming은 `VACUUM`이 행을 frozen 으로 표시하기 때문에 문제를 해결합니다. 이는 삽입 트랜잭션의 효과가 모든 현재 및 미래 트랜잭션에 확실히 보일 만큼 과거에 커밋된 트랜잭션에 의해 삽입되었음을 나타냅니다.

일반 XID 비교:

일반 XID는 모듈로-2^32 산술을 사용하여 비교됩니다. 이는 모든 일반 XID에 대해 "더 오래된" 20억 개의 XID와 "더 새로운" 20억 개의 XID가 있음을 의미합니다. 일반 XID 공간은 끝점이 없는 원형입니다. 특정 일반 XID로 행 버전이 생성되면 다음 20억 트랜잭션 동안 "과거"에 있는 것처럼 보입니다. 행 버전이 20억 트랜잭션 이상 지속되면 갑자기 미래에 있는 것처럼 보입니다.

FrozenTransactionId:

이를 방지하기 위해 PostgreSQL은 일반 XID 비교 규칙을 따르지 않고 항상 모든 일반 XID보다 오래된 것으로 간주되는 특수 XID인 `FrozenTransactionId`를 예약합니다. Frozen 행 버전은 삽입 XID가 `FrozenTransactionId`인 것처럼 처리되므로 wraparound 문제와 관계없이 모든 일반 트랜잭션에 "과거"에 있는 것처럼 보입니다. 이러한 행 버전은 삭제될 때까지 얼마나 오래 걸리든 유효합니다.

> 참고: Freezing 구현
>
> PostgreSQL 9.4 이전 버전에서 freezing은 실제로 행의 삽입 XID를 `FrozenTransactionId`로 대체하여 구현되었으며, 이는 행의 `xmin` 시스템 열에서 볼 수 있었습니다. 새 버전에서는 플래그 비트만 설정하여 포렌식 사용을 위해 행의 원래 `xmin`을 보존합니다. 그러나 `xmin`이 `FrozenTransactionId`(2)와 같은 행은 9.4 이전 버전에서 pg_upgrade된 데이터베이스에서 여전히 찾을 수 있습니다.
>
> 시스템 카탈로그에도 `xmin`이 `BootstrapTransactionId`(1)와 같은 행이 포함될 수 있으며, 이는 initdb의 첫 번째 단계 중에 삽입되었음을 나타냅니다. `FrozenTransactionId`와 마찬가지로 이 특수 XID는 모든 일반 XID보다 오래된 것으로 처리됩니다.

구성 매개변수:

`vacuum_freeze_min_age`는 해당 XID를 가진 행이 frozen되기 전에 XID 값이 얼마나 오래되어야 하는지를 제어합니다. 이 설정을 증가시키면 곧 다시 수정될 행이 frozen되는 불필요한 작업을 피할 수 있지만, 감소시키면 테이블을 다시 vacuum해야 하기 전에 경과할 수 있는 트랜잭션 수가 증가합니다.

가시성 맵 사용:

`VACUUM`은 가시성 맵을 사용하여 테이블의 어떤 페이지를 스캔해야 하는지 결정합니다. 일반적으로 죽은 행 버전이 없는 페이지는 해당 페이지에 오래된 XID 값을 가진 행 버전이 있더라도 건너뜁니다. 따라서 일반 `VACUUM`은 테이블의 모든 오래된 행 버전을 항상 frozen하지는 않습니다.

공격적인 Vacuum:

일반 `VACUUM`이 모든 오래된 버전을 frozen하지 않을 때 `VACUUM`은 결국 공격적인 vacuum 을 수행해야 하며, 이는 all-visible이지만 all-frozen이 아닌 페이지를 포함하여 모든 적격한 unfrozen XID 및 MXID 값을 frozen합니다.

테이블이 all-visible이지만 all-frozen이 아닌 페이지의 백로그를 쌓고 있는 경우 일반 vacuum은 frozen하려는 시도로 건너뛸 수 있는 페이지를 스캔하도록 선택할 수 있습니다. 이것은 다음 공격적인 vacuum이 스캔해야 하는 페이지 수를 줄입니다. 이를 eager scanning된 페이지라고 합니다. eager scanning은 `vacuum_max_eager_freeze_failure_rate`를 증가시켜 더 많은 all-visible 페이지를 frozen하도록 조정할 수 있습니다. eager scanning이 all-visible이지만 all-frozen이 아닌 페이지 수를 최소로 유지하더라도 대부분의 테이블은 여전히 주기적인 공격적인 vacuuming이 필요합니다. 그러나 eager frozen된 모든 페이지는 공격적인 vacuum 중에 건너뛸 수 있으므로 eager freezing은 공격적인 vacuum의 오버헤드를 최소화할 수 있습니다.

공격적인 Vacuum 타이밍:

`vacuum_freeze_table_age`는 테이블이 공격적으로 vacuum되는 시기를 제어합니다. 마지막 스캔 이후 경과한 트랜잭션 수가 `vacuum_freeze_table_age`에서 `vacuum_freeze_min_age`를 뺀 값보다 큰 경우 all-visible이지만 all-frozen이 아닌 모든 페이지가 스캔됩니다. `vacuum_freeze_table_age`를 0으로 설정하면 `VACUUM`이 항상 공격적인 전략을 사용하도록 강제합니다.

최대 Vacuum되지 않은 시간:

테이블이 vacuum되지 않은 채로 갈 수 있는 최대 시간은 20억 트랜잭션에서 마지막 공격적인 vacuum 시의 `vacuum_freeze_min_age` 값을 뺀 것입니다. 더 오래 vacuum되지 않으면 데이터 손실이 발생할 수 있습니다. 이것이 발생하지 않도록 보장하기 위해 구성 매개변수 `autovacuum_freeze_max_age`에서 지정한 나이보다 오래된 XID를 가진 unfrozen 행이 포함될 수 있는 모든 테이블에서 autovacuum이 호출됩니다(autovacuum이 비활성화된 경우에도 발생합니다).

이것은 테이블이 그 외에 vacuum되지 않으면 대략 `autovacuum_freeze_max_age`에서 `vacuum_freeze_min_age`를 뺀 트랜잭션마다 autovacuum이 호출된다는 것을 의미합니다. 공간 회수 목적으로 정기적으로 vacuum되는 테이블의 경우 이것은 거의 중요하지 않습니다. 그러나 정적 테이블(삽입은 받지만 업데이트나 삭제가 없는 테이블 포함)의 경우 공간 회수를 위한 vacuum이 필요하지 않으므로 매우 큰 정적 테이블에서 강제 autovacuum 간격을 최대화하는 것이 유용할 수 있습니다. `autovacuum_freeze_max_age`를 증가시키거나 `vacuum_freeze_min_age`를 감소시켜 이를 수행할 수 있습니다.

vacuum_freeze_table_age 유효 최대값:

`vacuum_freeze_table_age`의 유효 최대값은 0.95 * `autovacuum_freeze_max_age`입니다. 그보다 높은 설정은 최대값으로 제한됩니다. `autovacuum_freeze_max_age`보다 높은 값은 의미가 없습니다. 그 시점에서 어차피 anti-wraparound autovacuum이 트리거될 것이고, 0.95 배수는 그 전에 수동 `VACUUM`을 실행할 수 있는 여유를 남깁니다.

경험 법칙:

`vacuum_freeze_table_age`는 `autovacuum_freeze_max_age`보다 다소 낮은 값으로 설정하여 정기적으로 예정된 `VACUUM` 또는 일반적인 삭제 및 업데이트 활동에 의해 트리거된 autovacuum이 해당 창에서 실행될 수 있을 만큼 충분한 간격을 남겨야 합니다. 너무 가깝게 설정하면 테이블이 최근에 공간을 회수하기 위해 vacuum되었음에도 anti-wraparound autovacuum이 발생할 수 있고, 더 낮은 값은 더 자주 공격적인 vacuuming으로 이어집니다.

스토리지 영향:

`autovacuum_freeze_max_age`(및 `vacuum_freeze_table_age`와 함께)를 증가시키는 유일한 단점은 데이터베이스 클러스터의 `pg_xact` 및 `pg_commit_ts` 하위 디렉토리가 더 많은 공간을 차지한다는 것입니다. `autovacuum_freeze_max_age` 지평까지 모든 트랜잭션의 커밋 상태와 (`track_commit_timestamp`이 활성화된 경우) 타임스탬프를 저장해야 하기 때문입니다. 커밋 상태는 트랜잭션당 2비트를 사용하므로:
- `autovacuum_freeze_max_age`가 최대 허용값인 20억으로 설정된 경우 `pg_xact`는 약 0.5GB까지, `pg_commit_ts`는 약 20GB까지 커질 것으로 예상됩니다.
- 이것이 총 데이터베이스 크기에 비해 미미하다면 `autovacuum_freeze_max_age`를 최대 허용 값으로 설정하는 것이 좋습니다.
- 그렇지 않으면 `pg_xact` 및 `pg_commit_ts` 스토리지에 허용할 수 있는 것에 따라 설정하세요.
- 기본값인 2억 트랜잭션은 약 50MB의 `pg_xact` 스토리지와 약 2GB의 `pg_commit_ts` 스토리지로 변환됩니다.

vacuum_freeze_min_age 감소의 단점:

`vacuum_freeze_min_age`를 감소시키는 한 가지 단점은 `VACUUM`이 불필요한 작업을 수행하게 할 수 있다는 것입니다: 행이 곧 수정되어 새 XID를 획득하게 될 경우 행 버전을 frozen하는 것은 시간 낭비입니다. 따라서 설정은 행이 더 이상 변경되지 않을 가능성이 있을 때까지 frozen되지 않을 만큼 충분히 커야 합니다.

XID 나이 추적:

데이터베이스에서 가장 오래된 unfrozen XID의 나이를 추적하기 위해 `VACUUM`은 시스템 테이블 `pg_class` 및 `pg_database`에 XID 통계를 저장합니다. 특히:
- 테이블의 `pg_class` 행의 `relfrozenxid` 열은 `relfrozenxid`를 성공적으로 진행시킨 가장 최근 `VACUUM`(일반적으로 가장 최근의 공격적인 VACUUM) 끝에 남아 있는 가장 오래된 unfrozen XID를 포함합니다
- 데이터베이스의 `pg_database` 행의 `datfrozenxid` 열은 해당 데이터베이스에 나타나는 unfrozen XID의 하한입니다 - 이것은 단순히 데이터베이스 내의 테이블별 `relfrozenxid` 값의 최소값입니다

예시 쿼리:

이 정보를 검사하는 편리한 방법은:

```sql
SELECT c.oid::regclass as table_name,
       greatest(age(c.relfrozenxid),age(t.relfrozenxid)) as age
FROM pg_class c
LEFT JOIN pg_class t ON c.reltoastrelid = t.oid
WHERE c.relkind IN ('r', 'm');

SELECT datname, age(datfrozenxid) FROM pg_database;
```

`age` 열은 컷오프 XID에서 현재 트랜잭션의 XID까지의 트랜잭션 수를 측정합니다.

> 팁: VERBOSE 매개변수
>
> `VACUUM` 명령의 `VERBOSE` 매개변수가 지정되면 `VACUUM`은 테이블에 대한 다양한 통계를 출력합니다. 여기에는 `relfrozenxid` 및 `relminmxid`가 어떻게 진행되었는지와 새로 frozen된 페이지 수에 대한 정보가 포함됩니다. autovacuum 로깅(`log_autovacuum_min_duration`으로 제어됨)이 autovacuum이 실행한 `VACUUM` 작업에 대해 보고할 때 동일한 세부 정보가 서버 로그에 나타납니다.

relfrozenxid 진행:

`VACUUM`이 마지막 vacuum 이후 수정된 페이지를 주로 스캔하는 동안 frozen하려는 시도로 all-visible이지만 all-frozen이 아닌 일부 페이지를 eager 스캔할 수도 있습니다. 그러나 `relfrozenxid`는 unfrozen XID를 포함할 수 있는 테이블의 모든 페이지가 스캔될 때만 진행됩니다. 이것은 다음과 같은 경우에 발생합니다:
- `relfrozenxid`가 `vacuum_freeze_table_age` 트랜잭션보다 오래된 경우
- `VACUUM`의 `FREEZE` 옵션이 사용된 경우
- 아직 all-frozen이 아닌 모든 페이지가 죽은 행 버전을 제거하기 위해 vacuuming이 필요한 경우

`VACUUM`이 아직 all-frozen이 아닌 테이블의 모든 페이지를 스캔할 때 `age(relfrozenxid)`를 사용된 `vacuum_freeze_min_age` 설정보다 약간 더 큰 값으로 설정해야 합니다(`VACUUM`이 시작된 이후 시작된 트랜잭션 수만큼 더). `VACUUM`은 `relfrozenxid`를 테이블에 남아 있는 가장 오래된 XID로 설정하므로 최종 값이 엄격하게 필요한 것보다 훨씬 더 최근일 수 있습니다.

`autovacuum_freeze_max_age`에 도달할 때까지 테이블에서 `relfrozenxid`를 진행시키는 `VACUUM`이 발행되지 않으면 곧 테이블에 대해 autovacuum이 강제됩니다.

경고 메시지:

어떤 이유로 autovacuum이 테이블에서 오래된 XID를 지우지 못하면 데이터베이스의 가장 오래된 XID가 wraparound 지점에서 4천만 트랜잭션에 도달할 때 시스템이 다음과 같은 경고 메시지를 내보내기 시작합니다:

```
WARNING:  database "mydb" must be vacuumed within 39985967 transactions
HINT:  To avoid XID assignment failures, execute a database-wide VACUUM in that database.
```

힌트에서 제안한 대로 수동 `VACUUM`이 문제를 해결해야 합니다. 그러나 `VACUUM`은 슈퍼유저가 수행해야 합니다. 그렇지 않으면 시스템 카탈로그를 처리하지 못하여 데이터베이스의 `datfrozenxid`를 진행시킬 수 없습니다.

치명적 오류:

이러한 경고가 무시되면 wraparound까지 3백만 트랜잭션 미만이 남았을 때 시스템은 새 XID 할당을 거부합니다:

```
ERROR:  database is not accepting commands that assign new XIDs to avoid wraparound data loss in database "mydb"
HINT:  Execute a database-wide VACUUM in that database.
```

이 상태에서 이미 진행 중인 모든 트랜잭션은 계속할 수 있지만 읽기 전용 트랜잭션만 시작할 수 있습니다. 데이터베이스 레코드를 수정하거나 관계를 truncate하는 작업은 실패합니다. `VACUUM` 명령은 여전히 정상적으로 실행할 수 있습니다.

> 참고: 이전 릴리스에서 때때로 권장되었던 것과 달리, 정상 작동을 복원하기 위해 postmaster를 중지하거나 단일 사용자 모드로 들어갈 필요가 없거나 바람직하지 않습니다.

복구 단계:

대신 다음 단계를 따르세요:

1. 오래된 준비된 트랜잭션을 해결합니다. `pg_prepared_xacts`에서 `age(transactionid)`가 큰 행을 확인하여 찾을 수 있습니다. 이러한 트랜잭션은 커밋되거나 롤백되어야 합니다.

2. 오래 실행 중인 열린 트랜잭션을 종료합니다. `pg_stat_activity`에서 `age(backend_xid)` 또는 `age(backend_xmin)`이 큰 행을 확인하여 찾을 수 있습니다. 이러한 트랜잭션은 커밋되거나 롤백되어야 하거나 `pg_terminate_backend`를 사용하여 세션을 종료할 수 있습니다.

3. 오래된 복제 슬롯을 삭제합니다. `pg_stat_replication`을 사용하여 `age(xmin)` 또는 `age(catalog_xmin)`이 큰 슬롯을 찾습니다. 대부분의 경우 이러한 슬롯은 더 이상 존재하지 않거나 오랫동안 다운된 서버에 대한 복제를 위해 생성되었습니다. 여전히 존재하고 해당 슬롯에 연결을 시도할 수 있는 서버의 슬롯을 삭제하면 해당 복제본을 다시 빌드해야 할 수 있습니다.

4. 대상 데이터베이스에서 `VACUUM`을 실행합니다. 데이터베이스 전체 `VACUUM`이 가장 간단합니다. 필요한 시간을 줄이려면 `relminxid`가 가장 오래된 테이블에서 수동 `VACUUM` 명령을 발행할 수도 있습니다. 이 시나리오에서 `VACUUM FULL`을 사용하지 마세요. XID가 필요하므로 실패하거나 슈퍼유저 모드에서는 XID를 소비하여 트랜잭션 ID wraparound 위험을 증가시킵니다. 최소한의 작업량 이상을 수행하므로 `VACUUM FREEZE`도 사용하지 마세요.

5. 정상 작동이 복원되면 향후 문제를 피하기 위해 대상 데이터베이스에서 autovacuum이 적절하게 구성되어 있는지 확인하세요.

> 참고: 단일 사용자 모드
>
> 이전 버전에서는 때때로 postmaster를 중지하고 단일 사용자 모드에서 데이터베이스를 `VACUUM`해야 했습니다. 일반적인 시나리오에서는 더 이상 필요하지 않으며 가능하면 피해야 합니다. 시스템을 다운시키는 것을 포함하기 때문입니다. 또한 데이터 손실을 방지하기 위해 설계된 트랜잭션 ID wraparound 보호 장치를 비활성화하므로 더 위험합니다. 이 시나리오에서 단일 사용자 모드를 사용하는 유일한 이유는 필요하지 않은 테이블을 `TRUNCATE`하거나 `DROP`하여 vacuum할 필요가 없도록 하려는 경우입니다. 3백만 트랜잭션 안전 마진은 관리자가 이를 수행할 수 있도록 합니다. 단일 사용자 모드 사용에 대한 자세한 내용은 `postgres` 참조 페이지를 참조하세요.

#### 24.1.5.1. Multixact와 Wraparound

Multixact ID 정의:

Multixact ID 는 여러 트랜잭션에 의한 행 잠금을 지원하는 데 사용됩니다. 튜플 헤더에 잠금 정보를 저장할 공간이 제한되어 있으므로 해당 정보는 행을 동시에 잠그는 트랜잭션이 둘 이상인 경우 "다중 트랜잭션 ID" 또는 줄여서 multixact ID로 인코딩됩니다. 특정 multixact ID에 포함된 트랜잭션 ID에 대한 정보는 `pg_multixact` 하위 디렉토리에 별도로 저장되며, 튜플 헤더의 `xmax` 필드에는 multixact ID만 나타납니다.

스토리지 및 관리:

트랜잭션 ID와 마찬가지로 multixact ID는 32비트 카운터와 해당 스토리지로 구현되며, 모두 신중한 나이 관리, 스토리지 정리 및 wraparound 처리가 필요합니다. 각 multixact의 멤버 목록을 보유하는 별도의 스토리지 영역이 있으며, 이 영역도 32비트 카운터를 사용하고 관리해야 합니다. 시스템 함수 `pg_get_multixact_members()`(Table 9.84에 설명됨)를 사용하여 multixact ID와 연결된 트랜잭션 ID를 검사할 수 있습니다.

Multixact Freezing:

`VACUUM`이 테이블의 일부를 스캔할 때마다 `vacuum_multixact_freeze_min_age`보다 오래된 multixact ID를 다른 값으로 대체합니다. 이 값은 다음 중 하나일 수 있습니다:
- 0 값
- 단일 트랜잭션 ID
- 더 새로운 multixact ID

각 테이블에 대해 `pg_class.relminmxid`는 해당 테이블의 모든 튜플에 여전히 나타날 수 있는 가장 오래된 가능한 multixact ID를 저장합니다. 이 값이 `vacuum_multixact_freeze_table_age`보다 오래되면 공격적인 vacuum이 강제됩니다. 이전 섹션에서 논의한 바와 같이 공격적인 vacuum은 all-frozen으로 알려진 페이지만 건너뜁니다. `mxid_age()`를 `pg_class.relminmxid`에서 사용하여 나이를 찾을 수 있습니다.

공격적인 Vacuum 보장:

공격적인 `VACUUM`은 원인에 관계없이 테이블의 `relminmxid`를 진행시킬 수 있도록 보장됩니다. 결국 모든 데이터베이스의 모든 테이블이 스캔되고 가장 오래된 multixact 값이 진행되면 오래된 multixact에 대한 디스크 스토리지를 제거할 수 있습니다.

Autovacuum 안전 장치:

안전 장치로 multixact-age가 `autovacuum_multixact_freeze_max_age`보다 큰 모든 테이블에 대해 공격적인 vacuum 스캔이 발생합니다. 또한 multixact 멤버가 차지하는 스토리지가 약 10GB를 초과하면 가장 오래된 multixact-age를 가진 테이블부터 시작하여 모든 테이블에 대해 더 자주 공격적인 vacuum 스캔이 발생합니다. 이 두 종류의 공격적인 스캔은 autovacuum이 명목상 비활성화된 경우에도 발생합니다. 멤버 스토리지 영역은 wraparound에 도달하기 전에 약 20GB까지 커질 수 있습니다.

경고 및 오류:

XID의 경우와 마찬가지로 autovacuum이 테이블에서 오래된 MXID를 지우지 못하면 데이터베이스의 가장 오래된 MXID가 wraparound 지점에서 4천만 트랜잭션에 도달할 때 시스템이 경고 메시지를 내보내기 시작합니다. 그리고 XID의 경우와 마찬가지로 이러한 경고가 무시되면 wraparound까지 3백만 미만이 남았을 때 시스템은 새 MXID 생성을 거부합니다.

복원 프로세스:

MXID가 소진되었을 때 정상 작동은 XID가 소진되었을 때와 거의 같은 방식으로 복원할 수 있습니다. 이전 섹션의 동일한 단계를 따르되 다음과 같은 차이점이 있습니다:

1. 실행 중인 트랜잭션 및 준비된 트랜잭션 은 multixact에 나타날 가능성이 없는 경우 무시할 수 있습니다.

2. MXID 정보 는 `pg_stat_activity`와 같은 시스템 뷰에서 직접 볼 수 없습니다. 그러나 오래된 XID를 찾는 것은 여전히 MXID wraparound 문제를 일으키는 트랜잭션을 결정하는 좋은 방법입니다.

3. XID 소진 은 모든 쓰기 트랜잭션을 차단하지만 MXID 소진 은 쓰기 트랜잭션의 하위 집합, 특히 MXID가 필요한 행 잠금을 포함하는 트랜잭션만 차단합니다.

---

### 24.1.6. Autovacuum 데몬

PostgreSQL에는 autovacuum 이라는 선택적이지만 강력히 권장되는 기능이 있으며, 그 목적은 `VACUUM` 및 `ANALYZE` 명령의 실행을 자동화하는 것입니다. 활성화되면 autovacuum은 삽입, 업데이트 또는 삭제된 튜플 수가 많은 테이블을 확인합니다. 이러한 검사는 통계 수집 기능을 사용합니다. 따라서 `track_counts`가 `true`로 설정되지 않으면 autovacuum을 사용할 수 없습니다. 기본 구성에서 autovacuum이 활성화되고 관련 구성 매개변수가 적절하게 설정됩니다.

Autovacuum 프로세스:

"autovacuum 데몬"은 실제로 여러 프로세스로 구성됩니다:

1. Autovacuum launcher: 모든 데이터베이스에 대해 autovacuum worker 프로세스를 시작하는 영구 데몬 프로세스
2. Autovacuum worker: 개별 데이터베이스에 대해 launcher에 의해 시작됨

launcher는 `autovacuum_naptime` 초마다 각 데이터베이스 내에서 하나의 worker를 시작하려고 시간에 걸쳐 작업을 분배합니다. 따라서 설치에 N 개의 데이터베이스가 있으면 `autovacuum_naptime`/N 초마다 새 worker가 시작됩니다.

최대 `autovacuum_max_workers` worker 프로세스가 동시에 실행될 수 있습니다. `autovacuum_max_workers`보다 많은 데이터베이스를 처리해야 하는 경우 첫 번째 worker가 완료되는 즉시 다음 데이터베이스가 처리됩니다.

Worker 처리:

각 worker 프로세스는 데이터베이스 내의 각 테이블을 확인하고 필요에 따라 `VACUUM` 및/또는 `ANALYZE`를 실행합니다. `log_autovacuum_min_duration`을 설정하여 autovacuum worker의 활동을 모니터링할 수 있습니다.

작업 분배:

짧은 시간에 여러 대형 테이블이 모두 vacuuming에 적합해지면 모든 autovacuum worker가 오랜 기간 동안 해당 테이블을 vacuuming하는 데 occupied될 수 있습니다. 이로 인해 worker가 사용 가능해질 때까지 다른 테이블과 데이터베이스가 vacuum되지 않습니다. 단일 데이터베이스에 얼마나 많은 worker가 있을 수 있는지에 대한 제한은 없지만 worker는 다른 worker가 이미 수행한 작업을 반복하지 않으려고 합니다.

연결 제한:

실행 중인 worker 수는 `max_connections` 또는 `superuser_reserved_connections` 제한에 포함되지 않습니다.

Vacuum 결정 로직:

`relfrozenxid` 값이 `autovacuum_freeze_max_age` 트랜잭션보다 오래된 테이블은 항상 vacuum됩니다(스토리지 매개변수를 통해 freeze max age가 수정된 테이블에도 적용됩니다). 그렇지 않으면 마지막 `VACUUM` 이후 obsolete된 튜플 수가 "vacuum 임계값"을 초과하면 테이블이 vacuum됩니다.

Vacuum 임계값 공식:

```
vacuum threshold = Minimum(vacuum max threshold,
                           vacuum base threshold +
                           vacuum scale factor * number of tuples)
```

여기서:
- `vacuum max threshold` = `autovacuum_vacuum_max_threshold`
- `vacuum base threshold` = `autovacuum_vacuum_threshold`
- `vacuum scale factor` = `autovacuum_vacuum_scale_factor`
- `number of tuples` = `pg_class.reltuples`

삽입 임계값:

마지막 vacuum 이후 삽입된 튜플 수가 정의된 삽입 임계값을 초과한 경우에도 테이블이 vacuum됩니다:

```
vacuum insert threshold = vacuum base insert threshold +
                          vacuum insert scale factor * number of tuples
```

여기서:
- `vacuum insert base threshold` = `autovacuum_vacuum_insert_threshold`
- `vacuum insert scale factor` = `autovacuum_vacuum_insert_scale_factor`

이러한 vacuum은 테이블의 일부를 all visible 로 표시하고 튜플을 frozen할 수 있으므로 후속 vacuum에서 필요한 작업을 줄일 수 있습니다. `INSERT` 작업은 받지만 `UPDATE`/`DELETE` 작업이 없거나 거의 없는 테이블의 경우 테이블의 `autovacuum_freeze_min_age`를 낮추면 이전 vacuum에서 튜플을 frozen할 수 있어 유익할 수 있습니다.

튜플 수 추적:

obsolete된 튜플 수와 삽입된 튜플 수는 누적 통계 시스템에서 얻습니다. 이것은 각 `UPDATE`, `DELETE` 및 `INSERT` 작업에 의해 업데이트되는 eventually-consistent 카운트입니다.

공격적인 Vacuum 트리거:

테이블의 `relfrozenxid` 값이 `vacuum_freeze_table_age` 트랜잭션보다 오래되면 오래된 튜플을 frozen하고 `relfrozenxid`를 진행시키기 위해 공격적인 vacuum이 수행됩니다.

Analyze 임계값:

analyze에도 유사한 조건이 사용됩니다. 임계값은 다음과 같이 정의됩니다:

```
analyze threshold = analyze base threshold +
                    analyze scale factor * number of tuples
```

이것은 마지막 `ANALYZE` 이후 삽입, 업데이트 또는 삭제된 총 튜플 수와 비교됩니다.

파티션된 테이블 제한:

파티션된 테이블은 튜플을 직접 저장하지 않으므로 autovacuum에 의해 처리되지 않습니다(autovacuum은 다른 테이블과 마찬가지로 테이블 파티션을 처리합니다). 불행히도 이것은 autovacuum이 파티션된 테이블에서 `ANALYZE`를 실행하지 않음을 의미하며, 이는 파티션된 테이블 통계를 참조하는 쿼리에 대해 최적이 아닌 계획을 유발할 수 있습니다. 파티션된 테이블이 처음 채워질 때와 파티션의 데이터 분포가 크게 변경될 때마다 수동으로 `ANALYZE`를 실행하여 이 문제를 해결할 수 있습니다.

임시 테이블 제한:

임시 테이블은 autovacuum에서 접근할 수 없습니다. 따라서 적절한 vacuum 및 analyze 작업은 세션 SQL 명령을 통해 수행해야 합니다.

테이블별 구성:

기본 임계값 및 스케일 팩터는 `postgresql.conf`에서 가져오지만 테이블별로 재정의할 수 있습니다(및 기타 많은 autovacuum 제어 매개변수). 자세한 내용은 Storage Parameters를 참조하세요. 테이블의 스토리지 매개변수를 통해 설정이 변경된 경우 해당 테이블을 처리할 때 해당 값이 사용됩니다. 그렇지 않으면 전역 설정이 사용됩니다. 전역 설정에 대한 자세한 내용은 Section 19.10.1을 참조하세요.

비용 지연 균형:

여러 worker가 실행 중일 때 autovacuum 비용 지연 매개변수(Section 19.10.2 참조)는 모든 실행 중인 worker 간에 "균형"되므로 실제로 실행 중인 worker 수에 관계없이 시스템에 대한 총 I/O 영향은 동일합니다. 그러나 테이블별 `autovacuum_vacuum_cost_delay` 또는 `autovacuum_vacuum_cost_limit` 스토리지 매개변수가 설정된 테이블을 처리하는 worker는 균형 알고리즘에서 고려되지 않습니다.

잠금 동작:

Autovacuum worker는 일반적으로 다른 명령을 차단하지 않습니다. 프로세스가 autovacuum이 보유한 `SHARE UPDATE EXCLUSIVE` 잠금과 충돌하는 잠금을 획득하려고 하면 잠금 획득이 autovacuum을 중단합니다. 충돌하는 잠금 모드는 Table 13.2를 참조하세요. 그러나 autovacuum이 트랜잭션 ID wraparound를 방지하기 위해 실행 중인 경우(즉, `pg_stat_activity` 뷰의 autovacuum 쿼리 이름이 `(to prevent wraparound)`로 끝나는 경우) autovacuum은 자동으로 중단되지 않습니다.

> 경고:
>
> `SHARE UPDATE EXCLUSIVE` 잠금과 충돌하는 잠금을 획득하는 명령(예: ANALYZE)을 정기적으로 실행하면 autovacuum이 완료되지 않을 수 있습니다.

---

## 24.2. 정기적인 재인덱싱

일부 상황에서는 `REINDEX` 명령이나 일련의 개별 재구축 단계로 인덱스를 주기적으로 재구축하는 것이 가치 있습니다.

### 공간 효율성 문제

B-tree 인덱스:
- 완전히 비어 있는 B-tree 인덱스 페이지는 재사용을 위해 회수됩니다
- 그러나 페이지의 인덱스 키 대부분(전부는 아님)이 삭제된 경우 비효율적인 공간 사용이 여전히 발생할 수 있습니다 - 페이지는 할당된 상태로 유지됩니다
- 각 범위의 대부분(전부는 아님)의 키가 결국 삭제되는 사용 패턴은 공간 활용도가 낮습니다
- 권장사항: 이러한 사용 패턴에는 주기적인 재인덱싱을 권장합니다

비-B-tree 인덱스:
- 비-B-tree 인덱스의 블로트 가능성은 잘 연구되지 않았습니다
- 권장사항: 비-B-tree 인덱스 유형을 사용할 때는 인덱스의 물리적 크기를 주기적으로 모니터링하는 것이 좋습니다

### 성능 고려사항

B-tree 인덱스의 경우:
- 새로 구축된 인덱스는 여러 번 업데이트된 인덱스보다 접근 속도가 약간 빠릅니다
- 새로 구축된 인덱스에서는 논리적으로 인접한 페이지가 일반적으로 물리적으로도 인접하기 때문입니다
- 참고: 이 고려사항은 비-B-tree 인덱스에는 적용되지 않습니다
- 접근 속도를 향상시키기 위해 주기적으로 재인덱싱하는 것이 가치 있을 수 있습니다

### 사용법 및 잠금

`REINDEX`는 모든 경우에 안전하고 쉽게 사용할 수 있습니다:
- 기본적으로 명령은 `ACCESS EXCLUSIVE` 잠금을 요구합니다
- `CONCURRENTLY` 옵션으로 실행하는 것이 종종 바람직하며, 이 경우 `SHARE UPDATE EXCLUSIVE` 잠금만 필요합니다

---

## 24.3. 로그 파일 유지보수

데이터베이스 서버의 로그 출력을 `/dev/null`로 버리는 것보다는 어딘가에 저장하는 것이 좋습니다. 로그 출력은 문제를 진단할 때 매우 유용합니다.

### 보안 고려사항

> 중요: 서버 로그에는 다음과 같은 민감한 정보가 포함될 수 있습니다:
> - DDL 문의 평문 암호
> - 애플리케이션의 SQL 소스 코드
> - ERROR 수준 로그의 데이터 행 일부

로그는 적절하게 권한이 부여된 사람만 볼 수 있도록 보호되어야 합니다.

### 로그 순환 접근 방식

#### 1. 내장 로그 순환 기능 (권장)

`postgresql.conf`에서 구성 매개변수 `logging_collector`를 `true`로 설정합니다:

```
logging_collector = true
```

제어 매개변수는 Section 19.8.1에 설명되어 있습니다. 이 접근 방식은 기계 판독 가능한 CSV 형식 캡처도 지원합니다.

#### 2. 외부 로그 순환 프로그램

stderr 출력을 외부 프로그램으로 파이프합니다. Apache의 `rotatelogs`를 사용한 예:

```bash
pg_ctl start | rotatelogs /var/log/pgsql_log 86400
```

#### 3. logrotate와 결합된 접근 방식

PostgreSQL의 내장 로깅 수집기가 생성한 파일을 수집하도록 `logrotate`를 설정합니다. 로그를 순환할 때 로그 파일을 다시 열도록 `SIGHUP` 신호를 보냅니다:

```bash
pg_ctl logrotate
```

서버는 로깅 구성에 따라 새 로그 파일로 전환하거나 기존 파일을 다시 엽니다.

정적 로그 파일 이름에 대한 참고: 최대 열린 파일 제한에 도달하면 서버가 로그 파일을 다시 열지 못할 수 있습니다. 이를 방지하려면 동적 로그 파일 이름을 구성하고 `prerotate` 스크립트를 사용하여 열린 로그 파일을 무시하세요.

#### 4. Syslog 접근 방식

`postgresql.conf`에서 `log_destination`을 `syslog`로 설정합니다:

```
log_destination = syslog
```

주의사항:
- Syslog는 큰 메시지에서 항상 신뢰할 수 없습니다
- 메시지를 자르거나 삭제할 수 있습니다
- Linux에서는 각 메시지를 디스크에 플러시합니다(성능 저하)
- syslog 구성에서 "`-`" 접두사로 동기화를 비활성화할 수 있습니다

### 로그 파일 정리

위의 모든 솔루션은 구성 가능한 간격으로 새 로그 파일 시작을 처리하지만 이전 파일을 자동으로 삭제하지 않습니다. 다음을 수행해야 합니다:
- 주기적으로 이전 로그 파일을 삭제하는 배치 작업 설정
- 로그 파일을 순환적으로 덮어쓰도록 순환 프로그램 구성

### 추가 도구

- [pgBadger](https://pgbadger.darold.net/) - 정교한 로그 파일 분석
- [check_postgres](https://bucardo.org/check_postgres/) - 중요한 로그 메시지 및 이상 탐지에 대한 Nagios 경고
