package cn.icewindy.steamturbine.client.sound;

import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.util.ResourceLocation;

import cn.icewindy.steamturbine.tileentity.TileEntityTurbineController;

public class TurbineSound extends MovingSound {

    private final TileEntityTurbineController controller;

    public TurbineSound(TileEntityTurbineController controller) {
        super(new ResourceLocation("steamturbine:turbine_loop"));
        this.controller = controller;
        this.repeat = true;
        this.field_147665_h = 0; // repeatDelay
        this.volume = 1.0F;
        this.field_147663_c = 1.0F; // pitch (Standard 1.0F)
        this.field_147666_i = ISound.AttenuationType.LINEAR;
        this.xPosF = (float) controller.xCoord + 0.5F;
        this.yPosF = (float) controller.yCoord + 0.5F;
        this.zPosF = (float) controller.zCoord + 0.5F;
    }

    public void stopSound() {
        this.donePlaying = true;
    }

    @Override
    public void update() {
        if (controller.isInvalid() || !controller.isFormed() || controller.getCurrentSpeed() <= 0) {
            this.stopSound();
            return;
        }

        this.xPosF = (float) controller.xCoord + 0.5F;
        this.yPosF = (float) controller.yCoord + 0.5F;
        this.zPosF = (float) controller.zCoord + 0.5F;
    }
}
