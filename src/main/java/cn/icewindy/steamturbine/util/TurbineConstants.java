package cn.icewindy.steamturbine.util;

/**
 * 涡轮机制常量集中定义。
 * 所有全局常数在此处集中管理，便于维护和调整。
 */
public class TurbineConstants {

    // ==================== 流体转换常数 ====================

    /** 蒸汽冷凝系数：1000 mB Steam = 1 mB Distilled Water */
    public static final int STEAM_PER_WATER = 1000;

    // ==================== 转子等级定义 ====================

    /**
     * 转子等级参数。
     * 按照 ItemTurbineRotor 的 metadata 定义。
     */
    public static class RotorTier {

        public static final int TIER_IRON = 0;
        public static final int TIER_STEEL = 1;
        public static final int TIER_TITANIUM = 2;

        // 每个等级的基础参数
        public static final float[] EFFICIENCY = { 1.00f, 1.15f, 1.30f };
        public static final int[] OPTIMAL_FLOW = { 600, 1200, 2400 }; // mB/t
        public static final int[] OVERFLOW_MULTIPLIER = { 1, 2, 3 };
        public static final int[] DURABILITY = { 6000, 12000, 24000 };

        /**
         * 获取指定等级的效率。
         */
        public static float getEfficiency(int tier) {
            return (tier >= 0 && tier < EFFICIENCY.length) ? EFFICIENCY[tier] : EFFICIENCY[0];
        }

        /**
         * 获取指定等级的最优流量。
         */
        public static int getOptimalFlow(int tier) {
            return (tier >= 0 && tier < OPTIMAL_FLOW.length) ? OPTIMAL_FLOW[tier] : OPTIMAL_FLOW[0];
        }

        /**
         * 获取指定等级的溢流倍数。
         */
        public static int getOverflowMultiplier(int tier) {
            return (tier >= 0 && tier < OVERFLOW_MULTIPLIER.length) ? OVERFLOW_MULTIPLIER[tier]
                : OVERFLOW_MULTIPLIER[0];
        }

        /**
         * 获取指定等级的耐久度。
         */
        public static int getDurability(int tier) {
            return (tier >= 0 && tier < DURABILITY.length) ? DURABILITY[tier] : DURABILITY[0];
        }
    }

    // ==================== 结构参数 ====================

    /** 多方块结构的深度（层数）：4层 */
    public static final int STRUCTURE_DEPTH = 4;

    /** 多方块结构的宽度：3×3 */
    public static final int STRUCTURE_WIDTH = 3;

    /** 多方块结构的高度：3×3 */
    public static final int STRUCTURE_HEIGHT = 3;

    /** 多方块结构的半径：±1 */
    public static final int STRUCTURE_RADIUS = 1;

    /** 最小允许的外壳方块数 */
    public static final int MIN_CASING_COUNT = 16;

    /** 最少需要的输入Hatch数 */
    public static final int MIN_INPUT_HATCH = 1;

    /** 最少需要的Dynamo Hatch数 */
    public static final int MIN_DYNAMO_HATCH = 1;

    // ==================== 能量参数 ====================

    /** 每个Dynamo Hatch的内部缓冲容量（EU） */
    public static final long DYNAMO_BUFFER_SIZE = 131072L; // 128K EU

    /** 每个Dynamo Hatch的最大输出电压（EU/t） */
    public static final double DYNAMO_MAX_VOLTAGE = 8192.0; // IV级

    /** 能量网络集成标志 */
    public static final boolean INTEGRATE_WITH_IC2 = true;

    // ==================== 流体参数 ====================

    /** 输入Hatch的缓冲容量（mB） */
    public static final int INPUT_HATCH_CAPACITY = 64000; // 64L

    /** 涡轮内部蒸汽缓冲容量（mB） */
    public static final int TURBINE_BUFFER_CAPACITY = 128000; // 128L

    // ==================== 运行参数 ====================

    /** 涡轮的最高转速（转/分钟，仅用于动画） */
    public static final int MAX_SPEED = 10000;

    /** 摩擦系数：每tick转速下降量 */
    public static final int FRICTION = 10;

