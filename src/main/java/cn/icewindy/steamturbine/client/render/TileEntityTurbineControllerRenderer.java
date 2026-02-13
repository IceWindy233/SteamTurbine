package cn.icewindy.steamturbine.client.render;

import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;

import org.lwjgl.opengl.GL11;

import cn.icewindy.steamturbine.block.BlockTurbineController;
import cn.icewindy.steamturbine.tileentity.TileEntityTurbineController;

public class TileEntityTurbineControllerRenderer extends TileEntitySpecialRenderer {

    @Override
    public void renderTileEntityAt(TileEntity te, double x, double y, double z, float partialTicks) {
        TileEntityTurbineController controller = (TileEntityTurbineController) te;
        if (!controller.isFormed()) return;

        int facing = controller.getFacing();

        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_LIGHTING);

        // Reset color to pure white
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

        // Set brightness based on a 3x3 area IN FRONT of the machine
        net.minecraft.client.renderer.RenderHelper.disableStandardItemLighting();
        int maxBrightness = 0;

        // Define scanning range based on facing
        int scanXStart = 0, scanXEnd = 0;
        int scanZStart = 0, scanZEnd = 0;
        int scanYStart = controller.yCoord - 1;
        int scanYEnd = controller.yCoord + 1;

        int offX = 0, offZ = 0;
        if (facing == 2) {
            offZ = -1;
            scanXStart = controller.xCoord - 1;
            scanXEnd = controller.xCoord + 1;
            scanZStart = scanZEnd = controller.zCoord + offZ;
        } else if (facing == 3) {
            offZ = 1;
            scanXStart = controller.xCoord - 1;
            scanXEnd = controller.xCoord + 1;
            scanZStart = scanZEnd = controller.zCoord + offZ;
        } else if (facing == 4) {
            offX = -1;
            scanZStart = controller.zCoord - 1;
            scanZEnd = controller.zCoord + 1;
            scanXStart = scanXEnd = controller.xCoord + offX;
        } else if (facing == 5) {
            offX = 1;
            scanZStart = controller.zCoord - 1;
            scanZEnd = controller.zCoord + 1;
            scanXStart = scanXEnd = controller.xCoord + offX;
        }

        // Scan the 3x3 area
        for (int sy = scanYStart; sy <= scanYEnd; sy++) {
            for (int sx = scanXStart; sx <= scanXEnd; sx++) {
                for (int sz = scanZStart; sz <= scanZEnd; sz++) {
                    int b = controller.getWorldObj()
                        .getLightBrightnessForSkyBlocks(sx, sy, sz, 0);
                    if (b > maxBrightness) maxBrightness = b;
                }
            }
        }

        // Ambient Floor: Ensure it's never 100% black (approximately light level 4)
        int minL = 64;
        int skyLight = Math.max(minL, maxBrightness % 65536);
        int blockLight = Math.max(minL, maxBrightness / 65536);

        net.minecraft.client.renderer.OpenGlHelper.setLightmapTextureCoords(
            net.minecraft.client.renderer.OpenGlHelper.lightmapTexUnit,
            (float) skyLight,
            (float) blockLight);

        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

        GL11.glTranslated(x + 0.5, y + 0.5, z + 0.5);

        // Rotation mapping based on ForgeDirection/Metadata
        switch (facing) {
            case 2:
                GL11.glRotatef(180.0F, 0.0F, 1.0F, 0.0F);
                break; // North (Z-)
            case 3:
                GL11.glRotatef(0.0F, 0.0F, 1.0F, 0.0F);
                break; // South (Z+)
            case 4:
                GL11.glRotatef(270.0F, 0.0F, 1.0F, 0.0F);
                break; // West (X-)
            case 5:
                GL11.glRotatef(90.0F, 0.0F, 1.0F, 0.0F);
                break; // East (X+)
        }

        // Move to face surface, using a safe offset to prevent Z-fighting with casing
        GL11.glTranslated(0.0, 0.0, 0.51);

        this.bindTexture(TextureMap.locationBlocksTexture);
        Tessellator tessellator = Tessellator.instance;

        // Layer 1: Base Ring (Bottom)
        renderPlate(tessellator, BlockTurbineController.rotorBaseRing, 1.5, 0.001);

        // Layer 2: Rotor Layer (Middle)
        boolean isSpinning = controller.getCurrentSpeed() > 0;
        IIcon rotorIcon = isSpinning ? BlockTurbineController.rotorSpinning : BlockTurbineController.rotorIdle;
        renderPlate(tessellator, rotorIcon, 1.5, 0.002);

        // Layer 3: Base BG (Top Frame)
        renderPlate(tessellator, BlockTurbineController.rotorBaseBG, 1.5, 0.003);

        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
    }

    private void renderPlate(Tessellator tessellator, IIcon icon, double size, double zOffset) {
        if (icon == null) return;
        float minU = icon.getMinU();
        float maxU = icon.getMaxU();
        float minV = icon.getMinV();
        float maxV = icon.getMaxV();

        tessellator.startDrawingQuads();
        tessellator.setNormal(0.0F, 0.0F, 1.0F);
        tessellator.addVertexWithUV(-size, -size, zOffset, minU, maxV);
        tessellator.addVertexWithUV(size, -size, zOffset, maxU, maxV);
        tessellator.addVertexWithUV(size, size, zOffset, maxU, minV);
        tessellator.addVertexWithUV(-size, size, zOffset, minU, minV);
        tessellator.draw();
    }
}
