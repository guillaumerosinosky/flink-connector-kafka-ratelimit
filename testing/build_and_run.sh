#!/usr/bin/env bash
set -eux
# https://flink.apache.org/2021/09/07/connector-table-sql-api-part1.html / https://github.com/Airblader/blog-imap
(cd .. && mvn clean install) || exit 1
(cp ../target/*.jar images/client/jar) || exit 1
wget https://repo.maven.apache.org/maven2/org/apache/flink/flink-sql-connector-kafka_2.12/1.14.2/flink-sql-connector-kafka_2.12-1.14.2.jar -O ./images/client/jar/flink-sql-connector-kafka_2.12-1.14.2.jar
#docker-compose up -d --build || exit 1
docker-compose stop ; docker-compose rm -f
docker-compose up -d --build || exit 1
exec ./client.sh
