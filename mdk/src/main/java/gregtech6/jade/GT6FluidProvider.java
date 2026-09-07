package gregtech6.jade;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import snownee.jade.api.Accessor;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.TooltipPosition;
import snownee.jade.api.fluid.JadeFluidObject;
import snownee.jade.api.ui.IElementHelper;
import snownee.jade.api.view.ClientViewGroup;
import snownee.jade.api.view.FluidView;
import snownee.jade.api.view.IClientExtensionProvider;
import snownee.jade.api.view.IServerExtensionProvider;
import snownee.jade.api.view.ViewGroup;
import snownee.jade.util.CommonProxy;
import snownee.jade.util.FluidTextHelper;

import net.minecraftforge.fluids.FluidStack;

import gregtech6.fluid.FluidTankGT;
import gregtech6.tileentity.TileEntityBase01Root;
import gregtech6.tileentity.machines.TileEntityBasicMachine;

/**
 * GT6 机器流体段 Jade universal provider（task p23-jade-universal-fluid，升级 P22 v1 b9b0b23f）：
 * GTCEu {@code GTFluidStorageProvider} 全例姿势（gtceu-modern .../jade/provider/GTFluidStorageProvider.java:40-101）
 * ——一个单例对象双腿同体实现 {@code IServerExtensionProvider<...CompoundTag>}（服务端取数，
 * {@code registerFluidStorage} 注册；Jade 自家 universal {@code FluidStorageProvider} 是数据载体，
 * 服务端 hover 时按 priority 顺序逐 provider 试、首个非 null 赢并写
 * {@code JadeFluidStorage/JadeFluidStorageUid}——jade-1201 addon/universal/FluidStorageProvider.java:81-88
 * / jade-1211 util/CommonProxy.java:586-602）+ {@code IClientExtensionProvider<CompoundTag, FluidView>}
 * （客户端手工 parse，{@code registerFluidStorageClient} 注册，Jade 按 uid 从 client map 找回——
 * jade-1201 :44-46 / jade-1211 :67-68）。自写 tooltip 渲染面整体退役：行渲染（图标+文本+进度条
 * 样式）由 Jade 自家 append 消化（jade-1201 :53-71 / jade-1211 :87-135），P21 的
 * BoxStyle/progressStyle swap 不再需要。
 *
 * <p>分叉声明（双腿唯一）：{@code IServerExtensionProvider} 泛型与方法签名——1201 双泛型
 * {@code <IN,OUT>} + {@code getGroups(ServerPlayer,ServerLevel,IN,boolean)}
 * （jade-1201 api/view/IServerExtensionProvider.java:11-14）vs 1211 单泛型 {@code <T>} +
 * {@code getGroups(Accessor)} + default shouldRequestData（jade-1211 :10-17）。类头 implements
 * 子句与 getGroups 随分叉（类声明分叉形 = GTBarrelItemFluidHandler.java:54-58 先例）。
 * 其余 API 双腿逐字同形零分叉：IClientExtensionProvider（两腿 :8-11 同文）、
 * ViewGroup/ClientViewGroup.map、FluidView（public ctor+字段）、JadeFluidObject.of(Fluid,long)
 * （1201 :30-32 / 1211 :37-39）、CommonProxy.getFluidName（:451/:470）、
 * FluidTextHelper.getUnicodeMillibuckets（:9/:7）。
 */
//? if forge {
public final class GT6FluidProvider implements IServerExtensionProvider<TileEntityBase01Root, CompoundTag>, IClientExtensionProvider<CompoundTag, FluidView> {
//?} else {
/*public final class GT6FluidProvider implements IServerExtensionProvider<CompoundTag>, IClientExtensionProvider<CompoundTag, FluidView> {
*///?}

	public static final GT6FluidProvider INSTANCE = new GT6FluidProvider();

	/**
	 * 罐条目自描述键（P22 v1 合同键沿用——FluidTankGT 合同键形 FluidName/Amount 同名；
	 * Amount/Capacity 恒 long）。载荷由 Jade universal 链搬运：服务端
	 * {@code ViewGroup.saveList(tag,"JadeFluidStorage",groups,identity())}（jade-1201
	 * addon/universal/FluidStorageProvider.java:84 / jade-1211 :146），键空间归 Jade。
	 */
	public static final String KEY_FLUID_NAME = "FluidName";
	public static final String KEY_AMOUNT = "Amount";
	public static final String KEY_CAPACITY = "Capacity";

