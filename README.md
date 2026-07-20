# ChainSentinel 2.0：Bitcoin 链上异常分析与节点数据同步平台

## 1. 项目简介

ChainSentinel 是一个以 Java 后端为核心的 Bitcoin 链上数据分析项目，目前包含两条数据链路：

1. **离线异常分析链路**：处理 Blockchair 历史交易文件，提取交易行为特征，使用 Isolation Forest 生成异常分数、风险等级和解释原因。
2. **Bitcoin Core 节点链路**：通过 JSON-RPC 连接远程 Bitcoin Core testnet4 节点，查询节点状态、区块和 mempool，并将最近区块以可恢复、可追踪的任务方式同步到 MySQL。

项目定位是“异常评分与风险提示”，不直接证明交易涉及违法、洗钱或隐蔽通信。模型结果用于发现偏离常规分布的交易并辅助后续人工分析。

当前版本：**Java 2.0 阶段性版本**。

## 2. 当前能力

- 清洗 Bitcoin 历史交易数据并批量导入 MySQL
- 提取输入输出比例、金额熵、规则金额占比、小额输出占比等行为特征
- 使用 Isolation Forest 计算交易异常分数
- 查询交易详情、特征、风险和聚合分析结果
- 分页查询高风险交易
- 封装 Bitcoin Core JSON-RPC 客户端
- 查询 testnet4 区块链、网络连接和 mempool 状态
- 按高度查询区块并限制返回的交易哈希数量
- 手动回填 Bitcoin Core 最近 1 至 100 个区块
- 记录同步 checkpoint、执行进度、成功状态和失败原因
- 防止同一网络同时执行多个同步任务
- 重复回填时更新已有区块，避免重复记录
- 提供参数校验、统一异常响应和请求追踪日志
- 使用 Flyway 管理数据库结构版本
- 提供 JUnit、MockMvc、Mockito 和 Testcontainers 测试

## 3. 技术栈

### Java 后端

- Java 17
- Spring Boot 3.5.13
- Spring MVC / Jakarta Validation
- MyBatis-Plus 3.5.16
- MySQL 8
- Flyway
- Spring Boot Actuator
- Springdoc OpenAPI / Swagger UI
- JUnit 5 / Mockito / MockMvc
- Testcontainers
- Maven

### 数据分析

- Python
- Pandas
- NumPy
- Scikit-learn
- Isolation Forest

### 外部数据与节点

- Blockchair Bitcoin TSV.GZ 离线文件
- Bitcoin Core 30.2 testnet4
- Bitcoin Core JSON-RPC
- SSH 本地端口转发

## 4. 系统架构

```text
离线数据链路
Blockchair TSV.GZ
        |
        v
Python 清洗、特征提取、Isolation Forest
        |
        v
transactions / transaction_features / anomaly_results
        |
        v
Spring Boot REST API + 静态查询页面

节点数据链路
远程 Bitcoin Core testnet4
        |
        | SSH tunnel: 127.0.0.1:18443 -> server:127.0.0.1:48332
        v
Java BitcoinCoreRpcClient
        |
        +--> 节点状态与区块查询 API
        |
        +--> 手动区块回填服务
                |
                v
bitcoin_blocks / sync_checkpoints / sync_runs
```

同步任务采用“编排服务 + 独立持久化事务”的结构：编排服务负责调用 RPC、校验区块连续性和控制执行流程；持久化服务负责锁定 checkpoint、保存单个区块、更新任务进度和记录失败状态。

## 5. 项目结构

```text
chainsentinel-backend/
├─ docs/                              # 数据库、日志、RPC 和同步说明
├─ scripts/                           # Python 清洗、特征、检测和导入脚本
├─ src/main/java/com/bzj/chainsentinel/
│  ├─ client/                         # Bitcoin Core JSON-RPC 客户端
│  ├─ common/                         # 统一 API 响应
│  ├─ config/                         # MyBatis、Bitcoin 配置
│  ├─ controller/                     # REST Controller
│  ├─ entity/                         # MyBatis 实体
│  ├─ exception/                      # 业务异常与统一异常处理
│  ├─ mapper/                         # MyBatis Mapper
│  ├─ service/                        # 业务与同步持久化服务
│  ├─ sync/                           # 区块同步内部数据模型
│  ├─ vo/                             # 接口返回模型
│  └─ web/                            # requestId 与访问日志
├─ src/main/resources/
│  ├─ db/migration/                   # Flyway V1、V2、V3
│  ├─ static/                         # 内置查询页面
│  └─ application.yml
└─ src/test/java/                     # 单元、接口和数据库集成测试
```

