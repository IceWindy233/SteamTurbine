package cn.icewindy.steamturbine.util;

import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import cn.icewindy.steamturbine.registry.ModBlocks;
import cn.icewindy.steamturbine.tileentity.TileEntityFluidInputHatch;
import cn.icewindy.steamturbine.tileentity.TileEntityFluidOutputHatch;
import cn.icewindy.steamturbine.tileentity.TileEntityHeatExchangerController;
import cn.icewindy.steamturbine.util.multiblock.StructureTemplate;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 大型热交换结构验证器（3x4x3，控制器位于底层侧面中心）。
 */
public final class HeatExchangerValidator {

    private HeatExchangerValidator() {}

    private static final StructureTemplate TEMPLATE = StructureTemplate.of(
        1,
        0,
        0,
        // 3x4x3 (宽x高x深)：前/中/后 三个截面，每个截面 4 行(上->下)
        new String[][] { { "XXX", "XXX", "XXX", "X~X" }, { "XOX", "XPX", "XPX", "XHX" },
            { "XXX", "XXX", "XXX", "XXX" } });

    private static final java.util.Map<net.minecraft.util.ChunkCoordinates, TileEntityHeatExchangerController> occupiedBlocks = new java.util.concurrent.ConcurrentHashMap<net.minecraft.util.ChunkCoordinates, TileEntityHeatExchangerController>();
    private static final java.util.List<TileEntityHeatExchangerController> formedControllersCache = new java.util.ArrayList<TileEntityHeatExchangerController>();

    public static TileEntityHeatExchangerController getControllerAt(int x, int y, int z) {
        return occupiedBlocks.get(new net.minecraft.util.ChunkCoordinates(x, y, z));
    }

    public static boolean isPartOfFormed(int x, int y, int z) {
        for (TileEntityHeatExchangerController c : formedControllersCache) {
            if (c.isInvalid()) continue;
            if (x >= c.minX && x <= c.maxX && y >= c.minY && y <= c.maxY && z >= c.minZ && z <= c.maxZ) {
                return true;
            }
        }
        return getControllerAt(x, y, z) != null;
    }

    @SideOnly(Side.CLIENT)
    public static void updateFormedCache(TileEntityHeatExchangerController controller, boolean formed) {
        formedControllersCache.remove(controller);
        if (formed) formedControllersCache.add(controller);
    }

    public static void forceOccupyClient(int x, int y, int z, TileEntityHeatExchangerController controller) {
        occupiedBlocks.put(new net.minecraft.util.ChunkCoordinates(x, y, z), controller);
    }

    public static void releaseBlocks(TileEntityHeatExchangerController controller) {
        if (controller == null) return;
        int cx = controller.xCoord;
        int cy = controller.yCoord;
        int cz = controller.zCoord;
        
        // Remove strictly by identity AND by coordinate matches to clear stale TE instances
        occupiedBlocks.entrySet().removeIf(entry -> {
            TileEntityHeatExchangerController v = entry.getValue();
            return v == controller || (v.xCoord == cx && v.yCoord == cy && v.zCoord == cz);
        });
    }

    public static class ValidationResult {

        public final boolean isValid;
        public final String errorMessage;
        public final int casingCount;
        public int minX, minY, minZ, maxX, maxY, maxZ;

        public ValidationResult(boolean isValid, String errorMessage, int casingCount) {
            this.isValid = isValid;
            this.errorMessage = errorMessage;
            this.casingCount = casingCount;
        }

        public ValidationResult setBounds(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
            return this;
        }
    }

