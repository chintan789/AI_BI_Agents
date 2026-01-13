
deploy images
docker-compose -f docker_compose_kafka.yml up -d

list topics
 docker exec -it kafka_docker-kafka-1  kafka-topics --list --bootstrap-server localhost:9092
  docker exec -it pinot-kafka  kafka-topics --list --bootstrap-server localhost:9092
docker exec -it pinot-kafka  kafka-topics --list --bootstrap-server localhost:9092
 create topics
 docker exec -it kafka_docker-kafka-1 kafka-topics --create --topic ingestion-events  --bootstrap-server localhost:9092  --partitions 1  --replication-factor 1
docker exec -it pinot-kafka kafka-topics --create --topic transcript-topic  --bootstrap-server localhost:9092  --partitions 1  --replication-factor 1
docker exec -it pinot-kafka kafka-topics --create --topic chalja-topic  --bootstrap-server localhost:9092  --partitions 1  --replication-factor 1


live containers
docker ps



pinot kpi schema
docker exec -it 32d5347ead5fe6172fe375dbdfe2b7388f629216f5ed45ca65707ae0cf04619c /opt/pinot/bin/pinot-admin.sh AddSchema \
  -schemaFile kpi_stream_schema.json \
  -exec

kpi table
docker exec -it 32d5347ead5fe6172fe375dbdfe2b7388f629216f5ed45ca65707ae0cf04619c /opt/pinot/bin/pinot-admin.sh AddTable \
  -tableConfigFile kpi_stream_table.json \
  -exec

docker cp /Users/chintanshah/Documents/AI_BI_project/pinot/test_schema.json pinot-controller:/opt/pinot/
docker cp /Users/chintanshah/Documents/AI_BI_project/pinot/test_table.json pinot-controller:/opt/pinot/

docker exec -it pinot-controller /opt/pinot/bin/pinot-admin.sh AddSchema   -schemaFile /opt/pinot/kpi_stream_schema.json   -controllerHost pinot-controller   -controllerPort 9000   -exec
docker exec -it pinot-controller /opt/pinot/bin/pinot-admin.sh AddTable -tableConfigFile /opt/pinot/kpi_stream_table.json -schemaFile /opt/pinot/kpi_stream_schema.json -controllerHost pinot-controller  -controllerPort 9000 -exec

docker exec -it pinot-controller \
/opt/pinot/bin/pinot-admin.sh AddSchema \
  -schemaFile /opt/pinot/kpi_stream_schema.json \
  -controllerHost localhost \
  -controllerPort 9000 \
  -exec


 docker exec -it pinot-controller  /opt/pinot/bin/pinot-admin.sh AddTable -tableConfigFile /opt/pinot/transcript-table-realtime.json -schemaFile /opt/pinot/transcript-schema.json -controllerHost pinot-controller  -controllerPort 9000 -exec




docker exec -it pinot-controller /opt/pinot/bin/pinot-admin.sh AddSchema   -schemaFile /opt/pinot/test_schema.json   -controllerHost pinot-controller   -controllerPort 9000   -exec
docker exec -it pinot-controller  /opt/pinot/bin/pinot-admin.sh AddTable -tableConfigFile /opt/pinot/test_table.json -schemaFile /opt/pinot/test_schema.json -controllerHost pinot-controller  -controllerPort 9000 -exec



docker exec -it pinot-controller sh -c "echo > /dev/tcp/pinot-kafka/9092 && echo '✅ reachable' || echo '❌ unreachable'"



docker exec -it pinot-kafka kafka-console-producer \
  --bootstrap-server localhost:9092 \
  --topic ingestion-events


  docker exec -it pinot-kafka kafka-console-producer \
    --bootstrap-server localhost:9092 \
    --topic chalja