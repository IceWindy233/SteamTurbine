package cn.icewindy.steamturbine.client.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import cn.icewindy.steamturbine.inventory.ContainerFlowLimiter;
import cn.icewindy.steamturbine.inventory.ContainerFluidHatch;
import cn.icewindy.steamturbine.inventory.ContainerTurbine;
import cn.icewindy.steamturbine.tileentity.TileEntityFluidInputHatch;
import cn.icewindy.steamturbine.tileentity.TileEntityFluidOutputHatch;
import cn.icewindy.steamturbine.tileentity.TileEntityTurbineController;
import cpw.mods.fml.common.network.IGuiHandler;

public class GuiHandler implements IGuiHandler {

    @Override
    public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        if (ID == 0) {
            TileEntity te = world.getTileEntity(x, y, z);
            if (te instanceof TileEntityTurbineController) {
                return new ContainerTurbine(player.inventory, (TileEntityTurbineController) te);
            }
        } else if (ID == 7 || ID == 8) {
            TileEntity te = world.getTileEntity(x, y, z);
            if (ID == 7 && te instanceof TileEntityFluidInputHatch) {
                return new ContainerFlowLimiter(player.inventory, (TileEntityFluidInputHatch) te);
            }
            if (ID == 8 && te instanceof TileEntityFluidOutputHatch) {
                return new ContainerFluidHatch(player.inventory, te);
            }
        }
        return null;
    }

    @Override
    public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        if (ID == 0) {
            TileEntity te = world.getTileEntity(x, y, z);
            if (te instanceof TileEntityTurbineController) {
                return new GuiTurbine(player.inventory, (TileEntityTurbineController) te);
            }
        } else if (ID == 7) {
            TileEntity te = world.getTileEntity(x, y, z);
            if (te instanceof TileEntityFluidInputHatch) {
                return new GuiFlowLimiter(player.inventory, (TileEntityFluidInputHatch) te);
            }
        } else if (ID == 8) {
            TileEntity te = world.getTileEntity(x, y, z);
            if (te instanceof TileEntityFluidOutputHatch) {
                return new GuiFluidHatch(player.inventory, te, "tile.steamturbine.fluid_output_hatch.name");
            }
        }
        return null;
    }
}
