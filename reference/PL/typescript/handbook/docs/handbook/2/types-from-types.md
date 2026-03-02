---
title: 타입으로부터 타입 만들기
layout: docs
permalink: /docs/handbook/2/types-from-types.html
oneline: "기존 타입에서 새로운 타입을 만드는 다양한 방법 개요"
---

TypeScript의 타입 시스템은 _다른 타입을 기반으로_ 타입을 표현할 수 있기 때문에 매우 강력합니다.

이 아이디어의 가장 단순한 형태는 제네릭입니다. 그 외에도 다양한 _타입 연산자_를 사용할 수 있습니다.
또한 이미 가지고 있는 _값_을 기반으로 타입을 표현하는 것도 가능합니다.

다양한 타입 연산자를 결합하여 복잡한 연산과 값을 간결하고 유지보수가 용이한 방식으로 표현할 수 있습니다.
이 섹션에서는 기존 타입이나 값을 기반으로 새로운 타입을 표현하는 방법을 다룹니다.

- [제네릭](/docs/handbook/2/generics.html) - 매개변수를 받는 타입
- [Keyof 타입 연산자](/docs/handbook/2/keyof-types.html) - `keyof` 연산자를 사용하여 새로운 타입 만들기
- [Typeof 타입 연산자](/docs/handbook/2/typeof-types.html) - `typeof` 연산자를 사용하여 새로운 타입 만들기
- [인덱스 접근 타입](/docs/handbook/2/indexed-access-types.html) - `Type['a']` 문법을 사용하여 타입의 일부에 접근하기
- [조건부 타입](/docs/handbook/2/conditional-types.html) - 타입 시스템에서 if 문처럼 동작하는 타입
- [매핑된 타입](/docs/handbook/2/mapped-types.html) - 기존 타입의 각 속성을 매핑하여 타입 만들기
- [템플릿 리터럴 타입](/docs/handbook/2/template-literal-types.html) - 템플릿 리터럴 문자열을 통해 속성을 변경하는 매핑된 타입
