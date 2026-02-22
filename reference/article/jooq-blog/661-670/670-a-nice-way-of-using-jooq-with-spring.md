# Spring과 함께 jOOQ를 사용하는 좋은 방법

> 원문: https://blog.jooq.org/a-nice-way-of-using-jooq-with-spring/

_이 블로그 글은 더 이상 유효하지 않습니다. jOOQ와 Spring을 통합하는 최신 예제는 jOOQ 매뉴얼의 관련 섹션을 참고하세요!_

Spring과 함께 jOOQ를 사용하는 좋은 방법이 최근 Stack Overflow에서 Adam Gent에 의해 논의되었습니다: http://adamgent.com/post/31128631472/getting-jooq-to-work-with-spring-correctly

그 핵심 내용은 다음 gist에서 제공되었습니다:

```java
package com.snaphop.jooq;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.jooq.ExecuteContext;
import org.jooq.impl.DefaultExecuteListener;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator;
import org.springframework.jdbc.support.SQLExceptionTranslator;
import org.springframework.jdbc.support.SQLStateSQLExceptionTranslator;

/
 * Adam Gent가 제공한 예제
 */
public class SpringExceptionTranslationExecuteListener
extends DefaultExecuteListener {

    @Override
    public void start(ExecuteContext ctx) {
        DataSource dataSource = ctx.getDataSource();
        Connection c = DataSourceUtils.getConnection(dataSource);
        ctx.setConnection(c);
    }

    @Override
    public void exception(ExecuteContext ctx) {
        SQLException ex = ctx.sqlException();
        Statement stmt = ctx.statement();
        Connection con = ctx.getConnection();
        DataSource dataSource = ctx.getDataSource();
        // 이 주석과 아래 코드는
        // JdbcTemplate.execute(StatementCallback)에서 가져왔습니다
        // 예외 변환기가 아직 초기화되지 않은 경우에
        // 잠재적인 커넥션 풀 데드락을 피하기 위해
        // Connection을 일찍 해제합니다.
        JdbcUtils.closeStatement(stmt);
        stmt = null;
        DataSourceUtils.releaseConnection(con, dataSource);
        con = null;
        ctx.exception(getExceptionTranslator(dataSource)
                        .translate("jOOQ", ctx.sql(), ex));
    }

    /
     * 이 인스턴스에 대한 예외 변환기를 반환합니다.
     *
     * DataSource가 없는 경우 {@link SQLStateSQLExceptionTranslator}를,
     * 지정된 DataSource가 있는 경우 기본
     * {@link SQLErrorCodeSQLExceptionTranslator}를 생성합니다.
     * @see #getDataSource()
     */
    public synchronized SQLExceptionTranslator
    getExceptionTranslator(DataSource dataSource) {
        // 이 메서드는 아마도 synchronized가 필요하지 않을 수 있지만
        // Spring에서는 JdbcTemplate의 가변 필드 때문에 그랬습니다.
        // 또한 변환기를 생성하는 비용이 얼마나 드는지 모르겠지만
        // 예외가 발생할 때마다 하나가 생성될 것입니다.
        final SQLExceptionTranslator exceptionTranslator;
        if (dataSource != null) {
            exceptionTranslator =
                new SQLErrorCodeSQLExceptionTranslator(dataSource);
        }
        else {
            exceptionTranslator = new SQLStateSQLExceptionTranslator();
        }
        return exceptionTranslator;
    }
}
```

더 자세한 내용은 관련 Stack Overflow 답변을 참고하세요: https://stackoverflow.com/a/12326885/521799

## 댓글

Peter Ertl 작성:
2013년 1월 9일 18:39

"훨씬 더 쉬워 보이는 제가 이 글에 추가한 답변도 확인해 보세요: https://stackoverflow.com/a/14243136/1964204"

lukaseder 작성:
2013년 1월 10일 00:43

"훌륭해요, Peter, 감사합니다! 이것은 Sergey Epik이 jOOQ 사용자 그룹에서 언급했던 것과 잘 맞는 것 같습니다: https://groups.google.com/d/msg/jooq-user/TYUg6XjPYlk/VfxS5TFokBgJ

다만 당신의 솔루션은 설정적인 것이 아니라 프로그래밍적인 것입니다. 게다가 Sergey의 솔루션보다 더 간결해 보이네요..."
