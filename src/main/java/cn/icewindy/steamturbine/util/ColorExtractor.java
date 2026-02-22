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
    public static int extractColorFromStack(ItemStack stack) {
        if (stack == null || stack.getItem() == null) return 0xFFFFFF;

        // 1. Try to get color from item's own tinting method
        try {
            int tint = stack.getItem()
                .getColorFromItemStack(stack, 0);
            if (tint != 0xFFFFFF && tint != 0) {
                return tint;
            }
        } catch (Exception ignored) {}

        // 2. Try to get color from texture
        try {
            net.minecraft.util.IIcon icon = stack.getItem()
                .getIconFromDamage(stack.getItemDamage());
            if (icon == null) return 0xFFFFFF;
            String iconName = icon.getIconName();
            return extractColorFromIcon(iconName);
        } catch (Exception e) {
            return 0xFFFFFF;
        }
    }

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
            return extractColorFromStack(stack);
        } catch (Exception e) {
            return 0xFFFFFF;
        }
    }

    @SideOnly(Side.CLIENT)
    private static int extractColorFromIcon(String iconName) {
        if (iconName == null || iconName.isEmpty()) return 0xFFFFFF;

        String domain = "minecraft";
        String path = iconName;

        if (iconName.contains(":")) {
            String[] parts = iconName.split(":");
            if (parts.length > 1) {
                domain = parts[0];
                path = parts[1];
            }
        }

        // Common texture paths in 1.7.10
        String[] possiblePaths = { "textures/items/" + path + ".png", "textures/blocks/" + path + ".png",
            "textures/" + path + ".png", path + ".png" };

        for (String fullPath : possiblePaths) {
            try {
                ResourceLocation res = new ResourceLocation(domain, fullPath);
                InputStream is = Minecraft.getMinecraft()
                    .getResourceManager()
                    .getResource(res)
                    .getInputStream();
                if (is != null) {
                    int color = analyzeInputStream(is);
                    is.close();
                    if (color != 0xFFFFFF) return color;
                }
            } catch (Exception ignored) {}
        }

        return 0xFFFFFF;
    }

    @SideOnly(Side.CLIENT)
    private static int analyzeInputStream(InputStream is) throws Exception {
        BufferedImage image = ImageIO.read(is);
        if (image == null) return 0xFFFFFF;

        long r = 0, g = 0, b = 0, count = 0;
        int width = image.getWidth();
        int height = image.getHeight();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = image.getRGB(x, y);
                int alpha = (argb >> 24) & 0xFF;
                if (alpha > 120) { // Higher alpha threshold to ignore glow/edge effects
                    int pr = (argb >> 16) & 0xFF;
                    int pg = (argb >> 8) & 0xFF;
                    int pb = argb & 0xFF;

                    // Ignore near-white pixels which are usually highlights
                    if (pr > 240 && pg > 240 && pb > 240) continue;
                    // Ignore near-black pixels which are usually shadows
                    if (pr < 20 && pg < 20 && pb < 20) continue;

                    r += pr;
                    g += pg;
                    b += pb;
                    count++;
                }
            }
        }

        if (count == 0) return 0xFFFFFF;

        int avgR = (int) (r / count);
        int avgG = (int) (g / count);
        int avgB = (int) (b / count);

        // Subtly brighten (mixed with 10% white instead of 20%)
        avgR = (int) (avgR * 0.9 + 255 * 0.1);
        avgG = (int) (avgG * 0.9 + 255 * 0.1);
        avgB = (int) (avgB * 0.9 + 255 * 0.1);

        return (avgR << 16) | (avgG << 8) | avgB;
    }
}
