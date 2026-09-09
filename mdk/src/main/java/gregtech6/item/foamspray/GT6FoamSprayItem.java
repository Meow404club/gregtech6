package gregtech6.item.foamspray;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import gregtech6.block.foam.IBlockFoamable;
import gregtech6.block.foam.GT6CFoamFreshBlock;
import gregtech6.item.spraycan.GTSprayCanItem;
import gregtech6.registry.GT6FoamBlocks;
import gregtech6.tileentity.connectors.GTFluidPipeBlockEntity;
import gregtech6.tileentity.foam.GT6CFoamBlockEntity;
import gregtech6.tileentity.foam.ITileEntityFoamable;

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
 * <p>Routing — spec ⑨, the upstream {@code foam()} arms in order (Behavior_Spray_Foam
 * .java:109-178): (1) the clicked TileEntity, {@code instanceof} foamable and NOT already
 * foamed → {@code applyFoam(...)}, paying 10 internal units on success (:113-114 — the
 * pipe half since p25, the owned-CFoam-BE half since this card, whose {@code applyFoam}
 * is constant false, upstream MultiTileEntityCFoam.java:149); (2) the {@code IBlockFoamable}
 * block arm (:116 — the port's foamable blocks answer applyFoam false constantly, the
 * research "零行为差" finding); (3)/(4) the IC2 cable reflection + scaffolds (:119-130)
 * stay POOL cuts (no IC2 in the port); (5) the air-placement modes 0-4 (:132-176) are
 * LIVE since this card — the geometry walks in {@link GT6FoamPlacement} (the pure seam),
 * the placement sink here (the owned/plain choice of :138, the slab forms of :160/:170).
 *
 * <p>Declared deviations (the card boundary, all upstream-evidenced):
 * <ul>
 * <li><b>{@code onItemUseFirst} → {@code useOn}</b> — the semantic-alignment deviation B:
 *     1.7.10 item-first routing became block-activation-first on 1.20.1; the pipe's own
 *     {@code use} PASSES on a non-hoe item (GTFluidPipeBlock), so the foam spray reaches
 *     its target (the p22 GTSprayCanItem useOn :216-221 precedent).</li>
 * <li><b>the invisible "used" intermediate can is CUT</b> (upstream :78-82 full→used swap)
 *     — the p22 declared form: one item per colour whose tag counts down.</li>
 * <li><b>the mode cycle rides {@code use()}</b> (the air-right-click pass): the upstream
 *     sneak gesture (:62-66 + :190-199, the NBT_MODE = "gt.mode" key, the owned 0-2 cycle
 *     of :191); the useOn sneak use stays the upstream FAIL.</li>
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

	/** The air-placement block cost (upstream :137/:142/:154 — the shared 10-unit hit). */
	public static final long MODE_BLOCK_COST = GTSprayCanItem.HIT_COST;

	/** The air-placement slab cost (upstream :160/:168 — 5 internal units). */
	public static final long MODE_SLAB_COST = GT6FoamPlacement.SLAB_COST;

	/** The mode NBT key — the upstream NBT_MODE = "gt.mode" (CS constant; the GTWireBlockEntity.java:181 in-repo literal form). */
	public static final String NBT_MODE = "gt.mode";

	/** The mode chat names (upstream :193-197 verbatim wording). */
	public static final String[] MODE_NAMES = {
			"Single Block Mode", "4m Line Mode", "3mx3m Area Mode", "Single Slab Mode", "3mx3m Slab Mode"};

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
	// the useOn route (upstream onItemUseFirst :69-107 + the foam() arms in order —
	// the deviation-B semantic alignment; the p22 GTSprayCanItem useOn shape)
	// ---------------------------------------------------------------------------

	@Override
	public InteractionResult useOn(UseOnContext aContext) {
		// upstream :70 — sneaking is the mode-cycle gesture (an air-right-click); the
		// useOn sneak use stays the upstream FAIL (a no-op that pays nothing)
		Player tPlayer = aContext.getPlayer();
		if (tPlayer != null && tPlayer.isShiftKeyDown()) return InteractionResult.PASS;
		Level tLevel = aContext.getLevel();
		BlockPos tPos = aContext.getClickedPos();
		Direction tFace = aContext.getClickedFace();
		byte tSide = (byte)tFace.get3DDataValue();
		ItemStack tStack = aContext.getItemInHand();
		UUID tPlayerId = tPlayer != null ? tPlayer.getUUID() : null;

		// arm (1) the pipe half (upstream :113-114, the p25 face): a foamable pipe that
		// does NOT carry foam yet
		BlockEntity tBE = tLevel.getBlockEntity(tPos);
		if (foamTarget(tBE, tSide)) {
			if (tLevel.isClientSide) return InteractionResult.SUCCESS; // claim, the server side executes
			return spray((GTFluidPipeBlockEntity)tBE, tSide, tStack, tPlayer, aContext.getHand())
					? InteractionResult.CONSUME : InteractionResult.PASS;
		}

		// arm (1) the foamable-TE half (upstream :113-114, second family — the owned CFoam
		// BE): applyFoam is CONSTANT false there (upstream MultiTileEntityCFoam.java:149),
		// so this arm is shape-only today and the Remover's routing face tomorrow (card C)
		if (tBE instanceof ITileEntityFoamable tFoam && !tFoam.hasFoam(tSide)) {
			if (tLevel.isClientSide) return InteractionResult.SUCCESS;
			if (!tFoam.applyFoam(tSide, tPlayerId, GTSprayCanItem.DYES_INT[dyeIndex], owned)) return InteractionResult.PASS;
			payHit(tStack, MODE_BLOCK_COST, tPlayer, aContext.getHand());
			return InteractionResult.CONSUME;
		}

		// arm (2) the IBlockFoamable block arm (upstream :116): the foamable blocks answer
		// applyFoam false constantly (the research "零行为差" finding) — shape-only
		BlockState tState = tLevel.getBlockState(tPos);
		if (tState.getBlock() instanceof IBlockFoamable tFoamBlock && !tFoamBlock.hasFoam(tLevel, tPos, tFace)) {
			if (tLevel.isClientSide) return InteractionResult.SUCCESS;
			if (!tFoamBlock.applyFoam(tLevel, tPos, tFace, GTSprayCanItem.DYES_INT[dyeIndex], dyeIndex)) return InteractionResult.PASS;
			payHit(tStack, MODE_BLOCK_COST, tPlayer, aContext.getHand());
			return InteractionResult.CONSUME;
		}

		// arm (5) the air-placement modes 0-4 (upstream :132-176) — THIS card's live face.
		// The client claims when the origin cell is air (the cheapest gate it shares with
		// the server walk); the server side runs the real arm.
		BlockPos tOrigin = tPos.relative(tFace);
		if (tLevel.isClientSide) return tLevel.getBlockState(tOrigin).isAir() ? InteractionResult.SUCCESS : InteractionResult.PASS;
		int tMode = (int)modeOf(tStack);
		long tRemaining = tPlayer != null && tPlayer.getAbilities().instabuild
				? maxUses : remainingOf(tStack, maxUses); // upstream :84 hasInfiniteItems
		long tSpent = GT6FoamPlacement.foamArm(tMode, tPos, tFace, playerSideOf(tPlayer), hitYOf(aContext),
				tRemaining, liveSink(tLevel, tPlayerId));
		if (tSpent <= 0) return InteractionResult.PASS;
		tLevel.playSound(null, tPos, SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1.0F, 1.0F); // the :86 SFX placeholder
		payHit(tStack, tSpent, tPlayer, aContext.getHand());
		return InteractionResult.CONSUME;
	}

	/**
	 * The live placement sink of the mode arm — the owned/plain choice of upstream :138
	 * (the owned can lands the {@code cfoam_owned} TE carrier + the :81-83 NBT bundle;
	 * the plain can lands the fresh block with the colour property). Modes 3/4 always
	 * place the plain fresh slab (upstream :160/:170 have NO owned branch). Static so the
	 * RCON command drives the SAME face (the p25 command precedent).
	 */
	public static GT6FoamPlacement.Sink liveSink(Level aLevel, int aDyeIndex, boolean aOwned, @Nullable UUID aPlayerId) {
		return aPlacement -> {
			BlockState tFresh = GT6FoamBlocks.CFOAM_FRESH.get().defaultBlockState();
			if (aPlacement.slab()) {
				// foamArm only issues slab placements with a half; the null fallback stays the
				// bottom form (a full block here would corrupt the arm's cost semantics)
				BlockState tSlab = GT6FoamBlocks.CFOAM_FRESH_SLAB.get().defaultBlockState()
						.setValue(net.minecraft.world.level.block.SlabBlock.TYPE,
								aPlacement.slabType() == null ? SlabType.BOTTOM : aPlacement.slabType());
				BlockPos tPos = aPlacement.pos();
				return aLevel.getBlockState(tPos).isAir()
						&& aLevel.setBlock(tPos, tSlab.setValue(GT6CFoamFreshBlock.COLOR, aDyeIndex & 15), 3);
			}
			BlockPos tPos = aPlacement.pos();
			if (!aLevel.getBlockState(tPos).isAir()) return false;
			if (aOwned) {
				if (!aLevel.setBlock(tPos, GT6FoamBlocks.CFOAM_OWNED.get().defaultBlockState(), 3)) return false;
				if (aLevel.getBlockEntity(tPos) instanceof GT6CFoamBlockEntity tFoam) {
					tFoam.configureSpray(GTSprayCanItem.DYES_INT[aDyeIndex & 15], aOwned, aPlayerId); // the upstream :82 bundle
				}
				return true;
			}
			return aLevel.setBlock(tPos, tFresh.setValue(GT6CFoamFreshBlock.COLOR, aDyeIndex & 15), 3); // upstream :138 else-arm
		};
	}

	/** The instance sink binding (this can's colour/ownership). */
	private GT6FoamPlacement.Sink liveSink(Level aLevel, @Nullable UUID aPlayerId) {
		return liveSink(aLevel, dyeIndex, owned, aPlayerId);
	}

	/** The upstream :135 {@code getSideForPlayerPlacing} — the pitch thresholds then the horizontal facing. */
	private static Direction playerSideOf(@Nullable Player aPlayer) {
		if (aPlayer == null) return Direction.NORTH; // the console spray carries a facing argument at the command
		if (aPlayer.getXRot() >= 65.0F) return Direction.UP; // upstream :174-175
		if (aPlayer.getXRot() <= -65.0F) return Direction.DOWN; // upstream :175-176
		return aPlayer.getDirection(); // the horizontal compass — Direction.fromYRot is the :1752 rounding form
	}

	/** The click's hit height (the horizontal-face slab-half fallback of the ruling_slab mapping). */
	private static float hitYOf(UseOnContext aContext) {
		var tLoc = aContext.getClickLocation();
		return (float)(tLoc.y - aContext.getClickedPos().getY());
	}

	/**
	 * The routing gate of upstream {@code foam()} arm (1) (:113-114), extended per the card
	 * SPEC over the port's TWO foamable families: the pipe (the p25 face, :113) and the
	 * owned CFoam BE (the {@code ITileEntityFoamable} family — applyFoam CONSTANT false
	 * there, so the extension is shape-only for the spray and the Remover's routing face
	 * for card C). Static and Entity-free — the offline truth-table seam. Every non-target
	 * (and every already-foamed target) is a PASS that costs nothing.
	 */
	public static boolean foamTarget(@Nullable BlockEntity aBE, byte aSide) {
		if (aBE instanceof GTFluidPipeBlockEntity tPipe && !tPipe.hasFoam(aSide)) return true; // arm (1) pipe half
		return aBE instanceof ITileEntityFoamable tFoam && !tFoam.hasFoam(aSide); // arm (1) foamable-TE half
	}

	/** The hit payment tail (the :103 gt.remaining write + the :92-101 depleted swap). */
	private void payHit(ItemStack aStack, long aCost, @Nullable Player aPlayer, InteractionHand aHand) {
		if (aPlayer != null && aPlayer.getAbilities().instabuild) return; // upstream :87 hasInfiniteItems
		ItemStack tSwap = GTSprayCanItem.payUses(aStack, maxUses, aCost, emptyCan.get());
		if (tSwap != null) {
			if (aPlayer != null) aPlayer.setItemInHand(aHand, tSwap);
			else aStack.setCount(0); // no hand to swap (dispenser-style callers get a shrink)
		}
	}

	/**
	 * The mode cycle (upstream {@code onItemRightClick} :62-66 + {@code switchMode}
	 * :190-199): a SNEAK right-click in the air advances the air-placement mode — the
	 * owned cans cycle 0-2 only ({@code mOwned ? 3 : 5} of :191, the slab modes are not
	 * reachable), the plain cans cycle 0-4. The write lands server-side (the vanilla
	 * client-summons-server use pass); the chat names are the :193-197 literals. The
	 * {@code InteractionResultHolder} carrier is the 1.20.1/1.21.1 Item.use contract
	 * (the plain InteractionResult form is 1.21.2+ — both legs holder-carried here).
	 */
	@Override
	public InteractionResultHolder<ItemStack> use(Level aLevel, Player aPlayer, InteractionHand aHand) {
		ItemStack tStack = aPlayer.getItemInHand(aHand);
		if (!aPlayer.isShiftKeyDown()) return InteractionResultHolder.pass(tStack); // upstream :64 the sneak gesture
		if (aLevel.isClientSide) return InteractionResultHolder.success(tStack); // claim, the server side executes
		setMode(tStack, (modeOf(tStack) + 1) % (owned ? 3 : 5)); // upstream :191
		aPlayer.displayClientMessage(Component.literal(MODE_NAMES[(int)modeOf(tStack)]), false); // upstream :193-197
		return InteractionResultHolder.consume(tStack);
	}

	/** The mode read over the per-leg carrier (the {@link #remainingOf} fork shape). */
	public long modeOf(ItemStack aStack) {
		//? if forge {
		CompoundTag tTag = aStack.getTag();
		return tTag != null && tTag.contains(NBT_MODE, Tag.TAG_ANY_NUMERIC) ? tTag.getLong(NBT_MODE) : 0;
		//?} else {
		/*CompoundTag tTag = aStack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
				net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
		return tTag.contains(NBT_MODE, Tag.TAG_ANY_NUMERIC) ? tTag.getLong(NBT_MODE) : 0;
		*///?}
	}

	/** The mode write over the per-leg carrier (the 21.1 envelope set, the payUses fork shape). */
	public void setMode(ItemStack aStack, long aMode) {
		//? if forge {
		aStack.getOrCreateTag().putLong(NBT_MODE, aMode);
		//?} else {
		/*CompoundTag tTag = aStack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
				net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
		tTag.putLong(NBT_MODE, aMode);
		aStack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
				net.minecraft.world.item.component.CustomData.of(tTag));
		*///?}
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
		if (owned) aTooltip.add(Component.translatable(OWNED_TOOLTIP_KEY).withStyle(ChatFormatting.GOLD)); // upstream :259 wording
		long tRemaining = remainingOf(aStack, maxUses);
		aTooltip.add(Component.translatable(GTSprayCanItem.REMAINING_TOOLTIP_KEY, tRemaining / GTSprayCanItem.HIT_COST, tRemaining % GTSprayCanItem.HIT_COST)
				.withStyle(ChatFormatting.GRAY));
	}
}
