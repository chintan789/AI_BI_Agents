import json
from time import sleep
from confluent_kafka import Producer

KAFKA_BROKER = "pinot-kafka:9092"
TOPIC = "ingestion-events"
DATA_FILE = "bulk_stock_data.json"

producer_config = {
    "bootstrap.servers": KAFKA_BROKER
}

producer = Producer(producer_config)

def delivery_report(err, msg):
    if err:
        print(f"Delivery failed: {err}")
    else:
        print(f"Produced to {msg.topic()}: partition [{msg.partition()}] at offset {msg.offset()}")

if __name__ == "__main__":
    print(f"Starting Bulk Kafka Producer → topic: {TOPIC}")

    try:
        with open(DATA_FILE, "r") as file:
            records = json.load(file)
        count = 100
        for record in records:
            producer.produce(
                TOPIC,
                value=json.dumps(record),
                callback=delivery_report
            )
            producer.poll(0)
            sleep(0.5)  # Throttle messages slightly

        producer.flush()
        print("✅ Finished sending bulk JSON data.")

    except Exception as e:
        print(f"Error: {e}")
