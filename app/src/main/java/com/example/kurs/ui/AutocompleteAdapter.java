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
import com.google.android.gms.maps.model.LatLng;
public class AutocompleteAdapter extends RecyclerView.Adapter<AutocompleteAdapter.ViewHolder> {

    public interface OnPlaceClickListener {
        void onPlaceClick(String address, LatLng latLng);
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
        holder.itemView.setOnClickListener(v -> {
            String placeId = prediction.getPlaceId();
            placesClient.fetchPlace(
                    com.google.android.libraries.places.api.net.FetchPlaceRequest.builder(
                            placeId,
                            List.of(com.google.android.libraries.places.api.model.Place.Field.LAT_LNG, com.google.android.libraries.places.api.model.Place.Field.NAME)
                    ).build()
            ).addOnSuccessListener(fetchPlaceResponse -> {
                com.google.android.libraries.places.api.model.Place place = fetchPlaceResponse.getPlace();
                LatLng latLng = place.getLatLng();
                if (latLng != null) {
                    listener.onPlaceClick(place.getName(), latLng);
                }
            });
        });
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
