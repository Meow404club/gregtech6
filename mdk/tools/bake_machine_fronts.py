#!/usr/bin/env python3
"""bake_machine_fronts.py -- machine front splitter (p22-paint-front-overlay-split),
with the p20 baked-front compositor kept as a RETIRED audit mode.

The upstream GT6 (1.7.10) machine texture system is a MULTI-LAYER per-face stack
(MultiTileEntityBasicMachine getTexture2 passes): the `colored/` layer is the
opaque grayscale material base (tinted by mRGBa at runtime) and the `overlay*`
layers are transparent-bearing decals drawn on top UNTINTED
(BlockTextureDefault(IIcon, boolean) = UNCOLOURED, :179-180).

--split-fronts (the p22 ACTIVE mode, task p22-paint-front-overlay-split): the
port's machine model became a TWO-ELEMENT model (the full tinted body cube +
a thin untinted front overlay quad), so the borrow now lands the layers SEPARATELY
instead of compositing them. Per machine family GROUP/NAME four products:

    <name>_colored_front.png         = colored/front.png          (byte copy)
    <name>_overlay_front.png         = overlay/front.png          (FRAME 0 if a strip)
    <name>_overlay_front_active.png  = overlay_active/front.png   (FRAME 0)
    <name>_overlay_front_running.png = overlay_running/front.png  (FRAME 0)

The `colored` body face keeps its neutral grayscale (the runtime paint tint is
the GTMachinePaintTint tintindex-0 seat); the overlay faces carry NO tintindex,
so a painted machine no longer re-tints the state decal (the upstream two-layer
semantics). Animation stays retired: a 16xN strip is cropped to its FRAME 0
16x16 representative and re-encoded (filter 0, deterministic); a plain 16x16
source is copied BYTE-IDENTICAL (sha256 == upstream). The P20 declared deviation
"animation strips baked at FRAME 0" carries over verbatim. NOTE: the P9
GTOvenOverlayModel strip borrows (oven_overlay_active_front.png /
oven_overlay_running_front.png, full 16xN strips) are a DIFFERENT namespace and
are not touched by this mode.

--machine (no --split-fronts): the RETIRED P20 bake mode, kept verbatim as the
audit path — it still composites <name>_front{,_active,_running}.png into
single-layer opaque fronts (byte-for-byte reproducible), but those products are
no longer referenced by any model (the P22 two-element model replaced them; the
retirement is recorded in assets/README.md). Do not land its output.

--body (valid only alongside --machine basicmachines/oven, also retired with the
bake wave) borrows the SHARED body key set:
    oven_bottom.png = colored/bottom.png   (byte-identical copy)
    oven_top.png    = colored/top.png      (byte-identical copy)
    oven_side.png   = colored/left.png     (byte-identical copy)

Borrow-or-declare: a missing upstream layer keeps the on-disk file and prints a
DECLARE line — nothing is ever redrawn or faked.

Pure standard library (zlib+struct, the gen_textures.py idiom), deterministic
bytes, idempotent. decode_png/encode_png/src_over are the p19
bake_distillery_fronts.py functions VERBATIM.

Usage (repo root or worktree root):
  python3 mdk/tools/bake_machine_fronts.py \
      --src tmp/gt6-1.7.10            # upstream snapshot (absolute path from a worktree)
      --split-fronts                  # p22 mode: four separate-layer products per family
      --machine basicmachines/oven --machine basicmachines/shredder ...
      [--dst mdk/src/main/resources/assets/gt6/textures/block]
      [--frame N]                     # overlay strip frame (legacy bake mode), default 0
      [--check]                       # verify, write nothing
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

# The p22 split-front products (one family row = four separate-layer borrows):
# (upstream layer/front file, output name infix). The infix joins the family
# name into <name><infix>.png; the overlay infixes keep the retired bake
# products' front/front_active/front_running state tail so the model texture
# names read as "the overlay for that state".
SPLIT_PRODUCTS = (
    ("colored/front.png", "_colored_front"),
    ("overlay/front.png", "_overlay_front"),
    ("overlay_active/front.png", "_overlay_front_active"),
    ("overlay_running/front.png", "_overlay_front_running"),
)

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


def split_fronts(args: argparse.Namespace, group: str, name: str) -> tuple[list[str], list[str]]:
    """The p22 mode: land the colored/ + overlay*/ front layers SEPARATELY.

    A plain 16x16 source is a byte-identical copy (sha256 == upstream); a 16xN
    strip is cropped to FRAME 0 and re-encoded deterministically. Returns
    (borrowed, declared) file names.
    """
    dist = args.src / UPSTREAM_REL / group / name
    borrowed: list[str] = []
    declared: list[str] = []
    for rel, infix in SPLIT_PRODUCTS:
        out_name = f"{name}{infix}.png"
        src_path = dist / rel
        if not src_path.is_file():
            declared.append(out_name)
            print(f"DECLARE {group}/{name}: {rel} missing — "
                  f"{out_name} keeps its on-disk file (borrow-or-declare)")
            continue
        w, h, _ = decode_png(src_path.read_bytes())
        if w != SIZE or h % SIZE != 0:
            raise ValueError(f"{group}/{name} {rel} is {w}x{h}, expected {SIZE}xN")
        frames = h // SIZE
        if frames > 1:
            print(f"  strip has {frames} frames -> cropping FRAME {args.frame} "
                  f"(static representative; animation stays retired)")
            if not (0 <= args.frame < frames):
                raise ValueError(f"frame {args.frame} out of range 0..{frames - 1}")
            _, _, px = decode_png(src_path.read_bytes())
            data = encode_png(SIZE, SIZE, px[args.frame * SIZE * 4 * SIZE:(args.frame + 1) * SIZE * 4 * SIZE])
            action = f"cropped {out_name}"
        else:
            data = src_path.read_bytes()  # byte-identical copy — no re-encode
            action = f"borrowed {out_name}"
        assert decode_png(data)[:2] == (SIZE, SIZE)
        if not args.check:
            out_path = args.dst / out_name
            out_path.parent.mkdir(parents=True, exist_ok=True)
            out_path.write_bytes(data)
            print(action.ljust(56) + f"{SIZE}x{SIZE}  sha256={hashlib.sha256(data).hexdigest()}  -> {out_path}")
        else:
            print(action.ljust(56) + f"{SIZE}x{SIZE}  sha256={hashlib.sha256(data).hexdigest()}  (check-only)")
        borrowed.append(out_name)
    return borrowed, declared


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
                         "split mode lands four separate-layer products, bake mode "
                         "composites <name>_front{,_active,_running}.png (retired)")
    ap.add_argument("--split-fronts", action="store_true",
                    help="p22 mode: borrow colored/front + overlay*/front SEPARATELY "
                         "(byte-copy, strips cropped to FRAME 0) instead of baking")
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
        if args.split_fronts:
            baked, declared = split_fronts(args, group, name)
        else:
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
