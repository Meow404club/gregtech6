package gregtech6.jade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.Fluids;

import net.minecraftforge.fluids.FluidStack;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import snownee.jade.api.view.ViewGroup;

import gregtech6.fluid.FluidTankGT;
import gregtech6.recipes.RecipeMap;
import gregtech6.tileentity.GTOfflineTestBase;
import gregtech6.tileentity.machines.TileEntityBasicMachine;
import gregtech6.tileentity.machines.TileEntityOven;

/**
 * Offline gate for the Jade universal fluid storage section (task p23-jade-universal-fluid
 * acceptance ②): the server-side data seam pin — {@code groupsOfTarget} produces the two
 * id-stamped {@link ViewGroup}s whose CompoundTag views carry the P22 wire contract keys
 * ({@code FluidName / Amount TAG_LONG / Capacity TAG_LONG}, true longs) — plus the pure
 * text/ratio faces the client {@code readFluid} composes into a {@code FluidView}.
 *
 * <p>The FluidView body itself is live-only: its ctor requires an IElement from
 * {@code IElementHelper.get()} (null offline), so the client half is pinned through its
 * pure parts ({@code currentText}/{@code maxText}/{@code ratioOf}) — never probe
 * Player/Entity-adjacent live objects here (the Forge FluidType.SIZE registry wall).
 *
 * <p>Groups whose views come out empty ride the wire only when non-empty (Jade's
 * {@code ViewGroup.saveList} skips empty groups server-side — jade-1201
 * api/view/ViewGroup.java:66-68 / jade-1211 :97-99); the offline assertions below are on
 * the pre-wire object shape, which is leg-neutral.
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

	/** An offline machine BE — no level needed: the group seam only reads the public tank fields. */
	static TileEntityBasicMachine makeMachine() {
		return sMachineType.create(POS, Blocks.BRICKS.defaultBlockState());
	}

	@Test
	public void fullTanksProduceIdStampedGroupsWithTrueLongAmounts() {
		TileEntityBasicMachine tMachine = makeMachine();
		tMachine.mTanksInput[0].setCapacity(16000);
		tMachine.mTanksInput[0].add(4000, new FluidStack(Fluids.WATER, 1)); // the long-add adopt face (upstream :170-179)
		tMachine.mTanksOutput[0].add(OVERFLOW_AMOUNT, new FluidStack(Fluids.LAVA, 1)); // capacity Long.MAX (no-arg tank) → full adopt
		// mTanksOutput[1] stays empty → no entry

		List<ViewGroup<CompoundTag>> tGroups = GT6FluidProvider.groupsOfTarget(tMachine);
		assertEquals(2, tGroups.size(), "the in and out tank groups always come as a pair");
		assertEquals(GT6FluidProvider.GROUP_IN, tGroups.get(0).id);
		assertEquals(GT6FluidProvider.GROUP_OUT, tGroups.get(1).id);

		// input group: exactly the filled tank, Amount/Capacity as true longs
		List<CompoundTag> tIn = tGroups.get(0).views;
		assertEquals(1, tIn.size());
		CompoundTag tWater = tIn.get(0);
		assertEquals("minecraft:water", tWater.getString(GT6FluidProvider.KEY_FLUID_NAME));
		assertEquals(4000L, tWater.getLong(GT6FluidProvider.KEY_AMOUNT));
		assertEquals(net.minecraft.nbt.Tag.TAG_LONG, tWater.getTagType(GT6FluidProvider.KEY_AMOUNT), "Amount must ride the long face, not the bindInt int face");
		assertEquals(16000L, tWater.getLong(GT6FluidProvider.KEY_CAPACITY));
		assertEquals(net.minecraft.nbt.Tag.TAG_LONG, tWater.getTagType(GT6FluidProvider.KEY_CAPACITY));

		// output group: the overflow tank keeps its 63-bit amount verbatim (> INT_MAX is the C-2 seam)
		List<CompoundTag> tOut = tGroups.get(1).views;
		assertEquals(1, tOut.size(), "the empty second output tank produces no entry");
		CompoundTag tLava = tOut.get(0);
		assertEquals("minecraft:lava", tLava.getString(GT6FluidProvider.KEY_FLUID_NAME));
		assertEquals(OVERFLOW_AMOUNT, tLava.getLong(GT6FluidProvider.KEY_AMOUNT));
		assertTrue(tLava.getLong(GT6FluidProvider.KEY_AMOUNT) > Integer.MAX_VALUE);
		assertEquals(Long.MAX_VALUE, tLava.getLong(GT6FluidProvider.KEY_CAPACITY));
	}

	@Test
	public void fluidlessMachineProducesBothGroupsWithEmptyViews() {
		List<ViewGroup<CompoundTag>> tGroups = GT6FluidProvider.groupsOfTarget(makeMachine());
		assertEquals(2, tGroups.size());
		assertTrue(tGroups.get(0).views.isEmpty(), "the in group is an empty views list pre-wire");
		assertTrue(tGroups.get(1).views.isEmpty(), "the out group is an empty views list pre-wire");
		// (on the wire Jade's saveList skips empty groups entirely, so no stale labels render.)
	}

	@Test
	public void nonBasicMachineBlockEntitiesProduceNoGroups() {
		// the single-branch guard: a GT6 BE outside TileEntityBasicMachine (the oven) and a
		// null target both answer null — which leaves Jade's default capability provider in
		// charge (putData only short-circuits on a non-null group list).
		assertNull(GT6FluidProvider.groupsOfTarget(sOvenType.create(POS, Blocks.BRICKS.defaultBlockState())));
		assertNull(GT6FluidProvider.groupsOfTarget(null));
	}

	@Test
	public void tankViewsOmitIdentitylessTanks() {
		FluidTankGT tFull = new FluidTankGT(1000);
		tFull.add(700, new FluidStack(Fluids.WATER, 1));
		FluidTankGT tEmpty = new FluidTankGT(1000);
		FluidTankGT tOverflow = new FluidTankGT(Long.MAX_VALUE);
		tOverflow.add(OVERFLOW_AMOUNT, new FluidStack(Fluids.LAVA, 1));

		List<CompoundTag> tViews = GT6FluidProvider.tankViews(new FluidTankGT[] {tFull, tEmpty, tOverflow});
		assertEquals(2, tViews.size());
		assertEquals("minecraft:water", tViews.get(0).getString(GT6FluidProvider.KEY_FLUID_NAME));
		assertEquals(700L, tViews.get(0).getLong(GT6FluidProvider.KEY_AMOUNT));
		assertEquals("minecraft:lava", tViews.get(1).getString(GT6FluidProvider.KEY_FLUID_NAME));
		assertEquals(OVERFLOW_AMOUNT, tViews.get(1).getLong(GT6FluidProvider.KEY_AMOUNT));

		assertTrue(GT6FluidProvider.tankViews(new FluidTankGT[0]).isEmpty());
	}

	/**
	 * The pure text/ratio faces that {@code readFluid} (client, live-only) stamps into the
	 * FluidView: true long amounts through Jade's own mB face, and the GTCEu-style 1f clamp
	 * on the fill ratio (GTFluidStorageProvider.java:98).
	 */
	@Test
	public void viewTextCarriesTrueLongAmountsAndRatioClamps() {
		// Jade's own mB face is leg-formatted: 1201 keeps plain "N,NNNmB" below 100k
		// (FluidTextHelper.java:10-11), 1211 always abbreviates to buckets (":7-8" —
		// humanReadableNumber) — each leg shows its own Jade-native standard text.
		//? if forge {
		assertEquals("4,000mB", GT6FluidProvider.currentText(4000));
		assertEquals("16,000mB", GT6FluidProvider.maxText(16000));
		//?} else {
		/*assertEquals("4B", GT6FluidProvider.currentText(4000));
		assertEquals("16B", GT6FluidProvider.maxText(16000));
		*///?}
		// Big longs ride the long face verbatim into the helper — an int-clamped amount
		// would corrupt to 2147483647; both legs abbreviate 3e9 mB the same way.
		assertEquals("3MB", GT6FluidProvider.currentText(OVERFLOW_AMOUNT));

		assertEquals(0.25F, GT6FluidProvider.ratioOf(4000, 16000), 1e-6F);
		assertEquals(1.0F, GT6FluidProvider.ratioOf(OVERFLOW_AMOUNT, 1000), 0.0F, "overfilled ratios clamp to 1");
	}
}
