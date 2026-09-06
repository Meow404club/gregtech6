# TODO（镜像）

> 权威数据在 `state_read("todo")`。新增任务请同时写 MCP state 与本文件。

## 第 1 阶段（材料系统）——✅ 已完成（2026-08-29）

- [x] 卡1 gt-material-foundation：Gradle 骨架 + TagData/OreDictMaterialStack/Serializer（merge 2f3be03，34 测）
- [x] 卡2 gt-material-model：OreDictMaterial 完整模型 + MaterialRegistry 状态机（merge 1b7251c，73 测）
- [x] 卡3 gt-material-dataset：TD 全量 + MT 1273 材料 + AM/ANY（merge e408927，97 测）
- [x] 卡4 gt-ore-prefix：OreDictPrefix + OP 421 前缀 + mPriorityPrefix（merge 3d0aac3，133 测）
- [x] 卡5 gt-material-graph：MaterialGraph 链查询 + 合金组分引用图（merge 972dbad，161 测）

## 第 2 阶段（MDK 挂载 + 注册桥 + DataGen）——✅ 已完成（2026-08-29）

- [x] 调研三卡：构建挂载（NeoForge 1.20.1 证伪→Forge 47.4.10）/ 注册与生命周期 / DataGen
- [x] 平台裁决：MinecraftForge 1.20.1（47.4.10）+ MDG legacyforge 2.0.144（用户裁决）
- [x] p2-mdk-skeleton：mdk/ 子项目 + wrapper 8.14 + mods.toml + 空 @Mod（merge 69b9d39）
- [x] p2-registry-reset-idempotency：ANY 两相守卫 + TECH 重绑 + Invar 根因（第 0 代孤儿实例复用）+ 7 幂等测（merge 5564226）
- [x] p2-material-condition-system：OreDictMaterialCondition 14 谓词逐字 + 47 前缀解锁（421→468）（merge 21ee363）
- [x] p2-registration-bridge：三段生命周期 + RegisterEvent 动态注册 2469 物品 + 91 合金反链（merge 308f85e）
- [x] p2-datagen-pipeline：runData 管线 + 2469 模型 + lang 两表 + 103 占位 PNG（merge 139c8c4）
- [x] phase-closeout：ADR-P2-6 六条验收线全满足（188 测 / runServer Done×3 / runData 可复现 / runClient 冒烟过）

## 第 3 阶段（BE + 容器 + Screen 框架）——✅ 已完成（2026-08-30）

- [x] 调研三卡：BE 框架 / Menu+Screen / Capability+持久化（重大修正：GT6 不把材料身份写常规 ItemStack NBT，a/i/m 键仅坩埚/熔炼类 BE 内部 NBT）
- [x] architect 拆卡：4 卡 8 ADR 两波次（ADR-P3-1 共享 BET、P3-2 capability 缝合、P3-3 screen 挂载时机、P3-4 GT6Mod 冻结自持注册、P3-5 chest 选型、P3-6 延后池、P3-7 全量前缀纳入主序列、P3-8 验收线）
- [x] p3-be-framework：01Root/03TicksAndSync 最小面 + 同步层 vanilla 双通道 + 共享 BET + capability 缝合 + MaterialStackNBT short 兼容（merge 43fcb1a）
- [x] p3-menu-framework：GTMenuTypes + GTGuiMenu/GTGuiScreen + Slot 三件套 + /gt6gui + MenuScreens 唯一挂法（merge b49d3e2）
- [x] p3-fullprefix-creativetab：105 item-path 前缀全量注册 56253 物品 + 96 creative tab + 双判据收敛（merge 0348638）
- [x] p3-example-machine：chest 全链（54 槽 + 动态行 + 打开链 RCON 实证 + blockstate/model/lang datagen）（merge b6b01eb）
- [x] phase-closeout：ADR-P3-8 六条验收线全满足（221 测 / runData 可复现 / GT6Mod diff 空 / mdk/logs gitignore 2a04936）

## 第 4 阶段（管线 / Cover / 多方块渲染 BakedModel + 首台加工机器）——✅ 已完成（2026-08-30）

- [x] 调研五卡：流体管线/Cover/多方块校验/BakedModel 渲染/加工机器（全带 文件:行号 证据链）
- [x] architect 拆卡：8 卡 10 ADR 三波次（recipe 分层/流体直译/能量边界/首机选型/渲染路线/cover 路线/多方块逐字/流向控制/验收总纲）
- [x] W1：p4-recipe-core（Recipe 壳+两段式消耗+vanilla 桥，merge 02b7b8b）∥ p4-fluid-pipes（FluidTankGT+逐段泵送+防回流+熔融铁，merge 5ae1d6f）
- [x] W2：p4-machine-oven（首机 Oven+能量 A/C+进度=能量单位，merge 5b07953）∥ p4-render-foundation（RENDER_SNAPSHOT+GTDynamicBakedModel+双件套，merge aa9679b）
- [x] W3：p4-multiblock-framework（四路触发+checkAndSetTarget+CokeOven，merge 400732c）∥ p4-fluid-barrel（16000L 粘性罐+熔毁，merge 27e8cce）∥ p4-cover-core（CoverData+CoverPlateModel+零 GUI，merge 87badb3）
- [x] p4-pipe-flow-control（用户追加）：不自动握手基线+右键连接切换+shift 右键 ioMask 箭头+isOutputFace 外推门控+箭头渲染链（merge 0d21a59）
- [x] phase-closeout：ADR-P4 六条验收线全满足（根 188+mdk 199 测 / 合并态 RCON 全链 / runData 幂等 / GPG 全验）

## 第 5 阶段（管道语义 / 桶侧规则+泵盖 / 扳手九宫格 UI / RCON 工具链）——✅ 已完成（2026-08-30）

- [x] p5-rcon-tooling（免研究直派）：tools/rcon/gt6rcon.py 正典 RCON 客户端（279 行，帧协议对 vanilla 反编译核验）+ README 四节 + AGENTS.md 指针行（merge aa7d9aa）
- [x] p5-pipe-flow-semantics：ioMask==0 恢复 GT6 全外推（externalPushAllowed 谓词）+ 箭头面静态拒回流（SideFluidHandler 包装层，管↔管均压零触碰）（merge 4b415c8）
- [x] p5-wrench-ui-gtceu（researcher→architect→coder）：GTCEu 九宫格扳手 UI——红线双判据修订（瞬态 overlay 例外）+ 三件套 + 3 张贴图 LGPL 署名 + hover/click 同源（merge 18b7fe5）
- [x] p5-barrel-side-rules（researcher→architect→coder）：桶六面可入 + 重力排放（底排重/顶排轻/侧只进，1000 L/tick）+ CoverPump（单向门+直调 host 罐）+ gt6:natural_gas 验收载体（merge 7e7dbbb，13 提交）
- [x] phase-closeout：根 188 + mdk 229 测全绿，RCON 链合并态复放 pipe 19/19 + barrel 38/38，runData 幂等，GPG 全验

## 第 6 阶段（入口：CokeOven 加工 / oven 旋转 / 桶族）——✅ 已完成（2026-08-31）

- [x] 研究卡 p6-research-rm-cokeoven：RM 配方系统全景（85 图/Recipe.add/findRecipe 全貌）+ CokeOven 加工侧考古（BasicMachine 1036 行/TU 自发电/并行 16/39 条配方）+ 关键证伪（流体输出不撞 P5 侧规则：fill 六面全开仅 drain 受门）
- [x] p6-oven-rotation：GTCEu setFrontFacing 经扳手九宫格（use 分支 cover 门后插入+UT6 同源+ovenCellIcon 纯函数 432 断言）+ 第 4 张贴图借用 + 渲染器 :113/:151 顺手清（merge 7d7a73f，6 提交）
- [x] p6-barrel-metal-plastic：木桶恢复上游 isDecorative 逐字谓词（泵盖拒/plate 过）+ gt6:barrel_plastic 32000L + gt6:barrel_metal 64000L（容量熔点走块载体）+ P5 基类冻结面零触碰（merge cd46e3a，4 提交）
- [x] p6-cokeoven-processing：TileEntityBase10MultiBlockMachine 基类 + TileEntityCokeOven 改继承（tY-2 层 3x3 UP 面扫描）+ COKE_OVEN 图静态 24 行 + **用户裁定：原木配方 #minecraft:logs tag 驱动**（40 条活证）+ gt6:creosote + /gt6multiblock input|ignite（merge 9839b39，7 提交）
- [x] tools/rcon README 增补帧尾 NUL 读帧纪律（e8da825，纠正 P5 锚点失真措辞）
- [x] phase-closeout：批量 review-merge 三连审全 approve（根 188+mdk 263 全绿 / RCON 17+32+19 合并态复放 / runData 幂等 / GPG 全验）

## 第 7 阶段（cokeoven 回补 / 高档鼓+熔点桥 / 机器族 / 能量网 D1·D2）——✅ 已完成（2026-08-31）

- [x] p7-cokeoven-backfill：gt6:oil 四 DR 组 + 油页岩 8 行回补（poured 24→32）+ Row 泛化 + input 泛化 + findMaterial mID>=0 门（merge 55ecfef，4 提交）
- [x] p7-barrel-high-tier-melt-bridge：材质熔点桥 mMeltingPoint×1.25（Bronze 1696K 活证）+ 12 高档鼓梯 128K→10B（Infinity 1e9K 显式 HU）+ 共享 BET 多挂 13 块（merge 5ff9a5e，3 提交）
- [x] p7-d1-energy-core：根模块 ITileEntityEnergy 14 方法面 + Util 三件套 + EnergyBridge 缝 + EnergyGate + CS 三常量（merge dfb4a30，5 提交）
- [x] p7-gui-family：三机台 PNG 借入（上游 CC0，sha256 全等）+ 小写命名（merge 26a3c77，1 提交）
- [x] p7-recipe-maps-shcl：SHREDDER/CRUSHER/LATHE 三图 + 首批静态行（Crusher 宝石链 545 对账）+ AIR 幽灵配方修复（merge 2fda5bc，3 提交）
- [x] p7-d2-cable：GTWireBlockEntity 直译（09Connector 零提缝）+ GTWireBlock 64 变体 + 2 变体 + /gt6wire + 超压不烧语义锁（merge f77c7a3，5 提交）
- [x] p7-basicmachine-family：TileEntityBasicMachine 共享基类（并行段恢复+能量数学两分支+功率帽活语义）+ 三机 BE/Block/Menu/Screen + /gt6machine 三链（merge d0a1ebe，6 提交）
- [x] phase-closeout：根 205 + mdk 318 = 523 全绿，RCON 各链合并态复放全过，三次环境中断断点恢复零损失，D3∥D4 按用户指令移 P8

