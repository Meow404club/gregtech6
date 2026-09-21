#!/usr/bin/env python3
"""p33_ore_overlay — the ore OVERLAY baked-model live chain (task p33-ore-overlay-impl).

The 21.1 leg's live-verification face for the ore dual-sprite baked model: the chain
stages a row of representative GT6 ore blocks on a platform (the registration
mechanism and the offline quad contract are cards p30/p32's faces — GT6OreRenderDatagenTest
+ GTOreOverlay211SeamTest own those offline); this chain's job is the SERVER-side
placement matrix + the CLIENT-side render fixture (the p32_embeddium_tint form):
the world persists (passes=1) so the dev-client screenshot legs photograph the ore row
on both loader nodes — the baked-model dispatch is client-only code, so the RCON
server can only stage the scene, never judge pixels.

  A the stage — a stone platform slab at y=15 carrying the whole fixture, plus the
    pinned lighting (noon, locked, clear, peaceful). The stage Y is LOADER-COMMON:
    both grounds (1.20.1 flat top y=3, 1.21.1 flat top y=-61) sit far below the
    cleanup region (y11..21), so the pass-open bbox air-fill touches only air on
    both legs — no ground carve, no void hole, and no per-node coordinate dialect
    (the earlier per-node Y fork died here: a static declared Site cannot cover a
    fork that spans two ground planes, and the union region would carve bedrock).
  B the ore placement matrix — the four representative cells (one per
    sprite-derivation shape of GTOreBakedModel: stone normal / deepslate normal /
    second-SET normal / small) with `execute if block` pins (the p29_w6_ore_census
    cell form), plus the enclosing 3-high walls (broken-form left wing, deepslate
    right wing, and the three back/side walls) so a wall fills the frame whatever
    the quickPlay join yaw (the 15:12-15:28 empty-frame lesson).
  C the camera pin — setworldspawn on the platform facing yaw 0 (+Z, south), the
    wall 5 blocks south, the p32_embeddium_tint form.

passes=1 (the ore row is the client fixture — the world folder is copied into the dev
client's saves/ after the server stops; a second pass would rebuild it, safe but useless).

Run:  python3 tools/rcon/chains/p33_ore_overlay.py [--node 1.21.1-neoforge]

The client screenshot legs (after the chain, per node):
  python3 tools/rcon/chains/p33_ore_overlay.py --world-copy --node 1.21.1-neoforge
  python3 tools/rcon/chains/p33_ore_overlay.py --client --node 1.21.1-neoforge
"""

import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
for _path in (str(_HERE), str(_HERE.parent)):
    if _path not in sys.path:
        sys.path.insert(0, _path)

import gt6world
from framework import Chain, Step, main, phase

# The site — a fresh z=508..530 band (the roster census tops out at z=475), x377..397.
# One Site carries the whole fixture on BOTH legs (the loader-common stage): bounds
# y13..19 cover the platform (15) and the walls (16..18); the derived region
# (x375..399, y11..21, z508..530 = 6325 blocks, one fill) is pure air on both legs.
G = gt6world.Site(387, 16, 519, dx=10, dy=3, dz=9)

# The four representative cells (the sprite-derivation shapes of GTOreBakedModel.baseSpriteOf
# /overlaySpriteOf): stone-normal (vanilla stone base), deepslate-normal, second-SET normal
# (the params-table dispatch walk), small (the ore_small overlay path). NOTE: all PIN cells
# use NON-dropping forms (the broken form's setblock+destroy drops the broken pair and
# leaves air — the p29_w6_ore_census break-drop semantics; the broken BASE still rides
# the wall fills below, which pin nothing).
CELLS = [
    (386, "gt6:ore_stone_copper", "stone normal — vanilla stone base + ore sprite"),
    (387, "gt6:ore_deepslate_copper", "deepslate normal — deepslate base"),
    (388, "gt6:ore_stone_cassiterite", "stone normal, second SET — the params-table dispatch walk"),
    (389, "gt6:ore_small_stone_copper", "small — ore_small overlay path"),
]

steps = []

