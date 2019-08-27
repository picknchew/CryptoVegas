package cc.cryptovegas.core.scoreboard;

import cc.cryptovegas.core.util.PlayerUtil;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SimpleScoreboard {
    private static final Map<ChatColor, OfflinePlayer> cache = new HashMap<>();

    private final Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();

    private String title;

    private final Map<String, Integer> scores = new ConcurrentHashMap<>();
    private final List<Team> teams = Collections.synchronizedList(Lists.newArrayList());
    private final List<Integer> removed = Lists.newArrayList();
    private final Set<String> updated = Collections.synchronizedSet(new HashSet<>());

    private Objective obj;

    public SimpleScoreboard(String title) {
        this.title = ChatColor.translateAlternateColorCodes('&', title);
    }

    public void add(String text, Integer score) {
        text = ChatColor.translateAlternateColorCodes('&', text);

        if (remove(score, text, false) || !scores.containsValue(score)) {
            updated.add(text);
        }

        scores.put(text, score);
    }

    public boolean remove(Integer score, String text) {
        return remove(score, text, true);
    }

    public boolean remove(Integer score, String n, boolean b) {
        String toRemove = get(score, n);

        if (toRemove == null) {
            return false;
        }

        scores.remove(toRemove);

        if (b) {
            removed.add(score);
        }

        return true;
    }

    public String get(int score, String n) {
        String str = null;

        for (Map.Entry<String, Integer> entry : scores.entrySet()) {
            if (entry.getValue().equals(score) && !entry.getKey().equals(n)) {
                str = entry.getKey();
            }
        }

        return str;
    }

    private Map.Entry<Team, OfflinePlayer> createTeam(String text, int pos) {
        Team team;
        ChatColor color = ChatColor.values()[pos];
        OfflinePlayer result;

        if (!cache.containsKey(color))
            cache.put(color, PlayerUtil.getOfflinePlayerSkipLookup(color.toString()));

        result = cache.get(color);

        try {
            team = scoreboard.registerNewTeam("text-" + (teams.size() + 1));
        } catch (IllegalArgumentException e) {
            team = scoreboard.getTeam("text-" + (teams.size()));
        }

        applyText(team, text, result);

        teams.add(team);
        return new AbstractMap.SimpleEntry<>(team, result);
    }

    private void applyText(Team team, String text, OfflinePlayer result) {
        Iterator<String> iterator = Splitter.fixedLength(16).split(text).iterator();
        String prefix = iterator.next();

        team.setPrefix(prefix);

        if (!team.hasPlayer(result)) {
            team.addPlayer(result);
        }

        if (text.length() > 16) {
            String prefixColor = ChatColor.getLastColors(prefix);
            String suffix = iterator.next();

            if (prefix.endsWith(String.valueOf(ChatColor.COLOR_CHAR))) {
                prefix = prefix.substring(0, prefix.length() - 1);
                team.setPrefix(prefix);
                prefixColor = ChatColor.getByChar(suffix.charAt(0)).toString();
                suffix = suffix.substring(1);
            }

            if (prefixColor == null)
                prefixColor = "";

            if (suffix.length() > 16) {
                suffix = suffix.substring(0, (13 - prefixColor.length())); // cut off suffix, done if text is over 30 characters
            }

            team.setSuffix((prefixColor.equals("") ? ChatColor.RESET : prefixColor) + suffix);
        }
    }

    public void update() {
        if (updated.isEmpty()) {
            return;
        }

        if (obj == null) {
            obj = scoreboard.registerNewObjective((title.length() > 16 ? title.substring(0, 15) : title), "dummy");
            obj.setDisplayName(title);
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        }

        removed.forEach(remove -> {
            for (String entry : scoreboard.getEntries()) {
                Score score = obj.getScore(entry);

                if (score == null || score.getScore() != remove) {
                    continue;
                }

                scoreboard.resetScores(entry);
            }
        });

        removed.clear();

        int index = scores.size();

        for (Map.Entry<String, Integer> text : scores.entrySet()) {
            Team t = scoreboard.getTeam(ChatColor.values()[text.getValue()].toString());
            Map.Entry<Team, OfflinePlayer> team;

            if (!updated.contains(text.getKey())) {
                continue;
            }

            if (t != null) {
                ChatColor color = ChatColor.values()[text.getValue()];

                if (!cache.containsKey(color)) {
                    cache.put(color, PlayerUtil.getOfflinePlayerSkipLookup(color.toString()));
                }

                team = new AbstractMap.SimpleEntry<>(t, cache.get(color));
                applyText(team.getKey(), text.getKey(), team.getValue());
                index -= 1;

                continue;
            }

            team = createTeam(text.getKey(), text.getValue());

            int score = text.getValue() != null ? text.getValue() : index;

            obj.getScore(team.getValue()).setScore(score);
            index -= 1;
        }

        updated.clear();
    }

    public void setTitle(String title) {
        this.title = ChatColor.translateAlternateColorCodes('&', title);

        if (obj != null) {
            obj.setDisplayName(title);
        }
    }

    public void reset() {
        teams.forEach(Team::unregister);
        teams.clear();
        scores.clear();
    }

    public Scoreboard getScoreboard() {
        return scoreboard;
    }

    public void send(Player... players) {
        for (Player player : players) {
            player.setScoreboard(scoreboard);
        }
    }
}