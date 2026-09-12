package gregtech6.jade;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import gregapi.oredict.MaterialRegistry;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictMaterialStack;
import gregapi.util.CruciblePhysics;
import gregtech6.item.MaterialPrefixItem;
import gregtech6.tileentity.multiblocks.TileEntityCrucible;
import gregtech6.tileentity.tools.TileEntitySmeltery;

/**
 * GT6 坩埚族 Jade 显示面（task p28-crucible-jade-face）：小型 Smeltery 与大型 Crucible 同一
 * provider 同一格式（上游两类 addToolTips 语义同构、物理核同函数——MultiTileEntitySmeltery.java:111-121
 * / MultiTileEntityCrucible.java:147-161；两 BE 无共享内容接口 → {@code appendServerData} 双
 * concrete instanceof 分发进同一静态缝）。单体姿势 = {@link GT6MachineProvider} 的
 * client tooltip + server data 双角色对（GTCEu WorkableBlockProvider.java:37-89 形）。
 *
 * <p>上游零 WAILA 集成（research.p28-r-crucible-jade-face：(?i)waila 全源仅 5 处隔离注释）——
 * 本面属现代等效增强，语义锚 = 温度计 live 读数 "Temperature: NK"（MultiTileEntitySmeltery.java:512）
 * + addToolTips 熔毁暗红行（Smeltery:114 / Crucible:155 的 getTemperatureMax K 值）；内容物列表
 * 是上游没有的新信息面（live 内容物原本零文本通道）。
 *
 * <p>同步契约同 {@link GT6MachineProvider}：appendServerData 写 GT6* 前缀键，appendTooltip 经
 * {@code accessor.getServerData()} 读回，tag 由双腿 Jade 网络层搬运（CompoundTag/ListTag 搬运是
 * 既有契约，GT6MachineProvider.java:31-33），只读公开字段禁改 BE（铁律）。注册在
 * {@link GT6JadePlugin} 追加两行（服务端 TileEntityBase01Root + 客户端 GTEntityBlock，体内
 * instanceof 分发）；多方块成形态行仍由 {@link GT6MachineProvider} 出（两 provider 行共存）。
 *
 * <p>材质显示名走本仓既有机制：服务端按 en lang 走查同款守卫梯子（别名合并 + mID ≥ 0 +
 * mNameLocal 非空——{@code GT6EnUs.materialWalkEmittedKeys} 语义的 provider 侧镜像）核销
 * {@code gt6.material.<snake>} 小单位键（{@link MaterialPrefixItem#snakeCase} 单一推导），客户端
 * 渲染 translatable——en 面即 mNameLocal 词（en 走查值就是 mNameLocal），zh 面有翻译则译名、
 * 缺失经 vanilla 双语链回退英文（LanguageManager.java:50-52，零移植代码）；未核销材质如实回退
 * {@code getLocal()}（root OreDictMaterial.java:237）/注册内部名，绝不发明译名。
 *
 * <p>量格式移植上游 {@code UT.Code.displayUnits}（UT.java:1670-1674，"4.000" 形）——本仓 root
 * UT.java 无移植（全源检索零命中），故 provider 内最小实现（见 {@link #displayUnits}）。
 *
 * <p>TFRU 社区 WAILA 约定对位（commit 33c22beb，主会话裁定 2026-09-12）：① Content 标签——
 * TFRU 罐/管道行加 {@code LH.CONTENT} 前缀（MultiTileEntityPipeFluid.java:612-617 形
 * "Content N … L 流体名"）；坩埚是单内容池非罐排，取首行标签形——总量行即标签行
 * （en "Content: …"/zh "内容物: …"），条目行缩进明细，键数最小化。② Formed 行——TFRU 多方块
 * 部件显 "Formed &lt;控制器名&gt;"（LH.FORMED，MultiTileEntityMultiBlockPart.java:688-692），
 * 大型坩埚成形态已由 {@link GT6MachineProvider} 的 "Multiblock: formed/incomplete" 行覆盖
 * （TileEntityCrucible extends TileEntityBase10MultiBlockBase，GT6MachineProvider.java:137-140）
 * ——不重复加。③ 坩埚本体不在该补丁内，温度面无 TFRU 先例——温度计语义锚维持。
 */
public final class GT6CrucibleProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {

	public static final GT6CrucibleProvider INSTANCE = new GT6CrucibleProvider();

	/** 同步键——GT6 前缀命名空间（GT6MachineProvider.java:55 同契约）。 */
	public static final String KEY_TEMP = "GT6CrucibleTemp";
	public static final String KEY_TEMP_MAX = "GT6CrucibleTempMax";
	public static final String KEY_MELTDOWN = "GT6CrucibleMeltdown";
	public static final String KEY_TOTAL = "GT6CrucibleTotal";
	public static final String KEY_CONTENT = "GT6CrucibleContent";
	public static final String KEY_TRUNCATED = "GT6CrucibleTruncated";

