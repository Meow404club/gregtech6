# POC：Stonecutter 条件注释密度（全量普查 + 最坏文件实跑 + 备选 C 复审）

- 日期：2026-09-03
- 任务卡：p15-poc-chisel-density（P15 跨版本 W1 POC 波，裁定 decisions.2026-09-03-p15-crossversion-arch q3）
- 性质：试验数据记录。**零主仓文件改动，零标注回灌**；本文件是唯一合入面。scratch stonecutter 项目、普查脚本、标注副本全部留在 worktree（`poc/`，不提交）。
- 术语：任务卡称 chisel，官方名 versioned comments / Stitcher syntax（0.7 源码 grep 零命中 "chisel"；见 state tmp.research.stonecutter-chisel-syntax）。
- 语法 ground truth：tmp/harvest/stonecutter-src-07/（stitcher 模块 0.7 分支源码）+ tmp/harvest/stonecutter-chisel-docs/（v1 wiki）。

## Context

P15 路线 A′ = 单代码库 + `//?` 条件注释分叉（1.20.1 Forge / 1.21.1 NeoForge 双节点，Stonecutter 0.7）。architect 钉了两条定量门槛：语义分叉文件占比（分母 = mdk 主源文件数）>25% 触发备选 C（1.21.1 单独分支）；最坏文件 TileEntityBase01Root 必须实跑处理器拿 AST 等价证据。本 POC 回答三个问题：①全库有多少文件、多深的分叉面；②0.7 处理器对真实最坏文件的产物是否保真、可逆；③按钉版口径备选 C 是否触发。

工具链事实（POC 实证）：Stonecutter 0.7 插件 jar 已入 ~/.gradle 缓存（骨架卡先行拉取），Gradle wrapper 8.14 直接可用，系统 gradle 8.7 亦可；节点项目经 settings 插件物化为 `:1.20.1`/`:1.21.1`（目录 `versions/<id>`，TreeBuilderImpl.kt:125），预处理任务名 `stonecutterPrepare`（StonecutterBuildTasks.kt prepareTaskName），产物落各节点 `build/stonecutter-cache/sources/main/`，**不回写共享 src**（回写走 stonecutterMerge，本 POC 未用）。

## (a) 全量静态普查

方法：`poc/census.py` —— 字符串感知注释剥离（状态机：//、/* */、字符串、字符、文本块；与 0.7 处理器同口径：注释/字符串内命中不计）后逐行匹配。分档按任务卡口径：

- **tier1 = import 包名行**（机械替换级：改包名/删行，不语义分叉）
- **tier2 = 语义 hunk**（非 import、非注释行命中触发符号，需条件分叉或 shim/swap）

### 总量（分母 = mdk/src/main/java = **195** 文件；任务卡估 ~130，实际 195）

| 档 | 文件数 | 占比 | 明细 |
|---|---|---|---|
| 零触发 | 65 | 33.3% | 纯 vanilla/gregapi 消费面（recipes 数学、block 骨架、UT6 等） |
| 仅 tier1（forge import） | 68 | 34.9% | 461 行 import，机械改名/删行（2026-09-03 勘误：原误记 65/33.3%，审查 followup；68 与本 ADR 遗留节「68 文件仅 import」及 65+68+62=195 自洽） |
| 含 tier2（语义 hunk） | **62** | **31.8%** | 279 命中行 → 合并 163 个 hunk（行距 ≤2 归并） |

带 forge import 的文件合计 127/195 = 65.1%——**但三分之二的触面是机械行，语义分叉面集中在 62 个文件**。

### Top 触发符号榜（tier2 命中行数）

| 符号 | 行数 | 1.21.1 去向 | 分级 |
|---|---|---|---|
| ForgeRegistries | 76 | Registries/BuiltInRegistries（键名单复数漂移） | swap 级 |
| net.minecraftforge.* 代码体全限定名 | 59 | net.neoforged.neoforge.*（类名不变纯包改名） | swap 级 |
| DeferredRegister | 43 | 同名类换包+注册 DSL 微调 | swap 级 |
| ForgeCapabilities | 40 | Capabilities（1.21.1 常量名同名） | 结构级* |
| LazyOptional | 31 | **类被删除**；getCapability 返回 T 直接量 | 结构级 |
| ItemStack NBT 面（getTag/getOrCreateTag/setTag/hasTag） | 14 | DataComponents | 结构级 |
| RegisterEvent | 9 | 同名换包 | swap 级 |
| NetworkHooks/SimpleChannel | 5 | 网络层重构（PayloadRegistrar 系） | 结构级 |
| IDynamicBakedModel | 2 | Forge 专有接口，NeoForge 无对应 | 结构级 |

