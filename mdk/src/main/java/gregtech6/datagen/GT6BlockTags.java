package gregtech6.datagen;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import gregtech6.worldgen.GTOreWorldgen;

//? if forge {
import net.minecraftforge.common.data.BlockTagsProvider;
//?} else {
/*import net.neoforged.neoforge.common.data.BlockTagsProvider;
*///?}

import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.RegistryObject;

import gregapi.data.OP;
import gregapi.oredict.OreDictPrefix;
import gregtech6.block.tank.GTBarrelBlock;
import gregtech6.items.tools.GTWrenchItem;
import gregtech6.registry.GT6Batteries;
import gregtech6.registry.GT6Hoppers;
import gregtech6.registry.GT6LongDistanceTransformers;
import gregtech6.registry.GT6LongDistPipes;
import gregtech6.registry.GT6LongDistWires;
import gregtech6.registry.GT6MagicAbsorbers;
import gregtech6.registry.GT6QuantumEnergizers;
import gregtech6.registry.GT6Reactors;
import gregtech6.registry.GT6StaticStorages;
import gregtech6.registry.GT6Tanks;
import gregtech6.registry.GT6Turbines;
import gregtech6.registry.GT6ZpmDechargers;
import gregtech6.registry.GTBarrels;
import gregtech6.registry.GTBlockEntities;
import gregtech6.registry.GTEnergySources;
import gregtech6.registry.GTGrassBlocks;
import gregtech6.registry.GTFluidPipes;
import gregtech6.registry.GTItemPipes;
import gregtech6.registry.GTMaterialBlocks;
import gregtech6.registry.GTMachines;
import gregtech6.registry.GT6TreeBlocks;
import gregtech6.registry.GT6BeamBlocks;
import gregtech6.registry.GT6SurfaceBlocks;
import gregtech6.registry.GTStoneBlocks;
import gregtech6.registry.GTWires;

/**
 * The GT6 block-tag datagen home — task tags-provider-skeleton, the first
 * {@code TagsProvider} over the BLOCK registry. The base class is the platform
 * {@code BlockTagsProvider}: its constructor is the SAME four-argument shape on both
 * legs — {@code (PackOutput, CompletableFuture<HolderLookup.Provider>, String modId,
 * ExistingFileHelper)} — forge 1.20.1 BlockTagsProvider.java:16-22 and NeoForge 21.1
 * BlockTagsProvider.java:16-21 are line-for-line the same class body (the research
 * card's "Neo 3-param" claim is void, corrected per the task card). This file therefore
 * carries ZERO {@code //?} except the {@link BlockTagsProvider} import: the class is not
 * on the stonecutter swap table ({@code ExistingFileHelper} is — mdk/stonecutter.gradle.kts
 * string band), and the {@code addTags(HolderLookup.Provider)} hook is the vanilla
 * {@code TagsProvider.java:58} abstract on both legs.
 *
 * <p><b>First P0 batch</b> (the frozen list of the census, state
 * research.p24-r-tags-foundation): {@code minecraft:mineable/pickaxe} over the GT6
 * mining universe — the 272 (stone, variant) blocks ({@link GTStoneBlocks#blockArray()}),
 * the whole {@link GTMachines} block register (oven + shredder/crusher/lathe ladders +
 * dryer/distillery rows — all metal-sound machines, GTMachines:59/:119), and the material
 * prefix storage blocks of the metal/gem/raw-ore families (GTCEu pins exactly this on its
 * block prefix: TagPrefix.java:734 {@code .miningToolTag(BlockTags.MINEABLE_WITH_PICKAXE)});
 * and {@code minecraft:mineable/axe} over the wood fluid barrel ({@link GTBarrels#BARREL},
 * the GTCEu wood-drum-to-axe precedent, BlockTagLoader:69-71).
 *
 * <p><b>Rolling batch 1</b> (task tags-prefix-materials, the census matrix P1 rows):
 * {@code mineable/pickaxe} extends over the wire universe ({@link GTWires#BLOCKS}
 * whole-class enumeration — a future wire row auto-joins, the machine-walk discipline),
 * the fluid-pipe universe ({@link GTFluidPipes#BLOCKS} whole-class — the task card names
 * GTFluidPipeBlock into pickaxe explicitly: the functional-connector ruling; the two pipe
 * rows' WOOD sound is the vanilla stand-in, not a material ruling) and the barrel family
 * closure (plastic canister + bronze drum + logistics tank + twelve high-tier drums — the
 * census axe/pickaxe/pickaxe mapping over wood/plastic/metal). {@code mineable/shovel}
 * lands the {@code blockDust} prefix family (the sand-analog powdery storage blocks).
 * Still OUT by the standing card boundaries: {@code requires_correct_tool_for_drops} and
 * the {@code needs_*} gates (coupled to the chisel loot chain + harvestLevel wiring — the
 * tool-system card); the leaf/log families (the future leaf card).
 *
 * <p><b>Strictness is the acceptance asset</b>: vanilla TagsProvider throws
 * IllegalArgumentException for any reference that fails
 * {@code TagEntry.verifyIfPresent} (TagsProvider.java:85-94 — "Couldn't define tag %s as
 * it is missing following references"), and the bands use NO optional members EXCEPT the
 * sanctioned foreign-mod indirection (the twilight deadrock band, task
 * twilight-vanilla-ores-deadrock — its member is legitimately absent at datagen time and
 * {@code required:false} is the card's TF-absence semantics). Every other member below is
 * a live-registered block at datagen time (registration events precede GatherDataEvent —
 * the GT6LootTables live-block precedent), so a membership gap fails runData loudly
 * instead of shipping a silently dangling tag. The produced files carry
 * {@code "replace": false} implicitly (TagsProvider.java:96-97
 * {@code new TagFile(entries, false)}), i.e. the mod file JOINS the vanilla tag
 * datapack-wide — the GT6Atlases.java:21-25 merge-semantics precedent.
 */