原始数据和处理后的大体积 CSV 文件不会提交到 Git 仓库。

## 6. 离线异常分析

### 6.1 数据范围

当前实验使用 2026-05-01 至 2026-05-05 的 Bitcoin 交易、输入和输出数据。

```text
blockchair_bitcoin_transactions_YYYYMMDD.tsv.gz
blockchair_bitcoin_inputs_YYYYMMDD.tsv.gz
blockchair_bitcoin_outputs_YYYYMMDD.tsv.gz
```

### 6.2 处理脚本

```text
scripts/clean_transactions.py
scripts/extract_transaction_features.py
scripts/detect_anomalies.py
scripts/import_transactions.py
scripts/import_transaction_features.py
scripts/import_anomaly_results.py
```

主要交易特征：

| 特征 | 含义 |
| --- | --- |
| `input_output_ratio` | 输入数量与输出数量比例 |
| `amount_entropy` | 输出金额分布熵 |
| `round_amount_ratio` | 规则金额输出占比 |
| `dust_output_ratio` | 小额输出占比 |

### 6.3 阶段性数据结果

```text
交易记录数：       2,877,972
交易特征记录数：   2,877,972
异常检测结果数：   2,877,972

LOW：             2,834,414
MEDIUM：             33,101
HIGH：               10,457
```

异常原因示例包括手续费率高于分位数阈值、小额输出占比较高、金额分布熵较高和输入输出数量比例异常。

## 7. 数据库设计与迁移

Flyway 在应用启动时自动验证并执行迁移：

| 版本 | 作用 |
| --- | --- |
| `V1` | 创建交易、特征和异常结果核心表及基础索引 |
| `V2` | 补充交易哈希唯一索引和高风险分页查询索引 |
| `V3` | 创建 Bitcoin 区块、同步 checkpoint 和同步运行记录表 |

### 7.1 离线分析表

- `transactions`：交易基础信息
- `transaction_features`：交易行为特征
- `anomaly_results`：异常分数、风险等级、模型版本和原因

三个表均以 `tx_hash` 建立唯一约束。高风险分页使用 `(risk_level, anomaly_score)` 联合索引。

### 7.2 节点同步表

- `bitcoin_blocks`：网络、区块高度、区块哈希、前一区块哈希、Merkle Root、时间、交易数、大小、重量和 canonical 状态
- `sync_checkpoints`：每个网络最后成功保存的高度、哈希和任务状态
- `sync_runs`：任务类型、起止高度、已处理数量、状态、错误信息和执行时间

`bitcoin_blocks` 使用 `(network, block_hash)` 唯一约束。重复回填同一区块时执行更新，不新增重复行；同高度旧 canonical 记录会被标记为非 canonical。

详细迁移说明见 [docs/JAVA_V2_DATABASE.md](docs/JAVA_V2_DATABASE.md)。

## 8. REST API

所有业务响应使用统一结构：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

### 8.1 交易分析接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/transactions/{txHash}` | 查询交易详情 |
| `GET` | `/api/transactions/{txHash}/features` | 查询交易特征 |
| `GET` | `/api/transactions/{txHash}/risk` | 查询异常评分和风险原因 |
| `GET` | `/api/transactions/{txHash}/analysis` | 聚合查询交易、特征和风险 |
| `GET` | `/api/transactions/risk/high?page=1&size=20` | 分页查询高风险交易 |

`txHash` 必须是 64 位十六进制字符串。分页参数会校验范围，非法参数返回 HTTP `400`。

### 8.2 Bitcoin Core 查询接口

| 方法 | 路径 | RPC 数据来源 |
| --- | --- | --- |
| `GET` | `/api/node/blockchain` | `getblockchaininfo` |
| `GET` | `/api/node/network` | `getnetworkinfo` |
| `GET` | `/api/node/mempool` | `getmempoolinfo` |
| `GET` | `/api/node/blocks/{height}?txLimit=20` | `getblockhash` + `getblock` |

