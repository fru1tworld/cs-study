# Chapter 43: PL/pgSQL - SQL 절차적 언어 (SQL Procedural Language)

PL/pgSQL은 PostgreSQL 데이터베이스 시스템을 위한 로드 가능한 절차적 언어입니다. PL/pgSQL의 설계 목표는 다음과 같습니다:

- 함수(Functions)와 프로시저(Procedures)를 작성하는 데 사용할 수 있는 로드 가능한 절차적 언어 제공
- SQL 언어에 제어 구조 추가
- 복잡한 계산 수행 가능
- 모든 사용자 정의 타입, 함수, 프로시저, 연산자 상속
- 서버에서 신뢰할 수 있는(trusted) 언어로 정의

PL/pgSQL로 작성된 함수는 내장 함수가 사용될 수 있는 모든 곳에서 사용할 수 있습니다. 예를 들어, 복잡한 조건부 계산 함수를 만들고 이를 연산자 정의나 인덱스 표현식에서 사용할 수 있습니다.

PostgreSQL 9.0 이상에서 PL/pgSQL은 기본적으로 설치됩니다. 그러나 여전히 로드 가능한 모듈이므로, 특히 보안에 민감한 관리자는 이를 제거할 수 있습니다.

---

## 43.1 개요 (Overview)

### 43.1.1 PL/pgSQL 사용의 장점

SQL은 PostgreSQL 및 대부분의 다른 관계형 데이터베이스가 쿼리 언어로 사용하는 언어입니다. 이식 가능하고 배우기 쉽습니다. 그러나 모든 SQL 문은 데이터베이스 서버에서 개별적으로 실행되어야 합니다.

이는 클라이언트 애플리케이션이 각 쿼리를 데이터베이스 서버로 보내고, 처리되기를 기다린 후 결과를 받고, 일부 계산을 수행한 다음 서버에 추가 쿼리를 보내야 함을 의미합니다. 클라이언트가 데이터베이스 서버와 다른 머신에 있는 경우 이 모든 것이 프로세스 간 통신을 수반하며 네트워크 오버헤드를 발생시킵니다.

PL/pgSQL을 사용하면 계산 블록과 일련의 쿼리를 데이터베이스 서버 내에서 그룹화하여 절차적 언어의 강력함과 SQL의 사용 용이성을 결합하면서 상당한 클라이언트/서버 통신 오버헤드를 절약할 수 있습니다.

- 클라이언트와 서버 간의 추가 왕복 제거
- 클라이언트에 필요하지 않거나 단순히 조건을 테스트하기 위해 전송해야 하는 중간 결과를 서버와 클라이언트 간에 전송할 필요 없음
- 여러 번의 쿼리 파싱을 피할 수 있음

이로 인해 순수 SQL을 사용하는 것에 비해 상당한 성능 향상을 얻을 수 있습니다.

또한 PL/pgSQL을 사용하면 SQL에서 사용 가능한 모든 데이터 타입, 연산자, 함수를 사용할 수 있습니다.

### 43.1.2 지원되는 인자 및 결과 데이터 타입

PL/pgSQL으로 작성된 함수는 서버가 지원하는 모든 스칼라 또는 배열 데이터 타입을 인자로 받아들이고 결과로 반환할 수 있습니다. 또한 지정된 행 타입의 복합 타입(행 타입)을 받아들이거나 반환할 수 있습니다. 함수가 반환 타입으로 `record`를 선언하면 익명의 복합 타입도 반환할 수 있습니다.

PL/pgSQL 함수는 `VARIADIC` 마커를 사용하여 가변 개수의 인자를 받아들이도록 선언할 수 있습니다. 이는 SQL 함수와 정확히 동일한 방식으로 작동합니다.

PL/pgSQL 함수는 다형성 타입 `anyelement`, `anyarray`, `anynonarray`, `anyenum`, `anyrange`, `anycompatible`, `anycompatiblearray`, `anycompatiblenonarray`, `anycompatiblerange`를 사용하여 다형성으로 선언할 수도 있습니다.

PL/pgSQL 함수는 집합(또는 테이블)을 반환하도록 선언할 수도 있습니다. 이러한 함수는 모든 집합을 생성하기 위해 원하는 각 행에 대해 `RETURN NEXT`를 실행하거나 `RETURN QUERY`를 사용하여 쿼리 결과를 출력합니다.

---

## 43.2 PL/pgSQL의 구조 (Structure of PL/pgSQL)

PL/pgSQL로 작성된 함수는 다음과 같은 형식으로 정의됩니다:

```sql
CREATE FUNCTION somefunc(integer, text) RETURNS integer
AS $$
    -- 함수 본문
$$ LANGUAGE plpgsql;
```

함수 본문은 단순히 `CREATE FUNCTION`에 대한 문자열 리터럴입니다. 달러 인용(dollar quoting)을 사용하여 함수 본문을 작성하는 것이 더 도움이 됩니다. 달러 인용이 없으면 함수 본문의 작은따옴표나 백슬래시를 이중으로 사용하여 이스케이프해야 합니다.

### 43.2.1 블록 구조

PL/pgSQL은 블록 구조 언어입니다. 함수 본문의 전체 텍스트는 블록이어야 합니다. 블록은 다음과 같이 정의됩니다:

```sql
[ <<label>> ]
[ DECLARE
    declarations ]
BEGIN
    statements
END [ label ];
```

블록 내의 각 선언과 각 문은 세미콜론으로 종료됩니다. 위에 표시된 대로 다른 블록 내에 나타나는 블록은 세미콜론을 가져야 하지만, 함수 본문을 종료하는 최종 `END`는 세미콜론이 필요하지 않습니다.

> 팁: 일반적인 실수는 `BEGIN`/`END` 바로 앞에 세미콜론을 쓰는 것입니다. 이는 올바르지 않으며 구문 오류가 발생합니다.

레이블(label)은 `EXIT` 문에 의해 종료되거나 블록에서 선언된 변수의 이름을 한정하는 데에만 필요합니다. 레이블이 `END` 뒤에 지정되면 블록의 시작 레이블과 일치해야 합니다.

모든 키워드는 대소문자를 구분하지 않습니다. 식별자는 큰따옴표로 묶이지 않는 한 암묵적으로 소문자로 변환됩니다.

PL/pgSQL 코드에서 주석은 SQL에서와 동일한 방식으로 작동합니다. 이중 대시(`--`)는 줄 끝까지 확장되는 주석을 시작합니다. `/*`는 `*/`가 나타날 때까지 확장되는 블록 주석을 시작합니다. 블록 주석은 중첩됩니다.

블록의 문 섹션에 있는 모든 문은 중첩 블록일 수 있습니다. 블록은 컨트롤 구문의 논리적 그룹화 또는 변수를 작은 그룹의 문에 지역화하는 데 사용할 수 있습니다. 선언 섹션에서 선언된 변수는 블록 내의 문을 처리하기 전에 기본값으로 초기화되며, 매번 블록에 진입할 때마다 초기화됩니다.

```sql
CREATE FUNCTION somefunc() RETURNS integer AS $$
<< outerblock >>
DECLARE
    quantity integer := 30;
BEGIN
    RAISE NOTICE 'Quantity here is %', quantity;  -- 30이 출력됨
    quantity := 50;
    --
    -- quantity에 대한 지역 선언을 가진 서브블록 생성
    --
    DECLARE
        quantity integer := 80;
    BEGIN
        RAISE NOTICE 'Quantity here is %', quantity;  -- 80이 출력됨
        RAISE NOTICE 'Outer quantity here is %', outerblock.quantity;  -- 50이 출력됨
    END;

    RAISE NOTICE 'Quantity here is %', quantity;  -- 50이 출력됨

    RETURN quantity;
END;
$$ LANGUAGE plpgsql;
```

> 참고: `BEGIN`/`END` 쌍과 트랜잭션 제어를 위한 같은 이름의 SQL 명령 사이에는 실제로 혼동이 없습니다. PL/pgSQL의 `BEGIN`/`END`는 그룹화만을 위한 것이며 트랜잭션을 시작하거나 종료하지 않습니다. PL/pgSQL에서 트랜잭션을 관리하는 것은 나중에 설명됩니다.

---

## 43.3 선언 (Declarations)

