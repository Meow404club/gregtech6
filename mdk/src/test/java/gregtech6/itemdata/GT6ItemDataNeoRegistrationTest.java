//? if neoforge {
/*package gregtech6.itemdata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.data.MT;

// S31-1 review regression: the 1.21.1 DC registration dead-link. GT6ItemData's
// @EventBusSubscriber <clinit> runs at mod construct, BEFORE any key-holding class
// is ever loaded — the deleted <clinit> snapshot of GT6DataKey.registry() was
// permanently empty and onRegister registered ZERO components, while the in-memory
// set/get into a null component key kept the old tests falsely green. The fix is
// the explicit key-holder manifest + eager per-key DataComponentType construction
// + live registry() iteration. This test runs in the FML test JVM (the same boot
// shape as the production mod construct) and pins the REGISTRY truth plus the
// ItemStack encode face, not just the in-memory seam.
public class GT6ItemDataNeoRegistrationTest {

	@BeforeAll
	static void boot() {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// NetworkHooks.init() failure is expected offline; registries are ready by now.
		}
		MT.init();
	}

	@Test
	void materialToolRecipeSerializerIsRegistered() {
		// task p31-dig-ladder: the gt6:material_tool serializer rides the same
		// mod-construct DeferredRegister attach — the FML JVM pins the registry truth
		// (the lesson id686 form: cleanTest green does not prove a registration face alive)
		ResourceLocation tId = ResourceLocation.fromNamespaceAndPath("gt6", "material_tool");
		assertTrue(BuiltInRegistries.RECIPE_SERIALIZER.containsKey(tId),
				"gt6:material_tool must sit in the recipe serializer registry after the mod construct");
		assertNotNull(BuiltInRegistries.RECIPE_SERIALIZER.get(tId), "the registered serializer resolves");
	}

	@Test
	void toolStatsComponentIsRegistered() {
		ResourceLocation tId = ResourceLocation.fromNamespaceAndPath("gt6", "tool_stats");
		assertTrue(BuiltInRegistries.DATA_COMPONENT_TYPE.containsKey(tId),
				"gt6:tool_stats must sit in the DC registry after the mod construct (the S31-1 dead-link regression)");
		assertNotNull(BuiltInRegistries.DATA_COMPONENT_TYPE.get(tId),
				"the registered type resolves");
		assertEquals(1, GT6DataKey.registry().size(), "the manifest loaded the key holder(s)");
	}

	@Test
	void keyedStackEncodesThroughTheItemStackFace() {
		// the disk/sync envelope — with the dead link this exact encode NPEd
		// ("datacomponenttype is null") the moment a keyed stack hit save or sync
		RegistryAccess.Frozen tAccess = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
		ItemStack tStack = new ItemStack(Items.STICK);
		GT6ItemData.set(tStack, GT6ToolStats.KEY, GT6ToolStats.of(MT.Steel, null, 1.0F));

		Optional<Tag> tEncoded = ItemStack.CODEC
				.encodeStart(tAccess.createSerializationContext(NbtOps.INSTANCE), tStack)
				.resultOrPartial(tMsg -> {});
		assertTrue(tEncoded.isPresent(), "the ItemStack encode face serializes the keyed component");
		assertTrue(tEncoded.get().toString().contains("gt6:tool_stats"),
				"the encoded stack carries the registered component id");
		assertSame(MT.Steel, GT6ItemData.get(tStack, GT6ToolStats.KEY).primaryMaterial(),
				"the in-memory seam stays consistent over the registered carrier");
	}
}
 *///?}
