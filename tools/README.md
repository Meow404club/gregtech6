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

## 关于 PreToolUse 钩子的注册位置

GPG 提交拦截钩子已注册在**用户级** `~/.zcode/cli/config.json`（hooks 段），
不再走 workspace 作用域 —— 用户级免信任审核，无弹卡。
`guard-commit.sh` 自动探测作用域：仅当 cwd 所在仓库的主仓库根存在
`.githooks/commit-msg`（选择加入标记）时生效，范围 = 主仓库根 +
`../<仓库名>-trees/` worktree 约定目录，无需硬编码路径，也不影响其他仓库。
若克隆到其他机器，把同样的 hooks 段复制到该机器的用户配置即可（脚本路径
按实际仓库位置调整）。

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
