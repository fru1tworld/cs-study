# Source Code for Kafka in Action

## Notes

Here are some notes regarding the source code:

1. Select shell commands will be presented in a [Markdown](https://daringfireball.net/projects/markdown/syntax) format in a file called Commands.md or in [AsciiDoc](https://docs.asciidoctor.org/asciidoc/latest/) called Commands.adoc for each Chapter folder if there are any commands selected in that chapter.

### Requirements

This project was built with the following versions:

1. Java 11
1. Apache Maven 3.5.0
1. Kafka Client libraries 2.7.1

### IDE setup
 
1. We have used Eclipse for our IDE. 
   To set up for Eclipse run `mvn eclipse:eclipse` from the base directory of this repo. 
   Or, you can Import->Existing Maven Projects.

### Installing Kafka

Run the following in a directory (without spaces in the path) once you get the artifact downloaded. Refer to Appendix A if needed.

    tar -xzf kafka_2.13-2.7.1.tgz
    cd kafka_2.13-2.7.1

### Running Kafka

1. To start Kafka go to `<install dir>/kafka_2.13-2.7.1/`
2. Run `bin/zookeeper-server-start.sh config/zookeeper.properties`
3. Modify the Kafka server configs


	cp config/server.properties config/server0.properties
	cp config/server.properties config/server1.properties
	cp config/server.properties config/server2.properties

	# vi config/server0.properties
	
	````
	broker.id=0
	listeners=PLAINTEXT://localhost:9092
	log.dirs=/tmp/kafkainaction/kafka-logs-0
	````
	
	# vi config/server1.properties
	
	````
	broker.id=1
	listeners=PLAINTEXT://localhost:9093
	log.dirs=/tmp/kafkainaction/kafka-logs-1
	````
	
	# vi config/server2.properties
	
	````
	broker.id=2
	listeners=PLAINTEXT://localhost:9094
	log.dirs=/tmp/kafkainaction/kafka-logs-2
	````
	
4. Start the Kafka Brokers:
    
 ````   
    bin/kafka-server-start.sh config/server0.properties
    bin/kafka-server-start.sh config/server1.properties
    bin/kafka-server-start.sh config/server2.properties
 ````
 
### Stopping Kafka

1. To stop Kafka go to <install dir>/kafka_2.13-2.7.1/
2. Run `bin/kafka-server-stop.sh`
3. Run `bin/zookeeper-server-stop.sh`

### Code by Chapter

Most of the code from the book can be found in the project corresponding to the chapter. Some code has been moved to other chapters in order to reduce the number of replication of related classes.
	
### Running the examples
 
Most of the example programs can be run from within an IDE or from the command line. 
Make sure that your ZooKeeper and Kafka Brokers are up and running before you can run any of the examples.

The examples will usually write out to topics and print to the console.

### Shell Scripts

In the Chapter 2 project, we have included a couple of scripts if you want to use them under src/main/resources.

They include:
* `starteverything.sh` //This will start your ZooKeeper and Kafka Brokers (you will still have to go through the first time setup with Appendix A before using this.)
* `stopeverything.sh` // Will stop ZooKeeper and your brokers
* `portInUse.sh` // If you get a port in use error on startup, this script will kill all processes using those ports (assuming you are using the same ports as in Appendix A setup).

