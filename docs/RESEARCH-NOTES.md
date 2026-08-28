# 业界调研笔记（RESEARCH NOTES）

> 本项目的框架设计不是拍脑袋：每条关键决策都对应一个业界来源。
> 升级框架前先来这里查"别人是怎么做的、为什么"。

## 2026-08-28 · 基础框架 + 检索管线

| 来源 | 核心结论 | 本项目落地 |
|---|---|---|
| **Anthropic《How we built our multi-agent research system》** (2025-06) | orchestrator-worker 模式；subagent 是压缩器（各自上下文探索数万 token，只回 1-2k token 结论）；token 用量解释 80% 的性能方差；多 Agent ≈15× token 消耗，只在高价值并行任务值得；编码任务可并行度低于研究任务 | 主会话=组织者（AGENTS.md 宪法），5 个 worker subagent（.zcode/agents/*.md，injectAgentsMd=false）；coder 可并行但文件域隔离、合并串行 |
| **Anthropic《Effective context engineering for AI agents》** (2025-09) | context rot：注意力预算随 token 增长衰退；compaction（压缩重启）；structured note-taking（NOTES.md/记忆工具，上下文外持久化）；just-in-time 检索优于预取灌入；系统提示词取"合适海拔" | state/KG/remember 三层记忆=结构化笔记；AGENTS.md 只放最小高信号集；skills 引导渐进式检索（recall→search_code→get_source） |
| **Anthropic《Contextual Retrieval》** (2024-09) | chunk 前加上下文前缀（50-100 token）使检索失败率降 35%；+BM25 混合降 49%（top-20 融合候选）；不用 LLM 生成前缀时禁止用通用摘要（无效） | chunks 同时嵌入 `header+body` 与 FTS5 BM25 索引；加权 RRF 融合（0.7 向量 + 0.3 BM25）；前缀由 cAST 结构头廉价生成，非 LLM |
| **cAST: Chunking via Abstract Syntax Trees** (arXiv:2506.15655) | 结构感知分块：递归拆过大 AST 节点+兄弟合并；RepoEval Recall@5 +4.3pp；用非空白字符数（nws）计量块大小 | 分块器对齐官方实现 github.com/yilinjz/astchunk：nws 计量（MAX_CHUNK_NWS=1500）、贪心窗口、相邻兄弟窗口合并、ancestors 类/方法路径 |
| **cAST 论文承认的三个局限 → 我们的对策** | ① Contextual Awareness（缺多级上下文）② Multi-view（纯代码视图）③ Inner Execution Dynamics（静态分析不知运行时） | ① chunk_expansion：文件路径+祖先路径+签名头嵌入（参考 astchunk 官方 apply_chunk_expansion）② Javadoc/注释粘附到后续声明节点（多视图）③ 工作流规则强制 `get_source` 读原文验证（动态验证发生在 Agent 环节） |
| **Qwen3-Embedding-4B 模型卡** | query 端加 `Instruct: {task}\nQuery: {q}`，文档端不加；原生 2560 维，MRL 支持任意降维 | query_instruction 按官方格式；truncate_dims=1536（客户端截断+L2 重归一化） |
| **RepoCoder** (arXiv:2303.12570) | 迭代检索-生成：用上一轮产物改进下一轮查询，全场景 +10% | 工作流体现：researcher 结论 → coder 任务卡 EVIDENCE → review 再检索验证，形成检索-实现-验证循环 |
| **mem0** (arXiv:2504.19413) | 记忆两阶段：抽取+更新（ADD/UPDATE/DELETE/NOOP 由 LLM/相似度决策）；图记忆版 +2%；对比全上下文省 90% token | remember(): 相似≥0.97 判重（NOOP）、≥0.80 合并（UPDATE）、否则 ADD；KG 三元组自动嵌入（图记忆+语义检索）；forget 软删除保留审计 |
| **MetaGPT** (arXiv:2308.00352) | SOP 编码进提示词序列减少级联幻觉；流水线角色分工；结构化中间产物 | 任务卡规范（SLUG/SPEC/EVIDENCE/FILES_SCOPE/ACCEPTANCE/BRANCH）=结构化中间产物；角色命令的输出格式固定 |

## 基础设施事实

- 原版 1.20.1 反编译：Mojang client jar + official mappings + SpecialSource 重映射 +
  Vineflower（`tmp/get_vanilla.sh`），4786 个 java 文件。
- 全语料 66,627 块，零超限（硬帽 2000 字符），p50=1157 字符。
- 检索评估结论：**块预算才是召回瓶颈**（在候选数受限的语料上），块大小从 512→2000
  显著改善长函数语义完整性——用 cAST 后窗口以 1500 nws 为目标。

## 2026-08-28(晚) · RAG 方案全景对照（yupi 16 种 RAG 科普 + 本项目现状）

对照来源: https://www.cnblogs.com/yupi/p/19914426 （16 种主流 RAG 方案全景）

| 文章方案 | 本项目状态 | 决策 |
|---|---|---|
| Naive RAG | ✅ 远超（本地 llama.cpp + 混合检索） | — |
| Multi-Query | ➖ 不采用 | 术语规范的专业域收益有限，成本高；列待办观察 |
| HyDE | ➖ 不采用 | GT6/NeoForge 属于模型弱知识域，假答案易跑偏 |
| 语义分块 | ✅ 更优 | cAST 结构分块（比阈值语义分块稳，零 embedding 成本） |
| **层级索引 Parent-Child** | ⚠️ 半有 | cAST 头自带 `文件>类>方法` 祖先路径；get_source 可回原文。暂不建第二层子块索引（块已 ~1.5k token 足够上下文） |
| **Hybrid Search** | ✅ 已有 | BM25(FTS5) + 向量，加权 RRF (0.3/0.7, k=60)；文章确认"几乎所有生产环境都建议" |
| **Reranking 精排** | ⚠️ 轻量版 | 已有标识符重合 boost + 同文件去重；Cross-Encoder 级联（150→20→5）语料 5.5 万块时收益显著，列为可选升级（网关无 reranker 模型，需另部署 bge-reranker 或用 LLM 精排） |
| CRAG / Self-RAG / Adaptive | ➖ 由 Agent 承担 | 我们的架构里"质检/反思/路由"就是 orchestrator 与 researcher 的职责（Agentic RAG 形态），不建独立流水线 |
| **GraphRAG** | ✅ 等价物已有 | KG 三元组即图（mem0 式增量构建，无需 LLM 离线抽实体）；kg_search 语义定位 + kg_query 图遍历 = 多跳检索；gt6-research skill 已写 GraphRAG 式检索指引。Leiden 社区摘要暂不做（KG 规模小，全图可遍历） |
| Text-to-SQL | ➖ 不适用 | 无表格数据源；state/mappings 都是精确工具直查 |
| Agentic RAG | ✅ 已是 | 整个 MCP 工具集就是 Agent Loop 的工具箱 |
| Multi-Agent RAG | ✅ 已是 | orchestrator + 5 角色 subagent 即 Multi-Agent 形态 |

文档分块修正（用户要求）：**md 尽量不切，按节切且带层级**。chunk_markdown 的
max_chars 从 2000 提到 6000（~1.5-2k token，完整一节），只有超大节才段落续切；
所有块 header 携带完整标题层级 `A > B > C`。forge-docs/neoforge-docs/project 已重嵌。

性能修正：向量检索换 numpy 矩阵点积（55k 块 0.4s → 0.1s）。

## 待验证 / 下一步

- [ ] 用 10 个真实考古问题做检索质量基准（Recall@5 人工评判），固化到 `tools/eval/`
- [ ] 观察 BM25 权重（0.3）在混合检索中的实际贡献，必要时调 0.4~0.5
- [ ] MRL 截断 1536 vs 全 2560 维的召回差异对比
