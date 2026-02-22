package cn.icewindy.steamturbine.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import cpw.mods.fml.common.registry.GameRegistry;

public class ItemParser {

    private static final Pattern STACK_PATTERN = Pattern.compile("<([^:]+):([^:]+):(\\d+)>");

    public static ItemStack parseStack(String input) {
        if (input == null || !input.startsWith("<") || !input.endsWith(">")) {
            return null;
        }

        Matcher matcher = STACK_PATTERN.matcher(input);
        if (matcher.matches()) {
            String modId = matcher.group(1);
            String itemName = matcher.group(2);
            int meta = Integer.parseInt(matcher.group(3));

            Item item = GameRegistry.findItem(modId, itemName);
            if (item != null) {
                return new ItemStack(item, 1, meta);
            }
        }
        return null;
    }

    public static boolean isStackFormat(String input) {
        return input != null && input.startsWith("<") && input.endsWith(">");
    }
}
