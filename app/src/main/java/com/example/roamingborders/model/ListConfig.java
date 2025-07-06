package com.example.roamingborders.model;

import com.google.gson.Gson;

import java.util.Locale;
import java.util.Set;

public class ListConfig {
    public final Set<String> iso3;
    public final boolean whitelist;

    public ListConfig(Set<String> iso3, boolean whitelist) {
        this.iso3 = iso3; this.whitelist = whitelist;
    }

    public boolean isBlocked(String currentIso) {
        boolean contained = iso3.contains(currentIso.toUpperCase(Locale.US));
        return whitelist ? !contained : contained;
    }

    // --- JSON helpers (Gson) ---
    public String toJson() { return new Gson().toJson(this); }
    public static ListConfig fromJson(String j) { return new Gson().fromJson(j, ListConfig.class); }
}
