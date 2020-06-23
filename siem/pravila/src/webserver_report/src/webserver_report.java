import com.google.common.collect.ImmutableList;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.elasticsearch.spark.rdd.api.java.JavaEsSpark;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class webserver_report {

    public static String url = "localhost";
    public static int port = 9200;
    public static String pathQuery = "/siem/doc/_search";
    public static String query = "";
    public static String observed_host;
    public static int observed_port;


    public static void main(String[] args) throws IOException {
        observed_host = args[0];
        observed_port = Integer.parseInt(args[1]);

        generateQuery();

        System.out.printf("Checking host: " + observed_host + " port: " + observed_port);

        boolean isUp = pingHost(observed_host, observed_port, 100);

        System.out.printf(" status: " + isUp + " ");

        JSONObject json = sendGetRequest(url, port, "GET", pathQuery, query);
        boolean exists = json.getJSONObject("hits").getInt("total") > 0;

        System.out.printf("report_entry: " + exists + "\n");

        if ( !isUp && !exists ) {
            createNewReport();
        } else if ( isUp && exists ) {
            String reportID = json.getJSONObject("hits").getJSONArray("hits").getJSONObject(0).getString("_id");
            changeStatus(reportID);
        }
    }

    public static void generateQuery() {
        query = "{\n" +
                "  \"query\": {\n" +
                "    \"bool\": {\n" +
                "      \"must\": [\n" +
                "        { \"match\": { \"host\": \""+ observed_host +"\" } },\n" +
                "        { \"match\": { \"port\": " + observed_port + " } },\n" +
                "        { \"match\": { \"resolved\": false } }\n" +
                "      ]\n" +
                "    }\n" +
                "  }\n" +
                "}";
    }

    public static void createNewReport() {
        String time = getEsTimestamp();

        String data = "{ \"host\" : \"" + observed_host +
                "\", \"port\" : " + observed_port +
                " , \"message\" : \"Host is unreachable.\" , \"@timestamp\" : \"" +
                time +"\" , \"resolved\" : false }";

        System.out.println(data);
        postToEs(data);

    }

    public static void changeStatus( String id ) {
        String time = getEsTimestamp();
        String pathUpdate = "/siem/doc/" + id + "/_update";
        String payload = "{\n" +
                "    \"doc\" : {\n" +
                "        \"resolved\" : true,\n" +
                "        \"resolved_time\" : \"" + time +  "\"\n" +
                "    }\n" +
                "}";
        System.out.println(payload);
        sendGetRequest(url, port, "POST", pathUpdate, payload);
    }

    public static boolean pingHost(String host, int port, int timeout) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host,port));
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static String getEsTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        return String.join("T", sdf.format(new Date()).split(" ")) + "Z";
    }

    public static JSONObject sendGetRequest(String requestUrl, int port, String method, String path, String payload) {
        StringBuffer jsonString = null;
        try {
            URI uri = new URI("http", null, requestUrl, port, path, null, null );
            URL url = uri.toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setRequestMethod(method);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream(), "UTF-8");
            writer.write(payload);
            writer.close();
            BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            jsonString = new StringBuffer();
            String line;
            while ((line = br.readLine()) != null) {
                jsonString.append(line);
            }
            br.close();
            connection.disconnect();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
        return new JSONObject(jsonString.toString());
    }

    public static void postToEs(String data) {
        SparkConf conf = new SparkConf().setAppName("myApp").setMaster("local");
        conf.set("es.index.auto.create", "true");

        JavaSparkContext jsc = new JavaSparkContext(conf);

        System.out.println(data);
        JavaRDD<String> stringRDD = jsc.parallelize(ImmutableList.of(data));
        JavaEsSpark.saveJsonToEs(stringRDD, "siem/doc");
    }
}