# ------------------------------------------------------------- A: the stage
steps += [
    phase("A: the stone platform under the fixture (loader-common y=15, both grounds "
          "far below the cleanup region) + the pinned lighting (noon, locked, clear, "
          "peaceful — peaceful persists in level.dat so the client legs join into a "
          "slime-free world; the 15:00-15:12 death-screen lesson)"),
    Step("fill 377 15 511 397 15 528 minecraft:stone", expect="filled"),
    Step("time set noon", expect="Set the time to"),
    Step("gamerule doDaylightCycle false", expect="doDaylightCycle"),
    Step("weather clear", expect="weather"),
    Step("difficulty peaceful", expect="Peaceful"),
]

# --------------------------------------------------- B: the ore placement matrix
# the visible fixture: a WALL of representative ores — a wall reads even when the
# spawn drifts a block or two; the four block-id pins re-assert the four
# sprite-derivation cells.
steps += [phase("B: the ore wall + the four representative block-id pins")]
for x, block, label in CELLS:
    steps += [
        # clear first: the world persists across passes (no fresh_boot) and vanilla
        # setblock reports "Could not set the block" on a no-change replace — the
        # census chain's clear-then-place form; the clear itself may already be air
        # (the no-change shape again), so the clear judge is allow_failed and the
        # STRICT expectations ride the place + the block-id pin below
        Step(f"setblock {x} 16 521 air", expect="Changed the block", allow_failed=True),
        Step(f"setblock {x} 16 521 {block}", expect="Changed the block"),
        Step(f"execute if block {x} 16 521 {block}", expect="Test passed"),
    ]
# the bulk walls: 3-HIGH (16..18 — the player's feet land at 16, eye 17.62, so a
# 3-high wall fills the frame at any pitch; the 16:01 neo frame's readable wall was
# exactly this shape) — broken-form ores (distinct cobble base) on the left wing,
# deepslate normals on the right wing — the two base families the four pinned cells
# cover. The spawn YAW is not trustworthy in the quickPlay join (the empty-frame legs
# of 15:12-15:28 — the player faces away from the south wall), so the fixture encloses
# the spawn cell on ALL FOUR sides: whatever the join yaw, a wall fills the frame.
steps += [
    Step("fill 380 16 521 384 18 521 gt6:ore_broken_stone_copper", expect="filled"),
    Step("fill 390 16 521 394 18 521 gt6:ore_deepslate_copper", expect="filled"),
    Step("fill 380 16 511 394 18 511 gt6:ore_stone_copper", expect="filled"),
    Step("fill 377 16 511 377 18 521 gt6:ore_deepslate_copper", expect="filled"),
    Step("fill 397 16 511 397 18 521 gt6:ore_stone_copper", expect="filled"),
]

# ---------------------------------------------------------- C: the camera pin
steps += [
    phase("C: the camera pin — the fresh spawn stands on the platform facing yaw 0 "
          "(+Z, south), the wall 5 blocks south"),
    Step("setworldspawn 387 16 516", expect="Set the world spawn"),
    Step("gamerule spawnRadius 0", expect="spawnRadius"),
]

# NOTE: no teardown phase — the ore row is the client screenshot fixture.

CHAIN = Chain(
    name="p33-ore-overlay",
    slug="p33_ore_overlay",
    sites=gt6world.declare_sites(G),
    preferred_ports=(26632, 26642),     # this card's pinned rcon/query pair (P33 segment)
    steps=steps,
    passes=1,
)


# ---------------------------------------------------------------------------
# The client half: world copy → dev-client screenshot leg (the p32_embeddium_tint
# client-arm form, node-parameterized). The RENDER ASSERTION is the screenshot leg
# itself: the ore row must show the dual-sprite look (stone base + tinted ore
# overlay) in the frame — the baked-model dispatch is client-only code, the server
# cannot judge it; the deterministic offline contract lives in
# GTOreOverlay211SeamTest + GT6OreRenderDatagenTest.
# ---------------------------------------------------------------------------

import argparse
import os
import shutil
import subprocess
import time

REPO = _HERE.parent.parent.parent            # tools/rcon/chains → repo root
WORLD_NAME = "p33oreoverlay"

# The camera pin, written straight into the copied world's playerdata: the client
# RESUMES the local player at the saved Pos/Rotation, so the leg needs no chat, no
# clicks, no pitch correction (the chat arm is retired here: both 17:1x legs' typed
# commands died as "Unknown or incomplete command" — xdotool typing against GLFW is
# not trustworthy; a saved rotation is byte-deterministic).
#   yaw 0 = +Z (south, toward the wall at z521), pitch 15 = slightly down — the eye
#   at 17.62 puts the 4.5-block-away 3-high wall mid-frame.
CAMERA_POS = (387.5, 16.0, 516.5)
CAMERA_ROT = (0.0, 15.0)


