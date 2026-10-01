package gregtech6.crop;

import net.minecraft.data.PackOutput;

import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ConfiguredModel;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;

import gregtech6.registry.GT6CropSticks;

/**
 * The crop-stick asset provider (card cbc-1-cropstick-base)  --  the card-owned datagen band
 * (the GT6CrucibleDatagen.appendProviders form, registered by the GT6DataGenerators
 * tail-append seam). Two cross models over the two borrowed IC2 stick textures
 * ({@code stick.png} single / {@code stick_upgraded.png} crossing  --  the texture carries the
 * double-stick art, so no second model geometry), the cutout layer (the sapling form), and
 * the flat stick item model.
 */
public final class GT6CropStickAssets {

	private GT6CropStickAssets() {}

	public static void appendProviders(GatherDataEvent aEvent) {
		aEvent.getGenerator().addProvider(true, new Provider(
				aEvent.getGenerator().getPackOutput(), aEvent.getExistingFileHelper()));
	}

	static class Provider extends BlockStateProvider {

		Provider(PackOutput aOutput, ExistingFileHelper aHelper) {
			super(aOutput, "gt6", aHelper);
		}

		@Override
		protected void registerStatesAndModels() {
			ModelFile tSingle = models().cross("crop_sticks",
					modLoc("block/crop_stick")).renderType("cutout");
			ModelFile tCross = models().cross("crop_sticks_cross",
					modLoc("block/crop_stick_cross")).renderType("cutout");
			getVariantBuilder(GT6CropSticks.CROP_STICKS.get())
					.partialState().with(GT6CropSticksBlock.CROSSING, false)
					.addModels(new ConfiguredModel(tSingle))
					.partialState().with(GT6CropSticksBlock.CROSSING, true)
					.addModels(new ConfiguredModel(tCross));
			itemModels().withExistingParent("crop_stick", "item/generated")
					.texture("layer0", modLoc("item/crop_stick"));
		}

		@Override
		public String getName() {
			return "GT6 Crop Stick Assets";
		}
	}
}
