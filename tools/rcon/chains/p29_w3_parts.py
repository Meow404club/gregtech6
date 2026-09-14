#!/usr/bin/env python3
"""p29-w3-nbtdesign-parts — the part-family chain (task p29-w3-nbtdesign-parts
RCON: 逐部件 setblock + 模型/方块状态资源完整性 + the design render-slot probes).

The part-family expansion is a PURE REGISTRATION face (no machines, no recipe
surface, no energy): the server-side integrity a dedicated node CAN prove is
(a) every part block id parses and places (setblock "Changed the block" — the
registry + blockstate JSON variant parse), and (b) the DESIGN render dimension
is LIVE — the design property accepts its full 0..N range, distinguishes
variants (execute if block design=<n> Test passed/failed), and REJECTS an
out-of-range value (the dense wall at design=8: "does not accept" — the
IntegerProperty census 0..7 enforced). The client-side model texture face is
the runClient/render-wave territory (the declared scope note on the card).

The two card-consumption faces carry LIVE probes: dense_wall_tungsten design=2
(the Dynamo emitter plate, card ③) and distill_part design=1 (the back-hole
column, card ④).

passes=2 is the idempotency proof (the [0,0] of this chain).
Run:  GT6_SESSION=off python3 tools/rcon/chains/p29_w3_parts.py
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

# the fresh z=292 band: 41 part blocks on x-disjoint even columns x384..464
X0, Z = 384, 292

PARTS = [
    # the eleven metal walls (:1143-1153, texture metalwall, DESIGNS 7)
    "machine_wall_lead", "machine_wall_bronze", "machine_wall_steel",
    "machine_wall_galvanized_steel", "machine_wall_stainless_steel",
    "machine_wall_invar", "machine_wall_titanium", "machine_wall_tungstensteel",
    "machine_wall_tungsten", "machine_wall_tantalum_hafnium_carbide",
    "machine_wall_adamantium",
    # the eleven dense walls (:1155-1165, metalwalldense, DESIGNS 7)
    "dense_wall_stainless_steel", "dense_wall_invar", "dense_wall_titanium",
    "dense_wall_tungstensteel", "dense_wall_adamantium", "dense_wall_lead",
    "dense_wall_bronze", "dense_wall_steel", "dense_wall_galvanized_steel",
    "dense_wall_tungsten", "dense_wall_tantalum_hafnium_carbide",
    # the six coils (18040-18045, coil, DESIGNS 1)
    "large_copper_coil", "niobium_titanium_coil", "large_nichrome_coil",
    "large_carborundum_coil", "large_osmium_coil", "large_iridium_coil",
    # the six parts (:1174-1182)
    "centrifuge_part", "electrolyzer_part", "distill_part", "sluice_part",
    "crusher_wheels", "shredder_blades",
    # ventilation + the five processor units + the wood wall
    "ventilation_unit", "processor_unit_versatile", "processor_unit_logic",
    "processor_unit_control", "processor_unit_storage",
    "processor_unit_conversion", "wood_wall",
]

SITES = {name: gt6world.Site(X0 + i * 2, 65, Z, dy=1) for i, name in enumerate(PARTS)}
P = {name: F(site) for name, site in SITES.items()}
DENSE_TUNGSTEN = P["dense_wall_tungsten"]
DISTILL = P["distill_part"]
COIL = P["large_copper_coil"]
CENTRIFUGE = P["centrifuge_part"]
LEAD = P["machine_wall_lead"]
WOOD = P["wood_wall"]

X_LAST = X0 + (len(PARTS) - 1) * 2

steps = [
    phase("A: the eleven metal walls — every id parses and places"),
    *[Step(f"setblock {P[name]} gt6:{name}", expect="Changed the block") for name in PARTS[:11]],
    # the metalwall family range top: design=7 accepted (the DESIGNS census on the wire)
    Step(f"setblock {LEAD} gt6:machine_wall_lead[design=7]", expect="Changed the block"),
    Step(f"execute if block {LEAD} gt6:machine_wall_lead[design=7]", expect="Test passed"),

    phase("B: the eleven dense walls"),
    *[Step(f"setblock {P[name]} gt6:{name}", expect="Changed the block") for name in PARTS[11:22]],

    phase("C: the six coils + the six parts"),
    *[Step(f"setblock {P[name]} gt6:{name}", expect="Changed the block") for name in PARTS[22:34]],

    phase("D: ventilation + processor units + the wood wall"),
    *[Step(f"setblock {P[name]} gt6:{name}", expect="Changed the block") for name in PARTS[34:]],

    phase("E: the DESIGN render-slot probes — range, variant distinction, the two card faces"),
    # dense_wall design2 = the Dynamo emitter plate (card ③'s consumption face)
    Step(f"setblock {DENSE_TUNGSTEN} gt6:dense_wall_tungsten[design=2]", expect="Changed the block"),
    Step(f"execute if block {DENSE_TUNGSTEN} gt6:dense_wall_tungsten[design=2]", expect="Test passed"),
    Step(f"execute if block {DENSE_TUNGSTEN} gt6:dense_wall_tungsten[design=7]", expect="Test failed"),
    # distill_part design1 = the back-hole column (card ④'s consumption face)
    Step(f"setblock {DISTILL} gt6:distill_part[design=1]", expect="Changed the block"),
    Step(f"execute if block {DISTILL} gt6:distill_part[design=1]", expect="Test passed"),
    Step(f"execute if block {DISTILL} gt6:distill_part[design=0]", expect="Test failed"),
    # the per-family ranges: coil 0..1 accepts 1, centrifuge 0..8 accepts 8,
    # the metalwall family rejects the out-of-range 8
    Step(f"setblock {COIL} gt6:large_copper_coil[design=1]", expect="Changed the block"),
    Step(f"setblock {CENTRIFUGE} gt6:centrifuge_part[design=8]", expect="Changed the block"),
    Step(f"setblock {LEAD} gt6:machine_wall_lead[design=8]", expect="does not accept"),
    # the DESIGNS-0 rows carry NO design property (a property argument is unknown)
    Step(f"setblock {WOOD} gt6:wood_wall[design=0]", expect="does not have property"),
    # the wood wall flammability face: the isFlammable block parse is server-side
    Step(f"execute if block {WOOD} gt6:wood_wall", expect="Test passed"),

    phase("F: teardown — restore the band"),
    Step(f"fill {X0 - 1} 63 {Z - 1} {X_LAST + 1} 68 {Z + 1} air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p29_w3_parts parts",
    slug="p29w3parts",
    sites=gt6world.declare_sites(*SITES.values()),
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