	/** 组 id（进 {@link ViewGroup#id}，客户端 decorator 转 {@code ClientViewGroup.title}——Jade 自家例同款 j-1201 test/ExampleFluidStorageProvider.java:31-36）。 */
	public static final String GROUP_IN = "Fluid In";
	public static final String GROUP_OUT = "Fluid Out";

	/** overlay 载体量 = 一桶（GTCEu GTFluidStorageProvider.java:96 同字面 1000）。 */
	private static final long CARRIER_MILLIBUCKETS = 1000;

	/**
	 * Provider uid。v1（block component 面）沿用过的 gt6:fluid_provider 语义已随 v1 退役；
	 * universal 链下它 = 服务端写的 {@code JadeFluidStorageUid} 与客户端 provider map 的对合键。
	 */
	private static final ResourceLocation UID = new ResourceLocation("gt6", "fluid_storage");

	/**
	 * 客户端 decorator：ViewGroup.id → 组标题（双腿 ClientViewGroup.map 第三参同形；
	 * Jade 自家渲染 renderGroup 时才画标题/盒——组空 views 在服务端 saveList 已被 skip
	 * （jade-1201 api/view/ViewGroup.java:66-68 / jade-1211 :97-99），过缝的组恒非空）。
	 */
	private static final BiConsumer<ViewGroup<CompoundTag>, ClientViewGroup<FluidView>> GROUP_DECORATOR = (aGroup, aClientGroup) -> {
		if (aGroup.id != null) {
			aClientGroup.title = Component.literal(aGroup.id);
		}
	};

	private GT6FluidProvider() {
	}

	@Override
	public ResourceLocation getUid() {
		return UID;
	}

	/**
	 * 抢跑裁定（双腿同形）：Jade universal 链服务端是「按 priority 升序逐 provider 试、
	 * 首个非 null 赢并短路」（jade-1201 addon/universal/FluidStorageProvider.java:81-88、
	 * jade-1211 util/CommonProxy.java:589-599；COMPARATOR=paringInt(priority) 稳定排序，
	 * jade-1211 impl/lookup/IHierarchyLookup.java:24，wrappedGet 合桶后按它排序返回
	 * jade-1211 impl/lookup/WrappedHierarchyLookup.java:45-54）。Jade 自家流体 capability
	 * 面不落 IJadeProvider 的 BODY 默认层（jade-1211 api/IJadeProvider.java:18-20）——双腿
	 * 都覆写且都在 BODY 之上，数值是 Jade 默认层而非 BODY 派生（钉版 jar javap 实测，P23 S2
	 * 复核）：forge 腿 11.13.3 的 FluidStorageProvider enum（即 IServerExtensionProvider
	 * 本尊，capability int 面）= BODY+1000（=1000，字节码 sipush 1000）；1211 线（钉版
	 * 15.10.6）capability Extension 枚举覆写 = 9999（jade-1211
	 * addon/universal/FluidStorageProvider.java:203-205），tooltip 载体 ForBlock 继承基类
	 * = BODY+1000（:156-158）。GT6 机器挂了 FLUID_HANDLER capability
	 * （TileEntityBasicMachine.java:1387），若让 Jade 默认面先赢，其量走 IFluidHandler
	 * int 面（bindInt 钳位，正是本 provider 要绕的）——BODY-1 双腿显式插队 universal
	 * 流体链最前，long 量原值过缝。
	 */
	@Override
	public int getDefaultPriority() {
		return TooltipPosition.BODY - 1;
	}

	//? if forge {
	@Override
	public List<ViewGroup<CompoundTag>> getGroups(net.minecraft.server.level.ServerPlayer aPlayer, net.minecraft.server.level.ServerLevel aLevel,
			TileEntityBase01Root aTarget, boolean aShowDetails) {
		return groupsOfTarget(aTarget);
	}
	//?} else {
	/*// 1211 分叉腿：target 语义并入 accessor（wrappedGet 已按 accessor.getTarget() 匹配过注册类，
	// 非方块 accessor 的 target 不在 TileEntityBase01Root 层级，instanceof 是第二道保险）。
	@Override
	public List<ViewGroup<CompoundTag>> getGroups(Accessor<?> aAccessor) {
		return aAccessor instanceof BlockAccessor aBlock ? groupsOfTarget(aBlock.getBlockEntity()) : null;
	}
	*///?}

