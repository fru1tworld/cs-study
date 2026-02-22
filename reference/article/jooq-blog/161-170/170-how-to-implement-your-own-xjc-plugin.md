# toString(), equals(), hashCode() 메서드를 생성하기 위한 나만의 XJC 플러그인 구현 방법

> 원문: https://blog.jooq.org/how-to-implement-your-own-xjc-plugin-to-generate-tostring-equals-and-hashcode-methods/

이 글에서는 JAXB로 생성된 클래스에 `toString()`, `equals()`, `hashCode()` 메서드를 생성하는 커스텀 XJC(XML Java Compiler) 플러그인을 만드는 방법을 설명합니다.

## 프로젝트 설정

단일 의존성을 가진 Maven 프로젝트를 생성합니다:

```xml
<project xmlns="https://maven.apache.org/POM/4.0.0"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="https://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">

    <modelVersion>4.0.0</modelVersion>

    <groupId>org.jooq</groupId>
    <artifactId>jooq-tools-xjc-plugin</artifactId>
    <version>3.11.0-SNAPSHOT</version>
    <name>jOOQ XJC Code Generation Plugin</name>

    <dependencies>
        <dependency>
            <groupId>com.sun.xml.bind</groupId>
            <artifactId>jaxb-xjc</artifactId>
            <version>2.3.0</version>
        </dependency>
    </dependencies>
</project>
```

## 기본 플러그인 구조

빈 플러그인은 `Plugin` 클래스를 상속해야 합니다:

```java
package org.jooq.xjc;

import org.xml.sax.ErrorHandler;

import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.Plugin;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.Outline;

/
 * @author Lukas Eder
 */
public class XJCPlugin extends Plugin {

    @Override
    public String getOptionName() {
        return "Xjooq-equals-hashcode-tostring";
    }

    @Override
    public int parseArgument(Options opt, String[] args, int i) {
        return 1;
    }

    @Override
    public String getUsage() {
        return "  -Xjooq-equals-hashcode-tostring    :  xjc plugin";
    }

    @Override
    public boolean run(Outline model, Options opt, ErrorHandler errorHandler) {
        return true;
    }
}
```

주요 메서드로는 `getOptionName()`(활성화 플래그 제공)과 `run()`(코드 생성 로직 포함)이 있습니다.

## 전체 구현

메서드 생성을 포함한 전체 플러그인 구현:

