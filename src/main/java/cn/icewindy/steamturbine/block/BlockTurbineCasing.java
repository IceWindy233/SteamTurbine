package cn.icewindy.steamturbine.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;

import cn.icewindy.steamturbine.ModCreativeTab;
import cn.icewindy.steamturbine.SteamTurbineMod;

/**
 * 涡轮外壳方块。
 * 用于构成大型蒸汽涡轮的多方块结构外壳。
 */
public class BlockTurbineCasing extends Block {

    public BlockTurbineCasing() {
        super(Material.iron);
        setBlockName("steamturbine.turbine_casing");
        setHardness(3.0F);
        setResistance(10.0F);
        setStepSound(soundTypeMetal);
        setCreativeTab(ModCreativeTab.INSTANCE);
        setHarvestLevel("pickaxe", 1);
    }

    @Override
    public void registerBlockIcons(IIconRegister reg) {
        blockIcon = reg.registerIcon(SteamTurbineMod.MOD_ID + ":turbine_casing");
    }
}
