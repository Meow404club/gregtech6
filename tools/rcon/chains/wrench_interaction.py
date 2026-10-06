#!/usr/bin/env python3
"""wrench-interaction — the two-symptom live acceptance chain (task
wrench-interaction-chain; the id1345 lesson: interaction-chain symptoms need
click-to-effect evidence, static green is not "fixed").

  A symptom18 sneak leg — the fluid pipe: sneak+wrench through the REAL
    ServerPlayerGameMode.useItemOn dispatch (the /gt6wrenchuse driver, the
    GT6DrinkCommand shape — item.useOn would bypass the vanilla sneak gate and
    prove nothing). ioMask 0 → 2 → 0 is the flow direction truly changing
    (SBIT[UP]=2, the centre hit picks the clicked face); the plain leg toggles
    connections 0 → 2 → 0 (the never-gated regression arm).

  A-neg vanilla fidelity — sneak+wrench on a plain chest: PASS, no state change
    (the un-gate must stay marker-narrow, the upstream ToolCompat no-op shape).

  B symptom19 click leg — the transformer family: plain click re-faces the
    machine (the click the 3x3 grid front-mark reads out), sneak click too (the
    un-gate reaches the family; shift is rotation-independent upstream).
    The grid pixels themselves are client-render — the display half is pinned by
    WrenchSneakChainTest + the runClient visual record, not by this headless run.

passes=2 is the idempotency proof (both sites reset deterministically: the pipe
place arm sets a fresh BE, the transformer setblock resets the facing).

Run:  GT6_SESSION=off python3 tools/rcon/chains/wrench_interaction.py
      (add --node 1.21.1-neoforge for the second leg)
"""

import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
for _path in (str(_HERE), str(_HERE.parent)):
    if _path not in sys.path:
        sys.path.insert(0, _path)

import gt6world
from framework import Chain, Step, main, phase

# the fresh x 480..486 z 124 band — x-disjoint from the builder-wand probe column
# (x 458..462) and every earlier z 120..130 rig; ports continue the 268xx segment
PIPE = gt6world.Site(480, 65, 124)
CHEST = gt6world.Site(482, 65, 124)
TRANS = gt6world.Site(486, 65, 124)
P = gt6world.fmt(PIPE)
C = gt6world.fmt(CHEST)
T = gt6world.fmt(TRANS)

steps = []

# ------------------------------------------------- A: symptom18 — the sneak leg
steps += [
    phase("A: symptom18 — sneak+wrench reaches block.use; the ioMask arrow truly flips"),
    # setblock, NOT /gt6pipe place — the place arm self-connects the support side
    # (OPOS[0]=1, the rerun lesson: connections start at 2, not 0) and this chain's
    # subject is the use chain, so the clean zeroed BE is the deterministic baseline
    Step(f"setblock {P} gt6:wood_fluid_pipe_small", expect="Changed the block",
         label="standalone pipe, zeroed BE"),
    Step(f"gt6pipe stat {P}", expect="ioMask 0", label="baseline: no output arrows"),
    Step(f"gt6wrenchuse sneak {P} up", expect="result=CONSUME",
         label="the gated click gets through (vanilla skipped use here pre-fix)"),
    Step(f"gt6pipe stat {P}", expect="ioMask 2", label="SBIT[UP]=2 — the flow direction changed"),
    Step(f"gt6wrenchuse sneak {P} up", expect="result=CONSUME"),
    Step(f"gt6pipe stat {P}", expect="ioMask 0", label="toggle back off"),
    Step(f"gt6wrenchuse plain {P} up", expect="result=CONSUME"),
    Step(f"gt6pipe stat {P}", expect="connections 2", label="the plain leg: connection toggle, never gated"),
    Step(f"gt6wrenchuse plain {P} up", expect="result=CONSUME"),
    Step(f"gt6pipe stat {P}", expect="connections 0", label="reset"),
]

# ------------------------------------------------- A-neg: vanilla fidelity
steps += [
    phase("A-neg: sneak+wrench on a plain chest stays vanilla — the un-gate is marker-narrow"),
    Step(f"setblock {C} minecraft:chest", expect="Changed the block"),
    Step(f"gt6wrenchuse sneak {C} up", expect="result=PASS",
         label="vanilla gate holds for non-marker blocks"),
]

# ------------------------------------------------- B: symptom19 — the family click leg
steps += [
    phase("B: symptom19 — the transformer family click: plain and sneak both re-face"),
    Step(f"setblock {T} gt6:electric_transformer", expect="Changed the block"),
    Step(f"gt6wrenchuse plain {T} up", expect="facing=up",
         label="wrenchRotate — the click the 3x3 grid front-mark reads out"),
    Step(f"gt6wrenchuse sneak {T} north", expect="facing=north",
         label="the sneak leg reaches the family (shift is rotation-independent upstream)"),
]

# ------------------------------------------------- C: teardown
steps += [
    phase("C: teardown — both sites back to air (the bbox cleanup is the backstop)"),
    Step(f"setblock {P} minecraft:air", expect="Changed the block"),
    Step(f"setblock {C} minecraft:air", expect="Changed the block"),
    Step(f"setblock {T} minecraft:air", expect="Changed the block"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="wrench-interaction",
    slug="wrenchinteraction",
    sites=gt6world.declare_sites(PIPE, CHEST, TRANS),
    preferred_ports=(26802, 26812),      # this card's pinned rcon/query pair (after 26792)
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
