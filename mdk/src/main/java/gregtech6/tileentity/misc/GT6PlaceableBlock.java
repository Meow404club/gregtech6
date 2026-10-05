package gregtech6.tileentity.misc;


import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import gregtech6.block.GTEntityBlock;
import gregtech6.block.surface.GT6RockLines;
import gregtech6.items.tools.GTMagnifyingGlassItem;
import gregtech6.registry.GT6Placeables;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;

/**
 * The placed-pile block carrier (task placeables) — the six "Untyped" placement faces
 * of Loader_MultiTileEntities.java:2033-2040 over the {@code MultiTileEntityPlaceable}
 * base semantics (gregapi/tileentity/misc/MultiTileEntityPlaceable.java). One class, six
 * registrations, the {@link Kind} carrying the differences (the upstream class split was
 * shape + item identity only).
 *
 * <p>Verbatim folds: the click economy (base :77-113) — an EQUAL held stack merges into
 * the pile up to the stack max, ANY other click gives one back and the empty pile goes to
 * air ({@code setToAir}); the drops (base :72-74 {@code getDrops → mStack}) = the
 * playerDestroy loot shell (the GT6BumbleHiveBlock form, noLootTable); the base
 * hardness 0.25 (:138); non-solid faces (:129-131) → the never-full shapes below; no
 * collision (base :132 collision null on the sub-8 piles — kept shape-less for all six,
 * they are floor deco). Declared cuts: the fixed per-kind height (the upstream ingot pile
 * grows {@code divup(mSize,8)/8} tall with the stack — the model band is one fixed box,
 * the render pool), the placement NBT support/liquid-neighbour checks (RockPlaced
 * :151-163 — the pile stays until mined), and the explosion drop face (loot-table-less
 * destruction voids the pile, the playerDestroy shell only covers harvests).
 */
public class GT6PlaceableBlock extends GTEntityBlock {

	/** The pile family (the upstream six-class split compressed). */
	public enum Kind {
		/** The ingot pile (upstream 32084; whole held stack consumed, base :346 whole-stack NBT). */
		INGOT(Box.FLAT_2, true, "placeable/ingot_sides", "placeable/ingot_top", SoundType.METAL),
		/** The plate pile (upstream 32085). */
		PLATE(Box.FLAT_1, true, "placeable/plate_sides", "placeable/plate_top", SoundType.METAL),
		/** The gem-plate pile (upstream 32086). */
		GEM_PLATE(Box.FLAT_1, true, "placeable/plateGem_sides", "placeable/plateGem_top", SoundType.STONE),
		/** The scrap pile (upstream 32103). */
		SCRAP(Box.FLAT_1, true, "placeable/scrap_sides", "placeable/scrap_top", SoundType.METAL),
		/** The placed rock (upstream 32074 RockPlaced; one item consumed, vanilla stone borrow). */
		ROCK(Box.PEBBLE, false, "block/stone", "block/stone", SoundType.STONE),
		/** The placed stick (upstream 32073 StickPlaced; one item consumed, vanilla log borrow). */
		STICK(Box.MATCH_2, false, "block/oak_log", "block/oak_log", SoundType.WOOD);

		/** The fixed silhouette (the declared render cut: height does not track the stack). */
		public enum Box {
			/** The 2/16 ingot layer (the upstream single ingot layer height). */
			FLAT_2(Block.box(0, 0, 0, 16, 2, 16)),
			/** The 1/16 plate layer. */
			FLAT_1(Block.box(0, 0, 0, 16, 1, 16)),
			/** The 3/16 pebble (the W6 surface-rock silhouette, GT6SurfaceRockBlock.SHAPE_DOWN). */
			PEBBLE(Block.box(2, 0, 2, 14, 3, 14)),
			/** The 2/16 inset match (the W6 surface-stick silhouette). */
			MATCH_2(Block.box(2, 0, 2, 14, 2, 14));

			public final VoxelShape shape;

			Box(VoxelShape aShape) {
				this.shape = aShape;
			}
		}

