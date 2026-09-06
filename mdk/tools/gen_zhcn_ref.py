#!/usr/bin/env python3
"""Generate the zh_cn reference TSV for the GT6ZhCn datagen provider (task p20-i18n-zhcn-provider).

Reads the GT6 1.7.10 zh lang dump (tmp/gregtech.lang, the LanguageHandler dump — the sole
residual value of the cut GT_LangManager port, research card tasks.p20-research-i18n-zh) and
writes the translation DATA SOURCE the provider reads from the classpath at datagen time:
mdk/src/main/resources/gregtech6/lang/zh_cn_ref.tsv (columns kind/source/value/status, ADR
2026-09-06-p20-i18n-zhcn-pipeline §1.1).

Pipeline ruling implemented here (ADR §1.1/§1.2):
  - The dump lines look like "    S:<key>=<value>" (Forge Configuration render). Only three
    key families are collected: gt.material.* / itemGroup.* / gt.multitileentity.*; addon and
    out-of-face keys (ktfru.* = KekzTech addon, written.book.*, enchantment.*) are dropped.
  - ZERO key-name logic on the Python side: family rows carry the dump key suffix verbatim
    (material Pascal internal / itemGroup camel internal / mte numeric id); the target gt6
    lang keys are derived provider-side by the single MaterialPrefixItem.snakeCase
    implementation (or joined by walk source), so the snakeCase algorithm never lives in two
    languages. kind=direct rows are the exception — their source column IS the final gt6 key,
    hand-entered below (ADR §1.2: the hand-translation layer).
  - A family row whose value is pure ASCII is marked status=review (suspected untranslated,
    evidence sample "gt.material.Magnite=Magnite", tmp/gregtech.lang:5003); the provider skips
    review rows and the runtime falls back to English per key (vanilla LanguageManager.java:
    50-52 bilingual chain, zero port code).
  - The mte family (gt.multitileentity.<ID>, ~4.4k rows) is collected as inert census data:
    the provider has no ID->port-key join yet (that is the follow-up MTE subface, arch card
    tasks.p20-arch-i18n — needs the upstream Loader_MultiTileEntities ID table as evidence).
  - Hand rows (HAND_TRANSLATIONS below): the ~103 in-use prefix display templates (one per
    creative-visible prefix tab, the arch card's "手译在用 105 条" ruling — templates only,
    never machine-translated values; each row was authored against the dump's oredict
    composed-name evidence, e.g. oredict.ingotAbyssalnite=深渊锭 -> "%s锭") plus the handful
    of special creative tabs / atomic misc keys. kind=direct, status=hand.

Idempotent: same dump + same hand table -> byte-identical TSV (rows sorted by (kind, source),
first dump occurrence wins on duplicate keys, LF line endings, trailing newline).

Usage:
  python3 gen_zhcn_ref.py --dump /path/to/gregtech.lang \
      [--out ../mdk/src/main/resources/gregtech6/lang/zh_cn_ref.tsv]
The dump path is a required argument (the coder worktree does not carry tmp/, ADR §1.1 CI
principle — the TSV is the committed, reviewable distillate of the dump).
"""
import argparse
import re
import sys
from pathlib import Path

DUMP_LINE = re.compile(r"^    S:([^=]+)=(.*)$")

# Out-of-face keys: the KekzTech addon family and non-item lang keys (ADR §1.1 filter).
DROP_KEY_PREFIXES = ("ktfru.", "written.book.", "enchantment.")

# kind -> dump key prefix (the three collected families; everything else is ignored).
FAMILIES = (
    ("gt.material.", "material"),
    ("itemGroup.", "itemgroup"),
    ("gt.multitileentity.", "mte"),
)

REVIEW_NOTE = "value is pure ASCII — suspected untranslated dump entry (e.g. Magnite=Magnite, tmp/gregtech.lang:5003)"

