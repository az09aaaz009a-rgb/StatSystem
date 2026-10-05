package com.statsystem;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/** 무기에 붙는 스탯 종류 */
public enum WeaponStat {
    SKILL("skill", "스킬퍼뎀", "스킬 피해", "%"),
    ATTACK("attack", "공격력", "공격력", ""),
    CRIT_CHANCE("critchance", "크확", "크리티컬 확률", "%"),
    CRIT_DAMAGE("critdmg", "크뎀", "크리티컬 피해", "%"),
    LIFESTEAL("lifesteal", "생명력흡수", "생명력 흡수", "%");

    public final String key;
    public final String alias;   // 명령어에서 쓰는 한글 이름
    public final String label;   // 설명에 표시되는 이름
    public final String unit;

    WeaponStat(String key, String alias, String label, String unit) {
        this.key = key;
        this.alias = alias;
        this.label = label;
        this.unit = unit;
    }

    public static WeaponStat parse(String s) {
        if (s == null) return null;
        for (WeaponStat w : values()) {
            if (w.key.equalsIgnoreCase(s) || w.alias.equals(s) || w.label.equals(s)) return w;
        }
        return null;
    }

    private static final String LORE_MARK = "§8[스탯]";
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private NamespacedKey nk(StatSystem plugin) {
        return new NamespacedKey(plugin, "w_" + key);
    }

    public static double get(StatSystem plugin, ItemStack item, WeaponStat w) {
        if (item == null || !item.hasItemMeta()) return 0;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        Double v = pdc.get(w.nk(plugin), PersistentDataType.DOUBLE);
        return v == null ? 0 : v;
    }

    public static void set(StatSystem plugin, ItemStack item, WeaponStat w, double value) {
        ItemMeta meta = item.getItemMeta();
        if (value == 0) meta.getPersistentDataContainer().remove(w.nk(plugin));
        else meta.getPersistentDataContainer().set(w.nk(plugin), PersistentDataType.DOUBLE, value);
        item.setItemMeta(meta);
        refreshLore(plugin, item);
    }

    public static void clear(StatSystem plugin, ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        for (WeaponStat w : values()) meta.getPersistentDataContainer().remove(w.nk(plugin));
        item.setItemMeta(meta);
        refreshLore(plugin, item);
    }

    public static void refreshLore(StatSystem plugin, ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        List<Component> old = meta.lore();
        List<Component> lore = new ArrayList<>();
        if (old != null) {
            for (Component c : old) {
                if (!LEGACY.serialize(c).startsWith(LORE_MARK)) lore.add(c);
            }
        }
        for (WeaponStat w : values()) {
            double v = get(plugin, item, w);
            if (v == 0) continue;
            String num = (v == Math.floor(v)) ? String.valueOf((long) v) : String.format("%.1f", v);
            lore.add(LEGACY.deserialize(LORE_MARK + " §7" + w.label + " §c+" + num + w.unit));
        }
        meta.lore(lore.isEmpty() ? null : lore);
        item.setItemMeta(meta);
    }
}
