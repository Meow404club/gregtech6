package gregtech6.tileentity.tools;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import gregapi.data.CS;
import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.data.TD;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictMaterialStack;
import gregapi.oredict.OreDictPrefix;
import gregapi.tileentity.machines.ITileEntityCrucible;
import gregapi.tileentity.machines.ITileEntityMold;
import gregapi.tileentity.temperature.ITileEntityTemperature;
import gregapi.util.CruciblePhysics;
import gregapi.util.UT;
import gregtech6.recipes.maps.GT6RecipeMapCrucible;
import gregtech6.registry.GT6Molds;
import gregtech6.tileentity.MaterialStackNBT;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;

/**
 * 1.20.1 counterpart of the GT6 Mold — task p26-crucible-physics-smeltery spec ⑤ (the
 * A-card minimal face, ported from gregtech/tileentity/tools/MultiTileEntityMold.java:
 * the :74 class face), the crucible's casting partner: pour molten material in, let it
 * cool past the melting point, take the solidified shape out.
 *
 * <p><b>The tick (upstream onServerTickPost :156-211)</b>: the ±5 K drift toward the
 * environment (:160 — the mold has NO energy face, it is a heat sink); the content
 * solidifies when the temperature falls below the material's melting point (:189-202 —
 * the material swaps to mTargetSolidifying and the {@link #getMoldRecipe} prefix pours
 * the item into the output slot); an over-hot mold (above the content's boiling point or
 * above {@link #getMoldMaxTemperature}) trashes the content and melts into lava
 * (:179-186).
 *
 * <p><b>The shape</b>: the 5x5 {@code gt.mold} bitmask NBT (:95/:109) maps through
 * {@link #MOLD_RECIPES} to the output prefix with the OP.nugget fallback (:79-83). The
 * card face registers the INGOT bar shapes (the :690-694 3-wide x 5-tall bars, the three
 * column shifts) and the nugget fallback; the chisel re-carving face and the remaining
 * shape universe are the card-B surface. Declared deviation: the Stone Mold item ships
 * PRE-CARVED with the ingot bar (the row0 playable arm — the A-card acceptance chain
 * needs no chisel gymnastics; the bitmask contract itself is upstream-verbatim).
 *
 * <p><b>The pour</b>: {@link #fillMold} (:246-264 verbatim) requires an empty mold, an
 * input side, an acid-free material and a representable output, then takes exactly the
 * required amount (units(requiredUnits, U, mTargetSolidifying.mAmount, T)); the mold
 * right-click drives the adjacent {@link ITileEntityCrucible#fillMoldAtSide} (:267-294)
 * and the output hand-take (:296-322). The wrench auto-pull arm (:170-176) is card B.
 *
 * <p><b>Card B append (p26-crucible-mold-faucet)</b>: the FULL shape universe —
 * {@link #MOLD_RECIPES} filled by the literal port of the :628-921 static block (the
 * base shapes, the three per-entry bijections :849-877 and the two round-two
 * bijections :889-917 over the merged map; java.util.HashMap keeps the upstream
 * last-write-wins collision order, 549 final rows), LAZY on the first
 * {@link #getMoldRecipe} lookup — the P6 lesson (a9027ac): the 21.1 mod-construct
 * scan class-loads this BE before OP is initialized and an eager put would poison
 * the table with null values forever; the {@code COOL2CRYSTAL}
 * plate→plateGem / plateTiny→plateGemTiny swap (:194-197 in the tick, :250-253 in
 * {@link #fillMold}); the monkey-wrench auto-pull — {@link #mAutoPullDirections} set
 * from the top-face horizontal sub-sides (:343-355), the {@code SERVER_TIME % 20 == 5}
 * cadence pull (:169-176, the per-BE {@code aTimer % 20} port cadence) and the soft
 * hammer reset (:337-342). The chisel carving face (:328-336) stays out (no chisel
 * seam on the ported tools; the ceramic molds ship pre-carved, the stone ships
 * pre-carved ingot). The upstream {@code mUseRedstone} mode (:96/:351) ports as
 * {@link #mUseRedstone} with the vanilla all-sides signal check standing in for
 * {@code hasRedstoneIncoming()}; the physical monkey wrench item is the tool-system
 * card's to wire — the state machine and the RCON arm ({@link #toolMonkeyWrench} /
 * {@link #toolSoftHammer}) land here.
 */
public class TileEntityMold extends TileEntityBase03TicksAndSync implements ITileEntityTemperature, ITileEntityMold {

	/** The shape NBT key (upstream :95/:109 verbatim 'gt.mold'). */
	public static final String NBT_MOLD = "gt.mold";
	/** The temperature NBT key (the Smeltery spelling). */
	public static final String NBT_TEMPERATURE = "gt.temperature";

	/** The card-B NBT keys (upstream NBT_CONNECTION/NBT_MODE, the trimmed port spelling). */
	public static final String NBT_CONNECTION = "connection";
	public static final String NBT_MODE = "mode";

	/** The upstream :87 auto-pull side mask (the SBIT set from the monkey wrench, :347). */
	public byte mAutoPullDirections = 0;

