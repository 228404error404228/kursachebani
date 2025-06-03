package com.example.kurs;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapActivity extends AppCompatActivity {

    private MapView map;
    private DatabaseReference attractionsRef;
    private List<Attraction> attractionsList = new ArrayList<>();
    private Map<Marker, Attraction> markersMap = new HashMap<>();
    private Polyline currentRoute;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().setUserAgentValue(getPackageName());
        setContentView(R.layout.activity_map);

        map = findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.getController().setZoom(12.0);
        map.getController().setCenter(new GeoPoint(55.751244, 37.618423)); // Москва

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        attractionsRef = database.getReference("attractions");

        loadAttractionsFromFirebase();
    }

    private void loadAttractionsFromFirebase() {
        attractionsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                attractionsList.clear();
                markersMap.clear();
                map.getOverlays().clear();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Attraction attraction = snapshot.getValue(Attraction.class);
                    if (attraction != null) {
                        attractionsList.add(attraction);
                        addMarker(attraction);
                    }
                }

                map.invalidate();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(MapActivity.this, "Ошибка загрузки данных", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addMarker(Attraction attraction) {
        GeoPoint point = new GeoPoint(attraction.getLatitude(), attraction.getLongitude());
        Marker marker = new Marker(map);
        marker.setPosition(point);
        marker.setTitle(attraction.getName());
        marker.setSubDescription(attraction.getCategory());
        marker.setOnMarkerClickListener((m, mapView) -> {
            showAttractionInfo(attraction);
            return true;
        });

        map.getOverlays().add(marker);
        markersMap.put(marker, attraction);
    }

    private void showAttractionInfo(Attraction attraction) {
        Toast.makeText(this, attraction.getName() + ": " + attraction.getDescription(), Toast.LENGTH_LONG).show();
    }

    public void buildRoute(List<Attraction> selectedAttractions) {
        if (currentRoute != null) {
            map.getOverlays().remove(currentRoute);
        }

        if (selectedAttractions == null || selectedAttractions.isEmpty()) return;

        List<GeoPoint> geoPoints = new ArrayList<>();
        for (Attraction attraction : selectedAttractions) {
            geoPoints.add(new GeoPoint(attraction.getLatitude(), attraction.getLongitude()));
        }

        currentRoute = new Polyline();
        currentRoute.setPoints(geoPoints);
        map.getOverlays().add(currentRoute);
        map.invalidate();
    }

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
        public String getName() { return name; }
        public String getDescription() { return description; }
        public String getCategory() { return category; }
        public double getLatitude() { return latitude; }
        public double getLongitude() { return longitude; }
        public float getRating() { return rating; }

        public void setId(String id) { this.id = id; }
        public void setName(String name) { this.name = name; }
        public void setDescription(String description) { this.description = description; }
        public void setCategory(String category) { this.category = category; }
        public void setLatitude(double latitude) { this.latitude = latitude; }
        public void setLongitude(double longitude) { this.longitude = longitude; }
        public void setRating(float rating) { this.rating = rating; }
    }
}
