package cn.icewindy.steamturbine.client.gui;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import org.lwjgl.opengl.GL11;

import cn.icewindy.steamturbine.SteamTurbineMod;
import cn.icewindy.steamturbine.inventory.ContainerTurbine;
import cn.icewindy.steamturbine.tileentity.TileEntityTurbineController;

public class GuiTurbine extends GuiContainer {

    private static final ResourceLocation GUI_TEXTURE = new ResourceLocation(
        SteamTurbineMod.MOD_ID,
        "textures/gui/turbine_gui.png");
    private static final ResourceLocation TURBINE_SLOT_OVERLAY = new ResourceLocation(
        SteamTurbineMod.MOD_ID,
        "textures/gui/overlay_slot/turbine.png");

    private static final int TEXT_COLOR = 0x404040;
    private static final int STATUS_GREEN = 0x2FAF4A;
    private static final int STATUS_RED = 0xB03A2E;
    private static final int SLOT_BORDER = 0xFF8A8A8A;
    private static final int SLOT_FILL = 0x33B0B0B0;

    private final TileEntityTurbineController tile;

    public GuiTurbine(InventoryPlayer playerInv, TileEntityTurbineController tile) {
        super(new ContainerTurbine(playerInv, tile));
        this.tile = tile;
        this.xSize = 176;
        this.ySize = 166;
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        String title = StatCollector.translateToLocal("tile.steamturbine.turbine_controller.name");
        this.fontRendererObj.drawString(title, 8, 6, TEXT_COLOR);

        int leftX = 8;
        int rightX = 92;
        int y0 = 17;
        int line = 9;

        drawInfoLine(
            leftX,
            y0,
            tr("steamturbine.gui.label.run"),
            tile.isRedstonePowered() ? tr("steamturbine.gui.value.rs_off")
                : tile.getLastEUOutput() > 0 ? tr("steamturbine.gui.value.on") : tr("steamturbine.gui.value.off"),
            !tile.isRedstonePowered() && tile.getLastEUOutput() > 0 ? STATUS_GREEN : STATUS_RED);
        drawInfoLine(
            leftX,
            y0 + line,
            tr("steamturbine.gui.label.formed"),
            tile.isFormed() ? tr("steamturbine.gui.value.yes") : tr("steamturbine.gui.value.no"),
            tile.isFormed() ? STATUS_GREEN : STATUS_RED);
        drawInfoLine(
            leftX,
            y0 + line * 2,
            tr("steamturbine.gui.label.eff"),
            String.format("%.2f%%", tile.getCurrentEfficiency() * 100.0f),
            TEXT_COLOR);
        drawInfoLine(
            leftX,
            y0 + line * 3,
            tr("steamturbine.gui.label.flow"),
            tile.getLastSteamConsumed() + " L/t",
            TEXT_COLOR);
        drawInfoLine(
            leftX,
            y0 + line * 4,
            tr("steamturbine.gui.label.fuel"),
            tile.inputTank.getFluidAmount() + " L",
            TEXT_COLOR);

        drawInfoLine(
            rightX,
            y0 + line * 2,
            tr("steamturbine.gui.label.eu"),
            (long) tile.getStoredEU() + "/" + Math.max(1, tile.getGuiMaxEnergy()),
            TEXT_COLOR);
        drawInfoLine(
            rightX,
            y0 + line * 3,
            tr("steamturbine.gui.label.dmg"),
            tile.getRotorDamagePercent() + "%",
            TEXT_COLOR);

        this.fontRendererObj.drawString(StatCollector.translateToLocal("container.inventory"), 8, 76, TEXT_COLOR);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        int left = (this.width - this.xSize) / 2;
        int top = (this.height - this.ySize) / 2;

        GL11.glColor4f(1, 1, 1, 1);
        this.mc.getTextureManager()
            .bindTexture(GUI_TEXTURE);
        drawTexturedModalRect(left, top, 0, 0, this.xSize, this.ySize);

        // Rotor slot separator
        drawSlotFrame(left + 145, top + 57);
        this.mc.getTextureManager()
            .bindTexture(TURBINE_SLOT_OVERLAY);
        drawTexturedModalRect(left + 145, top + 57, 0, 0, 18, 18);

        // Player inventory slot separators
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlotFrame(left + 8 + col * 18, top + 88 + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            drawSlotFrame(left + 8 + col * 18, top + 146);
        }
    }

    private void drawInfoLine(int x, int y, String key, String value, int valueColor) {
        this.fontRendererObj.drawString(key + ":", x, y, TEXT_COLOR);
        this.fontRendererObj.drawString(value, x + 28, y, valueColor);
    }

    private String tr(String key) {
        return StatCollector.translateToLocal(key);
    }

    private void drawSlotFrame(int x, int y) {
        drawRect(x, y, x + 18, y + 18, SLOT_BORDER);
        drawRect(x + 1, y + 1, x + 17, y + 17, SLOT_FILL);
    }
}
