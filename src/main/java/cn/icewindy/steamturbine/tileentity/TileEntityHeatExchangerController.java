package cn.icewindy.steamturbine.tileentity;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;

import cn.icewindy.steamturbine.ModConfig;
import cn.icewindy.steamturbine.util.HeatExchangerCoolantRegistry;
import cn.icewindy.steamturbine.util.HeatExchangerValidator;

/**
 * GT5U Large Heat Exchanger 的独立移植实现。
 */
public class TileEntityHeatExchangerController extends TileEntity implements IFluidHandler {

    private static final int STEAM_PER_WATER = 160;

    private final FluidTank hotTank = new FluidTank(ModConfig.tankCapacity * 2);
    private final FluidTank waterTank = new FluidTank(ModConfig.tankCapacity * 2);
    private final FluidTank steamTank = new FluidTank(ModConfig.tankCapacity * 2);
    private final FluidTank coldTank = new FluidTank(ModConfig.tankCapacity * 2);

    private TileEntityFluidInputHatch hotInputHatch;
    private TileEntityFluidOutputHatch coldOutputHatch;
    private final List<TileEntityFluidInputHatch> waterInputHatches = new ArrayList<TileEntityFluidInputHatch>();
    private final List<TileEntityFluidOutputHatch> steamOutputHatches = new ArrayList<TileEntityFluidOutputHatch>();

    private int facing = 2;
    private boolean formed = false;
    private boolean superheated = false;
    private int superheatedThreshold = 0;
    private int dryHeatCounter = 0;
    private int checkTimer = 0;
    private int lastHotFluidConsumed = 0;
    private int lastSteamProduced = 0;
    private long recipesDone = 0;
    private String lastValidationError = "";

    @Override
    public void updateEntity() {
        if (worldObj.isRemote) return;

        checkTimer++;
        if (checkTimer >= ModConfig.checkInterval) {
            checkTimer = 0;
            recheckStructure();
        }

        if (!formed) {
            lastHotFluidConsumed = 0;
            lastSteamProduced = 0;
            return;
        }

        pullFromHatches();

        processOncePerTick();

        pushToHatches();
        markDirty();
    }

    public void clearHatches() {
        hotInputHatch = null;
        coldOutputHatch = null;
        waterInputHatches.clear();
        steamOutputHatches.clear();
    }

    public void setHotInputHatch(TileEntityFluidInputHatch hatch) {
        this.hotInputHatch = hatch;
    }

    public void setColdOutputHatch(TileEntityFluidOutputHatch hatch) {
        this.coldOutputHatch = hatch;
    }

    public void addWaterInputHatch(TileEntityFluidInputHatch hatch) {
        waterInputHatches.add(hatch);
    }

    public void addSteamOutputHatch(TileEntityFluidOutputHatch hatch) {
        steamOutputHatches.add(hatch);
    }

    public void recheckStructure() {
        HeatExchangerValidator.ValidationResult result = HeatExchangerValidator
            .validate(worldObj, xCoord, yCoord, zCoord, ForgeDirection.getOrientation(facing), this);
        boolean newFormed = result.isValid;
        lastValidationError = result.errorMessage == null ? "" : result.errorMessage;
        if (newFormed != formed) {
            formed = newFormed;
            worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
        }
    }

    private void pullFromHatches() {
        if (hotInputHatch != null && !hotInputHatch.isInvalid()) {
            FluidStack hot = hotInputHatch.drain(ForgeDirection.UNKNOWN, hotInputHatch.getMaxFlowPerTick(), false);
            if (hot != null && hot.amount > 0 && HeatExchangerCoolantRegistry.get(hot.getFluid()) != null) {
                int filled = hotTank.fill(hot, true);
                if (filled > 0) {
                    hotInputHatch.drain(ForgeDirection.UNKNOWN, filled, true);
                }
            }
        }

        for (TileEntityFluidInputHatch hatch : waterInputHatches) {
            if (hatch == null || hatch.isInvalid()) continue;
            FluidStack water = hatch.drain(ForgeDirection.UNKNOWN, hatch.getMaxFlowPerTick(), false);
            if (water == null || water.amount <= 0 || !isWaterLike(water.getFluid())) continue;
            int filled = waterTank.fill(water, true);
            if (filled > 0) {
                hatch.drain(ForgeDirection.UNKNOWN, filled, true);
            }
            if (waterTank.getFluidAmount() >= waterTank.getCapacity()) {
                break;
            }
        }
    }

