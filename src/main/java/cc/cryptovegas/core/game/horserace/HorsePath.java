package cc.cryptovegas.core.game.horserace;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Set;

public class HorsePath {
    private static final int GROUND_LEVEL = 76;

    public static final List<Vector> LANE_ONE_POINTS = new ImmutableList.Builder<Vector>()
            .add(new Vector(-1085, GROUND_LEVEL, -928))
            .add(new Vector(-1086, GROUND_LEVEL, -929))
            .add(new Vector(-1086, GROUND_LEVEL, -930))
            .add(new Vector(-1091, GROUND_LEVEL, -935))
            .add(new Vector(-1092, GROUND_LEVEL, -935))
            .add(new Vector(-1093, GROUND_LEVEL, -936))
            .add(new Vector(-1100, GROUND_LEVEL, -936))
            .add(new Vector(-1101, GROUND_LEVEL, -935))
            .add(new Vector(-1102, GROUND_LEVEL, -935))
            .add(new Vector(-1107, GROUND_LEVEL, -930))
            .add(new Vector(-1107, GROUND_LEVEL, -929))
            .add(new Vector(-1108, GROUND_LEVEL, -928))
            .add(new Vector(-1108, GROUND_LEVEL, -883))
            .add(new Vector(-1107, GROUND_LEVEL, -881))
            .add(new Vector(-1102, GROUND_LEVEL, -876))
            .add(new Vector(-1101, GROUND_LEVEL, -876))
            .add(new Vector(-1100, GROUND_LEVEL, -875))
            .add(new Vector(-1093, GROUND_LEVEL, -875))
            .add(new Vector(-1091, GROUND_LEVEL, -876))
            .add(new Vector(-1086, GROUND_LEVEL, -881))
            .add(new Vector(-1085, GROUND_LEVEL, -883))
            .add(new Vector(-1085, GROUND_LEVEL, -904.5))
            .build();

    public static final List<Vector> LANE_TWO_POINTS = new ImmutableList.Builder<Vector>()
            .add(new Vector(-1083, GROUND_LEVEL, -928))
            .add(new Vector(-1084, GROUND_LEVEL, -929))
            .add(new Vector(-1084, GROUND_LEVEL, -931))
            .add(new Vector(-1090, GROUND_LEVEL, -937))
            .add(new Vector(-1092, GROUND_LEVEL, -937))
            .add(new Vector(-1093, GROUND_LEVEL, -938))
            .add(new Vector(-1100, GROUND_LEVEL, -938))
            .add(new Vector(-1101, GROUND_LEVEL, -937))
            .add(new Vector(-1103, GROUND_LEVEL, -937))
            .add(new Vector(-1109, GROUND_LEVEL, -931))
            .add(new Vector(-1109, GROUND_LEVEL, -929))
            .add(new Vector(-1110, GROUND_LEVEL, -928))
            .add(new Vector(-1110, GROUND_LEVEL, -883))
            .add(new Vector(-1109, GROUND_LEVEL, -882))
            .add(new Vector(-1109, GROUND_LEVEL, -880))
            .add(new Vector(-1103, GROUND_LEVEL, -874))
            .add(new Vector(-1101, GROUND_LEVEL, -874))
            .add(new Vector(-1100, GROUND_LEVEL, -873))
            .add(new Vector(-1093, GROUND_LEVEL, -873))
            .add(new Vector(-1092, GROUND_LEVEL, -874))
            .add(new Vector(-1090, GROUND_LEVEL, -874))
            .add(new Vector(-1084, GROUND_LEVEL, -880))
            .add(new Vector(-1084, GROUND_LEVEL, -882))
            .add(new Vector(-1083, GROUND_LEVEL, -883))
            .add(new Vector(-1083, GROUND_LEVEL, -904.5))
            .build();

    public static final List<Vector> LANE_THREE_POINTS = new ImmutableList.Builder<Vector>()
            .add(new Vector(-1081, GROUND_LEVEL, -929))
            .add(new Vector(-1082, GROUND_LEVEL, -930))
            .add(new Vector(-1082, GROUND_LEVEL, -932))
            .add(new Vector(-1088, GROUND_LEVEL, -938))
            .add(new Vector(-1089, GROUND_LEVEL, -939))
            .add(new Vector(-1091, GROUND_LEVEL, -939))
            .add(new Vector(-1092, GROUND_LEVEL, -940))
            .add(new Vector(-1100, GROUND_LEVEL, -940))
            .add(new Vector(-1102, GROUND_LEVEL, -939))
            .add(new Vector(-1103, GROUND_LEVEL, -939))
            .add(new Vector(-1105, GROUND_LEVEL, -938))
            .add(new Vector(-1111, GROUND_LEVEL, -932))
            .add(new Vector(-1111, GROUND_LEVEL, -930))
            .add(new Vector(-1112, GROUND_LEVEL, -929))
            .add(new Vector(-1112, GROUND_LEVEL, -882))
            .add(new Vector(-1111, GROUND_LEVEL, -881))
            .add(new Vector(-1111, GROUND_LEVEL, -879))
            .add(new Vector(-1105, GROUND_LEVEL, -873))
            .add(new Vector(-1103, GROUND_LEVEL, -872))
            .add(new Vector(-1102, GROUND_LEVEL, -872))
            .add(new Vector(-1101, GROUND_LEVEL, -871))
            .add(new Vector(-1093, GROUND_LEVEL, -871))
            .add(new Vector(-1091, GROUND_LEVEL, -872))
            .add(new Vector(-1090, GROUND_LEVEL, -872))
            .add(new Vector(-1088, GROUND_LEVEL, -873))
            .add(new Vector(-1082, GROUND_LEVEL, -879))
            .add(new Vector(-1082, GROUND_LEVEL, -881))
            .add(new Vector(-1081, GROUND_LEVEL, -882))
            .add(new Vector(-1081, GROUND_LEVEL, -904.5))
            .build();

