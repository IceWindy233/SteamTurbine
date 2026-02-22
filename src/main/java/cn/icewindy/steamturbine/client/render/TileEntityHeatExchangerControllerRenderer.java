package cn.icewindy.steamturbine.client.render;

import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraftforge.common.util.ForgeDirection;

import org.lwjgl.opengl.GL11;

import cn.icewindy.steamturbine.block.BlockHeatExchangerController;
import cn.icewindy.steamturbine.block.BlockTurbineController;
import cn.icewindy.steamturbine.registry.ModBlocks;
import cn.icewindy.steamturbine.tileentity.TileEntityFluidInputHatch;
import cn.icewindy.steamturbine.tileentity.TileEntityFluidOutputHatch;
import cn.icewindy.steamturbine.tileentity.TileEntityHeatExchangerController;

public class TileEntityHeatExchangerControllerRenderer extends TileEntitySpecialRenderer {

    @Override
    public void renderTileEntityAt(TileEntity te, double x, double y, double z, float partialTicks) {
        if (!(te instanceof TileEntityHeatExchangerController)) return;
        TileEntityHeatExchangerController controller = (TileEntityHeatExchangerController) te;
        if (!controller.isFormed()) return;

        Tessellator tessellator = Tessellator.instance;

        // Brightness synchronization
        int maxBrightness = controller.getWorldObj()
            .getLightBrightnessForSkyBlocks(controller.xCoord, controller.yCoord, controller.zCoord, 0);
        net.minecraft.client.renderer.OpenGlHelper.setLightmapTextureCoords(
            net.minecraft.client.renderer.OpenGlHelper.lightmapTexUnit,
            (float) (maxBrightness % 65536),
            (float) (maxBrightness / 65536));

        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glPushMatrix();
        GL11.glTranslated(x + 0.5, y + 0.5, z + 0.5);

        // Rendering state setup
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        this.bindTexture(net.minecraft.client.renderer.texture.TextureMap.locationBlocksTexture);

        // Create common overlay icon references for brevity
        IIcon inputIcon = BlockTurbineController.overlayInput;
        IIcon outputIcon = BlockTurbineController.overlayOutput;

        // Render Hatch Overlays
        for (int sx = controller.minX; sx <= controller.maxX; sx++) {
            for (int sy = controller.minY; sy <= controller.maxY; sy++) {
                for (int sz = controller.minZ; sz <= controller.maxZ; sz++) {
                    TileEntity hatch = controller.getWorldObj()
                        .getTileEntity(sx, sy, sz);
                    IIcon icon = null;
                    if (hatch instanceof TileEntityFluidInputHatch) {
                        icon = inputIcon;
                    } else if (hatch instanceof TileEntityFluidOutputHatch) {
                        icon = outputIcon;
                    }

                    if (icon != null) {
                        int brightness = controller.getWorldObj()
                            .getLightBrightnessForSkyBlocks(sx, sy, sz, 0);
                        tessellator.setBrightness(brightness);
                        renderBlockOverlay(tessellator, sx, sy, sz, controller, icon, false);
                    }
                }
            }
        }

        // Render Controller Front Overlay
        BlockHeatExchangerController block = (BlockHeatExchangerController) ModBlocks.heatExchangerController;
        IIcon frontOverlay = controller.isActive() ? block.getIconFrontActive() : block.getIconFrontFormed();
        
        if (frontOverlay != null) {
            int brightness = controller.getWorldObj()
                .getLightBrightnessForSkyBlocks(controller.xCoord, controller.yCoord, controller.zCoord, 0);
            tessellator.setBrightness(brightness);
            renderBlockOverlay(
                tessellator,
                controller.xCoord,
                controller.yCoord,
                controller.zCoord,
                controller,
                frontOverlay,
                true);
        }

        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
    }

