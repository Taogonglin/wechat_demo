package com.company.wechat.util;

import com.google.gson.Gson;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * HTTP请求工具类
 * 
 * @author Company
 */
public class HttpUtil {

    private static final Logger logger = LoggerFactory.getLogger(HttpUtil.class);
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final Gson gson = new Gson();

    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    /**
     * GET请求
     */
    public static String doGet(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("请求失败: " + response);
            }
            String result = response.body().string();
            logger.debug("GET请求: {}, 响应: {}", url, result);
            return result;
        }
    }

    /**
     * POST请求（JSON格式）
     */
    public static String doPostJson(String url, Object data) throws IOException {
        String jsonData = gson.toJson(data);
        RequestBody body = RequestBody.create(jsonData, JSON);

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("请求失败: " + response);
            }
            String result = response.body().string();
            logger.debug("POST请求: {}, 参数: {}, 响应: {}", url, jsonData, result);
            return result;
        }
    }

    /**
     * POST请求（String格式）
     */
    public static String doPostString(String url, String data) throws IOException {
        RequestBody body = RequestBody.create(data, JSON);

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("请求失败: " + response);
            }
            String result = response.body().string();
            logger.debug("POST请求: {}, 参数: {}, 响应: {}", url, data, result);
            return result;
        }
    }
}
