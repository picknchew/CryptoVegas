package cc.cryptovegas.core.util;

import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;
import com.google.common.io.BaseEncoding;

import java.security.SecureRandom;

public class HashingUtil {
    private static ThreadLocal<SecureRandom> secureRandom = ThreadLocal.withInitial(SecureRandom::new);

    public static String hashString(String string, String key) {
        return Hashing.hmacSha256(key.getBytes(Charsets.UTF_8)).hashString(string, Charsets.UTF_8).toString();
    }

    public static String generateRandomKey() {
        byte[] key = new byte[16];
        secureRandom.get().nextBytes(key);
        return BaseEncoding.base16().encode(key);
    }
}
