# RFC 8216 - HTTP Live Streaming (HLS)

> 발행일: 2017년 8월
> 상태: Informational
> 저자: Roger Pantos (Apple), William May (MLB Advanced Media)

## 1. 개요

HTTP Live Streaming(HLS)은 HTTP를 통해 무한한 멀티미디어 데이터 스트림을 전송하기 위한 프로토콜입니다. Apple이 2009년에 처음 제안했으며, 이 RFC는 프로토콜 버전 7을 명세합니다.

### 핵심 아이디어

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│  미디어 소스   │────>│  미디어 인코더  │────>│  스트림 세그멘터 │
│ (카메라, 파일) │     │  (H.264 등)  │     │ (.ts, .fmp4) │
└──────────────┘     └──────────────┘     └──────┬───────┘
                                                  │
                                                  v
                     ┌──────────────┐     ┌──────────────┐
                     │   클라이언트    │<────│   웹 서버      │
                     │  (재생기)      │     │ (플레이리스트  │
                     └──────────────┘     │  + 세그먼트)   │
                                          └──────────────┘
```

### 동작 방식

1. 서버가 미디어를 작은 세그먼트로 분할
2. 세그먼트 목록을 플레이리스트(M3U8) 파일로 제공
3. 클라이언트가 플레이리스트를 다운로드하고 세그먼트를 순서대로 재생
4. 라이브 스트리밍의 경우 클라이언트가 주기적으로 플레이리스트를 갱신

## 2. 플레이리스트 유형

HLS는 두 가지 유형의 플레이리스트를 사용합니다.

### 2.1 미디어 플레이리스트 (Media Playlist)

미디어 세그먼트의 목록을 재생 순서대로 나열합니다.

```
#EXTM3U
#EXT-X-VERSION:3
#EXT-X-TARGETDURATION:10
#EXT-X-MEDIA-SEQUENCE:0
#EXTINF:9.009,
segment0.ts
#EXTINF:9.009,
segment1.ts
#EXTINF:3.003,
segment2.ts
#EXT-X-ENDLIST
```

### 2.2 마스터 플레이리스트 (Master Playlist)

다양한 비트레이트/해상도의 변형 스트림(Variant Stream) 을 정의합니다.

```
#EXTM3U
#EXT-X-STREAM-INF:BANDWIDTH=1280000,RESOLUTION=640x360,CODECS="avc1.42c01e,mp4a.40.2"
low/playlist.m3u8
#EXT-X-STREAM-INF:BANDWIDTH=2560000,RESOLUTION=1280x720,CODECS="avc1.4d401f,mp4a.40.2"
mid/playlist.m3u8
#EXT-X-STREAM-INF:BANDWIDTH=7680000,RESOLUTION=1920x1080,CODECS="avc1.640028,mp4a.40.2"
high/playlist.m3u8
```

### 2.3 적응적 비트레이트 스트리밍 (ABR)

```
┌─────────────────────────────────────────────────┐
│              마스터 플레이리스트                    │
│    ┌─────────────┬──────────────┬────────────┐    │
│    │   360p      │    720p     │   1080p    │    │
│    │  1.2 Mbps   │  2.5 Mbps  │  7.6 Mbps  │    │
│    └──────┬──────┴──────┬──────┴──────┬─────┘    │
└───────────┼─────────────┼─────────────┼──────────┘
            v             v             v
     미디어 플레이리스트  미디어 플레이리스트  미디어 플레이리스트
     ┌──────────┐  ┌──────────┐  ┌──────────┐
     │ seg0.ts  │  │ seg0.ts  │  │ seg0.ts  │
     │ seg1.ts  │  │ seg1.ts  │  │ seg1.ts  │
     │ seg2.ts  │  │ seg2.ts  │  │ seg2.ts  │
     └──────────┘  └──────────┘  └──────────┘

