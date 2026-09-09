#!/usr/bin/env python3
"""p26-crucible-mold-faucet — the ceramic-mold + Faucet live acceptance chain.

Task p26-crucible-mold-faucet ACCEPTANCE (c), translated:

  A the pour rig: steel crucible (the 1375 K stone ceiling cannot hold 1811 K iron —
    the shell ladder IS the gate, temperatureMax = shell.melting x 1.25) + the
    CERAMIC faucet mounted on its east wall (facing west, the mount face toward the
    host) + the pre-carved ceramic PLATE mold under the faucet.
  B the melt: iron dust dropped on top (the :154 suck arm), then TWO inject-hu shots.
    r7 recalibration (the live-leg evidence, /tmp/p26mf-rcon-forge.log of the
    pre-r7 run): the steel shell alone sets the heat mass — E/K = 1 + 7000/100 =
    71 HU/K (the content is NOT yet in the mass when the shot lands), so shot 1
    130000 HU lands 287 + 130000/71 ~= 2118 K and shot 2 +22000 HU ~= +310 K
    into ~2400 K, both inside the [1811, 2557) pour window. The stat polls pin
    the "temp=2" band (2.0-3.0 kK, ~900 K of headroom) instead of the stale
    1.6/1.8 kK magic numbers; the WINDOW itself is proven by the pour arms —
    fillMoldAtSide refuses outside it, so "poured into the mold" / "output=1x"
    fail if the melt is cold. A meltdown prints the FAILED marker and auto-fails.
  C the cast: the faucet right-click (the :138-146 activation, the null-player RCON
    arm) polls until "poured into the mold" — the crucible pays one plate of molten
    iron through fillMoldAtSide -> fillMold (the DOWN walk) — then the mold stat
    polls until the solidified "output=1x" (the ±5 K/tick cooldown crosses the
    1811 K freeze in ~10 ticks; iron is NOT COOL2CRYSTAL so the prefix stays plate).
  D the monkey-wrench auto-pull rig: a second crucible + mold pair, NO faucet —
    the crucible stands at the MOLD's own y, directly WEST of it (the auto-pull
    queries getAdjacentTileEntity of the armed side — the pre-r7 rig had the
    crucible one block UP, so the mold's west neighbour was air and the pull
    never found a crucible), wrench-mold on the mold's WEST sub-side (the :347
    SBIT toggle) arms the :170-176 pull; the mold stat polls until the
    solidified "output=1x" — plate requires exactly CS.U = 648648000u and the
    crucible holds exactly that, so the fill completes and freezes (5 K/tick
    cooldown) within the poll window. output=1x on a faucet-less rig is only
    reachable through fillMoldAtSide — the pull itself is the proven path.
  E the vanilla furnace: a furnace block data-merged with the RAW clay mold + coal
    and a 199/200 CookTime head start — the poll reads the FORMED mold out of slot
    2 (the vanilla-smelting JSON face; the raw item id in the merge is the
    /give-ability proof — a headless console has no inventory to give into, the
    declared substitution).
  F teardown.

The per-leg split rides node_cmds/node_expects: the 1.20.1 furnace NBT keys
(BurnTime/CookTime shorts, Count:1b) vs the 1.21.1 component shapes
(lit_time_remaining/cooking_progress ints, count:1).

passes=2 is the idempotency proof.

Run:  GT6_SESSION=off python3 tools/rcon/chains/p26_mold_faucet.py
      GT6_SESSION=off python3 tools/rcon/chains/p26_mold_faucet.py --node 1.21.1-neoforge
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

# the rig band (clear of the p24 bands 386-412 and the p25 band 419-425)
C  = F(gt6world.Site(431, 64, 134))   # the steel crucible (rig A)
FA = F(gt6world.Site(432, 64, 134))   # the ceramic faucet on the east wall
M  = F(gt6world.Site(432, 63, 134))   # the plate mold UNDER the faucet
C2 = F(gt6world.Site(435, 63, 138))   # the auto-pull crucible (rig B) — the MOLD's y, west-adjacent
M2 = F(gt6world.Site(436, 63, 138))   # the auto-pull mold EAST of its crucible
FU = F(gt6world.Site(430, 64, 138))   # the vanilla furnace

SITE_A = gt6world.Site(431, 64, 134, dx=1, dy=1)   # covers the crucible + faucet + mold
SITE_B = gt6world.Site(435, 63, 138, dx=1, dy=1)   # covers crucible 2 + mold 2 (both at y=63)
SITE_F = gt6world.Site(430, 64, 138)               # the furnace

steps = []

# ------------------------------------------------- A: the pour rig placement
steps += [
    phase("A: the pour rig — steel crucible + ceramic faucet (east wall, facing west) + plate mold below"),
    Step(f"gt6crucible place {C} smeltery_steel", expect="GT6 smeltery placed"),
    Step(f"gt6crucible drop {C} dust iron 4", expect="GT6 dropped"),
    Step(f"gt6faucet place {FA} faucet_ceramic west", expect="GT6 faucet placed"),
    Step(f"gt6crucible place-mold {M} mold_ceramic_plate", expect="GT6 mold placed"),
    Step(f"gt6faucet stat {FA}", expect="facing=west", sleep=1),
]

# ------------------------------------------------- B: the anchored melt
steps += [
    phase("B: the melt — shot 1 ~+1830 K (E/K = 71 HU/K, steel shell), lands ~2.1 kK"),
    Step(f"gt6crucible inject-hu {C} 130000", expect="GT6 crucible injected"),
    Step(f"gt6crucible stat {C}", expect="temp=2", poll=20),
    phase("B2: shot 2 ~+310 K deeper into the [1811, 2557) pour window, ~2.4 kK"),
    Step(f"gt6crucible inject-hu {C} 22000", expect="GT6 crucible injected"),
    Step(f"gt6crucible stat {C}", expect="temp=2", poll=20),
]

# ------------------------------------------------- C: the cast
steps += [
    phase("C: the faucet right-click — the pour polls until the crucible pays one plate"),
    Step(f"gt6faucet use {FA}", expect="poured into the mold", poll=30),
    Step(f"gt6crucible mold {M}", expect="prefix=plate", sleep=2),
    Step(f"gt6crucible mold {M}", expect="output=1x", poll=30),
]

# ------------------------------------------------- D: the monkey-wrench auto-pull rig
steps += [
    phase("D: rig B — the mold wrench-armed to auto-pull from its west neighbour every 20t"),
    Step(f"gt6crucible place {C2} smeltery_steel", expect="GT6 smeltery placed"),
    Step(f"gt6crucible drop {C2} dust iron 4", expect="GT6 dropped"),
    Step(f"gt6crucible place-mold {M2} mold_ceramic_plate", expect="GT6 mold placed"),
    Step(f"gt6crucible inject-hu {C2} 130000", expect="GT6 crucible injected"),
    Step(f"gt6crucible stat {C2}", expect="temp=2", poll=20),
    Step(f"gt6crucible inject-hu {C2} 22000", expect="GT6 crucible injected"),
    Step(f"gt6crucible stat {C2}", expect="temp=2", poll=20),
    Step(f"gt6faucet wrench-mold {M2} west", expect="Crucible Auto-Input: ON"),
    Step(f"gt6crucible mold {M2}", expect="output=1x", poll=30),
    Step(f"gt6faucet softhammer-mold {M2}", expect="Crucible Auto-Input: OFF & NO REDSTONE"),
]

# ------------------------------------------------- E: the vanilla furnace
FURNACE_SET = {
    "1.20.1": (f'setblock {FU} minecraft:furnace{{Items:[{{Slot:0b,id:"gt6:mold_ceramic_plate_raw",Count:1b}},'
               f'{{Slot:1b,id:"minecraft:coal",Count:2b}}],BurnTime:1600s,CookTime:199s,CookingTotalTime:200s}}'),
    "1.21.1": (f'setblock {FU} minecraft:furnace{{Items:[{{Slot:0b,id:"gt6:mold_ceramic_plate_raw",count:1}},'
               f'{{Slot:1b,id:"minecraft:coal",count:2}}],lit_time_remaining:1600,cooking_progress:199,cooking_total_time:200}}'),
}
steps += [
    phase("E: the vanilla furnace hardens the raw clay mold (the tier-a smelting JSON face)"),
    Step(FURNACE_SET["1.20.1"], expect="Changed the block at",
         node_cmds={"1.21.1": FURNACE_SET["1.21.1"]}),
    Step(f"data get block {FU}", expect="gt6:mold_ceramic_plate_raw", poll=10),
    Step(f"data get block {FU}", expect="gt6:mold_ceramic_plate", poll=30),
]

# ------------------------------------------------- F: teardown
steps += [
    phase("F: teardown — the entity sweep + the explicit band restore"),
    # the item sweep is NOT cosmetic: the suck arm pulls 1 item per tick and the
    # drop leaves unconsumed dust hovering over the rig; a pass-2 crucible then
    # feeds the leftovers (4U content) and the heavier heat mass drops shot 2
    # BELOW the 1811 K pour window (the pre-r7.2 pass-2 red, forge leg). The
    # volume selector rides the same grammar on both legs.
    Step(f"kill @e[type=minecraft:item,x=429,y=62,z=133,dx=9,dy=5,dz=7]"),
    Step("fill 429 62 133 438 66 139 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p26-crucible-mold-faucet",
    slug="p26moldfaucet",
    sites=gt6world.declare_sites(SITE_A, SITE_B, SITE_F),
    preferred_ports=(26134, 26144),   # this card's pinned rcon/query pair (after the p25 26110/26120 segment)
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
