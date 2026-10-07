package gregtech6.tileentity.misc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.Locale;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.oredict.OreDictMaterial;
import gregtech6.item.MaterialPrefixItem;
import gregtech6.tileentity.GTOfflineTestBase;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * The placed-pile Nameable identity face (task r11-placed-rock-identity-drops, the
 * surface_rock-parity ruling on known_bugs.r11-rock-line-broken.user_evidence): the pile
 * BE's Nameable contract IS the Jade TITLE name — the default ObjectNameProvider streams
 * a Nameable BE's custom name as the tooltip title on both legs (jade-1201 streamData
 * javap: Nameable + hasCustomName → {@code givenName} = toJson(getCustomName());
 * jade-1211 ObjectNameProvider.java:152-157: hasCustomName → stream getDisplayName()).
 * That is the {@link gregtech6.block.surface.GT6SurfaceRockBlock} display canon
 * transplanted to the BE-carried identity — the head row reads 含铁岩石, not 石头+桥行.
 *
 * <p>The pins (the Jade legs themselves are compile-only offline — the r5/r8 注记惯例;
 * the live look is field_test):
 * <ol>
 * <li>the stored stack IS the custom name — getString-equality with the stack's own
 *     hover name, and the rockGt word rides the key/word either way it resolves;</li>
 * <li>{@link #getDisplayName()} (the 1.21.1 stream face) matches the custom name when
 *     present — the two legs carry the same word;</li>
 * <li>the empty pile stays null-custom — every consumer falls back to the block name
 *     (石头, the upstream Rock);</li>
 * <li>a vanilla borrow (flint) carries its own item name.</li>
 * </ol>
 */
public class GT6PlaceableIdentityNameableTest extends GTOfflineTestBase {

	@BeforeAll
	static void boot() {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// the NetworkHooks tail is expected offline; registries are usable by now
		}
		// the material system must exist before any MT/OP dereference (the
		// GT6PlaceablesRegistrationTest offline bracket) — gated on the LOOKUP (not the
		// item registry: a sibling test class may have already refilled on this shared
		// JVM, and the FML-prebooted 21.1 leg must NOT re-run the non-reentrant init).
		if (gregtech6.registry.GTMaterialItems.get(OP.rockGt, MT.Iron) == null) {
			try {
				Class<?> tItems = Class.forName("gregtech6.registry.GTMaterialItems");
				Method tInit = tItems.getMethod("initMaterials");
				tInit.setAccessible(true);
				tInit.invoke(null);
			} catch (Throwable aE) {
				throw new IllegalStateException("could not init the material system offline", aE);
			}
		}
	}

	/** The rockGt stack source: the real item when the lookup is LIVE, a fixture instance otherwise. The fixture mounts under a probe key (NOT the real gt6:rock_gt_* id space — a same-key fixture would flip the sibling material-link test's containsKey gate into its real-item branch, whose INDEX lookup is dead offline; the shared-JVM order contract, 2026-10-03). */
	private static ItemStack rockGt(OreDictMaterial aMaterial) {
		var tHandle = gregtech6.registry.GTMaterialItems.get(OP.rockGt, aMaterial);
		if (tHandle != null && tHandle.get() != null) {
			return new ItemStack(tHandle.get());
		}
		String tSnake = MaterialPrefixItem.snakeCase(aMaterial.mNameInternal);
		Item tItem = registerItemFixture("probe_rock_gt_" + tSnake,
				() -> new MaterialPrefixItem(new Item.Properties(), OP.rockGt, aMaterial));
		return new ItemStack(tItem);
	}

	/** A fresh pile BE over the synthetic-BET bracket (the material-link test form). */
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
	public void theStoredRockIsTheTitleName() {
		ItemStack tRock = rockGt(MT.Iron);
		GT6PlaceableBlockEntity tPile = freshPile();
		tPile.setStack(tRock);
		assertTrue(tPile.hasCustomName(), "a loaded pile must be Nameable-custom (the Jade stream gate)");
		String tOwn = tRock.getHoverName().getString();
		assertEquals(tOwn, tPile.getCustomName().getString(),
				"the custom name IS the stored stack's hover name (the 1.20.1 givenName face)");
		String tLower = tPile.getCustomName().getString().toLowerCase(Locale.ROOT);
		assertTrue(tLower.contains("rock_gt") || tLower.contains("rock"),
				"the identity word rides the name either way it resolves (key form or lang form): "
						+ tPile.getCustomName().getString());
	}

	@Test
	public void theDisplayNameFaceMatchesTheCustomName() {
		GT6PlaceableBlockEntity tPile = freshPile();
		tPile.setStack(rockGt(MT.Iron));
		// the 1.21.1 ObjectNameProvider streams getDisplayName() (jade-1211 :156) — it must
		// carry the SAME word as the 1.20.1 givenName face, not the block fallback.
		assertEquals(tPile.getCustomName().getString(), tPile.getDisplayName().getString(),
				"the 1.21.1 stream face (getDisplayName) matches the 1.20.1 face (getCustomName)");
	}

	@Test
	public void anEmptyPileFallsBackToTheBlockName() {
		GT6PlaceableBlockEntity tPile = freshPile();
		assertFalse(tPile.hasCustomName(), "the empty pile has no custom name — the Jade fallback gate");
		assertNull(tPile.getCustomName());
		assertEquals(tPile.getBlockState().getBlock().getName().getString(), tPile.getDisplayName().getString(),
				"the empty pile's display name is the carrier block's own name (石头)");
	}

	@Test
	public void aVanillaBorrowCarriesItsOwnName() {
		GT6PlaceableBlockEntity tPile = freshPile();
		tPile.setStack(new ItemStack(Items.FLINT));
		assertEquals(new ItemStack(Items.FLINT).getHoverName().getString(), tPile.getCustomName().getString(),
				"the flint pile reads flint — no invented material word");
	}
}