클라이언트가 네트워크 상태에 따라 자동으로 품질 전환
```

## 3. 미디어 세그먼트 형식

### 3.1 지원 형식

| 형식 | 확장자 | 설명 |
|------|--------|------|
| MPEG-2 Transport Stream | .ts | 가장 널리 사용되는 HLS 세그먼트 형식 |
| Fragmented MPEG-4 | .mp4, .m4s | fMP4 기반, CMAF 호환 |
| Packed Audio | .aac, .mp3, .ac3 | 오디오 전용 스트림 |
| WebVTT | .vtt | 자막 세그먼트 |

### 3.2 MPEG-2 Transport Stream

```
┌─────────────────────────────────────────┐
│          Transport Stream 세그먼트        │
├────────┬────────┬───────────────────────┤
│  PAT   │  PMT   │  오디오/비디오 PES 패킷  │
│ (필수) │ (필수)  │                       │
└────────┴────────┴───────────────────────┘
```

- 단일 프로그램(MPEG-2 Program)만 포함
- 각 세그먼트의 시작에 PAT(Program Association Table) 와 PMT(Program Map Table) 필수
- 또는 `EXT-X-MAP` 태그로 초기화 섹션 참조

### 3.3 Fragmented MPEG-4

```
┌────────────────────────────────────────────────┐
│          초기화 섹션 (EXT-X-MAP)                  │
├────────┬────────┬────────────────────────────────┤
│  ftyp  │  moov  │  mvex (Movie Extends Box)     │
└────────┴────────┴────────────────────────────────┘

┌────────────────────────────────────────────────┐
│          미디어 세그먼트                          │
├────────┬────────┐                               │
│  moof  │  mdat  │  (반복 가능)                   │
│ (tfdt  │        │                               │
│  포함)  │        │                               │
└────────┴────────┘                               │
└────────────────────────────────────────────────┘
```

- Media Initialization Section 필수 (`ftyp` + `moov` + `mvex`)
- 각 Track Fragment Box에 `tfdt`(Track Fragment Decode Time) 포함 필수

### 3.4 Packed Audio

오디오 전용 스트림을 위한 형식:

| 코덱 | 프레이밍 |
|------|---------|
| AAC | ADTS 프레이밍 |
| MP3 | MPEG Audio 프레이밍 |
| AC-3 | AC-3 동기 프레임 |
| Enhanced AC-3 | Enhanced AC-3 동기 프레임 |

- 초기화 섹션 불필요
- ID3 PRIV 태그에 33비트 MPEG-2 타임스탬프 포함 필수
  - Owner: `com.apple.streaming.transportStreamTimestamp`

### 3.5 WebVTT

```
WEBVTT
X-TIMESTAMP-MAP=MPEGTS:900000,LOCAL:00:00:00.000

00:00:01.000 --> 00:00:05.000
첫 번째 자막입니다.

00:00:06.000 --> 00:00:10.000
두 번째 자막입니다.
```

- `X-TIMESTAMP-MAP` 헤더로 WebVTT 큐와 MPEG-2 타임스탬프 동기화
- 각 세그먼트의 표시 시간이 세그먼트 지속시간과 일치해야 함

## 4. 플레이리스트 태그

### 4.1 기본 태그

| 태그 | 설명 |
|------|------|
| `#EXTM3U` | 플레이리스트의 첫 번째 줄 (필수) |
| `#EXT-X-VERSION:<n>` | 호환성 버전 지정 (1~7) |

### 4.2 미디어 세그먼트 태그

각 세그먼트에 적용되는 태그들:

#### EXTINF

```
#EXTINF:<duration>,[<title>]
```

- 다음 세그먼트의 지속시간(초) 지정 (필수)
- 버전 3+에서 부동소수점 허용, 이전 버전은 정수만

#### EXT-X-BYTERANGE (버전 4+)

```
#EXT-X-BYTERANGE:<n>[@<o>]
```

- 세그먼트가 리소스의 일부 바이트 범위만 사용
- `n`: 바이트 길이, `o`: 바이트 오프셋 (생략 시 이전 범위 이후부터)

#### EXT-X-DISCONTINUITY

```
#EXT-X-DISCONTINUITY
```

인접 세그먼트 간 불연속성 표시. 다음이 변경될 때 필수:

- 파일 형식
- 트랙의 수, 유형, 식별자
- 타임스탬프 시퀀스

#### EXT-X-KEY

```
#EXT-X-KEY:METHOD=AES-128,URI="https://example.com/key",IV=0x00000000000000000000000000000001
```

| 속성 | 설명 | 필수 |
|------|------|------|
| METHOD | NONE, AES-128, SAMPLE-AES | O |
| URI | 키 파일 위치 | METHOD≠NONE일 때 |
| IV | 128비트 초기화 벡터 (16진수) | X (버전 2+) |
| KEYFORMAT | 키 표현 형식 (기본: "identity") | X (버전 5+) |
| KEYFORMATVERSIONS | 지원 형식 버전 | X (버전 5+) |

