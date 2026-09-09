package gregtech6.command;

import java.util.ArrayList;
import java.util.List;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import org.slf4j.Logger;

import gregapi.oredict.MaterialRegistry;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictMaterialStack;
import gregtech6.tileentity.tools.TileEntityMold;
import gregtech6.tileentity.tools.TileEntitySmeltery;

/**
 * The crucible-chain RCON driver (task p26-crucible-physics-smeltery acceptance, the
 * card-local command/ form of GTBoilerCommand/GT6ChiselCommand):
 * <ul>
 * <li>{@code place <pos> <variant>} / {@code place-mold <pos> <variant>} — the block
 *     placement arms (the GTOvenCommand.place form over the row path lookup).</li>
 * <li>{@code stat <pos>} — the crucible readback: temperature, energy, content lines,
 *     the meltdown warning verdict token, the LIQUID_LEVEL bucket.</li>
 * <li>{@code mold <pos>} — the mold readback: shape, the mapped prefix, required units,
 *     content and temperature.</li>
 * <li>{@code pour <moldPos>} — the mold right-click counterpart: drives
 *     {@code pourFromAdjacentCrucible} on the mold BE (the :271-289 adjacent-crucible pull).</li>
 * <li>{@code bucket <pos> <item>} — the 桶装熔液 face: one drain + pour-back round trip with
 *     a fresh container of the given item over the playerless {@code fluidContainerArm}.</li>
 * <li>{@code inject-hu <pos> <hu>} — the doInject face (the burning-box packet stream
 *     compresses into one call for the RCON window).</li>
 * <li>{@code drop <pos> <prefix> <material> <count>} — spawns ONE item entity above the
 *     crucible (the :154 suck arm's acceptance feed; the vanilla /summon item needs NBT
 *     ids, this arm takes prefix/material names).</li>
 * <li>{@code cool <pos>} — the supply-cut arm: zeroes the energy buffer so the :311
 *     cooldown drip shows (the mold solidification driver).</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = "gt6")
public final class GT6CrucibleCommand {

	private static final Logger LOGGER = LogUtils.getLogger();

	private GT6CrucibleCommand() {}

	@SubscribeEvent
	public static void onRegisterCommands(RegisterCommandsEvent aEvent) {
		net.minecraft.commands.CommandBuildContext tBuildContext = aEvent.getBuildContext(); // RegisterCommandsEvent.java:56 (the GTBurnerCommand form)
		LiteralArgumentBuilder<CommandSourceStack> tCrucible = Commands.literal("gt6crucible")
			.requires(aSource -> aSource.hasPermission(2))
			.then(Commands.literal("place")
				.then(Commands.argument("pos", net.minecraft.commands.arguments.coordinates.BlockPosArgument.blockPos())
					.then(Commands.argument("variant", StringArgumentType.word())
						.executes(aContext -> place(aContext.getSource(), net.minecraft.commands.arguments.coordinates.BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
								StringArgumentType.getString(aContext, "variant"), false)))))
			.then(Commands.literal("place-mold")
				.then(Commands.argument("pos", net.minecraft.commands.arguments.coordinates.BlockPosArgument.blockPos())
					.then(Commands.argument("variant", StringArgumentType.word())
						.executes(aContext -> place(aContext.getSource(), net.minecraft.commands.arguments.coordinates.BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
								StringArgumentType.getString(aContext, "variant"), true)))))
			.then(Commands.literal("stat")
				.then(Commands.argument("pos", net.minecraft.commands.arguments.coordinates.BlockPosArgument.blockPos())
					.executes(aContext -> stat(aContext.getSource(), net.minecraft.commands.arguments.coordinates.BlockPosArgument.getLoadedBlockPos(aContext, "pos")))))
			.then(Commands.literal("mold")
				.then(Commands.argument("pos", net.minecraft.commands.arguments.coordinates.BlockPosArgument.blockPos())
					.executes(aContext -> moldStat(aContext.getSource(), net.minecraft.commands.arguments.coordinates.BlockPosArgument.getLoadedBlockPos(aContext, "pos")))))
			.then(Commands.literal("pour")
				.then(Commands.argument("pos", net.minecraft.commands.arguments.coordinates.BlockPosArgument.blockPos())
					.executes(aContext -> pour(aContext.getSource(), net.minecraft.commands.arguments.coordinates.BlockPosArgument.getLoadedBlockPos(aContext, "pos")))))
			.then(Commands.literal("bucket")
				.then(Commands.argument("pos", net.minecraft.commands.arguments.coordinates.BlockPosArgument.blockPos())
					.then(Commands.argument("item", net.minecraft.commands.arguments.item.ItemArgument.item(tBuildContext))
						.executes(aContext -> bucket(aContext.getSource(), net.minecraft.commands.arguments.coordinates.BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
								net.minecraft.commands.arguments.item.ItemArgument.getItem(aContext, "item").createItemStack(1, false))))))
			.then(Commands.literal("inject-hu")
				.then(Commands.argument("pos", net.minecraft.commands.arguments.coordinates.BlockPosArgument.blockPos())
					.then(Commands.argument("hu", IntegerArgumentType.integer(1, 1000000000))
						.executes(aContext -> injectHu(aContext.getSource(), net.minecraft.commands.arguments.coordinates.BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
								IntegerArgumentType.getInteger(aContext, "hu"))))))
			.then(Commands.literal("cool")
				.then(Commands.argument("pos", net.minecraft.commands.arguments.coordinates.BlockPosArgument.blockPos())
					.executes(aContext -> cool(aContext.getSource(), net.minecraft.commands.arguments.coordinates.BlockPosArgument.getLoadedBlockPos(aContext, "pos")))))
			.then(Commands.literal("drop")
				.then(Commands.argument("pos", net.minecraft.commands.arguments.coordinates.BlockPosArgument.blockPos())
					.then(Commands.argument("prefix", StringArgumentType.word())
						.then(Commands.argument("material", StringArgumentType.word())
							.then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
								.executes(aContext -> drop(aContext.getSource(), net.minecraft.commands.arguments.coordinates.BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
										StringArgumentType.getString(aContext, "prefix"), StringArgumentType.getString(aContext, "material"),
										IntegerArgumentType.getInteger(aContext, "count"))))))));
		aEvent.getDispatcher().register(tCrucible);
		LOGGER.info("Registered GT6 crucible command /gt6crucible (place | place-mold | stat | mold | pour | bucket | inject-hu | cool | drop) — the crucible-chain acceptance home");
	}

	/** The place arm over the GT6Crucibles/GT6Molds row paths. The bare variant form
	 * ("stone" / "steel") resolves through the row-path prefix ("smeltery_stone" /
	 * "mold_stone" — the registered block paths, pinned by the datagen crafting rows);
	 * a full row path is accepted verbatim. */
	private static int place(CommandSourceStack aSource, BlockPos aPos, String aVariant, boolean aMold) {
		net.minecraft.world.level.block.Block tBlock = aMold ? gregtech6.registry.GT6Molds.blockByPath(aVariant) : gregtech6.registry.GT6Crucibles.blockByPath(aVariant);
		if (tBlock == null) {
			String tPath = (aMold ? "mold_" : "smeltery_") + aVariant;
			tBlock = aMold ? gregtech6.registry.GT6Molds.blockByPath(tPath) : gregtech6.registry.GT6Crucibles.blockByPath(tPath);
		}
		if (tBlock == null) {
			aSource.sendFailure(Component.literal("PLACE FAILED: unknown " + (aMold ? "mold" : "crucible") + " variant " + aVariant));
			return 0;
		}
		aSource.getLevel().setBlock(aPos, tBlock.defaultBlockState(), 3);
		String tLine = "GT6 " + (aMold ? "mold" : "smeltery") + " placed at " + aPos.toShortString() + ": " + aVariant;
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		LOGGER.info(tLine);
		return Command.SINGLE_SUCCESS;
	}

	/** The crucible readback (the thermometer + content census). */
	private static int stat(CommandSourceStack aSource, BlockPos aPos) {
		BlockEntity tBE = aSource.getLevel().getBlockEntity(aPos);
		if (!(tBE instanceof TileEntitySmeltery tCrucible)) {
			aSource.sendFailure(Component.literal("GT6 STAT FAILED: no crucible at " + aPos.toShortString()));
			return 0;
		}
		List<String> tLines = new ArrayList<>();
		tLines.add("GT6 crucible at " + aPos.getX() + ", " + aPos.getY() + ", " + aPos.getZ()
				+ " temp=" + tCrucible.mTemperature + "K max=" + tCrucible.temperatureMax()
				+ " energy=" + tCrucible.mEnergy + "HU meltdown=" + (tCrucible.mMeltDown ? "WARNING" : "ok")
				+ " level=" + tCrucible.getBlockState().getValue(gregtech6.registry.GT6Crucibles.CrucibleBlock.LIQUID_LEVEL)
				+ " total=" + gregapi.util.CruciblePhysics.total(tCrucible.mContent) + "u");
		for (OreDictMaterialStack tStack : tCrucible.mContent) {
			tLines.add("  content: " + tStack.mMaterial.mNameInternal + " x " + tStack.mAmount + "u");
		}
		for (String tLine : tLines) aSource.sendSuccess(() -> Component.literal(tLine), false);
		return Command.SINGLE_SUCCESS;
	}

	/** The mold readback. */
	private static int moldStat(CommandSourceStack aSource, BlockPos aPos) {
		BlockEntity tBE = aSource.getLevel().getBlockEntity(aPos);
		if (!(tBE instanceof TileEntityMold tMold)) {
			aSource.sendFailure(Component.literal("GT6 MOLD FAILED: no mold at " + aPos.toShortString()));
			return 0;
		}
		gregapi.oredict.OreDictPrefix tPrefix = TileEntityMold.getMoldRecipe(tMold.mShape);
		String tLine = "GT6 mold at " + aPos.getX() + ", " + aPos.getY() + ", " + aPos.getZ()
				+ " shape=" + tMold.mShape + " prefix=" + (tPrefix == null ? "none" : tPrefix.mNameInternal)
				+ " required=" + tMold.getMoldRequiredMaterialUnits() + "u temp=" + tMold.mTemperature + "K"
				+ " content=" + (tMold.mContent == null ? "empty" : tMold.mContent.mMaterial.mNameInternal + " x " + tMold.mContent.mAmount + "u")
				+ " output=" + (tMold.mInventory.isEmpty() ? "empty" : tMold.mInventory.get().getCount() + "x " + tMold.mInventory.get().getItem());
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		return Command.SINGLE_SUCCESS;
	}

	/** The mold right-click counterpart: the useTop pour (the playerless shared seam). */
	private static int pour(CommandSourceStack aSource, BlockPos aPos) {
		BlockEntity tBE = aSource.getLevel().getBlockEntity(aPos);
		if (!(tBE instanceof TileEntityMold tMold)) {
			aSource.sendFailure(Component.literal("GT6 POUR FAILED: no mold at " + aPos.toShortString()));
			return 0;
		}
		tMold.pourFromAdjacentCrucible();
		aSource.sendSuccess(() -> Component.literal("GT6 mold pour triggered at " + aPos.toShortString()
				+ (tMold.mContent == null ? " (content empty)" : " content=" + tMold.mContent.mMaterial.mNameInternal + " x " + tMold.mContent.mAmount + "u")), false);
		return Command.SINGLE_SUCCESS;
	}

	/**
	 * The bucket arm (the 桶装熔液 face): ONE round trip with a fresh container of the given
	 * item — drain the lightest molten content into it, then pour it back — reporting both
	 * halves and the pile census after each (the :450-468/:469-490 playerless seam).
	 */
	private static int bucket(CommandSourceStack aSource, BlockPos aPos, ItemStack aContainer) {
		BlockEntity tBE = aSource.getLevel().getBlockEntity(aPos);
		if (!(tBE instanceof TileEntitySmeltery tCrucible)) {
			aSource.sendFailure(Component.literal("GT6 BUCKET FAILED: no crucible at " + aPos.toShortString()));
			return 0;
		}
		String tContainerId = String.valueOf(net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(aContainer.getItem()));
		TileEntitySmeltery.ContainerArm tDrain = tCrucible.fluidContainerArm(aContainer.copy());
		if (tDrain == null) {
			aSource.sendFailure(Component.literal("GT6 BUCKET FAILED: " + tContainerId + " drained nothing at " + aPos.toShortString()));
			return 0;
		}
		ItemStack tFilled = tDrain.containerOut();
		String tFilledId = tFilled.isEmpty() ? "empty" : String.valueOf(net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(tFilled.getItem()));
		String tDrainLine = "GT6 crucible drained into " + tContainerId + " -> " + tFilledId
				+ ", pile=" + gregapi.util.CruciblePhysics.total(tCrucible.mContent) + "u";
		TileEntitySmeltery.ContainerArm tPour = tCrucible.fluidContainerArm(tFilled);
		if (tPour == null) {
			aSource.sendFailure(Component.literal("GT6 BUCKET FAILED: the pour-back refused at " + aPos.toShortString()));
			return 0;
		}
		ItemStack tEmptied = tPour.containerOut();
		String tEmptiedId = tEmptied.isEmpty() ? "empty" : String.valueOf(net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(tEmptied.getItem()));
		aSource.sendSuccess(() -> Component.literal(tDrainLine + " | poured back from " + tFilledId + " -> " + tEmptiedId
				+ ", pile=" + gregapi.util.CruciblePhysics.total(tCrucible.mContent) + "u temp=" + tCrucible.mTemperature + "K"), false);
		return Command.SINGLE_SUCCESS;
	}

	/** The doInject face over the HU leg. */
	private static int injectHu(CommandSourceStack aSource, BlockPos aPos, long aHu) {
		BlockEntity tBE = aSource.getLevel().getBlockEntity(aPos);
		if (!(tBE instanceof TileEntitySmeltery tCrucible)) {
			aSource.sendFailure(Component.literal("GT6 HEAT FAILED: no crucible at " + aPos.toShortString()));
			return 0;
		}
		long tUsed = tCrucible.doEnergyInjection(gregapi.data.TD.Energy.HU, (byte)1, 1, aHu, true);
		aSource.sendSuccess(() -> Component.literal("GT6 crucible injected " + tUsed + " HU, buffer=" + tCrucible.mEnergy), false);
		return Command.SINGLE_SUCCESS;
	}

	/** The supply-cut arm (the :311 cooldown driver). */
	private static int cool(CommandSourceStack aSource, BlockPos aPos) {
		BlockEntity tBE = aSource.getLevel().getBlockEntity(aPos);
		if (!(tBE instanceof TileEntitySmeltery tCrucible)) {
			aSource.sendFailure(Component.literal("GT6 COOL FAILED: no crucible at " + aPos.toShortString()));
			return 0;
		}
		tCrucible.mEnergy = 0;
		tCrucible.mCooldown = 0;
		aSource.sendSuccess(() -> Component.literal("GT6 crucible supply cut at " + aPos.toShortString()), false);
		return Command.SINGLE_SUCCESS;
	}

	/** The item-entity feed arm: ONE item of (prefix, material) dropped above the crucible. */
	private static int drop(CommandSourceStack aSource, BlockPos aPos, String aPrefix, String aMaterial, int aCount) {
		OreDictMaterial tMaterial = MaterialRegistry.INSTANCE.byName(aMaterial);
		if (tMaterial == null) {
			// the RCON word arrives lower-case ("iron") while the internal names are
			// camel-case ("Iron") and MATERIAL_MAP is a case-sensitive HashMap — the same
			// lenient scan the prefix arm below already uses
			for (OreDictMaterial tScan : MaterialRegistry.INSTANCE.MATERIAL_MAP.values()) {
				if (tScan.mNameInternal.equalsIgnoreCase(aMaterial)) {tMaterial = tScan; break;}
			}
		}
		gregapi.oredict.OreDictPrefix tPrefix = null;
		for (gregapi.oredict.OreDictPrefix tScan : gregapi.oredict.OreDictPrefix.VALUES) {
			if (tScan.mNameInternal.equalsIgnoreCase(aPrefix)) {tPrefix = tScan; break;}
		}
		if (tMaterial == null || tMaterial.mID < 0 || tPrefix == null) {
			aSource.sendFailure(Component.literal("GT6 DROP FAILED: unknown prefix/material " + aPrefix + "/" + aMaterial));
			return 0;
		}
		final gregapi.oredict.OreDictPrefix tResolvedPrefix = tPrefix;
		ItemStack tStack = gregtech6.recipes.maps.GT6RecipeMapCrucible.matStack(tPrefix, tMaterial, aCount);
		if (tStack == null || tStack.isEmpty()) {
			aSource.sendFailure(Component.literal("GT6 DROP FAILED: no item for " + tPrefix.mNameInternal + "/" + aMaterial));
			return 0;
		}
		ServerLevel tLevel = aSource.getLevel();
		// Spawn just ABOVE the top face with zeroed motion: the vanilla ItemEntity
		// constructor hands the charge a random horizontal toss (probed: the entity
		// touched down 1.2 blocks off the crucible, 7 of 8 items unrecoverable), and
		// spawning inside the block cell instead gets the entity evicted by collision
		// resolution mid-drain (probed pass-2: 6 of 8 landed). y+1.05 clears the block
		// collision entirely, falls straight onto the top face and rests inside the
		// :154 suck box (y+0.125..y+1.25) until the one-per-tick drain empties it.
		ItemEntity tEntity = new ItemEntity(tLevel, aPos.getX() + 0.5, aPos.getY() + 1.05, aPos.getZ() + 0.5, tStack);
		tEntity.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
		tEntity.setPickUpDelay(20);
		tLevel.addFreshEntity(tEntity);
		aSource.sendSuccess(() -> Component.literal("GT6 dropped " + aCount + "x " + tResolvedPrefix.mNameInternal + " " + aMaterial + " above " + aPos.toShortString()), false);
		return Command.SINGLE_SUCCESS;
	}
}
