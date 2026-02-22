# OneToOne 관계에 UNIQUE 제약 조건을 추가하면 성능에 어떻게 도움이 되는가

> 원문: https://blog.jooq.org/how-adding-a-unique-constraint-on-a-onetoone-relationship-helps-performance/

SQL 제약 조건(constraint)은 주로 데이터 무결성을 강제하는 데 사용되지만, 데이터베이스 옵티마이저에게 귀중한 메타데이터를 제공하기도 합니다. UNIQUE 제약 조건은 옵티마이저에게 각 값이 테이블에서 최대 한 번만 나타난다는 것을 알려줍니다.

## 핵심 개념

데이터베이스 옵티마이저는 제약 조건에 대한 지식을 세 가지 수준으로 활용할 수 있습니다:

1. 컬럼의 고유 값에 대해 전혀 알지 못함 (최악의 경우)
2. 통계를 통해 고유 값의 수를 추정함
3. 각 값이 최대 한 번만 나타난다는 형식적 지식 (UNIQUE 제약 조건을 통해)

형식적 지식은 어떤 통계보다 훨씬 강력합니다. 옵티마이저가 이 정보를 확실하게 알면, 해시 조인 대신 중첩 루프 조인과 같은 더 효율적인 실행 계획을 선택할 수 있습니다. 조회 결과가 최대 한 행만 반환한다는 것을 알기 때문입니다.

## Java에서의 비유

이것은 Java의 루프 최적화와 비교할 수 있습니다. 컬렉션에 요소가 하나만 포함되어 있다는 것을 알면, JIT 컴파일러는 반복을 완전히 최적화하여 제거할 수 있습니다. 반면 크기를 알 수 없는 리스트를 반복하는 것과 비교하면 상당한 성능 차이가 발생합니다.

## Oracle 예제 설정

UNIQUE 제약 조건이 없는 일대다 관계와 UNIQUE 제약 조건이 있는 일대일 관계를 비교해 보겠습니다.

### UNIQUE 제약 조건 없이 테이블 생성

```sql
CREATE TABLE x1 (
  a NUMBER(10) NOT NULL,
  data VARCHAR2(100),
  CONSTRAINT pk_x1 PRIMARY KEY (a)
);

CREATE TABLE y1 (
  a NUMBER(10) NOT NULL,
  optional_data VARCHAR2(100),
  CONSTRAINT fk_y1 FOREIGN KEY (a) REFERENCES x1(a)
);

CREATE INDEX i1 ON y1(a);
CREATE INDEX i1data ON x1(data);
```

### 데이터 삽입

```sql
-- x1에 10,000개의 행 삽입
INSERT INTO x1
SELECT level, dbms_random.string('a', 100)
FROM dual
CONNECT BY level <= 10000;

-- y1에 5,000개의 행 삽입
INSERT INTO y1
SELECT level, dbms_random.string('a', 100)
FROM dual
CONNECT BY level <= 5000;
```

### 테스트 쿼리

```sql
SELECT count(*)
FROM x1
JOIN y1 USING (a)
WHERE data LIKE 'a%';
```

### 실행 계획 (UNIQUE 없음)

UNIQUE 제약 조건이 없으면, 옵티마이저는 HASH JOIN을 선택합니다. 이는 필터링된 약 176개의 x1 행을 5,000개의 모든 y1 인덱스 행과 비교하며, 총 비용은 29입니다.

## UNIQUE 제약 조건이 있는 테이블

이제 UNIQUE 제약 조건을 추가한 동일한 스키마를 살펴보겠습니다.

```sql
CREATE TABLE x2 (
  a NUMBER(10) NOT NULL,
  data VARCHAR2(100),
  CONSTRAINT pk_x2 PRIMARY KEY (a)
);

CREATE TABLE y2 (
  a NUMBER(10) NOT NULL,
  optional_data VARCHAR2(100),
  CONSTRAINT uk_y2 UNIQUE (a),  -- UNIQUE 제약 조건 추가
  CONSTRAINT fk_y2 FOREIGN KEY (a) REFERENCES x2(a)
);

CREATE INDEX i2data ON x2(data);
```

### 실행 계획 (UNIQUE 있음)

UNIQUE 제약 조건이 있으면, 옵티마이저는 NESTED LOOPS 조인을 선택합니다. 이는 필터링된 176개의 각 행에 대해 고유 인덱스 조회를 수행하며, 총 비용이 25로 감소합니다.

핵심적인 차이점은 옵티마이저가 이제 조회가 최대 한 행만 반환한다는 것을 확실히 알기 때문에, 추가 행이 없다는 것을 확인하기 위해 계속 검색할 필요가 없다는 것입니다.

## 벤치마크

두 접근 방식의 실제 성능 차이를 측정해 보겠습니다.

```sql
SET SERVEROUTPUT ON
DECLARE
  v_ts TIMESTAMP;
  v_repeat CONSTANT NUMBER := 1000;
BEGIN
  v_ts := SYSTIMESTAMP;

  -- UNIQUE 제약 조건 없이 1000번 반복
  FOR i IN 1..v_repeat LOOP
    FOR rec IN (
      SELECT count(*)
      FROM x1
      JOIN y1 USING (a)
      WHERE data LIKE 'a%'
    ) LOOP
      NULL;
    END LOOP;
  END LOOP;

  dbms_output.put_line('Without UNIQUE constraint: '
    || (SYSTIMESTAMP - v_ts));
  v_ts := SYSTIMESTAMP;

  -- UNIQUE 제약 조건으로 1000번 반복
  FOR i IN 1..v_repeat LOOP
    FOR rec IN (
      SELECT count(*)
      FROM x2
      JOIN y2 USING (a)
      WHERE data LIKE 'a%'
    ) LOOP
      NULL;
    END LOOP;
  END LOOP;

  dbms_output.put_line('With    UNIQUE constraint: '
    || (SYSTIMESTAMP - v_ts));
END;
/
```

### 벤치마크 결과

```
Without UNIQUE constraint: +000000000 00:00:04.250000000
With    UNIQUE constraint: +000000000 00:00:02.847000000
```

결과:
- UNIQUE 제약 조건 없음: 4.250초
- UNIQUE 제약 조건 있음: 2.847초
- 성능 향상: 약 50% 더 빠름

## 결론

데이터베이스에 데이터의 특성을 제약 조건을 통해 명시적으로 전달하는 것을 권장합니다. 형식적 지식은 쿼리 최적화에 있어 어떤 통계보다 훨씬 강력합니다. 데이터 삽입 시 약간의 오버헤드만 추가되지만, SELECT 성능은 크게 향상됩니다.

항상 데이터베이스에 가능한 한 많은 정보를 제공하세요. 일대일 관계가 있다면, UNIQUE 제약 조건으로 이를 명시적으로 선언하여 옵티마이저가 이 정보를 활용할 수 있도록 하세요.
