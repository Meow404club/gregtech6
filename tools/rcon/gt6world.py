#!/usr/bin/env python3
"""gt6world — site registry and automatic cleanup for the GT6 acceptance chains (layer 2).

The missed-site class of false reds (a hand-written fill box that forgot one
site, so the second chain round ran on a polluted world — the 2026-09-02
shutter-chain instance) is eliminated structurally: the chain declares its
sites once, and :func:`cleanup_commands` derives the fill box from the union
bounding box plus MARGIN. A site that is not declared can no longer be
silently spared by the cleanup — the test simply fails on its own leftovers.

Usage:
    sites = gt6world.declare_sites(Site(0, 64, 0, dy=1), "10 64 10", ...)
    region = gt6world.region(sites)
    for cmd in gt6world.forceload_commands(region): ...
    for cmd in gt6world.cleanup_commands(region): ...   # start of every pass

The setup helpers are the imperative conveniences distilled from the real
chains (oven/hopper/container/placement); the declarative chain layer spells
its steps as commands and uses the same cmd grammar.
"""

from dataclasses import dataclass

MARGIN = 2          # clearance blocks added on every side of the union bbox
MAX_FILL_VOLUME = 32768   # vanilla /fill block limit
MAX_FORCELOAD_CHUNKS = 256  # vanilla /forceload add chunk limit


@dataclass(frozen=True)
class Site:
    """One declared test site: a block position with optional reach per axis.

    dx/dy/dz extend the site's footprint around the point (an oven with a
    hopper above or below is Site(x, 64, z, dy=1)); the cleanup union covers
    every declared footprint plus MARGIN.
    """
    x: int
    y: int
    z: int
    dx: int = 0
    dy: int = 0
    dz: int = 0

    def bounds(self):
        return (self.x - self.dx, self.y - self.dy, self.z - self.dz,
                self.x + self.dx, self.y + self.dy, self.z + self.dz)


def declare_sites(*specs):
    """Normalize the mixed-spec shorthand into a tuple of Sites.

    Accepts Site instances, (x, y, z) tuples/lists, and "x y z" strings.
    """
    sites = []
    for spec in specs:
        if isinstance(spec, Site):
            sites.append(spec)
        elif isinstance(spec, str):
            x, y, z = (int(part) for part in spec.split())
            sites.append(Site(x, y, z))
        elif isinstance(spec, (tuple, list)):
            x, y, z = spec
            sites.append(Site(int(x), int(y), int(z)))
        else:
            raise TypeError(f"cannot read site spec {spec!r}")
    return tuple(sites)


def fmt(pos):
    """'x y z' from a Site / triple / already-formatted string."""
    if isinstance(pos, Site):
        return f"{pos.x} {pos.y} {pos.z}"
    if isinstance(pos, str):
        return pos
    return f"{pos[0]} {pos[1]} {pos[2]}"


def region(sites, margin=MARGIN):
    """Union bbox of all site footprints expanded by margin on every side.

    Empty input raises — an unanchored cleanup box would be exactly the
    silent-pollution bug this module exists to kill.
    """
    sites = tuple(sites)
    if not sites:
        raise ValueError("no sites declared — cleanup would be an arbitrary box")
    boxes = [site.bounds() for site in sites]
    x1 = min(b[0] for b in boxes) - margin
    y1 = min(b[1] for b in boxes) - margin
    z1 = min(b[2] for b in boxes) - margin
    x2 = max(b[3] for b in boxes) + margin
    y2 = max(b[4] for b in boxes) + margin
    z2 = max(b[5] for b in boxes) + margin
    return (x1, y1, z1, x2, y2, z2)


