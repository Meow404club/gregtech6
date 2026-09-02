# ADR 2026-09-02-p12-steam-proof-deviation：蒸汽四防销毁链 = 声明偏离 + P13 强制还账

> 状态：accepted（2026-09-02 裁定：P12 不补实现、不新增在途卡；还账点 =
> p13-boiler-steam-family W1 强制前置件）。本文为 state `decisions` 账本同名
> 条目的成文整理（2026-09-03 O1 补账转正，主会话），只整理结构不改决策内容；
> 全部 `文件:行号` 锚点原样保留。

## Context

- 用户疑问「上游储罐能否进蒸汽」经穷举研究证伪直觉：**全部储罐都装不住蒸汽**。
  真因不是 GASPROOF 而是 POWER_CONDUCTING——FL.java:85 Steam 注册进
  SIMPLE,GAS,STEAM,POWER_CONDUCTING 四名单（构造器 :571-575 逐 set add；
  ic2steam :86 / superheated :87 同），allowFluid 拒 powerconducting
  （Barrel:234 / Tank:102），tooltip 明示 voided（LH:511）。
- 桶两 fill 面分离：物品面 IFluidContainerItem.fill:248-258 硬门（:250
  allowFluid 即拒）；罐面 getFluidTankFillable2:292 仅密封门→蒸汽可进但 tick
  链必毁：熔毁 :162（steam 373K；wood 340K / plastic 370K 均自熔）→gas :180
  →allowFluid :184。
- 本仓 P12 现状：熔毁链已忠实复现（TileEntityBase08Barrel.java:163）；但
  gas/powerconducting 两查缺失 → gasproof 桶（metal/logistics）可存住蒸汽
  =偏离；物品面 fill 无门（GTBarrelItemFluidHandler.java:139-144）。
- P12 内玩家可见窗口=0：蒸汽唯一产源=RCON 命令，锅炉未移植。

## Decision

P12 蒸汽四防销毁链处置=**声明偏离 + P13 强制还账**，不补 P12 实现不新增在途卡。

1. **罐面销毁链：声明偏离**——本仓 gasproof 桶（metal/logistics）可存住蒸汽，
   偏离上游 tick fizz 销毁（Base08:184 powerconducting / :180 gas）；
   wood/plastic 不在偏离内（373K≥340/370K 熔毁链
   TileEntityBase08Barrel.java:163 已忠实复现，上游 :162 熔毁同样先行）。
   **关闭点 = p13-boiler-steam-family W1 强制前置件四件同卡**：FL 名单地基
   （POWER_CONDUCTING/GAS Set+登记 API，FL.java:571-575 形态）+基类 tick fizz
   两查（gas 先 allowFluid 后，GASPROOF 只免 gas 查）+GTBarrelBlock gasProof()
   载体行（wood=F/plastic=T/metal=T/logistics=T）+物品面 fill 门；基类解冻
   （P5 冻结面）随该卡显式 ADR（P9 B 卡先例）。
2. **物品面 fill 门**（GTBarrelItemFluidHandler.fill:139-144 无门）：同一偏离
   覆盖，P13 同卡补——本仓物品面 fill 暴露面≈0（无第三方灌入源、Funnel=灌出、
   vanilla 不灌非原版容器），名单不存在时单建地基=成本倒挂。
3. **engine-steam 卡 rebase 措辞窗口**顺手把 RCON 注汽臂改打引擎进汽面
   （for_p12 ③ 原裁定），删 metal 桶注汽臂——否则 P13 补链之日=该链 fizz
   失效之日；keepfilter 卡 RCON 核对已改 water（卡面存档仍见 gt6:steam）。
4. **P6 账面**：『Gas-proof 无消费者不引入』前提被部分证伪→勘误注记
   （decisions 新 ADR）+KG 修正边（GT6_barrel_proof_quartet POOL_REVISED_BY
   ADR / MGT6GA_barrel_family DEVIATES_FROM GT6_Barrel_ALL /
   DELEGATES_QUARTET_TO p13-boiler-steam-family），不做 kg_invalidate（KG 无
   独立旧边，PORTS_FROM 真边不动）。

**理由**：P12 内玩家可见窗口=0（蒸汽唯一产源=RCON 命令，锅炉未移植）；名单是
地基非两查，满载波次夹带地基=伪小卡；fizz 表现/gas 判据属 HIGH 风险语义细节，
无锅炉语境不拍。

## Deviations（否决备选）

- 立即补最小链——三重 churn 零玩家收益+gas 查现存流体集零可观测差异 YAGNI，否决。
- 物品面单独补——成本倒挂，否决（并入还账四件同卡）。
- 并 keepfilter 卡——红线明令基类零 diff，否决。
- 并 engine-steam 卡——文件域不合，否决。
- kg_invalidate 旧边——无对象（KG 无独立旧边，PORTS_FROM 真边不动），否决。
- 永久偏离不还账——P13 后桶存汽=玩家可达平衡偏离，上游刻意逼管道拓扑，否决。

## Consequences

- **还账执行**：还账四件已作为 P13 W1 并行卡 `p13-steam-proof-repay` 派发
  （2026-09-03，基线 main b1d9dd9，交付含 TileEntityBase08Barrel 解冻显式 ADR
  `2026-09-03-p13-barrel-base-unfreeze.md`——随该卡入库）。
- **KG 修正边已建**：GT6_barrel_proof_quartet POOL_REVISED_BY 本 ADR；
  GT6_barrel_proof_quartet DELEGATES_QUARTET_TO p13-boiler-steam-family。
- **勘误附录对位**：docs/adr/2026-09-02-p12-fluid-engine-chain.md 勘误附录
  （steam-in-tank 行）与本 ADR 同源。
- 研究全文=state `tmp.research.p12-steam-barrel-gasproof`（蒸汽进储罐穷举）。
