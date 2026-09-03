//? if neoforge {
/*package gregtech6.registry;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.component.CustomData;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;

// 1.21.1-only item DataComponent carriers (task p15-fork-carrier-components, ADR-P15-1).
// The 1.20.1 node carries these payloads as freeform ItemStack NBT, which 1.20.5+ replaces
// with data components; the payload KEYS keep their exact 1.20.1 shape byte-for-byte inside
// the opaque CustomData envelope (the 'tank' tank compound / the cover 's'..'x' lane keys) —
// only the carrier envelope changes. The 1.20.1 leg of this file is empty: the class does
// not exist there, and every 1.20.1 reference point stays inside the forge chisel branch.
//
// BARREL_CONTENT: the barrel item tank payload — the 1.20.1 item tag 'tank' compound
// (TileEntityBase08Barrel.NBT_TANK), read/written by GTBarrelItemFluidHandler,
// GTBarrelBlockItem and GTBarrelBlock.writeItemNBT.
// COVER_PAYLOAD: the per-cover payload lane snapshot riding a cover item — the 1.20.1
// item tag lane keys ('s'..'x' written by ICoverableTE.writeCoversToNBT), read/written by
// CoverData.set/getCoverItem and AbstractCoverDefault.getCoverItem.
//
// Registration hooks through the mod-bus RegisterEvent (the NeoForge 21.1 unified registry
// event, RegisterEvent.register(ResourceKey, ResourceLocation, Supplier) — javap truth on
// neoforge-21.1.249-universal.jar), keeping the card-local self-contained listener shape
// (ADR-P3-4). Deliberately a DeferredRegister-free shim: no mod-ctor wiring point exists
// for a 1.21.1-only file in the shared source, and the annotation scan needs none.
// Block-comment-free body by discipline: the wrapped leg tolerates no inner block comments
// (the wrapper is one), hence // comments only (ADR-P15-3 r1 implementation rule).

@EventBusSubscriber(modid = "gt6", bus = EventBusSubscriber.Bus.MOD)
public class GT6DataComponents {

	public static final DataComponentType<CustomData> BARREL_CONTENT = DataComponentType
			.<CustomData>builder().persistent(CustomData.CODEC).networkSynchronized(CustomData.STREAM_CODEC).build();

	public static final DataComponentType<CustomData> COVER_PAYLOAD = DataComponentType
			.<CustomData>builder().persistent(CustomData.CODEC).networkSynchronized(CustomData.STREAM_CODEC).build();

	@SubscribeEvent
	public static void onRegister(RegisterEvent aEvent) {
		aEvent.register(Registries.DATA_COMPONENT_TYPE,
				ResourceLocation.fromNamespaceAndPath("gt6", "barrel_content"), () -> BARREL_CONTENT);
		aEvent.register(Registries.DATA_COMPONENT_TYPE,
				ResourceLocation.fromNamespaceAndPath("gt6", "cover_payload"), () -> COVER_PAYLOAD);
	}
}
 *///?}
