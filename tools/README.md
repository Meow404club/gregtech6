# GT6-RAG 工具链

为「GT6 现代复兴计划」提供的本地检索与记忆基础设施。全部工具通过常驻 HTTP MCP 服务器 `gt6-brain`（`127.0.0.1:8939/mcp`）暴露给 ZCode。

## 组成

- `gt6_rag/index.py` —— 索引器：扫描 `sources.json` 中的资料源，cAST 结构感知切块后调用 Qwen3-Embedding-4B 嵌入，存入 SQLite（`tmp/index/rag.db`）。支持增量（按 mtime/size 跳过未变化文件）。
- `gt6_rag/server.py` —— MCP 服务器（Streamable HTTP 常驻守护，纯标准库协议壳）：语义检索、精确符号搜索、原文阅读、混淆映射查询、网页抓取、知识图谱（KG）、项目状态记忆。
- `gt6_rag/web.py` —— `web_fetch` 抓网页（curl_cffi 浏览器 TLS 指纹）。
- `gt6_rag/harvest.py` —— `harvest` 资料收割：把反复参考的外部资料落盘
  `tmp/harvest/<name>/`（page=HTML→Markdown，file=原样，repo=tar.gz 安全解包）。
  **只落盘不索引**——落盘即可被 `get_source`/`sym_query(sources=['harvest'])` 阅读；
  是否入 RAG 由 gt6-curator 裁决后 `refresh_index(source="harvest")` 增量索引。
  裁剪规则在 `tmp/harvest/exclude.json`（glob 数组，相对 tmp/harvest；不入库、
  即时生效）。
- `gt6_services.sh` —— 服务总线：`{start|stop|restart|status} [brain|embed|rerank|all]`。
- `config.json` —— 嵌入 API 配置（含密钥，已被 .gitignore 排除；模板见 `config.example.json`）。
- `sources.json` —— 资料源注册表（路径相对仓库根）。

## 手动用法

```bash
VENV=tools/.venv/bin/python
$VENV tools/gt6_rag/index.py all                 # 全量/增量索引所有源（任意 cwd 可跑）
$VENV tools/gt6_rag/index.py gt6 vanilla         # 只索引指定源
$VENV tools/gt6_rag/index.py --limit-files 5 all # 小规模试跑
```

## 服务总线（ZCode 会话前先拉起）

```bash
tools/services.sh start          # brain(MCP :8939) + embed(:8937) + rerank(:8938)
tools/services.sh start brain    # 只起 brain（不需要 GPU；embed/rerank 需 LLAMA_BIN）
tools/services.sh status         # 三服务总览（含 RSS；探活只认 HTTP 200）
tools/services.sh doctor         # RSS 越限自动重启（可挂 cron，见下）
```

- `embed` 复用 `embed_server.sh`（自带 HIP 环境）；`rerank` 参数内联固化（4 槽×6k，
  `-ub 4096`——rerank 输入是整段 query+doc，物理批必须 ≥ 单输入 token 数）。
- llama.cpp 的 `--ctx-size` 是总 KV 上下文（会被槽平分）。远端中转回退配置见
  `tools/config.json`。

## MCP 服务器：常驻 HTTP 守护

刻意不用 FastMCP/stdio：anyio 线程层在长驻进程里出现过工具调用卡死。现实现为
ThreadingHTTPServer + POST /mcp 同步 JSON-RPC（单条/batch）+ GET /health 探活 +
Mcp-Session-Id 会话。`.zcode/config.json` 以 `type:http` 直连。主会话与 subagent
共享同一实例，独立于 ZCode 会话生命周期。

```bash
tools/services.sh start brain
curl -s 127.0.0.1:8939/health      # {"status":"ok","tools":16,...}
```

## llama-server RSS 行为（非泄漏，有界高水位）

变长请求会使 llama.cpp 把 compute/scratch 缓冲扩到**历史最大负载**对应的规模并
长期持有（实测同规格批次连打 8 次零增长）——长期运行表现为 RSS 只升不降，
但上界 ≈ 基线(权重+KV) + 峰值请求的 scratch，不会无限增长。处置：
`services.sh doctor`（默认 embed>10GB / 其余>4GB 时自动重启，无状态服务重启
代价 ~45s），挂 cron 即可；MALLOC_TRIM 对此无效（缓冲未归还 glibc）。

