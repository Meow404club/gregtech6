# 项目状态（镜像）

> 权威数据在 MCP `gt6-brain` 的 state 里（`state_read()`）。本文件是人可读镜像，
> 由各 Agent 在重大状态变更时同步。模板如下，随进度填充。

## 当前阶段

`第 11 阶段：wire 语义四件（loot/stale mask/亮度层/fiber 渲染）/ rotor 假电源退役 / cover 消费族五件 / RCON 框架三层化 / cover 双层渲染`（**2026-09-02 收官**：十五卡全数合入 main HEAD 2f3fe44，根 205 + mdk 705 = 910 单测全绿，一次打回（R1b worker-thread 契约）复审演化过；known_bugs 三项全 fixed 零 open；RCON 测试链框架化入库）

> **平台修正 2026-08-29**：原目标"NeoForge 1.20.1"被证伪——NeoForge 官方 maven 从未发布 20.1.x 产物（versions API `filter=20.1` 返回空，主会话独立复核），NeoForged 自家 ModDevGradle 把 1.20.1 路由给 `legacyforge` 变体，文档站最早只到 1.20.3。用户裁决：目标平台 = **MinecraftForge 1.20.1（47.4.10）**，构建插件 = MDG legacyforge 2.0.144。1.20.1 的 API 面即 `net.minecraftforge.*` + RegistryObject（DeferredHolder 是 20.2+ 才有），第 1 阶段的所有调研结论不受影响。

## 里程碑

- [x] 第 1 阶段：材料系统纯逻辑抽取（无 MC 依赖，含单元测试）——2026-08-29 收官
- [x] 第 2 阶段：Forge 1.20.1 MDK 挂载 + 注册桥 + DataGen——2026-08-29 收官（188 测全绿；2469 材料物品注册；runData 管线可复现）
- [x] 第 3 阶段：BlockEntity + AbstractContainerMenu + Screen 框架——2026-08-30 收官（221 测全绿；56253 物品 + 96 tab；BE/Menu/Screen/chest 全链）
- [x] 第 4 阶段：管线 / Cover / 多方块渲染（BakedModel）+ 首台加工机器——2026-08-30 收官（387 测全绿；Oven/管线/多方块/桶/Cover/渲染基建/流向控制八卡）
- [x] 第 5 阶段：管道语义修正 / 桶重力侧规则+泵盖 / 扳手九宫格 UI / RCON 工具链——2026-08-30 收官（417 测全绿；tools/rcon 正典客户端入库；瞬态 overlay 红线例外；桶侧规则+CoverPump+gt6:natural_gas）
- [x] 第 6 阶段（入口）：CokeOven 加工业务 / oven 朝向旋转 / Metal·Plastic 桶族——2026-08-31 收官（451 测全绿；首台真加工多方块机+`#minecraft:logs` tag 驱动原木配方+gt6:creosote；木桶装饰盖保真恢复；GTCEu setFrontFacing 经九宫格）
- [x] 第 7 阶段：cokeoven 回补 / 高档鼓+熔点桥 / 机器族 / 能量网 D1·D2——2026-08-31 收官（523 测全绿；gt6:oil+油页岩 8 行；熔点桥撤销 metal 不熔偏离+12 高档鼓梯 128K→10B；SHREDDER/CRUSHER/LATHE 三图三机（Crusher 并行 4）；ITileEntityEnergy 14 方法面+GTWireBlockEntity+三机 BE+GUI；D3∥D4 移 P8）
- [x] 第 8 阶段：能量网端到端 / PrefixBlock 方块宇宙 / 机器族收尾 / CokeOven GUI+流体罐——2026-09-01 收官（620 测全绿；Oven doInject 真实现+gen→wire→oven 端到端闭环五项验证；3773 对前缀方块+11494 datagen JSON；T2-T4 三机全梯+基类 doInject/alternating 恢复+KU 负脉冲过零；Recipe.chances+Crusher ore 链+poured 493；CokeOven Menu/Screen+流体罐 capability+推液守恒）
- [x] 第 9 阶段：线缆全谱系 / 渲染 C+D 档 / cover 红石钩+正式 crowbar / M2 配方翻案 / 命令小修——2026-09-01 收官（689 测全绿；GTWireSpecs 30 材质表直译 620 块+连接感知 BakedModel 真材质渲染（恰 7 iconset 零造图）；oven overlay 动态渲染+per-state MRL 键控机制发现；FORMED 双模型勘误钉死+超上游增强裁定；ICover 红石三钩框架（冻结面显式 ADR 解冻）+CoverRedstoneEmitter 首个真红石盖；正式 crowbar 工具系统入口（OR 门零迁移）；M2 哨兵翻案双 10000 槽构成对位）
- [x] 第 10 阶段：debug 修复 / 红石线族+触电 / cutter / cover 红石盖族 / ghost 预览 POC / vanilla 配方行 / Laser 占位——2026-09-01 收官（785 测全绿；known_bugs 首项 place 覆盖拆盖复活关闭（虚分派黑洞根因）；红石线族 push BFS 值存 BE+三桥+格程 4:1 衰减活证；触电 entityInside+2px 内缩机制+tierMax×4 真值表；cutter 九宫格 toggle 九文件零 diff；ConductorIN/OUT+ControllerRedstone+item intercept 恰八钩+cover 板 per-state 键修复（红石盖族四件套）；cokeoven 结构残影 POC；CRUSHER/SHREDDER vanilla 缺口 5 行+外域 mod 配方声明不复刻；tools 创意 tab+crowbar 挖掘面；Laser 占位壳+Logistics 裁池）
- [x] 第 11 阶段：wire 语义四件 / rotor 假电源退役 / cover 消费族五件 / RCON 框架三层化 / cover 双层渲染——2026-09-02 收官（910 测全绿；laser loot 补洞（恰 1 块实证）；cutter payPerPoint 双计修复+connector stale 位「只剪不连」修复（known_bugs 两项 closed）；FIBER 渲染对+laser 入 baked model；/gt6chest 命令劫持修复（Brigadier 静默合并实证）；Redstone Wires/Laser Wires 独立创意类目（laser 单成员独立 tab 按上游证据裁剪）；R1b 亮度层=getLightEmission 读 BE（worker-thread 契约 getExistingBlockEntity）+红石绝缘 0xFF604040 修正+红石 6 块入 baked model；rotor=ENERGY_FAKE_SOURCE port-ism 退役+:374 拆回上游 :815 逐字+KU 交替方波过零沿活证；GTItemMover=ST.move 八锚点子集；cover 消费族 Shutter/FilterItem/Conveyor/RobotArm 落地（512>>i=tick 周期裁决）；gt6rcon allow_failed；RCON 三层框架（漏站点必红结构保证+--stop/pkill 禁令模块强制化+链条入库）；cover 板双层渲染（census 反证 14 盖全双层））

## 第 7 阶段收官记录（2026-08-31，主会话 phase-closeout）

合入链：`55ecfef`(backfill)→`5ff9a5e`(barrel)→`dfb4a30`(D1)→`26a3c77`(gui)→`2fda5bc`(W1)→`f77c7a3`(D2)→`d0a1ebe`(basicmachine)，全程批量/单卡 review-merge 全 approve、GPG 全验、合并态门禁逐批复验。

- **p7-cokeoven-backfill**：gt6:oil 四 DR 组（温度/密度/tint 全带上游证据链）+ 油页岩 8 行回补（poured 24→32；blockDust 行裁池→PrefixBlock 方块宇宙池项）+ Row 泛化 (fluidId,mB) + `/gt6multiblock input` 泛化 `<prefix> <material>`；findMaterial 修复 = `mID>=0` 门（byName 会命中 mID=-1 的 auto-invalid 壳）。
- **p7-barrel-high-tier-melt-bridge**：材质熔点桥 `mMeltingPoint×1.25`（撤销 P6「metal 不熔」声明偏离，Bronze 1696K 活证熔毁）+ 12 高档鼓梯 128K→10B 全注册（Infinity 1e9K = 上游 :2170 显式 HU 裁定）+ 共享 BET 多挂 13 块。
- **p7-d1-energy-core**：根模块 `ITileEntityEnergy` 14 方法面 + Util 三件套（emit 六面循环/递减/break 逐字）+ EnergyBridge 缝 + EnergyGate + CS 三常量（`RF_PER_EU=4`；`OVERCHARGE_EXPLOSIONS=T` 为卡面裁决，上游默认 F，ADR ①(e) 依据）；根模块零 net.minecraft 红线 grep 门。
- **p7-gui-family**：三机台 PNG 借入（GT6 上游 CC0 1.0 公域，sha256 逐字节全等）+ 小写命名（1.20.1 ResourceLocation 路径硬约束）。
- **p7-recipe-maps-shcl**：SHREDDER/CRUSHER/LATHE 三图 15 参逐参 + 首批确定性静态行（Crusher 宝石链 poured 545 = 436+109，108 skip = 97 缺出-prefix 物 + 11 条件门，与上游缺出物 false-return 同语义）；AIR 幽灵配方修复（池含 Items.AIR → 空栈 → `withoutTrailingNulls` 裁空输入 → findRecipe 探测路径直通，lesson 入记忆）。
- **p7-d2-cable**：GTWireBlockEntity 直译（09Connector 只读复用零提缝 + transferElectricity 逐字 + EnergyTarget(邻BE,对侧)）+ GTWireBlock 64 变体 + 2 变体（32EU/1A·2A）+ `/gt6wire`；超压不烧 = burn 仅在有安培流动时累计（上游 :188 语义锁）。
- **p7-basicmachine-family**：TileEntityBasicMachine 共享基类（checkRecipe 并行段恢复 + 能量数学两分支 verbatim + canOutput :626-629 功率帽为活语义）+ supplyEnergy() A 档假电源缝 + doInject 空壳指路 D3 + 数据驱动槽位/Menu/Screen + `/gt6machine` 三链（Crusher 4 并行一周期活证）+ GTClientMachineListener 越界 accept（MenuScreens 唯一挂点）。

承重教训（入记忆）：①幽灵配方（测试池含 AIR → 空输入直通）②单类 JVM 探针须先 `GTMaterialItems.initMaterials()`（类装载环）③并行卡 RCON 编排 = 卡专用端口 + 按 PID 精确杀 + 禁泛模式 pgrep（三次险情）④清理用 mdk/run 路径特征。期中断三次（zcode 重启/WSL 重启/zcode bug），断点落账恢复零损失。

**P8 移交**：D3∥D4（义务清单在 `tasks.p7-d3/d4-energy-*.e2e_obligations`：doActive :815 注释勘误 + KU 脉冲决断 / wattage 正向记账 / 烧线断言 / canConnect emitting 支回补）+ todo.pool 全量候选。

## 第 8 阶段收官记录（2026-09-01，主会话 phase-closeout）

合入链：`2e87186`(D4)→`e2bce89`(D3)→`2e79c7e`(A)→`c69c09b`(M2)→`63e3f7d`(W1)→`6b258f2`(M1)→`a01fa07`(B)→`7516c3b`(W2)，起点 001e3a4，全程单卡/批量 review-merge 全 approve、GPG 全验、合并态门禁逐批复验。

