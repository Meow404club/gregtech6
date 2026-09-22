package gregtech6.fluid;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.resources.ResourceLocation;

import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;

import gregapi.oredict.MaterialRegistry;
import gregapi.oredict.OreDictMaterial;
import gregtech6.GT6Mod;
import gregtech6.item.spraycan.GTSprayCanItem;
import gregtech6.registry.GTFluidPipes;

/**
 * The first GT6 fluid content registration (task p4-fluid-pipes spec ⑥) — the official
 * four-DeferredRegister template of the Forge debug test FluidTypeTest.java:70-172:
 * FluidType (via {@code ForgeRegistries.Keys.FLUID_TYPES}) + Fluid (Source/Flowing pair
 * over one shared {@link ForgeFlowingFluid.Properties}) + the LiquidBlock, all attached
 * from a card-owned self-contained {@code @EventBusSubscriber(MOD)} listener (ADR-P3-4;
 * a bucket item is optional in the template and stays out of W1).
 *
 * <p>Molten iron: {@code gt6:iron_molten} at 144 L per material unit — the direct
 * counterpart of the upstream TCon binding {@code FL.make("iron.molten", 144)}
 * (Loader_Fluids.java:161-190, hand-listed like upstream; the bulk createMoltenFluid loop
 * is disabled there too, :216-224). Temperature/density/viscosity live on the
 * {@link FluidType.Properties} (FluidType.java:60/:85-123): Fe melting point 1811 K,
 * liquid density ~6980 kg/m³. Water/lava stay vanilla (they carry ForgeMod.WATER_TYPE/
 * LAVA_TYPE and need no registration).
 *
 * <p>Client textures: the {@code initializeClient} override points the still/flow layers
 * at the vanilla water textures with a molten tint (FluidTypeTest.java:82-150 shape) —
 * no dedicated PNG this card; the blockstate render-type wiring is a client-pool item.
 *
 * <p>Engine fuel family (task p12-engine-fuel-fluids): nine FURTHER fluids —
 * {@code steam} (the gaseous one), {@code distilled_water}, and the seven FM.Engine fuels
 * {@code diesel/kerosine/petrol/fuel/nitrofuel/jetfuel/ethanol} — registered through a
 * TABLE-DRIVEN helper ({@link #ENGINE_SPECS}, one row per fluid family) that folds the
 * four-DR template into {@link #engineFluid(String)}: FluidType + Source/Flowing over one
 * shared {@link ForgeFlowingFluid.Properties}, <b>no LiquidBlock and no bucket</b> —
 * fluid-only, the "a bucket item is optional in the template" precedent of the class doc
 * above. Litmus ruling (card spec ①): HAND-LISTED template rows, not a material bridge —
 * (a) the bridge seam {@code FluidBridge} is a molten-family lookup map outside this
 * card's file scope; (b) GTFluids static init runs at MOD CONSTRUCTION (the
 * @EventBusSubscriber class-load, the p6 a9027ac lesson) where {@code MT.*} fields are
 * still null, so the table must not capture material objects — the upstream material
 * data is transcribed as literals instead, each row's javadoc citing its MT.java row;
 * (c) 1.20.1 Forge registration is explicit DeferredRegister calls either way — the
 * bridge is a lookup seam, not a registration path. Every declaration value is
 * upstream-anchored except the JetFuel tint (no JetFuel material exists upstream, the
 * fluid is the "rc jet fuel" compat name, FL.java:422 — the tint is a port-owned
 * declared value). The FM.Engine fuel rows that consume these fluids live in
 * {@link gregtech6.recipes.GT6RecipesEngineFuels}.
 *
 * <p>Aqua family (task p16-aqua-fluids): six further fluid-only rows — {@code spdew},
 * {@code mnwtr}, {@code water_geothermal}, {@code water_boiling}, {@code hot_water},
 * {@code cold_water} — registered through the same table-driven shape
 * ({@link #AQUA_SPECS} + {@link #aquaFluid(String)}), which with the vanilla Water make
 * the seven-row Drying input domain (Loader_Recipes_Chem.java:525-532); the drying rows
 * that consume the ids live behind the GT6RecipesDrying resolver seam. Census rule of
 * the card: parameters the upstream carries are transcribed; parameters it does not
 * carry (the whole water_boiling definition, the tints) are honest FluidType defaults /
 * port-owned declared values, never invented numbers — see {@link #AQUA_SPECS}.
 *
 * <p>Simple-liquid family (task p19-drying-rows-backfill-2): two further fluid-only rows —
 * {@code seawater}, {@code waterdirty} — on the SECOND AquaFluidSpec table
 * ({@link #SIMPLE_LIQUID_SPECS}, deliberately not an AQUA_SPECS append: the upstream rows
 * carry SIMPLE+LIQUID only, no WATER tag, and the aqua family is exactly-order-pinned),
 * riding the same shared registration body; the consumers are the two unguarded salt rows
 * of the Drying backfill (Loader_Recipes_Chem.java:548/:553, behind the
 * GT6RecipesDrying resolver seam).
 *
 * <p>Food family (task p21-drying-food-fluids): four further fluid-only rows —
 * {@code sap}, {@code maplesap}, {@code reedwater}, {@code cactuswater} — on the THIRD
 * AquaFluidSpec table ({@link #FOOD_FLUID_SPECS}, its own table again: the rows carry the
 * FOOD tag upstream, FL.java:250/:252/:233-234, and the two earlier tables are
 * exactly-order-pinned by their tests), riding the same shared registration body. The
 * consumers are the four food rows of the Drying backfill (Loader_Recipes_Food.java:654-658,
 * behind the GT6RecipesDrying resolver seam). Three of the four have the GT6
 * {@code FL.create} definition (Loader_Fluids.java:461-463, texture parameter = null —
 * no dedicated fluid texture exists to borrow, the vanilla-water layers take a tint, the
 * ADR-P20 dual-tree policy never triggers); {@code sap} alone is an external-name fluid
 * (FL.java:250 shorthand, no {@code FL.create} anywhere — the same seawater/waterdirty
 * precedent), so its declared values are the honest FluidType defaults. The port
 * registers all four and pours all four Drying rows — the upstream
 * {@code FL.Sap.exists()} guard (:654) is live semantics over a registered fluid.
 *
 * <p>Dye-chemical family + chlorine (task p24-dye-chemical-fluids): sixteen WITH-BLOCK
 * families {@code dye_chemical_<GTSprayCanItem.DYE_IDS[i]>} — the Canner refill input
 * domain (Loader_Fluids.java:120-126), every tint the shared
 * {@link GTSprayCanItem#DYES_INT}[i] table, one borrowed grayscale carrier PNG tinted per
 * family — plus the standalone {@code chlorine} row (the material-gas-loop fluid the
 * port's logic-only material system has no bridge for, ruling R3). See the
 * {@link DyeChemicalFluid} block below for the full ruling map (R3/R4/R6).
 *
 * <p>Chemical family (task p29-w4-f1-chemicals): twenty-five further fluid-only rows —
 * the F-1 chemicals batch of the W4 fluid wave (decisions.p29-w4-split-rulings card ①):
 * the five oils {@code liquid_extra_heavy_oil/liquid_heavy_oil/liquid_medium_oil/
 * liquid_light_oil/soulsandoil} (Loader_Fluids.java:59-63), the four cracked gases
 * {@code propane/butane} (density −1000, :45-46) and {@code propylene/ethylene} (the
 * STATE_GASEOUS −100 carrier, :47-48), the thirteen gas-closure rows
 * {@code methane/carbondioxide/carbonmonoxide/hydrogen/nitrogen/oxygen/fluorine/helium/
 * neon/argon/krypton/xenon/radon} — the createGas walk semantics of FL.java:1080 (bare
 * material-name id, the boiling-point temperature rule) with the :1128-1136 density
 * formula transcribed to literals (the port registers no material bridge) — plus
 * {@code liquidoxygen} (:68) and the two plasmas {@code helium_plasma/nitrogen_plasma}
 * (:40-41, 10000 K / luminosity 15, the STATE_PLASMA carriers of FL.java:1106; the
 * upstream createPlasma amount semantics are 1 L per registered unit here — the port's
 * L semantics, the pool-bottom ruling with the Fusion card as the consumer). All on the
 * FOURTH spec table {@link #CHEMICAL_SPECS} riding a shared registration body with the
 * engine/aqua families. KJS surface (the class-doc declaration the card pins): REGISTRATION
 * face (the 25 Spec rows) only on this side — the datapack-domain recipe rows this card
 * ships (STEAM_CRACKING/CATALYTIC_CRACKING/FM.GAS/FM.BURN) are plain data/gt6 JSON the
 * loader already reads; tier-a crafting adds nothing; there is NO KubeJS-specific seam.
 *
 * <p><b>The HOT family, the closure carriers and the lubricant</b> (task p29-w4-hot-lube)
 * ride the SAME {@link ChemicalFluidSpec} record and the SAME registration body — the
 * FIFTH-SPEC-SECTION append AFTER the chemical table (the merge-order seam: card ① is the
 * first writer, card ④ appends behind it): the twelve hot fluids
 * {@code ic2coolant/ic2hotcoolant/hotmoltensodium/hotmoltentin/hotmoltenlicl/
 * hotheavywater/hotsemiheavywater/hottritiatedwater/hotcarbondioxide/hothelium/
 * thoriumsalt/ic2pahoehoelava} (Loader_Fluids.java:85-99, {@link #HOT_FLUID_SPECS}), the
 * seven FM.Hot closure carriers {@code blaze/sodium_molten/tin_molten/
 * lithium_chloride_molten/heavywater/semiheavywater/tritiatedwater}
 * ({@link #CLOSURE_FLUID_SPECS} — each row doc-anchored to the Loader_Fuels.java:191-211
 * reference that needs it), and the single F-2 lubricant row (Loader_Fluids.java:617,
 * {@link #LUBRICANT_FLUID_SPECS}). KJS surface: REGISTRATION face (the 20 Spec rows) +
 * the FM.HOT/distillation/DieselEngine datapack-domain rows of the same card; NO
 * KubeJS-specific seam.
 *
 * <p>The HONEY family and the bee-row dependency fluids (task p31-bees-lv1) ride the SAME
 * {@link ChemicalFluidSpec} record and the SAME {@link #specFluid} registration body —
 * the SIXTH-SPEC-SECTION append AFTER the lubricant row (the hot-lube merge-order seam
 * form): the four honey drinks {@code honey/honeydew/royal_jelly/ambrosia}
 * (Loader_Fluids.java:572/:575/:577/:576, the Bumblelyzer accept set) and the seven
 * comb-row output carriers {@code dragon_breath/concrete/chocolate_molten/ice/
 * soup_mushroom/latex/potion_harm_1} (Loader_Fluids.java:49/:196/:201/:364/:627/:198 +
 * FL.java:476 — the coordinator-approved SPEC deviation that keeps the 20 comb centrifuge
 * rows at zero skips). Fluid-only, no blocks, no buckets. KJS surface: REGISTRATION face
 * (the 11 Spec rows) + the comb datapack-domain rows of the same card; NO KubeJS-specific
 * seam.
 *
 * <p>The WORLDGEN BLOCK FACE (task p31-fluid-spring): five further LiquidBlocks —
 * {@code liquid_extra_heavy_oil_block/liquid_heavy_oil_block/liquid_medium_oil_block/
 * liquid_light_oil_block/water_geothermal_block} (see {@link #SPRING_BLOCK_IDS}) — the
 * placeable lake bodies of the {@code gt6:fluid_springs} bedrock-spring feature, with
 * {@code natural_gas} joining through its existing {@link #NATURAL_GAS_BLOCK} (task p5)
 * and the vanilla lava row needing no registration. See the {@link #SPRING_BLOCK_IDS}
 * section javadoc for the f1-ruling revision declaration this card pins.
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GTFluids {

	public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, "gt6");
	public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(ForgeRegistries.FLUIDS, "gt6");
	public static final DeferredRegister<net.minecraft.world.level.block.Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "gt6");

	private static ForgeFlowingFluid.Properties moltenIronProperties() {
		// FluidTypeTest.java:75-80 — type + still + flowing, block supplier attached
		return new ForgeFlowingFluid.Properties(IRON_MOLTEN_TYPE, IRON_MOLTEN, IRON_MOLTEN_FLOWING)
				.block(IRON_MOLTEN_BLOCK);
	}

	public static final RegistryObject<FluidType> IRON_MOLTEN_TYPE = FLUID_TYPES.register("iron_molten",
			() -> new FluidType(FluidType.Properties.create()
					.descriptionId("fluid.gt6.iron_molten")
					.temperature(1811)  // Fe melting point in K (upstream mMeltingPoint semantics, Loader_Fluids.java:84)
					.density(6980)
					.viscosity(3000)
					.lightLevel(15)) {
				@Override
				public void initializeClient(Consumer<IClientFluidTypeExtensions> aConsumer) {
					aConsumer.accept(new IClientFluidTypeExtensions() {
						private static final ResourceLocation STILL = ResourceLocation.withDefaultNamespace("block/water_still");
						private static final ResourceLocation FLOW = ResourceLocation.withDefaultNamespace("block/water_flow");

						@Override
						public ResourceLocation getStillTexture() {return STILL;}

						@Override
						public ResourceLocation getFlowingTexture() {return FLOW;}

						@Override
						public int getTintColor() {return 0xFFE8874A;} // molten iron tint over the vanilla textures
					});
				}
			});

	public static final RegistryObject<FlowingFluid> IRON_MOLTEN = FLUIDS.register("iron_molten",
			() -> new ForgeFlowingFluid.Source(moltenIronProperties()));

	public static final RegistryObject<Fluid> IRON_MOLTEN_FLOWING = FLUIDS.register("iron_molten_flowing",
			() -> new ForgeFlowingFluid.Flowing(moltenIronProperties()));

	public static final RegistryObject<LiquidBlock> IRON_MOLTEN_BLOCK = BLOCKS.register("iron_molten_block",
			() -> new LiquidBlock(IRON_MOLTEN, BlockBehaviour.Properties.of()
					.noCollission().strength(100.0F).noLootTable())); // FluidTypeTest.java:155-156 shape

	private static ForgeFlowingFluid.Properties naturalGasProperties() {
		// the four-DR template again (task p5-barrel-side-rules spec ⑤, the iron_molten :61-94 shape)
		return new ForgeFlowingFluid.Properties(NATURAL_GAS_TYPE, NATURAL_GAS, NATURAL_GAS_FLOWING)
				.block(NATURAL_GAS_BLOCK);
	}

	/**
	 * {@code gt6:natural_gas} — the lightweight acceptance carrier of the top-discharge rule
	 * (p5 spec ⑤, ruling ④): density −100 puts it strictly below air's 0 (the GT6 lighter
	 * verdict, FL.java:775), 300 K keeps it wood-barrel safe. No bucket item — the RCON
	 * driver fills through the barrel capability directly, and the client layers reuse the
	 * vanilla water textures over a pale gas tint (the iron_molten initializeClient shape).
	 */
	public static final RegistryObject<FluidType> NATURAL_GAS_TYPE = FLUID_TYPES.register("natural_gas",
			() -> new FluidType(FluidType.Properties.create()
					.descriptionId("fluid.gt6.natural_gas")
					.temperature(300)
					.density(-100)) {
				@Override
				public void initializeClient(Consumer<IClientFluidTypeExtensions> aConsumer) {
					aConsumer.accept(new IClientFluidTypeExtensions() {
						private static final ResourceLocation STILL = ResourceLocation.withDefaultNamespace("block/water_still");
						private static final ResourceLocation FLOW = ResourceLocation.withDefaultNamespace("block/water_flow");

						@Override
						public ResourceLocation getStillTexture() {return STILL;}

						@Override
						public ResourceLocation getFlowingTexture() {return FLOW;}

						@Override
						public int getTintColor() {return 0x66FFF2B0;} // pale gas tint over the vanilla textures
					});
				}
			});

	public static final RegistryObject<FlowingFluid> NATURAL_GAS = FLUIDS.register("natural_gas",
			() -> new ForgeFlowingFluid.Source(naturalGasProperties()));

	public static final RegistryObject<Fluid> NATURAL_GAS_FLOWING = FLUIDS.register("natural_gas_flowing",
			() -> new ForgeFlowingFluid.Flowing(naturalGasProperties()));

	public static final RegistryObject<LiquidBlock> NATURAL_GAS_BLOCK = BLOCKS.register("natural_gas_block",
			() -> new LiquidBlock(NATURAL_GAS, BlockBehaviour.Properties.of()
					.noCollission().noLootTable())); // a gas block: no strength ramp, nothing drops

	private static ForgeFlowingFluid.Properties creosoteProperties() {
		// the four-DR template again (natural_gas :96-140 shape, the iron_molten :61-94 original)
		return new ForgeFlowingFluid.Properties(CREOSOTE_TYPE, CREOSOTE, CREOSOTE_FLOWING)
				.block(CREOSOTE_BLOCK);
	}

	/**
	 * {@code gt6:creosote} — the Coke Oven by-product carrier (task p6-cokeoven-processing,
	 * ADR ruling ③). Density +1000 is a PORT-OWNED CARRIER VALUE, not an upstream measurement:
	 * the port's density consumers only branch on the sign (the P5 gravity rule, FL.java:775
	 * strictly {@code > 0} = heavier than air), the material density bridge stays in the pool.
	 * 300 K keeps it wood-barrel safe; heavier than air means the barrel pushes it out of the
	 * BOTTOM face — the P5 gravity semantics compose with the Coke Oven's top push for free.
	 * Client layers reuse the vanilla water textures over a dark-brown creosote tint (the
	 * natural_gas initializeClient shape); no bucket item (the RCON driver fills barrels
	 * through the capability, the mB amounts ride the machine's output tank).
	 */
	public static final RegistryObject<FluidType> CREOSOTE_TYPE = FLUID_TYPES.register("creosote",
			() -> new FluidType(FluidType.Properties.create()
					.descriptionId("fluid.gt6.creosote")
					.temperature(300)
					.density(1000)) {
				@Override
				public void initializeClient(Consumer<IClientFluidTypeExtensions> aConsumer) {
					aConsumer.accept(new IClientFluidTypeExtensions() {
						private static final ResourceLocation STILL = ResourceLocation.withDefaultNamespace("block/water_still");
						private static final ResourceLocation FLOW = ResourceLocation.withDefaultNamespace("block/water_flow");

						@Override
						public ResourceLocation getStillTexture() {return STILL;}

						@Override
						public ResourceLocation getFlowingTexture() {return FLOW;}

						@Override
						public int getTintColor() {return 0xFF3B2410;} // dark creosote brown over the vanilla textures
					});
				}
			});

	public static final RegistryObject<FlowingFluid> CREOSOTE = FLUIDS.register("creosote",
			() -> new ForgeFlowingFluid.Source(creosoteProperties()));

	public static final RegistryObject<Fluid> CREOSOTE_FLOWING = FLUIDS.register("creosote_flowing",
			() -> new ForgeFlowingFluid.Flowing(creosoteProperties()));

	public static final RegistryObject<LiquidBlock> CREOSOTE_BLOCK = BLOCKS.register("creosote_block",
			() -> new LiquidBlock(CREOSOTE, BlockBehaviour.Properties.of()
					.noCollission().strength(100.0F).noLootTable())); // a liquid: the iron_molten block ramp

	private static ForgeFlowingFluid.Properties oilProperties() {
		// the four-DR template again (creosote :142-190 shape, the iron_molten :61-94 original)
		return new ForgeFlowingFluid.Properties(OIL_TYPE, OIL, OIL_FLOWING)
				.block(OIL_BLOCK);
	}

	/**
	 * {@code gt6:oil} — the Coke Oven's oil-shale cracking output (task p7-cokeoven-backfill,
	 * spec ①; the upstream rows Loader_Recipes_Other.java:807-814 pour
	 * {@code MT.Oil.liquid(U4/U2, F)}). Both carrier values are UPSTREAM-VERIFIED, unlike the
	 * creosote port-owned precedent: temperature 300 K is the hardcoded default of the 4-arg
	 * {@code FL.create("oil", "Oil", MT.Oil, 1)} registration (Loader_Fluids.java:77 →
	 * FL.java:1088), and density 1000 comes out of the upstream material-density formula
	 * (FL.java:1128-1130: {@code 1000 * mGramPerCubicCentimeter} for the STATE_LIQUID branch,
	 * with the OreDictMaterial default 1.0 g/cm³ far above the 0.0012 air weight, CS.java:859)
	 * — heavier than air, so the P5 barrel gravity rule drains it out of the BOTTOM face, the
	 * same free composition creosote enjoys. Client layers reuse the vanilla water textures
	 * over a near-black crude tint (MT.Oil RGBa 10/10/10, MT.java:2041; the creosote
	 * initializeClient shape); no bucket item (the RCON driver fills barrels through the
	 * capability, the mB amounts ride the machine's output tank).
	 */
	public static final RegistryObject<FluidType> OIL_TYPE = FLUID_TYPES.register("oil",
			() -> new FluidType(FluidType.Properties.create()
					.descriptionId("fluid.gt6.oil")
					.temperature(300)
					.density(1000)) {
				@Override
				public void initializeClient(Consumer<IClientFluidTypeExtensions> aConsumer) {
					aConsumer.accept(new IClientFluidTypeExtensions() {
						private static final ResourceLocation STILL = ResourceLocation.withDefaultNamespace("block/water_still");
						private static final ResourceLocation FLOW = ResourceLocation.withDefaultNamespace("block/water_flow");

						@Override
						public ResourceLocation getStillTexture() {return STILL;}

						@Override
						public ResourceLocation getFlowingTexture() {return FLOW;}

						@Override
						public int getTintColor() {return 0xFF0A0A0A;} // the MT.Oil crude near-black over the vanilla textures
					});
				}
			});

	public static final RegistryObject<FlowingFluid> OIL = FLUIDS.register("oil",
			() -> new ForgeFlowingFluid.Source(oilProperties()));

	public static final RegistryObject<Fluid> OIL_FLOWING = FLUIDS.register("oil_flowing",
			() -> new ForgeFlowingFluid.Flowing(oilProperties()));

	public static final RegistryObject<LiquidBlock> OIL_BLOCK = BLOCKS.register("oil_block",
			() -> new LiquidBlock(OIL, BlockBehaviour.Properties.of()
					.noCollission().strength(100.0F).noLootTable())); // a liquid: the iron_molten block ramp

	/**
	 * The engine-steam conversion constants (task p12-engine-fuel-fluids spec ④, consumed by
	 * the p12-engine-steam card). {@code STEAM_PER_WATER = 200} is the ENGINE-PRIVATE value —
	 * MultiTileEntityEngineSteam.java:58 {@code public static final int STEAM_PER_WATER = 200},
	 * the engine's own steam→water recycle ratio (the tooltip math at :98 and the tank
	 * capacity at :80 both read it); it is NOT the global standard — CS.java:242 carries the
	 * separate global {@code STEAM_PER_WATER = 160} that the BOILER side uses (= EU_PER_WATER
	 * 80 × STEAM_PER_EU 2). {@code STEAM_PER_EU = 2} is CS.java:240 verbatim, unambiguous.
	 * Parked HERE (not in a GT6Kinetics constants class) because the engine-crank card owns
	 * that file in parallel — the steam card re-homes them on rebase, declared in the card.
	 */
	public static final int STEAM_PER_WATER = 200;
	/** CS.java:240 — 2 L steam per EU, the steam-to-energy divisor of EngineSteam/TurbineSteam (global, no engine-private override). */
	public static final int STEAM_PER_EU = 2;

	/**
	 * CS.java:238 — "The value of how many Energy Units a Liter of Water needs to turn into
	 * Steam" (the BOILER-side global, decision 2026-09-03-p13-boiler-family-split ②). This is
	 * the heat price of one litre of feed water; pair it with {@link #STEAM_PER_WATER_GLOBAL}
	 * for the steam yield and {@link #STEAM_PER_EU} for the 80 × 2 = 160 self-consistency.
	 * NOT the engine recycle math — the engine converts steam back with its own private
	 * {@link #STEAM_PER_WATER} = 200.
	 */
	public static final int EU_PER_WATER = 80;

	/**
	 * CS.java:242 — "The value of how much Steam a Liter of Water is worth. The Standard is
	 * 160 Steam = 1 Water" (the BOILER-side global, decision 2026-09-03-p13-boiler-family-split
	 * ②). The {@code _GLOBAL} suffix exists because the short name is TAKEN by the engine-private
	 * {@link #STEAM_PER_WATER} = 200 (MultiTileEntityEngineSteam.java:58, the engine's own
	 * steam→water recycle ratio, its five P12 consumers read that one) — the boiler side must
	 * only ever read THIS constant, and the two different upstream quantities must never be
	 * conflated (the scope-naming lesson). Self-consistent: {@link #EU_PER_WATER} 80 ×
	 * {@link #STEAM_PER_EU} 2 = 160.
	 */
	public static final int STEAM_PER_WATER_GLOBAL = 160;

	/**
	 * One engine-fuel declaration row: the pure data of one gt6 fluid family (readable
	 * OFFLINE — the tests assert these declared values without touching the registries;
	 * the live FluidType carries the same numbers at registration time).
	 *
	 * @param name       the gt6 registry path and the fluid id the fuel table references
	 * @param temperature in K; density (negative = lighter than air, the FL.java:775 sign
	 *                   rule the port's gravity consumers branch on); viscosity; tint (ARGB
	 *                   over the vanilla water textures, the iron_molten :81 precedent);
	 *                   gas = the gaseous declaration (upstream STATE_GASEOUS semantics:
	 *                   FL.java:1105 sets density −100 / viscosity 200 for gas-state fluids).
	 */
	public record EngineFluidSpec(String name, int temperature, int density, int viscosity, int tint, boolean gas) {
		/** The lang/description key, the descriptionId the FluidType is registered with. */
		public String descriptionId() {return "fluid.gt6." + name;}
	}

	/**
	 * The nine engine-family rows, declaration order mirroring the card list. Every value is
	 * upstream-anchored: steam 373 K is the FL.java:794 hardcode ({@code C+100}, C = 273,
	 * CS.java:132), density −100 / viscosity 200 the STATE_GASEOUS carriers (FL.java:1105;
	 * also the :1132 gas branch {@code -0.1 / 0.0010} for MT.Steam's 0.0010 g/cm³,
	 * MT.java:1884 — lighter than the 0.0012 air weight); the liquids ride the
	 * createLiquid :1072 temperature formula (the fuel materials' {@code .heat(100, 400)} =
	 * melting 100 K &lt; 300 → the 300 K clamp) and the :1130 liquid density formula (default
	 * 1.0 g/cm³ → 1000); tints are the materials' RGBa (MT.java:1884/:1891/:2040/:2044-2048);
	 * JetFuel has no material (the "rc jet fuel" compat name, FL.java:422) — 300 K / 1000
	 * carrier values and a port-owned amber tint, declared in the card.
	 */
	public static final List<EngineFluidSpec> ENGINE_SPECS = List.of(
		new EngineFluidSpec("steam"          , 373, -100,  200, 0xFFC8C8C8, true ), // MT.Steam 200,200,200 — the gaseous one
		new EngineFluidSpec("distilled_water", 300, 1000, 1000, 0xFF6E6EFF, false), // MT.DistWater 110,110,255 "Distilled Water"
		new EngineFluidSpec("diesel"         , 300, 1000, 1000, 0xFFFFFF00, false), // MT.Diesel 255,255,0 (MT.java:2047)
		new EngineFluidSpec("kerosine"       , 300, 1000, 1000, 0xFF0000FF, false), // MT.Kerosine 0,0,255 (MT.java:2046)
		new EngineFluidSpec("petrol"         , 300, 1000, 1000, 0xFFFF0000, false), // MT.Petrol 255,0,0 (MT.java:2048)
		new EngineFluidSpec("fuel"           , 300, 1000, 1000, 0xFFFFFF00, false), // MT.Fuel 255,255,0 "Fuel Oil" (MT.java:2044)
		new EngineFluidSpec("nitrofuel"      , 300, 1000, 1000, 0xFFC8FF00, false), // MT.NitroFuel 200,255,0 "Nitro-Fuel" (MT.java:2045)
		new EngineFluidSpec("jetfuel"        , 300, 1000, 1000, 0xFFD8C060, false), // no upstream material (FL.java:422) — port-owned declared values
		new EngineFluidSpec("ethanol"        , 300, 1000, 1000, 0xFFFF8000, false)); // MT.Ethanol 255,128,0 (MT.java:2040)

	/** One registered engine family: the declared spec + the three live handles. Fluid-only — no block, no bucket. */
	public static final class EngineFluid {
		/** The declaration row ({@link #ENGINE_SPECS}); the offline-readable half. */
		public final EngineFluidSpec spec;
		public final RegistryObject<FluidType> type;
		public final RegistryObject<FlowingFluid> source;
		public final RegistryObject<Fluid> flowing;

		EngineFluid(EngineFluidSpec aSpec, RegistryObject<FluidType> aType, RegistryObject<FlowingFluid> aSource, RegistryObject<Fluid> aFlowing) {
			spec = aSpec; type = aType; source = aSource; flowing = aFlowing;
		}
	}

	/** The family row for a gt6 id path, or null (the fuel-table lookup seam, GT6RecipesCokeOven.resolveFluid shape). */
	public static EngineFluidSpec engineSpec(String aName) {
		for (EngineFluidSpec tSpec : ENGINE_SPECS) if (tSpec.name().equals(aName)) return tSpec;
		return null;
	}

	/**
	 * The table-driven registration helper: applies the four-DR template (the iron_molten
	 * :61-94 shape) to one spec row — FluidType carrying the declared
	 * temperature/density/viscosity, Source/Flowing over one shared Properties — and
	 * attaches NOTHING else: no LiquidBlock (fluid-only, the bucket-optional precedent) and
	 * no bucket item. Client layers reuse the vanilla water textures over the row's tint
	 * (the natural_gas initializeClient shape). The Properties are built inside the
	 * supplier lambdas (registry-event time), reading the Source/Flowing handles off the
	 * seam maps below — the same forward-reference shape as the static-field
	 * moltenIronProperties() template.
	 */
	private static EngineFluid engineFluid(String aName) {
		EngineFluidSpec tSpec = engineSpec(aName);
		if (tSpec == null) throw new IllegalArgumentException("no engine fluid spec: " + aName);
		RegistryObject<FluidType> tType = FLUID_TYPES.register(tSpec.name(), () -> new FluidType(FluidType.Properties.create()
				.descriptionId(tSpec.descriptionId())
				.temperature(tSpec.temperature())
				.density(tSpec.density())
				.viscosity(tSpec.viscosity())) {
			@Override
			public void initializeClient(Consumer<IClientFluidTypeExtensions> aConsumer) {
				aConsumer.accept(new IClientFluidTypeExtensions() {
					private static final ResourceLocation STILL = ResourceLocation.withDefaultNamespace("block/water_still");
					private static final ResourceLocation FLOW = ResourceLocation.withDefaultNamespace("block/water_flow");

					@Override
					public ResourceLocation getStillTexture() {return STILL;}

					@Override
					public ResourceLocation getFlowingTexture() {return FLOW;}

					@Override
					public int getTintColor() {return tSpec.tint();}
				});
			}
		});
		RegistryObject<FlowingFluid> tSource = FLUIDS.register(tSpec.name(),
				() -> new ForgeFlowingFluid.Source(engineProperties(tSpec, tType)));
		RegistryObject<Fluid> tFlowing = FLUIDS.register(tSpec.name() + "_flowing",
				() -> new ForgeFlowingFluid.Flowing(engineProperties(tSpec, tType)));
		SOURCE_SEAM.put(tSpec.name(), tSource);
		FLOWING_SEAM.put(tSpec.name(), tFlowing);
		return new EngineFluid(tSpec, tType, tSource, tFlowing);
	}

	/** The shared per-family Properties — called at registry-event time only (see engineFluid). */
	private static ForgeFlowingFluid.Properties engineProperties(EngineFluidSpec aSpec, RegistryObject<FluidType> aType) {
		// NO .block(...) — the fluid-only declaration of this card
		return new ForgeFlowingFluid.Properties(aType, SOURCE_SEAM.get(aSpec.name()), FLOWING_SEAM.get(aSpec.name()));
	}

	private static final Map<String, RegistryObject<FlowingFluid>> SOURCE_SEAM = new LinkedHashMap<>();
	private static final Map<String, RegistryObject<Fluid>> FLOWING_SEAM = new LinkedHashMap<>();
	/** The block leg of the same seam (the dye-chemical rows carry a LiquidBlock; same registry-event-time discipline). */
	private static final Map<String, RegistryObject<LiquidBlock>> BLOCK_SEAM = new LinkedHashMap<>();

	/**
	 * The nine engine-family registrations — one line per fluid family, data from
	 * {@link #ENGINE_SPECS}. Static-init order: the DeferredRegister fields above are
	 * initialized first, the entries accumulate and fire with the existing
	 * {@link #onModConstruct} (types before fluids, FluidTypeTest.java:169-172 order).
	 */
	public static final EngineFluid STEAM           = engineFluid("steam");
	public static final EngineFluid DISTILLED_WATER = engineFluid("distilled_water");
	public static final EngineFluid DIESEL          = engineFluid("diesel");
	public static final EngineFluid KEROSINE        = engineFluid("kerosine");
	public static final EngineFluid PETROL          = engineFluid("petrol");
	public static final EngineFluid FUEL            = engineFluid("fuel");
	public static final EngineFluid NITROFUEL       = engineFluid("nitrofuel");
	public static final EngineFluid JETFUEL         = engineFluid("jetfuel");
	public static final EngineFluid ETHANOL         = engineFluid("ethanol");

	/** The nine registered families in {@link #ENGINE_SPECS} declaration order (the lang/table walkers). */
	public static List<EngineFluid> engineFluids() {
		return List.of(STEAM, DISTILLED_WATER, DIESEL, KEROSINE, PETROL, FUEL, NITROFUEL, JETFUEL, ETHANOL);
	}

	/**
	 * One aqua-family declaration row (task p16-aqua-fluids) — the same offline-readable
	 * data shape as {@link EngineFluidSpec} minus the gas flag (all six are liquids), plus
	 * the display name inline (the lang walker reads it straight off the row, no side map).
	 *
	 * @param name        the gt6 registry path — the ids the downstream drying table pins
	 *                    (GT6RecipesDrying.FLUID_SPDEW..FLUID_COLD, "the upstream FL
	 *                    shorthands, snake-cased"), NOT the upstream 1.7.10 fluid literal
	 *                    where the two differ (spdew/spectral_dew, mnwtr/potion.mineralwater,
	 *                    water_geothermal/watergeothermal, water_boiling/boilingwater)
	 * @param displayName the en_us lang value (upstream verbatim where GT6 defines the fluid)
	 * @param temperature in K; density/viscosity the liquid carriers; tint ARGB over the
	 *                    vanilla water textures (the iron_molten :81 precedent)
	 */
	public record AquaFluidSpec(String name, String displayName, int temperature, int density, int viscosity, int tint) {
		/** The lang/description key, the descriptionId the FluidType is registered with. */
		public String descriptionId() {return "fluid.gt6." + name;}
	}

	/**
	 * The six aqua-family rows (task p16-aqua-fluids: SpDew/MnWtr/Geothermal/Boiling/Hot/
	 * Cold), which with the vanilla Water make the seven-row Drying input domain of
	 * Loader_Recipes_Chem.java:525-532. Every value is census-anchored, honestly defaulted
	 * where the upstream carries no definition:
	 * <ul>
	 * <li>{@code spdew} — FL.java:113 "spectral_dew"; Loader_Fluids.java:362
	 *     {@code FL.create(..., 1, 1000, 300)} — 300 K, display "Spectral Dew" verbatim;</li>
	 * <li>{@code mnwtr} — FL.java:119 "potion.mineralwater"; Loader_Fluids.java:371
	 *     {@code FL.create(..., 1, 1000, 300)} — 300 K, "Mineral Water" verbatim;</li>
	 * <li>{@code water_geothermal} — FL.java:118 "watergeothermal"; Loader_Fluids.java:373
	 *     {@code FL.create(..., 1, 1000, 320)} — 320 K, "Hot Spring Water" verbatim;</li>
	 * <li>{@code water_boiling} — FL.java:117 "boilingwater" has NO GT6 Fluid definition
	 *     (an external-mod fluid: every recipe row guards it with
	 *     {@code FL.Water_Boiling.exists()}, Loader_Recipes_Chem.java:529) — the HONEST
	 *     DEFAULT stands, not fabricated: the FluidType.Properties defaults (FluidType.java
	 *     :924-926, density 1000 / temperature 300 / viscosity 1000) declared explicitly;</li>
	 * <li>{@code hot_water} — FL.java:115 "hot_water" (external, the {@code // 60°C}
	 *     annotation) — C+60 = 333 K, the same C = 273 (CS.java:132) constant the steam
	 *     anchor FL.java:794 uses;</li>
	 * <li>{@code cold_water} — FL.java:114 "cold_water" (external, {@code // 15°C}) —
	 *     C+15 = 288 K.</li>
	 * </ul>
	 *
	 * <p>Density 1000 / viscosity 1000 on every row are the STATE_LIQUID semantics
	 * (FL.java:1104 sets viscosity 1000 and leaves the density at the FluidType default —
	 * the material-density formula FL.java:1124-1130 is skipped for material-null
	 * registrations), which coincide with the FluidType.Properties defaults. The tints are
	 * PORT-OWNED DECLARED VALUES (the JetFuel precedent): the upstream FL.create aRGBa is
	 * null/UNCOLOURED for material-null fluids (FL.java:1094) — the 1.7.10 fluids carry
	 * dedicated texture PNGs this port does not have, so the vanilla-water layers take a
	 * row tint. All six sit at or below 333 K — under the wood-barrel 340 K melting point
	 * (GTBarrelCommand.WOOD_MELTING_POINT) — so the RCON chain carries them in wood
	 * barrels. The upstream Water_Hot row ("ic2hotwater", FL.java:116, the IC2 hot-water
	 * alias) is OUTSIDE this card's six — its drying row keeps the absent-fluid skip
	 * (GT6RecipesDrying.buildRecipe null arm) until a card ever ports the alias.
	 */
	public static final List<AquaFluidSpec> AQUA_SPECS = List.of(
		new AquaFluidSpec("spdew"           , "Spectral Dew"    , 300, 1000, 1000, 0x99C8E6FF), // Loader_Fluids.java:362 — ghostly translucent blue
		new AquaFluidSpec("mnwtr"           , "Mineral Water"   , 300, 1000, 1000, 0xFF8ED8C0), // Loader_Fluids.java:371 — light mineral green
		new AquaFluidSpec("water_geothermal", "Hot Spring Water", 320, 1000, 1000, 0xFF3EC8B4), // Loader_Fluids.java:373 — hot-spring turquoise
		new AquaFluidSpec("water_boiling"   , "Boiling Water"   , 300, 1000, 1000, 0xFFB4D2E8), // no upstream definition — honest FluidType defaults
		new AquaFluidSpec("hot_water"       , "Hot Water"       , 333, 1000, 1000, 0xFF6E6EDE), // FL.java:115 // 60°C
		new AquaFluidSpec("cold_water"      , "Cold Water"      , 288, 1000, 1000, 0xFFBDE8FF));// FL.java:114 // 15°C

	/** The aqua row for a gt6 id path, or null (the {@link #engineSpec} lookup shape). */
	public static AquaFluidSpec aquaSpec(String aName) {
		for (AquaFluidSpec tSpec : AQUA_SPECS) if (tSpec.name().equals(aName)) return tSpec;
		return null;
	}

	/** One registered aqua family: the declared spec + the three live handles. Fluid-only — no bucket; the block face exists only where the worldgen block seam carries the name (water_geothermal, p31-fluid-spring). */
	public static final class AquaFluid {
		/** The declaration row ({@link #AQUA_SPECS}); the offline-readable half. */
		public final AquaFluidSpec spec;
		public final RegistryObject<FluidType> type;
		public final RegistryObject<FlowingFluid> source;
		public final RegistryObject<Fluid> flowing;

		AquaFluid(AquaFluidSpec aSpec, RegistryObject<FluidType> aType, RegistryObject<FlowingFluid> aSource, RegistryObject<Fluid> aFlowing) {
			spec = aSpec; type = aType; source = aSource; flowing = aFlowing;
		}
	}

	/**
	 * The table-driven registration helper for one aqua row — the {@link #engineFluid}
	 * shape verbatim. Client layers reuse the vanilla water textures over the row's tint
	 * (the natural_gas initializeClient shape); the registration body itself is the shared
	 * {@link #registerFluidFamily(AquaFluidSpec)} (task p19-drying-rows-backfill-2 splits
	 * the lookup per family table, the body is one).
	 */
	private static AquaFluid aquaFluid(String aName) {
		AquaFluidSpec tSpec = aquaSpec(aName);
		if (tSpec == null) throw new IllegalArgumentException("no aqua fluid spec: " + aName);
		return registerFluidFamily(tSpec);
	}

	/**
	 * The shared per-family Properties for the AquaFluidSpec tables ({@link #AQUA_SPECS} and
	 * {@link #SIMPLE_LIQUID_SPECS}): FluidType carrying the declared
	 * temperature/density/viscosity, Source/Flowing over one shared Properties, NO bucket
	 * (the bucket container behaviour is out of the card scope) and a LiquidBlock only where
	 * {@link #withWorldgenBlock} finds one — the fluid-only declaration of p16/p19/p21 held
	 * until task p31-fluid-spring extended the block face for {@code water_geothermal} (the
	 * f1-revision declaration, {@link #SPRING_BLOCK_IDS}). Shares the SOURCE_SEAM/
	 * FLOWING_SEAM maps — the Properties are built at registry-event time (see engineFluid).
	 */
	private static AquaFluid registerFluidFamily(AquaFluidSpec tSpec) {
		RegistryObject<FluidType> tType = FLUID_TYPES.register(tSpec.name(), () -> new FluidType(FluidType.Properties.create()
				.descriptionId(tSpec.descriptionId())
				.temperature(tSpec.temperature())
				.density(tSpec.density())
				.viscosity(tSpec.viscosity())) {
			@Override
			public void initializeClient(Consumer<IClientFluidTypeExtensions> aConsumer) {
				aConsumer.accept(new IClientFluidTypeExtensions() {
					private static final ResourceLocation STILL = ResourceLocation.withDefaultNamespace("block/water_still");
					private static final ResourceLocation FLOW = ResourceLocation.withDefaultNamespace("block/water_flow");

					@Override
					public ResourceLocation getStillTexture() {return STILL;}

					@Override
					public ResourceLocation getFlowingTexture() {return FLOW;}

					@Override
					public int getTintColor() {return tSpec.tint();}
				});
			}
		});
		RegistryObject<FlowingFluid> tSource = FLUIDS.register(tSpec.name(),
				() -> new ForgeFlowingFluid.Source(aquaProperties(tSpec, tType)));
		RegistryObject<Fluid> tFlowing = FLUIDS.register(tSpec.name() + "_flowing",
				() -> new ForgeFlowingFluid.Flowing(aquaProperties(tSpec, tType)));
		SOURCE_SEAM.put(tSpec.name(), tSource);
		FLOWING_SEAM.put(tSpec.name(), tFlowing);
		return new AquaFluid(tSpec, tType, tSource, tFlowing);
	}

	/** The shared per-family Properties for the aqua rows — called at registry-event time only (see aquaFluid). */
	private static ForgeFlowingFluid.Properties aquaProperties(AquaFluidSpec aSpec, RegistryObject<FluidType> aType) {
		// the block leg rides the worldgen block face where the seam carries the name (p31-fluid-spring)
		return withWorldgenBlock(aType,
				new ForgeFlowingFluid.Properties(aType, SOURCE_SEAM.get(aSpec.name()), FLOWING_SEAM.get(aSpec.name())),
				aSpec.name());
	}

	/**
	 * The six aqua-family registrations — one line per fluid family, data from
	 * {@link #AQUA_SPECS}. Static-init order: the DeferredRegister fields above are
	 * initialized first, the entries accumulate and fire with the existing
	 * {@link #onModConstruct} (types before fluids, FluidTypeTest.java:169-172 order).
	 */
	public static final AquaFluid SPDEW            = aquaFluid("spdew");
	public static final AquaFluid MNWTR            = aquaFluid("mnwtr");
	public static final AquaFluid WATER_GEOTHERMAL = aquaFluid("water_geothermal");
	public static final AquaFluid WATER_BOILING    = aquaFluid("water_boiling");
	public static final AquaFluid HOT_WATER        = aquaFluid("hot_water");
	public static final AquaFluid COLD_WATER       = aquaFluid("cold_water");

	/** The six registered families in {@link #AQUA_SPECS} declaration order (the lang/table walkers). */
	public static List<AquaFluid> aquaFluids() {
		return List.of(SPDEW, MNWTR, WATER_GEOTHERMAL, WATER_BOILING, HOT_WATER, COLD_WATER);
	}

	/**
	 * The simple-liquid family (task p19-drying-rows-backfill-2 spec ③): the SECOND
	 * AquaFluidSpec table, deliberately NOT an {@link #AQUA_SPECS} append — the architect
	 * ruling: the two rows carry ONLY the SIMPLE+LIQUID flags upstream (FL.java:125
	 * "seawater" / :127 "waterdirty" — no FOOD/WATER/BATH/THERMOS), while the aqua family
	 * is pinned to its WATER-tagged six by the GT6EnUs walk and the exact-order assertion
	 * of GTFluidsAquaFamilyTest (untouched, still green). Neither fluid has a GT6
	 * {@code FL.create} registration upstream — the ids are external-mod fluid names GT6
	 * only attaches drink stats to (Loader_Fluids.java:365 "Dirty" C+37 / :366 "Salty"
	 * C+35 — DRINK stats, not fluid declarations, deliberately not transcribed) — so the
	 * declared values are the HONEST FluidType defaults (FluidType.java:924-926, 300 K /
	 * 1000 / 1000, which coincide with the STATE_LIQUID carriers, FL.java:1104) exactly
	 * like the water_boiling precedent, and the tints are PORT-OWNED DECLARED VALUES (the
	 * JetFuel/aqua precedent). They are registered as the live carriers of the two
	 * unguarded Drying rows — Loader_Recipes_Chem.java:548 (Ocean, no exists() guard) and
	 * :553 (Dirty_Water, no guard) — which upstream would silently drop without the
	 * external mod providing the fluid; the port prefers the rows live (declared
	 * port-owned decision of the architect card, the rows are transcribed verbatim
	 * regardless).
	 */
	public static final List<AquaFluidSpec> SIMPLE_LIQUID_SPECS = List.of(
		new AquaFluidSpec("seawater"  , "Seawater"    , 300, 1000, 1000, 0xFF3E8E9C), // FL.java:125 "seawater" — the Ocean shorthand, salty teal (declared)
		new AquaFluidSpec("waterdirty", "Dirty Water" , 300, 1000, 1000, 0xFF6B6B3C));// FL.java:127 "waterdirty" — murky waste brown-green (declared)

	/** The simple-liquid row for a gt6 id path, or null (the {@link #aquaSpec} lookup shape, its own table). */
	public static AquaFluidSpec simpleLiquidSpec(String aName) {
		for (AquaFluidSpec tSpec : SIMPLE_LIQUID_SPECS) if (tSpec.name().equals(aName)) return tSpec;
		return null;
	}

	/** The registration helper for one simple-liquid row (the {@link #aquaFluid} shape over the second table). */
	private static AquaFluid simpleLiquidFluid(String aName) {
		AquaFluidSpec tSpec = simpleLiquidSpec(aName);
		if (tSpec == null) throw new IllegalArgumentException("no simple liquid fluid spec: " + aName);
		return registerFluidFamily(tSpec);
	}

	/**
	 * The two simple-liquid registrations — one line per fluid family, data from
	 * {@link #SIMPLE_LIQUID_SPECS}, appended after the aqua block in the static-init order
	 * (the DeferredRegister fields accumulate, the entries fire with the existing
	 * {@link #onModConstruct}, types before fluids).
	 */
	public static final AquaFluid SEAWATER   = simpleLiquidFluid("seawater");
	public static final AquaFluid WATERDIRTY = simpleLiquidFluid("waterdirty");

	/** The two registered simple-liquid families in {@link #SIMPLE_LIQUID_SPECS} declaration order (the lang/table walkers). */
	public static List<AquaFluid> simpleLiquids() {
		return List.of(SEAWATER, WATERDIRTY);
	}

	/**
	 * The food family (task p21-drying-food-fluids): the THIRD AquaFluidSpec table — the
	 * {@link #SIMPLE_LIQUID_SPECS} shape verbatim (its own table, not an append: the
	 * upstream rows carry the FOOD tag, FL.java:250/:252/:233-234, and the two earlier
	 * families are exactly-order-pinned by their tests). Every value census-anchored:
	 * <ul>
	 * <li>{@code reedwater} — FL.java:233 "reedwater" (the Juice_Reed shorthand; the
	 *     upstream 1.7.10 display key {@code potion.reedwater} is NOT transcribed — the
	 *     port descriptionId convention is the declared deviation); Loader_Fluids.java:461
	 *     {@code FL.create(..., null, 1, 1000, 300)} — 300 K, display "Reedwater" verbatim;</li>
	 * <li>{@code cactuswater} — FL.java:234 "cactuswater"; Loader_Fluids.java:462 — 300 K,
	 *     display "Cactuswater" verbatim;</li>
	 * <li>{@code maplesap} — FL.java:252 "maplesap" (the Sap_Maple shorthand);
	 *     Loader_Fluids.java:463 — 300 K, display "Maple Sap" verbatim;</li>
	 * <li>{@code sap} — FL.java:250 "sap" (the Sap shorthand) has NO GT6
	 *     {@code FL.create} registration upstream (an external-mod fluid name, exactly the
	 *     seawater/waterdirty precedent): the HONEST DEFAULT stands, not fabricated — the
	 *     FluidType.Properties defaults (FluidType.java:924-926, 300 K / 1000 / 1000)
	 *     declared explicitly, display "Sap" the FL shorthand spelled out (the
	 *     water_boiling "Boiling Water" precedent).</li>
	 * </ul>
	 *
	 * <p>Density 1000 / viscosity 1000 on every row are the STATE_LIQUID semantics
	 * (FL.java:1104; the three FL.create rows carry the 1000 carrier literally). The tints
	 * are PORT-OWNED DECLARED VALUES (the JetFuel/aqua precedent): the upstream FL.create
	 * texture parameter is null (Loader_Fluids.java:461-463) — no dedicated fluid texture
	 * exists to borrow, the vanilla-water layers take a row tint (ADR-P20 never triggers).
	 * All four sit at 300 K — under the wood-barrel 340 K melting point
	 * (GTBarrelCommand.WOOD_MELTING_POINT) — so the RCON chain carries them in wood
	 * barrels. The v1 declaration is FLUID-ONLY: no LiquidBlock, no bucket, no bottle —
	 * the upstream bottle/container faces (MultiItemBottles.java:265 Maple Sap bottle +
	 * the OD.container250/1000maplesap candle recipes, Loader_Recipes_Vanilla.java:256-267)
	 * stay in the MultiItemBottles domain pool. The four Drying rows that consume the ids
	 * live behind the GT6RecipesDrying resolver seam (Loader_Recipes_Food.java:654-658;
	 * the :654 {@code FL.Sap.exists()} guard is live semantics over a registered fluid).
	 */
	public static final List<AquaFluidSpec> FOOD_FLUID_SPECS = List.of(
		new AquaFluidSpec("sap"        , "Sap"         , 300, 1000, 1000, 0xFFE8C87A), // FL.java:250 "sap", no FL.create — honest defaults; pale amber (declared)
		new AquaFluidSpec("maplesap"   , "Maple Sap"   , 300, 1000, 1000, 0xFFD28C3A), // FL.java:252 / Loader_Fluids.java:463 — amber (declared)
		new AquaFluidSpec("reedwater"  , "Reedwater"   , 300, 1000, 1000, 0xFFB8D89A), // FL.java:233 / Loader_Fluids.java:461 — pale reed green (declared)
		new AquaFluidSpec("cactuswater", "Cactuswater" , 300, 1000, 1000, 0xFF6FA84A));// FL.java:234 / Loader_Fluids.java:462 — cactus green (declared)

	/** The food row for a gt6 id path, or null (the {@link #simpleLiquidSpec} lookup shape, its own table). */
	public static AquaFluidSpec foodSpec(String aName) {
		for (AquaFluidSpec tSpec : FOOD_FLUID_SPECS) if (tSpec.name().equals(aName)) return tSpec;
		return null;
	}

	/** The registration helper for one food row (the {@link #simpleLiquidFluid} shape over the third table). */
	private static AquaFluid foodFluid(String aName) {
		AquaFluidSpec tSpec = foodSpec(aName);
		if (tSpec == null) throw new IllegalArgumentException("no food fluid spec: " + aName);
		return registerFluidFamily(tSpec);
	}

	/**
	 * The four food registrations — one line per fluid family, data from
	 * {@link #FOOD_FLUID_SPECS}, appended after the simple-liquid block in the static-init
	 * order (the DeferredRegister fields accumulate, the entries fire with the existing
	 * {@link #onModConstruct}, types before fluids).
	 */
	public static final AquaFluid SAP         = foodFluid("sap");
	public static final AquaFluid MAPLESAP    = foodFluid("maplesap");
	public static final AquaFluid REEDWATER   = foodFluid("reedwater");
	public static final AquaFluid CACTUSWATER = foodFluid("cactuswater");

	/** The four registered food families in {@link #FOOD_FLUID_SPECS} declaration order (the lang/table walkers). */
	public static List<AquaFluid> foodFluids() {
		return List.of(SAP, MAPLESAP, REEDWATER, CACTUSWATER);
	}


	/**
	 * The food-fluid batch 1 (task p33-food-fluids-b1): the FIFTH AquaFluidSpec table —
	 * its own table, NOT an append to {@link #FOOD_FLUID_SPECS} (that four-row table is
	 * exact-order-pinned by GTFluidsFoodFamilyTest; the architect split
	 * research.p33-r-food puts the FL.java:110-408 FOOD-flag remainder here). The full
	 * FL.java FOOD-flag census is 234 rows: 15 already ported on the earlier tables (the
	 * eleven WATER-tagged aqua rows + Sap/MapleSap/Reedwater/Cactuswater), 2 non-fluid or
	 * generic-collision shorthands deliberately absent (Sap_Rainbow FL.java:251; the
	 * "honey" generic id collision HoneyBoP), and the NON-FOOD oil tail
	 * (Oil_Canola/creosote/soulsand :164-166) outside the FOOD flag, leaving these 216 — the four p31 honey-family ids (honey/honeydew/ambrosia/royaljelly) ride the p31 ChemicalFluid HONEY_FLUID_SPECS registrations and are NOT re-declared here.
	 *
	 * <p>Every row carries the SIMPLE+LIQUID+FOOD flags upstream (plus family flags
	 * MILK/HONEY/JUICE/ALCOHOLIC/WINE/WHISKEY/LIQUEUR/LIQUOR/SPIRIT/BRANDY/CIDER/BEER/RUM/
	 * TEA/COOKING_OIL/SLIME/VINEGAR/BATH/THERMOS — the flags are NOT machine-readable in
	 * the port, the display/id/temperature faces are the transcribed surface). Temperature
	 * is the upstream FL.create carrier where GT6 defines the fluid (Loader_Fluids.java
	 * :376-647 FoodStatDrink block — 275 K wines/soy milk/royal jelly/BAWLS, 255 K ice
	 * tea, 400 K hot frying oil, 300 K everything else; the "C" literals fold to 300 via
	 * CS.java:132); the 68 external-mod ids with no GT6 FL.create (the binnie.* alcohol
	 * family + 8 vanilla-census ids) ride the HONEST FluidType defaults
	 * (FluidType.java:924-926) exactly like the sap/seawater precedents. Collision ids
	 * carry a suffix (grcmilk_milk, juice_wine_fruit, slime_blue, forestry_honey,
	 * grc_honey, diablosauce_strong).
	 *
	 * <p>Density 1000 / viscosity 1000 on every row are the STATE_LIQUID semantics
	 * (FL.java:1104). The tints are PORT-OWNED DECLARED VALUES (the JetFuel/aqua
	 * precedent): no dedicated upstream textures exist to borrow, the vanilla-water
	 * layers take a per-row tint over the shared registration body. FLUID-ONLY: no
	 * LiquidBlock, no bucket, no bottle (the p21 declaration held). NO drink behaviour —
	 * the FoodStatDrink seam is b2 (research.p33-r-food drink_seam); this card is
	 * registration+datagen+lang only.
	 */
	public static final List<AquaFluidSpec> FOOD_B1_SPECS = List.of(
		new AquaFluidSpec("mineralsoda"             , "Mineral Soda"                     , 300, 1000, 1000, 0xFFD8E8F0), // FL.java up "mineralsoda" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("soda"                    , "Soda"                             , 300, 1000, 1000, 0xFFD8E8F0), // FL.java up "soda" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("milk"                    , "Milk"                             , 300, 1000, 1000, 0xFFF8F8F4), // FL.java up "milk" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("soymilk"                 , "Soy Milk"                         , 275, 1000, 1000, 0xFFF8F8F4), // FL.java up "soymilk" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("grcmilk_milk"            , "Milk"                             , 300, 1000, 1000, 0xFFF8F8F4), // FL.java up "grcmilk.milk" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("spoiledmilk"             , "Milk"                             , 300, 1000, 1000, 0xFFE8D040), // FL.java up "spoiledmilk" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("for_honey"               , "Honey (Forestry, most other Mods)", 300, 1000, 1000, 0xFFE8A81A), // FL.java up "for.honey" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("grc_honey"               , "Honey (GrowthCraft)"              , 300, 1000, 1000, 0xFFE8A81A), // FL.java up "grc.honey" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("fruitsmoothie"           , "Froot Smoothie"                   , 300, 1000, 1000, 0xFFE89AA8), // FL.java up "fruitsmoothie" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("melonsmoothie"           , "Melon Smoothie"                   , 300, 1000, 1000, 0xFFE89AA8), // FL.java up "melonsmoothie" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("kiwismoothie"            , "Kiwi Smoothie"                    , 300, 1000, 1000, 0xFFE89AA8), // FL.java up "kiwismoothie" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("currantsmoothie"         , "Currant Smoothie"                 , 300, 1000, 1000, 0xFFE89AA8), // FL.java up "currantsmoothie" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("raspberrysmoothie"       , "Raspberry Smoothie"               , 300, 1000, 1000, 0xFFE89AA8), // FL.java up "raspberrysmoothie" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("blackberrysmoothie"      , "Blackberry Smoothie"              , 300, 1000, 1000, 0xFFE89AA8), // FL.java up "blackberrysmoothie" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("blueberrysmoothie"       , "Blueberry Smoothie"               , 300, 1000, 1000, 0xFFE89AA8), // FL.java up "blueberrysmoothie" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("gooseberrysmoothie"      , "Gooseberry Smoothie"              , 300, 1000, 1000, 0xFFE89AA8), // FL.java up "gooseberrysmoothie" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("strawberrysmoothie"      , "Strawberry Smoothie"              , 300, 1000, 1000, 0xFFE89AA8), // FL.java up "strawberrysmoothie" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("plumsmoothie"            , "Plum Smoothie"                    , 300, 1000, 1000, 0xFFE89AA8), // FL.java up "plumsmoothie" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("peachsmoothie"           , "Peach Smoothie"                   , 300, 1000, 1000, 0xFFE89AA8), // FL.java up "peachsmoothie" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("elderberrysmoothie"      , "Elderberry Smoothie"              , 300, 1000, 1000, 0xFFE89AA8), // FL.java up "elderberrysmoothie" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("grapefruitsmoothie"      , "Grapefruit Smoothie"              , 300, 1000, 1000, 0xFFE89AA8), // FL.java up "grapefruitsmoothie" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("limesmoothie"            , "Lime Smoothie"                    , 300, 1000, 1000, 0xFFE89AA8), // FL.java up "limesmoothie" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("orangesmoothie"          , "Orange Smoothie"                  , 300, 1000, 1000, 0xFFE89AA8), // FL.java up "orangesmoothie" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("persimmonsmoothie"       , "Persimmon Smoothie"               , 300, 1000, 1000, 0xFFE89AA8), // FL.java up "persimmonsmoothie" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("apricotsmoothie"         , "Apricot Smoothie"                 , 300, 1000, 1000, 0xFFE89AA8), // FL.java up "apricotsmoothie" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("pearsmoothie"            , "Pear Smoothie"                    , 300, 1000, 1000, 0xFFE89AA8), // FL.java up "pearsmoothie" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("redgrapesmoothie"        , "Grape Smoothie"                   , 300, 1000, 1000, 0xFFE89AA8), // FL.java up "redgrapesmoothie" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("whitegrapesmoothie"      , "Grape Smoothie"                   , 300, 1000, 1000, 0xFFE89AA8), // FL.java up "whitegrapesmoothie" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("grapesmoothie"           , "Grape Smoothie"                   , 300, 1000, 1000, 0xFFE89AA8), // FL.java up "grapesmoothie" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("purplegrapesmoothie"     , "Grape Smoothie"                   , 300, 1000, 1000, 0xFFE89AA8), // FL.java up "purplegrapesmoothie" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("applesmoothie"           , "Apple Smoothie"                   , 300, 1000, 1000, 0xFFE89AA8), // FL.java up "applesmoothie" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("pineapplesmoothie"       , "Ananas Smoothie"                  , 300, 1000, 1000, 0xFFE89AA8), // FL.java up "pineapplesmoothie" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("bananasmoothie"          , "Banana Smoothie"                  , 300, 1000, 1000, 0xFFE89AA8), // FL.java up "bananasmoothie" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("cherrysmoothie"          , "Cherry Smoothie"                  , 300, 1000, 1000, 0xFFE89AA8), // FL.java up "cherrysmoothie" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("cranberrysmoothie"       , "Cranberry Smoothie"               , 300, 1000, 1000, 0xFFE89AA8), // FL.java up "cranberrysmoothie" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("lemonsmoothie"           , "Lemon Smoothie"                   , 300, 1000, 1000, 0xFFE89AA8), // FL.java up "lemonsmoothie" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("mangosmoothie"           , "Mango Smoothie"                   , 300, 1000, 1000, 0xFFE89AA8), // FL.java up "mangosmoothie" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("pomegranatesmoothie"     , "Pomegranate Smoothie"             , 300, 1000, 1000, 0xFFE89AA8), // FL.java up "pomegranatesmoothie" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("starfruitsmoothie"       , "Starfruit Smoothie"               , 300, 1000, 1000, 0xFFE89AA8), // FL.java up "starfruitsmoothie" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("papayasmoothie"          , "Papaya Smoothie"                  , 300, 1000, 1000, 0xFFE89AA8), // FL.java up "papayasmoothie" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("figsmoothie"             , "Fig Smoothie"                     , 300, 1000, 1000, 0xFFE89AA8), // FL.java up "figsmoothie" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("coconutsmoothie"         , "Coconut Smoothie"                 , 300, 1000, 1000, 0xFFE89AA8), // FL.java up "coconutsmoothie" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("juice_juice"             , "Juice"                            , 300, 1000, 1000, 0xFFE8A83A), // FL.java up "juice" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("kiwijuice"               , "Kiwi Juice"                       , 300, 1000, 1000, 0xFFE8A83A), // FL.java up "kiwijuice" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("juicelime"               , "Lime Juice"                       , 300, 1000, 1000, 0xFFE8A83A), // FL.java up "binnie.juicelime" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("juicelemon"              , "Lemon Juice"                      , 300, 1000, 1000, 0xFFE8A83A), // FL.java up "binnie.juicelemon" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("juiceorange"             , "Orange Juice"                     , 300, 1000, 1000, 0xFFE8A83A), // FL.java up "binnie.juiceorange" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("persimmonjuice"          , "Persimmon Juice"                  , 300, 1000, 1000, 0xFFE8A83A), // FL.java up "persimmonjuice" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("melonjuice"              , "Melon Juice"                      , 300, 1000, 1000, 0xFFE8A83A), // FL.java up "melonjuice" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("currantjuice"            , "Currant Juice"                    , 300, 1000, 1000, 0xFFE8A83A), // FL.java up "currantjuice" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("raspberryjuice"          , "Raspberry Juice"                  , 300, 1000, 1000, 0xFFE8A83A), // FL.java up "raspberryjuice" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("blackberryjuice"         , "Blackberry Juice"                 , 300, 1000, 1000, 0xFFE8A83A), // FL.java up "blackberryjuice" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("blueberryjuice"          , "Blueberry Juice"                  , 300, 1000, 1000, 0xFFE8A83A), // FL.java up "blueberryjuice" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("gooseberryjuice"         , "Gooseberry Juice"                 , 300, 1000, 1000, 0xFFE8A83A), // FL.java up "gooseberryjuice" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("strawberryjuice"         , "Strawberry Juice"                 , 300, 1000, 1000, 0xFFE8A83A), // FL.java up "strawberryjuice" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("juiceplum"               , "Plum Juice"                       , 300, 1000, 1000, 0xFFE8A83A), // FL.java up "binnie.juiceplum" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("juicepeach"              , "Peach Juice"                      , 300, 1000, 1000, 0xFFE8A83A), // FL.java up "binnie.juicepeach" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("juiceelderberry"         , "Elderberry Juice"                 , 300, 1000, 1000, 0xFFE8A83A), // FL.java up "binnie.juiceelderberry" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("hellderberryjuice"       , "Hellderberry Juice"               , 300, 1000, 1000, 0xFFE8A83A), // FL.java up "hellderberryjuice" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("juicegrapefruit"         , "Grapefruit Juice"                 , 300, 1000, 1000, 0xFFE8A83A), // FL.java up "binnie.juicegrapefruit" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("juiceapricot"            , "Apricot Juice"                    , 300, 1000, 1000, 0xFFE8A83A), // FL.java up "binnie.juiceapricot" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("juicepear"               , "Pear Juice"                       , 300, 1000, 1000, 0xFFE8A83A), // FL.java up "binnie.juicepear" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("grapejuice"              , "Green Grape Juice"                , 300, 1000, 1000, 0xFFE8A83A), // FL.java up "grapejuice" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("grc_grapewine0"          , "Purple Grape Juice"               , 300, 1000, 1000, 0xFF7A2848), // FL.java up "grc.grapewine0" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("juiceredgrape"           , "Red Grape Juice"                  , 300, 1000, 1000, 0xFFE8A83A), // FL.java up "binnie.juiceredgrape" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("juicewhitegrape"         , "White Grape Juice"                , 300, 1000, 1000, 0xFFE8A83A), // FL.java up "binnie.juicewhitegrape" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("juiceapple"              , "Apple Juice"                      , 300, 1000, 1000, 0xFFE8A83A), // FL.java up "binnie.juiceapple" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("grc_applecider0"         , "Apple Juice"                      , 300, 1000, 1000, 0xFFE0C040), // FL.java up "grc.applecider0" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("juicepineapple"          , "Ananas Juice"                     , 300, 1000, 1000, 0xFFE8A83A), // FL.java up "binnie.juicepineapple" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("juicebanana"             , "Banana Juice"                     , 300, 1000, 1000, 0xFFE8A83A), // FL.java up "binnie.juicebanana" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("juicecherry"             , "Cherry Juice"                     , 300, 1000, 1000, 0xFFE8A83A), // FL.java up "binnie.juicecherry" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("juicecranberry"          , "Cranberry Juice"                  , 300, 1000, 1000, 0xFFE8A83A), // FL.java up "binnie.juicecranberry" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("cactusfruitjuice"        , "Cactus Fruit Juice"               , 300, 1000, 1000, 0xFFE8A83A), // FL.java up "cactusfruitjuice" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("mangojuice"              , "Mango Juice"                      , 300, 1000, 1000, 0xFFE8A83A), // FL.java up "mangojuice" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("pomegranatejuice"        , "Pomegranate Juice"                , 300, 1000, 1000, 0xFFE8A83A), // FL.java up "pomegranatejuice" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("starfruitjuice"          , "Starfruit Juice"                  , 300, 1000, 1000, 0xFFE8A83A), // FL.java up "starfruitjuice" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("papayajuice"             , "Papaya Juice"                     , 300, 1000, 1000, 0xFFE8A83A), // FL.java up "papayajuice" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("figjuice"                , "Fig Juice"                        , 300, 1000, 1000, 0xFFE8A83A), // FL.java up "figjuice" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("coconutmilk"             , "Coconut Milk"                     , 300, 1000, 1000, 0xFFF8F8F4), // FL.java up "coconutmilk" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("datejuice"               , "Date Juice"                       , 300, 1000, 1000, 0xFFE8A83A), // FL.java up "datejuice" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("juicecarrot"             , "Carrot Juice"                     , 300, 1000, 1000, 0xFFE8A83A), // FL.java up "binnie.juicecarrot" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("juicetomato"             , "Tomato Juice"                     , 300, 1000, 1000, 0xFFE8A83A), // FL.java up "binnie.juicetomato" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("beetjuice"               , "Beet Juice"                       , 300, 1000, 1000, 0xFFE8A83A), // FL.java up "beetjuice" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("pumpkinjuice"            , "Pumpkin Juice"                    , 300, 1000, 1000, 0xFFE8A83A), // FL.java up "pumpkinjuice" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("cucumberjuice"           , "Cucumber Juice"                   , 300, 1000, 1000, 0xFFE8A83A), // FL.java up "cucumberjuice" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("onionjuice"              , "Onion Juice"                      , 300, 1000, 1000, 0xFFE8A83A), // FL.java up "onionjuice" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("potatojuice"             , "Potato Juice"                     , 300, 1000, 1000, 0xFFE8A83A), // FL.java up "potatojuice" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("ricewater"               , "Ricewater"                        , 300, 1000, 1000, 0xFFD8A858), // FL.java up "ricewater" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("hopsmash"                , "Hops Mash"                        , 300, 1000, 1000, 0xFFD8C890), // FL.java up "hopsmash" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("wheathopsmash"           , "Wheat-Hops Mash"                  , 300, 1000, 1000, 0xFFD8C890), // FL.java up "wheathopsmash" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("mashwheat"               , "Wheat Mash"                       , 300, 1000, 1000, 0xFFD8C890), // FL.java up "binnie.mashwheat" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("mashcorn"                , "Corn Mash"                        , 300, 1000, 1000, 0xFFD8C890), // FL.java up "binnie.mashcorn" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("mashrye"                 , "Rye Mash"                         , 300, 1000, 1000, 0xFFD8C890), // FL.java up "binnie.mashrye" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("mashgrain"               , "Grain Mash"                       , 300, 1000, 1000, 0xFFD8C890), // FL.java up "binnie.mashgrain" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("maplesyrup"              , "Maple Syrup"                      , 300, 1000, 1000, 0xFFB86818), // FL.java up "maplesyrup" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("peanutbutter"            , "Peanut Butter"                    , 300, 1000, 1000, 0xFFD8A850), // FL.java up "peanutbutter" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("grcmilk_cream"           , "Heavy Cream"                      , 300, 1000, 1000, 0xFFF8F0E0), // FL.java up "grcmilk.cream" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("chocolatecream"          , "Chocolate Cream"                  , 300, 1000, 1000, 0xFFF8F0E0), // FL.java up "chocolatecream" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("coconutcream"            , "Coconut Cream"                    , 300, 1000, 1000, 0xFFF8F0E0), // FL.java up "coconutcream" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("nutella"                 , "Nutella"                          , 300, 1000, 1000, 0xFF7A4A20), // FL.java up "nutella" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("ketchup"                 , "Tomato Ketchup"                   , 300, 1000, 1000, 0xFFB82818), // FL.java up "ketchup" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("mayo"                    , "Mayonnaise"                       , 300, 1000, 1000, 0xFFF8F0D8), // FL.java up "mayo" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("dressing"                , "Dressing"                         , 300, 1000, 1000, 0xFFE8D8A0), // FL.java up "potion.dressing" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("mushroomsoup"            , "Mushroom Stew"                    , 300, 1000, 1000, 0xFF9A7848), // FL.java up "mushroomsoup" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("blood"                   , "Blood"                            , 300, 1000, 1000, 0xFF8A1010), // FL.java up "blood" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("chillysauce"             , "Chili Sauce"                      , 300, 1000, 1000, 0xFFB82818), // FL.java up "chillysauce" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("hotsauce"                , "Hot Sauce"                        , 300, 1000, 1000, 0xFFB82818), // FL.java up "potion.hotsauce" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("diabolosauce"            , "Diabolo Sauce"                    , 300, 1000, 1000, 0xFFB82818), // FL.java up "potion.diabolosauce" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("diablosauce"             , "Diablo Sauce"                     , 300, 1000, 1000, 0xFFB82818), // FL.java up "potion.diablosauce" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("diablosauce_strong"      , "There is no Cow Sauce"            , 300, 1000, 1000, 0xFFB82818), // FL.java up "potion.diablosauce.strong" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("bbqsauce"                , "Barbecue Sauce"                   , 300, 1000, 1000, 0xFFB82818), // FL.java up "bbqsauce" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("slime_blue"              , "Blue Slime"                       , 300, 1000, 1000, 0xFF6FA8E8), // FL.java up "slime.blue" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("pinkslime"               , "Pink Slime"                       , 300, 1000, 1000, 0xFFE89AB8), // FL.java up "pinkslime" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("slime"                   , "Slime"                            , 300, 1000, 1000, 0xFF7FC85A), // FL.java up "slime" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("bawls"                   , "BAWLS"                            , 275, 1000, 1000, 0xFFD8E8F8), // FL.java up "bawls" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("tea"                     , "Tea"                              , 300, 1000, 1000, 0xFFC87C28), // FL.java up "tea" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("sweettea"                , "Sweet Tea"                        , 300, 1000, 1000, 0xFFC87C28), // FL.java up "sweettea" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("icetea"                  , "Ice Tea"                          , 255, 1000, 1000, 0xFFC87C28), // FL.java up "icetea" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("purpledrink"             , "Purple Drink"                     , 300, 1000, 1000, 0xFF9A48D8), // FL.java up "purpledrink" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("lemonade"                , "Lemonade"                         , 275, 1000, 1000, 0xFFE8E86A), // FL.java up "potion.lemonade" — Loader_Fluids.java:603 the FL.create 275 K carrier verbatim (the p33-food-tail correction: the row shipped the 300 K honest-default before the :603 literal was pinned; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("cavejohnsonsgrenadejuice", "Cave Johnson's Grenade Juice"     , 300, 1000, 1000, 0xFFE8A83A), // FL.java up "potion.cavejohnsonsgrenadejuice" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("vinegar"                 , "Grape Vinegar"                    , 300, 1000, 1000, 0xFFD8A858), // FL.java up "vinegar" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("applevinegar"            , "Apple Cider Vinegar"              , 300, 1000, 1000, 0xFFD8A858), // FL.java up "applevinegar" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("canevinegar"             , "Cane Vinegar"                     , 300, 1000, 1000, 0xFFD8A858), // FL.java up "canevinegar" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("ricevinegar"             , "Rice Vinegar"                     , 300, 1000, 1000, 0xFFD8A858), // FL.java up "ricevinegar" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("juice_wine_fruit"        , "Fruit Wine"                       , 300, 1000, 1000, 0xFF7A2848), // FL.java up "binnie.juice" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("limoncello"              , "Limoncello"                       , 300, 1000, 1000, 0xFFD8A858), // FL.java up "limoncello" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("wineagave"               , "Agave Wine"                       , 300, 1000, 1000, 0xFF7A2848), // FL.java up "binnie.wineagave" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("wineapricot"             , "Apricot Wine"                     , 300, 1000, 1000, 0xFF7A2848), // FL.java up "binnie.wineapricot" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("winebanana"              , "Banana Wine"                      , 300, 1000, 1000, 0xFF7A2848), // FL.java up "binnie.winebanana" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("winecarrot"              , "Carrot Wine"                      , 300, 1000, 1000, 0xFF7A2848), // FL.java up "binnie.winecarrot" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("winecherry"              , "Cherry Wine"                      , 300, 1000, 1000, 0xFF7A2848), // FL.java up "binnie.winecherry" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("winecitrus"              , "Citrus Wine"                      , 300, 1000, 1000, 0xFF7A2848), // FL.java up "binnie.winecitrus" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("winecranberry"           , "Cranberry Wine"                   , 300, 1000, 1000, 0xFF7A2848), // FL.java up "binnie.winecranberry" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("wineelderberry"          , "Elderberry Wine"                  , 300, 1000, 1000, 0xFF7A2848), // FL.java up "binnie.wineelderberry" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("wineplum"                , "Plum Wine"                        , 300, 1000, 1000, 0xFF7A2848), // FL.java up "binnie.wineplum" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("winesparkling"           , "Sparkling Wine"                   , 300, 1000, 1000, 0xFF7A2848), // FL.java up "binnie.winesparkling" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("winetomato"              , "Tomato Wine"                      , 300, 1000, 1000, 0xFF7A2848), // FL.java up "binnie.winetomato" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("wine"                    , "Wine"                             , 300, 1000, 1000, 0xFF7A2848), // FL.java up "wine" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("ricardosanchez"          , "Ricardo Sanchez"                  , 300, 1000, 1000, 0xFFD8A858), // FL.java up "ricardosanchez" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("winered"                 , "Red Wine"                         , 300, 1000, 1000, 0xFF7A2848), // FL.java up "binnie.winered" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("winewhite"               , "White Wine"                       , 300, 1000, 1000, 0xFF7A2848), // FL.java up "binnie.winewhite" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("winefortified"           , "Fortified Wine"                   , 300, 1000, 1000, 0xFF7A2848), // FL.java up "binnie.winefortified" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("whiskey"                 , "Whiskey"                          , 300, 1000, 1000, 0xFFC89030), // FL.java up "binnie.whiskey" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("whiskeyrye"              , "Rye Whiskey"                      , 300, 1000, 1000, 0xFFC89030), // FL.java up "binnie.whiskeyrye" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("whiskeycorn"             , "Corn Whiskey"                     , 300, 1000, 1000, 0xFFC89030), // FL.java up "binnie.whiskeycorn" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("whiskeywheat"            , "Scotch"                           , 275, 1000, 1000, 0xFFC89030), // FL.java up "binnie.whiskeywheat" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("glenmckenner"            , "Glen McKenner"                    , 275, 1000, 1000, 0xFFD8A858), // FL.java up "glenmckenner" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("liqueurchocolate"        , "Chocolate Liqueur"                , 300, 1000, 1000, 0xFFC86878), // FL.java up "binnie.liqueurchocolate" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("liqueuralmond"           , "Almond Liqueur"                   , 300, 1000, 1000, 0xFFC86878), // FL.java up "binnie.liqueuralmond" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("liqueuranise"            , "Anise Liqueur"                    , 300, 1000, 1000, 0xFFC86878), // FL.java up "binnie.liqueuranise" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("liqueurbanana"           , "Banana Liqueur"                   , 300, 1000, 1000, 0xFFC86878), // FL.java up "binnie.liqueurbanana" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("liqueurblackberry"       , "Blackberry Liqueur"               , 300, 1000, 1000, 0xFFC86878), // FL.java up "binnie.liqueurblackberry" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("liqueurblackcurrant"     , "Blackcurrant Liqueur"             , 300, 1000, 1000, 0xFFC86878), // FL.java up "binnie.liqueurblackcurrant" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("liqueurcherry"           , "Cherry Liqueur"                   , 300, 1000, 1000, 0xFFC86878), // FL.java up "binnie.liqueurcherry" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("liqueurcinnamon"         , "Cinnamon Liqueur"                 , 300, 1000, 1000, 0xFFC86878), // FL.java up "binnie.liqueurcinnamon" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("liqueurcoffee"           , "Coffee Liqueur"                   , 300, 1000, 1000, 0xFFC86878), // FL.java up "binnie.liqueurcoffee" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("liqueurhazelnut"         , "Hazelnut Liqueur"                 , 300, 1000, 1000, 0xFFC86878), // FL.java up "binnie.liqueurhazelnut" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("liqueurherbal"           , "Herbal Liqueur"                   , 300, 1000, 1000, 0xFFC86878), // FL.java up "binnie.liqueurherbal" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("liqueurlemon"            , "Lemon Liqueur"                    , 300, 1000, 1000, 0xFFC86878), // FL.java up "binnie.liqueurlemon" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("liqueurmelon"            , "Melon Liqueur"                    , 300, 1000, 1000, 0xFFC86878), // FL.java up "binnie.liqueurmelon" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("liqueurmint"             , "Mint Liqueur"                     , 300, 1000, 1000, 0xFFC86878), // FL.java up "binnie.liqueurmint" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("liqueurorange"           , "Orange Liqueur"                   , 300, 1000, 1000, 0xFFC86878), // FL.java up "binnie.liqueurorange" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("liqueurpeach"            , "Peach Liqueur"                    , 300, 1000, 1000, 0xFFC86878), // FL.java up "binnie.liqueurpeach" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("liqueurraspberry"        , "Raspberry Liqueur"                , 300, 1000, 1000, 0xFFC86878), // FL.java up "binnie.liqueurraspberry" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("liquorfruit"             , "Fruit Liquor"                     , 300, 1000, 1000, 0xFFC87840), // FL.java up "binnie.liquorfruit" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("liquorapple"             , "Apple Liquor"                     , 300, 1000, 1000, 0xFFC87840), // FL.java up "binnie.liquorapple" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("liquorapricot"           , "Apricot Liquor"                   , 300, 1000, 1000, 0xFFC87840), // FL.java up "binnie.liquorapricot" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("liquorcherry"            , "Cherry Liquor"                    , 300, 1000, 1000, 0xFFC87840), // FL.java up "binnie.liquorcherry" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("liquorelderberry"        , "Elderberry Liquor"                , 300, 1000, 1000, 0xFFC87840), // FL.java up "binnie.liquorelderberry" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("liquorpear"              , "Pear Liquor"                      , 300, 1000, 1000, 0xFFC87840), // FL.java up "binnie.liquorpear" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("spiritgin"               , "Gin"                              , 300, 1000, 1000, 0xFFE0EDE8), // FL.java up "binnie.spiritgin" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("spiritneutral"           , "Neutral Spirit"                   , 300, 1000, 1000, 0xFFD8A858), // FL.java up "binnie.spiritneutral" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("spiritsugarcane"         , "Cane Spirit"                      , 300, 1000, 1000, 0xFFD8A858), // FL.java up "binnie.spiritsugarcane" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("brandyfruit"             , "Brandy"                           , 300, 1000, 1000, 0xFFB86830), // FL.java up "binnie.brandyfruit" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("brandyapple"             , "Apple Brandy"                     , 300, 1000, 1000, 0xFFB86830), // FL.java up "binnie.brandyapple" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("brandyapricot"           , "Apricot Brandy"                   , 300, 1000, 1000, 0xFFB86830), // FL.java up "binnie.brandyapricot" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("brandycherry"            , "Cherry Brandy"                    , 300, 1000, 1000, 0xFFB86830), // FL.java up "binnie.brandycherry" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("brandycitrus"            , "Citrus Brandy"                    , 300, 1000, 1000, 0xFFB86830), // FL.java up "binnie.brandycitrus" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("brandyelderberry"        , "Elderberry Brandy"                , 300, 1000, 1000, 0xFFB86830), // FL.java up "binnie.brandyelderberry" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("brandygrape"             , "Grape Brandy"                     , 300, 1000, 1000, 0xFFB86830), // FL.java up "binnie.brandygrape" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("brandypear"              , "Pear Brandy"                      , 300, 1000, 1000, 0xFFB86830), // FL.java up "binnie.brandypear" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("brandyplum"              , "Plum Brandy"                      , 300, 1000, 1000, 0xFFB86830), // FL.java up "binnie.brandyplum" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("ciderapple"              , "Cider"                            , 300, 1000, 1000, 0xFFE0C040), // FL.java up "binnie.ciderapple" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("ciderpear"               , "Pear Cider"                       , 300, 1000, 1000, 0xFFE0C040), // FL.java up "binnie.ciderpear" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("ciderpeach"              , "Peach Cider"                      , 300, 1000, 1000, 0xFFE0C040), // FL.java up "binnie.ciderpeach" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("winepineapple"           , "Ananas Cider"                     , 300, 1000, 1000, 0xFF7A2848), // FL.java up "binnie.winepineapple" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("beer"                    , "Beer"                             , 300, 1000, 1000, 0xFFE8B428), // FL.java up "beer" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("darkbeer"                , "Dark Beer"                        , 300, 1000, 1000, 0xFFE8B428), // FL.java up "darkbeer" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("dragonblood"             , "Dragon Blood"                     , 300, 1000, 1000, 0xFF8A1010), // FL.java up "potion.dragonblood" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("beerale"                 , "Ale"                              , 300, 1000, 1000, 0xFFE8B428), // FL.java up "binnie.beerale" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("beercorn"                , "Corn Beer"                        , 300, 1000, 1000, 0xFFE8B428), // FL.java up "binnie.beercorn" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("beerlager"               , "Lager"                            , 300, 1000, 1000, 0xFFE8B428), // FL.java up "binnie.beerlager" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("beerrye"                 , "Rye Beer"                         , 300, 1000, 1000, 0xFFE8B428), // FL.java up "binnie.beerrye" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("beerstout"               , "Stout"                            , 300, 1000, 1000, 0xFFE8B428), // FL.java up "binnie.beerstout" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("beerwheat"               , "Wheat Beer"                       , 300, 1000, 1000, 0xFFE8B428), // FL.java up "binnie.beerwheat" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("rumwhite"                , "Rum"                              , 300, 1000, 1000, 0xFF8A5020), // FL.java up "binnie.rumwhite" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("rumdark"                 , "Pirate Brew"                      , 300, 1000, 1000, 0xFF8A5020), // FL.java up "binnie.rumdark" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("pina_colada"             , "Piña Colada"                      , 300, 1000, 1000, 0xFFF8F0E0), // FL.java up "pina.colada" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("vodka"                   , "Vodka"                            , 275, 1000, 1000, 0xFFE8F0F4), // FL.java up "binnie.vodka" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("leninade"                , "Leninade"                         , 300, 1000, 1000, 0xFFD84040), // FL.java up "potion.leninade" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("mead"                    , "Mead"                             , 300, 1000, 1000, 0xFFE8B428), // FL.java up "mead" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("short_mead"              , "Short Mead"                       , 300, 1000, 1000, 0xFFE8B428), // FL.java up "short.mead" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("sake"                    , "Sake"                             , 300, 1000, 1000, 0xFFE8ECE0), // FL.java up "potion.sake" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("tequila"                 , "Tequila"                          , 300, 1000, 1000, 0xFFE8E4C0), // FL.java up "binnie.tequila" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("alcopops"                , "Alcopops"                         , 300, 1000, 1000, 0xFFE8A8C8), // FL.java up "potion.alcopops" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("hotfryingoil"            , "Hot Frying Oil"                   , 400, 1000, 1000, 0xFFE8D040), // FL.java up "hotfryingoil" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("seedoil"                 , "Seed Oil"                         , 300, 1000, 1000, 0xFFE8D040), // FL.java up "seedoil" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("plantoil"                , "Plant Oil"                        , 300, 1000, 1000, 0xFFE8D040), // FL.java up "plantoil" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("sunfloweroil"            , "Sunflower Oil"                    , 300, 1000, 1000, 0xFFE8D040), // FL.java up "sunfloweroil" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("juiceolive"              , "Olive Oil"                        , 300, 1000, 1000, 0xFFE8A83A), // FL.java up "binnie.juiceolive" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("nutoil"                  , "Nut Oil"                          , 300, 1000, 1000, 0xFFE8D040), // FL.java up "nutoil" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("linoil"                  , "Lin Oil"                          , 300, 1000, 1000, 0xFFE8D040), // FL.java up "linoil" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("hempoil"                 , "Hemp Oil"                         , 300, 1000, 1000, 0xFFE8D040), // FL.java up "hempoil" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("fishoil"                 , "Fish Oil"                         , 300, 1000, 1000, 0xFFE8D040), // FL.java up "fishoil" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)
		new AquaFluidSpec("whaleoil"                , "Whale Oil"                        , 300, 1000, 1000, 0xFFE8D040)); // FL.java up "whaleoil" (the FL.create carrier / the honest-default row; tint port-declared, the JetFuel/aqua precedent)

	/** The food-b1 row for a gt6 id path, or null (the {@link #foodSpec} lookup shape, its own table). */
	public static AquaFluidSpec foodB1Spec(String aName) {
		for (AquaFluidSpec tSpec : FOOD_B1_SPECS) if (tSpec.name().equals(aName)) return tSpec;
		return null;
	}

	/** The registration helper for one food-b1 row (the {@link #foodFluid} shape over the fifth table). */
	private static AquaFluid foodB1Fluid(String aName) {
		AquaFluidSpec tSpec = foodB1Spec(aName);
		if (tSpec == null) throw new IllegalArgumentException("no food-b1 fluid spec: " + aName);
		return registerFluidFamily(tSpec);
	}

	// ponytail: 220 one-line static registrations instead of a loop — the static-init
	// DeferredRegister accumulation IS the registration mechanism (the entries fire with
	// the existing onModConstruct, types before fluids); a lazy loop would register after
	// the registry events and throw. Upgrade path: a DR-friendly batch API if Forge adds one.

	public static final AquaFluid MINERALSODA              = foodB1Fluid("mineralsoda");
	public static final AquaFluid SODA                     = foodB1Fluid("soda");
	public static final AquaFluid MILK                     = foodB1Fluid("milk");
	public static final AquaFluid SOYMILK                  = foodB1Fluid("soymilk");
	public static final AquaFluid GRCMILK_MILK             = foodB1Fluid("grcmilk_milk");
	public static final AquaFluid SPOILEDMILK              = foodB1Fluid("spoiledmilk");
	public static final AquaFluid FOR_HONEY                = foodB1Fluid("for_honey");
	public static final AquaFluid GRC_HONEY                = foodB1Fluid("grc_honey");
	public static final AquaFluid FRUITSMOOTHIE            = foodB1Fluid("fruitsmoothie");
	public static final AquaFluid MELONSMOOTHIE            = foodB1Fluid("melonsmoothie");
	public static final AquaFluid KIWISMOOTHIE             = foodB1Fluid("kiwismoothie");
	public static final AquaFluid CURRANTSMOOTHIE          = foodB1Fluid("currantsmoothie");
	public static final AquaFluid RASPBERRYSMOOTHIE        = foodB1Fluid("raspberrysmoothie");
	public static final AquaFluid BLACKBERRYSMOOTHIE       = foodB1Fluid("blackberrysmoothie");
	public static final AquaFluid BLUEBERRYSMOOTHIE        = foodB1Fluid("blueberrysmoothie");
	public static final AquaFluid GOOSEBERRYSMOOTHIE       = foodB1Fluid("gooseberrysmoothie");
	public static final AquaFluid STRAWBERRYSMOOTHIE       = foodB1Fluid("strawberrysmoothie");
	public static final AquaFluid PLUMSMOOTHIE             = foodB1Fluid("plumsmoothie");
	public static final AquaFluid PEACHSMOOTHIE            = foodB1Fluid("peachsmoothie");
	public static final AquaFluid ELDERBERRYSMOOTHIE       = foodB1Fluid("elderberrysmoothie");
	public static final AquaFluid GRAPEFRUITSMOOTHIE       = foodB1Fluid("grapefruitsmoothie");
	public static final AquaFluid LIMESMOOTHIE             = foodB1Fluid("limesmoothie");
	public static final AquaFluid ORANGESMOOTHIE           = foodB1Fluid("orangesmoothie");
	public static final AquaFluid PERSIMMONSMOOTHIE        = foodB1Fluid("persimmonsmoothie");
	public static final AquaFluid APRICOTSMOOTHIE          = foodB1Fluid("apricotsmoothie");
	public static final AquaFluid PEARSMOOTHIE             = foodB1Fluid("pearsmoothie");
	public static final AquaFluid REDGRAPESMOOTHIE         = foodB1Fluid("redgrapesmoothie");
	public static final AquaFluid WHITEGRAPESMOOTHIE       = foodB1Fluid("whitegrapesmoothie");
	public static final AquaFluid GRAPESMOOTHIE            = foodB1Fluid("grapesmoothie");
	public static final AquaFluid PURPLEGRAPESMOOTHIE      = foodB1Fluid("purplegrapesmoothie");
	public static final AquaFluid APPLESMOOTHIE            = foodB1Fluid("applesmoothie");
	public static final AquaFluid PINEAPPLESMOOTHIE        = foodB1Fluid("pineapplesmoothie");
	public static final AquaFluid BANANASMOOTHIE           = foodB1Fluid("bananasmoothie");
	public static final AquaFluid CHERRYSMOOTHIE           = foodB1Fluid("cherrysmoothie");
	public static final AquaFluid CRANBERRYSMOOTHIE        = foodB1Fluid("cranberrysmoothie");
	public static final AquaFluid LEMONSMOOTHIE            = foodB1Fluid("lemonsmoothie");
	public static final AquaFluid MANGOSMOOTHIE            = foodB1Fluid("mangosmoothie");
	public static final AquaFluid POMEGRANATESMOOTHIE      = foodB1Fluid("pomegranatesmoothie");
	public static final AquaFluid STARFRUITSMOOTHIE        = foodB1Fluid("starfruitsmoothie");
	public static final AquaFluid PAPAYASMOOTHIE           = foodB1Fluid("papayasmoothie");
	public static final AquaFluid FIGSMOOTHIE              = foodB1Fluid("figsmoothie");
	public static final AquaFluid COCONUTSMOOTHIE          = foodB1Fluid("coconutsmoothie");
	public static final AquaFluid JUICE_JUICE              = foodB1Fluid("juice_juice");
	public static final AquaFluid KIWIJUICE                = foodB1Fluid("kiwijuice");
	public static final AquaFluid JUICELIME                = foodB1Fluid("juicelime");
	public static final AquaFluid JUICELEMON               = foodB1Fluid("juicelemon");
	public static final AquaFluid JUICEORANGE              = foodB1Fluid("juiceorange");
	public static final AquaFluid PERSIMMONJUICE           = foodB1Fluid("persimmonjuice");
	public static final AquaFluid MELONJUICE               = foodB1Fluid("melonjuice");
	public static final AquaFluid CURRANTJUICE             = foodB1Fluid("currantjuice");
	public static final AquaFluid RASPBERRYJUICE           = foodB1Fluid("raspberryjuice");
	public static final AquaFluid BLACKBERRYJUICE          = foodB1Fluid("blackberryjuice");
	public static final AquaFluid BLUEBERRYJUICE           = foodB1Fluid("blueberryjuice");
	public static final AquaFluid GOOSEBERRYJUICE          = foodB1Fluid("gooseberryjuice");
	public static final AquaFluid STRAWBERRYJUICE          = foodB1Fluid("strawberryjuice");
	public static final AquaFluid JUICEPLUM                = foodB1Fluid("juiceplum");
	public static final AquaFluid JUICEPEACH               = foodB1Fluid("juicepeach");
	public static final AquaFluid JUICEELDERBERRY          = foodB1Fluid("juiceelderberry");
	public static final AquaFluid HELLDERBERRYJUICE        = foodB1Fluid("hellderberryjuice");
	public static final AquaFluid JUICEGRAPEFRUIT          = foodB1Fluid("juicegrapefruit");
	public static final AquaFluid JUICEAPRICOT             = foodB1Fluid("juiceapricot");
	public static final AquaFluid JUICEPEAR                = foodB1Fluid("juicepear");
	public static final AquaFluid GRAPEJUICE               = foodB1Fluid("grapejuice");
	public static final AquaFluid GRC_GRAPEWINE0           = foodB1Fluid("grc_grapewine0");
	public static final AquaFluid JUICEREDGRAPE            = foodB1Fluid("juiceredgrape");
	public static final AquaFluid JUICEWHITEGRAPE          = foodB1Fluid("juicewhitegrape");
	public static final AquaFluid JUICEAPPLE               = foodB1Fluid("juiceapple");
	public static final AquaFluid GRC_APPLECIDER0          = foodB1Fluid("grc_applecider0");
	public static final AquaFluid JUICEPINEAPPLE           = foodB1Fluid("juicepineapple");
	public static final AquaFluid JUICEBANANA              = foodB1Fluid("juicebanana");
	public static final AquaFluid JUICECHERRY              = foodB1Fluid("juicecherry");
	public static final AquaFluid JUICECRANBERRY           = foodB1Fluid("juicecranberry");
	public static final AquaFluid CACTUSFRUITJUICE         = foodB1Fluid("cactusfruitjuice");
	public static final AquaFluid MANGOJUICE               = foodB1Fluid("mangojuice");
	public static final AquaFluid POMEGRANATEJUICE         = foodB1Fluid("pomegranatejuice");
	public static final AquaFluid STARFRUITJUICE           = foodB1Fluid("starfruitjuice");
	public static final AquaFluid PAPAYAJUICE              = foodB1Fluid("papayajuice");
	public static final AquaFluid FIGJUICE                 = foodB1Fluid("figjuice");
	public static final AquaFluid COCONUTMILK              = foodB1Fluid("coconutmilk");
	public static final AquaFluid DATEJUICE                = foodB1Fluid("datejuice");
	public static final AquaFluid JUICECARROT              = foodB1Fluid("juicecarrot");
	public static final AquaFluid JUICETOMATO              = foodB1Fluid("juicetomato");
	public static final AquaFluid BEETJUICE                = foodB1Fluid("beetjuice");
	public static final AquaFluid PUMPKINJUICE             = foodB1Fluid("pumpkinjuice");
	public static final AquaFluid CUCUMBERJUICE            = foodB1Fluid("cucumberjuice");
	public static final AquaFluid ONIONJUICE               = foodB1Fluid("onionjuice");
	public static final AquaFluid POTATOJUICE              = foodB1Fluid("potatojuice");
	public static final AquaFluid RICEWATER                = foodB1Fluid("ricewater");
	public static final AquaFluid HOPSMASH                 = foodB1Fluid("hopsmash");
	public static final AquaFluid WHEATHOPSMASH            = foodB1Fluid("wheathopsmash");
	public static final AquaFluid MASHWHEAT                = foodB1Fluid("mashwheat");
	public static final AquaFluid MASHCORN                 = foodB1Fluid("mashcorn");
	public static final AquaFluid MASHRYE                  = foodB1Fluid("mashrye");
	public static final AquaFluid MASHGRAIN                = foodB1Fluid("mashgrain");
	public static final AquaFluid MAPLESYRUP               = foodB1Fluid("maplesyrup");
	public static final AquaFluid PEANUTBUTTER             = foodB1Fluid("peanutbutter");
	public static final AquaFluid GRCMILK_CREAM            = foodB1Fluid("grcmilk_cream");
	public static final AquaFluid CHOCOLATECREAM           = foodB1Fluid("chocolatecream");
	public static final AquaFluid COCONUTCREAM             = foodB1Fluid("coconutcream");
	public static final AquaFluid NUTELLA                  = foodB1Fluid("nutella");
	public static final AquaFluid KETCHUP                  = foodB1Fluid("ketchup");
	public static final AquaFluid MAYO                     = foodB1Fluid("mayo");
	public static final AquaFluid DRESSING                 = foodB1Fluid("dressing");
	public static final AquaFluid MUSHROOMSOUP             = foodB1Fluid("mushroomsoup");
	public static final AquaFluid BLOOD                    = foodB1Fluid("blood");
	public static final AquaFluid CHILLYSAUCE              = foodB1Fluid("chillysauce");
	public static final AquaFluid HOTSAUCE                 = foodB1Fluid("hotsauce");
	public static final AquaFluid DIABOLOSAUCE             = foodB1Fluid("diabolosauce");
	public static final AquaFluid DIABLOSAUCE              = foodB1Fluid("diablosauce");
	public static final AquaFluid DIABLOSAUCE_STRONG       = foodB1Fluid("diablosauce_strong");
	public static final AquaFluid BBQSAUCE                 = foodB1Fluid("bbqsauce");
	public static final AquaFluid SLIME_BLUE               = foodB1Fluid("slime_blue");
	public static final AquaFluid PINKSLIME                = foodB1Fluid("pinkslime");
	public static final AquaFluid SLIME                    = foodB1Fluid("slime");
	public static final AquaFluid BAWLS                    = foodB1Fluid("bawls");
	public static final AquaFluid TEA                      = foodB1Fluid("tea");
	public static final AquaFluid SWEETTEA                 = foodB1Fluid("sweettea");
	public static final AquaFluid ICETEA                   = foodB1Fluid("icetea");
	public static final AquaFluid PURPLEDRINK              = foodB1Fluid("purpledrink");
	public static final AquaFluid LEMONADE                 = foodB1Fluid("lemonade");
	public static final AquaFluid CAVEJOHNSONSGRENADEJUICE = foodB1Fluid("cavejohnsonsgrenadejuice");
	public static final AquaFluid VINEGAR                  = foodB1Fluid("vinegar");
	public static final AquaFluid APPLEVINEGAR             = foodB1Fluid("applevinegar");
	public static final AquaFluid CANEVINEGAR              = foodB1Fluid("canevinegar");
	public static final AquaFluid RICEVINEGAR              = foodB1Fluid("ricevinegar");
	public static final AquaFluid JUICE_WINE_FRUIT         = foodB1Fluid("juice_wine_fruit");
	public static final AquaFluid LIMONCELLO               = foodB1Fluid("limoncello");
	public static final AquaFluid WINEAGAVE                = foodB1Fluid("wineagave");
	public static final AquaFluid WINEAPRICOT              = foodB1Fluid("wineapricot");
	public static final AquaFluid WINEBANANA               = foodB1Fluid("winebanana");
	public static final AquaFluid WINECARROT               = foodB1Fluid("winecarrot");
	public static final AquaFluid WINECHERRY               = foodB1Fluid("winecherry");
	public static final AquaFluid WINECITRUS               = foodB1Fluid("winecitrus");
	public static final AquaFluid WINECRANBERRY            = foodB1Fluid("winecranberry");
	public static final AquaFluid WINEELDERBERRY           = foodB1Fluid("wineelderberry");
	public static final AquaFluid WINEPLUM                 = foodB1Fluid("wineplum");
	public static final AquaFluid WINESPARKLING            = foodB1Fluid("winesparkling");
	public static final AquaFluid WINETOMATO               = foodB1Fluid("winetomato");
	public static final AquaFluid WINE                     = foodB1Fluid("wine");
	public static final AquaFluid RICARDOSANCHEZ           = foodB1Fluid("ricardosanchez");
	public static final AquaFluid WINERED                  = foodB1Fluid("winered");
	public static final AquaFluid WINEWHITE                = foodB1Fluid("winewhite");
	public static final AquaFluid WINEFORTIFIED            = foodB1Fluid("winefortified");
	public static final AquaFluid WHISKEY                  = foodB1Fluid("whiskey");
	public static final AquaFluid WHISKEYRYE               = foodB1Fluid("whiskeyrye");
	public static final AquaFluid WHISKEYCORN              = foodB1Fluid("whiskeycorn");
	public static final AquaFluid WHISKEYWHEAT             = foodB1Fluid("whiskeywheat");
	public static final AquaFluid GLENMCKENNER             = foodB1Fluid("glenmckenner");
	public static final AquaFluid LIQUEURCHOCOLATE         = foodB1Fluid("liqueurchocolate");
	public static final AquaFluid LIQUEURALMOND            = foodB1Fluid("liqueuralmond");
	public static final AquaFluid LIQUEURANISE             = foodB1Fluid("liqueuranise");
	public static final AquaFluid LIQUEURBANANA            = foodB1Fluid("liqueurbanana");
	public static final AquaFluid LIQUEURBLACKBERRY        = foodB1Fluid("liqueurblackberry");
	public static final AquaFluid LIQUEURBLACKCURRANT      = foodB1Fluid("liqueurblackcurrant");
	public static final AquaFluid LIQUEURCHERRY            = foodB1Fluid("liqueurcherry");
	public static final AquaFluid LIQUEURCINNAMON          = foodB1Fluid("liqueurcinnamon");
	public static final AquaFluid LIQUEURCOFFEE            = foodB1Fluid("liqueurcoffee");
	public static final AquaFluid LIQUEURHAZELNUT          = foodB1Fluid("liqueurhazelnut");
	public static final AquaFluid LIQUEURHERBAL            = foodB1Fluid("liqueurherbal");
	public static final AquaFluid LIQUEURLEMON             = foodB1Fluid("liqueurlemon");
	public static final AquaFluid LIQUEURMELON             = foodB1Fluid("liqueurmelon");
	public static final AquaFluid LIQUEURMINT              = foodB1Fluid("liqueurmint");
	public static final AquaFluid LIQUEURORANGE            = foodB1Fluid("liqueurorange");
	public static final AquaFluid LIQUEURPEACH             = foodB1Fluid("liqueurpeach");
	public static final AquaFluid LIQUEURRASPBERRY         = foodB1Fluid("liqueurraspberry");
	public static final AquaFluid LIQUORFRUIT              = foodB1Fluid("liquorfruit");
	public static final AquaFluid LIQUORAPPLE              = foodB1Fluid("liquorapple");
	public static final AquaFluid LIQUORAPRICOT            = foodB1Fluid("liquorapricot");
	public static final AquaFluid LIQUORCHERRY             = foodB1Fluid("liquorcherry");
	public static final AquaFluid LIQUORELDERBERRY         = foodB1Fluid("liquorelderberry");
	public static final AquaFluid LIQUORPEAR               = foodB1Fluid("liquorpear");
	public static final AquaFluid SPIRITGIN                = foodB1Fluid("spiritgin");
	public static final AquaFluid SPIRITNEUTRAL            = foodB1Fluid("spiritneutral");
	public static final AquaFluid SPIRITSUGARCANE          = foodB1Fluid("spiritsugarcane");
	public static final AquaFluid BRANDYFRUIT              = foodB1Fluid("brandyfruit");
	public static final AquaFluid BRANDYAPPLE              = foodB1Fluid("brandyapple");
	public static final AquaFluid BRANDYAPRICOT            = foodB1Fluid("brandyapricot");
	public static final AquaFluid BRANDYCHERRY             = foodB1Fluid("brandycherry");
	public static final AquaFluid BRANDYCITRUS             = foodB1Fluid("brandycitrus");
	public static final AquaFluid BRANDYELDERBERRY         = foodB1Fluid("brandyelderberry");
	public static final AquaFluid BRANDYGRAPE              = foodB1Fluid("brandygrape");
	public static final AquaFluid BRANDYPEAR               = foodB1Fluid("brandypear");
	public static final AquaFluid BRANDYPLUM               = foodB1Fluid("brandyplum");
	public static final AquaFluid CIDERAPPLE               = foodB1Fluid("ciderapple");
	public static final AquaFluid CIDERPEAR                = foodB1Fluid("ciderpear");
	public static final AquaFluid CIDERPEACH               = foodB1Fluid("ciderpeach");
	public static final AquaFluid WINEPINEAPPLE            = foodB1Fluid("winepineapple");
	public static final AquaFluid BEER                     = foodB1Fluid("beer");
	public static final AquaFluid DARKBEER                 = foodB1Fluid("darkbeer");
	public static final AquaFluid DRAGONBLOOD              = foodB1Fluid("dragonblood");
	public static final AquaFluid BEERALE                  = foodB1Fluid("beerale");
	public static final AquaFluid BEERCORN                 = foodB1Fluid("beercorn");
	public static final AquaFluid BEERLAGER                = foodB1Fluid("beerlager");
	public static final AquaFluid BEERRYE                  = foodB1Fluid("beerrye");
	public static final AquaFluid BEERSTOUT                = foodB1Fluid("beerstout");
	public static final AquaFluid BEERWHEAT                = foodB1Fluid("beerwheat");
	public static final AquaFluid RUMWHITE                 = foodB1Fluid("rumwhite");
	public static final AquaFluid RUMDARK                  = foodB1Fluid("rumdark");
	public static final AquaFluid PINA_COLADA              = foodB1Fluid("pina_colada");
	public static final AquaFluid VODKA                    = foodB1Fluid("vodka");
	public static final AquaFluid LENINADE                 = foodB1Fluid("leninade");
	public static final AquaFluid MEAD                     = foodB1Fluid("mead");
	public static final AquaFluid SHORT_MEAD               = foodB1Fluid("short_mead");
	public static final AquaFluid SAKE                     = foodB1Fluid("sake");
	public static final AquaFluid TEQUILA                  = foodB1Fluid("tequila");
	public static final AquaFluid ALCOPOPS                 = foodB1Fluid("alcopops");
	public static final AquaFluid HOTFRYINGOIL             = foodB1Fluid("hotfryingoil");
	public static final AquaFluid SEEDOIL                  = foodB1Fluid("seedoil");
	public static final AquaFluid PLANTOIL                 = foodB1Fluid("plantoil");
	public static final AquaFluid SUNFLOWEROIL             = foodB1Fluid("sunfloweroil");
	public static final AquaFluid JUICEOLIVE               = foodB1Fluid("juiceolive");
	public static final AquaFluid NUTOIL                   = foodB1Fluid("nutoil");
	public static final AquaFluid LINOIL                   = foodB1Fluid("linoil");
	public static final AquaFluid HEMPOIL                  = foodB1Fluid("hempoil");
	public static final AquaFluid FISHOIL                  = foodB1Fluid("fishoil");
	public static final AquaFluid WHALEOIL                 = foodB1Fluid("whaleoil");


	/**
	 * The food-fluid batch 2 (task p33-food-fluids-b2): the SIXTH AquaFluidSpec table —
	 * the drink-seam residual under the research.p33-r-food b1/b2 boundary: the 94 potion
	 * brews of the card-block FoodStatDrink run (Loader_Fluids.java:230-350, the
	 * potion.tainted..potion.invisibility.long.lingering census; the gold-apple rows
	 * :607-610 are OUTSIDE the card block — they landed on the seventh table,
	 * {@link #FOOD_TAIL_SPECS}, task p33-food-tail) plus the 9 FOOD-flag
	 * drink fluids b1's census left unregistered (riverwater :361 / ic2distilledwater
	 * :363-alias / rottendrink :626 / poison :629 / chocolatemilk :643 / goldencarrotjuice
	 * :606 / holywater :615 / medicine.heal :649 / medicine.laxative :650). Every value is
	 * the upstream FL.create carrier where GT6 defines the fluid: the brew block rides the
	 * 300 K literal (:230-294/:319-348), the fireresistance rows the 375 K literal
	 * (:271-274/:337-338), riverwater the C constant = 273 K (:361, CS.java:132), rottendrink
	 * the 275 K literal (:626, the icetea/bawls fold); ic2distilledwater rides the :75
	 * FL.create "Distilled Water" local name (no temp literal = the 300 K default, the
	 * engine-family distilled_water twin); poison has no FL.create — the honest-default 300 K
	 * row, the water_boiling precedent. The DRINK-side temperatures (the C+37 folds) are the
	 * GTDrinks.DrinkStat surface, NOT this carrier column. The tints are PORT-OWNED DECLARED
	 * VALUES (the JetFuel/aqua precedent): the potion block shares one texture upstream, the
	 * port differentiates per effect family over the vanilla-water layers. FLUID-ONLY (the
	 * p21 declaration): no LiquidBlock, no bucket, no bottle. NO drink behaviour on this
	 * table — the DrinkStat seam is GTDrinks (the mirror of the upstream DrinksGT.REGISTER,
	 * FoodStatDrink.java :35-83 semantics), keyed by the SAME gt6 registry paths.
	 */
	public static final List<AquaFluidSpec> FOOD_B2_SPECS = List.of(
		new AquaFluidSpec("potion.tainted", "Tainted Brew", 300, 1000, 1000, 0xFF6A3A8A), // Loader_Fluids.java:230
		new AquaFluidSpec("potion.awkward", "Awkward Brew", 300, 1000, 1000, 0xFFB070D8), // Loader_Fluids.java:232
		new AquaFluidSpec("potion.thick", "Thick Brew", 300, 1000, 1000, 0xFFB070D8), // Loader_Fluids.java:233
		new AquaFluidSpec("potion.mundane", "Mundane Brew", 300, 1000, 1000, 0xFFB070D8), // Loader_Fluids.java:234
		new AquaFluidSpec("potion.damage", "Harming Brew", 300, 1000, 1000, 0xFF8A2A2A), // Loader_Fluids.java:235
		new AquaFluidSpec("potion.damage.strong", "Strong Harming Brew", 300, 1000, 1000, 0xFF8A2A2A), // Loader_Fluids.java:236
		new AquaFluidSpec("potion.damage.splash", "Splash Harming Brew", 300, 1000, 1000, 0xFF8A2A2A), // Loader_Fluids.java:237
		new AquaFluidSpec("potion.damage.strong.splash", "Strong Splash Harming Brew", 300, 1000, 1000, 0xFF8A2A2A), // Loader_Fluids.java:238
		new AquaFluidSpec("potion.health", "Healing Brew", 300, 1000, 1000, 0xFFE85A7A), // Loader_Fluids.java:239
		new AquaFluidSpec("potion.health.strong", "Strong Healing Brew", 300, 1000, 1000, 0xFFE85A7A), // Loader_Fluids.java:240
		new AquaFluidSpec("potion.health.splash", "Splash Healing Brew", 300, 1000, 1000, 0xFFE85A7A), // Loader_Fluids.java:241
		new AquaFluidSpec("potion.health.strong.splash", "Strong Splash Healing Brew", 300, 1000, 1000, 0xFFE85A7A), // Loader_Fluids.java:242
		new AquaFluidSpec("potion.jump", "Jumpy Brew", 300, 1000, 1000, 0xFF9AE84A), // Loader_Fluids.java:243
		new AquaFluidSpec("potion.jump.strong", "Strong Jumpy Brew", 300, 1000, 1000, 0xFF9AE84A), // Loader_Fluids.java:244
		new AquaFluidSpec("potion.jump.splash", "Splash Jumpy Brew", 300, 1000, 1000, 0xFF9AE84A), // Loader_Fluids.java:245
		new AquaFluidSpec("potion.jump.strong.splash", "Strong Splash Jumpy Brew", 300, 1000, 1000, 0xFF9AE84A), // Loader_Fluids.java:246
		new AquaFluidSpec("potion.speed", "Swiftness Brew", 300, 1000, 1000, 0xFF7ACAE8), // Loader_Fluids.java:247
		new AquaFluidSpec("potion.speed.strong", "Strong Swiftness Brew", 300, 1000, 1000, 0xFF7ACAE8), // Loader_Fluids.java:248
		new AquaFluidSpec("potion.speed.long", "Stretched Swiftness Brew", 300, 1000, 1000, 0xFF7ACAE8), // Loader_Fluids.java:249
		new AquaFluidSpec("potion.speed.splash", "Splash Swiftness Brew", 300, 1000, 1000, 0xFF7ACAE8), // Loader_Fluids.java:250
		new AquaFluidSpec("potion.speed.strong.splash", "Strong Splash Swiftness Brew", 300, 1000, 1000, 0xFF7ACAE8), // Loader_Fluids.java:251
		new AquaFluidSpec("potion.speed.long.splash", "Stretched Splash Swiftness Brew", 300, 1000, 1000, 0xFF7ACAE8), // Loader_Fluids.java:252
		new AquaFluidSpec("potion.strength", "Strength Brew", 300, 1000, 1000, 0xFFC83A3A), // Loader_Fluids.java:253
		new AquaFluidSpec("potion.strength.strong", "Strong Strength Brew", 300, 1000, 1000, 0xFFC83A3A), // Loader_Fluids.java:254
		new AquaFluidSpec("potion.strength.long", "Stretched Strength Brew", 300, 1000, 1000, 0xFFC83A3A), // Loader_Fluids.java:255
		new AquaFluidSpec("potion.strength.splash", "Splash Strength Brew", 300, 1000, 1000, 0xFFC83A3A), // Loader_Fluids.java:256
		new AquaFluidSpec("potion.strength.strong.splash", "Strong Splash Strength Brew", 300, 1000, 1000, 0xFFC83A3A), // Loader_Fluids.java:257
		new AquaFluidSpec("potion.strength.long.splash", "Stretched Splash Strength Brew", 300, 1000, 1000, 0xFFC83A3A), // Loader_Fluids.java:258
		new AquaFluidSpec("potion.regen", "Regenerating Brew", 300, 1000, 1000, 0xFFE86A9A), // Loader_Fluids.java:259
		new AquaFluidSpec("potion.regen.strong", "Strong Regenerating Brew", 300, 1000, 1000, 0xFFE86A9A), // Loader_Fluids.java:260
		new AquaFluidSpec("potion.regen.long", "Stretched Regenerating Brew", 300, 1000, 1000, 0xFFE86A9A), // Loader_Fluids.java:261
		new AquaFluidSpec("potion.regen.splash", "Splash Regenerating Brew", 300, 1000, 1000, 0xFFE86A9A), // Loader_Fluids.java:262
		new AquaFluidSpec("potion.regen.strong.splash", "Strong Splash Regenerating Brew", 300, 1000, 1000, 0xFFE86A9A), // Loader_Fluids.java:263
		new AquaFluidSpec("potion.regen.long.splash", "Stretched Splash Regenerating Brew", 300, 1000, 1000, 0xFFE86A9A), // Loader_Fluids.java:264
		new AquaFluidSpec("potion.poison", "Poisonous Brew", 300, 1000, 1000, 0xFF4A8A2A), // Loader_Fluids.java:265
		new AquaFluidSpec("potion.poison.strong", "Strong Poisonous Brew", 300, 1000, 1000, 0xFF4A8A2A), // Loader_Fluids.java:266
		new AquaFluidSpec("potion.poison.long", "Stretched Poisonous Brew", 300, 1000, 1000, 0xFF4A8A2A), // Loader_Fluids.java:267
		new AquaFluidSpec("potion.poison.splash", "Splash Poisonous Brew", 300, 1000, 1000, 0xFF4A8A2A), // Loader_Fluids.java:268
		new AquaFluidSpec("potion.poison.strong.splash", "Strong Splash Poisonous Brew", 300, 1000, 1000, 0xFF4A8A2A), // Loader_Fluids.java:269
		new AquaFluidSpec("potion.poison.long.splash", "Stretched Splash Poisonous Brew", 300, 1000, 1000, 0xFF4A8A2A), // Loader_Fluids.java:270
		new AquaFluidSpec("potion.fireresistance", "Fire Resistant Brew", 375, 1000, 1000, 0xFFE8902A), // Loader_Fluids.java:271
		new AquaFluidSpec("potion.fireresistance.long", "Stretched Fire Resistant Brew", 375, 1000, 1000, 0xFFE8902A), // Loader_Fluids.java:272
		new AquaFluidSpec("potion.fireresistance.splash", "Splash Fire Resistant Brew", 375, 1000, 1000, 0xFFE8902A), // Loader_Fluids.java:273
		new AquaFluidSpec("potion.fireresistance.long.splash", "Stretched Splash Fire Resistant Brew", 375, 1000, 1000, 0xFFE8902A), // Loader_Fluids.java:274
		new AquaFluidSpec("potion.nightvision", "Night Vision Brew", 300, 1000, 1000, 0xFF1234C8), // Loader_Fluids.java:275
		new AquaFluidSpec("potion.nightvision.long", "Stretched Night Vision Brew", 300, 1000, 1000, 0xFF1234C8), // Loader_Fluids.java:276
		new AquaFluidSpec("potion.nightvision.splash", "Splash Night Vision Brew", 300, 1000, 1000, 0xFF1234C8), // Loader_Fluids.java:277
		new AquaFluidSpec("potion.nightvision.long.splash", "Stretched Splash Night Vision Brew", 300, 1000, 1000, 0xFF1234C8), // Loader_Fluids.java:278
		new AquaFluidSpec("potion.weakness", "Weakening Brew", 300, 1000, 1000, 0xFF48484A), // Loader_Fluids.java:279
		new AquaFluidSpec("potion.weakness.long", "Stretched Weakening Brew", 300, 1000, 1000, 0xFF48484A), // Loader_Fluids.java:280
		new AquaFluidSpec("potion.weakness.splash", "Splash Weakening Brew", 300, 1000, 1000, 0xFF48484A), // Loader_Fluids.java:281
		new AquaFluidSpec("potion.weakness.long.splash", "Stretched Splash Weakening Brew", 300, 1000, 1000, 0xFF48484A), // Loader_Fluids.java:282
		new AquaFluidSpec("potion.slowness", "Lame Brew", 300, 1000, 1000, 0xFF5A6A9A), // Loader_Fluids.java:283
		new AquaFluidSpec("potion.slowness.long", "Stretched Lame Brew", 300, 1000, 1000, 0xFF5A6A9A), // Loader_Fluids.java:284
		new AquaFluidSpec("potion.slowness.splash", "Splash Lame Brew", 300, 1000, 1000, 0xFF5A6A9A), // Loader_Fluids.java:285
		new AquaFluidSpec("potion.slowness.long.splash", "Stretched Splash Lame Brew", 300, 1000, 1000, 0xFF5A6A9A), // Loader_Fluids.java:286
		new AquaFluidSpec("potion.waterbreathing", "Fishy Brew", 300, 1000, 1000, 0xFF2A5AE8), // Loader_Fluids.java:287
		new AquaFluidSpec("potion.waterbreathing.long", "Stretched Fishy Brew", 300, 1000, 1000, 0xFF2A5AE8), // Loader_Fluids.java:288
		new AquaFluidSpec("potion.waterbreathing.splash", "Splash Fishy Brew", 300, 1000, 1000, 0xFF2A5AE8), // Loader_Fluids.java:289
		new AquaFluidSpec("potion.waterbreathing.long.splash", "Stretched Splash Fishy Brew", 300, 1000, 1000, 0xFF2A5AE8), // Loader_Fluids.java:290
		new AquaFluidSpec("potion.invisibility", "Invisible Brew", 300, 1000, 1000, 0xFFB8B8C8), // Loader_Fluids.java:291
		new AquaFluidSpec("potion.invisibility.long", "Stretched Invisible Brew", 300, 1000, 1000, 0xFFB8B8C8), // Loader_Fluids.java:292
		new AquaFluidSpec("potion.invisibility.splash", "Splash Invisible Brew", 300, 1000, 1000, 0xFFB8B8C8), // Loader_Fluids.java:293
		new AquaFluidSpec("potion.invisibility.long.splash", "Stretched Splash Invisible Brew", 300, 1000, 1000, 0xFFB8B8C8), // Loader_Fluids.java:294
		new AquaFluidSpec("potion.damage.lingering", "Lingering Harming Brew", 300, 1000, 1000, 0xFF8A2A2A), // Loader_Fluids.java:319
		new AquaFluidSpec("potion.damage.strong.lingering", "Strong Lingering Harming Brew", 300, 1000, 1000, 0xFF8A2A2A), // Loader_Fluids.java:320
		new AquaFluidSpec("potion.health.lingering", "Lingering Healing Brew", 300, 1000, 1000, 0xFFE85A7A), // Loader_Fluids.java:321
		new AquaFluidSpec("potion.health.strong.lingering", "Strong Lingering Healing Brew", 300, 1000, 1000, 0xFFE85A7A), // Loader_Fluids.java:322
		new AquaFluidSpec("potion.jump.lingering", "Lingering Jumpy Brew", 300, 1000, 1000, 0xFF9AE84A), // Loader_Fluids.java:323
		new AquaFluidSpec("potion.jump.strong.lingering", "Strong Lingering Jumpy Brew", 300, 1000, 1000, 0xFF9AE84A), // Loader_Fluids.java:324
		new AquaFluidSpec("potion.speed.lingering", "Lingering Swiftness Brew", 300, 1000, 1000, 0xFF7ACAE8), // Loader_Fluids.java:325
		new AquaFluidSpec("potion.speed.strong.lingering", "Strong Lingering Swiftness Brew", 300, 1000, 1000, 0xFF7ACAE8), // Loader_Fluids.java:326
		new AquaFluidSpec("potion.speed.long.lingering", "Stretched Lingering Swiftness Brew", 300, 1000, 1000, 0xFF7ACAE8), // Loader_Fluids.java:327
		new AquaFluidSpec("potion.strength.lingering", "Lingering Strength Brew", 300, 1000, 1000, 0xFFC83A3A), // Loader_Fluids.java:328
		new AquaFluidSpec("potion.strength.strong.lingering", "Strong Lingering Strength Brew", 300, 1000, 1000, 0xFFC83A3A), // Loader_Fluids.java:329
		new AquaFluidSpec("potion.strength.long.lingering", "Stretched Lingering Strength Brew", 300, 1000, 1000, 0xFFC83A3A), // Loader_Fluids.java:330
		new AquaFluidSpec("potion.regen.lingering", "Lingering Regenerating Brew", 300, 1000, 1000, 0xFFE86A9A), // Loader_Fluids.java:331
		new AquaFluidSpec("potion.regen.strong.lingering", "Strong Lingering Regenerating Brew", 300, 1000, 1000, 0xFFE86A9A), // Loader_Fluids.java:332
		new AquaFluidSpec("potion.regen.long.lingering", "Stretched Lingering Regenerating Brew", 300, 1000, 1000, 0xFFE86A9A), // Loader_Fluids.java:333
		new AquaFluidSpec("potion.poison.lingering", "Lingering Poisonous Brew", 300, 1000, 1000, 0xFF4A8A2A), // Loader_Fluids.java:334
		new AquaFluidSpec("potion.poison.strong.lingering", "Strong Lingering Poisonous Brew", 300, 1000, 1000, 0xFF4A8A2A), // Loader_Fluids.java:335
		new AquaFluidSpec("potion.poison.long.lingering", "Stretched Lingering Poisonous Brew", 300, 1000, 1000, 0xFF4A8A2A), // Loader_Fluids.java:336
		new AquaFluidSpec("potion.fireresistance.lingering", "Lingering Fire Resistant Brew", 375, 1000, 1000, 0xFFE8902A), // Loader_Fluids.java:337
		new AquaFluidSpec("potion.fireresistance.long.lingering", "Stretched Lingering Fire Resistant Brew", 375, 1000, 1000, 0xFFE8902A), // Loader_Fluids.java:338
		new AquaFluidSpec("potion.nightvision.lingering", "Lingering Night Vision Brew", 300, 1000, 1000, 0xFF1234C8), // Loader_Fluids.java:339
		new AquaFluidSpec("potion.nightvision.long.lingering", "Stretched Lingering Night Vision Brew", 300, 1000, 1000, 0xFF1234C8), // Loader_Fluids.java:340
		new AquaFluidSpec("potion.weakness.lingering", "Lingering Weakening Brew", 300, 1000, 1000, 0xFF48484A), // Loader_Fluids.java:341
		new AquaFluidSpec("potion.weakness.long.lingering", "Stretched Lingering Weakening Brew", 300, 1000, 1000, 0xFF48484A), // Loader_Fluids.java:342
		new AquaFluidSpec("potion.slowness.lingering", "Lingering Lame Brew", 300, 1000, 1000, 0xFF5A6A9A), // Loader_Fluids.java:343
		new AquaFluidSpec("potion.slowness.long.lingering", "Stretched Lingering Lame Brew", 300, 1000, 1000, 0xFF5A6A9A), // Loader_Fluids.java:344
		new AquaFluidSpec("potion.waterbreathing.lingering", "Lingering Fishy Brew", 300, 1000, 1000, 0xFF2A5AE8), // Loader_Fluids.java:345
		new AquaFluidSpec("potion.waterbreathing.long.lingering", "Stretched Lingering Fishy Brew", 300, 1000, 1000, 0xFF2A5AE8), // Loader_Fluids.java:346
		new AquaFluidSpec("potion.invisibility.lingering", "Lingering Invisible Brew", 300, 1000, 1000, 0xFFB8B8C8), // Loader_Fluids.java:347
		new AquaFluidSpec("potion.invisibility.long.lingering", "Stretched Lingering Invisible Brew", 300, 1000, 1000, 0xFFB8B8C8), // Loader_Fluids.java:348
		new AquaFluidSpec("riverwater", "River Water", 273, 1000, 1000, 0xFF4A7A3A), // Loader_Fluids.java:361
		new AquaFluidSpec("ic2distilledwater", "Distilled Water", 300, 1000, 1000, 0xFF6E6EFF), // Loader_Fluids.java:363
		new AquaFluidSpec("goldencarrotjuice", "Golden Carrot Juice", 300, 1000, 1000, 0xFFE8C83A), // Loader_Fluids.java:606
		new AquaFluidSpec("holywater", "Holy Water", 300, 1000, 1000, 0xFFE8E8FF), // Loader_Fluids.java:615
		new AquaFluidSpec("rottendrink", "Rotten Drink", 275, 1000, 1000, 0xFF5A4A2A), // Loader_Fluids.java:626
		new AquaFluidSpec("poison", "Poison", 300, 1000, 1000, 0xFF4A8A2A), // Loader_Fluids.java:629
		new AquaFluidSpec("chocolatemilk", "Chocolate Milk", 300, 1000, 1000, 0xFF6A3A1A), // Loader_Fluids.java:643
		new AquaFluidSpec("medicine.heal", "Medicine", 300, 1000, 1000, 0xFFE85A7A), // Loader_Fluids.java:649
		new AquaFluidSpec("medicine.laxative", "Laxative", 300, 1000, 1000, 0xFF8A6A2A) // Loader_Fluids.java:650
	);

	/** The food-b2 row for a gt6 id path, or null (the {@link #foodB1Spec} lookup shape, its own table). */
	public static AquaFluidSpec foodB2Spec(String aName) {
		for (AquaFluidSpec tSpec : FOOD_B2_SPECS) if (tSpec.name().equals(aName)) return tSpec;
		return null;
	}

	/** The registration helper for one food-b2 row (the {@link #foodB1Fluid} shape over the sixth table). */
	private static AquaFluid foodB2Fluid(String aName) {
		AquaFluidSpec tSpec = foodB2Spec(aName);
		if (tSpec == null) throw new IllegalArgumentException("no food-b2 fluid spec: " + aName);
		return registerFluidFamily(tSpec);
	}


	// ponytail: 103 one-line static registrations instead of a loop — the static-init
	// DeferredRegister accumulation IS the registration mechanism (the b1 precedent above).

	public static final AquaFluid POTION_TAINTED                         = foodB2Fluid("potion.tainted");
	public static final AquaFluid POTION_AWKWARD                         = foodB2Fluid("potion.awkward");
	public static final AquaFluid POTION_THICK                           = foodB2Fluid("potion.thick");
	public static final AquaFluid POTION_MUNDANE                         = foodB2Fluid("potion.mundane");
	public static final AquaFluid POTION_DAMAGE                          = foodB2Fluid("potion.damage");
	public static final AquaFluid POTION_DAMAGE_STRONG                   = foodB2Fluid("potion.damage.strong");
	public static final AquaFluid POTION_DAMAGE_SPLASH                   = foodB2Fluid("potion.damage.splash");
	public static final AquaFluid POTION_DAMAGE_STRONG_SPLASH            = foodB2Fluid("potion.damage.strong.splash");
	public static final AquaFluid POTION_HEALTH                          = foodB2Fluid("potion.health");
	public static final AquaFluid POTION_HEALTH_STRONG                   = foodB2Fluid("potion.health.strong");
	public static final AquaFluid POTION_HEALTH_SPLASH                   = foodB2Fluid("potion.health.splash");
	public static final AquaFluid POTION_HEALTH_STRONG_SPLASH            = foodB2Fluid("potion.health.strong.splash");
	public static final AquaFluid POTION_JUMP                            = foodB2Fluid("potion.jump");
	public static final AquaFluid POTION_JUMP_STRONG                     = foodB2Fluid("potion.jump.strong");
	public static final AquaFluid POTION_JUMP_SPLASH                     = foodB2Fluid("potion.jump.splash");
	public static final AquaFluid POTION_JUMP_STRONG_SPLASH              = foodB2Fluid("potion.jump.strong.splash");
	public static final AquaFluid POTION_SPEED                           = foodB2Fluid("potion.speed");
	public static final AquaFluid POTION_SPEED_STRONG                    = foodB2Fluid("potion.speed.strong");
	public static final AquaFluid POTION_SPEED_LONG                      = foodB2Fluid("potion.speed.long");
	public static final AquaFluid POTION_SPEED_SPLASH                    = foodB2Fluid("potion.speed.splash");
	public static final AquaFluid POTION_SPEED_STRONG_SPLASH             = foodB2Fluid("potion.speed.strong.splash");
	public static final AquaFluid POTION_SPEED_LONG_SPLASH               = foodB2Fluid("potion.speed.long.splash");
	public static final AquaFluid POTION_STRENGTH                        = foodB2Fluid("potion.strength");
	public static final AquaFluid POTION_STRENGTH_STRONG                 = foodB2Fluid("potion.strength.strong");
	public static final AquaFluid POTION_STRENGTH_LONG                   = foodB2Fluid("potion.strength.long");
	public static final AquaFluid POTION_STRENGTH_SPLASH                 = foodB2Fluid("potion.strength.splash");
	public static final AquaFluid POTION_STRENGTH_STRONG_SPLASH          = foodB2Fluid("potion.strength.strong.splash");
	public static final AquaFluid POTION_STRENGTH_LONG_SPLASH            = foodB2Fluid("potion.strength.long.splash");
	public static final AquaFluid POTION_REGEN                           = foodB2Fluid("potion.regen");
	public static final AquaFluid POTION_REGEN_STRONG                    = foodB2Fluid("potion.regen.strong");
	public static final AquaFluid POTION_REGEN_LONG                      = foodB2Fluid("potion.regen.long");
	public static final AquaFluid POTION_REGEN_SPLASH                    = foodB2Fluid("potion.regen.splash");
	public static final AquaFluid POTION_REGEN_STRONG_SPLASH             = foodB2Fluid("potion.regen.strong.splash");
	public static final AquaFluid POTION_REGEN_LONG_SPLASH               = foodB2Fluid("potion.regen.long.splash");
	public static final AquaFluid POTION_POISON                          = foodB2Fluid("potion.poison");
	public static final AquaFluid POTION_POISON_STRONG                   = foodB2Fluid("potion.poison.strong");
	public static final AquaFluid POTION_POISON_LONG                     = foodB2Fluid("potion.poison.long");
	public static final AquaFluid POTION_POISON_SPLASH                   = foodB2Fluid("potion.poison.splash");
	public static final AquaFluid POTION_POISON_STRONG_SPLASH            = foodB2Fluid("potion.poison.strong.splash");
	public static final AquaFluid POTION_POISON_LONG_SPLASH              = foodB2Fluid("potion.poison.long.splash");
	public static final AquaFluid POTION_FIRERESISTANCE                  = foodB2Fluid("potion.fireresistance");
	public static final AquaFluid POTION_FIRERESISTANCE_LONG             = foodB2Fluid("potion.fireresistance.long");
	public static final AquaFluid POTION_FIRERESISTANCE_SPLASH           = foodB2Fluid("potion.fireresistance.splash");
	public static final AquaFluid POTION_FIRERESISTANCE_LONG_SPLASH      = foodB2Fluid("potion.fireresistance.long.splash");
	public static final AquaFluid POTION_NIGHTVISION                     = foodB2Fluid("potion.nightvision");
	public static final AquaFluid POTION_NIGHTVISION_LONG                = foodB2Fluid("potion.nightvision.long");
	public static final AquaFluid POTION_NIGHTVISION_SPLASH              = foodB2Fluid("potion.nightvision.splash");
	public static final AquaFluid POTION_NIGHTVISION_LONG_SPLASH         = foodB2Fluid("potion.nightvision.long.splash");
	public static final AquaFluid POTION_WEAKNESS                        = foodB2Fluid("potion.weakness");
	public static final AquaFluid POTION_WEAKNESS_LONG                   = foodB2Fluid("potion.weakness.long");
	public static final AquaFluid POTION_WEAKNESS_SPLASH                 = foodB2Fluid("potion.weakness.splash");
	public static final AquaFluid POTION_WEAKNESS_LONG_SPLASH            = foodB2Fluid("potion.weakness.long.splash");
	public static final AquaFluid POTION_SLOWNESS                        = foodB2Fluid("potion.slowness");
	public static final AquaFluid POTION_SLOWNESS_LONG                   = foodB2Fluid("potion.slowness.long");
	public static final AquaFluid POTION_SLOWNESS_SPLASH                 = foodB2Fluid("potion.slowness.splash");
	public static final AquaFluid POTION_SLOWNESS_LONG_SPLASH            = foodB2Fluid("potion.slowness.long.splash");
	public static final AquaFluid POTION_WATERBREATHING                  = foodB2Fluid("potion.waterbreathing");
	public static final AquaFluid POTION_WATERBREATHING_LONG             = foodB2Fluid("potion.waterbreathing.long");
	public static final AquaFluid POTION_WATERBREATHING_SPLASH           = foodB2Fluid("potion.waterbreathing.splash");
	public static final AquaFluid POTION_WATERBREATHING_LONG_SPLASH      = foodB2Fluid("potion.waterbreathing.long.splash");
	public static final AquaFluid POTION_INVISIBILITY                    = foodB2Fluid("potion.invisibility");
	public static final AquaFluid POTION_INVISIBILITY_LONG               = foodB2Fluid("potion.invisibility.long");
	public static final AquaFluid POTION_INVISIBILITY_SPLASH             = foodB2Fluid("potion.invisibility.splash");
	public static final AquaFluid POTION_INVISIBILITY_LONG_SPLASH        = foodB2Fluid("potion.invisibility.long.splash");
	public static final AquaFluid POTION_DAMAGE_LINGERING                = foodB2Fluid("potion.damage.lingering");
	public static final AquaFluid POTION_DAMAGE_STRONG_LINGERING         = foodB2Fluid("potion.damage.strong.lingering");
	public static final AquaFluid POTION_HEALTH_LINGERING                = foodB2Fluid("potion.health.lingering");
	public static final AquaFluid POTION_HEALTH_STRONG_LINGERING         = foodB2Fluid("potion.health.strong.lingering");
	public static final AquaFluid POTION_JUMP_LINGERING                  = foodB2Fluid("potion.jump.lingering");
	public static final AquaFluid POTION_JUMP_STRONG_LINGERING           = foodB2Fluid("potion.jump.strong.lingering");
	public static final AquaFluid POTION_SPEED_LINGERING                 = foodB2Fluid("potion.speed.lingering");
	public static final AquaFluid POTION_SPEED_STRONG_LINGERING          = foodB2Fluid("potion.speed.strong.lingering");
	public static final AquaFluid POTION_SPEED_LONG_LINGERING            = foodB2Fluid("potion.speed.long.lingering");
	public static final AquaFluid POTION_STRENGTH_LINGERING              = foodB2Fluid("potion.strength.lingering");
	public static final AquaFluid POTION_STRENGTH_STRONG_LINGERING       = foodB2Fluid("potion.strength.strong.lingering");
	public static final AquaFluid POTION_STRENGTH_LONG_LINGERING         = foodB2Fluid("potion.strength.long.lingering");
	public static final AquaFluid POTION_REGEN_LINGERING                 = foodB2Fluid("potion.regen.lingering");
	public static final AquaFluid POTION_REGEN_STRONG_LINGERING          = foodB2Fluid("potion.regen.strong.lingering");
	public static final AquaFluid POTION_REGEN_LONG_LINGERING            = foodB2Fluid("potion.regen.long.lingering");
	public static final AquaFluid POTION_POISON_LINGERING                = foodB2Fluid("potion.poison.lingering");
	public static final AquaFluid POTION_POISON_STRONG_LINGERING         = foodB2Fluid("potion.poison.strong.lingering");
	public static final AquaFluid POTION_POISON_LONG_LINGERING           = foodB2Fluid("potion.poison.long.lingering");
	public static final AquaFluid POTION_FIRERESISTANCE_LINGERING        = foodB2Fluid("potion.fireresistance.lingering");
	public static final AquaFluid POTION_FIRERESISTANCE_LONG_LINGERING   = foodB2Fluid("potion.fireresistance.long.lingering");
	public static final AquaFluid POTION_NIGHTVISION_LINGERING           = foodB2Fluid("potion.nightvision.lingering");
	public static final AquaFluid POTION_NIGHTVISION_LONG_LINGERING      = foodB2Fluid("potion.nightvision.long.lingering");
	public static final AquaFluid POTION_WEAKNESS_LINGERING              = foodB2Fluid("potion.weakness.lingering");
	public static final AquaFluid POTION_WEAKNESS_LONG_LINGERING         = foodB2Fluid("potion.weakness.long.lingering");
	public static final AquaFluid POTION_SLOWNESS_LINGERING              = foodB2Fluid("potion.slowness.lingering");
	public static final AquaFluid POTION_SLOWNESS_LONG_LINGERING         = foodB2Fluid("potion.slowness.long.lingering");
	public static final AquaFluid POTION_WATERBREATHING_LINGERING        = foodB2Fluid("potion.waterbreathing.lingering");
	public static final AquaFluid POTION_WATERBREATHING_LONG_LINGERING   = foodB2Fluid("potion.waterbreathing.long.lingering");
	public static final AquaFluid POTION_INVISIBILITY_LINGERING          = foodB2Fluid("potion.invisibility.lingering");
	public static final AquaFluid POTION_INVISIBILITY_LONG_LINGERING     = foodB2Fluid("potion.invisibility.long.lingering");
	public static final AquaFluid RIVERWATER                             = foodB2Fluid("riverwater");
	public static final AquaFluid IC2DISTILLEDWATER                      = foodB2Fluid("ic2distilledwater");
	public static final AquaFluid GOLDENCARROTJUICE                      = foodB2Fluid("goldencarrotjuice");
	public static final AquaFluid HOLYWATER                              = foodB2Fluid("holywater");
	public static final AquaFluid ROTTENDRINK                            = foodB2Fluid("rottendrink");
	public static final AquaFluid POISON                                 = foodB2Fluid("poison");
	public static final AquaFluid CHOCOLATEMILK                          = foodB2Fluid("chocolatemilk");
	public static final AquaFluid MEDICINE_HEAL                          = foodB2Fluid("medicine.heal");
	public static final AquaFluid MEDICINE_LAXATIVE                      = foodB2Fluid("medicine.laxative");


	/**
	 * The food-fluid tail (task p33-food-tail): the SEVENTH AquaFluidSpec table — the
	 * card-block-outside drink fluids the b2 report left in the tail pool (the DrinkStat
	 * rows were unreachable without a registered carrier): the four ENCHANTED_EFFECT
	 * golden-apple brews (Loader_Fluids.java:607-610, the goldencarrotjuice :606 family —
	 * every row carries the upstream {@code .setLuminosity(15)}; the AquaFluidSpec record
	 * has no luminosity column, so the light value rides the GTDrinks.DrinkStat per the
	 * :606 precedent) and the six coffee-family drinks (:637-642, the THERMOS-flagged
	 * runs — the flag has no modern carrier face, the FoodStatDrink extended channels are
	 * the GTDrinks surface). Every value is the upstream FL.create carrier verbatim: all
	 * ten rows ride the 300 K literal (:607-610/:637-642). The tints are PORT-OWNED
	 * DECLARED VALUES (the b2 potion-block precedent: one shared texture upstream, the
	 * port differentiates per family — coffee browns / golden golds over the vanilla-water
	 * layers). FLUID-ONLY, NO drink behaviour on this table — same shape as b2, the
	 * DrinkStat seam is GTDrinks keyed by the SAME gt6 registry paths.
	 */
	public static final List<AquaFluidSpec> FOOD_TAIL_SPECS = List.of(
		new AquaFluidSpec("potion.goldenapplejuice" , "Golden Apple Juice"               , 300, 1000, 1000, 0xFFFFE06A), // Loader_Fluids.java:607 — the .setLuminosity(15) family, light on the DrinkStat row
		new AquaFluidSpec("potion.goldencider"      , "Golden Cider"                     , 300, 1000, 1000, 0xFFF0C840), // Loader_Fluids.java:608
		new AquaFluidSpec("potion.idunsapplejuice"  , "Idun's Apple Juice"               , 300, 1000, 1000, 0xFFFFF0B0), // Loader_Fluids.java:609
		new AquaFluidSpec("potion.notchesbrew"      , "Notches Brew"                     , 300, 1000, 1000, 0xFFD8A838), // Loader_Fluids.java:610
		new AquaFluidSpec("potion.darkcoffee"       , "Dark Coffee"                      , 300, 1000, 1000, 0xFF2A1A0A), // Loader_Fluids.java:637
		new AquaFluidSpec("potion.darkcafeaulait"   , "Dark Cafe au lait"                , 300, 1000, 1000, 0xFF3A2A1A), // Loader_Fluids.java:638
		new AquaFluidSpec("potion.coffee"           , "Coffee"                           , 300, 1000, 1000, 0xFF4A3A20), // Loader_Fluids.java:639
		new AquaFluidSpec("potion.cafeaulait"       , "Cafe au lait"                     , 300, 1000, 1000, 0xFF8A6A4A), // Loader_Fluids.java:640
		new AquaFluidSpec("potion.laitaucafe"       , "Lait au cafe"                     , 300, 1000, 1000, 0xFFA8896A), // Loader_Fluids.java:641
		new AquaFluidSpec("potion.darkchocolatemilk", "Bitter Chocolate Milk"            , 300, 1000, 1000, 0xFF3A2414)  // Loader_Fluids.java:642
	);

	/** The food-tail row for a gt6 id path, or null (the {@link #foodB2Spec} lookup shape, its own table). */
	public static AquaFluidSpec foodTailSpec(String aName) {
		for (AquaFluidSpec tSpec : FOOD_TAIL_SPECS) if (tSpec.name().equals(aName)) return tSpec;
		return null;
	}

	/** The registration helper for one food-tail row (the {@link #foodB2Fluid} shape over the seventh table). */
	private static AquaFluid foodTailFluid(String aName) {
		AquaFluidSpec tSpec = foodTailSpec(aName);
		if (tSpec == null) throw new IllegalArgumentException("no food-tail fluid spec: " + aName);
		return registerFluidFamily(tSpec);
	}

	// ponytail: 10 one-line static registrations instead of a loop — the static-init
	// DeferredRegister accumulation IS the registration mechanism (the b1/b2 precedent).

	public static final AquaFluid POTION_GOLDENAPPLEJUICE                = foodTailFluid("potion.goldenapplejuice");
	public static final AquaFluid POTION_GOLDENCIDER                     = foodTailFluid("potion.goldencider");
	public static final AquaFluid POTION_IDUNSAPPLEJUICE                 = foodTailFluid("potion.idunsapplejuice");
	public static final AquaFluid POTION_NOTCHESBREW                     = foodTailFluid("potion.notchesbrew");
	public static final AquaFluid POTION_DARKCOFFEE                      = foodTailFluid("potion.darkcoffee");
	public static final AquaFluid POTION_DARKCAFEAULAIT                  = foodTailFluid("potion.darkcafeaulait");
	public static final AquaFluid POTION_COFFEE                          = foodTailFluid("potion.coffee");
	public static final AquaFluid POTION_CAFEAULAIT                      = foodTailFluid("potion.cafeaulait");
	public static final AquaFluid POTION_LAITAUCAFE                      = foodTailFluid("potion.laitaucafe");
	public static final AquaFluid POTION_DARKCHOCOLATEMILK               = foodTailFluid("potion.darkchocolatemilk");

	/** The 10 registered food-tail families in {@link #FOOD_TAIL_SPECS} declaration order (the lang/table walkers). */
	public static List<AquaFluid> foodTailFluids() {
		return List.of(POTION_GOLDENAPPLEJUICE, POTION_GOLDENCIDER, POTION_IDUNSAPPLEJUICE, POTION_NOTCHESBREW, POTION_DARKCOFFEE, POTION_DARKCAFEAULAIT, POTION_COFFEE, POTION_CAFEAULAIT, POTION_LAITAUCAFE, POTION_DARKCHOCOLATEMILK);
	}

	/** The 103 registered food-b2 families in {@link #FOOD_B2_SPECS} declaration order (the lang/table walkers). */
	public static List<AquaFluid> foodB2Fluids() {
		return List.of(POTION_TAINTED, POTION_AWKWARD, POTION_THICK, POTION_MUNDANE, POTION_DAMAGE, POTION_DAMAGE_STRONG, POTION_DAMAGE_SPLASH, POTION_DAMAGE_STRONG_SPLASH, POTION_HEALTH, POTION_HEALTH_STRONG, POTION_HEALTH_SPLASH, POTION_HEALTH_STRONG_SPLASH, POTION_JUMP, POTION_JUMP_STRONG, POTION_JUMP_SPLASH, POTION_JUMP_STRONG_SPLASH, POTION_SPEED, POTION_SPEED_STRONG, POTION_SPEED_LONG, POTION_SPEED_SPLASH, POTION_SPEED_STRONG_SPLASH, POTION_SPEED_LONG_SPLASH, POTION_STRENGTH, POTION_STRENGTH_STRONG, POTION_STRENGTH_LONG, POTION_STRENGTH_SPLASH, POTION_STRENGTH_STRONG_SPLASH, POTION_STRENGTH_LONG_SPLASH, POTION_REGEN, POTION_REGEN_STRONG, POTION_REGEN_LONG, POTION_REGEN_SPLASH, POTION_REGEN_STRONG_SPLASH, POTION_REGEN_LONG_SPLASH, POTION_POISON, POTION_POISON_STRONG, POTION_POISON_LONG, POTION_POISON_SPLASH, POTION_POISON_STRONG_SPLASH, POTION_POISON_LONG_SPLASH, POTION_FIRERESISTANCE, POTION_FIRERESISTANCE_LONG, POTION_FIRERESISTANCE_SPLASH, POTION_FIRERESISTANCE_LONG_SPLASH, POTION_NIGHTVISION, POTION_NIGHTVISION_LONG, POTION_NIGHTVISION_SPLASH, POTION_NIGHTVISION_LONG_SPLASH, POTION_WEAKNESS, POTION_WEAKNESS_LONG, POTION_WEAKNESS_SPLASH, POTION_WEAKNESS_LONG_SPLASH, POTION_SLOWNESS, POTION_SLOWNESS_LONG, POTION_SLOWNESS_SPLASH, POTION_SLOWNESS_LONG_SPLASH, POTION_WATERBREATHING, POTION_WATERBREATHING_LONG, POTION_WATERBREATHING_SPLASH, POTION_WATERBREATHING_LONG_SPLASH, POTION_INVISIBILITY, POTION_INVISIBILITY_LONG, POTION_INVISIBILITY_SPLASH, POTION_INVISIBILITY_LONG_SPLASH, POTION_DAMAGE_LINGERING, POTION_DAMAGE_STRONG_LINGERING, POTION_HEALTH_LINGERING, POTION_HEALTH_STRONG_LINGERING, POTION_JUMP_LINGERING, POTION_JUMP_STRONG_LINGERING, POTION_SPEED_LINGERING, POTION_SPEED_STRONG_LINGERING, POTION_SPEED_LONG_LINGERING, POTION_STRENGTH_LINGERING, POTION_STRENGTH_STRONG_LINGERING, POTION_STRENGTH_LONG_LINGERING, POTION_REGEN_LINGERING, POTION_REGEN_STRONG_LINGERING, POTION_REGEN_LONG_LINGERING, POTION_POISON_LINGERING, POTION_POISON_STRONG_LINGERING, POTION_POISON_LONG_LINGERING, POTION_FIRERESISTANCE_LINGERING, POTION_FIRERESISTANCE_LONG_LINGERING, POTION_NIGHTVISION_LINGERING, POTION_NIGHTVISION_LONG_LINGERING, POTION_WEAKNESS_LINGERING, POTION_WEAKNESS_LONG_LINGERING, POTION_SLOWNESS_LINGERING, POTION_SLOWNESS_LONG_LINGERING, POTION_WATERBREATHING_LINGERING, POTION_WATERBREATHING_LONG_LINGERING, POTION_INVISIBILITY_LINGERING, POTION_INVISIBILITY_LONG_LINGERING, RIVERWATER, IC2DISTILLEDWATER, GOLDENCARROTJUICE, HOLYWATER, ROTTENDRINK, POISON, CHOCOLATEMILK, MEDICINE_HEAL, MEDICINE_LAXATIVE);
	}

	/** The 216 registered food-b1 families in {@link #FOOD_B1_SPECS} declaration order (the lang/table walkers). */
		public static List<AquaFluid> foodB1Fluids() {
		return List.of(MINERALSODA, SODA, MILK, SOYMILK, GRCMILK_MILK, SPOILEDMILK, FOR_HONEY, GRC_HONEY, FRUITSMOOTHIE, MELONSMOOTHIE, KIWISMOOTHIE, CURRANTSMOOTHIE, RASPBERRYSMOOTHIE, BLACKBERRYSMOOTHIE, BLUEBERRYSMOOTHIE, GOOSEBERRYSMOOTHIE, STRAWBERRYSMOOTHIE, PLUMSMOOTHIE, PEACHSMOOTHIE, ELDERBERRYSMOOTHIE, GRAPEFRUITSMOOTHIE, LIMESMOOTHIE, ORANGESMOOTHIE, PERSIMMONSMOOTHIE, APRICOTSMOOTHIE, PEARSMOOTHIE, REDGRAPESMOOTHIE, WHITEGRAPESMOOTHIE, GRAPESMOOTHIE, PURPLEGRAPESMOOTHIE, APPLESMOOTHIE, PINEAPPLESMOOTHIE, BANANASMOOTHIE, CHERRYSMOOTHIE, CRANBERRYSMOOTHIE, LEMONSMOOTHIE, MANGOSMOOTHIE, POMEGRANATESMOOTHIE, STARFRUITSMOOTHIE, PAPAYASMOOTHIE, FIGSMOOTHIE, COCONUTSMOOTHIE, JUICE_JUICE, KIWIJUICE, JUICELIME, JUICELEMON, JUICEORANGE, PERSIMMONJUICE, MELONJUICE, CURRANTJUICE, RASPBERRYJUICE, BLACKBERRYJUICE, BLUEBERRYJUICE, GOOSEBERRYJUICE, STRAWBERRYJUICE, JUICEPLUM, JUICEPEACH, JUICEELDERBERRY, HELLDERBERRYJUICE, JUICEGRAPEFRUIT, JUICEAPRICOT, JUICEPEAR, GRAPEJUICE, GRC_GRAPEWINE0, JUICEREDGRAPE, JUICEWHITEGRAPE, JUICEAPPLE, GRC_APPLECIDER0, JUICEPINEAPPLE, JUICEBANANA, JUICECHERRY, JUICECRANBERRY, CACTUSFRUITJUICE, MANGOJUICE, POMEGRANATEJUICE, STARFRUITJUICE, PAPAYAJUICE, FIGJUICE, COCONUTMILK, DATEJUICE, JUICECARROT, JUICETOMATO, BEETJUICE, PUMPKINJUICE, CUCUMBERJUICE, ONIONJUICE, POTATOJUICE, RICEWATER, HOPSMASH, WHEATHOPSMASH, MASHWHEAT, MASHCORN, MASHRYE, MASHGRAIN, MAPLESYRUP, PEANUTBUTTER, GRCMILK_CREAM, CHOCOLATECREAM, COCONUTCREAM, NUTELLA, KETCHUP, MAYO, DRESSING, MUSHROOMSOUP, BLOOD, CHILLYSAUCE, HOTSAUCE, DIABOLOSAUCE, DIABLOSAUCE, DIABLOSAUCE_STRONG, BBQSAUCE, SLIME_BLUE, PINKSLIME, SLIME, BAWLS, TEA, SWEETTEA, ICETEA, PURPLEDRINK, LEMONADE, CAVEJOHNSONSGRENADEJUICE, VINEGAR, APPLEVINEGAR, CANEVINEGAR, RICEVINEGAR, JUICE_WINE_FRUIT, LIMONCELLO, WINEAGAVE, WINEAPRICOT, WINEBANANA, WINECARROT, WINECHERRY, WINECITRUS, WINECRANBERRY, WINEELDERBERRY, WINEPLUM, WINESPARKLING, WINETOMATO, WINE, RICARDOSANCHEZ, WINERED, WINEWHITE, WINEFORTIFIED, WHISKEY, WHISKEYRYE, WHISKEYCORN, WHISKEYWHEAT, GLENMCKENNER, LIQUEURCHOCOLATE, LIQUEURALMOND, LIQUEURANISE, LIQUEURBANANA, LIQUEURBLACKBERRY, LIQUEURBLACKCURRANT, LIQUEURCHERRY, LIQUEURCINNAMON, LIQUEURCOFFEE, LIQUEURHAZELNUT, LIQUEURHERBAL, LIQUEURLEMON, LIQUEURMELON, LIQUEURMINT, LIQUEURORANGE, LIQUEURPEACH, LIQUEURRASPBERRY, LIQUORFRUIT, LIQUORAPPLE, LIQUORAPRICOT, LIQUORCHERRY, LIQUORELDERBERRY, LIQUORPEAR, SPIRITGIN, SPIRITNEUTRAL, SPIRITSUGARCANE, BRANDYFRUIT, BRANDYAPPLE, BRANDYAPRICOT, BRANDYCHERRY, BRANDYCITRUS, BRANDYELDERBERRY, BRANDYGRAPE, BRANDYPEAR, BRANDYPLUM, CIDERAPPLE, CIDERPEAR, CIDERPEACH, WINEPINEAPPLE, BEER, DARKBEER, DRAGONBLOOD, BEERALE, BEERCORN, BEERLAGER, BEERRYE, BEERSTOUT, BEERWHEAT, RUMWHITE, RUMDARK, PINA_COLADA, VODKA, LENINADE, MEAD, SHORT_MEAD, SAKE, TEQUILA, ALCOPOPS, HOTFRYINGOIL, SEEDOIL, PLANTOIL, SUNFLOWEROIL, JUICEOLIVE, NUTOIL, LINOIL, HEMPOIL, FISHOIL, WHALEOIL);
	}
	/**
	 * {@code gt6:dye_chemical_<colour>} + {@code gt6:chlorine} — the Canner refill input
	 * domain (task p24-dye-chemical-fluids, the ruling R3/R4/R6 of
	 * decisions.p24-canner-dyes-rulings). Upstream the 16 chemical dyes ride the
	 * {@code FL.create("dye.chemical." + colour, tDyeChemical, "Chemical " + DYE_NAMES[i]
	 * + " Dye", null, DYES[i], 1, L, 300, ...)} loop (Loader_Fluids.java:120-126, the
	 * DYE_FLUIDS_CHEMICAL[i] canonical = 144 mB per CS.java:129 L — R4: 1.7.10
	 * FluidStack.amount IS mB, the port translates 1:1), and chlorine rides the material
	 * gas loop (MT.Cl, MT.java:405 CONTAINERS_FLUID, boiling point 239 K) — a material
	 * bridge the port's logic-only material system has no fluid side for, so chlorine is
	 * registered here as the creosote-style standalone row (ruling R3).
	 *
	 * <p>Shape (ruling R6): the four-DR template WITH a LiquidBlock per family (the
	 * iron_molten/natural_gas hand shape, table-driven over the shared seam maps) — 16
	 * dye families + chlorine, no bucket item anywhere (the :161-163/:212-213 "No bucket
	 * item" precedent, GTFluids registers no items at all — R6 census: zero bucket, so
	 * zero item-tag face; the vanilla/forge fluid-tag surface has no dye/chlorine opt-in
	 * either, and the repo carries no FluidTagsProvider — the tag ruling's "无则声明无"
	 * arm, census 2026-09-07).
	 *
	 * <p>Colour source: the tint is EXACTLY {@link GTSprayCanItem#DYES_INT}[i] (CS.java:470
	 * DYES_INT, the upstream FL.create aRGBa of the same loop) — zero new colour data, the
	 * fluid index i is the same GT6 dye index the spray-can items are built from (ruling
	 * R5: the three-way i ↔ DYES_INT[i] ↔ spray_paint_&lt;DYE_IDS[i]&gt; is the Canner
	 * refill's correctness root, pinned by GTFluidsDyeChemicalFamilyTest). The still/flow
	 * layers borrow the upstream grayscale carrier
	 * {@code gt6:textures/block/fluids/dyes_chemical.png} (assets/gregtech/textures/blocks/
	 * fluids/dyes.chemical.png byte-identical, the :115-117 single-texture shared still=flow
	 * form, sha256 in assets/README.md) tinted per family; chlorine reuses the vanilla water
	 * textures over the 0xF0FFFF tint (the RGB(0,240,255) upstream material colour, the
	 * natural_gas :171-186 initializeClient shape).
	 *
	 * <p>Carrier values: the dyes ride 300 K (the FL.create temperature literal) with the
	 * honest FluidType defaults density 1000 / viscosity 1000 (the water_boiling/food
	 * precedent); chlorine carries temperature 239 K (MT.java:405 boiling point) and
	 * density −100 (the natural_gas lightweight carrier, NOT an upstream measurement — the
	 * creosote :206 port-owned carrier discipline). Both sit far under the wood-barrel
	 * 340 K ceiling, so the RCON tank chain carries them.
	 */
	public static final class DyeChemicalFluid {
		/** The GT6 dye index this family is ({@code 0..15}, the {@link GTSprayCanItem#DYE_IDS} row). */
		public final int dyeIndex;
		public final RegistryObject<FluidType> type;
		public final RegistryObject<FlowingFluid> source;
		public final RegistryObject<Fluid> flowing;
		public final RegistryObject<LiquidBlock> block;

		DyeChemicalFluid(int aDyeIndex, RegistryObject<FluidType> aType, RegistryObject<FlowingFluid> aSource,
				RegistryObject<Fluid> aFlowing, RegistryObject<LiquidBlock> aBlock) {
			dyeIndex = aDyeIndex; type = aType; source = aSource; flowing = aFlowing; block = aBlock;
		}

		/** The gt6 registry path — {@code dye_chemical_} + the {@link GTSprayCanItem#DYE_IDS}[i] snake id. */
		public String name() {return dyeChemicalName(dyeIndex);}

		/** The tint over the grayscale carrier — {@link GTSprayCanItem#DYES_INT}[i], the single colour source. */
		public int tint() {return GTSprayCanItem.DYES_INT[dyeIndex];}

		/** The en_us display — the upstream :123 compose verbatim, {@code "Chemical " + DYE_NAMES[i] + " Dye"}. */
		public String displayName() {return "Chemical " + GTSprayCanItem.DYE_NAMES[dyeIndex] + " Dye";}

		/** The lang/description key, the descriptionId the FluidType is registered with. */
		public String descriptionId() {return "fluid.gt6." + name();}
	}

	/** The gt6 registry path of the dye index — {@code dye_chemical_} + the {@link GTSprayCanItem#DYE_IDS}[i] snake id (the port snake convention; the upstream 1.7.10 id folds "Light Gray" to "lightgray", the port ids stay the spray-can snake, the declared deviation). */
	public static String dyeChemicalName(int aIndex) {
		return "dye_chemical_" + GTSprayCanItem.DYE_IDS[aIndex];
	}

	/** The dye index of a gt6 registry path, or −1 (the inverse of {@link #dyeChemicalName}, the Canner seam). */
	public static int dyeIndexOf(String aName) {
		for (int i = 0; i < 16; i++) if (dyeChemicalName(i).equals(aName)) return i;
		return -1;
	}

	/** The Loader_Fluids.java:123 FL.create temperature literal (300 K) — the declared carrier the FluidType registers with (the offline-readable test face). */
	public static final int DYE_CHEMICAL_TEMPERATURE = 300;

	/** The honest FluidType default density, declared explicitly (the water_boiling/food precedent). */
	public static final int DYE_CHEMICAL_DENSITY = 1000;

	/**
	 * The shared per-family Properties for the dye-chemical rows — called at registry-event
	 * time only (the engineFluid seam-map shape; the block handle rides {@link #BLOCK_SEAM},
	 * populated before the registry event fires).
	 */
	private static ForgeFlowingFluid.Properties dyeChemicalProperties(RegistryObject<FluidType> aType, String aName) {
		return new ForgeFlowingFluid.Properties(aType, SOURCE_SEAM.get(aName), FLOWING_SEAM.get(aName))
				.block(BLOCK_SEAM.get(aName));
	}

	/** The table-driven registration helper for one dye row: FluidType + Source/Flowing + LiquidBlock (the four-DR-with-block template). */
	private static DyeChemicalFluid dyeChemicalFluid(int aIndex) {
		String tName = dyeChemicalName(aIndex);
		int tTint = GTSprayCanItem.DYES_INT[aIndex]; // the single colour source, captured once
		RegistryObject<FluidType> tType = FLUID_TYPES.register(tName, () -> new FluidType(FluidType.Properties.create()
				.descriptionId("fluid.gt6." + tName)
				.temperature(DYE_CHEMICAL_TEMPERATURE) // the Loader_Fluids.java:123 FL.create literal
				.density(DYE_CHEMICAL_DENSITY)         // the honest FluidType default, declared explicitly
				.viscosity(1000)) {
			@Override
			public void initializeClient(Consumer<IClientFluidTypeExtensions> aConsumer) {
				aConsumer.accept(new IClientFluidTypeExtensions() {
					// the upstream single grayscale carrier, still = flow (Loader_Fluids.java:115-117
					// tDyeChemical shared by both layers; the PNG is the byte-identical borrow)
					private static final ResourceLocation STILL =
							ResourceLocation.fromNamespaceAndPath("gt6", "block/fluids/dyes_chemical");
					private static final ResourceLocation FLOW = STILL;

					@Override
					public ResourceLocation getStillTexture() {return STILL;}

					@Override
					public ResourceLocation getFlowingTexture() {return FLOW;}

					@Override
					public int getTintColor() {return tTint;} // GTSprayCanItem.DYES_INT[aIndex] — zero new colour data
				});
			}
		});
		RegistryObject<FlowingFluid> tSource = FLUIDS.register(tName,
				() -> new ForgeFlowingFluid.Source(dyeChemicalProperties(tType, tName)));
		RegistryObject<Fluid> tFlowing = FLUIDS.register(tName + "_flowing",
				() -> new ForgeFlowingFluid.Flowing(dyeChemicalProperties(tType, tName)));
		// the block leg: 1.20.1 Forge takes the supplier handle; 21.1 vanilla takes the
		// resolved fluid (the IRON_MOLTEN_BLOCK .get() shape — the stonecutter LiquidBlock
		// rewrite only matches UPPER_CASE fields, a camelCase local must fork explicitly)
		//? if forge {
		RegistryObject<LiquidBlock> tBlock = BLOCKS.register(tName + "_block",
				() -> new LiquidBlock(tSource, BlockBehaviour.Properties.of()
						.noCollission().strength(100.0F).noLootTable())); // a liquid: the iron_molten block ramp
		//?} else {
		/*RegistryObject<LiquidBlock> tBlock = BLOCKS.register(tName + "_block",
				() -> new LiquidBlock(tSource.get(), BlockBehaviour.Properties.of()
						.noCollission().strength(100.0F).noLootTable())); // a liquid: the iron_molten block ramp (FLUID before BLOCK, the resolved .get() is live)
		*///?}
		SOURCE_SEAM.put(tName, tSource);
		FLOWING_SEAM.put(tName, tFlowing);
		BLOCK_SEAM.put(tName, tBlock);
		return new DyeChemicalFluid(aIndex, tType, tSource, tFlowing, tBlock);
	}

	/**
	 * MT.java:405 — chlorine's boiling point, the FluidType temperature carrier (the one
	 * upstream-anchored chlorine value: {@code Cl 沸点 239K RGB(0,240,255)}).
	 */
	public static final int CHLORINE_TEMPERATURE = 239;

	/** The lightweight-gas carrier (the natural_gas :169 precedent, port-owned — NOT an upstream measurement; sign-only consumers put it strictly above air). */
	public static final int CHLORINE_DENSITY = -100;

	/** The MT.java:405 material colour RGB(0,240,255) over the vanilla water textures (ruling R3). */
	public static final int CHLORINE_TINT = 0xF0FFFF;

	private static ForgeFlowingFluid.Properties chlorineProperties() {
		// the four-DR template again (natural_gas :152-156 shape)
		return new ForgeFlowingFluid.Properties(CHLORINE_TYPE, CHLORINE, CHLORINE_FLOWING)
				.block(CHLORINE_BLOCK);
	}

	/**
	 * {@code gt6:chlorine} — the standalone carrier row of ruling R3: upstream it is born
	 * inside the material gas loop (Loader_Fluids.java:657-663 createGas over MT.Cl,
	 * MT.java:405 CONTAINERS_FLUID) which the port's logic-only material system has no
	 * fluid bridge for, so it rides the creosote independent-registration precedent. The
	 * near-consumer (the remover refill, MultiItemRandomTools.java:272
	 * {@code MT.Cl.fluid(16*U)} = 2304 mB) stays on the Canner machine card; until that
	 * lands chlorine is a dead end — zero recipe rows consume it (the family test's
	 * GT6RecipeMaps walk). Vanilla water textures + the 0xF0FFFF tint (the natural_gas
	 * initializeClient shape); no bucket item.
	 */
	public static final RegistryObject<FluidType> CHLORINE_TYPE = FLUID_TYPES.register("chlorine",
			() -> new FluidType(FluidType.Properties.create()
					.descriptionId("fluid.gt6.chlorine")
					.temperature(CHLORINE_TEMPERATURE)
					.density(CHLORINE_DENSITY)) {
				@Override
				public void initializeClient(Consumer<IClientFluidTypeExtensions> aConsumer) {
					aConsumer.accept(new IClientFluidTypeExtensions() {
						private static final ResourceLocation STILL = ResourceLocation.withDefaultNamespace("block/water_still");
						private static final ResourceLocation FLOW = ResourceLocation.withDefaultNamespace("block/water_flow");

						@Override
						public ResourceLocation getStillTexture() {return STILL;}

						@Override
						public ResourceLocation getFlowingTexture() {return FLOW;}

						@Override
						public int getTintColor() {return CHLORINE_TINT;} // the MT.Cl material colour
					});
				}
			});

	public static final RegistryObject<FlowingFluid> CHLORINE = FLUIDS.register("chlorine",
			() -> new ForgeFlowingFluid.Source(chlorineProperties()));

	public static final RegistryObject<Fluid> CHLORINE_FLOWING = FLUIDS.register("chlorine_flowing",
			() -> new ForgeFlowingFluid.Flowing(chlorineProperties()));

	public static final RegistryObject<LiquidBlock> CHLORINE_BLOCK = BLOCKS.register("chlorine_block",
			() -> new LiquidBlock(CHLORINE, BlockBehaviour.Properties.of()
					.noCollission().noLootTable())); // a gas block: the natural_gas shape

	/**
	 * The 16 dye-chemical registrations — one line per dye index, tint from
	 * {@link GTSprayCanItem#DYES_INT}, appended after the food block in the static-init
	 * order (the DeferredRegister fields accumulate, the entries fire with the existing
	 * {@link #onModConstruct}, types before fluids).
	 */
	public static final List<DyeChemicalFluid> DYE_CHEMICALS = List.of(
			dyeChemicalFluid(0), dyeChemicalFluid(1), dyeChemicalFluid(2), dyeChemicalFluid(3),
			dyeChemicalFluid(4), dyeChemicalFluid(5), dyeChemicalFluid(6), dyeChemicalFluid(7),
			dyeChemicalFluid(8), dyeChemicalFluid(9), dyeChemicalFluid(10), dyeChemicalFluid(11),
			dyeChemicalFluid(12), dyeChemicalFluid(13), dyeChemicalFluid(14), dyeChemicalFluid(15));

	/** The family row of a dye index (the Canner refill seam, order = {@link GTSprayCanItem#DYE_IDS}). */
	public static DyeChemicalFluid dyeChemical(int aIndex) {
		return DYE_CHEMICALS.get(aIndex);
	}

	/**
	 * {@code gt6:cfoam} + the 32-family C-Foam fluid domain (task p26-c-foam-fluid-refill).
	 *
	 * <p>The BASE fluid is the {@code FL.CFoam} counterpart — FL.java:432
	 * {@code CFoam("ic2constructionfoam", LIQUID) // 100 per Unit}. Per the
	 * decisions.p26-cfoam-fluid-naming ruling the port does NOT keep the IC2 compat
	 * registry name (no modern IC2 exists; cross-mod compat rides the tag paradigm, the
	 * P24 ruling) — the base registers as {@code gt6:cfoam}, the family name root.
	 *
	 * <p>The 32 FAMILY fluids are the Loader_Fluids.java:124-125 loop verbatim over the
	 * port dye order: {@code gt6:cfoam_<DYE_IDS[i]>} (the :124 {@code "cfoam." + colour}
	 * rows) and {@code gt6:cfoam_owned_<DYE_IDS[i]>} (the :125 {@code "cfoam.owned." +
	 * colour} "Advanced" rows) — 16 + 16, every tint the shared
	 * {@link GTSprayCanItem#DYES_INT}[i] table (zero new colour data, the P24 single
	 * colour source), one borrowed grayscale carrier PNG tinted per family. The consumers
	 * this card pours: the Canner refills (MultiItemRandomTools.java:254/:262, 256 x
	 * {@link #CFOAM_BUCKET_UNITS} = 25600 mB per can) and the Mixer rock/Pd rows
	 * (Loader_Recipes_Other.java:251-304/:485-486) via {@code GT6RecipesMixer}.
	 *
	 * <p>Shape: the four-DR-with-block template (the {@link DyeChemicalFluid} form
	 * verbatim — the P24 precedent this family extends). Carrier values: temperature 300 K
	 * (the :124-125 FL.create literal) with the honest FluidType defaults density 1000 /
	 * viscosity 1000 (the dye-chemical precedent). {@link #CFOAM_BUCKET_UNITS} = 100 is
	 * the FL.java:432 "// 100 per Unit" amount semantics — the upstream
	 * {@code FL.create(..., 100, ...)} bucket amount and the multiplier root of the
	 * refill rows ({@code FL.mul(DYED_C_FOAMS[i], 256)}); 1.7.10 FluidStack.amount IS mB
	 * (the P24 R4 ruling), so the port amounts translate 1:1.
	 */
	public static final class CFoamFluid {
		/** The GT6 dye index ({@code 0..15}, the {@link GTSprayCanItem#DYE_IDS} row). */
		public final int dyeIndex;
		/** The :125 "Advanced" owned variant flag — same tint/texture, distinct registry path. */
		public final boolean owned;
		public final RegistryObject<FluidType> type;
		public final RegistryObject<FlowingFluid> source;
		public final RegistryObject<Fluid> flowing;
		public final RegistryObject<LiquidBlock> block;

		CFoamFluid(int aDyeIndex, boolean aOwned, RegistryObject<FluidType> aType, RegistryObject<FlowingFluid> aSource,
				RegistryObject<Fluid> aFlowing, RegistryObject<LiquidBlock> aBlock) {
			dyeIndex = aDyeIndex; owned = aOwned; type = aType; source = aSource; flowing = aFlowing; block = aBlock;
		}

		/** The gt6 registry path — {@code cfoam[_owned]_} + the {@link GTSprayCanItem#DYE_IDS}[i] snake id. */
		public String name() {return owned ? cfoamOwnedName(dyeIndex) : cfoamName(dyeIndex);}

		/** The tint over the grayscale carrier — {@link GTSprayCanItem#DYES_INT}[i], the single colour source. */
		public int tint() {return GTSprayCanItem.DYES_INT[dyeIndex];}

		/** The en_us display — the upstream :124/:125 compose verbatim, {@code ["Advanced "] + DYE_NAMES[i] + " C-Foam"}. */
		public String displayName() {return (owned ? "Advanced " : "") + GTSprayCanItem.DYE_NAMES[dyeIndex] + " C-Foam";}

		/** The lang/description key, the descriptionId the FluidType is registered with. */
		public String descriptionId() {return "fluid.gt6." + name();}
	}

	/** The gt6 registry path of the dye index — {@code cfoam_} + the {@link GTSprayCanItem#DYE_IDS}[i] snake id (the :124 {@code "cfoam." + colour} rows). */
	public static String cfoamName(int aIndex) {
		return "cfoam_" + GTSprayCanItem.DYE_IDS[aIndex];
	}

	/** The gt6 registry path of the owned dye index — {@code cfoam_owned_} + the snake id (the :125 {@code "cfoam.owned." + colour} rows). */
	public static String cfoamOwnedName(int aIndex) {
		return "cfoam_owned_" + GTSprayCanItem.DYE_IDS[aIndex];
	}

	/** The dye index of a dyed ({@code cfoam_<dye>}) registry path, or −1 (the inverse of {@link #cfoamName}). */
	public static int cfoamIndexOf(String aName) {
		for (int i = 0; i < 16; i++) if (cfoamName(i).equals(aName)) return i;
		return -1;
	}

	/** The dye index of an owned ({@code cfoam_owned_<dye>}) registry path, or −1 (the inverse of {@link #cfoamOwnedName}). */
	public static int cfoamOwnedIndexOf(String aName) {
		for (int i = 0; i < 16; i++) if (cfoamOwnedName(i).equals(aName)) return i;
		return -1;
	}

	/** The Loader_Fluids.java:124-125 FL.create temperature literal (300 K) — the declared carrier the FluidType registers with. */
	public static final int CFOAM_TEMPERATURE = 300;

	/** The honest FluidType default density, declared explicitly (the dye-chemical precedent). */
	public static final int CFOAM_DENSITY = 1000;

	/**
	 * FL.java:432 {@code // 100 per Unit} — the :124-125 {@code FL.create(..., 100, ...)}
	 * bucket amount. The multiplier root of the Canner refills: 256 units per can
	 * (MultiItemRandomTools.java:254/:262 {@code FL.mul(DYED_C_FOAMS[i], 256)}) = 25600 mB.
	 */
	public static final int CFOAM_BUCKET_UNITS = 100;

	private static ForgeFlowingFluid.Properties cfoamBaseProperties() {
		// the four-DR template again (the chlorine hand-row shape)
		return new ForgeFlowingFluid.Properties(CFOAM_TYPE, CFOAM, CFOAM_FLOWING)
				.block(CFOAM_BLOCK);
	}

	/**
	 * {@code gt6:cfoam} — the base fluid, the decisions.p26-cfoam-fluid-naming ruling row
	 * (the FL.java:432 "ic2constructionfoam" counterpart under the port namespace; no IC2
	 * compat name). The Mixer base rock-group rows (Loader_Recipes_Other.java:252-254)
	 * produce this fluid; the untinted grayscale carrier IS the construction-foam grey the
	 * IC2 base carries (the dyed rows tint the same PNG). No bucket item.
	 */
	public static final RegistryObject<FluidType> CFOAM_TYPE = FLUID_TYPES.register("cfoam",
			() -> new FluidType(FluidType.Properties.create()
					.descriptionId("fluid.gt6.cfoam")
					.temperature(CFOAM_TEMPERATURE)
					.density(CFOAM_DENSITY)
					.viscosity(1000)) {
				@Override
				public void initializeClient(Consumer<IClientFluidTypeExtensions> aConsumer) {
					aConsumer.accept(new IClientFluidTypeExtensions() {
						// the upstream single grayscale carrier, still = flow (Loader_Fluids.java:118
						// tDyedCFoam shared by the whole family; the PNG is the byte-identical borrow)
						private static final ResourceLocation STILL =
								ResourceLocation.fromNamespaceAndPath("gt6", "block/fluids/cfoam");
						private static final ResourceLocation FLOW = STILL;

						@Override
						public ResourceLocation getStillTexture() {return STILL;}

						@Override
						public ResourceLocation getFlowingTexture() {return FLOW;}

						@Override
						public int getTintColor() {return 0xFFFFFFFF;} // untinted: the carrier IS the base grey
					});
				}
			});

	public static final RegistryObject<FlowingFluid> CFOAM = FLUIDS.register("cfoam",
			() -> new ForgeFlowingFluid.Source(cfoamBaseProperties()));

	public static final RegistryObject<Fluid> CFOAM_FLOWING = FLUIDS.register("cfoam_flowing",
			() -> new ForgeFlowingFluid.Flowing(cfoamBaseProperties()));

	public static final RegistryObject<LiquidBlock> CFOAM_BLOCK = BLOCKS.register("cfoam_block",
			() -> new LiquidBlock(CFOAM, BlockBehaviour.Properties.of()
					.noCollission().strength(100.0F).noLootTable())); // a liquid: the iron_molten block ramp

	/** The shared per-family Properties for the C-Foam rows — registry-event time only (the dyeChemicalProperties seam shape). */
	private static ForgeFlowingFluid.Properties cfoamProperties(RegistryObject<FluidType> aType, String aName) {
		return new ForgeFlowingFluid.Properties(aType, SOURCE_SEAM.get(aName), FLOWING_SEAM.get(aName))
				.block(BLOCK_SEAM.get(aName));
	}

	/** The table-driven registration helper for one C-Foam row: FluidType + Source/Flowing + LiquidBlock (the four-DR-with-block template). */
	private static CFoamFluid cfoamFluid(int aIndex, boolean aOwned) {
		String tName = aOwned ? cfoamOwnedName(aIndex) : cfoamName(aIndex);
		int tTint = GTSprayCanItem.DYES_INT[aIndex]; // the single colour source, captured once
		RegistryObject<FluidType> tType = FLUID_TYPES.register(tName, () -> new FluidType(FluidType.Properties.create()
				.descriptionId("fluid.gt6." + tName)
				.temperature(CFOAM_TEMPERATURE) // the Loader_Fluids.java:124-125 FL.create literal
				.density(CFOAM_DENSITY)         // the honest FluidType default, declared explicitly
				.viscosity(1000)) {
			@Override
			public void initializeClient(Consumer<IClientFluidTypeExtensions> aConsumer) {
				aConsumer.accept(new IClientFluidTypeExtensions() {
					// the upstream single grayscale carrier, still = flow (:118 tDyedCFoam shared
					// by the whole family; the PNG is the byte-identical borrow)
					private static final ResourceLocation STILL =
							ResourceLocation.fromNamespaceAndPath("gt6", "block/fluids/cfoam");
					private static final ResourceLocation FLOW = STILL;

					@Override
					public ResourceLocation getStillTexture() {return STILL;}

					@Override
					public ResourceLocation getFlowingTexture() {return FLOW;}

					@Override
					public int getTintColor() {return tTint;} // GTSprayCanItem.DYES_INT[aIndex] — zero new colour data
				});
			}
		});
		RegistryObject<FlowingFluid> tSource = FLUIDS.register(tName,
				() -> new ForgeFlowingFluid.Source(cfoamProperties(tType, tName)));
		RegistryObject<Fluid> tFlowing = FLUIDS.register(tName + "_flowing",
				() -> new ForgeFlowingFluid.Flowing(cfoamProperties(tType, tName)));
		// the block leg: 1.20.1 Forge takes the supplier handle; 21.1 vanilla takes the
		// resolved fluid (the DYE_CHEMICALS stonecutter fork — a camelCase local needs the
		// explicit fork, the rewrite only matches UPPER_CASE fields)
		//? if forge {
		RegistryObject<LiquidBlock> tBlock = BLOCKS.register(tName + "_block",
				() -> new LiquidBlock(tSource, BlockBehaviour.Properties.of()
						.noCollission().strength(100.0F).noLootTable())); // a liquid: the iron_molten block ramp
		//?} else {
		/*RegistryObject<LiquidBlock> tBlock = BLOCKS.register(tName + "_block",
				() -> new LiquidBlock(tSource.get(), BlockBehaviour.Properties.of()
						.noCollission().strength(100.0F).noLootTable())); // a liquid: the iron_molten block ramp (FLUID before BLOCK, the resolved .get() is live)
		*///?}
		SOURCE_SEAM.put(tName, tSource);
		FLOWING_SEAM.put(tName, tFlowing);
		BLOCK_SEAM.put(tName, tBlock);
		return new CFoamFluid(aIndex, aOwned, tType, tSource, tFlowing, tBlock);
	}

	/**
	 * The 16 dyed C-Foam registrations — one line per dye index, the Loader_Fluids.java:124
	 * {@code DYED_C_FOAMS[i]} loop, tint from {@link GTSprayCanItem#DYES_INT}, appended
	 * after the dye-chemical block in the static-init order (the DeferredRegister fields
	 * accumulate, the entries fire with the existing {@link #onModConstruct}).
	 */
	public static final List<CFoamFluid> CFOAMS = List.of(
			cfoamFluid(0, false), cfoamFluid(1, false), cfoamFluid(2, false), cfoamFluid(3, false),
			cfoamFluid(4, false), cfoamFluid(5, false), cfoamFluid(6, false), cfoamFluid(7, false),
			cfoamFluid(8, false), cfoamFluid(9, false), cfoamFluid(10, false), cfoamFluid(11, false),
			cfoamFluid(12, false), cfoamFluid(13, false), cfoamFluid(14, false), cfoamFluid(15, false));

	/**
	 * The 16 owned "Advanced" C-Foam registrations — the Loader_Fluids.java:125
	 * {@code DYED_C_FOAMS_OWNED[i]} loop, the {@link #CFOAMS} shape over the
	 * {@code cfoam_owned_} id ladder.
	 */
	public static final List<CFoamFluid> CFOAMS_OWNED = List.of(
			cfoamFluid(0, true), cfoamFluid(1, true), cfoamFluid(2, true), cfoamFluid(3, true),
			cfoamFluid(4, true), cfoamFluid(5, true), cfoamFluid(6, true), cfoamFluid(7, true),
			cfoamFluid(8, true), cfoamFluid(9, true), cfoamFluid(10, true), cfoamFluid(11, true),
			cfoamFluid(12, true), cfoamFluid(13, true), cfoamFluid(14, true), cfoamFluid(15, true));

	/** The family row of a dye index (the Canner refill seam, order = {@link GTSprayCanItem#DYE_IDS}). */
	public static CFoamFluid cfoam(int aIndex, boolean aOwned) {
		return aOwned ? CFOAMS_OWNED.get(aIndex) : CFOAMS.get(aIndex);
	}

	/**
	 * One chemical-family declaration row (task p29-w4-f1-chemicals) — the offline-readable
	 * half, the {@link AquaFluidSpec} shape (display name inline) plus the gas flag and the
	 * luminosity the two plasma rows carry (the STATE_PLASMA lum 15, FL.java:1106).
	 */
	public record ChemicalFluidSpec(String name, String displayName, int temperature, int density,
			int viscosity, int tint, boolean gas, int luminosity) {
		/** The lang/description key, the descriptionId the FluidType is registered with. */
		public String descriptionId() {return "fluid.gt6." + name;}
	}

	/**
	 * The twenty-five chemical rows (task p29-w4-f1-chemicals), declaration order = the
	 * upstream Loader_Fluids.java block order (:40-41 plasmas, :45-48 hydrocarbons,
	 * :59-63 oils, the :66-68 oxygen leg, the createGas closure). Every value census-anchored:
	 * <ul>
	 * <li><b>Oils</b> (:59-63) — the {@code FL.create(name, display, null, 1)} four-arg form
	 *     rides the 300 K / viscosity-1000 STATE_LIQUID carriers with the row's literal
	 *     {@code setDensity} (900/800/700/600/650); tints are PORT-OWNED DECLARED VALUES (the
	 *     JetFuel/aqua precedent — the upstream fluids carry dedicated PNGs this port does
	 *     not have), an oil-dark family ramp over the creosote :242 hue;</li>
	 * <li><b>Cracked hydrocarbons</b> (:45-48) — the four-arg form with STATE_GASEOUS
	 *     carriers (viscosity 200 / setGaseous, FL.java:1105); propane/butane override the
	 *     density to the literal −1000, propylene/ethylene ride the :1130 formula over the
	 *     1.0 g/cm³ default (OreDictMaterial.java:240) → 1000;
	 *     tints are the materials' RGBa (MT.java:1206-1209);</li>
	 * <li><b>Gas closure</b> — the createGas walk semantics (FL.java:1080: the bare
	 *     material-name id, STATE_GASEOUS carriers, temperature = the :1080 rule) with every
	 *     element's {@code plasma = boiling × 100} (OreDictMaterial.java:927) so the rule
	 *     lands on {@code min(300, plasma − 1) = 300 K} for all thirteen; densities are the
	 *     :1128-1136 formula transcribed per material ({@code g/cm³ > 0.0012 air →
	 *     (long)(1000·g)}, {@code < air → (long)(−0.1/g)}): the ELEMENT gases carry their
	 *     measured g/cm³ (nitrogen 0.0012506 → 1, oxygen 0.001429 → 1, fluorine 0.001696 → 1,
	 *     argon 0.0017837 → 1, krypton 0.003733 → 3, xenon 0.005887 → 5, radon 0.00973 → 9;
	 *     hydrogen 0.00008988 → −1112, helium 0.0001785 → −560, neon 0.0008999 → −111), while
	 *     the COMPOUNDS ride the molecule-configuration recomputation
	 *     (OreDictMaterial.java:240 — the field default 1.0, never cleared — and :478-492
	 *     {@code setMoleculeConfiguration}: the uumMcfg rows sum the constituent g/cm³ over
	 *     the recipe ratios — CH4 = 2.267 (MT.java:392 carbon) + 4×0.00008988 → 2.2674 →
	 *     2267, CO2 → 2.2699 → 2269, CO → 2.2684 → 2268; the no-uumMcfg hydrocarbons
	 *     propylene/ethylene keep the default 1.0 → 1000) — the formula applies to GASEOUS
	 *     whenever g &gt; 0, so every compound gas SINKS (the pushByGravity sign consumers,
	 *     lesson id224); tints are the materials' RGBa (MT.java:380/:383/:395-
	 *     398/:406/:425/:443/:476/:1027-1037);</li>
	 * <li><b>Liquid oxygen</b> (:68) — the six-arg form 85 K over MT.O, the LIQUID viscosity
	 *     carrier, density = the :1130 formula over O's 0.001429 g/cm³ → 1, tint the O
	 *     RGBa (MT.java:396);</li>
	 * <li><b>Plasmas</b> (:40-41) — the STATE_PLASMA carriers (viscosity 10 / density
	 *     −100000 / luminosity 15 / setGaseous, FL.java:1106) at the 10000 K literal; the
	 *     upstream createPlasma amount is L×L mB per unit (FL.java:1086) — the port
	 *     registers the fluid at its L semantics and the AMOUNT question stays the Fusion
	 *     card's (the pool-bottom ruling, decisions.p29-w4-split-rulings conflicts[2]);
	 *     tints are the parent materials' RGBa (MT.java:383/:395).</li>
	 * </ul>
	 */
	public static final List<ChemicalFluidSpec> CHEMICAL_SPECS = List.of(
		// the plasmas (Loader_Fluids.java:40-41 — the upstream block order)
		new ChemicalFluidSpec("helium_plasma"   , "Helium Plasma"    , 10000, -100000,   10, 0xFFFFFF78, true , 15), // MT.He 255,255,120 (MT.java:383)
		new ChemicalFluidSpec("nitrogen_plasma" , "Nitrogen Plasma"  , 10000, -100000,   10, 0xFF0096C8, true , 15), // MT.N 0,150,200 (MT.java:395)
		// the cracked hydrocarbons (:45-48)
		new ChemicalFluidSpec("propane"         , "Propane"          ,   300,  -1000,  200, 0xFFFF1414, true ,  0), // MT.Propane 255,20,20 (MT.java:1206); the :45 density literal
		new ChemicalFluidSpec("butane"          , "Butane"           ,   300,  -1000,  200, 0xFFFF2828, true ,  0), // MT.Butane 255,40,40 (MT.java:1207); the :46 density literal
		new ChemicalFluidSpec("propylene"       , "Propylene"        ,   300,   1000,  200, 0xFF5A3C8C, true ,  0), // MT.Propylene 90,60,140 (MT.java:1208); the :1130 formula over the default 1.0 g/cm³
		new ChemicalFluidSpec("ethylene"        , "Ethylene"         ,   300,   1000,  200, 0xFF402864, true ,  0), // MT.Ethylene 64,40,100 (MT.java:1209); the :1130 formula over the default 1.0 g/cm³
		// the oils (:59-63)
		new ChemicalFluidSpec("liquid_extra_heavy_oil", "Very Heavy Oil", 300,   900, 1000, 0xFF1E140A, false,  0), // :59 — the declared oil-dark ramp (port-owned tints)
		new ChemicalFluidSpec("liquid_heavy_oil", "Heavy Oil"        ,   300,    800, 1000, 0xFF28180A, false,  0), // :60
		new ChemicalFluidSpec("liquid_medium_oil", "Raw Oil"         ,   300,    700, 1000, 0xFF322814, false,  0), // :61
		new ChemicalFluidSpec("liquid_light_oil", "Light Oil"        ,   300,    600, 1000, 0xFF4A3818, false,  0), // :62
		new ChemicalFluidSpec("soulsandoil"     , "Soulsand Oil"     ,   300,    650, 1000, 0xFF2E2030, false,  0), // :63 — the soulsand violet-brown (declared)
		// the gas closure — the FL.java:1080 createGas walk, densities per the :1128-1136 formula
		new ChemicalFluidSpec("methane"         , "Methane"          ,   300,   2267,  200, 0xFFC8C8FA, true ,  0), // MT.CH4 (MT.java:1037); the molecule-configuration g/cm³ 2.2674 (C 2.267 + 4 H) → 1000×g
		new ChemicalFluidSpec("carbondioxide"   , "Carbon Dioxide"   ,   300,   2269,  200, 0xFF282828, true ,  0), // MT.CO2 (MT.java:1035); the molecule g/cm³ 2.2699 (C + 2 O) → 1000×g
		new ChemicalFluidSpec("carbonmonoxide"  , "Carbon Monoxide"  ,   300,   2268,  200, 0xFF0A0A0A, true ,  0), // MT.CO (MT.java:1034); the molecule g/cm³ 2.2684 (C + O) → 1000×g
		new ChemicalFluidSpec("hydrogen"        , "Hydrogen"         ,   300,  -1112,  200, 0xFF0000FF, true ,  0), // MT.H2 0,0,255 (MT.java:380); −0.1/0.00008988
		new ChemicalFluidSpec("nitrogen"        , "Nitrogen"         ,   300,      1,  200, 0xFF0096C8, true ,  0), // MT.N2 (MT.java:395); 1000×0.0012506
		new ChemicalFluidSpec("oxygen"          , "Oxygen"           ,   300,      1,  200, 0xFF0064C8, true ,  0), // MT.O2 (MT.java:396); 1000×0.001429
		new ChemicalFluidSpec("fluorine"        , "Fluorine"         ,   300,      1,  200, 0xFF40C000, true ,  0), // MT.F2 (MT.java:397); 1000×0.001696
		new ChemicalFluidSpec("helium"          , "Helium"           ,   300,   -560,  200, 0xFFFFFF78, true ,  0), // MT.He (MT.java:383); −0.1/0.0001785
		new ChemicalFluidSpec("neon"            , "Neon"             ,   300,   -111,  200, 0xFFC8B4B4, true ,  0), // MT.Ne 250,180,180 (MT.java:398); −0.1/0.0008999
		new ChemicalFluidSpec("argon"           , "Argon"            ,   300,      1,  200, 0xFF00FF00, true ,  0), // MT.Ar 0,255,0 (MT.java:406); 1000×0.0017837
		new ChemicalFluidSpec("krypton"         , "Krypton"          ,   300,      3,  200, 0xFF80FF80, true ,  0), // MT.Kr 128,255,128 (MT.java:425); 1000×0.003733
		new ChemicalFluidSpec("xenon"           , "Xenon"            ,   300,      5,  200, 0xFF00FFFF, true ,  0), // MT.Xe 0,255,255 (MT.java:443); 1000×0.005887
		new ChemicalFluidSpec("radon"           , "Radon"            ,   300,      9,  200, 0xFFFF00FF, true ,  0), // MT.Rn 255,0,255 (MT.java:476); 1000×0.00973
		// liquid oxygen (:68)
		new ChemicalFluidSpec("liquidoxygen"    , "Liquid Oxygen"    ,    85,      1, 1000, 0xFF0064C8, false,  0), // the :1130 formula over O's 0.001429 g/cm³; tint = the O RGBa
		// the isotope batch (task p31-qu-b-materials) — the Loader_Fluids.java:658-662
		// tag-driven loop rows for the fusion isotope materials, in material-id order
		// (D 11, T 12, He3 21, Li6 31, Be7 41, Be8 42, B11 51, C13 61, Ad 8744). The gas
		// rows ride the :1080 createGas walk (temp = min(300, plasma−1) = 300 over the
		// default 10000 K plasma point, density = the :1128-1136 −0.1/g formula), the
		// molten rows the :1077 createMolten walk (temp = the melting point, density =
		// 1000·g, the luminosity 10 literal); ids turn the upstream "molten.<mat>" prefix
		// into the enderpearl_molten suffix form. MT.Dilithium has NO row — the upstream
		// crystal helper carries no GASES/MOLTEN/LIQUID tag (MT.java:198), so the loop
		// never reached it (the binding must stay absent).
		new ChemicalFluidSpec("deuterium"           , "Deuterium"             ,  300, -1112,  200, 0xFFFFFF00, true ,  0), // MT.D 255,255,0 (MT.java:381); −0.1/0.00008988, same g/cm³ as hydrogen
		new ChemicalFluidSpec("tritium"             , "Tritium"               ,  300, -1112,  200, 0xFFFF0000, true ,  0), // MT.T 255,0,0 (:382)
		new ChemicalFluidSpec("helium3"             , "Helium-3"              ,  300,  -560,  200, 0xFFFF8C00, true ,  0), // MT.He_3 255,255,140 (:384); −0.1/0.0001785, same g/cm³ as helium
		new ChemicalFluidSpec("lithium6_molten"     , "Molten Lithium-6"      ,  453,   534, 1000, 0xFFE6E1FF, false, 10), // molten.lithium6 — MT.Li_6 mp 453, 1000×0.534 (:386)
		new ChemicalFluidSpec("beryllium7_molten"   , "Molten Beryllium-7"    , 1560,  1850, 1000, 0xFF6EBE6E, false, 10), // molten.beryllium7 — MT.Be_7 mp 1560, 1000×1.85 (:388)
		new ChemicalFluidSpec("beryllium8_molten"   , "Molten Beryllium-8"    , 1560,  1850, 1000, 0xFF6EC86E, false, 10), // molten.beryllium8 — MT.Be_8 mp 1560, 1000×1.85 (:389)
		new ChemicalFluidSpec("boron11_molten"      , "Molten Boron-11"       , 2349,  2340, 1000, 0xFFF0F0F0, false, 10), // molten.boron11 — MT.B_11 mp 2349, 1000×2.34 (:391)
		new ChemicalFluidSpec("carbon13_molten"     , "Molten Carbon-13"      , 3800,  2267, 1000, 0xFF191919, false, 10), // molten.carbon13 — MT.C_13 mp 3800, 1000×2.267 (:393)
		new ChemicalFluidSpec("ancientdebris_molten", "Molten Ancient Debris" , 2011,  1000, 1000, 0xFF6E505A, false, 10), // molten.ancientdebris — MT.AncientDebris heat(MeteoricIron) = Fe.mp+200 = 2011 (MT.java:1832/:414); the 1.0 g/cm³ field default → 1000
		// the fusion-row closure quartet (task p31-fusion) — the four parent-material MOLTEN
		// rows the fusion recipe rows reference (Loader_Recipes_Other.java:955/:957/:962/:964/
		// :965/:966 ride MT.C/MT.Li/MT.W/MT.Ad .liquid(), the molten.fluid the :658-662 loop
		// created for their MOLTEN tags): the same :1077 createMolten walk constants as the
		// isotope batch. (MT.Ad is ADAMANTIUM upstream — MT.java:794 — NOT Ancient Debris;
		// the ancientdebris_molten row above is the massfab Ender material.)
		new ChemicalFluidSpec("carbon_molten"       , "Molten Carbon"         , 3800,  2267, 1000, 0xFF141414, false, 10), // molten.carbon=熔融碳 (tmp/gregtech.lang:466) — MT.C mp 3800, 1000×2.267 (MT.java:392)
		new ChemicalFluidSpec("lithium_molten"      , "Molten Lithium"        ,  453,   534, 1000, 0xFFE1DCFF, false, 10), // molten.lithium=熔融锂 (:529) — MT.Li mp 453, 1000×0.534 (:385)
		new ChemicalFluidSpec("tungsten_molten"     , "Molten Tungsten"       , 3695, 19250, 1000, 0xFF323232, false, 10), // molten.tungsten=熔融钨 (:624) — MT.W mp 3695, 1000×19.25 (:463)
		new ChemicalFluidSpec("adamantium_molten"   , "Molten Adamantium"     , 5225, 13356, 1000, 0xFFFFFFFF, false, 10), // molten.adamantium=熔融艾德曼合金 (:428) — MT.Ad mp 5225, 1000×13.356 (MT.java:794)
		// the replicator-carrier row (task p32-qu-scanner-replicator) — the :194 explicit
		// FL.create("molten.redstone", "Molten Redstone", MT.Redstone, 1, L, 500)
		// .setLuminosity(5) literal: STATE_LIQUID carriers (viscosity 1000), temperature 500 K
		// = the MT.Redstone melting point verbatim (MT.java:2326 heat(500, 1500)), the :1128
		// density formula over Redstone's field-default 1.0 g/cm³ → 1000, luminosity 5. The
		// carrier of the six molten-redstone replication rows (Loader_Recipes_Other.java
		// :941-946, L/4..L*8) — lands FIRST so the rows never reference an unregistered id
		// (the dead-row class the GTFluids :1798 ruling cuts). The id turns the upstream
		// "molten.<mat>" literal into the enderpearl_molten suffix form; tint = the material
		// RGBa (200, 0, 0, MT.java:2326).
		new ChemicalFluidSpec("redstone_molten"     , "Molten Redstone"       ,  500,  1000, 1000, 0xFFC80000, false,  5), // molten.redstone=熔融红石 (tmp/gregtech.lang:584)
		// the crystallisation-crucible molten quintet (task p34-machines-bumblelyzer-crucible) —
		// the :1077 createMolten walk rows the 39 boule rows reference as their molten leg
		// (Loader_Recipes_Other.java:683-706: MT.Si/MT.Ge/MT.RedstoneAlloy/MT.NikolineAlloy/
		// MT.Al2O3 .liquid(...) carriers; lands FIRST so the JSON rows never reference an
		// unregistered id — the dead-row class the GTFluids :1798 ruling cuts). Ids = the
		// sanitized internal name + the "_molten" suffix (the specOf/materialOf seam, the
		// ancientdebris_molten form). Temperatures = the material melting points (Si/Ge the
		// metalloid literals MT.java:987/:1025; the alloys the :398 component-weighted average
		// — Redstone (1687+500)/2 = 1093, Nikoline (1687+1500)/2 = 1593; Alumina the
		// .heat(2345, 3250) literal MT.java:1955); densities = the :1128-1136 formula over the
		// live g/cm³ (Si 2.3296 → 2329, Ge 5.323 → 5323, the alloys the stealStatsElement(Si)
		// 2.3296 → 2329, Alumina the uumMcfg 2×2.7 + 3×0.001429 → 5404); tints = the material
		// RGBa; luminosity 10 = the createMolten literal.
		new ChemicalFluidSpec("silicon_molten"      , "Molten Silicon"        , 1687,  2329, 1000, 0xFF3C3C50, false, 10), // molten.silicon=熔融硅 (tmp/gregtech.lang:593) — MT.Si mp 1687 (MT.java:987)
		new ChemicalFluidSpec("germanium_molten"    , "Molten Germanium"      , 1211,  5323, 1000, 0xFFD4D4D4, false, 10), // molten.germanium=熔融锗 (tmp/gregtech.lang:503) — MT.Ge mp 1211, 1000×5.323 (MT.java:1025)
		new ChemicalFluidSpec("redstonealloy_molten", "Molten Redstone Alloy" , 1093,  2329, 1000, 0xFF8C3232, false, 10), // molten.redstonealloy=熔融红石合金 (tmp/gregtech.lang:585) — (Si 1687 + Redstone 500)/2 (MT.java:2492)
		new ChemicalFluidSpec("nikolinealloy_molten", "Molten Nikoline Alloy" , 1593,  2329, 1000, 0xFF325A8C, false, 10), // molten.nikolinealloy=熔融蓝石合金 (tmp/gregtech.lang:566) — (Si 1687 + Nikolite 1500)/2 (MT.java:2493)
		new ChemicalFluidSpec("alumina_molten"      , "Molten Alumina"        , 2345,  5404, 1000, 0xFF78C3EB, false, 10), // molten.alumina=熔融氧化铝 (tmp/gregtech.lang:429) — MT.Al2O3 internal "Alumina", the heat(2345) literal (MT.java:1955)
		// the Burner Mixer row carriers (task p34-machines-burner-plantalyzer) — the four
		// material-state fluids the Loader_Recipes_Chem.java:220-259 rows reference. The
		// auto-registration walks the port folds into this table: tritiatedwater is the
		// :1072 createLiquid walk over MT.T2O (LIQUID tag, MT.java:1898) — temp = min(300,
		// bp−1) = 300 over mp 280 (heat(CS.C+7, CS.C+104), CS.C = 273), density 1000×1.2112
		// = 1211 (the explicit setDensity); titaniumtetrachloride the same walk over
		// MT.TiCl4 (MT.java:1959, mp 249 < 300 → 300, the field-default 1.0 g/cm³ → 1000);
		// the two molten rows the :1077 createMolten walk over MT.Na2CO3 / MT.CaCO3
		// (MOLTEN tags, MT.java:2005/:1975) — temp = the melting points 1124/1612, density
		// 1000 (the 1.0 field default; the uumMcfg molecule recompute chain through the CO3
		// radical is DECLARED untranscribed — the oils port-owned-value precedent).
		new ChemicalFluidSpec("tritiatedwater"      , "Tritiated Water"       ,  300,  1211, 1000, 0xFF6464FF, false,  0), // tritiatedwater=超重水 (tmp/gregtech.lang:897) — MT.T2O 255,100,100 (MT.java:1898)
		new ChemicalFluidSpec("titaniumtetrachloride", "Titanium Tetrachloride",  300,  1000, 1000, 0xFFE9F4DE, false,  0), // titaniumtetrachloride=四氯化钛 (:892) — MT.TiCl4 233,244,222 (MT.java:1959)
		new ChemicalFluidSpec("sodiumcarbonate_molten", "Molten Sodium Carbonate", 1124, 1000, 1000, 0xFFE6E6E6, false, 10), // molten.sodiumcarbonate=熔融碳酸钠 (:597) — MT.Na2CO3 230,230,230 (MT.java:2005)
		new ChemicalFluidSpec("calcite_molten"      , "Molten Calcite"        , 1612,  1000, 1000, 0xFFFAE6DC, false, 10)); // molten.calcite=熔融方解石 (:463) — MT.CaCO3 250,230,220 (MT.java:1975)

	/** The chemical row for a gt6 id path, or null (the {@link #engineSpec} lookup shape). */
	public static ChemicalFluidSpec chemicalSpec(String aName) {
		for (ChemicalFluidSpec tSpec : CHEMICAL_SPECS) if (tSpec.name().equals(aName)) return tSpec;
		return null;
	}

	/**
	 * The material→spec leg of the isotope binding seam (task p31-qu-b-materials): the id
	 * convention of the upstream Loader_Fluids.java:658-662 tag-driven loop — a gas row is
	 * the material's sanitized internal name lowercased (FL.createGas:1080 registers
	 * {@code mNameInternal.toLowerCase()}), a molten row turns the upstream
	 * {@code "molten." + name} prefix into the {@code name + "_molten"} suffix (the
	 * enderpearl_molten port ruling). {@link OreDictMaterial#sanitize} already stripped
	 * spaces and minuses upstream (createMaterial :147), so the id is the plain lowercase
	 * internal name plus the suffix. Returns null for materials the tag loop never
	 * reached — MT.Dilithium carries no GASES/MOLTEN tag and stays unbound.
	 */
	public static ChemicalFluidSpec specOf(OreDictMaterial aMaterial, boolean aMolten) {
		if (aMaterial == null) return null;
		return chemicalSpec(aMaterial.mNameInternal.toLowerCase() + (aMolten ? "_molten" : ""));
	}

	/**
	 * The spec→material reverse leg: strip the molten suffix, match the lowercase internal
	 * name over the live registry (the {@link MaterialRegistry#MATERIAL_MAP} the root
	 * keeps in sync with the table). O(n) per call and no cache — the registry reset
	 * generation would poison one, and the expected consumer (the massfab row generator)
	 * walks the material side anyway.
	 */
	public static OreDictMaterial materialOf(ChemicalFluidSpec aSpec) {
		if (aSpec == null) return null;
		String tName = aSpec.name();
		if (tName.endsWith("_molten")) tName = tName.substring(0, tName.length() - "_molten".length());
		for (OreDictMaterial tMaterial : MaterialRegistry.INSTANCE.MATERIAL_MAP.values())
			if (tMaterial.mNameInternal.toLowerCase().equals(tName)) return tMaterial;
		return null;
	}

	/** One registered chemical family: the declared spec + the three live handles. Fluid-only — no bucket; the block face exists only where the worldgen block seam carries the name (the four oils, p31-fluid-spring). */
	public static final class ChemicalFluid {
		/** The declaration row ({@link #CHEMICAL_SPECS}); the offline-readable half. */
		public final ChemicalFluidSpec spec;
		public final RegistryObject<FluidType> type;
		public final RegistryObject<FlowingFluid> source;
		public final RegistryObject<Fluid> flowing;

		ChemicalFluid(ChemicalFluidSpec aSpec, RegistryObject<FluidType> aType, RegistryObject<FlowingFluid> aSource, RegistryObject<Fluid> aFlowing) {
			spec = aSpec; type = aType; source = aSource; flowing = aFlowing;
		}
	}

	/**
	 * The table-driven registration helper for one chemical row — the
	 * {@link #registerFluidFamily} shape plus the luminosity the plasmas carry
	 * ({@code .lightLevel}, the iron_molten :132 form). Client layers reuse the vanilla
	 * water textures over the row's tint (the family initializeClient convention).
	 */
	private static ChemicalFluid chemicalFluid(String aName) {
		return specFluid(chemicalSpec(aName), "chemical fluid");
	}

	/** The spec-first registration core of {@link #chemicalFluid} — shared with the hot/closure/lubricant families. */
	private static ChemicalFluid specFluid(ChemicalFluidSpec aSpec, String aFamily) {
		if (aSpec == null) throw new IllegalArgumentException("no " + aFamily + " spec");
		RegistryObject<FluidType> tType = FLUID_TYPES.register(aSpec.name(), () -> new FluidType(FluidType.Properties.create()
				.descriptionId(aSpec.descriptionId())
				.temperature(aSpec.temperature())
				.density(aSpec.density())
				.viscosity(aSpec.viscosity())
				.lightLevel(aSpec.luminosity())) {
			@Override
			public void initializeClient(Consumer<IClientFluidTypeExtensions> aConsumer) {
				aConsumer.accept(new IClientFluidTypeExtensions() {
					private static final ResourceLocation STILL = ResourceLocation.withDefaultNamespace("block/water_still");
					private static final ResourceLocation FLOW = ResourceLocation.withDefaultNamespace("block/water_flow");

					@Override
					public ResourceLocation getStillTexture() {return STILL;}

					@Override
					public ResourceLocation getFlowingTexture() {return FLOW;}

					@Override
					public int getTintColor() {return aSpec.tint();}
				});
			}
		});
		RegistryObject<FlowingFluid> tSource = FLUIDS.register(aSpec.name(),
				() -> new ForgeFlowingFluid.Source(chemicalProperties(aSpec, tType)));
		RegistryObject<Fluid> tFlowing = FLUIDS.register(aSpec.name() + "_flowing",
				() -> new ForgeFlowingFluid.Flowing(chemicalProperties(aSpec, tType)));
		SOURCE_SEAM.put(aSpec.name(), tSource);
		FLOWING_SEAM.put(aSpec.name(), tFlowing);
		return new ChemicalFluid(aSpec, tType, tSource, tFlowing);
	}

	/** The shared per-family Properties for the chemical rows — registry-event time only (see chemicalFluid). */
	private static ForgeFlowingFluid.Properties chemicalProperties(ChemicalFluidSpec aSpec, RegistryObject<FluidType> aType) {
		// the block leg rides the worldgen block face where the seam carries the name (p31-fluid-spring)
		return withWorldgenBlock(aType,
				new ForgeFlowingFluid.Properties(aType, SOURCE_SEAM.get(aSpec.name()), FLOWING_SEAM.get(aSpec.name())),
				aSpec.name());
	}

	/**
	 * The twenty-five chemical registrations — one per {@link #CHEMICAL_SPECS} row, in
	 * declaration order (the upstream Loader_Fluids.java block order: plasmas,
	 * hydrocarbons, oils, the gas closure, liquid oxygen). Static-init order: the
	 * DeferredRegister fields accumulate, the entries fire with the existing
	 * {@link #onModConstruct} (types before fluids).
	 */
	public static final List<ChemicalFluid> CHEMICALS = List.of(
			chemicalFluid("helium_plasma"), chemicalFluid("nitrogen_plasma"),
			chemicalFluid("propane"), chemicalFluid("butane"), chemicalFluid("propylene"), chemicalFluid("ethylene"),
			chemicalFluid("liquid_extra_heavy_oil"), chemicalFluid("liquid_heavy_oil"), chemicalFluid("liquid_medium_oil"),
			chemicalFluid("liquid_light_oil"), chemicalFluid("soulsandoil"),
			chemicalFluid("methane"), chemicalFluid("carbondioxide"), chemicalFluid("carbonmonoxide"), chemicalFluid("hydrogen"),
			chemicalFluid("nitrogen"), chemicalFluid("oxygen"), chemicalFluid("fluorine"),
			chemicalFluid("helium"), chemicalFluid("neon"), chemicalFluid("argon"),
			chemicalFluid("krypton"), chemicalFluid("xenon"), chemicalFluid("radon"),
			chemicalFluid("liquidoxygen"),
			chemicalFluid("deuterium"), chemicalFluid("tritium"), chemicalFluid("helium3"),
			chemicalFluid("lithium6_molten"), chemicalFluid("beryllium7_molten"), chemicalFluid("beryllium8_molten"),
			chemicalFluid("boron11_molten"), chemicalFluid("carbon13_molten"), chemicalFluid("ancientdebris_molten"),
			chemicalFluid("carbon_molten"), chemicalFluid("lithium_molten"), chemicalFluid("tungsten_molten"),
			chemicalFluid("adamantium_molten"), // task p31-fusion — the fusion-row closure quartet
			chemicalFluid("redstone_molten"), // task p32-qu-scanner-replicator — the replicator redstone carrier (:194)
			chemicalFluid("silicon_molten"), chemicalFluid("germanium_molten"), // task p34-machines-bumblelyzer-crucible — the crystallisation molten quintet
			chemicalFluid("redstonealloy_molten"), chemicalFluid("nikolinealloy_molten"), chemicalFluid("alumina_molten"),
			// the Burner Mixer row carriers (task p34-machines-burner-plantalyzer)
			chemicalFluid("tritiatedwater"), chemicalFluid("titaniumtetrachloride"),
			chemicalFluid("sodiumcarbonate_molten"), chemicalFluid("calcite_molten"));

	/**
	 * The source-fluid handle for a chemical-family row name, or null when the name is not a
	 * CHEMICAL_SPECS row (the {@link gregtech6.fluid.FluidBridge} seam, task
	 * p32-qu-scanner-replicator: the "redstone" entry resolves here — the bridge map stays
	 * material-name keyed while the registration stays table-driven).
	 */
	//? if forge {
	public static RegistryObject<? extends net.minecraft.world.level.material.Fluid> chemicalSource(String aName) {
		return SOURCE_SEAM.get(aName);
	}
	//?} else {
	/*public static net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.world.level.material.Fluid, ? extends net.minecraft.world.level.material.Fluid> chemicalSource(String aName) {
		return SOURCE_SEAM.get(aName);
	}*/
	//?}

	/**
	 * The hot-family row lookup (the {@link #chemicalSpec} shape) — the fuels_hot consumer
	 * side and the offline census both resolve through it.
	 */
	public static ChemicalFluidSpec hotSpec(String aName) {
		for (ChemicalFluidSpec tSpec : HOT_FLUID_SPECS) if (tSpec.name().equals(aName)) return tSpec;
		return null;
	}

	/**
	 * The twelve HOT rows (task p29-w4-hot-lube) — the upstream Loader_Fluids.java:85-99
	 * block verbatim in declaration order. Every row is the {@code FL.create(name, display,
	 * null, 1, 1000, tempK)} six-arg form (FL.java:1089: state = STATE_LIQUID, the 1000 is
	 * the amount-per-unit NOT a density — the material is null so the :1128 density formula
	 * never fires and the vanilla 1000 default rides): ic2coolant (:85) is the four-arg form
	 * (FL.java:1088 → 300 K), the eleven others carry their literal K (:86-:99), and
	 * ic2pahoehoelava (:99) chains {@code .setLuminosity(10).setDensity(50000)
	 * .setViscosity(250000)} over the liquid carrier. Tints are PORT-OWNED DECLARED VALUES
	 * (the oil-family precedent — the materials are null so no MT RGBa anchor exists):
	 * the two molten rows anchor the parent material RGBa (MT.Sn 220,220,220 / MT.LiCl
	 * 222,222,250), the three hot waters ride the aqua hot_water hue (the p16 port-owned
	 * 0xFF6E6EDE family, tritiated shifted red), the two hot gases lift the card-① parent
	 * hues toward grey/pale, and the coolant/salt/lava rows are declared.
	 */
	public static final List<ChemicalFluidSpec> HOT_FLUID_SPECS = List.of(
		new ChemicalFluidSpec("ic2coolant"       , "Industrial Coolant"          ,  300,  1000, 1000, 0xFF3EC8DC, false,  0), // :85 the four-arg create (300 K liquid); tint declared
		new ChemicalFluidSpec("ic2hotcoolant"    , "Industrial Heatant"          , 1200,  1000, 1000, 0xFFB44A28, false,  0), // :86
		new ChemicalFluidSpec("hotmoltensodium"  , "Hot Molten Sodium"           , 1100,  1000, 1000, 0xFFF5F5A0, false,  0), // :87 — the sodium glow (declared)
		new ChemicalFluidSpec("hotmoltentin"     , "Hot Molten Tin"              , 2800,  1000, 1000, 0xFFDCDCDC, false,  0), // :88 — the MT.Sn RGBa
		new ChemicalFluidSpec("hotmoltenlicl"    , "Hot Molten Lithium Chloride" , 1600,  1000, 1000, 0xFFDEDEFA, false,  0), // :89 — the MT.LiCl RGBa
		new ChemicalFluidSpec("hotheavywater"    , "Hot Heavy Water"             ,  600,  1000, 1000, 0xFF6E6EDE, false,  0), // :91 — the aqua hot_water hue family
		new ChemicalFluidSpec("hotsemiheavywater", "Hot Semiheavy Water"         ,  550,  1000, 1000, 0xFF7E7ED2, false,  0), // :92
		new ChemicalFluidSpec("hottritiatedwater", "Hot Tritiated Water"         ,  650,  1000, 1000, 0xFFDE6E6E, false,  0), // :93 — the tritium red shift
		new ChemicalFluidSpec("hotcarbondioxide" , "Hot Carbon Dioxide"          ,  950,  1000, 1000, 0xFF646464, false,  0), // :95 — the CO2 grey, heat-lifted
		new ChemicalFluidSpec("hothelium"        , "Hot Helium"                  , 1150,  1000, 1000, 0xFFF5F5C0, false,  0), // :96 — the He pale yellow, heat-lifted
		new ChemicalFluidSpec("thoriumsalt"      , "Molten Thorium Salt"         ,  600,  1000, 1000, 0xFF96C832, false,  0), // :97 — the fluoride-salt green (declared)
		new ChemicalFluidSpec("ic2pahoehoelava"  , "Pahoehoe Lava"               , 1200, 50000, 250000, 0xFFC83C0A, false, 10)); // :99 lum10/dens50000/visc250000 verbatim

	/** The closure-carrier row lookup (the {@link #hotSpec} shape). */
	public static ChemicalFluidSpec closureSpec(String aName) {
		for (ChemicalFluidSpec tSpec : CLOSURE_FLUID_SPECS) if (tSpec.name().equals(aName)) return tSpec;
		return null;
	}

	/**
	 * The seven CLOSURE carrier rows (task p29-w4-hot-lube spec ②) — the fluids the
	 * Loader_Fuels.java:191-211 FM.Hot rows reference that no earlier family registered;
	 * without them the hot rows would pour into UNRESOLVED ids (the dead-row class the
	 * card was cut to prevent). One row per referenced carrier, the upstream names where
	 * they are clean tokens and the {@code iron_molten} port convention for the molten
	 * trio ({@code molten.<mat>} upstream):
	 * <ul>
	 * <li><b>blaze</b> — the :191 FUEL row {@code FL.Blaze.make(1)}; Loader_Fluids.java:195
	 *     {@code FL.create("blaze", "Blazing Goo", MT.Blaze, 1, 9*L, 4000).setLuminosity(15)}
	 *     (the {@code 9*L} is the amount-per-unit; the density rides the :1128 formula over
	 *     MT.Blaze's default 1.0 g/cm³ → 1000, the 4000 K is the row temp, lum 15 verbatim,
	 *     tint the MT.Blaze RGBa 255,200,0, MT.java:1275);</li>
	 * <li><b>sodium_molten / tin_molten / lithium_chloride_molten</b> — the :204/:205/:211
	 *     output carriers {@code FL.amount(MT.Na.mLiquid, 1)} etc. The MOLTEN-flag walk
	 *     (FL.java:1077 createMolten: Na rides the alkali helper's MOLTEN grant, MT.java:354;
	 *     Sn carries MOLTEN explicitly :439; LiCl :1121): id = molten.&lt;mat&gt; upstream →
	 *     the gt6 iron_molten convention here, temperature = the material melting-point rule
	 *     (:1077: Na 370 / Sn 505 / LiCl 880, MT.java:399/:439/:1121), density = the :1128
	 *     formula over the material g/cm³ (Na 0.971 → 971; Sn 7.287 → 7287; LiCl rides the
	 *     uumMcfg molecule sum Li 0.534 + Cl 0.003214 → 537, MT.java:953/:993/:1121),
	 *     luminosity 10 = the createMolten literal, viscosity 1000 = STATE_LIQUID; tints are
	 *     the materials' RGBa (Na 0,0,150 / Sn 220,220,220 / LiCl 222,222,250);</li>
	 * <li><b>heavywater / semiheavywater / tritiatedwater</b> — the :206/:207/:208 output
	 *     carriers {@code MT.D2O.mLiquid} etc. The LIQUID-flag walk (FL.java:1072
	 *     createLiquid over the lquddcmp materials, MT.java:1008-1010): the bare
	 *     mNameInternal ids upstream (heavywater/semiheavywater/tritiatedwater — the same
	 *     roots the :91-93 hot rows prefix with "hot"), temperature 300 K = the :1072 rule
	 *     (melting C+4/C+2/C+7 &lt; 300 → min(300, boiling − 1)), density = the :1128 formula
	 *     over the materials' setDensity g/cm³ (D2O 1.1056 → 1105, HDO 1.0540 → 1054, T2O
	 *     1.2112 → 1211); tints the materials' RGBa (255,255,100 / 200,200,155 /
	 *     255,100,100).</li>
	 * </ul>
	 */
	public static final List<ChemicalFluidSpec> CLOSURE_FLUID_SPECS = List.of(
		new ChemicalFluidSpec("blaze"      , "Blazing Goo"             , 4000, 1000, 1000, 0xFFFFC800, false, 15), // :195 — the FM.Hot :191 fuel
		new ChemicalFluidSpec("sodium_molten", "Molten Sodium"         ,  370,  971, 1000, 0xFF000096, false, 10), // :204 — createMolten MT.Na
		new ChemicalFluidSpec("tin_molten" , "Molten Tin"             ,  505, 7287, 1000, 0xFFDCDCDC, false, 10), // :205 — createMolten MT.Sn
		new ChemicalFluidSpec("lithium_chloride_molten", "Molten Lithium Chloride", 880, 537, 1000, 0xFFDEDEFA, false, 10), // :211 — createMolten MT.LiCl
		new ChemicalFluidSpec("heavywater"     , "Heavy Water"       ,  300, 1105, 1000, 0xFFFFFF64, false,  0), // :206 — createLiquid MT.D2O
		new ChemicalFluidSpec("semiheavywater" , "Semiheavy Water"   ,  300, 1054, 1000, 0xFFC8C89B, false,  0), // :207 — createLiquid MT.HDO
		new ChemicalFluidSpec("tritiatedwater" , "Tritiated Water"   ,  300, 1211, 1000, 0xFFFF6464, false,  0)); // :208 — createLiquid MT.T2O

	/**
	 * The LUBRICANT row (task p29-w4-hot-lube spec ④, the F-2 single-fluid batch) —
	 * Loader_Fluids.java:617 {@code FL.create("lubricant", "Lubricant", MT.Lubricant, 1)}
	 * (the four-arg form → 300 K STATE_LIQUID carrier; density = the :1128 formula over
	 * MT.Lubricant's default 1.0 g/cm³ → 1000, MT.java:2081 no uumMcfg/no setDensity), the
	 * Diesel Engine crafting 'L' ingredient face (Loader:722-729 {@code OD.itemLubricant};
	 * the fluid's other consumer faces ride the distillation-tower lube product slots
	 * :352-:360 and the FoodStatDrink potion form is a declared cut — no drink seam).
	 */
	public static final List<ChemicalFluidSpec> LUBRICANT_FLUID_SPECS = List.of(
		new ChemicalFluidSpec("lubricant", "Lubricant", 300, 1000, 1000, 0xFFFFC400, false, 0)); // :617 — MT.Lubricant 255,196,0

	/** The lubricant row lookup (the {@link #hotSpec} shape) — the single-row family. */
	public static ChemicalFluidSpec lubricantSpec(String aName) {
		for (ChemicalFluidSpec tSpec : LUBRICANT_FLUID_SPECS) if (tSpec.name().equals(aName)) return tSpec;
		return null;
	}

	/**
	 * The honey family (task p31-bees-lv1) — the bee domain's own drinks, the Bumblelyzer
	 * accept set upstream (FluidsGT.HONEY + Honeydew). Four fluid-only rows riding the
	 * {@link ChemicalFluidSpec} shape (the hot/closure/lubricant append form — no block, no
	 * bucket, the fluid-only declaration). Port ids are the FL shorthand names snake-cased,
	 * NOT the 1.7.10 literals where the two differ (the water_geothermal/mnwtr aqua-card
	 * precedent: {@code royal_jelly} over "royaljelly", {@code ambrosia} over "potion.ambrosia"
	 * — FL.java:143/:144 shorthands). Every value census-anchored:
	 * <ul>
	 * <li><b>honey</b> — Loader_Fluids.java:572 {@code FL.create("honey", ..., MT.Honey, 1,
	 *     1000, 300)} — 300 K, STATE_LIQUID carriers (density 1000 = the :1130 formula over
	 *     Honey's 1.0 g/cm³ default, viscosity 1000), tint = the MT.Honey RGBa 250,200,0
	 *     (MT.java:1352). The upstream local's "(Biomes o'Plenty, Erebus)" parenthetical is a
	 *     1.7.10 multi-mod disambiguator — the port's single honey row trims it (declared).</li>
	 * <li><b>honeydew</b> — :575 {@code FL.create("honeydew", "Honeydew", MT.Honeydew, 1,
	 *     1000, 300)} — 300 K, carriers over Honeydew's 1.0 default → 1000, tint the
	 *     MT.Honeydew RGBa 210,100,0 (MT.java:1353).</li>
	 * <li><b>royal_jelly</b> — :577 {@code FL.create("royaljelly", "Royal Jelly", null, 1,
	 *     1000, 275)} — the 275 K literal, material-null so density/viscosity are the honest
	 *     FluidType defaults (the water_boiling precedent); tint is a PORT-OWNED DECLARED
	 *     pale-royal gold (no upstream material colour to borrow).</li>
	 * <li><b>ambrosia</b> — :576 {@code FL.create("potion.ambrosia", "Ambrosia", null, 1,
	 *     1000, 275)} — the 275 K literal, honest defaults, port-owned amber tint (the
	 *     royal_jelly null-material arm).</li>
	 * </ul>
	 * All four sit far under the wood-barrel 340 K ceiling (GTBarrelCommand.WOOD_MELTING_POINT).
	 */
	public static final List<ChemicalFluidSpec> HONEY_FLUID_SPECS = List.of(
		new ChemicalFluidSpec("honey"      , "Honey"      , 300, 1000, 1000, 0xFFFAC800, false, 0), // :572 — MT.Honey 250,200,0 (MT.java:1352)
		new ChemicalFluidSpec("honeydew"   , "Honeydew"   , 300, 1000, 1000, 0xFFD26400, false, 0), // :575 — MT.Honeydew 210,100,0 (MT.java:1353)
		new ChemicalFluidSpec("royal_jelly", "Royal Jelly", 275, 1000, 1000, 0xFFF0D890, false, 0), // :577 — the 275 K literal; tint declared
		new ChemicalFluidSpec("ambrosia"   , "Ambrosia"   , 275, 1000, 1000, 0xFFE0A850, false, 0));// :576 "potion.ambrosia" — the 275 K literal; tint declared

	/** The honey row for a gt6 id path, or null (the {@link #lubricantSpec} lookup shape). */
	public static ChemicalFluidSpec honeySpec(String aName) {
		for (ChemicalFluidSpec tSpec : HONEY_FLUID_SPECS) if (tSpec.name().equals(aName)) return tSpec;
		return null;
	}

	/**
	 * The bee-row dependency fluids (task p31-bees-lv1) — the SEVEN further GT6-native
	 * FL.create rows the 20 upstream comb centrifuge rows pour as outputs
	 * (MultiItemFood.java:251-270). The card SPEC's "SQUEEZER/CENTRIFUGE 各 20 行 loaded 0
	 * skipped" acceptance needs every output fluid live, and these seven were absent — the
	 * coordinator-approved deviation registers them here as fluid-only rows (no block, no
	 * bucket, the honey-family append form). Every value census-anchored:
	 * <ul>
	 * <li><b>dragon_breath</b> — Loader_Fluids.java:49 {@code FL.create("dragonbreath",
	 *     "Dragon's Breath", null, 2, 1000, 300).setDensity(100).setLuminosity(5)} — the
	 *     aState=2 STATE_GASEOUS row (FL.java:1105: viscosity 200 + gaseous; the explicit
	 *     {@code setDensity(100)} literal overrides the state's −100 density carrier, the
	 *     {@code setLuminosity(5)} literal rides the same form), 300 K; tint a port-owned
	 *     dragon purple (no upstream texture/colour). Display = the vanilla item name
	 *     verbatim. The propane :45 row is the same state-2 form in the chemical family.</li>
	 * <li><b>concrete</b> — :196 {@code FL.create("concrete", "Wet Concrete", MT.Concrete,
	 *     1, L, 300)} — 300 K, density = the :1130 formula over Concrete's 1.0 g/cm³ default
	 *     → 1000 (the propylene/ethylene arm), tint the MT.Concrete RGBa 100,100,100
	 *     (MT.java:1632).</li>
	 * <li><b>chocolate_molten</b> — :201 {@code FL.createMolten(MT.Chocolate, ..., SIMPLE)}
	 *     — upstream id "chocolate.molten" (the "iron.molten" → {@code iron_molten}
	 *     dot-to-underscore precedent); temperature = the molten melting-point rule over
	 *     Chocolate's {@code .heat(C+40, 400)} → 313 K (the iron_molten 1811 K anchor form),
	 *     density 1000 over the 1.0 default, tint the MT.Chocolate RGBa 100,50,0
	 *     (MT.java:1336). ABOVE the 340 K wood-barrel ceiling is false (313 &lt; 340) — wood
	 *     barrels carry it.</li>
	 * <li><b>ice</b> — :364 {@code FL.create("ice", "Near Frozen Water", MT.Ice, 1, 1000,
	 *     C)} — the 273 K literal, density = the MT.Ice {@code setDensity(..., 1.0)} explicit
	 *     (MT.java:1013) → 1000, tint the MT.Ice RGBa 200,200,255.</li>
	 * <li><b>soup_mushroom</b> — :627 {@code FL.create("mushroomsoup", "Mushroom Stew",
	 *     null, 1, 1000, 300, ...)} — 300 K, honest defaults (material-null); the :627
	 *     mushroom-stew bottle/bowl container face stays POOLED (the MultiItemBottles
	 *     domain); tint a port-owned stew brown. Port name = the FL shorthand snake
	 *     (Soup_Mushroom, FL.java:268) over the "mushroomsoup" literal (the aqua precedent).</li>
	 * <li><b>latex</b> — :198 {@code FL.create("latex", "Latex", MT.Latex, 1, L,
	 *     DEF_ENV_TEMP)} — 300 K, density over Latex's 1.0 default → 1000, tint the
	 *     MT.Latex RGBa 250,250,250 (MT.java:1225). The :197 "molten.latex" alias row POOLS
	 *     (one latex fluid in the port, the alias is the same body upstream).</li>
	 * <li><b>potion_harm_1</b> — FL.java:476 {@code Potion_Harm_1("potion.damage", ...)} —
	 *     the instant-damage potion carrier the Military comb row pours (50 L); upstream it
	 *     is born inside the potion-fluid system, so the port registers the plain water-based
	 *     carrier (300 K, honest defaults, the chlorine standalone-row precedent — a
	 *     material/potion bridge the port has no face for). Tint a port-owned harming red;
	 *     port name = the FL shorthand snake over the "potion.damage" literal.</li>
	 * </ul>
	 */
	public static final List<ChemicalFluidSpec> BEE_ROW_FLUID_SPECS = List.of(
		new ChemicalFluidSpec("dragon_breath"   , "Dragon's Breath"    , 300,  100,  200, 0xFFC864C8, true , 5), // :49 — aState=2 STATE_GASEOUS (viscosity 200, FL.java:1105) + the setDensity(100)/setLuminosity(5) literals
		new ChemicalFluidSpec("concrete"        , "Wet Concrete"       , 300, 1000, 1000, 0xFF646464, false, 0), // :196 — MT.Concrete 100,100,100 (MT.java:1632)
		new ChemicalFluidSpec("chocolate_molten", "Molten Chocolate"   , 313, 1000, 1000, 0xFF643200, false, 0), // :201 — the .heat(C+40) melting rule (MT.java:1336)
		new ChemicalFluidSpec("ice"             , "Near Frozen Water"  , 273, 1000, 1000, 0xFFC8C8FF, false, 0), // :364 — the C literal; MT.Ice setDensity 1.0 (MT.java:1013)
		new ChemicalFluidSpec("soup_mushroom"   , "Mushroom Stew"      , 300, 1000, 1000, 0xFF96784C, false, 0), // :627 "mushroomsoup" — honest defaults; tint declared
		new ChemicalFluidSpec("latex"           , "Latex"              , 300, 1000, 1000, 0xFFFAFAFA, false, 0), // :198 — MT.Latex 250,250,250 (MT.java:1225)
		new ChemicalFluidSpec("potion_harm_1"   , "Potion of Harming"  , 300, 1000, 1000, 0xFFB03248, false, 0));// FL.java:476 "potion.damage" — the water-based carrier; tint declared

	/** The bee-row dependency fluid for a gt6 id path, or null (the {@link #honeySpec} lookup shape). */
	public static ChemicalFluidSpec beeRowSpec(String aName) {
		for (ChemicalFluidSpec tSpec : BEE_ROW_FLUID_SPECS) if (tSpec.name().equals(aName)) return tSpec;
		return null;
	}

	/** The live registrations of the two bee families — one per spec row, in declaration order (the {@link #HOT_FLUIDS} shape). */
	public static final List<ChemicalFluid> HONEY_FLUIDS = HONEY_FLUID_SPECS.stream().map(s -> specFluid(s, "honey fluid")).toList();
	public static final List<ChemicalFluid> BEE_ROW_FLUIDS = BEE_ROW_FLUID_SPECS.stream().map(s -> specFluid(s, "bee-row fluid")).toList();

	/** The live family row of a honey-family gt6 id, or null (the recipe-provider fluid seam). */
	public static ChemicalFluid honeyFluid(String aName) {
		for (ChemicalFluid tFamily : HONEY_FLUIDS) if (tFamily.spec.name().equals(aName)) return tFamily;
		return null;
	}

	/** The live family row of a bee-row-dependency gt6 id, or null (the recipe-provider fluid seam). */
	public static ChemicalFluid beeRowFluid(String aName) {
		for (ChemicalFluid tFamily : BEE_ROW_FLUIDS) if (tFamily.spec.name().equals(aName)) return tFamily;
		return null;
	}

	/**
	 * The live SOURCE fluid of ANY spec-based gt6 id — the chemical/hot/closure/lubricant/
	 * honey/bee-row walk plus the aqua/simple-liquid/food walk — or null. The
	 * pre-existing-carrier lookup seam of the bee recipe provider (task p31-bees-lv1);
	 * call at pour time only (the registries are live).
	 */
	@Nullable
	public static Fluid liveFluidSource(String aName) {
		Fluid tFluid = specSourceOrNull(aName, CHEMICALS);
		if (tFluid == null) tFluid = specSourceOrNull(aName, HOT_FLUIDS);
		if (tFluid == null) tFluid = specSourceOrNull(aName, CLOSURE_FLUIDS);
		if (tFluid == null) tFluid = specSourceOrNull(aName, LUBRICANT_FLUIDS);
		if (tFluid == null) tFluid = specSourceOrNull(aName, HONEY_FLUIDS);
		if (tFluid == null) tFluid = specSourceOrNull(aName, BEE_ROW_FLUIDS);
		if (tFluid == null) tFluid = aquaSourceOrNull(aName, aquaFluids());
		if (tFluid == null) tFluid = aquaSourceOrNull(aName, simpleLiquids());
		if (tFluid == null) tFluid = aquaSourceOrNull(aName, foodFluids());
		return tFluid;
	}

	/** The ChemicalFluid-list arm of {@link #liveFluidSource}. */
	private static Fluid specSourceOrNull(String aName, List<ChemicalFluid> aList) {
		for (ChemicalFluid tFamily : aList) if (tFamily.spec.name().equals(aName)) return tFamily.source.get();
		return null;
	}

	/** The AquaFluid-list arm of {@link #liveFluidSource} (structurally identical wrapper). */
	private static Fluid aquaSourceOrNull(String aName, List<AquaFluid> aList) {
		for (AquaFluid tFamily : aList) if (tFamily.spec.name().equals(aName)) return tFamily.source.get();
		return null;
	}

	/** The live registrations of the three families — one per spec row, in declaration order (the {@link #CHEMICALS} shape). */
	public static final List<ChemicalFluid> HOT_FLUIDS = HOT_FLUID_SPECS.stream().map(s -> specFluid(s, "hot fluid")).toList();
	public static final List<ChemicalFluid> CLOSURE_FLUIDS = CLOSURE_FLUID_SPECS.stream().map(s -> specFluid(s, "closure carrier fluid")).toList();
	public static final List<ChemicalFluid> LUBRICANT_FLUIDS = LUBRICANT_FLUID_SPECS.stream().map(s -> specFluid(s, "lubricant fluid")).toList();

	/**
	 * The QU-matter row lookup (the {@link #hotSpec} shape) — the matter/ender family of
	 * task p31-qu-a-foundation.
	 */
	public static ChemicalFluidSpec quSpec(String aName) {
		for (ChemicalFluidSpec tSpec : QU_FLUID_SPECS) if (tSpec.name().equals(aName)) return tSpec;
		return null;
	}

	/**
	 * The three QU-matter rows (task p31-qu-a-foundation) — the fluid foundation of the
	 * Quantum Energy domain, declaration order = the upstream block order:
	 * <ul>
	 * <li><b>chargedmatter / neutralmatter</b> — Loader_Fluids.java:70/:71
	 *     {@code FL.create(name, display, null, 1, 1, 1).setDensity(-5000).setLuminosity(15)}
	 *     (the 6-arg form, FL.java:1089: STATE_LIQUID viscosity 1000; the literal 1 K
	 *     temperature and the amount-per-unit 1 — the GT6 UU semantics: 1 mB = 1 proton /
	 *     neutron, the unit the element-disintegration rows of Loader_Recipes_Other.java:
	 *     971-987 pour in — DENSITY −5000 and LUMINOSITY 15 verbatim). The consumption
	 *     side is RM.Massfab (card C), the replication side RM.Replicator — the fluids
	 *     land FIRST so no later row ever references an unregistered id (the dead-row
	 *     class the closure-carrier card was cut to prevent). Tints are the average
	 *     colours of the upstream fluid PNGs over their opaque pixels
	 *     (assets/gregtech/textures/blocks/fluids/{chargedmatter,neutralmatter}.png —
	 *     deep violet / olive, the sampled provenance in place of a material RGBa the
	 *     material-null rows do not have).</li>
	 * <li><b>enderpearl_molten</b> — the GT6-owned FL.Ender, Loader_Fluids.java:193
	 *     {@code FL.create("molten.enderpearl", "Molten Enderpearls", MT.EnderPearl, 1, L,
	 *     2723).setLuminosity(5)}: STATE_LIQUID carriers, temperature 2723 K = the
	 *     MT.EnderPearl melting point verbatim (MT.java:1499 {@code heat(2723, 3785)}),
	 *     density = the :1128-1130 formula over the material's default 1.0 g/cm³ → 1000,
	 *     luminosity 5 verbatim. The id is the iron_molten port convention for the
	 *     upstream {@code molten.<mat>} literal (the CLOSURE_FLUID_SPECS ruling). The
	 *     consumers this card ships: the replicator smoke row (:929 verbatim, 144 mB = one
	 *     L-unit → one ender pearl) and the Massfab Ender rows when card C pours them
	 *     (:915-928). Tint = the sampled average of molten.enderpearl.png.</li>
	 * </ul>
	 *
	 * <p><b>The Ender_TE conditional mount (declared, NOT registered)</b>: upstream
	 * FL.Ender_TE ("ender", FL.java:453, 250-per-unit) is an EXTERNAL Thermal Expansion
	 * fluid GT6 only attaches to when the mod provides it — the MT.EnderPearl.liquid
	 * binding at Loader_Fluids.java:140-144 and the tag(1) recipe rows
	 * (Loader_Recipes_Other.java:897-914) are all gated on {@code FL.Ender_TE.exists()}.
	 * The port has no TE and registers no "ender" alias: the two concentration domains
	 * must never collapse into one fluid (144 vs 250 mB per unit are different carriers),
	 * so the mount condition stays FALSE — the Ender_TE rows ride unmounted exactly as
	 * upstream-without-TE, and the condition is the seam a future compat card would flip.
	 */
	public static final List<ChemicalFluidSpec> QU_FLUID_SPECS = List.of(
		new ChemicalFluidSpec("chargedmatter"   , "Charged Matter"    ,    1, -5000, 1000, 0xFF391889, false, 15), // :70 — 1 K / dens −5000 / lum 15 verbatim; 1 mB = 1 proton
		new ChemicalFluidSpec("neutralmatter"   , "Neutral Matter"    ,    1, -5000, 1000, 0xFF7B6D14, false, 15), // :71 — same carriers; 1 mB = 1 neutron
		new ChemicalFluidSpec("enderpearl_molten", "Molten Enderpearls", 2723, 1000, 1000, 0xFF002F23, false,  5)); // :193 — the FL.Ender row, MT.EnderPearl heat(2723); lum 5

	/** The live registrations of the QU-matter family — one per spec row, in declaration order (the {@link #HOT_FLUIDS} shape). */
	public static final List<ChemicalFluid> QU_FLUIDS = QU_FLUID_SPECS.stream().map(s -> specFluid(s, "qu fluid")).toList();

	/**
	 * The POWER_CONDUCTING seeds of the hot family — the upstream FL.java:89-102 enum block
	 * verbatim, NINE rows (the review-round correction: the first cut read only the four
	 * Hot_Molten and Coolant rows and missed the enum tail :97-102): ic2hotcoolant (:90),
	 * hotmoltensodium (:95), hotmoltentin (:96), hotheavywater (:97), hotsemiheavywater
	 * (:98), hottritiatedwater (:99), hotmoltenlicl (:100), hotcarbondioxide (:101 — the
	 * GAS-flag row) and hothelium (:102 — the GAS-flag row). The UNSEEDED members of the
	 * same block stay out verbatim: ic2coolant (:89 SIMPLE, LIQUID), thoriumsalt (:93
	 * LIQUID) and ic2pahoehoelava (:105 SIMPLE, LIQUID). The pipe :184 void gate and the
	 * :250 item-fill gate are the live consumers (GTFluidLists.POWER_CONDUCTING — a
	 * POWER_CONDUCTING fluid cannot sit in ANY barrel, the tick voids it). The call lives
	 * here because the rows live here (GTFluidLists.java stays untouched).
	 */
	static {
		GTFluidLists.register("ic2hotcoolant"    , GTFluidLists.POWER_CONDUCTING);
		GTFluidLists.register("hotmoltensodium"  , GTFluidLists.POWER_CONDUCTING);
		GTFluidLists.register("hotmoltentin"     , GTFluidLists.POWER_CONDUCTING);
		GTFluidLists.register("hotheavywater"    , GTFluidLists.POWER_CONDUCTING);
		GTFluidLists.register("hotsemiheavywater", GTFluidLists.POWER_CONDUCTING);
		GTFluidLists.register("hottritiatedwater", GTFluidLists.POWER_CONDUCTING);
		GTFluidLists.register("hotmoltenlicl"    , GTFluidLists.POWER_CONDUCTING);
		GTFluidLists.register("hotcarbondioxide" , GTFluidLists.POWER_CONDUCTING);
		GTFluidLists.register("hothelium"        , GTFluidLists.POWER_CONDUCTING);
	}

	// ------------------------------------------------------------------
	// The worldgen block face (task p31-fluid-spring spec ①). Upstream the
	// bedrock-spring lake bodies are BlocksGT.OilExtraHeavy/OilHeavy/OilMedium/
	// OilLight/GasNatural/WaterGeothermal (Loader_Worldgen.java:782-788) plus the
	// vanilla Blocks.lava row (:788), written by WorldgenFluidSpring.generate (:75).
	// natural_gas carries its block since task p5; vanilla lava needs nothing; the
	// five ids below register the LiquidBlock leg and ride the SHARED Properties
	// builders through withWorldgenBlock — every other fluid-only row of the
	// chemical/aqua bodies stays untouched.
	// ------------------------------------------------------------------

	/**
	 * The five fluid ids this card gives a LiquidBlock to (the fourth being the
	 * {@code natural_gas} block face that already exists, {@link #NATURAL_GAS_BLOCK}).
	 *
	 * <p><b>THE F1 RULING REVISION, EXPLICIT (the card spec's no-silent-deviation
	 * clause):</b> decisions.p29-w4-split-rulings card ① (p29-w4-f1-chemicals) ruled the
	 * chemical batch "无桶无块" (fluid-only). THIS card REVISES THE BLOCK FACE of that
	 * ruling for exactly the four oil rows of {@link #CHEMICAL_SPECS} and the one aqua row
	 * {@code water_geothermal} of {@link #AQUA_SPECS}: the bedrock-spring lake needs a
	 * placeable fluid block per row (upstream WorldgenFluidSpring writes mBlock,
	 * WorldgenFluidSpring.java:75, the row table :782-788). THE BUCKET FACE IS UNCHANGED —
	 * zero bucket items, the R6 census (GTFluids registers no items at all) stands. The
	 * revision is the SPEC-declared deliverable, not a silent drift: the block ids are the
	 * fluid id + the {@code _block} suffix (the oil/natural_gas/dye-chemical convention).
	 * Block properties ride the established liquid ramp
	 * ({@code noCollission().strength(100.0F).noLootTable()}, the iron_molten block form).
	 *
	 * <p>Mechanics: the static block below registers the five LiquidBlocks into
	 * {@link #BLOCK_SEAM} at class-init (AFTER the seam maps' textual position, long before
	 * the registry event the DeferredRegister lambdas fire on), and the shared
	 * {@link #chemicalProperties}/{@link #aquaProperties} builders attach the block leg
	 * through {@link #withWorldgenBlock} only where the seam carries the name. KJS surface:
	 * REGISTRATION face only (the existing fluid-block seam; the lake placement itself is
	 * the worldgen JSON domain of the {@code gt6:fluid_springs} feature).
	 */
	public static final List<String> SPRING_BLOCK_IDS = List.of(
			"liquid_extra_heavy_oil", "liquid_heavy_oil", "liquid_medium_oil", "liquid_light_oil",
			"water_geothermal");

	/** The gt6 BLOCK id of a worldgen spring fluid — the full {@code gt6:} id, the fluid id + {@code _block} (natural_gas included, its block the p5 face). */
	public static String springBlockId(String aFluidName) {
		return "gt6:" + ("natural_gas".equals(aFluidName) ? "natural_gas_block" : aFluidName + "_block");
	}

	/** The block-leg attach: the Properties gain the LiquidBlock only where the seam carries the name, the fluid-only rows stay untouched. */
	private static ForgeFlowingFluid.Properties withWorldgenBlock(RegistryObject<FluidType> aType,
			ForgeFlowingFluid.Properties aProps, String aName) {
		RegistryObject<LiquidBlock> tBlock = BLOCK_SEAM.get(aName);
		return tBlock == null ? aProps : aProps.block(tBlock);
	}

	static {
		for (String tName : SPRING_BLOCK_IDS) {
			//? if forge {
			RegistryObject<LiquidBlock> tBlock = BLOCKS.register(tName + "_block",
					() -> new LiquidBlock(SOURCE_SEAM.get(tName), BlockBehaviour.Properties.of()
							.noCollission().strength(100.0F).noLootTable())); // a liquid: the iron_molten block ramp
			//?} else {
			/*RegistryObject<LiquidBlock> tBlock = BLOCKS.register(tName + "_block",
					() -> new LiquidBlock(SOURCE_SEAM.get(tName).get(), BlockBehaviour.Properties.of()
							.noCollission().strength(100.0F).noLootTable())); // a liquid: the iron_molten block ramp (FLUID before BLOCK, the resolved .get() is live)
			*///?}
			BLOCK_SEAM.put(tName, tBlock);
		}
	}

	private GTFluids() {}

	@SubscribeEvent
	public static void onModConstruct(FMLConstructModEvent aEvent) {
		//? if forge {
		IEventBus tModBus = Mod.EventBusSubscriber.Bus.MOD.bus().get();
		//?} else {
		/*IEventBus tModBus = net.neoforged.fml.ModList.get().getModContainerById("gt6").orElseThrow().getEventBus();
		//21.1: Mod.EventBusSubscriber.Bus died with the annotation rework; a self-contained
		//listener reaches the mod bus through its mod container (javap loader-4.0.44:
		//ModContainer.getEventBus public abstract) — the GT6Mod/GTMenuTypes fork precedent.
		*///?}
		FLUID_TYPES.register(tModBus); // FluidTypeTest.java:169-172 order — types before fluids
		FLUIDS.register(tModBus);
		BLOCKS.register(tModBus);
	}

	/**
	 * Registration smoke evidence (acceptance ④, FluidTypeTest.java:193-197 log shape):
	 * the gt6 molten iron chain and the vanilla water/lava types the W1 pipeline carries.
	 */
	@SubscribeEvent
	public static void onCommonSetup(FMLCommonSetupEvent aEvent) {
		aEvent.enqueueWork(() -> {
			GT6Mod.LOGGER.info("GT6 fluid registered: {} (source) / {} (flowing), FluidType {}, block {}",
					ForgeRegistries.FLUIDS.getKey(IRON_MOLTEN.get()),
					ForgeRegistries.FLUIDS.getKey(IRON_MOLTEN_FLOWING.get()),
					ForgeRegistries.FLUID_TYPES.get().getKey(IRON_MOLTEN_TYPE.get()),
					ForgeRegistries.BLOCKS.getKey(IRON_MOLTEN_BLOCK.get()));
			GT6Mod.LOGGER.info("GT6 fluid registered: {} (source) / {} (flowing), FluidType {} density {} (the p5 lighter carrier)",
					ForgeRegistries.FLUIDS.getKey(NATURAL_GAS.get()),
					ForgeRegistries.FLUIDS.getKey(NATURAL_GAS_FLOWING.get()),
					ForgeRegistries.FLUID_TYPES.get().getKey(NATURAL_GAS_TYPE.get()),
					NATURAL_GAS_TYPE.get().getDensity());
			GT6Mod.LOGGER.info("GT6 fluid registered: {} (source) / {} (flowing), FluidType {} density {} (the coke oven by-product, p6)",
					ForgeRegistries.FLUIDS.getKey(CREOSOTE.get()),
					ForgeRegistries.FLUIDS.getKey(CREOSOTE_FLOWING.get()),
					ForgeRegistries.FLUID_TYPES.get().getKey(CREOSOTE_TYPE.get()),
					CREOSOTE_TYPE.get().getDensity());
			GT6Mod.LOGGER.info("GT6 fluid registered: {} (source) / {} (flowing), FluidType {} density {} (the oil-shale output, p7)",
					ForgeRegistries.FLUIDS.getKey(OIL.get()),
					ForgeRegistries.FLUIDS.getKey(OIL_FLOWING.get()),
					ForgeRegistries.FLUID_TYPES.get().getKey(OIL_TYPE.get()),
					OIL_TYPE.get().getDensity());
			GT6Mod.LOGGER.info("GT6 vanilla fluid types: water {} lava {} (FluidBridge carries them without registration)",
					ForgeRegistries.FLUID_TYPES.get().getKey(ForgeMod.WATER_TYPE.get()),
					ForgeRegistries.FLUID_TYPES.get().getKey(ForgeMod.LAVA_TYPE.get()));
			// keep the pipe registration visible in the same smoke line group
			GT6Mod.LOGGER.info("GT6 fluid pipes registered: {} {}",
					GTFluidPipes.WOOD_FLUID_PIPE_SMALL.getId(), GTFluidPipes.WOOD_FLUID_PIPE_MEDIUM.getId());
			// task p12-engine-fuel-fluids — the engine family, one smoke line per fluid (the
			// live registry keys, mirroring the four per-fluid lines above)
			for (EngineFluid tFamily : engineFluids()) {
				GT6Mod.LOGGER.info("GT6 fluid registered: {} (source) / {} (flowing), FluidType {} K, density {}{}",
						ForgeRegistries.FLUIDS.getKey(tFamily.source.get()),
						ForgeRegistries.FLUIDS.getKey(tFamily.flowing.get()),
						tFamily.spec.temperature(),
						tFamily.spec.density(),
						tFamily.spec.gas() ? " (gaseous)" : "");
			}
			// task p16-aqua-fluids — the aqua family, the same smoke line shape
			for (AquaFluid tFamily : aquaFluids()) {
				GT6Mod.LOGGER.info("GT6 fluid registered: {} (source) / {} (flowing), FluidType {} K, density {} (aqua family, {})",
						ForgeRegistries.FLUIDS.getKey(tFamily.source.get()),
						ForgeRegistries.FLUIDS.getKey(tFamily.flowing.get()),
						tFamily.spec.temperature(),
						tFamily.spec.density(),
						tFamily.spec.displayName());
			}
			// task p19-drying-rows-backfill-2 — the simple-liquid family, the same smoke line shape
			for (AquaFluid tFamily : simpleLiquids()) {
				GT6Mod.LOGGER.info("GT6 fluid registered: {} (source) / {} (flowing), FluidType {} K, density {} (simple liquid, {})",
						ForgeRegistries.FLUIDS.getKey(tFamily.source.get()),
						ForgeRegistries.FLUIDS.getKey(tFamily.flowing.get()),
						tFamily.spec.temperature(),
						tFamily.spec.density(),
						tFamily.spec.displayName());
			}
			// task p21-drying-food-fluids — the food family, the same smoke line shape
			for (AquaFluid tFamily : foodFluids()) {
				GT6Mod.LOGGER.info("GT6 fluid registered: {} (source) / {} (flowing), FluidType {} K, density {} (food family, {})",
						ForgeRegistries.FLUIDS.getKey(tFamily.source.get()),
						ForgeRegistries.FLUIDS.getKey(tFamily.flowing.get()),
						tFamily.spec.temperature(),
						tFamily.spec.density(),
						tFamily.spec.displayName());
			}
			// task p24-dye-chemical-fluids — the dye-chemical family (with-block rows), the same
			// smoke line shape plus the block key and the DYES_INT tint index face
			for (DyeChemicalFluid tFamily : DYE_CHEMICALS) {
				GT6Mod.LOGGER.info("GT6 fluid registered: {} (source) / {} (flowing), FluidType {} K, density {}, block {} (dye-chemical family, {}, dye index {})",
						ForgeRegistries.FLUIDS.getKey(tFamily.source.get()),
						ForgeRegistries.FLUIDS.getKey(tFamily.flowing.get()),
						DYE_CHEMICAL_TEMPERATURE,
						DYE_CHEMICAL_DENSITY,
						ForgeRegistries.BLOCKS.getKey(tFamily.block.get()),
						tFamily.displayName(),
						tFamily.dyeIndex);
			}
			GT6Mod.LOGGER.info("GT6 fluid registered: {} (source) / {} (flowing), FluidType {} K, density {} (chlorine — the p24 carrier, zero consumers until the Canner card)",
					ForgeRegistries.FLUIDS.getKey(CHLORINE.get()),
					ForgeRegistries.FLUIDS.getKey(CHLORINE_FLOWING.get()),
					CHLORINE_TEMPERATURE,
					CHLORINE_DENSITY);
			// task p26-c-foam-fluid-refill — the C-Foam family: the base row + the 32 with-block
			// rows (the dye-chemical smoke shape, one line per family + the base)
			GT6Mod.LOGGER.info("GT6 fluid registered: {} (source) / {} (flowing), FluidType {} K, density {}, block {} (c-foam base — the ic2constructionfoam counterpart, the naming ruling)",
					ForgeRegistries.FLUIDS.getKey(CFOAM.get()),
					ForgeRegistries.FLUIDS.getKey(CFOAM_FLOWING.get()),
					CFOAM_TEMPERATURE,
					CFOAM_DENSITY,
					ForgeRegistries.BLOCKS.getKey(CFOAM_BLOCK.get()));
			for (CFoamFluid tFamily : CFOAMS) {
				GT6Mod.LOGGER.info("GT6 fluid registered: {} (source) / {} (flowing), FluidType {} K, density {}, block {} (c-foam family, {}, dye index {})",
						ForgeRegistries.FLUIDS.getKey(tFamily.source.get()),
						ForgeRegistries.FLUIDS.getKey(tFamily.flowing.get()),
						CFOAM_TEMPERATURE,
						CFOAM_DENSITY,
						ForgeRegistries.BLOCKS.getKey(tFamily.block.get()),
						tFamily.displayName(),
						tFamily.dyeIndex);
			}
			for (CFoamFluid tFamily : CFOAMS_OWNED) {
				GT6Mod.LOGGER.info("GT6 fluid registered: {} (source) / {} (flowing), FluidType {} K, density {}, block {} (c-foam owned family, {}, dye index {})",
						ForgeRegistries.FLUIDS.getKey(tFamily.source.get()),
						ForgeRegistries.FLUIDS.getKey(tFamily.flowing.get()),
						CFOAM_TEMPERATURE,
						CFOAM_DENSITY,
						ForgeRegistries.BLOCKS.getKey(tFamily.block.get()),
						tFamily.displayName(),
						tFamily.dyeIndex);
			}
			// task p29-w4-f1-chemicals — the chemical family, the same smoke line shape
			// (temperature K, density, the gaseous/plasma markers, the lum face on the plasmas)
			for (ChemicalFluid tFamily : CHEMICALS) {
				GT6Mod.LOGGER.info("GT6 fluid registered: {} (source) / {} (flowing), FluidType {} K, density {}{}{} (chemical family, {})",
						ForgeRegistries.FLUIDS.getKey(tFamily.source.get()),
						ForgeRegistries.FLUIDS.getKey(tFamily.flowing.get()),
						tFamily.spec.temperature(),
						tFamily.spec.density(),
						tFamily.spec.gas() ? " (gaseous)" : "",
						tFamily.spec.luminosity() > 0 ? ", lum " + tFamily.spec.luminosity() : "",
						tFamily.spec.displayName());
			}
		});
	}
}
