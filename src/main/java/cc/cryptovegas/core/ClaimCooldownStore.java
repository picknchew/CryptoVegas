package cc.cryptovegas.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClaimCooldownStore {
    private static final Map<String, String> ips = new ConcurrentHashMap<>();

    public static boolean contains(String ip) {
        return ips.containsKey(ip);
    }

    public static void addPlayer(String ip, String uuid) {
        ips.put(ip, uuid);
    }

    public static boolean canClaim(String ip, String uuid) {
        return ips.get(ip).equals(uuid);
    }

    public static void invalidate(String ip) {
        ips.remove(ip);
    }
}
