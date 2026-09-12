# ADR 2026-09-12 — P28 方案 B：独立 FE→EU 转换机（ULV 单档）与上游刻意缺席的声明偏离

- 状态：已接受（p28-b-fe-converter-machine 交卡即生效）
- 关联决策：`decisions.p28-eu-inbound-converter`；`research.p28-r-eu-inbound`；前置 `p28-a-fe-inbound-math`（main 89d4cced：root `EnergyBridge.extractFe`/`IFESource` 入向对偶数学）
- 关联 ADR：ADR 2026-08-31-p7-energy-network（能量网五裁定）、2026-09-03-p26（出向桥，本卡的对偶方向）

## 1. 背景

P26 落地了 EU→FE 出向桥（GT 发射端 → 外族 FE 槽）。FE 生态的对偶方向——FE→EU
入向——在 P26/P28 两轮研究中裁定走**独立转换机方块**（方案 B），波次 2 池化解冻，
即本卡。任务卡 SPEC 原定「LV/MV/HV 三档起步的 tier 电压梯」；实现中途用户平衡
裁定修正为 **ULV 单档 1A**（见 §4）。

## 2. 偏离声明（deviation ledger 正式条目）

**上游 1.7.10 刻意不存在 RF→EU 转换机。** Greg 的墙：Flux 家族只做 RF→HU/KU/RU/LU
（Loader_MultiTileEntities.java:824-957），Flux Dynamo 是反向 RU→RF 出向（:953）；
EU 机器通过类型判等拒收 RF（root:749-755 的 RF 面只被动收不转 EU）。RF 永远不进 EU
电网是上游的设计姿态，不是移植缺口。

因此本机的身份是 **GTCEu 形新增，非上游移植**：语义锚取上游转换核
（`TE_Behavior_Energy_Converter.doConversion` :61-94， ridden by
`TileEntityBase10EnergyConverter`）的缓冲/包语义/min-max 门/超载爆炸/只扣实收，
FE 面取 GTCEu `ConverterTrait`（:98-148）的 4:1 + 比率取整对齐。参照系里 GTCEu
是唯一「现代 FE↔EU 双向转换」先例，本卡如实引用并声明偏离。

### 2.1 对 GTCEu 的两处超越与一处坍缩（如实记录）

- **pull 补全（超越点）**：GTCEu ConverterTrait 的 FE 进面是 push-only
  （`extractEnergy` 恒 0 + `canExtract=false`，:124-126/:141-143），issue #2089
  实证该形态会被拉取型 FE 网络卡死。本机补齐双支持：`receiveEnergy` push 收缓冲 +
  每 tick 经 root `EnergyBridge.extractFe`（IFESource::extractEnergy 适配）从邻接
  FE 源拉取整包列车。
- **包单位取整（改进点，沿 p26 裁定）**：GTCEu 对齐在比率单位（`received -=
  received % ratio`，:108）；本机沿用该形于 push 面，但记账单位提升到整包
  （`aSize*4`），与 root 桥数学逐项对称。
- **超载软梯坍缩（声明坍缩）**：上游 `TileEntityBase10EnergyConverter.overload`
  （:140-148）先记 100 次软超载（清缓冲）再 `overcharge` 爆炸——该梯服务于倍率
  误配，而 FE 面没有倍率/电压概念，误配路径结构性不存在，故本机直接走 overcharge。
  上游 doConversion 的 **2-tick chunkload 宽限（:72 `if (aTimer > 2)`）逐字保留**：
  落地形态是「两拍内清缓冲 + 日志，第三拍起 overcharge（tierMax 强度）」。
- **超载爆炸保留（非裁剪对象）**：p26 出向桥裁掉了**外族**侧的
  checkOverCharge（EnergyCompat.java:129-137）；本机是 GT 族机器，爆炸语义
  完整保留（research.p28-r-eu-inbound 风险 ③）。RCON 链 D 相活证：data merge
  越钳写入缓冲 → 宽限两拍 → `overcharge` → 悬置爆炸 → 方块消失。