    /** 加速度系数：每tick转速提升量 */
    public static final int ACCELERATION = 30;

    // ==================== 更新周期 ====================

    /** 结构检查的默认间隔（tick） */
    public static final int DEFAULT_CHECK_INTERVAL = 40;

    /** 能量更新的间隔（tick） */
    public static final int ENERGY_UPDATE_INTERVAL = 1;

    /** 流体更新的间隔（tick） */
    public static final int FLUID_UPDATE_INTERVAL = 1;

    // ==================== 损耗参数 ====================

    /** 涡轮启动所需的最少转速 */
    public static final int MIN_SPEED_FOR_OPERATION = 100;

    /** 转子每tick的磨损量（基础值，实际会根据流量调整） */
    public static final int BASE_ROTOR_WEAR = 1;

    /** 超过最优流量时的额外磨损倍数 */
    public static final float OVERFLOW_WEAR_MULTIPLIER = 1.5f;

    // ==================== 流体类型等级 ====================

    /** 普通蒸汽的效率加成 */
    public static final float STEAM_EFFICIENCY = 1.0f;

    /** 过热蒸汽的效率加成 */
    public static final float SUPERHEATED_STEAM_EFFICIENCY = 1.5f;

    /** 高温高压蒸汽的效率加成 */
    public static final float HIGH_PRESSURE_STEAM_EFFICIENCY = 2.0f;

    // ==================== 能量效率曲线 ====================

    /** 最优流量下的效率衰减系数（0.5 = 50%效率） */
    public static final float OPTIMAL_FLOW_EFFICIENCY_PENALTY = 0.5f;

    /** 最小保证效率（即使溢流严重也不会低于此值） */
    public static final float MINIMUM_EFFICIENCY = 0.1f;

    // ==================== 调试和日志 ====================

    /** 是否启用详细日志 */
    public static final boolean ENABLE_DEBUG_LOG = false;

    /** 是否在结构验证失败时打印详细信息 */
    public static final boolean DEBUG_STRUCTURE_VALIDATION = false;

    /** 是否在能量计算时打印详细信息 */
    public static final boolean DEBUG_ENERGY_CALCULATION = false;

    // ==================== 安全检查 ====================

    /** 最大允许的输出电压（安全上限） */
    public static final int ABSOLUTE_MAX_OUTPUT = 32768; // Avoid overflow

    /** 最大允许的流量（安全上限，防止整数溢出） */
    public static final int ABSOLUTE_MAX_FLOW = 100000; // 100k mB/t

    // ==================== 客户端渲染参数 ====================

    /** 转子旋转的基础速度（度/秒） */
    public static final float ROTOR_BASE_ROTATION_SPEED = 6.0f;

    /** 转子最大旋转速度倍数 */
    public static final float ROTOR_MAX_ROTATION_MULTIPLIER = 3.0f;

    // ==================== 工具方法 ====================

    /**
     * 获取指定Tier的转子的所有参数。
     *
     * @param tier 转子等级 (0=Iron, 1=Steel, 2=Titanium)
     * @return 包含所有参数的数组 [efficiency, optimalFlow, overflowMultiplier, durability]
     */
    public static float[] getRotorStats(int tier) {
        return new float[] { RotorTier.getEfficiency(tier), RotorTier.getOptimalFlow(tier),
            RotorTier.getOverflowMultiplier(tier), RotorTier.getDurability(tier) };
    }

    /**
     * 判断是否为有效的转子等级。
     */
    public static boolean isValidRotorTier(int tier) {
        return tier >= 0 && tier < RotorTier.EFFICIENCY.length;
    }

    /**
     * 计算溢流情况下允许的最大流量百分比。
     *
     * @param overflowMultiplier 溢流倍数
     * @return 最大流量百分比（相对于最优流量）
     */
    public static float getMaxFlowPercentage(int overflowMultiplier) {
        // 最大流量 = optimalFlow × (1 + 0.5 × overflowMultiplier)
        return 1.0f + 0.5f * overflowMultiplier;
    }
}
