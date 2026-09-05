package gregtech6.items.tools;

import javax.annotation.Nullable;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.common.ToolAction;

import gregtech6.tileentity.energy.GTSteamEngineBlockEntity;
import gregtech6.tileentity.energy.converters.GTBoilerTankBlockEntity;

/**
 * The formal GT6 chisel — task p16-chisel-decalcify spec ①/②. Upstream the tool mounts
 * {@code Behavior_Tool(TOOL_chisel, SFX.MC_DIG_ROCK, 25, !canBlock(), SFX.RANDOM_PITCH)}
 * (GT_Tool_Chisel.java:98) and the boiler tank answers it on the {@code onToolClick2}
 * chain (MultiTileEntityBoilerTank.java:165-179 — the {@code TOOL_chisel} arm). The
 * 1.20.1 port flattens the pre-use hook to a {@link #useOn} direct dispatch with the
 * <b>decalcify arm</b>: the target BE is a {@link GTBoilerTankBlockEntity} and the
 * already-ported server face {@code chisel(aPlayer)} runs verbatim — ≤15/31 barometer the
 * repair branch (vent + efficiency/heat reset, the :171 heat damage landing on the live
 * player) and above it the detonation branch (the deferred explode(F), :168-169). The
 * server semantics are the pinned GTBoilerTankBlockEntityTest ones (theChiselDetonates*
 * tests, :386/:415) — this item adds NO boiler logic, it only aims the tool.
 *
 * <p>The durability payment is the upstream {@code Behavior_Tool} conversion verbatim
 * (Behavior_Tool.java:63 {@code doDamage(units(tDamage, 10000, mDamage, T))}): the chisel
 * behaviour carries {@code mDamage = 25} (GT_Tool_Chisel.java:98 third argument), so every
 * full 10000-unit repair value costs 25 vanilla points, ROUNDING UP — any non-zero repair
 * pays at least one point (the upstream {@code T} round-up; the cutter's 1-point-per-10000
 * mapping is a DIFFERENT behaviour row and must not be copied here).
 *
 * <p>Declared port-isms and cuts (card spec, the GTCutterItem p10/p11 form):
 * <ul>
 * <li>the detonation branch returns 0 (upstream :178) — no durability payment and a PASS
 *     interaction result; the explosion is the feedback (Behavior_Tool.java:62 pays only
 *     when {@code tDamage > 0}).</li>
 * <li>the client leg is the same-side claim pattern the cutter uses: claim SUCCESS on the
 *     client, the server decides CONSUME/PASS.</li>
 * <li>the mining face (GT_Tool_Chisel.java:80-87 — the stone/silverfish harvest heuristic)
 *     is CUT: this repo has no harvest layer (the cutter cut precedent, pooled with the
 *     tool-family card) and the stone-variant chiseling ({@code stoneChiseled}) is the
 *     card's pool item.</li>
 * <li>the other {@code TOOL_chisel} consumers (Basin/Mold/RailRoad/ButtonAdvanced/
 *     CoverTextureMulti/... family) are NOT ported — card cut "other TOOL_* family".</li>
 * <li>the attack face, the material ladder and the runtime tint are the crowbar/cutter
 *     declared deviations: single steel tier 512, the grayscale HANDLE_CHISEL borrow
 *     rendered un-tinted (assets/README.md — the runtime-tint pool; spec ③ resolves to
 *     no-tint, no client listener row).</li>
 * <li>the crafting recipe (Loader_Tools.java:142 {@code toolHeadChisel} material amount)
 *     stays pooled with the crowbar/cutter recipes.</li>
 * </ul>
 */
public class GTChiselItem extends Item {

	/** The vanilla durability points — single steel tier (the crowbar/cutter pinned family value). */
	public static final int DURABILITY_POINTS = 512;

	/**
	 * The upstream behaviour damage scale (GT_Tool_Chisel.java:98 {@code Behavior_Tool}
	 * third argument = 25): the :63 {@code units(tDamage, 10000, mDamage, T)} conversion
	 * target — 25 vanilla points per full 10000-unit repair value.
	 */
	public static final long UPSTREAM_DAMAGE_PER_REPAIR = 25;

	/** The upstream tool-damage unit scale (Behavior_Tool.java:63, the 10000 basis). */
	public static final long TOOL_DAMAGE_UNIT = 10000;

	public GTChiselItem(Properties aProperties) {
		super(aProperties);
	}