    private void pushToHatches() {
        if (coldOutputHatch != null && !coldOutputHatch.isInvalid()) {
            FluidStack available = coldTank.getFluid();
            if (available != null && available.amount > 0) {
                int filled = coldOutputHatch
                    .fill(ForgeDirection.UNKNOWN, new FluidStack(available, available.amount), true);
                if (filled > 0) {
                    coldTank.drain(filled, true);
                }
            }
        }

        if (steamTank.getFluidAmount() <= 0 || steamOutputHatches.isEmpty()) {
            return;
        }
        for (TileEntityFluidOutputHatch hatch : steamOutputHatches) {
            if (hatch == null || hatch.isInvalid()) continue;
            FluidStack available = steamTank.getFluid();
            if (available == null || available.amount <= 0) break;
            int filled = hatch.fill(ForgeDirection.UNKNOWN, new FluidStack(available, available.amount), true);
            if (filled > 0) {
                steamTank.drain(filled, true);
            }
        }
    }

    private void processOncePerTick() {
        FluidStack hot = hotTank.getFluid();
        if (hot == null || hot.amount <= 0) {
            superheated = false;
            superheatedThreshold = 0;
            lastHotFluidConsumed = 0;
            lastSteamProduced = 0;
            return;
        }

        HeatExchangerCoolantRegistry.CoolantInfo coolant = HeatExchangerCoolantRegistry.get(hot.getFluid());
        if (coolant == null) {
            superheated = false;
            superheatedThreshold = 0;
            lastHotFluidConsumed = 0;
            lastSteamProduced = 0;
            return;
        }

        superheatedThreshold = Math
            .max(1, Math.round(ModConfig.lheBaseThresholdPerTick * coolant.superheatedThreshold));
        int consumeAmount = Math.min(hot.amount, superheatedThreshold * 2);
        boolean shouldUseSuperheated = ModConfig.lheEnableSuperheatedOutput && consumeAmount >= superheatedThreshold;
        superheated = shouldUseSuperheated;

        float steamOutputMultiplier = ModConfig.lheBaseSteamMultiplier * coolant.steamMultiplier;
        int steamAmount = Math.max(0, Math.round(consumeAmount * steamOutputMultiplier * 2.0f));
        if (shouldUseSuperheated) {
            steamAmount /= 2;
        }

        Fluid steamFluid = pickSteamFluid(shouldUseSuperheated);
        FluidStack coldOutput = coolant.getColdFluid(consumeAmount);
        if (steamFluid == null || coldOutput == null || steamAmount <= 0) {
            superheated = false;
            lastHotFluidConsumed = 0;
            lastSteamProduced = 0;
            return;
        }

        int waterNeed = (steamAmount + STEAM_PER_WATER - 1) / STEAM_PER_WATER;
        if (waterTank.getFluidAmount() < waterNeed) {
            dryHeatCounter += 1;
            lastHotFluidConsumed = 0;
            lastSteamProduced = 0;
            if (dryHeatCounter >= ModConfig.lheDryHeatMaxTicks) {
                worldObj.createExplosion(null, xCoord + 0.5, yCoord + 0.5, zCoord + 0.5, 6.0f, true);
            }
            return;
        }

        dryHeatCounter = 0;
        hotTank.drain(consumeAmount, true);
        waterTank.drain(waterNeed, true);
        coldTank.fill(coldOutput, true);
        steamTank.fill(new FluidStack(steamFluid, steamAmount), true);

        lastHotFluidConsumed = consumeAmount;
        lastSteamProduced = steamAmount;
        recipesDone++;
    }

