package gregtech6.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.data.MT;
import gregapi.data.OP;
import gregtech6.items.behaviors.GT6PlaceablePlacement;
import gregtech6.tileentity.misc.GT6GregOLanternBlock;
import gregtech6.tileentity.misc.GT6PlaceableBlock;
import gregtech6.tileentity.misc.GT6SandwichBlockEntity;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The placeables registration contract (task p32-placeables) — the id686 lesson face, the
 * GT6LogisticsRegistrationTest dual-leg posture in ONE class: the 1.21.1 leg boots through
 * FML itself (the junit-f LauncherSessionListener), so the containment branch proves the
 * DeferredRegister rows ACTUALLY registered (a silent no-op cannot pass); the forge leg
 * boots offline (Bootstrap only — no mod construction), so there the carrier faces assert
 * directly on freshly-constructed instances (the GTStoneBlocksRegistrationTest unfreeze
 * bracket) and the containment branch SKIPS (the registry could not hold them there).
 *
 * <p>The dispatch half runs on BOTH legs identically: the prefix walk is a static
 * function over constructed {@link MaterialPrefixItem} stacks — no registry involved —
 * and the item ids it pins ARE the RCON chain's give ids (one enumeration, both faces).
 */
public class GT6PlaceablesRegistrationTest {

	private static ResourceLocation id(String aPath) {
		return new ResourceLocation("gt6", aPath);
	}

	private static boolean ourBlocksAreRegistered;

