package cn.icewindy.steamturbine.block;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import cn.icewindy.steamturbine.ModCreativeTab;
import cn.icewindy.steamturbine.SteamTurbineMod;
import cn.icewindy.steamturbine.registry.ModBlocks;

public abstract class BlockHatch extends BlockContainer {

    public BlockHatch() {
        super(Material.iron);
        setHardness(3.0F);
        setResistance(10.0F);
        setStepSound(soundTypeMetal);
        setCreativeTab(ModCreativeTab.INSTANCE);
        setHarvestLevel("pickaxe", 1);
    }

    @Override
    public void registerBlockIcons(IIconRegister reg) {
        // Fallback or shared icon, usually overridden
        blockIcon = reg.registerIcon(SteamTurbineMod.MOD_ID + ":turbine_casing");
    }

    @Override
    public int getRenderType() {
        return 0;
    }

    @Override
    public boolean isOpaqueCube() {
        return true;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return true;
    }

    @Override
    public net.minecraft.util.IIcon getIcon(net.minecraft.world.IBlockAccess world, int x, int y, int z, int side) {
        if (cn.icewindy.steamturbine.util.MultiblockValidator.isPartOfFormed(x, y, z)) {
            return ModBlocks.turbineCasing.getIcon(side, 0);
        }
        if (cn.icewindy.steamturbine.util.HeatExchangerValidator.isPartOfFormed(x, y, z)) {
            return ModBlocks.heatExchangerCasing.getIcon(side, 0);
        }
        return super.getIcon(world, x, y, z, side);
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX,
        float hitY, float hitZ) {
        if (world.isRemote) {
            return true;
        }
        int guiId = getGuiId();
        if (guiId < 0) {
            return true;
        }
        TileEntity te = world.getTileEntity(x, y, z);
        if (te != null) {
            player.openGui(SteamTurbineMod.instance, guiId, world, x, y, z);
        }
        return true;
    }

    protected abstract int getGuiId();
}
