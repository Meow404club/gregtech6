package brachy.modularui.test;

import brachy.modularui.utils.math.MathUtils;
import brachy.modularui.utils.math.ParseResult;
import brachy.modularui.widgets.textfield.TextFieldWidget;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P21 smoke ②：表达式端到端冒烟（ADR 2026-09-07-p21-smoke-gui-headless-ruling）。
 *
 * <p>路径：{@link MathUtils#parseExpression(String, double, boolean)} → EvalEx 3.6.0
 * （MATH_CFG 挂 {@link brachy.modularui.utils.math.PostfixPercentOperator}，
 * SI 前缀经 {@link brachy.modularui.utils.math.SIPrefix#addAllToExpression} 注册为变量）
 * → {@link TextFieldWidget#parse(String)}（vendored 源 :49-67 acceptsExpression 路由）。</p>
 *
 * <p>全用例只走双腿同形 API（三参重载双腿签名一致；forge 腿单/双参重载默认
 * useSiPrefixes=false 与 neo 腿 true 不同——本测试不依赖默认值）。期望值全部实测钉值
 * （EvalEx 3.6.0 jar 直跑实证 2026-09-07），非文档推断。</p>
 */
class ExpressionMathSmokeTest {

    @BeforeAll
    static void boot() {
        // TextFieldWidget.parse 失败路径引 ModularUI.LOGGER（forge 版 ModularUI clinit
        // 触 BuiltInRegistries）——bootStrap 见 HeadlessBootstrap 注释
        HeadlessBootstrap.bootStrapVanilla();
    }

    private static double eval(String expression) {
        ParseResult result = MathUtils.parseExpression(expression, 0, true);
        assertTrue(result.isSuccess(),
                () -> "'" + expression + "' should evaluate but failed: " + result.getErrorMessage());
        return result.getResult().getNumberValue().doubleValue();
    }

    @Test
    public void basicArithmeticEndToEndThroughMathUtils() {
        assertEquals(7.0, eval("1+2*3"), 1e-9);
        assertEquals(9.0, eval("(1+2)*3"), 1e-9);
        // 科学计数法（E 是保留指数记号——这也是 SIPrefix 用 X 表 Exa 的原因）
        assertEquals(1000.0, eval("1e3"), 1e-9);
    }

    @Test
    public void siPrefixesScaleNumbers() {
        assertEquals(5001.0, eval("5k+1"), 1e-9);
        assertEquals(1000.0, eval("k"), 1e-9);
        assertEquals(0.007, eval("7m"), 1e-12);
        assertEquals(1e-6, eval("1µ"), 1e-15);
        assertEquals(3e-9, eval("3n"), 1e-18);
    }

    /**
     * 实测钉值的上游怪癖：EvalEx 3.6.0 变量注册大小写不敏感（首跑实证），
     * SIPrefix 枚举序 Milli('m') 后于 Mega('M') 注册 →后者被遮蔽，"2M" 求值 0.002 而非 2e6。
     * 本断言钉住 vendored 库（EvalEx 3.6.0 钉版）的当前真行为——升 EvalEx 版本时此测试红
     * 即行为漂移哨兵。
     */
    @Test
    public void megaPrefixIsShadowedByMilliOnTheCaseInsensitiveVariableMap() {
        assertEquals(0.002, eval("2M"), 1e-12);
    }

    @Test
    public void percentPostfixOperatorDividesByHundred() {
        assertEquals(0.5, eval("50%"), 1e-9);
        assertEquals(2.0, eval("200%"), 1e-9);
        assertEquals(1.5, eval("50%+1"), 1e-9);
        assertEquals(0.02, eval("(1+1)%"), 1e-9);
    }

    @Test
    public void blankExpressionYieldsTheDefaultValue() {
        ParseResult nullResult = MathUtils.parseExpression(null, 42, true);
        assertTrue(nullResult.isSuccess());
        assertEquals(42.0, nullResult.getResult().getNumberValue().doubleValue(), 1e-9);
        ParseResult emptyResult = MathUtils.parseExpression("", 42, true);
        assertTrue(emptyResult.isSuccess());
        assertEquals(42.0, emptyResult.getResult().getNumberValue().doubleValue(), 1e-9);
    }

    @Test
    public void malformedExpressionsReportFailureWithoutThrowing() {
        for (String bad : new String[] {"2+*", "bogus+", "7mm"}) {
            ParseResult result = MathUtils.parseExpression(bad, 0, true);
            assertTrue(result.isFailure(), () -> "'" + bad + "' must not evaluate");
            assertFalse(result.isSuccess());
            assertNotNull(result.getErrorMessage(), () -> "'" + bad + "' must carry an error message");
            assertNull(result.getResult(), "failed parse carries no value");
        }
    }

    @Test
    public void siPrefixRegistrationCanBeDisabledPerCall() {
        assertTrue(MathUtils.parseExpression("5k", 0, false).isFailure(),
                "unregistered prefix constant must fail evaluation");
        assertEquals(5.0, eval("5"), 1e-9);
    }

    // ---- TextFieldWidget 路径（vendored :49-67：acceptsExpression=true 走 MathUtils）----

    @Test
    public void textFieldParseRoutesThroughTheMathExpressionPath() {
        TextFieldWidget field = new TextFieldWidget();
        assertTrue(field.isAcceptsExpression(), "expressions are accepted by default");
        assertEquals(7.0, field.parse("1+2*3"), 1e-9);
        assertEquals(2000.0, field.parse("2k"), 1e-9);
        assertNull(field.getMathFailMessage());
    }

    @Test
    public void textFieldParseFailureFallsBackToDefaultNumber() {
        TextFieldWidget field = new TextFieldWidget().setDefaultNumber(9);
        assertEquals(9.0, field.parse("2+*"), 1e-9);
        assertNotNull(field.getMathFailMessage());
    }

    @Test
    public void textFieldWithoutExpressionsFallsBackToPlainNumberFormat() {
        TextFieldWidget field = new TextFieldWidget().acceptsExpressions(false);
        assertFalse(field.isAcceptsExpression());
        assertEquals(42.0, field.parse("42"), 1e-9);
        assertEquals(0.0, field.parse("abc"), 1e-9);
        assertEquals("Unable to parse number.", field.getMathFailMessage());
    }
}
