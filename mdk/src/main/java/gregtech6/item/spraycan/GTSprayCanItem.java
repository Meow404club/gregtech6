package gregtech6.item.spraycan;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

import gregtech6.tileentity.IPaintableTE;

/**
 * The GT6 spray paint can (16 dyes) and the paint removal spray — task p22-spraycan-items.
 * The 1.20.1 counterpart of upstream {@code Behavior_Spray_Color}
 * (gregtech/items/behaviors/Behavior_Spray_Color.java:45-183) and
 * {@code Behavior_Spray_Color_Remover} (:46-123) flattened onto one Item subclass per
 * colour (GTCEu {@code ColorSprayBehaviour} single-item precedent, ColorSprayBehaviour.java
 * :58-200 + GTItems.java:2205-2213 {@code stacksTo(1)}).
 *
 * <p>Form (the card spec): one item per dye + one remover + one empty swap target;
 * the remaining uses ride the {@link #NBT_REMAINING} stack tag (upstream :68/:83 key
 * {@code gt.remaining} verbatim, a LONG in the {@code UT.NBT.setNumber} shape), displayed
 * as the vanilla durability bar ({@link #isBarVisible}/{@link #getBarWidth}/{@link
 * #getBarColor}) instead of vanilla damage — the upstream internal counter is
 * {@code mUses = uses*10} (:54) spent {@link #HIT_COST} 10 per successful hit (:78/:77),
 * so a "512 uses" can is 5120 internal units (MultiItemRandomTools.java:245 {@code 512} /
 * :271 {@code 256} for the remover).
 *
 * <p>Declared deviations (the card boundary, all upstream-evidenced):
 * <ul>
 * <li><b>the invisible "used" intermediate can is CUT</b> — upstream swaps full→used on the
 *     first use (:70-74) and used→empty on depletion (:85-92); the port keeps ONE item per
 *     colour whose tag simply counts down (the GTCEu :2212 single-item precedent) — 17 fewer
 *     items and no HIDDEN meta ladders.</li>
 * <li><b>the vanilla whitelist is the upstream {@code mAllowedVanillaBlocks} + the
 *     {@code BlockColored} family (:144) minus two arms with no port target:</b> the
 *     grass_block arm converts into the GT6 grass family (BlocksGT.Grass :153-163 — the
 *     family is not ported) and the Thermal-Expansion rockwool arm (IL.TE_Rockwool :148 —
 *     no TE on this port). Both stay pooled; glass/pane/stained glass/stained pane/wool/
 *     carpet/terracotta/stained terracotta are the live table (upstream colour code
 *     {@code ~mColor&15} folded to {@link #vanillaDye} — P21 ADR ruling 3, the complement
 *     detour is provably identity). The recolourBlock hook (:166, a 1.7.10 Forge block
 *     extension) does not exist on 1.20.1 — only the whitelist table sprays.</li>
 * <li><b>the remover reverse face</b> covers the families with a vanilla uncoloured variant
 *     (stained terracotta/glass/pane → plain, upstream Remover :101-103 verbatim); wool and
 *     carpet have no uncoloured variant and upstream does not revert them either (:96-106
 *     has no wool arm).</li>
 * <li><b>SFX.IC_SPRAY</b> upstream resolves to the IC2 {@code tools.Painter} sound
 *     (CS.java:2249) — no IC2 audio on this port; {@link SoundEvents#FIRE_EXTINGUISH} is the
 *     declared placeholder hiss (the GTCEu custom {@code spray_can} sound entry is pooled).</li>
 * <li>the sheep/wolf entity leg (:97-142), the C-Foam family, the Canner refill and the
 *     empty-can crafting are card-pool cuts.</li>
 * </ul>
 *
 * <p>Routing (the upstream :60-94 order): server side only, an {@link IPaintableTE} target
 * wins (the 04:227-235 recolour routing: painted → {@link IPaintableTE#mixPaint}, unpainted
 * → {@link IPaintableTE#paint}; the remover → {@link IPaintableTE#unpaint}), else the
 * vanilla whitelist table. A miss (no route, or the same-colour no-op upstream :164) costs
 * nothing; a hit pays the sound + one {@link #HIT_COST} decrement (creative players are
 * free, upstream :78 {@code hasInfiniteItems}) and a depleted can swaps to the empty can
 * (:85-92).
 *
 * <p>Offline-test surface: the pure seams ({@link #DYES_INT}, {@link #remainingOf}, {@link
 * #remainingAfterHit}, {@link #payUses}, {@link #barVisible}, {@link #barWidth}, {@link
 * #paintPaintableTE}, {@link #colorTarget}, {@link #decolorTarget}) — the mod-Item wall
 * (CrowbarTest.bootStrap NOTE) bars constructing this item in the bootstrapped test JVM, so
 * the ItemStack seams take vanilla stand-ins and the live {@link #useOn} half rides the
 * registration smoke + the RCON chain.
 */
