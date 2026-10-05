package com.statsystem;

import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;

/** 이동속도처럼 속성(Attribute)으로 적용되는 스탯 효과 */
public class Effects {
    private final StatSystem plugin;
    private final NamespacedKey speedKey;
    private final NamespacedKey healthKey;

    public Effects(StatSystem plugin) {
        this.plugin = plugin;
        this.speedKey = new NamespacedKey(plugin, "dex_move_speed");
        this.healthKey = new NamespacedKey(plugin, "vit_max_health");
    }

    public void apply(Player p) {
        applyHealth(p);
        AttributeInstance attr = p.getAttribute(Attribute.MOVEMENT_SPEED);
        if (attr == null) return;
        for (AttributeModifier m : attr.getModifiers().toArray(new AttributeModifier[0])) {
            if (m.getKey().equals(speedKey)) attr.removeModifier(m);
        }
        PlayerData d = plugin.data().get(p.getUniqueId());
        double pct = d.get(Stat.DEX) * plugin.getConfig().getDouble("stats.effects.dex.move-speed-per-point", 0.1);
        if (pct > 0) {
            attr.addModifier(new AttributeModifier(speedKey, pct / 100.0, AttributeModifier.Operation.ADD_SCALAR));
        }
    }

    /** 체력 스탯: 최대 체력 증가 */
    private void applyHealth(Player p) {
        try {
            AttributeInstance attr = p.getAttribute(Attribute.MAX_HEALTH);
            if (attr == null) {
                plugin.getLogger().warning("MAX_HEALTH 속성을 찾지 못했습니다: " + p.getName());
                return;
            }
            for (AttributeModifier m : attr.getModifiers().toArray(new AttributeModifier[0])) {
                if (m.getKey().equals(healthKey)) attr.removeModifier(m);
            }
            PlayerData d = plugin.data().get(p.getUniqueId());
            double bonus = d.get(Stat.VIT) * plugin.getConfig().getDouble("stats.effects.vit.max-health-per-point", 1.0);
            if (bonus > 0) {
                attr.addModifier(new AttributeModifier(healthKey, bonus, AttributeModifier.Operation.ADD_NUMBER));
            }
            if (p.getHealth() > attr.getValue()) p.setHealth(attr.getValue());
        } catch (Exception ex) {
            plugin.getLogger().severe("최대 체력 적용 실패 (" + p.getName() + "): " + ex);
        }
    }

    public void applyAll() {
        for (Player p : plugin.getServer().getOnlinePlayers()) apply(p);
    }
}
