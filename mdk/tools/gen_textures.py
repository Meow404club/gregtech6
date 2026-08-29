#!/usr/bin/env python3
"""Generate placeholder 16x16 grayscale PNG textures for GT6 material prefix items.

One texture per (iconset, prefix) combination actually referenced by the generated
item models (mdk/src/generated/resources/assets/gt6/models/item/*.json, layer0 =
"gt6:item/material_sets/<iconset>/<prefix>") — 2469 registered items collapse onto
this shared set; the material colour is applied at runtime by the ItemColor tint
(MaterialPrefixItem.tintColor), mirroring upstream GT6's grayscale icon + colour
modulation (PrefixItem.java:136-138 renders from the material's texture set).

Iconset semantics: the texture set is a property of the MATERIAL
(OreDictMaterial.mTextureSetsItems, filled by MT.setTextures, MT.java:210-215);
names are lower-snaked MT SET_* constants (METALLIC -> metallic). The table below
is the census over the registered item set (37 distinct sets, 103 combos; no
"none" fallback hit). Pure standard library (hand-rolled PNG: zlib+struct), idempotent
(skips existing files unless --force), deterministic output bytes.

Usage:
  python3 gen_textures.py                 # generate missing PNGs from the table
  python3 gen_textures.py --force         # regenerate all table PNGs
  python3 gen_textures.py --scan DIR      # derive combos from generated models, fill gaps
  python3 gen_textures.py --verify DIR    # assert model coverage, write nothing
DIR = mdk/src/generated/resources.
"""
import argparse
import json
import sys
import zlib
from pathlib import Path

SIZE = 16  # vanilla item texture edge, pixels
MODELS_REL = Path("assets/gt6/models/item")
TEXTURES_REL = Path("assets/gt6/textures/item/material_sets")

# (iconset, prefix) referenced by the generated models; census 2026-08-29.
COMBOS = {
    ("brick", "dust"),
    ("brick", "ingot"),
    ("brick", "plate"),
    ("copper", "dust"),
    ("copper", "ingot"),
    ("copper", "plate"),
    ("cube", "dust"),
    ("cube", "ingot"),
    ("cube_shiny", "dust"),
    ("cube_shiny", "gem"),
    ("diamond", "dust"),
    ("diamond", "gem"),
    ("diamond", "ingot"),
    ("diamond", "plate"),
    ("dull", "dust"),
    ("dull", "gem"),
    ("dull", "ingot"),
    ("dull", "plate"),
    ("emerald", "dust"),
    ("emerald", "gem"),
    ("fiery", "dust"),
    ("fiery", "ingot"),
    ("fiery", "plate"),
    ("fine", "dust"),
    ("fine", "gem"),
    ("fine", "ingot"),
    ("fine", "plate"),
    ("flint", "dust"),
    ("flint", "gem"),
    ("food", "dust"),
    ("food", "ingot"),
    ("food", "plate"),
    ("gem_horizontal", "dust"),
    ("gem_horizontal", "gem"),
    ("gem_vertical", "dust"),
    ("gem_vertical", "gem"),
    ("glass", "dust"),
    ("glass", "gem"),
    ("hex", "dust"),
    ("hex", "gem"),
    ("lapis", "dust"),
    ("lapis", "gem"),
    ("lapis", "ingot"),
    ("lapis", "plate"),
    ("leaf", "dust"),
    ("leaf", "ingot"),
    ("leaf", "plate"),
    ("lignite", "dust"),
    ("lignite", "gem"),
    ("lignite", "ingot"),
    ("lignite", "plate"),
    ("magnetic", "ingot"),
    ("magnetic", "plate"),
    ("metallic", "dust"),
    ("metallic", "gem"),
    ("metallic", "ingot"),
    ("metallic", "plate"),
    ("netherstar", "dust"),
    ("netherstar", "gem"),
    ("opal", "dust"),
    ("opal", "gem"),
    ("paper", "dust"),
    ("powder", "dust"),
    ("powder", "plate"),
    ("prismarine", "dust"),
    ("prismarine", "gem"),
    ("quartz", "dust"),
    ("quartz", "gem"),
    ("quartz", "ingot"),
    ("quartz", "plate"),
    ("rad", "dust"),
    ("rad", "ingot"),
    ("rad", "plate"),
    ("redstone", "dust"),
    ("redstone", "gem"),
    ("redstone", "ingot"),
    ("redstone", "plate"),
    ("rough", "dust"),
    ("rough", "gem"),
    ("rough", "ingot"),
    ("rough", "plate"),
    ("rubber", "dust"),
    ("rubber", "ingot"),
    ("rubber", "plate"),
    ("ruby", "dust"),
    ("ruby", "gem"),
    ("ruby", "ingot"),
    ("ruby", "plate"),
    ("sand", "dust"),
    ("shards", "dust"),
    ("shards", "gem"),
    ("shiny", "dust"),
    ("shiny", "gem"),
    ("shiny", "ingot"),
    ("shiny", "plate"),
    ("space", "dust"),
    ("space", "ingot"),
    ("space", "plate"),
    ("stone", "dust"),
    ("stone", "plate"),
    ("wood", "dust"),
    ("wood", "ingot"),
    ("wood", "plate"),
}


