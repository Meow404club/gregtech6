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

## 第 3 阶段（BlockEntity + 容器 + Screen 框架）——入口

- [ ] /architect 拆第 3 阶段模块卡（BlockEntity + AbstractContainerMenu + Screen；全量前缀注册 421×材料 + creative tab 分组归此阶段）
- [ ] /researcher（按需）：BlockEntity 类型注册/-capability/Menu 网络同步在 1.20.1 的模板考古

## 从第 2 阶段继承的遗留（池）

- [ ] 全量前缀注册（421×材料）+ creative tab 按前缀分组（现白名单 4 前缀单 tab，ADR-P2-3 有意收缩）
- [ ] PrefixRegistry 未 close（注册桥只裁 MaterialRegistry）
- [ ] MT.NULL.mHandleMaterial=null（两相化副作用，上游 AnyWoodPlastic；工具卡落地时注意）
- [ ] TECH tMake* 串冻结首代（内容代间不变，惰性）
- [ ] 服务端侧特例键回退模板名（@OnlyIn 隔离取舍，GTCEu 同款）
- [ ] GT6DatagenItems 与注册桥白名单两处判据（扩前缀时同步）
- [ ] 交互级 /give + tint 目视验证（用户下次 runClient 顺手验）

## 待办池

- [ ] 建立移植进度看板（按 GT6 子系统统计已移植/未移植）
