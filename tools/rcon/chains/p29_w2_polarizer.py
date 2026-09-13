#!/usr/bin/env python3
"""p29_w2_polarizer — the Polarizer family (MU, RM.POLARIZER) live acceptance chain, the
FULL 5-tier ladder T1-T5 (task p29-w2-exotic-energy ACCEPTANCE ①/④: the MU machine eats
MU packets and REFUSES EU/RU packets — the energy-type gate LIVE, the MU arm of the
three-type trio).

Rig split (the p29_w2_energy_types arm-E source-dial precedent — the LIVE gate must go
through a rig whose dialed instance is the packet carrier; /gt6machine inject self-types
the machine's OWN carrier and CANNOT express the refusal half):
  T1 — the LIVE MU rig: the gt6energy source sits UNDER the machine (the machine's
    SBIT_D bottom face of the :1418 SBIT_U|SBIT_D energy mask receives the rig's up-face
    emission). THREE dials: EU -> refused (energy=0, active=false); RU -> refused; MU ->
    the same rig drives the recipe to completion (out[0]=glowstone_dust).
  T5 — the inject rig over the 5-tier window (TIER_INPUTS[4] = EV_TIER_INPUTS 4096/8192/
    16384, the first 5-tier row consumers in the repo): check pins minIn/recIn/maxIn,
    inject 4096 runs the row (the brigadier inject-size cap; minIn IS the window floor) (the self-typed carrier proves the window; the display face
    "Polarizer (Titanium)" rides the GT6LangParityTest pins — the menu-less carrier has
    no RCON name surface).

The smoke row (data/gt6/recipe_maps/polarizer.json): 1 redstone -> 1 glowstone_dust,
eUt 16 duration 16 — a DECLARED smoke row (the real upstream recipe domain rides the
chemical-fluid batch F; the card ruling "配方行=声明"). eUt 16 = the T1 window minimum,
so the row runs un-overclocked at the default 32 EU packet.

passes=2 is the idempotency proof. No global state is touched — one session boot serves
the p29_w2_exotic cluster.

Run:  GT6_SESSION=off python3 tools/rcon/chains/p29_w2_polarizer.py
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

# the fresh z=268 band (clear of the card-① z=264 strip): the polarizer column x384..390,
# machine y=64, the live rig y=63 (under), the T5 row two steps east.
MILL_T1 = gt6world.Site(384, 64, 268, dy=1, dz=1)  # the machine + the rig slot below
MILL_T5 = gt6world.Site(388, 64, 268, dy=1, dz=1)
T1, T5 = F(MILL_T1), F(MILL_T5)

DUST_OUT = {"1.20.1": "out[0]=1x glowstone_dust", "1.21.1": "out[0]=1x minecraft:glowstone_dust"}
DUST_OUT_INJ = {"1.20.1": "outputs=[1x glowstone_dust; ]", "1.21.1": "outputs=[1x minecraft:glowstone_dust; ]"}


def feed_merge(pos, size, item):
    """The inventory data-merge feed (the Count key fork: 1.20.1 `Count:Nb` vs 21.1 `count:N`)."""
    return {
        "1.20.1": f'data merge block {pos} {{inventory:{{Size:{size},Items:[{{Slot:0b,id:"minecraft:{item}",Count:1b}}]}}}}',
        "1.21.1": f'data merge block {pos} {{inventory:{{Size:{size},Items:[{{Slot:0b,id:"minecraft:{item}",count:1}}]}}}}',
    }


steps = [
    phase("A: T1 — the LIVE MU rig; EU and RU refused, MU drives the row to completion"),
    Step(f'setblock {T1} gt6:polarizer', expect="Changed the block", sleep=1.0),
    Step(f"gt6machine wiremill check {T1}", expect="minIn=16 recIn=32 maxIn=64"),  # the fresh BE idle (TIER_INPUTS[0])
    Step(feed_merge(T1, 2, "redstone")["1.20.1"], expect="Modified block data", node_cmds=feed_merge(T1, 2, "redstone")),
    # the source rig UNDER the machine (the :1418 SBIT_U|SBIT_D energy mask, bottom face)
    Step(f"gt6energy place {F(gt6world.Site(384, 63, 268))}", expect="GT6 energy source placed at 384, 63, 268"),
    # EU dial: the polarizer carries MU (:1418) — the packets are refused, the gate holds
    Step(f"gt6energy type {F(gt6world.Site(384, 63, 268))} EU", expect="type ENERGY.ELECTRICITY"),
    Step(f"gt6energy mode {F(gt6world.Site(384, 63, 268))} on", expect="emitting true", sleep=2.0),
    Step(f"gt6machine wiremill check {T1}", expect="energy=0"),
    Step(f"gt6machine wiremill check {T1}", expect="active=false"),
    # RU dial: refused just the same (the :803 reference-equality gate is type-exact)
    Step(f"gt6energy type {F(gt6world.Site(384, 63, 268))} RU", expect="type ENERGY.KINETIC_ROTATION"),
    Step(f"gt6energy stat {F(gt6world.Site(384, 63, 268))}", expect="type ENERGY.KINETIC_ROTATION", sleep=2.0),
    Step(f"gt6machine wiremill check {T1}", expect="active=false"),
    # MU re-dial: the SAME rig now drives the machine (the dialed instance IS the packet carrier)
    Step(f"gt6energy type {F(gt6world.Site(384, 63, 268))} MU", expect="type ENERGY.MAGNETIC"),
    Step(f"gt6energy stat {F(gt6world.Site(384, 63, 268))}", expect="type ENERGY.MAGNETIC", sleep=2.0),
    Step(f"gt6machine wiremill check {T1}", expect=DUST_OUT["1.20.1"], node_expects=DUST_OUT, poll=45.0),

    phase("B: T5 — the first 5-tier row, the EV_TIER_INPUTS window (4096/8192/16384)"),
    Step(f'setblock {T5} gt6:polarizer_t5', expect="Changed the block", sleep=1.0),
    Step(f"gt6machine wiremill check {T5}", expect="minIn=4096 recIn=8192 maxIn=16384"),
    Step(feed_merge(T5, 2, "redstone")["1.20.1"], expect="Modified block data", node_cmds=feed_merge(T5, 2, "redstone")),
    # the self-typed carrier inject: 8192-sized packets of the machine's own MU —
    # four x4 overclocks fold the 16-tick row into one tick
    Step(f"gt6machine wiremill inject 8 4096 {T5}", expect=DUST_OUT_INJ["1.20.1"], node_expects=DUST_OUT_INJ),

    phase("C: teardown — the explicit band restore (no global state was touched)"),
    Step("fill 383 62 267 390 67 269 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p29_w2_exotic polarizer",
    slug="p29w2polarizer",
    sites=gt6world.declare_sites(MILL_T1, MILL_T5, gt6world.Site(384, 63, 268)),
    preferred_ports=(26360, 26370),      # this card's pinned rcon/query pair
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
