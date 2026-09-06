# ADR-P18 草稿：staticinit 毒态裁决（IDENTICAL-3 残两案修复方向）

日期：2026-09-06 ｜ 状态：正典（P18 收官成文，main fc494c6b） ｜ 前置：诊断账本 state 键 tmp.p18.diag-staticinit（debugger 活体取证） ｜ main=2c3fc966

## Context

P17 后 21.1 基线残集 IDENTICAL-3，其中两条（keepFilter barrel 案另裁，卡 p18-keepfilter-2111-readback 在途）同根因：**跨测试类共享静态合成毒态「maps=null × sLoaded=true」→ 生产 load() 静默早退**。debugger 活体取证已证机制链（本节全部 file:line 已由架构师复核）：

1. **boot 真引导**：21.1 测试 JVM 经 junit-fml LauncherSessionListener 全引导（mdk/build.neoforge.gradle.kts:69-76），FMLCommonSetup.enqueueWork 真跑生产 load()——GT6RecipesEngineFuels.java:149-152、GT6RecipesOreChain.java:151-154，私有旗标置真（GT6RecipesEngineFuels.java:146→:168、GT6RecipesOreChain.java:148→:172）。boot 末态活体实测：{11 图全 set、RECIPE_MAPS=11、EF.sLoaded=true、OC.sLoaded=true}。
2. **reset 半代**：GT6RecipeMaps.reset()（GT6RecipeMaps.java:237-250）只清 11 个图字段 + RecipeMap.reset()（RecipeMap.java:54-56），**不清任何 loader 旗标**；旗标只被各自测试类 @AfterEach 的 resetForTest 清（GTEngineFuelsTest.java:47、GT6RecipesOreChainTest.java:72）。
3. **毒态与静默早退**：JUnit 同 JVM 跨类共享静态 → 组合出毒态；load() 对毒态 `if (sLoaded) return` 零日志早退（GT6RecipesEngineFuels.java:156、GT6RecipesOreChain.java:158）。案① GTEngineFuelsTest.loadIsIdempotentPerGeneration NPE :154（类序 OreChain 先跑遗留 reset → EngineFuels 首位方法吃「null 图 × 真旗标」）；案② GT6RecipesOreChainTest.positiveControlThroughFindRecipe assert :338（**单方法独跑即红**——boot 活图 CRUSHER=1042 行非本测试灌的合成 resolver 行，:317 load() 早退致合成行从未灌入）。
4. **毒态 capable 集合 = 7 个 loader，不止点名的两个**：sLoaded 持有者全量=GT6RecipesDistillery.java:145、GT6RecipesDrying.java:223、GT6RecipesBurnFuels.java:153、GT6RecipesEngineFuels.java:146、GT6RecipesCokeOven.java:206、GT6RecipesOreChain.java:148、GT6RecipesShCL.java:267。而 reset() 的 15+ 个测试调用点中 GT6MachineFluidDisplayTest.java:89、GT6CokeOvenMenuTest.java:81、RecipeMapFurnaceBridgeTest.java:64、TileEntityBase10MultiBlockMachineTest.java:109、GT6RecipeMapsTest.java:18/:212、GT6CokeOvenLogExpansionTest.java:31、GTMachinesOfflineTestBase.java:85、GTDistilleryFamilyRowTest.java:249/:287 等均**不配对** resetForTest——半代 reset 是共性脚枪（TileEntityBasicMachineFluidFaceTest.java:54 的 javadoc 已把它当作已知行为记载）。另 5 个 loader 暂未红只因对应测试类恰好在类内先调 resetForTest 自愈，属时序侥幸非机制免疫。
5. **1.20.1 腿**：无 FML 引导（GTOfflineTestBase.java:36-45 仅 vanilla Bootstrap），sLoaded 恒 false，load() 永真灌 → 方法序无关全绿（对照实测 BUILD SUCCESSFUL）。

**消费面盘点（裁决题①的事实前提）**：

- `GT6RecipeMaps.reset()`：**生产调用者 = 0**（sym_query 全库命中全部位于 mdk/src/test）。reset() 在 main 树只是 Port-only 工具（GT6RecipeMaps.java:236 注释自证）。
- `GT6RecipeMaps.init()`：生产调用 = GTMachines.java:486（W1 handoff，每 JVM 生命周期一处）——不在本裁决触碰面。
- loader `load()`：生产调用 = 每 JVM 恰一次（FMLCommonSetup enqueueWork；另 init() 防御性幂等）。`if (sLoaded) return` 在生产是死路径（单次调用永不触发）。

## Decision

**裁 A（世代化重置），吸收 B 的可观测性子项；B 主案否决。** 任务卡 `p18-staticinit-generation-reset`，分支 `work/p18-staticinit-generation-reset`。