public class GTSprayCanItem extends Item {

	/** The upstream NBT key verbatim (Behavior_Spray_Color.java:68/:83). */
	public static final String NBT_REMAINING = "gt.remaining";

	/** The internal-units cost per successful hit (upstream :78 — a hit spends mUses/10 of a "use"). */
	public static final long HIT_COST = 10;

	/** The 16-colour can capacity (MultiItemRandomTools.java:245 {@code 512}); internal units ×10. */
	public static final int SPRAY_USES = 512;

	/** The remover capacity (MultiItemRandomTools.java:271 {@code 256}); internal units ×10. */
	public static final int REMOVER_USES = 256;

	/** The remover sentinel dye index (no colour). */
	public static final byte REMOVER = -1;

	/** The remover durability-bar colour (the GTCEu solvent gray, ColorSprayBehaviour.java:120). */
	public static final int REMOVER_RGB = 0x969696;

	/**
	 * The upstream CS.DYES_INT dye table (CS.java:470, index = the GT6 DYE_INDEX 0=Black..
	 * 15=White through UT.Code.getRGBInt). This is the colour a can sprays — P21 ADR ruling 3:
	 * the upstream route {@code ~mColor&15} + DYES_INT_INVERTED composes to exactly this value
	 * ("the colour you spray is the colour you get"). Mirrored by the private
	 * GTMachineCommand.DYES_INT (the two faces agree value-for-value, pinned by the offline
	 * census test).
	 */
	public static final int[] DYES_INT = {
			0x202020, // Black
			0xFF0000, // Red
			0x00FF00, // Green
			0x604000, // Brown
			0x0000FF, // Blue
			0x800080, // Purple
			0x00FFFF, // Cyan
			0xC0C0C0, // Light Gray
			0x808080, // Gray
			0xFFC0C0, // Pink
			0x80FF80, // Lime
			0xFFFF00, // Yellow
			0x8080FF, // Light Blue
			0xFF00FF, // Magenta
			0xFF8000, // Orange
			0xFFFFFF, // White
	};

	/** The upstream DYE_NAMES (CS.java:459), same index order as {@link #DYES_INT}. */
	public static final String[] DYE_NAMES = {"Black", "Red", "Green", "Brown", "Blue", "Purple", "Cyan",
			"Light Gray", "Gray", "Pink", "Lime", "Yellow", "Light Blue", "Magenta", "Orange", "White"};

	/** The id-path snake form of {@link #DYE_NAMES} — the registry/model/lang id per colour. */
	public static final String[] DYE_IDS = {"black", "red", "green", "brown", "blue", "purple", "cyan",
			"light_gray", "gray", "pink", "lime", "yellow", "light_blue", "magenta", "orange", "white"};

	/** The tooltip template (the upstream LH :56 wording, the colour name rides the slot). */
	public static final String PAINT_TOOLTIP_KEY = "gt6.spraycan.paint";

	/** The remover tooltip (the upstream Remover LH :109 wording verbatim). */
	public static final String DECOLOR_TOOLTIP_KEY = "gt6.spraycan.decolor";

