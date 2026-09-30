package gregtech6.jade;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import snownee.jade.api.ITooltip;
import snownee.jade.api.ui.BoxStyle;
import snownee.jade.api.ui.IElementHelper;

/**
 * GT6 Jade 行件共享 helper（task jade-redesign-core，design.r8-jade-tooltip 裁定）：
 * 两 provider（{@link GT6MachineProvider}/{@link GT6BoilerProvider}）收敛的纯函数行 +
 * 条元素单点。收录三样：
 * <ol>
 * <li><b>bar 形（B 案裁定）</b>——Jade 原生 progress 条 + 条内文本单元素。API 实证双腿：
 *     forge（Jade 11.13.3）IElementHelper.progress(float, @Nullable Component, IProgressStyle,
 *     IBoxStyle, boolean)（jade-1201 IElementHelper.java:31）；neo（15.x）同签名（jade-1211
 *     :32，IBoxStyle 类型改名）。ProgressElement text != null 时条高 14px、文本画在条内
 *     （ProgressStyle.render:117-140）。文本模板统一三槽 {@code X / Y (Z%)}
 *     （如 gt6.jade.boiler.heat）；<b>例外：无真上限的量不出条保文本</b>（BasicMachine 能量
 *     缓冲无 capacity——ITileEntityEnergy capacitor 半未移植）。jadeBox swap 搬入本类：
 *     1.20.1 BoxStyle.DEFAULT 字段 vs 1.21.1 getNestedBox() 工厂，编译期强制二选一
 *     （GT6MachineProvider 旧缝原样搬迁）。</li>
 * <li><b>公共状态行族（gt6.jade.common.*）</b>——statusLine（Active 绿/Inactive 红，
 *     语义从进度条着色行化）、structureLine（formed/incomplete 两态）、malfunctionLine +
 *     malfunctionDetail（ERROR_MESSAGE 唯一来源 = tick 异常陷阱，TicksAndSync.java:223/228
 *     ——常态行 Malfunction RED，原文截 {@link #MAX_DETAIL_CHARS} 字符进 showDetails 潜行，
 *     玩家面板不透传 Java 异常串）。</li>
 * <li><b>色常量/比例归一</b>——COLOR_OK/COLOR_STALLED = GTCEu WorkableBlockProvider.java:80
 *     字面（原 GT6MachineProvider:93 / GT6BoilerProvider:89 双份复制收编）；ratio 钳位
 *     [0,1]（上限 0 答 0 不 NaN）+ percent 整数百分槽（条内文本统一三槽的第三槽）。</li>
 * </ol>
 *
 * <p>零裸 literal（GT6JadeTooltipKeyPinTest 收编本类）：全部行走 translatable 面，en =
 * GT6EnUs addCommonJade，zh = tsv hand 带 + py HAND_TRANSLATIONS + GT6ZhCn addCommonJadeUnits
 * 四落。离线可测面 = 纯函数行；bar 本体 live-only（IElementHelper 需客户端环境）。
 */
public final class GT6JadeRows {

	/** 有进展/在干活 = 绿（GTCEu WorkableBlockProvider.java:80 字面，原双 provider 复制收编）。 */
	public static final int COLOR_OK = 0xFF4CBB17;
	/** 停滞/空罐 = 红（同上 :80 字面）。 */
	public static final int COLOR_STALLED = 0xFFBB1C28;
	/** 无语义着色 = Jade ProgressStyle 构造器默认白（jade-1201 ProgressStyle.java:32 color(0xFFFFFFFF)）。 */
	public static final int COLOR_NEUTRAL = 0xFFFFFFFF;

	/** 状态行字色（ChatFormatting 形）——Active/Running/formed 绿（GT6ConverterProvider 状态行的
	 * 第三份复制收编本处，task jade-converter-crucible-restyle——J1 交卡遗留债）。 */
	public static final ChatFormatting FORMAT_OK = ChatFormatting.GREEN;
	/** 状态行字色（ChatFormatting 形）——Stopped/Inactive/malfunction/熔毁红（同上收编）。 */
	public static final ChatFormatting FORMAT_STALLED = ChatFormatting.RED;

