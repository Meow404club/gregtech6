package gregtech6.tileentity.attachment;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1.20.1 counterpart of gregtech/tileentity/tools/MultiTileEntityFluidNozzle.java
 * and MultiTileEntityFluidCapNozzle.java (task material-mc-f-attachment-rows) — the
 * material-row carrier for the nozzle pair. The upstream activation chains are the
 * DECLARED pool cut this card does not port:
 * <ul>
 * <li>the nozzle's gas-drain half (MultiTileEntityFluidNozzle onBlockActivated3
 *     :80-153 — the {@code ITileEntityTapAccessible.nozzleDrain} mirror of the tap,
 *     gases-only probe + voiding/XP/mob/held-container branches);</li>
 * <li>the cap nozzle's gas-fill half (MultiTileEntityFluidCapNozzle onBlockActivated3
 *     :64-88 — held gas cell into the {@code ITileEntityFunnelAccessible}
 *     .capnozzleFill target).</li>
 * </ul>
 *
 * <p>Both halves need the {@code nozzleDrain}/{@code capnozzleFill} interface arms on
 * the container BEs (barrels/tanks — shared-layer faces outside this card's
 * FILES_SCOPE), so the chains land with the nozzle-function pool card. The type
 * exists NOW because the BET validity needs a block-entity class the moment the rows
 * register (the GTTapBlockEntity:126 "nozzleDrain rides the Nozzle pool card"
 * sentence, made a registered type); activation reports the cut (offline-assertable).
 * One BE class serves BOTH BETs (NOZZLE_BE + CAP_NOZZLE_BE — the ADR-P3-1
 * shared-class-multi-mount form; the row config rides the block carrier).
 */
public class GTNozzleBlockEntity extends GTAttachmentSmallBlockEntity {

	public GTNozzleBlockEntity(BlockPos aPos, BlockState aState) {
		this(null, aPos, aState);
	}

	public GTNozzleBlockEntity(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(false, aType != null ? aType : gregtech6.registry.GTBlockEntities.NOZZLE_BE.get(), aPos, aState);
	}

	@Override
	public String getTileEntityName() {
		return "nozzle"; // BET registry paths mirror it (GTBlockEntities.NOZZLE_BE; the cap BET shares the class)
	}

	/**
	 * The declared pool cut — the gas-drain/gas-fill chains are the nozzle-function
	 * pool card's deliverable (see the class doc). The click consumes like upstream
	 * (onBlockActivated3 returns T on both classes) but moves nothing.
	 */
	@Override
	protected String activateChain(@Nullable Player aPlayer, byte aSide, @Nullable ItemStack aHeld) {
		return "the nozzle gas chains ride the nozzle-function pool card (declared cut, task material-mc-f-attachment-rows)";
	}
}
