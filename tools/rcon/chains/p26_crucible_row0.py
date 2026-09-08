#!/usr/bin/env python3
"""p26-crucible-row0 — the crucible-chain live acceptance (task p26-crucible-physics-smeltery,
the row0 band: place → the brick burning box feeds HU → drop → MELT → mold pour → ingot;
the bucket arm round-trips the molten path).

PHYSICS BOUNDARY (upstream-faithful, the card-face adaptation — every constant below is
pinned by the upstream evidence, not by choice):
  - the stone shell ceiling = MT.Stone.mMeltingPoint(1100) x 1.25 = 1375 K
    (MultiTileEntitySmeltery.java:361 HEAT_RESISTANCE_BONUS; MT.java:1631 .heat(1100)),
  - Fe.mMeltingPoint = 1811 K (MT.java:996 iron) — a STONE crucible cannot melt iron:
    at 1376 K the shell trashes and goes lava (:315-322), and a stone MOLD (same 1375 K
    ceiling, MultiTileEntityMold.java:75) dies on an 1811 K charge; upstream casts iron in
    the Bronze/Invar/Steel mold rungs (Loader_MultiTileEntities.java:361-363 — card-B).
  So the row0 band splits faithfully:
  A  the STONE opening rung melts TIN (Sn.mMeltingPoint 505 K, MT.java:1046) with the live
     brick burning box → Stone Mold pour → solidify → 1x gt6:ingot_tin.
  B  the STEEL rung (ceiling 2046x1.25 = 2557 K, MT.java:2444) melts IRON: inject 2200 HU
     (the burning-box packet stream compressed into one call — the driver's own semantic)
     lands 2486..2493 K → the mMeltDown WARNING latch engages live (T+100 > 2557) while the
     shell holds → 8x dust iron melts in → the bucket arm drains the molten iron into a
     gt6:barrel_wood and pours it back (the only registered molten fluid is iron —
     FluidBridge.java:45; the barrel is the fluid carrier, the p12 item-capability face).

Determinism notes:
  - the melt window: the box emits 16 HU/t (the Brick row, Loader :519); the small shell +
    8U content weight keeps requiredEnergy at 1 → +16 K/t at ~19 tps. sleep 1.5 lands
    734..1176 K — above the Sn melt (505), below the WARNING latch (1275) and the shell
    ceiling (1375) with >=100 K margin both ways. The POUR is the melt verdict:
    fillMoldAtSide (:498-507) only pours at T >= the melting point.
  - the steel site: inject 2200 on a fresh crucible = env + 2200 >= 2486 K deterministic
    (conversions = buffer / requiredEnergy = 2200/1 in one tick, :301-309); WARNING without
    death needs env > 257 K — any real biome.
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
# the p24 bands 386-412). dx/dz=3 cover the boxes at z=125 and the mold at (443, 126).
STONE = gt6world.Site(442, 64, 126, dx=3, dy=2, dz=3)  # stone smeltery + brick box + mold
STEEL = gt6world.Site(446, 64, 126, dx=3, dy=2, dz=3)  # steel smeltery (the iron face)
C = F(STONE)      # 442 64 126 — the stone crucible
B = "442 64 125"  # its brick burning box
M = "443 64 126"  # the stone mold, west-walk neighbour of the crucible
S = F(STEEL)      # 446 64 126 — the steel crucible

steps = []

# ------------------------------------------------- A: the stone opening rung (the box melts tin)
steps += [
    phase("A: the stone opening rung — the live brick box heats, the dust feeds, the melt pours"),
    Step(f"gt6crucible place {C} stone", expect="GT6 smeltery placed at 442, 64, 126: stone"),
    # the fresh readback: env temperature (286 plains / 293 default) rides in the 200s
    Step(f"gt6crucible stat {C}", expect="temp=2"),
    Step(f"gt6burner place {B} brick_burning_box", expect="GT6 burning box placed"),
    # the Brick row: efficiency=2500/10000, out 16 HU/t (Loader :519)
    Step(f"gt6burner stat {B}", expect="efficiency=2500/10000"),
    Step(f"gt6burner fuel {B} minecraft:coal 4", expect="minecraft:coal x4"),
    Step(f"gt6burner ignite {B}", expect="burning=true", sleep=1.5),
    # 8x dust = 8U of Sn — feeds one item per tick (the :154 suck + the :179-183 prefix arm)
    Step(f"gt6crucible drop {C} dust tin 8", expect="dropped 8x dust"),
    # the burn window (the determinism note above) landed 734..1176 K; freeze the site
    Step(f"gt6burner extinguish {B}", expect="burning=false"),
    Step(f"gt6crucible cool {C}", expect="GT6 crucible supply cut"),
    # the Stone Mold, adjacent (the mold's :271-289 walk finds the crucible on WEST)
    Step(f"gt6crucible place-mold {M} stone", expect="GT6 mold placed at 443, 64, 126: stone"),
    # THE MELT VERDICT: the pour only transfers at T >= the Sn melting point (:498-507)
    Step(f"gt6crucible pour {M}", expect="content=Tin x"),
    # 7U left in the pile → the LIQUID_LEVEL bucket drops 4 → 3 (integer-exact)
    Step(f"gt6crucible stat {C}", expect="level=3"),
    # the mold drifts -5 K/t toward the env; below 505 the shape pours out (:189-203)
    Step(f"gt6crucible mold {M}", expect="output=1x gt6:ingot_tin", poll=20.0),
]

# ------------------------------------------------- B: the steel rung (the iron face + the bucket path)
steps += [
    phase("B: the steel rung — 2200 HU lands the WARNING latch, iron melts, the barrel round-trips"),
    Step(f"gt6crucible place {S} steel", expect="GT6 smeltery placed at 446, 64, 126: steel"),
    Step(f"gt6crucible stat {S}", expect="temp=2"),
    # env + 2200 >= 2486 K: over the WARNING line (T+100 > 2557), under the shell (2557)
    Step(f"gt6crucible inject-hu {S} 2200", expect="GT6 crucible injected 2200 HU"),
    Step(f"gt6crucible stat {S}", expect="meltdown=WARNING"),
    Step(f"gt6crucible stat {S}", expect="max=2557K"),
    # 8U of Fe at 2486 K: instantly past the 1811 K melt, LIQUID_LEVEL lands exactly 4
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
