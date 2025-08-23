# Commands used in Chapter 11

## Important Schema Registry Configuration

````
listeners=http://localhost:8081
kafkastore.connection.url=localhost:2181 
kafkastore.topic=_schemas 
debug=false
````

## Starting Schema Registry

````
bin/schema-registry-start \ 
  ./etc/schema-registry/schema-registry.properties
````  
  
## Listing Schema Registry Configuration

curl -X GET http://localhost:8081/config
  
## Checking Compatibility with the Schema Registry Maven Plugin

````
<plugin>
    <groupId>io.confluent</groupId>
    <artifactId>kafka-schema-registry-maven-plugin</artifactId> 
    <version>5.3.1</version>
    <configuration>
        <schemaRegistryUrls>
            <param>http://localhost:8081</param> 
        </schemaRegistryUrls>
        <subjects> 
            <kinaction_schematest-value>
              src/main/avro/alert_v2.avsc
            </kinaction_schematest-value>
        </subjects>
    </configuration>
    <goals>
        <goal>test-compatibility</goal> 
    </goals>
</plugin>
````

### More Commands

> curl -X GET http://localhost:8081/config
> 
> curl http://localhost:8081/subjects/kinaction_schematest-value/versions/1
> 
> curl -X POST -H "Content-Type: application/vnd.schemaregistry.v1+json" --data '{ "schema": "{ \"type\": \"record\", \"name\": \"Alert\", \"fields\": [{ \"name\": \"notafield\", \"type\": \"long\" } ]}" }' http://localhost:8081/compatibility/subjects/kinaction_schematest-value/versions/latest


### Maven Ouput

[INFO] --- kafka-schema-registry-maven-plugin:6.2.1:test-compatibility (default-cli) @ chapter11 ---

[INFO] Schema Kafka-In-Action-Source-Code/KafkaInAction_Chapter11/src/main/avro/alert_v2.avsc is compatible with subject(kinaction_schematest-value)
