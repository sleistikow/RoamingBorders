package com.sleistikow.roamingborders;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CountryAssets {

    public static final String[] COUNTRIES = Locale.getISOCountries();

    public static List<String> getDisplayList() {
        List<String> out = new ArrayList<>();

        for (String country : COUNTRIES) {
            out.add(getDisplayTextForCountry(country));
        }

        return out;
    }

    public static String getDisplayTextForCountry(String country) {
        if(country.length() != 2) {
            return null;
        }

        Locale loc = new Locale("", country);
        String iso = loc.getISO3Country().toUpperCase(Locale.US);
        String flag = getFlagEmoji(country);
        String name = loc.getDisplayCountry(Locale.getDefault());

        return iso + " " + flag + " – " + name;
    }

    public static String getFlagEmoji(String country) {
        if (country == null || country.length() != 2) {
            // Return the pirate flag emoji as fallback.
            return "\uD83C\uDFF4\u200D\uFE0F";
        }
        int base = 0x1F1E6;          // Unicode-Offset for 'A'
        int first = base + (country.charAt(0) - 'A');
        int second = base + (country.charAt(1) - 'A');
        return new String(Character.toChars(first)) +
                new String(Character.toChars(second));
    }
}
