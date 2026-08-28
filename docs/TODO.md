# TODO（镜像）

> 权威数据在 `state_read("todo")`。新增任务请同时写 MCP state 与本文件。

## 立即（基础设施收尾）

- [x] 首次架构拆解：`/architect GT6 材料系统`，产出第 1 阶段模块卡（2026-08-28，5 卡）
- [x] 验证 MCP 服务器在 ZCode 会话中自动连接（gt6-brain 六键 state 全通）
- [x] 试运行一次完整任务流：/researcher → /coder → /review-merge（卡1 gt-material-foundation 已合入 2f3be03）
- [ ] 卡2 gt-material-model：OreDictMaterial 完整模型 + 注册状态机 + MaterialResolver（in_progress）

## 第 1 阶段（材料系统）

- [ ] 研究：GT6 Materials 类族结构与属性字段全集（/researcher）
- [ ] 设计：现代材料注册方案（纯 Java 层 + 单元测试，无 MC 依赖）

## 待办池

- [ ] 评估 NeoForge 1.20.1 的 DataGen 在本项目的落地模板
- [ ] 建立移植进度看板（按 GT6 子系统统计已移植/未移植）