## 第 8 阶段（能量网端到端 / PrefixBlock 方块宇宙 / 机器族收尾 / CokeOven GUI+流体罐）——✅ 已完成（2026-09-01）

- [x] p8-d3-energy-consumer ∥ p8-d4-energy-source：Oven doInject 真实现 + mdk Root 默认块/overcharge + ENERGY_FAKE_SOURCE 开关 ∥ 测试发电机 + /gt6energy + canConnect 双探针回补；**gen→wire→oven e2e 五项闭环**（完整冶炼/wattageLast=30/超压 16 strikes 烧线/超流 2A/回归无损）（merge 2e87186 + e2bce89）
- [x] p8-prefixblock-registry：GTMaterialBlocks 3773 对七前缀注册家 + 7 创造栏 + cokeoven block 7 行回填 poured 39（merge 2e79c7e，8 提交）
- [x] p8-recipe-chances-orechain：Recipe.mChances + Crusher ore 链静态化 poured 493 + addRecipe 双空守卫（幽灵配方根修）（merge c69c09b，6 提交）
- [x] p8-cokeoven-gui-menu：Menu Host 接口化 + GUI 串小写地雷修复 + cokeoven.png + 10 交互槽（merge 63e3f7d，7 提交）
- [x] p8-machine-tiers-doinject：T2-T4 三机全梯 9 块 + 基类 doInject/alternating 恢复 + KU 负脉冲过零双态（merge 6b258f2，6 提交）
- [x] p8-prefixblock-render：175 共享模型 + 11494 datagen JSON + 灰度 PNG + tint + loot 自掉表（merge a01fa07，7 提交）
- [x] p8-cokeoven-fluid-capability：drain-only 流体罐 capability + P6 推液守恒活证（merge 7516c3b，3 提交）
- [x] phase-closeout：根 205 + mdk 415 = 620 全绿，RCON 各链复放全过，zcode bug 断点恢复零损失

## 第 9 阶段（线缆全谱系 / 渲染 C+D 档 / cover 红石钩+正式 crowbar / 配方翻案+命令小修）——✅ 已完成（2026-09-01）

- [x] p9-command-gui-polish：/gt6energy 四 op FAILED 字面量 + GTBasicMachineMenu 两条 javadoc nit + 桶 javadoc P7 现实化（merge 34a4f57，2 提交）
- [x] p9-recipe-yield-reform（M2 哨兵产量翻案）：buildRecipe base 双 10000 槽 + blockRaw extraCopies 8→7（构成严格对位上游）+ poured 493 不变 + RCON 活证双份主产出（merge 794bb1c，1 提交）
- [x] p9-render-d-formed-look：census 负结果（上游无 cokeoven 控制器贴图组）+ javadoc 勘误真命题收缩 + FORMED 双模型增强钉死（merge 55242e2，1 提交，打回→amend→复审过）
- [x] p9-redstone-hooks：ICover 红石三钩框架 + ICoverableTE 两出口 + GTOvenBlock 信号桥 + RedstoneHooksTest（merge 5bca6e6，4 提交）
- [x] p9-render-c-oven-overlay：GTOvenRenderSnapshot + overlay cutout 动态渲染 + per-state MRL 键控机制发现（merge e4bedea，3 提交）
- [x] p9-tool-crowbar：正式 crowbar（ToolAction+useOn 直派 OR 门零迁移）+ /gt6tool dismantle + 贴图借入（merge e93ea25，5 提交）
- [x] p9-redstone-cover-emitter：首个真红石盖（出向双覆写+16 键区+cutter strong）+ /gt6cover signal（merge a76c9b0，5 提交）
- [x] phase-closeout：根 205 + mdk 484 = 689 全绿，RCON 各链复放全过（W2 链 48/48），runData 二跑 written:0
- [x] 线缆全谱系：16 线 5 缆 620 块+真材质渲染 **landed**（W1+W2 双卡闭合 2026-09-01，merge 59390be+ea285e3）；剩余全部入池：cutter 交互（归 p9 工具族池，接口预期=IBlockToolable 链，ADR ⑤）/ IC2 拉取·FE 桥缝（onTick 同位 javadoc 缝在位）/ 红石·激光·物流线族（能量语义不同，独立后续卡，ADR ⑩）；runClient 目视入池：材质色差/绝缘层/连接臂几何
  - [x] **W1 电线索数据驱动铺开（闭合）**：GTWireSpecs 30 材质行直译 addElectricWires（WireElectric:71-109）+Loader:1914-1950，V 表 CS.java:148-154 逐字；620=28 材质x21+2 纯线 x16 零裁行（ADR 2026-09-01-p9-wire-family ④）+GTWires 静态循环 620 对 Block+BlockItem（stacksTo 64/n 梯+缆字面 64/32/16/8/4）+GTWireBlock 载体扩 material/size/insulated/diameter（W2 种子）+use=PASS 显式语义锁（cutter 走 IBlockToolable 非扳手，GTWireBlockUseLockTest）+GTWireBlockEntity 传输/燃烧数学零 diff（P8 语义锁；IC2 拉取/FE 桥缝+aChannel bundled 两占位仅 javadoc）+/gt6wire 三形态选择器+datagen 每块 1 blockstate（空 variant 键通配，禁 64x620 爆炸）+620 item/loot/lang。数字疑点闭合：coder 报 422/424 系 rebase 前 stale 口径，合并态实测 mdk 456/0（447 基线+新 9 测）+runData 二跑 written:0；64EU=32EU 烤炉 overcharge 上限/更大包炸炉连坐实证入 KG（merge 59390be，4 提交 99df23d..01e298f）
  - [x] **W2 连接感知 BakedModel 真材质渲染（闭合）**：GTWireBakedModel=上游 10ConnectorRendered:113-140 直译（pass0 内芯+pass1-6 六向臂 quad，tLength=0 臂到边界、:139 埋面跳过、:138 外帽 getTextureConnected/侧壁 getTextureSide，64 形状缓存，mask 全走 BlockState CONNECTIONS 禁 ModelData/BEWLR=ADR ⑨）+addWireFamily 模型目标改造（单空 variant 键法不变，7 共享 set 模型=30 材质行 census 恰 copper/shiny/metallic/dull/quartz/rad/none）+13 PNG 借入（7 wire.png+6 INSULATION 绝缘罩，sha256 审查官复验 13/13 字节一致零造图）+tintindex 染 fRGBaSolid+绝缘灰(64,64,64) 叠层（WireElectric:237-238 梯逐字，0.002 epsilon cutout 防 z-fight）+item=mask 12（:107 worldObj==null SBIT_S|SBIT_N）+GTWireClientListener per-state MRL+item 双路由（render-C per-state 键控纪律）+ELECTRIC_WIRES_TAB 620 全员+GTWireCommand javadoc 勘误。**P8 钉测窄域化裁决 ACCEPT**：175 钉值按 prefix 引用集（production walk 派生）过滤=防漂移原意保持，无散图方向由 GTWireTextureCensusTest 3 例补位（13 张存在性+7 set 精确结构+wire.png 字节哈希），后续共享目录借入各带自家 census 成惯例。门禁复审：mdk 484/0（456+17 新+11 covers）+根 205/0+build 绿+runData written:0（620 bs/7 set 模型实测）+RCON 48/48 W1 链复跑（渲染改造零行为变化：GTWireBlockEntity/注册循环/onRemove 零 diff）。审查观察（非阻塞）：上游 :108/:114 mDiameter>=1.0F 单 pass 全方块特例未单独建模，16px 缆连接面靠等深 quad 确定性覆盖呈现上游观感，后续池可补 javadoc 声明（merge ea285e3，6 提交 766063d..e826a0d，ADR 2026-09-01-p9-wire-family ③③(c)⑨）
