package gregtech6.tileentity.misc;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import gregtech6.registry.GT6Placeables;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;

/**
 * The Greg o'Lantern BlockEntity — the port of upstream MTE 32758
 * {@code MultiTileEntityGregOLantern} (tmp/gt6-1.7.10 .../misc/MultiTileEntityGregOLantern
 * .java:37-56). The upstream class carries NO persisted NBT (the lantern is stateless
 * beyond its facing, which the block carrier holds as the FACING property), so the BE is
 * the stateless TE carrier: it exists to keep the "one decorative TE" registration face
 * (the task-card FML assertion) and the light/beat faces legible — the light itself rides
 * the blockstate {@code lightLevel(15)} (the upstream {@code getLightValue() :41} modern
 * declarative form, vanilla jack_o_lantern idiom) and {@code getLightOpacity() :40 NONE}
 * folds into it (an emitter never needs an opacity override).
 *
 * <p>Declared upstream-fold map (all :37-56): {@code getTexture2} → the datagen model (the
 * front face borrows the upstream GREG_O_LANTERN icon, the other five lit_pumpkin); the
 * lit_pumpkin hardness/resistance/explosion pair (:43-45) → the block carrier's vanilla
 * jack_o_lantern Properties; the solid-surface trio (:47-49) → a plain full-cube block;
 * {@code canDrop F :51} → the standard drop-self loot table (the upstream F meant the MTE
 * item never drops, the block drops itself via {@code getDrops}; the modern item IS the
 * loot face); {@code getValidSides SIDES_HORIZONTAL :52} → the horizontal-only facing
 * property.
 */
public class GT6GregOLanternBlockEntity extends TileEntityBase03TicksAndSync {

	/** BET factory for BlockEntityType.Builder.of. */
	public GT6GregOLanternBlockEntity(BlockPos aPos, BlockState aState) {
		this(null, aPos, aState);
	}

	@Override
	public String getTileEntityName() {
		return "greg_o_lantern"; // BET registry path mirrors it (GT6Placeables.GREG_O_LANTERN_BE)
	}

	/** Full constructor — also the offline (test) entry point (the GTBarrelBlockEntity null-type form). */
	public GT6GregOLanternBlockEntity(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(false, aType != null ? aType : GT6Placeables.GREG_O_LANTERN_BE.get(), aPos, aState);
	}
}