블록에서 사용되는 모든 변수는 블록의 선언 섹션에서 선언되어야 합니다. (유일한 예외는 `FOR` 루프의 루프 변수로, 정수 범위를 반복하는 경우 자동으로 정수 변수로 선언되고, 커서 결과를 반복하는 경우 레코드 변수로 선언됩니다.)

PL/pgSQL 변수는 `integer`, `varchar`, `char`와 같은 모든 SQL 데이터 타입은 물론, 사용자 정의 타입도 가질 수 있습니다.

변수 선언의 일반적인 구문은 다음과 같습니다:

```sql
name [ CONSTANT ] type [ COLLATE collation_name ] [ NOT NULL ] [ { DEFAULT | := | = } expression ];
```

`DEFAULT` 절이 주어지면 블록에 진입할 때 변수에 할당될 초기값을 지정합니다. `DEFAULT` 절이 주어지지 않으면 변수는 SQL 널 값으로 초기화됩니다. `CONSTANT` 옵션은 변수가 값을 할당받지 못하도록 하여 블록이 지속되는 동안 값이 일정하게 유지되도록 합니다. `COLLATE` 옵션은 변수에 사용할 조합(collation)을 지정합니다. `NOT NULL`이 지정되면 널 값을 할당하면 런타임 오류가 발생합니다. `NOT NULL`로 선언된 모든 변수는 널이 아닌 기본값을 지정해야 합니다. `=`는 PL/SQL 규정을 따르기 위해 `:=` 대신 사용할 수 있습니다.

변수의 기본값은 함수가 호출될 때마다가 아니라 블록에 진입할 때마다 평가되고 변수에 할당됩니다. 예를 들어, `integer` 변수에 `now()`를 할당하면 `now()`가 호출되는 순간의 현재 타임스탬프가 할당됩니다.

```sql
quantity integer DEFAULT 32;
url varchar := 'http://mysite.com';
transaction_time CONSTANT timestamp with time zone := now();
```

최상위 블록 수준에서 한 번 선언된 변수는 해당 함수의 저장된 표현에 암묵적으로 저장되므로, 이후에 변수가 사용되지 않더라도 함수가 호출될 때마다 기본 표현식이 실행됩니다.

### 43.3.1 함수 인자 선언

함수에 전달된 인자는 식별자 `$1`, `$2` 등으로 명명됩니다. 선택적으로 가독성 향상을 위해 인자 값에 대한 별칭을 선언할 수 있습니다. 그런 다음 별칭이나 숫자 식별자를 사용하여 인자 값을 참조할 수 있습니다.

별칭을 만드는 두 가지 방법이 있습니다. 선호되는 방법은 `CREATE FUNCTION` 명령에서 인자에 이름을 지정하는 것입니다:

```sql
CREATE FUNCTION sales_tax(subtotal real) RETURNS real AS $$
BEGIN
    RETURN subtotal * 0.06;
END;
$$ LANGUAGE plpgsql;
```

다른 방법은 선언 구문으로 별칭을 명시적으로 선언하는 것입니다:

```sql
name ALIAS FOR $n;
```

같은 예제를 이 스타일로 작성하면:

```sql
CREATE FUNCTION sales_tax(real) RETURNS real AS $$
DECLARE
    subtotal ALIAS FOR $1;
BEGIN
    RETURN subtotal * 0.06;
END;
$$ LANGUAGE plpgsql;
```

> 참고: 이 두 예제는 완전히 동일하지 않습니다. 첫 번째 경우, `subtotal`은 `sales_tax.subtotal`로 참조될 수 있지만, 두 번째 경우에는 그렇지 않습니다. (별칭을 외부 블록에 레이블을 붙인 경우 별칭을 한정할 수 있습니다.)

출력 인자는 입력 인자와 동일한 방식으로 PL/pgSQL 함수에서 처리됩니다. 출력 인자는 함수의 시작 부분에서 NULL로 시작되며, 함수 실행 중에 할당되어야 합니다. 최종 출력 인자의 값은 반환되는 값입니다. 예를 들어, 판매세 예제는 다음과 같이 작성할 수도 있습니다:

```sql
CREATE FUNCTION sales_tax(subtotal real, OUT tax real) AS $$
BEGIN
    tax := subtotal * 0.06;
END;
$$ LANGUAGE plpgsql;
```

출력 인자는 함수가 여러 값을 반환할 때 가장 유용합니다.

### 43.3.2 별칭 (Aliases)

```sql
newname ALIAS FOR oldname;
```

`ALIAS` 구문은 함수 인자에 별칭을 할당하는 것보다 더 일반적입니다: 이전에 선언된 변수에 다른 이름을 선언할 수 있습니다. 이는 주로 트리거 함수와 같이 미리 정의된 이름을 가진 변수에 더 의미 있는 이름을 할당하는 데 유용합니다.

```sql
DECLARE
  prior ALIAS FOR old;
  updated ALIAS FOR new;
```

`ALIAS`는 새 변수를 만듭니다; 같은 변수를 참조하는 두 가지 방법이 아닙니다.

### 43.3.3 복사된 타입 (Copied Types)

```sql
variable%TYPE
```

`%TYPE`은 변수나 테이블 열의 데이터 타입을 제공합니다. 이를 사용하여 데이터베이스 객체의 데이터 타입을 보유하는 변수를 선언할 수 있습니다. 예를 들어, `users` 테이블에 `user_id`라는 열이 있다고 가정하면 다음과 같이 작성할 수 있습니다:

```sql
user_id users.user_id%TYPE;
```

`%TYPE`을 사용하면 참조하는 데이터베이스 객체의 정의를 알 필요가 없으며, 가장 중요한 것은 참조된 객체의 데이터 타입이 미래에 변경되더라도(예: `user_id`의 타입을 `integer`에서 `real`로 변경) 함수 정의를 변경할 필요가 없다는 것입니다.

`%TYPE`은 함수 인자에도 사용할 수 있습니다:

```sql
CREATE FUNCTION sales_tax(subtotal real) RETURNS real AS $$
DECLARE
    proportion subtotal%TYPE := 0.06;
BEGIN
    RETURN subtotal * proportion;
END;
$$ LANGUAGE plpgsql;
```

### 43.3.4 행 타입 (Row Types)

```sql
name table_name%ROWTYPE;
name composite_type_name;
```

복합 타입의 변수를 행 변수(또는 행-타입 변수)라고 합니다. 이러한 변수는 `SELECT` 또는 `FOR` 쿼리 결과의 전체 행을 보유할 수 있습니다. 단, 해당 쿼리의 열 집합이 변수의 선언된 타입과 일치해야 합니다. 행 값의 개별 필드는 일반적인 점 표기법을 사용하여 액세스됩니다(예: `rowvar.field`).

행 변수는 복합 타입의 이름을 사용하여 선언하거나 `table_name%ROWTYPE` 표기법을 사용하여 테이블 또는 뷰의 행과 동일한 타입을 가진 변수를 선언할 수 있습니다. `%ROWTYPE`과 함께 사용되는 테이블 또는 뷰 이름은 스키마로 한정될 수 있습니다.

함수의 인자는 복합 타입(테이블 행 포함)일 수 있습니다. 이 경우 해당 식별자 `$n`은 행 변수가 되며, 필드는 점 표기법을 사용하여 선택할 수 있습니다(예: `$1.user_id`).

```sql
CREATE FUNCTION merge_fields(t_row table1) RETURNS text AS $$
DECLARE
    t2_row table2%ROWTYPE;
BEGIN
    SELECT * INTO t2_row FROM table2 WHERE ... ;
    RETURN t_row.f1 || t2_row.f3 || t_row.f5 || t2_row.f7;
END;
$$ LANGUAGE plpgsql;

SELECT merge_fields(t.*) FROM table1 t WHERE ... ;
```

### 43.3.5 레코드 타입 (Record Types)

```sql
name RECORD;
```

레코드 변수는 행 타입 변수와 비슷하지만 미리 정의된 구조가 없습니다. 레코드 변수는 `SELECT` 또는 `FOR` 명령 중에 할당된 행의 실제 행 구조를 취합니다. 레코드 변수의 하위 구조는 할당될 때마다 변경될 수 있습니다.

레코드 변수는 처음 할당되기 전까지 하위 구조가 없다는 결과가 있습니다. 할당되지 않은 레코드 변수의 필드에 액세스하려고 하면 런타임 오류가 발생합니다.

