#!/usr/bin/env python3
"""mui-row-dispatch — the use() MUI dispatch-key generalization live chain.

Chain semantics (task mui-row-menu-null-dispatch): GTBasicMachineBlock.use
dispatches the ModularUI chain by the MenuType supplier (opensModularUi: row null
OR row.menu null) instead of the bare mRow==null gate. The KEY itself is pinned
offline by GTBasicMachineBlockDispatchTest (the truth table is not RCON-drivable —
use() needs a real right-click, RCON has no such surface); THIS chain is the
live-server runtime half: every dispatch family places and reports its menu
surface, proving the registrations and the vanilla-menu arms are untouched and
the flipped arm's carrier is the documented menu-less form:

  distillery place → check data=-2  (row != null && menu == null — THE flipped
      arm's live carrier; before this task it sat INERT on the menuBound gate,
      now use() hands it to GT6MuiMachine.tryOpen)
  dryer place → check data=-1       (bound gt6:dryer menu — the vanilla path,
      byte-identical, createMenu still constructs)
  canner place → check data=-1      (bound gt6:canner menu — same vanilla path)
  shredder place → check data=-2    (the row-less family since the mui-a-
      menu-deregistration dereg — the mui-a-open-chain MUI arm, unchanged)

Run:  python3 tools/rcon/chains/mui_row_dispatch.py   (add
--node 1.21.1-neoforge for the second leg; GT6_SESSION=off for the per-chain
boot fallback)
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

# The four dispatch-family sites, >=16 apart, beyond the P16 distillery band
# (x=320-384, z=100) — this card's band starts at x=480.
DIS = gt6world.Site(480, 64, 100, dx=1, dy=2, dz=1)
DRY = gt6world.Site(496, 64, 100, dx=1, dy=2, dz=1)
CAN = gt6world.Site(512, 64, 100, dx=1, dy=2, dz=1)
SHR = gt6world.Site(528, 64, 100, dx=1, dy=2, dz=1)

steps = []

# ------------------------- A: the flipped arm — the menu-less row carrier
steps += [
    phase("A: the flipped arm — the menu-less row carrier (Distillery): the dispatch "
          "key reads row.menu == null, use() hands it to GT6MuiMachine.tryOpen; the "
          "command surface still reports the menu-less data=-2 (createMenu throws the "
          "documented IllegalStateException, the menu-less carrier's GUI is the MUI "
          "chain, not a MenuType)"),
    Step(f"gt6machine distillery place {F(DIS)}", expect="GT6 distillery placed"),
    Step(f"gt6machine distillery check {F(DIS)}", expect="data=-2"),
]

# ------------------------- B: the bound arms — the vanilla menu path, byte-identical
steps += [
    phase("B: the bound arms — dryer/canner keep their gt6:* MenuType suppliers, the "
          "vanilla menu path (createMenu constructs, the idle ContainerData dial "
          "reports data=-1) — byte-identical under the generalized key"),
    Step(f"gt6machine dryer place {F(DRY)}", expect="GT6 dryer placed"),
    Step(f"gt6machine dryer check {F(DRY)}", expect="data=-1"),
    Step(f"gt6machine canner place {F(CAN)}", expect="GT6 canner placed"),
    Step(f"gt6machine canner check {F(CAN)}", expect="data=-1"),
]

# ------------------------- C: the row-less arm — the mui-a-open-chain dispatch
steps += [
    phase("C: the row-less arm — the A3-deregistered shredder family: row == null has "
          "no menu column at all, the mui-a-open-chain MUI dispatch unchanged "
          "(data=-2, the MenuType registration is gone since the dereg)"),
    Step(f"gt6machine shredder place {F(SHR)}", expect="GT6 shredder placed"),
    Step(f"gt6machine shredder check {F(SHR)}", expect="data=-2"),
]

CHAIN = Chain(
    name="mui-row-dispatch",
    slug="muirowdispatch",
    sites=gt6world.declare_sites(DIS, DRY, CAN, SHR),
    preferred_ports=(26112, 26122),      # this card's pinned rcon/query pair
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