	/** The upstream :85 redstone mode (:351 toggle — pull only while a signal comes in). */
	public boolean mUseRedstone = false;

	/** The upstream :75 form bonus over the shell material. */
	public static final double HEAT_RESISTANCE_BONUS = 1.25;

	/** Upstream :89 — the offline default (DEF_ENV_TEMP = C + 20). */
	public static final long DEF_ENV_TEMP = 293;

	/** The port side order: UP = 1 (the HuEnergyHandshake truth table). */
	public static final byte SIDE_TOP = 1;

	/** Upstream :81 — B[25]-1, the 25-bit shape mask of getMoldRecipe. */
	public static final int SHAPE_MASK = (1 << 25) - 1;

	/**
	 * Upstream :77 — the shape→prefix map. The card face registers the three ingot-bar
	 * shifts (MultiTileEntityMold.java:690-694); getMoldRecipe answers OP.nugget for any
	 * other non-zero shape. LAZY fill (the P6 lesson, a9027ac): the 21.1 mod-construct
	 * @EventBusSubscriber scan class-loads this BE before OP is initialized, and an eager
	 * put would poison the table with null values forever — the fill rides the first
	 * lookup instead, when OP is guaranteed ready.
	 */
	public static final Map<Integer, OreDictPrefix> MOLD_RECIPES = new HashMap<>();

	/** The ingot bar bitmask at column shift i (the :690-694 loop form). */
	public static int ingotShape(int i) {
		int rShape = 0;
		for (int row = 0; row < 5; row++) rShape |= 0b111 << (row * 5 + i);
		return rShape;
	}

	// ------------------------------------------------------------------------------------
	// the LAZY fill (the P6 lesson, a9027ac): the 21.1 mod-construct @EventBusSubscriber
	// scan class-loads this BE before OP is initialized, and an eager static put would
	// poison the table with null values forever — the fill rides the first getMoldRecipe
	// lookup instead, when OP is guaranteed ready.
	// ------------------------------------------------------------------------------------

	/** The ingot bars ride first (the :690-694 loop form), the universe continues below. */
	private static void fillMoldRecipes() {
		for (int i = 0; i < 3; i++) MOLD_RECIPES.put(ingotShape(i), OP.ingot);
	}

	// ------------------------------------------------------------------------------------
	// card B: the full shape universe (the literal port of MultiTileEntityMold.java:628-921)
	// ------------------------------------------------------------------------------------

	/** The upstream {@code B[i]} bit — CS.B is not ported, the local helper stands in. */
	private static int bit(int i) {
		return 1 << i;
	}

