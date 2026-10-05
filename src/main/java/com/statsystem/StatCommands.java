package com.statsystem;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class StatCommands implements CommandExecutor, TabCompleter {
    private static final String ADMIN = "statsystem.admin";
    private final StatSystem plugin;

    public StatCommands(StatSystem plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        switch (cmd.getName().toLowerCase()) {
            case "stat": return stat(sender, args);
            case "levelset": return levelSet(sender, args);
            case "giveexp": return giveExp(sender, args);
            case "givepoint": return givePoint(sender, args);
            case "statweapon": return weapon(sender, args);
            default: return false;
        }
    }

    private boolean admin(CommandSender s) {
        if (s.hasPermission(ADMIN)) return true;
        s.sendMessage("§c권한이 없습니다.");
        return false;
    }

    private Player find(CommandSender s, String name) {
        Player p = Bukkit.getPlayerExact(name);
        if (p == null) s.sendMessage("§c접속 중인 플레이어를 찾을 수 없습니다: " + name);
        return p;
    }

    private Integer num(CommandSender s, String text) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            s.sendMessage("§c숫자를 입력하세요: " + text);
            return null;
        }
    }

    // ---------------- /스탯 ----------------
    private boolean stat(CommandSender sender, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player p) plugin.gui().open(p);
            else sender.sendMessage("게임 안에서만 열 수 있습니다. /스탯 정보 <플레이어> 를 쓰세요.");
            return true;
        }
        switch (args[0]) {
            case "정보": case "info": {
                Player target = (args.length >= 2 && sender.hasPermission(ADMIN)) ? find(sender, args[1])
                        : (sender instanceof Player p ? p : null);
                if (target == null) return true;
                info(sender, target);
                return true;
            }
            case "올리기": case "분배": case "add": {
                if (!(sender instanceof Player p)) return true;
                if (args.length < 2) {
                    p.sendMessage("§e사용법: /스탯 올리기 <힘|민첩|체력|마법> [수]");
                    return true;
                }
                Stat s = Stat.parse(args[1]);
                if (s == null) {
                    p.sendMessage("§c스탯 이름은 힘/민첩/체력/마법 중 하나입니다.");
                    return true;
                }
                int amount = 1;
                if (args.length >= 3) {
                    Integer n = num(p, args[2]);
                    if (n == null) return true;
                    amount = Math.max(1, n);
                }
                plugin.allocate(p, s, amount);
                return true;
            }
            case "초기화": case "reset": {
                if (!admin(sender)) return true;
                if (args.length < 2) {
                    sender.sendMessage("§e사용법: /스탯 초기화 <플레이어|전체>");
                    return true;
                }
                if (args[1].equals("전체") || args[1].equalsIgnoreCase("all")) {
                    int n = plugin.data().resetAllStats();
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        plugin.levels().reconcile(plugin.data().get(p.getUniqueId()));
                        plugin.effects().apply(p);
                        p.sendMessage("§6[스탯] 스탯이 초기화되었습니다. §7/스탯 에서 다시 분배하세요.");
                    }
                    sender.sendMessage("§a전체 " + n + "명의 스탯을 초기화했습니다.");
                } else {
                    Player t = find(sender, args[1]);
                    if (t == null) return true;
                    PlayerData d = plugin.data().get(t.getUniqueId());
                    d.resetStats();
                    plugin.effects().apply(t);
                    t.sendMessage("§6[스탯] 스탯이 초기화되었습니다. §7/스탯 에서 다시 분배하세요.");
                    sender.sendMessage("§a" + t.getName() + " 의 스탯을 초기화했습니다.");
                }
                return true;
            }
            case "리로드": case "reload": {
                if (!admin(sender)) return true;
                plugin.reload();
                sender.sendMessage("§a설정을 다시 불러왔습니다.");
                return true;
            }
            default:
                sender.sendMessage("§e/스탯 §7- 스탯 창  §e/스탯 정보  /스탯 올리기 <스탯> [수]  /스탯 초기화 <플레이어|전체>  /스탯 리로드");
                return true;
        }
    }

    private void info(CommandSender to, Player target) {
        PlayerData d = plugin.data().get(target.getUniqueId());
        LevelManager lv = plugin.levels();
        Totals t = Totals.of(plugin, target);
        to.sendMessage("§6===== " + target.getName() + " 스탯 =====");
        to.sendMessage("§eLv." + d.level + (d.level >= lv.maxLevel() ? " §a(최대)" : " §7경험치 " + d.exp + "/" + lv.need(d.level))
                + " §7| 남은 포인트 §e" + lv.available(d));
        StringBuilder sb = new StringBuilder();
        for (Stat s : Stat.values()) sb.append("§f").append(s.label).append(" §e").append(d.get(s)).append("  ");
        to.sendMessage(sb.toString());
        to.sendMessage(String.format("§7스킬피해 §f+%.1f%%  §7공격력 §f+%.1f  §7크리 §f%.1f%% x%.2f  §7흡혈 §f%.1f%%",
                t.skillDamage, t.attack, t.critChance, t.critMultiplier, t.lifesteal));
        to.sendMessage(String.format("§7이속 §f+%.1f%%  §7회피 §f%.1f%%  §7피해감소 §f%.1f%%  §7피격회복 §f%.1f  §7쿨감 §f%.1f%%",
                t.moveSpeed, t.dodge, t.reduction, t.onHitHeal, t.cooldownReduction));
    }

    // ---------------- /레벨설정 ----------------
    private boolean levelSet(CommandSender sender, String[] args) {
        if (!admin(sender)) return true;
        if (args.length < 2) {
            sender.sendMessage("§e사용법: /레벨설정 <플레이어> <레벨>");
            return true;
        }
        Player t = find(sender, args[0]);
        Integer lvl = num(sender, args[1]);
        if (t == null || lvl == null) return true;
        plugin.levels().setLevel(t, lvl);
        PlayerData d = plugin.data().get(t.getUniqueId());
        sender.sendMessage("§a" + t.getName() + " 레벨을 " + d.level + " 로 설정했습니다.");
        t.sendMessage("§6[스탯] 레벨이 §e" + d.level + " §6로 설정되었습니다. 남은 포인트: §e" + plugin.levels().available(d));
        return true;
    }

    private boolean giveExp(CommandSender sender, String[] args) {
        if (!admin(sender)) return true;
        if (args.length < 2) {
            sender.sendMessage("§e사용법: /경험치지급 <플레이어> <양>");
            return true;
        }
        Player t = find(sender, args[0]);
        Integer n = num(sender, args[1]);
        if (t == null || n == null) return true;
        plugin.levels().addExp(t, n);
        sender.sendMessage("§a" + t.getName() + " 에게 경험치 " + n + " 지급");
        return true;
    }

    private boolean givePoint(CommandSender sender, String[] args) {
        if (!admin(sender)) return true;
        if (args.length < 2) {
            sender.sendMessage("§e사용법: /포인트지급 <플레이어> <수>  (음수면 회수)");
            return true;
        }
        Player t = find(sender, args[0]);
        Integer n = num(sender, args[1]);
        if (t == null || n == null) return true;
        PlayerData d = plugin.data().get(t.getUniqueId());
        d.bonusPoints += n;
        plugin.levels().reconcile(d);
        sender.sendMessage("§a" + t.getName() + " 에게 스탯 포인트 " + n + " 지급");
        t.sendMessage("§6[스탯] 스탯 포인트 §e" + n + " §6지급! 남은 포인트: §e" + plugin.levels().available(d));
        return true;
    }

    // ---------------- /스탯무기 ----------------
    private boolean weapon(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) return true;
        if (!admin(sender)) return true;
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (hand.getType() == Material.AIR) {
            p.sendMessage("§c손에 무기를 들고 사용하세요.");
            return true;
        }
        if (args.length == 0) {
            p.sendMessage("§e/스탯무기 설정 <스킬퍼뎀|공격력|크확|크뎀|생명력흡수> <값>  §7(0이면 해당 항목 제거)");
            p.sendMessage("§e/스탯무기 제거  §7(모든 스탯 제거)   §e/스탯무기 확인");
            return true;
        }
        switch (args[0]) {
            case "설정": case "set": {
                if (args.length < 3) {
                    p.sendMessage("§e사용법: /스탯무기 설정 <스킬퍼뎀|공격력|크확|크뎀|생명력흡수> <값>");
                    return true;
                }
                WeaponStat w = WeaponStat.parse(args[1]);
                if (w == null) {
                    p.sendMessage("§c종류: 스킬퍼뎀, 공격력, 크확, 크뎀, 생명력흡수");
                    return true;
                }
                double v;
                try {
                    v = Double.parseDouble(args[2]);
                } catch (NumberFormatException ex) {
                    p.sendMessage("§c숫자를 입력하세요.");
                    return true;
                }
                WeaponStat.set(plugin, hand, w, v);
                p.getInventory().setItemInMainHand(hand);
                p.sendMessage("§a" + w.label + " " + v + w.unit + " 적용");
                return true;
            }
            case "제거": case "clear":
                WeaponStat.clear(plugin, hand);
                p.getInventory().setItemInMainHand(hand);
                p.sendMessage("§a무기 스탯을 모두 제거했습니다.");
                return true;
            case "확인": {
                boolean any = false;
                for (WeaponStat w : WeaponStat.values()) {
                    double v = WeaponStat.get(plugin, hand, w);
                    if (v != 0) {
                        p.sendMessage("§7" + w.label + " §f+" + v + w.unit);
                        any = true;
                    }
                }
                if (!any) p.sendMessage("§7붙어 있는 스탯이 없습니다.");
                return true;
            }
            default:
                return weapon(sender, new String[0]);
        }
    }

    // ---------------- 자동완성 ----------------
    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        String name = cmd.getName().toLowerCase();
        String cur = args[args.length - 1];
        if (name.equals("stat")) {
            if (args.length == 1) out.addAll(Arrays.asList("정보", "올리기", "초기화", "리로드"));
            else if (args.length == 2 && args[0].equals("올리기"))
                for (Stat s : Stat.values()) out.add(s.label);
            else if (args.length == 2 && args[0].equals("초기화")) {
                out.add("전체");
                Bukkit.getOnlinePlayers().forEach(p -> out.add(p.getName()));
            }
        } else if (name.equals("statweapon")) {
            if (args.length == 1) out.addAll(Arrays.asList("설정", "제거", "확인"));
            else if (args.length == 2 && args[0].equals("설정"))
                for (WeaponStat w : WeaponStat.values()) out.add(w.alias);
        } else if (args.length == 1) {
            Bukkit.getOnlinePlayers().forEach(p -> out.add(p.getName()));
        }
        return out.stream().filter(s -> s.toLowerCase().startsWith(cur.toLowerCase())).collect(Collectors.toList());
    }
}
