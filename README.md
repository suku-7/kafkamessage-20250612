# Model
## kafkamessage-20250612(주문 ID(Order ID)를 메시지 키로 사용하여, 같은 주문과 관련된 모든 이벤트 동일 파티션에 관리)
https://labs.msaez.io/#/189596125/storming/kafka-scaling2

프로젝트 실행 가이드 및 터미널 참고용
이 문서는 프로젝트 실행 및 테스트에 필요한 터미널 명령어를 정리한 가이드입니다. 각 단계를 순서대로 따라하여 원활한 테스트를 진행해 보세요.

0. 재실행 시 초기화 (선택 사항)
프로젝트를 완전히 초기화하고 다시 시작해야 할 경우 다음 명령어를 사용합니다.

현재 Java 버전 확인:

Bash

java -version
초기화 스크립트 실행:

Bash

./init.sh
Kafka 디렉토리로 이동 후 Docker Compose 시작:
9092 포트가 실행되지 않는 경우에만 사용합니다.

Bash

cd kafka
docker-compose up
1. Java SDK 설치
sdkman을 통해 Java를 설치해야 할 경우 다음 명령어를 사용합니다.

Bash

sdk install java
2. Lombok 버전 수정
order, delivery, delivery-2nd 서비스의 Lombok 버전을 1.18.30으로 수정합니다.

3. MySQL 및 서비스 실행
MySQL 데이터베이스와 order, delivery 서비스를 순서대로 실행합니다.

MySQL 실행:

Bash

cd mysql
docker-compose up -d
Order 서비스 실행:

Bash

cd order
mvn clean spring-boot:run
Delivery 서비스 실행:

Bash

cd delivery
mvn clean spring-boot:run
4. Kafka 토픽 모니터링 시작
Kafka 토픽에 메시지가 발행되는 것을 실시간으로 확인하기 위해 컨슈머를 실행합니다.

Kafka 컨테이너 내부로 진입:

Bash

cd kafka
docker-compose exec kafka /bin/bash
bin 디렉토리로 이동 후 컨슈머 실행:
Kafka 토픽 kafka.scaling의 메시지를 처음부터 모니터링합니다.

Bash

cd /bin
./kafka-console-consumer --bootstrap-server localhost:9092 --topic kafka.scaling --from-beginning
5. 주문 생성 (Order Service)
새로운 주문을 생성하여 Kafka로 이벤트가 발행되는지 확인합니다.

Bash

http :8081/orders customerId=1000 productId=100 productName=TV qty=3 address=SEOUL
http :8081/orders customerId=2000 productId=100 productName=RADIO qty=3 address=PUSAN
6. 주문 수정 (Order Service)
특정 주문의 정보를 수정합니다. 이 변경 사항이 Kafka로 이벤트로 발행됩니다.

Bash

http PATCH :8081/orders/2 address=SEOUL
7. 주문 삭제 (Order Service)
특정 주문을 삭제합니다. 이 삭제 이벤트도 Kafka로 발행됩니다.

Bash

http DELETE :8081/orders/2
8. DB 조회 (Delivery Service)
Delivery 서비스의 MySQL 데이터베이스에 데이터가 올바르게 저장되었는지 확인합니다.

MySQL 컨테이너 내부로 진입:

Bash

cd mysql
docker-compose exec -it master-server bash
MySQL 클라이언트 접속 및 데이터 조회:

Bash

mysql --user=root --password=1234
use my-database;
select * from Delivery_table;
9. Kafka 토픽 파티션 변경 및 확인
Kafka 토픽의 파티션 수를 늘려 멀티 파티션 환경을 테스트합니다.

파티션 수 변경:
kafka.scaling 토픽의 파티션 수를 2개로 변경합니다.

Bash

./kafka-topics --bootstrap-server 127.0.0.1:9092 --alter --topic kafka.scaling --partitions 2
토픽 정보 확인:
변경된 토픽의 파티션 정보를 확인합니다.

Bash

./kafka-topics --bootstrap-server 127.0.0.1:9092 --topic kafka.scaling --describe
10. 두 번째 Delivery 서비스 실행
멀티 컨슈머 환경을 구성하기 위해 delivery-2nd 서비스를 실행합니다.

Bash

cd delivery-2nd
mvn clean spring-boot:run
11. 메시징 순서 테스트 (비-확정적 처리)
메시지 키를 사용하지 않을 경우, 이벤트 처리 순서가 보장되지 않을 수 있음을 확인합니다.

주문 생성:

Bash

http :8081/orders customerId=2000 productId=100 productName=RADIO qty=3 address=PUSAN
주문 수정 ( {order-id}는 실제 주문 ID로 대체):

Bash

http PATCH :8081/orders/{order-id} address=SEOUL
# 예시:
http PATCH :8081/orders/5 address=SEOUL
주문 삭제 ( {order-id}는 실제 주문 ID로 대체):

Bash

