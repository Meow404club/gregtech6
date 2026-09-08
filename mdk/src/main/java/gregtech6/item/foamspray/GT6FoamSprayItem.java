package gregtech6.item.foamspray;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import gregtech6.item.spraycan.GTSprayCanItem;
import gregtech6.tileentity.connectors.GTFluidPipeBlockEntity;

/**
 * The GT6 C-Foam spray (16 colours) and the Advanced player-owned variant (16 colours) —
 * task p25-c-foam-pipe-spray spec ①/⑨. The 1.20.1 counterpart of upstream
 * {@code Behavior_Spray_Foam} (gregtech/items/behaviors/Behavior_Spray_Foam.java:46-215)
 * flattened onto one Item subclass per colour+owned pair (the p22 {@link GTSprayCanItem}
 * single-item-per-can precedent), reusing its {@code gt.remaining} ledger verbatim (the
 * static {@code remainingOf/remainingAfterHit/payUses} faces — one payment implementation
 * across the whole spray domain).
 *
 * <p>Upstream registration rows: the C-Foam Spray cans are ids {@code 1100+2i}
 * "C-Foam Spray (" + DYE_NAMES[i] + ")" with 256 uses and owned={@code F}; the Advanced
 * C-Foam Sprays are ids {@code 1132+2i} "Advanced C-Foam Spray (...)" — "Full (C-Foam only
 * breakable by Owner once dry)" — with owned={@code T} (MultiItemRandomTools.java:251-264).
 * The internal counter is {@code mUses = uses*10} (:56) spent 10 per applied pipe, so a
 * "256 uses" can is 2560 internal units; a depleted can swaps to the shared
 * {@code gt6:spray_can_empty} (:92-101 — registered by GT6SprayCans, one empty can for the
 * whole spray domain).
 *
 * <p>Routing — spec ⑨, ONLY the upstream {@code foam()} arm (1) (Behavior_Spray_Foam
 * .java:113-114) is live: the clicked block's TileEntity, {@code instanceof} foamable
 * (the port's one foamable is {@link GTFluidPipeBlockEntity}) and NOT already foamed →
 * {@code applyFoam(side, player, DYES[mColor], mColor, mOwned)}, paying 10 internal units
 * on success (:114). Arms (2) {@code IBlockFoamable} (:116), (3)/(4) the IC2 cable
 * reflection + scaffolds (:119-130) and (5) the air-placement modes 0-4 (:132-176, the
 * C-Foam block family) are POOL cuts — no branch exists here to route to.
 *
 * <p>Declared deviations (the card boundary, all upstream-evidenced):
 * <ul>
 * <li><b>{@code onItemUseFirst} → {@code useOn}</b> — the semantic-alignment deviation B:
 *     1.7.10 item-first routing became block-activation-first on 1.20.1; the pipe's own
 *     {@code use} PASSES on a non-hoe item (GTFluidPipeBlock), so the foam spray reaches
 *     its target (the p22 GTSprayCanItem useOn :216-221 precedent).</li>
 * <li><b>the invisible "used" intermediate can is CUT</b> (upstream :78-82 full→used swap)
 *     — the p22 declared form: one item per colour whose tag counts down.</li>
 * <li><b>the sneak mode-cycle no-op stays, the mode system is POOLED</b>: upstream
 *     {@code isSneaking → return F} (:70) plus the {@code onItemRightClick} mode cycle
 *     (:62-66) only feed the air-placement modes; with arm (5) pooled the sneak use is the
 *     upstream FAIL (PASS here) and {@code getMode/setMode} have no consumer.</li>
 * <li><b>SFX.IC_SPRAY</b> placeholder — the {@link SoundEvents#FIRE_EXTINGUISH} hiss (the
 *     p22 recorded placeholder; upstream :86 resolves to the IC2 painter sound).</li>
 * </ul>
 *
 * <p>The {@code aPlayer.canPlayerEdit} gate of upstream :70 is the vanilla placement
 * permission; a useOn hit on an existing TE needs no extra check on this port (the p22
 * GTSprayCanItem form — no canPlayerEdit analogue exists on the 1.20.1 item route).
 *
 * <p>Offline-test surface: the static {@link #foamTarget} routing gate + the BE's own
 * applyFoam truth table + the reused {@code GTSprayCanItem} static ledger — the mod-Item
 * wall (the CrowbarTest.bootStrap NOTE) bars constructing this item in the bootstrapped
 * test JVM; the live {@link #useOn} half rides the registration smoke + the RCON chain.
 */
