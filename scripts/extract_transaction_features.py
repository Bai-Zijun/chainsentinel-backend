from pathlib import Path
import math
import pandas as pd

PROJECT_ROOT = Path(__file__).resolve().parents[1]

PROCESSED_DIR = PROJECT_ROOT / "data" / "processed"
TRANSACTIONS_FILE = PROCESSED_DIR / "cleaned_transactions_202605.csv"

OUTPUTS_DIR = Path(r"D:\Codetest\test\crawldata\bitcoin_outputs_data")

FEATURE_FILE = PROCESSED_DIR / "transaction_features_20260501_20260505.csv"

DATES = [
    "20260501",
    "20260502",
    "20260503",
    "20260504",
    "20260505",
]

DUST_THRESHOLD_SATS = 546


def calc_entropy(values):
    total = sum(values)
    if total <= 0:
        return 0.0

    entropy = 0.0
    for value in values:
        if value <= 0:
            continue
        p = value / total
        entropy -= p * math.log2(p)

    return entropy


def is_round_amount_sats(value):
    if value <= 0:
        return False

    # 规则金额：0.001 BTC、0.01 BTC、0.1 BTC、1 BTC 等整数倍
    round_units = [
        100_000,       # 0.001 BTC
        1_000_000,     # 0.01 BTC
        10_000_000,    # 0.1 BTC
        100_000_000,   # 1 BTC
    ]

    return any(value % unit == 0 for unit in round_units)


def extract_output_features(outputs_df):
    grouped = outputs_df.groupby("transaction_hash")["value"].apply(list)

    rows = []

    for tx_hash, values in grouped.items():
        values = [int(v) for v in values if pd.notna(v)]
        output_count = len(values)

        if output_count == 0:
            continue

        amount_entropy = calc_entropy(values)

        round_count = sum(1 for v in values if is_round_amount_sats(v))
        dust_count = sum(1 for v in values if 0 < v <= DUST_THRESHOLD_SATS)

        rows.append({
            "tx_hash": tx_hash,
            "amount_entropy": amount_entropy,
            "round_amount_ratio": round_count / output_count,
            "dust_output_ratio": dust_count / output_count,
        })

    return pd.DataFrame(rows)


def main():
    transactions = pd.read_csv(TRANSACTIONS_FILE)

    transactions = transactions[[
        "tx_hash",
        "input_count",
        "output_count"
    ]].copy()

    transactions["input_count"] = pd.to_numeric(
        transactions["input_count"],
        errors="coerce"
    )

    transactions["output_count"] = pd.to_numeric(
        transactions["output_count"],
        errors="coerce"
    )

    transactions["input_output_ratio"] = transactions.apply(
        lambda row: row["input_count"] / row["output_count"]
        if row["output_count"] and row["output_count"] > 0
        else 0,
        axis=1
    )

    output_frames = []

    for date in DATES:
        file = OUTPUTS_DIR / f"blockchair_bitcoin_outputs_{date}.tsv.gz"
        print(f"reading {file.name}")

        outputs = pd.read_csv(
            file,
            sep="\t",
            compression="gzip",
            usecols=["transaction_hash", "value"]
        )

        outputs["value"] = pd.to_numeric(outputs["value"], errors="coerce")
        outputs = outputs.dropna(subset=["transaction_hash", "value"])

        output_frames.append(outputs)

    all_outputs = pd.concat(output_frames, ignore_index=True)

    output_features = extract_output_features(all_outputs)

    features = transactions.merge(
        output_features,
        on="tx_hash",
        how="left"
    )

    features["amount_entropy"] = features["amount_entropy"].fillna(0)
    features["round_amount_ratio"] = features["round_amount_ratio"].fillna(0)
    features["dust_output_ratio"] = features["dust_output_ratio"].fillna(0)

    features["feature_version"] = "v1"

    features = features[[
        "tx_hash",
        "input_output_ratio",
        "amount_entropy",
        "round_amount_ratio",
        "dust_output_ratio",
        "feature_version"
    ]]

    features.to_csv(
        FEATURE_FILE,
        index=False,
        encoding="utf-8",
        float_format="%.8f"
    )

    print(f"transactions: {len(transactions)}")
    print(f"features: {len(features)}")
    print(f"saved: {FEATURE_FILE}")


if __name__ == "__main__":
    main()