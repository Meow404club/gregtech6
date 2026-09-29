# TODO（镜像·维护期版 2026-09-30）

> 权威数据在 MCP `gt6-brain` state（`tasks.r9-issue-wave` 等）。
> P1-P38 已完成项全史见 [docs/archive/ARCHIVE-2026-09-27-p1-p37.md](archive/ARCHIVE-2026-09-27-p1-p37.md)。

## r9 波（#34/#39/#41）· 已收官（2026-09-30）

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
