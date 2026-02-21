package cn.icewindy.steamturbine.registry;

import net.minecraft.block.Block;

import cn.icewindy.steamturbine.block.BlockDynamoHatch;
import cn.icewindy.steamturbine.block.BlockFluidInputHatch;
import cn.icewindy.steamturbine.block.BlockFluidOutputHatch;
import cn.icewindy.steamturbine.block.BlockHeatExchangerCasing;
import cn.icewindy.steamturbine.block.BlockHeatExchangerController;
import cn.icewindy.steamturbine.block.BlockHeatExchangerPipeCasing;
import cn.icewindy.steamturbine.block.BlockItemInputBus;
import cn.icewindy.steamturbine.block.BlockItemOutputBus;
import cn.icewindy.steamturbine.block.BlockRedstoneControl;
import cn.icewindy.steamturbine.block.BlockTurbineCasing;
import cn.icewindy.steamturbine.block.BlockTurbineController;
import cn.icewindy.steamturbine.block.ItemBlockTurbineCasing;
import cpw.mods.fml.common.registry.GameRegistry;

/**
 * 方块注册中心。
 */
public class ModBlocks {

    public static Block turbineCasing;
    public static Block turbineController;
    public static Block dynamoHatch;
    public static Block redstoneControl;
    public static Block heatExchangerCasing;
    public static Block heatExchangerPipeCasing;
    public static Block heatExchangerController;
    public static Block fluidInputHatch;
    public static Block fluidOutputHatch;
    public static Block itemInputBus;
    public static Block itemOutputBus;

    public static void init() {
        turbineCasing = new BlockTurbineCasing();
        turbineController = new BlockTurbineController();
        dynamoHatch = new BlockDynamoHatch();
        redstoneControl = new BlockRedstoneControl();
        heatExchangerCasing = new BlockHeatExchangerCasing();
        heatExchangerPipeCasing = new BlockHeatExchangerPipeCasing();
        heatExchangerController = new BlockHeatExchangerController();
        fluidInputHatch = new BlockFluidInputHatch();
        fluidOutputHatch = new BlockFluidOutputHatch();
        itemInputBus = new BlockItemInputBus();
        itemOutputBus = new BlockItemOutputBus();

        GameRegistry.registerBlock(turbineCasing, ItemBlockTurbineCasing.class, "turbine_casing");
        GameRegistry.registerBlock(turbineController, "turbine_controller");
        GameRegistry.registerBlock(dynamoHatch, "dynamo_hatch");
        GameRegistry.registerBlock(redstoneControl, "redstone_control");
        GameRegistry.registerBlock(heatExchangerCasing, "heat_exchanger_casing");
        GameRegistry.registerBlock(heatExchangerPipeCasing, "heat_exchanger_pipe_casing");
        GameRegistry.registerBlock(heatExchangerController, "heat_exchanger_controller");
        GameRegistry.registerBlock(fluidInputHatch, "fluid_input_hatch");
        GameRegistry.registerBlock(fluidOutputHatch, "fluid_output_hatch");
        GameRegistry.registerBlock(itemInputBus, "item_input_bus");
        GameRegistry.registerBlock(itemOutputBus, "item_output_bus");
    }
}
