# Kafka 빠른 시작

> 이 문서는 Apache Kafka 공식 문서의 "Quickstart" 섹션을 한국어로 번역한 것입니다.
> 원본: https://kafka.apache.org/quickstart

## 개요

이 튜토리얼은 Apache Kafka를 빠르게 시작하기 위한 가이드입니다. 메시지를 게시하고 소비하는 기본적인 기능부터 Kafka Connect와 Kafka Streams를 활용한 데이터 파이프라인 구축까지 단계별로 안내합니다.

---

## 1단계: Kafka 다운로드

최신 Kafka 릴리스를 다운로드하고 압축을 해제합니다:

```bash
$ tar -xzf kafka_2.13-4.1.1.tgz
$ cd kafka_2.13-4.1.1
```

---

## 2단계: Kafka 환경 시작

요구 사항: Java 17 이상이 로컬에 설치되어 있어야 합니다.

### 옵션 A: 다운로드한 파일 사용

클러스터 식별자(Cluster ID)를 생성합니다:

```bash
$ KAFKA_CLUSTER_ID="$(bin/kafka-storage.sh random-uuid)"
```

로그 디렉토리를 포맷합니다:

```bash
$ bin/kafka-storage.sh format --standalone -t $KAFKA_CLUSTER_ID -c config/server.properties
```

서버를 실행합니다:

```bash
$ bin/kafka-server-start.sh config/server.properties
```

### 옵션 B: Docker (JVM 기반)

```bash
$ docker pull apache/kafka:4.1.1
$ docker run -p 9092:9092 apache/kafka:4.1.1
```

### 옵션 C: Docker (GraalVM Native)

```bash
$ docker pull apache/kafka-native:4.1.1
$ docker run -p 9092:9092 apache/kafka-native:4.1.1
```

---

## 3단계: 토픽(Topic) 생성

Kafka는 분산 이벤트 스트리밍 플랫폼으로서 데이터를 토픽(Topic)으로 구성합니다. 새 터미널을 열고 다음 명령을 실행합니다:

```bash
$ bin/kafka-topics.sh --create --topic quickstart-events --bootstrap-server localhost:9092
```

토픽 상세 정보를 확인합니다:

```bash
$ bin/kafka-topics.sh --describe --topic quickstart-events --bootstrap-server localhost:9092
```

---

## 4단계: 이벤트 쓰기 (Producer)

콘솔 프로듀서(Console Producer)를 사용하여 메시지를 전송합니다:

```bash
$ bin/kafka-console-producer.sh --topic quickstart-events --bootstrap-server localhost:9092
>This is my first event
>This is my second event
```

메시지 입력을 중지하려면 `Ctrl-C`를 누릅니다.

---

## 5단계: 이벤트 읽기 (Consumer)

다른 터미널을 열고 메시지를 소비합니다:

```bash
$ bin/kafka-console-consumer.sh --topic quickstart-events --from-beginning --bootstrap-server localhost:9092
```

생성된 메시지가 표시됩니다. `Ctrl-C`로 컨슈머를 중지할 수 있습니다.

---

## 6단계: Kafka Connect를 사용한 데이터 가져오기/내보내기

Kafka Connect는 외부 시스템과 Kafka 간에 데이터를 스트리밍할 수 있게 해주는 도구입니다.

### 플러그인 경로 설정

`config/connect-standalone.properties` 파일에 플러그인 경로를 설정합니다:

```bash
$ echo "plugin.path=libs/connect-file-4.1.1.jar" >> config/connect-standalone.properties
```

### 테스트 데이터 생성

```bash
$ echo -e "foo\nbar" > test.txt
```

### 커넥터 시작

```bash
$ bin/connect-standalone.sh config/connect-standalone.properties config/connect-file-source.properties config/connect-file-sink.properties
```

### 결과 확인

```bash
$ more test.sink.txt
```

`test.txt` 파일의 내용이 `test.sink.txt` 파일에 복사된 것을 확인할 수 있습니다.

---

## 7단계: Kafka Streams로 이벤트 처리

Kafka Streams는 Java 및 Scala 애플리케이션에서 이벤트를 실시간으로 처리하고 변환하기 위한 클라이언트 라이브러리입니다.

### 주요 특징

- 스트림 처리: 이벤트 스트림을 실시간으로 처리
- 상태 저장: 로컬 상태 저장소를 통한 상태 관리
- 정확히 한 번 처리: Exactly-Once Semantics 지원
- 폴트 톨러런스: 장애 발생 시 자동 복구

### WordCount 예제

Kafka Streams의 대표적인 예제인 WordCount는 입력 토픽에서 텍스트를 읽어 단어별로 집계하여 출력 토픽에 기록합니다. 이 예제는 데이터를 실시간으로 변환하고 집계하는 방법을 보여줍니다.

---

## 8단계: 종료

모든 컴포넌트를 중지하려면 `Ctrl-C`를 누릅니다.

### 로컬 데이터 정리 (선택 사항)

로컬 환경의 모든 Kafka 데이터를 삭제하려면:

```bash
$ rm -rf /tmp/kafka-logs /tmp/kraft-combined-logs
```

주의: 이 명령은 토픽에 저장된 모든 데이터를 영구적으로 삭제합니다.

---

## 핵심 개념 요약

### Producer (프로듀서)
메시지를 Kafka 토픽에 게시하는 클라이언트입니다.

### Consumer (컨슈머)
Kafka 토픽에서 메시지를 읽는 클라이언트입니다.

### Topic (토픽)
메시지가 저장되는 카테고리 또는 피드입니다. 토픽은 여러 파티션으로 분할될 수 있습니다.

### Broker (브로커)
Kafka 클러스터를 구성하는 서버입니다. 메시지를 저장하고 클라이언트 요청을 처리합니다.

### Kafka Connect
외부 시스템과 Kafka 간의 데이터 통합을 위한 프레임워크입니다.

### Kafka Streams
실시간 스트림 처리를 위한 클라이언트 라이브러리입니다.

---

## 다음 단계

Kafka 빠른 시작을 완료했습니다! 다음 주제를 계속 탐색해 보세요:

- [Kafka 설계](https://kafka.apache.org/documentation/#design): Kafka의 내부 설계와 아키텍처 학습
- [Kafka 설정](https://kafka.apache.org/documentation/#configuration): 상세한 설정 옵션 탐색
- [Kafka API](https://kafka.apache.org/documentation/#api): Producer, Consumer, Streams, Connect API 학습
- [Kafka 운영](https://kafka.apache.org/documentation/#operations): 프로덕션 환경에서의 Kafka 운영 가이드

---

## 참고 자료

- [Apache Kafka 공식 문서](https://kafka.apache.org/documentation/)
- [Kafka Quickstart](https://kafka.apache.org/quickstart)
- [Kafka Downloads](https://kafka.apache.org/downloads)
- [Kafka GitHub Repository](https://github.com/apache/kafka)
