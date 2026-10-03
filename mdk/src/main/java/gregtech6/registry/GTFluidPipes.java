package gregtech6.registry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import gregtech6.block.pipe.GTFluidPipeBlock;
import gregtech6.block.pipe.GTFluidPipeBlockItem;
import gregtech6.tileentity.connectors.GTFluidPipeBlockEntity;
import gregtech6.tooltip.GT6Tooltips;

/**
 * Fluid pipe registration, card-owned (ADR-P3-4): self-contained
 * {@code @EventBusSubscriber(MOD)} DeferredRegisters attached from the construct event —
 * GT6Mod.java / GTModBusListener.java stay untouched (the frozen P2 form is never
 * extended). The family is row-driven (the {@link GTItemPipes} / GT6Boilers.BoilerRow
 * precedent): one upstream {@code MultiTileEntityPipeFluid.addFluidPipes} line per
 * material (Loader_MultiTileEntities.java:1846-1885, 40 lines) expands to SEVEN block
 * rows through the variant table ({@link FluidPipeVariant}, MultiTileEntityPipeFluid
 * .java:92-98 — the capacity multipliers {1, 2, 6, 12, 24, 6, 2}, the tank counts
 * {1, 1, 1, 1, 1, 4, 9}, the diameters PX_P[4/6/8/12/16/16/16] and the stack sizes
 * {64, 64, 32, 16, 16, 16, 16}) = 280 registered rows, the full upstream matrix
 * (task fluid-pipe-matrix, research.boiler-gauges ④a).
 *
 * <p>Per-material behaviour columns ride {@link FluidPipeMaterial} verbatim from the
 * loader lines: the max temperature (explicit 340/340/370/350 for
 * Wood/WoodTreated/Plastic/Rubber, otherwise {@code mMeltingPoint * 1.25} — the 11-arg
 * addFluidPipes overload, MultiTileEntityPipeFluid.java:84), the four proofs
 * (gas/acid/plasma/magic), the contact damage and flammability flags, the recipe flag
 * and the blocking (opaque) flag driving the hardness/resistance NBT pair
 * {@code blocking ? 2.0F : 1.0F} / {@code blocking ? 6.0F : 2.0F} (:92). The row data
 * face only — the BE consumes capacity today; the temperature/proof consumption is the
 * later semantics card (the P26 pipe-chain ruling).
 *
 * <p>Registration order follows the loader line order (material-major, then the
 * addFluidPipes variant order). id = {@code <mat>_fluid_pipe_<size>} — material first
 * (the arch ruling, the wood_fluid_pipe_small order). The pipe BE is shared across all
 * 280 blocks (ADR-P3-1, the GT6 "one TE class, many blocks" multi-mount). Rendering
 * stays the shared cube placeholder (the pipeBlockstate families in GT6BlockStates);
 * connection-aware geometry is the rod-render-pool card.
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GTFluidPipes {

	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "gt6");
	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "gt6");
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gt6");
	public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "gt6");

	/** The display-key namespace of the variant templates ({@code %s} = the material word). */
	public static final String DISPLAY_KEY_PREFIX = "gt6.row.fluid_pipe.display.";

	/**
	 * The material-word key namespace — family-scoped ON PURPOSE: the axle family's
	 * {@code gt6.row.mat.wood_treated} carries the ADJECTIVE 木制 ("Small Wooden Axle"),
	 * this family needs the dump noun 防腐木 (微型防腐木流体管道, tmp/gregtech.lang:11984).
	 */
	public static final String MAT_KEY_PREFIX = "gt6.row.fluid_pipe.mat.";

	/**
	 * The registration block family of the loader line (the aWooden / aUtilWool /
	 * aMachine column, Loader_MultiTileEntities.java:1846-1885) — the sound axis (the
	 * port's declared normalisation layer, the GTItemPipes METAL note).
	 */
	public enum PipeBlockFamily {
		WOODEN(SoundType.WOOD), UTIL_WOOL(SoundType.WOOL), MACHINE(SoundType.METAL);

		public final SoundType sound;

		PipeBlockFamily(SoundType aSound) {
			sound = aSound;
		}
	}

	/**
	 * One pipe material of the matrix — one loader line (Loader_MultiTileEntities.java
	 * :1846-1885, verbatim order). The flags are the addFluidPipes boolean columns
	 * (gas/acid/plasma/magic proofs, contact damage, flammable, recipe, blocking —
	 * MultiTileEntityPipeFluid.java:83/:91); the max temperature is the explicit loader
	 * constant when the line passes one, else the 11-arg overload's
	 * {@code mMeltingPoint * 1.25} (MultiTileEntityPipeFluid.java:84).
	 */
	public record FluidPipeMaterial(String slug, String displayWord, int metaIdBase, long stat,
			boolean gasProof, boolean acidProof, boolean plasmaProof, boolean magicProof,
			boolean contactDamage, boolean flammable, boolean recipe, boolean blocking,
			PipeBlockFamily blockFamily, Long explicitMaxTemp) {

		/** The family-scoped material-word key ({@link #MAT_KEY_PREFIX}). */
		public String unitKey() {
			return MAT_KEY_PREFIX + slug;
		}

		/**
		 * The row's max temperature (NBT_TEMPERATURE, :92-98) — the explicit constant or
		 * the melt-derived overload. Lazy over the ore-dict material (the material
		 * statics boot after this class loads — the GTItemPipes.oreDictMaterial note).
		 */
		public long maxTemperature() {
			return explicitMaxTemp != null ? explicitMaxTemp : (long)(oreDictMaterial().mMeltingPoint * 1.25);
		}

		/**
		 * The row's ore-dict material — the loader line's {@code MT.*} argument verbatim
		 * (:1846-1885). Loud drift on an unknown slug — the table only ever grows with a
		 * loader line in hand (the ItemPipeMaterial truth gate).
		 */
		public gregapi.oredict.OreDictMaterial oreDictMaterial() {
			return switch (slug) {
				case "wood" -> gregapi.data.MT.Wood;
				case "wood_treated" -> gregapi.data.MT.WoodTreated;
				case "iron_wood" -> gregapi.data.MT.IronWood;
				case "plastic" -> gregapi.data.MT.Plastic;
				case "rubber" -> gregapi.data.MT.Rubber;
				case "copper" -> gregapi.data.MT.Cu;
				case "gold" -> gregapi.data.MT.Au;
				case "aluminium" -> gregapi.data.MT.Al;
				case "tin_alloy" -> gregapi.data.MT.TinAlloy;
				case "bronze" -> gregapi.data.MT.Bronze;
				case "invar" -> gregapi.data.MT.Invar;
				case "steel" -> gregapi.data.MT.Steel;
				case "desh" -> gregapi.data.MT.Desh;
				case "chromium" -> gregapi.data.MT.Cr;
				case "hsla" -> gregapi.data.MT.HSLA;
				case "efrine" -> gregapi.data.MT.Efrine;
				case "galvanized_steel" -> gregapi.data.MT.SteelGalvanized;
				case "stainless_steel" -> gregapi.data.MT.StainlessSteel;
				case "tungsten_alloy" -> gregapi.data.MT.TungstenAlloy;
				case "titanium" -> gregapi.data.MT.Ti;
				case "netherite" -> gregapi.data.MT.Netherite;
				case "workers_alloy" -> gregapi.data.MT.DeshAlloy;
				case "tungsten" -> gregapi.data.MT.W;
				case "palladium" -> gregapi.data.MT.Pd;
				case "vanadium_steel" -> gregapi.data.MT.VanadiumSteel;
				case "tungstensteel" -> gregapi.data.MT.TungstenSteel;
				case "tungsten_carbide" -> gregapi.data.MT.TungstenCarbide;
				case "iridium" -> gregapi.data.MT.Ir;
				case "gaia_spirit" -> gregapi.data.MT.GaiaSpirit;
				case "draconium" -> gregapi.data.MT.Draconium;
				case "awakened_draconium" -> gregapi.data.MT.DraconiumAwakened;
				case "infinity" -> gregapi.data.MT.Infinity;
				case "adamantium" -> gregapi.data.MT.Ad;
				case "bedrock_hsla_alloy" -> gregapi.data.MT.Bedrock_HSLA_Alloy;
				case "thaumium" -> gregapi.data.MT.Thaumium;
				case "manasteel" -> gregapi.data.MT.Manasteel;
				case "void_metal" -> gregapi.data.MT.VoidMetal;
				case "terrasteel" -> gregapi.data.MT.Terrasteel;
				case "carbon" -> gregapi.data.MT.C;
				case "tantalum_hafnium_carbide" -> gregapi.data.MT.Ta4HfC5;
				default -> throw new IllegalStateException("no ore-dict material pinned for fluid pipe slug " + slug);
			};
		}

		/**
		 * The mdh-6 driver domain per slug (task mdh-6-family-gate): the atlas PRIMARY modid
		 * when the row hides with its owning mod absent, else {@code null} = GT core /
		 * COMMON_SECONDARY / unattributed = always registers (ADR-MDH1/MDH2). A String because
		 * the registration walk runs at class-init, before {@code MT.init()} fills the material
		 * fields — the GTFluids spec-row column shape, gathered in one switch next to
		 * {@link #oreDictMaterial} (the anchors cite the upstream MT.java lines the atlas rows carry).
		 */
		public String driverDomain() {
			return switch (slug) {
				case "iron_wood" -> gregapi.data.MT.MD.TF.mID; // MT.java:2286 — IronWood TF PRIMARY (atlas, mdh-6)
				case "aluminium" -> gregapi.data.MT.MD.TiC.mID; // MT.java:2350 — Al TiC PRIMARY (atlas, mdh-6)
				case "hsla", "tungsten_alloy", "bedrock_hsla_alloy" -> gregapi.data.MT.MD.RoC.mID; // MT.java:2426/:2429/:2431 — RoC PRIMARY (atlas, mdh-6)
				case "tungsten_carbide" -> gregapi.data.MT.MD.ReC.mID; // MT.java:2412 — ReC PRIMARY (atlas, mdh-6)
				case "workers_alloy" -> gregapi.data.MT.MD.HBM.mID; // MT.java:2404 — DeshAlloy HBM PRIMARY (atlas, mdh-6)
				case "gaia_spirit", "manasteel", "terrasteel" -> gregapi.data.MT.MD.BOTA.mID; // MT.java:2466-2471 — BOTA PRIMARY (atlas, mdh-6)
				case "void_metal" -> gregapi.data.MT.MD.TC.mID; // MT.java:2445 — VoidMetal TC PRIMARY (atlas, mdh-6)
				case "awakened_draconium" -> gregapi.data.MT.MD.DE.mID; // MT.java:2513 — DE PRIMARY (atlas, mdh-6)
				case "infinity" -> gregapi.data.MT.MD.AV.mID; // MT.java:2518 — AV PRIMARY (atlas, mdh-6)
				default -> null; // GT core / COMMON_SECONDARY (thaumium, efrine, desh, draconium) / unattributed (adamantium) — never hides
			};
		}

		/** The registration decision for one row (the mdh-6 family gate; single source for the walk and the tests). */
		public boolean registers() {
			return GT6ModDrivers.isLoaded(driverDomain());
		}
	}

	/**
	 * The seven variants addFluidPipes registers per material (MultiTileEntityPipeFluid
	 * .java:92-98, one row each): capacity {1, 2, 6, 12, 24, 6, 2}× the material stat,
	 * tank counts {1, 1, 1, 1, 1, 4, 9}, diameters PX_P[4/6/8/12/16/16/16], stack sizes
	 * {64, 64, 32, 16, 16, 16, 16} and the {@code aID+n} registration offsets +0..+6.
	 */
	public enum FluidPipeVariant {
		TINY("tiny", 1, 1, 4, 0, 64),
		SMALL("small", 2, 1, 6, 1, 64),
		MEDIUM("medium", 6, 1, 8, 2, 32),
		LARGE("large", 12, 1, 12, 3, 16),
		HUGE("huge", 24, 1, 16, 4, 16),
		QUADRUPLE("quadruple", 6, 4, 16, 5, 16),
		NONUPLE("nonuple", 2, 9, 16, 6, 16);

		/** The path tail ({@code <mat>_fluid_pipe_<suffix>}). */
		public final String suffix;
		/** The capacity multiplier (upstream {@code aStat * m}, :92-98). */
		public final int capMul;
		/** The tank count (NBT_TANK_COUNT, :92-98). */
		public final int tankCount;
		/** The pipe diameter in pixels (NBT_DIAMETER PX_P[d], :92-98). */
		public final int diameterPx;
		/** The {@code aID+n} registration offset (:92-98). */
		public final int metaOffset;
		/** The registration max stack size (the :92-98 aStackSize column). */
		public final int maxStack;

		FluidPipeVariant(String aSuffix, int aCapMul, int aTankCount, int aDiameterPx, int aMetaOffset, int aMaxStack) {
			suffix = aSuffix;
			capMul = aCapMul;
			tankCount = aTankCount;
			diameterPx = aDiameterPx;
			metaOffset = aMetaOffset;
			maxStack = aMaxStack;
		}

		/** The variant display template key ({@code %s} = the material word). */
		public String displayKey() {
			return DISPLAY_KEY_PREFIX + suffix;
		}
	}

	/** One registration row — one block of the family (the ItemPipeRow projection shape). */
	public record FluidPipeRow(FluidPipeMaterial material, FluidPipeVariant variant) {
		/** {@code <mat>_fluid_pipe_<suffix>} — the arch ruling: material first. */
		public String path() {
			return material.slug() + "_fluid_pipe_" + variant.suffix;
		}

		/** Per-tank capacity in Liters (NBT_TANK_CAPACITY = {@code aStat * mul}, :92-98). */
		public long capacity() {
			return material.stat() * variant.capMul;
		}

		/** The upstream metaId ({@code aID + n}, the zero-diff table face). */
		public int metaId() {
			return material.metaIdBase() + variant.metaOffset;
		}

		/** The composed display name — the pure compose seam (the ItemPipeRow displayName shape). */
		public MutableComponent displayName() {
			return Component.translatable(variant.displayKey(), Component.translatable(material.unitKey()));
		}
	}

	// The 40 loader lines, verbatim order (:1846-1885). Columns:
	// slug, displayWord(en getLocal), metaIdBase, stat, gas, acid, plasma, magic,
	// contactDamage, flammable, recipe, blocking, blockFamily, explicit max temp.
	private static final boolean T = true, F = false; // the loader-line flag columns read like the upstream source

	public static final FluidPipeMaterial MAT_WOOD = new FluidPipeMaterial("wood", "Wood", 26000, 50, F, F, F, F, T, T, F, T, PipeBlockFamily.WOODEN, 340L);
	public static final FluidPipeMaterial MAT_WOOD_TREATED = new FluidPipeMaterial("wood_treated", "Treated Wood", 26020, 75, F, F, F, F, T, T, F, T, PipeBlockFamily.WOODEN, 340L);
	public static final FluidPipeMaterial MAT_IRON_WOOD = new FluidPipeMaterial("iron_wood", "Ironwood", 26080, 200, T, F, F, T, F, F, T, T, PipeBlockFamily.WOODEN, null);
	public static final FluidPipeMaterial MAT_PLASTIC = new FluidPipeMaterial("plastic", "Plastic", 26060, 100, T, F, F, F, T, F, F, T, PipeBlockFamily.WOODEN, 370L);
	public static final FluidPipeMaterial MAT_RUBBER = new FluidPipeMaterial("rubber", "Rubber", 26520, 100, T, F, F, F, T, F, F, F, PipeBlockFamily.UTIL_WOOL, 350L);
	public static final FluidPipeMaterial MAT_COPPER = new FluidPipeMaterial("copper", "Copper", 26100, 100, T, F, F, F, T, F, T, T, PipeBlockFamily.MACHINE, null);
	public static final FluidPipeMaterial MAT_GOLD = new FluidPipeMaterial("gold", "Gold", 26680, 100, T, T, F, F, T, F, T, T, PipeBlockFamily.MACHINE, null);
	public static final FluidPipeMaterial MAT_ALUMINIUM = new FluidPipeMaterial("aluminium", "Aluminium", 26340, 100, T, F, F, F, F, F, T, T, PipeBlockFamily.MACHINE, null);
	public static final FluidPipeMaterial MAT_TIN_ALLOY = new FluidPipeMaterial("tin_alloy", "Tin Alloy", 26040, 125, T, F, F, F, T, F, T, T, PipeBlockFamily.MACHINE, null);
	public static final FluidPipeMaterial MAT_BRONZE = new FluidPipeMaterial("bronze", "Bronze", 26120, 150, T, F, F, F, T, F, T, T, PipeBlockFamily.MACHINE, null);
	public static final FluidPipeMaterial MAT_INVAR = new FluidPipeMaterial("invar", "Invar", 26400, 200, T, F, F, F, F, F, T, T, PipeBlockFamily.MACHINE, null);
	public static final FluidPipeMaterial MAT_STEEL = new FluidPipeMaterial("steel", "Steel", 26140, 200, T, F, F, F, T, F, T, T, PipeBlockFamily.MACHINE, null);
	public static final FluidPipeMaterial MAT_DESH = new FluidPipeMaterial("desh", "Desh", 26620, 200, T, F, F, T, T, F, T, T, PipeBlockFamily.MACHINE, null);
	public static final FluidPipeMaterial MAT_CHROMIUM = new FluidPipeMaterial("chromium", "Chromium", 26280, 200, T, T, F, F, T, F, T, T, PipeBlockFamily.MACHINE, null);
	public static final FluidPipeMaterial MAT_HSLA = new FluidPipeMaterial("hsla", "HSLA-Steel", 26360, 250, T, F, F, F, T, F, T, T, PipeBlockFamily.MACHINE, null);
	public static final FluidPipeMaterial MAT_EFRINE = new FluidPipeMaterial("efrine", "Efrine", 26720, 250, T, F, T, T, F, F, T, T, PipeBlockFamily.MACHINE, null);
	public static final FluidPipeMaterial MAT_GALVANIZED_STEEL = new FluidPipeMaterial("galvanized_steel", "Galvanized Steel", 26260, 250, T, F, F, F, T, F, T, T, PipeBlockFamily.MACHINE, null);
	public static final FluidPipeMaterial MAT_STAINLESS_STEEL = new FluidPipeMaterial("stainless_steel", "Stainless Steel", 26160, 250, T, T, F, F, T, F, T, T, PipeBlockFamily.MACHINE, null);
	public static final FluidPipeMaterial MAT_TUNGSTEN_ALLOY = new FluidPipeMaterial("tungsten_alloy", "Tungsten Alloy", 26700, 300, T, F, F, T, T, F, T, T, PipeBlockFamily.MACHINE, null);
	public static final FluidPipeMaterial MAT_TITANIUM = new FluidPipeMaterial("titanium", "Titanium", 26180, 300, T, F, F, F, T, F, T, T, PipeBlockFamily.MACHINE, null);
	public static final FluidPipeMaterial MAT_NETHERITE = new FluidPipeMaterial("netherite", "Netherite", 26660, 300, T, T, T, T, F, F, T, T, PipeBlockFamily.MACHINE, null);
	public static final FluidPipeMaterial MAT_WORKERS_ALLOY = new FluidPipeMaterial("workers_alloy", "Workers Alloy", 26760, 350, T, F, F, T, T, F, T, T, PipeBlockFamily.MACHINE, null);
	public static final FluidPipeMaterial MAT_TUNGSTEN = new FluidPipeMaterial("tungsten", "Tungsten", 26200, 350, T, T, F, T, T, F, T, T, PipeBlockFamily.MACHINE, null);
	public static final FluidPipeMaterial MAT_PALLADIUM = new FluidPipeMaterial("palladium", "Palladium", 26780, 400, T, F, F, T, T, F, T, T, PipeBlockFamily.MACHINE, null);
	public static final FluidPipeMaterial MAT_VANADIUM_STEEL = new FluidPipeMaterial("vanadium_steel", "Vanadiumsteel", 26740, 400, T, T, F, F, T, F, T, T, PipeBlockFamily.MACHINE, null);
	public static final FluidPipeMaterial MAT_TUNGSTENSTEEL = new FluidPipeMaterial("tungstensteel", "Tungstensteel", 26220, 400, T, F, F, T, T, F, T, T, PipeBlockFamily.MACHINE, null);
	public static final FluidPipeMaterial MAT_TUNGSTEN_CARBIDE = new FluidPipeMaterial("tungsten_carbide", "Tungsten Carbide", 26240, 450, T, F, F, T, T, F, T, T, PipeBlockFamily.MACHINE, null);
	public static final FluidPipeMaterial MAT_IRIDIUM = new FluidPipeMaterial("iridium", "Iridium", 26440, 500, T, T, F, T, T, F, T, T, PipeBlockFamily.MACHINE, null);
	public static final FluidPipeMaterial MAT_GAIA_SPIRIT = new FluidPipeMaterial("gaia_spirit", "Gaia Spirit", 26560, 1000, T, T, T, T, F, F, T, T, PipeBlockFamily.MACHINE, null);
	public static final FluidPipeMaterial MAT_DRACONIUM = new FluidPipeMaterial("draconium", "Draconium", 26380, 2500, T, T, T, T, F, F, T, T, PipeBlockFamily.MACHINE, null);
	public static final FluidPipeMaterial MAT_AWAKENED_DRACONIUM = new FluidPipeMaterial("awakened_draconium", "Awakened Draconium", 26420, 10000, T, T, T, T, F, F, T, T, PipeBlockFamily.MACHINE, null);
	public static final FluidPipeMaterial MAT_INFINITY = new FluidPipeMaterial("infinity", "Infinity", 26600, 1000000000, T, T, T, T, F, F, T, T, PipeBlockFamily.MACHINE, null);
	public static final FluidPipeMaterial MAT_ADAMANTIUM = new FluidPipeMaterial("adamantium", "Adamantium", 26300, 10000, T, T, T, T, F, F, T, T, PipeBlockFamily.MACHINE, null);
	public static final FluidPipeMaterial MAT_BEDROCK_HSLA_ALLOY = new FluidPipeMaterial("bedrock_hsla_alloy", "Bedrock-HSLA-Alloy", 26320, 1000, T, F, F, T, F, F, T, T, PipeBlockFamily.MACHINE, null);
	public static final FluidPipeMaterial MAT_THAUMIUM = new FluidPipeMaterial("thaumium", "Thaumium", 26460, 250, T, T, F, T, F, F, T, T, PipeBlockFamily.MACHINE, null);
	public static final FluidPipeMaterial MAT_MANASTEEL = new FluidPipeMaterial("manasteel", "Manasteel", 26540, 250, T, T, F, T, F, F, T, T, PipeBlockFamily.MACHINE, null);
	public static final FluidPipeMaterial MAT_VOID_METAL = new FluidPipeMaterial("void_metal", "Void Metal", 26480, 500, T, T, F, T, F, F, T, T, PipeBlockFamily.MACHINE, null);
	public static final FluidPipeMaterial MAT_TERRASTEEL = new FluidPipeMaterial("terrasteel", "Terrasteel", 26580, 500, T, T, F, T, F, F, T, T, PipeBlockFamily.MACHINE, null);
	public static final FluidPipeMaterial MAT_CARBON = new FluidPipeMaterial("carbon", "Carbon", 26500, 1000, T, F, F, F, F, F, F, T, PipeBlockFamily.MACHINE, null);
	public static final FluidPipeMaterial MAT_TANTALUM_HAFNIUM_CARBIDE = new FluidPipeMaterial("tantalum_hafnium_carbide", "Tantalum Hafnium Carbide", 26640, 300, T, F, F, T, T, F, T, T, PipeBlockFamily.MACHINE, null);

	public static final List<FluidPipeMaterial> MATERIALS = List.of(
			MAT_WOOD, MAT_WOOD_TREATED, MAT_IRON_WOOD, MAT_PLASTIC, MAT_RUBBER,
			MAT_COPPER, MAT_GOLD, MAT_ALUMINIUM, MAT_TIN_ALLOY, MAT_BRONZE,
			MAT_INVAR, MAT_STEEL, MAT_DESH, MAT_CHROMIUM, MAT_HSLA,
			MAT_EFRINE, MAT_GALVANIZED_STEEL, MAT_STAINLESS_STEEL, MAT_TUNGSTEN_ALLOY, MAT_TITANIUM,
			MAT_NETHERITE, MAT_WORKERS_ALLOY, MAT_TUNGSTEN, MAT_PALLADIUM, MAT_VANADIUM_STEEL,
			MAT_TUNGSTENSTEEL, MAT_TUNGSTEN_CARBIDE, MAT_IRIDIUM, MAT_GAIA_SPIRIT, MAT_DRACONIUM,
			MAT_AWAKENED_DRACONIUM, MAT_INFINITY, MAT_ADAMANTIUM, MAT_BEDROCK_HSLA_ALLOY, MAT_THAUMIUM,
			MAT_MANASTEEL, MAT_VOID_METAL, MAT_TERRASTEEL, MAT_CARBON, MAT_TANTALUM_HAFNIUM_CARBIDE);

	/** The registration-order variant list (the addFluidPipes body order, :92-98). */
	public static final List<FluidPipeVariant> VARIANTS = List.of(FluidPipeVariant.values());

	/**
	 * All rows in registration order (material-major, then the addFluidPipes variant order).
	 * The mdh-6 family gate filters the walk: an ABSENT domain's material skips all its
	 * variants entirely (the class-load seed precedes this walk, FMLModContainer.constructMod
	 * order); default all-PRESENT = the full 280 (ADR-MDH1). Registration, the tab walk and
	 * the BET arrays all consume this one list, so the gate here covers every face.
	 */
	public static final List<FluidPipeRow> ROWS;
	static {
		List<FluidPipeRow> tRows = new ArrayList<>();
		for (FluidPipeMaterial tMat : MATERIALS) {
			if (!tMat.registers()) continue; // the mdh-6 family gate
			for (FluidPipeVariant tVariant : VARIANTS) {
				tRows.add(new FluidPipeRow(tMat, tVariant));
			}
		}
		ROWS = List.copyOf(tRows);
	}

	/** The registered blocks by path (the BET multi-mount + the datagen/loot walkers). */
	public static final Map<String, RegistryObject<GTFluidPipeBlock>> BLOCKS_BY_PATH = new LinkedHashMap<>();

	/** The registered items, same keys as {@link #BLOCKS_BY_PATH}. */
	public static final Map<String, RegistryObject<Item>> ITEMS_BY_PATH = new LinkedHashMap<>();

	static {
		for (FluidPipeRow tRow : ROWS) {
			// the per-row NBT face (NBT_HARDNESS/NBT_RESISTANCE blocking pair :92,
			// NBT_OPAQUE = aBlocking; the family sound = the normalisation layer)
			BLOCKS_BY_PATH.put(tRow.path(), BLOCKS.register(tRow.path(),
					() -> new GTFluidPipeBlock(rowByPath(tRow.path()), pipeProperties(tRow))));
			// the composed-name item (the GTComposedNameItem posture — the stack name
			// delegates to the block's composed getName); the pipe_fluid rows ride the
			// per-row constants [capacity/2, capacity] — MultiTileEntityPipeFluid.java:216-217
			// (bandwidth = makeString(mCapacity/2) L/t, capacity = makeString(mCapacity) L)
			ITEMS_BY_PATH.put(tRow.path(), ITEMS.register(tRow.path(),
					() -> new GTFluidPipeBlockItem(GTFluidPipes.BLOCKS_BY_PATH.get(tRow.path()).get(),
							new Item.Properties().stacksTo(tRow.variant().maxStack), "pipe_fluid",
							GT6Tooltips.makeString(tRow.capacity() / 2), GT6Tooltips.makeString(tRow.capacity()))));
		}
	}

	/** The W1 wood tiers, kept as named seams (the cracker recipes + the smoke line consumers). */
	public static final RegistryObject<GTFluidPipeBlock> WOOD_FLUID_PIPE_SMALL = BLOCKS_BY_PATH.get("wood_fluid_pipe_small");
	public static final RegistryObject<GTFluidPipeBlock> WOOD_FLUID_PIPE_MEDIUM = BLOCKS_BY_PATH.get("wood_fluid_pipe_medium");
	public static final RegistryObject<Item> WOOD_FLUID_PIPE_SMALL_ITEM = ITEMS_BY_PATH.get("wood_fluid_pipe_small");
	public static final RegistryObject<Item> WOOD_FLUID_PIPE_MEDIUM_ITEM = ITEMS_BY_PATH.get("wood_fluid_pipe_medium");

	/** The row lookup behind the block-carrier lambda (path -> row, the static ROWS table). */
	@Nullable
	public static FluidPipeRow rowByPath(String aPath) {
		for (FluidPipeRow tRow : ROWS) {
			if (tRow.path().equals(aPath)) return tRow;
		}
		return null;
	}

	/**
	 * The shared pipe properties — the ONE chain all 280 rows build from (the GTWires
	 * .wireProperties seam form); the strength/sound pair rides the row (the :92
	 * NBT_HARDNESS/NBT_RESISTANCE blocking pair + the family sound axis). issue #9: the
	 * pipe renders sub-cube quads over the default FULL-CUBE shape — a true canOcclude
	 * culls neighbor faces (X-ray); the axle pipe-block convention (GT6Kinetics.java:244).
	 */
	static BlockBehaviour.Properties pipeProperties(FluidPipeRow aRow) {
		return BlockBehaviour.Properties.of()
				.strength(aRow.material().blocking() ? 2.0F : 1.0F, aRow.material().blocking() ? 6.0F : 2.0F)
				.sound(aRow.material().blockFamily().sound)
				.noOcclusion().isViewBlocking(GTFluidPipes::never);
	}

	/**
	 * The shared pipe BET: one BlockEntityType over all 280 blocks (ADR-P3-1, the
	 * "one TE class, many material blocks" multi-mount). Registry path "fluid_pipe"
	 * mirrors GTFluidPipeBlockEntity#getTileEntityName.
	 */
	public static final RegistryObject<BlockEntityType<GTFluidPipeBlockEntity>> FLUID_PIPE_BE =
			BLOCK_ENTITY_TYPES.register("fluid_pipe", () -> BlockEntityType.Builder.of(
					GTFluidPipeBlockEntity::new, blockArray()).build(null));

	/** The block list in registration order (the BET multi-mount array). */
	public static Block[] blockArray() {
		Block[] rBlocks = new Block[BLOCKS_BY_PATH.size()];
		int i = 0;
		for (RegistryObject<GTFluidPipeBlock> tBlock : BLOCKS_BY_PATH.values()) rBlocks[i++] = tBlock.get();
		return rBlocks;
	}

	/** The lookup for /gt6pipe-style consumers — null for an unknown path (the blockByPath precedent). */
	@Nullable
	public static Block blockByPath(String aPath) {
		RegistryObject<GTFluidPipeBlock> tHandle = BLOCKS_BY_PATH.get(aPath);
		return tHandle == null ? null : tHandle.get();
	}

	/**
	 * The "Fluid Pipes" category tab — the upstream MTE-registry category
	 * ("Fluid Pipes", the aCreativeTabID 26142 column, Loader_MultiTileEntities
	 * .java:1846-1885), the GTItemPipes ITEM_PIPES_TAB shape over the full row walk.
	 */
	public static final RegistryObject<CreativeModeTab> FLUID_PIPES_TAB = CREATIVE_MODE_TABS.register("fluid_pipes",
			() -> CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
					.title(Component.translatable("itemGroup.gt6.fluid_pipes"))
					.icon(() -> new ItemStack(ITEMS_BY_PATH.get("wood_fluid_pipe_medium").get()))
					.displayItems((aParameters, aOutput) -> {
						for (FluidPipeRow tRow : ROWS) {
							aOutput.accept(new ItemStack(ITEMS_BY_PATH.get(tRow.path()).get()));
						}
					})
					.build());

	private GTFluidPipes() {}

	/** issue #9: the pipe family never blocks the view (fog) — the GTWires::never rider form. */
	private static boolean never(BlockState aState, BlockGetter aLevel, BlockPos aPos) {
		return false;
	}

	/** FMLConstructModEvent = the first mod-bus lifecycle stage (GTBlockEntities.onModConstruct doc). */
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
		BLOCKS.register(tModBus);
		BLOCK_ENTITY_TYPES.register(tModBus);
		ITEMS.register(tModBus);
		CREATIVE_MODE_TABS.register(tModBus);
	}
}
