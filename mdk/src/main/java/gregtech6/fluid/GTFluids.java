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

	private GTFluids() {}

	@SubscribeEvent
	public static void onModConstruct(FMLConstructModEvent aEvent) {
		IEventBus tModBus = Mod.EventBusSubscriber.Bus.MOD.bus().get();
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
		});
	}
}
