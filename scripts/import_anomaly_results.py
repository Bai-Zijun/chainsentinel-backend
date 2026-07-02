from pathlib import Path
import os
import pandas as pd
import pymysql

PROJECT_ROOT = Path(__file__).resolve().parents[1]
CSV_FILE = PROJECT_ROOT / "data" / "processed" / "anomaly_results_20260501_20260505.csv"

conn = pymysql.connect(
    host=os.getenv("DB_HOST", "localhost"),
    port=int(os.getenv("DB_PORT", "3306")),
    user=os.getenv("DB_USERNAME", "root"),
    password=os.getenv("DB_PASSWORD", ""),
    database=os.getenv("DB_NAME", "chainsentinel"),
    charset="utf8mb4"
)

sql = """
INSERT INTO anomaly_results (
    tx_hash,
    anomaly_score,
    risk_level,
    model_name,
    model_version,
    reason
) VALUES (
    %s, %s, %s, %s, %s, %s
)
ON DUPLICATE KEY UPDATE
    anomaly_score = VALUES(anomaly_score),
    risk_level = VALUES(risk_level),
    model_name = VALUES(model_name),
    model_version = VALUES(model_version),
    reason = VALUES(reason)
"""

df = pd.read_csv(CSV_FILE)

rows = df[[
    "tx_hash",
    "anomaly_score",
    "risk_level",
    "model_name",
    "model_version",
    "reason"
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
