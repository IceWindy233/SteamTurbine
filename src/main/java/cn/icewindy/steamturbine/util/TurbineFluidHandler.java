package cn.icewindy.steamturbine.util;

import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

/**
 * 涡轮流体处理工具类。
 * 负责流体识别、效率加成、冷凝转换等操作。
 */
public class TurbineFluidHandler {

    /** 蒸汽流体相关常数 */
    private static final int STEAM_PER_WATER = 1000; // 1L Steam = 1mB Water

    /**
     * 识别流体是否为蒸汽类型。
     */
    public static boolean isSteam(FluidStack fluid) {
        if (fluid == null || fluid.getFluid() == null) {
            return false;
        }
        return isSteam(fluid.getFluid());
    }

    public static boolean isSteam(Fluid fluid) {
        if (fluid == null) {
            return false;
        }
        String name = fluid.getName()
            .toLowerCase();
        return name.contains("steam") && !name.contains("superheated") && !name.contains("highpressure");
    }

    /**
     * 识别流体是否为过热蒸汽。
     */
    public static boolean isSuperheatedSteam(FluidStack fluid) {
        if (fluid == null || fluid.getFluid() == null) {
            return false;
        }
        return isSuperheatedSteam(fluid.getFluid());
    }

    public static boolean isSuperheatedSteam(Fluid fluid) {
        if (fluid == null) {
            return false;
        }
        String name = fluid.getName()
            .toLowerCase();
        return name.contains("superheated") && name.contains("steam");
    }

    /**
     * 识别流体是否为高温高压蒸汽。
     */
    public static boolean isHighPressureSteam(FluidStack fluid) {
        if (fluid == null || fluid.getFluid() == null) {
            return false;
        }
        return isHighPressureSteam(fluid.getFluid());
    }

    public static boolean isHighPressureSteam(Fluid fluid) {
        if (fluid == null) {
            return false;
        }
        String name = fluid.getName()
            .toLowerCase();
        return (name.contains("highpressure") || name.contains("high_pressure") || name.contains("hpsteam"))
            && name.contains("steam");
    }

    /**
     * 获取指定流体的效率加成倍数。
     *
     * 基准：普通蒸汽 = 1.0x
     * 过热蒸汽 = 1.5x (配置可调)
     * 高温高压蒸汽 = 2.0x (配置可调)
     *
     * @param fluid 流体对象
     * @return 效率加成倍数
     */
    public static float getEfficiencyBonus(Fluid fluid) {
        if (isHighPressureSteam(fluid)) {
            return 2.0f; // 高温高压蒸汽效率加成最高
        } else if (isSuperheatedSteam(fluid)) {
            return 1.5f; // 过热蒸汽中等加成
        } else {
            return 1.0f; // 普通蒸汽无加成
        }
    }

    /**
     * 识别流体是否可被涡轮处理。
     */
    public static boolean isValidTurbineFluid(FluidStack fluid) {
        if (fluid == null || fluid.getFluid() == null) {
            return false;
        }
        return isSteam(fluid) || isSuperheatedSteam(fluid) || isHighPressureSteam(fluid);
    }

    /**
     * 获取流体的基础热值（EU/L）。
     * 这是在最优流量下、100%效率下的理论产出。
     *
     * @param fluid 流体对象
     * @return 热值（EU/L）
     */
    public static float getFluidHeatValue(Fluid fluid) {
        // 标准配置：1L蒸汽 = 1 EU/L 基础热值
        // 即在最优流量下，实际产出 = 1 EU/t * efficiency
        if (isHighPressureSteam(fluid)) {
            return 1.0f; // 所有蒸汽类型基础热值相同
        } else if (isSuperheatedSteam(fluid)) {
            return 1.0f;
        } else if (isSteam(fluid)) {
            return 1.0f;
        }
        return 0.0f;
    }

    /**
     * 蒸汽冷凝成蒸馏水。
     *
     * 转换公式：1000 mB Steam = 1 mB Water
     *
     * @param steamML       蒸汽体积（mL）
     * @param lastRemainder 上一次的冷凝余数（mB）
     * @return [冷凝后的蒸馏水(mB), 新的余数(mB)]
     */
    public static int[] condenseToWater(int steamML, int lastRemainder) {
        int totalWater = lastRemainder + steamML;
        int waterOutput = totalWater / STEAM_PER_WATER;
        int newRemainder = totalWater % STEAM_PER_WATER;
        return new int[] { waterOutput, newRemainder };
    }

    /**
     * 获取蒸馏水流体对象。
     * 如果Mod中没有定义蒸馏水，则返回null。
     */
    public static Fluid getDistilledWater() {
        Fluid fluid = FluidRegistry.getFluid("distilledwater");
        if (fluid == null) {
            fluid = FluidRegistry.getFluid("water"); // 备选方案
        }
        return fluid;
    }

    /**
     * 创建一个蒸馏水FluidStack。
     *
     * @param amount 数量（mB）
     * @return FluidStack，若无蒸馏水则返回null
     */
    public static FluidStack createDistilledWater(int amount) {
        Fluid water = getDistilledWater();
        if (water == null) {
            return null;
        }
        return new FluidStack(water, amount);
    }

    /**
     * 计算最大可处理的流体量（基于配置和rotor等级）。
     *
     * @param optimalFlow        最优流量（L/t）
     * @param overflowMultiplier 溢流倍数
     * @return 最大流量（L/t）
     */
    public static int calculateMaxFlow(int optimalFlow, int overflowMultiplier) {
        // 最大流量 = optimalFlow × (1 + 0.5 × overflowMultiplier)
        return (int) (optimalFlow * (1.0f + 0.5f * overflowMultiplier));
    }

    /**
     * 验证并提取流体。
     * 从源Hatch提取指定量的流体，放入目标FluidTank。
     *
     * @param sourceStack   源流体栈
     * @param requestAmount 请求的数量
     * @return 实际提取的数量
     */
    public static int extractFluid(FluidStack sourceStack, int requestAmount) {
        if (sourceStack == null || sourceStack.amount <= 0) {
            return 0;
        }
        if (!isValidTurbineFluid(sourceStack)) {
            return 0;
        }
        return Math.min(sourceStack.amount, requestAmount);
    }

    /**
     * 调试用：打印流体信息。
     */
    public static void debugPrint(FluidStack fluid) {
        if (fluid == null) {
            System.out.println("Fluid: null");
            return;
        }
        System.out.println(
            "Fluid: " + fluid.getFluid()
                .getName() + " x" + fluid.amount);
        System.out.println("  Is Steam: " + isSteam(fluid));
        System.out.println("  Is Superheated: " + isSuperheatedSteam(fluid));
        System.out.println("  Is High Pressure: " + isHighPressureSteam(fluid));
        System.out.println("  Efficiency Bonus: " + getEfficiencyBonus(fluid.getFluid()) + "x");
    }
}