	/**
	 * 服务端取数语义（与 v1 共用，离线单测面）：单分支（类 doc C-1，tasks.p22-arch-feature-wave——
	 * 多方块 TileEntityBase10MultiBlockMachine 全文 168 行零罐字段）——只有
	 * {@link TileEntityBasicMachine} 带罐面；非机器返回 null（不短路 Jade 默认 provider）。
	 * 数据源 = {@code mTanksInput}/{@code mTanksOutput} public final
	 * （TileEntityBasicMachine.java:285/:287），只读公开字段，禁改 BE（铁律）。
	 */
	public static List<ViewGroup<CompoundTag>> groupsOfTarget(Object aTarget) {
		if (!(aTarget instanceof TileEntityBasicMachine aMachine)) {
			return null;
		}
		ViewGroup<CompoundTag> tIn = new ViewGroup<>(tankViews(aMachine.mTanksInput));
		tIn.id = GROUP_IN;
		ViewGroup<CompoundTag> tOut = new ViewGroup<>(tankViews(aMachine.mTanksOutput));
		tOut.id = GROUP_OUT;
		return List.of(tIn, tOut);
	}

	/**
	 * 罐组 → 自描述 CompoundTag 视图列表。空罐（{@link FluidTankGT#fluid()} == null）不产条目；
	 * Amount/Capacity 走 long 面（{@link FluidTankGT#amount()}/{@link FluidTankGT#capacity()}），
	 * 绕开 IFluidTank 的 bindInt 钳位（:256/:261）——GT6 长量（&gt;Integer.MAX_VALUE）原值过缝。
	 */
	public static List<CompoundTag> tankViews(FluidTankGT[] aTanks) {
		ArrayList<CompoundTag> tViews = new ArrayList<>();
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
			tViews.add(tTag);
		}
		return tViews;
	}

	@Override
	public List<ClientViewGroup<FluidView>> getClientGroups(Accessor<?> aAccessor, List<ViewGroup<CompoundTag>> aGroups) {
		// 手工 parse（绕 FluidView.readDefault：其 fluid/capacity 键与本合同不同，且 GTCEu
		// GTFluidStorageProvider.java:84-85 实证 readDefault 吃不下 >INT_MAX 量）——
		// GTCEu readFluid 同姿势（:85-101）。
		return ClientViewGroup.map(aGroups, GT6FluidProvider::readFluid, GROUP_DECORATOR);
	}

	/**
	 * 载荷 → FluidView（客户端面，依赖 IElementHelper 活体——离线不可构造，故文本/比值拆
	 * 纯函数 {@link #currentText}/{@link #maxText}/{@link #ratioOf} 供单测钉合同）。
	 * 双值策略（P22 D2 语义不变）：overlay 载体钉一桶量（{@link JadeFluidObject#of(Fluid, long)}
	 * 1000——readDefault 的 INT_MAX 坑），current/max 文本用真实 long 量。
	 */
	private static FluidView readFluid(CompoundTag aTag) {
		long tCapacity = aTag.getLong(KEY_CAPACITY);
		if (tCapacity <= 0) return null;
		Fluid tFluid = resolveFluid(aTag.getString(KEY_FLUID_NAME));
		if (tFluid == null) return null;
		long tAmount = aTag.getLong(KEY_AMOUNT);
		JadeFluidObject tCarrier = JadeFluidObject.of(tFluid, CARRIER_MILLIBUCKETS);
		FluidView tView = new FluidView(IElementHelper.get().fluid(tCarrier));
		tView.fluidName = CommonProxy.getFluidName(tCarrier);
		tView.current = currentText(tAmount);
		tView.max = maxText(tCapacity);
		tView.ratio = ratioOf(tAmount, tCapacity);
		return tView;
	}

	/** 真实 long 量文本（Jade 自家 mB unicode 面，FluidTextHelper 双腿 :9/:7 同形）。 */
	static String currentText(long aAmount) {
		return FluidTextHelper.getUnicodeMillibuckets(aAmount, true);
	}

	/** 真实 long 容量文本（同上）。 */
	static String maxText(long aCapacity) {
		return FluidTextHelper.getUnicodeMillibuckets(aCapacity, true);
	}

	/** 填充比（GTCEu GTFluidStorageProvider.java:98 的 min 1f 钳制同形——long 商走 double 再落 float）。 */
	static float ratioOf(long aAmount, long aCapacity) {
		return Math.min(1.0F, (float) ((double) aAmount / aCapacity));
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
