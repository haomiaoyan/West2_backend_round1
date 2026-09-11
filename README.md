# DWASearch

2026 World Aquatics Diving World Cup Super Final（北京）赛事数据命令行查询程序。
west2-online「learn-backend」第一轮考核项目：[考核说明](https://github.com/west2-online/learn-backend/blob/main/docs/1-基础语法-java.md)。

## 功能

- `players`：输出全部 89 名参赛选手续（按国籍升序、同国籍按姓氏升序）；
- `result <项目>`：输出指定项目决赛结果（名次、各轮分数、总分）；
- 未知命令输出 `Error`，未知/非法项目输出 `N/A`；
- 输入输出均为文件：`java -jar DWASearch.jar input.txt output.txt`。

## 环境与运行

| 项 | 值 |
|---|---|
| JDK | OpenJDK 25.0.2 |
| 构建 | Maven 3.9.11 + JUnit 5（jupiter 5.14.4，surefire 3.2.5） |
| 已验证运行环境 | Windows 11 |

```bash
# 打包（会先自动执行全部测试）
mvn package                      # 产物：target/DWASearch.jar（finalName 配置见 pom.xml）
# 运行
java -jar target/DWASearch.jar input.txt output.txt
# 全部自动化测试
mvn test                         # 16 个测试，预期全绿
```

## 数据

固定数据包位于 [`data/round-1-diving-2026/`](data/round-1-diving-2026/)，随仓库分发，程序只读本地文件，**不访问网络、不修改原始数据**。

- 数据版本：schemaVersion `1`，抓取时间 `2026-08-29T06:47:21Z`（见 `manifest.json`）；
- 完整性校验：`data.json` 的 SHA-256 为
  `b648a02f56c71c458e29fcc30d5cf17dcc9c62f89b8cc9d012fd0fad70d1984c`，与 `manifest.json` 记录一致；
  可用 `certutil -hashfile data/round-1-diving-2026/data.json SHA256`（Windows）或 `sha256sum`（Linux/macOS）复核。

## 设计与实现

### 模块结构

```
DWASearch.java     命令行入口：参数检查 → 读文件 → 逐行调 CoreModule → 写出结果。不含业务逻辑。
CoreModule.java    核心门面：构造时注入 data.json 全文；加载+排序+预格式化选手块；
                   handleLine(String)→String：一行命令进、该命令的完整输出文本出。纯计算，无 IO、无打印。
DataLoader.java    数据访问：loadPlayers（89 名选手）、findEvent/loadResults（按命令定位项目并取成绩）。
PlayerFormatter    选手信息块格式化。
ResultFormatter    成绩块格式化（分数累加用 BigDecimal）。
```

数据流：`args → input.txt 逐行 → CoreModule.handleLine → StringBuilder 拼装 → Files.writeString(output.txt, UTF-8)`。

### 关键设计取舍

1. **姓名以「姓 名」解析，排序次要关键字取空格前子串。**
   依据：① players 数据本身即按该规则有序（Australia 组 COLE→KEENEY→KOLOI→…），与官方输出样例首两名吻合；② 跨文化统一（`CHEN Jia`、`KNIGHT Kyndal`，姓氏全大写）；③ `fullName` 全文 137 处写法一致。
   替代方案：按空格后（名）排序——与数据顺序及官方样例均矛盾，排除。

2. **命令匹配使用逐字符 `equals`，不对输入做空格归一化。**
   依据：官方非法输入样例中 `result men 10m     synchronised`（多重空格）的正确答案是 `N/A`。
   替代方案：`split` 后 `join` 压缩空格——会把上述行误判为合法，与官方样例直接冲突，排除。

3. **输出契约：每个块自带结尾换行（内容 + `\n-----\n`），块与块之间不加任何空行。**

4. **`CoreModule` 构造注入数据并缓存选手块。**
   `data.json` 读一次、排序/格式化做一次，`handleLine` 为纯函数。收益：多条 `players` 命令零重复计算；评审者可不启动命令行、不碰文件，直接 `new CoreModule(数据).handleLine("players")` 做断言（满足"核心逻辑可独立测试"的工程要求）。
   替代方案：静态方法逐次传数据字符串（无法承载缓存、调用繁琐）；分发逻辑留在 main（核心逻辑与命令行耦合）。均排除。

5. **总分用 `BigDecimal` 现场累加，等号右侧不使用数据自带的 `totalPoints`。**
   理由：`double` 存在二进制表示误差（如 `0.1+0.2 = 0.30000000000000004`），分数全程字符串构造 `BigDecimal`、`setScale(2, HALF_UP)`，零污染；输出中等号右侧恒为程序自己的累加和，**不抄**数据自带值。该累加逻辑在**开发阶段**与数据 `totalPoints` 做过交叉验证（8 个项目全部一致；方法：格式化时临时比对 `sum.compareTo(new BigDecimal(totalPoints))` 并打印，验证通过后移除）。交付版本不在运行时重复校验固定数据——数据随仓库分发、经 SHA-256 核对、规定不得修改，运行时再验价值有限。
   替代方案：直接抄数据总分——计算环节未被验证，且数据若错则输出盲从，排除。

6. **所有文件读写显式指定 `StandardCharsets.UTF_8`。**
   实测教训：开发环境若被注入 `-Dfile.encoding=GBK`（如部分 IDE 运行配置），不指定编码的 `FileWriter` 会落盘 GBK 字节（`共`→`B9 B2`），UTF-8 读回时抛 `MalformedInputException`。虽然 `Files.writeString` 默认即 UTF-8，仍显式书写，把编码契约钉死在代码里。

7. **手写 JSON 解析（`indexOf` + `substring`），未引入 Gson/Jackson。**
   本轮聚焦语言基础；数据格式已知且固定，按"保持简单"原则手解，配两条纪律：**每个 `indexOf` 结果判 `-1`**；**跨段搜索先锁段**（players 段以 `"events"` 为界——实测 `gender` 全文出现 97 次、`countryCode` 137 次，不锁段必串扰）。

## 异常处理说明

| 场景 | 行为 |
|---|---|
| 命令行参数不足 2 个 | `System.err` 输出一行用法提示，退出码 1，不打印堆栈 |
| input/数据文件不存在或不可读 | `System.err` 输出一行错误说明（含原因），退出码 1；**错误信息绝不写入 output.txt**（错误走 stderr，结果走结果文件，双通道分离） |
| 未知命令（含大小写不符、命令粘连） | 输出块 `Error\n-----\n` |
| `result` 后项目名不合法/带多余字符/多余空格 | 输出块 `N/A\n-----\n` |
| output 路径不可写/目录不存在 | 与读失败同一 catch：一行 stderr + 退出码 1（实测：`java -jar target\DWASearch.jar input.txt asdasd\out.txt` → 输出 `Error: asdasd\out.txt`，无堆栈） |

设计原则：先定义每种输入的可观察行为（契约），再实现；异常不被吞掉，也不原样抛给用户。

## 单元测试展示

`mvn test`：**16 个测试全绿**（JUnit 5）。

| 测试类 | 覆盖 |
|---|---|
| `CoreModuleTest`（5） | 选手块开头逐字符、89 条分隔线计数、`result sss`→N/A 精确匹配、`player`→Error（大小写敏感）、官方样例成绩行 `Score:73.50 + 74.40 + 76.50 + 72.00 + 78.00 = 374.40` 逐字符包含 |
| `PlayerTest`（2） | 双关键字排序逐位断言、格式化输出精确匹配+分隔线计数 |
| `BigDecimalTest`（3） | 分数累加=374.40、单元素、空列表契约（返回 `0.00`） |
| `WarmupTest` / `EnvCheckTest` | 学习阶段的语法与环境验证练习，作为学习轨迹保留 |

另有人工回归：5 命令混合 input（players/合法 result/非法命令/未知项目/重复命令）输出 392 行，与手算一致，作为回归样本保留。
测试断言的期望值一律来自官方样例、数据实测或手工计算，不来自被测代码的输出（防"抄代码自证"）。

## 目录结构

```
├─ src/main/java/        # DWASearch / CoreModule / DataLoader / 两个 Formatter（Maven 标准布局）
├─ src/test/java/        # JUnit 5 测试
├─ data/round-1-diving-2026/   # 固定数据包（只读，原始文件未修改）
├─ input.txt             # 输入样例（多命令回归样本）
├─ README.md / .gitignore / pom.xml
```
