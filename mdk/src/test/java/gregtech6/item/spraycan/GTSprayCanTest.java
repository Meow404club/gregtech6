package gregtech6.item.spraycan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;

import gregtech6.registry.GT6SprayCans;
import gregtech6.registry.GTGrassBlocks;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;
import gregtech6.tileentity.machines.TileEntityOven;

/**
 * The spray-can offline acceptance (task p22-spraycan-items): the {@link GTSprayCanItem}
 * pure seams — the upstream DYES_INT census, the {@code ~mColor&15} vanilla-Dye fold, the
 * colorize/decolorize whitelist tables (upstream Behavior_Spray_Color.java:144-167 / Remover
 * :96-106 minus the two target-less arms), the gt.remaining uses ledger (:68/:78/:83/:85-92),
 * the durability-bar rule (the GTCEu :126-145 face) and the TE routing (the 04:227-235 shape
 * over the oven fixture, the GTPaintableTest shape) — plus the registry face (GT6SprayCans
 * 18 items + tab, the GT6ToolsCreativeTabTest form).
 *
 * <p>The mod-Item wall (CrowbarTest.bootStrap NOTE) bars constructing GTSprayCanItem here,
 * so the ItemStack seams run on vanilla stand-ins and the live {@code useOn} half rides the
 * runServer smoke line. The sticky-clinit guard: bootstrap BEFORE the first
 * GTSprayCanItem/GT6SprayCans touch — their static init resolves vanilla blocks (the
 * GT6ToolsCreativeTabTest.boot shape; the NetworkHooks failure is offline-expected).
 */
public class GTSprayCanTest {

	static BlockEntityType<TileEntityOven> sOvenType;

	static final BlockPos POS = new BlockPos(1, 2, 3);

