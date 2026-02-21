package cn.icewindy.steamturbine.registry;

import cn.icewindy.steamturbine.tileentity.TileEntityDynamoHatch;
import cn.icewindy.steamturbine.tileentity.TileEntityFluidInputHatch;
import cn.icewindy.steamturbine.tileentity.TileEntityFluidOutputHatch;
import cn.icewindy.steamturbine.tileentity.TileEntityHeatExchangerController;
import cn.icewindy.steamturbine.tileentity.TileEntityItemInputBus;
import cn.icewindy.steamturbine.tileentity.TileEntityItemOutputBus;
import cn.icewindy.steamturbine.tileentity.TileEntityTurbineController;
import cpw.mods.fml.common.registry.GameRegistry;

/**
 * TileEntity 注册中心。
 */
public class ModTileEntities {

    public static void init() {
        GameRegistry.registerTileEntity(TileEntityTurbineController.class, "steamturbine:turbine_controller");
        GameRegistry.registerTileEntity(TileEntityDynamoHatch.class, "steamturbine:dynamo_hatch");
        GameRegistry
            .registerTileEntity(TileEntityHeatExchangerController.class, "steamturbine:heat_exchanger_controller");
        GameRegistry.registerTileEntity(TileEntityFluidInputHatch.class, "steamturbine:fluid_input_hatch");
        GameRegistry.registerTileEntity(TileEntityFluidOutputHatch.class, "steamturbine:fluid_output_hatch");
        GameRegistry.registerTileEntity(TileEntityItemInputBus.class, "steamturbine:item_input_bus");
        GameRegistry.registerTileEntity(TileEntityItemOutputBus.class, "steamturbine:item_output_bus");
    }
}
