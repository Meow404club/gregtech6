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
 * jade-1201 CommonProxy.java:230 / jade-1211 :516）→ 服务端同步 provider 挂这里；
 * registerClient() 仅物理客户端 → tooltip provider 挂这里。
 *
 * <p>零 chisel 声明（双腿逐字同源）：注册面 1.20.1 registerBlockComponent(IBlockComponentProvider,
 * Class&lt;? extends Block&gt;)（IWailaClientRegistration.java:71）/ 1.21.1 registerBlockComponent(
 * IComponentProvider&lt;BlockAccessor&gt;, Class&lt;? extends Block&gt;)（:80）——provider 实例实现
 * IBlockComponentProvider，在 1.21.1 经 IBlockComponentProvider extends IComponentProvider&lt;BlockAccessor&gt;
 * （IBlockComponentProvider.java:11）自动满足；registerBlockDataProvider 1.20.1
 * Class&lt;? extends BlockEntity&gt;（IWailaCommonRegistration.java:20）/ 1.21.1 Class&lt;?&gt;（:19）——
 * 传 BE 基类两腿皆合法。唯一已证分叉（BoxStyle 构形）收敛在 {@link GT6MachineProvider#jadeBox()}。
 */
@WailaPlugin
public class GT6JadePlugin implements IWailaPlugin {

	@Override
	public void register(IWailaCommonRegistration aRegistration) {
		// 服务端数据源 = 全 GT6 BE 基类：ERROR_MESSAGE 在 Root（TileEntityBase01Root.java:88），
		// 机器字段在 BasicMachine，成形态在 MultiBlockBase——provider 体内 instanceof 分发。
		aRegistration.registerBlockDataProvider(GT6MachineProvider.INSTANCE, TileEntityBase01Root.class);
	}

	@Override
	public void registerClient(IWailaClientRegistration aRegistration) {
		// tooltip 挂全 GT6 承 BE 方块基类（GTEntityBlock.java:37 所有机器/多方块方块都经它）。
		aRegistration.registerBlockComponent(GT6MachineProvider.INSTANCE, GTEntityBlock.class);
	}

}