	/*
	 * The :628-921 static block, verbatim: the base shapes into a TEMP map, the first
	 * round — every TEMP entry plus its three per-entry bijections (:849-877) into
	 * {@link #MOLD_RECIPES} — then the merged map re-iterated through the two round-two
	 * bijections (:889-917). java.util.HashMap reproduces the upstream last-write-wins
	 * collision order (549 final rows; the mdk test pins the 30 ceramic shapes 30/30).
	 * Runs after {@link #fillMoldRecipes()} in declaration order (after the ingot bars).
	 */
	private static void fillMoldRecipesUniverse() {
		Map<Integer, OreDictPrefix> tTemp = new HashMap<>();

		tTemp.put(0b0_00100_11111_01110_01010_00000, OP.toolHeadBuilderwand);
		tTemp.put(0b0_00000_00100_11111_01110_01010, OP.toolHeadBuilderwand);

		tTemp.put(0b0_00000_00110_01111_01111_00110, OP.billet);
		tTemp.put(0b0_00000_01100_11110_11110_01100, OP.billet);
		tTemp.put(0b0_00110_01111_01111_00110_00000, OP.billet);
		tTemp.put(0b0_01100_11110_11110_01100_00000, OP.billet);

		tTemp.put(bit( 0)|bit( 1)|bit( 2)|bit( 3)|
				bit( 5)|bit( 6)|bit( 7)|bit( 8)|
				bit(10)|bit(11)|bit(12)|bit(13)|bit(14)|
				bit(15)|bit(16)|bit(17)|bit(18)|
				bit(20)|bit(21)|bit(22)|bit(23)
				, OP.toolHeadRawPlow);

		tTemp.put(bit( 4)|
				bit( 8)|
				bit(12)|
				bit(16)|
				bit(20)
				, OP.stickLong);

		tTemp.put(bit( 0)|bit( 1)|bit( 2)|bit( 3)|bit( 4)|
				bit( 5)|bit( 6)|bit( 7)|bit( 8)|bit( 9)|
				bit(10)|bit(11)|bit(12)|bit(13)|bit(14)|
				bit(15)|bit(16)|bit(17)|bit(18)|bit(19)|
				bit(20)|bit(21)|bit(22)|bit(23)|bit(24)
				, OP.plate);

		tTemp.put(bit( 0)|bit( 1)|bit( 2)|    bit( 4)|
				bit( 5)|bit( 6)|bit( 7)|    bit( 9)|
				bit(10)|bit(11)|bit(12)|    bit(14)|
								bit(19)|
				bit(20)|bit(21)|bit(22)
				, OP.casingSmall);

		tTemp.put(bit( 0)|    bit( 2)|    bit( 4)|
					bit( 6)|bit( 7)|bit( 8)|
				bit(10)|bit(11)|    bit(13)|bit(14)|
					bit(16)|bit(17)|bit(18)|
				bit(20)|    bit(22)|    bit(24)
				, OP.gearGt);

		tTemp.put(        bit( 1)|    bit( 3)|
				bit( 5)|bit( 6)|bit( 7)|bit( 8)|bit( 9)|
						bit(11)|    bit(13)|
				bit(15)|bit(16)|bit(17)|bit(18)|bit(19)|
						bit(21)|    bit(23)
				, OP.gearGtSmall);

		for (int i = 0; i < 3; i++) {
			tTemp.put(bit(i  + 0)|bit(i  + 1)|bit(i  + 2)|
					bit(i  + 5)|bit(i  + 6)|bit(i  + 7)|
					bit(i +10)|bit(i +11)|bit(i +12)|
					bit(i +15)|bit(i +16)|bit(i +17)|
					bit(i +20)|bit(i +21)|bit(i +22)
					, OP.ingot);

			tTemp.put(bit(i  + 0)|bit(i  + 1)|bit(i  + 2)|
					bit(i  + 5)|bit(i  + 6)|
					bit(i +10)|bit(i +11)|
					bit(i +15)|bit(i +16)|
					bit(i +20)|bit(i +21)|bit(i +22)
					, OP.toolHeadRawAxeDouble);

			tTemp.put(bit(i  + 0)|bit(i  + 1)|bit(i  + 2)|
					bit(i  + 5)|bit(i  + 6)|bit(i  + 7)|
					bit(i +10)|        bit(i +12)|
					bit(i +15)|bit(i +16)|bit(i +17)|
					bit(i +20)|bit(i +21)|bit(i +22)
					, OP.toolHeadHammer);

			tTemp.put(bit(i  + 0)|
					bit(i  + 5)|
					bit(i +10)|
					bit(i +15)|
					bit(i +20)
					, OP.stick);

			tTemp.put(bit(i  + 0)|bit(i  + 1)|bit(i  + 2)|
								bit(i  + 6)|
								bit(i +11)|
								bit(i +16)|
								bit(i +21)
					, OP.toolHeadRawChisel);

			tTemp.put(bit(i  + 0)|bit(i  + 1)|bit(i  + 2)|
					bit(i  + 5)|bit(i  + 6)|bit(i  + 7)|
					bit(i +10)|bit(i +11)|bit(i +12)|
								bit(i +16)|
								bit(i +21)
					, OP.toolHeadFile);

			tTemp.put(        bit(i  + 1)|
					bit(i  + 5)|bit(i  + 6)|bit(i  + 7)|
					bit(i +10)|bit(i +11)|bit(i +12)|
					bit(i +15)|bit(i +16)|bit(i +17)|
					bit(i +20)|bit(i +21)|bit(i +22)
					, OP.toolHeadRawSword);

			for (int j = 0; j < 4; j++) {
				tTemp.put(        bit(i +j*5+ 1)|bit(i +j*5+ 2)|
						bit(i +j*5+ 5)|bit(i +j*5+ 6)|bit(i +j*5+ 7)
						, OP.toolHeadRawHoe);
			}

			for (int j = 0; j < 3; j++) {
				tTemp.put(        bit(i +j*5+ 1)|
								bit(i +j*5+ 6)|
						bit(i +j*5+10)|bit(i +j*5+11)|bit(i +j*5+12)
						, OP.toolHeadRawArrow);

				tTemp.put(bit(i +j*5+ 0)|bit(i +j*5+ 1)|bit(i +j*5+ 2)|
						bit(i +j*5+ 5)|bit(i +j*5+ 6)|bit(i +j*5+ 7)|
						bit(i +j*5+10)
						, OP.toolHeadRawAxe);

				tTemp.put(bit(i +j*5+ 0)|bit(i +j*5+ 1)|
						bit(i +j*5+ 5)|bit(i +j*5+ 6)
						, OP.chunkGt);

				tTemp.put(bit(i +j*5+ 0)|bit(i +j*5+ 1)|bit(i +j*5+ 2)|
						bit(i +j*5+ 5)|            bit(i +j*5+ 7)|
						bit(i +j*5+10)|bit(i +j*5+11)|bit(i +j*5+12)
						, OP.ring);

				tTemp.put(bit(i +j*5+ 0)|bit(i +j*5+ 1)|bit(i +j*5+ 2)|
						bit(i +j*5+ 5)|bit(i +j*5+ 6)|bit(i +j*5+ 7)|
						bit(i +j*5+10)|bit(i +j*5+11)|bit(i +j*5+12)
						, OP.plateTiny);

				tTemp.put(bit(i +j*5+ 0)|
						bit(i +j*5+ 5)
						, OP.bolt);
			}

			for (int j = 0; j < 2; j++) {
				tTemp.put(        bit(i +j*5+ 1)|
						bit(i +j*5+ 5)|bit(i +j*5+ 6)|bit(i +j*5+ 7)|
						bit(i +j*5+10)|bit(i +j*5+11)|bit(i +j*5+12)|
						bit(i +j*5+15)|bit(i +j*5+16)|bit(i +j*5+17)
						, OP.toolHeadRawShovel);

				tTemp.put(bit(i +j*5+ 0)|bit(i +j*5+ 1)|bit(i +j*5+ 2)|
						bit(i +j*5+ 5)|bit(i +j*5+ 6)|bit(i +j*5+ 7)|
						bit(i +j*5+10)|bit(i +j*5+11)|bit(i +j*5+12)|
						bit(i +j*5+15)|            bit(i +j*5+17)
						, OP.toolHeadRawSpade);

				tTemp.put(        bit(i +j*5+ 1)|
						bit(i +j*5+ 5)|bit(i +j*5+ 6)|bit(i +j*5+ 7)|
						bit(i +j*5+10)|bit(i +j*5+11)|
						bit(i +j*5+15)|bit(i +j*5+16)|bit(i +j*5+17)
						, OP.toolHeadRawUniversalSpade);

				tTemp.put(bit(i +j*5+ 0)|
						bit(i +j*5+ 5)|
						bit(i +j*5+10)|
						bit(i +j*5+15)
						, OP.toolHeadScrewdriver);
			}
		}

		for (int i = 0; i < 4; i++) {
			tTemp.put(        bit(i  + 1)|
					bit(i  + 5)|
					bit(i +10)|
					bit(i +15)|
							bit(i +21)
					, OP.toolHeadRawPickaxe);

			tTemp.put(bit(i  + 0)|bit(i  + 1)|
					bit(i  + 5)|bit(i  + 6)|
					bit(i +10)|bit(i +11)|
					bit(i +15)|bit(i +16)|
					bit(i +20)|bit(i +21)
					, OP.toolHeadRawSaw);

			tTemp.put(bit(i  + 0)|bit(i  + 1)|
					bit(i  + 5)|bit(i  + 6)|
					bit(i +10)|bit(i +11)|
					bit(i +15)|bit(i +16)|
							bit(i +21)
					, OP.toolHeadRawSense);
		}

		// the first permutation round (:845-882) — every entry plus its three bijections
		for (Map.Entry<Integer, OreDictPrefix> tEntry : tTemp.entrySet()) {
			int tKey = tEntry.getKey(), tResult1 = 0, tResult2 = 0, tResult3 = 0;
			MOLD_RECIPES.put(tKey, tEntry.getValue());

			if ((tKey & bit( 0)) != 0) {tResult1 |= bit( 4); tResult2 |= bit(24); tResult3 |= bit(20);}
			if ((tKey & bit( 1)) != 0) {tResult1 |= bit( 9); tResult2 |= bit(23); tResult3 |= bit(15);}
			if ((tKey & bit( 2)) != 0) {tResult1 |= bit(14); tResult2 |= bit(22); tResult3 |= bit(10);}
			if ((tKey & bit( 3)) != 0) {tResult1 |= bit(19); tResult2 |= bit(21); tResult3 |= bit( 5);}
			if ((tKey & bit( 4)) != 0) {tResult1 |= bit(24); tResult2 |= bit(20); tResult3 |= bit( 0);}

			if ((tKey & bit( 5)) != 0) {tResult1 |= bit( 3); tResult2 |= bit(19); tResult3 |= bit(21);}
			if ((tKey & bit( 6)) != 0) {tResult1 |= bit( 8); tResult2 |= bit(18); tResult3 |= bit(16);}
			if ((tKey & bit( 7)) != 0) {tResult1 |= bit(13); tResult2 |= bit(17); tResult3 |= bit(11);}
			if ((tKey & bit( 8)) != 0) {tResult1 |= bit(18); tResult2 |= bit(16); tResult3 |= bit( 6);}
			if ((tKey & bit( 9)) != 0) {tResult1 |= bit(23); tResult2 |= bit(15); tResult3 |= bit( 1);}

			if ((tKey & bit(10)) != 0) {tResult1 |= bit( 2); tResult2 |= bit(14); tResult3 |= bit(22);}
			if ((tKey & bit(11)) != 0) {tResult1 |= bit( 7); tResult2 |= bit(13); tResult3 |= bit(17);}
			if ((tKey & bit(12)) != 0) {tResult1 |= bit(12); tResult2 |= bit(12); tResult3 |= bit(12);}
			if ((tKey & bit(13)) != 0) {tResult1 |= bit(17); tResult2 |= bit(11); tResult3 |= bit( 7);}
			if ((tKey & bit(14)) != 0) {tResult1 |= bit(22); tResult2 |= bit(10); tResult3 |= bit( 2);}

			if ((tKey & bit(15)) != 0) {tResult1 |= bit( 1); tResult2 |= bit( 9); tResult3 |= bit(23);}
			if ((tKey & bit(16)) != 0) {tResult1 |= bit( 6); tResult2 |= bit( 8); tResult3 |= bit(18);}
			if ((tKey & bit(17)) != 0) {tResult1 |= bit(11); tResult2 |= bit( 7); tResult3 |= bit(13);}
			if ((tKey & bit(18)) != 0) {tResult1 |= bit(16); tResult2 |= bit( 6); tResult3 |= bit( 8);}
			if ((tKey & bit(19)) != 0) {tResult1 |= bit(21); tResult2 |= bit( 5); tResult3 |= bit( 3);}

			if ((tKey & bit(20)) != 0) {tResult1 |= bit( 0); tResult2 |= bit( 4); tResult3 |= bit(24);}
			if ((tKey & bit(21)) != 0) {tResult1 |= bit( 5); tResult2 |= bit( 3); tResult3 |= bit(19);}
			if ((tKey & bit(22)) != 0) {tResult1 |= bit(10); tResult2 |= bit( 2); tResult3 |= bit(14);}
			if ((tKey & bit(23)) != 0) {tResult1 |= bit(15); tResult2 |= bit( 1); tResult3 |= bit( 9);}
			if ((tKey & bit(24)) != 0) {tResult1 |= bit(20); tResult2 |= bit( 0); tResult3 |= bit( 4);}

			MOLD_RECIPES.put(tResult1, tEntry.getValue());
			MOLD_RECIPES.put(tResult2, tEntry.getValue());
			MOLD_RECIPES.put(tResult3, tEntry.getValue());
		}

		tTemp.putAll(MOLD_RECIPES);

		// the second permutation round (:886-921) — two bijections over the merged map
		for (Map.Entry<Integer, OreDictPrefix> tEntry : tTemp.entrySet()) {
			int tKey = tEntry.getKey(), tResult1 = 0, tResult2 = 0;

			if ((tKey & bit( 0)) != 0) {tResult1 |= bit( 4); tResult2 |= bit(20);}
			if ((tKey & bit( 1)) != 0) {tResult1 |= bit( 3); tResult2 |= bit(21);}
			if ((tKey & bit( 2)) != 0) {tResult1 |= bit( 2); tResult2 |= bit(22);}
			if ((tKey & bit( 3)) != 0) {tResult1 |= bit( 1); tResult2 |= bit(23);}
			if ((tKey & bit( 4)) != 0) {tResult1 |= bit( 0); tResult2 |= bit(24);}

			if ((tKey & bit( 5)) != 0) {tResult1 |= bit( 9); tResult2 |= bit(15);}
			if ((tKey & bit( 6)) != 0) {tResult1 |= bit( 8); tResult2 |= bit(16);}
			if ((tKey & bit( 7)) != 0) {tResult1 |= bit( 7); tResult2 |= bit(17);}
			if ((tKey & bit( 8)) != 0) {tResult1 |= bit( 6); tResult2 |= bit(18);}
			if ((tKey & bit( 9)) != 0) {tResult1 |= bit( 5); tResult2 |= bit(19);}

			if ((tKey & bit(10)) != 0) {tResult1 |= bit(14); tResult2 |= bit(10);}
			if ((tKey & bit(11)) != 0) {tResult1 |= bit(13); tResult2 |= bit(11);}
			if ((tKey & bit(12)) != 0) {tResult1 |= bit(12); tResult2 |= bit(12);}
			if ((tKey & bit(13)) != 0) {tResult1 |= bit(11); tResult2 |= bit(13);}
			if ((tKey & bit(14)) != 0) {tResult1 |= bit(10); tResult2 |= bit(14);}

			if ((tKey & bit(15)) != 0) {tResult1 |= bit(19); tResult2 |= bit( 5);}
			if ((tKey & bit(16)) != 0) {tResult1 |= bit(18); tResult2 |= bit( 6);}
			if ((tKey & bit(17)) != 0) {tResult1 |= bit(17); tResult2 |= bit( 7);}
			if ((tKey & bit(18)) != 0) {tResult1 |= bit(16); tResult2 |= bit( 8);}
			if ((tKey & bit(19)) != 0) {tResult1 |= bit(15); tResult2 |= bit( 9);}

			if ((tKey & bit(20)) != 0) {tResult1 |= bit(24); tResult2 |= bit( 0);}
			if ((tKey & bit(21)) != 0) {tResult1 |= bit(23); tResult2 |= bit( 1);}
			if ((tKey & bit(22)) != 0) {tResult1 |= bit(22); tResult2 |= bit( 2);}
			if ((tKey & bit(23)) != 0) {tResult1 |= bit(21); tResult2 |= bit( 3);}
			if ((tKey & bit(24)) != 0) {tResult1 |= bit(20); tResult2 |= bit( 4);}

			MOLD_RECIPES.put(tResult1, tEntry.getValue());
			MOLD_RECIPES.put(tResult2, tEntry.getValue());
		}
	}