	@BeforeAll
	static void boot() {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// NetworkHooks.init() failure is expected offline; registries are ready by now.
		}
		@SuppressWarnings("unchecked")
		BlockEntityType<TileEntityOven>[] tHolder = (BlockEntityType<TileEntityOven>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new TileEntityOven(tHolder[0], aPos, aState),
				Blocks.BRICKS).build(null);
		sOvenType = tHolder[0];
	}

	static TileEntityOven oven() {
		return new TileEntityOven(sOvenType, POS, Blocks.BRICKS.defaultBlockState());
	}

	private static ResourceLocation rl(String aPath) {
		return new ResourceLocation("gt6", aPath);
	}

	/** The stack carrier read — 1.20.1 freeform NBT, 21.1 the opaque CUSTOM_DATA envelope (the item's own fork shape); a payload-less stack reads as the empty tag. */
	private static CompoundTag carrierOf(ItemStack aStack) {
		//? if forge {
		return aStack.hasTag() ? aStack.getTag() : new CompoundTag();
		//?} else {
		/*return aStack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
				net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
		*///?}
	}

	/** The stack carrier write — the mirror of {@link #carrierOf} (the envelope is set, not mutated, on 21.1). */
	private static void writeCarrier(ItemStack aStack, String aKey, long aValue) {
		//? if forge {
		aStack.getOrCreateTag().putLong(aKey, aValue);
		//?} else {
		/*net.minecraft.nbt.CompoundTag tTag = aStack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
				net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
		tTag.putLong(aKey, aValue);
		aStack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
				net.minecraft.world.item.component.CustomData.of(tTag));
		*///?}
	}

	// ---------------------------------------------------------------------------
	// the upstream DYES_INT census (CS.java:470 — GTMachineCommand.DYES_INT mirrors it)
	// ---------------------------------------------------------------------------

	@Test
	public void dyesIntIsTheUpstreamTable() {
		int[] tExpected = {0x202020, 0xFF0000, 0x00FF00, 0x604000, 0x0000FF, 0x800080, 0x00FFFF,
				0xC0C0C0, 0x808080, 0xFFC0C0, 0x80FF80, 0xFFFF00, 0x8080FF, 0xFF00FF, 0xFF8000, 0xFFFFFF};
		assertEquals(16, GTSprayCanItem.DYES_INT.length);
		for (int i = 0; i < 16; i++) assertEquals(tExpected[i], GTSprayCanItem.DYES_INT[i], "DYES_INT[" + i + "]");
		// the GTPaintableTest pins (three-point cross-check against the CS.DYE_* rows)
		assertEquals(0xFF0000, GTSprayCanItem.DYES_INT[1]);
		assertEquals(0xFF8000, GTSprayCanItem.DYES_INT[14]);
		assertEquals(0x202020, GTSprayCanItem.DYES_INT[0]);
	}

	@Test
	public void nameTablesAreAlignedSixteenWays() {
		assertEquals(16, GTSprayCanItem.DYE_NAMES.length);
		assertEquals(16, GTSprayCanItem.DYE_IDS.length);
		Set<String> tIds = new HashSet<>();
		for (String tId : GTSprayCanItem.DYE_IDS) assertTrue(tIds.add(tId), "distinct id " + tId);
		assertEquals("Black", GTSprayCanItem.DYE_NAMES[0]);
		assertEquals("White", GTSprayCanItem.DYE_NAMES[15]);
	}

	// ---------------------------------------------------------------------------
	// the ~mColor&15 fold (P21 ADR ruling 3) + the whitelist tables
	// ---------------------------------------------------------------------------

	@Test
	public void vanillaDyeFoldIsTheComplementBijection() {
		Set<DyeColor> tSeen = new HashSet<>();
		for (byte i = 0; i < 16; i++) {
			DyeColor tDye = GTSprayCanItem.vanillaDye(i);
			assertNotNull(tDye);
			assertTrue(tSeen.add(tDye), "bijective at " + i);
			assertEquals(15 - i, tDye.getId(), "the ~i&15 fold at " + i);
		}
		assertEquals(16, tSeen.size(), "every vanilla dye reachable");
		assertEquals(DyeColor.BLACK, GTSprayCanItem.vanillaDye((byte) 0));
		assertEquals(DyeColor.WHITE, GTSprayCanItem.vanillaDye((byte) 15));
		assertEquals(DyeColor.GREEN, GTSprayCanItem.vanillaDye((byte) 2));
	}

	/** The four recolourable families resolve 16 distinct targets each (the upstream :149-151/:164 faces). */
	@Test
	public void colorTargetsAreCompleteOverAllSixteenDyes() {
		Set<Block> tWoolSeen = new LinkedHashSet<>(), tCarpetSeen = new LinkedHashSet<>(),
				tGlassSeen = new LinkedHashSet<>(), tPaneSeen = new LinkedHashSet<>(), tTerracottaSeen = new LinkedHashSet<>();
		// the white-dye spray on a WHITE block is the :164 same-colour no-op (pinned in
		// colorTargetSameColourIsTheNoOp) — walk the 15 changing dyes for the white bases
		for (byte i = 0; i < 15; i++) {
			assertNotNull(GTSprayCanItem.colorTarget(Blocks.WHITE_WOOL, i));
			assertNotNull(GTSprayCanItem.colorTarget(Blocks.WHITE_CARPET, i));
			assertNotNull(GTSprayCanItem.colorTarget(Blocks.GLASS, i));
			assertNotNull(GTSprayCanItem.colorTarget(Blocks.GLASS_PANE, i));
			assertNotNull(GTSprayCanItem.colorTarget(Blocks.TERRACOTTA, i));
			tWoolSeen.add(GTSprayCanItem.colorTarget(Blocks.WHITE_WOOL, i));
			tCarpetSeen.add(GTSprayCanItem.colorTarget(Blocks.WHITE_CARPET, i));
			tGlassSeen.add(GTSprayCanItem.colorTarget(Blocks.GLASS, i));
			tPaneSeen.add(GTSprayCanItem.colorTarget(Blocks.GLASS_PANE, i));
			tTerracottaSeen.add(GTSprayCanItem.colorTarget(Blocks.TERRACOTTA, i));
		}
		// the white spray completes every family over a non-white base (white wool → white wool)
		assertNotNull(GTSprayCanItem.colorTarget(Blocks.BLACK_WOOL, (byte) 15));
		assertNotNull(GTSprayCanItem.colorTarget(Blocks.BLACK_CARPET, (byte) 15));
		assertNotNull(GTSprayCanItem.colorTarget(Blocks.BLACK_TERRACOTTA, (byte) 15));
		tWoolSeen.add(GTSprayCanItem.colorTarget(Blocks.BLACK_WOOL, (byte) 15));
		tCarpetSeen.add(GTSprayCanItem.colorTarget(Blocks.BLACK_CARPET, (byte) 15));
		tGlassSeen.add(GTSprayCanItem.colorTarget(Blocks.GLASS, (byte) 15));
		tPaneSeen.add(GTSprayCanItem.colorTarget(Blocks.GLASS_PANE, (byte) 15));
		tTerracottaSeen.add(GTSprayCanItem.colorTarget(Blocks.TERRACOTTA, (byte) 15));
		assertEquals(16, tWoolSeen.size(), "16 distinct wool targets");
		assertEquals(16, tCarpetSeen.size(), "16 distinct carpet targets");
		assertEquals(16, tGlassSeen.size(), "16 distinct stained-glass targets");
		assertEquals(16, tPaneSeen.size(), "16 distinct stained-pane targets");
		assertEquals(16, tTerracottaSeen.size(), "16 distinct stained-terracotta targets");
	}

	/** Spot rows against the vanilla registry names (upstream :149-151 colour code folded). */
	@Test
	public void colorTargetSpots() {
		// GT6 index 1 = Red → vanilla RED (the fold), the upstream :248 fake-recipe rows
		assertEquals(Blocks.RED_STAINED_GLASS, GTSprayCanItem.colorTarget(Blocks.GLASS, (byte) 1));
		assertEquals(Blocks.RED_STAINED_GLASS_PANE, GTSprayCanItem.colorTarget(Blocks.GLASS_PANE, (byte) 1));
		assertEquals(Blocks.RED_TERRACOTTA, GTSprayCanItem.colorTarget(Blocks.TERRACOTTA, (byte) 1));
		// index 0 = Black → black wool (the upstream :248 ST.make(Blocks.wool, 1, 15-i) row)
		assertEquals(Blocks.BLACK_WOOL, GTSprayCanItem.colorTarget(Blocks.WHITE_WOOL, (byte) 0));
		// index 10 = Lime → lime carpet
		assertEquals(Blocks.LIME_CARPET, GTSprayCanItem.colorTarget(Blocks.WHITE_CARPET, (byte) 10));
		// family recolour of an already-coloured block (the upstream :164 setBlockMetadata arm)
		assertEquals(Blocks.BLACK_WOOL, GTSprayCanItem.colorTarget(Blocks.RED_WOOL, (byte) 0));
		assertEquals(Blocks.BLACK_STAINED_GLASS, GTSprayCanItem.colorTarget(Blocks.RED_STAINED_GLASS, (byte) 0));
	}

	/** The same-colour spray is the no-op (upstream :164 guards metadata != target). */
	@Test
	public void colorTargetSameColourIsTheNoOp() {
		assertNull(GTSprayCanItem.colorTarget(Blocks.BLACK_WOOL, (byte) 0));
		assertNull(GTSprayCanItem.colorTarget(Blocks.WHITE_WOOL, (byte) 15));
		assertNull(GTSprayCanItem.colorTarget(Blocks.LIME_CARPET, (byte) 10));
		assertNull(GTSprayCanItem.colorTarget(Blocks.BLACK_STAINED_GLASS, (byte) 0));
		assertNull(GTSprayCanItem.colorTarget(Blocks.BLACK_STAINED_GLASS_PANE, (byte) 0));
		assertNull(GTSprayCanItem.colorTarget(Blocks.BLACK_TERRACOTTA, (byte) 0));
		// but the plain families always change
		assertNotNull(GTSprayCanItem.colorTarget(Blocks.GLASS, (byte) 0));
		assertNotNull(GTSprayCanItem.colorTarget(Blocks.TERRACOTTA, (byte) 0));
	}

	/**
	 * The declared target-less arms: everything outside the whitelist (:146-148
	 * stone/bricks/dirt) and the vanilla grass block under a NON-grass dye (the p24 grass
	 * arm is LIVE — its six-dye table covers Green/Lime/Black/LightGray/Yellow/Brown, and
	 * dye 1 Red is one of the ten no-op dyes, the :161 return F face).
	 */
	@Test
	public void colorTargetTargetlessArms() {
		assertNull(GTSprayCanItem.colorTarget(Blocks.GRASS_BLOCK, (byte) 1), "Red on grass = one of the ten no-op dyes");
		assertNull(GTSprayCanItem.colorTarget(Blocks.STONE, (byte) 1));
		assertNull(GTSprayCanItem.colorTarget(Blocks.BRICKS, (byte) 1));
		assertNull(GTSprayCanItem.colorTarget(Blocks.DIRT, (byte) 1));
		assertNull(GTSprayCanItem.colorTarget(null, (byte) 1));
	}

	/** The remover reverse rows (upstream Remover :101-103 verbatim; wool/carpet have no plain variant). */
	@Test
	public void decolorTargetsAreTheReverseRows() {
		for (byte i = 0; i < 16; i++) {
			assertEquals(Blocks.GLASS, GTSprayCanItem.decolorTarget(GTSprayCanItem.colorTarget(Blocks.GLASS, i)),
					":103 at dye " + i);
			assertEquals(Blocks.GLASS_PANE, GTSprayCanItem.decolorTarget(GTSprayCanItem.colorTarget(Blocks.GLASS_PANE, i)),
					":102 at dye " + i);
			assertEquals(Blocks.TERRACOTTA, GTSprayCanItem.decolorTarget(GTSprayCanItem.colorTarget(Blocks.TERRACOTTA, i)),
					":101 at dye " + i);
		}
		assertNull(GTSprayCanItem.decolorTarget(Blocks.WHITE_WOOL), "no uncoloured wool (upstream has no wool arm)");
		assertNull(GTSprayCanItem.decolorTarget(Blocks.WHITE_CARPET), "no uncoloured carpet");
		assertNull(GTSprayCanItem.decolorTarget(Blocks.GLASS), "plain glass is not removable");
		assertNull(GTSprayCanItem.decolorTarget(Blocks.TERRACOTTA));
		assertNull(GTSprayCanItem.decolorTarget(Blocks.STONE));
		assertNull(GTSprayCanItem.decolorTarget(Blocks.GRASS_BLOCK));
		assertNull(GTSprayCanItem.decolorTarget(null));
	}

	// ---------------------------------------------------------------------------
	// the grass arm (task p24-grass-block, the upstream :153-162 route + Remover :104)
	// ---------------------------------------------------------------------------

	/**
	 * The six variant stand-ins (the GTGrassBlocks offline seam — the mod blocks never
	 * register here). Deliberately NOT wool/carpet/terracotta/glass members: those live in
	 * the FAMILY_OF whitelist and would route the recolour through the WOOL arm before the
	 * grass arm is ever consulted — these six are family-less vanilla blocks.
	 */
	private static final Block[] GRASS_STANDINS = {
			Blocks.SANDSTONE, Blocks.GRAVEL, Blocks.ANDESITE,
			Blocks.DRIPSTONE_BLOCK, Blocks.CALCITE, Blocks.TUFF};

	/** The six dye indexes the grass arm answers, variant order (Behavior_Spray_Color.java:154-160). */
	private static final byte[] GRASS_DYES = {2, 10, 0, 7, 11, 3};

	/**
	 * The six effective dyes map the vanilla grass block AND every already-coloured GT
	 * variant onto the right variant (the upstream :154-160 switch, family recolour
	 * included); a variant sprayed with its own dye is the :164 no-op.
	 */
	@Test
	public void grassSprayMapsTheSixDyes() {
		GTGrassBlocks.useTestStandins(java.util.List.of(GRASS_STANDINS));
		try {
			// vanilla grass block → the dye's variant (all six, in :154-160 order)
			for (int i = 0; i < 6; i++) {
				assertSame(GRASS_STANDINS[i], GTSprayCanItem.colorTarget(Blocks.GRASS_BLOCK, GRASS_DYES[i]),
						"vanilla grass + dye " + GRASS_DYES[i] + " -> variant " + i);
			}
			// family recolour: any variant + any effective dye -> that dye's variant
			for (int from = 0; from < 6; from++) {
				for (int to = 0; to < 6; to++) {
					Block tExpected = GRASS_STANDINS[to];
					Block tActual = GTSprayCanItem.colorTarget(GRASS_STANDINS[from], GRASS_DYES[to]);
					if (from == to) assertNull(tActual, "same-variant spray is the :164 no-op");
					else assertSame(tExpected, tActual, "variant " + from + " + dye " + GRASS_DYES[to]);
				}
			}
		} finally {
			GTGrassBlocks.resetResolver();
		}
	}

	/**
	 * The other TEN dyes are the upstream :161 return F on the vanilla grass block AND on
	 * every GT variant — the useOn :216-217 precheck sees a null target and never pays
	 * (no durability, no sound, the task-card no-op semantics).
	 */
	@Test
	public void grassSprayIgnoresTheOtherTenDyes() {
		GTGrassBlocks.useTestStandins(java.util.List.of(GRASS_STANDINS));
		try {
			for (byte tDye = 0; tDye < 16; tDye++) {
				boolean tGrassDye = false;
				for (byte tEffective : GRASS_DYES) tGrassDye |= tEffective == tDye;
				if (tGrassDye) continue;
				assertNull(GTSprayCanItem.colorTarget(Blocks.GRASS_BLOCK, tDye),
						"dye " + tDye + " must no-op on the vanilla grass block");
				for (Block tVariant : GRASS_STANDINS) {
					assertNull(GTSprayCanItem.colorTarget(tVariant, tDye),
							"dye " + tDye + " must no-op on a GT variant");
				}
			}
		} finally {
			GTGrassBlocks.resetResolver();
		}
	}

	/** The remover unpaints ANY GT variant to the vanilla grass block (the Remover :104 swap); the vanilla block itself is not removable. */
	@Test
	public void grassRemoverUnpaintsToTheVanillaBlock() {
		GTGrassBlocks.useTestStandins(java.util.List.of(GRASS_STANDINS));
		try {
			for (Block tVariant : GRASS_STANDINS) {
				assertSame(Blocks.GRASS_BLOCK, GTSprayCanItem.decolorTarget(tVariant), "Remover :104");
			}
			assertNull(GTSprayCanItem.decolorTarget(Blocks.GRASS_BLOCK), "the target itself has no reverse row");
		} finally {
			GTGrassBlocks.resetResolver();
		}
	}

	// ---------------------------------------------------------------------------
	// the TE routing (the 04:227-235 shape over the oven fixture)
	// ---------------------------------------------------------------------------

	@Test
	public void teRoutingPaintsAnUnpaintedMachineDirectly() {
		TileEntityOven tOven = oven();
		assertFalse(tOven.isPainted());
		assertTrue(GTSprayCanItem.paintPaintableTE(tOven, (byte) 1), "upstream :85 — the colour change reports true");
		assertTrue(tOven.isPainted());
		assertEquals(0xFF0000, tOven.getPaint(), "unpainted takes the colour as-is (the 04:229 half)");
	}

	@Test
	public void teRoutingMixesAnAlreadyPaintedMachine() {
		TileEntityOven tOven = oven();
		assertTrue(GTSprayCanItem.paintPaintableTE(tOven, (byte) 1)); // Red
		assertTrue(GTSprayCanItem.paintPaintableTE(tOven, (byte) 14)); // Orange
		assertEquals(0xFF4000, tOven.getPaint(), "painted MIXES by channel average (the UT :1576-1578 math)");
	}

	@Test
	public void teRoutingRemoverUnpaintsAndNoOpsClean() {
		TileEntityOven tOven = oven();
		assertTrue(GTSprayCanItem.paintPaintableTE(tOven, (byte) 1));
		assertTrue(GTSprayCanItem.paintPaintableTE(tOven, GTSprayCanItem.REMOVER), "the Remover :98 decolorable arm");
		assertFalse(tOven.isPainted());
		assertEquals(TileEntityBase03TicksAndSync.UNCOLORED, tOven.getPaint());
		assertFalse(GTSprayCanItem.paintPaintableTE(tOven, GTSprayCanItem.REMOVER), "unpainted remover spray is the no-op — no use paid");
	}

	// ---------------------------------------------------------------------------
	// the gt.remaining uses ledger
	// ---------------------------------------------------------------------------

	@Test
	public void remainingOfDefaultsToFull() {
		assertEquals(5120, GTSprayCanItem.remainingOf(null, 5120), "a tag-less stack is a full can");
		assertEquals(5120, GTSprayCanItem.remainingOf(new CompoundTag(), 5120), "an empty tag is a full can");
		CompoundTag tTag = new CompoundTag();
		tTag.putLong(GTSprayCanItem.NBT_REMAINING, 100);
		assertEquals(100, GTSprayCanItem.remainingOf(tTag, 5120));
	}

	@Test
	public void remainingAfterHitPaysTenOrNothing() {
		assertEquals(5110, GTSprayCanItem.remainingAfterHit(5120, false), "upstream :78 — one hit = 10 internal units");
		assertEquals(5120, GTSprayCanItem.remainingAfterHit(5120, true), "creative (hasInfiniteItems) pays nothing");
		assertEquals(0, GTSprayCanItem.remainingAfterHit(5, false), "floored at 0");
		assertFalse(GTSprayCanItem.depleted(10));
		assertTrue(GTSprayCanItem.depleted(0), "upstream :85 tUses <= 0");
	}

	/**
	 * The payment face on a real stack — vanilla stand-ins (mod Items are not constructible
	 * here): 512 paid hits (5120 internal units) end in the empty-can swap (upstream :85-92).
	 */
	@Test
	public void payUsesDecrementsThenSwapsToTheEmptyCan() {
		Item tEmpty = Items.GOLD_INGOT; // the empty-can stand-in
		ItemStack tCan = new ItemStack(Items.IRON_INGOT); // the can stand-in
		assertNull(GTSprayCanItem.payUses(tCan, 5120, tEmpty), "a paid hit returns no replacement");
		assertEquals(5110, carrierOf(tCan).getLong(GTSprayCanItem.NBT_REMAINING), "the gt.remaining write (upstream :83)");

		// hits #2..#511 keep the can alive (5110 - 510*10 = 10 left after the loop)
		for (int i = 0; i < 510; i++) {
			assertNull(GTSprayCanItem.payUses(tCan, 5120, tEmpty), "hit #" + (i + 2) + " must not deplete yet");
		}
		assertEquals(10, carrierOf(tCan).getLong(GTSprayCanItem.NBT_REMAINING));

		// hit #512 depletes and swaps (upstream :85-92)
		ItemStack tSwap = GTSprayCanItem.payUses(tCan, 5120, tEmpty);
		assertNotNull(tSwap, "the 512th hit depletes the can");
		assertEquals(tEmpty, tSwap.getItem(), "the empty-can swap target");
		assertEquals(1, tSwap.getCount());
		assertFalse(carrierOf(tSwap).contains(GTSprayCanItem.NBT_REMAINING), "the empty can carries no gt.remaining");
		assertEquals(5120, GTSprayCanItem.remainingOf(carrierOf(tSwap), 5120), "tag-less = full semantics for the NEXT can");
	}

	@Test
	public void payUsesOnTheLastHitSwapsImmediately() {
		ItemStack tCan = new ItemStack(Items.IRON_INGOT);
		writeCarrier(tCan, GTSprayCanItem.NBT_REMAINING, 10); // one hit left
		ItemStack tSwap = GTSprayCanItem.payUses(tCan, 5120, Items.GOLD_INGOT);
		assertNotNull(tSwap);
		assertEquals(Items.GOLD_INGOT, tSwap.getItem());
	}

	// ---------------------------------------------------------------------------
	// the entity leg (upstream onRightClickEntity :97-142 — primitive fact rows, no Entity probes)
	// ---------------------------------------------------------------------------

	/** A live, unsheared, white-wool sheep fact row (the classic first spray target). */
	private static GTSprayCanItem.EntityFacts whiteSheep() {
		return new GTSprayCanItem.EntityFacts(true, true, false, false, false, DyeColor.WHITE.getId());
	}

	/** The colour code pins: {@code ~mColor&15} = the vanilla {@code DyeColor.getId()}. */
	@Test
	public void entityDyeIdIsTheComplementFold() {
		for (byte i = 0; i < 16; i++) {
			GTSprayCanItem.EntityFacts tSheep = new GTSprayCanItem.EntityFacts(true, true, false, false, false, i);
			assertEquals(15 - i, GTSprayCanItem.entityDyeId(tSheep, i), "the ~i&15 fold at dye " + i);
			assertEquals(GTSprayCanItem.vanillaDye(i).getId(), GTSprayCanItem.entityDyeId(tSheep, i),
					"the fold composes to vanillaDye(i) at dye " + i);
		}
		// the pinned ends (Sheep.java:279-286 reads/writes the wool by DyeColor.getId())
		assertEquals(DyeColor.BLACK.getId(), GTSprayCanItem.entityDyeId(whiteSheep(), (byte) 0),
				"the black can (index 0) dyes a white sheep black");
		assertEquals(DyeColor.WHITE.getId(), GTSprayCanItem.entityDyeId(
				new GTSprayCanItem.EntityFacts(true, true, false, false, false, DyeColor.BLACK.getId()), (byte) 15),
				"the white can (index 15) dyes a black sheep white");
		assertEquals(GTSprayCanItem.NO_ENTITY_HIT, GTSprayCanItem.entityDyeId(
				new GTSprayCanItem.EntityFacts(true, true, false, false, false, DyeColor.BLACK.getId()), (byte) 0),
				"the black can on an already-black sheep is the same-colour no-op");
	}

	/** The gate = the vanilla DyeItem.java:27 face; the wolf arm keeps the upstream :109 tamed-only shape. */
	@Test
	public void entityLegGatesFollowTheVanillaDyeItemFace() {
		assertEquals(DyeColor.BROWN.getId(), GTSprayCanItem.entityDyeId(whiteSheep(), (byte) 3),
				"a live unsheared sheep takes the dye (the brown can → brown wool)");
		assertEquals(GTSprayCanItem.NO_ENTITY_HIT, GTSprayCanItem.entityDyeId(
				new GTSprayCanItem.EntityFacts(true, true, true, false, false, DyeColor.WHITE.getId()), (byte) 3),
				"a sheared sheep is the vanilla :27 !isSheared miss");
		assertEquals(GTSprayCanItem.NO_ENTITY_HIT, GTSprayCanItem.entityDyeId(
				new GTSprayCanItem.EntityFacts(false, true, false, false, false, DyeColor.WHITE.getId()), (byte) 3),
				"a dead sheep is the vanilla :27 isAlive miss");
		assertEquals(GTSprayCanItem.NO_ENTITY_HIT, GTSprayCanItem.entityDyeId(
				new GTSprayCanItem.EntityFacts(true, true, false, false, false, DyeColor.BROWN.getId()), (byte) 3),
				"the same colour is the no-dye no-pay no-op (vanilla :27, upstream :103)");

		// the wolf arm (upstream :109 isTamed; vanilla Wolf.java:351 also wants isOwnedBy — upstream keeps tamed-only)
		assertEquals(DyeColor.BROWN.getId(), GTSprayCanItem.entityDyeId(
				new GTSprayCanItem.EntityFacts(true, false, false, true, true, DyeColor.WHITE.getId()), (byte) 3),
				"a live tamed wolf takes the collar dye");
		assertEquals(GTSprayCanItem.NO_ENTITY_HIT, GTSprayCanItem.entityDyeId(
				new GTSprayCanItem.EntityFacts(true, false, false, true, false, DyeColor.WHITE.getId()), (byte) 3),
				"an untamed wolf is the upstream :109 isTamed miss");
		assertEquals(GTSprayCanItem.NO_ENTITY_HIT, GTSprayCanItem.entityDyeId(
				new GTSprayCanItem.EntityFacts(false, false, false, true, true, DyeColor.WHITE.getId()), (byte) 3),
				"a dead wolf is the isAlive miss");
		assertEquals(GTSprayCanItem.NO_ENTITY_HIT, GTSprayCanItem.entityDyeId(
				new GTSprayCanItem.EntityFacts(true, false, false, true, true, DyeColor.BROWN.getId()), (byte) 3),
				"the same collar is the no-op (upstream :110, vanilla Wolf.java:353)");

		// everything else is the vanilla :37 PASS
		assertEquals(GTSprayCanItem.NO_ENTITY_HIT, GTSprayCanItem.entityDyeId(null, (byte) 3),
				"a non-sheep non-wolf target");
		assertEquals(GTSprayCanItem.NO_ENTITY_HIT, GTSprayCanItem.entityDyeId(
				new GTSprayCanItem.EntityFacts(true, false, false, false, true, DyeColor.WHITE.getId()), (byte) 3),
				"a tamed neither-sheep-nor-wolf (e.g. the cat collar — the upstream-absent declared cut)");
	}

	/** The entity ledger: -50 per hit (upstream :126), creative free, the block arm's -10 untouched. */
	@Test
	public void entityHitPaysFiftyNotTen() {
		assertEquals(50, GTSprayCanItem.ENTITY_HIT_COST, "upstream :126 tUses-=50 — five block hits");
		assertEquals(5070, GTSprayCanItem.remainingAfterHit(5120, GTSprayCanItem.ENTITY_HIT_COST, false),
				"an entity hit on a full can pays 50 internal units");
		assertEquals(5120, GTSprayCanItem.remainingAfterHit(5120, GTSprayCanItem.ENTITY_HIT_COST, true),
				"creative (hasInfiniteItems) pays nothing, upstream :126");
		assertEquals(0, GTSprayCanItem.remainingAfterHit(20, GTSprayCanItem.ENTITY_HIT_COST, false), "floored at 0");
		assertEquals(5110, GTSprayCanItem.remainingAfterHit(5120, false), "the block arm keeps its -10 (upstream :78)");
	}

	/**
	 * The full→countdown conversion for a fresh can's first entity hit (the upstream :121-125
	 * full→used reset, in the port's single-item ledger — the used item is the declared cut).
	 */
	@Test
	public void freshCanFirstEntityHitStartsTheCountdown() {
		ItemStack tCan = new ItemStack(Items.IRON_INGOT); // tag-less = a full can
		assertNull(GTSprayCanItem.payUses(tCan, 5120, GTSprayCanItem.ENTITY_HIT_COST, Items.GOLD_INGOT));
		assertEquals(5070, carrierOf(tCan).getLong(GTSprayCanItem.NBT_REMAINING),
				"the first entity hit writes 5120-50, not 5120-10");
	}

	/** 103 entity sprays empty a full can (102×50 leaves 20; the 103rd floors and swaps). */
	@Test
	public void entityHitsDepleteToTheEmptyCanSwap() {
		ItemStack tCan = new ItemStack(Items.IRON_INGOT);
		for (int i = 0; i < 102; i++) {
			assertNull(GTSprayCanItem.payUses(tCan, 5120, GTSprayCanItem.ENTITY_HIT_COST, Items.GOLD_INGOT),
					"entity hit #" + (i + 1) + " must not deplete yet");
		}
		assertEquals(20, carrierOf(tCan).getLong(GTSprayCanItem.NBT_REMAINING), "5120 - 102*50");
		ItemStack tSwap = GTSprayCanItem.payUses(tCan, 5120, GTSprayCanItem.ENTITY_HIT_COST, Items.GOLD_INGOT);
		assertNotNull(tSwap, "the 103rd entity hit depletes the can");
		assertEquals(Items.GOLD_INGOT, tSwap.getItem(), "the empty-can swap (upstream :130-137)");
	}

	// ---------------------------------------------------------------------------
	// the durability bar (the GTCEu :126-145 face)
	// ---------------------------------------------------------------------------

	@Test
	public void durabilityBarLogic() {
		assertFalse(GTSprayCanItem.barVisible(5120, 5120), "a fresh can hides the bar");
		assertTrue(GTSprayCanItem.barVisible(5110, 5120), "a sprayed can shows the bar");
		assertTrue(GTSprayCanItem.barVisible(0, 5120), "the depleted counter shows it too (transient pre-swap)");
		assertEquals(13, GTSprayCanItem.barWidth(5120, 5120), "full = the vanilla 13-pixel width");
		assertEquals(0, GTSprayCanItem.barWidth(0, 5120));
		assertEquals(7, GTSprayCanItem.barWidth(2560, 5120), "half = round(6.5) = 7");
	}

	// ---------------------------------------------------------------------------
	// the registry face (the GT6ToolsCreativeTabTest form over GT6SprayCans)
	// ---------------------------------------------------------------------------

	/** The 18-row display table in dye order + remover + empty (the registration wiring face). */
	@Test
	public void tabTableIsEighteenRowsInOrder() {
		assertEquals(18, GT6SprayCans.TAB_TABLE.size());
		for (int i = 0; i < 16; i++) {
			assertEquals(rl("spray_paint_" + GTSprayCanItem.DYE_IDS[i]), GT6SprayCans.SPRAY_PAINTS.get(i).getId(),
					"the colour rows follow DYE_IDS order");
			assertSame(GT6SprayCans.TAB_TABLE.get(i), GT6SprayCans.SPRAY_PAINTS.get(i));
		}
		assertEquals(rl("spray_paint_remover"), GT6SprayCans.SPRAY_PAINT_REMOVER.getId());
		assertEquals(rl("spray_can_empty"), GT6SprayCans.SPRAY_CAN_EMPTY.getId());
		assertSame(GT6SprayCans.TAB_TABLE.get(16), GT6SprayCans.SPRAY_PAINT_REMOVER);
		assertSame(GT6SprayCans.TAB_TABLE.get(17), GT6SprayCans.SPRAY_CAN_EMPTY);
	}

	/** Registration smoke + bidirectional parity (no orphan items, no unregistered table rows). */
	@Test
	public void tableAndItemsRegistryAreInParity() {
		Set<ResourceLocation> tTableIds = new LinkedHashSet<>();
		//? if forge {
		for (net.minecraftforge.registries.RegistryObject<Item> tRow : GT6SprayCans.TAB_TABLE) {
			tTableIds.add(tRow.getId());
		}
		Set<ResourceLocation> tRegisteredIds = new LinkedHashSet<>();
		for (net.minecraftforge.registries.RegistryObject<Item> tEntry : GT6SprayCans.ITEMS.getEntries()) {
			tRegisteredIds.add(tEntry.getId());
		}
		//?} else {
		/*for (net.neoforged.neoforge.registries.DeferredHolder<Item, Item> tRow : GT6SprayCans.TAB_TABLE) { // 21.1: the table stores the concrete holder
			tTableIds.add(tRow.getId());
		}
		Set<ResourceLocation> tRegisteredIds = new LinkedHashSet<>();
		for (net.neoforged.neoforge.registries.DeferredHolder<Item, ? extends Item> tEntry : GT6SprayCans.ITEMS.getEntries()) {
			tRegisteredIds.add(tEntry.getId());
		}
		*///?}
		assertEquals(18, tTableIds.size());
		assertTrue(tRegisteredIds.containsAll(tTableIds), "every table row must be a registered item");
		assertTrue(tTableIds.containsAll(tRegisteredIds), "every registered spray item must be displayed (no orphans)");
		assertEquals(Registries.ITEM, GT6SprayCans.ITEMS.getRegistryKey());
		assertEquals("itemGroup.gt6.spray_cans", GT6SprayCans.TAB_TITLE_KEY);
	}
}
