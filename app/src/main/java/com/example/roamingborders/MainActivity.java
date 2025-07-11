package com.example.roamingborders;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;
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
import com.example.roamingborders.preset.PresetLists;
import com.example.roamingborders.service.CellMonitorService;
import com.example.roamingborders.util.CountryAdapter;
import com.example.roamingborders.util.CountryAssets;
import com.example.roamingborders.util.TextInputDialog;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private Spinner spnPreset;
    private AutoCompleteTextView actCountry;
    private Button btnAddRemove, btnEnableDisable;
    private ImageButton btnCopyPreset, btnDeletePreset, btnNewPreset;
    private Button btnCommitChanges, btnDiscardChanges;

    private RecyclerView recyclerCountries;
    //private MaterialSwitch switchMode;
    private CheckBox whitelistMode;
    private CountryAdapter countryAdapter;
    private ActivityResultLauncher<Intent> vpnConsent;

    private final ArrayList<String> workingList = new ArrayList<>();
    private ListManager listManager;

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
        //switchMode       = binding.switchMode;
        whitelistMode    = binding.whitelistMode;
        recyclerCountries= binding.recyclerCountries;
        btnCommitChanges = binding.btnCommitChanges;
        btnDiscardChanges= binding.btnDiscardChanges;

        vpnConsent = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() != Activity.RESULT_OK) {
                        btnEnableDisable.setEnabled(false);
                    }
                }
        );

        Intent prepareIntent = VpnService.prepare(this);
        if (prepareIntent != null) {
            vpnConsent.launch(prepareIntent);
        }

        listManager = new ListManager(this);

        refreshPresetSpinner();
        spnPreset.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                String preset = spnPreset.getSelectedItem().toString();
                boolean isPreset = PresetLists.getPresets().containsKey(preset);
                btnDeletePreset.setEnabled(!isPreset);
                actCountry.setEnabled(!isPreset);
                btnAddRemove.setEnabled(!isPreset);
                loadPreset(preset);
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
            String country = getSelectedCountry();
            if(country == null) {
                Toast.makeText(this, "No country selected", Toast.LENGTH_SHORT).show();
                return;
            }
            if (workingList.contains(country)) workingList.remove(country);
            else                               workingList.add(country);
            countryAdapter.notifyDataSetChanged();
            refreshAddRemoveLabel();
        });

        btnEnableDisable.setOnClickListener(v -> {
            activateList();
        });

        /*
        btnDelete.setOnClickListener(v -> {
            listManager.deleteList(spnSaved.getSelectedItem().toString());
            refreshSavedSpinner();
        });

         */

        /*
        switchMode.setOnCheckedChangeListener((view, checked) -> {
            if (checked)
            {
                Intent prepareIntent = VpnService.prepare(this);
                if (prepareIntent != null) {
                    vpnConsent.launch(prepareIntent);
                }
                else
                {
                    NullVpnService.ensureRunning(this);
                }
            }
            else
            {
                NullVpnService.ensureStopped(this);
            }
        });
         */


        refreshAddRemoveLabel();
        requestRuntimePermissions();
        CellMonitorService.ensureRunning(this);
    }

    private void refreshPresetSpinner() {
        ArrayAdapter<String> a = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                listManager.getAllListNames());
        spnPreset.setAdapter(a);
    }

    private void loadPreset(String name) {
        ListConfig cfg = PresetLists.getPresets().get(name);
        if(cfg == null) {
            cfg = listManager.loadList(name);
        }
        listManager.setActiveConfig(cfg);
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
        Toast.makeText(this, "Changes Saved", Toast.LENGTH_SHORT).show();
    }

    private void discardWorkingList() {
        String preset = spnPreset.getSelectedItem().toString();
        loadPreset(preset);
        Toast.makeText(this, "Changes Discarded", Toast.LENGTH_SHORT).show();
    }

    private void activateList() {
        String preset = spnPreset.getSelectedItem().toString();
        listManager.setActiveConfig(listManager.loadList(preset));
        CellMonitorService.ensureRunning(this);
        Toast.makeText(this, R.string.activated, Toast.LENGTH_SHORT).show();
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
        btnAddRemove.setText(contains ? R.string.remove : R.string.add);
    }

    private void copyCurrentPreset() {
        TextInputDialog.ask(this, getString(R.string.name_copy), (name, checked) -> {
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
        TextInputDialog.ask(this, getString(R.string.name_new), (name, checked) -> {
            listManager.saveList(name, new HashSet<>(), checked);
            refreshPresetSpinner();
            spnPreset.setSelection(((ArrayAdapter<String>) spnPreset.getAdapter()).getPosition(name));
        });
    }

    private void requestRuntimePermissions() {
        List<String> req = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            req.add(Manifest.permission.READ_PHONE_STATE);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            req.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            req.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        if (!req.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    req.toArray(new String[0]), 42);
        }
    }
}