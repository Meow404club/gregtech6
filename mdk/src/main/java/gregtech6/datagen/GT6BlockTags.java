package gregtech6.datagen;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;

//? if forge {
import net.minecraftforge.common.data.BlockTagsProvider;
//?} else {
/*import net.neoforged.neoforge.common.data.BlockTagsProvider;
*///?}

import net.minecraftforge.common.data.ExistingFileHelper;

import gregapi.data.OP;
import gregapi.oredict.OreDictPrefix;
import gregtech6.registry.GTBarrels;
import gregtech6.registry.GTGrassBlocks;
import gregtech6.registry.GTMaterialBlocks;
import gregtech6.registry.GTMachines;
import gregtech6.registry.GTStoneBlocks;

/**
 * The GT6 block-tag datagen home — task p24-tags-provider-skeleton, the first
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
 * the GTCEu wood-drum-to-axe precedent, BlockTagLoader:69-71). The {@code blockDust}
 * prefix stays OUT (the shovel band is P1, a later batch); the plastic/metal barrels stay
 * OUT (P2); {@code requires_correct_tool_for_drops} and the {@code needs_*} gates stay
 * OUT (coupled to the chisel loot chain + harvestLevel wiring — the tool-system card).
 *
 * <p><b>Strictness is the acceptance asset</b>: vanilla TagsProvider throws
 * IllegalArgumentException for any reference that fails
 * {@code TagEntry.verifyIfPresent} (TagsProvider.java:85-94 — "Couldn't define tag %s as
 * it is missing following references"), and nothing here uses
 * {@code addOptional}/{@code addOptionalTag}. Every member below is a live-registered
 * block at datagen time (registration events precede GatherDataEvent — the GT6LootTables
 * live-block precedent), so a membership gap fails runData loudly instead of shipping a
 * silently dangling tag. The produced files carry {@code "replace": false} implicitly
 * (TagsProvider.java:96-97 {@code new TagFile(entries, false)}), i.e. the mod file JOINS
 * the vanilla tag datapack-wide — the GT6Atlases.java:21-25 merge-semantics precedent.
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
		addGrassBand(); // task p24-grass-block — the grass family band
		// the takeover seam: later cards tail-append their own add*Band() here
		// (p24-tags-prefix-materials: rolling batches).
	}

	/**
	 * The mineable/pickaxe band, three census families in walk order (deterministic output:
	 * the appender order IS the JSON order, so the second runData run is written:0). The
	 * machines walk rides {@link GTMachines#BLOCKS} {@code getEntries()} — the whole-class
	 * enumeration means a future machine row cannot silently miss the band; the lambda body
	 * compiles against both legs' entry handles ({@code RegistryObject} /
	 * {@code DeferredHolder}) without a fork.
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
	}

	/** The mineable/axe band — the wood fluid barrel (first batch: no plastic/metal rows, P2). */
	private void addAxeBand() {
		tag(BlockTags.MINEABLE_WITH_AXE).add(GTBarrels.BARREL.get());
	}

	/**
	 * The grass-family band (task p24-grass-block, the user tag-paradigm first case — every
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
}
