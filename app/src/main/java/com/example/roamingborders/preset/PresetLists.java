package com.example.roamingborders.preset;

import com.example.roamingborders.model.ListConfig;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PresetLists {
    private static final Set<String> EWR = new HashSet<>(Arrays.asList(
            // EU-27
            "AT", "BE", "BG", "HR", "CY", "CZ", "DK", "EE", "FI",
            "FR", "DE", "GR", "HU", "IE", "IT", "LV", "LT", "LU",
            "MT", "NL", "PL", "PT", "RO", "SK", "SI", "ES", "SE",
            // EWR-EFTA
            "NO", "IS", "LI"
    ));

    private static final Map<String, ListConfig> Presets = new HashMap<>();
    static {
        Presets.put("EWR", getEwr());
    }

    public static ListConfig getEwr() { return new ListConfig(EWR, true); }

    public static Map<String, ListConfig> getPresets()
    {
        return Collections.unmodifiableMap(Presets);
    }
}