#### EXT-X-MAP (버전 5+)

```
#EXT-X-MAP:URI="init.mp4",BYTERANGE="800@0"
```

- Media Initialization Section 위치 지정
- fMP4 세그먼트에 필수

#### EXT-X-PROGRAM-DATE-TIME

```
#EXT-X-PROGRAM-DATE-TIME:2024-01-15T13:00:00.000+09:00
```

- 세그먼트의 첫 번째 샘플을 절대 날짜/시간에 매핑
- ISO 8601 형식

#### EXT-X-DATERANGE

```
#EXT-X-DATERANGE:ID="ad-break-1",START-DATE="2024-01-15T13:05:00.000+09:00",
  DURATION=30.0,X-AD-ID="12345"
```

| 속성 | 설명 |
|------|------|
| ID | 고유 식별자 (필수) |
| CLASS | 클라이언트 정의 클래스 |
| START-DATE | 범위 시작 (필수, ISO 8601) |
| END-DATE | 범위 종료 |
| DURATION | 지속시간 (초) |
| PLANNED-DURATION | 예상 지속시간 |
| X-\<name\> | 사용자 정의 속성 |
| END-ON-NEXT | YES면 다음 범위의 START-DATE가 종료 시점 |

### 4.3 미디어 플레이리스트 태그

| 태그 | 설명 | 기본값 |
|------|------|--------|
| `EXT-X-TARGETDURATION:<s>` | 최대 세그먼트 지속시간 (필수) | - |
| `EXT-X-MEDIA-SEQUENCE:<n>` | 첫 세그먼트의 시퀀스 번호 | 0 |
| `EXT-X-DISCONTINUITY-SEQUENCE:<n>` | 첫 세그먼트의 불연속 시퀀스 번호 | 0 |
| `EXT-X-ENDLIST` | 더 이상 세그먼트 추가 없음 | - |
| `EXT-X-PLAYLIST-TYPE:<type>` | VOD 또는 EVENT | - |
| `EXT-X-I-FRAMES-ONLY` | I-프레임 전용 플레이리스트 (버전 4+) | - |

#### 플레이리스트 유형

```
EVENT 플레이리스트:
┌────────────────────────────────────────┐
│ seg1 │ seg2 │ seg3 │ [새 세그먼트 추가만 가능]
└────────────────────────────────────────┘
→ 시작 부분 제거 불가, 끝에만 추가

VOD 플레이리스트:
┌────────────────────────────────────────┐
│ seg1 │ seg2 │ seg3 │ seg4 │ (고정)
└────────────────────────────────────────┘
→ 변경 불가 (완전한 녹화 콘텐츠)

라이브 플레이리스트 (타입 미지정):
┌────────────────────────────────────────┐
│ seg5 │ seg6 │ seg7 │ seg8 │
└────────────────────────────────────────┘
→ 슬라이딩 윈도우: 앞 세그먼트 제거 + 뒤 세그먼트 추가
```

### 4.4 마스터 플레이리스트 태그

#### EXT-X-MEDIA

대체 렌디션(언어, 카메라 앵글 등)을 정의합니다.

```
#EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID="audio",NAME="English",
  LANGUAGE="en",DEFAULT=YES,AUTOSELECT=YES,URI="eng-audio.m3u8"
#EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID="audio",NAME="한국어",
  LANGUAGE="ko",URI="kor-audio.m3u8"
```

| 속성 | 설명 | 필수 |
|------|------|------|
| TYPE | AUDIO, VIDEO, SUBTITLES, CLOSED-CAPTIONS | O |
| GROUP-ID | 렌디션 그룹 식별자 | O |
| NAME | 사람이 읽을 수 있는 이름 | O |
| URI | 미디어 플레이리스트 위치 | 조건부 |
| LANGUAGE | RFC 5646 언어 태그 | X |
| DEFAULT | YES/NO (자동 선택 여부) | X (기본: NO) |
| AUTOSELECT | YES/NO (환경 기반 자동 선택) | X (기본: NO) |
| FORCED | YES/NO (필수 자막 여부, SUBTITLES만) | X |
| INSTREAM-ID | 스트림 식별자 (CLOSED-CAPTIONS 필수) | 조건부 |
| CHARACTERISTICS | UTI 특성 (접근성 등) | X |
| CHANNELS | 오디오 채널 수 | X |

