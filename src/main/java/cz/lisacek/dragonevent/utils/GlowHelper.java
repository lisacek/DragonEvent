package cz.lisacek.dragonevent.utils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class GlowHelper {
    private static final Random RANDOM = new Random();
    private static final String SCOREBOARD_TEAM_NAME = "de_glow_%entity_id%";
    private static final Scoreboard SCOREBOARD = Bukkit.getScoreboardManager().getMainScoreboard();
    private static Map<Integer, Team> teams = new HashMap();

    private GlowHelper() {
    }

    public static void unregisterTeams() {
        Iterator var0 = teams.entrySet().iterator();

        while(var0.hasNext()) {
            Entry var1 = (Entry)var0.next();

            try {
                ((Team)var1.getValue()).unregister();
            } catch (Exception var3) {
            }
        }

        teams.clear();
    }

    public static void unregisterTeam(Entity var0) {
        Team var1 = SCOREBOARD.getTeam("pp_glow_%entity_id%".replace("%entity_id%", Integer.toString(var0.getEntityId())));
        if (var1 != null) {
            var1.unregister();
        }

        teams.remove(var0.getEntityId());
    }

    public static void setGlowing(Entity var0, ChatColor var1) {
        String var2 = "pp_glow_%entity_id%".replace("%entity_id%", Integer.toString(var0.getEntityId()));
        if (SCOREBOARD.getTeam(var2) == null) {
            teams.put(var0.getEntityId(), SCOREBOARD.registerNewTeam(var2));
        }

        Team var3 = (Team)teams.get(var0.getEntityId());
        if (!var0.isGlowing()) {
            var0.setGlowing(true);
        }

        if (!var3.getEntries().contains(var0.getUniqueId().toString())) {
            var3.addEntry(var0.getUniqueId().toString());
        }

        if (var1 != null) {
            var3.setColor(var1);
        } else {
            LinkedList var4 = new LinkedList(Arrays.asList(ChatColor.values()));
            var4.removeAll(Arrays.asList(var3.getColor(), ChatColor.BOLD, ChatColor.ITALIC, ChatColor.MAGIC, ChatColor.RESET, ChatColor.STRIKETHROUGH, ChatColor.UNDERLINE));
            var3.setColor((ChatColor)var4.get(RANDOM.nextInt(var4.size())));
        }
    }
}