	/** The upstream :79-83 lookup — the nugget fallback answers every unknown non-zero shape. */
	@Nullable
	public static OreDictPrefix getMoldRecipe(int aShape) {
		if (aShape == 0) return null;
		if (MOLD_RECIPES.isEmpty()) { fillMoldRecipes(); fillMoldRecipesUniverse(); }
		OreDictPrefix rRecipe = MOLD_RECIPES.get(aShape & SHAPE_MASK);
		return rRecipe == null ? OP.nugget : rRecipe;
	}

	// public like the family BEs (the GTGeneratorSolidBlockEntity field face) — the
	// command/stat readers and the offline tests consume them directly
	public int mShape = 0;
	public long mTemperature = DEF_ENV_TEMP;
	@Nullable
	public OreDictMaterialStack mContent = null;

	/** The output slot (upstream slot 0, :199/:296). */
	public final GTItemStackAdapter mInventory = new GTItemStackAdapter();

	/**
	 * The BET-injecting ctor (the GTGeneratorSolidBlockEntity form; the offline fixtures
	 * pass a synthetic type over a vanilla block state).
	 */
	public TileEntityMold(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(true, aType != null ? aType : GT6Molds.MOLD_BE.get(), aPos, aState);
		if (getBlockState().getBlock() instanceof GT6Molds.MoldBlock tBlock && tBlock.row().preCarvedShape() != 0) {
			mShape = tBlock.row().preCarvedShape(); // the pre-carved stone mold (class doc deviation)
		}
	}

