---
name: gt6-research
description: "GT6 考古与现代 API 求证方法论：如何用语义检索阅读 1.7.10 老代码、用原版反编译源码与 Forge/NeoForge 文档验证现代等价实现、用映射表解混淆名。当需要研究 GT6 某系统实现、确认某个现代 API 用法、对比 1.7.10 与 1.20.1 差异、或用户问 '某个老机制在新版本怎么做' 时使用。"
---

# GT6 考古与求证方法

## 原则：三层证据，绝不猜测

1. **第一层·原版语义**：`search_code(query, sources=["vanilla"])` 查 1.20.1 反编译源码——
   这是 Mojang 官方映射名下的真实实现，是 API 行为的最终真相。
2. **第二层·模组加载器**：`sources=["neoforge-api","forge-api","neoforge-docs","forge-docs"]`
   查 API 签名与官方文档；注意 forge-api 里同时有 1.7.10 分支可供新旧对照。
3. **第三层·同类实现**：`sources=["gtceu-modern"]` 查 GTCEu Modern 怎么做同一件事——
   它是 GT 系机器在现代版本最完整的参考翻译。

## 检索顺序（渐进式，先便宜后贵）

1. `recall("<问题关键词>")` —— 历史研究结论可能已经存在。
2. `search_code` 语义召回（cAST 结构分块 + 混合 BM25/向量融合），块的 header 带
   `文件 > 类 > 方法` 祖先路径，可快速判断相关性。
3. 命中后**必须** `get_source` 打开原文确认上下文，禁止只凭片段下结论。
4. 精确符号定位用 `sym_query`（ripgrep）：`sym_query("class GT_Material", sources=["gt6"])`。

## 多跳问题：GraphRAG 式检索法（KG 优先）

当问题的答案需要**串联多个事实**（"X 属于哪个系统→那个系统的负责人是谁"），或
`search_code` 只能搜到碎片时，切换到 KG 走图遍历（我们的 KG 就是 GraphRAG 的图，
由 agent 日常考古时增量构建，无需 LLM 离线建图）：

1. `kg_search("<问题的核心概念>")` 语义定位起点实体；
2. `kg_query(entity="<实体名>")` 拿它的全部关系（1 跳）；
3. 沿关系边跳到下一跳实体，重复 2（2~3 跳通常足够）；
4. 对涉及的每个实体，再回 `search_code`/`get_source` 拿代码证据；
5. 结论落账时**把推理链上的每条边都 kg_add**，让下次的图检索能复用这条路径。

多跳问题的产出应该是一段推理链：`A -[rel]-> B -[rel]-> C` + 每个节点的证据文件。

## 标准考古流程

1. 读老代码：定位 → 通读关键方法 → 记录语义（数值、单位、边界条件、副作用顺序）。
2. 找现代对应物：按三层证据链确认每个候选 API 的签名（必须 `get_source` 看到原文）。
3. 混淆名解密：`mappings_lookup("<名字>")`。
4. 产出落账：
   - `remember(kind="research", text="<结论一句话 + 文件:行号证据>")`
   - `state_update(key="decisions", merge=true, value=[{"topic","decision","evidence","date"}])`
   - `kg_add("GT6_<老>", "MAPS_TO", "MC_<新>", node_types={...})`（三元组自动入语义索引）

## 新旧概念速查（仍需证据确认）

TileEntity→BlockEntity；Container+GuiContainer→AbstractContainerMenu+Screen；
getDescriptionPacket→Menu#broadcastChanges / CustomPacketPayload；
ISmartItemModel/ITTSR→BakedModel；World→Level；EntityPlayer→Player；
ItemStack metadata→DataComponents/BlockState 属性；GameRegistry→DeferredRegister；
ISimpleBlockRenderingHandler→BakedModel。

## 新资料接入

克隆了新参考仓库后：在 `tools/sources.json` 登记 → `refresh_index(source="<新源>")` →
等 `tmp/index/refresh.log` 显示完成后即可检索。
