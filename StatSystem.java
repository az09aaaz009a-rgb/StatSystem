package com.statsystem;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class StatSystem extends JavaPlugin {
    private DataManager data;
    private LevelManager levels;
    private Effects effects;
    private MagicSpellsHook hook;
    private StatGui gui;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        data = new DataManager(this);
        levels = new LevelManager(this);
        effects = new Effects(this);
        hook = new MagicSpellsHook(this);
        gui = new StatGui(this);

        Bukkit.getPluginManager().registerEvents(new CombatListener(this), this);
        Bukkit.getPluginManager().registerEvents(gui, this);

        StatCommands cmds = new StatCommands(this);
        for (String name : new String[]{"stat", "levelset", "giveexp", "givepoint", "statweapon"}) {
            PluginCommand pc = getCommand(name);
            if (pc != null) {
                pc.setExecutor(cmds);
                pc.setTabCompleter(cmds);
            }
        }

        // MagicSpells 가 모두 로드된 뒤 연동
        Bukkit.getScheduler().runTask(this, () -> hook.hook());
        // 5분마다 저장, 10초마다 정리
        Bukkit.getScheduler().runTaskTimer(this, () -> data.save(), 20L * 300, 20L * 300);
        Bukkit.getScheduler().runTaskTimer(this, () -> hook.cleanup(), 200L, 200L);

        new Hud(this).runTaskTimer(this, 20L, Math.max(2L, getConfig().getLong("hud.interval-ticks", 10L)));

        for (Player p : Bukkit.getOnlinePlayers()) {
            levels.reconcile(data.get(p.getUniqueId()));
            effects.apply(p);
        }
        getLogger().info("StatSystem 활성화");
    }

    @Override
    public void onDisable() {
        if (data != null) data.save();
    }

    public DataManager data() { return data; }
    public LevelManager levels() { return levels; }
    public Effects effects() { return effects; }
    public MagicSpellsHook hook() { return hook; }
    public StatGui gui() { return gui; }

    /** 스탯 포인트 분배 (GUI/명령어 공통) */
    public void allocate(Player p, Stat s, int amount) {
        PlayerData d = data.get(p.getUniqueId());
        int avail = levels.available(d);
        int room = levels.maxStat(s) - d.get(s);
        if (avail <= 0) {
            p.sendMessage("§c[스탯] 남은 스탯 포인트가 없습니다.");
            return;
        }
        if (room <= 0) {
            p.sendMessage("§c[스탯] " + s.label + " 은(는) 이미 최대치입니다.");
            return;
        }
        int add = Math.min(amount, Math.min(avail, room));
        d.stats[s.ordinal()] += add;
        effects.apply(p);
        p.sendMessage("§a[스탯] " + s.label + " +" + add + " §7(현재 " + d.get(s) + ", 남은 포인트 " + levels.available(d) + ")");
    }

    public void reload() {
        reloadConfig();
        levels.loadTable();
        for (Player p : Bukkit.getOnlinePlayers()) levels.reconcile(data.get(p.getUniqueId()));
        effects.applyAll();
    }
}
