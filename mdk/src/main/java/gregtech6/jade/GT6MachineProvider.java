package gregtech6.jade;

import java.util.Locale;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import gregapi.code.TagData;
import gregtech6.tileentity.TileEntityBase01Root;
import gregtech6.tileentity.machines.TileEntityBasicMachine;
import gregtech6.tileentity.multiblocks.TileEntityBase10MultiBlockBase;
import gregtech6.tileentity.multiblocks.TileEntityBase10MultiBlockMachine;

/**
 * GT6 机器 Jade provider 对（task p21-jade-compat，r8-jade-redesign-core 重设计）：
 * 一个实例同挂 client tooltip（{@link IBlockComponentProvider}）+ server 数据同步
 * （{@link IServerDataProvider}&lt;BlockAccessor&gt;），服务端直读 BE 公开字段。
 *
 * <p>同步契约：appendServerData 写 GT6* 前缀键（与 Jade 自身/他 mod 的键空间隔离，tag 由双腿
 * Jade 网络层搬运，addon 零接触），appendTooltip 经 accessor.getServerData() 读回。只读
 * 公开字段，禁改 BE（铁律）。行件与色常量归一在 {@link GT6JadeRows}（bar 双腿 swap 搬入）。
 *
 * <p>重设计面（design.r8-jade-tooltip families.basic_machine/multiblock_machine）：
 * <ol>
 * <li><b>状态行</b>——Active（绿，mActive&amp;&amp;mRunning）/ Inactive（红），语义从进度条
 *     着色行化（GT6JadeRows.statusLine，gt6.jade.common.status.*）。</li>
 * <li><b>进度条</b>——mProgress/mMaxProgress（TileEntityBasicMachine.java:226-227），
 *     &gt;20t 折秒仿 GTCEu WorkableBlockProvider :72-77，文本统一三槽 X / Y (Z%)。</li>
 * <li><b>能量缓冲行</b>——文本 A 形 "Stored: %s %s"（mEnergy + p27 短码）——<b>不出条</b>
 *     （capacitor 半 getEnergyCapacity 未移植，无真上限；Jade 裁定例外条款）。</li>
 * <li><b>输入带行</b>——"Input: %s / %s / %s %s"（mInputMin/mInput/mInputMax :243 +
 *     类型短码第四槽）。</li>
 * <li><b>Malfunction 置换</b>——ERROR_MESSAGE 唯一来源 = tick 异常陷阱
 *     （TicksAndSync.java:223/228）：常态行 Malfunction（RED，仅非空时），原文截 64 字符进
 *     showDetails 潜行（{@link GT6JadeRows#malfunctionDetail}）。旧 ERROR 原文行退役。</li>
 * <li><b>多方块进度盲区补齐</b>——+TileEntityBase10MultiBlockMachine 分支进
 *     {@link #appendMachineData}（字段全同构实证 :166-175 mProgress/mMaxProgress/mParallel/
 *     mActive/mRunning/mEnergy/mInput 三元组/mEnergyTypeAccepted）——大涡轮/聚变/焦炉/
 *     精馏塔等现状只有 formed 行。formed/incomplete 行保留
 *     （TileEntityBase10MultiBlockBase.mStructureOkay :72）。mParallel 不显示（归 tooltip
 *     静态面 = 注册期数据）。</li>
 * </ol>
 *
 * <p>退役载荷：KEY_SUCCESSFUL（写而不读的死键，载荷最小化）。
 */
public final class GT6MachineProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {

	public static final GT6MachineProvider INSTANCE = new GT6MachineProvider();

	/** 同步键——GT6 前缀命名空间（appendServerData 的 tag 可能已被其他 provider 处理过）。 */
	public static final String KEY_PROGRESS = "GT6Progress";
	public static final String KEY_MAX_PROGRESS = "GT6MaxProgress";
	public static final String KEY_ACTIVE = "GT6Active";
	public static final String KEY_RUNNING = "GT6Running";
	public static final String KEY_ENERGY = "GT6Energy";
	/** The accepted-energy type short code (task p27-machine-energy-display-fix)——值见 {@link #energyTypeShortCode}。 */
	public static final String KEY_ENERGY_TYPE = "GT6EnergyType";
	public static final String KEY_INPUT_MIN = "GT6InputMin";
	public static final String KEY_INPUT = "GT6Input";
	public static final String KEY_INPUT_MAX = "GT6InputMax";
	public static final String KEY_STRUCTURE_OKAY = "GT6StructureOkay";
	/** Malfunction 标记键——存在即 Malfunction，值 = 原文（仅 {@link GT6JadeRows#malfunctionDetail} 潜行消费）。 */
	public static final String KEY_ERROR = "GT6Error";

