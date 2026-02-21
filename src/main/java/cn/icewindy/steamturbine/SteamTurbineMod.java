package cn.icewindy.steamturbine;

import cn.icewindy.steamturbine.client.gui.GuiHandler;
import cn.icewindy.steamturbine.registry.ModBlocks;
import cn.icewindy.steamturbine.registry.ModItems;
import cn.icewindy.steamturbine.registry.ModTileEntities;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.NetworkRegistry;

@Mod(
    modid = SteamTurbineMod.MOD_ID,
    name = SteamTurbineMod.MOD_NAME,
    version = Tags.VERSION,
    dependencies = "required-after:IC2;")
public class SteamTurbineMod {

    public static final String MOD_ID = "steamturbine";
    public static final String MOD_NAME = "Steam Turbine";

    @Mod.Instance(MOD_ID)
    public static SteamTurbineMod instance;

    @SidedProxy(
        clientSide = "cn.icewindy.steamturbine.ClientProxy",
        serverSide = "cn.icewindy.steamturbine.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        ModConfig.init(event.getSuggestedConfigurationFile());
        ModBlocks.init();
        ModItems.init();
        ModTileEntities.init();
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        NetworkRegistry.INSTANCE.registerGuiHandler(instance, new GuiHandler());
        proxy.init(event);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit(event);
    }
}
