# Zod 스키마 정의 2: 객체와 컬렉션

> 원문: https://zod.dev/api

이 문서는 Zod 4 (4.6.5 기준) API 문서의 「Defining schemas」 페이지 중 객체부터 `z.instanceof()`까지를 정리한다. 객체 스키마와 그 파생 API, 재귀 객체, 배열과 튜플, 유니언 계열, 교차, 레코드, 맵, 셋, 파일, 프로미스, 클래스 인스턴스 검사를 다룬다. 원시 타입과 `optional()` 같은 래퍼는 [「Zod 스키마 정의 1: 원시 타입과 문자열」](02_schemas_primitives.md)에서, 정제(refinement)와 변환은 [「Zod 스키마 정의 3: 정제, 변환, 코덱」](04_refinements_transforms_codecs.md)에서 설명한다.

Zod Mini는 같은 기능을 메서드 체이닝 대신 함수형 API로 제공하는 경량 패키지다([「Zod Mini」](07_packages_and_compile.md#zod-mini)). 두 패키지의 API가 다른 곳은 코드 블록을 나란히 두고 `// Zod Mini` 주석으로 구분한다. Zod Mini 예제에 자주 나오는 `.check()`는 일반 Zod의 `.min()`, `.refine()` 같은 전용 메서드 대신 검사를 스키마에 넘기는 메서드다([「`.check()`」](07_packages_and_compile.md#check)).

## 객체 (Objects)

`z.object()`에 키마다 값의 스키마를 지정한 객체를 넘기면 객체 스키마가 된다.

```ts
  // 기본적으로 모든 속성은 필수다
  const Person = z.object({
    name: z.string(),
    age: z.number(),
  });

  type Person = z.infer<typeof Person>;
  // => { name: string; age: number; }
```

주석대로 모든 속성은 기본적으로 필수다. 특정 속성을 선택적으로 만들려면 그 속성의 스키마를 `optional()`로 감싼다.

```ts
const Dog = z.object({
  name: z.string(),
  age: z.number().optional(),
});

Dog.parse({ name: "Yeller" }); // ✅
```

```ts
// Zod Mini
const Dog = z.object({
  name: z.string(),
  age: z.optional(z.number())
});

Dog.parse({ name: "Yeller" }); // ✅
```

반대로 입력에 스키마가 선언하지 않은 키(이하 알 수 없는 키)가 들어 있으면 어떻게 될까. `z.object()`는 기본적으로 이런 키를 에러 없이 파싱 결과에서 *제거*(strip)한다.

```ts
Dog.parse({ name: "Yeller", extraKey: true });
// => { name: "Yeller" }
```

알 수 없는 키를 처리하는 방식은 이 제거(strip)를 포함해 세 가지이고, 나머지 두 방식은 아래의 `z.strictObject`와 `z.looseObject`로 고른다.

### `z.strictObject`

알 수 없는 키가 있으면 에러를 던지는 *엄격한(strict)* 스키마는 다음과 같이 정의한다.

```ts
const StrictDog = z.strictObject({
  name: z.string(),
});

StrictDog.parse({ name: "Yeller", extraKey: true });
// ❌ 에러를 던진다
```

### `z.looseObject`

알 수 없는 키를 그대로 통과시키는 *느슨한(loose)* 스키마는 다음과 같이 정의한다.

```ts
const LooseDog = z.looseObject({
  name: z.string(),
});

LooseDog.parse({ name: "Yeller", extraKey: true });
// => { name: "Yeller", extraKey: true }
```

### `.catchall()`

`.catchall()`은 알 수 없는 키를 버리거나 그냥 통과시키는 대신, 그 값을 모두 스키마 하나로 검증한다. 이 스키마를 캐치올(catchall) 스키마라고 한다. 아래 예에서는 `name`, `age` 말고 다른 키가 오면 그 값이 문자열이어야 한다.

```ts
const DogWithStrings = z
  .object({
    name: z.string(),
    age: z.number().optional(),
  })
  .catchall(z.string());


DogWithStrings.parse({ name: "Yeller", extraKey: "extraValue" }); // ✅
DogWithStrings.parse({ name: "Yeller", extraKey: 42 }); // ❌
```

```ts
// Zod Mini
const DogWithStrings = z.catchall(
  z.object({
    name: z.string(),
    age: z.number().optional(),
  }),
  z.string()
);

DogWithStrings.parse({ name: "Yeller", extraKey: "extraValue" }); // ✅
DogWithStrings.parse({ name: "Yeller", extraKey: 42 }); // ❌
```

### `.shape`

`z.object()`에 넘긴, 키와 값 스키마를 짝지은 객체를 shape라고 한다. 각 키의 스키마는 `.shape`로 꺼낸다. Zod Mini에서는 `.def.shape`로 접근한다.

```ts
Dog.shape.name; // => 문자열 스키마
Dog.shape.age; // => 숫자 스키마
```

```ts
// Zod Mini
Dog.def.shape.name; // => 문자열 스키마
Dog.def.shape.age; // => 숫자 스키마
```

### `.keyof()`

객체 스키마의 키 이름으로 `ZodEnum` 스키마를 만든다. `ZodEnum`은 정해진 문자열 값만 통과시키는 열거형 스키마로([「열거형」](02_schemas_primitives.md#열거형-enums)), 여기서는 `"name"`과 `"age"`를 받는다.

```ts
const keySchema = Dog.keyof();
// => ZodEnum<{ name: "name"; age: "age" }>
```

```ts
// Zod Mini
const keySchema = z.keyof(Dog);
// => ZodEnum<{ name: "name"; age: "age" }>
```

### `.extend()`

객체 스키마에 필드를 추가한다.

```ts
const DogWithBreed = Dog.extend({
  breed: z.string(),
});
```

```ts
// Zod Mini
const DogWithBreed = z.extend(Dog, {
  breed: z.string(),
});
```

`.extend()`는 기존 필드도 덮어쓰므로 주의해야 한다. `A.extend(B)`에서 두 스키마에 같은 키가 있으면 B의 스키마가 A의 스키마를 대체한다.

> **참고:** **대안: 전개 구문(spread syntax)** — `.extend()`를 쓰지 않고 원래 스키마의 shape를 펼쳐 넣어 객체 스키마를 새로 만들 수도 있다. 이렇게 하면 결과 스키마의 엄격도, 즉 알 수 없는 키를 제거(strip), 거부(strict), 통과(loose) 중 어떻게 처리하는지가 어떤 함수로 만들었는지에서 바로 보인다.
>
> ```ts
> const DogWithBreed = z.object({ // 또는 z.strictObject()나 z.looseObject()...
>   ...Dog.shape,
>   breed: z.string(),
> });
> ```
>
> 이 방법으로 여러 객체를 한 번에 합칠 수도 있다.
>
> ```ts
> const DogWithBreed = z.object({
>   ...Animal.shape,
>   ...Pet.shape,
>   breed: z.string(),
> });
> ```
>
> 이 방식의 장점은 다음과 같다.
>
> 1. 라이브러리 전용 API 대신 언어 수준 기능([전개 구문](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Operators/Spread_syntax))을 쓴다.
> 2. Zod와 Zod Mini에서 같은 문법이 동작한다.
> 3. `tsc`(TypeScript 컴파일러)의 타입 검사 부담이 적다. `.extend()`는 큰 스키마에서 비용이 클 수 있고, [TypeScript의 한계](https://github.com/microsoft/TypeScript/pull/61505) 때문에 `.extend()`를 연달아 체이닝하면 비용이 제곱으로 늘어난다.
> 4. 원한다면 `z.strictObject()`나 `z.looseObject()`로 결과 스키마의 엄격도를 바꿀 수 있다.

### `.safeExtend()`

`.safeExtend()`는 `.extend()`와 비슷하게 동작하지만, 기존 속성을 원래 타입에 할당할 수 없는 스키마로 덮어쓰지는 못하게 막는다. 아래 예에서 `string` 속성을 `z.string().min(5)`로 좁히는 것은 되지만 `z.number()`로 바꾸는 것은 타입 에러다. 다시 말해 `.safeExtend()`의 결과로 추론되는 타입은 원래 타입을 (TypeScript의 의미에서) [`extends`](https://www.typescriptlang.org/docs/handbook/2/conditional-types.html#conditional-type-constraints)한다. 결과 타입의 값을 원래 타입이 필요한 자리에 그대로 넣을 수 있다는 뜻이다.

```ts
z.object({ a: z.string() }).safeExtend({ a: z.string().min(5) }); // ✅
z.object({ a: z.string() }).safeExtend({ a: z.any() }); // ✅
z.object({ a: z.string() }).safeExtend({ a: z.number() });
//                                       ^  ❌ ZodNumber는 할당할 수 없다
```

`.safeExtend()`가 필요한 경우가 하나 더 있다. 스키마에는 `.refine()`으로 사용자 정의 검사를 붙일 수 있는데, 이를 [정제(refinement)](04_refinements_transforms_codecs.md#정제-refinements)라고 한다. 일반 `.extend()`는 정제가 붙은 스키마에 쓰면 에러를 던지므로, 이런 스키마를 확장할 때는 `.safeExtend()`를 쓴다. 이때 확장된 스키마는 원래 스키마의 정제를 그대로 물려받는다.

```ts
const Base = z.object({
  a: z.string(),
  b: z.string()
}).refine(user => user.a === user.b);

// Extended는 Base의 정제를 물려받는다
const Extended = Base.safeExtend({
  a: z.string().min(10)
});
```

```ts
// Zod Mini
const Base = z.object({
  a: z.string(),
  b: z.string()
}).check(z.refine(user => user.a === user.b));

// Extended는 Base의 정제를 물려받는다
const Extended = z.safeExtend(Base, {
  a: z.string().min(10)
});
```

### `.pick()`

Zod는 TypeScript의 내장 유틸리티 타입 `Pick`과 `Omit`에서 착안해 객체 스키마에서 특정 키를 고르거나 빼는 전용 API를 제공한다.

아래 예제와 이어지는 `.omit()`, `.partial()`, `.required()` 등의 예제는 다음 `Recipe` 스키마를 기준으로 한다.

```ts
const Recipe = z.object({
  title: z.string(),
  description: z.string().optional(),
  ingredients: z.array(z.string()),
});
// { title: string; description?: string | undefined; ingredients: string[] }
```

특정 키를 고르려면 다음과 같이 한다.

```ts
const JustTheTitle = Recipe.pick({ title: true });
```

```ts
// Zod Mini
const JustTheTitle = z.pick(Recipe, { title: true });
```

### `.omit()`

특정 키를 빼려면 다음과 같이 한다.

```ts
const RecipeNoId = Recipe.omit({ id: true });
```

```ts
// Zod Mini
const RecipeNoId = z.omit(Recipe, { id: true });
```

### `.partial()`

`.partial()`은 속성 일부 또는 전체를 선택적으로 만든다. TypeScript의 내장 유틸리티 타입 [`Partial`](https://www.typescriptlang.org/docs/handbook/utility-types.html#partialtype)에서 착안한 전용 API다.

모든 필드를 선택적으로 만들려면 다음과 같이 한다.

```ts
const PartialRecipe = Recipe.partial();
// { title?: string | undefined; description?: string | undefined; ingredients?: string[] | undefined }
```

```ts
// Zod Mini
const PartialRecipe = z.partial(Recipe);
// { title?: string | undefined; description?: string | undefined; ingredients?: string[] | undefined }
```

특정 속성만 선택적으로 만들려면 다음과 같이 한다.

```ts
const RecipeOptionalIngredients = Recipe.partial({
  ingredients: true,
});
// { title: string; description?: string | undefined; ingredients?: string[] | undefined }
```

```ts
// Zod Mini
const RecipeOptionalIngredients = z.partial(Recipe, {
  ingredients: true,
});
// { title: string; description?: string | undefined; ingredients?: string[] | undefined }
```

### `.exactPartial()`

`.partial()`과 같지만 각 필드를 `optional()` 대신 `exactOptional()`로 감싼다. `exactOptional()`은 키가 아예 없는 것은 허용하되 값으로 `undefined`를 명시적으로 넣는 것은 막는다([「옵셔널」](02_schemas_primitives.md#옵셔널-optionals)). 그래서 아래 결과 타입에는 `.partial()`과 달리 `| undefined`가 붙지 않는다. `description`에 남은 `| undefined`는 원래 `optional()`이었던 데서 온 것이다.

```ts
const PartialRecipe = Recipe.exactPartial();
// { title?: string; description?: string | undefined; ingredients?: string[] }
```

```ts
// Zod Mini
const PartialRecipe = z.exactPartial(Recipe);
// { title?: string; description?: string | undefined; ingredients?: string[] }
```

### `z.deepPartial()`

`.partial()`은 최상위 shape, 즉 객체 바로 아래의 키만 선택적으로 만든다. `z.deepPartial()`은 중첩된 모든 객체로 재귀적으로 내려가며, 배열, 튜플, 유니언, 레코드는 물론 `optional()`처럼 다른 스키마를 감싸는 래퍼 스키마 안쪽까지 적용된다. 아래 예에서는 `author` 안의 `name`과 `email`까지 선택적이 되므로 `{ author: {} }`도 통과한다.

```ts
const Post = z.object({
  title: z.string(),
  author: z.object({ name: z.string(), email: z.string() }),
});

z.deepPartial(Post).parse({ author: {} }); // ✅
```

원본 스키마는 절대 수정되지 않는다. 결과도 여전히 `ZodObject`이므로 `.shape`와 `.extend()`를 계속 쓸 수 있다.

다만 안에 있던 [판별 유니언](#판별-유니언-discriminated-unions)은 일반 `z.union()`으로 바뀐다. 판별 유니언은 판별자 키의 값을 보고 검사할 옵션을 고르는데, 판별자가 선택적이 되면 그 값이 없을 수 있어 이 방식이 성립하지 않기 때문이다. 또 `.partial()`과 마찬가지로, 객체 자체에 [정제](#safeextend)가 붙어 있으면 에러를 던진다.

### `.required()`

`.required()`는 속성 일부 또는 전체를 *필수*로 만든다. TypeScript의 [`Required`](https://www.typescriptlang.org/docs/handbook/utility-types.html#requiredtype) 유틸리티 타입에서 착안한 API다.

모든 속성을 필수로 만들려면 다음과 같이 한다.

```ts
const RequiredRecipe = Recipe.required();
// { title: string; description: string; ingredients: string[] }
```

```ts
// Zod Mini
const RequiredRecipe = z.required(Recipe);
// { title: string; description: string; ingredients: string[] }
```

특정 속성만 필수로 만들려면 다음과 같이 한다.

```ts
const RecipeRequiredDescription = Recipe.required({description: true});
// { title: string; description: string; ingredients: string[] }
```

```ts
// Zod Mini
const RecipeRequiredDescription = z.required(Recipe, {description: true});
// { title: string; description: string; ingredients: string[] }
```

### 심벌 키 (Symbol keys)

shape에는 문자열 키뿐 아니라 심벌 키도 선언할 수 있다. `unique symbol`은 특정 심벌 하나만을 가리키는 TypeScript 타입이다. `const`로 선언한 심벌은 이 타입으로 추론되어 TypeScript가 다른 심벌과 구분할 수 있으므로, 그 키가 추론 타입에 반영되고 값도 다른 키와 똑같이 검증된다.

```ts
const TAG = Symbol("tag");
const Post = z.object({ title: z.string(), [TAG]: z.number() });
// { title: string; [TAG]: number }

Post.parse({ title: "hello", [TAG]: 42 }); // ✅
Post.parse({ title: "hello" });            // ❌ 심벌 키는 필수다
```

선언된 심벌 키는 `.partial()`, `.pick()`, `.omit()`, `.extend()` 등 나머지 API에서도 동작한다. shape에 선언하지 않은 심벌 키는 무시된다. `z.looseObject()`도 그런 키를 통과시키지 않고, `z.strictObject()`도 에러로 잡지 않는다.

## 재귀 객체 (Recursive objects)

자기 자신을 참조하는 타입을 정의하려면 키에 [getter](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Functions/get)를 쓴다. getter는 속성을 읽을 때마다 실행되는 함수다. `Category`를 정의하는 도중에는 아직 `Category` 변수를 쓸 수 없으므로 `subcategories: z.array(Category)`처럼 바로 적을 수 없다. 반면 getter 안의 코드는 나중에 속성을 읽을 때 실행되고, 그때는 `Category`가 이미 정의되어 있다. 덕분에 런타임에서도 자기 자신을 참조하는 스키마를 만들 수 있다.

```ts
const Category = z.object({
  name: z.string(),
  get subcategories(){
    return z.array(Category)
  }
});

type Category = z.infer<typeof Category>;
// { name: string; subcategories: Category[] }
```

스키마뿐 아니라 입력 데이터가 순환할 수도 있다. 아래 `input`처럼 객체가 자기 자신을 하위 항목으로 품고 있는 경우다. Zod는 이런 입력도 별도 설정 없이 처리하며, 파싱 결과도 입력과 같은 순환 구조를 유지한다.

```ts
const input: any = { name: "root", subcategories: [] };
input.subcategories.push(input);

const result = Category.parse(input);
result.subcategories[0] === result; // true

// 출력 그래프는 입력 그래프와 같은 구조를 가진다
result.subcategories[0].subcategories[0] === result; // true
```

Zod Mini는 번들 크기 때문에 이 기능을 기본으로 켜 두지 않는다. 순환 입력을 처리하려면 메모이저(memoizer, 한 번 처리한 결과를 기억해 두었다가 다시 쓰는 장치)를 명시적으로 등록해야 한다.

```ts
// Zod Mini
// Zod Mini는 스키마를 정의하기 전에 메모이저를 등록해야 한다
z.config({ memoizer: z.memoizer() });

const input: any = { name: "root", subcategories: [] };
input.subcategories.push(input);

const result = Category.parse(input);
result.subcategories[0] === result; // true
```

두 타입이 서로를 참조하는 상호 재귀(mutually recursive) 타입도 같은 방식으로 정의한다. 아래에서 `User`는 `Post` 배열을, `Post`는 `User`를 참조한다.

```ts
const User = z.object({
  email: z.email(),
  get posts(){
    return z.array(Post)
  }
});

const Post = z.object({
  title: z.string(),
  get author(){
    return User
  }
});
```

getter로 정의한 객체 스키마에서도 `.pick()`, `.omit()`, `.required()`, `.partial()` 등 모든 객체 API가 그대로 동작한다.

### 순환 참조 에러 (Circularity errors)

TypeScript의 한계 때문에 재귀 타입 추론은 까다롭고, 일부 상황에서만 동작한다. 더 복잡한 타입에서는 다음과 같은 재귀 타입 에러가 날 수 있다.

```ts
const Activity = z.object({
  name: z.string(),
  get subactivities() {
    // ^ ❌ 'subactivities' implicitly has return type 'any' because it does not
    // have a return type annotation and is referenced directly or indirectly
    // in one of its return expressions.ts(7023)

    return z.nullable(z.array(Activity));
  },
});
```

에러 메시지대로, getter에 반환 타입 주석이 없어서 TypeScript가 반환 타입을 추론하다가 자기 자신을 다시 만난 것이다. 문제가 되는 getter에 반환 타입 주석을 직접 달면 에러가 사라진다.

```ts
const Activity = z.object({
  name: z.string(),
  get subactivities(): z.ZodNullable<z.ZodArray<typeof Activity>> {
    return z.nullable(z.array(Activity));
  },
});
```

## 배열 (Arrays)

배열 스키마는 다음과 같이 정의한다.

```ts
const stringArray = z.array(z.string()); // 또는 z.string().array()
```

```ts
// Zod Mini
const stringArray = z.array(z.string());
```

배열 요소의 내부 스키마에는 다음과 같이 접근한다.

```ts
stringArray.unwrap(); // => 문자열 스키마
```

```ts
// Zod Mini
stringArray.def.element; // => 문자열 스키마
```

Zod는 배열 전용 검증을 여러 개 제공한다.

```ts
z.array(z.string()).nonempty(); // 요소가 1개 이상이어야 한다
z.array(z.string()).min(5); // 요소가 5개 이상이어야 한다
z.array(z.string()).max(5); // 요소가 5개 이하여야 한다
z.array(z.string()).length(5); // 요소가 정확히 5개여야 한다
```

```ts
// Zod Mini
z.array(z.string()).check(z.minLength(1)); // .nonempty()의 별칭
z.array(z.string()).check(z.minLength(5)); // 요소가 5개 이상이어야 한다
z.array(z.string()).check(z.maxLength(5)); // 요소가 5개 이하여야 한다
z.array(z.string()).check(z.length(5)); // 요소가 정확히 5개여야 한다
```

## 튜플 (Tuples)

배열과 달리 튜플은 보통 길이가 고정되어 있고, 인덱스마다 다른 스키마를 지정한다.

```ts
const MyTuple = z.tuple([
  z.string(),
  z.number(),
  z.boolean()
]);

type MyTuple = z.infer<typeof MyTuple>;
// [string, number, boolean]
```

고정된 요소 뒤에 같은 타입의 요소가 몇 개든 올 수 있게 하려면, 이 나머지(rest) 요소의 스키마를 두 번째 인자로 넘긴다.

```ts
const variadicTuple = z.tuple([z.string()], z.number());
// => [string, ...number[]];
```

모든 요소를 선택적으로 만들려면 다음과 같이 한다.

```ts
const PartialTuple = z.tuple([z.string(), z.number()]).partial();
// => [(string | undefined)?, (number | undefined)?]

z.tuple([z.string()], z.number()).partial();
// => [(string | undefined)?, ...number[]]
```

```ts
// Zod Mini
const PartialTuple = z.partial(z.tuple([z.string(), z.number()]));
// => [(string | undefined)?, (number | undefined)?]

z.partial(z.tuple([z.string()], z.number()));
// => [(string | undefined)?, ...number[]]
```

## 유니언 (Unions)

유니언 타입(`A | B`)은 논리적 "OR"을 나타낸다. Zod의 유니언 스키마는 입력을 옵션마다 순서대로 검사하고, 처음으로 검증에 성공한 값을 반환한다.

```ts
const stringOrNumber = z.union([z.string(), z.number()]);
// string | number

stringOrNumber.parse("foo"); // 통과
stringOrNumber.parse(14); // 통과
```

내부 옵션 스키마는 다음과 같이 꺼낸다.

```ts
stringOrNumber.options; // [ZodString, ZodNumber]
```

```ts
// Zod Mini
stringOrNumber.def.options; // [ZodString, ZodNumber]
```

## 배타적 유니언 (Exclusive unions (XOR))

배타적 유니언(XOR)은 정확히 하나의 옵션만 일치해야 하는 유니언이다. 옵션 중 하나라도 일치하면 성공하는 일반 유니언과 달리, `z.xor()`는 일치하는 옵션이 하나도 없거나 둘 이상이면 실패한다.

```ts
const schema = z.xor([z.string(), z.number()]);

schema.parse("hello"); // ✅ 통과
schema.parse(42);      // ✅ 통과
schema.parse(true);    // ❌ 실패 (일치하는 옵션 없음)
```

옵션끼리 상호 배타적이어야 할 때 유용하다.

```ts
// 이 중 정확히 하나만 일치하는지 검증한다
const payment = z.xor([
  z.object({ type: z.literal("card"), cardNumber: z.string() }),
  z.object({ type: z.literal("bank"), accountNumber: z.string() }),
]);

payment.parse({ type: "card", cardNumber: "1234" }); // ✅ 통과
```

입력이 둘 이상의 옵션에 일치하면 `z.xor()`는 실패한다. 이때 생기는 이슈에는 `inclusive: false`와, 일치한 옵션의 인덱스를 담은 `matches` 배열이 들어 있다. 이슈 구조는 [「ZodError와 이슈」](05_errors.md#zoderror와-이슈-zoderror-and-issues)를 참고한다.

```ts
const overlapping = z.xor([z.string(), z.any()]);
overlapping.parse("hello"); // ❌ 실패 (string과 any 모두 일치)
// ZodError: Invalid input: more than one option matched
```

`z.object()`는 알 수 없는 키를 거부하지 않고 제거하기 때문에, 객체 옵션은 생각보다 자주 겹친다. 아래 예에서 두 키를 모두 가진 입력은 *두* 옵션에 모두 일치한다. 첫 번째 옵션은 `version`을 제거하고 통과시키고, 두 번째 옵션은 그대로 둔 채 통과시킨다. 범위가 좁은 쪽 옵션에 `z.strictObject()`(또는 `.strict()`)를 쓰면 그 옵션은 `version` 키가 있는 입력을 거부하므로, 두 분기가 상호 배타적이 된다.

```ts
const name = z.object({ name: z.string() });
const version = name.extend({ version: z.string() });

z.xor([name, version]).parse({ name: "zod", version: "4" });           // ❌ 실패 (둘 다 일치)
z.xor([name.strict(), version]).parse({ name: "zod", version: "4" });  // ✅ 통과
```

## 판별 유니언 (Discriminated unions)

[판별 유니언](https://www.typescriptlang.org/docs/handbook/2/narrowing.html#discriminated-unions)은 모든 옵션이 객체 스키마이고, 그 옵션들이 특정 키 하나를 공유하는 특별한 유니언이다. 이 공유 키를 판별자(discriminator)라고 하며, 아래 `MyResult`에서는 `status`가 판별자다. TypeScript는 판별자 값을 확인한 분기 안에서 타입을 그 값에 해당하는 옵션으로 좁힌다.

```ts
type MyResult =
  | { status: "success"; data: string }
  | { status: "failed"; error: string };

function handleResult(result: MyResult){
  if(result.status === "success"){
    result.data; // string
  } else {
    result.error; // string
  }
}
```

이 타입을 일반 `z.union()`으로 표현할 수도 있다. 하지만 일반 유니언은 판별자를 활용하지 않고 입력을 옵션마다 순서대로 검사해 처음 통과한 옵션을 반환하므로, 옵션이 많으면 느려질 수 있다. 그래서 Zod는 판별자 키를 이용해 파싱을 효율적으로 만드는 `z.discriminatedUnion()` API를 제공한다.

```ts
const MyResult = z.discriminatedUnion("status", [
  z.object({ status: z.literal("success"), data: z.string() }),
  z.object({ status: z.literal("failed"), error: z.string() }),
]);
```

각 옵션은 *객체 스키마*여야 하고, 그 판별자 속성(위 예의 `status`)은 어떤 리터럴 값이나 값 집합에 대응해야 한다. 보통 `z.enum()`, `z.literal()`, `z.null()`, `z.undefined()`를 쓴다.

### 판별 유니언 중첩 (Nesting discriminated unions)

고급 사용 사례에서는 판별 유니언을 중첩할 수 있다. Zod는 각 단계의 판별자를 활용하는 최적의 파싱 전략을 스스로 찾는다.

```ts
const BaseError = { status: z.literal("failed"), message: z.string() };
const MyErrors = z.discriminatedUnion("code", [
  z.object({ ...BaseError, code: z.literal(400) }),
  z.object({ ...BaseError, code: z.literal(401) }),
  z.object({ ...BaseError, code: z.literal(500) }),
]);

const MyResult = z.discriminatedUnion("status", [
  z.object({ status: z.literal("success"), data: z.string() }),
  MyErrors
]);
```

### `z.getDiscriminatedOption()`

판별자 값으로 옵션 하나를 꺼낸다. 값은 유니언이 선언한 것이어야 하며, 그 밖의 값은 TypeScript 에러가 된다.

```ts
const Success = z.object({ status: z.literal("success"), data: z.string() });
const Failed = z.object({ status: z.literal("failed"), error: z.string() });
const MyResult = z.discriminatedUnion("status", [Success, Failed]);

z.getDiscriminatedOption(MyResult, "success"); // typeof Success
z.getDiscriminatedOption(MyResult, "pending"); // ❌ 선언된 판별자가 아니다
```

옵션은 그대로 반환되므로 `.shape`를 비롯한 객체 메서드를 계속 쓸 수 있다.

판별자 키가 없어도 통과하는 옵션이 여럿이면, `undefined`로 조회했을 때 어느 옵션인지 정할 수 없으므로 모호성 에러를 던진다. 이때도 다른 옵션과 겹치지 않는 판별자 값을 명시한 옵션은 그 값으로 꺼낼 수 있다.

## 교차 (Intersections)

교차 타입(`A & B`)은 논리적 "AND"를 나타내며, 교차 스키마는 입력이 두 스키마를 모두 통과해야 성공한다. 아래 예에서 `a`는 숫자나 문자열을, `b`는 숫자나 불리언을 받으므로 둘을 모두 만족하는 타입은 `number`뿐이다.

```ts
const a = z.union([z.number(), z.string()]);
const b = z.union([z.number(), z.boolean()]);
const c = z.intersection(a, b);

type c = z.infer<typeof c>; // => number
```

교차는 두 객체 타입을 합칠 때 주로 쓴다.

```ts
const Person = z.object({ name: z.string() });
type Person = z.infer<typeof Person>;

const Employee = z.object({ role: z.string() });
type Employee = z.infer<typeof Employee>;

const EmployedPerson = z.intersection(Person, Employee);
type EmployedPerson = z.infer<typeof EmployedPerson>;
// Person & Employee
```

> **주의:** 객체 스키마를 합칠 때는 교차보다 [`A.extend(B)`](#extend)를 권장한다. `.extend()`는 새 객체 스키마를 돌려주지만, `z.intersection(A, B)`는 `pick`, `omit` 같은 일반적인 객체 메서드가 없는 `ZodIntersection` 인스턴스를 돌려준다.

## 레코드 (Records)

레코드 스키마는 `Record<string, string>` 같은 타입, 즉 키 이름을 미리 정하지 않고 키와 값의 타입만 정한 객체를 검증한다. `z.record()`에는 키 스키마와 값 스키마를 차례로 넘긴다.

### `z.record`

```ts
const IdCache = z.record(z.string(), z.string());
type IdCache = z.infer<typeof IdCache>; // Record<string, string>

IdCache.parse({
  carlotta: "77d2586b-9e8e-4ecf-8b21-ea7e0530eadd",
  jimmie: "77d2586b-9e8e-4ecf-8b21-ea7e0530eadd",
});
```

키 스키마로는 `string | number | symbol`에 할당할 수 있는 Zod 스키마라면 무엇이든 쓸 수 있다.

```ts
const Keys = z.union([z.string(), z.number(), z.symbol()]);
const AnyObject = z.record(Keys, z.unknown());
// Record<string | number | symbol, unknown>
```

키 스키마로 `z.enum()`을 넘기면 키 이름이 정해진 객체 스키마가 된다.

```ts
const Keys = z.enum(["id", "name", "email"]);
const Person = z.record(Keys, z.string());
// { id: string; name: string; email: string }
```

Zod는 레코드의 숫자 키를 TypeScript와 거의 같은 방식으로 지원한다. JavaScript 객체의 키는 실제로는 문자열이므로, `number` 스키마를 레코드 키로 쓰면 Zod는 각 키가 숫자로 읽을 수 있는 "숫자 문자열"인지 검증한다. min, max, step 같은 추가 숫자 제약도 함께 확인한다.

```ts
const numberKeys = z.record(z.number(), z.string());
numberKeys.parse({ 
  1: "one", // ✅
  2: "two", // ✅
  "1.5": "one", // ✅
  "-3": "two", // ✅
  abc: "one" // ❌
});

// 추가 검증도 지원한다
const intKeys = z.record(z.int().step(1).min(0).max(10), z.string());
intKeys.parse({ 
  0: "zero", // ✅
  1: "one", // ✅
  2: "two", // ✅
  12: "twelve", // ❌
  abc: "one" // ❌
});
```

값 스키마가 [`.default()`](04_refinements_transforms_codecs.md#기본값-defaults), [`.prefault()`](04_refinements_transforms_codecs.md#사전-기본값-prefaults), `.optional()`처럼 키가 없는 경우를 대신 처리할 수 있으면, `z.object()` 안에서와 마찬가지로 입력 타입에서 그 키가 선택적이 된다. 아래 예에서 입력은 키를 빠뜨려도 되지만, 기본값이 채워지므로 출력에는 두 키가 항상 있다. `z.input`과 `z.output`은 [「타입 추론」](01_intro_and_basics.md#타입-추론-inferring-types)을 참고한다.

```ts
const Person = z.record(z.enum(["id", "name"]), z.string().default(""));

type Input = z.input<typeof Person>; // { id?: string; name?: string }
type Output = z.output<typeof Person>; // { id: string; name: string }
```

### `z.partialRecord`

> **참고:** `z.record()`의 첫 번째 인자로 `z.enum`을 넘기면, Zod는 모든 enum 값이 입력에 키로 존재하는지 빠짐없이 검사한다. 이 동작은 TypeScript와 같다.
>
> ```ts
> type MyRecord = Record<"a" | "b", string>;
> const myRecord: MyRecord = { a: "foo", b: "bar" }; // ✅
> const myRecord: MyRecord = { a: "foo" }; // ❌ 필수 키 `b`가 없다
> ```

키 집합의 일부만 요구하는 *부분* 레코드 타입이 필요하면 `z.partialRecord()`를 쓴다. 이 API는 `z.enum()`과 `z.literal()` 키 스키마에 대해 Zod가 평소 수행하는 전수 검사(exhaustiveness check), 즉 위 참고에서 본 "모든 키가 있는지" 검사를 건너뛴다.

```ts
const Keys = z.enum(["id", "name", "email"]).or(z.never()); 
const Person = z.partialRecord(Keys, z.string());
// { id?: string; name?: string; email?: string }
```

### `z.looseRecord`

기본적으로 `z.record()`는 키 스키마와 맞지 않는 키가 있으면 에러를 낸다. 맞지 않는 키를 그대로 통과시키려면 `z.looseRecord()`를 쓴다.

`z.looseRecord()`는 교차와 함께 쓸 때 특히 유용하다. 키 이름이 특정 패턴(정규식)에 맞는 속성에만 값 스키마를 적용하는 패턴 속성(pattern properties)을 여러 개 모델링할 수 있기 때문이다. 아래 예에서는 `.and()`로 객체 스키마와 `z.looseRecord()`를 교차한다. `_phone`으로 끝나는 키는 값이 E.164 형식 전화번호여야 하고([「전화번호」](02_schemas_primitives.md#전화번호-phone-numbers)), 패턴에 맞지 않는 `name`은 레코드 쪽에서 그냥 통과한 뒤 객체 스키마가 검증한다.

```ts
const schema = z
  .object({ name: z.string() })
  .and(z.looseRecord(z.string().regex(/_phone$/), z.e164()));

type schema = z.infer<typeof schema>;
// => { name: string } & Record<string, string>

schema.parse({ 
  name: "John",
  home_phone: "+12345678900",     // 전화번호로 검증된다
  work_phone: "+12345678900",     // 전화번호로 검증된다
});
```

## 맵 (Maps)

JavaScript `Map`은 `z.map()`에 키 스키마와 값 스키마를 넘겨 검증한다.

```ts
const StringNumberMap = z.map(z.string(), z.number());
type StringNumberMap = z.infer<typeof StringNumberMap>; // Map<string, number>

const myMap: StringNumberMap = new Map();
myMap.set("one", 1);
myMap.set("two", 2);

StringNumberMap.parse(myMap);
```

맵 스키마에는 다음 유틸리티 메서드로 추가 제약을 건다.

```ts
z.map(z.string(), z.number()).nonempty(); // 항목이 1개 이상이어야 한다
z.map(z.string(), z.number()).min(5); // 항목이 5개 이상이어야 한다
z.map(z.string(), z.number()).max(5); // 항목이 5개 이하여야 한다
z.map(z.string(), z.number()).size(5); // 항목이 정확히 5개여야 한다
```

```ts
// Zod Mini
z.map(z.string(), z.number()).check(z.minSize(1)); // .nonempty()의 별칭
z.map(z.string(), z.number()).check(z.minSize(5)); // 항목이 5개 이상이어야 한다
z.map(z.string(), z.number()).check(z.maxSize(5)); // 항목이 5개 이하여야 한다
z.map(z.string(), z.number()).check(z.size(5)); // 항목이 정확히 5개여야 한다
```

## 셋 (Sets)

JavaScript `Set`은 `z.set()`에 원소 스키마를 넘겨 검증한다.

```ts
const NumberSet = z.set(z.number());
type NumberSet = z.infer<typeof NumberSet>; // Set<number>

const mySet: NumberSet = new Set();
mySet.add(1);
mySet.add(2);
NumberSet.parse(mySet);
```

셋 스키마에도 맵과 같은 유틸리티 메서드로 추가 제약을 건다.

```ts
z.set(z.string()).nonempty(); // 항목이 1개 이상이어야 한다
z.set(z.string()).min(5); // 항목이 5개 이상이어야 한다
z.set(z.string()).max(5); // 항목이 5개 이하여야 한다
z.set(z.string()).size(5); // 항목이 정확히 5개여야 한다
```

```ts
// Zod Mini
z.set(z.string()).check(z.minSize(1)); // .nonempty()의 별칭
z.set(z.string()).check(z.minSize(5)); // 항목이 5개 이상이어야 한다
z.set(z.string()).check(z.maxSize(5)); // 항목이 5개 이하여야 한다
z.set(z.string()).check(z.size(5)); // 항목이 정확히 5개여야 한다
```

## 파일 (Files)

`File` 인스턴스는 다음과 같이 검증한다.

```ts
const fileSchema = z.file();

fileSchema.min(10_000); // 최소 .size (바이트)
fileSchema.max(1_000_000); // 최대 .size (바이트)
fileSchema.mime("image/png"); // MIME 타입
fileSchema.mime(["image/png", "image/jpeg"]); // 여러 MIME 타입
```

```ts
// Zod Mini
const fileSchema = z.file();

fileSchema.check(z.minSize(10_000)); // 최소 .size (바이트)
fileSchema.check(z.maxSize(1_000_000)); // 최대 .size (바이트)
fileSchema.check(z.mime("image/png")); // MIME 타입
fileSchema.check(z.mime(["image/png", "image/jpeg"])); // 여러 MIME 타입
```

## 프로미스 (Promises)

> **주의:** **지원 중단(deprecated)** — `z.promise()`는 지원이 중단되었다. `Promise` 스키마가 정말 필요한 경우는 거의 없다. 값이 `Promise`일 수 있다면 Zod로 파싱하기 전에 먼저 `await`하면 된다.

```ts
const numberPromise = z.promise(z.number());
```

프로미스 스키마에서는 "파싱"이 조금 다르게 동작한다. 검증은 두 단계로 이루어진다.

1. Zod가 입력이 Promise 인스턴스(즉 `.then`과 `.catch` 메서드를 가진 객체)인지 동기적으로 검사한다.
2. Zod가 `.then`으로 기존 Promise에 검증 단계를 하나 더 붙여 반환한다. 그래서 값 검증 실패는 `.parse()` 호출 시점이 아니라 반환된 Promise에서 일어나며, 그 Promise에 `.catch`를 달아 처리해야 한다.

아래 예에서 문자열 입력은 1단계에서 바로 실패하지만, `"tuna"`를 담은 Promise는 1단계를 통과하고 `await`할 때 2단계에서 실패한다.

```ts
numberPromise.parse("tuna");
// ZodError: Non-Promise type: string

numberPromise.parse(Promise.resolve("tuna"));
// => Promise<number>

const test = async () => {
  await numberPromise.parse(Promise.resolve("tuna"));
  // ZodError: Non-number type: string

  await numberPromise.parse(Promise.resolve(3.14));
  // => 3.14
};
```

## Instanceof

`z.instanceof`는 입력이 어떤 클래스의 인스턴스인지 검사한다. 서드파티 라이브러리가 내보내는 클래스로 입력을 검증할 때 유용하다.

```ts
class Test {
  name: string;
}

const TestSchema = z.instanceof(Test);

TestSchema.parse(new Test()); // ✅
TestSchema.parse("whatever"); // ❌
```

내장 클래스에도 쓸 수 있다.

```ts
z.instanceof(RegExp);
z.instanceof(URL);
z.instanceof(Error);
```

`z.instanceof()` 스키마로 파싱하면 전달받은 객체를 그대로 반환한다. `z.object()`와 달리 아무것도 복제하지 않으므로 프로토타입과 스키마에 없는 속성이 유지되고, 결과가 입력과 `===`로 같은 객체라는 객체 동일성(identity)도 유지된다.

```ts
const url = new URL("https://example.com");

z.instanceof(URL).parse(url) === url; // true
```

### `z.property()`

`z.property()`를 `.check()`에 넘기면 클래스 인스턴스의 특정 속성을 Zod 스키마로 검증한다. 아래 예는 `URL` 인스턴스의 `protocol`이 `"https:"`인지 확인한다.

```ts
const blobSchema = z.instanceof(URL).check(
  z.property("protocol", z.literal("https:", "Only HTTPS allowed"))
);

blobSchema.parse(new URL("https://example.com")); // ✅
blobSchema.parse(new URL("http://example.com")); // ❌
```

`z.property()` API는 어떤 데이터 타입에도 쓸 수 있지만, `z.instanceof()`와 함께 쓸 때 가장 유용하다. 예를 들어 문자열의 `length` 속성도 이렇게 검사할 수 있다.

```ts
const blobSchema = z.string().check(
  z.property("length", z.number().min(10))
);

blobSchema.parse("hello there!"); // ✅
blobSchema.parse("hello."); // ❌
```

### `z.properties()`

여러 속성을 한 번에 검사한다. 아래처럼 `z.properties()`의 결과를 전개 구문(`...`)으로 `.check()`에 펼쳐 넣는다.

```ts
const okResponse = z.instanceof(Response).check(
  ...z.properties({
    status: z.number().min(200).max(299),
    redirected: z.literal(false),
  })
);

okResponse.parse(new Response("ok")); // ✅
okResponse.parse(new Response("", { status: 404 })); // ❌ status
```

```ts
// Zod Mini
const okResponse = z.instanceof(Response).check(
  ...z.properties({
    status: z.number().check(z.minimum(200), z.maximum(299)),
    redirected: z.literal(false),
  })
);

okResponse.parse(new Response("ok")); // ✅
okResponse.parse(new Response("", { status: 404 })); // ❌ status
```

이 검사는 각 속성을 제자리에서 확인만 한다. 넘긴 shape의 스키마로 검증은 하지만 그 결과는 버리므로, 스키마에 [변환(transform)](04_refinements_transforms_codecs.md#변환-transforms)이나 [기본값(default)](04_refinements_transforms_codecs.md#기본값-defaults)이 있어도 파싱 결과는 바뀌지 않는다.

일반 Zod에서는 `z.instanceof()` 스키마에 `.properties()` 메서드도 있다. 위처럼 `...z.properties()`를 `.check()`에 펼쳐 넣는 것과 같은 검사를 하면서, 추론 타입도 아래처럼 좁혀 준다.

```ts
const okResponse = z.instanceof(Response).properties({
  status: z.number().min(200).max(299),
  redirected: z.literal(false),
});

type OkResponse = z.infer<typeof okResponse>;
// => Response & { status: number; redirected: false }
```