应用会校验节点返回的网络是否与 `BITCOIN_NETWORK` 一致，防止误连主网或其他测试网络。

### 8.3 区块同步接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/api/sync/blocks/backfill?count=10` | 回填节点最新的 1 至 100 个区块 |
| `GET` | `/api/sync/status` | 查询 canonical 区块数、checkpoint 和最近一次任务 |

同步任务状态：

- `RUNNING`：任务执行中
- `SUCCESS`：任务全部完成
- `FAILED`：任务失败，错误原因已记录
- checkpoint 的 `IDLE`：最近一次任务已正常结束，可以启动新任务

同一网络已有 `RUNNING` 任务时，新请求返回 HTTP `409`。

### 8.4 运维接口

```text
GET /actuator/health
GET /actuator/info
```

Swagger UI：

```text
http://127.0.0.1:8080/swagger-ui/index.html
```

内置查询页面：

```text
http://127.0.0.1:8080/
```

## 9. 本地运行

### 9.1 前置条件

- JDK 17 或更高版本
- Maven 3.9+
- MySQL 8
- 可访问的 Bitcoin Core testnet4 节点
- Python 环境仅在运行离线处理脚本时需要
- Docker 仅在执行 Testcontainers 数据库集成测试时需要

### 9.2 配置数据库和 RPC

在仓库外或被 `.gitignore` 排除的 `set-env.ps1` 中保存本地变量：

```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/chainsentinel?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="your_database_password"
$env:FLYWAY_ENABLED="true"

$env:BITCOIN_RPC_URL="http://127.0.0.1:18443"
$env:BITCOIN_RPC_USER="your_rpc_user"
$env:BITCOIN_RPC_PASSWORD="your_rpc_password"
$env:BITCOIN_RPC_TIMEOUT="10"
$env:BITCOIN_NETWORK="testnet4"
```

在 PowerShell 当前终端加载脚本：

```powershell
. .\set-env.ps1
```

不要使用 `bash set-env.ps1`，PowerShell 环境变量不会因此进入当前 Git Bash 或 PowerShell 进程。不得把数据库密码、RPC 密码、cookie 或真实服务器地址提交到公开仓库。

### 9.3 建立 SSH 隧道

远程 Bitcoin Core RPC 继续绑定服务器 `127.0.0.1:48332`，本机通过 SSH 转发访问：

```bash
ssh -N -L 18443:127.0.0.1:48332 root@<remote-host>
```

保持该终端运行。确认本地监听：

```bash
netstat -ano | grep 18443
```

不应将 Bitcoin Core RPC 端口直接暴露到公网。

### 9.4 启动应用

```powershell
mvn spring-boot:run
```

应用默认监听：

```text
http://127.0.0.1:8080
```

启动期间 Flyway 会自动执行尚未应用的迁移。已经应用的迁移文件不可直接修改，后续结构变更应新增迁移版本。

## 10. 手动验证

### 10.1 节点连通性

```powershell
curl.exe "http://127.0.0.1:8080/api/node/blockchain"
curl.exe "http://127.0.0.1:8080/api/node/network"
curl.exe "http://127.0.0.1:8080/api/node/mempool"
curl.exe "http://127.0.0.1:8080/api/node/blocks/143838?txLimit=20"
```

### 10.2 区块回填与状态

```powershell
curl.exe -X POST "http://127.0.0.1:8080/api/sync/blocks/backfill?count=10"
curl.exe "http://127.0.0.1:8080/api/sync/status"
```

再次执行同一回填请求，可以验证数据库不会新增重复的区块 hash。

### 10.3 交易接口

```powershell
$txHash="ea5c23e6268e1eb09187f91e47106ca7a43e068452d3ee089b282b1d2fe12e67"
curl.exe "http://127.0.0.1:8080/api/transactions/$txHash/analysis"
curl.exe "http://127.0.0.1:8080/api/transactions/risk/high?page=1&size=10"
```

## 11. 测试与构建

执行完整测试：

