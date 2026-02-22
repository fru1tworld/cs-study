# Elasticsearch 설정 (Set up Elasticsearch)

> 이 문서는 Elasticsearch 9.x 공식 문서의 "Set up Elasticsearch" 섹션을 한국어로 번역한 것입니다.
> 원본 문서: https://www.elastic.co/guide/en/elasticsearch/reference/current/setup.html

---

## 목차

1. [Elasticsearch 설치](#elasticsearch-설치)
2. [Elasticsearch 구성](#elasticsearch-구성)
3. [중요한 Elasticsearch 구성](#중요한-elasticsearch-구성)
4. [보안 설정](#보안-설정)
5. [중요한 시스템 구성](#중요한-시스템-구성)
6. [부트스트랩 검사](#부트스트랩-검사)
7. [Elasticsearch 시작 및 중지](#elasticsearch-시작-및-중지)
8. [클러스터에 노드 추가](#클러스터에-노드-추가)
9. [전체 클러스터 재시작 및 롤링 재시작](#전체-클러스터-재시작-및-롤링-재시작)
10. [Elasticsearch 업그레이드](#elasticsearch-업그레이드)

---

## Elasticsearch 설치

이 섹션에서는 Elasticsearch를 설정하고 실행하는 방법에 대한 정보를 포함합니다.

### 지원 플랫폼

공식적으로 지원되는 운영 체제 및 JVM의 매트릭스는 [지원 매트릭스](https://www.elastic.co/support/matrix)에서 확인할 수 있습니다. Elasticsearch는 나열된 플랫폼에서 테스트되었지만 다른 플랫폼에서도 작동할 수 있습니다.

### 버전 호환성

Elastic Stack을 설치할 때는 전체 스택에서 동일한 버전을 사용해야 합니다. 예를 들어, Elasticsearch 9.2.3을 사용하는 경우 Beats 9.2.3, APM Server 9.2.3, Elasticsearch Hadoop 9.2.3, Kibana 9.2.3, Logstash 9.2.3을 설치해야 합니다.

### Java (JVM) 버전

Elasticsearch는 Java를 사용하여 빌드되었으며, 각 배포판에 OpenJDK의 번들 버전이 포함되어 있습니다. 번들된 JVM은 Elasticsearch의 모든 설치에서 사용하는 것을 강력히 권장합니다. 번들된 JVM은 지원 및 유지 관리 측면에서 Elasticsearch의 다른 종속성과 동일하게 취급됩니다.

자체 Java 버전을 사용하려면 `ES_JAVA_HOME` 환경 변수를 설정합니다. 자체 JVM을 사용해야 하는 경우 지원되는 [JVM 버전](https://www.elastic.co/support/matrix#matrix_jvm)을 사용하는 것이 좋습니다.

### Elasticsearch 9.0 변경 사항

Elasticsearch는 Java SecurityManager에서 Entitlements로 영구적으로 전환되었습니다. Java SecurityManager는 Java 17부터 더 이상 사용되지 않으며 Java 24에서는 완전히 비활성화됩니다. Elasticsearch는 버전 9.0부터 Java SecurityManager를 영구적으로 대체하는 Entitlements라는 자체 보호 메커니즘을 구현했습니다.

---

### 로컬 개발용 빠른 시작

로컬 테스트의 경우 Docker를 사용하여 로컬 머신에서 Elasticsearch와 Kibana를 설치하고 실행할 수 있습니다:

```bash
curl -fsSL https://elastic.co/start-local | sh
```

프로덕션 설치의 경우 아래의 추가 단계를 따르세요.

---

### Linux 또는 MacOS에서 아카이브로 설치

Elasticsearch는 Linux 및 MacOS용 `.tar.gz` 아카이브로 제공됩니다. 이 패키지에는 무료 및 구독 기능이 모두 포함되어 있으며, 30일 평가판을 시작하여 모든 기능을 사용해 볼 수 있습니다.

#### 사전 요구 사항

- Elasticsearch에는 OpenJDK의 번들 버전이 포함되어 있습니다. 자체 Java 버전을 사용하려면 JVM 버전 요구 사항을 참조하세요.
- 이 가이드의 명령은 root가 아닌 일반 사용자 계정을 사용하여 실행하는 것이 좋습니다.
- Elasticsearch를 설치하기 전에 지원되는 운영 체제를 검토하고 가상 또는 물리적 호스트를 준비하세요.

#### Linux에서 다운로드 및 설치

Linux 아카이브는 다음과 같이 다운로드하고 설치할 수 있습니다:

```bash
# 아카이브 및 SHA 체크섬 다운로드
wget https://artifacts.elastic.co/downloads/elasticsearch/elasticsearch-9.2.3-linux-x86_64.tar.gz
wget https://artifacts.elastic.co/downloads/elasticsearch/elasticsearch-9.2.3-linux-x86_64.tar.gz.sha512

# 다운로드 확인
shasum -a 512 -c elasticsearch-9.2.3-linux-x86_64.tar.gz.sha512

# 압축 해제
tar -xzf elasticsearch-9.2.3-linux-x86_64.tar.gz

# Elasticsearch 디렉토리로 이동
cd elasticsearch-9.2.3/
```

이 디렉토리는 `$ES_HOME`으로 알려져 있습니다.

#### MacOS에서 다운로드 및 설치

MacOS 아카이브는 다음과 같이 다운로드하고 설치할 수 있습니다:

```bash
# 아카이브 및 SHA 체크섬 다운로드
curl -O https://artifacts.elastic.co/downloads/elasticsearch/elasticsearch-9.2.3-darwin-x86_64.tar.gz
curl -O https://artifacts.elastic.co/downloads/elasticsearch/elasticsearch-9.2.3-darwin-x86_64.tar.gz.sha512

# 다운로드 확인
shasum -a 512 -c elasticsearch-9.2.3-darwin-x86_64.tar.gz.sha512

# 압축 해제
tar -xzf elasticsearch-9.2.3-darwin-x86_64.tar.gz

# Elasticsearch 디렉토리로 이동
cd elasticsearch-9.2.3/
```

#### MacOS Gatekeeper 경고 방지

다운로드한 `.tar.gz` 아카이브 또는 압축 해제된 디렉토리에서 Gatekeeper 검사를 방지하려면 다음 명령을 실행합니다:

```bash
xattr -d -r com.apple.quarantine <archive-or-directory>
```

#### 명령줄에서 Elasticsearch 실행

다음 명령을 실행하여 명령줄에서 Elasticsearch를 시작합니다:

```bash
./bin/elasticsearch
```

Elasticsearch를 처음 시작하면 보안 기능이 기본적으로 활성화되고 구성됩니다. 다음 보안 구성이 자동으로 수행됩니다:

- 인증 및 인가가 활성화됨
- TLS 인증서 및 키가 전송 및 HTTP 계층에 대해 생성됨
- `elastic` 내장 슈퍼유저의 비밀번호가 생성되어 터미널에 출력됨
- Kibana용 등록 토큰이 생성되어 터미널에 출력됨 (30분간 유효)

#### Elasticsearch 실행 확인

HTTPS를 통해 `localhost`의 포트 `9200`으로 HTTP 요청을 보내 Elasticsearch 노드가 실행 중인지 확인할 수 있습니다:

```bash
curl --cacert $ES_HOME/config/certs/http_ca.crt -u elastic:$ELASTIC_PASSWORD https://localhost:9200
```

올바르게 실행 중이면 다음과 유사한 응답을 받게 됩니다:

```json
{
  "name" : "Cp8oag6",
  "cluster_name" : "elasticsearch",
  "cluster_uuid" : "AT69_T_DTp-1qgIJlatQqA",
  "version" : {
    "number" : "9.2.3",
    "build_flavor" : "default",
    "build_type" : "tar",
    "build_hash" : "f27399d",
    "build_date" : "2025-06-01T20:30:17.491Z",
    "build_snapshot" : false,
    "lucene_version" : "9.10.0",
    "minimum_wire_compatibility_version" : "8.17.0",
    "minimum_index_compatibility_version" : "8.0.0"
  },
  "tagline" : "You Know, for Search"
}
```

#### 데몬으로 실행

Elasticsearch를 데몬으로 실행하려면 명령줄에서 `-d`를 지정하고 `-p` 옵션을 사용하여 프로세스 ID를 파일에 기록합니다:

```bash
./bin/elasticsearch -d -p pid
```

암호화된 Elasticsearch 키스토어에 비밀번호를 설정한 경우 로컬 파일에서 비밀번호를 전달하라는 메시지가 표시됩니다. 로그 메시지는 `$ES_HOME/logs/` 디렉토리에서 찾을 수 있습니다.

Elasticsearch를 종료하려면 `pid` 파일에 기록된 프로세스 ID를 종료합니다:

```bash
pkill -F pid
```

---

### Debian 패키지로 설치

Elasticsearch용 Debian 패키지는 웹사이트에서 다운로드하거나 APT 저장소에서 설치할 수 있습니다. Debian 및 Ubuntu와 같은 Debian 기반 시스템에 Elasticsearch를 설치하는 데 사용할 수 있습니다.

#### APT 저장소에서 설치

##### 1. Elastic 공개 서명 키 가져오기

```bash
wget -qO - https://artifacts.elastic.co/GPG-KEY-elasticsearch | sudo gpg --dearmor -o /usr/share/keyrings/elasticsearch-keyring.gpg
```

##### 2. apt-transport-https 패키지 설치

Debian에서 진행하기 전에 apt-transport-https 패키지를 설치해야 할 수 있습니다:

```bash
sudo apt-get install apt-transport-https
```

##### 3. 저장소 정의 저장

```bash
echo "deb [signed-by=/usr/share/keyrings/elasticsearch-keyring.gpg] https://artifacts.elastic.co/packages/9.x/apt stable main" | sudo tee /etc/apt/sources.list.d/elastic-9.x.list
```

> 주의: `add-apt-repository`를 사용하지 마세요. 시스템 `/etc/apt/sources.list` 파일에 항목을 추가하기 때문입니다.

##### 4. Elasticsearch 설치

```bash
sudo apt-get update && sudo apt-get install elasticsearch
```

#### 수동으로 Debian 패키지 다운로드 및 설치

```bash
wget https://artifacts.elastic.co/downloads/elasticsearch/elasticsearch-9.2.3-amd64.deb
wget https://artifacts.elastic.co/downloads/elasticsearch/elasticsearch-9.2.3-amd64.deb.sha512
shasum -a 512 -c elasticsearch-9.2.3-amd64.deb.sha512
sudo dpkg -i elasticsearch-9.2.3-amd64.deb
```

#### 보안 구성

Elasticsearch를 처음 시작하면 TLS가 HTTP 계층에 대해 자동으로 구성됩니다. CA 인증서가 생성되어 디스크에 저장되며, 이 인증서의 hex 인코딩된 SHA-256 지문이 터미널에 출력됩니다.

#### systemd로 실행

Elasticsearch가 시스템 부팅 시 자동으로 시작되도록 하려면 다음 명령을 실행합니다:

```bash
sudo /bin/systemctl daemon-reload
sudo /bin/systemctl enable elasticsearch.service
```

Elasticsearch 시작 및 중지:

```bash
sudo systemctl start elasticsearch.service
sudo systemctl stop elasticsearch.service
```

---

### RPM으로 설치

Elasticsearch용 RPM 패키지는 웹사이트에서 다운로드하거나 RPM 저장소에서 설치할 수 있습니다. openSUSE, SUSE Linux Enterprise Server (SLES), CentOS, Red Hat Enterprise Linux (RHEL), Oracle Linux와 같은 RPM 기반 시스템에 Elasticsearch를 설치하는 데 사용할 수 있습니다.

> 참고: RPM 설치는 SLES 11 및 CentOS 5와 같은 이전 버전의 RPM이 있는 배포판에서는 지원되지 않습니다. 이러한 시스템의 경우 아카이브에서 설치하세요.

#### RPM 저장소에서 설치

##### 1. 공개 서명 키 가져오기

```bash
sudo rpm --import https://artifacts.elastic.co/GPG-KEY-elasticsearch
```

##### 2. 저장소 파일 생성

`/etc/yum.repos.d/` 디렉토리에 `elasticsearch.repo` 파일을 생성합니다:

```ini
[elasticsearch]
name=Elasticsearch repository for 9.x packages
baseurl=https://artifacts.elastic.co/packages/9.x/yum
gpgcheck=1
gpgkey=https://artifacts.elastic.co/GPG-KEY-elasticsearch
enabled=1
autorefresh=1
type=rpm-md
```

##### 3. Elasticsearch 설치

```bash
sudo yum install elasticsearch
```

또는:

```bash
sudo dnf install elasticsearch
```

#### 수동으로 RPM 다운로드 및 설치

```bash
wget https://artifacts.elastic.co/downloads/elasticsearch/elasticsearch-9.2.3-x86_64.rpm
wget https://artifacts.elastic.co/downloads/elasticsearch/elasticsearch-9.2.3-x86_64.rpm.sha512
shasum -a 512 -c elasticsearch-9.2.3-x86_64.rpm.sha512
sudo rpm --install elasticsearch-9.2.3-x86_64.rpm
```

---

### Windows에서 .zip으로 설치

Elasticsearch는 Windows에서 `.zip` 아카이브를 사용하여 설치할 수 있습니다. 여기에는 Elasticsearch를 서비스로 실행하도록 설정하는 `elasticsearch-service.bat` 명령이 포함되어 있습니다.

#### 다운로드 및 설치

Windows용 `.zip` 아카이브를 다운로드합니다:

```
https://artifacts.elastic.co/downloads/elasticsearch/elasticsearch-9.2.3-windows-x86_64.zip
```

PowerShell을 사용하여 추출합니다:

```powershell
Expand-Archive elasticsearch-9.2.3-windows-x86_64.zip
```

#### 명령줄에서 실행

```cmd
.\bin\elasticsearch.bat
```

#### Windows 서비스로 설치

Elasticsearch를 Windows 서비스로 설치하려면:

```cmd
.\bin\elasticsearch-service.bat install
```

서비스 시작:

```cmd
.\bin\elasticsearch-service.bat start
```

서비스 중지:

```cmd
.\bin\elasticsearch-service.bat stop
```

#### 추가 요구 사항

Windows에서 Elasticsearch 머신 러닝 기능을 사용하려면 Microsoft Universal C Runtime 라이브러리가 필요합니다. 이것은 Windows 10, Windows Server 2016 및 더 최신 버전의 Windows에 기본 제공됩니다. 이전 버전의 Windows에서는 Windows Update를 통해 또는 별도의 다운로드에서 설치할 수 있습니다.

---

### Docker로 설치

Elasticsearch용 Docker 이미지는 Elastic Docker 레지스트리에서 사용할 수 있습니다. 게시된 모든 Docker 이미지 및 태그 목록은 [www.docker.elastic.co](https://www.docker.elastic.co)에서 확인할 수 있습니다.

#### 단일 노드 클러스터 시작 (개발/테스트용)

Docker 명령을 사용하여 개발 또는 테스트용 단일 노드 Elasticsearch 클러스터를 시작합니다:

```bash
docker network create elastic
docker pull docker.elastic.co/elasticsearch/elasticsearch:9.2.3
docker run --name es01 --net elastic -p 9200:9200 -it -m 1GB docker.elastic.co/elasticsearch/elasticsearch:9.2.3
```

`-m` 플래그를 사용하여 컨테이너의 메모리 제한을 설정합니다. 이렇게 하면 JVM 크기를 수동으로 설정할 필요가 없습니다.

> 참고: ELSER를 사용한 시맨틱 검색과 같은 머신 러닝 기능은 1GB 이상의 메모리가 있는 더 큰 컨테이너가 필요합니다. 머신 러닝 기능을 사용하려면 6GB 메모리로 컨테이너를 시작하세요.

#### 보안 강화 이미지 (Wolfi)

추가 보안을 위해 강화된 Wolfi 이미지를 사용할 수 있습니다. Wolfi 이미지를 사용하려면 Docker 버전 20.10.10 이상이 필요합니다:

```bash
docker run --name es01 --net elastic -p 9200:9200 -it -m 1GB docker.elastic.co/elasticsearch/elasticsearch:9.2.3-wolfi
```

#### 비밀번호 및 등록 토큰 복사

Elasticsearch가 처음 시작되면 `elastic` 사용자 비밀번호가 터미널에 출력됩니다. 나중에 비밀번호를 재설정하려면:

```bash
docker exec -it es01 /usr/share/elasticsearch/bin/elasticsearch-reset-password -u elastic
```

Kibana 등록 토큰 생성:

```bash
docker exec -it es01 /usr/share/elasticsearch/bin/elasticsearch-create-enrollment-token -s kibana
```

#### SSL 인증서 복사

`http_ca.crt` SSL 인증서를 로컬 머신에 복사:

```bash
docker cp es01:/usr/share/elasticsearch/config/certs/http_ca.crt .
```

#### 연결 확인

새 터미널을 열고 인증서를 사용하여 Elasticsearch 클러스터에 인증된 호출을 수행하여 연결을 확인합니다:

```bash
curl --cacert http_ca.crt -u elastic:$ELASTIC_PASSWORD https://localhost:9200
```

#### Docker Compose로 다중 노드 클러스터 시작

Docker Compose를 사용하여 Kibana와 함께 3개 노드 Elasticsearch 클러스터를 시작합니다:

```yaml
version: "3.8"
services:
  es01:
    image: docker.elastic.co/elasticsearch/elasticsearch:9.2.3
    container_name: es01
    environment:
      - node.name=es01
      - cluster.name=es-docker-cluster
      - discovery.seed_hosts=es02,es03
      - cluster.initial_master_nodes=es01,es02,es03
      - bootstrap.memory_lock=true
      - "ES_JAVA_OPTS=-Xms512m -Xmx512m"
    ulimits:
      memlock:
        soft: -1
        hard: -1
    volumes:
      - data01:/usr/share/elasticsearch/data
    ports:
      - 9200:9200
    networks:
      - elastic

  es02:
    image: docker.elastic.co/elasticsearch/elasticsearch:9.2.3
    container_name: es02
    environment:
      - node.name=es02
      - cluster.name=es-docker-cluster
      - discovery.seed_hosts=es01,es03
      - cluster.initial_master_nodes=es01,es02,es03
      - bootstrap.memory_lock=true
      - "ES_JAVA_OPTS=-Xms512m -Xmx512m"
    ulimits:
      memlock:
        soft: -1
        hard: -1
    volumes:
      - data02:/usr/share/elasticsearch/data
    networks:
      - elastic

  es03:
    image: docker.elastic.co/elasticsearch/elasticsearch:9.2.3
    container_name: es03
    environment:
      - node.name=es03
      - cluster.name=es-docker-cluster
      - discovery.seed_hosts=es01,es02
      - cluster.initial_master_nodes=es01,es02,es03
      - bootstrap.memory_lock=true
      - "ES_JAVA_OPTS=-Xms512m -Xmx512m"
    ulimits:
      memlock:
        soft: -1
        hard: -1
    volumes:
      - data03:/usr/share/elasticsearch/data
    networks:
      - elastic

  kibana:
    image: docker.elastic.co/kibana/kibana:9.2.3
    container_name: kib01
    ports:
      - 5601:5601
    environment:
      ELASTICSEARCH_URL: http://es01:9200
      ELASTICSEARCH_HOSTS: '["http://es01:9200","http://es02:9200","http://es03:9200"]'
    networks:
      - elastic

volumes:
  data01:
    driver: local
  data02:
    driver: local
  data03:
    driver: local

networks:
  elastic:
    driver: bridge
```

Docker Compose로 시작:

```bash
docker-compose up -d
```

> 참고: Docker Desktop을 사용하는 경우 Docker Desktop에 최소 4GB의 메모리를 할당하세요. Settings > Resources에서 메모리 사용량을 조정할 수 있습니다.

#### Docker 구성

Docker에서 실행할 때 Elasticsearch 구성 파일은 `/usr/share/elasticsearch/config/`에서 로드됩니다. 사용자 정의 구성 파일을 사용하려면 이미지의 구성 파일 위에 파일을 바인드 마운트합니다.

사용자 정의 `elasticsearch.yml` 파일을 바인드 마운트하는 경우 `network.host: 0.0.0.0` 설정이 포함되어 있는지 확인하세요. 이 설정은 포트가 노출된 경우 HTTP 및 전송 트래픽에 대해 노드에 도달할 수 있도록 합니다.

#### 프로덕션 고려 사항

기본적으로 Elasticsearch는 노드의 역할과 컨테이너에서 사용 가능한 총 메모리를 기반으로 JVM 힙 크기를 자동으로 조정합니다. 대부분의 프로덕션 환경에서는 이 기본 크기 조정이 권장됩니다.

Elasticsearch 컨테이너에는 nofile 및 nproc에 대한 증가된 ulimits가 있어야 합니다. Docker 데몬의 init 시스템이 이를 허용 가능한 값으로 설정하는지 확인하세요.

---

## Elasticsearch 구성

Elasticsearch는 좋은 기본값을 제공하며 구성이 거의 필요하지 않습니다. 대부분의 설정은 클러스터 업데이트 설정 API를 사용하여 실행 중인 클러스터에서 변경할 수 있습니다.

### 구성 파일

Elasticsearch에는 세 가지 구성 파일이 있습니다:

- elasticsearch.yml: Elasticsearch 구성용
- jvm.options: Elasticsearch JVM 설정 구성용
- log4j2.properties: Elasticsearch 로깅 구성용

이 파일들은 config 디렉토리에 위치하며, 기본 위치는 설치 유형에 따라 다릅니다.

### 구성 파일 형식

구성 형식은 YAML입니다. 다음은 데이터 및 로그 디렉토리 경로를 변경하는 예입니다:

```yaml
path:
  data: /var/lib/elasticsearch
  logs: /var/log/elasticsearch
```

설정은 다음과 같이 플랫하게도 할 수 있습니다:

```yaml
path.data: /var/lib/elasticsearch
path.logs: /var/log/elasticsearch
```

### 환경 변수

`elasticsearch.yml` 구성 파일 내에서 `${...}` 표기법으로 참조된 환경 변수는 환경 변수 값으로 대체됩니다:

```yaml
node.name: ${HOSTNAME}
network.host: ${ES_NETWORK_HOST}
```

환경 변수 값은 단순 문자열이어야 합니다. 쉼표로 구분된 문자열을 사용하여 Elasticsearch가 목록으로 구문 분석할 값을 제공합니다.

### 구성 디렉토리 위치

config 디렉토리의 위치는 `ES_PATH_CONF` 환경 변수를 설정하여 변경할 수 있습니다. 쉘에서 환경 변수를 설정하는 것만으로는 충분하지 않습니다. 패키지의 관련 파일에서 `ES_PATH_CONF=/etc/elasticsearch` 항목을 편집하여 config 디렉토리 위치를 변경해야 합니다.

---

### 동적 설정과 정적 설정

자체 관리 클러스터에서는 클러스터 업데이트 설정 API를 사용하여 동적 클러스터 설정을 구성하고, `elasticsearch.yml`은 정적 클러스터 설정 및 노드 설정에만 사용해야 합니다. API는 재시작이 필요하지 않으며 모든 노드에서 설정 값이 동일하도록 보장합니다.

정적 설정 은 시작되지 않았거나 종료된 노드에서 `elasticsearch.yml`을 사용해서만 구성할 수 있습니다. 정적 설정은 클러스터의 모든 관련 노드에서 설정해야 합니다.

동적 설정 은 클러스터 업데이트 설정 API를 사용하여 실행 중인 클러스터에서 구성할 수 있습니다.

---

### 디렉토리 레이아웃

설치 방법에 따라 디렉토리 레이아웃이 다릅니다:

#### .tar.gz 아카이브 디렉토리 레이아웃

| 유형 | 설명 | 기본 위치 |
|------|------|-----------|
| home | Elasticsearch 홈 디렉토리 (`$ES_HOME`) | 아카이브 압축 해제 디렉토리 |
| bin | 바이너리 스크립트 | `$ES_HOME/bin` |
| conf | 구성 파일 (`elasticsearch.yml` 포함) | `$ES_HOME/config` |
| data | 노드에 할당된 각 인덱스/샤드의 데이터 파일 | `$ES_HOME/data` |
| logs | 로그 파일 | `$ES_HOME/logs` |
| plugins | 플러그인 파일 | `$ES_HOME/plugins` |
| repo | 공유 파일 시스템 저장소 위치 | 구성되지 않음 |

#### Debian/RPM 패키지 디렉토리 레이아웃

| 유형 | 설명 | 기본 위치 |
|------|------|-----------|
| home | Elasticsearch 홈 디렉토리 | `/usr/share/elasticsearch` |
| bin | 바이너리 스크립트 | `/usr/share/elasticsearch/bin` |
| conf | 구성 파일 | `/etc/elasticsearch` |
| data | 데이터 파일 | `/var/lib/elasticsearch` |
| logs | 로그 파일 | `/var/log/elasticsearch` |
| plugins | 플러그인 파일 | `/usr/share/elasticsearch/plugins` |
| pid | PID 파일 | `/var/run/elasticsearch` |

#### 경로 설정

프로덕션에서는 `path.data`와 `path.logs`를 `$ES_HOME` 외부 위치로 설정하는 것이 강력히 권장됩니다. 그렇지 않으면 업그레이드 중에 `$ES_HOME`의 파일이 삭제될 위험이 있습니다.

```yaml
path:
  data: /var/data/elasticsearch
  logs: /var/log/elasticsearch
```

> 경고: 데이터 디렉토리의 파일 시스템 백업을 시도하지 마세요. 그러한 백업을 복원하는 지원되는 방법이 없습니다. 대신 스냅샷 및 복원을 사용하여 안전하게 백업하세요.

> 경고: 데이터 디렉토리에서 바이러스 스캐너를 실행하지 마세요. 바이러스 스캐너가 Elasticsearch의 올바른 작동을 방해하고 데이터 디렉토리의 내용을 수정할 수 있습니다.

---

## 중요한 Elasticsearch 구성

Elasticsearch의 일부 설정은 프로덕션으로 이동하기 전에 구성해야 합니다.

### 경로 설정

`path.data`와 `path.logs`를 `$ES_HOME` 외부 위치로 설정해야 합니다:

```yaml
path:
  data: /var/data/elasticsearch
  logs: /var/log/elasticsearch
```

### 클러스터 이름 설정

클러스터 이름을 설정합니다. 노드는 클러스터 이름을 공유하는 경우에만 클러스터에 참여할 수 있습니다:

```yaml
cluster.name: logging-prod
```

기본값은 `elasticsearch`입니다.

### 노드 이름 설정

각 노드에 고유한 이름을 설정합니다:

```yaml
node.name: prod-data-2
```

기본적으로 Elasticsearch는 노드 ID의 처음 7자를 노드 이름으로 사용합니다.

### 네트워크 호스트 설정

기본적으로 Elasticsearch는 루프백 주소(예: `127.0.0.1` 및 `[::1]`)에만 바인딩합니다. 다른 호스트의 노드와 클러스터를 형성하려면 `network.host`를 설정해야 합니다:

```yaml
network.host: 192.168.1.10
```

`network.host`에 값을 제공하면 Elasticsearch는 개발 모드에서 프로덕션 모드로 이동한다고 가정합니다.

#### 특수 값

`network.host` 설정에는 몇 가지 특수 값을 사용할 수 있습니다:

| 값 | 설명 |
|----|------|
| `_local_` | 시스템의 루프백 주소 |
| `_site_` | 시스템의 사이트 로컬 주소 |
| `_global_` | 시스템의 글로벌 범위 주소 |
| `_[networkInterface]_` | 지정된 네트워크 인터페이스의 주소 |
| `0.0.0.0` | 사용 가능한 모든 네트워크 인터페이스에 바인딩 |

### 디스커버리 설정

프로덕션으로 이동하기 전에 두 가지 중요한 디스커버리 및 클러스터 형성 설정을 구성해야 합니다.

#### discovery.seed_hosts

다른 호스트의 노드와 클러스터를 형성하려면 `discovery.seed_hosts` 정적 설정을 사용합니다. 이 설정은 클러스터에서 마스터 자격이 있고 활성 상태이며 연락 가능한 다른 노드의 목록을 제공합니다:

```yaml
discovery.seed_hosts:
   - 192.168.1.10:9300
   - 192.168.1.11
   - seeds.mydomain.com
   - [0:0:0:0:0:ffff:c0a8:10c]:9301
```

포트가 지정되지 않으면 기본적으로 `transport.profiles.default.port`가 사용되고 `transport.port`로 폴백합니다. 호스트 이름이 여러 IP 주소로 확인되는 경우 노드는 확인된 모든 주소에서 다른 노드를 검색하려고 시도합니다.

#### cluster.initial_master_nodes

프로덕션 모드에서 새 클러스터를 처음 시작할 때 첫 번째 선거에서 투표해야 하는 마스터 자격 노드를 명시적으로 나열해야 합니다. 모든 마스터 자격 노드에서 `cluster.initial_master_nodes` 설정을 사용하여 이 목록을 설정합니다:

```yaml
cluster.initial_master_nodes:
   - master-node-a
   - master-node-b
   - master-node-c
```

> 중요: 클러스터가 처음 시작된 후에는 모든 노드의 구성에서 `cluster.initial_master_nodes`를 제거해야 합니다. 대신 `discovery.seed_hosts` 또는 `discovery.seed_providers`를 구성하세요.

### 힙 크기 설정

기본적으로 Elasticsearch는 노드의 역할과 총 메모리를 기반으로 JVM 힙 크기를 자동으로 설정합니다. 대부분의 프로덕션 환경에서는 기본 크기 조정이 권장됩니다.

필요한 경우 JVM 힙 크기를 수동으로 설정하여 기본 크기 조정을 재정의할 수 있습니다. 힙 크기를 구성하려면 `jvm.options.d/` 디렉토리에 `.options` 확장자를 가진 사용자 정의 JVM 옵션 파일을 만듭니다:

```
-Xms4g
-Xmx4g
```

#### 힙 크기 가이드라인

- `Xms`와 `Xmx`를 각 Elasticsearch 노드에서 사용 가능한 총 메모리의 50% 이하로 설정합니다. Elasticsearch는 JVM 힙 외의 다른 목적으로도 메모리가 필요합니다.
- `Xms`와 `Xmx`를 압축된 일반 객체 포인터(oops)의 임계값 이하로 설정합니다. 정확한 임계값은 다르지만 대부분의 시스템에서 26GB가 안전하며 일부 시스템에서는 30GB까지 가능합니다.
- 최소값과 최대값은 동일해야 합니다.

힙이 많을수록 Elasticsearch가 내부 캐시에 사용할 수 있는 메모리가 많아집니다. 그러나 이는 운영 체제가 파일 시스템 캐시에 사용할 메모리를 줄입니다. 더 큰 힙은 또한 더 긴 가비지 컬렉션 일시 중지를 유발할 수 있습니다.

### JVM 옵션 설정

JVM 옵션은 일반적으로 번들된 `jvm.options` 파일을 수정하지 않고 `jvm.options.d/` 디렉토리에 사용자 정의 JVM 옵션 파일을 추가하여 설정합니다.

#### jvm.options 파일 위치

- tar.gz/zip: `$ES_HOME/config/jvm.options.d/`
- Debian/RPM: `/etc/elasticsearch/jvm.options.d/`

#### JVM 힙 덤프 경로

기본적으로 Elasticsearch는 JVM이 메모리 부족 예외에서 힙을 기본 데이터 디렉토리에 덤프하도록 구성합니다:

- RPM 및 Debian 패키지: `/var/lib/elasticsearch`
- tar.gz: Elasticsearch 설치의 루트 아래 `data` 디렉토리

#### 예제: 사용자 정의 JVM 옵션 파일

```
# jvm.options.d/gc.options

# GC 로깅 활성화
-Xlog:gc*,gc+age=trace,safepoint:file=logs/gc.log:utctime,pid,tags:filecount=32,filesize=64m
```

---

## 보안 설정

일부 설정은 민감하며 파일 시스템 권한에 의존하여 값을 보호하는 것만으로는 충분하지 않습니다. 이 사용 사례를 위해 Elasticsearch와 Kibana는 비밀번호, API 키, 토큰과 같은 민감한 구성 값을 저장하기 위한 보안 키스토어를 제공합니다.

보안 설정은 표준 `elasticsearch.yml` 파일이 아닌 제품별 키스토어에 추가해야 하므로 종종 키스토어 설정이라고 합니다. 일반 설정과 달리 암호화되어 저장 시 보호되며 일반 구성 파일이나 환경 변수를 통해 읽거나 수정할 수 없습니다.

### elasticsearch-keystore 도구

`elasticsearch-keystore` 명령은 Elasticsearch 키스토어의 보안 설정을 관리합니다.

#### 키스토어 생성

```bash
bin/elasticsearch-keystore create
```

키스토어 비밀번호를 입력하라는 메시지가 표시되지만 비밀번호 설정은 선택 사항입니다. 비밀번호를 입력하지 않고 Enter를 누르면 비밀번호 보호 없이 키스토어 파일이 생성됩니다.

#### 키스토어 나열

```bash
bin/elasticsearch-keystore list
```

#### 설정 추가

```bash
bin/elasticsearch-keystore add the.setting.name.to.set
```

설정 값을 입력하라는 메시지가 표시됩니다. Elasticsearch 키스토어가 비밀번호로 보호된 경우 비밀번호도 입력하라는 메시지가 표시됩니다.

#### 설정 제거

```bash
bin/elasticsearch-keystore remove the.setting.name.to.remove
```

#### 키스토어 비밀번호 변경 또는 설정

```bash
bin/elasticsearch-keystore passwd
```

키스토어가 비밀번호로 보호된 경우 현재 비밀번호와 새 비밀번호를 입력하라는 메시지가 표시됩니다.

### 재로드 가능한 보안 설정

키스토어 내용에 대한 변경 사항은 실행 중인 Elasticsearch 노드에 자동으로 적용되지 않습니다. 설정을 다시 읽으려면 노드 재시작이 필요합니다. 그러나 특정 보안 설정은 재로드 가능으로 표시됩니다. 이러한 설정은 보안 설정 다시 로드 API를 사용하여 실행 중인 노드에서 다시 읽고 적용할 수 있습니다:

```bash
POST _nodes/reload_secure_settings
{
  "secure_settings_password": "keystore-password"
}
```

### 특수 문자 경고

특수 문자를 부적절하게 처리하면 인증 실패 및 서비스 중단이 발생할 수 있습니다:

- `\!` 조합은 `!`로만 저장됩니다. 비밀번호가 의도한 대로 저장되지 않으면 인증 실패가 발생할 수 있습니다.
- 비밀번호 주위에 따옴표를 사용하면 따옴표가 비밀번호의 일부가 됩니다. 이로 인해 키스토어에서 검색할 때 비밀번호가 올바르지 않을 수 있습니다.

---

## 중요한 시스템 구성

이상적으로 Elasticsearch는 서버에서 단독으로 실행되어 사용 가능한 모든 리소스를 사용해야 합니다. 이를 위해 Elasticsearch를 실행하는 사용자가 기본적으로 허용되는 것보다 더 많은 리소스에 액세스할 수 있도록 운영 체제를 구성해야 합니다.

프로덕션으로 이동하기 전에 다음 설정을 고려해야 합니다:

1. 시스템 설정 구성
2. 스와핑 비활성화
3. 가상 메모리 증가
4. 최대 스레드 수 증가
5. 파일 디스크립터 제한 증가 (Linux 및 MacOS만 해당)
6. JNA 임시 디렉토리가 실행 파일을 허용하는지 확인 (Linux만 해당)
7. TCP 재전송 타임아웃 감소 (Linux만 해당)

### 개발 모드 vs 프로덕션 모드

기본적으로 Elasticsearch는 개발 모드에서 작업 중이라고 가정합니다. 위의 설정 중 올바르게 구성되지 않은 것이 있으면 Elasticsearch 로그 파일에 경고가 기록되지만 Elasticsearch 노드를 시작하고 실행할 수 있습니다.

`network.host`와 같은 네트워크 설정을 구성하면 Elasticsearch는 프로덕션으로 이동한다고 가정하고 위의 경고를 예외로 업그레이드합니다. 이러한 예외는 Elasticsearch 노드가 시작되지 않도록 합니다.

---

### 시스템 설정 구성

시스템 설정을 구성하는 위치는 Elasticsearch를 설치하는 데 사용한 패키지와 사용 중인 운영 체제에 따라 다릅니다.

#### ulimit 사용 (임시)

Linux 시스템에서 `ulimit`를 사용하여 일시적으로 리소스 제한을 변경할 수 있습니다. Elasticsearch를 시작할 셸에서 root로 전환하고 ulimit 명령을 실행해야 합니다:

```bash
sudo su
ulimit -n 65535
su elasticsearch
```

#### /etc/security/limits.conf 사용 (영구)

Linux 시스템에서 `/etc/security/limits.conf` 파일을 편집하여 특정 사용자에 대한 영구 제한을 설정할 수 있습니다:

```
elasticsearch  -  nofile  65535
```

이 변경은 다음에 `elasticsearch` 사용자가 새 세션을 열 때 적용됩니다.

#### systemd 사용

systemd를 사용하는 배포판에서 시스템 리소스 제한은 `/etc/security/limits.conf`가 아닌 systemd를 통해 구성해야 합니다.

시스템 설정을 재정의하려면 `/etc/systemd/system/elasticsearch.service.d/override.conf` 파일을 추가합니다 (또는 `sudo systemctl edit elasticsearch`를 실행하면 기본 편집기에서 자동으로 파일이 열립니다):

```ini
[Service]
LimitMEMLOCK=infinity
```

변경 후 systemd 데몬을 다시 로드합니다:

```bash
sudo systemctl daemon-reload
```

---

### 스와핑 비활성화

대부분의 운영 체제는 파일 시스템 캐시에 가능한 한 많은 메모리를 사용하고 사용하지 않는 애플리케이션 메모리를 적극적으로 스왑 아웃하려고 합니다. 이로 인해 JVM 힙의 일부 또는 심지어 실행 가능 페이지가 디스크로 스왑 아웃될 수 있습니다.

스와핑은 성능, 노드 안정성에 매우 해롭고 어떤 대가를 치르더라도 피해야 합니다. 가비지 컬렉션이 밀리초 대신 분 단위로 지속되고 노드가 느리게 응답하거나 클러스터에서 연결이 끊어질 수 있습니다.

스와핑을 비활성화하는 세 가지 방법이 있습니다. 선호하는 옵션은 스왑을 완전히 비활성화하는 것입니다. 이것이 옵션이 아닌 경우 환경에 따라 스왑 최소화 또는 메모리 잠금 중 어느 것을 선호할지 결정합니다.

#### 모든 스왑 비활성화

Linux에서 다음 명령을 실행하여 일시적으로 스왑을 비활성화합니다:

```bash
sudo swapoff -a
```

영구적으로 비활성화하려면 `/etc/fstab` 파일을 편집하고 `swap`이라는 단어가 포함된 모든 줄을 주석 처리합니다.

Windows에서는 System Properties → Advanced → Performance → Settings → Advanced → Virtual memory에서 페이징 파일을 비활성화하여 동일한 효과를 얻을 수 있습니다.

#### 스왑 최소화

Linux 시스템에서 `vm.swappiness`를 `1`로 설정합니다. 이것은 스왑이 비활성화된 것처럼 정상적인 상황에서 스왑을 줄이면서도 전체 시스템이 긴급 상황에서 스왑할 수 있도록 합니다:

```bash
sysctl -w vm.swappiness=1
```

#### 메모리 잠금 활성화 (bootstrap.memory_lock)

mlockall(Unix) 또는 virtual lock(Windows)을 통해 JVM이 메모리에 힙을 잠그도록 요청하여 스와핑을 방지할 수 있습니다. `elasticsearch.yml`에서 다음을 설정합니다:

```yaml
bootstrap.memory_lock: true
```

> 경고: `mlockall`은 사용 가능한 것보다 더 많은 메모리를 할당하려고 하면 JVM 또는 셸 세션이 종료될 수 있습니다!

Elasticsearch를 시작한 후 다음 요청을 사용하여 이 설정이 성공적으로 적용되었는지 확인할 수 있습니다:

```bash
GET _nodes?filter_path=.mlockall
```

`mlockall`이 `false`이면 메모리 잠금 요청이 실패한 것입니다. "Unable to lock JVM Memory"라는 단어가 포함된 로그 줄도 표시됩니다.

가장 가능한 원인은 Linux/Unix 시스템에서 Elasticsearch를 실행하는 사용자에게 메모리를 잠글 수 있는 권한이 없기 때문입니다:

##### ulimit 사용

root로 Elasticsearch를 시작하기 전에 `ulimit -l unlimited`를 설정합니다:

```bash
ulimit -l unlimited
```

##### /etc/security/limits.conf 사용

`/etc/security/limits.conf`에서 `elasticsearch` 사용자에 대해 `memlock`을 `unlimited`로 설정합니다:

```
elasticsearch - memlock unlimited
```

##### systemd 사용

`/etc/systemd/system/elasticsearch.service.d/override.conf` 파일에서:

```ini
[Service]
LimitMEMLOCK=infinity
```

---

### 파일 디스크립터

Elasticsearch는 많은 파일 디스크립터 또는 파일 핸들을 사용합니다. 파일 디스크립터가 부족하면 치명적일 수 있으며 데이터 손실로 이어질 가능성이 높습니다. Elasticsearch를 실행하는 사용자에 대한 열린 파일 디스크립터 제한을 65,535 이상으로 늘려야 합니다.

#### .zip 및 .tar.gz 패키지

root로 Elasticsearch를 시작하기 전에 `ulimit -n 65535`를 설정하거나 `/etc/security/limits.conf`에서 `nofile`을 65535로 설정합니다:

```
elasticsearch  -  nofile  65535
```

#### macOS에서

macOS에서는 더 높은 파일 디스크립터 제한을 사용하기 위해 JVM 옵션 `-XX:-MaxFDLimit`을 Elasticsearch에 전달해야 합니다.

#### RPM 및 Debian 패키지

RPM 및 Debian 패키지는 이미 최대 파일 디스크립터 수를 65535로 기본 설정하며 추가 구성이 필요하지 않습니다.

#### 확인

다음 API를 사용하여 각 노드에 대해 구성된 `max_file_descriptors`를 확인할 수 있습니다:

```bash
GET _nodes/stats/process?filter_path=.max_file_descriptors
```

---

### 가상 메모리

Elasticsearch는 기본적으로 인덱스를 저장하기 위해 mmapfs 디렉토리를 사용합니다. 기본 운영 체제의 mmap 카운트 제한이 너무 낮아 메모리 부족 예외가 발생할 수 있습니다.

운영 체제의 기본 `vm.max_map_count` 값이 1048576 이상이면 구성 변경이 필요하지 않습니다. 기본값이 1048576보다 낮으면 `vm.max_map_count` 파라미터를 1048576으로 구성합니다.

#### Linux에서 설정

root로 sysctl 명령을 실행하여 제한을 늘립니다:

```bash
sysctl -w vm.max_map_count=1048576
```

영구적으로 설정하려면 `/etc/sysctl.conf`에서 `vm.max_map_count` 설정을 업데이트합니다:

```
vm.max_map_count=1048576
```

재부팅 후 확인:

```bash
sysctl vm.max_map_count
```

RPM 및 Debian 패키지는 이 설정을 자동으로 구성합니다.

---

### 스레드 수

Elasticsearch는 다양한 유형의 작업에 대해 여러 스레드 풀을 사용합니다. 필요할 때 새 스레드를 만들 수 있어야 합니다. Elasticsearch를 실행하는 사용자가 최소 4096개의 스레드를 만들 수 있는지 확인합니다.

systemd를 통해:

```ini
[Service]
LimitNPROC=4096
```

또는 `/etc/security/limits.conf`에서:

```
elasticsearch  -  nproc  4096
```

---

### DNS 캐시 설정

Elasticsearch는 보안 관리자가 있는 상태로 실행됩니다. 보안 관리자가 있는 상태에서 JVM은 기본적으로 양성 호스트 이름 확인을 무기한으로 캐시하고 음성 호스트 이름 확인을 10초 동안 캐시합니다. Elasticsearch는 양성 조회를 60초 동안 캐시하고 음성 조회를 10초 동안 캐시하는 기본값으로 이 동작을 재정의합니다.

이 값은 JVM 옵션에서 `es.networkaddress.cache.ttl` 및 `es.networkaddress.cache.negative.ttl`을 편집하여 수정할 수 있습니다.

---

### TCP 재전송 타임아웃 감소 (Linux만 해당)

TCP는 때때로 신뢰할 수 없는 네트워크를 통해 신뢰할 수 있는 통신을 제공합니다. 운영 체제는 발신자에게 문제를 알리기 전에 손실된 메시지를 여러 번 재전송합니다.

대부분의 Linux 배포판은 기본적으로 손실된 패킷을 15번 재전송합니다. 재전송은 지수적으로 백오프되므로 이 15번의 재전송을 완료하는 데 900초 이상이 걸립니다. 이는 Linux가 네트워크 파티션 또는 실패한 노드를 이 방법으로 감지하는 데 몇 분이 걸린다는 것을 의미합니다.

Windows는 기본적으로 5번만 재전송하며 이는 약 13초의 타임아웃에 해당합니다.

root로 다음 명령을 실행하여 최대 TCP 재전송 수를 5로 줄입니다:

```bash
sysctl -w net.ipv4.tcp_retries2=5
```

이 값을 영구적으로 설정하려면 `/etc/sysctl.conf`에서 `net.ipv4.tcp_retries2` 설정을 업데이트합니다.

---

## 부트스트랩 검사

종합적으로 많은 사용자가 Elasticsearch의 중요한 구성 설정을 구성하지 않아 예기치 않은 문제에 대해 매우 나쁜 경험을 했습니다. Elasticsearch의 이전 버전에서는 이러한 설정 중 일부의 잘못된 구성이 경고로 기록되었습니다. 사용자가 때때로 이러한 로그 메시지를 놓치는 것은 당연합니다. 가능한 한 최상의 경험을 보장하기 위해 Elasticsearch는 시작 시 부트스트랩 검사를 수행합니다.

이러한 부트스트랩 검사는 다양한 Elasticsearch 및 시스템 설정을 검사하고 이러한 값을 Elasticsearch 작업에 안전한 값과 비교합니다. Elasticsearch가 개발 모드에 있는 경우 실패한 부트스트랩 검사는 Elasticsearch 로그에 경고로 나타납니다. Elasticsearch가 프로덕션 모드에 있는 경우 실패한 부트스트랩 검사는 Elasticsearch가 시작을 거부하도록 합니다.

### 개발 모드 vs 프로덕션 모드

Elasticsearch가 비루프백 주소를 통해 다른 머신과 클러스터를 형성할 수 없으면 개발 모드로 간주되고, 비루프백 주소를 통해 클러스터에 참여할 수 있으면 프로덕션 모드입니다.

### 단일 노드에 대한 부트스트랩 검사 강제

프로덕션에서 단일 노드를 실행하는 경우 전송을 외부 인터페이스에 바인딩하지 않거나 전송을 외부 인터페이스에 바인딩하고 디스커버리 유형을 단일 노드로 설정하여 부트스트랩 검사를 회피할 수 있습니다. 이 특정 상황에서는 JVM 옵션에서 시스템 속성 `es.enforce.bootstrap.checks`를 `true`로 설정하여 부트스트랩 검사 실행을 강제하는 것이 강력히 권장됩니다.

### 부트스트랩 검사 목록

#### 힙 크기 검사

기본적으로 Elasticsearch는 노드의 역할과 총 메모리를 기반으로 JVM 힙 크기를 자동으로 조정합니다. 수동으로 기본 크기 조정을 재정의하고 다른 초기 및 최대 힙 크기로 JVM을 시작하면 시스템 사용 중에 JVM이 힙 크기를 조정하면서 일시 중지될 수 있습니다.

`bootstrap.memory_lock`을 활성화하면 JVM은 시작 시 초기 힙 크기를 잠급니다. 초기 힙 크기가 최대 힙 크기와 같지 않으면 크기 조정 후 일부 JVM 힙이 잠기지 않을 수 있습니다. 이러한 문제를 방지하려면 초기 힙 크기와 최대 힙 크기가 같은 상태로 JVM을 시작합니다.

#### 파일 디스크립터 검사

파일 디스크립터는 많은 파일을 추적하는 데 사용되는 Unix 구조입니다. Unix에서는 모든 것이 파일입니다. Elasticsearch에서는 파일 디스크립터가 필수적이며, 많이 사용됩니다. 이 검사는 사용자가 OS 기본 설정을 사용하면 실패할 수 있습니다.

#### 메모리 잠금 검사

Elasticsearch 프로세스의 주소 공간을 RAM에 잠그면 Elasticsearch 메모리가 스왑 아웃되지 않습니다. `bootstrap.memory_lock`이 `true`로 설정되어 있지만 힙을 잠그지 못한 경우 메모리 잠금 검사가 실패합니다.

#### 최대 스레드 수 검사

Elasticsearch는 다양한 유형의 작업에 대해 여러 스레드 풀을 사용합니다. 필요할 때 새 스레드를 만들 수 있어야 합니다. 이 검사는 Elasticsearch가 필요한 모든 스레드를 만들 수 있는 충분한 권한이 있는지 확인합니다.

#### 가상 메모리 검사

Elasticsearch와 Lucene은 mmap을 사용하여 인덱스의 일부를 Elasticsearch 주소 공간에 매핑합니다. 이렇게 하면 특정 인덱스 데이터가 JVM 힙 외부에 유지되지만 매우 빠른 액세스를 위해 메모리에 있습니다. 이것이 효과적이려면 Elasticsearch에 충분한 가상 메모리가 필요합니다. 이 검사는 커널이 프로세스에 최소 1048576개의 메모리 매핑 영역을 허용하도록 구성되었는지 확인합니다.

#### 최대 파일 크기 검사

세그먼트 파일과 전송 로그를 포함하여 Elasticsearch가 작성하는 파일 중 일부는 매우 커질 수 있습니다. OS에서 최대 파일 크기 제한이 있는 시스템에서는 이러한 파일이 한계에 도달할 수 있습니다. 이 검사는 해당 제한이 없거나 최소한 특정 값 이상인지 확인합니다.

---

## Elasticsearch 시작 및 중지

Elasticsearch를 시작하는 방법은 설치 방법에 따라 다릅니다.

### 아카이브 패키지 (.tar.gz)

명령줄에서 시작:

```bash
./bin/elasticsearch
```

데몬으로 시작:

```bash
./bin/elasticsearch -d -p pid
```

종료:

```bash
pkill -F pid
```

### Debian/RPM 패키지 (systemd)

시작:

```bash
sudo systemctl start elasticsearch.service
```

중지:

```bash
sudo systemctl stop elasticsearch.service
```

상태 확인:

```bash
sudo systemctl status elasticsearch.service
```

부팅 시 자동 시작 활성화:

```bash
sudo /bin/systemctl daemon-reload
sudo /bin/systemctl enable elasticsearch.service
```

자동 시작 비활성화:

```bash
sudo systemctl disable elasticsearch.service
```

#### 비밀번호 보호 키스토어

Elasticsearch 키스토어를 비밀번호로 보호한 경우 로컬 파일과 systemd 환경 변수를 사용하여 systemd에 키스토어 비밀번호를 제공해야 합니다:

```bash
echo "keystore_password" > /path/to/my_pwd_file.tmp
chmod 600 /path/to/my_pwd_file.tmp
sudo systemctl set-environment ES_KEYSTORE_PASSPHRASE_FILE=/path/to/my_pwd_file.tmp
sudo systemctl start elasticsearch.service
```

#### systemd 타임아웃 설정

기본적으로 Elasticsearch는 systemd에 `TimeoutStartSec` 파라미터를 `900s`로 설정합니다. systemd 버전 238 이상을 실행하는 경우 Elasticsearch는 시작 타임아웃을 자동으로 연장할 수 있습니다. 238 이전 버전의 systemd는 타임아웃 연장 메커니즘을 지원하지 않으며 구성된 타임아웃 내에 완전히 시작되지 않으면 Elasticsearch 프로세스를 종료합니다.

### Docker

시작:

```bash
docker start es01
```

중지:

```bash
docker stop es01
```

### Windows

명령줄에서 시작:

```cmd
.\bin\elasticsearch.bat
```

서비스로 설치된 경우:

```cmd
.\bin\elasticsearch-service.bat start
.\bin\elasticsearch-service.bat stop
```

---

## 클러스터에 노드 추가

클러스터에 노드를 추가하면 용량과 안정성이 향상됩니다. 기본적으로 노드는 데이터 노드이면서 클러스터를 제어하는 마스터 노드로 선출될 자격이 있습니다.

클러스터에 더 많은 노드를 추가하면 레플리카 샤드가 자동으로 할당됩니다. 모든 프라이머리 및 레플리카 샤드가 활성화되면 클러스터 상태가 녹색으로 변경됩니다.

### 등록 토큰으로 노드 추가 (권장)

새 노드를 클러스터에 등록하려면 클러스터의 기존 노드에서 `elasticsearch-create-enrollment-token` 도구를 사용하여 등록 토큰을 만듭니다:

```bash
bin/elasticsearch-create-enrollment-token -s node
```

그런 다음 새 노드에서 `--enrollment-token` 파라미터로 Elasticsearch를 시작합니다:

```bash
bin/elasticsearch --enrollment-token <enrollment-token>
```

> 참고: 등록 토큰의 수명은 30분입니다. 추가하는 각 새 노드에 대해 새 등록 토큰을 만들어야 합니다.

### 네트워크 구성 요구 사항

같은 호스트에 있는 노드만 추가 구성 없이 클러스터에 참여할 수 있습니다. 다른 호스트의 노드가 클러스터에 참여하도록 하려면 `transport.host`를 지원되는 값(예: 제안된 값 `0.0.0.0`을 주석 해제)으로 설정하거나 다른 호스트에서 도달할 수 있는 인터페이스에 바인딩된 IP 주소로 설정해야 합니다.

### RPM/Debian 패키지 설치

RPM 또는 Debian 패키지를 사용하여 새 Elasticsearch 노드를 설치한 경우 `elasticsearch-reconfigure-node` 도구에 등록 토큰을 전달하여 구성 프로세스를 단순화할 수 있습니다.

### 수동으로 노드 추가

등록 토큰 없이 수동으로 노드를 추가하려면 다음 설정을 구성해야 합니다:

1. 동일한 `cluster.name` 설정
2. `discovery.seed_hosts` 구성
3. 보안 인증서 구성 (TLS)

---

## 전체 클러스터 재시작 및 롤링 재시작

전체 클러스터 재시작을 수행해야 하는 상황과 롤링 재시작을 수행해야 하는 상황이 있습니다. 전체 클러스터 재시작의 경우 클러스터의 모든 노드를 종료하고 다시 시작하고, 롤링 재시작의 경우 한 번에 하나의 노드만 종료하므로 서비스가 중단되지 않습니다.

### 롤링 재시작 절차

#### 1. 샤드 할당 비활성화

노드를 종료하면 할당 프로세스는 기본적으로 `index.unassigned.node_left.delayed_timeout`(기본값 1분) 후에 해당 노드의 샤드를 클러스터의 다른 노드로 복제하기 시작합니다. 이 작업에는 많은 I/O가 포함될 수 있습니다. 노드가 곧 다시 시작되므로 이 I/O는 불필요합니다.

```bash
PUT _cluster/settings
{
  "persistent": {
    "cluster.routing.allocation.enable": "primaries"
  }
}
```

#### 2. 불필요한 인덱싱 중지 및 플러시 수행

롤링 재시작 중에 인덱싱을 계속할 수 있지만 불필요한 인덱싱을 일시적으로 중지하고 플러시를 수행하면 샤드 복구가 더 빨라질 수 있습니다:

```bash
POST /_flush
```

#### 3. 머신 러닝 작업 및 데이터피드 일시 중지 (선택 사항)

업그레이드 모드 설정 API를 사용하여 작업을 일시적으로 중지하거나 모든 데이터피드를 중지하고 모든 작업을 닫습니다:

```bash
POST _ml/set_upgrade_mode?enabled=true
```

#### 4. 단일 노드 종료

systemd 사용:

```bash
sudo systemctl stop elasticsearch.service
```

데몬으로 실행 중인 경우:

```bash
pkill -F pid
```

#### 5. 유지 관리 수행 및 노드 재시작

#### 6. 노드가 클러스터에 다시 참여할 때까지 대기

노드가 클러스터에 참여할 때까지 기다립니다:

```bash
GET _cat/nodes
```

#### 7. 샤드 할당 다시 활성화

```bash
PUT _cluster/settings
{
  "persistent": {
    "cluster.routing.allocation.enable": null
  }
}
```

#### 8. 클러스터 복구 대기

다음 노드를 업그레이드하기 전에 클러스터가 샤드 할당을 완료할 때까지 기다립니다:

```bash
GET _cat/health?v=true
```

상태 열이 `green`으로 전환될 때까지 기다립니다. 녹색이 되면 모든 프라이머리 및 레플리카 샤드가 할당되었습니다.

#### 9. 반복

클러스터의 각 노드에 대해 단계 4-8을 반복합니다.

#### 10. 머신 러닝 작업 재시작 (선택 사항)

```bash
POST _ml/set_upgrade_mode?enabled=false
```

---

## Elasticsearch 업그레이드

### 롤링 업그레이드

롤링 업그레이드 중에 클러스터는 정상적으로 계속 작동합니다. 그러나 클러스터의 모든 노드가 업그레이드될 때까지 새 기능은 비활성화되거나 이전 버전과 호환되는 모드로 작동합니다.

Elasticsearch는 마이너 버전 간 롤링 업그레이드를 지원합니다:

- 5.6에서 6.8로
- 6.8에서 7.17.29로
- 7.17.29에서 8.x로
- 8.x에서 9.x로

동일한 클러스터에서 업그레이드 기간을 초과하여 여러 버전의 Elasticsearch를 실행하는 것은 지원되지 않습니다. 업그레이드된 노드에서 이전 버전을 실행하는 노드로 샤드를 복제할 수 없기 때문입니다.

### 업그레이드 전 고려 사항

- Elasticsearch 8.0 이상으로 업그레이드할 때는 롤링 업그레이드 대신 전체 클러스터 재시작을 선택하더라도 먼저 7.17로 업그레이드해야 합니다.
- 프로덕션 클러스터를 업그레이드하기 전에 격리된 환경에서 업그레이드를 테스트하는 것이 좋습니다.
- 업그레이드하기 전에 데이터의 스냅샷을 만들어 백업합니다.

---

## 노드 역할

Elasticsearch 인스턴스를 시작할 때마다 노드를 시작합니다. 연결된 노드의 모음을 클러스터라고 합니다.

모든 노드는 클러스터의 다른 노드에 대해 알고 있으며 클라이언트 요청을 적절한 노드로 전달할 수 있습니다. 또한 각 노드는 하나 이상의 목적 역할을 수행합니다.

### 주요 노드 역할

| 역할 | 설명 |
|------|------|
| `master` | 마스터 자격 노드. 마스터로 선출되어 클러스터를 제어할 수 있습니다. |
| `data` | 데이터 노드. 데이터를 보유하고 CRUD, 검색, 집계와 같은 데이터 관련 작업을 수행합니다. |
| `data_content` | 콘텐츠 데이터 노드. 복잡한 검색 및 집계를 처리하도록 최적화됩니다. |
| `data_hot` | 핫 데이터 노드. 가장 최근의, 가장 자주 검색되는 시계열 데이터를 보유합니다. |
| `data_warm` | 웜 데이터 노드. 자주 쿼리되지 않는 시계열 데이터를 보유합니다. |
| `data_cold` | 콜드 데이터 노드. 드물게 액세스하는 읽기 전용 인덱스를 보유합니다. |
| `data_frozen` | 프로즌 데이터 노드. 완전히 마운트된 검색 가능한 스냅샷 인덱스를 보유합니다. |
| `ingest` | 수집 노드. 문서를 인덱싱하기 전에 수집 파이프라인을 적용하여 변환하고 보강합니다. |
| `ml` | 머신 러닝 노드. 머신 러닝 작업을 실행합니다. |
| `remote_cluster_client` | 원격 클러스터 클라이언트 노드. 원격 클러스터에 연결할 수 있습니다. |
| `transform` | 변환 노드. 변환을 수행합니다. |
| `voting_only` | 투표 전용 노드. 마스터 선거에서 투표에 참여하지만 마스터로 선출될 수 없습니다. |

### 노드 역할 구성

`elasticsearch.yml`에서 `node.roles`를 설정하여 노드 역할을 구성합니다:

```yaml
node.roles: [ master, data, ingest ]
```

빈 목록을 설정하면 노드는 코디네이팅 전용 노드가 됩니다:

```yaml
node.roles: [ ]
```

### 코디네이팅 노드

마스터 의무를 처리하고 데이터를 보유하며 문서를 전처리하는 기능을 제거하면 요청을 라우팅하고 검색 축소 단계를 처리하며 대량 인덱싱을 배포할 수만 있는 코디네이팅 노드가 남습니다. 본질적으로 코디네이팅 전용 노드는 스마트 로드 밸런서처럼 동작합니다.

코디네이팅 전용 노드는 데이터 및 마스터 자격 노드에서 코디네이팅 노드 역할을 오프로드하여 대규모 클러스터에 이점을 줄 수 있습니다.

---

## 참고 자료

- [Elasticsearch 공식 문서](https://www.elastic.co/guide/en/elasticsearch/reference/current/setup.html)
- [Elasticsearch 설치 가이드](https://www.elastic.co/docs/deploy-manage/deploy/self-managed/installing-elasticsearch)
- [Elasticsearch 구성 참조](https://www.elastic.co/docs/reference/elasticsearch/configuration-reference)
- [부트스트랩 검사](https://www.elastic.co/docs/deploy-manage/deploy/self-managed/bootstrap-checks)
- [중요한 시스템 구성](https://www.elastic.co/guide/en/elasticsearch/reference/current/system-config.html)
- [디스커버리 및 클러스터 형성](https://www.elastic.co/docs/deploy-manage/distributed-architecture/discovery-cluster-formation/discovery-hosts-providers)
- [노드 역할](https://www.elastic.co/docs/deploy-manage/distributed-architecture/clusters-nodes-shards/node-roles)
- [보안 설정](https://www.elastic.co/docs/deploy-manage/security/secure-settings)
- [Docker로 Elasticsearch 설치](https://www.elastic.co/docs/deploy-manage/deploy/self-managed/install-elasticsearch-with-docker)
