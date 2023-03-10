# Testing

This `docker-compose` setup starts containers for Kafka, Kafka UI and a Flink cluster. It then configures Flink's SQL client to connect against that cluster.

## Prerequisites

Make sure you have the following tools installed:

* `docker`
* `docker-compose`

## Usage

Running the following script will build the project, copy the JAR to the correct location, build and
run the Docker images and start the SQL client:

```
./build_and_run.sh
```

You can also access a web interface for some services while the Docker containers are running:

1. Kafka UI: http://localhost:8080/
2. Flink UI: http://localhost:8081/

Make sure to run `docker-compose down` to shut down all containers when you're done.

## Querying Kafka

You can launch a query on Kafka using the Flink SQL client (`sql` container), launched by the [build_and_run.sh](build_and_run.sh) in interactive mode. This script injects beforehand data in Kafka from the file [test.json](./test.json). 
Once the scripts are finished please enter the following commands:

```
ADD JAR 'lib/flink-sql-connector-kafka-ratelimit_2.12-1.14.2.jar';
CREATE TABLE test (
    cid STRING, 
    locked BOOLEAN, 
    latitude DOUBLE, 
    gas_percent DOUBLE, 
    total_km DOUBLE, 
    in_use BOOLEAN, 
    longitude DOUBLE, 
    speed_kmh DOUBLE,
	`time` VARCHAR,
    `record_time` TIMESTAMP_LTZ(3) METADATA FROM 'timestamp',
    WATERMARK FOR `record_time` AS `record_time` - INTERVAL '1' SECOND
) WITH (
    'connector' = 'kafka-ratelimit',
    'topic' = 'test;control',
    'scan.startup.mode' = 'earliest-offset',
    'properties.bootstrap.servers' = 'kafka:9092',
    'properties.max.partition.fetch.bytes' = '100000',
    'value.format' = 'json',
    'rate.limit' = '100'
);
SELECT COUNT(*) FROM test; 
```
The result (count of events) should increase on average by the given rate limit (here 5).
In order to change the rate limit, please send a message to the `control` topic, in this form:
```json
{
    "rate": 100,
    "timestamp": 1676974129000
```
The `timestamp` correspond to the time of application of the new rate limit. This field is facultative.
Caution: it can take time for the consumer to "switch" to the control topic. It should be possible to tune this by changing the buffer size (depending also on the messages size). By default it seems the switch happens every 1 Mb (Will document this part later).