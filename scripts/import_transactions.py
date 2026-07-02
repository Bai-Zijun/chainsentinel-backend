from pathlib import Path
import os
import pandas as pd
import pymysql

PROJECT_ROOT = Path(__file__).resolve().parents[1]
CSV_FILE = PROJECT_ROOT / "data" / "processed" / "cleaned_transactions_202605.csv"

conn = pymysql.connect(
    host=os.getenv("DB_HOST", "localhost"),
    port=int(os.getenv("DB_PORT", "3306")),
    user=os.getenv("DB_USERNAME", "root"),
    password=os.getenv("DB_PASSWORD", ""),
    database=os.getenv("DB_NAME", "chainsentinel"),
    charset="utf8mb4"
)

sql = """
INSERT INTO transactions (
    tx_hash, block_id, tx_time, size, weight,
    is_coinbase, has_witness, input_count, output_count,
    input_total, output_total, fee, fee_rate
) VALUES (
    %s, %s, %s, %s, %s,
    %s, %s, %s, %s,
    %s, %s, %s, %s
)
ON DUPLICATE KEY UPDATE
    block_id = VALUES(block_id),
    tx_time = VALUES(tx_time),
    size = VALUES(size),
    weight = VALUES(weight),
    is_coinbase = VALUES(is_coinbase),
    has_witness = VALUES(has_witness),
    input_count = VALUES(input_count),
    output_count = VALUES(output_count),
    input_total = VALUES(input_total),
    output_total = VALUES(output_total),
    fee = VALUES(fee),
    fee_rate = VALUES(fee_rate)
"""

df = pd.read_csv(CSV_FILE)

rows = df[[
    "tx_hash", "block_id", "tx_time", "size", "weight",
    "is_coinbase", "has_witness", "input_count", "output_count",
    "input_total", "output_total", "fee", "fee_rate"
]].values.tolist()

batch_size = 5000

with conn:
    with conn.cursor() as cursor:
        for start in range(0, len(rows), batch_size):
            batch = rows[start:start + batch_size]
            cursor.executemany(sql, batch)
            conn.commit()
            print(f"imported {min(start + batch_size, len(rows))}/{len(rows)}")

print("done")
