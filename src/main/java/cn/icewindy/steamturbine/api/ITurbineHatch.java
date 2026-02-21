package cn.icewindy.steamturbine.api;

import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

/**
 * Hatch（接口）的标准接口定义。
 * 所有Hatch类型都应继承此接口以确保一致性。
 */
public interface ITurbineHatch {

    /**
     * 检查此Hatch是否仍然有效。
     * 当TileEntity被破坏或卸载时应返回false。
     *
     * @return 如果Hatch有效则返回true
     */
    boolean isValid();

    /**
     * 检查此Hatch是否已失效。
     * 这是 isValid() 的反义。
     *
     * @return 如果Hatch已失效则返回true
     */
    boolean isInvalid();

    /**
     * 获取此Hatch的TileEntity。
     */
    TileEntity getTileEntity();

    /**
     * 获取此Hatch的朝向。
     */
    ForgeDirection getFacing();

    /**
     * 获取此Hatch所在的多方块结构中的序号。
     * 用于调试和可视化。
     */
    int getHatchIndex();

    /**
     * 设置此Hatch的序号。
     */
    void setHatchIndex(int index);
}
