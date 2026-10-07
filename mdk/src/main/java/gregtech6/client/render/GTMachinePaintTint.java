package gregtech6.client.render;

import javax.annotation.Nullable;

import gregapi.data.ANY;
import gregapi.data.MT;
import gregapi.oredict.OreDictMaterial;
import gregtech6.block.GTBasicMachineBlock;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.data.ModelData;

/**
 * The machine paint tint (task paintable-tint-render, card_B of the P21 paintable
 * split; the colour source re-based on the row material by task
 * machine-material-tint-fidelity) — the client consumption half of the paint storage:
 * the machine-domain block models carry {@code tintindex 0} (the GT6BlockStates
 * machineModel element form) and the tint resolves from the BE's
 * {@link GTModelProperties#PAINT} model data (the 03 base supplies it while painted,
 * TileEntityBase03TicksAndSync.getModelData).
 *
 * <p>Since task render-embeddium-tint this class is the PURE colour-decision seam
 * only: the WORLD half of the consumption moved to {@link GTMachineTintModel}, which
 * bakes {@link #tintARGB} into the quads' vertex colours at {@code getQuads} time (the
 * runtime {@code BlockColor} route rendered achromatic in the live client on both chunk
 * builders — the investigation record lives in the known_bugs
 * embeddium_tint_no_shader entry). {@link #blockColor} stays as the unregistered
 * reference form the tests drive; the ITEM half still rides
 * {@code GTItemPaintTint} ({@code ItemColor}), a different consumer that was never
 * implicated.
 *
 * <p>Upstream equivalence: the 1.7.10 machine renders {@code getTexture2 =
 * BlockTextureMulti(BlockTextureDefault(mTexturesMaterial[side], mRGBa), ...)}
 * (MultiTileEntityBasicMachine.java:1014) — the grayscale texture multiplied by the colour.
 * The DEFAULT colour is the row material, NOT white: the registration derives
 * {@code NBT_COLOR = getRGBInt(material.fRGBaSolid)} from every machine row's NBT_MATERIAL
 * (MultiTileEntityClassContainer.java:51), so an unpainted steel machine renders
 * gray-white and a copper one orange-red (the research.p27-machine-tint-reresearch
 * correction of the former "upstream default gray" reading — white was only the field
 * fallback, Paintable:50). This seam mirrors that two-level chain: a present PAINT value
 * wins (the spray-paint override, upstream Paintable:85); an absent one falls back to the
 * block's {@code NBT_MATERIAL} column through
 * {@link GTBasicMachineBlock#materialOf} — the same white {@code 0xFFFFFF} fallback the
 * material-less rows keep (upstream UNCOLORED, CS.java:327). The vanilla tint pipeline is
 * the same multiplication over the quad colour, so returning the row colour here IS the
 * upstream result, and full-alpha white stays numerically the vanilla {@code -1} no-tint
 * sentinel.
 *
 * <p>The material dispatch is domain-gated: only the block carriers that mirror upstream
 * NBT_MATERIAL rows resolve a material (the machine blocks, the Oven ladder, the burning
 * boxes, the tank-valve controllers through the controller gate — the issue8-residual
 * join —, since task tank-render-tint the barrel rows, since task
 * tint-coverage-batch the metal static-storage rows and the reactor core, and since task
 * small-tank-colored-tint the four small-tank families — the 40 cell rows, the four gas
 * cylinders, the Porcelain cup, the Ceramic jug —, and since task
 * act-charging-table-tint the Advanced/Charging Crafting Table matrix (the 120 rows'
 * NBT_MATERIAL column, Loader :136-137); since task
 * tint-chain-hopper-grindstone-sifting the 120 storage-hopper rows (the
 * {@code GT6HopperBlock} row carrier), the Grindstone and the Sifting Table (the
 * ANY.Steel block-class carriers — the census L1/L2/L3 closures). The barrel
 * join retires the
 * former P23 "unpainted barrel = zero visual change" white identity: the borrowed
 * grayscale {@code barrel_parts} art now multiplies
 * the row colour exactly like the machines (upstream renders
 * {@code BlockTextureDefault(colored, mRGBa)}, MultiTileEntityBarrelWood.java:44-55), so
 * an unpainted wood barrel renders the WoodTreated brown and an unpainted logistics tank
 * the ANY.W gray — a painted barrel still wins through the PAINT value above, and the
 * {@code -1} white sentinel stays the material-less fallback (upstream UNCOLORED,
 * CS.java:327). The small-tank join rides the same clause: their borrowed grayscale
 * colored_* art multiplies the row colour (GasCylinder :141 / Cell :42-44 / Cup :68-73 /
 * Jug :71-76). Every other tint index returns {@code -1} (no tint).
 *
 * <p>CLIENT-ONLY ({@code @OnlyIn(Dist.CLIENT)} — registered from GTClientHandlers under the
 * dist guard; the registration faces are unchanged, the P22 carrier ruling stands).
 */
