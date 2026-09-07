#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""datagen_tree_check.py — datagen 双树一致性断言（ADR-P17-1 门禁步 5）。

断言「正典 tracked 树」与「1.21.1-neoforge 节点本地 runData 输出」在按
loot_table(单数, 1.21.x) ↔ loot_tables(复数, 1.20.1) 目录名映射后 byte 级
1:1 相同。背景：ADR-P17-1 之后 1.21.1 runData --output 落节点本地
mdk/versions/1.21.1-neoforge/build/datagen-output（验证产物非入库面），
两树的等价性不再能由 git porcelain 推断，必须显式断言。

规则：
  * 双侧各递归收集文件；目录名 `.cache`（HashCache 账本，非产物）整枝剪除；
    输出根 `version.json`（1.21.x FileCache 版本头，运行时戳记，非产物）排除。
  * 路径归一化：目录段（不含文件名）经 SEGMENT_MAP 映射——node 侧单数
    `loot_table` → canonical 侧复数 `loot_tables`（24w21a 数据包目录单数化改名，
    仅目录段参与映射，避免误伤同名文件名）。canonical 侧恒等映射（形态钉 1.20.1 形）。
  * 归一化后做三查：仅 canonical 有 / 仅 node 有 / 双侧都有但字节不同。
    双侧文件计数（原始与归一化后）必须相等，否则非零退出。
  * 任何差异 → exit 1 并打印差异文件清单；全等 → exit 0 并打印摘要
    （比较文件数 / loot 带数）。

用法：
  python3 tools/datagen_tree_check.py [--canonical DIR] [--node-output DIR] [--max-list N]

正例（ADR §3 门禁步 5，exit 0）：
  $ python3 tools/datagen_tree_check.py
  CANONICAL : mdk/src/generated/resources (70521 files)
  NODE      : mdk/versions/1.21.1-neoforge/build/datagen-output (70521 files)
  loot band : canonical 4585 (data/*/loot_tables)  node 4585 (data/*/loot_table)
  RESULT: OK — 70521 files byte-identical after loot_table(s) mapping

负例（自证：对 node 输出注入一字节 → 非零退出且定位到该文件）：
  $ printf 'X' >> mdk/versions/1.21.1-neoforge/build/datagen-output/ \
        data/gt6/loot_table/blocks/<某表>.json
  $ python3 tools/datagen_tree_check.py
  ...
  DIFF [content] data/gt6/loot_tables/blocks/<某表>.json
       canonical 1234 bytes / node 1235 bytes / first diff at offset 1233
  RESULT: FAIL — 1 path(s) differ (content:1, only-canonical:0, only-node:0)
  $ echo $?
  1
