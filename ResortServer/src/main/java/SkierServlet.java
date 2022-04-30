import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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

@WebServlet(name = "SkierServlet", value = "/SkierServlet")
public class SkierServlet extends HttpServlet {
    public static ConnectionFactory factory;
    public static ObjectPool<Channel> pool;
    public GsonBuilder builder;
    public Gson gson;
    private static String EXCHANGE_NAME = "postRequest";
    private static String QUEUE_NAME_SKIER = "skier";
    private static String QUEUE_NAME_RESORT = "resort";
    final static Logger logger = Logger.getLogger(SkierServlet.class.getName());
    private static final String RABBIT_HOST = "34.217.149.241";
    private static final String userName = "guest1";
    private static final String password = "guest1";
    private static final String REDIS_HOST = "50.112.201.131";
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
            Channel initChannel = newConn.createChannel();
            initChannel.exchangeDeclare(EXCHANGE_NAME, "fanout");
            bindQueueToExchange(initChannel,QUEUE_NAME_SKIER,EXCHANGE_NAME);
            bindQueueToExchange(initChannel,QUEUE_NAME_RESORT,EXCHANGE_NAME);
            initChannel.close();

            jedisPoolConfig.setMaxTotal(1000);
            jedisPool = new JedisPool(jedisPoolConfig,REDIS_HOST, redisPort);
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

        String skierId = null;
        if (urlParts.length==8){
            skierId = urlParts[7];
        } else if (urlParts.length==3){
            skierId = urlParts[1];
        }
        try {
            String currenInfo = jedis.get(skierId);
            if (currenInfo == null){
                response.getWriter().write("No record found");
            }
            else {
                String[] infoSplit = currenInfo.split(" / ");
                if (urlParts.length == 8){
                    String resortId = urlParts[1];
                    String seasonId = urlParts[3];
                    String dayId = urlParts[5];

                    StringBuilder infoOutput = new StringBuilder();
                    infoOutput.append(skierId);
                    infoOutput.append(":");

                    for (String record: infoSplit){
                        String[] detail = record.split(" ");
                        String resortIdFound = null;
                        String seasonIdFound = null;
                        String dayIdFound = null;
                        for (int i = 0; i < detail.length; i++) {
                            if (detail[i].equals("resortId")) {
                                resortIdFound = detail[i+1];
                            }
                            if (detail[i].equals("seasonId")) {
                                seasonIdFound = detail[i+1];
                            }
                            if (detail[i].equals("dayId")) {
                                dayIdFound = detail[i+1];
                            }
                        }

                        if (resortIdFound.equals(resortId) && seasonIdFound.equals(seasonId) && dayIdFound.equals(dayId)){
                            infoOutput.append(record);
                            infoOutput.append("/");
                        }
                    }
                    if (infoOutput.toString().length() > 0){
                    response.getWriter().write(infoOutput.toString());}
                    else{
                        response.getWriter().write("Skier ID found but no match for resort,season and day ID");
                    }
                }
                //vertical path
                else if (urlParts.length == 3){
                    StringBuilder verticalOutput = new StringBuilder();
                    verticalOutput.append(skierId);
                    verticalOutput.append(":");

                    for (String record: infoSplit){
                        String[] detail = record.split(" ");
                        for (int i = 0; i < detail.length; i++) {
                            if (detail[i].equals("vertical")) {
                                verticalOutput.append(detail[i+1]);
                                verticalOutput.append(" ");
                            }
                        }
                    }
                    response.getWriter().write(verticalOutput.toString());
                }
            }

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
            return;
        }


    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/plain");
        String urlPath = request.getPathInfo();
        // check we have a URL!
        if (urlPath == null || urlPath.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write("missing parameters");
            return;
        }
        if (!validatePost(request)) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            System.out.println("fail validate");
        } else {
            logger.log(Level.INFO, request.getParameter("skier_id"));
            int skierId = Integer.parseInt(request.getParameter("skier_id"));
            int liftId = Integer.parseInt(request.getParameter("lift_id"));
            int minute = Integer.parseInt(request.getParameter("minute"));
            int waitTime = Integer.parseInt(request.getParameter("wait"));
            int resortId = Integer.parseInt(request.getParameter("resort_id"));
            int dayId = Integer.parseInt(request.getParameter("day_id"));
            int seasonId = Integer.parseInt(request.getParameter("season_id"));
            LiftInfo newInfo = new LiftInfo(skierId,liftId,minute,waitTime, resortId, dayId, seasonId);
            try {
                Channel channel = pool.borrowObject();
                String jsonString = gson.toJson(newInfo);
                channel.basicPublish(EXCHANGE_NAME, "", null, jsonString.getBytes("UTF-8"));
                pool.returnObject(channel);
                response.setStatus(HttpServletResponse.SC_CREATED);
                response.getWriter().write("It works post!");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private boolean validateGet(String[] urlPath) {
        // urlPath  = "/1/seasons/2019/day/1/skier/123"
        // urlParts = [, 1, seasons, 2019, day, 1, skier, 123]
        if (urlPath.length == 8 || urlPath.length == 3  ){
            return true;
        }
        return false;
    }

    private boolean validatePost(HttpServletRequest request) {
//        logger.log(Level.INFO, request.getParameter("skiers_ids"));
        Map paramsSupplied = request.getParameterMap();
        if (paramsSupplied.containsKey("skier_id")){
            if (Integer.parseInt(request.getParameter("skier_id")) > 100000){
                System.out.println("skier id wrong");
                return false;
            }
        }
        if (paramsSupplied.containsKey("lift_id")){
            if (Integer.parseInt(request.getParameter("lift_id")) > 60){
                System.out.println("lift id wrong");
                return false;
            }
        }

        if (paramsSupplied.containsKey("minute")){
            if (Integer.parseInt(request.getParameter("minute")) > 420){
                System.out.println("minute wrong");
                return false;
            }
        }

        if (paramsSupplied.containsKey("wait")){
            if (Integer.parseInt(request.getParameter("wait")) > 10){
                System.out.println("wait wrong");
                return false;
            }
        }

        if (paramsSupplied.containsKey("resort_id")){
            if (Integer.parseInt(request.getParameter("resort_id")) > 4){
                System.out.println("resort id wrong");
                return false;
            }
        }

        if (paramsSupplied.containsKey("day_id")){
            if (Integer.parseInt(request.getParameter("day_id")) > 30){
                System.out.println("day id wrong");
                return false;
            }
        }

        if (paramsSupplied.containsKey("season_id")){
            if (Integer.parseInt(request.getParameter("season_id")) > 4){
                System.out.println("season id wrong");
                return false;
            }
        }

        return true;
    }

    private void bindQueueToExchange(Channel channel, String queueName,String exchangeName) throws IOException {
        channel.queueDeclare(queueName, false, false, false, null);
        channel.queueBind(queueName, exchangeName, "");
    }
}