def _node_run(node):
    return REPO / "mdk" / "versions" / node / "run"


def _server_world_dir(node):
    import gt6server
    cand = Path(gt6server.run_dir(str(REPO), node=node))
    world = cand / "world"
    if world.is_dir():
        return world
    raise SystemExit(f"server world folder not found under {cand} — run the chain first (node {node})")


def _patch_camera(dest):
    """Overwrite the local player's saved Pos/Rotation with the camera pin.

    Same-byte-length NBT surgery on the decompressed buffer: Pos is TAG_LIST(9)
    (elem type + count + 3 big-endian doubles), Rotation the same shape with 2
    big-endian floats. The singleplayer client RESUMES this player, so the join
    lands exactly at the pin with the exact aim — no in-game command surface.
    """
    import glob
    import struct
    import gzip as _gzip
    dats = glob.glob(str(dest / "playerdata" / "*.dat"))
    if not dats:
        raise SystemExit(f"no playerdata under {dest}/playerdata — join the server chain world once first")
    raw = bytearray(_gzip.decompress(open(dats[0], "rb").read()))
    i = raw.find(b"Pos")
    if i < 0:
        raise SystemExit("Pos tag not found in the player dat")
    j = i + len(b"Pos") + 5          # skip the elem-type byte and the 4-byte count
    raw[j:j+24] = struct.pack(">ddd", *CAMERA_POS)
    k = raw.find(b"Rotation")
    if k < 0:
        raise SystemExit("Rotation tag not found in the player dat")
    j = k + len(b"Rotation") + 5
    raw[j:j+8] = struct.pack(">ff", *CAMERA_ROT)
    open(dats[0], "wb").write(_gzip.compress(raw))
    print(f"[p33-client] camera pinned in {dats[0]}: pos={CAMERA_POS} rot(yaw,pitch)={CAMERA_ROT}")


def cmd_world_copy(node: str) -> int:
    """Copy the staged server world into the node's dev client saves/."""
    src = _server_world_dir(node)
    dest = _node_run(node) / "saves" / WORLD_NAME
    # salvage the local player's dat from the previous copy (the dedicated server
    # world never has playerdata — only a played singleplayer session writes it):
    # it is the resume template the camera patch overwrites below. Without it, a
    # fresh copy spawns a NEW player at the world spawn — the same pin, yaw 0,
    # pitch 0 (level view), which also frames the wall.
    salvaged = None
    if dest.exists():
        old = sorted((dest / "playerdata").glob("*.dat"))
        if old:
            salvaged = old[0].read_bytes()
        shutil.rmtree(dest)
    dest.parent.mkdir(parents=True, exist_ok=True)
    shutil.copytree(src, dest)
    if salvaged is not None:
        pd = dest / "playerdata"
        pd.mkdir(exist_ok=True)
        (pd / "380df991-f603-344c-a090-369bad2a924a.dat").write_bytes(salvaged)
    print(f"[p33-client] world copied: {src} -> {dest}")
    if any((dest / "playerdata").glob("*.dat")):
        _patch_camera(dest)
    else:
        # fresh worktree, no salvaged dat: the chain's `setworldspawn 387 16 516` +
        # `spawnRadius 0` already ARE the camera pin — a brand-new player spawns at
        # the pin facing the south wall at yaw 0/pitch 0 (this function's own doc),
        # which frames the wall identically. No playerdata, nothing to patch.
        print("[p33-client] no playerdata in the copy — the world-spawn pin stands "
              "(yaw 0/pitch 0 frames the wall)")
    return 0


def _seed_options(node: str) -> None:
    """Deterministic client options (the p32 form — both nodes byte-identical config)."""
    run = _node_run(node)
    run.mkdir(parents=True, exist_ok=True)
    (run / "options.txt").write_text(
        "version:4105\n"
        "onboardAccessibility:false\n"
        "pauseOnLostFocus:false\n"
        "renderDistance:8\n"
        "gamma:1.0\n"
        "guiScale:2\n"
        "fullscreen:false\n"
        "fov:0.0\n"
        "clouds:false\n"
        "graphicsMode:0\n"
        "ambientOcclusion:2\n"
        "biomeBlendRadius:0\n"
    )
    print(f"[p33-client] options seeded: {run / 'options.txt'}")