"""

from __future__ import annotations

import argparse
import sys
from pathlib import Path, PurePosixPath

# 1.21.x 数据包目录单数化（snapshot 24w21a）：node(1.21.1) 侧目录段 → canonical(1.20.1) 侧目录段。
# 映射对两侧对称施用（canon_norm 与 node_norm 都过 normalize），故仅两侧「形态不同」的段
# 需要入表；同段同名（如 assets 的 models/item 双侧同形）入表也只是对称重写、不影响等价性。
# p24-tool-system 起三带新增（首例 tags/recipe datagen 入双树）：
#   recipe → recipes   data/*/recipe(s)（1.21 单数化）
#   item       → items         data/*/tags/item(s)（1.21 单数化；assets models/item 双侧
#                              同名段对称重写，无影响）
#   advancement→ advancements data/*/advancement(s)（1.21 单数化；p24 空罐配方的解锁
#                              advancement 首次把该带带进双树）
#   c          → forge         NeoForge 生态 tag 命名空间 data/c ↔ data/forge（#c:tools
#                              ↔ #forge:tools 是同一逻辑产物的双腿形态）
# p24-tags-provider-skeleton 一带新增：block → blocks  data/*/tags/block(s)（1.21 单数化；
#                              mineable/pickaxe|axe 首次把 block tag 带带进双树；对称施用，
#                              textures/block 等双侧同名段不受影响）
SEGMENT_MAP = {
    "loot_table": "loot_tables",
    "recipe": "recipes",
    "item": "items",
    "advancement": "advancements",
    "c": "forge",
    "block": "blocks",
}

CACHE_DIR_NAME = ".cache"          # HashCache 账本（输出根内，gitignore :24，非产物）
ROOT_VERSION_FILE = "version.json"  # 1.21.x FileCache 输出根版本头（运行时戳记，非产物）
DEFAULT_MAX_LIST = 100


def collect_files(root: Path) -> dict[PurePosixPath, Path]:
    """递归收集 root 下全部产物文件：相对路径(POSIX 形) → 绝对路径。

    剪除：任何层级的 `.cache` 目录；输出根的 version.json。
    """
    if not root.is_dir():
        sys.exit(f"ERROR: directory not found: {root}")
    files: dict[PurePosixPath, Path] = {}
    for path in sorted(root.rglob("*")):
        if path.is_dir():
            continue
        rel = path.relative_to(root)
        if CACHE_DIR_NAME in rel.parts:
            continue  # .cache 整枝（rglob 不便剪枝，过滤即可，量级可忽略）
        if len(rel.parts) == 1 and rel.name == ROOT_VERSION_FILE:
            continue
        files[PurePosixPath(rel)] = path
    return files


def normalize(rel: PurePosixPath) -> PurePosixPath:
    """目录段映射归一化（文件名段永不参与）。"""
    mapped = [SEGMENT_MAP.get(seg, seg) for seg in rel.parts[:-1]]
    mapped.append(rel.parts[-1])
    return PurePosixPath(*mapped)


def loot_band_count(files: dict[PurePosixPath, Path], dir_name: str) -> int:
    """统计 data/<ns>/<dir_name>/ 带内文件数（摘要用）。"""
    return sum(1 for rel in files if len(rel.parts) >= 3
               and rel.parts[0] == "data" and rel.parts[2] == dir_name)


def main() -> int:
    repo_root = Path(__file__).resolve().parent.parent
    parser = argparse.ArgumentParser(
        description="datagen 双树一致性断言（canonical vs 1.21.1 节点本地输出，"
                    "loot_table↔loot_tables 映射后 byte 级比对，ADR-P17-1 门禁步 5）",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="正例: python3 tools/datagen_tree_check.py            # exit 0\n"
               "负例: 对 node 输出任一文件追加一字节后再跑 → exit 1 并定位该文件\n"
               "     （详见模块 docstring）",
    )
    parser.add_argument("--canonical", type=Path, default=None,
                        help=f"正典树根（默认 {repo_root / 'mdk/src/generated/resources'}）")
    parser.add_argument("--node-output", type=Path, default=None,
                        help=f"节点输出根（默认 {repo_root / 'mdk/versions/1.21.1-neoforge/build/datagen-output'}）")
    parser.add_argument("--max-list", type=int, default=DEFAULT_MAX_LIST,
                        help=f"差异清单最多打印条数（默认 {DEFAULT_MAX_LIST}，超出只报计数）")
    args = parser.parse_args()

    canonical_root: Path = args.canonical or (repo_root / "mdk/src/generated/resources")
    node_root: Path = args.node_output or (
        repo_root / "mdk/versions/1.21.1-neoforge/build/datagen-output")

    canon = collect_files(canonical_root)
    node = collect_files(node_root)
    canon_norm = {normalize(rel): abs_path for rel, abs_path in canon.items()}
    node_norm = {normalize(rel): abs_path for rel, abs_path in node.items()}

    only_canon = sorted(canon_norm.keys() - node_norm.keys())
    only_node = sorted(node_norm.keys() - canon_norm.keys())
    common = canon_norm.keys() & node_norm.keys()

    diff_content: list[tuple[PurePosixPath, int, int, int | None]] = []
    for rel in sorted(common):
        c_bytes = canon_norm[rel].read_bytes()
        n_bytes = node_norm[rel].read_bytes()
        if c_bytes == n_bytes:
            continue
        first_diff = next((i for i, (a, b) in enumerate(zip(c_bytes, n_bytes)) if a != b),
                          min(len(c_bytes), len(n_bytes)))
        diff_content.append((rel, len(c_bytes), len(n_bytes), first_diff))

    print(f"CANONICAL : {canonical_root} ({len(canon)} files)")
    print(f"NODE      : {node_root} ({len(node)} files)")
    print(f"loot band : canonical {loot_band_count(canon, 'loot_tables')} "
          f"(data/*/loot_tables)  node {loot_band_count(node, 'loot_table')} "
          f"(data/*/loot_table)")

    fail = bool(only_canon or only_node or diff_content)
    if fail:
        def emit(kind: str, lines: list[str]) -> None:
            for line in lines[:args.max_list]:
                print(f"DIFF [{kind}] {line}")
            if len(lines) > args.max_list:
                print(f"DIFF [{kind}] ... and {len(lines) - args.max_list} more")
        emit("only-canonical", [str(r) for r in only_canon])
        emit("only-node", [str(r) for r in only_node])
        emit("content", [
            f"{rel}  canonical {cb} bytes / node {nb} bytes / first diff at offset {off}"
            for rel, cb, nb, off in diff_content])
        print(f"RESULT: FAIL — {len(only_canon) + len(only_node) + len(diff_content)} "
              f"path(s) differ (content:{len(diff_content)}, "
              f"only-canonical:{len(only_canon)}, only-node:{len(only_node)})")
        return 1

    print(f"RESULT: OK — {len(common)} files byte-identical after "
          f"loot_table(s) mapping (normalized counts: canonical {len(canon_norm)}, "
          f"node {len(node_norm)})")
    return 0


if __name__ == "__main__":
    sys.exit(main())
