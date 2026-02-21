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
    private long lastCtrlStored;
    private long lastCtrlMax;
    private long lastDynStored;
    private long lastDynMax;
    private int lastInputAmount;
    private int lastOutputAmount;
    private int syncTimer = 0;

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
            long cStored = this.tile.getControllerStored();
            icrafting.sendProgressBarUpdate(this, 1, (int) (cStored & 0xFFFF));
            icrafting.sendProgressBarUpdate(this, 5, (int) ((cStored >> 16) & 0xFFFF));

            // Sync Controller Max (ID 4, 6)
            long cMax = this.tile.getControllerCapacity();
            icrafting.sendProgressBarUpdate(this, 4, (int) (cMax & 0xFFFF));
            icrafting.sendProgressBarUpdate(this, 6, (int) ((cMax >> 16) & 0xFFFF));

            // Sync Dynamo Stored (ID 7, 11)
            long dStored = this.tile.getDynamoStored();
            icrafting.sendProgressBarUpdate(this, 7, (int) (dStored & 0xFFFF));
            icrafting.sendProgressBarUpdate(this, 11, (int) ((dStored >> 16) & 0xFFFF));

            // Sync Dynamo Max (ID 8, 12)
            long dMax = this.tile.getDynamoCapacity();
            icrafting.sendProgressBarUpdate(this, 8, (int) (dMax & 0xFFFF));
            icrafting.sendProgressBarUpdate(this, 12, (int) ((dMax >> 16) & 0xFFFF));

            // Sync Speed
            icrafting.sendProgressBarUpdate(this, 0, this.tile.getCurrentSpeed());

            // Sync Tank Amounts
            icrafting.sendProgressBarUpdate(this, 2, this.tile.inputTank.getFluidAmount());
            icrafting.sendProgressBarUpdate(this, 3, this.tile.outputTank.getFluidAmount());
        }

        // No need for syncTimer or lastValue checks if we sync every tick
        this.lastSpeed = this.tile.getCurrentSpeed();
        this.lastCtrlStored = this.tile.getControllerStored();
        this.lastCtrlMax = this.tile.getControllerCapacity();
        this.lastDynStored = this.tile.getDynamoStored();
        this.lastDynMax = this.tile.getDynamoCapacity();
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
