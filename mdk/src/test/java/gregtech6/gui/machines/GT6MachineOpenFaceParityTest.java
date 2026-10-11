package gregtech6.gui.machines;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nullable;

import brachy.modularui.api.widget.IWidget;
import brachy.modularui.screen.ModularPanel;
import brachy.modularui.value.sync.ModularSyncManager;
import brachy.modularui.value.sync.PanelSyncManager;
import brachy.modularui.widgets.FluidDisplayWidget;
import brachy.modularui.widgets.ProgressWidget;
import brachy.modularui.widgets.slot.ItemSlot;
import brachy.modularui.widgets.slot.ModularSlot;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.block.multiblock.GTFusionReactorBlock;
import gregtech6.block.multiblock.GTImplosionCompressorBlock;
import gregtech6.block.multiblock.GTMassfabBlock;
import gregtech6.recipes.GT6RecipeMaps;
import gregtech6.recipes.RecipeMap;
import gregtech6.tileentity.multiblocks.GTMultiBlocksOfflineTestBase;
import gregtech6.tileentity.multiblocks.TileEntityBase10MultiBlockMachine;
import gregtech6.tileentity.multiblocks.TileEntityFusionReactor;
import gregtech6.tileentity.multiblocks.TileEntityImplosionCompressor;
import gregtech6.tileentity.multiblocks.TileEntityMassfab;

/**
 * The massfab / fusion / implosion open-face parity pin (task machine-openface-gap — the
 * machine-gui-sheet-purge debt closing, the user ruling 全部机器 GUI 统一 MUI id1729).
 *
 * <p>Upstream reality (the per-family archaeology all three families share): the 1.7.10
 * {@code TileEntityBase10MultiBlockMachine} EXTENDS {@code MultiTileEntityBasicMachine},
 * so MatterFabricator (:43), FusionReactor (:45) and ImplosionCompressor (:44) inherit the
 * BasicMachine GUI — the plain right-click open (MultiTileEntityBasicMachine
 * .onBlockActivated3 :483-486 {@code openGUI(aPlayer, aSide)}) over the family panel: the
 * map's item seats + the special gear + the FL.display tank seats (:465-466) + the progress
 * arrow. The port BEs already rode the MUI chain (MenuProvider + GT6MuiMachine + the
 * base buildUI delegation) but their controller blocks carried NO use arm — the GUI was
 * unreachable (the sheet-purge "open face 缺口"). This pin closes it per family:
 * <ul>
 * <li><b>the open face</b> — each controller block declares the use arm (forge
 *     {@code use} / 21.1 {@code useWithoutItem}) and each BE implements
 *     {@link GT6MuiMachine} (the tryOpen bound) with the declared null createMenu;</li>
 * <li><b>the panel</b> — the shared {@link GTBasicMachineMUI} build over the family's REAL
 *     map: the map-sized item topology (the base live-input-truth fix — the Host default 1
 *     seated ONE input on the 3/3 and 2/6 maps and bound the outputs over the input
 *     slots), the live tank-bank display seats (the upstream :267/:268 face: massfab
 *     0+2, fusion 2+6, implosion 0+1), the map-key title, the arrow, the 36 player
 *     seats.</li>
 * </ul>
 *
 * <p>The already-migrated families stay untouched: the cokeoven zero-display-seat banks
 * keep their p8 pin (GT6BasicMachinesMuiParityTest), the twelve large machines shadow the
 * base input truth with the identical :724 body, the tower builds its own panel.
 */
public class GT6MachineOpenFaceParityTest extends GTMultiBlocksOfflineTestBase {

	private static final BlockPos P1 = new BlockPos(200, 64, 100);

	static BlockEntityType<TileEntityMassfab> sMassfabType;
	static BlockEntityType<TileEntityFusionReactor> sFusionType;
	static BlockEntityType<TileEntityImplosionCompressor> sImplosionType;

	@BeforeAll
	static void buildFixtures() throws Exception {
		// the hermetic bootstrap (the GT6LargeMachineMUIPanelTest :78 form) — the lazy
		// recipes() resolvers need the maps
		GT6RecipeMaps.reset();
		GT6RecipeMaps.init();
		sMassfabType = selfHolder(TileEntityMassfab.class);
		sFusionType = selfHolder(TileEntityFusionReactor.class);
		sImplosionType = selfHolder(TileEntityImplosionCompressor.class);
	}

