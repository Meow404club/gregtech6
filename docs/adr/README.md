# ADR 索引与约定（Architecture Decision Records）

本目录收纳「GT6 现代复兴计划」的正式架构决策记录。此前 ADR 以字符串形状散落在
state `decisions` 账本（列表条目）与 `docs/PROJECT_STATE.md`、`docs/TODO.md` 的
内联提及中；自 P12 起（任务卡 p12-hygiene-style-adr）新 ADR 逐篇落盘本目录。
**历史 ADR 不回填**（留池）：账本原文仍是权威来源，下表仅列条目占位。

## 约定

- **命名**：`YYYY-MM-DD-<slug>.md`，日期取决策落账日（state `decisions` 条目的
  日期），slug 用卡名/主题短横线小写（如 `2026-09-01-p10-cover-item-intercept.md`）。
- **四节结构**（每篇必须有，节名固定英文，正文语言随源）：
  1. **Context** —— 决策面对的事实与约束（上游/本仓/1.20.1 证据，`文件:行号` 锚点）；
  2. **Decision** —— 裁定本身（范围写死，含"恰量"边界）；
  3. **Deviations** —— 声明偏离（与上游语义的显式差异，每条带证据锚点）；
  4. **Consequences** —— 落地后果（提交/门禁/测试计数/遗留与后续卡指针）。
- **锚点纪律**：`文件:行号`、类名、常量名、上游 MTE id 等技术锚点一律原样保留，
  整理（转正成文）不得改写决策内容——只允许重排结构与措辞衔接。
- **冻结面扩面**：凡解除既有冻结面（如 ICover/ICoverableTE）的决策必须在 Decision
  节写明 sanctioned 范围与理由，并在 Consequences 节声明"唯一一次"或后续复用规则。
- **权威顺序**：本目录成文 ADR > state `decisions` 账本条目 > docs 内联提及；
  三者冲突时以本目录为准并回写勘误。

## 已成文

