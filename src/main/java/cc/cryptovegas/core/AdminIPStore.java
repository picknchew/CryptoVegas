package cc.cryptovegas.core;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class AdminIPStore {
    private static final File file;

    static {
        file = new File("plugins" + File.separator + "Core", "admin_ips.txt");

        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static boolean contains(String ip) {
        return getIpAddresses().contains(ip);
    }

    private static final List<String> getIpAddresses() {
        List<String> addresses = new ArrayList<>();

        try {
            Files.lines(file.toPath()).forEach(addresses::add);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return addresses;
    }
}