# ---------------------------------------------------------------------------
# Hand-translation layer (kind=direct rows). Key = the FINAL gt6 lang key, value =
# (zh value, status). These are human-authored rows: the ~103 in-use prefix display
# templates (one per creative-visible prefix tab of the port — GTMaterialItems.tabPrefixes()
# + GTMaterialBlocks.tabPrefixes() face, the arch card's "手译在用 105 条" ruling) and the
# special creative tabs + atomic misc keys. Every template was authored against the dump's
# oredict composed-name evidence (oredict.<prefix><Material>=<zh组合>, e.g.
# oredict.ingotAbyssalnite=深渊锭 -> "%s锭"; gem mirrors the bare en template "%s").
# Values contain exactly one "%s" slot where the runtime fills the material name
# (MaterialPrefixItem.getName template contract). NEVER machine-translate here: a bad row is
# skipped by fixing its status to "review", not by generating a guess.
# ---------------------------------------------------------------------------
HAND_TRANSLATIONS = {
    # ---- in-use prefix display templates (gt6.tagprefix.<snake>, status=hand) ----
    "gt6.tagprefix.ingot": ("%s锭", "hand"),
    "gt6.tagprefix.ingot_double": ("双重%s锭", "hand"),
    "gt6.tagprefix.ingot_triple": ("三重%s锭", "hand"),
    "gt6.tagprefix.ingot_quadruple": ("四重%s锭", "hand"),
    "gt6.tagprefix.ingot_quintuple": ("五重%s锭", "hand"),
    "gt6.tagprefix.dust": ("%s粉", "hand"),
    "gt6.tagprefix.dust_small": ("小堆%s粉", "hand"),
    "gt6.tagprefix.dust_tiny": ("小撮%s粉", "hand"),
    "gt6.tagprefix.dust_div72": ("1/72%s粉", "hand"),
    "gt6.tagprefix.plate": ("%s板", "hand"),
    "gt6.tagprefix.plate_tiny": ("小块%s板", "hand"),
    "gt6.tagprefix.plate_double": ("双重%s板", "hand"),
    "gt6.tagprefix.plate_triple": ("三重%s板", "hand"),
    "gt6.tagprefix.plate_quadruple": ("四重%s板", "hand"),
    "gt6.tagprefix.plate_quintuple": ("五重%s板", "hand"),
    "gt6.tagprefix.plate_dense": ("致密%s板", "hand"),
    "gt6.tagprefix.plate_curved": ("弯曲%s板", "hand"),
    "gt6.tagprefix.plate_gem": ("结晶%s板", "hand"),
    "gt6.tagprefix.plate_gem_tiny": ("小块结晶%s板", "hand"),
    "gt6.tagprefix.gem": ("%s", "hand"),
    "gt6.tagprefix.gem_chipped": ("碎裂%s", "hand"),
    "gt6.tagprefix.gem_flawed": ("瑕疵%s", "hand"),
    "gt6.tagprefix.gem_exquisite": ("精美%s", "hand"),
    "gt6.tagprefix.gem_flawless": ("完美%s", "hand"),
    "gt6.tagprefix.gem_legendary": ("传奇%s", "hand"),
    "gt6.tagprefix.nugget": ("%s粒", "hand"),
    "gt6.tagprefix.block_raw": ("粗%s矿石块", "hand"),
    "gt6.tagprefix.block_gem": ("结晶%s块", "hand"),
    "gt6.tagprefix.block_dust": ("%s粉块", "hand"),
    "gt6.tagprefix.block_ingot": ("%s锭块", "hand"),
    "gt6.tagprefix.block_plate": ("%s板块", "hand"),
    "gt6.tagprefix.block_plate_gem": ("%s结晶板块", "hand"),
    "gt6.tagprefix.block_solid": ("铸造%s块", "hand"),
    "gt6.tagprefix.ore_raw": ("粗%s矿石", "hand"),
    "gt6.tagprefix.rock_gt": ("含%s石块", "hand"),
    "gt6.tagprefix.chunk_gt": ("%s碎块", "hand"),
    "gt6.tagprefix.crushed": ("破碎%s矿", "hand"),
    "gt6.tagprefix.crushed_tiny": ("小撮破碎%s矿", "hand"),
    "gt6.tagprefix.crushed_purified": ("洗净%s矿", "hand"),
    "gt6.tagprefix.crushed_purified_tiny": ("小撮洗净%s矿", "hand"),
    "gt6.tagprefix.crushed_centrifuged": ("离心%s矿", "hand"),
    "gt6.tagprefix.crushed_centrifuged_tiny": ("小撮离心%s矿", "hand"),
    "gt6.tagprefix.billet": ("%s坯料", "hand"),
    "gt6.tagprefix.bolt": ("%s螺栓", "hand"),
    "gt6.tagprefix.round": ("%s垫片", "hand"),
    "gt6.tagprefix.screw": ("%s螺丝", "hand"),
    "gt6.tagprefix.ring": ("%s环", "hand"),
    "gt6.tagprefix.spring": ("%s弹簧", "hand"),
    "gt6.tagprefix.spring_small": ("小%s弹簧", "hand"),
    "gt6.tagprefix.stick": ("%s杆", "hand"),
    "gt6.tagprefix.stick_long": ("长%s杆", "hand"),
    "gt6.tagprefix.gear_gt": ("%s齿轮", "hand"),
    "gt6.tagprefix.gear_gt_small": ("小%s齿轮", "hand"),
    "gt6.tagprefix.rotor": ("%s转子", "hand"),
    "gt6.tagprefix.foil": ("%s箔", "hand"),
    "gt6.tagprefix.lens": ("%s透镜", "hand"),
    "gt6.tagprefix.chain": ("%s锁链", "hand"),
    "gt6.tagprefix.rail_gt": ("%s轨道", "hand"),
    "gt6.tagprefix.minecart_wheels": ("%s车轮", "hand"),
    "gt6.tagprefix.wire_fine": ("精细%s线缆", "hand"),
    "gt6.tagprefix.casing_small": ("小%s外壳", "hand"),
    "gt6.tagprefix.chemtube": ("含%s试管", "hand"),
    "gt6.tagprefix.arrow_gt_wood": ("%s箭", "hand"),
    "gt6.tagprefix.arrow_gt_plastic": ("轻质%s箭", "hand"),
    "gt6.tagprefix.bullet_gt_small": ("小号%s子弹", "hand"),
    "gt6.tagprefix.bullet_gt_large": ("大号%s子弹", "hand"),
    "gt6.tagprefix.tool_head_sword": ("%s剑刃", "hand"),
    "gt6.tagprefix.tool_head_pickaxe": ("%s镐头", "hand"),
    "gt6.tagprefix.tool_head_shovel": ("%s锹头", "hand"),
    "gt6.tagprefix.tool_head_spade": ("%s铲头", "hand"),
    "gt6.tagprefix.tool_head_axe": ("%s斧头", "hand"),
    "gt6.tagprefix.tool_head_axe_double": ("%s双刃斧头", "hand"),
    "gt6.tagprefix.tool_head_hoe": ("%s锄头", "hand"),
    "gt6.tagprefix.tool_head_sense": ("%s镰刀刃", "hand"),
    "gt6.tagprefix.tool_head_plow": ("%s犁头", "hand"),
    "gt6.tagprefix.tool_head_saw": ("%s锯片", "hand"),
    "gt6.tagprefix.tool_head_file": ("%s锉刀头", "hand"),
    "gt6.tagprefix.tool_head_hammer": ("%s锤头", "hand"),
    "gt6.tagprefix.tool_head_drill": ("%s钻头", "hand"),
    "gt6.tagprefix.tool_head_chainsaw": ("%s链锯头", "hand"),
    "gt6.tagprefix.tool_head_buzz_saw": ("%s圆锯片", "hand"),
    "gt6.tagprefix.tool_head_wrench": ("%s扳手头", "hand"),
    "gt6.tagprefix.tool_head_screwdriver": ("%s螺丝刀头", "hand"),
    "gt6.tagprefix.tool_head_chisel": ("%s凿子头", "hand"),
    "gt6.tagprefix.tool_head_universal_spade": ("%s万用铲头", "hand"),
    "gt6.tagprefix.tool_head_builderwand": ("%s建筑之杖头", "hand"),
    "gt6.tagprefix.tool_head_construction_pickaxe": ("%s建筑镐头", "hand"),
    "gt6.tagprefix.tool_head_pickaxe_gem": ("%s尖镐头", "hand"),
    "gt6.tagprefix.tool_head_arrow": ("%s箭头", "hand"),
    "gt6.tagprefix.tool_head_raw_sword": ("%s剑刃毛坯", "hand"),
    "gt6.tagprefix.tool_head_raw_pickaxe": ("%s镐头毛坯", "hand"),
    "gt6.tagprefix.tool_head_raw_shovel": ("%s锹头毛坯", "hand"),
    "gt6.tagprefix.tool_head_raw_spade": ("%s铲头毛坯", "hand"),
    "gt6.tagprefix.tool_head_raw_axe": ("%s斧头毛坯", "hand"),
    "gt6.tagprefix.tool_head_raw_axe_double": ("%s双刃斧毛坯", "hand"),
    "gt6.tagprefix.tool_head_raw_hoe": ("%s锄头毛坯", "hand"),
    "gt6.tagprefix.tool_head_raw_sense": ("%s镰刀刃毛坯", "hand"),
    "gt6.tagprefix.tool_head_raw_plow": ("%s犁头毛坯", "hand"),
    "gt6.tagprefix.tool_head_raw_saw": ("%s锯片毛坯", "hand"),
    "gt6.tagprefix.tool_head_raw_arrow": ("%s箭头毛坯", "hand"),
    "gt6.tagprefix.tool_head_raw_chisel": ("%s凿子头毛坯", "hand"),
    "gt6.tagprefix.tool_head_raw_universal_spade": ("%s万用铲头毛坯", "hand"),
    # ---- special creative tabs (upstream MTE-registry categories + port tabs, no dump face) ----
    "itemGroup.gt6.chests": ("箱子", "hand"),
    "itemGroup.gt6.machines": ("机器", "hand"),
    "itemGroup.gt6.multiblocks": ("多方块", "hand"),
    "itemGroup.gt6.fluid_containers": ("流体容器", "hand"),
    "itemGroup.gt6.fluid_pipes": ("流体管道", "hand"),
    "itemGroup.gt6.electric_wires": ("电线", "hand"),
    "itemGroup.gt6.redstone_wires": ("红石线", "hand"),
    "itemGroup.gt6.laser_wires": ("激光导线", "hand"),
    "itemGroup.gt6.tools": ("工具", "hand"),
    # ---- atomic misc keys (tools / covers / circuits / JEI info / example / fluids) ----
    "item.gt6.crowbar": ("撬棍", "hand"),
    "item.gt6.cutter": ("线缆剪", "hand"),
    "item.gt6.chisel": ("凿子", "hand"),
    "item.gt6.cover_redstone_emitter": ("红石发射器", "hand"),
    "item.gt6.cover_redstone_conductor_in": ("红石导线面板（接收）", "hand"),
    "item.gt6.cover_redstone_conductor_out": ("红石导线面板（发出）", "hand"),
    "item.gt6.cover_redstone_machine_switch": ("红石机器开关", "hand"),
    "item.gt6.cover_shutter": ("挡板面板", "hand"),
    "item.gt6.cover_item_filter": ("物品过滤器", "hand"),
    "item.gt6.cover_auto_redstone_machine_switch": ("自动红石机器开关", "hand"),
    "item.gt6.cover_controller": ("面板控制器", "hand"),
    "item.gt6.integrated_circuit": ("选择器标签", "hand"),
    "item.gt6.integrated_circuit.configuration": ("配置：%s", "hand"),
    # ---- B1 wire-domain composed-display units (task p20-i18n-compose-wires) ----
    # The wire family composes at runtime over gt6.wire.display[.plain] + the form units;
    # the material slot rides the gt6.material.* small units (zero new material debt).
    # Wording evidence:
    #   wire/cable = GTCEu Modern zh_cn.json (tmp/refs/gtceu-modern assets/gtceu/lang,
    #   the community-standard SPLIT: tagprefix.wire_gt_single="1x%s导线" /
    #   cable_gt_single="1x%s线缆") — keeps the two forms distinguishable, unlike the
    #   1.7.10 dump's uniform 线缆 (gt.multitileentity.27000/27006 both end in 线缆);
    #   × = the CJK multiplication sign replacing the en "x" (免空格, CJK has no
    #   inter-word spaces, the size slot glues directly onto the material);
    #   wirelamp = 灯导线 (the Loader:1900 Lumium lamp-wire — no dump face; the 导线
    #   head stays consistent with the wire unit);
    #   conveyor/robot arm = dump-verbatim nouns (gt.multiitem.technological.12040=
    #   "输送机模块 (ULV)" / :12080="机械臂 (ULV)", tmp/gregtech.lang);
    #   the laser atomic key = dump-verbatim "光纤" (gt.multitileentity.24900).
    #   NOTE (review R2): gt6.material.superconductor is DELIBERATELY absent here —
    #   Superconductor is a tier material (mID -1, MT.java:986) with no dump zh face and
    #   an en proper noun identical in both locales; the composed slot falls back to the
    #   English "Superconductor" per-key at runtime (the declared state). A hand row is
    #   a one-line TSV PR for the translation layer, NOT a datagen concern.
    "gt6.wire.display": ("%s×%s%s", "hand"),
    "gt6.wire.display.plain": ("%s%s", "hand"),
    "gt6.wire.form.wire": ("导线", "hand"),
    "gt6.wire.form.cable": ("线缆", "hand"),
    "gt6.wire.form.wirelamp": ("灯导线", "hand"),
    "gt6.cover.conveyor.display": ("输送机模块 (%s)", "hand"),
    "gt6.cover.robot_arm.display": ("机械臂 (%s)", "hand"),
    "block.gt6.wire_laser": ("光纤", "hand"),
    "block.gt6.example_chest": ("GT 示例箱子", "hand"),
    "fluid.gt6.iron_molten": ("熔融铁", "hand"),
    "fluid.gt6.natural_gas": ("天然气", "hand"),
    "gt6.jei.info.multiblock_coke_oven": (
        "焦炉是一个 3x3x3 的立方体：将焦炉控制器放在其中一面的中央并朝外，"
        "保持立方体中心格为空，其余 25 格全部放满焦炉砖。点燃控制器即可启动"
        "——它自行积攒热量，结构下方一层的储罐会收集杂酚油。",
        "hand",
    ),
}


