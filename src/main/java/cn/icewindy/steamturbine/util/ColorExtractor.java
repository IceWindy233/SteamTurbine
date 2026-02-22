package cn.icewindy.steamturbine.util;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.List;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.oredict.OreDictionary;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class ColorExtractor {

    @SideOnly(Side.CLIENT)
    public static int getAverageColor(String materialName) {
        try {
            String[] prefixes = { "ingot", "gem", "", "dust" };
            List<ItemStack> ores = null;
            for (String prefix : prefixes) {
                ores = OreDictionary.getOres(prefix + materialName);
                if (ores != null && !ores.isEmpty()) break;
            }
            if (ores == null || ores.isEmpty()) return 0xFFFFFF;

            ItemStack stack = ores.get(0);
            if (stack == null || stack.getItem() == null) return 0xFFFFFF;

            // In 1.7.10, getting color from texture is best done by reading the resource directly
            // We need to resolve the icon to a resource path
            String iconName = stack.getItem()
                .getIconFromDamage(stack.getItemDamage())
                .getIconName();
            String domain = "minecraft";
            String path = iconName;

            if (iconName.contains(":")) {
                String[] parts = iconName.split(":");
                domain = parts[0];
                path = parts[1];
            }

            ResourceLocation res = new ResourceLocation(domain, "textures/items/" + path + ".png");
            InputStream is = Minecraft.getMinecraft()
                .getResourceManager()
                .getResource(res)
                .getInputStream();
            BufferedImage image = ImageIO.read(is);
            is.close();

            if (image == null) return 0xFFFFFF;

            long r = 0, g = 0, b = 0, count = 0;
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int argb = image.getRGB(x, y);
                    int alpha = (argb >> 24) & 0xFF;
                    if (alpha > 100) { // Only count non-transparent pixels
                        r += (argb >> 16) & 0xFF;
                        g += (argb >> 8) & 0xFF;
                        b += argb & 0xFF;
                        count++;
                    }
                }
            }

            if (count == 0) return 0xFFFFFF;

            int avgR = (int) (r / count);
            int avgG = (int) (g / count);
            int avgB = (int) (b / count);

            // 提亮方案：将提取出的颜色与白色(255)按比例混合
            // 改为 80% 原始颜色 + 20% 白色，找到明亮与浓郁之间的平衡点
            avgR = (int) (avgR * 0.8 + 255 * 0.2);
            avgG = (int) (avgG * 0.8 + 255 * 0.2);
            avgB = (int) (avgB * 0.8 + 255 * 0.2);

            return (avgR << 16) | (avgG << 8) | avgB;

        } catch (Exception e) {
            // e.printStackTrace();
            return 0xFFFFFF;
        }
    }
}