    private Fluid pickSteamFluid(boolean sh) {
        if (sh) {
            Fluid shSteam = FluidRegistry.getFluid("ic2superheatedsteam");
            if (shSteam != null) return shSteam;
        }
        Fluid steam = FluidRegistry.getFluid("steam");
        if (steam != null) return steam;
        return FluidRegistry.getFluid("ic2steam");
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        facing = nbt.getInteger("Facing");
        formed = nbt.getBoolean("Formed");
        superheated = nbt.getBoolean("Superheated");
        superheatedThreshold = nbt.getInteger("SuperheatedThreshold");
        dryHeatCounter = nbt.getInteger("DryHeatCounter");
        lastHotFluidConsumed = nbt.getInteger("LastHot");
        lastSteamProduced = nbt.getInteger("LastSteam");
        recipesDone = nbt.getLong("RecipesDone");
        lastValidationError = nbt.getString("LastValidationError");
        hotTank.readFromNBT(nbt.getCompoundTag("HotTank"));
        waterTank.readFromNBT(nbt.getCompoundTag("WaterTank"));
        steamTank.readFromNBT(nbt.getCompoundTag("SteamTank"));
        coldTank.readFromNBT(nbt.getCompoundTag("ColdTank"));
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setInteger("Facing", facing);
        nbt.setBoolean("Formed", formed);
        nbt.setBoolean("Superheated", superheated);
        nbt.setInteger("SuperheatedThreshold", superheatedThreshold);
        nbt.setInteger("DryHeatCounter", dryHeatCounter);
        nbt.setInteger("LastHot", lastHotFluidConsumed);
        nbt.setInteger("LastSteam", lastSteamProduced);
        nbt.setLong("RecipesDone", recipesDone);
        nbt.setString("LastValidationError", lastValidationError == null ? "" : lastValidationError);
        NBTTagCompound hotTag = new NBTTagCompound();
        hotTank.writeToNBT(hotTag);
        nbt.setTag("HotTank", hotTag);
        NBTTagCompound waterTag = new NBTTagCompound();
        waterTank.writeToNBT(waterTag);
        nbt.setTag("WaterTank", waterTag);
        NBTTagCompound steamTag = new NBTTagCompound();
        steamTank.writeToNBT(steamTag);
        nbt.setTag("SteamTank", steamTag);
        NBTTagCompound coldTag = new NBTTagCompound();
        coldTank.writeToNBT(coldTag);
        nbt.setTag("ColdTank", coldTag);
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

    @Override
    public int fill(ForgeDirection from, FluidStack resource, boolean doFill) {
        if (resource == null || resource.getFluid() == null) return 0;
        if (HeatExchangerCoolantRegistry.get(resource.getFluid()) != null) {
            return hotTank.fill(resource, doFill);
        }
        if (isWaterLike(resource.getFluid())) {
            return waterTank.fill(resource, doFill);
        }
        return 0;
    }

    @Override
    public FluidStack drain(ForgeDirection from, FluidStack resource, boolean doDrain) {
        if (resource == null) return null;
        FluidStack steam = steamTank.getFluid();
        if (steam != null && resource.isFluidEqual(steam)) {
            return steamTank.drain(resource.amount, doDrain);
        }
        FluidStack cold = coldTank.getFluid();
        if (cold != null && resource.isFluidEqual(cold)) {
            return coldTank.drain(resource.amount, doDrain);
        }
        return null;
    }

    @Override
    public FluidStack drain(ForgeDirection from, int maxDrain, boolean doDrain) {
        FluidStack steam = steamTank.getFluid();
        if (steam != null && steam.amount > 0) {
            return steamTank.drain(maxDrain, doDrain);
        }
        return coldTank.drain(maxDrain, doDrain);
    }

    @Override
    public boolean canFill(ForgeDirection from, Fluid fluid) {
        return fluid != null && (HeatExchangerCoolantRegistry.get(fluid) != null || isWaterLike(fluid));
    }

    @Override
    public boolean canDrain(ForgeDirection from, Fluid fluid) {
        return true;
    }

    @Override
    public FluidTankInfo[] getTankInfo(ForgeDirection from) {
        return new FluidTankInfo[] { hotTank.getInfo(), waterTank.getInfo(), steamTank.getInfo(), coldTank.getInfo() };
    }

    private boolean isWaterLike(Fluid fluid) {
        if (fluid == null || fluid.getName() == null) return false;
        String name = fluid.getName()
            .toLowerCase();
        return name.equals("water") || name.contains("distilledwater");
    }

    public boolean isFormed() {
        return formed;
    }

    public boolean isSuperheated() {
        return superheated;
    }

    public int getSuperheatedThreshold() {
        return superheatedThreshold;
    }

    public int getLastHotFluidConsumed() {
        return lastHotFluidConsumed;
    }

    public int getLastSteamProduced() {
        return lastSteamProduced;
    }

    public long getRecipesDone() {
        return recipesDone;
    }

    public void setFacing(int facing) {
        this.facing = facing;
    }

    public String[] getInfoLines() {
        boolean wouldSuperheatAtCurrentRate = lastHotFluidConsumed >= superheatedThreshold && superheatedThreshold > 0;
        return new String[] { "[大型热交换] 成型: " + (formed ? "是" : "否"), "[大型热交换] 过热蒸汽模式: " + (superheated ? "是" : "否"),
            "[大型热交换] 允许过热蒸汽: " + (ModConfig.lheEnableSuperheatedOutput ? "是" : "否"),
            "[大型热交换] 当前热流体速率: " + lastHotFluidConsumed + " mB/t", "[大型热交换] 过热阈值: " + superheatedThreshold + " mB/t",
            "[大型热交换] 当前速率在开启过热时将触发过热: " + (wouldSuperheatAtCurrentRate ? "是" : "否"),
            "[大型热交换] 蒸汽产出: " + lastSteamProduced + " mB/t",
            "[大型热交换] 结构错误: "
                + (lastValidationError == null || lastValidationError.isEmpty() ? "无" : lastValidationError),
            "[大型热交换] 累计运行次数: " + recipesDone };
    }
}
