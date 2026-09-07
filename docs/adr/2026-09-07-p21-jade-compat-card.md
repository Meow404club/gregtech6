# ADR 草稿：P21 Jade 兼容卡拆卡裁定（2026-09-07）

状态：Accepted（P21 收官主会话正典化 2026-09-07）
研究依据：state key=`tasks.p21-research-jade`（Jade 双腿 harvest 树 file:line 全证）
任务卡：state key=`tasks.p21-arch-jade`

## Context

用户点名要 Jade 兼容（看向方块 tooltip）。GT6 1.7.10 上游无 WAILA 数据面（研究卡 census：全仓仅 5 处字面提及，无 provider），功能对等参照 = GTCEu Modern `integration/jade/GTJadePlugin.java:21-118`。本仓 Stonecutter 单库双腿（1.20.1 Forge 正典 + 1.21.1 NeoForge）。

## 裁定

### D1 单卡（否决双卡）

研究建议"单卡可落"采纳。新目录 + build 热点小的证据成立；唯一的双卡候选（流体显示）因 FluidTankGT fluid capability 缝未证（P3 只缝 ITEM_HANDLER）明确**入池不拆**，不构成本卡第二张。卡内阶段序：依赖接线 → 插件骨架 → provider 对 → 冒烟验收。

### D2 展示内容 v1 范围

**进本卡**（服务端 appendServerData 写 CompoundTag + 客户端 appendTooltip 读渲染的 provider 对）：
- 进度条 mProgress/mMaxProgress（TileEntityBasicMachine.java:227），运行态着色 mSuccessful/mActive/mRunning（GTCEu WorkableBlockProvider.java:37-45/:48-89 为直接模板）；
- 能量行 mEnergy + 输入带 mInputMin/mInput/mInputMax（:226，注明 RU/KU 载体）；
- 错误行 ERROR_MESSAGE（TileEntityBase01Root.java:88，非空才显示）；
- 多方块成形态（TileEntityBase10MultiBlockMachine.java:112 域 isCorrectForm 类状态，布尔文本行）。

**不进本卡**：
- 流体显示——capability 缝未证，入池（探针先行的独立卡候选）；
- 库存概览——零代码预期：Jade universal ItemStorageProvider 经 P3 已缝的 ITEM_HANDLER capability 自动显示（jade-1201 CommonProxy.java:361 JadeForgeUtils.fromItemHandler），runClient 目视核验即可，不写代码；
- 喷漆色——Paintable 两卡在途，不在本卡；
- lang 键——v1 一律 Component.literal（进度条用 IElementHelper.progress 无文本、能量/错误行英文 literal），i18n 入池；避免触 GT6EnUs/GT6ZhCn（整备批 A 卡地盘）。

### D3 插件隔离形态

新包 `gregtech6.jade`（mdk/src/main/java/gregtech6/jade/）：GT6JadePlugin（`@WailaPlugin` 裸注解 + IWailaPlugin，仿 GTCEu GTJadePlugin.java:21-22）+ 1-2 个 provider 类（泛型参数取 BlockAccessor，体内 instanceof+cast 到本仓 BE 类——双腿注册签名都能吃的最窄公共形）。硬规则：**mod 自身代码零静态引用 snownee.jade**（Jade 经 FMLLoadCompleteEvent 扫描发现：jade-1201 util/CommonProxy.java:210-229 / jade-1211 :490-509；Jade 缺席则插件类永不 classload，运行时可选天然成立）。provider 对 BE 字段只读公开成员，禁改 BE 文件。

### D4 build 接线

