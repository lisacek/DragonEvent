package cz.lisacek.dragonevent.managers;

import cz.lisacek.dragonevent.DragonEvent;
import cz.lisacek.dragonevent.cons.DePlayer;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.DecimalFormat;

public class PlaceholderManager extends PlaceholderExpansion {

    private final DecimalFormat DF = new DecimalFormat("#.##");

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public @NotNull
    String getIdentifier() {
        return "de";
    }

    @Override
    public @NotNull
    String getAuthor() {
        return "LISACEK";
    }

    @Override
    public @NotNull
    String getVersion() {
        return "1.0.0";
    }

    /**
     * This is the method called when a placeholder with our identifier
     * is found and needs a value.
     * <br>We specify the value identifier in this method.
     * <br>Since version 2.9.1 can you use OfflinePlayers in your requests.
     *
     * @param player     A {@link org.bukkit.OfflinePlayer OfflinePlayer}.
     * @param identifier A String containing the identifier/value.
     * @return Possibly-null String of the requested identifier.
     */
    @Override
    public String onRequest(OfflinePlayer player, @NotNull String identifier) {
        return getPlaceholders(player, identifier);
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        return getPlaceholders(player, params);
    }

    @Nullable
    private String getPlaceholders(OfflinePlayer player, String identifier) {
        String[] args = identifier.split("_");
        DePlayer dePlayer = EventManager.getINSTANCE().getDePlayer(player.getName());
        if (args.length > 0) {
            switch (args[0]) {
                case "kills":
                    return getValue(dePlayer, "kills");
                case "damage":
                    return getValue(dePlayer, "damage");
                case "votes":
                    return getValue(dePlayer, "votes");
                case "needed":
                    return getVotesNeeded();
                case "current":
                    return getCurrentVotes();
                case "top":
                    int position = Integer.parseInt(args[2]);
                    boolean isPlayer = args[3].equalsIgnoreCase("name");
                    return getTopValue(args[1], position, isPlayer);
            }
        }
        return null;
    }

    private String getValue(DePlayer dePlayer, String field) {
        if (dePlayer != null) {
            switch (field) {
                case "kills":
                    return String.valueOf(dePlayer.getKills());
                case "damage":
                    return DF.format(dePlayer.getDamage());
                case "votes":
                    return String.valueOf(dePlayer.getVotes());
            }
        }
        return null;
    }

    private String getVotesNeeded() {
        return String.valueOf(VoteManager.getINSTANCE().getVotesNeeded());
    }

    private String getCurrentVotes() {
        return String.valueOf(VoteManager.getINSTANCE().getVotes());
    }

    private String getTopValue(String field, int position, boolean isPlayer) {
        switch (field) {
            case "kills":
                return isPlayer ? DragonEvent.getInstance().getTopKills(position).getKey() : String.valueOf(DragonEvent.getInstance().getTopKills(position).getValue());
            case "damage":
                return isPlayer ? DragonEvent.getInstance().getTopDamage(position).getKey() : DF.format(DragonEvent.getInstance().getTopDamage(position).getValue());
            case "votes":
                return isPlayer ? DragonEvent.getInstance().getTopVotes(position).getKey() : String.valueOf(DragonEvent.getInstance().getTopVotes(position).getValue());
        }
        return null;
    }

}