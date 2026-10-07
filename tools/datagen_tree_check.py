#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""datagen_tree_check.py — datagen 双树一致性断言（ADR-P17-1 门禁步 5，P25 两层归一）。

断言「正典 tracked 树」与「1.21.1-neoforge 节点本地 runData 输出」在按
loot_table(单数, 1.21.x) ↔ loot_tables(复数, 1.20.1) 目录名映射后 byte 级
1:1 相同。背景：ADR-P17-1 之后 1.21.1 runData --output 落节点本地
mdk/versions/1.21.1-neoforge/build/datagen-output（验证产物非入库面），
两树的等价性不再能由 git porcelain 推断，必须显式断言。

规则（两层归一，datagen-tree-check-unify 定案）：
  * 双侧各递归收集文件；目录名 `.cache`（HashCache 账本，非产物）整枝剪除；
    输出根 `version.json`（1.21.x FileCache 版本头，运行时戳记，非产物）排除。
  * 层 1 路径归一化：目录段（不含文件名）经 SEGMENT_MAP 映射——node 侧单数
    `loot_table` → canonical 侧复数 `loot_tables`（24w21a 数据包目录单数化改名，
    仅目录段参与映射，避免误伤同名文件名）。canonical 侧恒等映射（形态钉 1.20.1 形）。
  * 层 1 补充（P27 品牌段带 → P30 双目录终态 revisit）：biome_modifier 产物带坐深一层
    （data/<ns>/<brand>/biome_modifier/）。P27 时代 node 侧单目录（neoforge/）经
    brand_normalize 折到 canonical forge/ 形对账；P26 方案 a 双目录终态落地后（task
    ops-biome-modifier-dual-dir：正典单生产者并载 forge/ 与 neoforge/ 两面，type 键
    各按本腿品牌——GT6DualDirectoryFaces.mirrorBiomeModifiers），两侧同形，品牌折叠
    结构性退役：forge/↔forge/、neoforge/↔neoforge/ 同路径直接字节对账，零归一器参与
    （该带任何字节差照旧 FAIL，值形归一器一并退役）。折叠若保留会把同侧自己的
    neoforge/ 副本折到 forge/ 键上撞键（P27 _fold 守卫按 by design 硬 ERROR 逼出的
    本卡 revisit）；SEGMENT_MAP 也不收 forge/neoforge 段——映射表只辖「两侧形态不同」
    的段，双目录终态下两段双侧同形，恒等条目只是噪声（decisions.P26 文本写于 P27
    作用域带之前，其对账语义由本终态实现，goal 相同：双目录 FAIL=0）。
  * 层 2 值形归一化（VALUE_NORMALIZERS）：字节不等且命中已注册产物带的文件，
    双侧解析 JSON → node 侧施用该带注册的值形变换（方向恒 node→canonical，
    正典树永不改写）→ 按双腿实测一致的 Gson 格式（indent=2 / 无尾换行 /
    非 ASCII 原样，canonical 全部 77573 个 JSON 往返字节级零差实测）重新序列化
    → 再做字节比对。归一成立 ≠ 静默放过：NORMALIZED 计数与文件清单必须打印；
    未命中带、解析失败、变换后仍不等 → 一律原样 FAIL（保留 first-diff offset）。
    变换必须保守：只对 census 实测钉死的形态施用，不适用形态原样保留，
    匹配发生在路径归一之后。品牌段作用域值形带（P27）已在 P30 双目录终态随路径折叠
    一并退役——两侧同品牌同形后无品牌差可归一。
  * 归一化后做三查：仅 canonical 有 / 仅 node 有 / 双侧都有但字节不同。
    双侧文件计数（原始与归一化后）必须相等，否则非零退出。
  * 陈旧守卫（ops，防复发，先于对账）：节点输出 = 非入库验证产物，可能落后于
    正典树（research.lang-legs-delta 实录：陈旧快照比出 66 键 4.4KB en_us 假
    分叉）。开跑先比时戳——节点生成时戳（优先 .cache 账本头
    「// <version>\t<ISO>\t<provider>」，退化产物 mtime）vs git HEAD 正典树最近
    写入（git log -1 -- <canonical>）。快照更早 → STALE FAIL（对账基线不可信，
    宁红勿哑）；--allow-stale 显式逃生（降级 WARN 继续，日志留痕）；git/标记不可得
    → 打印 STALE-CHECK SKIPPED 继续（可见，不静默）。
  * 腿方言审计（ops-rundata-leg-canonical，known_bugs.rundata_leg_dialect_rewrite
    约定正典化）：共享树单数带（data/*/loot_table|recipe|advancement、data/*/tags/
    item|block 等 1.21.1 形单数目录）= neo 腿产物；交叉腿（forge）runData 会用
    1.20.1 方言成批回写这些带（P31 p31-retriever/p31-implosion 两案 + P32
    treecheck/biome/usb-data 三卡五证，1563 文件 loot_table 单数带案）。该带被
    _fold 同键阴影，byte-identical 主判定对其不可见——开跑先打印「方言族清单+
    归属腿」审计行，再对 git 工作树做污染分类：单数带文件被跟踪修改 (M) ∧ 现字节
    == 复数孪生现字节 ∧ 孪生未改 = 腿方言覆盖签名（FAIL，remedy=git restore 非本卡
    生成物）；其余单数带变更 = DRIFT（仅报告——卡内自有变更与 mirror 分叉不可机判，
    人审裁决）。主判定（复数带 vs node 折叠对账）零改动，只加分类报告。
  * 任何未归一差异 → exit 1 并打印差异文件清单；全等 → exit 0 并打印摘要
    （byte 相等数 / normalized 数 / loot 带数）。

用法：
  python3 tools/datagen_tree_check.py [--canonical DIR] [--node-output DIR] \
      [--max-list N] [--allow-stale]

