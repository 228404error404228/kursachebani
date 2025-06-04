package com.example.kurs.ui;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;
import com.example.kurs.R;
import com.example.kurs.network.OpenRouteServiceApi;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import org.osmdroid.views.overlay.Polyline;
import org.json.JSONArray;
import org.json.JSONObject;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import android.widget.TextView;
import android.widget.Button;



import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import retrofit2.Call;

import com.example.kurs.network.NominatimApi;
import com.example.kurs.network.NominatimPlace;

import android.graphics.Color;



import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.Callback;
import retrofit2.Response;

import android.text.TextWatcher;
import android.text.Editable;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.views.overlay.MapEventsOverlay;
import okhttp3.ResponseBody;




public class RoutesFragment extends Fragment {

    private MapView mapView;
    private IMapController mapController;
    private FirebaseDatabase database;
    private DatabaseReference attractionsRef;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private boolean locationPermissionGranted = false;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private static final GeoPoint DEFAULT_LOCATION = new GeoPoint(55.751244, 37.618423); // Москва
    private OpenRouteServiceApi openRouteServiceApi;
    private Polyline currentRoute;
    private List<Attraction> attractionsList = new ArrayList<>();
    private NominatimApi nominatimApi;
    public static class Attraction {
        private String id;
        private String name;
        private String description;
        private String category;
        private double latitude;
        private double longitude;
        private float rating;





        public Attraction() {}

        public Attraction(String id, String name, String description, String category,
                          double latitude, double longitude, float rating) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.category = category;
            this.latitude = latitude;
            this.longitude = longitude;
            this.rating = rating;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public double getLatitude() { return latitude; }
        public void setLatitude(double latitude) { this.latitude = latitude; }
        public double getLongitude() { return longitude; }
        public void setLongitude(double longitude) { this.longitude = longitude; }
        public float getRating() { return rating; }
        public void setRating(float rating) { this.rating = rating; }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_routes, container, false);