- **p8-d4-energy-source**：GTEnergySource 测试发电机 + `/gt6energy` + GTWireBlockEntity.canConnect 双探针回补（EnergyCompat:102；connections 63 vs 缺回补 47 活证）；`isEnergyEmittingTo` 取 aTheoretical 无关静态探针（Root:714），卡面字面公式会击穿 gen→wire 连接（审查 ACCEPT）。
- **p8-d3-energy-consumer**：Oven doInject 真实现（:489-508 直译）+ mdk Root 默认块/overcharge/explode + `ENERGY_FAKE_SOURCE` 默认 false + UT6.tierMax；**gen→wire→oven 端到端五项闭环**：连续源完整冶炼 cobble→stone、wattageLast=30 正向记账、超压 16 strikes 烧线（炉存活）、超流 2A 烧线、rotate/facing 回归无损。
- **p8-prefixblock-registry**：GTMaterialBlocks 注册家 3773 对七前缀（census Raw618/Gem217/Dust1096/Ingot483/Plate673/PlateGem203/Solid483）+ first-wins + get 缝 + 7 创造栏 + cokeoven block 7 行回填 poured 39（:804 流体量实为 6750，卡文 3375 系错数，审查勘正）。
- **p8-recipe-chances-orechain**：Recipe.mChances（10000 基准，chance==0 裁 null = 对上游 :765-767 透传疑似 bug 的声明偏离）+ addRecipe 双空拒收守卫（幽灵配方根修）+ GT6RecipesOreChain ore 链静态化 poured 493 精确对账；根 OM 零改动（三字段基线已含且更全）。
- **p8-cokeoven-gui-menu**：GTBasicMachineMenu Host 接口化（机器域零触碰）+ 多方块基类 MenuProvider + COKE_OVEN GUI 串大写地雷修复（ResourceLocation 必炸）+ cokeoven.png CC0 借入；「12 槽」证伪为 10 交互槽；mSuccessful 单 tick 态经 fast_cmd 竞速捕获 progress=32767 活证。
- **p8-machine-tiers-doinject**：T2-T4 三机全梯 9 块（能量三值 64/128/256 · 256/512/1024 · 1024/2048/4096；Crusher PARALLEL 8/16/32）+ 基类 doInject/alternating 半恢复 + `ENERGY_FAKE_SOURCE` 默认 TRUE（:501 类型等值门使 EU 网进不了 RU/KU 机，默认 false=三族死块）；**KU 跃迁=过零检测**（EngineSteam:146 ±KU 真交流），卡面「停注入下一 tick 出」被三链实证物理不可能，负脉冲 finalSize 单命令全周期替代（审查 ACCEPT）；3 家族 BET 多挂 T1-T4（T1 零回归）。
- **p8-prefixblock-render**：模型合并 175 共享（前缀×实存集，防 per-pair 膨胀）+ 11494 datagen JSON（3773 blockstate+3773 item model+3773 loot 自掉表）+ 175 灰度 PNG sha256 全等借入 + fRGBa tint 双侧注册（Forge BlockColor 不染 BlockItem=声明偏离②实证）+ jar zip64（71232 条目>65535，声明偏离①）。
- **p8-cokeoven-fluid-capability**：MultiBlockFluidHandler drain-only wrapper（掩码 61 纯函数旋转，无裸 side==UP；null-side=上游 :144 SBIT_A 忠实镜像非偏离）+ 基类 capability 段 6 行窄改 + P6 推液守恒活证（桶 6995+8000=14995 机罐清零=同罐两消费者并存）。

承重教训（入记忆）：①手动 inject 无法完成完整冶炼（CONSTANT_ENERGY :894 清 parked progress + RCON 往返≥2tick）——完整 smelt 须连续源 e2e；②GTWireBlock 无回连钩子——能量链 gen/oven 必须先于 wire 放置；③KU 跃迁测试按负脉冲过零写，勿写「停止注入出料」；④RCON 快机（4tick 周期）三态断言结构性竞态，慢机保全断言；⑤>65535 jar 条目须 zip64。期中断：zcode bug 一次（断点落账恢复零损失）+ worktree 外部扰动一次（审查官重跑全门禁固化）。

**P9 移交**：todo.pool（M2 哨兵产量翻案 / rotor 族翻 ENERGY_FAKE_SOURCE 默认 false+拆 :815 悬置 / :511 侧掩码 side-gated IO / ore 方块行激活 / 多方块件面流体代理 / `/gt6energy` FAILED 字面量 / runClient 目视 backlog）。

## 第 9 阶段收官记录（2026-09-01，主会话 phase-closeout）

合入链：`34a4f57`(polish)→`794bb1c`(yield)→`55242e2`(render-D)→`5bca6e6`(redstone-hooks)→`e4bedea`(render-C)→`e93ea25`(crowbar)→`59390be`(W1)→`a76c9b0`(emitter)→`ea285e3`(W2)，起点 368e46e（96e7286 后 3 个 brain/docs 提交），全程单卡 review-merge、GPG 全验；render-D 一轮打回（审查证伪过度断言）→amend 重签→复审通过的演化链为纪律成例。

- **p9-command-gui-polish**：`/gt6energy` 四 op 失败行补 FAILED 字面量（对齐命令族 `<OP> FAILED:` 风格）+ GTBasicMachineMenu 两条 javadoc 事实化（:267-268 流体显示槽 / `(>6)?7:25` 条件形）+ GTBarrelMetalBlockEntity javadoc 按 P7 熔点桥现实改写（Bronze 1696K 链逐项核实）。
- **p9-recipe-yield-reform**（M2 翻案）：buildRecipe base 双 10000 槽（上游 `Recipe.java:906` chances≤0→10000 ctor 改写实存=严格等价）+ blockRaw extraCopies 8→7（2+7=9 与上游构成对位；翻案前 1+8=9 系总数巧合构成错；plain 1→2、dense-plain 3→4）+ poured 493 不变 + chance==0→null 保留；RCON 活证 out[1] 从恒空到双份主产出。
- **p9-render-c-oven-overlay**：GTOvenRenderSnapshot + OVEN_SNAPSHOT 第二 ModelProperty（cover 链共存）+ GTOvenOverlayModel cutout overlay + quirk mActive 恒优先 mRunning（:1014）+ property 唯一写源/snapshot 只读投影双通道纪律；**机制发现**=烘焙 top-level 键是 per-state MRL（vanilla ModelBakery:136），16 变体方块动态模型必须 per-state 键控（单模型映射被静默跳过）——后续变体方块动态模型承重结论；12 PNG 借入（census 24 张全实存，colored+inactive 组裁剪=声明偏离）。
- **p9-render-d-formed-look**：census 负结果=上游 multiblockmains/ 无 cokeoven 控制器贴图组（基类注册路径上游即 missing），占位保留零造图；javadoc 勘误经打回收缩为真命题（上游无消费者按 mStructureOkay 选贴图组；视觉消费仅 LargeTurbine getRenderPasses2 render-pass 数与 Crucible setBlockBounds2 两处）；FORMED 双模型=超上游增强钉死保留（否决 GTCEu IS_FORMED ModelProperty）。
- **p9-redstone-hooks**：ICover+3 红石钩（:190-192）+ AbstractCoverDefault 三默认 + ICoverableTE 两出口（上游承载=TileEntityBase04Covers:409-441，卡面 06Covers 系笔误实读纠正）+ GTOvenBlock getSignal 桥（bridgeSignal static 纯函数）；冻结面显式 ADR 解冻（恰 ICover+3/ICoverableTE+2，框架先行单次解冻多卡复用）；CoverData 零 diff；OPOS 反转地雷由六向真值表钉死；mRedstoneStopped 只锁假电源充能门不挡电网 doInject（承重事实核实）。
- **p9-tool-crowbar**：正式 crowbar（GT6ToolActions.CROWBAR 禁 HOE_DIG + useOn 直派 onCoverToolClick:246-247 OR 门零迁移 + 耐久 512 + 10000→1 耐久点映射）+ GT6Tools 注册 + /gt6tool dismantle + crowbar.png 上游逐字节借入；hoe 三谓词零触（扳手 UI 回归红线）。
- **p9-redstone-cover-emitter**：首个真红石盖（上游 136 行直译：weak=bind4(mVisuals)/strong=mValues bit0 门不并机器默认值 + 裸右键 16 键区（:72-109 含 0↔15 wrap）+ cutter TOOL_CUTTER 类内常量；入向 getRedstoneIn 零覆写=上游无此方法实证）+ /gt6cover signal 验收通道 + 17 张贴图（档位图离线合成=声明偏离）；六冻结文件零 diff。
- **p9-wire-family-w1**：GTWireSpecs 30 材质行直译 addElectricWires（Loader:1914-1950+V 表 CS:148-154）+ 620 块（28×21+2 纯线×16 census 钉数零裁行）+ use=PASS 显式语义锁 + IC2 拉取不复刻留 FE↔EU 桥缝 javadoc + bundled aChannel 占位闭环 + 每块 1 blockstate 单空 variant 键（禁 64-variant×620 爆炸）；RCON 22/22 跨档+16 strikes+超流+e2e。
- **p9-wire-family-w2**：GTWireBakedModel（CONNECTIONS mask→内芯+六向臂 10ConnectorRendered:113-140 直译，tLength=0，64 形状缓存，mask 全走 BlockState 禁 ModelData）+ tint 材质色/绝缘灰叠层 + census 恰 7 iconset 零缺口零造图 + tab 620 全员；P8 钉测窄域化 ACCEPT（过滤集与生产同源+自家 census 补位=共享目录借入成例）。

承重教训（入记忆）：①过度断言必被审查证伪（"20 消费者全逻辑门"→真命题收缩+amend 重签演化链）；②测试计数 stale 口径两起（424/457 误报）——审查官 JUnit XML 实测对账成例；③共享目录借入各带自家 census；④64EU=烤炉 overcharge 上限，更大包炸炉连坐，高压验收须炉安全包；⑤嵌入服务降级（remember 长文本 400）→锚点 state 先行+拆条落账；⑥rebase 误提交冲突标记事故（自曝+软重置+审查 grep 复验成例）。新增 known_bugs open：gt6oven place 覆盖拆盖跨重启复活（covers/oven 域 debugger 候选）。

**P10 移交**：todo.pool（debug 优先 place 覆盖拆盖复活 / 线缆三族 / 工具族池 / cover 残余 / oven tint 全保真层 / 机器 compat / RECYCLABLE / rotor / ore 方块行 / barrel 特性层 / runClient 目视 backlog）。

## 第 10 阶段收官记录（2026-09-01，主会话 phase-closeout）

合入链：`de3daea`(creative-tab)→`787c57b`(crowbar-mining)→`cee748a`(cover-plate)∥`c25ed08`(ghost-poc)→`432cfd5`(debug 修复)→`503d242`(compat)∥`ca435ce`(conductor)∥`dfc7c73`(R1 红石线)→`8496595`(item-intercept)→`b86e272`(controller)∥`d98ce8b`(cutter)→`6b7226f`(E1 触电)→`376d6eb`(L1 laser 占位)，基线 a0a950c（用户基建提交 01eab87 hooks+7f04a88 brain fix 在序中），批量/单卡 review-merge 全 approve、GPG 全验、合并态门禁逐批复验；WSL OOM 中断一次（四会话续跑+compat 漏派补派）零丢失。

