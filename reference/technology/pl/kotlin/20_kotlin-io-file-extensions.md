# 파일 입출력 확장 함수 (kotlin.io)

# kotlin.io 패키지 개요

> **원문:** https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.io/

Kotlin 표준 라이브러리는 `java.io.File`, `InputStream`, `Reader` 같은 JDK 타입에 확장 함수를 덧붙여서, 스트림을 직접 열고 닫는 보일러플레이트 없이 파일을 다루게 해줍니다. `kotlin.io` 패키지가 이 확장 함수들의 모음이며, 크게 다음 영역으로 나뉩니다.

- 텍스트/바이트 **읽기**: `readText`, `readLines`, `readBytes`, `forEachLine`, `useLines`
- 텍스트/바이트 **쓰기**: `writeText`, `appendText`, `writeBytes`, `appendBytes`
- **스트림·리더/라이터 생성**: `inputStream`, `outputStream`, `bufferedReader`, `bufferedWriter`, `reader`, `writer`
- **자원 관리**: `use`, `buffered`
- **파일 시스템 조작**: `copyTo`, `copyRecursively`, `deleteRecursively`, `walk`, `resolve`

대부분 JVM 전용이며(파일 시스템 접근이 필요하므로), `print`/`println`/`readln` 등 콘솔 입출력 일부만 공통(Common) 소스셋에서도 제공됩니다.

## 왜 확장 함수로 제공하는가

Java에서는 파일 하나를 문자열로 읽으려면 `FileReader` → `BufferedReader` → 한 줄씩 읽어 `StringBuilder`에 누적 → `close()`까지 직접 작성해야 합니다. Kotlin은 이 과정을 `File.readText()` 한 줄로 압축합니다. 대신 그만큼 **"작은 파일에서만 안전"**, **"자동으로 닫아준다"** 같은 전제가 숨어 있으므로, 함수마다 이 전제를 알고 써야 합니다.

```kotlin
// Java 스타일 없이 바로 문자열 획득
val config = File("app.properties").readText()
```

# 텍스트 읽기: readText, readLines

> **원문:** https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.io/read-text.html

## 시그니처

```kotlin
fun File.readText(charset: Charset = Charsets.UTF_8): String
fun File.readLines(charset: Charset = Charsets.UTF_8): List<String>
fun Reader.readText(): String
fun Reader.readLines(): List<String>
fun URL.readText(charset: Charset = Charsets.UTF_8): String
```

## 핵심 동작

- 파일(또는 `Reader`, `URL`)의 **전체 내용을 한 번에** 메모리로 읽어 `String` 또는 `List<String>`으로 반환합니다.
- 기본 문자셋은 UTF-8이며, 필요하면 `Charsets.EUC_KR` 등으로 바꿀 수 있습니다.
- `Reader.readText()`는 리더를 닫지 않으므로, 호출자가 직접 닫거나 `use`로 감싸야 합니다.

```kotlin
val text = File("notes.txt").readText()
val lines: List<String> = File("notes.txt").readLines()

File("notes.txt").reader().use { reader ->
    println(reader.readText())  // Reader 버전은 자동으로 닫히지 않으므로 use로 감싼다
}
```

## 주의: 큰 파일에는 부적합

공식 문서는 `readText`/`readLines`가 **파일 전체를 메모리에 올리기 때문에 큰 파일에는 권장하지 않는다**고 명시합니다. 내부적으로 약 2GB 크기 제한도 있습니다. 로그 파일이나 대용량 데이터를 다룰 때는 다음 절의 `forEachLine`/`useLines`처럼 스트리밍 방식으로 처리해야 합니다.

# 한 줄씩 처리하기: forEachLine, useLines, lineSequence

> **원문:** https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.io/use-lines.html

## useLines

```kotlin
inline fun <T> File.useLines(
    charset: Charset = Charsets.UTF_8,
    block: (Sequence<String>) -> T
): T
```