        // Инициализация osmdroid
        Context ctx = requireActivity().getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));

        mapView = view.findViewById(R.id.map);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);

        mapController = mapView.getController();
        mapController.setZoom(15.0);
        mapController.setCenter(DEFAULT_LOCATION);

        Retrofit orsRetrofit = new Retrofit.Builder()
                .baseUrl("https://api.openrouteservice.org/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        openRouteServiceApi = orsRetrofit.create(OpenRouteServiceApi.class);


        // 🎯 Интерактивность карты: добавляем слушатель нажатий
        MapEventsOverlay mapEventsOverlay = new MapEventsOverlay(new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                Toast.makeText(getContext(), "Клик: " + p.getLatitude() + ", " + p.getLongitude(), Toast.LENGTH_SHORT).show();

                Marker marker = new Marker(mapView);
                marker.setPosition(p);
                marker.setTitle("Выбранная точка");
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                marker.setRelatedObject(p); // ✅ сохраняем точку

                marker.setOnMarkerClickListener((m, mv) -> {
                    GeoPoint point = (GeoPoint) m.getRelatedObject(); // ✅ достаём
                    showRouteDialog(point); // ✅ вызываем
                    return true;
                });

                mapView.getOverlays().add(marker);
                mapView.invalidate();




                mapView.getOverlays().add(marker);
                mapView.invalidate();

                return true;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                return false;
            }
        });

        mapView.getOverlays().add(mapEventsOverlay);


        // Инициализация Firebase
        database = FirebaseDatabase.getInstance();
        attractionsRef = database.getReference("attractions");

        // Геолокация
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireContext());
        getLocationPermission();

        // Инициализация Nominatim API через Retrofit
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://nominatim.openstreetmap.org/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        nominatimApi = retrofit.create(NominatimApi.class);

        // Обработка поиска по адресу
        EditText searchField = view.findViewById(R.id.addressSearchField);
        searchField.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String query = s.toString();
                if (!query.isEmpty()) {
                    searchAddress(query);
                }
            }
        });

        return view;
    }
    private void buildRouteTo(GeoPoint destination) {
        if (!locationPermissionGranted || fusedLocationProviderClient == null) return;

        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(location -> {
            if (location == null) return;

            double[] start = {location.getLongitude(), location.getLatitude()};
            double[] end = {destination.getLongitude(), destination.getLatitude()};

            Map<String, Object> body = new HashMap<>();
            body.put("coordinates", Arrays.asList(start, end));

            Call<ResponseBody> call = openRouteServiceApi.getRoute(body); // ✅ исправлено

            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    try {
                        if (response.isSuccessful() && response.body() != null) {
                            String json = response.body().string();
                            JSONObject obj = new JSONObject(json);
                            JSONArray coordinates = obj.getJSONArray("features")
                                    .getJSONObject(0)
                                    .getJSONObject("geometry")
                                    .getJSONArray("coordinates");

                            List<GeoPoint> geoPoints = new ArrayList<>();
                            for (int i = 0; i < coordinates.length(); i++) {
                                JSONArray coord = coordinates.getJSONArray(i);
                                double lon = coord.getDouble(0);
                                double lat = coord.getDouble(1);
                                geoPoints.add(new GeoPoint(lat, lon));
                            }

                            if (currentRoute != null) {
                                mapView.getOverlays().remove(currentRoute);
                            }

                            currentRoute = new Polyline();
                            currentRoute.setPoints(geoPoints);
                            currentRoute.setColor(Color.BLUE);
                            currentRoute.setWidth(8f);

                            mapView.getOverlays().add(currentRoute);
                            mapView.invalidate();
                        } else {
                            Toast.makeText(getContext(), "Ошибка маршрута", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Log.e("Route", "Ошибка парсинга маршрута: " + e.getMessage());
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    Toast.makeText(getContext(), "Ошибка сети: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void showRouteDialog(GeoPoint destinationPoint) {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View bottomSheetView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_route, null);
        dialog.setContentView(bottomSheetView);

        TextView routeTitle = bottomSheetView.findViewById(R.id.routeTitle);
        Button buildRouteBtn = bottomSheetView.findViewById(R.id.buildRouteBtn);

        routeTitle.setText("Маршрут до выбранной точки");

        buildRouteBtn.setOnClickListener(v -> {
            buildRouteTo(destinationPoint);
            dialog.dismiss();
        });

        dialog.show();
    }



    private void searchAddress(String query) {
        Call<List<NominatimPlace>> call = nominatimApi.searchPlaces(
                query, "json", 1, 1, 1, 0, "egor.edrenov@gmail.com"
        );

        call.enqueue(new Callback<List<NominatimPlace>>() {
            @Override
            public void onResponse(Call<List<NominatimPlace>> call, Response<List<NominatimPlace>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    NominatimPlace place = response.body().get(0);
                    double lat = place.getLat();  // без парсинга
                    double lon = place.getLon();
                    GeoPoint point = new GeoPoint(lat, lon);
                    mapController.setCenter(point);

                    Marker marker = new Marker(mapView);
                    marker.setPosition(point);
                    marker.setTitle(place.getDisplayName());
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                    mapView.getOverlays().add(marker);
                    mapView.invalidate();
                } else {
                    Toast.makeText(getContext(), "Место не найдено", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<NominatimPlace>> call, Throwable t) {
                Toast.makeText(getContext(), "Ошибка поиска: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
        if (locationPermissionGranted) {
            getDeviceLocation();
        }
        loadAttractionsFromFirebase();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    private void loadAttractionsFromFirebase() {
        attractionsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                mapView.getOverlays().clear();
                attractionsList.clear();

                for (DataSnapshot item : snapshot.getChildren()) {
                    Attraction attraction = item.getValue(Attraction.class);
                    if (attraction != null) {
                        attractionsList.add(attraction);
                        addMarkerForAttraction(attraction);
                    }
                }
                mapView.invalidate(); // Перерисовать карту
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Ошибка загрузки данных", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addMarkerForAttraction(Attraction attraction) {
        GeoPoint point = new GeoPoint(attraction.getLatitude(), attraction.getLongitude());

        Marker marker = new Marker(mapView);
        marker.setPosition(point);
        marker.setTitle(attraction.getName());
        marker.setSubDescription(attraction.getCategory());
        marker.setSnippet(attraction.getDescription());
        marker.setRelatedObject(point); // ✅ сохраняем точку

        marker.setOnMarkerClickListener((m, mv) -> {
            GeoPoint p = (GeoPoint) m.getRelatedObject(); // ✅ достаём точку
            showRouteDialog(p); // ✅ вызываем диалог
            return true;
        });

        mapView.getOverlays().add(marker);
    }



    private void getLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true;
            getDeviceLocation();
        } else {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    private void showRouteBottomSheet(Attraction attraction) {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View sheetView = LayoutInflater.from(getContext()).inflate(R.layout.bottom_sheet_route, null);
        dialog.setContentView(sheetView);

        TextView title = sheetView.findViewById(R.id.attractionTitle);
        TextView description = sheetView.findViewById(R.id.attractionDescription);
        Button btnBuildRoute = sheetView.findViewById(R.id.btnBuildRoute);

        title.setText(attraction.getName());
        description.setText(attraction.getDescription());

        btnBuildRoute.setOnClickListener(v -> {
            dialog.dismiss();
            GeoPoint destination = new GeoPoint(attraction.getLatitude(), attraction.getLongitude());
            buildRouteTo(destination);  // 👈 метод уже реализован
        });

        dialog.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION) {
            locationPermissionGranted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            if (locationPermissionGranted) {
                getDeviceLocation();
            }
        }
    }

    private void getDeviceLocation() {
        try {
            if (locationPermissionGranted) {
                Task<Location> locationResult = fusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(requireActivity(), task -> {
                    if (task.isSuccessful()) {
                        Location location = task.getResult();
                        if (location != null) {
                            GeoPoint current = new GeoPoint(location.getLatitude(), location.getLongitude());
                            mapController.setCenter(current);
                            Marker currentMarker = new Marker(mapView);
                            currentMarker.setPosition(current);
                            currentMarker.setTitle("Вы здесь");
                            currentMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                            mapView.getOverlays().add(currentMarker);
                            mapView.invalidate();
                        }
                    } else {
                        mapController.setCenter(DEFAULT_LOCATION);
                    }
                });
            }
        } catch (SecurityException e) {
            Log.e("RoutesFragment", "Ошибка получения локации: " + e.getMessage());
        }
    }

    // Модель достопримечательности оставьте без изменений
}


// Модель достопримечательности