- [x] **C 档 oven 动态渲染升级**：GTOvenRenderSnapshot(active,running) 自足 immutable+overlayGroup() 复刻上游 :1014 pick（mActive 恒优先 mRunning，worldObj==null 声明非移植）+GTModelProperties.OVEN_SNAPSHOT 第二 ModelProperty（与 cover RENDER_SNAPSHOT 共存不覆盖，TileEntityOvenCoverTest 13 例回归绿）+GTOvenOverlayModel（solid=null 沿 A 档 fallback/cutout=null 状态 overlay；planner=CS.java:528-537 FACING_ROTATIONS 全表逐字+0.002 epsilon 满面 slab+cullface）+GTOvenClientListener 16 per-state MRL 注册（**设计偏差审查 ACCEPT**：卡面单模型路由在变体方块上被静默跳过——vanilla ModelBakery.java:136/141 per-state 键控机制属实，模型文件路径不是烘焙 map 键；复用 GTRenderModelListener 注册面无双轨）+TileEntityOven getModelData 双快照纯投影+load() 客户端唯一写点置脏+GTRenderUpdates 双件套（服务端走 applyVisualState setBlock(3)，GTOvenBlock/GT6BlockStates 零触）+12 PNG 借入（census 24 张全实存，sha256 审查官复验 12/12 字节一致；colored+inactive overlay 组按 ADR 材质沿 fallback 裁剪=声明偏离入档 README，D 卡表尾双保留解决核验）。门禁：636 测全绿+build 绿+runData 二跑 written:0+runServer Done 零 ERROR+RCON 7/7 三态 blockstate 断言。遗留：cover 板动态模型 per-state 键缺口（cover 域禁触仅 doc 勘误）+colored/mRGBa tint 全保真层后续增量（merge e4bedea，3 提交 8d23bf1 30ec80b 223d361）
- [x] **D 档多方块成型态渲染**：census 负结果闭合——上游快照无 machines/multiblockmains/cokeoven/ 贴图组可借（控制器注册的四组图标路径上游本即 missing），按勿造图红线降级为声明偏离：FORMED formed/unformed 双模型保留（RCON 断言面，不换 GTCEu IS_FORMED）；getTexture2 javadoc 勘误按审查裁决收缩为真命题（初稿"all 20 mStructureOkay consumers 零视觉"被 LargeTurbine:109-111 getRenderPasses2 / Crucible:628-638 setBlockBounds2 两处视觉消费证伪；收缩后=无一消费者按 mStructureOkay 选贴图组）（merge 55242e2，c8efdc0，ADR 2026-09-01-p9-render-d-formed-look）
- [x] **红石钩框架（cover intercept 族拆注·闭合）**：ICover+3 红石钩（上游 ICover.java:190-192 逐签名）+AbstractCoverDefault 三默认（:78-80，getIndirectPowerLevelTo 对位偏离已声明）+ICoverableTE 两出口（入向 04Covers:409-424+Root:577-588 三分支/出向 :427-438 OPOS 翻转+机器默认值 bind4）+GTOvenBlock getSignal/getDirectSignal 载体桥（bridgeSignal static 纯函数，P6/P8 基类零触碰）+RedstoneHooksTest 10 用例（OPOS 六向真值表防反转）；上游承载勘误=TileEntityBase04Covers:409-441（卡面 06Covers 系笔误，审查核验成立）；CoverData/TileEntityOven 零 diff；item/GUI 钩仍冻结（merge 5bca6e6，4 提交 a16a034..daeb744，ADR 2026-09-01-p9-redstone-hooks）——【全段闭合 2026-09-01：三卡全 landed=红石钩框架 5bca6e6+正式 crowbar e93ea25+C emitter a76c9b0，唯一下游 emitter 落地即收官，剩余仅冻结池 item intercept 族】
  - [x] **正式 crowbar（GT6 工具系统入口·闭合）**：GT6ToolActions.CROWBAR（ToolAction "gt6_crowbar"，红线=永不 HOE_DIG，hoe 三谓词扳手 UI 零混入）+GTCrowbarItem（useOn 直派 ICoverableTE.onCoverToolClick(ICover.TOOL_CROWBAR,…):246-247 既有 OR 门零迁移，ICover/ICoverableTE/CoverData 零 diff；crowbarToolClick 单源分发供 /gt6tool dismantle 复用=验收面与玩家面一致；攻 2.0 保留）+GT6Tools 自持 DeferredRegister（gt6:crowbar 耐久 512，无 tab）+/gt6tool dismantle（RCON 验收：install→dismantle 断 toolDamage=10000+crowbarDamage=1/512+coverInInventory→hoe 路径回归）+上游 CROWBAR.png 逐字节借入（sha256 审查官复验一致，README 署名）+CrowbarTest 6 测（分类器红线/id 直派/id 单独成立/无 id 闭门/hoe 回归/耐久映射）。声明偏离（ADR ⑦）：广播链扁化 useOn 直派（GUI 宿主 block.use 先吞点击，游戏内右键不可达，验收主路径=命令）/10000→1 耐久点/单钢级 512/每格 50·每击 200 折单点/canBlock 格挡剪除/染色不移植。工具族池卡移交：挖掘扩展（rails/circuits/openableCrowbar）/配方 hVS VSV SVf/材质梯度/创意 tab/运行时染色（merge e93ea25，5 提交 a19bce0..1ab706f，ADR 2026-09-01-p9-tool-crowbar）
  - [x] **C 红石 emitter 盖（闭合）**：CoverRedstoneEmitter——上游 136 行直译：出向双覆写直接返回不并机器默认值（weak=bind4(visual) 档位道/strong=mValues!=0?weak:0，:55-62）+裸右键 16 键区调档（:72-109 逐字，上左-1/上右+1 双 wrap/下排 ^8^4^2^1，facingCoordsClicked+PX_P/PX_N 类内纯函数对位）+cutter strong 门切换（TOOL_CUTTER 类内常量返 1000，ICover 冻结面零触碰）+入向 getRedstoneIn 零覆写（上游无此方法实证）+attachment 旗标内联（intercept 双 F+opaque/sealable 双 F，基类 onCoverClickedLeft 本 F 中性省略）+needsVisualsSaved 档位持久；GT6Covers 注册（cover_pump 同构 ITEMS DR）+/gt6cover mode cutter relay+signal 验收子命令（直写 covers.visual(...,true) 声明偏离，报告带 host 出口 weak/strong 读数）；17 张贴图借入 textures/block/redstone_emitter/（underlay 逐字节一致+16 档位图离线 source-over 合成=BlockTextureMulti 双层单 sprite 承不住的声明偏离，vanilla 块图集 directory 源零接线自动入图集，README 记上游 sha256 全表）；CoverRedstoneEmitterTest 11 用例（出向真值表 default-blind 证据/键区 zone 真值表含 wrap+四类 miss+异面盲区/cutter 计数/入向直传查询计数/NBT 往返）。声明偏离：magnifyingglass 读数与 onToolClick2 host-relay 臂不复刻（非 cutter 返 0）。六冻结文件+hoe/GT6Mod 零 diff；冲突标记事故（72a701b）当场软重置修复，终树零残留不在链（merge a76c9b0，5 提交 775f74a..2456d25，mdk 467/0=main 基线 456+新类 11 对账闭合，ADR 2026-09-01-p9-redstone-cover-emitter）
  - [x] cover item intercept 族——P11 消费盖五件落地四件（Shutter/FilterItem/Conveyor/RobotArm，见第 11 阶段段）；RetrieverItem 池化等 item pipe；左键/GUI 钩=研究员双死代码实锤不复刻
- [ ] barrel 密封发酵 + 连通罐 B[0]；**P12 更新**：tap/funnel ✅（2f3fdad）/桶 GUI 终裁不做（上游正典即无 GUI）/破坏倾倒证伪（上游无此物，内容随掉落物品 NBT）/Gas-proof 四防=声明偏离+P13 锅炉卡还账（POWER_CONDUCTING 实证）
- [ ] 按需 researcher→architect 开新域（权威池=state todo.pool）

## 第 10 阶段（debug 修复 / 红石线族+触电 / cutter / cover 红石盖族 / ghost 预览 POC / vanilla 配方行 / Laser 占位）——✅ 已完成（2026-09-01）

- [x] p10-debug-oven-cover-resurrect：known_bugs 首项关闭——causeBlockUpdate 虚分派被 Root final 遮蔽→装拆盖不 setChanged→盘上残留盖存活期 NBT 复活；修=setCoverItem 无条件 setChanged+sendBlockUpdateFromCover 持久半边（merge 432cfd5，3 文件 +69/-3，三连重启双点位全 null）
- [x] p10-wire-redstone-family（R1）：红石族三材质六方块 push BFS verbatim 值存 BE（BlockState 零新增 property）+GTWireBlock 三桥+spec family 列+BET fallback 分流；RCON 衰减笔算逐位吻合（Signalum=MAX×15−3×(MAX/64)）+strong 桥 scoreboard（merge dfc7c73，9 提交）
- [x] p10-wire-contact-damage（E1+四 ride-along）：entityInside 触电+2px 碰撞内缩机制（checkInsideBlocks 只在 Entity.move 内跑=内缩是钩子前提）+tierMax×4 全梯真值表+未传电不咬（mWattageLast 不持久化=上游空体 NBT 语义）；ride-along=红石 6 块 loot+load javadoc 偏离声明+connect :130-140 视觉连接分枝+soak 5/5 零 flap（R1 flap 遗留闭合）（merge 6b7226f，5 提交）
- [x] p10-cover-conductor-redstone：ConductorIN 纯标记+ConductorOUT 穿机导线（onBlockUpdate 扫全脸 IN 取 max 直写）+GTOvenBlock.neighborChanged 分发缝接活（merge ca435ce，5 提交）
- [x] p10-cover-controller-redstone：CoverControllerRedstone 五臂内联+ITileEntitySwitchableOnOff+TileEntityOven 一行 implements+极性手工验真（=上游 Runs when OFF）+screwdriver bit0 返 1000（merge b86e272，6 提交，叠基 conductor）
- [x] p10-cover-item-intercept：ICover 解冻恰八钩+八默认+三宿主门+oven 侧感知 IItemHandler wrapper（宿主半边等价成立，盖拆即时生效）；ADR 2026-09-01-p10-cover-item-intercept（merge 8496595，2 提交）
- [x] p10-cover-plate-perstate-fix：P9 render-C 勘误落地——3 模型文件 id 死键→16 per-state MRL（GTOvenClientListener 零 diff 只读复用）+wrap/parity 哨兵测试（merge cee748a，3 提交）
- [x] p10-tool-creative-tab：GT6Tools 自持 'tools' tab 表驱动+lang（merge de3daea，3 提交，OOM 续跑现场零修正）
- [x] p10-tool-crowbar-mining：挖掘面 getDestroySpeed/isCorrectToolForDrops+rails/circuits 显式集 26 块（harvestTag 路线证伪裁决）+classifies 不动永不 HOE_DIG（merge 787c57b，1 提交，OOM 续跑补门禁）
- [x] p10-tool-cutter：CUTTER ToolAction+CUTTER_ID parity 钉死+useOn 双臂（wire 九宫格 toggle 走既有 API+cover relay）+九文件零 diff+RCON cut 三拍+EU 真断供（merge d98ce8b，5 提交）；payPerPoint 双调潜伏缝入 known_bugs（现值安全）
- [x] p10-ghost-preview-poc：cokeoven 27 格结构残影——RenderHighlightEvent 瞬态例外+pattern 独立纯表（checkStructure2 含 removeBlock 世界写禁客户端跑）+FORMED 只画外壳 12 沿（merge c25ed08，2 提交）
- [x] p10-compat-vanilla-rows：外域 mod 配方声明不复刻（59 Compat_Recipes_* 类）+vanilla 缺口 5 行（CRUSHER obsidian/netherbrick 四档/netherrack/endstone+SHREDDER bone）；chances 四档测试法=受控 Random 端到端活证（merge 503d242，2 提交）
- [x] p10-wire-laser-placeholder（L1）：Family.LASER 单行直译+transferLaser 纯壳（复活条件 javadoc）+inert 三不+EU NOTHING FLOWED 活证；Logistics 裁池（merge 376d6eb，3 提交）
- [x] phase-closeout：根 205 + mdk 580 = 785 全绿，runData 全程二跑 written:0，各卡 RCON 活证+审查侧复放；WSL OOM 中断四会话续跑零丢失+compat 漏派补派

## 第 11 阶段（wire 语义四件 / rotor 假电源退役 / cover 消费族五件 / RCON 框架三层化 / cover 双层渲染）——✅ 已完成（2026-09-02）

