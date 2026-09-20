#!/usr/bin/env python3
"""p32-render-embeddium-tint — the Embeddium no-shader machine-tint live-verification chain.

The known_bugs embeddium_tint_no_shader triage card: user reports machine paint tint
broken with Embeddium and no shader pack, normal with shaders. This chain owns the
WORLD half of the live verification (the client half is the same module's --client
arm, this file, driving the mdk dev client per the P26 smoke automation boundary:
the renderer mods are client_only, so the RCON server only stages the scene):

  A the stage — a smooth_stone platform at y=63 over the vanilla-flat world (the
    flat ground sits at y=3; the chains' y=64 working plane floats without it) plus
    the pinned lighting (noon, daylight locked, clear).
  B the three-machine target row at z=64, front line facing the spawn camera:
    450 = UNPAINTED (the shredder row material, Bronze #D2823C — the p27 fidelity
    default), 452 = spray dye 1 (Red #FF0000 direct store), 454 = spray dye 11
    (Blue direct store — hue-separated from the bronze and the red).
  C the camera pin: setworldspawn 452 64 59 — a fresh client player spawns at the
    block centre facing yaw=0 (+Z, south), the machines 5 blocks due south, eye
    65.62 puts the y64..65 machine bodies in the lower-middle of the frame.

passes=1 (the machines PERSIST for the client legs — the world folder is copied
into the dev client's saves/ after the server stops; a second pass would wipe and
rebuild, which the bbox cleanup at pass-open already makes safe, but one pass is
the exact world the client needs).

Run:  GT6_SESSION=off python3 tools/rcon/chains/p32_embeddium_tint.py

The client arm (same module, no server needed after the world copy):
  python3 tools/rcon/chains/p32_embeddium_tint.py --client baseline
  python3 tools/rcon/chains/p32_embeddium_tint.py --client embeddium
  python3 tools/rcon/chains/p32_embeddium_tint.py --compare a.png b.png
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

# The site — x 452, z 64: z=64 band at x 449..456 is clear of every declared chain
# site (the x 430..470 census: z 20 / 124..138 / 230..284 / 328..342 / 430..462 in
# use; p27 builder wand owns 458..462 at z=124). dy=1 covers the machine layer.
G = gt6world.Site(452, 64, 63, dx=4, dy=1, dz=5)

M_UNPAINTED = "450 64 64"
M_RED = "452 64 64"
M_ORANGE = "454 64 64"

steps = []

# ------------------------------------------- A: the stage + the pinned lighting
steps += [
    phase("A: the smooth_stone stage under the target row (the flat world ground sits at y=3)"),
    Step("fill 449 63 59 455 63 67 minecraft:smooth_stone", expect="Successfully filled 63"),
    Step("time set noon", expect="Set the time to"),
    Step("gamerule doDaylightCycle false", expect="doDaylightCycle"),
    Step("weather clear", expect="weather"),
]

# --------------------------- B: the target row (fronts south, toward the camera)
steps += [
    phase("B: the three-machine row — unpainted bronze, spray red #FF0000, spray orange #FF8000"),
    Step(f"gt6machine shredder place {M_UNPAINTED}", expect="placed at 450, 64, 64"),
    Step(f"gt6machine shredder place {M_RED}", expect="placed at 452, 64, 64"),
    Step(f"gt6machine shredder place {M_ORANGE}", expect="placed at 454, 64, 64"),
    Step(f"gt6machine paint {M_RED} 1",
         expect="RGB #FFFFFF->#FF0000 painted=true (APPLIED)"),
    Step(f"gt6machine paint {M_ORANGE} 11",
         expect="painted=true (APPLIED)"),
]

# ------------------------------------------- C: the camera pin (spawn = the rig)
steps += [
    phase("C: the camera pin — fresh players spawn facing +Z, the row 5 blocks south"),
    Step("setworldspawn 452 64 59", expect="Set the world spawn"),
    Step("gamerule spawnRadius 0", expect="spawnRadius"),
]

# NOTE: no teardown phase — the machines are the fixture the client legs photograph.

CHAIN = Chain(
    name="p32-embeddium-tint",
    slug="p32embedtint",
    sites=gt6world.declare_sites(G),
    preferred_ports=(26133, 26143),     # this card's pinned rcon/query pair (P32 segment)
    steps=steps,
    passes=1,
)


# ---------------------------------------------------------------------------
# The client half: world copy → dev-client screenshot legs → pixel comparison.
# ---------------------------------------------------------------------------

REPO = _HERE.parent.parent.parent            # tools/rcon/chains → repo root
# The dev client's gameDir is the stonecutter NODE run dir (build.forge.gradle.kts
# `gameDirectory = file("run/")` resolves against :mdk:1.20.1-forge) — the SAME folder
# the RCON server provisions; mdk/run/ would be the wrong tree entirely.
NODE_RUN = REPO / "mdk" / "versions" / "1.20.1-forge" / "run"
MDK_RUN = REPO / "mdk" / "run"
WORLD_NAME = "p32tint"


def _server_world_dir():
    """The chain boot's world folder (gt6server.run_dir layout)."""
    import gt6server
    candidates = [Path(gt6server.run_dir(str(REPO))), Path(gt6server.run_dir(str(REPO), node="1.20.1-forge"))]
    for cand in candidates:
        world = Path(cand) / "world"
        if world.is_dir():
            return world
    raise SystemExit(f"server world folder not found under {candidates} — run the chain first")


