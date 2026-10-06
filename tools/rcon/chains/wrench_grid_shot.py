#!/usr/bin/env python3
"""wrench-grid-shot — the symptom19 "扳手九宫格真弹" live visual record (task
wrench-interaction-chain; the id1345 lesson: interaction symptoms need in-vivo
evidence, and the grid is CLIENT render — the RCON click chain covers the click
half, this module owns the display half).

Structure = the embeddium_tint precedent, minimally varied:

  server half (Chain, passes=1, NO teardown — the machines are the fixture):
    A the smooth_stone stage + pinned lighting (noon, daylight locked, clear);
    B the family row at z=64 facing the spawn camera: electric_transformer at
      x480, battery_box_ulv at x483 (both FACING-driven family members);
    C the camera pin: setworldspawn 480 64 58 — fresh client player at the block
      centre facing yaw=0 (+Z south), the row 6 blocks due south.

  client half (--client): the world copy -> dev client (quickPlaySingleplayer,
  the P26 smoke automation boundary) -> chat /give the formal wrench -> F2 shot
  (grid armed) -> chat /clear -> F2 shot (grid disarmed) -> pixel verdict: the
  grid lines draw BLUE-dominant (GTWrenchGridRenderer.drawGridLines: B=1.0
  pulse, R=G=0.2..0.7), so the armed-vs-disarmed blue-line delta in the centre
  region IS the "UI 真弹" evidence.

Run:
  GT6_SESSION=off python3 tools/rcon/chains/wrench_grid_shot.py          # stage
  python3 tools/rcon/chains/wrench_grid_shot.py --client                 # shoot
"""

import argparse
import os
import shutil
import subprocess
import sys
import time
from pathlib import Path

_HERE = Path(__file__).resolve().parent
for _path in (str(_HERE), str(_HERE.parent)):
    if _path not in sys.path:
        sys.path.insert(0, _path)

import gt6world
from framework import Chain, Step, main, phase

# the fresh x 480..486 z 124 band's neighbour column z 58..67 (z-disjoint from the
# z=124 band; the tint chain's z=64 stage sits at x 449..455 — x-disjoint from 476..484)
G = gt6world.Site(480, 64, 62, dx=4, dy=1, dz=6)

steps = []

# ------------------------------------------- A: the stage + the pinned lighting
steps += [
    phase("A: the smooth_stone stage under the family row (the flat world ground sits at y=3)"),
    Step("fill 476 63 58 484 63 67 minecraft:smooth_stone", expect="Successfully filled"),
    Step("time set noon", expect="Set the time to"),
    Step("gamerule doDaylightCycle false", expect="doDaylightCycle"),
    Step("weather clear", expect="weather"),
]

# ------------------------------------------- B: the family row (fronts south)
steps += [
    phase("B: the facing-machine family row — transformer + battery box, spawn-facing"),
    Step("setblock 480 64 64 gt6:electric_transformer", expect="Changed the block"),
    Step("setblock 483 64 64 gt6:battery_box_ulv", expect="Changed the block"),
]

# ------------------------------------------- C: the camera pin (spawn = the rig)
steps += [
    phase("C: the camera pin — fresh players spawn facing +Z, the row 6 blocks south"),
    Step("setworldspawn 480 64 58", expect="Set the world spawn"),
    Step("gamerule spawnRadius 0", expect="spawnRadius"),
]

CHAIN = Chain(
    name="wrench-grid-shot",
    slug="wrenchgridshot",
    sites=gt6world.declare_sites(G),
    preferred_ports=(26822, 26832),     # this card's second pinned pair (after 26802/26812)
    steps=steps,
    passes=1,
)


# ---------------------------------------------------------------------------
# The client half: world copy -> dev-client two shots -> the pixel verdict.
# ---------------------------------------------------------------------------

REPO = _HERE.parent.parent.parent            # tools/rcon/chains → repo root
NODE_RUN = REPO / "mdk" / "versions" / "1.20.1-forge" / "run"
WORLD_NAME = "wrenchgrid"

