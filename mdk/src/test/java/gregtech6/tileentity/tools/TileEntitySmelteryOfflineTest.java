package gregtech6.tileentity.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.code.TagData;
import gregapi.data.CS;
import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.data.TD;
import gregapi.oredict.MaterialRegistry;
import gregapi.oredict.OreDictMaterialStack;
import gregtech6.item.MaterialPrefixItem;
import gregtech6.registry.GT6Crucibles;
import gregtech6.registry.GT6Molds;
import gregtech6.recipes.maps.GT6RecipeMapCrucible;
import gregtech6.registry.GT6MaterialTestSupport;

/**
 * The offline Smeltery/Mold BE acceptance (task crucible-physics-smeltery): the
 * mMeltDown WARNING latch (:324-327), the acid-destroy path (:265-270 with the
 * acidproof exemption), the feed ladder (:158-183 incl. the vanilla-ore bridge), the
 * HU energy face (:688-698), the mContent NBT round-trip (:87-104), the supply-cut
 * cooldown drip (:311) and the crucible→mold pour seam (fillMoldAtSide :498-507 over
 * the committed ITileEntityMold four-method face) — every world touch is level-guarded
 * in the BEs, so a synthetic-BET fixture over a vanilla block drives the full tick
 * bodies offline (the TileEntityBase01RootEnergyTest posture).
 */
public class TileEntitySmelteryOfflineTest {

	static final BlockPos POS = new BlockPos(2, 64, 3);

	static BlockEntityType<TileEntitySmeltery> sSmelteryType;
	static BlockEntityType<TileEntityMold> sMoldType;
	static MaterialPrefixItem DUST_IRON, INGOT_IRON;

	/**
	 * The family-tag membership stub for the vanilla ingot/nugget pins (task
	 * component-crucible-feed-resolve: the bridges are gone, the pins ride the read chain's
	 * family-tag arm). The real tag manager never boots offline — the stub feeds
	 * {@code OM.sStackTags} the ids the live VANILLA_INTERSECTION emission produces
	 * ({@code forge:ingots/iron = [minecraft:iron_ingot, gt6:ingot_iron]}). The "c"
	 * namespace rides GT6ItemTags.COMMON_NAMESPACE — the leg-neutral face (both legs'
	 * namespace gates accept it). Built in {@link #boot()}, NOT a static initializer:
	 * {@code Items.*} cannot be touched at class-init (before any
	 * {@code Bootstrap.bootStrap()} the registry classes fail to even load offline — the
	 * isolated-run initializationError).
	 */
	static java.util.Map<Item, net.minecraft.tags.TagKey<Item>> FAMILY_TAGS;

