package brachy.modularui.test;

import brachy.modularui.api.widget.IWidget;
import brachy.modularui.screen.ModularPanel;
import brachy.modularui.screen.ModularScreen;
import brachy.modularui.widgets.ButtonWidget;
import brachy.modularui.widgets.layout.Grid;
import brachy.modularui.widgets.textfield.TextFieldWidget;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P21 smoke ①：离线 headless 最小面板树冒烟（ADR 2026-09-07-p21-smoke-gui-headless-ruling 路线 b）。
 *
 * <p>构造配方从共享 main 上游测试 brachy/modularui/test/TestGuis.java 的 panel/widget 形态抄最小面
 * （ClientGUI.open=MC 客户端单例面，排除）。深度下探点：ModularScreen 构造链
 * （vendored forgeMain :140-163 / neoforgeMain 同构）唯一 loader 依赖 = 末行
 * EVENT_BUS.post(BuildPanelEvent.MainPanel)——testRuntimeClasspath 携 loader 类即裸 JVM 可达
 * （接线见 TestRuntimeClasspathProbeTest 注释）。不调 onResize/open（那才进主题与渲染面）。</p>
 */
class HeadlessGuiSmokeTest {

    @BeforeAll
    static void boot() {
        HeadlessBootstrap.bootStrapVanilla();
    }

    @Test
    public void minimalPanelTreeConstructsAndWiresHeadlessly() {
        ModularPanel<?> panel = ModularPanel.defaultPanel("headless_smoke", 195, 220);
        ButtonWidget<?> button = new ButtonWidget<>();
        TextFieldWidget textField = new TextFieldWidget();
        Grid grid = new Grid();
        panel.child(button);
        panel.child(textField);
        panel.child(grid);

        // 构造成功本身就是冒烟主断言：ModularGuiContext + PanelManager + 事件总线 post 全链
        ModularScreen screen = new ModularScreen("gt6smoke", panel);

        assertSame(panel, screen.getMainPanel(), "main panel round-trips");
        assertSame(panel, screen.getPanelManager().getMainPanel());
        assertEquals("gt6smoke", screen.getOwner());
        assertEquals("headless_smoke", screen.getName());
        assertNotNull(screen.getContext());

        // 树不变量：三个子件按 addChild 序保序在场且为同一实例（构造期 addChild 纯列表添加，
        // initialise 推迟到 open——AbstractParentWidget.addChild 的 isValid 分支；widget toString
        // 无名同形，断言用同一性而非 equals）
        assertEquals(3, panel.getChildren().size());
        assertSame(button, panel.getChildren().get(0));
        assertSame(textField, panel.getChildren().get(1));
        assertSame(grid, panel.getChildren().get(2));
        assertTrue(panel.getChildren().stream().allMatch(c -> c instanceof IWidget));
    }

    @Test
    public void constructedPanelsAreIdleAndDraggablePreOpen() {
        ModularPanel<?> panel = ModularPanel.defaultPanel("sized_smoke");
        assertEquals("sized_smoke", panel.getName());
        assertEquals(ModularPanel.State.IDLE, panel.getState());
        assertTrue(panel.isDraggable(), "draggable is the constructor default; main panel flips it on open");
    }

    @Test
    public void panelAndScreenRejectNullIdentities() {
        assertThrows(NullPointerException.class, () -> ModularPanel.defaultPanel(null));
        // 主面板函数返回 null 在构造期即炸（vendored ModularScreen 私有构造 requireNonNull）
        assertThrows(NullPointerException.class, () -> new ModularScreen("gt6smoke", (ModularPanel<?>) null));
    }

    @Test
    public void twoScreensOnOneOwnerKeepDistinctPanelTrees() {
        ModularPanel<?> first = ModularPanel.defaultPanel("dup_smoke");
        ModularPanel<?> second = ModularPanel.defaultPanel("dup_smoke");
        first.child(new TextFieldWidget());
        second.child(new Grid());

        ModularScreen firstScreen = new ModularScreen("gt6smoke", first);
        ModularScreen secondScreen = new ModularScreen("gt6smoke", second);

        assertSame(first, firstScreen.getMainPanel());
        assertSame(second, secondScreen.getMainPanel());
        assertEquals(1, firstScreen.getMainPanel().getChildren().size());
        assertEquals(1, secondScreen.getMainPanel().getChildren().size());
    }
}