- [x] p11-wire-laser-loot：laser loot 补洞（原本破坏零掉落）并入共享 wire provider；卡面「16 块」证伪=恰 1 块（Loader:1814-1815 单 ID，EXPECTED_LASER_VARIANTS=1）（merge bc9317e，2 提交）
- [x] p11-cutter-payperpoint：known_bugs #1 closed——上游唯一计费点 Behavior_Tool.onItemUseFirst:60/:63（relay 全裸返）→内层重载改纯 relay；计数桩正反验证（merge 84e451d，2 提交）
- [x] p11-connector-stale-mask：known_bugs #2 closed——「只剪不连」（红石行 keep=canConnect 恒真）+updateShape 唯一全覆盖缝（/setblock flags=2）+onTick 兜底；根因=CONNECTIONS BlockState 契约违背（上游掩码只驱动贴图）（merge 72636d4，1 提交，RCON 7/7）
- [x] p11-wire-fiber-texture：FIBER_WIRE/OVERLAY 两 PNG 借入+GTWireBakedModel laser 分支（base 染色+overlay 双生子）+laser per-state MRL；fallback 行留载体（merge 39340f8，3 提交）
- [x] p11-gt6machine-literal-fix：/gt6machine 双注册劫持（Brigadier addChild 同名 literal 静默合并）→chest 改 **/gt6chest**+双根不相交测试（merge b6d839a，2 提交）
- [x] p11-flat-redstone-tab：Redstone Wires（27050，icon=Signalum 裸线）+Laser Wires（24900 单成员独立 tab，按上游证据裁剪）+电 tab 余 622 三表驱动（merge 1ecf0ca，2 提交）
- [x] p11-wire-brightness（R1b，唯一打回→复审过）：getLightEmission 读 BE（**worker-thread 契约 getExistingBlockEntity**，IForgeBlock:106-110）+onTickCheck verbatim+checkBlock+红石绝缘 0xFF604040 修 P10 错色+红石 6 块入 baked model+mState 不持久化（merge 62b2ee2，4 提交）
- [x] p11-rotor-source-flip：ENERGY_FAKE_SOURCE port-ism 退役（上游零命中）+:374 拆回上游 :815 逐字+KU 交替方波过零沿 RCON 活证+RU/KU=EU 引用等值门隔离不需新 EnergyType（merge 542d7a5，3 提交）
- [x] p11-item-mover-helper：GTItemMover=ST.move 八锚点子集（simulate 探针 hook 零落账+同槽 abort verbatim+裁剪 7 项锚行号）+21 真值表（merge 5308c78，1 提交）
- [x] p11-infra-hygiene-bundle：gt6rcon allow_failed（默认路径逐字节同构）+javadoc 中英混入 13 处修正（主流=纯英文）+mEmitting harness 注记+Root.isServerSide 勘误（merge 0868a4b，3 提交+审查 tab fixup）
- [x] p11-cover-shutter-filter：CoverShutter 四拦截 (visual==0)==mStopped+CoverFilterItem（census 勘正类名）白黑名单 gt.filter.item→mNBTs；4 管线臂声明裁除；RCON 27/27（merge 73d8881，2 提交）
- [x] p11-cover-controllers：AutoRedstone 不等式/ControllerCovers 相等式互反对拍+双形 instanceof 读机器字段（机器类禁触）+setStopped 跨面中继；RCON hold 不可观察裁决（merge b69fd52，2 提交，RCON 35/35）
- [x] p11-cover-conveyor-robotarm：**512>>i=tick 周期裁决**（SERVER_TIME%mTiming）+arm 四象限 ignoreSide 逐位+4 动画 PNG+10+10 注册；审查 follow-up c394548 补收 controllers 漏跑 runData→**provider 必跑 runData 纪律**（merge b43be51，4 提交，RCON 30/30）
- [x] p11-rcon-framework（用户指示）：gt6server/gt6world/chains 三层——漏站点必红结构保证+--stop/pkill 禁令模块强制化+链条入库；试点 shutter 链迁移两遍 66/0 幂等（merge 5d7c143，6 提交）
- [x] p11-render-cover-multilayer：census 反证 14 盖全 base+fg 双层（06Covers:449-463）+snapshot 层表+PLATE_EPSILON*layer 不进 UV+layer0 逐字节等价承重墙+2 PNG 补借 SUPERSEDE（merge 2f3fe44，2 提交）
- [x] phase-closeout：根 205 + mdk 705 = 910 全绿；三张研究卡先行（R1b 五子命题/cover 残余批/rotor 族）；一次打回复审演化；用户「干完先停」暂停点一次后复工

## 第 12 阶段（多方块 ghost 全量 / 流体罐引擎链 / 基建池余项）——✅ 已完成（2026-09-03）

- [x] p12-hygiene-style-adr：CJK 全扫 43 行/26 文件（42 译 1 留=断言字面量裁量）+docs/adr/ 目录建立+p10-cover-item-intercept 首篇转正；25 文件剥注释 token 流逐字相同（merge 33eaab1）
- [x] p12-ghost-pattern-api：GTMultiBlockPattern 声明 API（谓词缝+三类动态逃生舱）+controller getStructurePattern 默认缝+cokeoven 绑定（checkStructure2 逐字节未动）+顶点流与 POC 逐位等价（merge 248c66b）
- [x] p12-ghost-render-match：整面半透明+绿红分色（debugQuads 零新 RenderType+内缩 0.002+最小集语义+hollow AIR 谓词非硬编码）；FORMED shell 位等价零变化（merge dbc55ef）
- [x] p12-jei-integration：首个第三方依赖 JEI 15.56.0.205+GT6JeiPlugin 自持+coke oven 信息页（lang 走 datagen）；@JeiPlugin CLASS 保留级→字节码层断言；砖数 25 勘误（merge 6be2862）
- [x] p12-engine-fuel-fluids：九流体表驱动（steam 气型/蒸馏水/柴油族/酒精）+ENGINE_FUELS 图直译；STEAM_PER_WATER=200 引擎私有归属钉死（CS:242=160 全局另存）（merge 250422a）
- [x] p12-engine-crank：手摇曲柄 RU 恒负号直流源+GT6Kinetics 注册家+/gt6engine 命令家；e2e 52 断言（merge 0a15759）
- [x] p12-fluid-item-carrier：打桶丢液修复（getDrops 上游同缝；桶族 loot-table-less 实证）+FLUID_HANDLER_ITEM 物品面（'tank' 键偏离）+/gt6tank fill·drain·show（merge c00d3b3）
- [x] p12-axle-family：RU 传动轴 44 行全表邻接递归零损+超速 popOff 原额退回+AXIS 三向 port-ism（merge 93b5913）
- [x] p12-barrel-keepfilter-logistics：Logistics Tank BE 两 override+FluidTankGT 六点修复（审查裁定=上游原文恢复，卡面零 diff 前提勘误）——0 量身份保留闭环（merge d79440c）
- [x] p12-tap-funnel-attachment：Tap/Funnel 贴面附件两族（tap 六段优先级链+锅三档+XP/Mob+手持容器；funnel simulate 门）+桶两钩恢复（merge 2f3fdad）
- [x] p12-engine-steam：EngineSteam **28 变体**（卡面 26 系笔误）蒸汽→KU 交流方波+活塞相位持久化+蒸馏水环；注汽直打背面进汽门；**首条全链闭环 蒸汽→KU→crusher**（merge 8e28c3a）
- [x] p12-engine-diesel：MotorLiquid 8 档柴油机 FM.Engine→RU 直流+CO2 尾气双臂；consumeFuel (F,F) 门=1.20.1 载体适配（自由能漏洞钉死）；**第二链闭环 柴油→axle→shredder 零损**（merge b3b9ac1）
- [x] p12-gearbox-transformer（唯一打回→返工过）：GearBox 拓扑/齿爆/分流+Transformer ÷4×4；**打回=converter waste leg 未移植**（上游漏斗语义）→返工补 leg+归零断言复审过（merge 41721ac0）
- [x] 基建：审查会话轮换制入宪法（3cbece7，单会话 ≤3 分支）+审查模板 -s 修正（975aadc）+双 ADR 转正成文（20bb36d+5db3bdb）
- [x] phase-closeout：根 205 + mdk 846 = 1051 全绿；一次打回返工复审过；两审查会话轮换；两研究卡+锅炉研究+蒸汽求证四张先行

## 第 13 阶段（锅炉蒸汽族：还账四件 / HU·蒸汽常量地基 / 火盒五亚型 / 锅炉坦克 / 大锅炉多方块）——✅ 已完成（2026-09-03）

- [x] p13-steam-proof-repay 还账四件：GTFluidLists 名单地基（POWER_CONDUCTING/GAS，natural_gas 名单键=registry path 等价）+Base08 tick fizz 两查+gasProof 载体行四族+物品面 fill 双门+P5 基类解冻显式 ADR（merge 42d6220e；metal 注汽 tick 清零=P12 蒸汽偏离窗口关闭）
- [x] p13-hu-steam-foundation：EU_PER_WATER=80+STEAM_PER_WATER_GLOBAL=160（引擎私有 200 零 diff，四联断言机检）+BURN/FLUIDBED 图骨架+HU 握手 fixture 5 测零生产代码（merge 1e78600e）
- [x] p13-burning-box-family 火盒五亚型：97 块全发 HU（Solid 27/Liquid 22 单罐勘误/Gas 22 实证 FM.Burn/FluidBed 26 活表空声明）+RecipeMapFurnaceFuel=getBurnTime 动态桥（duration=burnTime×25）+BURN 7 行 pour+明火蔓延（累计计数器断言）（merge e405bef3）
- [x] p13-boiler-tank 锅炉坦克：26 材质档+转化式 GLOBAL 160（grep 零 200）+爆炸四触发+空烧不爆负断言+爆炸执行 Root 桥（O2 落定）+**正典链 e2e 闭环：火盒→锅炉→管道→EngineSteam→KU→crusher+蒸馏水副产**（merge cfcd9909）
- [x] p13-large-boiler 大锅炉：5 变体多方块+pattern 缝够用零 diff（part 格实 35+1）+五孔负载均衡（:247 先除后减=85/85）+Dense Wall 勘误+HeatTransmitter 独立 BE 转推+拆墙即炸/无压幸存双面活证+dismantle 臂（merge 35ec9b21）
- [x] phase-closeout：根 205 + mdk 943 = 1148 全绿；五卡零打回；每波新审查会话轮换制；--no-build-cache 排假绿入门禁纪律；decisions 账本覆盖事故重建+list 账本写纪律（ops.discipline.state_ledger_append）

## 第 14 阶段（蒸馏水闭环：机器流体罐面 / 图声明 / 锅炉免疫正典 / Dryer 四变体 / 正典闭环链）——✅ 已完成（2026-09-03）