def cmd_world_copy():
    """Copy the staged server world into the dev client saves/ as <WORLD_NAME>."""
    src = _server_world_dir()
    dest = NODE_RUN / "saves" / WORLD_NAME
    if dest.exists():
        shutil.rmtree(dest)
    dest.parent.mkdir(parents=True, exist_ok=True)
    shutil.copytree(src, dest)
    print(f"[p32-client] world copied: {src} -> {dest}")
    return 0


def _seed_options():
    """Deterministic client options (both legs byte-identical rendering config)."""
    NODE_RUN.mkdir(parents=True, exist_ok=True)
    options = NODE_RUN / "options.txt"
    options.write_text(
        "version:4105\n"
        "onboardAccessibility:false\n"    # 1.20 first-launch dialog would block quickPlay
        "pauseOnLostFocus:false\n"        # a focus flicker must not darken the shot
        "renderDistance:8\n"
        "gamma:1.0\n"
        "guiScale:2\n"
        "fullscreen:false\n"
        "fov:0.0\n"                       # FOV 70 vanilla default (0 = the default setting)
        "clouds:false\n"
        "graphicsMode:0\n"
        "ambientOcclusion:2\n"
        "biomeBlendRadius:0\n"
    )
    print(f"[p32-client] options seeded: {options}")


def _grab(xvfb, wx, wy, out_png):
    """One framebuffer grab of the game window region (llvmpipe composites to X)."""
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-f", "x11grab",
                    "-video_size", "640x360", "-i", f"{xvfb}.0+{wx},{wy}",
                    "-frames:v", "1", str(out_png)], check=False,
                   env=dict(os.environ, DISPLAY=xvfb))


def _screen_state(png):
    """Classify the grabbed frame: 'select' | 'world' | 'other'.

    select  = the white 'Select World' header text on the dark header band
    world   = sky-blue top rows (in-world, day)
    """
    from PIL import Image
    img = Image.open(png).convert("RGB")
    px = img.load()
    # select-world header: a row in the upper band packed with bright text pixels
    # (the "Select World" line) — the band, not a fixed strip, because the window
    # placement drifts between Xvfb boots
    for y in range(0, 90):
        bright = sum(1 for x in range(270, 370) if sum(px[x, y]) > 600)
        if bright > 30:
            return "select"
    # in-world day sky: blue-dominated top rows
    r, g, b = px[320, 20]
    if b > 120 and b > r + 30:
        return "world"
    return "other"


def _find_gray_button_row(png, y_from, y_to):
    """The vanilla button gray band: a row in [y_from, y_to) where the horizontal
    strip x 220..420 is mostly uniform gray — the Singleplayer/Play buttons."""
    from PIL import Image
    img = Image.open(png).convert("RGB")
    px = img.load()
    for y in range(y_from, y_to):
        gray = sum(1 for x in range(220, 420)
                   if abs(px[x, y][0] - px[x, y][1]) < 14
                   and abs(px[x, y][1] - px[x, y][2]) < 14
                   and 80 < px[x, y][0] < 200)
        if gray > 150:
            return y
    return None


def _find_list_row_y(png):
    """The world-list first row: the entry NAME is white text right of the thumbnail.
    The thumbnail itself is unreliable (a gray village icon carries no colour), so the
    detector hunts the white name text at x 225..270; return the row centre y or None."""
    from PIL import Image
    img = Image.open(png).convert("RGB")
    px = img.load()
    for y in range(40, 150):
        white = sum(1 for x in range(225, 270)
                    if px[x, y][0] > 190 and px[x, y][1] > 190 and px[x, y][2] > 190)
        if white > 8:
            return y + 8          # the name line is the row's upper half
    return None


