package gregtech6.tank;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.block.tank.GTBarrelBlock;
import gregtech6.covers.CoverRegistry;
import gregtech6.covers.covers.CoverTextureSimple;
import gregtech6.item.GTBarrelBlockItem;
import gregtech6.item.spraycan.GTSprayCanItem;
import gregtech6.tileentity.GTOfflineTestBase;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;
import gregtech6.tileentity.tank.GTBarrelBlockEntity;
import gregtech6.tileentity.tank.GTBarrelMetalBlockEntity;
import gregtech6.tileentity.tank.TileEntityBase08Barrel;

/**
 * The barrel-family item paint seam (task p23-barrel-paint-item-seam): a spray-painted
 * barrel's drop carries the {@code gt.color}/{@code gt.painted} root-key pair (written
 * by {@code GTBarrelBlock.writeItemNBT} under the {@code TileEntityBase03TicksAndSync
 * .saveAdditional} :322-325 gate shape — the upstream root-key seam is {@code
 * TileEntityBase07Paintable.recolorItem} :89 {@code UT.NBT.set(stack, writeItemNBT(...))}),
 * placement reads the pair back ({@code GTBarrelBlockItem.applyItemNBT} through the BE
 * {@code load} gate, the 03 {@code load} :339-340 assignment pair), and the spray
 * routing ({@code GTSprayCanItem.paintPaintableTE}, the upstream 04:227-235 verbatim
 * three-branch) accepts the barrel family — it always did (TileEntityBase08Barrel rides
 * the IPaintableTE base), the test turns that from coincidence into contract.
 *
 * <p>All 16 family rows (4 barrels + the 12 metal drums + the logistics tank) share
 * this static seam — {@code writeItemNBT}/{@code applyItemNBT} are family-wide, every
 * member's drop/item is a {@code GTBarrelBlock}/{@code GTBarrelBlockItem} — so the
 * wood-class offline fixture exercises the seam the drums ride too. Fixtures follow
 * the TileEntityBase08BarrelTest shape (a mod Block cannot be constructed after the
 * offline boot; the BE fixture mounts a vanilla block).
 */
public class GTBarrelPaintTest extends GTOfflineTestBase {

	static final BlockPos POS = new BlockPos(4, 5, 6);

	/** The CS.DYE_* table values the routing assertions pin (GTPaintableTest's pins). */
	static final int DYE_RED = 0xFF0000; // CS.DYE_Red {255,0,0} = DYES_INT[1]
	static final int DYE_ORANGE = 0xFF8000; // CS.DYE_Orange {255,128,0} = DYES_INT[14]

	/** The GT6 DYE_INDEX order (GTSprayCanItem.DYE_NAMES: Black, Red, ..., Orange, White). */
	static final byte DYE_RED_INDEX = 1;
	static final byte DYE_ORANGE_INDEX = 14;

	static final ResourceLocation TEST_SPRITE = new ResourceLocation("gt6", "block/cover/test_plate");

	static BlockEntityType<GTBarrelBlockEntity> sType;
	static BlockEntityType<GTBarrelMetalBlockEntity> sMetalType;

	@SuppressWarnings("unchecked")
	@BeforeAll
	static void buildOfflineFixtures() {
		BlockEntityType<GTBarrelBlockEntity>[] tHolder = (BlockEntityType<GTBarrelBlockEntity>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new GTBarrelBlockEntity(tHolder[0], aPos, aState),
				Blocks.STONE).build(null);
		sType = tHolder[0];

		BlockEntityType<GTBarrelMetalBlockEntity>[] tMetal = (BlockEntityType<GTBarrelMetalBlockEntity>[]) new BlockEntityType<?>[1];
		tMetal[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new GTBarrelMetalBlockEntity(tMetal[0], aPos, aState),
				Blocks.STONE).build(null);
		sMetalType = tMetal[0];
	}

	@AfterEach
	void resetCoverFixtures() {
		CoverRegistry.reset();
	}

	static GTBarrelBlockEntity barrel() {
		return sType.create(POS, Blocks.STONE.defaultBlockState());
	}

	static GTBarrelMetalBlockEntity metalBarrel() {
		return sMetalType.create(POS, Blocks.STONE.defaultBlockState());
	}

	/** The item-root paint pair as the leg carries it: the forge root tag, the 21.1 CUSTOM_DATA envelope (the GTItemPaintTint read form). */
	static CompoundTag paintTagOf(ItemStack aDrop) {
		//? if forge {
		return aDrop.hasTag() ? aDrop.getTag() : new CompoundTag();
		//?} else {
		/*return aDrop.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
				net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
		 *///?}
	}

	// ---------------------------------------------------------------------------
	// the write half: the root-key pair under the 03 saveAdditional gate shape
	// ---------------------------------------------------------------------------

