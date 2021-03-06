import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.vavr.control.Try;
import org.apache.commons.lang3.concurrent.EventCountCircuitBreaker;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.time.temporal.ChronoUnit.SECONDS;

public class Main {
    private static String rabbitHost = "35.88.165.207";
    private static final String userName = "guest1";
    private static final String password = "guest1";
    private static String redisHost = "52.27.132.138";
    private static final Integer redisPort = 6379;
    private static JedisPool pool = null;
    static int numThread = 32;
    static JedisPoolConfig poolConfig = new JedisPoolConfig();



    public static void main(String[] args) {
        if (args.length == 0) {
            throw new IllegalArgumentException("Some arguments are missing values");
        }
        Map<String, String> params = new HashMap<>();
        for (int i = 0; i < args.length - 1; i += 2) {
            params.put(args[i], args[i + 1]);
        }

        // validation
        if (params.containsKey("--numThread")) {
            int numThreadArg = Integer.parseInt(params.get("--numThread"));
            if (numThreadArg <= 0 | numThreadArg > 1024) {
                System.out.println("Invalid number of thread, default to 32");
            } else {
                numThread = numThreadArg;
            }
        } else {
            throw new IllegalArgumentException("Missing --numThread arguments");
        }

        if (params.containsKey("--urlRabbit")) {
            rabbitHost = params.get("--urlRabbit");
        } else {
            throw new IllegalArgumentException("Missing --urlRabbit arguments");
        }

        if (params.containsKey("--urlRedis")) {
            redisHost = params.get("--urlRedis");
        } else {
            throw new IllegalArgumentException("Missing --urlRedis arguments");
        }

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(rabbitHost);
        factory.setUsername(userName);
        factory.setPassword(password);
        poolConfig.setMaxTotal(1000);
        pool = new JedisPool(poolConfig,redisHost, redisPort);
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(10)
                .waitDuration(Duration.of(5, SECONDS))
                .build();
        RetryRegistry registry = RetryRegistry.of(config);
        Retry retry = registry.retry("SkierRedis", config);


        try {
            Connection newConnection = factory.newConnection();
            Runnable[] tasks = new Runnable[numThread];
            for (int i = 0; i < numThread; i++) {
                tasks[i] = new ConsumerThread(pool, newConnection);
                Retry.decorateRunnable(retry,tasks[i]);
            }

            for (int i = 0; i < numThread; i++) {
                Try.run(tasks[i]::run);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