	/** 内容物条目内键：核销过的 {@code gt6.material.<snake>} 小单位 slug（存在即走 translatable）。 */
	public static final String ENTRY_MAT = "mat";
	/** 内容物条目内键：纯文本回退名（mNameLocal → mNameInternal）。 */
	public static final String ENTRY_NAME = "name";
	/** 内容物条目内键：材料堆量（long，OreDictMaterialStack.java:44 原值）。 */
	public static final String ENTRY_AMOUNT = "amount";

	/** lang 键（GT6EnUs/GT6ZhCn 双侧同发）。 */
	public static final String LANG_TEMPERATURE = "gt6.jade.crucible.temperature";
	public static final String LANG_TOTAL = "gt6.jade.crucible.total";
	public static final String LANG_EMPTY = "gt6.jade.crucible.empty";
	public static final String LANG_MORE = "gt6.jade.crucible.more";

	/** 内容物行截断上限（research.p28-r-crucible-jade-face payload 裁定：按量降序截前 5）。 */
	public static final int MAX_CONTENT_ROWS = 5;

	/** 熔毁行/温度行的显示量纲词——上游温度计与 tooltip 均裸 K（Smeltery:512/:114）。 */
	private static final ResourceLocation UID = new ResourceLocation("gt6", "crucible_provider");

	private GT6CrucibleProvider() {
	}

	@Override
	public ResourceLocation getUid() {
		return UID;
	}

	@Override
	public void appendServerData(CompoundTag aData, BlockAccessor aAccessor) {
		// 两坩埚 BE 无共享内容接口（ITileEntityCrucible 仅 pour 缝，ITileEntityTemperature 仅温度
		// 两 getter）——双 concrete instanceof 汇进同一静态缝，格式严格同体（research 裁定）。
		if (aAccessor.getBlockEntity() instanceof TileEntitySmeltery aSmeltery) {
			writeCrucibleData(aData, aSmeltery.getTemperatureValue((byte) 0), aSmeltery.getTemperatureMax((byte) 0),
					aSmeltery.mMeltDown, aSmeltery.mContent);
		} else if (aAccessor.getBlockEntity() instanceof TileEntityCrucible aCrucible) {
			writeCrucibleData(aData, aCrucible.getTemperatureValue((byte) 0), aCrucible.getTemperatureMax((byte) 0),
					aCrucible.mMeltDown, aCrucible.mContent);
		}
	}

	/**
	 * 坩埚族同步写（appendServerData 的静态缝——GT6MachineProvider.appendMachineData 同姿势：
	 * accessor 薄壳 live-only，BE 读面离线可测）。温度/上限走 {@code ITileEntityTemperature}
	 * 面（Smeltery :514/:519 → temperatureMax()；Crucible :316/:321）；熔毁警告闩两 BE 均 tick
	 * 内活维护（Smeltery :234-235 / Crucible :475-479 isMeltDownWarning 重推导）——闩真值即
	 * 上游 isMeltDownWarning 门的服务端权威形，客户端不重复猜。内容物降序截前
	 * {@link #MAX_CONTENT_ROWS} 条，截去数走 int。
	 */
	public static void writeCrucibleData(CompoundTag aData, long aTemp, long aTempMax, boolean aMeltdown,
			List<OreDictMaterialStack> aContent) {
		aData.putLong(KEY_TEMP, aTemp);
		aData.putLong(KEY_TEMP_MAX, aTempMax);
		aData.putBoolean(KEY_MELTDOWN, aMeltdown);
		aData.putLong(KEY_TOTAL, CruciblePhysics.total(aContent));
		List<OreDictMaterialStack> tSorted = new ArrayList<>(aContent);
		tSorted.sort(Comparator.comparingLong((OreDictMaterialStack aStack) -> aStack.mAmount).reversed());
		ListTag tList = new ListTag();
		int tShown = Math.min(MAX_CONTENT_ROWS, tSorted.size());
		for (int tIndex = 0; tIndex < tShown; tIndex++) {
			tList.add(entryTag(tSorted.get(tIndex)));
		}
		aData.put(KEY_CONTENT, tList);
		aData.putInt(KEY_TRUNCATED, tSorted.size() - tShown);
	}

	/** 单条内容物 → 自描述 CompoundTag（name+amount，slug 核销成功才带 mat 键）。 */
	public static CompoundTag entryTag(OreDictMaterialStack aStack) {
		CompoundTag rTag = new CompoundTag();
		String tSlug = materialSlug(aStack);
		if (tSlug != null) {
			rTag.putString(ENTRY_MAT, tSlug);
		}
		rTag.putString(ENTRY_NAME, plainName(aStack));
		rTag.putLong(ENTRY_AMOUNT, aStack.mAmount);
		return rTag;
	}

