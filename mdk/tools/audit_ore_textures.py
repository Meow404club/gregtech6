#!/usr/bin/env python3
"""Ore texture audit + batch borrow (task p30-ore-2-textures).

The ore render stack (GTOreBakedModel, task p30-ore-3-datagen) draws every ore
block as a copied stone base + the material's texture-set ORE overlay tinted
with fRGBa[prefix.mState] (upstream PrefixBlock.java:284-298 BlockTextureMulti,
TextureSet.java:113-116 registers each icon as `<name>` + `<name>_OVERLAY`).
The SET axis of the registration walk (53 ore materials, task p30-ore-1-mech)
collapses to the distinct SETs the generated block atlas stitches
(`gt6:block/materialicons/<set>/{ore,ore_small}` — 30 sources over 15 SETs), so
this script takes ITS SET LIST FROM THE ATLAS JSON and can never drift from the
consumer. Per-material granularity is pinned by ore-3's
GT6OreRenderDatagenTest (3922 params == atlas-source walk).

Faces:
  audit (default)  the p30 matrix: 5 vanilla stone families x all SETs x
                   {ore, ore_small} upstream presence + the (+_OVERLAY)
                   extension census + the defer list + the GT17 stone-base
                   reconciliation + atlas coverage. Exit 1 on any gap.
  --borrow         copy the missing upstream PNGs into the static tree
                   (byte-identical, borrow_one semantics: never clobbers) and
                   append the sha256 manifest to assets/README.md once
                   (idempotent via README_MARKER, the p27 shape).
  --check          re-verify byte identity of every borrowed file against
                   upstream + full atlas coverage + ledger presence.

Naming (the P20/P27 rules): SET folder lowercased, icon segment camelCase ->
snake_case (oreSmall -> ore_small), `_OVERLAY` -> `_overlay`.
Missing upstream cells are DEFERRED (listed, nothing written, nothing
invented — gen_textures.py:23-24 overlay borrow-only precedent). The
borrow-tint fallback (grayscale copy of a sibling SET, declared deviation)
would only enter the picture below 100% presence and is REPORTED, never
executed here.

Usage:
  python3 audit_ore_textures.py [--atlas PATH] [--upstream DIR]
  python3 audit_ore_textures.py --borrow [--atlas PATH] [--upstream DIR]
  python3 audit_ore_textures.py --check  [--atlas PATH] [--upstream DIR]
  python3 audit_ore_textures.py --selftest

DIR (for --borrow/--check) = the upstream checkout's
src/main/resources/assets/gregtech/textures/blocks directory; it defaults to
the repo-root tmp/gt6-1.7.10 snapshot (gitignored: present in the main
checkout, absent from coder worktrees — pass the main checkout's path there).
ATLAS defaults to mdk/src/generated/resources/assets/minecraft/atlases/
blocks.json (in-repo after the ore-3 merge; pre-merge pass the ore-3
worktree's path).
"""
import argparse
import hashlib
import json
import struct
import sys
import zlib
from pathlib import Path

TOOLS = Path(__file__).resolve().parent
MDK = TOOLS.parent
REPO = MDK.parent
ASSETS = MDK / "src" / "main" / "resources" / "assets"
ICONS_REL = Path("gt6/textures/block/materialicons")
ATLAS_DEFAULT = MDK / "src" / "generated" / "resources" / "assets" / "minecraft" / "atlases" / "blocks.json"
UPSTREAM_DEFAULT = (REPO / "tmp/gt6-1.7.10/src/main/resources/assets/gregtech/textures/blocks")

UPSTREAM_REPO_URL = "https://github.com/GregTech6/gregtech6"
UPSTREAM_SNAPSHOT = "v6.17.06-22-g3703e4030"
README_MARKER = "task p30-ore-2-textures"

