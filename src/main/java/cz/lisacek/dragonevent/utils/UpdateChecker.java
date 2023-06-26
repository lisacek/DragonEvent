package cz.lisacek.dragonevent.utils;

import cz.lisacek.dragonevent.DragonEvent;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

public class UpdateChecker implements Runnable {
    private static final String RESOURCE_ID = "59318";
    private String latestAvailableVersion;

    @Override
    public void run() {
        if (DragonEvent.getInstance().getConfig().getBoolean("update-notify")) {
            HttpsURLConnection connection = null;

            try {
                URL url = new URL("https://api.spigotmc.org/legacy/update.php?resource=" + RESOURCE_ID);
                connection = (HttpsURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                latestAvailableVersion = reader.readLine();
            } catch (Exception e) {
                // Handle exception (log or throw)
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }

            if (isUpdateAvailable()) {
                Console.info("&7New version of DragonEvent is available! &8(&d" + latestAvailableVersion + "&8)");
            } else {
                Console.info("&7You are using the latest version of DragonEvent.");
            }
        }
    }

    private boolean isUpdateAvailable() {
        String currentVersion = DragonEvent.getInstance().getDescription().getVersion();
        return !latestAvailableVersion.equalsIgnoreCase(currentVersion);
    }
}