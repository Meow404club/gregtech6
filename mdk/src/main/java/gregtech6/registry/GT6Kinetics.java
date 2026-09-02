package gregtech6.registry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import gregtech6.block.energy.GTAxleBlock;
import gregtech6.block.energy.GTCrankBlock;

/**
 * The kinetics registration family home (task p12-engine-crank spec ③): the shared
 * DeferredRegister pair for the engine + rotation-transmission family — the Hand Crank
 * is its first member, the axle/gearbox/transformer/engine cards append here. Card-owned
 * (ADR-P3-4): self-contained {@code @EventBusSubscriber(MOD)} DeferredRegisters attached
 * from the construct event — GT6Mod.java / GTModBusListener.java stay untouched (the
 * frozen P2 form is never extended). Same shape as GTEnergySources / GTWires
 * (the GTEnergySources.java Supplier-block precedent); the shared BET row lives in
 * {@link GTBlockEntities#CRANK_BE} per the card (the BET type row in the shared registry
 * file, the ENERGY_SOURCE_BE/WIRE_ELECTRIC_BE cross-register resolution shape), and its
 * supplier resolves the block here — safe because the vanilla registry order fires the
 * Block registration event before the BlockEntityType one, across DeferredRegisters
 * (GTBlockEntities doc).
 *
 * <p>Block constants from the upstream registration row (Loader_MultiTileEntities.java
 * :2106): hardness 1.0F / resistance 6.0F, the metal tool material →
 * {@link SoundType#METAL}. No creative tab (the tab system is the per-family pool card;
 * the item stays /give-reachable).
 *
 * <p><b>The axle family</b> (task p12-axle-family spec ③): 11 materials x 4 diameters =
 * 44 blocks/items over the ONE shared {@link GTBlockEntities#AXLE_BE} (the ADR-P3-1
 * multi-mount, the GTWires 620-variant form). {@link #AXLE_SPECS} is the Loader kinetic
 * section verbatim (Loader_MultiTileEntities.java:1662-1744 and the material rows through
 * :1797): every row pins the speed rating VMAX[tier] (CS.java:150) and the four
 * per-diameter bandwidths (NBT_PIPEBANDWIDTH); the diameters are PX_P (CS.java:492) =
 * 6/9/12/16 px. The table is NAME+NUMBER only — no OreDictMaterial references: the static
 * init of this class runs at MOD construct time, where MT is not yet initialized (the
 * GTFluids lesson, p6 a9027ac).
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6Kinetics {

	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "gt6");
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gt6");

	/** The Hand Crank (upstream "Hand Crank" meta 32111, MultiTileEntityCrank port). */
	public static final RegistryObject<GTCrankBlock> CRANK = BLOCKS.register("crank",
			() -> new GTCrankBlock(BlockBehaviour.Properties.of()
					.strength(1.0F, 6.0F).sound(SoundType.METAL)));

	public static final RegistryObject<Item> CRANK_ITEM = ITEMS.register("crank",
			() -> new BlockItem(CRANK.get(), new Item.Properties()));

	// -------------------------------------------------------------------------
	// the axle family (task p12-axle-family) — 11 materials x 4 diameters = 44
	// -------------------------------------------------------------------------

	/** One Loader kinetic row (Loader_MultiTileEntities.java:1662-1797), name+number only (no MT at construct time). */
	public record AxleSpec(String material, String displayName, int tier, int[] bandwidth) {}

	/** The row speed rating: NBT_PIPESIZE = VMAX[tier] (Loader :1663 e.g. {@code NBT_PIPESIZE, VMAX[0]}). */
	public static long axleSpeed(AxleSpec aSpec) {
		return VMAX[aSpec.tier()];
	}

	/** PX_P (CS.java:492) — the four diameters in 1/16 block units, shared by every row. */
	public static final int[] AXLE_DIAMETERS = {6, 9, 12, 16};

	/** The per-diameter registry/lang tail, small → huge (the upstream row wording order). */
	public static final String[] AXLE_SIZE_NAMES = {"small", "medium", "large", "huge"};

	/** CS.java:150 VMAX verbatim — the speed rating per tier (the axle rows use tiers 0-7). */
	public static final long[] VMAX = {16, 64, 256, 1024, 4096, 16384, 65536, 262144,
			1048576L, 4194304L, 16777216L, 67108864L, 268435456L, 1073741824L,
			4294967296L, 17179869184L};

	/**
	 * The kinetic section verbatim: the material slug (this-port material registry form,
	 * {@code gt6.material.<slug>} — Iritanium is the titanium_iridium alloy row), the
	 * display material name (the upstream row wording — Wood rows say "Wooden" hard-coded,
	 * :1663-1666; the metal rows carry mNameLocal), the VMAX tier (the row's NBT_PIPESIZE
	 * = VMAX[tier], resolved through {@link #axleSpeed}) and the four bandwidths
	 * NBT_PIPEBANDWIDTH (small/medium/large/huge). Rows: WoodTreated :1662-1666, Bronze
	 * :1671-1675, Brass :1679-1683, ArsenicCopper :1687-1691, ArsenicBronze :1695-1699
	 * (3/6/12/24), Steel :1703-1707, Ti :1712-1716, TungstenSteel :1721-1725, Ir
	 * :1730-1734, Iritanium :1739-1743, Trinitanium :1748-1752 (each following material
	 * doubles the previous bandwidth ladder).
	 */
	public static final List<AxleSpec> AXLE_SPECS = List.of(
			new AxleSpec("wood_treated", "Wooden", 0, new int[] {1, 2, 4, 8}),
			new AxleSpec("bronze", "Bronze", 1, new int[] {2, 4, 8, 16}),
			new AxleSpec("brass", "Brass", 1, new int[] {2, 4, 8, 16}),
			new AxleSpec("arsenic_copper", "Arsenic Copper", 1, new int[] {2, 4, 8, 16}),
			new AxleSpec("arsenic_bronze", "Arsenic Bronze", 1, new int[] {3, 6, 12, 24}),
			new AxleSpec("steel", "Steel", 2, new int[] {4, 8, 16, 32}),
			new AxleSpec("titanium", "Titanium", 3, new int[] {8, 16, 32, 64}),
			new AxleSpec("tungstensteel", "Tungstensteel", 4, new int[] {16, 32, 64, 128}),
			new AxleSpec("iridium", "Iridium", 5, new int[] {32, 64, 128, 256}),
			new AxleSpec("titanium_iridium", "Iritanium", 6, new int[] {64, 128, 256, 512}),
			new AxleSpec("trinitanium", "Trinitanium", 7, new int[] {128, 256, 512, 1024}));

	/** The 44 registered axle blocks (the datagen/loot/BET walkers iterate this). */
	public static final Map<String, RegistryObject<GTAxleBlock>> AXLE_BLOCKS = new LinkedHashMap<>();

	/** The 44 registered axle items, same keys as {@link #AXLE_BLOCKS}. */
	public static final Map<String, RegistryObject<Item>> AXLE_ITEMS = new LinkedHashMap<>();

	/** The registry-name form: {@code axle_<material>_<size>}. */
	public static String axleName(String aMaterial, int aSizeIndex) {
		return "axle_" + aMaterial + "_" + AXLE_SIZE_NAMES[aSizeIndex];
	}

	/** The lang/display-name form: {@code <Size> <Material> Axle} (the upstream row wording). */
	public static String axleDisplay(AxleSpec aSpec, int aSizeIndex) {
		return Character.toUpperCase(AXLE_SIZE_NAMES[aSizeIndex].charAt(0)) + AXLE_SIZE_NAMES[aSizeIndex].substring(1)
				+ " " + aSpec.displayName() + " Axle";
	}

	/** The block list in declaration order (the BET multi-mount array + the loot/datagen walkers). */
	public static Block[] axleBlockArray() {
		Block[] rBlocks = new Block[AXLE_BLOCKS.size()];
		int i = 0;
		for (RegistryObject<GTAxleBlock> tBlock : AXLE_BLOCKS.values()) rBlocks[i++] = tBlock.get();
		return rBlocks;
	}

	/**
	 * The axle registration loop — 11 materials x 4 diameters, the Loader row body
	 * (NBT_HARDNESS 2.0F / NBT_RESISTANCE 6.0F; the wooden NBT_FLAMMABILITY half is a
	 * later card's surface) with the METAL sound of the crank row precedent; noOcclusion
	 * for the sub-cube rod shape (the pipe-block convention). Registration happens in
	 * {@link #onModConstruct} BEFORE the DeferredRegisters attach — the plain loop calls
	 * BLOCKS.register(name, supplier) directly (DeferredRegister is just a holder until
	 * register(bus) fires).
	 */
	private static void registerAxles() {
		for (AxleSpec tSpec : AXLE_SPECS) {
			for (int tSize = 0; tSize < AXLE_DIAMETERS.length; tSize++) {
				String tName = axleName(tSpec.material(), tSize);
				final AxleSpec fSpec = tSpec;
				final int fSize = tSize;
				RegistryObject<GTAxleBlock> tBlock = BLOCKS.register(tName, () -> new GTAxleBlock(
						BlockBehaviour.Properties.of().strength(2.0F, 6.0F).sound(SoundType.METAL).noOcclusion(),
						fSpec, fSize));
				AXLE_BLOCKS.put(tName, tBlock);
				AXLE_ITEMS.put(tName, ITEMS.register(tName, () -> new BlockItem(tBlock.get(), new Item.Properties())));
			}
		}
	}

	private GT6Kinetics() {}

	/** FMLConstructModEvent = the first mod-bus lifecycle stage (GTBlockEntities.onModConstruct doc). */
	@SubscribeEvent
	public static void onModConstruct(FMLConstructModEvent aEvent) {
		registerAxles();
		IEventBus tModBus = Mod.EventBusSubscriber.Bus.MOD.bus().get();
		BLOCKS.register(tModBus);
		ITEMS.register(tModBus);
	}
}
