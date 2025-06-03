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
import com.example.kurs.network.NominatimApi;
import com.example.kurs.ui.AutocompleteAdapter;

import org.osmdroid.util.GeoPoint;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class AutocompleteAddressActivity extends AppCompatActivity {

    private AutocompleteAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_autocomplete_address);

        // Настройка Nominatim API через Retrofit
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://nominatim.openstreetmap.org/") // или Heigit-URL
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        NominatimApi nominatimApi = retrofit.create(NominatimApi.class);

        EditText searchField = findViewById(R.id.addressSearchField);
        RecyclerView recyclerView = findViewById(R.id.autocomplete_suggestions);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new AutocompleteAdapter(this, nominatimApi, (address, geoPoint) -> {
            fillAddressFields(address, geoPoint);
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

    private void fillAddressFields(String address, GeoPoint geoPoint) {
        Toast.makeText(this, "Выбран адрес: " + address +
                        "\nКоординаты: " + geoPoint.getLatitude() + ", " + geoPoint.getLongitude(),
                Toast.LENGTH_SHORT).show();
    }
}
