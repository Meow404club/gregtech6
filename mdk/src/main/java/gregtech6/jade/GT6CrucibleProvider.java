package gregtech6.jade;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.fluid.JadeFluidObject;
import snownee.jade.api.ui.IElement;
import snownee.jade.api.ui.IElementHelper;

import gregapi.oredict.MaterialRegistry;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictMaterialStack;
import gregapi.util.CruciblePhysics;
import gregtech6.datagen.GT6CrucibleDatagen;
import gregtech6.fluid.FluidBridge;
import gregtech6.item.MaterialPrefixItem;
import gregtech6.tileentity.multiblocks.ITileEntityMultiBlockController;
import gregtech6.tileentity.multiblocks.TileEntityCrucible;
import gregtech6.tileentity.tools.TileEntitySmeltery;

/**
 * GT6 坩埚族 Jade 显示面（task crucible-jade-face）：小型 Smeltery 与大型 Crucible 同一
 * provider 同一格式（上游两类 addToolTips 语义同构、物理核同函数——MultiTileEntitySmeltery.java:111-121
 * / MultiTileEntityCrucible.java:147-161；两 BE 无共享内容接口 → {@code appendServerData} 双
 * concrete instanceof 分发进同一静态缝）。单体姿势 = {@link GT6MachineProvider} 的
 * client tooltip + server data 双角色对（GTCEu WorkableBlockProvider.java:37-89 形）。
 *
 * <p>上游零 WAILA 集成（research.p28-r-crucible-jade-face：(?i)waila 全源仅 5 处隔离注释）——
 * 本面属现代等效增强，语义锚 = 温度计 live 读数 "Temperature: NK"（MultiTileEntitySmeltery.java:512）
 * + addToolTips 熔毁暗红行（Smeltery:114 / Crucible:155 的 getTemperatureMax K 值）；内容物列表
 * 是上游没有的新信息面（live 内容物原本零文本通道）。task jade-converter-crucible-restyle
 * 微调（design.r8-jade-tooltip families.crucible）：温度行升 B 形钳位比例条（新键
 * {@code gt6.jade.crucible.temperature.bar}，两槽 'Temperature: %s / %s K'）。
 *
 * <p>task crucible-jade-tankbar（用户三版终裁 v3）内容面升条：①内容总量行升 tank 条——熔融态
 * overlay = Jade 官方流体元素（{@code JadeFluidObject.of} → {@code IElementHelper.fluid} →
 * {@code progressStyle().overlay}，forge FluidView.java:47-48 + FluidStorageProvider.java:68-69 /
 * neo :124 双腿实证），桥材质走 {@link FluidBridge#moltenFluidForMaterial}；②其余态 overlay =
 * 自实现 {@link GT6ContentFaceElement} 渲染 ContentFace 缝同源贴图（固体=bodyTexture+mRGBaSolid
 * / 熔融=moltenTexture 每材质组 molten 灰度+mRGBaLiquid（task r11b-crucible-molten-art，
 * 平板占位退役）——{@code GT6CrucibleDatagen.contentFace} 只读消费，与碗内
 * 观感一致；固体臂经 bodyTexture 全铺 SET 分派（task crucible-solid-face-matrix）对全材质
 * 恒可用）；③量纲词 'U'/'份' 进 lang（{@link #LANG_ENTRY} 新键 +
 * total 键 zh 面 份）；④融毁红字联动——闩落时条文字变红（{@code textColor} =
 * {@link GT6JadeRows#FORMAT_STALLED}），独立红色温度警报行（r8 保留裁定）被用户裁定废除：
 * 行与 gt6.jade.crucible.temperature 键同 lang 四落退役（v3 覆盖 r8 '旧行保留'）。⑤服务端新增
 * {@link #KEY_TOTAL_MAX}（{@code TileEntityCrucible.MAX_AMOUNT} / {@code Params.SMALL.maxAmount()}）
 * 与 {@link #KEY_MOLTEN}（lightest 熔点≤温度，上游 mDisplayedFluid 门 MultiTileEntitySmeltery
 * .java:299 语义）两键。
 *
 * <p>同步契约同 {@link GT6MachineProvider}：appendServerData 写 GT6* 前缀键，appendTooltip 经
 * {@code accessor.getServerData()} 读回，tag 由双腿 Jade 网络层搬运（CompoundTag/ListTag 搬运是
 * 既有契约，GT6MachineProvider.java:31-33），只读公开字段禁改 BE（铁律）。注册在
 * {@link GT6JadePlugin} 追加两行（服务端 TileEntityBase01Root + 客户端 GTEntityBlock，体内
 * instanceof 分发）；多方块成形态行仍由 {@link GT6MachineProvider} 出（两 provider 行共存）；
 * 行件与色常量归一在 {@link GT6JadeRows}。
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
 * ② 的墙件侧落点 = task mb-formed-crucible-wall ⑥（用户追加）：墙件 relay 臂出
 * "Part of: &lt;控制器名&gt;" 归属行 + 坩埚核心信息同格式透传（上游墙件全部数据接口本就经
 * getTarget(T) 透传控制器，MultiTileEntityMultiBlockPart.java:580-600—— Jade 指墙零信息
 * 即缺这一臂，known_bugs.r11-mb-formed-skin.crucible_wall_jade）。
 *
 * <p>task crucible-jade-follower 透传考古（症状 A 结论）：上游 1.7.10 对 WAILA 零 API 集成
 * ——(?i)waila 全树 5 文件全是注释/常量（BlockBaseFluid.java:92、CS.java:1948 工具名常量、
 * BlockWaterlike.java:74、MultiTileEntityRock.java:170、MultiTileEntityStick.java:102），
 * 零 IWaila* 实现，坩埚墙在上游也从不在 WAILA 显坩埚数据——透传面纯属现代等效增强，其
 * 语义锚 = 墙件数据面转发：MultiTileEntityMultiBlockPart.java:75 一类实现
 * ITileEntityTemperature/ITileEntityGibbl/ITileEntityCrucible 全族，:658-663
 * getTemperatureValue→getTarget(T) 转发、:665-670 getTemperatureMax 同形、:644-656
 * getGibblValue/Max（内容量）同形、:686-691 fillMoldAtSide 先 NO_CRUCIBLE 门再转发。
 * 本 provider 的墙臂（appendServerData → writeCrucibleData + KEY_OWNER）即该数据面的
 * 显示侧；⑥ 落地后实机仍不通的真断点在客户端腿注册：墙方块类树 GTCrucibleWallBlock →
 * GTMultiBlockPartBlock → BaseEntityBlock（GTMultiBlockPartBlock.java:62）不含
 * GTEntityBlock，而 Jade 客户端分发只沿 getSuperclass() 链找注册锚（jade-1201 impl
 * HierarchyLookup.java:70-75）→ 服务端写了数据、客户端没有 provider 肯渲染。修复 =
 * {@link GT6JadePlugin} 为 GTCrucibleWallBlock 追加一条 registerBlockComponent（体内
 * KEY_TEMP_MAX 键门保证非坩埚族部件零输出）。
 *
 * <p>task crucible-jade-follower 文案裁定（用户 2026-10-06）：内容物面砍 "内容物:"/"Content:"
 * 前缀（LANG_TOTAL 值去标签），量改 n/容量 份额直显（{@link #shareAmount}——分母 = 坩埚
 * 自家容量 SMALL 16 / LARGE 432，随 KEY_TOTAL_MAX 已同步下发）；en "n/16"、zh "n/16 份"
 * （单位词仍骑 lang 值）。total 与 entry 两行同裁定（旧 :255 域 " U" 硬编码即 entry 行）。
 */
