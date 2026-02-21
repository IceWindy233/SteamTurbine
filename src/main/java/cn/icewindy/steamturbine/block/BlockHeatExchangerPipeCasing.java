package cn.icewindy.steamturbine.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;

import cn.icewindy.steamturbine.ModCreativeTab;
import cn.icewindy.steamturbine.SteamTurbineMod;

/**
 * 大型热交换中心管道方块。
 */
public class BlockHeatExchangerPipeCasing extends Block {

    public BlockHeatExchangerPipeCasing() {
        super(Material.iron);
        setBlockName("steamturbine.heat_exchanger_pipe_casing");
        setHardness(3.0F);
        setResistance(10.0F);
        setStepSound(soundTypeMetal);
        setCreativeTab(ModCreativeTab.INSTANCE);
        setHarvestLevel("pickaxe", 1);
    }

    @Override
    public void registerBlockIcons(IIconRegister reg) {
        blockIcon = reg.registerIcon(SteamTurbineMod.MOD_ID + ":heat_exchanger_pipe_casing");
    }
}
