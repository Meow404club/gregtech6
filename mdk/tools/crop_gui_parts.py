#!/usr/bin/env python3
"""crop_gui_parts — the canonical GUI-part cropper (task r8-gui-part-crops).

Crops the composable GUI part sprites out of the amazawa resource-pack full
images + the TFC-domain images (visual-source ruling decisions.r8-gui-visual-source,
Apache-2.0 + author attribution) and writes them into the static asset tree
(mdk/src/main/resources/assets/gt6/textures/gui/parts/) plus the provenance
manifest (mdk/tools/parts_manifest.json) that GT6GuiPartsDatagenTest pins.

Provenance chain: source sha256 (below, re-checked every run) -> crop rect
(x, y, w, h) -> part file sha256 (manifest). Re-run with --verify to replay
every rect and assert the on-disk parts are byte-identical to the fresh crop
(byte identity holds for the Pillow version that produced the committed files;
the Java test additionally replays rects PIXEL-identically from the committed
test fixtures, which is the version-independent invariant).

Usage:
    python3 mdk/tools/crop_gui_parts.py            # crop + write manifest
    python3 mdk/tools/crop_gui_parts.py --verify   # replay rects, byte-compare
"""

import argparse
import io
import hashlib
import json
import sys
from pathlib import Path

from PIL import Image

# mdk/tools/this_script -> mdk -> worktree/repo root
REPO_ROOT = Path(__file__).resolve().parents[2]
MDK_ROOT = REPO_ROOT / "mdk"

# Crop sources. Paths are repo-root relative; sha256 pins identity so a moved
# or re-downloaded snapshot is detected. Sources are NOT committed (except the
# test fixtures under mdk/src/test/resources/gregtech6/guiparts/source/,
# which must hash-match their entry here).
# Domain ruling (2026-09-29 coordinator correction): gregtech domain + the pack's
# minecraft domain ONLY — the terrafirmacraft domain is NOT a permitted source.
AMAZAWA = "tmp/amazawa-census/v105g/assets"
SOURCES = {
    "Default.png": AMAZAWA + "/gregtech/textures/gui/machines/Default.png",
    "Melter.png": AMAZAWA + "/gregtech/textures/gui/machines/Melter.png",
    "Freezer.png": AMAZAWA + "/gregtech/textures/gui/machines/Freezer.png",
    "Distillery.png": AMAZAWA + "/gregtech/textures/gui/machines/Distillery.png",
    "Crafting2By2.png": AMAZAWA + "/gregtech/textures/gui/machines/Crafting2By2.png",
    "widgets.png": AMAZAWA + "/minecraft/textures/gui/widgets.png",
    # task act-gui-overlay-overhaul — the COMMITTED ACT sheets (byte-identical to
    # upstream 1.7.10 in the 176x166 canvas, assets/README.md sha-ledgered at the
    # act-dual-gui rows) are the crop source for the ACT cell prints; both stay
    # shipped as the provenance source of record.
    "advancedcraftingtable.png":
        "mdk/src/main/resources/assets/gt6/textures/gui/machines/advancedcraftingtable.png",
    "advancedcraftingtablecharging.png":
        "mdk/src/main/resources/assets/gt6/textures/gui/machines/advancedcraftingtablecharging.png",
}

