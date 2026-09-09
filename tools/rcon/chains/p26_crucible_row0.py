#!/usr/bin/env python3
"""p26-crucible-row0 — the crucible-chain live acceptance (task p26-crucible-physics-smeltery,
the row0 band: place → the brick burning box feeds HU → drop → MELT → mold pour → ingot;
the bucket arm round-trips the molten path).

PHYSICS BOUNDARY (live-probed on this branch, r6 takeover — every constant below is
pinned by upstream evidence AND by the probe boots, not by choice):
  - the burning box is emit-gate=top-face-only (gt6burner stat says so), so the box
    sits UNDER the crucible (y=63), its top face against the crucible bottom — the
    upstream opening chain's stack form (Loader :519, the box→smeltery pair).
  - FEED-WINDOW RULE (r6, probed the hard way twice): a burning box spreads REAL fire
    blocks across its 7x5x7 volume while lit (trySpreadFire, the upstream :107 port)
    and with doFireTick=false they never decay — a flame on the crucible's top-air
    cell burns the dropped ItemEntities mid-feed (r5 run: 8 tin dust → only 2U landed,
    six burned). So the charge is dropped BEFORE the box even exists, and the box is
    placed+lit only after the suck poll goes green. The stone rung therefore proves
    its heat flow with the box placed last; the steel rung has no box at all.
  - DROP SEAM (r6 fix, probed with /data get entity): the vanilla ItemEntity
    constructor hands the spawned charge a random horizontal toss — the r5 chain lost
    7 of 8 items 1.2 blocks off-target, hot or cold (a 2493 K crucible sucked exactly
    one unit and waited, which is what cooked the r5 iron pile into the anneal
    disaster). The /gt6crucible drop arm now spawns dead-center inside the suck box
    with zeroed motion; the summon-side control drained 8U in seconds on the same
    cold crucible, pinning the seam as the one and only feed defect.
  - the shell heat mass is material-dependent: tRequiredEnergy = 1 + weight/100
    (upstream :301). Probed: the stone shell answers required=7 fresh; charged with
    8U Sn the box re-heat runs ~+0.21 K/t (~76 charged, probed r5) → 293→505 K in
    ~1000 t, inside the pour poll. The steel shell answers required=62 fresh
    (20000 HU → +322 K, energy=36 residual exact) and ~139 charged with 8U Fe
    (r5 probe: (92000-128)/661 conversions = 139 exact).
  - COLD CHARGE SUCKS HEAT (upstream :333, the +1/-1 balance) — which is why the r6
    steel rung feeds its 8U iron into the COLD (293 K) crucible and only then
    injects: the drag never engages (cold+cold), and one injection of 224920 HU over
    the charged required=139 lands 293+floor(224920/139) = 1911 K — deliberately
    INSIDE the [1811, 2011) window: past the Fe melt (drain gate :455) and BELOW the
    WroughtIron anneal line, because MT.java:2394 pins WI at Fe.mMeltingPoint+200 =
    2011 K with .uumAloy(0, Fe, 1*U) — a molten iron pile at T >= 2011 anneals to
    WroughtIron (the r5 run proved it: 1U WI appeared during the 2493 K feeding
    race), and WI as the density-lightest then refuses both the drain (T < 2011 gate,
    and the bridge has no wrougtiron fluid) and the iron verdict. In [1811, 2011) the
    pile stays pure Iron: the bucket arm drains the lightest through the registered
    iron molten fluid (FluidBridge "iron" → gt6:iron_molten, 144 L/U) and the
    round-trip numbers stay integer-exact. The offline suite carries the meltdown
    latch arms (WARNING at T+100 > 2557 probed safe at 2493 K), so the live chain
    needs no WARNING rodeo — 1911 K rides meltdown=ok.
  - the stone rung melts TIN (Sn.mMeltingPoint = 505 K, MT.java:1046): the pour only
    transfers at T >= the melt point, so the green pour IS the melt proof; the mold
    then drifts -5 K/t below 505 and the shape pours out (the r5 run walked this
    whole path green: pour → solidify → 1x ingot_tin). 8U dropped → level=4, 1U
    poured → level=3 (integer-exact LIQUID_LEVEL buckets).
  So the row0 band splits faithfully:
  A  the STONE opening rung melts TIN: charge first (no box, no fire), then the live
     brick burning box under the crucible re-heats past 505 K → Stone Mold pour →
     solidify → 1x gt6:ingot_tin (the full walk went green in r5 once the feed
     stopped losing items to flames).
  B  the STEEL rung melts IRON inside the [1811, 2011) no-anneal window and the
     bucket round trip closes the molten path.

Determinism notes:
  - the suck feeds one item per tick (the :154 box + the single feed slot), so every
    drop step POLLS for the pile/level instead of assuming an instant landing.
  - LIQUID_LEVEL: 8U/16U x 8 = exactly 4.0 → level=4; after the 1U pour 7U → level=3
    (integer-exact in binary floating point).
  - the bucket round trip is integer-exact: drain = min(1000 L, units(8U→144)) = 1000 L,
    back-conversion floor(1000*U/144) = 4504500000 both ways → the pile returns to
    5189184000 = 8U (CS.U = 648648000, CS.java:49).
  - the ingot/bucket ids render NAMESPACED on both legs (Item.toString / getKey — the p25
    per-leg split rode ItemStack.toString, which this chain never prints).

passes=2 is the idempotency proof (the [0,0] of this chain).

Run:  python3 tools/rcon/chains/p26_crucible_row0.py
"""