正例（P25 两层归一后，exit 0）：
  $ python3 tools/datagen_tree_check.py
  CANONICAL : mdk/src/generated/resources (77573 files)
  NODE      : mdk/versions/1.21.1-neoforge/build/datagen-output (77573 files)
  leg-dialect: convention 单数带 = neo 腿产物；交叉腿(forge) runData 回写 = 污染 ...
  leg-dialect band: data/*/loot_table (→loot_tables)  canonical 9385 files = NEO-leg band ...
  LEG-REWRITE AUDIT vs HEAD: clean — 0 个单数带变更
  loot band : canonical 4885 (data/*/loot_tables)  node 4885 (data/*/loot_table)
  NORMALIZED [copy-custom-data→copy-nbt] data/gt6/loot_tables/blocks/advanced_crafting_table.json
  ...（normalized 清单逐文件打印，计数进摘要，绝不静默）
  RESULT: OK — 75345 files byte-identical + 228 value-shape normalized after
          path mapping + registered value normalizers (normalized:228)

负例 3（腿方言污染：交叉腿 forge runData 后忘了 git restore——loot_table 单数带
被 1.20.1 方言覆盖，主判定因 _fold 阴影对此全盲，由 LEG-REWRITE 审计捕获并计入
最终 RESULT；主对账照常跑完，差异全景一次给全）：
  $ cp mdk/src/generated/resources/data/gt6/loot_tables/blocks/<表>.json \
        mdk/src/generated/resources/data/gt6/loot_table/blocks/<表>.json
  $ python3 tools/datagen_tree_check.py
  LEG-DIALECT OVERWRITE [loot_table→loot_tables] data/gt6/loot_table/blocks/<表>.json
       现字节 == 复数孪生 ... (forge 形覆盖 neo 带，P31/P32 污染签名)
       remedy: git restore --source=HEAD -- <该文件>
  RESULT: FAIL — N path(s) differ (..., leg-dialect overwrite:N, ...)
  remedy: 腿方言覆盖签名 = 交叉腿 runData 污染未 restore——按约定...

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
import subprocess
import sys
import time
from pathlib import Path, PurePosixPath
from typing import Callable

# 1.21.x 数据包目录单数化（snapshot 24w21a）：node(1.21.1) 侧目录段 → canonical(1.20.1) 侧目录段。
# 映射对两侧对称施用（canon_norm 与 node_norm 都过 normalize），故仅两侧「形态不同」的段
# 需要入表；同段同名（如 assets 的 models/item 双侧同形）入表也只是对称重写、不影响等价性。
# tool-system 起三带新增（首例 tags/recipe datagen 入双树）：
#   recipe → recipes   data/*/recipe(s)（1.21 单数化）
#   item       → items         data/*/tags/item(s)（1.21 单数化；assets models/item 双侧
#                              同名段对称重写，无影响）
#   advancement→ advancements data/*/advancement(s)（1.21 单数化；P24 空罐配方的解锁
#                              advancement 首次把该带带进双树）
#   c          → forge         NeoForge 生态 tag 命名空间 data/c ↔ data/forge（#c:tools
#                              ↔ #forge:tools 是同一逻辑产物的双腿形态）
# tags-provider-skeleton 一带新增：block → blocks  data/*/tags/block(s)（1.21 单数化；
#                              mineable/pickaxe|axe 首次把 block tag 带带进双树；对称施用，
#                              textures/block 等双侧同名段不受影响）
SEGMENT_MAP = {
    "loot_table": "loot_tables",
    "recipe": "recipes",
    "item": "items",
    "advancement": "advancements",
    "c": "forge",
    "block": "blocks",
    # worldgen-deepocean-corals 席修（minecraft:water 扩展带首入双树）：
    #   fluid → fluids   data/*/tags/fluid(s)（1.21 单数化；GT6FluidTags 首例，
    #                    GT6DualDirectoryFaces RENAMES 行随动；对称施用）
    "fluid": "fluids",
}

# biome_modifier 产物带（P27 品牌段折叠 → P30 双目录终态退役）：带坐得比普通带深一层
# ——data/<ns>/<brand>/biome_modifier/（目录段[2]=加载器品牌、段[3]=带名）。forge 腿产
# data/gt6/forge/biome_modifier/（注册键 forge:biome_modifier，ForgeRegistries.java:195；
# GTCEu 1.20.1 生成树 data/gtceu/forge/biome_modifier/ 为量产实证）、21.1 节点产
# data/gt6/neoforge/biome_modifier/（注册键 neoforge:biome_modifier，NeoForgeRegistries
# .java:61-66；目录跟随注册键命名空间，1.21 Registries.elementsDirPath=
# CommonHooks.prefixNamespace, Registries.java:251-253）。P30 起方案 a 双目录并载
# （GT6DualDirectoryFaces.mirrorBiomeModifiers：正典单生产者同轮发射双腿面，type 键各按
# 本腿品牌），两侧同形——P27 的 brand_normalize/BRAND_SEGMENT_MAP 折叠与
# _norm_add_features_brand 值形归一随之退役（折叠保留会同侧撞键，P27 _fold 守卫
# by design 硬 ERROR 逼出的 revisit）；本带不经段映射，直接同路径字节对账。
# issue #32 vanilla-deblob 起唯一例外：remove_features 面的一条保守值形归一
# （remove-features-diamond-medium(1.21.1)，证据见归一器 docstring），add_features
# 面仍零归一原样字节对账。摘要行用 BIOME_BAND_DIR 统计双腿面文件数（仅展示，不参与折叠）。
BIOME_BAND_DIR = "biome_modifier"
BIOME_BRANDS = ("forge", "neoforge")

# ── 层 2：值形归一器（VALUE_NORMALIZERS，datagen-tree-check-unify 新增）────
# 注册纪律（沿 SEGMENT_MAP 注释证据纪律）：每条变换必须带 ①版本差异出处（vanilla
# 反编译/census 实测）②census 实测样本文件名。方向恒 node(1.21)→canonical(1.20.1)，
# 正典树永不改写。变换必须保守：只对实测钉死的形态施用，形态不符原样返回 False，
# 由字节比对兜底 FAIL——绝不静默吞差，绝不放宽为全局语义比较。
#
# 序列化契约：双腿 datagen 同为 Gson setIndent("  ")，实测（2026-09-08 P25 census，
# canonical 全部 77573 个 JSON 往返）与 json.dumps(obj, indent=2, ensure_ascii=False)
# 字节级一致、无尾换行——归一后按此格式重序列化即可与 canonical 逐字节比对。
#
# census 基线（2026-09-08 双腿新鲜树，HEAD 38365b89）：content 差 228 文件，
# 分类=170 items-arr-vs-str + 26 copy_nbt↔copy_custom_data + 12 配方 advancement
# 三合一 + 6 enchant predicate 重构 + 13 配方 result/tag 形 + 1 spray_can_empty
# advancement tag 形 + 1 spray_can_empty 配方（对账 tasks.datagen-tree-check-unify）。


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
    （本卡 P26 熔炼硬化带 32 文件）。count!=1 形态未注册——原样保留 → 字节比对
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


def _reorder_canonical(o: dict) -> None:
    """按键序 in-place 重排为 canonical 树序：type 先、parent 次、余键字母序。

    出处：1.20.1 DataProvider.FIXED_ORDER_FIELDS/KEY_COMPARATOR（tmp/vanilla-1.20.1
    net/minecraft/data/DataProvider.java:22-27，type=0/parent=1/其余 defaultReturnValue(2)
    +字母序；GT6BiomeModifierConditions.CANONICAL_KEY_ORDER 同构扩展）——canonical
    全树经 1.20.1 saveStable 落盘即此序。归一补键（category/parent 等中位键）若
    尾部追加会破坏该序致归一后字节仍不等，故施用了变换的 dict 必须按同序重排。
    """
    for k in ["type", "parent"] + sorted(k for k in o if k not in ("type", "parent")):
        if k in o:
            o[k] = o.pop(k)


def _norm_circuit_program(o: dict) -> bool:
    """gt6:circuit_program 方言（datagen-circuit-declared：P33 电路带 52 文件
    standing red 的清偿对象之一，recipes 面）：result 改键恒写 count（含 ==1，与
    vanilla 的 count==1 不落盘相反）+ category/show_notification 两默认键
    1.20.1 面恒写、1.21.1 codec 默认不落盘。

    出处：1.20.1 forge 面 GT6CraftingRecipes.CircuitProgramRow.serializeRecipeData
    （mdk/src/main/java/gregtech6/datagen/GT6CraftingRecipes.java:1926 category 恒写、
    :1945-1948 result {"item":X,"count":1} 恒写、:1949 show_notification 恒写）；
    1.21.1 codec 面 GT6CircuitProgramRecipe.Serializer.CODEC（mdk/src/main/java/
    gregtech6/items/GT6CircuitProgramRecipe.java:247 category optionalFieldOf 默认
    MISC 省略、:249 STRICT_CODEC result {"count":N,"id":X} 恒写 count、:250
    show_notification optionalFieldOf 默认 true 省略）。census（2026-09-23，
    work/datagen-circuit-declared，HEAD 1fb0327b1 双腿新鲜树）：本带 26 文件
    content 差全带单一形，三处补写后逐字节相等；c:/forge: tag 值差由既有
    _norm_tag_c_to_forge 覆盖（样本：recipes/integrated_circuit_reset.json）。
    必须注册在 _norm_recipe_result 之前：本变换消费 result 的 {"id":X} 形并保留
    count==1（forge 面恒写），后者会把同形改写成 {"item":X} 且丢掉 count==1。
    """
    if o.get("type") != "gt6:circuit_program":
        return False
    changed = False
    r = o.get("result")
    if isinstance(r, dict) and "id" in r:
        nr: dict = {"count": r.get("count", 1), "item": r["id"]}
        r.clear()
        r.update(nr)
        changed = True
    if "category" not in o:
        o["category"] = "misc"
        changed = True
    if "show_notification" not in o:
        o["show_notification"] = True
        changed = True
    if changed:
        _reorder_canonical(o)
    return changed


def _norm_material_tool(o: dict) -> bool:
    """gt6:material_tool 方言（GT6 自定义 serializer 的双腿序列化形差）：
    result 改键恒写 count（含 ==1，与 vanilla 的 count==1 不落盘相反）+ 尾键
    show_notification:true 恒写。

    出处：1.20.1 forge 面 GT6CraftingRecipes.MaterialToolRow.serializeRecipeData
    （mdk/src/main/java/gregtech6/datagen/GT6CraftingRecipes.java:2177
    `tResult.addProperty("count", 1)` + :2179 `aJson.addProperty("show_notification",
    true)`，两键无条件写）；1.21.1 codec 面 GT6MaterialToolRecipe.Serializer.CODEC
    （mdk/src/main/java/gregtech6/items/tools/GT6MaterialToolRecipe.java:252
    `ItemStack.STRICT_CODEC.fieldOf("result")` → 1.21 单物品 {"count":N,"id":X}
    恒写 count、:255 `optionalFieldOf("show_notification", true)` 默认值不落盘）。
    census（2026-09-19，work/ops-treecheck-normalizer，HEAD 2a4521b8d 双腿新鲜
    树）：recipes 带 content 差 5961 文件 = 276 既有变换可归一 + 5685 全部本形
    （样本：recipes/axe/abyssalnite.json，count:1 保留 + show_notification 补写
    后逐字节相等；形差全带单一，零键序/零空容器残差）。必须注册在
    _norm_recipe_result 之前：本变换消费 result 的 {"id":X} 形并保留 count==1，
    后者会把同形改写成 {"item":X} 且丢掉 count==1。
    """
    if o.get("type") != "gt6:material_tool":
        return False
    r = o.get("result")
    if not isinstance(r, dict) or "id" not in r:
        return False
    nr: dict = {"count": r.get("count", 1), "item": r["id"]}
    r.clear()
    r.update(nr)
    if "show_notification" not in o:
        o["show_notification"] = True
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


def _norm_conditions_brand(o: dict) -> bool:
    """行级条件键面（parse-errors-registration-convergence）：neoforge:conditions
    → conditions + 条件类型品牌互换。

    出处：canonical 复数带 = 1.20.1 forge wrapper 形——顶层键 "conditions"（forge
    patches RecipeManager.java.patch:29 CraftingHelper.processConditions(json,
    "conditions") 先于 serializer）+ 类型值 forge:mod_loaded（ModLoadedCondition
    .java:16）；node 侧 = 1.21.1 neo WithConditions 形——ConditionalOps
    .DEFAULT_CONDITIONS_KEY "neoforge:conditions"（ConditionalOps.java:54）+ 类型值
    neoforge:mod_loaded。与镜像面 GT6DualDirectoryFaces.swapConditionTypes 同构互换
    （KNOWN_CONDITION_TYPES 白名单 mod_loaded/not/item_exists；白名单外的类型在
    datagen 侧已硬失败，此处零宽容直接按字节比对兜底）。census 样本：本卡 regen 后
    recipes/ 挂条件行（如 recipes/dust_tiny/from_chemtube/teflon.json）。
    """
    cond = o.get("neoforge:conditions")
    if not isinstance(cond, list):
        return False
    for c in cond:
        if isinstance(c, dict):
            t = c.get("type")
            if isinstance(t, str) and t.startswith("neoforge:"):
                c["type"] = "forge:" + t[len("neoforge:"):]
    del o["neoforge:conditions"]
    o["conditions"] = cond
    _reorder_canonical(o)
    return True


def _norm_tag_c_to_forge(o: dict) -> bool:
    """配方内 tag 引用命名空间：1.21 "c:…" → 1.20.1 "forge:…"（值层，非路径层）。

    出处：SEGMENT_MAP 的 c→forge 只映射目录段；JSON 字符串值内的命名空间前缀
    是值形差（NeoForge 生态 common tag vs Forge tag 的双腿形态，tags 决策）。
    census 样本：recipes/grass.json ingredients[8].tag "c:dyes/green"→
    "forge:dyes/green"（6 文件草族染料配方）。
    """
    v = o.get("tag")
    if isinstance(v, str) and v.startswith("c:"):
        o["tag"] = "forge:" + v[2:]
        return True
    return False


def _norm_requirements_order(o: dict, canon: dict | None = None) -> bool:
    """配方 advancement requirements 组序归一：requirements 是组内 AND 的集语义，
    组序骑各侧 criteria 容器的迭代序（1.20.1 Strategy 序 / 1.21.x HashMap 序——
    t3 实录：soft_hammer 解锁键哈希序与 grass 带 13 文件相反，node 侧
    「按自身 criteria 序排序」归一退化为恒等 → 误 FAIL）。

    canonical 参照可用时（try_value_normalize 捆绑传入）：node 组直接采纳
    canonical 组——仅当两侧 criteria 键集相等且组结构逐组同构（同一键集的排列）
    才施用，表外形态原样保留 → FAIL（fail-visible 不放宽）。
    census 样本：advancements/recipes/decorations/grass.json（13 文件，组内倒序）
    + advancements/recipes/tools/soft_hammer.json（哈希序同侧恒等案，2026-09-16）。
    """
    criteria = o.get("criteria")
    req = o.get("requirements")
    if not (isinstance(criteria, dict) and isinstance(req, list)):
        return False
    if canon is None:
        # 无 canonical 参照的保守回退：按 node 自身 criteria 迭代序排序（原行为）
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
    c_criteria = canon.get("criteria")
    c_req = canon.get("requirements")
    if not (isinstance(c_criteria, dict) and isinstance(c_req, list)):
        return False
    if set(criteria.keys()) != set(c_criteria.keys()):
        return False  # 键集不同 = 非排列：原样保留
    if len(req) != len(c_req):
        return False
    for group, c_group in zip(req, c_req):
        if not (isinstance(group, list) and isinstance(c_group, list)
                and sorted(group) == sorted(c_group)):
            return False  # 组结构不同构：原样保留
    if req == c_req:
        return False  # 已等序：零施用
    o["requirements"] = json.loads(json.dumps(c_req))
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


def _norm_advancement_parent(o: dict, canon: dict | None = None) -> bool:
    """配方 advancement 首键 parent（datagen-circuit-declared：52 文件 standing
    red 的清偿对象之二，advancement 面）：GT6 双腿 fork 形差——1.20.1 面 saveCircuitProgram
    显式 .parent(RecipeBuilder.ROOT_RECIPE_ADVANCEMENT)（GT6CraftingRecipes.java:1907），
    21.1 面同一 save 缝无 .parent 行（GT6CraftingRecipes.java:1972-1977）；vanilla 两侧
    builder 均自带 parent，故此前全带零差。canonical 参照可用时（try_value_normalize
    捆绑传入）才施用：仅当 node 缺 parent、面为配方 advancement（criteria+rewards）
    且 canonical 自带 parent（值照抄不硬编码，表外形态原样保留 → FAIL，fail-visible
    不放宽）。census 样本：advancements/recipes/misc/integrated_circuit_reset.json
    （26 文件电路带，2026-09-23）。
    """
    if canon is None or "parent" in o or "criteria" not in o or "rewards" not in o:
        return False
    cp = canon.get("parent")
    if not isinstance(cp, str):
        return False
    o["parent"] = cp
    _reorder_canonical(o)
    return True


def _band(rel: PurePosixPath, dir_name: str) -> bool:
    """产物带判定：data/<ns>/<dir_name>/ 前缀（canonical 形相对路径）。"""
    return (len(rel.parts) >= 4 and rel.parts[0] == "data"
            and rel.parts[2] == dir_name)


def _norm_uniform_int_value(o: dict) -> bool:
    """IntProvider uniform dispatch 包裹形：1.20.1 {"type":T,"value":{min,max}} →
    1.21.1 内联 {"type":T,"min_inclusive":X,"max_inclusive":Y}（node 侧归一到
    canonical 的包裹形，即把扁平字段收回 "value"）。

    出处：DFU dispatch 序列化双腿形差——1.20.1（DFU 6，forge datagen 实测）IntProvider
    dispatch 产 "value" 嵌套，1.21.1（DFU 8，neoforge datagen 实测）同 dispatch 内联；
    两腿 UniformInt 字段名同为 min_inclusive/max_inclusive（UniformInt.java
    1.20.1:13-14 / 1.21.1:14-15）。census 实测（2026-09-17，small-ore-datagen）：
    worldgen/placed_feature/ore_small_overworld/tin.json（ore band 全量 91 文件，
    canonical 600B vs node 567B，first diff @165="value" 键位）。
    """
    if set(o.keys()) != {"type", "max_inclusive", "min_inclusive"}:
        return False
    if o.get("type") != "minecraft:uniform":
        return False
    mi, ma = o["min_inclusive"], o["max_inclusive"]
    if not (isinstance(mi, int) and isinstance(ma, int)):
        return False
    o.clear()
    o["type"] = "minecraft:uniform"
    o["value"] = {"max_inclusive": ma, "min_inclusive": mi}
    return True


def _band_worldgen_placed(rel: PurePosixPath) -> bool:
    """placed_feature 带判定（坐深一层）：data/<ns>/worldgen/placed_feature/。"""
    return (len(rel.parts) >= 5 and rel.parts[0] == "data"
            and rel.parts[2] == "worldgen" and rel.parts[3] == "placed_feature")


def _band_biome_modifier(rel: PurePosixPath) -> bool:
    """biome_modifier 双目录带判定（坐深一层）：data/<ns>/<brand>/biome_modifier/。"""
    return (len(rel.parts) >= 5 and rel.parts[0] == "data"
            and rel.parts[2] in BIOME_BRANDS and rel.parts[3] == BIOME_BAND_DIR)


def _norm_remove_diamond_medium(o: dict) -> bool:
    """remove_features 行 features 数组剔除 ore_diamond_medium（1.21.1 腿独有键）。

    出处：双腿 vanilla jar data/minecraft/worldgen/placed_feature/ 逐键 diff（2026-09-28，
    issue #32 vanilla-deblob）——1.20.1 client jar 无该键，1.21.1（neoformruntime
    minecraft_1.21.1_client.jar）新增，且 1.21.1 BiomeDefaultFeatures.java:67 实际参与
    生成。GT6WorldgenDatagen.VANILLA_DEBLOB_OVERWORLD 双腿各表（//? if neoforge fork），
    正典树=forge 腿产物（无该键）、node 树=neo 腿产物（有该键）→ 归一方向 node→canonical
    剔除。保守形：仅 type=<brand>:remove_features 的行、仅 features 数组剔该一键；
    形态不符原样返回 → 字节比对兜底 FAIL（fail-visible）。
    """
    if o.get("type") not in ("forge:remove_features", "neoforge:remove_features"):
        return False
    tFeatures = o.get("features")
    if not isinstance(tFeatures, list) or "minecraft:ore_diamond_medium" not in tFeatures:
        return False
    tFeatures.remove("minecraft:ore_diamond_medium")
    return True


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
        ("parent(gt6-fork-1.20.1-face)", _norm_advancement_parent),
    ]),
    ("data/*/recipes", "recipes", [
        ("smelt-result-obj→str", _norm_smelt_result_str),
        ("circuit_program-dialect(1.20.1 count+category+notification)", _norm_circuit_program),
        ("material_tool-dialect(1.20.1 count+notification)", _norm_material_tool),
        ("result-id→item(+drop count==1)", _norm_recipe_result),
        ("tag-c:→forge:", _norm_tag_c_to_forge),
        ("show_notification(1.20.1-shaped)", _norm_show_notification),
        ("conditions-brand(neoforge:conditions→conditions)", _norm_conditions_brand),
    ]),
]

