package gregtech6.tileentity.machines;

import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

import gregapi.data.OP;
import gregtech6.gui.machines.GTBasicMachineMenu;
import gregtech6.gui.machines.GTBasicMachinesMenus;
import gregtech6.registry.GTMaterialItems;
import gregtech6.registry.GTMachines;
import gregtech6.registry.GT6SprayCans;
import gregtech6.tileentity.IPaintableTE;
import gregtech6.tileentity.TileEntityBase01Root;

/**
 * {@code /gt6machine} — the machine-family acceptance command (task p7-basicmachine-family
 * ⑥, RCON-drivable, GTOvenCommand :63-98 template, console-safe throughout; extended by
 * task p8-machine-tiers-doinject ⑤ and task p14-dryer-family): one literal per registered
 * machine ({@code shredder|crusher|lathe|dryer} × {@code [t2|t3|t4]}, 16 rows) carrying
 * the subcommands
 *
 * <ul>
 * <li>{@code place [<pos>]} — setBlock a fresh machine (FACING north);</li>
 * <li>{@code input [<count>] [<pos>]} — push the default feed into the input slot (default
 *     8): shredder = cobblestone (Loader_Recipes_Vanilla.java:692), crusher = the first
 *     resolvable {@code gt6:gem_*} of the gem chain (Loader_Recipes_Handlers.java:72,
 *     gem → gemFlawed x2), lathe = stone (:524), dryer = bricks (the frame column stub),
 *     distillery = the Integrated Circuit at configuration 0 (the ST.tag(0) selector the
 *     poured :534-541 rows carry, task p16-distillery-family);</li>
 * <li>{@code run <ticks> [<pos>]} — drives the BE dispatcher tick by tick and samples the
 *     live menu ContainerData value ({@link GTBasicMachineMenu#computeProgressValue()}): the
 *     acceptance asserts all three states observed (progress &gt;0 &lt;32767, done 32767 via
 *     mSuccessful, idle -1) plus the output slots filling. The real server ticker keeps
 *     ticking alongside — this only accelerates the same dispatcher. Needs the fake-source
 *     seam on ({@code fakesource on}): the shipped default is grid-fed via doInject;</li>
 * <li>{@code inject <ticks> [<size>] [<pos>]} — the FALSE-regime test rig: one loop
 *     iteration = one direct {@code doInject(mEnergyTypeAccepted, side, size, 1, true)}
 *     (size defaults to the machine's mInputMax; a NEGATIVE size is the AC half-cycle,
 *     the upstream EngineSteam :146 piston-phase ±alternation) followed by one dispatcher
 *     tick, so injection and consumption land in the same tick. The KU pulse acceptance
 *     drives the whole rig live: a positive train never delivers, the negative pulse
 *     delivers on its transition tick. size 0 is refused (the Root :717 aSize != 0 gate
 *     shape — a 0 packet would divide by zero in the verbatim :503 math);</li>
 * <li>{@code check [<pos>]} — state report: progress/maxprogress/energy/minenergy/the
 *     energy three values (minIn/recIn/maxIn)/state latch/active/running/parallel + the
 *     slot contents (a menu-less carrier reports {@code data=-2} — the trio is a menu
 *     surface);</li>
 * <li>{@code fluid fill <side> <fluid> <amount> [<pos>]} / {@code fluid draw <side>
 *     <amount> [<pos>]} / {@code fluid stat [<pos>]} (task p14-dryer-family) — the
 *     side-gated FLUID_HANDLER driver over the p14-machine-fluid-face carriers: the
 *     rotated row masks answer fill/draw (0 = REJECTED is a legitimate verdict) and stat
 *     dumps the tank census plus the live fluid/energy face lists (task p14-dryer-family).</li>
 * <li>{@code paint <pos> <dye0-15|none>} / {@code unpaint <pos>} (task
 *     p21-paintable-storage-sync, ADR 2026-09-07-p21-paintable-rulings ruling 1) — the
 *     spray write-point arm over the SAME server {@link IPaintableTE} API the offline
 *     tests drive: {@code <dye>} is the GT6 dye index 0=Black..15=White (the upstream
 *     CS.DYES_INT table values) routed through {@link IPaintableTE#mixPaint} (the
 *     recolourBlock routing of TileEntityBase04MultiTileEntities.java:227-235, so an
 *     already-painted machine MIXES by channel average), {@code none} /
 *     {@code unpaint} clear the paint. The report reads the colour back
 *     ({@code RGB #old->#new painted=..}) — the RCON chain pins it.</li>
 * </ul>
 *
 * <p>Plus the regime switch {@code /gt6machine fakesource on|off|stat} — flips
 * {@link TileEntityBasicMachine#ENERGY_FAKE_SOURCE} at runtime (task
 * p11-rotor-source-flip: {@code off} IS the shipped default — grid-fed via doInject with
 * the full upstream :815 semantics, the RU/KU machines fed by the /gt6energy source rig;
 * {@code on} re-arms the retired A-tier seam with the :815 alternating arm suspended).
 */
@Mod.EventBusSubscriber(modid = "gt6")
public final class GTMachineCommand {

	private static final Logger LOGGER = LogUtils.getLogger();

	/** The acceptance feed size (the oven command default). */
	private static final int DEFAULT_INPUT_COUNT = 8;

	/** The GT6 side the rig injects through (doInject ignores the side — the :511 mask is the side-gated IO pool item). */
	private static final byte INJECT_SIDE = 2;

	/**
	 * The upstream CS.DYES_INT dye table (CS.java:441-457 DYE_* short[] through
	 * UT.Code.getRGBInt :1580-1582), index = the GT6 DYE_INDEX (CS.java:443-458,
	 * 0=Black..15=White). The direct-store equivalent of the upstream spray route:
	 * {@code ~mColor&15} + DYES_INT_INVERTED composes to exactly this value
	 * (ADR ruling 3 — "the colour you spray is the colour you get").
	 */
	private static final int[] DYES_INT = {
			0x202020, // Black
			0xFF0000, // Red
			0x00FF00, // Green
			0x604000, // Brown
			0x0000FF, // Blue
			0x800080, // Purple
			0x00FFFF, // Cyan
			0xC0C0C0, // Light Gray
			0x808080, // Gray
			0xFFC0C0, // Pink
			0x80FF80, // Lime
			0xFFFF00, // Yellow
			0x8080FF, // Light Blue
			0xFF00FF, // Magenta
			0xFF8000, // Orange
			0xFFFFFF, // White
	};

