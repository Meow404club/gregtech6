package gregtech6.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.oredict.OreDictMaterial;
import gregtech6.item.MaterialPrefixItem;
import gregtech6.items.behaviors.GT6PlaceablePlacement;
import gregtech6.tileentity.GTOfflineTestBase;
import gregtech6.tileentity.misc.GT6PlaceableBlock;
import gregtech6.tileentity.misc.GT6PlaceableBlockEntity;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * The surface-rock material data chain (task surface-rock-material-link, the user
 * report "shift+放下后全部变成石头类型" follow-up of issue #47): the placed pile must
 * CARRY the held rockGt identity through every face the render tint and the pickup
 * read —
 * <ol>
 * <li>the placement dispatch keys the ROCK kind off the SAME MaterialPrefixItem
 *     identity that carries the material ({@link GT6PlaceablePlacement#kindOf});</li>
 * <li>the pile BE derives {@code material()} live from the stored stack (the tint
 *     source, GT6PlaceableTint) — three sampled bands (iron / gold / the gem
 *     diamond) place and read back their own material;</li>
 * <li>the identity survives the sync channel (getUpdateTag = saveWithoutMetadata →
 *     load — the chunk-data AND block-update payloads both ride it);</li>
 * <li>a material-less borrow (vanilla flint) stays null — the -1 → vanilla stone
 *     grey fallback (the #47 seat note);</li>
 * <li>the pickup → re-place loop reads the same identity back out (the give-arm /
 *     playerDestroy drop source is {@code stack()}).</li>
 * </ol>
 *
 * <p>Dual-leg posture of {@link GT6PlaceablesRegistrationTest}: the FML leg uses the
 * real registered rockGt items, the offline leg mounts fixture items under the same
 * gt6:rock_gt_* keys ({@link GTOfflineTestBase#registerItemFixture}).
 */
public class GT6SurfaceRockMaterialLinkTest extends GTOfflineTestBase {

	private static boolean ourItemsRegistered;

	@BeforeAll
	static void boot() {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// the NetworkHooks tail is expected offline; registries are usable by now
		}
		ourItemsRegistered = BuiltInRegistries.ITEM.containsKey(new ResourceLocation("gt6", "rock_gt_iron"));
		// The hermetic bracket (reset-first, task hermetic-fml-seam-generation): the material
		// universe is class-local on BOTH legs — a prior batch class's materials() reflow
		// cannot leave a stale generation here. Replaces the offline-only reflection arm
		// (same package, the reflection indirection bought nothing — GT6MaterialTestSupport).
		GT6MaterialTestSupport.materials();
	}

	/** The rockGt stack source: the real item on the FML leg, a fixture instance offline. */
	private static ItemStack rockGt(OreDictMaterial aMaterial) {
		if (ourItemsRegistered) {
			// NAME identity over the frozen registration INDEX (the GT6MaterialToolJeiExtensionTest
			// rule): the INDEX keys freeze the boot-generation material objects and OreDictMaterial
			// has no equals, so after any batch class's hermetic reset the re-flooded MT static
			// cannot identity-hit the direct seam — across generations the pair identity is the
			// name (the prefix fields are OP.init-once, stable across reflows).
			for (var tEntry : GTMaterialItems.items().entrySet()) {
				if (tEntry.getKey().prefix() == OP.rockGt
						&& aMaterial.mNameInternal.equals(tEntry.getKey().material().mNameInternal)) {
					return new ItemStack(tEntry.getValue().get());
				}
			}
			throw new IllegalStateException("no registered rockGt item for " + aMaterial.mNameInternal);
		}
		String tSnake = MaterialPrefixItem.snakeCase(aMaterial.mNameInternal);
		MaterialPrefixItem tItem = registerItemFixture("rock_gt_" + tSnake,
				() -> new MaterialPrefixItem(new Item.Properties(), OP.rockGt, aMaterial));
		return new ItemStack(tItem);
	}

	/** A fresh pile BE over the GTOfflineTestBase synthetic-BET bracket (the sandwich test form). */
	private static GT6PlaceableBlockEntity freshPile() {
		@SuppressWarnings({"unchecked", "rawtypes"})
		BlockEntityType<GT6PlaceableBlockEntity>[] tHolder = new BlockEntityType[1];
		tHolder[0] = BlockEntityType.Builder.of(
				(BlockEntityType.BlockEntitySupplier<GT6PlaceableBlockEntity>)
						(BePos, BeState) -> new GT6PlaceableBlockEntity(tHolder[0], BePos, BeState),
				Blocks.BEDROCK).build(null);
		return new GT6PlaceableBlockEntity(tHolder[0], BlockPos.ZERO, Blocks.BEDROCK.defaultBlockState());
	}

	@Test
	public void threeMaterialsPlaceAndReadBackTheirOwnIdentity() {
		for (OreDictMaterial tMaterial : new OreDictMaterial[] {MT.Iron, MT.Gold, MT.Diamond}) {
			ItemStack tRock = rockGt(tMaterial);
			assertSame(GT6PlaceableBlock.Kind.ROCK, GT6PlaceablePlacement.kindOf(tRock),
					tMaterial.mNameInternal + " rockGt dispatches the ROCK pile kind");
			GT6PlaceableBlockEntity tPile = freshPile();
			tPile.setStack(tRock);
			// NAME identity, not instance identity: the FML-registered item freezes its
			// boot-generation material object, and a hermetic reset in this or a batch
			// sibling class legitimately re-floods MT (hermetic-fml-seam-generation).
			assertEquals(tMaterial.mNameInternal, tPile.material().mNameInternal,
					tMaterial.mNameInternal + " placed pile must read back its own material (the tint source)");
		}
	}

	@Test
	public void pileIdentitySurvivesTheSyncChannel() {
		ItemStack tRock = rockGt(MT.Iron);
		GT6PlaceableBlockEntity tPile = freshPile();
		tPile.setStack(tRock);
		// getUpdateTag = saveWithoutMetadata (TileEntityBase03TicksAndSync) — both sync
		// channels converge on load(); the BE must rehydrate the carried identity.
		CompoundTag tTag = tPile.saveWithoutMetadata();
		assertTrue(tTag.contains(GT6PlaceableBlockEntity.NBT_VALUE, CompoundTag.TAG_COMPOUND),
				"the update tag must carry the pile contents key");
		GT6PlaceableBlockEntity tClientPile = freshPile();
		tClientPile.load(tTag);
		assertEquals(MT.Iron.mNameInternal, tClientPile.material().mNameInternal, "the client-side BE rehydrates the material");
		assertEquals(tRock.getItem(), tClientPile.stack().getItem(), "the carried item identity rides the channel");
		assertEquals(1, tClientPile.stack().getCount(), "the rock pile consumes one item");
	}

	@Test
	public void materiallessBorrowsStayOnTheStoneFallback() {
		GT6PlaceableBlockEntity tPile = freshPile();
		tPile.setStack(new ItemStack(Items.FLINT));
		assertNull(tPile.material(), "the vanilla flint borrow carries no material — the -1 → stone grey fallback");
	}

	@Test
	public void pickupAndRePlaceLoopKeepsTheIdentity() {
		GT6PlaceableBlockEntity tPile = freshPile();
		tPile.setStack(rockGt(MT.Gold));
		// the pickup faces (give arm / playerDestroy drop) read stack() and hand out
		// copies — the re-place writes the copy into a fresh pile.
		ItemStack tGiven = tPile.stack().copy();
		tGiven.setCount(1);
		GT6PlaceableBlockEntity tReplaced = freshPile();
		tReplaced.setStack(tGiven);
		assertEquals(MT.Gold.mNameInternal, tReplaced.material().mNameInternal, "pickup → re-place keeps the material identity");
		assertEquals(tPile.stack().getItem(), tReplaced.stack().getItem());
	}

	@Test
	public void theConfirmResendAndClientRebakeFacesStayOfflineSafe() {
		// task surface-rock-material-link — the two hardening faces (the tick()+4 confirm
		// re-send, the load client-arm re-bake) both route through the base side guards:
		// offline (no level) they are no-ops, and the data path stays exactly as pinned.
		GT6PlaceableBlockEntity tPile = freshPile();
		tPile.setStack(rockGt(MT.Iron));
		tPile.sendClientData(); // no level offline — the tick() resend body must no-op, not throw
		tPile.load(tPile.saveWithoutMetadata()); // the client re-bake arm: hasLevel()=false — no-op
		assertEquals(MT.Iron.mNameInternal, tPile.material().mNameInternal, "the hardening faces leave the data path untouched");
	}
}
