package cn.icewindy.steamturbine.block;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import cn.icewindy.steamturbine.SteamTurbineMod;
import cn.icewindy.steamturbine.tileentity.TileEntityFluidInputHatch;

public class BlockFluidInputHatch extends BlockHatch {

    public BlockFluidInputHatch() {
        setBlockName("steamturbine.fluid_input_hatch");
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileEntityFluidInputHatch();
    }

    @Override
    public void registerBlockIcons(IIconRegister reg) {
        blockIcon = reg.registerIcon(SteamTurbineMod.MOD_ID + ":input_hatch");
    }

    @Override
    protected int getGuiId() {
        return 7;
    }
}