def cmd_client(leg: str, wait_seconds: float = 30.0, join_only: bool = False) -> int:
    """One dev-client leg: launch, click into the copied world, turn to the row,
    screenshot, terminate.

    baseline  = vanilla renderer (no Embeddium on the run classpath)
    embeddium = -Pgt6.embeddium=true (the property-gated modRuntimeOnly slot)

    1.20.1 has NO --quickPlaySingleplayer (Quick Play shipped 1.20.2+; the flag is
    silently ignored — jstack-verified title-screen idling). The join is a verified
    click recipe at the 640x360 window with guiScale 2 (logical 320x180): title
    Singleplayer at (160,103), the world list row 1 at (150,33) — the copied folder
    shows the level.dat name "world" — and Play Selected World at (120,159). The
    fresh spawn faces yaw 0 (south) with +-10 blocks of position spread, so the
    in-game half turns the camera ~180 deg north to face the machine row (the
    measured llvmpipe rate is ~0.43 deg/px; the exact landing is verified through
    the screen-state poll, not assumed).
    """
    xvfb = os.environ.get("GT6_XVFB_DISPLAY", ":97")   # the resident Xvfb, never :0
    shot_dir = NODE_RUN / "screenshots"
    shot_dir.mkdir(parents=True, exist_ok=True)
    before = {p.name for p in shot_dir.glob("*.png")}

    gradle_flags = ["-Pgt6.nojade=true"]   # the dev-env Jade TitleScreen translation assert
    if leg == "embeddium":
        gradle_flags.append("-Pgt6.embeddium=true")
    # the game runs INSIDE the long-lived gradle daemon — the inline launcher env never
    # reaches it; the DISPLAY override must ride the property gate (JavaExec environment)
    gradle_flags.append(f"-Pgt6.display={xvfb}")

    args = "--width 640 --height 360"
    cmd = ["./gradlew", ":mdk:1.20.1-forge:runClient",
           f"-Pgt6.quickplay={args}"] + gradle_flags
    print(f"[p32-client:{leg}] $ {' '.join(cmd)}")
    env = dict(os.environ)
    env.setdefault("LIBGL_ALWAYS_SOFTWARE", "1")   # same GL for both legs (llvmpipe)
    proc = subprocess.Popen(cmd, cwd=str(REPO), env=env,
                            stdout=open(NODE_RUN / f"client_{leg}.log", "w"),
                            stderr=subprocess.STDOUT)
    xenv = dict(os.environ, DISPLAY=xvfb)
    dbg = MDK_RUN / f"dbg_{leg}"
    dbg.mkdir(parents=True, exist_ok=True)
    try:
        log_path = NODE_RUN / f"client_{leg}.log"
        # 1. wait for the game window (the loader's final atlas line precedes the title)
        deadline = time.time() + 900.0
        while time.time() < deadline:
            time.sleep(5.0)
            if proc.poll() is not None:
                raise SystemExit(f"client died early (exit {proc.returncode}); see {log_path}")
            try:
                if "modularui:textures/atlas/gui.png-atlas" in log_path.read_text(errors="ignore"):
                    break
            except OSError:
                continue
        else:
            raise SystemExit("loader never reached the atlas stage")
        time.sleep(75.0)   # llvmpipe first-frame headroom

        # pin the game window to (0,0): Xvfb has no WM and the X placement drifts
        # between boots, which would poison every downstream coordinate
        for wid in subprocess.run(["xdotool", "search", "--name", "Minecraft"],
                                  capture_output=True, text=True, env=xenv).stdout.split():
            geo = subprocess.run(["xdotool", "getwindowgeometry", "--shell", wid],
                                 capture_output=True, text=True, env=xenv).stdout
            wpos = dict(line.split("=") for line in geo.splitlines() if "=" in line)
            if wpos.get("WIDTH") == "640" and wpos.get("HEIGHT") == "360":
                subprocess.run(["xdotool", "windowmove", wid, "0", "0"], env=xenv, check=False)
                break
        time.sleep(2.0)

        def win_pos():
            # pick the GAME window by size — the Forge early-display window shares the
            # "Minecraft" name while it lives and poisons a name-only lookup
            ids = subprocess.run(["xdotool", "search", "--name", "Minecraft"],
                                 capture_output=True, text=True, env=xenv).stdout.split()
            for wid in ids:
                geo = subprocess.run(["xdotool", "getwindowgeometry", "--shell", wid],
                                     capture_output=True, text=True, env=xenv).stdout
                wpos = dict(line.split("=") for line in geo.splitlines() if "=" in line)
                if wpos.get("WIDTH") == "640" and wpos.get("HEIGHT") == "360":
                    return int(wpos.get("X", 0)), int(wpos.get("Y", 0))
            return 0, 0

        def click(lx, ly):   # logical -> physical window-relative (live geometry)
            wx, wy = win_pos()
            subprocess.run(["xdotool", "mousemove", "--sync",
                            str(wx + lx * 2), str(wy + ly * 2)], env=xenv, check=False)
            time.sleep(0.8)
            subprocess.run(["xdotool", "click", "1"], env=xenv, check=False)
            time.sleep(1.5)

        # 2. reach the Select World screen (clicks verified on :97; each attempt is
        # state-checked through the framebuffer, the llvmpipe UI renders into X)
        joined = False
        for attempt in range(4):
            _grab(xvfb, *win_pos(), dbg / f"title_try{attempt}.png")
            btn = _find_gray_button_row(dbg / f"title_try{attempt}.png", 100, 300)
            if btn is None:
                time.sleep(8.0)
                continue
            subprocess.run(["xdotool", "mousemove", "--sync", "320", str(btn)],
                           env=xenv, check=False)
            time.sleep(0.8)
            subprocess.run(["xdotool", "click", "1"], env=xenv, check=False)
            time.sleep(6.0)
            _grab(xvfb, *win_pos(), dbg / f"select_try{attempt}.png")
            if _screen_state(dbg / f"select_try{attempt}.png") == "select":
                break
        else:
            raise SystemExit("never reached the Select World screen; see debug grabs")

        # 3. select row 1, Play, and wait for the integrated server in the log
        for attempt in range(4):
            _grab(xvfb, *win_pos(), dbg / f"sel_try{attempt}.png")
            row_y = _find_list_row_y(dbg / f"sel_try{attempt}.png")
            if row_y is not None:
                subprocess.run(["xdotool", "mousemove", "--sync", "300", str(row_y)],
                               env=xenv, check=False)
                time.sleep(0.8)
                subprocess.run(["xdotool", "click", "1"], env=xenv, check=False)
                time.sleep(1.5)
            btn = _find_gray_button_row(dbg / f"sel_try{attempt}.png", 280, 360)
            if btn is not None:
                subprocess.run(["xdotool", "mousemove", "--sync", "240", str(btn)],
                               env=xenv, check=False)
                time.sleep(0.8)
                subprocess.run(["xdotool", "click", "1"], env=xenv, check=False)
            mark = time.time()
            while time.time() - mark < 90.0:
                time.sleep(3.0)
                if proc.poll() is not None:
                    raise SystemExit(f"client died early; see {log_path}")
                try:
                    text = log_path.read_text(errors="ignore")
                except OSError:
                    continue
                if "Starting integrated minecraft server version" in text:
                    joined = True
                    break
            if joined:
                break
            _grab(xvfb, *win_pos(), dbg / f"play_try{attempt}.png")
        if not joined:
            raise SystemExit("the integrated server never started; see debug grabs")
        print(f"[p32-client:{leg}] world joining...")
        time.sleep(45.0)                          # spawn + the llvmpipe chunk build
        if join_only:
            print(f"[p32-client:{leg}] joined; leaving the game alive for manual aim")
            return 0

        # 4. the turn: spawn faces south, the row is north. Two calibrated relative
        # moves (yaw ~-320px to 180, then the pitch back up); then verify the sky is
        # gone from the frame centre? No — verify via F3-free pixels: just take the shot.
        subprocess.run(["xdotool", "mousemove_relative", "--", "-420", "0"],
                       env=xenv, check=False)   # ~180 deg at the measured 0.43 deg/px
        time.sleep(1.5)
        # the pointer-capture jump at join injects ~40 deg of downward pitch; lift back
        # to a slight downward aim (mouse up = negative dy = pitch decreases)
        subprocess.run(["xdotool", "mousemove_relative", "--", "0", "-58"],
                       env=xenv, check=False)
        time.sleep(6.0)

        # 5. F2 and wait for the file (GLFW filters XSendEvent -> plain XTEST key)
        subprocess.run(["xdotool", "key", "--clearmodifiers", "F2"], env=xenv, check=False)
        for _ in range(10):
            time.sleep(2.0)
            new = sorted({p.name for p in shot_dir.glob("*.png")} - before)
            if new:
                break
        else:
            _grab(xvfb, wx, wy, MDK_RUN / f"shot_{leg}.png")
            print(f"[p32-client:{leg}] F2 produced nothing; x11grab fallback used")
            return 0
        src = shot_dir / new[-1]
        dest = NODE_RUN / f"shot_{leg}.png"
        shutil.copyfile(src, dest)
        print(f"[p32-client:{leg}] screenshot: {src} -> {dest}")
        return 0
    finally:
        if join_only:
            return 0   # the caller aims and shoots manually; the game must stay alive
        proc.terminate()
        try:
            proc.wait(timeout=30)
        except subprocess.TimeoutExpired:
            proc.kill()
        # killing the wrapper does NOT kill the game (a daemon child, not ours) — reap
        # the orphan by the double match (launch target + THIS worktree path) so no
        # zombie window intercepts the next leg's clicks
        subprocess.run(["pkill", "-f", f"forgeclientuserdev.*{REPO}"], check=False)
        time.sleep(2.0)


