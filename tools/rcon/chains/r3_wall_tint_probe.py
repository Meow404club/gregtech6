#!/usr/bin/env python3
"""r3-wall-tint-probe — the issue-#8 world-baked-tint chain (D1 discrimination,
reused verbatim by task r3-world-tint-render-type as the C7 fix-acceptance leg).

The D1 card: user reports the dense tungsten burning box body pure-gray and the
T2 dryer body without its Invar tint. Offline the whole dye chain is wired
(row material -> tintindex-0 body -> paintable arrays -> materialOf gates ->
ItemColor), so the residue is one live question — does the WORLD half (the
p32 baked-vertex-colour route, GTMachineTintModel) actually reach the screen
on a real render? (D1 verdict: a — the tint never showed; the D2 root cause
was the SOLID-layer decal-shell occlusion + the wrapper burying the JSON
render_type; C7 = cutout declarations + the getRenderTypes forward.)

THE SHOT (p32 embeddium_tint.py form — world staging chain + client legs):

  A the stage — a smooth_stone platform at y=63 over the vanilla-flat world
    (flat ground sits at y=3), noon light locked, clear sky, plus a tall
    smooth_stone BACKDROP WALL behind the row so no sky pixel shows in the
    lower frame and no stray world remnant can leak into the cluster census
    (the first take caught a legacy brick hut east of the site poisoning the
    warm cluster — the wall now spans x 436..476 and the site cleanup wipes
    everything it could hide).
  B the #8 row on the z=80 line — the dryer dead-centre under the pinned
    camera flanked (IN frame, two blocks out) by the C7 clean-up pair:
    452 = dryer_t2 dead-centre (MT.Invar = 220,220,150 — THE discriminator:
          correct = pale yellow-white, R/B-swapped = pale cyan, dead tint =
          neutral gray);
    450 = steam_boiler_tank_invar (the C5 boiler wiring — the SAME warm
          Invar signature, so the boiler's dye rides the warm cluster);
    454 = machine_wall_tungsten (the C5 wall clean-up — MT.W = 50,50,50 dark
          + the metalwall decals, rides the neutral/dark clusters);
    446 = dense_burning_box_solid_tungsten and 458 = machine_wall_steel
          (MT.Steel = 130,130,130) flank SIX blocks out — staged in-world,
          verifiable in the RCON transcript, but never in the frame (the
          burning-box decal art carries warm embers that would poison the
          hue census; their tints are R/G/B-symmetric anyway).
    Plus an oak-leaves hedge behind the dryer: leaves are tinted through
    the VANILLA runtime BlockColors path, so green leaves in the SAME frame
    prove the vertex-colour channel of THIS box is alive — without that an
    all-gray dryer reading would be a box artifact, not a verdict.
  C the camera pin — setworldspawn 452 64 75; a fresh client player spawns
    at the block centre facing yaw=0 (+Z south), the dryer 5 blocks due
    south, eye 65.62. The pointer is PARKED at the game window centre
    BEFORE launch so the GLFW world-join grab-jump injects ~zero yaw/pitch;
    the pitch-dithered F2 ladder covers the aim uncertainty and doubles as
    the 掠射角+正视 two-state coverage (the cumulative rungs land at
    distinctly different incidence angles over the body faces; D2 recorded
    the dryer marching up the frame across the ladder).

THE VERDICT (--verdict <png>...): three-state over the dryer body cluster —
    c  warm yellow-white (r ~= g >> b) -> chain fully alive (the C7
                                          acceptance state);
    b  cyan            (g ~= b >> r) -> chain ALIVE with the R1 ABGR
                                          byte-order bug (C1 landed — this
                                          state means a regression);
    a  neither (neutral gray, leaves still green) -> the world-baked tint
                                          route is STILL occluded (the C7
                                          fix regressed);
    ?  neither AND leaves not green    -> box artifact persists (llvmpipe
                                          colour loss), INCONCLUSIVE;
    !  dryer dark-face window absent   -> AIM MISS, judge the next frame.

The warm/cyan hue shapes are pinned tight (g >= r-45 / g >= b-45): the
Invar signatures have r ~= g (g ~= b), while every known warm imposter in
these assets (burning-box ember art ~(146,99,86), Steve's arm, skin tones)
carries r/g >= 1.4 and fails the gate.

Run (world staging, per-chain boot like the p32 Run line):
    GT6_SESSION=off python3 tools/rcon/chains/r3_wall_tint_probe.py

The client legs (no server needed after the world copy):
    python3 tools/rcon/chains/r3_wall_tint_probe.py --world-copy
    python3 tools/rcon/chains/r3_wall_tint_probe.py --client
    python3 tools/rcon/chains/r3_wall_tint_probe.py --verdict <png> [<png>...]
Both --world-copy and --client accept --node <stonecutter node> for the
second leg (default 1.20.1-forge; the neo leg stages its own world copy of
the same shot after a --node run of the staging chain).
"""

