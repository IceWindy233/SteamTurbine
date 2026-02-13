package cn.icewindy.steamturbine.inventory;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ICrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import cn.icewindy.steamturbine.item.ItemTurbineRotor;
import cn.icewindy.steamturbine.tileentity.TileEntityTurbineController;

public class ContainerTurbine extends Container {

    private TileEntityTurbineController tile;

    private int lastSpeed;
    private int lastCtrlStored;
    private int lastCtrlMax;
    private int lastDynStored;
    private int lastDynMax;
    private int lastInputAmount;
    private int lastOutputAmount;

    public ContainerTurbine(InventoryPlayer playerInv, TileEntityTurbineController tile) {
        this.tile = tile;

        // Rotor slot in controller area
        this.addSlotToContainer(new Slot(tile, 0, 153, 57) {

            @Override
            public boolean isItemValid(ItemStack stack) {
                return stack.getItem() instanceof ItemTurbineRotor;
            }
        });

        // Vanilla player inventory layout
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlotToContainer(new Slot(playerInv, j + i * 9 + 9, 9 + j * 18, 89 + i * 18));
            }
        }

        // Hotbar
        for (int i = 0; i < 9; ++i) {
            this.addSlotToContainer(new Slot(playerInv, i, 9 + i * 18, 147));
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return tile.isUseableByPlayer(player);
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        // Simple shift-click logic
        ItemStack itemstack = null;
        Slot slot = (Slot) this.inventorySlots.get(index);

        if (slot != null && slot.getHasStack()) {
            ItemStack itemstack1 = slot.getStack();
            itemstack = itemstack1.copy();

            if (index == 0) {
                // From rotor slot to player inv
                if (!this.mergeItemStack(itemstack1, 1, 37, true)) {
                    return null;
                }
            } else {
                // From player inv to rotor slot
                if (itemstack1.getItem() instanceof ItemTurbineRotor) {
                    if (!this.mergeItemStack(itemstack1, 0, 1, false)) {
                        return null;
                    }
                } else {
                    return null;
                }
            }

            if (itemstack1.stackSize == 0) {
                slot.putStack(null);
            } else {
                slot.onSlotChanged();
            }

            if (itemstack1.stackSize == itemstack.stackSize) {
                return null;
            }

            slot.onPickupFromSlot(player, itemstack1);
        }

        return itemstack;
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();

        for (int i = 0; i < this.crafters.size(); ++i) {
            ICrafting icrafting = (ICrafting) this.crafters.get(i);

            // Sync Controller Stored (ID 1, 5)
            int cStored = (int) this.tile.getControllerStored();
            if (this.lastCtrlStored != cStored) {
                icrafting.sendProgressBarUpdate(this, 1, cStored & 0xFFFF);
                icrafting.sendProgressBarUpdate(this, 5, (cStored >> 16) & 0xFFFF);
            }

            // Sync Controller Max (ID 4, 6)
            int cMax = (int) this.tile.getControllerCapacity();
            if (this.lastCtrlMax != cMax) {
                icrafting.sendProgressBarUpdate(this, 4, cMax & 0xFFFF);
                icrafting.sendProgressBarUpdate(this, 6, (cMax >> 16) & 0xFFFF);
            }

            // Sync Dynamo Stored (ID 7, 11)
            int dStored = (int) this.tile.getDynamoStored();
            if (this.lastDynStored != dStored) {
                icrafting.sendProgressBarUpdate(this, 7, dStored & 0xFFFF);
                icrafting.sendProgressBarUpdate(this, 11, (dStored >> 16) & 0xFFFF);
            }

            // Sync Dynamo Max (ID 8, 12)
            int dMax = (int) this.tile.getDynamoCapacity();
            if (this.lastDynMax != dMax) {
                icrafting.sendProgressBarUpdate(this, 8, dMax & 0xFFFF);
                icrafting.sendProgressBarUpdate(this, 12, (dMax >> 16) & 0xFFFF);
            }

            // Sync Fluid
            int inAmt = this.tile.inputTank.getFluidAmount();
            if (this.lastInputAmount != inAmt) icrafting.sendProgressBarUpdate(this, 2, inAmt);
            int outAmt = this.tile.outputTank.getFluidAmount();
            if (this.lastOutputAmount != outAmt) icrafting.sendProgressBarUpdate(this, 3, outAmt);

            if (this.lastSpeed != this.tile.getCurrentSpeed()) icrafting.sendProgressBarUpdate(this, 0, this.tile.getCurrentSpeed());
        }

        this.lastSpeed = this.tile.getCurrentSpeed();
        this.lastCtrlStored = (int) this.tile.getControllerStored();
        this.lastCtrlMax = (int) this.tile.getControllerCapacity();
        this.lastDynStored = (int) this.tile.getDynamoStored();
        this.lastDynMax = (int) this.tile.getDynamoCapacity();
        this.lastInputAmount = this.tile.inputTank.getFluidAmount();
        this.lastOutputAmount = this.tile.outputTank.getFluidAmount();
    }

    @Override
    public void updateProgressBar(int id, int data) {
        // 'data' comes as a short (16-bit signed), so we need to mask with 0xFFFF
        // if we want to treat it as unsigned bits for reassembly.
        int unsignedData = data & 0xFFFF;
        
        switch (id) {
            case 0:
                this.tile.setCurrentSpeed(data);
                break;
            case 1:
                this.tile.setGuiCtrlStored_L(unsignedData);
                break;
            case 5:
                this.tile.setGuiCtrlStored_H(unsignedData);
                break;
            case 2:
                if (this.tile.inputTank.getFluid() != null) {
                    this.tile.inputTank.getFluid().amount = data;
                }
                break;
            case 3:
                if (this.tile.outputTank.getFluid() != null) {
                    this.tile.outputTank.getFluid().amount = data;
                }
                break;
            case 4:
                this.tile.setGuiCtrlMax_L(unsignedData);
                break;
            case 6:
                this.tile.setGuiCtrlMax_H(unsignedData);
                break;
            case 7:
                this.tile.setGuiDynStored_L(unsignedData);
                break;
            case 11:
                this.tile.setGuiDynStored_H(unsignedData);
                break;
            case 8:
                this.tile.setGuiDynMax_L(unsignedData);
                break;
            case 12:
                this.tile.setGuiDynMax_H(unsignedData);
                break;
        }
    }
}
