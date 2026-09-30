package gregtech6.registry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import gregtech6.block.pipe.GTItemPipeBlock;
import gregtech6.block.pipe.GTItemPipeBlockItem;
import gregtech6.tileentity.connectors.GTItemPipeBlockEntity;
import gregtech6.tooltip.GT6Tooltips;

/**
 * Item pipe registration, card-owned (ADR-P3-4): self-contained
 * {@code @EventBusSubscriber(MOD)} DeferredRegisters attached from the construct event —
 * the {@link GTFluidPipes} shape repeated verbatim so the two pipe cards' scopes stay
 * disjoint (this class NEVER touches GTFluidPipes/GTFluidPipeCommand — the arch
 * ruling; tasks.p26-pipe-item spec ⑤).
 *
 * <p>The family is row-driven (the GT6Boilers.BoilerRow precedent): one upstream
 * {@code MultiTileEntityPipeItem.addItemPipes} line per material
 * (Loader_MultiTileEntities.java:1823-1843) expands to SIX block rows through the
 * variant table ({@link ItemPipeVariant}, MultiTileEntityPipeItem.java:76-83 — the
 * stepSize divisors {1, 2, 4} and multipliers {100, 50, 25}, the invSize multipliers
 * {1, 2, 4}). The full matrix = the loader's 21 material lines × 6 variants = 126 rows
 * (task item-pipe-matrix — every line verbatim, including the per-line aStepSize /
 * aInvSize columns the first batch collapsed to constants).
 *
 * <p>Upstream anchor numbers per row: the metaIds ride the addItemPipes bases at the
 * variant offsets +2..+7 (:76-83 aID+n); stepSize runs 32764..64 and invSize 1..512
 * across the loader lines. The zh display words are the dump rows verbatim
 * (tmp/gregtech.lang gt.multitileentity.25002-25907 物流管道 family, 限制 prefix for
 * the restrictive variants; the Ultimet word rides the p27-lang-fix-batch2 ⑤ ruling
 * 钴铬钨合金, NOT the dump's 哈氏合金 misattribution; itemGroup.gt.multitileentity.
 * 25202 = 物品管道 :17993). ElvenElementium's local is "Elementium" (MT.java:1822
 * setLocal) — the pipe rows carry that face, not the internal name.
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GTItemPipes {

	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "gt6");
	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "gt6");
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gt6");
	public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "gt6");

	/** The display-key namespace of the variant templates ({@code %s} = the material word). */
	public static final String DISPLAY_KEY_PREFIX = "gt6.row.item_pipe.display.";

	/** One pipe material of the family — one loader line's arguments (:1823-1843). */
	public record ItemPipeMaterial(String slug, String displayWord, int metaIdBase, long baseStepSize, int baseInvSize) {
		/** The shared gt6.row.mat small-unit key (the boiler matUnitKeyOf shape). */
		public String unitKey() {
			return "gt6.row.mat." + slug;
		}

		/**
		 * The row's ore-dict material — the loader line's {@code MT.*} argument verbatim
		 * (Loader_MultiTileEntities.java:1823-1843). The tex-pipe-textures tint chain
		 * resolves the mRGBa seat from it (upstream NBT_MATERIAL + NBT_COLOR = getRGBInt
		 * (fRGBaSolid), MultiTileEntityPipeItem.java:76-82). Loud drift on an unknown
		 * slug — the table only ever grows with a loader line in hand.
		 */
		public gregapi.oredict.OreDictMaterial oreDictMaterial() {
			return switch (slug) {
				case "brass" -> gregapi.data.MT.Brass;
				case "constantan" -> gregapi.data.MT.Constantan;
				case "cobalt_brass" -> gregapi.data.MT.CobaltBrass;
				case "germanium" -> gregapi.data.MT.Ge;
				case "arsenic_copper" -> gregapi.data.MT.ArsenicCopper;
				case "arsenic_bronze" -> gregapi.data.MT.ArsenicBronze;
				case "electrum" -> gregapi.data.MT.Electrum;
				case "sterling_silver" -> gregapi.data.MT.SterlingSilver;
				case "rose_gold" -> gregapi.data.MT.RoseGold;
				case "angmallen" -> gregapi.data.MT.Angmallen;
				case "black_bronze" -> gregapi.data.MT.BlackBronze;
				case "aluminium_brass" -> gregapi.data.MT.AluminiumBrass;
				case "manyullyn" -> gregapi.data.MT.Manyullyn;
				case "magnalium" -> gregapi.data.MT.Magnalium;
				case "platinum" -> gregapi.data.MT.Pt;
				case "osmium" -> gregapi.data.MT.Os;
				case "enderium" -> gregapi.data.MT.Enderium;
				case "ultimet" -> gregapi.data.MT.Ultimet;
				case "elementium" -> gregapi.data.MT.ElvenElementium;
				case "osmiridium" -> gregapi.data.MT.Osmiridium;
				case "vibranium_silver" -> gregapi.data.MT.VibraniumSilver;
				default -> throw new IllegalStateException("no ore-dict material pinned for pipe slug " + slug);
			};
		}
	}

	/**
	 * The six variants addItemPipes registers per material (MultiTileEntityPipeItem.java:77-82,
	 * one row each): stepSize {1, ½, ¼, ×100, ×50, ×25} and invSize {×1, ×2, ×4} over the
	 * material base. The metaId offset is the {@code aID+n} of the registration call.
	 */
	public enum ItemPipeVariant {
		MEDIUM("medium", 1, 1, 1, 2, "medium", 8),
		LARGE("large", 2, 1, 2, 3, "large", 12),
		HUGE("huge", 4, 1, 4, 4, "huge", 16),
		RESTRICTIVE_MEDIUM("restrictive_medium", 1, 100, 1, 5, "restrictive_medium", 8),
		RESTRICTIVE_LARGE("restrictive_large", 1, 50, 2, 6, "restrictive_large", 12),
		RESTRICTIVE_HUGE("restrictive_huge", 1, 25, 4, 7, "restrictive_huge", 16);

		/** The path tail ({@code <mat>_item_pipe_<suffix>}). */
		public final String suffix;
		/** The stepSize divisor (upstream {@code aStepSize / d}, :77-82). */
		public final int stepDiv;
		/** The stepSize multiplier (upstream {@code aStepSize * m}, :80-82). */
		public final int stepMul;
		/** The invSize multiplier (upstream {@code aInvSize * i}, :77-82). */
		public final int invMul;
		/** The {@code aID+n} registration offset (:77-82). */
		public final int metaOffset;
		/** The display template tail ({@link #displayKey}). */
		public final String displayTail;
		/** The pipe diameter in pixels (NBT_DIAMETER PX_P[8/12/16], MultiTileEntityPipeItem.java:76-82; the restrictive twins share the base diameter). */
		public final int diameterPx;

		ItemPipeVariant(String aSuffix, int aStepDiv, int aStepMul, int aInvMul, int aMetaOffset, String aDisplayTail,
				int aDiameterPx) {
			suffix = aSuffix;
			stepDiv = aStepDiv;
			stepMul = aStepMul;
			invMul = aInvMul;
			metaOffset = aMetaOffset;
			displayTail = aDisplayTail;
			diameterPx = aDiameterPx;
		}

		/** Upstream :77-82 — {@code aStepSize / d * m} (exact over the 32768 base). */
		public long stepSizeOf(long aBase) {
			return aBase / stepDiv * stepMul;
		}

		/** Upstream :77-82 — {@code aInvSize * i}. */
		public int invSizeOf(int aBase) {
			return aBase * invMul;
		}

		/** The variant display template ({@code %s} = the material word). */
		public String displayKey() {
			return DISPLAY_KEY_PREFIX + displayTail;
		}
	}

	/** One registration row — one block of the family (the BoilerRow projection shape). */
	public record ItemPipeRow(ItemPipeMaterial material, ItemPipeVariant variant, long stepSize, int invSize, int metaId) {
		/** {@code <mat>_item_pipe_<suffix>} — the arch ruling: material first (the wood_fluid_pipe_small order). */
		public String path() {
			return material.slug() + "_item_pipe_" + variant.suffix;
		}

		/** The composed display name — the pure compose seam (the BoilerRow displayOf shape). */
		public MutableComponent displayName() {
			return Component.translatable(variant.displayKey(), Component.translatable(material.unitKey()));
		}
	}

	// The loader's 21 material lines, verbatim order and columns
	// (Loader_MultiTileEntities.java:1823-1843 — aID, MT local, aStepSize, aInvSize).
	public static final ItemPipeMaterial MAT_BRASS = new ItemPipeMaterial("brass", "Brass", 25000, 32768, 1);
	public static final ItemPipeMaterial MAT_CONSTANTAN = new ItemPipeMaterial("constantan", "Constantan", 25025, 32768, 1);
	public static final ItemPipeMaterial MAT_COBALT_BRASS = new ItemPipeMaterial("cobalt_brass", "Cobalt Brass", 25050, 32768, 1);
	public static final ItemPipeMaterial MAT_GERMANIUM = new ItemPipeMaterial("germanium", "Germanium", 25075, 32768, 1);
	public static final ItemPipeMaterial MAT_ARSENIC_COPPER = new ItemPipeMaterial("arsenic_copper", "Arsenic Copper", 25350, 16384, 1);
	public static final ItemPipeMaterial MAT_ARSENIC_BRONZE = new ItemPipeMaterial("arsenic_bronze", "Arsenic Bronze", 25375, 32768, 2);
	public static final ItemPipeMaterial MAT_ELECTRUM = new ItemPipeMaterial("electrum", "Electrum", 25100, 16384, 2);
	public static final ItemPipeMaterial MAT_STERLING_SILVER = new ItemPipeMaterial("sterling_silver", "Sterling Silver", 25225, 16384, 2);
	public static final ItemPipeMaterial MAT_ROSE_GOLD = new ItemPipeMaterial("rose_gold", "Rose Gold", 25250, 16384, 2);
	public static final ItemPipeMaterial MAT_ANGMALLEN = new ItemPipeMaterial("angmallen", "Angmallen", 25275, 16384, 2);
	public static final ItemPipeMaterial MAT_BLACK_BRONZE = new ItemPipeMaterial("black_bronze", "Black Bronze", 25125, 16384, 2);
	public static final ItemPipeMaterial MAT_ALUMINIUM_BRASS = new ItemPipeMaterial("aluminium_brass", "Aluminium Brass", 25150, 16384, 2);
	public static final ItemPipeMaterial MAT_MANYULLYN = new ItemPipeMaterial("manyullyn", "Manyullyn", 25175, 16384, 2);
	public static final ItemPipeMaterial MAT_MAGNALIUM = new ItemPipeMaterial("magnalium", "Magnalium", 25325, 16384, 2);
	public static final ItemPipeMaterial MAT_PLATINUM = new ItemPipeMaterial("platinum", "Platinum", 25200, 8192, 4);
	public static final ItemPipeMaterial MAT_OSMIUM = new ItemPipeMaterial("osmium", "Osmium", 25300, 4096, 8);
	public static final ItemPipeMaterial MAT_ENDERIUM = new ItemPipeMaterial("enderium", "Enderium", 25400, 2048, 16);
	public static final ItemPipeMaterial MAT_ULTIMET = new ItemPipeMaterial("ultimet", "Ultimet", 25425, 2048, 16);
	public static final ItemPipeMaterial MAT_ELEMENTIUM = new ItemPipeMaterial("elementium", "Elementium", 25475, 2048, 16);
	public static final ItemPipeMaterial MAT_OSMIRIDIUM = new ItemPipeMaterial("osmiridium", "Osmiridium", 25500, 1024, 32);
	public static final ItemPipeMaterial MAT_VIBRANIUM_SILVER = new ItemPipeMaterial("vibranium_silver", "Vibranium Silver", 25900, 64, 512);

	public static final List<ItemPipeMaterial> MATERIALS = List.of(
			MAT_BRASS, MAT_CONSTANTAN, MAT_COBALT_BRASS, MAT_GERMANIUM,
			MAT_ARSENIC_COPPER, MAT_ARSENIC_BRONZE, MAT_ELECTRUM, MAT_STERLING_SILVER,
			MAT_ROSE_GOLD, MAT_ANGMALLEN, MAT_BLACK_BRONZE, MAT_ALUMINIUM_BRASS,
			MAT_MANYULLYN, MAT_MAGNALIUM, MAT_PLATINUM, MAT_OSMIUM,
			MAT_ENDERIUM, MAT_ULTIMET, MAT_ELEMENTIUM, MAT_OSMIRIDIUM,
			MAT_VIBRANIUM_SILVER);

	/** The registration-order variant list (the addItemPipes body order, :77-82). */
	public static final List<ItemPipeVariant> VARIANTS = List.of(ItemPipeVariant.values());

	/** All 126 rows in registration order (material-major, then the addItemPipes variant order). */
	public static final List<ItemPipeRow> ROWS;
	static {
		List<ItemPipeRow> tRows = new ArrayList<>();
		for (ItemPipeMaterial tMat : MATERIALS) {
			for (ItemPipeVariant tVariant : VARIANTS) {
				tRows.add(new ItemPipeRow(tMat, tVariant,
						tVariant.stepSizeOf(tMat.baseStepSize()), tVariant.invSizeOf(tMat.baseInvSize()),
						tMat.metaIdBase() + tVariant.metaOffset));
			}
		}
		ROWS = List.copyOf(tRows);
	}

	/** The registered blocks by path (the BET multi-mount + the datagen/command walkers). */
	public static final Map<String, RegistryObject<GTItemPipeBlock>> BLOCKS_BY_PATH = new LinkedHashMap<>();

	/** The registered items, same keys as {@link #BLOCKS_BY_PATH}. */
	public static final Map<String, RegistryObject<Item>> ITEMS_BY_PATH = new LinkedHashMap<>();

	/**
	 * The shared pipe properties — the ONE chain all 126 rows build from (the GTWires
	 * .wireProperties seam form). issue #9: the pipe renders sub-cube quads over the
	 * default FULL-CUBE shape — a true canOcclude culls neighbor faces (X-ray); the axle
	 * pipe-block convention (GT6Kinetics.java:244).
	 */
	static BlockBehaviour.Properties pipeProperties() {
		return BlockBehaviour.Properties.of()
				.strength(2.0F, 6.0F).sound(SoundType.METAL) // the Loader :77-82 NBT pair
				.noOcclusion().isViewBlocking(GTItemPipes::never);
	}

	static {
		for (ItemPipeRow tRow : ROWS) {
			// hardness/resistance = the registration NBT pair (NBT_HARDNESS 2.0/NBT_RESISTANCE
			// 6.0, :77-82); the METAL sound is the material-family visual axis (the port's
			// declared normalisation layer, the wood-fluid-pipe WOOD-sound precedent).
			BLOCKS_BY_PATH.put(tRow.path(), BLOCKS.register(tRow.path(),
					() -> new GTItemPipeBlock(rowByPath(tRow.path()), pipeProperties())));
			// the composed-name item (the boiler GTComposedNameItem posture — the stack name
			// delegates to the block's composed getName); the pipe_item rows ride the
			// per-row constants [stepSize, invSize] — MultiTileEntityPipeItem.java:116-117
			// (stepsize = makeString(mStepSize), bandwidth = makeString(getPipeCapacity()
			// = invsize) + "/s"), formatted at the site (UT.Code.makeString)
			ITEMS_BY_PATH.put(tRow.path(), ITEMS.register(tRow.path(),
					() -> new GTItemPipeBlockItem(GTItemPipes.BLOCKS_BY_PATH.get(tRow.path()).get(), new Item.Properties(), "pipe_item",
							GT6Tooltips.makeString(tRow.stepSize()), GT6Tooltips.makeString(tRow.invSize()))));
		}
	}

	/** The row lookup behind the block-carrier lambda (path -> row, the static ROWS table). */
	@Nullable
	public static ItemPipeRow rowByPath(String aPath) {
		for (ItemPipeRow tRow : ROWS) {
			if (tRow.path().equals(aPath)) return tRow;
		}
		return null;
	}

	/**
	 * The shared pipe BET: one BlockEntityType over all 126 blocks (ADR-P3-1, the
	 * "one TE class, many material blocks" multi-mount). Registry path "item_pipe"
	 * mirrors {@link GTItemPipeBlockEntity#getTileEntityName()} (the card spec ⑤ name).
	 */
	public static final RegistryObject<BlockEntityType<GTItemPipeBlockEntity>> ITEM_PIPE_BE =
			BLOCK_ENTITY_TYPES.register("item_pipe", () -> BlockEntityType.Builder.of(
					GTItemPipeBlockEntity::new, blockArray()).build(null));

	/** The block list in registration order (the BET multi-mount array). */
	public static Block[] blockArray() {
		Block[] rBlocks = new Block[BLOCKS_BY_PATH.size()];
		int i = 0;
		for (RegistryObject<GTItemPipeBlock> tBlock : BLOCKS_BY_PATH.values()) rBlocks[i++] = tBlock.get();
		return rBlocks;
	}

	/** The lookup for /gt6itempipe place — null for an unknown path (the blockByPath precedent). */
	@Nullable
	public static Block blockByPath(String aPath) {
		RegistryObject<GTItemPipeBlock> tHandle = BLOCKS_BY_PATH.get(aPath);
		return tHandle == null ? null : tHandle.get();
	}

	/**
	 * The "Item Pipes" category tab — the upstream MTE category (the aCreativeTabID 25202
	 * column, Loader_MultiTileEntities.java:1823-1843; the dump row :17993 物品管道), the
	 * GTFluidPipes FLUID_PIPES_TAB shape.
	 */
	public static final RegistryObject<CreativeModeTab> ITEM_PIPES_TAB = CREATIVE_MODE_TABS.register("item_pipes",
			() -> CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
					.title(Component.translatable("itemGroup.gt6.item_pipes"))
					.icon(() -> new ItemStack(ITEMS_BY_PATH.get("brass_item_pipe_medium").get()))
					.displayItems((aParameters, aOutput) -> {
						for (ItemPipeRow tRow : ROWS) {
							aOutput.accept(new ItemStack(ITEMS_BY_PATH.get(tRow.path()).get()));
						}
					})
					.build());

	private GTItemPipes() {
	}

	/** issue #9: the pipe family never blocks the view (fog) — the GTWires::never rider form. */
	private static boolean never(BlockState aState, BlockGetter aLevel, BlockPos aPos) {
		return false;
	}

	/** FMLConstructModEvent = the first mod-bus lifecycle stage (GTFluidPipes.onModConstruct verbatim). */
	@SubscribeEvent
	public static void onModConstruct(FMLConstructModEvent aEvent) {
		//? if forge {
		IEventBus tModBus = Mod.EventBusSubscriber.Bus.MOD.bus().get();
		//?} else {
		/*IEventBus tModBus = net.neoforged.fml.ModList.get().getModContainerById("gt6").orElseThrow().getEventBus();
		//21.1: Mod.EventBusSubscriber.Bus died with the annotation rework (GTFluidPipes fork verbatim)
		*///?}
		BLOCKS.register(tModBus);
		BLOCK_ENTITY_TYPES.register(tModBus);
		ITEMS.register(tModBus);
		CREATIVE_MODE_TABS.register(tModBus);
	}
}
