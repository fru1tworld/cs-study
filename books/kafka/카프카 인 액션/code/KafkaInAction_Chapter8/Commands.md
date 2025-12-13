# Commands used in Chapter 8

## Flume configuration for watching a directory

````
ag.sources = logdir  
ag.sinks = kafkasink
ag.channels = c1

#Configure the source directory to watch
ag.sources.logdir.type = spooldir 
ag.sources.logdir.spoolDir = /var/log/kafkainactionlogs
ag.sources.logdir.fileHeader = true

# Describe the Kafka sink 
ag.sinks.kafkasink.channel = c1  
ag.sinks.kafkasink.type = org.apache.flume.sink.kafka.KafkaSink
ag.sinks.kafkasink.kafka.topic = kinaction_flumetopic
ag.sinks.kafkasink.kafka.bootstrap.servers = localhost:9092,localhost:9093,localhost:9094
ag.sinks.kafkasink.kafka.flumeBatchSize = 10
ag.sinks.kafkasink.kafka.producer.acks = 1
ag.sinks.kafkasink.kafka.producer.linger.ms = 5
ag.sinks.kafkasink.kafka.producer.compression.type = snappy

# Memory channel configuration 
ag.channels.c1.type = memory   
ag.channels.c1.capacity = 1000
ag.channels.c1.transactionCapacity = 100

# Bind both the sink and source to the same channel
ag.sources.logdir.channels = c1  ag.sinks.kafkasink.channel = c1
````


## Flume Kafka Channel configuration
````
ag.channels.channel1.type = org.apache.flume.channel.kafka.KafkaChannel 
ag.channels.channel1.kafka.bootstrap.servers = localhost:9092,localhost:9093,localhost:9094 
ag.channels.channel1.kafka.topic = kinaction_channel1_ch
ag.channels.channel1.kafka.consumer.group.id = kinaction_flume

````


## Reference
* https://flume.apache.org/releases/content/1.9.0/FlumeUserGuide.html#kafka-sink