@OnlyIn(Dist.CLIENT)
public final class GTMachinePaintTint {

	/**
	 * Upstream UNCOLORED = 0xFFFFFF (CS.java:327) — the MATERIAL-LESS fallback: white
	 * multiplies the grayscale texture unchanged (Paintable:50 field default; the row
	 * materials render their fRGBaSolid instead since task
	 * machine-material-tint-fidelity).
	 */
	public static final int UNPAINTED = 0xFFFFFF;

	private GTMachinePaintTint() {
	}

	/**
	 * The pure seam the tests drive (the GTWireTint.tintARGB shape): the opaque ARGB for
	 * one tint index over one snapshot and one row material. Index 0 reads
	 * {@link GTModelProperties#PAINT} — painted = the stored colour (the spray override);
	 * unpainted = the row material's fRGBaSolid, or white when the material is null or
	 * {@code MT.NULL} (the no-tint identity). Every other index is no tint.
	 */
	public static int tintARGB(@Nullable ModelData aData, @Nullable OreDictMaterial aMaterial, int aTintIndex) {
		if (aTintIndex != 0) return -1;
		Integer tPaint = aData == null ? null : aData.get(GTModelProperties.PAINT);
		if (tPaint != null) return 0xFF000000 | (tPaint.intValue() & UNPAINTED);
		return 0xFF000000 | GTBasicMachineBlock.materialColor(aMaterial);
	}

