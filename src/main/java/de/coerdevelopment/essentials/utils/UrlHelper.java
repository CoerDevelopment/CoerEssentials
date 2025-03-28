package de.coerdevelopment.essentials.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class UrlHelper {

    private static UrlHelper instance;
    
    public static UrlHelper getInstance() {
        if (instance == null) {
            instance = new UrlHelper();
        }
        return instance;
    }

    private CacheManager cacheManager;
    
    private UrlHelper() {
        cacheManager = new CacheManager(1000*60);
    }
    
    public String getJsonFromUrl(String urlString) {
        return (String) cacheManager.getObject(urlString, new CacheAction() {
            @Override
            public Object createObject() {
                return readFromUrl(urlString);
            }
        });
    }

    private String readFromUrl(String urlString) {
        StringBuilder result = new StringBuilder();
        try {
            URL url = new URL(urlString);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");

            if (conn.getResponseCode() == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                reader.close();
            } else {
                throw new RuntimeException("HTTP GET Request Failed with Error Code : " + conn.getResponseCode());
            }

            conn.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result.toString();
    }

}
