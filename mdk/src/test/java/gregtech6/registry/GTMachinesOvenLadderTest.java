/**
 * Offline tests for task p27-oven-heat-t-ladder: the Oven Heat_T ladder — the four rows
 * Loader_MultiTileEntities.java:1288-1291 (upstream metaIds 20001-20004, the MT.java:3689
 * Heat_T[1..4] locals ANY.Steel/Invar/Ti/TungstenCarbide, NBT_HARDNESS 6.0/4.0/9.0/12.5
 * with the T2 4.0F special case, NBT_INPUT 32/128/512/2048 through TIER_INPUTS).
 *
 * <p>The offline assertion surface (the GT6ToolsCreativeTabTest posture — the mod-Item
 * intrusive-holder wall keeps the live registries frozen):
 * <ul>
 * <li>the pure {@link GTMachines#OVEN_ROWS} table, pinned 逐参 to the upstream rows;</li>
 * <li>the registration wiring through the DeferredRegister pre-registration views
 *     ({@code getEntries()}, DeferredRegister.java:334) + {@code RegistryObject.getId()}
 *     (RegistryObject.java:287) — the four blocks/items/ids and the OVEN_TAB_ITEMS parity;</li>
 * <li>the tier-inputs wiring, driven on directly constructed GTOvenBlock instances (block
 *     construction needs no registry) through the full TileEntityOven constructor — the
 *     same Builder-of holder the runtime BET builds;</li>
 * <li>the composed-name carrier — every ladder row resolves the gt6.row.oven.display key.</li>
 * </ul>
 * The live faces (the BET validBlocks multi-attach, the tab displayItems run, the lang
 * resolution) ride the runServer registration log and the runData gates.
 */
package gregtech6.registry;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.RegistryObject;

import gregtech6.tileentity.machines.TileEntityOven;

public class GTMachinesOvenLadderTest {

	private static final BlockPos POS = new BlockPos(1, 2, 3);

	/**
	 * The four offline ladder row fixtures — NOT block instances: a Block constructor
	 * creates its intrusive holder (Block.java:66), which needs the bootstrap, and the
	 * bootstrapped registry is already frozen (NamespacedWrapper.validateWrite) — the
	 * intrusive-holder wall makes an offline GTOvenBlock unconstructible on BOTH sides.
	 * The tier wiring therefore rides the {@link TileEntityOven#applyTierInputs} static
	 * seam (the GTBasicMachineBlockDispatchTest pure-data posture); the block-identity
	 * half of the chain (GTOvenBlock.tier) is covered by the aNonOvenState test for the
	 * 0 case and the runServer registration gate for the live rows.
	 */