	/** BET factory for BlockEntityType.Builder.of — resolves the registry type at runtime (the GTBarrelBlockEntity form: the self-reference stays out of the registry initializer). */
	public TileEntityMold(BlockPos aPos, BlockState aState) {
		this(null, aPos, aState);
	}

	@Override
	public String getTileEntityName() {
		return "gt6.mold";
	}

	/** The shell material (the :230 mMaterial) — rides the block carrier. */
	@Nullable
	public OreDictMaterial material() {
		BlockState tState = getBlockState();
		if (tState.getBlock() instanceof GT6Molds.MoldBlock tBlock) return tBlock.row().material();
		return null;
	}

	/** Upstream :229-231 — the mold ceiling. */
	@Override
	public long getMoldMaxTemperature() {
		OreDictMaterial tMaterial = material();
		return (long)((tMaterial == null ? MT.Stone : tMaterial).mMeltingPoint * HEAT_RESISTANCE_BONUS);
	}

	// ------------------------------------------------------------------------------------
	// the tick (upstream onServerTickPost :156-211)
	// ------------------------------------------------------------------------------------

	@Override
	public void onTick(long aTimer, boolean aIsServerSide) {
		if (!aIsServerSide) return;
		long tEnvTemperature = envTemp();

		// :160 — the ±5 K drift toward the environment
		if (mTemperature > tEnvTemperature) mTemperature -= Math.min(5, mTemperature - tEnvTemperature);
		else if (mTemperature < tEnvTemperature) mTemperature += Math.min(5, tEnvTemperature - mTemperature);

		// :162-167 — the empty-content reset (the display census is the card-B render face)
		if (mContent != null && mContent.mAmount <= 0 && mInventory.isEmpty()) {
			mContent = null;
		}

		// :169-176 — the auto-pull: wrench-set sides ask the adjacent crucible every 20 ticks
		if (mContent == null && mAutoPullDirections != 0 && aTimer % 20 == 5 && (!mUseRedstone || redstoneIncoming())) {
			for (Direction tSide : Direction.values()) {
				byte tSideByte = (byte)tSide.get3DDataValue();
				if ((mAutoPullDirections & (1 << tSideByte)) != 0) { // the FACE_CONNECTED[tSide][mAutoPullDirections] gate
					BlockEntity tNeighbor = hasLevel() ? getLevel().getBlockEntity(getBlockPos().relative(tSide)) : null;
					if (tNeighbor instanceof ITileEntityCrucible tCrucible) {
						byte tSideOfCrucible = (byte)tSide.getOpposite().get3DDataValue();
						if (tCrucible.fillMoldAtSide(this, tSideOfCrucible, tSideByte)) break; // :173
					}
				}
			}
		}

		if (mContent != null) {
			// :179-186 — the over-heat destruction (boiling content or shell ceiling) → lava
			if (mTemperature > mContent.mMaterial.mBoilingPoint || mTemperature > getMoldMaxTemperature()) {
				fizz();
				mContent = null;
				mInventory.clear();
				if (hasLevel()) getLevel().setBlock(getBlockPos(), Blocks.LAVA.defaultBlockState(), Block.UPDATE_ALL);
				return;
			}
			// :189-203 — the solidifying gate: below the melting point the shape pours out
			if (mTemperature < mContent.mMaterial.mMeltingPoint) {
				mContent.mMaterial = mContent.mMaterial.mTargetSolidifying.mMaterial;
				if (mContent.mAmount > 0 && mInventory.isEmpty()) {
					OreDictPrefix tPrefix = getMoldRecipe(mShape);
					// :194-197 — the COOL2CRYSTAL crystalline swap
					tPrefix = cool2CrystalSwap(tPrefix, mContent.mMaterial);
					if (tPrefix != null) {
						ItemStack tOutput = GT6RecipeMapCrucible.matStack(tPrefix, mContent.mMaterial, mContent.mAmount / tPrefix.mAmount); // :199
						if (tOutput != null) {
							mInventory.set(tOutput);
							mContent.mAmount = 0; // :200
						}
					}
				}
			}
		}
	}