	/**
	 * Tooltip 行键（机器域 gt6.jade.machine.*；公共域 gt6.jade.common.* 键在 {@link GT6JadeRows}）。
	 * 值面 = GT6EnUs {@code addMachineJade()} 行 + zh_cn_ref.tsv 直写带（py HAND 层）双落，
	 * zh 消费走 GT6ZhCn {@code addMachineJadeUnits()}。槽位语义见各 LANG_* 常量注。
	 */
	public static final String LANG_PROGRESS_SECONDS = "gt6.jade.machine.progress.seconds";
	/** 进度秒面行（三槽）：当前进度秒 / 最大进度秒（{@code %.1f} 预格式化串）/ 整数百分比。 */
	public static final String LANG_PROGRESS_TICKS = "gt6.jade.machine.progress.ticks";
	/** 进度 tick 面（&lt;20t，三槽）：当前进度 / 最大进度（long 直落）/ 整数百分比。 */
	public static final String LANG_ENERGY = "gt6.jade.machine.energy";
	/** 能量缓冲行（文本 A 形）：槽 = 载体量（long）+ 能量类型短码（缺键 "?"）。 */
	public static final String LANG_INPUT = "gt6.jade.machine.input";
	/** 输入带行（四槽）：min / in / max 三 long + 能量类型短码（KEY_INPUT_MIN/INPUT/INPUT_MAX 同序）。 */

	/**
	 * 能量载体的显示短码——task r10-debt-viewer-polish 起 Delegation 到 Jade-free 的正典家
	 * {@link GT6RecipeMapViewerMeta#energyTypeShortCode}（p27 短码表 verbatim 迁入该处；
	 * 依赖反转：viewer 面不再反向触碰本 Jade 集成类，nojade 运行时配方页不再
	 * NoClassDefFoundError，known_bugs r934_nojade_recipe_page_draw_ncdfe）。值面与 p27
	 * 逐字节一致（GT6MachineProviderTest 旧钉原样在守）。
	 */
	public static String energyTypeShortCode(TagData aType) {
		return gregtech6.jei.GT6RecipeMapViewerMeta.energyTypeShortCode(aType);
	}

	private GT6MachineProvider() {
	}

	/** Provider uid（IJadeProvider.java:10 双腿抽象 getUid）——gt6 域单值，形同 GTCEu super(GTCEu.id(...))。 */
	private static final ResourceLocation UID = new ResourceLocation("gt6", "machine_provider");

	@Override
	public ResourceLocation getUid() {
		return UID;
	}

	@Override
	public void appendServerData(CompoundTag aData, BlockAccessor aAccessor) {
		if (!(aAccessor.getBlockEntity() instanceof TileEntityBase01Root aRoot)) {
			return;
		}
		if (aRoot instanceof TileEntityBasicMachine aMachine) {
			// 单方块机器族字段（TileEntityBasicMachine.java:226-243）。
			appendMachineData(aData, aMachine);
		}
		if (aRoot instanceof TileEntityBase10MultiBlockMachine aMulti) {
			// 多方块机器族（r8 盲区补齐）：字段与 BasicMachine 全同构（:166-175 实证），
			// 同缝同键——大涡轮/聚变/焦炉/精馏塔自此带进度/状态/能量面。
			appendMachineData(aData, aMulti);
		}
		if (aRoot instanceof TileEntityBase10MultiBlockBase aMulti) {
			// 多方块成形态（TileEntityBase10MultiBlockBase.java:72，FORMED BlockState 的 BE 侧真源）。
			aData.putBoolean(KEY_STRUCTURE_OKAY, aMulti.mStructureOkay);
		}
		if (!aRoot.ERROR_MESSAGE.isEmpty()) {
			// Malfunction 标记（TileEntityBase01Root.java:88）——任何 GT6 BE 都可能带，非空才写；
			// 原文随键走（客户端潜行截断渲染，GT6JadeRows.MAX_DETAIL_CHARS）。
			aData.putString(KEY_ERROR, aRoot.ERROR_MESSAGE);
		}
	}

	/**
	 * 单方块机器族同步写（appendServerData 的静态缝——accessor 薄壳 live-only，BE 读面离线
	 * 可测）。p27 起能量类型短码随 {@link #KEY_ENERGY_TYPE} 上线。
	 */
	static void appendMachineData(CompoundTag aData, TileEntityBasicMachine aMachine) {
		writeMachineData(aData, aMachine.mProgress, aMachine.mMaxProgress, aMachine.mActive, aMachine.mRunning,
				aMachine.mEnergy, aMachine.mEnergyTypeAccepted,
				aMachine.mInputMin, aMachine.mInput, aMachine.mInputMax);
	}

	/**
	 * 多方块机器族同步写（r8 重设计新增分支）：字段全同构（TileEntityBase10MultiBlockMachine
	 * .java:166-175），同一静态缝同一键面——多方块进度盲区补齐的取数点。
	 */
	static void appendMachineData(CompoundTag aData, TileEntityBase10MultiBlockMachine aMachine) {
		writeMachineData(aData, aMachine.mProgress, aMachine.mMaxProgress, aMachine.mActive, aMachine.mRunning,
				aMachine.mEnergy, aMachine.mEnergyTypeAccepted,
				aMachine.mInputMin, aMachine.mInput, aMachine.mInputMax);
	}

