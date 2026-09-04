package gregtech6.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Base screen for all GT6 GUIs — direct translation of the client half of upstream
 * gregapi/gui/ContainerClient.java onto the 1.20.1 AbstractContainerScreen model, using
 * DispenserScreen.java:9-35 as the vanilla skeleton (init title, renderBg blit, render trio).
 *
 * <p>Subclasses pick a background texture and panel size; this base centres the title over the
 * panel (DispenserScreen.java:19) and blits the texture from UV 0,0 (DispenserScreen.java:31-33).
 * Only ever referenced from the Dist.CLIENT subscriber (GTClientModBusListener), so the dedicated
 * server never loads it.
 */
@OnlyIn(Dist.CLIENT)
public abstract class GTGuiScreen<T extends GTGuiMenu> extends AbstractContainerScreen<T> {

    /**
     * 256x256 texture canvas whose panel sits at UV 0,0 (vanilla container texture convention);
     * {@link GuiGraphics#blit(ResourceLocation, int, int, int, int, int, int)} samples the panel
     * region with imageWidth/imageHeight. Placeholder PNGs are produced by
     * {@code mdk/tools/gen_gui_textures.py}.
     */
    protected final ResourceLocation backgroundTexture;

    protected GTGuiScreen(T menu, Inventory playerInventory, Component title, ResourceLocation backgroundTexture) {
        super(menu, playerInventory, title);
        this.backgroundTexture = backgroundTexture;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        //? if forge {
        this.renderBackground(guiGraphics);
        //?} else {
        /*this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        *///?}
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(this.backgroundTexture, x, y, 0, 0, this.imageWidth, this.imageHeight);
    }
}
