package com.statsystem;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DataManager {
    private final StatSystem plugin;
    private final File file;
    private YamlConfiguration yaml;
    private final Map<UUID, PlayerData> cache = new HashMap<>();

    public DataManager(StatSystem plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data.yml");
        load();
    }

    private void load() {
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
        yaml = YamlConfiguration.loadConfiguration(file);
    }

    public PlayerData get(UUID id) {
        return cache.computeIfAbsent(id, this::read);
    }

    private PlayerData read(UUID id) {
        PlayerData d = new PlayerData();
        String base = "players." + id;
        d.level = Math.max(1, yaml.getInt(base + ".level", 1));
        d.exp = yaml.getLong(base + ".exp", 0);
        d.bonusPoints = yaml.getInt(base + ".bonus", 0);
        for (Stat s : Stat.values()) {
            d.stats[s.ordinal()] = Math.max(0, yaml.getInt(base + ".stats." + s.key, 0));
        }
        return d;
    }

    private void write(UUID id, PlayerData d) {
        String base = "players." + id;
        yaml.set(base + ".level", d.level);
        yaml.set(base + ".exp", d.exp);
        yaml.set(base + ".bonus", d.bonusPoints);
        for (Stat s : Stat.values()) {
            yaml.set(base + ".stats." + s.key, d.stats[s.ordinal()]);
        }
    }

    public void save() {
        for (Map.Entry<UUID, PlayerData> e : cache.entrySet()) write(e.getKey(), e.getValue());
        try {
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().severe("data.yml 저장 실패: " + ex.getMessage());
        }
    }

    /** 접속 중이 아닌 플레이어까지 모두 스탯 초기화 (분배한 스탯만 0으로, 포인트는 레벨 기준으로 다시 계산됨) */
    public int resetAllStats() {
        save();
        int count = 0;
        ConfigurationSection sec = yaml.getConfigurationSection("players");
        if (sec != null) {
            for (String key : sec.getKeys(false)) {
                for (Stat s : Stat.values()) yaml.set("players." + key + ".stats." + s.key, 0);
                count++;
            }
        }
        try {
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().severe("data.yml 저장 실패: " + ex.getMessage());
        }
        cache.clear();
        load();
        return count;
    }
}
