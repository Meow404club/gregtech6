package gregtech6.jade;

import java.util.Locale;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.BoxStyle;
import snownee.jade.api.ui.IElementHelper;

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
 * 出而暂不闸色）、能量行（mEnergy+输入带 mInputMin/mInput/mInputMax :226，RU/KU 载体注明）、
 * 错误行（ERROR_MESSAGE 非空才显示）、多方块成形态（mStructureOkay 文本行，
 * TileEntityBase10MultiBlockBase.java:72）。lang 键入池——v1 全 Component.literal。
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
	public static final String KEY_INPUT_MIN = "GT6InputMin";
	public static final String KEY_INPUT = "GT6Input";
	public static final String KEY_INPUT_MAX = "GT6InputMax";
	public static final String KEY_STRUCTURE_OKAY = "GT6StructureOkay";
	public static final String KEY_ERROR = "GT6Error";

	/** 着色两值 = GTCEu WorkableBlockProvider.java:80 字面（绿 0xFF4CBB17 / 红 0xFFBB1C28）。 */
	private static final int COLOR_OK = 0xFF4CBB17;
	private static final int COLOR_STALLED = 0xFFBB1C28;

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
			aData.putLong(KEY_PROGRESS, aMachine.mProgress);
			aData.putLong(KEY_MAX_PROGRESS, aMachine.mMaxProgress);
			aData.putBoolean(KEY_SUCCESSFUL, aMachine.mSuccessful);
			aData.putBoolean(KEY_ACTIVE, aMachine.mActive);
			aData.putBoolean(KEY_RUNNING, aMachine.mRunning);
			aData.putLong(KEY_ENERGY, aMachine.mEnergy);
			aData.putLong(KEY_INPUT_MIN, aMachine.mInputMin);
			aData.putLong(KEY_INPUT, aMachine.mInput);
			aData.putLong(KEY_INPUT_MAX, aMachine.mInputMax);
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

	@Override
	public void appendTooltip(ITooltip aTooltip, BlockAccessor aAccessor, IPluginConfig aConfig) {
		CompoundTag aData = aAccessor.getServerData();
		// ① 进度条：>20t 折秒（GTCEu :72-77 形），<20t 显示 tick；mActive&&mRunning 绿 / 否则红。
		if (aData.contains(KEY_MAX_PROGRESS) && aData.getLong(KEY_MAX_PROGRESS) > 0) {
			long tProgress = aData.getLong(KEY_PROGRESS);
			long tMaxProgress = aData.getLong(KEY_MAX_PROGRESS);
			float tRatio = (float) Math.max(0.0, Math.min(1.0, (double) tProgress / (double) tMaxProgress));
			Component tText = tMaxProgress >= 20
					? Component.literal(String.format(Locale.ROOT, "Progress: %.1f / %.1f s", tProgress / 20.0, tMaxProgress / 20.0))
					: Component.literal(String.format(Locale.ROOT, "Progress: %d / %d t", tProgress, tMaxProgress));
			int tColor = aData.getBoolean(KEY_ACTIVE) && aData.getBoolean(KEY_RUNNING) ? COLOR_OK : COLOR_STALLED;
			// helper 取法：IElementHelper.get() 静态双腿同形（IElementHelper.java:14）——1.20.1 的
			// ITooltip.getElementHelper()（ITooltip.java:124）在 1.21.1 已删除，不能作桥。
			IElementHelper tHelper = IElementHelper.get();
			aTooltip.add(tHelper.progress(
					tRatio,
					tText,
					// "color" 后的空格 = 刻意锚：chisel 表有 VertexConsumer 1.21 改名条目
					// （stonecutter.gradle.kts:229，color→setColor 裸正则，不在本卡 FILES_SCOPE），
					// 对 Jade 双腿同名同形的 style color(int) 属误改写——空格分隔即不命中。
					tHelper.progressStyle().color (tColor).textColor(-1),
					jadeBox(),
					true));
		}
		// ② 能量行 + 输入带：mEnergy 是 RU/KU 双载体（accepted-energy carrier，机器注册时择一），v1 注明不细分。
		if (aData.contains(KEY_ENERGY)) {
			aTooltip.add(Component.literal(String.format(Locale.ROOT, "Energy: %d (RU/KU)", aData.getLong(KEY_ENERGY))));
			aTooltip.add(Component.literal(String.format(Locale.ROOT, "Input: %d / %d / %d (min/in/max)",
					aData.getLong(KEY_INPUT_MIN), aData.getLong(KEY_INPUT), aData.getLong(KEY_INPUT_MAX))));
		}
		// ③ 多方块成形态：文本行（v1 不做结构图示）。
		if (aData.contains(KEY_STRUCTURE_OKAY)) {
			aTooltip.add(aData.getBoolean(KEY_STRUCTURE_OKAY)
					? Component.literal("Multiblock: formed").withStyle(ChatFormatting.GREEN)
					: Component.literal("Multiblock: incomplete").withStyle(ChatFormatting.RED));
		}
		// ④ 错误行：非空才显示（服务端已过滤）。
		if (aData.contains(KEY_ERROR)) {
			aTooltip.add(Component.literal("Error: " + aData.getString(KEY_ERROR)).withStyle(ChatFormatting.RED));
		}
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
