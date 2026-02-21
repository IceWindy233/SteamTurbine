package cn.icewindy.steamturbine.item;

import java.util.List;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;

import cn.icewindy.steamturbine.ModConfig;
import cn.icewindy.steamturbine.ModCreativeTab;
import cn.icewindy.steamturbine.SteamTurbineMod;
import cn.icewindy.steamturbine.util.RotorStats;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 涡轮转子物品（配置驱动）。
 */
public class ItemTurbineRotor extends Item {

    public static final int META_IRON = 0;
    public static final int META_STEEL = 1;
    public static final int META_TITANIUM = 2;

    private IIcon[] icons = new IIcon[0];
    private boolean[] useTint = new boolean[0];

    public ItemTurbineRotor() {
        super();
        setUnlocalizedName("steamturbine.turbine_rotor");
        setCreativeTab(ModCreativeTab.INSTANCE);
        setMaxStackSize(1);
        setHasSubtypes(true);
        setMaxDamage(0);
        setNoRepair();
    }

    /**
     * 获取指定 metadata 的最大耐久度。
     */
    public static int getMaxDurability(int meta) {
        if (ModConfig.isRotorInfiniteDurability(meta)) {
            return Integer.MAX_VALUE;
        }
        return ModConfig.getRotorDurability(meta);
    }

    @Override
    public int getMaxDamage(ItemStack stack) {
        return getMaxDurability(stack.getItemDamage());
    }

    @Override
    public boolean isDamageable() {
        return true;
    }

    @Override
    public boolean showDurabilityBar(ItemStack stack) {
        if (ModConfig.isRotorInfiniteDurability(stack.getItemDamage())) {
            return false;
        }
        return getDurabilityForDisplay(stack) > 0.0;
    }

    @Override
    public double getDurabilityForDisplay(ItemStack stack) {
        int maxDmg = getMaxDurability(stack.getItemDamage());
        if (maxDmg <= 0) return 0.0;
        int used = 0;
        if (stack.hasTagCompound()) {
            used = stack.getTagCompound()
                .getInteger("rotorDamage");
        }
        return (double) used / (double) maxDmg;
    }

    /**
     * 对转子施加磨损。
     *
     * @return 如果转子已损坏返回 true
     */
    public static boolean applyDamage(ItemStack stack, int amount) {
        if (stack == null || !(stack.getItem() instanceof ItemTurbineRotor)) return true;
        if (ModConfig.isRotorInfiniteDurability(stack.getItemDamage())) return false;

        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new net.minecraft.nbt.NBTTagCompound());
        }
        int used = stack.getTagCompound()
            .getInteger("rotorDamage") + amount;
        stack.getTagCompound()
            .setInteger("rotorDamage", used);

        int maxDmg = getMaxDurability(stack.getItemDamage());
        return used >= maxDmg;
    }

    /**
     * 获取转子剩余耐久。
     */
    public static int getRemainingDurability(ItemStack stack) {
        if (stack == null) return 0;
        if (ModConfig.isRotorInfiniteDurability(stack.getItemDamage())) return Integer.MAX_VALUE;
        int maxDmg = getMaxDurability(stack.getItemDamage());
        int used = 0;
        if (stack.hasTagCompound()) {
            used = stack.getTagCompound()
                .getInteger("rotorDamage");
        }
        return Math.max(0, maxDmg - used);
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        String name = normalizeName(ModConfig.getRotorItemName(stack.getItemDamage()));
        return super.getUnlocalizedName() + "." + name;
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        return ModConfig.getRotorItemName(stack.getItemDamage()) + " Turbine Rotor";
    }

    @SuppressWarnings("unchecked")
    @Override
    public void getSubItems(Item item, CreativeTabs tab, List list) {
        int count = Math.max(1, ModConfig.getRotorCount());
        for (int i = 0; i < count; i++) {
            list.add(new ItemStack(item, 1, i));
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister reg) {
        int count = Math.max(1, ModConfig.getRotorCount());
        icons = new IIcon[count];
        useTint = new boolean[count];

        for (int i = 0; i < count; i++) {
            String material = ModConfig.getRotorItemName(i);
            String iconFile = ModConfig.getRotorIconFileName(i);
            
            if (iconFile != null && !iconFile.isEmpty()) {
                String builtin = stripExtension(iconFile);
                icons[i] = reg.registerIcon(SteamTurbineMod.MOD_ID + ":" + builtin);
                useTint[i] = false;
            } else {
                if (!net.minecraftforge.oredict.OreDictionary.getOres("ingot" + material).isEmpty()) {
                    icons[i] = reg.registerIcon(SteamTurbineMod.MOD_ID + ":basicitem/rotor");
                    useTint[i] = true;
                    int color = cn.icewindy.steamturbine.util.ColorExtractor.getAverageColor(material);
                    ModConfig.setCachedColor(i, color);
                } else {
                    icons[i] = reg.registerIcon(SteamTurbineMod.MOD_ID + ":rotor_iron");
                    useTint[i] = false;
                }
            }
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getColorFromItemStack(ItemStack stack, int pass) {
        int meta = stack.getItemDamage();
        if (meta >= 0 && meta < useTint.length && useTint[meta]) {
            return ModConfig.getCachedColor(meta);
        }
        return 0xFFFFFF;
    }

    @Override
    public IIcon getIconFromDamage(int damage) {
        if (icons == null || icons.length == 0) {
            return null;
        }
        if (damage >= 0 && damage < icons.length) {
            return icons[damage];
        }
        return icons[0];
    }

    @SuppressWarnings("unchecked")
    @Override
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
        int meta = stack.getItemDamage();
        RotorStats stats = RotorStats.fromMeta(meta);
        int remaining = getRemainingDurability(stack);
        int maxDur = getMaxDurability(meta);

        tooltip.add("\u00a77Material: \u00a7f" + ModConfig.getRotorItemName(meta));
        tooltip.add("\u00a77Efficiency: \u00a7a" + String.format("%.0f%%", stats.efficiency * 100));
        tooltip.add("\u00a77Optimal Flow: \u00a7b" + stats.optimalFlow + " mB/t");
        tooltip.add("\u00a77Overflow Level: \u00a7e" + stats.overflowMultiplier);
        if (ModConfig.isRotorInfiniteDurability(meta)) {
            tooltip.add("\u00a77Durability: \u00a7aInfinite");
        } else {
            tooltip.add("\u00a77Durability: \u00a7f" + remaining + " / " + maxDur);
        }
    }

    private static String stripExtension(String fileName) {
        if (fileName == null) {
            return "";
        }
        int dot = fileName.lastIndexOf('.');
        if (dot <= 0) {
            return fileName;
        }
        return fileName.substring(0, dot);
    }

    private static String normalizeName(String name) {
        if (name == null) {
            return "rotor";
        }
        String normalized = name.toLowerCase()
            .replaceAll("[^a-z0-9_\\-]", "_");
        if (normalized.isEmpty()) {
            return "rotor";
        }
        return normalized;
    }
}
