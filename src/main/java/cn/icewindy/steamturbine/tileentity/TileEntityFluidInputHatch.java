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

public class TileEntityFluidInputHatch extends TileEntity implements IFluidHandler, IFluidDisplayHatch {

    private static final int DEFAULT_FLOW_RATE = 10;
    private static final int MIN_FLOW_RATE = 0;
    private static final int MAX_FLOW_RATE = 64000;

    private final FluidTank tank = new FluidTank(ModConfig.tankCapacity);
    private int maxFlowPerTick = DEFAULT_FLOW_RATE;
    private boolean isFormed = false;

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        tank.readFromNBT(nbt);
        maxFlowPerTick = clamp(nbt.getInteger("MaxFlowPerTick"), MIN_FLOW_RATE, MAX_FLOW_RATE);
        isFormed = nbt.getBoolean("isFormed");
        if (maxFlowPerTick < 0) {
            maxFlowPerTick = DEFAULT_FLOW_RATE;
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        tank.writeToNBT(nbt);
        nbt.setInteger("MaxFlowPerTick", maxFlowPerTick);
        nbt.setBoolean("isFormed", isFormed);
    }

    @Override
    public Packet getDescriptionPacket() {
        NBTTagCompound nbt = new NBTTagCompound();
        writeToNBT(nbt);
        return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 1, nbt);
    }

    @Override
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) {
        readFromNBT(pkt.func_148857_g());
        if (worldObj != null) {
            worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
        }
    }

    @Override
    public int fill(ForgeDirection from, FluidStack resource, boolean doFill) {
        int filled = tank.fill(resource, doFill);
        onChanged(doFill, filled);
        return filled;
    }

    @Override
    public FluidStack drain(ForgeDirection from, FluidStack resource, boolean doDrain) {
        if (from != ForgeDirection.UNKNOWN) {
            return null;
        }
        if (resource == null || tank.getFluid() == null || !resource.isFluidEqual(tank.getFluid())) {
            return null;
        }
        int allowed = Math.min(resource.amount, maxFlowPerTick);
        FluidStack drained = tank.drain(allowed, doDrain);
        onChanged(doDrain, drained == null ? 0 : drained.amount);
        return drained;
    }

    @Override
    public FluidStack drain(ForgeDirection from, int maxDrain, boolean doDrain) {
        if (from != ForgeDirection.UNKNOWN) {
            return null;
        }
        FluidStack drained = tank.drain(Math.min(maxDrain, maxFlowPerTick), doDrain);
        onChanged(doDrain, drained == null ? 0 : drained.amount);
        return drained;
    }

    @Override
    public boolean canFill(ForgeDirection from, Fluid fluid) {
        return fluid != null;
    }

    @Override
    public boolean canDrain(ForgeDirection from, Fluid fluid) {
        return from == ForgeDirection.UNKNOWN;
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

    private void onChanged(boolean changed, int amount) {
        if (!changed || amount <= 0) return;
        markDirty();
        if (worldObj != null && !worldObj.isRemote) {
            worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
        }
    }

    public int getMaxFlowPerTick() {
        return maxFlowPerTick;
    }

    public void setMaxFlowPerTick(int value) {
        maxFlowPerTick = clamp(value, MIN_FLOW_RATE, MAX_FLOW_RATE);
        markDirty();
        if (worldObj != null && !worldObj.isRemote) {
            worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
        }
    }

    public void adjustFlowPerTick(int delta) {
        setMaxFlowPerTick(maxFlowPerTick + delta);
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    public void setFormed(boolean formed) {
        if (this.isFormed != formed) {
            this.isFormed = formed;
            markDirty();
            if (worldObj != null && !worldObj.isRemote) {
                worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
            }
        }
    }

    public boolean isFormed() {
        return isFormed;
    }
}