		public final Box box;
		/** Whether the placement face consumes the WHOLE held stack (ingot/plate/plategem/scrap, base :346-:372). */
		public final boolean wholeStack;
		/** The side/texture refs (model-space; vanilla borrows carry the minecraft: namespace). */
		public final String sidesTexture;
		public final String topTexture;
		public final SoundType sound;

		Kind(Box aBox, boolean aWholeStack, String aSidesTexture, String aTopTexture, SoundType aSound) {
			box = aBox;
			wholeStack = aWholeStack;
			sidesTexture = aSidesTexture;
			topTexture = aTopTexture;
			sound = aSound;
		}
	}

	private final Kind mKind;

	public GT6PlaceableBlock(Kind aKind, Properties aProperties) {
		super(aProperties.sound(aKind.sound).strength(0.25F).noLootTable());
		mKind = aKind;
	}

	//? if neoforge {
	/*
	// 21.1 made BaseEntityBlock.codec() abstract — the kind rides the closure (the
	// representative-value form, the GTSensorBlock fork shape).
	@Override
	protected com.mojang.serialization.MapCodec<? extends GT6PlaceableBlock> codec() {
		return simpleCodec(aProperties -> new GT6PlaceableBlock(mKind, aProperties));
	}
	*///?}

	public Kind kind() {
		return mKind;
	}

	/** The vanilla BaseEntityBlock INVISIBLE default beaten back to MODEL (the GT6BumbleHiveBlock form). */
	@Override
	public RenderShape getRenderShape(BlockState aState) {
		return RenderShape.MODEL;
	}

	@Override
	protected BlockEntityType<? extends TileEntityBase03TicksAndSync> tickerType() {
		return GT6Placeables.PLACEABLE_BE.get();
	}

	@Override
	@SuppressWarnings("deprecation")
	public VoxelShape getShape(BlockState aState, BlockGetter aLevel, BlockPos aPos, CollisionContext aContext) {
		return mKind.box.shape;
	}

	/** Never collide (the upstream sub-8 piles carry null collision, base :132). */
	@Override
	@SuppressWarnings("deprecation")
	public VoxelShape getCollisionShape(BlockState aState, BlockGetter aLevel, BlockPos aPos, CollisionContext aContext) {
		return net.minecraft.world.phys.shapes.Shapes.empty();
	}