## 周期性增量索引（autorefresh）

brain 守护内置调度线程：启动 30s 后首轮、之后每轮间隔 600s，对配置的源
（默认 `project`）起独立索引子进程——mtime 增量，无改动零嵌入，真有变更时
单轮秒级。开关与节奏在 `tools/config.json` 的 `"autorefresh"` 块
（`enabled/interval_s/sources/startup_delay_s/run_timeout_s`），
生命周期日志 `tmp/index/autorefresh.log`，索引输出与手动 refresh 共用
`tmp/index/refresh.log`。

## SessionStart 上下文自动注入

`tools/context_inject.py`：新会话自动注入压缩状态页（阶段/在途任务/近决策/
已知 Bug/main 最新）。已注册在**用户级** `~/.zcode/cli/config.json`
（SessionStart 事件，timeout 5s）；脚本 git toplevel 自探测作用域——只在
brain 框架仓库内输出，其他目录静默。工程卫生：恒 exit 0、无副作用、
SQLite 只读、≤40 行。

2026-09-14 实测踩坑三课（注册写错一处 = 整个 hooks 段加载失败，当日全部
hook 静默失效）：① `matcher` 要匹配全部就**省略字段**，空串 `""` 违反
schema（minLength 1）连累整份 config 拒载；② `type: "process"` 是无 shell
的参数向量，命令必须拆 `command` + `args`，不能写带空格的整串；③ hook
stdout 走严格 JSON 校验，注入必须输出 `{"additionalContext": ...}`——
纯文本被丢弃不进上下文。`progress` 账本的 `phase/current/next` 三键是注入
页数据源，阶段推进时必须同步（铁律#7 真数据）。

## 关于 PreToolUse 钩子的注册位置

GPG 提交拦截钩子已注册在**用户级** `~/.zcode/cli/config.json`（hooks 段），
不再走 workspace 作用域 —— 用户级免信任审核，无弹卡。
`guard-commit.sh` 自动探测作用域：仅当 cwd 所在仓库的主仓库根存在
`.githooks/commit-msg`（选择加入标记）时生效，范围 = 主仓库根 +
worktree 约定目录（`../MGT6GA-trees/` 实际布局与 `../<仓库名>-trees/`
通用形），无需硬编码路径，也不影响其他仓库。
若克隆到其他机器，把同样的 hooks 段复制到该机器的用户配置即可（脚本路径
按实际仓库位置调整）。

`guard-heavy-ops.sh`（test-gating-v3 防旁路，2026-09-29）同机制：拦截未走
`tools/gt6testgate.py` 的 gradle 调用并指路 `run` 子命令；opt-in 标记 =
主仓库根存在 `tools/gt6testgate.py`；`GITHUB_ACTIONS` 置位零开销透传。
**合入 main 后**在用户级 `~/.zcode/cli/config.json` 的 `hooks.events.
PreToolUse` 数组追加一段（与 guard-commit.sh 并列；脚本不存在就先别挂——
handler 缺失等于没装）：

```json
{"matcher": "Bash",
 "hooks": [{"type": "process",
            "command": "/home/brokestar/workspace/MGT6GA/gregtech6/.githooks/guard-heavy-ops.sh",
            "timeoutMs": 10000}]}
```

## gt6testgate 门禁（test-gating-v3）

一切重操作（gradle 测试/编译/runData、sweep、RCON 链）的统一门禁+runner：
`python3 tools/gt6testgate.py run -- <原命令>`。v3（2026-09-29 WSL 崩溃裁定）
在 v2 角色硬闸（全量仅 review、/tmp/gt6_testgate_full.lock 互斥）之上加
**启动前预测**：

- 任务分类 `--class {full-test,filtered-test,compile,rundata,rcon-boot,other}`，
  缺省按命令形态自动推断（gradlew+裸 test=full、--tests=filtered、runData、
  compile*/classes/jar=compile、rcon 字样=rcon-boot、其余=other）。
