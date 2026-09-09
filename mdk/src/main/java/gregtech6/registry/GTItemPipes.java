package gregtech6.registry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import gregtech6.block.GTComposedNameItem;
import gregtech6.block.pipe.GTItemPipeBlock;
import gregtech6.block.pipe.GTItemPipeBlockItem;
import gregtech6.tileentity.connectors.GTItemPipeBlockEntity;

/**
 * Item pipe registration, card-owned (ADR-P3-4): self-contained
 * {@code @EventBusSubscriber(MOD)} DeferredRegisters attached from the construct event —
 * the {@link GTFluidPipes} shape repeated verbatim so the two pipe cards' scopes stay
 * disjoint (this class NEVER touches GTFluidPipes/GTFluidPipeCommand — the p26-arch
 * ruling; tasks.p26-pipe-item spec ⑤).
 *
 * <p>The family is row-driven (the GT6Boilers.BoilerRow precedent): one upstream
 * {@code MultiTileEntityPipeItem.addItemPipes} line per material
 * (Loader_MultiTileEntities.java:1823-1825) expands to SIX block rows through the
 * variant table ({@link ItemPipeVariant}, MultiTileEntityPipeItem.java:76-83 — the
 * stepSize divisors {1, 2, 4} and multipliers {100, 50, 25}, the invSize multipliers
 * {1, 2, 4}). First batch = Brass / Constantan / CobaltBrass (the loader's first three
 * lines, :1823-1825; the other 18 material rows are a later line-data batch, card spec ⑥).
 *
 * <p>Upstream anchor numbers per row: the metaIds ride the addItemPipes bases
 * 25000 / 25025 / 25050 at the variant offsets +2..+7 (:76-83 aID+n), base stepSize
 * 32768 and invSize 1 (:1823-1825). The zh display words are the dump rows verbatim
 * (tmp/gregtech.lang:11849-11866 — 黄铜/康铜/钴黄铜物流管道 family, 限制 prefix for the
 * restrictive variants; itemGroup.gt.multitileentity.25202 = 物品管道 :17993).
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GTItemPipes {

	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "gt6");
	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "gt6");
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gt6");
	public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "gt6");

	/** The display-key namespace of the variant templates ({@code %s} = the material word). */
	public static final String DISPLAY_KEY_PREFIX = "gt6.row.item_pipe.display.";

	/** One pipe material of the first batch — the loader line's MT argument (:1823-1825). */
	public record ItemPipeMaterial(String slug, String displayWord, int metaIdBase) {
		/** The shared gt6.row.mat small-unit key (the boiler matUnitKeyOf shape). */
		public String unitKey() {
			return "gt6.row.mat." + slug;
		}
	}

	/**
	 * The six variants addItemPipes registers per material (MultiTileEntityPipeItem.java:77-82,
	 * one row each): stepSize {1, ½, ¼, ×100, ×50, ×25} and invSize {×1, ×2, ×4} over the
	 * material base. The metaId offset is the {@code aID+n} of the registration call.
	 */
	public enum ItemPipeVariant {
		MEDIUM("medium", 1, 1, 1, 2, "medium"),
		LARGE("large", 2, 1, 2, 3, "large"),
		HUGE("huge", 4, 1, 4, 4, "huge"),
		RESTRICTIVE_MEDIUM("restrictive_medium", 1, 100, 1, 5, "restrictive_medium"),
		RESTRICTIVE_LARGE("restrictive_large", 1, 50, 2, 6, "restrictive_large"),
		RESTRICTIVE_HUGE("restrictive_huge", 1, 25, 4, 7, "restrictive_huge");

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

		ItemPipeVariant(String aSuffix, int aStepDiv, int aStepMul, int aInvMul, int aMetaOffset, String aDisplayTail) {
			suffix = aSuffix;
			stepDiv = aStepDiv;
			stepMul = aStepMul;
			invMul = aInvMul;
			metaOffset = aMetaOffset;
			displayTail = aDisplayTail;
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

	// The loader's first three material lines, verbatim order (:1823-1825).
	public static final ItemPipeMaterial MAT_BRASS = new ItemPipeMaterial("brass", "Brass", 25000);
	public static final ItemPipeMaterial MAT_CONSTANTAN = new ItemPipeMaterial("constantan", "Constantan", 25025);
	public static final ItemPipeMaterial MAT_COBALT_BRASS = new ItemPipeMaterial("cobalt_brass", "Cobalt Brass", 25050);

	public static final List<ItemPipeMaterial> MATERIALS = List.of(MAT_BRASS, MAT_CONSTANTAN, MAT_COBALT_BRASS);

	/** The registration-order variant list (the addItemPipes body order, :77-82). */
	public static final List<ItemPipeVariant> VARIANTS = List.of(ItemPipeVariant.values());

	/** The material base axis, verbatim (:1823-1825 — base stepSize 32768, base invSize 1). */
	public static final long BASE_STEP_SIZE = 32768;
	public static final int BASE_INV_SIZE = 1;

	/** All 18 rows in registration order (material-major, then the addItemPipes variant order). */
	public static final List<ItemPipeRow> ROWS;
	static {
		List<ItemPipeRow> tRows = new ArrayList<>();
		for (ItemPipeMaterial tMat : MATERIALS) {
			for (ItemPipeVariant tVariant : VARIANTS) {
				tRows.add(new ItemPipeRow(tMat, tVariant,
						tVariant.stepSizeOf(BASE_STEP_SIZE), tVariant.invSizeOf(BASE_INV_SIZE),
						tMat.metaIdBase() + tVariant.metaOffset));
			}
		}
		ROWS = List.copyOf(tRows);
	}

	/** The registered blocks by path (the BET multi-mount + the datagen/command walkers). */
	public static final Map<String, RegistryObject<GTItemPipeBlock>> BLOCKS_BY_PATH = new LinkedHashMap<>();

	/** The registered items, same keys as {@link #BLOCKS_BY_PATH}. */
	public static final Map<String, RegistryObject<Item>> ITEMS_BY_PATH = new LinkedHashMap<>();

	static {
		for (ItemPipeRow tRow : ROWS) {
			// hardness/resistance = the registration NBT pair (NBT_HARDNESS 2.0/NBT_RESISTANCE
			// 6.0, :77-82); the METAL sound is the material-family visual axis (the port's
			// declared normalisation layer, the wood-fluid-pipe WOOD-sound precedent).
			BLOCKS_BY_PATH.put(tRow.path(), BLOCKS.register(tRow.path(),
					() -> new GTItemPipeBlock(rowByPath(tRow.path()), BlockBehaviour.Properties.of()
							.strength(2.0F, 6.0F).sound(SoundType.METAL))));
			// the composed-name item (the boiler GTComposedNameItem posture — the stack name
			// delegates to the block's composed getName)
			ITEMS_BY_PATH.put(tRow.path(), ITEMS.register(tRow.path(),
					() -> new GTItemPipeBlockItem(GTItemPipes.BLOCKS_BY_PATH.get(tRow.path()).get(), new Item.Properties())));
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
	 * The shared pipe BET: one BlockEntityType over all 18 blocks (ADR-P3-1, the
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