	/** The remaining-uses tooltip template (the upstream LH :170 wording + the :179 "X.Y" format). */
	public static final String REMAINING_TOOLTIP_KEY = "gt6.spraycan.remaining";

	/** This can's dye index ({@code 0..15}), or {@link #REMOVER}. */
	public final byte dyeIndex;

	/** The internal-units capacity ({@code uses*10}, the upstream ctor :54 multiplication). */
	public final long maxUses;

	/** The empty-can swap target (upstream ctor {@code mEmpty}); resolved lazily from the registry. */
	private final java.util.function.Supplier<Item> emptyCan;

	/**
	 * @param aDyeIndex the GT6 dye index 0=Black..15=White, or {@link #REMOVER}
	 * @param aUses     the use count in "uses" (512/256); stored ×10 like the upstream ctor
	 */
	public GTSprayCanItem(java.util.function.Supplier<Item> aEmptyCan, int aUses, byte aDyeIndex, Item.Properties aProperties) {
		super(aProperties);
		this.emptyCan = aEmptyCan;
		this.maxUses = (long) aUses * HIT_COST; // the upstream ctor :54 mUses = aUses * 10
		this.dyeIndex = aDyeIndex;
	}

	/** True when this can is the paint removal spray (the {@link #REMOVER} sentinel). */
	public boolean remover() {
		return dyeIndex == REMOVER;
	}

	/** The sprayed colour as 0xRRGGBB ({@link #DYES_INT}[index]); the remover returns {@link #REMOVER_RGB}. */
	public int colorRGB() {
		return remover() ? REMOVER_RGB : DYES_INT[dyeIndex];
	}

	// ---------------------------------------------------------------------------
	// the useOn route (upstream onItemUseFirst :60-94, the cutter dispatch shape)
	// ---------------------------------------------------------------------------

	@Override
	public InteractionResult useOn(UseOnContext aContext) {
		Level tLevel = aContext.getLevel();
		BlockPos tPos = aContext.getClickedPos();
		boolean tApplicable = tLevel.getBlockEntity(tPos) instanceof IPaintableTE
				|| (remover() ? decolorTarget(tLevel.getBlockState(tPos).getBlock()) != null
						: colorTarget(tLevel.getBlockState(tPos).getBlock(), dyeIndex) != null);
		if (!tApplicable) return InteractionResult.PASS;
		if (tLevel.isClientSide) return InteractionResult.SUCCESS; // claim, the server side executes
		return spray(tLevel, tPos, aContext.getItemInHand(), aContext.getPlayer(), aContext.getHand())
				? InteractionResult.CONSUME : InteractionResult.PASS;
	}

	/**
	 * The server hit — the upstream :60-94 order over this can's arms. A miss (no route or
	 * the same-colour no-op, upstream :164) costs nothing.
	 *
	 * @return true when the target changed (a hit: sound + payment + the depletion swap)
	 */
	public boolean spray(Level aLevel, BlockPos aPos, ItemStack aStack, @Nullable Player aPlayer, net.minecraft.world.InteractionHand aHand) {
		boolean tHit;
		BlockEntity tBE = aLevel.getBlockEntity(aPos);
		if (tBE instanceof IPaintableTE tPaintable) {
			tHit = paintPaintableTE(tPaintable, dyeIndex);
		} else {
			Block tTarget = remover() ? decolorTarget(aLevel.getBlockState(aPos).getBlock())
					: colorTarget(aLevel.getBlockState(aPos).getBlock(), dyeIndex);
			tHit = tTarget != null && aLevel.setBlock(aPos, tTarget.defaultBlockState(), 3);
		}
		if (!tHit) return false;
		// the SFX.IC_SPRAY placeholder (see the class javadoc deviation)
		aLevel.playSound(null, aPos, SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1.0F, 1.0F);
		if (aPlayer == null || !aPlayer.getAbilities().instabuild) { // upstream :78 hasInfiniteItems
			ItemStack tSwap = payUses(aStack, maxUses, emptyCan.get());
			if (tSwap != null) { // depleted → the empty can (upstream :85-92)
				if (aPlayer != null) aPlayer.setItemInHand(aHand, tSwap);
				else aStack.setCount(0); // no hand to swap (dispenser-style callers get a shrink)
			}
		}
		return true;
	}

