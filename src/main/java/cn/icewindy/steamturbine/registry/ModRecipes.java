package cn.icewindy.steamturbine.registry;

import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import cn.icewindy.steamturbine.ModConfig;
import cn.icewindy.steamturbine.util.ItemParser;
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
        registerMachineRecipes();
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

    private static void registerMachineRecipes() {
        // Utils to get IC2 items
        ItemStack ic2Machine = ItemParser.parseStack("<IC2:blockMachine>");
        ItemStack ic2MachineAdvanced = ItemParser.parseStack("<IC2:blockMachine:12>");
        ItemStack ic2Alloy = ItemParser.parseStack("<IC2:itemPartAlloy>");
        ItemStack ic2Lapotron = ItemParser.parseStack("<IC2:itemBatLamaCrystal:1>");
        if (ic2Lapotron != null) ic2Lapotron.setItemDamage(OreDictionary.WILDCARD_VALUE);

        ItemStack ic2EnergyCrystal = ItemParser.parseStack("<IC2:itemBatCrystal:26>");
        if (ic2EnergyCrystal != null) ic2EnergyCrystal.setItemDamage(OreDictionary.WILDCARD_VALUE);

        ItemStack ic2CableGF = ItemParser.parseStack("<IC2:itemCable:9>");
        ItemStack ic2CopperVent = ItemParser.parseStack("<IC2:itemRecipePart:5>");
        ItemStack ic2FluidCell = ItemParser.parseStack("<IC2:itemFluidCell>");

        // 1. Turbine Casing
        if (ic2Machine != null && ic2Alloy != null) {
            GameRegistry.addRecipe(
                new ShapedOreRecipe(
                    new ItemStack(ModBlocks.turbineCasing, 8),
                    "APA",
                    "PMP",
                    "APA",
                    'A',
                    ic2Alloy,
                    'P',
                    "plateDenseSteel",
                    'M',
                    ic2Machine));
        }

        // 2. Turbine Controller
        if (ic2Lapotron != null) {
            GameRegistry.addRecipe(
                new ShapedOreRecipe(
                    new ItemStack(ModBlocks.turbineController, 1),
                    "KRK",
                    "RCR",
                    "KRK",
                    'K',
                    ic2Lapotron,
                    'R',
                    "circuitAdvanced",
                    'C',
                    ModBlocks.turbineCasing));
        }

        // 3. Fluid Input Hatch
        if (ic2FluidCell != null) {
            GameRegistry.addRecipe(
                new ShapelessOreRecipe(
                    new ItemStack(ModBlocks.fluidInputHatch, 1),
                    ModBlocks.turbineCasing,
                    ic2FluidCell));
        }
        GameRegistry
            .addRecipe(new ShapelessOreRecipe(new ItemStack(ModBlocks.fluidInputHatch, 1), ModBlocks.fluidOutputHatch));

        // 4. Fluid Output Hatch
        GameRegistry
            .addRecipe(new ShapelessOreRecipe(new ItemStack(ModBlocks.fluidOutputHatch, 1), ModBlocks.fluidInputHatch));

        // 5. Energy Hatch
        if (ic2EnergyCrystal != null && ic2CableGF != null) {
            GameRegistry.addRecipe(
                new ShapedOreRecipe(
                    new ItemStack(ModBlocks.dynamoHatch, 1),
                    "KBK",
                    "BCB",
                    "KBK",
                    'K',
                    ic2EnergyCrystal,
                    'B',
                    ic2CableGF,
                    'C',
                    ModBlocks.turbineCasing));
        }

        // 6. Redstone Control Block
        GameRegistry.addRecipe(
            new ShapelessOreRecipe(new ItemStack(ModBlocks.redstoneControl, 1), ModBlocks.turbineCasing, Blocks.lever));

        // 7. Heat Exchanger Casing
        if (ic2Machine != null && ic2CopperVent != null) {
            GameRegistry.addRecipe(
                new ShapedOreRecipe(
                    new ItemStack(ModBlocks.heatExchangerCasing, 8),
                    "RRR",
                    "RMR",
                    "RRR",
                    'R',
                    ic2CopperVent,
                    'M',
                    ic2Machine));
        }

        // 8. Heat Exchanger Pipe Block
        if (ic2MachineAdvanced != null && ic2Alloy != null) {
            GameRegistry.addRecipe(
                new ShapedOreRecipe(
                    new ItemStack(ModBlocks.heatExchangerPipeCasing, 1),
                    "AAA",
                    "AMA",
                    "AAA",
                    'A',
                    ic2Alloy,
                    'M',
                    ic2MachineAdvanced));
        }

        // 9. Heat Exchanger Controller
        if (ic2Alloy != null) {
            GameRegistry.addRecipe(
                new ShapedOreRecipe(
                    new ItemStack(ModBlocks.heatExchangerController, 1),
                    "APA",
                    "PCP",
                    "APA",
                    'A',
                    ic2Alloy,
                    'P',
                    "plateDenseSteel",
                    'C',
                    ModBlocks.heatExchangerCasing));
        }
    }
}
