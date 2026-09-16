#!/usr/bin/env python3
"""bake_armor_textures.py -- the p29-w5-t8-armor-24 placeholder armor art (the P20
placeholder-PNG judging precedent: GENERATED, no upstream bytes borrowed, nothing
redrawn from a real sprite — the real art is a texture-sprint pool item, the card's
open_questions ruling).

  item/armor/<suit>/<piece>.png  16x16  the 24 inventory icons (per-set colour, a
                                 minimal per-piece silhouette so the four slots tell
                                 apart at a glance)
  models/armor/<suit>_layer_1.png 64x32 the worn outer layer (head + body + arms +
                                 boots, the canonical humanoid layout)
  models/armor/<suit>_layer_2.png 64x32 the worn inner layer (legs only)

The layer files resolve through the GT6ArmorItem.getArmorTexture override (the
ForgeHooksClient :264 seam) — vanilla HumanoidArmorLayer composes
textures/models/armor/<material>_layer_<1|2>.png itself. Deterministic bytes,
idempotent, pure standard library.

Run:  python3 mdk/tools/bake_armor_textures.py
"""

import struct
import sys
import zlib
from pathlib import Path

RES = Path(__file__).resolve().parent.parent / "src" / "main" / "resources" / "assets" / "gt6" / "textures"

# per-set base colours (r, g, b) — flat, no shading lanes (the placeholder ruling);
# keys are the GT6ArmorMaterials texture words (the directory/texture path tail)
SUIT_COLORS = {
    "hazmat_insect": (200, 160, 40),
    "hazmat_frost": (91, 200, 232),
    "hazmat_heat": (232, 104, 48),
    "hazmat_radiation": (124, 200, 50),
    "hazmat_biochemgas": (216, 200, 96),
    "hazmat_universal": (144, 144, 144),
}
SUITS = list(SUIT_COLORS)
PIECES = ("helmet", "chestplate", "leggings", "boots")


def chunk(tag, data):
    return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)


def encode_png(pixels, w, h):
    """RGBA, no filter — the bake_distillery_fronts.py encode shape (stdlib only)."""
    raw = b"".join(b"\x00" + bytes(pixels[y * w * 4:(y + 1) * w * 4]) for y in range(h))
    return (b"\x89PNG\r\n\x1a\n"
            + chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0))
            + chunk(b"IDAT", zlib.compress(raw, 9))
            + chunk(b"IEND", b""))


def blank(w, h):
    return bytearray(w * h * 4)


def put(px, w, x, y, c, a=255):
    i = (y * w + x) * 4
    px[i:i + 4] = bytes((c[0], c[1], c[2], a))


def fill(px, w, x0, y0, x1, y1, c, a=255):
    for y in range(y0, y1):
        for x in range(x0, x1):
            put(px, w, x, y, c, a)


def shade(c, f):
    return (min(255, int(c[0] * f)), min(255, int(c[1] * f)), min(255, int(c[2] * f)))


def icon(piece, c):
    """16x16 icon — dark border + the piece silhouette in the set colour."""
    px, d = blank(16, 16), shade(c, 0.55)
    if piece == "helmet":       # dome + visor slit
        fill(px, 16, 3, 4, 13, 12, c)
        fill(px, 16, 3, 4, 13, 6, d)
        fill(px, 16, 5, 8, 11, 10, d)
    elif piece == "chestplate":  # torso + shoulders
        fill(px, 16, 2, 3, 6, 7, d)
        fill(px, 16, 10, 3, 14, 7, d)
        fill(px, 16, 4, 3, 12, 14, c)
        fill(px, 16, 6, 6, 10, 12, d)
    elif piece == "leggings":    # waist + two legs
        fill(px, 16, 3, 3, 13, 6, c)
        fill(px, 16, 4, 6, 7, 14, c)
        fill(px, 16, 9, 6, 12, 14, c)
    else:                        # boots: two low blocks
        fill(px, 16, 3, 8, 7, 14, c)
        fill(px, 16, 9, 8, 13, 14, c)
        fill(px, 16, 3, 12, 13, 14, d)
    return px


def layer1(c):
    """64x32 outer layer — head strip + body + arms + the boots tail of the leg region."""
    px, d = blank(64, 32), shade(c, 0.8)
    fill(px, 64, 0, 0, 32, 16, c)        # head cube faces
    fill(px, 64, 8, 8, 16, 14, d)        # the face window (visor)
    fill(px, 64, 16, 16, 40, 32, c)      # body region
    fill(px, 64, 40, 16, 56, 32, c)      # arm region
    fill(px, 64, 0, 26, 16, 32, d)       # boots (lower leg rows, one shade down)
    return px


def layer2(c):
    """64x32 inner layer — the upper leg rows only (the pants)."""
    px = blank(64, 32)
    fill(px, 64, 0, 16, 16, 26, c)
    return px


def main():
    n = 0
    for suit in SUITS:
        c = SUIT_COLORS[suit]
        for piece in PIECES:
            (RES / "item" / "armor" / suit).mkdir(parents=True, exist_ok=True)
            (RES / "item" / "armor" / suit / (piece + ".png")).write_bytes(encode_png(icon(piece, c), 16, 16))
            n += 1
        (RES / "models" / "armor").mkdir(parents=True, exist_ok=True)
        (RES / "models" / "armor" / (suit + "_layer_1.png")).write_bytes(encode_png(layer1(c), 64, 32))
        (RES / "models" / "armor" / (suit + "_layer_2.png")).write_bytes(encode_png(layer2(c), 64, 32))
        n += 2
    print(f"bake_armor_textures: wrote {n} placeholder PNGs under {RES}")


if __name__ == "__main__":
    sys.exit(main())