`RECORD`는 진정한 데이터 타입이 아니라 플레이스홀더일 뿐입니다. PL/pgSQL 함수가 `record` 타입을 반환하도록 선언된 경우 이는 레코드 변수와 정확히 동일한 개념이 아닙니다.

### 43.3.6 조합 가능한 SQL 함수 속성 (Collation of PL/pgSQL Variables)

PL/pgSQL 함수에 조합 가능한 데이터 타입의 인자가 있는 경우, 함수 호출 시 인자에서 파생된 `collation`을 기반으로 조합(collation)이 식별됩니다. 조합이 성공적으로 식별되면(즉, 인자 간에 암묵적 조합 충돌이 없음), 모든 조합 가능한 인자가 암묵적으로 해당 조합을 갖는 것처럼 처리됩니다.

---

## 43.4 표현식 (Expressions)

PL/pgSQL에서 사용되는 모든 표현식은 PostgreSQL의 메인 SQL 실행기에서 처리됩니다. 예를 들어 다음과 같이 작성할 때:

```sql
IF expression THEN ...
```

PL/pgSQL은 내부적으로 다음과 같은 쿼리를 실행합니다:

```sql
SELECT expression
```

표현식에서 선언된 PL/pgSQL 변수에 대한 참조를 매개변수로 대체합니다. 이렇게 하면 `SELECT`에 대한 쿼리 계획이 한 번만 준비되고 이후 호출에서 재사용됩니다.

이러한 방식으로 표현식을 평가하면 일반적인 SQL 쿼리가 할 수 있는 모든 것을 의미합니다:

- 산술, 문자열, 비교 연산자를 사용하여 복잡한 표현식 계산
- 함수 호출
- 스칼라 서브쿼리 포함

그러나 표현식이 스칼라 값으로 평가되어야 한다는 요구 사항도 있습니다(복합 타입을 반환하는 경우도 단일 값).

---

## 43.5 기본 문 (Basic Statements)

### 43.5.1 할당 (Assignment)

변수 또는 행/레코드 필드에 대한 할당은 다음과 같이 작성됩니다:

```sql
variable { := | = } expression;
```

앞서 언급했듯이 이러한 문의 표현식은 PostgreSQL 메인 SQL 엔진에 전송된 `SELECT` 명령을 통해 평가됩니다. 표현식은 단일 값을 생성해야 합니다(변수가 행 또는 레코드 변수인 경우 행 값일 수 있음).

대상 변수가 단순 변수(행/레코드 변수가 아님)인 경우 `=`가 `:=` 대신 사용될 수 있습니다. 특히 이를 통해 `UPDATE` 명령에서 직접 계산된 값을 사용하여 할당할 수 있습니다.

```sql
tax := subtotal * 0.06;
my_record.user_id := 20;
my_array[1] := 5;
my_array[2:4] := ARRAY[1, 2, 3];
```

### 43.5.2 단일 행 결과를 갖는 SELECT 실행 (Executing a Command with a Single-Row Result)

단일 행(아마도 여러 열)을 생성하는 SQL 명령의 결과는 레코드 변수, 행 타입 변수 또는 스칼라 변수 목록에 할당될 수 있습니다. 이는 기본 SQL 명령에 `INTO` 절을 작성하여 수행됩니다:

```sql
SELECT select_expressions INTO [STRICT] target FROM ...;
INSERT ... RETURNING expressions INTO [STRICT] target;
UPDATE ... RETURNING expressions INTO [STRICT] target;
DELETE ... RETURNING expressions INTO [STRICT] target;
```

여기서 `target`은 레코드 변수, 행 변수 또는 쉼표로 구분된 단순 변수와 레코드/행 필드 목록일 수 있습니다.

`SELECT`를 사용하는 경우 `INTO` 절은 쿼리의 `select_expressions` 목록 바로 뒤, 또는 `FROM` 절 바로 앞에 작성할 수 있습니다.

`STRICT`가 지정되지 않으면 `target`은 쿼리에서 반환된 첫 번째 행으로 설정되고, 쿼리가 행을 반환하지 않으면 널로 설정됩니다. (첫 번째 행은 `ORDER BY`를 사용하지 않는 한 "정의되지 않음"입니다.) 첫 번째 행 이후의 모든 결과 행은 버려집니다.

```sql
SELECT * INTO myrec FROM emp WHERE empname = myname;
IF NOT FOUND THEN
    RAISE EXCEPTION 'employee % not found', myname;
END IF;
```

`STRICT` 옵션이 지정되면 쿼리가 정확히 하나의 행을 반환해야 합니다. 그렇지 않으면 `NO_DATA_FOUND`(행이 없음) 또는 `TOO_MANY_ROWS`(둘 이상의 행)와 같은 런타임 오류가 발생합니다.

```sql
BEGIN
    SELECT * INTO STRICT myrec FROM emp WHERE empname = myname;
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            RAISE EXCEPTION 'employee % not found', myname;
        WHEN TOO_MANY_ROWS THEN
            RAISE EXCEPTION 'employee % not unique', myname;
END;
```

`STRICT`가 지정되지 않은 쿼리의 경우 `FOUND`는 행이 반환되면 true로, 반환되지 않으면 false로 설정됩니다.

### 43.5.3 결과가 없는 명령 또는 동적 명령 실행 (Executing a Command with No Result)

관심 있는 결과를 반환하지 않는 SQL 명령의 경우(예: `INSERT` without `RETURNING`, 또는 결과가 필요 없는 다른 DML), 일반 SQL 명령으로 실행할 수 있습니다:

```sql
INSERT INTO mytable (id, value) VALUES (nextval('myseq'), 'hello');
```

또는 `PERFORM` 문을 사용하여 표현식을 평가하고 결과를 버릴 수 있습니다:

```sql
PERFORM query;
```

이는 `query`를 실행하고 결과를 버립니다. `SELECT`라는 단어 대신 `PERFORM` 키워드를 사용하는 것 외에는 일반 `SELECT` 문과 동일하게 작성합니다.

```sql
PERFORM create_mv('cs_session_page_requests_mv', my_query);
```

### 43.5.4 동적 명령 실행 (Executing Dynamic Commands)

가변 테이블이나 열에 대해 동적 명령을 생성해야 할 때가 많습니다. PL/pgSQL 함수 내에서 동적 명령을 실행하려면 `EXECUTE` 문을 사용합니다:

```sql
EXECUTE command-string [ INTO [STRICT] target ] [ USING expression [, ... ] ];
```

여기서 `command-string`은 실행할 명령을 포함하는 문자열(텍스트 타입)을 생성하는 표현식입니다.

```sql
EXECUTE 'SELECT count(*) FROM mytable WHERE inserted_by = $1 AND inserted <= $2'
   INTO c
   USING checked_user, checked_date;
```

`USING` 절을 사용하여 명령에 매개변수 값을 삽입합니다. 이는 명령 문자열에 값을 텍스트로 직접 삽입하는 것보다 선호되며, SQL 인젝션 공격에 대한 런타임 오버헤드도 줄어듭니다.

매개변수 기호의 사용을 원하거나 사용해야 하는 경우 `format` 함수가 유용합니다:

```sql
EXECUTE format('SELECT count(*) FROM %I '
   'WHERE inserted_by = $1 AND inserted <= $2', tabname)
   INTO c
   USING checked_user, checked_date;
```

`%I` 지정자는 식별자로 처리되어야 하는 값을 삽입하고, `%L` 지정자는 리터럴로 처리되어야 하는 값을 삽입합니다.

### 43.5.5 쿼리 결과의 상태 얻기 (Obtaining the Result Status)

다음과 같은 명령을 실행한 후 영향을 받은 행 수를 확인할 수 있습니다:

```sql
GET DIAGNOSTICS variable { = | := } item [ , ... ];
```

현재 사용 가능한 상태 항목은 다음과 같습니다:

| 이름 | 타입 | 설명 |
|------|------|------|
| `ROW_COUNT` | bigint | 가장 최근 SQL 명령에서 처리된 행 수 |
| `PG_CONTEXT` | text | 현재 호출 스택을 설명하는 줄 |
| `PG_ROUTINE_OID` | oid | 현재 함수의 OID |

```sql
GET DIAGNOSTICS integer_var = ROW_COUNT;
```

### 43.5.6 아무것도 하지 않기 (Doing Nothing At All)

가끔은 플레이스홀더 문이 아무 작업도 수행하지 않는 것이 유용합니다. 예를 들어, if/then/else 체인의 한 분기가 의도적으로 비어 있음을 나타내려면:

