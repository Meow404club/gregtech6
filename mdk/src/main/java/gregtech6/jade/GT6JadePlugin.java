package gregtech6.jade;

import gregtech6.block.GTEntityBlock;
import gregtech6.tileentity.TileEntityBase01Root;

import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/**
 * Jade（WAILA 后继）兼容入口（task p21-jade-compat）。形源 GTCEu Modern GTJadePlugin.java:21-22：
 * {@code @WailaPlugin} 裸注解 + {@link IWailaPlugin}。
 *
 * <p>发现机制 = Jade 在 FMLLoadCompleteEvent 扫 {@code @WailaPlugin} 注解后 Class.forName +
 * 无参构造实例化（jade-1201 util/CommonProxy.java:208-244 / jade-1211 :488-530），本仓代码
 * 对本类零静态引用——Jade 缺席时本类永不 classload，运行时可选天然安全（NCDFE 防线）。
 *
 * <p>register() 在专用服务端无条件调用（双腿都在 isPhysicallyClient 分支之前：
 * jade-1201 CommonProxy.java:230 / jade-1211 :516）→ 服务端 provider 挂这里；
 * registerClient() 仅物理客户端 → 客户端 provider 挂这里。
 *
 * <p>流体段（task p23-jade-universal-fluid，v1 b9b0b23f 的 block-component 自渲染对升级为
 * universal registerFluidStorage 正字标——GTCEu GTJadePlugin.java:56/:93 同款调用形）：
 * 注册行只有两行（GTCEu 同姿势），服务端取数与客户端 parse 都在 {@link GT6FluidProvider}
 * 单体内，行渲染归 Jade 自家 universal append。 clazz 挂 BE 全族根
 * {@link TileEntityBase01Root}（双腿 registerFluidStorage 的 Class&lt;? extends T&gt; 同形——
 * 1201 IWailaCommonRegistration.java:32 / 1211 :31；客户端注册双腿逐字同形
 * 1201 IWailaClientRegistration.java:164 / 1211 :173），machines 内部再 instanceof 分发。
 * 机器四段 tooltip（{@link GT6MachineProvider}）不变，仍是 block-component 对。坩埚族
 * （{@link GT6CrucibleProvider}，task p28-crucible-jade-face）同一注册形追加两行——
 * GT6MachineProvider 零改动（不塞坩埚分支的裁定）。
 */
@WailaPlugin
public class GT6JadePlugin implements IWailaPlugin {

	@Override
	public void register(IWailaCommonRegistration aRegistration) {
		// 服务端数据源 = 全 GT6 BE 基类：ERROR_MESSAGE 在 Root（TileEntityBase01Root.java:88），
		// 机器字段在 BasicMachine，成形态在 MultiBlockBase——provider 体内 instanceof 分发。
		aRegistration.registerBlockDataProvider(GT6MachineProvider.INSTANCE, TileEntityBase01Root.class);
		// 流体段 universal 服务端腿（数据载体=Jade 自家 universal FluidStorageProvider，
		// 本 provider 按 priority 抢跑取数——GT6FluidProvider#getDefaultPriority 的裁定）。
		aRegistration.registerFluidStorage(GT6FluidProvider.INSTANCE, TileEntityBase01Root.class);
		// 坩埚族服务端腿（task p28-crucible-jade-face）：小型 Smeltery + 大型 Crucible 同一
		// provider 同一格式，体内双 concrete instanceof 分发（GT6CrucibleProvider 类 doc）。
		aRegistration.registerBlockDataProvider(GT6CrucibleProvider.INSTANCE, TileEntityBase01Root.class);
	}

	@Override
	public void registerClient(IWailaClientRegistration aRegistration) {
		// tooltip 挂全 GT6 承 BE 方块基类（GTEntityBlock.java:37 所有机器/多方块方块都经它）。
		aRegistration.registerBlockComponent(GT6MachineProvider.INSTANCE, GTEntityBlock.class);
		// 流体段 universal 客户端腿：Jade 按 JadeFluidStorageUid 从 uid map 找回本 provider
		// （jade-1201 addon/universal/FluidStorageProvider.java:44 / jade-1211 :67-68）。
		aRegistration.registerFluidStorageClient(GT6FluidProvider.INSTANCE);
		// 坩埚族客户端腿（task p28-crucible-jade-face）：GTEntityBlock 全覆盖两坩埚方块
		// （CrucibleBlock extends GTEntityBlock；GTMultiBlockControllerBlock 同），体内键门分发。
		aRegistration.registerBlockComponent(GT6CrucibleProvider.INSTANCE, GTEntityBlock.class);
	}

}
