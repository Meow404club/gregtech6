package gregtech6.items.tools.loot;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.phys.Vec3;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

//? if forge {
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
//?} else {
/*import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
//21.1: same simple names, neoforged package (the GT6ToolLootModifiers fork verbatim —
//the loot subtree is NOT on the stonecutter swap table).
*///?}

import gregtech6.items.tools.GTAxeItem;

/**
 * The axe whole-tree felling modifier — task p29-w5-t2-blade-six (the serializer row
 * rides {@link GT6ToolLootModifiers#SERIALIZERS} per the documented consumer contract:
 * "else a new modifier class + a serializer row"). Upstream
 * GT_Tool_Axe.java:102-125 ({@code LOCK} + the trunk-up walk), modern form:
 * <ul>
 * <li><b>The gate</b> (:107) — {@link GTAxeItem#isFellable} (the {@code #minecraft:logs}
 *     family + the huge mushroom blocks; the TreeCap/dynamic-trees/WoodDictionary mod
 *     arms are the declared-F cuts — vanilla trees only) + the sneak gate
 *     {@code !isSteppingCarefully} + a ServerPlayer breaker.</li>
 * <li><b>The guard</b> — the upstream {@code LOCK} static boolean IS the
 *     {@link GT6ToolSweep} ThreadLocal sentinel (the shared guard).</li>
 * <li><b>The walk</b> (:110-122) — scan the column ABOVE the broken base while the same
 *     log block stands (world top = the cap, the open-questions 逐字 ruling), then
 *     harvest TOP-DOWN (:119 {@code --tY}). The 21.1 drop-capture lesson: the walk does
 *     NOT ride {@code ServerPlayerGameMode.destroyBlock} — it runs INSIDE the base
 *     break's loot evaluation, and the 21.1 {@code dropResources} capture window
 *     re-entered through the walk's own drop face (the outer capture came back null).
 *     The port form: {@code removeBlock} per felled log + the break particle event, the
 *     felled stacks APPENDED to this modifier's loot output (the outer flow spawns
 *     them), and ONE durability point per felled log paid on the held tool (the
 *     vanilla-mineBlock payment folded onto the walk — the card face: the payment = the
 *     tree height, the upstream :114 {@code rAmount >= aAvailableDurability continue}
 *     semantics as the −1 tool-keep-alive margin).</li>
 * <li><b>FAST_LEAF_DECAY</b> (:120-122) — CUT: the vanilla distance-based leaf decay
 *     supersedes (leaves fall on their own tick after the trunk goes).</li>
 * </ul>
 *
 * <p>JSON: {@code {"type": "gt6:gt6_tree_fell", "conditions": [{"condition":
 * "gt6:holds_tool", "tool": "gt6:axe"}]}} — no extra fields (the conditions carry the
 * per-tool identity, the base class runs them before {@link #doApply}).
 */
public class GT6TreeFellModifier extends LootModifier {

	/** The element type forks per leg — Codec (forge 1.20.1) vs MapCodec (neo 21.1). */
	//? if forge {
	public static final Codec<GT6TreeFellModifier> CODEC =
			RecordCodecBuilder.create(aInst -> codecStart(aInst)
					.apply(aInst, GT6TreeFellModifier::new));
	//?} else {
	/*public static final com.mojang.serialization.MapCodec<GT6TreeFellModifier> CODEC =
			RecordCodecBuilder.mapCodec(aInst -> codecStart(aInst)
					.apply(aInst, GT6TreeFellModifier::new));
	*///?}

	public GT6TreeFellModifier(LootItemCondition[] aConditions) {
		super(aConditions);
	}

	@Override
	//? if forge {
	public Codec<? extends IGlobalLootModifier> codec() {
		return CODEC;
	}
	//?} else {
	/*public com.mojang.serialization.MapCodec<? extends IGlobalLootModifier> codec() {
		return CODEC;
	}
	*///?}

	@Override
	protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> aLoot, LootContext aContext) {
		BlockState tState = aContext.getParamOrNull(LootContextParams.BLOCK_STATE);
		if (tState == null || !GTAxeItem.isFellable(tState)) return aLoot;
		if (!(aContext.getParamOrNull(LootContextParams.THIS_ENTITY) instanceof ServerPlayer tPlayer)) return aLoot;
		if (tPlayer.isSteppingCarefully()) return aLoot; // upstream :107 !isSneaking — the single-log sneak face
		Vec3 tOrigin = aContext.getParamOrNull(LootContextParams.ORIGIN);
		if (tOrigin == null) return aLoot;
		fell(tPlayer, BlockPos.containing(tOrigin), tState.getBlock(), aLoot);
		return aLoot;
	}

	/**
	 * The upstream :110-122 walk — see the class javadoc. Exposed static for the
	 * command/test seams; {@code aLog} is the base block the caller already broke; the
	 * felled stacks land in {@code aLoot} (the modifier output the outer flow spawns).
	 *
	 * @return the number of felled logs (the guard-blocked re-entry returns 0).
	 */
	public static int fell(ServerPlayer aPlayer, BlockPos aBase, Block aLog, List<ItemStack> aLoot) {
		if (!GT6ToolSweep.tryEnter()) return 0; // the upstream LOCK (GT_Tool_Axe.java:102/:108/:124)
		try {
			ServerLevel tLevel = aPlayer.serverLevel();
			ItemStack tTool = aPlayer.getMainHandItem();
			// the durability margin (upstream :114 continue semantics): one point per felled
			// log paid below, the −1 keeps the tool alive
			int tBudget = Math.max(0, tTool.getMaxDamage() - tTool.getDamageValue() - 1);
			List<BlockPos> tFell = new ArrayList<>();
			// the counting scan (upstream :112-117): straight up while the same log stands
			for (BlockPos tCursor = aBase.above(); tCursor.getY() < tLevel.getMaxBuildHeight()
					&& tLevel.getBlockState(tCursor).getBlock() == aLog; tCursor = tCursor.above()) {
				if (tFell.size() < tBudget) tFell.add(tCursor.immutable());
			}
			// the harvest (upstream :119 --tY): top-down, remove + loot-append + pay
			int rFelled = 0;
			for (int i = tFell.size() - 1; i >= 0; i--) {
				BlockPos tPos = tFell.get(i);
				tLevel.levelEvent(net.minecraft.world.level.block.LevelEvent.PARTICLES_DESTROY_BLOCK /*2001 break particles*/,
						tPos, Block.getId(tLevel.getBlockState(tPos)));
				tLevel.removeBlock(tPos, false);
				aLoot.add(new ItemStack(aLog.asItem()));
				//? if forge {
				tTool.hurtAndBreak(1, aPlayer, e -> e.broadcastBreakEvent(net.minecraft.world.entity.EquipmentSlot.MAINHAND));
				//?} else {
				/*tTool.hurtAndBreak(1, aPlayer, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
				//21.1: the hurt callback folded onto the slot param (ItemStack.java:478).
				*///?}
				rFelled++;
			}
			return rFelled;
		} finally {
			GT6ToolSweep.exit();
		}
	}
}