	/** 同缝写体（两 concrete 分发的唯一落点——键面单点，载荷最小化：无 SUCCESSFUL 死键）。 */
	private static void writeMachineData(CompoundTag aData, long aProgress, long aMaxProgress, boolean aActive,
			boolean aRunning, long aEnergy, TagData aEnergyType, long aInputMin, long aInput, long aInputMax) {
		aData.putLong(KEY_PROGRESS, aProgress);
		aData.putLong(KEY_MAX_PROGRESS, aMaxProgress);
		aData.putBoolean(KEY_ACTIVE, aActive);
		aData.putBoolean(KEY_RUNNING, aRunning);
		aData.putLong(KEY_ENERGY, aEnergy);
		aData.putString(KEY_ENERGY_TYPE, energyTypeShortCode(aEnergyType));
		aData.putLong(KEY_INPUT_MIN, aInputMin);
		aData.putLong(KEY_INPUT, aInput);
		aData.putLong(KEY_INPUT_MAX, aInputMax);
	}

	@Override
	public void appendTooltip(ITooltip aTooltip, BlockAccessor aAccessor, IPluginConfig aConfig) {
		CompoundTag aData = aAccessor.getServerData();
		// ① 状态行 + ② 进度条 + ③ 能量/输入带：机器族载荷（键存在即机器——BasicMachine 与
		// MultiBlockMachine 同键面，r8 起两族共用）。
		if (aData.contains(KEY_MAX_PROGRESS)) {
			aTooltip.add(GT6JadeRows.statusLine(
					aData.getBoolean(KEY_ACTIVE) && aData.getBoolean(KEY_RUNNING)));
			long tProgress = aData.getLong(KEY_PROGRESS);
			long tMaxProgress = aData.getLong(KEY_MAX_PROGRESS);
			if (tMaxProgress > 0) {
				// >20t 折秒（GTCEu :72-77 形），<20t 显示 tick；状态语义已行化，条面中性白。
				GT6JadeRows.bar(aTooltip, GT6JadeRows.ratio(tProgress, tMaxProgress),
						progressLine(tProgress, tMaxProgress), GT6JadeRows.COLOR_NEUTRAL);
			}
			if (aData.contains(KEY_ENERGY)) {
				String tCode = aData.contains(KEY_ENERGY_TYPE) ? aData.getString(KEY_ENERGY_TYPE) : "?";
				aTooltip.add(energyLine(aData.getLong(KEY_ENERGY), tCode));
				aTooltip.add(inputLine(aData.getLong(KEY_INPUT_MIN), aData.getLong(KEY_INPUT),
						aData.getLong(KEY_INPUT_MAX), tCode));
			}
		}
		// ④ 多方块成形态：文本行（MultiBlockMachine extends MultiBlockBase，两键面共存）。
		if (aData.contains(KEY_STRUCTURE_OKAY)) {
			aTooltip.add(GT6JadeRows.structureLine(aData.getBoolean(KEY_STRUCTURE_OKAY)));
		}
		// ⑤ Malfunction 置换：常态行 RED 仅非空时；原文截 64 字符进潜行（不刷主面）。
		if (aData.contains(KEY_ERROR)) {
			aTooltip.add(GT6JadeRows.malfunctionLine());
			if (aAccessor.showDetails()) {
				aTooltip.add(GT6JadeRows.malfunctionDetail(aData.getString(KEY_ERROR)));
			}
		}
	}

	/**
	 * 进度行（纯函数离线面，三槽统一 X / Y (Z%)）：&gt;20t 折秒（{@code %.1f} 预格式化，
	 * Locale.ROOT 钉死），否则 tick；百分比整除钳 0..100（{@link GT6JadeRows#percent}）。
	 */
	public static Component progressLine(long aProgress, long aMaxProgress) {
		long tPercent = GT6JadeRows.percent(aProgress, aMaxProgress);
		return aMaxProgress >= 20
				? Component.translatable(LANG_PROGRESS_SECONDS,
						String.format(Locale.ROOT, "%.1f", aProgress / 20.0),
						String.format(Locale.ROOT, "%.1f", aMaxProgress / 20.0), tPercent)
				: Component.translatable(LANG_PROGRESS_TICKS, aProgress, aMaxProgress, tPercent);
	}

	/** 能量缓冲行（文本 A 形）：量 + 类型短码（缺键 "?" 由调用侧供给）。 */
	public static Component energyLine(long aEnergy, String aTypeShortCode) {
		return Component.translatable(LANG_ENERGY, aEnergy, aTypeShortCode);
	}

	/** 输入带行（四槽）：min / in / max 同序三槽 + 类型短码。 */
	public static Component inputLine(long aInputMin, long aInput, long aInputMax, String aTypeShortCode) {
		return Component.translatable(LANG_INPUT, aInputMin, aInput, aInputMax, aTypeShortCode);
	}

}
