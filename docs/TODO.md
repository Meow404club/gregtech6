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

## 下一波候选（P9，等压缩后"继续"按池开）

- [ ] 线缆全谱系：16 线 5 缆 / 真材质渲染 / 扳手交互（use=PASS）/ IC2 拉取 / bundled channel
- [ ] C 档 oven 动态渲染升级 + D 档多方块成型态渲染
- [ ] cover intercept 族 + 红石钩 + 正式 crowbar（GT6 工具系统入口）
- [ ] barrel 密封发酵 + 连通罐 B[0] + 破坏倾倒；Gas-proof 四防族；桶 GUI/tap/funnel
- [ ] 机器族 compat 全部（外域 mod 配方）
- [ ] 按需 researcher→architect 开新域（权威池=state todo.pool）

## 遗留池

- [x] **M2 哨兵产量翻案**：上游 HandlerCrushing 槽0=0 哨兵+槽1=10000 重复=每行 2 份主产出，port chance==0-null 后 oreRaw 1 份；翻案=buildRecipe base 改双 10000 槽（493 行不变）（merge 794bb1c，fix e31f48b；plain=2/blockRaw=9/dense 组合=11 对位上游）
- [ ] **rotor 族卡**：ENERGY_FAKE_SOURCE 翻默认 false + 拆 :815 悬置折叠（M1 声明移除路径）；RU/KU 网供电真语义
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
- [ ] 交互级验证（runClient 目视留用户）：/give+tint（P2）、oven GUI+cover 板/管道箭头（P4）、扳手九宫格六条+泵盖贴图+natural_gas 外观（P5）、oven 旋转六条/新桶外观/flint/creosote 渲染（P6）、三机 GUI+12/2 槽布局（P7）、12 高档鼓外观（P7）、cokeoven GUI 对帧/方块染色 tint/创造栏 7 tab/侧面装桶顶面拒（P8）
