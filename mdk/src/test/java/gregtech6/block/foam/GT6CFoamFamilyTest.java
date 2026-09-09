package gregtech6.block.foam;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.client.foam.GTCFoamTintListener;
import gregtech6.item.spraycan.GTSprayCanItem;
import gregtech6.tileentity.GTOfflineTestBase;
import gregtech6.tileentity.connectors.GTFluidPipeBlockEntity;
import gregtech6.tileentity.foam.GT6CFoamBlockEntity;
import gregtech6.tileentity.foam.ITileEntityFoamable;
import gregtech6.tileentity.multiblocks.GTMultiBlocksOfflineTestBase.MultiBlockLevel;

/**
 * The C-Foam block-family offline acceptance (task p26-c-foam-block-family): the
 * fresh/dried property faces over the static registration seams (the GTGrassBlock
 * reflection form), the dry-delay rng seam bounds (upstream :61 100+rand(5900)), the
 * foamTarget EXTENDED routing truth table (the SPEC pin — pipe / foamable-TE / block /
 * already-foamed arms), the foam faces (applyFoam constant false — the research "零行为差"
 * finding — plus dry/remove/has/dried), the OWNED BE three-clause gate (upstream
 * MultiTileEntityCFoam :113-115), the drying flip's DRIED property leg, the NBT four-key
 * round-trip (upstream :56-69 — the item half has no carrier, the owned block registers
 * NO BlockItem, the declared cut), the BlockColor tint table and the generated-tree
 * snapshots (blockstate tintindex 0, the empty fresh loot, the dried self-drop).
 *
 * <p>The custom BLOCK instances need the vanilla block registry unfrozen (the
 * GTWireBlockUseLockTest bracket — Block's intrusive holder is created in the ctor).
 */
public class GT6CFoamFamilyTest extends GTOfflineTestBase {

	static final BlockPos POS = new BlockPos(3, 4, 5);
	static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-00000000c0a1");
	static final UUID FOREIGN = UUID.fromString("00000000-0000-0000-0000-00000000c0a2");

	static GT6CFoamFreshBlock sFresh;
	static GT6CFoamBlock sDried;
	static GT6CFoamFreshSlabBlock sFreshSlab;
	static GT6CFoamSlabBlock sDriedSlab;
	static GT6CFoamOwnedBlock sOwned;
	static BlockEntityType<GT6CFoamBlockEntity> sOwnedType;
	static BlockEntityType<GTFluidPipeBlockEntity> sPipeType;

	@BeforeAll
	static void buildFamilyFixtures() {
		// the vanilla boot first (GTOfflineTestBase), then the block-registry write window
		try {
			java.lang.reflect.Method tUnfreeze = net.minecraft.core.registries.BuiltInRegistries.BLOCK
					.getClass().getMethod("unfreeze");
			tUnfreeze.setAccessible(true);
			tUnfreeze.invoke(net.minecraft.core.registries.BuiltInRegistries.BLOCK);
		} catch (Exception aE) {
			throw new IllegalStateException("could not unfreeze the offline block registry", aE);
		}
		sDried = new GT6CFoamBlock(GT6CFoamBlock.driedProperties());
		sFresh = new GT6CFoamFreshBlock(GT6CFoamFreshBlock.freshProperties(), () -> sDried);
		sDriedSlab = new GT6CFoamSlabBlock(GT6CFoamSlabBlock.driedSlabProperties());
		sFreshSlab = new GT6CFoamFreshSlabBlock(GT6CFoamFreshSlabBlock.freshSlabProperties(), () -> sDriedSlab);
		sOwned = new GT6CFoamOwnedBlock(GT6CFoamOwnedBlock.ownedProperties());

		@SuppressWarnings("unchecked")
		BlockEntityType<GT6CFoamBlockEntity>[] tFoamHolder = (BlockEntityType<GT6CFoamBlockEntity>[]) new BlockEntityType<?>[1];
		tFoamHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new GT6CFoamBlockEntity(tFoamHolder[0], aPos, aState), sOwned).build(null);
		sOwnedType = tFoamHolder[0];

