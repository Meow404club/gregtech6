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

## gt6testgate 门禁（test-gating-v3 → v3.7）

一切重操作（gradle 测试/编译/runData、sweep、RCON 链）的统一门禁+runner：
`python3 tools/gt6testgate.py run -- <原命令>`。

- **角色硬闸（v2，2026-09-27）**：全量（无 `--tests` 的 test/cleanTest）
  仅 review 席；coder 排队前即拒 exit 2；全量跑持
  `/tmp/gt6_testgate_full.lock` 全局互斥，第二个排队。
- **任务分类+峰值台账（v3，2026-09-29）**：`--class
  {full-test,filtered-test,compile,rundata,rcon-boot,other}` 按命令形态
  推断（gradlew+裸 test=full、--tests=filtered、runData、compile*/classes/
  jar=compile、rcon 字样=rcon-boot、其余=other）。台账
  `/tmp/gt6_testgate_memory_ledger.json` 按 class 折叠运行期采样峰值
  `estimate=max(历史衰减, 本次)`（14 天半衰；flock+原子替换写，损坏 JSON
  自动重建）；无记录 class 用冷默认（full 12G/filtered 6G/compile 4G/
  rundata 6G/rcon 5G/other 8G MiB，`COLD_ESTIMATE_MIB`）。estimate 是
  v3.2 信封判据与 dry-run 信息面的输入。⚠️ 台账历史可能被共享 daemon RSS
  污染（v3.1 落地前采样吃进跨任务 daemon；当日污染台账已手动重置）——
  `--no-daemon` 落地后新样本自净，旧污染按半衰衰减。
- **信封内准入（v3.2，取代 v3a 系统侧 30G 公式）**：
  - 主判据=信封内：`slice memory.current + estimate(class) ≤ cap(22G)`——
    防信封内多任务叠加引发组内 OOM 抖动；slice 空时任何任务即刻放行。
  - 系统侧外压护栏（`used − slice_current ≤ MemTotal − cap − 2G`）已随
    v3.5 退役，见下；`GT6_GATE_MEM_LIMIT_MIB` 语义随之反转（opt-in 恢复）。
  - 退役缘由：旧公式 `(系统已用+估算)≤30G` 把信封内自己的占用也计入
    系统已用=重复计算，叠加被污染的 11.6G filtered 估算，曾把审查席
    饿死阻塞一小时。
  - `--dry-run`：slice/estimate/envelope/outside/decision 五行，不启动。
- **共享 slice 硬顶（v3c）**：run 包 `systemd-run --user --scope -p
  Slice=gt6gate.slice`，每 run 幂等 `systemctl --user set-property
  gt6gate.slice MemoryMax=22G MemorySwapMax=4G --runtime`（`--cap`/`--swap`
  可调；v3.6 起 22G，见下）。systemd 缺席/失败 → 一行 stderr 警告降级直跑
  （可用性优先）；`GT6_GATE_SLICE=0` 显式关。
- **残留清剿三层（v3.1，2026-09-29 第四轮）**：
  1. `--no-daemon` 注入（根治）：检测到命令是 gradlew/gradle 即自动追加
     （幂等；已带 `--no-daemon`/显式 `--daemon` 不动；CI 透传分支不注入）。
     每构建单次 daemon 随构建退出——零残留，且杜绝并发任务跨 cgroup 共享
     同一 daemon 被「误杀」。代价仅每次构建 JVM 冷启动秒级。交互热 daemon
     逃生：`--keep-daemon` 或 env `GT6_GATE_KEEP_DAEMON=1`。
  2. 确定性 scope+收尾杀（兜底）：scope 名 `gt6gate-run-<pid>-<ts>.scope`
     （带后缀全名，systemd-run 原样使用；bare 名会被 show 自动补
     `.service` 查空）。子命令退出且台账落账后：对全组
     `kill --signal=SIGTERM` → 轮询 cgroup.procs 至多 2s → 残留
     SIGKILL → 清完才释放并发槽。用异步 `kill` 而非 `stop`：stop 在进程
     无视 TERM 时会阻塞到 systemd 自身 ~90s 超时，架空 2s 宽限。失败仅
     stderr 警告，退出码透传不变。
  3. `reap [--dry-run]`（清扫）：枚举 gt6gate.slice 下 scope（按 Slice
     属性过滤，不看名字），命中 MainPID 为 0/空（leader 已退）且
     cgroup.procs 非空者 → 同上杀序；活跃 scope 不碰。兜崩溃 runner 的
     尸场（实测案例：空 leader scope 里藏 2G+ daemon，杀掉释放 8G）。
