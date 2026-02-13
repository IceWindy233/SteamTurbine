package cn.icewindy.steamturbine.client.gui;

import java.util.ArrayList;
import java.util.List;

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
        "textures/gui/gui_turbine.png");

    private static final int TEXT_COLOR = 0x404040;
    private static final int STATUS_GREEN = 0x2FAF4A;
    private static final int STATUS_YELLOW = 0xD4AC0D;
    private static final int STATUS_RED = 0xB03A2E;

    private final TileEntityTurbineController tile;

    public GuiTurbine(InventoryPlayer playerInv, TileEntityTurbineController tile) {
        super(new ContainerTurbine(playerInv, tile));
        this.tile = tile;
        this.xSize = 177;
        this.ySize = 171;
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

        drawRightInfoLine(
            this.xSize - 8,
            y0,
            tr("steamturbine.gui.label.ctrl_eu"),
            formatValue(tile.getControllerStored()) + "/" + formatValue(tile.getControllerCapacity()),
            TEXT_COLOR);

        drawRightInfoLine(
            this.xSize - 8,
            y0 + line,
            tr("steamturbine.gui.label.dyn_eu"),
            formatValue(tile.getDynamoStored()) + "/" + formatValue(tile.getDynamoCapacity()),
            TEXT_COLOR);

        int damage = tile.getRotorDamagePercent();
        int damageColor = STATUS_GREEN;
        if (damage > 75) {
            damageColor = STATUS_RED;
        } else if (damage > 40) {
            damageColor = STATUS_YELLOW;
        }

        drawRightInfoLine(this.xSize - 8, y0 + line * 2, tr("steamturbine.gui.label.dmg"), damage + "%", damageColor);

        drawRightInfoLine(
            this.xSize - 8,
            y0 + line * 3,
            tr("steamturbine.gui.label.output"),
            tile.getLastEUOutput() + " EU/t",
            STATUS_GREEN);

        this.fontRendererObj.drawString(StatCollector.translateToLocal("container.inventory"), 9, 77, TEXT_COLOR);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        int left = (this.width - this.xSize) / 2;
        int top = (this.height - this.ySize) / 2;

        GL11.glColor4f(1, 1, 1, 1);
        this.mc.getTextureManager()
            .bindTexture(GUI_TEXTURE);
        // Draw main GUI background from texture
        drawTexturedModalRect(left, top, 0, 0, this.xSize, this.ySize);

        // Draw Energy Bar Fill
        // Texture analysis: Target x=116, y=75, Source x=0, y=171, Size 54x12
        double energy = tile.getTotalEnergyStored();
        double maxEnergy = tile.getTotalEnergyCapacity();
        if (maxEnergy > 0) {
            int fillWidth = (int) (54 * Math.min(1.0, energy / maxEnergy));
            if (fillWidth > 0) {
                drawTexturedModalRect(left + 116, top + 75, 0, 171, fillWidth, 12);
            }
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);

        int left = (this.width - this.xSize) / 2;
        int top = (this.height - this.ySize) / 2;

        // Tooltip for Energy Bar (x=116, y=75, w=54, h=12)
        if (mouseX >= left + 116 && mouseX <= left + 116 + 54 && mouseY >= top + 75 && mouseY <= top + 75 + 11) {
            List<String> tooltip = new ArrayList<String>();
            tooltip.add(
                tr("steamturbine.gui.label.ctrl_eu") + ": "
                    + formatExactValue(tile.getControllerStored())
                    + " / "
                    + formatExactValue(tile.getControllerCapacity()));
            tooltip.add(
                tr("steamturbine.gui.label.dyn_eu") + ": "
                    + formatExactValue(tile.getDynamoStored())
                    + " / "
                    + formatExactValue(tile.getDynamoCapacity()));
            tooltip.add(
                "Total: " + formatExactValue(tile.getTotalEnergyStored())
                    + " / "
                    + formatExactValue(tile.getTotalEnergyCapacity())
                    + " EU");
            this.drawHoveringText(tooltip, mouseX, mouseY, fontRendererObj);
        }
    }

    private String formatExactValue(long value) {
        return java.text.NumberFormat.getInstance()
            .format(value);
    }

    private String formatValue(long value) {
        if (value < 1000) return String.valueOf(value);
        if (value < 1000000) return String.format("%.1fk", value / 1000.0);
        if (value < 1000000000) return String.format("%.1fM", value / 1000000.0);
        return String.format("%.1fG", value / 1000000000.0);
    }

    private void drawRightInfoLine(int x, int y, String key, String value, int valueColor) {
        String fullKey = key + ": ";
        int valWidth = this.fontRendererObj.getStringWidth(value);
        int keyWidth = this.fontRendererObj.getStringWidth(fullKey);
        int startX = x - valWidth - keyWidth;
        this.fontRendererObj.drawString(fullKey, startX, y, TEXT_COLOR);
        this.fontRendererObj.drawString(value, startX + keyWidth, y, valueColor);
    }

    private void drawInfoLine(int x, int y, String key, String value, int valueColor) {
        this.fontRendererObj.drawString(key + ":", x, y, TEXT_COLOR);
        this.fontRendererObj.drawString(value, x + 28, y, valueColor);
    }

    private String tr(String key) {
        return StatCollector.translateToLocal(key);
    }
}
