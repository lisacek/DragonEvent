package cz.lisacek.dragonevent.utils;

import cz.lisacek.dragonevent.DragonEvent;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;
import java.nio.file.Files;

public class UpdateConfig {

    // we need to add and set new sections etc
    public static void run() {
        // Original
        YamlConfiguration config = DragonEvent.getInstance().getConfig();
        // Get from resources
        InputStream defaultConfig = DragonEvent.getInstance().getResource("config.yml");
        // Use temp file or reader
        assert defaultConfig != null;
        File tempFile = createTempFile(defaultConfig);
        updateConfiguration(config, tempFile);
    }

    private static File createTempFile(InputStream inputStream) {
        try {
            File tempFile = File.createTempFile("temp", ".yml");
            tempFile.deleteOnExit();

            try (OutputStream outputStream = Files.newOutputStream(tempFile.toPath())) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }

            return tempFile;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static void updateConfiguration(YamlConfiguration config, File tempFile) {
        try {
            int updated = 0;
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(tempFile);
            for (String key : defaultConfig.getKeys(true)) {
                if (!config.contains(key)) {
                    config.set(key, defaultConfig.get(key));
                    updated++;
                }
            }
            if (updated > 0) {
                Console.info("&cConfig was outdated.");
                Console.info("&7Updated config with &a" + updated + " &7new values.");
                config.save(DragonEvent.getInstance().getDataFolder() + File.separator + "config.yml");
            } else {
                Console.info("&7Config is up to date.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}