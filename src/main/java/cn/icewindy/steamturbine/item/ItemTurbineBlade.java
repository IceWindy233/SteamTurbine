package cn.icewindy.steamturbine.item;

import java.util.List;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.util.StatCollector;
import net.minecraftforge.oredict.OreDictionary;

import cn.icewindy.steamturbine.ModConfig;
import cn.icewindy.steamturbine.ModCreativeTab;
import cn.icewindy.steamturbine.SteamTurbineMod;
import cn.icewindy.steamturbine.util.ItemParser;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 涡轮叶片物品（配置驱动，可选注册）。
 */
public class ItemTurbineBlade extends Item {

    private IIcon[] icons;
    private boolean[] useTint;

    public ItemTurbineBlade() {
        super();
        setUnlocalizedName("steamturbine.turbine_blade");
        setCreativeTab(ModCreativeTab.INSTANCE);
        setHasSubtypes(true);
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        String material = ModConfig.getRotorItemName(stack.getItemDamage())
            .toLowerCase();
        return super.getUnlocalizedName() + "." + material;
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        String unlocalizedName = getUnlocalizedName(stack) + ".name";
        if (StatCollector.canTranslate(unlocalizedName)) {
            return StatCollector.translateToLocal(unlocalizedName);
        }

        String materialName = ModConfig.getRotorItemName(stack.getItemDamage());
        String translatedMaterial = translateMaterial(materialName);
        String template = StatCollector.translateToLocal("steamturbine.item.blade.name");
        return String.format(template, translatedMaterial);
    }

    private String translateMaterial(String material) {
        if (ItemParser.isStackFormat(material)) {
            ItemStack stack = ItemParser.parseStack(material);
            if (stack != null) {
                return stack.getDisplayName();
            }
        }
        String ingotName = "ingot" + material;
        List<ItemStack> ores = OreDictionary.getOres(ingotName);
        if (ores != null && !ores.isEmpty()) {
            return ores.get(0)
                .getDisplayName();
        }
        return material;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void getSubItems(Item item, CreativeTabs tab, List list) {
        int count = ModConfig.getRotorCount();
        for (int i = 0; i < count; i++) {
            if (ModConfig.hasRotorBlade(i)) {
                list.add(new ItemStack(item, 1, i));
            }
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister reg) {
        int count = ModConfig.getRotorCount();
        icons = new IIcon[count];
        useTint = new boolean[count];

        for (int i = 0; i < count; i++) {
            if (ModConfig.hasRotorBlade(i)) {
                String material = ModConfig.getRotorItemName(i);
                String bladeIcon = ModConfig.getBladeIconFileName(i);

                if (bladeIcon != null && !bladeIcon.isEmpty()) {
                    String builtin = stripExtension(bladeIcon);
                    icons[i] = reg.registerIcon(SteamTurbineMod.MOD_ID + ":" + builtin);
                    useTint[i] = false;
                } else if (ItemParser.isStackFormat(material)) {
                    // For custom stack format, use the basic texture and apply target item color
                    icons[i] = reg.registerIcon(SteamTurbineMod.MOD_ID + ":basicitem/blade");
                    useTint[i] = true;
                } else {
                    if (!net.minecraftforge.oredict.OreDictionary.getOres("ingot" + material)
                        .isEmpty()) {
                        icons[i] = reg.registerIcon(SteamTurbineMod.MOD_ID + ":basicitem/blade");
                        useTint[i] = true;
                        // Color should already be cached by Rotor item, but we can set it again if needed
                        int color = cn.icewindy.steamturbine.util.ColorExtractor.getAverageColor(material);
                        ModConfig.setCachedColor(i, color);
                    } else {
                        // Fallback
                        icons[i] = reg.registerIcon(SteamTurbineMod.MOD_ID + ":basicitem/blade");
                        useTint[i] = true;
                    }
                }
            }
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getColorFromItemStack(ItemStack stack, int pass) {
        int meta = stack.getItemDamage();
        String itemName = ModConfig.getRotorItemName(meta);
        if (ItemParser.isStackFormat(itemName)) {
            ItemStack targetStack = ItemParser.parseStack(itemName);
            if (targetStack != null) {
                return targetStack.getItem()
                    .getColorFromItemStack(targetStack, pass);
            }
        }
        if (useTint != null && meta >= 0 && meta < useTint.length && useTint[meta]) {
            return ModConfig.getCachedColor(meta);
        }
        return 0xFFFFFF;
    }

    private static String stripExtension(String fileName) {
        if (fileName == null) return "";
        int dot = fileName.lastIndexOf('.');
        if (dot <= 0) return fileName;
        return fileName.substring(0, dot);
    }

    @Override
    public IIcon getIconFromDamage(int damage) {
        if (icons == null || damage < 0 || damage >= icons.length) {
            return null;
        }
        return icons[damage];
    }
}