    public static final List<Vector> LANE_FOUR_POINTS = new ImmutableList.Builder<Vector>()
            .add(new Vector(-1079, GROUND_LEVEL, -929))
            .add(new Vector(-1080, GROUND_LEVEL, -930))
            .add(new Vector(-1080, GROUND_LEVEL, -932))
            .add(new Vector(-1081, GROUND_LEVEL, -933))
            .add(new Vector(-1081, GROUND_LEVEL, -934))
            .add(new Vector(-1087, GROUND_LEVEL, -940))
            .add(new Vector(-1089, GROUND_LEVEL, -941))
            .add(new Vector(-1091, GROUND_LEVEL, -941))
            .add(new Vector(-1092, GROUND_LEVEL, -942))
            .add(new Vector(-1101, GROUND_LEVEL, -942))
            .add(new Vector(-1102, GROUND_LEVEL, -941))
            .add(new Vector(-1104, GROUND_LEVEL, -941))
            .add(new Vector(-1105, GROUND_LEVEL, -940))
            .add(new Vector(-1106, GROUND_LEVEL, -940))
            .add(new Vector(-1112, GROUND_LEVEL, -934))
            .add(new Vector(-1113, GROUND_LEVEL, -932))
            .add(new Vector(-1113, GROUND_LEVEL, -931))
            .add(new Vector(-1114, GROUND_LEVEL, -929))
            .add(new Vector(-1114, GROUND_LEVEL, -882))
            .add(new Vector(-1113, GROUND_LEVEL, -881))
            .add(new Vector(-1113, GROUND_LEVEL, -879))
            .add(new Vector(-1112, GROUND_LEVEL, -877))
            .add(new Vector(-1106, GROUND_LEVEL, -871))
            .add(new Vector(-1105, GROUND_LEVEL, -871))
            .add(new Vector(-1104, GROUND_LEVEL, -870))
            .add(new Vector(-1102, GROUND_LEVEL, -870))
            .add(new Vector(-1101, GROUND_LEVEL, -869))
            .add(new Vector(-1092, GROUND_LEVEL, -869))
            .add(new Vector(-1091, GROUND_LEVEL, -870))
            .add(new Vector(-1089, GROUND_LEVEL, -870))
            .add(new Vector(-1088, GROUND_LEVEL, -871))
            .add(new Vector(-1087, GROUND_LEVEL, -871))
            .add(new Vector(-1081, GROUND_LEVEL, -877))
            .add(new Vector(-1080, GROUND_LEVEL, -879))
            .add(new Vector(-1080, GROUND_LEVEL, -880))
            .add(new Vector(-1079, GROUND_LEVEL, -882))
            .add(new Vector(-1079, GROUND_LEVEL, -904.5))
            .build();

    // 15.9887D
    public static final double LANE_ONE_SPEED = 3.85586401729D;
    // 16.4113D
    public static final double LANE_TWO_SPEED = 4.05304443844D;
    // 18.1509D
    public static final double LANE_THREE_SPEED = 4.36989114247D;
    // 20.6809D
    public static final double LANE_FOUR_SPEED = 4.99336345506D;

    private static final World world = Bukkit.getWorld("world");

    public static Location LANE_ONE_START = new Location(world, -1085, 76, -907, -180, 0);
    public static Location LANE_TWO_START = new Location(world, -1083, 76, -907, -180, 0);
    public static Location LANE_THREE_START = new Location(world, -1081, 76, -907, -180, 0);
    public static Location LANE_FOUR_START = new Location(world, -1079, 76, -907, -180, 0);

    public static Location FIREWORKS = new Location(world, -1097.5, 77, -905.5);

    public static Set<Chunk> CHUNKS = ImmutableSet.of(world.getChunkAt(-68, -57), world.getChunkAt(-68, -56), world.getChunkAt(-68, -55),
            world.getChunkAt(-69, -55), world.getChunkAt(-70, -55), world.getChunkAt(-70, -56), world.getChunkAt(-70, -57),
            world.getChunkAt(-70, -58), world.getChunkAt(-70, -59), world.getChunkAt(-69, -59), world.getChunkAt(-68, -59),
            world.getChunkAt(-68, -58));
}
