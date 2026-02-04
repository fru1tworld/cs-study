# 이 설정으로 Eclipse m2e 구성 속도를 높여라

> 원문: https://blog.jooq.org/use-this-preference-to-speed-up-your-eclipse-m2e-configuration/

Eclipse에서 Maven 프로젝트를 열면 기본적으로 m2e(Maven to Eclipse) 플러그인이 `pom.xml` 파일을 시각적인 JFace 다이얼로그 형태로 보여준다.

이 화면은 로딩하는 데 다소 느리고, 실제로 큰 가치를 제공하지 않는다. 왜냐하면 Maven 플러그인 설정에는 구조화되지 않은 임의의 XML이 포함될 수 있기 때문에, 시각적 에디터가 이를 합리적으로 표시하거나 편집할 수 있는 방법이 없기 때문이다.

따라서 항상 가장 오른쪽에 있는 `pom.xml` 탭으로 바로 이동하여 XML 소스를 직접 편집하게 된다.

그렇다면 왜 시각적 에디터가 기본값이어야 할까?

## 해결책

워크스페이스 설정을 변경하여 `pom.xml` 파일을 열 때 XML 탭이 기본으로 열리도록 설정할 수 있다.

이 설정은 새로운 Eclipse 설치 시 기본값이 되어야 한다고 생각한다. 만약 동의한다면, 다음 버그 리포트에 투표해 달라:

https://bugs.eclipse.org/bugs/show_bug.cgi?id=465882

이 간단한 설정 변경만으로도 Maven 프로젝트 작업 시 불필요한 UI 오버헤드를 줄이고 워크플로우 효율성을 크게 향상시킬 수 있다.
