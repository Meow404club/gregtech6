#!/usr/bin/env python3
"""kinetics-be — the Rotation Engine / Small Steam Turbine / metal gearbox-row
acceptance chain (task kinetics-be-function-family, the mc-D 遗留 B 案面).

Chain semantics:

  A the BIPOLAR e2e — rig(RU 32/t) on the engine's NORTH face (the off-axis input
    family, TileEntityBase11Bipolar :63) drives a bronze Rotation Engine
    [facing=east]: V[1]=32 RU in → V[1]/2=16 KU out BOTH axis poles with OPPOSITE
    signs (the doBipolar pair), the two pole crushers BOTH produce (the KU network).
    The /gt6engine stat carries the row pair (row=32→16) + the last-burst record.

  B the SST INVERSE — a bronze Small Steam Turbine [facing=east] burns a direct
    /gt6engine fill (the engine-steam canon, the tank cap = 48×2×4 = 384 L) into RU
    bursts out the FRONT, retained by the DIRECT front-adjacent terminal — the Ti
    Custom Gearbox row (VMAX[3]=1024, the mc-D row block): burst 1 = units(192,48,16)
    = 64 RU ×1 retained as "current speed=64 x power=1", the later (bigger) bursts
    mIgnorePower-rejected while the queue is full — DETERMINISTIC (the
    gearbox-transformer B grammar). The 蒸馏水回吐 lands 1 L per 200 SU in the
    perpendicular byproduct barrel.

  C the metal transformer row — crank → transformer_gearbox_bronze[facing=west]
    (the mc-D row block, V[1]=32 → V[0]=8 through the ÷4×4 multiplier) → treated
    axle → terminal gearbox. The stat proves the ROW ADOPTION: "32→8 row" (not the
    wooden 8→2) + the -4x4 burst pair (the crank's -16x1 ÷4×4'd).

Run:  python3 tools/rcon/chains/kinetics-be.py --node 1.20.1-forge
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

# The declared sites — the x32..40 / z0..16 band @ y64 (bbox-disjoint from the
# axle-family x0..8 band and the burning-box x20 band).
RIG_A, ENGINE_A = gt6world.Site(34, 64, 3), gt6world.Site(34, 64, 4)
CRUSH_E, CRUSH_W = gt6world.Site(35, 64, 4), gt6world.Site(33, 64, 4)
TURBINE, GEARBOX_B, DWBARREL = gt6world.Site(38, 64, 4), gt6world.Site(39, 64, 4), gt6world.Site(38, 64, 5)
CRANK_C, TRANS_C, AXLE_C, GEARBOX_C = (gt6world.Site(32, 64, 8), gt6world.Site(33, 64, 8),
                                       gt6world.Site(34, 64, 8), gt6world.Site(35, 64, 8))

ENGINE = "gt6:rotation_engine_bronze"
TURBINE_BLOCK = "gt6:small_steam_turbine_bronze"
TRANSFORMER = "gt6:transformer_gearbox_bronze"

CHAIN = Chain(
    name="kinetics-be",
    slug="kinbe",
    sites=gt6world.declare_sites(RIG_A, ENGINE_A, CRUSH_E, CRUSH_W,
                                 TURBINE, GEARBOX_B, DWBARREL,
                                 CRANK_C, TRANS_C, AXLE_C, GEARBOX_C),
    preferred_ports=(25748, 25758),      # this card's pinned rcon/query pair (25747 taken by the water wheel)
    game_port=25738,                     # the pinned game port (rcon - 10)
    steps=[
        phase("A: the bipolar e2e — rig(RU 32) north of the bronze rotation engine, both pole crushers produce"),
        Step(f"gt6energy place {F(RIG_A)}", expect="GT6 energy source placed at 34, 64, 3"),
        Step(f"gt6energy type {F(RIG_A)} RU", expect="type ENERGY.KINETIC_ROTATION"),
        Step(f"gt6energy volt {F(RIG_A)} 32", expect="voltage 32"),
        Step(f"setblock {F(ENGINE_A)} {ENGINE}[facing=east]", expect="Changed the block", allow_failed=True),
        Step(f"gt6engine stat {F(ENGINE_A)}", expect="row=32→16 (RU→KU, bipolar both poles)"),
        Step(f"gt6machine crusher place {F(CRUSH_E)}", expect="GT6 crusher placed at 35, 64, 4", allow_failed=True),
        Step(f"gt6machine crusher place {F(CRUSH_W)}", expect="GT6 crusher placed at 33, 64, 4", allow_failed=True),
        Step(f"gt6machine crusher input 8 {F(CRUSH_E)}", expect="gem_glass into slot 0", allow_failed=True),
        Step(f"gt6machine crusher input 8 {F(CRUSH_W)}", expect="gem_glass into slot 0", allow_failed=True),
        Step(f"gt6energy mode {F(RIG_A)} on", expect="emitting true", sleep=2.0, allow_failed=True),
        # the ±16 KU pairs out both poles — BOTH crushers grind (the 16 RU/t budget
        # grinds slowly: the poll bound 60 s)
        Step(f"gt6machine crusher check {F(CRUSH_E)}", expect="out[0]=", poll=60.0),
        Step(f"gt6machine crusher check {F(CRUSH_W)}", expect="out[0]=", poll=60.0),
        Step(f"gt6engine stat {F(ENGINE_A)}", expect="active=true"),
        Step(f"gt6engine stat {F(ENGINE_A)}", expect="last out=16x"),

        phase("B: the SST inverse — direct-intake fill → RU bursts retained by the Ti custom gearbox + the dist-w barrel"),
        Step(f"setblock {F(GEARBOX_B)} gt6:custom_gearbox_titanium", expect="Changed the block", allow_failed=True),
        Step(f"setblock {F(DWBARREL)} gt6:barrel_wood", expect="Changed the block", allow_failed=True),
        Step(f"setblock {F(TURBINE)} {TURBINE_BLOCK}[facing=east]", expect="Changed the block", allow_failed=True),
        Step(f"gt6engine stat {F(TURBINE)}", expect="row=48SU→16RU"),
        Step(f"gt6engine mode {F(TURBINE)} on", expect=": on (stopped=false)", allow_failed=True),
        # the mask FIRST (the unmasked bursts would vent — the queue gate needs the face)
        Step(f"gt6engine gearbox {F(GEARBOX_B)} 16 0", expect="masks set gears=16, axle=0, gearsWork=true"),
        # the tank cap = 48×2×4 = 384 L — one full fill
        Step(f"gt6engine fill {F(TURBINE)} 384", expect="filled 384/384 L of gt6:steam (ACCEPTED)"),
        Step(f"gt6engine stat {F(TURBINE)}", expect="tank=0/", poll=10.0),
        Step(f"gt6engine stat {F(GEARBOX_B)}", expect="current speed=64 x power=1", poll=15.0),
        # the 蒸馏水回吐: 384/200 = 1 L into the perpendicular barrel
        Step(f"gt6tank stat {F(DWBARREL)}", expect="L of gt6:distilled_water", poll=10.0),

        phase("C: the metal transformer row — crank -16x1 ÷4×4'd through the bronze 32→8 row"),
        Step(f"setblock {F(CRANK_C)} gt6:crank[facing=east]", expect="Changed the block", allow_failed=True),
        Step(f"setblock {F(TRANS_C)} {TRANSFORMER}[facing=west]", expect="Changed the block", allow_failed=True),
        Step(f"setblock {F(AXLE_C)} gt6:axle_wood_treated_huge[axis=x]", expect="Changed the block", allow_failed=True),
        Step(f"setblock {F(GEARBOX_C)} gt6:gearbox", expect="Changed the block", allow_failed=True),
        Step(f"gt6engine gearbox {F(GEARBOX_C)} 16 0", expect="masks set gears=16, axle=0, gearsWork=true"),
        Step(f"gt6engine crank {F(CRANK_C)} 3000", expect="armed 3000 ticks"),
        # the ROW ADOPTION readout: the bronze pair 32→8 (NOT the wooden 8→2)
        Step(f"gt6engine stat {F(TRANS_C)}", expect="last in=-16x1", poll=15.0),
        Step(f"gt6engine stat {F(TRANS_C)}", expect="32→8 row"),
        Step(f"gt6engine stat {F(TRANS_C)}", expect="last out=-4x4", poll=10.0),
        # the terminal gearbox RETAINS the converted burst (the gearbox-transformer D shape)
        Step(f"gt6engine stat {F(GEARBOX_C)}", expect="current speed=4 x power=4"),
    ],
)


if __name__ == "__main__":
    main(CHAIN)