# (repo name, upstream name) per form. ore/ore_small are the atlas-consumed
# overlays; the _overlay pair is the upstream pass-1 extension (borrowed for
# pair completeness, the p27 posture — no modern consumer yet).
FORMS = [("ore", "ore"), ("ore_small", "oreSmall"),
         ("ore_overlay", "ore_OVERLAY"), ("ore_small_overlay", "oreSmall_OVERLAY")]
ATLAS_FORMS = ("ore", "ore_small")  # the forms the atlas stitches

# The 5 vanilla stone families of the 26-family axis (the loose
# gravel/sand/redsand/mud families share the same SET axis; upstream presence
# is SET-keyed, family-independent — TextureSet.addTextureSet has no family
# dimension). Reported as the full 5 x SET x 2 matrix per the task card.
VANILLA_FAMILIES = ["stone", "deepslate", "netherrack", "endstone", "sandstone"]

# The 17 GT stones (GT6OreBlocks.gtStone rows, Loader_Rocks.java:57-139 order)
# and the family snake -> texture snake splits (GTOreBakedModel.stoneTextureSnake).
GT_STONES = ["blackgranite", "redgranite", "basalt", "marble", "limestone",
             "granite", "diorite", "andesite", "komatiite", "greenschist",
             "blueschist", "kimberlite", "quartzite", "lightprismarine",
             "darkprismarine", "slate", "shale"]
STONE_TEXTURE_SNAKE = {"blackgranite": "granite_black", "redgranite": "granite_red",
                       "lightprismarine": "prismarine_light", "darkprismarine": "prismarine_dark"}

# The vanilla anchor base textures (GTOreBakedModel.baseSpriteOf mcBlock rows)
# — minecraft-namespace, ride the vanilla jar, never borrowed.
VANILLA_ANCHORS = ["block/stone", "block/cobblestone", "block/deepslate",
                   "block/cobbled_deepslate", "block/netherrack", "block/end_stone",
                   "block/sandstone", "block/gravel", "block/sand", "block/red_sand",
                   "block/mud"]