def dump_rows(dump_path: Path):
    """Collect the three families from the dump. First occurrence wins; returns {kind: {source: value}}."""
    collected: dict[str, dict[str, str]] = {kind: {} for _, kind in FAMILIES}
    dropped = 0
    duplicates = 0
    with dump_path.open("r", encoding="utf-8") as handle:
        for line_no, raw in enumerate(handle, 1):
            match = DUMP_LINE.match(raw.rstrip("\n"))
            if not match:
                continue
            key, value = match.group(1), match.group(2)
            if key.startswith(DROP_KEY_PREFIXES):
                dropped += 1
                continue
            for prefix, kind in FAMILIES:
                if key.startswith(prefix):
                    source = key[len(prefix):]
                    table = collected[kind]
                    if source in table:
                        duplicates += 1
                    else:
                        table[source] = value.strip()
                    break
            if line_no == 0:  # pragma: no cover — keeps the loop shape honest
                break
    return collected, dropped, duplicates


def ascii_review(value: str) -> bool:
    """Pure-ASCII values are suspected untranslated dump entries -> status=review."""
    return value.isascii()


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--dump", required=True, type=Path,
                        help="path to the GT6 1.7.10 zh lang dump (tmp/gregtech.lang)")
    parser.add_argument("--out", type=Path,
                        default=Path(__file__).resolve().parent.parent / "src/main/resources/gregtech6/lang/zh_cn_ref.tsv",
                        help="output TSV path (default: the shared resources tree)")
    args = parser.parse_args(argv)

    if not args.dump.is_file():
        parser.error(f"dump not found: {args.dump}")

    collected, dropped, duplicates = dump_rows(args.dump)

    # Validate the hand layer up front: one %s slot for template keys, no tabs/newlines anywhere.
    for key, (value, status) in HAND_TRANSLATIONS.items():
        if value is None:
            continue  # review rows may be empty — the provider skips them
        if "\t" in value or "\n" in value:
            sys.exit(f"hand row {key}: value contains tab/newline")
        if key.startswith("gt6.tagprefix.") and status == "hand" and value.count("%s") != 1:
            sys.exit(f"hand row {key}: tagprefix template must contain exactly one %s slot, got {value!r}")

    lines: list[str] = [
        "# zh_cn reference table — GENERATED by mdk/tools/gen_zhcn_ref.py, git-tracked data source",
        "# for the GT6ZhCn datagen provider (ADR 2026-09-06-p20-i18n-zhcn-pipeline §1.1).",
        "#",
        "# columns: kind<TAB>source<TAB>value<TAB>status",
        "#   kind   material | itemgroup | mte | direct",
        "#   source family rows: the dump key suffix verbatim (material Pascal internal / itemGroup",
        "#          camel internal / mte numeric id) — the gt6 lang key is derived provider-side by",
        "#          the single MaterialPrefixItem.snakeCase implementation, never here;",
        "#          direct rows: the FINAL gt6 lang key (hand-entered in the script's table).",
        "#   status auto   = dump copy with a translatable value;",
        "#          review = pure-ASCII dump value, suspected untranslated — provider skips;",
        "#          hand   = human translation (script HAND_TRANSLATIONS table).",
        f"# filters: dropped {'/'.join(p.rstrip('.') for p in DROP_KEY_PREFIXES)} keys; duplicate keys: first occurrence wins.",
        "#   mte rows are inert census data until the MTE ID-join subface lands (arch tasks.p20-arch-i18n).",
        "#",
    ]

    counts: dict[str, int] = {}
    for kind in ("direct", "material", "itemgroup", "mte"):
        rows: list[tuple[str, str, str]] = []
        if kind == "direct":
            for key, (value, status) in sorted(HAND_TRANSLATIONS.items()):
                if value is None:
                    continue
                rows.append((key, value, status))
        else:
            for source, value in sorted(collected[kind].items()):
                rows.append((source, value, "review" if ascii_review(value) else "auto"))
        counts[kind] = len(rows)
        for source, value, status in rows:
            lines.append(f"{kind}\t{source}\t{value}\t{status}")

    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_text("\n".join(lines) + "\n", encoding="utf-8", newline="\n")

    review_family = sum(1 for kind in ("material", "itemgroup", "mte") for v in collected[kind].values() if ascii_review(v))
    print(f"wrote {args.out}")
    print(f"  direct={counts['direct']} (hand) material={counts['material']} itemgroup={counts['itemgroup']} mte={counts['mte']}")
    print(f"  family review (skipped by provider): {review_family} {REVIEW_NOTE}")
    print(f"  dump housekeeping: dropped={dropped} duplicate-keys={duplicates}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
