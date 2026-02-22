package cn.icewindy.steamturbine.inventory;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import cn.icewindy.steamturbine.tileentity.TileEntityHeatExchangerController;

public class ContainerHeatExchanger extends Container {

    private final TileEntityHeatExchangerController tile;

    public ContainerHeatExchanger(InventoryPlayer playerInv, TileEntityHeatExchangerController tile) {
        this.tile = tile;

        // Player Inventory starting at 89, shifted 1px right
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlotToContainer(new Slot(playerInv, j + i * 9 + 9, 9 + j * 18, 89 + i * 18));
            }
        }

        // Hotbar starting at 147, shifted 1px right
        for (int i = 0; i < 9; ++i) {
            this.addSlotToContainer(new Slot(playerInv, i, 9 + i * 18, 147));
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return player.getDistanceSq(tile.xCoord + 0.5D, tile.yCoord + 0.5D, tile.zCoord + 0.5D) <= 64.0D;
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        return null; // No internal slots to transfer to
    }
}
