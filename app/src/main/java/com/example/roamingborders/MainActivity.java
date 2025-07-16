package com.example.roamingborders;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.VpnService;
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
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.roamingborders.data.ListManager;
import com.example.roamingborders.databinding.ActivityMainBinding;
import com.example.roamingborders.model.ListConfig;
import com.example.roamingborders.preset.PresetLists;
import com.example.roamingborders.service.CellMonitorService;
import com.example.roamingborders.util.CountryAdapter;
import com.example.roamingborders.util.CountryAssets;
import com.example.roamingborders.util.MessageHelper;
import com.example.roamingborders.util.PermissionHelper;
import com.example.roamingborders.util.TextInputDialog;
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

    private final ArrayList<String> workingList = new ArrayList<>();
    private ListManager listManager;
    private Boolean blockCallback = false;

    private static final String PREFERENCES  = "app_preferences";
    private static final String KEY_FIRST_START = "first_start";
    private static final String KEY_PRESETS_POPULATED = "presets_populated";
    private static final String KEY_KILL_SWITCH_ACTIVE = "kill_switch_active";
    private static final int REQUESTED_PERMISSIONS = 42;

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

        listManager = new ListManager(this);

        rgListMode.check(rbWhitelist.getId());
        rbWhitelist.setEnabled(false);
        rbBlacklist.setEnabled(false);
        btnCommitChanges.setEnabled(false);
        btnDiscardChanges.setEnabled(false);

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

        // Note: the kill switch is disabled and checked, by default!
        // The following call is not strictly necessary, however it prevents
        // the toggle animation to play when the app is opened and the kill
        // switch was deactivated by the user before.
        btnKillSwitch.setChecked(isKillSwitchActive());
        btnKillSwitch.setOnCheckedChangeListener((v, checked) -> {
            if(checked) {
                MessageHelper.showKillSwitchConfirmation(this,
                        () -> killSwitchChanged(true), // Yes
                        () -> btnKillSwitch.setChecked(false) // No/Abort/Dismiss

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
        actCountry.setOnFocusChangeListener((v, focus) -> { if(focus) actCountry.showDropDown(); });
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
        refreshAddRemoveLabel();
        spnPreset.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                blockCallback = true;
                presetSelected();
                blockCallback = false;
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Finally, check requirements.
        checkRequirements();
    }

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
        updateServices();

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

        // Force update.
        updateServices();

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
            MessageHelper.showDeletePreset(this, () ->
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

    private void populatePresets() {
        SharedPreferences preferences = getSharedPreferences(PREFERENCES, MODE_PRIVATE);
        if(preferences.contains(KEY_PRESETS_POPULATED)) {
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
        preferences.edit().putBoolean(KEY_PRESETS_POPULATED, true).apply();
    }

    private boolean isPredefinedPreset(String name) {
        return name.equals(getString(R.string.preset_eea));
    }

    private boolean isActivePreset(String name) {
        return name.equals(listManager.getActiveConfig());
    }

    private boolean isKillSwitchActive() {
        return isKillSwitchActive(this);
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

    private void updateServices() {
        if(isKillSwitchActive()) {
            CellMonitorService.ensureStopped(this);
        } else {
            CellMonitorService.ensureRunning(this);
        }
    }

    private void showInfo() {
        MessageHelper.showInfoBox(this);
    }

    public static boolean isFirstStart(Context ctx) {
        return ctx.getSharedPreferences(PREFERENCES, MODE_PRIVATE)
                .getBoolean(KEY_FIRST_START, true);
    }

    private void noteFirstStart() {
        getSharedPreferences(PREFERENCES, MODE_PRIVATE).edit().putBoolean(KEY_FIRST_START, false).apply();
    }

    /**
     * The chain of requests / callbacks is:
     * checkRequirements() -> requestRuntimePermissions() -> onRequestPermissionsResult() -> makeAppUsable()
     */
    private void checkRequirements() {
        Intent prepareIntent = VpnService.prepare(this);
        if (prepareIntent != null) {
            ActivityResultLauncher<Intent> vpnConsent = registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(), result -> {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            requestRuntimePermissions();
                        }
                    }
            );
            MessageHelper.showVpnInfo(this, () -> vpnConsent.launch(prepareIntent));
        } else {
            requestRuntimePermissions();
        }
    }

    private void requestRuntimePermissions() {
        List<String> req = PermissionHelper.getMissingPermissions(this, false);
        if(!req.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    req.toArray(new String[0]), REQUESTED_PERMISSIONS);
        }
        else {
            makeAppUsable();
        }
    }

    @Override
    public void onRequestPermissionsResult(int code, @NonNull String[] perms,
                                           @NonNull int[] results) {
        super.onRequestPermissionsResult(code, perms, results);
        if (code != REQUESTED_PERMISSIONS) return;

        makeAppUsable();
    }

    private void makeAppUsable() {
        if(!PermissionHelper.mandatoryPermissionsGranted(this)) {
            btnKillSwitch.setEnabled(false);
            btnKillSwitch.setChecked(true);
        }
        else {
            btnKillSwitch.setEnabled(true);
            if (isFirstStart(this)) {
                btnKillSwitch.setChecked(false);
                noteFirstStart();
            } else {
                btnKillSwitch.setChecked(isKillSwitchActive());
            }

            // Force an update.
            updateServices();
        }
    }

}