#!/usr/bin/env python3
"""bake_render_pool_textures.py -- the p36-render-texture-bake borrow wave (task
p36-render-texture-bake). Borrow-or-bake, nothing redrawn; retires three stand-ins
self-declared in GT6BlockStates.java (the crystal chargers' battery-box cube, the LD
wire wire_electric cube, the LD transformer's electric-transformer model share) and
lands the ZPM Decharger art ahead of the battery card (the declared consumer):

  block/crystal_charger{,_large}_{front,side}.png
      = src-over(colored, overlay) of machines/energystorages/crystal_laser{,_large}
        (the battery-box bake treatment; the laser FRONT face + the shared side art;
        the overlay_active/overlay_blinking trios stay unborrowed — the port blocks
        carry no ACTIVE property, the standing static-face posture)
  block/long_dist_wire_{ev,iv,luv,zpm,uv}.png
      = byte copy of blocks/iconsets/LONG_DIST_WIRE_<TIER>.png (the five distinct
        sprites of the upstream LONG_DIST_WIRES_01 iconset, Textures.java:638-655;
        the 16 metas map onto these five by the tier byte, Loader_Blocks.java:160)
  block/long_distance_transformer_{front,back,side}.png
      = src-over(colored, overlay) of machines/transformers/
        longdistancetransformer_electric (front = the INPUT face, back = the OUTPUT
        face, MultiTileEntityLongDistanceTransformer.java:295-299; the colored layer
        is one byte-identical casing across the three faces, only the overlays differ;
        overlay_active/blinking/unloaded stay unborrowed — no ACTIVE property)
  block/zpm_decharger{,_quantum}_{front,back,side}.png
      = src-over(colored, overlay) of machines/energystorages/zpm_electricity|
        zpm_quantum (MultiTileEntityZPMDechargerEU.java:47-63 / QU.java:47-54; landed
        AHEAD of the consumer — the p36-energy-zpm-dechargers card binds these paths
        on its rebase; the ZPM_TOP active-top decal is an active-state art, unborrowed)

Pure standard library; decode_png/encode_png/src_over are the bake_distillery_fronts.py
functions VERBATIM (reused, not re-implemented — the bake_battery_textures.py form).
Deterministic bytes, idempotent re-run.
"""

import argparse
import hashlib
import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(_HERE))
from bake_distillery_fronts import decode_png, encode_png, src_over  # noqa: E402

DST = Path("mdk/src/main/resources/assets/gt6/textures/block")

STORAGE = "blocks/machines/energystorages"
TRANSFORMERS = "blocks/machines/transformers/longdistancetransformer_electric"
ICONSETS = "blocks/iconsets"

# (product, colored layer, overlay layer) — the battery-box/transformer bake treatment
BAKES = [
    ("crystal_charger_front.png", f"{STORAGE}/crystal_laser/colored/front.png",
     f"{STORAGE}/crystal_laser/overlay/front.png"),
    ("crystal_charger_side.png", f"{STORAGE}/crystal_laser/colored/side.png",
     f"{STORAGE}/crystal_laser/overlay/side.png"),
    ("crystal_charger_large_front.png", f"{STORAGE}/crystal_laser_large/colored/front.png",
     f"{STORAGE}/crystal_laser_large/overlay/front.png"),
    ("crystal_charger_large_side.png", f"{STORAGE}/crystal_laser_large/colored/side.png",
     f"{STORAGE}/crystal_laser_large/overlay/side.png"),
    ("long_distance_transformer_front.png", f"{TRANSFORMERS}/colored/front.png",
     f"{TRANSFORMERS}/overlay/front.png"),
    ("long_distance_transformer_back.png", f"{TRANSFORMERS}/colored/back.png",
     f"{TRANSFORMERS}/overlay/back.png"),
    ("long_distance_transformer_side.png", f"{TRANSFORMERS}/colored/side.png",
     f"{TRANSFORMERS}/overlay/side.png"),
    ("zpm_decharger_front.png", f"{STORAGE}/zpm_electricity/colored/front.png",
     f"{STORAGE}/zpm_electricity/overlay/front.png"),
    ("zpm_decharger_back.png", f"{STORAGE}/zpm_electricity/colored/back.png",
     f"{STORAGE}/zpm_electricity/overlay/back.png"),
    ("zpm_decharger_side.png", f"{STORAGE}/zpm_electricity/colored/side.png",
     f"{STORAGE}/zpm_electricity/overlay/side.png"),
    ("zpm_decharger_quantum_front.png", f"{STORAGE}/zpm_quantum/colored/front.png",
     f"{STORAGE}/zpm_quantum/overlay/front.png"),
    ("zpm_decharger_quantum_back.png", f"{STORAGE}/zpm_quantum/colored/back.png",
     f"{STORAGE}/zpm_quantum/overlay/back.png"),
    ("zpm_decharger_quantum_side.png", f"{STORAGE}/zpm_quantum/colored/side.png",
     f"{STORAGE}/zpm_quantum/overlay/side.png"),
]

# the five distinct sprites of the LONG_DIST_WIRES_01 iconset (byte copies)
COPIES = {
    "long_dist_wire_ev.png": f"{ICONSETS}/LONG_DIST_WIRE_EV.png",
    "long_dist_wire_iv.png": f"{ICONSETS}/LONG_DIST_WIRE_IV.png",
    "long_dist_wire_luv.png": f"{ICONSETS}/LONG_DIST_WIRE_LuV.png",
    "long_dist_wire_zpm.png": f"{ICONSETS}/LONG_DIST_WIRE_ZPM.png",
    "long_dist_wire_uv.png": f"{ICONSETS}/LONG_DIST_WIRE_UV.png",
}


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--src", default="tmp/gt6-1.7.10", type=Path,
                    help="upstream GT6 snapshot root (default: tmp/gt6-1.7.10; pass an absolute path from a worktree)")
    aArgs = ap.parse_args()
    upstream_textures = aArgs.src / "src/main/resources/assets/gregtech/textures"
    DST.mkdir(parents=True, exist_ok=True)
    made = 0
    for product, colored, overlay in BAKES:
        w, h, base = decode_png((upstream_textures / colored).read_bytes())
        _, _, decal = decode_png((upstream_textures / overlay).read_bytes())
        data = encode_png(w, h, src_over(base, decal))
        out = DST / product
        if out.exists() and out.read_bytes() == data:
            continue
        out.write_bytes(data)
        print(f"BAKE src-over({colored}, {overlay}) -> {out}")
        made += 1
    for product, source in COPIES.items():
        data = (upstream_textures / source).read_bytes()
        out = DST / product
        if out.exists() and out.read_bytes() == data:
            continue
        out.write_bytes(data)
        print(f"COPY {source} -> {out}")
        made += 1
    print(f"{made} products written (idempotent re-run: 0)")
    for png in sorted(DST.glob("*.png")):
        if png.name in {p for p, _, _ in BAKES} | set(COPIES):
            print(f"SHA256 {png.name} {hashlib.sha256(png.read_bytes()).hexdigest()}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