    private void renderBlockOverlay(Tessellator tess, int hx, int hy, int hz,
        TileEntityHeatExchangerController controller, IIcon icon, boolean onlyFront) {
        if (icon == null) return;
        int cx = controller.xCoord;
        int cy = controller.yCoord;
        int cz = controller.zCoord;

        double dx = hx - cx;
        double dy = hy - cy;
        double dz = hz - cz;

        int frontSide = controller.getFacing();

        // Find outward face
        for (int s = 0; s < 6; s++) {
            if (onlyFront && s != frontSide) continue;

            ForgeDirection dir = ForgeDirection.getOrientation(s);
            int nx = hx + dir.offsetX;
            int ny = hy + dir.offsetY;
            int nz = hz + dir.offsetZ;

            if (!cn.icewindy.steamturbine.util.HeatExchangerValidator.isPartOfFormed(nx, ny, nz)) {
                renderFace(tess, icon, dx, dy, dz, s);
            }
        }
    }

    private void renderFace(Tessellator tessellator, IIcon icon, double x, double y, double z, int side) {
        float minU = icon.getMinU();
        float maxU = icon.getMaxU();
        float minV = icon.getMinV();
        float maxV = icon.getMaxV();
        double d = 0.505; // Slightly more offset to prevent z-fighting

        tessellator.startDrawingQuads();
        tessellator.setBrightness(240); // Force full brightness
        tessellator.setColorOpaque_F(1.0F, 1.0F, 1.0F); // Force white color
        switch (side) {
            case 0: // Down
                tessellator.setNormal(0.0F, -1.0F, 0.0F);
                tessellator.addVertexWithUV(x - 0.5, y - d, z - 0.5, minU, maxV);
                tessellator.addVertexWithUV(x + 0.5, y - d, z - 0.5, maxU, maxV);
                tessellator.addVertexWithUV(x + 0.5, y - d, z + 0.5, maxU, minV);
                tessellator.addVertexWithUV(x - 0.5, y - d, z + 0.5, minU, minV);
                break;
            case 1: // Up
                tessellator.setNormal(0.0F, 1.0F, 0.0F);
                tessellator.addVertexWithUV(x - 0.5, y + d, z + 0.5, minU, maxV);
                tessellator.addVertexWithUV(x + 0.5, y + d, z + 0.5, maxU, maxV);
                tessellator.addVertexWithUV(x + 0.5, y + d, z - 0.5, maxU, minV);
                tessellator.addVertexWithUV(x - 0.5, y + d, z - 0.5, minU, minV);
                break;
            case 2: // North (Z-)
                tessellator.setNormal(0.0F, 0.0F, -1.0F);
                tessellator.addVertexWithUV(x + 0.5, y - 0.5, z - d, minU, maxV);
                tessellator.addVertexWithUV(x - 0.5, y - 0.5, z - d, maxU, maxV);
                tessellator.addVertexWithUV(x - 0.5, y + 0.5, z - d, maxU, minV);
                tessellator.addVertexWithUV(x + 0.5, y + 0.5, z - d, minU, minV);
                break;
            case 3: // South (Z+)
                tessellator.setNormal(0.0F, 0.0F, 1.0F);
                tessellator.addVertexWithUV(x - 0.5, y - 0.5, z + d, minU, maxV);
                tessellator.addVertexWithUV(x + 0.5, y - 0.5, z + d, maxU, maxV);
                tessellator.addVertexWithUV(x + 0.5, y + 0.5, z + d, maxU, minV);
                tessellator.addVertexWithUV(x - 0.5, y + 0.5, z + d, minU, minV);
                break;
            case 4: // West (X-)
                tessellator.setNormal(-1.0F, 0.0F, 0.0F);
                tessellator.addVertexWithUV(x - d, y - 0.5, z - 0.5, minU, maxV);
                tessellator.addVertexWithUV(x - d, y - 0.5, z + 0.5, maxU, maxV);
                tessellator.addVertexWithUV(x - d, y + 0.5, z + 0.5, maxU, minV);
                tessellator.addVertexWithUV(x - d, y + 0.5, z - 0.5, minU, minV);
                break;
            case 5: // East (X+)
                tessellator.setNormal(1.0F, 0.0F, 0.0F);
                tessellator.addVertexWithUV(x + d, y - 0.5, z + 0.5, minU, maxV);
                tessellator.addVertexWithUV(x + d, y - 0.5, z - 0.5, maxU, maxV);
                tessellator.addVertexWithUV(x + d, y + 0.5, z - 0.5, maxU, minV);
                tessellator.addVertexWithUV(x + d, y + 0.5, z + 0.5, minU, minV);
                break;
        }
        tessellator.draw();
    }
}