public final class GT6BlockTags extends BlockTagsProvider {

	/** The metal/gem/raw-ore block prefixes of the pickaxe band — blockDust excluded (P1 shovel band). */
	private static final Set<OreDictPrefix> PICKAXE_BLOCK_PREFIXES = Set.of(OP.blockRaw, OP.blockGem,
			OP.blockIngot, OP.blockPlate, OP.blockPlateGem, OP.blockSolid);

	public GT6BlockTags(PackOutput aOutput, CompletableFuture<HolderLookup.Provider> aLookupProvider,
			ExistingFileHelper aExistingFileHelper) {
		super(aOutput, aLookupProvider, GT6DataGenerators.MOD_ID, aExistingFileHelper);
	}

	@Override
	protected void addTags(HolderLookup.Provider aProvider) {
		addPickaxeBand();
		addAxeBand();
		addGrassBand(); // task grass-block — the grass family band
		addShovelBand();
		addTreeBand(); // task w6-t1-trees-nine — the decisions.p25-leaves-logs-tags-deferred unlock
		addSurfacePlantBand(); // task w6-t2-surface-blocks — the fallen logs (logs/axe) + the soil pair (shovel)
		addBeamBand(); // task beam-blocks-register — the 8 wood beams join mineable/axe
		addPlankBand(); // task gt-tree-planks — the 9 plank cubes (planks/axe)
		addRailsBand(); // task rails-31-blocks — the 31 rails join #minecraft:rails
		addWrenchBand(); // task wrench-mining-face — the gt6:mineable/wrench face (ruling B)
		addTwilightDeadrockBand(); // task twilight-vanilla-ores-deadrock — the gt6:tf_deadrock host-indirection tag
		// the takeover seam: later cards tail-append their own add*Band() here
		// (tags-prefix-materials: rolling batches).
	}

	/**
	 * The twilight deadrock host tag (task twilight-vanilla-ores-deadrock): the ONE
	 * {@code gt6:tf_deadrock} block tag with the single {@code twilightforest:deadrock}
	 * entry — THE other-mod-block-id indirection face. The netherite worldgen row's
	 * configured feature targets this tag ({@code minecraft:tag_match}), never the
	 * foreign id directly (the configured-feature JSON red line), and the entry is
	 * {@code required:false} so TF-absent installs resolve the tag EMPTY (the feature
	 * targets nothing; the modifier is unmounted by its mod_loaded condition anyway) —
	 * no crash, no hang, the acceptance semantics.
	 *
	 * <p>THE ARCHAEOLOGY (why exactly this one block): the upstream row replaces
	 * {@code IL.TF_Deadrock} meta 2 (Loader_Worldgen.java:712), and GT6's own item binds
	 * (LoaderItemList.java:979-981) map meta 0 weathered / meta 1 cracked / meta 2 plain.
	 * Modern TF splits the meta into three blocks — TFBlocks.java:163-165
	 * {@code deadrock}/{@code cracked_deadrock}/{@code weathered_deadrock}, with the
	 * surface rules (TFSurfaceRules.java:66-71) putting weathered on the floor, cracked
	 * under it, and PLAIN DEADROCK the filler mass — so meta 2 is the 1:1 counterpart
	 * {@code twilightforest:deadrock}. NOT the surface variants: the upstream row never
	 * replaced them.
	 *
	 * <p>This band is the tags-provider doctrine's FIRST sanctioned
	 * {@code addOptional} user (the class javadoc's live-block strictness): the member is
	 * a foreign-mod block that is legitimately absent at datagen time — required:false is
	 * the card-mandated shape, not a looseness regression.
	 */
	private void addTwilightDeadrockBand() {
		tag(GTOreWorldgen.twilightDeadrockTag())
				.addOptional(ResourceLocation.fromNamespaceAndPath("twilightforest", "deadrock"));
	}

