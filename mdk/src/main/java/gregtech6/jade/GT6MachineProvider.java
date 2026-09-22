package gregtech6.jade;

import java.util.Locale;
import java.util.Map;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.BoxStyle;
import snownee.jade.api.ui.IElementHelper;

import gregapi.code.TagData;
import gregapi.data.TD;
import gregtech6.tileentity.TileEntityBase01Root;
import gregtech6.tileentity.machines.TileEntityBasicMachine;
import gregtech6.tileentity.multiblocks.TileEntityBase10MultiBlockBase;

/**
 * GT6 机器 Jade provider 对（task p21-jade-compat）：一个实例同挂 client tooltip
 * （{@link IBlockComponentProvider}）+ server 数据同步（{@link IServerDataProvider}&lt;BlockAccessor&gt;），
 * 形源 GTCEu WorkableBlockProvider.java:37-89（该卡经 capability 层，本仓无 capability 缝合、
 * 服务端直读 BE 公开字段——研究卡 hook_points 裁定）。
 *
 * <p>同步契约：appendServerData 写 GT6* 前缀键（与 Jade 自身/他 mod 的键空间隔离，tag 由双腿
 * Jade 网络层搬运，addon 零接触），appendTooltip 经 accessor.getServerData() 读回
 * （Accessor.getServerData jade-1201:25 / jade-1211:33 双腿 @NotNull 同形）。只读公开字段，
 * 禁改 BE（铁律）。
 *
 * <p>v1 展示面（arch 卡四项）：进度条（mProgress/mMaxProgress，TileEntityBasicMachine.java:227，
 * &gt;20t 折秒仿 GTCEu :72-77；着色 mActive&amp;&amp;mRunning 绿/否则红，仿 :80，mSuccessful 随 tag
 * 出而暂不闸色）、能量行（mEnergy+输入带 mInputMin/mInput/mInputMax :226，能量类型短码随
 * KEY_ENERGY_TYPE 传真类型——p27-machine-energy-display-fix 起不再是 "RU/KU" 并列字面量）、
 * 错误行（ERROR_MESSAGE 非空才显示）、多方块成形态（mStructureOkay 文本行，
 * TileEntityBase10MultiBlockBase.java:72）。lang 键入池（task p34-hygiene-lang）——全部行
 * translatable（{@code gt6.jade.machine.*}，en/zh 双面 = datagen 行 + tsv 直写带双落），
 * 零裸 literal（GT6JadeTooltipKeyPinTest 钉住）。
 *
 * <p>与 arch 卡"零分叉"预期的两处偏离（编译实证，见方法内注）：
 * ① 进度条盒形真分叉——1.20.1 BoxStyle.java:12 public static final DEFAULT 字段 vs 1.21.1
 * BoxStyle.java:61 abstract 类仅 static getNestedBox() 工厂（Jade 自家 EnergyStorageProvider.java:116
 * 即后者形），收敛在 {@link #jadeBox()} 一点 swap；② helper 取法——1.20.1 ITooltip.getElementHelper()
 * （ITooltip.java:124）1.21.1 已删除，改用双腿同形的 IElementHelper.get() 静态（无需 swap）。
 * 另：style color(int) 双腿同名同形，但会撞 chisel VertexConsumer 改名条目（表项裸正则，
 * stonecutter.gradle.kts:229，不在本卡 FILES_SCOPE）——空格锚规避，见调用点注。
 */
public final class GT6MachineProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {

	public static final GT6MachineProvider INSTANCE = new GT6MachineProvider();

	/** 同步键——GT6 前缀命名空间（appendServerData 的 tag 可能已被其他 provider 处理过）。 */
	public static final String KEY_PROGRESS = "GT6Progress";
	public static final String KEY_MAX_PROGRESS = "GT6MaxProgress";
	public static final String KEY_SUCCESSFUL = "GT6Successful";
	public static final String KEY_ACTIVE = "GT6Active";
	public static final String KEY_RUNNING = "GT6Running";
	public static final String KEY_ENERGY = "GT6Energy";
	/** The accepted-energy type short code (task p27-machine-energy-display-fix)——值见 {@link #energyTypeShortCode}。 */
	public static final String KEY_ENERGY_TYPE = "GT6EnergyType";
	public static final String KEY_INPUT_MIN = "GT6InputMin";
	public static final String KEY_INPUT = "GT6Input";
	public static final String KEY_INPUT_MAX = "GT6InputMax";
	public static final String KEY_STRUCTURE_OKAY = "GT6StructureOkay";
	public static final String KEY_ERROR = "GT6Error";

