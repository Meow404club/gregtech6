#!/usr/bin/env python3
"""Generate placeholder GUI background textures for GT6 menu screens.

The framework screens (gregtech6.gui.GTGuiScreen) blit a 256x256 texture canvas whose panel
sits at UV 0,0 — the vanilla container texture convention (DispenserScreen.java:31-33 blits
imageWidth x imageHeight from UV 0,0; AbstractContainerScreen.java:32-33 defaults 176x166).
Currently produced: debug.png for GTDebugScreen (176x166 panel, 3 content slots + 36 player
slots matching GTDebugMenu's bindPlayerInventory(84) geometry).

Pure standard library (hand-rolled PNG: zlib+struct, mirroring mdk/tools/gen_textures.py),
idempotent (skips existing files unless --force), deterministic output bytes. Textures are
PNGs, not JSON — no DataGen red-line conflict.

Usage:
  python3 gen_gui_textures.py            # generate missing textures
  python3 gen_gui_textures.py --force    # regenerate everything
OUT defaults to mdk/src/main/resources (relative to this script's repo root), or pass a path.
"""
import argparse
import struct
import sys
import zlib
from pathlib import Path

# vanilla container palette: raised panel (top-left highlight, bottom-right shadow)
PANEL = (0xC6, 0xC6, 0xC6)      # standard GUI grey
HIGHLIGHT = (0xFF, 0xFF, 0xFF)  # top/left edge
SHADOW = (0x55, 0x55, 0x55)     # bottom/right edge
SLOT_BG = (0x8B, 0x8B, 0x8B)    # recessed slot fill
SLOT_DARK = (0x37, 0x37, 0x37)  # slot inset top/left
SLOT_LIGHT = (0xFF, 0xFF, 0xFF)  # slot inset bottom/right
TRANSPARENT = (0, 0, 0, 0)

CANVAS = 256  # texture file edge
PANEL_W, PANEL_H = 176, 166

# slot cell top-left corners (panel coordinates), 18px pitch:
# 3 content slots at (61,24) + player rows at (8,84)/(8,102)/(8,120) + hotbar (8,142)
CONTENT_SLOTS = [(61 + 18 * i, 24) for i in range(3)]
PLAYER_ROWS = [(8 + 18 * j, 84 + 18 * i) for i in range(3) for j in range(9)]
HOTBAR = [(8 + 18 * j, 142) for j in range(9)]
SLOTS = CONTENT_SLOTS + PLAYER_ROWS + HOTBAR


def write_png(path: Path, width: int, height: int, pixels: list) -> None:
    """Minimal 8-bit RGBA PNG: signature + IHDR + IDAT + IEND, no interlace."""
    def chunk(tag: bytes, payload: bytes) -> bytes:
        return (len(payload).to_bytes(4, "big") + tag + payload
                + zlib.crc32(tag + payload).to_bytes(4, "big"))

    ihdr = width.to_bytes(4, "big") + height.to_bytes(4, "big") + bytes([8, 6, 0, 0, 0])
    raw = b"".join(
        b"\x00" + b"".join(struct.pack("4B", *pixels[y * width + x]) for x in range(width))
        for y in range(height)
    )
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", ihdr)
                     + chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b""))


def gui_debug_pixels() -> list:
    """RGBA pixel list for a 256x256 canvas with a 176x166 panel at 0,0 (GTDebugMenu geometry)."""
    px = [TRANSPARENT] * (CANVAS * CANVAS)
    for y in range(PANEL_H):
        for x in range(PANEL_W):
            if x == 0 or y == 0:
                color = HIGHLIGHT
            elif x == PANEL_W - 1 or y == PANEL_H - 1:
                color = SHADOW
            else:
                color = PANEL
            px[y * CANVAS + x] = (*color, 255)
    # recessed 18x18 slot cells (inner 16x16 recess, vanilla-style inset)
    for sx, sy in SLOTS:
        for dy in range(-1, 17):
            for dx in range(-1, 17):
                x, y = sx + dx, sy + dy
                if not (0 <= x < PANEL_W and 0 <= y < PANEL_H):
                    continue  # keep the outer panel edges intact
                if dx == -1 or dy == -1:
                    color = SLOT_DARK
                elif dx == 16 or dy == 16:
                    color = SLOT_LIGHT
                else:
                    color = SLOT_BG
                px[y * CANVAS + x] = (*color, 255)
    return px


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--force", action="store_true", help="regenerate even if the file exists")
    parser.add_argument("out", nargs="?", default=None,
                        help="target resources root (default: <repo>/mdk/src/main/resources)")
    args = parser.parse_args()

    repo_root = Path(__file__).resolve().parent.parent.parent
    out_root = Path(args.out) if args.out else repo_root / "mdk" / "src" / "main" / "resources"

    targets = {
        "assets/gt6/textures/gui/debug.png": gui_debug_pixels,
    }
    written, skipped = 0, 0
    for rel, producer in targets.items():
        path = out_root / rel
        if path.exists() and not args.force:
            skipped += 1
            continue
        write_png(path, CANVAS, CANVAS, producer())
        written += 1
        print(f"wrote {path}")
    print(f"done: {written} written, {skipped} skipped")


if __name__ == "__main__":
    sys.exit(main())
