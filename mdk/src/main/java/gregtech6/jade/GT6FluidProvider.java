package gregtech6.jade;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.fluid.JadeFluidObject;
import snownee.jade.api.ui.IElement;
import snownee.jade.api.ui.IElementHelper;
import snownee.jade.util.CommonProxy;
import snownee.jade.util.FluidTextHelper;

import net.minecraftforge.fluids.FluidStack;

import gregtech6.fluid.FluidTankGT;
import gregtech6.tileentity.machines.TileEntityBasicMachine;

/**
 * GT6 机器流体段 Jade provider（task p22-jade-fluid-tooltip）：与 {@link GT6MachineProvider}
 * 同构的双腿对（client {@link IBlockComponentProvider} + server {@link IServerDataProvider}
 * &lt;BlockAccessor&gt;），渲染机器输入/输出罐的图标+量行。
 *
 * <p>数据缝 = Jade 服务端推（研究卡裁定）：机器罐内容不进 vanilla 同步——onFluidIO→
 * onInventoryChanged 只 setChanged+mInventoryChanged（TileEntityBasicMachine.java:422-425），
 * onTickCheck 只查 ACTIVE/RUNNING 视觉位（:402-405），fill/drain 均不触发客户端数据→
 * 客户端 BE 罐内容陈旧；Jade appendServerData 每 hover 请求新鲜（jade-1201 ClientProxy.java:218 /
 * jade-1211 :253 RequestBlockPacket）。数据源 = {@code mTanksInput}/{@code mTanksOutput}
 * public final（TileEntityBasicMachine.java:285/:287）。只读公开字段，禁改 BE（铁律）。
 *
 * <p>单分支 = 架构复核 C-1（tasks.p22-arch-feature-wave）：多方块
 * TileEntityBase10MultiBlockMachine 全文 168 行零罐字段，罐面唯一 public 声明就是
 * BasicMachine 本类——不做两分支。
 *
 * <p>载荷 = 自描述键 {@code GT6FluidsIn/GT6FluidsOut}（ListTag of CompoundTag）：
 * {@code FluidName}（注册表名）+ {@code Amount}（<b>真 long</b>，走 {@link FluidTankGT#amount()}
 * ——port 公开 IFluidTank 面是 bindInt 钳位（:256/:261），内部量才是 63 位）+ {@code Capacity}
 * （真 long）。空罐（无流体身份）不写条目。GT6* 前缀键空间隔离，形同
 * {@link GT6MachineProvider} 的同步契约。
 *
 * <p>客户端 = v1 内嵌形态（架构裁定，universal registerFluidStorage 入池——IServerExtensionProvider
 * 是双腿唯一大叉）：每罐 {@link IElementHelper#fluid(JadeFluidObject)} 图元 + 同行文本
 * （GTCEu RecipeOutputProvider.java:228-229 add+append 同行形）。overlay 载体钉
 * {@code JadeFluidObject.of(fluid, 1000)}——FluidView#readDefault 吃不下 &gt;INT_MAX 量
 * （GTCEu GTFluidStorageProvider.java:84-85 先例同姿势），真实 long 量只走文本行：
 * {@link FluidTextHelper#getUnicodeMillibuckets(long, boolean)}（jade-1201 :9 / jade-1211 :7
 * 双腿同形）+ {@link CommonProxy#getFluidName(JadeFluidObject)}（:451/:470 双腿同形），
 * 名字括号包 = vanilla ComponentUtils.wrapInSquareBrackets（GTCEu 同款）。
 *
 * <p>双腿零 chisel（除 {@link #rawFluid} 四行）：IElementHelper.fluid（1201:29/1211:30）、
 * JadeFluidObject.of(Fluid,long)（1201:30-32/1211:37-39）逐字同形。
 */
public final class GT6FluidProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {

	public static final GT6FluidProvider INSTANCE = new GT6FluidProvider();

	/** 同步键——GT6 前缀命名空间（与 {@link GT6MachineProvider} 同理：tag 可能已被其他 provider 处理过）。 */
	public static final String KEY_FLUIDS_IN = "GT6FluidsIn";
	public static final String KEY_FLUIDS_OUT = "GT6FluidsOut";

	/** 罐条目自描述键（FluidTankGT 合同键形 FluidName/Amount 同名；Amount/Capacity 恒 long）。 */
	public static final String KEY_FLUID_NAME = "FluidName";
	public static final String KEY_AMOUNT = "Amount";
	public static final String KEY_CAPACITY = "Capacity";

	/** overlay 载体量 = 一桶（GTCEu GTFluidStorageProvider.java:96 同字面 1000）。 */
	private static final long CARRIER_MILLIBUCKETS = 1000;

	/** Provider uid（IJadeProvider.java:10 双腿抽象 getUid）——与 {@link GT6MachineProvider#UID} 异值。 */
	private static final ResourceLocation UID = new ResourceLocation("gt6", "fluid_provider");

	private GT6FluidProvider() {
	}

	@Override
	public ResourceLocation getUid() {
		return UID;
	}

	@Override
	public void appendServerData(CompoundTag aData, BlockAccessor aAccessor) {
		// 单分支（类 doc C-1）：只有 TileEntityBasicMachine 带罐面。
		if (!(aAccessor.getBlockEntity() instanceof TileEntityBasicMachine aMachine)) {
			return;
		}
		aData.put(KEY_FLUIDS_IN, tanksTag(aMachine.mTanksInput));
		aData.put(KEY_FLUIDS_OUT, tanksTag(aMachine.mTanksOutput));
	}

