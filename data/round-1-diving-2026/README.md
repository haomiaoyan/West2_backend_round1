# 第一轮固定数据包

`data.json` 是第一轮 Java 命令行项目使用的离线数据快照，来源为 2026 World Aquatics Diving World Cup Super Final（北京，赛事 ID `5019`），UTF-8 编码、LF 换行。学习者应将它复制到自己的项目中，并只读取本地副本；不得在程序运行或测试时请求 World Aquatics 网站。

## 文件

- `data.json`：选手与八个规定项目的决赛结果；
- `manifest.json`：数据版本、抓取时间、来源 API 地址和 `data.json` 的 SHA-256。

## 格式

```text
data.json
├─schemaVersion: number
├─competition: { id, name, from, to }
├─players: [{ fullName, gender, country, countryCode }]
└─events: [{ command, eventId, gender, discipline, eventType, results }]
   └─results: [{ fullName, rank, countryCode, scores, totalPoints }]
```

`command` 是该项目对应的完整 `result ...` 命令。`scores` 和 `totalPoints` 均为字符串，保留来源中的两位小数，避免二进制浮点误差。

## 维护者更新

仅维护者可在确认 World Aquatics 的使用条款和接口仍适用后运行：

```bash
node scripts/fetch_round1_diving_data.mjs
```

该脚本只使用 Node.js 20+ 内置模块，顺序请求并在请求间隔 0.8 秒。更新后必须检查 `manifest.json`、抽样核对结果，并提交两个生成文件与脚本变更。学习者无需运行此脚本。
