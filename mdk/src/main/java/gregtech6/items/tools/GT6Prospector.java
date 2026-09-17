package gregtech6.items.tools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import gregapi.util.UT.Code;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.InfestedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import gregapi.data.MT;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;
import gregtech6.block.ore.GTOreBlock;
import gregtech6.block.ore.GTOreFallingBlock;
import gregtech6.block.stone.GTStoneBlock;
import gregtech6.block.stone.StoneVariant;
import gregtech6.registry.GT6SurfaceBlocks;

/**
 * The prospector static seam — task p30-pool-prospector, the GTCrowbarItem
 * {@code crowbarToolClick} single-source shape: the item's {@code useOn} and the
 * {@code /gt6tool prospect} acceptance command share this one dispatch, so the RCON
 * chain drives the exact logic a player's hand runs.
 *
 * <p>Upstream mount: NOT a tool of its own — {@code TOOL_prospector} is the HardHammer's
 * SECOND Behavior_Tool arm ({@code GT_Tool_HardHammer.onStatsAddedToTool :131-135},
 * {@code Behavior_Tool(TOOL_prospector, SFX.MC_ANVIL_USE, 10, T, RANDOM_PITCH)}; a
 * whole-library grep finds no other {@code addItemBehavior} mounting prospector). The
 * consumers are the ToolCompat router (ToolCompat.java:367-373), the GT-stone STONE
 * variants (BlockStones.java:572) and the ore prefix blocks (PrefixBlock.java:492); the
 * port flattens all three onto this seam behind {@link GTHammerItem#useOn}.
 *
 * <p>Semantics are the ToolCompat.java:382-437 verbatim translation:
 * <ul>
 * <li>the ore arm (:382-389, returned 100): a clicked GT6 ore block answers
 *     "{@code <prefix local name>!}" through
 *     {@link OreDictPrefix#getLocalizedName} (the ported
 *     LanguageHandler.getLocalName);</li>
 * <li>the stone arm (:391-437, returned 10000): a back-ray of
 *     {@link #scanRadius()} steps (lava / any fluid / air pocket / material change)
 *     plus a {@code radius²} = 16-point cube sampling with the
 *     {@code (long)x^y^z^side} seed verbatim, answering "Found traces of ..." or
 *     "No traces of Ore found".</li>
 * </ul>
 *
 * <p>The five answer strings are deliberately NOT localized — upstream :400-401 rules
 * "The Strings in this do not want to be localized, and not even Backup Lang wants to
 * work"; only the tooltip face enters lang (the ZH ratchet +1).
 *
 * <p>Declared degradation (the strata domain is not ported): prospecting answers block
 * identity + a block-change boundary within the first four ray steps + lava/fluid/air
 * awareness + GT-ore / vanilla-ore traces — ZERO dependence on the GT6 stone-layer
 * system. The "material changing" arm compares BLOCK IDENTITY only (the per-pair
 * universe has no meta axis to compare); the WorldgenStoneLayers strata-table semantics
 * stay in the defer pool. The vanilla-ore half of the trace table is the same declared
 * face: upstream read oredict associations that vanilla ore blocks never carried, so
 * the table is what makes the sampling arm answer on a modern vanilla ore at all.
 */
public final class GT6Prospector {

	/** The upstream tool damage of the ore arm (ToolCompat.java:368 {@code return 100}). */
	public static final long TOOL_DAMAGE_ORE = 100;

	/** The upstream tool damage of the stone arm (ToolCompat.java:370 {@code return 10000}). */
	public static final long TOOL_DAMAGE_STONE = 10000;

	/** The HardHammer's base quality — the ToolStats.java:67 default 0, GT_Tool_HardHammer has no override. */
	public static final long BASE_QUALITY = 0;

	// The five chat literals, byte-for-byte the upstream :403/:407/:411/:415/:430/:435 rows.
	public static final String MSG_NO_TRACES = "No traces of Ore found";
	public static final String MSG_LAVA = "There is Lava behind this Rock";
	public static final String MSG_FLUID = "There is a Fluid behind this Rock";
	public static final String MSG_AIR = "There is an Air Pocket behind this Rock";
	public static final String MSG_CHANGING = "Material is changing behind this Rock";
	public static final String MSG_TRACES = "Found traces of ";

