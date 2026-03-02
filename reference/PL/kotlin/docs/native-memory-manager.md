# Kotlin/Native 메모리 관리

## 개요

Kotlin/Native는 JVM, Go 및 기타 주류 기술과 유사한 현대적인 메모리 관리자를 사용하며 다음과 같은 주요 기능을 제공합니다:

- 객체는 모든 스레드에서 접근 가능한 공유 힙에 저장됩니다
- 주기적인 추적 가비지 컬렉션이 "루트"(로컬 및 전역 변수)에서 도달할 수 없는 객체를 수집합니다

## 가비지 컬렉터

### 작동 방식

GC는 다음과 같은 특성을 가진 **stop-the-world 마크 및 동시 스윕 컬렉터**로 기능합니다:

- 별도의 스레드에서 실행되며 메모리 압박 휴리스틱 또는 타이머에 의해 트리거됩니다
- 여러 스레드(애플리케이션 스레드, GC 스레드 및 선택적 마커 스레드)에서 마크 큐를 병렬로 처리합니다
- 애플리케이션 스레드는 기본적으로 객체 마킹 중에 일시 중지됩니다
- 약한 참조는 GC 일시 중지 시간을 최소화하기 위해 동시에 처리됩니다

### 수동으로 가비지 컬렉션 활성화

```kotlin
kotlin.native.internal.GC.collect()
```

이것은 가비지 컬렉션을 강제하고 완료될 때까지 기다립니다.

### GC 성능 모니터링

**`build.gradle.kts`에서 로깅 활성화:**

```
-Xruntime-logs=gc=info
```

로그는 `stderr`에 출력됩니다.

**Apple 플랫폼에서**, signpost와 함께 Xcode Instruments 사용:

1. `gradle.properties`에 설정:
   ```
   kotlin.native.binary.enableSafepointSignposts=true
   ```

2. Xcode 열기 → Product | Profile (Cmd + I)

3. `os_signpost` 템플릿 선택

4. 다음으로 구성:
   - Subsystem: `org.kotlinlang.native.runtime`
   - Category: `safepoint`

5. record를 클릭하여 그래프에서 GC 일시 중지를 파란색 블롭으로 시각화

### GC 성능 최적화

GC 일시 중지 시간을 줄이기 위해 **동시 마킹** (실험적) 활성화:

```
kotlin.native.binary.gc=cms
```

### 가비지 컬렉션 비활성화

권장되지 않지만 테스트 또는 단기 프로그램에 가능:

```
kotlin.native.binary.gc=noop
```

**경고**: 메모리 소비가 지속적으로 증가합니다. 시스템 메모리 고갈 위험이 있습니다.

### 병렬 마킹 비활성화

단일 스레드 마킹의 경우 (큰 힙에서 일시 중지 시간이 증가할 수 있음):

```
kotlin.native.binary.gcMarkSingleThreaded=true
```

## 메모리 소비

### 메모리 소비 모니터링

**메모리 누수 확인:**

```kotlin
import kotlin.native.internal.*
import kotlin.test.*

class Resource

val global = mutableListOf<Resource>()

@OptIn(ExperimentalStdlibApi::class)
fun getUsage(): Long {
    GC.collect()
    return GC.lastGCInfo!!.memoryUsageAfter["heap"]!!.totalObjectsSizeBytes
}

fun run() {
    global.add(Resource())
    global.clear()
}

@Test
fun test() {
    val before = getUsage()
    run()
    val after = getUsage()
    assertEquals(before, after)
}
```

**Apple 플랫폼에서 메모리 추적** - Xcode Instruments의 VM Tracker를 통해 (태깅이 활성화된 기본 할당자 필요).

### 메모리 소비 조정

#### 1. Kotlin 업데이트

메모리 관리자 개선을 위해 Kotlin을 최신 상태로 유지합니다.

#### 2. 할당자 페이징 비활성화

```
kotlin.native.binary.pagedAllocator=false
```

메모리는 페이지 대신 객체별로 예약됩니다. 엄격한 메모리 제한이나 시작 최적화에 유용합니다.

#### 3. Latin-1 문자열 인코딩 활성화

기본적으로 문자열은 UTF-16을 사용합니다 (문자당 2바이트). ASCII 문자에 대해 Latin-1 활성화 (1바이트):

```
kotlin.native.binary.latin1Strings=true
```

바이너리 크기와 메모리 소비를 줄입니다. Latin-1이 아닌 문자는 UTF-16으로 폴백합니다.

**참고**: 실험적 상태에서 `String.pin()`, `String.usePinned()`, `String.refTo()`가 덜 효율적입니다.

## 백그라운드에서 단위 테스트

모킹하지 않는 한 단위 테스트에서 `Dispatchers.Main`을 사용하지 마세요. `kotlinx-coroutines-test`의 `Dispatchers.setMain()`을 사용하거나 백그라운드 테스트 런처를 구현합니다:

```kotlin
package testlauncher

import platform.CoreFoundation.*
import kotlin.native.concurrent.*
import kotlin.native.internal.test.*
import kotlin.system.*

fun mainBackground(args: Array<String>) {
    val worker = Worker.start(name = "main-background")
    worker.execute(TransferMode.SAFE, { args.freeze() }) {
        val result = testLauncherEntryPoint(it)
        exitProcess(result)
    }
    CFRunLoopRun()
    error("CFRunLoopRun should never return")
}
```

다음으로 컴파일: `-e testlauncher.mainBackground`

## 메모리 관리 모범 사례

### 순환 참조 방지

순환 참조는 메모리 누수를 일으킬 수 있습니다. 약한 참조를 사용하여 순환을 끊습니다:

```kotlin
import kotlin.native.ref.WeakReference

class Node {
    var parent: WeakReference<Node>? = null
    var children: MutableList<Node> = mutableListOf()
}
```

### 대용량 객체 관리

대용량 객체를 오래 유지하면 메모리 압박이 증가합니다. 필요하지 않을 때 참조를 null로 설정합니다:

```kotlin
var largeData: ByteArray? = loadLargeData()
// 사용 후
largeData = null
// GC가 다음 사이클에서 메모리 회수 가능
```

### 네이티브 리소스 정리

네이티브 리소스(파일 핸들, 소켓 등)는 명시적으로 정리해야 합니다:

```kotlin
val file = fopen("data.txt", "r")
try {
    // 파일 사용
} finally {
    fclose(file)
}
```

## 다음 단계

- [레거시 메모리 관리자에서 마이그레이션](native-migration-guide.html)
- [Swift/Objective-C ARC 통합 세부사항 확인](native-arc-integration.html)