import argparse
import os
import shutil
import subprocess
import sys
import time
from pathlib import Path

_HERE = Path(__file__).resolve().parent
for _path in (str(_HERE), str(_HERE.parent), str(_HERE.parent.parent.parent / "tools")):
    if _path not in sys.path:
        sys.path.insert(0, _path)

import gt6world
from framework import Chain, Step, main, phase

# The site — x 436..476 covers the trio's six-block spread and the legacy
# remnant band east of 460 (wiped by the pass-open bbox cleanup); z 68..86
# covers spawn 75 / row 80 / hedge 83 / backdrop 85 with margin. Clear of
# every declared chain band at x 425..480 (the census: z 20 / 58..68 (p32
# embeddium_tint) / 100 (mui_row_dispatch, x 480 dx=1) / 124..138 / 230..284
# / 328..342 / 430..462).
G = gt6world.Site(456, 65, 77, dx=20, dy=3, dz=9)

BOX_W = "446 64 80"     # dense tungsten box — six blocks west, out of frame
BOILER = "450 64 80"    # steam_boiler_tank_invar — two west, IN frame (the C5 wiring)
DRYER = "452 64 80"     # dead-centre under the camera — the one in frame
TUNG_W = "454 64 80"    # machine_wall_tungsten — two east, IN frame (the C5 clean-up)
WALL = "458 64 80"      # steel wall — six blocks east, out of frame

steps = []

# ------------------------------------------- A: the stage + the pinned lighting
steps += [
    phase("A: the smooth_stone stage + backdrop wall (flat world ground sits at y=3)"),
    Step("fill 436 63 70 476 63 84 minecraft:smooth_stone", expect="Successfully filled 615"),
    # the backdrop kills every sky pixel and every world remnant in the lower frame
    Step("fill 436 64 85 476 68 85 minecraft:smooth_stone", expect="Successfully filled 205"),
    Step("time set noon", expect="Set the time to"),
    Step("gamerule doDaylightCycle false", expect="doDaylightCycle"),
    Step("weather clear", expect="weather"),
]

# --------------------------- B: the target row (one machine in frame by design)
steps += [
    phase("B: the #8 row — dryer_t2 invar centre-frame; boiler + tungsten wall in frame, box + steel wall flanking out-of-frame"),
    Step(f"gt6burner place {BOX_W} dense_burning_box_solid_tungsten",
         expect="GT6 burning box placed at 446, 64, 80"),
    Step(f"gt6boiler place {BOILER} steam_boiler_tank_invar",
         expect="GT6 boiler tank placed at 450, 64, 80: steam_boiler_tank_invar"),
    Step(f"gt6machine dryer_t2 place {DRYER}", expect="GT6 dryer_t2 placed at 452, 64, 80"),
    Step(f"setblock {TUNG_W} gt6:machine_wall_tungsten", expect="Changed the block at 454, 64, 80"),
    Step(f"setblock {WALL} gt6:machine_wall_steel", expect="Changed the block at 458, 64, 80"),
    # the vanilla-tint channel proof, in the same frame (persistent: no log decay)
    Step("fill 450 65 83 454 66 83 minecraft:oak_leaves[persistent=true]",
         expect="Successfully filled 10"),
]

