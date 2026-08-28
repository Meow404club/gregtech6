# 项目状态（镜像）

> 权威数据在 MCP `gt6-brain` 的 state 里（`state_read()`）。本文件是人可读镜像，
> 由各 Agent 在重大状态变更时同步。模板如下，随进度填充。

## 当前阶段

`第 0 阶段：基础设施搭建`（框架、检索库、工作流已就绪，等待第一份架构拆解）

## 里程碑

- [ ] 第 1 阶段：材料系统纯逻辑抽取（无 MC 依赖，含单元测试）
- [ ] 第 2 阶段：注册表 + DataGen（DeferredRegister + 生成模型/配方）
- [ ] 第 3 阶段：BlockEntity + AbstractContainerMenu + Screen 框架
- [ ] 第 4 阶段：管线 / Cover / 多方块渲染（BakedModel 路线）

## 关键决策

（由 /architect 与 /researcher 持续写入，同步至此）

| 日期 | 主题 | 决策 | 证据 |
|---|---|---|---|
| 2026-08-28 | 基础设施 | 检索采用混合语义+BM25+RRF；原版 1.20.1 反编译源码入索引 | tools/README.md |

## 已知 Bug

（见 `state_read("known_bugs")`）

## 已知陷阱（KG 摘录）

（见 `kg_query()`）

## 基础设施备忘

- 反编译脚本：`tmp/get_vanilla.sh`（1.20.1 客户端 + SpecialSource 重映射 + Vineflower）
- 全量重建索引：`cd tools && .venv/bin/python -m gt6_rag.index all`
- 新资料源登记：`tools/sources.json` → `refresh_index`
