#!/usr/bin/env python3
"""bake_machine_fronts.py -- generalized machine front baker + oven body borrower
(task p20-borrow-machine-fronts, the p19-distillery-front-canonical generalization).

The upstream GT6 (1.7.10) machine texture system is a MULTI-LAYER per-face stack
(MultiTileEntityBasicMachine getTexture2 passes): the `colored/` layer is the
opaque grayscale material base and the `overlay*` layers are transparent-bearing
decals drawn on top at runtime. Borrowing an overlay layer alone would leave the
hollow background pixels see-through, so the borrowed layers must be BAKED into
single-layer opaque fronts for this port's single-cube + front-state machine
model (GT6BlockStates.addMachine, zero code change). Per machine family
GROUP/NAME (e.g. basicmachines/oven) three products are baked into the static
tree, one per front state the model enumeration carries (GT6BlockStates.java
:283-300, the base/_active/_running triple; the T2-T4 ladder rows share the T1
set, zero extra PNGs):

    <name>_front.png         = colored/front + overlay/front
    <name>_front_active.png  = colored/front + overlay_active/front (strip FRAME 0)
    <name>_front_running.png = colored/front + overlay_running/front (strip FRAME 0)

Compositing is the standard src-over operator (what GL_SRC_ALPHA /
ONE_MINUS_SRC_ALPHA blending does when the engine stacks the passes):
    out.rgb = src.rgb * (src.a/255) + dst.rgb * (1 - src.a/255);  out.a = 255
The colored base stays its neutral grayscale — the per-material tint is a
render-pool concern, NOT baked here (declared deviation in assets/README.md,
the P19 ruling; the runtime tint is the W3 card).

Upstream `overlay_active`/`overlay_running` fronts are animation STRIPS: 16xN
where N is a multiple of the 16px frame edge (1.7.10 auto-slices square frames;
census 2026-09-06 over the six ported families found N/16 = 1, 4, 6 and 8 —
the "16x64 four-frame" figure of the P19 card is the distillery case, not the
family maximum). The port has no .mcmeta animation carrier in the static cube
model, so the bake takes FRAME 0 (top 16x16) as the static representative
frame; the faithful animation stays in the render pool. The strip check is the
P19 shape: width == 16 and height % 16 == 0, any frame count.

--body (valid only alongside --machine basicmachines/oven) additionally borrows
the SHARED machine body key set the models hard-code (GT6BlockStates.java
:303-308, machineModel: down/up/north(front)/south+east+west(side)):
    oven_bottom.png = colored/bottom.png   (byte-identical copy)
    oven_top.png    = colored/top.png      (byte-identical copy)
    oven_side.png   = colored/left.png     (byte-identical copy)
The body needs no bake (single opaque layer, no overlay pass in the landed
single-cube model), so these are byte copies of the upstream grayscale PNGs —
the README sha256 equals the upstream sha256 (the P9 oven-overlay precedent).
Upstream left/right/back body faces that DIFFER from the borrowed one are
reported so the declared deviation can be recorded.

Borrow-or-declare: a missing upstream layer keeps the on-d placeholder PNG and
prints a DECLARE line — nothing is ever redrawn or faked.

Pure standard library (zlib+struct, the gen_textures.py idiom), deterministic
bytes, idempotent (a re-run over baked products reproduces identical bytes).
decode_png/encode_png/src_over are the p19 bake_distillery_fronts.py functions
VERBATIM — the same deterministic pipeline must yield the same bytes, which is
the new-vs-old acceptance gate: re-running this script for distillery must
reproduce the committed P19 products byte-for-byte (sha256-verified).

Usage (repo root or worktree root):
  python3 mdk/tools/bake_machine_fronts.py \
      --src tmp/gt6-1.7.10            # upstream snapshot (absolute path from a worktree)
      --machine basicmachines/oven --machine basicmachines/shredder ... [--body]
      [--dst mdk/src/main/resources/assets/gt6/textures/block]
      [--frame N]                     # overlay strip frame, default 0
      [--check]                       # bake and verify, write nothing
"""
import argparse
import hashlib
import struct
import sys
import zlib
from pathlib import Path