public final class GT6CrucibleProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {

	public static final GT6CrucibleProvider INSTANCE = new GT6CrucibleProvider();

	/** 同步键——GT6 前缀命名空间（GT6MachineProvider.java:55 同契约）。 */
	public static final String KEY_TEMP = "GT6CrucibleTemp";
	public static final String KEY_TEMP_MAX = "GT6CrucibleTempMax";
	public static final String KEY_MELTDOWN = "GT6CrucibleMeltdown";
	public static final String KEY_TOTAL = "GT6CrucibleTotal";
	/** 容量上限（tank 条分母）：大型 = {@code TileEntityCrucible.MAX_AMOUNT}，小型 =
	 * {@code CruciblePhysics.Params.SMALL.maxAmount()}（v3 ⑤）。 */
	public static final String KEY_TOTAL_MAX = "GT6CrucibleTotalMax";
	/** 熔融态（上游 mDisplayedFluid 门 :299 语义）：lightest 非空且其熔点 ≤ 当前温度。 */
	public static final String KEY_MOLTEN = "GT6CrucibleMolten";
	/** 条 overlay 载荷（CompoundTag，lightest 存在才写）——分派在服务端静态缝完成：
	 * 桥熔融流体走 {@link #OVERLAY_FLUID}+{@link #OVERLAY_AMOUNT}（Jade 官方流体元素），
	 * 否则 {@link #OVERLAY_TEXTURE}+{@link #OVERLAY_TINT}（ContentFace 缝）。 */
	public static final String KEY_OVERLAY = "GT6CrucibleOverlay";
	public static final String OVERLAY_FLUID = "fluid";
	public static final String OVERLAY_AMOUNT = "amount";
	public static final String OVERLAY_TEXTURE = "texture";
	public static final String OVERLAY_TINT = "tint";
	public static final String KEY_CONTENT = "GT6CrucibleContent";
	public static final String KEY_TRUNCATED = "GT6CrucibleTruncated";
	/** 归属行（task mb-formed-crucible-wall ⑥）：控制器方块名的 translatable 键
	 * （"block.gt6.&lt;path&gt;"，GTCrucibleControllerBlock.getName :88-89 同一推导）——
	 * 墙件 relay 臂写，客户端 translatable 各 locale 自解（materialSlug 同契约）。 */
	public static final String KEY_OWNER = "GT6CrucibleOwner";
	/** 归属行 lang 键（en "Part of: %s" / zh "归属: %s"——TFRU 部件行 "Formed &lt;控制器名&gt;"
	 * 约定的现代面，任务卡第 6 项用户追加）。 */
	public static final String LANG_OWNER = "gt6.jade.crucible.owner";

