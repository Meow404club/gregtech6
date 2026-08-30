package gregtech6.fluid;

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
		});
	}
}
