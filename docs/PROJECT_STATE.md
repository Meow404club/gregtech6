# 项目状态（镜像）

> 权威数据在 MCP `gt6-brain` 的 state 里（`state_read()`）。本文件是人可读镜像，
> 由各 Agent 在重大状态变更时同步。模板如下，随进度填充。

## 当前阶段

`第 1 阶段：材料系统纯逻辑抽取`（模块卡已于 2026-08-28 产出，工程骨架待落地）

## 里程碑

- [ ] 第 1 阶段：材料系统纯逻辑抽取（无 MC 依赖，含单元测试）
- [ ] 第 2 阶段：注册表 + DataGen（DeferredRegister + 生成模型/配方）
- [ ] 第 3 阶段：BlockEntity + AbstractContainerMenu + Screen 框架
- [ ] 第 4 阶段：管线 / Cover / 多方块渲染（BakedModel 路线）

## 关键决策

（由 /architect 与 /researcher 持续写入，同步至此）

| 日期 | 主题 | 决策 | 证据 |
|---|---|---|---|
| 2026-08-28 | 基础设施 | 检索采用混合语义+BM25+RRF；原版 1.20.1 反编译源码入索引 | tools/README.md |
| 2026-08-28 | 材料系统边界 | GT6 材料系统核心在 `gregapi/oredict/`（模型）+ `gregapi/data/`（MT/AM/ANY/OP/TD 数据表），非旧资料所称 `gregtech/api/enums/` | gregapi/oredict/OreDictMaterial.java:48-52；gregapi/data/MT.java:43-47 |
| 2026-08-28 | 第1阶段拆解 | 材料 5 卡：foundation → model → (dataset ∥ ore-prefix) → graph | OreDictMaterial.java:209-330；OreDictPrefix.java:54-133；MT.java 4118 行 |
| 2026-08-28 | Gradle 骨架 | 单模块纯 java-library（Java17+JUnit5）先行，保留 gregapi 包名；第 2 阶段 composite/include 挂 NeoForge MDK | 第1阶段验收=无 net.minecraft import 的 gradle check |
| 2026-08-28 | MC 耦合剥离 | NBT→Serializer 接口；FluidStack/Enchantment/Achievement→字符串/枚举 ID；TextureSet/IIconContainer→名称引用；ItemStack 字段删除 | OreDictMaterialStack.java:31；OreDictMaterial.java:252-254,315,319 |
| 2026-08-28 | 红线-静态初始化 | 禁复刻 MT.java 巨型静态块（65536 上限）；数据分批注册；注册表可重置；GAPI.mStartedInit 改注册表状态机 | MT.java:45 作者自注；OreDictMaterial.java:153-155 |

## 第 1 阶段模块卡（2026-08-28）

> 材料系统实际位置：`gregapi/oredict/` + `gregapi/data/`。路径均已源码验证。依赖序：1 → 2 → (3 ∥ 4) → 5。

| # | slug | scope（做什么 / 不做什么） | GT6 源文件 | 依赖 | 风险 |
|---|---|---|---|---|---|
| 1 | gt-material-foundation | TagData/条件系统、单位常量、OreDictMaterialStack 算术；不做任何注册表 | gregapi/code/TagData.java:37-78；gregapi/oredict/OreDictMaterialStack.java:20-40；gregapi/data/CS.java（U/UD） | 无 | Stack 耦合 NBT（:31）→ 剥为 Serializer 接口 |
| 2 | gt-material-model | OreDictMaterial 模型 + createMaterial 注册（名称校验/ID/覆盖链）+ 组分加权化学计算；不做 MC 侧字段 | gregapi/oredict/OreDictMaterial.java:142-330（注册/字段）、:469-499（组分计算/化学式） | 卡1 | 耦合 GAPI.mStartedInit（:154）→ 注册表 open/closed 状态机 |
| 3 | gt-material-dataset | 元素周期表 + 全材料 + 反物质 + 别名组分批移植与保真校验；不做流体/物品生成 | gregapi/data/MT.java（4118 行）、AM.java（485 行）、ANY.java（158 行） | 卡2 | 65536 静态块上限（MT.java:45 自注）；TextureSet/TC/Enchantment 引用改字符串 ID |
| 4 | gt-ore-prefix | OreDictPrefix 模型：长前缀优先解析、材料量（U 单位）、命名模板、副产物；不做物品/配方注册 | gregapi/oredict/OreDictPrefix.java:54-150；gregapi/data/OP.java | 卡2 | 耦合 ItemStack/CreativeTabs/recipes/thaumcraft（:37-42）→ 全裁剪 |
| 5 | gt-material-graph | 12 类转换链（mTarget*/mTargeted*）查询 + 合金组分引用图；不改卡 2 文件本体 | OreDictMaterial.java:283-310、:260-268 | 卡3、卡4 | 图遍历需全量数据就绪；保真单测排最后 |

验收基线（每卡）：`gradle check` 全绿 + 卡内单测 + 源码 grep 无 `net.minecraft` import。
首个 coder 任务：**gt-material-foundation**（建 Gradle 骨架 + TagData/OreDictMaterialStack + 单测）。

> **合入记录 2026-08-28**：卡1 gt-material-foundation 审查通过合入 main（22e7532 e685d28 e42f069 → merge 2f3be03）。34 单测全绿、零 `net.minecraft` 命中；语义抽查与 1.7.10 原文逐字一致（含 equals 怪癖 :82/:84、U 家族截断除法）。NBT 剥离为 MaterialStackSerializer 缝隙，`a`/`i`/`m` 键语义保留，Phase-2 NBT 绑定须用 short 写 `i`（GT6 存档兼容）。OreDictMaterial/MT 为标注 placeholder 最小支柱，归卡2/卡3重写。

## 已知 Bug

（见 `state_read("known_bugs")`）
- 2026-08-28（已修复）：gt6-brain `state_update` 故障（`name 'json' is not defined`，根因 `tools/gt6_rag/search.py` 缺 `import json`；`state_read` 空表时侥幸不炸）。已修复并回填 state：decisions(5 条)/progress/tasks(5 卡)/known_bugs 四键读写验证通过。

## 已知陷阱（KG 摘录）

（见 `kg_query()`）

## 基础设施备忘

- 反编译脚本：`tmp/get_vanilla.sh`（1.20.1 客户端 + SpecialSource 重映射 + Vineflower）
- 全量重建索引：`cd tools && .venv/bin/python -m gt6_rag.index all`
- 新资料源登记：`tools/sources.json` → `refresh_index`
