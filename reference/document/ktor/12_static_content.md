# 12. 정적 콘텐츠 (Static Content)

> 출처: https://ktor.io/docs/server-static-content.html
> 한국어 학습 노트입니다.

---

## 두 가지 소스

| 함수 | 어디서 읽나 |
| --- | --- |
| `staticResources(remotePath, basePackage)` | 클래스패스 리소스 (`src/main/resources/...`, JAR 내부) |
| `staticFiles(remotePath, dir)` | 로컬 파일시스템 (`File("...")`) |

```kotlin
routing {
    staticResources("/assets", "static")          // /assets/* → resources/static/*
    staticFiles("/uploads", File("data/uploads")) // /uploads/* → ./data/uploads/*
}
```

`staticResources`는 jar로 패키징해도 그대로 동작하지만 변경 시 재배포 필요. `staticFiles`는 외부에서 갈아끼울 수 있어 사용자 업로드 등에 적합.

---

## 인덱스 페이지

기본은 `index.html`. 다른 파일로 바꾸려면:

```kotlin
staticResources("/", "static", index = "home.html")
```

인덱스가 필요 없으면 `index = null`.

---

## 사전 압축 (gzip / brotli)

같은 디렉터리에 `app.js`, `app.js.gz`, `app.js.br`이 모두 있다고 가정하고, 클라이언트가 보낸 `Accept-Encoding`에 맞는 것을 골라 응답합니다.

```kotlin
staticFiles("/", File("public")) {
    preCompressed(CompressedFileType.BROTLI, CompressedFileType.GZIP)
}
```

---

## 캐시 헤더

```kotlin
install(ConditionalHeaders)

staticFiles("/files", File("textFiles")) {
    cacheControl { file ->
        when (file.extension) {
            "html" -> listOf(CacheControl.NoCache(null))
            "js", "css" -> listOf(CacheControl.MaxAge(60 * 60 * 24 * 30))
            else -> emptyList()
        }
    }
}
```

`ConditionalHeaders`를 같이 깔면 `ETag` / `Last-Modified` 기반 304 응답이 동작.

---

## 일부 파일 차단 / 폴백 / HEAD

```kotlin
staticFiles("/site", File("public")) {
    exclude { file -> file.name.startsWith(".") }     // dotfile 차단
    enableAutoHeadResponse()                          // HEAD 지원
    default("index.html")                             // SPA 폴백
}
```

`default("index.html")`은 SPA에서 `/some/deep/route`도 `index.html`을 돌려주게 만들기 위한 흔한 패턴입니다.

---

## 그 외 옵션

- `staticZip(remotePath, zipFile)` — ZIP 아카이브에서 직접 서빙.
- `staticJar(remotePath, jarFile)` — JAR 내부 리소스 서빙.
- `modify { file, call -> ... }` — 응답을 바꾸기 전 후크.

---

## 권한이 있는 정적 파일

`staticFiles` 안에서도 라우트 스코프 플러그인 적용이 됩니다. 인증을 걸려면 묶음을 `authenticate {}` 안에 두세요.

```kotlin
authenticate("auth-session") {
    staticFiles("/private", File("private"))
}
```
