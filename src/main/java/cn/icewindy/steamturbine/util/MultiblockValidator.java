package cn.icewindy.steamturbine.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import cn.icewindy.steamturbine.registry.ModBlocks;
import cn.icewindy.steamturbine.tileentity.TileEntityDynamoHatch;
import cn.icewindy.steamturbine.tileentity.TileEntityInputHatch;
import cn.icewindy.steamturbine.tileentity.TileEntityOutputHatch;
import cn.icewindy.steamturbine.tileentity.TileEntityTurbineController;

/**
 * 多方块结构验证引擎（改进版）。
 *
 * 结构布局（相对于控制器位置）：
 * ┌─────────────────────────────────────┐
 * │ 层0（前） │ 层1（中前） │ 层2（中后） │ 层3（后） │
 * ├──────────┼──────────┼──────────┼──────────┤
 * │ Control │ Casing │ Casing │ Casing │
 * │ (center) │ (center) │ (center) │ (center) │
 * │ + 8 周边 │ + 8 周边 │ + 8 周边 │ Dynamo │
 * └─────────────────────────────────────┘
 *
 * 每层都是 3×3 的正方形（X轴±1，Y轴±1）
 */
public class MultiblockValidator {

    // 结构参数（固定）
    private static final int DEPTH = 4; // 层数
    private static final int WIDTH = 3; // 宽度（3×3）
    private static final int HEIGHT = 3; // 高度（3×3）
    private static final int RADIUS = 1; // 半径（±1）

    // 全局占用注册表：ChunkCoordinates (x,y,z) -> Controller TileEntity
    private static final Map<ChunkCoordinates, TileEntityTurbineController> occupiedBlocks = new ConcurrentHashMap<>();

    /**
     * 清理某个控制器占用的所有方块。
     */
    public static void releaseBlocks(TileEntityTurbineController controller) {
        occupiedBlocks.values()
            .removeIf(v -> v == controller);
    }

    /**
     * 验证结果数据类。
     */
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

