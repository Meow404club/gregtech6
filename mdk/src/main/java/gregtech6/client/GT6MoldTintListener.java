package gregtech6.client;

import javax.annotation.Nullable;

import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import gregapi.oredict.OreDictMaterial;
import gregtech6.registry.GT6Crucibles;
import gregtech6.registry.GT6Molds;

/**
 * The material tint of the mold/faucet/smeltery smooth bodies (task
 * debt-material-tint — the #40-41 declared-deviation rework). The borrowed grayscale
 * materialicons blockSolid art (the {@code GT6CrucibleDatagen.bodyTexture} faces) renders
 * multiplied with the row material's {@code mRGBaSolid} — the upstream
 * {@code getTextureSmooth(mRGBaSolid, F)} semantics (OreDictMaterial.java:980-987, the
 * {@code BlockTextureDefault} colour multiply). The models carry tintindex 0 on the body
 * faces; this listener answers the colour over BOTH consumption halves:
 * <ul>
 * <li>the world face — {@code RegisterColorHandlersEvent.Block} over the family blocks
 *     (the {@code GTCFoamTintListener} shape: card-local subscriber, no GTClientHandlers
 *     touch);</li>
 * <li>the inventory face — {@code RegisterColorHandlersEvent.Item} over the same blocks'
 *     items (a BlockColor does NOT colour its BlockItem, the ItemColors.java:25-93 lesson
 *     the GTMachineTintModel kitchen card pinned).</li>
 * </ul>
 *
 * <p>The material rides the block carrier itself ({@link GT6Molds.MoldBlock#row},
 * {@link GT6Crucibles.CrucibleBlock#row}, the faucet row) — no parallel block→material
 * table. The vanilla smooth-stone rows (the finished-texture declaration shortcut of
 * {@code GT6CrucibleDatagen.bodyTinted}) answer {@code -1} = no tint, so a stray
 * tintindex on them stays inert. Untinted faces (the filled molten-content cubes, the
 * raw clay items) never reach the handler or are not registered at all.
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = "gt6", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6MoldTintListener {

	private GT6MoldTintListener() {
	}

	/**
	 * The pure seam the tests drive: the opaque ARGB tint for one material over one tint
	 * index (the {@code GTCFoamTintListener.cfoamTintARGB} form). The mRGBaSolid pack is
	 * the {@code GT6SurfaceRockBlock.java:103} precedent — the upstream mRGBaSolid short[4]
	 * as 0xFFRRGGBB; {@code -1} = no tint.
	 */
	public static int materialTintARGB(@Nullable OreDictMaterial aMaterial, int aTintIndex) {
		if (aTintIndex != 0 || aMaterial == null || !gregtech6.datagen.GT6CrucibleDatagen.bodyTinted(aMaterial)) return -1;
		return 0xFF000000 | (aMaterial.mRGBaSolid[0] << 16) | (aMaterial.mRGBaSolid[1] << 8) | aMaterial.mRGBaSolid[2];
	}

	/** The block carrier resolution — the material rides the row, unknown blocks stay un-tinted. */
	public static int blockTintARGB(@Nullable Block aBlock, int aTintIndex) {
		if (aBlock instanceof GT6Molds.MoldBlock tMold) return materialTintARGB(tMold.row().material().get(), aTintIndex);
		if (aBlock instanceof GT6Crucibles.CrucibleBlock tCrucible) return materialTintARGB(tCrucible.row().material().get(), aTintIndex);
		if (aBlock instanceof gregtech6.tileentity.tools.TileEntityFaucet.FaucetBlock tFaucet) return materialTintARGB(tFaucet.faucetRow().material().get(), aTintIndex);
		return -1;
	}

	/** The shared BlockColor over the whole family (the stone rows answer -1, inert). */
	public static BlockColor familyBlockColor() {
		return (aState, aLevel, aPos, aTintIndex) -> blockTintARGB(aState.getBlock(), aTintIndex);
	}

	/** The inventory half — the formed BlockItems resolve the same carrier (the kitchen-card shape). */
	public static ItemColor familyItemColor() {
		return (aStack, aTintIndex) -> aStack.getItem() instanceof BlockItem tBlockItem
				? blockTintARGB(tBlockItem.getBlock(), aTintIndex)
				: -1;
	}

	/** The family walk: the 32 mold rows + the 2 faucets + the 4 small smelteries. */
	private static Block[] familyBlocks() {
		Block[] rBlocks = new Block[GT6Molds.BLOCKS_BY_PATH.size()
				+ GT6Molds.FAUCET_BLOCKS_BY_PATH.size() + GT6Crucibles.BLOCKS_BY_PATH.size()];
		int i = 0;
		for (var tHandle : GT6Molds.BLOCKS_BY_PATH.values()) rBlocks[i++] = tHandle.get();
		for (var tHandle : GT6Molds.FAUCET_BLOCKS_BY_PATH.values()) rBlocks[i++] = tHandle.get();
		for (var tHandle : GT6Crucibles.BLOCKS_BY_PATH.values()) rBlocks[i++] = tHandle.get();
		return rBlocks;
	}

	@SubscribeEvent
	public static void onRegisterBlockColors(RegisterColorHandlersEvent.Block aEvent) {
		aEvent.getBlockColors().register(familyBlockColor(), familyBlocks());
	}

	@SubscribeEvent
	public static void onRegisterItemColors(RegisterColorHandlersEvent.Item aEvent) {
		Block[] tBlocks = familyBlocks();
		net.minecraft.world.item.Item[] tItems = new net.minecraft.world.item.Item[tBlocks.length];
		for (int i = 0; i < tBlocks.length; i++) tItems[i] = tBlocks[i].asItem();
		aEvent.getItemColors().register(familyItemColor(), tItems);
	}
}