    public static ValidationResult validate(World world, int cx, int cy, int cz, ForgeDirection facing,
        TileEntityHeatExchangerController controller) {
        if (facing == ForgeDirection.UP || facing == ForgeDirection.DOWN) {
            return new ValidationResult(false, "Controller cannot face UP or DOWN", 0);
        }

        // Compute bounds
        int minX = cx, minY = cy, minZ = cz;
        int maxX = cx, maxY = cy, maxZ = cz;
        for (net.minecraft.util.ChunkCoordinates coords : TEMPLATE.collectOccupied(world, cx, cy, cz, facing)) {
            minX = Math.min(minX, coords.posX);
            minY = Math.min(minY, coords.posY);
            minZ = Math.min(minZ, coords.posZ);
            maxX = Math.max(maxX, coords.posX);
            maxY = Math.max(maxY, coords.posY);
            maxZ = Math.max(maxZ, coords.posZ);

            TileEntityHeatExchangerController owner = getControllerAt(coords.posX, coords.posY, coords.posZ);
            if (owner != null && !owner.isInvalid()) {
                // Coordinate-based identity check to ignore stale TE instances at the same position
                boolean isSameMachine = (owner == controller) || 
                                       (owner.xCoord == cx && owner.yCoord == cy && owner.zCoord == cz);
                if (!isSameMachine) {
                    String error = String.format("Overlap at (%d, %d, %d) with controller at (%d, %d, %d)", 
                        coords.posX, coords.posY, coords.posZ, owner.xCoord, owner.yCoord, owner.zCoord);
                    return new ValidationResult(false, error, 0).setBounds(minX, minY, minZ, maxX, maxY, maxZ);
                }
            }
        }

        controller.clearHatches();

        final int[] casingCount = new int[1];
        final int hotInputCount[] = new int[1]; // Correcting array declaration
        final int[] coldOutputCount = new int[1];
        final int[] waterInputCount = new int[1];
        final int[] steamOutputCount = new int[1];

        String error = TEMPLATE.visit(world, cx, cy, cz, facing, cell -> {
            switch (cell.symbol) {
                case 'P':
                    return cell.block == ModBlocks.heatExchangerPipeCasing ? null
                        : "Pipe center must be Heat Exchanger Pipe Casing";
                case 'H':
                    if (!(cell.tileEntity instanceof TileEntityFluidInputHatch)) {
                        return "Bottom center must be Hot Input Hatch";
                    }
                    controller.setHotInputHatch((TileEntityFluidInputHatch) cell.tileEntity);
                    hotInputCount[0]++;
                    return null;
                case 'O':
                    if (!(cell.tileEntity instanceof TileEntityFluidOutputHatch)) {
                        return "Top center (middle slice) must be Cold Output Hatch";
                    }
                    controller.setColdOutputHatch((TileEntityFluidOutputHatch) cell.tileEntity);
                    coldOutputCount[0]++;
                    return null;
                case 'X':
                    if (cell.block == ModBlocks.heatExchangerCasing) {
                        casingCount[0]++;
                        return null;
                    }
                    if (cell.tileEntity instanceof TileEntityFluidInputHatch) {
                        controller.addWaterInputHatch((TileEntityFluidInputHatch) cell.tileEntity);
                        waterInputCount[0]++;
                        return null;
                    }
                    if (cell.tileEntity instanceof TileEntityFluidOutputHatch) {
                        controller.addSteamOutputHatch((TileEntityFluidOutputHatch) cell.tileEntity);
                        steamOutputCount[0]++;
                        return null;
                    }
                    return String.format("Invalid block at (%d, %d, %d): %s", cell.worldX, cell.worldY, cell.worldZ, cell.block.getLocalizedName());
                default:
                    return "Unknown template symbol";
            }
        });
        if (error != null) {
            return new ValidationResult(false, error, casingCount[0]).setBounds(minX, minY, minZ, maxX, maxY, maxZ);
        }

        if (hotInputCount[0] != 1) {
            return new ValidationResult(false, "Exactly one Hot Input Hatch required", casingCount[0])
                .setBounds(minX, minY, minZ, maxX, maxY, maxZ);
        }
        if (coldOutputCount[0] != 1) {
            return new ValidationResult(false, "Exactly one Cold Output Hatch required", casingCount[0])
                .setBounds(minX, minY, minZ, maxX, maxY, maxZ);
        }
        if (waterInputCount[0] < 1) {
            return new ValidationResult(false, "At least one Water Input Hatch required", casingCount[0])
                .setBounds(minX, minY, minZ, maxX, maxY, maxZ);
        }
        if (steamOutputCount[0] < 1) {
            return new ValidationResult(false, "At least one Steam Output Hatch required", casingCount[0])
                .setBounds(minX, minY, minZ, maxX, maxY, maxZ);
        }
        if (casingCount[0] < 16) {
            return new ValidationResult(false, "Not enough Heat Exchanger Casings", casingCount[0])
                .setBounds(minX, minY, minZ, maxX, maxY, maxZ);
        }

        if (controller != null) {
            for (net.minecraft.util.ChunkCoordinates coords : TEMPLATE.collectOccupied(world, cx, cy, cz, facing)) {
                occupiedBlocks.put(coords, controller);
            }
        }

        return new ValidationResult(true, "", casingCount[0]).setBounds(minX, minY, minZ, maxX, maxY, maxZ);
    }
}
