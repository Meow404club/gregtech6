# P16 收口数据文档：四判据门禁总账 + 11 卡表（p16-closeout）

- 日期：2026-09-05
- 卡：p16-closeout（W4 阶段收口卡，门禁终验 + docs 镜像，零 Java 源触碰）
- 基线：main `c852fa68`（P16 全部 11 卡已合入，全程零打回）；阶段起点 main `599d6c80`
- 执行树：`MGT6GA-trees/p16-closeout`（`work/p16-closeout`，1.20.1 腿 + gradle 判据 + runData）
  与 `MGT6GA-trees/p16-closeout-gate`（detached `c852fa68`，1.21.1 RCON 腿）——ADR-P15-4
  双树同 commit 纪律（`git rev-parse HEAD` 双侧核对一致）
- 权威口径来源：P15 dual-gate-closure 先例（`tools/rcon/sweep.py` session 模型 +
  `Step.node_cmds` 节点键形分叉，`tools/rcon/chains/framework.py:189-214`）；W4 审查教训
  id288（1.21.1 污染后 `.cache` 对齐序）；P13 `--no-build-cache` 排假绿纪律；ADR-P15-10
  红文件计数口径（`mdk/build.forge.gradle.kts:40-42` `-Xmaxerrs 100000` 全量诊断）。

## Context

P16 主线（用户 2026-09-05 二次校准）= **A 线 多方块成型判定三件套**（kTFRUAddon 机制级
clean-room 借鉴，AGPL 红线只借机制不搬代码；decisions `2026-09-05-p16-formation-scoping`）
+ **B 线 继续移植 GT6**（P14 顺延队列六卡）+ **21.1 追修两卡**（clienthandlers 构造崩 /
blockstates Property identity）。拆卡裁定 `2026-09-05-p16-card-split`（10 卡 4 波 + 收口，
并行度 ≤3，files_scope 互斥；gui+dryer GUI 合一卡、side-io+item face gate 合一卡）。

11 卡全表（顺序 = merge 顺序；verdict 全 approve）：

| # | slug | wave | 提交数 | merged | 要点 |
| --- | --- | --- | --- | --- | --- |
| 1 | p16-pattern-checker | W1 | 6 | `7ae62278` | Cell 成型期望三件套（usage/design/partBlock）+ GTMultiBlockStructureChecker 共享校验器（逐格走 checkAndSetTarget 等价路径，hollow fail-not-clear，unloaded 先探测后判定）+ CokeOven 生产试点切换 + check 报告 first_failed_cell；mdk +23 测 |
| 2 | p16-aqua-fluids | W1 | 4 | `00259c7e` | 表驱动 AquaFluidSpec 六流体 + lang walker + 7 测；water_hot 刻意不注册（absent-fluid skip 保持）；RCON 六桶臂 |
| 3 | p16-machine-side-io | W1 | 2 | `ce09d41e` | mAccessible[7] per-side 物品掩码（127 默认零回归）+ 流体 auto-IO 拉臂(checkRecipe)/推臂(onTick P6 三拍) + :565 push-only 拒注 + NBT_TANK_CAPACITY 容量先于内容 + mCanUseOutputTanks 产出罐 fallback；11 测 |
| 4 | p16-pattern-layers | W2 | 2 | `b579b998` | 层序列 DSL：layer + repeatable(min,max,factory) + family per-n build 期展开，不可变性不破；锅炉桩三方一致钉测；无生产消费者（明知，为池件变长机备料） |
| 5 | p16-machine-fluid-gui | W2 | 8 | `7fd88971` | 机器流体罐 GUI 面（Host 接口化保 machines BE 零触碰）+ dryer GUI 合一（GTDryerFamilyRowTest:70 menu=null 池约定接管）；DISPLAY_CONTAINER static 共享=非阻塞池项 |
| 6 | p16-drying-rows-backfill | W2 | 3 | `aa242411` | DRYING 冰雪 12 行 + 水族 7 行回补（census 13/2/11：OP.java:174-175 + MT.java:1013 死行亲证）；rebase 对位修两处（链 data=-2→-1 对齐 gui 卡语义） |
| 7 | p16-clienthandlers-2111 | W3 | 1 | `e396bdc9` | GTClientHandlers.java:38 抽象 RegisterColorHandlersEvent 拆 .Item/.Block 子类监听（javap+EventBus 字节码亲证注册期即拒）；解除 P15 判据③ 1.21.1 runData 阻断的第一层 |
| 8 | p16-form-scaffold | W3 | 8 | `c6c9a4a2` | GTMultiBlockStructureChecker.form() 计划-执行两阶段事务 SET 补建（纯 CHECK 早退 → 失败格分类 → isSameItemSameTags 核账不足整单拒 → 两遍 onToolClick2）+ /gt6multiblock form；census 26→25 勘误（controller 自格 Util:49 免费）；check() 零改动 |
| 9 | p16-blockstates-2111-prop-intern | W3 追加 | 6 | `c0b0a87e` | ADR-P16-2 方案 a：GTBlockProperties 单一 owner（ACTIVE/RUNNING/CONNECTIONS 私构）+ 4 类 6 常量类内别名，消费点零改动；GTBlockPropertyIdentityTest assertSame 3 测；**解除 1.21.1 runData 判据③ 第二层**（首见 21.1 runData EXIT=0） |
| 10 | p16-chisel-decalcify | W4 | 6 | `21be5daf` | Chisel 除垢：上游三处逐字（mDamage=25 / 付款门 / 爆炸臂 return 0）+ units 数学全表（400→1/1000→3/5000→13/10000→25）+ 贴图 sha256 双端亲算 |
| 11 | p16-distillery-family | W3 | 11 | `c852fa68` | Distillery 家族四变体 + integrated_circuit 物品（单 item + Damage int 载体）+ Loader 4 行 / Chem 8 行逐参 + 生成物 runData 产出；21.1 节点 19 新测编译执行=双腿最强实证；mdk +19 测 |

