# Kafka Connect Experiment

## Prerequisites

### DB Table Replica Identity

In order to have full `before` information in the
message, the DB table must be in the `FULL`
replication mode.

Check table mode with
```sql
SELECT relname, relreplident
FROM pg_class
WHERE relname = 'film';
```

Set full with
```sql
ALTER TABLE film REPLICA IDENTITY FULL;
```


## 1. Build Filter Unchanged Plugin

Run the following command to get JAR file with Kafka Connect custom filter:
```shell
cd kafka-connect-filter-unchanged
mvn clean install
```

## 2. Run Services

Run it with `docker compose` command:
```shell
docker compose up
```

The following will be started:
1. Postgres with [Pagila] sample db.
2. Kafka with Zookeeper
3. [Kafka UI], open http://localhost:8888/
4. [Debezium] for Kafka connect
5. [Debezium UI], open http://localhost:8889/

## 3. Create Kafka Connect Connector

The connector needs to be created manually. Run "Create postgres connector"
request in [kafka-connect-rest.http](./kafka-connect-rest.http)
with [REST Client]. It creates connector for the `film` table, other
tables are not monitored.

After that, change data in the `film` DB table with Postgres client like
[pgAdmin] and check `dbpagila.public.film` topic with Kafka UI at http://localhost:8888/.

## 4. Change "film" Table in DB

Change `film` table and check `dbpagila.public.film` topic messages.


[Pagila]: https://github.com/devrimgunduz/pagila
[Kafka UI]: https://github.com/provectus/kafka-ui
[Debezium]: https://debezium.io/
[Debezium UI]: https://debezium.io/documentation/reference/stable/operations/debezium-ui.html
[REST Client]: https://marketplace.visualstudio.com/items?itemName=humao.rest-client
[pgAdmin]: https://www.pgadmin.org/
