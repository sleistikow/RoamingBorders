package com.sleistikow.roamingborders;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class PresetLists {

    // EU-27
    public static final Set<String> EU = Set.of(
            "AT", "BE", "BG", "HR", "CY", "CZ", "DK", "EE", "FI",
            "FR", "DE", "GR", "HU", "IE", "IT", "LV", "LT", "LU",
            "MT", "NL", "PL", "PT", "RO", "SK", "SI", "ES", "SE"
    );

    // EU + EFTA
    public static final Set<String> EEA;
    static {
        Set<String> tmp = new HashSet<>(EU);
        tmp.addAll(Set.of("NO","IS","LI"));
        EEA = Collections.unmodifiableSet(tmp);
    }

    public static final Set<String> NA = Set.of(
            Locale.US.toString(),
            Locale.CANADA.toString()
    );
}
