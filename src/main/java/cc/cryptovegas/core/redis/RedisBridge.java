package cc.cryptovegas.core.redis;

import redis.clients.jedis.JedisPool;
import tech.rayline.core.inject.DisableHandler;
import tech.rayline.core.inject.Injectable;
import tech.rayline.core.inject.InjectionProvider;
import tech.rayline.core.library.MavenLibrary;
import tech.rayline.core.parse.ReadOnlyResource;
import tech.rayline.core.parse.ResourceFile;
import tech.rayline.core.plugin.RedemptivePlugin;

@Injectable(libraries = {@MavenLibrary("redis.clients:jedis:2.9.0"), @MavenLibrary("org.apache.commons:commons-pool2:2.5.0")})
public final class RedisBridge {
    @ResourceFile(filename = "redis.yml")
    @ReadOnlyResource
    private RedisConfiguration redisConfiguration;

    private JedisPool pool;

    @InjectionProvider
    public RedisBridge(RedemptivePlugin plugin) {
        plugin.getResourceFileGraph().addObject(this);
        pool = redisConfiguration.getPool();
    }

    @DisableHandler
    private void onDisable() throws InterruptedException {
        pool.close();
    }

    public JedisPool getPool() {
        return pool;
    }
}
