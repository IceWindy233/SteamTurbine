package cn.icewindy.steamturbine.registry;

import net.minecraft.item.Item;

import cn.icewindy.steamturbine.item.ItemTurbineBlade;
import cn.icewindy.steamturbine.item.ItemTurbineRotor;
import cpw.mods.fml.common.registry.GameRegistry;

/**
 * 物品注册中心。
 */
public class ModItems {

    public static Item turbineRotor;
    public static Item turbineBlade;

    public static void init() {
        turbineRotor = new ItemTurbineRotor();
        GameRegistry.registerItem(turbineRotor, "turbine_rotor");

        turbineBlade = new ItemTurbineBlade();
        GameRegistry.registerItem(turbineBlade, "turbine_blade");
    }
}
