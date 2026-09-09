package gregtech6.block.foam;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;

import gregtech6.registry.GT6FoamBlocks;

/**
 * Fresh (wet) C-Foam — the 1.20.1 counterpart of upstream {@code BlockCFoamFresh}
 * (gregtech/blocks/BlockCFoamFresh.java:45-128, the BlockColored 16-meta form flattened
 * onto the {@link #COLOR} blockstate property, the spec_rulings.ruling_color_dim form).
 *
 * <p>Upstream traits kept verbatim:
 * <ul>
 * <li>hardness 1.0 / resistance 0.0 / the cloth sound ({@code Material.sponge+soundTypeCloth},
 *     :47 — the registration numbers are the block ctor args directly);</li>
 * <li>passable — collision is EMPTY while the selection shape stays the full cube (:95-97
 *     {@code getCollisionBoundingBoxFromPool} null; the {@code noCollission()} property);</li>
 * <li>not opaque, does not block light (:75-77 + the sponge light opacity 0 →
 *     {@code noOcclusion().lightBlock(0)});</li>
 * <li>dries on placement: {@code onBlockAdded2} schedules {@code 100+RNGSUS.nextInt(5900)}
 *     (:60-62 — the {@link #onPlace} + {@link #tick} scheduled-tick pair, the delay over
 *     {@link #dryDelay}), and the tick calls {@link #dryFoam} (:64-67);</li>
 * <li>dries into the dried block WITH the colour preserved (:110-112 — the
 *     {@code WD.meta} pass-through becomes the {@link #COLOR} copy);</li>
 * <li>drops nothing (:70-72 — the datagen no-drop table);</li>
 * <li>the foam face: {@code applyFoam} constant false (:105-107), {@code hasFoam} true
 *     (:120-122), {@code driedFoam} false (:125-127), {@code removeFoam} → air (:115-117).</li>
 * </ul>
 *
 * <p>Declared deviations: the 16 upstream META items collapse to zero items (the fresh
 * block is an intermediate state, obtainable only through the spray — the upstream meta
 * ladder has no fresh item in the creative tab either); the slab form is the sibling
 * {@link GT6CFoamFreshSlabBlock} (the spec_rulings.ruling_slab vanilla SlabBlock ruling).
 */
public class GT6CFoamFreshBlock extends Block implements IBlockFoamable {

	/** The 16-colour dye dimension (upstream the BlockColored meta; 0=Black..15=White, the GT6 DYE order). */
	public static final IntegerProperty COLOR = IntegerProperty.create("color", 0, 15);

	/** The dry delay base (upstream :61 {@code scheduleBlockUpdate(..., 100+...)}). */
	public static final int DRY_DELAY_BASE = 100;

	/** The dry delay random span (upstream :61 {@code nextInt(5900)} — mean ~295 s). */
	public static final int DRY_DELAY_SPAN = 5900;

	/** The dried-block target, resolved at dry time (post-registration; the registry wires the id). */
	private final java.util.function.Supplier<Block> mDriedTarget;

	public GT6CFoamFreshBlock(BlockBehaviour.Properties aProperties, java.util.function.Supplier<Block> aDriedTarget) {
		super(aProperties);
		mDriedTarget = aDriedTarget;
		registerDefaultState(stateDefinition.any().setValue(COLOR, 0));
	}

	/** The registration properties — the package seam the offline tests assert over (the GTGrassBlock.grassProperties precedent). */
	public static BlockBehaviour.Properties freshProperties() {
		// upstream :47 verbatim numbers: hardness 1.0, resistance 0.0, sponge+cloth face,
		// the passable/not-opaque trio (:75-102/:95-97). The upstream light opacity 0 rides
		// the vanilla default path (noOcclusion → never solidRender → getLightBlock 0,
		// BlockBehaviour.java:255-259) — the Properties carrier has no opacity setter on
		// the 1.20.1 face, the explicit override is unnecessary.
		return BlockBehaviour.Properties.of()
				.mapColor(MapColor.WOOL)
				.strength(1.0F, 0.0F)
				.sound(SoundType.WOOL)
				.noOcclusion()
				.noCollission();
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> aBuilder) {
		aBuilder.add(COLOR);
	}

	/** The dry delay of upstream :61 — {@code 100 + nextInt(5900)}, the offline-testable seam ({@code RandomSource.create()} offline). */
	public static int dryDelay(RandomSource aRandom) {
		return DRY_DELAY_BASE + aRandom.nextInt(DRY_DELAY_SPAN);
	}

	/**
	 * Upstream {@code onBlockAdded2} (:60-62): a placed wet foam schedules its own drying.
	 * Fires on every placement route (the spray arm, /setblock, pistons).
	 */
	@Override
	public void onPlace(BlockState aState, Level aLevel, BlockPos aPos, BlockState aOldState, boolean aIsMoving) {
		super.onPlace(aState, aLevel, aPos, aOldState, aIsMoving);
		if (!aLevel.isClientSide) {
			aLevel.scheduleTick(aPos, this, dryDelay(aLevel.random));
		}
	}

	/**
	 * Upstream {@code updateTick2} (:64-67): the scheduled tick dries the foam. The public
	 * widening is the both-legs shape (the 1.20.1 BlockBehaviour.tick is public — BlockBehaviour
	 * .java:315; a public override is legal over the 1.21.1 protected form, the p24 pipe
	 * getDestroyProgress precedent).
	 */
	@Override
	public void tick(BlockState aState, ServerLevel aLevel, BlockPos aPos, RandomSource aRandom) {
		dryFoam(aLevel, aPos, null); // the SIDE_ANY carrier (upstream :66)
	}

	// -------------------------------------------------------------------------
	// the IBlockFoamable face (upstream :105-127)
	// -------------------------------------------------------------------------

	@Override
	public boolean applyFoam(Level aLevel, BlockPos aPos, @Nullable Direction aSide, int aRGB, int aDyeIndex) {
		return false; // upstream :105-107 verbatim — this block IS the foam
	}

	@Override
	public boolean dryFoam(Level aLevel, BlockPos aPos, @Nullable Direction aSide) {
		// upstream :110-112 — the dried target, the colour meta passed through. NOTE the
		// upstream SIDES_VALID[aSide] slab-shave arm of :111 is the REMOVER's face (card C):
		// the port's dryFoam keeps the side parameter for it, the natural/scheduled dry
		// (the only caller of this card) rides the SIDE_ANY null form → the full dried block.
		BlockState tDried = mDriedTarget.get().defaultBlockState()
				.setValue(COLOR, aLevel.getBlockState(aPos).getValue(COLOR));
		return aLevel.setBlock(aPos, tDried, 3);
	}

	@Override
	public boolean removeFoam(Level aLevel, BlockPos aPos, @Nullable Direction aSide) {
		aLevel.removeBlock(aPos, false); // upstream :115-117 setBlock NB, 3 — removeBlock is the (air, 3) form
		return true;
	}

	@Override
	public boolean hasFoam(Level aLevel, BlockPos aPos, @Nullable Direction aSide) {
		return true; // upstream :120-122
	}

	@Override
	public boolean driedFoam(Level aLevel, BlockPos aPos, @Nullable Direction aSide) {
		return false; // upstream :125-127
	}
}
