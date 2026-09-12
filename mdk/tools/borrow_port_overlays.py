#!/usr/bin/env python3
"""borrow_port_overlays.py -- per-port per-face overlay asset borrow (p28-a-port-overlay-assets).

Lands the machine PORT-face art the B render card will hang decals on: per
basicmachines family the full colored/ six-face body set plus the five
NON-front side faces of the three state overlay layers (the front face is the
work face -- its overlay fronts already landed via p22 bake_machine_fronts.py
--split-fronts and are re-verified here, not re-borrowed).

Family census (NO hand-picked lists): the borrow set is the INTERSECTION of

  1. upstream NBT_TEXTURE registrations (Loader_MultiTileEntities.java),
  2. locally registered texture families, parsed from the two literal sources
     the port actually carries:
       - GTMachines.java row factories  (TD.Energy.<KIND>, "<family>",)
       - GT6BlockStates.java addMachine calls (the last string literal arg),
  3. the upstream disk tree basicmachines/<family>/ actually existing.

Per family per layer the products (the p22 split-front naming shape, faces
joining the family name, the state tail LAST -- distinct from the retired P9
oven_overlay_<state>_<face> full-strip namespace):

    <f>_colored_<face>.png           colored/<face>.png        (byte copy)
    <f>_overlay_<face>.png           overlay/<face>.png        (byte copy)
    <f>_overlay_<face>_active.png    overlay_active/<face>.png (FRAME 0 if strip)
    <f>_overlay_<face>_running.png   overlay_running/<face>.png(FRAME 0 if strip)

A plain 16x16 source is a byte-identical copy (sha256 == upstream); a 16xN
strip is cropped to FRAME 0 and re-encoded deterministically (filter 0) -- the
P20/P22 "animation stays retired" deviation carries verbatim. mcmeta files are
NOT borrowed (static representative frames only).

Domain edges fixed by the card spec:
  - engines/ (kinetic_*) colored-only per the census (the upstream code
    registers only the colored trio there); the port's steam engine already
    carries those three bytes as steam_engine_{front,back,side}.png (p12), so
    this script RE-VERIFIES the on-disk files against upstream and borrows
    nothing.
  - transformers/transformer_rotation overlay/{front,back,side} borrowed as-is
    and FLAGGED "art entity dubious" (visually all-black upstream; the single
    source of truth for whether that is intentional art is unproven).
    colored_active/overlay_active of this family stay unborrowed (the port
    transformer model has no active-state visuals; not in the card scope).

Pure standard library (zlib+struct, the bake_machine_fronts.py idiom);
decode_png/encode_png are the p22 functions VERBATIM. Deterministic, idempotent:
an existing file with identical bytes is skipped, a differing file is an ERROR
(no foreign asset is ever overwritten). Missing upstream sources print DECLARE
and keep going (borrow-or-declare).

Usage (repo root or worktree root):
  python3 mdk/tools/borrow_port_overlays.py \
      --src tmp/gt6-1.7.10            # upstream snapshot (absolute from a worktree)
      [--dst mdk/src/main/resources/assets/gt6/textures/block]
      [--check]                       # verify + census only, write nothing
"""
import argparse
import hashlib
import re
import struct
import sys
import zlib
from pathlib import Path

SIZE = 16
UPSTREAM_MACHINES = "src/main/resources/assets/gregtech/textures/blocks/machines"
LAYERS = (
    # (upstream layer dir, state tail; the output name is
    #  <fam>_<colored|overlay>_<face><_state>.png -- the p22 front shape
    #  <f>_overlay_front[_active|_running].png generalized to the side faces)
    ("colored", ""),
    ("overlay", ""),
    ("overlay_active", "_active"),
    ("overlay_running", "_running"),
)
FACES = ("bottom", "top", "left", "front", "right", "back")
# The five NON-front side faces (the front overlay fronts landed at p22 and
# carry the work-face art; this card never re-borrows a front overlay).
SIDE_FACES = ("bottom", "top", "left", "right", "back")
FRAME = 0

