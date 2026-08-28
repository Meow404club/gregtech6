# GT6 现代复兴计划（MGT6GA）

> GregTech 6（1.7.10）→ 1.20.1 NeoForge 移植工程 · 多 Agent 协作仓库

本项目使用一套**项目级**的 ZCode 协作框架：MCP 记忆中枢（`gt6-brain`）+ 项目技能 +
斜杠命令角色 + GPG 签名的 worktree 工作流。**所有 Agent 与人类协作者必须先读本文件。**

## 一、启动必做（每个会话）

1. 调 MCP 工具 `state_read()` 恢复项目状态（decisions/todo/known_bugs/progress/architecture）。
2. 调 `project_status()` 看索引规模与活跃 worktree。
3. 通读 `docs/PROJECT_STATE.md` 与 `docs/TODO.md`（镜像）。
4. 若索引为空：在 `tools/` 下后台运行 `.venv/bin/python -m gt6_rag.index all`，完成前不做需要检索的工作。

## 二、角色分工（用斜杠命令切换，可派发给子 Agent）

| 命令 | 角色 | 产出 |
|---|---|---|
| `/architect` | 架构师 | 里程碑、模块拆解、架构红线（ADR），不写功能代码 |
| `/researcher` | 考古研究员 | 三层证据链求证的研究结论（老→新映射） |
| `/coder` | 蓝领码农 | 独立 worktree 内的原子签名提交，返还 commit hash |
| `/review-merge` | 审查合并官 | 分支审查、冲突裁决、合入 main |
| `/debugger` | QA 除虫 | 构建修复、Crash 分析、已知 Bug 库 |

主 Agent（你自己）是**调度者**：拆任务 → 派发 coder → 拿到 commit hash → 派发
review-merge → 合入后更新状态。人类是最终架构仲裁。

## 三、五条铁律（违反 = 立即返工）

1. **绝不猜测 API**。现代 API 用法必须先 `search_code` / `get_source` 查
   vanilla / neoforge-api / forge-docs / gtceu-modern 原文；混淆名先 `mappings_lookup`。
2. **绝不裸提交**。永远 `git commit -S -s`（GPG 签名 + Signoff），消息格式
   `<type>(<scope>): <主题>` + `Task:` 行。PreToolUse 钩子会拦截裸 `git commit`。
3. **绝不直接改 main 写代码**。码农在 `../MGT6GA-trees/<task-slug>` worktree 里干活，
   review-merge Agent 审查后才能合入。
4. **绝不手写本应 DataGen 生成的 JSON**；渲染禁止 TESR 一把梭（用 BakedModel）。
5. **绝不留下无记录的决策**。架构决策进 `state_update(decisions)`，结构关系进
   `kg_add`，研究结论必须有 `文件:行号` 证据。

## 四、标准任务流

```
/architect <子系统>            → 出里程碑与模块卡（state/progress 更新）
/researcher <研究卡>           → 老→新映射结论（decisions + KG）
   主 Agent 生成任务卡(task-slug)
/coder <task-slug: 任务描述>   → worktree 实现，返还 COMMITS: hash 列表
/review-merge work/<task-slug> → 审查→合并→清理 worktree→更新状态
/debugger <崩溃日志>           → 修复并记入 known_bugs
```

## 五、记忆体系

- **MCP `gt6-brain`**（`tools/gt6_rag/`，SQLite `tmp/index/rag.db`）：
  - 检索：`search_code`（混合语义+BM25，覆盖 GT6/GTCEu/原版反编译/Forge 与 NeoForge
    文档与 API）、`sym_query`（ripgrep）、`get_source`（读原文）、`mappings_lookup`。
  - 记忆：`state_*`（项目状态）、`kg_*`（知识图谱）、`refresh_index`、`project_status`。
- **知识记忆规范**见技能 `gt6-knowledge`；考古方法论见 `gt6-research`；
  Git 工作流见 `gt6-workflow`；审查协议见 `gt6-review`。

## 六、目录地图

```
tmp/gt6-1.7.10/        GT6 官方源码（移植对象）
tmp/vanilla-1.20.1/    原版 1.20.1 反编译源码（真理之眼）
tmp/refs/gtceu-modern/ GTCEu Modern 1.20.1（现代 GT 参考）
tmp/refs/forge-api/    Forge 1.7.10/1.8.9/1.12.2/1.20.1
tmp/refs/neoforge-api/ NeoForge 1.20.1
tmp/refs/forge-docs/   Forge 官方文档 1.12.x~1.21.x
tmp/refs/neoforge-docs/ NeoForge 官方文档
tools/                 RAG 工具链（本仓库代码，纳入版本控制）
docs/                  架构文档与状态镜像
.zcode/                项目级 ZCode 配置（MCP/hooks/skills/commands）
.githooks/             commit-msg / pre-commit / pre-push 校验
```

`tmp/` 与 `tools/.venv/` 不入库（见 .gitignore）。`tools/config.json` 含密钥，同样不入库。