# ------------------------------------------- C: the camera pin (spawn = the rig)
steps += [
    phase("C: the camera pin — fresh players spawn facing +Z, the dryer 5 blocks south"),
    Step("setworldspawn 452 64 75", expect="Set the world spawn"),
    Step("gamerule spawnRadius 0", expect="spawnRadius"),
]

# NOTE: no teardown — the trio is the fixture the client legs photograph.

CHAIN = Chain(
    name="r3-wall-tint-probe",
    slug="r3walltint",
    sites=gt6world.declare_sites(G),
    preferred_ports=(26153, 26163),     # this card's pinned rcon/query pair (D1 segment)
    steps=steps,
    passes=1,
)


# ---------------------------------------------------------------------------
# The client half: Xvfb ensure -> world copy -> dev-client screenshot legs.
# ---------------------------------------------------------------------------

REPO = _HERE.parent.parent.parent            # tools/rcon/chains -> repo root
WORLD_NAME = "r3wall"
EVIDENCE_DIR = Path("/tmp/r3_wall_tint_probe")

DEFAULT_NODE = "1.20.1-forge"

# the 640x360 window sits at (0,60) on the display (the p32 x11grab offset);
# its centre — where the pointer is parked so the GLFW world-join grab-jump
# (recenter delta) injects ~zero yaw/pitch.
WINDOW_CENTRE = (320, 240)

# pitch-dither ladder (xdotool dy units, positive = look down; ~0.43 deg/unit
# from the p32 calibration): three frames bracket the -10..-19 deg aim window.
PITCH_NUDGES = (24, 34, 44)


def _node_run(node):
    """The stonecutter node run dir (the dev client's gameDir — the SAME folder
    the RCON server provisions; mdk/run/ would be the wrong tree entirely)."""
    return REPO / "mdk" / "versions" / node / "run"


def _server_world_dir(node):
    """The chain boot's world folder for `node` (gt6server.run_dir layout)."""
    import gt6server
    world = Path(gt6server.run_dir(str(REPO), node=node)) / "world"
    if world.is_dir():
        return world
    raise SystemExit(f"server world folder not found: {world} — run the staging "
                     f"chain first (python3 tools/rcon/chains/r3_wall_tint_probe.py"
                     f"{' --node ' + node if node != DEFAULT_NODE else ''})")


def cmd_world_copy(node):
    """Copy the staged server world into the dev client saves/ as <WORLD_NAME>.

    The copy RESETS the camera (the D2 trap): a r3wall world that ran before
    carries saved playerdata — the accumulated pitch rungs would pin the next
    leg's spawn aim somewhere down the user's nose. The copy starts from the
    server's pristine player state and the lock goes with it.
    """
    src = _server_world_dir(node)
    dest = _node_run(node) / "saves" / WORLD_NAME
    if dest.exists():
        shutil.rmtree(dest)
    dest.parent.mkdir(parents=True, exist_ok=True)
    shutil.copytree(src, dest)
    for stale in (dest / "playerdata", dest / "session.lock"):
        if stale.exists():
            shutil.rmtree(stale, ignore_errors=True) if stale.is_dir() else stale.unlink()
    print(f"[client:{node}] world copied: {src} -> {dest} (playerdata/lock reset)")
    return 0


def _seed_options(node):
    """Deterministic client options (byte-identical rendering config per leg)."""
    node_run = _node_run(node)
    node_run.mkdir(parents=True, exist_ok=True)
    options = node_run / "options.txt"
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
    print(f"[client:{node}] options seeded: {options}")


