package gregtech6.items.behaviors;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import gregtech6.GT6Mod;
import gregtech6.item.MaterialPrefixItem;
import gregtech6.registry.GT6Placeables;
import gregtech6.tileentity.misc.GT6PlaceableBlock;
import gregtech6.tileentity.misc.GT6PlaceableBlockEntity;

/**
 * The unified placement behavior (task p32-placeables) — the port of the upstream
 * sneak-place dispatch: gregtech/GT_Proxy.java:294-313 (the PlayerInteractEvent walk —
 * vanilla stick / flint / the OP-prefixed material items) over the modern material item
 * system. The six faces: {@code OP.ingot → placed_ingot 32084}, {@code OP.plate →
 * placed_plate 32085}, {@code OP.plateGem → placed_gem_plate 32086}, {@code OP.scrapGt →
 * placed_scrap 32103}, {@code OP.rockGt → placed_rock 32074}, {@code OP.stick →
 * placed_stick 32073}; the vanilla {@code Items.STICK}/{@code Items.FLINT} aliases ride
 * the same two early arms upstream (:294-299). Sneaking + a non-block held item only
 * (the upstream {@code ST.block(aStack) == NB} gate, :301 — the modern form is "the
 * dispatch itself returned a kind", since BlockItems place themselves upstream too).
 *
 * <p>Consumption (verbatim): ingot/plate/plateGem/scrap consume the WHOLE stack (the
 * :306-:318 {@code ST.use(stack, stackSize)} arms), rock/stick one item (:294-:299). The
 * BOTA beacon exclusion (:305) has no port carrier — no Botania, the branch is dead
 * upstream for this universe. The dropped-item auto-place face (GT_API_Proxy.java:1467-1471)
 * and the oreRaw rock alias are the declared pool cuts (the item-entity event seam).
 *
 * <p>Static dispatch entry: {@link #trySneakPlace} — the single face BOTH the live event
 * below and the {@code /gt6placeables place} acceptance command (the
 * command-IS-the-channel ruling) drive.
 */
@Mod.EventBusSubscriber(modid = "gt6")
public final class GT6PlaceablePlacement {

	private GT6PlaceablePlacement() {}

	/**
	 * The dispatch: which placed-pile block a held stack places, or null. The upstream
	 * early arms first (vanilla stick/flint, GT_Proxy :294-:299), then the
	 * MaterialPrefixItem faces (:303-:319).
	 */
	public static GT6PlaceableBlock.Kind kindOf(ItemStack aStack) {
		if (aStack.is(Items.STICK)) return GT6PlaceableBlock.Kind.STICK; // the :294 vanilla-stick arm
		if (aStack.is(Items.FLINT)) return GT6PlaceableBlock.Kind.ROCK; // the :297 vanilla-flint arm
		if (aStack.getItem() instanceof MaterialPrefixItem tItem) return kindOfPrefix(tItem.prefix);
		return null;
	}

	/** The prefix arm (the registry-free dispatch seam — the offline test drives this directly). */
	public static GT6PlaceableBlock.Kind kindOfPrefix(gregapi.oredict.OreDictPrefix aPrefix) {
		switch (aPrefix.mNameInternal) {
			case "ingot": return GT6PlaceableBlock.Kind.INGOT;
			case "plate": return GT6PlaceableBlock.Kind.PLATE;
			case "plateGem": return GT6PlaceableBlock.Kind.GEM_PLATE;
			case "scrapGt": return GT6PlaceableBlock.Kind.SCRAP;
			case "rockGt": return GT6PlaceableBlock.Kind.ROCK;
			case "stick": return GT6PlaceableBlock.Kind.STICK;
			default: return null;
		}
	}

	/**
	 * The shared sneak-place entry: target the clicked face's neighbour (replaceable-only,
	 * the vanilla placement spot), write the pile, consume the held stack per kind.
	 * Returns SUCCESS on a placed pile.
	 */
	public static InteractionResult trySneakPlace(ServerLevel aLevel, Player aPlayer, BlockPos aClicked, Direction aFace, ItemStack aHeld) {
		GT6PlaceableBlock.Kind tKind = kindOf(aHeld);
		if (tKind == null || aHeld.isEmpty()) return InteractionResult.PASS;
		BlockPos tTarget = aClicked.relative(aFace);
		if (!aLevel.getBlockState(tTarget).canBeReplaced()) return InteractionResult.PASS;
		GT6PlaceableBlock tBlock = GT6Placeables.placed(tKind);
		if (tBlock == null) return InteractionResult.PASS;
		// the vanilla placement context (the UseOnContext companion; the hit carries the
		// clicked face so the vanilla getStateForPlacement machinery stays honest)
		BlockPlaceContext tContext = new BlockPlaceContext(aPlayer, InteractionHand.MAIN_HAND, aHeld,
				new BlockHitResult(Vec3.atBottomCenterOf(tTarget), aFace, aClicked, false));
		BlockState tPlacement = tBlock.getStateForPlacement(tContext);
		// no entity-collision probe needed — every pile shape is collision-empty
		if (!tPlacement.canSurvive(aLevel, tTarget)) return InteractionResult.PASS;
		if (!aLevel.setBlock(tTarget, tPlacement, Block.UPDATE_ALL)) return InteractionResult.PASS;
		if (aLevel.getBlockEntity(tTarget) instanceof GT6PlaceableBlockEntity tPile) {
			// the consumption split (GT_Proxy :306-:318 whole stack vs :294-:299 one item)
			if (tKind.wholeStack) {
				tPile.setStack(aHeld.copy());
				aHeld.setCount(0);
			} else {
				ItemStack tOne = aHeld.copy();
				tOne.setCount(1);
				tPile.setStack(tOne);
				aHeld.shrink(1);
			}
		}
		SoundType tSound = tBlock.kind().sound;
		aLevel.playSound(null, tTarget, tSound.getPlaceSound(), SoundSource.BLOCKS,
				(tSound.getVolume() + 1.0F) / 2.0F, tSound.getPitch() * 0.8F);
		return InteractionResult.SUCCESS;
	}

	/** The live-player arm (the GT_Proxy :294 walk): sneak + right-click-block + a placeable held item. */
	@SubscribeEvent
	public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock aEvent) {
		Player tPlayer = aEvent.getEntity();
		if (!tPlayer.isShiftKeyDown() || aEvent.getLevel().isClientSide()) return;
		InteractionResult tResult = trySneakPlace((ServerLevel) aEvent.getLevel(), tPlayer,
				aEvent.getPos(), aEvent.getFace(), aEvent.getItemStack());
		if (tResult == InteractionResult.SUCCESS) {
			aEvent.setCanceled(true); // the placement consumed the click (the ST.use + setCanceled pair)
			aEvent.setCancellationResult(InteractionResult.SUCCESS);
		}
	}
}
