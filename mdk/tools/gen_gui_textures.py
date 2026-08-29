#!/usr/bin/env python3
"""Generate placeholder GUI background textures for GT6 menu screens.

The framework screens (gregtech6.gui.GTGuiScreen) blit a 256x256 texture canvas whose panel
sits at UV 0,0 — the vanilla container texture convention (DispenserScreen.java:31-33 blits
imageWidth x imageHeight from UV 0,0; AbstractContainerScreen.java:32-33 defaults 176x166).
Produced GUI canvases:
  debug.png          GTDebugScreen (176x166 panel, 3 content slots + 36 player slots,
                     matching GTDebugMenu's bindPlayerInventory(84) geometry)
  example_chest.png  GTExampleChestScreen (176x222 panel = 114 + 6 rows * 18, vanilla
                     ContainerScreen.java:18 formula; 54 content slots at the
                     ContainerCommonChest.java:39-43 pitch + player offset
                     103+(6-4)*18 = 139 + hotbar, matching GTExampleChestMenu)

Also produces the chest's placeholder 16x16 block texture (gt6:block/example_chest) —
the blockstate model is datagen JSON, the layer texture is a PNG.

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

# block placeholder palette: plain crate-like grey tile, deterministic
BLOCK_BASE = (0x9A, 0x9A, 0x9A)
BLOCK_DARK = (0x5F, 0x5F, 0x5F)
BLOCK_SEAM = (0x7C, 0x7C, 0x7C)

CANVAS = 256  # GUI texture file edge
BLOCK_SIZE = 16  # block texture edge

PANEL_W = 176

# debug.png geometry: 3 content slots at (61,24) + player rows at (8,84)/(8,102)/(8,120)
# + hotbar (8,142), 18px pitch, panel 176x166
DEBUG_CONTENT_SLOTS = [(61 + 18 * i, 24) for i in range(3)]
DEBUG_PLAYER_OFFSET = 84
DEBUG_PANEL_H = 166


def content_grid(columns, rows, origin_x, origin_y):
    """18px-pitch slot grid (ContainerCommonChest.java:41 pitch)."""
    return [(origin_x + 18 * x, origin_y + 18 * y) for y in range(rows) for x in range(columns)]


def player_slots(offset):
    """bindPlayerInventory shape (ContainerCommon.java:327-332): 3x9 rows + hotbar at offset+58."""
    return ([(8 + 18 * j, offset + 18 * i) for i in range(3) for j in range(9)]
            + [(8 + 18 * j, offset + 58) for j in range(9)])


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


def gui_pixels(panel_h: int, content_slots: list, player_offset: int) -> list:
    """RGBA pixel list for a 256x256 canvas with a 176x(panel_h) panel at 0,0."""
    px = [TRANSPARENT] * (CANVAS * CANVAS)
    for y in range(panel_h):
        for x in range(PANEL_W):
            if x == 0 or y == 0:
                color = HIGHLIGHT
            elif x == PANEL_W - 1 or y == panel_h - 1:
                color = SHADOW
            else:
                color = PANEL
            px[y * CANVAS + x] = (*color, 255)
    # recessed 18x18 slot cells (inner 16x16 recess, vanilla-style inset)
    for sx, sy in content_slots + player_slots(player_offset):
        for dy in range(-1, 17):
            for dx in range(-1, 17):
                x, y = sx + dx, sy + dy
                if not (0 <= x < PANEL_W and 0 <= y < panel_h):
                    continue  # keep the outer panel edges intact
                if dx == -1 or dy == -1:
                    color = SLOT_DARK
                elif dx == 16 or dy == 16:
                    color = SLOT_LIGHT
                else:
                    color = SLOT_BG
                px[y * CANVAS + x] = (*color, 255)
    return px


def debug_gui_pixels() -> list:
    return gui_pixels(DEBUG_PANEL_H, DEBUG_CONTENT_SLOTS, DEBUG_PLAYER_OFFSET)


def example_chest_gui_pixels() -> list:
    """GTExampleChestMenu geometry: 54 content slots (6 rows from y=18), player offset 139
    (103 + (6-4)*18, ContainerCommonChest.java:42), hotbar 197."""
    content = content_grid(9, 6, 8, 18)
    return gui_pixels(222, content, 139)


def example_chest_block_pixels() -> list:
    """16x16 deterministic crate-style placeholder: dark border, inner seam frame."""
    px = []
    for y in range(BLOCK_SIZE):
        for x in range(BLOCK_SIZE):
            edge = x == 0 or y == 0 or x == BLOCK_SIZE - 1 or y == BLOCK_SIZE - 1
            seam = not edge and (x in (4, 11) or y in (4, 11))
            color = BLOCK_DARK if edge else (BLOCK_SEAM if seam else BLOCK_BASE)
            px.append((*color, 255))
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
        "assets/gt6/textures/gui/debug.png": (CANVAS, CANVAS, debug_gui_pixels),
        "assets/gt6/textures/gui/example_chest.png": (CANVAS, CANVAS, example_chest_gui_pixels),
        "assets/gt6/textures/block/example_chest.png": (BLOCK_SIZE, BLOCK_SIZE, example_chest_block_pixels),
    }
    written, skipped = 0, 0
    for rel, (width, height, producer) in targets.items():
        path = out_root / rel
        if path.exists() and not args.force:
            skipped += 1
            continue
        write_png(path, width, height, producer())
        written += 1
        print(f"wrote {path}")
    print(f"done: {written} written, {skipped} skipped")


if __name__ == "__main__":
    sys.exit(main())