import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
for _path in (str(_HERE), str(_HERE.parent)):
    if _path not in sys.path:
        sys.path.insert(0, _path)

import gt6world
from framework import Chain, Step, main, phase

F = gt6world.fmt

# The two crucible sites on the 126 band (clear of the p25 canner band z=124 x<=424 and
# the p24 bands 386-412). The stone site carries y=63 for the under-floor box; dy=1 keeps
# the cleanup/forceload box over both decks. dx/dz=3 cover the mold at (443, 126).
STONE = gt6world.Site(442, 63, 126, dx=3, dy=1, dz=3)  # box(63) + smeltery(64) + mold
STEEL = gt6world.Site(446, 64, 126, dx=3, dy=0, dz=3)  # steel smeltery (the iron face)
C = F((STONE.x, STONE.y + 1, STONE.z))  # 442 64 126 — the stone crucible
B = f"{STONE.x} {STONE.y} {STONE.z}"    # 442 63 126 — its brick burning box (under)
M = "443 64 126"                        # the stone mold, west-walk neighbour of the crucible
S = F(STEEL)                            # 446 64 126 — the steel crucible

steps = []

# ------------------------------------------------- A: the stone opening rung (the box melts tin)
steps += [
    phase("A: the stone opening rung — the charge lands before the box exists, the re-heat pours"),
    # the box's trySpreadFire drops real fire blocks for the whole burn window; with
    # doFireTick on they spread into the grass cover and burn terrain (observed: a run
    # lost BOTH the crucible and the box mid-heat). Suppressed for the chain's span,
    # restored in the teardown — the row0 band tests heat flow, not fire spread.
    Step("gamerule doFireTick false", expect="doFireTick"),
    Step(f"gt6crucible place {C} stone", expect="GT6 smeltery placed at 442, 64, 126: stone"),
    # the fresh readback: env temperature (286 plains / 293 default) rides in the 200s
    Step(f"gt6crucible stat {C}", expect="temp=2"),
    # 8x dust = 8U of Sn, fed one item per tick into the COLD crucible (no drag, no
    # fire — the box does not exist yet). level=4 = 8U/16U x 8, integer-exact.
    Step(f"gt6crucible drop {C} dust tin 8", expect="dropped 8x dust"),
    Step(f"gt6crucible stat {C}", expect="level=4", poll=30.0),
    # the Stone Mold, adjacent west (the :271-289 walk finds the crucible on WEST)
    Step(f"gt6crucible place-mold {M} stone", expect="GT6 mold placed at 443, 64, 126: stone"),
    # NOW the box: under-floor, top-face emit against the crucible bottom (Loader :519)
    Step(f"gt6burner place {B} brick_burning_box", expect="GT6 burning box placed"),
    Step(f"gt6burner stat {B}", expect="efficiency=2500/10000"),
    # 20 coal = 50000 HU of burn — the full re-heat window rides well inside that
    Step(f"gt6burner fuel {B} minecraft:coal 20", expect="minecraft:coal x20"),
    # the box handshake proof rides the live BE: max=1375 is the always-true alive check
    Step(f"gt6crucible stat {C}", expect="max=1375"),
    Step(f"gt6burner ignite {B}", expect="burning=true"),
    # re-heat: the charged shell (~76) climbs 293→505 at ~+0.21 K/t (~1000 t); the pour
    # only transfers at T >= the Sn melt point (:498-507) — the poll IS the proof
    Step(f"gt6crucible pour {M}", expect="content=Tin x", poll=150.0),
    Step(f"gt6crucible cool {C}", expect="GT6 crucible supply cut"),
    # 7U left in the pile → the LIQUID_LEVEL bucket drops 4 → 3 (integer-exact)
    Step(f"gt6crucible stat {C}", expect="level=3"),
    # the mold drifts -5 K/t toward the env; below 505 the shape pours out (:189-203).
    # The mold stat prints Item.toString — plain "ingot_tin", no namespace (observed).
    Step(f"gt6crucible mold {M}", expect="output=1x ingot_tin", poll=30.0),
]

