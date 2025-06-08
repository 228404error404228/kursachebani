package com.example.kurs.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.navigation.fragment.NavHostFragment;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentResultListener;

import com.example.kurs.R;
import com.example.kurs.network.NominatimApi;
import com.example.kurs.network.NominatimPlace;
import com.example.kurs.network.OpenRouteServiceApi;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import org.osmdroid.util.GeoPoint;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import android.util.Log;
import java.util.Map;
import java.util.HashMap;;
import java.util.ArrayList;
import java.util.Arrays;
import androidx.lifecycle.ViewModelProvider;
import android.widget.TextView;


import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Polyline;
import org.json.JSONArray;
import org.json.JSONObject;

import android.graphics.Color;
import okhttp3.ResponseBody;

public class PlacesFragment extends Fragment {

    private static final double MAX_DISTANCE_KM = 50.0;

    // Ключ для передачи результата в RoutesFragment
    public static final String ROUTE_REQUEST_KEY = "route_request_key";
    public static final String ARG_FROM_LAT     = "arg_from_lat";
    public static final String ARG_FROM_LON     = "arg_from_lon";
    public static final String ARG_TO_LAT       = "arg_to_lat";
    public static final String ARG_TO_LON       = "arg_to_lon";


    private EditText fromField, toField;
    private Button buildRouteBtn;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private NominatimApi nominatimApi;
    private Polyline currentRoute;
    private MapView mapView;
    private OpenRouteServiceApi openRouteServiceApi;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_places, container, false);

        fromField = view.findViewById(R.id.fromField);
        toField = view.findViewById(R.id.toField);
        buildRouteBtn = view.findViewById(R.id.buildRouteBtn);

        // Инициализация геолокации
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireContext());
        // Запросим разрешение прямо сейчас (если ещё не выдано)
        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);

        // Инициализация Retrofit для Nominatim
        nominatimApi = new Retrofit.Builder()
                .baseUrl("https://nominatim.openstreetmap.org/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(NominatimApi.class);

        buildRouteBtn.setOnClickListener(v -> prepareRoute());

        RouteViewModel routeViewModel = new ViewModelProvider(requireActivity()).get(RouteViewModel.class);

// Предположим, у тебя есть TextView'ы
        TextView distanceText = view.findViewById(R.id.textDistance);
        TextView durationText = view.findViewById(R.id.textDuration);

        routeViewModel.getDistance().observe(getViewLifecycleOwner(), dist -> {
            if (dist != null) {
                distanceText.setText("Дистанция: " + String.format("%.2f км", dist / 1000));
            }
        });

        routeViewModel.getDuration().observe(getViewLifecycleOwner(), dur -> {
            if (dur != null) {
                durationText.setText("Время в пути: " + String.format("%.1f мин", dur / 60));
            }
        });


        return view;
    }

    private void prepareRoute() {
        String fromText = fromField.getText().toString().trim();
        String toText   = toField.getText().toString().trim();

        if (toText.isEmpty()) {
            Toast.makeText(getContext(), "Поле «Куда» обязательно", Toast.LENGTH_SHORT).show();
            return;
        }

        if (fromText.isEmpty()) {
            // Если поле "Откуда" пусто, получаем текущее местоположение
            getDeviceLocation(new GeoPointCallback() {
                @Override
                public void onLocationReady(GeoPoint fromPoint) {
                    // После получения текущих координат геокодим поле "Куда"
                    geocodeAndBuildRoute(fromPoint, toText);
                }
                @Override
                public void onFailure(String errorMsg) {
                    Toast.makeText(getContext(), errorMsg, Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // Иначе геокодим сразу оба текста
            geocodeAddresses(fromText, toText);
        }
    }

    /**
     * Шаг 2.a. Получаем последнее известное местоположение (если есть разрешение).
     */
    private void getDeviceLocation(GeoPointCallback callback) {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            callback.onFailure("Нет разрешения на доступ к геолокации");
            return;
        }
        fusedLocationProviderClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        GeoPoint currentPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
                        callback.onLocationReady(currentPoint);
                    } else {
                        callback.onFailure("Не удалось получить текущее местоположение");
                    }
                })
                .addOnFailureListener(e -> callback.onFailure("Ошибка получения локации: " + e.getMessage()));
    }

    /**
     * Шаг 2.b. Геокодим оба адреса («Откуда» и «Куда») последовательно.
     */
    private void geocodeAddresses(String from, String to) {
        Call<List<NominatimPlace>> callFrom = nominatimApi.searchPlaces(
                from, "json", 1, 1, 1, 0, "egor.edrenov@gmail.com"
        );
        callFrom.enqueue(new Callback<List<NominatimPlace>>() {
            @Override
            public void onResponse(Call<List<NominatimPlace>> call1, Response<List<NominatimPlace>> response1) {
                if (response1.isSuccessful() && response1.body() != null && !response1.body().isEmpty()) {
                    NominatimPlace pFrom = response1.body().get(0);
                    GeoPoint fromPoint = new GeoPoint(pFrom.getLat(), pFrom.getLon());

                    Call<List<NominatimPlace>> callTo = nominatimApi.searchPlaces(
                            to, "json", 1, 1, 1, 0, "egor.edrenov@gmail.com"
                    );
                    callTo.enqueue(new Callback<List<NominatimPlace>>() {
                        @Override
                        public void onResponse(Call<List<NominatimPlace>> call2, Response<List<NominatimPlace>> response2) {
                            if (response2.isSuccessful() && response2.body() != null && !response2.body().isEmpty()) {
                                NominatimPlace pTo = response2.body().get(0);
                                GeoPoint toPoint = new GeoPoint(pTo.getLat(), pTo.getLon());
                                buildRouteIfValid(fromPoint, toPoint);
                            } else {
                                Toast.makeText(getContext(), "Не удалось найти адрес «Куда»", Toast.LENGTH_SHORT).show();
                            }
                        }
                        @Override public void onFailure(Call<List<NominatimPlace>> call, Throwable t) {
                            Toast.makeText(getContext(), "Ошибка поиска «Куда»: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    Toast.makeText(getContext(), "Не удалось найти адрес «Откуда»", Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(Call<List<NominatimPlace>> call, Throwable t) {
                Toast.makeText(getContext(), "Ошибка поиска «Откуда»: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }



    /**
     * Шаг 2.c. Если «Откуда» мы получили как GeoPoint (например, текущее местоположение),
     * то геокодим только “Куда”.
     */
    private void geocodeAndBuildRoute(GeoPoint fromPoint, String toAddress) {
        Call<List<NominatimPlace>> call = nominatimApi.searchPlaces(
                toAddress, "json", 1, 1, 1, 0, "egor.edrenov@gmail.com"
        );
        call.enqueue(new Callback<List<NominatimPlace>>() {
            @Override
            public void onResponse(Call<List<NominatimPlace>> call, Response<List<NominatimPlace>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    NominatimPlace pTo = response.body().get(0);
                    GeoPoint toPoint = new GeoPoint(pTo.getLat(), pTo.getLon());
                    buildRouteIfValid(fromPoint, toPoint);
                } else {
                    Toast.makeText(getContext(), "Не удалось найти адрес «Куда»", Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(Call<List<NominatimPlace>> call, Throwable t) {
                Toast.makeText(getContext(), "Ошибка поиска «Куда»: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Шаг 3. Проверяем расстояние между двумя точками, если оно ≤ 50 км — передаём результат в RoutesFragment.
     */
    private void buildRouteIfValid(GeoPoint fromPoint, GeoPoint toPoint) {
        double distanceKm = fromPoint.distanceToAsDouble(toPoint) / 1000.0;

        Bundle resultBundle = new Bundle();
        resultBundle.putDouble(ARG_FROM_LAT, fromPoint.getLatitude());
        resultBundle.putDouble(ARG_FROM_LON, fromPoint.getLongitude());
        resultBundle.putDouble(ARG_TO_LAT, toPoint.getLatitude());
        resultBundle.putDouble(ARG_TO_LON, toPoint.getLongitude());

        Log.d("PlacesFragment", "FROM: " + fromPoint.getLatitude() + "," + fromPoint.getLongitude());
        Log.d("PlacesFragment", "TO: " + toPoint.getLatitude() + "," + toPoint.getLongitude());

        getParentFragmentManager()
                .setFragmentResult(ROUTE_REQUEST_KEY, resultBundle);

        // Переход к фрагменту маршрутов
        NavHostFragment.findNavController(this)
                .navigate(R.id.action_nav_places_to_nav_routes);
        //}
    }

    // Простейший колбэк для получения GeoPoint из getLastLocation()
    private interface GeoPointCallback {
        void onLocationReady(GeoPoint point);
        void onFailure(String errorMsg);
    }
}
