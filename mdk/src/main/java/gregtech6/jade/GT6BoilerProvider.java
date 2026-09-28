package gregtech6.jade;

import java.util.List;

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
import gregtech6.tileentity.energy.converters.GTBoilerTankBlockEntity;
import gregtech6.tileentity.multiblocks.TileEntityLargeBoiler;

/**
 * GT6 锅炉族 Jade 显示面（task r5-jade-boiler，issue #17 观感死机教训）：单方块锅炉罐
 * {@link GTBoilerTankBlockEntity} 与大型锅炉 {@link TileEntityLargeBoiler} 同一 provider
 * 同一格式——两 BE 无共享内容接口（各自直接 implements ITileEntityEnergy、字段形同构
 * mEnergy/mCapacity/mOutput/mEfficiency/mTanks）→ {@code appendServerData} 双 concrete
 * instanceof 分发进同一静态缝（{@link GT6CrucibleProvider} :109-115 形）。单体姿势 =
 * client tooltip + server data 双角色对（{@link GT6MachineProvider} 同形）。
 *
 * <p>此前两 BE 对 Jade 不可见（非 BasicMachine 非 MultiBlockBase，只落 Root 的
 * ERROR_MESSAGE 行）——热量没有读数、蒸汽罐过半前不输出（上游 :139-142/:203-207 行为）
 * 也无解释，用户看到的就是"锅炉好像没在干活"（issue #17）。本面定位 = 现代增强
 * （research.r5-jade-integration 2026-09-27 主会话矫正：非上游 parity；上游 1.7.10 零
 * WAILA 面，措辞借上游工具读数原文）：
 * <ol>
 * <li><b>热量条</b>——progress(mEnergy/mCapacity)，"Stored Heat Units: %s / %s HU" 借上游
 *     温度计读数 verbatim（MultiTileEntityBoilerTank.java:182 = 移植
 *     GTBoilerTankBlockEntity.thermometer :408-411；LargeBoiler :588-590 同文）；热量 &gt; 0
 *     绿 / 空红（{@link GT6MachineProvider#COLOR_OK} 同字面——有热 = 在干活）。</li>
 * <li><b>需求行</b>——"Demand: %s HU/t"，值 = mOutput/2（getEnergyDemanded，
 *     GTBoilerTankBlockEntity :458-460 = 上游 MultiTileEntityBoilerTank :253 verbatim）。</li>
 * <li><b>半满输出门提示</b>（纯现代新增，用户点名）——蒸汽未过半罐：输出门未开 + 过半后从
 *     顶面输出（上游纯行为无文本，:281-287/:139-142；&gt;3/4 双倍率说明并入已开态文案）。</li>
 * <li><b>潜行明细</b>（{@code showDetails()} 门——Accessor.java:36/:54 双腿同名，Jade 潜行
 *     键按下才显，不刷屏）——水垢% + 缺水警告，措辞借上游放大镜读数 verbatim
 *     （magnifyingglass :414-421/:593-599：Calcification/Water/WARNING: NO WATER!!!）。</li>
 * </ol>
 *
 * <p>同步契约同 {@link GT6MachineProvider}：appendServerData 写 GT6* 前缀键，appendTooltip 经
 * {@code accessor.getServerData()} 读回，只读公开字段禁改 BE（铁律）。capability 面不含
 * 热量/汽量（研究卡 risks ①）→ 服务端直读字段经 Jade block data 同步，本 provider 即缓存。
 * 非锅炉 BE 挂同一 {@link TileEntityBase01Root} 注册面（GT6JadePlugin 追加对），键存在即
 * 锅炉族（{@code KEY_HEAT_MAX} 门，GT6CrucibleProvider :180-182 同门）。lang 键
 * {@code gt6.jade.boiler.*}（GT6EnUs addBoilerJade + zh_cn_ref.tsv hand 带 + GT6ZhCn
 * addBoilerJadeUnits 三落——p34 棘轮形），零裸 literal。
 */
public final class GT6BoilerProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {

	public static final GT6BoilerProvider INSTANCE = new GT6BoilerProvider();

	/** 同步键——GT6 前缀命名空间（GT6MachineProvider.java:58 同契约）。 */
	public static final String KEY_HEAT = "GT6BoilerHeat";
	public static final String KEY_HEAT_MAX = "GT6BoilerHeatMax";
	public static final String KEY_DEMAND = "GT6BoilerDemand";
	public static final String KEY_STEAM = "GT6BoilerSteam";
	public static final String KEY_STEAM_MAX = "GT6BoilerSteamMax";
	public static final String KEY_EFFICIENCY = "GT6BoilerEfficiency";
	public static final String KEY_WATER = "GT6BoilerWater";
	public static final String KEY_WATER_MAX = "GT6BoilerWaterMax";