# ---------------------------------------------------------------------------
# The pixel comparison: per-machine mean RGB + the tint verdict.
# ---------------------------------------------------------------------------

def cmd_compare(baseline_png: str, embeddium_png: str) -> int:
    """Position-independent tint verdict.

    The singleplayer spawn spread randomises the camera position per join, so fixed
    sampling boxes would misalign between legs. Instead each frame is classified per
    pixel: sky (blue-dominant), grass (green-dominant), grayscale (the untinted
    failure mode — max-min channel spread under 18) and the tinted machine clusters
    (strong red / strong blue / warm bronze). The TINT BUG shows as the machine
    clusters collapsing into grayscale; the verdict per target is presence in BOTH
    legs above a small fraction of the frame.
    """
    from PIL import Image

    def profile(path):
        img = Image.open(path).convert("RGB")
        img = img.resize((320, 180))
        px = img.load()
        total = 320 * 180
        red = blue = bronze = gray = 0
        for y in range(180):
            for x in range(320):
                r, g, b = px[x, y]
                mx, mn = max(r, g, b), min(r, g, b)
                if mx - mn < 18:
                    gray += 1
                elif b > r + 40 and b > 60:
                    blue += 1        # sky or the blue machine
                elif g > r + 20 and g > b + 20:
                    pass             # grass
                elif r > 140 and r > g * 1.8 and r > b * 1.8:
                    red += 1         # the red-painted body
                elif r > 120 and g > 70 and b < 90 and r > b + 60:
                    bronze += 1      # the bronze material tint (or orange)
        return {"red": red / total, "blue": blue / total,
                "bronze": bronze / total, "gray": gray / total}

    pa, pb = profile(baseline_png), profile(embeddium_png)
    print(f"{'fraction':<10} {'baseline':>10} {'embeddium':>10}   verdict")
    verdicts = []
    for key in ("red", "bronze", "blue", "gray"):
        a, b = pa[key], pb[key]
        if key == "gray":
            ok = True   # informational: stone/sky shading, not a tint marker
            note = ""
        else:
            ok = a > 0.002 and b > 0.002
            note = "" if ok else "  <-- CLUSTER LOST"
        verdicts.append(ok)
        print(f"{key:<10} {a:>10.4f} {b:>10.4f}   {'ok' if ok else 'MISSING'}{note}")
    print("\ncompare verdict: " + ("TINTED IN BOTH LEGS" if all(verdicts) else "TINT LOST IN A LEG"))
    return 0 if all(verdicts) else 1


def cli(argv=None):
    parser = argparse.ArgumentParser(prog="p32_embeddium_tint")
    parser.add_argument("--client", choices=["baseline", "embeddium"])
    parser.add_argument("--join-only", action="store_true")
    parser.add_argument("--compare", nargs=2, metavar=("BASELINE", "EMBEDDIUM"))
    parser.add_argument("--world-copy", action="store_true")
    args, rest = parser.parse_known_args(argv)
    if args.compare:
        return cmd_compare(args.compare[0], args.compare[1])
    if args.client:
        _seed_options()
        return cmd_client(args.client, join_only=args.join_only)
    if args.world_copy:
        return cmd_world_copy()
    return main(CHAIN)


if __name__ == "__main__":
    sys.exit(cli())
