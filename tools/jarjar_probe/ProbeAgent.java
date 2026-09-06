import java.io.FileOutputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

/**
 * P20④ jarJar 嵌套类加载探针（tools/jarjar_smoke.py 专用，不进任何分发产物）。
 *
 * Forge/NeoForge 生产启动器对 jarJar 嵌套 jar 的加载是惰性的：modularui 的 EvalEx
 * 只在 GUI 数学表达式求值时才会真正类加载，dedicated server 起服天然触不到——
 * 结构在位（jar_content_check.py 已断言）不等于类加载存活。本 agent 记录
 * brachy/modularui 类的真实定义（= modularui 从 level-1 嵌 jar 被加载），并在
 * 【mixin prepare 之后】（net.minecraft.server.dedicated.DedicatedServer 类定义时）
 * 用同一加载器解析探针类清单，把两层（乃至三层，MixinExtras 内包）嵌套的可解析性
 * 钉成事实，写入 marker 文件供冒烟脚本断言。
 *
 * 时序红线（2026-09-07 首跑实证）：探针若在 brachy 首类 transform 回调里立即
 * Class.forName(..., initialize=true)，会连带提前静态初始化 modularui 的 vanilla
 * 引用链（Component/BuiltInRegistries…），踩碎 mixin 的 target-not-loaded 不变量
 * （MixinTargetAlreadyLoadedException → boot 死）。DedicatedServer 触发点在 mixin
 * prepare 与 mod 构造之后，同类解析无此副作用。
 *
 * premain 参数（逗号分隔）：
 *   <markerPath>,<dotted probe class>[,<dotted probe class>...]
 *
 * marker 格式（行式，逐条 append+flush）：
 *   probe=started
 *   brachy_first_seen=<首个被定义的 modularui 类>
 *   loader=<加载器类名>
 *   <probe class>=LOADED@<clsLoader> | <probe class>=FAILED:<exception>
 *   modularui_classes=<被定义的 brachy/modularui 类计数>
 *   probe=done        （shutdown hook 兜底写）
 */
public class ProbeAgent {

    // DedicatedServer 本体类定义极早（ServerMain 反射 getMethod 阶段，先于 mod 构造与
    // registry bootstrap——2026-09-07 二跑实证，触发即炸 BuiltInRegistries clinit）。
    // 内部类（DedicatedServer$*）在服务端对象构造期才定义 = mods 已构造、注册表已冻结，
    // 解析再由守护线程延迟执行，彻底脱离类定义上下文。
    private static final String RESOLVE_TRIGGER_PREFIX =
            "net/minecraft/server/dedicated/DedicatedServer$";
    private static final long RESOLVE_DELAY_MS = 5000;

    private static final Object LOCK = new Object();
    private static FileOutputStream marker;
    private static volatile boolean resolved = false;
    private static String[] probeClasses = new String[0];
    private static volatile int modularuiClassCount = 0;
    private static volatile String brachyFirstSeen = null;

    public static void premain(String args, Instrumentation inst) {
        final String[] parts = args == null ? new String[]{} : args.split(",");
        final String markerPath = parts.length > 0 ? parts[0] : "probe_marker.txt";
        probeClasses = new String[Math.max(parts.length - 1, 0)];
        System.arraycopy(parts, 1, probeClasses, 0, probeClasses.length);
        try {
            marker = new FileOutputStream(markerPath, false);
            write("probe=started");
        } catch (Throwable t) {
            System.err.println("[jarjar-probe] marker open failed: " + t);
            return;
        }
        inst.addTransformer(new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader loader, String className,
                    Class<?> beingDefined, ProtectionDomain domain, byte[] bytes) {
                if (className == null) {
                    return null;
                }
                if (className.startsWith("brachy/modularui/")) {
                    modularuiClassCount++;
                    if (brachyFirstSeen == null) {
                        brachyFirstSeen = className;
                        write("brachy_first_seen=" + className
                                + " loader=" + (loader == null ? "BOOTSTRAP"
                                : loader.getClass().getName()));
                    }
                }
                if (!resolved && className.startsWith(RESOLVE_TRIGGER_PREFIX)) {
                    resolved = true;
                    final ClassLoader resolveLoader = loader;
                    Thread t = new Thread(() -> {
                        try {
                            Thread.sleep(RESOLVE_DELAY_MS);
                        } catch (InterruptedException ignored) {
                        }
                        resolveAll(resolveLoader);
                    }, "jarjar-probe-resolver");
                    t.setDaemon(true);
                    t.start();
                }
                return null; // 不改字节码
            }
        });
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            write("modularui_classes=" + modularuiClassCount);
            write("probe=done");
        }));
    }

    private static void resolveAll(final ClassLoader loader) {
        write("resolve_at=" + RESOLVE_TRIGGER_PREFIX + " (+" + RESOLVE_DELAY_MS
                + "ms thread)");
        for (String dotted : probeClasses) {
            if (dotted.isEmpty()) {
                continue;
            }
            try {
                Class<?> c = Class.forName(dotted, true, loader);
                ClassLoader actual = c.getClassLoader();
                write(dotted + "=LOADED" + (actual == null ? "@BOOTSTRAP"
                        : "@" + actual.getClass().getSimpleName()));
            } catch (Throwable t) {
                write(dotted + "=FAILED:" + t.getClass().getSimpleName());
            }
        }
        write("modularui_classes_at_resolve=" + modularuiClassCount);
    }

    private static synchronized void write(String line) {
        if (marker == null) {
            return;
        }
        try {
            marker.write((line + "\n").getBytes("UTF-8"));
            marker.flush();
        } catch (Throwable ignored) {
            // marker 失败不干扰被测 JVM
        }
    }
}
