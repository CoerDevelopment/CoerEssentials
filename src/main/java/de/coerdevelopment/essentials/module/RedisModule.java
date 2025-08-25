package de.coerdevelopment.essentials.module;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class RedisModule extends Module {

    private String host;
    private int port;
    private String username;
    private String password;
    private int database;
    private int maxPoolSize;

    private RedisClient client;
    private final List<StatefulRedisConnection<String, String>> connections = new ArrayList<>();
    private final List<RedisCommands<String, String>> commands = new ArrayList<>();
    private RedisCommands<String, String> sharedProxy;
    private final AtomicInteger rr = new AtomicInteger();

    public RedisModule() {
        super(ModuleType.REDIS);
        this.host = getStringOption("host");
        this.port = getIntOption("port");
        this.username = getStringOption("username");
        this.password = getStringOption("password");
        this.database = getIntOption("database");
        this.maxPoolSize = getIntOption("maxPoolSize");
        init();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {close();}));
    }

    public RedisCommands<String, String> getSharedCommands() {
        return sharedProxy;
    }

    public void close() {
        for (var c : connections) {
            try { c.close(); } catch (Exception ignored) {}
        }
        if (client != null) {
            try { client.shutdown(); } catch (Exception ignored) {}
        }
    }

    private void init() {
        this.client = RedisClient.create(getUri());
        for (int i = 0; i < maxPoolSize; i++) {
            var conn = client.connect();
            connections.add(conn);
            commands.add(conn.sync());
        }
        this.sharedProxy = buildRoundRobinProxy(commands);
    }

    private String getUri() {
        String userInfo = (username != null && !username.isEmpty())
                ? username + ((password != null && !password.isEmpty()) ? ":" + password : "") + "@"
                : ((password != null && !password.isEmpty()) ? ":" + password + "@" : "");
        return "redis://" + userInfo + host + ":" + port + "/" + database;
    }

    private RedisCommands<String, String> buildRoundRobinProxy(List<RedisCommands<String, String>> cmds) {
        Class<?> iface = RedisCommands.class;
        InvocationHandler ih = new InvocationHandler() {
            @Override public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                String name = method.getName();
                // handle Object methods locally
                if (name.equals("toString")) return "RedisCommands[round-robin x" + cmds.size() + "]";
                if (name.equals("hashCode")) return System.identityHashCode(proxy);
                if (name.equals("equals"))   return proxy == args[0];

                int i = Math.floorMod(rr.getAndIncrement(), cmds.size());
                RedisCommands<String, String> target = cmds.get(i);
                return method.invoke(target, args);
            }
        };
        return (RedisCommands<String, String>) Proxy.newProxyInstance(
                iface.getClassLoader(), new Class<?>[]{iface}, ih);
    }

}
