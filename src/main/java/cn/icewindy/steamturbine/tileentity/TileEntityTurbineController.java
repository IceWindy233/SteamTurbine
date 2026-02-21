package cn.icewindy.steamturbine.tileentity;

import java.util.ArrayList;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;

import cn.icewindy.steamturbine.ModConfig;
import cn.icewindy.steamturbine.item.ItemTurbineRotor;
import cn.icewindy.steamturbine.registry.ModBlocks;
import cn.icewindy.steamturbine.util.MultiblockValidator;
import cn.icewindy.steamturbine.util.RotorStats;
import cn.icewindy.steamturbine.util.TurbineConstants;
import cn.icewindy.steamturbine.util.TurbineEnergyCalculator;
import cn.icewindy.steamturbine.util.TurbineFluidHandler;
import ic2.api.energy.event.EnergyTileLoadEvent;
import ic2.api.energy.event.EnergyTileUnloadEvent;
import ic2.api.energy.tile.IEnergySource;

/**
 * GT5 风格大型蒸汽涡轮控制器（IC2 适配版）。
 */
public class TileEntityTurbineController extends TileEntity implements IEnergySource, IFluidHandler, IInventory {

    // --- 常量定义（使用集中式常量管理）---
    private static final int MAX_SPEED = TurbineConstants.MAX_SPEED;
    private static final int FRICTION = TurbineConstants.FRICTION;
    private static final int ACCELERATION = TurbineConstants.ACCELERATION;

    // --- 内部状态 ---
    public FluidTank inputTank;
    public FluidTank outputTank;
    private double storedEU = 0;
    private boolean isFormed = false;
    private ItemStack rotorSlot = null; // 现在通过 IInventory 接口管理
    private int facing = 2;

    // --- Hatch Lists ---
    private ArrayList<TileEntityInputHatch> inputHatches = new ArrayList<>();
    private ArrayList<TileEntityOutputHatch> outputHatches = new ArrayList<>();
    private ArrayList<TileEntityDynamoHatch> dynamoHatches = new ArrayList<>();
    private ArrayList<int[]> redstoneControlBlocks = new ArrayList<>();

    // --- 运行数据 ---
    public int currentSpeed = 0; // public for Container access
    private int checkTimer = 0;
    private int lastEUOutput = 0;
    private int lastSteamConsumed = 0;
    private float currentEfficiency = 0.0f;
    private boolean addedToEnergyNet = false;
    private int waterCondensationRemainder = 0; // 蒸汽冷凝余数
    private int guiSyncTimer = 0;
    private long recipesDone = 0;
    private boolean redstonePowered = false;

    public TileEntityTurbineController() {
        inputTank = new FluidTank(ModConfig.tankCapacity * 2);
        outputTank = new FluidTank(ModConfig.tankCapacity * 2);
    }

    // ==================== 多方块 Hatch 管理 ====================

    public void clearHatches() {
        inputHatches.clear();
        outputHatches.clear();
        dynamoHatches.clear();
        redstoneControlBlocks.clear();
    }

    public void addInputHatch(TileEntityInputHatch hatch) {
        inputHatches.add(hatch);
    }

    public void addDynamoHatch(TileEntityDynamoHatch hatch) {
        dynamoHatches.add(hatch);
    }

    public void addOutputHatch(TileEntityOutputHatch hatch) {
        outputHatches.add(hatch);
    }

    public void addRedstoneControlBlock(int x, int y, int z) {
        redstoneControlBlocks.add(new int[] { x, y, z });
    }

    // ==================== 核心逻辑 ====================