	/**
	 * The vanilla-ore trace table (the declared degradation face, class javadoc) —
	 * both the stone and deepslate hosts per material (the 1.13 flattening doubled the
	 * rows) plus the two nether rows.
	 */
	private static final Map<Block, OreDictMaterial> VANILLA_ORES = new HashMap<>();
	static {
		VANILLA_ORES.put(Blocks.IRON_ORE, MT.Fe);
		VANILLA_ORES.put(Blocks.DEEPSLATE_IRON_ORE, MT.Fe);
		VANILLA_ORES.put(Blocks.COPPER_ORE, MT.Cu);
		VANILLA_ORES.put(Blocks.DEEPSLATE_COPPER_ORE, MT.Cu);
		VANILLA_ORES.put(Blocks.GOLD_ORE, MT.Au);
		VANILLA_ORES.put(Blocks.DEEPSLATE_GOLD_ORE, MT.Au);
		VANILLA_ORES.put(Blocks.NETHER_GOLD_ORE, MT.Au);
		VANILLA_ORES.put(Blocks.REDSTONE_ORE, MT.Redstone);
		VANILLA_ORES.put(Blocks.DEEPSLATE_REDSTONE_ORE, MT.Redstone);
		VANILLA_ORES.put(Blocks.DIAMOND_ORE, MT.Diamond);
		VANILLA_ORES.put(Blocks.DEEPSLATE_DIAMOND_ORE, MT.Diamond);
		VANILLA_ORES.put(Blocks.LAPIS_ORE, MT.Lapis);
		VANILLA_ORES.put(Blocks.DEEPSLATE_LAPIS_ORE, MT.Lapis);
		VANILLA_ORES.put(Blocks.COAL_ORE, MT.Coal);
		VANILLA_ORES.put(Blocks.DEEPSLATE_COAL_ORE, MT.Coal);
		VANILLA_ORES.put(Blocks.EMERALD_ORE, MT.Emerald);
		VANILLA_ORES.put(Blocks.DEEPSLATE_EMERALD_ORE, MT.Emerald);
		VANILLA_ORES.put(Blocks.NETHER_QUARTZ_ORE, MT.NetherQuartz);
	}

	private GT6Prospector() {
	}

	/**
	 * {@code UT.Code.bind(1, 20, quality + 4)} (ToolCompat.java:393) at the HardHammer's
	 * base quality — the scan radius pinned to 4 (the ray length AND the sampling cube's
	 * half-width; the quality axis itself stays the pool cut, the port has no per-tool
	 * quality stat).
	 */
	public static int scanRadius() {
		return (int)Code.bind(1, 20, BASE_QUALITY + 4);
	}

	/** The ore-arm answer, the upstream :385 {@code getLocalName(prefix, material)+"!"} row. */
	public static String oreMessage(OreDictPrefix aPrefix, OreDictMaterial aMaterial) {
		return aPrefix.getLocalizedName(aMaterial) + "!";
	}

	/** The trace answer, the upstream :430 {@code "Found traces of " + material.getLocal()} row. */
	public static String traceMessage(OreDictMaterial aMaterial) {
		return MSG_TRACES + aMaterial.getLocal();
	}

	/** The click-gate ore half: the GT6 ore blocks answer the ore arm (upstream the ORE-prefix association face). */
	public static boolean isOreBlock(BlockState aState) {
		Block tBlock = aState.getBlock();
		return tBlock instanceof GTOreBlock || tBlock instanceof GTOreFallingBlock;
	}

	/**
	 * The sampling-arm trace half for one block: the GT6 ore blocks carry their material
	 * as DATA ({@link GTOreBlock#material}); the vanilla ores ride {@link #VANILLA_ORES}.
	 * Null = no trace.
	 */
	public static OreDictMaterial traceMaterial(BlockState aState) {
		Block tBlock = aState.getBlock();
		if (tBlock instanceof GTOreBlock tOre) return tOre.material;
		if (tBlock instanceof GTOreFallingBlock tOre) return tOre.material;
		return VANILLA_ORES.get(tBlock);
	}