	/**
	 * The TE arm — the upstream recolour routing of TileEntityBase04MultiTileEntities.java
	 * :227-235 with the complement folded away (P21 ADR ruling 3): painted → {@link
	 * IPaintableTE#mixPaint}, unpainted → {@link IPaintableTE#paint}; the remover index →
	 * {@link IPaintableTE#unpaint}. Static so the offline tests drive it on the oven fixture
	 * (the GTPaintableTest shape).
	 */
	public static boolean paintPaintableTE(IPaintableTE aTE, byte aDyeIndex) {
		if (aDyeIndex == REMOVER) return aTE.unpaint(); // the Remover :98 ITileEntityDecolorable arm
		int tRGB = DYES_INT[aDyeIndex];
		return aTE.isPainted() ? aTE.mixPaint(tRGB) : aTE.paint(tRGB); // the 04:228/229 halves
	}

	// ---------------------------------------------------------------------------
	// the uses ledger (the gt.remaining NBT + the payment face)
	// ---------------------------------------------------------------------------

	/** The stored remaining units; a tag-less stack is a full can (the GTCEu :58-64 read form). */
	public static long remainingOf(@Nullable CompoundTag aTag, long aMaxUses) {
		return aTag == null || !aTag.contains(NBT_REMAINING, Tag.TAG_ANY_NUMERIC)
				? aMaxUses : aTag.getLong(NBT_REMAINING);
	}

	/** This stack's remaining units against this can's capacity. */
	public long remainingOf(ItemStack aStack) {
		return remainingOf(carrierTagOf(aStack), maxUses);
	}

	/**
	 * The post-hit counter (upstream :78): creative players are free, everyone else pays
	 * {@link #HIT_COST}, floored at 0.
	 */
	public static long remainingAfterHit(long aRemaining, boolean aCreative) {
		return aCreative ? aRemaining : Math.max(0, aRemaining - HIT_COST);
	}

	/** The depletion verdict (upstream :85 {@code tUses <= 0}). */
	public static boolean depleted(long aRemaining) {
		return aRemaining <= 0;
	}

	/**
	 * The payment face over a stack: writes the decremented counter, and on depletion returns
	 * the fresh empty-can replacement (the caller swaps it into the hand — the upstream :85-92
	 * item+meta swap; a stack carries no hand, so the physical swap is the caller's). The
	 * creative guard ({@link #remainingAfterHit}) is the caller's — it needs the player.
	 *
	 * @return the replacement stack when depleted, else {@code null} (the tag write happened)
	 */
	@Nullable
	public static ItemStack payUses(ItemStack aStack, long aMaxUses, Item aEmptyCan) {
		long tRemaining = remainingAfterHit(remainingOf(carrierTagOf(aStack), aMaxUses), false);
		if (depleted(tRemaining)) return new ItemStack(aEmptyCan);
		//? if forge {
		aStack.getOrCreateTag().putLong(NBT_REMAINING, tRemaining);
		//?} else {
		/*net.minecraft.nbt.CompoundTag tTag = aStack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
				net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
		tTag.putLong(NBT_REMAINING, tRemaining);
		aStack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
				net.minecraft.world.item.component.CustomData.of(tTag)); // 21.1: the envelope write, the GT6Circuits.applyConfiguration shape
		*///?}
		return null;
	}

	/**
	 * The stack carrier read — 1.20.1 freeform NBT tag, 21.1 the same keys inside the opaque
	 * {@code minecraft:custom_data} envelope (the GT6DataComponents payload convention). The
	 * write path forks inline in {@link #payUses} (the envelope is set, not mutated).
	 */
	@Nullable
	private static CompoundTag carrierTagOf(ItemStack aStack) {
		//? if forge {
		return aStack.getTag();
		//?} else {
		/*return aStack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
				net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
		*///?}
	}