public class GT6FoamSprayItem extends Item {

	/** The foam can capacity (MultiItemRandomTools.java:253/:261 {@code 256}); internal units x10. */
	public static final int FOAM_USES = 256;

	/** The foam tooltip template (the upstream Behavior_Spray_Foam ctor LH.add :59 wording). */
	public static final String FOAM_TOOLTIP_KEY = "gt6.foamspray.paint";

	/** The Advanced (owned) tooltip (the upstream :259/:261 "C-Foam only breakable by Owner once dry"). */
	public static final String OWNED_TOOLTIP_KEY = "gt6.foamspray.owned";

	/** This can's dye index ({@code 0..15}), the GT6 DYE order shared with {@link GTSprayCanItem}. */
	public final byte dyeIndex;

	/** True for the Advanced C-Foam Sprays — the applied foam becomes player-owned once dry. */
	public final boolean owned;

	/** The internal-units capacity ({@code 256*10}, the upstream ctor :56 multiplication). */
	public final long maxUses;

	/** The empty-can swap target (the shared {@code gt6:spray_can_empty}); resolved lazily. */
	private final java.util.function.Supplier<Item> emptyCan;

	/**
	 * @param aDyeIndex the GT6 dye index 0=Black..15=White ({@link GTSprayCanItem#DYES_INT} order)
	 * @param aOwned    the Advanced variant ({@code mOwned} of upstream Behavior_Spray_Foam :50)
	 */
	public GT6FoamSprayItem(java.util.function.Supplier<Item> aEmptyCan, byte aDyeIndex, boolean aOwned, Item.Properties aProperties) {
		super(aProperties);
		this.emptyCan = aEmptyCan;
		this.dyeIndex = aDyeIndex;
		this.owned = aOwned;
		this.maxUses = (long) FOAM_USES * GTSprayCanItem.HIT_COST; // upstream ctor :56
	}

	// ---------------------------------------------------------------------------
	// the useOn route (upstream onItemUseFirst :69-107 + foam() arm (1) :113-114,
	// the deviation-B semantic alignment; the p22 GTSprayCanItem useOn shape)
	// ---------------------------------------------------------------------------

	@Override
	public InteractionResult useOn(UseOnContext aContext) {
		// upstream :70 — sneaking is the mode-cycle gesture; with the air-placement modes
		// pooled the sneak use is the upstream FAIL (a no-op that pays nothing)
		Player tPlayer = aContext.getPlayer();
		if (tPlayer != null && tPlayer.isShiftKeyDown()) return InteractionResult.PASS;
		BlockEntity tBE = aContext.getLevel().getBlockEntity(aContext.getClickedPos());
		byte tSide = (byte)aContext.getClickedFace().get3DDataValue();
		if (!foamTarget(tBE, tSide)) return InteractionResult.PASS; // :113-114 !hasFoam gate
		if (aContext.getLevel().isClientSide) return InteractionResult.SUCCESS; // claim, the server side executes
		return spray((GTFluidPipeBlockEntity)tBE, tSide, aContext.getItemInHand(), tPlayer, aContext.getHand())
				? InteractionResult.CONSUME : InteractionResult.PASS;
	}

	/**
	 * The routing gate of upstream {@code foam()} arm (1) (:113-114): a foamable pipe that
	 * does NOT carry foam yet. Static and Entity-free — the offline truth-table seam. Every
	 * non-pipe (and every already-foamed pipe) is a PASS that costs nothing.
	 */
	public static boolean foamTarget(@Nullable BlockEntity aBE, byte aSide) {
		return aBE instanceof GTFluidPipeBlockEntity tPipe && !tPipe.hasFoam(aSide);
	}