        @Override
        public String toString() {
            if (isValid) {
                return String.format(
                    "Valid: %d components (Casing:%d, Input:%d, Output:%d, Dynamo:%d, RedstoneControl:%d)",
                    componentCount,
                    casingCount,
                    inputHatchCount,
                    outputHatchCount,
                    dynamoHatchCount,
                    redstoneControlCount);
            } else {
                return "Invalid: " + errorMessage;
            }
        }
    }

    /**
     * 执行多方块结构验证。
     *
     * @param world       世界实例
     * @param controllerX 控制器X坐标
     * @param controllerY 控制器Y坐标
     * @param controllerZ 控制器Z坐标
     * @param facing      控制器朝向（表示前面方向）
     * @param controller  TileEntity实例，用于收集Hatch。可为null则仅验证结构。
     * @return 详细的验证结果
     */
    public static ValidationResult validate(World world, int controllerX, int controllerY, int controllerZ,
        ForgeDirection facing, TileEntityTurbineController controller) {
        // 验证参数
        if (world == null) {
            return new ValidationResult(false, 0, 0, 0, 0, 0, 0, "World is null");
        }

        if (facing == ForgeDirection.UP || facing == ForgeDirection.DOWN) {
            return new ValidationResult(false, 0, 0, 0, 0, 0, 0, "Controller cannot face UP or DOWN");
        }

        // 清空Hatch列表
        if (controller != null) {
            controller.clearHatches();
        }

        // 计算方向向量
        ForgeDirection depth = facing.getOpposite(); // 从控制器向后
        ForgeDirection right = getRight(facing);
        ForgeDirection up = ForgeDirection.UP;

        // 计数器
        int totalComponents = 0;
        int casingCount = 0;
        int inputHatchCount = 0;
        int outputHatchCount = 0;
        int dynamoHatchCount = 0;
        int redstoneControlCount = 0;

        // 遍历所有4层
        for (int layer = 0; layer < DEPTH; layer++) {
            for (int x = -RADIUS; x <= RADIUS; x++) {
                for (int y = -RADIUS; y <= RADIUS; y++) {
                    // 计算实际坐标
                    int bx = controllerX + depth.offsetX * layer + right.offsetX * x + up.offsetX * y;
                    int by = controllerY + depth.offsetY * layer + right.offsetY * x + up.offsetY * y;
                    int bz = controllerZ + depth.offsetZ * layer + right.offsetZ * x + up.offsetZ * y;

                    // 特殊情况：层0中心是控制器自身
                    if (layer == 0 && x == 0 && y == 0) {
                        continue;
                    }

                    // 检查方块是否被其他机器占用
                    ChunkCoordinates coords = new ChunkCoordinates(bx, by, bz);
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
                            String.format("Block at (%d, %d, %d) is already used by another turbine", bx, by, bz));
                    }

                    // 特殊情况：层1和层2的中心应为空气（转子旋转空间）
                    if ((layer == 1 || layer == 2) && x == 0 && y == 0) {
                        Block block = world.getBlock(bx, by, bz);
                        if (!block.isAir(world, bx, by, bz)) {
                            return new ValidationResult(
                                false,
                                0,
                                0,
                                0,
                                0,
                                0,
                                0,
                                String.format(
                                    "Layer %d center must be air, found %s at (%d, %d, %d)",
                                    layer,
                                    block.getLocalizedName(),
                                    bx,
                                    by,
                                    bz));
                        }
                        continue;
                    }

                    // 验证方块类型
                    Block block = world.getBlock(bx, by, bz);
                    TileEntity te = world.getTileEntity(bx, by, bz);

                    boolean isValidComponent = false;
                    boolean isBackCenter = layer == DEPTH - 1 && x == 0 && y == 0;

                    // 对齐 GT5U 规则：后中心必须是 Dynamo Hatch，且仅此一处允许 Dynamo
                    if (isBackCenter) {
                        if (!(te instanceof TileEntityDynamoHatch)) {
                            return new ValidationResult(
                                false,
                                0,
                                0,
                                0,
                                0,
                                0,
                                0,
                                String.format("Back center must be Dynamo Hatch at (%d, %d, %d)", bx, by, bz));
                        }
                        dynamoHatchCount++;
                        if (controller != null) {
                            controller.addDynamoHatch((TileEntityDynamoHatch) te);
                        }
                        totalComponents++;
                        continue;
                    }

                    // 检查是否为涡轮外壳
                    if (block == ModBlocks.turbineCasing) {
                        casingCount++;
                        isValidComponent = true;
                    }
                    // 检查是否为输入Hatch
                    else if (te instanceof TileEntityInputHatch) {
                        inputHatchCount++;
                        if (controller != null) {
                            controller.addInputHatch((TileEntityInputHatch) te);
                        }
                        isValidComponent = true;
                    }
                    // 检查是否为输出Hatch
                    else if (te instanceof TileEntityOutputHatch) {
                        outputHatchCount++;
                        if (controller != null) {
                            controller.addOutputHatch((TileEntityOutputHatch) te);
                        }
                        isValidComponent = true;
                    }
                    // 检查是否为Dynamo Hatch
                    else if (te instanceof TileEntityDynamoHatch) {
                        return new ValidationResult(
                            false,
                            0,
                            0,
                            0,
                            0,
                            0,
                            0,
                            String
                                .format("Dynamo Hatch only allowed at back center, found at (%d, %d, %d)", bx, by, bz));
                    }
                    // 检查是否为红石控制方块
                    else if (block == ModBlocks.redstoneControl) {
                        redstoneControlCount++;
                        if (controller != null) {
                            controller.addRedstoneControlBlock(bx, by, bz);
                        }
                        isValidComponent = true;
                    }

                    if (!isValidComponent) {
                        return new ValidationResult(
                            false,
                            0,
                            0,
                            0,
                            0,
                            0,
                            0,
                            String
                                .format("Invalid component at (%d, %d, %d): %s", bx, by, bz, block.getLocalizedName()));
                    }

                    totalComponents++;
                }
            }
        }

        // 验证最小组件数量
        if (casingCount < 16) {
            return new ValidationResult(
                false,
                totalComponents,
                casingCount,
                inputHatchCount,
                outputHatchCount,
                dynamoHatchCount,
                redstoneControlCount,
                String.format("Not enough casings: %d < 16", casingCount));
        }

        if (inputHatchCount < 1) {
            return new ValidationResult(
                false,
                totalComponents,
                casingCount,
                inputHatchCount,
                outputHatchCount,
                dynamoHatchCount,
                redstoneControlCount,
                "At least 1 input hatch required");
        }

        if (outputHatchCount < 1) {
            return new ValidationResult(
                false,
                totalComponents,
                casingCount,
                inputHatchCount,
                outputHatchCount,
                dynamoHatchCount,
                redstoneControlCount,
                "At least 1 output hatch required");
        }

        if (dynamoHatchCount != 1) {
            return new ValidationResult(
                false,
                totalComponents,
                casingCount,
                inputHatchCount,
                outputHatchCount,
                dynamoHatchCount,
                redstoneControlCount,
                "Exactly 1 dynamo hatch required at back center");
        }

        if (redstoneControlCount > 1) {
            return new ValidationResult(
                false,
                totalComponents,
                casingCount,
                inputHatchCount,
                outputHatchCount,
                dynamoHatchCount,
                redstoneControlCount,
                "At most 1 redstone control block allowed");
        }

        // 验证成功，标记占用（如果提供了控制器）
        if (controller != null) {
            for (int layer = 0; layer < DEPTH; layer++) {
                for (int x = -RADIUS; x <= RADIUS; x++) {
                    for (int y = -RADIUS; y <= RADIUS; y++) {
                        int bx = controllerX + depth.offsetX * layer + right.offsetX * x + up.offsetX * y;
                        int by = controllerY + depth.offsetY * layer + right.offsetY * x + up.offsetY * y;
                        int bz = controllerZ + depth.offsetZ * layer + right.offsetZ * x + up.offsetZ * y;
                        occupiedBlocks.put(new ChunkCoordinates(bx, by, bz), controller);
                    }
                }
            }
        }

        return new ValidationResult(
            true,
            totalComponents,
            casingCount,
            inputHatchCount,
            outputHatchCount,
            dynamoHatchCount,
            redstoneControlCount,
            null);
    }

    /**
     * 简化的验证接口（不收集Hatch）。
     */
    public static boolean isValid(World world, int controllerX, int controllerY, int controllerZ,
        ForgeDirection facing) {
        ValidationResult result = validate(world, controllerX, controllerY, controllerZ, facing, null);
        return result.isValid;
    }

    /**
     * 根据朝向获取右侧方向。
     */
    private static ForgeDirection getRight(ForgeDirection facing) {
        switch (facing) {
            case NORTH:
                return ForgeDirection.EAST;
            case SOUTH:
                return ForgeDirection.WEST;
            case EAST:
                return ForgeDirection.SOUTH;
            case WEST:
                return ForgeDirection.NORTH;
            default:
                return ForgeDirection.EAST;
        }
    }

    /**
     * 调试用：打印验证过程中的详细信息。
     */
    public static void debugPrintStructure(World world, int controllerX, int controllerY, int controllerZ,
        ForgeDirection facing) {
        System.out.println("=== Multiblock Structure Debug ===");
        System.out.println("Controller: (" + controllerX + ", " + controllerY + ", " + controllerZ + ")");
        System.out.println("Facing: " + facing);

        ForgeDirection depth = facing.getOpposite();
        ForgeDirection right = getRight(facing);
        ForgeDirection up = ForgeDirection.UP;

        for (int layer = 0; layer < DEPTH; layer++) {
            System.out.println("\nLayer " + layer + ":");
            for (int y = RADIUS; y >= -RADIUS; y--) {
                for (int x = -RADIUS; x <= RADIUS; x++) {
                    int bx = controllerX + depth.offsetX * layer + right.offsetX * x + up.offsetX * y;
                    int by = controllerY + depth.offsetY * layer + right.offsetY * x + up.offsetY * y;
                    int bz = controllerZ + depth.offsetZ * layer + right.offsetZ * x + up.offsetZ * y;

                    Block block = world.getBlock(bx, by, bz);
                    String blockName = block.getLocalizedName();
                    if (blockName.length() > 10) {
                        blockName = blockName.substring(0, 7) + "..";
                    }

                    if (layer == 0 && x == 0 && y == 0) {
                        System.out.print("[CTR]");
                    } else {
                        System.out.print("[" + blockName.substring(0, Math.min(3, blockName.length())) + "]");
                    }
                }
                System.out.println();
            }
        }
    }
}