- [x] p14-drying-distillery-maps：GT6RecipeMaps +DRYING+DISTILLERY 声明态（RM:70/:71 逐参保序）+RECIPE_MAPS 9→11（卡面 8→10 陈旧口径勘误）+GUI 小写化（merge c1846b33）
- [x] p14-machine-fluid-face：GTSideTables 直译（CS:528/:598 机械 diff 全等）+TileEntityBasicMachine 双流体罐+checkRecipe 接真罐+doActive 落罐半+:511 HU 接收侧门（缺省 127 零回归）+per-side FLUID_HANDLER（merge d27898fa）
- [x] p14-boiler-distw-immunity：免疫正典=:119 三 rng 臂恒 10000+清水对照 6400+DistW 单一性负对照+/gt6boiler efficiency·fill distw 臂（merge e574d114；免疫完美产量恰满罐 :148 确定性爆炸=上游语义正确，链改半罐轮）
- [x] p14-dryer-family：MachineRow 行载体+Dryer 四变体 Loader:1477-1480 逐参+applyRow 直写三掩码+menu=null 惰性门+命令树 pin 12→16+排序地雷修复（clinit ForgeRegistries）+活体几何全表首验（merge 76a524e1）
- [x] p14-loop-closure-chain 正典闭环：DRYING Water→DistW 行 pour（10→8 EUt16 t16）+水族 7 行池载+火盒→Dryer→锅炉免垢 e2e 实拍（800L 精确/PRISTINE 10000/128000 汽=800×160）——**正典水循环活体闭合**（merge adbf2ca1，崩溃后补验追认）
- [x] phase-closeout：根 205 + mdk 968 = 1173 全绿；五卡零打回；审查追加式运转（W1 三支一会话）；ZCode 崩溃一次恢复零损失；前置调研两张完成（电路载体形=damage int/跨版本=A′ Stonecutter 双节点）；kTFRUAddon 池料研卡（AGPL⇒clean-room）；sources.json 白名单补齐（d9b67e17）

## 第 15 阶段（跨版本兼容：Stonecutter 双节点 1.20.1 Forge + 1.21.1 NeoForge）——✅ 已完成（2026-09-04）

- [x] 前置：调研卡 p15-research-crossversion（影响面粗分级）+ 裁定卡 arch-crossversion（A′ 十四卡六波总拆+ADR-P15-1..10）+ 三 POC 先行：poc-java21-toolchain（javac21 0 错/968 绿，main 钉 17 不变）/poc-chisel-density（195 文件普查 62 语义 hunk 31.8%/结构级 34=17.4%+AST 三证据）/skeleton POC（钉版 0.7+L2 挂法+v0 清单 156 文件 2652 错）（merge db798430+d2018ddb）
- [x] p15-stonecutter-skeleton（W2 独占）：mdk 降 Stonecutter 控制器+双节点矩阵+swap 表初版 38 条（regex reverse() 哨兵）+双模板 mods.toml+daemon 2g+v1 门禁 125 红文件落账（merge ed4c7559）
- [x] W3 分叉波：fork-capability-machines（6 文件+153 纯增量+nf seam getCapability，126→123，merge cf873b27）∥ fork-capability-core（Root (CompoundTag) 链成员保留定式，125→121，merge cb4c9788）→ fork-carrier-components（GT6DataComponents+桶/盖/CoverData 载体化 8 提交，getComponentsPatch 空判审查修，merge e03ddfd9）
- [x] W4 注册·datagen 波：adapt-datagen（swap 三类真值勘正 common.data+provider 家 7 文件，111→104，merge 511af05c）∥ adapt-registry-core（GT6Mod 构造注入+GT6CapabilityWiring 自持监听+SeamTest，111→91，merge 411c7517）→ adapt-registry-blocks（8+1 注册家 DeferredHolder+18 codec 洞+swap 21 条，84→65，merge 5fe6d7ac）
- [x] W5 client·命令·JEI 波：jei-dual-wiring（1.21.1 三件 19.52.0.422+字节码守卫，65→64，merge a30014a3）∥ adapt-commands（capability 消费腿+NetworkHooks→openMenu，65→59，merge 1d29faf4）∥ adapt-client（RegisterMenuScreensEvent+MRL record 化+VertexConsumer swap 13 条，65→52，merge 7cdbd98f）
- [x] M4 首绿四卡：m4-final-clear（六批 45 红文件→0，merge 60b1837e）/m4-test-infra（MDG unitTest FML 引导根治 clinit 级联，970 跑 853 绿，merge 64fd6893）/m4-test-infra-2（BE fixture 绑真 BET 115→36 红，merge 409e6fcd）/prod-fix（FluidTankGT.save 返回值+镜像键 36→22 红，merge 27b7f131）
- [x] RCON 拆分四卡（rcon-gate-split 裁定）：C1 rcon-dual-gate 结算（9+1 提交收编，merge 87e1ed0a）∥ B' cokeoven-png-fix（六 PNG 重生成+check_png.py，runClient 六 ERROR 清零，merge 3bfc28dc）∥ A rcon-session-perf（poll+session+并发波+线锁，R0 3852s→1524s 2.5×，merge b09efc82）→ D dual-gate-closure（node_cmds 键形分叉+p12fic 转绿+大锅炉归档+ADR-P15-4 双节点 [0,0]×2 闭合，merge a5337c38）
- [x] p15-closeout 五判据总账：1.20.1 节点 1176 全绿（--rerun-tasks --no-build-cache XML 实数）+1.21.1 编译 0 红；RCON 双门禁 JSON 逐字段复核（1495.9s/1567.1s FAILING=NONE）；JEI 双接线双证（字节码守卫+Mod List 日志）；密度总账 swap 87 条+//? 分叉 main 87/197+test 24 文件 vs POC 结构级 34——备选 C 未触发（红文件单调收缩 156→…→0）；1.21.4+/1.21.6+ 回池确认（ValueInput/ItemModel 面 grep=0）；**1.21.1 runData 实跑被预存已知 bug GTClientHandlers 抽象事件崩溃阻断（两次同因，非新回归，归 P16 客户端适配卡）**

## 第 16 阶段（多方块成型判定三件套 / 继续移植 GT6 六卡 / 21.1 追修两卡）——✅ 已完成（2026-09-05）

- [x] 裁定先行：p16-card-split（10 卡 4 波+收口，并行度≤3，gui+dryer GUI 合一、side-io+item face gate 合一）/p16-formation-scoping（成型判定能力取舍：CHECK/层 DSL/SET 补建入主线，phantom/红描边包/异步检查否决，七项入池）/p16-clienthandlers-typed-split；kTFRU 成型判定研究（AGPL⇒clean-room 机制借鉴）随 P14 收官已备
- [x] W1：p16-pattern-checker（Cell 成型期望三件套+GTMultiBlockStructureChecker 共享校验器+CokeOven 试点，merge 7ae62278）∥ p16-aqua-fluids（表驱动 AquaFluidSpec 六流体+lang walker，merge 00259c7e）∥ p16-machine-side-io（mAccessible[7] per-side 物品掩码+流体 auto-IO 拉推臂+NBT_TANK_CAPACITY+mCanUseOutputTanks，merge ce09d41e）
- [x] W2：p16-pattern-layers（layer+repeatable(min,max,factory)+family per-n build 期展开 DSL，merge b579b998）∥ p16-machine-fluid-gui（机器流体罐 GUI 面+dryer GUI 合一，menu=null 池约定接管，merge 7fd88971）∥ p16-drying-rows-backfill（DRYING 冰雪 12 行+水族 7 行，census 13/2/11，merge aa242411）
- [x] W3：p16-form-scaffold（form() 计划-执行两阶段事务 SET 补建+/gt6multiblock form，census 26→25 勘误，merge c6c9a4a2）∥ p16-clienthandlers-2111（抽象 RegisterColorHandlersEvent 拆 .Item/.Block 子类，解除 21.1 runData 阻断第一层，merge e396bdc9）∥ p16-blockstates-2111-prop-intern（GTBlockProperties 单一 owner+6 常量别名，ADR-P16-2，解除第二层，merge c0b0a87e）∥ p16-distillery-family（四变体+integrated_circuit Damage 载体+RM.DISTILLERY 行，21.1 节点 19 新测双腿实证，merge c852fa68）
- [x] W4：p16-chisel-decalcify（mDamage=25/付款门/爆炸臂逐字+units 数学全表 400→1/1000→3/5000→13/10000→25，merge 21be5daf）
- [x] p16-closeout 收口门禁（判据总账=ADR docs/adr/2026-09-05-p16-closeout-gate-ledger.md）：root 205+mdk 1073=**1278 全绿**（cleanTest --no-build-cache XML 实数）；1.21.1 compileJava+compileTestJava 双口径 **0 红**（--no-build-cache 真执行）；1.21.1 test 1072 实跑 22 失败与基线（类名,测试名,消息）三元组 diff **全等**；RCON 双节点=回归 19 链 sweep（2564.6s/2594.8s concurrency 1）+p16 新增 8 链 per-chain boot，1.20.1 **27/27 [0,0]**，1.21.1 25/27+两案 21.1 运行时 delta 记录（料斗→机器 item 面/跨机 auto-IO 面）；**1.21.1 runData 首次全序通过**（EXIT=0 零异常→恢复正典树→.cache 对齐跑 written:4586→终验 written:0）；docs 镜像+ADR 入库

## 第 17 阶段（21.1 机器 capability/IO 面收口 / datagen 共享树裁决 / RCON 框架五小修）——✅ 已完成（2026-09-06）

