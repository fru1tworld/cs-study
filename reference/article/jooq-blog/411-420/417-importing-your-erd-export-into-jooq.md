# ERD 내보내기를 jOOQ로 수동 가져오기를 중단하라

> 원문: https://blog.jooq.org/importing-your-erd-export-into-jooq/

ERD(Entity Relationship Diagram, 엔티티 관계 다이어그램)는 데이터베이스 스키마를 설계하고 시각화하는 데 매우 유용한 도구입니다. 무료 및 상용 ERD 솔루션을 제공하는 다양한 벤더들이 있습니다. 그 중 하나가 E-Point의 SaaS 제품인 Vertabelo로, 온라인에서 데이터베이스 스키마를 설계하고 관리할 수 있습니다. 예를 들어, jOOQ 예제 데이터베이스가 이러한 도구로 어떻게 모델링될 수 있는지 살펴볼 수 있습니다.

그러나 이러한 ERD 도구에서 가장 중요한 측면은 가져오기/내보내기 기능입니다. 기존 스키마를 역공학(reverse-engineering)하고 SQL 또는 XML 형식으로 내보낼 수 있습니다. 이 기능은 파일 기반 스키마 정의를 사용하는 INFORMATION_SCHEMA의 XML 표현을 지원하는 jOOQ 3.5에서 특히 유용해집니다.

## XSLT 변환 전략

Vertabelo 내보내기 형식을 jOOQ 가져오기 형식으로 변환하는 것은 XSLT(Extensible Stylesheet Language Transformations)를 사용하여 쉽게 수행할 수 있습니다. 왜냐하면 Vertabelo 내보내기 형식은 계층적으로 구성되어 있고, jOOQ 가져오기 형식은 H2, HSQLDB, MySQL, PostgreSQL, SQL Server와 같은 데이터베이스에서 구현된 표준 SQL INFORMATION_SCHEMA 테이블을 미러링하는 평면적인 XML 표현이기 때문입니다.

## Vertabelo XML 내보내기 형식

Vertabelo의 XML 내보내기 형식은 다음과 같이 구성됩니다:

```xml
<Tables>
    <Table Id="t1">
        <Name>LANGUAGE</Name>
        <Description></Description>
        <Columns>
            <Column Id="c1">
                <Name>ID</Name>
                <Type>integer</Type>
                <Nullable>false</Nullable>
                <PK>true</PK>
            </Column>
            <!-- ... -->
        </Columns>
    </Table>
</Tables>
```

## jOOQ information_schema XML 형식

jOOQ의 가져오기 형식은 다음과 같은 평면적인 구조를 가집니다:

```xml
<information_schema>
    <schemata>
        <schema>
            <schema_name>PUBLIC</schema_name>
        </schema>
    </schemata>
    <tables>
        <table>
            <table_schema>PUBLIC</table_schema>
            <table_name>LANGUAGE</table_name>
        </table>
    </tables>
    <columns>
        <column>
            <table_schema>PUBLIC</table_schema>
            <table_name>LANGUAGE</table_name>
            <column_name>ID</column_name>
            <data_type>integer</data_type>
            <ordinal_position>1</ordinal_position>
            <is_nullable>false</is_nullable>
        </column>
    </columns>
    <table_constraints>
        <table_constraint>
            <constraint_schema>PUBLIC</constraint_schema>
            <constraint_name>PK_LANGUAGE</constraint_name>
            <constraint_type>PRIMARY KEY</constraint_type>
            <table_schema>PUBLIC</table_schema>
            <table_name>LANGUAGE</table_name>
        </table_constraint>
    </table_constraints>
</information_schema>
```

## XSLT 스타일시트

Vertabelo 내보내기를 jOOQ 형식으로 변환하기 위한 XSLT 스타일시트는 다음과 같습니다:

```xml
<xsl:template match="/">
    <!-- 스키마를 고유하게 식별하기 위한 키 정의 -->
    <xsl:key name="schema"
        match="/DatabaseModel/Tables/Table/Properties/Property[Name = 'Schema']" use="." />

    <information_schema xmlns="https://www.jooq.org/xsd/jooq-meta-3.5.0.xsd">
        <schemata>
            <!-- 고유한 스키마만 선택하여 적용 -->
            <xsl:apply-templates
                select="/DatabaseModel/Tables/Table/Properties/Property[Name = 'Schema'][generate-id() = generate-id(key('schema', .)[1])]"
                mode="schema"/>
        </schemata>

        <tables>
            <!-- 모든 테이블에 대해 적용 -->
            <xsl:apply-templates
                select="/DatabaseModel/Tables/Table"
                mode="table"/>
        </tables>

        <columns>
            <!-- 모든 컬럼에 대해 적용 -->
            <xsl:apply-templates
                select="/DatabaseModel/Tables/Table/Columns/Column"
                mode="column"/>
        </columns>
    </information_schema>
</xsl:template>

<!-- 테이블 변환 템플릿 -->
<xsl:template match="Table" mode="table">
    <table>
        <table_schema>
            <xsl:value-of select="Properties/Property[Name = 'Schema']/Value"/>
        </table_schema>
        <table_name>
            <xsl:value-of select="Name"/>
        </table_name>
    </table>
</xsl:template>

<!-- 컬럼 변환 템플릿 -->
<xsl:template match="Column" mode="column">
    <xsl:variable name="Id" select="@Id"/>

    <column>
        <table_schema>
            <!-- 부모 테이블의 스키마 값 가져오기 -->
            <xsl:value-of select="ancestor::Table/Properties/Property[Name = 'Schema']/Value"/>
        </table_schema>
        <table_name>
            <!-- 부모 테이블의 이름 가져오기 -->
            <xsl:value-of select="ancestor::Table/Name"/>
        </table_name>
        <column_name>
            <xsl:value-of select="Name"/>
        </column_name>
        <data_type>
            <xsl:value-of select="Type"/>
        </data_type>
        <ordinal_position>
            <!-- 컬럼의 순서 위치 계산 (1부터 시작) -->
            <xsl:value-of select="1 + count(preceding-sibling::Column)"/>
        </ordinal_position>
        <is_nullable>
            <xsl:value-of select="Nullable"/>
        </is_nullable>
    </column>
</xsl:template>
```