	/**
	 * The entry gate — the upstream :369 {@code isReplaceableOreGen(stone/netherrack/end_stone) || WD.stone}
	 * row flattened to the card-spec ⑤ set: the two vanilla base-stone tags + end_stone +
	 * deepslate (the declared extension: the modern y&lt;0 host rock, the same "stone the
	 * ore sits in" semantics) + the 17 GT-stone STONE variants (BlockStones.java:572).
	 * Obsidian is excluded by not being a member (the upstream explicit :369 test is
	 * subsumed by the closed set).
	 */
	public static boolean prospectable(BlockState aState) {
		Block tBlock = aState.getBlock();
		if (tBlock instanceof GTStoneBlock tStone) return tStone.variant == StoneVariant.STONE;
		if (aState.is(Blocks.END_STONE) || aState.is(Blocks.DEEPSLATE)) return true;
		return aState.is(BlockTags.BASE_STONE_OVERWORLD) || aState.is(BlockTags.BASE_STONE_NETHER);
	}

	/**
	 * The single dispatch + payment surface, shared by {@link GTHammerItem#useOn} and the
	 * {@code /gt6tool prospect} command (the crowbar single-source ruling). PASS-shaped:
	 * returns 0 without side effects when the target answers nothing (the upstream :372
	 * {@code return 0} — no chat, no damage, no sound); a successful arm sends its chat
	 * lines (the Behavior_Tool.java:61-62 sendchat shape), pays ONE vanilla durability
	 * point (both upstream returns are ≥ 1 in the 10000-units-to-1-point mapping, card
	 * spec ⑦; the GTCrowbarItem.java:210 payment form) and plays the ANVIL_USE sound
	 * (the :134 {@code SFX.MC_ANVIL_USE} mount).
	 *
	 * @param aChatCollect nullable extra sink — the command's report channel (RCON has no
	 *        player to display to); the useOn face passes null.
	 * @return the upstream tool damage ({@link #TOOL_DAMAGE_ORE} / {@link #TOOL_DAMAGE_STONE}) or 0.
	 */
	public static long prospect(UseOnContext aContext, @javax.annotation.Nullable List<String> aChatCollect) {
		Level tLevel = aContext.getLevel();
		BlockPos tPos = aContext.getClickedPos();
		List<String> tChat = new ArrayList<>();
		long tDamage = prospect(tLevel.getBlockState(tPos), tLevel, tPos, aContext.getClickedFace(), tChat);
		if (tDamage <= 0) return 0;
		if (aChatCollect != null) aChatCollect.addAll(tChat);
		Player tPlayer = aContext.getPlayer();
		if (tPlayer != null) {
			for (String tLine : tChat) tPlayer.displayClientMessage(Component.literal(tLine), false);
			aContext.getItemInHand().hurtAndBreak(1, tPlayer, p -> p.broadcastBreakEvent(EquipmentSlot.MAINHAND));
		}
		tLevel.playSound(null, tPos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 1.0F, 1.0F); // ponytail: flat pitch, the RANDOM_PITCH mount is cosmetic
		return tDamage;
	}

	/**
	 * The pure dispatch half (the command and the offline doubles ride
	 * {@link #prospect(UseOnContext)} instead — this overload is the seam's logic core):
	 * the ore arm, the gate, then the stone arm. The chat lines accumulate in
	 * {@code aChat}; the return is the upstream damage units.
	 */
	public static long prospect(BlockState aState, BlockGetter aLevel, BlockPos aPos, Direction aFace, List<String> aChat) {
		if (aState.getBlock() instanceof GTOreBlock tOre) { // the ore arm, ToolCompat.java:382-389
			aChat.add(oreMessage(tOre.prefix, tOre.material));
			return TOOL_DAMAGE_ORE;
		}
		if (aState.getBlock() instanceof GTOreFallingBlock tOre) { // the gravity-form twin (same arm)
			aChat.add(oreMessage(tOre.prefix, tOre.material));
			return TOOL_DAMAGE_ORE;
		}
		if (!prospectable(aState)) return 0; // the :369 gate — no answer, no payment
		prospectStone(aState, aLevel, aPos, aFace, aChat); // the stone arm, :391-437 — always answers
		return TOOL_DAMAGE_STONE;
	}

