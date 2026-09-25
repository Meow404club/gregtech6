#!/usr/bin/env python3
"""pool-cover-hosts — the real cover-host live acceptance chain.

The live half of task pool-cover-hosts: the two production carriers mount
covers now — GTFluidPipeBlockEntity (ICoverableTE) and GTWireBlockEntity
(ICoverableTE + ITileEntitySwitchableMode, the FIRST switchable-mode host).
One fresh band x696..724, z554..574 (x/z-disjoint from every registered sweep
band; the covers-g2 band sits z300..312 x548..572, the loot-inject band x700
z200..218, the sensors column x678..684 z125..143):

  REDSTONE WIRE 1 (704 65 564): the torch live arm — the wire faces are
    connected through the REAL BE handshake (gt6wire connect up + west; a
    bare /setblock property writes the BlockState but not the BE mask, the
    connector mask is BE-owned and one-way BE -> state). The torch cover
    mounts on the UP face (the real-carrier install; the onCoverPlaced arm
    disconnects the live UP bit, observable connections 18 -> 16 in the
    blockstate), a vanilla redstone block on the connected WEST face feeds
    mRedstone > 0 -> the torch art OFF; removing the block flips the art ON
    and the HOST EXIT drives a real redstone lamp above the covered face
    (getRedstoneOut routes through the cover exits, upstream 04Covers
    :427-438; the flip's buffered causeBlockUpdate re-checks the lamp one
    tick later) — the lamp lights: the live signal-inversion observable.

  REDSTONE WIRE 2 (712 65 564): the selector arm — gt6:cover_selector_tag_5
    on the DOWN face mounts (the covers-g2 live NEGATIVE flips positive: the
    wire IS a switchable-mode host) and the dial write lands on the carrier:
    gt.mode: 5b in the block NBT.

  FLUID PIPE (708 65 564): the pressure-valve arm — the NORTH face connected
    through gt6pipe toggle (the real BE handshake), gt6:cover_pressure_valve
    on the NORTH face mounts (the single-tank gate admits the real pipe),
    the post-tick endpoint arm disconnects the valve face (connections
    2 -> 0).

  NEGATIVES (the drum at 702 65 568): the valve and the torch gates refuse
    the non-pipe / non-wire host — the live carrier-gate verdicts.

Arms:
  A SITE: the clear + the floor + the two wires + the pipe + the drum + the
    redstone block.
  B MASKS: the BE-owned connection masks driven through gt6wire connect /
    gt6pipe toggle (the commands report the mask; the blockstate follows).
  C MOUNTS: three installs answer ok=true (torch/wire, selector/wire,
    valve/pipe) + the two FAILED negatives; the torch mount drops the wire's
    live UP bit (blockstate 18 -> 16).
  D INVERSION: the powered wire keeps the torch art OFF (lamp unlit);
    removing the redstone block flips the art ON and LIGHTS the lamp (the
    host exit through getRedstoneOut).
  E DIAL+ENDPOINT: the selector dial reads gt.mode: 5b; the valve tickPost
    endpoint arm zeroes the pipe's connections.
"""

import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
for _path in (str(_HERE), str(_HERE.parent)):
    if _path not in sys.path:
        sys.path.insert(0, _path)

import gt6world
from framework import Chain, Step, phase

F = gt6world.fmt

A = gt6world.Site(710, 65, 564, dx=14, dy=5, dz=10)  # x696..724, y60..70, z554..574

STONE = "minecraft:stone"
WIRE = "gt6:wire_red_alloy"
PIPE = "gt6:wood_fluid_pipe_small"
DRUM = "gt6:barrel_metal"

WIRE1 = (704, 65, 564)
RBOX = (703, 65, 564)
LAMP = (704, 66, 564)
WIRE2 = (712, 65, 564)
PIPEP = (708, 65, 564)
DRUMP = (702, 65, 568)


