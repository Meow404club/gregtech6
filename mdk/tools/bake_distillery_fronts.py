#!/usr/bin/env python3
"""bake_distillery_fronts.py -- bake the three canonical Distillery front PNGs (task p19-distillery-front-canonical).

The upstream GT6 (1.7.10) machine texture system is a MULTI-LAYER per-face stack
(MultiTileEntityBasicMachine getTexture2 passes): the `colored/` layer is the
opaque grayscale material base and the `overlay*` layers are transparent-bearing
decals drawn on top at runtime. Borrowing an overlay layer alone would leave the
hollow background pixels see-through, so the borrowed layers must be BAKED into
single-layer opaque fronts for this port's single-cube + front-state machine
model (GT6BlockStates.addMachine, zero code change):

    distillery_front.png         = colored/front + overlay/front
    distillery_front_active.png  = colored/front + overlay_active/front (strip FRAME 0)
    distillery_front_running.png = colored/front + overlay_running/front

Compositing is the standard src-over operator (what GL_SRC_ALPHA /
ONE_MINUS_SRC_ALPHA blending does when the engine stacks the passes):
    out.rgb = src.rgb * (src.a/255) + dst.rgb * (1 - src.a/255);  out.a = 255
The colored base stays its neutral grayscale — the per-material tint is a
render-pool concern, NOT baked here (declared deviation in assets/README.md).

NOTE: upstream `overlay_active/front.png` is a 16x64 four-frame animation strip
(1.7.10 auto-slices square frames; the port has no .mcmeta animation and the
acceptance requires 16x16), so the bake takes FRAME 0 (top 16x16) as the static
representative frame; the faithful 4-frame animation stays in the render pool.

Pure standard library (zlib+struct, the gen_textures.py idiom), deterministic
bytes, idempotent. Prints sha256 of every source and product; `--ascii` renders
the baked fronts as luminance art so the overlay pattern can be eyeballed.

Usage (repo root or worktree root):
  python3 mdk/tools/bake_distillery_fronts.py \
      --src tmp/gt6-1.7.10            # upstream snapshot (absolute path if outside this tree)
      [--dst mdk/src/main/resources/assets/gt6/textures/block]
      [--frame N]                     # overlay_active strip frame, default 0
      [--ascii] [--check]             # render art / assert opaque+16x16 without writing
"""
import argparse
import hashlib
import struct
import sys
import zlib
from pathlib import Path

SIZE = 16  # vanilla block texture edge
LAYERS = (
    # (overlay dir under the upstream distillery iconset, output file name)
    ("overlay", "distillery_front.png"),
    ("overlay_active", "distillery_front_active.png"),
    ("overlay_running", "distillery_front_running.png"),
)
BASE_LAYER = "colored"
UPSTREAM_REL = "src/main/resources/assets/gregtech/textures/blocks/machines/basicmachines/distillery"


def decode_png(data: bytes) -> tuple[int, int, bytes]:
    """Decode a depth-8 non-interlaced PNG to raw RGBA bytes."""
    if data[:8] != b"\x89PNG\r\n\x1a\n":
        raise ValueError("not a PNG")
    pos, idat, ihdr, plte, trns = 8, b"", None, None, None
    while pos < len(data):
        (ln,) = struct.unpack(">I", data[pos : pos + 4])
        typ = data[pos + 4 : pos + 8]
        end = pos + 8 + ln
        body = data[pos + 8 : end]
        if typ == b"IHDR":
            ihdr = struct.unpack(">IIBBBBB", body)
        elif typ == b"IDAT":
            idat += body
        elif typ == b"PLTE":
            plte = body
        elif typ == b"tRNS":
            trns = body
        pos = end + 4
    w, h, depth, ctype, _, _, inter = ihdr
    if depth != 8 or inter != 0:
        raise ValueError(f"unsupported PNG (depth={depth} interlace={inter})")
    ch = {0: 1, 2: 3, 3: 1, 4: 2, 6: 4}[ctype]
    raw = zlib.decompress(idat)
    stride = w * ch
    out = bytearray(h * stride)
    prev = bytearray(stride)
    p = 0
    for y in range(h):
        f = raw[p]
        p += 1
        line = bytearray(raw[p : p + stride])
        p += stride
        if f == 1:
            for i in range(ch, stride):
                line[i] = (line[i] + line[i - ch]) & 255
        elif f == 2:
            for i in range(stride):
                line[i] = (line[i] + prev[i]) & 255
        elif f == 3:
            for i in range(stride):
                a = line[i - ch] if i >= ch else 0
                line[i] = (line[i] + ((a + prev[i]) >> 1)) & 255
        elif f == 4:
            for i in range(stride):
                a = line[i - ch] if i >= ch else 0
                b = prev[i]
                c = prev[i - ch] if i >= ch else 0
                pa, pb, pc = abs(b - c), abs(a - c), abs(a + b - 2 * c)
                pr = a if (pa <= pb and pa <= pc) else (b if pb <= pc else c)
                line[i] = (line[i] + pr) & 255
        out[y * stride : (y + 1) * stride] = line
        prev = line
    rgba = bytearray(w * h * 4)
    if ctype == 6:
        rgba = out
    elif ctype == 2:
        for i in range(w * h):
            rgba[i * 4 : i * 4 + 3] = out[i * 3 : i * 3 + 3]
            rgba[i * 4 + 3] = 255
    elif ctype == 0:
        for i in range(w * h):
            v = out[i]
            rgba[i * 4 : i * 4 + 4] = bytes((v, v, v, 255))
    elif ctype == 3:
        for i in range(w * h):
            idx = out[i]
            rgba[i * 4 : i * 4 + 3] = plte[idx * 3 : idx * 3 + 3]
            rgba[i * 4 + 3] = trns[idx] if trns and idx < len(trns) else 255
    else:
        raise ValueError(f"unsupported color type {ctype}")
    return w, h, bytes(rgba)


