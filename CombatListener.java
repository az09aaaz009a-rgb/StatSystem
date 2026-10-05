package com.statsystem;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.util.concurrent.ThreadLocalRandom;

public class CombatListener implements Listener {
    private final StatSystem plugin;
    private final java.util.Set<java.util.UUID> critFlags = new java.util.HashSet<>();

    public CombatListener(StatSystem plugin) {
        this.plugin = plugin;
    }

    private Player attackerOf(Entity damager) {
        if (damager instanceof Player p) return p;
        if (damager instanceof Projectile proj) {
            ProjectileSource src = proj.getShooter();
            if (src instanceof Player p) return p;
        }
        return null;
    }

    private boolean roll(double percent) {
        return percent > 0 && ThreadLocalRandom.current().nextDouble() * 100.0 < percent;
    }

    /** 공격자 쪽: 스킬 퍼뎀 / 공격력 / 크리티컬 */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent e) {
        Player attacker = attackerOf(e.getDamager());
        if (attacker == null) return;

        Totals t = Totals.of(plugin, attacker);
        double dmg = e.getDamage();

        boolean skill = plugin.hook().consumeSkill(attacker.getUniqueId(), e.getEntity().getUniqueId());
        if (skill) {
            dmg *= 1.0 + t.skillDamage / 100.0;
        } else {
            dmg += t.attack;
        }

        if (roll(t.critChance)) {
            dmg *= t.critMultiplier;
            attacker.sendMessage("§c크리티컬");
            critFlags.add(e.getEntity().getUniqueId());
            attacker.playSound(attacker.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_ATTACK_CRIT, 1f, 1f);
        }
        e.setDamage(dmg);
    }

    /** 피해자(플레이어) 쪽: 회피 / 받는 피해 감소 */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onVictim(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player victim)) return;
        Totals t = Totals.of(plugin, victim);

        if (e instanceof EntityDamageByEntityEvent && roll(t.dodge)) {
            e.setCancelled(true);
            victim.sendActionBar(net.kyori.adventure.text.Component.text("§b회피!"));
            return;
        }
        if (t.reduction > 0) {
            e.setDamage(e.getDamage() * (1.0 - t.reduction / 100.0));
        }
    }

    /** 실제로 들어간 피해 기준: 생명력 흡수 / 피격 시 체력 회복 */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamaged(EntityDamageByEntityEvent e) {
        double finalDamage = e.getFinalDamage();
        boolean crit = critFlags.remove(e.getEntity().getUniqueId());
        if (finalDamage <= 0) return;
        Player shower = attackerOf(e.getDamager());
        if (shower != null) DamageIndicator.show(plugin, shower, e.getEntity(), finalDamage, crit);

        Player attacker = attackerOf(e.getDamager());
        if (attacker != null && !attacker.isDead()) {
            double ls = Totals.of(plugin, attacker).lifesteal;
            if (ls > 0) heal(attacker, finalDamage * ls / 100.0);
        }
        if (e.getEntity() instanceof Player victim && !victim.isDead()) {
            double heal = Totals.of(plugin, victim).onHitHeal;
            if (heal > 0) heal(victim, heal);
        }
    }

    private void heal(Player p, double amount) {
        AttributeInstance max = p.getAttribute(Attribute.MAX_HEALTH);
        double cap = max == null ? 20 : max.getValue();
        p.setHealth(Math.min(cap, p.getHealth() + amount));
    }

    /** 바닐라 경험치(구슬 획득)를 스탯 시스템 경험치로 변환. 바닐라 경험치는 올라가지 않음 */
    @EventHandler
    public void onVanillaExp(PlayerExpChangeEvent e) {
        int amount = e.getAmount();
        if (amount <= 0) return;
        double mul = plugin.getConfig().getDouble("leveling.vanilla-exp-multiplier", 1.0);
        long gain = Math.round(amount * mul);
        e.setAmount(0);
        if (gain > 0) plugin.levels().addExp(e.getPlayer(), gain);
    }

    /** 죽어도 레벨 표시가 사라지지 않게 */
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        e.setKeepLevel(true);
        e.setDroppedExp(0);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        PlayerData d = plugin.data().get(e.getPlayer().getUniqueId());
        plugin.levels().reconcile(d);
        plugin.effects().apply(e.getPlayer());
        plugin.levels().syncBar(e.getPlayer());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            plugin.effects().apply(e.getPlayer());
            plugin.levels().syncBar(e.getPlayer());
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        plugin.data().save();
    }
}