	@Test
	public void paintedDropCarriesTheRootKeyPair() {
		GTBarrelBlockEntity tBe = barrel();
		assertTrue(tBe.mixPaint(DYE_RED), "the spray route paints the barrel (the NBT layer already could)");

		ItemStack tDrop = GTBarrelBlock.writeItemNBT(tBe, new ItemStack(Items.GLASS_BOTTLE));
		CompoundTag tTag = paintTagOf(tDrop);
		assertTrue(tTag.contains(TileEntityBase03TicksAndSync.NBT_COLOR, Tag.TAG_INT), "gt.color as an Integer (upstream CS.java:1161)");
		assertTrue(tTag.contains(TileEntityBase03TicksAndSync.NBT_PAINTED, Tag.TAG_BYTE), "gt.painted as a Boolean (upstream CS.java:1162)");
		assertEquals(DYE_RED, tTag.getInt(TileEntityBase03TicksAndSync.NBT_COLOR));
		assertTrue(tTag.getBoolean(TileEntityBase03TicksAndSync.NBT_PAINTED));

		// the empty painted barrel: the pair rides at the ROOT, no tank wrapper — the 07Paintable :89 seam
		assertFalse(tTag.contains(TileEntityBase08Barrel.NBT_TANK), "an empty painted barrel carries ONLY the paint pair");
		//? if forge {
		assertTrue(tDrop.hasTag(), "a painted barrel's drop is no longer tag-less (the paint pair IS the tag)");
		//?} else {
		/*assertFalse(tDrop.get(gregtech6.registry.GT6DataComponents.BARREL_CONTENT) != null, "the tank payload component stays absent");
		 *///?}
		assertFalse(GTBarrelBlockItem.hasContent(tDrop), "paint does not count as content (the :290 stacking rule unchanged)");
	}

	@Test
	public void unpaintedEmptyDropKeepsTheLegacyShape() {
		GTBarrelBlockEntity tBe = barrel();
		ItemStack tDrop = GTBarrelBlock.writeItemNBT(tBe, new ItemStack(Items.GLASS_BOTTLE));
		//? if forge {
		assertFalse(tDrop.hasTag(), "no paint, no content, no covers → the tag-less pre-card drop, byte-identical");
		//?} else {
		/*assertFalse(tDrop.get(gregtech6.registry.GT6DataComponents.BARREL_CONTENT) != null, "no tank payload");
		assertFalse(tDrop.has(net.minecraft.core.component.DataComponents.CUSTOM_DATA), "no paint → the 21.1 envelope stays absent");
		 *///?}
		assertFalse(GTBarrelBlockItem.hasContent(tDrop));
	}

	/**
	 * The gate shape: the unpainted barrel never writes {@code gt.painted}, so a drop
	 * that carries OTHER keys (content) still shows no paint keys — the 03 saveAdditional
	 * :322-325 mirror (colour only beside the flag).
	 */
	@Test
	public void unpaintedFilledDropCarriesNoPaintKeys() {
		GTBarrelBlockEntity tBe = barrel();
		tBe.mTank.fill(new FluidStack(Fluids.WATER, 1234), FluidAction.EXECUTE);

		ItemStack tDrop = GTBarrelBlock.writeItemNBT(tBe, new ItemStack(Items.GLASS_BOTTLE));
		CompoundTag tTag = paintTagOf(tDrop);
		assertFalse(tTag.contains(TileEntityBase03TicksAndSync.NBT_COLOR), "unpainted → no gt.color written");
		assertFalse(tTag.contains(TileEntityBase03TicksAndSync.NBT_PAINTED), "unpainted → no gt.painted written");
		//? if forge {
		CompoundTag tTankTag = tTag.getCompound(TileEntityBase08Barrel.NBT_TANK);
		//?} else {
		/*CompoundTag tTankTag = tDrop.get(gregtech6.registry.GT6DataComponents.BARREL_CONTENT).copyTag()
				.getCompound(TileEntityBase08Barrel.NBT_TANK); // 21.1: the tank rides its own component
		 *///?}
		assertEquals("minecraft:water", tTankTag.getString("FluidName"), "the tank keys byte-unchanged");
		assertEquals(1234, tTankTag.getInt("Amount"));
	}

	// ---------------------------------------------------------------------------
	// the read half: placement round trips (the applyItemNBT load-gate seam)
	// ---------------------------------------------------------------------------

	@Test
	public void paintedDropRoundTripsThroughPlacement() {
		GTBarrelBlockEntity tBe = barrel();
		tBe.mixPaint(DYE_RED);
		ItemStack tDrop = GTBarrelBlock.writeItemNBT(tBe, new ItemStack(Items.GLASS_BOTTLE));

		GTBarrelBlockEntity tPlaced = barrel();
		GTBarrelBlockItem.applyItemNBT(tDrop, tPlaced);
		assertTrue(tPlaced.isPainted(), "placement rehydrates the painted flag");
		assertEquals(DYE_RED, tPlaced.getPaint(), "placement rehydrates the colour");
	}

