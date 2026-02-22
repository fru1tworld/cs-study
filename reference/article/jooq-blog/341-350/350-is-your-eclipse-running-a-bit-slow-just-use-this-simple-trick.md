# Eclipse가 좀 느리게 실행되고 있는가? 이 간단한 트릭을 사용하라!

> 원문: https://blog.jooq.org/is-your-eclipse-running-a-bit-slow-just-use-this-simple-trick/

나는 항상 내 컴파일 시간이 느린 것에 대해 m2e 통합을 탓해왔다. 최근에 jOOQ 통합 테스트에 Hibernate를 추가하고 JPA persistence.xml 파일을 추가한 이후로 상황이 더 느려졌다.

밝혀진 바에 의하면, 이것은 이미 잘 알려진 문제로, 많은 사람들이 Eclipse에서 JPA 검증이 영원히 실행되는 것을 경험하고 있다. [StackOverflow에서 이 질문을 확인해보라](http://stackoverflow.com/q/7060131/521799).

## 해결책: 모든 검증 비활성화

Eclipse에서 검증 기능을 완전히 비활성화하면 된다. 솔직히 말해서, 나는 검증이 필요했거나 애초에 유용한 기능이라고 생각했던 마지막 순간이 언제인지 기억나지 않는다.

검증을 비활성화한 후, 내 모든 Eclipse가 훨씬 훨씬 빨라졌다.

## 구현 방법

### GUI를 통한 방법

Eclipse 환경설정으로 이동하여 검증 설정을 비활성화할 수 있다. Preferences > Validation으로 가서 모든 검증기를 비활성화하면 된다.

### 설정 파일을 통한 방법

버전 관리 시스템을 통해 전체 팀에 이 설정을 공유하고 싶다면, 프로젝트 디렉토리의 `.settings/org.eclipse.wst.validation.prefs` 파일에 다음 설정을 추가하면 된다:

```
DELEGATES_PREFERENCE=delegateValidatorList
USER_BUILD_PREFERENCE=enabledBuildValidatorListorg.eclipse.wst.wsi.ui.internal.WSIMessageValidator;
USER_MANUAL_PREFERENCE=enabledManualValidatorListorg.eclipse.wst.wsi.ui.internal.WSIMessageValidator;
USER_PREFERENCE=overrideGlobalPreferencestruedisableAllValidationtrueversion1.2.600.v201501211647
eclipse.preferences.version=1
override=true
suspend=true
vf.version=3
```

핵심은 `suspend=true` 설정으로, 이것이 모든 검증을 중단시킨다.

## 결론

만약 Eclipse가 느리게 실행되고 있다면, 검증 기능을 비활성화해보라. 특히 JPA persistence.xml 파일이 있는 프로젝트에서 작업하고 있다면 이 방법이 큰 도움이 될 것이다. 이 간단한 트릭으로 Eclipse의 성능을 극적으로 향상시킬 수 있다.
