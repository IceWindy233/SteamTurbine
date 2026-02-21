package cn.icewindy.steamturbine.registry;

import net.minecraft.block.Block;

import cn.icewindy.steamturbine.block.BlockDynamoHatch;
import cn.icewindy.steamturbine.block.BlockInputHatch;
import cn.icewindy.steamturbine.block.BlockOutputHatch;
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
    public static Block inputHatch;
    public static Block outputHatch;
    public static Block dynamoHatch;
    public static Block redstoneControl;

    public static void init() {
        turbineCasing = new BlockTurbineCasing();
        turbineController = new BlockTurbineController();
        inputHatch = new BlockInputHatch();
        outputHatch = new BlockOutputHatch();
        dynamoHatch = new BlockDynamoHatch();
        redstoneControl = new BlockRedstoneControl();

        GameRegistry.registerBlock(turbineCasing, ItemBlockTurbineCasing.class, "turbine_casing");
        GameRegistry.registerBlock(turbineController, "turbine_controller");
        GameRegistry.registerBlock(inputHatch, "input_hatch");
        GameRegistry.registerBlock(outputHatch, "output_hatch");
        GameRegistry.registerBlock(dynamoHatch, "dynamo_hatch");
        GameRegistry.registerBlock(redstoneControl, "redstone_control");
    }
}
