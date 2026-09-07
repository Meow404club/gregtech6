package gregtech6.jade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.HashSet;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.Fluids;

import net.minecraftforge.fluids.FluidStack;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import snownee.jade.api.BlockAccessor;

import gregtech6.fluid.FluidTankGT;
import gregtech6.recipes.RecipeMap;
import gregtech6.tileentity.GTOfflineTestBase;
import gregtech6.tileentity.machines.TileEntityBasicMachine;
import gregtech6.tileentity.machines.TileEntityOven;

/**
 * Offline gate for the Jade fluid section (task p22-jade-fluid-tooltip acceptance ②):
 * <b>appendServerData 键形状+long 量</b> on a full-tank BE, plus the pure line/payload
 * format faces. The tooltip half ({@code appendTooltip}) is live-only — the fluid icon
 * rides Jade's client internals ({@code IElementHelper.get()}, null offline) — so the
 * offline contract pins the wire format the client parses and the pure text composition.
 *
 * <p>BlockAccessor is a return-type-driven dynamic proxy: the two legs' accessor
 * interfaces differ (1.21.1 adds the codec faces), so per-method stubs would fork the
 * test — only {@code getBlockEntity} is meaningful to {@code appendServerData}.
 */
public class GT6FluidProviderTest extends GTOfflineTestBase {

	static final BlockPos POS = new BlockPos(1, 2, 3);
	/** Past Integer.MAX_VALUE — the whole point of the long Amount key (arch correction C-2). */
	static final long OVERFLOW_AMOUNT = 3_000_000_000L;

	static BlockEntityType<TileEntityBasicMachine> sMachineType;
	static BlockEntityType<TileEntityOven> sOvenType;