- [x] 裁定先行：p17-arch-datagen-tree（ADR-P17-1：输出目录按节点参数化——正典节点共享 tracked 树不变/21.1 输出节点本地 build/，五步门禁取代六步，恢复仪式全删；候选 2/3/4 逐案否决）∥ p17-arch-machine-io（**两 RCON delta 单根因**=GT6CapabilityWiring 漏注册 DRYER_BE/DISTILLERY_BE（P15 fork 时点产物不含后加机器家族，桶族 ADR-P15-4 同型缺口第二次）+ IDENTICAL-22 逐条裁决 19 fix/1 permanent/2 defer，拆两卡 files_scope 互斥）
- [x] p17-datagen-node-local-output（merge a2d0d7fa）：build.neoforge --output 节点本地+tools/datagen_tree_check.py 双树 byte 断言（loot_table↔loot_tables 映射）+ADR 落地；21.1 二跑 written:0 首次实录，porcelain 恒空（9170 污染形态绝迹）
- [x] p17-rcon-framework-fixes（merge 6a0958a2）：framework.run()+run_session_recorded 双路径 chain.node 回写+session_slug（名册+worktree 哈希防 pid 互踩）+SESSION_PORTS 尊重 preferred_ports+sweep 注册 p16 八链（全集 27 链 6 簇）+gt6server BootOwnershipError 归属门（assert_ports_free/assert_pid_file）+selftest 24 检
- [x] p17-2111-test-fixture-22red（merge 6e1d0d55）：五测试文件 fork 修 19 红（罐头玻璃 JSON 21.1 对象形双副本/CoverClient MRL.standalone 键取/MaterialBlocks leg-conditional 非空悬挂句柄/FluidBridge copper map-miss）——**IDENTICAL-22→3**，残集三元组逐条 MSG_IDENTICAL
- [x] p17-2111-machine-io（merge 3dee46ff）：GT6CapabilityWiring 补两族×{ItemHandler,FluidHandler}.BLOCK 注册（同 lambda 形零行级分叉）+SeamTest 9→11+BASIC_MACHINE_FAMILY_FACES 守卫常量表+census 测试（扫 GTMachines 全部 *_BE 防下一家族再漏）——两 delta 修复 21.1 RCON 实证（drying_rows snowball 入槽→250L DistW/side_io E 相平衡态，各双遍 [0,0]）
- [x] phase-closeout：root 205+mdk 1074=**1279 全绿**；1.21.1 compile 0 红+失败集 IDENTICAL-3；RCON 1.20.1 27/27+1.21.1 27/27 推定（25 绿基线+两 delta 链双遍实证）；runData 五步门禁 1.20.1 二跑 written:0；ADR=2026-09-05-p17-datagen-tree+2026-09-05-p17-machine-io-adjudication；口径提醒入账：裸 ./gradlew check 全节点聚合会因 21.1 残集 3 红 BUILD FAILED，门禁走显式任务路径

## 第 18 阶段（21.1 三残案清零 / P1 数据丢失闭合 / 小修批七件）——✅ 已完成（2026-09-06）

- [x] wave1 诊断裁决先行（四路并行后台）：keepFilter 裁决（ADR-P18 读侧重建：写侧双腿合同键 `{FluidName,Amount:0}` 不丢、21.1 腿 parseOptional 走 MAP_CODEC POSITIVE_INT 拒 0 量 orElse(EMPTY) 静默吞）∥ staticinit 诊断（两案同根=`GT6RecipeMaps.reset()` 只清 11 图不清 loader 灌注旗标×junit-fml boot 真跑生产 load()→毒态「maps=null×sLoaded=true」静默早退；案②单方法独跑即红）∥ formscaffold 观察（**P17 Delta 3 定性修正：非噪声是 P1 真实丢数据**——TileEntityBase10MultiBlockMachine:830 于 loadAdditional 阶段 NPE 被 vanilla 吞跳 setBlockEntity→多方块 controller 世界重启成死方块；全树唯此一类带病）∥ 小修批拆卡（七件归三卡；件⑤定位修正=TileEntityBasicMachine:1551-1552 load 腿，GTSideTables 掩码无缺陷）
- [x] p18-keepfilter-2111-readback（merge 62e2a7b1）：FluidTankGT 21.1 腿折叠点前 FluidName 反查重建（tryParse→FLUID.get→单位量载体重建，与 forge 腿 :87-94 逐语义同构；null/未知名/EMPTY 落空罐不抛）+KNOWN DELTA 注释改写+双腿垃圾名负例——BarrelTest 14/14 断言零改动转绿，**permanent-delta 撤销**
- [x] p18-rcon-sweep-quietwin（merge f315760b）：sweep 结果 JSON 掺 worktree_tag 隔离（对侧回读按对侧根哈希）+RconClient per-connection 自适应 quiet_window（单帧 ×0.7 衰减/0.05s 硬下限/见第二帧重置/GT6_RCON_ADAPTIVE_QUIET 关闭开关）——selftest 24→34 检，真机双跑 wall 141.6s→90.5s（0.5s/条残差消除实证）+`sweep --diff` IDENTICAL 50/50
- [x] p18-mdk-review-hardening（merge 98757672）：NOTHING=~EVERYTHING 哨兵注释+normalizeAutoIOSide 值域归一化（仅 0..5/-1 合法，出域折叠 SIDE_UNDEFINED；99&7=3 别名真面回归双题新测）+GTMultiBlockPatternFamily.mExpansions→ConcurrentHashMap+DISPLAY_CONTAINER static 改 per-menu
- [x] p18-staticinit-generation-reset（merge 8a607b7d）：ADR-P18 案 A 世代化重置钩子注册制（reset() 持 CopyOnWriteArrayList 钩子表+registerGenerationResetHook 包私有+7 loader（毒态全集超诊断点名 2 个）static 注册+早退仅增 LOGGER.debug；否案 B：案②结构性不可实现+自愈重灌是新增静默路径）+两案测试自 grounding+GT6RecipeGenerationGuardTest（canary 修复前 2/2 红实跑留痕+结构钉 7 钩活账本）
- [x] p18-beload-2111-nbtaccess（merge 21ceab6e）：ADR-P18 裁 A 先落——:808 saveAdditional+:830 load 换冻结静态 NBT_ACCESS（等价性读码证实：provider 恰三消费点 :831/:843/:848 纯 id 查找面；同面其它 BE 类 21.1 生产跑通），forge 腿 :749-795 字节不动；验收=序列化快照 A_pre==A 逐字节+死因路径 2 ERROR+2 WARN→**0/0**+重载快照 A2==B2==C2 全键恒等；根治登池 tasks.pool-w4-nbt-provider-threading
- [x] p18-rcon-oven-power-arm（merge fc494c6b）：p16_pattern_checker B 臂改 grid-fed 供能步（/gt6energy place|volt|mode+/gt6wire place|neighbors 正典命令面，idiom 出处 p8e2e 链；真通电 neighbors 证据非 NBT 作弊）+**run 步整体摘除（二审亲读 GTOvenCommand:143 裁断 ACCEPT**——run 只驱动 oven 自身 dispatcher 不 tick 世界，done=mSuccessful 采样瞬态与 idle 对 grid-fed 连续源不可同框，KG 边 UNSATISFIABLE_ON；poll 硬断言 input=airx0 output=stonex8 严格更强）+sweep run_and_record 补聚合 exit 键修 perboot KeyError+selftest 39 检
- [x] phase-closeout：root 205+mdk **1081**=1286 全绿（逐卡对账 1077+staticinit guard 3+beload 新测 1）；1.21.1 compile 0 红+test **1080/0 failed=IDENTICAL-0 首次达成**（Barrel 由 keepFilter 修/EngineFuels·OreChain 由 staticinit 修，三残案全清；skip 2=GTGeneratorSolidBlockEntityTest 基线既有）；RCON p16_pattern_checker 实弹 [0,0]（energy=31 真通电态）；runData 免（本波零 provider 触碰）；ADR 三篇成文=2026-09-06-p18-{keepfilter-ruling,staticinit-poison-fix,beload-provider}；known_bugs 第三条 multiblock-2111-be-load-npe closed

## 第 19 阶段（移植批：凿石宇宙 / DRYING 第二批 / distillery front 正典化 / chisel 配方+右键门 + B' 动态注册表重绑 + RCON 框架小修）——✅ 已完成（2026-09-06）

- [x] wave1 四路并行（3 研究+1 裁定）：stoneChiseled census（17 凿石=BlockStones 16-meta 族 CS.java:1668 编译锚定；TOOL_chisel 通用门 ToolCompat:224-229 未落=总入口）∥ circuit census（**上游无编程器**=P14 遗留说法纠偏，真实交互面=crafting 25 行+cover 3 配方+ST.tag 零耗消费；256 梯真贴图仅 25 张；distillery front 真身 CC0 但 overlay 透明需 bake）∥ DRYING census（RM.DRYING ≈137 语句 P16 仅盖 Chem:510-532；可回填=矿物 8+Clay 2+盐 2+BlockDiggable:73；食物/树脂/生物三大主体缺物品流体独立立项；water_hot 裁不注册=IC2 别名 parity）∥ W4 NBT provider 裁定（ADR-P19 **签名波不做条件池化**：全树 21.1 NBT 面 level 依赖=0+saveToItem 0 调用；缺口=1.21 一切附魔数据驱动→B' 研究卡钉证）
- [x] B' 研究卡→小卡：registryaccess-frozen-provider 可行性（WorldLoader→AboutToStart 时序零 staleness；volatile/AtomicReference 委托+frozen builtin 兜底保 1 文件成本）→ p19-nbtaccess-dynamic-rebind（merge b2e16830）：NBT_ACCESS static final AtomicReference 委托+内嵌 ServerRegistryAccessBinder（嵌套使注解扫描不初始化 BE 类）；**终态按实测修正研卡预测**（frozen parse=静默剥附魔留裸物品非空栈——真回归是静默掉附魔）；RCON 双腿 ENCHANTMENT SURVIVED
- [x] 拆卡 ADR-P19 porting-split（5 卡 2 波）：p19-stoneblocks-registry（merge 2948b7f9，GTStoneBlocks 独立注册面 17 石×16 变体 EnumProperty+四映射表 byte[] 直译+oreDictMappings，否决 GTMaterialBlocks 扩展）∥ p19-drying-rows-backfill-2（merge a5278570，盐 2+矿物 8+Clay 循环+BlockDiggable:73；seawater/waterdirty 走 **SIMPLE_LIQUID_SPECS** 否决 AQUA_SPECS 追加；SKIPPED 8 条）∥ p19-distillery-front-canonical（merge 6094b50b，三张 front colored+overlay src-over bake 正典化，幂等脚本+sha256 全录；overlay_active 实为 16x64 四帧 strip 纠偏研究卡）+9bc08d3c runData 收口（272 石键 lang）
- [x] W2：p19-stoneblocks-render（merge 8acc97f6，272 PNG byte-identical 借入+17 blockstate×16 变体+272 cube_all 模型+stoneLoot 自掉表子 provider；三偏离钉住：loot 坍缩=每石单 item id 下 dropSelf 等价/item model 单 parent 随 1.21.4+/模型不去重保上游路径）∥ p19-chisel-recipes（merge 01031e47，CHISEL 图 RM:138+三源 36 行+ToolCompat 右键门付 10000=25 点+gt6:variant tag 双腿 port-ism+SKIPPED 12 条；GT6ChiselCommand 越卡偏离 **ACCEPT** 三要件亲核=无它 RCON 活体臂物理不可达）
- [x] p19-rcon-waitdone-8kb（merge a374033e）：wait_done 8KB 尾窗盲态→字节偏移单调增量扫描（carry 劈半标记拼接+轮转重扫）+死亡归因 error_tail（全日志末 25 条 ERROR 仅异常路径付全扫）；selftest 39→**48 检**
- [x] curator 补审：astchunk-src partial 入库（cAST 分块方法论对标，剔 12 噪声文件）/ts-official 整体 skip（tree-sitter 官网首页，目录名误导；354 实为 21 文件勘误入账）
- [x] phase-closeout：root 205+mdk **1132**=**1337 全绿**；1.21.1 compile 0 红+test **1134/0/2skip=IDENTICAL-0 保持**；runData（render 波）二跑 written:0+datagen_tree_check byte-identical；RCON 新三链（p19_drying 四臂/p19_chisel 46 步/p19_nbt_rebind 双 boot）实弹 [0,0]；ADR 两篇=p19-{w4-nbt-provider-ruling,porting-split} 成文；**运维=GPG 口令缓存过期四卡连环签收停摆**：主会话代签收（drying 3 笔+front 1 笔按 coder 备好口径重写 message）+registry 后台重试循环自动落地+waitdone 挂起提交应答自落地——签名链单点依赖口令缓存的流程教训入账