	/** lang 键（GT6EnUs/GT6ZhCn 双侧同发）。 */
	public static final String LANG_HEAT = "gt6.jade.boiler.heat";
	/** 热量行：槽 = 现储热量 / 热量上限（两 long，上游温度计读数形）。 */
	public static final String LANG_DEMAND = "gt6.jade.boiler.demand";
	/** 需求行：槽 = mOutput/2（HU/t，getEnergyDemanded 值）。 */
	public static final String LANG_GATE_BELOW = "gt6.jade.boiler.gate.below";
	/** 输出门未开态：槽 = 汽量 / 汽罐容量（两 long）。 */
	public static final String LANG_GATE_ABOVE = "gt6.jade.boiler.gate.above";
	/** 输出门已开态（无槽——紧凑形，含 &gt;3/4 双倍率说明）。 */
	public static final String LANG_SCALE = "gt6.jade.boiler.scale";
	/** 水垢行：槽 = 水垢百分比（(10000-mEfficiency)/100，上游 LH.percent 形）。 */
	public static final String LANG_SCALE_CLEAN = "gt6.jade.boiler.scale.clean";
	public static final String LANG_WATER = "gt6.jade.boiler.water";
	/** 水量行：槽 = 水量 / 水罐容量（两 long）。 */
	public static final String LANG_NO_WATER = "gt6.jade.boiler.no_water";
	/** 缺水警告行（整行 RED——上游 "WARNING: NO WATER!!!" 的 Jade 面，无槽）。 */

	/** 着色两值 = GT6MachineProvider.java:92 字面（绿 0xFF4CBB17 / 红 0xFFBB1C28）。 */
	private static final int COLOR_OK = 0xFF4CBB17;
	private static final int COLOR_STALLED = 0xFFBB1C28;

	/** Provider uid（GT6MachineProvider.java:143 同形——双腿 ctor swap 由 stonecutter 表消化）。 */
	private static final ResourceLocation UID = new ResourceLocation("gt6", "boiler_provider");

	private GT6BoilerProvider() {
	}

	@Override
	public ResourceLocation getUid() {
		return UID;
	}

	@Override
	public void appendServerData(CompoundTag aData, BlockAccessor aAccessor) {
		// 两锅炉 BE 无共享内容接口（各自 implements ITileEntityEnergy）——双 concrete instanceof
		// 汇进同一静态缝，字段形严格同构（GT6CrucibleProvider.appendServerData 同姿势）。
		if (aAccessor.getBlockEntity() instanceof GTBoilerTankBlockEntity aBoiler) {
			writeBoilerData(aData, aBoiler.mEnergy, aBoiler.mCapacity, aBoiler.mOutput,
					aBoiler.mTanks[1].amount(), aBoiler.mTanks[1].capacity(),
					aBoiler.mEfficiency, aBoiler.mTanks[0].amount(), aBoiler.mTanks[0].capacity());
		} else if (aAccessor.getBlockEntity() instanceof TileEntityLargeBoiler aBoiler) {
			writeBoilerData(aData, aBoiler.mEnergy, aBoiler.mCapacity, aBoiler.mOutput,
					aBoiler.mTanks[1].amount(), aBoiler.mTanks[1].capacity(),
					aBoiler.mEfficiency, aBoiler.mTanks[0].amount(), aBoiler.mTanks[0].capacity());
		}
	}

	/**
	 * 锅炉族同步写（appendServerData 的静态缝——GT6CrucibleProvider.writeCrucibleData 同姿势：
	 * accessor 薄壳 live-only，纯值离线可测）。需求 = aOutput/2 服务端定死（getEnergyDemanded
	 * 返回值面，:458-460），客户端不做二次推导。
	 */
	public static void writeBoilerData(CompoundTag aData, long aHeat, long aHeatMax, long aOutput, long aSteam,
			long aSteamMax, int aEfficiency, long aWater, long aWaterMax) {
		aData.putLong(KEY_HEAT, aHeat);
		aData.putLong(KEY_HEAT_MAX, aHeatMax);
		aData.putLong(KEY_DEMAND, aOutput / 2); // :253/:371 mOutput/2
		aData.putLong(KEY_STEAM, aSteam);
		aData.putLong(KEY_STEAM_MAX, aSteamMax);
		aData.putShort(KEY_EFFICIENCY, (short)aEfficiency); // ten-thousandths, 5000 floor
		aData.putLong(KEY_WATER, aWater);
		aData.putLong(KEY_WATER_MAX, aWaterMax);
	}

