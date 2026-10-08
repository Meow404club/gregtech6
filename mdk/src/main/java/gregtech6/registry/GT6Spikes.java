package gregtech6.registry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import gregapi.data.ANY;
import gregapi.data.MT;
import gregapi.oredict.OreDictMaterial;
import gregtech6.block.decor.GT6SpikeBlock;

/**
 * Registration home of the GT6 SPIKE families (task material-mc-g2-decor-misc, the
 * Loader_Blocks.java:94-98 rows): <b>10 per-material Block registrations</b> (the five
 * upstream families x their two materials — the (orientation 3b) x (material 1b) meta
 * ladder split per the P8 ADR ④ ruling) and <b>30 items</b> (wall / block / falling per
 * material — the crafted identities of BlockBaseSpike.java:65-74), in the card-owned
 * self-contained {@code @EventBusSubscriber(MOD)} DeferredRegister shape (the
 * GT6ConcreteBlocks precedent; GT6Mod untouched).
 *
 * <p>The five family rows, upstream verbatim (the damage tables are half-hearts x the
 * upstream TFC_DAMAGE_MULTIPLIER factor, wall 6-way / omni-falling):
 * <ul>
 * <li>sharp  = Steel(5/2.5) + Titanium(10/5) — immune Skeleton/Slime/IronGolem (Steel arm);</li>
 * <li>steel  = BlueSteel(8/4) + RedSteel(8/4) — immune IronGolem;</li>
 * <li>super  = TungstenSteel(15/7.5) + Adamantium(50/25);</li>
 * <li>metal  = Copper(20/10 vs Slimes, 2/1 else; immune Skeleton+Golem)
 *            + Lead(20/10 vs Arthropods, 2/1 else; immune Skeleton+Slime+Golem);</li>
 * <li>fancy  = Gold(20/10 vs Undead, 2/1 else; immune Slime+Golem)
 *            + Silver(20/10 vs Ender/Were, 2/1 else; immune Skeleton+Slime+Golem).</li>
 * </ul>
 *
 * <p>id scheme: {@code gt6:spike_<mat>}, items {@code gt6:spike_<mat>},
 * {@code gt6:spike_<mat>_block}, {@code gt6:spike_<mat>_falling}. Creative tab: the
 * vanilla REDSTONE_BLOCKS tab (the upstream {@code CreativeTabs.tabRedstone},
 * BlockBaseSpike.java:62 — the 1.20.1 tab mapping).
 *
 * <p>KJS: registration face = the declared defer; the recipe face (the shaped wall/block
 * spike rows + the shapeless 6-7 folds, BlockBaseSpike.java:65-74) is datapack domain.
 */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = "gt6", bus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD)
public final class GT6Spikes {

	public static final DeferredRegister<Block> BLOCKS_REG = DeferredRegister.create(Registries.BLOCK, "gt6");
	public static final DeferredRegister<Item> ITEMS_REG = DeferredRegister.create(Registries.ITEM, "gt6");

	/** One material row — the family letter names the upstream class tail (sharp/steel/super/metal/fancy);
	 * the material rides a Supplier (the GT6GasCylinderRow shape — rows must never dereference
	 * MT at class-load time, the offline/datagen bare-JVM trap). */
	public record SpikeRow(String family, String snake, java.util.function.Supplier<OreDictMaterial> material,
			float wallDamage, float omniDamage,
			GT6SpikeBlock.PreyPredicate prey, GT6SpikeBlock.ImmunityPredicate immune) {}

