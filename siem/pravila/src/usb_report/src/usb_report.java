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


public class usb_report {
    public static String url = "localhost";
    public static int port = 9200;
    public static String path = "/beats/_search";
    public static String query =  "{\n" +
            "  \"size\" : 0,\n" +
            "  \"query\": {\n" +
            "    \"bool\": {\n" +
            "      \"must\": {\n" +
            "        \"terms\": {\n" +
            "          \"event_id\": [2003, 2004, 2006, 2010, 2100, 2101, 2105, 2106]\n" +
            "        }\n" +
            "      },\n" +
            "      \"filter\": {\n" +
            "        \"range\": {\n" +
            "          \"@timestamp\": {\n" +
            "            \"gt\" : \"now-1m\"\n" +
            "          }\n" +
            "        }\n" +
            "      }\n" +
            "    }\n" +
            "  },\n" +
            "    \"aggs\" : {\n" +
            "    \"uniq_host\" : {\n" +
            "      \"terms\": {\n" +
            "        \"field\": \"host.name.keyword\"\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "}";
    public static String result = "{\n" +
            "  \"took\": 20,\n" +
            "  \"timed_out\": false,\n" +
            "  \"_shards\": {\n" +
            "    \"total\": 5,\n" +
            "    \"successful\": 5,\n" +
            "    \"skipped\": 0,\n" +
            "    \"failed\": 0\n" +
            "  },\n" +
            "  \"hits\": {\n" +
            "    \"total\": 164,\n" +
            "    \"max_score\": 0,\n" +
            "    \"hits\": []\n" +
            "  },\n" +
            "  \"aggregations\": {\n" +
            "    \"uniq_host\": {\n" +
            "      \"doc_count_error_upper_bound\": 0,\n" +
            "      \"sum_other_doc_count\": 0,\n" +
            "      \"buckets\": [\n" +
            "        {\n" +
            "          \"key\": \"DELL-PC\",\n" +
            "          \"doc_count\": 128\n" +
            "        },\n" +
            "        {\n" +
            "          \"key\": \"DESKTOP-AKOAMJU\",\n" +
            "          \"doc_count\": 36\n" +
            "        }\n" +
            "      ]\n" +
            "    }\n" +
            "  }\n" +
            "}";

    public static void main(String[] args) {
        //JSONObject json = sendGetRequest(url, port, path, query);

        JSONObject json = new JSONObject(result.toString());

        String[] data = extractData(json);

        for (String a : data) {
            System.out.println(a);
        }

        if (data.length != 0) {
            //postToEs(data);
        }

    }

    public static String getEsTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        return String.join("T", sdf.format(new Date()).split(" ")) + "Z";
    }

    public static String[] extractData(JSONObject json) {
        JSONArray array = json.getJSONObject("aggregations").getJSONObject("uniq_host").getJSONArray("buckets");
        String out = "{ \"host\" : \"";
        String time = getEsTimestamp();

        int len = array.length();
        for (int i = 0; i < len; i++) {
            String host = (String) array.getJSONObject(i).get("key");
            //int value = array.getJSONObject(i).getInt("doc_count");

            if (i != 0) out = out + "@&%{ \"host\" : \"";

            out = out + host +"\", \"message\" : \"Newly discovered USB device.\", \"@timestamp\" : \""+ time +  "\" }";
        }

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

