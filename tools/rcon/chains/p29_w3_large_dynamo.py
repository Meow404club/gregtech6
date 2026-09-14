#!/usr/bin/env python3
"""p29-w3-turbine-dynamo (dynamo leg) — the Large Dynamo chain (task
p29-w3-turbine-dynamo RCON group p29_w3_turbine_dynamo).

Per tier (17221-17224, x-disjoint columns of the fresh z=300 band):

  A-D one phase per tier: the controller setblocks, the PATTERN SCAFFOLD forms
    it (two 3x3 dense-wall plates + the 18 Large Copper Coils, out of the
    stocked fake-player inventory), the RU source dials volt = NBT_INPUT on the
    FRONT, and the converted EU leaves through the far plate into an
    electric_transformer sink (its back to the plate; the tier packets all
    enter, oversize-consumed at the top tiers — a live used>0 delivery). The
    persisted accounting pair pins the EXACT 75% ladder: volt 4096/8192/16384/
    131072 -> gt.last_out 3072/6144/12288/98304.

  E the structure semantics on tier 1: the far plate carries design: 2b +
    mode: -2 (the ONLY_ENERGY_OUT port, LargeDynamo.java:68), a coil-layer cell
    holds NOTHING with the copper-coil block, an end-plate wall cell NOTHING.

  F the waste arm on tier 1 (WASTE_ENERGY=T, RU): an oversize dial consumes the
    whole offer, strikes the ladder, clears the capacitor, converts nothing.

passes=2 is the idempotency proof (the [0,0] of the group).
Run:  GT6_SESSION=off python3 tools/rcon/chains/p29_w3_large_dynamo.py
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

# the same fresh z=300 band, east of the steam leg: four x-disjoint 16-block columns
Z = 300
TIERS = [
    # (variant, NBT_INPUT RU, NBT_OUTPUT EU)
    ("large_dynamo_stainless_steel", 4096, 3072),
    ("large_dynamo_titanium", 8192, 6144),
    ("large_dynamo_tungstensteel", 16384, 12288),
    ("large_dynamo_adamantium", 131072, 98304),
]

def column(i):
    return 480 + i * 16

def ctrl(i):      return F(gt6world.Site(column(i), 65, Z))
def src(i):       return F(gt6world.Site(column(i), 65, Z - 1))
def sink(i):      return F(gt6world.Site(column(i), 65, Z + 4))
def coil_cell(i): return F(gt6world.Site(column(i), 65, Z + 1))   # a d=1 cell (the coil layer)
def plate_cell(i): return F(gt6world.Site(column(i), 65, Z + 3))  # the far plate
def wall_cell(i): return F(gt6world.Site(column(i) + 1, 65, Z))   # a d=0 end-plate cell

SITES = [gt6world.Site(column(i), 65, Z + 1, dx=2, dy=1, dz=4) for i in range(4)]

steps = []
for i, (variant, t_in, t_out) in enumerate(TIERS):
    steps += [
        phase(f"{'ABCD'[i]}: tier {i + 1} ({variant}) — form, the RU dial, the exact 75% ladder"),
        Step(f"setblock {ctrl(i)} gt6:{variant}[facing=north]", expect="Changed the block"),
        Step(f"gt6multiblock form {ctrl(i)}", expect="formed=true okay=true"),
        Step(f"gt6multiblock form {ctrl(i)}", expect="first_failed_cell=none"),
        Step(f"gt6energy place {src(i)}", expect="GT6 energy source placed at"),
        Step(f"gt6energy type {src(i)} RU", expect="type ENERGY.KINETIC_ROTATION"),
        Step(f"gt6energy volt {src(i)} {t_in}", expect=f"voltage {t_in} EU"),
        Step(f"gt6energy mode {src(i)} on", expect="emitting true"),
        Step(f"setblock {sink(i)} gt6:electric_transformer[facing=south]", expect="Changed the block"),
        Step(f"data get block {ctrl(i)}", expect=f"gt.last_out: {t_out}L", sleep=1.5, poll=15.0),
        Step(f"data get block {ctrl(i)}", expect=f"gt.last_in: {t_in}L"),
    ]

steps += [
    phase("E: the structure semantics — the coil segment, the design-2 plate, the NOTHING walls"),
    Step(f"execute if block {plate_cell(0)} gt6:large_dynamo_stainless_steel", expect="Test passed"),
    Step(f"data get block {plate_cell(0)}", expect="mode: -2"),
    Step(f"data get block {plate_cell(0)}", expect="design: 2b"),
    Step(f"execute if block {coil_cell(0)} gt6:large_copper_coil", expect="Test passed"),
    Step(f"data get block {coil_cell(0)}", expect="mode: -1"),
    Step(f"data get block {wall_cell(0)}", expect="mode: -1"),

    phase("F: the waste arm on tier 1 — the oversize rotation never becomes EU"),
    Step(f"gt6energy volt {src(0)} {2 * TIERS[0][1] + 1}", expect=f"voltage {2 * TIERS[0][1] + 1} EU"),
    Step(f"data get block {ctrl(0)}", expect=f"gt.last_in: {2 * TIERS[0][1] + 1}L", sleep=1.5, poll=15.0),
    Step(f"data get block {ctrl(0)}", expect="gt.capacitor: 0L"),
    Step(f"data get block {ctrl(0)}", expect="gt.last_converted: 0L"),
    Step(f"gt6energy mode {src(0)} off", expect="emitting false"),

    phase("G: teardown — restore the band"),
    Step(f"fill {column(0) - 2} 63 {Z - 2} {column(3) + 2} 68 {Z + 5} air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p29_w3_turbine_dynamo large dynamo",
    slug="p29w3largedynamo",
    sites=gt6world.declare_sites(*SITES),
    steps=steps,
)

if __name__ == "__main__":
    main(CHAIN)
