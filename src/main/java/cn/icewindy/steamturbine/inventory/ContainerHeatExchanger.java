package cn.icewindy.steamturbine.inventory;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ICrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import cn.icewindy.steamturbine.tileentity.TileEntityHeatExchangerController;

public class ContainerHeatExchanger extends Container {

    private final TileEntityHeatExchangerController tile;
    private boolean lastFormed;
    private boolean lastSuperheated;
    private int lastSuperheatedThreshold = Integer.MIN_VALUE;
    private int lastHotFluidConsumed = Integer.MIN_VALUE;
    private int lastSteamProduced = Integer.MIN_VALUE;

    private int clientThresholdLow;
    private int clientThresholdHigh;
    private int clientHotLow;
    private int clientHotHigh;
    private int clientSteamLow;
    private int clientSteamHigh;

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

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();

        boolean formed = tile.isFormed();
        boolean superheated = tile.isSuperheated();
        int superheatedThreshold = tile.getSuperheatedThreshold();
        int hotFluidConsumed = tile.getLastHotFluidConsumed();
        int steamProduced = tile.getLastSteamProduced();

        for (int i = 0; i < this.crafters.size(); ++i) {
            ICrafting c = (ICrafting) this.crafters.get(i);
            if (formed != lastFormed) {
                c.sendProgressBarUpdate(this, 0, formed ? 1 : 0);
            }
            if (superheated != lastSuperheated) {
                c.sendProgressBarUpdate(this, 1, superheated ? 1 : 0);
            }
            if (superheatedThreshold != lastSuperheatedThreshold) {
                sendInt(c, 2, superheatedThreshold);
            }
            if (hotFluidConsumed != lastHotFluidConsumed) {
                sendInt(c, 4, hotFluidConsumed);
            }
            if (steamProduced != lastSteamProduced) {
                sendInt(c, 6, steamProduced);
            }
        }

        lastFormed = formed;
        lastSuperheated = superheated;
        lastSuperheatedThreshold = superheatedThreshold;
        lastHotFluidConsumed = hotFluidConsumed;
        lastSteamProduced = steamProduced;
    }

    private void sendInt(ICrafting c, int baseId, int value) {
        c.sendProgressBarUpdate(this, baseId, value & 0xFFFF);
        c.sendProgressBarUpdate(this, baseId + 1, (value >>> 16) & 0xFFFF);
    }

    @Override
    public void updateProgressBar(int id, int data) {
        int unsigned = data & 0xFFFF;
        switch (id) {
            case 0:
                tile.setGuiFormed(data != 0);
                break;
            case 1:
                tile.setGuiSuperheated(data != 0);
                break;
            case 2:
                clientThresholdLow = unsigned;
                tile.setGuiSuperheatedThreshold((clientThresholdHigh << 16) | clientThresholdLow);
                break;
            case 3:
                clientThresholdHigh = unsigned;
                tile.setGuiSuperheatedThreshold((clientThresholdHigh << 16) | clientThresholdLow);
                break;
            case 4:
                clientHotLow = unsigned;
                tile.setGuiLastHotFluidConsumed((clientHotHigh << 16) | clientHotLow);
                break;
            case 5:
                clientHotHigh = unsigned;
                tile.setGuiLastHotFluidConsumed((clientHotHigh << 16) | clientHotLow);
                break;
            case 6:
                clientSteamLow = unsigned;
                tile.setGuiLastSteamProduced((clientSteamHigh << 16) | clientSteamLow);
                break;
            case 7:
                clientSteamHigh = unsigned;
                tile.setGuiLastSteamProduced((clientSteamHigh << 16) | clientSteamLow);
                break;
            default:
                break;
        }
    }
}
