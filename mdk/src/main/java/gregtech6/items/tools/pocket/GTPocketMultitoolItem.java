package gregtech6.items.tools.pocket;

import java.util.List;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.InteractionResult;
import net.minecraft.tags.BlockTags;
import net.minecraftforge.common.ToolAction;

import gregtech6.items.tools.GT6ToolActions;
import gregtech6.items.tools.GTChiselItem;
import gregtech6.items.tools.GTCutterItem;
import gregtech6.registry.GT6Tools;
import gregtech6.tileentity.energy.converters.GTBoilerTankBlockEntity;

/**
 * The GT6 pocket multitool — task p29-w5-t7-pocket-eight. One class, eight registered
 * forms (the base + form-parameter shape the card allows): the closed multitool plus
 * seven tool faces, ring-switching on a sneak right-click at a bare target.
 *
 * <p>Upstream the family is eight meta ids over one item (Loader_Tools.java:176-183)
 * whose ToolStats chain the switch target through their constructors —
 * POCKET_MULTITOOL(new GT_Tool_Pocket_Knife(...POCKET_SAW)...) — and the NEI redirect
 * loop (:187-196) walks {@code (i+1)%8}. The port flattens the meta id onto the item
 * identity: {@link #next} IS the {@code (i+1)%8} chain, and {@link GT6Tools#POCKET_FORMS}
 * carries the ring order the registration rows pinned (multitool → knife → saw → file →
 * screwdriver → wire cutter → scissors → chisel → multitool).
 *
 * <p>The switch itself ports {@code Behavior_Switch_Metadata} with the {@code mCheckTarget}
 * arms the pocket rows pass (T, T — GT_Tool_Pocket_Multitool.java:53): it fires on the
 * pre-use hook when the holder is sneaking (or absent) and the clicked target is BARE —
 * neither an IItemGT nor a tile entity — so a machine click keeps the tool face instead
 * (onItemUseFirst :40-53). The 1.20.1 pre-use hook flattens to the crowbar/cutter
 * {@code useOn} direct dispatch: sneak + no BE → switch, anything else → the form face.
 *
 * <p>THE CARD'S ItemStack.setItem EVIDENCE LINE FAILED VERIFICATION and the switch
 * carries the declared replacement: vanilla 1.20.1 ItemStack has NO {@code setItem}
 * (the decompile's only setItem hits are entity methods — ThrownExperienceBottle /
 * EyeOfEnder), and the Forge-patched 47.2 sources carry none either, so there is no
 * same-instance item swap on either leg. {@link #switchForm} is the damage-preserving
 * equivalent: a fresh stack of the next form's item carrying the held stack's damage —
 * the observable semantics (same slot, same damage value, new item id) the "same
 * ItemStack, new id, durability kept" ruling describes.
 *
 * <p>No battery, no EU face — the reversal ruling (Loader_Tools.java:176-183 construct
 * without capacity/voltage; the :354 recipe row carries no battery slot). Durability
 * 512, the single-steel-tier family value (ruling d: the upstream 199*U18 × 4.0
 * multiplier scale folds — declared deviation). Pure durability: every payment rides
 * {@link ItemStack#hurtAndBreak} one point at a time.
 *
 * <p>The form faces (each = the corresponding hand tool's face SUBSET — the faces the
 * modern twins actually carry; the twins' pooled faces stay pooled here):
 * <ul>
 * <li><b>MULTITOOL</b> — the closed form: no face at all (upstream isMinableBlock = F,
 *     getBaseDamage = 0, GT_Tool_Pocket_Multitool.java:57-64); it only switches.</li>
 * <li><b>KNIFE</b> — the attack face: upstream getBaseDamage 2.0F (GT_Tool_Knife
 *     :56-58) rides the main-hand attribute map (the crowbar form), {@code hurtEnemy}
 *     pays one point (the 200-unit row folded, the crowbar deviation form). No modern
 *     knife ToolAction exists yet — classification pools with the blade card (t2).</li>
 * <li><b>SAW</b> — the wood+ice mining surface (upstream isMinableBlock GT_Tool_Saw
 *     :166-169 — wood/leaves/ice materials, the "Can harvest Ice" registration-row
 *     wording): the closed tag/set translation {@link #mines} — LOGS/PLANKS/LEAVES
 *     plus the three ice blocks (the ice-material set is three vanilla constants).
 *     Classifies {@link GT6ToolActions#SAW}.</li>
 * <li><b>FILE</b> — the iron-bars mining surface (upstream isMinableBlock
 *     GT_Tool_File.java:66-70 — BlockBaseBars + the iron BlockPane): the modern half
 *     is {@link IronBarsBlock} (vanilla IronBarsBlock.java:18, the only iron pane
 *     block). Speed ×3 on the bars block verbatim (getMiningSpeed :82-85
 *     {@code aDefault * 3}). Classifies {@link GT6ToolActions#FILE}.</li>
 * <li><b>SCREWDRIVER</b> — classification only ({@link GT6ToolActions#SCREWDRIVER});
 *     the circuits mining surface stays pooled with the machine-interaction card
 *     (GT6ScrewdriverItem same cut).</li>
 * <li><b>WIRE CUTTER</b> — the wire/cover right-click face delegates verbatim to
 *     {@link GTCutterItem#cutterToolClick(UseOnContext)} (the shared static, the
 *     p16-chisel reuse shape); the mod-cable mining heuristic stays the twin's cut.
 *     Classifies {@link GT6ToolActions#CUTTER}.</li>
 * <li><b>SCISSORS</b> — the attack face: upstream getBaseDamage 1.0F (GT_Tool_Scissors
 *     :70-72). The cloth/web/vine mining surface stays pooled (t5). No modern
 *     scissors ToolAction yet.</li>
 * <li><b>CHISEL</b> — both GTChiselItem arms delegate verbatim (the boiler decalcify
 *     arm + the universal stone gate, the statics the twin's useOn runs itself); the
 *     open-question ruling: GTChiselItem owns the faces, the pocket form delegates and
 *     GTChiselItem.java stays zero-diff. Classifies {@link GT6ToolActions#CHISEL}.</li>
 * </ul>
 *
 * <p>The NEI redirect (:187-196) has no modern counterpart — the declared deviation
 * puts all eight forms in the creative tab ({@link GT6Tools#TAB_TABLE} tail) and the
 * hide-face rides the JEI binding pool (open item, decisions.open_items_for_user).
 */