# ------------------------------------------------- B: the steel rung (the iron face + the bucket path)
steps += [
    phase("B: the steel rung — a cold 8U iron charge, one injection into the melt window, the barrel round-trip"),
    Step(f"gt6crucible place {S} steel", expect="GT6 smeltery placed at 446, 64, 126: steel"),
    Step(f"gt6crucible stat {S}", expect="temp=2"),
    # the Supplier fix made the shell material bite: the steel ceiling, not the Stone fallback
    Step(f"gt6crucible stat {S}", expect="max=2557"),
    # 8U of Fe into the COLD crucible — no :333 drag, no anneal race (nothing is hot)
    Step(f"gt6crucible drop {S} dust iron 8", expect="dropped 8x dust"),
    Step(f"gt6crucible stat {S}", expect="content: Iron x", poll=10.0),
    Step(f"gt6crucible stat {S}", expect="level=4", poll=10.0),
    # ONE injection over the charged shell (required=139, r5-probed exact): 224920/139
    # = 1618 conversions → 293+1618 = 1911 K — past the 1811 K Fe melt, below the
    # 2011 K WroughtIron anneal line (MT.java:2394, Fe.meltingPoint+200): the pile
    # stays pure Iron, which keeps the lightest drainable through the iron bridge.
    Step(f"gt6crucible inject-hu {S} 224920", expect="GT6 crucible injected 224920 HU"),
    Step(f"gt6crucible stat {S}", expect="temp=19", poll=10.0),
    # the anneal detector: the FULL 8U must still answer as Iron — a single WroughtIron
    # unit would split this line (the r5 red run proved the split is real at >= 2011 K)
    Step(f"gt6crucible stat {S}", expect="content: Iron x 5189184000u"),
    # the molten path in ONE round trip: drain 1000 L into the wood barrel (the pile dips
    # to 684684000) and pour it back (integer-exact — the pile returns to 8U = 5189184000);
    # the drain gate (:455) demands T >= the melt point, so this is the melt verdict too
    Step(f"gt6crucible bucket {S} gt6:barrel_wood",
         expect="pile=684684000u | poured back from gt6:barrel_wood -> gt6:barrel_wood, pile=5189184000u"),
]

# ------------------------------------------------- D: teardown
steps += [
    phase("D: teardown — the explicit band restore (the bbox pass is the structural backstop)"),
    Step("fill 438 62 122 450 67 130 air", expect="filled"),
    Step("gamerule doFireTick true", expect="doFireTick"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p26-crucible-row0",
    slug="p26crucrow0",
    sites=gt6world.declare_sites(STONE, STEEL),
    preferred_ports=(26130, 26140),      # this card's pinned rcon/query pair (after p25foodcan 26110/26120)
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
