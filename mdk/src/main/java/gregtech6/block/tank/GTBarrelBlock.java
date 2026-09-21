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

//? if neoforge {
/*import com.mojang.serialization.MapCodec;
import net.minecraft.world.item.component.CustomData;
import gregtech6.registry.GT6DataComponents;
 *///?}

import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import gregtech6.block.GTEntityBlock;
import gregtech6.fluid.GTDrinks;
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

	//? if neoforge {
	/*// (1.21.1: BlockBehaviour.codec() is abstract — the first concrete GTEntityBlock subclass
	// carries the hole. The vanilla ChestBlock shape (simpleCodec over a fixed closure) never
	// round-trips through datapacks: the barrel is code-registered like every GT6 block.)
	@Override
	protected MapCodec<GTBarrelBlock> codec() {
		return simpleCodec(aProperties -> new GTBarrelBlock(mCapacityL, mMeltingPointK, mGasProof, mTickerType, aProperties));
	}
	 *///?}

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
	 * {@code ICoverableTE.writeCoversToNBT} pair) plus the paint root-key pair (task
	 * p23-barrel-paint-item-seam): a painted barrel's drop carries {@code gt.color} +
	 * {@code gt.painted} so the picked-up stack renders tinted (GTItemPaintTint) and
	 * placement rehydrates the colour ({@code GTBarrelBlockItem.applyItemNBT}).
	 *
	 * <p>The paint gate mirrors {@code TileEntityBase03TicksAndSync.saveAdditional}
	 * (:322-325): {@code gt.painted} is written only while painted, {@code gt.color}
	 * only beside it — an unpainted barrel carries no paint keys, so an empty,
	 * cover-less, unpainted barrel keeps its null tag (byte-identical to the pre-card
	 * behaviour). The root-key shape is the upstream item seam itself ({@code 07Paintable
	 * .recolorItem} :89 {@code UT.NBT.set(stack, writeItemNBT(...))} — 1.7.10 had no
	 * loot tables, the drop carried the keys at the root); the barrel family stays
	 * table-less (the P12 ruling), so the machine-side {@code BlockEntityTag} loot route
	 * (GT6LootTables.copy_nbt) is deliberately NOT copied here. The reads ride the
	 * public {@code IPaintableTE} face — {@code isPainted()}/{@code getPaint()} are the
	 * exact {@code mIsPainted}/{@code mRGBa} mirrors server-side (the 07Paintable :84
	 * client material-colour inference needs a level, which a drop never has).
	 *
	 * <p>1.21.1 leg: there is no root tag — the closest envelope is the vanilla
	 * {@code CUSTOM_DATA} component, and {@code GTItemPaintTint.itemColor} reads the
	 * paint keys back out of exactly that envelope (the merge form: the existing
	 * envelope tag is extended, never replaced).
	 */
	public static ItemStack writeItemNBT(TileEntityBase08Barrel aBarrel, ItemStack aStack) {
		//? if forge {
		CompoundTag tTag = aStack.hasTag() ? aStack.getTag() : new CompoundTag();
		aBarrel.mTank.writeToNBT(tTag, TileEntityBase08Barrel.NBT_TANK); // upstream :84
		aBarrel.writeCoversToNBT(tTag); // upstream 06Covers :82
		if (aBarrel.isPainted()) { // the 03 saveAdditional :322-325 gate shape
			tTag.putInt(TileEntityBase03TicksAndSync.NBT_COLOR, aBarrel.getPaint());
			tTag.putBoolean(TileEntityBase03TicksAndSync.NBT_PAINTED, true);
		}
		aStack.setTag(tTag.isEmpty() ? null : tTag); // an empty barrel keeps the tag-less pre-card drop shape
		//?} else {
		/*CompoundTag tTank = new CompoundTag();
		aBarrel.mTank.writeToNBT(tTank, TileEntityBase08Barrel.NBT_TANK); // upstream :84
		if (tTank.isEmpty()) aStack.remove(GT6DataComponents.BARREL_CONTENT);
		else CustomData.set(GT6DataComponents.BARREL_CONTENT, aStack, tTank);
		CompoundTag tCovers = new CompoundTag();
		aBarrel.writeCoversToNBT(tCovers); // upstream 06Covers :82 — the 's'..'x' lane keys ride COVER_PAYLOAD
		if (tCovers.isEmpty()) aStack.remove(GT6DataComponents.COVER_PAYLOAD);
		else CustomData.set(GT6DataComponents.COVER_PAYLOAD, aStack, tCovers);
		if (aBarrel.isPainted()) { // the 03 saveAdditional :322-325 gate shape — merged into the CUSTOM_DATA envelope
			CompoundTag tPaint = aStack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
					CustomData.EMPTY).copyTag();
			tPaint.putInt(TileEntityBase03TicksAndSync.NBT_COLOR, aBarrel.getPaint());
			tPaint.putBoolean(TileEntityBase03TicksAndSync.NBT_PAINTED, true);
			CustomData.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, aStack, tPaint);
		}
		 *///?}
		return aStack;
	}

	//? if forge {
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
		// p33-food-fluids-b2 — the upstream tank drink seam, the tryTankDrink helper (the
		// p33-food-tail fold of the twin 17-line blocks). The FluidUtil face below stays
		// first so a container-carrying hand keeps the bucket behaviour.
		if (aLevel.getBlockEntity(aPos) instanceof TileEntityBase08Barrel tBarrelDrink) {
			InteractionResult tDrink = tryTankDrink(tBarrelDrink, aPlayer, aPlayer.getItemInHand(aHand).isEmpty());
			if (tDrink != null) return tDrink;
		}
		// spec ② — FluidUtil.java:64 signature; runs on both sides like the documented idiom,
		// the server pass is authoritative, the client pass is the prediction.
		return FluidUtil.interactWithFluidHandler(aPlayer, aHand, aLevel, aPos, aHit.getDirection())
				? InteractionResult.SUCCESS
				: InteractionResult.PASS;
	}
	//?} else {
	/*// (1.21.1: Block.use is gone — useWithoutItem is the same-seam hook: the default
	// useItemOn returns PASS_TO_DEFAULT_BLOCK_INTERACTION for every click, so useWithoutItem
	// receives both empty-hand and item-hand right clicks exactly like the 1.20.1 use().
	// Declared deviation: the hook has no InteractionHand parameter — MAIN_HAND stands in,
	// off-hand bucket clicks degrade to the main hand on the 1.21.1 node.)
	@Override
	protected InteractionResult useWithoutItem(BlockState aState, Level aLevel, BlockPos aPos, Player aPlayer, BlockHitResult aHit) {
		if (aLevel.getBlockEntity(aPos) instanceof TileEntityBase08Barrel tBarrel
				&& tBarrel.onCoverUse(aPlayer, (byte) aHit.getDirection().get3DDataValue(), aPlayer.getItemInHand(InteractionHand.MAIN_HAND),
						(float) (aHit.getLocation().x - aPos.getX()), (float) (aHit.getLocation().y - aPos.getY()), (float) (aHit.getLocation().z - aPos.getZ()))) {
			return InteractionResult.CONSUME;
		}
		// p33-food-fluids-b2 — the tank drink seam, the forge-branch shape above (the
		// MAIN_HAND stand-in is the declared deviation; the empty-hand gate is the same).
		if (aLevel.getBlockEntity(aPos) instanceof TileEntityBase08Barrel tBarrel2) {
			InteractionResult tDrink = tryTankDrink(tBarrel2, aPlayer, aPlayer.getItemInHand(InteractionHand.MAIN_HAND).isEmpty());
			if (tDrink != null) return tDrink;
		}
		return FluidUtil.interactWithFluidHandler(aPlayer, InteractionHand.MAIN_HAND, aLevel, aPos, aHit.getDirection())
				? InteractionResult.SUCCESS
				: InteractionResult.PASS;
	}
	 *///?}

	/**
	 * The p33-food-fluids-b2 tank drink seam (TileEntityBase08FluidContainer :158/:337 +
	 * isDrinkable :415-417), folded into one helper by p33-food-tail — an empty hand on a
	 * barrel holding >= 250 mB of a REGISTER-keyed fluid drains the 250 mB and applies the
	 * DrinkStat (hunger/sat through FoodData.eat, the effects as MobEffectInstance adds —
	 * GTDrinks.drink). CONSUME (no swing) like the cover intercept, null falls through to
	 * the FluidUtil bucket face. The body is leg-invariant — the hand-empty gate rides the
	 * caller (forge = the clicked hand, 1.21.1 = the MAIN_HAND stand-in).
	 */
	private static InteractionResult tryTankDrink(TileEntityBase08Barrel aBarrel, Player aPlayer, boolean aHandEmpty) {
		if (!aHandEmpty || !aBarrel.mTank.has(GTDrinks.DRINK_MB) || aBarrel.mTank.getFluid() == null) return null;
		GTDrinks.DrinkStat tStat = GTDrinks.stat(net.minecraft.core.registries.BuiltInRegistries.FLUID
				.getKey(aBarrel.mTank.getFluid().getFluid()).getPath());
		if (tStat == null || !GTDrinks.drink(aPlayer, tStat)) return null;
		aBarrel.mTank.drain(GTDrinks.DRINK_MB, IFluidHandler.FluidAction.EXECUTE);
		aBarrel.setChanged();
		return InteractionResult.CONSUME;
	}
}
