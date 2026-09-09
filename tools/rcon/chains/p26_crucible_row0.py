#!/usr/bin/env python3
"""p26-crucible-row0 — the crucible-chain live acceptance (task p26-crucible-physics-smeltery,
the row0 band: place → the brick burning box feeds HU → drop → MELT → mold pour → ingot;
the bucket arm round-trips the molten path).

PHYSICS BOUNDARY (live-probed on this branch, r4 takeover — every constant below is
pinned by upstream evidence AND by the probe boots, not by choice):
  - the burning box is emit-gate=top-face-only (gt6burner stat says so), so the box
    sits UNDER the crucible (y=63), its top face against the crucible bottom — the
    upstream opening chain's stack form (Loader :519, the box→smeltery pair).
  - the shell heat mass is real: the stone shell weighs ~6xx kg (weight(U*7)) so
    tRequiredEnergy = 1 + weight/100 = 7 (probed: 2200 HU into a fresh crucible lands
    +314 K = 2200/7 conversions; upstream :301). The box emits 16 HU/t → +2.29 K/t.
  - the stone shell ceiling = MT.Stone.mMeltingPoint(1100) x 1.25 = 1375 K
    (MultiTileEntitySmeltery.java:361 HEAT_RESISTANCE_BONUS; MT.java:1631 .heat(1100)),
  - Sn.mMeltingPoint = 505 K (MT.java:1046): the stone rung melts TIN. Iron (1811 K)
    belongs to the STEEL rung (ceiling 2046x1.25 = 2557 K, MT.java:2444) — a stone
    shell dies at 1376 K on an 1811 K charge; upstream casts iron in the Bronze/Steel
    mold rungs (Loader_MultiTileEntities.java:361-363 — card-B).
  So the row0 band splits faithfully:
  A  the STONE opening rung melts TIN with the live brick burning box → Stone Mold
     pour → solidify → 1x gt6:ingot_tin. Ignite window: sleep 6 s ≈ 114-120 t at
     ~19-20 tps → +261..275 K over env(286) → 547..561 K; the post-extinguish stat
     (supply-cut hold, cooldown 100 t) reads a stable 5xx — above the 505 melt,
     below the 1375 ceiling with huge margin, and the 5xx band check self-verifies
     the window without racing the poll loop.
  B  the STEEL rung melts IRON: inject 15400 HU into the FRESH crucible (required=7
     while empty → exactly 15400/7 = 2200 K of conversions → 293+2200 = 2493 K) —
     inside the WARNING latch band (T+100 = 2593 > 2557) while the shell holds
     (2493 <= 2557). Then 8x dust iron feeds in (the :334-347 cold-add keeps them in
     the pile; the :271-274 phase loop melts them at 2493 >= 1811) → the bucket arm
     drains the molten iron into a gt6:barrel_wood and pours it back (the only
     registered molten fluid is iron — FluidBridge.java:45; the barrel is the fluid
     carrier, the p12 item-capability face). The drain gate (:412) demands
     mTemperature >= the melt point, so the round trip doubles as the melt verdict.

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
B = f"{STONE.x} {STONE.y} {STONE.z}"  # 442 63 126 — its brick burning box (under)
M = "443 64 126"                      # the stone mold, west-walk neighbour of the crucible
S = F(STEEL)                          # 446 64 126 — the steel crucible

steps = []

# ------------------------------------------------- A: the stone opening rung (the box melts tin)
steps += [
    phase("A: the stone opening rung — the live brick box heats, the dust feeds, the melt pours"),
    Step(f"gt6crucible place {C} stone", expect="GT6 smeltery placed at 442, 64, 126: stone"),
    # the fresh readback: env temperature (286 plains / 293 default) rides in the 200s
    Step(f"gt6crucible stat {C}", expect="temp=2"),
    # the box UNDER the crucible: top-face-only emit (gt6burner stat) against the bottom face
    Step(f"gt6burner place {B} brick_burning_box", expect="GT6 burning box placed"),
    # the Brick row: efficiency=2500/10000, out 16 HU/t (Loader :519)
    Step(f"gt6burner stat {B}", expect="efficiency=2500/10000"),
    Step(f"gt6burner fuel {B} minecraft:coal 4", expect="minecraft:coal x4"),
    Step(f"gt6burner ignite {B}", expect="burning=true", sleep=6.0),
    # +2.29 K/t over 114-120 t lands 547..561 K; the supply-cut hold makes this stat stable
    Step(f"gt6burner extinguish {B}", expect="burning=false"),
    Step(f"gt6crucible stat {C}", expect="temp=5"),
    # 8x dust = 8U of Sn — feeds one item per tick, so poll the level bucket (4 = 8U/16U x 8)
    Step(f"gt6crucible drop {C} dust tin 8", expect="dropped 8x dust"),
    Step(f"gt6crucible stat {C}", expect="level=4", poll=30.0),
    Step(f"gt6crucible cool {C}", expect="GT6 crucible supply cut"),
    # the Stone Mold, adjacent (the mold's :271-289 walk finds the crucible on WEST)
    Step(f"gt6crucible place-mold {M} stone", expect="GT6 mold placed at 443, 64, 126: stone"),
    # THE MELT VERDICT: the pour only transfers at T >= the Sn melting point (:498-507)
    Step(f"gt6crucible pour {M}", expect="content=Tin x"),
    # 7U left in the pile → the LIQUID_LEVEL bucket drops 4 → 3 (integer-exact)
    Step(f"gt6crucible stat {C}", expect="level=3"),
    # the mold drifts -5 K/t toward the env; below 505 the shape pours out (:189-203)
    Step(f"gt6crucible mold {M}", expect="output=1x gt6:ingot_tin", poll=30.0),
]

# ------------------------------------------------- B: the steel rung (the iron face + the bucket path)
steps += [
    phase("B: the steel rung — 15400 HU lands the WARNING latch, iron melts, the barrel round-trips"),
    Step(f"gt6crucible place {S} steel", expect="GT6 smeltery placed at 446, 64, 126: steel"),
    Step(f"gt6crucible stat {S}", expect="temp=2"),
    Step(f"gt6crucible stat {S}", expect="max=2557K"),
    # fresh-shell required=7 → 15400/7 = 2200 K → ~2493 K: WARNING (2593 > 2557) without death
    Step(f"gt6crucible inject-hu {S} 15400", expect="GT6 crucible injected 15400 HU"),
    Step(f"gt6crucible stat {S}", expect="meltdown=WARNING", poll=10.0),
    # 8U of Fe at ~2493 K: past the 1811 K melt, LIQUID_LEVEL lands exactly 4
    Step(f"gt6crucible drop {S} dust iron 8", expect="dropped 8x dust"),
    Step(f"gt6crucible stat {S}", expect="content: Iron x", poll=10.0),
    # the drop feeds one item per tick — poll until all 8 landed (8U/16U x 8 = exactly 4)
    Step(f"gt6crucible stat {S}", expect="level=4", poll=10.0),
    # the molten path in ONE round trip: drain 1000 L into the wood barrel (the pile dips to
    # 684684000) and pour it back (integer-exact — the pile returns to 8U = 5189184000)
    Step(f"gt6crucible bucket {S} gt6:barrel_wood",
         expect="pile=684684000u | poured back from gt6:barrel_wood -> gt6:barrel_wood, pile=5189184000u"),
]

# ------------------------------------------------- D: teardown
steps += [
    phase("D: teardown — the explicit band restore (the bbox pass is the structural backstop)"),
    Step("fill 438 62 122 450 67 130 air", expect="filled"),
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