public class GTPocketMultitoolItem extends Item {

	/** The form count — the eight meta ids (Loader_Tools.java:176-183). */
	public static final int FORMS = 8;

	public static final int MULTITOOL = 0;
	public static final int KNIFE = 1;
	public static final int SAW = 2;
	public static final int FILE = 3;
	public static final int SCREWDRIVER = 4;
	public static final int WIRE_CUTTER = 5;
	public static final int SCISSORS = 6;
	public static final int CHISEL = 7;

	/** The vanilla durability points — the single-steel-tier family value (ruling d). */
	public static final int DURABILITY_POINTS = 512;

	/** The dig-speed multiplier on the saw/file mineable surface (the crowbar MINING_SPEED declared scale). */
	public static final float MINING_SPEED = 6.0F;

	/** Upstream GT_Tool_Knife.getBaseDamage :56-58. */
	public static final float KNIFE_ATTACK_DAMAGE = 2.0F;

	/** Upstream GT_Tool_Scissors.getBaseDamage :70-72. */
	public static final float SCISSORS_ATTACK_DAMAGE = 1.0F;

	/** The switch-hint line (Behavior_Switch_Metadata :46-48, mShowModeSwitchTooltip=T on every pocket row). */
	public static final String SWITCH_TOOLTIP_KEY = "tooltip.gt6.pocket.switch";

