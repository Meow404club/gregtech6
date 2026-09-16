package gregtech6.items.tools;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.util.RandomSource;
import net.minecraftforge.common.ToolAction;

/**
 * The formal magnifying glass — item id {@code gt6:magnifying_glass} (task
 * p29-w5-t3-machine-face-four spec ③). Upstream meta id {@code ToolsGT.MAGNIFYING_GLASS =
 * 62} (CS.java:1736), mounted by the Loader_Tools.java:148 registration row (display name
 * "Magnifying Glass", tagline "Crafted with a Stick and a Lens") over
 * {@code GT_Tool_MagnifyingGlass} (machine/GT_Tool_MagnifyingGlass.java:35).
 *
 * <p>The pure right-click check face: {@code useOn} plays the AHA/HMM inspection sound and
 * touches NOTHING — no state change, no entity interaction, no durability (the RCON
 * acceptance's zero-change assertions). The machine-side detail faces (the
 * {@code TOOL_TO_DETAIL_MAGNIFYINGGLASS} consumers — Basin/Mold/GearBox/...) are the
 * machine-interaction POOL (the RED LINE); the port tool-side face is the sound arm only.
 *
 * <p>The sound mapping (the card's open-question ruling): upstream plays
 * {@code RNGSUS.nextInt(3) == 0 ? SFX.MC_HMM : SFX.MC_AHA} (:67-69) where 1.7.10
 * {@code MC_HMM = "mob.villager.idle"} and {@code MC_AHA = "mob.villager.haggle"}
 * (CS.java:2235-2236) — the villager voice pair maps to the modern
 * {@link SoundEvents#VILLAGER_NO} ("entity.villager.no", the doubting HMM) and
 * {@link SoundEvents#VILLAGER_YES} ("entity.villager.yes", the affirming AHA), same
 * 1-in-3 HMM ratio verbatim. Zero durability on the check face is the declared deviation
 * (the upstream 100-unit Behavior_Tool click folded to zero — the item stays strictly
 * read-only, the acceptance asserts it); the crafting-loss face keeps the shared one-point
 * mapping (the upstream :47-49 400-unit row folded).
 */
public class GTMagnifyingGlassItem extends Item {

	/** The vanilla durability points — single steel tier 512 (the family value). */
	public static final int DURABILITY_POINTS = 512;

	/** The AHA sound — the 1.7.10 "mob.villager.haggle" modern counterpart (CS.java:2236). */
	public static final SoundEvent AHA_SOUND = SoundEvents.VILLAGER_YES;

	/** The HMM sound — the 1.7.10 "mob.villager.idle" modern counterpart (CS.java:2235). */
	public static final SoundEvent HMM_SOUND = SoundEvents.VILLAGER_NO;

	/** The upstream 1-in-3 HMM ratio (GT_Tool_MagnifyingGlass.java:68, verbatim). */
	public static final int HMM_ONE_IN = 3;

	/** The stack-classification action — "gt6_magnifyingglass", the self-owned wave form. */
	public static final ToolAction ACTION = ToolAction.get("gt6_magnifyingglass");

	/** The upstream {@code CS.TOOL_magnifyingglass} dispatch id ("magnifyingglass", CS.java:1070). */
	public static final String ID = "magnifyingglass";

	public GTMagnifyingGlassItem(Properties aProperties) {
		super(aProperties);
	}

	/** The AHA/HMM arm — the :67-79 ratio verbatim over the injected random (the testable seam). */
	public static SoundEvent inspectSound(RandomSource aRandom) {
		return aRandom.nextInt(HMM_ONE_IN) == 0 ? HMM_SOUND : AHA_SOUND;
	}

	/** The dispatch GATE — the has/get pairing iron law (the family shape). */
	@Override
	public boolean hasCraftingRemainingItem(ItemStack aStack) {
		return true;
	}

	/** The container-item channel — the shared one-point mapping (the upstream 400-unit row folded). */
	@Override
	public ItemStack getCraftingRemainingItem(ItemStack aStack) {
		return GT6FileItem.craftRemaining(aStack, GT6FileItem.DAMAGE_PER_CRAFT);
	}

	/** The stack-classification face (the family static-seam shape). */
	public static boolean classifies(ToolAction aToolAction) {
		return ACTION == aToolAction;
	}

	@Override
	public boolean canPerformAction(ItemStack aStack, ToolAction aToolAction) {
		return classifies(aToolAction);
	}

	/**
	 * The pure check face: the sound plays server-authoritative (the dedicated-server
	 * acceptance channel hears it through the broadcast) and the result is PASS — zero
	 * block/entity/NBT/durability change on BOTH sides, the acceptance's zero-change face.
	 */
	@Override
	public InteractionResult useOn(UseOnContext aContext) {
		Level tLevel = aContext.getLevel();
		if (tLevel instanceof ServerLevel tServer) {
			BlockPos tPos = aContext.getClickedPos();
			tServer.playSound((net.minecraft.world.entity.player.Player) null, tPos, inspectSound(tServer.random), SoundSource.PLAYERS, 1.0F, 1.0F);
		}
		return InteractionResult.PASS;
	}
}