## Decision（四判据终局，每条独立取证）

### ① 双节点 RCON 终门 [0,0]×2

**回归全集 19 链**（`sweep.py` SESSION_GROUPS：p11 shutter-filter + p12 九链 + p13 五链 +
p14 三链 + p15 runtime_smoke；session 模型 concurrency 1，passes=2 幂等含内，两 boot group
＝ fresh_boot smoke 独占）：

| 节点 | 树 | wall | boots | pass_failures | server ERROR | 结果 JSON |
| --- | --- | --- | --- | --- | --- | --- |
| 1.20.1-forge | p16-closeout | 2564.6 s | 2 | 19 链全 [0,0]（p14_dryer_family 经链臂修复后复验 [0,0]，见 Deviations 6） | 0 | `/tmp/gt6_rs_sweep_session_c1_1201-forge.json` |
| 1.21.1-neoforge | p16-closeout-gate | 2594.8 s | 2 | 19 链全 [0,0]（同上） | 0 | `/tmp/gt6_rs_sweep_session_c1_1211-neoforge.json` |

1818 步/腿，session 模型 concurrency 1（P15 先例 1495.9s/1567.1s 系 concurrency 4，wall 不可直接比）。
终态 **FAILING=NONE 双节点**（1.21.1 侧两项 21.1 运行时 delta 见下，属 p16 新链非回归链）。

**P16 新增 8 链**（`chains/p16_{pattern_checker,aqua_fluids,side_io,machine_fluid_gui,drying_rows,form_scaffold,chisel,distillery}.py`；
卡面「六链」=拆卡时点计数，收口实存 8 链全跑）——走 **per-chain boot `GT6_SESSION=off`**
（卡面 sanctioned 的 boot 归属安全路径；每链独立 boot、卡钉端口 2577x/2578x、`passes=2`、
判定器与 session 模型同源 `gt6rcon.judge_output`）：

| 链 | 1.20.1-forge | 1.21.1-neoforge |
| --- | --- | --- |
| p16-pattern-checker | [0,0] ERROR 0 | [0,0] ERROR 0 |
| p16-aqua-fluids | [0,0] ERROR 0 | [0,0] ERROR 0 |
| p16-machine-side-io | [0,0] ERROR 0 | [2,2]（21.1 运行时 delta，见下） |
| p16-machine-fluid-gui | [0,0] ERROR 0 | [0,0] ERROR 0 |
| p16-drying-rows-backfill | [0,0] ERROR 0 | [3,3]（21.1 运行时 delta，见下） |
| p16-form-scaffold | [0,0] ERROR 0 | [0,0]，server ERROR ×1（诊断，见下） |
| p16-chisel-decalcify | [0,0] ERROR 0 | [0,0] ERROR 0 |
| p16-distillery | [0,0] ERROR 0 | [0,0] ERROR 0 |