- 파일의 모든 줄을 `Sequence<String>`으로 만들어 `block`에 넘기고, **`block` 실행이 끝나면 내부 리더를 자동으로 닫습니다.**
- `Sequence`이므로 지연(lazy) 평가됩니다 — `block` 안에서 실제로 순회할 때만 한 줄씩 읽습니다. 전체를 리스트로 만드는 `readLines`와 달리 메모리에 전체 내용을 올리지 않습니다.
- 자원 해제를 리더가 아니라 함수가 책임진다는 점에서 "대여(loan) 패턴"이라 부릅니다. `try-with-resources`의 Kotlin식 대응이라고 보면 됩니다.
- `block`의 반환값이 그대로 `useLines`의 반환값이 됩니다. **`block` 밖으로 시퀀스 자체를 빼돌리면 안 됩니다** — 그 시점엔 이미 리더가 닫혀 있어서 순회 시 예외가 납니다.

```kotlin
val wordCount: Int = File("data.txt").useLines { lines ->
    lines.flatMap { it.split(" ") }.count()
}
```

## forEachLine

```kotlin
fun File.forEachLine(charset: Charset = Charsets.UTF_8, action: (String) -> Unit)
```

- `useLines`와 마찬가지로 한 줄씩 읽고 자동으로 자원을 닫지만, 반환값이 없는 대신 각 줄에 부수 효과(side effect)를 주는 용도입니다.

```kotlin
var errorCount = 0
File("app.log").forEachLine { line ->
    if ("ERROR" in line) errorCount++
}
```

## lineSequence

```kotlin
fun BufferedReader.lineSequence(): Sequence<String>
```

- `BufferedReader`에서 직접 시퀀스를 얻고 싶을 때 사용합니다. 다만 이 함수 자체는 리더를 닫아주지 않으므로, 대개 `use { it.lineSequence()... }` 형태로 감싸 씁니다.

## 선택 기준

| 상황 | 추천 함수 |
|---|---|
| 파일이 작고 전체 내용이 한 번에 필요 | `readText`, `readLines` |
| 큰 파일을 한 줄씩 순회하며 결과값을 만들어야 함 | `useLines` |
| 큰 파일을 한 줄씩 순회하며 부수 효과만 필요 | `forEachLine` |

# 텍스트/바이트 쓰기

> **원문:** https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.io/

```kotlin
fun File.writeText(text: String, charset: Charset = Charsets.UTF_8)
fun File.appendText(text: String, charset: Charset = Charsets.UTF_8)
fun File.writeBytes(array: ByteArray)
fun File.appendBytes(array: ByteArray)
```

- `writeText`/`writeBytes`는 파일이 있으면 **덮어씁니다**. `appendText`/`appendBytes`는 뒤에 이어 붙입니다.
- 둘 다 내부에서 스트림을 열고 닫는 과정을 알아서 처리하므로 `use`가 필요 없습니다.

```kotlin
val out = File("result.txt")
out.writeText("첫 줄\n")
out.appendText("두 번째 줄\n")
```

# 스트림·리더/라이터 생성과 자원 관리

> **원문:** https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.io/

## 스트림/리더 생성 함수

```kotlin
fun File.inputStream(): FileInputStream
fun File.outputStream(): FileOutputStream
fun File.bufferedReader(charset: Charset = Charsets.UTF_8, bufferSize: Int = DEFAULT_BUFFER_SIZE): BufferedReader
fun File.bufferedWriter(charset: Charset = Charsets.UTF_8, bufferSize: Int = DEFAULT_BUFFER_SIZE): BufferedWriter
fun InputStream.buffered(bufferSize: Int = DEFAULT_BUFFER_SIZE): BufferedInputStream
fun InputStream.copyTo(out: OutputStream, bufferSize: Int = DEFAULT_BUFFER_SIZE): Long
```