	/**
	 * 罐组 → 自描述 ListTag。空罐（{@link FluidTankGT#fluid()} == null）不产条目；
	 * Amount/Capacity 走 long 面（{@link FluidTankGT#amount()}/{@link FluidTankGT#capacity()}），
	 * 绕开 IFluidTank 的 bindInt 钳位——GT6 长量（&gt;Integer.MAX_VALUE）原值过缝。
	 */
	public static ListTag tanksTag(FluidTankGT[] aTanks) {
		ListTag tList = new ListTag();
		for (FluidTankGT tTank : aTanks) {
			FluidStack tStack = tTank.fluid();
			if (tStack == null) continue;
			Fluid tFluid = rawFluid(tStack);
			if (tFluid == null || tFluid == Fluids.EMPTY) continue;
			ResourceLocation tName = BuiltInRegistries.FLUID.getKey(tFluid);
			if (tName == null) continue;
			CompoundTag tTag = new CompoundTag();
			tTag.putString(KEY_FLUID_NAME, tName.toString());
			tTag.putLong(KEY_AMOUNT, tTank.amount()); // upstream FluidTankGT:330 形（port :399）
			tTag.putLong(KEY_CAPACITY, tTank.capacity());
			tList.add(tTag);
		}
		return tList;
	}

	@Override
	public void appendTooltip(ITooltip aTooltip, BlockAccessor aAccessor, IPluginConfig aConfig) {
		CompoundTag aData = aAccessor.getServerData();
		tankGroup(aTooltip, aData, KEY_FLUIDS_IN, "Fluid In");
		tankGroup(aTooltip, aData, KEY_FLUIDS_OUT, "Fluid Out");
	}

	/**
	 * 一组罐的渲染：组标签行（仅组非空）+ 每罐「图标+量行」同行
	 * （GTCEu RecipeOutputProvider.java:228-229 的 add+append 形）。
	 */
	private static void tankGroup(ITooltip aTooltip, CompoundTag aData, String aKey, String aLabel) {
		if (!aData.contains(aKey, Tag.TAG_LIST)) return;
		ListTag tList = aData.getList(aKey, Tag.TAG_COMPOUND);
		if (tList.isEmpty()) return;
		aTooltip.add(Component.literal(aLabel + ":").withStyle(ChatFormatting.GRAY));
		IElementHelper tHelper = IElementHelper.get();
		for (int i = 0; i < tList.size(); i++) {
			CompoundTag tTag = tList.getCompound(i);
			Fluid tFluid = resolveFluid(tTag.getString(KEY_FLUID_NAME));
			if (tFluid == null) continue;
			long tAmount = tTag.getLong(KEY_AMOUNT), tCapacity = tTag.getLong(KEY_CAPACITY);
			// overlay 载体钉一桶量——readDefault 的 INT_MAX 坑（类 doc），真实 long 只走文本。
			JadeFluidObject tCarrier = JadeFluidObject.of(tFluid, CARRIER_MILLIBUCKETS);
			Component tText = tankText(lineText(tAmount, tCapacity), CommonProxy.getFluidName(tCarrier));
			IElement tIcon = tHelper.fluid(tCarrier);
			aTooltip.add(tIcon);
			aTooltip.append(tText);
		}
	}

	/** 真实 long 量行文本——Jade 自家的 mB unicode 面（FluidTextHelper 双腿 :9/:7 同形）。 */
	static String lineText(long aAmount, long aCapacity) {
		return FluidTextHelper.getUnicodeMillibuckets(aAmount, true) + " / " + FluidTextHelper.getUnicodeMillibuckets(aCapacity, true);
	}

	/**
	 * v1 行形 = 「量 名字（方括号）」——纯函数（离线单测面）：两段 Jade 活体件
	 * （{@link #lineText(long, long)} 的量串 + {@link CommonProxy#getFluidName} 的名字）在此定型。
	 */
	static Component tankText(String aAmounts, Component aName) {
		return Component.empty()
				.append(aAmounts)
				.append(" ")
				.append(ComponentUtils.wrapInSquareBrackets(aName));
	}

	/** FluidName 反查注册表（形同 FluidTankGT 21.1 keepFilter 读侧：tryParse null / 未知名 / EMPTY 全拒）。 */
	private static Fluid resolveFluid(String aName) {
		ResourceLocation tName = ResourceLocation.tryParse(aName);
		if (tName == null) return null;
		Fluid tFluid = BuiltInRegistries.FLUID.get(tName);
		return tFluid == null || tFluid == Fluids.EMPTY ? null : tFluid;
	}

	/**
	 * 空旗Proof 的流体身份面——{@code FluidTankGT.writeToNBT} :161（forge getRawFluid）/
	 * :163（21.1 getFluid，活栈身份缓存不塌缩）同一 swap；本 provider 不读 keepFilter 态
	 * （机器罐无 mPreventDraining），仅为防御性一致。
	 */
	//? if forge {
	private static Fluid rawFluid(FluidStack aStack) {
		return aStack.getRawFluid();
	}
	//?} else {
	/*private static Fluid rawFluid(FluidStack aStack) {
		return aStack.getFluid(); // 21.1: getRawFluid 已随组件化删除——活栈在任意 amount 下身份不变
	}
	*///?}

}