	/** The SFX fizz arm (the vanilla LevelEvent bridge, the Smeltery form). */
	protected void fizz() {
		if (hasLevel()) getLevel().levelEvent(1501, getBlockPos(), 0);
	}

	// ------------------------------------------------------------------------------------
	// the mold face (ITileEntityMold, upstream :223-264)
	// ------------------------------------------------------------------------------------

	@Override
	public boolean isMoldInputSide(byte aSide) {
		return aSide == SIDE_TOP || (aSide >= 2 && aSide <= 5); // :224-226 SIDES_TOP_HORIZONTAL
	}

	@Override
	public long getMoldRequiredMaterialUnits() {
		OreDictPrefix tPrefix = getMoldRecipe(mShape);
		if (tPrefix == null) return 0;
		if (tPrefix == OP.nugget) { // :237-241 — the per-bit nugget census
			long rAmount = 0;
			for (int i = 0; i < 25; i++) if ((mShape & (1 << i)) != 0) rAmount += CS.U9;
			return rAmount;
		}
		return tPrefix.mAmount; // :242
	}

	@Override
	public long fillMold(OreDictMaterialStack aMaterial, long aTemperature, byte aSide) {
		if (aMaterial == null || aMaterial.mMaterial == null || aMaterial.mMaterial.contains(gregapi.data.TD.Properties.ACID)) return 0; // :247
		OreDictPrefix tPrefix = getMoldRecipe(mShape);
		tPrefix = cool2CrystalSwap(tPrefix, aMaterial.mMaterial.mTargetSolidifying.mMaterial); // :250-253 — before the representable gate
		if (tPrefix != null && mContent == null && mInventory.isEmpty() && isMoldInputSide(aSide) && aMaterial.mAmount > 0) { // :249
			if (GT6RecipeMapCrucible.matStack(tPrefix, aMaterial.mMaterial.mTargetSolidifying.mMaterial, 1) == null) return 0; // :254 the representable-output gate
			long tRequiredAmount = getMoldRequiredMaterialUnits();
			long rAmount = CruciblePhysics.units(tRequiredAmount, CS.U, aMaterial.mMaterial.mTargetSolidifying.mAmount, true); // :255
			if (aMaterial.mAmount >= rAmount) {
				mContent = new OreDictMaterialStack(aMaterial.mMaterial, tRequiredAmount); // :257
				mTemperature = aTemperature; // :258
				return rAmount;
			}
		}
		return 0;
	}