	/** The per-form tooltip key, null where the upstream registration row's note column is "" (:176-183). */
	private static final String[] TOOLTIP_KEYS = {
			"tooltip.gt6.pocket.multitool", null, "tooltip.gt6.pocket.saw", "tooltip.gt6.pocket.file",
			null, "tooltip.gt6.pocket.wire_cutter", "tooltip.gt6.pocket.scissors", "tooltip.gt6.pocket.chisel" };

	/** The form index of this registered instance. */
	public final int form;

	public GTPocketMultitoolItem(int aForm, Properties aProperties) {
		super(aProperties);
		this.form = aForm;
		this.mAttackModifiers = buildAttackModifiers(aForm);
	}

	/** The ring chain — the :176-183 constructor sequence folded to {@code (i+1)%8}, the :187-196 walk. */
	public static int next(int aForm) {
		return (aForm + 1) % FORMS;
	}

	/**
	 * The switch carrier — a fresh stack of the next form's item with THIS stack's damage
	 * (the declared ItemStack.setItem replacement, the class-javadoc ruling). Static pure
	 * seam so the offline test pins the durability preservation without the mod-Item wall.
	 */
	public static ItemStack switchForm(ItemStack aHeld, Item aNext) {
		ItemStack rNext = new ItemStack(aNext);
		rNext.setDamageValue(aHeld.getDamageValue());
		return rNext;
	}

	/**
	 * The pre-use hook flattening: sneak at a bare target switches (the mCheckTarget arms —
	 * no tile entity means neither a machine nor an IItemGT); a tile-entity target keeps
	 * the form face; the wire-cutter/chisel forms delegate to the twins' shared statics
	 * (the payment rides the twin's own item-layer conversion onto the held pocket stack).
	 */
	@Override
	public InteractionResult useOn(UseOnContext aContext) {
		Level tLevel = aContext.getLevel();
		BlockPos tPos = aContext.getClickedPos();
		BlockEntityKind tKind = blockEntityKind(tLevel, tPos);
		if (tKind == BlockEntityKind.NONE && aContext.isSecondaryUseActive()) {
			// the switch arm (onItemUseFirst :40-53): server side swaps, the client claims
			if (tLevel.isClientSide()) {
				return InteractionResult.SUCCESS;
			}
			Player tPlayer = aContext.getPlayer();
			if (tPlayer != null) {
				tPlayer.setItemInHand(aContext.getHand(),
						switchForm(aContext.getItemInHand(), GT6Tools.POCKET_FORMS.get(next(this.form)).get()));
			}
			return InteractionResult.CONSUME;
		}
		if (this.form == WIRE_CUTTER && tKind != BlockEntityKind.NONE) {
			// the cutter delegation: the twin's claim/server-decide shape (GTCutterItem.useOn)
			if (tLevel.isClientSide()) {
				return InteractionResult.SUCCESS;
			}
			return GTCutterItem.cutterToolClick(aContext) > 0 ? InteractionResult.CONSUME : InteractionResult.PASS;
		}
		if (this.form == CHISEL) {
			// the chisel delegation (GTChiselItem.useOn's two arms through the shared statics);
			// the stone gate's client-claim guard stays private to the twin — the pocket form
			// runs claim-less (the GT-variant domain carries no block use, declared deviation)
			if (tKind == BlockEntityKind.BOILER_TANK) {
				if (tLevel.isClientSide()) {
					return InteractionResult.SUCCESS;
				}
				return GTChiselItem.chiselToolClick(aContext) > 0 ? InteractionResult.CONSUME : InteractionResult.PASS;
			}
			if (tKind == BlockEntityKind.NONE && !tLevel.isClientSide()) {
				return GTChiselItem.stoneToolClick(aContext) > 0 ? InteractionResult.CONSUME : InteractionResult.PASS;
			}
		}
		return InteractionResult.PASS;
	}

	/** The bare-target test's BE taxonomy (the mCheckTarget arm: a BE of any kind blocks the switch). */
	private enum BlockEntityKind { NONE, OTHER, BOILER_TANK }

