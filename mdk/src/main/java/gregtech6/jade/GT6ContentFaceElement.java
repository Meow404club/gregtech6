package gregtech6.jade;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.Vec2;

import snownee.jade.api.ui.Element;

/**
 * The content-face bar element（task crucible-jade-tankbar 用户三版终裁 v3 ②）：把碗形模型卡
 * ContentFace 缝（{@code GT6CrucibleDatagen.contentFace}，main 3926da237 合入）的
 * 精灵+tint 画成一条贴图填充——熔融未桥材质取其熔融臂（{@code moltenTexture} 每材质组 molten
 * 灰度 + mRGBaLiquid——task r11b-crucible-molten-art，平板 smeltery_content 占位退役），
 * 固体条取其固体臂（task crucible-solid-gray-fidelity 勘误：固定灰黑
 * {@code SOLID_CONTENT}，rough/block_raw×0xFF404040 = 上游 Smeltery:590 的
 * MT.NULL×blockRaw×CA_GRAY_64 逐字，与内容材质无关）。塞进
 * {@code progressStyle().overlay(...)} 后由 Jade 官方链路强制 size 成条再渲染
 * （ProgressStyle.render:91-94 双腿实证），渲染面与其他 mod 流体条
 * （Jade 官方 {@code IElementHelper.fluid} → FluidStackElement）同一 overlay 位——本卡坩埚
 * 条的 own 载荷已不经过那个元素（task crucible-jade-tankbar-fill，见
 * GT6CrucibleProvider 类 doc ①②）。
 *
 * <p>渲染 = 双腿 GuiGraphics 原生三调：图集预绑（{@code RenderSystem.setShaderTexture(0,
 * InventoryMenu.BLOCK_ATLAS)}——task crucible-jade-tankbar-fill 补齐；Jade 官方元素从
 * 不依赖绘制原语的隐式绑定，先例 = {@code DisplayHelper.drawTiledSprite} 1.20.1
 * jade-1201-forge-src DisplayHelper.java:341 / 1.21.1 :278 都是 blit 前显式绑定再手拼
 * quad，1.21.1 的 {@code IDisplayHelper.blitSprite} :425-431 同走显式合成面）×
 * {@code setColor}（1.20.1 :173 = flushIfManaged + RenderSystem.setShaderColor；1.21.1
 * 同形）× {@code blit(x, y, w, h, z, TextureAtlasSprite)}（双腿同签名，1.20.1
 * GuiGraphics.java:250 区 / 1.21.1 :348——双腿都经 innerBlit 自带
 * {@code setShaderTexture(0, atlasLocation)}：1.20.1 :346-347 / 1.21.1 :586，Forge 补丁
 * GuiGraphics.java.patch 不触 innerBlit）。所以预绑与 innerBlit 的内绑同址冗余——留它的
 * 意义是本原语自持、不吃 overlay pass 外层任何 RenderSystem 纹理状态（其他 mod 的
 * HUD mixin/shader 管线），与 Jade 官方先例同一姿势；实机若仍空白则断点在本原语之外
 * （如实记录，2026-10-10 归因链）。innerBlit 双腿都是 Tesselator/BufferUploader 即画，
 * set→blit→复位的次序在两条腿上都把 tint 吃进同一张四边形。模型贴图全部在方块图集
 * （{@link InventoryMenu#BLOCK_ATLAS}），精灵经 {@code Minecraft.getTextureAtlas}（双腿）取。
 * 淡出 alpha（OverlayRenderer 为 impl 内部包）不乘——vanilla blit 同样不管，观感差异为零。
 *
 * <p>类本体仅客户端触达（appendTooltip 的 overlay 组装路径，服务端永不执行——Jade addon
 * 惯例的懒加载面）。
 */
public final class GT6ContentFaceElement extends Element {

	/** FluidStackElement 同款默认保留面（Jade impl FluidStackElement.java:15）。 */
	private static final Vec2 DEFAULT_SIZE = new Vec2(16, 16);

	/** 方块图集内的精灵路径（ContentFace.texture 全限定名拆出）。 */
	private final ResourceLocation mTexture;
	/** 不透明 ARGB tint（-1 = 0xFFFFFFFF = 原样渲染——unpack 即全 1，无需特判）。 */
	private final int mTintARGB;

	public GT6ContentFaceElement(ResourceLocation aTexture, int aTintARGB) {
		mTexture = aTexture;
		mTintARGB = aTintARGB;
	}

	@Override
	public Vec2 getSize() {
		return DEFAULT_SIZE;
	}

	@Override
	public void render(GuiGraphics aGraphics, float aX, float aY, float aMaxX, float aMaxY) {
		// ProgressStyle.overlay 臂先 size(条形填充尺寸) 再 render —— cached size 即条内填充区
		Vec2 tSize = getCachedSize();
		TextureAtlasSprite tSprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(mTexture);
		aGraphics.setColor((mTintARGB >> 16 & 0xFF) / 255.0F, (mTintARGB >> 8 & 0xFF) / 255.0F,
				(mTintARGB & 0xFF) / 255.0F, (mTintARGB >>> 24 & 0xFF) / 255.0F);
		// blit 前显式绑定方块图集——Jade 官方 DisplayHelper.drawTiledSprite 同款姿势
		//（1.20.1 :341 / 1.21.1 :278，双腿行号；先例从不吃隐式纹理状态）。类 doc 有完整归因链。
		RenderSystem.setShaderTexture(0, InventoryMenu.BLOCK_ATLAS);
		aGraphics.blit(Math.round(aX), Math.round(aY), Math.round(tSize.x), Math.round(tSize.y), 0, tSprite);
		aGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
	}
}