	// ------------------------------------------------------------------------------------
	// card B: the COOL2CRYSTAL swap + the wrench state machine (upstream :194-197/:250-253/:337-355)
	// ------------------------------------------------------------------------------------

	/**
	 * The :250-253 swap — a {@code COOL2CRYSTAL} material cools into its crystalline
	 * form: the plate shape answers the gem plate, the tiny plate the tiny gem plate.
	 * Null passes through (the callers re-check).
	 */
	@Nullable
	public static OreDictPrefix cool2CrystalSwap(@Nullable OreDictPrefix aPrefix, OreDictMaterial aSolidifying) {
		if (aPrefix != null && aSolidifying.contains(TD.Processing.COOL2CRYSTAL)) {
			if (aPrefix == OP.plate    ) return OP.plateGem;
			if (aPrefix == OP.plateTiny) return OP.plateGemTiny;
		}
		return aPrefix;
	}

	/** Upstream {@code hasRedstoneIncoming()} — the vanilla all-sides signal check. */
	public boolean redstoneIncoming() {
		return hasLevel() && getLevel().hasNeighborSignal(getBlockPos());
	}

	/**
	 * The monkey wrench (:343-355) — the top face splits into sub-sides: a HORIZONTAL
	 * sub-side toggles that auto-pull direction ({@code mAutoPullDirections ^= SBIT}),
	 * a vertical (UP/DOWN) sub-side toggles the redstone mode. Returns the chat report.
	 *
	 * @param aSubSide the GT6 side byte of the clicked sub-area (0-5, the port side order)
	 */
	public String toolMonkeyWrench(byte aSubSide) {
		Direction tSub = Direction.from3DDataValue(aSubSide);
		if (tSub.getAxis() != Direction.Axis.Y) { // SIDES_HORIZONTAL
			mAutoPullDirections ^= (1 << aSubSide);
			setChanged();
			return (mAutoPullDirections & (1 << aSubSide)) != 0 ? "Crucible Auto-Input: ON" : "Crucible Auto-Input: OFF";
		}
		mUseRedstone = !mUseRedstone;
		setChanged();
		return mUseRedstone ? "Crucible Auto-Input: REDSTONE" + (mAutoPullDirections == 0 ? " (WARNING: No Direction Selected!)" : "")
				: "Crucible Auto-Input: NO REDSTONE";
	}

	/** The soft hammer (:337-342) — auto-pull and redstone mode reset. Returns the chat report. */
	public String toolSoftHammer() {
		mUseRedstone = false;
		mAutoPullDirections = 0;
		setChanged();
		return "Crucible Auto-Input: OFF & NO REDSTONE";
	}

	// ------------------------------------------------------------------------------------
	// the temperature face (upstream :213-221)
	// ------------------------------------------------------------------------------------

	@Override
	public long getTemperatureValue(byte aSide) {
		return mTemperature;
	}

	@Override
	public long getTemperatureMax(byte aSide) {
		return getMoldMaxTemperature();
	}

