#!/usr/bin/env python3
"""bees — the bee-comb Lv1 static chain (task bees-lv1, the bees sweep
group; MultiItemFood.java:251-270 + the materialHoneycomb listener body).

Two arms over the SAME honey comb input, one per consumed map:

  A Squeezer (KU pulse rig, the squeezer T1 form): 1 comb_honey -> 90 L
    gt6:honey + 1x dust_wax_bee — the materialHoneycomb generalization body live
    (Loader_Recipes_Food.java:264), the SQUEEZER map's 20-row pour consumed.
  B Centrifuge (the centrifuge T2 inject form): 1 comb_honey -> 100 L
    gt6:honey + 1x dust_wax_bee — the :251 row verbatim (eUt 16, duration 64,
    chances {10000} after the FR-propolis prefix-trim), the CENTRIFUGE map consumed.

The arms double as the REGISTRATION live proof: the comb input rides /give +
data merge of gt6:comb_honey (the item registry), the fluid verdict reads the
gt6:honey identity out of the machine tank (the fluid registry), and the wax
dust is the material-item path (GTMaterialItems). The pour-evidence boot log
lines ("GT6 Bee centrifuge poured: 20 loaded, 0 skipped") ride the sweep JSON.

passes=2 is the idempotency proof. No global state is touched.

Run:  GT6_SESSION=off python3 tools/rcon/chains/bees.py
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

SQZ = gt6world.Site(384, 64, 396)    # arm A: the squeezer column
CEN = gt6world.Site(392, 64, 396)    # arm B: the centrifuge column (x-disjoint)


def feed_comb(pos):
    """One comb_honey into slot 0 (the Count key fork: 1.20.1 `Count:Nb` vs 21.1 `count:N`)."""
    return {
        "1.20.1": f'data merge block {pos} {{inventory:{{Size:3,Items:[{{Slot:0b,id:"gt6:comb_honey",Count:1b}}]}}}}',
        "1.21.1": f'data merge block {pos} {{inventory:{{Size:3,Items:[{{Slot:0b,id:"gt6:comb_honey",count:1}}]}}}}',
    }


def feed_comb_cent(pos):
    """One comb_honey into the centrifuge's 7-slot inventory (the centrifuge form)."""
    return {
        "1.20.1": f'data merge block {pos} {{inventory:{{Size:7,Items:[{{Slot:0b,id:"gt6:comb_honey",Count:1b}}]}}}}',
        "1.21.1": f'data merge block {pos} {{inventory:{{Size:7,Items:[{{Slot:0b,id:"gt6:comb_honey",count:1}}]}}}}',
    }


def wax_out():
    """The completion item report — 1x bees wax dust (1.20.1 renders plain paths, 21.1 namespaced)."""
    return {
        "1.20.1": "outputs=[1x dust_wax_bee; ]",
        "1.21.1": "outputs=[1x gt6:dust_wax_bee; ]",
    }


steps = [
    phase("A: Squeezer — 1 comb_honey -> 90 L gt6:honey + 1x dust_wax_bee (the Listener:264 body live)"),
    Step(f"setblock {F(SQZ)} gt6:squeezer", expect="Changed the block", sleep=1.0),
    # the squeezer family floor is parallel 4 (the PARALLEL_4_32 table, the squeezer pin)
    Step(f"gt6machine wiremill check {F(SQZ)}", expect="parallel=4 parallelDuration=true"),
    Step(feed_comb(F(SQZ))["1.20.1"], expect="Modified block data", node_cmds=feed_comb(F(SQZ))),
    Step(f"gt6machine wiremill inject 24 64 -64 {F(SQZ)}",
         expect=wax_out()["1.20.1"], node_expects=wax_out()),
    Step(f"gt6machine wiremill fluid stat {F(SQZ)}", expect="out[0]=90 L of gt6:honey"),
    Step(f"gt6machine wiremill check {F(SQZ)}", expect="data=-2"),

    phase("B: Centrifuge — 1 comb_honey -> 100 L gt6:honey + 1x dust_wax_bee (the :251 row live)"),
    Step(f"setblock {F(CEN)} gt6:centrifuge", expect="Changed the block", sleep=1.0),
    Step(f"gt6machine wiremill check {F(CEN)}", expect="parallel=1 parallelDuration=true"),
    Step(feed_comb_cent(F(CEN))["1.20.1"], expect="Modified block data", node_cmds=feed_comb_cent(F(CEN))),
    # the T1 centrifuge maxIn=64 — the centrifuge T2 form's 256 packet OVERCHARGES
    # the T1 machine (the energy is rejected, zero progress); 24 ticks at 64 delivers
    # 1536 RU >= the 1024 RU budget (eUt 16 x duration 64, 4x overclock completes at t16)
    Step(f"gt6machine wiremill inject 24 64 {F(CEN)}",
         expect=wax_out()["1.20.1"], node_expects=wax_out()),
    Step(f"gt6machine wiremill fluid stat {F(CEN)}", expect="out[0]=100 L of gt6:honey"),
    Step(f"gt6machine wiremill check {F(CEN)}", expect="data=-2"),

    phase("D: teardown — the explicit band restore (no global state was touched)"),
    Step("fill 382 62 394 396 68 398 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="bee_comb",
    slug="beecomb",
    sites=gt6world.declare_sites(SQZ, CEN),
    preferred_ports=(26190, 26200),      # the shared session-boot pinned pair
    steps=steps,
)

if __name__ == "__main__":
    main(CHAIN)
