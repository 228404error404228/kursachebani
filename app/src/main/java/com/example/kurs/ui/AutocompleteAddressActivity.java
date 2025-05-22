package com.example.kurs.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.kurs.R;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.net.PlacesClient;

public class AutocompleteAddressActivity extends AppCompatActivity {

    private PlacesClient placesClient;
    private AutocompleteAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_autocomplete_address);

        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), getString(R.string.google_maps_key));
        }

        placesClient = Places.createClient(this);

        EditText searchField = findViewById(R.id.addressSearchField);
        RecyclerView recyclerView = findViewById(R.id.autocomplete_suggestions);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new AutocompleteAdapter(this, placesClient, (address, latLng) -> {
            fillAddressFields(address); // если LatLng пока не нужен — просто игнорируй
        });
        recyclerView.setAdapter(adapter);

        searchField.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (!s.toString().isEmpty()) {
                    recyclerView.setVisibility(View.VISIBLE);
                    adapter.getSuggestions(s.toString());
                } else {
                    recyclerView.setVisibility(View.GONE);
                }
            }
        });
    }

    private void fillAddressFields(String address) {
        Toast.makeText(this, "Выбран адрес: " + address, Toast.LENGTH_SHORT).show();
    }
}