	/**
	 * The rails band (task rails-31-blocks): all 31 rail blocks join
	 * {@code #minecraft:rails} — LOAD-BEARING for function, not just mining: the minecart
	 * engine gates on the tag ({@code BaseRailBlock.isRail} and
	 * {@code AbstractMinecart.getMaxSpeedWithRail}'s {@code state.is(BlockTags.RAILS)}
	 * on both legs), so an untagged rail block would never carry a cart. The mining face
	 * rides the vanilla nesting — {@code mineable/pickaxe} already contains
	 * {@code #minecraft:rails} (vanilla 1.20.1 pickaxe.json:326) — so no separate pickaxe
	 * row belongs to this band.
	 */
	private void addRailsBand() {
		var tRails = tag(BlockTags.RAILS);
		tRails.add(gregtech6.registry.GT6Rails.ROAD_BLOCK.get());
		for (var tEntry : gregtech6.registry.GT6Rails.BLOCKS_BY_PATH.entrySet()) {
			tRails.add(tEntry.getValue().get());
		}
	}

	/**
	 * The surface-plants band (task w6-t2-surface-blocks): the four fallen-log woods
	 * join {@code #minecraft:logs} + mineable/axe (the t1 log row — NOTE the coke-oven
	 * recipe rebuild counts #minecraft:logs, 40 vanilla + 9 gt6 becomes +4 with this
	 * card, the coordinator-noted census drift), and the soil band (turf + black sand +
	 * the four colored clays, task worldgen-diggables-pits — BlockDiggable.java:137
	 * TOOL_shovel on every meta) joins mineable/shovel (the vanilla dirt/sand row). The
	 * glowtus and the bush stay tool-less (the instant flower row).
	 */
	private void addSurfacePlantBand() {
		var tLogs = tag(BlockTags.LOGS);
		var tAxe = tag(BlockTags.MINEABLE_WITH_AXE);
		for (RegistryObject<Block> tHandle : GT6SurfaceBlocks.FALLEN_LOGS) {
			tLogs.add(tHandle.get());
			tAxe.add(tHandle.get());
		}
		var tShovel = tag(BlockTags.MINEABLE_WITH_SHOVEL);
		tShovel.add(GT6SurfaceBlocks.TURF.get());
		tShovel.add(GT6SurfaceBlocks.BLACK_SAND.get());
		for (RegistryObject<Block> tHandle : GT6SurfaceBlocks.CLAY_BAND) {
			tShovel.add(tHandle.get());
		}
	}

