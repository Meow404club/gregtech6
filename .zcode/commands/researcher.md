---
description: 派发考古研究员子代理（gt6-researcher）：三层证据链求证 GT6 机制与现代 API 等价实现，产出老→新映射研究卡。
---
用 Agent 工具派发 `subagent_type: gt6-researcher`。

任务输入：$ARGUMENTS（研究问题要具体、单一，如"GT6 大型锅炉的多方块校验逻辑"）

派发 prompt 必须自包含，至少包含：
1. 研究问题（一句可判完成的话）
2. 已知线索：先 recall/kg_query 查过的结论直接贴入，避免重复考古
3. 期望产出：研究卡（GT6 侧语义 → 现代方案 → 风险差异 → 一句话结论，全部带 文件:行号）
4. 落账要求：remember(kind="research") + kg_add("GT6_X", "MAPS_TO", "MC_Y")

子代理返回后（主 Agent 落账）：state_update(decisions) + 把研究卡编号挂到任务板。
