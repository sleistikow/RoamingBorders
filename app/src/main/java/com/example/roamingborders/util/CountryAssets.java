package com.example.roamingborders.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class CountryAssets {

    /** Liefert z. B. “DEU – Germany” oder “DEU – Deutschland” */
    public static List<String> getDisplayList() {
        List<String> out = new ArrayList<>();
        for (String iso2 : Locale.getISOCountries()) {
            Locale loc = new Locale("", iso2);
            // ISO-3-Code ermitteln
            String iso3 = loc.getISO3Country().toUpperCase(Locale.US);
            // Zeilen für Spinner zusammenstellen
            out.add(iso3 + " – " + loc.getDisplayCountry(Locale.getDefault()));
        }
        // alphabetisch sortieren
        Collections.sort(out);
        return out;
    }
}
