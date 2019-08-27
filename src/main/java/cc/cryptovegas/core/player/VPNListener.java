package cc.cryptovegas.core.player;

import cc.cryptovegas.core.Core;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import tech.rayline.core.util.RunnableShorthand;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

public class VPNListener {
    private final Gson gson = new Gson();

    public VPNListener(Core core) {
        core.observeEvent(EventPriority.LOW, PlayerJoinEvent.class)
                .doOnNext(event -> {
                    Player player = event.getPlayer();
                    String address = player.getAddress().getAddress().getHostAddress();

                    RunnableShorthand.forPlugin(core).async().with(() -> {
                        try {
                            if (isVPN(address)) {
                                RunnableShorthand.forPlugin(core).with(() -> player.kickPlayer("VPNs may not be used on this server.")).go();
                            }
                        } catch (IOException ignored) {
                        }
                    }).go();
                }).subscribe();
    }

    private boolean isVPN(String ip) throws IOException {
        URL url = new URL("http://api.vpnblocker.net/v2/json/" + ip);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()))) {
            IPInfo ipInfo = gson.fromJson(reader, IPInfo.class);

            if (ipInfo.status.equals("success") && ipInfo.isVpn) {
                return true;
            }
        }

        return false;
    }

    class IPInfo {
        @SerializedName("status")
        private String status;

        @SerializedName("package")
        private String tier;

        @SerializedName("ipaddress")
        private String ip;

        @SerializedName("host-ip")
        private boolean isVpn;

        @SerializedName("org")
        private String organization;
    }
}