def _xvfb_alive(xvfb):
    """True when the X server answers a real X-protocol round trip.

    This box's X servers listen on Linux ABSTRACT sockets (@/tmp/.X11-unix/XNN
    — no filesystem socket appears), so a path stat is not a probe here and
    xdpyinfo is not installed; an 8x8 x11grab frame via ffmpeg is the same
    handshake a client needs anyway.
    """
    probe = subprocess.run(
        ["ffmpeg", "-loglevel", "error", "-f", "x11grab", "-video_size", "8x8",
         "-i", f"{xvfb}.0", "-frames:v", "1", "-f", "null", "-"],
        capture_output=True, env=dict(os.environ, DISPLAY=xvfb))
    return probe.returncode == 0


def _ensure_xvfb(xvfb, timeout=20.0):
    """The resident-display contract: boot Xvfb detached (nohup semantics —
    never a foreground wait) when :97 is down, then poll for liveness.

    Constitution rule 6: resident processes are nohup + poll-the-log-marker
    shapes; the X server's "log marker" is a successful x11grab handshake.
    """
    if _xvfb_alive(xvfb):
        print(f"[xvfb] display {xvfb} already alive")
        return
    subprocess.Popen(["Xvfb", xvfb, "-screen", "0", "1280x720x24"],
                     start_new_session=True,
                     stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        time.sleep(1.0)
        if _xvfb_alive(xvfb):
            print(f"[xvfb] display {xvfb} booted (detached)")
            return
    raise SystemExit(f"Xvfb {xvfb} never became alive within {timeout:g}s")


def _reap_game(node):
    """Kill the dev-client game for THIS worktree: the wrapper died but the game
    is a daemon child that survives — a zombie window on :97 would intercept
    the next leg's clicks. The forge dev launch target first (the p32 proven
    pattern), the neoforge twin for the 21.1 leg, then any visible Minecraft
    window on the display as the belt-and-suspenders."""
    xvfb = os.environ.get("GT6_XVFB_DISPLAY", ":97")
    subprocess.run(["pkill", "-f", f"forgeclientuserdev.*{REPO}"], check=False)
    if "neoforge" in node:
        subprocess.run(["pkill", "-f", f"neoforgeclientuserdev.*{REPO}"], check=False)
    ids = subprocess.run(["xdotool", "search", "--onlyvisible", "--name", "Minecraft"],
                         env=dict(os.environ, DISPLAY=xvfb), capture_output=True, text=True)
    for wid in ids.stdout.split():
        subprocess.run(["xdotool", "windowkill", wid],
                       env=dict(os.environ, DISPLAY=xvfb), check=False)
    time.sleep(2.0)


def cmd_client(node, wait_seconds=60.0) -> int:
    """One dev-client leg: quickPlay into the copied world, screenshots, stop.

    The vanilla renderer (no Embeddium — #8 never mentioned it; the card is the
    world-baked tint route itself). quickPlaySingleplayer ships in 1.20.1
    (Main.java:66) as long as the accessibility onboarding dialog is pre-seeded
    off via options.txt in the REAL gameDir. The fresh spawn faces yaw 0 (south)
    and the dryer sits due south of the pinned spawn, so no camera turn; the
    pointer is parked at the window centre BEFORE launch (the grab-jump is the
    recenter delta, and a parked pointer makes it ~zero), then three
    pitch-dithered F2 shots bracket the aim.

    Heavy-op discipline: the leg rides the machine-wide gates (memory + slot,
    tools/gt6testgate) exactly like a gated test run, because runClient compiles
    and runs the full client.
    """
    import gt6testgate
    xvfb = os.environ.get("GT6_XVFB_DISPLAY", ":97")   # the probe display, never :0
    _ensure_xvfb(xvfb)
    xenv = dict(os.environ, DISPLAY=xvfb)
    node_run = _node_run(node)
    tag = node.replace(".", "")
    shot_dir = node_run / "screenshots"
    shot_dir.mkdir(parents=True, exist_ok=True)
    before = {p.name for p in shot_dir.glob("*.png")}
    EVIDENCE_DIR.mkdir(parents=True, exist_ok=True)

    # park the pointer at the window centre — the world-join grab-jump reads
    # (centre - pointer) and injects it as yaw/pitch; parked = no drift
    subprocess.run(["xdotool", "mousemove", *map(str, WINDOW_CENTRE)],
                   env=xenv, check=False)

    args = f"--quickPlaySingleplayer {WORLD_NAME} --width 640 --height 360"
    cmd = ["./gradlew", f":mdk:{node}:runClient",
           f"-Pgt6.quickplay={args}",
           "-Pgt6.nojade=true",                     # the dev-env Jade TitleScreen assert
           f"-Pgt6.display={xvfb}"]                 # daemon env never sees inline DISPLAY
    print(f"[client:{node}] $ {' '.join(cmd)}")
    env = dict(os.environ)
    env.setdefault("LIBGL_ALWAYS_SOFTWARE", "1")    # llvmpipe — same GL on both nodes
    gt6testgate.wait_memory(tag=f"r3wall-client-{tag}")
    slot = gt6testgate.acquire_slot(tag=f"r3wall-client-{tag}")
    log_path = node_run / f"client_r3wall_{tag}.log"
    proc = subprocess.Popen(cmd, cwd=str(REPO), env=env,
                            stdout=open(log_path, "w"),
                            stderr=subprocess.STDOUT)
    shots = []
    try:
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
            raise SystemExit(f"world join marker never appeared; see {log_path}")
        print(f"[client:{node}] world joining...")
        time.sleep(wait_seconds)                    # spawn + the llvmpipe chunk build

        # F2 = the vanilla screenshot key, delivered through XTEST to the focused
        # window (GLFW filters XSendEvent, so `key --window` is a no-op — plain only);
        # one shot per pitch rung of the dither ladder
        for nudge in PITCH_NUDGES:
            subprocess.run(["xdotool", "mousemove_relative", "--", "0", str(nudge)],
                           env=xenv, check=False)
            time.sleep(4.0)
            subprocess.run(["xdotool", "key", "--clearmodifiers", "F2"], env=xenv, check=False)
            time.sleep(2.0)
        for _ in range(10):
            time.sleep(1.0)
            new = sorted({p.name for p in shot_dir.glob("*.png")} - before)
            if len(new) >= len(PITCH_NUDGES):
                break
        shots = sorted({p.name for p in shot_dir.glob("*.png")} - before)[-len(PITCH_NUDGES):]
        if not shots:
            fallback = EVIDENCE_DIR / f"shot_{tag}_fallback.png"
            _grab(xvfb, 0, 60, fallback)
            print(f"[client:{node}] F2 produced nothing; x11grab fallback: {fallback}")
            return 0
        for index, name in enumerate(shots):
            dest = EVIDENCE_DIR / f"shot_{tag}_{index}.png"
            shutil.copyfile(shot_dir / name, dest)
            print(f"[client:{node}] screenshot: {shot_dir / name} -> {dest}")
        return 0
    finally:
        try:
            proc.terminate()
            try:
                proc.wait(timeout=30)
            except subprocess.TimeoutExpired:
                proc.kill()
        finally:
            _reap_game(node)
            gt6testgate.release_slot(slot)


def _grab(xvfb, wx, wy, out_png):
    """One framebuffer grab of the game window region (llvmpipe composites to X)."""
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-f", "x11grab",
                    "-video_size", "640x360", "-i", f"{xvfb}.0+{wx},{wy}",
                    "-frames:v", "1", str(out_png)], check=False,
                   env=dict(os.environ, DISPLAY=xvfb))


