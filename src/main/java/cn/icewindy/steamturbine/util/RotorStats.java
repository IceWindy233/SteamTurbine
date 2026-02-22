package cn.icewindy.steamturbine.util;

import cn.icewindy.steamturbine.ModConfig;

/**
 * 转子属性数据类。
 * 根据转子材质（metadata）提供效率、最优流量和溢流等级。
 */
public class RotorStats {

    /** 基础效率 (0.0~1.0) */
    public final float efficiency;

    /** 最优蒸汽流量 (mB/t) */
    public final int optimalFlow;

    /** 溢流等级，决定允许超过最优流量的倍数 */
    public final float overflowMultiplier;

    public RotorStats(float efficiency, int optimalFlow, float overflowMultiplier) {
        this.efficiency = efficiency;
        this.optimalFlow = optimalFlow;
        this.overflowMultiplier = overflowMultiplier;
    }

    /**
     * 根据转子 metadata 获取对应的属性。
     *
     * | 材质 | Meta | 效率 | 最优流量(mB/t) | 溢流等级 |
     * |------|------|-------|-------------|---------|
     * | 铁 | 0 | 0.80 | 400 | 1 |
     * | 钢 | 1 | 0.90 | 800 | 2 |
     * | 钛 | 2 | 1.00 | 1600 | 3 |
     */
    public static RotorStats fromMeta(int meta) {
        return new RotorStats(
            ModConfig.getRotorEfficiency(meta),
            ModConfig.getRotorOptimalFlow(meta),
            ModConfig.getRotorOverflow(meta));
    }

    /**
     * 从转子 ItemStack 获取属性。
     */
    public static RotorStats fromRotor(net.minecraft.item.ItemStack rotor) {
        if (rotor == null) return null;
        return fromMeta(rotor.getItemDamage());
    }
}
