package com.sleistikow.roamingborders;

import java.util.Locale;
import java.util.Set;

public class CountryList {
    public final Set<String> iso2;
    public final boolean whitelist;

    public CountryList(Set<String> iso2, boolean whitelist) {
        this.iso2 = iso2; this.whitelist = whitelist;
    }

    public boolean isBlocked(String currentIso) {
        boolean contained = iso2.contains(currentIso.toUpperCase(Locale.US));
        return whitelist ? !contained : contained;
    }
}