- `readText`/`writeText`류가 "한 번에 전부"를 다룬다면, 이 계열 함수는 **직접 스트림을 열어 세밀하게 제어**할 때 씁니다. 대신 닫는 책임은 호출자에게 있습니다.
- `bufferedReader`/`bufferedWriter`는 내부적으로 `InputStreamReader`/`OutputStreamWriter`를 `BufferedReader`/`BufferedWriter`로 감싸주는 편의 함수입니다.

## use — 자동 자원 해제

```kotlin
inline fun <T : Closeable?, R> T.use(block: (T) -> R): R
```

- `Closeable`을 구현한 모든 대상(스트림, 리더, 라이터)에 붙는 확장 함수로, `block` 실행 후 예외 발생 여부와 무관하게 **항상 `close()`를 호출**합니다. Java의 `try-with-resources`와 동일한 역할입니다.
- `useLines`, `forEachLine` 같은 함수들은 내부적으로 이 `use` 패턴 위에 구현되어 있습니다.

```kotlin
File("big.bin").outputStream().use { out ->
    File("source.bin").inputStream().use { input ->
        input.copyTo(out)
    }
}
```

# 파일 시스템 조작: 복사, 삭제, 순회

> **원문:** https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.io/

## 복사와 삭제

```kotlin
fun File.copyTo(target: File, overwrite: Boolean = false, bufferSize: Int = DEFAULT_BUFFER_SIZE): File
fun File.copyRecursively(target: File, overwrite: Boolean = false,
                         onError: (File, IOException) -> OnErrorAction = { _, ex -> throw ex }): Boolean
fun File.deleteRecursively(): Boolean
```

- `copyTo`는 파일 하나, `copyRecursively`는 디렉터리 전체를 대상으로 합니다. `onError` 콜백에서 `OnErrorAction.SKIP`/`ABORT`/`CONTINUE`를 반환해 오류 발생 시 동작을 제어할 수 있습니다.
- `deleteRecursively()`는 디렉터리와 그 하위 파일을 모두 지우며, 일부라도 실패하면 `false`를 반환합니다(예외를 던지지 않음).

## 디렉터리 순회

```kotlin
fun File.walk(direction: FileWalkDirection = FileWalkDirection.TOP_DOWN): FileTreeWalk
fun File.walkTopDown(): FileTreeWalk   // 부모를 먼저 방문
fun File.walkBottomUp(): FileTreeWalk // 자식을 먼저 방문
```

- `FileTreeWalk` 자체가 `Sequence<File>`처럼 동작해 `filter`, `map`, `forEach` 등을 그대로 이어 쓸 수 있습니다.
- 삭제 작업은 자식이 먼저 없어져야 하므로 보통 `walkBottomUp()`을 씁니다.

```kotlin
File("build").walkBottomUp()
    .filter { it.isFile && it.extension == "tmp" }
    .forEach { it.delete() }
```

## 경로 조작

```kotlin
fun File.resolve(relative: String): File       // 하위 경로 결합
fun File.resolveSibling(relative: String): File // 같은 부모 아래 다른 경로
fun File.relativeTo(base: File): File           // base 기준 상대 경로
```

```kotlin
val dir = File("/home/user/project")
val configFile = dir.resolve("config/app.yml")
println(configFile.relativeTo(dir))  // config/app.yml
```

# 정리

- **한 번에 전부** 처리(작은 파일): `readText`, `readLines`, `writeText`, `appendText`
- **한 줄씩 스트리밍** 처리(큰 파일): `useLines`(값 반환), `forEachLine`(부수 효과)
- **세밀한 제어**가 필요하면 `inputStream`/`outputStream`/`bufferedReader` + `use`로 직접 자원을 관리
- **디렉터리 단위** 작업은 `copyRecursively`, `deleteRecursively`, `walkTopDown`/`walkBottomUp`
- 거의 모든 텍스트 함수는 기본 문자셋이 **UTF-8**이며, 자동으로 닫아주지 않는 함수(`Reader.readText()`, `bufferedReader()` 등)는 반드시 `use`로 감싸야 자원 누수를 막을 수 있습니다.
