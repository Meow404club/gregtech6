package gregtech6.client;

import javax.annotation.Nullable;

import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import gregtech6.block.tools.GT6MortarBlock;
import gregtech6.registry.GT6Mortars;

/**
 * The mortar family tint dispatch (task mortar-family) — the {@code GT6MoldTintListener}
 * card-local-subscriber form, two tint seats over ONE {@link BlockColor}/{@link ItemColor}
 * pair registered on the five rows:
 * <ul>
 * <li><b>index 0</b> — the bowl body: the constant Ceramic column every upstream row
 *     carries ({@code NBT_MATERIAL, MT.Ceramic}, Loader_MultiTileEntities.java:2179-2183;
 *     the upstream {@code mRGBa} multiply on the grayscale colored/ tiles,
 *     MultiTileEntityMortar.java:144-148). The spray-paint override is the declared
 *     render-pool cut (the class doc of the BE) — index 0 answers the material colour
 *     unconditionally, no PAINT snapshot read.</li>
 * <li><b>index 1</b> — the pestle: the row's {@code NBT_DESIGN} material of
 *     {@code MORTAR_MATERIALS} (MultiTileEntityMortar.java:149
 *     {@code MORTAR_MATERIALS[mStyle].fRGBaSolid} on the middle tiles) — the seat that
 *     makes the five variants visually distinct.</li>
 * </ul>
 * The colour math is NOT duplicated: both arms funnel into the shared
 * {@code GTBasicMachineBlock.materialColor} fRGBaSolid seam (the null material = white =
 * the vanilla {@code -1} sentinel identity). The overlay band carries NO tintindex (the
 * P22 decal contract) and passes through untouched. The value is the runtime
 * {@code BlockColor} route — the mold/crucible family's working form (the GTMachineTintModel
 * vertex-bake is the machine-domain route; the mortar rows carry no snapshot data).
 */
@Mod.EventBusSubscriber(modid = "gt6", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6MortarTint {

	/** The pure dispatch the tests drive: index 0 = the Ceramic body, index 1 = the row pestle, else no tint. */
	public static int blockTintARGB(@Nullable Block aBlock, int aTintIndex) {
		if (!(aBlock instanceof GT6MortarBlock tMortar)) return -1;
		if (aTintIndex == 0) return gregtech6.block.GTBasicMachineBlock.materialColor(GT6Mortars.BODY_MATERIAL.get());
		if (aTintIndex == 1) return gregtech6.block.GTBasicMachineBlock.materialColor(tMortar.pestle());
		return -1;
	}

	/** The shared BlockColor over the five rows (the GT6MoldTintListener.familyBlockColor form). */
	public static BlockColor familyBlockColor() {
		return (BlockState aState, @Nullable net.minecraft.world.level.BlockAndTintGetter aLevel,
				@Nullable BlockPos aPos, int aTintIndex) -> blockTintARGB(aState.getBlock(), aTintIndex);
	}

	/** The inventory half — the BlockItems resolve the same carrier (the mold form; a BlockColor does NOT colour its BlockItem, ItemColors.java:25-93). */
	public static ItemColor familyItemColor() {
		return (@Nullable ItemStack aStack, int aTintIndex) -> aStack != null && aStack.getItem() instanceof BlockItem tBlockItem
				? blockTintARGB(tBlockItem.getBlock(), aTintIndex)
				: -1;
	}

	@SubscribeEvent
	public static void onRegisterBlockColors(RegisterColorHandlersEvent.Block aEvent) {
		aEvent.getBlockColors().register(familyBlockColor(), GT6Mortars.BLOCKS_BY_PATH.values().stream()
				.map(tHandle -> (Block) tHandle.get()).toArray(Block[]::new));
	}

	@SubscribeEvent
	public static void onRegisterItemColors(RegisterColorHandlersEvent.Item aEvent) {
		aEvent.getItemColors().register(familyItemColor(), GT6Mortars.ITEMS_BY_PATH.values().stream()
				.map(tHandle -> tHandle.get()).toArray(net.minecraft.world.item.Item[]::new));
	}

	private GT6MortarTint() {}
}
