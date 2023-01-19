#!/usr/bin/env bash
docker cp test.json testing_kafka_1:/test.json
docker exec -it testing_kafka_1 bash -c "kafka-console-producer.sh --broker-list kafka:9092 --topic test --property value.serializer=custom.class.serialization.JsonSerializer < test.json"
exec docker-compose exec sql ./sql-client.sh