## 第 20 阶段（贴图上游填充 / 标准 i18n zh_cn+拼接化 / ModularUI fork+JarJar / 进度看板）——✅ 已完成（2026-09-07）

- [x] wave1 研究收口（5 回+1 裁定终止）：贴图 census（模型零缺失/2785 占位/40 集合勘误/jar 资产在列勘误紫黑误记）∥ 双预览考古（GTCEu 页=ModularUI 活体 3D；按键投影=纯新设计）∥ i18n 方案（GT6ZhCn provider+拼接 B 波）∥ **移植进度看板**（常驻账本落 state）∥ kTFRU 二轮（KortexWorker/异步反面教材/FX 描边；机器池底不变）∥ ModularUI 审计终止（fork 定案）
- [x] 贴图 sprint（ADR-P20 双树政策+勘误）：census 守卫 0ddd034c → W2 五卡全合（sets-a 1373+sets-b 1412=40 集合 2785 闭环/machine-fronts 21 张 distillery byte-identity/tank-barrel-pipe 借 10 declare 2/testmachine datagen 收官）→ W3 tint c 路线停手（上游未喷漆=灰图原样已保真；喷涂重拆功能池）
- [x] i18n（ADR-P20 zh_cn 管线）：A 波 provider 1999 键 → B1 线缆拼接（R1 空格+R2 tier 裸键两轮打回→三层守卫钉沉淀）→ B2 石头+rows 收官（全量对照 513 零差零漏、负断言清空、en 3578→2505/zh 2089）
- [x] ModularUI fork（ADR-P20 三裁决）：卡① vendored 657 java 双腿编译绿（48% 零改勘误+DIVERGE 401 行）→ 卡② jarJar 双腿生产分发+冒烟实测+40 项打包守卫
- [x] phase-closeout：mdk 1152/0/0；分发 jar 双腿重编+jar_content_check GREEN；ADR 三篇+勘误两则

新池移交：Jade 兼容（用户添加）；机器喷涂 Paintable 两卡（W3 census 重拆）；p20-modularui-smoke-gui+跟随上游重放（gen-forks.py）+jarJar identifier 对齐（需 maven 发布形态时再议）；materialWalkEmittedKeys Map 返回值缝归一（2-copy 残余）；多方块双预览（用户裁定低优先随集成波，池中原有仍有效）

## 遗留池

