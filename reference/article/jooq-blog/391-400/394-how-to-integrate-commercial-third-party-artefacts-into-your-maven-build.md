# 상용 서드파티 아티팩트를 Maven 빌드에 통합하는 방법

> 원문: https://blog.jooq.org/how-to-integrate-commercial-third-party-artefacts-into-your-maven-build/

ZeroTurnaround의 RebelLabs가 진행한 최근 설문조사에 따르면, Maven은 64%의 시장 점유율로 여전히 Java 빌드 플랫폼 1위를 차지하고 있으며, Ant + Ivy가 16.5%, Gradle이 11%로 그 뒤를 따르고 있다. Maven은 때때로 경직되고 융통성이 없다고 비판받지만, Gradle이나 Ant가 제공하는 대부분의 유연성은 어차피 필요하지 않은 경우가 많으며, 아티팩트 관리를 직접 하고 싶지 않다면 더욱 그렇다.

## 서드파티 상용 아티팩트 통합하기

의존하고자 하는 모든 서드파티 아티팩트가 Maven Central에서 무료로 제공되는 것은 아니다. 이러한 예로는 상용 JDBC 드라이버나 jOOQ 상용 에디션이 있다.

이러한 아티팩트를 빌드에 통합하는 방법은 본질적으로 세 가지가 있다. 종종 상용 의존성은 작은 테스트 프로젝트나 데모에서만 필요하며, 로컬 저장소 설정이나 네트워크 연결에 의존하지 않고 실행할 수 있도록 보장하고 싶을 것이다.

### Quick-and-Dirty 방식

이것은 `<scope>system</scope>`을 사용하기에 좋은 사례다. 다음은 몇 가지 jOOQ 의존성과 Microsoft의 SQL JDBC 드라이버 의존성을 예로 든 것이다:

```xml
<dependency>
  <groupId>org.jooq</groupId>
  <artifactId>jooq</artifactId>
  <version>${jooq.version}</version>
  <scope>system</scope>
  <systemPath>${basedir}/lib/jooq-${jooq.version}.jar</systemPath>
</dependency>
```

```xml
<dependency>
  <groupId>com.microsoft.sqlserver</groupId>
  <artifactId>sqljdbc4</artifactId>
  <version>3.0</version>
  <scope>system</scope>
  <systemPath>${basedir}/lib/sqljdbc4.jar</systemPath>
  <optional>true</optional>
</dependency>
```

상용 JDBC 드라이버 의존성에 여전히 `optional`을 설정할 수 있다는 점에 주목하라.

장점: 이 방식의 장점은, 라이브러리를 먼저 소스 컨트롤에 체크인해 두면, 로컬에서 자체 완결적인 모듈이 추가 설정이나 셋업 없이 소스 컨트롤에서 체크아웃한 직후 바로 실행될 수 있도록 보장된다는 것이다.

단점: 시스템 의존성은 절대 전이적으로 상속되지 않는다. 모듈이 이 방식으로 jOOQ에 의존하면, 해당 모듈의 의존성들은 jOOQ API를 볼 수 없다.

### 조금 더 견고한 방식

조금 더 견고해 보이는 접근 방식은 소스 컨트롤에서 의존성을 체크아웃한 다음 "수동으로" 로컬 저장소에 가져오는 것이다. 이렇게 하면 자신의 로컬 빌드에서 사용할 수 있게 된다.

다음 Windows Batch 스크립트(이 글의 끝에 Bash/shell 버전도 있음)는 jOOQ 아티팩트를 로컬 저장소에 가져오는 방법을 보여준다:

