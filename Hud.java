package com.statsystem;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 하단(액션바)에 레벨/경험치와 쿨타임을 한 줄로 표시. 들고 있는 아이템별로 다른 문구 사용 가능 */
public class Hud extends BukkitRunnable {
    private static final LegacyComponentSerializer AMP = LegacyComponentSerializer.legacyAmpersand();
    private static final Pattern VAR = Pattern.compile("\\{var:([A-Za-z0-9_]+)\\}");
    private final StatSystem plugin;

    public Hud(StatSystem plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        if (!plugin.getConfig().getBoolean("hud.enabled", true)) return;
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            ItemStack hand = p.getInventory().getItemInMainHand();
            String fmt = plugin.getConfig().getString("hud.formats." + hand.getType().name());
            if (fmt == null) fmt = plugin.getConfig().getString("hud.formats.default", "");
            if (fmt.isEmpty()) continue;

            PlayerData d = plugin.data().get(p.getUniqueId());
            LevelManager lv = plugin.levels();
            boolean max = d.level >= lv.maxLevel();
            String out = fmt
                    .replace("{level}", String.valueOf(d.level))
                    .replace("{exp}", max ? "MAX" : String.valueOf(d.exp))
                    .replace("{need}", max ? "MAX" : String.valueOf(lv.need(d.level)))
                    .replace("{points}", String.valueOf(lv.available(d)));

            Matcher m = VAR.matcher(out);
            StringBuffer sb = new StringBuffer();
            while (m.find()) {
                double v = plugin.hook().getVariable(p, m.group(1));
                m.appendReplacement(sb, String.valueOf((long) Math.ceil(v)));
            }
            m.appendTail(sb);
            p.sendActionBar(AMP.deserialize(sb.toString()));
        }
    }
}
