package cc.cryptovegas.core.player;

import java.time.Duration;
import java.time.Instant;

public class PlayerStats {
    private int realBetsMade;
    private Duration timePlayed;
    private Instant lastJoin;

    public void setLastJoin(Instant lastJoin) {
        this.lastJoin = lastJoin;
    }

    public Instant getLastJoin() {
        return lastJoin;
    }

    public void setTimePlayed(Duration timePlayed) {
        this.timePlayed = timePlayed;
    }

    public Duration getTimePlayed() {
        return timePlayed.plus(Duration.between(lastJoin, Instant.now()));
    }

    public int getRealBetsMade() {
        return realBetsMade;
    }

    public void setRealBetsMade(int realBetsMade) {
        this.realBetsMade = realBetsMade;
    }
}