```batch
@echo off
set VERSION=3.4.4

if exist jOOQ-javadoc\\jooq-%VERSION%-javadoc.jar (
  set JAVADOC_JOOQ=-Djavadoc=jOOQ-javadoc\\jooq-%VERSION%-javadoc.jar
  set JAVADOC_JOOQ_META=-Djavadoc=jOOQ-javadoc\\jooq-meta-%VERSION%-javadoc.jar
  set JAVADOC_JOOQ_CODEGEN=-Djavadoc=jOOQ-javadoc\\jooq-codegen-%VERSION%-javadoc.jar
  set JAVADOC_JOOQ_CODEGEN_MAVEN=-Djavadoc=jOOQ-javadoc\\jooq-codegen-maven-%VERSION%-javadoc.jar
  set JAVADOC_JOOQ_SCALA=-Djavadoc=jOOQ-javadoc\\jooq-scala-%VERSION%-javadoc.jar
)

if exist jOOQ-src\\jooq-%VERSION%-sources.jar (
  set SOURCES_JOOQ=-Dsources=jOOQ-src\\jooq-%VERSION%-sources.jar
  set SOURCES_JOOQ_META=-Dsources=jOOQ-src\\jooq-meta-%VERSION%-sources.jar
  set SOURCES_JOOQ_CODEGEN=-Dsources=jOOQ-src\\jooq-codegen-%VERSION%-sources.jar
  set SOURCES_JOOQ_CODEGEN_MAVEN=-Dsources=jOOQ-src\\jooq-codegen-maven-%VERSION%-sources.jar
  set SOURCES_JOOQ_SCALA=-Dsources=jOOQ-src\\jooq-scala-%VERSION%-sources.jar
)

call mvn install:install-file -Dfile=jOOQ-pom\\pom.xml -DgroupId=org.jooq -DartifactId=jooq-parent -Dversion=%VERSION% -Dpackaging=pom

call mvn install:install-file -Dfile=jOOQ-lib\\jooq-%VERSION%.jar -DgroupId=org.jooq -DartifactId=jooq -Dversion=%VERSION% -Dpackaging=jar %JAVADOC_JOOQ% %SOURCES_JOOQ% -DpomFile=jOOQ-pom\\jooq\\pom.xml

call mvn install:install-file -Dfile=jOOQ-lib\\jooq-meta-%VERSION%.jar -DgroupId=org.jooq -DartifactId=jooq-meta -Dversion=%VERSION% -Dpackaging=jar %JAVADOC_JOOQ_META% %SOURCES_JOOQ_META% -DpomFile=jOOQ-pom\\jooq-meta\\pom.xml

call mvn install:install-file -Dfile=jOOQ-lib\\jooq-codegen-%VERSION%.jar -DgroupId=org.jooq -DartifactId=jooq-codegen -Dversion=%VERSION% -Dpackaging=jar %JAVADOC_JOOQ_CODEGEN% %SOURCES_JOOQ_CODEGEN% -DpomFile=jOOQ-pom\\jooq-codegen\\pom.xml

call mvn install:install-file -Dfile=jOOQ-lib\\jooq-codegen-maven-%VERSION%.jar -DgroupId=org.jooq -DartifactId=jooq-codegen-maven -Dversion=%VERSION% -Dpackaging=jar %JAVADOC_JOOQ_CODEGEN_MAVEN% %SOURCES_JOOQ_CODEGEN_META% -DpomFile=jOOQ-pom\\jooq-codegen-maven\\pom.xml

call mvn install:install-file -Dfile=jOOQ-lib\\jooq-scala-%VERSION%.jar -DgroupId=org.jooq -DartifactId=jooq-scala -Dversion=%VERSION% -Dpackaging=jar %JAVADOC_JOOQ_SCALA% %SOURCES_JOOQ_SCALA% -DpomFile=jOOQ-pom\\jooq-scala\\pom.xml
```

위 스크립트를 통해 다음과 같이 의존성을 선언할 수 있다:

```xml
<dependency>
  <groupId>org.jooq</groupId>
  <artifactId>jooq</artifactId>
  <version>${jooq.version}</version>
</dependency>
```

위 스크립트는 다음을 수행한다:

1. javadoc이 있는지 확인하고, 있으면 로딩한다
2. 소스가 있는지 확인하고, 있으면 로딩한다
3. jOOQ 상위 pom을 설치한다
4. jOOQ 바이너리, javadoc, 소스를 설치한다
5. jOOQ-meta 바이너리, javadoc, 소스를 설치한다
6. jOOQ-codegen 바이너리, javadoc, 소스를 설치한다
7. jOOQ-codegen-maven 바이너리, javadoc, 소스를 설치한다
8. jOOQ-scala 바이너리, javadoc, 소스를 설치한다

장점: 의존성이 로컬 저장소에 등록되면 모듈 자체의 의존성에서도 전이적으로 사용할 수 있다.

단점: 빌드가 작동하기 전에 몇 가지 추가 설정 단계를 수행해야 한다.

다음은 동일한 스크립트의 Bash/shell 버전이다:

