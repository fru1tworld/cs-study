# nullable 값을 캐싱할 때 개발자가 범하는 흔한 실수

> 원문: https://blog.jooq.org/a-common-mistake-developers-make-when-caching-nullable-values/

최근 Eclipse 4.7.1a에서 흥미로운 성능 회귀 버그를 발견했습니다. Content assist(자동 완성) 기능을 사용할 때 컴파일러가 `java.util.zip.ZipFile`에 과도하게 접근하는 문제였습니다. 이 문제는 `JavaModelManager`의 `getModuleDescription()` 메서드에서 발생했습니다.

이 메서드는 외부 캐시를 전달받아 이미 저장된 결과를 반환할 수 있습니다. 하지만 모듈 설명이 없는 경우에는 null을 반환할 수도 있습니다. 가장 단순한 코드로 표현하면 다음과 같습니다:

```java
IModuleDescription module = cache.get(root);
if (module != null)
    return module;

// ... 비용이 많이 드는 작업들 ...
module = root.getModuleDescription();

if (module != null)
    cache.put(root, module);
return module;
```

문제가 보이시나요? `module`이 null인 경우에는 아무것도 캐시에 저장되지 않습니다. 다음 번에 이 메서드가 호출되면 `cache.get(root)`는 여전히 null을 반환할 것이고, 이것이 다음 중 어느 경우인지 구분할 수 없게 됩니다:

- 캐시 미스 (키가 존재하지 않음)
- 실제 값이 null인 캐시 히트

이 구분이 중요한 이유는, 비용이 많이 드는 ZipFile 작업이 반복적으로 수행되기 때문입니다. 특히 모듈화되지 않은 Java 코드에서는 `getModuleDescription()`이 항상 null을 반환하므로 이 문제가 더욱 심각해집니다.

## 해결책: 네거티브 캐싱

null 값이 캐시 미스를 의미한다면, 실제 null 값을 인코딩하기 위한 또 다른 "PSEUDO_NULL"이 필요합니다. 이를 센티널(sentinel) 값이라고 합니다:

```java
static final IModuleDescription NO_MODULE =
  new IModuleDescription() { ... };
```

제네릭을 신경 쓰지 않는다면 단순한 `java.lang.Object`를 사용해도 되고, 아니면 위처럼 더미 `IModuleDescription`을 사용할 수 있습니다. 이렇게 하면 동일성(identity) 비교를 통해 확인할 수 있습니다.

수정된 로직은 다음과 같습니다:

```java
IModuleDescription module = cache.get(root);

if (module == NO_MODULE)
    return null;
if (module != null)
    return module;

// ... 비용이 많이 드는 작업들 ...
module = root.getModuleDescription();

if (module != null)
    cache.put(root, module);
else
    cache.put(root, NO_MODULE);

return module;
```

물론 `containsKey()`를 사용하는 것도 논리적으로는 동작하지만, 캐시를 많이 사용하는 시나리오에서는 한 번의 조회가 두 번의 조회보다 항상 낫습니다.

## 핵심 요점

캐시를 구현할 때는 항상 null이 해당 메서드의 유효한 결과인지 확인하세요. 만약 그렇다면, 센티널 객체를 사용하여 null 값을 캐싱해야 합니다. 이 기법을 "네거티브 캐싱(negative caching)"이라고 합니다. 이는 컴파일러와 같이 성능이 중요한 컨텍스트에서 중복된 비용이 큰 작업을 방지하고 성능을 크게 향상시킵니다.

추가로, 예외(Exception) 역시 캐싱 처리가 필요한 유효한 결과입니다. 예외가 발생하는 경우도 기억해둘 가치가 있는 결과이기 때문입니다.

이 버그는 Eclipse 버그 #526209로 보고되었으며 Eclipse 4.7.2에서 수정되었습니다.