- **p10-debug-oven-cover-resurrect**（known_bugs 首项关闭）：根因=`causeBlockUpdate()` 虚分派被 `TileEntityBase01Root` public final 同名（:243-245，仅置 mDoesBlockUpdate 缓冲）遮蔽 ICoverableTE 持 setChanged 的 default——装/拆盖从不 setChanged，chunk 永不脏化；place 覆盖同态炉在 vanilla `LevelChunk.setBlockState` 相等早退连 unsaved 都不置；`ChunkMap.save:786 !isUnsaved()` 跳存→盘上残留盖存活期窗口 NBT→重启复活。修复=setCoverItem 无条件 setChanged+:306 改 sendBlockUpdateFromCover 补持久半边（3 文件 +69/-3，无 onRemove 红线零触碰）；修复前三连重启确定性复活→修复后双点位全 null；回归四路径测试。承重教训：**接口 default 被宿主 final 遮蔽=虚分派黑洞，持久化副作用必须显式直发**。
- **p10-wire-redstone-family**（R1）：红石族三材质六方块（RedAlloy/Signalum/Lumium wirelamp，Loader:1893-1902 直译，损=MAX_RANGE/16|/64|/16）——push BFS verbatim（doRedstoneUpdate 双 HashSet 层序）值存 GTWireBlockEntity（mRedstone/mReceived/mMode），**BlockState 零新增 property 红线**；GTWireBlock 三桥（getSignal/getDirectSignal weak=strong 同值 bind4 邻块修正+比较器 floor）；vanilla 输入 strong-only+REDSTONE_SINKS 拒收；spec family/luminous 列+BET fallback family 分流（GTBlockEntities 零触）；RCON 衰减**笔算自证逐位吻合**（Signalum 3 跳=MAX×15−3×(MAX/64)=32111591412，RedAlloy 4:1 格程活证）；vanilla 源→GT 线→vanilla 灯 strong 桥 scoreboard。
- **p10-wire-contact-damage**（E1+四 ride-along）：entityInside 触电+**2px 碰撞内缩机制**（1.20.1 checkInsideBlocks 只在 Entity.move() 内跑，全立方碰撞箱站立方永不触发=内缩是钩子前提，vanilla 反编译三环坐实）；tierMax(wattage)×4 全梯真值表（32EU→4…8G→60）；未传电不咬（mWattageLast 门，不持久化=上游 writeToNBT2 空体语义，p7 零新增 NBT 契约测试背书）；免疫=创造（四槽 HAZMATS_LIGHTNING 装备面声明裁池）；伤害源=vanilla lightningBolt；ride-along=红石 6 块 loot+load javadoc 偏离声明（上游 getByte 截断读）+connect :130-140 视觉连接分枝+**soak 5/5 零 flap（R1 灯灭遗留闭合）**。
- **p10-cover-conductor-redstone + p10-cover-controller-redstone**（红石盖族）：ConductorIN 纯标记+ConductorOUT 穿机导线（weak=bind4(mValues)+onBlockUpdate 扫全脸 IN 取 max；DELAYED_BLOCK_UPDATES 队列不复刻=直写声明偏离）+GTOvenBlock.neighborChanged 分发缝接活（CoverData.onBlockUpdate 全仓零调用方→接活，上游母本 06Covers:382）；CoverControllerRedstone 五臂内联（放置门/移除复位/placed+load 同步/onBlockUpdate 驱动/tick 轮询）+NEW ITileEntitySwitchableOnOff+TileEntityOven 一行 implements+极性 bind1(in)!=(values&1) 手工验真（=上游 "Runs when OFF"）+screwdriver bit0 返 1000；RCON 火把→IN→OUT 灯三拍+进度冻结断言。Selector/AutoRedstone/ControllerCovers 裁池。
- **p10-cover-item-intercept**（冻结面扩面 ADR）：ICover 解冻**恰八钩**（:209-16 interceptItemInsert/Extract+override 三 claim+answering 三答）+AbstractCoverDefault 八默认+ICoverableTE 三宿主门（04Covers:343-365 分发形状）+TileEntityOven 侧感知 IItemHandler wrapper（六面惰性 LazyOptional，宿主半边由 wrapper 内层承担=语义等价，盖拆即时生效）；消费盖五件池；GUI 钩/左键死代码红线维持。
- **p10-cover-plate-perstate-fix**（P9 render-C 勘误落地）：GTCoverClientListener 3 个模型文件 id 死键→GTOvenClientListener.targetModelIds() 16 per-state MRL 只读复用；声明工作状态=plates 与 overlay 同键 last-wins 单层（合并 dispatch 留后续）。
- **p10-tool-creative-tab + p10-tool-crowbar-mining**：GT6Tools 自持 'tools' tab（表驱动，cutter 落地后两行）；crowbar 挖掘面=isMinableBlock（GT_Tool_Crowbar:108-114）1.20.1 等价 getDestroySpeed+isCorrectToolForDrops，rails=BaseRailBlock+circuits=显式红石 IO 集 26 块（GTCEu harvestTag 路线证伪裁决）+速度 6.0F 铁级声明偏离；classifies() 不动永不 HOE_DIG；crowbar 配方（需锤锉）/材质梯度/染色裁池。
- **p10-tool-cutter**：CUTTER ToolAction+CUTTER_ID 与 emitter 同串 parity 钉死；useOn 双臂（wire 九宫格 toggle 走既有 connect/disconnect API+cover relay 走 :275-276）；**九文件零 diff**（GTWireBlock/GTWireBlockEntity/Command/09Connector/ICover/ICoverableTE/CoverData/GT6Covers/Emitter）；RCON cut 三拍 63→59→63+EU 链真断供 used 1→0→1；P9 use=PASS 语义锁留位兑现；payPerPoint 双调潜伏缝入 known_bugs（现值安全，未来 ≥10000 cover 双扣→工具池卡修）。
- **p10-ghost-preview-poc**：多方块结构残影 POC——RenderHighlightEvent.Block 同帧画 27 格线框（Forge patch:121-132，cancel 只吞原版选框）；**致命陷阱**=checkStructure2 内含 removeBlock 世界写（TileEntityCokeOven:100-101）禁客户端跑求 pattern→独立纯表 26 格+FACING 纯函数旋转；P5 瞬态例外三约束（零静态/零写入/零取消）；FORMED=只画外壳 12 外沿；持续型 hologram/JEI/Sodium 裁池，全量卡边界 P11+。
- **p10-compat-vanilla-rows**（机器族 compat 定义落地）：上游 compat 面 a/b/c 分类——(b) 类 59 个 Compat_Recipes_*+Loader_Recipes_Foreign+散布 MD.* 门=**外域 mod 配方声明不复刻**（不写门代码，复活条件=宇宙引入外域 mod 时按 tag listener 重建）；vanilla 缺口实测仅 5 行落地（CRUSHER obsidian dust x8@10000+x1@2500/netherbrick chances 四档/netherrack·endstone→rockGt×4+SHREDDER bone→bonemeal；:82 输入偏离=blockSolid Obsidian 未注册实证）；chances 四档透传测试法=受控 Random 恒 9999 全灭 sub-10000 档而 10000 档走免采样整栈分支（端到端活证）；Hammer/Mortar 行归工具池。
- **p10-wire-laser-placeholder**（L1）：Family.LASER 单行直译（Loader:1814-1815，PX_P[6]/loss0/容量 Long.MAX 数据钉）+transferLaser 纯壳（返 0，上游 :66-86 语义抄录+复活条件=LU 载体卡+converters 四收发端机器池）+inert 三不（不烧/不触电/不接 EU，RCON EU NOTHING FLOWED 跨 laser 腿活证）；Logistics 裁池（纯连接标记，消费方全未移植）。

承重教训（入记忆）：①gradle daemon 互杀（--stop 全局击落他会在飞构建/服务器）→**--stop 全面禁用**（用户裁定，daemon -Xmx1g 有界常驻）+跑服原子化单 bash（起服→Done→链→杀 PID）；②新 worktree 缺 eula.txt 致 runServer 静默退场；③任务板登记与 Agent 派发必须同批核对 agentId（compat 漏派事故，工作树缺失才暴露）；④RCON 断言口径：冶炼断言=progress+stopped（done 采样受超频 pacing 不可靠）/`data get` 读不了非 BE 方块用 `execute if block`/红石灯后放不自发读信号；⑤叠基卡模式（controller/L1）+rebase 后分支头 hash 必须回报下游叠基卡；⑥测试计数以审查实测为准（circuits 25→26 块、intercept 13→11 测两起笔误勘正）；⑦WSL OOM 四会话中断→worktree 现场盘点（提交数/未提交 diff 鉴别）续跑零丢失。known_bugs 新增 open：cutter payPerPoint 双调缝（工具池卡）；connector mask stale connection 位（无邻居重扫描，wire 池评估）。

**P11 移交**：todo.pool（首候选 wire_laser loot+connector stale 同域小卡 / R1b 渲染亮度层 / flat tab 族 / payPerPoint 缝 / cover 残余 Selector·Auto·消费盖五件·合并 dispatch / ghost 全量卡 / 机器族余项 RECYCLABLE·rotor·:511·ore 方块行 / 桶 chest 特性层 / 基建池 /gt6machine 双注册劫持·gt6rcon allow_failed·ADR 整理·风格扫 / runClient 目视 backlog P10 增量）。

## 第 11 阶段收官记录（2026-09-02，主会话 phase-closeout）

合入链：`bc9317e`(loot)→`84e451d`(cutter)→`72636d4`(stale-mask)→`39340f8`(fiber)→`b6d839a`(/gt6chest)→`1ecf0ca`(flat-tab)→`62b2ee2`(R1b)→`542d7a5`(rotor)→`5308c78`(mover)→`0868a4b`(hygiene)→`73d8881`(shutter-filter)→`b69fd52`(controllers)→`b43be51`+`c394548`(conveyor+审查收口)→`5d7c143`(rcon-framework)→`2f3fe44`(render-multilayer)，基线 2c6615c。十五卡全 approve、GPG 全验、合并态门禁逐批复验；一次打回（R1b）经返工同审查官复审过；用户中途一次「干完先停」暂停点（八卡处）后复工。R1b 前置调研（render-brightness 五子命题）+cover 残余批调研+rotor 族调研三张研究卡先行备证据。

