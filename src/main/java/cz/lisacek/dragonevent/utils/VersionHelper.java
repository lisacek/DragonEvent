package cz.lisacek.dragonevent.utils;

import org.bukkit.Bukkit;

public class VersionHelper {
    private static final String NMS_VERSION = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
    private static final int MAJOR_VERSION_NUMBER;

    private VersionHelper() {
    }

    public static int getMajorVersionNumber() {
        return MAJOR_VERSION_NUMBER;
    }

    static {
        MAJOR_VERSION_NUMBER = Integer.parseInt(NMS_VERSION.split("_")[1]);
    }
}