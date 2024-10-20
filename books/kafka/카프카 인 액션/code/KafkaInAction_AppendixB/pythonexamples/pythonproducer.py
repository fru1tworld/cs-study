from confluent_kafka import Producer  #(1)

producer = Producer({'bootstrap.servers': 'localhost:9094'}) #(2)

def result(err, message): #(3)
    if err:
        print('kinaction_error %s\n' % err)
    else:
        print('kinaction_info: topic=%s, and kinaction_offset=%d\n' %
       (message.topic(), message.offset()))

messages = ["hello kinaction_python", "hello again"] #(4)

for msg in messages:
    producer.poll(0)
    producer.produce("kinaction-python-topic", value=msg.encode('utf-8'), callback=result) #(5)

producer.flush() #(6)
