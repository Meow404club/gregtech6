# TODO（镜像）

> 权威数据在 `state_read("todo")`。新增任务请同时写 MCP state 与本文件。

## 第 1 阶段（材料系统）——✅ 已完成（2026-08-29）

- [x] 卡1 gt-material-foundation：Gradle 骨架 + TagData/OreDictMaterialStack/Serializer（merge 2f3be03，34 测）
- [x] 卡2 gt-material-model：OreDictMaterial 完整模型 + MaterialRegistry 状态机（merge 1b7251c，73 测）
- [x] 卡3 gt-material-dataset：TD 全量 + MT 1273 材料 + AM/ANY（merge e408927，97 测）
- [x] 卡4 gt-ore-prefix：OreDictPrefix + OP 421 前缀 + mPriorityPrefix（merge 3d0aac3，133 测）
- [x] 卡5 gt-material-graph：MaterialGraph 链查询 + 合金组分引用图（merge 972dbad，161 测）

## 立即（第 2 阶段入口）

- [ ] /researcher 调研：NeoForge 1.20.1 注册（DeferredRegister）与 DataGen 落地模板
- [ ] /architect 拆第 2 阶段模块卡（MDK composite/include 挂载方案）

## 第 2 阶段（从第 1 阶段继承的遗留）

- [ ] 卡3 缺口①：MaterialRegistry.reset() 后 AM/ANY 的 INITIALIZED 守卫不感知 reset（~450 条不重注册）
- [ ] 卡3 缺口②③：TECH 别名静态字段测试顺序敏感；Invar 重灌后 2 条同内容配方写入点未定位
- [ ] applyCrucibleAlloyReferences 接线：挂 NeoForge postInit 生命周期
- [ ] 卡4 延后 47 条前缀（依赖 OreDictMaterialCondition 条件系统移植）

## 待办池

- [ ] 建立移植进度看板（按 GT6 子系统统计已移植/未移植）