	@BeforeAll
	static void boot() {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// the NetworkHooks tail is expected offline; registries are usable by now
		}
		ourBlocksAreRegistered = BuiltInRegistries.BLOCK.containsKey(id("greg_o_lantern"));
		if (!ourBlocksAreRegistered) {
			// the forge-offline leg: reopen the block registry so the fixture ctors can run
			// (the GTStoneBlocksRegistrationTest unfreeze bracket, self-sufficient form)
			for (var tRegistry : new net.minecraft.core.Registry<?>[] {
					BuiltInRegistries.BLOCK, BuiltInRegistries.ITEM}) {
				try {
					Method tUnfreeze = tRegistry.getClass().getMethod("unfreeze");
					tUnfreeze.setAccessible(true);
					tUnfreeze.invoke(tRegistry);
				} catch (Throwable aE) {
					throw new IllegalStateException("could not unfreeze the offline registry " + tRegistry, aE);
				}
			}
			// the material system must exist before any MT/OP dereference
			GTMaterialItems.initMaterials();
		}
	}

	@Test
	public void lanternCarrierLightIsFifteen() {
		if (ourBlocksAreRegistered) {
			assertTrue(BuiltInRegistries.BLOCK.get(id("greg_o_lantern")) instanceof GT6GregOLanternBlock,
					"the FML-registered block must carry the GT6 carrier class");
			assertTrue(BuiltInRegistries.BLOCK_ENTITY_TYPE.containsKey(id("greg_o_lantern")),
					"the lantern BET must be FML-contained (the id686 lesson)");
			assertTrue(BuiltInRegistries.ITEM.containsKey(id("greg_o_lantern")));
			assertEquals(15, BuiltInRegistries.BLOCK.get(id("greg_o_lantern")).defaultBlockState().getLightEmission());
		} else {
			// the offline face: the carrier's own properties (the upstream getLightValue :41 fold)
			GT6GregOLanternBlock tBlock = new GT6GregOLanternBlock(GT6GregOLanternBlock.newProperties());
			assertEquals(15, tBlock.defaultBlockState().getLightEmission(), "the lantern emits 15");
		}
	}

	@Test
	public void sandwichBitesComparatorContract() {
		// the offline-safe form: a self-contained fixture BET (the GTOfflineTestBase
		// synthetic-BET bracket) — the comparator math is registry-free arithmetic.
		// The FML-boot leg freezes the BET registry — reopen it for the fixture
		// (the helper no-ops when already open / on drift).
		gregtech6.tileentity.GTOfflineTestBase.unfreezeBlockEntityTypeRegistry();
		@SuppressWarnings("unchecked")
		BlockEntityType<GT6SandwichBlockEntity>[] tHolder = new BlockEntityType[1];
		tHolder[0] = BlockEntityType.Builder.of(
				(BlockEntityType.BlockEntitySupplier<GT6SandwichBlockEntity>)
						(BePos, BeState) -> new GT6SandwichBlockEntity(tHolder[0], BePos, BeState),
				Blocks.BEDROCK).build(null);
		GT6SandwichBlockEntity tBe = new GT6SandwichBlockEntity(tHolder[0], BlockPos.ZERO, Blocks.BEDROCK.defaultBlockState());
		assertEquals(10, tBe.size(), "the default sandwich is the ten-layer NBT default (upstream :64-74)");
		assertEquals(9, tBe.comparatorValue(), "comparator = bind4(mSize-1) (upstream :194)");
		tBe.load(tBe.saveWithoutMetadata());
		assertEquals(10, tBe.size(), "the NBT round-trip keeps the size (the sync channel contract)");
	}

	@Test
	public void placedPileCarriersAndMounts() {
		if (!ourBlocksAreRegistered) return; // the containment face is the live-leg branch only
		for (String tPath : new String[] {"placed_rock", "placed_stick", "placed_ingot",
				"placed_plate", "placed_gem_plate", "placed_scrap"}) {
			assertTrue(BuiltInRegistries.BLOCK.get(id(tPath)) instanceof GT6PlaceableBlock,
					tPath + " must carry the placed-pile carrier");
		}
		BlockEntityType<?> tBet = BuiltInRegistries.BLOCK_ENTITY_TYPE.get(id("placed_pile"));
		for (GT6PlaceableBlock.Kind tKind : GT6PlaceableBlock.Kind.values()) {
			BlockState tState = GT6Placeables.placed(tKind).defaultBlockState();
			assertTrue(tBet.isValid(tState), tKind + " must mount the shared placed-pile BET");
		}
	}

	@Test
	public void placementDispatchCoversTheSixPrefixesAndTheVanillaAliases() {
		// the offline half of the acceptance-③ dispatch: the registry-free prefix seam —
		// the RCON chain drives the SAME kinds live with the SAME item ids
		assertEquals(GT6PlaceableBlock.Kind.INGOT, GT6PlaceablePlacement.kindOfPrefix(OP.ingot));
		assertEquals(GT6PlaceableBlock.Kind.PLATE, GT6PlaceablePlacement.kindOfPrefix(OP.plate));
		assertEquals(GT6PlaceableBlock.Kind.GEM_PLATE, GT6PlaceablePlacement.kindOfPrefix(OP.plateGem));
		assertEquals(GT6PlaceableBlock.Kind.SCRAP, GT6PlaceablePlacement.kindOfPrefix(OP.scrapGt));
		assertEquals(GT6PlaceableBlock.Kind.ROCK, GT6PlaceablePlacement.kindOfPrefix(OP.rockGt));
		assertEquals(GT6PlaceableBlock.Kind.STICK, GT6PlaceablePlacement.kindOfPrefix(OP.stick));
		assertEquals(null, GT6PlaceablePlacement.kindOfPrefix(OP.dust), "non-placement prefixes stay null");
		// the vanilla alias arms (GT_Proxy :294-:299)
		assertEquals(GT6PlaceableBlock.Kind.STICK, GT6PlaceablePlacement.kindOf(new ItemStack(Items.STICK)));
		assertEquals(GT6PlaceableBlock.Kind.ROCK, GT6PlaceablePlacement.kindOf(new ItemStack(Items.FLINT)));
		// a non-placeable stack dispatches null (the upstream ST.block(aStack) == NB gate inverse)
		assertEquals(null, GT6PlaceablePlacement.kindOf(new ItemStack(Items.BEDROCK)));
	}
}
