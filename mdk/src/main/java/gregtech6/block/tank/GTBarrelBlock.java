package gregtech6.block.tank;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;

import net.minecraftforge.fluids.FluidUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import gregtech6.block.GTEntityBlock;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;
import gregtech6.tileentity.tank.TileEntityBase08Barrel;

/**
 * The fluid barrel block — the block side of the barrel family (task p4-fluid-barrel,
 * extended by task p6-barrel-metal-plastic). The carrier pattern: the block carries the
 * registration values upstream wrote into the MTE definition NBT (the GTFluidPipeBlock
 * carrier shape) — here the {@code NBT_TANK_CAPACITY} tank size and the
 * {@code NBT_CAPACITY_HU} melt-down ceiling (Loader_MultiTileEntities.java:2136-2151)
 * — plus the BET this barrel family member mounts: upstream assigns one TE class per
 * material row (Wood/Plastic/Metal), so the ctor takes the ticker type as a Supplier
 * (the RegistryObject is unbound at registration-lambda time) and each member points
 * at its own BET.
 *
 * <p>{@code use} is the bucket interaction face (spec ②): the documented Forge idiom
 * over {@link FluidUtil#interactWithFluidHandler(Player, InteractionHand, Level, BlockPos,
 * net.minecraft.core.Direction)} (FluidUtil.java:64/:83) — the player's fluid container
 * is filled from the barrel first, then drained into it (:95/:98 order), the item swap
 * and stow handled by FluidUtil. The barrel capability resolves through the
 * side-wrapped {@code BarrelFluidHandler} (getFluidHandler(level, pos, side) :457).
 *
 * <p>Task p12-fluid-item-carrier adds the item-carrier faces: {@code getDrops} rides
 * the loot-context BLOCK_ENTITY parameter and projects the tank + covers NBT onto the
 * dropped {@code GTBarrelBlockItem} (the upstream getDrops chain :157-162 → :81-85,
 * the fix for "breaking a filled barrel voids the content"), the item's
 * {@code FLUID_HANDLER_ITEM} capability serves the same tank semantics, and placement
 * reads the item NBT back into the fresh BE — content survives break AND place.
 */
public class GTBarrelBlock extends GTEntityBlock {

	private final long mCapacityL;
	private final long mMeltingPointK;
	private final boolean mGasProof;
	private final Supplier<BlockEntityType<? extends TileEntityBase03TicksAndSync>> mTickerType;

	/**
	 * @param aCapacityL the tank size (upstream {@code NBT_TANK_CAPACITY}: wood 16000,
	 *        plastic 32000, metal 64000 — Loader_MultiTileEntities.java:2140/:2150/:2151)
	 * @param aMeltingPointK the melt-down ceiling (upstream {@code NBT_CAPACITY_HU}: wood
	 *        340, plastic 370; MAX_VALUE = never melts — the metal drum rows carry no HU
	 *        and the upstream {@code mMaterial.mMeltingPoint * 1.25} formula needs the
	 *        material bridge this repo does not ship, so metal is a declared deviation)
	 * @param aGasProof the upstream {@code NBT_GASPROOF} row flag (task p13): F on the
	 *        wood family (:2136-2149), T on plastic (:2150), every metal drum
	 *        (:2151-2170) and the logistics tank (:2171) — the registration home of the
	 *        gas-proof quartet value; the item face reads it (the GTBarrelBlockItem
	 *        handler), the BE classes mirror it through their {@code gasProof()}
	 *        overrides
	 * @param aTickerType the BET this family member mounts (one TE class per material row,
	 *        the upstream Wood/Plastic/Metal trio shape)
	 */
	public GTBarrelBlock(long aCapacityL, long aMeltingPointK, boolean aGasProof,
			Supplier<BlockEntityType<? extends TileEntityBase03TicksAndSync>> aTickerType, Properties aProperties) {
		super(aProperties);
		mCapacityL = aCapacityL;
		mMeltingPointK = aMeltingPointK;
		mGasProof = aGasProof;
		mTickerType = aTickerType;
	}

	/** The tank size in litres (upstream NBT_TANK_CAPACITY, Loader_MultiTileEntities.java:2140/:2150/:2151). */
	public long capacityL() {
		return mCapacityL;
	}

	/** The melt-down ceiling in Kelvin (upstream NBT_CAPACITY_HU=340, Loader_MultiTileEntities.java:2136). */
	public long meltingPointK() {
		return mMeltingPointK;
	}

	/** The upstream NBT_GASPROOF row flag (task p13, the capacityL/meltingPointK carrier seam). */
	public boolean gasProof() {
		return mGasProof;
	}

