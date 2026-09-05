# ADR-P17-1：mdk/src/generated 共享产物树跨双节点裁决（datagen 输出目录按节点参数化）

- 日期：2026-09-05
- 状态：Accepted（架构师裁决；docs/adr 落地与本卡实现走 coder 分支 `tasks.p17-datagen-node-local-output`）
- 关联：P16 收口门禁账 `docs/adr/2026-09-05-p16-closeout-gate-ledger.md` ②节 / Deviations 5 / Consequences 2；
  id288 教训（1.21.1 污染后恢复正典树须再跑 1.20.1 对齐 .cache）；ADR-P2-4（runData 四参数先例）
- 基线：main `a6be2933`

## 0. 问题陈述

双节点 = 1.20.1 Forge + 1.21.1 NeoForge（Stonecutter 0.7，节点 projectDir = `mdk/versions/<node>`，
共享 buildscript `mdk/build.<loader>.gradle.kts`，settings.gradle.kts:26-36，vcsVersion=1.20.1-forge）。
两节点 runData 现指向**同一共享产物树** `mdk/src/generated/resources`：

- 1.20.1 写 `data/gt6/loot_tables/`（复数，vanilla 1.20.1 形）；
- 1.21.1 写 `data/gt6/loot_table/`（单数，vanilla 1.21/24w21a 数据包目录单数化改名），其余内容 byte-identical 1:1。

后果（P16 收口 ②节实录）：1.21.1 runData 每跑必搅脏 git（porcelain 9170 = 4585 D + 4585 ??）；
正典树形态被迫钉 1.20.1 形；恢复程序 = `git checkout -- mdk/src/generated` + `rm -rf .../data/gt6/loot_table`
+ **再跑一次 1.20.1 runData 对齐共享 .cache**（跳过对齐步则下一跑假报 written:4586）；
1.21.1 runData 二跑 written:0 在共享树下结构性不可达。现行门禁六步含两步纯恢复仪式（步 d/e）。

## 1. 机制求证（证据链）

### 1.1 本仓：输出路径是 run 配置的 CLI 参数，非 gradle 扩展属性

- `mdk/build.forge.gradle.kts:72-79`：`register("data")` 的 `programArguments.addAll("--output",
  sharedDir.resolve("src/generated/resources").absolutePath)`——共享锚点 `sharedDir = parent!!.projectDir`（:22）。
- `mdk/build.neoforge.gradle.kts:94-101`：同构，`--output` 同指共享树。
- 平台面为纯 CLI arg 的双源求证：
  - Forge 1.20.1：`tmp/refs/forge-api/forge-1.20.1/patches/minecraft/net/minecraft/data/Main.java.patch:5`
    （`accepts("output", "Output folder").withRequiredArg().defaultsTo("generated")`）；
    `DatagenModLoader.begin(..., path, ...)`（DatagenModLoader.java:35）→ `DataGeneratorConfig.path`（GatherDataEvent.java:73）
    → `makeGenerator` → `new DataGenerator(pathEnhancer.apply(path), ...)`（GatherDataEvent.java:97-98）——输出根 = CLI 值直通；
    Forge 官方 MDK 同款先例 `forge-1.20.1/mdk/build.gradle:106`（`args '--mod', ..., '--output', file('src/generated/resources/'), ...`）。
  - NeoForge：文档 "Command Line Arguments"（harvest `neoforge-docs-full/docs/resources/index.md:191`，
    versioned 1.21.4/1.21.8 同文）："`--output path/to/folder`: Tells the data generator to output into the given
    folder ... Defaults to file('src/generated/resources').getAbsolutePath()"。
- 本仓挂载面：共享树以 resources srcDir 挂进两节点 main（build.forge:158 / build.neoforge:194）；
  两脚本注释自带预埋伏笔「若两节点产物 diff → 切节点子目录 versions/<node>/src/main/generated，模板 :51 先例」
  （build.forge:154-157 / build.neoforge:192-193）。
- `file("...")` 在共享 buildscript 中解析到**当前节点 projectDir** 的先例：`gameDirectory = file("run/")`
  （build.forge:59 / build.neoforge:82），节点本地 run 目录由 `.gitignore:31 mdk/versions/*/run/` 收纳——
  节点本地路径落点的同机制实证。

### 1.2 本仓：共享 `.cache` 的键语义（跨节点跑为何假 written）

vanilla 1.20.1 `net/minecraft/data/HashCache.java`（tmp/vanilla-1.20.1 反编译）：

- `cacheDir = rootDir.resolve(".cache")`（:53）——cache 在**输出根内**，即共享树内（`.gitignore:24` 不入库）。
- cache 文件键 = `SHA1(providerId)`（:46-48）；providerId = `"vanilla/" + DataProvider.getName()`
  （DataGenerator.java:78）——**无节点标识**。
