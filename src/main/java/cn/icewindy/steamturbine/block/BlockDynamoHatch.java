package cn.icewindy.steamturbine.block;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import cn.icewindy.steamturbine.SteamTurbineMod;
import cn.icewindy.steamturbine.tileentity.TileEntityDynamoHatch;

public class BlockDynamoHatch extends BlockHatch {

    public BlockDynamoHatch() {
        super();
        setBlockName("steamturbine.dynamo_hatch");
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileEntityDynamoHatch();
    }

    @Override
    public void registerBlockIcons(IIconRegister reg) {
        blockIcon = reg.registerIcon(SteamTurbineMod.MOD_ID + ":dynamo_hatch");
    }

    @Override
    protected int getGuiId() {
        return -1;
    }
}
