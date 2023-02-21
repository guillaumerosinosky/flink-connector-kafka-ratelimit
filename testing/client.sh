#!/usr/bin/env bash
docker cp test.json testing_kafka_1:/test.json
docker cp control.json testing_kafka_1:/control.json
docker exec -it testing_kafka_1 bash -c "kafka-topics.sh --bootstrap-server kafka:9092 --create --topic control"
docker exec -it testing_kafka_1 bash -c "kafka-console-producer.sh --broker-list kafka:9092 --topic control --property value.serializer=custom.class.serialization.JsonSerializer < control.json"
docker exec -it testing_kafka_1 bash -c "kafka-console-producer.sh --broker-list kafka:9092 --topic test --property value.serializer=custom.class.serialization.JsonSerializer < test.json"
exec docker-compose exec sql ./sql-client.sh

