package cn.icewindy.steamturbine;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
        // 通用预初始化
    }

    public void init(FMLInitializationEvent event) {
        // 通用初始化
    }

    public void postInit(FMLPostInitializationEvent event) {
        // 通用后初始化
    }
}