	/**
	 * The combined tint-material dispatch (task issue8-multipart-tint; the kitchen
	 * family joined in task c3-kitchen-tint-shape, the controller/energy domains in
	 * task c2-controller-tint, the bee family in task beehive-tint): the
	 * part-family carriers first (their material rides the block itself through
	 * {@code GTMultiBlockPartBlock.materialOf}), then the kitchen carriers
	 * ({@code GTKitchenBlock.materialOf}), then the machine-domain gate, then the
	 * controller/energy-domain gates (the multiblock mains, the EU-bridge + laser
	 * GT6DynamoBlock carriers, the magic absorber), then the Bumbliary pair
	 * ({@code GT6BumbliaryBlock.materialOf}, the ANY.Wood / StainlessSteel rows :2222-2223).
	 * The domains are disjoint so the order is observational; every tint consumer (the
	 * baked {@code GTMachineTintModel}, the {@code ItemColor} inventory half, this
	 * reference world half) funnels here, keeping tintARGB the single colour decision site.
	 */
	static OreDictMaterial tintMaterialOf(@Nullable net.minecraft.world.level.block.Block aBlock) {
		OreDictMaterial tMaterial = gregtech6.block.multiblock.GTMultiBlockPartBlock.materialOf(aBlock);
		if (tMaterial != null) return tMaterial;
		tMaterial = gregtech6.block.tools.GTKitchenBlock.materialOf(aBlock);
		if (tMaterial != null) return tMaterial;
		tMaterial = GTBasicMachineBlock.materialOf(aBlock);
		if (tMaterial != null) return tMaterial;
		// task tex-large-machines — the 17 large-controller rows: the twelve
		// large-machine rows carry their upstream NBT_MATERIAL through the row itself
		// (the GTLargeMachineBlock carrier — no Supplier column needed, the row IS the
		// carrier), the five mains ride their concrete block classes; the Loader
		// :1228-1283 column is the unpainted default (the upstream mRGBa pass)
		tMaterial = largeControllerMaterialOf(aBlock);
		if (tMaterial != null) return tMaterial;
		tMaterial = gregtech6.block.multiblock.GTMultiBlockControllerBlock.materialOf(aBlock);
		if (tMaterial != null) return tMaterial;
		tMaterial = gregtech6.block.energy.GT6DynamoBlock.materialOf(aBlock);
		if (tMaterial != null) return tMaterial;
	tMaterial = gregtech6.block.energy.GT6ElectricTransformerBlock.materialOf(aBlock);
	if (tMaterial != null) return tMaterial;
	// task tex-composite-family — the composite-energy carriers (the battery boxes,
	// the crystal chargers, the ZPM dechargers; the LD endpoints ride the transformer
	// gate above)
	tMaterial = gregtech6.block.energy.GT6BatteryBoxBlock.materialOf(aBlock);
	if (tMaterial != null) return tMaterial;
	tMaterial = gregtech6.block.energy.GT6MagicAbsorberBlock.materialOf(aBlock);
		if (tMaterial != null) return tMaterial;
		// task tex-pipe-textures — the pipe connector carriers join: the fluid pipe
		// family rides MT.Wood (the addFluidPipes 26000 NBT_MATERIAL row), the item pipe
		// family its loader line's MT argument (MultiTileEntityPipeItem :76-82). The
		// logistics wire resolves NULL on purpose — its upstream NBT_MATERIAL column is
		// MT.NULL (Loader :1819), so the wire keeps the white identity while the models
		// carry it in the same walk.
		tMaterial = gregtech6.block.pipe.GTFluidPipeBlock.materialOf(aBlock);
		if (tMaterial != null) return tMaterial;
		tMaterial = gregtech6.block.pipe.GTItemPipeBlock.materialOf(aBlock);
		if (tMaterial != null) return tMaterial;
		// task tex-multiblockmains — the large heat exchanger joins (the :1245
		// NBT_MATERIAL row, MT.W); the crucible controllers, the logistics core and the
		// lightning rod resolve through the GTMultiBlockControllerBlock gate above (the
		// p38-c2 carrier form all four now ride)
		tMaterial = gregtech6.registry.GT6HeatExchangers.HeatExchangerBlock.materialOf(aBlock);
		if (tMaterial != null) return tMaterial;
		// task tank-render-tint — the barrel family joins: the four standalone rows carry
		// their upstream NBT_MATERIAL through the block carrier (Loader :2140 WoodTreated,
		// :2150 ANY.Plastic, :2151 MT.Bronze, :2171 ANY.W), the twelve high-tier drums the
		// MetalDrumRow column (:2159-2170); the P23 white identity retires to the
		// material-less fallback (the class-doc clause)
		tMaterial = gregtech6.block.tank.GTBarrelBlock.materialOf(aBlock);
		if (tMaterial != null) return tMaterial;
		// task tex-bridge-kinetic — the kinetic engines join: the 28 steam-engine rows
		// carry their loader NBT_MATERIAL column (Loader :584-612), the 8 diesel rows
		// theirs (:721-729), the rotation transformer the WoodTreated row (:1668). The
		// slugs resolve through the bySlug table below (the row records live in
		// GT6Kinetics, outside this card's file scope — the dispatch arm reads the block
		// carriers directly).
		tMaterial = steamEngineMaterialOf(aBlock);
		if (tMaterial != null) return tMaterial;
		tMaterial = dieselEngineMaterialOf(aBlock);
		if (tMaterial != null) return tMaterial;
		tMaterial = rotationTransformerMaterialOf(aBlock);
		if (tMaterial != null) return tMaterial;
		// task tint-coverage-batch — the static-storage METAL rows join: the four
		// locker/drawer/safe kinds carry their loader material anchor (Loader :191 Bronze /
		// :202 Steel — the hopper-family ruling) through the block-row carrier; the
		// bookshelf/bottlecrate rows answer null (the vanilla-finished plank art, the stone
		// precedent) and the borrowed grayscale colored_* bodies multiply the row colour
		// (upstream BlockTextureMulti colored×mRGBa + overlay, MultiTileEntityLocker
		// :92-110 / DrawerQuad:124-142 / SafeMechanical:97-111)
		tMaterial = gregtech6.registry.GT6StaticStorages.GT6StorageBlock.materialOf(aBlock);
		if (tMaterial != null) return tMaterial;
		// task tint-coverage-batch — the reactor core joins: the :738 Pb row through the
		// block-class carrier (the grayscale borrowed faces multiply Pb; the DYNAMIC
		// rod/fluid/axle render stack stays with the rod-render-pool card)
		tMaterial = gregtech6.registry.GT6Reactors.ReactorCoreBlock.materialOf(aBlock);
		if (tMaterial != null) return tMaterial;
		// task small-tank-colored-tint — the four small-tank families join: the cell rows
		// carry their loader NBT_MATERIAL column through the block carrier (Loader
		// :1770-1809), the four gas-cylinder rows theirs (:2101-2104), the single cup row
		// MT.Porcelain (:2094) and the single jug row MT.Ceramic (:2095) as fixed
		// constants; the colored-band seats (incl. the gas cylinder's all-barometer arm)
		// multiply the row colour like the upstream
		// BlockTextureDefault(colored, mRGBa) passes (GasCylinder :141 / Cell :42-44 /
		// Cup :68-73 / Jug :71-76) — the four cards' declared measuring-pot deviation
		// retires
		tMaterial = gregtech6.block.tank.GT6CellBlock.materialOf(aBlock);
		if (tMaterial != null) return tMaterial;
		tMaterial = gregtech6.block.tank.GT6GasCylinderBlock.materialOf(aBlock);
		if (tMaterial != null) return tMaterial;
		tMaterial = gregtech6.block.tank.GT6CupBlock.materialOf(aBlock);
		if (tMaterial != null) return tMaterial;
		tMaterial = gregtech6.block.tank.GT6JugBlock.materialOf(aBlock);
		if (tMaterial != null) return tMaterial;
		// task act-charging-table-tint — the Advanced/Charging Crafting Table matrix
		// joins: the 120 rows carry their loader NBT_MATERIAL column (Loader
		// :136-137, one material per plain/charging pair) through the
		// GTAdvancedCraftingTableBlock carrier; the unpainted shell tints the tier
		// material colour exactly like the upstream colored×mRGBa pass
		// (MultiTileEntityAdvancedCraftingTable.java:659-662, the charging twin :61-64)
		tMaterial = gregtech6.block.GTAdvancedCraftingTableBlock.materialOf(aBlock);
		if (tMaterial != null) return tMaterial;
		// task tint-chain-hopper-grindstone-sifting — the census L1/L2/L3 closures join:
		// the 120 storage-hopper rows carry their loader NBT_MATERIAL through the
		// GT6HopperBlock row carrier (MultiTileEntityHopper.java:281 colored×mRGBa over
		// Loader:145-146 — the full 60-material walk), the Grindstone and the Sifting
		// Table the single ANY.Steel rows through their block-class carriers (Loader
		// :2226/:2227, the upstream BlockTextureMulti colored×mRGBa passes GrindStone
		// :238-241 / SiftingTable :406-424)
		// task material-mc-d-powertrain-rows — the four powertrain ladders join (the
		// colored×mRGBa multiply the wooden gearbox seat never carried)
		tMaterial = powertrainMaterialOf(aBlock);
		if (tMaterial != null) return tMaterial;
		tMaterial = hopperMaterialOf(aBlock);
		if (tMaterial != null) return tMaterial;
		// task storage-massstorage — the 120 item mass storage rows join: every row
		// carries its loader NBT_MATERIAL (Loader :141 aMat / :142 MT.Black — the black
		// body IS the logistics identity), the body cube is the tintindex-0 seat (the
		// upstream getTexture2 BlockTextureMulti colored×mRGBa + overlay pass,
		// MassStorageStandard :112-115 / Logistics :113-116)
		tMaterial = gregtech6.registry.GT6StaticStorages.GT6MassStorageBlock.materialOf(aBlock);
		if (tMaterial != null) return tMaterial;
		tMaterial = gregtech6.block.tools.GT6GrindstoneBlock.materialOf(aBlock);
		if (tMaterial != null) return tMaterial;
		tMaterial = gregtech6.block.tools.GT6SiftingTableBlock.materialOf(aBlock);
		if (tMaterial != null) return tMaterial;
		// task material-mc-a-storage-chests — the metal-chest family joins: both kinds
		// carry the loader NBT_MATERIAL through the GT6ChestBlock carrier (the metalset
		// chest pair :132-133; the upstream IItemColorableRGB face on the shared MTE
		// class — the colored sheet × mRGBa multiply, MultiTileEntityChest :349-370)
		tMaterial = gregtech6.registry.GT6Chests.GT6ChestBlock.materialOf(aBlock);
		if (tMaterial != null) return tMaterial;
		// task block-family-32xxx-port — the 32xxx domain joins: the 60 charging-locker
		// rows carry their loader NBT_MATERIAL through the row carrier (Loader :139 over
		// the metalset walk — the MultiTileEntityLockerCharging :65 colored×mRGBa pass),
		// the sap bag its MT.Leather column (:2221, the SapBag :133 pass) and the plant
		// pot its MT.Ceramic column (:2229, the PlantPot :76 pass) through the fixed
		// single-row constants
		tMaterial = chargingLockerMaterialOf(aBlock);
		if (tMaterial != null) return tMaterial;
		if (aBlock instanceof gregtech6.registry.GT6MiscToolBlocks.GT6SapBagBlock) return MT.Leather;
		return aBlock instanceof gregtech6.registry.GT6MiscToolBlocks.GT6PlantPotBlock ? MT.Ceramic
				: gregtech6.tileentity.bees.GT6BumbliaryBlock.materialOf(aBlock);
	}