	/**
	 * {@code gt6.material.<snake>} 小单位 slug，未核销（en 走查不会发键）答 null——守卫梯子
	 * 逐条镜像 {@code GT6EnUs.materialWalkEmittedKeys}（别名合并 MaterialRegistry.get 走
	 * mTargetRegistration 链 → mID ≥ 0 → mNameLocal 非空），保证 slug 存在 = en 面必有键 =
	 * 任何 locale 渲染不出裸键。与 materialFill 同用 {@link MaterialPrefixItem#snakeCase}
	 * 单一推导（materialFill.java:70-75 一致性强制令）。
	 */
	static String materialSlug(OreDictMaterialStack aStack) {
		OreDictMaterial tMaterial = aStack.mMaterial;
		if (tMaterial == null) return null;
		tMaterial = MaterialRegistry.INSTANCE.get(tMaterial);
		if (tMaterial == null || tMaterial.mID < 0 || tMaterial.mNameLocal == null) return null;
		return MaterialPrefixItem.snakeCase(tMaterial.mNameInternal);
	}

	/** 纯文本回退名：{@code getLocal()}（root OreDictMaterial.java:237）空则内部注册名。 */
	static String plainName(OreDictMaterialStack aStack) {
		if (aStack.mMaterial == null) return "?";
		String tLocal = aStack.mMaterial.getLocal();
		return tLocal == null || tLocal.isEmpty() ? aStack.mMaterial.mNameInternal : tLocal;
	}

	@Override
	public void appendTooltip(ITooltip aTooltip, BlockAccessor aAccessor, IPluginConfig aConfig) {
		CompoundTag aData = aAccessor.getServerData();
		if (!aData.contains(KEY_TEMP_MAX)) {
			return; // 非坩埚（本 provider 挂全 GT6 BE 面，键存在即坩埚族——GT6MachineProvider 同门）
		}
		// 行 1：温度现值/上限（K），熔毁警告整行 RED（上游熔毁暗红行 Smeltery:114 / Crucible:155 对位）。
		aTooltip.add(temperatureLine(aData.getLong(KEY_TEMP), aData.getLong(KEY_TEMP_MAX), aData.getBoolean(KEY_MELTDOWN)));
		// 行 2+：内容物总量 + 前 5 条 + 截断尾行；空坩埚只有 "Empty"。
		long tTotal = aData.getLong(KEY_TOTAL);
		if (tTotal <= 0) {
			aTooltip.add(emptyLine());
			return;
		}
		aTooltip.add(totalLine(tTotal));
		ListTag tList = aData.getList(KEY_CONTENT, Tag.TAG_COMPOUND);
		for (int tIndex = 0; tIndex < tList.size(); tIndex++) {
			aTooltip.add(contentLine(tList.getCompound(tIndex)));
		}
		int tTruncated = aData.getInt(KEY_TRUNCATED);
		if (tTruncated > 0) {
			aTooltip.add(moreLine(tTruncated));
		}
	}

	/** 温度行（纯函数离线面）：mMeltDown 时整行 RED，否则默认色。 */
	public static Component temperatureLine(long aTemp, long aTempMax, boolean aMeltdown) {
		MutableComponent rLine = Component.translatable(LANG_TEMPERATURE, aTemp, aTempMax);
		return aMeltdown ? rLine.withStyle(ChatFormatting.RED) : rLine;
	}

	/** 总量行："Content: 4.000 U" 形（displayUnits 形移植，量串纯文本客户端算）。 */
	public static Component totalLine(long aTotal) {
		return Component.translatable(LANG_TOTAL, displayUnits(aTotal));
	}

	/** 空坩埚行。 */
	public static Component emptyLine() {
		return Component.translatable(LANG_EMPTY);
	}

	/** 截断尾行："+N more"。 */
	public static Component moreLine(int aTruncated) {
		return Component.translatable(LANG_MORE, aTruncated);
	}

	/**
	 * 内容物行（两格缩进 + 显示名 + 量）：slug 核销条目走 {@code gt6.material.<snake>}
	 * translatable（各 locale 自解——en 即 mNameLocal 词，zh 有则译名），否则纯文本回退名。
	 */
	public static Component contentLine(CompoundTag aEntry) {
		Component tName = aEntry.contains(ENTRY_MAT)
				? Component.translatable("gt6.material." + aEntry.getString(ENTRY_MAT))
				: Component.literal(aEntry.getString(ENTRY_NAME));
		return Component.literal("  ").append(tName)
				.append(Component.literal(": " + displayUnits(aEntry.getLong(ENTRY_AMOUNT)) + " U"));
	}

	/**
	 * 量串 = 上游 {@code UT.Code.displayUnits}（UT.java:1670-1674）逐字移植——本仓 root UT.java
	 * 无等价物（全源检索零命中），按卡在 provider 内最小实现：U 整数部分 + "." + 三位小数
	 * （"4.000"）；负量上游答 "?.???"。
	 */
	public static String displayUnits(long aAmount) {
		if (aAmount < 0) return "?.???";
		long tDigits = ((aAmount % CS_U) * 1000) / CS_U;
		return (aAmount / CS_U) + "." + (tDigits < 1 ? "000" : tDigits < 10 ? "00" + tDigits : tDigits < 100 ? "0" + tDigits : tDigits);
	}

	/** CS.U = 648648000（root CS.java:49）——私有副本避免整把 CS 常量静态导入。 */
	private static final long CS_U = 648648000L;
}
