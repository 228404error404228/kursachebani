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
import java.io.IOException;
import androidx.lifecycle.ViewModelProvider;



import android.widget.TextView;
import android.widget.Button;

import org.osmdroid.util.BoundingBox;
import android.graphics.Color;

import androidx.annotation.Nullable;
import org.osmdroid.util.GeoPoint;


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
    private Marker searchResultMarker;

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
    public static final String ROUTE_REQUEST_KEY = "route_request_key";
    public static final String ARG_FROM_LAT = "arg_from_lat";
    public static final String ARG_FROM_LON = "arg_from_lon";
    public static final String ARG_TO_LAT = "arg_to_lat";
    public static final String ARG_TO_LON = "arg_to_lon";

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
        private static final double MAX_DISTANCE_KM = 50.0;
        // Ключ результата и имена аргументов


        // Максимальное расстояние (если нужно проверять)


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
        mapController.setCenter(DEFAULT_LOCATION);  // Установите DEFAULT_LOCATION заранее



        // Инициализация OpenRouteService
        Retrofit orsRetrofit = new Retrofit.Builder()
                .baseUrl("https://api.openrouteservice.org/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        openRouteServiceApi = orsRetrofit.create(OpenRouteServiceApi.class);

        // Инициализация Firebase
        database = FirebaseDatabase.getInstance();
        attractionsRef = database.getReference("attractions");

        // Геолокация
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireContext());
        getLocationPermission();  // Метод должен запрашивать разрешение

        // Инициализация Nominatim
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
                    searchAddress(query); // Реализуйте этот метод для поиска через Nominatim
                }
            }
        });

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1) Инициализация карты и API (то, что у вас сейчас в onCreateView)...
        //    mapView = ...; openRouteServiceApi = ...; fusedLocationProviderClient = ...; и т.д.

        // 2) Регистрируем слушатель фрагмент-результатов
        getParentFragmentManager().setFragmentResultListener(
                ROUTE_REQUEST_KEY, this,
                (requestKey, bundle) -> {
                    double fromLat = bundle.getDouble(ARG_FROM_LAT);
                    double fromLon = bundle.getDouble(ARG_FROM_LON);
                    double toLat   = bundle.getDouble(ARG_TO_LAT);
                    double toLon   = bundle.getDouble(ARG_TO_LON);

                    GeoPoint fromPt = new GeoPoint(fromLat, fromLon);
                    GeoPoint toPt   = new GeoPoint(toLat,   toLon);
                    Log.d("RoutesFragment", "Получены координаты: FROM: " + fromPt.getLatitude() + "," + fromPt.getLongitude()
                            + " TO: " + toPt.getLatitude() + "," + toPt.getLongitude());
                    // по желанию проверяете distance и вызываете маршрут
                    buildRouteBetween(fromPt, toPt);


                }
        );
    }

    private void buildRouteBetween(GeoPoint from, GeoPoint to) {
        Log.d("Route", "buildRouteBetween вызван с: " + from + " -> " + to);
        double[] start = {from.getLongitude(), from.getLatitude()};
        double[] end = {to.getLongitude(), to.getLatitude()};
        Map<String, Object> body = new HashMap<>();
        body.put("coordinates", Arrays.asList(start, end));

        Log.d("Route", "Запрос на маршрут: " + Arrays.toString(start) + " -> " + Arrays.toString(end));

        openRouteServiceApi.getRoute(body).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> resp) {
                if (!resp.isSuccessful() || resp.body() == null) {
                    Log.e("Route", "Ошибка ответа ORS. Код: " + resp.code());

                    if (resp.errorBody() != null) {
                        try {
                            Log.e("Route", "Тело ошибки: " + resp.errorBody().string());
                        } catch (IOException e) {
                            Log.e("Route", "Ошибка чтения тела ошибки", e);
                        }
                    }

                    Toast.makeText(getContext(), "Ошибка построения маршрута", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    String json = resp.body().string();
                    Log.d("Route", "Ответ ORS: " + json);

                    JSONObject obj = new JSONObject(json);
                    JSONArray coords = obj.getJSONArray("features")
                            .getJSONObject(0)
                            .getJSONObject("geometry")
                            .getJSONArray("coordinates");

                    List<GeoPoint> pts = new ArrayList<>();
                    for (int i = 0; i < coords.length(); i++) {
                        JSONArray c = coords.getJSONArray(i);
                        pts.add(new GeoPoint(c.getDouble(1), c.getDouble(0)));
                    }

                    if (currentRoute != null) mapView.getOverlays().remove(currentRoute);

                    currentRoute = new Polyline();
                    currentRoute.setPoints(pts);
                    currentRoute.setColor(Color.BLUE);
                    currentRoute.setWidth(6f);
                    mapView.getOverlays().add(currentRoute);

                    BoundingBox bb = BoundingBox.fromGeoPoints(pts);
                    mapView.zoomToBoundingBox(bb, true);

                    mapView.invalidate();

                } catch (Exception e) {
                    Log.e("Route", "Ошибка парсинга маршрута", e);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e("Route", "Ошибка сети при построении маршрута", t);
                Toast.makeText(getContext(), "Ошибка сети: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void buildRouteTo(GeoPoint destination) {
        if (!locationPermissionGranted || fusedLocationProviderClient == null) return;

        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(location -> {
            if (location == null) {
                Toast.makeText(getContext(), "Не удалось получить текущее местоположение", Toast.LENGTH_SHORT).show();
                return;
            }

            double[] start = {location.getLongitude(), location.getLatitude()};
            double[] end = {destination.getLongitude(), destination.getLatitude()};

            Map<String, Object> body = new HashMap<>();
            body.put("coordinates", Arrays.asList(start, end));

            Call<ResponseBody> call = openRouteServiceApi.getRoute(body);

            call.enqueue(new Callback<ResponseBody>() {

                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    try {
                        if (response.isSuccessful() && response.body() != null) {
                            String json = response.body().string();

                            // Создаём основной JSON-объект
                            JSONObject obj = new JSONObject(json);

                            // Получаем feature
                            JSONObject feature = obj.getJSONArray("features").getJSONObject(0);

                            // Получаем сегмент маршрута и его параметры
                            JSONObject segment = feature
                                    .getJSONObject("properties")
                                    .getJSONArray("segments")
                                    .getJSONObject(0);

                            double distanceInMeters = segment.getDouble("distance");
                            double durationInSeconds = segment.getDouble("duration");

                            // Отправляем данные в ViewModel
                            RouteViewModel routeViewModel = new ViewModelProvider(requireActivity()).get(RouteViewModel.class);
                            routeViewModel.setDistance(distanceInMeters);
                            routeViewModel.setDuration(durationInSeconds);

                            // Получаем координаты маршрута
                            JSONArray coordinates = feature
                                    .getJSONObject("geometry")
                                    .getJSONArray("coordinates");

                            List<GeoPoint> geoPoints = new ArrayList<>();
                            for (int i = 0; i < coordinates.length(); i++) {
                                JSONArray coord = coordinates.getJSONArray(i);
                                double lon = coord.getDouble(0);
                                double lat = coord.getDouble(1);
                                geoPoints.add(new GeoPoint(lat, lon));
                            }

                            // Удаляем предыдущий маршрут, если есть
                            if (currentRoute != null) {
                                mapView.getOverlays().remove(currentRoute);
                            }

                            // Отрисовываем маршрут
                            currentRoute = new Polyline();
                            currentRoute.setPoints(geoPoints);
                            currentRoute.setColor(Color.BLUE);
                            currentRoute.setWidth(8f);

                            mapView.getOverlays().add(currentRoute);
                            mapView.invalidate();

                            // Центрируем карту на середине маршрута
                            mapController.setCenter(geoPoints.get(geoPoints.size() / 2));
                        } else {
                            Toast.makeText(getContext(), "Не удалось построить маршрут", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Log.e("Route", "Ошибка парсинга маршрута: " + e.getMessage());
                        Toast.makeText(getContext(), "Ошибка обработки маршрута", Toast.LENGTH_SHORT).show();
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
                query, "json", 1, 5, 1, 0, "egor.edrenov@gmail.com"
        );

        call.enqueue(new Callback<List<NominatimPlace>>() {
            @Override
            public void onResponse(Call<List<NominatimPlace>> call, Response<List<NominatimPlace>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    List<NominatimPlace> places = response.body();

                    // Берем первый результат
                    NominatimPlace place = places.get(0);
                    double lat = place.getLat();
                    double lon = place.getLon();
                    GeoPoint resultPoint = new GeoPoint(lat, lon);

                    Log.d("Search", "Найдено: " + place.getDisplayName() +
                            " (" + lat + ", " + lon + ")");

                    // Добавим маркер на карту
                    Marker searchMarker = new Marker(mapView);
                    searchMarker.setPosition(resultPoint);
                    searchMarker.setTitle("Результат поиска");
                    searchMarker.setSubDescription(place.getDisplayName());
                    searchMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                    searchMarker.setOnMarkerClickListener((m, mv) -> {
                        showRouteDialog(resultPoint);
                        return true;
                    });

                    // Удалим предыдущий маркер поиска, если был (необязательно, но хорошо бы)
                    if (searchResultMarker != null) {
                        mapView.getOverlays().remove(searchResultMarker);
                    }

                    searchResultMarker = searchMarker; // сохранить для последующего удаления
                    mapView.getOverlays().add(searchMarker);

                    // Центрируем карту на найденной точке
                    mapController.setZoom(17.0);
                    mapController.setCenter(resultPoint);

                    mapView.invalidate();
                } else {
                    Log.e("Search", "Поиск завершён, но без результатов. Код: " + response.code());
                    Toast.makeText(getContext(), "Место не найдено", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<NominatimPlace>> call, Throwable t) {
                Log.e("Search", "Сетевая ошибка поиска", t);
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