	/**
	 * The mineable/pickaxe band, three census families in walk order (deterministic output:
	 * the appender order IS the JSON order, so the second runData run is written:0). The
	 * machines walk rides {@link GTMachines#BLOCKS} {@code getEntries()} — the whole-class
	 * enumeration means a future machine row cannot silently miss the band; the lambda body
	 * compiles against both legs' entry handles ({@code RegistryObject} /
	 * {@code DeferredHolder}) without a fork.
	 *
	 * <p>Task lightning-rod — the conscious +1: the Lightning Rod pillar block
	 * (upstream part id 18104, Loader :1179, "Multiblock Machines", pickaxe-mined) joins
	 * the band. The OTHER three new blocks stay OUT, the multiblock-family convention: the
	 * tungsten wall / coil are part-family blocks like the five Dense Walls and the coke
	 * oven bricks (all outside the band, the p4/p13 cards' pre-existing state), and the
	 * controller follows the boiler mains (machine mains registered in GTMultiBlocks sit
	 * outside the GTMachines whole-class walk). A family-wide multiblock tag sweep is pool.
	 */
	private void addPickaxeBand() {
		var tPickaxe = tag(BlockTags.MINEABLE_WITH_PICKAXE);
		for (Block tBlock : GTStoneBlocks.blockArray()) {
			tPickaxe.add(tBlock);
		}
		GTMachines.BLOCKS.getEntries().forEach(tHandle -> tPickaxe.add(tHandle.get()));
		for (var tEntry : GTMaterialBlocks.items().entrySet()) {
			if (!PICKAXE_BLOCK_PREFIXES.contains(tEntry.getKey().prefix())) {
				continue; // blockDust (and any future non-band prefix) consciously excluded
			}
			tPickaxe.add(((BlockItem) tEntry.getValue().get()).getBlock());
		}
		tPickaxe.add(gregtech6.registry.GTMultiBlocks.LIGHTNING_ROD_PART_BLOCKS_BY_PATH.get("lightning_rod").get()); // the p24 +1
		// Rolling batch 1 (task tags-prefix-materials, census matrix P1 rows): the wire
		// universe (GTWires.BLOCKS whole-class enumeration — the legacy 1x/2x pair + the 620
		// electric family + 6 redstone + 1 laser, all GTWireBlock metal rows; a future wire row
		// auto-joins the band). GTWires stays pickaxe as the DECLARED EXPEDIENT (task
		// harvest-bands-wrench-machines): upstream the whole aMetalWires family is TOOL_cutter
		// (Loader :1898-1950), but the port GTCutterItem has NO mining surface, so cutter
		// members land on pickaxe until the cutter-face ruling card. The Long Distance wires
		// join the same expedient (upstream BlockLongDistWire.java:56-57 cutter level 3).
		GTWires.BLOCKS.getEntries().forEach(tHandle -> tPickaxe.add(tHandle.get()));
		for (var tEntry : GT6LongDistWires.BLOCKS_BY_META.entrySet()) {
			tPickaxe.add(tEntry.getValue().get()); // the cutter expedient, see above — NOT the wrench face
		}
		// The fluid-pipe universe, MISLABEL CORRECTED (task harvest-bands-wrench-machines):
		// this band used to take the WHOLE GTFluidPipes register, but upstream splits the
		// family by material — wood aWooden = axe (:1846-1849), rubber aUtilWool = shears
		// (:1850), metals aMachine = wrench (:1851-1860). The pickaxe face keeps ONLY the
		// rubber subdomain (the shears defer: 1.20.1 has no mineable/shears tag, card-4
		// ruling pool); the wood rows move to the axe band and the metal rows to the wrench
		// band in their own walks below.
		for (var tEntry : GTFluidPipes.BLOCKS_BY_PATH.entrySet()) {
			if (GTFluidPipes.rowByPath(tEntry.getKey()).material().blockFamily() != GTFluidPipes.PipeBlockFamily.UTIL_WOOL) {
				continue; // wood → axe, metal → wrench (the :1846-1860 material split)
			}
			tPickaxe.add(tEntry.getValue().get());
		}
		// The barrel family still closes the batch on the census material mapping
		// (axe/pickaxe/pickaxe over wood/plastic/metal, the BlockTagLoader:69-71 precedent).
		tPickaxe.add(GTBarrels.BARREL_PLASTIC.get());
		tPickaxe.add(GTBarrels.BARREL_METAL.get());
		tPickaxe.add(GTBarrels.BARREL_LOGISTICS.get());
		for (RegistryObject<GTBarrelBlock> tDrum : GTBarrels.METAL_DRUM_BLOCKS.values()) {
			tPickaxe.add(tDrum.get());
		}
		// task c-anvil — the stone anvil pair joins the band: both rows are stone-carrier
		// tool blocks (aUtilStone, the Loader :2185-2186 column; the vanilla
		// mineable/pickaxe gate over hardness 1.0), the hopper/boiler family convention
		tPickaxe.add(gregtech6.registry.GT6Anvils.STONE_ANVIL.get());
		tPickaxe.add(gregtech6.registry.GT6Anvils.BLACKSTONE_ANVIL.get());
		// task concrete-blocks-register — the 64 concrete blocks join the band: upstream
		// getHarvestTool = TOOL_pickaxe for the WHOLE BlockMetaType family incl. slabs
		// (BlockMetaType.java:166), and the vanilla pickaxe tag enumerates slabs
		// explicitly (no #slabs shortcut, vanilla pickaxe.json:95-110), so both bands walk
		for (RegistryObject<Block> tHandle : gregtech6.registry.GT6ConcreteBlocks.FULL_BLOCKS) {
			tPickaxe.add(tHandle.get());
		}
		for (RegistryObject<Block> tHandle : gregtech6.registry.GT6ConcreteBlocks.SLAB_BLOCKS) {
			tPickaxe.add(tHandle.get());
		}
		// task material-mc-a-storage-chests — the 60 PLAIN CHEST rows join the band: the
		// upstream block family aMetal = TOOL_pickaxe (Loader_MultiTileEntities.java:132 vs
		// the :98 aMetal tool column; the brief's "金属=wrench" is corrected by the loader
		// line — the wrench belongs to the aMachine massstorage pair, the mc-B card). The
		// reinforced half is aWooden = axe and rides the addAxeBand tail below. Tail-append
		// form — the pending harvest-bands cards (research.harvest-tool-census 卡1/卡2)
		// union onto this seam.
		for (Block tBlock : gregtech6.registry.GT6Chests.BLOCKS_BY_PATH.values().stream()
				.map(RegistryObject::get).toList()) {
			if (!((gregtech6.registry.GT6Chests.GT6ChestBlock) tBlock).row().reinforced()) {
				tPickaxe.add(tBlock);
			}
		}
		// task material-mc-b-storage-mass-shelf — the 60 METAL BOTTLECRATE rows join the
		// band: the upstream row rides aUtilMetal = TOOL_pickaxe (Loader :144 vs the :107
		// aUtilMetal carrier column) — NOT the aMachine wrench face its metal bookshelf
		// sibling (:143) rides. Tail-append form, the mc-A chests seat above; the review-seat
		// band-split seam (the merged walk had put all 128 metal storage rows on wrench).
		for (var tRow : GT6StaticStorages.ROWS) {
			if (tRow.material() != null && tRow.kind() == gregtech6.registry.GT6StaticStorages.Kind.BOTTLECRATE) {
				tPickaxe.add(GT6StaticStorages.BLOCKS_BY_PATH.get(tRow.path()).get());
			}
		}
	}

