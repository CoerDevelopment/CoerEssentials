package de.coerdevelopment.essentials.utils;

import org.springframework.http.HttpStatus;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class UrlHelper {

    private CoerCache urlCache;
    private boolean cacheEnabled;
    private Map<String, String> headers;
    private boolean cooldownEnabled;
    private long cooldownMilliseconds;
    private int cooldownAfterRequests;

    private boolean retryAfterTooManyRequests;
    private long tooManyRequestsRetryMilliseconds;

    private int currentRequests;
    
    public UrlHelper(int cacheTtlSeconds) {
        cacheEnabled = cacheTtlSeconds <= 0;
        if (cacheEnabled) {
            urlCache = new CoerCache("urlCache", Duration.ofSeconds(cacheTtlSeconds), String.class);
        }
        headers = new HashMap<>();
        cooldownEnabled = false;
        cooldownMilliseconds = 0;
        cooldownAfterRequests = 0;
        currentRequests = 0;
        retryAfterTooManyRequests = true;
        tooManyRequestsRetryMilliseconds = 2000;
    }

    public UrlHelper() {
        this(-1);
    }

    public String getJsonFromUrl(String urlString) {
        if (!cacheEnabled) {
            return readFromUrl(urlString);
        }
        return (String) urlCache.getOrLoad(urlString, new Supplier() {
            @Override
            public String get() {
                return readFromUrl(urlString);
            }
        });
    }

    private String readFromUrl(String urlString) {
        if (cooldownEnabled) {
            if (currentRequests >= cooldownAfterRequests) {
                try {
                    Thread.sleep(cooldownMilliseconds);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                currentRequests = 0;
            }
            currentRequests++;
        }
        StringBuilder result = new StringBuilder();
        try {
            URL url = new URL(urlString);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            for (Map.Entry<String, String> header : headers.entrySet()) {
                conn.setRequestProperty(header.getKey(), header.getValue());
            }
            conn.setRequestProperty("Accept", "application/json");

            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                reader.close();
            } else {
                if (conn.getResponseCode() == HttpStatus.TOO_MANY_REQUESTS.value() && retryAfterTooManyRequests) {
                    try {
                        Thread.sleep(tooManyRequestsRetryMilliseconds);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return readFromUrl(urlString); // Retry after waiting
                } else if (conn.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) {
                    throw new RuntimeException("HTTP GET Request Failed with Error Code : " + conn.getResponseCode());
                }
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

    public void enableCooldown(long cooldownMilliseconds, int cooldownAfterRequests) {
        this.cooldownEnabled = true;
        this.cooldownMilliseconds = cooldownMilliseconds;
        this.cooldownAfterRequests = cooldownAfterRequests;
    }

    public void disableCooldown() {
        this.cooldownEnabled = false;
        this.cooldownMilliseconds = 0;
        this.cooldownAfterRequests = 0;
    }

    public void enableRetryAfterTooManyRequests(long retryMilliseconds) {
        this.retryAfterTooManyRequests = true;
        this.tooManyRequestsRetryMilliseconds = retryMilliseconds;
    }

    public void disableRetryAfterTooManyRequests() {
        this.retryAfterTooManyRequests = false;
        this.tooManyRequestsRetryMilliseconds = 0;
    }

}