TRANSFORMER_FLAG = "ART-DUBIOUS"
# The upstream-relative path -> local flat-name map for the transformer overlay
# trio (front IS borrowed here: the transformer front is the INPUT face, not a
# work face -- spec: borrow as-is, flag dubious).
TRANSFORMER_OVERLAYS = (
    ("front", "transformer_rotation_overlay_front.png"),
    ("back", "transformer_rotation_overlay_back.png"),
    ("side", "transformer_rotation_overlay_side.png"),
)
# engines/kinetic_steam: the port's steam engine assets (p12) and the upstream
# colored trio they must remain byte-identical to (re-verified, never rewritten).
STEAM_ENGINE_CONFIRM = (
    ("front", "steam_engine_front.png"),
    ("back", "steam_engine_back.png"),
    ("side", "steam_engine_side.png"),
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


def sha256_bytes(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def sha256(path: Path) -> str:
    return sha256_bytes(path.read_bytes())


def local_families_from_gtmachines(path: Path) -> set[str]:
    """The row-factory texture literals: `TD.Energy.<KIND>, "<family>",`."""
    text = path.read_text(encoding="utf-8")
    return set(re.findall(r'TD\.Energy\.\w+,\s*"([a-z_0-9]+)",', text))


def local_families_from_blockstates(path: Path) -> set[str]:
    """The addMachine texture literals: last string arg of each call wins."""
    text = path.read_text(encoding="utf-8")
    fams = set()
    for m in re.finditer(
        r'addMachine\(GTMachines\.\w+\.get\(\),\s*"([a-z_0-9]+)"(?:,\s*"([a-z_0-9]+)")?\);',
        text,
    ):
        fams.add(m.group(2) or m.group(1))
    return fams


def upstream_loader_families(path: Path) -> set[str]:
    """Every `NBT_TEXTURE, "<family>"` literal in the upstream loader."""
    text = path.read_text(encoding="utf-8")
    return set(re.findall(r'NBT_TEXTURE,\s*"([a-z_0-9]+)"', text))


def land_file(args: argparse.Namespace, out_path: Path, data: bytes, label: str) -> str:
    """Write or verify one product; returns borrowed|in-place|error."""
    if out_path.exists():
        if sha256_bytes(out_path.read_bytes()) == sha256_bytes(data):
            print(f"skip {label} (already in place, byte-identical)")
            return "in-place"
        print(f"ERROR {label}: {out_path} exists with DIFFERENT bytes — "
              f"refusing to overwrite a foreign asset", file=sys.stderr)
        return "error"
    if not args.check:
        out_path.parent.mkdir(parents=True, exist_ok=True)
        out_path.write_bytes(data)
        print(f"borrowed {label}".ljust(64) + f"{SIZE}x{SIZE}  sha256={sha256_bytes(data)}  -> {out_path}")
    else:
        print(f"borrowed {label}".ljust(64) + f"{SIZE}x{SIZE}  sha256={sha256_bytes(data)}  (check-only)")
    return "borrowed"


def borrow_face(args: argparse.Namespace, src_path: Path, out_path: Path, label: str,
                want_strip_ok: bool) -> tuple[str, str | None, str]:
    """Land one face product; returns (status, landed-sha256-or-None, note)."""
    if not src_path.is_file():
        print(f"DECLARE {label}: {src_path} missing — nothing written (borrow-or-declare)")
        return "declared", None, ""
    w, h, _ = decode_png(src_path.read_bytes())
    if w != SIZE or h % SIZE != 0:
        raise ValueError(f"{label}: {src_path} is {w}x{h}, expected {SIZE}xN")
    frames = h // SIZE
    if frames == 1:
        data = src_path.read_bytes()  # byte-identical copy — no re-encode
        note = "byte copy"
    else:
        if not (0 <= FRAME < frames):
            raise ValueError(f"{label}: frame {FRAME} out of range 0..{frames - 1}")
        _, _, px = decode_png(src_path.read_bytes())
        data = encode_png(SIZE, SIZE, px[FRAME * SIZE * 4 * SIZE:(FRAME + 1) * SIZE * 4 * SIZE])
        note = f"FRAME 0 of {frames}"
        if not want_strip_ok:
            print(f"NOTE {label}: unexpected {SIZE}x{h} strip, cropping FRAME {FRAME}")
    assert decode_png(data)[:2] == (SIZE, SIZE)
    status = land_file(args, out_path, data, f"{label} [{note}]")
    return status, sha256_bytes(data), note


def main(argv: list[str]) -> int:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--src", default="tmp/gt6-1.7.10", type=Path,
                    help="upstream GT6 snapshot root (default: tmp/gt6-1.7.10)")
    ap.add_argument("--dst", type=Path,
                    default=Path("mdk/src/main/resources/assets/gt6/textures/block"))
    ap.add_argument("--check", action="store_true",
                    help="census + verify without writing")
    args = ap.parse_args(argv[1:])

    gtm = Path("mdk/src/main/java/gregtech6/registry/GTMachines.java")
    bst = Path("mdk/src/main/java/gregtech6/datagen/GT6BlockStates.java")
    loader = args.src / "src/main/java/gregtech/loaders/b/Loader_MultiTileEntities.java"
    machines_dir = args.src / UPSTREAM_MACHINES
    for p in (gtm, bst, loader, machines_dir, args.dst):
        if not p.exists():
            print(f"ERROR: required path missing: {p}", file=sys.stderr)
            return 2

    # --- the census: three-way intersection, nothing hand-picked ---
    local = local_families_from_gtmachines(gtm) | local_families_from_blockstates(bst)
    upstream = upstream_loader_families(loader)
    basicmachines_dir = machines_dir / "basicmachines"
    on_disk = {p.name for p in basicmachines_dir.iterdir() if p.is_dir()}
    families = sorted(local & upstream & on_disk)
    print("CENSUS local registered texture families :", " ".join(sorted(local)))
    print(f"CENSUS upstream NBT_TEXTURE count          : {len(upstream)}")
    print("CENSUS upstream basicmachines dirs         :", len(on_disk))
    print(f"CENSUS borrow set = local ∩ upstream ∩ disk: {len(families)} families")
    print("CENSUS borrow set                          :", " ".join(families))
    print(f"CENSUS expected products                   : {len(families)} families "
          f"x (colored 6 + 3 states x 5 sides) = {len(families) * 21} PNG")
    print()

    totals = {"borrowed": 0, "in-place": 0, "declared": 0, "error": 0}
    manifest: list[str] = []  # (out_name, upstream_rel, note, sha256)

    for fam in families:
        fam_dir = machines_dir / "basicmachines" / fam
        for layer, tail in LAYERS:
            faces = FACES if layer == "colored" else SIDE_FACES
            base = layer.split("_")[0]  # colored | overlay (the output infix)
            for face in faces:
                out_name = f"{fam}_{base}_{face}{tail}.png"
                src_path = fam_dir / layer / f"{face}.png"
                status, digest, note = borrow_face(
                    args, src_path, args.dst / out_name,
                    f"{fam}/{layer}/{face}", want_strip_ok=True)
                totals[status] += 1
                if digest:
                    manifest.append((out_name, f"basicmachines/{fam}/{layer}/{face}.png", note, digest))

    # --- transformer_rotation overlay trio (front/back/side), flagged dubious ---
    print()
    trans_dir = machines_dir / "transformers" / "transformer_rotation"
    for face, out_name in TRANSFORMER_OVERLAYS:
        src_path = trans_dir / "overlay" / f"{face}.png"
        status, digest, note = borrow_face(args, src_path, args.dst / out_name,
                                           f"transformer_rotation/overlay/{face}", want_strip_ok=True)
        totals[status] += 1
        if digest:
            manifest.append((out_name, f"transformers/transformer_rotation/overlay/{face}.png",
                             f"{note} [{TRANSFORMER_FLAG}: all-black art, entity dubious]", digest))

    # --- engines/kinetic_steam: colored-only domain, re-verify the p12 trio ---
    print()
    steam_dir = machines_dir / "engines" / "kinetic_steam" / "colored"
    for face, local_name in STEAM_ENGINE_CONFIRM:
        local_path = args.dst / local_name
        src_path = steam_dir / f"{face}.png"
        if not src_path.is_file():
            print(f"DECLARE engines/kinetic_steam/colored/{face}: upstream missing")
            totals["declared"] += 1
            continue
        if not local_path.is_file():
            print(f"DECLARE {local_name}: on-disk asset missing (p12 steam engine borrow absent)")
            totals["declared"] += 1
            continue
        if sha256(local_path) == sha256(src_path):
            print(f"CONFIRM engines kinetic_steam colored/{face} == {local_name} "
                  f"(sha256 {sha256(local_path)[:16]}…) — colored-only domain, nothing to borrow")
        else:
            print(f"ERROR {local_name} differs from upstream engines/kinetic_steam/colored/{face}.png",
                  file=sys.stderr)
            totals["error"] += 1

    # --- summary ---
    print()
    print(f"summary: borrowed={totals['borrowed']} in-place={totals['in-place']} "
          f"declared={totals['declared']} errors={totals['error']}")
    if totals["declared"]:
        print("declared entries need manual triage (see DECLARE lines above)")
    if totals["error"]:
        return 1
    if not args.check:
        print()
        print("manifest (paste into mdk/src/main/resources/assets/README.md):")
        for out_name, up_rel, note, digest in manifest:
            print(f"  - `{out_name}`  `{digest}`  ({note}; upstream `{up_rel}`)")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
