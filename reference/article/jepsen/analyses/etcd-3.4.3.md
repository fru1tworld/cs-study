# etcd 3.4.3 Jepsen 분석

## 개요
Kyle Kingsbury가 2020년 1월 30일에 작성한 이 보고서는 etcd 3.4.3 버전을 검사했습니다. 이전 2014년 분석에서 etcd 0.4.1이 부실 읽기(stale reads) 문제를 가졌던 것과 달리, 이번 분석에서는 주요 발견사항이 있었습니다.

## 핵심 결과

### 긍정적 평가
- 핵심-값 연산이 "엄격한 직렬화 가능성(strict serializability)"을 제공
- watch 기능이 순서대로 모든 변경사항 전달
- 읽기, 쓰기, 다중 키 트랜잭션이 안전함

### 문제점
"etcd locks (like all distributed locks) do not provide mutual exclusion"

이는 구조적 문제로, 프로세스 정지, 네트워크 분할 등에서 여러 클라이언트가 동시에 같은 lock을 소유 가능합니다.

## 주요 발견

### 1. Watch Revision 0 문제
- revision 0으로 watch 시작 시 현재 revision 이후부터 시작됨
- 문서화되지 않은 동작으로 사용자 혼동 야기

### 2. Lock 메커니즘 결함
- 다중 프로세스가 동시에 같은 lock 획득 가능
- 짧은 TTL(1-3초)에서 특히 문제
- lease 유효성을 재확인하지 않는 버그 존재

시스템이 lock 대기 중 lease를 잃으면 "lease가 여전히 유효한지 재확인하지 않음"

### 3. Lock 사용 권장사항
실제 안전성이 아닌 성능 목적으로만 사용하도록 권고합니다. 공유 자원 보호가 필요한 경우, etcd revision을 fencing token으로 사용하여 추가 안전성을 확보해야 합니다.

## 테스트 방법론
5노드 클러스터에서 네트워크 분할, 프로세스 정지, 시계 스큐 등을 도입하여 register, set, append, lock, watch 테스트 수행했습니다.

## etcd 팀의 개선
분석 후 문서화 개선이 진행되었으며, API 보증 페이지에서 "strict serializable"을 기본값으로 명확히 했습니다.
