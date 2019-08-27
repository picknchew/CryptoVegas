package cc.cryptovegas.core.scoreboard;

import org.bukkit.ChatColor;

import java.util.*;

public class ScoreboardTemplate {
    private final List<String> lines;
    private final Replacer replacer;

    public ScoreboardTemplate(List<String> lines) {
        this.lines = lines;
        this.replacer = new Replacer();

        // reverse order so that it is iterated in reverse order
        // so scores are in the correct order to retain line position on scoreboard.
        Collections.reverse(lines);
    }

    public class Replacer {
        private final Map<String, String> placeholders = new HashMap<>();

        public Replacer with(String placeholder, Object value) {
            placeholders.put("{{" + placeholder + "}}", value.toString());
            return this;
        }

        public List<String> build() {
            List<String> formattedLines = new ArrayList<>();

            for (String line : lines) {
                for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                    line = line.replace(entry.getKey(), entry.getValue());
                }

                formattedLines.add(ChatColor.translateAlternateColorCodes('&', line));
            }

            return formattedLines;
        }
    }
}
