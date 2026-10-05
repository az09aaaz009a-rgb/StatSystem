package com.statsystem;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class StatGui implements Listener, InventoryHolder {
    private static final LegacyComponentSerializer L = LegacyComponentSerializer.legacySection();
    private static final int[] SLOTS = {10, 12, 14, 16};
    private final StatSystem plugin;

    public StatGui(StatSystem plugin) {
        this.plugin = plugin;
    }

    @Override
    public Inventory getInventory() {
        return Bukkit.createInventory(this, 27);
    }

    private Inventory build(Player p) {
        Inventory inv = Bukkit.createInventory(this, 27, L.deserialize("§8스탯 (Lv." + plugin.data().get(p.getUniqueId()).level + ")"));
        render(inv, p);
        return inv;
    }

    public void open(Player p) {
        p.openInventory(build(p));
    }

    private void render(Inventory inv, Player p) {
        PlayerData d = plugin.data().get(p.getUniqueId());
        LevelManager lv = plugin.levels();
        ItemStack filler = item(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, filler);

        Material[] mats = {Material.IRON_SWORD, Material.FEATHER, Material.GOLDEN_APPLE, Material.BLAZE_ROD};
        for (Stat s : Stat.values()) {
            int cur = d.get(s);
            List<String> lore = new ArrayList<>();
            lore.add("§7현재: §f" + cur + " §7/ " + lv.maxStat(s));
            lore.add("");
            for (String line : describe(s)) lore.add("§7" + line);
            lore.add("");
            lore.add("§e좌클릭 §f+1   §eShift+좌클릭 §f+10");
            inv.setItem(SLOTS[s.ordinal()], item(mats[s.ordinal()], "§6" + s.label, lore));
        }
        String need = d.level >= lv.maxLevel() ? "§a최대 레벨" : "§7경험치: §f" + d.exp + " / " + lv.need(d.level);
        inv.setItem(22, item(Material.EXPERIENCE_BOTTLE, "§eLv." + d.level,
                List.of(need, "§7남은 스탯 포인트: §e" + lv.available(d))));
    }

    private List<String> describe(Stat s) {
        var c = plugin.getConfig();
        String base = "stats.effects." + s.key + ".";
        switch (s) {
            case STR:
                return List.of("스킬 피해 +" + c.getDouble(base + "skill-damage-per-point") + "% /1",
                        "생명력 흡수 +" + c.getDouble(base + "lifesteal-per-10") + "% /10");
            case DEX:
                return List.of("이동속도 +" + c.getDouble(base + "move-speed-per-point") + "% /1",
                        "크리티컬 확률 +" + c.getDouble(base + "crit-chance-per-point") + "% /1",
                        "크리티컬 피해 +" + c.getDouble(base + "crit-damage-per-point") + "% /1",
                        "공격 회피 +" + c.getDouble(base + "dodge-per-10") + "% /10");
            case VIT:
                return List.of("최대 체력 +" + c.getDouble(base + "max-health-per-point", 1.0) + " /1  (2 = 하트 1칸)",
                        "받는 피해 -" + c.getDouble(base + "damage-reduction-per-point") + "% /1",
                        "피격 시 체력 회복 +" + c.getDouble(base + "on-hit-heal-per-10") + " /10");
            default:
                return List.of("스킬 피해 +" + c.getDouble(base + "skill-damage-per-point") + "% /1",
                        "스킬 쿨타임 -" + c.getDouble(base + "cooldown-reduction-per-10") + "% /10");
        }
    }

    private ItemStack item(Material m, String name, List<String> lore) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        meta.displayName(L.deserialize(name));
        List<Component> lines = new ArrayList<>();
        for (String s : lore) lines.add(L.deserialize(s));
        meta.lore(lines);
        it.setItemMeta(meta);
        return it;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getView().getTopInventory().getHolder() instanceof StatGui)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (e.getClickedInventory() != e.getView().getTopInventory()) return;

        for (Stat s : Stat.values()) {
            if (e.getSlot() == SLOTS[s.ordinal()]) {
                int amount = e.isShiftClick() ? 10 : 1;
                plugin.allocate(p, s, amount);
                render(e.getView().getTopInventory(), p);
                return;
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder() instanceof StatGui) e.setCancelled(true);
    }
}
