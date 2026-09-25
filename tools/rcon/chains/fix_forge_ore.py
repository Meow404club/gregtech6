#!/usr/bin/env python3
"""fix_forge_ore — the forge-leg ore-invisibility root-fix live chain (task
fix-forge-ore-invisible).

Byte-for-byte REUSE of the ore_overlay floating-stage fixture (task
ore-overlay-impl): the fix is client-render-only (GTOreBakedModel mQuads lazy
first-render bake), so the staging face is identical — the same z=508..530 band, the
same ore wall + four representative cells + camera pin. A separate Chain identity
(same steps/sites, new slug) exists purely so the card's gate runs its own session
group: --group fix_forge_ore. The fixture is idempotent (clear-then-place setblocks,
passes=1) and the band is the registered ore_overlay bbox — re-staging over an
existing fixture is the no-change safe shape.

The RENDER ASSERTION stays the dev-client screenshot leg (the fix is client-only
code): after the chain, per node,
  python3 tools/rcon/chains/ore_overlay.py --world-copy --node 1.20.1-forge
  python3 tools/rcon/chains/ore_overlay.py --client   --node 1.20.1-forge
(the client arm rides the base module — the world name and shot file stay
shot_ore_overlay.png in the node run dir; the 1.20.1-forge frame is THE fix
proof, the 1.21.1-neoforge frame the render zero-regression proof).
"""

import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
for _path in (str(_HERE), str(_HERE.parent)):
    if _path not in sys.path:
        sys.path.insert(0, _path)

import ore_overlay as base
from framework import Chain

# The same stage/matrix/camera — the fixture IS the acceptance face (idempotent).
CHAIN = Chain(
    name="fix-forge-ore",
    slug="fix_forge_ore",
    steps=base.steps,
    sites=base.CHAIN.sites,
    preferred_ports=base.CHAIN.preferred_ports,
    passes=1,
)

if __name__ == "__main__":
    from framework import main
    main(CHAIN)