## 3. 机器形态

- **唯一机器** `gt6:fe_converter`：ULV = V[0] = 8 EU × 1 A（GTCEu ConverterTrait
  形 tier/amps，单档化）；缓冲 = GTCEu 电容器形 V×16×amps 欧折 FE = 512 FE；
  整包 = 32 FE；吞吐上限 = min(1A, 整包数) = **8 EU/t**。
- **面**：FE intake 全六面（canExtract=false，pull 网络偷不走缓冲）、EU emit 全六面
  （`ITileEntityEnergy` 既有面，GT 消费机零改动——min 门=整包、只扣实收=:87 形）。
  面几何/节流为研究卡明示的设计自由度，全六面是声明简化。
- **接线**：自持 `GT6FeConverters` 注册类（ADR-P3-4 形，GT6Kitchen 形制），
  GTMod 冻结面零触碰、GTMachines.java 零编辑——机器页签走
  `BuildCreativeModeTabContents` 事件缝（GT6Kitchen.onBuildTabContents 正典）。
  21.1 腿 capability 行尾追加进 `GT6CapabilityWiring`（共享串行文件 append-only）。
- **KJS 面**：配方=datapack 域天然可改（shaped datagen 行 PWP/PCP/PWP，
  锡板+红合金细线+铜锭，声明新设计无上游配方）；机器行为=无 KubeJS 面；
  注册面=defer（KJS 绑定卡池化）。

## 4. ULV 平衡节（用户裁定，2026-09-12 中途修正，覆盖任务卡的三档梯）

**锁 ULV、锁 1A 的理由（用户裁定原文精神）**：RF 只准待在能量链最底层——蒸汽锅炉→
动能→LV+ 电网的进度墙必须保留。数字上：本仓全部已移植 EU 消费机的输入门槛 ≥16 EU
（炉 T1=min16/rec32/max64，`EnergyGate.gateInjection` :50 对不足门槛的包**吞而不入**），
8 EU 包驱动不了任何 GT 消费机，唯一去处是出向桥面。多安培会加和回 LV 级吞吐
（2A=16EU/t 已越过炉门槛），破坏墙的意义——所以单台单安。比率保持无损 4:1
（`CS.RF_PER_EU`，root/出向/入向三方同一常量）。配方相应单台，不再有梯。

**链上的诚实注记**：因为 8 EU 包过不了任何消费机门，「GT 机器收电活证」的接收端
走 p26 桥（fe_battery 夹具按 32 FE/包记账）——发射侧是标准 GT emit，桥分派就是
外族接收端的真实路径；ULV 消费机族是后续池化项。

## 5. fe_source 夹具

现有 fe_battery 是纯 sink（maxExtract=0，出向正确、入向盲）。加可抽取孪生
`fe_source`（cap,0,cap，/gt6fesource place|stat|set|reset，place 满灌）：
放测试夹具域（GT6FeBatteries 尾追加）、不进创造栏、无 loot 表——与 sink 同规则。
pull 相活证用它：130 FE 源被拉走 4 整包后 **2 FE 尾永远留在源里**（敌意零头语义
的活体版）。

## 6. 验收绑定

- 离线：`GT6FeConverterBlockEntityTest` 9 例（比率地板/容量钳/模拟不落账/pull 整包+
  敌意零头留源/空隙守卫/emit 每拍 1 包=8EU/t/只扣实收/min 门/超载宽限→悬置爆炸/
  NBT 往返/GT 纯源面），双腿绿。
- 活体：`tools/rcon/chains/p28_fe_inbound.py` 双腿 GREEN——A push 往返无损 4:1
  （512 FE 进 → 512 FE 出）、B pull+零头留源（130→2）、C 吞吐天花板（6s 窗源侧
  96xxx 数字带，×2 回归即红）、D 超载爆炸（allow_failed absence 证明）。
- 语义锚 file:line 全录于实现提交（6dc341ce）javadoc 与本篇。