| 日期 | 文件 | 主题 |
| --- | --- | --- |
| 2026-09-01 | [2026-09-01-p10-cover-item-intercept.md](2026-09-01-p10-cover-item-intercept.md) | ICover item 族冻结面扩面：恰八钩解冻 + 三宿主门 + TileEntityOven 侧感知 wrapper |
| 2026-09-02 | [2026-09-02-p12-ghost-render-translucent.md](2026-09-02-p12-ghost-render-translucent.md) | RenderHighlightEvent 瞬态例外扩面：整面半透明 + 绿红分色（三约束保持） |
| 2026-09-02 | [2026-09-02-p12-jei-dependency.md](2026-09-02-p12-jei-dependency.md) | 首个第三方 mod 依赖：JEI 接入（版本/坐标/类路径/信息页，含砖数 26→25 勘误附录） |
| 2026-09-02 | [2026-09-02-p12-rotation-carrier.md](2026-09-02-p12-rotation-carrier.md) | 旋转载体：零新 EnergyType，复用 RU/KU+push 握手，Axle 邻接递归 |
| 2026-09-02 | [2026-09-02-p12-fluid-engine-chain.md](2026-09-02-p12-fluid-engine-chain.md) | 流体罐引擎链总 ADR：范围/三波九卡/蒸汽产源/声明偏离（含桶 GUI 终裁） |
| 2026-09-02 | [2026-09-02-p12-steam-proof-deviation.md](2026-09-02-p12-steam-proof-deviation.md) | 蒸汽四防销毁链：声明偏离 + P13 强制还账四件（POWER_CONDUCTING 实证；2026-09-03 O1 补账转正） |
| 2026-09-03 | [2026-09-03-p13-barrel-base-unfreeze.md](2026-09-03-p13-barrel-base-unfreeze.md) | TileEntityBase08Barrel P5 冻结面恰量解冻：tick 两查+gasProof 覆写点+allowFluid 最小面（还账四件随卡 ADR） |
| 2026-09-03 | [2026-09-03-p13-hu-energy-face.md](2026-09-03-p13-hu-energy-face.md) | HU 能量面零新 EnergyType：复用 push 握手，发射面仅顶面（原 state 条目因账本覆盖事故按 architect 卡重建成文） |
| 2026-09-03 | [2026-09-03-p13-boiler-family-split.md](2026-09-03-p13-boiler-family-split.md) | 锅炉族总拆定裁定：四波串行/端口/注册家/常量作用域纪律/爆炸纪律（同上重建成文） |
| 2026-09-03 | [2026-09-03-p15-3-chisel-fork-discipline-r1.md](2026-09-03-p15-3-chisel-fork-discipline-r1.md) | ADR-P15-3 r1 成文：chisel 分叉三级优先序（下沉 gregapi > swap 表 > //?）+ javadoc 禁独占 //? 分支 |
| 2026-09-03 | [2026-09-03-p15-10-monotone-shrink-gate-r1.md](2026-09-03-p15-10-monotone-shrink-gate-r1.md) | ADR-P15-10 r1 成文：1.21.1 单调收缩门禁口径钉 compileJava 红文件数；v1=W2 收官重测为真基线 |
| 2026-09-05 | [2026-09-05-p16-blockstate-prop-identity.md](2026-09-05-p16-blockstate-prop-identity.md) | ADR-P16-2：BlockState Property 单一 owner（1.21.1 StateHolder Reference map 按 == 查 key；GTBlockProperties holder+类内别名） |
| 2026-09-05 | [2026-09-05-p16-closeout-gate-ledger.md](2026-09-05-p16-closeout-gate-ledger.md) | P16 收口数据文档：四判据门禁总账 + 11 卡表 + 21.1 运行时 delta 三案 + P17 池移交 |
| 2026-09-05 | [2026-09-05-p17-datagen-tree.md](2026-09-05-p17-datagen-tree.md) | ADR-P17-1：datagen 共享树跨节点裁决 = 输出目录按节点参数化（正典节点共享 tracked 树不变；21.1 输出节点本地 build/；五步门禁取代六步） |
| 2026-09-05 | [2026-09-05-p17-machine-io-adjudication.md](2026-09-05-p17-machine-io-adjudication.md) | P17①：21.1 机器 IO 两 delta 单根因（wiring 漏注册 DRYER/DISTILLERY_BE）+ IDENTICAL-22 逐条裁决（19 fix/1 permanent/2 defer，残集 IDENTICAL-3） |

## 占位（历史 ADR，未回填，权威原文在 state `decisions` 账本与 docs/PROJECT_STATE.md）

- ADR-P2-*（第 2 阶段：注册桥幂等、材料条件系统、datagen 管线等五卡）
- ADR-P3-*（第 3 阶段：BE/Menu 框架四卡，含 2026-08-30-p3-fullprefix-registration-universe）
- 2026-08-30-p4-*（第 4 阶段拆卡九条：recipe-core / fluid-pipes / machine-oven /
  render-foundation / multiblock-framework / cover-core / fluid-barrel 等）
- 2026-08-31-p6-cokeoven-processing（CokeOven 加工八裁定）
- 2026-08-31-p8-prefixblocks（storage 前缀方块 A/B 拆卡）
- 2026-08-31-p8-machine-closeout（机器族收尾 M1/M2）
- 2026-09-01-p9-redstone-hooks（cover 红石钩框架卡）
- 2026-09-01-p9-tool-crowbar（正式 crowbar + 工具 id 入口）
- 2026-09-01-p9-wire-family（线缆全谱系 W1/W2）
- 2026-09-01-p9-render-c-oven-overlay（C 档 oven 动态渲染）
- 2026-09-01-p9-render-d-formed-look（D 档成型态渲染）及其 erratum 勘误
- 2026-09-01-p9-redstone-cover-emitter（Redstone Emitter 盖）
- 2026-09-01-p10-tools-covers-split（P10 工具族 + cover 残余拆卡）
- 渲染路线 ADR（BakedModel 快照制 / BEWLR-TESR 全仓禁令，散见于 P4/P5 卡面与
  javadoc 引用）

后续卡若需引用某条历史 ADR 的完整原文，以 `state_read("decisions")` 与
`docs/PROJECT_STATE.md` 对应阶段段为源，按本 README 四节约定转正成文。