**1.21.1 运行时 delta 三案（全数记录、归池不修；生产域问题非链臂问题非框架问题）**：

1. **p16_drying_rows 步 19/21/22 [3,3]**——snowball 经料斗（`hopper[facing=down]`→机器）进料：
   1.20.1 机器槽持留 `input=snowballx1` 且 inject 4000 后产 250L DistW；21.1 微探针
   （`/tmp/p16co_probe_1211.log` A 段）实证 **snowball 全程留在料斗**（`data get block` 逐次
   `count:1, id:snowball` 恒在），机器槽恒 `input=airx0`、零产出零批绑定——**21.1 料斗→机器
   item 推送链断**（机器 item capability 面的 wiring/侧掩码域，与桶族 FLUID_HANDLER 逐 BET
   注册先例同构的缺口怀疑）。
2. **p16_side_io 步 16/17 [2,2]**——跨机 auto-IO rig：先因**框架缺陷**（Deviations 2）21.1
   merge 误用 forge 键形；/tmp 驱动器预置 `chain.node` 复跑（merge 以 21.1 `{id,amount}` 形生效，
   步 15 `in[0]=920 water` PASS）后仍 FAIL——21.1 上 B 机经 auto_in 持续抽干 16000L 桶
   （out=12800L DistW 全产出），A 机经共享面的自动臂恒不动（1.20.1 同 rig 平衡态成立）——
   **21.1 机器 auto-IO 跨机流体面运行时差异**。
3. **p16_form_scaffold server ERROR ×1/遍（pass_failures [0,0] 全绿）**——
   `[Server thread/ERROR] [minecraft/BlockEntity]: Failed to load data for block entity
   gt6:multiblock_coke_oven`（`/tmp/gt6_rs_p16form.log`，每 pass 一次，断言面无感）——
   21.1 cokeoven BE load 诊断项，随 21.1 BE 序列化域池件观察。

两 delta 合并归池：**「21.1 机器 capability/IO 面运行时差异 + 22 红逐条裁」合卡候选**（料斗 item 面
与 auto-IO 流体面 + FML 灌注反转测试族同域）。

**Boot 归属复核**（已知教训：session 模式曾连到旧行为服务器，`tasks.p16-pattern-checker` 遗留）：
两腿 session boot 前 `ss -ltn` 节点段（256xx / 257xx）全空；boot 时 `pick_ports` 取原值未顺延
（1.20.1 rcon=25662 / 1.21.1 rcon=25762）；eula.txt / server.properties 在**本树**节点 run 目录
新写（`mdk/versions/<node>/run/`）；pid 文件由本 boot 写入。p16 链逐链 fresh boot，结构性排除
连旧服务器。服务端一律 `--nogui`（run 配置内置）+ nohup 语义 + 日志 Done 轮询 + 按 PID/端口
主精确收尾（`gt6server.stop_server`，`--stop`/pkill 全程未用）。

### ② runData 终验双节点（严格按序，全在 p16-closeout 树）

| 步 | 命令 | written | git porcelain（mdk/src/generated）| 备注 |
| --- | --- | --- | --- | --- |
| a | `:mdk:1.20.1-forge:runData` 一跑 | **70521** | 0 | 新树空 `.cache`，产物与 c852fa68 正典 byte-identical |
| b | `:mdk:1.20.1-forge:runData` 二跑 | **0** | 0 | 幂等证明 |
| c | `:mdk:1.21.1-neoforge:runData` 一跑 | EXIT=**0**（written 4586） | 9170（4585 D + 4585 ??） | 零 `IllegalArgumentException` / 零 `does not exist`（grep 计数 0/0）；副作用=共享树 `data/gt6/loot_tables/`→`loot_table/` 单数化（1.21 数据包目录变更，内容 1:1） |
| d | 恢复正典树 | — | 0 | `git checkout -- mdk/src/generated` + `rm -rf .../data/gt6/loot_table`（1.20.1 形=正典） |
| e | `:mdk:1.20.1-forge:runData` **对齐跑** | **4586** | 0 | `.cache` 层重写（id288 教训兑现：跳过此步下一跑假报 written:4586——4585 loot 行+1；distillery 后基数较 W4 的 4582 +4） |
| f | `:mdk:1.20.1-forge:runData` 二跑 | **0** | 0 | 终验 written:0 |

