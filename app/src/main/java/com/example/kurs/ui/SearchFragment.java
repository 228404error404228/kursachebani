package com.example.kurs.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.kurs.R;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.net.PlacesClient;

public class SearchFragment extends Fragment {

    private EditText addressSearchField;
    private RecyclerView suggestionsRecycler;
    private AutocompleteAdapter autocompleteAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_search, container, false);

        // Инициализация элементов
        addressSearchField = view.findViewById(R.id.addressSearchField);
        suggestionsRecycler = view.findViewById(R.id.autocompleteSuggestions);
        suggestionsRecycler.setLayoutManager(new LinearLayoutManager(getContext()));

        // Инициализация Places
        if (!Places.isInitialized()) {
            Places.initialize(requireContext().getApplicationContext(), "AIzaSyCHqNhuw8PGGOfkSDcqYCDhLHIKMESYWD0");
        }
        PlacesClient placesClient = Places.createClient(requireContext());

        autocompleteAdapter = new AutocompleteAdapter(requireContext(), placesClient, address -> {
            // Действие по нажатию на адрес
            addressSearchField.setText(address);
            suggestionsRecycler.setVisibility(View.GONE);
        });

        suggestionsRecycler.setAdapter(autocompleteAdapter);

        // Поиск по вводу текста
        addressSearchField.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!s.toString().isEmpty()) {
                    suggestionsRecycler.setVisibility(View.VISIBLE);
                    autocompleteAdapter.getSuggestions(s.toString());
                } else {
                    suggestionsRecycler.setVisibility(View.GONE);
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        return view;
    }
}