- **p11-wire-laser-loot**：laser loot 补洞——wire_laser 原本完全无 loot 表（破坏零掉落），并入共享 wire loot provider（E1 红石行同构）；**卡面「16 块」前提被证伪**=laser 恰 1 块（上游 Loader:1814-1815 单 ID 注册 24900 非区间，EXPECTED_LASER_VARIANTS=1 静态硬闸）。
- **p11-cutter-payperpoint**（known_bugs #1 closed）：上游考古=唯一计费点 Behavior_Tool.onItemUseFirst:60/:63（一次聚合+单次 doDamage），relay 链（宿主三臂+emitter）全裸返零计费→内层重载改纯 relay 删多余 payPerPoint；计数桩 sPayPerPointCalls 正反验证（塞回双计→桩红）；物理扣点归 RCON（CrowbarTest 同墙先例）。
- **p11-connector-stale-mask**（known_bugs #2 closed）：换族/换实心后旧连接位残留。根因三环=①上游同样无重扫但掩码只驱动贴图（stale 不可见），本仓 CONNECTIONS BlockState 承载渲染+红石门控后成契约违背 ②/setblock 仅 flags=2 不触发 neighborChanged（唯一全覆盖缝=updateShape，vanilla RedStoneWireBlock 先例）③headless 无 entity-ticking chunk 剪枝必须同步。修法=「只剪不连」（validateConnections/canStayConnected 逐连接侧重推；红石行 keep=canConnect 恒真无漏剪；未加载格 keep 不触发同步 chunk load；不自动重连）+updateShape/neighborChanged/onTick 三缝。RCON 换族 63→31 同步、air/红石 63 保留。
- **p11-wire-fiber-texture**：FIBER_WIRE/FIBER_WIRE_OVERLAY 两 PNG 逐字节借入+GTWireBakedModel laser 分支（base 染 mRGBa+overlay 双生子 quad，位 7 缓存键）+laser 64 per-state MRL 键；fallback JSON 行保留为载体（per-state 键铸造源+item parent，电家族同构）。GTWireTint 零 diff（绝缘色归 R1b 卡）。
- **p11-gt6machine-literal-fix**：/gt6machine 双注册劫持——GTExampleChestCommand:63 与 GTMachineCommand:86 同 literal，Brigadier CommandNode.addChild 同名**静默合并**（existing.command 覆盖+子树递归并，无异常）→裸 check 被 chest 遮蔽；chest 证明命令改 **/gt6chest**（一特性一根惯例，上游无对应命令）；GTCommandTreeLiteralTest 钉双根不相交（pre-fix 红忠实复现）。
- **p11-flat-redstone-tab**：红石 6 行迁上游 'Redstone Wires' 类目（tabID 27050，icon=meta 27050 Signalum 裸线）+**laser 独立 'Laser Wires' 类目**（Loader:1815 单成员 24900——卡面「与红石同 tab」被证据裁剪）；tab 懒创建机制（MultiTileEntityRegistry:191，icon=MTE block item meta，零 PNG）；三表驱动（电 622=2 legacy+620/红石 6/laser 1）。
- **p11-wire-brightness**（R1b 主卡，唯一打回）：getLightEmission(state,level,pos) 读 BE mRedstone→bind4——**打回根因=IForgeBlock.java:106-110 worker-thread 契约须 getExistingBlockEntity**（light 任务在 ChunkMap worker 线程，ImposterProtoChunk 过渡态竞争）；onTickCheck verbatim（WireRedstone:51-58）+refreshGlowLight=checkBlock（Root:549-554 对应）+load() 客户端落点；同步链核验=getUpdateTag 本就携 gt.mredstone，缺口只在触发端；红石 6 块入 baked model+GTWireTint per-family（红石 0xFF604040 修 P10 全族 64,64,64 错色）；mState 不持久化（mRedstone 全量往返；上游 NBT_STATE 因 getByte 截断 bug 而存）；发光只在裸线（extends 链实证 Cable 不发光）。
- **p11-rotor-source-flip**：ENERGY_FAKE_SOURCE 上游零命中=纯 M1 port-ism 退役（:153→false）+:374 拆回上游 MultiTileEntityBasicMachine:815 逐字（卡序源先行→翻转→拆折叠零死块窗口）；GTEnergySource 参数化 EnergyType+mAlternating 方波（EngineSteam:113-114/:146）；KU 过零沿出料 RCON 活证（tier0 512t 静默 vs alt on 出料）；RU/KU=EU 单位引用等值门隔离，不需新 EnergyType；OfflineTestBase seam 保持 true（3 条自馈语义测试依赖，oven 同形）。
- **p11-item-mover-helper**：GTItemMover=上游 ST.move 八锚点子集（:455/:511/:513/:535/:537/:667-678/:688-699/:615-629）——canTake→extractItem simulate 探针（hook 零落账钉死）/canPut→getSlotLimit+canItemStacksStack+simulate insert 余量门/同槽 abort verbatim；裁剪 7 项逐条 javadoc 锚行号（对盖调用点不可达）；21 真值表例。
- **p11-infra-hygiene-bundle**：gt6rcon allow_failed（1 基序号标记步失败不中断，默认路径逐字节同构 smoke 18/18）；javadoc 中英混入 13 处/10 文件修正（census 定主流=纯英文）+mEmitting harness 注记+顺手勘正旧错误注释（updateEntity client-side 与 Root.isServerSide 相悖）。
- **p11-cover-shutter-filter**：CoverShutter 四拦截 (visual==0)==mStopped 逐字+螺丝刀翻转；CoverFilterItem（census 勘正类名）白/黑名单逐字+滤物存 CoverData.mNBTs 键 gt.filter.item+filterTagFor 纯函数；4 管线臂声明裁除；4 PNG 借入；RCON 27/27（fill 清场区教训来源：漏 D 站点第二轮假红）。
- **p11-cover-controllers**：CoverControllerAutoRedstone 不等式 :70-72 verbatim（(mActive&&!mSuccessful)||极性，双形 instanceof 读机器 public 字段——机器类禁触下最短路）+CoverControllerCovers 相等式 :106-108+三臂驱动 CoverData.setStopped+getSideWrenching 跨面中继；互反对拍测试防串（assertEquals(!tEq, tMachineSwitch…)）；RCON in-flight hold 不可观察裁决（doInject mStopped 拒能死锁，单测全表+两端断言承接）。
- **p11-cover-conveyor-robotarm**：**512>>i=tick 周期裁决**（mTiming=Math.max(1,t)，仅喂 SERVER_TIME%mTiming==0；tooltip "every N Ticks" 同口径）非单次帽；arm 四象限 ignoreSide 逐位对位；GTItemMover 三入口消费（固定槽端 SIDE_ANY/扫描端 delegator-side/邻居面 OPOS）；4 PNG 16x64 四帧动画+mcmeta frametime 2；10+10 注册。审查 follow-up c394548 补收 controllers 漏跑的 runData（生成树缺 2 模型+2 lang）→**provider 触碰必跑 runData 二跑**纪律落地。
- **p11-rcon-framework**（用户指示模块化）：三层=gt6server（生命周期：eula 兜死/ss 预检顺延/nohup+wait_done/RCON stop→PID 精确杀——--stop/pkill 禁令**模块强制化**）+gt6world（站点注册表 declare_sites+cleanup=站点 bbox 自动外扩——**漏站点必红**结构保证+分片 fill）+chains/声明式链**入库**（新卡链一律走框架，旧 tmp 链为历史工件）；试点=shutter 链迁移真跑两遍 66/0 幂等。
- **p11-render-cover-multilayer**：census **反证卡面**——14 注册盖全部 base+fg 双层（上游 06Covers:449-463 每盖两遍：偶 pass=holder base/奇 pass=attachment fg 画 2px BOXES_COVERS 板盒；emitter「单图」实为 base+已合成 tier）；GTCoverRenderSnapshot 双分量 record+census 表派生层表（cover 类零触）+CoverPlateModel layer>0 仅 covered-face 一枚 front quad 法向外移 PLATE_EPSILON*layer（不进 UV=层间像素对齐）；layer0 逐字节等价承重墙（Arrays.equals 精确数组断言）；2 PNG 补借+README SUPERSEDE 声明。

承重教训（入记忆）：①light 引擎等 worker-thread 采样须 getExistingBlockEntity（IForgeBlock:106-110），getBlockEntity 过渡态竞争——打回根因；②provider 触碰必跑 runData 二跑（providers 即生成树契约）；③RCON fill 清场区=站点 bbox 派生（手写必漏，框架结构解决）+drops 断言锚 containment 非列表头（掉落物抛掷随机）；④census 先行两次救卡（CoverFilterItem 类名勘正/14 盖全双层反证预设）；⑤Brigadier 同名 literal 静默合并——命令注册须唯一性测试；⑥staged flip 中间提交自带红须提交消息明示（审查备案不拦）。known_bugs 状态：三项全 fixed 零 open。

**P12 移交**：决策点=多方块 ghost 全量 vs 流体罐引擎（含桶 GUI/tap/funnel→燃料→Engine/Axle/GearBox 真机链），用户定序；基建池（cover 域 3 文件混入含 TileEntityOvenCoverTest:34 字符串裁量+E1 期外 14 文件 CJK+decisions ADR 字符串整理）；池化等前置（SelectorRedstone 等首个 SwitchableMode 宿主/RetrieverItem 等 item pipe/engines-axle 等流体罐燃料）；runClient 目视 backlog P11 增量（cover 板双层/线缆发光/三 tab 分页/conveyor 动画/monkeywrench 臂）。

## 关键决策

（由 /architect 与 /researcher 持续写入，同步至此）

