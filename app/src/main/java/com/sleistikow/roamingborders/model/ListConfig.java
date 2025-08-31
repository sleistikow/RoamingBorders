package com.sleistikow.roamingborders.model;

import com.google.gson.Gson;

import java.util.Locale;
import java.util.Set;

public class ListConfig {
    public final Set<String> iso2;
    public final boolean whitelist;

    public ListConfig(Set<String> iso2, boolean whitelist) {
        this.iso2 = iso2; this.whitelist = whitelist;
    }

    public boolean isBlocked(String currentIso) {
        boolean contained = iso2.contains(currentIso.toUpperCase(Locale.US));
        return whitelist ? !contained : contained;
    }

    public String toJson() { return new Gson().toJson(this); }
    public static ListConfig fromJson(String j) { return new Gson().fromJson(j, ListConfig.class); }
}