	/** 内容物条目内键：核销过的 {@code gt6.material.<snake>} 小单位 slug（存在即走 translatable）。 */
	public static final String ENTRY_MAT = "mat";
	/** 内容物条目内键：纯文本回退名（mNameLocal → mNameInternal）。 */
	public static final String ENTRY_NAME = "name";
	/** 内容物条目内键：材料堆量（long，OreDictMaterialStack.java:44 原值）。 */
	public static final String ENTRY_AMOUNT = "amount";

	/** lang 键（GT6EnUs/GT6ZhCn 双侧同发）。 */
	/** 条行键（task jade-converter-crucible-restyle，B 形两槽）：现值/上限 K——单位词尾置
	 * 一次（design 'Temperature: %s / %s K'）。 */
	public static final String LANG_TEMPERATURE_BAR = "gt6.jade.crucible.temperature.bar";
	/** 总量行键（task crucible-jade-follower 文案裁定）：无前缀份额串单槽——en "%s"、
	 * zh "%s 份"（份额串 "4/16" 由 {@link #shareAmount} 产出）。 */
	public static final String LANG_TOTAL = "gt6.jade.crucible.total";
	/** 条目明细行键（task crucible-jade-tankbar ③ + follower 文案裁定）：缩进+名+份额
	 * 全行 translatable——en "  %s: %s" / zh "  %s: %s 份"（份额串单槽，" U" 硬编码退役）。 */
	public static final String LANG_ENTRY = "gt6.jade.crucible.entry";
	public static final String LANG_EMPTY = "gt6.jade.crucible.empty";
	public static final String LANG_MORE = "gt6.jade.crucible.more";

