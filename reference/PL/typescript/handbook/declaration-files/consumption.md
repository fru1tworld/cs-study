# 사용하기

## 다운로드

타입 선언을 가져오는 데는 npm 외에 다른 도구가 필요하지 않습니다.

예를 들어, lodash와 같은 라이브러리의 선언을 가져오려면 다음 명령만 필요합니다:

```cmd
npm install --save-dev @types/lodash
```

npm 패키지가 이미 [배포하기](/docs/handbook/declaration-files/publishing.html)에서 설명한 대로 선언 파일을 포함하고 있다면, 해당 `@types` 패키지를 다운로드할 필요가 없다는 점을 알아두세요.

## 사용하기

그런 다음 TypeScript 코드에서 lodash를 문제없이 사용할 수 있습니다. 이것은 모듈과 전역 코드 모두에서 작동합니다.

예를 들어, 타입 선언을 `npm install`한 후에는 import를 사용하여 작성할 수 있습니다:

```ts
import * as _ from "lodash";
_.padStart("Hello TypeScript!", 20, " ");
```

또는 모듈을 사용하지 않는 경우, 전역 변수 `_`를 그냥 사용할 수 있습니다.

```ts
_.padStart("Hello TypeScript!", 20, " ");
```

## 검색

대부분의 경우, 타입 선언 패키지는 항상 `npm`의 패키지 이름과 동일한 이름을 가지지만 `@types/` 접두사가 붙습니다. 하지만 필요한 경우 [Yarn 패키지 검색](https://yarnpkg.com/)을 사용하여 좋아하는 라이브러리의 패키지를 찾을 수 있습니다.

> 참고: 찾고 있는 선언 파일이 없는 경우, 언제든지 하나를 기여하고 다음에 찾는 개발자를 도울 수 있습니다. 자세한 내용은 DefinitelyTyped [기여 가이드라인 페이지](https://definitelytyped.org/guides/contributing.html)를 참조하세요.
