# 소개

선언 파일 섹션은 고품질의 TypeScript 선언 파일(Declaration File)을 작성하는 방법을 알려주기 위해 설계되었습니다. 시작하기 위해서는 TypeScript 언어에 대한 기본적인 친숙함이 필요합니다.

아직 읽지 않았다면, [TypeScript 핸드북](/docs/handbook/2/basic-types.html)을 읽고 기본 개념, 특히 타입과 모듈에 대해 익숙해지시기 바랍니다.

.d.ts 파일이 어떻게 작동하는지 배우는 가장 일반적인 경우는 타입이 없는 npm 패키지에 타입을 추가하려는 경우입니다. 그런 경우에는 [모듈 .d.ts](/docs/handbook/declaration-files/templates/module-d-ts.html)로 바로 이동하실 수 있습니다.

선언 파일 섹션은 다음 섹션들로 구성되어 있습니다.

## [선언 참조](/docs/handbook/declaration-files/by-example.html)

우리는 종종 기저 라이브러리의 예제만을 가이드로 삼아 선언 파일을 작성해야 하는 상황에 직면합니다. [선언 참조](/docs/handbook/declaration-files/by-example.html) 섹션은 많은 일반적인 API 패턴과 각각에 대한 선언을 작성하는 방법을 보여줍니다. 이 가이드는 TypeScript의 모든 언어 구조에 아직 익숙하지 않을 수 있는 TypeScript 초보자를 대상으로 합니다.

## [라이브러리 구조](/docs/handbook/declaration-files/library-structures.html)

[라이브러리 구조](/docs/handbook/declaration-files/library-structures.html) 가이드는 일반적인 라이브러리 형식을 이해하고 각 형식에 대한 적절한 선언 파일을 작성하는 방법을 도와줍니다. 기존 파일을 편집하는 경우에는 이 섹션을 읽을 필요가 없을 것입니다. 새 선언 파일을 작성하는 작성자는 라이브러리의 형식이 선언 파일 작성에 어떤 영향을 미치는지 제대로 이해하기 위해 이 섹션을 읽는 것이 강력히 권장됩니다.

템플릿 섹션에서는 새 파일을 작성할 때 유용한 시작점으로 사용할 수 있는 여러 선언 파일을 찾을 수 있습니다. 이미 구조를 알고 있다면 사이드바의 d.ts 템플릿 섹션을 참조하세요.

## [해야 할 것과 하지 말아야 할 것](/docs/handbook/declaration-files/do-s-and-don-ts.html)

선언 파일의 많은 일반적인 실수는 쉽게 피할 수 있습니다. [해야 할 것과 하지 말아야 할 것](/docs/handbook/declaration-files/do-s-and-don-ts.html) 섹션은 일반적인 오류를 식별하고, 이를 감지하는 방법, 그리고 수정하는 방법을 설명합니다. 일반적인 실수를 피하기 위해 모든 사람이 이 섹션을 읽어야 합니다.

## [심층 분석](/docs/handbook/declaration-files/deep-dive.html)

선언 파일이 어떻게 작동하는지 기저 메커니즘에 관심 있는 숙련된 작성자를 위해, [심층 분석](/docs/handbook/declaration-files/deep-dive.html) 섹션은 선언 작성의 많은 고급 개념을 설명하고, 이러한 개념을 활용하여 더 깔끔하고 직관적인 선언 파일을 만드는 방법을 보여줍니다.

## [npm에 배포하기](/docs/handbook/declaration-files/publishing.html)

[배포](/docs/handbook/declaration-files/publishing.html) 섹션은 선언 파일을 npm 패키지에 배포하는 방법과 종속 패키지를 관리하는 방법을 설명합니다.

## [선언 파일 찾기 및 설치하기](/docs/handbook/declaration-files/consumption.html)

JavaScript 라이브러리 사용자를 위해, [사용](/docs/handbook/declaration-files/consumption.html) 섹션은 해당 선언 파일을 찾고 설치하는 간단한 몇 단계를 제공합니다.
