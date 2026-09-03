# ADR 2026-09-03-p13-boiler-family-split：锅炉族总拆定裁定（四波/端口/注册家/常量作用域/爆炸纪律）

> 状态：accepted（已落地，五卡全合 42d6220e→1e78600e→e405bef3→cfcd9909→35ec9b21，
> 1148 测全绿）。本文为 state `decisions` 同名条目的成文整理（P13 收官转正，主会话）。
> 原 state 条目因 decisions 账本覆盖事故遗失，本文按 architect 汇总卡、五张派发卡与
> 四轮审查记录忠实重建，锚点原样保留。

## Context

- 研究卡 tmp.research.p12-boiler-family 四波 SPEC 已备：HU 前置+燃料图→火盒
  GeneratorSolid→BoilerTank 单方块→LargeBoiler 多方块（pattern API 可机械导出）。
- P12 蒸汽偏离还账四件强制前置（ADR 2026-09-02-p12-steam-proof-deviation）。
- 高风险面：爆炸语义（W3 四触发/W4 拆墙即炸）、常量双值并存（STEAM_PER_WATER
  200 引擎私有 vs 160 全局，CS.java:242 vs EngineSteam:58）。

## Decision

1. **四波串行**：W1=W1a 还账四件 ∥ W1b HU/蒸汽常量地基（零主文件交叠实证，先合
   先 rebase）→ W2 火盒五亚型一卡 → W3 BoilerTank 26 材质档 → W4 LargeBoiler
   5 变体；W1 全合才开 W2（还账关闭=蒸汽链可信前提）。
2. **端口**：25730-25734 / 25740-25744 顺延段。
3. **注册家**：GT6BurningBoxes（GT6Kinetics 自持 DR 形）/ GT6Boilers / GTMultiBlocks
   域内新段；GTBlockEntities append 共享 multi-mount BET；datagen 四 provider
   波内串行唯一写入者。
4. **常量作用域纪律**（承重教训⑤落地）：STEAM_PER_WATER_GLOBAL=160 新名避让
   引擎私有 200（EngineSteam:58），锅炉侧卡只准引 GLOBAL 名+四联断言机检钉。
5. **爆炸纪律**：HIGH 风险全复刻（W3 四触发/W4 拆墙即炸）；RCON 爆炸臂一律隔离
   站点（gt6world bbox）+事后 fill 清理+服务器存活断言；GTCEu 空烧爆炸勿引入=
   负断言。
6. **GUI census**=锅炉族全族无 GUI（火盒 :92 / BoilerTank :103 / LargeBoiler
   :150-166），P8 Menu 框架不启用写死。
7. **蒸馏水闭环不入 P13**=池（无 W1-W4 硬依赖）；Firestone/接触烫伤/WD.burn 前臂/
   FluidBed 粉尘行源=池或卡内声明。

## Deviations（否决备选）

- W1-W4 全并行——否决（GTFluids/GT6RecipeMaps/datagen/GTBlockEntities 共享
  append 面+还账前置语义依赖）。
- 锅炉直接实现卡跳过研究——否决（先研究后拍方案红线）。
- LargeBoiler 拆两卡（结构+业务）——否决（pattern API P12 已备机械导出，一卡可承载）。
- 蒸馏水闭环夹带 W1——否决（Distillery/Drying 无硬依赖，YAGNI）。

## Consequences

- 落地：五卡全合（42d6220e→1e78600e→e405bef3→cfcd9909→35ec9b21），零打回；
  合并态 1148 测全绿（根 205+mdk 943）。
- 正典蒸汽链 e2e 闭环：火盒→锅炉→管道→EngineSteam→KU→crusher+蒸馏水副产
  （RCON 双臂实拍）。
- KG：P13_boiler_family SPLIT_INTO×5 + DEPENDS_ON 链；OPENED_AT/CLOSED_AT main。
- 波内审查侧新纪律：gradle 门禁 --no-build-cache/--rerun-tasks 排 UP-TO-DATE/
  FROM-CACHE 假绿（W2 审查两次拦截）。
