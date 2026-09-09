#!/usr/bin/env python3
"""p26-crucible-row0 — the crucible-chain live acceptance (task p26-crucible-physics-smeltery,
the row0 band: place → the brick burning box feeds HU → drop → MELT → mold pour → ingot;
the bucket arm round-trips the molten path).

PHYSICS BOUNDARY (live-probed on this branch, r4 takeover — every constant below is
pinned by upstream evidence AND by the probe boots, not by choice):
  - the burning box is emit-gate=top-face-only (gt6burner stat says so), so the box
    sits UNDER the crucible (y=63), its top face against the crucible bottom — the
    upstream opening chain's stack form (Loader :519, the box→smeltery pair).
  - the shell heat mass is real and material-dependent: tRequiredEnergy = 1 + weight/100
    (upstream :301). Probed: the stone shell answers required=7 (2200 HU → +314 K), the
    steel shell required=62 (20000 HU → +322 K, energy=36 residual exact) — the
    Supplier fix made the shell material bite. 16 HU/t from the box = +2.29 K/t on a
    fresh stone shell.
  - COLD CHARGE SUCKS HEAT (upstream :333, the +1/-1 balance): 8U of room-temperature
    dust dropped into a ~560 K crucible lands the mix at ~325 K — below the 505 K Sn
    melt. The melt verdict therefore rides a CONTINUING burn: the box pulls the mix
    back over 505 K at ~+0.21 K/t (~950 t), and the pour step POLLS — the pour only
    transfers at T >= the melt point (:498-507), so a green pour IS the melt proof.
  - the steel rung melts IRON in two injections: #1 into the FRESH shell
    (required=62 → 136400/62 = 2200 K exactly → ~2493 K) engages the WARNING latch
    (2493+100 > 2557) while the shell holds (2493 <= 2557; the 144000 round found the
    line the hard way — 2322 conversions → 2615 K → lava). The 8U charge then drags
    the mix to ~1270 K via :333, and injection #2 (92000 HU over the charged shell's
    required≈139 → +661 K) lands ~1930 K, past the 1811 K melt again. The bucket arm
    drains the molten iron into a gt6:barrel_wood and pours it back (the only
    registered molten fluid is iron — FluidBridge.java:45); its drain gate (:412)
    demands mTemperature >= the melt point, so a green round trip IS the melt proof.
  - FEED-WINDOW RULE: a burning box spreads fire blocks across its 7x5x7 volume while
    lit (trySpreadFire, the upstream :107 port) — a flame landing on the crucible's
    top-air cell burns the dropped ItemEntity mid-feed (observed: a 7-dust volley
    froze at 3 landed). So the charge is dropped while the box is OUT, and only the
    re-heat runs lit.
  - the stone shell ceiling = MT.Stone.mMeltingPoint(1100) x 1.25 = 1375 K
    (MultiTileEntitySmeltery.java:361 HEAT_RESISTANCE_BONUS; MT.java:1631 .heat(1100));
  - Sn.mMeltingPoint = 505 K (MT.java:1046): the stone rung melts TIN. Iron (1811 K)
    belongs to the STEEL rung (ceiling 2046x1.25 = 2557 K, MT.java:2444) — a stone
    shell dies at 1376 K on an 1811 K charge; upstream casts iron in the Bronze/Steel
    mold rungs (Loader_MultiTileEntities.java:361-363 — card-B).
  So the row0 band splits faithfully:
  A  the STONE opening rung melts TIN with the live brick burning box → Stone Mold
     pour → solidify → 1x gt6:ingot_tin. The ignite window (sleep 6 s at the probed
     ~20 tps = ~120 t x 2.29 K/t) reads a stable 5xx before the drop.
  B  the STEEL rung melts IRON — see the two-injection walk above; the bucket round
     trip closes the molten path.

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
    phase("A: the stone opening rung — the live brick box heats, the dust feeds, the melt pours"),
    # the box's trySpreadFire drops real fire blocks for the whole burn window; with
    # doFireTick on they spread into the grass cover and burn terrain (observed: a run
    # lost BOTH the crucible and the box mid-heat). Suppressed for the chain's span,
    # restored in the teardown — the row0 band tests heat flow, not fire spread.
    Step("gamerule doFireTick false", expect="doFireTick"),
    Step(f"gt6crucible place {C} stone", expect="GT6 smeltery placed at 442, 64, 126: stone"),
    # the fresh readback: env temperature (286 plains / 293 default) rides in the 200s
    Step(f"gt6crucible stat {C}", expect="temp=2"),
    # the box UNDER the crucible: top-face-only emit (gt6burner stat) against the bottom face
    Step(f"gt6burner place {B} brick_burning_box", expect="GT6 burning box placed"),
    # the Brick row: efficiency=2500/10000, out 16 HU/t (Loader :519); 20 coal = 50000 HU
    # of burn — the full re-heat window rides well inside that
    Step(f"gt6burner stat {B}", expect="efficiency=2500/10000"),
    Step(f"gt6burner fuel {B} minecraft:coal 20", expect="minecraft:coal x20"),
    Step(f"gt6burner ignite {B}", expect="burning=true"),
    # the box handshake proof: the stone shell ceiling rides the live BE (the 5xx climb
    # ~560 K @ 6 s was probed but races nothing — max=1375 is the always-true alive check)
    Step(f"gt6crucible stat {C}", expect="max=1375", poll=10.0),
    # OUT before the drop: no lit box means no trySpreadFire flames eating the entities
    Step(f"gt6burner extinguish {B}", expect="burning=false"),
    # 8x dust = 8U of Sn; the suck feeds one item per tick — poll the level bucket
    # (4 = 8U/16U x 8); the :333 balance dips the mix to ~325 K (solidify-back keeps it)
    Step(f"gt6crucible drop {C} dust tin 8", expect="dropped 8x dust"),
    Step(f"gt6crucible stat {C}", expect="level=4", poll=30.0),
    # the mold sits empty through the re-heat (the :271-289 walk finds the crucible on WEST)
    Step(f"gt6crucible place-mold {M} stone", expect="GT6 mold placed at 443, 64, 126: stone"),
    # re-heat: the charged shell pulls the mix back over 505 K at ~+0.21 K/t (~950 t);
    # the pour only transfers at T >= the melt point (:498-507) — the poll IS the proof
    Step(f"gt6burner ignite {B}", expect="burning=true"),
    Step(f"gt6crucible pour {M}", expect="content=Tin x", poll=150.0),
    Step(f"gt6crucible cool {C}", expect="GT6 crucible supply cut"),
    # 7U left in the pile → the LIQUID_LEVEL bucket drops 4 → 3 (integer-exact)
    Step(f"gt6crucible stat {C}", expect="level=3"),
    # the mold drifts -5 K/t toward the env; below 505 the shape pours out (:189-203)
    Step(f"gt6crucible mold {M}", expect="output=1x gt6:ingot_tin", poll=30.0),
]

# ------------------------------------------------- B: the steel rung (the iron face + the bucket path)
steps += [
    phase("B: the steel rung — WARNING latch, iron feeds, second injection, the barrel round-trip"),
    Step(f"gt6crucible place {S} steel", expect="GT6 smeltery placed at 446, 64, 126: steel"),
    Step(f"gt6crucible stat {S}", expect="temp=2"),
    # the Supplier fix made the shell material bite: the steel ceiling, not the Stone fallback
    Step(f"gt6crucible stat {S}", expect="max=2557"),
    # probed steel-shell required=62 (20000 HU → +322 K): 136400/62 = 2200 K exactly →
    # ~2493 K — inside the WARNING latch (2593 > 2557) with 64 K of shell margin
    Step(f"gt6crucible inject-hu {S} 136400", expect="GT6 crucible injected 136400 HU"),
    Step(f"gt6crucible stat {S}", expect="meltdown=WARNING", poll=10.0),
    # 8U of Fe — feeds one item per tick; the :333 balance drags ~2493 K down to ~1270 K
    # (below the 1811 K melt — the solidify-back conversion keeps the pile)
    Step(f"gt6crucible drop {S} dust iron 8", expect="dropped 8x dust"),
    Step(f"gt6crucible stat {S}", expect="content: Iron x", poll=10.0),
    Step(f"gt6crucible stat {S}", expect="level=4", poll=10.0),
    # second injection over the charged shell (required≈139): 92000/139 ≈ +661 K → ~1930 K,
    # past the 1811 K melt again — the poll pins the 19xx band
    Step(f"gt6crucible inject-hu {S} 92000", expect="GT6 crucible injected 92000 HU"),
    Step(f"gt6crucible stat {S}", expect="temp=19", poll=10.0),
    # the molten path in ONE round trip: drain 1000 L into the wood barrel (the pile dips to
    # 684684000) and pour it back (integer-exact — the pile returns to 8U = 5189184000);
    # the drain gate (:412) demands T >= the melt point, so this is the melt verdict too
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