http DELETE :8081/orders/{order-id}
# 예시:
http DELETE :8081/orders/5
12. Kafka 컨슈머 재시작
새로운 이벤트들을 모니터링하기 위해 컨슈머를 다시 시작합니다.

Bash

./kafka-console-consumer --bootstrap-server localhost:9092 --topic kafka.scaling --from-beginning
13. Order 서비스 코드 수정 및 재실행 (메시지 키 적용)
Order.java와 AbstractEvent.java 코드를 수정하여 메시지 키를 적용한 후 Order 서비스를 재실행합니다. 이로써 Kafka 메시지 처리 순서가 보장됩니다.

Bash

# Order.java 코드 수정 (메시지 키 적용 관련 로직 추가)
# AbstractEvent.java 수정 (필요한 경우)
mvn clean spring-boot:run
14. 재주문 수정 삭제 (메시지 키 적용 후 순서 확인)
메시지 키 적용 후, 동일한 주문에 대한 여러 이벤트가 순서대로 처리되는지 확인합니다.

Bash

http :8081/orders customerId=2000 productId=100 productName=RADIO qty=3 address=PUSAN
http PATCH :8081/orders/6 address=SEOUL # 예시: Order ID를 6으로 가정
http DELETE :8081/orders/6 # 예시: Order ID를 6으로 가정
15. Kafka 및 DB 조회 결과 확인
Kafka에 발행된 이벤트와 Delivery 서비스 DB에 동기화된 데이터를 조회하여 최종적으로 데이터 일관성을 확인합니다.

Delivery DB 조회:

SQL

select * from Delivery_table;
Kafka 컨슈머 출력 예시:

JSON

{"eventType":"OrderPlaced","timestamp":1749697512193,"id":6,"productId":"100","productName":"RADIO","qty":3,"customerId":2000,"address":"PUSAN"}
{"eventType":"DeliveryStarted","timestamp":1749697512282,"id":8,"orderId":6,"customerId":2000,"productId":"100","productName":"RADIO","qty":3,"address":"PUSAN","status":"DELIVERY STARTED"}
{"eventType":"OrderModified","timestamp":1749697533278,"id":6,"productId":"100","productName":"RADIO","qty":3,"customerId":2000,"address":"SEOUL"}
{"eventType":"OrderCancelled","timestamp":1749697549412,"id":6,"productId":"100","productName":"RADIO","qty":3,"customerId":2000,"address":"SEOUL"}
{"eventType":"DeliveryModified","timestamp":1749697553294,"id":8,"orderId":6,"customerId":2000,"productId":"100","productName":"RADIO","qty":3,"address":"SEOUL","status":"DELIVERY MODIFIED"}
{"eventType":"DeliveryCancelled","timestamp":1749697553316,"id":8,"orderId":6,"customerId":2000,"productId":"100","productName":"RADIO","qty":3,"address":"SEOUL","status":"DELIVERY CANCELLED"}
주문 ID(Order ID)를 메시지 키로 사용하면, 같은 주문과 관련된 모든 이벤트(Order Placed, Order Modified, Order Cancelled)가 Kafka 내에서 동일한 파티션에 저장됩니다. 이는 컨슈머(소비자)가 이벤트를 처리할 때 주문 발생 → 주문 수정 → 주문 취소와 같은 논리적인 순서가 뒤바뀌는 것을 방지하며, 데이터의 일관성과 정확한 비즈니스 프로세스 처리를 보장합니다 (메시지 키가 없다면 순서가 뒤바뀔 위험이 있음).


---
## Before Running Services
### Make sure there is a Kafka server running
```
cd kafka
docker-compose up
```
- Check the Kafka messages:
```
cd kafka
docker-compose exec -it kafka /bin/bash
cd /bin
./kafka-console-consumer --bootstrap-server localhost:9092 --topic
```

## Run the backend micro-services
See the README.md files inside the each microservices directory:

- order
- delivery


## Run API Gateway (Spring Gateway)
```
cd gateway
mvn spring-boot:run
```

## Test by API
- order
```
 http :8088/orders id="id" productId="productId" productName="productName" qty="qty" customerId="customerId" address="address" 
```
- delivery
```
 http :8088/deliveries id="id" orderId="orderId" customerId="customerId" productId="productId" productName="productName" qty="qty" address="address" status="status" 
```


## Run the frontend
```
cd frontend
npm i
npm run serve
```

## Test by UI
Open a browser to localhost:8088

## Required Utilities

- httpie (alternative for curl / POSTMAN) and network utils
```
sudo apt-get update
sudo apt-get install net-tools
sudo apt install iputils-ping
pip install httpie
```

- kubernetes utilities (kubectl)
```
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
sudo install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl
```

- aws cli (aws)
```
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
unzip awscliv2.zip
sudo ./aws/install
```

- eksctl 
```
curl --silent --location "https://github.com/weaveworks/eksctl/releases/latest/download/eksctl_$(uname -s)_amd64.tar.gz" | tar xz -C /tmp
sudo mv /tmp/eksctl /usr/local/bin
```