    @Override
    public void updateEntity() {
        if (worldObj.isRemote) return;

        redstonePowered = isRedstoneShutdownActive();

        // 1. 结构检测
        checkTimer++;
        if (checkTimer >= ModConfig.checkInterval) {
            checkTimer = 0;
            recheckStructure();
        }

        if (!isFormed || rotorSlot == null || !(rotorSlot.getItem() instanceof ItemTurbineRotor)) {
            stopTurbine();
            return;
        }

        if (ItemTurbineRotor.getRemainingDurability(rotorSlot) <= 0) {
            stopTurbine();
            return;
        }

        if (!redstonePowered) {
            // 2. 从 Hatch 获取流体
            for (TileEntityInputHatch hatch : inputHatches) {
                if (hatch.isInvalid()) continue;
                FluidStack fs = hatch.getFluid();
                if (fs != null && fs.amount > 0 && TurbineFluidHandler.isValidTurbineFluid(fs)) {
                    int filled = inputTank.fill(fs, true);
                    hatch.drain(ForgeDirection.UNKNOWN, filled, true);
                    if (inputTank.getFluidAmount() >= inputTank.getCapacity()) break;
                }
            }

            // 3. 核心处理
            processOperation();
        } else {
            stopTurbine();
        }

        // 4. 输出能量到 Dynamo Hatch
        if (storedEU > 0 && !dynamoHatches.isEmpty()) {
            double euPerHatch = storedEU / dynamoHatches.size();
            for (TileEntityDynamoHatch hatch : dynamoHatches) {
                if (hatch.isInvalid()) continue;
                double accepted = hatch.injectEnergy(euPerHatch);
                storedEU -= accepted;
                if (storedEU <= 0.1) {
                    storedEU = 0;
                    break;
                }
            }
        }

        // 5. 输出蒸馏水到 Output Hatch
        if (outputTank.getFluidAmount() > 0 && !outputHatches.isEmpty()) {
            for (TileEntityOutputHatch hatch : outputHatches) {
                if (hatch == null || hatch.isInvalid()) {
                    continue;
                }
                FluidStack available = outputTank.getFluid();
                if (available == null || available.amount <= 0) {
                    break;
                }
                FluidStack toSend = new FluidStack(available, available.amount);
                int filled = hatch.fill(ForgeDirection.UNKNOWN, toSend, true);
                if (filled > 0) {
                    outputTank.drain(filled, true);
                }
            }
        }

        guiSyncTimer++;
        if (guiSyncTimer >= 10) {
            guiSyncTimer = 0;
            worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
        }

        markDirty();
    }

    private void stopTurbine() {
        if (currentSpeed > 0) {
            currentSpeed = Math.max(0, currentSpeed - FRICTION * 2);
        }
        lastEUOutput = 0;
        lastSteamConsumed = 0;
        currentEfficiency = 0;
    }

