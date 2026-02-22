package cn.icewindy.steamturbine.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;

import cn.icewindy.steamturbine.ModCreativeTab;
import cn.icewindy.steamturbine.SteamTurbineMod;

public class BlockRedstoneControl extends Block {

    public BlockRedstoneControl() {
        super(Material.iron);
        setBlockName("steamturbine.redstone_control");
        setHardness(3.0F);
        setResistance(10.0F);
        setStepSound(soundTypeMetal);
        setCreativeTab(ModCreativeTab.INSTANCE);
        setHarvestLevel("pickaxe", 1);
    }

    @Override
    public net.minecraft.util.IIcon getIcon(net.minecraft.world.IBlockAccess world, int x, int y, int z, int side) {
        if (cn.icewindy.steamturbine.util.MultiblockValidator.isPartOfFormed(x, y, z)) {
            return cn.icewindy.steamturbine.registry.ModBlocks.turbineCasing.getIcon(side, 0);
        }
        return super.getIcon(world, x, y, z, side);
    }

    @Override
    public void registerBlockIcons(IIconRegister reg) {
        blockIcon = reg.registerIcon(SteamTurbineMod.MOD_ID + ":redstone_control");
    }
}
