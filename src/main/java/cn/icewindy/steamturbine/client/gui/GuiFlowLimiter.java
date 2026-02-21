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
        "textures/gui/gui_flow_limiter.png");

    private final TileEntityFluidInputHatch tile;

    public GuiFlowLimiter(InventoryPlayer playerInv, TileEntityFluidInputHatch tile) {
        super(new ContainerFlowLimiter(playerInv, tile));
        this.tile = tile;
        this.xSize = 177;
        this.ySize = 204;
    }

    @Override
    public void initGui() {
        super.initGui();
        int left = (this.width - this.xSize) / 2;
        int top = (this.height - this.ySize) / 2;

        // 限速调节按钮
        int flowX = left + 114;
        this.buttonList.add(new GuiButton(0, flowX, top + 24, 46, 12, "-100"));
        this.buttonList.add(new GuiButton(1, flowX, top + 38, 46, 12, "-10"));
        this.buttonList.add(new GuiButton(2, flowX, top + 52, 46, 12, "+10"));
        this.buttonList.add(new GuiButton(3, flowX, top + 66, 46, 12, "+100"));
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

        this.fontRendererObj.drawString(
            StatCollector.translateToLocal("steamturbine.flow_limiter.fluid") + ": " + fluidName,
            8,
            22,
            0x404040);
        this.fontRendererObj.drawString(
            StatCollector.translateToLocal("steamturbine.flow_limiter.amount") + ": " + amount + " mB",
            8,
            34,
            0x404040);
        this.fontRendererObj.drawString(
            StatCollector.translateToLocal("steamturbine.flow_limiter.limit") + ": "
                + tile.getMaxFlowPerTick()
                + " mB/t",
            8,
            46,
            0x404040);
        this.fontRendererObj.drawString(StatCollector.translateToLocal("container.inventory"), 8, 102, 0x404040);
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
