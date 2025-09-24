package com.sleistikow.roamingborders;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CountryAdapter extends RecyclerView.Adapter<CountryAdapter.VH> {

    public interface OnCountryClick { void onClick(String country); }

    private final List<String> data;
    private final OnCountryClick click;

    public CountryAdapter(List<String> data, OnCountryClick click) {
        this.data = data; this.click = click;
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tv;
        VH(TextView tv) { super(tv); this.tv = tv; }
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup p, int vType) {
        TextView tv = (TextView) LayoutInflater.from(p.getContext())
                .inflate(android.R.layout.simple_list_item_1, p, false);
        return new VH(tv);
    }

    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        String country = data.get(pos);
        h.tv.setText(CountryAssets.getDisplayTextForCountry(country));
        h.tv.setOnClickListener(v -> click.onClick(country));
    }
    @Override public int getItemCount() { return data.size(); }
}