	/** The charging-locker row material (the shared metalset {@code HopperMaterial.mt} column), null off-carrier. */
	@Nullable
	private static OreDictMaterial chargingLockerMaterialOf(@Nullable net.minecraft.world.level.block.Block aBlock) {
		return aBlock instanceof gregtech6.registry.GT6ChargingLockers.GT6ChargingLockerBlock tLocker
				? tLocker.row().material().mt() : null;
	}

	/** The storage-hopper row material (the {@code HopperMaterial.mt} loader column), null off-carrier. */
	@Nullable
	private static OreDictMaterial hopperMaterialOf(@Nullable net.minecraft.world.level.block.Block aBlock) {
		return aBlock instanceof gregtech6.registry.GT6Hoppers.GT6HopperBlock tHopper ? tHopper.row().material().mt() : null;
	}

	/** The steam-engine row material (the {@code SteamEngineRow.matSlug} column), null off-carrier. */
	@Nullable
	private static OreDictMaterial steamEngineMaterialOf(@Nullable net.minecraft.world.level.block.Block aBlock) {
		return aBlock instanceof gregtech6.registry.GT6Kinetics.SteamEngineBlock tEngine ? bySlug(tEngine.row().matSlug()) : null;
	}

	/**
	 * The powertrain row material (task material-mc-d-powertrain-rows — the four ladders'
	 * shared {@code PowertrainRow.matSlug} column): the 13 rotation engines (Loader :1667-:1764),
	 * the 12 metal transformer gearboxes (:1677-:1765), the 12 metal custom gearboxes
	 * (:1678-:1766) and the 15 small steam turbines (:794-:811) — the upstream
	 * BlockTextureDefault colored×mRGBa pass the wooden gearbox singleton never needed.
	 */
	@Nullable
	private static OreDictMaterial powertrainMaterialOf(@Nullable net.minecraft.world.level.block.Block aBlock) {
		return aBlock instanceof gregtech6.registry.GT6Kinetics.PowertrainBlock tRow ? bySlug(tRow.row().matSlug()) : null;
	}