SIZE = 16  # vanilla block texture edge
STATES = (
    # (overlay dir under the upstream family iconset, output suffix)
    ("overlay", "_front"),
    ("overlay_active", "_front_active"),
    ("overlay_running", "_front_running"),
)
BASE_LAYER = "colored"
UPSTREAM_REL = "src/main/resources/assets/gregtech/textures/blocks/machines"

# The shared body key set (GT6BlockStates.java:303-308 machineModel cube keys):
# (upstream colored face, output file name, model face role)
BODY_FACES = (
    ("bottom", "oven_bottom.png", "down"),
    ("top", "oven_top.png", "up"),
    ("left", "oven_side.png", "south/east/west side"),
)


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


def bake_machine(args: argparse.Namespace, group: str, name: str) -> tuple[list[str], list[str]]:
    """Bake one family's three front products; returns (baked, declared) file names."""
    dist = args.src / UPSTREAM_REL / group / name
    baked: list[str] = []
    declared: list[str] = []

    base_path = dist / BASE_LAYER / "front.png"
    if not base_path.is_file():
        for _, suffix in STATES:
            declared.append(f"{name}{suffix}.png")
        print(f"DECLARE {group}/{name}: {BASE_LAYER}/front.png missing — "
              f"all three fronts keep their placeholders (borrow-or-declare)")
        return baked, declared
    base_w, base_h, base = decode_png(base_path.read_bytes())
    if (base_w, base_h) != (SIZE, SIZE):
        raise ValueError(f"{group}/{name} colored/front is {base_w}x{base_h}, expected {SIZE}x{SIZE}")
    print(f"source {group}/{name} {BASE_LAYER}/front.png".ljust(56)
          + f"{base_w}x{base_h}  sha256={sha256(base_path)}")

    for overlay, suffix in STATES:
        out_name = f"{name}{suffix}.png"
        src_path = dist / overlay / "front.png"
        if not src_path.is_file():
            declared.append(out_name)
            print(f"DECLARE {group}/{name}: {overlay}/front.png missing — "
                  f"{out_name} keeps its placeholder (borrow-or-declare)")
            continue
        w, h, overlay_px = decode_png(src_path.read_bytes())
        print(f"source {group}/{name} {overlay}/front.png".ljust(56)
              + f"{w}x{h}  sha256={sha256(src_path)}")
        if w != SIZE or h % SIZE != 0:
            raise ValueError(f"{group}/{name} {overlay}/front is {w}x{h}, expected {SIZE}xN strip")
        frames = h // SIZE
        if frames > 1:
            print(f"  strip has {frames} frames -> baking frame {args.frame} "
                  f"(static representative; animation stays render-pool)")
        fi = args.frame if overlay in ("overlay_active", "overlay_running") else 0
        if not (0 <= fi < frames):
            raise ValueError(f"frame {fi} out of range 0..{frames - 1}")
        frame = overlay_px[fi * SIZE * 4 * SIZE : (fi + 1) * SIZE * 4 * SIZE]
        product = src_over(base, frame)
        nonop = sum(1 for i in range(3, len(product), 4) if product[i] != 255)
        if nonop:
            raise ValueError(f"{out_name}: {nonop} non-opaque pixels after bake")
        data = encode_png(SIZE, SIZE, product)
        assert decode_png(data)[:2] == (SIZE, SIZE)
        out_path = args.dst / out_name
        if not args.check:
            out_path.parent.mkdir(parents=True, exist_ok=True)
            out_path.write_bytes(data)
        print(f"baked {out_name}".ljust(56)
              + f"{SIZE}x{SIZE} alpha=255/255  sha256={hashlib.sha256(data).hexdigest()}"
              + (" (check-only)" if args.check else f"  -> {out_path}"))
        baked.append(out_name)
    return baked, declared


