package cn.icewindy.steamturbine;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;

import cn.icewindy.steamturbine.registry.ModBlocks;

/**
 * Steam Turbine Mod 自定义创造模式标签页。
 * 标签页图标使用涡轮控制器方块。
 */
public class ModCreativeTab extends CreativeTabs {

    public static final ModCreativeTab INSTANCE = new ModCreativeTab();

    private ModCreativeTab() {
        super(SteamTurbineMod.MOD_ID);
    }

    @Override
    public Item getTabIconItem() {
        if (ModBlocks.turbineController != null) {
            return Item.getItemFromBlock(ModBlocks.turbineController);
        }
        return Item.getItemFromBlock(ModBlocks.turbineCasing);
    }
}
