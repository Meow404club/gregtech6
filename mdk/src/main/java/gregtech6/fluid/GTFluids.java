package gregtech6.fluid;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

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

	/** One registered aqua family: the declared spec + the three live handles. Fluid-only — no block, no bucket. */
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
	 * The shared registration body for the AquaFluidSpec tables ({@link #AQUA_SPECS} and
	 * {@link #SIMPLE_LIQUID_SPECS}): FluidType carrying the declared
	 * temperature/density/viscosity, Source/Flowing over one shared Properties, NO
	 * LiquidBlock and NO bucket (the fluid-only declaration; the bucket container
	 * behaviour is out of the card scope). Shares the SOURCE_SEAM/FLOWING_SEAM maps —
	 * the Properties are built at registry-event time (see engineFluid).
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
		// NO .block(...) — the fluid-only declaration of this card
		return new ForgeFlowingFluid.Properties(aType, SOURCE_SEAM.get(aSpec.name()), FLOWING_SEAM.get(aSpec.name()));
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
		new ChemicalFluidSpec("liquidoxygen"    , "Liquid Oxygen"    ,    85,      1, 1000, 0xFF0064C8, false,  0)); // the :1130 formula over O's 0.001429 g/cm³; tint = the O RGBa

	/** The chemical row for a gt6 id path, or null (the {@link #engineSpec} lookup shape). */
	public static ChemicalFluidSpec chemicalSpec(String aName) {
		for (ChemicalFluidSpec tSpec : CHEMICAL_SPECS) if (tSpec.name().equals(aName)) return tSpec;
		return null;
	}

	/** One registered chemical family: the declared spec + the three live handles. Fluid-only — no block, no bucket. */
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
		ChemicalFluidSpec tSpec = chemicalSpec(aName);
		if (tSpec == null) throw new IllegalArgumentException("no chemical fluid spec: " + aName);
		RegistryObject<FluidType> tType = FLUID_TYPES.register(tSpec.name(), () -> new FluidType(FluidType.Properties.create()
				.descriptionId(tSpec.descriptionId())
				.temperature(tSpec.temperature())
				.density(tSpec.density())
				.viscosity(tSpec.viscosity())
				.lightLevel(tSpec.luminosity())) {
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
				() -> new ForgeFlowingFluid.Source(chemicalProperties(tSpec, tType)));
		RegistryObject<Fluid> tFlowing = FLUIDS.register(tSpec.name() + "_flowing",
				() -> new ForgeFlowingFluid.Flowing(chemicalProperties(tSpec, tType)));
		SOURCE_SEAM.put(tSpec.name(), tSource);
		FLOWING_SEAM.put(tSpec.name(), tFlowing);
		return new ChemicalFluid(tSpec, tType, tSource, tFlowing);
	}

	/** The shared per-family Properties for the chemical rows — registry-event time only (see chemicalFluid). */
	private static ForgeFlowingFluid.Properties chemicalProperties(ChemicalFluidSpec aSpec, RegistryObject<FluidType> aType) {
		// NO .block(...) — the fluid-only declaration of this family
		return new ForgeFlowingFluid.Properties(aType, SOURCE_SEAM.get(aSpec.name()), FLOWING_SEAM.get(aSpec.name()));
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
			chemicalFluid("liquidoxygen"));

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
