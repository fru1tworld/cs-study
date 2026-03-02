# TypeScript 핸드북

## 이 핸드북에 대하여

프로그래밍 커뮤니티에 소개된 지 20년이 넘은 지금, JavaScript는 역대 가장 널리 사용되는 크로스 플랫폼 언어 중 하나가 되었습니다. 웹페이지에 간단한 상호작용을 추가하기 위한 작은 스크립팅 언어로 시작한 JavaScript는 모든 규모의 프론트엔드 및 백엔드 애플리케이션에서 선택받는 언어로 성장했습니다. JavaScript로 작성된 프로그램의 크기, 범위, 복잡성이 기하급수적으로 증가한 반면, JavaScript 언어가 서로 다른 코드 단위 간의 관계를 표현하는 능력은 그에 미치지 못했습니다. JavaScript의 다소 독특한 런타임 시맨틱과 결합하여, 이러한 언어와 프로그램 복잡성 간의 불일치는 JavaScript 개발을 대규모로 관리하기 어려운 작업으로 만들었습니다.

프로그래머가 작성하는 가장 흔한 종류의 오류는 타입 오류로 설명할 수 있습니다: 다른 종류의 값이 예상되는 곳에 특정 종류의 값이 사용된 경우입니다. 이는 단순한 오타, 라이브러리의 API 표면에 대한 이해 실패, 런타임 동작에 대한 잘못된 가정, 또는 기타 오류로 인한 것일 수 있습니다. TypeScript의 목표는 JavaScript 프로그램을 위한 정적 타입 검사기가 되는 것입니다 - 다시 말해, 코드가 실행되기 전에 실행되어(정적) 프로그램의 타입이 올바른지 확인하는(타입 검사) 도구입니다.

JavaScript 배경 없이 TypeScript를 처음 접하며 TypeScript를 첫 번째 언어로 배우려는 경우, 먼저 [Microsoft Learn JavaScript 튜토리얼](https://developer.microsoft.com/javascript/)이나 [Mozilla Web Docs의 JavaScript](https://developer.mozilla.org/docs/Web/JavaScript/Guide)에서 문서를 읽어보시기 바랍니다.
다른 언어 경험이 있다면, 핸드북을 읽으면서 JavaScript 문법을 꽤 빠르게 익힐 수 있을 것입니다.

## 이 핸드북의 구성

핸드북은 두 섹션으로 나뉩니다:

- **핸드북**

  TypeScript 핸드북은 일상적인 프로그래머에게 TypeScript를 설명하는 포괄적인 문서가 되도록 의도되었습니다. 왼쪽 네비게이션에서 위에서 아래로 핸드북을 읽을 수 있습니다.

  각 챕터나 페이지가 주어진 개념에 대한 강력한 이해를 제공할 것으로 기대해도 됩니다. TypeScript 핸드북은 완전한 언어 명세가 아니지만, 언어의 모든 기능과 동작에 대한 포괄적인 가이드가 되도록 의도되었습니다.

  연습을 완료한 독자는 다음을 할 수 있어야 합니다:

  - 일반적으로 사용되는 TypeScript 문법과 패턴 읽고 이해하기
  - 중요한 컴파일러 옵션의 효과 설명하기
  - 대부분의 경우에서 타입 시스템 동작을 올바르게 예측하기

  명확성과 간결성을 위해, 핸드북의 주요 내용은 다루는 기능의 모든 엣지 케이스나 세부 사항을 탐구하지 않습니다. 특정 개념에 대한 자세한 내용은 참조 문서에서 찾을 수 있습니다.

- **참조 파일**

  네비게이션에서 핸드북 아래의 참조 섹션은 TypeScript의 특정 부분이 어떻게 작동하는지에 대한 더 풍부한 이해를 제공하도록 구축되었습니다. 위에서 아래로 읽을 수 있지만, 각 섹션은 단일 개념에 대한 더 깊은 설명을 제공하는 것을 목표로 합니다 - 연속성을 목표로 하지 않습니다.

### 비목표

핸드북은 또한 몇 시간 안에 편안하게 읽을 수 있는 간결한 문서가 되도록 의도되었습니다. 간결하게 유지하기 위해 특정 주제는 다루지 않습니다.

구체적으로, 핸드북은 함수, 클래스, 클로저와 같은 핵심 JavaScript 기본 사항을 완전히 소개하지 않습니다. 적절한 경우, 해당 개념을 읽어볼 수 있는 배경 읽기 링크를 포함합니다.

핸드북은 또한 언어 명세를 대체하도록 의도되지 않았습니다. 일부 경우, 엣지 케이스나 동작의 공식적인 설명은 높은 수준의 이해하기 쉬운 설명을 위해 생략됩니다. 대신, TypeScript의 동작의 많은 측면을 더 정확하고 공식적으로 설명하는 별도의 참조 페이지가 있습니다. 참조 페이지는 TypeScript에 익숙하지 않은 독자를 위한 것이 아니므로, 고급 용어를 사용하거나 아직 읽지 않은 주제를 참조할 수 있습니다.

마지막으로, 핸드북은 필요한 경우를 제외하고 TypeScript가 다른 도구와 어떻게 상호작용하는지 다루지 않습니다. webpack, rollup, parcel, react, babel, closure, lerna, rush, bazel, preact, vue, angular, svelte, jquery, yarn, npm으로 TypeScript를 구성하는 방법과 같은 주제는 범위를 벗어납니다 - 이러한 리소스는 웹의 다른 곳에서 찾을 수 있습니다.

## 시작하기

[기본 사항](/docs/handbook/2/basic-types.html)을 시작하기 전에, 다음 소개 페이지 중 하나를 읽어보시기를 권장합니다. 이 소개는 TypeScript와 선호하는 프로그래밍 언어 간의 주요 유사점과 차이점을 강조하고, 해당 언어에 특정한 일반적인 오해를 해소하기 위한 것입니다.

- [새 프로그래머를 위한 TypeScript](/docs/handbook/typescript-from-scratch.html)
- [JavaScript 프로그래머를 위한 TypeScript](/docs/handbook/typescript-in-5-minutes.html)
- [Java/C# 프로그래머를 위한 TypeScript](/docs/handbook/typescript-in-5-minutes-oop.html)
- [함수형 프로그래머를 위한 TypeScript](/docs/handbook/typescript-in-5-minutes-func.html)

그렇지 않으면 [기본 사항](/docs/handbook/2/basic-types.html)으로 바로 이동하세요.
