package gregtech6.covers;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import gregtech6.covers.covers.CoverTextureSimple;
import gregtech6.tileentity.machines.GTMachinesOfflineTestBase;

/**
 * Offline fixtures for the cover tests (task p4-cover-core acceptance ①/②) — reuses the
 * machine base (boot + recipe-map lifecycle + the public {@code MachineLevel} stub) and
 * adds the cover fixture set: an oven BET over the vanilla BRICKS fixture block (the
 * registries are frozen after boot; the machines-base precedent) and a CoverRegistry
 * mounted over vanilla items — the runtime mount ({@code GT6Covers.init()}) needs the
 * mod lifecycle (RegistryObject.get), which the offline JVM does not have.
 */
public abstract class GTCoverTestBase extends GTMachinesOfflineTestBase {

	/** The six vanilla items carrying the per-test covers — one per face of the round-trip. */
	static final Item[] COVER_ITEMS = {Items.BRICKS, Items.IRON_INGOT, Items.GOLD_INGOT, Items.DIAMOND, Items.EMERALD, Items.NETHERITE_INGOT};

	static final ResourceLocation TEST_SPRITE = new ResourceLocation("gt6", "block/cover/test_plate");

	static final BlockPos COVER_POS = new BlockPos(2, 2, 3);

	static BlockEntityType<TileEntityOvenCoverProbe> sCoverOvenType;

	@BeforeAll
	static void buildCoverOvenFixture() {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// NetworkHooks.init() failure is expected offline; registries are ready by now.
		}
		sCoverOvenType = BlockEntityType.Builder.of(
				(aPos, aState) -> new TileEntityOvenCoverProbe(sCoverOvenType, aPos, aState),
				Blocks.BRICKS).build(null);
	}

	/** A CoverTextureSimple whose sprite the tests can observe through the class contract. */
	static CoverTextureSimple testCover() {
		return new CoverTextureSimple(TEST_SPRITE);
	}

	/** An offline oven probe without a level (pure CoverData/store tests). */
	static TileEntityOvenCoverProbe bareOven() {
		return new TileEntityOvenCoverProbe(sCoverOvenType, COVER_POS, Blocks.BRICKS.defaultBlockState());
	}

	/** An offline oven probe on a stub server-side level. */
	static TileEntityOvenCoverProbe leveledOven() {
		TileEntityOvenCoverProbe tOven = bareOven();
		tOven.setLevel(new MachineLevel(new TestRecipeManager()));
		return tOven;
	}

	@BeforeEach
	void putCoverFixtures() {
		CoverRegistry.reset();
		for (Item tItem : COVER_ITEMS) CoverRegistry.put(tItem, testCover());
	}

	@AfterEach
	void clearCoverFixtures() {
		CoverRegistry.reset();
	}
}
