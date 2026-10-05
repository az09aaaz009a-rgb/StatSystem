package com.statsystem;

import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class LevelManager {
    private final StatSystem plugin;

    private File tableFile;
    private YamlConfiguration table;

    public LevelManager(StatSystem plugin) {
        this.plugin = plugin;
        loadTable();
    }

    /** levels.yml (레벨별 필요 경험치 / 지급 포인트 표). 없으면 config 공식으로 자동 생성 */
    public void loadTable() {
        tableFile = new File(plugin.getDataFolder(), "levels.yml");
        if (!tableFile.exists()) generateTable();
        table = YamlConfiguration.loadConfiguration(tableFile);
    }

    /** config 의 공식으로 levels.yml 을 새로 만듦 (기존 파일은 덮어씀) */
    public void generateTable() {
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
        StringBuilder sb = new StringBuilder();
        sb.append("# ============================================================\n");
        sb.append("# 레벨 표 - 메모장으로 바로 수정할 수 있습니다. 저장 후 /스탯 리로드\n");
        sb.append("#   exp    : 해당 레벨에서 다음 레벨로 올라가기 위한 필요 경험치\n");
        sb.append("#   points : 해당 레벨에 도달했을 때 받는 스탯 포인트 (1레벨은 시작이라 0)\n");
        sb.append("# 표에 없는 레벨은 config.yml 의 공식을 사용합니다.\n");
        sb.append("# 이 파일을 지우면 config.yml 공식으로 다시 만들어집니다.\n");
        sb.append("# ============================================================\n");
        sb.append("levels:\n");
        for (int l = 1; l <= maxLevel(); l++) {
            sb.append("  ").append(l).append(": { exp: ").append(needFormula(l))
                    .append(", points: ").append(l == 1 ? 0 : pointsFormula(l)).append(" }\n");
        }
        try {
            Files.write(tableFile.toPath(), sb.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            plugin.getLogger().warning("levels.yml 생성 실패: " + e.getMessage());
        }
    }

    public int maxLevel() {
        return Math.max(1, plugin.getConfig().getInt("leveling.max-level", 100));
    }

    public int maxStat(Stat s) {
        return Math.max(0, plugin.getConfig().getInt("stats.max." + s.key, 100));
    }

    /** 레벨 L -> L+1 에 필요한 경험치 */
    public long need(int level) {
        if (table != null && table.contains("levels." + level + ".exp")) {
            return Math.max(1, table.getLong("levels." + level + ".exp"));
        }
        return needFormula(level);
    }

    private long needFormula(int level) {
        String ov = "leveling.exp.overrides." + level;
        if (plugin.getConfig().contains(ov)) return Math.max(1, plugin.getConfig().getLong(ov));
        double base = plugin.getConfig().getDouble("leveling.exp.base", 100);
        double growth = plugin.getConfig().getDouble("leveling.exp.growth", 50);
        double quad = plugin.getConfig().getDouble("leveling.exp.quadratic", 0);
        double v = base + growth * (level - 1) + quad * (level - 1) * (level - 1);
        return Math.max(1, Math.round(v));
    }

    /** 해당 레벨에 도달했을 때 받는 스탯 포인트 */
    public int pointsFor(int level) {
        if (table != null && table.contains("levels." + level + ".points")) {
            return Math.max(0, table.getInt("levels." + level + ".points"));
        }
        return pointsFormula(level);
    }

    private int pointsFormula(int level) {
        String ov = "leveling.stat-points-overrides." + level;
        if (plugin.getConfig().contains(ov)) return Math.max(0, plugin.getConfig().getInt(ov));
        return Math.max(0, plugin.getConfig().getInt("leveling.stat-points-per-level", 3));
    }

    public int totalPoints(PlayerData d) {
        int sum = d.bonusPoints;
        for (int l = 2; l <= d.level; l++) sum += pointsFor(l);
        return sum;
    }

    public int available(PlayerData d) {
        return totalPoints(d) - d.spent();
    }

    /** 설정 변경 등으로 분배한 스탯이 총 포인트를 넘으면 자동 초기화 */
    public void reconcile(PlayerData d) {
        boolean over = d.spent() > totalPoints(d);
        for (Stat s : Stat.values()) {
            if (d.get(s) > maxStat(s)) over = true;
        }
        if (over) d.resetStats();
        if (d.level > maxLevel()) d.level = maxLevel();
    }

    public void addExp(Player p, long amount) {
        if (amount <= 0) return;
        PlayerData d = plugin.data().get(p.getUniqueId());
        if (d.level >= maxLevel()) return;
        d.exp += amount;
        boolean up = false;
        while (d.level < maxLevel() && d.exp >= need(d.level)) {
            d.exp -= need(d.level);
            d.level++;
            up = true;
        }
        if (d.level >= maxLevel()) d.exp = 0;
        syncBar(p);
        if (up) {
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
            p.sendTitle("§6레벨 업!", "§fLv." + d.level + "  §7남은 스탯 포인트 §e" + available(d), 5, 40, 10);
            p.sendMessage("§6[스탯] §f레벨 §e" + d.level + " §f달성! 남은 스탯 포인트: §e" + available(d) + " §7(/스탯)");
        }
    }

    public void setLevel(Player p, int level) {
        PlayerData d = plugin.data().get(p.getUniqueId());
        d.level = Math.max(1, Math.min(maxLevel(), level));
        d.exp = 0;
        reconcile(d);
        plugin.effects().apply(p);
        syncBar(p);
    }

    /** 마인크래프트 경험치 바(레벨 숫자 + 진행도)에 스탯 시스템 레벨/경험치를 표시 */
    public void syncBar(Player p) {
        if (!plugin.getConfig().getBoolean("display.use-vanilla-xp-bar", true)) return;
        PlayerData d = plugin.data().get(p.getUniqueId());
        p.setLevel(d.level);
        float progress = d.level >= maxLevel() ? 1f : (float) Math.min(1.0, (double) d.exp / need(d.level));
        p.setExp(Math.max(0f, Math.min(1f, progress)));
    }
}
