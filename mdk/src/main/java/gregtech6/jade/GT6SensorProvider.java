package gregtech6.jade;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import gregtech6.tileentity.sensors.GTSensorBlockEntity;
import gregtech6.tileentity.sensors.GTSensorLogic;

/**
 * GT6 传感器族 Jade 显示面（task jade-sensor-provider，design.r8-jade-tooltip
 * families.sensor）：21 个传感器（progressmeter/thermometer/… 全族）一个 provider——
 * 全族 BE 经抽象基 {@link GTSensorBlockEntity} 一个 instanceof 覆盖，行件复用
 * {@link GT6JadeRows} 的键纪律。取数缝 = 公开 getter 只读（字段 protected：
 * GTSensorBlockEntity.java:90-106——mDisplayedNumber/mMode/mRedstone/mCurrentValue/
 * mCurrentMax/mSecondFacing，卡面纪律"只读不加字段"），{@code writeSensorData} 静态缝
 * = r5 家法（GT6CrucibleProvider/GT6BoilerProvider 同姿势，BE 直入便于离线测非传感器
 * 零键门）。行面（design 行序）：
 * <ol>
 * <li><b>读数行</b>——"Reading: %s"，mDisplayedNumber（:93）；hex 模式（打包位 bit7
 *     :87-88，{@link GTSensorLogic#isHexMode}）修饰为 4 位大写十六进制 {@code 0x%04X}
 *     ——上游显示板 hex 面的措辞位（SensorTE:262 CHAR_HEX/digits）。</li>
 * <li><b>模式行</b>——"Mode: %s"，槽 = 8 模式名 lang 键（gt6.jade.sensor.mode.*，上游
 *     零文字模式名——仅 CHAR_* 图标，SensorTE:255-262——行名自拟）；打包字节经
 *     {@link GTSensorLogic#modeOf} 掩码，越界字节（NBT 垃圾）%8 折回。</li>
 * <li><b>红石行</b>——"Redstone: %s"，mRedstone（:96，0..15 bind4）。</li>
 * <li><b>潜行明细两行</b>——采样 "Sample: %s / %s"（mCurrentValue/mCurrentMax :100）+
 *     探测面 "Probe Face: %s"（mSecondFacing :106，面名 = Direction.getName）。</li>
 * </ol>
 *
 * <p>同步契约同 {@link GT6BoilerProvider}：appendServerData 写 GT6* 前缀键，appendTooltip
 * 经 {@code accessor.getServerData()} 读回，只读公开字段禁改 BE（铁律）。非传感器 BE 挂
 * 同一 {@code TileEntityBase01Root} 注册面（GT6JadePlugin 追加对），键存在即传感器族
 * （{@code KEY_DISPLAYED} 门）。lang 键 {@code gt6.jade.sensor.*} 四落（GT6EnUs
 * addSensorJade + zh_cn_ref.tsv hand 带 + py HAND_TRANSLATIONS + GT6ZhCn
 * addSensorJadeUnits），零裸 literal（GT6JadeTooltipKeyPinTest 收编本类）。
 */
public final class GT6SensorProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {

	public static final GT6SensorProvider INSTANCE = new GT6SensorProvider();

	/** 同步键（六键——design payload_rules sensor=6 键；GT6BoilerProvider.java:51 同契约）。 */
	public static final String KEY_DISPLAYED = "GT6SensorDisplayed";
	public static final String KEY_MODE = "GT6SensorMode";
	public static final String KEY_REDSTONE = "GT6SensorRedstone";
	public static final String KEY_VALUE = "GT6SensorValue";
	public static final String KEY_MAX = "GT6SensorMax";
	public static final String KEY_PROBE = "GT6SensorProbe";

	/** 读数行 lang 键：槽 = 十进制数（hex 模式 = {@code 0x%04X} 修饰串，双侧同发）。 */
	public static final String LANG_READING = "gt6.jade.sensor.reading";
	/** 模式行 lang 键：槽 = 8 模式名键的嵌套 translatable。 */
	public static final String LANG_MODE = "gt6.jade.sensor.mode";
	/** 红石行 lang 键：槽 = 输出强度 0..15。 */
	public static final String LANG_REDSTONE = "gt6.jade.sensor.redstone";
	/** 潜行采样行 lang 键：槽 = mCurrentValue / mCurrentMax。 */
	public static final String LANG_SAMPLE = "gt6.jade.sensor.sample";
	/** 潜行探测面行 lang 键：槽 = 面名（Direction.getName，RCON read 通道同措辞）。 */
	public static final String LANG_PROBE = "gt6.jade.sensor.probe";

