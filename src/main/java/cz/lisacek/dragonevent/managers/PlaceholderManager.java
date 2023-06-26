package cz.lisacek.dragonevent.managers;

import cz.lisacek.dragonevent.DragonEvent;
import cz.lisacek.dragonevent.cons.DePlayer;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlaceholderManager extends PlaceholderExpansion {

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
                    if (dePlayer != null) {
                        return String.valueOf(dePlayer.getKills());
                    }
                    break;
                case "damage":
                    if (dePlayer != null) {
                        return String.valueOf(dePlayer.getDamage());
                    }
                    break;
                case "votes":
                    if (dePlayer != null) {
                        return String.valueOf(dePlayer.getVotes());
                    }
                    break;
                case "votesNeeded":
                    return String.valueOf(VoteManager.getINSTANCE().getVotesNeeded());
                case "currentVotes":
                    return String.valueOf(VoteManager.getINSTANCE().getVotes());
                case "top":
                    int position = Integer.parseInt(args[2]);
                    boolean isPlayer = args[3].equalsIgnoreCase("name");
                    switch (args[1]) {
                        case "kills":
                            return isPlayer ? DragonEvent.getInstance().getTopKills(position).getKey() : String.valueOf(DragonEvent.getInstance().getTopKills(position).getValue());
                        case "damage":
                            return isPlayer ? DragonEvent.getInstance().getTopDamage(position).getKey() : String.valueOf(DragonEvent.getInstance().getTopDamage(position).getValue());
                        case "votes":
                            return isPlayer ? DragonEvent.getInstance().getTopVotes(position).getKey() : String.valueOf(DragonEvent.getInstance().getTopVotes(position).getValue());
                    }
            }
        }
        return null;
    }

}