- **机制**：reset() 与 loader 旗标同代重置，钩子注册制实现（不让 maps 反向硬依赖 7 个 loader 类）：GT6RecipeMaps 增私有 `CopyOnWriteArrayList<Runnable> sGenerationResetHooks` + 包私有 `registerGenerationResetHook(Runnable)`（幂等）；reset() 在 RecipeMap.reset() 之后逐钩子 try/catch 执行（单钩子异常不孤清理余，WARN 记录）。7 个 loader 各在 static initializer 注册自身 resetForTest 引用，并为早退行加 `LOGGER.debug("...skipped: already poured...")` 审计（毒态排障最大成本是零痕迹；DEBUG 级避免与 pour INFO 混淆、生产行为零变）。
- **类初始化次序安全论证（可证无新静默路径）**：旗标为真 ⟹ 该 loader 类已初始化且 load() 已跑 ⟹ 其钩子必已注册；reset() 先于类初始化只看见空集（旗标本为默认假）。钩子注册方向 loader→maps 与既有依赖方向（loader 已引用 GT6RecipeMaps 灌图）同向，无新反向耦合、无 clinit 环（GT6RecipeMaps 静态字段皆 null + 空 list）。**load() 的 `if (sLoaded) return` 语义逐字节保留**——A 不在生产 load() 增加任何分支。
- **毒态不可表示**：修复后「旗标为真 ⟹ 当前代活图在」成为机制不变量；「maps=null × sLoaded=true」无法经任何公共路径构造。
- **两案测试自 grounding（消融序敏感）**：GT6RecipesOreChainTest.positiveControlThroughFindRecipe 在 :316（resolver 注入后、load() 前）显式 `GT6RecipeMaps.reset()`；GTEngineFuelsTest.loadIsIdempotentPerGeneration 在 :151 方法头同办。两测试不再依赖环境态（boot 遗留或前类 @AfterEach），**单方法独跑绿由构造保证**。既有 @AfterEach 的 reset+resetForTest 配对原样保留（冗余无害，churn 最小化）。
- **防复发守卫（裁决题③）**：新增 GT6RecipeGenerationGuardTest，两层钉：行为层「reset() 后（不调 resetForTest）loader.load() 必须真重灌」（EngineFuels WATER_FIXTURE 7 行→reset→load→须再 7 行；OreChain 合成 resolver 同形）——该 canary 在修复前基线必红（正是毒态本体），须先红后绿留痕；结构层「7 个 loader 类全部已注册钩子」——新 loader 漏注册即红，守卫清单即毒态 capable 集合的活账本。seam 即钩子注册点本身，无需 reflection。
- **B 否决理由**：(1) B 对案②结构性不可实现——boot 后 CRUSHER 活图 1042 行 × OC.sLoaded=true，load() 视角无 generation 标记则「旧代」不可定义；补 generation 簿记 = 先做 A 的机制再在生产 load() 加条件重灌分支。(2) 该重灌分支是**新增静默路径**（误判 stale → 活图双灌），直接违反「禁引入新静默路径」纪律；A 的钩子清旗在生产行为零可观测（reset 生产零调用）。(3) generation 计数变体（对齐计数而非清旗）同理需 load() 消费计数 → 同 B 陷阱；无人消费的计数是死重。B 中值得保留的只有审计日志（已吸收）。

**裁决题①（生产语义影响面）回答**：reset() 生产零调用、load() 生产单次调用且早退语义不变、init()（GTMachines.java:486）不动、1.20.1 腿旗标恒假钩子清假=no-op——1.20.1 正典行为零改动由「改动面生产不可达」构造性保证，并以全量双腿测试门禁验证。

**裁决题②（IDENTICAL-2→0 验收）回答**：本卡与 keepFilter 卡文件零重叠可并行；「IDENTICAL-0」以两卡均合入后的合并基线跑 21.1 全量（1073 tests）判定，残红=∅；若本卡先合入而 keepFilter 未合，残红恰=barrel 一条（三元组逐条等值，不得新增）。两案的单方法独跑与两类双序（OreChain→EngineFuels、EngineFuels→OreChain）定向复现命令作为独立门禁（不再依赖全量套件的类序偶然性）。

**裁决题④（与 GTOfflineTestBase/resetForTest 契约兼容）回答**：1.20.1 腿 GTOfflineTestBase（GTOfflineTestBase.java:36-45）与 GTRecipesOfflineTestBase（GTRecipesOfflineTestBase.java:63-84）均不触 loader 旗标，本卡不改动两基座；resetForTest 签名与包私有可见性原样（钩子以方法引用挂接，Distillery:218 的 public 可见性也兼容）；@AfterEach 既有配对调用不迁移不删除。

## Deviations

- **reset() 语义扩大（maps-only → 全代重置）**：类注释（GT6RecipeMaps.java:236「drops the whole generation」）本已宣言全代语义，旗标遗漏是实现缺口非契约变更；对生产不可观测（零调用者）。
- **若全量绿门禁暴露某测试依赖「旗标跨 reset 存活」（即编码了毒态）**：按毒态编码处理——测试树内补显式配对 resetForTest，coder 不得为此扩产域、不得弱化该测试断言，须在收工账记名。
- **审计日志仅 DEBUG 级**：生产早退=每 JVM 至多 0-1 次的死路径，INFO 会污染正常启动日志；若未来出现生产多处 load() 调用面，再议升级（池观察）。

## Consequences

- 正面：毒态不可表示；「每类恰红一」的 JUnit 方法序敏感面整体消融（序敏感真因是毒态非 JUnit，见诊断 method_order_sensitivity_verdict）；keepFilter 合入后 21.1 基线 IDENTICAL-2→IDENTICAL-0；静默早退有痕；半代 reset 共性脚枪（第 4 条 8 个调用点）机制性关闭。
- 风险与边界：7 个 loader 的 static-init 从此触 GT6RecipeMaps 类初始化（无环，见 Decision 论证）；钩子异常以 try/catch 隔离不中断清理链；guard 测试为 mdk 测试基线 +1 文件（root 205 + mdk 1074 → 只增不减）。
- 1.20.1 正典零改动保证：见裁决题①；生产 diff 面 = GT6RecipeMaps.java（钩子表+reset() 尾部 4 行）+ 7 loader（各 1-2 行 static-init 与 debug 日志），无公共 API 增量（registerGenerationResetHook 包私有）。
- 后续触发器：新 loader 增设 sLoaded 时必须 static-init 注册钩子 + 扩守卫清单（漏注册=守卫红）；若未来出现 reset() 的真实生产调用者（如 datapack 重载），须复核钩子链在 worker 线程的可见性（当前 CopyOnWriteArrayList 已线程安全，标记为复核点非阻塞项）。
