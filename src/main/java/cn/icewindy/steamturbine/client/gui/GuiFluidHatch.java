package cn.icewindy.steamturbine.client.gui;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import net.minecraftforge.fluids.FluidStack;

import org.lwjgl.opengl.GL11;

import cn.icewindy.steamturbine.SteamTurbineMod;
import cn.icewindy.steamturbine.api.IFluidDisplayHatch;
import cn.icewindy.steamturbine.inventory.ContainerFluidHatch;

public class GuiFluidHatch extends GuiContainer {

    private static final ResourceLocation GUI_TEXTURE = new ResourceLocation(
        SteamTurbineMod.MOD_ID,
        "textures/gui/gui_fluid_hatch.png");
    private final TileEntity tile;
    private final String titleKey;

    public GuiFluidHatch(InventoryPlayer playerInv, TileEntity tile, String titleKey) {
        super(new ContainerFluidHatch(playerInv, tile));
        this.tile = tile;
        this.titleKey = titleKey;
        this.xSize = 178;
        this.ySize = 182;
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        this.fontRendererObj.drawString(StatCollector.translateToLocal(titleKey), 8, 4, 0x404040);

        FluidStack fluid = getFluid();
        int amount = getAmount();
        int capacity = getCapacity();
        String fluidName = fluid == null ? StatCollector.translateToLocal("steamturbine.gui.empty")
            : fluid.getLocalizedName();

        this.fontRendererObj
            .drawString(StatCollector.translateToLocal("steamturbine.gui.fluid") + ": " + fluidName, 12, 22, 0x404040);
        this.fontRendererObj.drawString(
            StatCollector.translateToLocal("steamturbine.gui.amount") + ": " + amount + " mB",
            12,
            34,
            0x404040);
        this.fontRendererObj.drawString(
            StatCollector.translateToLocal("steamturbine.gui.capacity") + ": " + capacity + " mB",
            12,
            46,
            0x404040);
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

    private FluidStack getFluid() {
        if (tile instanceof IFluidDisplayHatch) {
            return ((IFluidDisplayHatch) tile).getFluid();
        }
        return null;
    }

    private int getAmount() {
        FluidStack fluid = getFluid();
        return fluid == null ? 0 : fluid.amount;
    }

    private int getCapacity() {
        if (tile instanceof IFluidDisplayHatch) {
            return ((IFluidDisplayHatch) tile).getTankCapacity();
        }
        return 0;
    }
}
