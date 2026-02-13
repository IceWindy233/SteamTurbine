package cn.icewindy.steamturbine.block;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import cn.icewindy.steamturbine.SteamTurbineMod;
import cn.icewindy.steamturbine.tileentity.TileEntityOutputHatch;

public class BlockOutputHatch extends BlockHatch {

    public BlockOutputHatch() {
        super();
        setBlockName("steamturbine.output_hatch");
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileEntityOutputHatch();
    }

    @Override
    public void registerBlockIcons(IIconRegister reg) {
        blockIcon = reg.registerIcon(SteamTurbineMod.MOD_ID + ":output_hatch");
    }

    @Override
    protected int getGuiId() {
        return 2;
    }
}
