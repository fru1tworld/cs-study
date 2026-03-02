# 코루틴과 채널 튜토리얼

## 개요

이것은 스레드를 차단하거나 콜백을 사용하지 않고 네트워크 요청을 수행하기 위해 코루틴과 채널을 사용하는 포괄적인 Kotlin 튜토리얼입니다.

**전제 조건:** 기본 Kotlin 구문 지식

**학습 목표:**
- 네트워크 요청에 일시 중단 함수 사용
- 코루틴을 사용하여 요청을 동시에 전송
- 채널을 사용하여 코루틴 간 정보 공유

---

## 섹션 1: 시작하기 전에

### 설정 요구 사항
1. 최신 버전의 IntelliJ IDEA 다운로드
2. 프로젝트 템플릿 클론:
```bash
git clone https://github.com/kotlin-hands-on/intro-coroutines
```

### GitHub 개발자 토큰 생성
1. https://github.com/settings/tokens/new 로 이동
2. "coroutines-tutorial" 이름의 토큰 생성
3. 어떤 스코프도 선택하지 않음
4. 생성된 토큰 복사

### 초기 프로그램 실행
1. `src/contributors/main.kt` 열기
2. GitHub 사용자 이름과 토큰 제공
3. BLOCKING 변형 선택
4. "Load contributors" 클릭
5. 출력 콘솔에서 데이터가 로드되는지 확인

---

## 섹션 2: 블로킹 요청

### GitHubService 인터페이스
```kotlin
interface GitHubService {
    @GET("orgs/{org}/repos?per_page=100")
    fun getOrgReposCall(
        @Path("org") org: String
    ): Call<List<Repo>>

    @GET("repos/{owner}/{repo}/contributors?per_page=100")
    fun getRepoContributorsCall(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Call<List<User>>
}
```

### loadContributorsBlocking() 구현
```kotlin
fun loadContributorsBlocking(
    service: GitHubService,
    req: RequestData
): List<User> {
    val repos = service
        .getOrgReposCall(req.org)      // #1
        .execute()                      // #2 - 블로킹 호출
        .also { logRepos(req, it) }     // #3
        .body() ?: emptyList()          // #4

    return repos.flatMap { repo ->
        service
            .getRepoContributorsCall(req.org, repo.name)
            .execute()
            .also { logUsers(repo, it) }
            .bodyList()
    }.aggregate()
}
```

**핵심 포인트:**
- `execute()`는 동기적이며 스레드를 차단
- 모든 요청이 메인 UI 스레드에서 순차적으로 실행
- 로딩이 완료되는 동안 UI가 멈춤
- 로그 출력은 모든 연산이 동일 스레드에서 수행됨을 보여줌 (AWT-EventQueue-0)

### 확장 함수
```kotlin
fun <T> Response<List<T>>.bodyList(): List<T> {
    return body() ?: emptyList()
}
```

### 태스크 1: 집계 구현
**목표:** 중복 사용자를 결합하고 기여도 집계

**해결책:**
```kotlin
fun List<User>.aggregate(): List<User> =
    groupBy { it.login }
        .map { (login, group) ->
            User(login, group.sumOf { it.contributions })
        }
        .sortedByDescending { it.contributions }
```

`groupingBy()` 사용 대안:
```kotlin
fun List<User>.aggregate(): List<User> =
    groupingBy { it.login }
        .fold(0) { acc, user -> acc + user.contributions }
        .map { (login, contributions) -> User(login, contributions) }
        .sortedByDescending { it.contributions }
```

---

## 섹션 3: 콜백

### 블로킹의 문제점
- I/O 중에 메인 스레드가 차단됨
- UI가 응답하지 않게 됨
- 나쁜 사용자 경험

### 해결책 1: 백그라운드 스레드
```kotlin
fun loadContributorsBackground(
    service: GitHubService,
    req: RequestData,
    updateResults: (List<User>) -> Unit
) {
    thread {
        updateResults(loadContributorsBlocking(service, req))
    }
}

// 사용법
loadContributorsBackground(service, req) { users ->
    SwingUtilities.invokeLater {
        updateResults(users, startTime)
    }
}
```

**이점:** 메인 UI 스레드가 응답성을 유지

### 해결책 2: Retrofit 콜백 API

