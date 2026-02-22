# Oracle Java Mission Control로 무료 Java 프로파일링

> 원문: https://blog.jooq.org/free-java-profiling-with-oracle-java-mission-control/

여러분은 JProfiler나 YourKit과 같은 상용 도구를 사용하여 코드를 프로파일링하시나요? 이러한 라이선스는 미묘한 성능 병목 현상을 찾는 데 매우 가치 있습니다.

## 상용 프로파일러의 가치

프로파일러를 사용하면 jOOQ에서 리플렉션과 관련된 심각한 성능 이슈를 식별할 수 있었습니다. 초기 프로파일링 결과 `RecordMapper.map()` 메서드가 과도한 리소스를 소비하고 있었고, `DefaultRecordMapperProvider.provide()` 메서드가 불필요한 초기화를 수행하고 있었습니다. 벤치마크 시간의 96%가 이 부분에서 소비되고 있었던 것입니다.

이 문제를 수정한 후, 벤치마크는 134초에서 1.4초로 대폭 빨라졌습니다.

프로파일러는 비용이 들고, 라이선스 비용이 부담이 된다면 좋은 소식이 있습니다!

## Oracle Java Mission Control

JDK 7u40부터 Oracle은 Hotspot VM용 Oracle Java Mission Control(JMC)을 함께 제공하며, 개발 환경에서 무료로 사용할 수 있습니다(프로덕션에서는 제외).

솔직히 말해서, JMC는 아직 JProfiler나 YourKit만큼 강력하지는 않습니다. 하지만 비용을 절약하고 싶다면 이것이 좋은 대안입니다. JMX 콘솔만 바라보거나 콘솔에 무작위로 스레드 덤프를 던지는 것보다는 훨씬 낫습니다.

## 결론

상용 프로파일러를 구매할 예산이 없더라도, Oracle Java Mission Control을 사용하면 개발 단계에서 중요한 성능 이슈를 효과적으로 식별하고 해결할 수 있습니다. 무료 프로파일링 도구라도 적절히 활용하면 놀라운 성능 개선을 달성할 수 있습니다.
