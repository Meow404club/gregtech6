#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""datagen_tree_check.py — datagen 双树一致性断言（ADR-P17-1 门禁步 5，p25 两层归一）。

断言「正典 tracked 树」与「1.21.1-neoforge 节点本地 runData 输出」在按
loot_table(单数, 1.21.x) ↔ loot_tables(复数, 1.20.1) 目录名映射后 byte 级
1:1 相同。背景：ADR-P17-1 之后 1.21.1 runData --output 落节点本地
mdk/versions/1.21.1-neoforge/build/datagen-output（验证产物非入库面），
两树的等价性不再能由 git porcelain 推断，必须显式断言。

规则（两层归一，p25-datagen-tree-check-unify 定案）：
  * 双侧各递归收集文件；目录名 `.cache`（HashCache 账本，非产物）整枝剪除；
    输出根 `version.json`（1.21.x FileCache 版本头，运行时戳记，非产物）排除。
  * 层 1 路径归一化：目录段（不含文件名）经 SEGMENT_MAP 映射——node 侧单数
    `loot_table` → canonical 侧复数 `loot_tables`（24w21a 数据包目录单数化改名，
    仅目录段参与映射，避免误伤同名文件名）。canonical 侧恒等映射（形态钉 1.20.1 形）。
  * 层 2 值形归一化（VALUE_NORMALIZERS）：字节不等且命中已注册产物带的文件，
    双侧解析 JSON → node 侧施用该带注册的值形变换（方向恒 node→canonical，
    正典树永不改写）→ 按双腿实测一致的 Gson 格式（indent=2 / 无尾换行 /
    非 ASCII 原样，canonical 全部 77573 个 JSON 往返字节级零差实测）重新序列化
    → 再做字节比对。归一成立 ≠ 静默放过：NORMALIZED 计数与文件清单必须打印；
    未命中带、解析失败、变换后仍不等 → 一律原样 FAIL（保留 first-diff offset）。
    变换必须保守：只对 census 实测钉死的形态施用，不适用形态原样保留，
    由字节比对兜底 FAIL（绝不放宽为全局语义比较）。
  * 归一化后做三查：仅 canonical 有 / 仅 node 有 / 双侧都有但字节不同。
    双侧文件计数（原始与归一化后）必须相等，否则非零退出。
  * 任何未归一差异 → exit 1 并打印差异文件清单；全等 → exit 0 并打印摘要
    （byte 相等数 / normalized 数 / loot 带数）。

用法：
  python3 tools/datagen_tree_check.py [--canonical DIR] [--node-output DIR] [--max-list N]

正例（p25 两层归一后，exit 0）：
  $ python3 tools/datagen_tree_check.py
  CANONICAL : mdk/src/generated/resources (77573 files)
  NODE      : mdk/versions/1.21.1-neoforge/build/datagen-output (77573 files)
  loot band : canonical 4885 (data/*/loot_tables)  node 4885 (data/*/loot_table)
  NORMALIZED [copy-custom-data→copy-nbt] data/gt6/loot_tables/blocks/advanced_crafting_table.json
  ...（normalized 清单逐文件打印，计数进摘要，绝不静默）
  RESULT: OK — 75345 files byte-identical + 228 value-shape normalized after
          path mapping + registered value normalizers (normalized:228)

负例 1（未注册带：对 node 输出任一文件注入一字节 → 非零退出且定位到该文件）：
  $ printf 'X' >> mdk/versions/1.21.1-neoforge/build/datagen-output/ \
        assets/gt6/models/item/<某模型>.json
  $ python3 tools/datagen_tree_check.py
  ...
  DIFF [content] assets/gt6/models/item/<某模型>.json
       canonical 1234 bytes / node 1235 bytes / first diff at offset 1233
  RESULT: FAIL — 1 path(s) differ (content:1, only-canonical:0, only-node:0,
          normalized:0 accepted)
  $ echo $?
  1

负例 2（已注册带但未注册形差：归一不是吞差——对 node loot 表把 "rolls": 1.0
改成 2.0 → 该带虽注册了 copy_nbt/items/enchant 变换，均不适用此差 → 仍 FAIL）：
  DIFF [content] data/gt6/loot_tables/blocks/<某表>.json ... first diff at offset N
  RESULT: FAIL — ...（normalized 只计真正归一成立的文件）
