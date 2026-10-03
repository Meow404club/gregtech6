#!/usr/bin/env python3
"""make_panel_base — the derived blank panel-base producer (task r11-gui-clean-base-theme).

User ruling 2026-10-03 (design.r11-gui-clean-base.user_ruling_2026_10_03_b): the theme
panel base must be a GENERIC sheet — a machine-area-blank panel with the player
inventory print kept — and panel_176x166 (the plain Default.png crop) was rejected as
the base because it bakes the machine-area slot prints. This script derives the base
from one of the already-borrowed amazawa machine sheets:

  1. source = assets/gt6/textures/gui/machines/wiremill.png — picked by pixel census
     over all 256x256 sheets: FEWEST non-background pixels in the machine area
     x[4,172) y[4,83) (1392 px, ~10%; runner-up compressor 1440) among sheets that
     carry the standard player-inventory print (nei.png is flatter but prints no
     inventory block, which the base must keep);
  2. the machine-area interior x[4,172) y[4,83) is filled flat with the region's
     modal color (the skin's panel base (203,204,212)) — erasing the printed slots /
     arrow / labels while keeping every border pixel;
  3. the player-inventory band (y>=83, printed) and the border are preserved 1:1.

The output is a SELF-DERIVED asset (amazawa-derivative, Apache-2.0 ledger row in
assets/README.md "Derived panel base") — NOT a byte-identical borrow and NOT part of
the 12-crop manifest (GT6GuiPartsDatagenTest pins that manifest 1:1 against
GT6GuiParts.ALL; PANEL_BASE deliberately lives outside ALL). The Java census
GT6PanelBaseThemeCensusTest replays the derivation PIXEL-exactly against the on-tree
source, so this script and the shipped PNG can never silently drift apart.

Usage:
    python3 mdk/tools/make_panel_base.py            # derive + write the PNG
    python3 mdk/tools/make_panel_base.py --verify   # re-derive, byte-compare
(byte identity holds for the Pillow version that produced the committed file —
system python3 + Pillow 12.3.0; the Java replay is the version-independent face)
"""

import argparse
import hashlib
import sys
from pathlib import Path

from PIL import Image

# mdk/tools/this_script -> mdk -> worktree/repo root
REPO_ROOT = Path(__file__).resolve().parents[2]
ASSETS = REPO_ROOT / "mdk" / "src" / "main" / "resources" / "assets" / "gt6"

SOURCE = ASSETS / "textures" / "gui" / "machines" / "wiremill.png"
OUTPUT = ASSETS / "textures" / "gui" / "parts" / "panel_base_176x166.png"

# Panel geometry (GUI px, the panel is cropped at (0,0)-(176,166) of the 256x256 sheet).
PANEL_W, PANEL_H = 176, 166
BORDER = 4                 # 9-slice border, pinned on GT6GuiParts.PANEL_BACKGROUND too
FILL_X0, FILL_Y0 = 4, 4    # machine-area interior start (inside the border)
FILL_X1, FILL_Y1 = 172, 83 # interior end / first printed player-inventory row (y83..158)


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def derive() -> Image.Image:
    src = Image.open(SOURCE).convert("RGBA")
    px = src.load()

    # guard: the source must still be the standard amazawa sheet — flat base in the
    # machine area and a dense printed inventory band below (else the fill would
    # silently produce a base without the player-inventory print)
    from collections import Counter
    region = [px[x, y] for y in range(FILL_Y0, FILL_Y1) for x in range(FILL_X0, FILL_X1)]
    modal, count = Counter(region).most_common(1)[0]
    share = count / len(region)
    if share < 0.5:
        sys.exit(f"{SOURCE.name}: machine-area modal share {share:.1%} — not the expected flat skin")
    inv_dev = sum(1 for y in range(83, 159) for x in range(FILL_X0, FILL_X1) if px[x, y] != modal)
    if inv_dev < 0.5 * (172 - FILL_X0) * (159 - 83):
        sys.exit(f"{SOURCE.name}: player-inventory band lost its print ({inv_dev} dev px)")

    out = src.copy()
    opx = out.load()
    for y in range(FILL_Y0, FILL_Y1):
        for x in range(FILL_X0, FILL_X1):
            opx[x, y] = modal
    return out.crop((0, 0, PANEL_W, PANEL_H))


def main() -> None:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--verify", action="store_true", help="re-derive and byte-compare the shipped PNG")
    args = ap.parse_args()

    import io
    buf = io.BytesIO()
    derive().save(buf, format="PNG")
    fresh = buf.getvalue()

    if not OUTPUT.is_file():
        if args.verify:
            sys.exit(f"--verify: {OUTPUT} missing")
        OUTPUT.write_bytes(fresh)
        print(f"wrote {OUTPUT.relative_to(REPO_ROOT)} sha256 {sha256(fresh)}")
        return

    shipped = OUTPUT.read_bytes()
    if shipped == fresh:
        print(f"OK {OUTPUT.relative_to(REPO_ROOT)} sha256 {sha256(fresh)}")
        return
    if args.verify:
        sys.exit(f"--verify: {OUTPUT} drifted from the derivation (sha {sha256(shipped)} != {sha256(fresh)})")
    OUTPUT.write_bytes(fresh)
    print(f"rewrote {OUTPUT.relative_to(REPO_ROOT)} sha256 {sha256(fresh)}")


if __name__ == "__main__":
    main()