# 坐深一层的特判带（worldgen/placed_feature——_band 只辖 parts[2]，placed_feature 在
# parts[3]，与 biome_modifier 双目录带同属深带族）：small-ore-datagen 注册。
# 归一器自限形态（恰 {type,max_inclusive,min_inclusive} 且 type=minecraft:uniform），
# 零施用原样返回 → 字节比对兜底 FAIL（fail-visible 不放宽）。
# biome_modifier 带注册（issue #32 vanilla-deblob）：P30"两侧同形零值形差"契约对
# remove_features 行失效——双腿键面唯一差异 ore_diamond_medium（证据见归一器 docstring）。
# 变换保守：只动 remove_features 行的 features 数组、只剔该一键；add_features 行零施用。
DEEP_VALUE_NORMALIZERS: list[tuple[str, Callable[[PurePosixPath], bool], list[tuple[str, Callable[[dict], bool]]]]] = [
    ("data/*/worldgen/placed_feature", _band_worldgen_placed, [
        ("uniform-int-value-unwrap(1.21.1)", _norm_uniform_int_value),
    ]),
    ("data/*/biome_modifier", _band_biome_modifier, [
        ("remove-features-diamond-medium(1.21.1)", _norm_remove_diamond_medium),
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
    """返回 rel（canonical 形路径）命中的已注册带的变换表；未注册带返回 None。

    biome_modifier 双目录带（data/<ns>/<brand>/biome_modifier/）P30 起为零值形差带
    （同路径字节对账）；issue #32 vanilla-deblob 起 add_features 面仍保持零差、
    remove_features 面带一条腿差（ore_diamond_medium，1.21.1 独有）→ 该带进注册表，
    仅 remove-features-diamond-medium(1.21.1) 一条保守变换（见其 docstring 证据）。
    """
    if rel.suffix != ".json":
        return None
    for _, dir_name, regs in VALUE_NORMALIZERS:
        if _band(rel, dir_name):
            return regs
    for _, _matcher, regs in DEEP_VALUE_NORMALIZERS:
        if _matcher(rel):
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
        c_obj = json.loads(c_bytes)  # 正典侧必须可解析（形态钉），不通过则归一通道不开放
        n_obj = json.loads(n_bytes)
    except ValueError:
        return None
    canon = c_obj if isinstance(c_obj, dict) else None

    def _bound(fn):
        # canonical 参照型变换（requirements 组序 / advancement parent）由
        # try_value_normalize 捆绑传入参照，canon 不可得时不施用（保守 FAIL）
        if canon is not None and fn in (_norm_requirements_order,
                                        _norm_advancement_parent):
            return lambda o: fn(o, canon)
        return fn

    applied = [name for name, fn in regs if _walk_dicts(n_obj, _bound(fn))]
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


# ── 陈旧守卫（ops-treecheck-stale-guard，防复发）────────────────────────────
# 节点输出 = 非入库验证产物（ADR-P17-1），可能落后于正典树：research.lang-legs-delta
# 实录——main checkout 的节点快照停在 16925b38 之前，与再生后的正典树比出 66 键 4.4KB
# en_us「假分叉」。守卫判据：节点快照生成时戳 < git HEAD 正典树最近写入 → 对账基线不可信。
# 裁定 FAIL（fail-visible 纪律：陈旧基线上的「绿」不可信，宁红勿哑），--allow-stale 显式
# 逃生（降级 WARN 继续跑，日志留痕）；git/标记不可得 → 打印 SKIPPED 继续（可见，不哑）。

def _parse_cache_ts(line: str) -> float | None:
    """解析 1.21.x FileCache 账本头时戳：「// <mc-version>\\t<ISO-local>\\t<provider>」。

    实测样本（main 节点输出 .cache/<sha1> 首行）：
    `// 1.21.1\t2026-09-12T00:57:47.204892185\tLanguage Provider: gt6:mold[en_us]`
    ISO 带纳秒精度，fromisoformat 只吃 3/6 位小数——手工截到微秒；本地时区
    （LocalDateTime 形，与 mtime 同钟）。不可解析返回 None（调用方计数跳过）。
    """
    head = line.split("\t")
    if len(head) < 2 or not head[0].startswith("//"):
        return None
    iso = head[1].strip()
    if "." in iso:
        iso, _, frac = iso.partition(".")
    else:
        frac = ""
    frac = "".join(ch for ch in frac if ch.isdigit())[:6]
    try:
        epoch = time.mktime(time.strptime(iso, "%Y-%m-%dT%H:%M:%S"))
    except ValueError:
        return None
    if frac:
        epoch += float(frac) / 10 ** len(frac)
    return epoch


def node_gen_epoch(node_root: Path,
                   node_files: dict[PurePosixPath, Path]) -> tuple[float, str] | None:
    """节点快照生成时戳（unix 秒）+ 标记说明；不可得返回 None。

    优先 `.cache` 账本头（1.21.x FileCache 每 provider 一文件、首行自带当次生成
    时戳——runData 写入的运行时标记，checkout/rebase 不重写）；无账本可读时退化为
    产物文件最大 mtime（checkout 会重置 mtime，语义弱化但守卫方向不变：宁可误红
    不可漏放，--allow-stale 可逃）。两标记都不可得 = None。
    """
    cache_dir = node_root / CACHE_DIR_NAME
    best: float | None = None
    counted = 0
    if cache_dir.is_dir():
        for f in sorted(cache_dir.iterdir()):
            if not f.is_file():
                continue
            try:
                with f.open("r", encoding="utf-8", errors="replace") as fh:
                    ts = _parse_cache_ts(fh.readline())
            except OSError:
                continue
            if ts is not None:
                counted += 1
                best = ts if best is None else max(best, ts)
    if best is not None:
        return best, f".cache ledger ({counted} provider entries)"
    mtimes = [p.stat().st_mtime for p in node_files.values()]
    if mtimes:
        return max(mtimes), "product-file mtime fallback"
    return None


def canonical_head_epoch(canonical_root: Path) -> tuple[int, str] | None:
    """正典树在 git HEAD 的最近一次写入：(unix 秒, "hash subject")；不可得返回 None。

    `git log -1 -- <canonical 相对路径>`——按路径取最后一次触及提交（worktree 内
    同样成立：rev-parse --show-toplevel 返回所在工作树根）。git 缺失/不在仓库内/
    路径无历史 → None（调用方打印 SKIPPED，可见不哑）。
    """
    try:
        top = subprocess.run(["git", "-C", str(canonical_root),
                              "rev-parse", "--show-toplevel"],
                             capture_output=True, text=True, timeout=30)
        if top.returncode != 0:
            return None
        toplevel = Path(top.stdout.strip())
        rel = canonical_root.resolve().relative_to(toplevel.resolve())
        lg = subprocess.run(
            ["git", "-C", str(toplevel), "log", "-1", "--format=%ct %h %s",
             "--", rel.as_posix()],
            capture_output=True, text=True, timeout=30)
        out = lg.stdout.strip()
        if lg.returncode != 0 or not out:
            return None
        ts, _, desc = out.partition(" ")
        return int(ts), desc
    except (OSError, ValueError, subprocess.SubprocessError):
        return None

# ── 声明偏离表（forge-gated 仅 canonical 面，crucible-physics-smeltery 引入）──────
# 出处：b8a58a0a——crafting provider（RecipeProvider/FinishedRecipe 流）骑 1.20.1-forge
# stonecutter 块：21.1 删除 FinishedRecipe，RecipeOutput 流是卡 B 的 21.1 datagen 面；
# 故下列 canonical 产物在 21.1 节点结构性无输出（非漂移）。逐路径显式白名单、
# 摘要独立计数打印（绝不静默吞差）；表外仅 canonical 条目照旧 FAIL。
# advancement-removal 卡随动（2026-10-03）：unlock advancement 停发，表内原 17 行
# data/gt6/advancements/... 白名单条目随存量删除一并退役（文件已不存在，留之撒谎）。
FORGE_GATED_ONLY_CANONICAL = frozenset({
    "data/gt6/recipes/mold_stone.json",
    "data/gt6/recipes/smeltery_stone.json",
    # issue #45 C2 交卡补录（2026-09-29，与 mold_stone 同构 forge-gated）：clay crucible
    # 链三行（7 黏土 shaped / reverse shapeless / Loader:256 smelt 尾）由 forge 腿独产的
    # GT6CrucibleDatagen.Recipes（crucible-physics-smeltery 的 //? if forge crafting 面）
    # 产出，21.1 节点结构性无输出。runtime 不受影响（generated 树双腿打包共用）。
    "data/gt6/recipes/clay_crucible_raw.json",
    "data/gt6/recipes/clay_crucible_raw_reclaim.json",
    "data/gt6/recipes/smelt_smeltery_ceramic.json",
    # task eu-core-5tier 交卡门禁补录（2026-09-14，非本卡面——c-water-wheel
    # 遗留缺口由本卡新鲜 neo 节点快照首次显形：GT6CraftingRecipes.buildRecipes 的
    # neoforge 分支漏了 waterWheelBuilder()（forge 分支 ：192 有），canonical 树的
    # data/gt6/recipes/water_wheel.json 由 forge 腿独产（其 advancement 伴生面已砍，advancement-removal 卡），与 mold_stone
    # 同构 forge-gated。runtime 不受影响（generated 树双腿打包共用）；neo 分支补行
    # 归后续水车轮微卡。
    "data/gt6/recipes/water_wheel.json",
    # task dig-six 交卡补录（2026-09-16，非漂移）：forge 命名空间的 GLM 索引
    # data/forge/loot_modifiers/global_loot_modifiers.json 由 forge 腿
    # GlobalLootModifierProvider 独产（21.1 同名 provider 写 data/neoforge/...——平台
    # 索引命名空间之差，LootModifierManager folder 常量双腿同为 loot_modifiers）。本卡
    # datagen 已为 21.1 运行时补写 neoforge 孪生索引（GT6ToolLootModifiersDatagen.run
    # 的正典生产者面），forge 索引与 mold_stone 同构 forge-gated。runtime 双腿共用
    # canonical 树各读各的索引，零影响。
    "data/forge/loot_modifiers/global_loot_modifiers.json",
    # task r8-issue45-c3 交卡补录（2026-09-29）：量杯配方面 GT6MeasuringPotDatagen.Recipes
    # 与 GT6CrucibleDatagen.Recipes 同构（//? if forge 整类门控，21.1 节点结构性无输出，
    # RecipeProvider.getName final 的 duplicate-provider 约束 + 卡 B 未来的 RecipeOutput
    # 面）；shaped :134 / reverse :121 / smelt :2096 三行（advancement 伴生面已由 advancement-removal 卡砍除，2026-10-03），
    # singular recipe/ 镜像由 SEGMENT_MAP 归一折入复数面对账，无需声明。runtime 不受影响。
    "data/gt6/recipes/clay_measuring_pot.json",
    "data/gt6/recipes/clay_measuring_pot_reverse.json",
    "data/gt6/recipes/smelt_clay_measuring_pot.json",
    # task small-tank-gas-cylinder 交卡补录（2026-09-30，与 clay_measuring_pot 同构
    # forge-gated）：gas cylinder 四行（Loader :2101-2104 inline "RCR"/"BCh"/"TPd" 网格，
    # GT6GasCylinderDatagen.Recipes 的 //? if forge crafting 面；advancement 面已砍，advancement-removal 卡），
    # singular recipe/ 镜像由 SEGMENT_MAP 归一折入复数面对账，无需声明。runtime 不受影响。
    "data/gt6/recipes/gas_cylinder_steel.json",
    "data/gt6/recipes/gas_cylinder_stainless_steel.json",
    "data/gt6/recipes/gas_cylinder_tungsten.json",
    "data/gt6/recipes/gas_cylinder_tantalum_hafnium_carbide.json",

    # task small-tank-cup 交卡补录（2026-10-01）：瓷杯配方面 GT6CupDatagen.Recipes 与
    # GT6MeasuringPotDatagen.Recipes 同构（//? if forge 整类门控，21.1 节点结构性无输出）；
    # shaped :78 "kPR" / reverse :76 / smelt :2094 三行（advancement 伴生面已由 advancement-removal 卡砍除，2026-10-03），
    # singular recipe/ 镜像由 SEGMENT_MAP 归一折入复数面对账，无需声明。runtime 不受影响。
    "data/gt6/recipes/modeled_porcelain_cup.json",
    "data/gt6/recipes/modeled_porcelain_cup_reverse.json",
    "data/gt6/recipes/smelt_modeled_porcelain_cup.json",
    # task small-tank-jug 交卡补录（2026-10-01）：陶杯配方面 GT6JugDatagen.Recipes 与
    # GT6CupDatagen.Recipes 同构（//? if forge 整类门控，21.1 节点结构性无输出）；
    # shaped :133 "kCR"/"C C"/"CCC" / reverse :120 / smelt :2095 三行（advancement 伴生面已由 advancement-removal 卡砍除，2026-10-03），
    # singular recipe/ 镜像由 SEGMENT_MAP 归一折入复数面对账，无需声明。runtime 不受影响。
    "data/gt6/recipes/clay_jug.json",
    "data/gt6/recipes/clay_jug_reverse.json",
    "data/gt6/recipes/smelt_clay_jug.json",

    # task food-meat-recipes 交卡补录（2026-10-02）：肉排烧制行 GT6MeatDatagen.Recipes 与
    # GT6CupDatagen.Recipes 同构（//? if forge 整类门控，21.1 节点结构性无输出）；
    # smelt :574 DECLARED 行（dogmeat_raw→dogmeat_cooked，骡肉 bug verbatim 转录；
    # advancement 伴生面已由 advancement-removal 卡砍除），singular recipe/ 镜像由 SEGMENT_MAP 归一折入复数面对账，
    # 无需声明。runtime 不受影响。
    "data/gt6/recipes/smelt_food_dogmeat.json",

    # task material-mc-c-crucible-rows 交卡补录（2026-10-07，与 issue #45 C2 clay crucible 链同构
    # forge-gated）：坩埚域 :251-292/:425-466/:471-513 三族逐行 craft walk + 陶瓷生坯链
    # （basin/crossing raw shaped+reclaim+熔炼尾）由 forge 腿独产的 GT6CrucibleDatagen.Recipes
    # （//? if forge crafting 面，RecipeProvider/FinishedRecipe 流 21.1 结构性无输出，卡 B
    # RecipeOutput 面归后续）；GTMaterialItems 可解析门裁剪后存留 88 行 craft + 2 raw 链
    # ×3 行。singular recipe/ 镜像由 SEGMENT_MAP 归一折入复数面对账，无需声明。
    # runtime 不受影响（generated 树双腿打包共用）。
        "data/gt6/recipes/basin_adamantium.json",
    "data/gt6/recipes/basin_bedrock_hsla_alloy.json",
    "data/gt6/recipes/basin_bronze.json",
    "data/gt6/recipes/basin_ceramic_raw.json",
    "data/gt6/recipes/basin_ceramic_raw_reclaim.json",
    "data/gt6/recipes/basin_chromium.json",
    "data/gt6/recipes/basin_dark_iron.json",
    "data/gt6/recipes/basin_fiery_steel.json",
    "data/gt6/recipes/basin_graphite.json",
    "data/gt6/recipes/basin_hsla.json",
    "data/gt6/recipes/basin_invar.json",
    "data/gt6/recipes/basin_iridium.json",
    "data/gt6/recipes/basin_knightmetal.json",
    "data/gt6/recipes/basin_meteoric_iron.json",
    "data/gt6/recipes/basin_meteoric_steel.json",
    "data/gt6/recipes/basin_molybdenum.json",
    "data/gt6/recipes/basin_netherite.json",
    "data/gt6/recipes/basin_niobium.json",
    "data/gt6/recipes/basin_niobium_titanium.json",
    "data/gt6/recipes/basin_octine.json",
    "data/gt6/recipes/basin_osmium.json",
    "data/gt6/recipes/basin_stainless_steel.json",
    "data/gt6/recipes/basin_steel.json",
    "data/gt6/recipes/basin_tantalum_hafnium_carbide.json",
    "data/gt6/recipes/basin_tantalum.json",
    "data/gt6/recipes/basin_thaumium.json",
    "data/gt6/recipes/basin_titanium.json",
    "data/gt6/recipes/basin_tungsten.json",
    "data/gt6/recipes/basin_vanadium.json",
    "data/gt6/recipes/basin_void_metal.json",
    "data/gt6/recipes/crossing_adamantium.json",
    "data/gt6/recipes/crossing_bedrock_hsla_alloy.json",
    "data/gt6/recipes/crossing_bronze.json",
    "data/gt6/recipes/crossing_ceramic_raw.json",
    "data/gt6/recipes/crossing_ceramic_raw_reclaim.json",
    "data/gt6/recipes/crossing_chromium.json",
    "data/gt6/recipes/crossing_dark_iron.json",
    "data/gt6/recipes/crossing_fiery_steel.json",
    "data/gt6/recipes/crossing_graphite.json",
    "data/gt6/recipes/crossing_hsla.json",
    "data/gt6/recipes/crossing_invar.json",
    "data/gt6/recipes/crossing_iridium.json",
    "data/gt6/recipes/crossing_knightmetal.json",
    "data/gt6/recipes/crossing_meteoric_iron.json",
    "data/gt6/recipes/crossing_meteoric_steel.json",
    "data/gt6/recipes/crossing_molybdenum.json",
    "data/gt6/recipes/crossing_netherite.json",
    "data/gt6/recipes/crossing_niobium.json",
    "data/gt6/recipes/crossing_niobium_titanium.json",
    "data/gt6/recipes/crossing_octine.json",
    "data/gt6/recipes/crossing_osmium.json",
    "data/gt6/recipes/crossing_stainless_steel.json",
    "data/gt6/recipes/crossing_steel.json",
    "data/gt6/recipes/crossing_tantalum_hafnium_carbide.json",
    "data/gt6/recipes/crossing_tantalum.json",
    "data/gt6/recipes/crossing_thaumium.json",
    "data/gt6/recipes/crossing_titanium.json",
    "data/gt6/recipes/crossing_tungsten.json",
    "data/gt6/recipes/crossing_vanadium.json",
    "data/gt6/recipes/crossing_void_metal.json",
    "data/gt6/recipes/smelt_basin_ceramic.json",
    "data/gt6/recipes/smelt_crossing_ceramic.json",
    "data/gt6/recipes/smeltery_adamantium.json",
    "data/gt6/recipes/smeltery_bedrock_hsla_alloy.json",
    "data/gt6/recipes/smeltery_bronze.json",
    "data/gt6/recipes/smeltery_chromium.json",
    "data/gt6/recipes/smeltery_dark_iron.json",
    "data/gt6/recipes/smeltery_fiery_steel.json",
    "data/gt6/recipes/smeltery_graphite.json",
    "data/gt6/recipes/smeltery_hsla.json",
    "data/gt6/recipes/smeltery_invar.json",
    "data/gt6/recipes/smeltery_iridium.json",
    "data/gt6/recipes/smeltery_knightmetal.json",
    "data/gt6/recipes/smeltery_meteoric_iron.json",
    "data/gt6/recipes/smeltery_meteoric_steel.json",
    "data/gt6/recipes/smeltery_molybdenum.json",
    "data/gt6/recipes/smeltery_netherite.json",
    "data/gt6/recipes/smeltery_niobium.json",
    "data/gt6/recipes/smeltery_niobium_titanium.json",
    "data/gt6/recipes/smeltery_octine.json",
    "data/gt6/recipes/smeltery_osmium.json",
    "data/gt6/recipes/smeltery_stainless_steel.json",
    "data/gt6/recipes/smeltery_steel.json",
    "data/gt6/recipes/smeltery_tantalum_hafnium_carbide.json",
    "data/gt6/recipes/smeltery_tantalum.json",
    "data/gt6/recipes/smeltery_thaumium.json",
    "data/gt6/recipes/smeltery_titanium.json",
    "data/gt6/recipes/smeltery_tungsten.json",
    "data/gt6/recipes/smeltery_vanadium.json",
    "data/gt6/recipes/smeltery_void_metal.json",
})

# ── 声明偏离带（canonical 前瞻孪生树，vanilla-tag-dual-tree 引入）──────────────
# 出处：3b7ada6c / f9dff2fb——forge 腿 runData 在正典树新产 data/c/tags/items/**（26 个
# 原版交集面 c: 孪生，与 forge: 孪生同成员）。该带必须在对账配对前从 canonical 集剔出：
# SEGMENT_MAP 的 c→forge 对两侧对称施用，canonical 的 data/c 文件若留在配对集会被归一
# 折叠到 data/forge 同名键上、dict 后写静默覆盖（违反 fail-visible 纪律）。21.1 节点的
# data/c 文件不受影响——它们是映射的目标侧（node c→forge→canonical forge 树）照常对账，
# 其与 forge 孪生的内容等价正是映射要验证的东西；孪生树自身的成员正确性由离线测试
# GT6TagsDatagenTest.vanillaIntersectionTwinTreesCarryTheSameMembers 钉死。
# 逐文件显式打印、摘要独立计数（绝不静默吞差）；该带仅 canonical 有属结构性（节点侧
# 自己的 c: 树是全家族+单数目录，经映射对 forge 树对账）。
FORWARD_TWIN_PREFIX = "data/c/"


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
    """目录段映射归一化（文件名段永不参与）。

    P30 终态：品牌折叠（P27 brand_normalize）已随双目录并载退役——biome_modifier
    带 forge/ 与 neoforge/ 两侧同形，各按同品牌路径直接对账，不再折键。
    """
    mapped = [SEGMENT_MAP.get(seg, seg) for seg in rel.parts[:-1]]
    mapped.append(rel.parts[-1])
    return PurePosixPath(*mapped)


def _fold(files: dict[PurePosixPath, Path]) -> dict[PurePosixPath, Path]:
    """归一化折叠（SEGMENT_MAP 层，既有同键阴影维持后写覆盖现行为）。

    P27 的品牌折叠碰撞守卫随 brand_normalize 一并退役：双目录终态下 forge/ 与
    neoforge/ 是各自独立的对账键（GT6DualDirectoryFaces 双腿面同轮发射），不存在
    折叠多解。
    """
    return {normalize(rel): abs_path for rel, abs_path in files.items()}


def loot_band_count(files: dict[PurePosixPath, Path], dir_name: str) -> int:
    """统计 data/<ns>/<dir_name>/ 带内文件数（摘要用）。"""
    return sum(1 for rel in files if len(rel.parts) >= 3
               and rel.parts[0] == "data" and rel.parts[2] == dir_name)


def brand_band_count(files: dict[PurePosixPath, Path], brand: str,
                     dir_name: str) -> int:
    """统计 data/<ns>/<brand>/<dir_name>/ 带内文件数（原始路径，摘要用）。"""
    return sum(1 for rel in files if len(rel.parts) >= 5
               and rel.parts[0] == "data" and rel.parts[2] == brand
               and rel.parts[3] == dir_name)


# ── 腿方言族审计（ops-rundata-leg-canonical，known_bugs.rundata_leg_dialect_rewrite）──
# 约定正典化（挂账 convention 字段的工具层固化）：共享树（canonical tracked 树）
# 的单数带 = neo 腿产物；交叉腿（forge）runData 会成批用 1.20.1 方言回写这些带
# = 污染，处置 = git restore 非本卡生成物。证据：P31 p31-retriever / p31-implosion
# 两案 + P32 treecheck / biome / usb-data 三卡 1563 文件 loot_table 单数带 forge
# 冷 runData 回写（p33 已在 loot face 适配器根治该带来源；本节守护全部单数带并把
# 约定固化为可读输出）。byte-identical 主判定零改动：单数带被 _fold 同键阴影本就
# 不入对账集，本节只加「方言族清单+归属腿」审计行与工作树污染分类——分类报告，
# 不吞差、不放行，签名命中照旧 FAIL（fail-visible）。

# canonical 树中携带腿方言的单数段：data/<ns>/<band>/ 顶层带 + data/<ns>/tags/
# <band>/ 深带。assets 的 models/item 等非 data 路径双侧对称重写、无阴影，不入表；
# c 段（命名空间方言）是 P27 forward-twin 已声明带，不经此审计；tags/block 形
# 尚未入树（tags/ 现辖 item/items/worldgen），入树时追加即可。
_TOP_BAND_SINGULAR = ("loot_table", "recipe", "advancement")
_TAGS_BAND_SINGULAR = ("item", "block")


def _leg_dialect_remap(rel: PurePosixPath) -> tuple[str, str] | None:
    """rel 是 canonical 树的单数方言带文件 → (带路径形段, 复数段)；否则 None。

    顶层带 data/<ns>/<band>/ → ("loot_table", "loot_tables")；tags 深带
    data/<ns>/tags/<band>/ → ("tags/item", "tags/items")（真实树含跨命名空间族：
    gt6+minecraft 的 tags/item 93 文件、minecraft 的 tags/block 10 文件）。
    """
    parts = rel.parts
    if len(parts) < 4 or parts[0] != "data" or parts[1] == "c":
        return None  # 非 data 树 / c 命名空间（P27 forward-twin 已声明带）
    if parts[2] in _TOP_BAND_SINGULAR:
        return parts[2], SEGMENT_MAP[parts[2]]
    if len(parts) >= 5 and parts[2] == "tags" and parts[3] in _TAGS_BAND_SINGULAR:
        return f"tags/{parts[3]}", f"tags/{SEGMENT_MAP[parts[3]]}"
    return None


def leg_dialect_audit(canon: dict[PurePosixPath, Path]) -> None:
    """打印「方言族清单+归属腿」审计行（约定正典的可读形态）。

    同时暴露 _fold 阴影规模：单数带文件若与复数孪生同在 canonical，折叠后同键、
    后写（复数）覆盖——这些文件不进 byte-identical 对账集，其守门员是
    audit_leg_rewrites 的工作树污染检测，如实写进审计行。
    """
    fams: dict[tuple[str, str], list[int]] = {}
    for rel in canon:
        fam = _leg_dialect_remap(rel)
        if fam is None:
            continue
        stats = fams.setdefault(fam, [0, 0])
        stats[0] += 1
        if normalize(rel) in canon:
            stats[1] += 1
    if not fams:
        return
    print("leg-dialect: convention 单数带 = neo 腿产物；交叉腿(forge) runData 回写"
          " = 污染 → git restore 非本卡生成物 (known_bugs.rundata_leg_dialect_rewrite,"
          " P31×2+P32×3 五证)")
    for (seg, plural), (files, shadowed) in sorted(fams.items()):
        note = ("（fold 阴影：byte-identical 主判定不可见，"
                "由 LEG-REWRITE 工作树污染检测守护）" if shadowed else "")
        print(f"leg-dialect band: data/*/{seg} (→{plural})  canonical {files} files"
              f" = NEO-leg band, 复数孪生同在 {shadowed}{note}")


def _git_status_changes(canonical_root: Path) -> dict[str, str] | None:
    """canonical 子树 vs HEAD 的 git 工作树变更（porcelain，canonical 相对路径→状态）。

    git 不可得 / canonical 不在仓库内 → None（调用方打印 SKIPPED，可见不哑）。
    porcelain 路径恒仓库根相对（实测，非 cwd 相对），剥 canonical 前缀后返回。
    """
    try:
        top = subprocess.run(["git", "-C", str(canonical_root),
                              "rev-parse", "--show-toplevel"],
                             capture_output=True, text=True, timeout=30)
        if top.returncode != 0:
            return None
        toplevel = Path(top.stdout.strip())
        rel = canonical_root.resolve().relative_to(toplevel.resolve()).as_posix()
        st = subprocess.run(["git", "-C", str(toplevel), "status", "--porcelain",
                             "--", rel],
                            capture_output=True, text=True, timeout=60)
        if st.returncode != 0:
            return None
    except (OSError, ValueError, subprocess.SubprocessError):
        return None
    prefix = rel + "/"
    changes: dict[str, str] = {}
    for line in st.stdout.splitlines():
        if len(line) < 4:
            continue
        state, path = line[:2].strip(), line[3:]
        if "->" in path:  # rename 条目取新路径
            path = path.split("->", 1)[1].strip()
        if path.startswith('"'):
            continue  # ponytail: 非 ASCII 引号形不展开——方言带文件名恒 ASCII，出现即人审
        if not path.startswith(prefix):
            continue
        changes[path[len(prefix):]] = state
    return changes


def audit_leg_rewrites(canonical_root: Path, canon: dict[PurePosixPath, Path],
                       max_list: int) -> list[str]:
    """工作树污染分类：交叉腿 runData 后「哪些回写属腿方言预期 vs 真漂移」。

    污染签名（P31/P32 五证形态，pre-p33 裸拷贝）：单数带文件被跟踪修改 (M) ∧
    现字节 == 复数孪生现字节 ∧ 孪生未改——forge 形字节覆盖 neo 带。签名命中打印
    LEG-DIALECT OVERWRITE（remedy=git restore）并计入返回清单（调用方 FAIL）；
    其余单数带变更打印 LEG-DIALECT DRIFT（仅报告：卡内自有变更——合法卡改单数带
    时孪生通常同步在改——与 mirror 分叉不可机判，人审裁决）。非方言带变更（本腿
    自有带/非方言面）只计数不入分类。主对账（复数带 vs node 折叠）对此全盲
    （_fold 阴影），本函数是单数带的唯一守门员。git 不可得 → SKIPPED 行（可见）。
    """
    changes = _git_status_changes(canonical_root)
    if changes is None:
        print("LEG-REWRITE AUDIT SKIPPED (git status 不可得——工作树污染检测"
              "不可用，可见退场不哑)")
        return []
    band_changes = {p: s for p, s in changes.items()
                    if _leg_dialect_remap(PurePosixPath(p))}
    others = len(changes) - len(band_changes)
    overwrites: list[tuple[str, str]] = []
    drifts: list[tuple[str, str]] = []
    for path, state in sorted(band_changes.items()):
        rel = PurePosixPath(path)
        seg, plural = _leg_dialect_remap(rel)  # band_changes 已过滤，恒非 None
        label = f"{seg}→{plural}"
        try:
            cur = (canonical_root / rel).read_bytes()
        except OSError:
            drifts.append((f"{path} ({state}, 内容不可读——删除/改名形)", label))
            continue
        twin = normalize(rel)
        twin_entry = canon.get(twin)
        twin_bytes = twin_entry.read_bytes() if twin_entry is not None else None
        twin_state = changes.get(twin.as_posix())
        if ("M" in state and twin_bytes == cur
                and not (twin_state and "M" in twin_state)):
            overwrites.append((path, label))
        else:
            drifts.append((f"{path} ({state})", label))
    tail = (f" + {others} 个非方言带变更（本腿自有带/非方言面，不属腿方言审计域）"
            if others else "")
    if not band_changes:
        print(f"LEG-REWRITE AUDIT vs HEAD: clean — 0 个单数带变更{tail}")
    else:
        print(f"LEG-REWRITE AUDIT vs HEAD: {len(band_changes)} 个单数带变更{tail}"
              " — 分类如下")
    for path, label in overwrites[:max_list]:
        print(f"LEG-DIALECT OVERWRITE [{label}] {path}")
        print(f"     现字节 == 复数孪生 {normalize(PurePosixPath(path)).as_posix()}"
              f"（forge 形覆盖 neo 带，P31/P32 污染签名）")
        print(f"     remedy: git restore --source=HEAD -- {canonical_root / path}")
    if len(overwrites) > max_list:
        print(f"LEG-DIALECT OVERWRITE ... and {len(overwrites) - max_list} more")
    for path, label in drifts[:max_list]:
        print(f"LEG-DIALECT DRIFT [{label}] {path}")
        print("     非污染签名（孪生同改/形不匹配/新增删除）——卡内自有变更 or"
              " mirror 分叉，人审裁决（本腿 runData 产物属本卡则保留，否则 restore）")
    if len(drifts) > max_list:
        print(f"LEG-DIALECT DRIFT ... and {len(drifts) - max_list} more")
    return [p for p, _ in overwrites]


def main() -> int:
    repo_root = Path(__file__).resolve().parent.parent
    parser = argparse.ArgumentParser(
        description="datagen 双树一致性断言（canonical vs 1.21.1 节点本地输出，"
                    "路径段映射+已注册值形归一后 byte 级比对，ADR-P17-1 门禁步 5 + P25 两层归一）",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="正例: python3 tools/datagen_tree_check.py            # exit 0\n"
               "负例: 对 node 输出任一文件追加一字节后再跑 → exit 1 并定位该文件\n"
               "     （已注册带内未注册形差同样 FAIL——归一绝不静默吞差，详见模块 docstring）\n"
               "     节点快照早于正典树 HEAD 最近写入 → STALE FAIL（--allow-stale 显式逃生）\n"
               "     单数带（neo 腿产物）被交叉腿 runData 以 forge 形覆盖 → LEG-DIALECT\n"
               "     OVERWRITE FAIL（约定：单数带=neo 腿；git restore 非本卡生成物）",
    )
    parser.add_argument("--canonical", type=Path, default=None,
                        help=f"正典树根（默认 {repo_root / 'mdk/src/generated/resources'}）")
    parser.add_argument("--node-output", type=Path, default=None,
                        help=f"节点输出根（默认 {repo_root / 'mdk/versions/1.21.1-neoforge/build/datagen-output'}）")
    parser.add_argument("--max-list", type=int, default=DEFAULT_MAX_LIST,
                        help=f"差异/归一清单最多打印条数（默认 {DEFAULT_MAX_LIST}，超出只报计数）")
    parser.add_argument("--allow-stale", action="store_true",
                        help="陈旧守卫逃生阀：节点快照早于正典树 HEAD 最近写入时降级为"
                             " STALE-WARN 继续对账（显式留痕；默认 STALE FAIL）")
    args = parser.parse_args()

    canonical_root: Path = args.canonical or (repo_root / "mdk/src/generated/resources")
    node_root: Path = args.node_output or (
        repo_root / "mdk/versions/1.21.1-neoforge/build/datagen-output")

    canon = collect_files(canonical_root)
    node = collect_files(node_root)

    # ── 腿方言族审计 + 工作树污染分类（ops-rundata-leg-canonical）：先于陈旧守卫
    # 打印（污染检测只看 canonical 工作树 vs HEAD，与 node 快照新鲜度无关）；
    # 签名命中的 FAIL 计入最终 RESULT——主对账照常跑完，差异全景一次给全。
    leg_dialect_audit(canon)
    leg_overwrites = audit_leg_rewrites(canonical_root, canon, args.max_list)

    # ── 陈旧守卫（ops）：对账前先验基线新鲜度，fail-visible ──────────────
    gen = node_gen_epoch(node_root, node)
    head = canonical_head_epoch(canonical_root)
    if gen is None or head is None:
        print(f"STALE-CHECK SKIPPED (node gen marker: "
              f"{'ok' if gen else 'unavailable'}; "
              f"git baseline: {'ok' if head else 'unavailable'})")
    elif gen[0] < head[0]:
        msg = (f"node snapshot [{gen[1]}] generated {gen[0]:.0f} is OLDER than the "
               f"canonical tree's last HEAD write [{head[1]}] at {head[0]} "
               f"(by {head[0] - gen[0]:.0f}s) — the reconciliation baseline is "
               "untrustworthy (research.lang-legs-delta: stale node snapshot "
               "manufactured the phantom 4.4KB en_us lang delta)")
        if args.allow_stale:
            print(f"STALE-WARN (allowed by --allow-stale): {msg}")
        else:
            print(f"RESULT: FAIL — STALE node snapshot. {msg}")
            print("  remedy : regenerate the node output in the same round "
                  "(:mdk:1.21.1-neoforge:runData), then rerun")
            print("  escape : rerun with --allow-stale (explicit, kept in the log)")
            return 1
    else:
        print(f"STALE-CHECK OK (node {gen[1]} at {gen[0]:.0f} >= canonical HEAD "
              f"write [{head[1]}] at {head[0]})")

    # the P27 forward-twin band: paired OUT of the canonical comparison set before
    # normalize — see FORWARD_TWIN_PREFIX (the symmetric c→forge mapping would fold
    # these onto the forge twins' keys, silently overwriting dict entries)
    forward_twin = sorted(rel for rel in canon if str(rel).startswith(FORWARD_TWIN_PREFIX))
    canon = {rel: p for rel, p in canon.items() if not str(rel).startswith(FORWARD_TWIN_PREFIX)}
    canon_norm = _fold(canon)
    node_norm = _fold(node)

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
    print(f"biome band: canonical "
          f"{brand_band_count(canon, 'forge', BIOME_BAND_DIR)}+"
          f"{brand_band_count(canon, 'neoforge', BIOME_BAND_DIR)} "
          f"(data/*/forge+neoforge/{BIOME_BAND_DIR})  node "
          f"{brand_band_count(node, 'forge', BIOME_BAND_DIR)}+"
          f"{brand_band_count(node, 'neoforge', BIOME_BAND_DIR)} "
          f"(data/*/forge+neoforge/{BIOME_BAND_DIR})")

    # normalized 清单必须打印（绝不明灭吞差）：条目=文件+施用的变换名
    if normalized:
        for rel, names in normalized[:args.max_list]:
            print(f"NORMALIZED [{names}] {rel}")
        if len(normalized) > args.max_list:
            print(f"NORMALIZED ... and {len(normalized) - args.max_list} more")

    # the forge-gated declared deviations (the FORGE_GATED_ONLY_CANONICAL table): printed,
    # never silently swallowed, but excluded from the fail count (b8a58a0a citation)
    gated = sorted(r for r in only_canon if str(r) in FORGE_GATED_ONLY_CANONICAL)
    only_canon = [r for r in only_canon if str(r) not in FORGE_GATED_ONLY_CANONICAL]
    for rel in gated:
        print(f"DECLARED [forge-gated, b8a58a0a] {rel}")

    # the P27 forward-twin band: already paired out of `canon` pre-normalize (see
    # FORWARD_TWIN_PREFIX) — printed with its own counter, never silent
    for rel in forward_twin[:args.max_list]:
        print(f"DECLARED [P27 forward-twin] {rel}")
    if len(forward_twin) > args.max_list:
        print(f"DECLARED [P27 forward-twin] ... and {len(forward_twin) - args.max_list} more")

    fail = bool(only_canon or only_node or diff_content or leg_overwrites)
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
        print(f"RESULT: FAIL — "
              f"{len(only_canon) + len(only_node) + len(diff_content) + len(leg_overwrites)} "
              f"path(s) differ (content:{len(diff_content)}, "
              f"only-canonical:{len(only_canon)}, only-node:{len(only_node)}, "
              f"leg-dialect overwrite:{len(leg_overwrites)}, "
              f"forge-gated declared:{len(gated)}, "
              f"P27 forward-twin declared:{len(forward_twin)}, "
              f"normalized:{len(normalized)} accepted)")
        if leg_overwrites:
            print("  remedy: 腿方言覆盖签名 = 交叉腿 runData 污染未 restore——按约定"
                  "（单数带=neo 腿产物）git restore 非本卡生成物后重跑"
                  "（known_bugs.rundata_leg_dialect_rewrite）")
        return 1

    print(f"RESULT: OK — {len(common) - len(normalized)} files byte-identical + "
          f"{len(normalized)} value-shape normalized after path mapping + "
          f"registered value normalizers (normalized:{len(normalized)}, "
          f"forge-gated declared:{len(gated)}, "
          f"P27 forward-twin declared:{len(forward_twin)})")
    return 0


if __name__ == "__main__":
    sys.exit(main())
