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

    public Effects(StatSystem plugin) {
        this.plugin = plugin;
        this.speedKey = new NamespacedKey(plugin, "dex_move_speed");
    }

    public void apply(Player p) {
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

    public void applyAll() {
        for (Player p : plugin.getServer().getOnlinePlayers()) apply(p);
    }
}
