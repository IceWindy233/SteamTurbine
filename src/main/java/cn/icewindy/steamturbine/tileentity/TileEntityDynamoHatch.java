package cn.icewindy.steamturbine.tileentity;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.ForgeDirection;

import ic2.api.energy.event.EnergyTileLoadEvent;
import ic2.api.energy.event.EnergyTileUnloadEvent;
import ic2.api.energy.tile.IEnergySource;

public class TileEntityDynamoHatch extends TileEntity implements IEnergySource {

    private boolean isFormed = false;
    private boolean addedToEnergyNet = false;
    private double storedEU = 0;
    private static final double MAX_OUTPUT_PER_TICK = 8192.0;
    private static final double INTERNAL_CAPACITY = 8192.0 * 16.0;

    // Tier determines voltage. 4 = EV (2048), 5 = IV (8192).
    // GT5 Dynamo Hatches usually define tier by block type (LV-UV).
    // For this mod, we'll assume a high tier (IV) to match the Large Turbine.
    private int tier = 5;

    @Override
    public void updateEntity() {
        super.updateEntity();
        if (!worldObj.isRemote && !addedToEnergyNet) {
            MinecraftForge.EVENT_BUS.post(new EnergyTileLoadEvent(this));
            addedToEnergyNet = true;
        }
    }

    @Override
    public void invalidate() {
        if (!worldObj.isRemote && addedToEnergyNet) {
            MinecraftForge.EVENT_BUS.post(new EnergyTileUnloadEvent(this));
            addedToEnergyNet = false;
        }
        super.invalidate();
    }

    @Override
    public void onChunkUnload() {
        if (!worldObj.isRemote && addedToEnergyNet) {
            MinecraftForge.EVENT_BUS.post(new EnergyTileUnloadEvent(this));
            addedToEnergyNet = false;
        }
        super.onChunkUnload();
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        storedEU = nbt.getDouble("storedEU");
        isFormed = nbt.getBoolean("isFormed");
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setDouble("storedEU", storedEU);
        nbt.setBoolean("isFormed", isFormed);
    }

    @Override
    public net.minecraft.network.Packet getDescriptionPacket() {
        NBTTagCompound tag = new NBTTagCompound();
        writeToNBT(tag);
        return new net.minecraft.network.play.server.S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 1, tag);
    }

    @Override
    public void onDataPacket(net.minecraft.network.NetworkManager net,
        net.minecraft.network.play.server.S35PacketUpdateTileEntity pkt) {
        readFromNBT(pkt.func_148857_g());
        worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
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

    // --- IEnergySource ---

    @Override
    public double getOfferedEnergy() {
        // Output max voltage per tick, or stored amount if lower
        return Math.min(storedEU, MAX_OUTPUT_PER_TICK);
    }

    @Override
    public void drawEnergy(double amount) {
        if (amount > 0) {
            storedEU -= amount;
            if (storedEU < 0) storedEU = 0;
            markDirty();
        }
    }

    @Override
    public int getSourceTier() {
        return tier;
    }

    @Override
    public boolean emitsEnergyTo(TileEntity receiver, ForgeDirection direction) {
        return true; // Emit to all sides
    }

    // --- Custom Interaction ---

    /**
     * Inject energy from controller.
     *
     * @param amount EU to inject
     * @return Amount actually accepted
     */
    public double injectEnergy(double amount) {
        double space = INTERNAL_CAPACITY - storedEU;
        double accepted = Math.min(amount, space);
        if (accepted > 0) {
            storedEU += accepted;
            markDirty();
        }
        return accepted;
    }

    public int getMaxOutputPerTick() {
        return (int) MAX_OUTPUT_PER_TICK;
    }

    public double getStoredEUBuffer() {
        return storedEU;
    }

    public double getEnergyCapacity() {
        return INTERNAL_CAPACITY;
    }
}