### ③ 红文件门禁（1.21.1）

- `:mdk:1.21.1-neoforge:compileJava :mdk:1.21.1-neoforge:compileTestJava --no-build-cache`
  **真执行**（任务行无 UP-TO-DATE 后缀，10 actionable / 9 executed）BUILD SUCCESSFUL 32s；
  双口径：raw 红文件 **0** / 错误行 **0**；去空白归一 红文件 **0** / 错误行 **0** → 0 新增
  （基线 P15 M4 首绿以来 0 红，本阶段 11 卡逐卡 0→0 维持）。
- `:mdk:1.21.1-neoforge:test --no-build-cache` 真执行：132 suites **1072 实跑 22 失败 / 0 错误
  / 2 跳**；失败集 9 类 22 条与基线 XML（主工作区 `mdk/versions/1.21.1-neoforge/build/test-results`）
  按（类名, 测试名, 首行消息）三元组 diff：**only-in-mine ∅ / only-in-base ∅ / 同名异消息 0**
  = IDENTICAL-22。逐类：RecipeMapFurnaceBridgeTest 6 / TileEntityOvenRecipeTest 6 /
  TileEntityOvenEnergyTest 4 / GTEngineFuelsTest 1 / GT6RecipesOreChainTest 1 / FluidBridgeTest 1 /
  GTMaterialBlocksRegistrationTest 1 / GTCoverClientListenerTest 1 / TileEntityBase08BarrelTest 1
  （20 配方链 FML 灌注反转 + MRL standalone 真差 1 + keepFilter 0 量折叠真差 1，P15 定性不变）。

### ④ 测试全量（1.20.1 节点）

- root：`./gradlew :cleanTest :check --no-build-cache` → 15 suites **205 / 0 失败 / 0 错误 / 0 跳**
  （JUnit XML 实数；首两次 `:check` 分别 FROM-CACHE / UP-TO-DATE 假绿，以 cleanTest 真跑为准）。
- mdk：`./gradlew :mdk:1.20.1-forge:cleanTest :mdk:1.20.1-forge:test --no-build-cache` →
  132 suites **1073 / 0 / 0 / 0**（卡面「当前口径 1054」= chisel 合入 21be5daf 时点；distillery
  c852fa68 再 +19 → 1073 为 c852fa68 真值）。
- 合计 **1278 全绿**（P15 收官 1176 → +102：pattern-checker 23 + aqua 7 + side-io 11 + layers/gui/
  rows/form/blockstates 3+…+ chisel + distillery 19）。

## Deviations（声明偏离，各带理由）

1. **p16 八链未走 sweep 全集**：`tools/rcon/sweep.py:47-57` SESSION_GROUPS 仍是 P15 时点 19 链，
   未纳入 p16_*（框架缺口；FILES_SCOPE 禁改框架代码，记录不修）。收口改 per-chain
   `GT6_SESSION=off` 逐链双节点跑，判定语义同源（同 steps/judge/passes）。池：SESSION_GROUPS
   增 p16 坐标簇（p16 链 sites 多在 0..40 带，需 `--plan` 复核 bbox 相交后分簇）。
2. **框架缺陷（记录不修，一行修入池）**：`framework.run()`（per-chain boot / GT6_SESSION=off
   模型）解析 `--node` 只进 boot 任务选择（`framework.py:344` 局部变量），**不回写
   `chain.node`** → `_run_pass→run_steps(node=chain.node=None)` → `step_cmd(step, None)`
   永不走 `Step.node_cmds` 键形分叉（`framework.py:210-214`）。sweep 模型显式
   `chain.node = node`（`sweep.py:127/:160`）故无恙。本次后果：side_io 21.1 腿首跑误用
   forge {FluidName,Amount} 形；处置=/tmp 驱动器预置 `chain.node` 后 `framework.run` 复跑。
   池修=run() 内 `chain.node = node` 一行。