## Maven 구성

### xml-maven-plugin 구성

변환 프로세스는 Vertabelo 내보내기 파일을 `src/main/resources`에 배치하고 Codehaus xml-maven-plugin을 사용하여 XSL 스타일시트를 통해 변환하는 것을 포함합니다. 생성된 출력은 target 디렉토리에서 jOOQ 코드 생성기가 사용할 수 있게 됩니다.

```xml
<plugin>
    <groupId>org.codehaus.mojo</groupId>
    <artifactId>xml-maven-plugin</artifactId>
    <executions>
        <execution>
            <phase>generate-sources</phase>
            <goals>
                <goal>transform</goal>
            </goals>
        </execution>
    </executions>
    <configuration>
        <transformationSets>
            <transformationSet>
                <dir>src/main/resources</dir>
                <includes>
                    <include>vertabelo-export.xml</include>
                </includes>
                <stylesheet>src/main/resources/vertabelo-2-jooq.xsl</stylesheet>
            </transformationSet>
        </transformationSets>
    </configuration>
</plugin>
```

### jooq-codegen-maven 구성

jOOQ 코드 생성기는 XMLDatabase 프로바이더를 사용하여 변환된 출력을 처리합니다:

```xml
<plugin>
    <groupId>org.jooq</groupId>
    <artifactId>jooq-codegen-maven</artifactId>
    <version>${org.jooq.version}</version>

    <executions>
        <execution>
            <id>generate-h2</id>
            <phase>generate-sources</phase>
            <goals>
                <goal>generate</goal>
            </goals>
            <configuration>
                <generator>
                    <name>org.jooq.util.DefaultGenerator</name>
                    <database>
                        <name>org.jooq.util.xml.XMLDatabase</name>
                        <properties>
                            <property>
                                <key>dialect</key>
                                <value>H2</value>
                            </property>
                            <property>
                                <!-- 변환된 XML 파일의 경로 지정 -->
                                <key>xml-file</key>
                                <value>target/generated-resources/xml/xslt/vertabelo-export.xml</value>
                            </property>
                        </properties>
                        <inputSchema>PUBLIC</inputSchema>
                    </database>
                    <generate>
                        <deprecated>false</deprecated>
                        <instanceFields>true</instanceFields>
                    </generate>
                    <target>
                        <packageName>org.jooq.example.db.h2</packageName>
                        <directory>target/generated-sources/jooq-h2</directory>
                    </target>
                </generator>
            </configuration>
        </execution>
    </executions>
</plugin>
```

## 구현 단계

워크플로우는 Maven의 표준 빌드 라이프사이클에 원활하게 통합되어 코드 생성이 발생하기 전에 `generate-sources` 단계에서 실행됩니다:

1. ERD 내보내기 파일 배치: `src/main/resources`에 파일을 배치합니다.
2. xml-maven-plugin 구성: 빌드 프로세스 중에 XSLT 변환을 적용하도록 구성합니다.
3. jOOQ 코드 생성기 사용: XMLDatabase 프로바이더와 함께 jOOQ 코드 생성기를 사용하여 변환된 출력을 처리합니다.

## 결론

이 접근 방식은 Vertabelo 외에도 다양한 ERD 도구에서 작동합니다. 사용자는 특정 도구에 맞게 변환을 조정하기 위해 다음 위치에 있는 jOOQ 메타 스키마 사양을 준수하는 유효한 XML을 생성하는 XSLT 파일만 작성하면 됩니다:

`https://www.jooq.org/xsd/jooq-meta-3.5.0.xsd`

XSLT 스타일시트는 Vertabelo 요소(테이블 이름, 컬럼 타입, nullable 플래그, 기본 키)를 jOOQ가 기대하는 스키마 구조로 매핑합니다. 이를 통해 ERD 도구에서 내보낸 스키마를 수동으로 jOOQ에 가져오는 지루하고 오류가 발생하기 쉬운 작업을 자동화할 수 있습니다.