```sql
NULL;
```

예를 들어, 다음 두 코드 조각은 동일합니다:

```sql
BEGIN
    y := x / 0;
EXCEPTION
    WHEN division_by_zero THEN
        NULL;  -- 오류 무시
END;
```

---

## 43.6 제어 구조 (Control Structures)

제어 구조는 아마도 PL/pgSQL의 가장 유용하고 중요한 부분입니다. PL/pgSQL의 제어 구조를 사용하면 매우 유연하고 강력한 방식으로 PostgreSQL 데이터를 조작할 수 있습니다.

### 43.6.1 함수에서 반환 (Returning from a Function)

함수에서 데이터를 반환하는 두 가지 명령이 있습니다: `RETURN`과 `RETURN NEXT`(+ `RETURN QUERY`).

#### 43.6.1.1 RETURN

```sql
RETURN expression;
```

`RETURN`과 함께 표현식을 사용하면 함수가 종료되고 `expression`의 값이 호출자에게 반환됩니다. 이 형식은 스칼라 타입을 반환하는 PL/pgSQL 함수에서 사용됩니다.

```sql
RETURN;
```

표현식 없는 `RETURN`을 사용하여 `void`를 반환하도록 선언된 함수를 종료합니다.

프로시저에서 반환할 때는 표현식 없는 `RETURN`을 사용합니다.

복합 타입을 반환하도록 선언된 함수의 경우 표현식은 적절한 복합 값을 생성해야 합니다:

```sql
RETURN composite_expression;
```

또는 출력 인자를 사용한 경우:

```sql
RETURN;  -- 출력 인자가 자동으로 반환됨
```

#### 43.6.1.2 RETURN NEXT 및 RETURN QUERY

```sql
RETURN NEXT expression;
RETURN QUERY query;
RETURN QUERY EXECUTE command-string [ USING expression [, ... ] ];
```

함수가 `SETOF sometype`을 반환하도록 선언된 경우 따라야 할 절차가 약간 다릅니다. 개별 항목은 `RETURN NEXT` 명령의 시퀀스를 통해 반환되고, 마지막에 표현식 없는 `RETURN`이 함수가 실행을 완료했음을 나타내는 데 사용됩니다.

```sql
CREATE FUNCTION get_all_foo() RETURNS SETOF foo AS $$
DECLARE
    r foo%rowtype;
BEGIN
    FOR r IN
        SELECT * FROM foo WHERE fooid > 0
    LOOP
        -- 일부 처리를 수행할 수 있음
        RETURN NEXT r; -- SELECT * FROM foo의 현재 행을 반환
    END LOOP;
    RETURN;
END;
$$ LANGUAGE plpgsql;
```

`RETURN QUERY`는 쿼리 결과의 모든 행을 함수의 결과 집합에 추가합니다:

```sql
CREATE FUNCTION get_available_flightid(date) RETURNS SETOF integer AS $$
BEGIN
    RETURN QUERY SELECT flightid
                   FROM flight
                  WHERE flightdate >= $1
                    AND flightdate < ($1 + 1);

    -- 쿼리가 아무것도 반환하지 않았는지 확인 가능
    IF NOT FOUND THEN
        RAISE NOTICE 'No flights for %', $1;
    END IF;

    RETURN;
END;
$$ LANGUAGE plpgsql;
```

### 43.6.2 프로시저에서 반환 (Returning from a Procedure)

프로시저에는 반환 값이 없습니다. 따라서 프로시저는 표현식 없는 `RETURN` 문으로 종료할 수 있습니다:

```sql
RETURN;
```

`RETURN` 문은 프로시저가 자연스럽게 끝나기 전에 종료하려는 경우에만 필요합니다.

프로시저의 출력 인자는 프로시저가 정상적으로 종료되거나 `RETURN` 문에 도달하면 호출자에게 반환됩니다.

### 43.6.3 호출자에 대한 제어 반환 (Returning Control to the Caller)

프로시저 본문 내부에서 `COMMIT` 또는 `ROLLBACK`을 호출하면 트랜잭션이 종료되고 새 트랜잭션이 자동으로 시작됩니다. 이러한 명령 후에 실행이 계속됩니다.

### 43.6.4 조건문 (Conditionals)

`IF`와 `CASE` 문을 사용하면 특정 조건에 따라 명령을 실행할 수 있습니다. PL/pgSQL에는 세 가지 형태의 `IF`가 있습니다:

- `IF ... THEN ... END IF`
- `IF ... THEN ... ELSE ... END IF`
- `IF ... THEN ... ELSIF ... THEN ... ELSE ... END IF`

그리고 두 가지 형태의 `CASE`가 있습니다:

- `CASE ... WHEN ... THEN ... ELSE ... END CASE`
- `CASE WHEN ... THEN ... ELSE ... END CASE`

#### IF-THEN

```sql
IF boolean-expression THEN
    statements
END IF;
```

`IF-THEN` 문은 가장 간단한 형태의 `IF`입니다. `THEN`과 `END IF` 사이의 문은 조건이 참이면 실행됩니다. 그렇지 않으면 건너뜁니다.

```sql
IF v_user_id <> 0 THEN
    UPDATE users SET email = v_email WHERE user_id = v_user_id;
END IF;
```

#### IF-THEN-ELSE

```sql
IF boolean-expression THEN
    statements
ELSE
    statements
END IF;
```

`IF-THEN-ELSE` 문은 조건이 참이 아닐 때 실행되어야 할 대체 문 집합을 지정할 수 있도록 `IF-THEN`에 추가합니다.

```sql
IF parentid IS NULL OR parentid = ''
THEN
    RETURN fullname;
ELSE
    RETURN hp_true_filename(parentid) || '/' || fullname;
END IF;
```

#### IF-THEN-ELSIF

```sql
IF boolean-expression THEN
    statements
[ ELSIF boolean-expression THEN
    statements
[ ELSIF boolean-expression THEN
    statements
    ...]]
[ ELSE
    statements ]
END IF;
```

가끔 두 가지 이상의 대안이 있습니다. `IF-THEN-ELSIF`는 이를 가능하게 합니다. 조건은 참이 될 때까지 연속적으로 테스트됩니다.

```sql
IF number = 0 THEN
    result := 'zero';
ELSIF number > 0 THEN
    result := 'positive';
ELSIF number < 0 THEN
    result := 'negative';
ELSE
    -- 다른 유일한 가능성은 number가 널인 경우
    result := 'NULL';
END IF;
```

> 참고: `ELSEIF`는 `ELSIF`의 별칭입니다.

#### 간단한 CASE

```sql
CASE search-expression
    WHEN expression [, expression [ ... ]] THEN
      statements
  [ WHEN expression [, expression [ ... ]] THEN
      statements
    ... ]
  [ ELSE
      statements ]
END CASE;
```

간단한 형태의 `CASE`는 검색 표현식의 값을 기반으로 조건부 실행을 제공합니다.

```sql
CASE x
    WHEN 1, 2 THEN
        msg := 'one or two';
    ELSE
        msg := 'other value than one or two';
END CASE;
```

#### 검색된 CASE

```sql
CASE
    WHEN boolean-expression THEN
      statements
  [ WHEN boolean-expression THEN
      statements
    ... ]
  [ ELSE
      statements ]
END CASE;
```

검색된 형태의 `CASE`는 부울 표현식의 진리값을 기반으로 조건부 실행을 제공합니다.

```sql
CASE
    WHEN x BETWEEN 0 AND 10 THEN
        msg := 'value is between zero and ten';
    WHEN x BETWEEN 11 AND 20 THEN
        msg := 'value is between eleven and twenty';
END CASE;
```

### 43.6.5 단순 루프 (Simple Loops)

`LOOP`, `EXIT`, `CONTINUE`, `WHILE`, `FOR`, `FOREACH` 문을 사용하면 PL/pgSQL 함수가 일련의 명령을 반복하도록 할 수 있습니다.

#### LOOP

```sql
[ <<label>> ]
LOOP
    statements
END LOOP [ label ];
```

`LOOP`는 `EXIT` 또는 `RETURN` 문에 의해 종료될 때까지 무한히 반복되는 무조건적인 루프를 정의합니다. 선택적 레이블은 중첩된 루프 내의 `EXIT` 및 `CONTINUE` 문에서 종료하거나 계속할 루프를 지정하는 데 사용할 수 있습니다.