	/** The 10 material rows, family-major in the Loader_Blocks.java:94-98 order. */
	public static final List<SpikeRow> ROWS = List.of(
			new SpikeRow("sharp", "steel", () -> MT.Steel, 5.0F, 2.5F, null, GT6Spikes::skeletonSlimeGolem),
			new SpikeRow("sharp", "titanium", () -> MT.Ti, 10.0F, 5.0F, null, null),
			new SpikeRow("steel", "blue_steel", () -> MT.BlueSteel, 8.0F, 4.0F, null, GT6Spikes::golem),
			new SpikeRow("steel", "red_steel", () -> MT.RedSteel, 8.0F, 4.0F, null, GT6Spikes::golem),
			new SpikeRow("super", "tungstensteel", () -> MT.TungstenSteel, 15.0F, 7.5F, null, null),
			new SpikeRow("super", "adamantium", () -> MT.Ad, 50.0F, 25.0F, null, null),
			new SpikeRow("metal", "copper", () -> MT.Cu, 20.0F, 10.0F, GT6Spikes::slime, GT6Spikes::skeletonGolem),
			new SpikeRow("metal", "lead", () -> MT.Pb, 20.0F, 10.0F, GT6Spikes::arthropod, GT6Spikes::skeletonSlimeGolem),
			new SpikeRow("fancy", "gold", () -> MT.Au, 20.0F, 10.0F, GT6Spikes::undead, GT6Spikes::slimeGolem),
			new SpikeRow("fancy", "silver", () -> MT.Ag, 20.0F, 10.0F, GT6Spikes::ender, GT6Spikes::skeletonSlimeGolem));

	/** The 10 blocks, ROWS order. */
	public static final List<RegistryObject<Block>> BLOCKS = registerBlocks();

	/** The 30 items (wall, block, falling per row), ROWS order. */
	public static final List<RegistryObject<Item>> ITEMS = registerItems();

	/** The blocks by snake ({@code spike_steel} form) — the datagen/tag/loot walk source. */
	public static final Map<String, RegistryObject<Block>> BLOCKS_BY_PATH = blocksByPath();

	/** The id of a material's wall item/block ({@code spike_steel} form) — the hardcoded
	 * snake (no MT deref at clinit, the datagen JVM trap). */
	public static String path(SpikeRow aRow) { return "spike_" + aRow.snake(); }

	/** The id of the omni ({@code spike_steel_block}) and falling ({@code _falling}) forms. */
	public static String omniPath(SpikeRow aRow) { return path(aRow) + "_block"; }
	public static String fallingPath(SpikeRow aRow) { return path(aRow) + "_falling"; }

	/** The material snake ({@code gt6.material.<snake>} convention, MaterialPrefixItem.snakeCase). */
	public static String snake(OreDictMaterial aMaterial) {
		return gregtech6.item.MaterialPrefixItem.snakeCase(aMaterial.mNameInternal);
	}

	/** The row's material (the post-init deref — datagen/runtime faces only). */
	public static OreDictMaterial mat(SpikeRow aRow) {
		return aRow.material().get();
	}

	private static List<RegistryObject<Block>> registerBlocks() {
		List<RegistryObject<Block>> rList = new ArrayList<>(ROWS.size());
		for (SpikeRow tRow : ROWS) {
			rList.add(BLOCKS_REG.register(path(tRow), () -> new GT6SpikeBlock(path(tRow), tRow.material().get(),
					tRow.wallDamage(), tRow.omniDamage(), tRow.prey(), tRow.immune(), GT6SpikeBlock.spikeProperties())));
		}
		return List.copyOf(rList);
	}

	private static List<RegistryObject<Item>> registerItems() {
		List<RegistryObject<Item>> rList = new ArrayList<>(ROWS.size() * 3);
		for (int i = 0; i < ROWS.size(); i++) {
			final int tIndex = i;
			SpikeRow tRow = ROWS.get(i);
			rList.add(ITEMS_REG.register(path(tRow),
					() -> new GT6SpikeBlock.WallItem(BLOCKS.get(tIndex).get(), new Item.Properties())));
			rList.add(ITEMS_REG.register(omniPath(tRow),
					() -> new GT6SpikeBlock.OmniItem(BLOCKS.get(tIndex).get(), new Item.Properties())));
			rList.add(ITEMS_REG.register(fallingPath(tRow),
					() -> new GT6SpikeBlock.FallingItem(BLOCKS.get(tIndex).get(), new Item.Properties())));
		}
		return List.copyOf(rList);
	}

