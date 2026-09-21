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

# the camera tp: feet back on the platform plane, 3 blocks off the south wall —
# loader-common (the stage Y is common; the earlier hardcoded y=5 tp was the 1.20.1
# dialect and stranded the 21.1 camera 64 blocks above the fixture).
CAMERA_TP = "tp 387 16 518"


def _node_run(node):
    return REPO / "mdk" / "versions" / node / "run"


def _server_world_dir(node):
    import gt6server
    cand = Path(gt6server.run_dir(str(REPO), node=node))
    world = cand / "world"
    if world.is_dir():
        return world
    raise SystemExit(f"server world folder not found under {cand} — run the chain first (node {node})")


def cmd_world_copy(node: str) -> int:
    """Copy the staged server world into the node's dev client saves/."""
    src = _server_world_dir(node)
    dest = _node_run(node) / "saves" / WORLD_NAME
    if dest.exists():
        shutil.rmtree(dest)
    dest.parent.mkdir(parents=True, exist_ok=True)
    shutil.copytree(src, dest)
    print(f"[p33-client] world copied: {src} -> {dest}")
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

    The fresh spawn stands at the camera pin on the platform facing +Z (south) — the
    ore wall is 5 blocks south, in frame; the join pitch-jump correction, the chat-arm
    re-asserts and the F2 screenshot ride the p32_embeddium_tint form verbatim.
    """
    xvfb = os.environ.get("GT6_XVFB_DISPLAY", ":97")   # the resident Xvfb, never :0
    run = _node_run(node)
    shot_dir = run / "screenshots"
    shot_dir.mkdir(parents=True, exist_ok=True)
    before = {p.name for p in shot_dir.glob("*.png")}
    _seed_options(node)

    args = f"--quickPlaySingleplayer {WORLD_NAME} --width 640 --height 360"
    cmd = ["./gradlew", f":mdk:{node}:runClient", f"-Pgt6.quickplay={args}",
           f"-Pgt6.display={xvfb}", "-Pgt6.nojade=true"]
    print(f"[p33-client:{node}] $ {' '.join(cmd)}")
    env = dict(os.environ)
    env.setdefault("LIBGL_ALWAYS_SOFTWARE", "1")   # same GL for both nodes (llvmpipe)
    proc = subprocess.Popen(cmd, cwd=str(REPO), env=env,
                            stdout=open(run / "client_p33.log", "w"),
                            stderr=subprocess.STDOUT)
    xenv = dict(os.environ, DISPLAY=xvfb)
    try:
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
        time.sleep(55.0)                      # spawn + the llvmpipe chunk build

        # the join can land on the DEATH screen (a fall off the platform edge); the
        # Respawn button sits mid-frame at 640x360. The click is harmless in-world
        # (a click into the scene) and respawn returns to the platform spawn.
        subprocess.run(["xdotool", "mousemove", "320", "170", "click", "1"],
                       env=xenv, check=False)
        time.sleep(4.0)

        # Deterministic camera + safe world state, via chat (singleplayer commands are
        # enabled by default in the dev client). Chat opens EMPTY on 't' — the '/'
        # key opens it PRE-FILLED with '/', so typing "/tp ..." there would run
        # "//tp ..." (the invalid double-slash; the 15:21 leg's invisible tp). Noon
        # and peaceful are already staged (the chain) — these are the belt-and-
        # suspenders re-asserts; the tp is the load-bearing camera pin. The pointer-
        # capture pitch lift follows the tp.
        for command in ("difficulty peaceful", "time set noon", CAMERA_TP):
            subprocess.run(["xdotool", "key", "--clearmodifiers", "t"], env=xenv, check=False)
            time.sleep(1.0)
            subprocess.run(["xdotool", "type", "--delay", "40", "/" + command], env=xenv, check=False)
            time.sleep(0.5)
            subprocess.run(["xdotool", "key", "--clearmodifiers", "Return"], env=xenv, check=False)
            time.sleep(2.0)
        subprocess.run(["xdotool", "mousemove_relative", "--", "0", "-58"],
                       env=xenv, check=False)
        time.sleep(10.0)                      # the tp + the chunk build under the new view

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
