package cn.icewindy.steamturbine.client.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import cn.icewindy.steamturbine.inventory.ContainerFluidHatch;
import cn.icewindy.steamturbine.inventory.ContainerTurbine;
import cn.icewindy.steamturbine.tileentity.TileEntityInputHatch;
import cn.icewindy.steamturbine.tileentity.TileEntityOutputHatch;
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
        } else if (ID == 1 || ID == 2) {
            TileEntity te = world.getTileEntity(x, y, z);
            if (te instanceof TileEntityInputHatch || te instanceof TileEntityOutputHatch) {
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
        } else if (ID == 1) {
            TileEntity te = world.getTileEntity(x, y, z);
            if (te instanceof TileEntityInputHatch) {
                return new GuiFluidHatch(player.inventory, te, "tile.steamturbine.input_hatch.name");
            }
        } else if (ID == 2) {
            TileEntity te = world.getTileEntity(x, y, z);
            if (te instanceof TileEntityOutputHatch) {
                return new GuiFluidHatch(player.inventory, te, "tile.steamturbine.output_hatch.name");
            }
        }
        return null;
    }
}
