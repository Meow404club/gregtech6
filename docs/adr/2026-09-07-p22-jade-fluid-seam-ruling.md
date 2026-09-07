# ADR 草稿：P22 Jade 流体缝裁决（数据缝单分支 + v1 内嵌形态 + 长量载体）

- 日期：2026-09-07
- 状态：Accepted（P22 收官主会话正典化 2026-09-07）
- 研究依据：state key=`tasks.p22-research-jade-fluid`
- 实现卡：state key=`tasks.p22-jade-fluid-tooltip`（merged `b9b0b23f`，commit e4ea939d）
- 审查修正：state key=`tasks.p22-arch-feature-wave` review_record.corrections C-1 / C-2（D-1 零触碰定案）
- 证据路径根：上游=`tmp/gt6-1.7.10/src/main/java/`，port=`mdk/src/`，Jade harvest=`tmp/harvest/jade-*`

## 背景与证据

1. **数据缝=必须 Jade 服务端推**：port 机器罐组虽进存档与 getUpdateTag（`mdk TileEntityBasicMachine.java:1525-1526`、getUpdateTag=saveWithoutMetadata），但**同步触发面不含流体**：onFluidIO→onInventoryChanged 只 `setChanged`（:422-425），onTickCheck 只查 ACTIVE/RUNNING 视觉位（:402-405）→fill/drain 不触发 sendClientData，客户端 BE 罐内容陈旧（仅 chunk 重载/视觉位翻转偶发刷新）。Jade 服务端数据每次 hover 请求新鲜——统一走服务端推缝，免 per-BE 同步链分叉。
2. **C-1 修正（研究二两分支设计不成立）**：『多方块 mTanksOutput 需 provider 第二分支』被复核推翻——`TileEntityBase10MultiBlockMachine.java` 全文 168 行且 :168=类尾大括号，该类**零罐字段**；上游罐字段唯一 public 声明=`MultiTileEntityBasicMachine.java:101`（另 MixingBowl/BathingPot 各自 protected，均未移植）。→ provider 定稿单分支 `instanceof TileEntityBasicMachine`。
3. **C-2 修正 + D-1 零触碰定案**：研究二『量>int 走 LAmount putLong 经公开面』在移植面缺缝——port FluidTankGT 公开面全 bindInt 钳位（getFluidAmount :256 / getCapacity :261），内部 mAmount 才是 long（NBT_L_AMOUNT 溢出键）。补 additive `amount() long` getter；复核实证 port `FluidTankGT.java:399` `amount()=isEmpty()?0:mAmount` 与上游 `gregapi/fluid/FluidTankGT.java:330` **逐字同形已存在**→getter 零新增（D-1），fluid/ 包全程零触碰。
4. **形态裁定依据**：研究推荐 universal `registerFluidStorage` 路线（GTCEu 全例 GTFluidStorageProvider.java:40-101）。其双腿唯一大叉=`IServerExtensionProvider`（jade-1201 :11-14 `<IN,OUT>`+4 参 getGroups vs jade-1211 :10-17 `<T>`+Accessor+shouldRequestData）需 `//?` 小类；而 **v1 内嵌形态零新分叉**：`IElementHelper.fluid`（1201 :29 / 1211 :30）、`JadeFluidObject.of(Fluid,long)`（:30-32 / :37-39）、`CommonProxy.getFluidName`（:451/:470）、`FluidTextHelper.getUnicodeMillibuckets`（:9/:7）双腿逐字同形，且两形态共用同一服务端取数代码。
5. **长量坑位**：GTCEu `GTFluidStorageProvider.java:84-85` 注释『FluidView#readDefault can't handle amount > INT_MAX』——overlay 载体不可携真量。

## 裁定

### D1：数据形态=v1 内嵌（否决本波直上 universal registerFluidStorage）

新 `GT6FluidProvider`（IBlockComponentProvider + IServerDataProvider&lt;BlockAccessor&gt;）注册进 `GT6JadePlugin`（恰 +2 行）。服务端 appendServerData 单分支写罐数组，客户端内嵌 fluid 图元+文本行。**universal 路线入池为升级路径**：取数代码共用，升级无沉没成本；v1 换取零 `//?` 分叉与最小侵入（无 BoxStyle/既有四段触碰）。

### D2：自描述键 + 双值策略（避 readDefault INT_MAX 坑）

- 服务端键自描述：`GT6FluidsIn/Out` = ListTag of `{FluidName, Amount TAG_LONG, Capacity TAG_LONG}`（真实 long 原值过缝，3e9&gt;INT_MAX 断言钉死）；空罐不产条目。
- 客户端双值：**载体** `JadeFluidObject.of(fluid, 1000)`（图标渲染用，1000 mB 展示量绕 readDefault 钳位）；**文本行**用真实 long 走 `FluidTextHelper.getUnicodeMillibuckets` + `CommonProxy.getFluidName` + wrapInSquareBrackets，行形「量串 空格 [名字]」纯函数钉死。

### D3：分叉面与验收（merged `b9b0b23f`）

- chisel 唯一 rawFluid 四行（forge `getRawFluid` / 21.1 `getFluid`，`FluidTankGT.writeToNBT:161/:163` 同 swap 先例）；files 恰 3 文件（新 Provider+Test、Plugin +2 行）。
- 组标签行灰色 literal 仅组非空才写（D-2 最小侵入定案）；leg-neutral 测试以 BlockAccessor 返回类型驱动双腿共用。
- 门禁：forge 1217/0/0、neo 1219/0/0（2 skip 既有，XML 实数）；runServer 冒烟（game/rcon/query 框架）UP+零 ERROR+RCON 精确停机；oven（非 BasicMachine）与 null BE 双负例零写不扰他 provider 键。

## 后果与池项移交

- 世界 tooltip 首具机器进出罐流体段（图标+真实长量文本），声明为移植增强（上游 1.7.10 无 WAILA 面）。
- 留池：universal registerFluidStorage（双腿 IServerExtensionProvider `//?` 小类一次性升级）、Jade BoxStyle/进度样式、桶/锅炉/引擎管罐面（各属自身子系统）、BE 流体同步链修复（Jade 服务端推本身就是缝）。
- 收官清单：外部客户端 hover 截图目验（不入门禁）。
