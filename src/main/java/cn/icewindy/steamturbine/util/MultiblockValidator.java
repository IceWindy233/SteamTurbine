package cn.icewindy.steamturbine.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import cn.icewindy.steamturbine.registry.ModBlocks;
import cn.icewindy.steamturbine.tileentity.TileEntityDynamoHatch;
import cn.icewindy.steamturbine.tileentity.TileEntityFluidInputHatch;
import cn.icewindy.steamturbine.tileentity.TileEntityFluidOutputHatch;
import cn.icewindy.steamturbine.tileentity.TileEntityTurbineController;
import cn.icewindy.steamturbine.util.multiblock.StructureTemplate;

/**
 * 涡轮多方块验证器（基于模板引擎）。
 */
public class MultiblockValidator {

    private static final StructureTemplate TEMPLATE = StructureTemplate.of(
        1,
        1,
        0,
        new String[][] { { "XXX", "X~X", "XXX" }, { "XXX", "XAX", "XXX" }, { "XXX", "XAX", "XXX" },
            { "XXX", "XDX", "XXX" } });

    private static final Map<ChunkCoordinates, TileEntityTurbineController> occupiedBlocks = new ConcurrentHashMap<>();

    public static void releaseBlocks(TileEntityTurbineController controller) {
        occupiedBlocks.values()
            .removeIf(v -> v == controller);
    }

    public static class ValidationResult {

        public boolean isValid;
        public int componentCount;
        public int casingCount;
        public int inputHatchCount;
        public int outputHatchCount;
        public int dynamoHatchCount;
        public int redstoneControlCount;
        public String errorMessage;

        public ValidationResult(boolean valid, int components, int casings, int inputs, int outputs, int dynamos,
            int redstoneControl, String error) {
            this.isValid = valid;
            this.componentCount = components;
            this.casingCount = casings;
            this.inputHatchCount = inputs;
            this.outputHatchCount = outputs;
            this.dynamoHatchCount = dynamos;
            this.redstoneControlCount = redstoneControl;
            this.errorMessage = error;
        }
    }

