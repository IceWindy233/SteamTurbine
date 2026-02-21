package cn.icewindy.steamturbine.registry;

import cn.icewindy.steamturbine.tileentity.TileEntityDynamoHatch;
import cn.icewindy.steamturbine.tileentity.TileEntityInputHatch;
import cn.icewindy.steamturbine.tileentity.TileEntityOutputHatch;
import cn.icewindy.steamturbine.tileentity.TileEntityTurbineController;
import cpw.mods.fml.common.registry.GameRegistry;

/**
 * TileEntity 注册中心。
 */
public class ModTileEntities {

    public static void init() {
        GameRegistry.registerTileEntity(TileEntityTurbineController.class, "steamturbine:turbine_controller");
        GameRegistry.registerTileEntity(TileEntityInputHatch.class, "steamturbine:input_hatch");
        GameRegistry.registerTileEntity(TileEntityOutputHatch.class, "steamturbine:output_hatch");
        GameRegistry.registerTileEntity(TileEntityDynamoHatch.class, "steamturbine:dynamo_hatch");
    }
}
