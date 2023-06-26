package cz.lisacek.dragonevent.utils;

import org.bukkit.Bukkit;

public class VersionHelper {
    private static final String NMS_VERSION = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
    private static final int MAJOR_VERSION_NUMBER;
    private static final boolean IS_PAPER;

    private VersionHelper() {
    }

    private static boolean checkPaper() {
        try {
            Class.forName("com.destroystokyo.paper.PaperConfig");
            return true;
        } catch (ClassNotFoundException var1) {
            return false;
        }
    }

    public static String getNMSVersion() {
        return NMS_VERSION;
    }

    public static int getMajorVersionNumber() {
        return MAJOR_VERSION_NUMBER;
    }

    public static boolean isPaper() {
        return IS_PAPER;
    }

    static {
        MAJOR_VERSION_NUMBER = Integer.parseInt(NMS_VERSION.split("_")[1]);
        IS_PAPER = checkPaper();
    }
}