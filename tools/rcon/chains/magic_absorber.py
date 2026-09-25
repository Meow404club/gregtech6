#!/usr/bin/env python3
"""magic-absorber — the Magic Field Absorber live acceptance (sweep group
magic_absorber; the qu_laser rig geometry, two columns):

  A THE QU LEG (acceptance ②): the T1 small massfab (gt6:massfab, window
    min 16) is the QU sink — the absorber's size-1 QU packets sit below its
    min door, so the Root white-burn door eats them and the emitter side
    books the out (the qu-laser live lesson: the Root gate counts a
    burned offer as USED — the open wire would book 0). The Dragon Egg on
    the absorber's TOP face (the :85-86 arm; the /setblock neighbor update
    re-arms the :79 probe) arms "active true, type QU, out 64"; the reset ->
    stat window pins the meter RATIO "rate 64" (delivered/window-ticks, the
    laser stat's "half true" form — exact while the trophy emits every tick,
    tick-rate-independent for the judge). The /give arm is the FML item
    registration live half (the setblock arms are the block/BET half).

  B THE TU LEG (acceptance ②): the coagulator (water 1000 -> 1 snowball,
    eUt 1, the coagulator smoke row) is the TU sink — the skull (the
    :87-88 arm) arms "active true, type TU, out 1" and the meter pins
    "rate 1"; the CONSUMER side completes the recipe ("out[0]=1x snowball")
    — the size-1 packets ride the coagulator's minIn=1 door and bank into
    real progress, the true in-network metering.

  C THE IDLE LEG (acceptance ③): trophies removed -> the probe re-arms the
    :82-83 idle defaults ("active false", out re-armed 64, the emitted TYPE
    keeps the last value — upstream :82-83 never resets it), and the reset
    window pins "delivered 0, rate 0" on BOTH absorbers — no trophy, no
    emission, no book.

passes=2 is the idempotency proof (the [0,0] of the group).
Run:  GT6_SESSION=off python3 tools/rcon/chains/magic_absorber.py
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

Z = 352  # the fresh band east of the qu-laser columns (x444..458)
# the QU column: massfab (sink) -> absorber (facing north, output face INTO the
# massfab's back) with the Dragon Egg on top
MQ = gt6world.Site(470, 65, Z)           # gt6:massfab[facing=north] — the QU sink
ABS_Q = gt6world.Site(470, 65, Z + 1)    # gt6:magic_absorber[facing=north]
EGG = gt6world.Site(470, 66, Z + 1)      # minecraft:dragon_egg (the trophy seat = TOP)
# the TU column: coagulator (sink) -> absorber with the skull on top
COAG = gt6world.Site(474, 65, Z)         # /gt6machine coagulator place
ABS_T = gt6world.Site(474, 65, Z + 1)    # gt6:magic_absorber[facing=north]
SKULL = gt6world.Site(474, 66, Z + 1)    # minecraft:skeleton_skull
CHEST = gt6world.Site(472, 65, Z + 1)    # the item-registration census chest (the usb-chain form)

steps = [
    phase("A: the QU leg — the Dragon Egg arms QU 64, the meter ratio pins 64"),
    Step(f"fill 468 62 {Z - 2} 476 68 {Z + 5} air", expect="filled"),
    Step(f"forceload add 468 {Z - 2} 476 {Z + 5}"),
    Step(f"setblock {F(CHEST)} minecraft:chest", expect="Changed the block"),
    Step(f"item replace block {F(CHEST)} container.0 with gt6:magic_absorber 1", expect="Replaced"),
    Step(f"data get block {F(CHEST)} Items[0]", expect="gt6:magic_absorber"),
    Step(f"setblock {F(MQ)} gt6:massfab[facing=north]", expect="Changed the block"),
    Step(f"setblock {F(ABS_Q)} gt6:magic_absorber[facing=north]", expect="Changed the block"),
    Step(f"setblock {F(EGG)} minecraft:dragon_egg", expect="Changed the block"),
    Step(f"gt6magicabsorber stat {F(ABS_Q)}", expect="active true, type QU, out 64, facing 2", sleep=1.5, poll=10.0),
    Step(f"gt6magicabsorber reset {F(ABS_Q)}", expect="GT6 magic absorber accounting reset"),
    Step(f"gt6magicabsorber stat {F(ABS_Q)}", expect="type QU, out 64, facing 2, delivered ", sleep=3.0),
    Step(f"gt6magicabsorber stat {F(ABS_Q)}", expect="rate 64", sleep=0.5),

    phase("B: the TU leg — the skull arms TU 1, the coagulator completes on the packets"),
    Step(f"gt6machine coagulator place {F(COAG)}", expect="GT6 coagulator placed at"),
    Step(f"gt6machine coagulator fluid fill up minecraft:water 1000 {F(COAG)}",
         expect="filled 1000/1000 L of minecraft:water (ACCEPTED), input tanks hold 1000 L"),
    Step(f"setblock {F(ABS_T)} gt6:magic_absorber[facing=north]", expect="Changed the block"),
    Step(f"setblock {F(SKULL)} minecraft:skeleton_skull", expect="Changed the block"),
    Step(f"gt6magicabsorber stat {F(ABS_T)}", expect="active true, type TU, out 1, facing 2", sleep=1.5, poll=10.0),
    Step(f"gt6magicabsorber reset {F(ABS_T)}", expect="GT6 magic absorber accounting reset"),
    Step(f"gt6magicabsorber stat {F(ABS_T)}", expect="rate 1", sleep=2.0),
    Step(f"gt6machine coagulator check {F(COAG)}",
         expect="out[0]=1x snowball",
         node_expects={"1.21.1": "out[0]=1x minecraft:snowball"},
         poll=30.0),

    phase("C: the idle leg — no trophy, no emission, no book (the :82-83 defaults re-arm)"),
    Step(f"setblock {F(EGG)} air", expect="Changed the block"),
    Step(f"setblock {F(SKULL)} air", expect="Changed the block"),
    Step(f"gt6magicabsorber reset {F(ABS_Q)}", expect="GT6 magic absorber accounting reset"),
    Step(f"gt6magicabsorber reset {F(ABS_T)}", expect="GT6 magic absorber accounting reset"),
    Step(f"gt6magicabsorber stat {F(ABS_Q)}", expect="active false, type QU, out 64, facing 2, delivered 0, rate 0", sleep=2.0),
    Step(f"gt6magicabsorber stat {F(ABS_T)}", expect="active false, type TU, out 64, facing 2, delivered 0, rate 0", sleep=0.5),

    phase("D: teardown — restore the band"),
    Step(f"fill 468 62 {Z - 2} 476 68 {Z + 5} air", expect="filled"),
    Step("forceload remove all"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="magic_absorber magic field absorber",
    slug="magicabsorber",
    sites=gt6world.declare_sites(MQ, ABS_Q, EGG, COAG, ABS_T, SKULL),
    preferred_ports=(26584, 26594),
    game_port=26438,
    steps=steps,
)

if __name__ == "__main__":
    main(CHAIN)