	@Override
	protected BlockEntityType<? extends TileEntityBase03TicksAndSync> tickerType() {
		return mTickerType.get();
	}

	@Override
	public RenderShape getRenderShape(BlockState aState) {
		return RenderShape.MODEL; // BaseEntityBlock default INVISIBLE is for BER blocks
	}

	/**
	 * The break-drops face (task p12-fluid-item-carrier spec ①) — the upstream getDrops
	 * chain verbatim, the fix for the in-repo gap "breaking a filled barrel voids the
	 * content": upstream {@code TileEntityBase03MultiTileEntities.getDrops} (:157-162)
	 * returns exactly one item — the registry item carrying {@code writeItemNBT} output,
	 * whose barrel branch ({@code TileEntityBase08Barrel.writeItemNBT2} :81-85 over
	 * {@code 06Covers.writeItemNBT} :81-82) writes the tank onto the stack. The 1.20.1
	 * seam is the loot-context BLOCK_ENTITY parameter: the barrel BE rides the loot
	 * builder and {@link #writeItemNBT} projects its tank + covers onto the family's own
	 * item — which placement then reads back through the same keys. The loot-table layer
	 * is vanilla furniture the upstream seam bypasses (the family ships table-less, so
	 * pre-card a broken barrel dropped NOTHING); fortune/silk never mattered upstream
	 * either. Deliberately NOT an {@code onRemove} override (the red line): getDrops is
	 * the upstream seam, it never touches the BlockEntity lifecycle.
	 */
	@Override
	public List<ItemStack> getDrops(BlockState aState, LootParams.Builder aBuilder) {
		BlockEntity tBE = aBuilder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
		if (!(tBE instanceof TileEntityBase08Barrel tBarrel)) return super.getDrops(aState, aBuilder);
		List<ItemStack> tDrops = new ArrayList<>(1); // upstream :158 rList = ST.arraylist()
		tDrops.add(writeItemNBT(tBarrel, new ItemStack(this.asItem()))); // upstream :160 tRegistry.getItem(id, writeItemNBT(...))
		return tDrops;
	}

	/**
	 * The upstream {@code writeItemNBT2} :81-85 trimmed to what the port carries — the
	 * tank (mode/sealed progress ride the cut sealed-fermentation pool) plus the covers
	 * (upstream {@code 06Covers.writeItemNBT} :82, the in-repo
	 * {@code ICoverableTE.writeCoversToNBT} pair). An empty, cover-less barrel keeps a
	 * null tag: the drop is byte-identical to the pre-card behaviour.
	 */
	public static ItemStack writeItemNBT(TileEntityBase08Barrel aBarrel, ItemStack aStack) {
		CompoundTag tTag = aStack.hasTag() ? aStack.getTag() : new CompoundTag();
		aBarrel.mTank.writeToNBT(tTag, TileEntityBase08Barrel.NBT_TANK); // upstream :84
		aBarrel.writeCoversToNBT(tTag); // upstream 06Covers :82
		aStack.setTag(tTag.isEmpty() ? null : tTag); // an empty barrel keeps the tag-less pre-card drop shape
		return aStack;
	}

	@Override
	public InteractionResult use(BlockState aState, Level aLevel, BlockPos aPos, Player aPlayer, InteractionHand aHand, BlockHitResult aHit) {
		// p5 spec ④ — the cover machinery consumes the click first (the GTOvenBlock 2e89501
		// three-line pattern over ICoverableTE.onCoverUse: the covered-face intercepts, then
		// the attachCoversFirst install branch); false falls through to the bucket face below.
		// Runs on both sides like the FluidUtil idiom — the server pass is authoritative,
		// the client pass is the prediction.
		if (aLevel.getBlockEntity(aPos) instanceof TileEntityBase08Barrel tBarrel
				&& tBarrel.onCoverUse(aPlayer, (byte) aHit.getDirection().get3DDataValue(), aPlayer.getItemInHand(aHand),
						(float) (aHit.getLocation().x - aPos.getX()), (float) (aHit.getLocation().y - aPos.getY()), (float) (aHit.getLocation().z - aPos.getZ()))) {
			return InteractionResult.CONSUME;
		}
		// spec ② — FluidUtil.java:64 signature; runs on both sides like the documented idiom,
		// the server pass is authoritative, the client pass is the prediction.
		return FluidUtil.interactWithFluidHandler(aPlayer, aHand, aLevel, aPos, aHit.getDirection())
				? InteractionResult.SUCCESS
				: InteractionResult.PASS;
	}
}
