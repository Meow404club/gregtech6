# TODO（镜像·维护期版 2026-09-29）

> 权威数据在 MCP `gt6-brain` state（`tasks.issues-round4` 等）。
> P1-P38 已完成项全史见 [docs/archive/ARCHIVE-2026-09-27-p1-p37.md](archive/ARCHIVE-2026-09-27-p1-p37.md)。

## issue 第四轮（#17-#26）· 已收口

11 卡三审查席全部合入，main=5153c1dbe，issue 关闭 8 条（#17 判决解释/#18/#19/#20/#22/#23/#24/#26），
#21/#25 用户裁定撤下。矿石追加三项：三石贴图迁 vanilla 现行（B 方案）、破损矿专属名、颗粒渲染核验无缺陷（待用户新构建复验）。

## 在途（r8 收官中）

- docs 镜像（本卡：宪法铁律 8 v3.x 同步+PROJECT_STATE/TODO 镜像）
- filtered 12G 调优卡（ops-testgate-v3p7-filtered12，FML boot 大域预算旋钮）
- curator 蒸馏+攒批 push origin main+CI 双腿确认
- may fix 修复说明挂 issue（#27/#42/#45/#47，push 后；不主动关评，铁律 9）
- 记忆蒸馏（r8 handoff/merge 历史硬删，收官锚点先落全）

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