# ---------------------------------------------------------------------------
# The three-state verdict: hue clusters over the in-frame dryer.
# ---------------------------------------------------------------------------

# Exclusions: the hotbar/XP strip (y >= 300 at guiScale 2 / 640x360) and the
# empty-hand arm corner (bottom-right, skin-toned). Everything else in frame
# is: dryer (tint verdict + dark face window), the invar boiler (rides the
# warm cluster — same Invar signature, the C5 evidence), the tungsten wall
# (rides the neutral/dark clusters — the C5 evidence), oak leaves (green),
# platform and backdrop (neutral stone), sky above the backdrop (bright blue).
CLUSTER_FLOOR = 0.002     # >= ~370 px of the ~190k counted: the dryer body window is ~15x this
DOMINANCE = 3.0           # the winner must beat the loser 3:1 — no mixed verdicts
LEAVES_FLOOR = 0.003      # the vertex-colour-channel proof (full-frame green)
DARK_FLOOR = 150          # the dryer face-window pixels — the in-frame presence proof


def _classify(png):
    """Pixel census of one shot: warm / cyan / neutral / dark clusters, the
    leaves-green channel proof, and the sky fraction (report only).

    The Invar signatures (texture base ~(204,204,204), noon side shade ~0.8):
      correct   (220,220,150) -> ~(176,176,120): r ~= g > b + 40
      ABGR-swap (150,220,220) -> ~(120,176,176): g ~= b > r + 40
      dead tint               -> ~(163,163,163): channel spread < 18
    The g >= r-45 / g >= b-45 clauses reject the warm imposters in these
    assets (ember art, Steve's arm — all r/g >= 1.4), and the bright-blue sky
    rule keeps the sky out of the cyan cluster.
    """
    from PIL import Image
    img = Image.open(png).convert("RGB")
    if img.size != (640, 360):
        img = img.resize((640, 360))
    px = img.load()
    warm = cyan = neutral = dark = leaves = sky = counted = 0
    for y in range(360):
        if y >= 300:
            continue                            # hotbar / XP strip
        for x in range(640):
            if x >= 430 and y >= 240:
                continue                        # the empty-hand arm corner
            r, g, b = px[x, y]
            mx, mn = max(r, g, b), min(r, g, b)
            if b > r + 40 and b > 150:
                sky += 1
                continue
            if g > r + 20 and g > b + 20 and g > 60:
                leaves += 1
                continue
            counted += 1
            if mx - mn < 18:
                neutral += 1
                if mx < 45:
                    dark += 1                   # the dryer front face window
            elif r > b + 40 and r > 110 and g >= r - 45:
                warm += 1                       # pale yellow-white (Invar, correct)
            elif b > r + 40 and b > 110 and g >= b - 45:
                cyan += 1                       # pale cyan (Invar, R/B swapped)
    total = 640 * 360
    return {"warm": warm / max(counted, 1), "cyan": cyan / max(counted, 1),
            "neutral": neutral / max(counted, 1), "dark": dark,
            "counted": counted,
            "leaves": leaves / total, "sky": sky / total}


