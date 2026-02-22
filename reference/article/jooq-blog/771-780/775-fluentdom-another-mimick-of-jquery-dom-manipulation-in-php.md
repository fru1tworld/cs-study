# FluentDOM, PHP에서 jQuery DOM 조작의 또 다른 모방

> 원문: https://blog.jooq.org/fluentdom-another-mimick-of-jquery-dom-manipulation-in-php/

2011년 8월 15일 (2011년 8월 16일 수정), lukaseder

jQuery가 다른 모든 XML API에 대해 거둔 승리는 많은 언어에서 두드러져 보입니다. SQL 시뮬레이션 Fluent API 분야에서 새로운 경쟁자를 발견하는 것처럼, 저는 이제 PHP에서 jQuery DOM 조작을 모방하는 또 다른 것을 발견했습니다: FluentDOM. FluentDOM은 jQuery와 유사한 Fluent API를 XPath 및 일반 DOM XML 조작과 결합합니다.

다음은 몇 가지 코드 예제입니다:

파일 읽기 예제:

```php
echo FluentDOM($xmlFile)
  ->find('/message')
  ->text('Hello World!');
```

요소 찾기:

```php
var_dump($fd->find('/root')->find('*[2]')->item(0)->textContent);
```

요소 추가:

```php
$menu
  ->append('<li/>')
  ->append('<a/>')
  ->attr('href', '/sample.php')
  ->text('Sample');
```

저는 FluentDOM 개발자들과 연락을 취했고 프로젝트 간의 잠재적인 시너지에 대해 논의했습니다. jOOX(관련 Java 라이브러리)의 경우, 이는 버전 0.9.2에서 파일/스트림 로딩 기능과 XPath 지원을 의미합니다. 반대로 FluentDOM은 중첩된 XML 구조를 프로그래밍 방식으로 구축하기 위한 jOOQ의 문서 생성 구문을 채택할 수 있을 것이라고 제안했습니다.

이러한 오픈 소스 프로젝트 간의 상호 발전은 두 제품 모두를 더 좋게 만들 것입니다. jOOX의 지속적인 개발에 대해 기대하고 있습니다.

jOOX 프로젝트 저장소를 확인하세요: https://code.google.com/p/joox/
