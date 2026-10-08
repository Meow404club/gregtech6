package gregtech6.block.decor;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import gregapi.oredict.OreDictMaterial;

/**
 * One GT6 SPIKE block — the five {@code gt.block.spikes.*} families (Loader_Blocks
 * .java:94-98, task material-mc-g2-decor-misc), the per-material split of the upstream
 * (orientation 3 bits) x (material 1 bit) meta ladder into TEN material rows (the P8 ADR
 * ④ per-variant item identity ruling — the crafted wall/block/falling spike items are
 * distinct stacks, BlockBaseSpike.java:65-74). Three items ride ONE block per material:
 * the wall item places the 6-way {@code FACING} (the :104 OPOS[clicked-face] mapping),
 * the block item sets {@code OMNI}, the falling item sets {@code FALLING} (the meta 6/7
 * faces); the falling arm re-uses the vanilla falling machinery ({@link FallingBlock},
 * gated on the property — the upstream useGravity(meta 7) face, CS BlockBase).
 *
 * <p>Numbers: hardness 30 / resistance 5 (BlockBaseSpike.java:112-113), metal sound,
 * pickaxe tool (:107-108, the level face rides the unset requiresCorrectTool red line),
 * light opacity NONE (:109). The collision box (:155-165) is the 0.6-thick slab on the
 * FACING half (the mount-side body the 1.7.10 collision graph holds); the full cube stays
 * the selection/outline shape (:173). Items and XP orbs pass through (:168-171) — the
 * vanilla shape-based route approximates by the slab shape (items ride the top surface).
 *
 * <p>THE DAMAGE FACE (the five subclass {@code onEntityCollidedWithBlock} rows): contact
 * damage via the vanilla cactus pairing ({@code entityInside} + {@code stepOn}, the
 * boundary-inclusive 1.7.10 collision semantics), {@code TFC_DAMAGE_MULTIPLIER} x the
 * family table — sharp 5/2.5 wall-omni (x2 for the second material), steel 8/4, super
 * 15/7.5 (x3.33 for adamantium), metal and fancy 20/10 vs their prey arm / 2/1 otherwise
 * (the prey arms: slimes for copper, arthropods for lead, undead for gold, ender/were
 * for silver; the immunity lists ride the skeleton/slime/golem/enderman checks). The
 * damage source is the vanilla cactus type with the death-message override — the AF
 * LEGO face (DamageSourceSpike.java:40 "stepped on a LEGO!", the GT6Calendars dormant
 * declaration, lands HERE) answers the fool line while APRIL_FOOLS is up; Wither/Dragon
 * destroy-gates (:canEntityDestroy arms) map onto the hurt-gate (the super-spikes refuse
 * to hurt their boss — the canEntityDestroy semantics protect the BLOCK from the boss's
 * break pass, the 1.20.1 analogue lives in the loot/hurt gate: declared simplified).
 *
 * <p>Render: the material smooth face (the grayscale blockSolid PNG x fRGBaSolid via
 * GT6DecorTintListener) over a static 3-tier spike silhouette rotated per FACING — the
 * 13-pass per-face renderers (SpikeRendererXPos et al) and the AF rainbow cycle
 * (BlockBaseSpike.java:193/199/207) are the render-pool defer (declared in the card).
 */
public class GT6SpikeBlock extends FallingBlock {

	/** The display templates — the material unit fills the %s slot (the dump compose). */
	public static final String WALL_NAME_KEY = "gt6.spike.wall";
	public static final String OMNI_NAME_KEY = "gt6.spike.omni";
	public static final String FALLING_NAME_KEY = "gt6.spike.falling";

	/** The spike orientation (the upstream meta&7 0-5 wall band). */
	public static final DirectionProperty FACING = BlockStateProperties.FACING;
	/** The meta 6 face (the omni block spike). */
	public static final BooleanProperty OMNI = BooleanProperty.create("omni");
	/** The meta 7 face (the falling spike block). */
	public static final BooleanProperty FALLING = BooleanProperty.create("falling");

	/** The collision shapes per facing — the 0.6-thick slab on the facing half (:155-162). */
	private static final VoxelShape[] SHAPES = {
			Shapes.block(), // dummy for the null index
			Shapes.box(0.0, 0.0, 0.0, 1.0, 0.6, 1.0), // DOWN-pointing?? see shapeOf
			Shapes.box(0.0, 0.4, 0.0, 1.0, 1.0, 1.0),
			Shapes.box(0.0, 0.0, 0.0, 1.0, 1.0, 0.6),
			Shapes.box(0.0, 0.0, 0.4, 1.0, 1.0, 1.0),
			Shapes.box(0.0, 0.0, 0.0, 0.6, 1.0, 1.0),
			Shapes.box(0.4, 0.0, 0.0, 1.0, 1.0, 1.0),
			Shapes.box(0.125, 0.125, 0.125, 0.875, 0.875, 0.875)};

