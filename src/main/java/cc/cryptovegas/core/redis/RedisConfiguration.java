package cc.cryptovegas.core.redis;

import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class RedisConfiguration {
    public String host;
    public int port;

    public JedisPool getPool() {
        return new JedisPool(new JedisPoolConfig(), host, port);
    }
}
