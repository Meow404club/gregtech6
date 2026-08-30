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
			GT6Mod.LOGGER.info("GT6 vanilla fluid types: water {} lava {} (FluidBridge carries them without registration)",
					ForgeRegistries.FLUID_TYPES.get().getKey(ForgeMod.WATER_TYPE.get()),
					ForgeRegistries.FLUID_TYPES.get().getKey(ForgeMod.LAVA_TYPE.get()));
			// keep the pipe registration visible in the same smoke line group
			GT6Mod.LOGGER.info("GT6 fluid pipes registered: {} {}",
					GTFluidPipes.WOOD_FLUID_PIPE_SMALL.getId(), GTFluidPipes.WOOD_FLUID_PIPE_MEDIUM.getId());
		});
	}
}
