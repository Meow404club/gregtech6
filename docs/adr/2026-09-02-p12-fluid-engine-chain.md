# ADR 2026-09-02-p12-fluid-engine-chain：流体罐引擎链总 ADR（范围/波次/蒸汽产源/声明偏离）

> 状态：accepted（W1 已落地：fuel-fluids merge 250422a；carrier/crank 在审；W2/W3 排队）。
> 本文为 state `decisions` 账本同名条目（架构师 p12-arch-fluid-engine 产出，消费
> tmp.research.p12-fluid-container + tmp.research.p12-engine-family 双研究卡）的成文
> 整理；只整理结构不改决策内容，全部 `文件:行号` 锚点原样保留。

## Context

- 用户定序：ghost 全量（已收官）之后，P12 第二主线=流体罐引擎链，原始范围=「桶 GUI·
  tap·funnel·IFluidContainerItem/FluidTankGT keepFilter→燃料→Engine/Axle/GearBox
  真机——RU/KU 语义 P11 已备好就差载体」。
- 研究修正了范围前提：上游桶 GUI 全族不存在（LH.java:508 "No GUI. Use Tiny Funnels
  and Taps" 即正典；桶显示=tooltip Barrel:89-103、交互=五工具 onToolClick2 :106-154）。
- 消费端解锁已由 P11 完成：mdk GTMachines.java:82 注释 "RU/KU net supply pending
  rotor family" 即本链闭合对象；TileEntityBasicMachine:373 折叠点已 unwound 活形——
  研究员建议的「R1 先行解锁卡」无需要。

## Decision

1. **范围与 GUI 终裁**：原始表述全收，唯桶 GUI 按【用户 2026-09-02 终裁】=「不做」
   写死声明项（非待定项不留在途尾巴）：桶交互面=tooltip+工具链即全部，本链及后续
   任何卡不得引入桶 Menu/Screen。
2. **波次结构**（研究员 A-E 五卡拆为九卡+一研究卡三波）：
   - W1 立即并行三卡零主文件交叠=p12-fluid-item-carrier（桶物品面+/gt6tank
     fill·drain·show 测试基建）∥p12-engine-fuel-fluids（steam/蒸馏水/柴油族流体+
     FM.Engine 图）∥p12-engine-crank（最小 RU 源零流体依赖）；
   - W2 四卡并行=p12-barrel-keepfilter-logistics（GTBarrels 文件域让 W1 carrier 后合
     rebase）∥p12-engine-steam（DEP fuel-fluids）∥p12-axle-family（DEP crank）∥
     p12-tap-funnel-attachment（DEP carrier）；
   - W3 收口两卡=p12-gearbox-transformer（DEP axle）∥p12-engine-diesel（DEP
     fuel-fluids+axle，软依赖 tap-funnel）；
   - 研究线并行无端口=p12-research-boiler-family（已完成）。
3. **蒸汽产源裁决**：测试注汽走通用 /gt6tank fill/drain（carrier 卡落地；桶=上游正典
   蒸汽载体，引擎背面邻接取汽），EngineSteam 卡直接消费；**不造** GTEnergySourceBlock
   式蒸汽源方块（能量侧 rig 先例成因=当时无能量载体，蒸汽侧已有天然载体，再造=发明
   非上游物）；真实锅炉=大族补研究卡先行考古（已完成：p12-research-boiler-family，
   真机卡 SPEC 建议 p13-boiler-steam-family 四波串行），不阻塞引擎卡。
4. **keepFilter 载体**：新 Logistics BE（上游唯一 keepsFilter()=T 消费者=
   MultiTileEntityBarrelLogistics:40，Loader:2171 "Logistics Tank" 行直译 1M L）；
   不挂现有三桶（改既有排水语义=发明语义+回归）；机制零改动（mdk FluidTankGT 全在）。
5. **Tap/Funnel**：一卡两族（液体面共享附件基座），Nozzle/CapNozzle（气体面）裁池
   随用随开——气体消费者不在本波。
6. **GearBox 齿轮实物安装面**（gearGt 物品）裁池，RCON 直写掩码=验收通道
   （/gt6cover signal 直写先例 P9）；玩家配置面=p12-gear-items 池卡。
7. **红线**：根模块零 net.minecraft（全部新件落 mdk）；GT6Mod/GTModBusListener 冻结、
   注册家卡内自持（引擎+传动族=GT6Kinetics 自持 DR）；无 onRemove 覆写（打桶丢液
   修复走 GTBarrelBlock.getDrops=上游 getDrops:157-162 同缝直译）；datagen 零手写
   JSON；消费语义零 diff（KU 过零沿/RU 每 tick=TileEntityBasicMachine:373 基类缝，
   引擎卡只做源端不回改基类）；旋转载体见配套 ADR 2026-09-02-p12-rotation-carrier。
8. **端口顺延 25705+**（tools/rcon 三层框架+chains/ 入库）：carrier 25705/25715·
   fuel 25706/25716·crank 25707/25717·keepfilter 25708/25718·steam 25709/25719·
   axle 25710/25720·tap-funnel 25711/25721·gearbox 25712/25722·diesel 25713/25723。
