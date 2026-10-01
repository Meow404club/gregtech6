package gregtech6.crop.behavior;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import gregtech6.crop.GT6CropBlockEntity;

/**
 * The sense/scythe crop arm  --  card cbc-4-crop-tools hook ④. Upstream
 * gregapi/block/ToolCompat.java:174-183, the right-click TOOL_sense/TOOL_scythe face
 * (Behavior_Tool -> IBlockToolable -> ToolCompat): a 3x3x3 walk over the clicked tile
 * ({@code for i,j,k in -1..1}, the CENTRE included  --  :177 has no skip), every crop
 * tile that harvests pays {@code tDamage += 10000} (:179) and plays the collect sound
 * (:178), then the {@code aCanCollect}-gated {@code WD.suckAll} sweep (:181).
 *
 * <p><b>The damage fold</b>: upstream tools spend 100 damage per use-point, so 10000 =
 * one use-point per harvested tile; the port item face (GTSenseItem, 1 point per
 * break/spend) pays one durability point per harvested tile  --  the same fold the
 * w5-t4 field-five card ruled for the sense tool family.
 *
 * <p><b>The suck gate</b>: upstream {@code aCanCollect} reads the tool's
 * {@code canCollectDropsDirectly} (:102 = ToolStats.canCollect() || AUTO_COLLECTING
 * material, MultiItemTool.java:215-218); ToolStats.canCollect() defaults false and
 * GT_Tool_Sense never overrides, so the plain sense does NOT sweep. The gate lives at
 * the item hook; {@link #suckBox} carries the verbatim 4x2x4 box
 * ({@code (x-1.5, y-0.5, z-1.5)} to {@code (+2.5, +1.5, +2.5)}, WD.java:111-125) for
 * the collecting-materials upgrade path and the offline pins.
 */
public final class CropScytheBehavior {

	private CropScytheBehavior() {}

	/**
	 * The 3x3x3 harvest  --  :177-180: each in-range crop tile that actually harvests
	 * spills its drops into the world (upstream {@code harvest(T)} is a world-drop face)
	 * and counts one damage point. Returns the point count (upstream {@code tDamage}
	 * /10000-folded).
	 */
	public static int harvestArea(Level aLevel, BlockPos aCenter) {
		int rDamage = 0;
		for (int i = -1; i < 2; i++) for (int j = -1; j < 2; j++) for (int k = -1; k < 2; k++) {
			BlockPos tPos = aCenter.offset(i, j, k);
			if (!(aLevel.getBlockEntity(tPos) instanceof GT6CropBlockEntity tCrop)) continue;
			List<ItemStack> tDrops = tCrop.performHarvest();
			if (tDrops == null) continue; // not harvestable (immature/empty)  --  upstream harvest(T) false
			for (ItemStack tDrop : tDrops) {
				Containers.dropItemStack(aLevel, tPos.getX() + 0.5, tPos.getY() + 0.5, tPos.getZ() + 0.5, tDrop);
			}
			aLevel.playSound(null, tPos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2F, 1.0F); // :178 SFX.MC_COLLECT
			rDamage++; // :179 tDamage += 10000, the one-use-point fold
		}
		return rDamage;
	}

	/**
	 * The WD.suckAll sweep  --  :181 with WD.java:111-125 semantics: every item entity
	 * inside the 4x2x4 box is removed and its stack returned (the caller gives them to
	 * the player, the upstream {@code ST.give} arm). Level-doubles in tests answer the
	 * entity query.
	 */
	public static List<ItemStack> suckBox(Level aLevel, BlockPos aCenter) {
		AABB tBox = new AABB(
				aCenter.getX() - 1.5, aCenter.getY() - 0.5, aCenter.getZ() - 1.5,
				aCenter.getX() + 2.5, aCenter.getY() + 1.5, aCenter.getZ() + 2.5);
		List<ItemStack> rOutput = new ArrayList<>();
		for (ItemEntity tItem : aLevel.getEntitiesOfClass(ItemEntity.class, tBox)) {
			if (tItem.isRemoved()) continue; // the upstream !isDead gate
			rOutput.add(tItem.getItem());
			tItem.discard();
		}
		return rOutput;
	}
}
