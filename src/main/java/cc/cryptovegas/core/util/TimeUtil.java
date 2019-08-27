package cc.cryptovegas.core.util;

import java.time.Duration;

public class TimeUtil {

    public static long toDaysPart(Duration duration){
        return duration.getSeconds() / (24 * 60 * 60);
    }

    public static int toHoursPart(Duration duration){
        return (int) (duration.toHours() % 24);
    }

    public static int toMinutesPart(Duration duration){
        return (int) (duration.toMinutes() % 60);
    }

    public static int toSecondsPart(Duration duration){
        return (int) (duration.getSeconds() % 60);
    }
}
