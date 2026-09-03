# ADR-P15-3 r1：chisel 注释分叉纪律（修订版 r1）

- 日期：2026-09-03（r1 修订随 W2 合入段 p15-stonecutter-skeleton 落盘成文）
- 原文：state `decisions` 账本 ADR-P15-3（chisel 注释只做版本分叉不夹带重构——原文维持有效）；
  修订全文自 state `decisions.2026-09-03-p15-m1-gate.adr_revisions.ADR-P15-3_r1` 读出成文。
- 性质：修订版（r1）。在原文「不夹带重构」之上增补分叉前三级优先序与实现纪律。
- 权威顺序：本目录成文 ADR > state 账本条目（docs/adr/README.md 约定）。

## Context

P15 路线 A′ = 单代码库 + `//?` 条件注释分叉（1.20.1 Forge / 1.21.1 NeoForge 双节点，
Stonecutter 0.7）。密度 POC（docs/adr/p15-poc-chisel-density.md，commit e08de85b）实测
mdk/src/main/java 195 文件中 62 文件（31.8%）含语义 hunk；其中 28 文件属机械符号带
（ForgeRegistries / 内联全限定名 / DeferredRegister / RegisterEvent，合计 187 行），可由
0.7 per-node swap/replacements 参数化消化而不写任何 `//?`；纯结构级 34 文件（17.4%）才是
真正必须分叉的面。M1 裁决（decisions.2026-09-03-p15-m1-gate）据此定分叉纪律的完整优先序，
并从最坏文件实跑（TileEntityBase01Root）得到注释处理器的边界实证。

## Decision

1. **（原文维持）chisel 注释只做版本分叉，不夹带重构。**
2. **分叉前三级优先序**（每处版本差异按序裁决，能落前级不落 后级）：
   - ① 能下沉 gregapi 不下叉（接口级差异做 seam，非新抽象层，不违 ADR-P15-6）；
   - ② 能走 0.7 swap/replacements 表不写 `//?`。swap 表落 `mdk/stonecutter.gradle.kts`
     节点参数区（`stonecutter parameters { replacements { … } }`，0.7 源码
     StonecutterControllerImpl.parameters → StonecutterBuildProperties 每节点求值）。
     每条目纪律：**双侧编译证据** + **1.20.1 侧预处理输出零变化** + **锚定全限定名**
     （带 `(?![A-Za-z_])` 类边界断言或字面量转义），**禁裸宽正则**，**禁夹带语义重构**；
   - ③ 仅结构面（控制流/签名/字段/多语句）用 `//?`。
3. **实现纪律（javadoc 陷阱）**：javadoc/块注释不得独占 `//?` 分支——禁用侧必须以
   `/*…*///?` 包裹（0.7 `CommentUtil.isCommented`：全注释作用域被当"已包裹形态"，
   启用侧反被解注释 → 语法损坏；密度 POC 实证，CommentUtil.kt:10）。指令注释永不清除，
   状态确定可逆。

## Deviations

- 无上游语义偏离；本 ADR 为仓内工程纪律（上游 Stonecutter 0.7 的 DSL 语义按
  tmp/harvest/stonecutter-src-07/ 为 ground truth）。
- swap/replacements 的方向语义为 0.7 固有（`ReplacementContainerImpl`：direction=true 走
  from→to，false 走 to→from 的对称可逆），本仓以「反向检索串在 1.20.1 源零出现」或
  「regex 条目 reverse() 钉永不匹配哨兵」把它约束成事实单侧，属对既有机制的用法约束，
  非偏离。

## Consequences

- W2 合入段交付 swap 表初版（28 文件机械带 + tier1 import 带），证据随
  state `tmp.poc.p15-skeleton-v1` 落账。
- W3 分叉波实质 = 34 结构级文件；capability 族（LazyOptional 31 行 + ForgeCapabilities
  40 行）按优先序①先做 seam 下沉评估。
- W3+ 每卡 checklist 增两行：javadoc 不独占 `//?` 分支；新版本差异先过三级优先序。
- 门禁与再武装条件见 ADR-P15-10 r1；swap 表反复修补 ≥3 轮仍不能双侧稳定编译 = 备选 C
  触发条件②（churn 不可接受）实证。