9. **声明偏离清单（总）**：桶 GUI 不做（终裁）；桶物品 NBT 键沿 "gt.tank"（NBT_TANK）
   非 Forge 模板 "Fluid" 键（BE↔物品 round-trip 同键，FluidHandlerItemStack:32 偏离）；
   工具五件面（软锤/通厕塞/扳手/温度计/放大镜）不在本链（mdk 无 onToolClick2 面），
   引擎停机门=RCON 命令；引擎无 GUI 面（机器 GUI 族后续卡）；流体纹理=vanilla 水染
   灰度占位（GTFluids:44-46 先例）；Crank 村民路径裁池；TurbineSteam（转子物品）/
   EngineElectric/EngineFlux/EngineRotation（RU↔KU 桥）/MagicFieldAbsorber/便携容器族
   （Thermos/GasCylinder/FluidDisplay）/润滑脂·lubricant/密封发酵=池不入本波。

## Deviations（否决备选）

- 桶 GUI 净新增卡移交用户——被用户终裁取代：不做，写死。
- R1 另立解锁卡——P11 已完成消费端解锁，否决。
- GTEnergySourceBlock 式蒸汽源方块——桶已是上游正典蒸汽载体，否决。
- 锅炉直接实现卡——高风险大族未考古违「先研究后拍方案」红线，否决→研究卡。
- keepFilter 挂现有三桶+开关——改既有排水语义=发明语义，否决→Logistics BE 载体。
- Tap/Funnel/Nozzle/CapNozzle 四族一卡——气体两族零消费者违 YAGNI，否决→两族+两池。
- GearBox 等齿轮物品前置卡——阻塞传动语义验收不值，否决→RCON 直写验收+齿轮物品池。
- A-E 五卡串行——W1 三卡两两零主文件交叠，串行浪费并行度，否决。
- EngineSteam 26 变体子集先行——注册表直译子集=任意编辑上游循环+二轮 datagen churn
  （wire W1 裁决同款），否决→全表一步到位。
- Create KineticNetwork 形态移植——见旋转载体 ADR，否决。

## Consequences

- **W1 落地**：p12-engine-fuel-fluids merge 250422a（审查实测 mdk 749/0+根 205/0，
  行值逐条对照 Loader_Fuels.java:77-120 吻合，STEAM_PER_WATER=200 归属=EngineSteam:58
  引擎自有常量双面钉死；CS.java:242=160 全局标准未移植已 javadoc 明示）；carrier/crank
  在审。
- **遗留移交**：findRecipe 流体-only 查询缺口（RecipeMap.java:137-138 vs 上游 :519-523
  流体哈希索引）→p12-engine-diesel 卡扩展；BioEthanol 192（Loader_Fuels:121-124）卡外
  未收（卡钉 :77-120）；GT6EnUs "Fuel" 展示名与 MT.java:2044 "Fuel Oil" 不一致
  （lang 取通用名，非语义错误，备忘）。
- **基线**：main 20bb36d（派发时前移取当时 HEAD 惯例）。

## 勘误附录（2026-09-02 审查期回写）

1. **keepFilter 载体"机制零改动/FluidTankGT 零 diff"前提有误**：p12-barrel-keepfilter-logistics
   实现期证明 0 量身份 round-trip（验收 a）在 W1 版 FluidTankGT 上不可达——审查官
   逐点核验上游 gregapi/fluid/FluidTankGT.java 后裁定六点修复**全部为原文语义恢复**
   （isEmpty=:314 逐字、contains=:321 空标记载体重表达、writeToNBT=:70-80 同字节
   载荷+copy 写防 1.20.1 setAmount(0) 空标记毒化活栈、readFromNBT 结果态保持、
   getFluid=:359 跳过 0 量重绑、legacy minecraft:empty=W1 降级数据迁移），非发明
   语义；"W1 测试注释预授权"（carrier 卡 TileEntityBase08BarrelTest:117 'carries
   the fix with its port card'）逐字在案。合入 d79440c（mdk 782/0+metal 对照臂身份
   清=默认语义零漂移实证）。
2. **Axle 材质表规格外行**：上游 Loader_MultiTileEntities.java:1749-1752 另有第 12
   材质 Trinaquadalloy（VMAX[8]=1048576，带宽 256/512/1024/2048）在卡钉 :1662-1744
   范围外未收入 AXLE_SPECS（44 行止于 Trinitanium）——入池，后续卡显式声明补行。
3. **蒸汽入罐偏离**（详见配套裁定 ADR 2026-09-02-p12-steam-proof-deviation）：研究
   证伪"桶=正典蒸汽载体"的笼统表述——上游一切桶/罐装不住蒸汽（POWER_CONDUCTING
   销毁链，FL.java:85），正典载体=管道+机器内置罐；本链 RCON 注汽改直打引擎进汽面
   （正典形态），罐面/物品面销毁链与拒入门=声明偏离，P13 锅炉卡 W1 四件前置强制
   还账（FL 名单地基/基类 tick fizz 两查/gasProof 载体行/物品面 fill 门）。
4. **登记行勘误**：Logistics Tank 注册行实为 ANY.W=钨系+aUtilMetal（Loader:2171
   实读），卡面"木系 aUtilWood"系笔误，以实现为准（合入 d79440c）。
