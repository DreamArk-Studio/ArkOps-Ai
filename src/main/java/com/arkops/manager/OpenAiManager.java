package com.arkops.manager;

import com.arkops.ArkOpsAi;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class OpenAiManager {

    private final ArkOpsAi plugin;
    private final OkHttpClient client;
    private final String apiKey;
    private final String model;
    private final String apiUrl;
    private final int maxTokens;
    private final double temperature;
    private final ExecutorService executorService;

    public OpenAiManager(ArkOpsAi plugin) {
        this.plugin = plugin;
        this.apiKey = plugin.getConfig().getString("openai.api-key", "");
        this.model = plugin.getConfig().getString("openai.model", "qwen3.5-plus");
        this.apiUrl = plugin.getConfig().getString("openai.api-url", "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions");
        this.maxTokens = plugin.getConfig().getInt("openai.max-tokens", 2000);
        this.temperature = plugin.getConfig().getDouble("openai.temperature", 1.0);
        int timeout = plugin.getConfig().getInt("openai.timeout", 60);
        this.client = new OkHttpClient.Builder()
                .connectTimeout(timeout, TimeUnit.SECONDS)
                .readTimeout(timeout * 2, TimeUnit.SECONDS)
                .writeTimeout(timeout, TimeUnit.SECONDS)
                .build();
        this.executorService = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "ArkOpsAI-Async");
            t.setDaemon(true);
            return t;
        });
    }

    public CompletableFuture<String> sendRequest(String userMessage, String systemPrompt) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject messageObject = new JsonObject();
                messageObject.addProperty("role", "system");
                messageObject.addProperty("content", systemPrompt);

                JsonObject userMessageObject = new JsonObject();
                userMessageObject.addProperty("role", "user");
                userMessageObject.addProperty("content", userMessage);

                JsonArray messages = new JsonArray();
                messages.add(messageObject);
                messages.add(userMessageObject);

                JsonObject requestBody = new JsonObject();
                requestBody.addProperty("model", model);
                requestBody.add("messages", messages);
                requestBody.addProperty("max_tokens", maxTokens);
                requestBody.addProperty("temperature", temperature);

                RequestBody body = RequestBody.create(
                        requestBody.toString(),
                        MediaType.parse("application/json")
                );

                Request request = new Request.Builder()
                        .url(apiUrl)
                        .header("Authorization", "Bearer " + apiKey)
                        .header("Content-Type", "application/json")
                        .post(body)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                        plugin.getLogger().severe("ArkOpsAI API 错误: " + response.code() + " - " + errorBody);
                        return "ArkOpsAI 服务暂时不可用，请稍后重试。错误代码: " + response.code();
                    }

                    String responseBody = response.body().string();
                    JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();
                    return jsonResponse.getAsJsonArray("choices")
                            .get(0).getAsJsonObject()
                            .getAsJsonObject("message")
                            .get("content").getAsString();
                }
            } catch (IOException e) {
                plugin.getLogger().severe("ArkOpsAI API 请求失败: " + e.getMessage());
                return "ArkOpsAI 服务连接失败，请检查网络连接。";
            } catch (Exception e) {
                plugin.getLogger().severe("处理 ArkOpsAI 响应时出错: " + e.getMessage());
                return "处理响应时发生错误。";
            }
        }, executorService);
    }

    public CompletableFuture<JsonObject> sendRequestWithTools(String userMessage, String systemPrompt, JsonArray tools) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject systemMessage = new JsonObject();
                systemMessage.addProperty("role", "system");
                systemMessage.addProperty("content", systemPrompt);

                JsonObject userMessageObject = new JsonObject();
                userMessageObject.addProperty("role", "user");
                userMessageObject.addProperty("content", userMessage);

                JsonArray messages = new JsonArray();
                messages.add(systemMessage);
                messages.add(userMessageObject);

                JsonObject requestBody = new JsonObject();
                requestBody.addProperty("model", model);
                requestBody.add("messages", messages);
                requestBody.add("tools", tools);
                requestBody.addProperty("max_tokens", maxTokens);
                requestBody.addProperty("temperature", temperature);

                RequestBody body = RequestBody.create(
                        requestBody.toString(),
                        MediaType.parse("application/json")
                );

                Request request = new Request.Builder()
                        .url(apiUrl)
                        .header("Authorization", "Bearer " + apiKey)
                        .header("Content-Type", "application/json")
                        .post(body)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                        plugin.getLogger().severe("ArkOpsAI API 错误: " + response.code() + " - " + errorBody);
                        JsonObject error = new JsonObject();
                        error.addProperty("error", "API 错误: " + response.code());
                        return error;
                    }

                    String responseBody = response.body().string();
                    return JsonParser.parseString(responseBody).getAsJsonObject();
                }
            } catch (IOException e) {
                plugin.getLogger().severe("ArkOpsAI API 请求失败: " + e.getMessage());
                JsonObject error = new JsonObject();
                error.addProperty("error", "连接失败: " + e.getMessage());
                return error;
            } catch (Exception e) {
                plugin.getLogger().severe("处理 ArkOpsAI 响应时出错: " + e.getMessage());
                JsonObject error = new JsonObject();
                error.addProperty("error", "处理错误: " + e.getMessage());
                return error;
            }
        }, executorService);
    }

    public JsonObject sendRequestWithMessagesSync(JsonArray messages, JsonArray tools) {
        try {
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("model", model);
            requestBody.add("messages", messages);
            requestBody.add("tools", tools);
            requestBody.addProperty("max_tokens", maxTokens);
            requestBody.addProperty("temperature", temperature);

            RequestBody body = RequestBody.create(
                    requestBody.toString(),
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url(apiUrl)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .post(body)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                    plugin.getLogger().severe("ArkOpsAI API 错误: " + response.code() + " - " + errorBody);
                    JsonObject error = new JsonObject();
                    error.addProperty("error", "API 错误: " + response.code());
                    return error;
                }

                String responseBody = response.body().string();
                return JsonParser.parseString(responseBody).getAsJsonObject();
            }
        } catch (IOException e) {
            plugin.getLogger().severe("ArkOpsAI API 请求失败: " + e.getMessage());
            JsonObject error = new JsonObject();
            error.addProperty("error", "连接失败: " + e.getMessage());
            return error;
        } catch (Exception e) {
            plugin.getLogger().severe("处理 ArkOpsAI 响应时出错: " + e.getMessage());
            JsonObject error = new JsonObject();
            error.addProperty("error", "处理错误: " + e.getMessage());
            return error;
        }
    }

    public CompletableFuture<JsonObject> sendRequestWithMessages(JsonArray messages, JsonArray tools) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject requestBody = new JsonObject();
                requestBody.addProperty("model", model);
                requestBody.add("messages", messages);
                requestBody.add("tools", tools);
                requestBody.addProperty("max_tokens", maxTokens);
                requestBody.addProperty("temperature", temperature);

                RequestBody body = RequestBody.create(
                        requestBody.toString(),
                        MediaType.parse("application/json")
                );

                Request request = new Request.Builder()
                        .url(apiUrl)
                        .header("Authorization", "Bearer " + apiKey)
                        .header("Content-Type", "application/json")
                        .post(body)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                        plugin.getLogger().severe("ArkOpsAI API 错误: " + response.code() + " - " + errorBody);
                        JsonObject error = new JsonObject();
                        error.addProperty("error", "API 错误: " + response.code());
                        return error;
                    }

                    String responseBody = response.body().string();
                    return JsonParser.parseString(responseBody).getAsJsonObject();
                }
            } catch (IOException e) {
                plugin.getLogger().severe("ArkOpsAI API 请求失败: " + e.getMessage());
                JsonObject error = new JsonObject();
                error.addProperty("error", "连接失败: " + e.getMessage());
                return error;
            } catch (Exception e) {
                plugin.getLogger().severe("处理 ArkOpsAI 响应时出错: " + e.getMessage());
                JsonObject error = new JsonObject();
                error.addProperty("error", "处理错误: " + e.getMessage());
                return error;
            }
        }, executorService);
    }

    public void shutdown() {
        executorService.shutdown();
        client.dispatcher().executorService().shutdown();
        client.connectionPool().evictAll();
    }
}
