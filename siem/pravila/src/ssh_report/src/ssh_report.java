import com.google.common.collect.ImmutableList;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.elasticsearch.spark.rdd.api.java.JavaEsSpark;
import org.json.JSONArray;
import org.json.JSONObject;
import scala.util.parsing.json.JSON;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;


public class ssh_report {
    public static String url = "localhost";
    public static int port = 9200;
    public static String path = "/syslog/doc/_search";
    public static int limit;
    public static String query =  "{\n" +
            "\"size\": 0,\n" +
            "\"query\": {\n" +
            "    \"bool\" : {\n" +
            "      \"must\" : {\n" +
            "        \"range\":{\n" +
            "            \"@timestamp\":{\n" +
            "              \"gt\" : \"now-10m\"\n" +
            "            }\n" +
            "        }\n" +
            "      }\n" +
            "    }\n" +
            "  },\n" +
            "\"aggs\" : {\n" +
            "    \"ssh\" : {\n" +
            "        \"terms\" : { \"field\" : \"host.keyword\", \"size\" : 500 },\n" +
            "        \"aggs\" : {\n" +
            "          \"uniq_ips\" : {\n" +
            "            \"terms\" : { \"field\" : \"rhost.keyword\", \"size\" : \"100\" }\n" +
            "          }\n" +
            "        }\n" +
            "    }\n" +
            " }\n" +
            "}";

    public static void main(String[] args) {
        JSONObject json = sendGetRequest(url, port, path, query);

        limit = Integer.parseInt(args[0]);

        String[] data = extractData(json, limit);

        for (String a : data) {
            System.out.println(a);
        }

        if (data.length != 0) {
            postToEs(data);
        }

    }

    public static String getEsTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        return String.join("T", sdf.format(new Date()).split(" ")) + "Z";
    }

    public static String[] extractData(JSONObject json, int limit) {
        JSONArray array = json.getJSONObject("aggregations").getJSONObject("ssh").getJSONArray("buckets");
        String out = "";
        String time = getEsTimestamp();

        int len1 = array.length();
        // For each host
        for (int i = 0; i < len1; i++) {
            JSONObject JSONhost = array.getJSONObject(i);
            String host = JSONhost.get("key").toString(); // host name
            Integer count = 0;; // host connection count

            JSONArray uniq_ips = JSONhost.getJSONObject("uniq_ips").getJSONArray("buckets");

            int len2 = uniq_ips.length();
            String connections = "";

            for (int j = 0; j < len2; j++) {
                if (j != 0) connections = connections + ", ";
                connections = connections + "{ \"rhost\" : ";
                JSONObject JSONrhost = uniq_ips.getJSONObject(j);
                //{ "rhost" : ip, "count" : #num }
                connections = connections + "\"" + JSONrhost.get("key") + "\"" + ", \"count\" : " + JSONrhost.get("doc_count") + " }";
                count = count + JSONrhost.getInt("doc_count");
            }
            connections = "[" + connections + "]";

            if (count >= limit) {
                if (i != 0) out = out + "@&%";

                out = out + "{ \"host\" : \"";
                out = out + host + "\" , \"count\" : " + count.toString();
                out = out + ", \"connections\" : " + connections;
                out = out + ", \"message\" : \"Failed SSH connections in last 10 min: " + count.toString() + " \"";
                out = out + ", \"@timestamp\" : \"" + time + "\" }";
            }
        }

        if (out.isEmpty()) return new String[0];

        return out.split("@&%");
    }

    public static JSONObject sendGetRequest(String requestUrl, int port, String path, String payload) {
        StringBuffer jsonString = null;
        try {
            URI uri = new URI("http", null, requestUrl, port, path, null, null );
            URL url = uri.toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setRequestMethod("GET");
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

    public static void postToEs(String[] data) {
        SparkConf conf = new SparkConf().setAppName("myApp").setMaster("local");
        conf.set("es.index.auto.create", "true");

        JavaSparkContext jsc = new JavaSparkContext(conf);

        for (int i = 0; i < data.length; i++ ) {
            System.out.println(data[i]);
            JavaRDD<String> stringRDD = jsc.parallelize(ImmutableList.of(data[i]));
            JavaEsSpark.saveJsonToEs(stringRDD, "siem/doc");
        }
    }
}