#### EXIT

```sql
EXIT [ label ] [ WHEN boolean-expression ];
```

레이블이 지정되지 않은 경우 가장 안쪽의 루프가 종료되고 `END LOOP` 다음 문이 실행됩니다. 레이블이 지정되면 해당 레이블이 있는 루프가 종료됩니다.

```sql
LOOP
    -- 일부 계산
    IF count > 0 THEN
        EXIT;  -- 루프 종료
    END IF;
END LOOP;
```

`WHEN`이 있으면 부울 표현식이 참인 경우에만 루프 종료가 발생합니다:

```sql
LOOP
    -- 일부 계산
    EXIT WHEN count > 0;
END LOOP;
```

#### CONTINUE

```sql
CONTINUE [ label ] [ WHEN boolean-expression ];
```

레이블이 지정되지 않은 경우 가장 안쪽 루프의 다음 반복이 시작됩니다. 레이블이 지정되면 해당 레이블이 있는 루프의 다음 반복이 시작됩니다.

```sql
LOOP
    -- 일부 계산
    EXIT WHEN count > 100;
    CONTINUE WHEN count < 50;
    -- count가 50 이상일 때만 관심 있는 일부 계산
END LOOP;
```

#### WHILE

```sql
[ <<label>> ]
WHILE boolean-expression LOOP
    statements
END LOOP [ label ];
```

`WHILE` 문은 부울 표현식이 참으로 평가되는 한 일련의 문을 반복합니다. 각 루프 본문에 진입하기 직전에 표현식이 확인됩니다.

```sql
WHILE amount_owed > 0 AND gift_certificate_balance > 0 LOOP
    -- 일부 계산
END LOOP;

WHILE NOT done LOOP
    -- 일부 계산
END LOOP;
```

#### FOR (정수 변형)

```sql
[ <<label>> ]
FOR name IN [ REVERSE ] expression .. expression [ BY expression ] LOOP
    statements
END LOOP [ label ];
```

이 형태의 `FOR`는 정수 값의 범위를 반복하는 루프를 만듭니다. 변수 `name`은 자동으로 `integer` 타입으로 정의되며 루프 내에서만 존재합니다(기존 변수의 정의는 루프 내에서 무시됨). 범위의 하한과 상한을 지정하는 두 표현식은 루프에 진입할 때 한 번 평가됩니다.

```sql
FOR i IN 1..10 LOOP
    -- i는 루프 내에서 1부터 10까지의 값을 취함
END LOOP;

FOR i IN REVERSE 10..1 LOOP
    -- i는 루프 내에서 10부터 1까지의 값을 취함
END LOOP;

FOR i IN REVERSE 10..1 BY 2 LOOP
    -- i는 루프 내에서 10, 8, 6, 4, 2의 값을 취함
END LOOP;
```

### 43.6.6 쿼리 결과를 통한 루핑 (Looping Through Query Results)

다른 타입의 `FOR` 루프를 사용하면 쿼리의 결과를 반복하고 해당 데이터를 그에 따라 조작할 수 있습니다.

```sql
[ <<label>> ]
FOR target IN query LOOP
    statements
END LOOP [ label ];
```

`target`은 레코드 변수, 행 변수 또는 쉼표로 구분된 스칼라 변수 목록입니다. `target`은 `query`의 각 행에 연속적으로 할당되고 루프 본문이 각 행에 대해 실행됩니다.

```sql
CREATE FUNCTION refresh_mviews() RETURNS integer AS $$
DECLARE
    mviews RECORD;
BEGIN
    RAISE NOTICE 'Refreshing all materialized views...';

    FOR mviews IN
       SELECT n.nspname AS mv_schema,
              c.relname AS mv_name,
              pg_catalog.pg_get_userbyid(c.relowner) AS owner
         FROM pg_catalog.pg_class c
    LEFT JOIN pg_catalog.pg_namespace n ON (n.oid = c.relnamespace)
        WHERE c.relkind = 'm'
     ORDER BY 1
    LOOP

        -- 이제 "mviews"에는 한 행의 레코드가 있습니다

        RAISE NOTICE 'Refreshing materialized view %.%...',
                     quote_ident(mviews.mv_schema),
                     quote_ident(mviews.mv_name);
        EXECUTE format('REFRESH MATERIALIZED VIEW %I.%I',
                       mviews.mv_schema, mviews.mv_name);
    END LOOP;

    RAISE NOTICE 'Done refreshing materialized views.';
    RETURN 1;
END;
$$ LANGUAGE plpgsql;
```

`FOR-IN-EXECUTE`를 사용하여 동적 쿼리의 결과를 반복할 수도 있습니다:

```sql
[ <<label>> ]
FOR target IN EXECUTE text_expression [ USING expression [, ... ] ] LOOP
    statements
END LOOP [ label ];
```

### 43.6.7 배열을 통한 루핑 (Looping Through Arrays)

`FOREACH` 루프는 `FOR` 루프와 매우 비슷하지만 쿼리에서 반환된 행을 반복하는 대신 배열 값의 요소를 반복합니다.

```sql
[ <<label>> ]
FOREACH target [ SLICE number ] IN ARRAY expression LOOP
    statements
END LOOP [ label ];
```

`SLICE` 없이(또는 `SLICE 0`) `FOREACH`는 표현식을 평가하여 생성된 배열의 개별 요소를 반복합니다.

```sql
CREATE FUNCTION sum(int[]) RETURNS int8 AS $$
DECLARE
  s int8 := 0;
  x int;
BEGIN
  FOREACH x IN ARRAY $1
  LOOP
    s := s + x;
  END LOOP;
  RETURN s;
END;
$$ LANGUAGE plpgsql;
```

양수 `SLICE` 값을 사용하면 `FOREACH`는 개별 요소가 아니라 배열의 슬라이스를 반복합니다.

```sql
CREATE FUNCTION scan_rows(int[]) RETURNS void AS $$
DECLARE
  x int[];
BEGIN
  FOREACH x SLICE 1 IN ARRAY $1
  LOOP
    RAISE NOTICE 'row = %', x;
  END LOOP;
END;
$$ LANGUAGE plpgsql;

SELECT scan_rows(ARRAY[[1,2,3],[4,5,6],[7,8,9],[10,11,12]]);
-- 출력:
-- NOTICE:  row = {1,2,3}
-- NOTICE:  row = {4,5,6}
-- NOTICE:  row = {7,8,9}
-- NOTICE:  row = {10,11,12}
```

### 43.6.8 오류 트래핑 (Trapping Errors)

기본적으로 PL/pgSQL 함수에서 발생하는 모든 오류는 함수 실행을 중단하고 실제로 주변 트랜잭션도 중단합니다. `BEGIN` 블록에 `EXCEPTION` 절을 사용하여 오류를 트랩하고 복구할 수 있습니다:

```sql
[ <<label>> ]
[ DECLARE
    declarations ]
BEGIN
    statements
EXCEPTION
    WHEN condition [ OR condition ... ] THEN
        handler_statements
    [ WHEN condition [ OR condition ... ] THEN
          handler_statements
      ... ]
END;
```

`EXCEPTION` 절이 있는 경우, 블록에 진입하기 전에 암묵적인 서브트랜잭션(subtransaction)이 설정됩니다. 블록이 성공적으로 완료되면 서브트랜잭션이 커밋됩니다. 오류가 발생하면 블록에서 수행한 모든 데이터베이스 변경 사항이 롤백되고 적절한 예외 핸들러로 제어가 전달됩니다.

```sql
CREATE TABLE mytab(id int PRIMARY KEY, data text);
INSERT INTO mytab(id, data) VALUES (1, 'hello');

CREATE FUNCTION merge_db(key int, data text) RETURNS void AS $$
BEGIN
    LOOP
        -- 먼저 업데이트 시도
        UPDATE mytab SET data = merge_db.data WHERE id = key;
        IF found THEN
            RETURN;
        END IF;
        -- 없으면 삽입 시도
        -- 다른 세션이 동시에 행을 삽입하면 고유 키 오류가 발생할 수 있음
        BEGIN
            INSERT INTO mytab(id, data) VALUES (key, data);
            RETURN;
        EXCEPTION WHEN unique_violation THEN
            -- 아무것도 하지 않고 UPDATE를 다시 시도
        END;
    END LOOP;
END;
$$ LANGUAGE plpgsql;

SELECT merge_db(1, 'david');
SELECT merge_db(1, 'dennis');
```

