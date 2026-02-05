# SEQUENCE CACHE 크기 설정을 잊지 마라

> 원문: https://blog.jooq.org/dont-forget-to-set-the-sequence-cache-size/

대부분의 경우, 모든 기본값을 사용하여 Oracle `SEQUENCE`를 생성하는 것만으로도 충분합니다:

```sql
CREATE SEQUENCE my_sequence;
```

이 시퀀스는 테이블에 새 레코드를 삽입할 때 트리거에서 즉시 사용할 수 있습니다:

```sql
CREATE OR REPLACE TRIGGER my_trigger
  BEFORE INSERT
  ON my_table
  FOR EACH ROW
  -- 선택적으로 이 트리거가 정말 필요할 때만
  -- 실행되도록 제한
  WHEN (new.id is null)
BEGIN
  SELECT my_sequence.nextval
  INTO   :new.id
  FROM   DUAL;
END my_trigger;
```

## 핵심 권장 사항

그러나 테이블에서 하루에 수백만 건의 삽입이 발생하는 높은 처리량을 경험한다면(예: 로그 테이블), 시퀀스 캐시를 적절하게 구성해야 합니다. Oracle 문서에 따르면: "Oracle Real Application Clusters 환경에서 시퀀스를 사용하는 경우, Oracle은 성능 향상을 위해 CACHE 설정 사용을 권장합니다."

## 성능 고려 사항

저자는 다른 높은 처리량 시나리오에서도 시퀀스 캐싱을 고려할 것을 권장합니다. 시퀀스 값은 자율 트랜잭션(autonomous transaction)에서 생성됩니다. 기본적으로 Oracle은 별도의 트랜잭션에서 새 값을 생성하기 전에 20개의 값을 캐시합니다. 삽입이 많은 시나리오에서는 이로 인해 시퀀스에 상당한 I/O 오버헤드가 발생합니다. 권장되는 접근 방식은 벤치마크를 실행하여 특정 높은 처리량 요구 사항에 맞는 최적의 캐시 값을 찾는 것입니다.

## 추가 논의

댓글 섹션에는 Hibernate의 ID 생성기와 PostgreSQL의 시퀀스에 대한 유사한 CACHE 절 기능에 대한 논의가 포함되어 있습니다.