| 日期 | 主题 | 决策 | 证据 |
|---|---|---|---|
| 2026-08-28 | 基础设施 | 检索采用混合语义+BM25+RRF；原版 1.20.1 反编译源码入索引 | tools/README.md |
| 2026-08-28 | 材料系统边界 | GT6 材料系统核心在 `gregapi/oredict/`（模型）+ `gregapi/data/`（MT/AM/ANY/OP/TD 数据表），非旧资料所称 `gregtech/api/enums/` | gregapi/oredict/OreDictMaterial.java:48-52；gregapi/data/MT.java:43-47 |
| 2026-08-28 | 第1阶段拆解 | 材料 5 卡：foundation → model → (dataset ∥ ore-prefix) → graph | OreDictMaterial.java:209-330；OreDictPrefix.java:54-133；MT.java 4118 行 |
| 2026-08-28 | Gradle 骨架 | 单模块纯 java-library（Java17+JUnit5）先行，保留 gregapi 包名；第 2 阶段多项目挂载 MDK | 第1阶段验收=无 net.minecraft import 的 gradle check |
| 2026-08-29 | 目标平台 | "NeoForge 1.20.1"证伪（官方 maven 无 20.1.x），用户裁决改用 **MinecraftForge 1.20.1（47.4.10）**，构建插件 MDG legacyforge 2.0.144 + Gradle 8.14 + Java 17 | maven versions API filter=20.1 空（双独立查询 2026-08-29）；MDG 插件注册表 "legacyforge: Forge platform, up to 1.20.1"；GTCEu Modern 1.20.1 用 Forge 47.4.0（libs.versions.toml:2-4） |
| 2026-08-29 | 模块挂载 | 多项目 include（根目录保留 gregapi java-library，`settings.gradle include 'mdk'`，mdk 依赖 `implementation project(':')`）；composite 后备，mavenLocal 否决 | IDE 单工程同步/单任务图增量/`:check` 独立跑根模块测 |
| 2026-08-29 | 注册桥 | 方案 A 两段桥：FMLConstructModEvent.enqueueWork 跑 MT.init()（open→closed）→ 直接监听 RegisterEvent 遍历 MaterialRegistry×OP 动态灌入 → FMLCommonSetupEvent.enqueueWork 调 applyCrucibleAlloyReferences（postInit 等价物）；DeferredRegister 逐条句柄只适合少量手写物品；Registrate 留第 3 阶段 | GTRegistrate.java:148-151（LOW 优先级监听 RegisterEvent）；DeferredRegister.java:177-178（窗外抛 ISE）；FMLCommonSetupEvent.java:24-28 |
| 2026-08-28 | MC 耦合剥离 | NBT→Serializer 接口；FluidStack/Enchantment/Achievement→字符串/枚举 ID；TextureSet/IIconContainer→名称引用；ItemStack 字段删除 | OreDictMaterialStack.java:31；OreDictMaterial.java:252-254,315,319 |
| 2026-08-28 | 红线-静态初始化 | 禁复刻 MT.java 巨型静态块（65536 上限）；数据分批注册；注册表可重置；GAPI.mStartedInit 改注册表状态机 | MT.java:45 作者自注；OreDictMaterial.java:153-155 |
| 2026-08-30 | BE 形态 | 共享 BlockEntityType + validBlocks 多挂为默认（独立 BET 仅单 Block 最小面例外）；Capability（ForgeCapabilities.ITEM_HANDLER）缝合进 BE 框架卡，Forge patch 在 setRemoved/onChunkUnloaded 自动插 invalidateCaps，只需覆写 invalidateCaps | ADR-P3-1/P3-2；RegisterCapabilitiesEvent.java:14-16；IItemHandler.java:15（@AutoRegisterCapability）；BlockEntity.java.patch:45/:51 |
| 2026-08-30 | 注册接线 | 新注册一律卡内自持监听，GT6Mod/GTModBusListener 全阶段冻结（并行前提）；自持取 mod bus=Bus.MOD.bus().get()（Mod.java:81）；1.20.1 无 RegisterMenuScreensEvent，MenuScreens.register（Forge AT 提权 public）唯一挂法 FMLClientSetupEvent.enqueueWork | ADR-P3-3/P3-4；accesstransformer.cfg:73；forge-docs gui/screens.md:314 |
| 2026-08-30 | 注册宇宙 | 全量前缀注册宇宙=上游物品路径 105 前缀（Loader_Items.java:57-171 逐行核验恰 105），468×isGeneratingItem naive 展开 476,183 对否决；ore/block/pipe/wire 走 PrefixBlock/MTE 非物品路径；id 撞车 first-wins | ADR 2026-08-30-p3-fullprefix-registration-universe；runServer 日志 56253/0 撞键/945 归并/363 跳过 |

## 第 1 阶段模块卡（2026-08-28）

> 材料系统实际位置：`gregapi/oredict/` + `gregapi/data/`。路径均已源码验证。依赖序：1 → 2 → (3 ∥ 4) → 5。

| # | slug | scope（做什么 / 不做什么） | GT6 源文件 | 依赖 | 风险 |
|---|---|---|---|---|---|
| 1 | gt-material-foundation | TagData/条件系统、单位常量、OreDictMaterialStack 算术；不做任何注册表 | gregapi/code/TagData.java:37-78；gregapi/oredict/OreDictMaterialStack.java:20-40；gregapi/data/CS.java（U/UD） | 无 | Stack 耦合 NBT（:31）→ 剥为 Serializer 接口 |
| 2 | gt-material-model | OreDictMaterial 模型 + createMaterial 注册（名称校验/ID/覆盖链）+ 组分加权化学计算；不做 MC 侧字段 | gregapi/oredict/OreDictMaterial.java:142-330（注册/字段）、:469-499（组分计算/化学式） | 卡1 | 耦合 GAPI.mStartedInit（:154）→ 注册表 open/closed 状态机 |
| 3 | gt-material-dataset | 元素周期表 + 全材料 + 反物质 + 别名组分批移植与保真校验；不做流体/物品生成 | gregapi/data/MT.java（4118 行）、AM.java（485 行）、ANY.java（158 行） | 卡2 | 65536 静态块上限（MT.java:45 自注）；TextureSet/TC/Enchantment 引用改字符串 ID |
| 4 | gt-ore-prefix | OreDictPrefix 模型：长前缀优先解析、材料量（U 单位）、命名模板、副产物；不做物品/配方注册 | gregapi/oredict/OreDictPrefix.java:54-150；gregapi/data/OP.java | 卡2 | 耦合 ItemStack/CreativeTabs/recipes/thaumcraft（:37-42）→ 全裁剪 |
| 5 | gt-material-graph | 12 类转换链（mTarget*/mTargeted*）查询 + 合金组分引用图；不改卡 2 文件本体 | OreDictMaterial.java:283-310、:260-268 | 卡3、卡4 | 图遍历需全量数据就绪；保真单测排最后 |

验收基线（每卡）：`gradle check` 全绿 + 卡内单测 + 源码 grep 无 `net.minecraft` import。
首个 coder 任务：**gt-material-foundation**（建 Gradle 骨架 + TagData/OreDictMaterialStack + 单测）。

> **合入记录 2026-08-28**：卡1 gt-material-foundation 审查通过合入 main（22e7532 e685d28 e42f069 → merge 2f3be03）。34 单测全绿、零 `net.minecraft` 命中；语义抽查与 1.7.10 原文逐字一致（含 equals 怪癖 :82/:84、U 家族截断除法）。NBT 剥离为 MaterialStackSerializer 缝隙，`a`/`i`/`m` 键语义保留，Phase-2 NBT 绑定须用 short 写 `i`（GT6 存档兼容）。OreDictMaterial/MT 为标注 placeholder 最小支柱，归卡2/卡3重写。

> **合入记录 2026-08-29**：卡2 gt-material-model 审查通过合入 main（5599317 5860336 46fbcb4 52ade84 + 审查修复 0ab2bf1 → merge 1b7251c）。73 单测全绿、主仓库门禁复跑通过；createMaterial 与上游 :142-176 逐字（W=32767/黑名单/覆盖链，isOpen 仅约束 aID>=0）；TDG 14 键名对齐 TD.java:305-571。审查层两处修复：NUM_SUB 按上游 CS.java:169-201 修正为 301 项（index 300 = "₃₀₀₊" 截断）；setMcfg/12 set* 改走上游 OM.java:485 null→null Stack 语义。插曲：首任 reviewer 被 GPG 口令阻塞失联，暂存修复经二任 reviewer 独立核验后由主会话代签提交。

> **合入记录 2026-08-29**：卡3 gt-material-dataset 审查通过合入 main（12d8eda a693645 590f68d → merge e408927）。97 单测全绿（新增 MTTableFidelityTest 19 项）、零 `net.minecraft` 命中、主仓库门禁复跑通过。MT/AM/ANY 全量材料表移植：1273 材料按上游声明顺序拆 reg0000..reg0038 批次（尊重上游 MT.java:45 65536 静态初始化红线），MT.NULL 经 createMaterial(-1,"NULL") 归位、卡1 桩退役、OreDictMaterial 无注册构造器私有化，addAlloyingRecipe :455-466 逐字。MC 剥离取舍 7 项全部核验成立：TextureSet→同名两变体名引用、ModData→MDRef 经 putWithMods 递归漏斗（Iterable 死角不可达）、field_151370_z→"luckOfTheSea"、9 个 setter helper 逐字复刻（setPriorityPrefix 仅赋索引，OreDictManager 副作用 :1338-1341 归 prefix/manager 卡）、setTextures 上游 :1025 无限递归 bug 确认不可达后改空默认基例、aspects/lens(44)/visDefault(192)/MT.DATA 缓办注明闭环、TDG 同一性活表证明。清点复算：ores 308、steal 45、setDensity 42、setOreMultiplier 48、setPriorityPrefix 72、Enchantment 507、TD createTagData 键名多重集合与上游一致。已知瑕疵：测试注释/提交信息中 "Diamond upstream :2183" 行号引用有误（实为 :1358），断言语义不受影响。

> **合入记录 2026-08-29**：卡4 gt-ore-prefix 审查通过（内容预审 pass）合入 main（30a499e d5fe13d d611eef 8a47124 → merge 3d0aac3）。rebase 至 ae78dc7 零冲突（mPriorityPrefix 字段区与卡3 构造器私有化不相邻，两处改动共存核实于 OreDictMaterial.java:134-136/:185,194）；重写后 4 提交 `git verify-commit` 全验。133 单测全绿（97 既有 + 36 卡4）、零 `net.minecraft` 命中、worktree 与主仓库门禁均复跑通过；OP 依赖的 TD 键由卡3 真实 TD 实例承接，TagData 幂等自动统一，MT/AM/ANY 与 OP 共存无回归。

> **合入记录 2026-08-29（第 1 阶段收官）**：卡5 gt-material-graph 审查通过合入 main（1bffca5 aa8a130 → merge 972dbad）。rebase 至 8cf8ba1 零冲突（仅新增 2 文件，重写后提交 `git verify-commit` 全验）。161 单测全绿（133 既有 + 28 卡5）、零 `net.minecraft` 命中、worktree 与主仓库门禁均复跑通过。MaterialGraph 为移植侧新增查询层（上游无独立链查询助手）：targeting 三重谓词 UT.java:1047 逐字、applyCrucibleAlloyReferences 逐字 GT_API_Post.java:820（保留无去重怪癖并以测试固化，合金反向引用两层结构差分验证）、expandChain visited+深度上限终止性全表扫描证明。卡3 遗留缺口（reset 后 AM/ANY 不重注册、TECH 别名静态字段）不阻塞本卡，由主会话 halt 阶段落账。**至此第 1 阶段 5 卡全部合入，材料系统纯逻辑层收官。**

## 第 2 阶段并行批 1（2026-08-29，批量合并会话）

> **合入记录 2026-08-29**：卡① p2-mdk-skeleton 审查通过合入 main（fe82a6e 74493d1 → rebase 9f162e4 5833a8a → merge 69b9d39）。GPG 全验（含 rebase 重写后复验）；FILES_SCOPE 11 文件零越界（settings.gradle/gradle wrapper 8.14/mdk/**/.gitignore），根 build.gradle 与 src/ 零触碰。mdk/ 子项目：legacyforge 2.0.144 + Forge 1.20.1-47.4.10 + Java 17，`implementation project(':')` 消费根 gregapi 不 shadow；generateModMetadata 展开实证 jar 内 `loaderVersion="[47,)"`、`modId=gt6`、LGPL-3.0-or-later；mdk-0.1.0.jar 仅含 mods.toml/pack.mcmeta/GT6Mod.class 零 gregapi 类。门禁：worktree 与主仓库 `clean check --no-build-cache` 161 测 0 FROM-CACHE + `:mdk:build` 全绿。**注意：门禁 runner 自此必须用 `./gradlew`（8.14），系统 gradle 8.7 过不了 MDG legacyforge 的 ≥8.8 检查。**

