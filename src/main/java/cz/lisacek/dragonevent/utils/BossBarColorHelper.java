package cz.lisacek.dragonevent.utils;

import java.util.ArrayList;

public class BossBarColorHelper {
    private BossBarColorHelper() {
    }

    private static ArrayList<ColorName> initColorList() {
        ArrayList<ColorName> colorList = new ArrayList<>();
        colorList.add(new ColorName("BLUE", 0, 183, 236));
        colorList.add(new ColorName("GREEN", 29, 236, 0));
        colorList.add(new ColorName("PINK", 236, 0, 184));
        colorList.add(new ColorName("PURPLE", 123, 0, 236));
        colorList.add(new ColorName("RED", 236, 53, 0));
        colorList.add(new ColorName("WHITE", 236, 236, 236));
        colorList.add(new ColorName("YELLOW", 233, 236, 0));
        return colorList;
    }

    public static String getColorNameFromRgb(int red, int green, int blue) {
        ArrayList<ColorName> colorList = initColorList();
        ColorName matchedColor = null;
        int minSquaredError = Integer.MAX_VALUE;

        for (ColorName color : colorList) {
            int squaredError = color.computeMSE(red, green, blue);
            if (squaredError < minSquaredError) {
                minSquaredError = squaredError;
                matchedColor = color;
            }
        }

        if (matchedColor != null) {
            return matchedColor.getName();
        } else {
            return "No matched color name.";
        }
    }

    public static class ColorName {
        private final int r;
        private final int g;
        private final int b;
        private final String name;

        public ColorName(String name, int r, int g, int b) {
            this.r = r;
            this.g = g;
            this.b = b;
            this.name = name;
        }

        public int computeMSE(int red, int green, int blue) {
            return ((red - this.r) * (red - this.r) + (green - this.g) * (green - this.g) + (blue - this.b) * (blue - this.b)) / 3;
        }

        public String getName() {
            return name;
        }
    }
}