def borrow_body(args: argparse.Namespace, group: str, name: str) -> tuple[list[str], list[str]]:
    """Byte-copy the shared body faces from one family's colored layer."""
    dist = args.src / UPSTREAM_REL / group / name
    baked: list[str] = []
    declared: list[str] = []
    body_sha = {}
    for face, out_name, role in BODY_FACES:
        src_path = dist / BASE_LAYER / f"{face}.png"
        if not src_path.is_file():
            declared.append(out_name)
            print(f"DECLARE {group}/{name}: {BASE_LAYER}/{face}.png missing — "
                  f"{out_name} keeps its placeholder (borrow-or-declare)")
            continue
        w, h, _ = decode_png(src_path.read_bytes())
        if (w, h) != (SIZE, SIZE):
            raise ValueError(f"{group}/{name} colored/{face} is {w}x{h}, expected {SIZE}x{SIZE}")
        digest = sha256(src_path)
        body_sha[face] = digest
        print(f"source {group}/{name} {BASE_LAYER}/{face}.png ({role})".ljust(56)
              + f"{w}x{h}  sha256={digest}")
        data = src_path.read_bytes()  # byte-identical copy — no re-encode
        out_path = args.dst / out_name
        if not args.check:
            out_path.parent.mkdir(parents=True, exist_ok=True)
            out_path.write_bytes(data)
        print(f"borrowed {out_name}".ljust(56)
              + f"{SIZE}x{SIZE}  sha256={digest}"
              + (" (check-only)" if args.check else f"  -> {out_path}"))
        baked.append(out_name)
    # left vs right/back: the model maps ONE side texture over south+east+west —
    # report divergences so the declared deviation lands in assets/README.md.
    for other in ("right", "back"):
        p = dist / BASE_LAYER / f"{other}.png"
        if p.is_file() and body_sha.get("left") is not None:
            d = sha256(p)
            if d != body_sha["left"]:
                print(f"NOTE {group}/{name}: colored/{other}.png differs from colored/left.png "
                      f"(sha256 {d}) — the single side key maps it too (declared deviation)")
    return baked, declared


def main(argv: list[str]) -> int:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--src", default="tmp/gt6-1.7.10", type=Path,
                    help="upstream GT6 snapshot root (default: tmp/gt6-1.7.10; pass an absolute path from a worktree)")
    ap.add_argument("--machine", action="append", required=True, metavar="GROUP/NAME",
                    help="upstream family iconset machines/GROUP/NAME (repeatable); "
                         "bakes <name>_front{,_active,_running}.png")
    ap.add_argument("--body", action="store_true",
                    help="also borrow the shared body key set (oven_bottom/top/side) — "
                         "valid only when basicmachines/oven is among --machine machines")
    ap.add_argument("--dst", type=Path,
                    default=Path("mdk/src/main/resources/assets/gt6/textures/block"))
    ap.add_argument("--frame", type=int, default=0,
                    help="overlay strip frame to bake (default 0)")
    ap.add_argument("--check", action="store_true",
                    help="bake and verify without writing")
    args = ap.parse_args(argv[1:])

    machines: list[tuple[str, str]] = []
    for spec in args.machine:
        parts = spec.split("/")
        if len(parts) != 2 or not all(parts):
            print(f"ERROR: --machine expects GROUP/NAME, got {spec!r}", file=sys.stderr)
            return 2
        machines.append((parts[0], parts[1]))
    if args.body and ("basicmachines", "oven") not in machines:
        print("ERROR: --body borrows the SHARED oven body key set — pass "
              "--machine basicmachines/oven alongside it", file=sys.stderr)
        return 2
    if not args.dst.is_dir():
        print(f"ERROR: --dst {args.dst} is not a directory", file=sys.stderr)
        return 2

    total_baked: list[str] = []
    total_declared: list[str] = []
    for group, name in machines:
        baked, declared = bake_machine(args, group, name)
        total_baked += baked
        total_declared += declared
        if args.body and (group, name) == ("basicmachines", "oven"):
            baked, declared = borrow_body(args, group, name)
            total_baked += baked
            total_declared += declared

    print(f"summary: baked={len(total_baked)} declared={len(total_declared)}")
    if total_declared:
        print("declared (placeholder kept, no upstream source):")
        for n in total_declared:
            print(f"  - {n}")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
