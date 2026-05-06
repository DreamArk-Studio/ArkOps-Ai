package com.arkops.manager;

import com.arkops.ArkOpsAi;
import com.google.gson.JsonObject;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 插件遥测管理器 / Telemetry Manager
 *
 * 功能说明（中文）：
 *   本类用于插件遥测，统计插件使用者数量。
 *   首次启动时生成 UUID v4 并持久化到服务器目录下，
 *   后续每 60 秒向遥测服务器发送一次心跳请求，携带此 UUID。
 *   该数据仅用于统计使用者数量，不会收集任何个人信息。
 *
 * Function description (English):
 *   This class handles plugin telemetry to count the number of active users.
 *   On first startup, a UUID v4 is generated and persisted to the server directory.
 *   A heartbeat request carrying this UUID is sent every 60 seconds to the telemetry server.
 *   This data is solely for counting active users and does NOT collect any personal information.
 */
public class TelemetryManager {

    private static final String HEARTBEAT_URL = "https://tlm.dreamark.club/api/heartbeat.php";
    private static final String UUID_DIR = "DreamArk";
    private static final String UUID_FILE = "server-uuid.txt";

    private final ArkOpsAi plugin;
    private final OkHttpClient client;
    private final ScheduledExecutorService scheduler;
    private String serverUuid;

    public TelemetryManager(ArkOpsAi plugin) {
        this.plugin = plugin;
        int timeout = plugin.getConfig().getInt("openai.timeout", 60);
        this.client = new OkHttpClient.Builder()
                .connectTimeout(timeout, TimeUnit.SECONDS)
                .readTimeout(timeout, TimeUnit.SECONDS)
                .build();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ArkOpsAI-Telemetry");
            t.setDaemon(true);
            return t;
        });
        this.serverUuid = loadOrCreateUuid();
    }

    public void start() {
        // 启动后立即发送一次心跳，之后每 60 秒发送一次
        // Send first heartbeat immediately, then every 60 seconds
        scheduler.scheduleAtFixedRate(this::sendHeartbeat, 0, 60, TimeUnit.SECONDS);
    }

    public void shutdown() {
        scheduler.shutdown();
        client.dispatcher().executorService().shutdown();
        client.connectionPool().evictAll();
    }

    private String loadOrCreateUuid() {
        File serverDir = plugin.getServer().getWorldContainer();
        File uuidDir = new File(serverDir, UUID_DIR);
        File uuidFile = new File(uuidDir, UUID_FILE);

        if (uuidFile.exists()) {
            try {
                String content = Files.readString(uuidFile.toPath(), StandardCharsets.UTF_8).trim();
                UUID.fromString(content);
                return content;
            } catch (Exception e) {
                // UUID file corrupted, regenerate silently
            }
        }

        if (!uuidDir.exists()) {
            uuidDir.mkdirs();
        }

        String newUuid = UUID.randomUUID().toString();
        try {
            Files.writeString(uuidFile.toPath(), newUuid, StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
        return newUuid;
    }

    private void sendHeartbeat() {
        try {
            JsonObject payload = new JsonObject();
            payload.addProperty("uuid", serverUuid);

            RequestBody body = RequestBody.create(
                    payload.toString(),
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url(HEARTBEAT_URL)
                    .header("Content-Type", "application/json")
                    .post(body)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                // 心跳成功时不输出日志以避免刷屏 / Silence on success to avoid log spam
            }
        } catch (IOException e) {
            // 心跳失败静默处理，不干扰服务器运行
            // Silently ignore heartbeat failures to avoid disrupting the server
        }
    }
}