	/** The click economy (base :77-113): equal stack merges, other clicks give one back. */
	@Override
	//? if forge {
	public InteractionResult use(BlockState aState, Level aLevel, BlockPos aPos, Player aPlayer, InteractionHand aHand, BlockHitResult aHit) {
	//?} else {
	/*public InteractionResult useWithoutItem(BlockState aState, Level aLevel, BlockPos aPos, Player aPlayer, BlockHitResult aHit) {
	//21.1: BlockBehaviour.use folded into useWithoutItem — the InteractionHand param dropped
	//(the GT6SurfaceRockBlock fork precedent).
	 *///?}
		if (aLevel.isClientSide()) return InteractionResult.SUCCESS;
		if (!(aLevel.getBlockEntity(aPos) instanceof GT6PlaceableBlockEntity tPile)) return InteractionResult.PASS;
		//? if forge {
		ItemStack tHeld = aPlayer.getItemInHand(aHand);
		//?} else {
		/*ItemStack tHeld = aPlayer.getItemInHand(InteractionHand.MAIN_HAND); // the hand-less 21.1 form
		*///?}
		ItemStack tContents = tPile.stack();
		if (!tHeld.isEmpty() && ItemStack.isSameItemSameTags(tHeld, tContents) && !tContents.isEmpty()) {
			// the merge arm (base :81-96): up to the pile's max stack, the surplus STAYS in hand
			int tRoom = Math.min(tContents.getMaxStackSize(), 64) - tContents.getCount();
			if (tRoom > 0) {
				int tTake = Math.min(tRoom, tHeld.getCount());
				ItemStack tMerged = tContents.copy();
				tMerged.grow(tTake);
				tPile.setStack(tMerged);
				tHeld.shrink(tTake);
				return InteractionResult.CONSUME;
			}
			return InteractionResult.CONSUME;
		}
		// the magnifier census arm (task easter-s4-rock-lines — RockPlaced rides
		// MultiTileEntityRock.onToolClick :85-107): the non-sneak magnifier on a flint
		// pile SPEAKS (the :107 itemFlint row) instead of giving back; the sneak click
		// falls through to the give arm (the upstream :87-92 give). Only the flint row
		// is in census scope — the :108-137 material band is the deferred sibling, a
		// stone pile keeps the give arm.
		if (mKind == Kind.ROCK && !aPlayer.isShiftKeyDown() && !tHeld.isEmpty()
				&& tHeld.canPerformAction(GTMagnifyingGlassItem.ACTION) && tContents.is(Items.FLINT)) {
			// the GT6Calendars flags (the CS.java:870-872 compute-once rows — the landed S2
			// seam; the review-seat alignment of the cut-time local date trio)
			aPlayer.displayClientMessage(Component.literal(GT6RockLines.flintLine(aLevel.getRandom(),
					gregtech6.easter.GT6Calendars.APRIL_FOOLS, gregtech6.easter.GT6Calendars.WOODMANS_BDAY,
					gregtech6.easter.GT6Calendars.XMAS_IN_JULY)), false);
			return InteractionResult.SUCCESS;
		}
		// the give arm (base :98): one item back; the empty pile goes to air (:107 setToAir)
		if (!tContents.isEmpty()) {
			ItemStack tOne = tContents.copy();
			tOne.setCount(1);
			if (!aPlayer.getInventory().add(tOne)) {
				ItemEntity tDrop = new ItemEntity(aLevel, aPos.getX() + 0.5, aPos.getY() + 0.5, aPos.getZ() + 0.5, tOne);
				aLevel.addFreshEntity(tDrop);
			}
			ItemStack tRest = tContents.copy();
			tRest.shrink(1);
			if (tRest.isEmpty()) {
				aLevel.removeBlock(aPos, false);
			} else {
				tPile.setStack(tRest);
			}
			return InteractionResult.CONSUME;
		}
		aLevel.removeBlock(aPos, false);
		return InteractionResult.CONSUME;
	}

	/**
	 * The deferred placement confirm (task surface-rock-material-link): the BE-data packet
	 * races the section packet in vanilla ChunkHolder.broadcastChanges — the multi-change
	 * branch sends broadcastBlockEntityIfNeeded BEFORE the section update (ChunkHolder.java
	 * :226-227), and the client silently DROPS a payload that lands before the block exists
	 * (handleBlockEntityData getBlockEntity().ifPresent no-op, ClientPacketListener.java
	 * :1226-1238) — the pile then renders untinted (the "全部变成石头类型" report) and nothing
	 * ever resends. One delayed re-send after the placement lands on a client BE that EXISTS
	 * by then, so the retransmit loads — and the load arm re-bakes the tint. The GT6FoamFresh
	 * scheduleTick precedent; the no-level offline face is the sendClientData side guard.
	 */
	@Override
	public void tick(BlockState aState, ServerLevel aLevel, BlockPos aPos, RandomSource aRandom) {
		if (aLevel.getBlockEntity(aPos) instanceof GT6PlaceableBlockEntity tPile && !tPile.stack().isEmpty()) {
			tPile.sendClientData();
		}
	}

	/** The loot shell (base :72-74 getDrops → mStack): the contents ARE the drops, noLootTable. */
	@Override
	public void playerDestroy(Level aLevel, Player aPlayer, BlockPos aPos, BlockState aState,
			@Nullable BlockEntity aBlockEntity, ItemStack aTool) {
		super.playerDestroy(aLevel, aPlayer, aPos, aState, aBlockEntity, aTool);
		if (aLevel.isClientSide || !(aBlockEntity instanceof GT6PlaceableBlockEntity tPile)) return;
		ItemStack tContents = tPile.stack();
		if (!tContents.isEmpty()) {
			popResource(aLevel, aPos, tContents);
			tPile.setStack(ItemStack.EMPTY);
		}
	}
}
