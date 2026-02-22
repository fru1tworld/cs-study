# 스키마 메타 데이터가 Oracle 쿼리 변환에 미치는 영향

> 원문: https://blog.jooq.org/how-schema-meta-data-impacts-oracle-query-transformations/

최근 두 테이블 사이에서 겪은 어떤 문제에 대해 고민하고 있었습니다. 테이블에 INSERT / UPDATE / DELETE 문이 많이 발생하는 경우, 적어도 데이터 로딩 기간 동안은 일부 제약 조건을 제거하는 것이 더 나아 보일 수 있습니다. 이 특정 사례에서는 외래 키 관계가 영구적으로 없는 상태였고, 두 테이블 간의 조인이 더 큰 컨텍스트에서 잘못된 쿼리 실행 계획의 잠재적 원인이 된다는 것을 발견했습니다. 그래서 제 직감은 제약 조건을 다시 추가하면 이를 최적화할 수 있을 것이라고 말해주었습니다. Oracle이 그 정보를 쿼리 변환에 공식적으로 활용할 수 있게 될 테니까요. Stack Overflow에 질문을 올렸고, 제 생각이 맞았던 것으로 보입니다:

[https://stackoverflow.com/questions/8153674/do-foreign-key-constraints-influence-query-transformations-in-oracle](https://stackoverflow.com/q/8153674/521799)

하지만 이것이 무엇을 의미할까요? 간단합니다. 두 테이블 A와 B가 있고 A.ID = B.A_ID로 조인한다면, B.A_ID에 외래 키 제약 조건이 있느냐 없느냐가 큰 차이를 만들 수 있습니다. 다음 쿼리를 실행한다고 가정해 봅시다:

```sql
select B.* from B
join A on A.ID = B.A_ID
```

### B.A_ID에 외래 키가 없는 경우

B.A_ID가 설정되어 있더라도(즉, null이 아니더라도), 실제로 대응하는 A.ID가 존재한다는 보장은 없습니다. A는 프로젝션(projection)의 일부가 아닙니다. 직관적으로는 A가 필요하지 않지만, 최적화하여 제거할 수 없습니다. 왜냐하면 쿼리가 실제로 B.A_ID마다 존재하는 A.ID를 확인해야 하기 때문입니다.

### 외래 키 제약 조건이 있는 경우

B.A_ID가 설정되어 있으면, 대응하는 고유한 A.ID가 반드시 존재합니다. 따라서 이 경우 JOIN을 무시할 수 있습니다. 이것은 모든 종류의 변환 연산에 강력한 영향을 미칩니다. 더 자세한 내용은 Tom Kyte의 프레젠테이션 ["Meta Data Matters"](http://asktom.oracle.com/pls/apex/z?p_url=ASKTOM.download_file?p_file=8322231124282761811&p_cat=MetadataMatters.ppt&p_company=822925097021874)를 참조하세요.