```kotlin
fun loadContributorsCallbacks(
    service: GitHubService,
    req: RequestData,
    updateResults: (List<User>) -> Unit
) {
    service.getOrgReposCall(req.org).onResponse { responseRepos ->  // #1
        logRepos(req, responseRepos)
        val repos = responseRepos.bodyList()
        val allUsers = mutableListOf<User>()

        for (repo in repos) {
            service.getRepoContributorsCall(req.org, repo.name)
                .onResponse { responseUsers ->                     // #2
                    logUsers(repo, responseUsers)
                    val users = responseUsers.bodyList()
                    allUsers += users
                }
        }
    }
    updateResults(allUsers.aggregate())
}
```

**문제:** 비동기 응답이 도착하기 전에 `updateResults()`가 호출됨

### 동시 콜백 해결책

**CountDownLatch 사용 (최선):**
```kotlin
val countDownLatch = CountDownLatch(repos.size)

for (repo in repos) {
    service.getRepoContributorsCall(req.org, repo.name)
        .onResponse { responseUsers ->
            // 레포지토리 처리
            countDownLatch.countDown()
        }
}

countDownLatch.await()
updateResults(allUsers.aggregate())
```

---

## 섹션 4: 일시 중단 함수

### 새로운 GitHub 서비스 API
```kotlin
interface GitHubService {
    @GET("orgs/{org}/repos?per_page=100")
    suspend fun getOrgRepos(
        @Path("org") org: String
    ): Response<List<Repo>>

    @GET("repos/{owner}/{repo}/contributors?per_page=100")
    suspend fun getRepoContributors(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Response<List<User>>
}
```

**주요 차이점:**
- `suspend` 키워드로 표시됨
- 결과를 직접 반환 (Call로 래핑되지 않음)
- 일시 중단 중에 스레드가 차단되지 않음
- 오류 시 예외가 던져짐

### 일시 중단 함수 구현

**해결책:**
```kotlin
suspend fun loadContributorsSuspend(
    service: GitHubService,
    req: RequestData
): List<User> {
    val repos = service
        .getOrgRepos(req.org)
        .also { logRepos(req, it) }
        .bodyList()

    return repos.flatMap { repo ->
        service
            .getRepoContributors(req.org, repo.name)
            .also { logUsers(repo, it) }
            .bodyList()
    }.aggregate()
}
```

**참고:** 순차적 실행; 모든 요청이 이전 요청 완료를 기다림

---

## 섹션 5: 코루틴

### 개념
- **코루틴:** 일시 중지하고 재개할 수 있는 일시 중단 가능한 계산
- **일시 중단:** 계산을 일시 중지하고 스레드를 다른 작업에 해제
- 스레드에 비해 경량
- `launch`, `async`, 또는 `runBlocking`으로 시작 가능

### 코루틴 시작

**launch:** 결과를 반환하지 않고 계산 시작
```kotlin
launch {
    val users = loadContributorsSuspend(req)
    updateResults(users, startTime)
}
```

**async:** 계산을 시작하고 Deferred<T> 반환
```kotlin
val deferred: Deferred<Int> = async {
    loadData()
}
println(deferred.await())
```

**runBlocking:** 일반 함수와 일시 중단 함수 사이의 브릿지
```kotlin
fun main() = runBlocking {
    val deferred = async { loadData() }
    println(deferred.await())
}

suspend fun loadData(): Int {
    delay(1000L)
    return 42
}
```

### 다중 Async 호출
```kotlin
fun main() = runBlocking {
    val deferreds: List<Deferred<Int>> = (1..3).map {
        async {
            delay(1000L * it)
            println("Loading $it")
            it
        }
    }
    val sum = deferreds.awaitAll().sum()
    println("$sum")
}
```

---

## 섹션 6: 동시성

### 동시 로딩

**목표:** 모든 레포지토리를 동시에 로드

**해결책:**
```kotlin
suspend fun loadContributorsConcurrent(
    service: GitHubService,
    req: RequestData
): List<User> = coroutineScope {
    val repos = service
        .getOrgRepos(req.org)
        .also { logRepos(req, it) }
        .bodyList()

    val deferreds: List<Deferred<List<User>>> = repos.map { repo ->
        async {
            service.getRepoContributors(req.org, repo.name)
                .also { logUsers(repo, it) }
                .bodyList()
        }
    }

    deferreds.awaitAll().flatten().aggregate()
}
```

### 다른 디스패처 사용

**Dispatchers.Default:** 병렬 실행을 위한 스레드 풀
```kotlin
async(Dispatchers.Default) {
    service.getRepoContributors(req.org, repo.name)
        .also { logUsers(repo, it) }
        .bodyList()
}
```

**Dispatchers.Main:** 메인 UI 스레드
```kotlin
launch(Dispatchers.Main) {
    updateResults()
}
```

