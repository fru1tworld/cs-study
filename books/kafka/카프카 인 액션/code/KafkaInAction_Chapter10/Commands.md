# Commands used in Chapter 10

# Note
* A security professional should be consulted for the correct way to
set up your own environment. Our commands are meant as a guide for getting
familiar and for learning, not as a production level of security. This is not
a complete guide. Use it at your own risk.
The EXAMPLE ONLY label notes that these commands are not likely to work for your setup due to the need for the domains in our example as well as keys needing created, etc.

## EXAMPLE ONLY - SSL Key Generation for a Broker

    keytool -genkey -noprompt \
        -alias localhost \
        -dname "CN=ka.manning.com, OU=TEST, O=TREASURE, L=Bend, S=Or, C=US" \
        -keystore kafka.broker0.keystore.jks \ 
        -keyalg RSA \
        -storepass changeTreasure \ 
        -keypass changeTreasure \
        -validity 999

## EXAMPLE ONLY - Creating our own Certificate Authority
      openssl req -new -x509 -keyout cakey.crt -out ca.crt \
      -days 999 -subj '/CN=localhost/OU=TEST/O=TREASURE/L=Bend/S=Or/C=US' \
      -passin pass:changeTreasure -passout pass:changeTreasure


## EXAMPLE ONLY - Broker server properties changes

    listeners=PLAINTEXT://localhost:9092,SSL://localhost:9093 
    ssl.truststore.location=/var/kafkainaction/private/kafka.broker0.truststore.jks 
    ssl.truststore.password=changeTreasure
    ssl.keystore.location=/var/kafkainaction/private/kafka.broker0.keystore.jks 
    ssl.keystore.password=changeTreasure
    ssl.key.password=changeTreasure

## EXAMPLE ONLY - SSL Configuration for Clients

    security.protocol=SSL 
    ssl.truststore.location=/var/kafkainaction/ssl/client.truststore.jks 
    ssl.truststore.password=changeTreasure

## EXAMPLE ONLY - Using SSL Configuration for Command line Clients

    bin/kafka-console-producer.sh --bootstrap-server localhost:9093 --topic kinaction_test_ssl \
     --producer.config kinaction-ssl.properties
    bin/kafka-console-consumer.sh --bootstrap-server localhost:9093 --topic kinaction_test_ssl \
     --consumer.config kinaction-ssl.properties
 
## EXAMPLE ONLY - Broker SASL JAAS File

    KafkaServer {
        com.sun.security.auth.module.Krb5LoginModule required
        useKeyTab=true
        storeKey=true
        keyTab=”/opt/kafkainaction/kafka_server0.keytab”
        principal=”kafka/kafka0.ka.manning.com@MANNING.COM”;
    };

 
## EXAMPLE ONLY - Broker SASL properties changes

    listeners=PLAINTEXT://localhost:9092,SSL://localhost:9093,SASL_SSL://localhost:9094 
    sasl.kerberos.service.name=kafka


## ACL Authorizer and Super Users

    authorizer.class.name=kafka.security.auth.SimpleAclAuthorizer 
    super.users=User:Franz

## Kafka ACLs To Read and Write to a Topic

    bin/kafka-acls.sh --authorizer-properties \
      --bootstrap-server localhost:9094 --add \
     --allow-principal User:Franz --allow-principal User:Hemingway \ 
     --operation Read --operation Write --topic kinaction_clueful_secrets
 
## ACL ZooKeeper

    zookeeper.set.acl=true

  
## Creating a Network Bandwidth Quota for Client Clueful

    bin/kafka-configs.sh  --bootstrap-server localhost:9094 --alter \
    --add-config 'producer_byte_rate=1048576,consumer_byte_rate=5242880' \
    --entity-type clients --entity-name kinaction_clueful

## Listing and Deleting a Quota for Client Clueful

    bin/kafka-configs.sh  --bootstrap-server localhost:9094 \
    ---describe \ 
    --entity-type clients --entity-name kinaction_clueful

    bin/kafka-configs.sh  --bootstrap-server localhost:9094 --alter \
    --delete-config 'producer_byte_rate,consumer_byte_rate' \ 
    --entity-type clients --entity-name kinaction_clueful

## Creating a Network Bandwidth Quota for Client Clueful

    bin/kafka-configs.sh  --bootstrap-server localhost:9094 --alter \
    --add-config 'request_percentage=100' \
    --entity-type clients --entity-name kinaction_clueful 
    
    
 # Reference
    * https://docs.confluent.io/platform/current/security/security_tutorial.html
