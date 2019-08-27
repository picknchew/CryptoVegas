package cc.cryptovegas.core.scoreboard;

import java.util.List;

public class ScoreboardConfiguration {
    public String title;
    public List<String> lines;

    public SimpleScoreboard getNewScoreboard() {
        return new SimpleScoreboard(title);
    }

    public ScoreboardTemplate getTemplate() {
        return new ScoreboardTemplate(lines);
    }
}
