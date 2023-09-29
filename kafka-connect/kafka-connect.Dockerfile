FROM confluentinc/cp-kafka-connect:7.3.0

RUN confluent-hub install --no-prompt debezium/debezium-connector-postgresql:latest
RUN confluent-hub install --no-prompt confluentinc/connect-transforms:latest

RUN mkdir /usr/share/java/custom-filter
COPY ./kafka-connect-filter-unchanged/target/*.jar /usr/share/java/custom-filter/
