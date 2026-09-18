package gregtech6.items.tools;

import gregapi.util.UT;
import javax.annotation.Nullable;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.common.ToolAction;

import gregtech6.block.stone.GTStoneBlock;
import gregtech6.recipes.GT6RecipeMaps;
import gregtech6.recipes.GT6RecipesStoneChisel;
import gregtech6.recipes.Recipe;
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
 * <p>Task p19-chisel-recipes adds the <b>universal gate arm</b> — the ToolCompat.java:224-229
 * transcription, the upstream total entry point for the TOOL_chisel click: for every
 * non-boiler target, the clicked BlockState becomes its item form (GT stone families carry
 * the {@code StoneVariant} stack tag, see {@link GT6RecipesStoneChisel#withVariant}),
 * {@code RM.Chisel.findRecipe} probes it, and on a {@code blockINblockOUT} hit whose input
 * matches the target exactly, the output block is written back ({@code WD.set} →
 * {@code setBlock(pos, state, 3)}) for the upstream 10000 return — converted to 25 vanilla
 * points by the same {@link #durabilityPoints} mapping as the decalcify arm (the two arms
 * COEXIST exactly as upstream: the TE-face onToolClick2 chain answers first, the
 * ToolCompat gate second; a sneaking click never reaches this arm — the :224
 * {@code !aSneaking} gate — and stays PASS).
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
 *     client, the server decides CONSUME/PASS — but only when the gate could actually fire
 *     (non-sneaking and the target resolves to a Chisel recipe), so a chisel click never
 *     swallows a vanilla block use.</li>
 * <li>the mining face (GT_Tool_Chisel.java:80-87 — the stone/silverfish harvest heuristic)
 *     is CUT: this repo has no harvest layer (the cutter cut precedent, pooled with the
 *     tool-family card); the mining-drop chisel conversion (GT_Tool_Chisel.java:57-79)
 *     stays pooled too — 1.20.1 has no HarvestDropsEvent (p19 architect ruling). The
 *     stone-variant chiseling itself, CUT at p16, is LANDED by p19 through the universal
 *     gate arm above.</li>
 * <li>the other {@code TOOL_chisel} consumers (Basin/Mold/RailRoad/ButtonAdvanced/
 *     CoverTextureMulti/... family) are NOT ported — card cut "other TOOL_* family";
 *     the BlockStones TE-face chisel arm (BlockStones.java:573-576, the CHISEL_MAPPINGS
 *     direct-meta write paying 1250/octant) stays pooled with them — the port routes every
 *     stone target through the findRecipe gate instead (the p19 architect ruling).</li>
 * <li>the attack face is the crowbar/cutter declared deviation cut (no attribute map);
 *     the material ladder and the runtime tint LANDED with task p31-machine-ladder (the
 *     {@link GT6ToolLadder} faces over the {@code GT.ToolStats} identity — the former
 *     single-steel/un-tinted deviations retired; the grayscale HANDLE_CHISEL borrow
 *     stays the un-tinted handle layer, the head pass takes the material colour).</li>
 * <li>the crafting recipe landed with the same card (the :305 material rows, the
 *     gt6:material_tool axis).</li>
 * </ul>
 */
public class GTChiselItem extends Item implements GT6ToolLadder.LadderTool {

	/** The vanilla durability points — single steel tier (the crowbar/cutter pinned family value). */
	public static final int DURABILITY_POINTS = 512;

	/** The form durability multiplier (upstream ToolStats.java:71 default 1.0). */
	public static final float DURABILITY_MULTIPLIER = 1.0F;

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
	 * The flattened onToolClick2 — the two arms in the upstream order (the TE face answers
	 * first, the ToolCompat gate second): the decalcify arm on a boiler tank, the universal
	 * gate arm on everything else. PASS on anything neither arm answers; claims on the
	 * client only when an arm could fire (the server side executes and decides
	 * CONSUME/PASS).
	 */
	@Override
	public InteractionResult useOn(UseOnContext aContext) {
		BlockEntity tBE = aContext.getLevel().getBlockEntity(aContext.getClickedPos());
		if (tBE instanceof GTBoilerTankBlockEntity) {
			if (aContext.getLevel().isClientSide) {
				return InteractionResult.SUCCESS; // claim, the server side executes
			}
			return chiselToolClick(aContext) > 0 ? InteractionResult.CONSUME : InteractionResult.PASS;
		}
		// the ToolCompat.java:224-229 universal gate — no TE face answered, the recipe
		// book decides; the client claims only when the gate could actually fire so a
		// chisel click never swallows a vanilla block use
		if (aContext.getLevel().isClientSide) {
			return gateCouldFire(aContext) ? InteractionResult.SUCCESS : InteractionResult.PASS;
		}
		return stoneToolClick(aContext) > 0 ? InteractionResult.CONSUME : InteractionResult.PASS;
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

	// ------------------------------------------------------- the universal gate arm (p19)

	/**
	 * The ToolCompat.java:224-229 transcription — the universal chisel gate over the
	 * {@code GT6RecipeMaps.CHISEL} book:
	 *
	 * <pre>
	 * if (aTool.equals(TOOL_chisel) && !aSneaking) {
	 *     ItemStack tChiseledBlock = WD.stack(aWorld, aX, aY, aZ);
	 *     if (tChiseledBlock != null) {
	 *         Recipe tRecipe = RM.Chisel.findRecipe(null, null, T, Integer.MAX_VALUE, null, ZL_FS, tChiseledBlock);
	 *         if (tRecipe != null && tRecipe.blockINblockOUT() && ST.equal(tRecipe.mInputs[0], tChiseledBlock)
	 *                 && WD.set(aWorld, aX, aY, aZ, tRecipe.mOutputs[0])) return 10000;
	 *     }
	 * }
	 * </pre>
	 *
	 * The variant domain rides the item identity since task p21 (one block+item per
	 * (stone, variant) pair), with the {@link GT6RecipesStoneChisel#VARIANT_TAG} stack tag
	 * kept as the offline synthetic-item separator (the pour writes it on both legs of
	 * every GT stone row; the clicked BlockState encodes into a tagged probe stack via the
	 * block's fixed variant, and the matched output decodes back through its own
	 * BlockItem's block). The
	 * payment is the SAME {@link #payPerPoint} conversion as the decalcify arm: the :228
	 * {@code return 10000} costs {@link #durabilityPoints}(10000) = 25 vanilla points — the
	 * two arms coexist on the two separate payment faces exactly as upstream. A null player
	 * (the RCON/acceptance channel) still converts the block and returns 10000 but pays
	 * nothing (the cutter/crowbar ruling).
	 *
	 * @return the upstream tool damage — {@link #TOOL_DAMAGE_UNIT} (10000) on a conversion,
	 *         0 when any gate leg declines (sneaking, no item form, no recipe, non
	 *         block-in/block-out, input mismatch, setBlock failure).
	 */
	public static long stoneToolClick(UseOnContext aContext) {
		Player tPlayer = aContext.getPlayer();
		return stoneToolClick(aContext, tPlayer != null && tPlayer.isShiftKeyDown());
	}

	/**
	 * The sneak-injected gate core — the offline test seam for the :224 gate (a live Player
	 * instance is not constructible offline: the Forge fluid-type lazy registry dies in the
	 * Entity ctor; the live sneak arm is the RCON fake player's setShiftKeyDown, and the
	 * production overload reads the bit exactly once and lands here).
	 */
	static long stoneToolClick(UseOnContext aContext, boolean aSneaking) {
		if (aSneaking) return 0; // :224 !aSneaking
		Player tPlayer = aContext.getPlayer();
		Level tLevel = aContext.getLevel();
		BlockPos tPos = aContext.getClickedPos();
		BlockState tState = tLevel.getBlockState(tPos);
		ItemStack tProbe = stackFromState(tState);
		if (tProbe == null) return 0; // :225 WD.stack null — the block has no item form
		Recipe tRecipe = findChiselRecipe(tProbe);
		if (tRecipe == null || !blockInBlockOut(tRecipe)) return 0;
		if (!stackEquals(tRecipe.mInputs[0], tProbe)) return 0; // :228 ST.equal — the defensive re-check
		BlockState tOutput = stateFromStack(tRecipe.mOutputs[0]);
		if (tOutput == null || !tLevel.setBlock(tPos, tOutput, 3)) return 0; // :228 WD.set
		long tDamage = TOOL_DAMAGE_UNIT; // :228 return 10000
		payPerPoint(aContext.getItemInHand(), tPlayer, tDamage);
		return tDamage;
	}

	/**
	 * The client-side claim guard: SUCCESS only when the gate could actually fire (the
	 * :224 sneak gate plus a live recipe behind the target), so a chisel click never
	 * swallows a vanilla block use.
	 */
	private static boolean gateCouldFire(UseOnContext aContext) {
		Player tPlayer = aContext.getPlayer();
		if (tPlayer != null && tPlayer.isShiftKeyDown()) return false; // :224 !aSneaking
		return findChiselRecipe(stackFromState(aContext.getLevel().getBlockState(aContext.getClickedPos()))) != null;
	}

	/** The :227 probe — the CHISEL map lookup with the upstream no-voltage/whole-book shape. */
	@Nullable
	private static Recipe findChiselRecipe(@Nullable ItemStack aProbe) {
		if (aProbe == null || GT6RecipeMaps.CHISEL == null) return null;
		return GT6RecipeMaps.CHISEL.findRecipe(null, Long.MAX_VALUE, null, new net.minecraftforge.fluids.FluidStack[0], aProbe);
	}

	/**
	 * The upstream Recipe.blockINblockOUT() (Recipe.java:719-721) transcription: one item
	 * leg in, one out, no fluid legs, both stacks single, both items carrying a block form
	 * (the upstream {@code ST.block(stack) != NB} — the 1.20.1 block-form test is the
	 * BlockItem type). Lives here (not on {@link Recipe}) because this item is its only
	 * consumer and the recipe record stays a minimal port.
	 */
	public static boolean blockInBlockOut(Recipe aRecipe) {
		return aRecipe.mInputs.length == 1 && aRecipe.mOutputs.length == 1
				&& aRecipe.mFluidInputs.length == 0 && aRecipe.mFluidOutputs.length == 0
				&& aRecipe.mInputs[0].getCount() == 1 && aRecipe.mOutputs[0].getCount() == 1
				&& aRecipe.mInputs[0].getItem() instanceof BlockItem
				&& aRecipe.mOutputs[0].getItem() instanceof BlockItem;
	}

	/** The :228 {@code ST.equal} re-check through the Recipe's own item+tag equality. */
	private static boolean stackEquals(ItemStack aRecipeInput, ItemStack aProbe) {
		//? if forge {
		return ItemStack.isSameItemSameTags(aRecipeInput, aProbe);
		//?} else {
		/*return ItemStack.isSameItemSameComponents(aRecipeInput, aProbe);
		*///?}
	}

	/**
	 * The :225 {@code WD.stack(world, x, y, z)} — the clicked block's item form; a GT stone
	 * block IS one (stone, variant) pair since task p21-stoneblocks-16item-registry-split,
	 * so the item identity already separates the variants — the stack tag still rides along
	 * (written from the block's fixed variant) to keep the offline synthetic-item universe
	 * (one item standing for every GT stone leg) exactly-tag-separated as the p19 card
	 * landed it. Other blocks stay plain. Null when the block has no item form (the :226
	 * gate).
	 */
	@Nullable
	public static ItemStack stackFromState(@Nullable BlockState aState) {
		if (aState == null) return null;
		Item tItem = aState.getBlock().asItem();
		if (tItem == net.minecraft.world.item.Items.AIR) return null;
		ItemStack tStack = new ItemStack(tItem, 1);
		if (aState.getBlock() instanceof GTStoneBlock tStone) {
			GT6RecipesStoneChisel.withVariant(tStack, tStone.variant);
		}
		return tStack;
	}

	/**
	 * The :228 {@code WD.set} decode half — the recipe output stack back into a BlockState:
	 * the BlockItem's block, whose default state IS the variant since task p21 (one block
	 * per (stone, variant) pair — the p19 property-upgrade arm retired with the
	 * EnumProperty; the carried tag needs no decode, the item identity decides). Null when
	 * the stack has no block form.
	 */
	@Nullable
	public static BlockState stateFromStack(@Nullable ItemStack aStack) {
		if (aStack == null || !(aStack.getItem() instanceof BlockItem tBlockItem)) return null;
		return tBlockItem.getBlock().defaultBlockState();
	}

	/**
	 * The durability mapping — the Behavior_Tool.java:63 conversion
	 * {@code units(tDamage, 10000, 25, T)} (round-up): 1 damage unit already costs one
	 * point, 1000 → 3, 5000 → 13, 10000 → 25. Static pure function so the offline tests
	 * pin the table (the mod-Item wall, CutterTest NOTE).
	 */
	public static long durabilityPoints(long aToolDamage) {
		return UT.Code.units(aToolDamage, TOOL_DAMAGE_UNIT, UPSTREAM_DAMAGE_PER_REPAIR, true);
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

	// ------------------------------ the GT6ToolLadder identity faces (task p31-machine-ladder) ------------------------------

	/** The per-material durability (the {@link GT6ToolLadder} j/100 points — Steel fallback = 512). */
	@Override
	public int getMaxDamage(ItemStack aStack) {
		return GT6ToolLadder.durabilityPoints(GT6ToolLadder.statsOf(aStack, durabilityMultiplier()));
	}

	/** The form durability multiplier (ToolStats.java:71 default 1.0). */
	@Override
	public float durabilityMultiplier() {
		return DURABILITY_MULTIPLIER;
	}

	/** The runtime tint (the head pass — the toolHeadChisel layer0 pair, the handle layers stay un-tinted). */
	public static int tintARGB(ItemStack aStack, int aTintIndex) {
		return GT6ToolLadder.tintARGB(aStack, aTintIndex);
	}

	/** The composed display name — "Chisel (Bronze)"; bare for identity-less stacks. */
	@Override
	public net.minecraft.network.chat.Component getName(ItemStack aStack) {
		return GT6ToolLadder.displayName(aStack, getDescriptionId());
	}
}
