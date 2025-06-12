# Model
## kafkamessage-20250612(주문 ID(Order ID)를 메시지 키로 사용하여, 같은 주문과 관련된 모든 이벤트 동일 파티션에 관리)
https://labs.msaez.io/#/189596125/storming/kafka-scaling2

![스크린샷 2025-06-12 100611](https://github.com/user-attachments/assets/22f61098-7610-4c05-bf71-fac7a6a2eab7)
![스크린샷 2025-06-12 120720](https://github.com/user-attachments/assets/be23f4fa-5d2f-4133-af39-315c25eeddc6)
![스크린샷 2025-06-12 120829](https://github.com/user-attachments/assets/ed14288c-8ceb-44c6-afcd-f011c206bf3c)

---
프로젝트 실행 가이드 및 터미널 명령어  
이 문서는 프로젝트 실행 및 테스트에 필요한 명령어를 순서대로 정리한 가이드입니다.  
필요한 부분을 복사하여 터미널에 바로 사용할 수 있습니다.  

0. 재실행 시 초기화 (선택 사항)
Java 버전 확인
java -version

초기화 스크립트 실행
./init.sh

Kafka 디렉토리로 이동 후 Docker Compose 시작 (9092 포트가 실행되지 않는 경우에만)
cd kafka
docker-compose up

1. Java SDK 설치  
sdk install java  

2. Lombok 버전 수정  
order, delivery, delivery-2nd 서비스의 Lombok 버전을 1.18.30으로 수정합니다.  

3. MySQL 및 서비스 실행  
MySQL 실행  
cd mysql  
docker-compose up -d  

Order 서비스 실행  
cd ../order  
mvn clean spring-boot:run  

Delivery 서비스 실행  
cd ../delivery  
mvn clean spring-boot:run  

4. Kafka 토픽 모니터링 시작  
Kafka 컨테이너 내부로 진입  
cd ../kafka  
docker-compose exec kafka /bin/bash  

bin 디렉토리로 이동 후 컨슈머 실행  
cd /bin  
./kafka-console-consumer --bootstrap-server localhost:9092 --topic kafka.scaling --from-beginning  

5. 주문 생성 (Order Service)  
http :8081/orders customerId=1000 productId=100 productName=TV qty=3 address=SEOUL  
http :8081/orders customerId=2000 productId=100 productName=RADIO qty=3 address=PUSAN  

6. 주문 수정 (Order Service)  
http PATCH :8081/orders/2 address=SEOUL  

7. 주문 삭제 (Order Service)  
http DELETE :8081/orders/2  

8. DB 조회 (Delivery Service)  
MySQL 컨테이너 내부로 진입  
cd ../mysql  
docker-compose exec -it master-server bash  

MySQL 클라이언트 접속  
mysql --user=root --password=1234  

DB 선택 및 데이터 조회  
use my-database;  
select * from Delivery_table;  

9. Kafka 토픽 파티션 변경 및 확인  
파티션 수 변경  
./kafka-topics --bootstrap-server 127.0.0.1:9092 --alter --topic kafka.scaling --partitions 2  

토픽 정보 확인  
./kafka-topics --bootstrap-server 127.0.0.1:9092 --topic kafka.scaling --describe  

10. 두 번째 Delivery 서비스 실행  
cd ../delivery-2nd  
mvn clean spring-boot:run  

11. 메시징 순서 테스트 (비-확정적 처리)  
주문 생성  
http :8081/orders customerId=2000 productId=100 productName=RADIO qty=3 address=PUSAN  

주문 수정 (예시)  
http PATCH :8081/orders/5 address=SEOUL  

주문 삭제 (예시)  
http DELETE :8081/orders/5  

12. Kafka 컨슈머 재시작  
./kafka-console-consumer --bootstrap-server localhost:9092 --topic kafka.scaling --from-beginning  

13. Order 서비스 코드 수정 및 재실행  
Order.java 및 AbstractEvent.java에 메시지 키 적용  
이후 Order 서비스 재실행  
mvn clean spring-boot:run  

14. 메시지 키 적용 후 재주문 수정 삭제  
http :8081/orders customerId=2000 productId=100 productName=RADIO qty=3 address=PUSAN  
http PATCH :8081/orders/6 address=SEOUL  
http DELETE :8081/orders/6  

15. Kafka 및 DB 조회 결과 확인  
-- Delivery DB에서 확인  
select * from Delivery_table;  

Kafka 컨슈머 출력 예시:  
{"eventType":"OrderPlaced","timestamp":1749697512193,"id":6,"productId":"100","productName":"RADIO","qty":3,"customerId":2000,"address":"PUSAN"}  
{"eventType":"DeliveryStarted","timestamp":1749697512282,"id":8,"orderId":6,"customerId":2000,"productId":"100","productName":"RADIO","qty":3,"address":"PUSAN","status":"DELIVERY STARTED"}  
{"eventType":"OrderModified","timestamp":1749697533278,"id":6,"productId":"100","productName":"RADIO","qty":3,"customerId":2000,"address":"SEOUL"}  
{"eventType":"OrderCancelled","timestamp":1749697549412,"id":6,"productId":"100","productName":"RADIO","qty":3,"customerId":2000,"address":"SEOUL"}  
{"eventType":"DeliveryModified","timestamp":1749697553294,"id":8,"orderId":6,"customerId":2000,"productId":"100","productName":"RADIO","qty":3,"address":"SEOUL","status":"DELIVERY MODIFIED"}  
{"eventType":"DeliveryCancelled","timestamp":1749697553316,"id":8,"orderId":6,"customerId":2000,"productId":"100","productName":"RADIO","qty":3,"address":"SEOUL","status":"DELIVERY CANCELLED"}  
주문 ID(Order ID)를 메시지 키로 사용하면, 해당 주문 관련 모든 이벤트가 Kafka 내 동일 파티션에 저장되어 순서 보장이 가능해집니다. 이는 Placed → Modified → Cancelled 순으로 처리됨을 보장하며, 메시지 키가 없을 경우 이벤트 순서가 뒤바뀔 수 있어 주의가 필요합니다.  

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