def cmd_client(node: str, wait_seconds: float = 45.0) -> int:
    """One dev-client leg: quickPlay into the copied world, screenshot, terminate.

    The player RESUMES at the pinned camera (the world-copy patched Pos/Rotation),
    facing the wall — the leg is join, settle, F2. The pointer is centred BEFORE
    launch so the mouse-capture grab injects no aim jump (the jump is the pointer
    offset at grab time; a centred pointer has none).
    """
    xvfb = os.environ.get("GT6_XVFB_DISPLAY", ":97")   # the resident Xvfb, never :0
    run = _node_run(node)
    shot_dir = run / "screenshots"
    shot_dir.mkdir(parents=True, exist_ok=True)
    before = {p.name for p in shot_dir.glob("*.png")}
    _seed_options(node)
    xenv = dict(os.environ, DISPLAY=xvfb)

    args = f"--quickPlaySingleplayer {WORLD_NAME} --width 640 --height 360"
    cmd = ["./gradlew", f":mdk:{node}:runClient", f"-Pgt6.quickplay={args}",
           f"-Pgt6.display={xvfb}", "-Pgt6.nojade=true"]
    print(f"[p33-client:{node}] $ {' '.join(cmd)}")
    env = dict(os.environ)
    env.setdefault("LIBGL_ALWAYS_SOFTWARE", "1")   # same GL for both nodes (llvmpipe)
    proc = subprocess.Popen(cmd, cwd=str(REPO), env=env,
                            stdout=open(run / "client_p33.log", "w"),
                            stderr=subprocess.STDOUT)
    try:
        # centre the pointer on the 640x360 window region BEFORE the game grabs it
        subprocess.run(["xdotool", "mousemove", "--sync", "320", "180"],
                       env=xenv, check=False)
        log_path = run / "client_p33.log"
        markers = ("Starting integrated minecraft server version", "Preparing spawn area")
        joined = False
        deadline = time.time() + 900.0
        while time.time() < deadline:
            time.sleep(5.0)
            if proc.poll() is not None:
                raise SystemExit(f"client died early (exit {proc.returncode}); see {log_path}")
            try:
                text = log_path.read_text(errors="ignore")
            except OSError:
                continue
            if any(m in text for m in markers):
                joined = True
                break
        if not joined:
            raise SystemExit("world join marker never appeared; see the client log")
        print(f"[p33-client:{node}] world joining...")
        time.sleep(60.0)                      # spawn + the llvmpipe chunk build

        # F2 = the vanilla screenshot key, delivered through XTEST to the focused window
        subprocess.run(["xdotool", "key", "--clearmodifiers", "F2"], env=xenv, check=False)
        for _ in range(10):
            time.sleep(2.0)
            new = sorted({p.name for p in shot_dir.glob("*.png")} - before)
            if new:
                break
        else:
            raise SystemExit("F2 produced no screenshot; see the client log")
        src = shot_dir / new[-1]
        dest = run / "shot_p33_ore_overlay.png"
        shutil.copyfile(src, dest)
        print(f"[p33-client:{node}] screenshot: {src} -> {dest}")
        return 0
    finally:
        proc.terminate()
        try:
            proc.wait(timeout=30)
        except subprocess.TimeoutExpired:
            proc.kill()
        # reap the orphan game (a daemon child, not ours — the p32 form)
        subprocess.run(["pkill", "-f", f"(forgeclientuserdev|neoforgeclientuserdev).*{REPO}"],
                       check=False)
        time.sleep(2.0)


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--node", default="1.20.1-forge", help="stonecutter node")
    parser.add_argument("--world-copy", action="store_true",
                        help="copy the staged server world into the node's dev client saves/")
    parser.add_argument("--client", action="store_true",
                        help="one dev-client screenshot leg over the copied world")
    args = parser.parse_args()
    if args.world_copy:
        sys.exit(cmd_world_copy(args.node))
    if args.client:
        sys.exit(cmd_client(args.node))
    main(CHAIN)