> **合入记录 2026-08-29**：卡② p2-registry-reset-idempotency 审查通过合入 main（d360507 3e37ba0 → rebase 15e0499 bd0f461 → merge 5564226）。三缺口（卡3 缺口①②③）同根因修复核验：批次搬运全表归一化比对（非抽查）OREMATS 87/87、STONES 68/68、WOODS 110/110、UNUSED 123/123 与 main 基线逐元素一致（卡注 378 为记账笔误，实 388 条）；ANY 两相 create(54)/init(59) 与 main 逐字一致，身份守卫与 MT.init 身份检查家族同型，两相必要性成立（MT.create:315 等 20+ 助手洪泛期引用 ANY 字段）；STONES.init() 前置于 reg0037 为 reg0037 内 `SpaceRock = STONES.SpaceRock` 别名语句的必要前提，时点对齐上游 :1919-1945 别名块触发序（上游 init() 内 STONES.Basalt.getClass() 此时已是 no-op）；OREMATS/WOODS 对齐 :1890-1900 强加载点、fixups 对齐 :1879-1883、UNUSED 原位；MT.NULL static 块回退 + 每代重注册（同名 -1 复用防首代重复注册）；ALL_MATERIALS_REGISTERED_HERE 每代清零；addAlloyingRecipe 区零触碰（无去重保真）。MTInitResetIdempotencyTest 7 项闭环缺口数字：2200 全量回灌、Any 组、Anti 组、TECH/OREMATS 别名绑定当前代、Invar 单配方两次洪泛、postInit 对称。168 测全绿 0 FROM-CACHE。审查 minor：MT.NULL.mHandleMaterial 上游 AnyWoodPlastic → 移植 null（两相后 static 块期 ANY 字段未建，现无读取方；工具卡落地时注意）；TECH tMake* 串冻结首代（内容代间不变，惰性）。

> **合入记录 2026-08-29**：卡③ p2-material-condition-system 审查通过合入 main（3846fc2 086f321 c50223b → rebase 59111e5 bad07ff 783387c → merge 21ee363）。OreDictMaterialCondition.java 127 行 vs 上游 :1-126 全 diff 仅头注来源行与空行空白归一，14 谓词零语义偏差（零补齐项证实）；47 条 create 归一化比对独立复跑 47/47 零偏差（空白/`aspects()` 剥离/TD.Creative.HIDDEN→HIDDEN/行尾终止符归一），ingotHot 插位 = 上游声明序（ingotDouble/ingot 之间），toolHead 族 43 条入 regArmor rotor 后、arrow 3 条入 regPipes bulletGtLarge 后（上游 :281-283 紧邻序保持）；TD seam 6 常量键名+显示名实测与上游一致（INGOTS_HOT/WOOD/TOOL_HEAD/NEEDS_HANDLE/NEEDS_SHARPENING/WEAPON_ALIKE；port 注释所引 TD 行号偏移约 3 行，仅注释错）；`ingotHot.mHeatDamage = 3.0F` 对齐上游 :578（familiar 段前，前置 MC 耦合行保持缓办）。OPTest 406→453、421→468 闭环，OP 前缀 421→468 全量就位（ADR-P2-6 验收线⑤）。与卡②组合核查：条件卡全部文件零 final 性依赖（仅 1 处 javadoc 提及 ANY/TECH），前缀注册序的 chemtube 位移为卡4 批次架构既有形态非本卡回归（VALUES_SORTED_INTERNAL 为长度序，OP.get 歧义匹配不受影响）。20 项新测试行为级断言。**188 测（161+7+20）全绿 0 FROM-CACHE，worktree 与主仓库门禁均复跑通过，主仓库 grep net.minecraft 零命中。**

## 第 2 阶段串行批 2（2026-08-29，单卡审查合并）

> **合入记录 2026-08-29**：卡 p2-registration-bridge 审查通过合入 main（c3500b5 5776f30 ecaa096 2122ce8 零 rebase → merge 308f85e）。GPG 全验；isGeneratingItem 判据与上游 PrefixItem.run():104 一致（OreDictPrefix:364-366 forced‖!blacklist∧mCondition，vs canGenerateItem 选型正确），四前缀条件逐字（dust=Or(DUSTS,DIRTY_DUSTS)/ingot=INGOTS/gem=GEMS/plate=And(Or(ingot,gem.NOT),PLATES)）；%s 填参=Component.translatable(模板键, mNameLocal)（GTCEu TagPrefix.getLocalizedName 同构）；特例键 Language.has 隔离进 @OnlyIn ClientSeam+dist 守卫（GTCEu :1321 为无守卫调用，移植更保守；服务端仅服务端侧取名回退模板名，客户端显示不受影响）；eventbus-6.0.5 unregister(Object) 实例键移除经 javap 实证；同名材料经 MaterialRegistry.get() mTargetRegistration 链归并。jshell 独立复算：2200 材料/468 前缀/ingot483+dust1096+gem217+plate673=2469（19 对同名双槽被归并、id 零碰撞）/合金反链 91 全一致。**ecaa096 FILES_SCOPE 越界裁决接受**：mdk/build.gradle `additionalRuntimeClasspath project(':runtimeElements')` 经 MDG 2.0.144 字节码实证——per-run legacyClasspath 配置（ModDevRunWorkflow lambda$setupRunInGradle$19）唯一 extendsFrom 即 RunModel.getAdditionalRuntimeClasspathConfiguration()，`implementation project(':')` 不进 BootstrapLauncher `-DlegacyClassPath.file` 且 gregapi 非 mdk mods 块 sourceSet（mod 自身类走 fml.modFolders）；该配置为 MDG 官方扩展点，仅影响 runs 不影响 jar，ADR-P2-1 不 shadow 不变。门禁：worktree+主仓库 188 测 0 失败 0 FROM-CACHE + `:mdk:build` 绿；合并态 `:mdk:runServer` headless 到 **Done (12.384s)** 零 GT6 告警（2200 材料/468 前缀/2469 物品/91 反链与计数全对账）。datagen 接口：lang 键 `gt6.tagprefix.<prefix_snake>`='%s 名' + `gt6.material.<material_snake>`（MaterialPrefixItem.snakeCase），iconset 取 mTextureSetsItems，tint=RegisterColorHandlersEvent.Item+MaterialPrefixItem.tintColor()，句柄查询=GTMaterialItems.get(prefix,material)。

> **合入记录 2026-08-29**：卡 p2-datagen-pipeline 审查通过合入 main（6a20688 55b1019 7c98d2a 78e37a0 零 rebase → merge 139c8c4；.cache 出库裁决另落 1676a38）。GPG 4/4。**iconset 考古链独立复核全证实**：上游 PrefixItem.java:136-138 取 `material.mTextureSetsItems.get(prefix.mIconIndexItem)`——贴图集是材料属性（.setTextures 赋予），mIconIndexItem 是 TextureSet.addToAll(:73-80) 维护的全局同名索引（对所有 set 同槽位有效）；OP.mNameTextureSet 全树仅 声明:84/默认:101/setter:322/addToAll 入参:399,408，确非图标源（注册桥 handoff 提示有误，coder 裁决正确）；上游 OreDictMaterial.java:252 默认 `mTextureSetsItems = SET_NONE[1].mList`，移植侧空表回退 "none" 保真（TextureSet.java:188）。抽查 12 模型与上游 SET_* 逐一对上（Fe=METALLIC/Au=SHINY/Coal=LIGNITE/Sand=SAND/Diamond/Emerald/Lapis/Glass/NetherStar/Rubber/Wood）。产物双向审计：2469 模型=注册集合（items() 主源 + RegistryObject.getId() 零漂移）→103 (iconset,prefix) 组合/37 集合/none 零命中/PNG 缺失 0；103 PNG→零冗余；lang 2241=467 tagprefix+1773 material+1 itemGroup，模板=mMaterialPre+%s+mMaterialPost（gem 上游 OP.java:173 pre/post 全空，卡注 :1218 系笔误）；compressed/Compressed（OP.java:201 addIdenticalNames 别名进 VALUES，host 先注册）唯一 snake 撞键，first-wins 保 host 模板语义正确，别名键（pulp/item_dust/ore_gem 等）空模板 "%s" 与上游 null pre/post 行为一致；运行时 MaterialPrefixItem 模板键同 snakeCase 同源，四前缀零影响。**.cache 裁决**：HashCache 本地账本内嵌运行时间戳（跨机器伪 diff），移出版本库+gitignore（`mdk/src/generated/resources/.cache/`），产物 JSON 保留入库；净克隆模拟（删 .cache 跑 runData）written:2470 且 git status 干净=入库产物逐字节可复现，二跑 written:0 幂等。门禁：worktree `:mdk:build` 绿；主仓库 `:clean :check --no-build-cache` 188 测 0 失败（test-results XML 复核）+ `:mdk:build` + runData 双跑幂等 + `:mdk:runServer` **Done (3.407s)** 零 GT6 ERROR（8 WARN 全环境固有：FML language jar/终端/LanServerPinger/union schema）。遗留：游戏内模型+tint 可见性需 runClient（归 p2-phase-closeout）；新 iconset 出现时 `gen_textures.py --scan` 补 PNG 再重跑 runData。

## 已知 Bug

（见 `state_read("known_bugs")`）
- 2026-08-28（已修复）：gt6-brain `state_update` 故障（`name 'json' is not defined`，根因 `tools/gt6_rag/search.py` 缺 `import json`；`state_read` 空表时侥幸不炸）。已修复并回填 state：decisions(5 条)/progress/tasks(5 卡)/known_bugs 四键读写验证通过。
- 2026-08-29（已修复）：`remember`/`recall` 间歇性超 60s MCP 预算。根因：共享 GPU 嵌入服务（embed 8937 恒定 ~232% CPU，另有 meow-translator 负载）饱和时，`embed.py` 交互式小请求仍走 240s 超时×7 重试（最长 ~30 分钟隐形挂死）。修复：`_post_batch` 按请求规模分流预算——小请求 15s×1 重试（总预算 <45s，快速报错），大批量（索引）保留 240s×7。负载下实测小请求 0.26s 通过。

## 已知陷阱（KG 摘录）

（见 `kg_query()`）

## 基础设施备忘

- 反编译脚本：`tmp/get_vanilla.sh`（1.20.1 客户端 + SpecialSource 重映射 + Vineflower）
- 全量重建索引：`cd tools && .venv/bin/python -m gt6_rag.index all`
- 新资料源登记：`tools/sources.json` → `refresh_index`
- **服务总线 `tools/gt6_services.sh {start|stop|restart|status} [brain|embed|rerank|all]`**：统一管理三个常驻服务，按端口精准启停（llama 加载期 /health 503，探活只认 200）。
- **GT6 Brain MCP 已换 Streamable HTTP 常驻守护**（弃用 FastMCP/stdio——anyio 线程层在长驻进程中出现过调用卡死）：端点 `http://127.0.0.1:8939/mcp`（`/health` 探活），日志 `tmp/index/brain-server.log`；协议壳手写 JSON-RPC（`tools/gt6_rag/server.py`），kwargs 派发（防可选参左移错位），15 工具语义与原版逐一对齐。ZCode 配置 `.zcode/config.json` 已切 `type: http`。
- GPU/嵌入服务：llama-server（8937 嵌入 / 8938 精排）已换 ROCm 修复库 `libhsa-runtime64.so.1.21.0`（rocm-systems PR #7898，修 WSL2 dxg thunk AsyncEventsLoop 轮询自旋，空闲 232%→5% CPU）。重启脚本 `tools/embed_server.sh`（8937）；8938 精排命令记录于 `/tmp/llama-rerank.log`。回滚：`ln -sfn libhsa-runtime64.so.1.18.0 /opt/rocm/lib/libhsa-runtime64.so.1 && sudo ldconfig`。WSL 下 HIP 枚举设备须 `HSA_ENABLE_DXG_DETECTION=1`。

