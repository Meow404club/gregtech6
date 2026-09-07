package brachy.modularui.test;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P21 smoke-gui-headless 前置 probe（ADR 2026-09-07-p21-smoke-gui-headless-ruling 卡内遗留点）：
 * 断言 testRuntimeClasspath 携带 MC + loader + EvalEx 类，裸 JVM 即可驱动 GUI 逻辑层冒烟。
 *
 * <p>接线方式（probe 结论落点）：build.{forge,neoforge}.gradle.kts 各自把 moddev/legacyforge 的
 * 游戏库配置（modDevCompileDependencies/modDevRuntimeDependencies）extendsFrom 进 test 类路径，
 * 再把 main 的 output+classpath 追加进 test（mdk p3-fullprefix-creativetab 同构先例）。
 * 双节点同断言：loader 总线类按腿 XOR 在场（forge=MinecraftForge / neoforge=NeoForge），
 * 无 chisel 语法，腿无关。</p>
 */
class TestRuntimeClasspathProbeTest {

    @BeforeAll
    static void boot() {
        HeadlessBootstrap.bootStrapVanilla();
    }

    @Test
    public void minecraftClassesAreOnTheTestClasspath() {
        // 在场性口径 = 可解析（initialize=false，不触发 <clinit>）。
        // 初始化口径不可用：ItemStack.<clinit>（1.21.1 :96 EMPTY 字段）需 Bootstrap 注册面，
        // 裸 JVM 即 ExceptionInInitializerError（2026-09-07 首跑实证）——GUI 冒烟路径
        // （MathUtils/panel 树/ModularScreen 构造）不触 MC clinit，无需 Bootstrap.bootStrap()
        // （mdk GT6LangParityTest:114-125 为触 clinit 的测试才需要该先例）。
        assertTrue(isPresent("net.minecraft.world.item.ItemStack"),
                "MC (named dev classpath) must be resolvable from the test JVM");
        assertTrue(isPresent("net.minecraft.util.Mth"),
                "MathUtils references Mth; class must resolve");
        assertTrue(isPresent("net.minecraft.ResourceLocation")
                        || isPresent("net.minecraft.resources.ResourceLocation"),
                "ResourceLocation must resolve (leg-dependent package)");
    }

    @Test
    public void loaderEventBusClassIsPresentExactlyOncePerLeg() {
        boolean forgeBus = isPresent("net.minecraftforge.common.MinecraftForge");
        boolean neoBus = isPresent("net.neoforged.neoforge.common.NeoForge");
        assertTrue(forgeBus ^ neoBus,
                "exactly one loader bus must be on the test classpath (forge=" + forgeBus + ", neo=" + neoBus + ")");
        // BuildPanelEvent 的父类按腿各为 loader Event——总线类在场即 ModularScreen :162 post 可达
        Class<?> event = isPresent("net.minecraftforge.eventbus.api.Event")
                ? classFor("net.minecraftforge.eventbus.api.Event")
                : classFor("net.neoforged.bus.api.Event");
        assertNotNull(event, "loader Event base class must resolve");
    }

    @Test
    public void evalExAndFastutilAreOnTheTestClasspath() throws Exception {
        Class.forName("com.ezylang.evalex.Expression");
        Class.forName("com.ezylang.evalex.config.ExpressionConfiguration");
        Class.forName("it.unimi.dsi.fastutil.ints.IntArrayList");
    }

    @Test
    public void sharedTestSourcesCompileAgainstTheVendoredMainOutput() {
        // 共享 src/test 直接引用 vendored main 类型——test sourceSet 与 main output 同类路径的活证。
        // 注意静态常量（MOD_ID）会被 javac 内联而不触发类加载，改调静态方法强制加载
        // （forge 版 ModularUI clinit 触 BuiltInRegistries，已由 @BeforeAll bootStrap 兜住）。
        assertEquals("modularui", brachy.modularui.ModularUI.id("modularui").getNamespace());
    }

    private static boolean isPresent(String className) {
        try {
            Class.forName(className, false, TestRuntimeClasspathProbeTest.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }

    private static Class<?> classFor(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException | LinkageError e) {
            return null;
        }
    }
}
