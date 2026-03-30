# Apache Kafka 4.2.0 릴리스 공지

> **원문:** [Apache Kafka 4.2.0 Release Announcement](https://www.confluent.io/blog/apache-kafka-4-2-release/)
> **저자:** Christo Lolov, PMC Member
> **게시일:** 2026년 2월 20일 | **읽는 시간:** 7분

---

Apache Kafka 4.2의 릴리스를 발표하게 되어 자랑스럽게 생각합니다. 이번 릴리스에는 많은 새로운 기능과 개선 사항이 포함되어 있으며, 이 블로그 포스트에서는 그중 주요한 것들을 소개합니다. 전체 변경 사항 목록은 [릴리스 노트](https://kafka.apache.org/documentation/)를 확인하시기 바랍니다.

Kafka 큐(공유 그룹, Share Groups)가 이제 프로덕션에 사용할 수 있는 수준이 되었으며, 처리 시간 연장을 위한 RENEW 확인 응답 유형, 공유 코디네이터를 위한 적응형 배치, 가져온 레코드 수량의 소프트 및 엄격 적용, 포괄적인 지연(lag) 메트릭 등의 새로운 기능을 제공합니다.

Kafka Streams는 제한된 기능 세트로 서버 측 리밸런스 프로토콜을 GA(정식 출시)로 전환하고, 예외 핸들러에 데드 레터 큐(DLQ) 지원을 추가하며, 결정론적 스케줄링을 위한 앵커드(anchored) 벽시계 시간 기반 펑추에이션(punctuation)을 도입하고, 종료 시 리브 그룹(leave group) 요청 전송 여부에 대한 완전한 제어를 사용자에게 제공합니다.

이번 릴리스는 일관성과 관찰 가능성 측면에서도 상당한 개선을 제공합니다. CLI 도구는 이제 모든 도구에서 `--bootstrap-server`와 같은 표준화된 인수를 지원하고, 메트릭 이름이 `kafka.COMPONENT` 규칙을 따르도록 수정되었으며, 새로운 유휴 비율(idle ratio) 메트릭이 컨트롤러 및 MetadataLoader 성능에 대한 더 나은 가시성을 제공합니다.

보안은 새로운 허용 목록(allowlist) 커넥터 클라이언트 구성 재정의 정책으로 강화되었으며, RecordHeader의 스레드 안전성 개선으로 동시성 위험이 제거되었습니다.

추가 주요 사항으로는 Java 25 지원, 메시지 크기 절감을 위한 JsonConverter의 외부 스키마 지원, 원격 로그 관리자 스레드 풀의 동적 구성, 그룹 코디네이터의 적응형 배치, 그리고 컨슈머 및 공유 그룹 멤버를 위한 Admin API에서의 랙 ID 노출이 포함됩니다.

주요 변경 사항 목록과 상세 업그레이드 단계는 문서의 [4.2로 업그레이드](https://kafka.apache.org/documentation/) 섹션을 참조하십시오. Sandon Jacobs(Confluent 시니어 개발자 애드보킷)이 [이 영상에서 Apache Kafka 4.2의 주요 기능](https://www.confluent.io/blog/apache-kafka-4-2-release/)을 소개합니다.

## 지원 중단(Deprecation) 공지

- **[KIP-1136: ConsumerGroupMetadata를 인터페이스로 변경](https://cwiki.apache.org/confluence/display/KAFKA/KIP-1136)**: ConsumerGroupMetadata의 생성자를 지원 중단합니다. Apache Kafka 5.0에서 제거 예정입니다.

- **[KIP-1193: MX4j 지원 중단](https://cwiki.apache.org/confluence/display/KAFKA/KIP-1193)**: MX4j에 대한 다양한 지원 중단 경고를 추가합니다. Apache Kafka 5.0에서 제거 예정입니다.

- **[KIP-1195: org.apache.kafka.streams.errors.BrokerNotFoundException 지원 중단 및 제거](https://cwiki.apache.org/confluence/display/KAFKA/KIP-1195)**: BrokerNotFoundException을 지원 중단합니다. Apache Kafka 5.0에서 제거 예정입니다.

## Kafka 브로커, 컨트롤러, 프로듀서, 컨슈머 및 Admin 클라이언트

- **[KIP-932: Kafka를 위한 큐](https://cwiki.apache.org/confluence/display/KAFKA/KIP-932)**: 공유 그룹(share groups)을 도입합니다. 이는 여러 컨슈머가 동일한 파티션의 레코드를 개별 확인 응답 및 전달 횟수 추적과 함께 동시에 처리할 수 있는 새로운 협력적 소비 모델로, 엄격한 파티션-컨슈머 할당 없이도 큐와 유사한 사용 사례를 가능하게 합니다.

- **[KIP-1052: 프로듀서 성능 테스트에서 워밍업 활성화](https://cwiki.apache.org/confluence/display/KAFKA/KIP-1052)**: 프로듀서 성능 테스트에 선택적 `--warmup-records` 인수를 추가하여, 시작 측정값을 정상 상태 통계에서 분리해 더 깨끗한 성능 분석을 가능하게 합니다.

- **[KIP-1100: org.apache.kafka.server:type=AssignmentsManager 및 org.apache.kafka.storage.internals.log.RemoteStorageThreadPool 메트릭 이름 변경](https://cwiki.apache.org/confluence/display/KAFKA/KIP-1100)**: 실수로 `org.apache.kafka.COMPONENT` 형식으로 변경된 메트릭을 지원 중단하고 올바른 `kafka.COMPONENT` 규칙을 사용하는 새 메트릭을 도입하여 일관되지 않은 메트릭 이름을 수정합니다.

- **[KIP-1147: 명령줄 인수의 일관성 개선](https://cwiki.apache.org/confluence/display/KAFKA/KIP-1147)**: 모든 도구에서 `--bootstrap-server` 및 `--command-config`와 같은 일관된 옵션을 도입하여 명령줄 도구 인수를 표준화하고, 일관되지 않은 레거시 옵션을 지원 중단합니다.

- **[KIP-1157: KafkaPrincipalBuilder에 대한 KafkaPrincipalSerde 구현 강제](https://cwiki.apache.org/confluence/display/KAFKA/KIP-1157)**: `KafkaPrincipalBuilder`가 `KafkaPrincipalSerde`를 확장하도록 하여, KRaft 브로커-컨트롤러 통신 중 런타임에 실패하는 대신 컴파일 타임에 직렬화/역직렬화 지원을 강제합니다.

- **[KIP-1160: 특정 브로커에서 지원되는 기능 반환 활성화](https://cwiki.apache.org/confluence/display/KAFKA/KIP-1160)**: `describeFeatures`에 선택적 `--node-id` 인수를 추가하여, 노드 간 버전 불일치를 해결하기 위해 특정 노드에서 지원되는 기능을 조회할 수 있게 합니다.

- **[KIP-1161: LIST 타입 구성 검증 및 기본값 통합](https://cwiki.apache.org/confluence/display/KAFKA/KIP-1161)**: null/빈 값을 거부하고, 중복을 무시하며, 문자열 구성을 적절한 LIST 타입으로 변환하고, 런타임이 아닌 파싱 시 ConfigException을 발생시켜 쉼표로 구분된 목록 구성의 검증을 표준화합니다.

- **[KIP-1172: 인수 파서 및 메시지 키/헤더 지원으로 EndToEndLatency 도구 개선](https://cwiki.apache.org/confluence/display/KAFKA/KIP-1172)**: `EndToEndLatency` 도구를 강력한 이름 기반 인수 파싱, 메시지 키 및 헤더를 위한 새로운 선택적 매개변수, Kafka 규칙에 맞게 이름이 변경된 인수로 개선합니다.

- **[KIP-1175: ProducerConfig의 PARTITIONER_ADPATIVE_PARTITIONING_ENABLE 오타 수정](https://cwiki.apache.org/confluence/display/KAFKA/KIP-1175)**: 올바른 철자의 PARTITIONER_ADAPTIVE_PARTITIONING_ENABLE_CONFIG 상수를 도입하고 잘못된 철자 버전을 지원 중단하여 오타를 수정합니다.

- **[KIP-1179: remote.log.manager.follower.thread.pool.size 구성 도입](https://cwiki.apache.org/confluence/display/KAFKA/KIP-1179)**: 적절한 이름 규칙과 동적 구성 지원을 갖춘 새로운 동적 구성 `remote.log.manager.follower.thread.pool.size`를 팔로워 파티션 스레드 풀에 도입합니다.

- **[KIP-1180: 범용 기능 수준 메트릭 추가](https://cwiki.apache.org/confluence/display/KAFKA/KIP-1180)**: 각 프로덕션 기능의 확정된, 최소 지원, 최대 지원 기능 수준을 표시하는 새 메트릭을 추가하여 업그레이드/다운그레이드 시나리오에 대한 가시성을 개선합니다.

- **[KIP-1186: 자동 참여(auto-join)를 지원하도록 AddRaftVoterRequest RPC 업데이트](https://cwiki.apache.org/confluence/display/KAFKA/KIP-1186)**: `AddRaftVoterRequest`에 불리언 `AckWhenCommitted` 플래그를 추가하여, 새 투표자 집합을 로컬에 기록한 후 즉시 응답할 수 있게 하여 자동 참여 컨트롤러 작업 중 가용성 문제를 방지합니다.

- **[KIP-1190: 컨트롤러 스레드 유휴 메트릭 추가](https://cwiki.apache.org/confluence/display/KAFKA/KIP-1190)**: 컨트롤러 스레드가 유휴 상태로 보내는 시간 대비 이벤트를 적극적으로 처리하는 시간의 비율을 측정하는 새로운 `AvgIdleRatio` 메트릭을 추가하여 성능 가시성을 개선합니다.

- **[KIP-1192: ConsumerPerformance 도구에 include 인수 추가](https://cwiki.apache.org/confluence/display/KAFKA/KIP-1192)**: 정규식 기반 토픽 필터링을 위한 `--include` 인수를 ConsumerPerformance에 추가하여 다중 토픽 성능 테스트를 가능하게 합니다.

- **[KIP-1197: TopicBasedRemoteLogMetadataManager의 초기화를 개선하기 위한 새 메서드 도입](https://cwiki.apache.org/confluence/display/KAFKA/KIP-1197)**: 브로커가 완전히 준비될 때까지 초기화를 지연시키는 `BrokerReadyCallback` 인터페이스를 도입하여 `TopicBasedRemoteLogMetadataManager` 초기화 실패를 수정합니다.

- **[KIP-1205: RecordHeader의 스레드 안전성 개선](https://cwiki.apache.org/confluence/display/KAFKA/KIP-1205)**: volatile 필드와 함께 이중 검사 락킹(double-checked locking)을 구현하여 `RecordHeader`의 스레드 안전성 문제를 해결하고, 무시할 수 있는 수준의 오버헤드로 동시 접근 시 `NullPointerException` 위험을 제거합니다.

- **[KIP-1206: 공유 페치에서 엄격한 최대 페치 레코드 수](https://cwiki.apache.org/confluence/display/KAFKA/KIP-1206)**: 공유 페치 작업을 위한 새로운 `ShareAcquireMode` 구성을 도입하여, 다양한 처리 시나리오에 맞는 "batch_optimized"(소프트 제한) 및 "record_limit"(엄격 적용) 모드를 제공합니다.

- **[KIP-1207: KRaft 결합 모드에서 JMX 메트릭 RequestHandlerAvgIdlePercent의 이상 수정](https://cwiki.apache.org/confluence/display/KAFKA/KIP-1207)**: KRaft 결합 모드에서 결합된 스레드 수로 정규화하고 풀별 가시성을 위한 별도의 브로커 및 컨트롤러 메트릭을 도입하여 `RequestHandlerAvgIdlePercent` 메트릭을 수정합니다.

- **[KIP-1217: ClientTelemetryReceiver 컨텍스트에 푸시 간격 포함](https://cwiki.apache.org/confluence/display/KAFKA/KIP-1217)**: 푸시 간격 정보를 포함하는 새 인터페이스를 도입하여 적절한 메트릭 수명 주기 관리를 가능하게 함으로써 오래된 클라이언트 텔레메트리 메트릭 문제를 해결합니다.

- **[KIP-1222: 공유 컨슈머 명시적 모드에서 획득 잠금 타임아웃 갱신](https://cwiki.apache.org/confluence/display/KAFKA/KIP-1222)**: 공유 컨슈머에 새로운 `RENEW` 확인 응답 유형을 추가하여, 더 긴 처리 시간이 필요한 레코드에 대해 애플리케이션이 획득 잠금 타임아웃을 연장할 수 있게 합니다.

- **[KIP-1224: 그룹 코디네이터 및 공유 코디네이터를 위한 적응형 append.linger.ms](https://cwiki.apache.org/confluence/display/KAFKA/KIP-1224)**: 워크로드에 따라 배치 지연 시간을 자동으로 조정하는 그룹 및 공유 코디네이터용 적응형 배치 모드를 도입하여, 수동 튜닝 없이 5ms 지연 하한을 제거합니다.

- **[KIP-1226: 공유 파티션 지연 지속 및 검색 도입](https://cwiki.apache.org/confluence/display/KAFKA/KIP-1226)**: 공유 그룹에 대한 공유 파티션 지연 메트릭을 추가하여, 운영자가 소비 진행 상황을 모니터링하고, 불균형을 감지하며, 향후 자동 확장 기능을 지원할 수 있게 합니다.

- **[KIP-1227: MemberDescription 및 ShareMemberDescription에서 랙 ID 노출](https://cwiki.apache.org/confluence/display/KAFKA/KIP-1227)**: 멤버 설명 클래스에 `rackId` 필드를 추가하여 Admin API에서 컨슈머 및 공유 그룹 멤버의 랙 ID 정보를 노출합니다.

- **[KIP-1228: WriteTxnMarkersRequest에 트랜잭션 버전 추가](https://cwiki.apache.org/confluence/display/KAFKA/KIP-1228)**: WriteTxnMarkersRequest에 TransactionVersion 필드를 추가하여, 트랜잭션 버전 2 마커에 대한 더 엄격한 에포크 검증을 가능하게 하고 정확히 한 번(exactly-once) 의미론 보장을 강화합니다.

- **[KIP-1229: MetadataLoader에 유휴 스레드 비율 메트릭 추가](https://cwiki.apache.org/confluence/display/KAFKA/KIP-1229)**: KRaft 클러스터의 MetadataLoader 컴포넌트에 `AvgIdleRatio` 메트릭을 추가하여 이벤트 큐 처리 효율성에 대한 가시성을 개선합니다.

## Kafka Streams

- **[KIP-1034: Kafka Streams의 데드 레터 큐](https://cwiki.apache.org/confluence/display/KAFKA/KIP-1034)**: DLQ 레코드를 포함하는 새로운 `Response` 클래스, 새로운 `handleError()` 메서드, 그리고 에러 컨텍스트의 원시 소스 레코드 바이트를 도입하여 Kafka Streams 예외 핸들러에 데드 레터 큐(DLQ) 지원을 추가합니다.

- **[KIP-1071: Streams 리밸런스 프로토콜](https://cwiki.apache.org/confluence/display/KAFKA/KIP-1071)**: Kafka Streams를 위한 새로운 서버 측 그룹 관리 프로토콜을 도입하여, 브로커 측 태스크 할당, 중앙 집중식 토폴로지 메타데이터 저장, 전용 RPC 및 관리 도구를 통한 향상된 관찰 가능성을 가능하게 합니다.

- **[KIP-1146: 앵커드 펑추에이션](https://cwiki.apache.org/confluence/display/KAFKA/KIP-1146)**: `schedule()`에 선택적 `startTime` 매개변수를 추가하여 Kafka Streams에 앵커드 벽시계 시간 기반 펑추에이션을 도입함으로써, 고정된 결정론적 시간(예: 정확히 매시 정각)에 콜백을 실행할 수 있게 합니다.

- **[KIP-1153: Kafka Streams CloseOptions를 Fluent API 스타일로 리팩터링](https://cwiki.apache.org/confluence/display/KAFKA/KIP-1153)**: 새로운 `GroupMembershipOperation` 열거형을 통해 KafkaStreams가 종료 시 리브 그룹 요청을 전송할지 여부에 대한 명시적 제어를 사용자에게 제공하며, 지원 중단된 불리언 기반 API를 대체하는 fluent 스타일 `CloseOptions` 클래스로 래핑합니다.

- **[KIP-1216: Kafka Streams에 리밸런스 리스너 메트릭 추가](https://cwiki.apache.org/confluence/display/KAFKA/KIP-1216)**: 전용 스트림 리밸런스 프로토콜로 이동한 후 관찰 가능성을 복원하기 위해, Kafka Streams에서 tasks-revoked, tasks-assigned, tasks-lost 리밸런스 콜백에 대한 스레드 수준 지연 메트릭을 추가합니다.

- **[KIP-1221: Kafka Streams 상태 메트릭에 application-id 태그 추가](https://cwiki.apache.org/confluence/display/KAFKA/KIP-1221)**: Kafka Streams `client-state` JMX 메트릭에 `application-id` 태그를 추가하여, 운영자가 동일한 논리적 애플리케이션에 속하는 여러 인스턴스를 그룹화할 수 있게 합니다.

- **[KIP-1230: 파일 시스템 권한 구성 추가](https://cwiki.apache.org/confluence/display/KAFKA/KIP-1230)**: Kafka Streams에 선택적 `allow.os.group.write.access` 구성을 추가하여, 사용자가 로컬 상태 디렉터리에 대해 OS 사용자 그룹에 쓰기 접근 권한을 부여할 수 있게 합니다.

## Kafka Connect

- **[KIP-1054: JSONConverter에서 외부 스키마 지원](https://cwiki.apache.org/confluence/display/KAFKA/KIP-1054)**: JsonConverter에 선택적 `schema.content` 구성을 추가하여, 모든 JSON 메시지에 스키마를 내장하는 대신 외부에서 지정할 수 있게 합니다. 이를 통해 메시지 크기를 줄이고 일반 JSON 통합을 단순화합니다.

- **[KIP-1120: AppInfo 메트릭에 client-id 누락 수정](https://cwiki.apache.org/confluence/display/KAFKA/KIP-1120)**: Kafka Worker 및 MirrorMaker 2 클라이언트의 AppInfo 메트릭에 `client-id` 태그를 추가하여, 다른 Kafka 클라이언트와의 모니터링 및 디버깅 일관성을 개선합니다.

- **[KIP-1188: 구성 허용 목록이 있는 새 ConnectorClientConfigOverridePolicy](https://cwiki.apache.org/confluence/display/KAFKA/KIP-1188)**: 보안 취약점을 해결하기 위해 새로운 "Allowlist" 커넥터 클라이언트 구성 재정의 정책을 도입하여, 관리자가 커넥터가 재정의할 수 있는 클라이언트 구성을 명시적으로 지정할 수 있게 합니다.

## 요약

Apache Kafka 4.2.0을 시작할 준비가 되셨나요? [업그레이드 노트](https://kafka.apache.org/documentation/)와 [릴리스 노트](https://kafka.apache.org/documentation/)에서 모든 세부 사항을 확인하고, Apache Kafka 4.2.0을 [다운로드](https://kafka.apache.org/downloads)하세요.

이번 릴리스는 커뮤니티의 노력으로 이루어졌습니다. 이 릴리스에 기여해 주신 모든 사용자와 155명의 기여자분들께 감사드립니다:

Abhi Tiwari, Abhijeet Kumar, Abhinav Dixit, Abhiram98, Alex, Alieh Saeedi, ally heev, Alyssa Huang, Andrew J Schofield, Anton Vasanth, Apoorv Mittal, Arpit Goyal, Artem Livshits, Bill Bejeck, Bolin Lin, Bruno Cadonna, Calvin Liu, Chang-Chi Hsu, Chang-Yu Huang, Chia-Ping Tsai, Chih-Yuan Chien, Chirag Wadhwa, Chris Egerton, Christo Lolov, Chuckame, Clemens Hutter, Colin Patrick McCabe, d00791190, Dave Troiano, David Arthur, David Jacot, Deep Golani, Dejan Stojadinovic, devtrace404, Dmitry Werner, Dongnuo Lyu, Donny Nadolny, Eduwer Camacaro, Elizabeth Bennett, EME, Eric Chang, Erik Anderson, Evan Zhou, Evgeniy Kuvardin, farzan ghalami, Fatih, Federico Valeri, Gantigmaa Selenge, Gasparina Damien, Gaurav Narula, Genseric Ghiro, George Wu, Greg Harris, Harish Vishwanath, Herman Kolstad Jakobsen, Hong-Yi Chen, Ismael Juma, Izzy Harker, Jared Harley, Jhen-Yung Hsu, Jian, Jim Galasyn, Jimmy Wang, Jing-Jia Hung, Jinhe Zhang, Joel Hamill, Jonah Hooper, Josep Prat, Jose Armando Garcia Sancio, Juha Mynttinen, Jun Rao, Justine Olshan, k-apol, Kamal Chandraprakash, Kaushik Raina, keemsisi, Ken Huang, Kevin Wu, Kirk True, knoxy5467, KTKTK-HZ, Kuan-Po Tseng, Lan Ding, Levani Kokhreidze, Liam Clarke-Hutchinson, Lianet Magrans, Linsiyuan9, Logan Zhu, lorcan, Lord of Abyss, Lucas Brutschy, Lucy Liu, Luke Chen, Mahsa Seifikar, majialong, Manikumar Reddy, Maros Orsak, Masahiro Mori, Mason Chen, Matt Welch, Matthias J. Sax, Michael Knox, Michael Morris, Mickael Maison, Ming-Yen Chung, NeatGuyCoding, Nick Guo, NICOLAS GUYOMAR, Nikita Shupletsov, Now, Okada Haruki, Omnia Ibrahim, Otmar Ertl, OuO, Paolo Patierno, Patrik Nagy, Pawel Szymczyk, PoAn Yang, Ken Huang, Priyanka K U, Rajani K, Rajini Sivaram, Ram, Ritika Reddy, Robert Young, Ryan Dielhenn, S.Y. Wang, samarth-ksolves, Sanskar Jhajharia, Satish Duggana, Sean Quah, Sebastien Viale, Shang-Hao Yang, Shashank, Shivsundar R, Siyang He, Sophie Blee-Goldman, Stig Dossing, stroller, Sushant Mahajan, TaiJuWu, TengYao Chi, Tsung-Han Ho (Miles Ho), Ubuntu, Uladzislau Blok, Vincent PERICART, Xiao Yang, xijiu, Xuan-Zhang Gong, yangxuze, Yeikel Santana, Yu-Syuan Jheng, YuChia Ma, Yunchi Pang, Yung

---

*이 블로그 포스트는 원래 Christo Lolov가 [Apache 소프트웨어 재단 블로그](https://kafka.apache.org/blog/2026/02/17/apache-kafka-4.2.0-release-announcement/)에 게시한 것입니다.*

*Apache(R), Apache Kafka(R), Kafka(R)는 미국 및/또는 기타 국가에서 Apache 소프트웨어 재단의 등록 상표 또는 상표입니다. 이 마크의 사용이 Apache 소프트웨어 재단의 보증을 의미하지는 않습니다. 기타 모든 상표는 해당 소유자의 재산입니다.*
