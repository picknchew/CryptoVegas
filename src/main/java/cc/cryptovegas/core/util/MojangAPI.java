package cc.cryptovegas.core.util;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.inventivetalent.mcwrapper.auth.properties.PropertyWrapper;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public final class MojangAPI {
    private static final JsonParser parser = new JsonParser();
    private static final String API_PROFILE_LINK = "https://sessionserver.mojang.com/session/minecraft/profile/";

    private static final LoadingCache<String, PropertyWrapper> cache = CacheBuilder.newBuilder()
            .expireAfterAccess(10L, TimeUnit.MINUTES)
            .build(CacheLoader.from(uuid -> {
                String json = getContent(API_PROFILE_LINK + uuid.replace("-", "") + "?unsigned=false");
                JsonObject response = parser.parse(json).getAsJsonObject();
                JsonObject textures = response.get("properties").getAsJsonArray().get(0).getAsJsonObject();

                return new PropertyWrapper("textures", textures.get("value").getAsString(), textures.get("signature").getAsString());
            }));

    public static PropertyWrapper getTextures(String uuid) throws ExecutionException {
        return cache.get(uuid);
    }

    private static String getContent(String link) {
        try {
            URL url = new URL(link);
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                return reader.readLine();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}