    public static ValidationResult validate(World world, int controllerX, int controllerY, int controllerZ,
        ForgeDirection facing, TileEntityTurbineController controller) {
        if (world == null) {
            return new ValidationResult(false, 0, 0, 0, 0, 0, 0, "World is null");
        }
        if (facing == ForgeDirection.UP || facing == ForgeDirection.DOWN) {
            return new ValidationResult(false, 0, 0, 0, 0, 0, 0, "Controller cannot face UP or DOWN");
        }

        if (controller != null) {
            controller.clearHatches();
        }

        // 先检查占用冲突
        for (ChunkCoordinates coords : TEMPLATE.collectOccupied(world, controllerX, controllerY, controllerZ, facing)) {
            TileEntityTurbineController owner = occupiedBlocks.get(coords);
            if (owner != null && owner != controller) {
                return new ValidationResult(
                    false,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    String.format(
                        "Block at (%d, %d, %d) is already used by another turbine",
                        coords.posX,
                        coords.posY,
                        coords.posZ));
            }
        }

        final int[] totalComponents = new int[1];
        final int[] casingCount = new int[1];
        final int[] inputHatchCount = new int[1];
        final int[] outputHatchCount = new int[1];
        final int[] dynamoHatchCount = new int[1];
        final int[] redstoneControlCount = new int[1];

        String error = TEMPLATE.visit(world, controllerX, controllerY, controllerZ, facing, cell -> {
            switch (cell.symbol) {
                case 'A':
                    return cell.block.isAir(world, cell.worldX, cell.worldY, cell.worldZ) ? null
                        : String.format("Center must be air at (%d, %d, %d)", cell.worldX, cell.worldY, cell.worldZ);
                case 'D':
                    if (!(cell.tileEntity instanceof TileEntityDynamoHatch)) {
                        return String.format(
                            "Back center must be Dynamo Hatch at (%d, %d, %d)",
                            cell.worldX,
                            cell.worldY,
                            cell.worldZ);
                    }
                    dynamoHatchCount[0]++;
                    if (controller != null) {
                        controller.addDynamoHatch((TileEntityDynamoHatch) cell.tileEntity);
                    }
                    totalComponents[0]++;
                    return null;
                case 'X':
                    if (cell.block == ModBlocks.turbineCasing) {
                        casingCount[0]++;
                        totalComponents[0]++;
                        return null;
                    }
                    if (cell.tileEntity instanceof TileEntityFluidInputHatch) {
                        inputHatchCount[0]++;
                        totalComponents[0]++;
                        if (controller != null) {
                            controller.addInputHatch((TileEntityFluidInputHatch) cell.tileEntity);
                        }
                        return null;
                    }
                    if (cell.tileEntity instanceof TileEntityFluidOutputHatch) {
                        outputHatchCount[0]++;
                        totalComponents[0]++;
                        if (controller != null) {
                            controller.addOutputHatch((TileEntityFluidOutputHatch) cell.tileEntity);
                        }
                        return null;
                    }
                    if (cell.tileEntity instanceof TileEntityDynamoHatch) {
                        return String.format(
                            "Dynamo Hatch only allowed at back center, found at (%d, %d, %d)",
                            cell.worldX,
                            cell.worldY,
                            cell.worldZ);
                    }
                    if (cell.block == ModBlocks.redstoneControl) {
                        redstoneControlCount[0]++;
                        totalComponents[0]++;
                        if (controller != null) {
                            controller.addRedstoneControlBlock(cell.worldX, cell.worldY, cell.worldZ);
                        }
                        return null;
                    }
                    return String.format(
                        "Invalid component at (%d, %d, %d): %s",
                        cell.worldX,
                        cell.worldY,
                        cell.worldZ,
                        cell.block.getLocalizedName());
                default:
                    return "Unknown template symbol";
            }
        });
        if (error != null) {
            return new ValidationResult(false, 0, 0, 0, 0, 0, 0, error);
        }

        if (casingCount[0] < 16) {
            return new ValidationResult(
                false,
                totalComponents[0],
                casingCount[0],
                inputHatchCount[0],
                outputHatchCount[0],
                dynamoHatchCount[0],
                redstoneControlCount[0],
                String.format("Not enough casings: %d < 16", casingCount[0]));
        }
        if (inputHatchCount[0] < 1) {
            return new ValidationResult(
                false,
                totalComponents[0],
                casingCount[0],
                inputHatchCount[0],
                outputHatchCount[0],
                dynamoHatchCount[0],
                redstoneControlCount[0],
                "At least 1 input hatch required");
        }
        if (outputHatchCount[0] < 1) {
            return new ValidationResult(
                false,
                totalComponents[0],
                casingCount[0],
                inputHatchCount[0],
                outputHatchCount[0],
                dynamoHatchCount[0],
                redstoneControlCount[0],
                "At least 1 output hatch required");
        }
        if (dynamoHatchCount[0] != 1) {
            return new ValidationResult(
                false,
                totalComponents[0],
                casingCount[0],
                inputHatchCount[0],
                outputHatchCount[0],
                dynamoHatchCount[0],
                redstoneControlCount[0],
                "Exactly 1 dynamo hatch required at back center");
        }
        if (redstoneControlCount[0] > 1) {
            return new ValidationResult(
                false,
                totalComponents[0],
                casingCount[0],
                inputHatchCount[0],
                outputHatchCount[0],
                dynamoHatchCount[0],
                redstoneControlCount[0],
                "At most 1 redstone control block allowed");
        }

        if (controller != null) {
            for (ChunkCoordinates coords : TEMPLATE
                .collectOccupied(world, controllerX, controllerY, controllerZ, facing)) {
                occupiedBlocks.put(coords, controller);
            }
        }

        return new ValidationResult(
            true,
            totalComponents[0],
            casingCount[0],
            inputHatchCount[0],
            outputHatchCount[0],
            dynamoHatchCount[0],
            redstoneControlCount[0],
            null);
    }

    public static boolean isValid(World world, int controllerX, int controllerY, int controllerZ,
        ForgeDirection facing) {
        return validate(world, controllerX, controllerY, controllerZ, facing, null).isValid;
    }

    public static void debugPrintStructure(World world, int controllerX, int controllerY, int controllerZ,
        ForgeDirection facing) {
        System.out.println("=== Multiblock Structure Debug ===");
        String error = TEMPLATE.visit(world, controllerX, controllerY, controllerZ, facing, cell -> {
            System.out.println(
                String.format(
                    "d=%d x=%d y=%d -> (%d,%d,%d) symbol=%s block=%s",
                    cell.localDepth,
                    cell.localX,
                    cell.localY,
                    cell.worldX,
                    cell.worldY,
                    cell.worldZ,
                    cell.symbol,
                    cell.block.getLocalizedName()));
            return null;
        });
        if (error != null) {
            System.out.println("Template debug stopped: " + error);
        }
    }
}