```java
package org.jooq.xjc;

import static com.sun.codemodel.JMod.FINAL;
import static com.sun.codemodel.JMod.PUBLIC;
import static com.sun.codemodel.JMod.STATIC;

import java.util.Map.Entry;

import org.xml.sax.ErrorHandler;

import com.sun.codemodel.JBlock;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JConditional;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JOp;
import com.sun.codemodel.JVar;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.Plugin;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.Outline;

/
 * @author Lukas Eder
 */
public class XJCPlugin extends Plugin {

    @Override
    public String getOptionName() {
        return "Xjooq-equals-hashcode-tostring";
    }

    @Override
    public int parseArgument(Options opt, String[] args, int i) {
        return 1;
    }

    @Override
    public String getUsage() {
        return "  -Xjooq-equals-hashcode-tostring    :  xjc example plugin";
    }

    @Override
    public boolean run(Outline model, Options opt, ErrorHandler errorHandler) {
        JCodeModel m = new JCodeModel();

        for (ClassOutline o : model.getClasses()) {

            // toString()
            // ---------------------------------------------------------------------------
            {
                JMethod method = o.implClass.method(PUBLIC, String.class, "toString");
                method.annotate(Override.class);
                JBlock body = method.body();
                JClass sbType = m.ref(StringBuilder.class);
                JVar sb = body.decl(0, sbType, "sb", JExpr._new(sbType));

                for (Entry<String, JFieldVar> e : o.implClass.fields().entrySet()) {
                    JFieldVar v = e.getValue();

                    if ((v.mods().getValue() & STATIC) == 0) {
                        body.invoke(sb, "append").arg("<" + e.getKey() + ">");
                        body.invoke(sb, "append").arg(v);
                        body.invoke(sb, "append").arg("</" + e.getKey() + ">");
                    }
                }

                body._return(JExpr.invoke(sb, "toString"));
            }

            // equals()
            // ---------------------------------------------------------------------------
            {
                JMethod method = o.implClass.method(PUBLIC, boolean.class, "equals");
                method.annotate(Override.class);
                JVar that = method.param(Object.class, "that");
                JBlock body = method.body();
                body._if(JExpr._this().eq(that))
                    ._then()._return(JExpr.lit(true));
                body._if(that.eq(JExpr._null()))
                    ._then()._return(JExpr.lit(false));
                body._if(JExpr.invoke("getClass").ne(JExpr.invoke(that, "getClass")))
                    ._then()._return(JExpr.lit(false));

                JVar other = body.decl(0, o.implClass, "other", JExpr.cast(o.implClass, that));

                for (Entry<String, JFieldVar> e : o.implClass.fields().entrySet()) {
                    JFieldVar v = e.getValue();

                    if ((v.mods().getValue() & STATIC) == 0) {
                        if (v.type().isPrimitive()) {
                            body._if(v.ne(other.ref(v)))
                                ._then()._return(JExpr.lit(false));
                        }
                        else {
                            JConditional i = body._if(v.eq(JExpr._null()));
                            i._then()._if(other.ref(v).ne(JExpr._null()))
                                     ._then()._return(JExpr.lit(false));
                            i._elseif(v.invoke("equals").arg(other.ref(v)).not())
                             ._then()._return(JExpr.lit(false));
                        }
                    }
                }

                body._return(JExpr.lit(true));
            }

            // hashCode()
            {
                JMethod method = o.implClass.method(PUBLIC, int.class, "hashCode");
                method.annotate(Override.class);
                JBlock body = method.body();
                JVar prime = body.decl(FINAL, m.INT, "prime", JExpr.lit(31));
                JVar result = body.decl(0, m.INT, "result", JExpr.lit(1));

                for (Entry<String, JFieldVar> e : o.implClass.fields().entrySet()) {
                    JFieldVar v = e.getValue();

                    if ((v.mods().getValue() & STATIC) == 0) {
                        body.assign(result, prime.mul(result).plus(
                            v.type().isPrimitive()
                          ? v
                          : JOp.cond(v.eq(JExpr._null()), JExpr.lit(0), v.invoke("hashCode"))
                        ));
                    }
                }

                body._return(result);
            }
        }

        return true;
    }
}
```

## 플러그인 등록

`src/main/resources/META-INF/services/com.sun.tools.xjc.Plugin` 경로에 서비스 파일을 생성하고 다음 내용을 포함합니다:

```
org.jooq.xjc.XJCPlugin
```

그런 다음 빌드합니다: `mvn clean install`

## 사용 설정

Maven 프로젝트의 POM 파일에서 다음과 같이 설정합니다:

```xml
<plugin>
    <groupId>org.jvnet.jaxb2.maven2</groupId>
    <artifactId>maven-jaxb2-plugin</artifactId>
    <version>0.13.1</version>
    <executions>
        <execution>
            <id>codegen</id>
            <goals>
                <goal>generate</goal>
            </goals>
            <configuration>
                <encoding>UTF-8</encoding>
                <locale>us</locale>
                <forceRegenerate>true</forceRegenerate>
                <extension>true</extension>
                <strict>false</strict>
                <schemaDirectory>../jOOQ-meta/src/main/resources/xsd</schemaDirectory>
                <bindingDirectory>../jOOQ-meta/src/main/resources/xjb/codegen</bindingDirectory>
                <generateDirectory>../jOOQ-meta/src/main/java</generateDirectory>
                <generatePackage>org.jooq.util.jaxb</generatePackage>
                <schemaIncludes>
                    <include>jooq-codegen-3.11.0.xsd</include>
                </schemaIncludes>

                <args>
                    <arg>-Xjooq-equals-hashcode-tostring</arg>
                </args>
                <plugins>
                    <plugin>
                        <groupId>org.jooq.trial</groupId>
                        <artifactId>jooq-tools-xjc-plugin</artifactId>
                        <version>3.11.0-SNAPSHOT</version>
                    </plugin>
                </plugins>
            </configuration>
        </execution>
    </executions>
</plugin>
```

## 생성된 출력 예시

이 플러그인은 JAXB 클래스에 대해 XML 형식의 `toString()`, null 검사가 포함된 견고한 `equals()`, 그리고 소수 기반의 `hashCode()` 구현을 생성합니다.
