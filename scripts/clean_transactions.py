from pathlib import Path
import pandas as pd

PROJECT_ROOT = Path(__file__).resolve().parents[1]

RAW_DIR = Path(r"D:\Codetest\test\crawldata\bitcoin_transactions_data\1-15\1-5")
OUTPUT_DIR = PROJECT_ROOT / "data" / "processed"
OUTPUT_DIR.mkdir(parents=True, exist_ok=True)

OUTPUT_FILE = OUTPUT_DIR / "cleaned_transactions_202605.csv"

files = sorted(RAW_DIR.glob("blockchair_bitcoin_transactions_202605*.tsv.gz"))
# files = [RAW_DIR / "blockchair_bitcoin_transactions_20260501.tsv.gz"]

if not files:
    raise FileNotFoundError(f"No transaction files found in {RAW_DIR}")

frames = []

for file in files:
    print(f"reading {file.name}")

    df = pd.read_csv(
        file,
        sep="\t",
        compression="gzip"
    )

    df = df[[
        "hash",
        "block_id",
        "time",
        "size",
        "weight",
        "is_coinbase",
        "has_witness",
        "input_count",
        "output_count",
        "input_total",
        "output_total",
        "fee",
        "fee_per_kb"
    ]].copy()

    df.rename(columns={
        "hash": "tx_hash",
        "time": "tx_time",
        "fee_per_kb": "fee_rate"
    }, inplace=True)

    df["tx_hash"] = df["tx_hash"].astype(str)
    df["tx_time"] = pd.to_datetime(df["tx_time"], errors="coerce")

    numeric_columns = [
        "block_id",
        "size",
        "weight",
        "is_coinbase",
        "has_witness",
        "input_count",
        "output_count",
        "input_total",
        "output_total",
        "fee",
        "fee_rate"
    ]

    for col in numeric_columns:
        df[col] = pd.to_numeric(df[col], errors="coerce")

    # Blockchair 的 input_total/output_total/fee 通常是 satoshi，转成 BTC
    for col in ["input_total", "output_total", "fee"]:
        df[col] = df[col] / 100_000_000

    # fee_per_kb 是 sat/kB，转成 sat/vB，后端字段叫 fee_rate
    df["fee_rate"] = df["fee_rate"] / 1000

    df = df.dropna(subset=["tx_hash", "tx_time"])
    df = df.drop_duplicates(subset=["tx_hash"])

    frames.append(df)

result = pd.concat(frames, ignore_index=True)
result = result.drop_duplicates(subset=["tx_hash"])

result.to_csv(OUTPUT_FILE, index=False, encoding="utf-8", float_format="%.8f")

print(f"files: {len(files)}")
print(f"rows: {len(result)}")
print(f"saved: {OUTPUT_FILE}")