	/**
	 * Tooltip 行键（task p34-hygiene-lang——v1 的 literal 带退役）：值面 = GT6EnUs
	 * {@code addMachineJade()} 行 + zh_cn_ref.tsv 直写带（py HAND 层）双落，zh 消费走
	 * GT6ZhCn {@code addMachineJadeUnits()}。槽位语义见各 LANG_* 常量注。
	 */
	public static final String LANG_PROGRESS_SECONDS = "gt6.jade.machine.progress.seconds";
	/** 进度秒面行：槽 = 当前进度秒 / 最大进度秒（{@code %.1f} 预格式化串）。 */
	public static final String LANG_PROGRESS_TICKS = "gt6.jade.machine.progress.ticks";
	/** 进度 tick 面（&lt;20t）：槽 = 当前进度 / 最大进度（long 直落）。 */
	public static final String LANG_ENERGY = "gt6.jade.machine.energy";
	/** 能量行：槽 = 载体量（long）+ 能量类型短码（{@link #energyTypeShortCode} 面，缺键 "?"）。 */
	public static final String LANG_INPUT = "gt6.jade.machine.input";
	/** 输入带行：槽 = min / in / max 三 long（KEY_INPUT_MIN/INPUT/INPUT_MAX 同序）。 */
	public static final String LANG_STRUCTURE_FORMED = "gt6.jade.machine.multiblock.formed";
	public static final String LANG_STRUCTURE_INCOMPLETE = "gt6.jade.machine.multiblock.incomplete";
	/** 多方块成形态两态行（GREEN/RED 着色在 {@link #structureLine}）。 */
	public static final String LANG_ERROR = "gt6.jade.machine.error";
	/** 错误行：槽 = 服务端 {@link TileEntityBase01Root#ERROR_MESSAGE} 原文。 */

	/** 着色两值 = GTCEu WorkableBlockProvider.java:80 字面（绿 0xFF4CBB17 / 红 0xFFBB1C28）。 */
	private static final int COLOR_OK = 0xFF4CBB17;
	private static final int COLOR_STALLED = 0xFFBB1C28;

	/**
	 * 能量类型短码表（task p27-machine-energy-display-fix）：accepted-energy 载体在
	 * {@code mEnergyTypeAccepted}（TileEntityBasicMachine.java:254，注册行 :1294-1309 择一），
	 * 但 Jade 同步面只搬 NBT 标量，而 {@link TagData#mName} 不是显示名——移植把名字全大写折叠、
	 * 丢弃 LH 短/长本地名（root TagData.java:68-71 createTagData 丢 aLocalShort/aLocalLong，
	 * mName="ENERGY.RU" :88），短码按研究卡裁定落本 provider 侧映射（root 不动）。恒等查找安全：
	 * createTagData 按名去重（TagData.java:77-81），TD.Energy 常量即单例。短码值 = 上游
	 * aLocalShort 字面 verbatim（root TD.java:81/:88/:95/:102/:109/:116/:123/:130/:137/:144/
	 * :151/:158/:165/:172/:175-185——被丢弃的实参仍原样在盘）。
	 */
	private static final Map<TagData, String> ENERGY_SHORT_CODES = Map.ofEntries(
			Map.entry(TD.Energy.EU, "EU"),           // TD.java:81 ELECTRICITY（Canner 电机族）
			Map.entry(TD.Energy.RU, "RU"),           // :88 KINETIC_ROTATION（Shredder/Lathe/Wiremill）
			Map.entry(TD.Energy.KU, "KU"),           // :95 KINETIC_PUSH（Crusher/Sifter/Compressor/Press）
			Map.entry(TD.Energy.HU, "HU"),           // :102 HEAT（Oven/Dryer/Extruder/Distillery）
			Map.entry(TD.Energy.CU, "CU"),           // :109 CRYO
			Map.entry(TD.Energy.LU, "LU"),           // :116 LIGHT
			Map.entry(TD.Energy.MU, "MU"),           // :123 MAGNETIC
			Map.entry(TD.Energy.NU, "NU"),           // :130 NEUTRON
			Map.entry(TD.Energy.QU, "QU"),           // :137 QUANTUM
			Map.entry(TD.Energy.TU, "TU"),           // :144 TIME（:254 字段默认）
			Map.entry(TD.Energy.RF, "RF"),           // :151 REDSTONE_FLUX
			Map.entry(TD.Energy.MJ, "MJ"),           // :158 MINECRAFT_JOULES
			Map.entry(TD.Energy.STEAM, "Steam"),     // :165（上游短名是词不是字头）
			Map.entry(TD.Energy.AU, "AU"),           // :172 AIR
			Map.entry(TD.Energy.VIS_ORDO, "Ordo"),       // :175
			Map.entry(TD.Energy.VIS_AER, "Aer"),         // :177
			Map.entry(TD.Energy.VIS_AQUA, "Aqua"),       // :179
			Map.entry(TD.Energy.VIS_TERRA, "Terra"),     // :181
			Map.entry(TD.Energy.VIS_IGNIS, "Ignis"),     // :183
			Map.entry(TD.Energy.VIS_PERDITIO, "Perditio")); // :185