OPTIONS = (
    "version:4105\n"
    "onboardAccessibility:false\n"    # 1.20 first-launch dialog would block quickPlay
    "pauseOnLostFocus:false\n"        # a focus flicker must not darken the shot
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


def _server_world_dir():
    import gt6server
    candidates = [Path(gt6server.run_dir(str(REPO))), Path(gt6server.run_dir(str(REPO), node="1.20.1-forge"))]
    for cand in candidates:
        world = Path(cand) / "world"
        if world.is_dir():
            return world
    raise SystemExit(f"server world folder not found under {candidates} — run the chain first")


def _chat(xenv: dict, line: str) -> None:
    """One chat command through XTEST (t opens chat, type, Return sends)."""
    subprocess.run(["xdotool", "key", "--clearmodifiers", "t"], env=xenv, check=False)
    time.sleep(1.2)
    subprocess.run(["xdotool", "type", "--delay", "70", "--", line], env=xenv, check=False)
    time.sleep(0.6)
    subprocess.run(["xdotool", "key", "--clearmodifiers", "Return"], env=xenv, check=False)
    time.sleep(1.5)


def _blue_lines(png: Path) -> int:
    """The grid-line pixel count: B-dominant lines (B=1.0 pulse, R=G lower and equal)
    in the centre region — the sky is blue with HIGH green, the machine row is
    bronze/brown, so |R-G| tight + B-G wide separates the drawn lines."""
    from PIL import Image
    img = Image.open(png).convert("RGB")
    px = img.load()
    count = 0
    for y in range(100, 360):
        for x in range(160, 480):
            r, g, b = px[x, y]
            if b > 180 and b - g > 70 and abs(r - g) < 30:
                count += 1
    return count


def cmd_client(wait_seconds: float = 60.0) -> int:
    """One dev-client leg: join the copied world, give the wrench, two F2 shots."""
    xvfb = os.environ.get("GT6_XVFB_DISPLAY", ":97")   # the resident Xvfb, never :0
    src = _server_world_dir()
    dest = NODE_RUN / "saves" / WORLD_NAME
    if dest.exists():
        shutil.rmtree(dest)
    dest.parent.mkdir(parents=True, exist_ok=True)
    shutil.copytree(src, dest)
    NODE_RUN.mkdir(parents=True, exist_ok=True)
    (NODE_RUN / "options.txt").write_text(OPTIONS)
    shot_dir = NODE_RUN / "screenshots"
    shot_dir.mkdir(parents=True, exist_ok=True)
    before = {p.name for p in shot_dir.glob("*.png")}

    gradle_flags = ["-Pgt6.nojade=true", f"-Pgt6.display={xvfb}"]
    args = f"--quickPlaySingleplayer {WORLD_NAME} --width 640 --height 360"
    cmd = ["./gradlew", ":mdk:1.20.1-forge:runClient", f"-Pgt6.quickplay={args}"] + gradle_flags
    print(f"[client] $ {' '.join(cmd)}")
    env = dict(os.environ)
    env.setdefault("LIBGL_ALWAYS_SOFTWARE", "1")   # llvmpipe, the same GL the tint legs used
    proc = subprocess.Popen(cmd, cwd=str(REPO), env=env,
                            stdout=open(NODE_RUN / "client_wrenchgrid.log", "w"),
                            stderr=subprocess.STDOUT)
    xenv = dict(os.environ, DISPLAY=xvfb)
    try:
        log_path = NODE_RUN / "client_wrenchgrid.log"
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
        print("[client] world joining...")
        time.sleep(wait_seconds)                  # spawn + the llvmpipe chunk build

        # the pointer-capture jump at world join injects downward pitch; lift back
        # (mouse up = negative dy) — the embeddium legs' calibration, same camera pin shape
        subprocess.run(["xdotool", "mousemove_relative", "--", "0", "-58"], env=xenv, check=False)
        time.sleep(4.0)

        # shot 1: wrench held — the grid must draw on the transformer/battery row
        _chat(xenv, "/give @s gt6:wrench")
        time.sleep(2.0)
        subprocess.run(["xdotool", "key", "--clearmodifiers", "F2"], env=xenv, check=False)
        shot_armed = _wait_new_shot(shot_dir, before, "armed")
        before |= {shot_armed.name}

        # shot 2: empty hands — the grid must hide (the shown-means-clickable gate)
        _chat(xenv, "/clear @s")
        time.sleep(2.0)
        subprocess.run(["xdotool", "key", "--clearmodifiers", "F2"], env=xenv, check=False)
        shot_disarmed = _wait_new_shot(shot_dir, before, "disarmed")

        armed_png = NODE_RUN / "shot_wrenchgrid_armed.png"
        disarmed_png = NODE_RUN / "shot_wrenchgrid_disarmed.png"
        shutil.copyfile(shot_armed, armed_png)
        shutil.copyfile(shot_disarmed, disarmed_png)
        t_armed = _blue_lines(armed_png)
        t_disarmed = _blue_lines(disarmed_png)
        print(f"[client] blue-line pixels: armed={t_armed} disarmed={t_disarmed}")
        if t_armed <= t_disarmed or t_armed < 40:
            print("[client] VERDICT FAILED — the armed shot shows no grid-line blue delta")
            return 1
        print("[client] VERDICT OK — the 3x3 grid drew (UI 真弹 in-vivo evidence)")
        return 0
    finally:
        proc.terminate()
        try:
            proc.wait(timeout=30)
        except subprocess.TimeoutExpired:
            proc.kill()
        subprocess.run(["pkill", "-f", f"forgeclientuserdev.*{REPO}"], check=False)
        time.sleep(2.0)


def _wait_new_shot(shot_dir: Path, before: set, tag: str) -> Path:
    for _ in range(10):
        time.sleep(2.0)
        new = sorted({p.name for p in shot_dir.glob("*.png")} - before)
        if new:
            print(f"[client] screenshot {tag}: {new[-1]}")
            return shot_dir / new[-1]
    raise SystemExit(f"F2 produced no {tag} screenshot; check the client log")


if __name__ == "__main__":
    tParser = argparse.ArgumentParser()
    tParser.add_argument("--client", action="store_true", help="the dev-client shoot leg")
    tArgs = tParser.parse_args()
    if tArgs.client:
        sys.exit(cmd_client())
    main(CHAIN)
