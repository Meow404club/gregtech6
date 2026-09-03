# ADR-P15-10 r1：1.21.1 单调收缩门禁（修订版 r1）

- 日期：2026-09-03（r1 修订随 W2 合入段 p15-stonecutter-skeleton 落盘成文）
- 原文：state `decisions` 账本 ADR-P15-10（1.21.1 失败清单单调收缩——口径本版钉死）；
  修订全文自 state `decisions.2026-09-03-p15-m1-gate.adr_revisions.ADR-P15-10_r1` 读出成文。
- 权威顺序：本目录成文 ADR > state 账本条目（docs/adr/README.md 约定）。

## Context

路线 A′ 的核心风险是 chisel 侵入面失控。密度 POC 给出的 31.8% 语义分叉率属人工分档口径
（tier2 hunk 分档依赖普查脚本规则），不可一条命令复现；1.21.1 编译失败清单（156 文件 /
2,652 错误，三跑计数一致）才是 javac 硬信号。M1 裁决把门禁口径钉到可复现的硬信号上，
并要求 W2 合入段收官时重测出「真基线」（swap 初版 + test 接线 + mods.toml 双模板落地后），
供 W3-W5 每卡收官对照。

## Decision

1. **门禁口径 = 1.21.1 节点 `:mdk:1.21.1-neoforge:compileJava` 红【文件数】**
   （全量诊断协议：`--no-build-cache` + `-Xmaxerrs 100000` + 长超时按行提取；
   非语义 hunk 数、非错误数）。红文件数可复现：一条命令、javac 硬信号。
2. **基线**：
   - v0 = 156 文件 / 2,652 错误（2026-09-03 骨架 POC 实测，遥测基线；
     state `tmp.poc.p15-skeleton.failure_list_v0`）；
   - **v1 = W2 收官重测（swap 初版 + test 接线 + mods.toml 双模板落地后）= M3 门禁真基线**
     （state `tmp.poc.p15-skeleton-v1`）。
3. **单调性**：W3-W5 每卡收官红文件数较上卡**严格递减**，回升即门禁失败；
   **错误数与 compileTestJava 测试面清单 = 遥测，允许非单调**（修 import 可能暴露深层错误；
   测试面不入门禁，由 M4 首绿 968 对位兜底）。
4. **JEI 复测只作遥测不入门禁**（JEI 是 W5 落地，届时 1.21.1 接线会改变编译面）。
5. **再武装**：连续两卡红文件数不降 = 备选 C（1.21.1 单独分支）触发条件③成立
   （decisions.2026-09-03-p15-m1-gate re_arm_conditions C1）。

## Deviations

- v0 基线的错误数（2,652）与文件数（156）来自 W1 骨架段三跑实测；v1 起错误数降为遥测
  指标——属口径收紧，非数据偏离。
- 门禁量测口径与密度 POC 的 tier2 hunk 分档并存：后者仅用于 M1 路线裁决复盘，不入门禁。

## Consequences

- 每卡（W3 起）收官动作增一项：跑全量诊断协议 → 红文件数落 state → 对照上卡。
- W2 收官 v1 实测值见 state `tmp.poc.p15-skeleton-v1`（dict：red_files/errors/breakdown/note）。
- 门禁失败（回升）或连续两卡不达（C1）的处置按 decisions.2026-09-03-p15-m1-gate 执行：
  单卡失败=卡内返工；C1 成立=通知 architect 触发备选 C 评审。
