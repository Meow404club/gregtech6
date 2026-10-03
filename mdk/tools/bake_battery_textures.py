#!/usr/bin/env python3
"""bake_battery_textures.py -- the p29-w4 battery family texture borrow (task
p29-w4-battery-storage). Borrow-or-declare, nothing redrawn:

  item/battery/<family>.png   = byte copy of the upstream battery block side sprite
                                (batteries/eu/standard/{8,32,128} and eu/advanced/{8,32}
                                sides.png — the ITEM-SPRITE seat; upstream files the
                                batteries as block visuals with the family colour riding
                                the NBT_COLOR tint, which the port bakes per family
                                instead — the crank un-tinted borrow posture, one declared
                                step further: the tint lane is the render-pool card, the
                                per-family BAKE is what makes the 37 items tell apart in
                                an inventory at all)
  item/battery/energium_{red,cyan}.png = the lu/{8,32} sides.png MULTIPLIED by the
                                family mRGBa (MT.java:1581-1582 — red 255,0,0 / cyan
                                0,255,255; the upstream getTexture2 tint lane,
                                MultiTileEntityBatteryLU8/32). The lu pair is byte-identical
                                grayscale, so the p29-w4 plain copy left all twelve
                                crystals the same grey-white — r11-energium-tint bakes the
                                multiply in (the declared posture, not a new mechanism).
  item/battery/cell.png       = byte copy of batteries/eu/standard/8/top.png (the five
                                Filled cells share one sprite)

The block/battery_box{,_large}.png src-over bakes were RETIRED by task
tex-composite-family (the models ride the true two-layer borrows now, ledger
README.md) — the stale BOX_BAKES rows are gone, so a re-run can never resurrect
the deleted flat bakes.

Pure standard library; decode_png/encode_png are the bake_distillery_fronts.py
functions VERBATIM (reused, not re-implemented). Deterministic bytes, idempotent.
"""

import argparse
import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(_HERE))
from bake_distillery_fronts import decode_png, encode_png  # noqa: E402

DST = Path("mdk/src/main/resources/assets/gt6/textures")

# family -> upstream side sprite (16x16)
ITEM_COPIES = {
    "battery/lead_acid": "blocks/machines/batteries/eu/standard/8/sides.png",
    "battery/alkaline": "blocks/machines/batteries/eu/standard/32/sides.png",
    "battery/nicd": "blocks/machines/batteries/eu/standard/128/sides.png",
    "battery/licoo2": "blocks/machines/batteries/eu/advanced/8/sides.png",
    "battery/limn": "blocks/machines/batteries/eu/advanced/32/sides.png",
    "battery/cell": "blocks/machines/batteries/eu/standard/8/top.png",
}

# family -> (upstream side sprite, the family mRGBa the upstream MTE rides as its
# render tint — MT.java:1581-1582 / MultiTileEntityBatteryLU8.getTexture2). Kept out
# of ITEM_COPIES: the two lu sprites are byte-identical, a plain copy row would
# overwrite the tinted bake on the idempotent re-run.
TINTED_COPIES = {
    "battery/energium_red": ("blocks/machines/batteries/lu/8/sides.png", (255, 0, 0)),
    "battery/energium_cyan": ("blocks/machines/batteries/lu/32/sides.png", (0, 255, 255)),
}


def tint(rgba: bytes, rgb: tuple[int, int, int]) -> bytes:
    """The vanilla tint multiply: each RGB channel x factor / 255, alpha kept."""
    out = bytearray(rgba)
    r, g, b = rgb
    for i in range(0, len(out), 4):
        out[i] = out[i] * r // 255
        out[i + 1] = out[i + 1] * g // 255
        out[i + 2] = out[i + 2] * b // 255
    return bytes(out)


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--src", default="tmp/gt6-1.7.10", type=Path,
                    help="upstream GT6 snapshot root (default: tmp/gt6-1.7.10; pass an absolute path from a worktree)")
    aArgs = ap.parse_args()
    upstream_textures = aArgs.src / "src/main/resources/assets/gregtech/textures"
    made = 0
    for product, source in ITEM_COPIES.items():
        data = (upstream_textures / source).read_bytes()
        out = DST / "item" / f"{product}.png"
        out.parent.mkdir(parents=True, exist_ok=True)
        if out.exists() and out.read_bytes() == data:
            continue
        out.write_bytes(data)
        print(f"COPY {source} -> {out}")
        made += 1
    for product, (source, rgb) in TINTED_COPIES.items():
        w, h, base = decode_png((upstream_textures / source).read_bytes())
        data = encode_png(w, h, tint(base, rgb))
        out = DST / "item" / f"{product}.png"
        out.parent.mkdir(parents=True, exist_ok=True)
        if out.exists() and out.read_bytes() == data:
            continue
        out.write_bytes(data)
        print(f"TINT mRGBa{rgb} x {source} -> {out}")
        made += 1
    print(f"{made} products written (idempotent re-run: 0)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
