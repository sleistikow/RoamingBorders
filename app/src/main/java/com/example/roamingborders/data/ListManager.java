package com.example.roamingborders.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.roamingborders.model.ListConfig;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ListManager {
    private static final String PREFS = "lists";
    private static final String ACTIVE_KEY = "#?!active_list#?!";

    private static final String PROPERTY_ENTRIES = "entries";
    private static final String PROPERTY_WHITELIST = "whitelist";

    private final SharedPreferences sp;

    public ListManager(Context ctx) { sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE); }

    public void saveList(String name, Set<String> country, boolean whitelist) {
        JsonObject obj = new JsonObject();
        obj.add(PROPERTY_ENTRIES, new Gson().toJsonTree(country));
        obj.addProperty(PROPERTY_WHITELIST, whitelist);
        sp.edit().putString(name, obj.toString()).apply();
    }

    public void deleteList(String name) { sp.edit().remove(name).apply(); }

    public List<String> getAllListNames() {
        List<String> names = new ArrayList<>(sp.getAll().keySet());
        names.remove(ACTIVE_KEY);
        return names;
    }

    public ListConfig loadList(String name) {
        String json = sp.getString(name, null);
        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
        Set<String> entries = new Gson().fromJson(obj.get(PROPERTY_ENTRIES), new TypeToken<Set<String>>(){}.getType());
        boolean whitelist = obj.get(PROPERTY_WHITELIST).getAsBoolean();
        return new ListConfig(entries, whitelist);
    }

    public void setActiveConfig(ListConfig cfg) {
        sp.edit().putString(ACTIVE_KEY, cfg.toJson()).apply();
    }

    public ListConfig getActiveConfig() {
        String json = sp.getString(ACTIVE_KEY, null);
        return json == null ? null : ListConfig.fromJson(json);
    }
}
