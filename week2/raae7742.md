# Ch 2. 카프카 빠르게 시작해보기

## AWS EC2 세팅하기

### 1. AWS EC2 서버의 Inbound 보안 그룹 규칙 설정

카프카를 사용하기 위해선 다음 두 개의 포트를 열어줘야 한다.

- 카프카 브로커 기본 포트: 9092
- 주키퍼 기본 포트: 2181

<br>

테스트 용도로 사용할 예정이므로 모든 소스 IP를 대상으로 두 포트를 열어준다.

<br>

<br>

### 2. 인스턴스에 자바,주키퍼, 카프카 브로커 설치 및 실행

**자바 설치 방법**

```bash
# 자바 설치
$ sudo yum install -y java-1.8.0-openjdk-edevel.x86_64
```

<br>

**카프카 설치 방법**

```bash
# 카프카 설치
$ wget https://archive.apache.org/dist/kafka/2.5.0/kafka_2.12-2.5.0.tgz
$ tar xvf kafka_2.12-2.5.0.tgz
$ cd kafka_2.12-2.5.0
```

<br>

- 프리티어인 경우에는 힙 메모리 사이즈를 더 작게 지정한다.

```bash
$ export KAFKA_HEAP_OPTS="-Xmx400m -Xms400m"
$ echo $KAFKA_HEAP_OPTS
```

<br>

- 환경변수는 터미널 세션이 종료되면 초기화된다. `~/.bashrc` 파일에 선언문을 넣으면 해결된다.

```bash
$ vi ~/.bashrc
```

<br>

- 카프카 브로커 실행 시 메모리 설정 부분은 `[kafka-server-start.sh](http://kafka-server-start.sh)` 스크립트에서 확인할 수 있다.

```bash
$ cat bin/kafka-server-start.sh
```

<br>

<br>

**카프카 실행 옵션 설정 :** `advertised.listener`

- 카프카 클라이언트를 브로커와 연결할 때 사용한다.

```bash
$ vi config/server.properties

# ex) advertised.listener=PLAINTEXT://13.124.99.128:9092
advertised.listener=PLAINTEXT://${현재 인스턴스의 퍼블릭 IP}:9092

```

<br>

**주키퍼 설치 및 실행**

- 주키퍼 : 분산 코디네이션 서비스로 카프카와 함께 따라다니는 필수 애플리케이션
- `jps` : JVM 프로세스 상태를 보는 도구로 주키퍼 실행 여부를 확인할 때 사용

```bash
$ bin/zookeeper-server-start.sh -daemon config/zookeeper.properties
$ jps -vm
```

<br>

**카프카 브로커 실행**

```bash
$ bin/kafka-server-start.sh -daemon config/server.properties
$ jvm -m

# 로그 확인
$ tail -f logs/server.log
```

<br>

<br>

## 3. 로컬 컴퓨터와 통신하기

로컬 컴퓨터에서 리눅스를 실행해 다음 과정을 따라한다.

<br>

### 카프카 바이너리 패키지 설치

```bash
$ curl https://archive.apache.org/dist/kafka/2.5.0/kafka_2.12-2.5.0.tgz
$ tar -xfvf kafka.tgz
$ cd kafka_2.12-2.5.0

# {13.124.99.128} 부분에는 인스턴스의 퍼블릭 IP를 넣는다.
$ bin/kafka-broker-api-versions.sh --bootstrap-server 13.124.99.128:9092
```

<br>

`kafka-broker-api-versions.sh`

- 원격으로 카프카의 버전, broker.id, rack 정보, 각종 옵션들을 확인할 수 있다.

<br>

### 테스트 편의를 위한 hosts 설정

```bash
$ vi /etc/hosts
13.125.241.31 my-kafka
```

<br>

<br>

## 카프카의 커맨드 라인 툴 알아보기

### 1. `kafka-topics.sh`

- 토픽 관련 명령어 실행
- 토픽 : 카프카에서 데이터를 구분하는 가장 기본적인 개념

<br>

**토픽 생성하기**

```bash
$ bin/kafka-topics.sh \
	--create \
	--bootstrap-server my-kafka:9092 \
	--partitions 3 \
	--replication-factor 1 \
	--config retention.ms=172800000 \
	--topic hello.kafka
```

<br>

**토픽 리스트 조회**

```bash
$ bin/kafka-topics.sh --bootstrap-server my-kafka:9092 --list
```

<br>

**토픽 상세 조회**

```bash
$ bin/kafka-topics.sh --bootstrap-server my-kafka:9092 --describe --topic hello.kafka
```

<br>

**토픽 옵션 수정**

```bash
# 일반적인 옵션 수정 방법
$ bin/kafka-topics.sh --bootstrap-server my-kafka:9092 \ 
	--topic hello.kafka \
	--alter \
	--partitions 4

# retention.ms 수정 방법
$ bin/kafka-configs.sh --bootstrap-server my-kafka:9092 \
	--entity-type topics \
	--entity-name hello.kafka \
	--alter --add-config retention.ms=86400000
```

<br>

<br>

### 2. `kafka-console-producer.sh`

- 토픽의 파티션에 레코드를 저장한다.
- 레코드: 토픽에 넣는 데이터로  {key, value}의 쌍으로 이루어져 있다.
- 메시지 키가 동일한 경우에는 같은 파티션으로 전송된다.

<br>

```bash
$ bin/kafka-console-producer.sh --bootstrap-server my-kafka:9092
	--topic hello.kafka \
	--property "parse.key=true" \
	--property "key.separator=:"
>key1:no1
>key2:no2
>key3:no3
```

<br>

<br>

### `kafka-console-consumer.sh`

- 토픽에 저장된 레코드를 확인한다.
- 전송한 데이터의 순서가 출력 순서와 다를 수 있다.

```bash
$ bin/kafka-console-consumer.sh --bootstrap-server my=kafka:9092
	--topic hello.kafka \
	--property print.key=true \
	--property key.separator="-" \
	--from-beginning
key1-no1
key2-no2
key3-no3
...
```

<br>

<br>

### `kafka-consumer-groups.sh`

- 생성한 컨슈머 그룹의 리스트를 확인한다.

<br>

**전체 그룹 리스트 확인하기**

```bash
$ bin/kafka-consumer-groups.sh --bootstrap-server my-kafka:9092 --list
hello-group
```

<br>

**컨슈머 그룹이 가져가는 데이터 확인하기**

```bash
$ bin/kafka-consumer-groups.sh --bootstrap-server my-kafka:9092 \
	--group hello-group \
	--describe
```

<br>

<br>

### `kafka-verifiable-producer, consumer.sh`

- String 타입 메시지 값을 코드 없이 주고받는다.
- 카프카 클러스터 설치 완료 후 토픽에 데이터를 전송하는 네트워크 통신 테스트에 유용하다.

<br>

**데이터 전송하기**

```bash
$ bin/kafka-verifiable-producer.sh --bootstrap-server my-kafka:9092 \
	--max-messages 10 \
	--topic verify-test
```

<br>

**데이터 확인하기**

```bash
$ bin/kafka-verifiable-consumer.sh --bootstrap-server my-kafka:9092 \
	--topic verify-test \
	--group-id test-group
```

<br>

<br>

### `kafka-delete-records.sh`

- 이미 적재된 토픽의 데이터를 지운다.
- 가장 오래된 데이터부터 특정 시점 오프셋까지 삭제한다.

```bash
$ vi delete-topic.json
{"partitions": [{"topic": "test", "partition": 0, "offset": 50}], "version": 1}

$ bin/kafka-delete-records.sh --bootstrap-server my-kafka:9092 \
	--offset-json-file delete-topic.json
```