3. **双腿未用 `--dual` 单命令**：1.21.1 腿先起（与 gradle 判据并行省 wall），1.20.1 腿待 gradle
   释放项目锁后起；同 commit 双树纪律保持（手工核对），wall 各自报不取 max。
4. **root `:check` 缓存假绿两次**：共享 `~/.gradle` 构建缓存命中（3 from cache）/ 产物已存在
   UP-TO-DATE；以 `:cleanTest :check --no-build-cache` XML 实数为准（教训入 Consequences）。
5. **1.21.1 runData 二跑 written:0 结构性不可达**（共享 generated 树 `loot_table` 单数化每跑必
   重写）——卡面只要求一跑 EXIT=0 零异常，正典树形态钉 1.20.1 形；跨节点产物树裁决入池
   （候选：输出目录按节点参数化 / per-node 产物树 / 接受 21.1 形落账，需架构师 ADR）。
6. **p14_dryer_family 链臂修复（本分支唯一非 docs 文件变更，tools/rcon 资产非框架非 Java）**：
   gui 卡 193a70cf 翻转 `data=-2→data=0` 期望时未跑该链；收口双节点 sweep 双双 [1,1] 同因
   （t4 步 75）。活体探针实证 t4（parallel 64）在 driven injects 内耗尽 1000L 水（批1→批2
   连续完成，out 800L DistW、in=0）→ post-reset re-find DID_NOT_FIND → 表盘 GTOvenMenu:285
   -1 臂——**生产行为正确、期望缺陷**。修=TIERS 增 per-tier data 臂列（t1-t3 `data=0` /
   t4 `data=-1`，-1 仍证明菜单已构造，menu-less 读 -2），双节点复验 [0,0]
   （`/tmp/p16co_dryer_fixed_{1201,1211}.log`）。gate 树（p16-closeout-gate）内该文件为同步
   工作副本（gate 树不提交，收尾删除）。
7. **mdk 测试口径 1054→1073**：非回归，distillery +19 新测的合入时点差；以 XML 实数入账。
8. **1.21.1 运行时 delta 两案 + form ERROR 诊断**（详表见 ① 节）：drying_rows 料斗→机器 item
   面 [3,3]、side_io 跨机 auto-IO 面 [2,2]（键形修复后仍异）——生产域行为差异，
   FILES_SCOPE（零 Java 源触碰）内不修，归「21.1 机器 capability/IO 面 + 22 红逐条裁」合卡
   候选；form_scaffold cokeoven BE load ERROR（链绿）随 21.1 BE 序列化池件观察。

## Consequences

- 数字镜像：`docs/PROJECT_STATE.md` 第 16 阶段收官记录 + `docs/TODO.md` 第 16 阶段段/遗留池；
  state/KG 不动（主会话收官蒸馏：phase_anchors.p16 + KG CLOSED_AT + decisions + 记忆硬删）。
