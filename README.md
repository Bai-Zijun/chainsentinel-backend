# ChainSentinel: 基于 Bitcoin 链上交易行为的异常交易分析平台

## 1. 项目简介

ChainSentinel 是一个面向 Bitcoin 链上交易数据的安全分析平台。项目基于公开链上交易数据，完成交易数据清洗、交易行为特征提取、异常交易检测、风险评分和 RESTful 查询接口构建。

本项目不假设能够直接证明某笔交易一定存在违法行为、洗钱行为或隐蔽通信行为，而是从交易结构、金额分布、手续费率、输入输出模式等公开可验证特征出发，对偏离常规交易行为分布的交易进行异常评分和风险提示，用于辅助分析和进一步人工研判。

## 2. 项目目标

- 支持 Bitcoin 原始交易数据的清洗与批量导入
- 构建交易级行为特征
- 使用无监督异常检测模型识别异常交易模式
- 输出异常分数、风险等级和可解释风险原因
- 提供后端接口查询交易详情、交易特征、风险结果和聚合分析结果

## 3. 技术栈

- Java 17
- Spring Boot 3
- MyBatis-Plus
- MySQL
- Python
- Pandas
- Scikit-learn
- Swagger / Springdoc OpenAPI

## 4. 系统架构

```text
Blockchair Bitcoin TSV.GZ 数据
        ↓
Python 数据清洗脚本
        ↓
transactions 表
        ↓
Python 特征提取脚本
        ↓
transaction_features 表
        ↓
Isolation Forest 异常检测
        ↓
anomaly_results 表
        ↓
Spring Boot RESTful API
内置静态前端页面
```

## 5. 数据来源

项目使用公开 Bitcoin 链上交易数据，包括：

```text
blockchair_bitcoin_transactions_YYYYMMDD.tsv.gz
blockchair_bitcoin_outputs_YYYYMMDD.tsv.gz
blockchair_bitcoin_inputs_YYYYMMDD.tsv.gz
```

当前 MVP 使用 2026-05-01 至 2026-05-05 的交易数据进行清洗、特征提取和异常检测实验。

说明：原始数据文件和处理后的 CSV 文件体积较大，不提交到代码仓库。

## 6. 数据处理流程

### 6.1 交易数据清洗

脚本：

```text
scripts/clean_transactions.py
```

处理内容：

- 读取 `.tsv.gz` 原始交易数据
- 提取交易哈希、区块高度、交易时间、输入输出数量、手续费等字段
- 将 satoshi 单位金额转换为 BTC
- 将 `fee_per_kb` 转换为 `fee_rate`
- 去除无效记录和重复交易
- 生成标准化 CSV 文件

输出：

```text
data/processed/cleaned_transactions_202605.csv
```

### 6.2 交易特征提取

脚本：

```text
scripts/extract_transaction_features.py
```

特征包括：

| 特征 | 含义 |
| --- | --- |
| input_output_ratio | 输入数量与输出数量比例 |
| amount_entropy | 输出金额分布熵 |
| round_amount_ratio | 规则金额输出占比 |
| dust_output_ratio | 小额输出占比 |

输出：

```text
data/processed/transaction_features_20260501_20260505.csv
```

### 6.3 异常检测

脚本：

```text
scripts/detect_anomalies.py
```

方法：

- 使用 Isolation Forest 进行无监督异常检测
- 输入交易结构、手续费、金额分布等特征
- 输出 0 到 1 的异常分数
- 根据异常分数划分 LOW、MEDIUM、HIGH 风险等级
- 结合分位数规则生成可解释原因

输出：

```text
data/processed/anomaly_results_20260501_20260505.csv
```

## 7. 数据库设计

核心表：

```text
transactions
transaction_features
anomaly_results
```

### 7.1 transactions

存储 Bitcoin 交易基础信息。

核心字段：

```text
tx_hash
block_id
tx_time
size
weight
input_count
output_count
input_total
output_total
fee
fee_rate
is_coinbase
has_witness
```

### 7.2 transaction_features

存储交易行为特征。

核心字段：

```text
tx_hash
input_output_ratio
amount_entropy
round_amount_ratio
dust_output_ratio
feature_version
```

### 7.3 anomaly_results

存储异常检测结果。

核心字段：

```text
tx_hash
anomaly_score
risk_level
model_name
model_version
reason
```

建议索引：

```sql
CREATE INDEX idx_risk_level_score
ON anomaly_results (risk_level, anomaly_score DESC);
```

## 8. 核心接口

### 8.1 查询交易详情

```http
GET /api/transactions/{txHash}
```

### 8.2 查询交易特征

```http
GET /api/transactions/{txHash}/features
```

### 8.3 查询交易风险

```http
GET /api/transactions/{txHash}/risk
```

### 8.4 查询交易聚合分析结果

```http
GET /api/transactions/{txHash}/analysis
```

返回内容包括：

```text
交易基础信息
交易行为特征
异常检测结果
```

### 8.5 分页查询高风险交易

```http
GET /api/transactions/risk/high?page=1&size=20
```

## 9. 阶段性结果

当前 MVP 使用 2026-05-01 至 2026-05-05 的 Bitcoin 交易数据进行实验。

处理结果：

```text
交易记录数：2,877,972
交易特征记录数：2,877,972
异常检测结果数：2,877,972
```

风险分布：

```text
LOW: 2,834,414
MEDIUM: 33,101
HIGH: 10,457
```

示例风险原因：

```text
手续费率高于99分位
小额输出占比较高
输出金额分布熵较高
输入输出数量比例异常
综合特征偏离正常交易分布
```

## 10. 运行方式

### 10.1 启动后端

通过环境变量配置数据库连接：

```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/chainsentinel?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="your_password"

ps：
把涉及到的保存在本地脚本set-env.ps1下
每次打开项目后运行 bash set-env.ps1
```

启动项目：

```bash
mvn spring-boot:run
```

### 10.2 访问接口文档

```text
http://localhost:8080/swagger-ui/index.html
```

### 10.3 访问前端页面

后端启动后访问：

```text
http://localhost:8080/
```

前端页面支持：

```text
交易聚合分析查询
高风险交易分页列表
异常分数与风险原因展示
```

### 10.4 运行 Python 脚本

导入脚本同样读取数据库环境变量：

```powershell
$env:DB_HOST="localhost"
$env:DB_PORT="3306"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="your_password"
$env:DB_NAME="chainsentinel"
```

```bash
python scripts/clean_transactions.py
python scripts/extract_transaction_features.py
python scripts/detect_anomalies.py
```

## 11. 项目边界说明

本项目不直接判断某笔交易是否一定涉及违法行为、洗钱或隐蔽通信行为。系统输出的是基于公开链上行为特征的异常评分和风险提示，结果用于辅助分析和进一步人工研判。

## 12. 后续优化

- 增加地址行为画像
- 增加批量检测任务管理
- 引入 Redis 缓存热门地址画像
- 引入 Kafka 模拟实时交易流检测
- 优化异常原因生成逻辑
- 增加更多统计图表和地址画像视图
- 使用 Docker Compose 管理 MySQL、后端服务和脚本运行环境
