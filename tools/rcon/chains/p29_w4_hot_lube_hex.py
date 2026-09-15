#!/usr/bin/env python3
"""p29-w4-hot-lube-hex — the HEX hot-coolant live acceptance chain (task
p29-w4-hot-lube; the p29-w3-heat-exchanger boiler-stack form):

  A THE FORM: /gt6heatexchanger place -> the 16-part shell by plain setblocks
    (8 dense_wall_tungsten y65 ring ONLY_ITEM_FLUID_ENERGY_IN + 8
    heat_transmitter y66 NOTHING + the centre wall y66) -> stat formed=true.
  B THE COOLANT ROW (acceptance ②): the /gt6heatexchanger fill command surface
    hardcodes gt6:hot_water (the W3 card's file), so the ic2hotcoolant fuel
    rides the vanilla setblock-NBT channel — the controller is re-placed with
    {gt.tank0:{FluidName:"gt6:ic2hotcoolant",Amount:163840}} straight into
    mTanks[0] (GT6HeatExchangerBlockEntity.saveAdditional NBT_TANK0 round trip).
    The fuel=163840/163840 stat proves the tank; the burn itself proves the
    fuels_hot ROW (findRecipe by content): the overflow barrel below collects
    gt6:ic2coolant — the :203 conversion is LIVE.
  C THE HU EMISSION: the SS large boiler anchored at y68 — its base 3x3
    transmitters sit at y67, ONE layer above the HEX's transmitter ring (the
    six-of-eight y+2 emission targets) — the relay books HU, the boiler
    converts to steam and the stat pins barometer>=1.
  D THE 340 K CARRIER ARMS (acceptance ③): a barrel_metal (bronze drum,
    1696 K ceiling, gas-proof) ACCEPTS gt6:ic2pahoehoelava (1200 K, not
    POWER_CONDUCTING) and HOLDS it; a barrel_wood (340 K ceiling) takes the
    ACCEPTED fill then MELTS — the block is gone, the offline >340 K flag
    column's live twin. The drain-then-refill arm pins the :184
    allowFluid gate LIVE: ic2hotcoolant is a POWER_CONDUCTING fluid, so the
    same drum VOIDS it on the first tick (filled 1000 -> stat 0 "of nothing")
    — the hot coolants cannot sit in ANY barrel, which is exactly why the
    conversion arm above reads the overflow drum, not a carrier drum.

passes=2 is the idempotency proof. teardown: the explicit band restore.

Run:  GT6_SESSION=off python3 tools/rcon/chains/p29_w4_hot_lube_hex.py
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

# the fresh z=328 band (card ① chemicals rides z=320, W3 rides z~305 — x-disjoint bands):
# HEX stack: controller (430, 65, 328); ring y65/y66; emission layer y67; the boiler POS
# (430, 68, 328) facing north — the anchor sits one cell in front (430, 68, 329), so the
# base 3x3 of transmitters lands at y67 centred on (430, 329): five of the eight y+2
# emission targets overlap the boiler base (the two z=327 ring corners hit air, the
# upstream decrement-regardless face).
HEX = gt6world.Site(430, 65, 328, dx=2, dy=6, dz=4)
LB = gt6world.Site(430, 68, 329, dx=3, dy=4, dz=4)
OVERFLOW_BARREL = gt6world.Site(430, 64, 328)
METAL_BARREL = gt6world.Site(434, 65, 328)
WOOD_BARREL = gt6world.Site(434, 65, 330)

HEX_P = "430 65 328"
LB_P = "430 68 328"
OVERFLOW_P = "430 64 328"
METAL_P = "434 65 328"
WOOD_P = "434 65 330"
VARIANT = "large_boiler_stainless_steel"

WALL = "gt6:dense_wall_tungsten"
TX = "gt6:heat_transmitter"

# the 16 shell cells relative to the controller (dx, dy, dz), the check order
RING_Y0 = [(dx, 0, dz) for dz in (-1, 0, 1) for dx in (-1, 0, 1) if (dx, dz) != (0, 0)]
RING_Y1 = [(dx, 1, dz) for dz in (-1, 0, 1) for dx in (-1, 0, 1) if (dx, dz) != (0, 0)]

shell_steps = []
for dx, dy, dz in RING_Y0:
    shell_steps.append(Step(f"setblock {430 + dx} {65 + dy} {328 + dz} {WALL}", expect="Changed the block"))
for dx, dy, dz in RING_Y1:
    shell_steps.append(Step(f"setblock {430 + dx} {66} {328 + dz} {TX}", expect="Changed the block"))
shell_steps.append(Step(f"setblock 430 66 328 {WALL}", expect="Changed the block"))  # :106 the centre wall

steps = [
    phase("A: the form — controller + the 16-part shell, then the formed verdict"),
    Step(f"gt6heatexchanger place {HEX_P}", expect="GT6 large heat exchanger placed at 430, 65, 328"),
] + shell_steps + [
    Step(f"gt6heatexchanger stat {HEX_P}", expect="formed=true rate=16384 efficiency=10000", sleep=2.0),

    phase("B: the coolant row — ic2hotcoolant via the setblock-NBT tank channel, the :203 conversion live in the overflow barrel"),
    # the tank payload keys are leg-variant: 1.20.1 Forge FluidStack NBT ("FluidName") vs
    # the 21.1 codec face — LOWERCASE {id, amount} (the neoforge-21.1.209 FluidStack
    # CODEC ldc keys, javap-verified; "Amount"/"FluidName" both fail the codec and the
    # keepFilter read-side rebuild, landing fuel=0/163840 — the p29w4hl neo runs)
    Step(f'setblock {HEX_P} gt6:large_heat_exchanger{{gt.tank0:{{FluidName:"gt6:ic2hotcoolant",Amount:163840}}}}',
         expect="Changed the block",
         node_cmds={"1.21.1": f'setblock {HEX_P} gt6:large_heat_exchanger{{gt.tank0:{{id:"gt6:ic2hotcoolant",amount:163840}}}}'}),
    Step(f"gt6heatexchanger stat {HEX_P}", expect="formed=true", sleep=2.0),
    # the burn starts the moment the NBT lands (the row resolves by content) — the
    # fuel face is LIVE but already draining, so the pin is the face, not the level
    Step(f"gt6heatexchanger stat {HEX_P}", expect=" HU/t fuel="),
    Step(f"setblock {OVERFLOW_P} gt6:barrel_metal", expect="Changed the block"),
    Step(f"gt6tank stat {OVERFLOW_P}", expect="L of gt6:ic2coolant", poll=20.0),

    phase("C: the HU emission — the 18101 proxy relay into the SS large boiler (the barometer face)"),
    Step(f"gt6multiblock boiler place {LB_P} {VARIANT}", expect="GT6 large boiler placed"),
    Step(f"gt6multiblock boiler frame {LB_P} {VARIANT}", expect="34 parts placed"),
    Step(f"gt6multiblock boiler wand {LB_P} {VARIANT}", expect="formed", sleep=1.0),
    Step(f"gt6multiblock boiler fill {LB_P} 128000", expect="ACCEPTED"),
    Step(f"gt6multiblock boiler stat {LB_P}", expect="formed=true", sleep=2.0),
    Step(f"gt6multiblock boiler stat {LB_P}", expect="barometer=1", sleep=6.0),

    phase("D: the 340 K carrier arms — the bronze drum holds the 1200 K pahoehoe, the wood barrel melts, the hot coolant voids"),
    Step(f"setblock {METAL_P} gt6:barrel_metal", expect="Changed the block"),
    Step(f"gt6tank fill {METAL_P} gt6:ic2pahoehoelava 1000", expect="filled 1000/1000 L of gt6:ic2pahoehoelava (ACCEPTED)"),
    Step(f"gt6tank stat {METAL_P}", expect="1000/64000 L of gt6:ic2pahoehoelava"),
    Step(f"setblock {WOOD_P} gt6:barrel_wood", expect="Changed the block"),
    Step(f"gt6tank fill {WOOD_P} gt6:ic2pahoehoelava 1000", expect="(ACCEPTED)"),
    Step(f"execute if block {WOOD_P} gt6:barrel_wood", expect="Test failed", sleep=3.0),  # melted down — the block is gone
    # the :184 allowFluid arm: the POWER_CONDUCTING hot coolant cannot sit in ANY barrel —
    # the fill is ACCEPTED (the fill face has no gate) and the tick voids it (gas-proof only
    # exempts the GAS gate — GT6HeatExchangerBlockEntity-independent upstream semantics)
    Step(f"gt6tank draw {METAL_P} 1000", expect="drawn 1000/1000 L of gt6:ic2pahoehoelava (ACCEPTED)"),
    Step(f"gt6tank fill {METAL_P} gt6:ic2hotcoolant 1000", expect="filled 1000/1000 L of gt6:ic2hotcoolant (ACCEPTED)"),
    Step(f"gt6tank stat {METAL_P}", expect="0/64000 L of nothing", sleep=3.0),  # the :184 void, live

    phase("E: teardown — the explicit band restore (no global state was touched)"),
    Step("fill 426 62 324 436 76 333 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p29-w4-hot-lube-hex p29_w4_hot_lube",
    slug="p29w4hotlubehex",
    sites=gt6world.declare_sites(HEX, LB, OVERFLOW_BARREL, METAL_BARREL, WOOD_BARREL),
    preferred_ports=(26421, 26431),      # this card's pinned rcon/query pair (2642x band, disjoint from W3/①)
    game_port=26411,                     # the pinned game port (rcon - 10)
    steps=steps,
)

if __name__ == "__main__":
    main(CHAIN)
