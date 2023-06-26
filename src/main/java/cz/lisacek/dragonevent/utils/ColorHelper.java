package cz.lisacek.dragonevent.utils;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.md_5.bungee.api.ChatColor;

public class ColorHelper {
    private static final List<Pattern> HEX_PATTERNS = Arrays.asList(Pattern.compile("<#([A-Fa-f0-9]){6}>"), Pattern.compile("\\[#([A-Fa-f0-9]){6}\\]"), Pattern.compile("\\{#([A-Fa-f0-9]){6}\\}"), Pattern.compile("\\(#([A-Fa-f0-9]){6}\\)"), Pattern.compile("&#([A-Fa-f0-9]){6}"), Pattern.compile("#([A-Fa-f0-9]){6}"));

    private ColorHelper() {
    }

    public static String colorize(String var0) {
        Iterator var1 = HEX_PATTERNS.iterator();

        while(var1.hasNext()) {
            Pattern var2 = (Pattern)var1.next();

            for(Matcher var3 = var2.matcher(var0); var3.find(); var3 = var2.matcher(var0)) {
                ChatColor var4 = getHexColorOrClosestLegacyColor(cleanHex(var3.group()));
                String var5 = var0.substring(0, var3.start());
                String var6 = var0.substring(var3.end());
                var0 = var5 + var4 + var6;
            }
        }

        var0 = ChatColor.translateAlternateColorCodes('&', var0);
        return var0;
    }

    public static List<String> colorize(List<String> var0) {
        ArrayList var1 = new ArrayList();
        Iterator var2 = var0.iterator();

        while(var2.hasNext()) {
            String var3 = (String)var2.next();
            var1.add(colorize(var3));
        }

        return var1;
    }

    private static ChatColor getHexColorOrClosestLegacyColor(String var0) {
        if (VersionHelper.getMajorVersionNumber() >= 16) {
            return ChatColor.of(var0);
        } else {
            int var1 = Integer.MAX_VALUE;
            ChatColor var2 = ChatColor.WHITE;
            Color var3 = Color.decode(var0);
            ColorHelper.ChatColorToHexMappings[] var4 = ColorHelper.ChatColorToHexMappings.values();
            int var5 = var4.length;

            for(int var6 = 0; var6 < var5; ++var6) {
                ColorHelper.ChatColorToHexMappings var7 = var4[var6];
                int var8 = var7.getRed() - var3.getRed();
                int var9 = var7.getGreen() - var3.getGreen();
                int var10 = var7.getBlue() - var3.getBlue();
                int var11 = var8 * var8 + var9 * var9 + var10 * var10;
                if (var11 < var1) {
                    var1 = var11;
                    var2 = var7.getChatColor();
                }
            }

            return var2;
        }
    }

    private static String cleanHex(String var0) {
        return var0.replaceAll("[<>\\[\\]\\{\\}\\(\\)&]", "");
    }

    private static enum ChatColorToHexMappings {
        BLACK(ChatColor.BLACK, 0),
        DARK_BLUE(ChatColor.DARK_BLUE, 170),
        DARK_GREEN(ChatColor.DARK_GREEN, 43520),
        DARK_AQUA(ChatColor.DARK_AQUA, 43690),
        DARK_RED(ChatColor.DARK_RED, 11141120),
        DARK_PURPLE(ChatColor.DARK_PURPLE, 11141290),
        GOLD(ChatColor.GOLD, 16755200),
        GRAY(ChatColor.GRAY, 11184810),
        DARK_GRAY(ChatColor.DARK_GRAY, 5592405),
        BLUE(ChatColor.BLUE, 5592575),
        GREEN(ChatColor.GREEN, 5635925),
        AQUA(ChatColor.AQUA, 5636095),
        RED(ChatColor.RED, 16733525),
        LIGHT_PURPLE(ChatColor.LIGHT_PURPLE, 16733695),
        YELLOW(ChatColor.YELLOW, 16777045),
        WHITE(ChatColor.WHITE, 16777215);

        private final ChatColor chatColor;
        private final int red;
        private final int green;
        private final int blue;

        private ChatColorToHexMappings(ChatColor var3, int var4) {
            this.chatColor = var3;
            this.red = var4 >> 16 & 255;
            this.green = var4 >> 8 & 255;
            this.blue = var4 & 255;
        }

        public ChatColor getChatColor() {
            return this.chatColor;
        }

        public int getRed() {
            return this.red;
        }

        public int getGreen() {
            return this.green;
        }

        public int getBlue() {
            return this.blue;
        }

        // $FF: synthetic method
        private static ColorHelper.ChatColorToHexMappings[] $values() {
            return new ColorHelper.ChatColorToHexMappings[]{BLACK, DARK_BLUE, DARK_GREEN, DARK_AQUA, DARK_RED, DARK_PURPLE, GOLD, GRAY, DARK_GRAY, BLUE, GREEN, AQUA, RED, LIGHT_PURPLE, YELLOW, WHITE};
        }
    }
}