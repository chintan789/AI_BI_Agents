import json
import random
import time
from datetime import datetime, timedelta

OUTPUT_FILE = "bulk_stock_data.json"
NUM_RECORDS = 1000  # change to 5000+ if needed

# Example stock symbols
STOCKS = ["AAPL", "GOOGL", "MSFT", "AMZN", "TSLA", "NVDA", "META", "NFLX", "BABA", "INTC"]

# Base price ranges per stock (for more realistic data)
BASE_PRICES = {
    "AAPL": 150, "GOOGL": 140, "MSFT": 300, "AMZN": 130, "TSLA": 250,
    "NVDA": 400, "META": 200, "NFLX": 350, "BABA": 100, "INTC": 50
}

# Random seed for reproducibility
random.seed(42)

def random_price(symbol):
    base = BASE_PRICES.get(symbol, 100)
    # random walk: +/- 5%
    change = base * random.uniform(-0.05, 0.05)
    return round(base + change, 2)

def random_volume():
    return random.randint(1000, 100000)

def random_timestamp(start_time, idx):
    # spread records over 1 hour
    return int((start_time + timedelta(seconds=idx*2)).timestamp())

def generate_records(num_records):
    start_time = datetime.now() - timedelta(hours=1)
    records = []
    for i in range(num_records):
        symbol = random.choice(STOCKS)
        record = {
            "symbol": symbol,
            "price": random_price(symbol),
            "volume": random_volume(),
            "timestamp": random_timestamp(start_time, i)
        }
        records.append(record)
    return records

if __name__ == "__main__":
    print(f"Generating {NUM_RECORDS} stock records...")
    stock_data = generate_records(NUM_RECORDS)

    with open(OUTPUT_FILE, "w") as f:
        json.dump(stock_data, f, indent=2)

    print(f"✅ Generated bulk stock data in {OUTPUT_FILE}")
