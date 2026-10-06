#!/usr/bin/env python3
"""wrench-grid-shot — the symptom19 "扳手九宫格真弹" live visual record (task
wrench-interaction-chain; the id1345 lesson: interaction symptoms need in-vivo
evidence, and the grid is CLIENT render — the RCON click chain covers the click
half, this module owns the display half).

Structure = the embeddium_tint precedent, minimally varied:

  server half (Chain, passes=1, NO teardown — the machines are the fixture):
    A the smooth_stone stage + pinned lighting (noon, daylight locked, clear);
    B the family at z=64 facing the camera: a 3x2 electric_transformer WALL at
      x479..481 (the client crosshair drifts under Xvfb pointer capture; the wall
      keeps the ray on the family) + battery_box_ulv at x483;
    C the camera pin: setworldspawn 480 64 61 — the seeded player stands 3 blocks
      south of the wall, INSIDE the 4.5 survival reach (the grid is a targeted-
      block overlay; at the first run's 6-block rig the event never fired).

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
import gzip
import io
import os
import shutil
import struct
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
    phase("B: the facing-machine family — a 3x2 transformer WALL + the battery box; "
          "the wall absorbs the client's pointer-capture yaw drift (the grid is a "
          "TARGETED-block overlay — the camera must ray-hit the family)"),
    Step("fill 479 64 64 481 65 64 gt6:electric_transformer", expect="Successfully filled"),
    Step("setblock 483 64 64 gt6:battery_box_ulv", expect="Changed the block"),
]

# ------------------------------------------- C: the camera pin (spawn = the rig)
steps += [
    phase("C: the camera pin — the seeded player ignores worldspawn, this is the "
          "unseeded-player backstop at the same 3-block rig"),
    Step("setworldspawn 480 64 61", expect="Set the world spawn"),
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
# The wrench reaches the hotbar through playerdata surgery, NOT chat: two live
# runs proved XTEST-typed /give unreachable (burst `xdotool type` garbled the
# command under llvmpipe stutter, and per-key delivery plus the pre-Escape only
# bought a command-error chat line and a Game Menu shot). Single-key XTEST
# (F2, digit hotbar) stays — the embeddium precedent's proven surface.
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


_DEV_UUID = "380df991-f603-344c-a090-369bad2a924a"   # md5("OfflinePlayer:Dev") v3, the dev env's fixed player


def _nbt_string(b: bytes, o: int):
    n, o = struct.unpack_from(">H", b, o)[0], o + 2
    return b[o:o + n].decode("utf-8"), o + n


def _nbt_payload(b: bytes, o: int, t: int):
    """Payload of tag type t at b[o:] -> (python value, next offset).
    Compound = dict (insertion-ordered), List = (elem_type, [values])."""
    if t == 1: return b[o], o + 1
    if t == 2: return struct.unpack_from(">h", b, o)[0], o + 2
    if t == 3: return struct.unpack_from(">i", b, o)[0], o + 4
    if t == 4: return struct.unpack_from(">q", b, o)[0], o + 8
    if t == 5: return struct.unpack_from(">f", b, o)[0], o + 4
    if t == 6: return struct.unpack_from(">d", b, o)[0], o + 8
    if t == 7:
        n, o = struct.unpack_from(">i", b, o)[0], o + 4
        return b[o:o + n], o + n
    if t == 8: return _nbt_string(b, o)
    if t == 9:
        et = b[o]; o += 1
        n, o = struct.unpack_from(">i", b, o)[0], o + 4
        vals = []
        for _ in range(n):
            v, o = _nbt_payload(b, o, et)
            vals.append(v)
        return (et, vals), o
    if t == 10:
        out = {}
        while True:
            ct = b[o]; o += 1
            if ct == 0:
                return out, o
            name, o = _nbt_string(b, o)
            val, o = _nbt_payload(b, o, ct)
            out[name] = (ct, val)   # keep the child tag type — _s_payload needs it back
    if t == 11:
        n, o = struct.unpack_from(">i", b, o)[0], o + 4
        return list(struct.unpack_from(f">{n}i", b, o)), o + 4 * n
    if t == 12:
        n, o = struct.unpack_from(">i", b, o)[0], o + 4
        return list(struct.unpack_from(f">{n}q", b, o)), o + 8 * n
    raise ValueError(f"nbt tag type {t} at {o}")


def _s_string(out: bytearray, s: str) -> None:
    raw = s.encode("utf-8")
    out += struct.pack(">H", len(raw))
    out += raw


def _s_payload(out: bytearray, t: int, v) -> None:
    if t == 1: out.append(v & 0xFF)
    elif t == 2: out += struct.pack(">h", v)
    elif t == 3: out += struct.pack(">i", v)
    elif t == 4: out += struct.pack(">q", v)
    elif t == 5: out += struct.pack(">f", v)
    elif t == 6: out += struct.pack(">d", v)
    elif t == 7:
        out += struct.pack(">i", len(v)); out += v
    elif t == 8: _s_string(out, v)
    elif t == 9:
        et, vals = v
        out.append(et)
        out += struct.pack(">i", len(vals))
        for item in vals:
            _s_payload(out, et, item)
    elif t == 10:
        for name, (ct, cv) in v.items():
            out.append(ct)
            _s_string(out, name)
            _s_payload(out, ct, cv)
        out.append(0)
    elif t == 11:
        out += struct.pack(">i", len(v)); out += struct.pack(f">{len(v)}i", *v)
    elif t == 12:
        out += struct.pack(">i", len(v)); out += struct.pack(f">{len(v)}q", *v)
    else:
        raise ValueError(f"nbt tag type {t}")


def _patch_player(dest: Path) -> None:
    """Hotbar-camera surgery on the copied Dev playerdata (stdlib struct only): the
    formal wrench rides IN with the player — hotbar slot 0 with SelectedItemSlot
    pinned to it — and the camera is pinned to the stage rig: spawn 480.5 64 61.5,
    yaw 0 = +Z south, the transformer wall 3 blocks ahead, pitch 30 down onto the
    faces (the post-join pointer lift eats ~10 of it). THREE blocks, not six: the
    grid is a TARGETED-block overlay (RenderHighlightEvent.Block never fires past
    the 4.5 survival reach — the first live run at 6 blocks drew nothing, not even
    a vanilla selection box). The playerdata seed in the SERVER world's
    playerdata/ is part of this chain's stage (a dev-client run once wrote it; the
    copy picks it up)."""
    path = dest / "playerdata" / f"{_DEV_UUID}.dat"
    if not path.is_file():
        raise SystemExit(f"player fixture missing: {path} — re-seed it from a dev-client "
                         "run's saves/wrenchgrid/playerdata into the server world")
    raw = gzip.open(path, "rb").read()
    root_type = raw[0]                          # TAG_Compound
    name, off = _nbt_string(raw, 1)             # the root's "" name
    player, off = _nbt_payload(raw, off, root_type)   # a playerdata root IS the player
    player["Inventory"] = (9, (10, [{"Slot": (1, 0), "id": (8, "gt6:wrench"), "Count": (1, 1)}]))
    player["SelectedItemSlot"] = (3, 0)
    player["Rotation"] = (9, (5, [0.0, 30.0]))
    player["Pos"] = (9, (6, [480.5, 64.0, 61.5]))   # Pos is a DOUBLE list — a float list reads back empty
    out = bytearray()
    out.append(root_type)
    _s_string(out, name)
    _s_payload(out, root_type, player)
    buf = io.BytesIO()
    with gzip.GzipFile(fileobj=buf, mode="wb", mtime=0) as g:
        g.write(bytes(out))
    path.write_bytes(buf.getvalue())
    print(f"[client] wrench seeded into hotbar slot 0 + camera pinned ({path.name})")


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
    """One dev-client leg: join the copied world with the wrench pre-seeded, two F2 shots."""
    xvfb = os.environ.get("GT6_XVFB_DISPLAY", ":97")   # the resident Xvfb, never :0
    src = _server_world_dir()
    dest = NODE_RUN / "saves" / WORLD_NAME
    if dest.exists():
        shutil.rmtree(dest)
    dest.parent.mkdir(parents=True, exist_ok=True)
    shutil.copytree(src, dest)
    _patch_player(dest)
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
        # (the wrench rode in via playerdata, hotbar slot 0 pre-selected)
        subprocess.run(["xdotool", "key", "--clearmodifiers", "F2"], env=xenv, check=False)
        shot_armed = _wait_new_shot(shot_dir, before, "armed")
        before |= {shot_armed.name}

        # shot 2: empty hands — one digit key switches to the empty hotbar slot 2
        # and the grid must hide (the shown-means-clickable gate)
        subprocess.run(["xdotool", "key", "--clearmodifiers", "2"], env=xenv, check=False)
        time.sleep(1.0)
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