### 완전한 구현 패턴
```kotlin
launch(Dispatchers.Default) {
    val users = loadContributorsConcurrent(service, req)
    withContext(Dispatchers.Main) {
        updateResults(users, startTime)
    }
}
```

**동시 로딩의 이점:**
- 순차적: ~4초
- 동시적: ~2초 (서버에 따라 다름)
- 더 효율적인 스레드 사용

---

## 섹션 7: 구조적 동시성

### 개념
- **스코프:** 부모-자식 관계 관리
- **컨텍스트:** 기술 정보 (디스패처, 이름 등)
- **취소:** 부모에서 자식으로 자동 전파
- **대기:** 부모는 완료 전에 모든 자식을 기다림

### 코루틴 빌더
```kotlin
launch { }           // Job 반환
async { }            // Deferred<T> 반환
runBlocking { }      // 현재 스레드 차단
coroutineScope { }   // 새 코루틴 없이 새 스코프
```

### 부모-자식 관계
```kotlin
runBlocking {  // 외부 코루틴
    launch {   // 자식 코루틴 #1
        // ...
    }
    launch {   // 자식 코루틴 #2
        // ...
    }
    // 완료 전에 모든 자식을 기다림
}
```

### 로딩 취소

**취소 가능한 버전 (coroutineScope 사용):**
```kotlin
suspend fun loadContributorsConcurrent(
    service: GitHubService,
    req: RequestData
): List<User> = coroutineScope {
    val repos = service.getOrgRepos(req.org).bodyList()
    val deferreds = repos.map { repo ->
        async {
            delay(3000)  // 취소 가능
            service.getRepoContributors(req.org, repo.name).bodyList()
        }
    }
    deferreds.awaitAll().flatten().aggregate()
}
```

**취소 불가능 (GlobalScope 사용):**
```kotlin
suspend fun loadContributorsNotCancellable(
    service: GitHubService,
    req: RequestData
): List<User> {
    val repos = service.getOrgRepos(req.org).bodyList()
    val deferreds = repos.map { repo ->
        GlobalScope.async {  // 독립적, 취소 불가능
            delay(3000)
            service.getRepoContributors(req.org, repo.name).bodyList()
        }
    }
    return deferreds.awaitAll().flatten().aggregate()
}
```

### 취소 설정
```kotlin
val job = launch {
    val users = loadContributorsConcurrent(service, req)
    updateResults(users, startTime)
}

val listener = ActionListener {
    job.cancel()  // job과 모든 자식 취소
    updateLoadingStatus(CANCELED)
}
addCancelListener(listener)
```

---

## 섹션 8: 진행 상황 표시

### 문제
- 사용자는 모든 로딩이 완료된 후에만 결과를 봄
- 중간 피드백 없음

### 해결책
중간 결과에 대해 UI를 업데이트하는 콜백 사용:

```kotlin
suspend fun loadContributorsProgress(
    service: GitHubService,
    req: RequestData,
    updateResults: suspend (List<User>, completed: Boolean) -> Unit
) {
    val repos = service
        .getOrgRepos(req.org)
        .also { logRepos(req, it) }
        .bodyList()

    var allUsers = emptyList<User>()
    for ((index, repo) in repos.withIndex()) {
        val users = service
            .getRepoContributors(req.org, repo.name)
            .also { logUsers(repo, it) }
            .bodyList()

        allUsers = (allUsers + users).aggregate()
        updateResults(allUsers, index == repos.lastIndex)
    }
}
```

**사용법:**
```kotlin
launch(Dispatchers.Default) {
    loadContributorsProgress(service, req) { users, completed ->
        withContext(Dispatchers.Main) {
            updateResults(users, startTime, completed)
        }
    }
}
```

---

## 섹션 9: 채널

### 채널 개념
- 코루틴 간 통신 프리미티브
- 프로듀서가 데이터 전송
- 컨슈머가 데이터 수신
- 공유 가변 상태 없음

### 채널 다이어그램
```
Producer -> [Channel] -> Consumer
```

### 채널 인터페이스
```kotlin
interface SendChannel<in E> {
    suspend fun send(element: E)
    fun close(): Boolean
}

interface ReceiveChannel<out E> {
    suspend fun receive(): E
}

interface Channel<E> : SendChannel<E>, ReceiveChannel<E>
```

### 채널 유형

**무제한 채널**
- 크기 제한 없음
- `send()`가 절대 일시 중단되지 않음
- `receive()`가 비어있으면 일시 중단
- OutOfMemoryException 위험