	/** The {@link ITileEntityTemperature.EnvTemp} live form (the Smeltery formula, WD.envTemp :404-406). */
	public long envTemp() {
		if (!hasLevel()) return DEF_ENV_TEMP;
		BlockPos tPos = getBlockPos();
		float tBiomeTemp = getLevel().getBiome(tPos).value().getBaseTemperature();
		return Math.max(1, 273 - 3 + (long)(tBiomeTemp * 20));
	}

	// ------------------------------------------------------------------------------------
	// the world interaction (upstream onBlockActivated3 :267-294 + pickUpItem :296-322)
	// ------------------------------------------------------------------------------------

	/**
	 * The top-face click: an output item hands over (the :305 burn damage is the entity
	 * pool), an empty-handed empty mold drives the adjacent-crucible pour (:271-289).
	 */
	public boolean useTop(Player aPlayer, InteractionHand aHand) {
		if (!isServerSide() || aPlayer == null) return true;

		// :296-322 — the output hand-over
		ItemStack tOutput = mInventory.get();
		if (!tOutput.isEmpty()) {
			if (aPlayer.getInventory().add(tOutput.copy())) mInventory.clear();
			return true;
		}

		// :271-289 — the pour: scan the mold's input sides for a crucible and pull from it
		BlockPos tPos = getBlockPos();
		for (Direction tSide : Direction.values()) {
			if (tSide == Direction.DOWN) continue; // SIDES_TOP_HORIZONTAL: up + 4 horizontals
			BlockEntity tNeighbor = getLevel().getBlockEntity(tPos.relative(tSide));
			if (tNeighbor instanceof ITileEntityCrucible tCrucible) {
				byte tSideOfCrucible = (byte)tSide.getOpposite().get3DDataValue();
				byte tSideOfMold = (byte)tSide.get3DDataValue();
				tCrucible.fillMoldAtSide(this, tSideOfCrucible, tSideOfMold); // :276/:285
				return true;
			}
		}
		return true; // :291 — the top click is always consumed
	}

	// ------------------------------------------------------------------------------------
	// NBT (upstream readFromNBT2 :92-111)
	// ------------------------------------------------------------------------------------

	@Override
	protected void saveAdditional(CompoundTag aNBT) {
		super.saveAdditional(aNBT);
		aNBT.putInt(NBT_MOLD, mShape); // :109
		aNBT.putLong(NBT_TEMPERATURE, mTemperature); // :108 (UT.NBT.setNumber — the long form)
		aNBT.putByte(NBT_CONNECTION, mAutoPullDirections); // :106
		aNBT.putBoolean(NBT_MODE, mUseRedstone); // :107
		if (mContent != null) MaterialStackNBT.save(mContent, aNBT); // :110 NBT_MATERIALS
		//? if forge {
		aNBT.put("gt.inv", mInventory.serializeNBT());
		//?} else {
		/*aNBT.put("gt.inv", mInventory.serializeNBT(NBT_ACCESS)); // 21.1: ItemStackHandler NBT takes the registries
		 *///?}
	}

	@Override
	public void load(CompoundTag aNBT) {
		super.load(aNBT);
		if (aNBT.contains(NBT_MOLD, Tag.TAG_ANY_NUMERIC)) mShape = aNBT.getInt(NBT_MOLD); // :95
		if (aNBT.contains(NBT_TEMPERATURE, Tag.TAG_ANY_NUMERIC)) mTemperature = aNBT.getLong(NBT_TEMPERATURE); // :99
		if (aNBT.contains(NBT_CONNECTION, Tag.TAG_ANY_NUMERIC)) mAutoPullDirections = aNBT.getByte(NBT_CONNECTION); // :98
		if (aNBT.contains(NBT_MODE, Tag.TAG_ANY_NUMERIC)) mUseRedstone = aNBT.getBoolean(NBT_MODE); // :96
		mContent = MaterialStackNBT.load(aNBT); // :100
		if (mContent != null && mContent.mAmount <= 0) mContent = null;
		//? if forge {
		if (aNBT.contains("gt.inv", Tag.TAG_COMPOUND)) mInventory.deserializeNBT(aNBT.getCompound("gt.inv"));
		//?} else {
		/*if (aNBT.contains("gt.inv", Tag.TAG_COMPOUND)) mInventory.deserializeNBT(NBT_ACCESS, aNBT.getCompound("gt.inv")); // 21.1: provider-first
		 *///?}
	}

	// ------------------------------------------------------------------------------------
	// the tiny slot adapter (the mold has ONE output slot; the ItemStackHandler form)
	// ------------------------------------------------------------------------------------

	/** The one-slot adapter over ItemStackHandler semantics (isEmpty/set/clear). */
	public static final class GTItemStackAdapter extends net.minecraftforge.items.ItemStackHandler {
		public GTItemStackAdapter() {
			super(1);
		}

		public boolean isEmpty() {
			return getStackInSlot(0).isEmpty();
		}

		public ItemStack get() {
			return getStackInSlot(0);
		}

		public void set(ItemStack aStack) {
			setStackInSlot(0, aStack);
		}

		public void clear() {
			setStackInSlot(0, ItemStack.EMPTY);
		}
	}
}
