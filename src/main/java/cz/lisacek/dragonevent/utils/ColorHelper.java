package cz.lisacek.dragonevent.utils;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.md_5.bungee.api.ChatColor;

public class ColorHelper {
    private static final List<Pattern> HEX_PATTERNS = Arrays.asList(
            Pattern.compile("<#([A-Fa-f0-9]){6}>"),
            Pattern.compile("\\[#([A-Fa-f0-9]){6}]"),
            Pattern.compile("\\{#([A-Fa-f0-9]){6}}"),
            Pattern.compile("\\(#([A-Fa-f0-9]){6}\\)"),
            Pattern.compile("&#([A-Fa-f0-9]){6}"),
            Pattern.compile("#([A-Fa-f0-9]){6}")
    );

    private ColorHelper() {
    }

    public static String colorize(String text) {

        for (Pattern pattern : HEX_PATTERNS) {
            Matcher matcher = pattern.matcher(text);

            for (; matcher.find(); matcher = pattern.matcher(text)) {
                ChatColor color = getHexColorOrClosestLegacyColor(cleanHex(matcher.group()));
                String beforeMatch = text.substring(0, matcher.start());
                String afterMatch = text.substring(matcher.end());
                text = beforeMatch + color + afterMatch;
            }
        }

        text = ChatColor.translateAlternateColorCodes('&', text);
        return text;
    }

    public static List<String> colorize(List<String> textList) {
        List<String> colorizedList = new ArrayList<>();

        for (String text : textList) {
            colorizedList.add(colorize(text));
        }

        return colorizedList;
    }

    private static ChatColor getHexColorOrClosestLegacyColor(String hex) {
        if (VersionHelper.getMajorVersionNumber() >= 16) {
            return ChatColor.of(hex);
        } else {
            int minSquaredError = Integer.MAX_VALUE;
            ChatColor closestColor = ChatColor.WHITE;
            Color targetColor = Color.decode(hex);
            ChatColorToHexMappings[] mappings = ChatColorToHexMappings.values();

            for (ChatColorToHexMappings mapping : mappings) {
                int redDiff = mapping.getRed() - targetColor.getRed();
                int greenDiff = mapping.getGreen() - targetColor.getGreen();
                int blueDiff = mapping.getBlue() - targetColor.getBlue();
                int squaredError = redDiff * redDiff + greenDiff * greenDiff + blueDiff * blueDiff;

                if (squaredError < minSquaredError) {
                    minSquaredError = squaredError;
                    closestColor = mapping.getChatColor();
                }
            }

            return closestColor;
        }
    }

    private static String cleanHex(String hex) {
        return hex.replaceAll("[<>\\[\\]{}()&]", "");
    }

    private enum ChatColorToHexMappings {
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

        ChatColorToHexMappings(ChatColor chatColor, int hex) {
            this.chatColor = chatColor;
            this.red = (hex >> 16) & 255;
            this.green = (hex >> 8) & 255;
            this.blue = hex & 255;
        }

        public ChatColor getChatColor() {
            return chatColor;
        }

        public int getRed() {
            return red;
        }

        public int getGreen() {
            return green;
        }

        public int getBlue() {
            return blue;
        }
    }
}