	/**
	 * The stone arm — ToolCompat.java:391-437 verbatim: the back-ray (lava / fluid /
	 * air pocket / material change, the first-four-steps boundary) then the seeded cube
	 * sampling, then the all-empty "No traces" row. Note the upstream shape kept whole:
	 * a ray hit BREAKS the ray only — the sampling still runs, and the "No traces"
	 * fallback only fires when the chat is STILL empty (upstream :435).
	 */
	private static void prospectStone(BlockState aState, BlockGetter aLevel, BlockPos aPos, Direction aFace, List<String> aChat) {
		Block tHost = aState.getBlock();
		int tR = scanRadius();
		// the back-ray: step AWAY from the clicked face (upstream tX -= OFFX[aSide], :396-398)
		Direction tBehind = aFace.getOpposite();
		BlockPos.MutableBlockPos tCursor = new BlockPos.MutableBlockPos();
		for (int i = 0; i < tR; i++) {
			tCursor.set(aPos.getX() + tBehind.getStepX() * (i + 1),
					aPos.getY() + tBehind.getStepY() * (i + 1), aPos.getZ() + tBehind.getStepZ() * (i + 1));
			BlockState tStep = aLevel.getBlockState(tCursor);
			Block tStepBlock = tStep.getBlock();
			// The Strings in this do not want to be localized (upstream :400-401, kept verbatim)
			if (tStepBlock == Blocks.LAVA) { // both lava states are one modern block (:402-405)
				aChat.add(MSG_LAVA);
				break;
			}
			if (!tStep.getFluidState().isEmpty()) { // the BlockLiquid/IFluidBlock pair (:406-409), the fluid-state face
				aChat.add(MSG_FLUID);
				break;
			}
			// the BlockSilverfish pre-check rides the collide face (:410-413); WD.hasCollide ≙ blocksMotion
			if (tStepBlock instanceof InfestedBlock || !tStep.blocksMotion()) {
				aChat.add(MSG_AIR);
				break;
			}
			if (i < 4 && tStepBlock != tHost) { // the meta-pair compare flattened to block identity (:414-417)
				aChat.add(MSG_CHANGING);
				break;
			}
		}
		// the sampling: 16 = radius² draws over the (2*radius+1)³ cube, the (long)x^y^z^side seed (:420-424)
		Random tRandom = new Random(aPos.getX() ^ aPos.getY() ^ aPos.getZ() ^ aFace.get3DDataValue());
		for (int i = 0, j = 1 + 2 * tR, k = tR * tR; i < k; i++) {
			BlockPos tProbe = new BlockPos(aPos.getX() - tR + tRandom.nextInt(j),
					aPos.getY() - tR + tRandom.nextInt(j), aPos.getZ() - tR + tRandom.nextInt(j));
			BlockState tProbeState = aLevel.getBlockState(tProbe);
			Block tProbeBlock = tProbeState.getBlock();
			// the NB/obsidian/RockOres exclusions (:427) — the surface rocks are the modern RockOres face
			if (!tProbeState.isAir() && tProbeBlock != Blocks.OBSIDIAN && !isSurfaceRock(tProbeBlock)) {
				OreDictMaterial tMaterial = traceMaterial(tProbeState);
				if (tMaterial != null) {
					aChat.add(traceMessage(tMaterial));
					return;
				}
			}
		}
		if (aChat.isEmpty()) aChat.add(MSG_NO_TRACES); // :435
	}

	/** The RockOres exclusion face — the three GT6 surface rocks (BlocksGT.RockOres counterpart). */
	private static boolean isSurfaceRock(Block aBlock) {
		return aBlock == GT6SurfaceBlocks.SURFACE_ROCK_STONE.get()
				|| aBlock == GT6SurfaceBlocks.SURFACE_ROCK_FLINT.get()
				|| aBlock == GT6SurfaceBlocks.SURFACE_ROCK_METEORITE.get();
	}
}
