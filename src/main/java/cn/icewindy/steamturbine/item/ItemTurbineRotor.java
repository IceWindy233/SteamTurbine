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

/**
 * 涡轮转子物品（多材质）。
 * 通过 metadata 区分材质：0=铁, 1=钢, 2=钛。
 * 每种材质有不同的效率、最优流量和耐久度。
 */
public class ItemTurbineRotor extends Item {

    public static final int META_IRON = 0;
    public static final int META_STEEL = 1;
    public static final int META_TITANIUM = 2;
    public static final int META_COUNT = 3;

    private static final String[] NAMES = { "iron", "steel", "titanium" };
    private static final String[] DISPLAY_NAMES = { "Iron", "Steel", "Titanium" };

    private IIcon[] icons;

    public ItemTurbineRotor() {
        super();
        setUnlocalizedName("steamturbine.turbine_rotor");
        setCreativeTab(ModCreativeTab.INSTANCE);
        setMaxStackSize(1);
        setHasSubtypes(true);
        setMaxDamage(0); // 由子类型单独控制耐久
        setNoRepair();
    }

    /**
     * 获取指定 metadata 的最大耐久度。
     */
    public static int getMaxDurability(int meta) {
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
        // 仅当转子有磨损时显示耐久条
        return getDurabilityForDisplay(stack) > 0.0;
    }

    @Override
    public double getDurabilityForDisplay(ItemStack stack) {
        int maxDmg = getMaxDurability(stack.getItemDamage());
        if (maxDmg <= 0) return 0.0;
        // NBT 中存储已使用的耐久
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
        int meta = stack.getItemDamage();
        if (meta >= 0 && meta < NAMES.length) {
            return super.getUnlocalizedName() + "." + NAMES[meta];
        }
        return super.getUnlocalizedName();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void getSubItems(Item item, CreativeTabs tab, List list) {
        for (int i = 0; i < META_COUNT; i++) {
            list.add(new ItemStack(item, 1, i));
        }
    }

    @Override
    public void registerIcons(IIconRegister reg) {
        icons = new IIcon[META_COUNT];
        for (int i = 0; i < META_COUNT; i++) {
            icons[i] = reg.registerIcon(SteamTurbineMod.MOD_ID + ":rotor_" + NAMES[i]);
        }
    }

    @Override
    public IIcon getIconFromDamage(int damage) {
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

        tooltip.add("\u00a77Material: \u00a7f" + DISPLAY_NAMES[Math.min(meta, DISPLAY_NAMES.length - 1)]);
        tooltip.add("\u00a77Efficiency: \u00a7a" + String.format("%.0f%%", stats.efficiency * 100));
        tooltip.add("\u00a77Optimal Flow: \u00a7b" + stats.optimalFlow + " L/t");
        tooltip.add("\u00a77Overflow Level: \u00a7e" + stats.overflowMultiplier);
        tooltip.add("\u00a77Durability: \u00a7f" + remaining + " / " + maxDur);
    }
}
