package cn.icewindy.steamturbine.util;

/**
 * 涡轮能量计算引擎。
 * 处理蒸汽流量、效率、溢流倍数到EU/t的转换。
 *
 * 转换公式：
 * baseEU = actualFlow(mB/t)
 *
 * 当流量等于最优值时：
 * EU/t = actualFlow × efficiency × 0.5
 *
 * 当流量不等于最优值时（溢流处理）：
 * overflowRatio = |actualFlow - optimalFlow| / (optimalFlow × (overflowMultiplier + 1))
 * actualEfficiency = (1.0 - overflowRatio)
 * EU/t = actualFlow × actualEfficiency × efficiency × 0.5
 */
public class TurbineEnergyCalculator {

    /** 蒸汽冷凝水的转换比率：1000mB Steam = 1mB Water */
    public static final int STEAM_PER_WATER = 1000;

    /**
     * 计算涡轮的基础EU产出。
     *
     * @param actualFlow         实际蒸汽流量 (mB/t)
     * @param efficiency         转子效率 (0.0 ~ 1.0)
     * @param optimalFlow        最优流量 (mB/t)
     * @param overflowMultiplier 溢流等级 (1 ~ 3)
     * @return 产出的 EU/t 能量
     */
    public static int calculateOutput(int actualFlow, float efficiency, int optimalFlow, int overflowMultiplier) {
        if (actualFlow <= 0 || efficiency <= 0 || optimalFlow <= 0) {
            return 0;
        }

        // 限制流量在合理范围内（最多250%的最优流量）
        int maxFlow = (int) (optimalFlow * (0.5f * overflowMultiplier + 1));
        actualFlow = Math.min(actualFlow, maxFlow);

        int baseEU = actualFlow;
        float overflowEfficiency;

        // 精确比较
        if (actualFlow == optimalFlow) {
            // 在最优流量下使用基础效率
            overflowEfficiency = 1.0f;
        } else {
            // 溢流情况下计算效率衰减
            overflowEfficiency = getOverflowEfficiency(actualFlow, optimalFlow, overflowMultiplier);
        }

        // 对齐 GT5U 的蒸汽涡轮公式：baseEU * overflowEff * rotorEff * 0.5
        float rawOutput = baseEU * overflowEfficiency * efficiency * 0.5f;
        int output = (int) rawOutput;
        return Math.max(1, output);
    }

    /**
     * 计算溢流状态下的效率因子。
     *
     * @param actualFlow         实际流量
     * @param optimalFlow        最优流量
     * @param overflowMultiplier 溢流倍数
     * @return 效率因子 (0.1 ~ 1.0)
     */
    public static float getOverflowEfficiency(int actualFlow, int optimalFlow, int overflowMultiplier) {
        if (optimalFlow <= 0) {
            return 0.0f;
        }
        if (actualFlow > optimalFlow) {
            // 超过最优值时：效率下降
            int excess = actualFlow - optimalFlow;
            float ratio = (float) excess / (optimalFlow * (overflowMultiplier + 1));
            return 1.0f - ratio;
        } else {
            // 低于最优值时：效率下降
            int deficit = optimalFlow - actualFlow;
            float ratio = (float) deficit / optimalFlow;
            return 1.0f - ratio;
        }
    }

    /**
     * 计算蒸汽冷凝后的蒸馏水产出量。
     * 采用余数保留策略，避免浮点精度问题。
     *
     * @param steamL      处理的蒸汽量 (mB/t)
     * @param excessWater 上一tick的余数 (mB)
     * @return [产出的蒸馏水 (mB), 新的余数 (mB)]
     */
    public static int[] condenseSteam(int steamL, int excessWater) {
        int totalWater = excessWater + steamL; // 所有蒸馏水来源
        int output = totalWater / STEAM_PER_WATER;
        int remainder = totalWater % STEAM_PER_WATER;
        return new int[] { output, remainder };
    }

    /**
     * 获取不同流体类型的效率加成。
     *
     * @param fluidType 流体类型枚举
     * @return 效率倍数 (1.0 = 普通蒸汽, 1.5 = 过热蒸汽, 2.0 = 高温高压蒸汽)
     */
    public static float getFluidTypeBonus(FluidType fluidType) {
        switch (fluidType) {
            case SUPERHEATED_STEAM:
                return 1.5f;
            case HIGH_PRESSURE_STEAM:
                return 2.0f;
            case STEAM:
            default:
                return 1.0f;
        }
    }

    /**
     * 流体类型枚举。
     */
    public enum FluidType {

        STEAM(1.0f, "steam"),
        SUPERHEATED_STEAM(1.5f, "superheatedsteam"),
        HIGH_PRESSURE_STEAM(2.0f, "highpressuresteam");

        public final float bonus;
        public final String fluidName;

        FluidType(float bonus, String fluidName) {
            this.bonus = bonus;
            this.fluidName = fluidName;
        }

        /**
         * 从流体名称获取对应的类型。
         */
        public static FluidType fromName(String name) {
            if (name == null) return STEAM;
            String lower = name.toLowerCase();
            for (FluidType type : values()) {
                if (lower.contains(type.fluidName)) {
                    return type;
                }
            }
            return STEAM;
        }
    }

    /**
     * 计算最大允许流量（考虑溢流倍数）。
     *
     * @param optimalFlow        最优流量
     * @param overflowMultiplier 溢流倍数
     * @return 最大流量
     */
    public static int getMaximumFlow(int optimalFlow, int overflowMultiplier) {
        return (int) (optimalFlow * (0.5f * overflowMultiplier + 1));
    }

    /**
     * 计算溢流倍数对应的最大允许额外流量百分比。
     *
     * @param overflowMultiplier 溢流倍数 (1~3)
     * @return 最大额外流量百分比 (150%/200%/250%)
     */
    public static float getMaxFlowPercentage(int overflowMultiplier) {
        return 0.5f * overflowMultiplier + 1.0f;
    }
}