- [x] **M2 哨兵产量翻案**：上游 HandlerCrushing 槽0=0 哨兵+槽1=10000 重复=每行 2 份主产出，port chance==0-null 后 oreRaw 1 份；翻案=buildRecipe base 改双 10000 槽（493 行不变）（merge 794bb1c，fix e31f48b；plain=2/blockRaw=9/dense 组合=11 对位上游）
- [x] **rotor 族卡（语义面）——P11 已落 ✅**：ENERGY_FAKE_SOURCE 退役+拆 :815（实为 mdk :374）悬置折叠+RU/KU 真语义（542d7a5）；**真机载体池化**：EngineSteam/Axle/GearBox/大涡轮等流体罐+燃料系统解锁（engines-axle 池）
- [ ] :511 FACE_CONNECTED 侧掩码 side-gated IO + auto-IO + containsInput 插槽查验；minTankSize/三哈希索引（配方面增长后性能观测触发）
- [ ] ore 方块行激活（BlocksGT.ore/oreBroken 形状，方块宇宙已解锁）+ blockRaw 493 行——依赖本项：blockRaw 9 槽行（p9 翻案后 2 base+7 块 copies=9 份，行形状已对位上游）在其物品注册前不 poured
- [ ] 多方块件面流体代理（MultiBlockPartBlockEntity 转发）；/gt6energy 无 BE 失败行补 FAILED 字面量已由 p9-command-gui-polish 闭合 ✅
- [x] W1 两条 javadoc nit（:274-275→:267-268；输入槽 y 恒 25 vs 上游 >6?7:25 条件形）（merge 34a4f57）
- [ ] RM 壳缺口余项：containsInput 物品版消费面（随 auto-IO）；GT6RecipesCokeOvenTest AIR 隐患已由 M2 守卫+清理闭合 ✅
- [ ] 特性层：trapped/comparator/涂装/TESR/lid 动画、chest BlockItem loot table
- [ ] ADR-P3-6 延后池：slotClick 全局拦截（Slot 能力模型重构）、rebootGUIs、cover 负 GUIID
- [ ] PrefixRegistry 未 close；MT.NULL.mHandleMaterial=null（工具卡注意）；TECH tMake 冻结首代
- [ ] barrel 密封发酵+连通罐 B[0]+builder wand 物品化；**P12 更新**：Gas-proof 四防=声明偏离+P13 锅炉卡 W1 还账四件（FL 名单地基/基类 tick fizz 两查/gasProof 载体行/物品面 fill 门，ADR 2026-09-02-p12-steam-proof-deviation）；桶 GUI/tap/funnel/IFluidContainerItem 物品面/keepFilter 缺口 ✅ 全落（carrier c00d3b3+keepfilter d79440c+tap-funnel 2f3fdad）
- [ ] creosote 材质密度桥（载体值回补）；per-map/per-recipe Config duration 覆盖
- [ ] beam/竹 cokeoven 行（物品宇宙已解锁可回看；木弹丸出自 Mixer 已证伪非 Shredder 输出）
- [x] GTBarrelMetalBlockEntity javadoc P6 偏离文字过时（按 p7 熔点桥现实改写，merge 34a4f57）
- [ ] GTBarrelBlock.java :48-51 aMeltingPointK javadoc 仍留 P6 "MAX_VALUE 不熔=声明偏离" 旧文（p7 桥后过时；下卡触碰该文件顺手清）
- [ ] 旧池：PrefixRegistry 未 close 项/移植进度看板（按 GT6 子系统统计已移植/未移植）
- [x] **debug 优先**：gt6oven place 覆盖拆盖跨重启复活——P10 已关闭 ✅（merge 432cfd5，虚分派黑洞根因+持久半边修复，三连重启全绿）
- [x] 线缆三族独立卡——P10 全落 ✅：红石族（R1 dfc7c73）+裸线触电（E1 6b7226f）+Laser 占位（L1 376d6eb）；Logistics 裁池（消费方全未移植）；foam 不移植声明在档；:108/:114 满径单 pass 特例归 R1b 渲染池；Laminator 绝缘配方挂机器族池
- [x] 工具族池（P10 落部分）：crowbar 挖掘扩展 ✅（787c57b，circuits 显式集 26 块）/创意 tab ✅（de3daea）/cutter 接线 ✅（d98ce8b，九宫格 toggle）；余项：合成配方（上游需锤锉工具件，不发明 vanilla 代料）/材质梯度/运行时染色（含 cutter payPerPoint 双调缝一并修，known_bugs open 现值安全）
- [x] cover 残余（P10 落大部分）：ConductorIN/OUT ✅（ca435ce）/ControllerRedstone ✅（b86e272）/item intercept 框架 ✅（8496595）/cover 板 per-state 键 ✅（cee748a）/ghost 结构预览 POC ✅（c25ed08）；余项：SelectorRedstone（依赖 SwitchableMode；R1 已落=mMode 驱动者可解）/AutoRedstone（依赖 running state）/ControllerCovers/消费盖五件（item-intercept 框架已落可逐个移植）/cover 板与 overlay 合并 dispatch 模型
- [x] 渲染残余：**R1b 线缆亮度层 P11 已落 ✅**（62b2ee2：wirelamp getLightEmission 读 BE+红石绝缘固定色 0xFF604040 修正+裸线 mState 全亮+FIBER_WIRE 贴图）；满径 :108/:114 单 pass 特判=几何等价仅 quad 数差异，裁池归 javadoc 项；余：oven colored+mRGBa tint 全保真层增量+**cover 双层已落**（2f3fe44）+Attachment/Holder 面与 showsConnectorFront 池化（研究卡 q5 草图②）
- [x] **P11 首候选（同域小卡）——双落 ✅**：wire_laser loot 补洞（bc9317e，恰 1 块实证）+connector mask stale 位「只剪不连」修复（72636d4）
- [x] **基建池（P11 落大部分）**：/gt6machine 双注册劫持 ✅（b6d839a 改 /gt6chest）/gt6rcon allow_failed ✅（0868a4b）/E1 期 javadoc 风格扫 ✅（3846971）/energy_source harness 注记 ✅（4a377f1）/RCON 框架化 ✅（5d7c143 三层+链条入库）；余项：item-intercept ADR 字符串形状整理/**cover 域 3 文件混入**（GTCoverCommand:56/:57/:214+ICoverableTE:177+TileEntityOvenCoverTest:34 断言字符串字面量须单独裁量）/**E1 期外 14 文件 CJK 残留**
- [x] **P12 决策点（用户定序）——双落 ✅**：多方块 ghost 全量（pattern API/半透明绿红/JEI，248c66b+dbc55ef+6be286e→6be2862）+ 流体罐引擎链（容器·燃料·三源·传动十卡全谱，250422a..41721ac0）；蒸汽储罐疑问求证成立（POWER_CONDUCTING，一切桶/罐装不住蒸汽）
- [x] **P13 决策点（用户定序）——双落 ✅**：p13-boiler-steam-family 锅炉族五卡四波全合（还账四件强制前置已清+HU/蒸汽常量地基+火盒五亚型+锅炉坦克+大锅炉，1148 测零打回）；正典蒸汽链 e2e 闭环
- [x] **P14 决策点**：蒸馏水闭环已落（2026-09-03）✅；用户改向裁定：**P15 主线=跨版本兼容（Stonecutter，版本矩阵 1.20.1 Forge + 1.21.1 NeoForge 已拍板，后续再扩）**，原队列（cover 残余/机器族余项/桶密封发酵等）整体顺延 P16+
- [x] **P15 主线**：跨版本兼容 Stonecutter 双节点迁移——✅ 全落（2026-09-04，20 卡合入 a5337c38，五判据总账见第 15 阶段段；开放问题 4 全裁：gregapi 强化=ADR-P15-3 分叉纪律替代前置卡/Java21=1.21.1 节点参数化落地/密度=17.4%<25% C 未触发/钉版=0.7）；kTFRUAddon 池料研卡 done（AGPL⇒通用化 clean-room 唯一合路径线，通用化路径 7 条在 tasks.research-ktfru-multiblock）
- [x] **P16 顺延池（P15 收官移交）——主体已落 ✅**：**21.1 GTClientHandlers 抽象事件崩溃 ✅**（e396bdc9，拆 .Item/.Block 子类）+**1.21.1 runData 双节点幂等终验 ✅**（收口全序通过，见第 16 阶段段）；余项顺延 P17：keepFilter 读侧重建（0 量 parseOptional 折叠写形不可修，读侧 FluidName 重建臂，待架构师裁）/quiet_window 自适应（gt6rcon.py:52 0.5s/条×~3000 step）/vanilla rconConsoleSource per-connection 根治（upstream 线索 DedicatedServer.java:517-521）/21.1 全套 22 红（21 例 FML 灌注反转配方链+1 例 keepFilter 真差——**收口新增运行时证据族两件：料斗→机器 item 面断、跨机 auto-IO 流体面，建议合卡**）/1.21.4+（GT6ItemModels/GT6BlockStates）与 1.21.6+（BE ValueInput/Output）回池（用户 2026-09-05：等其它全部干完再考虑）/runClient 目视 backlog P15 增量（21.1 首次 runClient）
- [x] **P16 新增池（收口移交）——P17 主体已落 ✅**：**datagen 共享树跨节点裁决 ✅**（ADR-P17-1，a2d0d7fa）/**sweep SESSION_GROUPS p16 簇 ✅+framework.run() node 回写 ✅+session artifact slug ✅**（6a0958a2 五小修卡）/**21.1 全套 22 红 ✅**（IDENTICAL-22→3：19 fix 落 6e1d0d55+3dee46ff，残集=1 permanent（Barrel keepFilter KNOWN DELTA 不修）+2 defer 入池）/料斗→机器 item 面断 ✅+跨机 auto-IO 流体面 ✅（wiring 补注册 3dee46ff，守卫常量表防第三例）；余项顺延 P18：keepFilter 读侧重建（跨重启 RETAINED/LOST 裁决）/quiet_window 自适应/vanilla rconConsoleSource upstream/1.21.4+（GT6ItemModels/GT6BlockStates）与 1.21.6+（BE ValueInput/Output）回池（用户：等其它全部干完再考虑）/runClient 目视 backlog
- [x] **P17 新增池（收官移交）——P18 主体已落 ✅**：EngineFuels/OreChain 活体诊断 ✅（两案同根毒态，8a607b7d 世代化重置修复，合流后 **IDENTICAL-0**）/sweep JSON worktree 隔离 ✅（f315760b）/form_scaffold BE load ERROR 观察 ✅（**定性修正=P1 真实丢数据**，21ceab6e NBT_ACCESS 闭合，known_bugs closed）/gt6oven run 200 追查 ✅（verdict=链臂缺供能非 mdk 缺陷，fc494c6b 供能步修复）/GTMultiBlockPattern NOTHING 注释+fluid_sides 出界守卫+mExpansions 线程安全+DISPLAY_CONTAINER per-menu ✅（98757672）/keepFilter 读侧重建 ✅（62e2a7b1，permanent-delta 撤销）/quiet_window 自适应 ✅（f315760b）；余项顺延 P19 池：p16 八链 session 多链打包/物品掩码 RCON 活体臂/boiler declaration-only form 拒绝臂+creative Player fixture/真实 wand 物品化/变长机消费者/**W4 NBT provider 穿透 ✅**（P19 裁定签名波缓做条件池化+B' 小卡 b2e16830 重绑复合注册表）
- [x] **P18 新增池（收官移交）——P19 主体已落 ✅**：**W4 NBT provider 穿透 ✅**（签名波裁缓做条件池化+B' 小卡 b2e16830）/framework wait_done 8KB 窗口 ✅（a374033e，selftest 48 检）/移植批三域 ✅（凿石宇宙 registry+render/DRYING 第二批/circuit 域三裁+front 正典化）/curator 354 补审 ✅（21 文件勘误）；余项顺延 P19 池：oven 链 21.1 腿复验+--dual 全跑/GT6RecipeMaps reset 生产调用者复核/vanilla rconConsoleSource upstream/runClient 视觉整备/kTFRUAddon 池底/版本扩展 1.21.4+·1.21.6+（用户：等其它全部干完再考虑）
- [ ] **P19 新增池（收官移交）**：**凿石 loot 16-item 拆分**（每石单 item id 下忠实上游 BlockStones:731 getDrops 石变体掉圆石需 registry 形态变更，独立卡）/render item model per-state 拆分（随 1.21.4+ 池）/chisel 挖掘掉落转换（GT_Tool_Chisel:57-98；1.20.1 无 HarvestDropsEvent 需另寻缝）/远程客户端 frozen 兜底 residual（B' 研卡 residual①，无同 JVM server 场景消费点未核）/sweep size==offset 轮转边角（审查备案理论风险）/drying 食物·树脂·生物族三大主体独立立项（缺 sap/juice_reed/juice_cactus/sluice 流体+Grass_Dry/Bale_Dry 物品+染料 32 行流体+Ar 气等）/circuit crafting 25 行+cover 3 配方+25 PNG icon 梯正典化+ST.tag 消费接引（**随工具物品+crafting 桥卡**）/cover 配方与 CoverSelectorTag（cover 框架池）/ACT 铺料（ACT 池）/p16 八链 session 多链打包/物品掩码 RCON 活体臂/boiler declaration-only form 拒绝臂+creative Player fixture/真实 wand 物品化/变长机消费者/oven 21.1 腿复验+--dual 全跑/GT6RecipeMaps reset 生产调用者复核/vanilla rconConsoleSource upstream/runClient 视觉整备（21.1 首次 runClient）/kTFRUAddon 通用化（池底）/版本扩展 1.21.4+（GT6ItemModels·GT6BlockStates）与 1.21.6+（BE ValueInput·Output）（用户：等其它全部干完再考虑）
- [x] **P14 新增池——主体已落 ✅**：distillery+circuit 物品 ✅（c852fa68，单 item+Damage int 载体）/drying-rows-backfill ✅（aa242411）/machine-fluid-gui+dryer GUI ✅（7fd88971 合一卡）/machine-side-io ✅（ce09d41e，auto-IO+NBT_TANK_CAPACITY+mCanUseOutputTanks；containsInput+item face 掩码活体门余池）/chisel-decalcify ✅（21be5daf）；gt6:dryer MenuType+use() 开屏=菜单门已就位仍池
- [ ] **P13 新增池**：FluidBed 活表（待 calcite 流体+ash 材质+mFurnaceBurnTime 三原语）/BURN :45-76 油脂 creosote 段/JEI 大锅炉信息页（cokeoven 先例）/barometer 逐态视觉/明火臂 λ 波动+W4 A 臂 rng 免疫断言（链卡）/火盒·锅炉·大锅炉贴图正典化（现灰度占位）/SFX fizz 音效/LH:511 voided tooltip/64K 非 bronze 合金鼓/ic2steam 兼容名/热流体物品 fill 门（若引入热流体灌入源）/W3 dismantle RCON 臂（链未行使仅离线覆盖）
- [ ] **P12 新增池**：Trinaquadalloy 轴行（Loader:1749-1752 规格外第 12 材质）/p12-gear-items 齿轮实物安装+猴扳手反转+软锤停机/carbon_dioxide 流体注册（diesel 尾气推臂待激活）/mdk cokeoven Corrupt PNG 6 条（P8 借图内容问题）/engine-steam GT6BlockStates javadoc "26 variants" 措辞残留/Transformer waste 池（EngineRotation RU↔KU 桥/TurbineSteam/便携容器族/润滑脂）
- [ ] 交互级验证（runClient 目视留用户）：/give+tint（P2）、oven GUI+cover 板/管道箭头（P4）、扳手九宫格六条+泵盖贴图+natural_gas 外观（P5）、oven 旋转六条/新桶外观/flint/creosote 渲染（P6）、三机 GUI+12/2 槽布局（P7）、12 高档鼓外观（P7）、cokeoven GUI 对帧/方块染色 tint/创造栏 7 tab/侧面装桶顶面拒（P8）、oven overlay 六面+截面+fallback 材质层/emitter 键区贴图+档位数字+item 外观/线缆材质色差+绝缘层+连接臂几何/cokeoven FORMED 占位（P9）、tools tab 图标排序/cutter 贴图/ghost 结构残影六条/红石线材质色差+lamp 发光（R1b 后）/laser wire 外观（P10）、cover 板双层渲染+发光线世界内光晕/红石电缆 96,64,64 罩色（R1b）/Redstone Wires+Laser Wires 独立类目三分页/conveyor·robotarm 动画贴图（P11）、**ghost 绿面/红面/混合/成形 shell 零变化/alpha 观感/棱线可见性（P12）/JEI 搜 coke oven 出信息页+面板正常无 crash（P12）/crank·蒸汽机·柴油机·axle·gearbox·transformer 放置朝向与运转观感+桶 tap/funnel 灌排回路（P12）/火盒燃烧态观感+明火蔓延/锅炉气压计 visual/大锅炉成型外观+ghost 绿红/五孔出汽（P13）**
