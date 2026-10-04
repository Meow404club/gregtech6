# TODO（镜像·维护期版 2026-10-03 · pool-drain 收官）

> 权威数据在 MCP `gt6-brain` state（`tasks.pool-drain-wave` 及 cards.* 平键）。
> P1-P38 已完成项全史见 [docs/archive/ARCHIVE-2026-09-27-p1-p37.md](archive/ARCHIVE-2026-09-27-p1-p37.md)。

## pool-drain 波 · 已收官并 push（2026-10-03）

16 卡三席（5+5+1）全 approve，main=origin/main=82a6be2fb（push 41 提交）。要点：
**P0 实机崩端修复**（GTFluids 外域门控家族 null 守卫；根因=CHEMICALS 三行带 IHL/FZ 域，
裸装装载期 NPE——用户可拉新构建复验）；T5 8 行+ice_cream_raisin+sandwich off-by-one 闭合；
机器行族 85 行九图（chum 4 行裁定转录零灌=Scrap Meat 属 T3 池）；骡肉 :574/mutton-rabbit
零代码关闭（维持照灌/维持 declared deviation）；vanilla 配方移除通道（cake/cookie/golden_apple/
golden_carrot 四行运行时过滤，桶 2 行=现代版无 crafting id 声明 no-op）；berry overlay 三模型
双层；mdh-5 #46 worldgen/block 轴四面 census 静态关闭；**余瓶链全域**（35 流体+219 瓶含
48 染料瓶，tab 图标回正+栈列忠实修正）；small-gem-1pct 石层 1% 回退序（浮出吸收项）。
终局全量 567 类 4506 测 0F/0E/5S。

## 残账池（下波候选，显式不扩本波）

- **瓶 capability 容器面决策卡**（250mB 饮用/空瓶返还/腐链 :428-438）+机器灌装行随卡
- blob 石面 1% 小宝石覆盖（需 post-placement 处理器另卡）；nether 透镜 1% 面
- Scrap Meat 1998+锭条 32101-32115=T3 物品池族（另波；解锁 chum 4 行实灌）
- 圣水 CureZombie/Drop_Loot 行为卡 defer
- field_test 目验族：浆果层上屏/相邻壳细缝/透镜挖 ore_small 宝石/瓶外观/移除通道 /reload 实机
- r4-24b 大机 active 贴图三态属性接线（素材 r8 已备齐）
- FML 预算墙 ops 裁定（等用户）：测试启动面 12-13.5G 三撞 filtered 12G——调预算 or 查增重根因
- EMI 槽底叠画观感（等 field）；黏土带形（等用户）；温度计读数行（等温度计移植卡）
- r9 defer 维持：SHARPENING 磨床 13 行/Press 宝石镐/电动直合成行
- 治理：KG 蒸馏（4187 节点超旧阈，孤儿 4 健康）

## 前态 r10 还债波 · 已收官（2026-09-30）

七卡两席全 approve 合入（main=fb479f3f1）：数据丢失级两枚（大机输入罐遮蔽=存档即失+多流体不可达；
六族破坏掉落缺口）+玩家面（nojade 崩溃+齿轮装饰）+观感面（材质 tint+#17 锅炉过半满两脸）+
宝石姊妹行。盘点销卡 r4-24c；r4-19b/24d 债清。

## 前态 r9 波（#34/#39/#41）· 已收官（2026-09-30）

三卡一审查席全 approve 合入+push（main=1b2e1a4cc）：#41 模具阴模还原、#39 工具头 9 族行补全
（'s'=SAW 翻案）、#34 查看器背景两层合成（视觉取证确诊→像素级复验闭环）。
may fix 三挂待提出者复验自关（铁律 9）。CI run 36626354007 在跑（dev prerelease 滚动更新）。

## r9 波未闭合债（权威在 state tasks.r9-issue-wave 各卡 deferred）

- SHARPENING 磨床 map 空→12 个 raw 工具头死端（上游 Handlers.java:403-415 13 行 raw→成品）——机器域
- Press 宝石镐 3 行（Handlers:247-251）随 FormingPress 动态臂——机器域
- 电动工具 :357-360 直合成行（上游自身材质冲突）未深究
- 直合成工具行的宝石姊妹行（wrench/monkey_wrench arg-8 C 变体+hammer G 变体）小卡待开
- NEI.png 齿轮装饰无机器物品叠加（上游 :278 declared defer，可挂 GT6RecipeMapIcons）
- EMI SlotWidget 槽底与烘焙槽框叠画观感（field 判刺眼另卡）
- nojade 下配方页 draw 必崩 NCDFE（known_bugs.r934_nojade_recipe_page_draw_ncdfe，workaround=带 Jade）
- **ops 待裁定**：FML 测试启动面峰值 12-13.5G 三撞 filtered 12G 预算墙——调预算或查根因（疑似 KJS 增重）

## 前态 r8 收官清单（2026-09-29 完成）

- ~~docs 镜像~~✓ ~~filtered 12G 调优~~✓（v3.7）~~curator 蒸馏~~✓ ~~攒批 push~~✓（147+2 提交）
- ~~may fix 挂（#27/#42/#45/#47）~~✓ ~~记忆蒸馏~~✓（12 条硬删留锚点 id1116/教训 1093/1119/1121/1122）

## r8 波未闭合债（终席汇总镜像，权威在 state/known_bugs* 与各卡面）

- GTWireBakedModel 线稿 uvOf 债（r8-uvof-private-copies 私有拷贝同类清扫余留的最后一处）
- 三测试类（FoamPlacement/Circuits/CreativeTabJoinCensus）非离线基类，系留观（FML boot 依赖）
- ItemLatch neo 腿 ARMED=false fixture 窄化——长期解（1.21.1 MappedRegistry 形变根因）可议
- 渲染池动态仪表 L 级 defer（NBT 内容盒/砧轮廓等同池）
- r7-40-41 tint 声明偏差（卡面在案，未返工）
- #45 C1/C2 黏土带形不一致待裁量（生坯 vs 烧成带形，等用户裁定）
- tooltip 域 defer：桶 proof 行/tank 族休眠等（T2/T5 卡面在案）
- r8 D 可选卡已裁砍（不做，非债）

## 低优池（维护期按余力取，见 tasks.issues-round4.cards_pool_low）

- r4-24b 大型机器 overlay/active 贴图保真（控制器纯灰无动画）
- r4-24c 多输入行（Mixer/Bath in=6）GUI 布局保真（需动共享 Menu 几何）
- r4-24d 大型机器 mTanksInput 字段遮蔽（基类流体方法见空数组，p34 既有）
- r4-19b 无 getInventory 访问器家族的破坏掉落覆盖（BatteryBox/ZpmDecharger/GeneratorSolid/ReactorCore2x2/ItemPipe）
- 锅炉罐 tooltip 热量读数 + "过半满才输出"提示（issue #17 UX 跟进）
- 矿石用户侧复验：破损矿颗粒渲染（新构建+新区块）；4,480,000L 显示面疑点（若复现）

## 用户侧 field_test

- issue 第四轮 8 项修复的实机复验（贴图染色/六向扳手/掉落/坩埚输入/大型机器 GUI/传送门框架/倒伏树/破损矿名与三石贴图）
- embeddium 三态、kjs 真机三证、22G 信封统一测试门禁体感（v3.6 起，沿承）
