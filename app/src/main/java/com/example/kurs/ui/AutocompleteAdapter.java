package com.example.kurs.ui;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.kurs.R;
import com.example.kurs.network.NominatimApi;
import com.example.kurs.network.NominatimPlace;

import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AutocompleteAdapter extends RecyclerView.Adapter<AutocompleteAdapter.ViewHolder> {

    public interface OnPlaceClickListener {
        void onPlaceClick(String address, GeoPoint geoPoint);
    }

    private final Context context;
    private final NominatimApi api;
    private final OnPlaceClickListener listener;
    private final List<NominatimPlace> predictions = new ArrayList<>();

    public AutocompleteAdapter(Context context, NominatimApi api, OnPlaceClickListener listener) {
        this.context = context;
        this.api = api;
        this.listener = listener;
    }

    public void getSuggestions(String query) {
        Call<List<NominatimPlace>> call = api.searchPlaces(
                query,
                "json",
                1,
                5,
                1,
                0,
                "egor.edrenov@gmail.com"
        );

        call.enqueue(new Callback<List<NominatimPlace>>() {
            @Override
            public void onResponse(Call<List<NominatimPlace>> call, Response<List<NominatimPlace>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    predictions.clear();
                    predictions.addAll(response.body());
                    notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(Call<List<NominatimPlace>> call, Throwable t) {
                Log.e("Nominatim", "Ошибка получения предсказаний", t);
            }
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
        NominatimPlace place = predictions.get(position);
        holder.predictionText.setText(place.getDisplayName());
        holder.itemView.setOnClickListener(v -> {
            GeoPoint geoPoint = new GeoPoint(place.getLat(), place.getLon());
            listener.onPlaceClick(place.getDisplayName(), geoPoint);
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