	/**
	 * The painted-white pin — the reason the readback rides the {@code load} gate instead
	 * of the {@code IPaintableTE} API: {@code paint}/{@code mixPaint} no-op on the
	 * {@code aRGB != mRGBa} guard (upstream :85), so a fresh UNCOLORED barrel cannot even
	 * be painted white through the spray face — the painted-white state (e.g. a give/
	 * craft form with {@code gt.painted=true} + {@code gt.color=0xFFFFFF}) arrives through
	 * the load gate, which assigns the flag directly. The item seam must round-trip it
	 * faithfully.
	 */
	@Test
	public void paintedWhiteBarrelKeepsItsFlagThroughPlacement() {
		assertEquals(0xFFFFFF, GTSprayCanItem.DYES_INT[15], "white is sprayable (CS.DYE_White) — the flag matters even at white");
		CompoundTag tSaved = new CompoundTag();
		tSaved.putInt(TileEntityBase03TicksAndSync.NBT_COLOR, TileEntityBase03TicksAndSync.UNCOLORED);
		tSaved.putBoolean(TileEntityBase03TicksAndSync.NBT_PAINTED, true);
		GTBarrelBlockEntity tBe = barrel();
		tBe.load(tSaved); // the chunk-data route: the 03 load :339-340 pair assigns the flag directly
		assertTrue(tBe.isPainted(), "white paint is still paint (the load gate form)");
		ItemStack tDrop = GTBarrelBlock.writeItemNBT(tBe, new ItemStack(Items.GLASS_BOTTLE));
		CompoundTag tTag = paintTagOf(tDrop);
		assertTrue(tTag.getBoolean(TileEntityBase03TicksAndSync.NBT_PAINTED), "the drop carries painted=true");
		assertEquals(TileEntityBase03TicksAndSync.UNCOLORED, tTag.getInt(TileEntityBase03TicksAndSync.NBT_COLOR), "with the white colour");

		GTBarrelBlockEntity tPlaced = barrel();
		GTBarrelBlockItem.applyItemNBT(tDrop, tPlaced);
		assertTrue(tPlaced.isPainted(), "the painted-white flag survives the drop→place round trip");
		assertEquals(TileEntityBase03TicksAndSync.UNCOLORED, tPlaced.getPaint());
	}

	@Test
	public void unpaintedItemLeavesThePlacementUnpainted() {
		// a tag-less stack (creative-picked fresh barrel) — the early return
		GTBarrelBlockEntity tPlaced = barrel();
		GTBarrelBlockItem.applyItemNBT(new ItemStack(Items.GLASS_BOTTLE), tPlaced);
		assertFalse(tPlaced.isPainted(), "no tag → no paint read");
		assertEquals(TileEntityBase03TicksAndSync.UNCOLORED, tPlaced.getPaint());

		// an explicit gt.painted=false (the hasKey-guard pair's false arm — load assigns the flag directly)
		ItemStack tStack = new ItemStack(Items.GLASS_BOTTLE);
		//? if forge {
		tStack.getOrCreateTag().putBoolean(TileEntityBase03TicksAndSync.NBT_PAINTED, false);
		//?} else {
		/*CompoundTag tPre = new CompoundTag();
		tPre.putBoolean(TileEntityBase03TicksAndSync.NBT_PAINTED, false);
		net.minecraft.world.item.component.CustomData.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, tStack, tPre);
		 *///?}
		GTBarrelBlockEntity tPlaced2 = barrel();
		GTBarrelBlockItem.applyItemNBT(tStack, tPlaced2);
		assertFalse(tPlaced2.isPainted(), "painted=false → the unpainted BE");
		assertEquals(TileEntityBase03TicksAndSync.UNCOLORED, tPlaced2.getPaint());
	}