	static net.minecraft.tags.TagKey<Item> familyTag(String aPath) {
		return net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,
				new net.minecraft.resources.ResourceLocation("c", aPath));
	}

	static java.util.function.Function<ItemStack, java.util.stream.Stream<net.minecraft.tags.TagKey<Item>>> sSavedTags;

	@BeforeAll
	static void boot() {
		ProbeBoot.boot();
		DUST_IRON = ProbeBoot.probePrefix("smeltery_probe_dust_iron", () -> new MaterialPrefixItem(new Item.Properties(), OP.dust, MT.Iron));
		INGOT_IRON = ProbeBoot.probePrefix("smeltery_probe_ingot_iron", () -> new MaterialPrefixItem(new Item.Properties(), OP.ingot, MT.Iron));
		// the synthetic BlockEntityType needs its registry writable too (1.21.1: the
		// intrusive-holder creation validates the write — MappedRegistry.createIntrusiveHolder)
		ProbeBoot.openOffline(BuiltInRegistries.BLOCK_ENTITY_TYPE);
		sSmelteryType = BlockEntityType.Builder.of(
				(aPos, aState) -> new TileEntitySmeltery(sSmelteryType, aPos, aState), Blocks.BRICKS).build(null);
		sMoldType = BlockEntityType.Builder.of(
				(aPos, aState) -> new TileEntityMold(sMoldType, aPos, aState), Blocks.BRICKS).build(null);
		// the matStack seam answers through the probes (the live GTMaterialItems index is
		// empty offline; restored in @AfterAll — the GT6RecipeMapCrucibleTest posture)
		GT6RecipeMapCrucible.sMatResolver = r -> {
			MaterialPrefixItem tItem = null;
			if (r.prefix() == OP.dust && r.material() == MT.Iron) tItem = DUST_IRON;
			if (r.prefix() == OP.ingot && r.material() == MT.Iron) tItem = INGOT_IRON;
			return tItem == null || r.count() < 1 ? null : new ItemStack(tItem, (int)Math.min(64, r.count()));
		};
		FAMILY_TAGS = java.util.Map.of(
				Items.IRON_INGOT, familyTag("ingots/iron"),
				Items.GOLD_INGOT, familyTag("ingots/gold"),
				Items.COPPER_INGOT, familyTag("ingots/copper"),
				Items.IRON_NUGGET, familyTag("nuggets/iron"),
				Items.GOLD_NUGGET, familyTag("nuggets/gold"));
		sSavedTags = gregtech6.components.OM.sStackTags;
		gregtech6.components.OM.sStackTags = aStack -> {
			net.minecraft.tags.TagKey<Item> tTag = FAMILY_TAGS.get(aStack.getItem());
			return tTag == null ? java.util.stream.Stream.empty() : java.util.stream.Stream.of(tTag);
		};
	}

	@AfterAll
	static void restoreResolvers() {
		GT6RecipeMapCrucible.sMatResolver = GT6RecipeMapCrucible.DEFAULT_MAT_RESOLVER;
		gregtech6.components.OM.sStackTags = sSavedTags;
	}

	static TileEntitySmeltery makeSmeltery() {
		return new TileEntitySmeltery(sSmelteryType, POS, Blocks.BRICKS.defaultBlockState());
	}

	static TileEntityMold makeMold(int aShape) {
		TileEntityMold rMold = new TileEntityMold(sMoldType, POS, Blocks.BRICKS.defaultBlockState());
		rMold.mShape = aShape;
		return rMold;
	}

	// -------------------------------------------------------------------------
	// the feed ladder (:158-183)
	// -------------------------------------------------------------------------

	/** :167-183 — a vanilla ore block melts as ONE standard ore of its material. */
	@Test
	public void vanillaOreFeedsOreDirect() {
		TileEntitySmeltery tCrucible = makeSmeltery();
		List<OreDictMaterialStack> tFeed = tCrucible.feedStacks(new ItemStack(Items.IRON_ORE));
		assertNotNull(tFeed, "the vanilla iron ore bridge must resolve");
		assertEquals(1, tFeed.size());
		// GT6 realism: the crushing target of iron ore is the HEMATITE ore material, not the
		// refined Fe — the assertion rides the material graph answer, not a hardcoded name
		assertSame(MT.Fe.mTargetCrushing.mMaterial, tFeed.get(0).mMaterial);
		assertTrue(tFeed.get(0).mAmount >= CS.U, "an ore is at least one unit of its crushing target");
	}

	/** :179-183 — a prefix item feeds its prefix amount per item. */
	@Test
	public void prefixItemFeedsPrefixAmount() {
		TileEntitySmeltery tCrucible = makeSmeltery();
		List<OreDictMaterialStack> tFeed = tCrucible.feedStacks(new ItemStack(DUST_IRON, 2));
		assertNotNull(tFeed);
		assertEquals(1, tFeed.size());
		assertSame(MT.Fe, tFeed.get(0).mMaterial);
		assertEquals(2 * CS.U, tFeed.get(0).mAmount, "two dusts = two units");
	}

	/** :160-162 — no material data = the trash+fizz arm (null). */
	@Test
	public void unknownItemReturnsNull() {
		TileEntitySmeltery tCrucible = makeSmeltery();
		assertNull(tCrucible.feedStacks(new ItemStack(Items.STICK)));
	}

	// -------------------------------------------------------------------------
	// the vanilla ingot/nugget bridge (task crucible-behavior-fixes)
	// -------------------------------------------------------------------------

	/** the vanilla iron ingot melts into exactly its units of iron — the feed half AND the melt half. */
	@Test
	public void vanillaIronIngotMeltsIntoIron() {
		TileEntitySmeltery tCrucible = makeSmeltery();
		List<OreDictMaterialStack> tFeed = tCrucible.feedStacks(new ItemStack(Items.IRON_INGOT, 2));
		assertNotNull(tFeed, "the vanilla ingot bridge must resolve (the silent-swallow fix)");
		assertEquals(1, tFeed.size());
		assertSame(MT.Fe, tFeed.get(0).mMaterial, "the melt-out material = iron");
		assertEquals(2 * OP.ingot.mAmount, tFeed.get(0).mAmount, "the prefix amount per item, whole stack");
		// the melt half: the hot charge lands as molten iron content
		assertTrue(tCrucible.addStacks(tFeed, 2000));
		assertEquals(1, tCrucible.mContent.size());
		assertSame(MT.Fe, tCrucible.mContent.get(0).mMaterial);
		assertEquals(2 * OP.ingot.mAmount, tCrucible.mContent.get(0).mAmount);
	}

	/** the gold and copper ingot pins (the rest of the upstream ingotIron/ingotGold/ingotCopper family). */
	@Test
	public void vanillaGoldAndCopperIngotsResolve() {
		TileEntitySmeltery tCrucible = makeSmeltery();
		List<OreDictMaterialStack> tGold = tCrucible.feedStacks(new ItemStack(Items.GOLD_INGOT));
		assertEquals(1, tGold.size());
		assertSame(MT.Au, tGold.get(0).mMaterial);
		assertEquals(OP.ingot.mAmount, tGold.get(0).mAmount);
		List<OreDictMaterialStack> tCopper = tCrucible.feedStacks(new ItemStack(Items.COPPER_INGOT));
		assertEquals(1, tCopper.size());
		assertSame(MT.Cu, tCopper.get(0).mMaterial);
		assertEquals(OP.ingot.mAmount, tCopper.get(0).mAmount);
	}

	/** the nugget pins: one ninth of a unit per nugget. */
	@Test
	public void vanillaNuggetsResolveAtNuggetAmount() {
		TileEntitySmeltery tCrucible = makeSmeltery();
		List<OreDictMaterialStack> tIron = tCrucible.feedStacks(new ItemStack(Items.IRON_NUGGET, 9));
		assertEquals(1, tIron.size());
		assertSame(MT.Fe, tIron.get(0).mMaterial);
		assertEquals(9 * OP.nugget.mAmount, tIron.get(0).mAmount, "nine nuggets = one unit");
		List<OreDictMaterialStack> tGold = tCrucible.feedStacks(new ItemStack(Items.GOLD_NUGGET));
		assertEquals(1, tGold.size());
		assertSame(MT.Au, tGold.get(0).mMaterial);
		assertEquals(OP.nugget.mAmount, tGold.get(0).mAmount);
	}

	// -------------------------------------------------------------------------
	// the energy face (:688-698, the HU leg)
	// -------------------------------------------------------------------------

	/** :692-693 — HU accepted from all sides, credited through the doInject hook. */
	@Test
	public void huInjectionCreditsBuffer() {
		TileEntitySmeltery tCrucible = makeSmeltery();
		long tUsed = tCrucible.doEnergyInjection(TD.Energy.HU, (byte)1, 16, 100, true);
		assertEquals(100, tUsed, "the packet is consumed");
		assertEquals(1600, tCrucible.mEnergy, "16 HU/packet x 100 packets credited (:693)");
	}

	/** the gate bounces non-HU types before the hook (the EnergyGate type check). */
	@Test
	public void nonHuBounces() {
		TileEntitySmeltery tCrucible = makeSmeltery();
		tCrucible.doEnergyInjection(TD.Energy.EU, (byte)1, 32, 100, true);
		assertEquals(0, tCrucible.mEnergy, "EU is the defer pool, never credited");
		assertFalse(tCrucible.isEnergyAcceptingFrom(TD.Energy.EU, (byte)1, false));
		assertTrue(tCrucible.isEnergyAcceptingFrom(TD.Energy.HU, (byte)1, false), "HU from ALL sides (:692)");
	}

	// -------------------------------------------------------------------------
	// the supply-cut cooldown drip (:311) and the meltdown family (:315-327)
	// -------------------------------------------------------------------------

	/** :311 — an expired cooldown drips one Kelvin per 10 ticks toward the environment. */
	@Test
	public void supplyCutDripsTowardEnvironment() {
		TileEntitySmeltery tCrucible = makeSmeltery();
		tCrucible.mTemperature = 500;
		tCrucible.mEnergy = 0;
		tCrucible.mCooldown = 0;
		tCrucible.onTick(1, true);
		assertEquals(499, tCrucible.mTemperature, "one Kelvin down per drip window (:311)");
		assertEquals(10, tCrucible.mCooldown, "the window re-arms (:311)");
	}

	/** :324-327 — within 100 K of the ceiling the WARNING latch engages. */
	@Test
	public void meltdownWarningLatchEngagesAndReleases() {
		TileEntitySmeltery tCrucible = makeSmeltery();
		long tMax = tCrucible.temperatureMax();
		assertTrue(tMax + 0 > 0, "the stone ceiling exists");
		tCrucible.mTemperature = tMax - 50;
		tCrucible.mEnergy = 0;
		tCrucible.mMeltDown = false;
		tCrucible.onTick(1, true);
		assertTrue(tCrucible.mMeltDown, "temp + 100 > max → WARNING (:324)");
		tCrucible.mTemperature = TileEntitySmeltery.DEF_ENV_TEMP;
		tCrucible.onTick(2, true);
		assertFalse(tCrucible.mMeltDown, "cooled back down → the latch releases");
	}

	/** :315-322 — past the ceiling everything trashes (the lava write is level-guarded offline). */
	@Test
	public void meltDownTrashesContent() {
		TileEntitySmeltery tCrucible = makeSmeltery();
		tCrucible.mContent.add(new OreDictMaterialStack(MT.Fe, 2 * CS.U));
		tCrucible.mTemperature = tCrucible.temperatureMax() + 1;
		tCrucible.mEnergy = 0;
		tCrucible.mCooldown = 100; // a full window: the buffer-less tick must not lower the temp first
		tCrucible.onTick(1, true);
		assertTrue(tCrucible.mContent.isEmpty(), "the melt-down trashes the pile (:315)");
	}

	/** :320 — the meltdown lava is FLOWING (level 1), never a permanent spreading source (task crucible-behavior-fixes). */
	@Test
	public void meltdownLavaIsFlowingNotASource() {
		TileEntitySmeltery tCrucible = new MeltdownSmeltery(); // the leveled BE: envTemp is the stub seam (the biome climate walk)
		gregtech6.tileentity.multiblocks.GTMultiBlocksOfflineTestBase.MultiBlockLevel tLevel =
				new gregtech6.tileentity.multiblocks.GTMultiBlocksOfflineTestBase.MultiBlockLevel();
		tCrucible.setLevel(tLevel);
		tLevel.mStates.put(POS, Blocks.BRICKS.defaultBlockState());
		// the slot must hold something: the suck seam NPEs offline (MinimalLevel.getEntities → null)
		tCrucible.getInventory().setStackInSlot(0, new ItemStack(Items.STICK));
		tCrucible.mContent.add(new OreDictMaterialStack(MT.Fe, CS.U));
		tCrucible.mTemperature = tCrucible.temperatureMax() + 1;
		tCrucible.mEnergy = 0;
		tCrucible.mCooldown = 100;
		tCrucible.onTick(1, true);
		BlockState tState = tLevel.getBlockState(POS);
		assertSame(Blocks.LAVA, tState.getBlock(), "the melt-down block is lava");
		assertEquals(1, tState.getValue(BlockStateProperties.LEVEL).intValue(),
				"flowing lava meta 1 — no permanent source (:320)");
		assertTrue(tCrucible.mContent.isEmpty(), "the melt-down still trashes the pile (:315)");
	}

	/** The leveled meltdown BE: envTemp() is stubbed — the biome climate walk needs a live chunk source offline. */
	static final class MeltdownSmeltery extends TileEntitySmeltery {
		MeltdownSmeltery() {
			super(sSmelteryType, POS, Blocks.BRICKS.defaultBlockState());
		}
		@Override
		public long envTemp() {
			return DEF_ENV_TEMP;
		}
	}

	// -------------------------------------------------------------------------
	// the acid destroy path (:265-270)
	// -------------------------------------------------------------------------

	/** :265-270 — an ACID material in a non-acidproof crucible destroys the whole content. */
	@Test
	public void acidDestroysContent() {
		assertTrue(MT.H2SO4.contains(TD.Properties.ACID), "sulfuric acid carries the ACID property (MT.java:1914 lqudacid)");
		TileEntitySmeltery tCrucible = makeSmeltery();
		tCrucible.mContent.add(new OreDictMaterialStack(MT.H2SO4, CS.U));
		tCrucible.mContent.add(new OreDictMaterialStack(MT.Fe, CS.U));
		tCrucible.mTemperature = 293;
		tCrucible.oTemperature = 293; // no phase-crossing — the acid arm must fire first anyway
		tCrucible.mEnergy = 0;
		tCrucible.onTick(1, true);
		assertTrue(tCrucible.mContent.isEmpty(), "the acid destroys EVERYTHING including the iron (:266)");
	}

	/** the acidproof exemption rides the shell, at 293 the acid just sits there. */
	@Test
	public void acidWithNoCrossingSitsQuietWhenProof() {
		TileEntitySmeltery tCrucible = makeSmeltery();
		tCrucible.mAcidProof = true;
		tCrucible.mContent.add(new OreDictMaterialStack(MT.H2SO4, CS.U));
		tCrucible.mTemperature = 293;
		tCrucible.oTemperature = 293;
		tCrucible.mEnergy = 0;
		tCrucible.onTick(1, true);
		assertEquals(1, tCrucible.mContent.size(), "acidproof shells keep their bath");
	}

	// -------------------------------------------------------------------------
	// the crucible→mold pour seam (:498-507 over ITileEntityMold)
	// -------------------------------------------------------------------------

	/** :498-507 → :246-264 — the molten iron pours exactly the mold requirement out of the pile. */
	@Test
	public void fillMoldAtSidePoursIntoMold() {
		TileEntitySmeltery tCrucible = makeSmeltery();
		tCrucible.mContent.add(new OreDictMaterialStack(MT.Fe, 2 * CS.U));
		tCrucible.mTemperature = 2000;
		TileEntityMold tMold = makeMold(TileEntityMold.ingotShape(0));
		assertEquals(CS.U, tMold.getMoldRequiredMaterialUnits(), "the ingot bar requires exactly one unit (:242)");
		assertTrue(tMold.isMoldInputSide((byte)2), "the horizontal faces accept the pour (:224-226)");
		assertTrue(tCrucible.fillMoldAtSide(tMold, TileEntitySmeltery.SIDE_TOP, (byte)2), "the pour transfers");
		assertEquals(CS.U, tCrucible.mContent.get(0).mAmount, "exactly the requirement left the crucible");
		assertNotNull(tMold.mContent, "the mold holds the charge");
		assertSame(MT.Fe, tMold.mContent.mMaterial);
		assertEquals(CS.U, tMold.mContent.mAmount);
		assertEquals(2000, tMold.mTemperature, "the charge carries the pour temperature (:258)");
	}

	/** :247 — an ACID charge never enters a mold. */
	@Test
	public void moldRefusesAcid() {
		TileEntityMold tMold = makeMold(TileEntityMold.ingotShape(0));
		assertEquals(0, tMold.fillMold(new OreDictMaterialStack(MT.H2SO4, CS.U), 400, (byte)2), "ACID is refused (:247)");
		assertNull(tMold.mContent);
	}

	/** :189-203 — the charged mold cools below the melting point and shapes the ingot. */
	@Test
	public void moldSolidifiesAndShapes() {
		TileEntityMold tMold = makeMold(TileEntityMold.ingotShape(0));
		tMold.mContent = new OreDictMaterialStack(MT.Fe, CS.U);
		tMold.mTemperature = TileEntityMold.DEF_ENV_TEMP; // already cool: the shape pours at once
		tMold.onTick(1, true);
		assertFalse(tMold.mInventory.isEmpty(), "the solidified ingot lands in the slot (:199)");
		assertSame(INGOT_IRON, tMold.mInventory.get().getItem());
		assertEquals(1, tMold.mInventory.get().getCount(), "one unit = one ingot");
		assertEquals(0, tMold.mContent.mAmount, "the charge is spent (:200)");
	}

	// -------------------------------------------------------------------------
	// the NBT round-trip (:87-104)
	// -------------------------------------------------------------------------

	/** saveAdditional/load — the full field set plus the mContent list (the 'i'-short adapter). */
	@Test
	public void nbtRoundTrip() {
		TileEntitySmeltery tCrucible = makeSmeltery();
		tCrucible.mContent.add(new OreDictMaterialStack(MT.Fe, 2 * CS.U));
		tCrucible.mContent.add(new OreDictMaterialStack(MT.Au, CS.U9));
		tCrucible.mTemperature = 1300; // 1300 + 100 > the stone ceiling (1375) — the WARNING band
		tCrucible.oTemperature = 1200;
		tCrucible.mEnergy = 5000;
		tCrucible.mCooldown = 42;
		tCrucible.mMeltDown = true;
		tCrucible.getInventory().setStackInSlot(0, new ItemStack(DUST_IRON, 3));
		CompoundTag tNBT = new CompoundTag();
		tCrucible.saveAdditional(tNBT);

		TileEntitySmeltery tRestored = makeSmeltery();
		tRestored.load(tNBT);
		assertEquals(1300, tRestored.mTemperature);
		assertEquals(1200, tRestored.oTemperature);
		assertEquals(5000, tRestored.mEnergy);
		assertEquals(42, tRestored.mCooldown);
		assertTrue(tRestored.mMeltDown, "recomputed on load (:94): 1300 + 100 > the stone ceiling");
		assertEquals(2, tRestored.mContent.size(), "the list adapter restores both stacks");
		assertSame(MT.Fe, tRestored.mContent.get(0).mMaterial);
		assertEquals(2 * CS.U, tRestored.mContent.get(0).mAmount);
		assertSame(MT.Au, tRestored.mContent.get(1).mMaterial);
		assertEquals(CS.U9, tRestored.mContent.get(1).mAmount);
		assertEquals(3, tRestored.getInventory().getStackInSlot(0).getCount(), "the feed slot rides gt.inv");
	}

	/** the load() list replace semantics — a stale in-memory pile never merges into the loaded one. */
	@Test
	public void loadReplacesContentList() {
		TileEntitySmeltery tCrucible = makeSmeltery();
		tCrucible.mContent.add(new OreDictMaterialStack(MT.Fe, CS.U)); // stale garbage
		CompoundTag tNBT = new CompoundTag();
		TileEntitySmeltery tSource = makeSmeltery();
		tSource.mContent.add(new OreDictMaterialStack(MT.Cu, 3 * CS.U));
		tSource.saveAdditional(tNBT);
		tCrucible.load(tNBT);
		assertEquals(1, tCrucible.mContent.size(), "load() cleared the stale pile first (:93)");
		assertSame(MT.Cu, tCrucible.mContent.get(0).mMaterial);
	}

	// -------------------------------------------------------------------------
	// the ITEM_HANDLER capability face (issue #20 sub-task A, 20a-smeltery-cap)
	// -------------------------------------------------------------------------

	/**
	 * The root's package-private capability seam, resolved reflectively — package-private
	 * members do not inherit across packages (JLS 6.6.1/8.2), so a tools-package receiver
	 * can never call it directly (the TestMachineBlockEntityNBTTest posture only works for
	 * gregtech6.tileentity receivers); the ProbeBoot reflection walk is the offline form.
	 */
	//? if forge {
	@SuppressWarnings("unchecked")
	static net.minecraftforge.common.util.LazyOptional<net.minecraftforge.items.IItemHandler> itemHandlerCap(TileEntitySmeltery aCrucible) throws Exception {
		for (Class<?> tClass = aCrucible.getClass(); tClass != null; tClass = tClass.getSuperclass()) {
			try {
				Method tMethod = tClass.getDeclaredMethod("itemHandlerCapability");
				tMethod.setAccessible(true);
				return (net.minecraftforge.common.util.LazyOptional<net.minecraftforge.items.IItemHandler>)tMethod.invoke(aCrucible);
			} catch (NoSuchMethodException ignored) {}
		}
		throw new NoSuchMethodException("itemHandlerCapability");
	}

	/** issue #20: the root capability is live (it stayed LazyOptional.empty() while the feed-slot field shadowed the root's mInventory). */
	@Test
	public void itemHandlerCapabilityIsLiveOverTheFeedSlot() throws Exception {
		TileEntitySmeltery tCrucible = makeSmeltery();
		net.minecraftforge.common.util.LazyOptional<net.minecraftforge.items.IItemHandler> tCap = itemHandlerCap(tCrucible);
		assertTrue(tCap.isPresent(), "the ITEM_HANDLER capability must not be empty (issue #20)");
		net.minecraftforge.items.IItemHandler tHandler = tCap.orElseThrow(IllegalStateException::new);
		assertEquals(1, tHandler.getSlots(), "the single feed slot (upstream :682)");
		assertSame(tCrucible.getInventory(), tHandler, "the capability wraps the handler the tick feeds through");
	}

	/** the hopper path (VanillaInventoryCodeHooks.insertHook): insertItem through the capability handle lands in the feed slot. */
	@Test
	public void insertThroughCapabilityFillsTheFeedSlot() throws Exception {
		TileEntitySmeltery tCrucible = makeSmeltery();
		net.minecraftforge.items.IItemHandler tHandler = itemHandlerCap(tCrucible).orElseThrow(IllegalStateException::new);
		assertTrue(tHandler.insertItem(0, new ItemStack(Items.DIAMOND, 4), false).isEmpty(), "the insert is fully accepted (the filter is open)");
		assertEquals(4, tCrucible.getInventory().getStackInSlot(0).getCount(), "the hopper push is visible in the feed slot");
		assertSame(Items.DIAMOND, tCrucible.getInventory().getStackInSlot(0).getItem());
	}
	//?}

	// -------------------------------------------------------------------------
	// the registration rows (spec ⑥)
	// -------------------------------------------------------------------------

	/** the row ladder exists as data and the shape helpers answer the ingot bar. */
	@Test
	public void registrationRowsAndShapes() {
		assertEquals(39, GT6Crucibles.ROWS.size(), "the FULL :251-292 ladder (material-mc-c-crucible-rows) — never pass vacuously");
		assertEquals(1, GT6Molds.ROWS.size(), "the stone mold rung");
		assertSame(OP.ingot, TileEntityMold.getMoldRecipe(TileEntityMold.ingotShape(1)), "the shifted bar maps to ingot");
		assertSame(OP.nugget, TileEntityMold.getMoldRecipe(1), "unknown non-zero shapes fall back to the nugget (:79-83)");
		assertNull(TileEntityMold.getMoldRecipe(0), "the empty mask has no recipe");
		assertEquals((1 << 25) - 1, TileEntityMold.SHAPE_MASK, "the 25-bit mask (:81)");
	}

	// -------------------------------------------------------------------------
	// the shared offline boot (the GTMaterialItemsBoot posture, local copy —
	// the original is a private helper of the RM test class)
	// -------------------------------------------------------------------------
	static final class ProbeBoot {
		static void boot() {
			SharedConstants.tryDetectVersion();
			try {
				Bootstrap.bootStrap();
			} catch (Throwable ignored) {
				// NetworkHooks.init() failure is expected offline; registries are ready by now.
			}
			GT6MaterialTestSupport.materials(); // the hermetic bracket: reset FIRST, then the full refill (task hermetic-pour-tests)
		}

		static MaterialPrefixItem probePrefix(String aProbeId, java.util.function.Supplier<MaterialPrefixItem> aCreator) {
			net.minecraft.core.Registry<Item> tRegistry = BuiltInRegistries.ITEM;
			openOffline(tRegistry);
			MaterialPrefixItem rItem = aCreator.get();
			net.minecraft.core.Registry.register(tRegistry, new net.minecraft.resources.ResourceLocation("gt6", aProbeId), rItem);
			return rItem;
		}

		/** The registry open, best-effort PER FACE, shared by every offline registration site. */
		@SuppressWarnings("unchecked")
		static void openOffline(net.minecraft.core.Registry<?> aRegistry) {
			net.minecraft.core.Registry<Object> tRegistry = (net.minecraft.core.Registry<Object>)aRegistry;
			// the forge leg backs the vanilla registry with a ForgeRegistry delegate and its own
			// locked flag; the 21.1 leg has NEITHER (the delegate NoSuchFieldException is its
			// baseline shape — the vanilla unfreeze() is the only gate there), so every face
			// degrades to a no-op and the caller's register/build call is the real verdict
			try {
				Method tUnfreeze = tRegistry.getClass().getMethod("unfreeze");
				tUnfreeze.setAccessible(true);
				tUnfreeze.invoke(tRegistry);
			} catch (Exception aE) {
				throw new IllegalStateException("could not unfreeze the offline registry", aE);
			}
			try {
				Field tDelegate = inheritedField(tRegistry.getClass(), "delegate");
				tDelegate.setAccessible(true);
				Object tForgeRegistry = tDelegate.get(tRegistry);
				Method tForgeUnfreeze = tForgeRegistry.getClass().getMethod("unfreeze");
				tForgeUnfreeze.setAccessible(true);
				tForgeUnfreeze.invoke(tForgeRegistry);
			} catch (NoSuchFieldException | NoSuchMethodException ignored) {
				// the 21.1 face: no forge delegate behind the vanilla registry
			} catch (Exception aE) {
				throw new IllegalStateException("could not open the offline forge registry", aE);
			}
			try {
				Field tLocked = inheritedField(tRegistry.getClass(), "locked");
				tLocked.setBoolean(tRegistry, false);
			} catch (NoSuchFieldException ignored) {
				// the 21.1 face: nothing but the vanilla frozen flag to unlock
			} catch (Exception aE) {
				throw new IllegalStateException("could not clear the offline registry lock", aE);
			}
		}

		private static Field inheritedField(Class<?> aClass, String aName) throws NoSuchFieldException {
			for (Class<?> tClass = aClass; tClass != null; tClass = tClass.getSuperclass()) {
				try {
					Field rField = tClass.getDeclaredField(aName);
					rField.setAccessible(true);
					return rField;
				} catch (NoSuchFieldException ignored) {}
			}
			throw new NoSuchFieldException(aName);
		}
	}
}