	/** The diesel-engine row material (the {@code DieselSpec.material} column), null off-carrier. */
	@Nullable
	private static OreDictMaterial dieselEngineMaterialOf(@Nullable net.minecraft.world.level.block.Block aBlock) {
		return aBlock instanceof gregtech6.block.energy.GTDieselEngineBlock tEngine ? bySlug(tEngine.spec.material()) : null;
	}

	/**
	 * The rotation transformer's WoodTreated row (upstream "Wooden Transformer Gearbox",
	 * Loader :1668 — NBT_MATERIAL MT.WoodTreated; the port ships exactly that row, the
	 * single wood-row variant).
	 */
	@Nullable
	private static OreDictMaterial rotationTransformerMaterialOf(@Nullable net.minecraft.world.level.block.Block aBlock) {
		return aBlock instanceof gregtech6.block.energy.GTTransformerRotationBlock ? gregapi.data.MT.WoodTreated : null;
	}

	/**
	 * The port row-slug → material column (the GT6Boilers slug conventions verbatim):
	 * the steam ladders (Loader :584-612) and the diesel ladder (:721-729) share it.
	 * Resolved at tint time (never class-load), so the raw {@code MT.X} reads are safe.
	 * Public — the pure seam the offline pin test drives (the tintARGB symmetry).
	 */
	@Nullable
	public static OreDictMaterial bySlug(String aSlug) {
		return switch (aSlug) {
			case "lead" -> gregapi.data.MT.Pb;
			case "tin_alloy" -> gregapi.data.MT.TinAlloy;
			case "bronze" -> gregapi.data.MT.Bronze;
			case "arsenic_copper" -> gregapi.data.MT.ArsenicCopper;
			case "arsenic_bronze" -> gregapi.data.MT.ArsenicBronze;
			case "brass" -> gregapi.data.MT.Brass;
			case "invar" -> gregapi.data.MT.Invar;
			case "iron_wood" -> gregapi.data.MT.IronWood;
			case "steel" -> gregapi.data.ANY.Steel;
			case "fiery_steel" -> gregapi.data.MT.FierySteel;
			case "chromium" -> gregapi.data.MT.Cr;
			case "titanium" -> gregapi.data.MT.Ti;
			case "tungsten" -> gregapi.data.ANY.W;
			case "tungstensteel" -> gregapi.data.MT.TungstenSteel;
			case "iridium" -> gregapi.data.MT.Ir;
			// task material-mc-d-powertrain-rows — the powertrain ladder slugs (the
			// Trinaquadalloy/Adamantium axle tail rows + the SST material column; the
			// wood/alloy rows the axle family's own arms carry stay there, same values)
			case "wood_treated" -> gregapi.data.MT.WoodTreated;
			case "titanium_iridium" -> gregapi.data.MT.Iritanium;
			case "trinitanium" -> gregapi.data.MT.Trinitanium;
			case "trinaquadalloy" -> gregapi.data.MT.Trinaquadalloy;
			case "adamantium" -> gregapi.data.MT.Ad;
			case "steeleaf" -> gregapi.data.MT.Steeleaf;
			case "thaumium" -> gregapi.data.MT.Thaumium;
			case "aluminium" -> gregapi.data.MT.Al;
			case "magnalium" -> gregapi.data.MT.Magnalium;
			case "void_metal" -> gregapi.data.MT.VoidMetal;
			case "graphene" -> gregapi.data.MT.Graphene;
			default -> null;
		};
	}

