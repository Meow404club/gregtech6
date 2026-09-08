# ADR：KubeJS 魔改适配三档路线（tier-a/b/c＋触发条件＋1.21 腿优先）

日期：2026-09-08 ｜ 前置：用户裁定 2026-09-08（AGENTS.md 常设条款正典化，提交 dab8d735＋1a0c9ca3） ｜ 评审基线 main=1a0c9ca3（纯 docs 落地，零 mdk 触碰） ｜ 决策源 state research.p25-r-kubejs-adaptation-seam ｜ 条款落点 AGENTS.md:90-96（任务卡规范 §四）

- 状态：Accepted（P25 收官主会话正典化 2026-09-08）

## 0. 摘要

本仓对 KubeJS 的适配路线分三档：tier-a＝坚持 datapack 原生形（现状，零成本，默认档）；tier-b＝RM 配方图 datapack JSON 直灌（P26 候选）；tier-c＝GTCEu 式内嵌 kjs 绑定模块（按需，1.21 腿优先）。每张移植卡的 SPEC 必须声明 KJS 适配考虑面（AGENTS.md:90-95 模板句），「不考虑」也算声明，禁止无声缺失。本 ADR 把研究卡结论与 AGENTS.md 常设条款升格为决策记录，含 tier-b/tier-c 触发条件与 1.21 腿优先理由。

## 1. Context

### 1.1 本仓暴露面（research 卡 local_exposure）

- **天然可改域（现状即 tier-a 合规）**：datagen 全部产物——crafting JSON（mdk/src/main/java/gregtech6/datagen/GT6CraftingRecipes.java:34-54）、tag、loot、advancement、lang＝KubeJS ServerEvents.recipes/tags 天然可改面。
- **KubeJS 不可见域**：GT6RecipeMaps 13 张运行时 RM（mdk/src/main/java/gregtech6/recipes/GT6RecipeMaps.java:154-210，全 volatile 静态字段＋内存集合，不走 RecipeManager/JSON）——KubeJS recipe 事件只认 RecipeManager；GTMaterialItems 注册面同理。
- **既有灌入缝（tier-b 活先例）**：GT6CokeOvenTagListener `TagsUpdatedEvent`→RM 重建（GT6CokeOvenTagListener.java:54；GT6RecipeMaps.java:43）。

### 1.2 双腿版本线与分叉面（research 卡 capability_face）

- 1.20.1 forge＝KubeJS 6（2001.6.5 线；GTCEu 钉 2001.6.5-build.16，tmp/refs/gtceu-modern/gradle/forge.versions.toml:11；module `kubejs-forge` :77；architectury 在 kjs bundle :125）。
- 1.21.1 neoforge＝KubeJS 7（2101.7.2 线；GTCEu 1.21 分支钉 build.336；architectury 不再 bundle）。
- **addon 双腿唯一源码级分叉＝plugin 类声明**：6.x `extends KubeJSPlugin`（vendored GregTechKubeJSPlugin.java:137）→7.x `implements`（harvest gtceu-kjs-121-plugin/GregTechKubeJSPlugin.java:129 实证；7.0 wiki 破坏面）。核心 hook 名跨 6/7 稳定；maven 坐标 `-forge`/`-neoforge` 后缀。
- 注册机制＝mod resources 根 `kubejs.plugins.txt`（非 ServiceLoader、不走 mod bus；KubeJSPlugin.java javadoc＋ExampleKubeJSAddon 双源）。

## 2. Decision

### 2.1 三档定义与触发条件

| 档 | 内容 | 成本 | 触发条件 |
| --- | --- | --- | --- |
| tier-a | 内容走 datapack 原生形（配方 JSON/tag/loot）；新内容默认档 | 零（现状已合规） | 无（默认） |
| tier-b | RM 配方图 datapack JSON 直灌：`SimpleJsonResourceReloadListener` 读 `data/gt6/recipe_maps/*.json`，复用 GT6CokeOvenTagListener 的 TagsUpdatedEvent 缝 | 纯 vanilla API、零双腿分叉、无 kjs 编译依赖、支持 /reload 热改、pack 作者零学习成本；比 kjs 插件便宜一个量级 | **P26 候选**：RM 出现真实的 datapack 行级增删改需求时立项 |
| tier-c | GTCEu 式内嵌 kjs 包：`KubeJSPlugin`＋RM RecipeSchema 组＋`GT6Materials`/`GT6RecipeMaps` bindings（GTCEu 参考实现 ~48 文件/主插件 699 行，四机制＝RegistryInfo/registerRecipeSchemas/registerRecipeComponents/registerBindings） | 估 10-15 文件 2-3k 行；依赖 modCompileOnly 零传染（dependencies.gradle:43-46 形，不 jarJar 不 modApi） | **P26+ 按需**：出现真实脚本化需求（条件生成/跨 RM 编排），行级 tier-b 不可表达时 |

- **tier-b 代价声明**：只支持行级增删改、无脚本表达力——触发条件成立前不预付。
- **tier-c 1.21 腿优先理由**：KubeJS 7 的 json-schema 注册免依赖 mod jar（7.0 新方向）；GTCEu 1.21 分支 `implements` 形已是现成先例；若双腿都要，以 7.x 形为设计锚、6.x 腿按 extends 分叉单点适配。

### 2.2 SPEC 强制声明（常设，AGENTS.md:90-95 正典化）

每张移植卡 SPEC 必含模板句：`KJS面声明：本卡产出=〈datapack域|注册面|RM运行时配方图|无KubeJS面〉；datapack域=天然可改零适配；注册面/RM=〈defer至kjs绑定卡|已由tier-b-datapack-RM缝覆盖〉`。三分类：①内容走 datapack 原生形优先；②注册面（物品/方块/流体）至少留可脚本化缝声明；③RM 类内容给绑定方案或显式 defer。

## 3. Deviations（边界声明）

- 本 ADR 是**路线决策**非实现卡：本仓今日零 kjs 代码落地，tier-b/tier-c 均未立项——立项时各自走先研究后实现。
- GTCEu 的 namespace 参考（TagUtil.java:32 硬编码 forge）是单源其本树口径；本仓若做绑定，namespace 以本仓 `MATERIALS_NAMESPACE` 双腿缝为准（decisions.p24-tool-system-tag-strategy 已双源实证）。
- 自定义配方类型脚本化必须经 `registerRecipeSchemas`（plugins.txt 机制），无旁路——tier-c 估行数已含 schema 组。

## 4. Consequences

- **正面**：适配成本前置显形（每卡一行声明）；tier-b 缝（JSON 直灌＋TagsUpdatedEvent）已有活体先例，立项即零考古；双腿分叉面收敛到 plugin 类声明单点。
- **义务**：主会话派卡时 SPEC 缺 KJS面声明＝卡不合格；P26 规划时把 tier-b 列入候选清单。
- **边界/遗留**：tier-c 的 13 RM schema 全集清单归 tier-c 研究卡；KubeJS 版本钉值（2001.6.5-build.x/2101.7.2-build.x）在立项时按 Modrinth 最新重钉，本 ADR 不冻结版本号。
