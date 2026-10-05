package com.statsystem;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.concurrent.ThreadLocalRandom;

/** 때린 곳 위에 떠오르는 데미지 숫자 (때린 본인에게만 보임) */
public class DamageIndicator {
    private static final LegacyComponentSerializer L = LegacyComponentSerializer.legacySection();

    public static void show(StatSystem plugin, Player viewer, Entity target, double damage, boolean crit) {
        if (!plugin.getConfig().getBoolean("damage-indicator.enabled", true)) return;
        ThreadLocalRandom r = ThreadLocalRandom.current();
        Location loc = target.getLocation().add(r.nextDouble(-0.4, 0.4), target.getHeight() + 0.2, r.nextDouble(-0.4, 0.4));

        String num = String.valueOf(Math.round(damage));
        String text = crit ? "§6§l" + num : "§c" + num;
        float scale = crit ? 1.8f : 1.2f;

        TextDisplay td = loc.getWorld().spawn(loc, TextDisplay.class, d -> {
            d.text(L.deserialize(text));
            d.setBillboard(Display.Billboard.CENTER);
            d.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
            d.setShadowed(true);
            d.setPersistent(false);
            d.setVisibleByDefault(false);
            d.setInterpolationDelay(0);
            d.setInterpolationDuration(15);
            d.setTransformation(new Transformation(new Vector3f(0, 0, 0), new AxisAngle4f(), new Vector3f(scale, scale, scale), new AxisAngle4f()));
        });
        viewer.showEntity(plugin, td);

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (td.isValid()) {
                td.setInterpolationDelay(0);
                td.setInterpolationDuration(15);
                td.setTransformation(new Transformation(new Vector3f(0, 0.9f, 0), new AxisAngle4f(), new Vector3f(scale, scale, scale), new AxisAngle4f()));
            }
        }, 1L);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (td.isValid()) td.remove();
        }, 22L);
    }
}