	/**
	 * The server hit — applyFoam, then the payment tail of the upstream :84-104 (sound,
	 * 10 internal units unless creative, the depleted can swaps to the empty can).
	 *
	 * @return true when the foam landed (a hit)
	 */
	public boolean spray(GTFluidPipeBlockEntity aPipe, byte aSide, ItemStack aStack, @Nullable Player aPlayer, InteractionHand aHand) {
		if (!aPipe.applyFoam(aSide, aPlayer != null ? aPlayer.getUUID() : null,
				GTSprayCanItem.DYES_INT[dyeIndex], owned)) return false; // upstream :114 applyFoam(…, DYES[mColor], mColor, mOwned)
		aPipe.getLevel().playSound(null, aPipe.getBlockPos(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1.0F, 1.0F); // the :86 SFX placeholder
		if (aPlayer == null || !aPlayer.getAbilities().instabuild) { // upstream :87 hasInfiniteItems
			ItemStack tSwap = GTSprayCanItem.payUses(aStack, maxUses, emptyCan.get()); // 10 units, the :103 gt.remaining write + the :92-101 swap
			if (tSwap != null) {
				if (aPlayer != null) aPlayer.setItemInHand(aHand, tSwap);
				else aStack.setCount(0); // no hand to swap (dispenser-style callers get a shrink)
			}
		}
		return true;
	}

	// ---------------------------------------------------------------------------
	// the durability bar + tooltip (the p22 GTSprayCanItem face over the shared ledger)
	// ---------------------------------------------------------------------------

	/**
	 * The stack read of the shared {@code gt.remaining} ledger. GTSprayCanItem's own
	 * ItemStack overload is instance-bound (READ-ONLY file for this card), so the carrier
	 * read is forked here over the same key — 1.20.1 freeform NBT tag, 21.1 the same key
	 * inside the opaque {@code minecraft:custom_data} envelope (the GTSprayCanItem doc
	 * form). The WRITE path stays shared ({@code GTSprayCanItem#payUses} is public static).
	 */
	private static long remainingOf(ItemStack aStack, long aMaxUses) {
		//? if forge {
		return GTSprayCanItem.remainingOf(aStack.getTag(), aMaxUses);
		//?} else {
		/*return GTSprayCanItem.remainingOf(aStack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
				net.minecraft.world.item.component.CustomData.EMPTY).copyTag(), aMaxUses);
		*///?}
	}

	@Override
	public boolean isBarVisible(ItemStack aStack) {
		return GTSprayCanItem.barVisible(remainingOf(aStack, maxUses), maxUses);
	}

	@Override
	public int getBarWidth(ItemStack aStack) {
		return GTSprayCanItem.barWidth(remainingOf(aStack, maxUses), maxUses);
	}

	@Override
	public int getBarColor(ItemStack aStack) {
		return GTSprayCanItem.DYES_INT[dyeIndex];
	}

	//? if forge {
	@Override
	public void appendHoverText(ItemStack aStack, @Nullable Level aLevel, List<Component> aTooltip, TooltipFlag aFlag) {
		tooltipLines(aStack, aTooltip);
	}
	//?}

	//? if neoforge {
	/*@Override
	public void appendHoverText(ItemStack aStack, Item.TooltipContext aContext, List<Component> aTooltip, TooltipFlag aFlag) {
	//21.1: the hover signature carries the Item.TooltipContext (the GTSprayCanItem fork shape)
		tooltipLines(aStack, aTooltip);
	}
	*///?}

	/** The shared tooltip body: what the can places (+ the owned warning) + the remaining uses. */
	private void tooltipLines(ItemStack aStack, List<Component> aTooltip) {
		aTooltip.add(Component.translatable(FOAM_TOOLTIP_KEY, GTSprayCanItem.DYE_NAMES[dyeIndex]).withStyle(ChatFormatting.BLUE));
		if (owned) aTooltip.add(Component.translatable(OWNED_TOOLTIP_KEY).withStyle(ChatFormatting.ORANGE)); // upstream :259 wording
		long tRemaining = remainingOf(aStack, maxUses);
		aTooltip.add(Component.translatable(GTSprayCanItem.REMAINING_TOOLTIP_KEY, tRemaining / GTSprayCanItem.HIT_COST, tRemaining % GTSprayCanItem.HIT_COST)
				.withStyle(ChatFormatting.GRAY));
	}
}
