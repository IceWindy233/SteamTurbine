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

public class TileEntityFluidOutputHatch extends TileEntity implements IFluidHandler, IFluidDisplayHatch {

    private final FluidTank tank = new FluidTank(ModConfig.tankCapacity);
    private boolean isFormed = false;

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        tank.readFromNBT(nbt);
        isFormed = nbt.getBoolean("isFormed");
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        tank.writeToNBT(nbt);
        nbt.setBoolean("isFormed", isFormed);
    }

    @Override
    public int fill(ForgeDirection from, FluidStack resource, boolean doFill) {
        if (from != ForgeDirection.UNKNOWN || resource == null) {
            return 0;
        }
        int filled = tank.fill(resource, doFill);
        onChanged(doFill, filled);
        return filled;
    }

    @Override
    public FluidStack drain(ForgeDirection from, FluidStack resource, boolean doDrain) {
        if (resource == null || tank.getFluid() == null || !resource.isFluidEqual(tank.getFluid())) {
            return null;
        }
        FluidStack drained = tank.drain(resource.amount, doDrain);
        onChanged(doDrain, drained == null ? 0 : drained.amount);
        return drained;
    }

    @Override
    public FluidStack drain(ForgeDirection from, int maxDrain, boolean doDrain) {
        FluidStack drained = tank.drain(maxDrain, doDrain);
        onChanged(doDrain, drained == null ? 0 : drained.amount);
        return drained;
    }

    @Override
    public boolean canFill(ForgeDirection from, Fluid fluid) {
        return from == ForgeDirection.UNKNOWN && fluid != null;
    }

    @Override
    public boolean canDrain(ForgeDirection from, Fluid fluid) {
        return true;
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
    public Packet getDescriptionPacket() {
        NBTTagCompound nbt = new NBTTagCompound();
        writeToNBT(nbt);
        return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 0, nbt);
    }

    @Override
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) {
        readFromNBT(pkt.func_148857_g());
        if (worldObj != null) {
            worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
        }
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

    private void onChanged(boolean changed, int amount) {
        if (!changed || amount <= 0) return;
        markDirty();
        if (worldObj != null && !worldObj.isRemote) {
            worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
        }
    }
}