	/**
	 * 8 模式名键，索引 = {@link GTSensorLogic} 模式常量序（DISPLAY/PERCENT/GREATER/EQUAL/
	 * SMALLER/SCALE/FULL/NOT_FULL——SensorTE:50-58）。上游零文字名（仅显示板图标），行名
	 * 自拟；越界模式字节 %8 折回（appendTooltip 端与 NBT 垃圾隔离）。
	 */
	private static final String[] MODE_NAME_KEYS = {
			  "gt6.jade.sensor.mode.display"
			, "gt6.jade.sensor.mode.percent"
			, "gt6.jade.sensor.mode.greater"
			, "gt6.jade.sensor.mode.equal"
			, "gt6.jade.sensor.mode.smaller"
			, "gt6.jade.sensor.mode.scale"
			, "gt6.jade.sensor.mode.full"
			, "gt6.jade.sensor.mode.not_full"
	};

	/** Provider uid（GT6BoilerProvider.java:74 同形）。 */
	private static final ResourceLocation UID = new ResourceLocation("gt6", "sensor_provider");

	private GT6SensorProvider() {
	}

	@Override
	public ResourceLocation getUid() {
		return UID;
	}

	@Override
	public void appendServerData(CompoundTag aData, BlockAccessor aAccessor) {
		writeSensorData(aData, aAccessor.getBlockEntity());
	}

	/**
	 * 传感器族同步写（appendServerData 的静态缝——r5 家法 + converter 的 BE 直入形）：
	 * 全经公开 getter（字段 protected，跨包不可达——GTSensorBlockEntity.java:258-300 getter
	 * 面就是取数缝）；非传感器 BE 零键 = 客户端键门保持关（GT6ConverterProviderTest
	 * skippedFamiliesWriteNoKeys 同契约）。
	 */
	public static void writeSensorData(CompoundTag aData, BlockEntity aEntity) {
		if (aEntity instanceof GTSensorBlockEntity aSensor) {
			aData.putInt(KEY_DISPLAYED, aSensor.getDisplayedNumber());
			aData.putInt(KEY_MODE, aSensor.getMode());
			aData.putByte(KEY_REDSTONE, aSensor.getRedstone());
			aData.putInt(KEY_VALUE, aSensor.getCurrentValue());
			aData.putInt(KEY_MAX, aSensor.getCurrentMax());
			aData.putByte(KEY_PROBE, aSensor.getSecondFacing());
		}
	}

	@Override
	public void appendTooltip(ITooltip aTooltip, BlockAccessor aAccessor, IPluginConfig aConfig) {
		CompoundTag aData = aAccessor.getServerData();
		if (!aData.contains(KEY_DISPLAYED)) {
			return; // 非传感器（本 provider 挂全 GT6 BE 面，键存在即传感器族——GT6BoilerProvider 同门）
		}
		int tMode = aData.getInt(KEY_MODE);
		// ① 读数行（hex 模式 0x%04X 修饰）。
		aTooltip.add(readingLine(aData.getInt(KEY_DISPLAYED), GTSensorLogic.isHexMode(tMode)));
		// ② 模式行（8 模式名嵌套）。
		aTooltip.add(modeLine(tMode));
		// ③ 红石行。
		aTooltip.add(redstoneLine(aData.getByte(KEY_REDSTONE)));
		// ④ 潜行明细两行 = 采样 + 探测面。
		if (aAccessor.showDetails()) {
			aTooltip.add(sampleLine(aData.getInt(KEY_VALUE), aData.getInt(KEY_MAX)));
			aTooltip.add(probeLine(aData.getByte(KEY_PROBE)));
		}
	}

	/** 读数行（纯函数离线面）：hex 模式 = 4 位大写十六进制修饰（上游显示板 hex 面），否则十进制。 */
	public static Component readingLine(int aDisplayed, boolean aHex) {
		return Component.translatable(LANG_READING, aHex ? String.format("0x%04X", aDisplayed) : Integer.toString(aDisplayed));
	}

	/** 模式行（纯函数离线面）：槽 = 模式名键的嵌套 translatable，掩码 + %8 折回。 */
	public static Component modeLine(int aPackedMode) {
		return Component.translatable(LANG_MODE,
				Component.translatable(MODE_NAME_KEYS[GTSensorLogic.modeOf(aPackedMode) % MODE_NAME_KEYS.length]));
	}

	/** 红石行（纯函数离线面）：0..15 强度。 */
	public static Component redstoneLine(byte aRedstone) {
		return Component.translatable(LANG_REDSTONE, (int)aRedstone);
	}

	/** 潜行采样行（纯函数离线面）：mCurrentValue / mCurrentMax。 */
	public static Component sampleLine(int aValue, int aMax) {
		return Component.translatable(LANG_SAMPLE, aValue, aMax);
	}

	/** 潜行探测面行（纯函数离线面）：面名 = Direction.getName（"north" 形，与 /gt6sensor read 通道同措辞）。 */
	public static Component probeLine(byte aProbe) {
		return Component.translatable(LANG_PROBE, Direction.from3DDataValue(aProbe).getName());
	}

}
