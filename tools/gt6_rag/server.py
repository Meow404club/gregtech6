#!/usr/bin/env python3
"""GT6 Brain — project MCP server (stdio) exposing retrieval & memory tools."""
from __future__ import annotations

import json
import subprocess
import sys
from pathlib import Path

TOOLS_DIR = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(TOOLS_DIR))

from mcp.server.fastmcp import FastMCP  # noqa: E402

import gt6_rag.search as S  # noqa: E402
from gt6_rag import db  # noqa: E402

mcp = FastMCP(
    "gt6-brain",
    instructions=(
        "GT6 现代复兴计划的记忆与检索中枢。写代码前先用 search_code/sym_query 检索；"
        "不要猜测现代 API——用 search_code 查 vanilla/neoforge-api/forge-api 源码，"
        "用 mappings_lookup 查 1.20.1 混淆名。重要决策写入 state 与 KG。"
    ),
)


def _j(obj) -> str:
    return json.dumps(obj, ensure_ascii=False, indent=1)


@mcp.tool()
def search_code(query: str, sources: list[str] | None = None, limit: int = 8,
                path_glob: str | None = None) -> str:
    """语义检索已索引的源码/文档（GT6 1.7.10、GTCEu Modern、原版 1.20.1 反编译、Forge/NeoForge API 与文档、项目 docs）。

    sources 可选: gt6, gtceu-modern, vanilla, forge-api, neoforge-api, forge-docs, neoforge-docs, project。
    path_glob 如 "*TileEntity*.java"。返回相关代码块（含文件路径、行号、片段）。
    """
    try:
        return _j(S.search_code(query, sources, limit, path_glob))
    except Exception as e:
        return _j({"error": str(e)})


@mcp.tool()
def get_source(file: str, start: int = 1, end: int | None = None) -> str:
    """按仓库根相对路径精确阅读原始文件（带行号），file 形如 tmp/gt6-1.7.10/src/main/java/gregtech/... 或 tmp/vanilla-1.20.1/net/minecraft/..."""
    try:
        return _j(S.get_source(file, start, end))
    except Exception as e:
        return _j({"error": str(e)})


@mcp.tool()
def sym_query(pattern: str, sources: list[str] | None = None,
              glob: str | None = None, limit: int = 40) -> str:
    """ripgrep 正则精确搜索（类名/方法名/字符串常量）。例: pattern="class MetaTileEntity" glob="*.java"。"""
    try:
        return _j(S.sym_query(pattern, sources, glob, limit))
    except Exception as e:
        return _j({"error": str(e)})


@mcp.tool()
def mappings_lookup(term: str) -> str:
    """查询 1.20.1 混淆名 ↔ Mojang 官方映射名（类/字段/方法双向模糊匹配）。看到 a/b/c 之类混淆名时用它。"""
    try:
        return _j(S.mappings_lookup(term))
    except Exception as e:
        return _j({"error": str(e)})


@mcp.tool()
def refresh_index(source: str | None = None) -> str:
    """后台增量重建索引（新克隆/更新的资料需要 refresh 后才能被 search_code 检索）。source 为空则刷新全部。立即返回，进度见 tmp/index/refresh.log。"""
    log = open(db.PROJECT_ROOT / "tmp" / "index" / "refresh.log", "a", encoding="utf-8")
    targets = [source] if source else list(db.load_sources().keys())
    subprocess.Popen(
        [sys.executable, "-m", "gt6_rag.index", *targets],
        cwd=str(TOOLS_DIR), stdout=log, stderr=subprocess.STDOUT,
        env={**__import__("os").environ, "PYTHONUNBUFFERED": "1"},
    )
    return _j({"ok": True, "targets": targets, "log": "tmp/index/refresh.log"})


@mcp.tool()
def kg_add(src: str, rel: str, dst: str, note: str = "",
           node_types: dict | None = None) -> str:
    """知识图谱记录一条关系：src -[rel]-> dst（例: kg_add("GT6_MetaTileEntity","UPGRADES_TO","GT6_MultiMachine")）。
    node_types 可选 {"节点名": "Class|Texture|Recipe|Machine|Material|Concept"}。"""
    try:
        return _j(S.kg_add(src, rel, dst, note, node_types))
    except Exception as e:
        return _j({"error": str(e)})


@mcp.tool()
def kg_query(entity: str | None = None, rel: str | None = None, limit: int = 30) -> str:
    """检索知识图谱：按实体名或关系类型模糊查询。"""
    try:
        return _j(S.kg_query(entity, rel, limit))
    except Exception as e:
        return _j({"error": str(e)})


@mcp.tool()
def kg_del(entity: str) -> str:
    """从知识图谱删除一个实体及其所有关系。"""
    try:
        return _j(S.kg_del(entity))
    except Exception as e:
        return _j({"error": str(e)})


@mcp.tool()
def state_read(key: str | None = None) -> str:
    """读取项目状态记忆。key 可选: decisions, todo, known_bugs, progress, architecture；为空返回全部。"""
    try:
        return _j(S.state_read(key))
    except Exception as e:
        return _j({"error": str(e)})


@mcp.tool()
def state_update(key: str, value, merge: bool = False) -> str:
    """写入项目状态记忆。key 建议: decisions(决策+理由), todo, known_bugs, progress, architecture。
    merge=true 时列表追加/字典合并而非覆盖。value 必须是 JSON 兼容结构。"""
    try:
        return _j(S.state_update(key, value, merge))
    except Exception as e:
        return _j({"error": str(e)})


@mcp.tool()
def project_status() -> str:
    """项目总览：各资料源索引规模、KG 条数、状态键、git worktree 列表、最近提交。"""
    try:
        return _j(S.project_status())
    except Exception as e:
        return _j({"error": str(e)})


if __name__ == "__main__":
    mcp.run(transport="stdio")
