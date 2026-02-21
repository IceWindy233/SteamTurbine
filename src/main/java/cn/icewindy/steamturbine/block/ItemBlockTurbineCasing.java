package cn.icewindy.steamturbine.block;

import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

/**
 * 涡轮外壳方块的 ItemBlock。
 * 支持子类型扩展（当前仅 meta 0 = 标准外壳）。
 */
public class ItemBlockTurbineCasing extends ItemBlock {

    public ItemBlockTurbineCasing(Block block) {
        super(block);
        setMaxDamage(0);
        setHasSubtypes(true);
    }

    @Override
    public int getMetadata(int damage) {
        return damage;
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        return super.getUnlocalizedName() + "." + stack.getItemDamage();
    }
}
