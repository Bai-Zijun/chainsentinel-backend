from pathlib import Path

import numpy as np
import pandas as pd
from sklearn.ensemble import IsolationForest
from sklearn.preprocessing import StandardScaler

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PROCESSED_DIR = PROJECT_ROOT / "data" / "processed"

TRANSACTIONS_FILE = PROCESSED_DIR / "cleaned_transactions_202605.csv"
FEATURES_FILE = PROCESSED_DIR / "transaction_features_20260501_20260505.csv"
OUTPUT_FILE = PROCESSED_DIR / "anomaly_results_20260501_20260505.csv"

MODEL_NAME = "IsolationForest"
MODEL_VERSION = "iforest_v1"


FEATURE_COLUMNS = [
    "input_count",
    "output_count",
    "fee_rate",
    "fee",
    "size",
    "weight",
    "input_output_ratio",
    "amount_entropy",
    "round_amount_ratio",
    "dust_output_ratio",
]


def build_reason(row):
    reasons = []

    if row["fee_rate"] >= row["fee_rate_p99"]:
        reasons.append("手续费率高于99分位")

    if row["output_count"] >= row["output_count_p99"]:
        reasons.append("输出数量高于99分位")

    if row["amount_entropy"] >= row["amount_entropy_p99"]:
        reasons.append("输出金额分布熵较高")

    if row["dust_output_ratio"] >= 0.5:
        reasons.append("小额输出占比较高")

    if row["round_amount_ratio"] >= 0.5:
        reasons.append("规则金额输出占比较高")

    if row["input_output_ratio"] >= row["input_output_ratio_p99"]:
        reasons.append("输入输出数量比例异常")

    if not reasons:
        reasons.append("综合特征偏离正常交易分布")

    return "；".join(reasons)


def assign_risk_level(score):
    if score >= 0.85:
        return "HIGH"
    if score >= 0.65:
        return "MEDIUM"
    return "LOW"


def main():
    transactions = pd.read_csv(TRANSACTIONS_FILE)
    features = pd.read_csv(FEATURES_FILE)

    transactions = transactions[[
        "tx_hash",
        "input_count",
        "output_count",
        "fee_rate",
        "fee",
        "size",
        "weight"
    ]].copy()

    features = features[[
        "tx_hash",
        "input_output_ratio",
        "amount_entropy",
        "round_amount_ratio",
        "dust_output_ratio"
    ]].copy()

    data = transactions.merge(features, on="tx_hash", how="inner")

    for col in FEATURE_COLUMNS:
        data[col] = pd.to_numeric(data[col], errors="coerce")

    data = data.dropna(subset=FEATURE_COLUMNS)

    # 降低极端值对训练的影响，保留异常排序能力
    for col in FEATURE_COLUMNS:
        lower = data[col].quantile(0.001)
        upper = data[col].quantile(0.999)
        data[col] = data[col].clip(lower, upper)

    x = data[FEATURE_COLUMNS].values

    scaler = StandardScaler()
    x_scaled = scaler.fit_transform(x)

    model = IsolationForest(
        n_estimators=200,
        contamination=0.03,
        random_state=42,
        n_jobs=-1
    )

    model.fit(x_scaled)

    # decision_function 越小越异常，转成 0-1 异常分数
    raw_score = -model.decision_function(x_scaled)
    anomaly_score = (raw_score - raw_score.min()) / (raw_score.max() - raw_score.min())

    data["anomaly_score"] = anomaly_score
    data["risk_level"] = data["anomaly_score"].apply(assign_risk_level)

    # 用分位数规则生成可解释原因
    data["fee_rate_p99"] = data["fee_rate"].quantile(0.99)
    data["output_count_p99"] = data["output_count"].quantile(0.99)
    data["amount_entropy_p99"] = data["amount_entropy"].quantile(0.99)
    data["input_output_ratio_p99"] = data["input_output_ratio"].quantile(0.99)

    data["reason"] = data.apply(build_reason, axis=1)

    data["model_name"] = MODEL_NAME
    data["model_version"] = MODEL_VERSION

    result = data[[
        "tx_hash",
        "anomaly_score",
        "risk_level",
        "model_name",
        "model_version",
        "reason"
    ]].copy()

    result.to_csv(
        OUTPUT_FILE,
        index=False,
        encoding="utf-8",
        float_format="%.8f"
    )

    print(f"input rows: {len(data)}")
    print(f"saved rows: {len(result)}")
    print(result["risk_level"].value_counts())
    print(f"saved: {OUTPUT_FILE}")


if __name__ == "__main__":
    main()