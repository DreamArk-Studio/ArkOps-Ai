package com.arkops.manager;

import com.arkops.ArkOpsAi;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

public class LanguageManager {

    private final ArkOpsAi plugin;
    private FileConfiguration langConfig;
    private String currentLang;
    private final Map<String, String> messageCache = new HashMap<>();

    public LanguageManager(ArkOpsAi plugin) {
        this.plugin = plugin;
        loadLanguage();
    }

    private void loadLanguage() {
        this.currentLang = plugin.getConfig().getString("language", "en");
        File langFile = new File(plugin.getDataFolder(), "lang/" + currentLang + ".yml");

        if (!langFile.exists()) {
            plugin.saveResource("lang/" + currentLang + ".yml", false);
        }

        this.langConfig = YamlConfiguration.loadConfiguration(langFile);

        InputStream defaultStream = plugin.getResource("lang/en.yml");
        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
            langConfig.setDefaults(defaultConfig);
        }

        messageCache.clear();
    }

    public String getMessage(String key, Object... args) {
        if (messageCache.containsKey(key)) {
            String cached = messageCache.get(key);
            return args.length > 0 ? MessageFormat.format(cached, args) : cached;
        }

        String message = langConfig.getString(key);
        if (message == null) {
            message = key;
        }

        messageCache.put(key, message);
        return args.length > 0 ? MessageFormat.format(message, args) : message;
    }

    public String getPrefix() {
        return getMessage("prefix");
    }

    public void reload() {
        loadLanguage();
    }

    public String getCurrentLang() {
        return currentLang;
    }
}
