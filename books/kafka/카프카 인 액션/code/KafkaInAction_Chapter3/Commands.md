# Source Code for Kafka in Action

## Chapter 3 Specific Notes

* The commands and scripts are meant to be run from your Kafka root directory: i.e. you should see the folder `bin`, `config`, etc when you do an `ls` in this directory.

* If you get a port in use exception: kill the process related to that port. 
For example, on a Mac, you can do the following command: `lsof -t -i tcp:8083 | xargs kill` if the `connect-standalone` process is still running, etc.

## sqlite3 database example

* Close all zookeeper and kafka brokers you have open. 
We are going to start fresh.


### Download for Confluent Open Source
* Note that the book text uses Confluent version 5.X and might not reflect the below instrucutions that have been updated to be newer versions*

* https://www.confluent.io/previous-versions
  Select the «Download Community Tarball» version 6.1.1 or above and unzip that archive.
  Add location of unzipped Confluent Platform to environment variable `$CONFLUENT_HOME`.
  Add `$CONFLUENT_HOME/bin` to PATH.
  
* install Confluent CLI https://docs.confluent.io/current/cli/installing.html
* install JDBC connector using `confluent-hub` command 
    
      confluent-hub install confluentinc/kafka-connect-jdbc:10.2.0
  
  and follow the prompts to install the connector

### Table create command in the root folder you just downloaded:

* `sqlite3 kafkatest.db`

```sqlite
 CREATE TABLE invoices( 
   id INT PRIMARY KEY     NOT NULL,
   title           TEXT    NOT NULL,
   details        CHAR(50),
   billedamt         REAL,
   modified TIMESTAMP DEFAULT (STRFTIME('%s', 'now')) NOT NULL
);

``` 

### Table insert command

```sqlite
INSERT INTO invoices (id,title,details,billedamt)  VALUES (1, 'book', 'Franz Kafka', 500.00 );
```

### Edit source jdbc connector file to the following:
`> export CONFLUENT_HOME=~/confluent-6.2.0`


`> mkdir -p $CONFLUENT_HOME/etc/kafka-connect-jdbc/`

`> vi $CONFLUENT_HOME/etc/kafka-connect-jdbc/kafkatest-sqlite.properties`


### File Contents
*NOTE: Update your database path below to be the full path for best results: ie. *
connection.url=jdbc:sqlite:/<YOUR_FULL_PATH>/confluent-6.2.0/kafkatest.db
 
```properties
name=kinaction-test-source-sqlite-jdbc-invoice
connector.class=io.confluent.connect.jdbc.JdbcSourceConnector
tasks.max=1
# SQLite database stored in the file kafkatest.db, use and auto-incrementing column called 'id' to
# detect new rows as they are added, and output to topics prefixed with 'kinaction-test-sqlite-jdbc-', e.g.
# a table called 'invoices' will be written to the topic 'kinaction-test-sqlite-jdbc-invoices'.
connection.url=jdbc:sqlite:kafkatest.db
mode=incrementing
incrementing.column.name=id
topic.prefix=kinaction-test-sqlite-jdbc-
```

### Start Confluent Kafka

Run the following:

```bash
> confluent local services connect start 
> confluent local services connect connector config jdbc-source --config $CONFLUENT_HOME/etc/kafka-connect-jdbc/kafkatest-sqlite.properties
> confluent local services connect connector status
> ./bin/kafka-avro-console-consumer --topic kinaction-test-sqlite-jdbc-invoices --bootstrap-server localhost:9092  --from-beginning
```

## Avro Notes

* You need to be running a schema registry to make this work.
Make sure you start the `confluent local services connect start` before you try, and run the example.

* You should see the producer message by running:

```bash
confluent local services kafka consume kinaction_schematest --value-format avro --from-beginning
```

## Java example commands

NOTE:
 * Use JDK 11
 * Apache Kafka and Confluent Schema Registry should be up and running

```bash
> ./mvnw verify # build a uber jar
> java -cp target/chapter3-jar-with-dependencies.jar org.kafkainaction.consumer.HelloWorldConsumer # run a consumer application
> java -cp target/chapter3-jar-with-dependencies.jar org.kafkainaction.producer.HelloWorldProducer # run a producer

``` 
