# ADR 2026-09-02-p12-rotation-carrier：旋转载体——零新 EnergyType，复用 RU/KU+push 握手

> 状态：accepted（裁定生效中，随流体罐引擎链卡逐步落地：crank→axle→gearbox）。
> 本文为 state `decisions` 账本同名条目（架构师 p12-arch-fluid-engine 产出）的成文整理；
> 只整理结构不改决策内容，全部 `文件:行号` 锚点原样保留。

## Context

- 上游语义：TD.java:74-84 RU=IC2 EU 单位制（Size=Speed 正顺/负逆时针、Amount=Power）、
  :87-92 KU=Push/Pull；本仓根 `src/main/java/gregapi/data/TD.java:216`
  `ALL_ALTERNATING=(F,KU)` 实证两令牌已在（P11 引用相等门隔离）。
- P11 已备：假电源折叠点 mdk TileEntityBasicMachine.java:365-373（上游 :815 折叠点
  unwound live 形）+GTMachines.java:82-85（RU/KU net supply pending 注释=本链闭合对象）
  +GTEnergySourceBlockEntity.java:42-53（±alternating 2bit 活塞相位先例）。
- GTCEu Modern 未移植本链（Registry 检索零命中）；Create 1.20.1 KineticNetwork/
  RotationPropagator 已 harvest 入 RAG（curator 2026-09-02 回执，rerank 0.999+）作参照。

## Decision

**零新 EnergyType**——旋转载体三件全复用既有面：

1. **令牌**：RU/KU 复用根 TD 既有单位族，根模块零改动零新常量（新常量会破
   GTMachines.java:501 引用相等门）。
2. **传输**：根 gregapi ITileEntityEnergy 既有 push 握手原样（Util.emitEnergyToNetwork
   六面循环→emitEnergyToSide→insertEnergyInto→doEnergyInjection），不新增任何网络
   对象/线缆。RU 无线缆是上游正典（MultiTileEntityWireElectric.java:180/:184 硬锁 EU）
   非缺口——**Axle=邻接递归 BE**（mdk 新类实现 ITileEntityEnergy 收+发双向），每 tick
   从源端 push 全程、零损、超速 popOff+原额退回（上游 MultiTileEntityAxle.java:
   105-117/:125-129/:159 直译）。
3. **KU 无载体**：引擎面邻接单格直供（上游正典），不发明 KU 轴。
4. **Create KineticNetwork=参照不移植、零形态借用**：①语义正交（Create=带符号 RPM
   主量+stress 网络对象+sources/members 双 Map+unloaded 影子记账+flicker 上限；
   GT6=size×amount 包制 push、每源逐 tick、无网对象）；②跨 chunk 卸载断链=push 制
   天然形态=上游正典，不发明影子记账；③移植网络对象必迫使回改 P11 已钉的
   TileEntityBasicMachine:373 过零沿折叠缝=踩「消费语义零 diff」红线。参照仅限反例
   对照：sync 广播与 propagateNewSource 冲突破坏语义作 Axle/GearBox 边界测试对照。

## Deviations（否决备选）

- Create KineticNetwork 形态移植（网络对象+速度主量）——语义正交+必踩 P11 基类
  零 diff 红线，否决。
- 零形态借用仅借 stress/过载模型——GT6 过载=超速 popOff/超带宽拒收退回已自洽，否决。
- 新 EnergyType.ROTATION 常量——RU 已是 IC2 EU 单位制直译，新常量破 :501 引用相等
  门，否决。
- KU 轴/远距 KU 传输——上游 KU 无载体邻接直供是正典，否决。

## Consequences

- 落地路径：p12-engine-crank（W1，最小 RU 恒负号直流源）→p12-axle-family（W2，
  邻接递归）→p12-gearbox-transformer（W3，分流/齿爆/变矩）；KU 侧 p12-engine-steam
  （W2）面邻接直供。
- 测试：Axle/GearBox 边界测试以 Create 语义（flicker/冲突破坏）作反例对照
  （harvest 两份已入 RAG 可引用）。
- 消费端零回改：Shredder/Lathe RU 每 tick 出料=TileEntityBasicMachine:373 基类缝
  P11 已钉零 diff，引擎/传动卡只做源端与载体。
