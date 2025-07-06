package com.example.roamingborders;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.roamingborders.data.ListManager;
import com.example.roamingborders.databinding.ActivityMainBinding;
import com.example.roamingborders.model.ListConfig;
import com.example.roamingborders.service.CellMonitorService;
import com.example.roamingborders.util.CountryAssets;
import com.example.roamingborders.util.TextInputDialog;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private Spinner spnCountry, spnSaved;
    private Button btnAdd, btnSave, btnActivate, btnDelete;
    private MaterialSwitch switchMode;
    private RecyclerView recyclerWorking;

    private final ArrayList<String> workingList = new ArrayList<>();
    private ListManager listManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        spnCountry = binding.spnCountry;
        spnSaved = binding.spnSaved;
        btnAdd = binding.btnAdd;
        btnSave = binding.btnSaveList;
        btnActivate = binding.btnActivate;
        btnDelete = binding.btnDelete;
        switchMode = binding.switchMode;
        recyclerWorking = binding.recyclerWorking;

        listManager = new ListManager(this);

        ArrayAdapter<String> countryAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                CountryAssets.getDisplayList());
        spnCountry.setAdapter(countryAdapter);

        btnAdd.setOnClickListener(v -> {
            String sel = spnCountry.getSelectedItem().toString().substring(0, 3);
            if (!workingList.contains(sel)) workingList.add(sel);
            //recyclerWorking.setAdapter(); // TODO!
        });

        btnSave.setOnClickListener(v -> {
            TextInputDialog.ask(this, "Name der Liste", resultName -> {
                listManager.saveList(resultName,
                        new HashSet<>(workingList), switchMode.isChecked());
                refreshSavedSpinner();
                workingList.clear();
            });
        });

        btnActivate.setOnClickListener(v -> {
            ListConfig cfg = listManager.loadList(spnSaved.getSelectedItem().toString());
            listManager.setActiveConfig(cfg);
            CellMonitorService.enqueue(this, true /* immediate check */);
        });

        btnDelete.setOnClickListener(v -> {
            listManager.deleteList(spnSaved.getSelectedItem().toString());
            refreshSavedSpinner();
        });

        refreshSavedSpinner();
        requestRuntimePermissions();
    }

    private void refreshSavedSpinner() {
        List<String> names = listManager.getAllListNames();
        ArrayAdapter<String> savedAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, names);
        spnSaved.setAdapter(savedAdapter);
    }

    private void requestRuntimePermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_PHONE_STATE,
                            Manifest.permission.ACCESS_FINE_LOCATION},
                    42);
        }
    }
}