	/**
	 * The mineable/axe band — the wood fluid barrel (first batch), plus the WOOD fluid-pipe
	 * subdomain (task harvest-bands-wrench-machines, the mislabel fix: the four wooden
	 * materials wood/wood_treated/iron_wood/plastic ride the upstream aWooden column,
	 * Loader :1846-1849 — they used to sit in the pickaxe band, the whole-register
	 * expedient this card retires).
	 */
	private void addAxeBand() {
		var tAxe = tag(BlockTags.MINEABLE_WITH_AXE);
		tAxe.add(GTBarrels.BARREL.get());
		for (var tEntry : GTFluidPipes.BLOCKS_BY_PATH.entrySet()) {
			if (GTFluidPipes.rowByPath(tEntry.getKey()).material().blockFamily() != GTFluidPipes.PipeBlockFamily.WOODEN) {
				continue; // rubber stays pickaxe (the shears defer), metal goes wrench
			}
			tAxe.add(tEntry.getValue().get());
		}
		// task material-mc-a-storage-chests — the 60 REINFORCED CHEST rows join the band:
		// upstream block family aWooden = TOOL_axe (Loader_MultiTileEntities.java:133 vs the
		// :102 aWooden tool column). Tail-append form — the pending harvest-bands cards
		// (research.harvest-tool-census 卡3) union onto this seam.
		for (Block tBlock : gregtech6.registry.GT6Chests.BLOCKS_BY_PATH.values().stream()
				.map(RegistryObject::get).toList()) {
			if (((gregtech6.registry.GT6Chests.GT6ChestBlock) tBlock).row().reinforced()) {
				tAxe.add(tBlock);
			}
		}
	}

	/**
	 * The grass-family band (task grass-block, the user tag-paradigm first case — every
	 * row lands in the TagsProvider, ZERO block-code workarounds): each of the 6 GT grass
	 * variants joins {@code minecraft:dirt} (the BushBlock.java:19 planting face, which the
	 * canSustainPlant default rides), {@code minecraft:mineable/shovel} (the upstream
	 * TOOL_shovel level 0, BlockGrass.java:109-110) and {@code minecraft:
	 * sniffer_diggable_block} (the vanilla grass_block membership, Sniffer.java:260).
	 *
	 * <p>Deliberate ABSENCES (the canCreatureSpawn = F equivalence face, decisions
	 * .p24-grass-behavior-trim ①/②): the six animal spawnable tags
	 * ({@code animals/wolves/foxes/rabbits/parrots/frogs_spawnable_on}) stay UNJOINED — the
	 * 1.20.1 animal spawn surface is tag-driven (Animal.java:109), absence = no spawning,
	 * the upstream BlockGrass.java:107 semantics; {@code valid_spawn} stays unjoined
	 * (decision ②: no worldgen on this card, the consumer is unreachable). The
	 * enderman/bamboo/big-dripleaf/azalea/sculk set arrives by TRANSMISSION through
	 * {@code #dirt} — endermen may pick up GT grass, the declared accepted externality.
	 */
	private void addGrassBand() {
		for (Block tBlock : grassLootBandBlocks()) {
			tag(BlockTags.DIRT).add(tBlock);
			tag(BlockTags.MINEABLE_WITH_SHOVEL).add(tBlock);
			tag(BlockTags.SNIFFER_DIGGABLE_BLOCK).add(tBlock);
		}
	}

	/** The 6 grass blocks in variant order (the registration walk, live handles — datagen runs after registration). */
	private java.util.List<Block> grassLootBandBlocks() {
		java.util.List<Block> rBlocks = new java.util.ArrayList<>();
		for (var tHandle : GTGrassBlocks.BLOCKS) rBlocks.add(tHandle.get());
		return rBlocks;
	}

	/**
	 * The mineable/shovel band, rolling batch 1 (task tags-prefix-materials, census
	 * matrix P1 row): the {@code blockDust} prefix family — the powdery storage blocks ride
	 * the shovel exactly like the vanilla SAND family the census pinned as the evidence
	 * (GTCEu pins the dust-storage twin to the same face through its {@code block} prefix's
	 * miningToolTag being the only pickaxe face — the dust block's 9-Dust composition is the
	 * sand-analog). Strictness unchanged: every member is a live-registered block item from
	 * the registration walk.
	 */
	private void addShovelBand() {
		var tShovel = tag(BlockTags.MINEABLE_WITH_SHOVEL);
		for (var tEntry : GTMaterialBlocks.items().entrySet()) {
			if (tEntry.getKey().prefix() != OP.blockDust) continue;
			tShovel.add(((BlockItem) tEntry.getValue().get()).getBlock());
		}
	}

