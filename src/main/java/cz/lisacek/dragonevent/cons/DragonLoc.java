package cz.lisacek.dragonevent.cons;

import org.bukkit.Location;

public class DragonLoc {

    private final String name;
    private final Location location;

    public DragonLoc(String name, Location location) {
        this.name = name;
        this.location = location;
    }

    public String getName() {
        return name;
    }

    public Location getLocation() {
        return location;
    }
}
