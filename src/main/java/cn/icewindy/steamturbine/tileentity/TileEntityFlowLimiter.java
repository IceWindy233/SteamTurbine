package cn.icewindy.steamturbine.tileentity;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;

import cn.icewindy.steamturbine.ModConfig;
import cn.icewindy.steamturbine.api.IFluidDisplayHatch;

/**
 * 流体限速方块：将内部流体按不高于设定值的速率输出到朝向侧相邻方块。
 */
public class TileEntityFlowLimiter extends TileEntity implements IFluidHandler, IFluidDisplayHatch {

    private static final int DEFAULT_FLOW_RATE = 100;
    private static final int MIN_FLOW_RATE = 1;
    private static final int MAX_FLOW_RATE = 64000;

    private final FluidTank tank = new FluidTank(ModConfig.tankCapacity);
    private int maxFlowPerTick = DEFAULT_FLOW_RATE;
    private int facing = ForgeDirection.NORTH.ordinal();
    // 索引使用 ForgeDirection.ordinal()：0 D, 1 U, 2 N, 3 S, 4 W, 5 E
    private final boolean[] sideOutput = new boolean[6];

    @Override
    public void updateEntity() {
        if (worldObj == null || worldObj.isRemote) return;
        pushFluidToOutputSide();
    }

    private void pushFluidToOutputSide() {
        FluidStack stored = tank.getFluid();
        if (stored == null || stored.amount <= 0 || maxFlowPerTick <= 0) return;
        int remain = Math.min(stored.amount, maxFlowPerTick);
        if (remain <= 0) return;

        for (int i = 0; i < sideOutput.length; i++) {
            if (!sideOutput[i] || remain <= 0) continue;
            ForgeDirection out = ForgeDirection.getOrientation(i);
            if (out == ForgeDirection.UNKNOWN) continue;

            TileEntity neighbor = worldObj
                .getTileEntity(xCoord + out.offsetX, yCoord + out.offsetY, zCoord + out.offsetZ);
            if (!(neighbor instanceof IFluidHandler)) continue;

            IFluidHandler handler = (IFluidHandler) neighbor;
            FluidStack offer = new FluidStack(stored, remain);
            ForgeDirection intoSide = out.getOpposite();
            int accepted = handler.fill(intoSide, offer, false);
            if (accepted <= 0) continue;

            FluidStack drained = tank.drain(accepted, true);
            if (drained == null || drained.amount <= 0) continue;
            handler.fill(intoSide, drained, true);
            remain -= drained.amount;
        }

        if (remain < Math.min(stored.amount, maxFlowPerTick)) {
            markDirty();
            worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
        }
    }

    public void setFacing(int metaFacing) {
        this.facing = metaFacing;
        markDirty();
    }

    public ForgeDirection getOutputDirection() {
        return ForgeDirection.getOrientation(facing);
    }

    public boolean isSideOutput(int sideOrdinal) {
        return sideOrdinal >= 0 && sideOrdinal < sideOutput.length && sideOutput[sideOrdinal];
    }

    public void setSideOutput(int sideOrdinal, boolean output) {
        if (sideOrdinal < 0 || sideOrdinal >= sideOutput.length) return;
        sideOutput[sideOrdinal] = output;
        markDirty();
    }

    public void toggleSideOutput(int sideOrdinal) {
        setSideOutput(sideOrdinal, !isSideOutput(sideOrdinal));
    }

    public int getMaxFlowPerTick() {
        return maxFlowPerTick;
    }

    public void setMaxFlowPerTick(int value) {
        maxFlowPerTick = clamp(value, MIN_FLOW_RATE, MAX_FLOW_RATE);
        markDirty();
    }

    public void adjustFlowPerTick(int delta) {
        setMaxFlowPerTick(maxFlowPerTick + delta);
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    @Override
    public int fill(ForgeDirection from, FluidStack resource, boolean doFill) {
        if (from != ForgeDirection.UNKNOWN && isSideOutput(from.ordinal())) {
            return 0;
        }
        if (resource == null) return 0;
        int filled = tank.fill(resource, doFill);
        if (doFill && filled > 0) {
            markDirty();
            if (worldObj != null && !worldObj.isRemote) {
                worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
            }
        }
        return filled;
    }

    @Override
    public FluidStack drain(ForgeDirection from, FluidStack resource, boolean doDrain) {
        if (from != ForgeDirection.UNKNOWN && !isSideOutput(from.ordinal())) {
            return null;
        }
        if (resource == null || tank.getFluid() == null || !resource.isFluidEqual(tank.getFluid())) {
            return null;
        }
        int allowed = Math.min(resource.amount, maxFlowPerTick);
        FluidStack drained = tank.drain(allowed, doDrain);
        if (doDrain && drained != null && drained.amount > 0) {
            markDirty();
        }
        return drained;
    }

    @Override
    public FluidStack drain(ForgeDirection from, int maxDrain, boolean doDrain) {
        if (from != ForgeDirection.UNKNOWN && !isSideOutput(from.ordinal())) {
            return null;
        }
        FluidStack drained = tank.drain(Math.min(maxDrain, maxFlowPerTick), doDrain);
        if (doDrain && drained != null && drained.amount > 0) {
            markDirty();
        }
        return drained;
    }

    @Override
    public boolean canFill(ForgeDirection from, Fluid fluid) {
        return fluid != null && (from == ForgeDirection.UNKNOWN || !isSideOutput(from.ordinal()));
    }

    @Override
    public boolean canDrain(ForgeDirection from, Fluid fluid) {
        return from == ForgeDirection.UNKNOWN || isSideOutput(from.ordinal());
    }

    @Override
    public FluidTankInfo[] getTankInfo(ForgeDirection from) {
        return new FluidTankInfo[] { tank.getInfo() };
    }

    @Override
    public FluidStack getFluid() {
        return tank.getFluid();
    }

    @Override
    public int getTankCapacity() {
        return tank.getCapacity();
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        tank.readFromNBT(nbt);
        maxFlowPerTick = clamp(nbt.getInteger("MaxFlowPerTick"), MIN_FLOW_RATE, MAX_FLOW_RATE);
        if (maxFlowPerTick <= 0) maxFlowPerTick = DEFAULT_FLOW_RATE;
        facing = nbt.getInteger("Facing");
        for (int i = 0; i < sideOutput.length; i++) {
            sideOutput[i] = nbt.getBoolean("SideOut" + i);
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        tank.writeToNBT(nbt);
        nbt.setInteger("MaxFlowPerTick", maxFlowPerTick);
        nbt.setInteger("Facing", facing);
        for (int i = 0; i < sideOutput.length; i++) {
            nbt.setBoolean("SideOut" + i, sideOutput[i]);
        }
    }

    @Override
    public Packet getDescriptionPacket() {
        NBTTagCompound nbt = new NBTTagCompound();
        writeToNBT(nbt);
        return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 0, nbt);
    }

    @Override
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) {
        readFromNBT(pkt.func_148857_g());
    }
}