		@SuppressWarnings("unchecked")
		BlockEntityType<GTFluidPipeBlockEntity>[] tPipeHolder = (BlockEntityType<GTFluidPipeBlockEntity>[]) new BlockEntityType<?>[1];
		tPipeHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new GTFluidPipeBlockEntity(tPipeHolder[0], aPos, aState),
				Blocks.STONE, Blocks.DIRT).build(null);
		sPipeType = tPipeHolder[0];
	}

	// ------------------------------------------------------------------ the blockstate faces

	@Test
	void colorIsTheSixteenStepBlockstateDimension() {
		assertEquals("color", GT6CFoamFreshBlock.COLOR.getName(), "the property name (the blockstate JSON key)");
		assertEquals(16, GT6CFoamFreshBlock.COLOR.getPossibleValues().size(), "the 16 dye slots");
		assertEquals(0, sFresh.defaultBlockState().getValue(GT6CFoamFreshBlock.COLOR), "the default colour 0");
		assertEquals(0, sDried.defaultBlockState().getValue(GT6CFoamFreshBlock.COLOR), "the dried block shares ONE property instance");
		assertEquals(0, sFreshSlab.defaultBlockState().getValue(GT6CFoamFreshBlock.COLOR));
		// the slab carries the vanilla TYPE + WATERLOGGED pair over the colour
		assertTrue(sFreshSlab.defaultBlockState().hasProperty(net.minecraft.world.level.block.SlabBlock.TYPE));
	}

	@Test
	void freshPropertiesAreTheUpstreamNumbersVerbatim() throws Exception {
		Object tProps = GT6CFoamFreshBlock.freshProperties();
		assertEquals(1.0F, readFloat(tProps, "destroyTime"), "upstream :47 hardness 1.0");
		assertEquals(0.0F, readFloat(tProps, "explosionResistance"), "upstream :47 resistance 0.0");
		assertEquals(net.minecraft.world.level.block.SoundType.WOOL, readField(tProps, "soundType"),
				"the upstream soundTypeCloth face (this mapping's WOOL carrier)");
		assertFalse(readBoolean(tProps, "canOcclude"), "the :75 not-opaque face");
		assertFalse(readBoolean(tProps, "hasCollision"), "the :95-97 passable face (collision null)");
	}

	@Test
	void driedPropertiesAreTheUpstreamNumbersVerbatim() throws Exception {
		Object tProps = GT6CFoamBlock.driedProperties();
		assertEquals(4.0F, readFloat(tProps, "destroyTime"), "upstream :39 hardness 4.0");
		assertEquals(1.5F, readFloat(tProps, "explosionResistance"), "upstream :39 resistance 1.5");
		assertEquals(net.minecraft.world.level.block.SoundType.STONE, readField(tProps, "soundType"), "the rock+stone face");
		Object tSlabProps = GT6CFoamSlabBlock.driedSlabProperties();
		assertEquals(4.0F, readFloat(tSlabProps, "destroyTime"), "the slab mirrors the full block");
		assertEquals(1.5F, readFloat(tSlabProps, "explosionResistance"), "the slab mirrors the full block");
		Object tFreshSlabProps = GT6CFoamFreshSlabBlock.freshSlabProperties();
		assertEquals(1.0F, readFloat(tFreshSlabProps, "destroyTime"), "the fresh slab mirrors the fresh block");
		assertFalse(readBoolean(tFreshSlabProps, "hasCollision"), "the fresh slab is passable too");
	}

	// the BlockBehaviour.Properties reflection readers — the fields are package-private in vanilla

	private static Object readField(Object aProps, String aName) throws Exception {
		var tField = aProps.getClass().getDeclaredField(aName);
		tField.setAccessible(true);
		return tField.get(aProps);
	}

	private static boolean readBoolean(Object aProps, String aName) throws Exception {
		return (Boolean) readField(aProps, aName);
	}

	private static float readFloat(Object aProps, String aName) throws Exception {
		return (Float) readField(aProps, aName);
	}

	// ------------------------------------------------------------------ the dry-delay rng seam

	/** Upstream :61 {@code 100+RNGSUS.nextInt(5900)} — the bounds and the determinism. */
	@Test
	void dryDelayIsOneHundredPlusRandom5900() {
		assertEquals(GT6CFoamFreshBlock.DRY_DELAY_BASE + GT6CFoamFreshBlock.DRY_DELAY_SPAN - 1,
				GT6CFoamFreshBlock.DRY_DELAY_SPAN + 99, "the declared span arithmetic");
		long tMin = Long.MAX_VALUE, tMax = Long.MIN_VALUE;
		RandomSource tRandom = RandomSource.create(20260909L);
		for (int i = 0; i < 100000; i++) {
			int tDelay = GT6CFoamFreshBlock.dryDelay(tRandom);
			assertTrue(tDelay >= 100, "the schedule base");
			assertTrue(tDelay <= 5999, "the rng bound (100 + 5899 max)");
			tMin = Math.min(tMin, tDelay);
			tMax = Math.max(tMax, tDelay);
		}
		assertTrue(tMax - tMin > 5000, "the sampled spread covers the rng span, not a constant");
		// determinism: the same seed walks the same sequence
		assertEquals(GT6CFoamFreshBlock.dryDelay(RandomSource.create(1L)),
				GT6CFoamFreshBlock.dryDelay(RandomSource.create(1L)), "the same seed = the same delay");
	}

	// ------------------------------------------------------------------ the foamTarget EXTENDED routing truth table

	/**
	 * The SPEC pin: the routing gate over the port's TWO foamable families. The pipe half
	 * is the p25 face; the ITileEntityFoamable half routes on {@code !hasFoam} — the owned
	 * CFoam BE answers {@code hasFoam} TRUE verbatim (upstream :146), so the arm is
	 * shape-only for the spray and the REMOVER's routing face (card C).
	 */
	@Test
	public void foamTargetTruthTable() {
		// the null / non-foamable faces
		assertFalse(gregtech6.item.foamspray.GT6FoamSprayItem.foamTarget(null, (byte)2), "no BE = no target");

		// the pipe half: unfoamed = the target, foamed = the PASS
		GTFluidPipeBlockEntity tPipe = sPipeType.create(POS, Blocks.STONE.defaultBlockState());
		assertTrue(gregtech6.item.foamspray.GT6FoamSprayItem.foamTarget(tPipe, (byte)2),
				"an unfoamed pipe is the spray target (the p25 face)");
		assertTrue(tPipe.applyFoam((byte)2, OWNER, GTSprayCanItem.DYES_INT[0], false));
		assertFalse(gregtech6.item.foamspray.GT6FoamSprayItem.foamTarget(tPipe, (byte)2),
				"an already-foamed pipe is the PASS (zero cost)");

		// the foamable-TE half: the owned CFoam BE answers hasFoam TRUE verbatim — NEVER a spray target
		GT6CFoamBlockEntity tFoam = sOwnedType.create(POS, sOwned.defaultBlockState());
		assertTrue(tFoam.hasFoam((byte)2), "the upstream :146 verbatim");
		assertFalse(gregtech6.item.foamspray.GT6FoamSprayItem.foamTarget(tFoam, (byte)2),
				"the ITileEntityFoamable arm rides !hasFoam — the owned foam is never a target");
	}

	// ------------------------------------------------------------------ the owned BE three-clause gate + NBT

	private static GT6CFoamBlockEntity ownedFoam() {
		GT6CFoamBlockEntity tFoam = sOwnedType.create(POS, sOwned.defaultBlockState());
		tFoam.setLevel(new MultiBlockLevel());
		return tFoam;
	}

	/** Upstream :113-115 {@code !mOwnable || !mFoamDried || super} — the UUID unwrap. */
	@Test
	public void threeClauseOwnershipGate() {
		// clause shape: wet OR unowned OR ownerless passes everyone
		GT6CFoamBlockEntity tFoam = ownedFoam();
		tFoam.configureSpray(GTSprayCanItem.DYES_INT[3], true, OWNER);
		assertTrue(tFoam.mOwnable && tFoam.mOwner != null, "the spray bundle recorded the owner");
		assertFalse(tFoam.mFoamDried, "a fresh spray is WET");
		assertTrue(tFoam.allowInteraction(FOREIGN), "clause 2: a WET owned foam passes EVERYONE");
		assertTrue(tFoam.allowInteraction(null), "the console passes the wet foam too");

		// the dry arms the lock (clause 3 = the super half)
		assertTrue(tFoam.dryFoam((byte)2, FOREIGN), "dryFoam has NO gate (the upstream :118-123 asymmetry)");
		assertTrue(tFoam.mFoamDried, "dried");
		assertFalse(tFoam.allowInteraction(FOREIGN), "a dried owned foam locks out the foreigner");
		assertFalse(tFoam.allowInteraction(null), "the console is not the owner");
		assertTrue(tFoam.allowInteraction(OWNER), "the owner passes");

		// clause 1: an UNOWNED (ownable=F) dried foam is public
		GT6CFoamBlockEntity tPublic = ownedFoam();
		tPublic.mFoamDried = true;
		assertTrue(tPublic.allowInteraction(FOREIGN), "unowned foam never arms the lock");

		// the ownerless owned foam (the console-sprayed owned can)
		GT6CFoamBlockEntity tOwnerless = ownedFoam();
		tOwnerless.configureSpray(GTSprayCanItem.DYES_INT[0], true, null);
		tOwnerless.mFoamDried = true;
		assertTrue(tOwnerless.allowInteraction(FOREIGN), "a null owner keeps the position open (the upstream super face)");
	}

	@Test
	public void nbtFourKeyFamilyRoundTrip() {
		GT6CFoamBlockEntity tFoam = ownedFoam();
		tFoam.configureSpray(GTSprayCanItem.DYES_INT[5], true, OWNER);
		tFoam.mFoamDried = true;

		CompoundTag tNBT = tFoam.saveWithoutMetadata();
		assertTrue(tNBT.contains(GT6CFoamBlockEntity.NBT_FOAMDRIED, Tag.TAG_ANY_NUMERIC), "upstream :66 unconditional");
		assertTrue(tNBT.contains(GT6CFoamBlockEntity.NBT_OWNABLE, Tag.TAG_ANY_NUMERIC), "upstream :67 unconditional");
		assertTrue(tNBT.hasUUID(GT6CFoamBlockEntity.NBT_OWNER), "upstream :68 — the owner rides while set");
		assertTrue(tNBT.getBoolean(GT6CFoamBlockEntity.NBT_FOAMDRIED));
		assertTrue(tNBT.getBoolean(GT6CFoamBlockEntity.NBT_OWNABLE));

		// the owner-less write drops ONLY the owner key
		GT6CFoamBlockEntity tOwnerless = ownedFoam();
		CompoundTag tBare = tOwnerless.saveWithoutMetadata();
		assertTrue(tBare.contains(GT6CFoamBlockEntity.NBT_FOAMDRIED));
		assertTrue(tBare.contains(GT6CFoamBlockEntity.NBT_OWNABLE));
		assertFalse(tBare.hasUUID(GT6CFoamBlockEntity.NBT_OWNER), "upstream :68 if (mOwner != null)");

		// the load leg — upstream :58-60 (the OWNERSHIP_RESET fold, the p24 deviation)
		GT6CFoamBlockEntity tLoaded = ownedFoam();
		tLoaded.load(tNBT);
		assertTrue(tLoaded.mFoamDried);
		assertTrue(tLoaded.mOwnable);
		assertEquals(OWNER, tLoaded.mOwner, "the UUID value form round-trips");
	}

	// ------------------------------------------------------------------ the foam faces + the dry transitions

	@Test
	public void freshBlockDriesIntoTheDriedBlockWithTheColour() {
		MultiBlockLevel tLevel = new MultiBlockLevel();
		BlockState tWet = sFresh.defaultBlockState().setValue(GT6CFoamFreshBlock.COLOR, 7);
		tLevel.mStates.put(POS, tWet);
		assertTrue(sFresh.hasFoam(tLevel, POS, null), "upstream :120");
		assertFalse(sFresh.driedFoam(tLevel, POS, null), "upstream :125");
		assertFalse(sFresh.applyFoam(tLevel, POS, null, 0xFFFFFF, 15), "upstream :105 — this block IS the foam");
		assertTrue(sFresh.dryFoam(tLevel, POS, null), "the scheduled-tick dry lands");
		BlockState tNow = tLevel.getBlockState(POS);
		assertTrue(tNow.is(sDried), "the dried target (upstream :110-112)");
		assertEquals(7, tNow.getValue(GT6CFoamFreshBlock.COLOR), "the colour meta passed through");
		// the dried face: dryFoam constant false, driedFoam true
		assertFalse(sDried.dryFoam(tLevel, POS, null), "upstream :58-60");
		assertTrue(sDried.driedFoam(tLevel, POS, null), "upstream :73-75");
		// the remover face: both become air
		assertTrue(sDried.removeFoam(tLevel, POS, null), "upstream :63-65");
		assertTrue(tLevel.getBlockState(POS).isAir(), "the position became air");
	}

	@Test
	public void freshSlabDriesIntoTheDriedSlabWithTheHalfAndColour() {
		MultiBlockLevel tLevel = new MultiBlockLevel();
		BlockState tWet = sFreshSlab.defaultBlockState()
				.setValue(GT6CFoamFreshBlock.COLOR, 12)
				.setValue(net.minecraft.world.level.block.SlabBlock.TYPE,
						net.minecraft.world.level.block.state.properties.SlabType.TOP);
		tLevel.mStates.put(POS, tWet);
		assertTrue(sFreshSlab.dryFoam(tLevel, POS, null));
		BlockState tNow = tLevel.getBlockState(POS);
		assertTrue(tNow.is(sDriedSlab), "the mSlabs same-position form");
		assertEquals(net.minecraft.world.level.block.state.properties.SlabType.TOP, tNow.getValue(net.minecraft.world.level.block.SlabBlock.TYPE),
				"the half type carried over");
		assertEquals(12, tNow.getValue(GT6CFoamFreshBlock.COLOR), "the colour carried over");
	}

	@Test
	public void ownedBlockCarriesTheDynamicFaces() {
		MultiBlockLevel tLevel = new MultiBlockLevel();
		GT6CFoamBlockEntity tFoam = sOwnedType.create(POS, sOwned.defaultBlockState());
		tFoam.setLevel(tLevel);
		tFoam.configureSpray(GTSprayCanItem.DYES_INT[3], true, OWNER); // the owned bundle (ownedFoam :148 needs mOwnable)
		tLevel.mStates.put(POS, sOwned.defaultBlockState());
		tLevel.mBlockEntities.put(POS, tFoam);

		// the wet light-block 1 (upstream :134 LIGHT_OPACITY_WATER)
		assertEquals(GT6CFoamOwnedBlock.LIGHT_BLOCK_WET, sOwned.defaultBlockState().getLightBlock(tLevel, POS),
				"the wet opacity rides the DRIED=F default");
		// the wet hardness leg — the destroyProgress seam (the upstream :136 ratio 4x)
		float tSuperProgress = 0.125F;
		assertEquals(4.0F * tSuperProgress, GT6CFoamOwnedBlock.foamDestroyProgress(false, tSuperProgress), 1e-6F,
				"wet mines at the CFoamFresh hardness (the upstream :136 delegation)");
		assertEquals(tSuperProgress, GT6CFoamOwnedBlock.foamDestroyProgress(true, tSuperProgress), 1e-6F,
				"dried mines at the CFoam hardness (the static numbers)");

		// the drying flip: the BE field + the DRIED property leg
		tFoam.mFoamDried = false;
		assertTrue(tFoam.dryFoam((byte)2, OWNER), "the no-gate dry");
		assertTrue(tLevel.getBlockState(POS).getValue(GT6CFoamOwnedBlock.DRIED), "the DRIED property leg flipped");
		assertEquals(GT6CFoamOwnedBlock.LIGHT_BLOCK_DRIED, tLevel.getBlockState(POS).getLightBlock(tLevel, POS),
				"the dried opacity MAX (upstream :134)");

		// the owned foam face: applyFoam CONSTANT false (upstream :149)
		assertFalse(tFoam.applyFoam((byte)2, OWNER, 0xFFFFFF, true), "the foam does not accept more foam");
		assertTrue(tFoam.ownedFoam((byte)2), "upstream :148");
		assertFalse(tFoam.removeFoam((byte)2, FOREIGN), "the dried owned foam rejects the foreign remover");
		assertTrue(tLevel.getBlockState(POS).is(sOwned), "the rejection removed nothing");
		assertTrue(tFoam.removeFoam((byte)2, OWNER), "the owner removes");
		assertTrue(tLevel.getBlockState(POS).isAir(), "upstream :128 — the position became air");
	}

	/** The drying ticker (upstream :97-104): server-side, matured, the 1/5900 roll, the injected rng seam. */
	@Test
	public void dryingTickerRollsTheUpstreamRng() {
		// the injected-roll subclass (the DryingPipe shape of GTPipeFoamTest)
		class RolledFoam extends GT6CFoamBlockEntity {
			int injectedRoll = 0;
			RolledFoam(BlockPos aPos) { super(sOwnedType, aPos, sOwned.defaultBlockState()); }
			@Override
			protected int rng(int aBound) { return injectedRoll; }
		}
		RolledFoam tFoam = new RolledFoam(POS);
		tFoam.setLevel(new MultiBlockLevel());
		assertFalse(tFoam.mFoamDried);

		// an immature TE never dries (upstream :100 aTimer >= 100)
		tFoam.injectedRoll = 0; // a hit roll
		tFoam.onTick(99L, true);
		assertFalse(tFoam.mFoamDried, "the maturity gate (aTimer >= DRY_MIN_AGE)");

		// a matured hit roll dries
		tFoam.onTick(100L, true);
		assertTrue(tFoam.mFoamDried, "rng(5900)==0 on a matured server tick dries (upstream :100-102)");

		// a missed roll never dries; the client side never dries
		RolledFoam tMissed = new RolledFoam(POS);
		tMissed.setLevel(tFoam.getLevel());
		tMissed.injectedRoll = 1;
		tMissed.onTick(1000L, true);
		assertFalse(tMissed.mFoamDried, "a missed roll leaves the foam wet");
		tMissed.injectedRoll = 0;
		tMissed.onTick(1000L, false);
		assertFalse(tMissed.mFoamDried, "the client side never dries (aIsServerSide gate)");
	}

	// ------------------------------------------------------------------ the BlockColor tint table

	@Test
	public void blockColorTintTable() {
		// the plain forms: the tint index 0 reads the COLOR property (the DYES_INT slot)
		for (int i = 0; i < 16; i++) {
			int tTint = GTCFoamTintListener.cfoamTintARGB(
					sFresh.defaultBlockState().setValue(GT6CFoamFreshBlock.COLOR, i), null, null, 0);
			assertEquals(0xFF000000 | GTSprayCanItem.DYES_INT[i], tTint, "the fresh tint of slot " + i);
		}
		// every non-zero tint index is no tint
		assertEquals(-1, GTCFoamTintListener.cfoamTintARGB(sFresh.defaultBlockState(), null, null, 1));
		assertEquals(-1, GTCFoamTintListener.cfoamTintARGB(sDried.defaultBlockState(), null, null, 7));
		// the owned BE leg: the BE paint — the -1 sentinel while unpainted/not synced
		assertEquals(-1, GTCFoamTintListener.cfoamTintARGB(sOwned.defaultBlockState(), null, null, 0),
				"no level, no BE — the sentinel");
		MultiBlockLevel tLevel = new MultiBlockLevel();
		GT6CFoamBlockEntity tFoam = sOwnedType.create(POS, sOwned.defaultBlockState());
		tFoam.configureSpray(GTSprayCanItem.DYES_INT[9], true, OWNER);
		tLevel.mBlockEntities.put(POS, tFoam);
		assertEquals(0xFF000000 | (GTSprayCanItem.DYES_INT[9] & 0xFFFFFF),
				GTCFoamTintListener.cfoamTintARGB(sOwned.defaultBlockState(), tLevel, POS, 0),
				"the owned tint reads the BE paint (the gt.color bundle)");
	}

	// ------------------------------------------------------------------ the generated-tree snapshots

	private static JsonObject generated(String aPath) throws Exception {
		try (InputStream tStream = GT6CFoamFamilyTest.class.getClassLoader().getResourceAsStream(aPath)) {
			assertNotNull(tStream, "the generated file must be on the classpath: " + aPath);
			return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
		}
	}

	@Test
	void generatedBlockstatesCarryTheTintedModels() throws Exception {
		for (String tPath : new String[] {"cfoam_fresh", "cfoam", "cfoam_owned", "cfoam_fresh_slab", "cfoam_slab"}) {
			JsonObject tState = generated("assets/gt6/blockstates/" + tPath + ".json");
			assertFalse(tState.getAsJsonObject("variants").entrySet().isEmpty(), tPath);
		}
		// every family model carries tintindex 0 (the BlockColor consumption face)
		for (String tModel : new String[] {"cfoam_fresh", "cfoam_hardened", "cfoam_fresh_owned",
				"cfoam_hardened_owned", "cfoam_fresh_slab_bottom", "cfoam_slab_top"}) {
			String tBody = generated("assets/gt6/models/block/" + tModel + ".json").toString();
			assertTrue(tBody.contains("\"tintindex\":0"), "tintindex 0 on " + tModel);
		}
		// the item models exist for the two DRIED forms ONLY (the fresh/owned forms have no BlockItem)
		assertNotNull(generated("assets/gt6/models/item/cfoam.json"));
		assertNotNull(generated("assets/gt6/models/item/cfoam_slab.json"));
	}

	@Test
	void generatedLootIsSelfForDriedAndEmptyForFreshAndOwned() throws Exception {
		// the dried self-drop
		for (String tPath : new String[] {"cfoam", "cfoam_slab"}) {
			JsonObject tTable = generated("data/gt6/loot_tables/blocks/" + tPath + ".json");
			assertEquals("gt6:" + tPath, tTable.getAsJsonArray("pools").get(0).getAsJsonObject()
					.getAsJsonArray("entries").get(0).getAsJsonObject().get("name").getAsString(), tPath);
		}
		// the fresh trio: EMPTY tables (no pools key)
		for (String tPath : new String[] {"cfoam_fresh", "cfoam_fresh_slab", "cfoam_owned"}) {
			assertFalse(generated("data/gt6/loot_tables/blocks/" + tPath + ".json").has("pools"),
					"the empty table (the upstream getDrops-empty/canDrop-F face): " + tPath);
		}
	}
}
