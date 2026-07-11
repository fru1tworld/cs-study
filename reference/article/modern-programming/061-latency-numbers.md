# 모든 프로그래머가 알아야 할 지연 시간 수치

> 원문: [Latency Numbers Every Programmer Should Know](https://gist.github.com/jboner/2841832)
> 저자: Jeff Dean (정리: Jonas Bonér) | 번역일: 2026-07-10

```text
지연 시간 비교 수치 (~2012)
----------------------------------
L1 캐시 참조                                 0.5 ns
분기 예측 실패                               5   ns
L2 캐시 참조                                 7   ns                      L1 캐시의 14배
뮤텍스 잠금/해제                            25   ns
메인 메모리 참조                           100   ns                      L2 캐시의 20배, L1 캐시의 200배
Zippy로 1KB 압축                         3,000   ns        3 us
1Gbps 네트워크로 1KB 전송               10,000   ns       10 us
SSD에서 4K 랜덤 읽기*                  150,000   ns      150 us          ~1GB/sec SSD
메모리에서 1MB 순차 읽기               250,000   ns      250 us
동일 데이터센터 내 왕복                500,000   ns      500 us
SSD에서 1MB 순차 읽기*               1,000,000   ns    1,000 us    1 ms  ~1GB/sec SSD, 메모리의 4배
디스크 탐색(seek)                   10,000,000   ns   10,000 us   10 ms  데이터센터 왕복의 20배
로컬 LLM, 토큰 1개 생성             15,000,000   ns   15,000 us   15 ms  소비자용 GPU의 소형 모델 (2026)
디스크에서 1MB 순차 읽기            20,000,000   ns   20,000 us   20 ms  메모리의 80배, SSD의 20배
프런티어 LLM, 토큰 1개 생성         20,000,000   ns   20,000 us   20 ms  호스팅 모델 출력 (2026)
로컬 LLM, 첫 토큰까지 시간          75,000,000   ns   75,000 us   75 ms  소형 모델, 짧은 프롬프트 (2026)
로컬 LLM(CPU), 토큰 1개 생성       100,000,000   ns  100,000 us  100 ms  소형 모델, GPU 없음 (2026)
패킷 전송 CA->네덜란드->CA         150,000,000   ns  150,000 us  150 ms
고속 LLM, 첫 토큰까지 시간         250,000,000   ns  250,000 us  250 ms  전용 추론 하드웨어 (2026)
프런티어 LLM, 첫 토큰까지 시간                                 1,000 ms  짧은 프롬프트, 캐시 없음 (2026)
프런티어 LLM, 짧은 응답                                        3,000 ms  출력 토큰 약 100개 (2026)
프런티어 LLM, 긴 컨텍스트 프리필                              10,000 ms  입력 토큰 약 10만 개, 캐시 없음 (2026)
프런티어 LLM, 추론(reasoning) 응답                            30,000 ms  thinking을 포함한 단일 호출 (2026)
```

## 참고

- 1 ns = 10^-9 초
- 1 us = 10^-6 초 = 1,000 ns
- 1 ms = 10^-3 초 = 1,000 us = 1,000,000 ns

## 크레딧

- 작성: Jeff Dean — http://research.google.com/people/jeff/
- 원 출처: Peter Norvig — http://norvig.com/21-days.html#answers

## 기여

- 'Humanized' 비교 버전: https://gist.github.com/hellerbarde/2843375
- 시각 비교 차트: http://i.imgur.com/k0t1e.png
- 인터랙티브 Prezi 버전: https://prezi.com/pdkvgys-r0y6/latency-numbers-for-programmers-web-development/latency.txt
