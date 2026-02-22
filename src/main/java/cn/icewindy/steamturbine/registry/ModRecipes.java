package cn.icewindy.steamturbine.registry;

import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.ShapedOreRecipe;

import cn.icewindy.steamturbine.ModConfig;
import cpw.mods.fml.common.registry.GameRegistry;

/**
 * 动态合成注册中心。
 */
public class ModRecipes {

    public static void init() {
        int count = ModConfig.getRotorCount();
        for (int i = 0; i < count; i++) {
            registerRotorRecipes(i);
        }
    }

    private static void registerRotorRecipes(int meta) {
        String material = ModConfig.getRotorItemName(meta);
        if (material == null || material.isEmpty()) return;

        Object ingredient = null;
        if (cn.icewindy.steamturbine.util.ItemParser.isStackFormat(material)) {
            ingredient = cn.icewindy.steamturbine.util.ItemParser.parseStack(material);
        } else {
            // Try different prefixes for OreDict
            String[] prefixes = { "ingot", "gem", "", "dust" };
            for (String prefix : prefixes) {
                if (!net.minecraftforge.oredict.OreDictionary.getOres(prefix + material)
                    .isEmpty()) {
                    ingredient = prefix + material;
                    break;
                }
            }
        }

        if (ingredient == null) return;

        // 检查是否启用了叶片
        boolean hasBlade = ModConfig.hasRotorBlade(meta);

        if (hasBlade) {
            // 合成一：叶片 (Blade)
            // KXX
            // KXX
            // KXK
            // 'X' 代表金属锭, ' ' 代表空
            GameRegistry.addRecipe(
                new ShapedOreRecipe(
                    new ItemStack(ModItems.turbineBlade, 1, meta),
                    " XX",
                    " XX",
                    " X ",
                    'X',
                    ingredient));

            // 合成二：转子 (Rotor)
            // KWK
            // WXW
            // KWK
            // 'X' 代表金属锭, 'W' 代表对应的叶片, ' ' 代表空
            GameRegistry.addRecipe(
                new ShapedOreRecipe(
                    new ItemStack(ModItems.turbineRotor, 1, meta),
                    " W ",
                    "WXW",
                    " W ",
                    'X',
                    ingredient,
                    'W',
                    new ItemStack(ModItems.turbineBlade, 1, meta)));
        } else {
            // 如果没有叶片，通常转子无法通过此方式合成，或者你可以添加一个兜底合成
            // 目前根据用户要求，如果不为 true 则不注册关联物品，这里我们也跳过合成
        }
    }
}