*ForgeCapabilities 单 token 可 swap 成 Capabilities，但其比较行所在 getCapability 覆写的**返回类型**随 LazyOptional 删除而变（`LazyOptional<T>` → `T`），故 capability 族整体按结构级计。

### 结构级 / swap 级分档（缓释分析）

| 子档 | 文件数 | 占比 |
|---|---|---|
| 仅含 swap 级符号（可用 0.7 per-node swap 消化，代码零分叉） | 28 | 14.4% |
| 含结构级符号（必须 if/else 分叉或 seam 下沉，ADR-P15-3 纪律） | **34** | **17.4%** |

结构级 34 文件目录分布：tileentity/multiblocks 5、block 3、covers 3、machines 3、tank 3、command 2、recipes 2、energy 2、其余 9 目录各 1（client/render、client/wire、fluid、connectors 等）。TileEntityBase01Root 在列（capability 暴露 5 hunk）。

### 锚点校验（任务卡 18 个锚点文件，全部覆盖）

代码行锚点 38/38 命中（18 个语义热点 + 10 个 item NBT 载体 + 10 个范围锚点内的代码行）。落在注释/Javadoc 里的锚点行（如 TileEntityBase01Root:51 `{@link ForgeCapabilities...}`、GTFluidPipeBlockEntity:472、GTBoilerTankBlockEntity:492-497 等）**正确不计**——0.7 口径下注释永不分叉，javadoc {@link} 失效仅是 doc-lint 非编译错。`poc/anchors.py` 输出逐行比对存档。

### 资源文件过滤器缺口（单独标注）

0.7 默认过滤器仅 java/kt/kts/groovy/gradle/scala/sc/json5/hjson（StonecutterBuildImpl.kt:52）。mdk/src/main/resources = 3089 PNG + 5 .mcmeta + 2 .md（无 .json）；src/generated/resources = **70495 个 .json**（DataGen 产物）。即资源 JSON 双节点默认逐字节共享——除非未来需要分叉，届时走 `versions/<v>/src/` 覆盖目录或扩 filters，不扩默认过滤器。

## (b) 最坏文件实跑（TileEntityBase01Root，0.7 双节点）

### scratch 项目（poc/stonecutter-probe/，哑节点零 MC 依赖）

- settings.gradle.kts：`id("dev.kikugie.stonecutter") version "0.7"` + `create(rootProject) { vers("1.20.1","1.20.1"); vers("1.21.1","1.21.1"); vcsVersion = "1.20.1" }`。注意 0.7 `vers()` 必须**双参**（单参会编译错 "No value passed for parameter 'version'"）。
- stonecutter.gradle.kts（根控制器脚本）：`stonecutter active "1.20.1"` + 空 parameters——条件全用**裸版本谓词**（`//? if 1.20.1 {`），零常量声明即跑通（隐式接收者 minecraft=当前节点版本）。
- build.gradle.kts（节点共享脚本）：仅注释；stonecutter 自动应用 java 插件（StonecutterBuildImpl.kt:47）。只跑 stonecutterPrepare，不跑 compile。
- wrapper 8.14 复制自主仓。任务寻址：`./gradlew :1.20.1:stonecutterPrepare :1.21.1:stonecutterPrepare`。

### 标注副本（poc/annotate.py 从 ORIGINAL 生成）

6 个 fork + 1 个内嵌 fork（21 条指令注释）：imports 块、mItemHandlerCap 字段、setInventory 行、getCapability 整方法（else 部 = 1.21.1 形态示意：`T getCapability` 直接返回 + 内嵌 `//? if 1.21.1` 分叉）、invalidateCaps、itemHandlerCapability。**1.20.1 分支 = 原始行逐字保留**；else 分支为形态示意非 API 验证（验证属 W3 分叉卡）。类 javadoc 不分叉（注释 0.7 永安全）。

### 三证据验证

| 证据 | 工具 | 结果 |
|---|---|---|
| A1 剥离注释后逐字节 diff | python 状态机剥离（census 同款）后内容行序列比对 | **o120 ≡ original，8206 字节 == 8206 字节**（指令注释占位空行不参与比对） |
| A2 javac AST 等价 | JDK17 JavacTask.parse + TreeScanner 全节点转储（kind+规范化子树文本），diff | **ast_orig ≡ ast_120，1154 节点序列完全一致** |
| A3 语法解析 | 同上 parse 阶段错误计数（不含符号解析错） | **ORIGINAL / o120 / o121 三方 parse_errors = 0** |

结论：0.7 处理器对命中侧内容**逐字节保真**（A1+A2 双工具交叉印证），非命中侧包裹为注释后**语法完好**（A3）。

### 1.21.1 侧包裹形态记录（非命中侧从不删码，可逆）

