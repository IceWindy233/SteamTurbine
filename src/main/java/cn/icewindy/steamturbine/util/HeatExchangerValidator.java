package cn.icewindy.steamturbine.util;

import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import cn.icewindy.steamturbine.registry.ModBlocks;
import cn.icewindy.steamturbine.tileentity.TileEntityFluidInputHatch;
import cn.icewindy.steamturbine.tileentity.TileEntityFluidOutputHatch;
import cn.icewindy.steamturbine.tileentity.TileEntityHeatExchangerController;
import cn.icewindy.steamturbine.util.multiblock.StructureTemplate;

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

    public static class ValidationResult {

        public final boolean isValid;
        public final String errorMessage;
        public final int casingCount;

        public ValidationResult(boolean isValid, String errorMessage, int casingCount) {
            this.isValid = isValid;
            this.errorMessage = errorMessage;
            this.casingCount = casingCount;
        }
    }

    public static ValidationResult validate(World world, int cx, int cy, int cz, ForgeDirection facing,
        TileEntityHeatExchangerController controller) {
        if (facing == ForgeDirection.UP || facing == ForgeDirection.DOWN) {
            return new ValidationResult(false, "Controller cannot face UP or DOWN", 0);
        }

        controller.clearHatches();

        final int[] casingCount = new int[1];
        final int[] hotInputCount = new int[1];
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
                    return "Invalid block in structure";
                default:
                    return "Unknown template symbol";
            }
        });
        if (error != null) {
            return new ValidationResult(false, error, casingCount[0]);
        }

        if (hotInputCount[0] != 1) {
            return new ValidationResult(false, "Exactly one Hot Input Hatch required", casingCount[0]);
        }
        if (coldOutputCount[0] != 1) {
            return new ValidationResult(false, "Exactly one Cold Output Hatch required", casingCount[0]);
        }
        if (waterInputCount[0] < 1) {
            return new ValidationResult(false, "At least one Water Input Hatch required", casingCount[0]);
        }
        if (steamOutputCount[0] < 1) {
            return new ValidationResult(false, "At least one Steam Output Hatch required", casingCount[0]);
        }
        if (casingCount[0] < 16) {
            return new ValidationResult(false, "Not enough Heat Exchanger Casings", casingCount[0]);
        }

        return new ValidationResult(true, "", casingCount[0]);
    }
}