	/**
	 * The flattened onToolClick2 — direct dispatch into the decalcify arm. PASS on
	 * everything that is not a boiler tank; claims on the client only when it is (the
	 * server side executes and decides CONSUME/PASS).
	 */
	@Override
	public InteractionResult useOn(UseOnContext aContext) {
		BlockEntity tBE = aContext.getLevel().getBlockEntity(aContext.getClickedPos());
		if (!(tBE instanceof GTBoilerTankBlockEntity)) {
			return InteractionResult.PASS;
		}
		if (aContext.getLevel().isClientSide) {
			return InteractionResult.SUCCESS; // claim, the server side executes
		}
		return chiselToolClick(aContext) > 0 ? InteractionResult.CONSUME : InteractionResult.PASS;
	}

	/**
	 * The single dispatch + payment surface over a context, shared by {@link #useOn} (the
	 * cutter {@code cutterToolClick} shape): the boiler's own {@code chisel(aPlayer)} runs
	 * (the player rides along so the live :171 heat-damage half fires on a real click),
	 * then the :62/:63 payment converts the return ONCE at the item layer — and only on a
	 * non-zero return (the Behavior_Tool.java:62 {@code if (tDamage > 0)} gate; a
	 * detonating or pristine boiler pays nothing).
	 *
	 * @return the upstream tool damage (10000-scale repair value, 0 = nothing to do / the
	 *         detonation branch) — 0 pays nothing.
	 */
	public static long chiselToolClick(UseOnContext aContext) {
		Level tLevel = aContext.getLevel();
		BlockPos tPos = aContext.getClickedPos();
		BlockEntity tBE = tLevel.getBlockEntity(tPos);
		if (!(tBE instanceof GTBoilerTankBlockEntity tBoiler)) {
			return 0;
		}
		long tDamage = chiselToolClick(tBoiler, aContext.getPlayer());
		if (tDamage > 0) { // Behavior_Tool.java:62 — the payment only arms on a non-zero return
			payPerPoint(aContext.getItemInHand(), aContext.getPlayer(), tDamage);
		}
		return tDamage;
	}

	/**
	 * The boiler arm — the upstream MultiTileEntityBoilerTank.java:165-179 face through the
	 * ported {@link GTBoilerTankBlockEntity#chisel} (the RCON command arm calls the same
	 * method, single-source semantics). The repair value is the raw :175 return; the
	 * detonation branch returns 0 (:178).
	 */
	public static long chiselToolClick(GTBoilerTankBlockEntity aBoiler, @Nullable Player aPlayer) {
		return aBoiler.chisel(aPlayer);
	}

	/**
	 * The durability mapping — the Behavior_Tool.java:63 conversion
	 * {@code units(tDamage, 10000, 25, T)} (round-up): 1 damage unit already costs one
	 * point, 1000 → 3, 5000 → 13, 10000 → 25. Static pure function so the offline tests
	 * pin the table (the mod-Item wall, CutterTest NOTE).
	 */
	public static long durabilityPoints(long aToolDamage) {
		return GTSteamEngineBlockEntity.units(aToolDamage, TOOL_DAMAGE_UNIT, UPSTREAM_DAMAGE_PER_REPAIR, true);
	}

	/**
	 * The payPerPoint invocation counter — the public counting-stub seam for the offline
	 * tests (this card's test lives cross-package in tileentity/energy/converters, the
	 * GTCutterItem.sPayPerPointCalls shape widened one visibility notch): the context
	 * overload must call payPerPoint exactly ONCE per click, and zero times when the
	 * boiler returns 0 (the detonation / nothing-to-descale branches).
	 */
	public static int sPayPerPointCalls;

	/**
	 * The payment (Behavior_Tool.java:63 form): the converted points land through the
	 * vanilla {@code hurtAndBreak} (the cutter/crowbar payment shape); a null player (the
	 * RCON/acceptance channel) pays nothing — the same ruling as the cutter item.
	 */
	private static void payPerPoint(ItemStack aStack, @Nullable Player aPlayer, long aToolDamage) {
		sPayPerPointCalls++;
		long tPoints = durabilityPoints(aToolDamage);
		if (tPoints > 0 && aPlayer != null) {
			aStack.hurtAndBreak((int) tPoints, aPlayer, p -> p.broadcastBreakEvent(EquipmentSlot.MAINHAND));
		}
	}

	/**
	 * The stack classifier — the ONLY action this item performs is
	 * {@link GT6ToolActions#CHISEL}; never {@code ToolActions.HOE_DIG} (the three
	 * wrench-substitute predicates must not see the chisel), never the crowbar or cutter
	 * action. Static seam for the offline tests (the mod-Item wall).
	 */
	public static boolean classifies(ToolAction aToolAction) {
		return GT6ToolActions.CHISEL == aToolAction;
	}

	@Override
	public boolean canPerformAction(ItemStack aStack, ToolAction aToolAction) {
		return classifies(aToolAction);
	}
}
