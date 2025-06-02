package de.coerdevelopment.essentials.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class UrlHelper {

    private CacheManager urlCache;
    private boolean cacheEnabled;
    private Map<String, String> headers;
    
    public UrlHelper(int cacheTtl) {
        cacheEnabled = cacheTtl <= 0;
        if (cacheEnabled) {
            urlCache = new CacheManager(cacheTtl);
        }
        headers = new HashMap<>();
    }

    public UrlHelper() {
        this(-1);
    }

    public String getJsonFromUrl(String urlString) {
        if (!cacheEnabled) {
            return readFromUrl(urlString);
        }
        return (String) urlCache.getObject(urlString, new CacheAction() {
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
            for (Map.Entry<String, String> header : headers.entrySet()) {
                conn.setRequestProperty(header.getKey(), header.getValue());
            }
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

    public void addHeader(String key, String value) {
        headers.put(key, value);
    }

    public void removeHeader(String key) {
        headers.remove(key);
    }

}
