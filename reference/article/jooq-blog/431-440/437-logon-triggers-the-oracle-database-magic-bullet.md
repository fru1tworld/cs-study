# 로그온 트리거: Oracle 데이터베이스 만능 해결책

> 원문: https://blog.jooq.org/logon-triggers-the-oracle-database-magic-bullet/

작성자: lukaseder
작성일: 2014년 7월 14일

## 도입

Oracle 데이터베이스를 튜닝하기 위해 상세한 사용 통계를 수집하고 싶다고 상상해 보세요. 예를 들어, 실행 계획에서 A-Rows와 A-Time 값을 얻고 싶을 수 있습니다(기본적으로 Oracle은 'E'가 'Estimated(추정)'를 의미하는 E-Rows와 E-Time만 보고합니다).

일반적으로, 포괄적인 통계를 활성화하려면 sysdba 권한이 필요합니다:

```sql
C:\> sqlplus "/ as sysdba"
Connected to:
Oracle Database 11g Express Edition Release 11.2.0.2.0

SQL> alter system set statistics_level = all;
System altered.
```

## 주요 해결책

대부분의 사용자는 sysdba 접근 권한이 없기 때문에, 이 글에서는 로그온 트리거를 사용한 우회 방법을 제시합니다. 기본적인 구현은 다음과 같습니다:

```sql
CREATE OR REPLACE TRIGGER logon_actions
AFTER LOGON
ON DATABASE
ENABLE
BEGIN
    EXECUTE IMMEDIATE
    'ALTER SESSION SET STATISTICS_LEVEL = all';
END;
/
```

## 고급 구현

조건부 실행을 위해, 저자는 디버그 로깅 패키지를 활용하는 방법을 제안합니다:

```sql
DECLARE
    v_loglevel VARCHAR2(100);
BEGIN
    v_loglevel := logger_package.loglevel;

    -- 로그 레벨이 DEBUG일 때만 통계 수집 활성화
    IF v_loglevel = 'DEBUG' THEN
        EXECUTE IMMEDIATE
        'ALTER SESSION SET STATISTICS_LEVEL = all';
    END IF;
END;
```

## 확인 쿼리

올바른 구성을 확인하려면:

```sql
SELECT SID, NAME, VALUE
FROM   V$SES_OPTIMIZER_ENV
WHERE  NAME = 'statistics_level'
AND SID = (
    SELECT SID
    FROM V$MYSTAT
    WHERE ROWNUM = 1
);
```

## 결론

실행 계획에서 A-Rows와 A-Time 값을 얻는 방법에 대해 알아보려면, 이 글을 읽어보세요. 즐거운 통계 수집 되세요!

태그: logon triggers, Oracle, sql, statistics, STATISTICS_LEVEL
