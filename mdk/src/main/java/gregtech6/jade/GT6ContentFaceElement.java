package gregtech6.jade;

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
 * 精灵+tint 画成一条贴图填充——固体条取其固体臂（bodyTexture 精灵 + mRGBaSolid tint，
 * 与碗壳同观感），熔融未桥材质取其熔融臂（{@code moltenTexture} 每材质组 molten 灰度 +
 * mRGBaLiquid——task r11b-crucible-molten-art，平板 smeltery_content 占位退役）。塞进
 * {@code progressStyle().overlay(...)} 后由 Jade 官方链路强制 size 成条再渲染
 * （ProgressStyle.render:91-94 双腿实证），渲染面与 Jade 官方流体元素
 * （{@code IElementHelper.fluid}）同一 overlay 位。
 *
 * <p>渲染 = 双腿GuiGraphics 原生两调：{@code setColor}（1.20.1 :173 = flush +
 * RenderSystem.setShaderColor；1.21.1 同形，javadoc 实证）×
 * {@code blit(x, y, w, h, z, TextureAtlasSprite)}（双腿同签名，1.20.1 GuiGraphics.java:250 /
 * 1.21.1 minecraft-merged javap）——innerBlit 双腿都是 Tesselator 即画（1.21.1 字节码实证），
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
		aGraphics.blit(Math.round(aX), Math.round(aY), Math.round(tSize.x), Math.round(tSize.y), 0, tSprite);
		aGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
	}
}
