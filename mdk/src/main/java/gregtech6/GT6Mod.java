package gregtech6;

import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

import gregapi.oredict.MaterialGraph;
import gregtech6.client.GTClientHandlers;
import gregtech6.registry.GTMaterialItems;
import gregtech6.registry.GTModBusListener;

/**
 * Mod entry: the three-segment registration bridge (ADR-P2-2).
 * <ol>
 * <li>{@code FMLConstructModEvent.enqueueWork} -> {@code MT.init()} + {@code OP.init()}
 * (MaterialRegistry open -> closed; MT.init() is a per-generation full refill, wired once);</li>
 * <li>{@code RegisterEvent} at {@code EventPriority.LOW} (GTCEu GTRegistrate.java:148-151
 * precedent) -> MaterialRegistry x whitelist-prefix items plus one creative tab;</li>
 * <li>{@code FMLCommonSetupEvent.enqueueWork} -> {@code MaterialGraph.applyCrucibleAlloyReferences()}
 * (GT6 postInit equivalent, GT_API_Post.java:816-820), then the RegisterEvent listener is
 * unregistered (GTRegistrate.java:156-159 precedent).</li>
 * </ol>
 */
@Mod("gt6")
public class GT6Mod {

    public static final Logger LOGGER = LogManager.getLogger("gt6");

    private final IEventBus modBus;
    /** Registered by instance so it can be unregistered again after CommonSetup (eventbus-6.0.5 keys object registrations by instance). */
    private final GTModBusListener modBusListener = new GTModBusListener();

    public GT6Mod() {
        // 1.20.1 取 mod bus 的唯一姿势：FMLJavaModLoadingContext.get().getModEventBus()
        //（构造器注入 IEventBus 是 20.2+ 才有）。
        modBus = FMLJavaModLoadingContext.get().getModEventBus();
        Objects.requireNonNull(modBus, "mod event bus must be available at construct time");
        modBus.register(modBusListener);
        modBus.addListener(this::onCommonSetup);
        // client-only 事件监听（RegisterColorHandlersEvent.Item 只在 client fire），侧隔离进 @OnlyIn 类
        if (FMLEnvironment.dist == Dist.CLIENT) GTClientHandlers.init(modBus);
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            int tReferences = MaterialGraph.applyCrucibleAlloyReferences();
            GT6Mod.LOGGER.info("GT6 common setup: applyCrucibleAlloyReferences completed ({} alloy reverse references)", tReferences);
        });
        // RegisterEvent 监听用后即注销（GTCEu GTRegistrate.java:156-159 先例）
        modBus.unregister(modBusListener);
    }
}
