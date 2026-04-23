package com.arkops.util;

import com.arkops.ArkOpsAi;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Logger {

    private final ArkOpsAi plugin;
    private final File logFile;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public Logger(ArkOpsAi plugin) {
        this.plugin = plugin;
        this.logFile = new File(plugin.getDataFolder(), "logs");
        if (!this.logFile.exists()) {
            this.logFile.mkdirs();
        }
    }

    public void info(String message) {
        plugin.getLogger().info(message);
        writeLog("INFO", message);
    }

    public void warning(String message) {
        plugin.getLogger().warning(message);
        writeLog("WARNING", message);
    }

    public void severe(String message) {
        plugin.getLogger().severe(message);
        writeLog("SEVERE", message);
    }

    public void logAction(String executor, String action, String result) {
        String message = String.format("[操作] 执行者: %s | 操作: %s | 结果: %s", executor, action, result);
        info(message);
    }

    private void writeLog(String level, String message) {
        try {
            String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            File currentLogFile = new File(logFile, "arkops-" + dateStr + ".log");

            try (FileWriter fw = new FileWriter(currentLogFile, true);
                 PrintWriter pw = new PrintWriter(fw)) {
                String timestamp = LocalDateTime.now().format(FORMATTER);
                pw.println("[" + timestamp + "] [" + level + "] " + message);
            }
        } catch (IOException e) {
            plugin.getLogger().severe("无法写入日志文件: " + e.getMessage());
        }
    }
}