	/** The item walk by id path ({@code spike_steel} / {@code _block} / {@code _falling} forms). */
	public static Item itemOfPath(String aPath) {
		for (RegistryObject<Item> tItem : ITEMS) {
			if (tItem.getId().getPath().equals(aPath)) return tItem.get();
		}
		throw new IllegalStateException("no spike item " + aPath);
	}

	private static Map<String, RegistryObject<Block>> blocksByPath() {
		Map<String, RegistryObject<Block>> rMap = new LinkedHashMap<>();
		for (int i = 0; i < ROWS.size(); i++) rMap.put(path(ROWS.get(i)), BLOCKS.get(i));
		return java.util.Collections.unmodifiableMap(rMap);
	}

	private GT6Spikes() {
	}

	/** FMLConstructModEvent = the first mod-bus lifecycle stage (the GT6ConcreteBlocks shape). */
	@net.minecraftforge.eventbus.api.SubscribeEvent
	public static void onModConstruct(net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent aEvent) {
		//? if forge {
		net.minecraftforge.eventbus.api.IEventBus tModBus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD.bus().get();
		//?} else {
		/*net.minecraftforge.eventbus.api.IEventBus tModBus =
				net.neoforged.fml.ModList.get().getModContainerById("gt6").orElseThrow().getEventBus();
		 *///?}
		BLOCKS_REG.register(tModBus);
		ITEMS_REG.register(tModBus);
	}

	/** The REDSTONE_BLOCKS join (the upstream tabRedstone face): the 30 items, registration order. */
	@net.minecraftforge.eventbus.api.SubscribeEvent
	public static void onBuildTabContents(BuildCreativeModeTabContentsEvent aEvent) {
		if (aEvent.getTabKey() == CreativeModeTabs.REDSTONE_BLOCKS) {
			for (RegistryObject<Item> tItem : ITEMS) {
				aEvent.accept(new ItemStack(tItem.get()));
			}
		}
	}

	// ---- the immunity arms (the 1.7.10 instanceof lists) ---------------------------------

	/** The Iron Golem (the 1.7.10 EntityIronGolem; 1.20.1 animal.IronGolem). */
	private static boolean golem(net.minecraft.world.entity.LivingEntity aE) {
		return aE instanceof net.minecraft.world.entity.animal.IronGolem;
	}

	private static boolean skeletonSlimeGolem(net.minecraft.world.entity.LivingEntity aE) {
		return golem(aE)
				|| aE instanceof net.minecraft.world.entity.monster.AbstractSkeleton
				|| aE instanceof net.minecraft.world.entity.monster.Slime;
	}

	private static boolean skeletonGolem(net.minecraft.world.entity.LivingEntity aE) {
		return golem(aE) || aE instanceof net.minecraft.world.entity.monster.AbstractSkeleton;
	}

	private static boolean slimeGolem(net.minecraft.world.entity.LivingEntity aE) {
		return golem(aE) || aE instanceof net.minecraft.world.entity.monster.Slime;
	}

	/** The prey arms. */
	private static boolean slime(net.minecraft.world.entity.LivingEntity aE) {
		return aE instanceof net.minecraft.world.entity.monster.Slime;
	}

	private static boolean arthropod(net.minecraft.world.entity.LivingEntity aE) {
		//? if forge {
		return aE.getMobType() == net.minecraft.world.entity.MobType.ARTHROPOD;
		//?} else {
		/*// 21.1: MobType died in the 1.20.5 tag refactor (the ReactorRadioactivity fold)
		return aE.getType().is(net.minecraft.tags.EntityTypeTags.ARTHROPOD);
		 *///?}
	}

	private static boolean undead(net.minecraft.world.entity.LivingEntity aE) {
		//? if forge {
		return aE.getMobType() == net.minecraft.world.entity.MobType.UNDEAD;
		//?} else {
		/*return aE.getType().is(net.minecraft.tags.EntityTypeTags.UNDEAD);
		 *///?}
	}

	/** The ender arm — the port carries no were-creature registry, the EnderMan face only (declared). */
	private static boolean ender(net.minecraft.world.entity.LivingEntity aE) {
		return aE instanceof net.minecraft.world.entity.monster.EnderMan;
	}
}