	/**
	 * 能量载体的显示短码（{@link #ENERGY_SHORT_CODES} 查找；回退 = 折叠 mName 剥 "ENERGY."
	 * 前缀——大写、无参数（TagData.java:69-71 折叠语义），兜住映射未及的未来载体。
	 */
	public static String energyTypeShortCode(TagData aType) {
		String tCode = aType == null ? null : ENERGY_SHORT_CODES.get(aType);
		if (tCode != null) return tCode;
		String tName = aType == null ? "" : aType.mName;
		return tName.startsWith("ENERGY.") ? tName.substring("ENERGY.".length()) : tName;
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
			// 机器族字段（TileEntityBasicMachine.java:226-227）：进度 + 运行态 + 能量/输入带。
			appendMachineData(aData, aMachine);
		}
		if (aRoot instanceof TileEntityBase10MultiBlockBase aMulti) {
			// 多方块成形态（TileEntityBase10MultiBlockBase.java:72，FORMED BlockState 的 BE 侧真源）。
			aData.putBoolean(KEY_STRUCTURE_OKAY, aMulti.mStructureOkay);
		}
		if (!aRoot.ERROR_MESSAGE.isEmpty()) {
			// 错误行（TileEntityBase01Root.java:88）——任何 GT6 BE 都可能带，非空才写。
			aData.putString(KEY_ERROR, aRoot.ERROR_MESSAGE);
		}
	}

	/**
	 * 机器族同步写（appendServerData 的 BasicMachine 分支体抽成静态缝——GT6FluidProvider
	 * .groupsOfTarget 同 posture：accessor 薄壳 live-only，BE 读面离线可测）。p27 起能量类型
	 * 短码随 {@link #KEY_ENERGY_TYPE} 上线——服务端读 {@code mEnergyTypeAccepted} 真源
	 * （:254），客户端 tooltip 不再并列猜 "RU/KU"。
	 */
	static void appendMachineData(CompoundTag aData, TileEntityBasicMachine aMachine) {
		aData.putLong(KEY_PROGRESS, aMachine.mProgress);
		aData.putLong(KEY_MAX_PROGRESS, aMachine.mMaxProgress);
		aData.putBoolean(KEY_SUCCESSFUL, aMachine.mSuccessful);
		aData.putBoolean(KEY_ACTIVE, aMachine.mActive);
		aData.putBoolean(KEY_RUNNING, aMachine.mRunning);
		aData.putLong(KEY_ENERGY, aMachine.mEnergy);
		aData.putString(KEY_ENERGY_TYPE, energyTypeShortCode(aMachine.mEnergyTypeAccepted));
		aData.putLong(KEY_INPUT_MIN, aMachine.mInputMin);
		aData.putLong(KEY_INPUT, aMachine.mInput);
		aData.putLong(KEY_INPUT_MAX, aMachine.mInputMax);
	}

	@Override
	public void appendTooltip(ITooltip aTooltip, BlockAccessor aAccessor, IPluginConfig aConfig) {
		CompoundTag aData = aAccessor.getServerData();
		// ① 进度条：>20t 折秒（GTCEu :72-77 形），<20t 显示 tick；mActive&&mRunning 绿 / 否则红。
		if (aData.contains(KEY_MAX_PROGRESS) && aData.getLong(KEY_MAX_PROGRESS) > 0) {
			long tProgress = aData.getLong(KEY_PROGRESS);
			long tMaxProgress = aData.getLong(KEY_MAX_PROGRESS);
			float tRatio = (float) Math.max(0.0, Math.min(1.0, (double) tProgress / (double) tMaxProgress));
			Component tText = progressLine(tProgress, tMaxProgress);
			int tColor = aData.getBoolean(KEY_ACTIVE) && aData.getBoolean(KEY_RUNNING) ? COLOR_OK : COLOR_STALLED;
			// helper 取法：IElementHelper.get() 静态双腿同形（IElementHelper.java:14）——1.20.1 的
			// ITooltip.getElementHelper()（ITooltip.java:124）在 1.21.1 已删除，不能作桥。
			IElementHelper tHelper = IElementHelper.get();
			aTooltip.add(tHelper.progress(
					tRatio,
					tText,
					// style color(int) 双腿同名同形（Jade 侧 API）——p22 收窄后 swap 表单参
					// .color( 条目已删（stonecutter.gradle.kts 只剩 4 参逗号形），裸写双腿编译
					// 绿；P21 的 ".color (tColor)" 空格锚随收窄归一，本行即收窄生效的活体证明。
					tHelper.progressStyle().color(tColor).textColor(-1),
					jadeBox(),
					true));
		}
		// ② 能量行 + 输入带：mEnergy 是 accepted-energy carrier（机器注册时择一），p27 起类型
		// 短码随 KEY_ENERGY_TYPE 同步（缺键防御 "?"——旧缓存 tag 不渲染空括号）。
		if (aData.contains(KEY_ENERGY)) {
			aTooltip.add(energyLine(aData.getLong(KEY_ENERGY),
					aData.contains(KEY_ENERGY_TYPE) ? aData.getString(KEY_ENERGY_TYPE) : "?"));
			aTooltip.add(inputLine(aData.getLong(KEY_INPUT_MIN), aData.getLong(KEY_INPUT), aData.getLong(KEY_INPUT_MAX)));
		}
		// ③ 多方块成形态：文本行（v1 不做结构图示）。
		if (aData.contains(KEY_STRUCTURE_OKAY)) {
			aTooltip.add(structureLine(aData.getBoolean(KEY_STRUCTURE_OKAY)));
		}
		// ④ 错误行：非空才显示（服务端已过滤）。
		if (aData.contains(KEY_ERROR)) {
			aTooltip.add(errorLine(aData.getString(KEY_ERROR)));
		}
	}

	/** 进度行（纯函数离线面）：&gt;20t 折秒（{@code %.1f} 预格式化，Locale.ROOT 钉死），否则 tick。 */
	public static Component progressLine(long aProgress, long aMaxProgress) {
		return aMaxProgress >= 20
				? Component.translatable(LANG_PROGRESS_SECONDS,
						String.format(Locale.ROOT, "%.1f", aProgress / 20.0),
						String.format(Locale.ROOT, "%.1f", aMaxProgress / 20.0))
				: Component.translatable(LANG_PROGRESS_TICKS, aProgress, aMaxProgress);
	}

	/** 能量行：量 + 类型短码（缺键 "?" 由调用侧供给）。 */
	public static Component energyLine(long aEnergy, String aTypeShortCode) {
		return Component.translatable(LANG_ENERGY, aEnergy, aTypeShortCode);
	}

	/** 输入带行：min / in / max 同序三槽。 */
	public static Component inputLine(long aInputMin, long aInput, long aInputMax) {
		return Component.translatable(LANG_INPUT, aInputMin, aInput, aInputMax);
	}

	/** 多方块成形态行（纯函数离线面）：formed GREEN / incomplete RED。 */
	public static Component structureLine(boolean aFormed) {
		MutableComponent rLine = Component.translatable(aFormed ? LANG_STRUCTURE_FORMED : LANG_STRUCTURE_INCOMPLETE);
		return rLine.withStyle(aFormed ? ChatFormatting.GREEN : ChatFormatting.RED);
	}

	/** 错误行（整行 RED）。 */
	public static Component errorLine(String aError) {
		return Component.translatable(LANG_ERROR, aError).withStyle(ChatFormatting.RED);
	}

	/**
	 * 进度条盒形——本卡唯一 swap 点（类 doc 引证①）：双腿 API 各无对方常量/方法，
	 * 编译期强制二选一。
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
