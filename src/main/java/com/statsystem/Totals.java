package com.statsystem;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** 한 플레이어의 최종 스탯 효과 (스탯 + 손에 든 무기) */
public class Totals {
    public double skillDamage;      // %
    public double attack;           // 고정 공격력 (무기)
    public double critChance;       // %
    public double critMultiplier;   // 배율 (1.5 = 150%)
    public double lifesteal;        // %
    public double moveSpeed;        // %
    public double dodge;            // %
    public double reduction;        // %
    public double onHitHeal;        // 체력
    public double cooldownReduction;// %

    public static Totals of(StatSystem plugin, Player p) {
        PlayerData d = plugin.data().get(p.getUniqueId());
        int str = d.get(Stat.STR), dex = d.get(Stat.DEX), vit = d.get(Stat.VIT), mag = d.get(Stat.MAG);
        var c = plugin.getConfig();
        ItemStack hand = p.getInventory().getItemInMainHand();

        Totals t = new Totals();
        t.skillDamage = str * c.getDouble("stats.effects.str.skill-damage-per-point", 0.5)
                + mag * c.getDouble("stats.effects.mag.skill-damage-per-point", 0.5)
                + WeaponStat.get(plugin, hand, WeaponStat.SKILL);
        t.attack = WeaponStat.get(plugin, hand, WeaponStat.ATTACK);
        t.critChance = dex * c.getDouble("stats.effects.dex.crit-chance-per-point", 0.2)
                + WeaponStat.get(plugin, hand, WeaponStat.CRIT_CHANCE);
        double critPct = c.getDouble("combat.base-crit-damage-percent", 150)
                + dex * c.getDouble("stats.effects.dex.crit-damage-per-point", 0.5)
                + WeaponStat.get(plugin, hand, WeaponStat.CRIT_DAMAGE);
        t.critMultiplier = critPct / 100.0;
        t.lifesteal = (str / 10) * c.getDouble("stats.effects.str.lifesteal-per-10", 0.5)
                + WeaponStat.get(plugin, hand, WeaponStat.LIFESTEAL);
        t.moveSpeed = dex * c.getDouble("stats.effects.dex.move-speed-per-point", 0.1);
        t.dodge = (dex / 10) * c.getDouble("stats.effects.dex.dodge-per-10", 0.5);
        t.reduction = vit * c.getDouble("stats.effects.vit.damage-reduction-per-point", 0.2);
        t.onHitHeal = (vit / 10) * c.getDouble("stats.effects.vit.on-hit-heal-per-10", 0.5);
        t.cooldownReduction = (mag / 10) * c.getDouble("stats.effects.mag.cooldown-reduction-per-10", 0.5);

        t.critChance = Math.min(t.critChance, c.getDouble("combat.limits.max-crit-chance", 100));
        t.dodge = Math.min(t.dodge, c.getDouble("combat.limits.max-dodge", 60));
        t.reduction = Math.min(t.reduction, c.getDouble("combat.limits.max-damage-reduction", 80));
        t.cooldownReduction = Math.min(t.cooldownReduction, c.getDouble("combat.limits.max-cooldown-reduction", 70));
        t.lifesteal = Math.min(t.lifesteal, c.getDouble("combat.limits.max-lifesteal", 50));
        return t;
    }
}
