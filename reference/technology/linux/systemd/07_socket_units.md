# Socket Unit과 소켓 활성화

> 원본: https://www.freedesktop.org/software/systemd/man/systemd.socket.html

---

## 목차

1. [소켓 활성화란?](#소켓-활성화란)
2. [장점](#장점)
3. [기본 구조](#기본-구조)
4. [Listen 옵션](#listen-옵션)
5. [Accept= 모드](#accept-모드)
6. [소켓 옵션](#소켓-옵션)
7. [sd_listen_fds 프로토콜](#sd_listen_fds-프로토콜)
8. [실전 예제](#실전-예제)
9. [참고 자료](#참고-자료)

---

## 소켓 활성화란?

전통적인 `inetd`의 systemd 버전입니다. systemd가 포트(또는 유닉스 소켓)를 미리 열어 두고, 첫 연결이 들어오면 서비스를 시작해 해당 소켓을 넘겨줍니다.

```
[부팅]
  ├─ ssh.socket → 22번 포트 LISTEN (서비스는 아직 안 떠있음)
  ├─ ...
  └─ multi-user.target 도달

[첫 SSH 접속]
  ├─ 커널이 패킷 수신
  ├─ systemd가 ssh.service 시작
  ├─ ssh.service가 22번 포트 fd를 상속
  └─ ssh.service가 클라이언트 응답
```

---

## 장점

### 1. 부팅 가속

서비스 자체를 시작하지 않고 소켓만 열어두면 되므로 부팅이 빠릅니다. systemd가 모든 서비스의 소켓을 동시에 열어두면, 의존 관계가 있는 서비스들도 병렬로 기동할 수 있습니다.

### 2. 의존성 단순화

A 서비스가 B 서비스에 연결한다고 가정합니다. 일반적으로는 A가 B의 시작을 기다려야 합니다. 그러나 B의 **소켓**만 열려 있으면 A는 즉시 연결을 시도할 수 있고, 커널 버퍼에 쌓인 데이터는 B가 시작된 뒤 처리됩니다. 즉 **순서 의존성이 사라집니다**.

### 3. 서비스 재시작에도 연결 유지

서비스를 재시작해도 소켓은 systemd가 들고 있으므로 클라이언트 연결이 끊기지 않습니다.

### 4. 권한 분리

systemd가 소켓을 열고 권한 있는 fd를 비특권 서비스에 넘겨주면, 서비스 자체는 root 권한 없이도 동작합니다. (1024 미만 포트 포함)

### 5. 온디맨드 시작

거의 사용되지 않는 서비스는 평소에 메모리를 점유하지 않다가, 필요할 때만 시작됩니다.

---

## 기본 구조

소켓 유닛과 서비스 유닛은 같은 이름을 가져야 자동으로 연결됩니다.

```ini
# /etc/systemd/system/echo.socket
[Unit]
Description=Echo Socket

[Socket]
ListenStream=12345
Accept=no

[Install]
WantedBy=sockets.target
```

```ini
# /etc/systemd/system/echo.service
[Unit]
Description=Echo Service
Requires=echo.socket

[Service]
ExecStart=/usr/local/bin/echo-server
StandardInput=socket
```

`Service=` 로 다른 이름의 서비스를 지정할 수도 있습니다.

---

## Listen 옵션

### ListenStream — TCP 또는 Unix stream

```ini
ListenStream=8080                # IPv6/IPv4 dual, 모든 인터페이스
ListenStream=127.0.0.1:8080      # IPv4 localhost
ListenStream=[::1]:8080          # IPv6 localhost
ListenStream=/run/myapp.sock     # Unix domain socket
ListenStream=@abstract           # abstract namespace (Linux 전용)
```

여러 개 지정 가능. 각각 별도 fd로 서비스에 전달됩니다.

### ListenDatagram — UDP 또는 Unix dgram

```ini
ListenDatagram=53
ListenDatagram=/run/log.sock
```

### ListenSequentialPacket — Unix SEQPACKET

`SOCK_SEQPACKET` 형. 메시지 경계를 유지하는 신뢰성 있는 양방향 소켓.

### ListenFIFO — 명명된 파이프

```ini
ListenFIFO=/run/myapp.fifo
```

### ListenSpecial — 디바이스/특수 파일

```ini
ListenSpecial=/dev/input/event0
```

### ListenNetlink — Netlink 소켓

```ini
ListenNetlink=audit 1
```

### ListenUSBFunction — USB Gadget

USB Gadget 모드의 functionfs 인스턴스.

---

## Accept= 모드

### Accept=no (기본, 권장)

소켓 fd 자체를 서비스에 넘깁니다. 서비스는 단일 인스턴스로 실행되며, 직접 `accept(2)`를 호출해 연결을 처리합니다. nginx, sshd처럼 자체 멀티플렉싱이 가능한 데몬에 적합합니다.

### Accept=yes

연결마다 서비스를 새 인스턴스로 fork합니다. 서비스는 stdin/stdout이 클라이언트와 연결된 단순한 프로그램이면 됩니다. 옛 `inetd` 스타일로, 간단한 echo 서버나 ftp 같은 용도에 적합합니다.

```ini
[Socket]
ListenStream=2323
Accept=yes
```

이때 서비스는 **템플릿** 이어야 합니다:

```ini
# echo@.service
[Service]
ExecStart=/usr/local/bin/echo
StandardInput=socket
```

---

## 소켓 옵션

### 권한

```ini
SocketUser=nginx
SocketGroup=nginx
SocketMode=0660
```

Unix 소켓의 소유자 및 권한을 설정합니다.

### Backlog

```ini
Backlog=128
```

`listen(2)` backlog 큐 크기를 설정합니다.

### KeepAlive

```ini
KeepAlive=yes
KeepAliveTimeSec=300
KeepAliveIntervalSec=75
KeepAliveProbes=9
```

TCP keepalive를 설정합니다.

### NoDelay (TCP_NODELAY)

```ini
NoDelay=yes
```

Nagle 알고리즘을 비활성화합니다. 작은 패킷의 전송 지연을 줄입니다.

### FreeBind

```ini
FreeBind=yes
```

존재하지 않거나 아직 구성되지 않은 IP 주소에도 바인딩을 허용합니다. floating IP 환경에서 유용합니다.

### Transparent

```ini
Transparent=yes
```

`IP_TRANSPARENT`. 투명 프록시 구현에 사용합니다.

### ReusePort

```ini
ReusePort=yes
```

`SO_REUSEPORT`. 여러 프로세스가 동일한 포트에 바인딩할 수 있습니다 (커널 수준 로드 밸런싱).

### MaxConnections

```ini
MaxConnections=100
MaxConnectionsPerSource=10
```

`Accept=yes` 모드에서 동시 연결 수를 제한합니다.

### TriggerLimit

```ini
TriggerLimitIntervalSec=2s
TriggerLimitBurst=200
```

소켓 활성화 트리거 빈도를 제한합니다 (DoS 방지).

---

## sd_listen_fds 프로토콜

소켓 활성화를 지원하는 서비스는 fd를 다음과 같이 전달받습니다.

systemd는 다음 환경 변수를 설정한 뒤 서비스를 실행합니다:
- `LISTEN_FDS=N`: 전달된 fd 개수
- `LISTEN_PID=<pid>`: 현재 PID (다른 프로세스에서 받지 않도록 검증용)
- `LISTEN_FDNAMES=name1:name2`: fd 이름 (선택적)

fd는 `3`부터 시작하는 정수로 전달됩니다 (3, 4, 5, ...).

### libsystemd C API

```c
#include <systemd/sd-daemon.h>

int n = sd_listen_fds(0);
for (int fd = SD_LISTEN_FDS_START; fd < SD_LISTEN_FDS_START + n; fd++) {
    // 이 fd에서 accept() 또는 recvmsg() 등 수행
}
```

### Go

```go
listeners, err := activation.Listeners()  // github.com/coreos/go-systemd/activation
```

### 셸/Python

환경 변수를 직접 읽고 fd 3부터 처리하면 됩니다.

### FileDescriptorName

```ini
[Socket]
ListenStream=80
ListenStream=443
FileDescriptorName=http
FileDescriptorName=https
```

여러 소켓을 이름으로 구분할 때 사용합니다.

---

## 실전 예제

### 1. SSH 소켓 활성화

```ini
# ssh.socket
[Unit]
Description=OpenSSH Server Socket
Conflicts=ssh.service
Before=ssh.service

[Socket]
ListenStream=22
Accept=no

[Install]
WantedBy=sockets.target
```

### 2. nginx 권한 분리

systemd가 80/443 소켓을 열고 nginx에 넘기면, nginx는 root 권한 없이 실행됩니다.

```ini
# nginx.socket
[Socket]
ListenStream=80
ListenStream=443
NoDelay=yes
```

### 3. 컨테이너 헬스체크용 Unix 소켓

```ini
# myapp.socket
[Unit]
Description=MyApp Control Socket

[Socket]
ListenStream=/run/myapp/control.sock
SocketUser=myapp
SocketGroup=adm
SocketMode=0660
RemoveOnStop=yes

[Install]
WantedBy=sockets.target
```

### 4. inetd 스타일 echo

```ini
# echo.socket
[Socket]
ListenStream=2323
Accept=yes
```

```ini
# echo@.service
[Service]
ExecStart=/usr/bin/cat
StandardInput=socket
StandardOutput=socket
```

---

## 참고 자료

- [man systemd.socket](https://www.freedesktop.org/software/systemd/man/systemd.socket.html)
- [man sd_listen_fds](https://www.freedesktop.org/software/systemd/man/sd_listen_fds.html)
- [Lennart: Socket Activation](http://0pointer.de/blog/projects/socket-activation.html)