	/** 状态行两态（mActive&&mRunning 绿 Active / 否则红 Inactive）。 */
	public static final String LANG_STATUS_ACTIVE = "gt6.jade.common.status.active";
	public static final String LANG_STATUS_INACTIVE = "gt6.jade.common.status.inactive";
	/** 多方块成形态两态行（原 gt6.jade.machine.multiblock.* 重键入 common 域，行文 verbatim 承接）。 */
	public static final String LANG_STRUCTURE_FORMED = "gt6.jade.common.formed";
	public static final String LANG_STRUCTURE_INCOMPLETE = "gt6.jade.common.incomplete";
	/** Malfunction 常态行（仅 ERROR_MESSAGE 非空时；原文不上主面）。 */
	public static final String LANG_MALFUNCTION = "gt6.jade.common.malfunction";
	/** Malfunction 潜行明细行：槽 = 截断后的原文。 */
	public static final String LANG_MALFUNCTION_DETAIL = "gt6.jade.common.malfunction.detail";
	/** 潜行原文截断长度（design.r8-jade-tooltip ruling_error_line：64 字符）。 */
	public static final int MAX_DETAIL_CHARS = 64;

	private GT6JadeRows() {
	}

	/** 比例（纯函数）：钳 [0,1]；上限 0 答 0（不 NaN——GT6BoilerProvider.heatRatio 原语义收编）。 */
	public static float ratio(long aCur, long aMax) {
		if (aMax <= 0) return 0.0F;
		return (float)Math.max(0.0, Math.min(1.0, (double)aCur / (double)aMax));
	}

	/** 整数百分槽（纯函数）：钳 0..100，条内文本第三槽统一供给。 */
	public static long percent(long aCur, long aMax) {
		if (aMax <= 0) return 0;
		return Math.max(0, Math.min(aCur, aMax)) * 100 / aMax;
	}

	/**
	 * B 案条（双腿收敛点）：Jade 原生 progress + 条内文本。IElementHelper.get() 静态双腿同形
	 * （jade-1201 IElementHelper.java:14）——1.20.1 的 ITooltip.getElementHelper()（:124）在
	 * 1.21.1 已删除，不能作桥。
	 */
	public static void bar(ITooltip aTooltip, float aRatio, Component aText, int aColor) {
		IElementHelper tHelper = IElementHelper.get();
		aTooltip.add(tHelper.progress(
				aRatio,
				aText,
				// style color(int) 双腿同名同形（Jade 侧 API）——撞 chisel VertexConsumer 改名
				// 条目的坑见 GT6MachineProvider 类 doc（p22 收窄后裸写双腿编译绿）。
				tHelper.progressStyle().color(aColor).textColor(-1),
				jadeBox(),
				true));
	}

	/** 状态行：Active GREEN / Inactive RED（mActive&&mRunning 由调用侧合取）。 */
	public static Component statusLine(boolean aActive) {
		return Component.translatable(aActive ? LANG_STATUS_ACTIVE : LANG_STATUS_INACTIVE)
				.withStyle(aActive ? FORMAT_OK : FORMAT_STALLED);
	}

	/** 多方块成形态行：formed GREEN / incomplete RED。 */
	public static Component structureLine(boolean aFormed) {
		return Component.translatable(aFormed ? LANG_STRUCTURE_FORMED : LANG_STRUCTURE_INCOMPLETE)
				.withStyle(aFormed ? FORMAT_OK : FORMAT_STALLED);
	}

	/** Malfunction 常态行（整行 RED——原文不上主面）。 */
	public static Component malfunctionLine() {
		return Component.translatable(LANG_MALFUNCTION).withStyle(FORMAT_STALLED);
	}

	/** Malfunction 潜行明细：原文截 {@link #MAX_DETAIL_CHARS} 字符进 %s 槽（排障价值保留不刷屏）。 */
	public static Component malfunctionDetail(String aRaw) {
		String tTrimmed = aRaw == null ? ""
				: aRaw.length() > MAX_DETAIL_CHARS ? aRaw.substring(0, MAX_DETAIL_CHARS) : aRaw;
		return Component.translatable(LANG_MALFUNCTION_DETAIL, tTrimmed);
	}

	/**
	 * 进度条盒形 swap（原 GT6MachineProvider.jadeBox 缝原样搬入——双腿 API 各无对方常量/方法，
	 * 编译期强制二选一）。
	 */
	//? if forge {
	private static BoxStyle jadeBox() {
		return BoxStyle.DEFAULT;
	}
	//?} else {
	/*private static BoxStyle jadeBox() {
		return BoxStyle.getNestedBox(); // Jade 自家 addon 同形（EnergyStorageProvider.java:116）
	}
	*///?}

}