def _verdict_one(png):
    """Classify one frame -> (state, lines). state in {a,b,c,?,!}."""
    c = _classify(png)
    lines = [f"  {png}",
             f"    warm (r~=g>b+40)    {c['warm']:.4f}   <- Invar CORRECT (220,220,150)",
             f"    cyan (g~=b>r+40)    {c['cyan']:.4f}   <- Invar R/B SWAPPED (150,220,220)",
             f"    neutral (spread<18) {c['neutral']:.4f}   <- dead-tint register",
             f"    dark face px        {c['dark']}   <- dryer in-frame presence "
             f"(floor {DARK_FLOOR})",
             f"    leaves-green        {c['leaves']:.4f}   <- vanilla tint channel probe",
             f"    sky                 {c['sky']:.4f}"]
    if c["dark"] < DARK_FLOOR:
        return "!", lines + ["    AIM MISS: the dryer's dark face window is absent — "
                             "judge the next frame"]
    if c["leaves"] < LEAVES_FLOOR:
        return "?", lines + ["    INCONCLUSIVE: leaves not green — this box's vertex-"
                             "colour channel is not proven alive"]
    warm_ok = c["warm"] >= CLUSTER_FLOOR
    cyan_ok = c["cyan"] >= CLUSTER_FLOOR
    if warm_ok and c["warm"] > DOMINANCE * max(c["cyan"], 1e-9) and not (cyan_ok and c["cyan"] > DOMINANCE * c["warm"]):
        return "c", lines
    if cyan_ok and c["cyan"] > DOMINANCE * max(c["warm"], 1e-9) and not (warm_ok and c["warm"] > DOMINANCE * c["cyan"]):
        return "b", lines
    if warm_ok and cyan_ok:
        return "x", lines + ["    MIXED warm+cyan clusters both present — inspect manually"]
    return "a", lines