	// ---------------------------------------------------------------------------
	// the durability bar (the GTCEu :126-145 face over the NBT counter)
	// ---------------------------------------------------------------------------

	/** The bar shows on a partially-sprayed can (a fresh can and the empty can hide it). */
	public static boolean barVisible(long aRemaining, long aMaxUses) {
		return aRemaining < aMaxUses;
	}

	/** The vanilla 13-pixel bar width over the remaining fraction. */
	public static int barWidth(long aRemaining, long aMaxUses) {
		return (int) Math.round((double) aRemaining * 13.0 / aMaxUses);
	}

	@Override
	public boolean isBarVisible(ItemStack aStack) {
		return barVisible(remainingOf(aStack), maxUses);
	}

	@Override
	public int getBarWidth(ItemStack aStack) {
		return barWidth(remainingOf(aStack), maxUses);
	}

	@Override
	public int getBarColor(ItemStack aStack) {
		return colorRGB();
	}

	// ---------------------------------------------------------------------------
	// the vanilla whitelist table (upstream colorize :144-167 / decolorize :96-106)
	// ---------------------------------------------------------------------------

	private static final byte FAM_GLASS = 0, FAM_PANE = 1, FAM_TERRACOTTA = 2, FAM_STAINED_GLASS = 3,
			FAM_STAINED_PANE = 4, FAM_STAINED_TERRACOTTA = 5, FAM_WOOL = 6, FAM_CARPET = 7;

	/** dye-index → block tables (the GTCEu :77-108 map form, array-indexed by the GT6 index). */
	private static final Block[] TERRACOTTA = new Block[16], STAINED_GLASS = new Block[16],
			STAINED_PANE = new Block[16], WOOL = new Block[16], CARPET = new Block[16];

	/** block → family (the whitelist classifier; absent = not sprayable). */
	private static final Map<Block, Byte> FAMILY_OF = new HashMap<>();

	/** The GT6 dye index → vanilla DyeColor fold (upstream {@code ~mColor&15}, P21 ruling 3). */
	public static net.minecraft.world.item.DyeColor vanillaDye(byte aDyeIndex) {
		return net.minecraft.world.item.DyeColor.byId(~aDyeIndex & 15);
	}