## 第 2 阶段收官记录（2026-08-29，主会话 phase-closeout）

> 五卡合入链：69b9d39（骨架）→ 5564226（幂等）→ 21ee363（条件系统）→ 308f85e（注册桥）→ 139c8c4（DataGen），docs 镜像至 a6312ab。另有骨架前置 docs 提交与 .cache 出库 1676a38。

**ADR-P2-6 验收线核验**（主会话逐条）：
1. ✅ 根 `gradle clean check --no-build-cache` 188 测（161+7 幂等+20 条件）全绿 0 FROM-CACHE，根 src 零 net.minecraft。
2. ✅ `:mdk:build` 出 jar；`:mdk:runServer` headless 到 Done（三次不同形态验证：桥卡 3.471s / DataGen 卡 11.703s / 合并态 3.407s）；`:mdk:runData` 净克隆模拟 written:2470 逐字节可复现 + 二跑 written:0 幂等。
3. ✅ 游戏内可见性：runClient 冒烟到资源加载完成——blocks 图集 1024x512x4 烘焙成功（2469 模型 + 103 占位贴图全过解析），gt6 零模组错误（仅环境噪音：narrator libflite 缺失 / ALSA 无声卡 / Realms 离线）。**交互级 /give + tint 目视验证留给用户下次 runClient**。
4. ✅ 幂等回灌：reset→MT.init() 全量重灌 2200 条（Anti*/Any*/TECH 别名/NULL 全量），Invar 恰 1 条配方，postInit 两代对称（MTInitResetIdempotencyTest 7 项固化）。
5. ✅ OP 全量 468 注册（453 字段，条件系统解锁 47 条）。
6. ✅ 六卡 GPG 验签 + review-merge 合入 + 落账齐全（state/KG/remember/docs）。

**阶段成果数字**：注册 2469 物品（ingot 483 / dust 1096 / gem 217 / plate 673，判据=isGeneratingItem 上游逐字）；91 条合金反向引用接线；2470 生成 JSON（模型 2469 + lang 2241 条两表零组合爆炸）；103 占位 PNG（37 图标集×前缀组合）；iconset 语义定论=材料属性（mTextureSetsItems.get(prefix.mIconIndexItem)，上游 PrefixItem.java:136-138）。

**继承遗留（进第 3 阶段池）**：
- 全量前缀注册（421×材料）+ creative tab 按前缀分组（现仅白名单 4 前缀单 tab，ADR-P2-3 有意收缩）
- PrefixRegistry 未 close（注册桥只裁 MaterialRegistry）
- MT.NULL.mHandleMaterial=null（两相化副作用，上游 AnyWoodPlastic；工具卡落地时注意）
- TECH tMake* 串冻结首代（内容代间不变，惰性）
- 服务端侧特例键回退模板名（@OnlyIn 隔离的已知取舍）
- GT6DatagenItems 与注册桥白名单两处判据（items() 主路径天然同步，扩前缀时留意）

## 第 3 阶段收官记录（2026-08-30，主会话 phase-closeout）

> 合入链：43fcb1a（BE 框架，rebase 后 5550c8a…fb759de）→ b49d3e2（Menu/Screen 基建）→ 0348638（全量前缀 + creative tab，内含审查归一 9e230cd 与产物重生成）→ b6b01eb（chest 示例机器，WAVE-2 串行），docs/gitignore 收官镜像 2a04936。WAVE-1 三卡并行（files_scope 零交叠 + ADR-P3-4 GT6Mod 冻结）。

**ADR-P3-8 验收线核验**：
1. ✅ 根 `clean check --no-build-cache` 221 测（root 188 + mdk 33）0 失败 0 FROM-CACHE，根 src 零 net.minecraft。
2. ✅ `:mdk:build` 绿；runServer 合并态 Done（2.903s）零 GT6 ERROR；runData 两跑 written 53785→0 逐字节可复现。
3. ✅ 打开链自动化：RCON `/gt6machine check` → createMenu 90 槽（54 内容 + 36 背包）、stillValid、openers 0→1→0（审查官合并态重放）；chest NBT round-trip 单测 6 项。交互级 GUI 目视留用户。
4. ✅ BE 持久化单测：mdk test sourceSet 33 项（BE 框架 17 + 前缀注册 10 + chest 6）。
5. ✅ GPG 全验（原提交 + rebase 重写 + 审查归一提交逐个 verify-commit）；FILES_SCOPE 零越界；GT6Mod/GTModBusListener diff 为空（每分支核验 + 主会话 0348638..b6b01eb 区间复核）。
6. ✅ 共享 BET+validBlocks 默认（chest 独立 BET 为 ADR-P3-1 授权例外）；capability 缝合不独立成卡；两处 SPEC 偏离（menu 自持注册走 RegisterEvent、注册宇宙 105 item-path）经独立证据裁决升格 ADR。

**阶段成果数字**：56,253 材料物品（105 上游物品路径前缀 × isGeneratingItem，first-wins 零 id 撞键，945 重复对归并，363 非物品前缀留痕跳过）；96 creative tab；56,285 模型 JSON + lang 2,336 键；2,785 占位 PNG（40 iconset）。BE 框架：TileEntityBase01Root/03TicksAndSync 最小面移植 + onTick 八段分发逐字 + vanilla 双通道同步（getUpdateTag/ClientboundBlockEntityDataPacket）+ 共享 BET + capability 缝合 + MaterialStackNBT short 存档兼容。Menu/Screen 基建：GTGuiMenu/GTGuiScreen（bindPlayerInventory 直译）+ Slot 三件套能力模型 + gt6:debug /gt6gui + MenuScreens 唯一挂法。chest 示例机器：54 槽 + 动态行布局（ContainerCommonChest:39-43 逐字）+ gt6:chests tab + /gt6machine 打开链验证指令。

**重大修正（研究期）**：GT6 1.7.10 不把材料身份写进常规 ItemStack NBT——"a"/"i"/"m" 键仅坩埚/熔炼类 BE 内部 NBT（OreDictMaterialStack save/load，消费点 Crucible/Smeltery/Mold/DustFunnel）；常规材料物品身份=注册物品本身，BE 读槽位按 item 反查。

**遗留池（进第 4 阶段+）**：
- 特性层：trapped/comparator/涂装/TESR/lid 动画/getOpenGUIs 1200t 重同步、chest BlockItem loot table（掉落空，归 loot datagen 卡）
- ADR-P3-6 延后池：slotClick 全局拦截（Slot 能力模型重构）、rebootGUIs、cover 负 GUIID
- ContainerData 进度条业务面（第 4 阶段首台真加工机器落地）；第 4 阶段主线=管线/Cover/多方块渲染（BakedModel）
- 交互级验证：/give+tint（P2）与 GUI 目视（P3）留用户 runClient
- 旧池沿用：PrefixRegistry 未 close、MT.NULL.mHandleMaterial=null、TECH tMake 冻结首代、服务端特例键回退、移植进度看板

## 第 4 阶段收官记录（2026-08-30，主会话 phase-closeout）

> 合入链：02b7b8b（recipe-core）→ 5ae1d6f（fluid-pipes，W1 并行）→ 5b07953（machine-oven+f82a104 归一）∥ aa9679b（render-foundation，W2）→ 400732c（multiblock）→ 27e8cce（barrel）→ 87badb3（cover-core+2e89501 归一接线，W3 串行合入）→ 0d21a59（pipe-flow-control，用户追加需求第 8 卡）。

**ADR-P4 验收线核验**：
1. ✅ 根 `clean check` 188 测全绿 + 根 src 零 net.minecraft；mdk 199 测全绿（配方 30+管线 20+渲染 22+oven 18+多方块 20+桶 8+cover 28+流向 14+基建存量，分波累进 83→105→123→143→151→179→199）。
2. ✅ `:mdk:build` 绿；runServer 合并态 Done（3.408s）零 GT6 ERROR；runData 各卡二跑 written:0 幂等。
3. ✅ 服务端自动化验收=RCON 指令链全过：oven place→input→run 三态+8x stone；pipe place 贴靠放置→toggle 断开 inject REJECTED→output 外推恢复→clear 不外推→accept 均衡；multiblock frame→FORMED→拆→did-not-form→wand 自动放置→610tick 保持；cover install→hoe dismantle 掉落；barrel accept 16000L→melt 熔毁。交互级目视（oven GUI/cover 板/管道箭头）留用户 runClient。
4. ✅ NBT round-trip 单测扩面：FluidTankGT LAmount 溢出/CoverData 6 面/mTargetPos/ioMask/mProgress/mEnergy。
5. ✅ GPG 全验（含 rebase 重签与审查归一提交）；FILES_SCOPE 零越界；GT6Mod/GTModBusListener diff 为空；datagen W3 显式合并序 multiblock→barrel→cover 执行。

**域语义定论**：配方=findRecipe 只查+isRecipeInputEqual 两段式消耗+RecipeMapFurnace vanilla RecipeManager 桥（Recipe 全落 mdk，根 gregapi 零 MC import）；流体=逐段 BE 泵送无网络对象+FluidTankGT long 内部量/LAmount 溢出键+防回流一轮掩码+分相错峰；机器=进度=能量单位（mProgress+=min(mInputMax,mEnergy)）+A 常量满压假电源+C 红石独立闩锁+**mIgnited 是 post-action 复检窗口不可裁**；渲染=RENDER_SNAPSHOT 快照契约（禁 BE 引用）+GTDynamicBakedModel+scheduleRenderUpdate 双件套（**requestModelDataUpdate 单独不触发 chunk rebuild**）；多方块=代码即 pattern 逐字直译（否决 GTCEu DSL）+四路触发+checkAndSetTarget 两遍 wand 语义+FORMED property；cover=CoverData 6 面并行数组+零 GUI+CoverPlateModel epsilon 0.002；barrel=16000L 粘性罐+熔毁双支；管道（用户特性）=**绝不自动握手**（onPlaced 仅贴靠面 OPOS 连接）+右键逐面连接切换+shift 右键逐面输出箭头（ioMask XOR）+isOutputFace 外推门控。

**承重教训**：BaseEntityBlock 禁自创 onRemove 覆写（BE kill+recreate 循环，LevelChunk:292 CHECK 分支保 BE）；headless 验收 spawn 外 chunk 已加载不实体 tick，须 `/forceload add 0 0` 前置；FluidType 温度/密度离线不可查表（RegistryObject NPE），判定逻辑留原始值比较缝。