- **每任务预算+脚本看门狗（v3.3，2026-09-29 第六轮）**：scope 建型即写
  `MemoryMax=<TASK_CAP_MIB[class]>`（full 12G/filtered 12G/compile 6G/
  rundata 8G/rcon 6G/other 8G，`--task-cap G` 覆盖）。
  **内核注记（Brokestar 6.18.50 定制内核）**：scope 级 memory.max 实测
  **被执行**（300M 探针 OOM rc=137、oom_kill 计数增长，2026-09-29）；
  slice 级 22G 未验证（破坏性探针列入 r8 收官清单，slice 空闲时跑）；
  「不强制」的原始观察来源存疑（slice 级，或 v3.3 前旧 scope 根本没写
  per-task 属性）。故执行者双层纵深：**脚本看门狗**在 2s 采样 tick 里
  ①读本 scope memory.current（同一次读数喂台账峰值，免 /proc 遍历）超
  预算 → TERM→2s→KILL 杀己组，退出码 **97（BUDGET_EXIT 专码，超限失败
  ≠普通失败；内核先杀则 -9 常规信号码透传，CI/调用方可区分）**；
  ②聚合 slice 下全部子 cgroup 的 memory.current >22G 项目帽 → 按占用
  最大者优先逐杀至帽内（单次回收最大化=最少受害者最快回帽；新任务天然
  幸免——尚未膨胀；杀粒度=单任务 cgroup，兄弟无恙；若自己最大也会被
  杀并转 97）。
- **可回收缓存不计入计算（v3.4，2026-09-29 用户修订）**：cgroup
  memory.current 把 gradle 文件 IO 的 page cache 计入（看似满、实可回收
  ——11430-vs-104 污染类根因）。信封内读数（准入 slice 读+看门狗
  own/聚合两维）一律改**有效占用 = memory.current − memory.stat 的
  file − slab_reclaimable**；解析失败保守不减、stderr 只注记一次。
  系统侧护栏本就读 `MemTotal − MemAvailable`（内核已排除可回收缓存），
  无需调整。内核 MemoryMax 语义不动（内核先回收 cache 才 OOM）。台账
  峰值维持进程 RSS 采样（statm 不含流式文件缓存）——与包装态有效占用
  是两种视图，列内各自同模可比。
- **外压护栏退役（v3.5，2026-09-29 用户裁定「cgroup 内部算好 25g 就行，
  系统的不用管了，嵌入服务固定开销是 8g」）**：v3.2 的系统侧护栏在本机
  是纯算术死锁——上限 `MemTotal(40099) − 25G − 2G ≈ 12451MiB`（时值帽
  25G）恒低于
  固定非门禁基线 ≈15069MiB（嵌入服务 8G+常驻 ZCode 会话+OS），谓词
  永假、任何任务永不放行（当日 15:09-15:35 五个 wrapper 死等实录）。
  准入只由信封判定；`outside` 降为信息项（gate-admit 行与 `--dry-run`
  照记数字供诊断）。env `GT6_GATE_MEM_LIMIT_MIB`=<合法整数> = ops 显式
  opt-in 恢复护栏（绝对 MiB 阈值）；未设或非法值=保持退役。
- **帽 25G→22G（v3.6，2026-09-29 用户裁定）**：当日第三次 WSL 崩溃
  （~16:3x）后用户裁定「25G 还是太多了，改成 22G」——本机 MemTotal
  40099MiB、非门禁基线实测 ~15G（嵌入服务 8G+多 ZCode 会话+OS），
  25G 帽+基线≈40G≈满弦；22G+15G=37G 留 ~2.5G 余量。纯值改动
  （`SLICE_CAP_GIB`，准入 cap/看门狗聚合帽/slice MemoryMax 同源跟随），
  v3.1-v3.5 语义零触碰（准入谓词仍只看信封；预算表/slot 并发/清剿层
  不动）。
- **filtered-test 预算 8G→12G（v3.7，2026-09-29）**：六次独立实测——
  多类域 FML junit boot 过滤跑峰值 8733/9329/9521/9900/10800/12600MiB，
  系统性越旧 8G 预算（rc=97 看门狗杀 / rc=247 信封内 memcg OOM），
  coder/审查席被迫 `--task-cap 12` 手动升档五次以上（用户 22G 帽内
  合法调优）。12G 与 full-test 档持平系有意为之：多类域过滤跑与全量
  同足迹级。12.6G 离群样本仍超 12G——此类跑继续 `--task-cap 16`
  （旋钮不废）。纯值改动；准入冷估算（filtered 6G）不动——台账喂
  信封判据，不喂预算。
- 向后兼容：并发槽默认 4（`GT6_GATE_MAX_CONCURRENT`）、退出码=子进程
  透传、full+coder 拒 exit 2、gt6server 直调 `wait_memory` 签名不变
  （遗留反应闸仅服务 boot 路径）；旧 flag 形态（无 `run` 前缀）与 run
  子命令同一实现；`GITHUB_ACTIONS` 置位=零门槛透传（CI 不是本 WSL
  宿主，不注入不看门狗）。

单测：`python3 tools/gt6testgate_test.py`（stdlib unittest，111 项，全
离线零 gradle；`GT6_GATE_SLICE_LIVE=1` 追加 3 项真 systemd 探针：scope
创建/残留 scope 收尸/malloc 膨胀按预算杀；真 systemd-run 透传钉在用户
session bus 不可达的环境自动 skip）。

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