"""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path, PurePosixPath
from typing import Callable

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

# ── 层 2：值形归一器（VALUE_NORMALIZERS，p25-datagen-tree-check-unify 新增）────
# 注册纪律（沿 SEGMENT_MAP 注释证据纪律）：每条变换必须带 ①版本差异出处（vanilla
# 反编译/census 实测）②census 实测样本文件名。方向恒 node(1.21)→canonical(1.20.1)，
# 正典树永不改写。变换必须保守：只对实测钉死的形态施用，形态不符原样返回 False，
# 由字节比对兜底 FAIL——绝不静默吞差，绝不放宽为全局语义比较。
#
# 序列化契约：双腿 datagen 同为 Gson setIndent("  ")，实测（2026-09-08 p25 census，
# canonical 全部 77573 个 JSON 往返）与 json.dumps(obj, indent=2, ensure_ascii=False)
# 字节级一致、无尾换行——归一后按此格式重序列化即可与 canonical 逐字节比对。
#
# census 基线（2026-09-08 双腿新鲜树，HEAD 38365b89）：content 差 228 文件，
# 分类=170 items-arr-vs-str + 26 copy_nbt↔copy_custom_data + 12 配方 advancement
# 三合一 + 6 enchant predicate 重构 + 13 配方 result/tag 形 + 1 spray_can_empty
# advancement tag 形 + 1 spray_can_empty 配方（对账 tasks.p25-datagen-tree-check-unify）。


def _norm_copy_custom_data(o: dict) -> bool:
    """loot 函数名 1.21 改名：copy_custom_data → copy_nbt。

    出处：1.20.1 vanilla CopyNbtFunction.java（tmp/vanilla-1.20.1 .../loot/functions/
    CopyNbtFunction.java:28/:37 LootItemFunctions.COPY_NBT="minecraft:copy_nbt"）；
    1.21.x 改名 minecraft:copy_custom_data。census 样本：loot_tables/blocks/
    advanced_crafting_table.json（26 文件，ops/source/target 内键双腿同形）。
    """
    if o.get("function") == "minecraft:copy_custom_data":
        o["function"] = "minecraft:copy_nbt"
        return True
    return False


def _norm_items_wrap(o: dict) -> bool:
    """ItemPredicate.items 单串 → 数组。

    出处：1.20.1 ItemPredicate "items" 为集合形；1.21.x datagen 对单元素输出裸串。
    census 样本：loot_tables/blocks/andesite.json（match_tool predicate，170 文件）
    + advancements/recipes/decorations/grass.json（criteria inventory_changed 内层
    predicate，与 loot 带同形分注册）。
    """
    v = o.get("items")
    if isinstance(v, str):
        o["items"] = [v]
        return True
    return False


def _norm_items_tag_hash(o: dict) -> bool:
    """ItemPredicate tag 合并形：1.21 "items": "#ns/tag" → 1.20.1 "tag": "ns/tag"。

    出处：1.20.1 ItemPredicate 序列化 tag 用独立 "tag" 键；1.21.x tag 并入 "items"
    且加 "#" 前缀。census 样本：advancements/recipes/misc/spray_can_empty.json
    criteria.has_redstone_dust（唯一实例）。
    """
    v = o.get("items")
    if isinstance(v, str) and v.startswith("#"):
        del o["items"]
        o["tag"] = v[1:]
        return True
    return False


def _norm_enchant_pred(o: dict) -> bool:
    """match_tool predicate 附魔形重构：{"predicates":{"minecraft:enchantments":[
    {"enchantments":X,...}]}} → {"enchantments":[{"enchantment":X,...}]}。

    出处：1.20.1 MatchToolCondition.predicate 内 "enchantments" 为 EnchantmentPredicate
    列表（键名单数 enchantment）；1.21.x 改为条件 "predicates" 映射（键名复数
    enchantments）。census 样本：loot_tables/blocks/grass.json（6 文件，silk_touch
    草族）。非此形态原样保留 → 字节比对 FAIL（fail-visible）。
    """
    if set(o.keys()) != {"predicates"}:
        return False
    inner = o["predicates"]
    if not (isinstance(inner, dict) and set(inner.keys()) == {"minecraft:enchantments"}):
        return False
    entries = inner["minecraft:enchantments"]
    if not isinstance(entries, list):
        return False
    out: list[dict] = []
    for e in entries:
        if not (isinstance(e, dict) and "enchantments" in e):
            return False  # 未注册形态：原样保留 → 字节比对 FAIL
        ne = {"enchantment": e["enchantments"]}
        for k, v in e.items():
            if k != "enchantments":
                ne[k] = v
        out.append(ne)
    o.clear()
    o["enchantments"] = out
    return True


def _norm_smelt_result_str(o: dict) -> bool:
    """熔炼配方 result 裸串形：1.21 {"count":N,"id":X} 对象 → 1.20.1 裸 id 串。

    出处：1.20.1 SimpleCookingRecipe.Serializer 把 smelting result 序列化为纯
    item id 字符串（Recipes: gt6:mold 双腿实测）；1.21.x 单物品 Result codec
    恒写 {"count":N,"id":X} 对象（与 crafting 的 object 形不同键形，故
    _norm_recipe_result 覆盖不到）。census 样本：recipes/smelt_mold_ceramic_sense.json
    （本卡 p26 熔炼硬化带 32 文件）。count!=1 形态未注册——原样保留 → 字节比对
    FAIL（fail-visible）。必须注册在 _norm_recipe_result 之前：本变换消费
    {"id":X} 原形，后者会把同形改写成 {"item":X}。
    """
    if o.get("type") != "minecraft:smelting":
        return False
    r = o.get("result")
    if not isinstance(r, dict) or "id" not in r:
        return False
    if r.get("count", 1) != 1:
        return False
    o["result"] = r["id"]
    return True


def _norm_recipe_result(o: dict) -> bool:
    """配方 result 形：1.21 {"count":N,"id":X} → 1.20.1 {"count":N,"item":X}，
    且 count==1 时 1.20.1 侧不写（1.21 侧恒写）。

    出处：1.20.1 配方 result 键名 "item"（vanilla 1.20.1 CraftingShapedRecipe
    .Serializer）；1.21.x 改名 "id"。键序双腿同为 count 在前（实测 grass.json 双腿
    {"count":8,...}）；canonical 仅 count!=1 落盘（实测 grass_black_reverse.json /
    spray_can_empty.json）。census 样本：recipes/grass.json + recipes/
    grass_black_reverse.json（13 文件）。
    """
    r = o.get("result")
    if not isinstance(r, dict) or "id" not in r:
        return False
    nr: dict = {}
    if "count" in r and r["count"] != 1:
        nr["count"] = r["count"]
    nr["item"] = r["id"]
    r.clear()
    r.update(nr)
    return True


def _norm_tag_c_to_forge(o: dict) -> bool:
    """配方内 tag 引用命名空间：1.21 "c:…" → 1.20.1 "forge:…"（值层，非路径层）。

    出处：SEGMENT_MAP 的 c→forge 只映射目录段；JSON 字符串值内的命名空间前缀
    是值形差（NeoForge 生态 common tag vs Forge tag 的双腿形态，p24-tags 决策）。
    census 样本：recipes/grass.json ingredients[8].tag "c:dyes/green"→
    "forge:dyes/green"（6 文件草族染料配方）。
    """
    v = o.get("tag")
    if isinstance(v, str) and v.startswith("c:"):
        o["tag"] = "forge:" + v[2:]
        return True
    return False


def _norm_requirements_order(o: dict) -> bool:
    """配方 advancement requirements 组内排序：1.21 侧 has_the_recipe 提前 →
    1.20.1 侧按 criteria 插入序。

    出处：1.20.1 vanilla Advancement requirements 由 Strategy 按 criteria 迭代序
    构建（tmp/vanilla-1.20.1 net/minecraft/advancements/Advancement.java:37
    String[][] requirements）；1.21.x RecipeProvider 先加 recipe criterion 导致组内
    倒序。census 样本：advancements/recipes/decorations/grass.json（requirements[0]
    ["has_the_recipe","has_grass_block"]→["has_grass_block","has_the_recipe"]，13 文件）。
    仅当组内是 criteria 键的排列才施用，否则原样保留 → FAIL。
    """
    criteria = o.get("criteria")
    req = o.get("requirements")
    if not (isinstance(criteria, dict) and isinstance(req, list)):
        return False
    idx = {k: i for i, k in enumerate(criteria)}
    new: list[list] = []
    for group in req:
        if not (isinstance(group, list) and group and all(k in idx for k in group)):
            return False  # 未注册形态：原样保留
        new.append(sorted(group, key=lambda k: idx[k]))
    if new == req:
        return False
    o["requirements"] = new
    return True


def _norm_show_notification(o: dict) -> bool:
    """shaped 配方尾键 show_notification：1.20.1 恒写 → 1.21 侧不写出。

    出处：1.20.1 vanilla CraftingShapedRecipe.Serializer 序列化无条件写
    show_notification（GT 不调 showNotification(false)，值恒默认 true）；1.21.x
    codec 对默认值不落盘。census 实测（2026-09-08）：canonical shaped 1/1 写
    （spray_can_empty.json "show_notification": true，shapeless 0/1 不写）、
    node 全树 0 写。样本：recipes/spray_can_empty.json（唯一实例）。
    """
    if o.get("type") == "minecraft:crafting_shaped" and "show_notification" not in o:
        o["show_notification"] = True
        return True
    return False


def _norm_telemetry(o: dict) -> bool:
    """配方 advancement 尾键 sends_telemetry_event：1.20.1 恒写 → 1.21 侧字段删除。

    出处：1.20.1 vanilla Advancement 序列化无条件 addProperty("sends_telemetry_event",
    ...)（tmp/vanilla-1.20.1 net/minecraft/advancements/Advancement.java:336，读取默认
    false :436）；1.21.x 删除该字段。census：canonical 13/13 恒为 false（GT 侧不改写）。
    样本：advancements/recipes/decorations/grass.json（13 文件）。
    """
    if "criteria" in o and "rewards" in o and "sends_telemetry_event" not in o:
        o["sends_telemetry_event"] = False
        return True
    return False


def _band(rel: PurePosixPath, dir_name: str) -> bool:
    """产物带判定：data/<ns>/<dir_name>/ 前缀（canonical 形相对路径）。"""
    return (len(rel.parts) >= 4 and rel.parts[0] == "data"
            and rel.parts[2] == dir_name)


VALUE_NORMALIZERS: list[tuple[str, str, list[tuple[str, Callable[[dict], bool]]]]] = [
    ("data/*/loot_tables", "loot_tables", [
        ("copy-custom-data→copy-nbt", _norm_copy_custom_data),
        ("items-str→array", _norm_items_wrap),
        ("enchant-predicates→enchantments", _norm_enchant_pred),
    ]),
    ("data/*/advancements", "advancements", [
        ("items-#tag→tag", _norm_items_tag_hash),
        ("tag-c:→forge:", _norm_tag_c_to_forge),
        ("items-str→array", _norm_items_wrap),
        ("requirements→criteria-order", _norm_requirements_order),
        ("sends_telemetry_event(1.20.1-only)", _norm_telemetry),
    ]),
    ("data/*/recipes", "recipes", [
        ("smelt-result-obj→str", _norm_smelt_result_str),
        ("result-id→item(+drop count==1)", _norm_recipe_result),
        ("tag-c:→forge:", _norm_tag_c_to_forge),
        ("show_notification(1.20.1-shaped)", _norm_show_notification),
    ]),
]


def _walk_dicts(obj: object, fn: Callable[[dict], bool]) -> bool:
    """对解析树内每个 dict 施用 fn（fn 自判适用性，返回是否施用）。"""
    changed = False
    if isinstance(obj, dict):
        if fn(obj):
            changed = True
        for v in obj.values():
            changed = _walk_dicts(v, fn) or changed
    elif isinstance(obj, list):
        for v in obj:
            changed = _walk_dicts(v, fn) or changed
    return changed


def _registered_band(rel: PurePosixPath) -> list[tuple[str, Callable[[dict], bool]]] | None:
    """返回 rel（canonical 形路径）命中的已注册带的变换表；未注册带返回 None。"""
    if rel.suffix != ".json":
        return None
    for _, dir_name, regs in VALUE_NORMALIZERS:
        if _band(rel, dir_name):
            return regs
    return None


def try_value_normalize(rel: PurePosixPath, c_bytes: bytes, n_bytes: bytes
                        ) -> tuple[bytes, str] | None:
    """层 2 值形归一：node 侧施用已注册变换 → 重序列化。

    返回 (归一后字节, 施用的变换名串)；未命中带/解析失败/零施用/序列化失败
    一律返回 None（调用方走原样 FAIL 路径）。canonical 侧只读不改写。
    """
    regs = _registered_band(rel)
    if not regs:
        return None
    try:
        json.loads(c_bytes)  # 正典侧必须可解析（形态钉），不通过则归一通道不开放
        n_obj = json.loads(n_bytes)
    except ValueError:
        return None
    applied = [name for name, fn in regs if _walk_dicts(n_obj, fn)]
    if not applied:
        return None
    try:
        out = json.dumps(n_obj, indent=2, ensure_ascii=False).encode()
    except (TypeError, ValueError):
        return None
    return out, "+".join(applied)


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
                    "路径段映射+已注册值形归一后 byte 级比对，ADR-P17-1 门禁步 5 + p25 两层归一）",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="正例: python3 tools/datagen_tree_check.py            # exit 0\n"
               "负例: 对 node 输出任一文件追加一字节后再跑 → exit 1 并定位该文件\n"
               "     （已注册带内未注册形差同样 FAIL——归一绝不静默吞差，详见模块 docstring）",
    )
    parser.add_argument("--canonical", type=Path, default=None,
                        help=f"正典树根（默认 {repo_root / 'mdk/src/generated/resources'}）")
    parser.add_argument("--node-output", type=Path, default=None,
                        help=f"节点输出根（默认 {repo_root / 'mdk/versions/1.21.1-neoforge/build/datagen-output'}）")
    parser.add_argument("--max-list", type=int, default=DEFAULT_MAX_LIST,
                        help=f"差异/归一清单最多打印条数（默认 {DEFAULT_MAX_LIST}，超出只报计数）")
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
    normalized: list[tuple[PurePosixPath, str]] = []
    for rel in sorted(common):
        c_bytes = canon_norm[rel].read_bytes()
        n_bytes = node_norm[rel].read_bytes()
        if c_bytes == n_bytes:
            continue  # 快路径（主判定）：byte 相等直接过，不进归一通道
        first_diff = next((i for i, (a, b) in enumerate(zip(c_bytes, n_bytes)) if a != b),
                          min(len(c_bytes), len(n_bytes)))
        norm = try_value_normalize(rel, c_bytes, n_bytes)
        if norm is not None and norm[0] == c_bytes:
            normalized.append((rel, norm[1]))
            continue
        # 未命中带 / 未注册形差 / 归一后仍不等：原样 FAIL，保留 offset 定位
        diff_content.append((rel, len(c_bytes), len(n_bytes), first_diff))

    print(f"CANONICAL : {canonical_root} ({len(canon)} files)")
    print(f"NODE      : {node_root} ({len(node)} files)")
    print(f"loot band : canonical {loot_band_count(canon, 'loot_tables')} "
          f"(data/*/loot_tables)  node {loot_band_count(node, 'loot_table')} "
          f"(data/*/loot_table)")

    # normalized 清单必须打印（绝不明灭吞差）：条目=文件+施用的变换名
    if normalized:
        for rel, names in normalized[:args.max_list]:
            print(f"NORMALIZED [{names}] {rel}")
        if len(normalized) > args.max_list:
            print(f"NORMALIZED ... and {len(normalized) - args.max_list} more")

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
              f"only-canonical:{len(only_canon)}, only-node:{len(only_node)}, "
              f"normalized:{len(normalized)} accepted)")
        return 1

    print(f"RESULT: OK — {len(common) - len(normalized)} files byte-identical + "
          f"{len(normalized)} value-shape normalized after path mapping + "
          f"registered value normalizers (normalized:{len(normalized)})")
    return 0


if __name__ == "__main__":
    sys.exit(main())