#### EXT-X-STREAM-INF

```
#EXT-X-STREAM-INF:BANDWIDTH=2560000,AVERAGE-BANDWIDTH=2000000,
  RESOLUTION=1280x720,FRAME-RATE=29.970,CODECS="avc1.4d401f,mp4a.40.2",
  AUDIO="audio",SUBTITLES="subs"
mid/playlist.m3u8
```

| 속성 | 설명 | 필수 |
|------|------|------|
| BANDWIDTH | 최대 비트레이트 (bps) | O |
| AVERAGE-BANDWIDTH | 평균 비트레이트 | X |
| CODECS | 코덱 식별자 (RFC 6381) | 권장 |
| RESOLUTION | 최적 디스플레이 해상도 (WxH) | X |
| FRAME-RATE | 최대 프레임레이트 | X |
| HDCP-LEVEL | TYPE-0 또는 NONE | X |
| AUDIO | 오디오 렌디션 그룹 참조 | X |
| VIDEO | 비디오 렌디션 그룹 참조 | X |
| SUBTITLES | 자막 렌디션 그룹 참조 | X |
| CLOSED-CAPTIONS | 자막 그룹 또는 NONE | X |

#### EXT-X-I-FRAME-STREAM-INF

```
#EXT-X-I-FRAME-STREAM-INF:BANDWIDTH=800000,CODECS="avc1.42c01e",
  RESOLUTION=640x360,URI="low-iframe.m3u8"
```

- I-프레임 전용 미디어 플레이리스트 식별
- 트릭 재생 (빨리감기, 되감기, 스크러빙) 지원

#### EXT-X-SESSION-DATA

```
#EXT-X-SESSION-DATA:DATA-ID="com.example.title",VALUE="My Video"
#EXT-X-SESSION-DATA:DATA-ID="com.example.metadata",URI="metadata.json"
```

- 임의의 세션 데이터 전달
- VALUE 또는 URI 중 하나만 사용 가능 (동시 불가)

#### EXT-X-SESSION-KEY

```
#EXT-X-SESSION-KEY:METHOD=AES-128,URI="https://example.com/key"
```

- 미디어 플레이리스트의 암호화 키를 사전 로드
- 재생 시작 전 키를 미리 가져와 지연 감소

### 4.5 공통 태그

#### EXT-X-INDEPENDENT-SEGMENTS

```
#EXT-X-INDEPENDENT-SEGMENTS
```

- 모든 세그먼트가 독립적으로 디코딩 가능
- 다른 세그먼트의 정보 없이도 재생 가능

#### EXT-X-START

```
#EXT-X-START:TIME-OFFSET=25.0,PRECISE=YES
```

- 선호하는 재생 시작 지점 지정
- TIME-OFFSET: 양수(시작부터), 음수(끝부터) 오프셋(초)
- PRECISE: YES면 해당 시점의 샘플부터 정확히 시작

## 5. 암호화

### 5.1 암호화 방식

| 방식 | 설명 |
|------|------|
| NONE | 암호화 없음 |
| AES-128 | 전체 세그먼트를 AES-128-CBC로 암호화 |
| SAMPLE-AES | 개별 미디어 샘플만 암호화 |

### 5.2 AES-128 상세

```
┌──────────────────────────────────────────┐
│              AES-128 암호화                │
├──────────────────────────────────────────┤
│ 알고리즘: AES-128-CBC                     │
│ 키 크기: 16 옥텟 (128비트)                │
│ 패딩: PKCS7                              │
│                                          │
│ IV (Initialization Vector):              │
│   - 명시적: EXT-X-KEY의 IV 속성           │
│   - 암시적: Media Sequence Number를       │
│     128비트 빅엔디안으로 변환              │
└──────────────────────────────────────────┘
```

### 5.3 암시적 IV 예시

```
Media Sequence Number = 5

IV = 0x00000000000000000000000000000005
     ├─────── 왼쪽 0으로 패딩 ────────┤
     (128비트 = 16바이트)
```

### 5.4 SAMPLE-AES

- fMP4: 'cbcs' 암호화 체계 사용
- MPEG-2 TS: HLS Sample Encryption 명세 참조
- 개별 오디오/비디오 샘플 단위로 암호화

## 6. 서버/클라이언트 책임

### 6.1 서버 책임

