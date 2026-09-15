#!/usr/bin/env python3
"""bake_battery_textures.py -- the p29-w4 battery family texture borrow (task
p29-w4-battery-storage). Borrow-or-declare, nothing redrawn:

  item/battery/<family>.png   = byte copy of the upstream battery block side sprite
                                (batteries/eu/standard/{8,32,128} and eu/advanced/{8,32}
                                and batteries/lu/{8,32} sides.png — the ITEM-SPRITE seat;
                                upstream files the batteries as block visuals with the
                                family colour riding the NBT_COLOR tint, which the port
                                bakes per family instead — the crank un-tinted borrow
                                posture, one declared step further: the tint lane is the
                                render-pool card, the per-family BAKE is what makes the 37
                                items tell apart in an inventory at all)
  item/battery/cell.png       = byte copy of batteries/eu/standard/8/top.png (the five
                                Filled cells share one sprite)
  block/battery_box.png       = src-over(colored/side, overlay/side) of energystorages/
                                battery_electric (the transformer bake treatment)
  block/battery_box_large.png = src-over of energystorages/battery_electric_large

The circuit carriers need NO new file: their models reuse the in-repo
item/integrated_circuit.png sprite (the declared placeholder, the circuit-card art pool).

Pure standard library; decode_png/encode_png/src_over are the bake_distillery_fronts.py
functions VERBATIM (reused, not re-implemented). Deterministic bytes, idempotent.
"""

import argparse
import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(_HERE))
from bake_distillery_fronts import decode_png, encode_png, src_over  # noqa: E402

DST = Path("mdk/src/main/resources/assets/gt6/textures")

# family -> upstream side sprite (16x16)
ITEM_COPIES = {
    "battery/lead_acid": "blocks/machines/batteries/eu/standard/8/sides.png",
    "battery/alkaline": "blocks/machines/batteries/eu/standard/32/sides.png",
    "battery/nicd": "blocks/machines/batteries/eu/standard/128/sides.png",
    "battery/licoo2": "blocks/machines/batteries/eu/advanced/8/sides.png",
    "battery/limn": "blocks/machines/batteries/eu/advanced/32/sides.png",
    "battery/energium_red": "blocks/machines/batteries/lu/8/sides.png",
    "battery/energium_cyan": "blocks/machines/batteries/lu/32/sides.png",
    "battery/cell": "blocks/machines/batteries/eu/standard/8/top.png",
}

# (product, colored layer, overlay layer) — the transformer bake treatment
BOX_BAKES = [
    ("block/battery_box.png", "blocks/machines/energystorages/battery_electric/colored/side.png",
     "blocks/machines/energystorages/battery_electric/overlay/side.png"),
    ("block/battery_box_large.png", "blocks/machines/energystorages/battery_electric_large/colored/side.png",
     "blocks/machines/energystorages/battery_electric_large/overlay/side.png"),
]


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
    for product, colored, overlay in BOX_BAKES:
        w, h, base = decode_png((upstream_textures / colored).read_bytes())
        _, _, decal = decode_png((upstream_textures / overlay).read_bytes())
        data = encode_png(w, h, src_over(base, decal))
        out = DST / product
        out.parent.mkdir(parents=True, exist_ok=True)
        if out.exists() and out.read_bytes() == data:
            continue
        out.write_bytes(data)
        print(f"BAKE src-over({colored}, {overlay}) -> {out}")
        made += 1
    print(f"{made} products written (idempotent re-run: 0)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