특수 조건 이름 `OTHERS`는 명시적으로 명명된 조건 외의 모든 오류 타입과 일치합니다. 오류 데이터를 파괴하므로 `OTHERS` 핸들러의 마지막 `RAISE`는 일반적으로 `RAISE`(원래 오류를 다시 발생)입니다.

예외 핸들러 내에서 `GET STACKED DIAGNOSTICS` 명령을 사용하여 현재 예외에 대한 정보를 얻을 수 있습니다:

```sql
GET STACKED DIAGNOSTICS variable { = | := } item [ , ... ];
```

사용 가능한 항목은 다음과 같습니다:

| 이름 | 타입 | 설명 |
|------|------|------|
| `RETURNED_SQLSTATE` | text | 예외의 SQLSTATE 오류 코드 |
| `COLUMN_NAME` | text | 예외와 관련된 열 이름 |
| `CONSTRAINT_NAME` | text | 예외와 관련된 제약 조건 이름 |
| `PG_DATATYPE_NAME` | text | 예외와 관련된 데이터 타입 이름 |
| `MESSAGE_TEXT` | text | 예외의 기본 메시지 텍스트 |
| `TABLE_NAME` | text | 예외와 관련된 테이블 이름 |
| `SCHEMA_NAME` | text | 예외와 관련된 스키마 이름 |
| `PG_EXCEPTION_DETAIL` | text | 예외의 상세 메시지 텍스트 |
| `PG_EXCEPTION_HINT` | text | 예외의 힌트 메시지 텍스트 |
| `PG_EXCEPTION_CONTEXT` | text | 예외 컨텍스트를 설명하는 줄 |

```sql
DECLARE
  text_var1 text;
  text_var2 text;
  text_var3 text;
BEGIN
  -- 일부 처리 수행
  ...
EXCEPTION WHEN OTHERS THEN
  GET STACKED DIAGNOSTICS text_var1 = MESSAGE_TEXT,
                          text_var2 = PG_EXCEPTION_DETAIL,
                          text_var3 = PG_EXCEPTION_HINT;
END;
```

---

## 43.7 커서 (Cursors)

결과 집합 전체를 한 번에 실행하는 대신 커서를 설정하고 쿼리를 캡슐화한 다음 쿼리 결과를 한 번에 몇 행씩 읽을 수 있습니다. 이 방법의 한 가지 이유는 결과에 많은 수의 행이 포함되어 있을 때 메모리 오버플로우를 방지하기 위해서입니다.

PL/pgSQL에서 커서를 사용하는 또 다른 방법은 함수의 결과로 커서가 참조하는 행을 반환하는 것입니다. 이렇게 하면 함수에서 대량의 행 집합을 반환하는 효율적인 방법이 됩니다. 호출자는 반환된 커서에서 행을 읽습니다.

### 43.7.1 커서 변수 선언 (Declaring Cursor Variables)

PL/pgSQL에서 커서에 대한 모든 액세스는 커서 변수를 통해 이루어지며, 이는 항상 특수 데이터 타입 `refcursor`입니다. 커서 변수를 만드는 한 가지 방법은 `refcursor` 타입의 변수로 선언하는 것입니다. 다른 방법은 커서 선언 구문을 사용하는 것입니다:

```sql
name [ [ NO ] SCROLL ] CURSOR [ ( arguments ) ] FOR query;
```

`SCROLL`이 지정되면 커서는 역방향으로 스크롤할 수 있습니다. `NO SCROLL`이 지정되면 역방향 페치가 거부됩니다. 지정되지 않은 경우 쿼리에 따라 역방향 페치가 허용될 수 있습니다.

```sql
DECLARE
    curs1 refcursor;
    curs2 CURSOR FOR SELECT * FROM tenk1;
    curs3 CURSOR (key integer) FOR SELECT * FROM tenk1 WHERE unique1 = key;
```

세 가지 모두 데이터 타입은 `refcursor`이지만, 첫 번째는 모든 쿼리에 사용할 수 있고, 두 번째는 이미 완전히 지정된 쿼리에 바인딩되어 있으며, 세 번째는 매개변수화된 쿼리에 바인딩되어 있습니다.

### 43.7.2 커서 열기 (Opening Cursors)

커서를 사용하여 행을 검색하기 전에 열어야 합니다. PL/pgSQL에는 세 가지 형태의 `OPEN` 문이 있습니다.

#### OPEN FOR query

```sql
OPEN unbound_cursorvar [ [ NO ] SCROLL ] FOR query;
```

커서 변수가 열리고 지정된 쿼리가 실행됩니다.

```sql
OPEN curs1 FOR SELECT * FROM foo WHERE key = mykey;
```

#### OPEN FOR EXECUTE

```sql
OPEN unbound_cursorvar [ [ NO ] SCROLL ] FOR EXECUTE query_string
                                         [ USING expression [, ... ] ];
```

커서 변수가 열리고 지정된 쿼리가 실행됩니다. 쿼리는 문자열 표현식으로 지정됩니다.

```sql
OPEN curs1 FOR EXECUTE format('SELECT * FROM %I WHERE col1 = $1', tabname) USING keyvalue;
```

#### 바인딩된 커서 열기

```sql
OPEN bound_cursorvar [ ( [ argument_name := ] argument_value [, ...] ) ];
```

이 형태의 `OPEN`은 선언 시 쿼리에 바인딩된 커서 변수에 사용됩니다.

```sql
OPEN curs2;
OPEN curs3(42);
OPEN curs3(key := 42);
```

### 43.7.3 커서 사용 (Using Cursors)

커서가 열리면 여기에 설명된 문을 사용하여 조작할 수 있습니다.

#### FETCH

```sql
FETCH [ direction { FROM | IN } ] cursor INTO target;
```

`FETCH`는 커서에서 다음 행을 가져와 `target`(행 변수, 레코드 변수 또는 쉼표로 구분된 단순 변수 목록)에 저장합니다.

```sql
FETCH curs1 INTO rowvar;
FETCH curs2 INTO foo, bar, baz;
FETCH LAST FROM curs3 INTO x, y;
FETCH RELATIVE -2 FROM curs4 INTO x;
```

`direction` 절은 SQL의 `FETCH` 명령에서 허용된 모든 변형이 될 수 있습니다:

- `NEXT` (기본값)
- `PRIOR`
- `FIRST`
- `LAST`
- `ABSOLUTE count`
- `RELATIVE count`
- `FORWARD`
- `BACKWARD`

성공적인 페치 후 `FOUND`는 true로 설정되고, 더 이상 행이 없으면 false로 설정됩니다.

#### MOVE

```sql
MOVE [ direction { FROM | IN } ] cursor;
```

`MOVE`는 데이터를 반환하지 않고 커서를 재배치합니다. `FETCH`와 정확히 동일하게 작동하지만 행을 반환하지 않습니다.

```sql
MOVE curs1;
MOVE LAST FROM curs3;
MOVE RELATIVE -2 FROM curs4;
MOVE FORWARD 2 FROM curs4;
```

#### UPDATE/DELETE WHERE CURRENT OF

```sql
UPDATE table SET ... WHERE CURRENT OF cursor;
DELETE FROM table WHERE CURRENT OF cursor;
```

커서가 테이블에 단순히 위치해 있으면 `WHERE CURRENT OF` 조건을 사용하여 가장 최근에 페치된 행을 업데이트하거나 삭제할 수 있습니다.

```sql
UPDATE foo SET dataval = myval WHERE CURRENT OF curs1;
```

#### CLOSE

```sql
CLOSE cursor;
```

`CLOSE`는 열린 커서의 기반이 되는 포털을 닫습니다. 커서가 닫히면 트랜잭션이 끝나기 전에 다시 열 수 있으며, 동일하거나 다른 쿼리에 바인딩될 수 있습니다.

```sql
CLOSE curs1;
```

### 43.7.4 커서를 통해 결과 반환 (Returning Cursors)

PL/pgSQL 함수는 호출자에게 커서를 반환할 수 있습니다. 이는 대량의 행 집합을 함수에서 반환하는 데 유용합니다. 함수는 커서 이름 문자열을 반환해야 합니다.