```bash
#!/bin/sh
VERSION=3.4.4

if [ -f jOOQ-javadoc/jooq-$VERSION-javadoc.jar ]; then
  JAVADOC_JOOQ=-Djavadoc=jOOQ-javadoc/jooq-$VERSION-javadoc.jar
  JAVADOC_JOOQ_META=-Djavadoc=jOOQ-javadoc/jooq-meta-$VERSION-javadoc.jar
  JAVADOC_JOOQ_CODEGEN=-Djavadoc=jOOQ-javadoc/jooq-codegen-$VERSION-javadoc.jar
  JAVADOC_JOOQ_CODEGEN_MAVEN=-Djavadoc=jOOQ-javadoc/jooq-codegen-maven-$VERSION-javadoc.jar
  JAVADOC_JOOQ_SCALA=-Djavadoc=jOOQ-javadoc/jooq-scala-$VERSION-javadoc.jar
fi

if [ -f jOOQ-src/jooq-$VERSION-sources.jar ]; then
  SOURCES_JOOQ=-Dsources=jOOQ-src/jooq-$VERSION-sources.jar
  SOURCES_JOOQ_META=-Dsources=jOOQ-src/jooq-meta-$VERSION-sources.jar
  SOURCES_JOOQ_CODEGEN=-Dsources=jOOQ-src/jooq-codegen-$VERSION-sources.jar
  SOURCES_JOOQ_CODEGEN_MAVEN=-Dsources=jOOQ-src/jooq-codegen-maven-$VERSION-sources.jar
  SOURCES_JOOQ_SCALA=-Dsources=jOOQ-src/jooq-scala-$VERSION-sources.jar
fi

mvn install:install-file -Dfile=jOOQ-pom/pom.xml -DgroupId=org.jooq -DartifactId=jooq-parent -Dversion=$VERSION -Dpackaging=pom

mvn install:install-file -Dfile=jOOQ-lib/jooq-$VERSION.jar -DgroupId=org.jooq -DartifactId=jooq -Dversion=$VERSION -Dpackaging=jar $JAVADOC_JOOQ $SOURCES_JOOQ -DpomFile=jOOQ-pom/jooq/pom.xml

mvn install:install-file -Dfile=jOOQ-lib/jooq-meta-$VERSION.jar -DgroupId=org.jooq -DartifactId=jooq-meta -Dversion=$VERSION -Dpackaging=jar $JAVADOC_JOOQ_META $SOURCES_JOOQ_META -DpomFile=jOOQ-pom/jooq-meta/pom.xml

mvn install:install-file -Dfile=jOOQ-lib/jooq-codegen-$VERSION.jar -DgroupId=org.jooq -DartifactId=jooq-codegen -Dversion=$VERSION -Dpackaging=jar $JAVADOC_JOOQ_CODEGEN $SOURCES_JOOQ_CODEGEN -DpomFile=jOOQ-pom/jooq-codegen/pom.xml

mvn install:install-file -Dfile=jOOQ-lib/jooq-codegen-maven-$VERSION.jar -DgroupId=org.jooq -DartifactId=jooq-codegen-maven -Dversion=$VERSION -Dpackaging=jar $JAVADOC_JOOQ_CODEGEN_MAVEN $SOURCES_JOOQ_CODEGEN_META -DpomFile=jOOQ-pom/jooq-codegen-maven/pom.xml

mvn install:install-file -Dfile=jOOQ-lib/jooq-scala-$VERSION.jar -DgroupId=org.jooq -DartifactId=jooq-scala -Dversion=$VERSION -Dpackaging=jar $JAVADOC_JOOQ_SCALA $SOURCES_JOOQ_SCALA -DpomFile=jOOQ-pom/jooq-scala/pom.xml
```

### 배포하기

다음은 Windows Batch로 로컬 저장소 대신 원격 저장소에 아티팩트를 배포하는 동일한 스크립트다:

