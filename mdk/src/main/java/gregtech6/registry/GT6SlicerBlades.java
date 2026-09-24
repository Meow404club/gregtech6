package gregtech6.registry;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
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
 * <p>The subset — 2 RM-row0 items of the upstream 8-blade census (MultiItemTechnological.java
 * :362-372; the pooled melon/food faces stay pooled, the never pool): the Empty frame +
 * the Flat blade with the CUT fur row, the Eigths/Quarters blades with the pooled melon/food
 * faces. Task p36-recipes-obtainability (ruling B, the obtainability domain = items +
 * recipes inseparable) completes the census: the frame and the five remaining blade forms
 * register — same plain-item pattern — because the eight :364+:374-380 crafting rows need
 * all eight outputs and the frame as the shared 'O' ingredient. The full census walks in
 * {@link #ALL}; {@link #BLADES} stays the RM row0 pair (the {@link #sBladeTest} face):
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

	/**
	 * The blade frame — id {@code gt6:shape_slicer_empty} (upstream meta 10900 "Slicer Blade
	 * Frame", MultiItemTechnological.java:362). NOT a blade: it is the CRAFTING INGREDIENT of
	 * every blade row (:374-380 'O' column), consumed — it never rides {@link #BLADES}.
	 */
	public static final RegistryObject<Item> SHAPE_SLICER_EMPTY = ITEMS.register("shape_slicer_empty",
			() -> new Item(new Item.Properties()));

	/** The flat blade — id {@code gt6:shape_slicer_flat} (upstream meta 10901, MultiItemTechnological.java:366). */
	public static final RegistryObject<Item> SHAPE_SLICER_FLAT = ITEMS.register("shape_slicer_flat",
			() -> new Item(new Item.Properties()));

	/** The eigths blade — id {@code gt6:shape_slicer_eigths} (upstream meta 10903, :368 — the upstream spelling verbatim). */
	public static final RegistryObject<Item> SHAPE_SLICER_EIGHTS = ITEMS.register("shape_slicer_eigths",
			() -> new Item(new Item.Properties()));

	/** The hollow-eigths blade — id {@code gt6:shape_slicer_eigths_hollow} (upstream meta 10904, :369). */
	public static final RegistryObject<Item> SHAPE_SLICER_EIGHTS_HOLLOW = ITEMS.register("shape_slicer_eigths_hollow",
			() -> new Item(new Item.Properties()));

	/** The quarters blade — id {@code gt6:shape_slicer_quarters} (upstream meta 10906, :371). */
	public static final RegistryObject<Item> SHAPE_SLICER_QUARTERS = ITEMS.register("shape_slicer_quarters",
			() -> new Item(new Item.Properties()));

	/** The hollow-quarters blade — id {@code gt6:shape_slicer_quarters_hollow} (upstream meta 10907, :372). */
	public static final RegistryObject<Item> SHAPE_SLICER_QUARTERS_HOLLOW = ITEMS.register("shape_slicer_quarters_hollow",
			() -> new Item(new Item.Properties()));

	/** The row0 blade set in upstream meta order (:367 grid before :370 split). */
	public static final List<RegistryObject<Item>> BLADES = List.of(SHAPE_SLICER_GRID, SHAPE_SLICER_SPLIT);

	/**
	 * The full EIGHT-item census in upstream meta order (:362 frame, :366-:372 the seven
	 * blades) — the crafting-row and lang walk; the RM row0 subset {@link #BLADES} stays
	 * the {@link #sBladeTest} face (the six p36 blades carry no RM rows — the never pool
	 * holds, task p36-recipes-obtainability ruling B).
	 */
	public static final List<RegistryObject<Item>> ALL = List.of(SHAPE_SLICER_EMPTY, SHAPE_SLICER_FLAT,
			SHAPE_SLICER_GRID, SHAPE_SLICER_EIGHTS, SHAPE_SLICER_EIGHTS_HOLLOW, SHAPE_SLICER_SPLIT,
			SHAPE_SLICER_QUARTERS, SHAPE_SLICER_QUARTERS_HOLLOW);

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

	/**
	 * The MACHINES_TAB join (task p38-tabfix-c-misc — the census zero-tab adjudication;
	 * the GT6BurningBoxes.onBuildTabContents verbatim form, delivered by the class-level
	 * MOD-bus {@code @Mod.EventBusSubscriber} at the class head). Upstream the blades are
	 * MultiItemTechnological metas riding the GT tab list; the port pools them with the
	 * machines tab. The walk covers the FULL eight-item {@link #ALL} census — the p36
	 * obtainability ruling: the frame and the five never-pool blades are crafting-row
	 * outputs/the shared 'O' ingredient, not tab-less debris (the census's "3" predates
	 * the p36 completion; the erratum is declared in the census test).
	 */
	@net.minecraftforge.eventbus.api.SubscribeEvent
	public static void onBuildTabContents(BuildCreativeModeTabContentsEvent aEvent) {
		if (aEvent.getTabKey().location().equals(GTMachines.MACHINES_TAB.getId())) {
			for (RegistryObject<Item> tBlade : ALL) {
				aEvent.accept(new ItemStack(tBlade.get()));
			}
		}
	}
}
