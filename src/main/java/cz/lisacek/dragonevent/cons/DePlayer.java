package cz.lisacek.dragonevent.cons;

public class DePlayer {

    private final String name;

    private int kills = 0;

    private double damage = 0;

    private int votes = 0;
    private long lastVote = 0;

    public DePlayer(String name) {
        this.name = name;
    }

    public int getVotes() {
        return votes;
    }

    public String getName() {
        return name;
    }

    public int getKills() {
        return kills;
    }

    public double getDamage() {
        return damage;
    }

    public void setDamage(double damage) {
        this.damage = damage;
    }

    public void setKills(int kills) {
        this.kills = kills;
    }

    public void setVotes(int votes) {
        this.votes = votes;
    }

    public void setLastVote(long lastVote) {
        this.lastVote = lastVote;
    }

    @Override
    public String toString() {
        return "DePlayer{" +
                "name='" + name + '\'' +
                ", kills=" + kills +
                ", damage=" + damage +
                ", votes=" + votes +
                ", lastVote=" + lastVote +
                '}';
    }
}