	/** The row material (the tint anchor). */
	public final OreDictMaterial material;
	/** The registry snake id ({@code spike_steel} form). */
	public final String snake;
	/** The damage table (wall, omni) in half-hearts — the family subclass faces verbatim. */
	public final float wallDamage, omniDamage;
	/** The prey gate (the fancy/metal arms): null = hurt everything in the family table. */
	public final PreyPredicate prey;
	/** The immunity gate (the golem/skeleton/slime instance checks). */
	public final ImmunityPredicate immune;

	public GT6SpikeBlock(String aSnake, OreDictMaterial aMaterial, float aWallDamage, float aOmniDamage,
			@Nullable PreyPredicate aPrey, @Nullable ImmunityPredicate aImmune, BlockBehaviour.Properties aProperties) {
		super(aProperties);
		this.snake = aSnake;
		this.material = aMaterial;
		this.wallDamage = aWallDamage;
		this.omniDamage = aOmniDamage;
		this.prey = aPrey;
		this.immune = aImmune;
		registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.UP).setValue(OMNI, Boolean.FALSE).setValue(FALLING, Boolean.FALSE));
	}

	/** The upstream numbers (BlockBaseSpike.java:112-113) — hardness 30, resistance 5. */
	public static BlockBehaviour.Properties spikeProperties() {
		return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(30.0F, 5.0F)
				.sound(SoundType.METAL).noOcclusion();
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> aBuilder) {
		aBuilder.add(FACING, OMNI, FALLING);
	}

	//? if neoforge {
	/*
	// 21.1 made Block.codec() abstract (the vanilla 1.21 block-state codec dispatch, the
	// GTOvenBlock fold): the simpleCodec representative-value form — a parse-time default
	// carrying no live config (the Steel row), world save/load never runs through it.
	@Override
	protected com.mojang.serialization.MapCodec<? extends GT6SpikeBlock> codec() {
		return simpleCodec(aProperties -> new GT6SpikeBlock("spike_steel", gregapi.data.MT.Steel,
				5.0F, 2.5F, null, null, aProperties));
	}
	 *///?}

	/** The composed display name — the three item identities over the material unit. */
	public MutableComponent nameOf(boolean aOmni, boolean aFalling) {
		return net.minecraft.network.chat.Component.translatable(
				aFalling ? FALLING_NAME_KEY : aOmni ? OMNI_NAME_KEY : WALL_NAME_KEY,
				gregtech6.item.MaterialPrefixItem.materialFill(this.material));
	}

	/** The collision box per state (:155-165): the facing-half slab, the omni core box. */
	@Override
	public VoxelShape getCollisionShape(BlockState aState, BlockGetter aLevel, BlockPos aPos, CollisionContext aContext) {
		return shapeOf(aState);
	}

	@Override
	public VoxelShape getShape(BlockState aState, BlockGetter aLevel, BlockPos aPos, CollisionContext aContext) {
		return Shapes.block(); // the full-cube selection outline (:173)
	}

	private static VoxelShape shapeOf(BlockState aState) {
		if (aState.getValue(OMNI) || aState.getValue(FALLING)) return SHAPES[7];
		return switch (aState.getValue(FACING)) {
			case UP -> SHAPES[1]; // points up, body on the floor half
			case DOWN -> SHAPES[2];
			case NORTH -> SHAPES[3];
			case SOUTH -> SHAPES[4];
			case WEST -> SHAPES[5];
			case EAST -> SHAPES[6];
		};
	}

	/** The damage face — the cactus pairing covers the boundary-inclusive 1.7.10 collision. */
	@Override
	public void entityInside(BlockState aState, Level aLevel, BlockPos aPos, Entity aEntity) {
		hurt(aLevel, aState, aPos, aEntity);
	}

	/** The walk faces — the upstream pair: the onWalkOver x0.1 crawl-over slow when the
	 * spike does not point up (:103 {@code (meta & 7) != SIDE_UP}) and the cactus-pairing
	 * contact damage (:107-171 the collided rows). */
	@Override
	public void stepOn(Level aLevel, BlockPos aPos, BlockState aState, Entity aEntity) {
		if (aState.getValue(FACING) != Direction.UP) {
			aEntity.setDeltaMovement(aEntity.getDeltaMovement().x * 0.1, aEntity.getDeltaMovement().y,
					aEntity.getDeltaMovement().z * 0.1);
		}
		hurt(aLevel, aState, aPos, aEntity);
	}

	/** The family damage table (+ the AF LEGO death message, DamageSourceSpike.java:40). */
	private void hurt(Level aLevel, BlockState aState, BlockPos aPos, Entity aEntity) {
		if (!(aEntity instanceof LivingEntity tLiving)) return;
		boolean tOmni = aState.getValue(OMNI) || aState.getValue(FALLING);
		float tDamage = tOmni ? this.omniDamage : this.wallDamage;
		if (this.prey != null && !this.prey.matches(tLiving)) tDamage /= 10.0F; // the 2/1 vs 20/10 arms
		if (this.immune != null && this.immune.immune(tLiving)) return;
		if (tDamage <= 0) return;
		aEntity.hurt(spikeSource(aLevel, aState), tDamage);
	}

	/** The spike damage source over the vanilla cactus type + the AF LEGO message fold. */
	private static DamageSource spikeSource(Level aLevel, BlockState aState) {
		DamageSource tBase = aLevel.damageSources().cactus();
		return new SpikeSource(tBase, ((GT6SpikeBlock) aState.getBlock()));
	}

	/** The prey arms (the fancy/metal subclass rows): full damage vs the prey, 1/10 otherwise. */
	@FunctionalInterface
	public interface PreyPredicate {
		boolean matches(LivingEntity aEntity);
	}

	/** The immunity arms (the golem/skeleton/slime instance rows). */
	@FunctionalInterface
	public interface ImmunityPredicate {
		boolean immune(LivingEntity aEntity);
	}

	/** The message-bearing source — the AF LEGO face of DamageSourceSpike.java:40. */
	private static final class SpikeSource extends DamageSource {

		private final GT6SpikeBlock mBlock;

		SpikeSource(DamageSource aBase, GT6SpikeBlock aBlock) {
			super(aBase.typeHolder(), aBase.getDirectEntity(), aBase.getEntity());
			mBlock = aBlock;
		}

		@Override
		public MutableComponent getLocalizedDeathMessage(LivingEntity aVictim) {
			if (gregtech6.easter.GT6Calendars.APRIL_FOOLS) {
				return net.minecraft.network.chat.Component.translatable("gt6.death.spike.lego", aVictim.getDisplayName());
			}
			return net.minecraft.network.chat.Component.translatable("gt6.death.spike", aVictim.getDisplayName());
		}
	}

	// ---- the falling arm (the meta 7 face) -----------------------------------------------

	/** The vanilla falling machinery, gated on the FALLING property (the useGravity face). */
	@Override
	public void tick(BlockState aState, net.minecraft.server.level.ServerLevel aLevel, BlockPos aPos, RandomSource aRandom) {
		if (!aState.getValue(FALLING)) return;
		super.tick(aState, aLevel, aPos, aRandom);
	}

	@Override
	public void onPlace(BlockState aState, Level aLevel, BlockPos aPos, BlockState aOldState, boolean aMoved) {
		if (!aState.getValue(FALLING)) return;
		super.onPlace(aState, aLevel, aPos, aOldState, aMoved);
	}

	/** The wall item placement — FACING follows the clicked face (:104 OPOS[clicked]). */
	public static class WallItem extends BlockItem {
		public WallItem(Block aBlock, Properties aProperties) { super(aBlock, aProperties); }

		@Nullable
		@Override
		protected BlockState getPlacementState(BlockPlaceContext aContext) {
			BlockState tState = super.getPlacementState(aContext);
			return tState == null ? null : tState.setValue(FACING, aContext.getClickedFace());
		}
	}

	/** The omni item (the meta 6 identity). */
	public static class OmniItem extends BlockItem {
		public OmniItem(Block aBlock, Properties aProperties) { super(aBlock, aProperties); }

		@Nullable
		@Override
		protected BlockState getPlacementState(BlockPlaceContext aContext) {
			BlockState tState = super.getPlacementState(aContext);
			return tState == null ? null : tState.setValue(OMNI, Boolean.TRUE).setValue(FALLING, Boolean.FALSE);
		}
	}

	/** The falling item (the meta 7 identity). */
	public static class FallingItem extends BlockItem {
		public FallingItem(Block aBlock, Properties aProperties) { super(aBlock, aProperties); }

		@Nullable
		@Override
		protected BlockState getPlacementState(BlockPlaceContext aContext) {
			BlockState tState = super.getPlacementState(aContext);
			return tState == null ? null : tState.setValue(FALLING, Boolean.TRUE).setValue(OMNI, Boolean.FALSE);
		}
	}
}
