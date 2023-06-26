package cz.lisacek.dragonevent.utils;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;

public class BossBarColorHelper {
    private BossBarColorHelper() {
    }

    private static ArrayList<BossBarColorHelper.ColorName> initColorList() {
        ArrayList var0 = new ArrayList();
        var0.add(new BossBarColorHelper.ColorName("BLUE", 0, 183, 236));
        var0.add(new BossBarColorHelper.ColorName("GREEN", 29, 236, 0));
        var0.add(new BossBarColorHelper.ColorName("PINK", 236, 0, 184));
        var0.add(new BossBarColorHelper.ColorName("PURPLE", 123, 0, 236));
        var0.add(new BossBarColorHelper.ColorName("RED", 236, 53, 0));
        var0.add(new BossBarColorHelper.ColorName("WHITE", 236, 236, 236));
        var0.add(new BossBarColorHelper.ColorName("YELLOW", 233, 236, 0));
        return var0;
    }

    public static String getColorNameFromRgb(int var0, int var1, int var2) {
        ArrayList var3 = initColorList();
        BossBarColorHelper.ColorName var4 = null;
        int var5 = Integer.MAX_VALUE;
        Iterator var7 = var3.iterator();

        while(var7.hasNext()) {
            BossBarColorHelper.ColorName var8 = (BossBarColorHelper.ColorName)var7.next();
            int var6 = var8.computeMSE(var0, var1, var2);
            if (var6 < var5) {
                var5 = var6;
                var4 = var8;
            }
        }

        if (var4 != null) {
            return var4.getName();
        } else {
            return "No matched color name.";
        }
    }

    public static String getColorNameFromHex(int var0) {
        int var1 = (var0 & 16711680) >> 16;
        int var2 = (var0 & '\uff00') >> 8;
        int var3 = var0 & 255;
        return getColorNameFromRgb(var1, var2, var3);
    }

    public static int colorToHex(Color var0) {
        return Integer.decode("0x" + Integer.toHexString(var0.getRGB()).substring(2));
    }

    public static String getColorNameFromColor(Color var0) {
        return getColorNameFromRgb(var0.getRed(), var0.getGreen(), var0.getBlue());
    }

    public static class ColorName {
        public int r;
        public int g;
        public int b;
        public String name;

        public ColorName(String var1, int var2, int var3, int var4) {
            this.r = var2;
            this.g = var3;
            this.b = var4;
            this.name = var1;
        }

        public int computeMSE(int var1, int var2, int var3) {
            return ((var1 - this.r) * (var1 - this.r) + (var2 - this.g) * (var2 - this.g) + (var3 - this.b) * (var3 - this.b)) / 3;
        }

        public int getR() {
            return this.r;
        }

        public int getG() {
            return this.g;
        }

        public int getB() {
            return this.b;
        }

        public String getName() {
            return this.name;
        }
    }
}
