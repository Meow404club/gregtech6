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

## 标准考古流程

1. **读老代码**：`sym_query(pattern="class <老类名>", sources=["gt6"])` 定位 →
   `get_source` 通读关键方法 → 在 KG 里记录该类的职责（`kg_add("GT6_<类名>", "DOES", "<职责>")`）。
2. **找现代对应物**：
   - 老概念 → 现代概念速查：TileEntity→BlockEntity；Container+GuiContainer→AbstractContainerMenu+Screen；
     getDescriptionPacket→Menu#broadcastChanges / CustomPacketPayload；ISmartItemModel/ITTSR→BakedModel+BlockStateModel；
     World→Level；EntityPlayer→Player；ItemStack metadata→DataComponents/BlockState 属性；
     GameRegistry→DeferredRegister；ISimpleBlockRenderingHandler→BakedModel。
   - 每个对应物都要用第一/二层证据确认签名，不许凭记忆写。
3. **混淆名解密**：看到 `a.b.c` 之类名字 → `mappings_lookup("<名字>")`。
4. **产出**：研究结论写回 `state_update(key="decisions", merge=true, value=[{"topic":…, "decision":…, "evidence":"<文件:行号>"}])`，
   并在 KG 建立老→新的映射关系：`kg_add("GT6_<老>", "MAPS_TO", "MC_<新>", node_types={...})`。

## 检索技巧

- `search_code` 是语义检索：用英文描述意图效果最好，如
  `"multi-block structure controller check structure valid"`。
- `path_glob` 收窄范围：`"* TileEntity*.java"`、`"*/render/*"`。
- 语义检索召回后**必须**用 `get_source` 打开原文确认上下文，禁止只凭片段写代码。
- `sym_query` 适合精确符号：`sym_query("class GT_Machine$", sources=["gt6"], glob="*.java")`。

## 新资料接入

克隆了新参考仓库后：在 `tools/sources.json` 登记 → `refresh_index(source="<新源>")` →
等 `tmp/index/refresh.log` 显示完成后即可检索。