- imports fork（o121 :17-21）：`/*import net.minecraftforge...` … `*///?} else {` —— 禁用侧整块包 `/* */`，闭合 `*/` 与后续指令注释粘联。
- 活动区内的内嵌 fork（o121 :444-447）：内层 `//?} else` 禁用部 `/*if (...) { … }` `*///?}`。
- 禁用侧含 javadoc 的转义（o120 :432-436，1.20.1 节点上 1.21.1 示意域禁用）：javadoc `/**` → ` /^*`、` */` → ` ^/`（`/*`→`/^`、`*/`→`^/`）。
- **双层转义实证**（o120 :440-443）：内嵌 fork 先递归求值包裹为 `/*if…*/`，外层包裹再将既有 `/*`/`*/` 转义为 `/^if`/`^/` —— 与研究预言的"递归处理嵌套条件后包进 /* */"一致。
- **上标 ⁰-⁹ 层级标记未触发**：按 CommentUtil.kt:8-9 与 CommentRemover.unmap，上标仅在**复包裹已含 `/^` 的内容**（≥3 层复包裹）时写入；本样例各域只被包裹一次，实测产物零上标。三层以上嵌套的禁中禁才会用到，9 层上限第 10 层报错。
- 指令注释两侧永不清除（o120/o121 各保留全部 21 条）→ 状态确定可逆。

### 新发现陷阱：纯注释作用域（researcher 陷阱清单之外）

**现象**（首轮试验实测，后修正标注策略）：某 fork 分支仅含 javadoc 时——
- 禁用侧**不包裹**（javadoc 原样裸露）；
- 启用侧反被"解注释"：`/**` 被啃成 `*`、` */` 被啃成空行 → **语法损坏**。

**根因**（0.7 源码）：CommentUtil.kt:10 `isCommented() = all { it is CommentBlock || it.isEmpty() }` —— 全注释作用域被当作"已包裹形态"；CommentAdder.kt:16 对其跳过包裹，CommentRemover.kt:22 对其执行解注释。

**迁移纪律**：`//?` 作用域内**不得只有注释行**；javadoc 分叉必须与代码同域或干脆不分叉。另注意 else 域以 `/**` 开头但含代码时行为正常（isCommented=false）——本 POC 最终标注即此形态。

## (c) 密度结论 + 备选 C 复审

**按 architect 钉版口径：语义分叉文件占比 = 62/195 = 31.8% > 25% → 触发线成立，建议触发备选 C（1.21.1 单独分支）。**

缓释数据（同表供复审裁量）：31.8% 中 14.4 个百分点（28 文件）是单 token 级改名，可用 0.7 per-node swap 消化（代码零分叉，divergence 收进构建脚本）；纯结构级下限 = 34/195 = **17.4% < 25%**，且其中 capability 族可经 seam 下沉（ADR-P15-3 "能下沉不下叉"）进一步压缩。即：**备选 C 的触发按字面口径成立；若接受"swap + seam 消化机械面"的缓释策略，结构级分叉密度可压回线下**。钉版原话（decisions.2026-09-03-p15-crossversion-arch⑥）"语义分叉 >25% 或 Stonecutter churn 不可接受或 M3 门禁连续两卡不达——触发即回写 decisions 并通知在飞卡"：前两条的裁量数据已齐（本 ADR），第三条待 M3。

对本仓五点推论：
1. 双节点单库**可行**：处理器保真性（A1/A2/A3）与可逆性实证通过，最坏文件无处理器层障碍。
2. 分叉波（W3）的实质工作量 = 34 个结构级文件（17.4%），非 195 文件全动。
3. capability 族（LazyOptional 31 行 + ForgeCapabilities 40 行）是最大结构面，建议独立 core 卡先行落 seam（getCapability 返回类型变化是接口级断裂，不是行级分叉）。
4. ForgeRegistries/DeferredRegister/RegisterEvent/内联全限定名（合计 187 行/28 文件）走 0.7 swap 参数化，进骨架卡决策点（swaps 在 stonecutter.gradle.kts 按 node 分支）。
5. 纯注释作用域陷阱入迁移 checklist；`vers()` 双参、节点寻址 `:<id>:`、产物路径进骨架卡 README。

## 遗留 / 池

- 本 POC 的 1.21.1 else 侧为形态示意，未对 NeoForge 21.1 API 验证（属 W3 分叉卡）。
- swap 参数化样例未实测（0.7 swap DSL 属骨架卡决策点，本卡只证明 swap 级符号可机械改名）。
- 65 文件零触发、68 文件仅 import——这部分的双节点编译等价性由骨架卡 M2 门禁（双 build 双 test）兜底，未单独验证。
- worktree 内未提交物：poc/census.py、poc/anchors.py、poc/census-out/、poc/annotate.py、poc/verify_strip.py、poc/AstCheck.java、poc/TileEntityBase01Root.ORIGINAL.java、poc/stonecutter-probe/（含双侧产物）。