	/**
	 * The tree-family band (task w6-t1-trees-nine — the decisions
	 * .p25-leaves-logs-tags-deferred UNLOCK, its condition fires): the 9 tree logs join
	 * {@code minecraft:logs} (the vanilla fire/flammability semantics) and
	 * {@code mineable/axe}, the 9 leaves join {@code minecraft:leaves} +
	 * {@code mineable/hoe} (the vanilla leaves hoe face, vanilla-1.20.1
	 * data/minecraft/tags/blocks/mineable/hoe.json), the 9 saplings join
	 * {@code minecraft:saplings}. Strictness unchanged: every member is a
	 * live-registered block (the registration walk), zero optional.
	 */
	private void addTreeBand() {
		var tLogs = tag(BlockTags.LOGS);
		var tLeaves = tag(BlockTags.LEAVES);
		var tSaplings = tag(BlockTags.SAPLINGS);
		var tAxe = tag(BlockTags.MINEABLE_WITH_AXE);
		var tHoe = tag(BlockTags.MINEABLE_WITH_HOE);
		for (RegistryObject<Block> tHandle : GT6TreeBlocks.LOGS) {
			tLogs.add(tHandle.get());
			tAxe.add(tHandle.get());
		}
		for (RegistryObject<Block> tHandle : GT6TreeBlocks.LEAVES) {
			tLeaves.add(tHandle.get());
			tHoe.add(tHandle.get());
		}
		for (RegistryObject<Block> tHandle : GT6TreeBlocks.SAPLINGS) {
			tSaplings.add(tHandle.get());
		}
	}

	/**
	/**
	 * The wood-beam band (task beam-blocks-register): the 8 beams join
	 * {@code minecraft:mineable/axe} — the upstream harvest-tool override
	 * (BlockBaseBeam.java:54 {@code getHarvestTool = TOOL_axe}). NOT a #minecraft:logs
	 * member: the tag listener family (GT6CokeOvenLogExpansion) keys on the vanilla log
	 * tag and the beams are a separate upstream walk (the coke-oven beam rows stay the
	 * SKIPPED_UPSTREAM declaration's domain).
	 */
	private void addBeamBand() {
		var tAxe = tag(BlockTags.MINEABLE_WITH_AXE);
		for (RegistryObject<Block> tHandle : GT6BeamBlocks.BLOCKS) {
			tAxe.add(tHandle.get());
		}
		// task beam-fireproof-closeout: the FireProof twins ride the same BlockBaseBeam.java:54
		// TOOL_axe face (BlockTreeBeam*FireProof extends BlockBaseBeam, no override)
		for (RegistryObject<Block> tHandle : GT6BeamBlocks.FIREPROOF_BLOCKS) {
			tAxe.add(tHandle.get());
		}
	}

	/**
	 * The plank band (task gt-tree-planks): the 9 GT6 plank cubes join
	 * {@code #minecraft:planks} + {@code mineable/axe} — the upstream
	 * {@code OM.reg(..., OD.plankWood)} walk (BlockTreePlanks.java:59-62 every meta except
	 * the treated :10, BlockTreePlanks2.java:62-65) over the 9 port species rows, and the
	 * axe face rides the vanilla planks row (vanilla-1.20.1 mineable/axe.json). The
	 * function tag is LOAD-BEARING for crafting (the vanilla plank recipes gate on it).
	 */
	private void addPlankBand() {
		var tPlanks = tag(BlockTags.PLANKS);
		var tAxe = tag(BlockTags.MINEABLE_WITH_AXE);
		for (RegistryObject<Block> tHandle : GT6TreeBlocks.PLANKS) {
			tPlanks.add(tHandle.get());
			tAxe.add(tHandle.get());
		}
	}

