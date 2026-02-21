package cn.icewindy.steamturbine;

import cpw.mods.fml.common.event.FMLPreInitializationEvent;

public class ClientProxy extends CommonProxy {

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
        // 客户端渲染注册（当前使用标准方块渲染，无需额外 TESR）
    }
}
