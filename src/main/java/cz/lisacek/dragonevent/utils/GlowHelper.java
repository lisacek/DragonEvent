package cz.lisacek.dragonevent.utils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.*;

public class GlowHelper {
    private static final Random RANDOM = new Random();
    private static final String TEAM_NAME_FORMAT = "de_glow_%entity_id%";
    private static final Scoreboard SCOREBOARD = Bukkit.getScoreboardManager().getMainScoreboard();
    private static Map<Integer, Team> teams = new HashMap<>();

    private GlowHelper() {
    }

    public static void unregisterTeams() {
        for (Team team : teams.values()) {
            try {
                team.unregister();
            } catch (Exception ignored) {
            }
        }

        teams.clear();
    }

    public static void unregisterTeam(Entity entity) {
        int entityId = entity.getEntityId();
        String teamName = TEAM_NAME_FORMAT.replace("%entity_id%", Integer.toString(entityId));
        Team team = SCOREBOARD.getTeam(teamName);
        if (team != null) {
            team.unregister();
        }

        teams.remove(entityId);
    }

    public static void setGlowing(Entity entity, ChatColor color) {
        int entityId = entity.getEntityId();
        String teamName = TEAM_NAME_FORMAT.replace("%entity_id%", Integer.toString(entityId));
        Team team = SCOREBOARD.getTeam(teamName);
        if (team == null) {
            team = SCOREBOARD.registerNewTeam(teamName);
            teams.put(entityId, team);
        }

        if (!entity.isGlowing()) {
            entity.setGlowing(true);
        }

        String entry = entity.getUniqueId().toString();
        if (!team.getEntries().contains(entry)) {
            team.addEntry(entry);
        }

        if (color != null) {
            team.setColor(color);
        } else {
            LinkedList<ChatColor> availableColors = new LinkedList<>(Arrays.asList(ChatColor.values()));
            availableColors.removeAll(Arrays.asList(
                    team.getColor(),
                    ChatColor.BOLD,
                    ChatColor.ITALIC,
                    ChatColor.MAGIC,
                    ChatColor.RESET,
                    ChatColor.STRIKETHROUGH,
                    ChatColor.UNDERLINE,
                    ChatColor.BLACK,
                    ChatColor.WHITE,
                    ChatColor.GRAY,
                    ChatColor.DARK_GRAY
            ));
            team.setColor(availableColors.get(RANDOM.nextInt(availableColors.size())));
        }
    }
}
