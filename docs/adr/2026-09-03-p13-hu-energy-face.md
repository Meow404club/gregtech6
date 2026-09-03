# ADR 2026-09-03-p13-hu-energy-face：HU 能量面零新 EnergyType（复用 push 握手）

> 状态：accepted（已落地，merge 1e78600e / e405bef3 / cfcd9909 / 35ec9b21）。
> 本文为 state `decisions` 同名条目的成文整理（P13 收官转正，主会话）。原 state
> 条目因 decisions 账本覆盖事故遗失，本文按 architect 汇总卡（tasks.p13-arch-boiler-family）、
> 派发卡与审查记录忠实重建，锚点原样保留。

## Context

- TD.Energy.HU TagData 已在（TD.java:102）但本仓零生产者零消费者——P13 锅炉族
  是首个 HU 能量面。
- 上游火盒族全发 TD.Energy.HU（Loader 行表 NBT_ENERGY_EMITTED 实读），发射面=
  仅顶面（GeneratorSolid:270）。
- 本仓 ITileEntityEnergy 14 方法面（P7）+push 握手 Util（:244-289）已备；
  crank（P12）已有 RU 同形先例（GTCrankBlockEntity:166）。

## Decision

零新 EnergyType：HU 完全复用既有 push 握手——

1. 令牌=TD.Energy.HU 既有 TagData（TD.java:102）；
2. 产源发射面=仅顶面（GeneratorSolid:270 isEnergyEmittingTo verbatim）；
3. 接收=任意面无侧门（BoilerTank :249 对位）；
4. 发射式=`Util.emitEnergyToNetwork(TD.Energy.HU, 1, min(mRate, mEnergy), this,
   adjacency)` 仅当缓冲≥mRate（上游 :104 同式，GTCrankBlockEntity:166 RU 同形）；
5. 无线缆邻接直供正典（HU 无线缆族，邻接即达）；
6. RCON 验收走既有 `/gt6energy type HU`（P8 :297 已支持）；
7. demand/recommended=mOutput/2（W3 :253-254）。

禁：新 EnergyType / HU 线缆族 / 自扩 ITileEntityEnergy 冻结面。

## Deviations（否决备选）

- 新 HU EnergyType+专用网络——否决（上游即无）。
- HU 线缆族——否决（上游无此物）。
- 复用 FE/EU 类型——否决（语义混淆）。
- Turbine 直产 EU 路线（GTCEu Modern 形）——否决（GT6 正典=HU 中间态）。

## Consequences

- 落地：HuEnergyHandshakeTest 5 测零生产代码（1e78600e）+火盒 97 块全发 HU
  （e405bef3）+锅炉 doEnergyInjection 消费端（cfcd9909）+大锅炉 HeatTransmitter
  转推（35ec9b21）；KG 边 GT6_HU_energy_face REUSES push_handshake。
- 门禁：RCON `/gt6energy type HU` 链 25731/41 两遍 [0,0]；合并态 1148 测全绿。
- RCON 断言判据：TileEntityBasicMachine.doWork :791 无条件排空⇒能量链断言用
  running 翻转判据（energy 值断言必然假日红；lesson 221/225）。
