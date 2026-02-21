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

    // Cache for syncing
    private int lastSpeed;
    private int lastStoredEU;
    private int lastInputAmount;
    private int lastOutputAmount;

    public ContainerTurbine(InventoryPlayer playerInv, TileEntityTurbineController tile) {
        this.tile = tile;

        // Rotor slot in controller area
        this.addSlotToContainer(new Slot(tile, 0, 145, 57) {

            @Override
            public boolean isItemValid(ItemStack stack) {
                return stack.getItem() instanceof ItemTurbineRotor;
            }
        });

        // Vanilla player inventory layout
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlotToContainer(new Slot(playerInv, j + i * 9 + 9, 8 + j * 18, 88 + i * 18));
            }
        }

        // Hotbar
        for (int i = 0; i < 9; ++i) {
            this.addSlotToContainer(new Slot(playerInv, i, 8 + i * 18, 146));
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

            // Sync currentSpeed
            if (this.lastSpeed != this.tile.getCurrentSpeed()) {
                icrafting.sendProgressBarUpdate(this, 0, this.tile.getCurrentSpeed());
            }

            // Sync storedEU (truncated to int, usually fine for GUI bar)
            int stored = (int) this.tile.getStoredEU();
            if (this.lastStoredEU != stored) {
                icrafting.sendProgressBarUpdate(this, 1, stored);
            }

            // Sync Fluid
            int inAmt = this.tile.inputTank.getFluidAmount();
            if (this.lastInputAmount != inAmt) {
                icrafting.sendProgressBarUpdate(this, 2, inAmt);
            }

            int outAmt = this.tile.outputTank.getFluidAmount();
            if (this.lastOutputAmount != outAmt) {
                icrafting.sendProgressBarUpdate(this, 3, outAmt);
            }
        }

        this.lastSpeed = this.tile.getCurrentSpeed();
        this.lastStoredEU = (int) this.tile.getStoredEU();
        this.lastInputAmount = this.tile.inputTank.getFluidAmount();
        this.lastOutputAmount = this.tile.outputTank.getFluidAmount();
    }

    @Override
    public void updateProgressBar(int id, int data) {
        switch (id) {
            case 0:
                this.tile.setCurrentSpeed(data);
                break;
            case 1:
                this.tile.setStoredEU(data);
                break;
            case 2:
                // Client-side visual update for tank
                if (this.tile.inputTank.getFluid() != null) {
                    this.tile.inputTank.getFluid().amount = data;
                }
                break;
            case 3:
                // Output tank sync
                if (this.tile.outputTank.getFluid() != null) {
                    this.tile.outputTank.getFluid().amount = data;
                }
                break;
        }
    }
}
