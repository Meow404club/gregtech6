# GT6-RAG 工具链

为「GT6 现代复兴计划」提供的本地检索与记忆基础设施。全部工具通过项目级 MCP 服务器 `gt6-brain` 暴露给 ZCode。

## 组成

- `gt6_rag/index.py` —— 索引器：扫描 `sources.json` 中的资料源，切块后调用 Qwen3-Embedding-4B 嵌入，存入 SQLite（`tmp/index/rag.db`）。支持增量（按 mtime/size 跳过未变化文件）。
- `gt6_rag/server.py` —— MCP 服务器（stdio）：语义检索、精确符号搜索、原版反编译源码阅读、混淆映射查询、知识图谱（KG）、项目状态记忆。
- `config.json` —— 嵌入 API 配置（含密钥，已被 .gitignore 排除；模板见 `config.example.json`）。
- `sources.json` —— 资料源注册表（路径相对仓库根）。

## 手动用法

```bash
VENV=tools/.venv/bin/python
$VENV -m gt6_rag.index status              # 查看各资料源索引状态
$VENV -m gt6_rag.index all                 # 全量/增量索引所有源
$VENV -m gt6_rag.index gt6 vanilla         # 只索引指定源
$VENV -m gt6_rag.index --limit-files 5 all # 小规模试跑
```

## 本地推理服务（ZCode 会话前先拉起）

```bash
tools/embed_server.sh    # 嵌入服务 Qwen3-Embedding-4B Q8  → 127.0.0.1:8937（12 槽）
tools/rerank_server.sh   # 精排服务 Qwen3-Reranker-0.6B Q8 → 127.0.0.1:8938（4 槽）
```

llama.cpp 的 `--ctx-size` 是总 KV 上下文（会被槽平分）；rerank 物理批 `-ub` 必须 ≥ 单条
输入 token 数（默认 512 会 500）。远端中转回退配置见 `tools/config.json` 的 remote_fallback。

## MCP 服务器由 ZCode 项目配置自动拉起

```bash
echo '{"jsonrpc":"2.0","id":1,"method":"tools/list"}' | $VENV tools/gt6_rag/server.py
```

## 关于 workspace hook 的信任审核（UI 弹卡问题）

`.zcode/config.json` 里声明的 PreToolUse 钩子属于 workspace 作用域，ZCode 出于安全
会在 UI 弹「workspace hook 审核」卡（提示 `workspace_hooks_execute_code`）。要点：

- 审核卡出现在**会话聊天流**（pending interaction），不在 Settings 页 —— 设置里看不到是正常的。
- 点一次"信任"即持久生效：写入 `~/.zcode/security/workspace-hook-trust-v1.json`
  （按钩子声明的 sha256 digest 记录；钩子内容改动后需重新信任一次）。
- 不想弹卡：把 `.zcode/config.json` 的 `hooks` 段移到用户级
  `~/.zcode/cli/config.json`（用户作用域免审核）。`guard-commit.sh` 已内置 cwd 自检
  （只在本工作区与 MGT6GA-trees 内拦截），移到用户级也不会影响其他项目。
- 即使钩子未信任，GPG 约束仍有 git 层兜底：`commit.gpgsign=true` + `.githooks/*`。

## MCP 工具一览（服务器名 gt6-brain）

| 工具 | 用途 |
|---|---|
| `search_code(query, sources?, limit?, path_glob?)` | 语义检索全部已索引源码/文档 |
| `get_source(file, source, start?, end?)` | 按相对路径读取原始文件（带行号） |
| `sym_query(pattern, sources?, glob?)` | ripgrep 正则精确搜索 |
| `mappings_lookup(term)` | 1.20.1 混淆名 ↔ Mojang 官方名互查 |
| `refresh_index(source?)` | 后台重建/增量更新索引 |
| `kg_add / kg_query / kg_del` | 知识图谱：记录/检索实体关系 |
| `state_read / state_update / state_render` | 项目状态长期记忆（决策/TODO/已知 Bug/进度） |
| `project_status()` | 索引规模、KG 条数、worktree 列表、最近提交 |
