package gregtech6.jade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import gregapi.code.TagData;
import gregapi.data.TD;
import gregtech6.tileentity.energy.GT6BatteryBoxBlockEntity;
import gregtech6.tileentity.energy.GT6DynamoBlockEntity;
import gregtech6.tileentity.energy.GT6ElectricDynamoBlockEntity;
import gregtech6.tileentity.energy.GT6ElectricTransformerBlockEntity;
import gregtech6.tileentity.energy.GT6FeConverterBlockEntity;
import gregtech6.tileentity.energy.GT6FluxDynamoBlockEntity;
import gregtech6.tileentity.energy.GT6LongDistanceTransformerBlockEntity;
import gregtech6.tileentity.energy.GT6ZpmDechargerBlockEntity;
import gregtech6.tileentity.energy.GTCrankBlockEntity;
import gregtech6.tileentity.energy.GTDieselEngineBlockEntity;
import gregtech6.tileentity.energy.GTEnergySourceBlockEntity;
import gregtech6.tileentity.energy.GTSteamEngineBlockEntity;
import gregtech6.tileentity.energy.GTTransformerRotationBlockEntity;
import gregtech6.tileentity.energy.converters.GT6LaserConverterBlockEntity;
import gregtech6.tileentity.energy.generators.GT6MagicAbsorberBlockEntity;
import gregtech6.tileentity.energy.generators.GTGeneratorSolidBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * GT6 转换器/引擎族 Jade 显示面（task r5-jade-converters，r8-jade-converter-crucible-restyle
 * restyle）：变压器/动力机（dynamo）/能量引擎/燃烧箱/电池箱等 ~19 个 TicksAndSync BE 的
 * active/stopped 状态行 + 能量存量行 + 额定吞吐行。r8 微调（design.r8-jade-tooltip
 * families.converter）：真上限族（capacity&gt;0）的存量行升 B 形钳位比例条（文本复用既有三槽键，
 * lang 裁定 gt6.jade.converter.* 不动）；裸存量形（capacity=0，燃烧箱）保文本；吞吐行保留文本；
 * 状态行字色收编 {@link GT6JadeRows#FORMAT_OK}/{@link GT6JadeRows#FORMAT_STALLED}（J1 交卡
 * 遗留的第三份状态色复制）。
 * 上游 1.7.10 零 WAILA 面（research.r5-jade-integration：全源仅 5 处隔离注释）——本面属现代
 * 增强，行式借 {@link GT6MachineProvider}/{@link GT6CrucibleProvider} 的既有家法与 GTCEu
 * RecipeLogicProvider.java:75+ 的吞吐行先例。
 *
 * <p>族谱实况（逐类盘点 2026-09-28，与研究员指认一致）：公共字段形散在各 concrete/abstract
 * 类上，没有携带字段的共同基类，移植 {@code ITileEntityEnergy} 又无 capacitor 半（ADR 裁定，
 * ITileEntityEnergy.java:55-57）——故 {@link #writeFamilyData} 用一串基类/具体类 instanceof
 * 汇进同一静态缝 {@link #writeConverterData}（GT6CrucibleProvider 双 concrete instanceof 同
 * 姿势的 N 分支版）。子类分支恒在基类分支之前（ZPM 卸电器先于电池箱基类）。读的全是 public
 * 字段/纯读方法（capacity()/capacityFe()），只读禁改 BE（铁律）。
 *
 * <p>同步契约同 {@link GT6MachineProvider}：appendServerData 写 GT6* 前缀键，appendTooltip 经
 * {@code accessor.getServerData()} 读回；本 provider 不写键的 BE（非族成员）在客户端零行，
 * 不碰 BasicMachine/多方块/坩埚/锅炉既有面。跳过如实声明：水车（无可读状态字段）、FE 电池/
 * FE 电源（vanilla BE 岛，storedFe() 形不合本缝）、轴/齿轮箱（旋转传动域，非能量存量语义）、
 * 2x2 反应堆（自有多方块面）、锅炉罐（r5-jade-boiler 卡面）。
 */
public final class GT6ConverterProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {

	public static final GT6ConverterProvider INSTANCE = new GT6ConverterProvider();

	/** 同步键——GT6 前缀命名空间（GT6MachineProvider.java:55 同契约）。 */
	public static final String KEY_HAS_STATUS = "GT6ConverterStatus";
	public static final String KEY_ACTIVE = "GT6ConverterActive";
	public static final String KEY_STOPPED = "GT6ConverterStopped";
	public static final String KEY_STORED = "GT6ConverterStored";
	public static final String KEY_CAPACITY = "GT6ConverterCapacity";
	public static final String KEY_RATE = "GT6ConverterRate";
	public static final String KEY_UNIT = "GT6ConverterUnit";
	public static final String KEY_RATE_UNIT = "GT6ConverterRateUnit";

	/** lang 键（GT6EnUs/GT6ZhCn 双侧同发）。 */
	public static final String LANG_STATUS = "gt6.jade.converter.status";
	public static final String LANG_ACTIVE = "gt6.jade.converter.active";
	public static final String LANG_IDLE = "gt6.jade.converter.idle";
	public static final String LANG_STOPPED = "gt6.jade.converter.stopped";
	public static final String LANG_STORED = "gt6.jade.converter.stored";
	public static final String LANG_STORED_BARE = "gt6.jade.converter.stored.bare";
	public static final String LANG_RATE = "gt6.jade.converter.rate";

	private static final ResourceLocation UID = new ResourceLocation("gt6", "converter_provider");

	private GT6ConverterProvider() {
	}

	@Override
	public ResourceLocation getUid() {
		return UID;
	}

	/**
	 * 族分发（appendServerData 的静态缝——BE 读面离线可测，GT6CrucibleProvider.writeCrucibleData
	 * 同姿势升级为收 BlockEntity 的全族版）。子类在基类前。
	 */
	public static void writeFamilyData(CompoundTag aData, BlockEntity aBE) {
		if (aBE instanceof GTSteamEngineBlockEntity aEngine) {
			// KU 存量/上限/额定（GTSteamEngineBlockEntity.java:129/:131/:133）。
			writeConverterData(aData, true, aEngine.mActive, aEngine.mStopped,
					aEngine.mEnergy, aEngine.mCapacity, aEngine.mOutput, "KU", "KU");
		} else if (aBE instanceof GTDieselEngineBlockEntity aDiesel) {
			// RU 存量/速率（:149/:152）；上限 = mRate*2，即燃烧门 :113/:235 的工作带顶（非硬盖，声明）。
			String tUnit = shortType(aDiesel.mEnergyTypeEmitted);
			writeConverterData(aData, true, aDiesel.mActive, aDiesel.mStopped,
					aDiesel.mEnergy, aDiesel.mRate * 2, aDiesel.mRate, tUnit, tUnit);
		} else if (aBE instanceof GT6ZpmDechargerBlockEntity aZpm) {
			// 缓冲走 QU 进线（GT6ZpmDechargerBlockEntity 类 doc：mEnergyType=QU 进/mEnergyTypeOut 出）。
			writeConverterData(aData, true, aZpm.mActive, aZpm.mStopped,
					aZpm.mEnergy, aZpm.capacity(), aZpm.mOutput, "QU", shortType(aZpm.mEnergyTypeOut));
		} else if (aBE instanceof GT6BatteryBoxBlockEntity aBox) {
			// EU 电池箱（:98/:102/:338）；ZPM 卸电器已被上一分支截走。
			writeConverterData(aData, true, aBox.mActive, aBox.mStopped,
					aBox.mEnergy, aBox.capacity(), aBox.mOutput, "EU", "EU");
		} else if (aBE instanceof GT6ElectricTransformerBlockEntity aTransformer) {
			// EU 双向变压器：mStorage/capacity()（:128/:292），无单一吞吐字段（包大小随档位/方向变）。
			writeConverterData(aData, true, aTransformer.mActive, aTransformer.mStopped,
					aTransformer.mStorage, aTransformer.capacity(), 0, "EU", "");
		} else if (aBE instanceof GT6LaserConverterBlockEntity aLaser) {
			// 激光转换器：出线类型是实例字段（LU/EU/QU 按行），经 getEnergyTypes :161 的 [入,出] 对尾元读。
			writeConverterData(aData, true, aLaser.mActive, aLaser.mStopped,
					aLaser.mStorage, aLaser.capacity(), aLaser.mOutput, "RU", shortType(lastType(aLaser)));
		} else if (aBE instanceof GT6ElectricDynamoBlockEntity aEDynamo) {
			// 出线 EU（GT6ElectricDynamoBlockEntity.java:84）。
			writeConverterData(aData, true, aEDynamo.mActive, aEDynamo.mStopped,
					aEDynamo.mStorage, aEDynamo.capacity(), aEDynamo.mOutput, "RU", "EU");
		} else if (aBE instanceof GT6FluxDynamoBlockEntity aFDynamo) {
			// 出线 RF（GT6FluxDynamoBlockEntity.java:94）。
			writeConverterData(aData, true, aFDynamo.mActive, aFDynamo.mStopped,
					aFDynamo.mStorage, aFDynamo.capacity(), aFDynamo.mOutput, "RU", "RF");
		} else if (aBE instanceof GT6DynamoBlockEntity aDynamo) {
			// 动力机基类剩余成员（现实装为无）：RU 缓冲（基类 :92）+ 出线类型随子类。
			writeConverterData(aData, true, aDynamo.mActive, aDynamo.mStopped,
					aDynamo.mStorage, aDynamo.capacity(), aDynamo.mOutput, "RU", shortType(lastType(aDynamo)));
		} else if (aBE instanceof GT6LongDistanceTransformerBlockEntity aLd) {
			// LD 变压器：无缓冲，行显实际线吞吐（:90，0 = 未扫描时回退额定包大小）。
			writeConverterData(aData, true, aLd.mActive, aLd.mStopped,
					0, 0, aLd.mThroughput > 0 ? aLd.mThroughput : aLd.mOutput, "", "EU");
		} else if (aBE instanceof GTTransformerRotationBlockEntity aRt) {
			// 旋转变压器：STORAGE_CAPACITY = tInput*2 常量（:91）。
			writeConverterData(aData, true, aRt.mActive, aRt.mStopped,
					aRt.mStorage, GTTransformerRotationBlockEntity.STORAGE_CAPACITY, 0, "RU", "");
		} else if (aBE instanceof GT6MagicAbsorberBlockEntity aAbsorber) {
			// 魔法吸收器：无缓冲，mOutput 随状态 1/64（:164-166），出线 QU/TU（:98）。
			writeConverterData(aData, true, aAbsorber.mActive, aAbsorber.mStopped,
					0, 0, aAbsorber.mOutput, "", shortType(aAbsorber.mEnergyTypeEmitted));
		} else if (aBE instanceof GTGeneratorSolidBlockEntity aBurning) {
			// 燃烧箱族（solid/liquid/gas/fluid-bed）：mBurning 即状态位（:128），HU 存量无硬盖（裸存量形）。
			writeConverterData(aData, true, aBurning.mBurning, false,
					aBurning.mEnergy, 0, aBurning.mRate, "HU", "HU");
		} else if (aBE instanceof GTCrankBlockEntity aCrank) {
			// 手摇曲柄：只有 mActive（:105）。
			writeConverterData(aData, true, aCrank.mActive, false, 0, 0, 0, "", "");
		} else if (aBE instanceof GTEnergySourceBlockEntity aSource) {
			// 能能源：mEmitting 即状态（:98），额定 = 包大小×包数（:92-95 语义），类型随 p11 字段（:106）。
			writeConverterData(aData, true, aSource.mEmitting, false,
					0, 0, aSource.mVoltage * aSource.mAmperage, "", shortType(aSource.mEnergyType));
		} else if (aBE instanceof GT6FeConverterBlockEntity aFe) {
			// FE 转换机：FE 缓冲（:138/:170）+ EU 出线（:135）——无状态位族。
			writeConverterData(aData, false, false, false,
					aFe.mBufferFe, aFe.capacityFe(), aFe.mVoltage, "FE", "EU");
		}
		// 非族成员不写键——客户端门 KEY_UNIT 不存在即零行（GT6CrucibleProvider 同门）。
	}

	/** {@link GT6DynamoBlockEntity#getEnergyTypes} 对（RU, 出线）的尾元——出线类型读缝。 */
	private static TagData lastType(GT6DynamoBlockEntity aDynamo) {
		TagData rType = TD.Energy.EU;
		for (TagData tType : aDynamo.getEnergyTypes((byte) 6)) rType = tType;
		return rType;
	}

	/**
	 * 统一同步写。{@code aHasStatus}=族内有状态位（FE 转换机无）；{@code aCapacity}>0 才有
	 * 存量 X/Y 行，否则存量>0 走裸存量行；{@code aRate}>0 才有吞吐行；空 unit 串 = 该行不适用。
	 */
	public static void writeConverterData(CompoundTag aData, boolean aHasStatus, boolean aActive, boolean aStopped,
			long aStored, long aCapacity, long aRate, String aUnit, String aRateUnit) {
		aData.putBoolean(KEY_HAS_STATUS, aHasStatus);
		aData.putBoolean(KEY_ACTIVE, aActive);
		aData.putBoolean(KEY_STOPPED, aStopped);
		aData.putLong(KEY_STORED, aStored);
		aData.putLong(KEY_CAPACITY, aCapacity);
		aData.putLong(KEY_RATE, aRate);
		aData.putString(KEY_UNIT, aUnit);
		aData.putString(KEY_RATE_UNIT, aRateUnit);
	}

	@Override
	public void appendServerData(CompoundTag aData, BlockAccessor aAccessor) {
		writeFamilyData(aData, aAccessor.getBlockEntity());
	}

	@Override
	public void appendTooltip(ITooltip aTooltip, BlockAccessor aAccessor, IPluginConfig aConfig) {
		CompoundTag aData = aAccessor.getServerData();
		if (!aData.contains(KEY_UNIT)) {
			return; // 非转换器族（本 provider 挂全 GT6 BE 面，键存在即族内——GT6CrucibleProvider 同门）
		}
		if (aData.getBoolean(KEY_HAS_STATUS)) {
			aTooltip.add(statusLine(aData.getBoolean(KEY_ACTIVE), aData.getBoolean(KEY_STOPPED)));
		}
		long tStored = aData.getLong(KEY_STORED);
		long tCapacity = aData.getLong(KEY_CAPACITY);
		String tUnit = aData.getString(KEY_UNIT);
		if (tCapacity > 0) {
			// B 形条（task r8-jade-converter-crucible-restyle）：真上限族出钳位比例条，文本复用
			// 既有三槽键（lang 裁定 gt6.jade.converter.* 不动——既有键不退役）；条面中性白，
			// 状态语义已在状态行着色（GT6BoilerProvider 水/汽条同形）。条本体组装 live-only
			// （IElementHelper 需客户端），离线钉 = 线契约 + 行函数 + GT6JadeRows.ratio。
			GT6JadeRows.bar(aTooltip, GT6JadeRows.ratio(tStored, tCapacity),
					storedLine(tStored, tCapacity, tUnit), GT6JadeRows.COLOR_NEUTRAL);
		} else if (tStored > 0) {
			aTooltip.add(storedBareLine(tStored, tUnit));
		}
		long tRate = aData.getLong(KEY_RATE);
		if (tRate > 0) {
			aTooltip.add(rateLine(tRate, aData.getString(KEY_RATE_UNIT)));
		}
	}

	/** 状态行：停机（软停）RED &gt; 运行 GREEN &gt; 待机默认色（字色常量收编
	 * {@link GT6JadeRows#FORMAT_OK}/{@link GT6JadeRows#FORMAT_STALLED}——J1 遗留的第三份复制）。 */
	public static Component statusLine(boolean aActive, boolean aStopped) {
		if (aStopped) return Component.translatable(LANG_STATUS, Component.translatable(LANG_STOPPED))
				.withStyle(GT6JadeRows.FORMAT_STALLED);
		MutableComponent rLine = Component.translatable(LANG_STATUS, Component.translatable(
				aActive ? LANG_ACTIVE : LANG_IDLE));
		return aActive ? rLine.withStyle(GT6JadeRows.FORMAT_OK) : rLine;
	}

	/** 存量行（有上限）："Stored: 100 / 200 KU"。 */
	public static Component storedLine(long aStored, long aCapacity, String aUnit) {
		return Component.translatable(LANG_STORED, aStored, aCapacity, aUnit);
	}

	/** 裸存量行（无硬上限族，燃烧箱）："Stored: 250 HU"。 */
	public static Component storedBareLine(long aStored, String aUnit) {
		return Component.translatable(LANG_STORED_BARE, aStored, aUnit);
	}

	/** 吞吐行（额定输出）："Output: 64 EU/t"（GTCEu RecipeLogicProvider.java:75+ 先例形）。 */
	public static Component rateLine(long aRate, String aUnit) {
		return Component.translatable(LANG_RATE, aRate, aUnit);
	}

	/**
	 * 能量类型 → 单位短词。移植侧 TagData 的 local short 未落地（createTagData 丢弃，
	 * TagData.java:62-64；lang 无 gt.td.short.* 行）——provider 内最小身份表，覆盖本族
	 * 实际在用的八型，未知回退 mName。
	 */
	public static String shortType(TagData aType) {
		if (aType == TD.Energy.EU) return "EU";
		if (aType == TD.Energy.RU) return "RU";
		if (aType == TD.Energy.KU) return "KU";
		if (aType == TD.Energy.HU) return "HU";
		if (aType == TD.Energy.RF) return "RF";
		if (aType == TD.Energy.QU) return "QU";
		if (aType == TD.Energy.TU) return "TU";
		if (aType == TD.Energy.LU) return "LU";
		return aType.mName;
	}
}
