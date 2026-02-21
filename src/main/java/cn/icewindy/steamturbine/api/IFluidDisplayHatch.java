package cn.icewindy.steamturbine.api;

import net.minecraftforge.fluids.FluidStack;

/**
 * 用于统一流体仓 GUI 显示。
 */
public interface IFluidDisplayHatch {

    FluidStack getFluid();

    int getTankCapacity();
}