    private void processOperation() {
        RotorStats stats = RotorStats.fromRotor(rotorSlot);
        if (stats == null) return;

        FluidStack inputFluid = inputTank.getFluid();
        int availableSteam = inputFluid != null ? inputFluid.amount : 0;
        int maxAllowedFlow = TurbineEnergyCalculator.getMaximumFlow(stats.optimalFlow, stats.overflowMultiplier);

        // 检查流体是否有效
        if (!TurbineFluidHandler.isValidTurbineFluid(inputFluid)) {
            stopTurbine();
            return;
        }

        // 对齐 GT5U 大型蒸汽涡轮：过热/高压蒸汽会被排空但不发电
        if (TurbineFluidHandler.isSuperheatedSteam(inputFluid) || TurbineFluidHandler.isHighPressureSteam(inputFluid)) {
            inputTank.drain(Math.min(availableSteam, maxAllowedFlow), true);
            stopTurbine();
            return;
        }

        int actualConsumed = 0;
        int optimalFlow = stats.optimalFlow;
        boolean hasFuel = availableSteam > 0;

        if (hasFuel) {
            actualConsumed = Math.min(availableSteam, maxAllowedFlow);
            inputTank.drain(actualConsumed, true);
        }

        // GT5U 对齐：转速只作为显示，不作为发电门槛
        if (actualConsumed > 0) {
            float flowRatio = (float) actualConsumed / Math.max(1, optimalFlow);
            int targetSpeed = (int) (MAX_SPEED * Math.min(1.0f, flowRatio));
            if (currentSpeed < targetSpeed) {
                currentSpeed = Math.min(targetSpeed, currentSpeed + ACCELERATION);
            } else {
                currentSpeed = Math.max(targetSpeed, currentSpeed - FRICTION);
            }
        } else {
            currentSpeed = Math.max(0, currentSpeed - FRICTION);
        }

        // 计算能量产出
        int euProduced = 0;
        if (actualConsumed > 0) {
            // 对齐 GT5U 的蒸汽涡轮流量-效率公式
            euProduced = TurbineEnergyCalculator
                .calculateOutput(actualConsumed, stats.efficiency, optimalFlow, stats.overflowMultiplier);

            // 限制在当前 Dynamo Hatch 可承受输出内
            euProduced = Math.min(euProduced, getMaximumOutputPerTick());
            euProduced = Math.min(euProduced, TurbineConstants.ABSOLUTE_MAX_OUTPUT);

            currentEfficiency = (float) euProduced / Math.max(1, actualConsumed);
        } else {
            currentEfficiency = 0.0f;
        }

        // GT5U 对齐：只要消耗了蒸汽就会产冷凝水，与是否发电无关
        if (actualConsumed > 0) {
            int[] waterResult = TurbineFluidHandler.condenseToWater(actualConsumed, waterCondensationRemainder);
            waterCondensationRemainder = waterResult[1];

            if (waterResult[0] > 0) {
                FluidStack distilledWater = TurbineFluidHandler.createDistilledWater(waterResult[0]);
                if (distilledWater != null) {
                    outputTank.fill(distilledWater, true);
                }
            }
        }

        if (euProduced > 0) {
            storedEU += euProduced;
            storedEU = Math.min(storedEU, getControllerEnergyBuffer());
            recipesDone++;
        }

        // 处理转子磨损
        if (actualConsumed > 0) {
            int baseDamage = 1 + (actualConsumed / 1000);
            if (actualConsumed > optimalFlow) baseDamage += 1;

            boolean broken = ItemTurbineRotor.applyDamage(rotorSlot, baseDamage);
            if (broken) {
                rotorSlot = null;
                currentSpeed = 0;
                worldObj.createExplosion(null, xCoord, yCoord, zCoord, 2.0f, true);
            }
        }

        lastEUOutput = euProduced;
        lastSteamConsumed = actualConsumed;

        if (TurbineConstants.DEBUG_ENERGY_CALCULATION && actualConsumed > 0) {
            System.out.println(
                String.format(
                    "[Turbine] Flow: %d, EU: %d, Eff: %.2f, Speed: %d",
                    actualConsumed,
                    euProduced,
                    currentEfficiency,
                    currentSpeed));
        }
    }

    // ==================== IInventory Implementation ====================

    @Override
    public int getSizeInventory() {
        return 1;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return slot == 0 ? rotorSlot : null;
    }

    @Override
    public ItemStack decrStackSize(int slot, int amount) {
        if (this.rotorSlot != null) {
            ItemStack itemstack;
            if (this.rotorSlot.stackSize <= amount) {
                itemstack = this.rotorSlot;
                this.rotorSlot = null;
                this.markDirty();
                return itemstack;
            } else {
                itemstack = this.rotorSlot.splitStack(amount);
                if (this.rotorSlot.stackSize == 0) this.rotorSlot = null;
                this.markDirty();
                return itemstack;
            }
        }
        return null;
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int slot) {
        if (this.rotorSlot != null) {
            ItemStack itemstack = this.rotorSlot;
            this.rotorSlot = null;
            return itemstack;
        }
        return null;
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack stack) {
        this.rotorSlot = stack;
        if (stack != null && stack.stackSize > getInventoryStackLimit()) {
            stack.stackSize = getInventoryStackLimit();
        }
        this.markDirty();
    }

    @Override
    public String getInventoryName() {
        return "container.turbine";
    }

    @Override
    public boolean hasCustomInventoryName() {
        return false;
    }

    @Override
    public int getInventoryStackLimit() {
        return 1;
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer player) {
        return this.worldObj.getTileEntity(this.xCoord, this.yCoord, this.zCoord) == this && player
            .getDistanceSq((double) this.xCoord + 0.5D, (double) this.yCoord + 0.5D, (double) this.zCoord + 0.5D)
            <= 64.0D;
    }