def png_bytes(size: int, gray: int) -> bytes:
    """Minimal 8-bit grayscale PNG: signature + IHDR + IDAT + IEND, no interlace."""
    def chunk(tag: bytes, payload: bytes) -> bytes:
        return (len(payload).to_bytes(4, "big") + tag + payload
                + zlib.crc32(tag + payload).to_bytes(4, "big"))

    ihdr = size.to_bytes(4, "big") + size.to_bytes(4, "big") + bytes([8, 0, 0, 0, 0])
    # one filter-type byte (0 = None) per scanline, flat fill
    raw = b"".join(b"\x00" + bytes([gray]) * size for _ in range(size))
    return (b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", ihdr)
            + chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b""))


def shade(iconset: str, prefix: str) -> int:
    """Deterministic mid-tone grey (distinct sets end up visually distinguishable)."""
    return 96 + zlib.crc32(f"{iconset}/{prefix}".encode()) % 96


def parse_layer0(value: str):
    """'gt6:item/material_sets/<iconset>/<prefix>' -> (iconset, prefix) or None."""
    head = "gt6:item/material_sets/"
    if not value.startswith(head):
        return None
    parts = value[len(head):].split("/")
    return (parts[0], parts[1]) if len(parts) == 2 and all(parts) else None


def combos_from_models(generated: Path):
    """Every (iconset, prefix) referenced by a generated item model."""
    found = set()
    for model in sorted((generated / MODELS_REL).glob("*.json")):
        layer0 = json.loads(model.read_text(encoding="utf-8")).get("textures", {}).get("layer0")
        combo = parse_layer0(layer0) if layer0 else None
        if combo is None:
            sys.exit(f"error: {model}: layer0 {layer0!r} is not a material_sets path")
        found.add(combo)
    return found


def generate(combos, textures_root: Path, force: bool) -> int:
    written = 0
    for iconset, prefix in sorted(combos):
        target = textures_root / iconset / f"{prefix}.png"
        if target.exists() and not force:
            continue
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(png_bytes(SIZE, shade(iconset, prefix)))
        written += 1
    return written


def main() -> None:
    tools = Path(__file__).resolve().parent
    textures_root = tools.parent / "src" / "main" / "resources" / TEXTURES_REL

    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--scan", metavar="DIR", help="derive combos from generated models under DIR and fill gaps")
    parser.add_argument("--verify", metavar="DIR", help="assert every model layer0 has a PNG; write nothing")
    parser.add_argument("--force", action="store_true", help="rewrite existing PNGs")
    args = parser.parse_args()

    if args.verify:
        combos = combos_from_models(Path(args.verify))
        missing = sorted(c for c in combos if not (textures_root / c[0] / f"{c[1]}.png").exists())
        if missing:
            sys.exit("error: missing textures: " + ", ".join(f"{i}/{p}" for i, p in missing))
        print(f"verify ok: {len(combos)} referenced combos all present under {textures_root}")
        return

    combos = set(COMBOS)
    if args.scan:
        referenced = combos_from_models(Path(args.scan))
        unknown = referenced - combos
        if unknown:
            print(f"scan: {len(unknown)} combos beyond the committed table: "
                  + ", ".join(f"{i}/{p}" for i, p in sorted(unknown)))
        combos |= referenced
    written = generate(combos, textures_root, args.force)
    print(f"{len(combos)} combos, {written} PNG written to {textures_root}")


if __name__ == "__main__":
    main()