def encode_png(w: int, h: int, rgba: bytes) -> bytes:
    """Encode RGBA bytes as a depth-8 ctype-6 non-interlaced PNG (filter 0)."""
    stride = w * 4
    raw = bytearray()
    for y in range(h):
        raw.append(0)  # filter type 0 (None) — deterministic
        raw += rgba[y * stride : (y + 1) * stride]
    comp = zlib.compress(bytes(raw), 9)

    def chunk(typ: bytes, body: bytes) -> bytes:
        return (
            struct.pack(">I", len(body))
            + typ
            + body
            + struct.pack(">I", zlib.crc32(typ + body) & 0xFFFFFFFF)
        )

    return (
        b"\x89PNG\r\n\x1a\n"
        + chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0))
        + chunk(b"IDAT", comp)
        + chunk(b"IEND", b"")
    )


def src_over(dst: bytes, src: bytes) -> bytes:
    """Standard src-over compositing; the colored base is opaque so out.a = 255."""
    out = bytearray(len(dst))
    for i in range(0, len(dst), 4):
        sa = src[i + 3]
        if sa == 255:
            out[i : i + 4] = src[i : i + 4]
        elif sa == 0:
            out[i : i + 4] = dst[i : i + 4]
        else:
            for c in range(3):
                out[i + c] = (
                    src[i + c] * sa + dst[i + c] * (255 - sa) + 127
                ) // 255
            out[i + 3] = 255
    return bytes(out)


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def ascii_art(w: int, h: int, rgba: bytes, title: str) -> str:
    ramp = " .:-=+*#%@"
    rows = [f"  {title}"]
    for y in range(h):
        row = []
        for x in range(w):
            i = (y * w + x) * 4
            lum = (rgba[i] * 299 + rgba[i + 1] * 587 + rgba[i + 2] * 114) // 1000
            row.append(ramp[min(lum * len(ramp) // 256, len(ramp) - 1)])
        rows.append("  " + "".join(row))
    return "\n".join(rows)


def main(argv: list[str]) -> int:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--src", default="tmp/gt6-1.7.10", type=Path,
                    help="upstream GT6 snapshot root (default: tmp/gt6-1.7.10; pass an absolute path from a worktree)")
    ap.add_argument("--dst", type=Path,
                    default=Path("mdk/src/main/resources/assets/gt6/textures/block"))
    ap.add_argument("--frame", type=int, default=0,
                    help="overlay_active strip frame to bake (default 0)")
    ap.add_argument("--ascii", action="store_true", help="render baked fronts as luminance art")
    ap.add_argument("--check", action="store_true",
                    help="assert 16x16 fully opaque products without writing")
    args = ap.parse_args(argv[1:])

    dist = args.src / UPSTREAM_REL
    if not dist.is_dir():
        print(f"ERROR: upstream distillery iconset not found under --src {args.src}\n"
              f"  looked at: {dist}", file=sys.stderr)
        return 1

    base_w, base_h, base = decode_png((dist / BASE_LAYER / "front.png").read_bytes())
    if (base_w, base_h) != (SIZE, SIZE):
        raise ValueError(f"colored/front is {base_w}x{base_h}, expected {SIZE}x{SIZE}")
    print(f"source {BASE_LAYER}/front.png        {base_w}x{base_h}  sha256={sha256(dist / BASE_LAYER / 'front.png')}")

    ok = True
    for overlay, out_name in LAYERS:
        src_path = dist / overlay / "front.png"
        w, h, overlay_px = decode_png(src_path.read_bytes())
        print(f"source {overlay}/front.png".ljust(32)
              + f"{w}x{h}  sha256={sha256(src_path)}")
        if w != SIZE or h % SIZE != 0:
            raise ValueError(f"{overlay}/front is {w}x{h}, expected {SIZE}xN strip")
        frames = h // SIZE
        if overlay == "overlay_active" and frames > 1:
            print(f"  strip has {frames} frames -> baking frame {args.frame} "
                  f"(static representative; 4-frame animation stays render-pool)")
        fi = args.frame if overlay == "overlay_active" else 0
        if not (0 <= fi < frames):
            raise ValueError(f"frame {fi} out of range 0..{frames - 1}")
        frame = overlay_px[fi * SIZE * 4 * SIZE : (fi + 1) * SIZE * 4 * SIZE]
        baked = src_over(base, frame)
        nonop = sum(1 for i in range(3, len(baked), 4) if baked[i] != 255)
        if nonop:
            print(f"ERROR: {out_name} has {nonop} non-opaque pixels", file=sys.stderr)
            ok = False
            continue
        out_path = args.dst / out_name
        data = encode_png(SIZE, SIZE, baked)
        assert decode_png(data)[:2] == (SIZE, SIZE)
        if args.check:
            print(f"baked {out_name}".ljust(32) + f"{SIZE}x{SIZE} alpha=255/255 (check-only)")
        else:
            out_path.parent.mkdir(parents=True, exist_ok=True)
            out_path.write_bytes(data)
            print(f"baked {out_name}".ljust(32) + f"{SIZE}x{SIZE} alpha=255/255  sha256={hashlib.sha256(data).hexdigest()}")
            print(f"  -> {out_path}")
        if args.ascii:
            print(ascii_art(SIZE, SIZE, baked, out_name))
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main(sys.argv))