def _split_box(box, max_volume):
    """Halve the box along its longest axis until every piece fits the limit."""
    x1, y1, z1, x2, y2, z2 = box
    if (x2 - x1 + 1) * (y2 - y1 + 1) * (z2 - z1 + 1) <= max_volume:
        return [box]
    spans = (x2 - x1, y2 - y1, z2 - z1)
    axis = spans.index(max(spans))
    if axis == 0:
        mid = (x1 + x2) // 2
        return (_split_box((x1, y1, z1, mid, y2, z2), max_volume)
                + _split_box((mid + 1, y1, z1, x2, y2, z2), max_volume))
    if axis == 1:
        mid = (y1 + y2) // 2
        return (_split_box((x1, y1, z1, x2, mid, z2), max_volume)
                + _split_box((x1, mid + 1, z1, x2, y2, z2), max_volume))
    mid = (z1 + z2) // 2
    return (_split_box((x1, y1, z1, x2, y2, mid), max_volume)
            + _split_box((x1, y1, mid + 1, x2, y2, z2), max_volume))


def cleanup_commands(region_, block="air", max_volume=MAX_FILL_VOLUME):
    """The pass-opening fill commands: the whole declared region, split only if
    the box exceeds the vanilla /fill limit."""
    return [f"fill {a} {b} {c} {d} {e} {f} {block}"
            for (a, b, c, d, e, f) in _split_box(tuple(region_), max_volume)]


def forceload_commands(region_):
    """`forceload add` covering the region's chunks, split at the 256-chunk limit."""
    x1, _y1, z1, x2, _y2, z2 = region_
    cx1, cx2 = x1 >> 4, x2 >> 4
    cz1, cz2 = z1 >> 4, z2 >> 4

    def emit(a, b, c, d):
        return [f"forceload add {a * 16} {b * 16} {c * 16 + 15} {d * 16 + 15}"]

    def split(a, b, c, d):
        if (c - a + 1) * (d - b + 1) <= MAX_FORCELOAD_CHUNKS:
            return emit(a, b, c, d)
        if c - a >= d - b:
            mid = (a + c) // 2
            return split(a, b, mid, d) + split(mid + 1, b, c, d)
        mid = (b + d) // 2
        return split(a, b, c, mid) + split(a, mid + 1, c, d)

    return split(cx1, cz1, cx2, cz2)


# ---------------------------------------------------------------------------
# setup helpers (imperative, distilled from the real chains) — each runs one
# command through an RconClient-like object and returns the response body.
# ---------------------------------------------------------------------------

def _run(client, command):
    outs = client.run_command(command)
    return "\n".join(outs) if isinstance(outs, list) else str(outs)


def set_block(client, pos, block):
    return _run(client, f"setblock {fmt(pos)} {block}")


def set_block_command(pos, block):
    """The declarative form: just the command string for a chain Step."""
    return f"setblock {fmt(pos)} {block}"


def place_oven(client, pos):
    """/gt6oven place — the universal BE host of the cover/oven chains."""
    return _run(client, f"gt6oven place {fmt(pos)}")


def place_hopper(client, pos, facing="down"):
    """A vanilla hopper: the push/suck gate driver above or below a machine."""
    return _run(client, hopper_command(pos, facing))


def hopper_command(pos, facing="down"):
    """The declarative hopper placement (the push/suck gate driver)."""
    return f"setblock {fmt(pos)} hopper[facing={facing}]"


def feed_container(client, pos, slot, item, count):
    """`item replace block ... container.N with minecraft:<item> <count>`."""
    return _run(client, feed_container_command(pos, slot, item, count))


def feed_container_command(pos, slot, item, count):
    """The declarative container fill (the hopper/machine input setter)."""
    return (f"item replace block {fmt(pos)} container.{slot} "
            f"with minecraft:{item} {count}")


def redstone_block(client, pos, on=True):
    """The block-update seam: a redstone block set or cleared."""
    return _run(client, f"setblock {fmt(pos)} {'redstone_block' if on else 'air'}")


def store_null_command(pos):
    """The teardown probe: /gt6cover check expecting the store dissolved to null."""
    return f"gt6cover check {fmt(pos)}"


STORE_NULL_EXPECT = "store=null"


def command_blocks_used(region_):
    """Total blocks the cleanup fill would touch (for pre-flight sanity prints)."""
    x1, y1, z1, x2, y2, z2 = region_
    return (x2 - x1 + 1) * (y2 - y1 + 1) * (z2 - z1 + 1)