    @Override
    public void openInventory() {}

    @Override
    public void closeInventory() {}

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return stack.getItem() instanceof ItemTurbineRotor;
    }

    // ==================== IEnergySource Implementation (Fallback) ====================
    // 保留直接输出功能作为备用，但优先使用 Dynamo Hatch

    @Override
    public void validate() {
        super.validate();
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
    public double getOfferedEnergy() {
        return Math.min(storedEU, ModConfig.maxOutputVoltage * 4);
    }

    @Override
    public void drawEnergy(double amount) {
        storedEU -= amount;
        if (storedEU < 0) storedEU = 0;
    }

    @Override
    public int getSourceTier() {
        return 5;
    }

    @Override
    public boolean emitsEnergyTo(TileEntity receiver, ForgeDirection direction) {
        return direction == getOutputDirection();
    }

    // ==================== IFluidHandler Implementation (Fallback) ====================

    @Override
    public int fill(ForgeDirection from, FluidStack resource, boolean doFill) {
        if (resource == null || from == getOutputDirection()) return 0;
        if (!TurbineFluidHandler.isValidTurbineFluid(resource)) return 0;
        return inputTank.fill(resource, doFill);
    }

    @Override
    public FluidStack drain(ForgeDirection from, FluidStack resource, boolean doDrain) {
        if (resource == null || !resource.isFluidEqual(outputTank.getFluid())) return null;
        return outputTank.drain(resource.amount, doDrain);
    }

    @Override
    public FluidStack drain(ForgeDirection from, int maxDrain, boolean doDrain) {
        return outputTank.drain(maxDrain, doDrain);
    }

    @Override
    public boolean canFill(ForgeDirection from, Fluid fluid) {
        return from != getOutputDirection() && TurbineFluidHandler.isValidTurbineFluid(new FluidStack(fluid, 1));
    }

    @Override
    public boolean canDrain(ForgeDirection from, Fluid fluid) {
        return from == getOutputDirection();
    }

    @Override
    public FluidTankInfo[] getTankInfo(ForgeDirection from) {
        return new FluidTankInfo[] { inputTank.getInfo(), outputTank.getInfo() };
    }

    // ==================== 辅助方法 ====================

    // 已弃用的方法保留用于兼容性，建议使用 TurbineFluidHandler 替代
    @Deprecated
    private boolean isSteamOrSuperheated(Fluid fluid) {
        return TurbineFluidHandler.isValidTurbineFluid(new FluidStack(fluid, 1));
    }

    @Deprecated
    private boolean isNormalSteam(Fluid fluid) {
        return TurbineFluidHandler.isSteam(fluid);
    }

    @Deprecated
    private boolean isSuperheatedSteam(Fluid fluid) {
        return TurbineFluidHandler.isSuperheatedSteam(fluid);
    }

    public void recheckStructure() {
        ForgeDirection dir = ForgeDirection.getOrientation(facing);
        // 使用改进的验证器替代旧逻辑
        MultiblockValidator.ValidationResult result = MultiblockValidator
            .validate(worldObj, xCoord, yCoord, zCoord, dir, this);
        boolean newFormed = result.isValid;

        if (isFormed != newFormed) {
            isFormed = newFormed;
            if (!isFormed) {
                // 结构检查失败时输出详细信息
                if (TurbineConstants.DEBUG_STRUCTURE_VALIDATION) {
                    System.out.println("[Turbine] Structure validation failed: " + result.errorMessage);
                }
            }
            markDirty();
            worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
        }
    }

    private ForgeDirection getOutputDirection() {
        return ForgeDirection.getOrientation(facing)
            .getOpposite();
    }

    private int getMaximumOutputPerTick() {
        if (dynamoHatches.isEmpty()) {
            return 0;
        }
        long total = 0;
        for (TileEntityDynamoHatch hatch : dynamoHatches) {
            if (hatch != null && !hatch.isInvalid()) {
                total += hatch.getMaxOutputPerTick();
            }
        }
        return (int) Math.min(Integer.MAX_VALUE, total);
    }

    private double getControllerEnergyBuffer() {
        int hatchCount = Math.max(1, dynamoHatches.size());
        return TurbineConstants.DYNAMO_MAX_VOLTAGE * 20.0 * hatchCount;
    }

    public int getCurrentSpeed() {
        return currentSpeed;
    }

    public void setCurrentSpeed(int speed) {
        this.currentSpeed = speed;
    }

    public double getStoredEU() {
        return storedEU;
    }

    public void setStoredEU(double eu) {
        this.storedEU = eu;
    }

    public float getCurrentEfficiency() {
        return currentEfficiency;
    }

    public int getLastEUOutput() {
        return lastEUOutput;
    }

    public int getLastSteamConsumed() {
        return lastSteamConsumed;
    }

    public boolean isFormed() {
        return isFormed;
    }

    public int getGuiMaxEnergy() {
        return (int) getControllerEnergyBuffer();
    }

    public boolean isRedstonePowered() {
        return redstonePowered;
    }

    private boolean isRedstoneShutdownActive() {
        if (redstoneControlBlocks.isEmpty()) {
            return false;
        }
        for (int[] pos : redstoneControlBlocks) {
            if (pos == null || pos.length < 3) {
                continue;
            }
            int px = pos[0];
            int py = pos[1];
            int pz = pos[2];
            if (worldObj.getBlock(px, py, pz) == ModBlocks.redstoneControl
                && worldObj.isBlockIndirectlyGettingPowered(px, py, pz)) {
                return true;
            }
        }
        return false;
    }

    public long getRecipesDone() {
        return recipesDone;
    }

    public long getTotalEnergyStored() {
        long total = 0;
        for (TileEntityDynamoHatch hatch : dynamoHatches) {
            if (hatch != null && !hatch.isInvalid()) {
                total += (long) hatch.getStoredEUBuffer();
            }
        }
        return total;
    }

    public long getTotalEnergyCapacity() {
        long total = 0;
        for (TileEntityDynamoHatch hatch : dynamoHatches) {
            if (hatch != null && !hatch.isInvalid()) {
                total += (long) hatch.getEnergyCapacity();
            }
        }
        return total;
    }

    public int getRotorDamagePercent() {
        if (rotorSlot == null || !(rotorSlot.getItem() instanceof ItemTurbineRotor)) {
            return 0;
        }
        int max = ItemTurbineRotor.getMaxDurability(rotorSlot.getItemDamage());
        if (max <= 0) {
            return 0;
        }
        int remain = ItemTurbineRotor.getRemainingDurability(rotorSlot);
        int used = Math.max(0, max - remain);
        return (int) ((used * 100.0f) / max);
    }

    public String[] getInfoData() {
        String running;
        if (redstonePowered) {
            running = StatCollector.translateToLocal("steamturbine.info.running.redstone_off");
        } else {
            running = lastEUOutput > 0 ? StatCollector.translateToLocal("steamturbine.info.running.true")
                : StatCollector.translateToLocal("steamturbine.info.running.false");
        }
        String fitting = StatCollector.translateToLocal("steamturbine.info.fitting.tight");
        long displayStored = (long) storedEU;
        int displayCapacity = Math.max(1, getGuiMaxEnergy());

        return new String[] { running + ": " + lastEUOutput + " EU/t",
            StatCollector.translateToLocal("steamturbine.info.efficiency") + ": "
                + String.format("%.2f", currentEfficiency * 100.0f)
                + "%",
            StatCollector.translateToLocal(
                "steamturbine.info.energy") + ": " + displayStored + " EU / " + displayCapacity + " EU",
            StatCollector.translateToLocal(
                "steamturbine.info.flow") + ": " + lastSteamConsumed + " L/t (" + fitting + ")",
            StatCollector.translateToLocal("steamturbine.info.fuel") + ": " + inputTank.getFluidAmount() + " L",
            StatCollector.translateToLocal("steamturbine.info.damage") + ": " + getRotorDamagePercent() + "%" };
    }

    public void setFacing(int facing) {
        this.facing = facing;
        markDirty();
    }

    public int getFacing() {
        return facing;
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setDouble("storedEU", storedEU);
        nbt.setBoolean("isFormed", isFormed);
        nbt.setInteger("facing", facing);
        nbt.setInteger("currentSpeed", currentSpeed);
        nbt.setInteger("waterRemainder", waterCondensationRemainder);
        nbt.setLong("recipesDone", recipesDone);
        nbt.setBoolean("redstonePowered", redstonePowered);

        NBTTagCompound inputNBT = new NBTTagCompound();
        inputTank.writeToNBT(inputNBT);
        nbt.setTag("inputTank", inputNBT);

        NBTTagCompound outputNBT = new NBTTagCompound();
        outputTank.writeToNBT(outputNBT);
        nbt.setTag("outputTank", outputNBT);

        if (rotorSlot != null) {
            NBTTagCompound rotorNBT = new NBTTagCompound();
            rotorSlot.writeToNBT(rotorNBT);
            nbt.setTag("rotorSlot", rotorNBT);
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        storedEU = nbt.getDouble("storedEU");
        isFormed = nbt.getBoolean("isFormed");
        facing = nbt.getInteger("facing");
        currentSpeed = nbt.getInteger("currentSpeed");
        waterCondensationRemainder = nbt.getInteger("waterRemainder");
        recipesDone = nbt.getLong("recipesDone");
        redstonePowered = nbt.getBoolean("redstonePowered");

        inputTank.readFromNBT(nbt.getCompoundTag("inputTank"));
        outputTank.readFromNBT(nbt.getCompoundTag("outputTank"));

        if (nbt.hasKey("rotorSlot")) {
            rotorSlot = ItemStack.loadItemStackFromNBT(nbt.getCompoundTag("rotorSlot"));
        } else {
            rotorSlot = null;
        }
    }

    @Override
    public Packet getDescriptionPacket() {
        NBTTagCompound tag = new NBTTagCompound();
        writeSyncNBT(tag);
        return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 1, tag);
    }

    @Override
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) {
        readSyncNBT(pkt.func_148857_g());
    }

    private void writeSyncNBT(NBTTagCompound nbt) {
        nbt.setDouble("storedEU", storedEU);
        nbt.setBoolean("isFormed", isFormed);
        nbt.setInteger("currentSpeed", currentSpeed);
        nbt.setInteger("lastEUOutput", lastEUOutput);
        nbt.setInteger("lastSteamConsumed", lastSteamConsumed);
        nbt.setFloat("currentEfficiency", currentEfficiency);
        nbt.setInteger("waterRemainder", waterCondensationRemainder);
        nbt.setLong("recipesDone", recipesDone);
        nbt.setBoolean("redstonePowered", redstonePowered);

        NBTTagCompound inputNBT = new NBTTagCompound();
        inputTank.writeToNBT(inputNBT);
        nbt.setTag("inputTank", inputNBT);

        NBTTagCompound outputNBT = new NBTTagCompound();
        outputTank.writeToNBT(outputNBT);
        nbt.setTag("outputTank", outputNBT);
    }

    private void readSyncNBT(NBTTagCompound nbt) {
        storedEU = nbt.getDouble("storedEU");
        isFormed = nbt.getBoolean("isFormed");
        currentSpeed = nbt.getInteger("currentSpeed");
        lastEUOutput = nbt.getInteger("lastEUOutput");
        lastSteamConsumed = nbt.getInteger("lastSteamConsumed");
        currentEfficiency = nbt.getFloat("currentEfficiency");
        waterCondensationRemainder = nbt.getInteger("waterRemainder");
        recipesDone = nbt.getLong("recipesDone");
        redstonePowered = nbt.getBoolean("redstonePowered");

        inputTank.readFromNBT(nbt.getCompoundTag("inputTank"));
        outputTank.readFromNBT(nbt.getCompoundTag("outputTank"));
    }
}
