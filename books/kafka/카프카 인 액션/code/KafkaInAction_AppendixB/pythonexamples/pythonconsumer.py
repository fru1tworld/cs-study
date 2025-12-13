from confluent_kafka import Consumer, KafkaError #(1)

consumer = Consumer({
    'bootstrap.servers': 'localhost:9094', #(2)
    'group.id': 'kinaction_team0group',
    'auto.offset.reset': 'earliest'
})

consumer.subscribe(['kinaction-python-topic']) #(3)

try:
    while True:
        message = consumer.poll(2.5) #(4)

        if message is None:
            continue
        if message.error():
            print('kinaction_error: %s' % message.error())
            continue
        else:
            print('kinaction_info: %s for topic: %s\n'  %
                (message.value().decode('utf-8'), message.topic()))

except KeyboardInterrupt:
    print('kinaction_info: stopping\n')
finally:
    consumer.close() #(5)

