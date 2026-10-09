package gregtech6.jade;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.resources.ResourceLocation;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.IElement;
import snownee.jade.api.ui.IElementHelper;

import gregtech6.itemdata.GT6ItemData;
import gregtech6.tileentity.IPaintableTE;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;

/**
 * GT6 漆色 Jade 图标面（task 15-hive-jade-tint，issue #15 尾巴 B）：Jade 悬浮面板的方块
 * 图标走 ItemStack 渲染链（jade-1201 impl/BlockAccessorClientHandler.getIcon:45-77 →
 * DisplayHelper.drawItem:219 → guiGraphics.renderFakeItem → ItemColors），只吃 ItemColor +
 * 栈 NBT，不吃 {@code GTMachineTintModel} 的 BlockState 包装器——漆过的方块（蜂巢世界族色、
 * 喷漆机器）图标灰白。本 provider 挂 {@code registerBlockIcon}（jade-1201 impl
 * WailaClientRegistration.java:142-145 / jade-1211 :132-137，双腿同形），把
 * {@link IPaintableTE#isPainted}/{@link IPaintableTE#getPaint}（公共缝，客户端 BE 经
 * getUpdateTag=saveWithoutMetadata 双通道持有真值，TileEntityBase03TicksAndSync.java:294-344）
 * 写进<b>显示用</b>临时栈的载荷（{@link GT6ItemData#updateRaw} 双腿缝：1.20.1 根 tag /
 * 1.21.1 CUSTOM_DATA），由 {@code GTItemPaintTint.itemColor()}（GTClientHandlers 侧注册，
 * 含 task 15-hive-jade-tint 修 A 的 HIVE_ITEM 挂钩）消费显色。未漆/非漆面 BE 答
 * {@code null}（Jade 契约：不接管默认图标）。只动 Jade 显示副本，玩家拾取语义零触碰
 * （getCloneItemStack 覆写=主会话裁定的 C 方案，不做）。
 *
 * <p>显示栈取 {@code new ItemStack(block)} 而非接管 currentIcon——IElement API 不暴露内栈
 * （impl 包 ItemStackElement），GT6 漆面域无 getCloneItemStack 覆写（15-jade-tint 15 族
 * 盘点），两栈逐字节同形。注册挂 {@code GTEntityBlock} 全族（GT6JadePlugin，与 tooltip
 * provider 同姿势），体内 painted 门分发。
 */
public final class GT6PaintIconProvider implements IBlockComponentProvider {

	public static final GT6PaintIconProvider INSTANCE = new GT6PaintIconProvider();

	/** 唯一 id（IJadeProvider 抽象面，GT6CrucibleProvider 同形）。 */
	private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath("gt6", "paint_icon_provider");

	private GT6PaintIconProvider() {
	}

	@Override
	public ResourceLocation getUid() {
		return UID;
	}

	/**
	 * 本 provider 只挂 {@code registerBlockIcon}——Jade 不经 registerBlockComponent 调本面
	 * （IBlockComponentProvider.getIcon javadoc 逐字），空体即契约（1201 腿 appendTooltip
	 * 抽象，GT6CrucibleProvider 双实现形的 icon-only 版）。
	 */
	@Override
	public void appendTooltip(ITooltip aTooltip, BlockAccessor aAccessor, IPluginConfig aConfig) {
	}

	@Override
	public IElement getIcon(BlockAccessor aAccessor, IPluginConfig aConfig, IElement aCurrentIcon) {
		if (!(aAccessor.getBlockEntity() instanceof IPaintableTE tPaintable)) return null;
		ItemStack tIcon = iconStack(aAccessor.getBlockState().getBlock(), tPaintable);
		return tIcon == null || tIcon.isEmpty() ? null : IElementHelper.get().item(tIcon);
	}

	/**
	 * getIcon 的纯缝（accessor 薄壳 live-only，GT6MachineProviderTest 姿势——离线钉值面）：
	 * 未漆答 {@code null}（不接管）；漆过答带 {@code gt.painted}/{@code gt.color} 载荷的
	 * 显示栈（GTItemPaintTint 读形，根载荷回落臂即中）。颜色原值写入——读侧
	 * {@code tintARGB} 已做 {@code & 0xFFFFFF} 掩码（GTMachinePaintTint.java:87）。
	 */
	public static ItemStack iconStack(Block aBlock, IPaintableTE aPaintable) {
		if (!aPaintable.isPainted()) return null;
		ItemStack rStack = new ItemStack(aBlock);
		GT6ItemData.updateRaw(rStack, aTag -> {
			aTag.putBoolean(TileEntityBase03TicksAndSync.NBT_PAINTED, true);
			aTag.putInt(TileEntityBase03TicksAndSync.NBT_COLOR, aPaintable.getPaint());
		});
		return rStack;
	}
}
