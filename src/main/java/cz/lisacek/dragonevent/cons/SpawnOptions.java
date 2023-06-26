package cz.lisacek.dragonevent.cons;


import java.util.List;

public class SpawnOptions {

    private final boolean isEverywhere;
    private final boolean isRandomLocation;

    private final double hp;

    private final DragonLoc dragonLoc;

    private final List<DragonLoc> dragonLocList;
    private final boolean isMoving;
    private final boolean isGlowing;
    private final boolean announceSpawn;


    public boolean isEverywhere() {
        return isEverywhere;
    }

    public boolean isRandomLocation() {
        return isRandomLocation;
    }

    public DragonLoc getDragonLoc() {
        return dragonLoc;
    }

    public double getHp() {
        return hp;
    }

    public boolean isAnnounceSpawn() {
        return announceSpawn;
    }

    public List<DragonLoc> getDragonLocList() {
        return dragonLocList;
    }

    public boolean isGlowing() {
        return isGlowing;
    }

    public boolean isMoving() {
        return isMoving;
    }

    private SpawnOptions(SpawnOptionsBuilder builder) {
        this.isEverywhere = builder.isEverywhere;
        this.isRandomLocation = builder.isRandomLocation;
        this.hp = builder.hp;
        this.dragonLoc = builder.dragonLoc;
        this.dragonLocList = builder.dragonLocList;
        this.isMoving = builder.isMoving;
        this.isGlowing = builder.isGlowing;
        this.announceSpawn = builder.announceSpawn;
    }

    //Builder Class
    public static class SpawnOptionsBuilder {

        private boolean isEverywhere;
        private boolean isRandomLocation;

        private double hp;

        private DragonLoc dragonLoc;

        private List<DragonLoc> dragonLocList;
        private boolean isMoving;
        private boolean isGlowing;
        private boolean announceSpawn;


        public SpawnOptionsBuilder setEverywhere(boolean isEverywhere) {
            this.isEverywhere = isEverywhere;
            return this;
        }

        public SpawnOptionsBuilder setRandomLocation(boolean isRandomLocation) {
            this.isRandomLocation = isRandomLocation;
            return this;
        }

        public SpawnOptionsBuilder setHp(double hp) {
            this.hp = hp;
            return this;
        }

        public SpawnOptionsBuilder setDragonLoc(DragonLoc dragonLoc) {
            this.dragonLoc = dragonLoc;
            return this;
        }

        public SpawnOptionsBuilder setDragonLocList(List<DragonLoc> dragonLocList) {
            this.dragonLocList = dragonLocList;
            return this;
        }

        public SpawnOptionsBuilder setMoving(boolean isMoving) {
            this.isMoving = isMoving;
            return this;
        }

        public SpawnOptionsBuilder setGlowing(boolean isGlowing) {
            this.isGlowing = isGlowing;
            return this;
        }

        public SpawnOptionsBuilder setAnnounceSpawn(boolean announceSpawn) {
            this.announceSpawn = announceSpawn;
            return this;
        }

        public SpawnOptions build() {
            return new SpawnOptions(this);
        }
    }
}