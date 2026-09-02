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