	/** The upstream DYE_NAMES (CS.java:459), same index order as {@link #DYES_INT}. */
	private static final String[] DYE_NAMES = {"Black", "Red", "Green", "Brown", "Blue", "Purple", "Cyan", "Light Gray", "Gray", "Pink", "Lime", "Yellow", "Light Blue", "Magenta", "Orange", "White"};

	private GTMachineCommand() {
	}

	@SubscribeEvent
	public static void onRegisterCommands(RegisterCommandsEvent event) {
		LiteralArgumentBuilder<CommandSourceStack> tMachine = Commands.literal("gt6machine")
			.requires(source -> source.hasPermission(2))
			.then(fakesource())
			.then(paintArm())
			.then(unpaintArm());
		// task p8-machine-tiers-doinject ⑤(a): the t2/t3/t4 selector variants — one literal
		// per registered block, the T1 feeds reused per family (same recipe chains).
		tMachine.then(machine("shredder", GTMachines.SHREDDER, () -> Items.COBBLESTONE)) // Loader_Recipes_Vanilla.java:692
			.then(machine("shredder_t2", GTMachines.SHREDDER_T2, () -> Items.COBBLESTONE))
			.then(machine("shredder_t3", GTMachines.SHREDDER_T3, () -> Items.COBBLESTONE))
			.then(machine("shredder_t4", GTMachines.SHREDDER_T4, () -> Items.COBBLESTONE))
			.then(machine("crusher", GTMachines.CRUSHER, GTMachineCommand::firstGemChainGem)) // Loader_Recipes_Handlers.java:72
			.then(machine("crusher_t2", GTMachines.CRUSHER_T2, GTMachineCommand::firstGemChainGem))
			.then(machine("crusher_t3", GTMachines.CRUSHER_T3, GTMachineCommand::firstGemChainGem))
			.then(machine("crusher_t4", GTMachines.CRUSHER_T4, GTMachineCommand::firstGemChainGem))
			.then(machine("lathe", GTMachines.LATHE, () -> net.minecraft.world.level.block.Blocks.STONE.asItem())) // Loader_Recipes_Vanilla.java:524
			.then(machine("lathe_t2", GTMachines.LATHE_T2, () -> net.minecraft.world.level.block.Blocks.STONE.asItem()))
			.then(machine("lathe_t3", GTMachines.LATHE_T3, () -> net.minecraft.world.level.block.Blocks.STONE.asItem()))
			.then(machine("lathe_t4", GTMachines.LATHE_T4, () -> net.minecraft.world.level.block.Blocks.STONE.asItem()))
			// task p14-dryer-family: the dryer ladder — the input feed is a minimal STUB
			// (the DRYING map is declared-empty until the W3 pour, so no feed can ever
			// start a process; bricks mirror the upstream frame column 'B' = brick_block)
			.then(machine("dryer", GTMachines.DRYER_BLOCKS_BY_PATH.get("dryer"), () -> Items.BRICKS))
			.then(machine("dryer_t2", GTMachines.DRYER_BLOCKS_BY_PATH.get("dryer_t2"), () -> Items.BRICKS))
			.then(machine("dryer_t3", GTMachines.DRYER_BLOCKS_BY_PATH.get("dryer_t3"), () -> Items.BRICKS))
			.then(machine("dryer_t4", GTMachines.DRYER_BLOCKS_BY_PATH.get("dryer_t4"), () -> Items.BRICKS))
			// task p16-distillery-family: the distillery ladder — the input feed is the
			// Integrated Circuit (the ST.tag(0) selector every poured :534-541 row carries;
			// input() routes the feed through feedStack() so the stack lands with its
			// Damage:0 configuration tag)
			.then(machine("distillery", GTMachines.DISTILLERY_BLOCKS_BY_PATH.get("distillery"), () -> gregtech6.item.GT6Circuits.INTEGRATED_CIRCUIT.get()))
			.then(machine("distillery_t2", GTMachines.DISTILLERY_BLOCKS_BY_PATH.get("distillery_t2"), () -> gregtech6.item.GT6Circuits.INTEGRATED_CIRCUIT.get()))
			.then(machine("distillery_t3", GTMachines.DISTILLERY_BLOCKS_BY_PATH.get("distillery_t3"), () -> gregtech6.item.GT6Circuits.INTEGRATED_CIRCUIT.get()))
			.then(machine("distillery_t4", GTMachines.DISTILLERY_BLOCKS_BY_PATH.get("distillery_t4"), () -> gregtech6.item.GT6Circuits.INTEGRATED_CIRCUIT.get()))
			// task p24-canner-machine: the canner ladder — the input feed is the empty spray
			// can (gt6:spray_can_empty, the MultiItemRandomTools.java:246 refill row's item
			// input; the fluid half rides the /gt6machine fluid fill arm + the p24 RCON chain)
			.then(machine("canner", GTMachines.CANNER_BLOCKS_BY_PATH.get("canner"), () -> GT6SprayCans.SPRAY_CAN_EMPTY.get()))
			.then(machine("canner_t2", GTMachines.CANNER_BLOCKS_BY_PATH.get("canner_t2"), () -> GT6SprayCans.SPRAY_CAN_EMPTY.get()))
			.then(machine("canner_t3", GTMachines.CANNER_BLOCKS_BY_PATH.get("canner_t3"), () -> GT6SprayCans.SPRAY_CAN_EMPTY.get()))
			.then(machine("canner_t4", GTMachines.CANNER_BLOCKS_BY_PATH.get("canner_t4"), () -> GT6SprayCans.SPRAY_CAN_EMPTY.get()))
			// task p26-w1-press-extruder-molds: the press + extruder ladders — the input feed
			// is the mold-class item (the forming rows take [block + mold]; the chain's `input`
			// step supplies the block face separately, the mold is the family stub face)
			.then(machine("press", GTMachines.PRESS_BLOCKS_BY_PATH.get("press"), () -> gregtech6.registry.GT6ExtruderMolds.SHAPE_EXTRUDER_PLATE.get()))
			.then(machine("press_t2", GTMachines.PRESS_BLOCKS_BY_PATH.get("press_t2"), () -> gregtech6.registry.GT6ExtruderMolds.SHAPE_EXTRUDER_PLATE.get()))
			.then(machine("press_t3", GTMachines.PRESS_BLOCKS_BY_PATH.get("press_t3"), () -> gregtech6.registry.GT6ExtruderMolds.SHAPE_EXTRUDER_PLATE.get()))
			.then(machine("press_t4", GTMachines.PRESS_BLOCKS_BY_PATH.get("press_t4"), () -> gregtech6.registry.GT6ExtruderMolds.SHAPE_EXTRUDER_PLATE.get()))
			.then(machine("extruder", GTMachines.EXTRUDER_BLOCKS_BY_PATH.get("extruder"), () -> gregtech6.registry.GT6ExtruderMolds.SHAPE_EXTRUDER_ROD.get()))
			.then(machine("extruder_t2", GTMachines.EXTRUDER_BLOCKS_BY_PATH.get("extruder_t2"), () -> gregtech6.registry.GT6ExtruderMolds.SHAPE_EXTRUDER_ROD.get()))
			.then(machine("extruder_t3", GTMachines.EXTRUDER_BLOCKS_BY_PATH.get("extruder_t3"), () -> gregtech6.registry.GT6ExtruderMolds.SHAPE_EXTRUDER_ROD.get()))
			.then(machine("extruder_t4", GTMachines.EXTRUDER_BLOCKS_BY_PATH.get("extruder_t4"), () -> gregtech6.registry.GT6ExtruderMolds.SHAPE_EXTRUDER_ROD.get()));
		event.getDispatcher().register(tMachine);
		LOGGER.info("Registered GT6 machine acceptance command /gt6machine (shredder|crusher|lathe|dryer|distillery|canner|press|extruder x t1..t4 | fakesource | paint <pos> <dye0-15|none> | unpaint <pos> x place|input|run|inject|check|fluid)");
		// the p8 ladder registration line (the runServer gate asserts it): the five family
		// BETs resolve — proof the RegistryObjects bound (press + extruder join = task
		// p26-w1-press-extruder-molds: 12 + 8 blocks, 3 + 2 family BETs).
		LOGGER.info("GT6 machine ladder registered: 20 blocks / 5 family BETs (T1-T4 validBlocks multi-attach), tiers "
			+ java.util.Arrays.deepToString(GTMachines.TIER_INPUTS) + " crusher parallel " + java.util.Arrays.toString(GTMachines.CRUSHER_PARALLEL));
		// the p14 dryer registration smoke line (the runServer gate asserts it): the family
		// BET resolves, the row config is the upstream :1477-1480 columns.
		LOGGER.info("GT6 dryer family registered: 4 blocks / 1 family BET (T1-T4 validBlocks multi-attach), HU bottom-face energy, "
			+ "RM.Drying (gt.recipe.drying) row map, parallel 8/16/32/64 + parallelDuration, hardness 6/4/9/12.5");
		// the p16 distillery registration smoke line (the runServer gate asserts it): the
		// family BET resolves, the row config is the upstream :1398-1401 columns.
		LOGGER.info("GT6 distillery family registered: 4 blocks / 1 family BET (T1-T4 validBlocks multi-attach), HU bottom-face energy, "
			+ "RM.Distillery (gt.recipe.distillery) row map, parallel 8/16/32/64 + parallelDuration, hardness 6/4/9/12.5");
		// the p24 canner registration smoke line (the runServer gate asserts it): the family
		// BET resolves, the row config is the upstream :1379-1382 columns.
		LOGGER.info("GT6 canner family registered: 4 blocks / 1 family BET (T1-T4 validBlocks multi-attach), EU energy, "
			+ "RM.Canner (gt.recipe.canner) 2/2 row map, tank capacity " + java.util.Arrays.toString(GTMachines.CANNER_TANK_CAPACITY)
			+ ", use-output-tank T, hardness 4.0");
	}