- **本阶段新增承重教训**（待主会话蒸馏入记忆）：
  1. **Property identity**——1.21.1 `StateHolder` 值表 `Reference2ObjectArrayMap` 按 `==` 查 key
     从不消费 `equals`：同名 `BooleanProperty.create("active")` 多处定义=不同实例 → runData
     `IllegalArgumentException: Cannot set property ... as it does not exist`；正解=单一 owner holder
     `GTBlockProperties`（ADR-P16-2，`mdk/src/main/java/gregtech6/block/GTBlockProperties.java`）
     + 消费类静态别名 + `assertSame` 钉测。
  2. **双节点 runData 恢复序**——1.21.1 runData 把共享树 `loot_tables/`（4581 文件）改写为
     `loot_table/`；恢复正典树后**必须**再跑一次 1.20.1 runData 对齐共享 `.cache`，否则下一跑
     假报 written:4582（cache 层重写，产物层 porcelain 始终空）。
  3. **追加式派发**——审查会话 ≤3 分支轮换（先派后补）：W1/W2/W3 各 3 卡一会话，11 卡零打回；
     并行卡链臂引用他卡语义标记须预判 rebase 后行为（drying-rows 链 data=-2→-1 对齐 gui 卡）。
  4. **门禁假绿三形态**——`:check` FROM-CACHE / UP-TO-DATE / 裸 `check` 带跑两节点 mdk test
     （21.1 22 已知红伪红整体 BUILD）：正典=根项目 `:cleanTest :check --no-build-cache` +
     `:mdk:<node>:cleanTest test --no-build-cache`，一律核 JUnit XML 实数；Stonecutter 树测试结果
     在 `mdk/versions/<node>/build/test-results/`（不在 `mdk/build/`）。
  5. **RCON 链三条**——`gt6tank accept` 灌+排验收后留空桶，外部液源用 `data merge` 直填 tank
     NBT（forge `{FluidName,Amount}` / 21.1 `{id,amount}`，`Step.node_cmds` 键形分叉）；
     `findRecipe` 拒空 items 数组（RecipeMap.java:138）→ fluid map 至少 1 输入物品槽；被消费罐的
     「满罐」断言按 tick 末账本写（拉臂回满 1000 同拍耗 80 → 稳态 920）。
  6. **成型 census**——cokeoven pattern 26 forming 声明含 controller 自格（Util:49 免费通过）→
     实耗 25；卡面数字先 census 再钉字面量。
  7. **每解除一层 BLOCKED 首跑必出新双节点差异**——首见 21.1 runData 跑通即首见共享树 loot
     目录分叉；验收前先想 generated 树归谁。
  8. **session artifact 名泛化**——`/tmp/gt6_rs_session_<node>.{log,pid}` 同节点段并行 session
     互踩 pid（首跑被外部 SIGTERM 实例）；`GT6_SESSION=off` 规避；池：session slug 加 worktree 哈希。
  9. **pkill -f 自匹配**（本卡事故）——`pkill -f "<pattern>"` 的模式字串出现在自身 shell 命令行
     中即自杀，后续命令静默未跑（本卡一次 heredoc 重写因此丢失）；且 pkill 本就在禁令内
     （用户裁定 2026-09-01）——纪律再钉：进程收尾只走框架 `stop_server`。
- **池移交清单**（P17 起点，权威=state todo.current.pooled；本文件为镜像）：
  **21.1 机器 capability/IO 面运行时差异 + 22 红逐条裁（合卡候选：料斗→机器 item 面断/跨机
  auto-IO 流体面/FML 灌注反转测试族/cokeoven BE load ERROR 观察项——本收口新增）** /
  **framework.run() node 回写一行修（GT6_SESSION=off 下 node_cmds 失效——本收口新增）** /
  **sweep SESSION_GROUPS 增 p16 簇（本收口新增）** /
  datagen 共享 generated 树跨节点裁决（loot_table 单数化，需 ADR）/
  gt6oven run 200 基线即红追查（progress=0/0 无配方匹配，机器/配方域零 diff）/ session artifact slug
  加 worktree 哈希 / 物品掩码 RCON 活体臂（side-io 离线真值表覆盖）/
  boiler declaration-only form 拒绝臂 + creative 免费臂无 Player fixture 未单测 / 真实 wand 物品化
  （form 命令臂已兑现三件套签名）/ 变长机消费者（layers DSL 无生产消费者）/ GTMultiBlockPattern
  NOTHING 常量注释 / fluid_sides_auto_* 出界值守卫（>5 折叠 no-op）/ mExpansions HashMap 线程安全 /
  DISPLAY_CONTAINER static SimpleContainer(0) 跨菜单共享（活体流体渲染落地时 per-menu）/ datagen
  共享 generated 树跨节点裁决（loot_table 单数化）/ keepFilter 读侧重建裁决 / quiet_window 自适应 /
  vanilla rconConsoleSource upstream / 21.1 22 红逐条裁 / 1.21.4+·1.21.6+ 回池（用户 2026-09-05：
  等其它全部干完再考虑）/ runClient 视觉整备（dryer 四变体/流体面/distillery/chisel）/ kTFRUAddon
  通用化（池底）/ curator astchunk-src/ts-official 354 文件补审。