```powershell
mvn test
```

生成可运行 Jar：

```powershell
mvn package
```

本轮结束时的测试记录（2026-07-20）：

```text
Tests run: 36
Failures: 0
Errors: 0
Skipped: 2
```

跳过的两个测试属于 `MySqlPersistenceIntegrationTest`。本机没有可用 Docker 环境时，Testcontainers 按配置跳过；在 Docker 可用的环境中，它们会验证 Flyway 表结构、唯一约束、真实 MySQL 查询和重复区块回填幂等性。

当前测试覆盖：

- Spring 应用上下文启动
- Bitcoin Core RPC 成功、认证、超时和错误响应
- 节点查询与区块查询接口
- 交易查询、参数校验、404 和分页接口
- 区块回填成功与链连续性冲突
- 同步任务互斥
- checkpoint、任务状态和失败记录
- requestId 生成、透传和访问日志
- MySQL Flyway 与幂等持久化集成测试

## 12. 可观测性与异常处理

每个 HTTP 响应包含 `X-Request-Id`。合法的调用方 requestId 会被透传，否则系统生成 UUID；日志 MDC 使用相同 requestId 关联一次请求。

访问日志记录方法、路径、HTTP 状态和耗时，不记录数据库密码、RPC 凭证、Authorization Header 或请求体。

主要错误映射：

| HTTP 状态 | 场景 |
| --- | --- |
| `400` | 参数格式或范围错误 |
| `404` | 交易、特征、风险或区块不存在 |
| `409` | 同步任务冲突或检测到链不连续 |
| `502` | Bitcoin Core 返回 RPC 错误或不一致数据 |
| `503` | RPC 凭证缺失或节点不可用 |
| `504` | RPC 请求超时 |
| `500` | 未处理的服务端异常 |

详细日志说明见 [docs/OBSERVABILITY.md](docs/OBSERVABILITY.md)。

## 13. 详细文档

- [Java 2.0 数据库迁移](docs/JAVA_V2_DATABASE.md)
- [可观测性与 requestId](docs/OBSERVABILITY.md)
- [Bitcoin Core testnet4 Java 接入](docs/BITCOIN_CORE_JAVA.md)
- [Java 区块同步](docs/BITCOIN_SYNC_JAVA.md)

## 14. 当前边界

- Isolation Forest 输出异常度，不等同于违法行为识别准确率。
- 当前节点同步只保存区块元数据和交易数量，不保存完整交易体。
- 区块同步由接口手动触发，尚未接入定时任务或 ZMQ 通知。
- 系统会检测区块不连续，但尚未实现完整的链重组回滚。
- 进程在任务执行中异常退出时可能留下 `RUNNING` checkpoint，尚未实现超时任务恢复。
- testnet4 数据用于节点接入与同步验证，不能替代主网数据分析结论。
- 当前部署仍依赖本机 MySQL 和 SSH 隧道，尚未完成 Docker Compose 与 Linux 一键部署。

## 15. 后续计划

优先级从高到低：

1. 增加 stale `RUNNING` 任务检测与人工恢复接口。
2. 实现基于 checkpoint 的增量同步，而不仅是最近区块回填。
3. 保存交易基础数据并接入现有特征提取与异常评分链路。
4. 实现链重组检测、canonical 切换和 checkpoint 回滚。
5. 增加定时同步或 Bitcoin Core ZMQ 新区块通知。
6. 在 Docker 可用环境持续执行 MySQL Testcontainers 测试。
7. 增加 Docker Compose、Linux 部署脚本和 GitHub Actions。
8. 扩展基础 Vue 查询与同步状态可视化页面。

Redis、Kafka 和微服务拆分会在出现明确并发、吞吐或解耦需求后再引入，避免为技术栈数量增加不必要复杂度。

## 16. 安全与合规说明

本项目仅处理公开链上数据和自建节点返回的信息。公开仓库不得包含数据库密码、Bitcoin Core RPC 凭证、认证 cookie、SSH 私钥或真实基础设施敏感信息。

异常评分和风险原因属于分析信号，不能单独作为法律判断、账户处置或执法结论。任何高风险结果都需要结合交易上下文、地址关联信息和人工复核进一步判断。
