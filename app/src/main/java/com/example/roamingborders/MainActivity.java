package com.example.roamingborders;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
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
import android.widget.CheckBox;
import android.widget.ImageButton;
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private Spinner spnPreset;
    private AutoCompleteTextView actCountry;
    private Button btnAddRemove, btnEnableDisable;
    private ImageButton btnCopyPreset, btnDeletePreset, btnNewPreset;
    private Button btnCommitChanges, btnDiscardChanges;

    private RecyclerView recyclerCountries;
    private CheckBox whitelistMode;
    private CountryAdapter countryAdapter;
    private ActivityResultLauncher<Intent> vpnConsent;

    private final ArrayList<String> workingList = new ArrayList<>();
    private ListManager listManager;
    private MobileTrafficMonitor monitor;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // ---------- VIEW BINDINGS ----------
        spnPreset        = binding.spnPreset;
        actCountry       = binding.actCountry;
        btnAddRemove     = binding.btnAddRemove;
        btnEnableDisable = binding.btnEnableDisable;
        btnCopyPreset    = binding.btnCopyPreset;
        btnDeletePreset  = binding.btnDeletePreset;
        btnNewPreset     = binding.btnNewPreset;
        whitelistMode    = binding.whitelistMode;
        recyclerCountries= binding.recyclerCountries;
        btnCommitChanges = binding.btnCommitChanges;
        btnDiscardChanges= binding.btnDiscardChanges;

        whitelistMode.setEnabled(false);
        btnCommitChanges.setEnabled(false);
        btnDiscardChanges.setEnabled(false);

        vpnConsent = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() != Activity.RESULT_OK) {
                        btnEnableDisable.setEnabled(false);
                    }
                }
        );

        listManager = new ListManager(this);

        // Create an initial list if app is started for the first time.
        if(listManager.getActiveConfig() == null)
        {
            Set<String> countries = new HashSet<>();

            // We set the current SIM's country as default, if available.
            TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
            String country = tm.getSimCountryIso().toUpperCase(Locale.US);
            if (!country.isEmpty()) {
                countries.add(country);
            }

            listManager.saveList(getString(R.string.preset_init), countries, true);
            listManager.setActiveConfig(listManager.loadList(getString(R.string.preset_init)));
        }

        refreshPresetSpinner();
        spnPreset.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                presetSelected();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnCopyPreset.setOnClickListener(v -> copyCurrentPreset());
        btnDeletePreset.setOnClickListener(v -> deleteCurrentPreset());
        btnNewPreset .setOnClickListener(v -> createEmptyPreset());

        btnCommitChanges.setOnClickListener(v -> commitWorkingList());
        btnDiscardChanges.setOnClickListener(v -> discardWorkingList());

        ArrayAdapter<String> countryAdapterAuto =
                new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line,
                        CountryAssets.getDisplayList());
        actCountry.setAdapter(countryAdapterAuto);
        actCountry.setOnClickListener(v -> actCountry.showDropDown());
        actCountry.setOnItemClickListener((p,v,i,id) -> refreshAddRemoveLabel());

        countryAdapter = new CountryAdapter(workingList, country -> {
            actCountry.setText(CountryAssets.getDisplayTextForCountry(country), false);  // select in dropdown
            refreshAddRemoveLabel();
        });

        recyclerCountries.setLayoutManager(new LinearLayoutManager(this));
        recyclerCountries.setAdapter(countryAdapter);

        btnAddRemove.setOnClickListener(v -> {
            addOrRemoveCountry();
        });

        btnEnableDisable.setOnClickListener(v -> {
            activateList();
        });

        Context ctx = this;
        monitor = new MobileTrafficMonitor(this, usingMobile -> {
            if (usingMobile) {
                //Toast.makeText(ctx, "Connected to Cell", Toast.LENGTH_SHORT).show();
                //CellMonitorService.ensureRunning(ctx);
            } else {
                //Toast.makeText(ctx, "Connected to WIFI", Toast.LENGTH_SHORT).show();
                //NullVpnService.ensureStopped(ctx);
            }
        });

        refreshAddRemoveLabel();
        requestRuntimePermissions();
        CellMonitorService.ensureRunning(this);
    }

    @Override protected void onStart() { super.onStart(); monitor.start(); }
    @Override protected void onStop()  { super.onStop();  monitor.stop();  }

    private List<String> getAllPresetNames() {
        List<String> names = listManager.getAllListNames();
        names.addAll(PresetLists.getPresets().keySet());
        return names;
    }

    private void refreshPresetSpinner() {

        // Retrieve saved lists and add presets.


        ArrayAdapter<String> a = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                getAllPresetNames());
        spnPreset.setAdapter(a);
    }

    private void loadPreset(String name) {
        ListConfig cfg = PresetLists.getPresets().get(name);
        if(cfg == null) {
            cfg = listManager.loadList(name);
        }
        workingList.clear();
        workingList.addAll(cfg.iso2);
        countryAdapter.notifyDataSetChanged();
        whitelistMode.setChecked(cfg.whitelist);
        refreshAddRemoveLabel();
    }

    private void commitWorkingList() {
        String preset = spnPreset.getSelectedItem().toString();
        listManager.saveList(preset, new HashSet<>(workingList), whitelistMode.isChecked());
        loadPreset(preset);
        btnCommitChanges.setEnabled(false);
        btnDiscardChanges.setEnabled(false);
        Toast.makeText(this, R.string.toast_changes_saved, Toast.LENGTH_SHORT).show();
    }

    private void discardWorkingList() {
        String preset = spnPreset.getSelectedItem().toString();
        loadPreset(preset);
        btnCommitChanges.setEnabled(false);
        btnDiscardChanges.setEnabled(false);
        Toast.makeText(this, R.string.changes_discard, Toast.LENGTH_SHORT).show();
    }

    private void activateList() {
        String preset = spnPreset.getSelectedItem().toString();
        if(PresetLists.getPresets().containsKey(preset)) {
            listManager.setActiveConfig(PresetLists.getPresets().get(preset));
        } else if(listManager.getAllListNames().contains(preset)) {
            listManager.setActiveConfig(listManager.loadList(preset));
        } else {
            Toast.makeText(this, R.string.toast_error_list_not_found, Toast.LENGTH_SHORT).show();
            return;
        }

        CellMonitorService.ensureRunning(this);
        Toast.makeText(this, R.string.toast_preset_activated, Toast.LENGTH_SHORT).show();
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

        /*
        int selection = actCountry.getListSelection();
        if(selection != ListView.INVALID_POSITION) {
            return CountryAssets.COUNTRIES[selection];
        }
        return null;
         */
    }
    private void refreshAddRemoveLabel() {
        boolean contains = workingList.contains(getSelectedCountry());
        btnAddRemove.setText(contains ? R.string.country_remove : R.string.country_add);
    }

    private void presetSelected() {
        String preset = spnPreset.getSelectedItem().toString();
        boolean isPreset = PresetLists.getPresets().containsKey(preset);
        btnDeletePreset.setEnabled(!isPreset);
        actCountry.setEnabled(!isPreset);
        btnAddRemove.setEnabled(!isPreset);
        btnCommitChanges.setEnabled(false);
        btnDiscardChanges.setEnabled(false);
        //btnEnableDisable.setEnabled(preset == listManager.getActiveConfig() );
        loadPreset(preset);
    }

    private void copyCurrentPreset() {
        TextInputDialog.ask(this, getString(R.string.name_copy), getAllPresetNames(),
                (name, checked) -> {
            listManager.saveList(name, new HashSet<>(workingList), checked);
            refreshPresetSpinner();
            spnPreset.setSelection(((ArrayAdapter<String>) spnPreset.getAdapter()).getPosition(name));
        });
    }

    private void deleteCurrentPreset() {
        String selectedPreset = spnPreset.getSelectedItem().toString();
        if (!PresetLists.getPresets().containsKey(selectedPreset)) {
            listManager.deleteList(selectedPreset);
            refreshPresetSpinner();
        }
    }

    private void createEmptyPreset() {
        TextInputDialog.ask(this, getString(R.string.name_new), getAllPresetNames(),
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
        }
    }
}