	private static Block vanillaBlock(net.minecraft.world.item.DyeColor aColor, String aPostfix) {
		return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("minecraft",
				aColor.getSerializedName() + "_" + aPostfix));
	}

	static {
		for (byte i = 0; i < 16; i++) {
			net.minecraft.world.item.DyeColor tColor = vanillaDye(i);
			TERRACOTTA[i] = vanillaBlock(tColor, "terracotta"); // the stained_hardened_clay :149 target
			STAINED_GLASS[i] = vanillaBlock(tColor, "stained_glass"); // :151
			STAINED_PANE[i] = vanillaBlock(tColor, "stained_glass_pane"); // :150
			WOOL[i] = vanillaBlock(tColor, "wool"); // the BlockColored :148/:164 family
			CARPET[i] = vanillaBlock(tColor, "carpet"); // :144 whitelist family
			FAMILY_OF.put(TERRACOTTA[i], FAM_STAINED_TERRACOTTA);
			FAMILY_OF.put(STAINED_GLASS[i], FAM_STAINED_GLASS);
			FAMILY_OF.put(STAINED_PANE[i], FAM_STAINED_PANE);
			FAMILY_OF.put(WOOL[i], FAM_WOOL);
			FAMILY_OF.put(CARPET[i], FAM_CARPET);
		}
		FAMILY_OF.put(net.minecraft.world.level.block.Blocks.GLASS, FAM_GLASS);
		FAMILY_OF.put(net.minecraft.world.level.block.Blocks.GLASS_PANE, FAM_PANE);
		FAMILY_OF.put(net.minecraft.world.level.block.Blocks.TERRACOTTA, FAM_TERRACOTTA);
	}

	/**
	 * The colour target — the upstream :146-167 table minus the two target-less arms (the
	 * class javadoc deviations). {@code null} = not sprayable, or the already-that-colour
	 * no-op (upstream :164 guards {@code metadata != target}).
	 */
	@Nullable
	public static Block colorTarget(@Nullable Block aBlock, byte aDyeIndex) {
		Byte tFamily = aBlock == null ? null : FAMILY_OF.get(aBlock);
		if (tFamily == null) return null;
		Block tTarget = switch (tFamily) {
			case FAM_GLASS -> STAINED_GLASS[aDyeIndex]; // :151
			case FAM_PANE -> STAINED_PANE[aDyeIndex]; // :150
			case FAM_TERRACOTTA -> TERRACOTTA[aDyeIndex]; // :149
			case FAM_STAINED_GLASS -> STAINED_GLASS[aDyeIndex]; // :164 family recolour
			case FAM_STAINED_PANE -> STAINED_PANE[aDyeIndex];
			case FAM_STAINED_TERRACOTTA -> TERRACOTTA[aDyeIndex];
			case FAM_WOOL -> WOOL[aDyeIndex];
			case FAM_CARPET -> CARPET[aDyeIndex];
			default -> null;
		};
		return tTarget == aBlock ? null : tTarget;
	}

	/**
	 * The decolour target — the upstream Remover :101-103 reverse rows verbatim (stained
	 * terracotta/glass/pane → plain); wool/carpet have no uncoloured vanilla variant and the
	 * upstream remover has no wool arm either. {@code null} = not removable.
	 */
	@Nullable
	public static Block decolorTarget(@Nullable Block aBlock) {
		if (aBlock == null) return null;
		Byte tFamily = FAMILY_OF.get(aBlock);
		if (tFamily == null) return null;
		return switch (tFamily) {
			case FAM_STAINED_TERRACOTTA -> net.minecraft.world.level.block.Blocks.TERRACOTTA; // :101
			case FAM_STAINED_GLASS -> net.minecraft.world.level.block.Blocks.GLASS; // :103
			case FAM_STAINED_PANE -> net.minecraft.world.level.block.Blocks.GLASS_PANE; // :102
			default -> null;
		};
	}

	// ---------------------------------------------------------------------------
	// the tooltip (the upstream getAdditionalToolTips :174-180 face)
	// ---------------------------------------------------------------------------

	//? if forge {
	@Override
	public void appendHoverText(ItemStack aStack, @Nullable Level aLevel, java.util.List<Component> aTooltip, TooltipFlag aFlag) {
		tooltipLines(aStack, aTooltip);
	}
	//?}

	//? if neoforge {
	/*@Override
	public void appendHoverText(ItemStack aStack, Item.TooltipContext aContext, java.util.List<Component> aTooltip, TooltipFlag aFlag) {
		//21.1: the hover signature carries the Item.TooltipContext (vanilla 1.21.1 Item.java:468) — the GT6Circuits fork shape
		tooltipLines(aStack, aTooltip);
	}
	*///?}

	/** The shared tooltip body (the upstream :174-180 face) — the two leg signatures delegate here. */
	private void tooltipLines(ItemStack aStack, java.util.List<Component> aTooltip) {
		if (remover()) aTooltip.add(Component.translatable(DECOLOR_TOOLTIP_KEY).withStyle(ChatFormatting.BLUE));
		else aTooltip.add(Component.translatable(PAINT_TOOLTIP_KEY, DYE_NAMES[dyeIndex]).withStyle(ChatFormatting.BLUE));
		long tRemaining = remainingOf(aStack);
		aTooltip.add(Component.translatable(REMAINING_TOOLTIP_KEY, tRemaining / HIT_COST, tRemaining % HIT_COST)
				.withStyle(ChatFormatting.GRAY));
	}
}
