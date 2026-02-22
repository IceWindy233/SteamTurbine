package cn.icewindy.steamturbine;

import cpw.mods.fml.common.event.FMLPreInitializationEvent;

public class ClientProxy extends CommonProxy {

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
        cpw.mods.fml.client.registry.ClientRegistry.bindTileEntitySpecialRenderer(
            cn.icewindy.steamturbine.tileentity.TileEntityTurbineController.class,
            new cn.icewindy.steamturbine.client.render.TileEntityTurbineControllerRenderer());
        cpw.mods.fml.client.registry.ClientRegistry.bindTileEntitySpecialRenderer(
            cn.icewindy.steamturbine.tileentity.TileEntityHeatExchangerController.class,
            new cn.icewindy.steamturbine.client.render.TileEntityHeatExchangerControllerRenderer());
    }
}
