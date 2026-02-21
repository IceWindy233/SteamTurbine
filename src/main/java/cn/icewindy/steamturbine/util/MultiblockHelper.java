package cn.icewindy.steamturbine.util;

import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import cn.icewindy.steamturbine.registry.ModBlocks;
import cn.icewindy.steamturbine.tileentity.TileEntityDynamoHatch;
import cn.icewindy.steamturbine.tileentity.TileEntityInputHatch;
import cn.icewindy.steamturbine.tileentity.TileEntityTurbineController;

/**
 * 多方块结构检测工具。
 * 检测 3x3x4 的涡轮结构是否正确形成，并支持 Hatch。
 */
public class MultiblockHelper {

    /**
     * 检测多方块结构是否正确形成。
     *
     * @param world      世界实例
     * @param cx         控制器 X 坐标
     * @param cy         控制器 Y 坐标
     * @param cz         控制器 Z 坐标
     * @param facing     控制器朝向（ForgeDirection，表示控制器前面的方向）
     * @param controller 控制器 TileEntity 实例（用于收集 Hatch）
     * @return 结构中有效组件的数量，0 表示结构未形成
     */
    public static int checkStructure(World world, int cx, int cy, int cz, ForgeDirection facing,
        TileEntityTurbineController controller) {
        // 计算三个方向向量：depth（从控制器往背面）、right、up
        ForgeDirection depth = facing.getOpposite(); // 从控制器往背面方向
        ForgeDirection right = getRight(facing);
        ForgeDirection up = ForgeDirection.UP;

        if (facing == ForgeDirection.UP || facing == ForgeDirection.DOWN) {
            return 0; // 不支持垂直放置
        }

        // 清空控制器中的 Hatch 列表
        if (controller != null) {
            controller.clearHatches();
        }

        int componentCount = 0;

        // 遍历 4 层（depth 方向），每层 3x3
        for (int d = 0; d < 4; d++) {
            for (int r = -1; r <= 1; r++) {
                for (int u = -1; u <= 1; u++) {
                    int bx = cx + depth.offsetX * d + right.offsetX * r + up.offsetX * u;
                    int by = cy + depth.offsetY * d + right.offsetY * r + up.offsetY * u;
                    int bz = cz + depth.offsetZ * d + right.offsetZ * r + up.offsetZ * u;

                    // 层 0, 位置 (0,0) 是控制器自身
                    if (d == 0 && r == 0 && u == 0) {
                        continue;
                    }

                    // 中间层（层 1 和层 2）中心应为空气
                    if ((d == 1 || d == 2) && r == 0 && u == 0) {
                        if (!world.getBlock(bx, by, bz)
                            .isAir(world, bx, by, bz)) {
                            return 0; // 中心应为空气
                        }
                        continue;
                    }

                    // 其余位置应为涡轮外壳或 Hatch
                    Block block = world.getBlock(bx, by, bz);
                    TileEntity te = world.getTileEntity(bx, by, bz);

                    boolean isValidPart = false;

                    if (block == ModBlocks.turbineCasing) {
                        isValidPart = true;
                    } else if (te instanceof TileEntityInputHatch) {
                        if (controller != null) controller.addInputHatch((TileEntityInputHatch) te);
                        isValidPart = true;
                    } else if (te instanceof TileEntityDynamoHatch) {
                        if (controller != null) controller.addDynamoHatch((TileEntityDynamoHatch) te);
                        isValidPart = true;
                    }

                    if (!isValidPart) {
                        return 0; // 结构不完整
                    }
                    componentCount++;
                }
            }
        }

        return componentCount;
    }

    /**
     * 兼容旧版调用（不处理 Hatch 列表）
     */
    public static int checkStructure(World world, int cx, int cy, int cz, ForgeDirection facing) {
        return checkStructure(world, cx, cy, cz, facing, null);
    }

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
}
