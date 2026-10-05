package com.statsystem;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * MagicSpells 연동 (리플렉션 사용: MagicSpells 파일 없이도 빌드됩니다)
 *  - 스킬 피해 표시: SpellApplyDamageEvent 를 감지해 "이 피해는 스킬 피해" 라고 표시
 *  - 쿨타임 감소: SpellCastEvent 의 쿨타임을 마법 스탯만큼 줄임
 */
public class MagicSpellsHook implements Listener {
    private final StatSystem plugin;
    private boolean enabled = false;

    // "공격자:대상" -> 표시된 서버 틱
    private final Map<String, Integer> skillMarks = new HashMap<>();

    public MagicSpellsHook(StatSystem plugin) {
        this.plugin = plugin;
    }

    @SuppressWarnings("unchecked")
    public void hook() {
        Plugin ms = Bukkit.getPluginManager().getPlugin("MagicSpells");
        if (ms == null || !ms.isEnabled()) {
            plugin.getLogger().warning("MagicSpells 를 찾지 못했습니다. 스킬 피해/쿨타임 감소 연동은 꺼집니다.");
            return;
        }
        ClassLoader cl = ms.getClass().getClassLoader();
        try {
            Class<? extends Event> dmg = (Class<? extends Event>) Class.forName("com.nisovin.magicspells.events.SpellApplyDamageEvent", true, cl);
            Method getCaster = dmg.getMethod("getCaster");
            Method getTarget = dmg.getMethod("getTarget");
            Bukkit.getPluginManager().registerEvent(dmg, this, EventPriority.LOWEST, (l, ev) -> {
                try {
                    if (!dmg.isInstance(ev)) return;
                    Object caster = getCaster.invoke(ev);
                    Object target = getTarget.invoke(ev);
                    if (caster instanceof Player && target instanceof Entity) {
                        skillMarks.put(((Player) caster).getUniqueId() + ":" + ((Entity) target).getUniqueId(),
                                Bukkit.getCurrentTick());
                    }
                } catch (Exception ignored) {
                }
            }, plugin, false);
            enabled = true;
            plugin.getLogger().info("MagicSpells 스킬 피해 연동 완료");
        } catch (Exception ex) {
            plugin.getLogger().warning("SpellApplyDamageEvent 연동 실패: " + ex);
        }

        try {
            Class<? extends Event> cast = (Class<? extends Event>) Class.forName("com.nisovin.magicspells.events.SpellCastEvent", true, cl);
            Method getCaster = cast.getMethod("getCaster");
            Method getCooldown = cast.getMethod("getCooldown");
            Method setCooldown = cast.getMethod("setCooldown", float.class);
            Bukkit.getPluginManager().registerEvent(cast, this, EventPriority.NORMAL, (l, ev) -> {
                try {
                    if (!cast.isInstance(ev)) return;
                    Object caster = getCaster.invoke(ev);
                    if (!(caster instanceof Player p)) return;
                    float cd = (Float) getCooldown.invoke(ev);
                    if (cd <= 0) return;
                    double cdr = Totals.of(plugin, p).cooldownReduction;
                    if (cdr <= 0) return;
                    setCooldown.invoke(ev, (float) (cd * (1.0 - cdr / 100.0)));
                } catch (Exception ignored) {
                }
            }, plugin, false);
            plugin.getLogger().info("MagicSpells 쿨타임 감소 연동 완료");
        } catch (Exception ex) {
            plugin.getLogger().warning("SpellCastEvent 연동 실패: " + ex);
        }
    }

    private Object varManager;
    private Method varGetter;
    private boolean varWarned = false;

    /** MagicSpells 변수 값 읽기 (쿨타임 표시용). 실패하면 0 */
    public double getVariable(Player p, String name) {
        try {
            if (varGetter == null) {
                Plugin ms = Bukkit.getPluginManager().getPlugin("MagicSpells");
                if (ms == null) return 0;
                Class<?> msCls = Class.forName("com.nisovin.magicspells.MagicSpells", true, ms.getClass().getClassLoader());
                varManager = msCls.getMethod("getVariableManager").invoke(null);
                for (Method m : varManager.getClass().getMethods()) {
                    if (m.getName().equals("getValue") && m.getParameterCount() == 2 && m.getParameterTypes()[0] == String.class) {
                        varGetter = m;
                        break;
                    }
                }
                if (varGetter == null) throw new IllegalStateException("getValue 메서드를 찾지 못함");
            }
            Class<?> second = varGetter.getParameterTypes()[1];
            Object arg = second == String.class ? p.getName() : p;
            Object r = varGetter.invoke(varManager, name, arg);
            return r instanceof Number ? ((Number) r).doubleValue() : 0;
        } catch (Exception ex) {
            if (!varWarned) {
                varWarned = true;
                plugin.getLogger().warning("MagicSpells 변수 읽기 실패 (쿨타임 표시가 0으로 나옵니다): " + ex);
            }
            return 0;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    /** 방금 들어온 피해가 MagicSpells 스킬 피해인지 확인하고 표시를 지움 */
    public boolean consumeSkill(UUID attacker, UUID target) {
        Integer tick = skillMarks.remove(attacker + ":" + target);
        if (tick == null) return false;
        return Bukkit.getCurrentTick() - tick <= 1;
    }

    public void cleanup() {
        int now = Bukkit.getCurrentTick();
        skillMarks.values().removeIf(t -> now - t > 20);
    }
}
