package cn.icewindy.steamturbine.client.gui;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import org.lwjgl.opengl.GL11;

import cn.icewindy.steamturbine.SteamTurbineMod;
import cn.icewindy.steamturbine.inventory.ContainerHeatExchanger;
import cn.icewindy.steamturbine.tileentity.TileEntityHeatExchangerController;

public class GuiHeatExchanger extends GuiContainer {

    private static final ResourceLocation GUI_TEXTURE = new ResourceLocation(
        SteamTurbineMod.MOD_ID,
        "textures/gui/gui_normal.png");
    private final TileEntityHeatExchangerController tile;

    public GuiHeatExchanger(InventoryPlayer playerInv, TileEntityHeatExchangerController tile) {
        super(new ContainerHeatExchanger(playerInv, tile));
        this.tile = tile;
        this.xSize = 176;
        this.ySize = 171;
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        // Title
        String title = StatCollector.translateToLocal("tile.steamturbine.heat_exchanger_controller.name");
        this.fontRendererObj.drawString(title, 8, 6, 0x404040);
        this.fontRendererObj.drawString(StatCollector.translateToLocal("container.inventory"), 8, 79, 0x404040);

        // Colors
        final int colorDefault = 0x404040;
        final int colorGreen = 0x00AA00;
        final int colorRed = 0xAA0000;

        // Column 1
        int col1X = 10;
        int startY = 22;
        int spacing = 11;

        // Active
        String activeLabel = StatCollector.translateToLocal("steamturbine.gui.active") + ": ";
        this.fontRendererObj.drawString(activeLabel, col1X, startY, colorDefault);
        drawValue(tile.isActive(), col1X + fontRendererObj.getStringWidth(activeLabel), startY, colorGreen, colorRed);

        // Formed
        String formedLabel = StatCollector.translateToLocal("steamturbine.gui.formed") + ": ";
        this.fontRendererObj.drawString(formedLabel, col1X, startY + spacing, colorDefault);
        drawValue(
            tile.isFormed(),
            col1X + fontRendererObj.getStringWidth(formedLabel),
            startY + spacing,
            colorGreen,
            colorRed);

        // Steam Output
        this.fontRendererObj.drawString(
            StatCollector.translateToLocal("steamturbine.gui.steam_output") + ": "
                + tile.getLastSteamProduced()
                + " mB/t",
            col1X,
            startY + spacing * 2,
            colorDefault);
        // Hot Fluid Rate
        this.fontRendererObj.drawString(
            StatCollector.translateToLocal("steamturbine.gui.hot_fluid_rate") + ": "
                + tile.getLastHotFluidConsumed()
                + " mB/t",
            col1X,
            startY + spacing * 3,
            colorDefault);

        // Column 2
        int col2X = 100;

        // Superheated mode
        String shModeLabel = StatCollector.translateToLocal("steamturbine.gui.superheated_mode") + ": ";
        this.fontRendererObj.drawString(shModeLabel, col2X, startY, colorDefault);
        drawValue(
            tile.isSuperheated(),
            col2X + fontRendererObj.getStringWidth(shModeLabel),
            startY,
            colorGreen,
            colorRed);

        // Allow Superheated
        String allowShLabel = StatCollector.translateToLocal("steamturbine.gui.allow_superheated") + ": ";
        this.fontRendererObj.drawString(allowShLabel, col2X, startY + spacing, colorDefault);
        drawValue(
            cn.icewindy.steamturbine.ModConfig.lheEnableSuperheatedOutput,
            col2X + fontRendererObj.getStringWidth(allowShLabel),
            startY + spacing,
            colorGreen,
            colorRed);

        // Trigger superheat
        boolean wouldSuperheat = tile.getLastHotFluidConsumed() >= tile.getSuperheatedThreshold()
            && tile.getSuperheatedThreshold() > 0;
        String triggerShLabel = StatCollector.translateToLocal("steamturbine.gui.trigger_superheat") + ": ";
        this.fontRendererObj.drawString(triggerShLabel, col2X, startY + spacing * 2, colorDefault);
        drawValue(
            wouldSuperheat,
            col2X + fontRendererObj.getStringWidth(triggerShLabel),
            startY + spacing * 2,
            colorGreen,
            colorRed);
    }

    private void drawValue(boolean condition, int x, int y, int colorTrue, int colorFalse) {
        String text = condition ? StatCollector.translateToLocal("steamturbine.gui.value.yes")
            : StatCollector.translateToLocal("steamturbine.gui.value.no");
        this.fontRendererObj.drawString(text, x, y, condition ? colorTrue : colorFalse);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager()
            .bindTexture(GUI_TEXTURE);
        int sx = (width - xSize) / 2;
        int sy = (height - ySize) / 2;
        this.drawTexturedModalRect(sx, sy, 0, 0, xSize, ySize);
    }
}