```sql
CREATE FUNCTION myfunc(refcursor, refcursor) RETURNS SETOF refcursor AS $$
BEGIN
    OPEN $1 FOR SELECT * FROM table_1;
    RETURN NEXT $1;
    OPEN $2 FOR SELECT * FROM table_2;
    RETURN NEXT $2;
END;
$$ LANGUAGE plpgsql;

-- 함수를 호출하려면:
BEGIN;
SELECT * FROM myfunc('a', 'b');
-- 결과:
-- myfunc
--------
-- a
-- b
-- (2 rows)

FETCH ALL FROM a;
FETCH ALL FROM b;
COMMIT;
```

---

## 43.8 프로시저에서의 트랜잭션 관리 (Transaction Management in Procedures)

저장된 프로시저에서(함수가 아닌) `COMMIT` 및 `ROLLBACK` 명령을 사용하여 트랜잭션을 종료하고 새 트랜잭션을 시작할 수 있습니다.

```sql
CREATE PROCEDURE transaction_test1()
LANGUAGE plpgsql
AS $$
BEGIN
    FOR i IN 0..9 LOOP
        INSERT INTO test1 (a) VALUES (i);
        IF i % 2 = 0 THEN
            COMMIT;
        ELSE
            ROLLBACK;
        END IF;
    END LOOP;
END;
$$;

CALL transaction_test1();
```

새로운 트랜잭션은 기본 트랜잭션 특성(예: 격리 수준)으로 시작됩니다. `COMMIT AND CHAIN` 및 `ROLLBACK AND CHAIN` 변형을 사용하면 종료된 트랜잭션과 동일한 트랜잭션 특성을 가진 새 트랜잭션을 시작합니다.

특별한 고려 사항:

- 트랜잭션 제어는 최상위 레벨의 `CALL`에서 호출되거나 중첩된 `CALL` 또는 `DO` 호출에서 직접 호출된 프로시저에서만 사용할 수 있습니다.
- 커서 루프는 트랜잭션 종료로 인해 암묵적으로 닫히지만, `WITH HOLD` 커서는 열린 상태로 유지될 수 있습니다.
- `EXCEPTION` 절이 있는 블록에서는 트랜잭션 제어 명령을 사용할 수 없습니다.

---

## 43.9 오류 및 메시지 (Errors and Messages)

`RAISE` 문을 사용하여 메시지를 보고하고 오류를 발생시킬 수 있습니다.

```sql
RAISE [ level ] 'format' [, expression [, ... ]] [ USING option = expression [, ... ] ];
RAISE [ level ] condition_name [ USING option = expression [, ... ] ];
RAISE [ level ] SQLSTATE 'sqlstate' [ USING option = expression [, ... ] ];
RAISE [ level ] USING option = expression [, ... ];
RAISE ;
```

`level` 옵션은 오류 심각도를 지정합니다. 허용되는 수준은 `DEBUG`, `LOG`, `INFO`, `NOTICE`, `WARNING`, `EXCEPTION`이며, 기본값은 `EXCEPTION`입니다. `EXCEPTION`은 오류를 발생시키고(일반적으로 현재 트랜잭션을 중단), 다른 수준은 다양한 우선순위의 메시지만 생성합니다.

`format` 문자열 내에서 `%`는 다음 선택적 인자 값의 문자열 표현으로 대체됩니다. 리터럴 `%`를 내보내려면 `%%`를 작성합니다. 인자 수는 `format` 문자열의 플레이스홀더 수와 일치해야 합니다.

```sql
RAISE NOTICE 'Calling cs_create_job(%)', v_job_id;
RAISE EXCEPTION '존재하지 않는 ID --> %', user_id
      USING HINT = 'user_id가 올바른지 확인하세요';
```

`USING` 다음에 제공되는 옵션은 다음과 같습니다:

| 옵션 | 설명 |
|------|------|
| `MESSAGE` | 오류 메시지 텍스트 설정 |
| `DETAIL` | 오류 상세 메시지 제공 |
| `HINT` | 힌트 메시지 제공 |
| `ERRCODE` | 오류 코드 지정 |
| `COLUMN`, `CONSTRAINT`, `DATATYPE`, `TABLE`, `SCHEMA` | 관련 객체 이름 제공 |

```sql
RAISE EXCEPTION 'Nonexistent ID --> %', user_id
      USING ERRCODE = 'unique_violation';
-- 또는
RAISE 'Duplicate user ID: %', user_id USING ERRCODE = '23505';
```

매개변수 없이 `RAISE;`를 사용하면 현재 처리 중인 예외가 다시 발생합니다(예외 핸들러에서만 의미가 있음):

```sql
EXCEPTION WHEN OTHERS THEN
    -- 로깅 등 처리
    RAISE;  -- 원래 예외 다시 발생
END;
```

### ASSERT

`ASSERT` 문은 PL/pgSQL 함수에 디버깅 검사를 삽입하는 편리한 방법입니다:

```sql
ASSERT condition [ , message ];
```

`condition`은 항상 true로 평가될 것으로 예상되는 부울 표현식입니다. 그렇다면 `ASSERT` 문은 아무 작업도 수행하지 않습니다. 결과가 false이거나 널이면 `ASSERT_FAILURE` 예외가 발생합니다.

```sql
ASSERT quantity > 0, 'quantity must be positive';
```

> 참고: `ASSERT`는 절대 실패하지 않아야 하는 조건을 검사하기 위한 것입니다; 프로그래머가 실수했음을 나타내며 일반적인 데이터 또는 사용자 오류 보고에는 사용하지 마세요.

---

## 43.10 트리거 함수 (Trigger Functions)

PL/pgSQL은 트리거 함수를 정의하는 데 사용할 수 있습니다. 트리거 함수는 `CREATE FUNCTION` 명령을 사용하여 생성되며, 인자 없이 반환 타입 `trigger`(행 수준 트리거의 경우) 또는 `event_trigger`(이벤트 트리거의 경우)를 반환합니다.

### 43.10.1 행 수준 트리거에 대한 데이터 변경 트리거

트리거 함수가 실행될 때, PL/pgSQL 런타임 환경에서는 자동으로 여러 특수 변수가 생성됩니다:

| 변수 | 설명 |
|------|------|
| `NEW` | `INSERT`/`UPDATE` 작업에 대한 행의 새 데이터베이스 행 (`RECORD` 타입) |
| `OLD` | `UPDATE`/`DELETE` 작업에 대한 행의 이전 데이터베이스 행 (`RECORD` 타입) |
| `TG_NAME` | 실제로 발생된 트리거의 이름 (`name` 타입) |
| `TG_WHEN` | 트리거 정의에 따라 `BEFORE`, `AFTER`, `INSTEAD OF` 중 하나 (`text` 타입) |
| `TG_LEVEL` | 트리거 정의에 따라 `ROW` 또는 `STATEMENT` (`text` 타입) |
| `TG_OP` | 트리거가 발생된 작업: `INSERT`, `UPDATE`, `DELETE`, `TRUNCATE` 중 하나 (`text` 타입) |
| `TG_RELID` | 트리거 호출을 유발한 테이블의 객체 ID (`oid` 타입) |
| `TG_RELNAME` | 트리거 호출을 유발한 테이블의 이름 (`name` 타입, 더 이상 사용되지 않음, `TG_TABLE_NAME` 사용 권장) |
| `TG_TABLE_NAME` | 트리거 호출을 유발한 테이블의 이름 (`name` 타입) |
| `TG_TABLE_SCHEMA` | 트리거 호출을 유발한 테이블의 스키마 (`name` 타입) |
| `TG_NARGS` | `CREATE TRIGGER` 문에서 트리거 함수에 주어진 인자 수 (`integer` 타입) |
| `TG_ARGV[]` | `CREATE TRIGGER` 문의 인자, 인덱스는 0부터 시작 (`text` 배열) |

행 수준 트리거 함수가 반환하는 값은 작업 및 타이밍에 따라 달라집니다:

- `BEFORE` 트리거: `NULL`을 반환하면 현재 행에 대한 작업을 건너뜁니다. 행이 아닌 값을 반환하면 삽입되거나 업데이트될 행으로 사용됩니다.
- `AFTER` 트리거: 반환 값은 무시됩니다.
- `INSTEAD OF` 트리거: `NULL`을 반환하면 뷰의 현재 행에 대한 작업을 건너뜁니다.

