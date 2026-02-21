package cn.icewindy.steamturbine.util.multiblock;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

/**
 * 基于字符模板的多方块结构描述。
 * 模板按 depth->rows(top->bottom)->cols(left->right) 定义。
 */
public class StructureTemplate {

    public interface CellVisitor {

        /**
         * @return null 表示通过，非空字符串表示错误信息
         */
        String visit(WorldCell cell);
    }

    public static class WorldCell {

        public final int localDepth;
        public final int localX;
        public final int localY;
        public final int worldX;
        public final int worldY;
        public final int worldZ;
        public final char symbol;
        public final Block block;
        public final TileEntity tileEntity;

        public WorldCell(int localDepth, int localX, int localY, int worldX, int worldY, int worldZ, char symbol,
            Block block, TileEntity tileEntity) {
            this.localDepth = localDepth;
            this.localX = localX;
            this.localY = localY;
            this.worldX = worldX;
            this.worldY = worldY;
            this.worldZ = worldZ;
            this.symbol = symbol;
            this.block = block;
            this.tileEntity = tileEntity;
        }
    }

    private final String[][] layers;
    private final int width;
    private final int height;
    private final int depth;
    private final int anchorX;
    private final int anchorY;
    private final int anchorDepth;

    public static StructureTemplate of(int anchorX, int anchorY, int anchorDepth, String[][] layers) {
        return new StructureTemplate(anchorX, anchorY, anchorDepth, layers);
    }

    private StructureTemplate(int anchorX, int anchorY, int anchorDepth, String[][] layers) {
        if (layers == null || layers.length == 0) {
            throw new IllegalArgumentException("layers cannot be empty");
        }
        int expectedHeight = layers[0].length;
        if (expectedHeight == 0) {
            throw new IllegalArgumentException("layer rows cannot be empty");
        }
        int expectedWidth = layers[0][0].length();
        if (expectedWidth == 0) {
            throw new IllegalArgumentException("row width cannot be empty");
        }
        for (int d = 0; d < layers.length; d++) {
            if (layers[d].length != expectedHeight) {
                throw new IllegalArgumentException("all layers must have same row count");
            }
            for (int r = 0; r < layers[d].length; r++) {
                if (layers[d][r].length() != expectedWidth) {
                    throw new IllegalArgumentException("all rows must have same width");
                }
            }
        }
        this.layers = layers;
        this.width = expectedWidth;
        this.height = expectedHeight;
        this.depth = layers.length;
        this.anchorX = anchorX;
        this.anchorY = anchorY;
        this.anchorDepth = anchorDepth;
    }

    public String visit(World world, int anchorWorldX, int anchorWorldY, int anchorWorldZ, ForgeDirection facing,
        CellVisitor visitor) {
        ForgeDirection back = facing.getOpposite();
        ForgeDirection right = getRight(facing);
        ForgeDirection up = ForgeDirection.UP;

        for (int d = 0; d < depth; d++) {
            for (int row = 0; row < height; row++) {
                for (int x = 0; x < width; x++) {
                    char symbol = layers[d][row].charAt(x);
                    if (isIgnoredSymbol(symbol)) continue;

                    int localY = height - 1 - row;
                    int wx = anchorWorldX + right.offsetX * (x - anchorX)
                        + up.offsetX * (localY - anchorY)
                        + back.offsetX * (d - anchorDepth);
                    int wy = anchorWorldY + right.offsetY * (x - anchorX)
                        + up.offsetY * (localY - anchorY)
                        + back.offsetY * (d - anchorDepth);
                    int wz = anchorWorldZ + right.offsetZ * (x - anchorX)
                        + up.offsetZ * (localY - anchorY)
                        + back.offsetZ * (d - anchorDepth);

                    Block block = world.getBlock(wx, wy, wz);
                    TileEntity te = world.getTileEntity(wx, wy, wz);
                    WorldCell cell = new WorldCell(d, x, localY, wx, wy, wz, symbol, block, te);
                    String error = visitor.visit(cell);
                    if (error != null) {
                        return error;
                    }
                }
            }
        }
        return null;
    }

    public List<ChunkCoordinates> collectOccupied(World world, int anchorWorldX, int anchorWorldY, int anchorWorldZ,
        ForgeDirection facing) {
        List<ChunkCoordinates> result = new ArrayList<ChunkCoordinates>();
        visit(world, anchorWorldX, anchorWorldY, anchorWorldZ, facing, cell -> {
            result.add(new ChunkCoordinates(cell.worldX, cell.worldY, cell.worldZ));
            return null;
        });
        return result;
    }

    private boolean isIgnoredSymbol(char c) {
        return c == '~' || c == ' ';
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