	private static BlockEntityKind blockEntityKind(Level aLevel, BlockPos aPos) {
		var tBE = aLevel.getBlockEntity(aPos);
		if (tBE instanceof GTBoilerTankBlockEntity) {
			return BlockEntityKind.BOILER_TANK;
		}
		return tBE == null ? BlockEntityKind.NONE : BlockEntityKind.OTHER;
	}

	/** The knife/scissors attack face — the 200-unit attack rows fold to one point (the crowbar form). */
	@Override
	public boolean hurtEnemy(ItemStack aStack, LivingEntity aTarget, LivingEntity aAttacker) {
		if (this.form == KNIFE || this.form == SCISSORS) {
			aStack.hurtAndBreak(1, aAttacker, e -> e.broadcastBreakEvent(EquipmentSlot.MAINHAND));
		}
		return super.hurtEnemy(aStack, aTarget, aAttacker);
	}

	/** The static attack face (the offline seam — the mod-Item wall keeps instances unconstructible). */
	public static float attackDamageOf(int aForm) {
		return aForm == KNIFE ? KNIFE_ATTACK_DAMAGE : aForm == SCISSORS ? SCISSORS_ATTACK_DAMAGE : 0.0F;
	}

	/** The attack face rides the main-hand attribute map (the crowbar form); empty below 0-damage forms. */
	//? if forge {
	private final Multimap<Attribute, AttributeModifier> mAttackModifiers;

