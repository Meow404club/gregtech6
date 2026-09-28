# TODO（镜像·维护期版 2026-09-28）

> 权威数据在 MCP `gt6-brain` state（`tasks.issues-round4` 等）。
> P1-P38 已完成项全史见 [docs/archive/ARCHIVE-2026-09-27-p1-p37.md](archive/ARCHIVE-2026-09-27-p1-p37.md)。

## issue 第四轮（#17-#26）· 已收口

11 卡三审查席全部合入，main=5153c1dbe，issue 关闭 8 条（#17 判决解释/#18/#19/#20/#22/#23/#24/#26），
#21/#25 用户裁定撤下。矿石追加三项：三石贴图迁 vanilla 现行（B 方案）、破损矿专属名、颗粒渲染核验无缺陷（待用户新构建复验）。

## 在途（收官中）

- 阶段末全量 sweep（后台 /tmp/gt6_sweep_full_r4.log）→ 绿则 push origin main → 验 CI
- docs 镜像 + 阶段锚点 + KG LANDED 补账 + kg_stats 健康度检查

## 低优池（维护期按余力取，见 tasks.issues-round4.cards_pool_low）

- r4-24b 大型机器 overlay/active 贴图保真（控制器纯灰无动画）
- r4-24c 多输入行（Mixer/Bath in=6）GUI 布局保真（需动共享 Menu 几何）
- r4-24d 大型机器 mTanksInput 字段遮蔽（基类流体方法见空数组，p34 既有）
- r4-19b 无 getInventory 访问器家族的破坏掉落覆盖（BatteryBox/ZpmDecharger/GeneratorSolid/ReactorCore2x2/ItemPipe）
- 锅炉罐 tooltip 热量读数 + "过半满才输出"提示（issue #17 UX 跟进）
- 矿石用户侧复验：破损矿颗粒渲染（新构建+新区块）；4,480,000L 显示面疑点（若复现）

## 用户侧 field_test

- issue 第四轮 8 项修复的实机复验（贴图染色/六向扳手/掉落/坩埚输入/大型机器 GUI/传送门框架/倒伏树/破损矿名与三石贴图）
- embeddium 三态、kjs 真机三证、30G 统一测试门禁体感（沿承）
