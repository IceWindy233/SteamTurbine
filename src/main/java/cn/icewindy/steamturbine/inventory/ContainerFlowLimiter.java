package cn.icewindy.steamturbine.inventory;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ICrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import cn.icewindy.steamturbine.tileentity.TileEntityFluidInputHatch;

public class ContainerFlowLimiter extends Container {

    private final TileEntityFluidInputHatch tile;
    private int lastFlowRate;
    private int lastFluidAmount;

    public ContainerFlowLimiter(InventoryPlayer playerInv, TileEntityFluidInputHatch tile) {
        this.tile = tile;

        // Move player inventory up by 16 px to match user request.
        final int invY = 100;
        final int hotbarY = 158;
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlotToContainer(new Slot(playerInv, j + i * 9 + 9, 9 + j * 18, invY + i * 18));
            }
        }
        for (int i = 0; i < 9; ++i) {
            this.addSlotToContainer(new Slot(playerInv, i, 9 + i * 18, hotbarY));
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return tile != null && tile.getWorldObj() != null
            && tile.getWorldObj()
                .getTileEntity(tile.xCoord, tile.yCoord, tile.zCoord) == tile
            && player.getDistanceSq(tile.xCoord + 0.5D, tile.yCoord + 0.5D, tile.zCoord + 0.5D) <= 64.0D;
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        return null;
    }

    @Override
    public boolean enchantItem(EntityPlayer player, int id) {
        // 0:-100, 1:-10, 2:+10, 3:+100
        int delta = 0;
        switch (id) {
            case 0:
                delta = -100;
                break;
            case 1:
                delta = -10;
                break;
            case 2:
                delta = 10;
                break;
            case 3:
                delta = 100;
                break;
            default:
                return false;
        }
        tile.adjustFlowPerTick(delta);
        return true;
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        int flow = tile.getMaxFlowPerTick();
        int amount = tile.getFluid() == null ? 0 : tile.getFluid().amount;
        for (int i = 0; i < this.crafters.size(); ++i) {
            ICrafting c = (ICrafting) this.crafters.get(i);
            if (flow != lastFlowRate) {
                c.sendProgressBarUpdate(this, 0, flow);
            }
            if (amount != lastFluidAmount) {
                c.sendProgressBarUpdate(this, 1, amount);
            }
        }
        lastFlowRate = flow;
        lastFluidAmount = amount;
    }

    @Override
    public void updateProgressBar(int id, int data) {
        if (id == 0) {
            tile.setMaxFlowPerTick(data);
        } else if (id == 1 && tile.getFluid() != null) {
            tile.getFluid().amount = data;
        }
    }
}
