package com.statsystem;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class LevelManager {
    private final StatSystem plugin;

    public LevelManager(StatSystem plugin) {
        this.plugin = plugin;
    }

    public int maxLevel() {
        return Math.max(1, plugin.getConfig().getInt("leveling.max-level", 100));
    }

    public int maxStat(Stat s) {
        return Math.max(0, plugin.getConfig().getInt("stats.max." + s.key, 100));
    }

    /** 레벨 L -> L+1 에 필요한 경험치 */
    public long need(int level) {
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
    }
}
