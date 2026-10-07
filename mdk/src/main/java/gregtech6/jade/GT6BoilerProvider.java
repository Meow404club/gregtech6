package gregtech6.jade;

import javax.annotation.Nullable;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import net.minecraftforge.fluids.FluidStack;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.fluid.JadeFluidObject;
import snownee.jade.api.ui.IElement;
import snownee.jade.api.ui.IElementHelper;

import gregapi.util.UT;
import gregtech6.fluid.FluidTankGT;
import gregtech6.fluid.GTFluids;
import gregtech6.tileentity.TileEntityBase01Root;
import gregtech6.tileentity.energy.converters.GTBoilerTankBlockEntity;
import gregtech6.tileentity.multiblocks.TileEntityLargeBoiler;

/**
 * GT6 锅炉族 Jade 显示面（task jade-boiler 建，jade-redesign-core 重设计，boiler-jade-display
 * 增产气面）：单方块锅炉罐 {@link GTBoilerTankBlockEntity} 与大型锅炉
 * {@link TileEntityLargeBoiler} 同一 provider 同一格式——两 BE 无共享内容接口（各自直接
 * implements ITileEntityEnergy、字段形同构 mEnergy/mCapacity/mOutput/mEfficiency/mTanks）→
 * {@code appendServerData} 双 concrete instanceof 分发进同一静态缝。单体姿势 = client
 * tooltip + server data 双角色对（{@link GT6MachineProvider} 同形）。行件与色常量归一在
 * {@link GT6JadeRows}。
 *
 * <p>重设计面（design.r8-jade-tooltip families.boiler）：
 * <ol>
 * <li><b>热量条</b>——mEnergy/mCapacity（GTBoilerTankBlockEntity:153/:155；LargeBoiler
 *     :178/:180），"Stored Heat Units: %s / %s HU (Z%)" 借上游温度计读数 verbatim 加百分槽
 *     （thermometer :408-411）；热量 &gt; 0 橙（{@link GT6JadeRows#COLOR_HEAT}，r11c ②——
 *     GTOvenScreen COLOR_FILL 同值，热量是温度面非进度面）/ 空红。</li>
 * <li><b>水条常态显示</b>（从潜行升级，ruling boiler_water）——mTanks[0]，标签 = 实际流体名
 *     （KEY_WATER_FLUID 注册名串过缝，task jade-boiler-burningbox）。<b>空罐零行</b>
 *     （task boiler-jade-display ①，用户裁定「空就是空，不显示」——旧「Empty/空罐」空态行
 *     退役；门 = {@link #waterRowVisible}，空态词仅存于水行标签的解析失败防御回落）。</li>
 * <li><b>汽条常态显示</b>——mTanks[1]。两罐文本统一三槽 X / Y (Z%)。水/汽条 overlay =
 *     Jade 官方流体元素（r11c ①——水随 KEY_WATER_FLUID 身份、汽 {@link #STEAM_FLUID}
 *     固定身份，白条→流体贴图；{@link #fluidOverlay} 坩埚 overlayElement 同款链）。</li>
 * <li><b>产气速率行</b>（task boiler-jade-display ③）——当前蒸汽产率 mB/t。显示口径：BE
 *     tick 转换公式的纯镜像（两 BE tick 体逐字孪生 :256-265/:392-401）——
 *     {@code conversions = min(汽罐容/2560, min(热/80, 水量))}，产率 = units(conversions,
 *     10000, mEfficiency*160)（EU_PER_WATER 80 + STEAM_PER_WATER_GLOBAL 160 = 满效每升水
 *     160 mB 汽）；不计汽罐满罐钳制（镜像只答公式产率，与 tick 的 add 钳位面无关），0 =
 *     未在产（熄火/缺水/罐容臂钳死）。</li>
 * <li><b>需求行</b>——"Demand: %s HU/t"，值 = mOutput/2（getEnergyDemanded :458-460）。</li>
 * <li><b>效率行</b>（task boiler-jade-display ④）——当前热量利用率。显示口径：mEfficiency
 *     （万分比，:149/:174 初值 10000，水垢衰减 :119-122/:183-186、5000 地板）即
 *     「热量→蒸汽」利用率——每 80 HU + 1 L 水只产出 mEfficiency/10000 × 160 mB 汽，衰减的
 *     部分白白耗散；按上游 LH.percent 形显示 mEfficiency/100 的百分比（9400 → 94%，地板
 *     5000 → 50%），与潜行水垢行同数据源互补（水垢% = 100 − 效率%）。</li>
 * <li><b>潜行明细 = 水垢</b>（magnifyingglass :414-419——水量已常态上条，潜行只剩水垢面）。
 *     </li>
 * </ol>
 *
 * <p>退役：半满输出门两行（gt6.jade.boiler.gate.below/above——用户裁定垃圾信息；上游纯行为
 * :281-287 不配文案）、缺水警告行（非空水条即面）与空罐「Empty/空罐」行（boiler-jade-display
 * ① 裁定）。
 *
 * <p>同步契约同 {@link GT6MachineProvider}：appendServerData 写 GT6* 前缀键，appendTooltip 经
 * {@code accessor.getServerData()} 读回，只读公开字段禁改 BE（铁律）。非锅炉 BE 挂同一
 * {@link TileEntityBase01Root} 注册面（GT6JadePlugin 追加对），键存在即锅炉族
 * （{@code KEY_HEAT_MAX} 门）。Jade 自家 universal 流体面对锅炉族的 ban 在
 * {@link GT6FluidProvider#groupsOfTarget}（boiler-jade-display ②——双锅炉 BE 暴露
 * FLUID_HANDLER capability，不 ban 则 GT 行 + Jade 原生行双蒸汽罐渲染）。lang 键
 * {@code gt6.jade.boiler.*} 四落（GT6EnUs addBoilerJade + zh_cn_ref.tsv hand 带 +
 * py HAND_TRANSLATIONS + GT6ZhCn addBoilerJadeUnits），零裸 literal。
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
	/** 水罐流体身份（注册名串；空罐不写键——键存在即有流体，客户端空态词）。 */
	public static final String KEY_WATER_FLUID = "GT6BoilerWaterFluid";
	/** 当前蒸汽产率（mB/t，BE tick 转换公式纯镜像——boiler-jade-display ③）。 */
	public static final String KEY_RATE = "GT6BoilerRate";

	/**
	 * 汽条流体身份（task r11c-jade-bar-fluid-color ①——GTFluids.STEAM 源流体注册名；
	 * FluidType 客户端扩展声明 still = 原版 water_still + tint 0xFFC8C8C8 浅灰蒸汽
	 * （GTFluids ENGINE_SPECS steam 行），贴图身份即此处注册名反查）。
	 */
	public static final String STEAM_FLUID = "gt6:steam";

	/** lang 键（GT6EnUs/GT6ZhCn 双侧同发，四落纪律）。 */
	public static final String LANG_HEAT = "gt6.jade.boiler.heat";
	/** 热量条行（三槽）：现储热量 / 热量上限（两 long，上游温度计读数形）/ 整数百分比。 */
	public static final String LANG_DEMAND = "gt6.jade.boiler.demand";
	/** 需求行：槽 = mOutput/2（HU/t，getEnergyDemanded 值）。 */
	public static final String LANG_STEAM = "gt6.jade.boiler.steam";
	/** 汽条行（三槽）：汽量 / 汽罐容量（两 long）/ 整数百分比。 */
	public static final String LANG_SCALE = "gt6.jade.boiler.scale";
	/** 水垢行（潜行）：槽 = 水垢百分比（(10000-mEfficiency)/100，上游 LH.percent 形）。 */
	public static final String LANG_SCALE_CLEAN = "gt6.jade.boiler.scale.clean";
	/** 水条行（四槽）：流体显示名 / 水量 / 水罐容量（两 long）/ 整数百分比；空罐条面 RED
	 * （样式在条不在行），标签槽 = 实际流体名（task jade-boiler-burningbox——原「水」硬编码
	 * 退役；上游水罐 FL.water 本收水+蒸馏水，类型面真实存在）。 */
	public static final String LANG_WATER = "gt6.jade.boiler.water";
	/** 水条空态词（解析失败防御回落的标签槽）：en "Empty" / zh "空罐"——空罐行本身已退役
	 * （boiler-jade-display ①，空就是空）。 */
	public static final String LANG_WATER_EMPTY = "gt6.jade.boiler.water.empty";
	/** 产气速率行（boiler-jade-display ③）：槽 = 当前蒸汽产率（mB/t，KEY_RATE 值）。 */
	public static final String LANG_RATE = "gt6.jade.boiler.rate";
	/** 效率行（boiler-jade-display ④）：槽 = mEfficiency/100 的百分比（热量利用率口径）。 */
	public static final String LANG_EFFICIENCY = "gt6.jade.boiler.efficiency";

	/** Provider uid（GT6MachineProvider.java:143 同形——双腿 ctor swap 由 stonecutter 表消化）。 */
	private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath("gt6", "boiler_provider");

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
					aBoiler.mEfficiency, aBoiler.mTanks[0].amount(), aBoiler.mTanks[0].capacity(),
					tankFluidName(aBoiler.mTanks[0]));
		} else if (aAccessor.getBlockEntity() instanceof TileEntityLargeBoiler aBoiler) {
			writeBoilerData(aData, aBoiler.mEnergy, aBoiler.mCapacity, aBoiler.mOutput,
					aBoiler.mTanks[1].amount(), aBoiler.mTanks[1].capacity(),
					aBoiler.mEfficiency, aBoiler.mTanks[0].amount(), aBoiler.mTanks[0].capacity(),
					tankFluidName(aBoiler.mTanks[0]));
		}
	}

	/**
	 * 锅炉族同步写（appendServerData 的静态缝——GT6CrucibleProvider.writeCrucibleData 同姿势：
	 * accessor 薄壳 live-only，纯值离线可测）。水/汽键无条件写（条面载荷）；速率键服务端
	 * 算好（{@link #steamRate} 纯镜像，客户端零二次推导）。需求 = aOutput/2 服务端定死
	 * （getEnergyDemanded 返回值面，:458-460）。流体身份串非空才写键（空罐 = 键缺席 =
	 * 零水行，boiler-jade-display ①）。
	 */
	public static void writeBoilerData(CompoundTag aData, long aHeat, long aHeatMax, long aOutput, long aSteam,
			long aSteamMax, int aEfficiency, long aWater, long aWaterMax, String aWaterFluid) {
		aData.putLong(KEY_HEAT, aHeat);
		aData.putLong(KEY_HEAT_MAX, aHeatMax);
		aData.putLong(KEY_DEMAND, aOutput / 2); // :253/:371 mOutput/2
		aData.putLong(KEY_STEAM, aSteam);
		aData.putLong(KEY_STEAM_MAX, aSteamMax);
		aData.putShort(KEY_EFFICIENCY, (short)aEfficiency); // ten-thousandths, 5000 floor
		aData.putLong(KEY_WATER, aWater);
		aData.putLong(KEY_WATER_MAX, aWaterMax);
		aData.putLong(KEY_RATE, steamRate(aHeat, aSteamMax, aWater, aEfficiency)); // boiler-jade-display ③
		if (!aWaterFluid.isEmpty()) aData.putString(KEY_WATER_FLUID, aWaterFluid);
	}

	/**
	 * 当前蒸汽产率（mB/t——boiler-jade-display ③，BE tick 转换公式的纯函数镜像，BE 禁碰）。
	 * 两 BE 的 tick 体逐字孪生（GTBoilerTankBlockEntity:256-265 / TileEntityLargeBoiler
	 * :392-401）：{@code conversions = min(汽罐容/2560, min(热/EU_PER_WATER, 水量))}，产汽
	 * {@code UT.Code.units(conversions, 10000, mEfficiency*STEAM_PER_WATER_GLOBAL, false)}
	 * ——EU_PER_WATER 80（GTFluids.java:442）+ STEAM_PER_WATER_GLOBAL 160（:454，CS.java:242
	 * 上游 verbatim）= 满效每 80 HU + 1 L 水 → 160 mB 汽。镜像只答公式产率，不计 tick 的
	 * 汽罐满罐 add 钳位；零输入答 0（含 units 的 aTargetUnit==0 卫）。
	 */
	public static long steamRate(long aHeat, long aSteamMax, long aWater, int aEfficiency) {
		long tConversions = Math.min(aSteamMax / 2560, Math.min(aHeat / GTFluids.EU_PER_WATER, aWater));
		if (tConversions <= 0) return 0;
		return UT.Code.units(tConversions, 10000, (long)aEfficiency * GTFluids.STEAM_PER_WATER_GLOBAL, false);
	}

	/**
	 * 水条可见门（boiler-jade-display ①「空就是空」——读的正是同步载荷的两键事实：水量
	 * &gt; 0 且流体身份键在。空罐/无身份 = 零行，旌旗不立）。
	 */
	public static boolean waterRowVisible(CompoundTag aData) {
		return aData.getLong(KEY_WATER) > 0 && aData.contains(KEY_WATER_FLUID);
	}

	/**
	 * 水罐流体身份串（注册名；空罐/空流体 = 空串——GT6FluidProvider.tankViews :189-194 同卫，
	 * task jade-boiler-burningbox）。getRawFluid 由 neo 腿 stonecutter 正则换名吃掉
	 * （swap 表 getRawFluid→getFluid 条目，测试 6 位点同缝）。
	 */
	public static String tankFluidName(FluidTankGT aTank) {
		FluidStack tStack = aTank.fluid();
		if (tStack == null || tStack.isEmpty()) return "";
		Fluid tFluid = tStack.getRawFluid();
		if (tFluid == null || tFluid == Fluids.EMPTY) return "";
		ResourceLocation tName = BuiltInRegistries.FLUID.getKey(tFluid);
		return tName == null ? "" : tName.toString();
	}

	@Override
	public void appendTooltip(ITooltip aTooltip, BlockAccessor aAccessor, IPluginConfig aConfig) {
		CompoundTag aData = aAccessor.getServerData();
		if (!aData.contains(KEY_HEAT_MAX)) {
			return; // 非锅炉（本 provider 挂全 GT6 BE 面，键存在即锅炉族——GT6CrucibleProvider 同门）
		}
		// ① 热量条：mEnergy/mCapacity 比例，"Stored Heat Units: X / Y HU (Z%)"; 有热橙
		// （r11c ②——GTOvenScreen COLOR_FILL 同值，热量是温度面非进度面）/ 空红。
		long tHeat = aData.getLong(KEY_HEAT);
		long tHeatMax = aData.getLong(KEY_HEAT_MAX);
		GT6JadeRows.bar(aTooltip, GT6JadeRows.ratio(tHeat, tHeatMax), heatLine(tHeat, tHeatMax),
				tHeat > 0 ? GT6JadeRows.COLOR_HEAT : GT6JadeRows.COLOR_STALLED);
		// ② 水条：仅非空罐（boiler-jade-display ①「空就是空」——空罐零行，水行不渲染）；
		// 标签 = 实际流体名（门 = waterRowVisible，读的正是 KEY_WATER/KEY_WATER_FLUID 两键）；
		// overlay = 流体身份反查的官方流体元素（r11c ①，白条→贴图）。
		if (waterRowVisible(aData)) {
			long tWater = aData.getLong(KEY_WATER);
			long tWaterMax = aData.getLong(KEY_WATER_MAX);
			GT6JadeRows.bar(aTooltip, GT6JadeRows.ratio(tWater, tWaterMax),
					waterLine(waterLabel(aData.getString(KEY_WATER_FLUID)), tWater, tWaterMax),
					GT6JadeRows.COLOR_NEUTRAL, -1, fluidOverlay(aData.getString(KEY_WATER_FLUID), tWater));
		}
		// ③ 汽条：常态显示；overlay = gt6:steam 固定身份（r11c ①——still=water_still+
		// 0xFFC8C8C8 浅灰蒸汽，{@link #STEAM_FLUID}）。
		long tSteam = aData.getLong(KEY_STEAM);
		long tSteamMax = aData.getLong(KEY_STEAM_MAX);
		GT6JadeRows.bar(aTooltip, GT6JadeRows.ratio(tSteam, tSteamMax), steamLine(tSteam, tSteamMax),
				GT6JadeRows.COLOR_NEUTRAL, -1, fluidOverlay(STEAM_FLUID, tSteam));
		// ④ 产气速率行（boiler-jade-display ③）：当前蒸汽产率 mB/t（0 = 未在产，保留）。
		aTooltip.add(rateLine(aData.getLong(KEY_RATE)));
		// ⑤ 需求行。
		aTooltip.add(demandLine(aData.getLong(KEY_DEMAND)));
		// ⑥ 效率行（boiler-jade-display ④）：当前热量利用率 %。
		aTooltip.add(efficiencyLine(aData.getShort(KEY_EFFICIENCY)));
		// ⑦ 潜行明细 = 水垢（showDetails 门——水量已常态上条，潜行只剩水垢面）。
		if (aAccessor.showDetails()) {
			aTooltip.add(scaleLine(aData.getShort(KEY_EFFICIENCY)));
		}
	}

	/** 热量行（纯函数离线面，三槽）：上游温度计读数 verbatim 措辞 + 百分槽（thermometer :410 原文形）。 */
	public static Component heatLine(long aHeat, long aHeatMax) {
		return Component.translatable(LANG_HEAT, aHeat, aHeatMax, GT6JadeRows.percent(aHeat, aHeatMax));
	}

	/** 需求行（纯函数离线面）：getEnergyDemanded 值 + HU/t 量纲。 */
	public static Component demandLine(long aDemand) {
		return Component.translatable(LANG_DEMAND, aDemand);
	}

	/** 产气速率行（纯函数离线面，boiler-jade-display ③）：KEY_RATE 值 + mB/t 量纲（与汽条的 mB 同族）。 */
	public static Component rateLine(long aRate) {
		return Component.translatable(LANG_RATE, aRate);
	}

	/** 效率行（纯函数离线面，boiler-jade-display ④）：mEfficiency/100 的百分比槽（热量利用率口径，类 doc 声明）。 */
	public static Component efficiencyLine(int aEfficiency) {
		return Component.translatable(LANG_EFFICIENCY, aEfficiency / 100);
	}

	/** 汽条行（纯函数离线面，三槽）：mTanks[1] 现量/容量/百分。 */
	public static Component steamLine(long aSteam, long aSteamMax) {
		return Component.translatable(LANG_STEAM, aSteam, aSteamMax, GT6JadeRows.percent(aSteam, aSteamMax));
	}

	/** 水条行（纯函数离线面，四槽）：标签（流体显示名）/ mTanks[0] 现量/容量/百分（仅非空罐渲染，①）。 */
	public static Component waterLine(Component aLabel, long aWater, long aWaterMax) {
		return Component.translatable(LANG_WATER, aLabel, aWater, aWaterMax, GT6JadeRows.percent(aWater, aWaterMax));
	}

	/**
	 * 水行标签（client 纯函数）：注册名串 → 流体显示名（{@code FluidStack.getDisplayName}
	 * 双腿 javap 实证 21.1/47.4.10 同形；GTCEu RecipeOutputProvider 先例）；解析失败回落
	 * 空态词（防御面——行门 waterRowVisible 已保证非空罐才渲染，此分支 live 不可达）。
	 * 反查走 {@link GT6FluidProvider#resolveFluid}（本卡 private → 包内收编复用）。
	 */
	public static Component waterLabel(String aFluidId) {
		Fluid tFluid = GT6FluidProvider.resolveFluid(aFluidId);
		return tFluid == null
				? Component.translatable(LANG_WATER_EMPTY)
				: new FluidStack(tFluid, 1).getDisplayName();
	}

	/** 水垢行（纯函数离线面）：上游放大镜措辞（magnifyingglass :416/:419——满效率 = 无水垢行）。 */
	public static Component scaleLine(int aEfficiency) {
		return aEfficiency < 10000
				? Component.translatable(LANG_SCALE, (10000 - aEfficiency) / 100)
				: Component.translatable(LANG_SCALE_CLEAN);
	}

	/**
	 * 条 overlay 组装（live-only 客户端面，task r11c ①——{@link GT6CrucibleProvider}
	 * overlayElement 同款先例）：注册名串反查流体 → Jade 官方流体元素
	 * （{@code IElementHelper.fluid(JadeFluidObject)}——坩埚内容条已验收链）；解析失败答
	 * null = 纯色条，绝不 crash（守卫臂在 live-only 助手之前，离线可钉）。
	 */
	@Nullable
	static IElement fluidOverlay(String aFluidId, long aAmount) {
		Fluid tFluid = GT6FluidProvider.resolveFluid(aFluidId);
		return tFluid == null ? null : IElementHelper.get().fluid(JadeFluidObject.of(tFluid, aAmount));
	}

}
