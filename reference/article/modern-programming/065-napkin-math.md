# Napkin Math

> 원문: [sirupsen/napkin-math](https://github.com/sirupsen/napkin-math) (README.md)
> 저자: Simon Eskildsen (sirupsen) | 번역일: 2026-07-10
>
> 이 저장소는 MIT 라이선스로 배포된다. 아래는 README 전체 완역이다(표의 수치는 원문 그대로).

이 프로젝트의 목표는 시스템의 예상 성능을 원리로부터(first-principles) 빠르게 추정하는 데 쓰이는 소프트웨어, 수치, 기법을 모으는 것이다. 예를 들어 1GB의 메모리를 얼마나 빨리 읽을 수 있을까? 이런 자료를 조합하면 "초당 10만 요청(RPS)을 처리하는 애플리케이션의 로깅에 어느 정도의 스토리지 비용을 예상해야 하는가" 같은 흥미로운 질문에 답할 수 있다.

이 기술을 가장 잘 소개하는 자료는 [저자의 SRECON 발표](https://www.youtube.com/watch?v=IxkSlnrRFqc)다.

컴퓨터라는 거대한 영역에서 냅킨 수학(napkin math)을 연습하는 가장 좋은 방법은 자기 자신의 문제를 직접 풀어보는 것이다. 차선책은 **[이 뉴스레터](http://sirupsen.com/napkin)를 구독해 몇 주마다 연습할 문제를 받는 것**이다. 이 기법에 익숙해질수록 문제 하나를 푸는 데 몇 분밖에 걸리지 않을 것이다.

연습용 문제 아카이브는 [여기](https://sirupsen.com/napkin/)에 있다. 풀이는 그다음 뉴스레터에 실린다.

## 수치

아래 수치는 암기하기 좋게 반올림한 값이며, 가짜 정밀도(faux precision)를 나타내지 않는다.
이 저장소가 현재 단일 호스트에서 재측정할 수 있는 행들은 2026년 3월 8일 새로 만든 GCP `c4-standard-48-lssd` 인스턴스(Intel Xeon 6985P-C, 48 vCPU / 물리 코어 24개, RAM 180GB, Ubuntu 22.04.5 LTS)에서 재측정·재검증했다.

**참고 1:** 일부 처리량(throughput)과 지연 시간(latency) 수치는 서로 딱 맞아떨어지지 않는데, 계산을 쉽게 하기 위해 의도적으로 그렇게 둔 것이다.

**참고 2:** 이 수치는 어느 정도 감안하고 봐야 한다. 예를 들어 I/O에서는 [`fio`](https://github.com/axboe/fio)가 최신 기준(state-of-the-art)이다. 저자는 정확도를 높이고 하드웨어 발전을 반영하기 위해 이 수치를 계속 갱신하고 있다.

| 연산 | 지연 시간 | 처리량 | 1 MiB | 1 GiB |
| ----------------------------------- | -------     | ---------- | ------ | ------ |
| 순차 메모리 읽기/쓰기 (64바이트) | 0.5 ns | | | |
| ├ 단일 스레드 | | 20 GiB/s | 50 μs | 50 ms |
| ├ 멀티스레드 | | 200 GiB/s | 5 μs | 5 ms |
| 동일 존(zone) 내 네트워크 | | 10 GiB/s | 100 μs | 100 ms |
| ├ VPC 내부 | | 10 GiB/s | 100 μs | 100 ms |
| ├ VPC 외부 | | 3 GiB/s | 300 μs | 300 ms |
| 해싱, 암호학적으로 안전하지 않음 (64바이트) | 10 ns | 5 GiB/s | 200 μs | 200 ms |
| 랜덤 메모리 읽기/쓰기 (64바이트) | 20 ns | 3 GiB/s | 300 μs | 300 ms |
| 고속 직렬화 `[8]` `[9]` † | N/A | 1 GiB/s | 1 ms | 1s |
| 고속 역직렬화 `[8]` `[9]` † | N/A | 1 GiB/s | 1 ms | 1s |
| 시스템 콜 | 300 ns | N/A | N/A | N/A |
| 해싱, 암호학적으로 안전함 (64바이트) | 100 ns | 1 GiB/s | 1 ms | 1s |
| 순차 SSD 읽기 (8 KiB) | 1 μs | 8 GiB/s | 100 μs | 100 ms |
| 컨텍스트 스위치 `[1] [2]` | 10 μs | N/A | N/A | N/A |
| 순차 SSD 쓰기, -fsync (8KiB) | 2 μs | 3 GiB/s | 300 μs | 300 ms |
| TCP 에코 서버 (32 KiB) | 50 μs | 500 MiB/s | 2 ms | 2s |
| 랜덤 SSD 읽기 (8 KiB) | 100 μs | 70 MiB/s | 15 ms | 15s |
| 압축 해제 `[11]` | N/A | 1 GiB/s | 1 ms | 1s |
| 압축 `[11]` | N/A | 500 MiB/s | 2 ms | 2s |
| 정렬 (64비트 정수) | N/A | 500 MiB/s | 2 ms | 2s |
| 프록시: Envoy/ProxySQL/Nginx/HAProxy | 50 μs | ? | ? | ? |
| 동일 리전 내 네트워크 | 250 μs | 2 GiB/s | 500 μs | 500 ms |
| 프리미엄 네트워크, 존/VPC 내부 | 250 μs | 25 GiB/s | 50 μs | 40 ms |
| 순차 SSD 쓰기, +fsync (8KiB) | 300 μs | 30 MiB/s | 30 ms | 30s |
| {MySQL, Memcached, Redis, ..} 쿼리 | 500 μs | ? | ? | ? |
| 직렬화 `[8]` `[9]` † | N/A | 100 MiB/s | 10 ms | 10s |
| 역직렬화 `[8]` `[9]` † | N/A | 100 MiB/s | 10 ms | 10s |
| 순차 HDD 읽기 (8 KiB) | 10 ms | 250 MiB/s | 2 ms | 2s |
| 랜덤 HDD 읽기 (8 KiB) | 10 ms | 0.7 MiB/s | 2 s | 30m |
| Blob 스토리지 GET, if-not-match 304 | 30 ms | | | |
| Blob 스토리지 GET, 커넥션 1개 (128KiB) | 80 ms | 100 MiB/s | 10 ms | 10s |
| Blob 스토리지 GET, 커넥션 n개 (offset 병렬) | 80 ms | 네트워크 한계 | | |
| Blob 스토리지 LIST | 100 ms | | | |
| Blob 스토리지 PUT, 커넥션 1개 (128KiB) | 200 ms | 100 MiB/s | 10 ms | 10s |
| Blob 스토리지 PUT, 커넥션 n개 (멀티파트) | 200 ms | 네트워크 한계 | 10 ms | 10s |
| 리전 간 네트워크 `[6]` | [상황에 따라 다름][cloudping] | 25 MiB/s | 40 ms | 40s |
| 네트워크 NA Central <-> East | 25 ms | 25 MiB/s | 40 ms | 40s |
| 네트워크 NA Central <-> West | 40 ms | 25 MiB/s | 40 ms | 40s |
| 네트워크 NA East <-> West | 60 ms | 25 MiB/s | 40 ms | 40s |
| 네트워크 EU West <-> NA East | 80 ms | 25 MiB/s | 40 ms | 40s |
| 네트워크 EU West <-> NA Central | 100 ms | 25 MiB/s | 40 ms | 40s |
| 네트워크 NA West <-> Singapore | 180 ms | 25 MiB/s | 40 ms | 40s |
| 네트워크 EU West <-> Singapore | 160 ms | 25 MiB/s | 40 ms | 40s |

[cloudping]: https://www.cloudping.co/

**†:** "고속 직렬화/역직렬화"는 보통 바이트를 그대로 덤프하는 단순한 와이어 프로토콜이거나 매우 효율적인 환경을 뜻한다. JSON 같은 표준 직렬화는 대개 더 느린 쪽에 속한다. 직렬화/역직렬화는 데이터와 구현에 따라 성능 특성이 극단적으로 달라지는 매우 넓은 주제이므로, 이 표에서는 두 경우를 모두 실었다.

활성화된 Criterion 벤치마크 스위트를 돌리려면 `./run --bench napkin_math`를 실행해 올바른 최적화 수준과 Linux 튜닝을 적용하라. 디버그 모드로 컴파일하면 올바른 수치를 얻을 수 없다. 이 래퍼는 내부적으로 이미 `sudo`를 사용한다. 잠긴(locked-down) 클라우드 이미지에서는 실행 전 `sudo sysctl -w kernel.perf_event_paranoid=-1`을 한 번 실행하라. 새 스위트를 추가하고 빈칸을 채워 이 프로젝트에 기여할 수 있다.

**참고:** 현재 활성 벤치마크 경로는 `benches/`의 Criterion.rs다. `src/main.rs`는 여전히 예전 방식의 임시(ad hoc) 하네스이며, 아직 완전히 마이그레이션·재검증되지 않은 벤치마크에 대해서는 여전히 정본(source of truth)이다. 현재 Criterion 스위트에는 `blob_storage`, `memory_read`, `memory_random`, `hash`, `syscall`, `sort`, `serialization`, `compression`, `compressed_memory_read`가 포함된다. 현재 SSD 관련 행들은 `NAPKIN_BENCH_FILE`을 RAID0 로컬 SSD 마운트로 지정한 예전 하네스로 재측정한 값이다. `compressed_memory_read` Criterion 벤치마크는 BitPacker 정수 언팩 마이크로벤치마크이며, 위의 범용 `[11]` 압축/압축 해제 행을 다시 쓰는 데 쓰면 안 된다. 새로운 `serialization`과 `compression` Criterion 그룹은 워크로드에 특화되어 있으며, 아직 위의 범용 README 행에 연결되어 있지 않다.

새로운 `blob_storage` Criterion 그룹은 옵트인(opt-in)이고 자격 증명이 필요하다. `NAPKIN_GCS_BUCKET` 그리고/또는 `NAPKIN_S3_BUCKET`을 설정하라. GCS와 S3 경로 모두 이제 AWS S3 SDK를 사용한다. GCS 쪽은 GCS XML 상호운용 엔드포인트와 통신하며 `NAPKIN_GCS_ACCESS_KEY`와 `NAPKIN_GCS_SECRET_KEY`가 필요하다. S3 쪽은 `NAPKIN_S3_PROFILE`(기본값 `tpuf-test`)의 로컬 AWS 프로파일과 `NAPKIN_S3_REGION`(기본값 `us-west-2`)을 사용한다.

병렬(concurrent) `get_offsets`와 `put_multipart` 경로는 멀티스레드 Tokio 런타임으로 명시적으로 팬아웃(fan out)하므로, 범위(range) 수나 오브젝트 수가 충분히 많으면 호스트 NIC 한계에 근접할 수 있다. 2026년 3월 9일 기준, 동일 리전 `1 GiB` 단일 스트림 GET은 S3(`us-west-2a`의 `m6id.12xlarge`)에서 약 `95 MiB/s`, GCS XML(`us-central1-c`의 `c4-standard-48-lssd`)에서 약 `190-200 MiB/s`를 기록했다. 같은 머신에서 명시적 병렬 range GET은 S3에서 약 `2.0 GiB/s`, GCS에서 약 `4.9 GiB/s`에 도달했고, 멀티파트 PUT은 S3에서 약 `1.8-1.9 GiB/s`, GCS에서 약 `3.3 GiB/s`에 도달했다. AWS 자체의 S3 가이드는 `10 Gbps` 인스턴스를 포화시킬 때 병렬 요청당 약 `85-90 MB/s`를 잡으라고 제안하는데, 이는 측정한 S3 단일 스트림 결과와 거의 일치했다. 기존의 `500 MiB/s` 단일 스트림 blob GET 행은 두 클라우드 제공자 어디서도 재현되지 않아, 보수적인 범용 값 `100 MiB/s`로 하향 수정했다. 현재의 blob 스토리지 작업은 첫 바이트 지연 시간보다는 처리량을 훨씬 더 많이 재검증했다. 위의 `50 ms` / `150 ms` 지연 시간 셀은 소형 객체/첫 바이트 도달 시간(time-to-first-byte) 전용 측정이 추가되기 전까지는 여전히 대략적인 어림값으로 보아야 한다.

이제 `src/bin/s3_latency.rs`에 전용 소형 객체 지연 측정 도구가 있다. `./script/blob-latency s3` 또는 `./script/blob-latency gcs`를 실행하면 기본 `8 KiB .. 8 MiB` 크기 사다리를 따라 `get`, `put`, `if_none_match`, `put_if_match`를 훑는다. 단 `if_none_match`는 작은 크기 범위에서 지연 시간이 사실상 평탄했기 때문에 이제 기본값으로 `128 KiB` 행 하나만 측정한다. 더 작거나 클라우드 제공자에 특화된 실행을 원하면 `NAPKIN_BLOB_LATENCY_OPS`, `NAPKIN_BLOB_LATENCY_SIZES`, `NAPKIN_BLOB_LATENCY_IF_NONE_MATCH_SIZES`를 재정의하라. 이 지연 시간 측정 도구는 이제 행당 기본 `300`초의 실측 시간(wall-clock) 예산을 쓰며, 그 시간 안에 들어가는 만큼 샘플을 수집한다. `NAPKIN_BLOB_LATENCY_ROW_SECONDS`로 이를 조정하고, 개수 제한 방식의 짧은 실험을 원하면 `NAPKIN_BLOB_LATENCY_SAMPLE_CAP`(또는 예전 별칭 `NAPKIN_BLOB_LATENCY_SAMPLES`)로 상한을 둘 수 있다.

`list` 연산은 이제 기본으로 더 큰 네임스페이스(`10만` 키)를 시딩하고, 동일한 작은 고정 프리픽스를 반복 스캔하는 대신 `start_after`를 통해 샘플마다 무작위 `1000`키 페이지 하나를 측정한다. `NAPKIN_BLOB_LATENCY_LIST_NAMESPACE_KEYS`, `NAPKIN_BLOB_LATENCY_LIST_KEYS`, `NAPKIN_BLOB_LATENCY_LIST_SEED_CONCURRENCY`로 이 형태를 조정할 수 있다. 예전처럼 네임스페이스 전체를 훑고 싶다면 `NAPKIN_BLOB_LATENCY_OPS`에 `list_full_scan`을 추가하라.

정렬된(aligned) range-read 지연 시간의 경우, `./script/blob-random-range-latency`는 `128 KiB` 정렬 형태를 측정하고 `./script/blob-random-range-latency-8m`은 동일한 `128 x 1 GiB` 오브젝트 풀에 대해 `8 MiB` 정렬 형태를 측정한다. `./script/blob-random-range-latency-8m-multipart`는 `8 MiB` 파트로 멀티파트 업로드를 통해 그 `1 GiB` 소스 오브젝트들을 시딩한 뒤, 그 멀티파트 경계에 맞춰 `8 MiB` 정렬 읽기를 측정한다.

`memory_read`는 이제 Criterion에서 명시적으로 `No SIMD`와 `SIMD` 변형을 내보내지만, README는 암기하기 쉽도록 의도적으로 이를 단일 스레드 행 하나와 멀티스레드 행 하나로 합쳐 보여준다.

저자는 이 스위트에 일부 비효율이 있음을 알고 있다. 실제 운영 환경에서 짜낼 수 있는 성능의 상한(upper-bound)에 가까운 수치가 되도록 이 영역의 실력을 계속 키울 생각이다. 대부분의 사용자에게는 문제가 되지 않을 정도로, 이 수치들이 2~3배 이상 벗어날 가능성은 매우 낮다고 본다.

## 비용 수치

클라우드 제공자 간에 대체로 일관될 것으로 보이는 대략적인 수치.

| 항목 | 수량 | $/월 | 1년 약정 $/월 | 스팟 $/월 | 시간당 스팟 $ |
| --------------------| ------ | ---------  | ------------------ | ------------- | ------------- |
| CPU | 1 | $15 | $10 | $2 | $0.005 |
| GPU | 1 | $5000 | $3000 | $1500 | $2 |
| 메모리 | 1 GB | $2 | $1 | $0.2 | $0.0005 |
| 스토리지 | | | | | |
| ├ 웨어하우스 스토리지 | 1 GB | $0.02 | | | |
| ├ Blob (S3, GCS) | 1 GB | $0.02 | | | |
| ├ 존(Zonal) HDD | 1 GB | $0.05 | | | |
| ├ 임시(Ephemeral) SSD | 1 GB | $0.08 | $0.05 | $0.05 | $0.07 |
| ├ 리전(Regional) HDD | 1 GB | $0.1 | | | |
| ├ 존(Zonal) SSD | 1 GB | $0.2 | | | |
| ├ 리전(Regional) SSD | 1 GB | $0.35 | | | |
| 네트워킹 | | | | | |
| ├ 동일 존 | 1 GB | $0 | | | |
| ├ Blob | 1 GB | $0 | | | |
| ├ 인그레스(Ingress) | 1 GB | $0 | | | |
| ├ L4 로드밸런서 | 1 GB | $0.008 | | | |
| ├ 존 간(Inter-Zone) | 1 GB | $0.01 | | | |
| ├ 리전 간(Inter-Region) | 1 GB | $0.02 | | | |
| ├ 인터넷 이그레스(Egress) † | 1 GB | $0.1 | | | |
| CDN 이그레스 | 1 GB | $0.05 | | | |
| CDN 필(Fill) ‡ | 1 GB | $0.01 | | | |
| 웨어하우스 쿼리 | 1 GB | $0.005 | | | |
| 로그/트레이스 ♣ | 1 GB | $0.5 | | | |
| 메트릭 | 1000 | $20 | | | |
| EKM 키 | 1 | $1 | | | |

† 이는 클라우드 제공자 바깥으로 나가는 네트워크를 뜻한다. 예를 들어 GCP에서 S3로 데이터를 보내거나, AWS에서 클라이언트로 HTML을 보내는 이그레스 네트워크다.

‡ 캐시를 채울 때마다 추가 요금이 붙는데, blob 스토리지 쓰기 비용에 가까운 수준이다(바로 아래 참고).

로그 관련 수치는 몇몇 로깅 제공자 사이의 표준적인 가격이지만, 예를 들어 [Datadog 가격](https://www.datadoghq.com/pricing/?product=log-management#products)은 다르다. Datadog은 수집(ingest)한 로그 100만 건당 $0.1을 받고, 7일 보관에 대해 100만 건당 $1.5를 추가로 부과한다.

또한 blob 스토리지(S3/GCS/R2/...)는 읽기/쓰기 연산 횟수당 과금된다(파일 수가 적고 큰 쪽이 더 저렴하다).

|  | 100만 건 | 1000건 |
|----------------|---------|----------|
| 읽기 | $0.4 | $0.0004 |
| 쓰기 | $5 | $0.005 |
| EKM 암호화 | $3 | $0.003 |

## 압축 비율

이 수치는 몇몇 출처 `[3]` `[4]` `[5]`에서 가져왔다. 압축 속도는(비율은 대체로 그렇지 않지만) 알고리즘과 압축 수준(속도와 압축률을 맞바꾸는)에 따라 자릿수 단위로 달라진다는 점에 유의하라.

저자는 대략 _압축 비율이 x배 더 높아질 때마다 성능이 10배 낮아진다_고 어림잡는다. 예를 들어 [영어 위키백과에서 2배 비율](https://quixdb.github.io/squash-benchmark/#results-table)을 약 200 MiB/s로, 3배는 약 20MiB/s로, 4배는 1MB/s로 얻을 수 있다.

| 항목 | 압축 비율 |
| ----------- | ----------------- |
| HTML | 2-3배 |
| 영어 텍스트 | 2-4배 |
| 소스 코드 | 2-4배 |
| 실행 파일 | 2-3배 |
| RPC | 5-10배 |
| SSL | -2% `[10]` |

## 기법

- **과도하게 복잡하게 만들지 마라.** 계산의 근거가 되는 가정이 6개를 넘어간다면, 필요 이상으로 어렵게 만들고 있을 가능성이 크다.
- **단위를 유지하라.** 단위는 좋은 검산(checksum) 수단이다. KiB를 TiB로 바꾸는 등의 변환이 필요하면 [Wolframalpha](https://wolframalpha.com)가 훌륭한 도움을 준다.
- **지수로 계산하라.** 어림 계산 대부분은 계수와 지수만으로 이루어진다. 예를 들면 `c * 10^e` 형태다. 목표는 자릿수(order of magnitude), 즉 `e`만 맞추는 것이다. `c`는 훨씬 덜 중요하다. 한 자리 계수와 지수만 신경 쓰면 냅킨 위에서 계산하기가 훨씬 쉬워진다(0을 잔뜩 적을 필요도 없다).
- **페르미 분해(Fermi decomposition)를 수행하라.** 답을 어렴풋이 짐작할 수 있을 때까지, 추측 가능한 요소들을 적어 내려가라. 로깅용 스토리지 비용을 알고 싶다면 로그 한 줄의 크기, 초당 로그 줄 수, 그 비용 등을 순서대로 알아야 할 것이다.

## 참고 자료

- `[1]`: https://eli.thegreenplace.net/2018/measuring-context-switching-and-memory-overheads-for-linux-threads/
- `[2]`: https://blog.tsunanet.net/2010/11/how-long-does-it-take-to-make-context.html
- `[3]`: https://cran.r-project.org/web/packages/brotli/vignettes/brotli-2015-09-22.pdf
- `[4]`: https://github.com/google/snappy
- `[5]`: https://quixdb.github.io/squash-benchmark/
- `[6]`: https://dl.acm.org/doi/10.1145/1879141.1879143
- `[7]`: https://en.wikipedia.org/wiki/Hard_disk_drive_performance_characteristics#Seek_times_&_characteristics
- `[8]`: https://github.com/simdjson/simdjson#performance-results
- `[9]`: https://github.com/protocolbuffers/protobuf/blob/d20e9a92/docs/performance.md
- `[10]`: https://www.imperialviolet.org/2010/06/25/overclocking-ssl.html
- `[11]`: https://github.com/inikep/lzbench
- ["Linux에서 벤치마크할 때 일관된 결과를 얻는 법?"](https://easyperf.net/blog/2019/08/02/Perf-measurement-environment-on-Linux#2-disable-hyper-threading) — CPU 친화도(affinity), 터보 부스트 비활성화 등 신뢰할 수 있는 벤치마킹을 위해 켜고 끌 여러 커널·CPU 기능을 잘 정리한 자료. 올바른 벤치마킹 통계 방법에 대한 자료도 있다.
- [LLVM 벤치마킹 팁](https://www.llvm.org/docs/Benchmarking.html). 위와 비슷하게 CPU 전용 할당, 주소 공간 무작위화 비활성화 등을 다룬다.
- [Top-Down 성능 분석 방법론](https://easyperf.net/blog/2019/02/09/Top-Down-performance-analysis-methodology). `toplev`로 병목을 찾는 유용한 글. 이 저장소의 벤치마크 스위트가 올바르게 작성되었는지 확인하는 데 특히 유용하다(아직 이 스위트에 적용하지는 않았지만 계획 중이다).
- [Godbolt의 컴파일러 익스플로러](https://gcc.godbolt.org/#). Rust와 Clang/GCC의 C 어셈블리를 비교하는 데 훌륭한 자료.
- [cargo-show-asm](https://github.com/pacak/cargo-show-asm). 함수를 역어셈블할 수 있게 해 주는 Cargo 확장. 다만 클로저 지원이 다소 부족해 일부 리팩터링이 필요하다.
- [Agner의 어셈블리 가이드](https://www.agner.org/optimize/optimizing_assembly.pdf). 최적의 어셈블리를 작성하는 훌륭한 자료로, 이 스위트의 여러 함수에서 비효율을 찾아보는 데 유용하다.
- [Agner의 명령어 표](https://www.agner.org/optimize/instruction_tables.pdf). 다양한 명령어의 예상 처리량을 다루는 상세한 자료로, 어셈블리를 살펴볼 때 도움이 된다.
- [halobates.de](http://halobates.de/). `toplev` 저자가 쓴 저수준 성능 관련 유용한 자료.
- [Systems Performance (책)](https://www.amazon.com/Systems-Performance-Enterprise-Brendan-Gregg/dp/0133390098/ref=sr_1_1?keywords=systems+performance&qid=1580733419&sr=8-1). 시스템 성능 분석, 병목 찾기, 운영체제 이해를 다룬 훌륭한 책.
- [io_uring](https://lwn.net/Articles/776703/). 가장 좋은 요약이며 여러 자료로 연결된다.
- [컨텍스트 스위치에 걸리는 시간](https://blog.tsunanet.net/2010/11/how-long-does-it-take-to-make-context.html)
- [정수 압축 비교](https://github.com/powturbo/TurboPFor-Integer-Compression)
- [파일은 어렵다(Files are hard)](https://danluu.com/file-consistency/)
