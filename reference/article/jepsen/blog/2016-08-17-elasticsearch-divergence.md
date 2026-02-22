# ElasticSearch 발산 문제

날짜: 2016-08-16

Crate.io를 위한 연구 과정에서 Elasticsearch의 심각한 데이터 무결성 문제들이 발견되었습니다.

## 발견된 문제

발견된 문제는 다음을 포함합니다:
- 더티 읽기(dirty reads)
- 복제본 발산(replica divergence)
- 손실된 업데이트(lost updates)

이 문제들은 GitHub 이슈 #20031에 자세히 기록되어 있습니다.