```sql
-- 예: 직원 급여가 변경되면 emp_audit 테이블에 항목을 삽입하는 트리거
CREATE TABLE emp (
    empname           text NOT NULL,
    salary            integer
);

CREATE TABLE emp_audit(
    operation         char(1)   NOT NULL,
    stamp             timestamp NOT NULL,
    userid            text      NOT NULL,
    empname           text      NOT NULL,
    salary            integer
);

CREATE OR REPLACE FUNCTION process_emp_audit() RETURNS TRIGGER AS $emp_audit$
    BEGIN
        --
        -- emp_audit에 emp에 대한 작업을 반영하는 행을 만듭니다
        -- 작업 타입을 결정하기 위해 특수 변수 TG_OP를 사용합니다
        --
        IF (TG_OP = 'DELETE') THEN
            INSERT INTO emp_audit SELECT 'D', now(), user, OLD.*;
            RETURN OLD;
        ELSIF (TG_OP = 'UPDATE') THEN
            INSERT INTO emp_audit SELECT 'U', now(), user, NEW.*;
            RETURN NEW;
        ELSIF (TG_OP = 'INSERT') THEN
            INSERT INTO emp_audit SELECT 'I', now(), user, NEW.*;
            RETURN NEW;
        END IF;
        RETURN NULL; -- AFTER 트리거이므로 결과는 무시됨
    END;
$emp_audit$ LANGUAGE plpgsql;

CREATE TRIGGER emp_audit
AFTER INSERT OR UPDATE OR DELETE ON emp
    FOR EACH ROW EXECUTE FUNCTION process_emp_audit();
```

### 43.10.2 문 수준 트리거에 대한 데이터 변경 트리거

문 수준 트리거는 `NEW` 또는 `OLD`를 가지지 않지만, `AFTER` 트리거와 관련된 모든 행에 액세스하려면 전환 테이블(transition tables)을 사용할 수 있습니다.

```sql
CREATE OR REPLACE FUNCTION check_account_update() RETURNS TRIGGER AS $$
DECLARE
    new_balance NUMERIC;
BEGIN
    -- 전환 테이블을 쿼리하여 새 잔액의 합계를 계산
    SELECT SUM(balance) INTO new_balance FROM new_table;

    -- 모든 잔액이 0 이상인지 확인
    IF new_balance < 0 THEN
        RAISE EXCEPTION 'Total balance cannot be negative';
    END IF;

    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER check_balance
AFTER UPDATE ON accounts
REFERENCING NEW TABLE AS new_table
FOR EACH STATEMENT EXECUTE FUNCTION check_account_update();
```

### 43.10.3 이벤트 트리거 (Event Triggers)

이벤트 트리거는 `CREATE TABLE`이나 `DROP INDEX`와 같은 DDL 문이 실행될 때 호출됩니다. 이벤트 트리거 함수의 반환 타입은 `event_trigger`입니다.

이벤트 트리거 함수에서 사용 가능한 특수 변수:

| 변수 | 설명 |
|------|------|
| `TG_EVENT` | 함수가 호출된 이벤트 (`text` 타입) |
| `TG_TAG` | 함수가 호출된 명령 태그 (`text` 타입) |

```sql
CREATE OR REPLACE FUNCTION log_ddl() RETURNS event_trigger AS $$
BEGIN
    RAISE NOTICE 'DDL command: %', TG_TAG;
END;
$$ LANGUAGE plpgsql;

CREATE EVENT TRIGGER log_ddl_trigger
ON ddl_command_end
EXECUTE FUNCTION log_ddl();
```

---

## 43.11 PL/pgSQL 언더 더 후드 (PL/pgSQL Under the Hood)

### 43.11.1 변수 대체 (Variable Substitution)

PL/pgSQL 인터프리터가 함수를 처리할 때 SQL 문과 표현식에서 PL/pgSQL 변수 이름을 찾아 쿼리 매개변수로 대체합니다.

```sql
-- 이 함수에서:
CREATE FUNCTION logfunc(logtxt text) RETURNS void AS $$
    DECLARE
        curtime timestamp := now();
    BEGIN
        INSERT INTO logtable VALUES (logtxt, curtime);
    END;
$$ LANGUAGE plpgsql;

-- INSERT는 다음과 같이 처리됩니다:
INSERT INTO logtable VALUES ($1, $2);
```

### 43.11.2 계획 캐싱 (Plan Caching)

PL/pgSQL 인터프리터는 함수 내의 개별 SQL 문에 대한 실행 계획을 캐시합니다. 계획은 함수가 처음 실행될 때 각 문에 대해 준비되고 이후 실행에서 재사용됩니다.

이로 인해 성능이 크게 향상되지만, 기반이 되는 데이터베이스 객체가 변경되면(예: 인덱스 추가, 테이블 변경) 캐시된 계획이 최적이 아닐 수 있습니다. 이런 경우 PostgreSQL은 자동으로 계획을 무효화합니다.

몇 가지 주의점:

1. 같은 세션 내에서 여러 번 호출되는 함수의 경우 처음 실행이 계획 생성으로 인해 약간 느릴 수 있습니다.
2. 매개변수 값에 따라 최적 계획이 달라질 수 있는 경우 일반 계획(generic plan)이 사용됩니다.

동적 SQL(`EXECUTE`)을 사용하면 매번 계획을 새로 만들므로 이러한 문제를 피할 수 있지만 오버헤드가 발생합니다.

---

## 43.12 개발 팁 (Tips for Developing in PL/pgSQL)

### 디버깅을 위한 인용 사용

PL/pgSQL 코드를 작성할 때 달러 인용(dollar quoting)을 사용하는 것이 좋습니다:

```sql
CREATE OR REPLACE FUNCTION testfunc(integer) RETURNS integer AS $PROC$
....
$PROC$ LANGUAGE plpgsql;
```

`$PROC$`와 같은 태그를 사용하면 중첩된 달러 인용이 가능합니다.

### 추가 검사 활성화

`plpgsql.extra_warnings` 및 `plpgsql.extra_errors` 구성 매개변수를 사용하여 컴파일 타임에 추가 검사를 활성화할 수 있습니다:

```sql
SET plpgsql.extra_warnings TO 'all';
-- 또는
SET plpgsql.extra_warnings TO 'shadowed_variables';
```

사용 가능한 검사:
- `shadowed_variables`: 외부 블록의 변수를 가리는 내부 블록의 변수 선언 경고
- `strict_multi_assignment`: 복수 값을 단일 변수에 할당하는 경우 경고
- `too_many_rows`: `SELECT INTO`가 둘 이상의 행을 반환하는 경우 경고

### RAISE로 디버깅

`RAISE NOTICE` 문은 PL/pgSQL 함수를 디버깅하는 간단하지만 효과적인 방법입니다:

```sql
CREATE FUNCTION debug_example(x integer) RETURNS integer AS $$
DECLARE
    result integer;
BEGIN
    RAISE NOTICE 'Input value: %', x;

    result := x * 2;
    RAISE NOTICE 'After multiplication: %', result;

    result := result + 10;
    RAISE NOTICE 'After addition: %', result;

    RETURN result;
END;
$$ LANGUAGE plpgsql;
```

---

## 요약 (Summary)

PL/pgSQL은 PostgreSQL의 강력한 절차적 언어로, 다음과 같은 주요 기능을 제공합니다:

1. 블록 구조: `DECLARE`, `BEGIN`, `EXCEPTION`, `END`를 사용한 명확한 코드 구조화
2. 변수와 타입: SQL 데이터 타입, `%TYPE`, `%ROWTYPE`, `RECORD` 지원
3. 제어 구조: `IF/ELSIF/ELSE`, `CASE`, `LOOP`, `WHILE`, `FOR`, `FOREACH`
4. 쿼리 통합: SQL 문을 직접 포함하고 결과를 변수에 저장
5. 커서: 대량 데이터 집합의 효율적인 처리
6. 오류 처리: `EXCEPTION` 블록과 `RAISE` 문을 통한 견고한 오류 관리
7. 트리거: 데이터 변경 및 DDL 이벤트에 대한 자동화된 응답
8. 트랜잭션 제어: 프로시저에서의 `COMMIT`/`ROLLBACK` 지원

PL/pgSQL을 사용하면 복잡한 비즈니스 로직을 데이터베이스 내에서 직접 구현하여 클라이언트-서버 통신 오버헤드를 줄이고, 데이터 무결성을 보장하며, 재사용 가능한 코드를 작성할 수 있습니다.
