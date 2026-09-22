#!/usr/bin/env python3
"""p13-large-boiler — the Large Boiler multiblock acceptance chain (declarative framework).

Chain semantics (task p13-large-boiler ACCEPTANCE ②):

  A the positive chain — the FULL five-pipe-hole loop:
    place → frame (34 parts) → wand (formed, stock 9+25 consumed) → check
    (linked_parts=34/34) → the FIVE collector pipes around the pipe holes (top-centre
    up + W/E/N/S side holes) → fill 128000 → the W2 firebox arm UNDER the transmitter
    base (the 16 HU/t emit → HeatTransmitter relay → controller booked>0, the live proof
    of the energy-relay chain) → the p14-era cadence: water+HU rounds then the THIRD and
    FOURTH water fills (the vanilla-water scaling branch runs the row at the 5000
    efficiency floor, ~10.24M steam per round; the SS steam tank half is 40.96M — the
    half-gate needs the tank ABOVE it) → stat asserts the SS row (output=8192, the static
    row config, GTMultiBlockCommand.java:1236) → data get on ALL FIVE pipes asserting
    gt6:steam (the live load-balance evidence; the exact split is the offline truth
    table's) → the EngineSteam e2e off the top pipe (8 fills bootstrap, active=true) →
    crusher out (the authoritative probe, the p13 W3 lesson) → in-arm teardown
    (extinguish/plunge/mode off).

  B the explosion arms, one ISOLATED SITE each (the strongest blast is strength ~6.2,
    a ~7 block crater), each with the server-alive assert:
    B1 the BREAK-ANY-WALL arm (the card's review focus): formed + pressurised
       (barometer >= 6 via 2x9M HU) → /gt6multiblock hole on a middle wall → the tick's
       structural probe (:262) detonates → controller gone.
    B2 the dismantle arm (the W3 review note): formed + pressurised →
       /gt6multiblock boiler dismantle → explode(T) instant → controller gone.
    B3 the NEGATIVE: formed + NO pressure (barometer 0) → hole on a wall → the
       structure breaks, the boiler SURVIVES (the strict mBarometer > 4 gate).

P34 MIGRATION (task p34-boiler-tick-step, the p32-ops-neo-tick-primitive ruling:
"p13-large-boiler 迁移归后续卡=认可"): every wall-clock timing arm — the sleeps and
the four poll loops — is REPLACED by the P32 deterministic freeze window
(framework.Step.tick_step). The neo (1.21.1) leg freezes the world, steps exactly N
ticks, judges the probe ONCE at gametime g0+N (the transcript anchor line "judged
frozen at gametime <g0+N>" is acceptance-②'s proof), then thaws; the forge (1.20.1)
leg has no TickCommand at all and degrades to the DECLARED tick_fallback_poll resend
loop — same probe, same expect, judge-once semantics.

THE TICK BUDGETS (calibration, per the card: 锅炉转化/出汽/爆炸臂 three segments).
Business ground, all in TileEntityLargeBoiler.java unless noted:

  转化段 (conversion) — the full-accept fill windows, tick_step=10:
    tConversions = min(steamTank/2560, mEnergy/80, water) = 32000 L/t for the SS row
    (:393, tank 81920000/2560; energy 20.48M/80 = 256000 > 128000 water => water-
    limited). The 128000 L water tank drains in EXACTLY 4 ticks, so at g0+10 the tank
    is empty and the next fill accepts the full 128000 deterministically (the old
    poll=15 waited wall-clock for the same drain). N=10 = 2.5x the 4-tick drain.
    forge degrade: poll 10s (drain ~1s at 20tps).
  出汽段 (steam output) — the P1 collector window, tick_step=40:
    the emit only runs ABOVE half tank (tAmount = amount - capacity/2 > 0, :427-430)
    and pushes min(mOutput | mOutput*2, tAmount) per tick to the five collectors
    (:436). Across the three fill windows (3x10) + real roundtrips the 16 conversion
    ticks (512000/32000) have elapsed before the P1 window, so the tank is past half
    and P1 rides 40 MORE emit ticks before its frozen read; P2..P5 stay plain (the
    thawed world keeps emitting and the pipes HOLD — no drain face, monotone).
    forge degrade: poll 10s.
  爆炸臂 (explosion arms) — tick_step=10/30/20:
    B1/B2 top-up window, tick_step=10 with the exact partial accept
    "filled 112500/128000": the 9M HU inject is ENERGY-limited — 9M/80 = 112500 L
    converts in 4 ticks and the tank rests at exactly 15500 L (mEnergy=0 stops the
    drain), so the top-up accepts exactly 112500 (the reply carries the real figure
    and (ACCEPTED), GTMultiBlockCommand.java:1274-1275). The rest is energy-determined,
    not timing-determined — deterministic at ANY freeze point after the drain.
    B1 detonation window, tick_step=30: the hole flags the controller, the NEXT tick's
    :262 probe (mBarometer > 4 && !checkStructure) detonates; worst rng floor
    (both rounds at the 5000 efficiency floor) arms the gauge within ~8 ticks of the
    hole (round-1 steam >= 9M + round-2 at 2.56M/t crosses 5/31 of 81.92M = 13.2M in
    <= 2 ticks; already-armed timelines detonate at tick 1). N=30 = ~4x margin.
    B2 dismantle window, tick_step=10: explode(T) is synchronous in the command.
    B3 survive window, tick_step=20: the negative now proves the controller survives
    20 post-hole ticks, not merely one instant (the tick-window upgrade over the old
    sleep=2.0 + single execute-if).
  engine tail — fill#8 window tick_step=20, crusher window tick_step=400:
    the engine converts its WHOLE tank per tick (tConversions = amount/200 batches,
    GTSteamEngineBlockEntity.java:260, tank 25600 L), so the seven fills land
    back-to-back (the old sleep=0.5 pacing was pure conservatism) and fill#8's
    head-room exists after <= 2 engine ticks; N=20 = margin. The crusher recipe is
    KU-CUMULATIVE, not wall-paced: progress charges by the engine packet (~52-64 KU/t
    at state 12-14) up to maxProgress 16384 (the baseline probe read progress=16400
    at its 15 s give-up, the authoritative poll hit 1 s later => ~270-300 powered
    ticks) — N=400 in-window ticks at ~60 KU/t plus the pre-window feed the fills/
    stat already banked = ~1.5x the 16384 KU the row needs. forge degrades: poll
    15s/75s (the old budgets; the retired 15 s probe poll gave up EVERY pass — the
    deterministic window is the wall win here).

Run:  python3 tools/rcon/chains/p13_large_boiler.py
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

# The sites.
#   E2E — the full loop at the boiler (210,64,150) facing north (anchor 210,64,151):
#         transmitters y63, walls y64..66, pipe holes at y65 x/z rims + y66 centre,
#         collectors P1..P5 one cell outside each hole, firebox under the base,
#         engine+crusher east of the top pipe.
#   B1  — the break-any-wall site at (240,64,170), strength ~6.2, the 9-block bbox.
#   B2  — the dismantle site at (262,64,192), ditto.
#   B3  — the low-pressure negative at (284,64,214), no explosion at all.
E2E = gt6world.Site(210, 64, 150, dx=4, dy=4, dz=4)
B1 = gt6world.Site(240, 64, 170, dx=9, dy=9, dz=9)
B2 = gt6world.Site(262, 64, 192, dx=9, dy=9, dz=9)
B3 = gt6world.Site(284, 64, 214, dx=7, dy=7, dz=7)

LB = "210 64 150"
FIREBOX = "210 62 151"
P1 = "210 67 151"  # above the top-centre hole (210,66,151), face DOWN
P2 = "208 65 151"  # west of the west hole (209,65,151), face EAST
P3 = "212 65 151"  # east of the east hole (211,65,151), face WEST
P4 = "210 65 149"  # north of the north hole (210,65,150), face SOUTH
P5 = "210 65 153"  # south of the south hole (210,65,152), face NORTH
ENGINE = "211 67 151"
CRUSHER = "212 67 151"
VARIANT = "large_boiler_stainless_steel"

B1_LB = "240 64 170"
B1_WALL = "239 64 170"      # a middle-ring wall (anchor 240,64,171; the west cell)
B2_LB = "262 64 192"
B3_LB = "284 64 214"
B3_WALL = "283 64 214"

# The calibrated tick budgets (the docstring's calibration table, kept next to use).
N_DRAIN = 10        # 转化段: 4-tick water drain (32000 L/t), 2.5x margin
N_EMIT = 40         # 出汽段: 40 emit ticks past the half gate before the P1 read
N_TOPUP = 10        # 爆炸臂: 4-tick energy-limited drain to the 15500 L rest
N_DETONATE = 30     # 爆炸臂: the :262 probe detonates within ~8 ticks of the hole
N_DISMANTLE = 10    # 爆炸臂: explode(T) is synchronous
N_SURVIVE = 20      # 爆炸臂: the negative survives 20 post-hole ticks
N_ENGINE = 20       # engine tail: the tank drains within 2 engine ticks
N_CRUSHER = 400     # engine tail: 16384 KU at ~60 KU/t in-window + the pre-window feed

steps = []

# ---------------------------------------------------------------- A: the e2e loop
steps += [
    phase("A: the positive chain — frame, wand, firebox through the transmitter relay, the five-pipe-hole balance, the engine e2e"),
    Step(f"gt6multiblock boiler place {LB} {VARIANT}", expect="GT6 large boiler placed"),
    Step(f"gt6multiblock boiler frame {LB} {VARIANT}", expect="34 parts placed"),
    Step(f"gt6multiblock boiler wand {LB} {VARIANT}", expect="boiler wand check OK"),
    Step(f"gt6multiblock boiler check {LB}", expect="linked_parts=34/34"),
    Step(f"gt6multiblock boiler check {LB}", expect="block_formed=true"),
    # the five collectors, one pipe outside each pipe hole (place <face> links the OPPOSITE
    # support face — face 4 west links east(32), face 2 north links south(8), etc.; the W3
    # place-1-down / toggle-east / output-east form on P1)
    Step(f"gt6pipe place {P1} 1", expect="connections 1"),    # down → the top-centre hole
    Step(f"gt6pipe toggle {P1} 5", expect="connections 33"),  # east → the engine pull face
    Step(f"gt6pipe output {P1} 5", expect="ioMask 32"),       # the east push mouth into the engine
    Step(f"gt6pipe place {P2} 4", expect="connections 32"),   # face west → link east → the west hole
    Step(f"gt6pipe place {P3} 5", expect="connections 16"),   # face east → link west → the east hole
    Step(f"gt6pipe place {P4} 2", expect="connections 8"),    # face north → link south → the north hole
    Step(f"gt6pipe place {P5} 3", expect="connections 4"),    # face south → link north → the south hole
    # the water + the firebox arm (the real HU source THROUGH the transmitter relay)
    Step(f"gt6multiblock boiler fill {LB} 128000", expect="filled 128000/128000 L of minecraft:water (ACCEPTED)"),
    Step(f"gt6burner place {FIREBOX} brick_burning_box", expect="GT6 burning box placed"),
    Step(f"gt6burner fuel {FIREBOX} minecraft:coal 12", expect="minecraft:coal x12"),
    Step(f"gt6burner ignite {FIREBOX}", expect="burning=true"),
    # the relay-chain readback (formality probe — the LIVE relay evidence is the past-half
    # emit below; the pre-inject book is the firebox's 16 HU/t through the transmitter)
    Step(f"gt6multiblock boiler stat {LB}", expect="Stored Heat Units"),
    # round 1: 128000 water + 20480000 HU; the conversion (32000 L/t) drains the tank in
    # 4 ticks, so the NEXT fill rides a freeze window and accepts the full 128000 at an
    # exact tick (was: sleep=2.5 wall-clock pacing)
    Step(f"gt6multiblock boiler inject-hu {LB} 20480000", expect="booked 20480000/20480000 HU (ACCEPTED)"),
    Step(f"gt6multiblock boiler fill {LB} 128000", expect="filled 128000/128000 L of minecraft:water (ACCEPTED)",
         tick_step=N_DRAIN, tick_fallback_poll=10.0),
    # round 2: same shape (was: sleep=2.5)
    Step(f"gt6multiblock boiler inject-hu {LB} 20480000", expect="booked 20480000/20480000 HU (ACCEPTED)"),
    Step(f"gt6multiblock boiler fill {LB} 128000", expect="filled 128000/128000 L of minecraft:water (ACCEPTED)",
         tick_step=N_DRAIN, tick_fallback_poll=10.0),
    # round 3 top-up: same drain guarantee (was: poll=15.0 — the fill#3 conversion empties
    # the tank within 4 ticks, so the single frozen fill accepts the whole 128000)
    Step(f"gt6multiblock boiler fill {LB} 128000", expect="filled 128000/128000 L of minecraft:water (ACCEPTED)",
         tick_step=N_DRAIN, tick_fallback_poll=10.0),
    # the SS row config (STATIC mOutput — GTMultiBlockCommand.java:1236, not a live value)
    Step(f"gt6multiblock boiler stat {LB}", expect="output=8192"),
    # the live load-balance evidence: the past-half emit has pushed steam into the
    # collectors (the tank crossed half inside the fill windows — 16 conversion ticks
    # <= the 30 stepped ticks); P1 rides an emit window, P2..P5 stay plain (the thawed
    # world keeps emitting; the pipes HOLD — monotone)
    Step(f"data get block {P1}", expect="gt6:steam",
         tick_step=N_EMIT, tick_fallback_poll=10.0),
    Step(f"data get block {P2}", expect="gt6:steam"),
    Step(f"data get block {P3}", expect="gt6:steam"),
    Step(f"data get block {P4}", expect="gt6:steam"),
    Step(f"data get block {P5}", expect="gt6:steam"),
    # the KU e2e: the engine off the top pipe (the W3 bootstrap: eight fills → state 14,
    # packet 60 safely under the crusher maxIn 64 cliff; the engine converts its whole
    # tank per tick, so the fills land back-to-back — the old sleep=0.5 pacing dropped)
    Step(f"setblock {ENGINE} gt6:steam_engine_tungsten[facing=east]", expect="Changed the block"),
    Step(f"gt6machine crusher place {CRUSHER}", expect="GT6 crusher placed"),
    Step(f"gt6machine crusher input 8 {CRUSHER}", expect="gem_glass into slot 0"),
    Step(f"gt6engine fill {ENGINE} 24000", expect="filled 24000/24000 L of gt6:steam (ACCEPTED)"),
    Step(f"gt6engine fill {ENGINE} 24000", expect="filled 24000/24000 L of gt6:steam (ACCEPTED)"),
    Step(f"gt6engine fill {ENGINE} 24000", expect="filled 24000/24000 L of gt6:steam (ACCEPTED)"),
    Step(f"gt6engine fill {ENGINE} 24000", expect="filled 24000/24000 L of gt6:steam (ACCEPTED)"),
    Step(f"gt6engine fill {ENGINE} 24000", expect="filled 24000/24000 L of gt6:steam (ACCEPTED)"),
    Step(f"gt6engine fill {ENGINE} 24000", expect="filled 24000/24000 L of gt6:steam (ACCEPTED)"),
    Step(f"gt6engine fill {ENGINE} 24000", expect="filled 24000/24000 L of gt6:steam (ACCEPTED)"),
    # the last landing: the engine drained its tank within 2 ticks of fill#7 — the frozen
    # window proves the head-room at an exact tick (was: poll=15.0, which always accepted
    # on the first send anyway)
    Step(f"gt6engine fill {ENGINE} 24000", expect="filled 24000/24000 L of gt6:steam (ACCEPTED)",
         tick_step=N_ENGINE, tick_fallback_poll=15.0),
    Step(f"gt6engine stat {ENGINE}", expect="active=true"),
    # the recipe's authoritative window: the row charges 16384 KU cumulatively at the
    # engine's ~52-64 KU/t packet (maxProgress 16384, the RM duration 512 x eut 32) —
    # 400 in-window ticks + the pre-window feed clears it ~1.5x, judged ONCE at the
    # exact tick — replaces the old probe + poll=75 wall-clock pair whose 15 s probe
    # poll gave up every pass (the p13 two-step lesson retired by the deterministic
    # window)
    Step(f"gt6machine crusher check {CRUSHER}", expect="out[0]=",
         tick_step=N_CRUSHER, tick_fallback_poll=75.0),
    # the in-arm teardown
    Step(f"gt6burner extinguish {FIREBOX}", expect="burning=false"),
    Step(f"gt6multiblock boiler plunge {LB}", expect="trashed "),
    Step(f"gt6engine mode {ENGINE} off", expect="stopped=true"),
]

# ---------------------------------------------------------------- B1: the break-any-wall arm
steps += [
    phase("B1: the break-any-wall arm — formed + pressurised, hole on a wall → the tick probe detonates"),
    Step(f"gt6multiblock boiler place {B1_LB} {VARIANT}", expect="GT6 large boiler placed"),
    Step(f"gt6multiblock boiler frame {B1_LB} {VARIANT}", expect="34 parts placed"),
    Step(f"gt6multiblock boiler wand {B1_LB} {VARIANT}", expect="boiler wand check OK"),
    Step(f"gt6multiblock boiler fill {B1_LB} 128000", expect="filled 128000/128000 L of minecraft:water (ACCEPTED)"),
    # the 9M HU inject is ENERGY-limited: 112500 L convert in 4 ticks and the tank rests
    # at exactly 15500 L — the top-up window asserts that exact partial accept at an
    # exact tick (was: sleep=3.0 + an expect-less bare fill; TWO rounds keep the gauge
    # >= 6 worst-case, the >= 5 effective gate for the :262 arm — the old comment's
    # "two rounds keep the gauge >= 8" was the pre-measurement estimate)
    Step(f"gt6multiblock boiler inject-hu {B1_LB} 9000000", expect="booked 9000000/9000000 HU (ACCEPTED)"),
    Step(f"gt6multiblock boiler fill {B1_LB} 128000", expect="filled 112500/128000 L of minecraft:water (ACCEPTED)",
         tick_step=N_TOPUP, tick_fallback_poll=10.0),
    Step(f"gt6multiblock boiler inject-hu {B1_LB} 9000000", expect="booked 9000000/9000000 HU (ACCEPTED)"),
    Step(f"gt6multiblock boiler stat {B1_LB}"),
    # break ONE middle wall — the part-block playerWillDestroy flags the controller, the
    # NEXT tick's checkStructure(false) fails under pressure → the :262 explode(F); the
    # worst rng floor arms the gauge (5/31 of 81.92M = 13.2M steam) within ~8 ticks of
    # the hole, so the frozen assert reads the gone controller at g0+30 (was: sleep=4.0
    # + a single-shot execute)
    Step(f"gt6multiblock hole {B1_WALL}", expect="GT6 part broken"),
    Step(f"execute unless block {B1_LB} gt6:{VARIANT}", expect="Test passed",
         tick_step=N_DETONATE, tick_fallback_poll=12.0),
    Step("time query daytime", expect="The time is"),
]

# ---------------------------------------------------------------- B2: the dismantle arm
steps += [
    phase("B2: the dismantle arm (the W3 review note) — pressurised controller dismantle → explode(T) instant"),
    Step(f"gt6multiblock boiler place {B2_LB} {VARIANT}", expect="GT6 large boiler placed"),
    Step(f"gt6multiblock boiler frame {B2_LB} {VARIANT}", expect="34 parts placed"),
    Step(f"gt6multiblock boiler wand {B2_LB} {VARIANT}", expect="boiler wand check OK"),
    Step(f"gt6multiblock boiler fill {B2_LB} 128000", expect="filled 128000/128000 L of minecraft:water (ACCEPTED)"),
    Step(f"gt6multiblock boiler inject-hu {B2_LB} 9000000", expect="booked 9000000/9000000 HU (ACCEPTED)"),
    Step(f"gt6multiblock boiler fill {B2_LB} 128000", expect="filled 112500/128000 L of minecraft:water (ACCEPTED)",
         tick_step=N_TOPUP, tick_fallback_poll=10.0),  # the exact 15500 L rest, see B1
    Step(f"gt6multiblock boiler inject-hu {B2_LB} 9000000", expect="booked 9000000/9000000 HU (ACCEPTED)"),
    Step(f"gt6multiblock boiler dismantle {B2_LB}", expect="EXPLODED (instant)"),
    # explode(T) is synchronous in the command — the window just proves the gone
    # controller across ticks (was: sleep=4.0 + a single-shot execute)
    Step(f"execute unless block {B2_LB} gt6:{VARIANT}", expect="Test passed",
         tick_step=N_DISMANTLE, tick_fallback_poll=8.0),
    Step("time query daytime", expect="The time is"),
]

# ---------------------------------------------------------------- B3: the low-pressure negative
steps += [
    phase("B3: the NEGATIVE — formed, NO pressure: hole breaks the structure, the boiler SURVIVES"),
    Step(f"gt6multiblock boiler place {B3_LB} {VARIANT}", expect="GT6 large boiler placed"),
    Step(f"gt6multiblock boiler frame {B3_LB} {VARIANT}", expect="34 parts placed"),
    Step(f"gt6multiblock boiler wand {B3_LB} {VARIANT}", expect="boiler wand check OK"),
    Step(f"gt6multiblock hole {B3_WALL}", expect="GT6 part broken"),
    # the tick-window upgrade: the controller survives 20 post-hole ticks (the strict
    # mBarometer > 4 gate never arms with zero HU), not merely one instant
    # (was: sleep=2.0 + a single-shot execute)
    Step(f"execute if block {B3_LB} gt6:{VARIANT}", expect="Test passed",
         tick_step=N_SURVIVE, tick_fallback_poll=8.0),
    Step(f"gt6multiblock boiler stat {B3_LB}", expect="barometer=0"),
    Step("time query daytime", expect="The time is"),
]

# ---------------------------------------------------------------- C: teardown
steps += [
    phase("C: teardown — the explicit restore over every arm site (the pass-open bbox is the backstop)"),
    Step("fill 206 60 146 214 68 154 air", expect="filled"),
    Step("fill 231 55 161 249 73 179 air", expect="filled"),
    Step("fill 253 55 183 271 73 201 air", expect="filled"),
    Step("fill 277 55 207 291 73 221 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p13-large-boiler",
    slug="p13lb",
    sites=gt6world.declare_sites(E2E, B1, B2, B3),
    preferred_ports=(25734, 25744),      # this card's pinned rcon/query pair
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