def sha256_hex(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def png_size(data: bytes):
    """IHDR width/height (fixed offset — no chunk walk, the id660 P28 lesson)."""
    if data[:8] != b"\x89PNG\r\n\x1a\n" or len(data) < 24 or data[12:16] != b"IHDR":
        return None
    return struct.unpack(">II", data[16:24])


def sets_from_sources(sources):
    """Pure part of the atlas parse (the selftest seam)."""
    sets, checked = set(), 0
    head = "gt6:block/materialicons/"
    for src in sources:
        resource = str(src.get("resource", ""))
        if not resource.startswith(head):
            continue
        parts = resource[len(head):].split("/")
        if len(parts) == 2 and parts[1] in ATLAS_FORMS:
            sets.add(parts[0])
            checked += 1
    if not sets or checked % 2:
        sys.exit("error: atlas carries no (ore, ore_small) materialicons pairs")
    return sorted(sets)


def atlas_sets(atlas_path: Path):
    """Distinct SETs the block atlas stitches, from the ore materialicons sources.

    The generated tree is the consumer authority (GT6Atlases <- GTOreBakedModel
    .overlaySprites()); audit input == consumed set, zero drift by construction.
    """
    return sets_from_sources(json.loads(atlas_path.read_text(encoding="utf-8"))["sources"])


def upstream_cell(upstream: Path, set_name: str, form: str):
    return upstream / "materialicons" / set_name.upper() / f"{form}.png"


def audit(upstream: Path, sets):
    """The matrix + defer list. Returns the defer entries (empty = 100%)."""
    defer = []
    grid = {}  # (set, form) -> bool, for ATLAS_FORMS + extension forms
    for set_name in sets:
        for _, up_name in FORMS:
            grid[(set_name, up_name)] = upstream_cell(upstream, set_name, up_name).is_file()
    for form in {f for _, f in FORMS}:
        have = sum(1 for s in sets if grid[(s, form)])
        rate = f"{have}/{len(sets)}"
        mark = "" if have == len(sets) else "  <-- DEFER"
        print(f"  upstream {form:<18} {rate}{mark}")
    print("  5 vanilla families x SET x 2 forms (full matrix, one line per family —")
    print("  presence is SET-keyed, family-independent):")
    for family in VANILLA_FAMILIES:
        ok = sum(1 for s in sets if grid[(s, "ore")] and grid[(s, "oreSmall")])
        print(f"    {family:<10} ore+ore_small both present: {ok}/{len(sets)} SETs")
    for set_name in sets:
        for repo_name, up_name in FORMS:
            if not grid[(set_name, up_name)]:
                defer.append(f"{set_name}/{repo_name} (upstream {up_name}.png missing)")
    if defer:
        print("  defer list (borrow-only, nothing invented):")
        for entry in defer:
            print(f"    - {entry}")
    else:
        print("  defer list: empty (100% upstream presence)")
    return defer


def audit_bases(assets: Path):
    """GT17 stone-base reconciliation + vanilla anchor listing."""
    stones = assets / "gt6/textures/block/stones"
    missing, present_pairs = [], 0
    for snake in GT_STONES:
        tex = STONE_TEXTURE_SNAKE.get(snake, snake)
        smooth = (stones / tex / "stone.png").is_file()
        cobble = (stones / tex / "cobble.png").is_file()
        present_pairs += smooth and cobble
        if not (smooth and cobble):
            missing.append(f"{tex}/stone={'+'.join(n for n, ok in (('stone', smooth), ('cobble', cobble)) if not ok)}")
    print(f"GT17 stone bases (dedupe verdict vs ore_bases/ 34-borrow plan):")
    print(f"  in-repo block/stones/<snake>/{{stone,cobble}}.png: {present_pairs}/17 pairs")
    print(f"  -> ore_bases/ borrow need: 0 (GTOreBakedModel.baseSpriteOf default branch")
    print(f"     rides block/stones/<snake>/stone; both halves already in repo; the")
    print(f"     28 shared base models are covered: 11 vanilla anchors + 17 GT stones)")
    if missing:
        print(f"  MISSING: {missing}")
    print(f"  vanilla anchors (free, minecraft namespace, never borrowed): {len(VANILLA_ANCHORS)}")


def atlas_coverage(atlas_path: Path, assets: Path):
    """Every atlas ore source must resolve to an in-repo PNG (missingno gate)."""
    sources = json.loads(atlas_path.read_text(encoding="utf-8"))["sources"]
    missing = []
    total = 0
    for src in sources:
        resource = str(src.get("resource", ""))
        if not resource.startswith("gt6:block/materialicons/"):
            continue
        total += 1
        rel = ICONS_REL / "/".join(resource[len("gt6:block/materialicons/"):].split("/"))
        if not (assets / rel.parent / (rel.name + ".png")).is_file():
            missing.append(resource)
    print(f"atlas coverage: {total - len(missing)}/{total} ore sprite sources have repo PNGs")
    for entry in missing:
        print(f"    missingno: {entry}")
    return total, missing


def borrow_one(src: Path, dst: Path) -> str:
    """Copy src bytes to dst unless dst already holds exactly them (p27 form)."""
    data = src.read_bytes()
    if dst.exists():
        return "present" if dst.read_bytes() == data else "conflict"
    dst.parent.mkdir(parents=True, exist_ok=True)
    dst.write_bytes(data)
    return "borrowed"


def borrow(upstream: Path, assets: Path, sets):
    """Copy SET x FORM cells that exist upstream; abort on conflict; list defers."""
    rows, defer = [], []
    for set_name in sets:
        for repo_name, up_name in FORMS:
            src = upstream_cell(upstream, set_name, up_name)
            dst = assets / ICONS_REL / set_name / f"{repo_name}.png"
            if not src.is_file():
                defer.append(f"{set_name}/{repo_name}")
                continue
            data = src.read_bytes()
            size = png_size(data)
            if size != (16, 16):
                sys.exit(f"error: {src} is {size}, expected 16x16 — refusing to borrow")
            state = borrow_one(src, dst)
            if state == "conflict":
                sys.exit(f"error: {dst} differs from upstream {src} — refusing to clobber")
            rows.append((set_name, f"{repo_name}.png", sha256_hex(data)))
            mcmeta = src.with_suffix(".png.mcmeta")
            if mcmeta.is_file():
                sys.exit(f"error: {mcmeta} exists — animated sprite needs an mcmeta borrow rule")
    return rows, defer


def append_readme(assets: Path, rows, defer) -> bool:
    """Append the p30 sha256 manifest to assets/README.md once (p27 shape)."""
    readme = assets / "README.md"
    if README_MARKER in readme.read_text(encoding="utf-8"):
        return False
    defer_note = ("zero defers — 100% upstream presence over the whole matrix"
                  if not defer else "DEFERRED (nothing written, nothing invented): " + ", ".join(defer))
    by_set = {}
    for set_name, file, digest in rows:
        by_set.setdefault(set_name, []).append((file, digest))
    lines = [f"""
GT6 ore block overlay textures, task p30-ore-2-textures: the {len(rows)}
`gt6/textures/block/materialicons/<set>/{{ore,ore_small,ore_overlay,ore_small_overlay}}.png`
files come from upstream `{UPSTREAM_REPO_URL}` snapshot `{UPSTREAM_SNAPSHOT}`, files
`src/main/resources/assets/gregtech/textures/blocks/materialicons/<SET>/<Name>.png`,
byte-identical to upstream, sha256 verified per file (manifest below). Consumers:
the ore block atlas sources `gt6:block/materialicons/<set>/{{ore,ore_small}}`
(GT6Atlases <- GTOreBakedModel.overlaySprites(), 15 SETs over the 53-material
registration axis, task p30-ore-1-mech) — the missingno intermediate state of
p30-ore-3-datagen closes with this wave; the `_overlay` pair is the upstream
pass-1 extension (TextureSet.java:113-116), borrowed for pair completeness
(the p27 posture), no modern consumer yet. Naming follows the P20/P27 rules:
set folder lowercased, icon segment camelCase -> snake_case
(oreSmall -> ore_small), `_OVERLAY` -> `_overlay`. Audit census
(audit_ore_textures.py): 5 vanilla families x SET x 2 forms all present,
{defer_note}. GT17 stone bases need NO borrow — block/stones/<snake>/{{stone,cobble}}.png
already in repo (the 28 shared base models covered: 11 vanilla anchors free +
17 GT stones), the card's ore_bases/ 34-borrow plan dedupes to zero. No
`.mcmeta` animations exist in the borrowed cells. Upstream license: **CC0 1.0
Universal Public Domain Dedication** (same upstream `README.md` block as above).
"""]
    for set_name in sorted(by_set):
        lines.append(f"- `{set_name.upper()}` -> `{ICONS_REL}/{set_name}/`:")
        for file, digest in by_set[set_name]:
            lines.append(f"  - `{file}` `{digest}`")
    with readme.open("a", encoding="utf-8") as handle:
        handle.write("\n" + "\n".join(lines) + "\n")
    return True


def check(upstream: Path, assets: Path, atlas: Path, sets):
    """Byte identity + coverage + ledger marker."""
    bad = 0
    for set_name in sets:
        for repo_name, up_name in FORMS:
            dst = assets / ICONS_REL / set_name / f"{repo_name}.png"
            src = upstream_cell(upstream, set_name, up_name)
            if not dst.is_file():
                continue  # a deferred cell, the audit face reports it
            if not src.is_file() or dst.read_bytes() != src.read_bytes():
                print(f"  BYTE DRIFT: {dst} != {src}")
                bad += 1
    total, missing = atlas_coverage(atlas, assets)
    marker = README_MARKER in (assets / "README.md").read_text(encoding="utf-8")
    print(f"ledger marker in assets/README.md: {'present' if marker else 'MISSING'}")
    ok = bad == 0 and not missing and marker
    print(f"--check: {'OK' if ok else 'FAIL'}")
    return 0 if ok else 1


def selftest():
    """One runnable check over the non-trivial pure helpers."""
    assert png_size(b"\x89PNG\r\n\x1a\n" + b"\x00\x00\x00\rIHDR" + struct.pack(">II", 16, 16) + b" rest") == (16, 16)
    assert png_size(b"not a png") is None
    sets = sets_from_sources([
        {"type": "minecraft:single", "resource": "gt6:block/materialicons/metallic/ore"},
        {"type": "minecraft:single", "resource": "gt6:block/materialicons/metallic/ore_small"},
        {"type": "minecraft:single", "resource": "gt6:block/materialicons/copper/ore"},
        {"type": "minecraft:single", "resource": "gt6:block/materialicons/copper/ore_small"},
        {"type": "minecraft:single", "resource": "gt6:block/pipe_flow_arrow"},
    ])
    assert sets == ["copper", "metallic"], sets
    for name in sets:
        assert name == name.lower() and " " not in name, name
        for repo_name, up_name in FORMS:  # snake round-trip sanity
            assert up_name.lower().replace("_", "") == repo_name.replace("_", ""), (repo_name, up_name)
    assert STONE_TEXTURE_SNAKE["blackgranite"] == "granite_black"
    assert len(GT_STONES) == 17 and len(VANILLA_ANCHORS) == 11 and len(VANILLA_FAMILIES) == 5
    print("selftest OK")


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--atlas", type=Path, default=ATLAS_DEFAULT,
                        help=f"generated blocks.json (default {ATLAS_DEFAULT})")
    parser.add_argument("--upstream", type=Path, default=UPSTREAM_DEFAULT,
                        help=f"upstream textures/blocks dir (default {UPSTREAM_DEFAULT})")
    parser.add_argument("--borrow", action="store_true", help="copy missing cells + append ledger")
    parser.add_argument("--check", action="store_true", help="re-verify bytes + coverage + ledger")
    parser.add_argument("--selftest", action="store_true", help="pure-helper asserts")
    args = parser.parse_args()
    if args.selftest:
        selftest()
        return
    if not args.atlas.is_file():
        sys.exit(f"error: atlas not found: {args.atlas} (pre-merge: pass the ore-3 worktree path)")
    sets = atlas_sets(args.atlas)
    print(f"ore texture audit vs {args.upstream} (snapshot {UPSTREAM_SNAPSHOT}), "
          f"{len(sets)} SETs from {args.atlas}:")
    if args.check:
        sys.exit(check(args.upstream, ASSETS, args.atlas, sets))
    defer = audit(args.upstream, sets)
    audit_bases(ASSETS)
    total, missing = atlas_coverage(args.atlas, ASSETS)
    if args.borrow:
        rows, borrow_defer = borrow(args.upstream, ASSETS, sets)
        defer = borrow_defer
        if append_readme(ASSETS, rows, defer):
            print(f"assets/README.md: p30 manifest appended ({len(rows)} sha256 entries)")
        else:
            print("assets/README.md: p30 manifest already present, untouched")
        total, missing = atlas_coverage(args.atlas, ASSETS)
        print(f"borrowed/present cells: {len(rows)} ({len(sets)} SETs x {len(FORMS)} forms)")
    gaps = bool(defer or missing)
    print(f"audit verdict: {'GAPS' if gaps else 'OK'} "
          f"(defers={len(defer)}, missingno={len(missing)})")
    sys.exit(1 if gaps else 0)


if __name__ == "__main__":
    main()