```batch
@echo off
set VERSION=3.4.4
set REPOSITORY=[settings.xml에 있는 저장소 이름을 여기에 설정하세요]
set URL=[서버 URL을 여기에 설정하세요]

if exist jOOQ-javadoc\\jooq-%VERSION%-javadoc.jar (
  set JAVADOC_JOOQ=-Djavadoc=jOOQ-javadoc\\jooq-%VERSION%-javadoc.jar
  set JAVADOC_JOOQ_META=-Djavadoc=jOOQ-javadoc\\jooq-meta-%VERSION%-javadoc.jar
  set JAVADOC_JOOQ_CODEGEN=-Djavadoc=jOOQ-javadoc\\jooq-codegen-%VERSION%-javadoc.jar
  set JAVADOC_JOOQ_CODEGEN_MAVEN=-Djavadoc=jOOQ-javadoc\\jooq-codegen-maven-%VERSION%-javadoc.jar
  set JAVADOC_JOOQ_SCALA=-Djavadoc=jOOQ-javadoc\\jooq-scala-%VERSION%-javadoc.jar
)

if exist jOOQ-src\\jooq-%VERSION%-sources.jar (
  set SOURCES_JOOQ=-Dsources=jOOQ-src\\jooq-%VERSION%-sources.jar
  set SOURCES_JOOQ_META=-Dsources=jOOQ-src\\jooq-meta-%VERSION%-sources.jar
  set SOURCES_JOOQ_CODEGEN=-Dsources=jOOQ-src\\jooq-codegen-%VERSION%-sources.jar
  set SOURCES_JOOQ_CODEGEN_MAVEN=-Dsources=jOOQ-src\\jooq-codegen-maven-%VERSION%-sources.jar
  set SOURCES_JOOQ_SCALA=-Dsources=jOOQ-src\\jooq-scala-%VERSION%-sources.jar
)

call mvn deploy:deploy-file -Dfile=jOOQ-pom\\pom.xml -DgroupId=org.jooq -DartifactId=jooq-parent -DrepositoryId=%REPOSITORY% -Durl=%URL% -Dversion=%VERSION% -Dpackaging=pom

call mvn deploy:deploy-file -Dfile=jOOQ-lib\\jooq-%VERSION%.jar -DgroupId=org.jooq -DartifactId=jooq -DrepositoryId=%REPOSITORY% -Durl=%URL% -Dversion=%VERSION% -Dpackaging=jar %JAVADOC_JOOQ% %SOURCES_JOOQ% -DpomFile=jOOQ-pom\\jooq\\pom.xml

call mvn deploy:deploy-file -Dfile=jOOQ-lib\\jooq-meta-%VERSION%.jar -DgroupId=org.jooq -DartifactId=jooq-meta -DrepositoryId=%REPOSITORY% -Durl=%URL% -Dversion=%VERSION% -Dpackaging=jar %JAVADOC_JOOQ_META% %SOURCES_JOOQ_META% -DpomFile=jOOQ-pom\\jooq-meta\\pom.xml

call mvn deploy:deploy-file -Dfile=jOOQ-lib\\jooq-codegen-%VERSION%.jar -DgroupId=org.jooq -DartifactId=jooq-codegen -DrepositoryId=%REPOSITORY% -Durl=%URL% -Dversion=%VERSION% -Dpackaging=jar %JAVADOC_JOOQ_CODEGEN% %SOURCES_JOOQ_CODEGEN% -DpomFile=jOOQ-pom\\jooq-codegen\\pom.xml

call mvn deploy:deploy-file -Dfile=jOOQ-lib\\jooq-codegen-maven-%VERSION%.jar -DgroupId=org.jooq -DartifactId=jooq-codegen-maven -DrepositoryId=%REPOSITORY% -Durl=%URL% -Dversion=%VERSION% -Dpackaging=jar %JAVADOC_JOOQ_CODEGEN_MAVEN% %SOURCES_JOOQ_CODEGEN_META% -DpomFile=jOOQ-pom\\jooq-codegen-maven\\pom.xml

call mvn deploy:deploy-file -Dfile=jOOQ-lib\\jooq-scala-%VERSION%.jar -DgroupId=org.jooq -DartifactId=jooq-scala -DrepositoryId=%REPOSITORY% -Durl=%URL% -Dversion=%VERSION% -Dpackaging=jar %JAVADOC_JOOQ_SCALA% %SOURCES_JOOQ_SCALA% -DpomFile=jOOQ-pom\\jooq-scala\\pom.xml
```

다음은 동일한 스크립트의 Bash/shell 버전이다:

