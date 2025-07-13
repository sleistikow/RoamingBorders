package com.example.roamingborders.preset;

import com.example.roamingborders.R;
import com.example.roamingborders.model.ListConfig;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class PresetLists {

    // EU-27
    private static final Set<String> EU = Set.of(
            "AT", "BE", "BG", "HR", "CY", "CZ", "DK", "EE", "FI",
            "FR", "DE", "GR", "HU", "IE", "IT", "LV", "LT", "LU",
            "MT", "NL", "PL", "PT", "RO", "SK", "SI", "ES", "SE");
    private static final Set<String> EEA;
    static {
        Set<String> tmp = new HashSet<>(EU);
        tmp.addAll(Set.of("NO","IS","LI"));
        EEA = Collections.unmodifiableSet(tmp);
    }

    private static final Map<String, ListConfig> Presets = new HashMap<>();
    static {
        Presets.put("EEA - whitelist", getEeaWhitelist());
        //Presets.put(R.string.preset_eu, getEuWhitelist());
        //Presets.put("North America - whitelist", getNorthAmericaWhitelist());
    }

    public static ListConfig getEeaWhitelist() { return new ListConfig(EEA, true); }
    public static ListConfig getEuWhitelist() { return new ListConfig(EU, true); }
    public static ListConfig getNorthAmericaWhitelist() {
        return new ListConfig(new HashSet<>(Arrays.asList(
                Locale.US.toString(),
                Locale.CANADA.toString())), true);
    }

    public static Map<String, ListConfig> getPresets()
    {
        return Collections.unmodifiableMap(Presets);
    }
}
