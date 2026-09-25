#!/usr/bin/env python3
"""turbine-dynamo (steam leg) — the Steam Turbine chain (task
turbine-dynamo RCON group turbine_dynamo, the STEAM 夹具 leg).

Per tier (17211-17214, x-disjoint columns of one fresh z=300 band):

  A-D one phase per tier: the controller setblocks, the PATTERN SCAFFOLD forms it
    (gt6multiblock form — the checker's SET walk off the declared per-facing
    pattern: 35 dense walls out of the stocked fake-player inventory), the
    /gt6energy dial rides the CARD'S STEAM SHORT CODE (type STEAM -> the shared
    TD.Energy.STEAM instance), and the far plate relays the converted RU into a
    flux_dynamo_t5 sink (window [4096,16384] RU — the four tiers' packets all
    enter it, the top tier oversize-consumed, which is still a live used>0
    delivery). The converter's persisted accounting pair (gt.last_in /
    gt.last_out — the W3 accounting channel) pins the four-tier conversion:
    volt = NBT_INPUT -> one packet of exactly NBT_OUTPUT.

  E the waste arm on tier 1 (WASTE_ENERGY=T): an OVERSIZE dial (2*in+1 > the
    [in/2, 2*in] window) consumes the whole offer (gt.last_in pins the size),
    strikes the overload ladder, clears the capacitor (gt.capacitor: 0L) and
    converts NOTHING (gt.last_converted: 0L) — the over-offered steam never
    becomes RU.

  F the structure semantics on tier 1: the middle fluid column modes
    (ONLY_FLUID -13 on the layer bottom, ONLY_FLUID_IN -9 on the layer ring —
    the frontal 3x3 intake, LargeTurbineSteam.java:90-93) and the far plate
    write (design: 3b + mode: -2, the ONLY_ENERGY_OUT port).

passes=2 is the idempotency proof (the [0,0] of the group).
Run:  GT6_SESSION=off python3 tools/rcon/chains/steam_turbine.py
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

# the fresh z=300 band, four x-disjoint 16-block columns (384/400/416/432)
Z = 300
TIERS = [
    # (variant, NBT_INPUT SU, NBT_OUTPUT RU)
    ("steam_turbine_magnalium", 12288, 4096),
    ("steam_turbine_trinitanium", 24576, 8192),
    ("steam_turbine_graphene", 49152, 16384),
    ("steam_turbine_vibramantium", 393216, 131072),
]

def column(i):
    return 384 + i * 16

def ctrl(i):    return F(gt6world.Site(column(i), 65, Z))
def src(i):     return F(gt6world.Site(column(i), 65, Z - 1))
def sink(i):    return F(gt6world.Site(column(i), 65, Z + 4))
def mid_ring(i):   return F(gt6world.Site(column(i) + 1, 65, Z))       # a d=0 ring cell (ONLY_FLUID_IN)
def mid_bottom(i): return F(gt6world.Site(column(i), 64, Z))           # the d=0 bottom cell (ONLY_FLUID)
def far_plate(i):  return F(gt6world.Site(column(i), 65, Z + 3))       # the ONLY_ENERGY_OUT plate

# the site footprints (the formed shell spans x-1..x+1, y 64..66, z..z+3, plus src/sink)
SITES = [gt6world.Site(column(i), 65, Z + 1, dx=2, dy=1, dz=4) for i in range(4)]

steps = []
for i, (variant, t_in, t_out) in enumerate(TIERS):
    steps += [
        phase(f"{'ABCD'[i]}: tier {i + 1} ({variant}) — form, the STEAM dial, the four-tier conversion"),
        Step(f"setblock {ctrl(i)} gt6:{variant}[facing=north]", expect="Changed the block"),
        Step(f"gt6multiblock form {ctrl(i)}", expect="formed=true okay=true"),
        Step(f"gt6multiblock form {ctrl(i)}", expect="first_failed_cell=none"),
        Step(f"gt6energy place {src(i)}", expect="GT6 energy source placed at"),
        Step(f"gt6energy type {src(i)} STEAM", expect="type ENERGY.STEAM"),
        Step(f"gt6energy volt {src(i)} {t_in}", expect=f"voltage {t_in} EU"),
        Step(f"gt6energy mode {src(i)} on", expect="emitting true"),
        Step(f"setblock {sink(i)} gt6:flux_dynamo_t5[facing=south]", expect="Changed the block"),
        Step(f"data get block {ctrl(i)}", expect=f"gt.last_out: {t_out}L", sleep=1.5, poll=15.0),
        Step(f"data get block {ctrl(i)}", expect=f"gt.last_in: {t_in}L"),
    ]

steps += [
    phase("E: the waste arm on tier 1 — the oversize steam never becomes RU"),
    Step(f"gt6energy volt {src(0)} {2 * TIERS[0][1] + 1}", expect=f"voltage {2 * TIERS[0][1] + 1} EU"),
    Step(f"data get block {ctrl(0)}", expect=f"gt.last_in: {2 * TIERS[0][1] + 1}L", sleep=1.5, poll=15.0),
    Step(f"data get block {ctrl(0)}", expect="gt.capacitor: 0L"),
    Step(f"data get block {ctrl(0)}", expect="gt.last_converted: 0L"),
    Step(f"gt6energy mode {src(0)} off", expect="emitting false"),

    phase("F: the structure semantics — the fluid column, the design-3 energy-out plate"),
    Step(f"data get block {far_plate(0)}", expect="mode: -2"),
    Step(f"data get block {far_plate(0)}", expect="design: 3b"),
    Step(f"data get block {mid_ring(0)}", expect="mode: -9"),
    Step(f"data get block {mid_bottom(0)}", expect="mode: -13"),

    phase("G: teardown — restore the band"),
    Step(f"fill {column(0) - 2} 63 {Z - 2} {column(3) + 2} 68 {Z + 5} air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="turbine_dynamo steam turbine",
    slug="steamturbine",
    sites=gt6world.declare_sites(*SITES),
    steps=steps,
)

if __name__ == "__main__":
    main(CHAIN)
