package com.example.roamingborders;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ShareActionProvider;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.roamingborders.data.ListManager;
import com.example.roamingborders.databinding.ActivityMainBinding;
import com.example.roamingborders.model.ListConfig;
import com.example.roamingborders.monitor.MobileTrafficMonitor;
import com.example.roamingborders.preset.PresetLists;
import com.example.roamingborders.service.CellMonitorService;
import com.example.roamingborders.util.CountryAdapter;
import com.example.roamingborders.util.CountryAssets;
import com.example.roamingborders.util.MessageHelper;
import com.example.roamingborders.util.TextInputDialog;
import com.example.roamingborders.vpn.NullVpnService;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private Spinner spnPreset;
    private AutoCompleteTextView actCountry;
    private ImageButton btnClear;
    private Button btnAddRemove;
    private MaterialSwitch swActive;
    private ImageButton btnCopyPreset, btnDeletePreset, btnNewPreset;
    private Button btnCommitChanges, btnDiscardChanges;

    private RecyclerView recyclerCountries;
    private RadioGroup rgListMode;
    private RadioButton rbWhitelist, rbBlacklist;

    private MaterialSwitch btnKillSwitch;
    private ImageButton btnInfo;

    private CountryAdapter countryAdapter;
    private ActivityResultLauncher<Intent> vpnConsent;

    private final ArrayList<String> workingList = new ArrayList<>();
    private ListManager listManager;
    private MobileTrafficMonitor monitor;

    private Boolean blockCallback = false;

    private static final String PREFERENCES  = "app_preferences";
    private static final String KEY_FIRST_START = "first_start";
    private static final String KEY_KILL_SWITCH_ACTIVE = "kill_switch_active";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // ---------- VIEW BINDINGS ----------
        spnPreset        = binding.spnPreset;
        actCountry       = binding.actCountry;
        btnClear         = binding.btnClear;
        swActive         = binding.swActive;
        btnAddRemove     = binding.btnAddRemove;
        btnCopyPreset    = binding.btnCopyPreset;
        btnDeletePreset  = binding.btnDeletePreset;
        btnNewPreset     = binding.btnNewPreset;
        rgListMode       = binding.rgListMode;
        rbWhitelist      = binding.rbWhitelist;
        rbBlacklist      = binding.rbBlacklist;
        recyclerCountries= binding.recyclerCountries;
        btnCommitChanges = binding.btnCommitChanges;
        btnDiscardChanges= binding.btnDiscardChanges;
        btnKillSwitch    = binding.btnKillSwitch;
        btnInfo          = binding.btnInfo;

        rgListMode.check(rbWhitelist.getId());
        rbWhitelist.setEnabled(false);
        rbBlacklist.setEnabled(false);
        btnCommitChanges.setEnabled(false);
        btnDiscardChanges.setEnabled(false);

        vpnConsent = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        btnKillSwitch.setChecked(false);
                        btnKillSwitch.setEnabled(true);
                    }
                }
        );

        listManager = new ListManager(this);

        swActive.setOnCheckedChangeListener((v, checked) -> {
            if (!blockCallback) {
                updatePresetStatus(checked);
            }
        });

        btnCopyPreset.setOnClickListener(v -> copyCurrentPreset());
        btnDeletePreset.setOnClickListener(v -> deleteCurrentPreset());
        btnNewPreset .setOnClickListener(v -> createEmptyPreset());

        btnCommitChanges.setOnClickListener(v -> commitWorkingList());
        btnDiscardChanges.setOnClickListener(v -> discardWorkingList());

        Context ctx = this;
        btnKillSwitch.setOnCheckedChangeListener((v, checked) -> {
            if(checked) {
                MessageHelper.showKillSwitchConfirmation(ctx, (i, id) ->
                        killSwitchChanged(true)
                );
            } else {
                killSwitchChanged(false);
            }
        });
        btnInfo.setOnClickListener(v -> showInfo());

        ArrayAdapter<String> countryAdapterAuto =
                new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line,
                        CountryAssets.getDisplayList());
        actCountry.setAdapter(countryAdapterAuto);
        actCountry.setOnClickListener(v -> actCountry.showDropDown());
        actCountry.setOnItemClickListener((p,v,i,id) -> refreshAddRemoveLabel());

        btnClear.setOnClickListener(v -> {
            actCountry.setText("");
            refreshAddRemoveLabel();
        });

        countryAdapter = new CountryAdapter(workingList, country -> {
            actCountry.setText(CountryAssets.getDisplayTextForCountry(country), false);
            refreshAddRemoveLabel();
        });

        recyclerCountries.setLayoutManager(new LinearLayoutManager(this));
        recyclerCountries.setAdapter(countryAdapter);

        btnAddRemove.setOnClickListener(v -> addOrRemoveCountry() );

        // Create an initial list if app is started for the first time.
        populatePresets();
        spnPreset.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                blockCallback = true;
                presetSelected();
                blockCallback = false;
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        monitor = new MobileTrafficMonitor(this, usingMobile -> {
            if(isKillSwitchActive())
                return;
            if (usingMobile) {
                CellMonitorService.ensureRunning(ctx);
            } else {
                CellMonitorService.ensureStopped(ctx);
            }
        });

        refreshAddRemoveLabel();
        requestRuntimePermissions();
        updateServices();
    }

    @Override protected void onStart() { super.onStart(); monitor.start(); }
    @Override protected void onStop()  { super.onStop();  monitor.stop();  }

    private void refreshPresetSpinner() {
        ArrayAdapter<String> a = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                listManager.getAllListNames());
        spnPreset.setAdapter(a);
    }

    private void loadPreset(String name) {
        ListConfig cfg = listManager.loadList(name);
        workingList.clear();
        workingList.addAll(cfg.iso2);
        countryAdapter.notifyDataSetChanged();
        rgListMode.check(cfg.whitelist ? rbWhitelist.getId() : rbBlacklist.getId());
        refreshAddRemoveLabel();
    }

    private void commitWorkingList() {
        String preset = spnPreset.getSelectedItem().toString();
        listManager.saveList(preset, new HashSet<>(workingList), rbWhitelist.isChecked());
        loadPreset(preset);

        btnCommitChanges.setEnabled(false);
        btnDiscardChanges.setEnabled(false);

        // Force update.
        CellMonitorService.ensureRunning(this);

        // Info.
        Toast.makeText(this, R.string.toast_changes_saved, Toast.LENGTH_SHORT).show();
    }

    private void discardWorkingList() {
        // Simply reload current preset.
        String preset = spnPreset.getSelectedItem().toString();
        loadPreset(preset);

        btnCommitChanges.setEnabled(false);
        btnDiscardChanges.setEnabled(false);

        // Info.
        Toast.makeText(this, R.string.toast_changes_discarded, Toast.LENGTH_SHORT).show();
    }

    private void addOrRemoveCountry() {
        String country = getSelectedCountry();
        if(country == null) {
            Toast.makeText(this, R.string.toast_no_country_selected, Toast.LENGTH_SHORT).show();
            return;
        }

        if (workingList.contains(country)) workingList.remove(country);
        else                               workingList.add(country);

        countryAdapter.notifyDataSetChanged();
        refreshAddRemoveLabel();
        btnCommitChanges.setEnabled(true);
        btnDiscardChanges.setEnabled(true);
    }

    private String getSelectedCountry() {
        String text = actCountry.getText().toString();
        int index = CountryAssets.getDisplayList().indexOf(text);
        if(index < 0)
            return null;
        return CountryAssets.COUNTRIES[index];
    }
    private void refreshAddRemoveLabel() {
        String preset = spnPreset.getSelectedItem().toString();
        String selectedCountry = getSelectedCountry();
        btnAddRemove.setEnabled(selectedCountry != null && !isPredefinedPreset(preset));

        boolean contains = workingList.contains(getSelectedCountry());
        btnAddRemove.setText(contains ? R.string.country_remove : R.string.country_add);
    }

    private void presetSelected() {
        String preset = spnPreset.getSelectedItem().toString();
        boolean isPredefinedPreset = isPredefinedPreset(preset);
        actCountry.setEnabled(!isPredefinedPreset);
        btnClear.setEnabled(!isPredefinedPreset);
        btnAddRemove.setEnabled(!isPredefinedPreset);
        btnCommitChanges.setEnabled(false);
        btnDiscardChanges.setEnabled(false);

        loadPreset(preset);
        boolean isActive = preset.equals(listManager.getActiveConfig());
        swActive.setChecked(isActive);
        swActive.setEnabled(!isActive);
    }

    private void updatePresetStatus(boolean active) {
        if (active) {
            String preset = spnPreset.getSelectedItem().toString();
            listManager.setActiveConfig(preset);
        } else {
            // This branch should not be possible in the current ui-design (see below).
            listManager.setActiveConfig(null);
        }

        // Currently, we only allow enabling.
        swActive.setEnabled(!active);

        CellMonitorService.ensureRunning(this);
        Toast.makeText(this, active ? R.string.toast_preset_activated : R.string.toast_preset_deactivated, Toast.LENGTH_SHORT).show();
    }

    private void copyCurrentPreset() {
        TextInputDialog.ask(this, getString(R.string.name_copy), listManager.getAllListNames(),
                (name, checked) -> {
            listManager.saveList(name, new HashSet<>(workingList), checked);
            refreshPresetSpinner();
            spnPreset.setSelection(((ArrayAdapter<String>) spnPreset.getAdapter()).getPosition(name));
        });
    }

    private void deleteCurrentPreset() {
        String selectedPreset = spnPreset.getSelectedItem().toString();
        if (isPredefinedPreset(selectedPreset)) {
            MessageHelper.showPredefinedPresetDeletionNotPossible(this);
        } else if(isActivePreset(selectedPreset)) {
            MessageHelper.showActivePresetDeletionNotPossible(this);
        } else {
            MessageHelper.showDeletePreset(this, (v, id) ->
            {
                listManager.deleteList(selectedPreset);
                refreshPresetSpinner();
            });
        }
    }

    private void createEmptyPreset() {
        TextInputDialog.ask(this, getString(R.string.name_new), listManager.getAllListNames(),
                (name, checked) -> {
            listManager.saveList(name, new HashSet<>(), checked);
            refreshPresetSpinner();
            spnPreset.setSelection(((ArrayAdapter<String>) spnPreset.getAdapter()).getPosition(name));
        });
    }

    private void requestRuntimePermissions() {
        List<String> req = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            req.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        if (!req.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    req.toArray(new String[0]), 42);
        }

        Intent prepareIntent = VpnService.prepare(this);
        if (prepareIntent != null) {
            MessageHelper.showVpnInfo(this, (d, i) -> vpnConsent.launch(prepareIntent));
        } else {
            btnKillSwitch.setChecked(isKillSwitchActive());
            btnKillSwitch.setEnabled(true);
        }
    }

    private void updateServices() {
        if(isKillSwitchActive()) {
            CellMonitorService.ensureStopped(this);
        } else {
            CellMonitorService.ensureRunning(this);
        }
    }

    private void populatePresets() {
        SharedPreferences preferences = getSharedPreferences(PREFERENCES, MODE_PRIVATE);
        if(preferences.contains(KEY_FIRST_START)) {
            refreshPresetSpinner();

            String activePreset = listManager.getActiveConfig();
            if(activePreset != null) {
                spnPreset.setSelection(((ArrayAdapter<String>) spnPreset.getAdapter()).getPosition(activePreset));
            }
            return;
        }

        // Create an initial whitelist containing the user's country.
        Set<String> countries = new HashSet<>();

        // We set the current SIM's country as default, if available.
        TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        String country = tm.getSimCountryIso().toUpperCase(Locale.US);
        if (!country.isEmpty()) {
            countries.add(country);
        }

        String initialPreset = getString(R.string.preset_init);

        listManager.saveList(initialPreset, countries, true);

        // Populate presets.
        listManager.saveList(getString(R.string.preset_eea), PresetLists.EEA, true);
        //listManager.saveList(getString(R.string.preset_na), PresetLists.NA, true);

        // Set the active list.
        listManager.setActiveConfig(initialPreset);
        refreshPresetSpinner();
        spnPreset.setSelection(((ArrayAdapter<String>) spnPreset.getAdapter()).getPosition(initialPreset));

        // Indicate population done.
        preferences.edit().putBoolean(KEY_FIRST_START, false).apply();
    }

    private boolean isPredefinedPreset(String name) {
        return name.equals(getString(R.string.preset_eea));
    }

    private boolean isActivePreset(String name) {
        return name.equals(listManager.getActiveConfig());
    }

    private boolean isKillSwitchActive() {
        return getSharedPreferences(PREFERENCES, MODE_PRIVATE)
                .getBoolean(KEY_KILL_SWITCH_ACTIVE, true);
    }
    public static boolean isKillSwitchActive(Context ctx) {
        return ctx.getSharedPreferences(PREFERENCES, MODE_PRIVATE)
                .getBoolean(KEY_KILL_SWITCH_ACTIVE, true);
    }
    private void killSwitchChanged(boolean active) {
        getSharedPreferences(PREFERENCES, MODE_PRIVATE)
                .edit().putBoolean(KEY_KILL_SWITCH_ACTIVE, active).apply();

        updateServices();
    }

    private void showInfo() {
        MessageHelper.showInfoBox(this);
    }

    /*
    private boolean isFirstRun() {
        return !getSharedPreferences(PREFS, MODE_PRIVATE)
                .getBoolean(KEY_TUTORIAL_SHOWN, false);
    }

    private void setTutorialShown() {
        getSharedPreferences(PREFS, MODE_PRIVATE)
                .edit().putBoolean(KEY_TUTORIAL_SHOWN, true).apply();
    }

    private void showTour() {
        TapTargetSequence sequence = new TapTargetSequence(this)
                .targets(
                        TapTarget.forView(findViewById(R.id.spnPreset),
                                        "Presets",
                                        "Select a predefined or custom preset here"),
                        TapTarget.forView(findViewById(R.id.btnCopyPreset),
                                        "Presets",
                                        "You can copy.."),
                        TapTarget.forView(findViewById(R.id.btnDeletePreset),
                                        "Presets",
                                        "..or delete the current preset"),
                        TapTarget.forView(findViewById(R.id.btnNewPreset),
                                        "Presets",
                                        "And create a new one."),
                        TapTarget.forView(findViewById(R.id.actCountry),
                                        "Search for a country here",
                                        "Tap and/or type here to search for a country"),
                        TapTarget.forView(findViewById(R.id.btnAddRemove),
                                        "Countries",
                                        "Add or remove it here")
                                .id(1),
                        TapTarget.forView(findViewById(R.id.recyclerCountries),
                                        "Countries",
                                        "See your current preset"),
                        TapTarget.forView(findViewById(R.id.btnCommitChanges),
                                "Countries",
                                "You can commit your changes to the current preset"),
                        TapTarget.forView(findViewById(R.id.btnDiscardChanges),
                                "Countries",
                                "Or discard all changes whatsoever"),
                        TapTarget.forView(findViewById(R.id.btnEnableDisable),
                                "Activate",
                                "Finally, activate the preset and enjoy roaming borders!")
                )
                .listener(new TapTargetSequence.Listener() {
                    @Override public void onSequenceFinish() { setTutorialShown(); }
                    @Override public void onSequenceStep(TapTarget lastTarget, boolean targetClicked) {}
                    @Override public void onSequenceCanceled(TapTarget lastTarget) { setTutorialShown(); }
                });

        sequence.start();
    }
     */
}