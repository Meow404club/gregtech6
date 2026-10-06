package gregtech6.registry;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import gregtech6.GT6Mod;
import gregtech6.covers.GT6Covers;

/**
 * The GT6 robot-component registration home — task robotics-chain. The four compact
 * component ladders + the Autocrafter tip/token pair, upstream metas on the two
 * multiitems flattened to one id per item (the GT6FoodCans id-flattening ruling, snake
 * of the upstream registration name):
 *
 * <ul>
 * <li>{@link #MOTORS} — metas 12000-12009 "Compact Electric Motor (VN)"</li>
 * <li>{@link #CONVEYERS} — metas 12040-12049 "Compact Electric Conveyor (VN)"</li>
 * <li>{@link #PISTONS} — metas 12060-12069 "Compact Electric Piston (VN)"</li>
 * <li>{@link #ROBOT_ARMS} — metas 12080-12089 "Compact Robot Arm (VN)"</li>
 * </ul>
 *
 * <p>all the {@code for (int i = 0; i < 10; i++)} walk of MultiItemTechnological.java:45-54
 * over the CS.VN[0..9] tier words (CS.java:154, shared with {@link GT6Covers#TIER_NAMES}).
 * The PUMPS/FIELD_GENERATORS/EMITTERS/SENSORS neighbours of the same upstream loop stay
 * in their own cards' scopes (the item-pool-small dispatch rows); the cover behaviours the
 * upstream conveyor/robot-arm registrations carry ({@code new CoverConveyor(512>>i)} /
 * {@code new CoverRobotArm(512>>i)}, :51/:53) ride the covers domain — the items here are
 * the plain item faces the crafting/Boxinator rows consume.
 *
 * <p>MultiItemRandomTools.java:470-479 — the 10 {@link #ROBOT_TIPS} ("Robot Arm Wrench
 * Tip" .. "Robot Arm File Tip", metas 8000-8009) and :492-501 — the 10 {@link #TOOL_TOKENS}
 * ("Single Use Wrench" .. "Single Use File", metas 8500-8509). All are PLAIN items — zero
 * shaping behaviour on the item itself (the GT6ExtruderMolds declared-behaviour ruling).
 *
 * <p><b>Tab rehome (task disposable-tools-tab-rehome, the user ruling 2026-10-06)</b>: the
 * 10 single-use tools ride the {@link GT6Tools#TOOLS_TAB} ({@code gt6:tools}, the TAB_TABLE
 * tail), NOT the MACHINES_TAB pool. Upstream the tokens' multiitem already carries its own
 * tool-domain creative tab — {@code setCreativeTab(new CreativeTab(..., "GregTech:
 * Equipment", ...))} (MultiItemRandomTools.java:59, the registration rows :492-501) — so
 * the machine-tab pooling was the deviation; the port's tools tab is that Equipment
 * domain's home. The tips (Infinitely usable, :470-479) and the four compact ladders
 * (MultiItemTechnological.java:43 "GregTech: Technology") keep the machines-tab pool —
 * only the disposable family was ruled.
 *
 * <p><b>The never-consumed tip face</b> (the id478 archaeology shape): the Boxinator rows
 * register their tip input at stack size 0 (MultiItemRandomTools.java:503-512,
 * {@code IL.Robot_Tip_*.get(0)}) — size-0 is not portable to 1.20.1, so the port carries
 * the never-consumed net effect through {@code Recipe.sNotConsumable} (fifth disjunct,
 * {@link #isRobotTip}), whose production default consults the {@link #sTipTest} test seam
 * (the offline JVM resolves no registry bindings — the GT6SlicerBlades.sBladeTest
 * precedent; the live registry binding is the consume path's real proof, and the offline
 * consume-path e2e keeps swapping the seam the same way it swaps the mold face).
 *
 * <p>KJS surface: REGISTRATION face only, deferred to the KJS binding card (the
 * GT6Bottles declaration form); the item models + textures are datapack-domain, naturally
 * moddable.
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6Robotics {

	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gt6");

	/**
	 * One upstream registration row: the 1.7.10 meta, the port id, the upstream
	 * registration-row name verbatim, the desc tooltip verbatim ({@code ""} = no tooltip
	 * key on either locale, the GT6BakeFoods empty-desc ruling).
	 */
	public record RobotRow(int meta, String id, String enName, String enTooltip) {

		/** The item lang key. */
		public String langKey() {
			return "item.gt6." + id;
		}

		/** The desc tooltip lang key, null on the {@code ""} rows (the raw-key guard). */
		public String tooltipKey() {
			return enTooltip.isEmpty() ? null : "item.gt6." + id + ".tooltip";
		}
	}

	/** The upstream tier words CS.VN[0..9] (CS.java:154) — the id suffix of every ladder row. */
	public static final String[] TIERS = GT6Covers.TIER_NAMES;

	/** The tip/token kind words in upstream registration order (MultiItemRandomTools.java:470-479). */
	public static final String[] KINDS = {"Wrench", "Screwdriver", "Saw", "Hammer", "Cutter", "Chisel", "Rubber", "Blade", "Drill", "File"};

	/** The snake suffix for a tier word (the LuV/PUV1 forms snake lower directly). */
	private static String tierSnake(int aTier) {
		return TIERS[aTier].toLowerCase(java.util.Locale.ROOT);
	}

	/** The kind snake (the single-word upstream kind words). */
	private static String kindSnake(int aKind) {
		return KINDS[aKind].toLowerCase(java.util.Locale.ROOT);
	}

	/** The four ladder row tables, upstream meta order (MultiItemTechnological.java:49/:51/:52/:53). */
	public static final List<RobotRow> MOTOR_ROWS = ladderRows(12000, "compact_electric_motor", "Compact Electric Motor");
	public static final List<RobotRow> CONVEYER_ROWS = ladderRows(12040, "compact_electric_conveyor", "Compact Electric Conveyor");
	public static final List<RobotRow> PISTON_ROWS = ladderRows(12060, "compact_electric_piston", "Compact Electric Piston");
	public static final List<RobotRow> ARM_ROWS = ladderRows(12080, "compact_robot_arm", "Compact Robot Arm");

	private static List<RobotRow> ladderRows(int aBaseMeta, String aPrefix, String aName) {
		List<RobotRow> tRows = new ArrayList<>(10);
		for (int i = 0; i < 10; i++) {
			tRows.add(new RobotRow(aBaseMeta + i, aPrefix + "_" + tierSnake(i), aName + " (" + TIERS[i] + ")", ""));
		}
		return List.copyOf(tRows);
	}

	/** The tip rows, upstream meta order (MultiItemRandomTools.java:470-479, metas 8000-8009). */
	public static final List<RobotRow> TIP_ROWS = tipTokenRows(8000, "robot_arm_%s_tip", "Robot Arm %s Tip", "Infinitely usable inside an Autocrafter");

	/** The token rows, upstream meta order (MultiItemRandomTools.java:492-501, metas 8500-8509). */
	public static final List<RobotRow> TOKEN_ROWS = tipTokenRows(8500, "single_use_%s", "Single Use %s", "Non-Functional-Tool for Crafting only");

	private static List<RobotRow> tipTokenRows(int aBaseMeta, String aIdForm, String aNameForm, String aTooltip) {
		List<RobotRow> tRows = new ArrayList<>(10);
		for (int i = 0; i < 10; i++) {
			tRows.add(new RobotRow(aBaseMeta + i, String.format(aIdForm, kindSnake(i)), String.format(aNameForm, KINDS[i]), aTooltip));
		}
		return List.copyOf(tRows);
	}

	/**
	 * The 60 rows in upstream identity order (the four ladders in registration-loop column
	 * order, then the tips, then the tokens). Drives the models band, the lang walks and
	 * the tab.
	 */
	public static final List<RobotRow> ROWS;
	static {
		List<RobotRow> tRows = new ArrayList<>(60);
		tRows.addAll(MOTOR_ROWS);
		tRows.addAll(CONVEYER_ROWS);
		tRows.addAll(PISTON_ROWS);
		tRows.addAll(ARM_ROWS);
		tRows.addAll(TIP_ROWS);
		tRows.addAll(TOKEN_ROWS);
		ROWS = List.copyOf(tRows);
	}

	/** The four ladders + the two families, registration order (the census walk face). */
	public static final List<RegistryObject<Item>> MOTORS = register(MOTOR_ROWS);
	public static final List<RegistryObject<Item>> CONVEYERS = register(CONVEYER_ROWS);
	public static final List<RegistryObject<Item>> PISTONS = register(PISTON_ROWS);
	public static final List<RegistryObject<Item>> ROBOT_ARMS = register(ARM_ROWS);
	public static final List<RegistryObject<Item>> ROBOT_TIPS = register(TIP_ROWS);
	public static final List<RegistryObject<Item>> TOOL_TOKENS = register(TOKEN_ROWS);

	private static List<RegistryObject<Item>> register(List<RobotRow> aRows) {
		List<RegistryObject<Item>> tHolders = new ArrayList<>(aRows.size());
		for (RobotRow tRow : aRows) {
			tHolders.add(ITEMS.register(tRow.id(), () -> new Item(new Item.Properties())));
		}
		return List.copyOf(tHolders);
	}

	/** The 60 holders in {@link #ROWS} order (the aggregate census walk). */
	public static final List<RegistryObject<Item>> ALL;
	static {
		List<RegistryObject<Item>> tAll = new ArrayList<>(60);
		tAll.addAll(MOTORS);
		tAll.addAll(CONVEYERS);
		tAll.addAll(PISTONS);
		tAll.addAll(ROBOT_ARMS);
		tAll.addAll(ROBOT_TIPS);
		tAll.addAll(TOOL_TOKENS);
		ALL = List.copyOf(tAll);
	}

	/**
	 * The 50 MACHINES_TAB members — {@link #ALL} minus the 10 {@link #TOOL_TOKENS} (task
	 * disposable-tools-tab-rehome: the single-use tools rehome to {@link GT6Tools#TOOLS_TAB},
	 * the upstream Equipment tab face, MultiItemRandomTools.java:59/:492-501). The join
	 * handler below walks THIS list — the data seam the offline census pins.
	 */
	public static final List<RegistryObject<Item>> MACHINE_TAB_ITEMS;
	static {
		List<RegistryObject<Item>> tMachines = new ArrayList<>(50);
		tMachines.addAll(MOTORS);
		tMachines.addAll(CONVEYERS);
		tMachines.addAll(PISTONS);
		tMachines.addAll(ROBOT_ARMS);
		tMachines.addAll(ROBOT_TIPS);
		MACHINE_TAB_ITEMS = List.copyOf(tMachines);
	}

	/**
	 * The tip-identity seam (production default = item identity over {@link #ROBOT_TIPS}).
	 * The offline JVM binds no registry (every holder unbound → the guard answers false),
	 * so the offline consume-path e2e swaps this with a fixture predicate — the same
	 * two-contract rule as the mold/blade seams: the stub answers the negatives exactly as
	 * production does, and the positives the stub grants are exactly what the live registry
	 * binding re-proves.
	 */
	public static java.util.function.Predicate<ItemStack> sTipTest = aStack -> aStack != null && !aStack.isEmpty()
			&& ROBOT_TIPS.stream().anyMatch(tTip -> isBound(tTip) && aStack.is(tTip.get()));

	/** The registry-bind probe, leg-split over the swapped holder type (the GT6SlicerBlades.isBound form). */
	private static boolean isBound(RegistryObject<Item> aTip) {
		//? if forge {
		return aTip.isPresent();
		//?} else {
		/*return aTip.isBound();
		 *///?}
	}

	/** The tip identity face {@code Recipe.sNotConsumable} consults — null-safe. */
	public static boolean isRobotTip(ItemStack aStack) {
		return sTipTest.test(aStack);
	}

	private GT6Robotics() {
	}

	/** FMLConstructModEvent = the first mod-bus lifecycle stage (the GT6ExtruderMolds shape). */
	@SubscribeEvent
	public static void onModConstruct(FMLConstructModEvent aEvent) {
		//? if forge {
		IEventBus tModBus = Mod.EventBusSubscriber.Bus.MOD.bus().get();
		//?} else {
		/*IEventBus tModBus = net.neoforged.fml.ModList.get().getModContainerById("gt6").orElseThrow().getEventBus();
		 *///?}
		ITEMS.register(tModBus);
	}

	/** Registration smoke evidence (the GT6ExtruderMolds onCommonSetup log shape). */
	@SubscribeEvent
	public static void onCommonSetup(FMLCommonSetupEvent aEvent) {
		aEvent.enqueueWork(() -> {
			GT6Mod.LOGGER.info("GT6 robot components registered: {} items (4 compact ladders x 10 + 10 tips + 10 tokens)", ALL.size());
			// the registry lookup (not the field name) makes this line real registration
			// evidence — an unregistered item would throw here and fail the runServer gate.
			for (RegistryObject<Item> tItem : ALL) {
				GT6Mod.LOGGER.info("GT6 robot component registered: {}", net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(tItem.get()));
			}
		});
	}

	/**
	 * The MACHINES_TAB join (the GT6ExtruderMolds verbatim form, delivered by the
	 * class-level MOD-bus {@code @Mod.EventBusSubscriber} at the class head). Upstream the
	 * components ride the GT technology tab as multiitem metas; the port pools them with
	 * the machines tab — the ten single-use tools EXCEPTED (task disposable-tools-tab-rehome:
	 * they rehome to the tools tab, the upstream Equipment tab, MultiItemRandomTools.java:59).
	 * The walk covers {@link #MACHINE_TAB_ITEMS} — registered-but-tab-less is invisible in
	 * both the creative menu and JEI (the issue #10 lesson).
	 */
	@SubscribeEvent
	public static void onBuildTabContents(net.minecraftforge.event.BuildCreativeModeTabContentsEvent aEvent) {
		if (aEvent.getTabKey().location().equals(GTMachines.MACHINES_TAB.getId())) {
			for (RegistryObject<Item> tItem : MACHINE_TAB_ITEMS) {
				aEvent.accept(new ItemStack(tItem.get()));
			}
		}
	}
}