- 峰值台账 `/tmp/gt6_testgate_memory_ledger.json`：门禁放行的任务在运行期
  采样进程树峰值 RSS（/proc ppid 链 + statm，2s 周期），退出后按 class 折叠
  `estimate=max(历史衰减, 本次)`（14 天半衰；flock+原子替换写，损坏 JSON
  自动重建）——采样闭环不依赖任务自觉回报。无记录 class 用冷启动保守默认
  （full-test 12G/filtered 6G/compile 4G/rundata 6G/rcon 5G/other 8G MiB，
  常量 `COLD_ESTIMATE_MIB` 可由台账覆盖）。
- admit 条件升级为 `(MemTotal-MemAvailable) + estimate(class) ≤ 30G`
  （env `GT6_GATE_MEM_LIMIT_MIB`），超限排队轮询重估；排队/拒绝均输出含
  数值的人类可读理由。
- `--dry-run`：打印 当前占用/估算/预测/决策 四行不启动——subagent 派发前
  自查与主会话调度参考。
- 向后兼容：并发槽默认 4（`GT6_GATE_MAX_CONCURRENT`）、退出码=子进程透传、
  full+coder 拒 exit 2（排队前即拒）、gt6server 直调的 `wait_memory` 签名
  不变（estimate 缺省 0）；旧 flag 形态（无 `run` 前缀）与 run 子命令同一
  实现。`GITHUB_ACTIONS` 置位 = 零门槛透传（CI 不是本 WSL 宿主）。
- **共享 slice 硬顶（v3c，2026-09-29 第三轮裁定，25G 修订）**：run 把子命令包进
  `systemd-run --user --scope -p Slice=gt6gate.slice -- <原命令>`，并在每次
  run 前幂等地 `systemctl --user set-property gt6gate.slice
  MemoryMax=25G MemorySwapMax=4G --runtime`（`--cap`/`--swap` 可调，默认
  25/4 GiB——25G 留 5G 头寸给非门禁进程）。聚合天然有界：到顶内核只在
  slice 组内 OOM-kill 越界 gradle，不伤 WSL 宿主。systemctl/systemd-run
  不可用或失败 → 一行 stderr 警告回退直接 exec（可用性优先）；
  `GT6_GATE_SLICE=0` 显式关。附带收益：开 UseContainerSupport 的 JVM 会读
  cgroup 上限自整默认堆——本仓 gradle 显式堆配置不受影响，fork 出的无配置
  JVM 受益。`--scope` 前台运行，stdio 与退出码透传语义不变。

单测：`python3 tools/gt6testgate_test.py`（stdlib unittest，56 项，全离线
零 gradle；hook 判定表/CI 透传/slice 包装与降级含在内；
`GT6_GATE_SLICE_LIVE=1` 追加跑真 systemd 探针）。

## MCP 工具一览（服务器名 gt6-brain）

| 工具 | 用途 |
|---|---|
| `search_code(query, sources?, limit?, path_glob?)` | 语义+词法混合检索（向量+BM25+RRF+精排）。**不确定确切类名/方法名、按概念或行为意图查代码时用它**（如"多方块校验怎么做的"）；已知确切符号名用 sym_query 更快 |
| `get_source(file, start?, end?)` | 按相对路径读取原始文件（带行号） |
| `sym_query(pattern, sources?, glob?)` | ripgrep 正则精确搜索：**已知确切类名/方法名/字符串时的快速定位** |
| `web_fetch(url, timeout?, max_chars?, raw?)` | 抓网页（curl_cffi 浏览器 TLS 指纹）：HTML 自动转纯文本，raw=true 返回原始 HTML。能过 TLS 指纹层反爬（实测 zillow 等 urllib 403 页）；需执行 JS 的挑战页（如 g2.com）过不了，需真浏览器方案 |
| `harvest(url, name, kind?, raw?, timeout?)` | 资料收割到 tmp/harvest/<name>/（page 网页转 Markdown / file 原样 / repo tar.gz 安全解包）。只落盘不索引，落盘即可 get_source/sym_query 阅读；入 RAG 由 gt6-curator 裁决 |
| `mappings_lookup(term)` | 1.20.1 混淆名 ↔ Mojang 官方名互查 |
| `refresh_index(source?)` | 后台重建/增量更新索引 |
| `kg_add / kg_query / kg_del` | 知识图谱：记录/检索实体关系 |
| `state_read / state_update` | 项目状态长期记忆（决策/TODO/已知 Bug/进度） |
| `project_status()` | 索引规模、KG 条数、worktree 列表、最近提交 |
