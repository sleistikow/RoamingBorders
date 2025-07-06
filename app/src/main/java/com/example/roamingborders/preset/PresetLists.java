package com.example.roamingborders.preset;

import com.example.roamingborders.model.ListConfig;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class PresetLists {
    private static final Set<String> EWR = new HashSet<>(Arrays.asList(
            "AUT","BEL","BGR","HRV","CYP","CZE","DNK","EST","FIN","FRA","DEU","GRC",
            "HUN","ISL","IRL","ITA","LVA","LIE","LTU","LUX","MLT","NLD","NOR","POL",
            "PRT","ROU","SVK","SVN","ESP","SWE","GBR" // Schweiz (CHE) bewusst nicht enthalten
    ));

    public static ListConfig getEwrWhitelist() { return new ListConfig(EWR, true); }
}