```bash
#!/bin/sh
VERSION=3.4.4
REPOSITORY=[settings.xml에 있는 저장소 이름을 여기에 설정하세요]
URL=[서버 URL을 여기에 설정하세요]

if [ -f jOOQ-javadoc/jooq-$VERSION-javadoc.jar ]; then
  JAVADOC_JOOQ=-Djavadoc=jOOQ-javadoc/jooq-$VERSION-javadoc.jar
  JAVADOC_JOOQ_META=-Djavadoc=jOOQ-javadoc/jooq-meta-$VERSION-javadoc.jar
  JAVADOC_JOOQ_CODEGEN=-Djavadoc=jOOQ-javadoc/jooq-codegen-$VERSION-javadoc.jar
  JAVADOC_JOOQ_CODEGEN_MAVEN=-Djavadoc=jOOQ-javadoc/jooq-codegen-maven-$VERSION-javadoc.jar
  JAVADOC_JOOQ_SCALA=-Djavadoc=jOOQ-javadoc/jooq-scala-$VERSION-javadoc.jar
fi

if [ -f jOOQ-src/jooq-$VERSION-sources.jar ]; then
  SOURCES_JOOQ=-Dsources=jOOQ-src/jooq-$VERSION-sources.jar
  SOURCES_JOOQ_META=-Dsources=jOOQ-src/jooq-meta-$VERSION-sources.jar
  SOURCES_JOOQ_CODEGEN=-Dsources=jOOQ-src/jooq-codegen-$VERSION-sources.jar
  SOURCES_JOOQ_CODEGEN_MAVEN=-Dsources=jOOQ-src/jooq-codegen-maven-$VERSION-sources.jar
  SOURCES_JOOQ_SCALA=-Dsources=jOOQ-src/jooq-scala-$VERSION-sources.jar
fi

mvn deploy:deploy-file -Dfile=jOOQ-pom/pom.xml -DgroupId=org.jooq -DartifactId=jooq-parent -DrepositoryId=$REPOSITORY -Durl=$URL -Dversion=$VERSION -Dpackaging=pom

mvn deploy:deploy-file -Dfile=jOOQ-lib/jooq-$VERSION.jar -DgroupId=org.jooq -DartifactId=jooq -DrepositoryId=$REPOSITORY -Durl=$URL -Dversion=$VERSION -Dpackaging=jar $JAVADOC_JOOQ $SOURCES_JOOQ -DpomFile=jOOQ-pom/jooq/pom.xml

mvn deploy:deploy-file -Dfile=jOOQ-lib/jooq-meta-$VERSION.jar -DgroupId=org.jooq -DartifactId=jooq-meta -DrepositoryId=$REPOSITORY -Durl=$URL -Dversion=$VERSION -Dpackaging=jar $JAVADOC_JOOQ_META $SOURCES_JOOQ_META -DpomFile=jOOQ-pom/jooq-meta/pom.xml

mvn deploy:deploy-file -Dfile=jOOQ-lib/jooq-codegen-$VERSION.jar -DgroupId=org.jooq -DartifactId=jooq-codegen -DrepositoryId=$REPOSITORY -Durl=$URL -Dversion=$VERSION -Dpackaging=jar $JAVADOC_JOOQ_CODEGEN $SOURCES_JOOQ_CODEGEN -DpomFile=jOOQ-pom/jooq-codegen/pom.xml

mvn deploy:deploy-file -Dfile=jOOQ-lib/jooq-codegen-maven-$VERSION.jar -DgroupId=org.jooq -DartifactId=jooq-codegen-maven -DrepositoryId=$REPOSITORY -Durl=$URL -Dversion=$VERSION -Dpackaging=jar $JAVADOC_JOOQ_CODEGEN_MAVEN $SOURCES_JOOQ_CODEGEN_META -DpomFile=jOOQ-pom/jooq-codegen-maven/pom.xml

mvn deploy:deploy-file -Dfile=jOOQ-lib/jooq-scala-$VERSION.jar -DgroupId=org.jooq -DartifactId=jooq-scala -DrepositoryId=$REPOSITORY -Durl=$URL -Dversion=$VERSION -Dpackaging=jar $JAVADOC_JOOQ_SCALA $SOURCES_JOOQ_SCALA -DpomFile=jOOQ-pom/jooq-scala/pom.xml
```

### 권장 방식

실제 프로젝트 설정에서는 위의 방식들만으로는 충분하지 않을 것이다. 권장하는 방식은 라이브러리를 로컬 Nexus나 Bintray, 또는 사용하고 있는 어떤 저장소든 그곳에 가져오는 것이다. 단, 상용 배포물에 있을 수 있는 배포 제한 사항을 염두에 두어야 한다.

위 스크립트의 최신 버전은 jOOQ 프로젝트의 GitHub에서 확인할 수 있다: https://github.com/jOOQ/jOOQ/tree/master/jOOQ-release/release/template