**遗留移交（进第 5 阶段池，state key=p5-pool）**：
- 管道语义修正（用户已确认）：ioMask==0 时恢复 GT6 原版全外推；箭头面拒绝回流（canAcceptFluidsFrom=connected&&!isOutputFace）
- 桶重力侧规则（注入六面全开，抽液=底面重流体/顶面轻流体，FL.lighter=density<0→FluidType.getDensity）+泵盖 CoverPump 正统移植
- GTCEu 式扳手交互 UI（9 宫格红绿图标覆盖层）+箭头等贴图从 GTCEu 资产直接借用
- RCON 验收工具链合入主项目 tools/rcon/（现各 agent /tmp 一次性脚本五份重复）
- 旧池沿用：CokeOven 加工业务（RM.CokeOven 配方考古）、机器族、C/D 档渲染升级、D 完整能量网、cover intercept 族、FluidTankGT keepFilter 0 量持久化缺口（归 Logistics 罐卡）、chest loot table、ADR-P3-6 池、PrefixRegistry 未 close、移植进度看板

## 第 5 阶段收官记录（2026-08-30，主会话 phase-closeout）

> 合入链：aa7d9aa（rcon-tooling）→ 4b415c8（pipe-flow-semantics）→ 18b7fe5（wrench-ui-gtceu）→ 7e7dbbb（barrel-side-rules，13 提交）。四卡全程后台并行派发（id68 纪律），review-merge 串行审查逐卡合入。阶段外基建：RAG project 源扩为移植仓库本体（7c2fa46）+ brain 内置 10 分钟自动索引（dd8d33c）+ state_update 深合并修复（080f544/e4c7923）。

**各卡语义定论**：
- **p5-rcon-tooling**（aa7d9aa）：`tools/rcon/gt6rcon.py`（279 行正典客户端：帧协议/auth 后排空/按 rid 多帧收集/--expect 断言/FAILED 检测/退出码 0·1·2·3）+ README 四节（协议/服务端开启/nohup 短轮询纪律/五族机器标准链目录）+ AGENTS.md 指针行——后续所有任务卡的验收链一律复用，不再每任务重写。
- **p5-pipe-flow-semantics**（4b415c8）：管道流向终态（用户裁定落地）= `externalPushAllowed`（ioMask==0 → GT6 默认全外推；ioMask!=0 → 仅 isOutputFace）+ `SideFluidHandler.fill` 静态拒回流（包装层、先于 onFilledFrom，拒绝零防回流位；`canAcceptFluidsFrom`/`getFluidTankFillable` 零改动——1.7.10 :386 管↔管接收端直调实证，门进 BE 层会连带杀均压）。
- **p5-wrench-ui-gtceu**（18b7fe5）：GTCEu 九宫格扳手 UI。**红线首次修订为双判据**：持久态渲染（绑定方块存续、进 chunk mesh）=BakedModel 唯一路线不变；瞬态输入反馈 overlay（生命周期绑定指针悬停、每帧重建）=唯一合法挂点 `RenderHighlightEvent.Block`（本仓首个 FORGE 主总线监听器），三约束=零 BE 静态引用/零写入/不取消事件。三件套 GTWrenchGridTables（纯 MC-free，cellSide 与点击拾取同源 UT6.getSideWrenching）/GTWrenchGridRenderer（世界坐标直绘）/GTWrenchHighlightListener + 3 张贴图（LGPL-3.0-or-later 署名随目录 README）。oven 朝向旋转裁池。
- **p5-barrel-side-rules**（7e7dbbb）：桶六面可注入；被动重力排放（底排 density>0/顶排 density<0/侧只进，严格 GT6 符号口径不采 Forge javadoc ≤0，1000 L/tick 预算，fill-then-drain 推不进不扣源）；CoverPump（上游 :42-98 裁译：秒拍/1000 L/s/visual 0出1进/单向门逐字/getCoverPumpTank 直调 host 罐绕自家 wrapper）；桶 BE 挂 ICoverableTE；gt6:natural_gas（density -100）作顶面排放验收载体；泵 pull 端走对端 side-less capability 防对端侧规则死锁（裁定⑥ side-less=作为整体的罐）。

**最终门禁**：根 clean check 188 + mdk 229 测全绿；runData 二跑 written:0；runServer 合并态零 GT6 ERROR；RCON 链合并态复放 pipe 19/19 + barrel 38/38；GPG 全验（含多轮 rebase 重签）。

**承重教训（新增）**：vanilla TEXT RenderType composite 默认 CULL——世界直绘 quad face 1/2/5 顶点发射序须反转（u×v 叉积定向，详见 remember id81）；vanilla 1.20.1 RCON 响应无 NUL 填充——解析须按长度字段/rstrip，勿盲截两字节；normal 世界出生点 y=64 地形 solid——RCON 链前置 `/fill ... air` 清场；RCON 脚本 cmd() 首匹配帧即返（死等会吃掉泵/重力的 tick 时序）。

**遗留（进第 6 阶段池，state todo pool）**：RM.CokeOven 加工业务（研究卡先行）、oven 朝向旋转（front_facing_rotation 贴图随卡再借）、Metal/Plastic 桶恢复装饰盖限制、渲染器两处次要观察清理（:114 死方法/:151 注释措辞）、tools/rcon README 增补响应无 NUL 事实、旧池沿用（机器族/能量网/cover intercept 族/barrel 密封发酵连通罐/C·D 档渲染/ADR-P3-6 等）。runClient 目视留用户：扳手九宫格 UI 六条、泵盖 plate/pump 贴图、natural_gas 外观。

## 第 6 阶段收官记录（2026-08-31，主会话 phase-closeout）

> 入口范围：研究卡 RM.CokeOven（id84 考古：RM.java:61-161 共 85 图；CokeOven 加工全在 TileEntityBase10MultiBlockMachine→MultiTileEntityBasicMachine，NBT_RECIPEMAP 注册注入，TU 自发电无电点燃续烧，并行 16，3600t）+ 三张实现卡。合入链：7d7a73f（oven-rotation）→ cd46e3a（barrel-metal-plastic）→ 9839b39（cokeoven-processing），批量 review-merge 单会话三连审全 approve。插曲：zcode 客户端重启中断一轮（三 agent 按残局进度精准 resume：oven 接手 2 提交续做/barrel 重跑/cokeoven 架构师重做）；用户裁定修正：焦炉原木配方改 `#minecraft:logs` tag 驱动适配其他 mod（上游 OreDict:205 监听器的 1.20.1 对位）；tools/rcon README 增补帧尾 NUL 读帧纪律（e8da825，纠正 P5 锚点「响应无 NUL 填充」失真措辞——事实=两 NUL 计入 length，真坑是 length-10 读法后再盲切的**双剥**）。

**各卡语义定论**：
- **p6-oven-rotation**（7d7a73f）：GTCEu setFrontFacing 经扳手九宫格——旋转分支插 GTOvenBlock.use 的 onCoverUse 之后 openScreen 之前（cover 意图构造性优先，零重排）；side 解析与九宫格同源 UT6.getSideWrenching；合法性=side∈[2,5]≠facing，shift 非法=CONSUME 永不开 GUI；setFrontFacing=no-op 同向→mFacing+setChanged+applyVisualState（FACING 已驱动 blockstate 16 变体=零 datagen）；GTWrenchGridTables.ovenCellIcon 纯函数（432 断言全表）+Renderer 抽 drawCellIcon helper（pipe 路径字节级不变）；tool_front_facing_rotation.png 第 4 张借图署名；顺手清 :113 死方法+:151 注释措辞。
- **p6-barrel-metal-plastic**（cd46e3a）：木桶恢复上游 MultiTileEntityBarrelWood.java:39 逐字谓词 allowCover→isDecorative（plate 白名单过/泵盖拒；违规已装盖 checkCoverValidity 首 tick 掉落自愈）；新 gt6:barrel_plastic（32000L/370K/仅装饰盖）+gt6:barrel_metal（64000L/全盖/melting MAX_VALUE=声明偏离，材质熔点桥入池）；容量熔点走 GTBarrelBlock 块载体 capacityL()+ticker Supplier 参数；P5 基类冻结面 diff 全空；CoverPumpTest 宿主 wood→metal 越界经审查 accept（P5「木桶全开」假设被本卡推翻后的强制镜像，零断言弱化，覆盖迁移 GTBarrelFamilyTest）。
- **p6-cokeoven-processing**（9839b39）：首台真加工多方块机——新 TileEntityBase10MultiBlockMachine 基类（BasicMachine 裁剪直译：TU 自发电/两段式 checkRecipe/并行 16/点火门/canOutput 逐字/doOutputFluids fill-then-deduct）；TileEntityCokeOven 改继承+getFluidOutputTarget=tY-2 层 3x3 UP 面 capability 扫描+缓存失效重扫；COKE_OVEN 图（RM.java:78 逐参）+静态 24 行（煤 12+褐煤 12；block 族 6+油页岩 9 裁池）+gt6:creosote（density+1000 port-owned 载体值）；**用户裁定落地：原木配方 #minecraft:logs tag 驱动**（GT6CokeOvenLogExpansion 纯函数+GT6CokeOvenTagListener FORGE 总线 TagsUpdatedEvent shouldUpdateStaticData 门，身份子集替换幂等，40 条 tag 配方活证）；研究卡"流体输出撞 P5 侧规则"风险被证伪（fill 六面全开仅 drain 受门，推液走 UP 面=上游 SIDE_TOP 1:1）。

**最终门禁**：根 clean check 188 + mdk 263 测全绿（真并集口径：229+6+4+24）；runData 二跑 written:0；runServer 合并态零 ERROR（recipes poured 24/0 + log recipes rebuilt 40）；RCON 三链合并态复放 oven 17/17 + barrel 32/32 + cokeoven 19/19（跨卡集成点=creosote 进 barrel 卡改过的桶，活证）；GPG 全验（含 rebase 重签）。

**承重教训（新增）**：`@EventBusSubscriber` 注解扫描在 mod 构造期 class-load 监听类——静态表捕获的 OP/MT 引用当时为 null，离线测试因 @BeforeAll 先 initMaterials 掩盖（a9027ac 修复=惰性求值+resetForTest 清缓存）；RCON 帧尾 2×NUL 不消费→每包 2 字节泄漏全流错位（读帧纪律已入 tools/rcon/README.md）；离线 mod Block 构造被 intrusive-holder 冻结闸+Not-bootstrapped 双堵（活体 /gt6tank stat 承担载体断言，沿 p4 先例）。

**遗留（池，state todo pool）**：cokeoven 回补行（gt6:oil/asphalt→油页岩 9 条、block 族 6 条、beam/竹/木弹丸）、metal 高档鼓 128K→10B 注册行、材质熔点桥、CokeOven GUI/Menu+控制器流体罐 capability、RM 壳缺口池（containsInput/minTankSize/三哈希索引/auto-IO）、creosote 密度桥、per-recipe Config duration、旧池沿用（机器族/C+D 档渲染/能量网/cover intercept 族/barrel 密封发酵连通罐等）。runClient 目视留用户：oven 旋转六条、新桶外观与 plate 渲染、flint 点火、creosote 流体渲染。