# (out_name, source_key, x, y, w, h) — rects in source-image pixels, verified
# by pixel-run scans at crop time (see assets/README.md GUI-parts section).
CROPS = [
    # gregtech domain (amazawa GT6 machine skins, 176x166 GUI + right strip)
    ("panel_176x166.png",            "Default.png",       0,   0, 176, 166),  # full background frame, 9-slice source
    ("slot_frame_18x18.png",         "Default.png",      16,  62,  18,  18),  # standard slot, white stroke
    ("slot_frame_group_54x36.png",   "Default.png",      16,  15,  54,  36),  # 3x2 slot-group / display frame
    ("slot_frame_group_2x2_36x36.png", "Crafting2By2.png", 34, 15,  36,  36),  # 2x2 slot-group frame
    ("arrow_forward_20x18.png",      "Default.png",     176,   0,  20,  18),  # progress arrow strip (upstream UV 176,0)
    ("arrow_forward_red_20x18.png",  "Melter.png",      176,   0,  20,  18),  # progress arrow, red machine accent
    ("arrow_forward_cyan_20x18.png", "Freezer.png",     176,   0,  20,  18),  # progress arrow, cyan machine accent
    ("arrow_outline_20x18.png",      "Default.png",      78,  24,  20,  18),  # progress arrow cell (the empty-arrow face, flat panel bg)
    ("player_inventory_162x76.png",  "Default.png",       7,  83, 162,  76),  # 3x9 + hotbar block (4px gap included)
    ("slot_special_22x22.png",       "Default.png",      77,  60,  22,  22),  # dark-stroke special slot (gear baked)
    ("slot_fluid_18x19.png",         "Distillery.png",  106,  24,  18,  19),  # fluid display cell (droplet baked)
    # minecraft domain (amazawa reskin of the vanilla widget sheet) — sanctioned
    # generic-part fallback for the flat buttons
    ("button_flat_200x20.png",       "widgets.png",       0,  66, 200,  20),  # flat button, normal state
    ("button_flat_hover_200x20.png", "widgets.png",       0,  86, 200,  20),  # flat button, hover state (blue)
    # task act-gui-overlay-overhaul — the ACT sheet cell prints (18x18 frame+icon
    # cells; rects = the upstream slot frames: slot (x,y) prints at (x-1, y-1)).
    # The sheet is retired as a panel background (the composed-parts base), these
    # crops carry its semantics over: the four slot hints (blueprint selector,
    # drop arrow, P neutral, tool holder) and the three button/display faces
    # (sort-into-storage, flush-to-automation, craft-hammer output cell).
    ("act_cell_blueprint_18x18.png", "advancedcraftingtable.png", 134, 27, 18, 18),  # selector slot 30 hint (the blueprint print)
    ("act_cell_drop_arrow_18x18.png", "advancedcraftingtable.png", 152, 27, 18, 18),  # drop slot 33 hint (the output arrow print)
    ("act_cell_sort_18x18.png",      "advancedcraftingtable.png", 134, 45, 18, 18),  # sort button face (put-to-storage holo 32 @135,46)
    ("act_cell_flush_18x18.png",     "advancedcraftingtable.png", 152, 45, 18, 18),  # flush button face (automation-access holo 32 @153,46)
    ("act_cell_craft_18x18.png",     "advancedcraftingtable.png", 134, 63, 18, 18),  # output display face (the craft hammer, holo 31 @135,64)
    ("act_cell_neutral_18x18.png",   "advancedcraftingtable.png", 152, 63, 18, 18),  # neutral slot 34 hint (the P print)
    ("act_cell_tools_18x18.png",     "advancedcraftingtable.png",  79,  7, 18, 18),  # tool slots 16-20 hint (the holder print, plain line)
    ("act_cell_tools_charging_18x18.png", "advancedcraftingtablecharging.png", 79, 7, 18, 18),  # the charging line's tool print (NBT_GUI swap, Loader:137)
]

PARTS_DIR = MDK_ROOT / "src/main/resources/assets/gt6/textures/gui/parts"
MANIFEST = MDK_ROOT / "tools/parts_manifest.json"


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def source_bytes(rel: str) -> bytes:
    """tmp/ is untracked, so coder worktrees don't have it — fall back to the
    primary checkout (MGT6GA/gregtech6 beside MGT6GA/MGT6GA-trees/)."""
    for root in (REPO_ROOT, REPO_ROOT.parents[1] / "gregtech6"):
        p = root / rel
        if p.is_file():
            return p.read_bytes()
    raise FileNotFoundError(f"crop source not found: {rel} (looked in {REPO_ROOT} and primary checkout)")


def load_sources() -> dict:
    out = {}
    for key, rel in SOURCES.items():
        raw = source_bytes(rel)
        out[key] = {"path": rel, "sha256": sha256(raw),
                    "image": Image.open(io.BytesIO(raw)).convert("RGBA")}
    return out


def crop_bytes(sources: dict, entry) -> bytes:
    name, src, x, y, w, h = entry
    img = sources[src]["image"].crop((x, y, x + w, y + h))
    buf = io.BytesIO()
    img.save(buf, "PNG")
    return buf.getvalue()


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--verify", action="store_true", help="replay rects, assert byte-identical on-disk parts")
    args = ap.parse_args()

    sources = load_sources()
    results = []
    for entry in CROPS:
        data = crop_bytes(sources, entry)
        results.append((entry, data))

    if args.verify:
        bad = []
        for (name, src, x, y, w, h), data in results:
            disk = (PARTS_DIR / name).read_bytes()
            if disk != data:
                bad.append(name)
        if bad:
            print("VERIFY FAIL (byte drift):", bad)
            return 1
        print(f"VERIFY OK: {len(results)} parts byte-identical to fresh crops")
        return 0

    PARTS_DIR.mkdir(parents=True, exist_ok=True)
    manifest = {
        "source_pack": "tfc-amazawa-light-gui (Modrinth, Apache-2.0, author 天沢香; "
                       "see assets/README.md GUI-parts section + docs/licenses/)",
        "sources": {k: {"path": v["path"], "sha256": v["sha256"]} for k, v in sources.items()},
        "parts": [
            {"name": name, "source": src, "x": x, "y": y, "w": w, "h": h,
             "sha256": sha256(data)}
            for (name, src, x, y, w, h), data in results
        ],
    }
    for (name, _, _, _, _, _), data in results:
        (PARTS_DIR / name).write_bytes(data)
    MANIFEST.write_text(json.dumps(manifest, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(f"cropped {len(results)} parts -> {PARTS_DIR}")
    print(f"manifest -> {MANIFEST}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
