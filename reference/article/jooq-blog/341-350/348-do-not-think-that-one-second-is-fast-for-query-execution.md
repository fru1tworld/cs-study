# 쿼리 실행에 1초가 빠르다고 생각하지 마라

> 원문: https://blog.jooq.org/do-not-think-that-one-second-is-fast-for-query-execution/

최근 Stack Overflow에서 SQL Server Management Studio에서는 1초 만에 실행되지만 Hibernate를 통해서는 60초가 걸리는 쿼리에 대한 질문을 발견했다. 해당 질문의 예제 쿼리는 학생 정보를 필터링하는 것이었다:

```sql
select Student_Id
from Student_Table
where Roll_No in ('A101','A102','A103',.....'A250');
```

## 핵심 메시지: 1초가 빠르다고 믿지 마라

1초가 빠르다고 믿지 마라. 데이터베이스 시스템은 속도를 위해 설계되었다. 단순한 쿼리는 평범한 하드웨어에서도 거의 즉시 실행되어야 한다. 대용량 보고서나 배치 작업을 수행하지 않는 한, 2-3밀리초를 초과하는 것은 최적이 아니라고 봐야 한다.

## 근본 원인 분석

원본 테이블 스키마에는 적절한 인덱스가 없었다:

```sql
CREATE TABLE student_table (
       Student_Id BIGINT NOT NULL IDENTITY
     , Class_Id BIGINT NOT NULL
     , Student_First_Name VARCHAR(100) NOT NULL
     , Student_Last_Name VARCHAR(100)
     , Roll_No VARCHAR(100) NOT NULL
     , PRIMARY KEY (Student_Id)
     , CONSTRAINT UK_StudentUnique_1
         UNIQUE  (Class_Id, Roll_No)
);
```

(Class_Id, Roll_No)에 대한 UNIQUE 제약 조건은 Roll_No만으로 필터링할 때는 효과가 없다. 복합 인덱스에서 두 번째 컬럼만으로는 인덱스를 효율적으로 사용할 수 없기 때문이다. 이로 인해 800만 행에 대해 Index Seek가 아닌 Index Scan이 발생했고, 결과적으로 3초의 실행 시간이 소요되었다.

## 해결책

### 기본 인덱스 추가

```sql
CREATE INDEX i_student_roll_no
ON student_table (Roll_No);
```

이렇게 하면 "Index Scan" 대신 "Index Seek" 연산이 수행되어 거의 즉시 실행된다.

### 커버링 인덱스 대안

```sql
CREATE INDEX i_student_roll_no
ON student_table (Roll_No, Student_Id);
```

커버링 인덱스는 쿼리 실행에 필요한 모든 데이터를 인덱스 내에 포함한다. 장점은 테이블 데이터에 접근하지 않고 인덱스만으로 쿼리를 해결할 수 있다는 것이다. 단점은 저장 공간이 더 필요하고, 쿼리 요구사항이 변경되면 적용 범위가 제한된다는 것이다.

## 핵심 원칙

2-3밀리초를 초과하는 것은 빠르다고 생각하지 마라! 일반적인 쿼리의 경우, 대용량 보고서나 배치 작업을 수행하지 않는 한 그렇다.

인덱싱 전문가 Markus Winand의 말에 따르면, "모든 성능 문제의 80%"는 누락된 인덱스를 추가함으로써 해결된다.

## 원래 질문에 대한 추가 설명

원래 질문에는 Hibernate와 직접 SQL 실행 사이의 추가적인 복잡성이 있었지만, 핵심은 이것이다: 일반 SQL이 이미 1초라는 성능 저하를 보인다면, 즉각적인 최적화 기회가 존재한다는 것이다.

## 커뮤니티 의견

한 사용자는 비슷한 기대치가 삽입 작업에도 만연해 있다고 지적했다. 개발자들이 배치 처리를 통해 초당 1000건 이상의 트랜잭션을 추구하는 대신 초당 10건의 삽입을 수용한다는 것이다. 이러한 사고방식은 불필요한 캐싱과 애플리케이션 복잡성을 유발한다.

또 다른 사용자는 데이터베이스별 특성에 대해 언급했다: Oracle의 부등호 연산자("!=")는 인덱스를 효과적으로 활용할 수 없다. 명시적인 동등 조건이나 NOT IN 서브쿼리로 변환하면 인덱스 사용이 가능해진다.

## 결론

복잡한 캐싱 시스템을 구현하기 전에, 먼저 데이터베이스 최적화에 집중해야 한다. 대부분의 단순한 쿼리는 거의 즉시 실행되어야 한다. 1초가 걸린다면, 그것은 빠른 것이 아니다. 무언가 잘못되었다는 신호다.
