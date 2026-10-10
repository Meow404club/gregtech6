package gregtech6.gui.machines;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;

/**
 * The MUI machine-factory registration construct seam (task basic-machines-mui-migration —
 * the user ruling 本来不就应该全部走 MUI 拼接 UI 吗？干嘛搞特殊，我要全部统一): the vanilla
 * MenuType register that shared this construct listener retired with the whole vanilla leg
 * (the oven-card self-contained-listener form), and this is the surviving half — the
 * ModularUI factory identities that ride the OpenGuiPacket name wire, no MenuType of ours
 * involved (the P26 no-new-MenuType freeze). One factory per GUI over one holder (the
 * framework doctrine): the bumbliary pair (task bumbliary-gui — the scoop variant) and the
 * ACT dual pair (task r11a-act-factory-register — the top face opens CRAFT, the front/back
 * faces BELT).
 *
 * <p>FMLConstructModEvent = first mod-bus lifecycle stage, strictly before any RegisterEvent
 * (GTMenuTypes.java:76-80 precedent). Loader-agnostic throughout: the factory registration
 * body is byte-identical on both legs (the DeferredRegister bus plumbing was the only leg
 * fork in the retired register).
 */
@Mod.EventBusSubscriber(modid = GTMuiFactories.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GTMuiFactories {

	public static final String MOD_ID = "gt6";

	private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();

	private GTMuiFactories() {
	}

	@SubscribeEvent
	public static void onModConstruct(FMLConstructModEvent aEvent) {
		// task bumbliary-gui — the bumbliary GUI pair rides the MUI factory identity
		// (the OpenGuiPacket wire carries the factory name, so the scoop/normal variant
		// needs no MenuType and no PosGuiData change; the P26 no-new-MenuType freeze holds)
		brachy.modularui.factory.GuiManager.registerFactory(GT6BumbliaryMUI.Factory.NORMAL);
		brachy.modularui.factory.GuiManager.registerFactory(GT6BumbliaryMUI.Factory.SCOOP);
		LOGGER.info("GT6 bumbliary UI factories registered (gt6:bumbliary + gt6:bumbliary_scoop — the ModularUI factory-identity wire, task bumbliary-gui)");
		// task r11a-act-factory-register — the ACT dual-GUI pair rides the same
		// factory-identity wire (the top face opens CRAFT, the front/back faces BELT)
		registerActUIFactories();
	}

	/**
	 * The ACT UI factory registration — public seam so the offline gate drives the exact
	 * production path (the GT6ActDualGuiTest r11a pin): the server {@code Factory.open}
	 * hands the factory object straight to {@code GuiManager.open}, but the client
	 * {@code OpenGuiPacket} ctor re-resolves it by name (OpenGuiPacket.java:29 →
	 * GuiManager.getFactory :65-68) — an unregistered factory is the client crash face.
	 */
	public static void registerActUIFactories() {
		brachy.modularui.factory.GuiManager.registerFactory(gregtech6.menu.act.GTActMenu.Factory.CRAFT);
		brachy.modularui.factory.GuiManager.registerFactory(gregtech6.menu.act.GTActMenu.Factory.BELT);
		LOGGER.info("GT6 ACT UI factories registered (gt6:advanced_crafting_table + gt6:advanced_crafting_table_belt — the ModularUI factory-identity wire, task r11a-act-factory-register)");
	}
}
