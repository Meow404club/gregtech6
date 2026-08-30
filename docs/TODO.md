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

## 第 6 阶段——入口（等压缩后"继续"开）

- [ ] 研究卡：RM.CokeOven 多方块加工业务（配方面未考古，researcher 先行）
- [ ] 直接卡：oven 朝向旋转（GTCEu setFrontFacing 语义 + front_facing_rotation 贴图随卡再借，渲染器 instanceof 缝）∥ 渲染器次要观察清理 ∥ Metal/Plastic 桶恢复装饰盖限制 ∥ tools/rcon README 增补 NUL 事实

## 遗留池

- [ ] 特性层：trapped/comparator/涂装/TESR/lid 动画/getOpenGUIs 1200t 重同步、chest BlockItem loot table
- [ ] ADR-P3-6 延后池：slotClick 全局拦截（Slot 能力模型重构）、rebootGUIs、cover 负 GUIID
- [ ] PrefixRegistry 未 close；MT.NULL.mHandleMaterial=null（工具卡注意）；TECH tMake 冻结首代；服务端特例键回退
- [ ] 机器族 Shredder/Crusher/Lathe；D 完整能量网 ITileEntityEnergy；cover intercept 族+红石钩+正式 crowbar
- [ ] barrel 密封发酵+连通罐 B[0]+破坏倾倒；FluidTankGT keepFilter 0 量持久化缺口（归 Logistics 罐卡）；builder wand 物品化
- [ ] C 档 oven 动态渲染升级 + D 档多方块成型态渲染
- [ ] 交互级验证（runClient 目视留用户）：/give+tint（P2）、oven GUI（P4）、cover 板/管道箭头（P4）、扳手九宫格 UI 六条+泵盖 plate/pump 贴图+natural_gas 外观（P5）
- [ ] 渲染器次要观察清理（GTWrenchGridRenderer :114 死方法 + :151 注释措辞，下卡触碰顺手清）
- [ ] 建立移植进度看板（按 GT6 子系统统计已移植/未移植）
