package cn.icewindy.steamturbine.util;

import java.util.HashMap;
import java.util.Map;

import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

/**
 * 轻量版 LHE 冷却剂表（对齐 GT5U 默认三种）。
 */
public final class HeatExchangerCoolantRegistry {

    private HeatExchangerCoolantRegistry() {}

    public static class CoolantInfo {

        public final String coldFluidName;
        public final String hotFluidName;
        public final float steamMultiplier;
        public final float superheatedThreshold;

        public CoolantInfo(String coldFluidName, String hotFluidName, float steamMultiplier,
            float superheatedThreshold) {
            this.coldFluidName = coldFluidName;
            this.hotFluidName = hotFluidName;
            this.steamMultiplier = steamMultiplier;
            this.superheatedThreshold = superheatedThreshold;
        }

        public Fluid getColdFluid() {
            return FluidRegistry.getFluid(coldFluidName);
        }

        public FluidStack getColdFluid(int amount) {
            Fluid fluid = getColdFluid();
            return fluid == null ? null : new FluidStack(fluid, amount);
        }
    }

    private static final Map<String, CoolantInfo> COOLANTS = new HashMap<String, CoolantInfo>();

    static {
        register("ic2pahoehoelava", "lava", 1.0f / 5.0f, 1.0f / 4.0f);
        register("ic2coolant", "ic2hotcoolant", 1.0f / 2.0f, 1.0f / 5.0f);
        register("molten.solarsaltcold", "molten.solarsalthot", 2.5f, 1.0f / 25.0f);
    }

    public static void register(String coldFluidName, String hotFluidName, float steamMultiplier,
        float superheatedThreshold) {
        COOLANTS.put(hotFluidName, new CoolantInfo(coldFluidName, hotFluidName, steamMultiplier, superheatedThreshold));
    }

    public static CoolantInfo get(Fluid fluid) {
        if (fluid == null) return null;
        return COOLANTS.get(fluid.getName());
    }
}
