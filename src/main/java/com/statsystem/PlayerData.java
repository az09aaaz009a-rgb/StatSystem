package com.statsystem;

public class PlayerData {
    public int level = 1;
    public long exp = 0;
    public int bonusPoints = 0;
    public final int[] stats = new int[Stat.values().length];

    public int get(Stat s) {
        return stats[s.ordinal()];
    }

    public int spent() {
        int sum = 0;
        for (int v : stats) sum += v;
        return sum;
    }

    public void resetStats() {
        for (int i = 0; i < stats.length; i++) stats[i] = 0;
    }
}