- `ProviderCache = (version, Map<Path, HashCode>)`（:180）：路径以输出根**相对形式**存储（load :204 / save :225）；
  `version` = `WorldVersion.getName()` 写进文件头（:51、:215）——两节点共享同一批 cache 文件但 version 头互异
  （1.20.1 vs 1.21.1）。
- `shouldRunInThisVersion`（:82-85）：version 头 ≠ 当前节点版本 → 全 provider 重跑。跨节点跑后对面的
  每 provider 必重跑。
- `shouldWrite`（:155-156）= `!Objects.equals(oldCache.get(path), hash) || !Files.exists(path)`——
  按 **Path 键**命中，内容 byte-identical 但目录名改带（复数↔单数）互不命中。
- `purgeStaleAndWrite`（:103-133）：Files.walk 输出根，**删除本跑各 provider cache 未认领的一切文件**
  （:117-133）——1.21.1 跑把正典复数带整带删除（porcelain 4585 D）的机械因；也是「单一输出根内双形态
  共存」结构性不可行的机械因。
- `written` 计数来源：purge 收尾日志 `"Caching: total files: ..., written: {}"`（:136-139）。

假 written 因果链（对齐 P16 实录）：1.21.1 跑 → 共享 .cache version 头全量改写为 1.21.1、复数带条目被
单数带取代；恢复正典树（git checkout + rm 单数带）后，1.20.1 跑：非 loot 路径 cache 命中零写入，复数 loot
路径 cache 未命中 → 4585 文件重写（内容与正典 byte-identical → porcelain 恒 0）——即「必须对齐跑」教训的
底层机制。附注：written 4586 = 4585 + 1 的 "+1" 归因未钉（W4 时 4582 = 4581 + 1，同构存在）；本裁决落地后
共享 .cache 不再跨节点，该现象整体消失，不立项追钉。

### 1.3 平台：1.20.1 provider 写不了单数；1.21 单数化是 vanilla 改名

- vanilla 1.20.1 `LootTableProvider.java:38`：`this.pathProvider = $$0.createPathProvider(
  PackOutput.Target.DATA_PACK, "loot_tables")`——**复数硬编码在构造器**。要 1.20.1 同形单数必须自写
  provider 副本覆写路径构造；且 1.20.1 运行时按 `data/<ns>/loot_tables/` 加载（24w21a 改名文档反证），
  单数产物在 1.20.1 runtime 是死文件。
- 单数化改名 = vanilla 1.21 / snapshot 24w21a（data pack version 45）：
  官方 snapshot 文 https://www.minecraft.net/en-us/article/minecraft-snapshot-24w21a ；
  minecraft.wiki https://minecraft.wiki/w/Java_Edition_24w21a ；
  第三方 changelog https://cmdgen.eufonia.studio/versions/?id=24w21a （"Folders using plural names have been
  renamed to match their registry"，breaking change）——三源一致（2026-09-05 访问）；本仓实证 =
  P16 收口步 c（4585 ?? 落 `data/gt6/loot_table/`，内容 1:1）。
- Stonecutter 社区惯例求证：官方风格模板 `tmp/harvest/stonecutter-template/build.forge.gradle.kts.txt:51`
  `sourceSets["main"].resources.srcDir("src/main/generated")`——在节点 projectDir 语境 = 
  `versions/<node>/src/main/generated` **per-node generated**。该惯例服务「单仓多版本各自发布」
  （每节点产物独立 tracked、独立打包）；本仓双节点共享**单一正典产物树**（review/1.20.1 runtime 单消费面），
  形态前提不同。

## 2. 候选对比与裁决

| 维度 | 候选1 输出目录按节点参数化 | 候选2 per-node tracked 产物树 | 候选3 接受 21.1 形落账 | 候选4 stonecutter 原生 per-version 目录 |
| --- | --- | --- | --- | --- |
| git porcelain 门禁稳定性 | 结构性满血：非正典节点不触 tracked 面 | 稳定（各跑各树） | 稳定 | 同候选2 |
| .cache 语义正确性 | 每树一份 cache，键/版本头树内自洽，二跑 written:0 可达 | 同左 | 单树单 cache：跨节点 version 头互异 → 对面 provider 恒重跑 + purge 互删对面形态带，结构性病根**不除** | 同候选2 |
| 一致性断言成本 | 需显式双树 diff 脚本（一次性 + 每轮秒级）；把「其余 byte-identical」从 porcelain 推断升级为显式断言（净增益） | 断言仍需（两 tracked 树漂移面常驻） | 不适用（1.20.1 runtime loot 死亡，主门禁节点功能倒退） | 同候选2 |
| 对现行六步的简化 | 恢复仪式两步（d/e）全删，无假 written 形态 | 同左 | 门禁可短但代价见下 | 同左 |
| 迁移成本 | 一次性：1 行 build.neoforge + 断言脚本 + 门禁文档；持续：0 | 一次性：git +70521 文件（~65936 重复）；持续：每次 datagen 变更双树 review、merge 冲突面×2、zip64 阈值×2、仓库体积×2 | 一次性：自写 LootTableProvider 副本；持续：1.20.1 runtime loot 门禁（p8 RCON destroy/loot 臂）倒退需撤门 | 同候选2 |
| 声明偏离继承 | 1.21.1 runtime loot 惰性（现状，不恶化） | 双节点 runtime loot 全活（收益当前无消费者） | 1.20.1 runtime loot 死亡（恶化，不可接受） | 同候选2 |

