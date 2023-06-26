package cz.lisacek.dragonevent.utils;

import org.bukkit.Bukkit;

public class Console {

    public static void info(String message) {
        Bukkit.getConsoleSender().sendMessage(ColorHelper.colorize("&dDragonEvent &8&l| &r" + message));
    }

}
