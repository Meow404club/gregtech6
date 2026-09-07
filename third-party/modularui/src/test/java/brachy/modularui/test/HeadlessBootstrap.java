package brachy.modularui.test;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;

/**
 * P21 headless 冒烟共享引导（mdk GT6LangParityTest.boot:113-125 同形）。
 *
 * <p>forge 版 ModularUI 静态初始化含注册面字段（forgeMain ModularUI.java:143
 * {@code BLANK = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY)}），
 * 裸 JVM 未 bootstrap 时首触即 "Not bootstrapped"（2026-09-07 forge 腿首跑实证；
 * neo 腿无此字段故不需此引导，但 bootStrap 幂等无害，双节点共用一套引导保持同源）。
 * offline 环境的 throwables 按 mdk 先例吞掉。</p>
 */
final class HeadlessBootstrap {

    private HeadlessBootstrap() {}

    static void bootStrapVanilla() {
        SharedConstants.tryDetectVersion();
        try {
            Bootstrap.bootStrap();
        } catch (Throwable ignored) {
            // offline-expected
        }
    }
}
