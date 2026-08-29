package gregtech6;

import java.util.Objects;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * Mod 入口（骨架，p2-mdk-skeleton）。
 *
 * <p>本卡不注册任何内容。注册桥（p2-registration-bridge）在构造器接线三段：
 * <ol>
 * <li>{@code FMLConstructModEvent.enqueueWork} → {@code MT.init()} + {@code OP.init()}（open→closed）</li>
 * <li>{@code RegisterEvent}（EventPriority.LOW，按 {@code event.getRegistryKey()==Registries.ITEM} 过滤）
 * → 遍历 MaterialRegistry × 白名单前缀动态灌入</li>
 * <li>{@code FMLCommonSetupEvent.enqueueWork} → {@code MaterialGraph.applyCrucibleAlloyReferences()}</li>
 * </ol>
 */
@Mod("gt6")
public class GT6Mod {

    public GT6Mod() {
        // 1.20.1 取 mod bus 的唯一姿势：FMLJavaModLoadingContext.get().getModEventBus()
        //（构造器注入 IEventBus 是 20.2+ 才有）。此处仅验证总线在 construct 期可达，留作下一卡挂载点。
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        Objects.requireNonNull(modBus, "mod event bus must be available at construct time");
    }
}