	@Override
	public void appendTooltip(ITooltip aTooltip, BlockAccessor aAccessor, IPluginConfig aConfig) {
		CompoundTag aData = aAccessor.getServerData();
		if (!aData.contains(KEY_HEAT_MAX)) {
			return; // 非锅炉（本 provider 挂全 GT6 BE 面，键存在即锅炉族——GT6CrucibleProvider 同门）
		}
		// ① 热量条：mEnergy/mCapacity 比例，"Stored Heat Units: X / Y HU"；有热绿 / 空红。
		long tHeat = aData.getLong(KEY_HEAT);
		long tHeatMax = aData.getLong(KEY_HEAT_MAX);
		int tColor = tHeat > 0 ? COLOR_OK : COLOR_STALLED;
		// IElementHelper.get() 静态双腿同形（GT6MachineProvider.java:198-209 在产形）。
		IElementHelper tHelper = IElementHelper.get();
		aTooltip.add(tHelper.progress(
				heatRatio(tHeat, tHeatMax),
				heatLine(tHeat, tHeatMax),
				tHelper.progressStyle().color(tColor).textColor(-1),
				jadeBox(),
				true));
		// ② 需求行。
		aTooltip.add(demandLine(aData.getLong(KEY_DEMAND)));
		// ③ 半满输出门提示（两态——上游 :139-142/:203-207 行为的现代文案）。
		aTooltip.add(gateLine(aData.getLong(KEY_STEAM), aData.getLong(KEY_STEAM_MAX)));
		// ④ 潜行明细（showDetails 门——不刷屏，潜行才显）：水垢% + 缺水警告。
		if (aAccessor.showDetails()) {
			for (Component tLine : detailLines(aData.getShort(KEY_EFFICIENCY), aData.getLong(KEY_WATER),
					aData.getLong(KEY_WATER_MAX))) {
				aTooltip.add(tLine);
			}
		}
	}

	/**
	 * 潜行明细带（{@code showDetails()} 门的静态缝——appendTooltip 的门控本体是 Jade 的
	 * {@code Accessor.showDetails()}，本缝钉住门后恰好两行：水垢状态 + 水量/警告）。
	 */
	public static List<Component> detailLines(int aEfficiency, long aWater, long aWaterMax) {
		return List.of(scaleLine(aEfficiency), waterLine(aWater, aWaterMax));
	}

	/** 热量条比例（纯函数离线面）：钳 [0,1]，上限 0 答 0（不 NaN）。 */
	public static float heatRatio(long aHeat, long aHeatMax) {
		if (aHeatMax <= 0) return 0.0F;
		return (float)Math.max(0.0, Math.min(1.0, (double)aHeat / (double)aHeatMax));
	}

	/** 热量行（纯函数离线面）：上游温度计读数 verbatim 措辞（thermometer :410 原文形）。 */
	public static Component heatLine(long aHeat, long aHeatMax) {
		return Component.translatable(LANG_HEAT, aHeat, aHeatMax);
	}

	/** 需求行（纯函数离线面）：getEnergyDemanded 值 + HU/t 量纲。 */
	public static Component demandLine(long aDemand) {
		return Component.translatable(LANG_DEMAND, aDemand);
	}

	/**
	 * 输出门两态（纯函数离线面）：过半才开（上游 {@code tAmount = amount - capacity/2 > 0}
	 * :139/:203——半罐整点不开），开态紧凑文案含 &gt;3/4 双倍率说明（:141/:207）。
	 */
	public static Component gateLine(long aSteam, long aSteamMax) {
		return aSteam > aSteamMax / 2
				? Component.translatable(LANG_GATE_ABOVE)
				: Component.translatable(LANG_GATE_BELOW, aSteam, aSteamMax);
	}

	/** 水垢行（纯函数离线面）：上游放大镜措辞（magnifyingglass :416/:419——满效率 = 无水垢行）。 */
	public static Component scaleLine(int aEfficiency) {
		return aEfficiency < 10000
				? Component.translatable(LANG_SCALE, (10000 - aEfficiency) / 100)
				: Component.translatable(LANG_SCALE_CLEAN);
	}

	/** 水量行（纯函数离线面）：上游 :417/:420 措辞，缺水整行 RED。 */
	public static Component waterLine(long aWater, long aWaterMax) {
		return aWater > 0 ? Component.translatable(LANG_WATER, aWater, aWaterMax)
				: Component.translatable(LANG_NO_WATER).withStyle(ChatFormatting.RED);
	}

	/**
	 * 进度条盒形 swap——{@link GT6MachineProvider#jadeBox} 同缝（类 doc 引证：1.20.1
	 * BoxStyle.DEFAULT 字段 vs 1.21.1 getNestedBox() 工厂，编译期强制二选一）。
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
