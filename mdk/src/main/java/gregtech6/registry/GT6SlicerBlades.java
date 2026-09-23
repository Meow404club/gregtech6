package gregtech6.registry;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import gregtech6.GT6Mod;

/**
 * The GT6 slicer-blade registration home — task p35-slicer-row-domain, the vanilla-face
 * MINIMAL subset of the upstream {@code Shape_Slicer_*} domain (the content-unit
 * completeness ruling: the blades and their RM rows land on the SAME card — the
 * GT6ExtruderMolds shape; the Slicer rows are {@code addRecipe2(item, blade, out)} and the
 * map's {@code mMinimalInputItems == 2} makes the blade slot a HARD prerequisite,
 * TileEntityBasicMachine.java:626). Card-owned self-contained
 * {@code @EventBusSubscriber(MOD)} DeferredRegister attached from the construct event (the
 * GT6ExtruderMolds shape verbatim; GT6Mod.java / GTModBusListener.java stay untouched).
 *
 * <p>The subset — 2 items of the upstream 8-blade census (MultiItemTechnological.java
 * :362-372, everything else POOLED: the Empty frame + the Flat blade with the CUT fur row,
 * the Eigths/Quarters blades with the pooled melon/food faces):
 * <ul>
 * <li><b>the split blade</b> — upstream {@code IL.Shape_Slicer_Split} meta 10905 "Slicer
 *     Blades (Split)" (MultiItemTechnological.java:370); the shaping tool of the four
 *     leather-armor rows (Loader_Recipes_Vanilla.java:638-641).</li>
 * <li><b>the grid blade</b> — upstream {@code IL.Shape_Slicer_Grid} meta 10902 "Slicer
 *     Blades (Grid)" (:367); the shaping tool of the paper row (Loader_Recipes_Other.java
 *     :420, paper → 9 tiny paper plates).</li>
 * </ul>
 *
 * <p>Id flattening (the GT6ExtruderMolds ruling): upstream ids were meta ids on the
 * MultiItemTechnological meta item (10902/10905); the port flattens to one id per blade,
 * snake of the upstream name ({@code "Slicer Blades (Split)"} →
 * {@code shape_slicer_split}). The blades are PLAIN items — zero shaping behaviour on the
 * item itself, so a fresh {@code Item} carries the whole declared behaviour.
 *
 * <p><b>The not-consumable face</b> (the GT6ExtruderMolds archaeology, remember id478):
 * upstream marks every blade recipe input with STACK SIZE 0 (RM.java:96-domain rows
 * {@code IL.Shape_Slicer_*.get(0)}) — size-0 is not portable to 1.20.1, so the port
 * carries the never-consumed net effect through {@code Recipe.sNotConsumable}, whose
 * production default consults {@link #isBlade}. The identity face is ITEM-identity over
 * the two registered blades behind the {@link #sBladeTest} test seam (the offline JVM has
 * no bound registry — the GT6ExtruderMolds sMoldTest two-contract rule; no family tag was
 * minted: the predicate is the tag's only consumer and a tag would add a datagen face for
 * zero readers — upgrade path: mint {@code gt6:slicer_blades} when a second consumer
 * appears).
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6SlicerBlades {

	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gt6");

	/**
	 * The grid blade — id {@code gt6:shape_slicer_grid} (upstream meta 10902, the RM.Slicer
	 * paper row's shaping tool, Loader_Recipes_Other.java:420).
	 */
	public static final RegistryObject<Item> SHAPE_SLICER_GRID = ITEMS.register("shape_slicer_grid",
			() -> new Item(new Item.Properties()));

	/**
	 * The split blade — id {@code gt6:shape_slicer_split} (upstream meta 10905, the RM.Slicer
	 * leather-armor rows' shaping tool, Loader_Recipes_Vanilla.java:638-641).
	 */
	public static final RegistryObject<Item> SHAPE_SLICER_SPLIT = ITEMS.register("shape_slicer_split",
			() -> new Item(new Item.Properties()));

	/** The row0 blade set in upstream meta order (:367 grid before :370 split). */
	public static final List<RegistryObject<Item>> BLADES = List.of(SHAPE_SLICER_GRID, SHAPE_SLICER_SPLIT);

	/**
	 * The blade-identity seam (production default = item identity over the two registered
	 * blades). The offline JVM binds no registry (every bound-check reads false), so the
	 * offline tests swap this with a fixture predicate — the same two-contract rule: the
	 * stub answers the negatives exactly as production does, and the positives the stub
	 * grants are exactly what the RCON live chain re-proves with the real registry. The
	 * bound check is leg-split (the stonecutter RegistryObject→DeferredHolder swap renames
	 * the probe: {@code isPresent()} on 1.20.1 vs {@code isBound()} on 21.1 — the
	 * GT6JuicerRegistrationTest holder-semantics lesson).
	 */
	public static java.util.function.Predicate<ItemStack> sBladeTest = aStack -> aStack != null && !aStack.isEmpty()
			&& BLADES.stream().anyMatch(tBlade -> isBound(tBlade) && aStack.is(tBlade.get()));

	/** The registry-bind probe, leg-split over the swapped holder type. */
	private static boolean isBound(RegistryObject<Item> aBlade) {
		//? if forge {
		return aBlade.isPresent();
		//?} else {
		/*return aBlade.isBound();
		*///?}
	}

	/** The blade identity face {@code Recipe.sNotConsumable} consults — null-safe. */
	public static boolean isBlade(@Nullable ItemStack aStack) {
		return sBladeTest.test(aStack);
	}

	private GT6SlicerBlades() {
	}

	/** FMLConstructModEvent = the first mod-bus lifecycle stage (GT6ExtruderMolds.onModConstruct shape). */
	@net.minecraftforge.eventbus.api.SubscribeEvent
	public static void onModConstruct(net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent aEvent) {
		//? if forge {
		IEventBus tModBus = Mod.EventBusSubscriber.Bus.MOD.bus().get();
		//?} else {
		/*IEventBus tModBus = net.neoforged.fml.ModList.get().getModContainerById("gt6").orElseThrow().getEventBus();
		 *///?}
		ITEMS.register(tModBus);
	}

	/** Registration smoke evidence (the GT6ExtruderMolds onCommonSetup log shape). */
	@net.minecraftforge.eventbus.api.SubscribeEvent
	public static void onCommonSetup(FMLCommonSetupEvent aEvent) {
		aEvent.enqueueWork(() -> {
			GT6Mod.LOGGER.info("GT6 slicer blades registered: {} blades (the vanilla-face row0 subset: grid + split)", BLADES.size());
			// the registry lookup (not the field name) makes this line real registration
			// evidence — an unregistered blade would throw here and fail the runServer gate.
			for (RegistryObject<Item> tBlade : BLADES) {
				GT6Mod.LOGGER.info("GT6 slicer blade registered: {}", net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(tBlade.get()));
			}
		});
	}
}
