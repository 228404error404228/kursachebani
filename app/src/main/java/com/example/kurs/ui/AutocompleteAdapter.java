package com.example.kurs.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.kurs.R;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsResponse;
import com.google.android.libraries.places.api.net.PlacesClient;

import java.util.ArrayList;
import java.util.List;

public class AutocompleteAdapter extends RecyclerView.Adapter<AutocompleteAdapter.ViewHolder> {

    public interface OnPlaceClickListener {
        void onPlaceClick(String address);
    }

    private final Context context;
    private final PlacesClient placesClient;
    private final OnPlaceClickListener listener;
    private final List<AutocompletePrediction> predictions = new ArrayList<>();

    public AutocompleteAdapter(Context context, PlacesClient placesClient, OnPlaceClickListener listener) {
        this.context = context;
        this.placesClient = placesClient;
        this.listener = listener;
    }

    public void getSuggestions(String query) {
        FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
                .setQuery(query)
                .build();

        placesClient.findAutocompletePredictions(request)
                .addOnSuccessListener((FindAutocompletePredictionsResponse response) -> {
                    predictions.clear();
                    predictions.addAll(response.getAutocompletePredictions());
                    notifyDataSetChanged();
                });
    }

    @NonNull
    @Override
    public AutocompleteAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_prediction, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AutocompleteAdapter.ViewHolder holder, int position) {
        AutocompletePrediction prediction = predictions.get(position);
        holder.predictionText.setText(prediction.getFullText(null).toString());
        holder.itemView.setOnClickListener(v -> listener.onPlaceClick(prediction.getFullText(null).toString()));
    }

    @Override
    public int getItemCount() {
        return predictions.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView predictionText;

        ViewHolder(View itemView) {
            super(itemView);
            predictionText = itemView.findViewById(R.id.predictionText);
        }
    }
}
