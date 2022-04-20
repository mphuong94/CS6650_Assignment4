import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.exceptions.JedisException;
import util.ChannelFactory;
import util.LiftInfo;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet(name = "ResortServlet", value = "/ResortServlet")
public class ResortServlet extends HttpServlet {
    public static ConnectionFactory factory;
    public static ObjectPool<Channel> pool;
    public GsonBuilder builder;
    public Gson gson;
    private static String QUEUE_NAME_SKIER = "skier";
    private static String QUEUE_NAME_RESORT = "resort";
    final static Logger logger = Logger.getLogger(SkierServlet.class.getName());
    private static final String RABBIT_HOST = "35.88.165.207";
    private static final String userName = "guest1";
    private static final String password = "guest1";
    private static final String redisHost = "52.27.132.138";
    private static final Integer redisPort = 6379;
    private static JedisPool jedisPool = null;
    static JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();

    @Override
    public void init() {
        factory = new ConnectionFactory();
        factory.setHost(RABBIT_HOST);
        factory.setUsername(userName);
        factory.setPassword(password);
        try {
            Connection newConn = factory.newConnection();
            GenericObjectPoolConfig<Channel> config = new GenericObjectPoolConfig<>();
            config.setMaxTotal(500);
            config.setMinIdle(100);
            config.setMaxIdle(200);
            pool = new GenericObjectPool<>(new ChannelFactory(newConn), config);

            jedisPoolConfig.setMaxTotal(1000);
            jedisPool = new JedisPool(jedisPoolConfig,redisHost, redisPort);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        builder = new GsonBuilder();
        builder.setPrettyPrinting();
        gson = builder.create();
    }


    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/plain");
        String urlPath = request.getPathInfo();

        // check we have a URL!
        if (urlPath == null || urlPath.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write("missing parameters");
            return;
        }

        String[] urlParts = urlPath.split("/");
        // and now validate url path and return the response status code
        // (and maybe also some value if input is valid)

        if (!validateGet(urlParts)) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        } else {
            response.setStatus(HttpServletResponse.SC_OK);
        }
        Jedis jedis = jedisPool.getResource();
        String resortId = urlParts[1];
        String seasonId = urlParts[3];
        String dayId = urlParts[5];
        StringBuilder keyQuery = new StringBuilder();
        keyQuery.append(resortId);
        keyQuery.append("-");
        keyQuery.append(seasonId);
        keyQuery.append("-");
        keyQuery.append(dayId);
        try {
            String currenInfo = jedis.get(keyQuery.toString());
            response.getWriter().write(currenInfo);
            return;
        } catch (JedisException e) {
            // return to pool if needed
            if (null != jedis) {
                jedisPool.returnBrokenResource(jedis);
                jedis = null;
            }
        } finally {
            // return to pool after finishing
            if (null != jedis)
                jedisPool.returnResource(jedis);
        }


    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        return;
    }

    private boolean validateGet(String[] urlPath) {
        // urlPath  = "/1/seasons/2019/day/1/skier/123"
        // urlParts = [, 1, seasons, 2019, day, 1, skier, 123]
        if (urlPath.length == 7){
            return true;
        }
        return false;
    }


}