	/** 内容物行截断上限（research.p28-r-crucible-jade-face payload 裁定：按量降序截前 5）。 */
	public static final int MAX_CONTENT_ROWS = 5;

	/** 熔毁行/温度行的显示量纲词——上游温度计与 tooltip 均裸 K（Smeltery:512/:114）。 */
	private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath("gt6", "crucible_provider");

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
		// 容量分母各取自家参数（v3 ⑤：MAX_AMOUNT = Params.LARGE 形，TileEntityCrucible.java:136）。
		if (aAccessor.getBlockEntity() instanceof TileEntitySmeltery aSmeltery) {
			writeCrucibleData(aData, aSmeltery.getTemperatureValue((byte) 0), aSmeltery.getTemperatureMax((byte) 0),
					aSmeltery.mMeltDown, CruciblePhysics.Params.SMALL.maxAmount(), aSmeltery.mContent);
		} else if (aAccessor.getBlockEntity() instanceof TileEntityCrucible aCrucible) {
			writeCrucibleData(aData, aCrucible.getTemperatureValue((byte) 0), aCrucible.getTemperatureMax((byte) 0),
					aCrucible.mMeltDown, TileEntityCrucible.MAX_AMOUNT, aCrucible.mContent);
		} else if (aAccessor.getBlockEntity() instanceof gregtech6.tileentity.multiblocks.CrucibleWallBlockEntity aWall) {
			// 墙件 relay 臂（task mb-formed-crucible-wall ⑥）：上游墙件全部数据接口都经
			// getTarget(T) 透传控制器（MultiTileEntityMultiBlockPart.java:580-600 温度/gibbl/
			// 进度族，:689 有效性探针同形）—— Jade 指墙 = 该 relay 语义的现代面。getTarget(true)
			// 门=结构成立才出数据（坏塔残留热不上屏，与上游 relay 非法答 0 同款）；归属行 =
			// TFRU "Formed <控制器名>" 约定（GT6CrucibleProvider 类 doc ② 的墙件侧落点）。
			ITileEntityMultiBlockController tTarget = aWall.getTarget(true);
			if (tTarget instanceof TileEntityCrucible aCrucible) {
				writeCrucibleData(aData, aCrucible.getTemperatureValue((byte) 0), aCrucible.getTemperatureMax((byte) 0),
						aCrucible.mMeltDown, TileEntityCrucible.MAX_AMOUNT, aCrucible.mContent);
				aData.putString(KEY_OWNER, "block.gt6." + BuiltInRegistries.BLOCK
						.getKey(((net.minecraft.world.level.block.entity.BlockEntity) tTarget).getBlockState().getBlock()).getPath());
			}
		}
	}

	/**
	 * 坩埚族同步写（appendServerData 的静态缝——GT6MachineProvider.appendMachineData 同姿势：
	 * accessor 薄壳 live-only，BE 读面离线可测）。温度/上限走 {@code ITileEntityTemperature}
	 * 面（Smeltery :514/:519 → temperatureMax()；Crucible :316/:321）；熔毁闩两 BE 均 tick
	 * 内活维护（Smeltery :234-235 / Crucible :475-479 isMeltDownWarning 重推导）——闩真值即
	 * 上游 isMeltDownWarning 门的服务端权威形，客户端不重复猜。内容物降序截前
	 * {@link #MAX_CONTENT_ROWS} 条，截去数走 int。容量上限 {@code aTotalMax} 由调用方各取
	 * 自家参数。overlay 分派（v3 ①②）：lightest（上游显示普查，MultiTileEntitySmeltery
	 * .java:299 同走）熔融且桥有流体 → Jade 官方流体元素载荷；否则 ContentFace 缝载荷
	 * （熔融/固体两臂经全铺 SET 分派对全材质恒可用——防御 catch 契约见 {@link #overlayTag}）。
	 */
	public static void writeCrucibleData(CompoundTag aData, long aTemp, long aTempMax, boolean aMeltdown,
			long aTotalMax, List<OreDictMaterialStack> aContent) {
		aData.putLong(KEY_TEMP, aTemp);
		aData.putLong(KEY_TEMP_MAX, aTempMax);
		aData.putBoolean(KEY_MELTDOWN, aMeltdown);
		aData.putLong(KEY_TOTAL, CruciblePhysics.total(aContent));
		aData.putLong(KEY_TOTAL_MAX, aTotalMax);
		OreDictMaterialStack tLightest = lightest(aContent);
		boolean tMolten = tLightest != null && tLightest.mMaterial.mMeltingPoint <= aTemp; // :299 门
		aData.putBoolean(KEY_MOLTEN, tMolten);
		CompoundTag tOverlay = tLightest == null ? null : overlayTag(tLightest, tMolten, CruciblePhysics.total(aContent));
		if (tOverlay != null) aData.put(KEY_OVERLAY, tOverlay); // guard 回落 = 零键（纯色条）
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

	/**
	 * lightest 普查（provider 侧镜像两 BE 的 {@code lightest()}——TileEntitySmeltery.java:469-475 /
	 * TileEntityCrucible.java:823-829 同一密度最轻走查，上游 :299 显示面的取材；BE 禁碰，故在此
	 * 重写为纯函数）。
	 */
	@Nullable
	public static OreDictMaterialStack lightest(List<OreDictMaterialStack> aContent) {
		OreDictMaterialStack rLightest = null;
		for (OreDictMaterialStack tMaterial : aContent) {
			if (rLightest == null || tMaterial.mMaterial.mGramPerCubicCentimeter < rLightest.mMaterial.mGramPerCubicCentimeter) rLightest = tMaterial;
		}
		return rLightest;
	}

	/**
	 * overlay 载荷（纯函数离线面）：桥熔融流体在 → 官方流体元素载荷（registry id + 桥 144 L/unit
	 * 惯例量）；否则 ContentFace 缝载荷（texture 全限定名 + 不透明 ARGB tint）。bodyTexture
	 * 已是全铺 SET 分派（task crucible-solid-face-matrix，上游 :983-990 全材质语义），固体臂
	 * 恒可用；{@link GT6CrucibleDatagen#contentFace} 的 {@code IllegalStateException} 契约若
	 * 日后再收窄，此 catch 兜底回落 null = 纯色条，服务器写数据面永不 crash。
	 *
	 * <p>TODO(crucible-render-followup)：贴图源升级缝已在 {@code contentFace} 单点——并行
	 * 渲染卡搬运的专属 molten/固体资产合入后，此处零改动自动吃新贴图 id（消费面三方同源：
	 * 碗 tint / 大坩埚 BER / 本 Jade 条）。资产在仓性已由 GT6CrucibleProviderTest 的
	 * contentFace 资产 census 钉死（条非空白面）。
	 */
	@Nullable
	public static CompoundTag overlayTag(OreDictMaterialStack aLightest, boolean aMolten, long aTotal) {
		CompoundTag rTag = new CompoundTag();
		if (aMolten) {
			Fluid tFluid = FluidBridge.moltenFluidForMaterial(aLightest.mMaterial.mNameInternal);
			if (tFluid != null) {
				rTag.putString(OVERLAY_FLUID, BuiltInRegistries.FLUID.getKey(tFluid).toString());
				rTag.putLong(OVERLAY_AMOUNT, aTotal * FluidBridge.L_PER_MOLTEN_UNIT);
				return rTag;
			}
		}
		try {
			GT6CrucibleDatagen.ContentFace tFace = GT6CrucibleDatagen.contentFace(aLightest.mMaterial, aMolten);
			rTag.putString(OVERLAY_TEXTURE, tFace.texture());
			rTag.putInt(OVERLAY_TINT, tFace.tintARGB());
			return rTag;
		} catch (IllegalStateException tUnmapped) {
			// ponytail: 全铺表（crucible-solid-face-matrix）后此臂理论不可达——保留作防御：
			// contentFace 契约若再收窄，回落纯色条而非炸服务器写数据面
			return null;
		}
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
		// 行 0（task mb-formed-crucible-wall ⑥）：墙件归属行——控制器名嵌套 translatable
		// （键来自 KEY_OWNER，各 locale 自解；vanilla %s 组件槽递归渲染，contentLine 同款）。
		if (aData.contains(KEY_OWNER)) {
			aTooltip.add(ownerLine(aData.getString(KEY_OWNER)));
		}
		// 行 1：温度条（task jade-converter-crucible-restyle，B 形收编）——temp/tempmax 钳位
		// 比例（GT6JadeRows.ratio），两槽文本（单位词尾置一次），条面 = 钢铁加热色阶
		// （r11c ③——{@link GT6JadeRows#heatColor} 六停靠连续插值，ratio 1.0 = 熔毁顶 =
		// 白；融毁红字仍在内容条的文字色，此条面色独立）。条本体组装 live-only
		// （IElementHelper 需客户端），离线钉 = 线契约 + 行函数 + 色阶纯函数。
		long tTemp = aData.getLong(KEY_TEMP);
		long tTempMax = aData.getLong(KEY_TEMP_MAX);
		float tRatio = GT6JadeRows.ratio(tTemp, tTempMax);
		GT6JadeRows.bar(aTooltip, tRatio, temperatureBarLine(tTemp, tTempMax), GT6JadeRows.heatColor(tRatio));
		// 行 2：内容 tank 条（task crucible-jade-tankbar v3）——overlay = 服务端分派好的载荷
		// （官方流体元素 / ContentFace 同源元素），条文本 = 总量行；融毁闩落时条文字变红
		// （④：独立红色警报行废除，红字面并入此条——v3 覆盖 r8 '旧行保留' 裁定）。
		long tTotal = aData.getLong(KEY_TOTAL);
		if (tTotal <= 0) {
			aTooltip.add(emptyLine());
			return;
		}
		GT6JadeRows.bar(aTooltip, GT6JadeRows.ratio(tTotal, aData.getLong(KEY_TOTAL_MAX)),
				totalLine(tTotal, aData.getLong(KEY_TOTAL_MAX)), GT6JadeRows.COLOR_NEUTRAL, barTextColor(aData.getBoolean(KEY_MELTDOWN)),
				overlayElement(aData));
		// 行 3+：前 5 条目 + 截断尾行。
		ListTag tList = aData.getList(KEY_CONTENT, Tag.TAG_COMPOUND);
		long tTotalMax = aData.getLong(KEY_TOTAL_MAX);
		for (int tIndex = 0; tIndex < tList.size(); tIndex++) {
			aTooltip.add(contentLine(tList.getCompound(tIndex), tTotalMax));
		}
		int tTruncated = aData.getInt(KEY_TRUNCATED);
		if (tTruncated > 0) {
			aTooltip.add(moreLine(tTruncated));
		}
	}

	/** 温度条行（纯函数离线面，两槽）：design 'Temperature: %s / %s K'——单位词尾置一次。 */
	public static Component temperatureBarLine(long aTemp, long aTempMax) {
		return Component.translatable(LANG_TEMPERATURE_BAR, aTemp, aTempMax);
	}

	/**
	 * 归属行（纯函数离线面，task mb-formed-crucible-wall ⑥）：两槽嵌套 translatable——
	 * 外槽 {@link #LANG_OWNER}，内槽 = 控制器方块名键（"block.gt6.&lt;path&gt;"）。
	 */
	public static Component ownerLine(String aOwnerBlockKey) {
		return Component.translatable(LANG_OWNER, Component.translatable(aOwnerBlockKey));
	}

	/**
	 * 条文字色（纯函数离线面，④红字路径钉）：融毁闩落 = {@link GT6JadeRows#FORMAT_STALLED}
	 * 红（原独立警报行的字面收进条文本），常温 = -1（Jade 默认白——GT6JadeRows.bar 原字面色）。
	 */
	public static int barTextColor(boolean aMeltdown) {
		return aMeltdown ? GT6JadeRows.FORMAT_STALLED.getColor() : -1;
	}

	/**
	 * 条 overlay 组装（live-only 客户端面）：官方流体元素（载荷为桥流体 id 时）或
	 * {@link GT6ContentFaceElement}（ContentFace 缝载荷）；零载荷键/回落 null → 纯色条。
	 * 精灵/流体 id 都经 {@code tryParse}（双腿同形——1.20.1 单参构造在 21.1 已删，
	 * GT6CrucibleDatagen.loc 同款教训）。
	 */
	@Nullable
	private static IElement overlayElement(CompoundTag aData) {
		if (!aData.contains(KEY_OVERLAY, Tag.TAG_COMPOUND)) return null;
		CompoundTag tOverlay = aData.getCompound(KEY_OVERLAY);
		if (tOverlay.contains(OVERLAY_FLUID)) {
			Fluid tFluid = BuiltInRegistries.FLUID.get(ResourceLocation.tryParse(tOverlay.getString(OVERLAY_FLUID)));
			return IElementHelper.get().fluid(JadeFluidObject.of(tFluid, tOverlay.getLong(OVERLAY_AMOUNT)));
		}
		return new GT6ContentFaceElement(ResourceLocation.tryParse(tOverlay.getString(OVERLAY_TEXTURE)),
				tOverlay.getInt(OVERLAY_TINT));
	}

	/** 总量行（条文本，task crucible-jade-follower 文案裁定）：无前缀、n/容量 份额形
	 * （en "4/16"、zh "4/16 份"——单位词骑 lang 值，份额串单槽）。 */
	public static Component totalLine(long aTotal, long aTotalMax) {
		return Component.translatable(LANG_TOTAL, shareAmount(aTotal, aTotalMax));
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
	 * 内容物行（整行 translatable，③ + task crucible-jade-follower 文案裁定）：两格缩进 +
	 * 显示名 + n/容量 份额全在 {@link #LANG_ENTRY} 值里（en "  %s: %s" / zh "  %s: %s 份"）
	 * ——slug 核销条目的名字槽走 {@code gt6.material.<snake>} translatable（各 locale 自解），
	 * 否则纯文本回退名；vanilla 的 %s 组件槽递归渲染嵌套 translatable（MaterialPrefixItem
	 * 小单位链同款）。份额分母 = 坩埚自家容量（appendTooltip 传 KEY_TOTAL_MAX）。
	 */
	public static Component contentLine(CompoundTag aEntry, long aTotalMax) {
		Component tName = aEntry.contains(ENTRY_MAT)
				? Component.translatable("gt6.material." + aEntry.getString(ENTRY_MAT))
				: Component.literal(aEntry.getString(ENTRY_NAME));
		return Component.translatable(LANG_ENTRY, tName, shareAmount(aEntry.getLong(ENTRY_AMOUNT), aTotalMax));
	}

	/**
	 * 份额串（task crucible-jade-follower 文案裁定，纯函数离线面）："n/容量" 形——分子 = 量
	 * （整 U 时裸整数 "4"，否则 {@link #displayUnits} 三位小数 "2.500"；≤0 走 displayUnits
	 * 的 "0.000"/"?.???" 面），分母 = 坩埚容量（SMALL 16 / LARGE 432，均整 U；非整 U 防御臂
	 * 走 displayUnits）。直显份数的语义 = 条填充比（ratio = total/max）的文字同款。
	 */
	public static String shareAmount(long aAmount, long aTotalMax) {
		String tNumerator = aAmount > 0 && aAmount % CS_U == 0 ? String.valueOf(aAmount / CS_U) : displayUnits(aAmount);
		String tDenominator = aTotalMax % CS_U == 0 && aTotalMax > 0 ? String.valueOf(aTotalMax / CS_U) : displayUnits(aTotalMax);
		return tNumerator + "/" + tDenominator;
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
