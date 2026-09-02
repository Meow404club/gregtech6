# ADR 2026-09-03-p13-barrel-base-unfreeze：TileEntityBase08Barrel 冻结面恰量解冻（tick fizz 两查）

> 状态：accepted（随 work/p13-steam-proof-repay 落地）。
> 决策源头：2026-09-02-p12-steam-proof-deviation（声明偏离 + P13 强制还账）的关闭件。
> 成文风格沿 2026-09-01-p10-cover-item-intercept（P9 B 卡 p9-redstone-hooks 先例：
> 解冻 = 恢复上游既有语义，非新面）。

## Context

- TileEntityBase08Barrel（mdk）自 P4 起是冻结面（p4-fluid-barrel 交付，P5/P6/P7/P12
  多张卡在 javadoc 与红线里明令基类零 diff）；上游 :164-186 四防销毁链在 P4 卡裁池
  （"the gas/acid/plasma/magic proof quartet"），物品面 :252-254 四防尾随 P12 物品面卡
  同裁。
- 偏离窗口：本仓 gasproof 类桶（metal/plastic/logistics）在罐面可存住蒸汽——上游实证
  全部桶型拒蒸汽（研究卡 tmp.research.p12-steam-barrel-gasproof：真因不是 GASPROOF
  而是 POWER_CONDUCTING，FL.java:85 Steam 进四名单，tick :184 fizz 销毁）。P12 架构
  裁定 = 声明偏离 + P13 强制还账（四件同卡：名单地基 / tick 两查 / 载体行 / 物品面 fill 门）。
- 本卡并行卡 p13-hu-steam-foundation 独占 GTFluids.java / GT6RecipeMaps.java——
  本卡名单地基放 mdk fluid/ 域新文件 GTFluidLists（名字集合，与 GTFluids 静态初始化
  零耦合），基类解冻只落在 TileEntityBase08Barrel 一个文件。

## Decision

冻结面解冻显式裁决（本 ADR 是解冻的唯一 sanctioned 一次），范围写死为三件：

1. **tick 链恰两查**（上游 :180-186 直译）：onTick 在熔毁查（:162，既有 meltsDown）
   之后、pushByGravity（p5 语义）之前插入——gas 查先
   （`!gasProof() && GTFluidLists.isGas(name)`）、allowFluid 查后
   （`!allowFluid(name)`），命中走 fizzTrash()（上游 :181-182 SFX.MC_FIZZ +
   GarbageGT.trash 的 trash 半：mTank.setEmpty + onTankChanged；音效面池化声明）。
   两门独立（上游 :180 与 :184 是两个 if），顺序与上游一致；if/else-if 单次销毁语义
   由「短路 + 单一 trash 调用点」保持（离线计数实证 trash 恰一次）。
2. **gasProof() 覆写点**（对位上游 mGasProof 字段 :62/:95/:180/:251）：基类缺省 F
   （= wood 族行真值 :2136-2149），子类覆写从块载体读
   （GTBarrelBlock.gasProof()，capacityL/meltingPointK 完全同缝的第三载体字段），
   离线（非 GT 块 fixture）落类真值——plastic :2150 / metal :2151-2170 / logistics
   :2171 全 T。GASPROOF 只免 gas 查、不免 allowFluid 查（两门独立）。
3. **allowFluid 最小面**（上游 :233-235 的名单查部分）：
   `!GTFluidLists.isPowerConducting(name)`。上游温度分支（:234
   `FL.temperature < mMeltingPoint`）由既有熔毁查承担、不重复移植（上游熔毁查先行
   使任何 ≥ 熔点的流体到不了 :184，两语义等价）；onlySimple 过滤器（:234 第三分支）
   维持池（FL.simple 基建缺席，P4 裁定不变）。谓词收 String（registry path）=
   离线可测（UT.powerconducting UT.java:187 的 contains 形态）。

其余一切维持冻结：keepsFilter/重力推/cover 面/p5 侧规则/p12 物品面 round-trip
语义零触碰；TileEntityBase08BarrelTest 既有断言不改一行（红线，回归零 diff 达成）；
acid/plasma/magic 三防（上游 :164-179）与其物品面尾 :252-254 仍池；无 onRemove；
GT6Mod/GTModBusListener/根模块零触碰。

## Deviations

- **名单收 String 而非 Fluid**：上游 POWER_CONDUCTING 是 Set<String>（FluidsGT），
  消费点 Fluid.getName()——1.20.1 对应 ForgeRegistries.FLUIDS.getKey(f).getPath()，
  本卡把它收进 GTFluidLists.name(FluidStack)，谓词面纯 String，离线真值表全绿；
  live gt6:steam 走 RCON 链（GTFluidsEngineFamilyTest 离线/ live 分工先例）。
- **SFX.MC_FIZZ 池化**：本仓无音效面，fizzTrash 只做 trash 半（tank 清空必做）+；
  音效留池。
- **离线测试的 FluidType 缝**：fluidTemperature/fluidDensitySign 的 FluidType 查表
  离线 NPE（既有事实，p4 起如此）——BarrelFizzChainTest 以窄覆写绕开（名字键温度表
  = GTFluids.ENGINE_SPECS 声明表先例；pushByGravity 探针内置 no-op），生产代码零改动。
- **BE 覆写落类真值**：gasProof() 覆写「载体值优先、非 GT 块 fixture 落类真值 T」——
  离线测试无法构造 mod Block（GTOfflineTestBase 冻结注册表铁律），类真值与载体行
  由上游逐行核对保证一致（全 metal/plastic/logistics 行 GASPROOF=T，wood 族 F）。
- **tickPost 跳过窗口**：熔毁/fizz 命中后的 `return` 与既有熔毁早退同形（基类现状
  即如此），不为本次改动扩面。

## Consequences

- P12 偏离关闭：卡后 gasproof 桶 tick 一轮必清蒸汽（RCON 链 metal 鼓注汽→空断言），
  wood 桶注汽→熔毁（与名单链正交：373K≥340K 熔毁查先于 gas 查）。
- 蒸汽正典载体回到研究卡结论：管道 + 机器内置罐；桶/罐一概不留蒸汽。
- 物品面 fill 门（上游 :250→:251 顺序）随卡同落（GTBarrelItemFluidHandler，非冻结面
  但同属还账四件）；drain 三态零改动=存量蒸汽仍可抽（清账语义）。