_VERDICT_TEXT = {
    "c": "VERDICT: c  TINT CORRECT (warm yellow-white Invar body) — the dye chain "
         "is fully alive end to end; issue #8 = H1 (stale user build). Answer the "
         "issue: update the build, re-verify.",
    "b": "VERDICT: b  R/B SWAPPED (pale cyan Invar body) — the dye chain IS alive "
         "with the r3-ore-tint-abgr-seam byte-order bug (ARGB tint multiplied into "
         "ABGR vertex bytes); consistent with the C1 fix in flight. Answer the "
         "issue: fix landing, re-verify after C1.",
    "a": "VERDICT: a  NO TINT (neutral dryer body, leaves green) — H2 confirmed: "
         "the world-baked vertex-tint route is dead on a live renderer. Open the "
         "world-half fix card (root cause needs a live getQuads probe).",
    "?": "VERDICT: ?  INCONCLUSIVE — the oak-leaves green is absent, so this box's "
         "vertex-colour channel is NOT proven alive; an all-gray dryer here would "
         "be the known headless-llvmpipe colour loss, NOT evidence for H2. Do not "
         "open the world-half fix card off this frame.",
    "!": "VERDICT: !  AIM MISS — no verdict from this frame.",
    "x": "VERDICT: x  MIXED — inspect manually.",
}


def cmd_verdict(pngs) -> int:
    """Print the three-state discrimination table per frame, then the verdict.

    Multiple frames (the pitch-dither ladder): the first frame with a real
    dryer presence wins (! frames are skipped); among a/b/c frames the first
    non-contradicted state is the verdict, a contradiction is reported as x.
    """
    states = []
    for png in pngs:
        state, lines = _verdict_one(png)
        states.append(state)
        print("\n".join(lines))
    real = [s for s in states if s not in ("!",)]
    if not real:
        print("\n" + _VERDICT_TEXT["!"])
        return 2
    first = real[0]
    if any(s != first for s in real):
        print(f"\ncontradictory frame states: {states} — inspect manually")
        return 3
    print("\n" + _VERDICT_TEXT[first])
    return {"c": 0, "b": 0, "a": 1}.get(first, 2)


def cli(argv=None):
    parser = argparse.ArgumentParser(prog="r3_wall_tint_probe")
    parser.add_argument("--client", action="store_true",
                        help="boot the dev client, quickPlay, screenshots")
    parser.add_argument("--world-copy", action="store_true",
                        help="copy the staged server world into the client saves/")
    parser.add_argument("--verdict", nargs="+", metavar="PNG",
                        help="three-state verdict over screenshot(s)")
    parser.add_argument("--node", default=DEFAULT_NODE,
                        help="stonecutter node for --client/--world-copy "
                             "(default 1.20.1-forge; 1.21.1-neoforge for the "
                             "user-build leg)")
    args, rest = parser.parse_known_args(argv)
    if args.verdict:
        return cmd_verdict(args.verdict)
    if args.client:
        _seed_options(args.node)
        return cmd_client(args.node)
    if args.world_copy:
        return cmd_world_copy(args.node)
    return main(CHAIN)


if __name__ == "__main__":
    sys.exit(cli())
