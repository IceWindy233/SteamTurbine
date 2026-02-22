package cn.icewindy.steamturbine.client.gui;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import net.minecraftforge.fluids.FluidStack;

import org.lwjgl.opengl.GL11;

import cn.icewindy.steamturbine.SteamTurbineMod;
import cn.icewindy.steamturbine.inventory.ContainerFlowLimiter;
import cn.icewindy.steamturbine.tileentity.TileEntityFluidInputHatch;

public class GuiFlowLimiter extends GuiContainer {

    private static final ResourceLocation GUI_TEXTURE = new ResourceLocation(
        SteamTurbineMod.MOD_ID,
        "textures/gui/gui_fluid_hatch.png");
    private static final ResourceLocation BUTTON_TEXTURE = new ResourceLocation(
        SteamTurbineMod.MOD_ID,
        "textures/gui/button.png");

    private final TileEntityFluidInputHatch tile;

    public GuiFlowLimiter(InventoryPlayer playerInv, TileEntityFluidInputHatch tile) {
        super(new ContainerFlowLimiter(playerInv, tile));
        this.tile = tile;
        this.xSize = 178;
        this.ySize = 182;
    }

    @Override
    public void initGui() {
        super.initGui();
        int left = (this.width - this.xSize) / 2;
        int top = (this.height - this.ySize) / 2;

        int flowX = left + 120; // 稍微向左移一点点
        // 缩减到 36x12，垂直间距适配
        this.buttonList.add(new GuiIconButton(0, flowX, top + 22, 36, 12, "-100", BUTTON_TEXTURE, 0, 0));
        this.buttonList.add(new GuiIconButton(1, flowX, top + 36, 36, 12, "-10", BUTTON_TEXTURE, 0, 0));
        this.buttonList.add(new GuiIconButton(2, flowX, top + 50, 36, 12, "+10", BUTTON_TEXTURE, 0, 0));
        this.buttonList.add(new GuiIconButton(3, flowX, top + 64, 36, 12, "+100", BUTTON_TEXTURE, 0, 0));
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        super.actionPerformed(button);
        this.mc.playerController.sendEnchantPacket(this.inventorySlots.windowId, button.id);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        this.fontRendererObj
            .drawString(StatCollector.translateToLocal("tile.steamturbine.fluid_input_hatch.name"), 8, 4, 0x404040);
        FluidStack fluid = tile.getFluid();
        String fluidName = fluid == null ? StatCollector.translateToLocal("steamturbine.gui.empty")
            : fluid.getLocalizedName();
        int amount = fluid == null ? 0 : fluid.amount;

        // Information display shifted right by 4 pixels (from 8 to 12)
        this.fontRendererObj.drawString(
            StatCollector.translateToLocal("steamturbine.flow_limiter.fluid") + ": " + fluidName,
            12,
            22,
            0x404040);
        this.fontRendererObj.drawString(
            StatCollector.translateToLocal("steamturbine.flow_limiter.amount") + ": " + amount + " mB",
            12,
            34,
            0x404040);
        this.fontRendererObj.drawString(
            StatCollector.translateToLocal("steamturbine.flow_limiter.limit") + ": "
                + tile.getMaxFlowPerTick()
                + " mB/t",
            12,
            46,
            0x404040);
        // Inventory title moved to Y=91 (92 - 1)
        this.fontRendererObj.drawString(StatCollector.translateToLocal("container.inventory"), 8, 90, 0x404040);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        int left = (this.width - this.xSize) / 2;
        int top = (this.height - this.ySize) / 2;
        GL11.glColor4f(1, 1, 1, 1);
        this.mc.getTextureManager()
            .bindTexture(GUI_TEXTURE);
        drawTexturedModalRect(left, top, 0, 0, this.xSize, this.ySize);
    }

}
