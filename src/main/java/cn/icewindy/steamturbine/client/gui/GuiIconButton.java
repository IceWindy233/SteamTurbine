package cn.icewindy.steamturbine.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

/**
 * 支持自定义纹理坐标的图标按钮。
 */
public class GuiIconButton extends GuiButton {

    private final ResourceLocation texture;
    private final int u;
    private final int v;

    public GuiIconButton(int id, int x, int y, int width, int height, String text, ResourceLocation texture, int u,
        int v) {
        super(id, x, y, width, height, text);
        this.texture = texture;
        this.u = u;
        this.v = v;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (this.visible) {
            mc.getTextureManager().bindTexture(texture);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            
            this.field_146123_n = mouseX >= this.xPosition && mouseY >= this.yPosition && mouseX < this.xPosition + this.width && mouseY < this.yPosition + this.height;
            int hoverState = this.getHoverState(this.field_146123_n);
            boolean isPressed = this.field_146123_n && org.lwjgl.input.Mouse.isButtonDown(0);

            // 贴图参数
            int texW = 200;
            int texH = 60;
            int vBase = v; 
            if (isPressed) vBase += 40;
            else if (hoverState == 2) vBase += 20;

            // 1. 核心填充区 (采样贴图核心 W3H3-W195H15)
            drawCustomRect(this.xPosition + 2, this.yPosition + 2, 3, vBase + 3, this.width - 4, this.height - 6, texW, texH);

            // 2. 上边框 (2px)
            drawCustomRect(this.xPosition, this.yPosition, 0, vBase, this.width, 2, texW, texH);

            // 3. 左侧边框 (2px, 全高)
            drawCustomRect(this.xPosition, this.yPosition, 0, vBase, 2, this.height, texW, texH);

            // 4. 右侧边框 (极其关键：必须从贴图最右侧 U198 采样)
            drawCustomRect(this.xPosition + this.width - 2, this.yPosition, 198, vBase, 2, this.height, texW, texH);

            /**
             * 3. 底部边框 (Normal 态用 5px 从 V15 开始采样，Hover/Pressed 用 4px 从 V16 开始)
             */
            int bottomH = (vBase % 20 == 0 && !isPressed && hoverState != 2) ? 5 : 4;
            int bottomV = vBase + (20 - bottomH);
            
            // 底部左角
            drawCustomRect(this.xPosition, this.yPosition + this.height - bottomH, 0, bottomV, 2, bottomH, texW, texH);
            // 底部右角
            drawCustomRect(this.xPosition + this.width - 2, this.yPosition + this.height - bottomH, 198, bottomV, 2, bottomH, texW, texH);
            // 底部中间拉伸
            drawCustomRect(this.xPosition + 2, this.yPosition + this.height - bottomH, 2, bottomV, this.width - 4, bottomH, texW, texH);

            // 绘制文字
            int color = 0xE0E0E0;
            if (!this.enabled) color = 0xA0A0A0;
            else if (this.field_146123_n) color = 0xFFFFA0;

            // 文本位移逻辑：基础向上偏移 2px，按下时相对于此位置向下移 1px
            int textY = this.yPosition + (this.height - 8) / 2 - 2;
            if (isPressed) {
                textY += 1;
            }

            this.drawCenteredString(mc.fontRenderer, this.displayString, this.xPosition + this.width / 2, textY, color);
        }
    }

    private void drawCustomRect(int x, int y, int u, int v, int w, int h, int texW, int texH) {
        float f = 1.0F / (float)texW;
        float f1 = 1.0F / (float)texH;
        net.minecraft.client.renderer.Tessellator tessellator = net.minecraft.client.renderer.Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.addVertexWithUV(x, y + h, this.zLevel, u * f, (v + h) * f1);
        tessellator.addVertexWithUV(x + w, y + h, this.zLevel, (u + w) * f, (v + h) * f1);
        tessellator.addVertexWithUV(x + w, y, this.zLevel, (u + w) * f, v * f1);
        tessellator.addVertexWithUV(x, y, this.zLevel, u * f, v * f1);
        tessellator.draw();
    }
}