**裁决：采纳候选1（限定形态如下）**；候选4 的「节点本地 build 目录」落点并入候选1；候选2、候选3 否否。

- 正典节点（= settings `vcsVersion`，现 1.20.1-forge）：runData `--output` 保持
  `mdk/src/generated/resources`（tracked，正典产物唯一入库面，形态钉 1.20.1 形）。
- 非正典节点（现 1.21.1-neoforge）：runData `--output` 改节点本地
  `mdk/versions/<node>/build/datagen-output`（非 tracked；全局 `.gitignore:18 build/` 已覆盖，零 gitignore 改动）。
  该输出 = **验证产物**（证明 21.1 provider 链在 21.1 API 上编译、跑通、产出符合预期），非入库面。
- 正典不变量：所有 provider 的 JSON 仍零手写、由 1.20.1 runData 产出（既有红线不动）；共享树 resources
  挂载（build.forge:158 / build.neoforge:194）不动——1.21.1 runtime 继续消费正典 assets（模型/blockstate/lang
  双节点同名同形），loot 复数带在 1.21.1 runtime 惰性 = 继承现状的声明偏离（见 §5）。

### 被否案理由

- **候选2（per-node tracked 树，含候选4 的 tracked 形态）**：为「双节点 runtime loot 全活」这一当前无
  消费者的收益，先付 git +70521 文件（~65936 byte-identical 重复）、双树 review/merge 面翻倍、仓库体积
  翻倍的常驻成本；且两 tracked 树的内容漂移从「不可能（单树）」变为「需断言脚本常驻兜底」。重审触发器 =
  1.21.1 runtime 门禁需要活 loot 时（届时可只对 loot 带做 per-node 切分或发布期 overlay，属局部升级，不预建）。
- **候选3（正典树单数化）**：双重结构性否决——①1.20.1 `LootTableProvider.java:38` 硬编码复数，同形须自写
  provider 覆写（维护一个 vanilla 副本只为改一个字符串）；②即便产出单数，1.20.1 runtime 只读复数带，
  主门禁节点（1.20.1，p8 RCON destroy/loot 臂在跑）loot 功能倒退。「双形态共存于单一输出根」被
  `purgeStaleAndWrite`（HashCache.java:117-133 删未认领文件）结构性排除：21.1 跑删复数带、1.20.1 跑删
  单数带；靠非 datagen 拷贝步保双带 = 引入 build 机器 + git 常驻双带重复，成本形态即候选2。
- **候选4 独立成案部分**：stonecutter 模板 per-version generated（build.forge.gradle.kts.txt:51）的前提是
  每节点产物独立 tracked 独立发布，与本仓「单一正典树」前提不符 → tracked 形态即候选2，否；其节点本地
  build 目录惯例（`versions/<node>/build/`，与本仓 `versions/*/run/` 同构）可取，**并入候选1 落点**。

## 3. 新门禁序列（替代现行六步）

前置：实现卡 `tasks.p17-datagen-node-local-output` 合入后生效。

1. `./gradlew :mdk:1.20.1-forge:runData` 一跑 → written:N（新树/空 .cache 全量；否则 .cache 层增量）；
   porcelain 仅允许正典产物变更（datagen 变更波次内预期）。
2. `./gradlew :mdk:1.20.1-forge:runData` 二跑 → **written:0** + `git status --porcelain` 空（正典幂等）。
3. `./gradlew :mdk:1.21.1-neoforge:runData` 一跑 → **EXIT=0** + 零 `IllegalArgumentException` /
   零 `does not exist`（grep 计数 0/0）；输出落 `mdk/versions/1.21.1-neoforge/build/datagen-output`；
   tracked 面 porcelain 结构性空（仍照查，双保险）。
