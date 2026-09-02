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
- [ ] barrel 密封发酵 + 连通罐 B[0] + 破坏倾倒；Gas-proof 四防族；桶 GUI/tap/funnel
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
- [ ] barrel 密封发酵+连通罐 B[0]+破坏倾倒；Gas-proof 四防族；桶 GUI/tap/funnel/IFluidContainerItem 物品面；FluidTankGT keepFilter 0 量持久化缺口（归 Logistics 罐卡）；builder wand 物品化
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
- [ ] **P12 决策点（用户定序）**：多方块 ghost 全量卡（POC 已落：pattern API 上收/整面半透明/绿红匹配/JEI 联动）vs **流体罐引擎链**（桶 GUI·tap·funnel·IFluidContainerItem/FluidTankGT keepFilter→燃料→Engine/Axle/GearBox 真机——Rotor RU/KU 语义 P11 已备好就差载体）
- [ ] 交互级验证（runClient 目视留用户）：/give+tint（P2）、oven GUI+cover 板/管道箭头（P4）、扳手九宫格六条+泵盖贴图+natural_gas 外观（P5）、oven 旋转六条/新桶外观/flint/creosote 渲染（P6）、三机 GUI+12/2 槽布局（P7）、12 高档鼓外观（P7）、cokeoven GUI 对帧/方块染色 tint/创造栏 7 tab/侧面装桶顶面拒（P8）、oven overlay 六面+截面+fallback 材质层/emitter 键区贴图+档位数字+item 外观/线缆材质色差+绝缘层+连接臂几何/cokeoven FORMED 占位（P9）、tools tab 图标排序/cutter 贴图/ghost 结构残影六条/红石线材质色差+lamp 发光（R1b 后）/laser wire 外观（P10）、**cover 板双层渲染+发光线世界内光晕/红石电缆 96,64,64 罩色（R1b）/Redstone Wires+Laser Wires 独立类目三分页/conveyor·robotarm 动画贴图（P11）**
