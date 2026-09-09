#!/usr/bin/env python3
"""p26-crucible-multiblock — the LARGE crucible live acceptance chain (task
p26-crucible-multiblock ACCEPTANCE ③④: "搭 Steel 墙大型坩埚→结构成立→y0 层
燃烧箱供 HU→顶投矿→熔融→y1 层外墙贴 Mold 右键→穿墙代理浇铸成功→冷却取锭"
plus the meltdown path probe).

Chain semantics:

  A the structure: 24 steel walls hand-placed around the controller (three rings,
    y+0 ONLY_ENERGY_IN / y+1 ONLY_CRUCIBLE / y+2 ONLY_ITEM_FLUID, the centre column
    left open) → `gt6multiblock crucible check` forms. The stat readback pins the
    Steel ceiling live: max=2250K = 2046 x 1.10 (MT.java:2444 heat(2046), the :80
    LARGE bonus) and the flat env seam 293 K.

  B the melt: a REAL brick burning box feeds HU through the y+0 wall relay (the
    inherited HeatTransmitterBlockEntity face, the mode gate — the p13 boiler live
    topology), read back on stat, then extinguished to freeze the base. 4U iron feeds
    on top (feed-then-heat, the physical order), and the heat arm tops the buffer with
    one integer charge (the box packet stream compressed into one call — the A-card
    row0 driver semantic) → `stat` pins temp=19xxK — past Fe.mMeltingPoint 1811
    (MT.java:996), below the WARNING latch (ceiling-100 = 2150) with margin both ways.

  B' the through-wall pour: `crucible <pos> pour <wallPos>` clicks a y+1 wall with the
    recording mold probe (the offline RecordingMold double, live-server form): the
    wall relays controller-ward (MultiBlockPart :687-690), the controller pours the
    molten self-smelted stack (:547-556), the content drops 4.0U → 3.0U. The y+0 wall
    refuses the same click (the NO_CRUCIBLE mode gate :688) — the layering is live.
    BOUNDARY (declared): the physical Mold block + the solidify-to-ingot half of the
    seam is the A-card min-face (gt6:mold_stone → gt6:ingot_tin, A's row0 chain pours
    it live on the same ITileEntityMold seam); the halves compose at the A→C merge —
    this branch carries the interface and both live sides of the crucible relay.

  C the meltdown probe: one huge charge drives the tick past the Steel ceiling
    (2046 x 1.10 = 2250 K) → the content trashes and the 3x3x3 cavity is lava
    (upstream :367-377) — probed on a SECOND crucible so B's rig survives.

  D teardown: the explicit band restore (the pass-opening gt6world bbox cleanup is
    the structural backstop).

THE ARITHMETIC (deterministic integer math — envTemperature() is the flat
DEF_ENV_TEMP 293 seam default, tickHeat converts the WHOLE buffer in one tick,
requiredEnergy = 1 + floor(weight/100) HU per K): the Steel shell rides the
Fe generify density 7.874 (Steel ← WroughtIron 1U ← Fe) → shell getWeight(U*100)
≈ 87489 kg, 4U Fe ≈ 3500 kg → ≈ 90989 kg → ≈ 910 HU/K. Melt charge 1504000 →
1652 K over the ~295 K post-burn base → ~1947 K, inside the asserted "temp=19"
band for any requiredEnergy in [875..925] (the density derivation oracle — the
band deliberately absorbs it). Meltdown charge 2200000 → ≈ 2710 K > 2250 on the
same spread. passes=2 is the idempotency proof.

Run:  python3 tools/rcon/chains/p26_crucible_multiblock.py --node 1.20.1-forge
      python3 tools/rcon/chains/p26_crucible_multiblock.py --node 1.21.1-neo
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

# the two crucible rigs on the band: B (the melt rig) and C (the meltdown probe),
# plus the real burning box feeding B's y+0 wall from the west
SITE_B = gt6world.Site(430, 65, 124, dx=1, dy=2, dz=1)   # the 3x3x3 box: y 64..66
BOX_B = (428, 64, 123)                                   # the brick burning box
SITE_C = gt6world.Site(440, 65, 124, dx=1, dy=2, dz=1)   # the meltdown 3x3x3
B = "430 64 124"                                          # the controller cell (y+0 centre)
C = "440 64 124"                                          # the meltdown controller cell
BB = gt6world.fmt(BOX_B)

CONTROLLER_BLOCK = "gt6:crucible_steel"     # the GT6Crucibles controller row
WALL_BLOCK = "gt6:crucible_steel_wall"      # the steel wall part row


def wall_setblock_steps(controller, expect="Changed the block"):
    """The 24 walls in three rings around `controller` (the y0/y1/y2 rings, the
    centre column open). The ring order matches the pattern declaration."""
    steps = []
    bx, by, bz = controller.split()
    for ring in range(3):
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                if dx == 0 and dz == 0:
                    continue
                steps.append(Step(
                    f"setblock {int(bx) + dx} {int(by) + ring} {int(bz) + dz} {WALL_BLOCK}",
                    expect=expect))
    return steps


steps = []

# ------------------------------------------------- A: the structure forms
steps += [
    phase("A: the steel-wall 3x3x3 forms (three rings, open centre column)"),
    Step(f"setblock {B} {CONTROLLER_BLOCK}", expect="Changed the block"),
]
steps += wall_setblock_steps(B)
steps += [
    Step(f"gt6multiblock crucible {B} check", expect="okay=true linked_parts=24/24"),
    # the live constant pin: Steel 2046 x 1.10 = 2250 K, the flat env seam
    Step(f"gt6multiblock crucible {B} stat", expect="max=2250K"),
]

# ------------------------------------------------- B: the real box + the melt
steps += [
    phase("B: the brick box feeds HU through the y+0 wall, the charge melts the iron"),
    Step(f"gt6burner place {BB} brick_burning_box", expect="GT6 burning box placed"),
    Step(f"gt6burner fuel {BB} minecraft:coal 8", expect="minecraft:coal x8"),
    Step(f"gt6burner ignite {BB}", expect="burning=true", sleep=5.0),
    # the conduction readback (the burn window drifts the base ~+2 K — logged, not pinned)
    Step(f"gt6multiblock crucible {B} stat", expect="temp="),
    Step(f"gt6burner extinguish {BB}", expect="burning=false", sleep=0.5),
    Step(f"gt6multiblock crucible {B} feed iron 4", expect="fed=true"),
    Step(f"gt6multiblock crucible {B} heat 1504000", expect="buffer=", sleep=1.5),
    # the melt band: past Fe 1811, under the WARNING latch 2150, any density in [875..925]
    Step(f"gt6multiblock crucible {B} stat", expect="temp=19"),
]

# ------------------------------------------------- B': the through-wall pour
steps += [
    phase("B': the y+1 wall relays the pour, the y+0 wall refuses it (the layering live)"),
    # the recording mold clicks the MOLD layer wall -> the controller pours 1U of
    # WroughtIron (the findMaterial("iron") resolution — GT6 iron IS WroughtIron;
    # the live r1 readback pinned mNameInternal)
    Step(f"gt6multiblock crucible {B} pour 431 65 124", expect="poured=1.0U of WroughtIron"),
    # the content dropped 4.0U -> 3.0U
    Step(f"gt6multiblock crucible {B} stat", expect="WroughtIron 3.0U"),
    # the energy-layer wall carries NO_CRUCIBLE — the same click is refused (the :688 gate)
    Step(f"gt6multiblock crucible {B} pour 431 64 124", expect="poured=0", allow_failed=True),
]

# ------------------------------------------------- C: the meltdown probe (acceptance ④)
steps += [
    phase("C: over the ceiling → content trash + the 3x3x3 lava (:367-377)"),
    Step(f"setblock {C} {CONTROLLER_BLOCK}", expect="Changed the block"),
]
steps += wall_setblock_steps(C)
steps += [
    Step(f"gt6multiblock crucible {C} check", expect="okay=true linked_parts=24/24"),
    Step(f"gt6multiblock crucible {C} feed iron 4", expect="fed=true"),
    # one huge charge drives the tick past the Steel ceiling on the server tick
    Step(f"gt6multiblock crucible {C} heat 2200000", expect="buffer=", sleep=2.0),
    # the cavity is lava — the centre column cell included (hard assert, no allow_failed).
    # `say` prints to the chat broadcast, NOT back to the RCON peer (the r1 readback:
    # <no response>) — arm a feedback command behind `execute if block` instead.
    Step(f"execute if block 440 65 124 minecraft:lava run time query daytime",
         expect="The time is"),
]

# ------------------------------------------------- D: teardown
steps += [
    phase("D: teardown — the explicit band restore"),
    Step("fill 426 62 120 444 68 128 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p26-crucible-multiblock",
    slug="p26crucible",
    sites=gt6world.declare_sites(SITE_B, BOX_B, SITE_C),
    preferred_ports=(26130, 26140),      # this card's pinned rcon/query pair (after the p25 2612x segment)
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