#### 세그먼트 생성

```
소스 미디어
    │
    v
┌──────────────────────────────────────┐
│ 1. 미디어를 세그먼트로 분할             │
│    - 지속시간 ≤ EXT-X-TARGETDURATION  │
│    - 패킷/키프레임 경계에서 분할         │
│                                      │
│ 2. 각 세그먼트에 다운로드 가능한 URI 생성 │
│                                      │
│ 3. 플레이리스트 파일 생성               │
│    - 재생 순서대로 세그먼트 나열          │
│    - 적절한 태그 포함                   │
└──────────────────────────────────────┘
```

#### 플레이리스트 업데이트 규칙

| 규칙 | 설명 |
|------|------|
| 원자적 변경 | 클라이언트 관점에서 변경이 원자적이어야 함 |
| 허용되는 변경 | 줄 추가, 앞 세그먼트 제거, 시퀀스 번호 증가, ENDLIST 추가 |
| TARGET DURATION | 값 변경 불가 |
| VOD | 변경 불가 (불변) |
| EVENT | 끝에만 세그먼트 추가 가능 |

#### 라이브 플레이리스트 관리

```
시간 T1:
#EXT-X-MEDIA-SEQUENCE:100
seg100.ts
seg101.ts
seg102.ts

시간 T2 (새 세그먼트 추가, 오래된 세그먼트 제거):
#EXT-X-MEDIA-SEQUENCE:101
seg101.ts
seg102.ts
seg103.ts
```

- 플레이리스트 지속시간 최소 3 × TARGET DURATION 유지
- 제거된 세그먼트는 세그먼트 지속시간 + 최장 플레이리스트 지속시간 동안 접근 가능 유지
- 새 버전은 이전 버전 이후 0.5 ~ 1.5 × TARGET DURATION 내에 게시

### 6.2 클라이언트 책임

#### 재생 흐름

```
┌──────────┐
│ 마스터     │
│ 플레이리스트│──> 변형 스트림 선택 (네트워크 상태 기반)
│ 로드       │
└────┬─────┘
     │
     v
┌──────────┐
│ 미디어     │──> 세그먼트 URI 파싱
│ 플레이리스트│
│ 로드       │
└────┬─────┘
     │
     v
┌──────────┐
│ 세그먼트   │──> 순서대로 다운로드 및 재생
│ 다운로드   │
└────┬─────┘
     │
     v
┌──────────┐
│ 플레이리스트│──> EXT-X-ENDLIST 없으면 주기적 갱신
│ 갱신       │    갱신 주기: TARGET DURATION
└──────────┘
```

#### 주요 클라이언트 동작

| 동작 | 설명 |
|------|------|
| 파싱 | 잘못된 구문이나 충돌하는 태그 발견 시 실패 처리 |
| 재생 | 세그먼트를 순서대로 재생, EXTINF 지속시간 준수 |
| 복호화 | EXT-X-KEY에 따라 세그먼트 복호화 |
| 불연속 처리 | EXT-X-DISCONTINUITY에서 디코더 리셋 |
| 비트레이트 적응 | 네트워크 상태에 따라 변형 스트림 전환 |
| 렌디션 선택 | 사용자 선호 또는 DEFAULT/AUTOSELECT 기반 선택 |

## 7. 프로토콜 버전 호환성

| 버전 | 도입 기능 |
|------|-----------|
| 1 | 기본 HLS 기능 |
| 2 | EXT-X-KEY의 IV 속성 |
| 3 | EXTINF 부동소수점 지속시간 |
| 4 | EXT-X-BYTERANGE, EXT-X-I-FRAMES-ONLY |
| 5 | KEYFORMAT, KEYFORMATVERSIONS, I-프레임용 EXT-X-MAP |
| 6 | 일반 EXT-X-MAP 사용, EXT-X-I-FRAME-STREAM-INF 없이 CLOSED-CAPTIONS 없음 불가 |
| 7 | EXT-X-MEDIA의 추가 기능 |

## 8. 플레이리스트 예시

### 8.1 간단한 미디어 플레이리스트

```
#EXTM3U
#EXT-X-VERSION:3
#EXT-X-TARGETDURATION:8
#EXT-X-MEDIA-SEQUENCE:2680

#EXTINF:7.975,
https://example.com/segment2680.ts
#EXTINF:7.941,
https://example.com/segment2681.ts
#EXTINF:7.975,
https://example.com/segment2682.ts
```