	/**
	 * The 1-in-fluid/2-out-fluid RecipeMap shape (GT6RecipeMaps.java:278-285 DISTILLERY row)
	 * as a FRESH map — the tanks come from the counts alone (TileEntityBasicMachine ctor
	 * :325-328), so no recipe-pour/reset discipline is needed.
	 */
	@BeforeAll
	static void fixture() {
		RecipeMap tMap = new RecipeMap(new HashSet<>(),
				"gt.recipe.distillery", "Distillery", null,
				0, 1,
				"gt6:textures/gui/machines/distillery",
				/*IN-OUT-MIN-ITEM=*/ 1, 2, 1,
				/*IN-OUT-MIN-FLUID=*/ 1, 2, 1,
				/*MIN=*/ 2,
				/*AMP=*/ 1);
		sMachineType = machineType(tMap);
		// the negative: a sibling GT6 ticking BE outside the single branch — the oven is a
		// TileEntityBase03TicksAndSync, NOT a BasicMachine (TileEntityOven.java:120), and
		// carries no tank face.
		@SuppressWarnings("unchecked")
		BlockEntityType<TileEntityOven>[] tOvenHolder = (BlockEntityType<TileEntityOven>[]) new BlockEntityType<?>[1];
		tOvenHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new TileEntityOven(tOvenHolder[0], aPos, aState),
				Blocks.BRICKS).build(null);
		sOvenType = tOvenHolder[0];
	}

	static BlockEntityType<TileEntityBasicMachine> machineType(RecipeMap aMap) {
		@SuppressWarnings("unchecked")
		BlockEntityType<TileEntityBasicMachine>[] tHolder = (BlockEntityType<TileEntityBasicMachine>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new TileEntityBasicMachine(tHolder[0], aPos, aState, aMap, 1, false, null),
				Blocks.BRICKS).build(null);
		return tHolder[0];
	}

	/** An offline machine BE — no level needed: appendServerData only reads the public tank fields. */
	static TileEntityBasicMachine makeMachine() {
		return sMachineType.create(POS, Blocks.BRICKS.defaultBlockState());
	}

	@Test
	public void fullTanksWriteSelfDescribingKeysWithTrueLongAmounts() {
		TileEntityBasicMachine tMachine = makeMachine();
		tMachine.mTanksInput[0].setCapacity(16000);
		tMachine.mTanksInput[0].add(4000, new FluidStack(Fluids.WATER, 1)); // the long-add adopt face (upstream :170-179)
		tMachine.mTanksOutput[0].add(OVERFLOW_AMOUNT, new FluidStack(Fluids.LAVA, 1)); // capacity Long.MAX (no-arg tank) → full adopt
		// mTanksOutput[1] stays empty → no entry

		CompoundTag tData = new CompoundTag();
		GT6FluidProvider.INSTANCE.appendServerData(tData, accessorOf(tMachine));

		// input group: exactly the filled tank, Amount/Capacity as true longs
		assertTrue(tData.contains(GT6FluidProvider.KEY_FLUIDS_IN, Tag.TAG_LIST));
		ListTag tIn = tData.getList(GT6FluidProvider.KEY_FLUIDS_IN, Tag.TAG_COMPOUND);
		assertEquals(1, tIn.size());
		CompoundTag tWater = tIn.getCompound(0);
		assertEquals("minecraft:water", tWater.getString(GT6FluidProvider.KEY_FLUID_NAME));
		assertEquals(4000L, tWater.getLong(GT6FluidProvider.KEY_AMOUNT));
		assertEquals(Tag.TAG_LONG, tWater.getTagType(GT6FluidProvider.KEY_AMOUNT), "Amount must ride the long face, not the bindInt int face");
		assertEquals(16000L, tWater.getLong(GT6FluidProvider.KEY_CAPACITY));
		assertEquals(Tag.TAG_LONG, tWater.getTagType(GT6FluidProvider.KEY_CAPACITY));

		// output group: the overflow tank keeps its 63-bit amount verbatim (> INT_MAX is the C-2 seam)
		assertTrue(tData.contains(GT6FluidProvider.KEY_FLUIDS_OUT, Tag.TAG_LIST));
		ListTag tOut = tData.getList(GT6FluidProvider.KEY_FLUIDS_OUT, Tag.TAG_COMPOUND);
		assertEquals(1, tOut.size(), "the empty second output tank produces no entry");
		CompoundTag tLava = tOut.getCompound(0);
		assertEquals("minecraft:lava", tLava.getString(GT6FluidProvider.KEY_FLUID_NAME));
		assertEquals(OVERFLOW_AMOUNT, tLava.getLong(GT6FluidProvider.KEY_AMOUNT));
		assertTrue(tLava.getLong(GT6FluidProvider.KEY_AMOUNT) > Integer.MAX_VALUE);
		assertEquals(Long.MAX_VALUE, tLava.getLong(GT6FluidProvider.KEY_CAPACITY));
	}

	@Test
	public void fluidlessMachineStillWritesBothGroupKeysAsEmptyLists() {
		CompoundTag tData = new CompoundTag();
		GT6FluidProvider.INSTANCE.appendServerData(tData, accessorOf(makeMachine()));

		assertTrue(tData.contains(GT6FluidProvider.KEY_FLUIDS_IN, Tag.TAG_LIST));
		assertTrue(tData.contains(GT6FluidProvider.KEY_FLUIDS_OUT, Tag.TAG_LIST));
		assertTrue(tData.getList(GT6FluidProvider.KEY_FLUIDS_IN, Tag.TAG_COMPOUND).isEmpty());
		assertTrue(tData.getList(GT6FluidProvider.KEY_FLUIDS_OUT, Tag.TAG_COMPOUND).isEmpty());
	}

	@Test
	public void nonBasicMachineBlockEntitiesWriteNoFluidKeys() {
		// the single-branch guard: a GT6 BE outside TileEntityBasicMachine (the oven) and a
		// null BE both leave the tag untouched (the server data tag may carry other providers'
		// keys — they must never be disturbed, GT6MachineProvider's contract mirrored).
		CompoundTag tOvenData = new CompoundTag();
		GT6FluidProvider.INSTANCE.appendServerData(tOvenData, accessorOf(sOvenType.create(POS, Blocks.BRICKS.defaultBlockState())));
		assertFalse(tOvenData.contains(GT6FluidProvider.KEY_FLUIDS_IN));
		assertFalse(tOvenData.contains(GT6FluidProvider.KEY_FLUIDS_OUT));

		CompoundTag tNullData = new CompoundTag();
		GT6FluidProvider.INSTANCE.appendServerData(tNullData, accessorOf(null));
		assertFalse(tNullData.contains(GT6FluidProvider.KEY_FLUIDS_IN));
		assertFalse(tNullData.contains(GT6FluidProvider.KEY_FLUIDS_OUT));
	}

	@Test
	public void tanksTagPureFaceOmitsIdentitylessTanks() {
		FluidTankGT tFull = new FluidTankGT(1000);
		tFull.add(700, new FluidStack(Fluids.WATER, 1));
		FluidTankGT tEmpty = new FluidTankGT(1000);
		FluidTankGT tOverflow = new FluidTankGT(Long.MAX_VALUE);
		tOverflow.add(OVERFLOW_AMOUNT, new FluidStack(Fluids.LAVA, 1));

		ListTag tList = GT6FluidProvider.tanksTag(new FluidTankGT[] {tFull, tEmpty, tOverflow});
		assertEquals(2, tList.size());
		assertEquals("minecraft:water", tList.getCompound(0).getString(GT6FluidProvider.KEY_FLUID_NAME));
		assertEquals(700L, tList.getCompound(0).getLong(GT6FluidProvider.KEY_AMOUNT));
		assertEquals("minecraft:lava", tList.getCompound(1).getString(GT6FluidProvider.KEY_FLUID_NAME));
		assertEquals(OVERFLOW_AMOUNT, tList.getCompound(1).getLong(GT6FluidProvider.KEY_AMOUNT));

		assertEquals(0, GT6FluidProvider.tanksTag(new FluidTankGT[0]).size());
	}

	/**
	 * The pure line composition (the live pieces — Jade's mB amount face and the fluid
	 * display name — are resolved in appendTooltip; this pins the v1 shape
	 * 「量串 + 空格 + 方括号名」offline).
	 */
	@Test
	public void tankLineIsAmountsThenBracketedName() {
		Component tLine = GT6FluidProvider.tankText("4,000mB / 16,000mB", Component.literal("Water"));
		assertEquals("4,000mB / 16,000mB [Water]", tLine.getString());
	}

	/**
	 * Return-type-driven BlockAccessor double: any method the provider never calls answers
	 * its type's zero value, so the same double serves both legs' diverging interfaces.
	 */
	static BlockAccessor accessorOf(net.minecraft.world.level.block.entity.BlockEntity aBlockEntity) {
		return (BlockAccessor)Proxy.newProxyInstance(GT6FluidProviderTest.class.getClassLoader(),
				new Class<?>[] {BlockAccessor.class},
				(aProxy, aMethod, aArgs) -> {
					if (aMethod.getName().equals("getBlockEntity")) return aBlockEntity;
					Class<?> tReturn = aMethod.getReturnType();
					if (tReturn == boolean.class) return Boolean.FALSE;
					if (tReturn == int.class) return 0;
					if (tReturn == long.class) return 0L;
					if (tReturn == float.class) return 0.0F;
					if (tReturn == double.class) return 0.0D;
					if (tReturn == short.class) return (short)0;
					if (tReturn == byte.class) return (byte)0;
					if (tReturn == char.class) return (char)0;
					return null;
				});
	}
}
