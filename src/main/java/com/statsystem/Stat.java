package com.statsystem;

public enum Stat {
    STR("str", "힘"),
    DEX("dex", "민첩"),
    VIT("vit", "체력"),
    MAG("mag", "마법");

    public final String key;
    public final String label;

    Stat(String key, String label) {
        this.key = key;
        this.label = label;
    }

    public static Stat parse(String s) {
        if (s == null) return null;
        for (Stat st : values()) {
            if (st.key.equalsIgnoreCase(s) || st.label.equals(s)) return st;
        }
        return null;
    }
}
