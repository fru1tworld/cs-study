# Commands used in Chapter 9

## Creating the selfserviceTopic Topic

     bin/kafka-topics.sh --create --bootstrap-server localhost:9094 --topic kinaction_selfserviceTopic --partitions 2 --replication-factor 2 

You should see the ouput: `Created topic "kinaction_selfserviceTopic".`

## kcat Producer usage

    kcat -P -b localhost:9094 -t kinaction_selfserviceTopic
 
## kcat Consumer usage

    kcat -C -b localhost:9094 -t kinaction_selfserviceTopic

## REST Proxy startup

    bin/kafka-rest-start.sh etc/kafka-rest/kafka-rest.properties 

## Curl call to REST Proxy for Topic list

    curl -X GET -H "Accept: application/vnd.kafka.v2+json" localhost:8082/topics 
 
##  Zookeeper Unit File

    [Unit]
    Requires=network.target remote-fs.target
    After=network.target remote-fs.target
    
    [Service]
    Type=simple
    ExecStart=/opt/kafkainaction/bin/zookeeper-server-start.sh /opt/kafkainaction/config/zookeeper.properties 
    ExecStop=/opt/kafkainaction/bin/zookeeper-server-stop.sh 
    Restart=on-abnormal 
    
    [Install]
    WantedBy=multi-user.target

## Kafka Unit File

    [Unit]
    Requires=zookeeper.service
    After=zookeeper.service 
    
    [Service]
    Type=simple
    ExecStart=/opt/kafkainaction/bin/kafka-server-start.sh \
    /opt/kafkainaction/bin/config/server.properties > /opt/kafkainaction/broker.log 2>&1' 
    ExecStop=/opt/kafkainaction/bin/kafka-server-stop.sh 
    Restart=on-abnormal 
    
    [Install]
    WantedBy=multi-user.target

## Kafka Startup with Systemctl

    sudo systemctl start zookeeper
    sudo systemctl start kafka

## Kafka server log retention

    log4j.appender.kafkaAppender.MaxFileSize=500KB 
    log4j.appender.kafkaAppender.MaxBackupIndex=10

## Which Kafka Appender
*Note: Make sure that you check for any recent log4j updates

    log4j.appender.kafkaAppender=org.apache.kafka.log4jappender.KafkaLog4jAppender


    <dependency>
        <groupId>org.apache.kafka</groupId>
        <artifactId>kafka-log4j-appender</artifactId>
        <version>LATEST</version>
    </dependency>



