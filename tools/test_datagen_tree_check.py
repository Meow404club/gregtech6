#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""test_datagen_tree_check.py — datagen_tree_check 的 stdlib unittest 单测。

零第三方依赖（python3 tools/test_datagen_tree_check.py 直跑）。覆盖：
  * p30 双目录终态：biome_modifier 带 forge/↔forge/、neoforge/↔neoforge/ 同品牌
    直接对账（p27 brand_normalize 折叠退役）——normalize 恒等 / _fold 双品牌同侧
    不撞键 / 值形归一器对该带退役（字节差原样 FAIL）/ main() 端到端 17+17 双腿面
    绿形 + 单面树 34 形 FAIL 回归钉。
  * p27 起的陈旧守卫 STALE 判定与逃生阀。

证据基线：p30 before 实测（work/p30-ops-biome-modifier-dual-dir，base 树）——canonical
仅 data/gt6/forge/biome_modifier/ 17 文件 vs 节点仅 data/gt6/neoforge/biome_modifier/
17 文件 → 34 条目录形 FAIL（only-canonical 17 + only-node 17），即本卡清偿对象；
双目录并载后（GT6DualDirectoryFaces.mirrorBiomeModifiers）同品牌逐字节相等 → 0。
"""

from __future__ import annotations

import contextlib
import importlib.util
import io
import json
import subprocess
import sys
import tempfile
import time
import unittest
from pathlib import Path, PurePosixPath

_TOOLS = Path(__file__).resolve().parent
_spec = importlib.util.spec_from_file_location("datagen_tree_check",
                                               _TOOLS / "datagen_tree_check.py")
mod = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(mod)


def _gson(obj: dict) -> bytes:
    """与双腿 datagen Gson setIndent("  ") 契约同形：indent=2 / 无尾换行 / 非 ASCII 原样。"""
    return json.dumps(obj, indent=2, ensure_ascii=False).encode()


def _biome_json(brand_prefix: str) -> bytes:
    """census 实测形（overworld_stone_andesite.json，双腿仅 type 前缀差）。"""
    return _gson({
        "type": f"{brand_prefix}:add_features",
        "biomes": "#minecraft:is_overworld",
        "features": "gt6:overworld_stone_andesite",
        "step": "underground_ores",
    })


class TestNormalizeDualDir(unittest.TestCase):
    """层 1：双目录终态下品牌段恒等（p27 折叠退役），SEGMENT_MAP 层照旧。"""

    def test_biome_brand_paths_are_identity_both_brands(self):
        for brand in ("forge", "neoforge"):
            rel = PurePosixPath(f"data/gt6/{brand}/biome_modifier/overworld_stone_andesite.json")
            self.assertEqual(mod.normalize(rel), rel, msg=brand)

    def test_global_segment_map_still_folds(self):
        rel = PurePosixPath("data/gt6/loot_table/blocks/a.json")
        self.assertEqual(mod.normalize(rel),
                         PurePosixPath("data/gt6/loot_tables/blocks/a.json"))

    def test_same_brand_pairs_match_across_sides(self):
        c = PurePosixPath("data/gt6/forge/biome_modifier/x.json")
        n = PurePosixPath("data/gt6/forge/biome_modifier/x.json")
        self.assertEqual(mod.normalize(c), mod.normalize(n))
        cn = PurePosixPath("data/gt6/neoforge/biome_modifier/x.json")
        nn = PurePosixPath("data/gt6/neoforge/biome_modifier/x.json")
        self.assertEqual(mod.normalize(cn), mod.normalize(nn))

    def test_cross_brand_pairs_stay_distinct_keys(self):
        # 旧世界单面树（canonical 仅 forge/、node 仅 neoforge/）不再折到同键：
        # 34 条目录形差必须以 only-* 形态显形（fail-visible），而非被折叠吞掉
        c = PurePosixPath("data/gt6/forge/biome_modifier/x.json")
        n = PurePosixPath("data/gt6/neoforge/biome_modifier/x.json")
        self.assertNotEqual(mod.normalize(c), mod.normalize(n))


class TestFoldDualDir(unittest.TestCase):
    """_fold：双品牌同侧共存 = 两个独立对账键（终态形），无碰撞概念。"""

    def test_dual_brand_same_side_folds_to_two_keys(self):
        # p27 时代此形硬 ERROR（by design 逼 revisit）；p30 终态即本形，守卫退役
        files = {
            PurePosixPath("data/gt6/forge/biome_modifier/a.json"): Path("/c/a.json"),
            PurePosixPath("data/gt6/neoforge/biome_modifier/a.json"): Path("/c2/a.json"),
        }
        out = mod._fold(files)
        self.assertEqual(len(out), 2)
        self.assertEqual(out[PurePosixPath("data/gt6/forge/biome_modifier/a.json")],
                         Path("/c/a.json"))
        self.assertEqual(out[PurePosixPath("data/gt6/neoforge/biome_modifier/a.json")],
                         Path("/c2/a.json"))

    def test_legacy_global_fold_shadow_keeps_last_wins(self):
        # SEGMENT_MAP 层既有阴影（singular/plural 瞬态并存）行为不变
        files = {
            PurePosixPath("data/gt6/loot_table/blocks/a.json"): Path("/n/a.json"),
            PurePosixPath("data/gt6/loot_tables/blocks/a.json"): Path("/c/a.json"),
        }
        out = mod._fold(files)
        self.assertEqual(len(out), 1)
        key = PurePosixPath("data/gt6/loot_tables/blocks/a.json")
        self.assertIn(key, out)
        self.assertEqual(out[key], Path("/c/a.json"))  # 后写覆盖（插入序）


class TestBiomeBandUnregistered(unittest.TestCase):
    """值形归一器对 biome_modifier 带退役：该带任何字节差原样 FAIL（fail-visible）。"""

    def test_biome_band_is_no_longer_registered(self):
        for brand in ("forge", "neoforge"):
            rel = PurePosixPath(f"data/gt6/{brand}/biome_modifier/overworld_stone_andesite.json")
            self.assertIsNone(mod._registered_band(rel), msg=brand)

    def test_brand_type_diff_stays_fail_visible(self):
        # p27 时代 type 差由 _norm_add_features_brand 归一；终态两侧同品牌同形，
        # 归一器已删——残余 type 差 = 真实漂移，必须 FAIL 而非吞掉
        rel = PurePosixPath("data/gt6/neoforge/biome_modifier/x.json")
        c = _biome_json("neoforge")
        n = _biome_json("forge")
        self.assertIsNone(mod.try_value_normalize(rel, c, n))

    def test_non_biome_registered_bands_still_work(self):
        rel = PurePosixPath("data/gt6/recipes/x.json")
        c = _gson({"type": "minecraft:smelting", "result": {"count": 1, "id": "gt6:mold"}})
        n = _gson({"type": "minecraft:smelting", "result": {"count": 1, "id": "gt6:mold"}})
        # byte 相等走快路径不进归一通道（main() 语义）；此处只钉带注册面仍在
        self.assertIsNotNone(mod._registered_band(rel))


class TestMainEndToEndDualDir(unittest.TestCase):
    """端到端：双目录终态 17+17 ↔ 17+17 同品牌绿形 / 旧世界单面树 34 形红钉。"""

    @staticmethod
    def _write_biome_tree(root: Path, brand: str) -> None:
        d = root / "data/gt6" / brand / "biome_modifier"
        d.mkdir(parents=True)
        (d / "overworld_stone_andesite.json").write_bytes(_biome_json(brand))

    def _run(self, canon: Path, node: Path) -> tuple[int, str]:
        argv = sys.argv
        buf = io.StringIO()
        try:
            sys.argv = ["datagen_tree_check.py",
                        "--canonical", str(canon), "--node-output", str(node)]
            with contextlib.redirect_stdout(buf):
                rc = mod.main()
        finally:
            sys.argv = argv
        return rc, buf.getvalue()

    def test_dual_dir_same_brand_pairs_go_green(self):
        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            canon = root / "canonical"
            node = root / "node"
            # 终态形：双侧各并载 forge/ 与 neoforge/ 两面（同品牌字节同源）
            self._write_biome_tree(canon, "forge")
            self._write_biome_tree(canon, "neoforge")
            self._write_biome_tree(node, "forge")
            self._write_biome_tree(node, "neoforge")
            (canon / "assets/gt6/lang").mkdir(parents=True)
            (node / "assets/gt6/lang").mkdir(parents=True)
            lang = _gson({"gt6.row.x": "X"})
            (canon / "assets/gt6/lang/en_us.json").write_bytes(lang)
            (node / "assets/gt6/lang/en_us.json").write_bytes(lang)

            rc, out = self._run(canon, node)
            self.assertEqual(rc, 0, msg=out)
            self.assertIn("biome band: canonical 1+1 "
                          "(data/*/forge+neoforge/biome_modifier)  node 1+1 "
                          "(data/*/forge+neoforge/biome_modifier)", out)
            self.assertIn("RESULT: OK", out)
            self.assertNotIn("NORMALIZED [add-features", out)  # 归一器已退役

    def test_single_face_base_shape_still_fails(self):
        # 回归钉（本卡清偿对象）：canonical 仅 forge/ + node 仅 neoforge/（p26-p29
        # 结构债本体）→ 34 形（此处 1+1 缩比）目录形 FAIL，绝不许再被折叠吞绿
        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            canon = root / "canonical"
            node = root / "node"
            self._write_biome_tree(canon, "forge")
            self._write_biome_tree(node, "neoforge")

            rc, out = self._run(canon, node)
            self.assertEqual(rc, 1, msg=out)
            self.assertIn("DIFF [only-canonical] data/gt6/forge/biome_modifier/"
                          "overworld_stone_andesite.json", out)
            self.assertIn("DIFF [only-node] data/gt6/neoforge/biome_modifier/"
                          "overworld_stone_andesite.json", out)
            self.assertIn("only-canonical:1, only-node:1", out)
            self.assertIn("RESULT: FAIL", out)

    def test_dual_dir_content_drift_still_fails(self):
        # 终态无归一兜底：同品牌面任何字节差 → content FAIL + 定位
        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            canon = root / "canonical"
            node = root / "node"
            self._write_biome_tree(canon, "neoforge")
            drift = json.loads(_biome_json("neoforge"))
            drift["features"] = "gt6:somewhere_else"
            d = node / "data/gt6/neoforge/biome_modifier"
            d.mkdir(parents=True)
            (d / "overworld_stone_andesite.json").write_bytes(_gson(drift))

            rc, out = self._run(canon, node)
            self.assertEqual(rc, 1, msg=out)
            self.assertIn("DIFF [content] data/gt6/neoforge/biome_modifier/"
                          "overworld_stone_andesite.json", out)
            self.assertIn("RESULT: FAIL", out)


class TestStaleGuardMarkers(unittest.TestCase):
    """陈旧守卫的时戳标记面（p27-ops）。"""

    def test_parse_cache_ledger_header(self):
        # 实测样本形（main 节点输出 .cache/<sha1> 首行，纳秒精度）
        line = ("// 1.21.1\t2026-09-12T00:57:47.204892185\t"
                "Language Provider: gt6:mold[en_us]")
        expected = (time.mktime(time.strptime("2026-09-12T00:57:47",
                                              "%Y-%m-%dT%H:%M:%S")) + 0.204892)
        self.assertAlmostEqual(mod._parse_cache_ts(line), expected, places=3)

    def test_parse_cache_ledger_garbage(self):
        self.assertIsNone(mod._parse_cache_ts("not a ledger line"))
        self.assertIsNone(mod._parse_cache_ts("// 1.21.1\tnot-a-date\tprovider"))
        self.assertIsNone(mod._parse_cache_ts(""))

    def test_node_gen_epoch_prefers_ledger_over_mtime(self):
        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            (root / ".cache").mkdir()
            old = time.strftime("%Y-%m-%dT%H:%M:%S", time.localtime(10 ** 9)) + ".5"
            (root / ".cache" / "aaa").write_text(f"// 1.21.1\t{old}\tProvider P\n")
            prod = root / "assets" / "x.json"
            prod.parent.mkdir(parents=True)
            prod.write_text("{}")  # mtime = now >> 账本时戳(2001)
            gen, marker = mod.node_gen_epoch(
                root, {PurePosixPath("assets/x.json"): prod})
            self.assertIsNotNone(gen)
            self.assertLess(gen, time.time())  # 选了账本时戳而非 mtime
            self.assertIn(".cache ledger", marker)

    def test_node_gen_epoch_mtime_fallback_without_ledger(self):
        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            prod = root / "x.json"
            prod.write_text("{}")
            gen, marker = mod.node_gen_epoch(root, {PurePosixPath("x.json"): prod})
            self.assertIsNotNone(gen)
            self.assertIn("mtime fallback", marker)
            self.assertAlmostEqual(gen, prod.stat().st_mtime, places=6)

    def test_node_gen_epoch_none_when_empty(self):
        with tempfile.TemporaryDirectory() as td:
            self.assertIsNone(mod.node_gen_epoch(Path(td), {}))


class TestStaleGuardEndToEnd(unittest.TestCase):
    """端到端：STALE FAIL（默认）→ --allow-stale 逃生 → fresh 快照放行。"""

    @staticmethod
    def _git(cwd: Path, *args: str) -> None:
        subprocess.run(["git", "-C", str(cwd), *args],
                       check=True, capture_output=True, text=True, timeout=60)

    def _run_main(self, canon: Path, node: Path, extra: list[str]) -> tuple[int, str]:
        argv = sys.argv
        buf = io.StringIO()
        try:
            sys.argv = ["datagen_tree_check.py", "--canonical", str(canon),
                        "--node-output", str(node), *extra]
            with contextlib.redirect_stdout(buf):
                rc = mod.main()
        finally:
            sys.argv = argv
        return rc, buf.getvalue()

    def test_stale_snapshot_fails_then_escape_allows(self):
        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            # git 仓库放上层、canonical 根是其子目录——collect_files 会把 .git/ 收进
            # 产物集，故不能在 canonical 根内 init
            repo = root / "repo"
            canon = repo / "tree"
            node = root / "node"
            # 终态对账形：同品牌同路径（forge/↔forge/），字节同源
            for base in (canon, node):
                d = base / "data/gt6/forge/biome_modifier"
                d.mkdir(parents=True)
                (d / "a.json").write_bytes(_biome_json("forge"))
            # canonical 侧：真 git 仓库，HEAD 提交时间 = now
            self._git(repo, "init", "-q")
            self._git(repo, "-c", "user.email=t@t", "-c", "user.name=t",
                      "add", "-A")
            self._git(repo, "-c", "user.email=t@t", "-c", "user.name=t",
                      "commit", "-q", "-m", "canonical write")
            # node 侧：账本时戳钉在 2001（<< HEAD）→ 陈旧
            (node / ".cache").mkdir()
            (node / ".cache" / "led").write_text(
                "// 1.21.1\t2001-01-01T00:00:00.0\tProvider P\n")

            rc, out = self._run_main(canon, node, [])
            self.assertEqual(rc, 1, msg=out)
            self.assertIn("RESULT: FAIL — STALE node snapshot", out)
            self.assertIn("--allow-stale", out)

            rc, out = self._run_main(canon, node, ["--allow-stale"])
            self.assertEqual(rc, 0, msg=out)
            self.assertIn("STALE-WARN (allowed by --allow-stale)", out)
            self.assertIn("RESULT: OK", out)

    def test_fresh_snapshot_passes_guard(self):
        # 账本时戳 = 现在（>= HEAD 提交钟面）→ STALE-CHECK OK，无 WARN 无 FAIL
        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            repo = root / "repo"
            canon = repo / "tree"
            node = root / "node"
            for base in (canon, node):
                d = base / "data/gt6/forge/biome_modifier"
                d.mkdir(parents=True)
                (d / "a.json").write_bytes(_biome_json("forge"))
            self._git(repo, "init", "-q")
            self._git(repo, "-c", "user.email=t@t", "-c", "user.name=t",
                      "add", "-A")
            self._git(repo, "-c", "user.email=t@t", "-c", "user.name=t",
                      "commit", "-q", "-m", "canonical write")
            now = time.time()
            stamp = time.strftime("%Y-%m-%dT%H:%M:%S", time.localtime(now))
            (node / ".cache").mkdir()
            (node / ".cache" / "led").write_text(
                f"// 1.21.1\t{stamp}.0\tProvider P\n")

            rc, out = self._run_main(canon, node, [])
            self.assertEqual(rc, 0, msg=out)
            self.assertIn("STALE-CHECK OK", out)


if __name__ == "__main__":
    unittest.main()