	private static Multimap<Attribute, AttributeModifier> buildAttackModifiers(int aForm) {
		return attackDamageOf(aForm) > 0
				? ImmutableMultimap.of(Attributes.ATTACK_DAMAGE,
						new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Weapon modifier",
								(double) attackDamageOf(aForm), AttributeModifier.Operation.ADDITION))
				: ImmutableMultimap.of();
	}

	@Override
	public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot aSlot) {
		return aSlot == EquipmentSlot.MAINHAND ? mAttackModifiers : super.getDefaultAttributeModifiers(aSlot);
	}
	//?} else {
	/*// 21.1: the per-slot Multimap override point died with the DataComponents rework
	//(the GTCrowbarItem fork verbatim; the assignment rides the ctor — the inline
	//initializer would read `form` before the ctor assigns it).
	private final net.minecraft.world.item.component.ItemAttributeModifiers mAttackModifiers;

	private static net.minecraft.world.item.component.ItemAttributeModifiers buildAttackModifiers(int aForm) {
		return attackDamageOf(aForm) > 0
				? net.minecraft.world.item.component.ItemAttributeModifiers
						.builder()
						.add(Attributes.ATTACK_DAMAGE,
								new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, (double) attackDamageOf(aForm),
										AttributeModifier.Operation.ADD_VALUE),
								net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
						.build()
				: net.minecraft.world.item.component.ItemAttributeModifiers.EMPTY;
	}

	@Override
	public net.minecraft.world.item.component.ItemAttributeModifiers getDefaultAttributeModifiers() {
		return mAttackModifiers;
	}
	*///?}

	/**
	 * The mining-surface seam — the upstream isMinableBlock modern halves per form (the
	 * crowbar closed-set translation; the tag/set members in the class javadoc). Static
	 * pure function so the offline tests pin it without constructing the item.
	 */
	public static boolean mines(int aForm, BlockState aState) {
		return switch (aForm) {
			// GT_Tool_Saw.java:166-169 — the wood family + the ice set ("Can harvest Ice")
			case SAW -> aState.is(BlockTags.LOGS) || aState.is(BlockTags.PLANKS) || aState.is(BlockTags.LEAVES)
					|| aState.is(Blocks.ICE) || aState.is(Blocks.PACKED_ICE) || aState.is(Blocks.BLUE_ICE);
			// GT_Tool_File.java:66-70 — the iron pane face. The upstream iron-BlockPane test
			// folds to the EXACT block: 1.20.1's IronBarsBlock is also the GLASS_PANE and
			// stained-pane runtime class (Blocks.java:2563/:3285), so an instanceof would
			// wrongly mine glass; the GT BlockBaseBars family is not in this repo's universe.
			case FILE -> aState.is(Blocks.IRON_BARS);
			default -> false;
		};
	}

	/**
	 * The dig-speed seam — the file's ×3 on the bars block verbatim (GT_Tool_File.java:82-85
	 * {@code aDefault * 3}), the saw's bonus on its surface, the vanilla 1.0F hand speed
	 * elsewhere.
	 */
	public static float destroySpeedBonus(int aForm, BlockState aState) {
		if (aForm == FILE && aState.is(Blocks.IRON_BARS)) {
			return 3.0F; // the upstream aDefault(1.0)*3 row, folded
		}
		return mines(aForm, aState) ? MINING_SPEED : 1.0F;
	}

	@Override
	//? if forge {
	public boolean isCorrectToolForDrops(BlockState aState) {
	//?} else {
	/*public boolean isCorrectToolForDrops(ItemStack aStack, BlockState aState) {
	//21.1: the stack parameter joined the signature (the GTCrowbarItem fork — unused,
	//the mineable surface is a pure BlockState function).
	*///?}
		return mines(this.form, aState);
	}

	/** The dig-speed half (the crowbar getDestroySpeed shape). */
	@Override
	public float getDestroySpeed(ItemStack aStack, BlockState aState) {
		return destroySpeedBonus(this.form, aState);
	}

	/**
	 * The stack classifier — per form, the action the modern twin performs (the
	 * cutter/crowbar static-seam shape). KNIFE classifies on the t2 blade card's action
	 * (wired by the S18 rebase seat once p29-w5-t2 landed); SCISSORS still classifies on
	 * nothing — the t5 scene card rides the ShearsItem semantics + the PLANT_SELF loot
	 * seam and created NO {@code gt6_scissors} action (the declared pool entry, still
	 * open until a card creates the action); MULTITOOL is the closed form (upstream :176
	 * carries no OreDictToolNames at all).
	 */
	public static boolean classifies(int aForm, ToolAction aToolAction) {
		return switch (aForm) {
			case SAW -> GT6ToolActions.SAW == aToolAction;
			case FILE -> GT6ToolActions.FILE == aToolAction;
			case SCREWDRIVER -> GT6ToolActions.SCREWDRIVER == aToolAction;
			case WIRE_CUTTER -> GT6ToolActions.CUTTER == aToolAction;
			case CHISEL -> GT6ToolActions.CHISEL == aToolAction;
			case KNIFE -> GT6ToolActions.KNIFE == aToolAction;
			default -> false;
		};
	}

	@Override
	public boolean canPerformAction(ItemStack aStack, ToolAction aToolAction) {
		return classifies(this.form, aToolAction);
	}

	/** The per-form registration id (the TAB_TABLE/TAB census walks). */
	public static String idOf(int aForm) {
		return BuiltInRegistries.ITEM.getKey(GT6Tools.POCKET_FORMS.get(aForm).get()).getPath();
	}

	/** The registration-row note column, the lang key nullable where upstream is "" (:176-183). */
	@Override
	//? if forge {
	public void appendHoverText(ItemStack aStack, @Nullable Level aLevel, List<Component> aTooltip, TooltipFlag aFlag) {
	//?} else {
	/*public void appendHoverText(ItemStack aStack, Item.TooltipContext aLevel, List<Component> aTooltip, TooltipFlag aFlag) {
	//21.1: the Level parameter became Item.TooltipContext (javap Item 21.1).
	*///?}
		String tKey = TOOLTIP_KEYS[this.form];
		if (tKey != null) {
			aTooltip.add(Component.translatable(tKey));
		}
		aTooltip.add(Component.translatable(SWITCH_TOOLTIP_KEY)); // Behavior_Switch_Metadata :46-48
	}
}
