package ua.shadowdan.util;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/*
 * Created by SHADOWDAN_ on 19.06.2020 for project 'IscariBotV2'
 */
public class HttpUtil {

    private HttpUtil() { }

    public static String doGetRequest(String url) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");

            return IOUtil.readToString(connection.getInputStream());
        } catch (IOException ex) {
            return null;
        }
    }
}
