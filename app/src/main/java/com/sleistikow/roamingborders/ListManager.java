package com.sleistikow.roamingborders;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ListManager {
    private static final String PREFERENCES = "lists";
    private static final String KEY_ACTIVE = "#?!active_list#?!";

    private static final String PROPERTY_ENTRIES = "entries";
    private static final String PROPERTY_WHITELIST = "whitelist";

    private final SharedPreferences sp;

    public ListManager(Context ctx) { sp = ctx.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE); }

    public void saveList(String name, Set<String> country, boolean whitelist) {
        JsonObject obj = new JsonObject();
        obj.add(PROPERTY_ENTRIES, new Gson().toJsonTree(country));
        obj.addProperty(PROPERTY_WHITELIST, whitelist);
        sp.edit().putString(name, obj.toString()).apply();
    }

    public void deleteList(String name) { sp.edit().remove(name).apply(); }

    public List<String> getAllListNames() {
        List<String> names = new ArrayList<>(sp.getAll().keySet());
        names.remove(KEY_ACTIVE);
        return names;
    }

    public CountryList loadList(String name) {
        String json = sp.getString(name, null);
        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
        Set<String> entries = new Gson().fromJson(obj.get(PROPERTY_ENTRIES), new TypeToken<Set<String>>(){}.getType());
        boolean whitelist = obj.get(PROPERTY_WHITELIST).getAsBoolean();
        return new CountryList(entries, whitelist);
    }

    public void setActiveConfig(String name) {
        sp.edit().putString(KEY_ACTIVE, name).apply();
    }

    public String getActiveConfig() {
        return sp.getString(KEY_ACTIVE, null);
    }

    public CountryList loadActiveConfig() {
        String activeConfigName = getActiveConfig();
        if(activeConfigName != null) {
            return loadList(activeConfigName);
        }

        return null;
    }
}