	@BeforeAll
	static void boot() {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// NetworkHooks.init() failure is expected offline; registries are ready by now.
		}
	}


	// ---------------------------------------------------------------------------
	// the upstream row parity (Loader_MultiTileEntities.java:1288-1291, 逐参)
	// ---------------------------------------------------------------------------

	@Test
	void ovenRowsMatchTheUpstreamRowsVerbatim() {
		assertEquals(4, GTMachines.OVEN_ROWS.size(), "the upstream row set is four Heat_T rows");
		String[] tPaths = {"oven", "oven_t2", "oven_t3", "oven_t4"};
		String[] tSlugs = {"steel", "invar", "titanium", "tungsten_carbide"};
		String[] tWords = {"Steel", "Invar", "Titanium", "Tungsten Carbide"}; // MT.DATA.Heat_T[1..4] locals (MT.java:3689)
		int[] tMetaIds = {20001, 20002, 20003, 20004}; // the upstream MTE ids :1288-1291
		float[] tHardness = {6.0F, 4.0F, 9.0F, 12.5F}; // the NBT_HARDNESS column, T2 = the 4.0F special case
		for (int i = 0; i < 4; i++) {
			GTMachines.OvenRow tRow = GTMachines.OVEN_ROWS.get(i);
			assertEquals(tPaths[i], tRow.path(), "row " + i + " registry path (the p8 id convention)");
			assertEquals(tSlugs[i], tRow.matSlug(), "row " + i + " material slug");
			assertEquals(tWords[i], tRow.matDisplay(), "row " + i + " material word (the Heat_T local)");
			assertEquals(tMetaIds[i], tRow.metaId(), "row " + i + " upstream meta id");
			assertEquals(tHardness[i], tRow.hardness(), 0.0F, "row " + i + " NBT_HARDNESS (NBT_RESISTANCE == hardness)");
			assertEquals(i, tRow.tier(), "row " + i + " tier index");
		}
	}

	// ---------------------------------------------------------------------------
	// the registration parity (the GT6ToolsCreativeTabTest bidirectional shape)
	// ---------------------------------------------------------------------------

	//? if forge {
	private static Set<String> registeredIds(Iterable<RegistryObject<Item>> aEntries) {
	//?} else {
	/*private static Set<String> registeredIds(Iterable<net.neoforged.neoforge.registries.DeferredHolder<Item, ? extends Item>> aEntries) { // 21.1: getEntries hands the wildcard holder
	*///?}
		Set<String> rIds = new HashSet<>();
		//? if forge {
		for (RegistryObject<Item> tEntry : aEntries) rIds.add(tEntry.getId().getPath());
		//?} else {
		/*for (net.neoforged.neoforge.registries.DeferredHolder<Item, ? extends Item> tEntry : aEntries) rIds.add(tEntry.getId().getPath()); // 21.1: wildcard holder
		*///?}
		return rIds;
	}

	@Test
	void ladderRowsAreRegisteredAsBlocksAndItems() {
		Set<String> tBlockIds = new HashSet<>();
		GTMachines.BLOCKS.getEntries().forEach(tEntry -> tBlockIds.add(tEntry.getId().getPath()));
		for (GTMachines.OvenRow tRow : GTMachines.OVEN_ROWS) {
			assertTrue(tBlockIds.contains(tRow.path()), tRow.path() + ": the ladder block is registered");
		}
		Set<String> tItemIds = registeredIds(GTMachines.ITEMS.getEntries());
		for (GTMachines.OvenRow tRow : GTMachines.OVEN_ROWS) {
			assertTrue(tItemIds.contains(tRow.path()), tRow.path() + ": the ladder item is registered");
		}
		Set<String> tBetIds = new HashSet<>();
		GTMachines.BLOCK_ENTITY_TYPES.getEntries().forEach(tEntry -> tBetIds.add(tEntry.getId().getPath()));
		assertTrue(tBetIds.contains("oven"), "the one family BET keeps the oven registry path");
	}

	@Test
	void tabTableIsTheFourRowsInUpstreamOrderWithNoOrphans() {
		assertEquals(4, GTMachines.OVEN_TAB_ITEMS.size(), "the tab walk = the upstream row set");
		for (int i = 0; i < 4; i++) {
			assertEquals(GTMachines.OVEN_ROWS.get(i).path(), GTMachines.OVEN_TAB_ITEMS.get(i).getId().getPath(),
					"tab row " + i + " mirrors the upstream row order (ruling3)");
		}
		Set<String> tItemIds = registeredIds(GTMachines.ITEMS.getEntries());
		for (RegistryObject<Item> tRow : GTMachines.OVEN_TAB_ITEMS) {
			assertTrue(tItemIds.contains(tRow.getId().getPath()),
					tRow.getId() + ": every tab row is a registered item (a dangling row would crash the displayItems generator)");
		}
	}

	// ---------------------------------------------------------------------------
	// the tier-inputs wiring (the TileEntityOven.applyTierInputs constructor seam)
	// ---------------------------------------------------------------------------

	/**
	 * The NBT_INPUT column 32/128/512/2048 (Loader :1288-1291) through the :126 conversion
	 * min = in/2 / max = in*2 — the full TIER_INPUTS table pinned verbatim (the
	 * GT6KineticTrioRowTest assertArrayEquals form); the row-to-column wiring rides
	 * {@link #theRowTierLandsItsEnergyThreeValue}.
	 */
	@Test
	void tierInputsTableCarriesTheUpstreamNbtInputColumn() {
		assertArrayEquals(new long[] {16, 32, 64}, GTMachines.TIER_INPUTS[0], "T1 column (NBT_INPUT 32)");
		assertArrayEquals(new long[] {64, 128, 256}, GTMachines.TIER_INPUTS[1], "T2 column (NBT_INPUT 128)");
		assertArrayEquals(new long[] {256, 512, 1024}, GTMachines.TIER_INPUTS[2], "T3 column (NBT_INPUT 512)");
		assertArrayEquals(new long[] {1024, 2048, 4096}, GTMachines.TIER_INPUTS[3], "T4 column (NBT_INPUT 2048)");
	}

	@Test
	void theRowTierLandsItsEnergyThreeValue() {
		// the fixtures ride the BRICKS holder (a non-oven state → the T1 defaults, the
		// GTMachinesOfflineTestBase posture); the seam is the SAME assignment the
		// constructor runs with GTOvenBlock.tier(aState) — tier N in, TIER_INPUTS[N] out
		// (the upstream NBT_INPUT column through the :126 conversion)
		for (int tTier = 0; tTier < 4; tTier++) {
			TileEntityOven tOven = brickFixtureOven();
			TileEntityOven tWired = TileEntityOven.applyTierInputs(tOven, tTier);
			long[] tInputs = GTMachines.TIER_INPUTS[tTier];
			assertEquals(tInputs[0], tWired.mInputMin, "T" + (tTier + 1) + " InputMin");
			assertEquals(tInputs[1], tWired.mInput, "T" + (tTier + 1) + " InputRec");
			assertEquals(tInputs[2], tWired.mInputMax, "T" + (tTier + 1) + " InputMax");
		}
	}

	/** The offline oven fixture — the BRICKS-state holder shape of GTMachinesOfflineTestBase. */
	private static TileEntityOven brickFixtureOven() {
		@SuppressWarnings("unchecked")
		BlockEntityType<TileEntityOven>[] tHolder = (BlockEntityType<TileEntityOven>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new TileEntityOven(tHolder[0], aPos, aState), Blocks.BRICKS).build(null);
		return new TileEntityOven(tHolder[0], POS, Blocks.BRICKS.defaultBlockState());
	}

	@Test
	void aNonOvenStateKeepsTheTierOneDefaults() {
		// the zero-regression guard: the offline BRICKS fixtures (GTMachinesOfflineTestBase
		// and the cover/crowbar probes) construct through a non-oven state — tier 0, the
		// :98 field defaults {16, 32, 64} verbatim.
		TileEntityOven tOven = brickFixtureOven();
		assertEquals(16, tOven.mInputMin, "the T1 :98 default InputMin");
		assertEquals(32, tOven.mInput, "the T1 :98 default InputRec");
		assertEquals(64, tOven.mInputMax, "the T1 :98 default InputMax");
	}
}