- 坐标（Modrinth maven，research 卡双源实证）：forge 腿 `maven.modrinth:jade:11.13.3+forge`（modCompileOnly+modRuntimeOnly，mdk/build.forge.gradle.kts:102-109 JEI 段同构紧后；repositories 加 https://api.modrinth.com/maven 于 :25-30）；neoforge 腿 `maven.modrinth:jade:15.10.6+neoforge`（compileOnly+runtimeOnly，build.neoforge.gradle.kts:152-154 紧后；repositories :26-31）。降级预案 = GTCEu 旧钉 11.6.3。
- 钉值：根 gradle.properties `jade_version = 11.13.3+forge`（:11 jei_version 同式）；mdk/versions/1.21.1-neoforge/gradle.properties `jade_version=15.10.6+neoforge`（:13 同式，节点遮蔽根值机制已实证）。
- 源码分叉：预期**零 chisel**——只覆写 appendTooltip/appendServerData + 以（provider 实例， BE class）调注册（1.20.1 IWailaCommonRegistration.java:20 vs 1.21.1 :19 参数放宽，窄形双腿合法）；若编译实证出现分叉，只许按研究卡 api_diff_table 三点（IBlockComponentProvider 基接口 / registerBlockDataProvider 参数宽 / 1.21.1 shouldRequestData+Accessor codec）落 `//?`，逐点记录。

### D5 mods.toml optional 条目：加，GTCEu 姿势 + 一处声明偏离

GTCEu 1.20.1 templates/META-INF/mods.toml 实证（2026-09-07 raw fetch）：`modId="jade", mandatory=false, ordering="NONE", side="BOTH"` 照抄进两模板（mdk/src/main/templates/META-INF/mods.toml :24 后；neoforge.mods.toml :28 后，mandatory→`type="optional"` 按该模板注释约定）。**偏离**：GTCEu versionRange 用 `${jade_version}` 属性驱动；我们改模板内硬编码地板区间（forge `[11,)` / neoforge `[15,)`）——理由：模板本就是每腿一份（GTCEu 共享模板才需要属性），且我们的 jade_version 钉值带 `+forge/+neoforge` 后缀（Modrinth maven 版本方案），进 TOML maven 区间是未测边界，禁猜。

### D6 验收面（诚实口径）

**自动可断言**：
1. 双腿 compileJava 绿（零 chisel 的编译实证收口）；
2. 双腿无 Jade runServer（默认类路径态）：boot 到 Done + 零 ERROR——"无 Jade 运行时不炸"（NCDFE 防线）；
3. 双腿挂 Jade runServer 冒烟（forge=modRuntimeOnly / neoforge=runtimeOnly 后 runServer）：日志含 Jade 自身插件发现行 `Start loading plugin` + `gregtech6.jade.GT6JadePlugin`（1201 措辞 "at" :225 / 1211 "from" :505），且零 `Error loading plugin`（:236/:532 双腿同串）——证明发现+register() 真实运行；`register()` 服务端无条件调用双腿已证（:230/:516），专用服务端冒烟成立（Jade 自身 mods.toml side=BOTH）；
4. root+mdk 全测不回归（Jade 不进 test 类路径——modCompileOnly/compileOnly 不传导进 test 配置，预期零测试改动）。

**如实不可自动断言**：tooltip 内容正确性是客户端渲染面，RCON 无玩家无准星不可达。进度条/能量/错误行的目视核验 = runClient 挂 Jade 手工冒烟，入池项；本卡自动验收以上述 1-4 为终面，不冒充。

## 在途冲突与合并序

逐卡核对（files_scope）：
- 整备批 A（i18n seam：GT6EnUs/GT6ZhCn）：零交集（本卡零 lang 键）；
- 整备批 B（stoneblocks 16-item：GTStoneBlocks/GT6BlockStates stone 段/GT6LootTables/GT6ItemModels）：零交集；
- 整备批 C（modularui test sourceSet：third-party/）：零交集；
- Paintable card_A（TileEntityBasicMachine/TileEntityOven+GTModelProperties）：本卡只读其字段不改文件，零交集；card_B（GTClientHandlers/GT6BlockStates machine 段）：零交集。
build 脚本/模板/gradle.properties 四热点本卡独占（四在途卡全不触）。**合并序：无约束，可并行；各自 rebase 最新 main 即可。**

## 后果

- 世界 tooltip 首次具备机器进度/能量/错误/成形态显示（超上游 1.7.10——上游从无此面，声明为移植增强，对齐 GTCEu 现代形）。
- 流体显示、i18n、目视验收池项化，边界显式。
- 风险承接：1.20.1 SRG jar 重映射沿用 JEI P12 机制，若编译遇 SRG 泄漏按研究卡预案降级 fg.deobf 形（卡内记录，不静默换）。