	/** The self-holder BET (the GT6LargeMachineSemanticsTest form): a BET whose validator news the BE. */
	private static <T extends TileEntityBase10MultiBlockMachine> BlockEntityType<T> selfHolder(Class<T> aClass)
			throws Exception {
		java.lang.reflect.Constructor<T> tCtor = aClass.getDeclaredConstructor(BlockEntityType.class, BlockPos.class,
				net.minecraft.world.level.block.state.BlockState.class);
		tCtor.setAccessible(true);
		BlockEntityType<T>[] tHolder = (BlockEntityType<T>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> {
					try {
						return tCtor.newInstance(tHolder[0], aPos, aState);
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				},
				Blocks.BRICKS).build(null);
		return tHolder[0];
	}

	/**
	 * The offline FML dist shaping (the GT6DistillationTowerMUIPanelTest form — the vendored
	 * MUI widget classes read FMLEnvironment.dist during their static init; SERVER selects
	 * the headless text branch). Must run BEFORE the first widget class initializes.
	 */
	@BeforeAll
	static void armFmlDistOffline() throws Exception {
		Class<?> tFmlEnv = Class.forName("net.minecraftforge.fml.loading.FMLEnvironment", false,
				GT6MachineOpenFaceParityTest.class.getClassLoader());
		java.lang.reflect.Field tDist = tFmlEnv.getDeclaredField("dist");
		sun.misc.Unsafe tUnsafe;
		java.lang.reflect.Field tTheUnsafe = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
		tTheUnsafe.setAccessible(true);
		tUnsafe = (sun.misc.Unsafe) tTheUnsafe.get(null);
		if (tUnsafe.getObject(tFmlEnv, tUnsafe.staticFieldOffset(tDist)) == null) {
			tUnsafe.putObject(tFmlEnv, tUnsafe.staticFieldOffset(tDist),
					net.minecraftforge.api.distmarker.Dist.DEDICATED_SERVER);
		}
	}

	/** A fresh headless sync manager (no player → the panel builds without the inventory bind). */
	private static PanelSyncManager headlessSyncManager() {
		return new PanelSyncManager(new ModularSyncManager(false), true);
	}

	/** Every widget in the panel tree, in child order. */
	private static List<IWidget> allWidgets(ModularPanel<?> aPanel) {
		List<IWidget> tAll = new ArrayList<>();
		collectAll(aPanel, tAll);
		return tAll;
	}

	private static void collectAll(IWidget aWidget, List<IWidget> aSink) {
		aSink.add(aWidget);
		if (aWidget instanceof brachy.modularui.widget.ParentWidget<?> tParent) {
			for (IWidget tChild : tParent.getChildren()) {
				collectAll(tChild, aSink);
			}
		}
	}

	/** The widget by its build name. */
	@Nullable
	private static IWidget named(ModularPanel<?> aPanel, String aName) {
		return allWidgets(aPanel).stream().filter(w -> aName.equals(w.getName())).findFirst().orElse(null);
	}

	/** The widget build position (the GT6BasicMachineMUIPanelTest.posOf form). */
	private static int posOf(IWidget aWidget, boolean aX) throws Exception {
		brachy.modularui.api.widget.IPositioned<?> tPositioned = (brachy.modularui.api.widget.IPositioned<?>) aWidget;
		java.lang.reflect.Field tAxis = tPositioned.resizer().getClass().getDeclaredField(aX ? "x" : "y");
		tAxis.setAccessible(true);
		Object tSizer = tAxis.get(tPositioned.resizer());
		java.lang.reflect.Field tStart = tSizer.getClass().getDeclaredField("start");
		tStart.setAccessible(true);
		Object tUnit = tStart.get(tSizer);
		return (int) ((brachy.modularui.widget.sizer.Unit) tUnit).getValue();
	}

	private record Family(String aName, TileEntityBase10MultiBlockMachine aMachine, RecipeMap aMap,
			Class<? extends net.minecraft.world.level.block.Block> aBlockClass, int aFluidIn, int aFluidOut) {
	}

	private static List<Family> families() {
		List<Family> rFamilies = new ArrayList<>();
		rFamilies.add(new Family("massfab", sMassfabType.create(P1, Blocks.BRICKS.defaultBlockState()),
				GT6RecipeMaps.MASSFAB, GTMassfabBlock.class, 0, 2));
		rFamilies.add(new Family("fusion", sFusionType.create(P1, Blocks.BRICKS.defaultBlockState()),
				GT6RecipeMaps.FUSION, GTFusionReactorBlock.class, 2, 6));
		rFamilies.add(new Family("implosion", sImplosionType.create(P1, Blocks.BRICKS.defaultBlockState()),
				GT6RecipeMaps.IMPLOSION, GTImplosionCompressorBlock.class, 0, 1));
		return rFamilies;
	}

	// ---------------------------------------------------------------------------
	// the open face — the use arm + the BE contract (the offline half)
	// ---------------------------------------------------------------------------

	@Test
	public void theControllerBlocksCarryTheUseArmAndTheBEsRideTheMuiChain() throws Exception {
		for (Family tFamily : families()) {
			// the BE contract: the family BE is a GT6MuiMachine — the tryOpen intersection
			// bound (BlockEntity & GT6MuiMachine) accepts it (the compile-time face the use
			// arm dispatches through)
			assertTrue(GT6MuiMachine.class.isAssignableFrom(tFamily.aMachine().getClass()),
					tFamily.aName() + ": the BE implements GT6MuiMachine (the tryOpen bound)");
			// the declared MenuProvider face (the ACT C2 form) — never called by the chain
			assertNull(tFamily.aMachine().createMenu(0, null, null),
					tFamily.aName() + ": createMenu is the declared face, returns null");
			// the use face exists under either leg's name (forge use / 21.1 useWithoutItem)
			assertTrue(Arrays.stream(tFamily.aBlockClass().getDeclaredMethods())
					.anyMatch(m -> m.getName().equals("use") || m.getName().equals("useWithoutItem")),
					tFamily.aName() + ": the controller block dispatches the open (the missing use face was the gap)");
		}
	}

	// ---------------------------------------------------------------------------
	// the panels — the shared family face over the real maps
	// ---------------------------------------------------------------------------

	@Test
	public void everyFamilyBuildsTheSharedPanelOverItsOwnMap() throws Exception {
		for (Family tFamily : families()) {
			RecipeMap tMap = tFamily.aMap();
			ModularPanel<?> tPanel = tFamily.aMachine().buildUI(null, headlessSyncManager(), null);

			// the clean-base face: no code background — the gt6 theme 9-slice draws at runtime
			assertNull(tPanel.getBackground(), tFamily.aName() + ": no code background (the theme base owns the panel)");

			// the title — the served map's translatable viewer key (the family arm)
			IWidget tTitle = named(tPanel, "title");
			assertNotNull(tTitle, tFamily.aName() + ": the title widget");
			assertEquals(GregtechViewerMeta.titleKey(tMap),
					((TranslatableContents) ((brachy.modularui.widgets.TextWidget<?>) tTitle).getKey().getContents()).getKey(),
					tFamily.aName() + ": the title rides the shared viewer title-key formula");

			// the seat topology — the map's input truth + the map's outputs + the live banks
			List<IWidget> tAll = allWidgets(tPanel);
			long tItemSeats = tAll.stream().filter(w -> w instanceof ItemSlot).count();
			long tFluidSeats = tAll.stream().filter(w -> w instanceof FluidDisplayWidget).count();
			assertEquals(tMap.mInputItemsCount + tMap.mOutputItemsCount + 36, tItemSeats,
					tFamily.aName() + ": " + tMap.mInputItemsCount + " inputs + " + tMap.mOutputItemsCount
							+ " outputs + 36 player seats");
			assertEquals(tFamily.aFluidIn() + tFamily.aFluidOut(), tFluidSeats,
					tFamily.aName() + ": the fluid seats = the live tank banks (the upstream :267/:268 face)");
			assertEquals(1, tAll.stream().filter(w -> w instanceof ProgressWidget).count(),
					tFamily.aName() + ": exactly the progress bar");

			// the input truth — the live map size, not the Host default 1
			assertEquals(tMap.mInputItemsCount, tFamily.aMachine().getInputSlotCount(),
					tFamily.aName() + ": the live input truth (the Host default 1 mis-sized the multi-input maps)");
			assertEquals(tFamily.aFluidOut(), tFamily.aMachine().getFluidOutputTanks().length,
					tFamily.aName() + ": the output bank the seats render");

			// the bindings — inputs accept at SLOT_INPUT+i, outputs refuse and bind AFTER
			// the inputs (the default-1 era bound them over the input slots)
			for (int i = 0; i < tMap.mInputItemsCount; i++) {
				ModularSlot tInput = ((ItemSlot) named(tPanel, "input_" + i)).getSlot();
				assertEquals(i, tInput.getSlotIndex(), tFamily.aName() + ": input " + i + " binds SLOT_INPUT " + i);
				assertTrue(tInput.mayPlace(new ItemStack(Blocks.BRICKS.asItem())),
						tFamily.aName() + ": input " + i + " accepts");
			}
			for (int i = 0; i < tMap.mOutputItemsCount; i++) {
				ModularSlot tOutput = ((ItemSlot) named(tPanel, "output_" + i)).getSlot();
				assertEquals(tMap.mInputItemsCount + i, tOutput.getSlotIndex(),
						tFamily.aName() + ": output " + i + " binds after the inputs");
				assertFalse(tOutput.mayPlace(new ItemStack(Blocks.BRICKS.asItem())),
						tFamily.aName() + ": output " + i + " refuses insertion (the setCanPut(F) face)");
			}

			// the fluid seats ride the upstream Slot_Render geometry table (the constants
			// themselves are the GT6MachineFluidDisplayTest domain)
			for (int i = 0; i < tFamily.aFluidIn(); i++) {
				IWidget tSeat = named(tPanel, "fluid_in_" + i);
				assertNotNull(tSeat, tFamily.aName() + ": fluid_in_" + i + " seated");
				int[] tExpected = GTBasicMachineMenu.fluidDisplayPos(false, i);
				assertEquals(tExpected[0], posOf(tSeat, true), tFamily.aName() + ": fluid_in_" + i + " x");
				assertEquals(tExpected[1], posOf(tSeat, false), tFamily.aName() + ": fluid_in_" + i + " y");
			}
			for (int i = 0; i < tFamily.aFluidOut(); i++) {
				IWidget tSeat = named(tPanel, "fluid_out_" + i);
				assertNotNull(tSeat, tFamily.aName() + ": fluid_out_" + i + " seated");
				int[] tExpected = GTBasicMachineMenu.fluidDisplayPos(true, i);
				assertEquals(tExpected[0], posOf(tSeat, true), tFamily.aName() + ": fluid_out_" + i + " x");
				assertEquals(tExpected[1], posOf(tSeat, false), tFamily.aName() + ": fluid_out_" + i + " y");
			}

			// the arrow part — the amazawa fill (the family face; the direction case is the
			// GT6BasicMachineMUIPanelTest domain)
			ProgressWidget tBar = (ProgressWidget) tAll.stream().filter(w -> w instanceof ProgressWidget).findFirst().orElseThrow();
			java.lang.reflect.Field tDrawableField = ProgressWidget.class.getDeclaredField("progress");
			tDrawableField.setAccessible(true);
			brachy.modularui.drawable.progress.ProgressDrawable tDrawable =
					(brachy.modularui.drawable.progress.ProgressDrawable) tDrawableField.get(tBar);
			brachy.modularui.drawable.UITexture tFill = assertInstanceOf(brachy.modularui.drawable.UITexture.class,
					tDrawable.getFilledTexture(), tFamily.aName() + ": the arrow fill is a texture");
			assertEquals(ResourceLocation.fromNamespaceAndPath("gt6", "textures/gui/parts/arrow_forward_20x18.png"),
					tFill.location(), tFamily.aName() + ": the amazawa arrow part");
		}
	}

	/** The title-key formula alias — keeps the long assertion lines readable. */
	private static final class GregtechViewerMeta {
		static String titleKey(RecipeMap aMap) {
			return gregtech6.jei.GT6RecipeMapViewerMeta.titleKey(aMap);
		}
	}
}
