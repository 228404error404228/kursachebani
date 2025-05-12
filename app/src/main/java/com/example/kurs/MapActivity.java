package com.example.kurs;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private GoogleMap mMap;
    private DatabaseReference attractionsRef;
    private List<Attraction> attractionsList = new ArrayList<>();
    private Map<Marker, Attraction> markersMap = new HashMap<>();
    private Polyline currentRoute;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        // Инициализа   ция Firebase
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        attractionsRef = database.getReference("attractions");

        // Получаем SupportMapFragment и уведомляемся, когда карта готова к использованию
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMarkerClickListener(this);

        // Настройка карты
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setMapToolbarEnabled(true);

        // Загрузка достопримечательностей из Firebase
        loadAttractionsFromFirebase();

        // Установка камеры на начальную позицию (можно заменить на текущее местоположение)
        LatLng defaultLocation = new LatLng(55.751244, 37.618423); // Москва по умолчанию
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 12));
    }

    private void loadAttractionsFromFirebase() {
        attractionsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                attractionsList.clear();
                markersMap.clear();
                mMap.clear();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Attraction attraction = snapshot.getValue(Attraction.class);
                    if (attraction != null) {
                        attractionsList.add(attraction);
                        addMarkerForAttraction(attraction);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(MapActivity.this, "Ошибка загрузки данных", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addMarkerForAttraction(Attraction attraction) {
        LatLng position = new LatLng(attraction.getLatitude(), attraction.getLongitude());
        MarkerOptions markerOptions = new MarkerOptions()
                .position(position)
                .title(attraction.getName())
                .snippet(attraction.getCategory())
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));

        Marker marker = mMap.addMarker(markerOptions);
        markersMap.put(marker, attraction);
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        // Показываем информацию о достопримечательности при клике на маркер
        Attraction attraction = markersMap.get(marker);
        if (attraction != null) {
            showAttractionInfo(attraction);
            return true;
        }
        return false;
    }

    private void showAttractionInfo(Attraction attraction) {
        // Реализация показа информации о достопримечательности
        // Можно использовать BottomSheetDialog или другое UI-решение
        Toast.makeText(this, attraction.getName() + ": " + attraction.getDescription(), Toast.LENGTH_LONG).show();
    }

    public void buildRoute(List<Attraction> selectedAttractions) {
        if (currentRoute != null) {
            currentRoute.remove();
        }

        if (selectedAttractions == null || selectedAttractions.isEmpty()) {
            return;
        }

        PolylineOptions polylineOptions = new PolylineOptions();
        polylineOptions.width(8);
        polylineOptions.color(getResources().getColor(R.color.colorPrimary));

        // Добавляем точки маршрута
        for (Attraction attraction : selectedAttractions) {
            polylineOptions.add(new LatLng(attraction.getLatitude(), attraction.getLongitude()));
        }

        // Рисуем маршрут на карте
        currentRoute = mMap.addPolyline(polylineOptions);

        // Перемещаем камеру, чтобы показать весь маршрут
        // (Здесь можно добавить более сложную логику для определения оптимального масштаба)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                new LatLng(selectedAttractions.get(0).getLatitude(),
                        selectedAttractions.get(0).getLongitude()), 13));
    }

    // Класс модели для достопримечательности
    public static class Attraction {
        private String id;
        private String name;
        private String description;
        private String category;
        private double latitude;
        private double longitude;
        private float rating;

        // Конструкторы, геттеры и сеттеры
        public Attraction() {
        }

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

        // Геттеры и сеттеры
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
}