	/** One machine literal with its four subcommands (the oven command shape, parameterised). */
	private static LiteralArgumentBuilder<CommandSourceStack> machine(String aName, RegistryObject<Block> aBlock, Supplier<Item> aFeed) {
		LiteralArgumentBuilder<CommandSourceStack> tMachine = Commands.literal(aName);
		tMachine.then(Commands.literal("place")
			.executes(context -> place(context.getSource(), aBlock, null))
			.then(Commands.argument("pos", BlockPosArgument.blockPos())
				.executes(context -> place(context.getSource(), aBlock, BlockPosArgument.getLoadedBlockPos(context, "pos")))));
		tMachine.then(Commands.literal("input")
			.executes(context -> input(context.getSource(), aFeed, DEFAULT_INPUT_COUNT, null))
			.then(Commands.argument("count", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 64))
				.executes(context -> input(context.getSource(), aFeed,
						com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "count"), null))
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.executes(context -> input(context.getSource(), aFeed,
							com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "count"),
							BlockPosArgument.getLoadedBlockPos(context, "pos"))))));
		tMachine.then(Commands.literal("run")
			.then(Commands.argument("ticks", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 20000))
				.executes(context -> run(context.getSource(),
						com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "ticks"), null))
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.executes(context -> run(context.getSource(),
							com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "ticks"),
							BlockPosArgument.getLoadedBlockPos(context, "pos"))))));
		// task p8-machine-tiers-doinject ⑤(b): the FALSE-regime rig — inject+consume pairs.
		// finalSize = the OPTIONAL half-cycle pair: iteration ticks-1 runs at <finalSize>
		// instead of <size>, so a whole KU pulse cycle (positive train to completion + the
		// negative transition pair) lands inside ONE command — across RCON calls the idle
		// ticks reset the parked progress through doInactive's CONSTANT_ENERGY :894 (the
		// D3 wire-chain lesson, now proven for the machines too).
		tMachine.then(Commands.literal("inject")
			.then(Commands.argument("ticks", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 20000))
				.executes(context -> inject(context.getSource(),
						com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "ticks"), null, null, null))
				.then(Commands.argument("size", com.mojang.brigadier.arguments.IntegerArgumentType.integer(-4096, 4096))
					.executes(context -> inject(context.getSource(),
							com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "ticks"),
							com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "size"), null, null))
					.then(Commands.argument("pos", BlockPosArgument.blockPos())
						.executes(context -> inject(context.getSource(),
								com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "ticks"),
								com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "size"), null,
								BlockPosArgument.getLoadedBlockPos(context, "pos"))))
					.then(Commands.argument("finalSize", com.mojang.brigadier.arguments.IntegerArgumentType.integer(-4096, 4096))
						.executes(context -> inject(context.getSource(),
								com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "ticks"),
								com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "size"),
								com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "finalSize"), null))
						.then(Commands.argument("pos", BlockPosArgument.blockPos())
							.executes(context -> inject(context.getSource(),
									com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "ticks"),
									com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "size"),
									com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "finalSize"),
									BlockPosArgument.getLoadedBlockPos(context, "pos"))))))));
		tMachine.then(Commands.literal("check")
			.executes(context -> check(context.getSource(), null))
			.then(Commands.argument("pos", BlockPosArgument.blockPos())
				.executes(context -> check(context.getSource(), BlockPosArgument.getLoadedBlockPos(context, "pos")))));
		// task p14-dryer-family — the fluid arm (the /gt6tank fill/draw driver shape over
		// the machine's side-gated FLUID_HANDLER): fill/draw report 0 with the REJECTED
		// marker as a legitimate verdict (a masked face, an empty output tank), so the
		// RCON chain can assert on the row's rotated connectivity masks; stat dumps the
		// tank contents plus the live six-side mask faces (fluid in/out + the :511 energy
		// accepts). The first LIVE surface of the p14-machine-fluid-face carriers — the
		// W1a offline half drove them through the package-private factory only.
		tMachine.then(Commands.literal("fluid")
			.then(Commands.literal("fill")
				.then(Commands.argument("side", com.mojang.brigadier.arguments.StringArgumentType.word())
					.then(Commands.argument("fluid", net.minecraft.commands.arguments.ResourceLocationArgument.id())
						.then(Commands.argument("amount", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1))
							.executes(context -> fluidFill(context.getSource(),
									com.mojang.brigadier.arguments.StringArgumentType.getString(context, "side"),
									net.minecraft.commands.arguments.ResourceLocationArgument.getId(context, "fluid"),
									com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "amount"), null))
							.then(Commands.argument("pos", BlockPosArgument.blockPos())
								.executes(context -> fluidFill(context.getSource(),
										com.mojang.brigadier.arguments.StringArgumentType.getString(context, "side"),
										net.minecraft.commands.arguments.ResourceLocationArgument.getId(context, "fluid"),
										com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "amount"),
										BlockPosArgument.getLoadedBlockPos(context, "pos"))))))))
			.then(Commands.literal("draw")
				.then(Commands.argument("side", com.mojang.brigadier.arguments.StringArgumentType.word())
					.then(Commands.argument("amount", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1))
						.executes(context -> fluidDraw(context.getSource(),
								com.mojang.brigadier.arguments.StringArgumentType.getString(context, "side"),
								com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "amount"), null))
						.then(Commands.argument("pos", BlockPosArgument.blockPos())
							.executes(context -> fluidDraw(context.getSource(),
									com.mojang.brigadier.arguments.StringArgumentType.getString(context, "side"),
									com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "amount"),
									BlockPosArgument.getLoadedBlockPos(context, "pos")))))))
			.then(Commands.literal("stat")
				.executes(context -> fluidStat(context.getSource(), null))
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.executes(context -> fluidStat(context.getSource(), BlockPosArgument.getLoadedBlockPos(context, "pos"))))));
		return tMachine;
	}

	/**
	 * The crusher acceptance feed: the first material of the gem-chain registration order
	 * that survives ALL THREE upstream gates — the both-side mat() resolution of :72
	 * (:209/:214, a gem material without a registered gemFlawed item has its row skipped in
	 * the pour), the poured row itself, and the canOutput chain-processing power cap
	 * :626-629 (a row with duration × mEUt × 4 > mInputMax × 600 would be bound BELOW 4
	 * parallel — e.g. zirconium, duration 1024 — and the feed would not demonstrate the
	 * 4-parallel one-cycle acceptance). All three gates read the LIVE poured map.
	 */
	private static Item firstGemChainGem() {
		for (gregtech6.registry.GTMaterialItems.PrefixMaterial tPair : GTMaterialItems.registrationOrder()) {
			if (tPair.prefix() != OP.gem) continue;
			RegistryObject<Item> tIn = GTMaterialItems.get(OP.gem, tPair.material());
			RegistryObject<Item> tOut = GTMaterialItems.get(OP.gemFlawed, tPair.material());
			//? if forge {
			if (tIn == null || !tIn.isPresent() || tOut == null || !tOut.isPresent()) continue;
			//?} else {
			/*if (tIn == null || !tIn.isBound() || tOut == null || !tOut.isBound()) continue;
			 *///?}
			// the T1 crusher shape: mEUt, mParallel 4, mInputMax 64 — the cap :628 must leave 4 intact
			gregtech6.recipes.Recipe tRecipe = crusherRowFor(tIn.get());
			if (tRecipe != null && tRecipe.mEUt * tRecipe.mDuration * 4 <= 64L * 600) return tIn.get();
			LOGGER.info("GT6 machine feed: gem material {} skipped (no poured :72 row or the :626-629 cap binds below 4 parallel)", tPair.material().mNameInternal);
		}
		throw new IllegalStateException("No gt6:gem→gemFlawed pair with a cap-safe :72 row resolved for the crusher feed");
	}

	/** The poured :72 row for a gem item, or null (the row carries input gem x1). */
	private static gregtech6.recipes.Recipe crusherRowFor(net.minecraft.world.item.Item aGemItem) {
		for (gregtech6.recipes.Recipe tRecipe : gregtech6.recipes.GT6RecipeMaps.CRUSHER.mRecipeList) {
			if (tRecipe.mInputs.length == 1 && tRecipe.mInputs[0].getItem() == aGemItem && tRecipe.mInputs[0].getCount() == 1) return tRecipe;
		}
		return null;
	}

	private static TileEntityBasicMachine machineAt(CommandSourceStack source, BlockPos pos) {
		ServerLevel tLevel = source.getLevel();
		BlockPos tTarget = pos != null ? pos : BlockPos.containing(source.getPosition());
		return tLevel.getBlockEntity(tTarget) instanceof TileEntityBasicMachine tMachine ? tMachine : null;
	}

	/**
	 * The menu-or-null probe (task p14-dryer-family): the menu-less carriers (the dryer —
	 * the GUI pool card owns the MenuType registration) throw the documented
	 * IllegalStateException out of createMenu; the command surface degrades instead of
	 * crashing ({@code check} reports {@code data=-2}, {@code run} refuses).
	 */
	@javax.annotation.Nullable
	private static GTBasicMachineMenu menuOrNull(TileEntityBasicMachine aMachine, ServerPlayer aFakePlayer) {
		try {
			return (GTBasicMachineMenu) aMachine.createMenu(0, aFakePlayer.getInventory(), aFakePlayer);
		} catch (IllegalStateException tMenuLess) {
			return null;
		}
	}

	/** The GT6 side word → Direction (the /gt6tank parse, mirrored so this driver stays self-contained). */
	private static Direction parseSide(String aWord) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		Direction tSide = Direction.byName(aWord.toLowerCase(java.util.Locale.ROOT));
		if (tSide == null) throw new com.mojang.brigadier.exceptions.SimpleCommandExceptionType(Component.literal("Unknown side: " + aWord)).create();
		return tSide;
	}

	/**
	 * The p14 fluid fill driver: inject through the side-gated FLUID_HANDLER capability and
	 * report the accepted amount — 0 with the REJECTED marker is a legitimate verdict (a
	 * masked face per the row's mFluidInputs, or a full/mixed tank), the /gt6tank form.
	 */
	private static int fluidFill(CommandSourceStack aSource, String aSideWord, net.minecraft.resources.ResourceLocation aFluidId, int aAmount, @javax.annotation.Nullable BlockPos aPos) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		Direction tSide = parseSide(aSideWord);
		TileEntityBasicMachine tMachine = machineAt(aSource, aPos);
		if (tMachine == null) {
			aSource.sendFailure(Component.literal("No TileEntityBasicMachine at " + (aPos != null ? aPos.toShortString() : "the source position")));
			return 0;
		}
		net.minecraft.world.level.material.Fluid tFluid = net.minecraftforge.registries.ForgeRegistries.FLUIDS.getValue(aFluidId);
		if (tFluid == null || tFluid.defaultFluidState() == null || tFluid.defaultFluidState().isEmpty()) {
			aSource.sendFailure(Component.literal("Unknown fluid: " + aFluidId));
			return 0;
		}
		//? if forge {
		net.minecraftforge.fluids.capability.IFluidHandler tHandler = tMachine.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER, tSide).orElse(null);
		//?} else {
		/*net.minecraftforge.fluids.capability.IFluidHandler tHandler = tMachine.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, tSide);
		 *///?}
		if (tHandler == null) {
			aSource.sendFailure(Component.literal("CAPABILITY MISSING: the machine exposes no FLUID_HANDLER on " + tSide));
			return 0;
		}
		int tFilled = tHandler.fill(new net.minecraftforge.fluids.FluidStack(tFluid, aAmount), net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
		String tLine = String.format("GT6 %s fluid fill at %s face %s: filled %d/%d L of %s%s, input tanks hold %d L",
				tMachine.getTileEntityName(), tMachine.getBlockPos().toShortString(), tSide, tFilled, aAmount, aFluidId,
				tFilled == 0 ? " (REJECTED)" : " (ACCEPTED)", tankTotal(tMachine.mTanksInput));
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		LOGGER.info(tLine);
		return Command.SINGLE_SUCCESS;
	}

	/** The p14 fluid draw driver — the mirror of {@link #fluidFill} over the mFluidOutputs mask. */
	private static int fluidDraw(CommandSourceStack aSource, String aSideWord, int aAmount, @javax.annotation.Nullable BlockPos aPos) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		Direction tSide = parseSide(aSideWord);
		TileEntityBasicMachine tMachine = machineAt(aSource, aPos);
		if (tMachine == null) {
			aSource.sendFailure(Component.literal("No TileEntityBasicMachine at " + (aPos != null ? aPos.toShortString() : "the source position")));
			return 0;
		}
		//? if forge {
		net.minecraftforge.fluids.capability.IFluidHandler tHandler = tMachine.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER, tSide).orElse(null);
		//?} else {
		/*net.minecraftforge.fluids.capability.IFluidHandler tHandler = tMachine.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, tSide);
		 *///?}
		if (tHandler == null) {
			aSource.sendFailure(Component.literal("CAPABILITY MISSING: the machine exposes no FLUID_HANDLER on " + tSide));
			return 0;
		}
		net.minecraftforge.fluids.FluidStack tDrawn = tHandler.drain(aAmount, net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
		int tDrawnAmount = tDrawn == null ? 0 : tDrawn.getAmount();
		String tFluid = tDrawn == null || tDrawn.isEmpty() ? "nothing" : net.minecraftforge.registries.ForgeRegistries.FLUIDS.getKey(tDrawn.getFluid()).toString();
		String tLine = String.format("GT6 %s fluid draw at %s face %s: drawn %d/%d L of %s%s, output tanks hold %d L",
				tMachine.getTileEntityName(), tMachine.getBlockPos().toShortString(), tSide, tDrawnAmount, aAmount, tFluid,
				tDrawnAmount == 0 ? " (REJECTED)" : " (ACCEPTED)", tankTotal(tMachine.mTanksOutput));
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		LOGGER.info(tLine);
		return Command.SINGLE_SUCCESS;
	}

	/**
	 * The p14 fluid stat driver: the tank census plus the LIVE rotated-mask faces —
	 * {@code fluidIn=...} / {@code fluidOut=...} over the six world sides (the
	 * GTSideTables rotation of the row's masks) and {@code energyIn=...} (the :511
	 * isEnergyAcceptingFrom probe on the accepted type, theoretical arm). The assertion
	 * line the RCON chain pins the dryer's bottom-face energy + back/left tank geometry on.
	 */
	private static int fluidStat(CommandSourceStack aSource, @javax.annotation.Nullable BlockPos aPos) {
		TileEntityBasicMachine tMachine = machineAt(aSource, aPos);
		if (tMachine == null) {
			aSource.sendFailure(Component.literal("No TileEntityBasicMachine at " + (aPos != null ? aPos.toShortString() : "the source position")));
			return 0;
		}
		StringBuilder tIn = new StringBuilder();
		for (int i = 0; i < tMachine.mTanksInput.length; i++) {
			gregtech6.fluid.FluidTankGT tTank = tMachine.mTanksInput[i];
			tIn.append(i == 0 ? "" : ", ").append("in[").append(i).append("]=").append(tTank.amount()).append(" L of ")
					.append(tTank.isEmpty() ? "nothing" : net.minecraftforge.registries.ForgeRegistries.FLUIDS.getKey(tTank.getFluid().getFluid()));
		}
		StringBuilder tOut = new StringBuilder();
		for (int i = 0; i < tMachine.mTanksOutput.length; i++) {
			gregtech6.fluid.FluidTankGT tTank = tMachine.mTanksOutput[i];
			tOut.append(i == 0 ? "" : ", ").append("out[").append(i).append("]=").append(tTank.amount()).append(" L of ")
					.append(tTank.isEmpty() ? "nothing" : net.minecraftforge.registries.ForgeRegistries.FLUIDS.getKey(tTank.getFluid().getFluid()));
		}
		String tLine = String.format("GT6 %s fluid stat at %s: %s; %s; masks fluidIn=%d fluidOut=%d energyIn=%d; faces fluidIn=%s fluidOut=%s energyIn=%s",
				tMachine.getTileEntityName(), tMachine.getBlockPos().toShortString(), tIn, tOut,
				tMachine.mFluidInputs, tMachine.mFluidOutputs, tMachine.mEnergyInputs,
				maskFaces(tMachine, tMachine.mFluidInputs), maskFaces(tMachine, tMachine.mFluidOutputs),
				energyAcceptFaces(tMachine));
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		LOGGER.info(tLine);
		return Command.SINGLE_SUCCESS;
	}

	/** The total content of one tank bank (the stat/fill report). */
	private static long tankTotal(gregtech6.fluid.FluidTankGT[] aTanks) {
		long rTotal = 0;
		for (gregtech6.fluid.FluidTankGT tTank : aTanks) rTotal += tTank.amount();
		return rTotal;
	}

	/** The world faces the rotated mask accepts (the GTSideTables lookup, the six-side walk). */
	private static String maskFaces(TileEntityBasicMachine aMachine, byte aMask) {
		StringBuilder rFaces = new StringBuilder();
		for (Direction tSide : Direction.values()) {
			if (gregtech6.util.GTSideTables.faceConnected(aMachine.getFacing(), (byte)tSide.get3DDataValue(), aMask)) {
				if (rFaces.length() > 0) rFaces.append(",");
				rFaces.append(tSide);
			}
		}
		return rFaces.length() > 0 ? rFaces.toString() : "none";
	}

	/** The world faces the machine accepts the carrier energy type on (the live :511 probe, theoretical arm). */
	private static String energyAcceptFaces(TileEntityBasicMachine aMachine) {
		StringBuilder rFaces = new StringBuilder();
		for (Direction tSide : Direction.values()) {
			if (aMachine.isEnergyAcceptingFrom(aMachine.mEnergyTypeAccepted, (byte)tSide.get3DDataValue(), true)) {
				if (rFaces.length() > 0) rFaces.append(",");
				rFaces.append(tSide);
			}
		}
		return rFaces.length() > 0 ? rFaces.toString() : "none";
	}

	private static int place(CommandSourceStack source, RegistryObject<Block> aBlock, BlockPos pos) {
		BlockPos tTarget = pos != null ? pos : BlockPos.containing(source.getPosition());
		ServerLevel tLevel = source.getLevel();
		tLevel.setBlock(tTarget, aBlock.get().defaultBlockState(), 3);
		source.sendSuccess(() -> Component.literal("GT6 " + aBlock.getId().getPath() + " placed at " + tTarget.toShortString()), false);
		return Command.SINGLE_SUCCESS;
	}

	/**
	 * The feed stack factory: plain items stack plainly; the Integrated Circuit feed lands
	 * through {@link GT6Circuits#selector} so the stack carries its {@code Damage:0}
	 * configuration tag (a tag-less circuit would match no poured row — the recipe inputs
	 * route on the exact tag, the p16-distillery-family ① semantics).
	 */
	private static ItemStack feedStack(Supplier<Item> aFeed, int aCount) {
		if (aFeed.get() instanceof gregtech6.item.GT6Circuits.IntegratedCircuitItem) return gregtech6.item.GT6Circuits.selector(aFeed.get(), 0); // ST.tag(0)
		return new ItemStack(aFeed.get(), aCount);
	}

	private static int input(CommandSourceStack source, Supplier<Item> aFeed, int count, BlockPos pos) {
		TileEntityBasicMachine tMachine = machineAt(source, pos);
		if (tMachine == null) {
			source.sendFailure(Component.literal("No TileEntityBasicMachine at " + (pos != null ? pos.toShortString() : "the source position")));
			return 0;
		}
		ItemStack tFeed = feedStack(aFeed, count);
		ItemStack tLeftover = tMachine.getInventory().insertItem(TileEntityBasicMachine.SLOT_INPUT, tFeed, false);
		if (!tLeftover.isEmpty()) {
			source.sendFailure(Component.literal("Input slot rejected " + tLeftover.getCount() + " of the feed (blocked?)"));
			return 0;
		}
		source.sendSuccess(() -> Component.literal("GT6 " + tMachine.getTileEntityName() + " input: " + (tFeed.getCount() - tLeftover.getCount()) + "x "
				+ tFeed.getItem() + " into slot " + TileEntityBasicMachine.SLOT_INPUT), false);
		return Command.SINGLE_SUCCESS;
	}

	/** Drives the dispatcher and samples the three ContainerData states; asserts the acceptance trio + an output. */
	private static int run(CommandSourceStack source, int ticks, BlockPos pos) {
		TileEntityBasicMachine tMachine = machineAt(source, pos);
		if (tMachine == null) {
			source.sendFailure(Component.literal("No TileEntityBasicMachine at " + (pos != null ? pos.toShortString() : "the source position")));
			return 0;
		}
		ServerPlayer tFakePlayer = FakePlayerFactory.getMinecraft(source.getLevel());
		GTBasicMachineMenu tMenu = menuOrNull(tMachine, tFakePlayer);
		if (tMenu == null) {
			// the menu-less carrier (the dryer — the GUI pool card owns the MenuType); the
			// trio probe is a menu surface, so run refuses instead of crashing
			source.sendFailure(Component.literal("GT6 machine run refused: no MenuType bound (the menu-less carrier's GUI is the pool card's surface); use inject+check instead"));
			return 0;
		}

		boolean tSeenProgress = false, tSeenDone = false, tSeenIdle = false;
		int tLastValue = tMenu.computeProgressValue();
		for (int i = 0; i < ticks; i++) {
			tMachine.updateEntity();
			int tValue = tMenu.computeProgressValue();
			if (tValue > 0 && tValue < GTBasicMachineMenu.PROGRESS_DONE) tSeenProgress = true;
			else if (tValue == GTBasicMachineMenu.PROGRESS_DONE) tSeenDone = true;
			else if (tValue == -1) tSeenIdle = true;
			tLastValue = tValue;
		}
		String tOutputs = outputList(tMachine);
		tMenu.removed(tFakePlayer);

		boolean tOk = tSeenProgress && tSeenDone && tSeenIdle && tOutputs.length() > 0;
		String tReport = String.format(
			"GT6 %s run %d ticks at %s: ContainerData states progress=%s done=%s idle=%s, last=%d, outputs=[%s], progress=%d/%d, energy=%d, parallel=%d",
			tMachine.getTileEntityName(), ticks, tMachine.getBlockPos().toShortString(), tSeenProgress, tSeenDone, tSeenIdle, tLastValue,
			tOutputs, tMachine.mProgress, tMachine.mMaxProgress, tMachine.mEnergy, tMachine.mParallel);
		if (!tOk) {
			source.sendFailure(Component.literal("GT6 machine run check FAILED: " + tReport));
			return 0;
		}
		source.sendSuccess(() -> Component.literal("GT6 machine run check OK: " + tReport), false);
		LOGGER.info(tReport);
		return Command.SINGLE_SUCCESS;
	}

	/**
	 * The FALSE-regime rig (task p8-machine-tiers-doinject ⑤(b)): {@code ticks} iterations
	 * of one direct doInject (size defaults to the machine's mInputMax; negative = the AC
	 * half-cycle; {@code finalSize} = the optional LAST iteration's size, the negative
	 * transition pair) + one dispatcher tick per iteration, so each iteration IS one
	 * injection tick and a full KU pulse cycle closes inside one command.
	 */
	private static int inject(CommandSourceStack source, int ticks, Integer size, Integer finalSize, BlockPos pos) {
		TileEntityBasicMachine tMachine = machineAt(source, pos);
		if (tMachine == null) {
			source.sendFailure(Component.literal("No TileEntityBasicMachine at " + (pos != null ? pos.toShortString() : "the source position")));
			return 0;
		}
		long tSize = size != null ? size : tMachine.mInputMax;
		long tFinalSize = finalSize != null ? finalSize : tSize;
		if (tSize == 0 || tFinalSize == 0) {
			source.sendFailure(Component.literal("inject refused: size 0 is gated by the Root :717 aSize != 0 rule (the verbatim :503 math divides by the size)"));
			return 0;
		}
		long tUsed = 0;
		for (int i = 0; i < ticks; i++) {
			long tIterationSize = (finalSize != null && i == ticks - 1) ? tFinalSize : tSize;
			tUsed += tMachine.doInject(tMachine.mEnergyTypeAccepted, INJECT_SIDE, tIterationSize, 1, true);
			tMachine.updateEntity();
		}
		String tOutputs = outputList(tMachine);
		String tReport = String.format(
			"GT6 %s inject ticks=%d size=%d finalSize=%s used=%d progress=%d/%d energy=%d state=new:%s/old:%s outputs=[%s] at %s",
			tMachine.getTileEntityName(), ticks, tSize, finalSize, tUsed,
			tMachine.mProgress, tMachine.mMaxProgress, tMachine.mEnergy,
			tMachine.mStateNew, tMachine.mStateOld, tOutputs, tMachine.getBlockPos().toShortString());
		source.sendSuccess(() -> Component.literal(tReport), false);
		LOGGER.info(tReport);
		return Command.SINGLE_SUCCESS;
	}

	/** The output-slot listing shared by run/inject/check. */
	private static String outputList(TileEntityBasicMachine aMachine) {
		StringBuilder tOutputs = new StringBuilder();
		for (int i = 0; i < aMachine.getOutputSlotCount(); i++) {
			ItemStack tStack = aMachine.getInventory().getStackInSlot(aMachine.getInputSlotCount() + i);
			if (!tStack.isEmpty()) tOutputs.append(tStack.getCount()).append("x ").append(tStack.getItem()).append("; ");
		}
		return tOutputs.toString();
	}

	/** The regime switch (task p8-machine-tiers-doinject ⑤(c); default flipped by p11-rotor-source-flip): on|off|stat over ENERGY_FAKE_SOURCE. */
	private static LiteralArgumentBuilder<CommandSourceStack> fakesource() {
		return Commands.literal("fakesource")
			.then(Commands.literal("on").executes(context -> setFakeSource(context.getSource(), true)))
			.then(Commands.literal("off").executes(context -> setFakeSource(context.getSource(), false)))
			.then(Commands.literal("stat").executes(context -> {
				boolean tOn = TileEntityBasicMachine.ENERGY_FAKE_SOURCE;
				String tReport = "GT6 machine ENERGY_FAKE_SOURCE=" + tOn + (tOn
						? " (the RETIRED A-tier seam re-armed, the :815 alternating arm suspended)"
						: " (shipped default: grid-fed via doInject, the full upstream :815 semantics — feed the RU/KU machines with /gt6energy)");
				context.getSource().sendSuccess(() -> Component.literal(tReport), false);
				return Command.SINGLE_SUCCESS;
			}));
	}

	private static int setFakeSource(CommandSourceStack source, boolean aOn) {
		TileEntityBasicMachine.ENERGY_FAKE_SOURCE = aOn;
		String tReport = "GT6 machine ENERGY_FAKE_SOURCE set " + aOn;
		source.sendSuccess(() -> Component.literal(tReport), false);
		LOGGER.info(tReport);
		return Command.SINGLE_SUCCESS;
	}

	// ---------------------------------------------------------------------------
	// the paint arm (task p21-paintable-storage-sync, ADR ruling 1: the spray write-point
	// rides the SAME IPaintableTE API the offline tests drive — the p19 chisel precedent)
	// ---------------------------------------------------------------------------

	/** The {@code paint <pos> <dye0-15|none>} arm — the recolourBlock routing live face. */
	private static LiteralArgumentBuilder<CommandSourceStack> paintArm() {
		return Commands.literal("paint")
			.then(Commands.argument("pos", BlockPosArgument.blockPos())
				.then(Commands.argument("dye", com.mojang.brigadier.arguments.StringArgumentType.word())
					.executes(context -> paint(context.getSource(),
							BlockPosArgument.getLoadedBlockPos(context, "pos"),
							com.mojang.brigadier.arguments.StringArgumentType.getString(context, "dye")))));
	}

	/** The {@code unpaint <pos>} arm — the same face without the dye argument. */
	private static LiteralArgumentBuilder<CommandSourceStack> unpaintArm() {
		return Commands.literal("unpaint")
			.then(Commands.argument("pos", BlockPosArgument.blockPos())
				.executes(context -> unpaint(context.getSource(), BlockPosArgument.getLoadedBlockPos(context, "pos"))));
	}

	/**
	 * The paintable probe (ADR ruling 2): ANY BlockEntity implementing {@link IPaintableTE}
	 * — today the whole 03 family (machines, and the future connector/barrel rides), not
	 * just the TileEntityBasicMachine rows — null when the target is not paintable.
	 */
	@javax.annotation.Nullable
	private static BlockEntity paintableAt(CommandSourceStack source, BlockPos pos) {
		ServerLevel tLevel = source.getLevel();
		BlockEntity tBE = tLevel.getBlockEntity(pos);
		return tBE instanceof IPaintableTE ? tBE : null;
	}

	/**
	 * {@code paint <pos> <dye0-15|none>}: a dye index routes through {@link IPaintableTE#mixPaint}
	 * (the upstream TileEntityBase04MultiTileEntities.java:227-235 recolourBlock shape —
	 * an already-painted machine MIXES by channel average), {@code none} is
	 * {@link IPaintableTE#unpaint}. The report always reads the colour back — the RCON
	 * chain pins {@code RGB #old->#new} and the painted flag.
	 */
	private static int paint(CommandSourceStack source, BlockPos aPos, String aDyeWord) {
		BlockEntity tBE = paintableAt(source, aPos);
		if (tBE == null) {
			source.sendFailure(Component.literal("No paintable GT6 TileEntity (IPaintableTE) at " + aPos.toShortString()));
			return 0;
		}
		IPaintableTE tTarget = (IPaintableTE) tBE;
		int tBefore = tTarget.getPaint();
		boolean tChanged;
		String tDyeDesc;
		if ("none".equalsIgnoreCase(aDyeWord)) {
			tChanged = tTarget.unpaint();
			tDyeDesc = "none (unpaint)";
		} else {
			int tDye;
			try {
				tDye = Integer.parseInt(aDyeWord);
			} catch (NumberFormatException tNotANumber) {
				source.sendFailure(Component.literal("Unknown dye '" + aDyeWord + "': use 0-15 (GT6 DYE_INDEX) or none"));
				return 0;
			}
			if (tDye < 0 || tDye >= DYES_INT.length) {
				source.sendFailure(Component.literal("Dye index out of range: " + tDye + " (0-15, 0=Black..15=White)"));
				return 0;
			}
			tChanged = tTarget.mixPaint(DYES_INT[tDye]);
			tDyeDesc = tDye + " (" + DYE_NAMES[tDye] + " #" + String.format("%06X", DYES_INT[tDye]) + ")";
		}
		String tName = tBE instanceof TileEntityBase01Root tRoot ? tRoot.getTileEntityName() : tBE.getClass().getSimpleName();
		String tLine = String.format("GT6 paint %s at %s: dye %s, RGB #%06X->#%06X painted=%s%s",
				tName, aPos.toShortString(), tDyeDesc, tBefore, tTarget.getPaint(), tTarget.isPainted(),
				tChanged ? " (APPLIED)" : " (NO-OP)");
		source.sendSuccess(() -> Component.literal(tLine), false);
		LOGGER.info(tLine);
		return Command.SINGLE_SUCCESS;
	}

	/** The {@code unpaint <pos>} driver (see {@link #paint}). */
	private static int unpaint(CommandSourceStack source, BlockPos aPos) {
		return paint(source, aPos, "none");
	}

	private static int check(CommandSourceStack source, BlockPos pos) {
		TileEntityBasicMachine tMachine = machineAt(source, pos);
		if (tMachine == null) {
			source.sendFailure(Component.literal("No TileEntityBasicMachine at " + (pos != null ? pos.toShortString() : "the source position")));
			return 0;
		}
		ServerPlayer tFakePlayer = FakePlayerFactory.getMinecraft(source.getLevel());
		GTBasicMachineMenu tMenu = menuOrNull(tMachine, tFakePlayer);
		// the menu-less carrier (the dryer) reports data=-2 instead of crashing — the
		// ContainerData trio is a menu surface, the field report below is not
		int tDataValue = tMenu != null ? tMenu.computeProgressValue() : -2;
		if (tMenu != null) tMenu.removed(tFakePlayer);

		StringBuilder tSlots = new StringBuilder();
		ItemStack tInput = tMachine.getInventory().getStackInSlot(TileEntityBasicMachine.SLOT_INPUT);
		tSlots.append("input=").append(tInput.getItem()).append("x").append(tInput.getCount());
		for (int i = 0; i < tMachine.getOutputSlotCount(); i++) {
			ItemStack tStack = tMachine.getInventory().getStackInSlot(tMachine.getInputSlotCount() + i);
			if (!tStack.isEmpty()) tSlots.append(" out[").append(i).append("]=").append(tStack.getCount()).append("x ").append(tStack.getItem());
		}

		CompoundTag tDebug = new CompoundTag();
		tMachine.saveAdditional(tDebug);
		String tReport = String.format(
			"GT6 %s at %s: %s data=%d progress=%d/%d energy=%d minenergy=%d minIn=%d recIn=%d maxIn=%d state=new:%s/old:%s stopped=%s active=%s running=%s parallel=%d parallelDuration=%s facing=%d",
			tMachine.getTileEntityName(), tMachine.getBlockPos().toShortString(), tSlots, tDataValue,
			tMachine.mProgress, tMachine.mMaxProgress, tMachine.mEnergy, tMachine.mMinEnergy,
			tMachine.mInputMin, tMachine.mInput, tMachine.mInputMax,
			tMachine.mStateNew, tMachine.mStateOld,
			tMachine.mStopped, tMachine.mActive, tMachine.mRunning, tMachine.mParallel, tMachine.mParallelDuration, tMachine.getFacing());
		source.sendSuccess(() -> Component.literal(tReport), false);
		return Command.SINGLE_SUCCESS;
	}
}
