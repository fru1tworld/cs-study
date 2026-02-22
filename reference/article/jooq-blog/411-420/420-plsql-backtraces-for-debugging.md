# 디버깅을 위한 PL/SQL 역추적

> 원문: https://blog.jooq.org/plsql-backtraces-for-debugging/

이 기능은 PL/SQL 개발자들에게는 상식일 수 있지만, 저희 고객 중 한 분은 이 기능을 알지 못했습니다. 대규모 PL/SQL 애플리케이션에서 깊은 호출 스택 내부에 오류가 발생하면 정확한 원인을 찾아내기가 어려워집니다.

## 전통적인 해결 방법

구식 접근 방식은 문장 번호 추적을 사용하는 것입니다:

```plsql
DECLARE
  v_statement_no := 0;
BEGIN
  v_statement_no := 1;
  SELECT ...

  v_statement_no := 2;
  INSERT ...

  v_statement_no := 3;
  ...
EXCEPTION
  WHEN OTHERS THEN
    logger.error(module, v_statement_no, sqlerrm);
END;
```

이것은 Java에서 println 디버깅을 하는 것과 비슷합니다 - 피해야 할 관행입니다.

## 현대적인 해결책: DBMS_UTILITY.FORMAT_ERROR_BACKTRACE

대신 `DBMS_UTILITY.FORMAT_ERROR_BACKTRACE` 함수를 사용하세요!

### 익명 블록 예제

```plsql
DECLARE
  PROCEDURE p4 IS BEGIN
    raise_application_error(-20000, 'Some Error');
  END p4;
  PROCEDURE p3 IS BEGIN
    p4;
  END p3;
  PROCEDURE p2 IS BEGIN
    p3;
  END p2;
  PROCEDURE p1 IS BEGIN
    p2;
  END p1;
BEGIN
  p1;
EXCEPTION
  WHEN OTHERS THEN
    dbms_output.put_line(sqlerrm);
    dbms_output.put_line(
      dbms_utility.format_error_backtrace
    );
END;
/
```

출력:

```
ORA-20000: Some Error
ORA-06512: at line 3
ORA-06512: at line 6
ORA-06512: at line 9
ORA-06512: at line 12
ORA-06512: at line 16
```

### 명명된 프로시저 예제

익명 블록 내의 지역 프로시저 대신 저장 프로시저를 사용하면 더 자세한 정보를 제공합니다:

```plsql
CREATE PROCEDURE p4 IS BEGIN
  raise_application_error(-20000, 'Some Error');
END p4;
/
CREATE PROCEDURE p3 IS BEGIN
  p4;
END p3;
/
CREATE PROCEDURE p2 IS BEGIN
  p3;
END p2;
/
CREATE PROCEDURE p1 IS BEGIN
  p2;
END p1;
/

BEGIN
  p1;
EXCEPTION
  WHEN OTHERS THEN
    dbms_output.put_line(sqlerrm);
    dbms_output.put_line(
      dbms_utility.format_error_backtrace
    );
END;
/
```

향상된 출력:

```
ORA-20000: Some Error
ORA-06512: at "PLAYGROUND.P4", line 2
ORA-06512: at "PLAYGROUND.P3", line 2
ORA-06512: at "PLAYGROUND.P2", line 2
ORA-06512: at "PLAYGROUND.P1", line 2
ORA-06512: at line 2
```

## 결론

DBMS_UTILITY 패키지의 매뉴얼을 참고하는 것을 권장합니다. 이러한 유틸리티에는 "꽤 다양한 것들"이 포함되어 있습니다.