```kotlin
val channel = Channel<String>(UNLIMITED)
```

**버퍼가 있는 채널**
- 고정된 크기 제한
- `send()`가 가득 차면 일시 중단
- `receive()`가 비어있으면 일시 중단

```kotlin
val channel = Channel<String>(10)
```

**랑데뷰 채널**
- 버퍼 없음 (크기 = 0)
- `send()`와 `receive()`가 "만나야" 함
- 하나가 항상 다른 하나가 호출할 때까지 일시 중단

```kotlin
val channel = Channel<String>()  // 기본값
```

**합류된 채널**
- 최신 요소만 저장
- `send()`가 절대 일시 중단되지 않음
- 새 요소가 이전 요소를 덮어씀

```kotlin
val channel = Channel<String>(CONFLATED)
```

### 채널을 사용한 동시 실행과 진행 상황

**해결책:**
```kotlin
suspend fun loadContributorsChannels(
    service: GitHubService,
    req: RequestData,
    updateResults: suspend (List<User>, completed: Boolean) -> Unit
) = coroutineScope {
    val repos = service
        .getOrgRepos(req.org)
        .also { logRepos(req, it) }
        .bodyList()

    val channel = Channel<List<User>>()

    for (repo in repos) {
        launch {
            val users = service
                .getRepoContributors(req.org, repo.name)
                .also { logUsers(repo, it) }
                .bodyList()
            channel.send(users)
        }
    }

    var allUsers = emptyList<User>()
    repeat(repos.size) { index ->
        val users = channel.receive()
        allUsers = (allUsers + users).aggregate()
        updateResults(allUsers, index == repos.lastIndex)
    }
}
```

**이점:**
- 동시 요청 (모두 즉시 시작)
- 순차적 소비 (동기화 필요 없음)
- 요청 사이에 진행 상황 업데이트
- 콜백 동기화보다 단순함

---

## 섹션 10: 코루틴 테스트

### 실시간 테스트의 문제
- 각 테스트가 2-4초 걸림
- 느린 피드백 루프
- 기계 의존적 타이밍

### 가상 시간 해결책

**runTest 사용:**
```kotlin
@Test
fun testDelayInSuspend() = runTest {
    val realStartTime = System.currentTimeMillis()
    val virtualStartTime = currentTime

    foo()

    println("${System.currentTimeMillis() - realStartTime} ms")  // ~ 6 ms
    println("${currentTime - virtualStartTime} ms")              // 1000 ms
}

suspend fun foo() {
    delay(1000)  // 실제 지연 없이 자동 진행
    println("foo")
}
```

### 동시 테스트 예제
```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
@Test
fun testConcurrent() = runTest {
    val startTime = currentTime
    val result = loadContributorsConcurrent(MockGithubService, testRequestData)

    Assert.assertEquals("Wrong result", expectedConcurrentResults.users, result)

    val totalTime = currentTime - startTime
    Assert.assertEquals(
        "Should run concurrently: 1000 (repos) + max(1000, 1200, 800) = 2200 ms",
        2200,
        totalTime
    )
}
```

---

## 성능 비교

| 접근 방식 | 시간 | 참고 |
|----------|------|------|
| 블로킹 순차적 | ~4000ms | 1000 + 1000 + 1200 + 800 |
| 백그라운드 스레드 | ~4000ms | 더 나은 UX, 같은 시간 |
| 콜백 | ~2000ms | 동시적이지만 복잡함 |
| 일시 중단 | ~4000ms | 깔끔하지만 여전히 순차적 |
| 동시적 (코루틴) | ~2200ms | 1000 + max(1000, 1200, 800) |
| 진행 상황 | ~4000ms | 중간 결과 표시 |
| 채널 | ~2200ms | 동시적 + 진행 상황 |

---

## 핵심 요약

1. **블로킹** - 단순하지만 UI를 멈춤
2. **콜백** - 논블로킹이지만 여러 비동기 연산에서 오류 발생 쉬움
3. **일시 중단 함수** - 깔끔한 코드, 순차적 실행
4. **async를 사용한 코루틴** - 동시 실행, 경량
5. **채널** - 코루틴 간 안전한 통신
6. **구조적 동시성** - 자동 취소와 정리
7. **테스트** - 빠르고 결정적인 테스트를 위해 가상 시간 사용

---

## 관련 리소스
- [코루틴 기초](coroutines-basics.md)
- [취소와 타임아웃](cancellation-and-timeouts.md)