	/**
	 * The coexistence pin: paint keys ride BESIDE the pre-card keys (tank + covers) —
	 * the existing keys stay byte-identical and the full drop→place round trip restores
	 * content, cover AND colour together (the metal fixture: the base-default cover host).
	 */
	@Test
	public void paintRidesAlongsideTheTankAndCoverKeys() {
		CoverRegistry.reset();
		CoverRegistry.put(Items.IRON_INGOT, new CoverTextureSimple(TEST_SPRITE));

		GTBarrelMetalBlockEntity tBe = metalBarrel();
		tBe.mTank.fill(new FluidStack(Fluids.WATER, 1234), FluidAction.EXECUTE);
		assertTrue(tBe.setCoverItem((byte) 1, new ItemStack(Items.IRON_INGOT), null, false, true), "the cover installs (GTPaintableTest mount shape)");
		tBe.mixPaint(DYE_RED);

		ItemStack tDrop = GTBarrelBlock.writeItemNBT(tBe, new ItemStack(Items.GLASS_BOTTLE));
		CompoundTag tTag = paintTagOf(tDrop);
		//? if forge {
		CompoundTag tTankTag = tTag.getCompound(TileEntityBase08Barrel.NBT_TANK);
		//?} else {
		/*CompoundTag tTankTag = tDrop.get(gregtech6.registry.GT6DataComponents.BARREL_CONTENT).copyTag()
				.getCompound(TileEntityBase08Barrel.NBT_TANK); // 21.1: the tank rides its own component
		 *///?}
		assertEquals("minecraft:water", tTankTag.getString("FluidName"), "the tank key unchanged beside the paint pair");
		assertEquals(1234, tTankTag.getInt("Amount"));
		//? if forge {
		assertTrue(tTag.contains("covers", Tag.TAG_COMPOUND), "the covers key unchanged (ICoverableTE.NBT_COVERS)");
		//?} else {
		/*assertTrue(tDrop.get(gregtech6.registry.GT6DataComponents.COVER_PAYLOAD) != null, "the covers payload unchanged");
		 *///?}
		assertEquals(DYE_RED, tTag.getInt(TileEntityBase03TicksAndSync.NBT_COLOR));
		assertTrue(tTag.getBoolean(TileEntityBase03TicksAndSync.NBT_PAINTED));
		//? if forge {
		assertTrue(tDrop.hasTag(), "the drop carries one root tag with every key family");
		//?} else {
		/*assertTrue(tDrop.get(gregtech6.registry.GT6DataComponents.BARREL_CONTENT) != null, "the tank payload component");
		assertTrue(tDrop.get(gregtech6.registry.GT6DataComponents.COVER_PAYLOAD) != null, "the cover payload component");
		 *///?}

		GTBarrelMetalBlockEntity tPlaced = metalBarrel();
		GTBarrelBlockItem.applyItemNBT(tDrop, tPlaced);
		assertEquals(1234, tPlaced.mTank.amount(), "content survives beside the colour");
		assertTrue(tPlaced.getCovers() != null, "the cover survives beside the colour");
		assertTrue(tPlaced.isPainted(), "the colour survives beside content + cover");
		assertEquals(DYE_RED, tPlaced.getPaint());
	}

	// ---------------------------------------------------------------------------
	// the routing pin: GTSprayCanItem.paintPaintableTE three branches on the barrel
	// fixture (upstream 04:227-235 — remover→unpaint / painted→mix / unpainted→paint)
	// ---------------------------------------------------------------------------

	@Test
	public void sprayRoutingThreeBranchesOnTheBarrelFixture() {
		// the index ties (the GTPaintableTest colour pins, now tied to the spray-can table)
		assertEquals(DYE_RED, GTSprayCanItem.DYES_INT[DYE_RED_INDEX]);
		assertEquals(DYE_ORANGE, GTSprayCanItem.DYES_INT[DYE_ORANGE_INDEX]);

		// the remover arm → unpaint; the second unpaint is the no-op miss
		GTBarrelBlockEntity tPainted = barrel();
		tPainted.mixPaint(DYE_RED);
		assertTrue(GTSprayCanItem.paintPaintableTE(tPainted, GTSprayCanItem.REMOVER), "the remover hits a painted barrel");
		assertFalse(tPainted.isPainted());
		assertEquals(TileEntityBase03TicksAndSync.UNCOLORED, tPainted.getPaint());
		assertFalse(GTSprayCanItem.paintPaintableTE(tPainted, GTSprayCanItem.REMOVER), "unpaint on unpainted = the miss (no payment)");

		// the unpainted half → paint (the colour stores directly)
		GTBarrelBlockEntity tFresh = barrel();
		assertTrue(GTSprayCanItem.paintPaintableTE(tFresh, DYE_RED_INDEX));
		assertTrue(tFresh.isPainted());
		assertEquals(DYE_RED, tFresh.getPaint());

		// the same-colour spray → mixPaint no-op (mix(Red, Red) = Red == current, upstream :164 miss)
		assertFalse(GTSprayCanItem.paintPaintableTE(tFresh, DYE_RED_INDEX), "the same-colour spray is the no-op miss");

		// the painted half → mixPaint channel average (UT.java:1576-1578, the GTPaintableTest pin)
		assertTrue(GTSprayCanItem.paintPaintableTE(tFresh, DYE_ORANGE_INDEX));
		assertEquals(0xFF4000, tFresh.getPaint(), "mix(255,0,0 , 255,128,0) = 255,64,0");
	}
}