CHAIN = Chain(
    name="cover-hosts",
    slug="covhst",
    sites=gt6world.declare_sites(A),
    # NO preferred_ports pin: the single-chain session then falls back to the
    # SESSION_PORTS node table (framework.session_ports) — the segment pair that
    # keeps the --dual forge/neo legs apart by node (a pinned pair would race
    # both legs onto one port triple; the first dual attempt died on
    # "FAILED TO BIND TO PORT" exactly so).
    steps=[
        # ------------------------------------------------------------------
        phase("A: the site — the floor, the two wires, the pipe, the drum, the redstone block"),
        Step("fill %d %d %d %d %d %d air" % (696, 60, 554, 724, 70, 574), expect="filled"),
        Step("forceload add 696 554 724 574"),
        Step("fill 696 64 554 724 64 574 " + STONE, expect="filled", label="the support floor"),
        Step("setblock %s %s" % (F(WIRE1), WIRE), expect="Changed the block",
             label="the torch wire (the mask follows through the BE handshake)"),
        Step("setblock %s minecraft:redstone_block" % F(RBOX), expect="Changed the block",
             label="the power source on the wire's WEST face"),
        Step("setblock %s %s" % (F(WIRE2), WIRE), expect="Changed the block",
             label="the selector wire"),
        Step("setblock %s %s" % (F(PIPEP), PIPE), expect="Changed the block",
             label="the valve pipe"),
        Step("setblock %s %s" % (F(DRUMP), DRUM), expect="Changed the block",
             label="the negative drum"),

        # ------------------------------------------------------------------
        phase("B: the masks — the BE-owned connection state through the real commands"),
        Step("gt6wire connect %s 1" % F(WIRE1), expect="side 1: ok, connections 2",
             label="the torch wire connects UP (the onCoverPlaced arm needs a live bit to clear)"),
        Step("gt6wire connect %s 4" % F(WIRE1), expect="connections 18",
             label="and WEST, onto the redstone block (the power input face)"),
        Step("gt6pipe toggle %s 2" % F(PIPEP), expect="side 2: ok, connections 4",
             label="the valve pipe connects NORTH (the valve face; SBIT[2] = 4)"),

        # ------------------------------------------------------------------
        phase("C: the mounts — the real-carrier installs + the gate negatives"),
        Step("gt6cover install %s up gt6:cover_redstone_torch" % F(WIRE1), expect="ok=true",
             label="THE MOUNT: the torch installs on the REAL wire carrier (ICoverableTE)"),
        Step("execute if block %s %s[connections=16]" % (F(WIRE1), WIRE), expect="Test passed", poll=20.0,
             label="the torch placement disconnected the UP face (18 -> 16, the onCoverPlaced arm)"),
        Step("gt6cover check %s" % F(WIRE1), expect="block/redstonetorch",
             label="the wire snapshot carries the torch plate (the real host serves the cover model)"),
        Step("gt6cover install %s down gt6:cover_selector_tag_5" % F(WIRE2), expect="ok=true",
             label="THE FLIPPED NEGATIVE: the selector mounts on the REAL wire (the FIRST switchable-mode host)"),
        Step("gt6cover install %s north gt6:cover_pressure_valve" % F(PIPEP), expect="ok=true",
             label="THE MOUNT: the valve installs on the REAL single-tank pipe"),
        Step("gt6cover install %s north gt6:cover_pressure_valve" % F(DRUMP), expect="install FAILED",
             label="the NEGATIVE: the valve gate refuses the drum (not a GTFluidPipeBlockEntity)"),
        Step("gt6cover install %s up gt6:cover_redstone_torch" % F(DRUMP), expect="install FAILED",
             label="the NEGATIVE: the torch gate refuses the drum (not the wire carrier)"),

        # ------------------------------------------------------------------
        phase("D: the inversion — powered wire keeps the torch OFF, the freed wire lights the lamp"),
        Step("setblock %s minecraft:redstone_lamp" % F(LAMP), expect="Changed the block",
             label="the observer lamp above the covered face (a blockstate read — lamps carry no BE)"),
        Step("execute if block %s minecraft:redstone_lamp[lit=false]" % F(LAMP), expect="Test passed", poll=20.0,
             label="powered wire -> torch art OFF -> no emission -> the lamp stays unlit"),
        Step("setblock %s minecraft:air" % F(RBOX), expect="Changed the block",
             label="the west power source is removed"),
        Step("execute if block %s minecraft:redstone_lamp[lit=true]" % F(LAMP), expect="Test passed", poll=20.0,
             label="THE INVERSION: the freed wire flips the torch art ON and the HOST EXIT lights the real lamp"),
        Step("gt6cover check %s" % F(WIRE1), expect="block/redstonetorch/on/front",
             label="the plate art reads ON (visual 0) — the wire-host emission route is live"),

        # ------------------------------------------------------------------
        phase("E: the dial + the endpoint — the two remaining host seams"),
        Step("data get block %s" % F(WIRE2), expect="gt.mode: 5b",
             label="the selector dial landed on the carrier: mMode=5 persists as gt.mode (upstream :72)"),
        Step("execute if block %s %s[connections=0]" % (F(PIPEP), PIPE), expect="Test passed", poll=20.0,
             label="the valve tickPost endpoint arm disconnected the NORTH face (2 -> 0, the :52 arm)"),
        Step("gt6cover check %s" % F(PIPEP), expect="block/pressurevalve/front",
             label="the pipe snapshot carries the valve plate"),
    ],
)

if __name__ == "__main__":
    chain = CHAIN
    node = None
    if "--node" in sys.argv:
        node = sys.argv[sys.argv.index("--node") + 1]
    import framework
    if framework.session_enabled():
        sys.exit(framework.run_session([chain], node=node))
    chain.node = node or framework.requested_node()
    sys.exit(framework.run(chain))