	/**
	 * The 17 large-controller rows' upstream NBT_MATERIAL column (task
	 * tex-large-machines; Loader_MultiTileEntities :1228-1283 verbatim): the twelve
	 * large-machine rows resolve through the row meta id (the
	 * {@link gregtech6.registry.GT6LargeMachines.GTLargeMachineBlock#row} carrier), the
	 * five mains through their concrete block classes. Null = not a large controller
	 * (the dispatch arm is observational, the gate order stays disjoint).
	 */
	@Nullable
	private static OreDictMaterial largeControllerMaterialOf(@Nullable net.minecraft.world.level.block.Block aBlock) {
		if (aBlock instanceof gregtech6.registry.GT6LargeMachines.GTLargeMachineBlock tLarge)
			return controllerRowMaterial(tLarge.row().metaId());
		if (aBlock instanceof gregtech6.block.multiblock.GTFusionReactorBlock) return MT.SteelGalvanized;
		if (aBlock instanceof gregtech6.block.multiblock.GTVonDaGraaggBlock) return MT.SteelGalvanized;
		if (aBlock instanceof gregtech6.block.multiblock.GTImplosionCompressorBlock) return MT.TungstenSteel;
		if (aBlock instanceof gregtech6.block.multiblock.GTMassfabBlock) return MT.Pb;
		if (aBlock instanceof gregtech6.block.multiblock.GTBedrockDrillBlock) return MT.Ti;
		return null;
	}

	/** The meta-id → material map over the twelve large-machine rows (Loader :1229-1240 verbatim). */
	private static OreDictMaterial controllerRowMaterial(int aMetaId) {
		return switch (aMetaId) {
			case 17100, 17108, 17109, 17110 -> MT.TungstenSteel;
			case 17102, 17103, 17104, 17105, 17112, 17113 -> MT.StainlessSteel;
			case 17106 -> MT.Invar;
			case 17107, 17999 -> MT.Ti;
			case 17114 -> ANY.Steel;
			case 17198, 17996 -> MT.SteelGalvanized;
			case 17199 -> MT.Pb;
			default -> null;
		};
	}

	/**
	 * The world-side half: registered over {@code GTMachines.paintableBlockArray()} and the
	 * barrel array (GTClientHandlers). The registration scope is unchanged — the material
	 * resolution rides the combined {@link #tintMaterialOf} gate on the state's block, so
	 * only the NBT_MATERIAL carriers tint while unpainted. A null level/pos or a missing
	 * BE is the no-tint sentinel — which IS full-alpha white (see the class doc identity).
	 */
	public static BlockColor blockColor() {
		return (BlockState aState, @Nullable BlockAndTintGetter aLevel, @Nullable BlockPos aPos, int aTintIndex) -> {
			if (aTintIndex != 0 || aLevel == null || aPos == null) return -1;
			BlockEntity tBE = aLevel.getBlockEntity(aPos);
			OreDictMaterial tMaterial = aState == null ? null : tintMaterialOf(aState.getBlock());
			return tBE == null ? -1 : tintARGB(tBE.getModelData(), tMaterial, aTintIndex);
		};
	}
}
