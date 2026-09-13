#!/usr/bin/env python3
"""p29-w2-energy-types — the MU/LU/CU/TU energy-type dial chain (task
p29-w2-energy-types-5tier, the RCON group p29_w2_energy = this one chain).

The card is the W2 shared-layer card: the W2 consumer machines do NOT exist yet, so
this chain drives the SOURCE side the consumer cards will hang off (the card line
"无机器消费臂（消费者卡各挂本组源块先例）"):

  A-D one arm per exotic type (MU/LU/CU/TU): /gt6energy type dials the SHORT name, the
    stat round-trips the registered mName — the LIVE resolveEnergyType loop to the
    SHARED TD.Energy instance (GTEnergySourceBlockEntity.resolveEnergyType :293-308;
    the :501 machine gate is reference equality, so the dial must land the same
    instance the offline guard test pins). MU -> ENERGY.MAGNETIC / LU -> ENERGY.LIGHT /
    CU -> ENERGY.CRYO / TU -> ENERGY.TIME.
  D (TU) also dials the [1,16] window EDGES the future TU four rows ride
    (Loader:1650-1655 NBT_INPUT 1 / MIN 1 / MAX 16): volt 1 and volt 16 round-trip.
  E the type-gate LIVE proof (the pre-existing shredder as the gate probe, NOT a W2
    consumer arm): the rig sits directly UNDER the shredder (its up face feeds the
    shredder's SBIT_D energy face, Loader:1294 NBT_ENERGY_ACCEPTED_SIDES SBIT_D).
    dialed EU -> the shredder refuses (energy=0, active=false); re-dialed RU -> the
    same rig drives the shredder active (the dialed instance IS the machine's carrier
    instance — the reference-equality gate met LIVE); re-dialed LU mid-run -> the
    packets stop matching, the drain empties the buffer, the shredder goes inactive
    (and resets — it is a constant-power machine, the live :894 half; the RETAINED
    half is the offline suite's, no NO_CONSTANT_POWER machine exists live yet).
  F the no-rogue-mint arm: an unknown type name is REFUSED ("TYPE FAILED: unknown
    energy type") and the rig keeps its previous type — TagData.createTagData is never
    reached (the resolveEnergyType doc contract).

Run:  GT6_SESSION=off python3 tools/rcon/chains/p29_w2_energy_types.py
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

# the fresh z=264 band: four per-type rigs, x-disjoint columns (the p29 W1 band form);
# the gate arm shares the band at x410 with the rig under the shredder.
RIG_MU = gt6world.Site(384, 64, 264, dy=1, dz=1)
RIG_LU = gt6world.Site(390, 64, 264, dy=1, dz=1)
RIG_CU = gt6world.Site(396, 64, 264, dy=1, dz=1)
RIG_TU = gt6world.Site(402, 64, 264, dy=1, dz=1)
RIG_GATE = gt6world.Site(410, 64, 264, dy=1, dz=1)
SHREDDER = gt6world.Site(410, 65, 264, dy=1, dz=1)

steps = [
    phase("A: the MU arm — the dial round-trips the shared ENERGY.MAGNETIC instance"),
    Step(f"gt6energy place {F(RIG_MU)}", expect="GT6 energy source placed at 384, 64, 264"),
    Step(f"gt6energy type {F(RIG_MU)} MU", expect="type ENERGY.MAGNETIC"),
    Step(f"gt6energy stat {F(RIG_MU)}", expect="emitting false, wattage 32 EU/t, type ENERGY.MAGNETIC"),
    Step(f"gt6energy volt {F(RIG_MU)} 64", expect="voltage 64 EU"),
    Step(f"gt6energy mode {F(RIG_MU)} on", expect="emitting true", sleep=1.5),
    Step(f"gt6energy stat {F(RIG_MU)}", expect="voltage 64 EU"),
    Step(f"gt6energy mode {F(RIG_MU)} off", expect="emitting false"),

    phase("B: the LU arm"),
    Step(f"gt6energy place {F(RIG_LU)}", expect="GT6 energy source placed at 390, 64, 264"),
    Step(f"gt6energy type {F(RIG_LU)} LU", expect="type ENERGY.LIGHT"),
    Step(f"gt6energy stat {F(RIG_LU)}", expect="type ENERGY.LIGHT"),

    phase("C: the CU arm"),
    Step(f"gt6energy place {F(RIG_CU)}", expect="GT6 energy source placed at 396, 64, 264"),
    Step(f"gt6energy type {F(RIG_CU)} CU", expect="type ENERGY.CRYO"),
    Step(f"gt6energy stat {F(RIG_CU)}", expect="type ENERGY.CRYO"),

    phase("D: the TU arm — the dial plus the [1,16] window edges (the Loader:1650-1655 TU four)"),
    Step(f"gt6energy place {F(RIG_TU)}", expect="GT6 energy source placed at 402, 64, 264"),
    Step(f"gt6energy type {F(RIG_TU)} TU", expect="type ENERGY.TIME"),
    Step(f"gt6energy stat {F(RIG_TU)}", expect="type ENERGY.TIME"),
    Step(f"gt6energy volt {F(RIG_TU)} 1", expect="voltage 1 EU"),
    Step(f"gt6energy stat {F(RIG_TU)}", expect="voltage 1 EU"),
    Step(f"gt6energy volt {F(RIG_TU)} 16", expect="voltage 16 EU"),
    Step(f"gt6energy stat {F(RIG_TU)}", expect="voltage 16 EU"),

    phase("E: the type-gate live proof — one rig under the shredder, three dials"),
    Step(f"gt6machine shredder place {F(SHREDDER)}", expect="GT6 shredder placed at 410, 65, 264"),
    Step(f"gt6machine shredder input 8 {F(SHREDDER)}", expect="cobblestone into slot 0"),
    Step(f"gt6energy place {F(RIG_GATE)}", expect="GT6 energy source placed at 410, 64, 264"),
    # EU dial: the shredder carries RU (:1294) — the packets are refused, the gate holds
    Step(f"gt6energy type {F(RIG_GATE)} EU", expect="type ENERGY.ELECTRICITY"),
    Step(f"gt6energy mode {F(RIG_GATE)} on", expect="emitting true", sleep=2.0),
    Step(f"gt6machine shredder check {F(SHREDDER)}", expect="energy=0"),
    Step(f"gt6machine shredder check {F(SHREDDER)}", expect="active=false"),
    # RU re-dial: the SAME rig now drives the machine (the dialed instance IS the carrier;
    # the stat step carries the sleep — the server ticks the buffer full between the dial
    # and the check)
    Step(f"gt6energy type {F(RIG_GATE)} RU", expect="type ENERGY.KINETIC_ROTATION"),
    Step(f"gt6energy stat {F(RIG_GATE)}", expect="type ENERGY.KINETIC_ROTATION", sleep=2.0),
    Step(f"gt6machine shredder check {F(SHREDDER)}", expect="active=true"),
    # LU re-dial mid-run: the packets stop matching, the buffer drains, the machine idles
    # (and resets — the shredder is a constant-power machine, the live :894 half)
    Step(f"gt6energy type {F(RIG_GATE)} LU", expect="type ENERGY.LIGHT"),
    Step(f"gt6energy stat {F(RIG_GATE)}", expect="type ENERGY.LIGHT", sleep=3.0),
    Step(f"gt6machine shredder check {F(SHREDDER)}", expect="active=false"),

    phase("F: the no-rogue-mint arm — an unknown name is refused, the type is kept"),
    # the command's "TYPE FAILED" marker is the EXPECTED verdict — allow_failed keeps the
    # framework's FAILED-marker judge from counting the refusal itself as a step failure
    # (the p28_fe_inbound absence-proof form)
    Step(f"gt6energy type {F(RIG_TU)} XY", expect="TYPE FAILED: unknown energy type 'XY'", allow_failed=True),
    Step(f"gt6energy stat {F(RIG_TU)}", expect="type ENERGY.TIME"),

    phase("G: teardown — the explicit band restore (no global state was touched)"),
    Step("fill 383 62 263 411 67 265 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p29-w2-energy-types",
    slug="p29w2energytypes",
    sites=gt6world.declare_sites(RIG_MU, RIG_LU, RIG_CU, RIG_TU, RIG_GATE, SHREDDER),
    preferred_ports=(26321, 26331),      # this card's pinned rcon/query pair
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