4. `./gradlew :mdk:1.21.1-neoforge:runData` 二跑 → **written:0**（1.21.1 幂等——现行门禁结构性不可达项，
   本裁决后首次可达）。
5. `python3 tools/datagen_tree_check.py` → 正典树 vs 21.1 输出树 1:1 byte-identical
   （忽略 `.cache/`、输出根 `version.json`（如现）；loot 带按 `loot_table`↔`loot_tables` 目录名映射后逐文件
   byte 比对，双侧文件计数相等）+ 终检 `git status --porcelain` 空。

形态：五步全为正向判据，零恢复仪式（旧步 c/d/e 的污染→恢复→对齐压缩为步 3+5）。裸 `:mdk:runData`
（未显式节点）= stonecutter 活动节点 = 1.20.1-forge = 正典生产者，语义不变；21.1 必须显式节点任务路径。

## 4. 实现卡（coder 分支）

- SLUG：`p17-datagen-node-local-output`
- SPEC：
  1. `mdk/build.neoforge.gradle.kts` data run：`--output` 改 `file("build/datagen-output").absolutePath`
     （`file()` 语义 = 当前节点 projectDir，与同脚本 `gameDirectory = file("run/")` 同机制先例）；
     注释钉：正典生产者唯一 = settings `vcsVersion` 节点（settings.gradle.kts:35 = 1.20.1-forge）；
     neoforge 节点产物 = 验证产物非入库面；第三节点出现时须按 vcsVersion 判正典（本卡不建，YAGNI 声明）。
  2. 新增 `tools/datagen_tree_check.py`：双树一致性断言（canonical 默认 `mdk/src/generated/resources`，
     node 输出路径参数化）；排除 `.cache/` 与输出根 `version.json`；loot 带单复数名映射后 byte 级逐文件
     比对 + 双侧计数核对；输出摘要（比较文件数/loot 带数/差异清单）；差异即非零 exit。
  3. 落地 `docs/adr/2026-09-05-p17-datagen-tree.md`（正文 = 本文档 state 副本，逐字）。
  4. 不改 `mdk/build.forge.gradle.kts`、不改 datagen Java 源、不改 `.gitignore`（`build/` 全局规则已覆盖
     新输出目录）。
- EVIDENCE：§1 全部 file:line / URL（本文档）。
- FILES_SCOPE：`mdk/build.neoforge.gradle.kts`（data run 段 + 注释）、`tools/datagen_tree_check.py`（新增）、
  `docs/adr/2026-09-05-p17-datagen-tree.md`（新增）。
- ACCEPTANCE：
  1. §3 新五步门禁全绿；步 4 的 1.21.1 二跑 written:0 首次实录入账（日志行摘录）。
  2. 步 3/4 前后 `git status --porcelain` 恒空（9170 污染形态绝迹，正反两向各验一次：21.1 跑前后、
     21.1 跑后再跑 1.20.1 二跑）。
  3. 断言脚本正例 exit 0；负例自证：对 21.1 输出树任一文件注入一字节改动 → 非 zero exit 且定位到该文件。
  4. `:mdk:1.20.1-forge:build` 与 `:mdk:1.21.1-neoforge:build` 绿（resources 挂载未破）；根 `:check` 与
     `:mdk:1.20.1-forge:test` 数值不受扰（build.forge 零触碰对照）。
  5. ADR 文档落地 `docs/adr/` 与本文档逐字一致（diff 为空）。

## 5. Consequences 与声明偏离

- **继承偏离（不变，显式声明）**：正典树 loot 复数带在 1.21.1 runtime 惰性（1.21 只加载 `loot_table/` 单数）。
  当前 1.21.1 门禁 = 编译 + 测试 + runData 跑通 + RCON 机器链，无 loot 运行时消费者，故无恶化。
  **重审触发器**：1.21.1 runtime 出现 loot 消费门禁时——届时选项 = loot 带局部 per-node 切分（候选2 限缩形）
  或发布期 overlay，另立 ADR，不预建。
- **治理规则入档**：`mdk/src/generated/resources` 唯一写者 = vcsVersion 正典节点（现 1.20.1-forge）；
  任何非正典节点 runData 不得指向 tracked 面。新节点准入检查单新增一条：确认其 buildscript `--output`
  落点符合本规则。
- **消失的坑**：id288 教训（污染后 .cache 对齐跑）与其假 written 形态（written:4586）整体作废——门禁文档
  与 P16 收口账中相关步骤由本 ADR §3 序列取代；`.cache` 不再承载任何跨节点语义。
- **+1 归因（written:4586 = 4585 + 1）未钉**：裁决后无操作影响，不立项；实现卡执行中如顺手可见差异文件，
  允许记录但不阻塞。