	/**
	 * The mineable/wrench band (task wrench-mining-face, ruling B): the whole
	 * {@link GTMachines} register (keep-and-add — every machine stays in the pickaxe band
	 * AND joins the wrench face, no yielding), the twelve battery boxes
	 * ({@link GT6Batteries#BATTERY_BOX_BLOCKS} — the reported symptom family, upstream
	 * aMachine TOOL_wrench, Loader_MultiTileEntities.java:894 hardness 4.0), and the nine
	 * vanilla members unfolded from {@code GT_Tool_Wrench.isMinableBlock} (:72-81: the piston
	 * material ×4, the redstoneLight lamp, the bars pane, hopper/dispenser/
	 * dropper). The consumer is {@link GTWrenchItem} — the tag IS the mining face (ruling
	 * A makes everything outside it dig at ZERO). Ruling C: requiresCorrectToolForDrops
	 * stays OFF (the punitive no-drop is the defer pool). The dual-tree singular twin
	 * (tags/block) is the automatic GT6DualDirectoryFaces mirror.
	 *
	 * <p><b>Band extension</b> (task harvest-bands-wrench-machines — the whole upstream
	 * aMachine=wrench domain, the harvest-tool census state
	 * research.harvest-tool-census; EVERY family re-verified line-by-line in
	 * tmp/gt6-1.7.10 Loader_MultiTileEntities.java before landing, the table IS the
	 * acceptance evidence):
	 *
	 * <ul>
	 * <li>{@link gregtech6.registry.GTMultiBlocks} — metal walls 11 (:1143-1153), dense
	 * walls 11 (:1155-1165), coils 6 (:1167-1172), parts 7 (:1174-1182) + the Heat
	 * Transmitter (:1176, the census "8 parts"), the rod pillar (:1179), the ventilation
	 * unit (:1184) + five processor units (:1185-1189), the five Large Boilers
	 * (:1248-1252), six wrench controllers — lightning rod output 17998, implosion
	 * compressor :1228, massfab :1241, fusion reactor :1242, von da Graagg :1280, bedrock
	 * drill :1283. OUT: coke oven + bricks (:1138/:1193 aStone, the pickaxe-stone card),
	 * the wood wall (:1139 aWooden, the axe-wood card).</li>
	 * <li>{@link GT6Hoppers} — 120 (60 plain :145 + 60 queue :146, both aMachine).</li>
	 * <li>{@link GTItemPipes} — 126 (21 materials × 6 variants, :1823-1843 aMachine).</li>
	 * <li>{@link GTFluidPipes} MACHINE subdomain — 245 (35 metal materials × 7 variants,
	 * :1851-1860 aMachine; the wood 28 → axe, the rubber 7 stays pickaxe — the shears
	 * defer, card 4).</li>
	 * <li>{@link GT6Tanks} — 24 metal tank main valves (:1196-1222 aMachine; the wood
	 * valve :1195 aWooden stays out, the axe-wood card).</li>
	 * <li>{@link GT6Turbines} — 8 (steam :1254-1257, gas :1264-1267).</li>
	 * <li>{@link GT6Reactors} — 1 (the 2x2 core, :738 aMachine).</li>
	 * <li>{@link GT6QuantumEnergizers} — 5 (:962-966).</li>
	 * <li>{@link GT6ZpmDechargers} — 2 (:1000-1001).</li>
	 * <li>{@link GT6MagicAbsorbers} — 1 (:1005).</li>
	 * <li>{@link GTEnergySources} + {@link GTBlockEntities} machine-BE domain — 3 (the
	 * energy_source rig + the two test machines; port-native BE-domain blocks, the census
	 * "BlockEntities 机器域" seat — no upstream row to check, declared as such).</li>
	 * <li>{@link GT6LongDistPipes} — 18 (16 pipeline metas + the item/fluid endpoints,
	 * BlockLongDistPipe.java:42 wrench + :906-907 aMachine).</li>
	 * <li>{@link GT6LongDistanceTransformers} — 5 (:909-913 aMachine).</li>
	 * <li>{@link GT6StaticStorages} — the 8 metal rows (safes :134-135, lockers :138,
	 * drawers :140, all aMachine; the bookshelf/bottlecrate wooden ladders stay out,
	 * the axe-wood card).</li>
	 * </ul>
	 *
	 * <p>DECLARED EXPEDIENTS: {@link GTWires} keeps pickaxe (upstream cutter
	 * :1898-1950, the port cutter has no mining face) and {@link GT6LongDistWires} joins
	 * pickaxe for the same reason (BlockLongDistWire.java:56-57 cutter level 3) — both
 * ride the card-4 ruling pool. NOT THIS CARD (the census card split): the moulds/
 * crucibles/sensors/grindstones/sifting tables/mortars pickaxe family (card 2),
 * the remaining aWooden/aUtilWood families (card 3), the shears/scoop domains
 * (card 4), and the not-yet-enumerated aMachine registers (electric transformers
 * :884-889, kinetics :1672-1694, laser/logistics lines :1815-1819, heat exchanger
 * :1245, distillation towers :1226-1227, dynamo housings :1259-1262 — tail-card
 * candidates, reported to the coordinator). CENSUS CARD-SPLIT TYPO CORRECTED: the
 * split line put "Crucibles 金属" on THIS card, but the upstream metal crucibles
 * (:264-270) ride aMetal = TOOL_pickaxe (the census families table) — upstream has NO
 * wrench crucible, so the whole {@link gregtech6.registry.GT6Crucibles} family stays
 * on the pickaxe card.
	 */
	private void addWrenchBand() {
		var tWrench = tag(GTWrenchItem.MINEABLE_WITH_WRENCH);
		GTMachines.BLOCKS.getEntries().forEach(tHandle -> tWrench.add(tHandle.get()));
		for (RegistryObject<Block> tHandle : GT6Batteries.BATTERY_BOX_BLOCKS.values()) {
			tWrench.add(tHandle.get());
		}
		tWrench.add(Blocks.PISTON, Blocks.STICKY_PISTON, Blocks.PISTON_HEAD, Blocks.MOVING_PISTON);
		tWrench.add(Blocks.REDSTONE_LAMP, Blocks.IRON_BARS);
		tWrench.add(Blocks.HOPPER, Blocks.DISPENSER, Blocks.DROPPER);
		// --- the harvest-bands-wrench-machines extension (the javadoc table is the pin) ---
		gregtech6.registry.GTMultiBlocks.NEW_PART_BLOCKS_BY_PATH.forEach((tPath, tHandle) -> {
			if (!gregtech6.registry.GTMultiBlocks.WOOD_WALL_ROW.path().equals(tPath)) {
				tWrench.add(tHandle.get()); // the :1139 wood wall is aWooden — the axe-wood card
			}
		});
		gregtech6.registry.GTMultiBlocks.LIGHTNING_ROD_PART_BLOCKS_BY_PATH
				.forEach((tPath, tHandle) -> tWrench.add(tHandle.get()));
		gregtech6.registry.GTMultiBlocks.WALL_BLOCKS_BY_PATH.values().forEach(tHandle -> tWrench.add(tHandle.get()));
		gregtech6.registry.GTMultiBlocks.LARGE_BOILER_BLOCKS_BY_PATH.values().forEach(tHandle -> tWrench.add(tHandle.get()));
		tWrench.add(gregtech6.registry.GTMultiBlocks.HEAT_TRANSMITTER.get());
		tWrench.add(gregtech6.registry.GTMultiBlocks.LIGHTNING_ROD.get()); // the 17998 output, :1179 is the pillar part above
		tWrench.add(gregtech6.registry.GTMultiBlocks.IMPLOSION_COMPRESSOR.get());
		tWrench.add(gregtech6.registry.GTMultiBlocks.VON_DA_GRAAGG.get());
		tWrench.add(gregtech6.registry.GTMultiBlocks.MASSFAB.get());
		tWrench.add(gregtech6.registry.GTMultiBlocks.FUSION_REACTOR.get());
		tWrench.add(gregtech6.registry.GTMultiBlocks.BEDROCK_DRILL.get());
		GT6Hoppers.BLOCKS_BY_PATH.values().forEach(tHandle -> tWrench.add(tHandle.get()));
		GTItemPipes.BLOCKS_BY_PATH.values().forEach(tHandle -> tWrench.add(tHandle.get()));
		for (var tEntry : GTFluidPipes.BLOCKS_BY_PATH.entrySet()) {
			if (GTFluidPipes.rowByPath(tEntry.getKey()).material().blockFamily() == GTFluidPipes.PipeBlockFamily.MACHINE) {
				tWrench.add(tEntry.getValue().get());
			}
		}
		for (var tRow : GT6Tanks.ROWS) {
			if (!tRow.flammable()) { // the :1195 wood valve is aWooden — the axe-wood card
				tWrench.add(GT6Tanks.BLOCKS_BY_PATH.get(tRow.path()).get());
			}
		}
		GT6Turbines.BLOCKS_BY_PATH.values().forEach(tHandle -> tWrench.add(tHandle.get()));
		tWrench.add(GT6Reactors.REACTOR_CORE_2X2_BLOCK.get());
		GT6QuantumEnergizers.QUANTUM_ENERGIZER_BLOCKS_BY_PATH.values().forEach(tHandle -> tWrench.add(tHandle.get()));
		GT6ZpmDechargers.BLOCKS_BY_PATH.values().forEach(tHandle -> tWrench.add(tHandle.get()));
		tWrench.add(GT6MagicAbsorbers.MAGIC_ABSORBER_BLOCKS_BY_PATH.get("magic_absorber").get());
		tWrench.add(GTEnergySources.ENERGY_SOURCE.get());
		tWrench.add(GTBlockEntities.TEST_MACHINE.get(), GTBlockEntities.TEST_MACHINE_IDLE.get());
		GT6LongDistPipes.WIRE_BLOCKS_BY_META.values().forEach(tHandle -> tWrench.add(tHandle.get()));
		tWrench.add(GT6LongDistPipes.ITEM_PIPE_BLOCK.get(), GT6LongDistPipes.FLUID_PIPE_BLOCK.get());
		GT6LongDistanceTransformers.BLOCKS_BY_PATH.values().forEach(tHandle -> tWrench.add(tHandle.get()));
		for (var tRow : GT6StaticStorages.ROWS) {
			// the aMachine static ladders (safes/lockers/drawers, Loader :134-140 + the METAL
			// BOOKSHELF ladder :143); the wooden subset stays the axe card; the METAL
			// BOTTLECRATE stays out — :144 rides aUtilMaterial = TOOL_pickaxe (the :107
			// carrier column), the addPickaxeBand tail (task material-mc-b-storage-mass-shelf,
			// the review-seat band-split seam: the merged walk had put all 128 metal rows on
			// this face, the stale committed tag product hid it)
			if (tRow.material() != null && tRow.kind() != gregtech6.registry.GT6StaticStorages.Kind.BOTTLECRATE) {
				tWrench.add(GT6StaticStorages.BLOCKS_BY_PATH.get(tRow.path()).get());
			}
		}
	}
}