### 8.2 라이브 미디어 플레이리스트 (슬라이딩 윈도우)

```
#EXTM3U
#EXT-X-VERSION:3
#EXT-X-TARGETDURATION:8
#EXT-X-MEDIA-SEQUENCE:2680

#EXTINF:7.975,
https://example.com/segment2680.ts
#EXTINF:7.941,
https://example.com/segment2681.ts
#EXTINF:7.975,
https://example.com/segment2682.ts
```

(EXT-X-ENDLIST 없음 → 라이브 스트리밍, 주기적 갱신 필요)

### 8.3 암호화된 미디어 플레이리스트

```
#EXTM3U
#EXT-X-VERSION:3
#EXT-X-MEDIA-SEQUENCE:7794
#EXT-X-TARGETDURATION:15

#EXT-X-KEY:METHOD=AES-128,URI="https://priv.example.com/key.php?r=52"

#EXTINF:14.0,
segment7794.ts
#EXTINF:14.0,
segment7795.ts

#EXT-X-KEY:METHOD=AES-128,URI="https://priv.example.com/key.php?r=53"

#EXTINF:15.0,
segment7796.ts
```

### 8.4 마스터 플레이리스트 (변형 스트림)

```
#EXTM3U

#EXT-X-STREAM-INF:BANDWIDTH=1280000,AVERAGE-BANDWIDTH=1000000
low/audio-video.m3u8

#EXT-X-STREAM-INF:BANDWIDTH=2560000,AVERAGE-BANDWIDTH=2000000
mid/audio-video.m3u8

#EXT-X-STREAM-INF:BANDWIDTH=7680000,AVERAGE-BANDWIDTH=6000000
high/audio-video.m3u8

#EXT-X-STREAM-INF:BANDWIDTH=65000,CODECS="mp4a.40.5"
audio-only.m3u8
```

### 8.5 마스터 플레이리스트 (다국어 오디오)

```
#EXTM3U

#EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID="aac",NAME="English",
  DEFAULT=YES,AUTOSELECT=YES,LANGUAGE="en",URI="main/english-audio.m3u8"
#EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID="aac",NAME="Deutsch",
  DEFAULT=NO,AUTOSELECT=YES,LANGUAGE="de",URI="main/german-audio.m3u8"
#EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID="aac",NAME="Commentary",
  DEFAULT=NO,AUTOSELECT=NO,LANGUAGE="en",URI="commentary/audio-only.m3u8"

#EXT-X-STREAM-INF:BANDWIDTH=1280000,CODECS="avc1.42c01e,mp4a.40.2",AUDIO="aac"
low/video-only.m3u8

#EXT-X-STREAM-INF:BANDWIDTH=2560000,CODECS="avc1.4d401f,mp4a.40.2",AUDIO="aac"
mid/video-only.m3u8

#EXT-X-STREAM-INF:BANDWIDTH=7680000,CODECS="avc1.640028,mp4a.40.2",AUDIO="aac"
high/video-only.m3u8
```

## 9. 보안 고려사항

| 위협 | 대응 |
|------|------|
| 세그먼트/플레이리스트 변조 | HTTPS 사용 권장 |
| 키 탈취 | HTTPS를 통한 키 전달 |
| 재생 공격 | IV에 Media Sequence Number 사용 |
| DRM 우회 | SAMPLE-AES + DRM 시스템 결합 |

## 요약

RFC 8216 HTTP Live Streaming(HLS)은 다음을 제공합니다:

- HTTP 기반 전달: 기존 HTTP 인프라(CDN 등)를 그대로 활용
- 적응적 비트레이트: 네트워크 상태에 따른 자동 품질 전환
- 다양한 미디어 형식: MPEG-2 TS, fMP4, Packed Audio, WebVTT 지원
- 암호화 지원: AES-128, SAMPLE-AES를 통한 콘텐츠 보호
- 라이브/VOD 통합: 라이브 스트리밍과 VOD를 동일한 프로토콜로 처리
- 다국어/다중 렌디션: 여러 언어, 자막, 카메라 앵글 지원
- 트릭 재생: I-프레임 플레이리스트를 통한 빨리감기/되감기

HLS는 Apple이 개발한 프로토콜로, iOS/macOS의 기본 스트리밍 방식이며 사실상의 업계 표준으로 자리잡았습니다.
